"""Live integration check. Stop the target server before running this script.

Uses operator-only /sw check fixtures with normal ServerPlayer and respawn packets.
Pass --server-dir to test an isolated instance while the main server stays running.
Never connects to or edits the legacy Paper server.
"""
from pathlib import Path
import argparse
import json
import os
import subprocess
import threading
import time

PROJECT = Path(__file__).resolve().parents[1]
SERVER = PROJECT.parent
JAVA = SERVER / "runtime/jdk-25/bin/java.exe"
OUT = PROJECT / "build" / "verification"
OUT.mkdir(parents=True, exist_ok=True)
# Applied translations may have been removed from the mod by the user.
LANGUAGE_FILE = PROJECT / "src/main/resources/assets/skywars2/lang/en_us.json"
CONSOLE_LANGUAGE = json.loads(LANGUAGE_FILE.read_text(encoding="utf-8")) if LANGUAGE_FILE.exists() else {}


def require_stopped(server_dir):
    """Refuse a server still saving its world, even after its listening port closed."""
    props = dict(line.split("=", 1) for line in (server_dir / "server.properties").read_text(
        encoding="utf-8").splitlines() if line and not line.startswith("#") and "=" in line)
    world = (server_dir / props.get("level-name", "world")).resolve()
    world.relative_to(server_dir.resolve())
    lock = world / "session.lock"
    if os.name == "nt" and lock.exists():
        import msvcrt
        with lock.open("r+b") as stream:
            try:
                msvcrt.locking(stream.fileno(), msvcrt.LK_NBLCK, 1)
            except OSError as error:
                raise RuntimeError("The isolated server still holds its world lock; wait for shutdown") from error
            msvcrt.locking(stream.fileno(), msvcrt.LK_UNLCK, 1)


def console_text(key, *args):
    # Native server Language renders mod keys in English; clients receive components.
    # Keys moved by the user into their client pack have no server-side Language
    # entry. In that case Minecraft's console renders the key itself.
    full_key = "skywars2." + key
    template = CONSOLE_LANGUAGE.get(full_key)
    if template is None:
        return full_key
    return template % args if args else template


class Server:
    def __init__(self, name):
        self.lines = []
        self.condition = threading.Condition()
        self.log = (OUT / f"{name}.log").open("w", encoding="utf-8")
        self.process = subprocess.Popen(
            [str(JAVA), "-Xms1G", "-Xmx4G", "-jar", "fabric-server-launch.jar", "nogui"],
            cwd=SERVER, stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
            encoding="utf-8", errors="replace", bufsize=1,
            creationflags=subprocess.CREATE_NO_WINDOW if os.name == "nt" else 0,
        )
        threading.Thread(target=self.read, daemon=True).start()

    def read(self):
        for line in self.process.stdout:
            with self.condition:
                self.lines.append(line.rstrip())
                self.log.write(line)
                self.log.flush()
                self.condition.notify_all()
            if "[check]" in line or ("[game]" in line and " -> " in line):
                print(line.rstrip(), flush=True)

    def send(self, *commands):
        with self.condition:
            cursor = len(self.lines)
        for command in commands:
            self.process.stdin.write(command + "\n")
        self.process.stdin.flush()
        return cursor

    def expect(self, message, cursor=0, timeout=90):
        deadline = time.monotonic() + timeout
        with self.condition:
            while time.monotonic() < deadline:
                for index, line in enumerate(self.lines[cursor:], cursor):
                    if "[check] FAIL" in line:
                        raise AssertionError(line)
                    if message in line:
                        # Suite names have prefixes (kiuas / kiuas-menu); a completed
                        # neighbour must not satisfy the next command's result wait.
                        suffix = line.partition(message)[2]
                        if not message.startswith("[check] PASS ") or not suffix or suffix[0].isspace():
                            return index
                if self.process.poll() is not None:
                    raise AssertionError(f"Server exited {self.process.returncode}: waiting for {message}")
                self.condition.wait(min(1, deadline - time.monotonic()))
        raise AssertionError(f"Timed out waiting for {message}; last output: {self.lines[-8:]}")

    def check(self, name):
        cursor = self.send("sw check " + name)
        self.expect("[check] PASS " + name, cursor)
        return cursor

    def prepare_map(self, map_id, broken=False):
        command = f"sw map {map_id}" + (" broken" if broken else "")
        cursor = self.send(command)
        self.expect(f"Selected map ready: {map_id}", cursor, timeout=180)
        return cursor

    def stop(self):
        try:
            if self.process.poll() is None:
                self.send("stop")
                try:
                    self.process.wait(timeout=90)
                except subprocess.TimeoutExpired:
                    # Only our child process: never leave an orphan holding the test world.
                    self.process.kill()
                    self.process.wait(timeout=30)
                    raise
        finally:
            self.log.close()


def check_interrupted_loading():
    active = None
    try:
        # Lobby map preparation is asynchronous; the start transition itself is now synchronous.
        active = Server("loading-shutdown")
        active.expect("LOADING -> LOBBY")
        active.check("bots")
        cursor = active.send("sw map 1")
        active.stop()
        assert not any("Couldn't load structure" in line or "ClosedByInterruptException" in line
                       for line in active.lines), "Shutdown interrupted Minecraft's shared resource stream"
        active = None

        active = Server("loading-recovery")
        active.expect("LOADING -> LOBBY")
        active.check("cleanup")
        active.check("bots")
        active.check("lobby")
        assert not any("-> COUNTDOWN" in line for line in active.lines), "Cancelled loading resumed a match after restart"
        active.stop()
        active = None
        print("MAP PREPARATION INTERRUPTION CHECKS PASSED", flush=True)
        return ["Clean shutdown while lobby map preparation is pending",
                "Restart after interrupted preparation returns to a clean lobby"]
    finally:
        if active is not None:
            active.stop()


