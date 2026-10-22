"""Run native lightning policy and typed-item regressions in two isolated matches."""
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
        raise ValueError("Lightning rule checks require build/verification/key-reuse-server")
    cycle.SERVER = allowed
    properties = dict(line.split("=", 1) for line in (allowed / "server.properties").read_text(
        encoding="utf-8").splitlines() if line and not line.startswith("#") and "=" in line)
    if properties.get("server-port") != "25576":
        raise ValueError("Lightning rule isolated server must use port 25576")
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
    source_sha256 = hashlib.sha256(artifact.read_bytes()).hexdigest()
    shutil.copyfile(artifact, installed_jar)
    tested_sha256 = hashlib.sha256(installed_jar.read_bytes()).hexdigest()
    assert tested_sha256 == source_sha256, "Isolated installed JAR differs from source"
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
                        lbcLevelups=True, lbcLevelupLimit=4, mapId="1", broken=False)
        settings_path.parent.mkdir(parents=True, exist_ok=True)
        settings_path.write_text(json.dumps(settings, indent=2) + "\n", encoding="utf-8")

        active = cycle.Server("lightning-rules")
        active.expect("LOADING -> LOBBY", timeout=180)
        active.expect("Selected map ready: 1", timeout=180)
        active.check("bots")
        for match in (1, 2):
            if match > 1:
                active.prepare_map(1)
            cursor = active.send("sw start")
            active.expect("-> PLAYING", cursor)
            for name in ("lightning-rules", "lightning-rules-chain", "typed-items",
                         "boltbolt", "infbolt-egg", "electro-gear", "electric-damage",
                         "kfc-cultus", "mobgun"):
                active.check(name)
                completed.append({"match": match, "check": name})
            active.check("boltbolt-stop-probe")
            active.check("infbolt-stop-probe")
            active.check("mobgun-stop-probe")
            cursor = active.send("sw stop")
            active.expect("LOADING -> LOBBY", cursor)
            for name in ("boltbolt-cleanup", "infbolt-cleanup", "mobgun-cleanup", "electric-damage-cleanup"):
                active.check(name)
                completed.append({"match": match, "check": name})
        success = True
        print("LIGHTNING RULE CHECKS PASSED", flush=True)
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
            (cycle.OUT / "lightning-rules-results.json").write_text(
                json.dumps({"success": success, "completed": completed, "artifactSha256": tested_sha256,
                            "sourceArtifactSha256": source_sha256,
                            "elapsedSeconds": round(time.monotonic() - started, 2)},
                           indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--server-dir", type=Path, required=True,
                        help="Isolated Fabric server directory; the main server is refused")
    run(parser.parse_args().server_dir)
