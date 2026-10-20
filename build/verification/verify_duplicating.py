"""Final read-only Duplicating evidence check; writes only verified archive/proof."""
from __future__ import annotations
import hashlib
import json
import shutil
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools'))
from import_lbc_consuming import RP, migrated
from import_lbc_fireblaze import select_entry
from import_lbc_loadouts import SOURCE
from import_lbc_wands import build_single_entry_table

PRIMARY = ['duplicating-menu', 'duplicating-crafting', 'duplicating', 'divider',
           'duplicating-academy', 'duplicating-expand', 'duplicating-trial', 'consuming-academy']
PRIMARY += ['second match: ' + name for name in ['duplicating', 'divider',
            'duplicating-academy', 'duplicating-expand', 'duplicating-trial', 'consuming-academy']]
CONSUMING = ['consuming-menu', 'consuming-crafting', 'consuming', 'consuming-donors',
             'consuming-academy', 'consuming-trial', 'infbolt-egg']
CONSUMING += ['second match: ' + name for name in
              ['consuming', 'consuming-donors', 'consuming-academy', 'consuming-trial']]
# Root must set this to the final runner's exact sequence before final invocation.
SHARED = ['teleport-wand', 'wands-electro', 'electro-gear', 'exahoming',
          'tannerkivi', 'tannerkivi-movement', 'teleport-wand-cleanup',
          'electro-family-cleanup', 'exahoming-cleanup', 'boltbolt-cleanup', 'tannerkivi-cleanup']

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def read(path):
    return json.loads(path.read_text(encoding='utf-8-sig'))

def verify_sources(resources):
    ported = resources / 'data/lbc/loot_table/ported'
    for name, source, key, marker in (
        ('duplicating_wand', 'tuntijaa_greed_avarice', 'dublicating_wand', 'dublicating_wand:1'),
        ('divider', 'greed_greed', 'divider', 'greed_greed:1')):
        table = migrated(SOURCE / f'loot_table/{source}.json')
        pool, entry = select_entry(table, key, marker)
        entry = json.loads(json.dumps(entry)); entry['name'] = 'skywars2:' + name
        assert read(ported / f'{name}.json') == build_single_entry_table(table, pool, entry), name
    expected = read(ported / 'duplicating_wand.json')
    entry = expected['pools'][0]['entries'][0]
    entry['name'] = 'skywars2:academy_duplicating_wand'
    functions = entry['modifier']
    custom = next(fn for fn in functions if fn.get('type') == 'minecraft:set_custom_data')
    custom['tag'] = custom['tag'].rstrip('}') + ',upgradable:0,upgrade_available:1,upgraded:1,mode:1}'
    for name in ('add_upgrade_desc', 'tiers/ss'):
        modifier = migrated(SOURCE / f'item_modifier/{name}.json')
        functions.extend(modifier if isinstance(modifier, list) else [modifier])
    assert read(ported / 'academy_duplicating_wand.json') == expected

def audit_design(paths, resources):
    keys = {'upgrade_condition_dublicating', 'upgrade_succesful_dublicating',
            'mode_default', 'mode_expand', 'mode_divide', 'mode_black_hole'}
    models = set(); styles = set()
    def visit(value):
        if isinstance(value, list):
            for child in value: visit(child)
        elif isinstance(value, dict):
            if 'translate' in value: keys.add(value['translate'])
            if 'minecraft:item_model' in value: models.add(value['minecraft:item_model'])
            if 'minecraft:tooltip_style' in value: styles.add(value['minecraft:tooltip_style'])
            for child in value.values(): visit(child)
    for path in paths: visit(read(resources / path))
    languages = {}
    for language in ('ru_ru', 'en_us'):
        path = RP / f'assets/minecraft/lang/{language}.json'
        data = read(path)
        assert not keys - data.keys(), (language, keys - data.keys())
        languages[language] = {'sha256': sha(path), 'values': {key: data[key] for key in sorted(keys)}}
    for identifier in models:
        namespace, name = identifier.split(':')
        assert (RP / f'assets/{namespace}/items/{name}.json').is_file(), identifier
    for identifier in styles:
        namespace, name = identifier.split(':')
        for suffix in ('background', 'frame'):
            assert (RP / f'assets/{namespace}/textures/gui/sprites/tooltip/{name}_{suffix}.png').is_file()
    # Forbid duplicate newly ported source keys, while allowing pre-existing shared keys.
    forbidden = {'dublicating_wand', 'dublicating_wand.lore', 'divider', 'divider.lore',
                 'divider.lore.1', 'upgrade_condition_dublicating', 'upgrade_succesful_dublicating'}
    for base in (resources / 'assets', ROOT / 'resourcepack_template/assets'):
        if base.exists():
            for path in base.glob('*/lang/*.json'):
                assert not forbidden.intersection(read(path)), f'duplicate source keys in {path}'
    return {'keys': sorted(keys), 'models': sorted(models), 'tooltipStyles': sorted(styles), 'languages': languages}

