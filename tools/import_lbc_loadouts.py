"""Read legacy selectors/attributes; emit the supported native loadout catalog only."""
from pathlib import Path
import json
import re
from import_game_resources import migrate_loot_schema

ROOT = Path(__file__).resolve().parents[1]
SOURCE = Path('E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc')
GAME = SOURCE / 'function/swrg_kit_integration/game'
GUI = SOURCE / 'function/swrg_kit_integration/gui/select'


def source(name):
    return (GUI / f'{name}.mcfunction').read_text(encoding='utf-8-sig')


def gate(page, slot):
    text = source(page)
    for line in text.splitlines():
        if re.search(rf'swrg.gui_select(?:=| matches ){slot}(?:\D|$)', line) and 'true_advancements/' in line and 'false' not in line and 'tellraw' not in line:
            match = re.search(r'(lbc:true_advancements/[^=]+)=true', line)
            if match:
                advancement = match[1]
                failure = next((json.loads(line[line.index('{"translate"'):])['translate']
                                for line in text.splitlines()
                                if advancement + '=false' in line and '{"translate"' in line), None)
                assert failure, (page, slot, advancement)
                return dict(gate=advancement, lockedKey=failure)
    return {}


CAST_TRINKETS = [
    (5, 4, 'blood_anal_boom', 'skywars2:blood_anal_boom'),
    (21, 20, 'mana_gem', 'skywars2:mana_gem'),
]


