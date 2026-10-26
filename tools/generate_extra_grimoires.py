"""Add missing wand books without editing the author's resource pack."""
from copy import deepcopy
from pathlib import Path
import json
import shutil

ROOT = Path(__file__).resolve().parents[1]
RP = Path('C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8')
# spellid242 is the existing random-only flying wand;189 has no collectible book.
BOOKS = [
    *[(243+i, 'tiny_'+name, 'tiny_'+name+'_wand', 'epic', None)
      for i, name in enumerate(('fire','blind','air','stone','teleport','ruby','terra','acid','electro'))],
    (252,'soulless','soulcorruption_wand','base',None),
    (253,'soulless_metasola','academy_soulcorruption_wand','base','mode_create_metasola'),
    (254,'halloween_wind','vex_wind_wand','rare',None),
    (255,'halloween_wind_self','academy_vex_wind_wand','rare','mode_user'),
    (256,'halloween_wind_others','academy_vex_wind_wand','rare','mode_not_user'),
    (257,'halloween_wind_charges','academy_vex_wind_wand','rare','mode_windball'),
    (258,'fartus','fartus_wand','legendary',None),
    (259,'gaycaster','gaycaster','legendary',None),
    (260,'ice','ice_wand','legendary',None),
    (261,'shish_wall','shish_wall_wand','rare',None),
    *[(271+i, 'gaycaster_'+name, 'academy_gaycaster', 'legendary', mode) for i,(name,mode) in enumerate((
        ('red','mode_red'),('orange','mode_orange'),('yellow','mode_yellow'),('green','mode_green'),
        ('cyan','mode_light_blue'),('blue','mode_blue'),('purple','mode_purple')))],
    (278,'snow','snow_wand','rare',None),
    (279,'snow_academy','academy_snow_wand','rare',None),
]
RETIRED=('summoner_vexes','psychovex','summoner_skeletons','summoner_ravager','summoner_elite_skeletons',
         'summoner_ghost_rider','summoner_ghasts','summoner_blazes','summoner_guardians')
COLORS = {'base':'gray','rare':'yellow','epic':'dark_purple','legendary':'red'}

def read(path):
    return json.loads(path.read_text(encoding='utf-8'))

def write(path, value, indent=2):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes((json.dumps(value,ensure_ascii=False,indent=indent)+'\n').encode('utf-8'))

def text(key, color):
    return {'translate':key,'italic':False,'color':color}

