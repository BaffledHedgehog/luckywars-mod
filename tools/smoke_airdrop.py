import argparse
import hashlib
import json
import shutil
import sys
import time
from pathlib import Path

TOOLS = Path(__file__).resolve().parent
sys.path.insert(0, str(TOOLS))
import smoke_game_cycle as cycle


def run(server_dir):
    if server_dir:
        cycle.SERVER = server_dir.resolve()
    if cycle.SERVER == cycle.PROJECT.parent:
        raise ValueError("Use an isolated verification server")
    settings = cycle.SERVER / "config/luckywars/game.json"
    profiles = cycle.SERVER / "config/luckywars/players.json"
    backups = {p: p.read_bytes() if p.exists() else None for p in (settings, profiles)}
    if backups[settings] is None:
        raise FileNotFoundError(settings)
    artifact = cycle.PROJECT / "build/libs/skywars2-0.1.0.jar"
    shutil.copyfile(artifact, cycle.SERVER / "mods/skywars2.jar")
    tested_sha = hashlib.sha256((cycle.SERVER / "mods/skywars2.jar").read_bytes()).hexdigest()
    active = None
    started = time.monotonic()
    completed = []
    try:
        config = json.loads(backups[settings].decode("utf-8"))
        config.update(gameMode=1, mapId="1", broken=False, stageOneSeconds=1, stageTwoSeconds=2, stageTwoEvent=8)
        settings.write_text(json.dumps(config, indent=2) + "\n", encoding="utf-8")
        active = cycle.Server("airdrop")
        active.expect("LOADING -> LOBBY", timeout=180)
        active.expect("Selected map ready: 1", timeout=180)
        active.check("bots")
        active.check("snapshot")
        active.check("airdrop-fast")
        cursor = active.send("sw start")
        active.expect("COUNTDOWN -> PLAYING", cursor)
        active.expect("PHASE_ONE -> PHASE_TWO", cursor)
        active.expect("SWRG airdrop ready", cursor, timeout=180)
        active.check("airdrop")
        completed.append("real phase-two chicken/cart passengers, normalized counts, genuine attributes and map-chest refill")
        cursor = active.check("airdrop-stop")
        active.expect("LOADING -> LOBBY", cursor)
        active.prepare_map(1)
        active.check("airdrop-cleanup")
        completed.append("native match stop removes all carriers and carts")
        active.check("restore")
        print("AIRDROP LIVE CHECKS PASSED", flush=True)
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
            (cycle.OUT / "airdrop-results.json").write_text(json.dumps({
                "completed": completed, "artifactSha256": tested_sha,
                "elapsedSeconds": round(time.monotonic() - started, 2)
            }, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--server-dir", type=Path, required=True,
                        help="Run against an isolated server directory instead of the main server")
    args = parser.parse_args()
    run(args.server_dir)