def add_cast_trinkets(groups, import_payloads=False):
    """Catalog the cast dependencies at their actual source IDs and page gates."""
    for ident, slot, name, item in CAST_TRINKETS:
        table_id = f'lbc:ported/{name}'
        matches = [record for record in groups['trinkets'] if record.get('id') == ident]
        if matches:
            assert len(matches) == 1 and matches[0].get('table') == table_id, (
                f'Conflicting trinket catalog entry for source ID {ident}')
        else:
            groups['trinkets'].append(dict(id=ident, page=11000, slot=slot,
                                           table=table_id, **gate(11000, slot)))
        if import_payloads:
            loot = json.loads((SOURCE / f'loot_table/trinket/{name}.json').read_text(encoding='utf-8-sig'))
            migrate_loot_schema(loot)
            loot['pools'][0]['entries'][0]['name'] = item
            destination = ROOT / f'src/main/resources/data/lbc/loot_table/ported/{name}.json'
            destination.parent.mkdir(parents=True, exist_ok=True)
            destination.write_text(json.dumps(loot, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')


def update_catalog_only():
    """Add only the cast trinket records, preserving every existing resource file."""
    target = ROOT / 'src/main/resources/skywars2/lbc_loadouts.json'
    groups = json.loads(target.read_text(encoding='utf-8'))
    add_cast_trinkets(groups)
    target.write_text(json.dumps(groups, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')


def main():
    groups = dict(kits=[], skills=[], levelups=[], trinkets=[])
    for ident, table in [
        (1, 'broken_totem'), (2, '3_purify_potion'), (4, 'life_in_death'),
        (5, 'hell'), (7, 'moisei'), (9, 'nanobot'), (10, 'ghost'),
        (15, 'dynamite'), (17, 'brbad'), (22, 'explosive_feets'), (23, 'fast_sword'),
    ]:
        slot = ident-1 if ident <= 18 else ident
        groups['kits'].append(dict(id=ident, page=9000, slot=slot, table='lbc:kit/'+table,
                                   **gate(9000, slot)))
        # Legacy `functions` is silently ignored by 26.3; reuse the established
        # codec migration so counts, potion components and sword attributes survive.
        loot = json.loads((SOURCE / f'loot_table/kit/{table}.json').read_text(encoding='utf-8-sig'))
        migrate_loot_schema(loot)
        if ident == 22:
            # Server identity differs; Polymer keeps the original chainmail-boots client appearance.
            loot['pools'][0]['entries'][0]['name'] = 'skywars2:rocket_boots'
        if ident == 17:
            for pool, item in zip(loot['pools'], ['skywars2:termit', 'skywars2:sulfur', 'skywars2:phosphin', 'skywars2:gremuch'], strict=True):
                pool['entries'][0]['name'] = item
        destination = ROOT / f'src/main/resources/data/lbc/loot_table/kit/{table}.json'
        destination.parent.mkdir(parents=True, exist_ok=True)
        destination.write_text(json.dumps(loot, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
    modifier = json.loads((SOURCE / 'item_modifier/add_lore_rocket.json').read_text(encoding='utf-8-sig'))
    migrate_loot_schema(modifier)
    # Registry entries require an object; arrays are only accepted by the inline loot codec.
    if isinstance(modifier, list):
        modifier = {'type': 'minecraft:sequence', 'functions': modifier}
    modifier_destination = ROOT / 'src/main/resources/data/lbc/item_modifier/add_lore_rocket.json'
    modifier_destination.parent.mkdir(parents=True, exist_ok=True)
    modifier_destination.write_text(json.dumps(modifier, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
    # Purifier kit references a shared item table; import just this dependency.
    loot = json.loads((SOURCE / 'loot_table/purifypotion_1.json').read_text(encoding='utf-8-sig'))
    migrate_loot_schema(loot)
    (ROOT / 'src/main/resources/data/lbc/loot_table/purifypotion_1.json').write_text(
        json.dumps(loot, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
    groups['skills'].append(dict(id=5, page=10000, slot=4, **gate(10000, 4)))
    # Keep the full source entity preset, including all five firework explosions and LifeTime.
    rocket_source = (SOURCE / 'function/items/trinket_effects/charge_firework.mcfunction').read_text(encoding='utf-8-sig')
    rocket_preset = rocket_source.splitlines()[0].partition(' ~ ~ ~ ')[2]
    assert rocket_preset.startswith('{') and rocket_preset.endswith('}')
    (ROOT / 'src/main/resources/skywars2/ghast_rocket.snbt').write_text(rocket_preset+'\n', encoding='utf-8')
    for ident, slot, table, item in [
        (1, 0, 'blaze', 'skywars2:blaze_trinket'),
        (2, 1, 'ghast', 'skywars2:ghast_trinket'),
        (10, 9, 'intercal', 'skywars2:intercal'),
        (16, 15, 'gneiss_stone', 'skywars2:gneiss_stone'),
        (18, 17, 'snow_gem', 'skywars2:snow_gem'),
        (20, 19, 'ice_gem', 'skywars2:ice_gem'),
    ]:
        groups['trinkets'].append(dict(id=ident, page=11000, slot=slot,
                                        table=f'lbc:trinket/{table}', **gate(11000, slot)))
        loot = json.loads((SOURCE / f'loot_table/trinket/{table}.json').read_text(encoding='utf-8-sig'))
        migrate_loot_schema(loot)
        loot['pools'][0]['entries'][0]['name'] = item
        destination = ROOT / f'src/main/resources/data/lbc/loot_table/trinket/{table}.json'
        destination.parent.mkdir(parents=True, exist_ok=True)
        destination.write_text(json.dumps(loot, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
    add_cast_trinkets(groups, import_payloads=True)
    supported = {3,4,5,6,7,8,9,10,12,13,15,16,18,20,21,22,23,24} | set(range(33,45))
    raw = (GAME / 'levelup_select/select_1.mcfunction').read_text(encoding='utf-8-sig')
    pattern = r'matches (\d+) run attribute @s (\S+) modifier add (\S+) ([\d.-]+) (\S+)'
    modifiers = {}
    for ident, attribute, key, amount, operation in re.findall(pattern, raw):
        modifiers.setdefault(int(ident), []).append(dict(attribute=attribute, key=key, amount=float(amount), operation=operation))
    for ident in sorted(supported):
        page, slot = (8000, ident-1) if ident <= 24 else (8100, ident-28)
        groups['levelups'].append(dict(id=ident, page=page, slot=slot, modifiers=modifiers[ident], **gate(page, slot)))
    target = ROOT / 'src/main/resources/skywars2/lbc_loadouts.json'
    target.write_text(json.dumps(groups, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
    print({group: len(entries) for group, entries in groups.items()})


if __name__ == '__main__':
    main()