def generate():
    leaves=ROOT/'src/main/resources/data/lbc/loot_table/ported'
    manifest_path=ROOT/'src/main/resources/skywars2/remaining_grimoires.json'
    manifest=read(manifest_path)
    previous_names={'skywars2:'+d['id'] for d in manifest['items'] if d['spellId']>=243}
    manifest['items']=[d for d in manifest['items'] if d['spellId']<243]
    broad_path=ROOT/'src/main/resources/data/lbc/loot_table/spells_library.json'
    broad=read(broad_path); extra_names={f'skywars2:{b[1]}_grimoire' for b in BOOKS}
    broad['pools'][0]['entries']=[e for e in broad['pools'][0]['entries'] if e['name'] not in extra_names|previous_names]
    template=ROOT/'resourcepack_template'
    # Author explicitly removed the entire summoning-book category. Never reuse its spell IDs262..270.
    for name in RETIRED:
        paths=[leaves/(name+'_grimoire.json'),template/f'assets/lbc/items/spelllib/{name}.json',
               template/f'assets/lbc/models/item/spelllib/{name}.json',template/f'assets/lbc/textures/item/spelllib/{name}.png']
        for path in paths:
            path.resolve().relative_to(ROOT.resolve())
            assert not path.is_symlink(),path
            if path.exists():path.unlink()
    translations={lang:read(RP/f'assets/minecraft/lang/{lang}.json') for lang in ('ru_ru','en_us','rpr')}
    # Existing author edits in template are authoritative; only add new keys/remove retired books.
    additions={lang:read(template/f'updates/extra_grimoires.{lang}.json') if (template/f'updates/extra_grimoires.{lang}.json').exists() else {} for lang in translations}
    wanted={'libspell_'+name+suffix for _,name,*_ in BOOKS for suffix in ('','.lore')}
    additions={lang:{key:value for key,value in values.items() if key in wanted and key not in translations[lang]} for lang,values in additions.items()}
    rows=[]
    for sid,name,wand,rarity,mode in BOOKS:
        native=name+'_grimoire'; model='lbc:spelllib/'+name
        source=read(leaves/(wand+'.json'))['pools'][0]['entries'][0]
        components={}
        for fn in source['modifier']:
            if fn['type']=='minecraft:set_components':components.update(fn['components'])
        title=next(fn['name'] for fn in source['modifier'] if fn['type']=='minecraft:set_name')
        if isinstance(title,list):title=next(t for t in title if 'translate' in t)
        wand_key=title['translate']
        tier=components['minecraft:tooltip_style']; key='libspell_'+name
        lore=[[text('type','aqua'),text('libspell','aqua')],{'text':''},text(key+'.lore','gold'),
              [text('rarity','white'),text(rarity,COLORS[rarity])]]
        if mode or name=='snow_academy':lore[-1].append(text('enhanced','gray'))
        title=[text(key,'white')]
        data=f'{{transformable:1,gadget:1,libspell:1,spellwithcooldown:0,spellid:{sid}}}'
        entry={'type':'minecraft:item','name':'skywars2:'+native,'modifier':[
            {'type':'minecraft:set_components','components':{'minecraft:max_stack_size':99,'minecraft:tooltip_style':tier,'minecraft:item_model':model}},
            {'type':'minecraft:set_name','name':title}, {'type':'minecraft:set_lore','mode':'replace_all','lore':lore},
            {'type':'minecraft:set_custom_data','tag':data}]}
        write(leaves/(native+'.json'),{'pools':[{'rolls':1,'entries':[entry]}]})
        broad['pools'][0]['entries'].append(deepcopy(entry))
        manifest['items'].append({'spellId':sid,'id':native,'model':model,'tooltip':tier,'count':99,'name':title,'lore':lore,'data':data})
        for lang,old in translations.items():
            label=old.get(wand_key,wand_key); selected=old.get(mode,mode) if mode else None
            ru=lang=='ru_ru'
            if key not in old:additions[lang].setdefault(key,(f'Гримуар «{label}»' if ru else f'Grimoire: {label}')+(f' — {selected}' if selected else ''))
            if key+'.lore' not in old:additions[lang].setdefault(key+'.lore',(f'Одноразовое заклинание жезла «{label}».' if ru else f'Cast {label} once.')+(f' Режим: {selected}.' if ru and selected else f' Mode: {selected}.' if selected else ''))
        item_path=Path(f'assets/lbc/items/spelllib/{name}.json')
        model_path=Path(f'assets/lbc/models/item/spelllib/{name}.json')
        if not (RP/item_path).exists():write(template/item_path,{'model':{'type':'minecraft:model','model':'lbc:item/spelllib/'+name}})
        if not (RP/model_path).exists():write(template/model_path,{'parent':'minecraft:item/generated','textures':{'layer0':'lbc:item/spelllib/'+name}})
        target=template/f'assets/lbc/textures/item/spelllib/{name}.png';target.parent.mkdir(parents=True,exist_ok=True)
        if not target.exists() and not (RP/target.relative_to(template)).exists():shutil.copyfile(RP/'assets/minecraft/textures/item/spelllib/random.png',target)
        rows.append({'spellId':sid,'id':native,'wand':wand,'tier':tier,'rarity':rarity,'mode':mode})
    write(manifest_path,manifest);write(broad_path,broad)
    for lang,entries in additions.items():
        target=template/f'updates/extra_grimoires.{lang}.json'
        if entries:write(target,entries)
        elif target.exists():
            target.resolve().relative_to(ROOT.resolve())
            assert not target.is_symlink(),target
            target.unlink()
    write(template/'EXTRA_GRIMOIRES.json',rows)
    advancement=ROOT/'src/main/resources/data/lbc/advancement/true_advancements/legends/all_grimoires.json'
    adv=read(advancement);prototype=deepcopy(adv['criteria']['spell_1'])
    adv['criteria'].pop('spell_189',None)
    for key in list(adv['criteria']):
        if key.startswith('spell_') and int(key[6:])>=243:adv['criteria'].pop(key)
    for sid,*_ in BOOKS:
        criterion=deepcopy(prototype);criterion['conditions']['items'][0]['predicates']['minecraft:custom_data']=f'{{libspell:1,spellid:{sid}}}'
        adv['criteria'][f'spell_{sid}']=criterion
    assert len(adv['criteria'])==240+len(BOOKS)
    write(advancement,adv,indent=4)
    print(f'Generated{len(BOOKS)} extra books,{240+len(BOOKS)}-entry library/collection and matching model/texture/key placeholders; author RP untouched.')

if __name__=='__main__':generate()
