"""Verify nuclear shader carriers and both real wand integration paths on the isolated server."""
from pathlib import Path
import hashlib
import json
import shutil
import time
import smoke_game_cycle as cycle


def run() -> None:
    cycle.SERVER = (cycle.PROJECT / "build/verification/key-reuse-server").resolve()
    cycle.require_stopped(cycle.SERVER)
    paths = [cycle.SERVER / "config/luckywars/game.json", cycle.SERVER / "config/luckywars/players.json"]
    backups = {path: path.read_bytes() if path.exists() else None for path in paths}
    artifact = cycle.PROJECT / "build/libs/skywars2-0.1.0.jar"
    shutil.copyfile(artifact, cycle.SERVER / "mods/skywars2.jar")
    sha = hashlib.sha256(artifact.read_bytes()).hexdigest()
    active = None
    passed = False
    completed = []
    started = time.monotonic()
    try:
        settings = json.loads(backups[paths[0]] or b"{}")
        settings.update(lives=10, health=20, gameMode=1, countdownSeconds=1,
                        stageOneSeconds=600, stageTwoSeconds=1200, endingSeconds=1,
                        stageOneEvent=0, stageTwoEvent=0, vanillaKits=False, vanillaSkills=False,
                        lbcKits=True, lbcSkills=True, lbcLevelups=True, lbcLevelupLimit=4,
                        offhandWands=True, mapId="1", broken=False)
        paths[0].write_text(json.dumps(settings, indent=2) + "\n", encoding="utf-8")
        active = cycle.Server("nuclear-shader")
        active.expect("LOADING -> LOBBY", timeout=180)
        active.expect("Selected map ready: 1", timeout=180)
        active.check("bots")
        for match in (1, 2):
            cursor = active.send("sw start")
            active.expect("-> PLAYING", cursor)
            for suite in ("nuclear-visual", "exasperato-academy", "exoperso", "exoperso-blast"):
                active.check(suite)
                completed.append(f"Match {match}: {suite}")
            cursor = active.send("sw stop")
            active.expect("LOADING -> LOBBY", cursor)
            active.check("exasperato-cleanup")
            active.check("exoperso-cleanup")
            completed.append(f"Match {match}: native queue, display and lifecycle cleanup")
            if match == 1:
                active.prepare_map(1)
        passed = True
        print("NUCLEAR SHADER CHECKS PASSED", flush=True)
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
            (cycle.OUT / "nuclear-shader-results.json").write_text(json.dumps(
                dict(passed=passed, completed=completed, artifactSha256=sha,
                     elapsedSeconds=round(time.monotonic()-started, 2)), indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    run()