def main():
    resources = ROOT / 'src/main/resources'
    artifact = ROOT / 'build/libs/skywars2-0.1.0.jar'
    digest = sha(artifact)
    verify_sources(resources)
    names = ['duplicating_wand', 'academy_duplicating_wand', 'divider']
    item_paths = [f'data/lbc/loot_table/ported/{name}.json' for name in names]
    paths = list(item_paths)
    template = 'data/lbc/structure/obsidian_sphere_air_r7.nbt'
    assert (resources / template).read_bytes() == (SOURCE / 'structure/obsidian_sphere_air_r7.nbt').read_bytes()
    paths.append(template)
    recipe = 'data/skywars2/recipe/duplicating_combine.json'
    dtype = 'data/skywars2/damage_type/duplicating_hole.json'
    assert read(resources / recipe) == {'type': 'skywars2:duplicating_combine'}
    assert read(resources / dtype) == {'exhaustion': 0.0, 'message_id': 'outsideBorder',
                                      'scaling': 'when_caused_by_living_non_player'}
    paths.extend([recipe, dtype])
    for tag in ['bypasses_cooldown', 'bypasses_armor', 'bypasses_wolf_armor', 'no_knockback']:
        path = f'data/minecraft/tags/damage_type/{tag}.json'
        assert 'skywars2:duplicating_hole' in read(resources / path)['values']
        paths.append(path)
    java = ['SkyWars2.java'] + ['game/' + name + '.java' for name in
        ['LbcDuplicating', 'LbcWands', 'LbcItems', 'LbcAcademy', 'LbcCraftingRecipe',
         'GameChecks', 'MapManager', 'LbcIndirectKills', 'LbcDamage']]
    with zipfile.ZipFile(ROOT / 'build/libs/skywars2-0.1.0-sources.jar') as sources:
        assert sources.testzip() is None
        for name in java:
            relative = 'me/skywars2/' + name
            assert sources.read(relative) == (ROOT / 'src/main/java' / relative).read_bytes(), name
    with zipfile.ZipFile(artifact) as jar:
        assert jar.testzip() is None
        for path in paths:
            assert jar.read(path) == (resources / path).read_bytes(), path
        assert 'me/skywars2/game/LbcDuplicating.class' in jar.namelist()
    design = audit_design(item_paths, resources)
    old_digest = 'a0c4cb03c4682c99e71e0c197b376f98dc6781e1f153ec7ee61cba67408606a5'
    assert sha(ROOT.parent / 'mods/skywars2.jar') == old_digest
    assert sha(ROOT / 'build/verification/skywars2-before-tanner-movement-a0c4cb03.jar') == old_digest
    assert isinstance(SHARED, list) and SHARED, 'Set exact SHARED sequence from final runner before verification'
    runtime = {}
    for name, expected in [('duplicating-results', PRIMARY), ('consuming-results', CONSUMING),
                           ('duplicating-shared-results', SHARED)]:
        report = read(ROOT / f'build/verification/{name}.json')
        assert report['artifactSha256'] == digest, name
        assert report['completed'] == expected, (name, report['completed'], expected)
        runtime[name] = report
    archive = ROOT / f'build/verification/skywars2-duplicating-{digest[:8]}.jar'
    shutil.copyfile(artifact, archive)
    assert sha(archive) == digest
    proof = {'artifactSha256': digest, 'archive': archive.relative_to(ROOT).as_posix(),
             'fullSourceLootPayloads': names, 'sourceJavaFilesMatched': len(java),
             'sourceJavaPaths': java, 'reusedTemplates': [template],
             'packagedResourceBytesMatched': len(paths), 'crc': 'passed',
             'recipe': 'passed', 'damageTypeAndTags': 'passed', 'design': design,
             'mainJarSha256': old_digest, 'installed': False,
             'completed': {name: len(report['completed']) for name, report in runtime.items()},
             'limits': ['No real-client visual test', 'No mass-load benchmark'], 'runtime': runtime}
    (ROOT / 'build/verification/duplicating-verification.json').write_text(
        json.dumps(proof, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({key: value for key, value in proof.items() if key != 'runtime'}, ensure_ascii=False, indent=2))

if __name__ == '__main__':
    main()
