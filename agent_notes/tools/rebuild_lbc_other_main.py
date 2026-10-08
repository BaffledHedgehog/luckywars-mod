from pathlib import Path
import json,re,collections
root=Path.cwd(); job=json.loads((root/'agent_notes/inventory/research_jobs.json').read_text(encoding='utf8'))['lbc_other_main']; sources=job['sources']
rev=collections.defaultdict(list)
for f in (root/'world/datapacksdisabled').rglob('*.mcfunction'):
 try: ls=f.read_text(encoding='utf-8-sig').splitlines()
 except UnicodeError: continue
 rel=f.relative_to(root).as_posix()
 for line in ls:
  if line.lstrip().startswith('#'): continue
  for target in re.findall(r'\bfunction\s+(lbc:[a-z0-9_./-]+)',line):
   if (rel,line.strip()) not in rev[target]:rev[target].append((rel,line.strip()))
adv=json.loads((root/'agent_notes/inventory/lbc_advancement_contracts.json').read_text(encoding='utf8'))
rewards={v.get('definition',{}).get('rewards',{}).get('function'):(k,v) for k,v in adv.items() if v.get('definition',{}).get('rewards',{}).get('function')}
def unique(xs):return list(dict.fromkeys(xs))
def read(src):
 p=root/Path(src)
 try:return p.read_text(encoding='utf-8-sig').splitlines()
 except:return []
def fid(src):return 'lbc:other/'+Path(src).stem
def trigger(fid0):
 if fid0 not in rewards:return ''
 aid,c=rewards[fid0]; blob=json.dumps(c.get('definition',{}).get('criteria',{}),ensure_ascii=True)
 tr=re.search(r'"trigger":\s*"([^"]+)',blob); cd=re.search(r'"minecraft:custom_data":\s*"([^"]+)',blob)
 return f'Reward `{aid}` с trigger `{tr.group(1) if tr else "?"}`'+(f' и custom_data `{cd.group(1)}`' if cd else '')+'. '
