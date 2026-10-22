"""Run Woodificator checks in the fixed isolated server, across two matches."""
from pathlib import Path
import argparse
import hashlib
import json
import shutil
import time

import smoke_game_cycle as cycle


def run(server_dir: Path) -> None:
    allowed = (cycle.PROJECT / "build/verification/key-reuse-server").resolve()
    if server_dir.resolve() != allowed:
        raise ValueError("Woodificator only runs in build/verification/key-reuse-server")
    cycle.SERVER = allowed
    properties = dict(line.split("=", 1) for line in (allowed / "server.properties").read_text(
        encoding="utf-8").splitlines() if line and not line.startswith("#") and "=" in line)
    if properties.get("server-port") != "25576":
        raise ValueError("Woodificator isolated server must use port 25576")
    if not (allowed / "fabric-server-launch.jar").is_file():
        raise FileNotFoundError("Isolated Fabric launcher is absent")
    cycle.require_stopped(allowed)
    settings_path = allowed / "config/luckywars/game.json"
    profiles_path = allowed / "config/luckywars/players.json"
    backups = {path: path.read_bytes() if path.exists() else None
               for path in (settings_path, profiles_path)}
    artifact = cycle.PROJECT / "build/libs/skywars2-0.1.0.jar"
    source_sha256 = hashlib.sha256(artifact.read_bytes()).hexdigest()
    installed = allowed / "mods/skywars2.jar"
    installed.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(artifact, installed)
    tested_sha256 = hashlib.sha256(installed.read_bytes()).hexdigest()
    if source_sha256 != tested_sha256:
        raise AssertionError("Installed artifact differs from source artifact")
    active = None
    started = time.monotonic()
    completed = []
    success = False
    try:
        settings = json.loads(backups[settings_path] or b"{}")
        settings.update(lives=2, health=20, day=1, gameMode=1, countdownSeconds=1,
                        stageOneSeconds=600, stageTwoSeconds=1200, endingSeconds=1,
                        stageOneEvent=0, stageTwoEvent=0, vanillaKits=False,
                        vanillaSkills=False, lbcKits=True, lbcSkills=True,
                        lbcLevelups=True, lbcLevelupLimit=4, mapId="1", broken=False, noBorders=True)
        settings_path.parent.mkdir(parents=True, exist_ok=True)
        settings_path.write_text(json.dumps(settings, indent=2) + "\n", encoding="utf-8")
        active = cycle.Server("woodificator")
        active.expect("LOADING -> LOBBY", timeout=180)
        active.expect("Selected map ready: 1", timeout=180)
        active.check("bots")
        for match in (1, 2):
            if match == 2:
                active.prepare_map(1)
            cursor = active.send("sw start")
            active.expect("-> PLAYING", cursor)
            for suite in ("woodificator-menu", "woodificator", "woodificator-stop-probe"):
                active.check(suite)
                completed.append({"match": match, "suite": suite})
            cursor = active.send("sw stop")
            active.expect("LOADING -> LOBBY", cursor)
            active.check("woodificator-cleanup")
            completed.append({"match": match, "suite": "woodificator-cleanup"})
        success = True
        print("WOODIFICATOR CHECKS PASSED", flush=True)
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
            (cycle.OUT / "woodificator-results.json").write_text(json.dumps({
                "success": success, "completed": completed, "artifactSha256": tested_sha256,
                "sourceArtifactSha256": source_sha256, "serverDirectory": str(allowed),
                "serverPort": 25576, "elapsedSeconds": round(time.monotonic() - started, 2)
            }, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--server-dir", type=Path, required=True)
    run(parser.parse_args().server_dir)
