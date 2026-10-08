"""Read-only datapack audit: every command in repeated placement/saver families."""
import collections
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
NOTES = ROOT / 'agent_notes'
job = json.loads((NOTES / 'inventory/research_jobs.json').read_text(encoding='utf-8'))['skywars_maps_admin']
errors = []

def active(path):
    return [x.strip() for x in (ROOT/path).read_text(encoding='utf-8-sig').splitlines() if x.strip() and not x.lstrip().startswith('#')]

paths = {Path(p).stem:p for p in job['sources'] if '/maploader/map/' in p and p.endswith('.mcfunction')}
numeric = {k:v for k,v in paths.items() if re.fullmatch(r'\d+b?(?:_broken)?', k)}
known = {Path(r['source']).stem for r in json.loads((NOTES/'inventory/maps_structures.json').read_text(encoding='utf-8'))['structures'] if r['group']=='maps'}
doc = (NOTES/'skywars_maps_admin.md').read_text(encoding='utf-8')
place_rx = re.compile(r'place template swrg:(\S+) (-?\d+) (-?\d+) (-?\d+) none none (1|0\.75) (0|1)( strict)?')
placements = {}
for stem, path in {**numeric, 'temp':paths['temp'], 'temp_full':paths['temp_full']}.items():
    commands = active(path)
    rows, scores = [], []
    for command in commands:
        m = place_rx.fullmatch(command)
        if m:
            ident,x,y,z,integrity,seed,strict = m.groups()
            rows.append({'id':ident,'x':int(x),'y':int(y),'z':int(z),'integrity':integrity,'seed':seed,'strict':bool(strict)})
        elif re.fullmatch(r'scoreboard players set #map swrg.math -?\d+', command):
            scores.append(command.rsplit(' ',1)[1])
        else: errors.append({'source':path,'unknown_command':command})
    if stem in numeric and rows:
        expected_score = str(int(stem.split('_')[0].removesuffix('b')) - 1 + (1000 if 'b' in stem.split('_')[0] else 0))
        if scores != [expected_score]: errors.append({'source':path,'scores':scores,'expected':expected_score})
    if rows:
        ids = {r['id'] for r in rows}; xs = [r['x'] for r in rows]; zs = [r['z'] for r in rows]
        cells=[stem,scores[0] if scores else '—',str(len(rows)),f'{len(ids & known)}/{len(ids)}',','.join(map(str,sorted({r['y'] for r in rows}))),','.join(sorted({r['integrity'] for r in rows})),','.join(sorted({r['seed'] for r in rows})),f'{min(xs)}..{max(xs)} / {min(zs)}..{max(zs)}']
    else: cells=[stem,scores[0] if scores else '—','0','—','—','—','—','—']
    expected_row='| `'+cells[0]+'` | '+' | '.join(cells[1:])+' |'
    if expected_row not in doc: errors.append({'source':path,'missing_or_wrong_doc_row':expected_row})
    placements[path]={'commands':commands,'placements':rows,'scores':scores}

pairs=[]
for stem,path in numeric.items():
    if not stem.endswith('_broken'): continue
    base=stem.removesuffix('_broken')
    normal=active(numeric[base]); broken=active(path)
    normalized=[re.sub(r' none none 0\.75 ', ' none none 1 ', c) for c in broken]
    if base=='5b': normalized=[re.sub(r' none none 1 1 strict$', ' none none 1 0 strict',c) for c in normalized]
    if normal!=normalized: errors.append({'source':path,'pair_mismatch':base})
    pairs.append({'base':base,'commands':len(normal),'empty':not normal,'seed_exception':base=='5b'})