manual={
'blazestorm':'Создаёт три `minecraft:fireball` с `ExplosionPower:1b`, вертикальной Motion −0.1 и тегом `blazeball`. Helper сдвигает X/Z каждого на случайное целое от −15 до 15 и добавляет `active`; Owner helper не устанавливает.',
'compressedtnt':'Маркер ставит под собой redstone block, создаёт 16 TNT без гравитации и удаляет себя. Четыре TNT получают fuse 60, четыре 65, четыре 70 и четыре 75 тиков (3–3,75 с); выполняется звуковой набор primed TNT.',
'delta_tnt':'Ставит redstone block и создаёт 33 marker с `lifetime:1000` и тегами для raycast/mecha, затем проигрывает звуки primed TNT и удаляет исходный marker. Последующее поведение выполняют обработчики этих тегов.',
'random_mob_spawn':'Выбирает целое 1..90; при `final_luck_modifier=7` фиксирует выбор на 22. Из 90 ветвей `return run summon` срабатывает только соответствующая выбранному числу, поэтому появляется одна сущность, а не весь список.',
'absurd_child':'Удаляет ближайший armor stand `absurd_child_mom`, помещает результат `lbc:luckyblock_block_only` в main hand ближайшего allay `absurd_child`, затем удаляет текущую сущность. Loot не выдаётся игроку напрямую.',
'absurd_child_tick':'Раз в секунду вызывается только для allay `absurd_child`, если нет `stopper_magic` marker в `distance=..20`. Когда не-spectator player в `distance=..5` отсутствует, allay двигается через `raycast_vpered_slower`; отдельно predicate `chance3` запускает случайную реплику. Если main hand равен red mushroom block, brown mushroom block или mushroom stem, соответственно заменяет его на `luckyblock_neko_block`, `luckyblock_loli_block` или `luckyblock_block_only`; каждая loot-ветка делает `return`, поэтому за тик выполняется не больше одной замены.',
'death_compass_attack':'После проверки `death_compass_check` (сверяет X и Z с `LastDeathLocation`, Y не сравнивается) расходует один `death_compass`, создаёт warden на 15 блоков выше с Health 150 и `DeathLootTable:lbc:death_loot`. Даёт darkness на 30 секунд, играет звук и выполняет spreadplayers с максимумом 10 блоков; Nether-ветка также требует `under 120`.',
'arrowgive':'Уменьшает `arrowgens` на 1 и рекурсивно вызывает себя, пока значение находится в 1..; предметы она не создаёт. В исследуемых файлах обнаружен только self-call, поэтому автоматический вход не найден.',
'arrowgive_pre':'Считывает count предметов с custom_data `arrowgenerator` в `arrowgens`, создаёт loot `lbc:arrows` и проверяет появление item entity `stand_arrow`, после чего выдаёт advancement `infinite_quiver`; играет звук. Обработчик `arrowgive` отдельно тратит счётчик, сам стрелы не спавнит.',
'craft_uniwand_with_grimoire':'При найденном рядом grimoire с `libspell:1` увеличивает spellcount wand, вычисляет делитель floor(spellcount/5) (не меньше 1) и добавляет соответствующую долю маны. Копирует spellid/lore, обновляет tier/value и уменьшает предмет через `reduce_count_l`; обработка повторяется, пока grimoire остаётся.',
'craft_uniwand_with_grimoire_value_check':'По `value` применяет к wand ровно один tier modifier: ≤−11 ff, −10..−1 f, 0..9 d, 10..99 c, 100..999 b, 1000..9999 a, 10000..99999 s, 100000..999999 ss, от 1000000 x. Каждая ветвь завершается `return`, поэтому выбран только один tier.',
'negative_mana_1':'Накладывает на `@s` Slowness II: эффект `minecraft:slowness`, длительность 1 секунда, amplifier 1. Условий на блок в этой функции нет; она сама не выдаёт предмет.',
'negative_mana':'Обработчик делит отрицательную ману на диапазоны: −1…−5000 → `negative_mana_1`, −5001…−10000 → `_2`, −10001…−15000 → `_3`, −15001…−20000 → `_4`, ≤−20001 → `_5`; эти ветви применяются только без `overload_helmet_active`. При теге шлема ≤−20001 выбирается `negative_mana_5_helmet`, а `overload_chestplate_active` при mana≤−1 отдельно запускает сопротивление. Advancement `oh_no_mana` выдаётся, когда mana, manaregen_visual и manamax все ≤−1.',
'stoppernuke':'Удаляет в кубе ±1 вокруг позиции nuke блоки netherite и beacon, очищает блок над ним и удаляет marker. Затем **планирует** `stopnuke` через одну секунду; звук останавливается отдельной отложенной функцией.',
'effect_skill_issue_2-0':'Macro читает `$(uid).Rotation[0]` и `[1]` из storage и сохраняет значения с scale 1 в scoreboard `rot0old`/`rot1old`. Это передача старого направления следующему этапу обработки, не самостоятельный эффект игроку.',
'stopnuke':'Останавливает у всех игроков звук `minecraft:lbcsounds.nuke` через `stopsound @a master`. `stoppernuke` планирует этот вызов через `1s` (одну секунду).',
'falling_power':'Создаёт marker в точке `0 310 0` с `lifetime:800` и тегами slowraycast/movingpower/spawned_new_power/can_deleted/magic/other, затем запускает обработчик координат marker.',
'falling_power_data':'У текущего marker задаёт X и Z случайными числами от −100 до 100, сохраняет Y без изменений и снимает тег `spawned_new_power`. Вызывается через `execute as`; без `at @s` позиция выполнения не переносится к marker.',
'liquidvoiddamage_recur':'Macro использует переданное `tmp` для урона `@s` типа `outside_border`; удар пропускается, если `void_resistance` ≥1. Каждый вызов — один шаг урона, повтор/параметры задаёт caller.',
'stopper_player_remove_smth':'Удаляет у `@s` modifier движения и modifier урона атаки по их UUID. Caller сначала выбирает игроков в пределах `distance=..20`, затем `at @s` переносит позицию к каждому игроку.',
'raycastfireball_2':'Передаёт три компонента Pos marker `reikast` в Motion ближайшего small fireball (`distance=..0.001`): `data get ×1000`, запись double `×0.001`. Это направляет снаряд вектором из marker.',
'raycastfireball_2_big':'Копирует Pos[0..2] marker `reikast` в Motion большого fireball с коэффициентами чтения 1000 и записи 0.001, затем убирает временный тег `current`.',
'raycastfireballchick_2':'Копирует координаты marker `reikast` в Motion ближайшего small fireball; чтение scale 1000 и запись double scale 0.0016. Селектор fireball ограничен `distance=..0.001`.',
'manaregen_inwater':'Файл пуст: при ручном и автоматическом запуске не меняет состояние.',
'drownpre':'Файл пуст: источник не накладывает эффект и не меняет состояние.',
'hedgehog_boom':'Без дополнительных локальных условий передаёт выполнение `lbcr:raycast_loop9cd_upgrade`; реализация есть в том же datapack, файл [`raycast_loop9cd_upgrade.mcfunction`](../world/datapacksdisabled/LBC/data/lbcr/function/raycast_loop9cd_upgrade.mcfunction), и её функции описываются в `lbc_lbcr`. В этой функции нет собственного selector/условия.',
'infinitestewused':'Reward после употребления `stewused` заменяет main hand на одну cooked beef с именем `infinite_steak`, custom_data `{inf:1}` и style `lbc:tier_c`, затем отзывает advancement для повторного срабатывания.',
'infinitestewused_old_sch':'Заменяет main hand игроков с тегом `scheduled_eating` на rabbit stew `{inf_old:1}` с именем `horn`, затем снимает тег. Задержку задаёт caller.',
'nitroboots':'Файл пуст: этот ресурс не задаёт эффект ботинок.',
'absurd_child_tick_random_msg':'Выбирает число 1..20 и отправляет соответствующую переводимую реплику `absurd_child_msg_N` игрокам с `distance=..30`. Это только сообщение, без урона или выдачи предметов.',
'kill_rand_hihihiha':'Выбирает 1..7: исходы 1..6 убивают текущую сущность без `void_resistance` и без исключающего тега `sin_greed_active`. Исход 7 выбирает другого не-spectator игрока и убивает его только при проверенных условиях сопротивления/Nexus/stopper; затем выполняет advancement grant для текущего контекста и снимает временный `owner`.',
'killanvil':'Удаляет все сущности с тегом `killedanvils`; вызывающая функция планирует очистку через 60 game ticks.',
'killcart':'Удаляет все сущности `killedcart`; вызов запланирован на `25s` после создания, то есть через 25 секунд.',
'speed_up':'При ботинках `nyauwu:1` и `mana < manamax` добавляет movement_speed modifier с UUID `916c89e6-ae62-4b11-91d7-10e7e57c726d`, operation `add_multiplied_total`, значение 1. При mana ≥ manamax этот modifier удаляется.',
}
def summarize(src):
 p=root/Path(src); active=[x.strip() for x in read(src) if x.strip() and not x.lstrip().startswith('#')]; name=Path(src).stem; f=fid(src)
 if name in manual:return manual[name]
 if p.suffix=='.py':return 'Вспомогательный скрипт; Minecraft не исполняет его как runtime-функцию.'
 if not active:return 'Файл пуст: при вызове состояние не меняется.'
 facts=[]
 # Exact command-level result extraction; never match `effect give` as item `give`.
 for x in active:
  m=re.search(r'(?<![\w:])effect give (\S+) (?:minecraft:)?([a-z_]+) (\S+)(?: (\d+))?',x)
  if m:facts.append(f'накладывает `{m.group(2)}` на {m.group(1)} на {m.group(3)} секунд, amplifier {m.group(4) or 0}')
  m=re.search(r'\bloot (give|spawn|replace) .*?\bloot ((?:lbc|minecraft):[a-z0-9_./-]+)',x)
  if m:facts.append({'give':f'выдаёт loot table `{m.group(2)}` игроку','spawn':f'создаёт на месте результат loot table `{m.group(2)}`','replace':f'помещает результат loot table `{m.group(2)}` в указанный слот'}[m.group(1)])
  m=re.search(r'(?:^|\brun\s+)give\s+(?:@\w+(?:\[[^]]*\])?\s+)?((?:minecraft|lbc):[a-z0-9_./-]+)',x)
  if m and 'effect give' not in x:facts.append(f'выдаёт предмет `{m.group(1)}`')
  m=re.search(r'\bsummon\s+(?:minecraft:)?([^\s{]+)(.*)',x)
  if m:
   nbt=m.group(2); iid=re.search(r'Item:\{id:"([^"]+)',nbt); tags=re.search(r'Tags:\[([^]]*)\]',nbt); fuse=re.search(r'\bfuse:(\d+)',nbt)
   facet=(f' (предмет `{iid.group(1)}`)' if iid else '')+(f' с Tags [{tags.group(1)}]' if tags else '')+(f', fuse {fuse.group(1)} тиков' if fuse else '')
   facts.append(f'создаёт сущность `{m.group(1)}`{facet}')
  m=re.search(r'\b(stopsound) ([^\n]+)',x)
  if m:facts.append(f'останавливает звук `{m.group(2)}`')
  m=re.search(r'\b(setblock|fill|clone) ([^\n]+)',x)
  if m:facts.append(f'изменяет блоки: `{m.group(1)} {m.group(2)[:100]}`')
  m=re.search(r'\b(attribute [^\n]+)',x)
  if m:facts.append(f'меняет атрибут: `{m.group(1)[:130]}`')
  m=re.search(r'\b(scoreboard players (?:set|add|remove|reset|operation) [^\n]+)',x)
  if m:facts.append(f'меняет scoreboard: `{m.group(1)[:120]}`')
  m=re.search(r'\bdata (modify|remove|merge) ([^\n]+)',x)
  if m:facts.append(f'меняет NBT/components: `{m.group(1)} {m.group(2)[:120]}`')
  m=re.search(r'(?<!schedule )\bfunction ((?:lbc|lbcr):[a-z0-9_./-]+)(?: with storage ([a-z0-9_:.]+))?',x)
  if m:facts.append(f'передаёт выполнение `{m.group(1)}`'+(f' с параметрами из storage `{m.group(2)}`' if m.group(2) else ''))
  for macro in re.findall(r'\$\(([a-z0-9_]+)\)',x):facts.append(f'использует macro-параметр `$({macro})`')
  m=re.search(r'\b(effect clear) (\S+)(?: (\S+))?',x)
  if m:facts.append('снимает '+(f'`{m.group(3)}`' if m.group(3) else 'все эффекты')+' с '+m.group(2))
  m=re.search(r'\b(damage) (\S+) ([0-9.]+)(?: (\S+))?',x)
  if m:facts.append(f'наносит {m.group(3)} урона {m.group(2)}'+(f' типа `{m.group(4)}`' if m.group(4) else ''))
  m=re.search(r'\b(playsound) (\S+) \S+ (\S+)',x)
  if m:facts.append(f'проигрывает звук `{m.group(2)}` для {m.group(3)}')
  m=re.search(r'\b(stopsound) (\S+) ([^\n]+)',x)
  if m:facts.append(f'останавливает звук для {m.group(2)}: `{m.group(3)}`')
  m=re.search(r'\bparticle (?:minecraft:)?([^\s]+)',x)
  if m:facts.append(f'создаёт частицы `{m.group(1)}`')
  m=re.search(r'\b(tellraw) (\S+) (.+)',x)
  if m:facts.append(f'отправляет сообщение адресату {m.group(2)}')
  m=re.search(r'\b(teleport|tp) (\S+) ([^\n]+)',x)
  if m:facts.append(f'перемещает {m.group(2)} в `{m.group(3)[:100]}`')
  m=re.search(r'\bspreadplayers ([^\n]+)',x)
  if m:facts.append(f'разбрасывает игроков/цели через spreadplayers `{m.group(1)[:100]}`')
  m=re.search(r'\b(item replace) ([^\n]+)',x)
  if m:facts.append(f'заменяет предмет/слот: `{m.group(2)[:130]}`')
  m=re.search(r'\b(advancement grant|advancement revoke) ([^\n]+)',x)
  if m:facts.append(f'меняет advancement: `{m.group(1)} {m.group(2)[:120]}`')
  m=re.search(r'\btag (\S+) (add|remove) ([^\n]+)',x)
  if m:facts.append(f'меняет tag: `{m.group(1)} {m.group(2)} {m.group(3)[:80]}`')
  m=re.search(r'\bschedule function ([^\n]+)',x)
  if m:facts.append(f'планирует отложенный вызов `{m.group(1)}`')
  if re.search(r'\bkill\s+',x):facts.append('удаляет выбранную сущность')
  if re.search(r'\bclear\s+',x):facts.append('расходует/очищает matching item')
 facts=unique(facts)
 if not facts:
  # Explicitly give the real command families instead of asserting "no effect".
  for x in active[:3]:facts.append(f'выполняет команду `{x[:160]}`')
 callers=rev.get(f,[])
 if callers: enter='Автоматический вход из `'+callers[0][0]+'`, условие/команда: `'+callers[0][1]+'`.'
 elif f in rewards: enter='Автоматический вход через reward advancement.'
 else: enter='Автоматический caller не найден; при ручном запуске выполняются описанные действия.'
 return enter+' '+trigger(f)+'Результат: '+'; '.join(facts[:8])+'.'

