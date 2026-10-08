from pathlib import Path
lines=Path('world/datapacksdisabled/LBC/data/lbc/function/other/imba/imbaxe_use.mcfunction').read_text(encoding='utf-8').splitlines()
for i,line in enumerate(lines):
 if line.startswith('execute if score'):
  print(i+1,line)
  if i>1000: break
