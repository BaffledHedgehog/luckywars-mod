#!/usr/bin/env python3
"""Extract modern Lifestealer only; preserve the complete legacy item design."""
from __future__ import annotations
import copy
import hashlib
import json
from pathlib import Path
from import_game_resources import migrate_loot_schema
from import_lbc_loadouts import ROOT, SOURCE
from import_lbc_wands import build_single_entry_table, select_ordinary_wand_entry, write_table

PORTED = ROOT / 'src/main/resources/data/lbc/loot_table/ported'
RP = Path('C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3/minecraft/resourcepacks/all_in_one_1.21.8')

def migrated(path):
    payload = json.loads(path.read_text(encoding='utf-8-sig'))
    migrate_loot_schema(payload)
    return payload

def payloads():
    source = SOURCE / 'loot_table/transformables_staff.json'
    table = migrated(source)
    pool, entry = select_ordinary_wand_entry(table, 'vampire_wand', source, 'lifestealer:1')
    entry = copy.deepcopy(entry)
    entry['name'] = 'skywars2:lifestealer_wand'
    base = build_single_entry_table(table, pool, entry)
    upgraded = copy.deepcopy(base)
    upgraded['pools'][0]['entries'][0]['name'] = 'skywars2:academy_lifestealer_wand'
    functions = upgraded['pools'][0]['entries'][0]['modifier']
    data = next(fn for fn in functions if fn.get('type') == 'minecraft:set_custom_data')
    assert data['tag'] == '{transformable:1,staff:1,magicwand:1,lifestealer:1}'
    data['tag'] = data['tag'][:-1] + ',upgradable:0,upgrade_available:1,upgraded:1,mode:1}'
    for name in ('add_upgrade_desc', 'tiers/a'):
        modifier = migrated(SOURCE / f'item_modifier/{name}.json')
        functions.extend(modifier if isinstance(modifier, list) else [modifier])
    return {'lifestealer': base, 'lifestealer_upgraded': upgraded}

def audit_design(payload):
    keys = {'upgrade_condition_lifestealer', 'upgrade_succesful_lifestealer', 'player_vampirism', 'mode_caster', 'mode_charge'}
    models, tiers = set(), set()
    def visit(value):
        if isinstance(value, list):
            for child in value: visit(child)
        elif isinstance(value, dict):
            if 'translate' in value: keys.add(value['translate'])
            if 'minecraft:item_model' in value: models.add(value['minecraft:item_model'])
            if 'minecraft:tooltip_style' in value: tiers.add(value['minecraft:tooltip_style'])
            for child in value.values(): visit(child)
    visit(payload)
    for language in ('ru_ru', 'en_us'):
        path = RP / f'assets/minecraft/lang/{language}.json'
        before = hashlib.sha256(path.read_bytes()).hexdigest()
        data = json.loads(path.read_text(encoding='utf-8-sig'))
        assert not keys - data.keys(), (language, sorted(keys - data.keys()))
        assert hashlib.sha256(path.read_bytes()).hexdigest() == before
    for identifier in models:
        namespace, name = identifier.split(':')
        item = RP / f'assets/{namespace}/items/{name}.json'
        assert item.is_file(), identifier
        model = json.loads(item.read_text(encoding='utf-8-sig'))['model']['model']
        ns, name = model.split(':')
        assert (RP / f'assets/{ns}/models/{name}.json').is_file(), model
    for identifier in tiers:
        namespace, name = identifier.split(':')
        assert (RP / f'assets/{namespace}/textures/gui/sprites/tooltip/{name}_background.png').is_file(), identifier
        assert (RP / f'assets/{namespace}/textures/gui/sprites/tooltip/{name}_frame.png').is_file(), identifier
    return len(keys), sorted(models), sorted(tiers)

def main():
    expected = payloads()
    design = audit_design(list(expected.values()))
    for name, payload in expected.items():
        path = PORTED / f'{name}.json'
        write_table(path, payload)
        assert json.loads(path.read_text(encoding='utf-8')) == payload
    assert payloads() == expected
    print(f'Verified two complete Lifestealer payloads; {design[0]} RU/EN keys; models {design[1]}; tiers {design[2]}. No RP writes.')

if __name__ == '__main__':
    main()
