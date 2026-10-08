"""Read-only verification of repetitive source families and copied appendices."""
import json
import re
from pathlib import Path

base = Path('world/datapacksdisabled/swarogskywars/data/swrg/function')
report = {'errors': [], 'families': {}}

def commands(rel):
    return [l.strip() for l in (base / (rel + '.mcfunction')).read_text(encoding='utf-8').splitlines()
            if l.strip() and not l.lstrip().startswith('#')]

def check(name, actual, expected):
    report['families'][name] = {'commands': len(actual), 'verified': actual == expected}
    if actual != expected:
        report['errors'].append(name)

for name in ['set_count', 'set_count_entity']:
    actual = commands('game/chests/' + name)
    # Compare every slot line with slot 0, including all punctuation and selectors.
    pattern = r'Slot:\s*\d+b'
    normalized = [re.sub(pattern, 'Slot:0b', l) for l in actual[:27]]
    slots = [int(re.search(r'Slot:\s*(\d+)b', l)[1]) for l in actual[:27]]
    ok = len(actual) == 28 and slots == list(range(27)) and len(set(normalized)) == 1
    report['families'][name] = {'commands': len(actual), 'slots': slots, 'verified': ok,
                                'representative': actual[0], 'cleanup': actual[-1]}
    if not ok:
        report['errors'].append(name)

actual = commands('game/start/2_halloween')
check('halloween_25_identical_summons', actual[:25], [actual[0]] * 25)
report['families']['halloween_tail'] = actual[25:]

prefixes = json.loads(Path('agent_notes/skywars_game_prefixes.json').read_text(encoding='utf-8'))
for name, rows in prefixes['lists'].items():
    actual = commands('game/start/team_prefix/list_' + name)
    start = 'data modify storage swrg:core prefix append value '
    values = [json.loads(l[len(start):]) for l in actual if l.startswith(start)]
    expected = [{k: v for k, v in row.items() if k != 'index'} for row in rows['entries']]
    check('prefix_' + name, values, expected)

chests = json.loads(Path('agent_notes/skywars_chests.json').read_text(encoding='utf-8'))
for name, table in chests['tables'].items():
    source = json.loads(Path(table['source']).read_text(encoding='utf-8'))
    check('chests_' + name, source['pools'], table['pools'])
    if table['pool_count'] != len(source['pools']):
        report['errors'].append('pool_count_' + name)

for name in ['en', 'ru']:
    actual = commands('core/lang/' + name)
    data_rows = [l for l in actual if l.startswith('data ')]
    extras = [l for l in actual if not l.startswith('data ')]
    expected = [f'scoreboard players set #lang swrg.math {2 if name == "en" else 1}',
                'execute as @e[tag=swrg.lobby] run data modify entity @s CustomName set from storage swrg:lang entity.lobby',
                f'function lbc:swrg_kit_integration/lang_{name}']
    check('language_' + name + '_nondata', extras, expected)
    report['families']['language_' + name + '_data'] = data_rows

out = Path('agent_notes/inventory/skywars_variants_audit.json')
out.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
print(json.dumps({'errors': report['errors'], 'families': len(report['families'])}))
