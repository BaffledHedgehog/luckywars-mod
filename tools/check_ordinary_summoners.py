"""Source-preserving ordinary summoner items/formations and public pool closure."""
from copy import deepcopy
from pathlib import Path
import hashlib
import json
import import_lbc_ordinary_summoners as importer
from import_game_resources import migrate_loot_schema


def main():
    root=importer.ROOT
    original=json.loads((importer.SOURCE/'loot_table/transformables_summons.json').read_text(encoding='utf-8-sig'))
    migrate_loot_schema(original)
    current=json.loads((root/'src/main/resources/data/lbc/loot_table/transformables_summons.json').read_text(encoding='utf-8'))
    assert len(original['pools'][0]['entries'])==len(current['pools'][0]['entries'])==8
    for old,new in zip(original['pools'][0]['entries'],current['pools'][0]['entries']):
        expected=deepcopy(old);expected['name']=new['name']
        assert expected==new and new['name'].startswith('skywars2:'),new['name']
    source=json.loads((root/'src/main/resources/skywars2/ordinary_summoners.json').read_text(encoding='utf-8'))
    paths=[root/'src/main/resources/skywars2/ordinary_summoners.json',root/'src/main/resources/data/lbc/loot_table/transformables_summons.json']
    rp=Path('C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8')
    for row in source['items']:
        path=root/f"src/main/resources/data/lbc/loot_table/ported/{row['id']}.json";paths.append(path)
        leaf=json.loads(path.read_text(encoding='utf-8'))['pools'][0]['entries'][0]
        broad=next(e for e in current['pools'][0]['entries'] if e['name']=='skywars2:'+row['id'])
        expected=deepcopy(broad);expected.pop('weight',None);expected.pop('quality',None)
        assert leaf==expected
        namespace,name=row['model'].split(':')
        assert (rp/f'assets/{namespace}/items/{name}.json').exists()
        assert len(source['formations'][row['kind']]) in (1,2,4,9,20)
        assert source['presets']['ordinary_'+row['kind']].startswith('{id:"minecraft:')
    before={p:hashlib.sha256(p.read_bytes()).hexdigest() for p in paths}
    importer.generate()
    assert all(hashlib.sha256(p.read_bytes()).hexdigest()==value for p,value in before.items())
    print('PASS7 exact source items/presets/formations,8 native ordinary pool entries, unchanged weights/components, authored RP models and deterministic generation')


if __name__=='__main__':main()
