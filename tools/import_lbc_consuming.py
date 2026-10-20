#!/usr/bin/env python3
"""Import source Consuming, its two real donors and Academy entity payloads."""
from __future__ import annotations
import hashlib
import json
import re
from collections import deque

from import_game_resources import migrate_loot_schema
from import_lbc_fireblaze import PORTED, import_item, select_entry
from import_lbc_loadouts import ROOT, SOURCE
from import_lbc_wands import import_wand_family, build_single_entry_table, write_table
from import_lbc_reforge_resources import dependencies, migrate_score_attribute_amounts
from import_lbc_weapons import migrate_legacy_score_value_checks

RESOURCES = ROOT / 'src/main/resources'
RP = ROOT.__class__('C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8')
TRIAL = SOURCE / 'function/workingitems/awaken_tuntija_wands/consuming_wand_upgrade.mcfunction'
NAMES = ('consuming_wand', 'academy_consuming_wand', 'creeper_army_spell', 'destroyer_spell')


def migrated(path):
    data = json.loads(path.read_text(encoding='utf-8-sig'))
    migrate_loot_schema(data)
    return data


def import_presets():
    matches = re.findall(r'^summon (minecraft:skeleton) (\S+) (\S+) (\S+) (\{.*\})$',
                         TRIAL.read_text(encoding='utf-8-sig'), re.MULTILINE)
    if len(matches) != 28 or len({(m[0], m[4]) for m in matches}) != 1:
        raise SystemExit('Expected exactly 28 identical source Void Man payloads')
    def offset(value):
        if not value.startswith('~'):
            raise SystemExit('Expected relative source summon coordinates')
        return float(value[1:] or 0)
    entity, _, _, _, nbt = matches[0]
    payload = {'originalDataVersion': 4440,
               'presets': {'void_man': '{id:"' + entity + '",' + nbt[1:]},
               'spawns': [{'preset': 'void_man', 'offset': [offset(v) for v in m[1:4]]}
                          for m in matches]}
    path = RESOURCES / 'skywars2/consuming_presets.json'
    write_table(path, payload)
    assert json.loads(path.read_text(encoding='utf-8')) == payload
    return path


def import_loot_closure():
    queue = deque([('loot_table', 'voidman')]); seen = set(); paths = []
    while queue:
        kind, name = queue.popleft()
        if (kind, name) in seen:
            continue
        seen.add((kind, name))
        relative = f'{kind}/{name}.json'
        data = json.loads((SOURCE / relative).read_text(encoding='utf-8-sig'))
        queue.extend(dependencies(data, kind))
        migrate_loot_schema(data)
        migrate_legacy_score_value_checks(data)
        migrate_score_attribute_amounts(data)
        destination = RESOURCES / 'data/lbc' / relative
        if not destination.exists() or json.loads(destination.read_text(encoding='utf-8-sig')) != data:
            write_table(destination, data)
        assert json.loads(destination.read_text(encoding='utf-8-sig')) == data
        paths.append(destination)
    return paths


def verify_sources():
    for name, source, marker in (
        ('consuming_wand', SOURCE / 'loot_table/tuntijaa_greed_destroy.json', 'consuming_wand:1'),
        ('creeper_army_spell', SOURCE / 'loot_table/transformables_spells.json', 'creeperz:1'),
        ('destroyer_spell', SOURCE / 'loot_table/transformables_spells.json', 'trap2:1'),
    ):
        table = migrated(source)
        pool, entry = select_entry(table, name, marker)
        entry = json.loads(json.dumps(entry)); entry['name'] = 'skywars2:' + name
        assert json.loads((PORTED / f'{name}.json').read_text(encoding='utf-8')) == build_single_entry_table(table, pool, entry)
    base = json.loads((PORTED / 'consuming_wand.json').read_text(encoding='utf-8'))
    expected = json.loads(json.dumps(base)); entry = expected['pools'][0]['entries'][0]
    entry['name'] = 'skywars2:academy_consuming_wand'
    functions = entry['modifier']
    custom = next(fn for fn in functions if fn.get('type') == 'minecraft:set_custom_data')
    custom['tag'] = custom['tag'].rstrip('}') + ',upgradable:0,upgrade_available:1,upgraded:1,mode:1}'
    for name in ('add_upgrade_desc', 'tiers/x'):
        modifier = migrated(SOURCE / f'item_modifier/{name}.json')
        functions.extend(modifier if isinstance(modifier, list) else [modifier])
    assert json.loads((PORTED / 'academy_consuming_wand.json').read_text(encoding='utf-8')) == expected


