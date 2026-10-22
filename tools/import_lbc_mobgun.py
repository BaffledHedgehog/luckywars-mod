#!/usr/bin/env python3
"""Extract Mobgun; apply authorized 2.5s lore and single native potion fixes."""
from __future__ import annotations
import copy
import hashlib
import json
import re
from pathlib import Path
from import_game_resources import migrate_loot_schema
from import_lbc_loadouts import ROOT, SOURCE
from import_lbc_wands import build_single_entry_table, write_table

RP = Path('C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3/minecraft/resourcepacks/all_in_one_1.21.8')
RESOURCES = ROOT / 'src/main/resources'

def payloads():
    table = json.loads((SOURCE / 'loot_table/transformables_shooter.json').read_text(encoding='utf-8-sig'))
    migrate_loot_schema(table)
    matches = [(pool, entry) for pool in table['pools'] for entry in pool['entries']
               if 'mobgun:1' in json.dumps(entry)]
    assert len(matches) == 2
    pool, entry = next((p, e) for p, e in matches if e.get('weight') == 10)
    other = matches[1][1] if matches[0][1] is entry else matches[0][1]
    assert entry['modifier'] == other['modifier']
    entry = copy.deepcopy(entry); entry['name'] = 'skywars2:mobgun'
    # User decision: retain actual source 50 ticks and make the displayed time agree.
    lore = next(fn for fn in entry['modifier'] if fn.get('type') == 'minecraft:set_lore')['lore']
    assert lore[1][0]['text'] == '3.5 '
    lore[1][0]['text'] = '2.5 '
    loot = build_single_entry_table(table, pool, entry)
    text = (SOURCE / 'function/workingitems/mobgun.mcfunction').read_text(encoding='utf-8-sig')
    pattern = r'^execute anchored eyes if score #random105 lbc.math matches (\d+) positioned \^ \^ \^ run summon (\S+) \^ \^ \^(\S+) (\{.*\})$'
    outcomes = [{'outcome': int(number), 'offset': float(offset), 'nbt': '{id:"' + entity + '",' + nbt[1:]}
                for number, entity, offset, nbt in re.findall(pattern, text, re.MULTILINE)]
    assert len(outcomes) == 113 and {x['outcome'] for x in outcomes} == set(range(1, 115)) - {75}
    potion_text = (SOURCE / 'function/workingitems/mobgun_random_lingering_potion.mcfunction').read_text(encoding='utf-8-sig')
    pattern = r'^execute anchored eyes if score #random86 lbc.math matches (\d+) run summon (\S+) ~ ~ ~ (\{.*\})$'
    potions = [{'outcome': int(number), 'nbt': '{id:"' + entity + '",' + nbt[1:]}
               for number, entity, nbt in re.findall(pattern, potion_text, re.MULTILINE)]
    assert len(potions) == 89 and {x['outcome'] for x in potions} == set(range(1, 87))
    # Each source item/effect combination becomes one equally likely shot. The four
    # branches sharing legacy score 43 remain four independent catalog entries.
    combos = set()
    for index, potion in enumerate(potions, 1):
        item = re.search(r'Item:\{id:"(minecraft:(?:lingering|splash)_potion)"', potion['nbt']).group(1)
        effect = re.search(r'potion:"(minecraft:[^"]+)"', potion['nbt']).group(1)
        assert (item, effect) not in combos
        combos.add((item, effect))
        potion['sourceOutcome'] = potion['outcome']
        potion['outcome'] = index
        potion['nbt'] = potion['nbt'].replace('{id:"minecraft:lingering_potion",', '{id:"' + item + '",', 1)
    assert len(combos) == 89
    assert {effect for item, effect in combos if item == 'minecraft:lingering_potion'} >= {
        'minecraft:weaving', 'minecraft:wind_charged', 'minecraft:oozing'}
    # These five types were appended to the legacy file after 1.21.8. They only
    # carry id/Tags, whose native 26.3 schema is unchanged; older DFU choices
    # cannot decode their newer entity IDs.
    for entry in outcomes:
        if 110 <= entry['outcome'] <= 114:
            assert re.fullmatch(r'\{id:"minecraft:[a-z_]+",Tags:\["raycastmob","prestartf"\]\}', entry['nbt'])
            entry['nativeFormat'] = True
    catalog = {'originalDataVersion': 4440, 'outcomes': outcomes, 'potions': potions}
    return loot, catalog

def audit_design(loot):
    keys = {'mob_gun', 'mob_gun.lore', 'type', 'shooter', 'recharge_time', 'lbc.adv.shooter.mob_gun', 'lbc.adv.shooter.mob_gun.lore'}
    for language in ('ru_ru', 'rpr', 'en_us'):
        path = RP / f'assets/minecraft/lang/{language}.json'
        before = hashlib.sha256(path.read_bytes()).hexdigest()
        data = json.loads(path.read_text(encoding='utf-8-sig'))
        assert not keys - data.keys()
        assert hashlib.sha256(path.read_bytes()).hexdigest() == before
    item = json.loads((RP / 'assets/lbc/items/mobgun.json').read_text(encoding='utf-8-sig'))
    assert item['model']['model'] == 'lbc:item/mobgun'
    model = json.loads((RP / 'assets/lbc/models/item/mobgun.json').read_text(encoding='utf-8-sig'))
    assert model['textures']['layer0'] == 'minecraft:item/mobgun'
    assert (RP / 'assets/minecraft/textures/item/mobgun.png').is_file()
    assert 'lbc:tier_d' in json.dumps(loot)

def main():
    loot, catalog = payloads(); audit_design(loot)
    for path, payload in ((RESOURCES / 'data/lbc/loot_table/ported/mobgun.json', loot),
                          (RESOURCES / 'skywars2/mobgun_presets.json', catalog)):
        write_table(path, payload)
        assert json.loads(path.read_text(encoding='utf-8')) == payload
    assert payloads() == (loot, catalog)
    print('Verified Mobgun full source design, 114 outcomes and 89 uniform single native potion combinations. No RP writes.')

if __name__ == '__main__':
    main()
