from pathlib import Path
import json
job=json.loads(Path('agent_notes/inventory/research_jobs.json').read_text(encoding='utf-8'))['lbc_other_imba']
for s in job['sources']:
 rel=s.split('/other/imba/')[-1] if '/other/imba/' in s else 'artifact/'+s.rsplit('/',1)[-1]
 if any(x in s for x in ['bad_apple/','blood_slab/','cum_slab/','cunny_slab/'] ) and '/pasta/' not in s:
  p=Path(s); print(f'{rel:60} {len(p.read_text(encoding="utf-8").splitlines()):4}')
