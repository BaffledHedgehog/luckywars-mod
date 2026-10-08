from pathlib import Path
import json,re,collections
root=Path.cwd(); job='lbc_other_attributes'; sources=json.loads((root/'agent_notes/inventory/research_jobs.json').read_text(encoding='utf-8'))[job]['sources']; adv=json.loads((root/'agent_notes/inventory/lbc_advancement_contracts.json').read_text(encoding='utf-8'))
byreward={v.get('definition',{}).get('rewards',{}).get('function'): (k,v) for k,v in adv.items() if v.get('definition',{}).get('rewards',{}).get('function')}
# Reverse function callers from the function index.
functions=json.loads((root/'agent_notes/inventory/functions.json').read_text(encoding='utf-8')); rev=collections.defaultdict(set)
for f in functions:
 for t in f.get('calls',[]): rev[t].add(f['source'])

def lines(src):
 return [x.strip() for x in (root/Path(src)).read_text(encoding='utf-8-sig').splitlines() if x.strip() and not x.lstrip().startswith('#')]
def pathrel(src): return Path(src).as_posix()
def attrrow(src):
 ls=lines(src); name=Path(src).stem
 attr=next((m.group(1) for l in ls if (m:=re.search(r'attribute @s (minecraft:[a-z_]+) base (?:get|set)',l))), '—')
 scale=next((m.group(1) for l in ls if (m:=re.search(r'attribute @s minecraft:[a-z_]+ base get (\d+)',l))), '—')
 op='no direct attribute delta'
 deltas=[]
 for l in ls:
  m=re.search(r'scoreboard players (add|remove) tmp lbc\.math (-?\d+)',l)
  if m: deltas.append(('+' if m.group(1)=='add' else '-')+m.group(2))
  m=re.search(r'scoreboard players operation tmp lbc\.math ([+\-*/])= tmp1 lbc\.math',l)
  if m: deltas.append(m.group(1)+' tmp1')
 if deltas: op=', '.join(deltas)
 update=next((m.group(1) for l in ls if (m:=re.search(r'function lbc:other/attributes/([a-z_]+_update)',l))), name if name.endswith('_update') else '—')
 revoke=next((m.group(1) for l in ls if (m:=re.search(r'advancement revoke @s only (lbc:[a-z0-9_./-]+)',l))), '—')
 return name,attr.replace('minecraft:',''),scale,op,update,revoke

def iteminfo(src):
 fid='lbc:other/attributes/pills/'+Path(src).stem
 hit=byreward.get(fid)
 if not hit: return 'no advancement reward found', 'manual/caller path'
 aid,c=hit; definition=c['definition']; criteria=definition.get('criteria',{}); blob=json.dumps(criteria,ensure_ascii=True)
 trig=re.search(r'"trigger":\s*"([^"]+)',blob); custom=re.search(r'"minecraft:custom_data":\s*"([^"]+)',blob)
 item=re.search(r'"items":\s*\[([^\]]*)\]',blob)
 inp=custom.group(1) if custom else (item.group(1) if item else 'no explicit item selector')
 dims='outside minecraft:nexus and minecraft:imprinted' if 'minecraft:nexus' in blob and 'minecraft:imprinted' in blob else 'see criterion'
 return aid+'; '+(trig.group(1) if trig else '?')+'; '+inp+'; '+dims, fid

def pillout(src):
 ls=lines(src); out=[]
 for l in ls:
  m=re.search(r'effect give @s (minecraft:[a-z_]+) (\S+)(?: (\d+))?',l)
  if m: out.append(f'effect {m.group(1)} {m.group(2)} amp={m.group(3) or "0"}')
  m=re.search(r'function lbc:other/attributes/([a-z_]+)',l)
  if m: out.append('attribute handler `'+m.group(1)+'`')
 return '; '.join(out) if out else 'see commands below'

