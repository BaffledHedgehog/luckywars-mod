from pathlib import Path
base=Path('world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab')
for p in [base/'cast.mcfunction',base/'correction_cunny.mcfunction',base/'correction_cunny_uoh.mcfunction',base/'correction_cunny_uoh_kanikani.mcfunction',base/'correction_cunny_uoh_pasta_select.mcfunction',base/'end.mcfunction',base/'tick.mcfunction',base/'tick1.mcfunction']:
 print('\n### '+p.stem+' ('+str(len(p.read_text(encoding='utf-8').splitlines()))+')'); print(p.read_text(encoding='utf-8'))
