from pathlib import Path
p=Path('world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/cunnys/4_spawn_cunny.mcfunction')
lines=p.read_text(encoding='utf-8').splitlines()
for n,l in enumerate(lines,1):
 if 'team join' in l: print(n,l)
 elif not l.startswith('execute if entity @s[team='):
  print(n,l if len(l)<220 else l[:220]+'...')
print('lines',len(lines),'joins',sum('team join' in x for x in lines),'unique teams',len(set(__import__('re').search(r'team=(\d+)',x).group(1) for x in lines if 'team join' in x)))
