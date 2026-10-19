"""Verify Iron Axe editor overlays, chest refill, and container cleanup on an isolated server."""
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
    files = [cycle.SERVER / "config/luckywars" / name for name in ("game.json", "players.json")]
    backups = {p: p.read_bytes() if p.exists() else None for p in files}
    jar = cycle.PROJECT / "build/libs/skywars2-0.1.0.jar"
    shutil.copyfile(jar, cycle.SERVER / "mods/skywars2.jar")
    digest = hashlib.sha256(jar.read_bytes()).hexdigest()
    active = None
    completed = []
    started = time.monotonic()
    try:
        settings = json.loads(backups[files[0]] or b"{}")
        settings.update(mapId="35", broken=False, gameMode=1, lives=2, health=20,
                        countdownSeconds=1, stageOneSeconds=600, stageTwoSeconds=1200,
                        endingSeconds=1, stageOneEvent=0, stageTwoEvent=0,
                        vanillaKits=False, vanillaSkills=False, lbcKits=False,
                        lbcSkills=False, lbcLevelups=False)
        files[0].write_text(json.dumps(settings, indent=2) + "\n", encoding="utf-8")
        active = cycle.Server("map-markers")
        active.expect("LOADING -> LOBBY", timeout=180)
        active.expect("Selected map ready: 35", timeout=180)
        active.check("bots")
        active.check("map-editor-cleanup")
        completed.append("Iron Axe retains 29 anchors and 20 filled chests, excludes editor overlays, and refills without drops")
        active.prepare_map(1)
        active.check("map-no-ground-items")
        completed.append("Replacing a filled map with block/entity drops enabled does not spill container contents")
        active.prepare_map(35)
        active.check("map-editor-cleanup")
        cursor = active.send("sw start")
        active.expect("-> PLAYING", cursor)
        active.check("chests")
        active.check("map-no-ground-items")
        completed.append("Iron Axe reload and actual match start keep native loot inside containers")
        cursor = active.send("sw stop")
        active.expect("LOADING -> LOBBY", cursor)
        active.stop()
        active = None
        print("MAP MARKER CHECKS PASSED", flush=True)
    finally:
        try:
            if active is not None:
                active.stop()
        finally:
            for p, original in backups.items():
                if original is None:
                    p.unlink(missing_ok=True)
                else:
                    p.write_bytes(original)
            (cycle.OUT / "map-markers-results.json").write_text(json.dumps({
                "completed": completed, "artifactSha256": digest,
                "elapsedSeconds": round(time.monotonic() - started, 2),
            }, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--server-dir", type=Path, required=True)
    run(parser.parse_args().server_dir)
