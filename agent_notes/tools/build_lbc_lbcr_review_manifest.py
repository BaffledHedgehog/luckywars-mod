import json, hashlib, re, pathlib, collections
base=pathlib.Path('.')
job=json.load(open(base/'agent_notes/inventory/research_jobs.json',encoding='utf-8'))['lbc_lbcr']
sources=job['sources']; cov=json.load(open(base/'agent_notes/coverage/lbc_lbcr.json',encoding='utf-8'))
allfunc=[p for p in [base/'world/datapacksdisabled/LBC/data/lbc/function',base/'world/datapacksdisabled/LBC/data/lbcr/function'] if p.exists()]
texts={}
for d in allfunc:
 for p in d.rglob('*.mcfunction'): texts[p.as_posix().replace('\\','/')]=p.read_text(encoding='utf-8',errors='replace')
records=[]; fingerprints=collections.defaultdict(list); calls=collections.defaultdict(list)
for row in cov:
 rel=row['source']; text=(base/rel).read_text(encoding='utf-8',errors='replace'); name=pathlib.Path(rel).stem
 cmds=[x.strip() for x in text.splitlines() if x.strip() and not x.lstrip().startswith('#')]
 fp='\n'.join(re.sub(r'\s+',' ',x) for x in cmds)
 fingerprints[hashlib.sha256(fp.encode()).hexdigest()].append(rel)
 out=sorted(set(re.findall(r'\bfunction\s+(?:with\s+storage\s+\S+\s+)?(?:lbcr|lbc):([\w./-]+)',text)))
 for target in out: calls[target].append(rel)
 records.append({'source':rel,'section':row['section'],'sha256':hashlib.sha256((base/rel).read_bytes()).hexdigest(),'bytes':(base/rel).stat().st_size,'commands':len(cmds),'empty':not cmds,'fingerprint':hashlib.sha256(fp.encode()).hexdigest(),'calls':out,'lines':cmds})
# caller map inventory across tracked functions
callers={}
for rec in records:
 stem=rec['source'].split('/function/',1)[1].removesuffix('.mcfunction')
 callers[stem]=[]
needle=re.compile(r'\bfunction\s+(?:with\s+storage\s+\S+\s+)?lbcr:([\w./-]+)')
for p,txt in texts.items():
 for target in needle.findall(txt):
  # map target prefix
  if target in callers: callers[target].append(p)
for rec in records: rec['callers']=sorted(set(callers.get(rec['source'].split('/function/',1)[1].removesuffix('.mcfunction'),[])))
manifest={'job':'lbc_lbcr','source_count':len(sources),'unique_paths':len(set(sources)),'coverage_rows':len(cov),'coverage_statuses':dict(collections.Counter(x['status'] for x in cov)),'distinct_command_fingerprints':len(fingerprints),'duplicate_groups':[v for v in fingerprints.values() if len(v)>1],'records':records}
out=base/'agent_notes/inventory/lbc_lbcr_review_manifest.json'
tmp=out.with_suffix('.json.tmp');tmp.write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf-8');tmp.replace(out)
print(json.dumps({'sources':len(sources),'distinct_fps':len(fingerprints),'empty':sum(r['empty'] for r in records),'duplicate_groups':sum(len(v)>1 for v in fingerprints.values()),'records_with_no_local_callers':sum(not r['callers'] for r in records),'manifest':str(out)},ensure_ascii=False))
