import json, pathlib, re, hashlib, collections
base=pathlib.Path('.')
job=json.load(open(base/'agent_notes/inventory/research_jobs.json',encoding='utf-8'))['lbc_other_imba']
rows=json.load(open(base/'agent_notes/coverage/lbc_other_imba.json',encoding='utf-8'))
allfunc=base/'world/datapacksdisabled/LBC/data/lbc/function'
texts={p.as_posix().replace('\\','/'):p.read_text(encoding='utf-8',errors='replace') for p in allfunc.rglob('*.mcfunction')}
callmap=collections.defaultdict(list)
for path,text in texts.items():
 for t in re.findall(r'\bfunction\s+lbc:([\w./-]+)',text): callmap[t].append(path)
records=[]; fps=collections.defaultdict(list); names=set()
for r in rows:
 rel=r['source'];p=base/rel;text=p.read_text(encoding='utf-8',errors='replace')
 logical=rel.split('/data/lbc/function/',1)[1].removesuffix('.mcfunction')
 commands=[line.strip() for line in text.splitlines() if line.strip() and not line.lstrip().startswith('#')]
 # keep every output command without filtering/truncation
 output=[line for line in commands if re.search(r'\b(?:loot|give|effect|damage|summon|particle|playsound|fill|setblock|place\s+template|item\s+(?:replace|modify)|scoreboard\s+players\s+(?:add|remove|set|operation))\b',line)]
 fp='\n'.join(re.sub(r'\s+',' ',x) for x in commands)
 rec={'source':rel,'section':r['section'],'status':r['status'],'sha256':hashlib.sha256(p.read_bytes()).hexdigest(),'bytes':p.stat().st_size,'command_count':len(commands),'command_fingerprint':hashlib.sha256(fp.encode()).hexdigest(),'commands':commands,'outputs':output,'literal_callers':sorted(set(callmap.get(logical,[])))}
 records.append(rec);fps[rec['command_fingerprint']].append(rel);names.add(logical)
manifest={'job':'lbc_other_imba','source_count':len(job['sources']),'unique_sources':len(set(job['sources'])),'coverage_rows':len(rows),'distinct_normalized_command_fingerprints':len(fps),'duplicate_groups':[v for v in fps.values() if len(v)>1],'records':records}
out=base/'agent_notes/inventory/review_other_imba_manifest.json';tmp=out.with_suffix('.json.tmp');tmp.write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf-8');tmp.replace(out)
print(json.dumps({'sources':len(records),'unique':len(set(job['sources'])),'fingerprints':len(fps),'dupegroups':sum(len(x)>1 for x in fps.values()),'no_callers':sum(not x['literal_callers'] for x in records),'empty':sum(not x['commands'] for x in records),'manifest':str(out)}))
