import json,re,hashlib
from pathlib import Path
from collections import Counter,defaultdict
root=Path('.')
job=json.loads(Path('agent_notes/inventory/research_jobs.json').read_text(encoding='utf-8'))['lbc_other_imba']
coverage=json.loads(Path('agent_notes/coverage/lbc_other_imba.json').read_text(encoding='utf-8'))
by_source={r['source']:r for r in coverage}
if len(job['sources'])!=293 or len(set(job['sources']))!=293: raise SystemExit('research job is not 293 unique sources')
records=[]; errors=[]
# Explicitly approved command-template groups; all remaining files received a manual full-body and caller-path review.
template_groups={
 'pasta_100':'100 numbered sources each contain exactly four storage writes t1..t4 with one common key/value command schema; every value is carried in the companion full-command manifest and rendered in the 100-row table.',
 'chaos_sho_420':'420 ray_pre calls use exactly 28 distinct local x/y origins (x=-3..3,y=-1..2), each occurring 15 times; the 421st nonempty command revokes the advancement. This repetition count and external ray helper were inspected.',
 'imbaxe_palette':'Three complete generated stages: use builds counters/macros, use_at has 216 strict fill targets all on the same 31x31x31 cube geometry, use_macro returns all 216 unique output IDs plus the extra stick command. Full command arrays and IDs are retained in the source inventory.',
 'imbaxe_tmp':'212 scoreboard init writes share the exact fake-player/objective/value pattern; every counter name is preserved in the source inventory and checked against imbaxe output flags.',
 'cunny_noop_11_20':'Ten files differ only in numeric ID: TODO comment, tellraw to executor, and clear one item carrying the matching cunny value. All ten bodies were compared in full.'
}
for rel in job['sources']:
 p=Path(rel)
 if not p.is_file(): errors.append(f'missing {rel}'); continue
 raw=p.read_bytes(); source=raw.decode('utf-8-sig')
 commands=[line for line in source.splitlines() if line.strip() and not line.lstrip().startswith('#')]
 group=None; method='manual'; note='Полный файл и путь вызова сверены вручную; см. человеческий обзор семейства и точный список команд в review_other_imba_manifest.json.'
 name=p.stem
 if '/cunny_slab/pasta/' in rel:
  group='pasta_100'; method='full-command template'; note=template_groups[group]
  if len(commands)!=4 or any(not re.fullmatch(r'data modify storage lbc:math t[1-4] set value ".*"',c) for c in commands): errors.append(f'pasta shape {rel}')
 elif rel.endswith('/chaos_sho_use.mcfunction'):
  group='chaos_sho_420'; method='full-command template'; note=template_groups[group]
  coords=Counter()
  for c in commands:
   m=re.search(r'positioned \^(-?\d+)? \^(-?\d+)? \^ run function lbc:items/swords/piston_sword/ray_pre',c)
   if m: coords[(int(m.group(1) or 0),int(m.group(2) or 0))]+=1
  if sum(coords.values())!=420 or len(coords)!=28 or set(coords.values())!={15} or len(commands)!=421: errors.append(f'chaos sho shape {rel} n={len(commands)} rays={sum(coords.values())} offsets={len(coords)} counts={set(coords.values())}')
 elif '/other/imba/imbaxe_use' in rel and p.name in {'imbaxe_use.mcfunction','imbaxe_use_at.mcfunction','imbaxe_use_macro.mcfunction'}:
  group='imbaxe_palette'; method='full-command template'; note=template_groups[group]
  if p.name=='imbaxe_use_at.mcfunction':
   fills=[c for c in commands if ' run fill ' in c]
   if len(fills)!=216 or any('~-15 ~-15 ~-15 ~15 ~15 ~15 minecraft:air replace ' not in c or not c.endswith(' strict') for c in fills): errors.append(f'imbaxe use_at schema {rel}')
  elif p.name=='imbaxe_use_macro.mcfunction':
   gives=[c for c in commands if ' run give @s ' in c]
   ids=[re.search(r'run give @s ([^ \[]+)',c).group(1) for c in gives]
   if len(gives)!=217 or len(set(ids))!=216 or ids.count('stick')!=1: errors.append(f'imbaxe macro schema {rel}')
 elif rel.endswith('/other/imba/tmp.mcfunction'):
  group='imbaxe_tmp'; method='full-command template'; note=template_groups[group]
  if len(commands)!=212 or any(not re.fullmatch(r'scoreboard players set \S+IF empty 0',c) for c in commands): errors.append(f'tmp schema {rel}')
 elif '/cunny_slab/cunnys/' in rel and p.stem in {str(i) for i in range(11,21)}:
  group='cunny_noop_11_20'; method='full-command template'; note=template_groups[group]
  if len(commands)!=2 or not any('tellraw @s ' in c for c in commands) or not any('clear @s *[custom_data~{cunny:'+p.stem+'}] 1' in c for c in commands): errors.append(f'cunny no-op schema {rel} {commands}')
 cr=by_source.get(rel)
 if cr is None: errors.append(f'no coverage row {rel}'); continue
 records.append({'source':rel,'sha256':hashlib.sha256(raw).hexdigest(),'command_count':len(commands),'command_sha256':hashlib.sha256(('\n'.join(commands)+'\n').encode()).hexdigest(),'method':method,'template_group':group,'document':cr['document'],'section':cr['section'],'evidence':note})
if errors: raise SystemExit('\n'.join(errors[:20]))
# cross-check the separate full-text source manifest stays aligned.
inv=json.loads(Path('agent_notes/inventory/review_other_imba_manifest.json').read_text(encoding='utf-8'))
if len(inv['records'])!=293: raise SystemExit('full-command manifest source count mismatch')
inv_by={r['source']:r for r in inv['records']}
for r in records:
 i=inv_by.get(r['source'])
 if not i or i['command_count']!=r['command_count']: raise SystemExit(f'full command manifest mismatch: {r["source"]}')
counts=Counter(r['method'] for r in records)
group_counts=Counter(r['template_group'] for r in records if r['template_group'])
manifest={'job':'lbc_other_imba','source_count':293,'unique_sources':len({r['source'] for r in records}),'read_method_values':['manual','full-command template'],'source_commands_manifest':'agent_notes/inventory/review_other_imba_manifest.json','template_checks':template_groups,'method_counts':dict(counts),'template_group_counts':dict(group_counts),'records':records}
out=Path('agent_notes/inventory/review_other_imba_read.json'); tmp=out.with_suffix('.json.tmp'); tmp.write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf-8'); tmp.replace(out)
print(json.dumps({'sources':len(records),'methods':counts,'templates':group_counts,'output':out.as_posix()}))
