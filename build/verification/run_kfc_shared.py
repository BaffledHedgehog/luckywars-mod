"""Check the existing systems whose owners are reused by KFC on the same final JAR."""
import hashlib
import json
import shutil
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools"))
import smoke_game_cycle as cycle

cycle.SERVER = ROOT / "build/verification/key-reuse-server"
cycle.require_stopped(cycle.SERVER)
settings = cycle.SERVER / "config/luckywars/game.json"
profiles = cycle.SERVER / "config/luckywars/players.json"
backups = {p: p.read_bytes() if p.exists() else None for p in (settings, profiles)}
artifact = ROOT / "build/libs/skywars2-0.1.0.jar"
shutil.copyfile(artifact, cycle.SERVER / "mods/skywars2.jar")
digest = hashlib.sha256(artifact.read_bytes()).hexdigest()
active = None
done = []
passed = False
started = time.monotonic()
try:
    data = json.loads(backups[settings] or b"{}")
    data.update(lives=2, health=20, day=1, gameMode=1, countdownSeconds=1,
                stageOneSeconds=600, stageTwoSeconds=1200, endingSeconds=1,
                stageOneEvent=0, stageTwoEvent=0, vanillaKits=False,
                vanillaSkills=False, lbcKits=True, lbcSkills=True,
                lbcLevelups=True, lbcLevelupLimit=4, offhandWands=True, mapId="1", broken=False)
    settings.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")
    active = cycle.Server("kfc-shared")
    active.expect("LOADING -> LOBBY", timeout=180)
    active.expect("Selected map ready: 1", timeout=180)
    active.check("bots")
    for suite in ("regressions", "pinegun-menu", "slr-menu", "electro-gear-menu", "magic-menu"):
        active.check(suite)
        done.append(suite)
    cursor = active.send("sw start")
    active.expect("-> PLAYING", cursor)
    for suite in ("fireblaze-menu", "psycho-menu", "pinegun", "slr", "fireblaze-wand", "psycho", "electro-gear", "magic"):
        if suite == "fireblaze-wand":
            # Its 14 source trial positions reach 150 blocks away. Loading chunks inside
            # one synchronous check does not publish every entity section immediately.
            # Prepare both axes before the check, then remove only these fixture forces.
            rectangles = ("~-160 ~-8 ~160 ~8", "~-8 ~-160 ~8 ~160")
            for rectangle in rectangles:
                active.send("execute in skywars2:arena at TestAlpha run forceload add " + rectangle)
            time.sleep(2)
            try:
                active.check(suite)
            finally:
                for rectangle in rectangles:
                    active.send("execute in skywars2:arena at TestAlpha run forceload remove " + rectangle)
        else:
            active.check(suite)
        done.append(suite)
    active.check("slr-stop-probe")
    done.append("slr-stop-probe")
    cursor = active.send("sw stop")
    active.expect("LOADING -> LOBBY", cursor)
    for suite in ("pinegun-cleanup", "slr-cleanup", "fireblaze-cleanup", "psycho-cleanup", "magic-cleanup"):
        active.check(suite)
        done.append(suite)
    passed = True
    print("KFC SHARED CHECKS PASSED", flush=True)
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
        (ROOT / "build/verification/kfc-shared-results.json").write_text(
            json.dumps({"passed": passed, "completed": done, "artifactSha256": digest,
                        "elapsedSeconds": round(time.monotonic() - started, 2)}, indent=2) + "\n",
            encoding="utf-8")
