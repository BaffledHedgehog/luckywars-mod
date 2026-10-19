"""Exercise native skill hooks on an isolated, stopped server; restore its settings."""
from pathlib import Path
import argparse
import hashlib
import json
import shutil
import time
import smoke_game_cycle as cycle


def run(server_dir):
    cycle.SERVER = server_dir.resolve()
    if cycle.SERVER == cycle.PROJECT.parent:
        raise ValueError("Use an isolated verification server")
    files = [cycle.SERVER / "config/luckywars" / name for name in ("game.json", "players.json")]
    backups = {path: path.read_bytes() if path.exists() else None for path in files}
    artifact = cycle.PROJECT / "build/libs/skywars2-0.1.0.jar"
    shutil.copyfile(artifact, cycle.SERVER / "mods/skywars2.jar")
    settings = json.loads(backups[files[0]] or b"{}")
    settings.update(lives=2, health=60, countdownSeconds=1, stageOneSeconds=600,
                    stageTwoSeconds=1200, endingSeconds=1, stageOneEvent=0,
                    stageTwoEvent=0, vanillaKits=False, vanillaSkills=True, ranked=True,
                    mapId="1", broken=False)
    files[0].write_text(json.dumps(settings, indent=2) + "\n", encoding="utf-8")
    active = None
    completed = []
    started = time.monotonic()
    try:
        active = cycle.Server("vanilla-skills")
        active.expect("LOADING -> LOBBY")
        active.expect("Selected map ready: 1", timeout=180)
        active.check("bots")
        active.check("lobby")
        cursor = active.send("sw start")
        active.expect("-> PLAYING", cursor)
        active.check("skills")
        completed.append("native fall/food/damage hooks, Vex targeting and death/respawn cleanup")
        cursor = active.check("skills-stop")
        active.expect("LOADING -> LOBBY", cursor)
        active.prepare_map(1)
        active.check("skills-cleanup")
        completed.append("match cleanup")
        active.stop()
        active = None
        print("VANILLA SKILL CHECKS PASSED", flush=True)
    finally:
        if active is not None:
            active.stop()
        for path, original in backups.items():
            if original is None:
                path.unlink(missing_ok=True)
            else:
                path.write_bytes(original)
        (cycle.OUT / "vanilla-skills-results.json").write_text(json.dumps({
            "completed": completed, "artifactSha256": hashlib.sha256(artifact.read_bytes()).hexdigest(),
            "elapsedSeconds": round(time.monotonic() - started, 2)
        }, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--server-dir", type=Path, required=True)
    run(parser.parse_args().server_dir)
