"""Ten visual, weapon, terrain and time-stop corrections with shared native regressions."""
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


def run(server_dir: Path, focus: list[str] | None = None, keep_going: bool = False) -> None:
    if keep_going and not focus:
        raise ValueError("--keep-going requires explicit --focus suites")
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
    lock = allowed / "codex-visual-contact.lock"
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
        active = cycle.Server("visual-contact")
        active.expect("LOADING -> LOBBY", timeout=180)
        active.expect("Selected map ready: 1", timeout=180)
        for suite in ("bots", "loot-test-catalog"):
            active.check(suite)
        for match in ((1,) if focus else (1, 2)):
            if match == 2:
                active.prepare_map(1)
            for suite in ("kfc-menu", "frost-menu", "slr-menu"):
                active.check(suite)
                completed.append({"match": match, "suite": suite})
            cursor = active.send("sw start")
            active.expect("-> PLAYING", cursor)
            for suite in (focus or ("academy-single-use", "kiuas-support", "kiuas", "minigun", "minigun-visual", "rifle-zoom", "vex-family", "special-eggs", "ultraponos-drops", "time-stop-contacts", "time-stop", "time-stop-schedules", "attribute-policy", "armor-calculation", "negative-attributes", "negative-mining", "chain-lightning-fixes", "egg-presentation", "antigravity-drops", "encounter-eggs", "creature-drops", "lightning-rules", "lightning-rules-chain", "player-rules", "team-colors", "blood-confuse", "evoker-bass", "slr", "soulhunter", "halloween-wind", "fartus", "chef", "babylon", "defensive-arcanums", "void-arcanum", "neutral-crafts", "ice-wand", "ice-materials", "corruption", "corrupted-glyphs", "erecto", "tesla", "awakened-acid", "gaycaster", "prism", "caustic-omega", "advanced-snow", "neutral-wand", "mobgun", "academy-acquisition", "kfc-counters")):
                try:
                    active.check(suite)
                except AssertionError as error:
                    if not keep_going:
                        raise
                    failures.append({"match": match, "suite": suite, "error": str(error)})
                    continue
                completed.append({"match": match, "suite": suite})
            for probe in (() if focus else ("team-colors-stop-probe", "soulhunter-stop-probe", "blood-confuse-stop-probe", "evoker-bass-stop-probe", "slr-stop-probe", "corruption-stop-probe", "ice-wand-stop-probe", "void-arcanum-stop-probe", "defensive-arcanums-stop-probe", "babylon-stop-probe", "fartus-stop-probe", "chef-stop-probe", "halloween-wind-stop-probe", "antigravity-drops-stop-probe", "encounter-eggs-stop-probe", "creature-drops-stop-probe", "academy-single-use-stop-probe", "rifle-zoom-stop-probe", "time-stop-contacts-stop-probe")):
                active.check(probe)
                completed.append({"match": match, "suite": probe})
            cursor = active.send("sw stop")
            active.expect("LOADING -> LOBBY", cursor)
            cleanup = "babylon-cleanup"
            for suite in (() if focus else ("team-colors-cleanup", "blood-confuse-cleanup", "evoker-bass-cleanup", "slr-cleanup", "soulhunter-cleanup", "halloween-wind-cleanup", "chef-cleanup","fartus-cleanup",cleanup,"defensive-arcanums-cleanup","void-arcanum-cleanup","ice-wand-cleanup","corruption-cleanup", "antigravity-drops-cleanup", "encounter-eggs-cleanup", "creature-drops-cleanup", "academy-single-use-cleanup", "rifle-zoom-cleanup", "time-stop-contacts-cleanup")):
                active.check(suite)
                completed.append({"match": match, "suite": suite})
        if failures:
            raise AssertionError(f"Focused suites failed: {failures}")
        success = True
        print("VISUAL CONTACT CHECKS PASSED", flush=True)
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
        attempt(lambda: (cycle.OUT / "visual-contact-results.json").write_text(json.dumps({
            "success": success and not cleanup_errors,
            "scope": "native-mode1-visual-contact-family-and-regressions",
            "completed": completed, "checkFailures": failures, "artifactSha256": tested_sha256,
            "sourceArtifactSha256": source_sha256, "serverDirectory": str(allowed),
            "serverPort": 25576,
            "cleanupErrors": [str(error) for error in cleanup_errors],
            "measurementLimit": "Native synchronous fixtures and actual match stops. Real client pixels, full natural physics, actual disk restart/unload/disconnect and peak-capacity performance are not qualified. Elapsed wall time includes boot, map loading and regression checks.",
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
