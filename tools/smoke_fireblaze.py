"""Run native Fireblaze and Fire Wand checks on an isolated server copy."""
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

        active = cycle.Server("fireblaze")
        active.expect("LOADING -> LOBBY", timeout=180)
        active.expect("Selected map ready: 1", timeout=180)
        active.check("bots")
        cursor = active.send("sw start")
        active.expect("-> PLAYING", cursor)
        active.check("fireblaze-menu")
        completed.append("Blaze Wand and Fireblaze source loot identity, exact model/name/lore, and Polymer proxy")
        active.check("fireblaze-crafting")
        completed.append("Native Fire/Academy Fire/legacy stfire + native/legacy Blaze Wand exact 2x2/3x3 recipe")
        active.check("fireblaze-wand")
        completed.append("Native Fireblaze marker, projectile and terrain runtime checks")
        active.check("fire-wand")
        completed.append("Fire Wand real-tick regression")
        cursor = active.send("sw stop")
        active.expect("LOADING -> LOBBY", cursor)
        active.check("fireblaze-cleanup")
        active.check("fire-wand-cleanup")
        active.prepare_map(1)
        cursor = active.send("sw start")
        active.expect("-> PLAYING", cursor)
        active.check("fireblaze-wand")
        completed.append("Fireblaze runtime checks resume in the second match")
        cursor = active.send("sw stop")
        active.expect("LOADING -> LOBBY", cursor)
        active.check("fireblaze-cleanup")
        active.check("fire-wand-cleanup")
        print("FIREBLAZE CHECKS PASSED", flush=True)
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
            (cycle.OUT / "fireblaze-results.json").write_text(
                json.dumps({"completed": completed, "artifactSha256": tested_sha256,
                            "elapsedSeconds": round(time.monotonic() - started, 2)},
                           indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--server-dir", type=Path, required=True,
                        help="Isolated Fabric server directory; the main server is refused")
    run(parser.parse_args().server_dir)
