"""Read-only source/component/package/RP checks; never invokes a generator."""
import json
from pathlib import Path
from import_lbc_tools import ROOT, SOURCE, expected


def main():
    templates = expected()
    for name, value in templates.items():
        assert json.loads((ROOT / 'src/main/resources' / name).read_text(encoding='utf-8')) == value, name
    recipe = ROOT / 'src/main/resources/data/skywars2/recipe/communism_combine.json'
    assert json.loads(recipe.read_text()) == {'type': 'skywars2:communism_combine'}
    catalog = json.loads((ROOT / 'src/main/resources/skywars2/loot_test_catalog.json').read_text())
    for family in ('axe', 'hoe', 'pickaxe'):
        assert f'lbc:transformables_{family}' in catalog['tables']
        if family == 'pickaxe': assert f'lbc:transformables_{family}_all' not in catalog['tables']
        # The special-tools follow-up closes expanded axe/hoe pools with native abilities.
    ray = (SOURCE.parent / 'lbcr/function/communism.mcfunction').read_text(encoding='utf-8')
    assert 'Distance 1' in ray and 'Distance=..200' in ray and '^ ^ ^1' in ray
    subordinate = (SOURCE.parent / 'lbcr/function/podchinenie_communism.mcfunction').read_text(encoding='utf-8')
    assert 'tag=summoned_cultist' in subordinate and 'tag=summactivated' in subordinate
    # Current author pack is read-only; collect exact source translation references recursively.
    rp = Path('C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8')
    keys = set()
    def collect(value):
        if isinstance(value, dict):
            if 'translate' in value: keys.add(value['translate'])
            for child in value.values(): collect(child)
        elif isinstance(value, list):
            for child in value: collect(child)
    for value in templates.values(): collect(value)
    for locale in ('ru_ru', 'en_us'):
        lang = json.loads((rp / f'assets/minecraft/lang/{locale}.json').read_text(encoding='utf-8-sig'))
        assert not keys - lang.keys(), (locale, keys - lang.keys())
    for model in ('communism', 'hammer', 'sickle'):
        assert (rp / f'assets/lbc/items/{model}.json').is_file(), model
    print(f'Ordinary tools: {len(templates)} exact source tables, native recipe, three catalog pools and {len(keys)} existing RP keys passed; no writes')


if __name__ == '__main__':
    main()
