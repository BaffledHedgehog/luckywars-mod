"""Run bounded Stonekivi and Tannerkivi checks on an isolated server copy."""
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
        raise ValueError("--server-dir must name an isolated server directory")
    launcher = cycle.SERVER / "fabric-server-launch.jar"
    if not launcher.is_file():
        raise FileNotFoundError(f"Isolated server has no {launcher.name}: {cycle.SERVER}")
    cycle.require_stopped(cycle.SERVER)

    settings_path = cycle.SERVER / "config/luckywars/game.json"
    profiles_path = cycle.SERVER / "config/luckywars/players.json"
    backups = {path: path.read_bytes() if path.exists() else None
               for path in (settings_path, profiles_path)}
    artifact = cycle.PROJECT / "build/libs/skywars2-0.1.0.jar"
    if not artifact.is_file():
        raise FileNotFoundError(f"Build the released project artifact before running smoke checks: {artifact}")
    installed_jar = cycle.SERVER / "mods/skywars2.jar"
    installed_jar.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(artifact, installed_jar)
    tested_sha256 = hashlib.sha256(installed_jar.read_bytes()).hexdigest()
    active = None
    started = time.monotonic()
    completed = []
    try:
        settings = json.loads(backups[settings_path] or b"{}")
        settings.update(lives=2, health=20, day=1, gameMode=1, countdownSeconds=1,
                        stageOneSeconds=600, stageTwoSeconds=1200, endingSeconds=1,
                        stageOneEvent=0, stageTwoEvent=0, vanillaKits=False,
                        vanillaSkills=False, lbcKits=True, lbcSkills=True,
                        lbcLevelups=True, lbcLevelupLimit=4, mapId="1", broken=False)
        settings_path.parent.mkdir(parents=True, exist_ok=True)
        settings_path.write_text(json.dumps(settings, indent=2) + "\n", encoding="utf-8")

        active = cycle.Server("stonekivi")
        active.expect("LOADING -> LOBBY", timeout=180)
        active.expect("Selected map ready: 1", timeout=180)
        active.check("bots")
        active.check("stonekivi-menu")
        completed.append("Stonekivi and actual Tannerkivi loot payloads, lore/colors/tier/model/flags and Polymer projections")

        cursor = active.send("sw start")
        active.expect("-> PLAYING", cursor)
        active.check("stonekivi-crafting")
        completed.append("Native/Academy/legacy crafting in 2x2 and 3x3 grids, exact ingredients, preview and take")
        active.check("stonekivi-wand")
        completed.append("2499/2500 mana gate, one queued normal hit, bounded source templates, used-offhand four-mode cycle, Earth queue drain, and Comet item/allied-mob targets")
        active.check("stonekivi-sun-stream")
        completed.append("Two concurrent paid Sun casts make first-turn progressive writes; cached competing edits survive, filled containers produce no drops, bounded spikes do not expand the flood, each area receives one 40-damage impact, and stop cancels retained work")
        active.check("stonekivi-sun-start")
        time.sleep(3)
        active.check("stonekivi-sun-finish")
        completed.append("Real-tick progressive Sun lava/magma writes, diagonal connectivity and attributed surface damage/Slowness V")
        active.check("tannerkivi")
        completed.append("Automatic 499/500/515 activation thresholds, block and Arrow conversion, exclusion marker, dual 1030 activation, 30-per-tick upkeep, and paid-work drain after aura stop")
        active.check("tannerkivi-movement")
        completed.append("Dense 31-cube, 32 single-block plus eight four-block moves, exact pending-mask oracle on three axes, 256 undrained shifts, dirt/no-op event dedup and paid dirty work after hand removal")

        cursor = active.send("sw stop")
        active.expect("LOADING -> LOBBY", cursor)
        active.check("stonekivi-cleanup")
        active.check("tannerkivi-cleanup")
        active.prepare_map(1)
        cursor = active.send("sw start")
        active.expect("-> PLAYING", cursor)
        active.check("tannerkivi")
        completed.append("Tannerkivi reactivates and drains paid work in a second match after lifecycle reset")
        cursor = active.send("sw stop")
        active.expect("LOADING -> LOBBY", cursor)
        active.check("tannerkivi-cleanup")
        print("STONEKIVI CHECKS PASSED", flush=True)
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
            (cycle.OUT / "stonekivi-results.json").write_text(
                json.dumps({"completed": completed, "artifactSha256": tested_sha256,
                            "elapsedSeconds": round(time.monotonic() - started, 2)},
                           indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--server-dir", type=Path, required=True,
                        help="Isolated Fabric server directory; the main server is refused")
    run(parser.parse_args().server_dir)
