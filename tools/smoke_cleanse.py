"""Run immediate cleanse and existing artifact regression checks in one isolated match."""
from pathlib import Path
import hashlib
import json
import shutil
import sys
import time
import uuid

import smoke_game_cycle as cycle


def run(server_dir):
    cycle.SERVER = server_dir.resolve()
    if cycle.SERVER == cycle.PROJECT.parent:
        raise ValueError("Use an isolated verification server")
    cycle.require_stopped(cycle.SERVER)
    config_paths = [cycle.SERVER / "config/luckywars" / name for name in ("game.json", "players.json")]
    backups = {path: path.read_bytes() if path.exists() else None for path in config_paths}
    jar = cycle.PROJECT / "build/libs/skywars2-0.1.0.jar"
    installed = cycle.SERVER / "mods/skywars2.jar"
    old_jar = installed.read_bytes() if installed.exists() else None
    active = None
    outcomes = []
    started = time.monotonic()
    try:
        settings = json.loads(backups[config_paths[0]] or b"{}")
        settings.update(lives=2, health=20, day=1, gameMode=1, countdownSeconds=1, stageOneSeconds=600,
                        stageTwoSeconds=1200, endingSeconds=1, stageOneEvent=0, stageTwoEvent=0,
                        vanillaKits=False, vanillaSkills=False, lbcKits=True, lbcSkills=True,
                        lbcLevelups=True, lbcLevelupLimit=4, mapId="1", broken=False)
        config_paths[0].write_text(json.dumps(settings, indent=2) + "\n", encoding="utf-8")
        profiles = json.loads(backups[config_paths[1]] or b"{}")
        ident = str(uuid.UUID(bytes=hashlib.md5(b"OfflinePlayer:TestAlpha").digest(), version=3))
        profiles.setdefault(ident, {})["levelups"] = [33, 43, 5, 24]
        profiles[ident]["lbcTrinket"] = 16
        profiles[ident].pop("trinkets", None)
        config_paths[1].write_text(json.dumps(profiles, indent=2) + "\n", encoding="utf-8")
        shutil.copyfile(jar, installed)
        tested_sha256 = hashlib.sha256(installed.read_bytes()).hexdigest()

        active = cycle.Server("cleanse-immediate")
        active.expect("LOADING -> LOBBY", timeout=180)
        active.expect("Selected map ready: 1", timeout=180)
        active.check("bots")
        outcomes.append("two native test players created")
        # Existing metabolism assertions require a clean loadout with exactly 20 max health.
        active.check("artifact-passives-menu")
        outcomes.append("original passive loot/Polymer checks and clean loadout setup")
        cursor = active.send("sw start")
        active.expect("-> PLAYING", cursor)
        for check, description in (
                ("artifact-cleanse-immediate", "immediate native/custom status guards and passive gates"),
                ("artifact-debuffs", "fixed type-5 list, levitation policy and shared custom cleanup"),
                ("artifact-passives", "existing type-6/type-8 ordering, inventory and role checks")):
            active.check(check)
            outcomes.append(description)
        cursor = active.send("sw stop")
        active.expect("LOADING -> LOBBY", cursor)
        active.prepare_map(1)
        active.check("artifact-passives-cleanup")
        outcomes.append("match-stop cleanup")
        active.stop()
        active = None
        report = {"result": "passed", "jar_sha256": tested_sha256, "elapsed_seconds": round(time.monotonic() - started, 2),
                  "checks": outcomes}
        (cycle.OUT / "cleanse-immediate-results.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
        print(json.dumps(report, indent=2), flush=True)
    finally:
        if active is not None:
            active.stop()
        for path, content in backups.items():
            if content is None:
                path.unlink(missing_ok=True)
            else:
                path.write_bytes(content)
        if old_jar is None:
            installed.unlink(missing_ok=True)
        else:
            installed.write_bytes(old_jar)


if __name__ == "__main__":
    if len(sys.argv) != 2:
        raise SystemExit("usage: python tools/smoke_cleanse.py <isolated-server>")
    run(Path(sys.argv[1]))
