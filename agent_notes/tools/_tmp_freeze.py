from pathlib import Path
base=Path('world/datapacksdisabled/LBC/data/lbc/function/other/freeze/block')
for p in sorted(base.glob('*.mcfunction')):
 print('\n###',p.name); print(p.read_text(encoding='utf8'))
