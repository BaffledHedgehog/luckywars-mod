from pathlib import Path
base=Path('world/datapacksdisabled/LBC/data/lbc/function/other/artifact/colors')
for p in sorted(base.glob('*.mcfunction')):
 print('\n### '+p.stem)
 for line in p.read_text(encoding='utf-8').splitlines():
  if any(x in line for x in ('effect give','effect clear','scoreboard','fill ','attribute','execute if block','teleport','tp ')): print(line)
