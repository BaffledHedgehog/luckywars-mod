from pathlib import Path
import hashlib
import json
import re

notes = Path('agent_notes')
p = notes / 'lbc_kit_interface.md'
s = p.read_text(encoding='utf8')
edits = {
    'Вариант 2 удваивает amount первых 24 записей attribute_modifiers': 'Вариант2 заменяет amount первых24 записей attribute_modifiers на `0.1×floor(20×amount)` — примерно удваивает, но округляет вниз до десятых',
    'от позиции глаз и далее на расстоянии 1–16 блоков по взгляду': 'с центрами на расстоянии1–17 блоков от глаз по взгляду: caller сначала сдвигается на1, а helper использует смещения0..16',
    '10100 добавляет skills 28–36 (skill27 в назначенных handlers отсутствует)': '10100 добавляет skills28–36; skill33 в slot5 открыт без advancement, skills25–27 в назначенных selectors отсутствуют',
    'к ближайшему живому игроку': 'к ближайшему игроку не в spectator',
}
for old, new in edits.items():
    assert old in s, old
    s = s.replace(old, new)
p.write_text(s, encoding='utf8')

# Full-file checks of the repetitive multiplier; no prefix-only comparison.
base = Path('world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration')
commands = [x.strip() for x in (base/'items/multiply_2.mcfunction').read_text(encoding='utf8').splitlines() if x.strip() and not x.lstrip().startswith('#')]
expected = ['execute unless data entity @s Item.components."minecraft:attribute_modifiers" run function lbc:swrg_kit_integration/items/attribute_fix_item']
expected += [f'execute store result entity @s Item.components."minecraft:attribute_modifiers"[{i}].amount double 0.1 run data get entity @s Item.components."minecraft:attribute_modifiers"[{i}].amount 20' for i in range(24)]
assert commands == expected
manifest = json.loads((notes/'inventory/lbc_kit_interface_read_manifest.json').read_text(encoding='utf8'))
jobs = json.loads((notes/'inventory/research_jobs.json').read_text(encoding='utf8'))
assert {x['source'] for x in manifest} == set(jobs['lbc_kit_interface']['sources'])
for row in manifest:
    assert hashlib.sha256(Path(row['source']).read_bytes()).hexdigest() == row['sha256'], row['source']
for name, score, function in [('killed_by_bloodshed',3,'bloodshed_player'), ('killed_by_wagnerkilleffect',2,'wagnerhammer')]:
    adv = json.loads(Path(f'world/datapacksdisabled/LBC/data/lbc/advancement/{name}.json').read_text(encoding='utf8'))
    crit = adv['criteria']['requirement']
    assert crit['trigger'] == 'minecraft:entity_killed_player'
    assert crit['conditions']['entity'][0]['predicate']['type'] == 'minecraft:player'
    value = crit['conditions']['entity'][1]
    assert value['range'] == score and value['value']['score'] == 'swrg.kill_effect'
    assert adv['rewards']['function'] == f'lbc:swrg_kit_integration/kill_effects/{function}'
audit = {'sources':146, 'manifest_sha_matches':146, 'multiplier_complete_commands':25,
         'death_advancement_rewards_checked':2, 'errors':[]}
(notes/'inventory/kit_interface_root_audit.json').write_text(json.dumps(audit, indent=2)+'\n', encoding='utf8')
print(json.dumps(audit))
