"""Verify exact SLR build, resource payloads and completed isolated runtime reports."""
import hashlib
import json
import shutil
import sys
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools'))
from import_game_resources import migrate_loot_schema
from import_lbc_consuming import RP
from import_lbc_loadouts import SOURCE

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def read(path):
    return json.loads(path.read_text(encoding='utf-8-sig'))

def main():
    resources = ROOT / 'src/main/resources'
    artifact = ROOT / 'build/libs/skywars2-0.1.0.jar'
    digest = sha(artifact)
    source = read(SOURCE / 'loot_table/slr.json')
    migrate_loot_schema(source)
    source['pools'][0]['entries'][0]['name'] = 'skywars2:slr'
    paths = ['data/lbc/loot_table/ported/slr.json', 'data/lbc/loot_table/slr.json']
    for path in paths:
        assert read(resources / path) == source, path
    for tag in ('breakable_slr', 'glasses_2', 'glasses', 'airs'):
        path = f'data/minecraft/tags/block/{tag}.json'
        assert read(resources / path) == read(SOURCE.parent / f'minecraft/tags/block/{tag}.json')
        paths.append(path)
    java = ['SkyWars2.java'] + ['game/' + name + '.java' for name in
            ['LbcShooters', 'LbcWands', 'LbcItems', 'LbcMagic', 'GameMenus', 'GameChecks']]
    java += ['mixin/' + name + '.java' for name in ['MovementLimitsMixin',
            'LobbyInventoryPacketMixin', 'ObserverPacketMixin', 'PlayerMechanicsMixin',
            'CreativeBlockDropsMixin', 'LbcColorEffectsMixin']]
    with zipfile.ZipFile(ROOT / 'build/libs/skywars2-0.1.0-sources.jar') as archive:
        for path in java:
            full = 'me/skywars2/' + path
            assert archive.read(full) == (ROOT / 'src/main/java' / full).read_bytes(), full
    with zipfile.ZipFile(artifact) as archive:
        assert archive.testzip() is None
        for path in paths:
            assert archive.read(path) == (resources / path).read_bytes(), path
    keys = {'karich_mode_' + str(n) for n in range(1, 5)}
    def collect(value):
        if isinstance(value, list):
            for child in value: collect(child)
        elif isinstance(value, dict):
            if 'translate' in value: keys.add(value['translate'])
            for child in value.values(): collect(child)
    collect(source)
    languages = {}
    for lang in ('ru_ru', 'en_us'):
        path = RP / f'assets/minecraft/lang/{lang}.json'
        data = read(path)
        assert not keys - data.keys()
        languages[lang] = {'sha256': sha(path), 'values': {k: data[k] for k in sorted(keys)}}
    assert (RP / 'assets/lbc/items/super_rifle.json').is_file()
    primary = read(ROOT / 'build/verification/slr-results.json')
    regression = read(ROOT / 'build/verification/slr-shared-results.json')
    for report in (primary, regression):
        assert report['artifactSha256'] == digest
    assert len(primary['completed']) == 5
    assert regression['completed'] == ['frost-menu', 'pinegun-menu', 'frost', 'pinegun',
                                        'frost-cleanup', 'pinegun-cleanup']
    main_jar = ROOT.parent / 'mods/skywars2.jar'
    old = 'a0c4cb03c4682c99e71e0c197b376f98dc6781e1f153ec7ee61cba67408606a5'
    assert sha(main_jar) == old, 'Main server JAR changed unexpectedly'
    build_log = (ROOT / 'build/verification/slr-build.log').read_text(encoding='utf-16')
    assert 'BUILD SUCCESSFUL' in build_log and 'BUILD FAILED' not in build_log
    target = ROOT / f'build/verification/skywars2-slr-{digest[:8]}.jar'
    shutil.copyfile(artifact, target)
    report = {'artifact': str(target), 'sha256': digest,
              'unchangedMainSha256': old, 'javaSourceFiles': java,
              'packagedResources': paths, 'resourcePackLanguages': languages,
              'slrRuntime': primary, 'shooterRegression': regression,
              'limits': ['No real-client visual test', 'No mass-load MSPT benchmark']}
    (ROOT / 'build/verification/slr-verification.json').write_text(
        json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({'sha256': digest, 'archive': str(target),
                      'checks': len(primary['completed']) + len(regression['completed'])}))

if __name__ == '__main__':
    main()
