"""Exercise the Windows install guard without touching a real server."""
import hashlib
import json
import os
from pathlib import Path
import socket
import subprocess
import sys
import tempfile
import time
import unittest


INSTALLER = Path(__file__).with_name("install_when_stopped.py").resolve()
CHILD = """import runpy, sys
from pathlib import Path
namespace = runpy.run_path(sys.argv[1])
install = namespace['install']
install.__globals__['PROJECT'] = Path(sys.argv[2]).resolve()
install.__globals__['MAIN'] = Path(sys.argv[2]).resolve().parent
install(Path(sys.argv[3]))
"""


@unittest.skipUnless(os.name == "nt", "Uses Minecraft's Windows world-lock guard")
class InstallGuardChecks(unittest.TestCase):
    def exercise(self, hold_world):
        import msvcrt

        with tempfile.TemporaryDirectory(prefix="skywars-install-guard-") as directory:
            main = Path(directory)
            project = main / "project"
            stage = project / "build/verification/candidate"
            stage.mkdir(parents=True)
            (main / "mods").mkdir()
            (main / "world").mkdir()
            installed = main / "mods/skywars2.jar"
            installed.write_bytes(b"previous qualified archive")
            artifact = stage / "candidate.jar"
            artifact.write_bytes(b"new qualified archive")
            old_hash = hashlib.sha256(installed.read_bytes()).hexdigest()
            new_hash = hashlib.sha256(artifact.read_bytes()).hexdigest()
            qualification = stage / "qualification.json"
            qualification.write_text(json.dumps({
                "artifact": str(artifact.relative_to(project)),
                "sha256": new_hash, "mainJarUnchanged": old_hash,
            }), encoding="utf-8")
            with socket.socket() as unrelated, (main / "world/session.lock").open("w+b") as lock:
                unrelated.bind(("127.0.0.1", 0))
                unrelated.listen()
                (main / "server.properties").write_text(
                    f"level-name=world\nserver-port={unrelated.getsockname()[1]}\n",
                    encoding="utf-8")
                lock.write(b"x")
                lock.flush()
                lock.seek(0)
                locked = hold_world
                if locked:
                    msvcrt.locking(lock.fileno(), msvcrt.LK_NBLCK, 1)
                worker = subprocess.Popen([sys.executable, "-c", CHILD,
                    str(INSTALLER), str(project), str(qualification)],
                    stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
                try:
                    if hold_world:
                        deadline = time.monotonic() + 10
                        receipt = stage / "main-install.json"
                        while not receipt.exists() and worker.poll() is None and time.monotonic() < deadline:
                            time.sleep(.05)
                        self.assertTrue(receipt.exists(), "Worker did not reach the world guard")
                        time.sleep(.3)
                        self.assertIsNone(worker.poll(), "Worker ignored the held world lock")
                        self.assertEqual(hashlib.sha256(installed.read_bytes()).hexdigest(), old_hash)
                        lock.seek(0)
                        msvcrt.locking(lock.fileno(), msvcrt.LK_UNLCK, 1)
                        locked = False
                    stdout, stderr = worker.communicate(timeout=10)
                    self.assertEqual(worker.returncode, 0, stdout + stderr)
                    receipt = json.loads((stage / "main-install.json").read_text())
                    self.assertEqual(receipt["status"], "installed")
                    self.assertFalse(receipt["restarted"])
                    self.assertEqual(hashlib.sha256(installed.read_bytes()).hexdigest(), new_hash)
                    self.assertEqual(hashlib.sha256(Path(receipt["backup"]).read_bytes()).hexdigest(), old_hash)
                finally:
                    if worker.poll() is None:
                        worker.terminate()
                        worker.communicate(timeout=10)
                    if locked:
                        lock.seek(0)
                        msvcrt.locking(lock.fileno(), msvcrt.LK_UNLCK, 1)

    def test_unrelated_listener_does_not_block_install(self):
        self.exercise(hold_world=False)

    def test_live_world_blocks_install_until_released(self):
        self.exercise(hold_world=True)


if __name__ == "__main__":
    unittest.main()
