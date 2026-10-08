"""Check all chance-model commands and both complete spell dispatchers."""
from pathlib import Path
import json
import re

ROOT = Path(__file__).resolve().parents[2]
BASE = ROOT/'world/datapacksdisabled/LBC/data/lbc/function/items'
NOTES = ROOT/'agent_notes'
MODELS = {
 'acid_chance':(150,70,.001,'charge_acid_ball'),
 'bass_chance':(150,50,.001,'lbc:workingitems/basscast'),
 'blaze_chance':(250,75,.001,'charge_small_fireball'),
 'ghast_chance':(150,50,.001,'charge_firework'),
 'penis_chance':(100,25,.001,'charge_penis'),
 'tnt_chance':(250,50,.001,'charge_tnt'),
 'bedrcock':(200,100,.001,'bedrcock_arrow'),
 'cocktail':(100,50,.001,'cocktail_1'),
 'fourier':(150,50,.001,'fourier_1'),
 'gneiss_stone':(100,20,.001,'gneiss_stone_1'),
 'hedgehog_gauntlet':(100,20,.001,'hedgehog_gauntlet_1'),
 'ice_gem':(100,10,.001,'ice_gem_1'),
 'intercal':(150,50,.001,'intercal_hit'),
 'polymorph':(2,1,.001,'polymorph_activate'),
 'psycho_soul':(40,10,.001,'psycho_soul_activate'),
 'rocket_spell':(50,10,.001,'rocket_spell_arrow'),
 'rtp':(50,25,.001,'rtp_gas'),
 'snow_pearl':(100,20,.001,'snow_pearl_1'),
 'spawn_blaze':(500,100,.001,'summon_blaze'),
 'sunprism':(5,1,.0001,'sunprism_activate'),
 'twilight_sparkle':(50,20,.001,'twilight_sparkle_activate'),
 'volos':(50,10,.001,'modify_volos'),
}
ANCHORED = {'polymorph','psycho_soul','rocket_spell','sunprism','twilight_sparkle'}

def commands(path):
    return [x.strip() for x in path.read_text(encoding='utf8').splitlines() if x.strip() and not x.lstrip().startswith('#')]

formulas = {}
for name,(base,slope,scale,payload) in MODELS.items():
    target = payload if ':' in payload else 'lbc:items/trinket_effects/'+payload
    expected = [f'scoreboard players set final lbc.math {base}',
                f'scoreboard players operation tmp lbc.math *= {slope} lbc.math',
                f'scoreboard players remove tmp lbc.math {slope}',
                'scoreboard players operation final lbc.math += tmp lbc.math',
                f'execute store result storage lbc.math tmp double {scale} run scoreboard players get final lbc.math']
    if name.endswith('_chance'):
        expected += [f'function lbc:items/trinket_effects/{name}_storage with storage lbc.math']
        macro_key = 'tmpd' if name == 'acid_chance' else 'tmp'
        anchored = ' anchored eyes positioned ^ ^ ^' if name == 'penis_chance' else ''
        macro = f'$execute if predicate {{"condition":"minecraft:random_chance","chance":$({macro_key})}}{anchored} run function {target}'
        assert commands(BASE/f'trinket_effects/{name}_storage.mcfunction') == [macro], name
    else:
        expected += [f'data modify storage lbc.math tmpf set value "{target}"']
        prefix = 'execute anchored eyes positioned ^ ^ ^ run ' if name in ANCHORED else ''
        expected += [prefix+'function lbc:items/trinket_effects/func_with_chance_universal with storage lbc.math']
    assert commands(BASE/f'trinket_effects/{name}.mcfunction') == expected, name
    formulas[name] = {'intercept_percent':round((base-slope)*scale*100,6),
                      'per_tmp_percent':round(slope*scale*100,6),'target':target,
                      'macro_key_mismatch':name=='acid_chance'}
assert commands(BASE/'trinket_effects/func_with_chance_universal.mcfunction') == ['$execute if predicate {"condition":"minecraft:random_chance","chance":$(tmp)} run function $(tmpf)']

route_pattern = re.compile(r'^execute if score spell lbc.math matches ([\d.]+)(.*?) run function (\S+)$')
routes = {}
for name in ['select','select_wand']:
    rows=[]
    remaining=[]
    for line in commands(BASE/f'libspells/{name}.mcfunction'):
        match = route_pattern.fullmatch(line)
        if match:
            rows.append({'range':match[1],'context':match[2].strip(),'target':match[3], 'command':line})
        else:
            remaining.append(line)
    if name == 'select':
        prefix = ['execute store result score spell lbc.math run data get entity @e[type=minecraft:snowball,sort=nearest,limit=1] Item.components.minecraft:custom_data{"libspell":1}.spellid',
                  'tag @s add caster','tag @s add raycaster','scoreboard players set @s Distance 0',
                  'kill @e[type=minecraft:snowball,distance=..10,sort=nearest,limit=1,nbt={Item:{components:{"minecraft:custom_data":{libspell:1}}}}]']
    else:
        prefix = ['$execute store result score spell lbc.math run data get entity @s SelectedItem.components."minecraft:custom_data".spell$(currspell) 1',
                  'tag @s add caster','tag @s add raycaster','scoreboard players set @s Distance 0']
    expected = prefix + ['execute if score spell lbc.math matches 128 store result score spell lbc.math run random value 1..242']*5
    expected += ['scoreboard players set check lbc.math 0','tag @s remove caster','tag @s remove raycaster']
    assert remaining == expected, name
    routes[name]=rows
a={(r['range'],r['context'],r['target']) for r in routes['select']}
b={(r['range'],r['context'],r['target']) for r in routes['select_wand']}
assert a <= b and b-a == {('189','anchored eyes positioned ^ ^ ^','lbc:workingitems/awaken_tuntija_wands/acidacid/click_upgraded_mode_default_lib')}
result = {'formula_helpers':formulas,'spell_routes':routes,'errors':[],
          'documented_source_defects':['acid_chance stores tmp; acid_chance_storage reads tmpd',
                                      'select has no ID189 route; select_wand has it']}
(NOTES/'inventory/arcana_routes_chances_audit.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
print(json.dumps({'formula_helpers':len(formulas),'spell_routes':{k:len(v) for k,v in routes.items()},'errors':0,'source_defects':2}))
