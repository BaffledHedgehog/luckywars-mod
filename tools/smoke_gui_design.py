"""Verify the final GUI artifact on an explicitly selected, stopped test server."""
from pathlib import Path
import argparse
import json
import hashlib
import shutil
import time
import smoke_game_cycle as cycle


def run(server_dir, locked_only=False, admin_only=False):
    cycle.SERVER = server_dir.resolve()
    if cycle.SERVER == cycle.PROJECT.parent:
        raise ValueError("Use an isolated verification server")
    cycle.require_stopped(cycle.SERVER)
    settings = cycle.SERVER / "config/luckywars/game.json"
    original = settings.read_bytes() if settings.exists() else None
    players = cycle.SERVER / "config/luckywars/players.json"
    original_players = players.read_bytes() if players.exists() else None
    artifact = cycle.PROJECT / "build/libs/skywars2-0.1.0.jar"
    shutil.copyfile(artifact, cycle.SERVER / "mods/skywars2.jar")
    sha = hashlib.sha256(artifact.read_bytes()).hexdigest()
    active = None
    completed = []
    started = time.monotonic()
    try:
        active = cycle.Server("admin-controls" if admin_only else "locked-perks" if locked_only else "design-final")
        active.expect("LOADING -> LOBBY")
        suites = (["bots", "admin-controls", "menu-navigation", "menus"] if admin_only else
                  ["bots", "locked-perks", "menu-navigation", "menus"] if locked_only else
                  ["bots", "lobby", "lobby-inventory", "menu-navigation", "design-export", "menus", "fast", "watch"])
        for suite in suites:
            active.check(suite)
            completed.append(suite)
        if locked_only or admin_only:
            active.stop()
            active = None
            print("ADMIN CONTROLS AND EVENT SETTINGS CHECKS PASSED" if admin_only else
                  "LOCKED PERKS AND RANKED CHECKS PASSED", flush=True)
            return
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
        if original_players is not None:
            players.write_bytes(original_players)
        else:
            players.unlink(missing_ok=True)
        (cycle.OUT / ("admin-controls-results.json" if admin_only else "locked-perks-results.json" if locked_only else "design-final-results.json")).write_text(json.dumps({
            "completed": completed, "artifactSha256": sha, "elapsedSeconds": round(time.monotonic() - started, 2)
        }, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--server-dir", type=Path, required=True)
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument("--locked-only", action="store_true")
    mode.add_argument("--admin-only", action="store_true")
    args = parser.parse_args()
    run(args.server_dir, args.locked_only, args.admin_only)
