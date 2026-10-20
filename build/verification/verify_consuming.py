"""Final read-only Consuming evidence check; writes only proof JSON and archive."""
from __future__ import annotations
import hashlib
import json
import re
import shutil
import sys
import zipfile
from collections import deque
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools'))
from import_lbc_consuming import verify_sources, audit_design, TRIAL
from import_lbc_loadouts import SOURCE
from import_game_resources import migrate_loot_schema
from import_lbc_weapons import migrate_legacy_score_value_checks
from import_lbc_reforge_resources import dependencies, migrate_score_attribute_amounts


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def read(path):
    return json.loads(path.read_text(encoding='utf-8-sig'))


def main():
    resources = ROOT / 'src/main/resources'
    artifact = ROOT / 'build/libs/skywars2-0.1.0.jar'
    digest = sha(artifact)
    verify_sources()  # Complete ordinary/donor + source-derived Academy equality, no writes.
    ported = 'data/lbc/loot_table/ported/'
    names = ['consuming_wand', 'academy_consuming_wand', 'creeper_army_spell', 'destroyer_spell']
    paths = [f'{ported}{name}.json' for name in names]
    preset_path = 'skywars2/consuming_presets.json'
    presets = read(resources / preset_path)
    matches = re.findall(r'^summon (minecraft:skeleton) (\S+) (\S+) (\S+) (\{.*\})$',
                         TRIAL.read_text(encoding='utf-8-sig'), re.MULTILINE)
    assert len(matches) == 28 and len({(m[0], m[4]) for m in matches}) == 1
    entity, _, _, _, nbt = matches[0]
    expected = {'originalDataVersion': 4440,
                'presets': {'void_man': '{id:"' + entity + '",' + nbt[1:]},
                'spawns': [{'preset': 'void_man', 'offset': [float(v[1:] or 0) for v in m[1:4]]}
                           for m in matches]}
    assert presets == expected, 'full raw source preset and ordered offsets'
    paths.append(preset_path)

    queue = deque([('loot_table', 'voidman')]); seen = set(); closure = []
    while queue:
        kind, name = queue.popleft()
        if (kind, name) in seen:
            continue
        seen.add((kind, name))
        relative = f'{kind}/{name}.json'
        expected = read(SOURCE / relative)
        queue.extend(dependencies(expected, kind))
        migrate_loot_schema(expected)
        migrate_legacy_score_value_checks(expected)
        migrate_score_attribute_amounts(expected)
        path = 'data/lbc/' + relative
        assert read(resources / path) == expected, path
        closure.append(path)
    assert len(closure) == 2, closure
    paths.extend(closure)

    # Silence now uses the author-approved sphere erasure. Its old eight structure
    # files are not evidence requirements and are deliberately absent here.
    templates = ['meditation', 'air11x11']
    for name in templates:
        path = f'data/lbc/structure/{name}.nbt'
        assert (resources / path).read_bytes() == (SOURCE / f'structure/{name}.nbt').read_bytes()
        paths.append(path)
    paths += ['data/skywars2/recipe/consuming_combine.json',
              'data/skywars2/damage_type/consuming.json',
              'data/minecraft/tags/damage_type/bypasses_cooldown.json',
              'data/minecraft/tags/damage_type/is_projectile.json']
    assert read(resources / 'data/skywars2/recipe/consuming_combine.json')['type'] == 'skywars2:consuming_combine'
    assert read(resources / 'data/skywars2/damage_type/consuming.json') == {
        'exhaustion': .1, 'message_id': 'arrow', 'scaling': 'when_caused_by_living_non_player'}
    for tag in ['bypasses_cooldown', 'is_projectile']:
        assert 'skywars2:consuming' in read(resources / f'data/minecraft/tags/damage_type/{tag}.json')['values']

    java = ['SkyWars2.java', 'game/BlockAreaWork.java', 'game/GameChecks.java',
            'game/LbcAcademy.java', 'game/LbcConsuming.java', 'game/LbcCraftingRecipe.java',
            'game/LbcDamage.java', 'game/LbcIndirectKills.java', 'game/LbcItems.java',
            'game/LbcWands.java', 'game/LbcBlaze.java', 'game/LbcBlind.java',
            'game/LbcBolts.java', 'game/LbcElectro.java', 'game/LbcHoming.java',
            'game/LbcOverload.java', 'mixin/ElectroNativeMixins.java', 'mixin/WandProjectileMixin.java']
    with zipfile.ZipFile(ROOT / 'build/libs/skywars2-0.1.0-sources.jar') as sources:
        assert sources.testzip() is None
        for name in java:
            relative = 'me/skywars2/' + name
            assert sources.read(relative) == (ROOT / 'src/main/java' / relative).read_bytes(), name
    with zipfile.ZipFile(artifact) as jar:
        assert jar.testzip() is None
        for path in paths:
            assert jar.read(path) == (resources / path).read_bytes(), path
        assert any(name.startswith('me/skywars2/game/LbcConsuming') and name.endswith('.class')
                   for name in jar.namelist())

    # Audit is read-only and checks all source keys/model/equipment availability.
    design = audit_design([resources / f'{ported}{name}.json' for name in names]
                          + [resources / path for path in closure])
    forbidden = {'consuming_wand', 'consuming_wand.lore', 'creeper_army_spell',
                 'destroyer_spell', 'upgrade_condition_consuming', 'upgrade_succesful_consuming', 'void_man'}
    for base in [resources / 'assets', ROOT / 'resourcepack_template/assets']:
        if base.exists():
            for language in base.glob('*/lang/*.json'):
                assert not forbidden.intersection(read(language)), f'duplicate source keys in {language}'

    main_jar = ROOT.parent / 'mods/skywars2.jar'
    old_digest = 'a0c4cb03c4682c99e71e0c197b376f98dc6781e1f153ec7ee61cba67408606a5'
    assert sha(main_jar) == old_digest
    assert sha(ROOT / 'build/verification/skywars2-before-tanner-movement-a0c4cb03.jar') == old_digest
    runtime = {}
    for name in ['consuming-results', 'consuming-shared-results', 'boltbolt-results']:
        report = read(ROOT / f'build/verification/{name}.json')
        assert report['artifactSha256'] == digest, name
        assert isinstance(report['completed'], list) and report['completed'], name
        runtime[name] = report
    expected_checks = ['consuming-menu', 'consuming-crafting', 'consuming', 'consuming-donors',
                       'consuming-academy', 'consuming-trial', 'infbolt-egg']
    expected_checks += ['second match: ' + name for name in
                        ['consuming', 'consuming-donors', 'consuming-academy', 'consuming-trial']]
    assert runtime['consuming-results']['completed'] == expected_checks
    assert runtime['consuming-shared-results']['completed'] == [
        'teleport-wand', 'wands-electro', 'electro-gear', 'exahoming',
        'tannerkivi', 'tannerkivi-movement', 'teleport-wand-cleanup',
        'electro-family-cleanup', 'exahoming-cleanup', 'boltbolt-cleanup', 'tannerkivi-cleanup']
    assert len(runtime['boltbolt-results']['completed']) == 5
    archive = ROOT / f'build/verification/skywars2-consuming-{digest[:8]}.jar'
    shutil.copyfile(artifact, archive)
    assert sha(archive) == digest
    proof = {'artifactSha256': digest, 'archive': archive.relative_to(ROOT).as_posix(),
             'fullSourceLootPayloads': names, 'sourceJavaFilesMatched': len(java),
             'sourceJavaPaths': java, 'sourcePresetSpawns': len(matches),
             'voidManLootClosure': closure, 'reusedTemplates': templates,
             'packagedResourceBytesMatched': len(paths), 'crc': 'passed',
             'recipe': 'passed', 'damageTypeAndTags': 'passed',
             'design': design, 'mainJarSha256': old_digest, 'installed': False,
             'completed': {'consuming': len(expected_checks),
                           'shared': len(runtime['consuming-shared-results']['completed']),
                           'boltbolt': len(runtime['boltbolt-results']['completed'])},
             'limits': ['No real-client visual test', 'No mass-load benchmark',
                        'Known Polymer packet-context warning for fake-player chunks'],
             'runtime': runtime}
    (ROOT / 'build/verification/consuming-verification.json').write_text(
        json.dumps(proof, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({key: value for key, value in proof.items() if key != 'runtime'}, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
