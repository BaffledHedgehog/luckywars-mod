"""Verify source contracts and rendered mutation parameters without rewriting docs."""
import hashlib
import json
from collections import Counter
from pathlib import Path
from document_advancements import code, conditions, predicate, TRIGGERS
from document_loot import function_description, describe_entry, provider

ROOT=Path(__file__).resolve().parents[2]
N=ROOT/'agent_notes'
jobs=json.loads((N/'inventory/research_jobs.json').read_text(encoding='utf-8'))
all_cov={}
for f in (N/'coverage').glob('*.json'):
    for row in json.loads(f.read_text(encoding='utf-8-sig')): all_cov.setdefault(row['source'],row)
cache={}
def section(source):
    row=all_cov[source]
    if row['document'] not in cache: cache[row['document']]=(ROOT/row['document']).read_text(encoding='utf-8')
    text=cache[row['document']]; marker='## '+row['section']+'\n'
    assert marker in text,source
    return text.split(marker,1)[1].split('\n## ',1)[0]
contracts={}
for name in ['lbc_advancement_contracts','lbc_loot_contracts']:
    d=json.loads((N/'inventory'/f'{name}.json').read_text(encoding='utf-8'))
    contracts.update({v['source']:v['definition'] for v in d.values()})
result={}; fingerprints=[]; counts=Counter(); amount=Counter(); empty=[]
for job in ['lbc_advancements','lbc_loot_definitions','lbc_loot_uncrafted','lbc_predicates_modifiers','lbc_registry_support']:
    valid=aux=0
    for source in jobs[job]['sources']:
        p=ROOT/source;raw=p.read_bytes();fingerprints.append({'source':source,'sha256':hashlib.sha256(raw).hexdigest()})
        body=section(source)
        if p.suffix not in ('.json','.mcmeta'): aux+=1;continue
        if not raw.strip(): empty.append(source);continue
        obj=json.loads(raw.decode('utf-8-sig'));valid+=1
        if source in contracts: assert obj==contracts[source],source
        if '/advancement/' in source:
            for key,c in obj.get('criteria',{}).items():
                assert key in body,source
                assert c['trigger'].split(':')[-1] in TRIGGERS,source
                assert conditions(c.get('conditions',{})) in body,source
                counts['criteria']+=1
            assert 'requirements' not in obj,source
            reward=obj.get('rewards',{}).get('function')
            if reward:
                assert reward in body,source
                assert (ROOT/'world/datapacksdisabled/LBC/data/lbc/function'/(reward.split(':')[1]+'.mcfunction')).is_file(),source
                counts['reward_functions']+=1
        elif job in ('lbc_loot_definitions','lbc_loot_uncrafted'):
            unknown={'components':set(),'functions':set(),'entries':set()}
            for pool in obj.get('pools',[]):
                assert provider(pool.get('rolls',1)) in body,source
                assert provider(pool.get('bonus_rolls',0)) in body,source
                if pool.get('conditions'):
                    assert predicate(pool['conditions']) in body,source
                for k,entry in enumerate(pool.get('entries',[]),1):
                    lines=[];describe_entry(entry,str(k),lines,unknown)
                    assert all(line in body for line in lines),source
                for f in pool.get('functions',[]):
                    assert function_description(f,unknown) in body,source
            for f in obj.get('functions',[]):
                assert function_description(f,unknown) in body,source
            assert not any(unknown.values()),(source,unknown)
        elif '/item_modifier/' in source:
            fs=obj if isinstance(obj,list) else [obj];previous=-1
            for f in fs:
                rendered=function_description(f,{'components':set(),'functions':set(),'entries':set()})
                pos=body.find(rendered,previous+1);assert pos>=0,(source,rendered)
                previous=pos;counts['modifier_steps']+=1
                if f.get('function','').split(':')[-1]=='set_attributes':
                    counts['set_attributes']+=1;counts['explicit_replace']+='replace' in f
                    assert f.get('replace',True) is True,source
                    for m in f['modifiers']:
                        assert m['operation']=='add_value',source
                        a=m['amount'];amount[a.get('type','').split(':')[-1] if isinstance(a,dict) else 'literal']+=1
        elif '/predicate/' in source:
            kind=obj.get('condition','').split(':')[-1] if isinstance(obj,dict) else None
            if kind=='location_check':
                assert code({k:v for k,v in obj.items() if k!='condition'}) in body,source
            elif kind=='entity_scores':
                assert code(obj.get('entity','this')) in body and code(obj.get('scores',{})) in body,source
            else:
                assert predicate(obj) in body,source
        elif '/tags/' in source:
            assert code(obj.get('values',[])) in body,source
            counts['tags']+=1
        elif job=='lbc_registry_support':
            if '/dimension/' in source:
                assert code(obj['generator']) in body and code(obj['type']) in body,source
            elif '/loot_table/' in source:
                unknown={'components':set(),'functions':set(),'entries':set()}
                for i,pool in enumerate(obj.get('pools',[]),1):
                    assert provider(pool.get('rolls',1)) in body,source
                    for k,entry in enumerate(pool.get('entries',[]),1):
                        lines=[];describe_entry(entry,str(k),lines,unknown)
                        assert all(line in body for line in lines),source
                    for f in pool.get('functions',[]):
                        assert function_description(f,unknown) in body,source
                for f in obj.get('functions',[]):
                    assert function_description(f,unknown) in body,source
                assert not any(unknown.values()),(source,unknown)
            else:
                assert code(obj) in body,source
    result[job]={'sources':len(jobs[job]['sources']),'parsed_definitions':valid,'auxiliary':aux}
assert amount=={'constant':37,'score':14,'storage':1,'literal':6},amount
assert counts['set_attributes']==36 and counts['explicit_replace']==2,counts
payload={'jobs':result,'counts':dict(counts),'attribute_amount_providers':dict(amount),'empty_json':empty,'fingerprints':fingerprints,'errors':[]}
(N/'inventory/typed_json_root_audit.json').write_text(json.dumps(payload,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps({k:v for k,v in payload.items() if k!='fingerprints'},ensure_ascii=False))
