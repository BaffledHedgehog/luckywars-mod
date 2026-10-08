import re
from pathlib import Path
base=Path('world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta')
rows=[]
variants={}
for i in range(1,101):
 p=base/f'{i}.mcfunction'; lines=p.read_text(encoding='utf-8').splitlines()
 vals=[]
 for j,line in enumerate(lines,1):
  m=re.fullmatch(r'data modify storage lbc:math t([1-4]) set value "(.*)"',line)
  if not m: raise ValueError(f'{p}:{j}: unexpected command {line!r}')
  vals.append(m.group(2))
 if len(vals)!=4: raise ValueError(f'{p}: wanted 4 commands got {len(vals)}')
 variants[str(i)]=vals
 rows.append(f'| {i} | `t1`: `{vals[0]}`<br>`t2`: `{vals[1]}`<br>`t3`: `{vals[2]}`<br>`t4`: `{vals[3]}` | [source](../world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/{i}.mcfunction) |')

p=Path('agent_notes/lbc_other_imba.md'); s=p.read_text(encoding='utf-8')
start=s.index('## Генерация Cunny sign и макро-случайные тексты')
end=s.index('\n## Поведенческий обзор и проверенные развилки',start)
intro='''## Генерация Cunny sign и макро-случайные тексты

`cunny_slab/tick` выбирает correction marker; `correction_cunny` смещается на 10 блоков и удаляет marker после работы. `correction_cunny_uoh` превращает воздух под позицией в barrel с loot table `lbc:imba/cunny_slabs`, выбирает дерево из 12 вариантов и rotation 0..15. Затем она дважды независимо выбирает номер 1..100: первый текст копирует в `t12..t42` для задней стороны, второй задаёт `t1..t4` для передней. `correction_cunny_uoh_kanikani` ставит waxed sign с обеими сторонами; текстовые макросы ниже только записывают четыре строки в storage и сами мир не меняют. Это не один фиксированный случайный текст: таблица показывает все 100 полных наборов четырёх строк, а выборы сторон независимы.

| Номер | Четыре строки, задаваемые в storage | Файл |
|---:|---|---|
'''+ '\n'.join(rows)+'\n'
s=s[:start]+intro+s[end:]
# Replace each pasta source card with exact-difference row reference and trigger semantics.
for i,vals in variants.items():
 name=f'lbc:other/imba/cunny_slab/pasta/{i}'
 pat=r'(### `'+re.escape(name)+r'`\n)(.*?)(?=\n### |\n## |\Z)'
 repl=r'\1Вариант `'+i+r'` записывает полный набор четырёх строк `t1..t4` в `storage lbc:math`; отдельного расхода или изменения мира нет. Автоматический вызов выполняет макрос `correction_cunny_uoh_pasta_select` для независимого выбора текста на каждой стороне Cunny sign; точные строки перечислены в таблице генерации выше. [Исходник](../world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta/'+i+r'.mcfunction)'
 s,n=re.subn(pat,repl,s,count=1,flags=re.S)
 if n!=1: raise ValueError(f'could not replace card {name}, count={n}')
t=p.with_suffix('.md.tmp'); t.write_text(s,encoding='utf-8',newline='\n'); t.replace(p)
print('wrote',len(rows),'full variant rows')
