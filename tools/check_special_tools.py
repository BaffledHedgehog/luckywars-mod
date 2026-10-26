"""Read-only source/pool/RP contracts for Mortis and Death Scythe."""
import json
from pathlib import Path
from import_lbc_special_tools import ROOT, SOURCE, expected


def main():
    tables=expected()
    for name,value in tables.items():assert json.loads((ROOT/'src/main/resources'/name).read_text(encoding='utf-8'))==value,name
    keys=set()
    def collect(v):
        if isinstance(v,dict):
            if 'translate' in v:keys.add(v['translate'])
            for child in v.values():collect(child)
        elif isinstance(v,list):
            for child in v:collect(child)
    for t in tables.values():collect(t)
    rp=Path('C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8')
    for lang in ('en_us','ru_ru'):
        available=json.loads((rp/f'assets/minecraft/lang/{lang}.json').read_text(encoding='utf-8-sig'))
        assert not keys-available.keys(),(lang,keys-available.keys())
    for model in ('killer_shovel','death_skythe','halloween_mace','soulhunter_mace','banhammer','banhammer3d'):assert (rp/f'assets/lbc/items/{model}.json').is_file(),model
    source=(SOURCE.parent/'lbcr/function/raycast_loop14.mcfunction').read_text(encoding='utf-8')
    assert '0.08' in source and 'Distance=..70' in source and '15 cactus' in source
    mortis=(SOURCE/'function/workingitems/mortisrc.mcfunction').read_text(encoding='utf-8')
    assert 'Distance=..40' in mortis and '^ ^ ^0.25' in mortis and 'black_stained_glass' in mortis
    catalog=json.loads((ROOT/'src/main/resources/skywars2/loot_test_catalog.json').read_text())['tables']
    for pool in ('transformables_shovel','transformables_shovel_all','transformables_hoe_all','death_loot'):assert 'lbc:'+pool in catalog
    assert 'lbc:transformables_axe_all' in catalog
    print(f'Special tools: {len(tables)} source tables, four closed pools, {len(keys)} existing RP keys and six models verified; no writes')


if __name__=='__main__':main()
