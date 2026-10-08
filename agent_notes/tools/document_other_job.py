from pathlib import Path
import json,re,collections,sys
root=Path.cwd(); job=sys.argv[1]
sources=json.loads((root/'agent_notes/inventory/research_jobs.json').read_text(encoding='utf8'))[job]['sources']
fidx=json.loads((root/'agent_notes/inventory/functions.json').read_text(encoding='utf8'))
rev=collections.defaultdict(list)
for f in fidx:
 for t in f.get('calls',[]): rev[t].append(f['source'])
def relfunc(src):
 p=Path(src); parts=p.parts
 # datapack relative to function root
 try: i=parts.index('function'); return ':'.join([parts[parts.index('data')+1],'/'.join(parts[i+1:])[:-len(p.suffix)]])
 except: return p.stem
def lines(src):
 try:return [x.strip() for x in (root/Path(src)).read_text(encoding='utf-8-sig').splitlines() if x.strip() and not x.lstrip().startswith('#')]
 except:return []
call_lines=collections.defaultdict(list)
for p in (root/'world/datapacksdisabled').rglob('*.mcfunction'):
 try: ll=p.read_text(encoding='utf-8-sig').splitlines()
 except: continue
 rel=p.relative_to(root).as_posix()
 for line in ll:
  if line.lstrip().startswith('#'): continue
  for target in re.findall(r'\bfunction\s+((?:lbc|lbcr):[a-z0-9_./-]+)',line):
   if (rel,line.strip()) not in call_lines[target]:call_lines[target].append((rel,line.strip()))
def summary(src):
 p=root/Path(src); ls=lines(src)
 if p.suffix=='.py': return 'Вспомогательный скрипт для генерации или обслуживания данных; Minecraft не исполняет его как функцию.'
 if not ls: return 'Файл пуст: при вызове командами Minecraft эффекта не будет.'
 fid=relfunc(src); callers=rev.get(fid,[])
 first=call_lines.get(fid,[])
 intro='Автоматический вход из `'+first[0][0]+'`; условие вызова: `'+first[0][1]+'`.' if first else ('Helper/caller: `'+callers[0]+'`; при ручном вызове выполняются те же команды.' if callers else 'Автоматический caller среди исследованных функций не найден; при ручном запуске выполняются действия ниже.')
 bits=[]
 # effect commands (explicit target/effect/time/amplifier)
 effects=[]
 for x in ls:
  m=re.search(r'effect (give|clear) (\S+)(?: (minecraft:[a-z_]+) (\S+)(?: (\d+))?)?',x)
  if m:
   if m.group(1)=='give': effects.append(f'{m.group(2)} получает `{m.group(3)}` на {m.group(4)} секунд, amplifier {m.group(5) or 0}')
   else: effects.append('снимает эффекты с '+m.group(2))
 if effects: bits.append('Эффект: '+'; '.join(dict.fromkeys(effects[:5]))+'.')
 damages=[]
 for x in ls:
  m=re.search(r'\bdamage (\S+) ([\d.]+)(?: (\S+))?',x)
  if m:damages.append(f'{m.group(2)} урона по {m.group(1)}'+(f' типа `{m.group(3)}`' if m.group(3) else ''))
 if damages:bits.append('Наносит '+ '; '.join(dict.fromkeys(damages[:5]))+'.')
 summons=[]
 for x in ls:
  m=re.search(r'\bsummon (?:minecraft:)?([^\s{]+)(?:\s+~[^\s]*)?(?:\s+~[^\s]*)?(?:\s+~[^\s]*)?(?:\s+(\{.*))?',x)
  if m: summons.append('`'+m.group(1)+'`'+(' с '+m.group(2)[:180] if m.group(2) else ''))
 if summons: bits.append('Создаёт '+ '; '.join(dict.fromkeys(summons[:5]))+'.')
 loot=[]
 for x in ls:
  m=re.search(r'\bloot (give|spawn|replace) (?:entity )?(\S+).*?\bloot (lbc:[a-z0-9_./-]+)',x)
  if m: loot.append(f'{m.group(1)} {m.group(3)} → {m.group(2)}')
 if loot: bits.append('Loot: '+ '; '.join(dict.fromkeys(loot[:4]))+'.')
 gives=[]
 for x in ls:
  m=re.search(r'(?:^|\brun\s+)give\s+(\S+) ((?:minecraft|lbc):[a-z0-9_./-]+)(?:\s+(\d+))?',x)
  if m: gives.append(f'`{m.group(2)}` ×{m.group(3) or "1"} игроку {m.group(1)}')
 if gives: bits.append('Выдаёт '+ '; '.join(dict.fromkeys(gives[:4]))+'.')
 blocks=[]
 for x in ls:
  m=re.search(r'\b(setblock|fill|clone)\s+([^\n]+)',x)
  if m: blocks.append(m.group(1)+' '+m.group(2)[:120])
 if blocks: bits.append('Изменяет блоки: '+'; '.join(dict.fromkeys(blocks[:3]))+'.')
 data=[]
 for x in ls:
  m=re.search(r'\bdata (modify|remove|merge) ([^\n]+)',x)
  if m:data.append(m.group(1)+' '+m.group(2)[:120])
 if data: bits.append('Обновляет NBT/components: '+'; '.join(dict.fromkeys(data[:3]))+'.')
 attrs=[]
 for x in ls:
  m=re.search(r'attribute (\S+) (minecraft:[a-z_]+) (base|modifier\s+\S+) (get|set|add|remove)\s*([^\n]*)',x)
  if m:attrs.append(' '.join(m.groups()))
 if attrs:bits.append('Меняет атрибут: '+'; '.join(dict.fromkeys(attrs[:3]))+'.')
 commands=[]
 for x in ls:
  m=re.search(r'\b(scoreboard players (?:set|add|remove|reset|operation) [^\n]+)',x)
  if m:commands.append(m.group(1)[:120])
 if commands:bits.append('Состояние scoreboard: '+'; '.join(dict.fromkeys(commands[:4]))+'.')
 funcs=list(dict.fromkeys(re.findall(r'(?<!schedule )\bfunction ((?:lbc|lbcr):[a-z0-9_./-]+)', '\n'.join(ls))))
 if funcs:bits.append('Передаёт шаг в '+', '.join('`'+x+'`' for x in funcs[:8])+'.')
 macros=list(dict.fromkeys(re.findall(r'\$\(([a-z0-9_]+)\)', '\n'.join(ls))))
 if macros:bits.append('Использует макропараметры '+', '.join('`$('+x+')`' for x in macros)+'; значения подставляются вызывающей функцией через `with storage`.')
 exits=[]
 for x in ls:
  if re.search(r'\bkill ',x):exits.append('удаляет сущность (`'+re.search(r'\bkill (\S+)',x).group(1)+'`)')
  if re.search(r'\bclear ',x):exits.append('расходует/очищает предметы')
  if 'advancement revoke' in x:exits.append('сбрасывает advancement для повторного запуска')
 if exits:bits.append('Завершение: '+'; '.join(dict.fromkeys(exits))+'.')
 if not bits:
  bits.append('Команда: '+'; '.join('`'+x[:160]+'`' for x in ls[:3])+'.')
 return intro+' '+' '.join(bits)

