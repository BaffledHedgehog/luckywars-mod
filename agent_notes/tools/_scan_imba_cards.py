import json,re
from pathlib import Path
job=json.loads(Path('agent_notes/inventory/research_jobs.json').read_text(encoding='utf-8'))['lbc_other_imba']
md=Path('agent_notes/lbc_other_imba.md').read_text(encoding='utf-8')
for s in job['sources']:
 name=s.split('/function/')[1][:-len('.mcfunction')].replace('/','/')
 m=re.search(r'### `lbc:'+re.escape(name)+r'`\n(.*?)(?=\n### |\n## |\Z)',md,re.S)
 if not m: print('NO CARD',name); continue
 txt=m.group(1).strip().splitlines()[0] if m.group(1).strip() else ''
 if any(x in txt.lower() for x in ['caller не найден','не найдены','служебн','перечень команд','передаёт шаг','helper','не видно','нет команд','управляющ','вспомогательн','по имени','не определяет','таблиц','вызывает']):
  print(name, '=>', txt[:260])
