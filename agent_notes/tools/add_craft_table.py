from pathlib import Path
import re
src=Path('world/datapacksdisabled/LBC/data/lbc/function/other/craft_ground.mcfunction').read_text(encoding='utf-8-sig').splitlines()
rows=[]
for line in src:
 if 'run function lbc:' not in line: continue
 tags=re.findall(r'custom_data":\{([^}]+)\}',line)
 dest=re.search(r'run function (lbc:[a-z0-9_./-]+)',line)
 if len(tags)>=2 and dest:
  def clean(x): return ', '.join(re.findall(r'([a-zA-Z0-9_]+):1',x))
  rows.append((clean(tags[0]),clean(tags[1]),dest.group(1)))
p=Path('agent_notes/lbc_other_main.md'); s=p.read_text(encoding='utf-8')
heading='### `lbc:other/craft_ground`'
i=s.find(heading); j=s.find('\n### ',i+len(heading))
chunk=s[i:j]
if '#### Наземные рецепты' not in chunk:
 table='\n\n#### Наземные рецепты: проверяемые пары маркеров\n\n| Предмет `@s` custom_data | Ближайший второй предмет custom_data | Обработчик результата |\n|---|---|---|\n'
 for a,b,d in rows: table+=f'| `{a}` | `{b}` | `{d}` |\n'
 s=s[:j]+table+s[j:]
p.write_text(s,encoding='utf-8')
print(f'added {len(rows)} verified recipe rows')
