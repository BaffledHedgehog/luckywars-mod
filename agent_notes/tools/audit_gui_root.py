"""Full-command checks for repetitive GUI branches; not a gameplay test."""
import hashlib
import json
import re
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
BASE = ROOT / 'world/datapacksdisabled/swarogskywars/data/swrg'
NOTES = ROOT / 'agent_notes'
jobs = json.loads((NOTES/'inventory/research_jobs.json').read_text(encoding='utf-8'))
def commands(rel):
    return [s.strip() for s in (BASE/rel).read_text(encoding='utf-8').splitlines() if s.strip() and not s.lstrip().startswith('#')]
def predicate(n):
    return {'condition':'minecraft:inverted','term':{'condition':'minecraft:entity_properties','entity':'this','predicate':{'nbt':f'{{EnderItems:[{{Slot:{n}b}}]}}'}}}
for n in range(27):
    assert json.loads((BASE/f'predicate/gui/slot{n}.json').read_text()) == predicate(n)
assert json.loads((BASE/'predicate/gui/0.json').read_text()) == predicate(0)
assert json.loads((BASE/'predicate/gui/use.json').read_text()) == {'condition':'minecraft:any_of','terms':[predicate(n) for n in range(27)]}
expected = [f'execute as @r[scores={{swrg.gui_page={5100+100*k},swrg.roll_timer=0,swrg.case_id={i}}}] positioned 10241024 {i} 10241024 run function swrg:gui/roll/{k}' for k in range(5) for i in range(1,9)]
expected += ['scoreboard players remove @a[scores={swrg.roll_timer=1..}] swrg.roll_timer 1','schedule function swrg:gui/roll/player 1t']
assert commands('function/gui/roll/player.mcfunction') == expected
allocation = []
for k in range(5):
    allocation += [f'execute if entity @s[scores={{swrg.gui_select={k},swrg.keys{k}=1..}}] unless entity @a[scores={{swrg.case_id={i}}}] run scoreboard players set @s swrg.case_id {i}' for i in range(1,9)]
    allocation += [f'scoreboard players set @s[scores={{swrg.gui_select={k},swrg.keys{k}=1..}}] swrg.gui_page {5100+100*k}']
allocation += ['scoreboard players set @s[scores={swrg.gui_select=18}] swrg.gui_page 0','scoreboard players set @s swrg.gui_select -1']
assert commands('function/gui/select/5000.mcfunction') == allocation
prices = [[100,400,700],[150,600,1050],[150,600,1050],[200,800,1400],[100,400,750]]
purchase = []
for k, amounts in enumerate(prices):
    currency = 'swrg.memcoins' if k == 4 else 'swrg.coins'
    for a,(price,count) in enumerate(zip(amounts,[1,5,10])):
        sel=f'@s[scores={{swrg.gui_select={3*k+a},{currency}={price}..}}]'
        purchase += [f'scoreboard players add {sel} swrg.keys{k} {count}',f'playsound minecraft:entity.player.levelup master {sel} ~ ~ ~ 1 2',f'scoreboard players remove {sel} {currency} {price}']
purchase += ['scoreboard players set @s[scores={swrg.gui_select=18}] swrg.gui_page 0','scoreboard players set @s swrg.gui_select -1']
assert commands('function/gui/select/6000.mcfunction') == purchase
assert not commands('function/gui/page/7400_halloween.mcfunction')
assert not commands('function/gui/page/7500_halloween.mcfunction')
json_records=[]
rewardbars=[]
for source in jobs['skywars_gui']['sources']:
    p=ROOT/source
    if p.suffix != '.json': continue
    obj=json.loads(p.read_text(encoding='utf-8'))
    def walk(x):
        if isinstance(x,dict):
            for k,v in x.items():
                if k=='rewardbar': rewardbars.append(v)
                yield from walk(v)
        elif isinstance(x,list):
            for v in x: yield from walk(v)
        elif isinstance(x,str):
            for n in re.findall(r'rewardbar\s*:\s*(\d+)',x): rewardbars.append(int(n))
        yield None
    list(walk(obj))
    json_records.append({'source':source,'sha256':hashlib.sha256(p.read_bytes()).hexdigest(),'contract':obj})
assert 21 not in rewardbars
roll_variants=[]
for k in range(5):
    c=commands(f'function/gui/roll/{k}.mcfunction')
    roll_variants.append({'case':k,'commands':c})
result={'sources':397,'predicate_full_equal':29,'scheduler_full_equal':42,'allocation_full_equal':47,'purchase_full_equal':47,'empty_seasonal_functions':2,'prices':prices,'rewardbars_in_gui_json':sorted(set(rewardbars)),'json_contracts':json_records,'roll_variants':roll_variants,'error_count':0}
(NOTES/'inventory/gui_root_audit.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps({k:v for k,v in result.items() if k not in ('json_contracts','roll_variants')},ensure_ascii=False))
