"""Prepare an isolated integration server; never copy or modify the running world."""
from pathlib import Path
import shutil
import json

project = Path(__file__).resolve().parents[1]
source = project.parent
target = project / 'build/verification/key-reuse-server'
target.mkdir(parents=True, exist_ok=True)
for filename in ['fabric-server-launch.jar', 'eula.txt']:
    shutil.copy2(source / filename, target / filename)
for folder in ['libraries', 'versions']:
    shutil.copytree(source / folder, target / folder, dirs_exist_ok=True)
(target / 'mods').mkdir(exist_ok=True)
for jar in (source / 'mods').glob('*.jar'):
    if jar.name != 'skywars2.jar':
        shutil.copy2(jar, target / 'mods' / jar.name)
(target / 'config/luckywars').mkdir(parents=True, exist_ok=True)
shutil.copy2(source / 'config/luckywars/game.json', target / 'config/luckywars/game.json')
(target / 'config/polymer').mkdir(parents=True, exist_ok=True)
(target / 'config/polymer/auto-host.json').write_text(
    json.dumps({'enabled': True, 'required': False, 'type': 'polymer:automatic', 'settings': {}}), encoding='utf-8')
(target / 'server.properties').write_text(
    'server-ip=127.0.0.1\nserver-port=25576\nonline-mode=true\n'
    'enable-rcon=false\nallow-flight=true\nview-distance=4\nsimulation-distance=4\n'
    'pause-when-empty-seconds=60\nlevel-seed=0\n', encoding='utf-8')
print(f'Isolated test server prepared: {target}')
