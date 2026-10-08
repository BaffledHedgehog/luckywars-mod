from pathlib import Path
import json,re,collections
root=Path.cwd(); job=json.loads((root/'agent_notes/inventory/research_jobs.json').read_text(encoding='utf-8'))['lbc_other_main']; sources=job['sources']
# Direct call-site map from exact lines, excluding comments.
reverse=collections.defaultdict(list)
for p in (root/'world/datapacksdisabled').rglob('*.mcfunction'):
 try: src_lines=p.read_text(encoding='utf-8-sig').splitlines()
 except UnicodeError: continue
 rel=p.relative_to(root).as_posix()
 for line in src_lines:
  if line.lstrip().startswith('#'): continue
  for target in re.findall(r'\bfunction\s+(lbc:[a-z0-9_./-]+)',line):
   pair=(rel,line.strip())
   if pair not in reverse[target]: reverse[target].append(pair)
adv=json.loads((root/'agent_notes/inventory/lbc_advancement_contracts.json').read_text(encoding='utf-8'))
rew={v.get('definition',{}).get('rewards',{}).get('function'):(k,v) for k,v in adv.items() if v.get('definition',{}).get('rewards',{}).get('function')}

def unique(vals): return list(dict.fromkeys(vals))
def get_adv(fid):
 if fid not in rew:return None
 aid,row=rew[fid]; definition=row.get('definition',{}); criteria=json.dumps(definition.get('criteria',{}),ensure_ascii=True)
 trig=re.search(r'"trigger":\s*"([^"]+)',criteria)
 cd=re.search(r'"minecraft:custom_data":\s*"([^"]+)',criteria)
 items=re.search(r'"items":\s*\[([^\]]*)\]',criteria)
 inp=cd.group(1) if cd else (items.group(1) if items else 'item condition not literal')
 dims='';
 if 'minecraft:nexus' in criteria and 'minecraft:imprinted' in criteria: dims='; no trigger in nexus/imprinted'
 return f'{aid} ({trig.group(1) if trig else "trigger?"}; {inp}{dims})'
