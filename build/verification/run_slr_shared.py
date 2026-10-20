"""Run the shared Frost/input and Razorpine regressions on the final SLR artifact."""
import hashlib
import json
import shutil
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools'))
import smoke_game_cycle as cycle

cycle.SERVER = ROOT / 'build/verification/key-reuse-server'
cycle.require_stopped(cycle.SERVER)
settings = cycle.SERVER / 'config/luckywars/game.json'
profiles = cycle.SERVER / 'config/luckywars/players.json'
backups = {p: p.read_bytes() if p.exists() else None for p in (settings, profiles)}
artifact = ROOT / 'build/libs/skywars2-0.1.0.jar'
shutil.copyfile(artifact, cycle.SERVER / 'mods/skywars2.jar')
digest = hashlib.sha256(artifact.read_bytes()).hexdigest()
active = None
done = []
started = time.monotonic()
try:
    data = json.loads(backups[settings] or b'{}')
    data.update(lives=2, health=20, day=1, gameMode=1, countdownSeconds=1,
                stageOneSeconds=600, stageTwoSeconds=1200, endingSeconds=1,
                stageOneEvent=0, stageTwoEvent=0, vanillaKits=False,
                vanillaSkills=False, lbcKits=True, lbcSkills=True,
                lbcLevelups=True, lbcLevelupLimit=4, mapId='1', broken=False)
    settings.write_text(json.dumps(data, indent=2) + '\n', encoding='utf-8')
    active = cycle.Server('slr-shared')
    active.expect('LOADING -> LOBBY', timeout=180)
    active.expect('Selected map ready: 1', timeout=180)
    active.check('bots')
    active.check('frost-menu'); done.append('frost-menu')
    active.check('pinegun-menu'); done.append('pinegun-menu')
    cursor = active.send('sw start')
    active.expect('-> PLAYING', cursor)
    for suite in ('frost', 'pinegun'):
        active.check(suite); done.append(suite)
    cursor = active.send('sw stop')
    active.expect('LOADING -> LOBBY', cursor)
    for suite in ('frost-cleanup', 'pinegun-cleanup'):
        active.check(suite); done.append(suite)
    print('SLR SHARED CHECKS PASSED', flush=True)
finally:
    try:
        if active is not None: active.stop()
    finally:
        for path, data in backups.items():
            if data is None: path.unlink(missing_ok=True)
            else: path.write_bytes(data)
        (ROOT / 'build/verification/slr-shared-results.json').write_text(
            json.dumps({'completed': done, 'artifactSha256': digest,
                        'elapsedSeconds': round(time.monotonic()-started, 2)}, indent=2)+'\n',
            encoding='utf-8')