out='''# LBC: атрибуты, таблетки и преобразования инвентаря

Задание `lbc_other_attributes` охватывает 331 точный источник. Основные входы — advancement с триггером употребления предмета, функции-посредники атрибутов, старые эффекты экипировки и обработчики передачи предметов из инвентаря в выброшенные сущности. Реальные идентификаторы и числа приведены в таблицах; названия переводов из subtitle не считаются описанием эффекта.

## Изменение базовых атрибутов

Обычные обработчики читают текущий `attribute @s ... base get S` в целочисленный scoreboard `tmp`, прибавляют/вычитают дельту и вызывают парную функцию `*_update` с `storage minecraft:lbcmath`; update применяет macro `$attribute @s ... base set $(tmp)`. Следовательно, изменение к базе равно указанному числу scoreboard units / S. Функции `*_up_godclaw` используют значение `tmp1` вместо постоянного шага; это динамическая формула, и размер задаётся вызывающим кодом. Если есть advancement revoke, функция сбрасывает его для повторного срабатывания, но сама не удаляет предмет.

| Функция | Атрибут | Scale S | Изменение в tmp | Итог к базе | Update | Advancement reset |
|---|---|---:|---|---|---|---|
'''
attr_sources=sorted(s for s in sources if '/other/attributes/' in s and '/pills/' not in s and s.endswith('.mcfunction') and not Path(s).stem.startswith(('gen','random_attrs','randdouble')))
for src in attr_sources:
 name,a,scale,delta,update,revoke=attrrow(src)
 final=delta
 if scale.isdigit():
  parts=[]
  for d in delta.split(', '):
   m=re.fullmatch(r'([+\-])(\d+)',d)
   if m:
    n=int(m.group(2))/int(scale); parts.append(m.group(1)+str(n).rstrip('0').rstrip('.') if n else '0')
   elif 'tmp1' in d: parts.append(d+' / '+scale)
   else: parts.append(d)
  final=', '.join(parts)
 out+='| `'+name+'` | `'+a+'` | '+scale+' | `'+delta+'` | `'+final+'` | `'+update+'` | `'+revoke+'` |\n'
 out+='''

Пограничные варианты сохраняют собственный шаг и scale: `max_health` обычный `+2`, `max_health_1` — `+1`, `_down_two` — `−2`, `_down_eight` — `−8`, `_up_eight` — `+8`; knockback resistance обычный шаг равен 10, тогда как `_down`/`_up` тоже меняют на 10. Для `movement_speed` базовый шаг ±2000 при scale 100000 (±0.02), tiny +100 (0.001). Однако `attribute base get S` сначала сохраняет квантованное целое значение в scoreboard: деление delta на S описывает шаг после этого округления, а не гарантированную абсолютную дельту от любой дробной исходной базы. `_godclaw`-версии используют считанное/переданное целое `tmp1`.

## Таблетки: trigger, входной предмет и результат

Для функций из `attributes/pills` найденные advancement-контракты показывают `minecraft:consume_item` и custom data `pill:<число>`; условия игрока исключают измерения `minecraft:nexus` и `minecraft:imprinted`. Успешное употребление расходует предмет по самому consume trigger, затем reward вызывает функцию ниже. Обработчик обычно применяет эффект либо вызывает attribute handler, показывает subtitle и звук `minecraft:lbcsounds.pill_good`/`pill_bad`, после чего отзывает advancement `lbc:pills/pill_*` для следующего предмета. Отзыв advancement не возвращает таблетку.

| Функция | Advancement / trigger / custom data | Результат эффекта или атрибута |
|---|---|---|
'''
pill_sources=sorted(s for s in sources if '/attributes/pills/' in s and s.endswith('.mcfunction'))
for src in pill_sources:
 info,_=iteminfo(src); out+='| `'+Path(src).stem+'` | `'+info.replace('|','/')+'` | '+pillout(src)+' |\n'
out+='''

Атрибутные таблетки используют те же значения из таблицы выше; плохие изменения запускают соответствующий `*_down`, хорошие — `*_up`. Эффекты с суффиксом `_99` имеют буквально приведённые высокие amplifier-значения, не обычную длительность/силу: это отдельные ветки и отдельные custom_data IDs в таблице. Функции, для которых не найден reward-контракт, в таблице явно отмечены как ручной/caller запуск.

## Случайный выбор изменения атрибута

'''
for s in sorted(x for x in sources if any(k in x for k in ['random_attrs','randdouble','gen_rand'])):
 ls=lines(s); out+='### `'+Path(s).stem+'`\n\n'
 if Path(s).suffix=='.py': out+='Это генератор файлов; сам не является исполняемой Minecraft-функцией.\n\n'
 elif ls: out+='Команды: '+'; '.join('`'+x+'`' for x in ls)+'\n\n'
 else: out+='Файл пуст.\n\n'
