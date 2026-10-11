"""Verify the final GUI artifact on an explicitly selected, stopped test server."""
from pathlib import Path
import argparse
import json
import shutil
import time
import smoke_game_cycle as cycle


def run(server_dir):
    cycle.SERVER = server_dir.resolve()
    settings = cycle.SERVER / "config/luckywars/game.json"
    original = settings.read_bytes() if settings.exists() else None
    active = None
    completed = []
    started = time.monotonic()
    try:
        active = cycle.Server("design-final")
        active.expect("LOADING -> LOBBY")
        for suite in ["bots", "lobby", "lobby-inventory", "menu-navigation", "design-export", "menus", "fast", "watch"]:
            active.check(suite)
            completed.append(suite)
        cursor = active.send("sw start")
        active.expect("PASS watch/stale-menu", cursor)
        completed.append("loading-actions/navigation/countdown/stale-menu")
        active.stop()
        active = None
        shutil.copyfile(cycle.SERVER / "menu-design-snapshot.json", cycle.OUT / "menu-design-snapshot.json")
        print("FINAL GUI CHECKS PASSED", flush=True)
    finally:
        if active is not None:
            active.stop()
        if original is not None:
            settings.write_bytes(original)
        (cycle.OUT / "design-final-results.json").write_text(json.dumps({
            "completed": completed, "elapsedSeconds": round(time.monotonic() - started, 2)
        }, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--server-dir", type=Path, required=True)
    run(parser.parse_args().server_dir)
