from pathlib import Path
base=Path('world/datapacksdisabled/LBC/data/lbc/function/other/imba')
for name in ['chaos_sho_use','imbaxe_use','imbaxe_use_at','imbaxe_use_macro','tmp']:
 p=base/(name+'.mcfunction'); lines=p.read_text(encoding='utf-8').splitlines(); print('\n###',name,'lines',len(lines),'nonempty',sum(bool(x) for x in lines))
 print('FIRST:'); print('\n'.join(lines[:18])); print('LAST:'); print('\n'.join(lines[-15:]))
 from collections import Counter
 print('PREFIXES',Counter(x.split(' ',1)[0] for x in lines if x).most_common(15))