def summarize(src):
 p=root/Path(src); lines=p.read_text(encoding='utf-8-sig').splitlines(); active=[x.strip() for x in lines if x.strip() and not x.lstrip().startswith('#')]
 if p.suffix=='.py': return 'Это вспомогательный генератор, не runtime-функция.'
 fid='lbc:other/'+p.stem; callers=[x for x in reverse.get(fid,[]) if x[0] != p.relative_to(root).as_posix()]; adventry=get_adv(fid)
 entry=[]
 if adventry: entry.append('Reward-вход: '+adventry)
 if callers:
  rel,line=callers[0]; entry.append('caller `'+rel+'` вызывает её командой `'+line+'`')
 else: entry.append('из исследованных функций прямого caller не найден; команды доступны при ручном запуске')
 effects=[]
 # Summons and salient NBT values, retaining distinct values for variant families.
 summons=collections.defaultdict(list)
 for line in active:
  for m in re.finditer(r'\bsummon\s+(?:minecraft:)?([^\s{]+)',line):
   typ=m.group(1); tail=line[m.end():]; nbt=tail[tail.find('{'):] if '{' in tail else ''
   salient=[]
   for key in ['fuse','NoGravity','ExplosionPower','Motion','Tags','Health','PickupDelay','Owner','Damage','CustomName','PersistenceRequired']:
    mm=re.search(r'\b'+key+r':(\[[^\]]*\]|\{[^}]*\}|[^,}\]]+)',nbt)
    if mm: salient.append(key+':'+mm.group(1))
   val=', '.join(salient) if salient else ''
   if val not in summons[typ]:summons[typ].append(val)
 if summons:
  chunks=[]
  random_cases=[]
  for line in active:
   m=re.search(r'execute if score (#?[a-z0-9_]+) lbc\.math matches (-?\d+) run summon (?:minecraft:)?([^\s{]+)',line)
   if m: random_cases.append((m.group(1),int(m.group(2)),m.group(3)))
  if len(random_cases) == len(active) and len({x[0] for x in random_cases}) == 1:
   score=random_cases[0][0]
   entities=unique([x[2] for x in random_cases])
   effects.append(f'выбирает одну из {len(random_cases)} условных ветвей по `{score}`; возможные сущности: '+', '.join('`'+x+'`' for x in entities))
   summons={}
  if not summons: pass
  else:
   for typ,vals in summons.items():
    cnt=sum(len(re.findall(r'\bsummon\s+(?:minecraft:)?'+re.escape(typ)+r'\b',l)) for l in active)
    payload='; '.join(vals)
    chunks.append(f'{typ} ×{cnt}'+(f' ({payload})' if payload else ''))
  if chunks: effects.append('создаёт '+', '.join(chunks))
 # Effect give/clear and damage
 ef=[]
 for line in active:
  for m in re.finditer(r'(?<![\w:])effect give (\S+) (?:minecraft:)?([a-z_]+) (\S+)(?: (\d+))?',line):
   # Java /effect duration is seconds. SNBT fuse suffixes are parsed separately as ticks.
   ef.append(f'{m.group(2)} {m.group(3)} seconds, amplifier {m.group(4) or "0"} на {m.group(1)}')
 if ef: effects.append('накладывает '+ '; '.join(unique(ef)))
 dmg=[]
 for line in active:
  for m in re.finditer(r'\bdamage (\S+) ([\d.]+)(?: (?:minecraft:)?([a-z_]+))?',line): dmg.append(f'{m.group(1)}: {m.group(2)}'+(f' ({m.group(3)})' if m.group(3) else ''))
 if dmg: effects.append('наносит damage '+ '; '.join(unique(dmg)))
 # Loot and item input/output
 loot=[]
 for line in active:
  m=re.search(r'\bloot (give|spawn|replace) [^\n]*?\sloot\s+((?:lbc|minecraft):[a-z0-9_./-]+)',line)
  if m: loot.append((m.group(1),m.group(2),line))
  # Match a command token only at command start or immediately after `run`.
  # In particular, `effect give` is an effect command, never an item give.
  m=re.search(r'(?:^|\brun\s+)give\s+(?:@\w+(?:\[[^]]*\])?\s+)?((?:minecraft|lbc):[a-z0-9_./-]+(?:\[[^]]*\])?)',line)
  if m: loot.append(('give',m.group(1),line))
 if loot:
  desc=[]
  for mode,item,line in loot:
   if mode=='replace': desc.append(f'заменяет содержимое слота/сущности loot table `{item}`')
   elif mode=='spawn': desc.append(f'создаёт предметы из loot table `{item}`')
   elif mode=='give': desc.append(f'выдаёт loot table `{item}`')
   else: desc.append(f'выдаёт `{item}`')
  effects.append('; '.join(unique(desc)))
 # Score changes and attributes
 scores=[]
 for line in active:
  m=re.search(r'scoreboard players (set|add|remove|reset|operation) (.+)',line)
  if m: scores.append(m.group(1)+' '+m.group(2))
 if scores: effects.append('меняет scoreboard: '+'; '.join(unique(scores)))
 attrs=[]
 for line in active:
  m=re.search(r'attribute (\S+) (minecraft:[a-z_]+) (base|modifier\s+\w+) (get|set|add|remove)\s*([^\n]*)',line)
  if m: attrs.append(' '.join(m.groups()).strip())
 if attrs: effects.append('работает с атрибутом: '+'; '.join(unique(attrs)))
 # Block, world, and movement commands.
 world=[]
 for line in active:
  for verb in ['setblock','fill','clone','tp','teleport','spreadplayers','place template','forceload','gamerule','weather']:
   m=re.search(r'\b'+re.escape(verb)+r'\s+([^\n]+)',line)
   if m: world.append(verb+' '+m.group(1))
 if world: effects.append('меняет мир/позиции: '+'; '.join(unique(world)[:5]))
 # Data changes, particles and sounds
 data=[]
 for line in active:
  m=re.search(r'\bdata (modify|remove|merge) ([^\n]+)',line)
  if m: data.append(m.group(1)+' '+m.group(2))
 if data: effects.append('меняет NBT/components: '+'; '.join(unique(data)[:4]))
 pfx=unique(re.findall(r'\bparticle (?:minecraft:)?([^\s{]+)', '\n'.join(active)))
 if pfx: effects.append('эффекты частиц: '+', '.join('`'+x+'`' for x in pfx))
 snd=unique(re.findall(r'\bplaysound (?:minecraft:)?([^\s]+)', '\n'.join(active)))
 if snd: effects.append('звук: '+', '.join('`'+x+'`' for x in snd))
 calls=unique(re.findall(r'\bfunction (lbc:[a-z0-9_./-]+)', '\n'.join(active)))
 if calls: effects.append('передаёт дальнейшую часть в '+', '.join('`'+x+'`' for x in calls))
 # Exit/consumption.
 exits=[]
 for line in active:
  if re.search(r'\bkill\s+',line): exits.append('удаляет '+re.search(r'\bkill\s+([^\n]+)',line).group(1))
  m=re.search(r'\bclear\s+([^\n]+)',line)
  if m: exits.append('clear '+m.group(1))
  m=re.search(r'advancement revoke\s+([^\n]+)',line)
  if m: exits.append('отзывает advancement '+m.group(1))
  m=re.search(r'schedule function\s+([^\n]+)',line)
  if m: exits.append('планирует '+m.group(1))
  if 'run return' in line or line.startswith('return '): exits.append('return')
 macros=unique(re.findall(r'\$([a-z][a-z0-9_]*)', '\n'.join(active)))
 if macros: effects.append('читает macro-параметры '+', '.join('`$('+x+')`' for x in macros)+'; фактическое значение передаётся вызывающим `with storage`')
 if effects: result='Вход: '+'; '.join(entry)+'. Результат: '+'. '.join(x[:850] for x in effects)+'.'
 elif not active: result='Вход: '+'; '.join(entry)+'. Результат: файл пуст, команды отсутствуют, состояние не меняется.'
 else: result='Вход: '+'; '.join(entry)+'. Результат: команды указаны в исходнике; требуются ручное чтение и проверка перед описанием, автоматическая сводка не классифицирует этот файл.'
 if exits: result+=' Завершение/расход: '+ '; '.join(unique(exits)[:8])+'.'
 return result

