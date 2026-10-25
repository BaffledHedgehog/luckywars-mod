"""Read-only source evidence inventory; does not assert call-graph/behavior equivalence."""
from pathlib import Path
import argparse
import csv
import hashlib
import json
import re
import zipfile

ROOT = Path(__file__).resolve().parents[1]
LEGACY = Path('E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data')
PACK = Path('C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8')
# Explicit production implementation families. Shared helpers remain visible in native_calls.json.
FAMILIES = {
 'IceWand': 'ice_wand', 'Corruption': 'corruption_wand', 'AcidCorruption': 'acid_corruption_wand',
 'Erecto': 'erecto_wand', 'Tesla': 'tesla_wand', 'Prism': 'unstable_prism',
 'Gaycaster': 'gaycaster', 'GaycasterAcademy': 'academy_gaycaster',
 'AwakenedAcid': 'awakened_acid_wand', 'CausticOmega': 'caustic_wand omega_flag',
 'AdvancedSnow': 'advanced_ender_wand snow_wand', 'EnderWiki': 'ender_wand bad_wiki',
 'Neutral': 'neutral_wand feather_greed neutral_ball', 'BloodConfuse': 'blood_wand confuse_wand',
 'EvokerBass': 'evoker_wand bass_wand', 'Lifestealer': 'lifestealer lifestealer_upgraded',
 'Blaze': 'blaze_wand fireblaze_wand falling_stars_wand', 'Summons': 'summoner_vexes_wand psychovex_wand',
 'Bolts': 'infbolt_wand lightning_bolts_wand boltbolt_wand bolt_material', 'Consuming': 'consuming_wand creeper_army_spell destroyer_spell',
 'Duplicating': 'duplicating_wand divider', 'Homing': 'exahoming_wand',
 'Psycho': 'psychowand psychoblind_wand', 'Air': 'air_wand', 'Blind': 'blind_wand',
 'Stone': 'stone_wand', 'Teleport': 'teleport_wand', 'Ruby': 'ruby_wand',
 'Terra': 'terra_wand terra_sword true_terra_sword terra_razorpine', 'Electro': 'electro_wand cyberkatana shit_katana electro_shield electroblocker homing_lightning',
 'Exasperato': 'exasperato_wand', 'Exoperso': 'exoperso_wand', 'Bedrock': 'bedrock_wand',
 'Wall': 'wall_wand', 'Stonekivi': 'stonekivi_wand', 'Sheep': 'sheep_wand',
 'Danmaku': 'danmaku_staff danmaku_grimoire', 'Soulless': 'soulcorruption_wand metasola_1 metasola_2 metasola_3 metasola_4 metasola_5 metasola_6',
 'Wands': 'tiny_fire_wand tiny_blind_wand tiny_air_wand tiny_stone_wand tiny_teleport_wand tiny_ruby_wand tiny_terra_wand tiny_acid_wand tiny_electro_wand fire_wand acid_wand',
 'VoidArcanum': 'void_arcanum', 'DefensiveArcanums': 'healing_arcanum barrier_arcanum',
 'Tanner': 'tannerkivi', 'Kiuas': 'kiuaskivi sun_arcanum', 'Reactive': 'reactive_shish shish_wall_wand',
 'Shooters': 'slr govnomet razorpine transformables_shooter transformables_shooter_all phantom_knives kfc_gun woodificator attack_helicopter helicopter_boss',
 'Govnomet': 'govnomet', 'VexSword': 'vex_sword', 'Frost': 'ice_sword',
 'HalloweenWind': 'vex_wind_wand halloween_mace', 'Ak47': 'ak47 ak47_finite', 'Magnum': 'magnum',
 'Mobgun': 'mobgun', 'LaserMinigun': 'laser_minigun',
 'Fartus': 'fartus_wand', 'Wtf': 'wtf_item', 'Hohlopitek': 'hohlopitek',
 'Soulhunter': 'soulhunter_mace', 'Stand': 'stand_oraora stand_arrow homing_lightning',
 'TimeStop': 'stand_clocks', 'Babylon': 'babylon_gate', 'Magic': 'magic_beacon mechanism_beacon',
 'Homes': 'magic_mirror', 'IceMaterials': 'runic_ice_shard neutral_ball',
}
# These owners are compared in the companion drop/passive audit. A mapped leaf
# establishes ownership, never a blanket PASS or a new explicit-sound requirement.
DROP_FAMILIES={
 'Academy':'academy_token magic_academy_drop',
 'AntDrops':'acid_burst_drop ant_swarm_drop muravei_spawn_egg',
 'Antigravity':'antigravity_drop', 'BeeDrop':'bees_drop bees_drop_old honey_bee_spawn_egg',
 'Trinkets':'blood_anal_boom',
 'CageDrops':'cage_drop_anvil cage_drop_fireball cage_drop_lava cage_drop_obsidian cage_drop_potions',
 'ChestDropItem':'chest_drop_bastion chest_drop_bonus chest_drop_end_city chest_drop_treasure chest_drop_village',
 'CorrosionDrops':'corrosion_drop corrosion_drop_old',
 'CreeperDrops':'creeper_drop creeper_drop_old sarvatra_creeper_spawn_egg',
 'FeedbackDrops':'death_lust_drop yamete_drop',
 'FallingNetheriteDrops':'falling_netherite_drop falling_netherite_drop_old',
 'CreatureDrops':'fat_skeleton_spawn_egg normal_fat_skeleton_drop old_bob_drop old_skeleton_drop old_vex_drop vex_drop low_vex_spawn_egg',
 'HelicopterEncounter':'helicopter_encounter helicopter_spawn_egg',
 'NpcDropItems':'hohol_spawn_egg shahid_spawn_egg',
 'HoleDrops':'hole_drop hole_drop_old', 'Homes':'home_drop',
 'IslandDrops':'island_lower_drop_old island_raise_drop', 'KfcFood':'kfc_chick',
 'EnzanDrop':'levitation_drop', 'LibraryDrop':'library_drop library_drop_old',
 'LuckyPigs':'lucky_pig_spawn_egg lucky_pigs_drop',
 'LuckyRoomDrops':'lucky_room_drop old_lucky_room_drop', 'Magnum':'magnum_ammo',
 'Mana':'mana_gem mana_add_mana_drink mana_add_mana_lingering_potion mana_add_mana_splash_potion mana_add_manamax_drink mana_add_manamax_lingering_potion mana_add_manamax_splash_potion mana_add_manaregen_drink mana_add_manaregen_lingering_potion mana_add_manaregen_splash_potion mana_remove_mana_drink mana_remove_mana_lingering_potion mana_remove_mana_splash_potion mana_temporary_mana_drink mana_temporary_mana_lingering_potion mana_temporary_mana_splash_potion',
 'Colors':'mystical_artifact mystical_artifact_arrows mystical_artifact_clear mystical_artifact_colors mystical_artifact_debuff mystical_artifact_explosions mystical_artifact_liquids mystical_artifact_metabolism',
 'PortalDrops':'old_random_portal_drop random_portal_drop',
 'DroneDrops':'old_tnt_drone_drop tnt_drone_drop',
 'OpAnvilDrop':'op_anvil_drop op_anvil_drop_old',
 'OverloadArmor':'overload_boots overload_chestplate overload_crux overload_helmet overload_leggings',
 'PisaDrops':'pisa_boats_drop', 'PotionSpawnerDrops':'potion_spawner_drop potion_spawner_drop_old',
 'RerollCrafting':'reroll_machine', 'Items':'scare_drop scare_drop_old',
 'SpecialDrops':'shulker_bullets_drop shulker_bullets_drop_old',
 'SlabDrops':'slab_drop slab_drop_old slab_spawn_egg', 'Stalin':'stalin_drop stalin_spawn_egg',
 'TntDrops':'tnt_drop', 'Transformator':'transformator_drop transformator_spawn_egg',
 'Ultraponos':'ultraponos_drop', 'ZombieTntDrops':'zombie_tnt_drop',
}
ACTIVE_FAMILIES=dict(FAMILIES)
for _family,_leaves in DROP_FAMILIES.items():
    FAMILIES[_family]=(FAMILIES.get(_family,'')+' '+_leaves).strip()

