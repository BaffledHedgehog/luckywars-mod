"""Import every remaining library entry and the complete library with exact source components."""
from copy import deepcopy
from pathlib import Path
import json
import re
import import_lbc_firearms as firearms
from import_lbc_elemental_grimoires import SPELLS, RP
from import_lbc_loadouts import ROOT
from import_lbc_shooter_pair import translate_keys
from import_lbc_wands import build_single_entry_table, write_table
from import_lbc_woodificator import read_json

NAMES = {
    1:'evoker',2:'evoker_spiral',3:'evoker_ring',4:'evoker_fan',
    5:'bass',6:'bass_weakness',7:'bass_slowness',8:'bass_blindness',9:'bass_fatigue',
    10:'corruption',11:'corruption_poison',12:'corruption_wither',13:'corruption_hunger',14:'corruption_damage',
    15:'ender',16:'ender_safe',17:'lifestealer',18:'lifestealer_strong',19:'lifestealer_charge',
    20:'confuse',21:'confuse_invisibility',22:'confuse_teleport',23:'confuse_speed',24:'confuse_levitation',
    25:'blood',26:'blood_safe',27:'badwiki',
    112:'wall',113:'wall_horizontal',114:'wall_stair',115:'wall_break',
    116:'advanced_ender',117:'advanced_ender_safe',118:'advanced_ender_top',119:'advanced_ender_clip',
    120:'falling_stars',121:'falling_stars_rain',122:'falling_stars_big',
    123:'prism',124:'prism_awakened',125:'blaze',126:'blaze_head',127:'random',128:'random_all',
    129:'hedgehog',130:'hedgehog_sniper',131:'hedgehog_laser',132:'hedgehog_tank',133:'hedgehog_healer',
    134:'sheep',135:'sheep_awakened',136:'psycho',137:'psycho_sound',138:'psycho_screamer',139:'psycho_look_back',140:'psycho_scare',
    141:'tesla',142:'tesla_chain',143:'tesla_instant',144:'caustic_acid',145:'caustic_acid_burning',
    146:'omega',147:'omega_safe',148:'neutral',149:'neutral_skulls',150:'neutral_fireballs',151:'neutral_big_fireball',152:'neutral_fireworks',153:'neutral_potion',
    154:'lightning',155:'lightning_chain',156:'lightning_reforge',157:'exoperso',158:'exoperso_awakened',
    195:'acid_corruption',196:'acid_corruption_reverse',197:'acid_corruption_death',198:'acid_corruption_mana',
    237:'bedrock',238:'bedrock_cube',239:'bedrock_clear_cubes',240:'bedrock_regen',241:'bedrock_remove',
}
NAMES.update({i:f'badwiki_{i-27:02d}' for i in range(28,112)})

def main():
    firearms.RP=RP
    source=firearms.migrated('spells_library')
    broad=deepcopy(source)
    descriptors=[]
    ids=set()
    for pool,broad_pool in zip(source['pools'],broad['pools']):
        for original,entry in zip(pool['entries'],broad_pool['entries']):
            modifiers=entry.get('modifier',[])
            data=next(fn['tag'] for fn in modifiers if fn['type']=='minecraft:set_custom_data')
            spell_id=int(re.search(r'\bspellid\s*:\s*(\d+)',data)[1])
            assert spell_id not in ids,spell_id
            ids.add(spell_id)
            name=SPELLS.get(spell_id,NAMES.get(spell_id,'danmaku' if spell_id==159 else None))
            assert name is not None,spell_id
            entry['name']=f'skywars2:{name}_grimoire'
            if spell_id not in NAMES:continue
            result=build_single_entry_table(source,pool,entry)
            restored=deepcopy(result);restored['pools'][0]['entries'][0]['name']=original['name']
            assert restored==build_single_entry_table(source,pool,original)
            for language in ('ru_ru','en_us','rpr'):
                translations=read_json(RP/f'assets/minecraft/lang/{language}.json')
                # The source deliberately uses untranslated question marks for unknown properties.
                assert not set(translate_keys([result]))-translations.keys()-{'?','? '},(spell_id,language)
            components=next(fn['components'] for fn in modifiers if fn['type']=='minecraft:set_components')
            firearms.verify_model(components['minecraft:item_model'],item_definition=True)
            title=next(fn['name'] for fn in modifiers if fn['type']=='minecraft:set_name')
            lore=next(fn['lore'] for fn in modifiers if fn['type']=='minecraft:set_lore')
            descriptors.append({'spellId':spell_id,'id':name+'_grimoire','model':components['minecraft:item_model'],
                                'tooltip':components['minecraft:tooltip_style'],'count':components['minecraft:max_stack_size'],
                                'name':title,'lore':lore,'data':data})
            write_table(ROOT/f'src/main/resources/data/lbc/loot_table/ported/{name}_grimoire.json',result)
    assert ids==set(range(1,242))-{189} and len(descriptors)==167
    raw=ROOT.parent.parent/'cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/badwiki_lib.mcfunction'
    effects={}
    for match in re.finditer(r'matches (\d+) run effect give @s minecraft:(\w+) (\d+) (\d+)( true)?',raw.read_text(encoding='utf-8')):
        i,effect,seconds,amplifier,hidden=match.groups()
        effects[i]={'id':effect,'ticks':int(seconds) if effect in ('instant_health','instant_damage','saturation') else int(seconds)*20,
                    'amplifier':int(amplifier),'hidden':bool(hidden)}
    assert set(map(int,effects))==set(range(28,112))
    path=ROOT/'src/main/resources/skywars2/remaining_grimoires.json'
    path.write_text(json.dumps({'items':descriptors,'wiki':effects},ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    write_table(ROOT/'src/main/resources/data/lbc/loot_table/spells_library.json',broad)
    import generate_extra_grimoires
    generate_extra_grimoires.generate()
    print(f'Imported {len(descriptors)} remaining books, {len(effects)} fixed effects, complete {len(ids)}-entry library; RP read-only.')

if __name__=='__main__':main()
