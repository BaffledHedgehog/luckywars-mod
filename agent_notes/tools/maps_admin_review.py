"""Temporary source extraction helpers for the maps/admin audit."""
import json
from pathlib import Path
root=Path(__file__).resolve().parents[2]
job=json.loads((root/'agent_notes/inventory/research_jobs.json').read_text(encoding='utf-8'))['skywars_maps_admin']
if '--callers' not in __import__('sys').argv:
    for rel in job['sources']:
        path=root/rel
        if '/developer/' in rel and rel.endswith('.mcfunction') and path.stat().st_size < 7000:
            print('\n### '+rel)
            print(path.read_text(encoding='utf-8'))

if '--callers' in __import__('sys').argv:
    import collections,re
    base=root/'world/datapacksdisabled/swarogskywars/data/swrg/function'
    wanted=re.compile(r'function\s+swrg:(maploader/(?:map_select_[a-z0-9_]+|lobby|clear1?|map/always[a-z_]+)|developer/[A-Za-z0-9_./-]+)')
    callers=collections.defaultdict(list)
    for path in base.rglob('*.mcfunction'):
        if '/maploader/' in path.as_posix(): continue
        rel=path.relative_to(root).as_posix()
        for line_no,line in enumerate(path.read_text(encoding='utf-8-sig').splitlines(),1):
            for m in wanted.finditer(line): callers[m.group(1)].append((rel,line_no,line.strip()))
    print('\n### CALLER SUMMARY')
    for target,refs in sorted(callers.items()):
        print(f'{target}: {len(refs)}')
        for ref in refs[:5]: print(' ',*ref)

if '--mapstats' in __import__('sys').argv:
    import collections,re
    catalog=json.loads((root/'agent_notes/inventory/maps_structures.json').read_text(encoding='utf-8'))
    known={Path(r['source'].split('/structure/',1)[1]).stem for r in catalog['structures'] if r['group']=='maps'}
    jobpaths=[p for p in job['sources'] if '/map/' in p and p.endswith('.mcfunction')]
    rx=re.compile(r'place template swrg:([^\s]+)\s+(-?\d+)\s+(-?\d+)\s+(-?\d+)\s+none\s+none\s+([0-9.]+)\s+(\S+)')
    parsed={}
    for rel in jobpaths:
        rows=[]; path=root/rel
        for ln,line in enumerate(path.read_text(encoding='utf-8-sig').splitlines(),1):
            m=rx.search(line)
            if m:
                ident,x,y,z,integrity,seed=m.groups()
                rows.append((ident,int(x),int(y),int(z),float(integrity),seed,line.strip()))
        parsed[rel]=rows
    print('MAP FUNC STATS')
    for rel,rows in sorted(parsed.items()):
        if not rows: continue
        coords=collections.Counter(x[4] for x in rows); ys=sorted({x[2] for x in rows});
        missing=sorted({x[0] for x in rows if x[0] not in known})
        score=[l.strip() for l in (root/rel).read_text(encoding='utf-8-sig').splitlines() if 'scoreboard players set #map ' in l]
        print(Path(rel).stem,len(rows),'unique',len({x[0] for x in rows}),'missing',len(missing),'integrity',coords,'y',ys,'score',score[:1], 'id-sample',','.join(missing[:3]))
    pairs=[]
    by_stem={Path(k).stem:k for k in parsed}
    for stem,rel in by_stem.items():
        if not stem.endswith('_broken'): continue
        base=stem.removesuffix('_broken')
        if base not in by_stem: continue
        a=parsed[by_stem[base]]; b=parsed[rel]
        def norm(rows): return [re.sub(r'(none none )0\.75( )',r'\g<1>1\g<2>',r[-1]) for r in rows]
        pairs.append((base,len(a),len(b),len(a)==len(b) and norm(a)==norm(b),collections.Counter(r[4] for r in b)))
    print('BROKEN PAIRS',len(pairs),'exact except 0.75->1',sum(p[3] for p in pairs),'different',sum(not p[3] for p in pairs))
    print('DIFF PAIRS',[p for p in pairs if not p[3]][:30])
