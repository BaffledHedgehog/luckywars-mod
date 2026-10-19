"""Isolated fatal-overload integration check; no production source changes."""
import hashlib
import json
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path('tools').resolve()))
import smoke_game_cycle as cycle

cycle.SERVER = Path('build/verification/key-reuse-server').resolve()
paths = [cycle.SERVER / 'config/luckywars' / name for name in ('game.json', 'players.json')]
backups = {p: p.read_bytes() if p.exists() else None for p in paths}
artifact = cycle.SERVER / 'mods/skywars2.jar'
sha = hashlib.sha256(artifact.read_bytes()).hexdigest()
assert sha == '72b4ec22bd53dd142748582b58319b25793f713d89275e414c7001b829bf46d5'
server = None
started = time.monotonic()
try:
    settings = json.loads(backups[paths[0]] or b'{}')
    settings.update(gameMode=1, lives=2, health=20, countdownSeconds=1, endingSeconds=1,
                    stageOneSeconds=600, stageTwoSeconds=1200, stageOneEvent=0,
                    stageTwoEvent=0, mapId='1', broken=False)
    paths[0].write_text(json.dumps(settings, indent=2)+'\n', encoding='utf-8')
    server = cycle.Server('overload-burst')
    server.expect('Selected map ready: 1', timeout=180)
    server.check('bots')
    cursor = server.send('sw start')
    server.expect('-> PLAYING', cursor)
    server.send('execute in skywars2:arena run setblock 0 199 0 minecraft:stone')
    server.send('execute in skywars2:arena run tp TestAlpha 0 200 0')
    server.send('scoreboard players set TestAlpha manamax 5000')
    cursor = server.send('scoreboard players set TestAlpha mana -100000000')
    # Await the actual tick/death, rather than testing NBT in the command queue before that tick.
    server.expect('TestAlpha was killed', cursor, timeout=15)
    cursor = server.send('execute in skywars2:arena if block 0 199 0 minecraft:air run say OVERLOAD_BIG_RAY_BLOCK_PASS')
    server.expect('OVERLOAD_BIG_RAY_BLOCK_PASS', cursor, timeout=15)
    server.stop()
    server = None
    result = dict(artifactSha256=sha, elapsedSeconds=round(time.monotonic()-started, 2),
                  passed=['native lethal debt death', 'big ray clears the stone below its origin'])
    Path('build/verification/overload-burst-results.json').write_text(json.dumps(result, indent=2)+'\n', encoding='utf-8')
    print(json.dumps(result), flush=True)
finally:
    if server is not None:
        server.stop()
    for path, original in backups.items():
        if original is None:
            path.unlink(missing_ok=True)
        else:
            path.write_bytes(original)
