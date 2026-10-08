from pathlib import Path
base=Path('world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects')
for d in base.iterdir():
 if d.is_dir():
  print('\n',d.name)
  for p in d.glob('*.mcfunction'):
   print(p.name,':',p.read_text(encoding='utf8').strip().replace('\n',' | ')[:250])