def dump(path, value):
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output',type=Path,default=ROOT/'build/verification/all-item-sounds/wands')
    args=parser.parse_args(); out=args.output; out.mkdir(parents=True,exist_ok=True)
    legacy=[]
    pattern=re.compile(r'\bplaysound\s+(\S+)\s+(\S+)\s+(@[a-z](?:\[[^\]]*\])?|\S+)\s*(.*)')
    for p in sorted(LEGACY.rglob('*.mcfunction')):
        for n,line in enumerate(p.read_text(encoding='utf-8-sig').splitlines(),1):
            if line.lstrip().startswith('#'):continue
            m=pattern.search(line)
            if m:
                tail=m[4].split(); event=m[1] if ':' in m[1] else 'minecraft:'+m[1]
                legacy.append(dict(file=p.relative_to(LEGACY).as_posix(),line=n,event=event,category=m[2],
                    target=m[3],position=tail[:3],volume=tail[3] if len(tail)>3 else '1',
                    pitch=tail[4] if len(tail)>4 else '1',minVolume=tail[5] if len(tail)>5 else '0',command=line))
    dump(out/'legacy_playsound.json',legacy)
    native=[]; sources=ROOT/'src/main/java/me/skywars2/game'
    for p in sorted(sources.glob('Lbc*.java')):
        lines=p.read_text(encoding='utf-8').splitlines()
        for i,line in enumerate(lines):
            if re.search(r'LbcOverload\.sound\(|\.playSound\(|new (?:[\w.]+\.)?ClientboundSoundPacket\(',line):
                native.append(dict(file=p.relative_to(ROOT).as_posix(),line=i+1,
                    context='\n'.join(lines[max(0,i-1):min(len(lines),i+4)])))
    dump(out/'native_calls.json',native)
    leaves=[]
    for p in sorted((ROOT/'src/main/resources/data/lbc/loot_table/ported').glob('*.json')):
        raw=p.read_text(encoding='utf-8'); ids=sorted(set(re.findall(r'"name"\s*:\s*"(skywars2:[^"]+)"',raw)))
        families=[k for k,v in FAMILIES.items() if any(p.stem in {s,'academy_'+s} for s in v.split())]
        active=any(p.stem in {s,'academy_'+s} for v in ACTIVE_FAMILIES.values() for s in v.split())
        basis='active matrix in ALL_ITEM_WAND_SOUND_AUDIT_2026_10_25.md' if active else 'companion ALL_ITEM_DROP_SOUND_AUDIT_2026_10_25.md; ownership inventory, not per-leaf runtime qualification'
        leaves.append(dict(leaf=p.stem,items=ids,families=families,review_basis=basis if families else 'unclassified: requires source owner review'))
    dump(out/'ready_leaves.json',leaves)
    with (out/'family_matrix.csv').open('w',encoding='utf-8-sig',newline='') as f:
        writer=csv.writer(f);writer.writerow(['family','ready_leaves','native_sound_sites','status'])
        for family in FAMILIES:
            paths=[f"{n['file']}:{n['line']}" for n in native if Path(n['file']).stem=='Lbc'+family]
            writer.writerow([family,';'.join(l['leaf'] for l in leaves if family in l['families']),';'.join(paths),
                'family-level inventory; manual comparison in agent_notes/'+
                ('ALL_ITEM_DROP_SOUND_AUDIT_2026_10_25.md' if family in DROP_FAMILIES and family not in ACTIVE_FAMILIES
                 else 'ALL_ITEM_WAND_SOUND_AUDIT_2026_10_25.md')+'; no per-leaf runtime qualification'])
    # Verify custom event definitions and their OGG/event references recursively.
    definitions={}
    for p in (PACK/'assets').glob('*/sounds.json'):
        for key,value in json.loads(p.read_text(encoding='utf-8-sig')).items():
            definitions[p.parent.name+':'+key]=value
    custom=sorted({x['event'] for x in legacy if 'lbcsounds.' in x['event']})
    custom += sorted(set(re.findall(r'"(minecraft:lbcsounds\.[\w.]+)"(?!\s*\+)', '\n'.join(p.read_text(encoding='utf-8') for p in sources.glob('Lbc*.java'))))-set(custom))
    def inspect(event,seen):
        if event in seen:return {'event':event,'cycle':True}
        definition=definitions.get(event)
        if definition is None:return {'event':event,'definition_present':False}
        refs=[]
        for entry in definition.get('sounds',[]):
            value={'name':entry} if isinstance(entry,str) else entry
            name=value['name']; name=name if ':' in name else 'minecraft:'+name
            if value.get('type')=='event':refs.append(inspect(name,seen|{event}))
            else:
                ns,key=name.split(':',1);p=PACK/'assets'/ns/'sounds'/(key+'.ogg')
                refs.append({'name':name,'present':p.is_file(),'sha256':hashlib.sha256(p.read_bytes()).hexdigest() if p.is_file() else None})
        return {'event':event,'definition_present':True,'definition':definition,'references':refs}
    dump(out/'custom_assets.json',[inspect(e,set()) for e in sorted(set(custom))])
    source_jars=sorted((ROOT/'.gradle/loom-cache/minecraftMaven').rglob('*26.3-sources.jar'))
    vanilla={'available':bool(source_jars)}
    if source_jars:
        jar=source_jars[0]
        with zipfile.ZipFile(jar) as archive:
            registry=archive.read('net/minecraft/sounds/SoundEvents.java').decode('utf-8')
            events={'minecraft:'+value for value in re.findall(r'"([a-z_]+\.[a-z0-9_.]+)"',registry)}
            dynamic=[]
            for kind in ('Cat','Chicken','Cow','Pig','Wolf'):
                declaration=registry.index('private static Map<'+kind+'SoundVariants.SoundSet')
                block=registry[declaration:]
                next_method=block.find('\n\tprivate ',1)
                if next_method>=0:block=block[:next_method]
                suffixes=re.findall(r'registerForHolder\("entity\." \+ soundEventIdentifier \+ "\.([a-z_]+)"\)',block)
                name=next(name for name in archive.namelist() if name.endswith('/'+kind+'SoundVariants.java'))
                variants=archive.read(name).decode('utf-8')
                prefixes=re.findall(r'\b[A-Z_]+\("[a-z_]+",\s*"([a-z_]+)"\)',variants)
                for prefix in prefixes:
                    for suffix in suffixes:events.add('minecraft:entity.'+prefix+'.'+suffix)
                dynamic.append(dict(variant_source=name,prefixes=prefixes,suffixes=suffixes))
        literal_sites=[]
        for path in sorted((ROOT/'src/main/java').rglob('*.java')):
            if 'Checks' in path.name:continue
            for number,line in enumerate(path.read_text(encoding='utf-8').splitlines(),1):
                for match in re.finditer(r'"(minecraft:(?:ambient|block|entity|event|item|music|ui|weather|intentionally_empty)\.[a-z0-9_.]+)"(?!\s*\+)',line):
                    event=match[1]
                    literal_sites.append(dict(file=path.relative_to(ROOT).as_posix(),line=number,event=event,
                        registered=event in events,author_pack_override=event in definitions))
        unknown=[site for site in literal_sites if not site['registered'] and not site['author_pack_override']]
        vanilla.update(source_jar=str(jar),source_sha256=hashlib.sha256(jar.read_bytes()).hexdigest(),
            registry_ids=len(events),dynamic_registration_expansions=dynamic,native_literal_sites=literal_sites,
            unknown_native_literals=unknown,legacy_unknown_events=sorted({site['event'] for site in legacy
                if site['event'].startswith('minecraft:') and 'lbcsounds.' not in site['event']
                and site['event'] not in events and site['event'] not in definitions}))
    dump(out/'vanilla_registry_validation.json',vanilla)
    summary=dict(legacy_playsound_sites=len(legacy),native_sound_sites=len(native),
        ready_leaves=len(leaves),family_count=len(FAMILIES),mapped_leaves=sum(bool(l['families']) for l in leaves),
        unmapped_leaves=[l['leaf'] for l in leaves if not l['families']],
        custom_events=len(set(custom)),method='Static inventory; conditions/call graphs and client playback are not automatically qualified.')
    summary['unknown_native_vanilla_literals']=len(vanilla.get('unknown_native_literals',[])) if vanilla['available'] else None
    dump(out/'summary.json',summary);print(json.dumps(summary,indent=2))

if __name__=='__main__':main()
