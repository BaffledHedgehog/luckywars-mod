"""Read-only exact totem components, source terrain mask, RP references and packaged leaves."""
import json
from pathlib import Path
import nbtlib
from import_lbc_totems import ROOT, SOURCE, expected


def main():
    tables=expected()
    for name,value in tables.items(): assert json.loads((ROOT/'src/main/resources'/name).read_text(encoding='utf-8'))==value,name
    shape=nbtlib.load(SOURCE/'structure/notsourcelava.nbt')
    assert [int(i) for i in shape['size']]==[30,30,30] and len(shape['blocks'])==27000
    assert str(shape['palette'][0]['Name'])=='minecraft:lava' and str(shape['palette'][0]['Properties']['level'])=='8'
    assert all(int(v['state'])==0 for v in shape['blocks'])
    keys=set()
    def collect(v):
        if isinstance(v,dict):
            if 'translate' in v:keys.add(v['translate'])
            for child in v.values():collect(child)
        elif isinstance(v,list):
            for child in v:collect(child)
    for t in tables.values():collect(t)
    rp=Path('C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8')
    for lang in ('ru_ru','en_us'):
        available=json.loads((rp/f'assets/minecraft/lang/{lang}.json').read_text(encoding='utf-8-sig'));assert not keys-available.keys(),(lang,keys-available.keys())
    for model in ('infinite_totem','deviltotem'):assert (rp/f'assets/lbc/items/{model}.json').is_file(),model
    catalog=json.loads((ROOT/'src/main/resources/skywars2/loot_test_catalog.json').read_text())['tables']
    assert 'lbc:transformables_totem' in catalog and 'lbc:infinite_totem' in catalog
    print(f'Totems: {len(tables)} exact tables, source30-cube lava8 mask, {len(keys)} existing RP keys and two models verified; no writes')


if __name__=='__main__':main()
