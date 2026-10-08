import json,re
from pathlib import Path
jobs=['lbc_other_events','lbc_other_magic','lbc_other_world_tech','lbc_other_imba','lbc_other_attributes','lbc_other_consumables_craft','lbc_other_main','lbc_other_misc']
for job in jobs:
 p=Path('agent_notes/coverage/'+job+'.json'); rows=json.loads(p.read_text(encoding='utf8'))
 for row in rows:
  doc=Path(row['document']); s=doc.read_text(encoding='utf8'); heads={m.group(1).strip() for m in re.finditer(r'^#{1,6}\s+(.+)$',s,re.M)}
  if row['section'] in heads: continue
  parts=Path(row['source']).parts
  i=parts.index('function'); namespace=parts[parts.index('data')+1]; path='/'.join(parts[i+1:])[:-len('.mcfunction')]
  h=f'`{namespace}:{path}`'
  if h in heads: row['section']=h
  else: print('NO H3',job,row['source'],h)
 p.write_text(json.dumps(rows,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
