"""Run native Duplicating and Divider checks on an isolated server copy."""
from pathlib import Path
import argparse
import hashlib
import json
import shutil
import time

import smoke_game_cycle as cycle


def run(server_dir: Path, performance_only: bool = False) -> None:
    allowed = (cycle.PROJECT / "build/verification/key-reuse-server").resolve()
    if server_dir.resolve() != allowed:
        raise ValueError("Duplicating only runs in build/verification/key-reuse-server")
    cycle.SERVER = allowed
    properties = dict(line.split("=", 1) for line in (allowed / "server.properties").read_text(
        encoding="utf-8").splitlines() if line and not line.startswith("#") and "=" in line)
    if properties.get("server-port") != "25576":
        raise ValueError("Duplicating isolated server must use port 25576")
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
    source_sha256 = hashlib.sha256(artifact.read_bytes()).hexdigest()
    installed_jar = cycle.SERVER / "mods/skywars2.jar"
    installed_jar.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(artifact, installed_jar)
    tested_sha256 = hashlib.sha256(installed_jar.read_bytes()).hexdigest()
    if tested_sha256 != source_sha256:
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

        active = cycle.Server("duplicating")
        active.expect("LOADING -> LOBBY", timeout=180)
        active.expect("Selected map ready: 1", timeout=180)
        active.check("bots")
        suites = ("duplicating-performance",) if performance_only else (
            "duplicating-menu", "duplicating-crafting", "duplicating", "divider",
            "duplicating-academy", "duplicating-expand", "duplicating-trial",
            "duplicating-performance", "duplicating-coalescing", "consuming-academy")
        for match in range(1, 2 if performance_only else 3):
            if match == 2:
                active.prepare_map(1)
            cursor = active.send("sw start")
            active.expect("-> PLAYING", cursor)
            for suite in suites:
                active.check(suite)
                completed.append({"match": match, "suite": suite})
            if not performance_only:
                active.check("duplicating-stop-probe")
                completed.append({"match": match, "suite": "duplicating-stop-probe"})
            cursor = active.send("sw stop")
            active.expect("LOADING -> LOBBY", cursor)
            if not performance_only:
                active.check("duplicating-cleanup")
                completed.append({"match": match, "suite": "duplicating-cleanup"})
        success = True
        print("DUPLICATING CHECKS PASSED", flush=True)
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
            (cycle.OUT / "duplicating-results.json").write_text(
                json.dumps({"success": success, "completed": completed, "artifactSha256": tested_sha256,
                            "sourceArtifactSha256": source_sha256, "serverDirectory": str(allowed),
                            "serverPort": 25576, "performanceOnly": performance_only,
                            "elapsedSeconds": round(time.monotonic() - started, 2)},
                           indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--server-dir", type=Path, required=True,
                        help="Isolated Fabric server directory; the main server is refused")
    parser.add_argument("--performance-only", action="store_true",
                        help="Run only the baseline-compatible performance suite in one match")
    args = parser.parse_args()
    run(args.server_dir, args.performance_only)
