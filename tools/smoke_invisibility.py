"""Run the full invisibility Polymer checks on an isolated, stopped Fabric server."""
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
        raise FileNotFoundError(f"Build the mod artifact first: {artifact}")
    installed_jar = cycle.SERVER / "mods/skywars2.jar"
    installed_jar.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(artifact, installed_jar)
    artifact_sha256 = hashlib.sha256(installed_jar.read_bytes()).hexdigest()

    active = None
    started = time.monotonic()
    completed = []
    result_path = cycle.OUT / "full-invisibility-results.json"
    try:
        settings = json.loads(backups[settings_path] or b"{}")
        settings.update(lives=2, health=20, day=1, gameMode=1, countdownSeconds=1,
                        stageOneSeconds=600, stageTwoSeconds=1200, endingSeconds=1,
                        stageOneEvent=0, stageTwoEvent=0, vanillaKits=False,
                        vanillaSkills=False, lbcKits=True, lbcSkills=True,
                        lbcLevelups=True, lbcLevelupLimit=4, mapId="1", broken=False)
        settings_path.parent.mkdir(parents=True, exist_ok=True)
        settings_path.write_text(json.dumps(settings, indent=2) + "\n", encoding="utf-8")

        active = cycle.Server("full-invisibility")
        active.expect("LOADING -> LOBBY", timeout=180)
        active.expect("Selected map ready: 1", timeout=180)
        active.check("bots")
        cursor = active.send("sw start")
        active.expect("-> PLAYING", cursor)
        # GameChecks waits through native effect ticks before announcing PASS.
        active.check("full-invisibility")
        completed.append(
            "Viewer-specific Interaction proxy, native player bounds, mount codecs and melee/projectile/other damage; "
            "effect downgrade, clear and expiry; pickup codec for self/external/camera viewers and actual inventory pickup"
        )

        # Existing spectator suite: native RMB camera, copied inventory/effects and detach
        # must still work when the observed live player is fully invisible to outsiders.
        active.send("effect give TestAlpha minecraft:invisibility 600 2 true")
        active.check("observer-match")
        completed.append("Existing spectator lifecycle with level III target: native camera, inventory/effects and detach")

        cursor = active.send("sw stop")
        active.expect("LOADING -> LOBBY", cursor)
        active.prepare_map(1)
        active.check("cleanup")
        active.stop()
        active = None
        print("FULL INVISIBILITY CHECKS PASSED", flush=True)
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
            result_path.write_text(
                json.dumps({"completed": completed, "artifactSha256": artifact_sha256,
                            "elapsedSeconds": round(time.monotonic() - started, 2)},
                           indent=2) + "\n",
                encoding="utf-8",
            )


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--server-dir", type=Path, required=True,
                        help="Isolated Fabric server directory; the main server is refused")
    args = parser.parse_args()
    run(args.server_dir)
