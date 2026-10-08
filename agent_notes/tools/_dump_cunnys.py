from pathlib import Path
base=Path('world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/cunnys')
for p in sorted(base.glob('*.mcfunction')):
 if p.stem in {'4_spawn_cunny'}: continue
 print('\n### '+p.stem+' ('+str(len(p.read_text(encoding='utf-8').splitlines()))+')')
 print(p.read_text(encoding='utf-8'))
