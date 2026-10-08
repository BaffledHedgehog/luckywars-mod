import hashlib,json,re
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2];N=ROOT/'agent_notes'
j=json.loads((N/'inventory/research_jobs.json').read_text(encoding='utf-8'))
doc=(N/'lbc_other_attributes.md').read_text(encoding='utf-8')
rows={}
for line in doc.splitlines():
    m=re.match(r'\| `([^`]+)` \|',line)
    if m:rows.setdefault(m[1],[]).append(line)
records=[];effects=functions=0;specials=[]
for source in j['lbc_other_attributes']['sources']:
    p=ROOT/source;raw=p.read_bytes();cmds=[x.strip() for x in raw.decode('utf-8-sig').splitlines() if x.strip() and not x.lstrip().startswith('#')]
    record={'source':source,'sha256':hashlib.sha256(raw).hexdigest(),'commands':cmds}
    if '/attributes/pills/' in source and p.suffix=='.mcfunction':
        assert p.stem in rows,source
        row='\n'.join(rows[p.stem])
        for c in cmds:
            m=re.fullmatch(r'effect give @s (\S+) (\S+) (\S+)(?: \S+)?',c)
            if m:
                assert f'effect {m[1]} {m[2]} amp={m[3]}' in row,(source,row,c);effects+=1
            m=re.fullmatch(r'function lbc:other/attributes/(\S+)',c)
            if m:
                assert f'`{m[1]}`' in row,(source,row,c);functions+=1
        special=[c for c in cmds if c.startswith(('scoreboard ','tag '))]
        if special:specials.append({'source':source,'actions':special,'row':row})
        record['table_row']=row
    records.append(record)
result={'sources':331,'pill_effect_commands_equal':effects,'pill_attribute_calls_equal':functions,'special_pill_rows':specials,'source_records':records,'errors':[]}
(N/'inventory/attribute_tables_root_audit.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps({k:v for k,v in result.items() if k not in ('source_records','special_pill_rows')},ensure_ascii=False))
for r in specials:print(r['source'].split('/')[-1],r['actions'],r['row'].split('|')[-2].strip())