out+='## Передача предметов из инвентаря в item entities\n\n'
drop_sources=sorted(s for s in sources if '/other/drop_inv/' in s and s.endswith('.mcfunction'))
out+='Каждый `drop_inv` helper создаёт рядом item entity на основе временного `minecraft:stone`, копирует в неё поле `Item` из выбранного слота и снимает временный tag `modifyitem`. Сам helper исходный слот не очищает: ручной вызов оставляет оригинал рядом с копией. Caller `projectiles/gadgets/honey_pl` вызывает все 41 helper (slots 0..35, offhand и четыре armor slots), затем сам заменяет inventory/offhand/armor игрока на air; очистка выполняется caller-ом. `PickupDelay:40` равен 40 game ticks (2 секунды). Варианты различаются источником поля: `0..35` — inventory, `100..103` — boots/legs/chest/head, `-106` — offhand.\n\n| Файл | Источник предмета |\n|---|---|\n'
for s in drop_sources:
 name=Path(s).stem; ls=lines(s); source=next((m.group(1) for l in ls if (m:=re.search(r'Item set from entity @s (Inventory\[\{Slot:[^}]+\}\]|equipment\.[a-z]+)',l))), '—')
 out+='| `'+name+'` | `'+source+'` |\n'
out+='''

## Новый игровой цикл: legacy предметы и очистка мира

'''
for s in sorted(x for x in sources if '/other/new_game/' in x):
 name=Path(s).stem; ls=lines(s)
 out+='### `new_game/'+name+'`\n\n'
 if not ls: out+='Файл пуст.\n\n'; continue
 calls=sorted(set(re.findall(r'function lbc:([a-z0-9_./-]+)', '\n'.join(ls))))
 # Summarize schedules, conditions, effects, gamerules, cleanups while preserving exact command in source link.
 summ=[]
 for pat,label in [(r'function lbc:([a-z0-9_./-]+)','вызов'),(r'schedule function lbc:([a-z0-9_./-]+) ([^ ]+)','повтор'),(r'minecraft:custom_data[^\n]{0,100}','проверка legacy custom_data'),(r'effect give [^\n]+','эффект'),(r'scoreboard players (?:add|remove|set) [^\n]+','счётчик'),(r'gamerule [^\n]+','gamerule'),(r'tp [^\n]+','перемещение'),(r'fill [^\n]+','очистка блоков')]:
  vals=[]
  for l in ls:
   for m in re.finditer(pat,l):
    v=m.group(0)
    if v not in vals: vals.append(v)
  if vals: summ.append(label+': '+'; '.join('`'+v+'`' for v in vals))
 out+='\n'.join('- '+x for x in summ)+'\n\n'
out+='## Остальные и пустые источники\n\n'
other=sorted(s for s in sources if s not in attr_sources and s not in pill_sources and s not in drop_sources and '/other/new_game/' not in s and not any(k in s for k in ['random_attrs','randdouble','gen_rand']))
for s in other:
 ls=lines(s); out+='### `'+Path(s).as_posix().split('/function/')[1].removesuffix('.mcfunction')+'`\n\n'
 if not ls: out+='Пустая функция.\n\n'; continue
 # This group contains helper generation / pill random / reset orchestration; list active behavior commands.
 out+='Команды: '+'; '.join('`'+x+'`' for x in ls)+'\n\n'
(root/'agent_notes/lbc_other_attributes.md').write_text(out,encoding='utf-8')
# Coverage exact sources -> precise section.
records=[]
for s in sources:
 if '/attributes/pills/' in s: sec='Таблетки: trigger, входной предмет и результат'
 elif '/other/attributes/' in s and not Path(s).stem.startswith(('gen','random_attrs','randdouble')): sec='Изменение базовых атрибутов'
 elif '/other/drop_inv/' in s: sec='Передача предметов из инвентаря в item entities'
 elif '/other/new_game/' in s: sec='Новый игровой цикл: legacy предметы и очистка мира'
 elif any(k in s for k in ['random_attrs','randdouble','gen_rand']): sec='Случайный выбор изменения атрибута'
 else: sec='Остальные и пустые источники'
 records.append({'source':s,'document':'agent_notes/lbc_other_attributes.md','section':sec,'status':'non_runtime' if Path(s).suffix=='.py' else ('non_runtime' if not lines(s) else 'documented'),'note':'Exact values and command effects are indexed in this section.'})
(root/'agent_notes/coverage/lbc_other_attributes.json').write_text(json.dumps(records,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print('wrote',len(sources),'sources; attrs',len(attr_sources),'pills',len(pill_sources),'drop slots',len(drop_sources),'doc bytes',len(out.encode('utf-8')))


