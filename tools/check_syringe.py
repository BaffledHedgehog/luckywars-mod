"""Read-only source/presentation and complete expanded summon pool closure."""
from copy import deepcopy
from pathlib import Path
import json
from import_game_resources import migrate_loot_schema

ROOT=Path(__file__).resolve().parents[1]
SOURCE=Path('E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc')
RP=Path('C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8')
read=lambda p:json.loads(p.read_text(encoding='utf-8-sig'))


def check():
    tables=ROOT/'src/main/resources/data/lbc/loot_table'
    for name,native in [('siggewinne_syringe','skywars2:siggewinne_syringe'),('tuntijaa_blind_vex','skywars2:psychovex_wand')]:
        old=read(SOURCE/f'loot_table/{name}.json');migrate_loot_schema(old)
        old['pools'][0]['entries'][0]['name']=native
        assert read(tables/f'{name}.json')==old,name
    assert read(tables/'ported/siggewinne_syringe.json')==read(tables/'siggewinne_syringe.json')
    old=read(SOURCE/'loot_table/transformables_summons_all.json');migrate_loot_schema(old)
    assert read(tables/'transformables_summons_all.json')==old,'Original expanded pool weights/delegation'
    source=read(ROOT/'src/main/resources/skywars2/syringe.json')
    entry=read(tables/'siggewinne_syringe.json')['pools'][0]['entries'][0]
    functions={f['type']:f for f in entry['modifier']}
    assert source['name']==functions['minecraft:set_name']['name']
    assert source['lore']==functions['minecraft:set_lore']['lore'] and len(source['lore'])==7
    assert source['data']==functions['minecraft:set_custom_data']['tag']
    assert source['model']=='lbc:syringe' and source['tooltip']=='lbc:tier_a'
    assert (RP/'assets/lbc/items/syringe.json').is_file()
    sounds=read(RP/'assets/minecraft/sounds.json')
    assert all('lbcsounds.'+name in sounds for name in ['fuwafuwa','awaawa','yurayura'])
    keys=read(RP/'assets/minecraft/lang/ru_ru.json')
    assert all('siggewinne_syringe'+s in keys for s in ['',*[f'.lore.{i}' for i in range(1,6)]])
    catalog=read(ROOT/'src/main/resources/skywars2/loot_test_catalog.json')['tables']
    assert all(t in catalog for t in ['lbc:transformables_summons_all','lbc:siggewinne_syringe','lbc:tuntijaa_blind_vex'])
    print('PASS exact source Syringe presentation, native aliases, unchanged expanded summon weights, authored model/translations/three sounds and ready acquisition closure; no generation')


if __name__=='__main__':check()
