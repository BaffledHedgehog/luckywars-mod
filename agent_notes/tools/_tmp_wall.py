from pathlib import Path
base=Path('world/datapacksdisabled/LBC/data/lbc/function/other/baffledwall')
for p in sorted(base.glob('*.mcfunction')):
 print('\n##',p.name)
 for i,l in enumerate(p.read_text(encoding='utf8').splitlines()):
  if l.strip() and not l.lstrip().startswith('#'): print(l)
