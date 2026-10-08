#!/usr/bin/env python3
"""Build a complete, content-derived catalogue of the repository's map NBT."""
import collections, hashlib, json, pathlib, sys
from maps_nbt import load

ROOT=pathlib.Path(__file__).resolve().parents[2]
SOURCES=[('maps','world/datapacksdisabled/maps'),('lbc','world/datapacksdisabled/LBC/data/lbc/structure')]
def xyz(v): return tuple(v) if isinstance(v,list) else None
def walk(v, path=''):
    if isinstance(v,dict):
        for k,x in v.items(): yield from walk(x, path+'/'+k)
    elif isinstance(v,list):
        for i,x in enumerate(v): yield from walk(x,path+f'[{i}]')
def summarize(path, group):
    raw=path.read_bytes(); name, n=load(path)
    size=n.get('size'); palette=n.get('palette',n.get('Palette',[])); blocks=n.get('blocks',n.get('Blocks',[])); ents=n.get('entities',n.get('Entities',[]))
    pnames=[]
    for p in palette:
        if isinstance(p,dict): pnames.append(p.get('Name',p.get('name','?')))
    pc=collections.Counter(pnames)
    counts=collections.Counter()
    for b in blocks:
        if isinstance(b,dict):
            i=b.get('state',b.get('State',-1)); counts[pnames[i] if isinstance(i,int) and 0<=i<len(pnames) else '?']+=1
    notable=[]
    for b in blocks:
        if not isinstance(b,dict): continue
        state=b.get('nbt',b.get('Nbt',b.get('data',{})))
        if not state: continue
        pos=xyz(b.get('pos',b.get('Pos')))
        stateid=pnames[b.get('state',-1)] if isinstance(b.get('state'),int) and 0<=b['state']<len(pnames) else '?'
        item={'pos':pos,'block':stateid,'nbt':state}
        # keep only meaningful markers, inventories, loot and commands
        text=json.dumps(state,ensure_ascii=False).lower()
        if any(k in text for k in ('command','loot','items','custom_data','customdata','marker','spawn','team','chest','sign','book','container','lock')):
            notable.append(item)
    entity_summary=[]
    for e in ents:
        if isinstance(e,dict):
            entity_nbt=e.get('nbt',e)
            entity_summary.append({'id':entity_nbt.get('id'),'pos':e.get('pos',entity_nbt.get('Pos')),
                                   'blockPos':e.get('blockPos'),'tags':entity_nbt.get('Tags',[]),
                                   'nbt':entity_nbt,'raw':e})
    return {'source':path.relative_to(ROOT).as_posix(),'group':group,'file_bytes':len(raw),'sha256':hashlib.sha256(raw).hexdigest(),
      'root_name':name,'size':size,'palette_size':len(palette),'block_entries':len(blocks),'entity_entries':len(ents),
      'palette':dict(sorted(pc.items())),'block_counts':dict(sorted(counts.items())),
      'markers':notable,'entities':entity_summary,'root_keys':sorted(n.keys()),
      'format_notes':{'data_version':n.get('DataVersion'),'author':n.get('author')}}

def main():
    rows=[]
    for group, rel in SOURCES:
        paths=sorted((ROOT/rel).rglob('*.nbt'))
        print(f'[{group}] {len(paths)} files',file=sys.stderr)
        for i,p in enumerate(paths,1):
            try: rows.append(summarize(p,group))
            except Exception as e: rows.append({'source':p.relative_to(ROOT).as_posix(),'group':group,'file_bytes':p.stat().st_size,'sha256':hashlib.sha256(p.read_bytes()).hexdigest(),'error':repr(e)})
            if i%100==0: print(f'  {i}/{len(paths)}',file=sys.stderr)
    out=ROOT/'agent_notes/inventory/maps_structures.json'; out.parent.mkdir(parents=True,exist_ok=True)
    out.write_text(json.dumps({'schema':'maps-structures-v1','total':len(rows),'sources':{g:sum(x['group']==g for x in rows) for g,_ in SOURCES},'structures':rows},ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    coverage=[]
    for item in rows:
        coverage.append({'source':item['source'],'document':'agent_notes/maps_structures.md',
          'section':'Наборы и полный каталог' if item['group']=='maps' else 'Структуры LBC',
          'status':'documented' if 'error' not in item else 'unresolved',
          'note':('Полный разбор NBT, размер файла и SHA-256 приведены в inventory/maps_structures.json.' if 'error' not in item else 'NBT parsing failed: '+item['error'])})
    cov=ROOT/'agent_notes/coverage/maps_catalogue.json'
    cov.write_text(json.dumps(coverage,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(f'wrote {out} ({len(rows)} records; errors={sum("error" in x for x in rows)})')
if __name__=='__main__': main()
