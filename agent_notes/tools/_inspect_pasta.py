from pathlib import Path
base=Path('world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/pasta')
for name in ['1','2','3','10','100']:
 p=base/(name+'.mcfunction'); print('\n###',name,p.exists())
 if p.exists(): print('\n'.join(p.read_text(encoding='utf-8').splitlines()))
