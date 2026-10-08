import re
from collections import Counter
from pathlib import Path
p=Path('world/datapacksdisabled/LBC/data/lbc/function/other/imba/chaos_sho_use.mcfunction')
c=Counter()
for line in p.read_text(encoding='utf-8').splitlines():
 m=re.search(r'positioned \^(-?\d+)? \^(-?\d+)? \^ run',line)
 if m: c[(int(m.group(1) or 0),int(m.group(2) or 0))]+=1
print('all',sum(c.values()),'unique',len(c))
for x in sorted(set(k[0] for k in c)):
 print(x, sorted((y,n) for (xx,y),n in c.items() if xx==x))
