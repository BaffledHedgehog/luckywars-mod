"""Verify the pending HUD, Overload, acid and lobby changes on an isolated server."""
from pathlib import Path
import argparse
import hashlib
import json
import shutil
import time

import smoke_game_cycle as cycle


def run(server_dir, attributes_only=False, reset_only=False, zone_only=False):
    cycle.SERVER = server_dir.resolve()
    if cycle.SERVER == cycle.PROJECT.parent:
        raise ValueError("Use an isolated server")
    cycle.require_stopped(cycle.SERVER)
    paths = [cycle.SERVER / "config/luckywars" / name for name in ("game.json", "players.json")]
    backups = {p: p.read_bytes() if p.exists() else None for p in paths}
    artifact = cycle.PROJECT / "build/libs/skywars2-0.1.0.jar"
    installed = cycle.SERVER / "mods/skywars2.jar"
    shutil.copyfile(artifact, installed)
    sha = hashlib.sha256(installed.read_bytes()).hexdigest()
    active = None
    checks = []
    started = time.monotonic()
    success = False
    try:
        config = json.loads(backups[paths[0]] or b"{}")
        config.update(gameMode=1, lives=2, health=20, day=1, countdownSeconds=1,
                      stageOneSeconds=600, stageTwoSeconds=1200, endingSeconds=1,
                      stageOneEvent=0, stageTwoEvent=0, vanillaKits=False, vanillaSkills=False,
                      lbcKits=True, lbcSkills=True, lbcLevelups=True, lbcLevelupLimit=4,
                      offhandWands=True, mapId="1", broken=False)
        if reset_only:
            config.update(lives=1, health=40, pvpMode=1, countdownSeconds=2, endingSeconds=15,
                          lbcKits=False, lbcSkills=False)
        if zone_only:
            config.update(lives=2, health=100, pvpMode=2, stageOneSeconds=1, stageTwoSeconds=2,
                          endingSeconds=15, stageTwoEvent=6, noBorders=True,
                          lbcKits=False, lbcSkills=False, lbcLevelups=False)
        paths[0].write_text(json.dumps(config, indent=2) + "\n", encoding="utf-8")
        label = "zone-barrier" if zone_only else "attribute-reset" if reset_only else "attribute-hud" if attributes_only else "ui-gameplay"
        active = cycle.Server(label)
        active.expect("LOADING -> LOBBY", timeout=180)
        active.expect("Selected map ready: 1", timeout=180)

        def check(name):
            cursor = active.check(name)
            checks.append(name)
            return cursor

        check("bots")
        if zone_only:
            check("entity-limits")
            cursor = active.send("sw start")
            active.expect("-> PHASE_TWO", cursor)
            check("zone-barrier")
            check("zone-barrier-lethal")
            check("respawn")
            cursor = active.send("sw stop")
            active.expect("LOADING -> LOBBY", cursor)
            check("zone-barrier-cleanup")
            active.stop()
            active = None
            success = True
            print("ZONE AND ENTITY LIMIT CHECKS PASSED", flush=True)
            return
        if reset_only:
            cursor = check("attribute-reset-start")
            active.expect("-> PLAYING", cursor)
            check("attribute-reset-playing")
            cursor = check("attribute-reset-cancel")
            active.expect("LOADING -> LOBBY", cursor)
            check("attribute-reset-lobby")
            active.prepare_map(1)
            cursor = check("attribute-reset-start")
            active.expect("-> PLAYING", cursor)
            check("attribute-reset-playing")
            check("attribute-reset-finish")
            check("respawn")
            cursor = active.send("sw stop")
            active.expect("LOADING -> LOBBY", cursor)
            check("attribute-reset-lobby")
            check("attribute-hud")
            active.stop()
            active = None
            success = True
            print("ATTRIBUTE RESET CHECKS PASSED", flush=True)
            return
        if attributes_only:
            check("attribute-hud")
            active.stop()
            active = None
            success = True
            print("ATTRIBUTE HUD CHECKS PASSED", flush=True)
            return
        check("mana-potions-menu")
        check("overload-menu")
        check("brbad-menu")
        check("lobby-origin")
        cursor = active.send("sw start")
        active.expect("-> PLAYING", cursor)
        for name in ("lobby-overlay-started", "attribute-hud", "mana-potions", "overload", "brbad"):
            check(name)
        check("lobby-aftermath-setup")
        cursor = active.send("sw stop")
        active.expect("LOADING -> LOBBY", cursor, timeout=180)
        check("lobby-aftermath")
        check("brbad-cleanup")

        # Preserve the completed map until this EXPLICIT choice, as requested by the user.
        cursor = active.send("sw map 1")
        check("lobby-loading-overlay")
        active.expect("Selected map ready: 1", cursor, timeout=180)
        cursor = active.send("sw start")
        active.expect("-> PLAYING", cursor)
        check("lobby-next-map")
        check("observer-match")
        cursor = active.send("sw stop")
        active.expect("LOADING -> LOBBY", cursor, timeout=180)
        check("overload-cleanup")
        check("mana-potions-cleanup")
        check("cleanup")
        active.stop()
        active = None
        success = True
        print("PENDING UI/GAMEPLAY CHECKS PASSED", flush=True)
    finally:
        try:
            if active is not None:
                active.stop()
        finally:
            for path, content in backups.items():
                if content is None:
                    path.unlink(missing_ok=True)
                else:
                    path.write_bytes(content)
            report = dict(result="passed" if success else "failed", completed=checks,
                          artifactSha256=sha, elapsedSeconds=round(time.monotonic() - started, 2))
            label = "zone-barrier" if zone_only else "attribute-reset" if reset_only else "attribute-hud" if attributes_only else "ui-gameplay"
            (cycle.OUT / f"{label}-results.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--server-dir", type=Path, required=True)
    modes = parser.add_mutually_exclusive_group()
    modes.add_argument("--attributes-only", action="store_true", help="Check attribute column, idle timing and lifecycle")
    modes.add_argument("--attribute-reset-only", action="store_true", help="Check registry-wide reset at start, cancel and natural ending")
    modes.add_argument("--zone-only", action="store_true", help="Check entity limits and circular zone geometry, damage and cleanup")
    args = parser.parse_args()
    run(args.server_dir, args.attributes_only, args.attribute_reset_only, args.zone_only)
