"""Related native regressions for Consuming's shared work, targeting and origin hooks."""
import sys, json, time, hashlib
from pathlib import Path

sys.path.insert(0, str(Path('tools').resolve()))
import smoke_game_cycle as c

prefix = sys.argv[1] if len(sys.argv) > 1 else 'consuming'
if prefix not in {'consuming', 'duplicating'}:
    raise ValueError('Expected consuming or duplicating report prefix')
c.SERVER = Path('build/verification/key-reuse-server').resolve()
assert c.SERVER != c.PROJECT.parent
c.require_stopped(c.SERVER)
settings = c.SERVER / 'config/luckywars/game.json'
profiles = c.SERVER / 'config/luckywars/players.json'
backups = {p: p.read_bytes() if p.exists() else None for p in (settings, profiles)}
a = None
done = []
started = time.monotonic()
sha = hashlib.sha256((c.SERVER / 'mods/skywars2.jar').read_bytes()).hexdigest()
assert sha == hashlib.sha256((c.PROJECT / 'build/libs/skywars2-0.1.0.jar').read_bytes()).hexdigest()
try:
    data = json.loads(backups[settings] or b'{}')
    data.update(lives=2, health=20, day=1, gameMode=1, countdownSeconds=1,
                stageOneSeconds=600, stageTwoSeconds=1200, endingSeconds=1,
                stageOneEvent=0, stageTwoEvent=0, vanillaKits=False,
                vanillaSkills=False, lbcKits=True, lbcSkills=True,
                lbcLevelups=True, lbcLevelupLimit=4, mapId='1', broken=False)
    settings.write_text(json.dumps(data, indent=2) + '\n', encoding='utf8')
    a = c.Server(prefix + '-shared')
    a.expect('LOADING -> LOBBY', timeout=180)
    a.expect('Selected map ready: 1', timeout=180)
    a.check('bots')
    cursor = a.send('sw start')
    a.expect('-> PLAYING', cursor)
    for suite in ['teleport-wand', 'wands-electro', 'electro-gear', 'exahoming',
                  'tannerkivi', 'tannerkivi-movement']:
        a.check(suite)
        done.append(suite)
        # NPC callbacks deliberately debounce duplicate interactions within one native tick.
        time.sleep(.15)
    cursor = a.send('sw stop')
    a.expect('LOADING -> LOBBY', cursor)
    for suite in ['teleport-wand-cleanup', 'electro-family-cleanup',
                  'exahoming-cleanup', 'boltbolt-cleanup', 'tannerkivi-cleanup']:
        a.check(suite)
        done.append(suite)
        time.sleep(.15)
    print(prefix.upper() + ' SHARED REGRESSION PASSED', flush=True)
finally:
    try:
        if a is not None:
            a.stop()
    finally:
        for path, raw in backups.items():
            if raw is None:
                path.unlink(missing_ok=True)
            else:
                path.write_bytes(raw)
        (c.OUT / (prefix + '-shared-results.json')).write_text(json.dumps(dict(
            completed=done, artifactSha256=sha,
            elapsedSeconds=round(time.monotonic() - started, 2)), indent=2) + '\n', encoding='utf8')
