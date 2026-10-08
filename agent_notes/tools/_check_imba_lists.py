import re
from pathlib import Path
lines=Path('world/datapacksdisabled/LBC/data/lbc/function/other/imba/imbaxe_use_at.mcfunction').read_text(encoding='utf-8').splitlines()
cs=[c for c in lines if ' run fill ' in c]
print('fill',len(cs))
print('minecraft target regex',sum(bool(re.search(r' replace (minecraft:[^ ]+) strict$',c)) for c in cs))
for c in cs:
 m=re.search(r' replace (\S+) strict$',c)
 if m and not m.group(1).startswith('minecraft:'): print('non-minecraft',m.group(1),c)
