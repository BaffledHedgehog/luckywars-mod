"""Exercise asynchronous map selection and match start on the isolated verification server."""
from pathlib import Path
import argparse
import hashlib
import json
import shutil
import time
import smoke_game_cycle as cycle


DEFAULT_SERVER = cycle.PROJECT / "build/verification/key-reuse-server"


def run(server_dir=DEFAULT_SERVER):
    server = server_dir.resolve()
    # Keep this regression isolated; never let an argument redirect it to the live server.
    if server != DEFAULT_SERVER.resolve():
        raise ValueError(f"This smoke test is restricted to {DEFAULT_SERVER}")
    cycle.SERVER = server
    files = [server / "config/luckywars" / name for name in ("game.json", "players.json")]
    backups = {path: path.read_bytes() if path.exists() else None for path in files}
    artifact = cycle.PROJECT / "build/libs/skywars2-0.1.0.jar"
    results_path = cycle.OUT / "selection-regressions-results.json"
    active = None
    completed = []
    started = time.monotonic()
    try:
        shutil.copyfile(artifact, server / "mods/skywars2.jar")
        settings = json.loads(backups[files[0]] or b"{}")
        settings.update(mapId="1", gameMode=1, lives=1, health=60, broken=False, countdownSeconds=1,
                        stageOneSeconds=600, stageTwoSeconds=1200, endingSeconds=5,
                        stageOneEvent=0, stageTwoEvent=0, offhandWands=True)
        files[0].write_text(json.dumps(settings, indent=2) + "\n", encoding="utf-8")

        active = cycle.Server("selection-regressions")
        active.expect("LOADING -> LOBBY", timeout=180)
        active.expect("Selected map ready: 1", timeout=180)
        for check in ("bots", "regressions"):
            active.check(check)
        completed.append("startup selected-map preparation and regression fixtures")

        cursor = active.send("sw map 2", "sw check lobby")
        active.expect("[check] PASS lobby", cursor, timeout=90)
        active.expect("Selected map ready: 2", cursor, timeout=180)
        cursor = active.send("sw map 1", "sw map 2")
        active.expect("Selected map ready: 2", cursor, timeout=180)
        completed.append("map 2 load and latest-selection-wins while map preparation is queued")

        cursor = active.send("sw start")
        active.expect("-> PLAYING", cursor, timeout=90)
        active.check("air-regression")
        active.check("observer-match")
        completed.append("match starts from prepared map and observer lifecycle fixture passes")

        cursor = active.send("sw check kill-reward", "sw check respawn")
        active.expect("-> ENDING", cursor, timeout=90)
        active.check("victory-regression")
        completed.append("native death, elimination, winner and actual fireworks")

        cursor = active.send("sw stop")
        active.expect("LOADING -> LOBBY", cursor, timeout=90)
        cursor = active.send("sw map 1")
        active.expect("Selected map ready: 1", cursor, timeout=180)
        completed.append("stop returns to usable lobby and default map 1 is prepared")
        cursor = active.check("map-start-menu")
        active.expect("[check] ARMED map-start-menu", cursor, timeout=30)
        active.expect("COUNTDOWN -> PLAYING", cursor, timeout=300)
        assert sum("Map operation 2 success=true" in line for line in active.lines[cursor:]) == 1, "Repeated GUI selection must paste map 2 once"
        completed.append("native GUI map selection queues a one-click start through asynchronous preparation")

        cursor = active.send("sw stop")
        active.expect("LOADING -> LOBBY", cursor, timeout=90)
        cursor = active.send("sw map 1")
        active.expect("Selected map ready: 1", cursor, timeout=180)
        cursor = active.check("map-start-quickstart")
        active.expect("[check] ARMED map-start-quickstart", cursor, timeout=30)
        active.expect("COUNTDOWN -> PLAYING", cursor, timeout=300)
        completed.append("native quickstart entity callback queues and auto-starts during map preparation")

        cursor = active.send("sw stop")
        active.expect("LOADING -> LOBBY", cursor, timeout=90)
        active.stop()
        active = None
        print("MAP SELECTION REGRESSIONS PASSED", flush=True)
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
            results_path.write_text(json.dumps({
                "completed": completed,
                "artifactSha256": hashlib.sha256(artifact.read_bytes()).hexdigest(),
                "elapsedSeconds": round(time.monotonic() - started, 2),
            }, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--server-dir", type=Path, default=DEFAULT_SERVER)
    run(parser.parse_args().server_dir)
