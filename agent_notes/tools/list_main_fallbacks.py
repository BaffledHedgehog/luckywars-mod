from pathlib import Path
import re
s=Path('agent_notes/lbc_other_main.md').read_text(encoding='utf8')
for m in re.finditer(r'### `([^`]+)`\s*\n\n\*\*Фактическая работа\.\*\* ([^\n]*)',s):
 if 'прямых эффектов не видно' in m.group(2):
  fid=m.group(1).split(':',1)[1]
  p=Path('world/datapacksdisabled/LBC/data/lbc/function')/(fid+'.mcfunction')
  try: lines=[x.strip() for x in p.read_text(encoding='utf8').splitlines() if x.strip() and not x.lstrip().startswith('#')]
  except: lines=[]
  print('\n'+m.group(1)+' :: '+' || '.join(lines[:10]))
