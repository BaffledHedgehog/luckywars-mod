"""Check unfinished Academy progress across player/server sessions and reward lifecycle."""
from pathlib import Path
import argparse
import hashlib
import json
import shutil
import time
import smoke_game_cycle as cycle


def run(server_dir: Path) -> None:
    cycle.SERVER = server_dir.resolve()
    if cycle.SERVER == cycle.PROJECT.parent:
        raise ValueError("Use an isolated verification server")
    cycle.require_stopped(cycle.SERVER)
    launcher = cycle.SERVER / "fabric-server-launch.jar"
    if not launcher.is_file():
        raise FileNotFoundError(launcher)
    files = [cycle.SERVER / "config/luckywars" / name
             for name in ("game.json", "players.json")]
    backups = {path: path.read_bytes() if path.exists() else None for path in files}
    artifact = cycle.PROJECT / "build/libs/skywars2-0.1.0.jar"
    installed = cycle.SERVER / "mods/skywars2.jar"
    installed.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(artifact, installed)
    tested_sha256 = hashlib.sha256(installed.read_bytes()).hexdigest()
    active = None
    completed = []
    started = time.monotonic()
    passed = False
    try:
        settings = json.loads(backups[files[0]] or b"{}")
        settings.update(lives=3, health=20, day=1, gameMode=1, countdownSeconds=1,
                        stageOneSeconds=600, stageTwoSeconds=1200, endingSeconds=1,
                        stageOneEvent=0, stageTwoEvent=0, vanillaKits=False,
                        vanillaSkills=False, lbcKits=False, lbcSkills=False,
                        lbcLevelups=False, lbcChallenges=True, ranked=False,
                        mapId="1", broken=False)
        files[0].parent.mkdir(parents=True, exist_ok=True)
        files[0].write_text(json.dumps(settings, indent=2) + "\n", encoding="utf-8")
        active = cycle.Server("academy-challenge")
        active.expect("LOADING -> LOBBY", timeout=180)
        active.expect("Selected map ready: 1", timeout=180)
        active.check("bots")
        active.check("academy-challenge-menu")
        completed.append("Native GUI selection gates, separate resets, ranked access and challenge toggle")
        cursor = active.send("sw start")
        active.expect("-> PLAYING", cursor)
        active.check("academy-challenge-projection")
        completed.append("Vanilla/native Polymer names and lore hide for one viewer, preserve authoritative stacks and reveal on clear")
        active.check("academy-challenge-partial")
        completed.append("Seven real NPC promotions persist unfinished active challenge progress immediately")
        cursor = active.send("sw stop")
        active.expect("LOADING -> LOBBY", cursor)
        active.prepare_map(1)
        active.check("academy-challenge-rejoin")
        completed.append("Native disconnect/rejoin and repeated GUI selection retain active challenge and seven promotions")
        active.check("academy-cleanup")
        active.stop()
        active = None

        cycle.require_stopped(cycle.SERVER)
        active = cycle.Server("academy-challenge-partial-restart")
        active.expect("LOADING -> LOBBY", timeout=180)
        active.expect("Selected map ready: 1", timeout=180)
        active.check("bots")
        active.check("academy-challenge-partial-persisted")
        completed.append("Full server restart retains unfinished activity, seven promotions and unselected reward")
        cursor = active.send("sw start")
        active.expect("-> PLAYING", cursor)
        active.check("academy-challenge-projection")
        active.check("academy-challenge-resume")
        completed.append("Thirteen further native NPC promotions resume seven-to-twenty and complete exactly once without selecting reward")
        cursor = active.send("sw stop")
        active.expect("LOADING -> LOBBY", cursor)
        active.prepare_map(1)
        active.check("academy-challenge-select-reward")
        active.check("academy-cleanup")
        active.stop()
        active = None

        cycle.require_stopped(cycle.SERVER)
        active = cycle.Server("academy-challenge-restart")
        active.expect("LOADING -> LOBBY", timeout=180)
        active.expect("Selected map ready: 1", timeout=180)
        active.check("bots")
        active.check("academy-challenge-persisted")
        completed.append("Native completion and independent reward selection survive a full server restart")
        cursor = active.send("sw start")
        active.expect("-> PLAYING", cursor)
        active.check("academy-challenge-reward")
        completed.append("Actual start and native death/respawn grant one Academy token independently of normal kits")
        cursor = active.send("sw stop")
        active.expect("LOADING -> LOBBY", cursor)
        active.prepare_map(1)
        active.check("academy-cleanup")
        active.stop()
        active = None
        passed = True
    finally:
        try:
            if active is not None:
                active.stop()
        finally:
            for path, original in backups.items():
                if original is None:
                    path.unlink(missing_ok=True)
                else:
                    path.write_bytes(original)
            (cycle.OUT / "academy-challenge-results.json").write_text(
                json.dumps({"passed": passed, "completed": completed,
                            "artifactSha256": tested_sha256,
                            "elapsedSeconds": round(time.monotonic() - started, 2)},
                           indent=2) + "\n", encoding="utf-8")
    print(f"ACADEMY CHALLENGE CHECKS PASSED ({time.monotonic() - started:.1f}s)", flush=True)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--server-dir", type=Path, required=True)
    run(parser.parse_args().server_dir)
