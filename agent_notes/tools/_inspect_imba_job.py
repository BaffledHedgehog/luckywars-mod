import json
from pathlib import Path
job=json.loads(Path('agent_notes/inventory/research_jobs.json').read_text(encoding='utf-8'))['lbc_other_imba']
print('sources',len(job['sources']))
from collections import Counter
c=Counter('/'.join(x.split('/')[:-1]) for x in job['sources'])
for k,v in sorted(c.items()): print(v,k)
print('docs',job.get('documents'))