saver_path=next(p for p in job['sources'] if p.endswith('/developer/saver.mcfunction'))
sets=[]; markers=[]; other=[]
set_rx=re.compile(r'setblock (~-?\d*) (~-?\d*) (~-?\d*) minecraft:structure_block\[mode=save\]\{ignoreEntities:0b,author:"_Swarog",posX:0,mode:"SAVE",posY:1,sizeX:48,posZ:0,integrity:1\.0f,showair:0b,name:"minecraft:62-(\d+)",sizeY:48,sizeZ:48,showboundingbox:1b\}')
marker_rx=re.compile(r'summon (?:minecraft:)?marker (~-?\d*) (~-?\d*) (~-?\d*) \{Tags:\["(saver_marker|map_saver)"\]\}')
coord=lambda v:int(v[1:] or 0)
for command in active(saver_path):
    m=set_rx.fullmatch(command)
    if m: sets.append({'pos':list(map(coord,m.groups()[:3])),'name':f'minecraft:62-{m[4]}'});continue
    m=marker_rx.fullmatch(command)
    if m: markers.append({'pos':list(map(coord,m.groups()[:3])),'tag':m[4]});continue
    other.append(command)
grid={(x,1,z) for x in range(-240,193,48) for z in range(-240,193,48)}
if {tuple(r['pos']) for r in sets}!=grid or len(sets)!=100: errors.append('saver setblock grid')
if {r['name'] for r in sets}!={f'minecraft:62-{i}' for i in range(1,101)}: errors.append('saver names')
if {tuple(r['pos']) for r in markers}!=grid or len(markers)!=100: errors.append('saver marker grid')
for row in markers:
    x,_,z=row['pos']; expected='map_saver' if x in (-240,192) or z in (-240,192) else 'saver_marker'
    if row['tag']!=expected: errors.append({'saver_marker':row,'expected_tag':expected})
if other!=['execute as @e[type=marker,tag=map_saver] at @s run function swrg:developer/saver_check']: errors.append({'saver_other':other})

selectors={}
for path in job['sources']:
    if '/maploader/map_select_' not in path: continue
    mapping={}; cleanup=[]
    for command in active(path):
        m=re.fullmatch(r'execute if score saved_map swrg.math matches (\d+) in minecraft:(white|imprinted) run function swrg:maploader/map/(\d+b?(?:_broken)?)',command)
        if m: mapping[int(m[1])]=m[3]
        else: cleanup.append(command)
    stem=Path(path).stem; offset=50 if stem.endswith('2') else 25 if stem.endswith('1') else 0
    suffix='b' if stem.endswith('b') else ''; broken='_broken' if 'imprinted' in stem else ''
    expected={i:str(offset+i+1 if i<18 else offset+i)+suffix+broken for i in list(range(18))+list(range(19,26))}
    if mapping!=expected: errors.append({'selector':path,'wrong_mapping':mapping})
    dim='imprinted' if broken else 'white'
    expected_cleanup=[] if broken else ['execute in minecraft:white as @e[type=minecraft:marker,tag=swrg.spawn,distance=0..] at @s run fill ~-1 ~-1 ~-1 ~1 ~4 ~1 minecraft:air']
    expected_cleanup.append(f'execute in minecraft:{dim} run kill @e[distance=0..]')
    if cleanup!=expected_cleanup: errors.append({'selector':path,'cleanup':cleanup})
    selectors[path]={'mapping':mapping,'cleanup':cleanup}

report={'numeric_functions':len(numeric),'numeric_place_commands':sum(len(placements[p]['placements']) for p in numeric.values()),'pairs':pairs,'placements':placements,'saver':{'source':saver_path,'blocks':sets,'markers':markers,'other':other},'selectors':selectors,'errors':errors}
(NOTES/'inventory/maps_admin_variants_audit.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps({'numeric_functions':len(numeric),'place_commands':report['numeric_place_commands'],'pairs':len(pairs),'empty_pairs':sum(p['empty'] for p in pairs),'saver_blocks':len(sets),'saver_marker_tags':dict(collections.Counter(r['tag'] for r in markers)),'selectors':len(selectors),'errors':errors},ensure_ascii=False))
raise SystemExit(bool(errors))
