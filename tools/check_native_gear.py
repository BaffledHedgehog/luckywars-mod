"""Read-only source components, closed pools, native Electro alias and existing RP translations."""
import json
from pathlib import Path
from import_lbc_native_gear import ROOT, expected


def main():
    tables = expected()
    for name, data in tables.items():
        assert json.loads((ROOT / 'src/main/resources' / name).read_text(encoding='utf-8')) == data, name
    keys = set()
    def collect(value):
        if isinstance(value, dict):
            if 'translate' in value:
                keys.add(value['translate'])
            for child in value.values():
                collect(child)
        elif isinstance(value, list):
            for child in value:
                collect(child)
    for data in tables.values():
        collect(data)
    rp = Path('C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8')
    for language in ('ru_ru', 'en_us'):
        present = json.loads((rp / f'assets/minecraft/lang/{language}.json').read_text(encoding='utf-8-sig'))
        assert not keys - present.keys(), (language, keys - present.keys())
    catalog = json.loads((ROOT / 'src/main/resources/skywars2/loot_test_catalog.json').read_text())['tables']
    assert all(name in catalog for name in ('lbc:transformables_shield', 'lbc:transformables_trident', 'lbc:electro_shield'))
    print(f'Native gear: {len(tables)} exact tables, {len(keys)} existing RP keys, original banner patterns and native Electro alias verified; no writes')


if __name__ == '__main__':
    main()
