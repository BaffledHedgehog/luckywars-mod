import json
from pathlib import Path
m=json.loads(Path('agent_notes/inventory/review_other_imba_read.json').read_text(encoding='utf-8'))
print('read_records',len(m['records']),'methods',m['method_counts'],'template_groups',m['template_group_counts'])
for r in m['records']:
 if r['source'].endswith('/imbaxe_use_at.mcfunction') or r['source'].endswith('/chaos_sho_use.mcfunction') or '/pasta/100.' in r['source']:
  print(r['source'],r['method'],r['command_count'])
