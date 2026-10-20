import hashlib
import json
import shutil
import sys
import zipfile
from collections import deque
from pathlib import Path

root = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(root / 'tools'))
from import_game_resources import migrate_loot_schema
from import_lbc_loadouts import SOURCE
from import_lbc_weapons import migrate_legacy_score_value_checks
from import_lbc_reforge_resources import dependencies, migrate_score_attribute_amounts

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

resources = root / 'src/main/resources'
artifact = root / 'build/libs/skywars2-0.1.0.jar'
digest = sha(artifact)
manifest = json.loads((root / 'agent_notes/INFBOLT_RESOURCE_MANIFEST.json').read_text(encoding='utf8'))
ported = 'data/lbc/loot_table/ported/'
payloads = {'bolt_material': ('lightning_bolt_egg', 'skywars2:bolt_material'),
            'infbolt_wand': ('tuntijaa_bolts_reforging', 'skywars2:infbolt_wand')}
for name, (source_name, native) in payloads.items():
    expected = json.loads((SOURCE / f'loot_table/{source_name}.json').read_text(encoding='utf-8-sig'))
    migrate_loot_schema(expected)
    expected['pools'][0]['entries'][0]['name'] = native
    actual = json.loads((resources / f'{ported}{name}.json').read_text(encoding='utf8'))
    assert actual == expected, name

queue = deque(('loot_table', name) for name in manifest['roots'])
seen = set()
source_paths = []
while queue:
    kind, name = queue.popleft()
    if (kind, name) in seen:
        continue
    seen.add((kind, name))
    expected = json.loads((SOURCE / f'{kind}/{name}.json').read_text(encoding='utf-8-sig'))
    queue.extend(dependencies(expected, kind))
    migrate_loot_schema(expected)
    migrate_legacy_score_value_checks(expected)
    migrate_score_attribute_amounts(expected)
    path = f'data/lbc/{kind}/{name}.json'
    assert json.loads((resources / path).read_text(encoding='utf8')) == expected, path
    source_paths.append(path)

java = ['game/LbcWands.java', 'game/LbcBolts.java', 'game/LbcElectro.java',
        'game/LbcCraftingRecipe.java', 'game/GameChecks.java', 'mixin/ElectroNativeMixins.java']
with zipfile.ZipFile(root / 'build/libs/skywars2-0.1.0-sources.jar') as sources:
    for name in java:
        relative = f'me/skywars2/{name}'
        assert sources.read(relative) == (root / 'src/main/java' / relative).read_bytes(), name

paths = source_paths + [f'{ported}{name}.json' for name in payloads]
paths += ['data/skywars2/recipe/infbolt_combine.json', 'skywars2.mixins.json']
with zipfile.ZipFile(artifact) as jar:
    assert jar.testzip() is None
    for path in paths:
        assert jar.read(path) == (resources / path).read_bytes(), path
    assert f'{ported}academy_infbolt_wand.json' not in jar.namelist()
    mixins = json.loads(jar.read('skywars2.mixins.json'))
    assert 'ElectroNativeMixins$Egg' in mixins['mixins']
    assert json.loads(jar.read('data/skywars2/recipe/infbolt_combine.json'))['type'] == 'skywars2:infbolt_combine'

main = root.parent / 'mods/skywars2.jar'
main_sha = sha(main)
assert main_sha == 'a0c4cb03c4682c99e71e0c197b376f98dc6781e1f153ec7ee61cba67408606a5'
assert sha(root / 'build/verification/skywars2-before-tanner-movement-a0c4cb03.jar') == main_sha
results = {}
for name, expected_checks in [('infbolt-results', 6), ('boltbolt-results', 5), ('infbolt-shared-results', 9)]:
    results[name] = json.loads((root / f'build/verification/{name}.json').read_text(encoding='utf8'))
    assert results[name]['artifactSha256'] == digest, name
    assert len(results[name]['completed']) == expected_checks, name
archive = root / f'build/verification/skywars2-infbolt-{digest[:8]}.jar'
shutil.copyfile(artifact, archive)
proof = dict(artifactSha256=digest, archive=archive.relative_to(root).as_posix(),
             fullSourceLootPayloads=list(payloads), sourceJavaFilesMatched=len(java),
             ultraReforgeResourcesMatched=len(source_paths), incrementalResources=len(manifest['paths']),
             packagedResourceBytesMatched=len(paths), crc='passed', recipe='passed',
             academyInfbolt='absent by author decision', mainJarSha256=main_sha, installed=False,
             limits=['No real-client visual test', 'No mass-load benchmark',
                     'Reforge can return legacy items with unported abilities',
                     'Known Polymer packet-context warning for fake-player chunks'],
             runtime=results)
(root / 'build/verification/infbolt-verification.json').write_text(json.dumps(proof, indent=2) + '\n', encoding='utf8')
print(json.dumps({key: value for key, value in proof.items() if key != 'runtime'}, indent=2))
