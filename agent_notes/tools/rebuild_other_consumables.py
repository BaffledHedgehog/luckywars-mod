from pathlib import Path
import json,re,collections
root=Path.cwd(); job='lbc_other_consumables_craft'; sources=json.loads(Path('agent_notes/inventory/research_jobs.json').read_text(encoding='utf8'))[job]['sources']
def lines(src):return [x.strip() for x in (root/Path(src)).read_text(encoding='utf-8-sig').splitlines() if x.strip() and not x.lstrip().startswith('#')]
def fid(src):
 parts=Path(src).parts; i=parts.index('function')
 return parts[i-1]+':'+Path(*parts[i+1:]).with_suffix('').as_posix()
def uniq(x):return list(dict.fromkeys(x))
functions=json.loads(Path('agent_notes/inventory/functions.json').read_text(encoding='utf8')); rev=collections.defaultdict(list)
for f in functions:
 for t in f.get('calls',[]):rev[t].append(f['source'])
adv=json.loads(Path('agent_notes/inventory/lbc_advancement_contracts.json').read_text(encoding='utf8'))
reward={v.get('definition',{}).get('rewards',{}).get('function'):(k,v) for k,v in adv.items() if v.get('definition',{}).get('rewards',{}).get('function')}
dispatch={}
for p in [Path('world/datapacksdisabled/LBC/data/lbc/function/other/craft_ground.mcfunction'),Path('world/datapacksdisabled/LBC/data/lbc/function/other/craft/inventory_craft_check.mcfunction')]:
 for x in lines(p):
  m=re.search(r'run function (lbc:[a-z0-9_./-]+)',x)
  if m:dispatch[m.group(1)]=x
def craft_input(f):
 x=dispatch.get(f,'')
 if f in ('lbc:other/craft/crafted_electroshield','lbc:other/craft/crafted_electroshield_inv'):return 'stelectro_tiny:1 + ordinary minecraft:shield without custom_data energy_shield:1'
 if f in ('lbc:other/craft/crafted_mana','lbc:other/craft/crafted_mana_inv'):return 'stfire_tiny:1 + any second item entity/slot (ground path additionally requires NBT Thrower)'
 tags=re.findall(r'custom_data[":~]*\{([^}]+)\}',x)
 ids=re.findall(r'Item:\{id:"([^"]+)',x)
 return ' + '.join('{' + ', '.join(re.findall(r'[A-Za-z0-9_]+:-?\d+[bBsSlL]?',t)) + '}' for t in tags[:2]) or (' + '.join(ids[:2]) if ids else 'вход по selector см. source')
def outputs(src):
 ls=lines(src); vals=[]
 for x in ls:
  m=re.search(r'loot (spawn|give|replace) .*?\bloot ([a-z0-9_.-]+:[a-z0-9_./-]+)',x)
  if m:vals.append({'spawn':'создаёт результат','give':'выдаёт результат','replace':'помещает результат в слот'}[m.group(1)]+' таблицы `'+m.group(2)+'`')
  m=re.search(r'\beffect give (\S+) (?:minecraft:)?([a-z_]+) (\S+)(?: (\d+))?',x)
  if m:vals.append(f'эффект `{m.group(2)}`: {m.group(3)} секунд, amplifier {m.group(4) or 0}, цель {m.group(1)}')
  m=re.search(r'\bdamage (\S+) ([\d.]+)(?: ([\w:.-]+))?',x)
  if m:vals.append(f'урон {m.group(2)} `{m.group(3) or "generic"}` по {m.group(1)}')
  m=re.search(r'\bsummon (?:minecraft:)?([^\s{]+)(.*)',x)
  if m:
   extra=[]
   for key in ('fuse','ExplosionPower','Health','Tags'):
    q=re.search(r'\b'+key+r':(\[[^]]*\]|[^,}\]]+)',m.group(2))
    if q:extra.append(key+'='+q.group(1))
   vals.append(f'создаёт `{m.group(1)}`'+(' ('+', '.join(extra)+')' if extra else ''))
  m=re.search(r'\b(clear|kill) (\S+)(.*)',x)
  if m and not x.startswith('effect clear '):vals.append(('очищает предмет/цель' if m.group(1)=='clear' else 'удаляет сущность')+' `'+m.group(2)+'`'+(' '+m.group(3).strip() if m.group(3).strip() else ''))
  m=re.search(r'\beffect clear (\S+)(?: (\S+))?',x)
  if m:vals.append('снимает '+(f'`{m.group(2)}`' if m.group(2) else 'все активные эффекты')+' с '+m.group(1))
  m=re.search(r'\b(attribute @s [^\n]+)',x)
  if m:vals.append('изменение атрибута: `'+m.group(1)+'`')
  m=re.search(r'\b(scoreboard players (?:set|add|remove|reset|operation) [^\n]+)',x)
  if m:vals.append('scoreboard: `'+m.group(1)+'`')
  m=re.search(r'\b(setblock|fill|spreadplayers|tp|teleport) ([^\n]+)',x)
  if m:vals.append('мир/позиция: `'+m.group(1)+' '+m.group(2)[:110]+'`')
  m=re.search(r'\bfunction ((?:lbc|lbcr):[a-z0-9_./-]+)(?: with storage ([a-z0-9_:.]+))?',x)
  if m:vals.append('передаёт этап в `'+m.group(1)+'`'+(f' с параметрами storage `{m.group(2)}`' if m.group(2) else ''))
  m=re.search(r'\bplaysound (\S+) \S+ (\S+)',x)
  if m:vals.append('звук `'+m.group(1)+'` для '+m.group(2))
  m=re.search(r'\bparticle (?:minecraft:)?([^\s]+)',x)
  if m:vals.append('частицы `'+m.group(1)+'`')
  m=re.search(r'\bitem replace ([^\n]+)',x)
  if m:vals.append('заменяет предмет/слот: `'+m.group(1)[:130]+'`')
 return uniq(vals)
