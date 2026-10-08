"""Disjoint source assignments for incremental researcher batches."""
import json
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
NOTES = ROOT / 'agent_notes'


def classify(row):
    pack, rel = row['pack'], row['relative']
    if pack == 'maps' or row['suffix'] == '.nbt':
        return 'maps_catalogue'
    if pack == 'swarogskywars':
        if rel.startswith(('data/swrg/function/core/', 'data/swrg/function/game/', 'data/swrg/function/lobby/')) or rel == 'data/minecraft/tags/function/load.json':
            return 'skywars_game'
        if rel.startswith(('data/swrg/function/gui/', 'data/swrg/loot_table/gui/', 'data/swrg/predicate/gui/', 'data/swrg/item_modifier/')):
            return 'skywars_gui'
        if rel.startswith(('data/swrg/function/maploader/', 'data/swrg/function/developer/')):
            return 'skywars_maps_admin'
        return 'skywars_support'
    if rel.startswith('data/lbcr/'):
        return 'lbc_lbcr'
    if not rel.startswith('data/lbc/function/'):
        if rel.startswith('data/lbc/loot_table/uncrafted/'):
            return 'lbc_loot_uncrafted'
        if rel.startswith('data/lbc/loot_table/'):
            return 'lbc_loot_definitions'
        if rel.startswith('data/lbc/advancement/'):
            return 'lbc_advancements'
        if rel.startswith(('data/lbc/predicate/', 'data/lbc/item_modifier/')):
            return 'lbc_predicates_modifiers'
        return 'lbc_registry_support'
    tail = rel[len('data/lbc/function/'):]
    if '/' not in tail:
        return 'lbc_runtime'
    family, rest = tail.split('/', 1)
    if family == 'drops':
        return 'lbc_drop_effects'
    if family in {'armor', 'bedwars', 'effects', 'projectiles', 'raycast', 'ui'}:
        return 'lbc_' + family
    if family == 'items':
        item_family = rest.split('/')[0]
        if item_family in {'wands', 'swords', 'bows', 'crossbow', 'shooter', 'shields'}:
            return 'lbc_items_weapons'
        return 'lbc_items_arcana_trinkets'
    if family == 'swrg_kit_integration':
        return 'lbc_kit_game' if rest.startswith('game/') else 'lbc_kit_interface'
    if family == 'workingitems':
        subfamily = rest.split('/')[0]
        if '/' not in rest:
            return 'lbc_workingitems_main'
        if subfamily == 'awaken_tuntija_wands':
            return 'lbc_workingitems_awaken'
        if subfamily in {'gaycaster', 'witch_gens', 'sauvojen_staffs', 'sauvojen_staffs_tiny'}:
            return 'lbc_workingitems_generators'
        return 'lbc_workingitems_other'
    if family == 'other':
        subfamily = rest.split('/')[0]
        if '/' not in rest:
            return 'lbc_other_main'
        if subfamily in {'attributes', 'drop_inv', 'new_game'}:
            return 'lbc_other_attributes'
        if subfamily in {'magic_academy', 'grimoire_dungeon', 'sauvojen_tuntija'}:
            return 'lbc_other_magic'
        if subfamily in {'imba', 'god_hedgehog', 'artifact'}:
            return 'lbc_other_imba'
        if subfamily in {'event', 'halloween', 'bucks', 'troll_box'}:
            return 'lbc_other_events'
        if subfamily in {'craft', 'eatables', 'mana_potions', 'negative_mana', 'greed', 'kapusta'}:
            return 'lbc_other_consumables_craft'
        if subfamily in {'nexus', 'techno', 'wisp_book', 'railroad', 'nuke'}:
            return 'lbc_other_world_tech'
        return 'lbc_other_misc'
    return 'unassigned'


def main():
    rows = json.loads((NOTES / 'inventory/files.json').read_text(encoding='utf-8'))
    groups = defaultdict(list)
    for row in rows:
        groups[classify(row)].append(row['source'])
    out = NOTES / 'inventory/research_jobs.json'
    previous = json.loads(out.read_text(encoding='utf-8')) if out.exists() else {}
    jobs = {}
    for name, sources in sorted(groups.items()):
        old = previous.get(name, {})
        jobs[name] = {'status': old.get('status', 'pending'), 'agent': old.get('agent'),
                      'documents': old.get('documents', []), 'sources': sources}
    for name in ('maps_catalogue', 'skywars_game', 'lbc_drop_effects'):
        if name not in previous:
            jobs[name]['status'] = 'researching'
            jobs[name]['agent'] = '/root/' + name
    out.write_text(json.dumps(jobs, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({name: len(job['sources']) for name, job in jobs.items()}, ensure_ascii=False))


if __name__ == '__main__':
    main()
