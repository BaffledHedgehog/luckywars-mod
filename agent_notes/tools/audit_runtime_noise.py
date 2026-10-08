"""Check complete command sequences for the documented Motion/Pos families."""
from pathlib import Path
import hashlib
import json

root = Path(__file__).resolve().parents[2]
source = root / 'world/datapacksdisabled/LBC/data/lbc/function'
rows, errors = [], []

def check(name, expected, params):
    p = source / (name + '.mcfunction')
    raw = p.read_bytes()
    actual = [s.strip() for s in raw.decode('utf-8-sig').splitlines()
              if s.strip() and not s.lstrip().startswith('#')]
    if actual != expected:
        errors.append(name)
    rows.append({'source': p.relative_to(root).as_posix(),
                 'sha256': hashlib.sha256(raw).hexdigest(),
                 'complete_command_match': actual == expected,
                 'parameters': params})

def family(name, field, axes, ranges, read_scale, write_scale,
           additive=True, prefix=None, extras=()):
    prefix = prefix or ('temp_motion' if field == 'Motion' else 'temp_pos')
    lines = []
    if additive:
        for a in axes:
            lines.append(f'execute store result score {prefix}_{a} lbc.math run data get entity @s {field}[{a}] {read_scale}')
    for a, limits in zip(axes, ranges):
        holder = 'rand' if additive else f'{prefix}_{a}'
        lines.append(f'execute store result score {holder} lbc.math run random value {limits}')
        if additive:
            lines.append(f'scoreboard players operation {prefix}_{a} lbc.math += rand lbc.math')
    for a in axes:
        lines.append(f'execute store result entity @s {field}[{a}] double {write_scale} run scoreboard players get {prefix}_{a} lbc.math')
    lines.extend(extras)
    check(name, lines, dict(field=field, axes=axes, ranges=ranges,
                          read_scale=read_scale, write_scale=write_scale,
                          additive=additive, extras=extras))

for n, limits, scale in [
    ('rand_motion', ['-6000..6000','0..10000','-6000..6000'],'0.0001'),
    ('rand_motion_small',['-50..50']*3,'0.01'),
    ('rand_motion_smallest',['-2500..2500','-500..500','-2500..2500'],'0.001'),
    ('rand_motion_very_small',['-10..10']*3,'0.01'),
    ('rand_motion_very_very_small',['-100..100']*3,'0.0004')]:
    family(n,'Motion',[0,1,2],limits,None,scale,False)
for suffix, limits, read, write in [
    ('',['-10..10']*3,'10','0.1'),
    ('_small',['-4..4']*3,'10','0.1'),
    ('_big',['-1500..1500']*3,'1000','0.001'),
    ('_smallest',['-100..100']*3,'10000','0.0001')]:
    family('rand_motion_razbros'+suffix,'Motion',[0,1,2],limits,read,write)
family('rand_power_razbros_small','Motion',[0,1,2],
       ['-30..30','-5..5','-30..30'],'1000','0.001',prefix='temp_power')

specs = {
    '0_5':([0,1,2],50,'100','0.01'),
    '1':([0,1,2],1,'1','1'), '10':([0,2],10,'1','1'),
    '15':([0,1,2],15,'1','1'), '15_active':([0,2],15,'1','1'),
    '15_xz':([0,2],15,'1','1'), '2':([0,2],2,'1','1'),
    '20':([0,2],20,'1','1'), '20_active':([0,2],20,'1','1'),
    '2_':([0,1,2],200,'100','0.01'),
    '2_motion':([0,1,2],200,'100','0.01'),
    '30':([0,2],30,'1','1'), '35_active':([0,1,2],35,'1','1'),
    '5_y':([0,2],5,'1','1'), '7':([0,1,2],7,'1','1'),
    '7_y':([0,2],7,'1','1')}
for suffix, (axes, radius, read, write) in specs.items():
    ranges = [f'-{radius}..{radius}' for _ in axes]
    extras = []
    if suffix in ('2_','2_motion'):
        ranges[1] = '0..200'
        extras.append('tag @s remove doubledcurr')
    if suffix == '2_motion':
        extras.insert(0, 'function lbc:rand_motion_razbros_big')
    if suffix.endswith('_active'):
        extras.append('tag @s add active')
    family('universal_pos_razb_'+suffix,'Pos',axes,ranges,read,write,extras=extras)

report = {'files':len(rows),'errors':errors,'rows':rows}
(root/'agent_notes/inventory/runtime_noise_audit.json').write_text(
    json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps({'files':len(rows),'errors':errors}))
raise SystemExit(bool(errors))
