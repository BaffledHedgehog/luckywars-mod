"""Syringe and complete expanded summoner pool, with native cross-family regressions."""
from pathlib import Path
import argparse
import hashlib
import json
import os
import re
import shutil
import socket
import sys
import time

sys.path.insert(0,str(Path(__file__).resolve().parent))
import smoke_game_cycle as cycle


def next_server_tick(server):
    """Independent Academy fixtures must not look like two packets of one native click."""
    def game_time():
        cursor = server.send("time query gametime")
        line = server.expect("The game time is ", cursor, timeout=10)
        return int(re.search(r"The game time is (\d+)", server.lines[line]).group(1))
    previous = game_time()
    deadline = time.monotonic() + 5
    while time.monotonic() < deadline:
        time.sleep(.025)
        if game_time() != previous:
            return
    raise AssertionError("The native server clock did not advance between independent fixtures")


def run(server_dir: Path, focus: list[str] | None = None, keep_going: bool = False) -> None:
    # Full runs collect independent fixture failures, then reject the entire release.
    keep_going = keep_going or focus is None
    failures = []
    allowed = (cycle.PROJECT / "build/verification/key-reuse-server").resolve()
    if server_dir.resolve() != allowed:
        raise ValueError("Gameplay checks require build/verification/key-reuse-server")
    cycle.SERVER = allowed
    props = dict(line.split("=", 1) for line in (allowed / "server.properties").read_text(
        encoding="utf-8").splitlines() if line and not line.startswith("#") and "=" in line)
    if props.get("server-port") != "25576":
        raise ValueError("Isolated server must use port 25576")
    world = (allowed / props.get("level-name", "world")).resolve()
    world.relative_to(allowed)
    if not (allowed / "fabric-server-launch.jar").is_file():
        raise FileNotFoundError("Isolated Fabric launcher is absent")
    lock = allowed / "codex-syringe.lock"
    shared_lock = allowed / "codex-native-fixture.lock"
    shared_descriptor = os.open(shared_lock, os.O_CREAT | os.O_EXCL | os.O_WRONLY)
    try:
        descriptor = os.open(lock, os.O_CREAT | os.O_EXCL | os.O_WRONLY)
    except BaseException:
        os.close(shared_descriptor)
        shared_lock.unlink()
        raise
    os.write(descriptor, str(os.getpid()).encode("ascii"))
    active = None
    backups = {}
    completed = []
    success = False
    source_sha256 = tested_sha256 = None
    started = time.monotonic()
    try:
        cycle.require_stopped(allowed)
        with socket.socket() as probe:
            if probe.connect_ex(("127.0.0.1", 25576)) == 0:
                raise RuntimeError("Port 25576 is already accepting connections")
        settings_path = allowed / "config/luckywars/game.json"
        profiles_path = allowed / "config/luckywars/players.json"
        # scoreboard contains permanent modifier settings and match-owned counters.
        for path in (settings_path, profiles_path, world / "data/scoreboard.dat", world / "data/scoreboard.dat_old", allowed / "mods/skywars2.jar"):
            backups[path] = path.read_bytes() if path.exists() else None
        artifact = cycle.PROJECT / "build/libs/skywars2-0.1.0.jar"
        source_sha256 = hashlib.sha256(artifact.read_bytes()).hexdigest()
        installed = allowed / "mods/skywars2.jar"
        installed.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(artifact, installed)
        tested_sha256 = hashlib.sha256(installed.read_bytes()).hexdigest()
        if source_sha256 != tested_sha256:
            raise AssertionError("Installed artifact differs from source artifact")
        settings = json.loads(backups[settings_path] or b"{}")
        settings.update(lives=2, health=20, day=1, gameMode=1, countdownSeconds=1,
                        stageOneSeconds=600, stageTwoSeconds=1200, endingSeconds=1,
                        stageOneEvent=0, stageTwoEvent=0, vanillaKits=False,
                        vanillaSkills=False, lbcKits=True, lbcSkills=True,
                        lbcLevelups=True, lbcLevelupLimit=4, mapId="1", broken=False, noBorders=True)
        settings_path.parent.mkdir(parents=True, exist_ok=True)
        settings_path.write_text(json.dumps(settings, indent=2) + "\n", encoding="utf-8")
        active = cycle.Server("syringe")
        active.expect("LOADING -> LOBBY", timeout=180)
        active.expect("Selected map ready: 1", timeout=180)
        for suite in ("bots", "loot-test-catalog"):
            active.check(suite)
        for match in ((1,) if focus else (1, 2)):
            if match == 2:
                active.prepare_map(1)
            for suite in ("kfc-menu",):
                active.check(suite)
                completed.append({"match": match, "suite": suite})
            cursor = active.send("sw start")
            active.expect("-> PLAYING", cursor)
            for suite in (focus or ("syringe", "ordinary-summoners", "summons", "summons-crafting", "friendly-projectiles", "kfc-rewards", "creeper-drops", "special-eggs")):
                # Native combat regressions can leave dropped loot after their actors retire.
                # This disposable match has no real players/items; keep independent fixtures clean.
                for entity_type in ("minecraft:item", "minecraft:experience_orb"):
                    active.send(f"execute in minecraft:overworld run kill @e[type={entity_type}]")
                next_server_tick(active)
                try:
                    active.check(suite)
                except AssertionError as error:
                    if not keep_going:
                        raise
                    failures.append({"match": match, "suite": suite, "error": str(error)})
                    continue
                completed.append({"match": match, "suite": suite})
            for probe in (() if focus else ("syringe-stop-probe",)):
                active.check(probe)
                completed.append({"match": match, "suite": probe})
            cursor = active.send("sw stop")
            active.expect("LOADING -> LOBBY", cursor)
            for suite in (() if focus else ("syringe-cleanup",)):
                active.check(suite)
                completed.append({"match": match, "suite": suite})
        if failures:
            raise AssertionError(f"Focused suites failed: {failures}")
        success = True
        print("SYRINGE CHECKS PASSED", flush=True)
    finally:
        original_error = sys.exc_info()[1]
        cleanup_errors = []

        def attempt(action):
            try:
                action()
                return True
            except BaseException as error:
                cleanup_errors.append(error)
                return False

        if active is not None:
            attempt(active.stop)
        # Never restore files while a server might still be using them.
        if attempt(lambda: cycle.require_stopped(allowed)):
            for path, original in backups.items():
                def restore(path=path, original=original):
                    if original is None:
                        path.unlink(missing_ok=True)
                    else:
                        path.parent.mkdir(parents=True, exist_ok=True)
                        path.write_bytes(original)
                attempt(restore)
        # Each release is independent: one failed restore/close cannot strand another lock.
        attempt(lambda: os.close(descriptor))
        attempt(lambda: lock.unlink(missing_ok=True))
        attempt(lambda: os.close(shared_descriptor))
        attempt(lambda: shared_lock.unlink(missing_ok=True))
        attempt(lambda: (cycle.OUT / "syringe-results.json").write_text(json.dumps({
            "success": success and not cleanup_errors,
            "scope": "native-mode1-syringe-and-expanded-summoners",
            "completed": completed, "checkFailures": failures, "artifactSha256": tested_sha256,
            "sourceArtifactSha256": source_sha256, "serverDirectory": str(allowed),
            "serverPort": 25576,
            "cleanupErrors": [str(error) for error in cleanup_errors],
            "measurementLimit": "Actual item use, native outbound sound packets, payload ownership and actual match stops. Real client audio playback, physical client disconnect and full server restart and peak-capacity performance are not qualified. Elapsed wall time includes boot, map loading and regression checks.",
            "elapsedSeconds": round(time.monotonic() - started, 2)
        }, indent=2) + "\n", encoding="utf-8"))
        if original_error is None and cleanup_errors:
            raise cleanup_errors[0]



if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--server-dir", type=Path, required=True)
    parser.add_argument("--focus", nargs="+", help="Run selected suites in one disposable match for debugging")
    parser.add_argument("--keep-going", action="store_true", help="Collect independent focused-suite failures before stopping")
    args = parser.parse_args()
    run(args.server_dir, args.focus, args.keep_going)