def mana_branches(src):
 out=[]
 for x in lines(src):
  m=re.search(r'execute if score tmp lbc\.math matches (\d+) run (.+)',x)
  if not m:continue
  n=int(m.group(1)); cmd=m.group(2)
  q=re.search(r'scoreboard players add (\S+) (\S+) (\d+)',cmd)
  if q:result=f'{q.group(2)} +{q.group(3)}'
  else:
   q=re.search(r'effect give (\S+) minecraft:([a-z_]+) (\d+) (\d+)',cmd)
   result=f'{q.group(2)}: {q.group(3)} seconds, amplifier {q.group(4)}, target {q.group(1)}' if q else cmd
  out.append((n,result))
 return out
def entry(src):
 f=fid(src); hits=rev.get(f,[]); r=reward.get(f)
 if r:
  aid,row=r; blob=json.dumps(row.get('definition',{}).get('criteria',{}),ensure_ascii=True)
  tr=re.search(r'"trigger":\s*"([^"]+)',blob); cd=re.search(r'"minecraft:custom_data":\s*"([^"]+)',blob)
  dims='; trigger исключён в `minecraft:nexus` и `minecraft:imprinted`' if 'minecraft:nexus' in blob and 'minecraft:imprinted' in blob else ''
  return f'Употребление/событие автоматически вызывает reward `{aid}` (`{tr.group(1) if tr else "trigger см. JSON"}`'+(f', custom_data `{cd.group(1)}`' if cd else '')+dims+').' 
 if hits:return 'Helper вызывается из `'+hits[0]+'`; самостоятельного предметного trigger у функции нет.'
 return 'Автоматический caller/reward среди исследованных файлов не найден; при ручном вызове исполняются действия ниже.'
craft=[]; eat=[]; other=collections.defaultdict(list)
for src in sources:
 rel=Path(src).as_posix(); p=Path(src)
 if '/craft/' in rel:craft.append(src)
 elif '/eatables/' in rel:eat.append(src)
 else:other['/'.join(rel.split('/')[-2:-1])].append(src)
out='''# LBC: craft и расходуемые механики

Охват: все 221 источника job `lbc_other_consumables_craft`. Предметные функции разбираются по trigger/reward, helper-ы по caller и реальным командам. `/effect give` задаёт секунды; SNBT fuse — игровые тики. Названия переводов не используются как единственное доказательство эффекта.

## Рецепты из выброшенных предметов и crafting slots

`craft_ground` проверяет два item entity с `custom_data` (или item/model selector), носитель рецепта должен иметь `craftusable:1`; ингредиенты расположены в `distance=0.001..1`. Проверка не действует в Nexus и Imprinted. `inventory_craft_check` запускает пары по `player.crafting.*`; парный `_inv`-handler уменьшает реальные слоты через count modifier и применяет дополнительные проверки количества. В таблице указаны literal custom_data inputs диспетчера и loot/effect выход каждой ветки; при отсутствии `_inv` пара в inventory dispatcher не обнаружена.

| Обработчик | Входная пара (точные маркеры из dispatcher) | Выход по handler | Источник |
|---|---|---|---|
'''
for src in sorted(craft):
 f=fid(src); result='; '.join(outputs(src)[:5])
 if Path(src).stem in ('crafted_electroshield','crafted_electroshield_inv'):result='loot `lbc:electro_shield`; predicate-based catalyst cost is detailed below'
 if Path(src).stem in ('crafted_mana','crafted_mana_inv'):result='uniform choice from 64 effects/scoreboard changes; full outcome table below'
 out+='| `'+f+'` | `'+craft_input(f)+'` | '+result+' | ['+Path(src).name+'](../'+src.replace('\\','/')+') |\n'
