from pathlib import Path
base=Path('world/datapacksdisabled/LBC/data/lbc/function/other/imba/runes')
for p in sorted(base.glob('*.mcfunction')):
 print('\n### '+p.stem+' ('+str(len(p.read_text(encoding='utf-8').splitlines()))+')'); print(p.read_text(encoding='utf-8'))