# Insert one Russian description per runtime source, immediately under its exact function heading.
doc=root/'agent_notes/lbc_other_main.md'; text=doc.read_text(encoding='utf-8');
for src in sources:
 p=root/Path(src)
 if p.suffix!='.mcfunction': continue
 fid='lbc:other/'+p.stem; heading='### `'+fid+'`'; at=text.find(heading)
 if at<0: print('no heading',fid); continue
 insert=at+len(heading)
 if text[insert:insert+20].find('Фактическая работа')>=0: continue
 text=text[:insert]+'\n\n**Фактическая работа.** '+summarize(src)+'\n'+text[insert:]
# Hand-verified descriptions for complex sources and helper semantics.
manual={
'lbc:other/compressedtnt':'Маркер с тегом `compressedtnt`, вызванный из `markers_2t1`, заменяет свой блок на redstone, создаёт 16 TNT без гравитации в четырёх кольцах и удаляет себя. Fuse распределён поровну: 4 TNT по 60, 4 по 65, 4 по 70 и 4 по 75 тиков (3, 3.25, 3.5 и 3.75 секунды); шесть звуков primed TNT используют разные pitch.',
'lbc:other/blazestorm':'Функция создаёт ровно три `minecraft:fireball` с `ExplosionPower:1b`, `Motion:[0,-0.1,0]` и тегом `blazeball`. Затем helper `lbc:universal_pos_razb_15_active` добавляет к X и Z каждого снаряда случайное целое −15..15, записывает новую позицию и ставит тег `active`; этот helper Owner не задаёт.',
'lbc:other/delta_tnt':'Маркер `delta_tnt` ставит redstone block и создаёт 33 marker в группе, каждый с lifetime 1000 и raycast/mecha tags; функция воспроизводит звуки primed TNT и удаляет исходный marker. Дальнейшее поведение определяется обработчиками этих marker, перечисленными в самой функции.',
}
for fid,paragraph in manual.items():
 heading='### `'+fid+'`'; at=text.find(heading)
 if at<0: continue
 start=text.find('**Фактическая работа.** ',at); end=text.find('\n',start)
 text=text[:start]+'**Фактическая работа.** '+paragraph+text[end:]
# Explain geometry and contexts without guessing selector regions.
anchor='Направление `@s`, `@e`, игрока и владельца не смешивается: это существенно для предметов и снарядов.'
text=text.replace(anchor,anchor+' Выборки `dx/dy/dz` и `distance` читаются совместно; `execute as` сохраняет позицию выполнения, пока нет отдельного `at`/`positioned`. Для областей указываются буквальные селекторы, без упрощения их до радиуса.')
doc.write_text(text,encoding='utf-8')
print('added prose to',sum(Path(s).suffix=='.mcfunction' for s in sources),'runtime functions')