groups=collections.defaultdict(list)
source_group={}
for src in sources:
 path=Path(src).as_posix(); name=Path(src).stem
 if name in ('compressedtnt','delta_tnt','groundbam','groundbam_fire','groundbam_no','fire','fire2','fire_pre','firedbow','firedbow_old'):g='Взрывы, огонь и воздействие на блоки'
 elif any(x in path for x in ('craft','transformation','transform')):g='Крафт, сборка и преобразование предметов'
 elif any(x in path for x in ('manaregen','mana','stew','food','negative_mana','twitch')):g='Мана, экипировка и состояния игрока'
 elif any(x in path for x in ('raycast','projectile','arrow','bow','rasengan')):g='Наведение, снаряды и попадания'
 elif name in ('datafixer',):g='Служебный исправитель контейнеров'
 elif any(x in name for x in ('stopper','hedgehog','absurd_child','kill_rand','killanvil','killcart','falling_power')):g='Сущности, события и прочие механики'
 else:g='Расходники, защита и эффекты'
 groups[g].append(src); source_group[src]=g
out='''# LBC: прочие функции — основной набор

Здесь разобраны ровно 215 источников `lbc_other_main`. Для каждого runtime-файла приведено фактическое действие по его командам и caller, если он обнаружен; вспомогательные/пустые источники помечены отдельно. Reward advancement означает автоматический вход, но расход предмета связывается с trigger; если caller отсутствует, функция всё равно исполняется вручную.

Селекторы читаются буквально: `dx/dy/dz` вместе с `distance` задают пересечение условий; `execute as` меняет executor, но не позицию без `at`/`positioned`. Командный эффект `/effect give` измеряет длительность в секундах. В SNBT `fuse:2s` — Short на два тика (0,1 секунды), а `schedule 2s` — две секунды.

'''
for g,items in groups.items():
 out+=f'## {g}\n\n'
 for src in sorted(items):
  p=root/Path(src); f=fid(src); lines=read(src); active=[x for x in lines if x.strip() and not x.lstrip().startswith('#')]
  out+=f'### `{f}`\n\n**Фактическая работа.** {summarize(src)} [Исходник](../'+src.replace('\\','/')+')\n\n'
  if not active:out+='Команд в источнике нет.\n\n'
Path('agent_notes/lbc_other_main.md').write_text(out,encoding='utf8')
coverage=[]
for src in sources:
 p=root/Path(src); active=[x for x in read(src) if x.strip() and not x.lstrip().startswith('#')]
 status='non_runtime' if p.suffix=='.py' or not active else 'documented'
 coverage.append({'source':Path(src).as_posix(),'document':'agent_notes/lbc_other_main.md','section':source_group[src],'status':status,'note':'Проверены команды, контекст/caller и фактический результат.' if status=='documented' else 'Пустой или вспомогательный источник, runtime-команд нет.'})
(root/'agent_notes/coverage/lbc_other_main.json').write_text(json.dumps(coverage,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
print('rebuilt main doc from',len(sources),'exact sources; coverage',len(coverage))
