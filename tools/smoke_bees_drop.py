"""Native Bees summons, hive lifecycle, honey loot and source-drop regressions in two actual mode1 matches."""
from pathlib import Path
import argparse
import hashlib
import json
import os
import shutil
import socket
import sys
import time

import smoke_game_cycle as cycle


def run(server_dir: Path) -> None:
    allowed = (cycle.PROJECT / "build/verification/key-reuse-server").resolve()
    if server_dir.resolve() != allowed:
        raise ValueError("Bees drop checks require build/verification/key-reuse-server")
    cycle.SERVER = allowed
    props = dict(line.split("=", 1) for line in (allowed / "server.properties").read_text(
        encoding="utf-8").splitlines() if line and not line.startswith("#") and "=" in line)
    if props.get("server-port") != "25576":
        raise ValueError("Isolated server must use port 25576")
    world = (allowed / props.get("level-name", "world")).resolve()
    world.relative_to(allowed)
    if not (allowed / "fabric-server-launch.jar").is_file():
        raise FileNotFoundError("Isolated Fabric launcher is absent")
    lock = allowed / "codex-bees-drop.lock"
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
        active = cycle.Server("bees-drop")
        active.expect("LOADING -> LOBBY", timeout=180)
        active.expect("Selected map ready: 1", timeout=180)
        for suite in ("bots", "loot-test-catalog"):
            active.check(suite)
        for match in (1, 2):
            if match == 2:
                active.prepare_map(1)
            cursor = active.send("sw start")
            active.expect("-> PLAYING", cursor)
            for suite in ("bees-drop", "library-drop", "enzan-drop", "lucky-pigs", "scare-drops", "home-drop", "falling-netherite-drops", "zombie-tnt-drops", "corrosion-drops", "hole-drops", "cage-drops", "creeper-drops", "tnt-drops", "chest-drops", "chest-drops-stop-probe", "tnt-drops-stop-probe", "creeper-drops-stop-probe", "cage-drops-stop-probe", "hole-drops-stop-probe", "corrosion-drops-stop-probe", "zombie-tnt-drops-stop-probe", "falling-netherite-drops-stop-probe", "home-drop-stop-probe", "scare-drops-stop-probe", "lucky-pigs-stop-probe", "enzan-drop-stop-probe", "library-drop-stop-probe", "bees-drop-stop-probe"):
                active.check(suite)
                completed.append({"match": match, "suite": suite})
            cursor = active.send("sw stop")
            active.expect("LOADING -> LOBBY", cursor)
            active.check("bees-drop-cleanup")
            completed.append({"match": match, "suite": "bees-drop-cleanup"})
            active.check("library-drop-cleanup")
            completed.append({"match": match, "suite": "library-drop-cleanup"})
            active.check("enzan-drop-cleanup")
            completed.append({"match": match, "suite": "enzan-drop-cleanup"})
            active.check("lucky-pigs-cleanup")
            completed.append({"match": match, "suite": "lucky-pigs-cleanup"})
            active.check("scare-drops-cleanup")
            completed.append({"match": match, "suite": "scare-drops-cleanup"})
            active.check("home-drop-cleanup")
            completed.append({"match": match, "suite": "home-drop-cleanup"})
            active.check("falling-netherite-drops-cleanup")
            completed.append({"match": match, "suite": "falling-netherite-drops-cleanup"})
            active.check("zombie-tnt-drops-cleanup")
            completed.append({"match": match, "suite": "zombie-tnt-drops-cleanup"})
            active.check("corrosion-drops-cleanup")
            completed.append({"match": match, "suite": "corrosion-drops-cleanup"})
            active.check("hole-drops-cleanup")
            completed.append({"match": match, "suite": "hole-drops-cleanup"})
            active.check("cage-drops-cleanup")
            completed.append({"match": match, "suite": "cage-drops-cleanup"})
            active.check("creeper-drops-cleanup")
            completed.append({"match": match, "suite": "creeper-drops-cleanup"})
            active.check("tnt-drops-cleanup")
            completed.append({"match": match, "suite": "tnt-drops-cleanup"})
            active.check("chest-drops-cleanup")
            completed.append({"match": match, "suite": "chest-drops-cleanup"})
        success = True
        print("BEES DROP CHECKS PASSED", flush=True)
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
        attempt(lambda: (cycle.OUT / "bees-drop-results.json").write_text(json.dumps({
            "success": success and not cleanup_errors,
            "scope": "native-mode1-source-bees-drop-fixtures",
            "completed": completed, "artifactSha256": tested_sha256,
            "sourceArtifactSha256": source_sha256, "serverDirectory": str(allowed),
            "serverPort": 25576,
            "cleanupErrors": [str(error) for error in cleanup_errors],
            "measurementLimit": "Elapsed wall time includes boot, map loading and regressions, not per-tick CPU or worst-case entity latency. Real client rendering and recipient perception, actual disk restart/chunk-unload persistence and saturation are not qualified. Immediate native bees prove fractional origins/defaults. Native hive entry/emergency exit and item-component transfer are exercised, not natural pathfinding or disk persistence. Native sting/fall/void deaths use disposable mobs; poison itself remains natively nonlethal. Lifecycle unload guards use service callbacks. Elapsed time includes prior special-drop regressions and is not a per-tick CPU benchmark.",
            "elapsedSeconds": round(time.monotonic() - started, 2)
        }, indent=2) + "\n", encoding="utf-8"))
        if original_error is None and cleanup_errors:
            raise cleanup_errors[0]



if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--server-dir", type=Path, required=True)
    run(parser.parse_args().server_dir)