def run():
    active = None
    settings = SERVER / "config/luckywars/game.json"
    original = settings.read_bytes() if settings.exists() else None
    started = time.time()
    results = []
    try:
        active = Server("game-cycle")
        active.expect("LOADING -> LOBBY")
        active.expect("Selected map ready:", timeout=180)
        original_health = json.loads(settings.read_text(encoding="utf-8"))["health"]
        cursor = active.send("settings health 19")
        active.expect(console_text("settings.saved", "health", 19), cursor)
        assert json.loads(settings.read_text(encoding="utf-8"))["health"] == 19
        cursor = active.send("settings health 61")
        active.expect(console_text("settings.range", "health", 1, 60), cursor)
        assert json.loads(settings.read_text(encoding="utf-8"))["health"] == 19
        cursor = active.send(f"settings health {original_health}")
        active.expect(console_text("settings.saved", "health", original_health), cursor)
        for check in ["bots", "lobby", "lobby-rules", "lobby-inventory", "design-export", "menus", "fast"]:
            active.check(check)
        watch_cursor = active.check("watch")
        active.expect("Selected map ready:", watch_cursor, timeout=180)
        cursor = active.send("sw start")
        active.expect("COUNTDOWN -> PLAYING", cursor)
        # Ordinary found compasses track opponents; the lobby menu compass is no longer granted.
        active.send("give TestAlpha minecraft:compass", "give TestBeta minecraft:compass")
        active.expect("PHASE_ONE -> PHASE_TWO", cursor)
        cursor = active.send("settings health 19")
        active.expect(console_text("settings.saved", "health", 19), cursor)
        assert json.loads(settings.read_text(encoding="utf-8"))["health"] == 19
        cursor = active.send(f"settings health {original_health}")
        active.expect(console_text("settings.saved", "health", original_health), cursor)
        for check in ["playing", "inventory-unlocked", "phases", "tracker", "chests", "late", "bridge"]:
            active.check(check)
        active.check("kill-reward")
        active.send("sw check respawn")
        time.sleep(.3)
        cursor = active.send("damage TestAlpha 100 minecraft:generic_kill", "sw check respawn")
        active.expect("-> ENDING", cursor)
        active.check("ending")
        active.expect("PASS watch: lobby preparation -> countdown -> playing -> phases -> ending -> lobby", cursor)
        cleanup_cursor = active.check("cleanup")
        active.expect("Selected map ready:", cleanup_cursor, timeout=180)
        results.append("GUI + stages + late join + victory rewards + bridge cleanup")

        cursor = active.check("lives")
        active.expect("PASS lives: vanilla packet respawn", cursor)
        lobby_cursor = active.expect("LOADING -> LOBBY", cursor)
        active.expect("Selected map ready:", lobby_cursor, timeout=180)
        active.check("lobby")
        results.append("Two lives: actual damage death, PERFORM_RESPAWN, elimination")

        active.check("one-life")
        active.check("snapshot")
        cursor = active.send("sw start")
        active.expect("COUNTDOWN -> PLAYING", cursor)
        cursor = active.send("damage TestAlpha 100 minecraft:generic_kill", "damage TestBeta 100 minecraft:generic_kill", "sw check respawn")
        active.expect("-> ENDING", cursor)
        active.check("draw")
        lobby_cursor = active.expect("LOADING -> LOBBY", cursor)
        active.expect("Selected map ready:", lobby_cursor, timeout=180)
        results.append("Simultaneous elimination produces a draw without victory rewards")

        active.check("restore")
        cursor = active.send("sw map 1 broken")
        active.expect("Selected map ready:", cursor, timeout=180)
        cursor = active.send("sw start")
        active.expect("COUNTDOWN -> PLAYING", cursor)
        active.check("bridge")
        active.stop()
        active = None
        results.append("Broken map placement and clean shutdown during active match")

        active = Server("restart-cleanup")
        active.expect("LOADING -> LOBBY")
        active.check("cleanup")
        active.check("bots")
        active.check("lobby")
        results.append("Restart clears prior map and outside-template bridge from bounds journal")
        # This chunk was forced solely for the other-dimension entity probe.
        active.send("execute in minecraft:overworld run forceload remove 0 0")
        active.stop()
        active = None

        results.extend(check_interrupted_loading())
        print("ALL LIVE CHECKS PASSED", flush=True)
    finally:
        if active is not None:
            active.stop()
        # Restore operator settings regardless of test outcome. Test profiles are
        # retained in players.json as evidence; they have isolated offline UUIDs.
        if original is not None:
            settings.write_bytes(original)
        (OUT / "results.json").write_text(json.dumps({"completed": results,
                "elapsedSeconds": round(time.time() - started, 2)}, indent=2), encoding="utf-8")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--server-dir", type=Path,
                        help="Run against an isolated server directory instead of the main server")
    args = parser.parse_args()
    if args.server_dir:
        SERVER = args.server_dir.resolve()
    run()