out+='''

### Рецепт с 64 исходами: `crafted_mana` и `crafted_mana_inv`

Диспетчер требует catalyst `stfire_tiny` и второй выброшенный предмет с `Thrower`; inventory-пара требует `stfire_tiny` плюс любой предмет в crafting slots. Один вызов выбирает равновероятное число 1..64, применяет ровно соответствующую строку ниже, затем добавляет к получателю теги `have_lucky_effects`, `have_lucky_effects_1s`, `have_lucky_effects_5t`, flame particles и firecharge sound. Ground-вариант адресует ближайшего не-spectator игрока (`@p`); inventory-вариант — владельца `@s`. Оба расходуют одну единицу второго предмета, catalyst `stfire_tiny` сохраняется.

| Случай | Изменение scoreboard или эффект (длительность `/effect give` в секундах) |
|---:|---|
'''
mana_src=next(s for s in craft if Path(s).stem=='crafted_mana')
for n,value in mana_branches(mana_src):out+=f'| {n} | {value} |\n'
out+='''

### Неодинаковый расход: `crafted_electroshield`

Ground: catalyst `stelectro_tiny` + обычный shield (без custom_data `energy_shield`); создаёт `lbc:electro_shield`, всегда удаляет один обычный shield, а catalyst удаляется только если predicate `lbc:chance40` сработал. Inventory: обычный shield уменьшается через `count_reduce`; после этого predicate `chance60` срабатывает — функция делает `return fail`, оставляя catalyst. В оставшихся 40% ветка доходит до `count_reduce` catalyst, поэтому он расходуется; output loot производится до проверки шанса в обеих ветках.

'''
out+='''

Наземная ветка, если присутствует, ищет item entity поблизости и после рецепта удаляет именно matching dropped inputs. Inventory-ветка работает с crafting slots, поэтому предметы там уменьшаются по stack count; она не является копией наземного расхода. Два результата могут различаться даже при одинаковых маркерах — таблица сохраняет оба обработчика отдельно.

## Употребляемые предметы

Следующие reward-функции запускаются advancement-trigger; consume_item уже расходует использованный предмет. Эффекты и численные длительности здесь взяты из `/effect give` в источнике, а не из имен/tooltip. Helpers без reward отмечены конкретным вызывающим файлом либо как ручной вызов.

| Функция | Активация | Фактический эффект / расход / переход |
|---|---|---|
'''
for src in sorted(eat):
 f=fid(src); result=outputs(src); out+='| `'+f+'` | '+entry(src)+' | '+'; '.join(result[:7])+' |\n'
for grp,items in sorted(other.items()):
 heading='Семейство: '+grp.replace('_',' ')
 out+=f'\n## {heading}\n\n'
 for src in sorted(items):
  result=outputs(src); ls=lines(src)
  out+='### `'+fid(src)+'`\n\n'+entry(src)+' '+'; '.join(result[:7]) if result else entry(src)+' В буквальных командах: '+'; '.join('`'+x[:160]+'`' for x in ls[:3])
  out+=' [Исходник](../'+src.replace('\\','/')+')\n\n'
Path('agent_notes/lbc_other_consumables_craft.md').write_text(out,encoding='utf8')
# Coverage links to exact section headers.
coverage=[]
for src in sources:
 rel=Path(src).as_posix()
 if '/craft/' in rel:sec='Рецепты из выброшенных предметов и crafting slots'
 elif '/eatables/' in rel:sec='Употребляемые предметы'
 else:sec='Семейство: '+rel.split('/')[-2].replace('_',' ')
 status='documented' if lines(src) else 'non_runtime'
 coverage.append({'source':rel,'document':'agent_notes/lbc_other_consumables_craft.md','section':sec,'status':status,'note':'Описаны вход/условия, буквальный результат и расход по source.' if status=='documented' else 'Пустой helper; runtime-команд нет.'})
Path('agent_notes/coverage/lbc_other_consumables_craft.json').write_text(json.dumps(coverage,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
print('built',len(craft),'craft',len(eat),'eatables',sum(map(len,other.values())),'other')