def audit_design(paths):
    keys = {'upgrade_condition_consuming', 'upgrade_succesful_consuming', 'mode_default',
            'mode_silence', 'mode_the_end', 'mode_avarice', 'void_man'}
    models = set(); equipment = {'lbc:void'}
    def visit(value):
        if isinstance(value, list):
            for child in value: visit(child)
        elif isinstance(value, dict):
            if 'translate' in value: keys.add(value['translate'])
            if 'minecraft:item_model' in value: models.add(value['minecraft:item_model'])
            for child in value.values(): visit(child)
    for path in paths: visit(json.loads(path.read_text(encoding='utf-8-sig')))
    languages = {}
    for language in ('ru_ru', 'en_us'):
        path = RP / f'assets/minecraft/lang/{language}.json'
        data = json.loads(path.read_text(encoding='utf-8-sig'))
        missing = sorted(keys - data.keys())
        if missing: raise SystemExit(f'Missing {language} keys: {missing}')
        languages[language] = {'sha256': hashlib.sha256(path.read_bytes()).hexdigest(), 'allKeysPresent': True}
    for identifier in models:
        namespace, name = identifier.split(':')
        assert (RP / f'assets/{namespace}/items/{name}.json').exists(), identifier
    for identifier in equipment:
        namespace, name = identifier.split(':')
        assert (RP / f'assets/{namespace}/equipment/{name}.json').exists(), identifier
    return {'keys': sorted(keys), 'models': sorted(models), 'equipment': sorted(equipment), 'languages': languages}


def main():
    import_wand_family(SOURCE / 'loot_table/tuntijaa_greed_destroy.json',
                       PORTED / 'consuming_wand.json', PORTED / 'academy_consuming_wand.json',
                       'consuming_wand', 'skywars2:consuming_wand', 'skywars2:academy_consuming_wand',
                       academy_modifiers=('add_upgrade_desc', 'tiers/x'), data_marker='consuming_wand:1')
    for name, marker in (('creeper_army_spell', 'creeperz:1'), ('destroyer_spell', 'trap2:1')):
        import_item(SOURCE / 'loot_table/transformables_spells.json', PORTED / f'{name}.json',
                    name, marker, 'skywars2:' + name)
    paths = [PORTED / f'{name}.json' for name in NAMES]
    paths.append(import_presets()); paths.extend(import_loot_closure())
    verify_sources()
    templates = {}
    for name in ('meditation', 'air11x11', *(f'silence_{i}' for i in range(1, 9))):
        path = RESOURCES / f'data/lbc/structure/{name}.nbt'
        assert path.read_bytes() == (SOURCE / f'structure/{name}.nbt').read_bytes()
        templates[path.relative_to(RESOURCES).as_posix()] = hashlib.sha256(path.read_bytes()).hexdigest()
    report = {'paths': sorted(path.relative_to(RESOURCES).as_posix() for path in paths),
              'sha256': {path.relative_to(RESOURCES).as_posix(): hashlib.sha256(path.read_bytes()).hexdigest() for path in paths},
              'reusedTemplates': templates, 'sourceComparison': 'complete payload equality passed',
              'voidManSpawns': 28, 'design': audit_design([path for path in paths if path.name != 'consuming_presets.json'])}
    write_table(ROOT / 'agent_notes/CONSUMING_RESOURCE_MANIFEST.json', report)
    print('Verified four complete item payloads, 28 source spawn offsets, two-table loot closure, ten reused templates and RP design keys/models')


if __name__ == '__main__':
    main()

