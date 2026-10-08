from pathlib import Path
for folder in ['bad_apple','blood_slab','cum_slab']:
 base=Path('world/datapacksdisabled/LBC/data/lbc/function/other/imba')/folder
 for p in sorted(base.glob('*.mcfunction')):
  print('\n### '+folder+'/'+p.stem+' ('+str(len(p.read_text(encoding='utf-8').splitlines()))+')')
  print(p.read_text(encoding='utf-8'))
