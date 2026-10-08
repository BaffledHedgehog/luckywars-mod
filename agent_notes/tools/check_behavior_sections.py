"""Detect coverage pointing at empty sections; this does not prove prose correctness."""
import json,re
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2];N=ROOT/'agent_notes'
docs={};empty=[];missing=[];total=0
for p in (N/'coverage').glob('*.json'):
    for row in json.loads(p.read_text(encoding='utf-8-sig')):
        total+=1
        if row['document'] not in docs:
            path=ROOT/row['document']
            if not path.is_file():docs[row['document']]={};continue
            raw=path.read_text(encoding='utf-8-sig');heads=list(re.finditer(r'^(#{1,6})\s+(.+?)\s*$',raw,re.M));sections={}
            for i,m in enumerate(heads):
                end=len(raw)
                for h in heads[i+1:]:
                    if len(h[1])<=len(m[1]):end=h.start();break
                sections[m[2]]=raw[m.end():end].strip()
            docs[row['document']]=sections
        body=docs[row['document']].get(row['section'])
        if body is None:missing.append({'source':row['source'],'document':row['document'],'section':row['section']})
        elif not body:empty.append({'source':row['source'],'document':row['document'],'section':row['section']})
result={'coverage_rows':total,'empty_linked_sections':empty,'missing_sections':missing,'scope':'Empty body check includes child sections; not a semantic completeness assertion.'}
(N/'inventory/behavior_section_audit.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps({'coverage_rows':total,'empty_linked_rows':len(empty),'missing_sections':len(missing),'empty_targets':sorted({(r['document'],r['section']) for r in empty})},ensure_ascii=False))
raise SystemExit(bool(empty or missing))