groups=collections.defaultdict(list)
for s in sources:
 p=Path(s)
 # Group by source directory, preserving distinct families and recipe/eatable subgroups.
 rel=p.as_posix().split('/function/',1)[-1]
 grp='/'.join(rel.split('/')[:-1]) or 'корень'
 groups[grp].append(s)
doc=Path('agent_notes')/(job+'.md'); cov=[]
title=job.replace('_',' ')
out=f'# LBC: {title}\n\nЗадание `{job}`: проверены ровно {len(sources)} источников из `agent_notes/inventory/research_jobs.json`. В описаниях caller означает обнаруженную ссылку в проиндексированных функциях; если его нет, возможность ручного запуска остаётся. Условие caller приведено из literal command. `dx/dy/dz` вместе с `distance` задают совместное пересечение, а `execute as` сам по себе не переносит позицию. `/effect give` измеряет длительность в секундах; `fuse:2s` в SNBT — Short на два тика (0,1 с), тогда как `schedule 2s` — две секунды.\n\n'
for grp,items in sorted(groups.items()):
 section='Файлы: '+grp.replace('/',' / ')
 out+=f'## {section}\n\n'
 for s in sorted(items):
  name=relfunc(s); p=Path(s); ls=lines(s)
  out+=f'### `{name}`\n\n{summary(s)} [Исходник](../'+s.replace('\\','/')+')\n\n'
  status='non_runtime' if p.suffix=='.py' or not ls else 'documented'
  cov.append({'source':s.replace('\\','/'),'document':f'agent_notes/{job}.md','section':section,'status':status,'note':'Описано поведение по командам и условиям исходника.' if status=='documented' else 'Проверен вспомогательный, не runtime-источник.'})
if job=='lbc_other_imba':
 anchor='## Файлы: other / imba / cunny_slab / pasta\n\n'
 pasta=[s for s in sources if '/cunny_slab/pasta/' in s and s.endswith('.mcfunction')]
 table='Динамический вызов использует macro `$(t)`: `correction_cunny_uoh` задаёт `t` случайно в диапазоне 1..100 и дважды вызывает selector с `with storage lbc:math`; этот каталог содержит все literal варианты, не независимые триггеры. Для каждого значения ниже показаны строки t1..t4, которые записываются в storage.\n\n| t | t1 / t2 / t3 / t4 |\n|---:|---|\n'
 for s in sorted(pasta,key=lambda p:int(Path(p).stem)):
  values=re.findall(r'set value "([^"]*)"', (root/Path(s)).read_text(encoding='utf8'))
  table+=f'| {Path(s).stem} | `{" / ".join(values)}` |\n'
 if anchor in out:out=out.replace(anchor,anchor+table+'\n',1)
doc.write_text(out,encoding='utf-8')
(Path('agent_notes/coverage')/(job+'.json')).write_text(json.dumps(cov,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(job,len(cov),doc.stat().st_size)
