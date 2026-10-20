"""Final KFC evidence audit. Run only after both runtime reports pass."""
from __future__ import annotations
import copy
import hashlib
import json
import shutil
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'build/verification'
RES = ROOT / 'src/main/resources'
sys.path.insert(0, str(ROOT / 'tools'))
from import_game_resources import migrate_loot_schema
from import_lbc_consuming import RP
from import_lbc_loadouts import SOURCE
from import_lbc_fireblaze import select_entry
from import_lbc_wands import build_single_entry_table

def read(path):
    return json.loads(path.read_text(encoding='utf-8-sig'))

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def migrated(name):
    value = read(SOURCE / f'loot_table/{name}.json')
    migrate_loot_schema(value)
    return value

def walk(value):
    if isinstance(value, dict):
        yield value
        for child in value.values():
            yield from walk(child)
    elif isinstance(value, list):
        for child in value:
            yield from walk(child)

def main():
    artifact = ROOT / 'build/libs/skywars2-0.1.0.jar'
    digest = sha(artifact)
    runtime = {}
    # Gate before writing archive/proof; failed or partial runs are never proof.
    for name in ('kfc-results', 'kfc-shared-results'):
        report = read(OUT / f'{name}.json')
        assert report.get('passed') is True, f'{name}: runtime has not passed'
        assert report['artifactSha256'] == digest, f'{name}: stale artifact'
        assert isinstance(report.get('completed'), list) and report['completed'], name
        runtime[name] = report
    native = runtime['kfc-results']['completed']
    native_log=(OUT/'kfc.log').read_text(encoding='utf-8')
    assert '[check] FAIL' not in native_log
    for suite in ('kfc','kfc-food','kfc-rewards','kfc-scores','kfc-cultus','kfc-stop-probe','kfc-cleanup'):
        assert sum(line.endswith('[check] PASS '+suite) for line in native_log.splitlines())==2, suite
    assert len(native) == 5, native
    for match in (1, 2):
        assert sum(str(check).startswith(f'Match {match}:') for check in native) == 2, native
    raw_log = (OUT / 'kfc-build.log').read_bytes()
    log = raw_log.decode('utf-16') if raw_log.startswith((b'\xff\xfe', b'\xfe\xff')) else raw_log.decode('utf-8-sig')
    assert 'BUILD SUCCESSFUL' in log and 'BUILD FAILED' not in log

    table = migrated('transformables_shooter')
    pool, entry = select_entry(table, 'kfc_gun', 'kfcgun:1')
    gun = build_single_entry_table(table, pool, copy.deepcopy(entry))
    gun['pools'][0]['entries'][0]['name'] = 'skywars2:kfc_gun'
    chicken = migrated('kfc_yumyumyummy')
    entries = [entry for pool in chicken['pools'] for entry in pool['entries']]
    assert len(entries) == 1 and entries[0]['name'] == 'minecraft:cooked_chicken'
    entries[0]['name'] = 'skywars2:kfc_chick'
    payloads = {'ported/kfc_gun': gun, 'ported/kfc_chick': chicken, 'kfc_yumyumyummy': chicken}
    for name in ('govno', 'govno_eaten', 'govno_lot', 'govno_lot_2', 'win_button', 'penis_cultus'):
        payload = migrated(name)
        if name in ('govno_eaten', 'win_button', 'penis_cultus'):
            payload['pools'][0]['entries'][0]['name'] = f'skywars2:{name}'
        payloads[name] = payload
    paths = []
    references = set()
    for name, expected in payloads.items():
        path = f'data/lbc/loot_table/{name}.json'
        assert read(RES / path) == expected, path
        paths.append(path)
        for node in walk(expected):
            if node.get('type') in ('minecraft:loot_table', 'loot_table'):
                reference = node.get('value', node.get('name'))
                if isinstance(reference, str) and reference.startswith('lbc:'):
                    references.add(reference[4:])
    assert not references - payloads.keys(), f'Unverified loot dependencies: {references - payloads.keys()}'

    keys = {'penis'}
    models = {f'lbc:penis_{i}' for i in range(1, 7)}
    styles = set()
    for node in walk(list(payloads.values())):
        if 'translate' in node:
            keys.add(node['translate'])
        if 'minecraft:item_model' in node:
            models.add(node['minecraft:item_model'])
        if 'minecraft:tooltip_style' in node:
            styles.add(node['minecraft:tooltip_style'])
    languages = {}
    for language in ('ru_ru', 'en_us'):
        path = RP / f'assets/minecraft/lang/{language}.json'
        assert not keys - read(path).keys(), (language, sorted(keys - read(path).keys()))
        languages[language] = sha(path)
    for identifier in models:
        namespace, name = identifier.split(':', 1)
        assert (RP / f'assets/{namespace}/items/{name}.json').is_file(), identifier
        model = read(RP / f'assets/{namespace}/models/item/{name}.json')
        for texture in model.get('textures', {}).values():
            if not texture.startswith('#'):
                ns, value = texture.split(':', 1)
                assert (RP / f'assets/{ns}/textures/{value}.png').is_file(), texture
    for identifier in styles:
        namespace, name = identifier.split(':', 1)
        for suffix in ('background', 'frame'):
            assert (RP / f'assets/{namespace}/textures/gui/sprites/tooltip/{name}_{suffix}.png').is_file()

    damage = 'data/skywars2/damage_type/cultus_arrow.json'
    assert read(RES / damage) == {'exhaustion': .1, 'message_id': 'arrow', 'scaling': 'when_caused_by_living_non_player'}
    paths.append(damage)
    for tag in ('bypasses_cooldown', 'is_projectile'):
        path = f'data/minecraft/tags/damage_type/{tag}.json'
        data = read(RES / path)
        assert 'skywars2:cultus_arrow' in data['values'] and data.get('replace', False) is False
        if tag == 'bypasses_cooldown':
            assert 'minecraft:arrow' not in data['values'], 'Vanilla arrow cooldown changed globally'
        paths.append(path)
    paths += ['data/minecraft/tags/damage_type/arrow_only.json',
              'data/minecraft/tags/damage_type/acceptable_for_manavampirism.json', 'skywars2.mixins.json']
    assert {'KfcLightningMixin$Targets', 'KfcLightningMixin$Impact'} <= set(read(RES / 'skywars2.mixins.json')['mixins'])
    java = ['SkyWars2.java'] + ['game/' + name + '.java' for name in (
        'GameController', 'MatchEvents', 'LbcItems', 'LbcWands', 'LbcShooters', 'LbcBlaze', 'LbcDamage',
        'LbcKfcFood', 'LbcKfcRewards', 'LbcElectro', 'LbcPsycho', 'LbcTerra', 'LbcMagic',
        'LbcIndirectKills', 'OriginalGuiDesign', 'GameChecks')] + ['mixin/KfcLightningMixin.java']
    with zipfile.ZipFile(ROOT / 'build/libs/skywars2-0.1.0-sources.jar') as sources:
        assert sources.testzip() is None
        for name in java:
            relative = 'me/skywars2/' + name
            assert sources.read(relative) == (ROOT / 'src/main/java' / relative).read_bytes(), name
    with zipfile.ZipFile(artifact) as jar:
        assert jar.testzip() is None
        for path in paths:
            assert jar.read(path) == (RES / path).read_bytes(), path
        for name in ('LbcKfcFood', 'LbcKfcRewards'):
            assert f'me/skywars2/game/{name}.class' in jar.namelist()
    old_digest = 'a0c4cb03c4682c99e71e0c197b376f98dc6781e1f153ec7ee61cba67408606a5'
    assert sha(ROOT.parent / 'mods/skywars2.jar') == old_digest
    archive = OUT / f'skywars2-kfc-{digest[:8]}.jar'
    shutil.copyfile(artifact, archive)
    assert sha(archive) == digest
    proof = {'artifactSha256': digest, 'archive': archive.relative_to(ROOT).as_posix(),
             'sourceJavaPaths': java, 'sourceJavaFilesMatched': len(java),
             'completeSourceLootPayloads': sorted(payloads), 'lootReferences': sorted(references),
             'packagedResourcePaths': paths, 'crc': 'passed', 'build': 'SUCCESSFUL',
             'design': {'resourcePack': str(RP), 'keys': sorted(keys), 'models': sorted(models),
                        'tooltipStyles': sorted(styles), 'languageSha256': languages, 'readOnly': True},
             'mainJarSha256': old_digest, 'installed': False, 'runtime': runtime,
             'limits': ['No real-client visual test', 'No mass-load/MSPT benchmark']}
    (OUT / 'kfc-verification.json').write_text(json.dumps(proof, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({key: value for key, value in proof.items() if key != 'runtime'}, ensure_ascii=False, indent=2))

if __name__ == '__main__':
    main()
