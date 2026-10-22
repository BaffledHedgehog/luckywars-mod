"""Qualify LuckyWars category ownership/settings in the fixed isolated server."""
from pathlib import Path
import argparse
import hashlib
import json
import shutil
import time

import smoke_game_cycle as cycle


def run(server_dir: Path, settings_only: bool = False) -> None:
    allowed = (cycle.PROJECT / "build/verification/key-reuse-server").resolve()
    if server_dir.resolve() != allowed:
        raise ValueError("Lucky modifiers only run in build/verification/key-reuse-server")
    cycle.SERVER = allowed
    properties = dict(line.split("=", 1) for line in (allowed / "server.properties").read_text(
        encoding="utf-8").splitlines() if line and not line.startswith("#") and "=" in line)
    if properties.get("server-port") != "25576":
        raise ValueError("Lucky modifiers isolated server must use port25576")
    if not (allowed / "fabric-server-launch.jar").is_file():
        raise FileNotFoundError("Isolated Fabric launcher is absent")
    cycle.require_stopped(allowed)
    world = (allowed / properties.get("level-name", "world")).resolve()
    world.relative_to(allowed)
    settings_path = allowed / "config/luckywars/game.json"
    profiles_path = allowed / "config/luckywars/players.json"
    # Restore the exact persisted global scores as well as configuration/profiles.
    paths = (settings_path, profiles_path, world / "data/scoreboard.dat",
             world / "data/scoreboard.dat_old")
    backups = {path: path.read_bytes() if path.exists() else None for path in paths}
    artifact = cycle.PROJECT / "build/libs/skywars2-0.1.0.jar"
    source_sha256 = hashlib.sha256(artifact.read_bytes()).hexdigest()
    installed = allowed / "mods/skywars2.jar"
    installed.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(artifact, installed)
    tested_sha256 = hashlib.sha256(installed.read_bytes()).hexdigest()
    if source_sha256 != tested_sha256:
        raise AssertionError("Installed artifact differs from source artifact")
    active = None
    completed = []
    started = time.monotonic()
    success = False
    try:
        settings = json.loads(backups[settings_path] or b"{}")
        settings.update(lives=2, health=20, day=1, gameMode=1, luckyModifier=7,
                        countdownSeconds=1, stageOneSeconds=600, stageTwoSeconds=1200,
                        endingSeconds=1, stageOneEvent=0, stageTwoEvent=0,
                        vanillaKits=False, vanillaSkills=False, lbcKits=True,
                        lbcSkills=True, lbcLevelups=True, lbcLevelupLimit=4,
                        mapId="1", broken=False, noBorders=True)
        settings_path.parent.mkdir(parents=True, exist_ok=True)
        settings_path.write_text(json.dumps(settings, indent=2) + "\n", encoding="utf-8")
        active = cycle.Server("lucky-modifiers")
        active.expect("LOADING -> LOBBY", timeout=180)
        active.expect("Selected map ready: 1", timeout=180)
        active.check("bots")
        active.check("lucky-modifiers-menu")
        completed.append({"phase": "lobby", "suite": "lucky-modifiers-menu"})
        active.check("lucky-modifiers-conditions")
        completed.append({"phase": "lobby", "suite": "lucky-modifiers-conditions"})
        for match in (() if settings_only else (1, 2)):
            if match == 2:
                active.prepare_map(1)
                cursor = active.send("settings luckymodifier 0")
                active.expect(cycle.console_text("settings.saved", "luckyModifier", 0), cursor)
                if json.loads(settings_path.read_text(encoding="utf-8"))["luckyModifier"] != 0:
                    raise AssertionError("Second normal match must retain configured random0")
            active.check("lucky-modifiers-snapshot")
            completed.append({"match": match, "suite": "lucky-modifiers-snapshot"})
            cursor = active.send("sw start")
            active.expect("-> PLAYING", cursor)
            for suite in ("lucky-modifiers", "shooter-pools", "mobgun", "lightning-acquisition"):
                active.check(suite)
                completed.append({"match": match, "suite": suite})
            cursor = active.send("sw stop")
            active.expect("LOADING -> LOBBY", cursor)
            active.check("lucky-modifiers-cleanup")
            completed.append({"match": match, "suite": "lucky-modifiers-cleanup"})
        success = True
        print("LUCKY MODIFIER CHECKS PASSED", flush=True)
    finally:
        try:
            if active is not None:
                active.stop()
        finally:
            cycle.require_stopped(allowed)
            for path, original in backups.items():
                if original is None:
                    path.unlink(missing_ok=True)
                else:
                    path.parent.mkdir(parents=True, exist_ok=True)
                    path.write_bytes(original)
            (cycle.OUT / "lucky-modifiers-results.json").write_text(json.dumps({
                "success": success,
                "scope": ("lobby settings/dispatcher/JSON/codec conditions, exhaustive resolver and simulated LuckyWars service branch; no real match lifecycle qualification"
                          if settings_only else "settings/dispatcher/codec conditions, simulated LuckyWars service branch and two real ordinary start/stop retention cycles"),
                "settingsOnly": settings_only,
                "policy": "resolve only at committed LuckyWars start; retain resolved score and GUI configuration across ordinary starts/ends",
                "completed": completed, "artifactSha256": tested_sha256,
                "sourceArtifactSha256": source_sha256, "serverDirectory": str(allowed),
                "serverPort": 25576, "elapsedSeconds": round(time.monotonic() - started, 2),
            }, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--server-dir", type=Path, required=True)
    parser.add_argument("--settings-only", action="store_true",
                        help="Qualify lobby settings and source conditions without starting matches")
    args = parser.parse_args()
    run(args.server_dir, settings_only=args.settings_only)
