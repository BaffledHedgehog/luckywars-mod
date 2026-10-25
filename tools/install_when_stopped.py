"""Install a qualified local build after the main server shuts down normally."""
from pathlib import Path
import argparse
import hashlib
import json
import os
import socket
import time


PROJECT = Path(__file__).resolve().parents[1]
MAIN = PROJECT.parent


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def qualified(qualification):
    qualification = qualification.resolve(strict=True)
    qualification.relative_to(PROJECT / "build/verification")
    evidence = json.loads(qualification.read_text(encoding="utf-8"))
    artifact = (PROJECT / evidence["artifact"]).resolve(strict=True)
    artifact.relative_to(PROJECT / "build/verification")
    expected = evidence["sha256"]
    if artifact.suffix != ".jar" or digest(artifact) != expected:
        raise ValueError("Qualified artifact hash mismatch")
    return qualification, evidence, artifact, expected


def install(qualification):
    qualification, evidence, artifact, expected = qualified(qualification)
    installed = MAIN / "mods/skywars2.jar"
    baseline = digest(installed)
    receipt = qualification.parent / "main-install.json"
    props = dict(line.split("=", 1) for line in (MAIN / "server.properties").read_text(
        encoding="utf-8").splitlines() if line and not line.startswith("#") and "=" in line)
    world = (MAIN / props.get("level-name", "world")).resolve(strict=True)
    world.relative_to(MAIN)
    lock_path = world / "session.lock"
    port = int(props.get("server-port", "25565"))

    def record(status, **extra):
        receipt.write_text(json.dumps(dict(status=status, artifact=str(artifact),
            sha256=expected, previousSha256=baseline, server=str(MAIN), workerPid=os.getpid(), **extra),
            indent=2) + "\n", encoding="utf-8")

    if baseline == expected:
        record("installed", alreadyPresent=True)
        return
    if baseline != evidence["mainJarUnchanged"]:
        raise RuntimeError("Main JAR changed since qualification; preserving newer installation")
    request = PROJECT / "build/verification/main-install-request.json"
    staged_request = request.with_suffix(f".{os.getpid()}.tmp")
    staged_request.write_text(json.dumps({"qualification": str(qualification), "sha256": expected}) + "\n", encoding="utf-8")
    os.replace(staged_request, request)
    # Only one installer may target the main JAR. The OS releases this lock on exit.
    import msvcrt
    guard = PROJECT / "build/verification/main-install.guard"
    with guard.open("a+b") as exclusive:
        exclusive.seek(0)
        try:
            msvcrt.locking(exclusive.fileno(), msvcrt.LK_NBLCK, 1)
        except OSError:
            # The existing worker reads this request and adopts the newest qualified update.
            record("queued-for-existing-installer")
            return
        record("waiting-for-normal-shutdown")
        while True:
            next_request = json.loads(request.read_text(encoding="utf-8"))
            latest = Path(next_request["qualification"])
            if latest != qualification or next_request.get("sha256", expected) != expected:
                next_qualification, next_evidence, next_artifact, next_expected = qualified(latest)
                if next_evidence["mainJarUnchanged"] != baseline:
                    raise RuntimeError("Queued update expects a different main JAR")
                record("superseded", replacement=str(next_qualification))
                qualification, evidence, artifact, expected = next_qualification, next_evidence, next_artifact, next_expected
                receipt = qualification.parent / "main-install.json"
                record("waiting-for-normal-shutdown")
            if digest(installed) != baseline:
                record("cancelled-main-jar-changed")
                return
            try:
                with lock_path.open("r+b") as world_lock:
                    msvcrt.locking(world_lock.fileno(), msvcrt.LK_NBLCK, 1)
                    try:
                        with socket.socket() as probe:
                            probe.settimeout(.25)
                            if probe.connect_ex(("127.0.0.1", port)) == 0:
                                raise BlockingIOError("Server still accepts connections")
                        if digest(artifact) != expected or digest(installed) != baseline:
                            raise RuntimeError("Artifact or main JAR changed while waiting")
                        backups = sorted((PROJECT / "build/verification").glob(f"*-{baseline[:8]}.jar"))
                        backup = next((p for p in backups if digest(p) == baseline),
                            PROJECT / f"build/verification/main-rollback-{baseline[:8]}.jar")
                        if backup.exists() and digest(backup) != baseline:
                            raise RuntimeError("Existing rollback hash mismatch")
                        if not backup.exists():
                            backup.write_bytes(installed.read_bytes())
                        if digest(backup) != baseline:
                            raise RuntimeError("Rollback hash mismatch")
                        staged = MAIN / "mods/skywars2.jar.pending"
                        staged.write_bytes(artifact.read_bytes())
                        if digest(staged) != expected:
                            raise RuntimeError("Staged JAR hash mismatch")
                        os.replace(staged, installed)
                        if digest(installed) != expected:
                            raise RuntimeError("Installed JAR hash mismatch")
                        record("installed", backup=str(backup), restarted=False)
                        return
                    finally:
                        world_lock.seek(0)
                        msvcrt.locking(world_lock.fileno(), msvcrt.LK_UNLCK, 1)
            except (PermissionError, BlockingIOError):
                # The old JVM can retain its JAR handles briefly after releasing the world.
                time.sleep(2)
            except OSError as error:
                if getattr(error, "winerror", None) in (32, 33, 36) or error.errno in (13, 36):
                    time.sleep(2)
                else:
                    raise


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("qualification", type=Path)
    install(parser.parse_args().qualification)
