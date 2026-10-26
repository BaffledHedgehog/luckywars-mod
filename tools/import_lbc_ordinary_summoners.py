"""Import all seven remaining source summoners, their native formations and books."""
from copy import deepcopy
import json
import re
from import_lbc_loadouts import ROOT, SOURCE
from import_game_resources import migrate_loot_schema

KINDS = [
    ('skeletons','summoner_skeletons','summonerskeletons','skeleton_soul',3000),
    ('razoritel','summoner_ravager','summonerrazoritel','ravager_soul',5000),
    ('eliteskeletons','summoner_elite_skeletons','summonereliteskeletons','elite_skeleton_soul',4000),
    ('ghostrider','summoner_ghost_rider','summonerghostrider','ghost_rider_soul',4000),
    ('ghast','summoner_ghasts','summonerghast','ghast_soul',3000),
    ('blaze','summoner_blazes','summonerblaze','blaze_soul',3000),
    ('guardians','summoner_guardians','summonerguardians','guardian_soul',3000),
]


def write(path, value):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_bytes((json.dumps(value,ensure_ascii=False,indent=2)+'\n').encode('utf-8'))


def generate():
    source=json.loads((SOURCE/'loot_table/transformables_summons.json').read_text(encoding='utf-8-sig'))
    migrate_loot_schema(source)
    entries=source['pools'][0]['entries'];items=[];presets={};forms={}
    for kind,native,flag,title,price in KINDS:
        matches=[e for e in entries if any(fn.get('type')=='minecraft:set_custom_data' and flag+':1' in fn['tag'] for fn in e['modifier'])]
        assert len(matches)==1,(kind,matches)
        entry=deepcopy(matches[0]);entry['name']='skywars2:'+native
        leaf=deepcopy(entry);leaf.pop('weight',None);leaf.pop('quality',None)
        write(ROOT/f'src/main/resources/data/lbc/loot_table/ported/{native}.json',{'pools':[{'rolls':1,'entries':[leaf]}]})
        cs={}
        for fn in entry['modifier']:
            if fn['type']=='minecraft:set_components':cs.update(fn['components'])
        name=next(fn['name'] for fn in entry['modifier'] if fn['type']=='minecraft:set_name')
        lore=next(fn['lore'] for fn in entry['modifier'] if fn['type']=='minecraft:set_lore')
        data=next(fn['tag'] for fn in entry['modifier'] if fn['type']=='minecraft:set_custom_data')
        items.append({'kind':kind,'id':native,'flag':flag,'price':price,'cost':title+'_mana_use','name':name,'lore':lore,'data':data,'model':cs['minecraft:item_model'],'tooltip':cs['minecraft:tooltip_style']})
        commands=(SOURCE/f'function/workingitems/summoner/{kind}.mcfunction').read_text(encoding='utf-8-sig').splitlines()
        roots=[]
        for line in commands:
            m=re.fullmatch(r'(execute rotated ~ 0 run )?summon (minecraft:\w+) (\S+) (\S+) (\S+) (\{.*\})',line)
            if not m:continue
            local=bool(m[1]);coordinates=[float(v[1:] or 0) for v in (m[3],m[4],m[5])]
            assert all(v.startswith('^' if local else '~') for v in (m[3],m[4],m[5]))
            preset='{id:"'+m[2]+'",'+m[6][1:]
            key='ordinary_'+kind
            assert key not in presets or presets[key]==preset,kind
            presets[key]=preset;roots.append({'local':local,'offset':coordinates})
        assert len(roots)=={'skeletons':20,'razoritel':1,'eliteskeletons':2,'ghostrider':1,'ghast':9,'blaze':1,'guardians':4}[kind]
        forms[kind]=roots
    # Existing Vex is the eighth ordinary family; preserve every source weight/component.
    mapping={flag:'skywars2:'+native for _,native,flag,_,_ in KINDS}|{'summonervexes':'skywars2:summoner_vexes_wand'}
    for entry in entries:
        tag=next(fn['tag'] for fn in entry['modifier'] if fn['type']=='minecraft:set_custom_data')
        entry['name']=next(value for flag,value in mapping.items() if flag+':1' in tag)
    write(ROOT/'src/main/resources/data/lbc/loot_table/transformables_summons.json',source)
    write(ROOT/'src/main/resources/skywars2/ordinary_summoners.json',{'originalDataVersion':4440,'items':items,'presets':presets,'formations':forms})
    print('Imported seven source summoners, native passenger presets/formations and complete eight-entry ordinary pool')


if __name__=='__main__':generate()
