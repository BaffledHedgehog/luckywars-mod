"""Link every literal maploader template call to its NBT and report gaps."""
import json,re
from collections import defaultdict
from pathlib import Path
root=Path(__file__).resolve().parents[2]
loader=root/'world/datapacksdisabled/swarogskywars/data/swrg/function/maploader'
rx=re.compile(r'\bplace\s+template\s+swrg:([^\s]+)')
refs=defaultdict(list); calls_by_function=defaultdict(list); dynamic=[]
for path in sorted(loader.rglob('*.mcfunction')):
    rel=path.relative_to(root).as_posix()
    for line_no,line in enumerate(path.read_text(encoding='utf-8-sig').splitlines(),1):
        m=rx.search(line)
        if not m: continue
        ident=m.group(1).rstrip(',;')
        if not re.fullmatch(r'[A-Za-z0-9_./-]+',ident):
            dynamic.append({'function':rel,'line':line_no,'expression':ident,'command':line.strip()})
            continue
        # position is the first three integer arguments after the template id.
        tail=line[m.end():].strip().split()
        position=None
        if len(tail)>=3 and all(re.fullmatch(r'-?\d+',v) for v in tail[:3]):
            position=[int(v) for v in tail[:3]]
        # `none none <integrity> <seed> [strict]` follows the position.
        integrity=seed=mode=None
        if len(tail)>=8 and tail[3:5]==['none','none']:
            try: integrity=float(tail[5])
            except ValueError: pass
            seed=tail[6]
            mode=tail[7]
        row={'function':rel,'line':line_no,'position':position,'integrity':integrity,'seed':seed,
             'placement_mode':mode,'command':line.strip()}
        refs[ident].append(row); calls_by_function[rel].append(ident)

catalogue=root/'agent_notes/inventory/maps_structures.json'
data=json.loads(catalogue.read_text(encoding='utf-8'))
map_rows=[r for r in data['structures'] if r['group']=='maps']
by_id={Path(r['source'].split('/structure/',1)[1]).with_suffix('').as_posix():r for r in map_rows}
unlinked=[]
for row in map_rows:
    ident=Path(row['source'].split('/structure/',1)[1]).with_suffix('').as_posix()
    row['maploader_calls']=refs.get(ident,[])
    if not row['maploader_calls']: unlinked.append(row['source'])

function_summary=[]; families=defaultdict(lambda:{'ids':set(),'matched':set(),'functions':[],'calls':0})
for function,ids in sorted(calls_by_function.items()):
    unique=set(ids); found=unique & set(by_id); missing=sorted(unique-found)
    status='complete' if unique and not missing else ('partial' if found else 'zero')
    path=Path(function); name=path.stem
    family=re.sub(r'(?:_broken|_brokenb|_broken2|_broken1)$','',name)
    item={'function':function,'family':family,'calls':len(ids),'unique_template_ids':len(unique),
          'matched_nbt_ids':len(found),'unmatched_nbt_ids':missing,'status':status}
    function_summary.append(item)
    fam=families[family]; fam['ids'].update(unique); fam['matched'].update(found); fam['functions'].append(function); fam['calls']+=len(ids)
family_summary=[]
for family,v in sorted(families.items()):
    missing=sorted(v['ids']-v['matched'])
    status='complete' if v['ids'] and not missing else ('partial' if v['matched'] else 'zero')
    family_summary.append({'family':family,'functions':sorted(v['functions']),'calls':v['calls'],
        'unique_template_ids':len(v['ids']),'matched_nbt_ids':len(v['matched']),
        'unmatched_nbt_ids':missing,'status':status})
referenced=set(refs); unmatched=sorted(referenced-set(by_id))
fnroot=root/'world/datapacksdisabled/LBC/data/lbc/function'
lbc_rx=re.compile(r'\bplace\s+(template|structure)\s+lbc:([^\s]+)')
lbc_refs=defaultdict(list); lbc_dynamic=[]
for path in sorted(fnroot.rglob('*.mcfunction')):
    rel=path.relative_to(root).as_posix()
    for line_no,line in enumerate(path.read_text(encoding='utf-8-sig').splitlines(),1):
        m=lbc_rx.search(line)
        if not m: continue
        kind,ident=m.groups(); ident=ident.rstrip(',;')
        if not re.fullmatch(r'[A-Za-z0-9_./-]+',ident):
            lbc_dynamic.append({'function':rel,'line':line_no,'expression':ident,'command':line.strip()}); continue
        lbc_refs[ident].append({'kind':kind,'function':rel,'line':line_no,'command':line.strip()})
lbc_rows=[r for r in data['structures'] if r['group']=='lbc']
lbc_by_id={Path(r['source'].split('/structure/',1)[1]).with_suffix('').as_posix():r for r in lbc_rows}
for row in lbc_rows:
    ident=Path(row['source'].split('/structure/',1)[1]).with_suffix('').as_posix()
    row['lbc_callers']=lbc_refs.get(ident,[])
lbc_unmatched=sorted(set(lbc_refs)-set(lbc_by_id))
data['maploader_summary']={'literal_calls':sum(map(len,refs.values())),'template_ids':len(referenced),
  'linked_map_files':sum(bool(r['maploader_calls']) for r in map_rows),'map_files_without_literal_calls':unlinked,
  'referenced_template_ids_without_nbt':unmatched,'dynamic_or_unparsed_calls':dynamic,
  'functions':function_summary,'families':family_summary}
data['lbc_summary']={'literal_calls':sum(map(len,lbc_refs.values())),'template_ids':len(lbc_refs),
  'linked_structure_files':sum(bool(r['lbc_callers']) for r in lbc_rows),
  'structure_files_without_literal_callers':[r['source'] for r in lbc_rows if not r['lbc_callers']],
  'referenced_ids_without_snapshot_nbt':lbc_unmatched,'dynamic_or_unparsed_calls':lbc_dynamic}
catalogue.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
from collections import Counter
print(f"literal calls={sum(map(len,refs.values()))}; unique IDs={len(referenced)}; map NBT linked={sum(bool(r['maploader_calls']) for r in map_rows)}/{len(map_rows)}; unmatched IDs={len(unmatched)}; dynamic={len(dynamic)}")
print('function statuses:',dict(Counter(x['status'] for x in function_summary)))
print('family statuses:',dict(Counter(x['status'] for x in family_summary)))
print('unparsed sample:',dynamic[:5])
print(f"LBC literal calls={data['lbc_summary']['literal_calls']}; IDs={data['lbc_summary']['template_ids']}; NBTs linked={data['lbc_summary']['linked_structure_files']}/{len(lbc_rows)}; unmatched IDs={len(lbc_unmatched)}; dynamic={len(lbc_dynamic)}")
