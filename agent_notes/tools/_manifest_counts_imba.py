import json
from pathlib import Path
m=json.loads(Path('agent_notes/inventory/review_other_imba_manifest.json').read_text(encoding='utf-8'))
for name in ('imbaxe_use','imbaxe_use_at','imbaxe_use_macro','chaos_sho_use','tmp'):
 r=next(x for x in m['records'] if x['source'].endswith('/'+name+'.mcfunction'))
 print(name,r['command_count'],'outputs',len(r['outputs']),'literal',len(r['literal_callers']))
for i in (1,2,10,100):
 r=next(x for x in m['records'] if x['source'].endswith(f'/pasta/{i}.mcfunction'))
 print('pasta',i,r['command_count'],len(r['literal_callers']))
