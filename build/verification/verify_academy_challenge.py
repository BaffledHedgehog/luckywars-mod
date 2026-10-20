"""Verify the native challenge build and archive only the fully tested artifact."""
import hashlib
import json
import shutil
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools'))
from import_game_resources import migrate_loot_schema
from import_lbc_loadouts import SOURCE
from import_lbc_consuming import RP

def read(path):
    return json.loads(path.read_text(encoding='utf-8-sig'))

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def main():
    artifact = ROOT / 'build/libs/skywars2-0.1.0.jar'
    resources = ROOT / 'src/main/resources'
    digest = sha(artifact)
    primary = read(ROOT / 'build/verification/academy-challenge-results.json')
    shared = read(ROOT / 'build/verification/academy-challenge-shared-results.json')
    assert primary['passed'] and len(primary['completed']) == 5
    assert shared['completed'] == ['regressions', 'attribute-hud', 'magic-menu', 'magic', 'magic-cleanup']
    assert primary['artifactSha256'] == shared['artifactSha256'] == digest
    expected = read(SOURCE / 'advancement/challenges/no_knowledge.json')
    assert expected.pop('rewards') == {'function': 'lbc:swrg_kit_integration/gain/challenge_completed'}
    advancement = 'data/lbc/advancement/challenges/no_knowledge.json'
    assert read(resources / advancement) == expected
    assert 'display' not in expected and expected['criteria']['requirement']['trigger'] == 'minecraft:impossible'
    token = read(SOURCE / 'loot_table/kit/academy_token.json')
    migrate_loot_schema(token)
    token['pools'][0]['entries'][0]['name'] = 'skywars2:academy_token'
    # Existing native importer omits the implicit THIS target and wraps a lone name component.
    for modifier in token['pools'][0]['entries'][0]['modifier']:
        if modifier.get('entity') == 'this': modifier.pop('entity')
        if isinstance(modifier.get('name'), dict): modifier['name'] = [modifier['name']]
    token_path = 'data/lbc/loot_table/ported/academy_token.json'
    assert read(resources / token_path) == token
    java = ['SkyWars2.java'] + ['game/' + name + '.java' for name in
        ['LbcLoadouts', 'GameMenus', 'PlayerStore', 'GameConfig', 'LbcAcademy',
         'OriginalGuiDesign', 'LbcSkills', 'LbcItems', 'AttributeHud', 'GameController',
         'GameChecks', 'RegressionChecks']]
    java += ['mixin/' + name + '.java' for name in ['AdvancementFilterMixin', 'KnowledgePacketMixin']]
    with zipfile.ZipFile(ROOT / 'build/libs/skywars2-0.1.0-sources.jar') as archive:
        for path in java:
            full = 'me/skywars2/' + path
            assert archive.read(full) == (ROOT / 'src/main/java' / full).read_bytes(), full
    paths = [advancement, token_path, 'skywars2.mixins.json', 'skywars2/gui_design.json']
    with zipfile.ZipFile(artifact) as archive:
        assert archive.testzip() is None
        for path in paths:
            assert archive.read(path) == (resources / path).read_bytes(), path
        assert 'KnowledgePacketMixin' in json.loads(archive.read('skywars2.mixins.json'))['mixins']
    design = read(resources / 'skywars2/gui_design.json')
    page = next(p for p in design['pages'] if p['namespace'] == 'lbc' and p['pageId'] == '12000')
    keys = {'challenge_completed', 'new_challenge', 'lbc.adv.academy_upgraded.lore'}
    models = {'lbc:no_knowledge', 'lbc:locked', 'lbc:advancement_icon/academy'}
    def collect(value):
        if isinstance(value, list):
            for child in value: collect(child)
        elif isinstance(value, dict):
            if 'translate' in value: keys.add(value['translate'])
            for child in value.values(): collect(child)
    collect(page)
    collect(token)
    languages = {}
    for lang in ('ru_ru', 'en_us'):
        data = read(RP / f'assets/minecraft/lang/{lang}.json')
        assert not keys - data.keys(), (lang, keys - data.keys())
        languages[lang] = {k: data[k] for k in sorted(keys)}
    for identifier in models:
        namespace, path = identifier.split(':')
        assert (RP / f'assets/{namespace}/items/{path}.json').is_file()
    old = 'a0c4cb03c4682c99e71e0c197b376f98dc6781e1f153ec7ee61cba67408606a5'
    assert sha(ROOT.parent / 'mods/skywars2.jar') == old
    log = (ROOT / 'build/verification/academy-challenge-build.log').read_text(encoding='utf-16')
    assert 'BUILD SUCCESSFUL' in log and 'BUILD FAILED' not in log
    target = ROOT / f'build/verification/skywars2-academy-challenge-{digest[:8]}.jar'
    shutil.copyfile(artifact, target)
    report = {'sha256': digest, 'archive': str(target), 'unchangedMainSha256': old,
              'runtime': primary, 'sharedRegression': shared, 'javaSources': java,
              'packagedResources': paths, 'sourceModels': sorted(models), 'languages': languages,
              'limits': ['No real-client visual test', 'No mass-load MSPT benchmark',
                         'Legacy global reward pick/win statistics remain a separate migration']}
    (ROOT / 'build/verification/academy-challenge-verification.json').write_text(
        json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({'sha256': digest, 'archive': str(target), 'checks': 10}))

if __name__ == '__main__':
    main()
