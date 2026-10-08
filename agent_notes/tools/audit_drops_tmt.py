"""Verify all TMT array append commands and generation attempt counts."""
from pathlib import Path
from collections import Counter
import hashlib,json,re
r=Path(__file__).resolve().parents[2]
d=r/'world/datapacksdisabled/LBC/data/lbc/function/drops/tmt'
rows=[];errors=[]
for suffix,key,count in [('nbttags','tags',1049),('names','names',11682),
                         ('hex','hex',16),('sound','sound',1735),
                         ('attribute','attribute',32),('enchantment','enchantment',42)]:
    p=d/('set_storage_'+suffix+'.mcfunction');raw=p.read_bytes()
    values=[];prefix=f'data modify storage lbc:tags {key} append value '
    for line in raw.decode('utf-8-sig').splitlines():
        line=line.strip()
        if not line or line.startswith('#'):continue
        if not line.startswith(prefix):errors.append(str(p)+': unexpected command');continue
        values.append(json.loads(line[len(prefix):]))
    distinct=Counter(json.dumps(v,ensure_ascii=False) for v in values)
    if len(values)!=count:errors.append(suffix+': count')
    rows.append(dict(source=p.relative_to(r).as_posix(),sha256=hashlib.sha256(raw).hexdigest(),
                     count=len(values),unique=len(distinct),
                     repeated=[{'value':json.loads(v),'occurrences':n} for v,n in distinct.items() if n>1],
                     first=values[0],last=values[-1]))
s=(d/'select_random.mcfunction').read_text(encoding='utf-8-sig')
attempts=Counter(re.findall(r'chance":([0-9.]+).*function lbc:drops/tmt/(select_random_\w+)',s))
expected={('1.0','select_random_attribute'):1,('0.2','select_random_attribute'):4,
          ('0.1','select_random_attribute'):8,('0.05','select_random_attribute'):16,
          ('0.2','select_random_enchantment'):4,('0.1','select_random_enchantment'):17}
if dict(attempts)!=expected:errors.append('attempt distribution')
report=dict(arrays=rows,attempts=[dict(chance=float(k[0]),function=k[1],count=n) for k,n in attempts.items()],errors=errors)
(r/'agent_notes/inventory/drops_tmt_audit.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps(dict(arrays=[(x['count'],x['unique']) for x in rows],attempts=report['attempts'],errors=errors)))
raise SystemExit(bool(errors))
