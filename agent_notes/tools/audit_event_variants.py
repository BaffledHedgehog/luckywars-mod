"""Read whole curse-slot bodies and check the complete 100-criterion dispatcher."""
from pathlib import Path
import hashlib,json,re
r=Path(__file__).resolve().parents[2]
d=r/'world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween'
errors=[];rows=[]
def commands(p):
 return [l.strip() for l in p.read_text(encoding='utf-8-sig').splitlines() if l.strip() and not l.lstrip().startswith('#')]
b='10241024 166 10241024'
for p in sorted((d/'pumpkin_king_sword/slot').glob('*.mcfunction')):
 actual=commands(p);i=0
 random=re.fullmatch(r'execute store result score t lbc.math run random value 1\.\.(\d+)',actual[0])
 n=int(random[1]) if random else 1
 if random:i+=1
 source=re.fullmatch('item replace block '+b+r' container\.0 from entity @s ([\w.]+)',actual[i])
 if not source:errors.append(p.name+':source');continue
 slot=source[1];expected=actual[:i]+[actual[i]];i+=1;targets=[]
 for k in range(1,n+1):
  prefix=f'execute if score t lbc.math matches {k} run ' if random else ''
  pat=re.escape(prefix+'item replace block '+b+' container.1 from entity @s ')+r'([\w.]+)'
  m=re.fullmatch(pat,actual[i]);i+=1
  if not m:errors.append(p.name+':target');break
  targets.append(m[1]);expected.append(m[0])
 expected.append(f'execute if items block {b} container.1 * run data modify block {b} Items[0].components."minecraft:custom_data".storage append from block {b} Items[1]');i+=1
 if slot=='container.9':
  expected.append(f'data modify block {b} Items[0].components."minecraft:equippable" set value '+ '{"slot":"feet"}')
 for k,target in enumerate(targets,1):
  prefix=f'execute if score t lbc.math matches {k} run ' if random else ''
  if slot.startswith('armor.') or slot=='weapon.offhand':
   if target.startswith('armor.'):
    expected.append(prefix+f'data modify block {b} Items[0].components."minecraft:equippable" set value '+json.dumps({'slot':target.split('.')[1]},separators=(',',':')))
 for k,target in enumerate(targets,1):
  prefix=f'execute if score t lbc.math matches {k} run ' if random else ''
  expected.append(prefix+f'item replace entity @s {target} from block {b} container.0')
 seed='pumpkin_seeds'
 if slot.startswith('armor.'):seed+='[equippable={slot:"'+slot.split('.')[1]+'"}]'
 expected.append(f'item replace entity @s {slot} with {seed}')
 if actual!=expected:errors.append(p.name+':whole command body')
 rows.append(dict(source=p.relative_to(r).as_posix(),sha256=hashlib.sha256(p.read_bytes()).hexdigest(),slot=slot,targets=targets,complete_match=actual==expected))
p=d/'eat_halloween_food.mcfunction'
expected=['scoreboard players add @s pumpkin_counter 1']+[f'execute if score @s pumpkin_counter matches {i} run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food {i}' for i in range(1,101)]
if commands(p)!=expected:errors.append('food dispatcher')
report=dict(slots=len(rows),food_criteria=100,errors=errors,rows=rows)
(r/'agent_notes/inventory/events_variants_audit.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
lines=['# Pumpkin Curse: все переходы слотов','','Выбор равновероятен среди перечисленных целей. Сам проход проверяет слоты последовательно; перемещение вперёд может продолжиться в том же проходе.','','| Исходный слот | Возможные цели |','|---|---|']
lines.extend('| '+x['slot']+' | '+', '.join(x['targets'])+' |' for x in rows)
(r/'agent_notes/inventory/pumpkin_curse_slots.md').write_text('\n'.join(lines)+'\n',encoding='utf-8')
print(json.dumps(dict(slots=len(rows),food_criteria=100,errors=errors)))
raise SystemExit(bool(errors))
