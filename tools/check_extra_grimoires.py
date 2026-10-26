"""Offline closure for the added library families and the copyable RP template."""
from pathlib import Path
import hashlib
import json
import subprocess
import generate_extra_grimoires as gen

ROOT = gen.ROOT
load = gen.read
digest = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()


def check():
    rows = load(ROOT/'resourcepack_template/EXTRA_GRIMOIRES.json')
    assert len(rows) == 28 and [r['spellId'] for r in rows] == list(range(243, 262))+list(range(271,280))
    manifest = load(ROOT/'src/main/resources/skywars2/remaining_grimoires.json')
    extra = [r for r in manifest['items'] if r['spellId'] >= 243]
    assert len(extra) == 28
    broad_path = 'src/main/resources/data/lbc/loot_table/spells_library.json'
    old_broad = json.loads(subprocess.check_output(['git', 'show', 'a5fa2013:'+broad_path], cwd=ROOT))
    broad = load(ROOT/broad_path)
    entries = broad['pools'][0]['entries']
    assert len(entries) == 268 and entries[:240] == old_broad['pools'][0]['entries']
    names = [e['name'] for e in entries]
    assert len(set(names)) == 268
    models = ROOT/'resourcepack_template'
    def asset(relative):
        # Applied author assets remain authoritative; the template contains additions only.
        return gen.RP/relative if (gen.RP/relative).exists() else models/relative
    for row, definition in zip(rows, extra):
        name = row['id'].removesuffix('_grimoire')
        assert row['spellId'] == definition['spellId']
        leaf = load(ROOT/f"src/main/resources/data/lbc/loot_table/ported/{row['id']}.json")
        entry = leaf['pools'][0]['entries'][0]
        assert entry == entries[names.index('skywars2:'+row['id'])]
        assert entry['modifier'][-1]['tag'] == definition['data']
        assert definition['model'] == 'lbc:spelllib/'+name
        components = {}
        for fn in load(ROOT/f"src/main/resources/data/lbc/loot_table/ported/{row['wand']}.json")['pools'][0]['entries'][0]['modifier']:
            if fn['type'] == 'minecraft:set_components':
                components.update(fn['components'])
        assert row['tier'] == definition['tooltip'] == components['minecraft:tooltip_style']
        item = load(asset(f'assets/lbc/items/spelllib/{name}.json'))
        model = load(asset(f'assets/lbc/models/item/spelllib/{name}.json'))
        assert item['model']['model'] == 'lbc:item/spelllib/'+name
        assert model['textures']['layer0'] == 'lbc:item/spelllib/'+name
        assert asset(f'assets/lbc/textures/item/spelllib/{name}.png').read_bytes().startswith(b'\x89PNG\r\n\x1a\n')
    for lang in ('ru_ru','en_us','rpr'):
        key_file=models/f'updates/extra_grimoires.{lang}.json'
        keys = load(key_file) if key_file.exists() else {}
        author = load(gen.RP/f'assets/minecraft/lang/{lang}.json')
        assert not set(keys).intersection(author)
        assert all(value and '?' not in value for value in keys.values())
        combined = author | keys
        for row in rows:
            key = 'libspell_'+row['id'].removesuffix('_grimoire')
            assert key in combined and key+'.lore' in combined
    adv_path = 'src/main/resources/data/lbc/advancement/true_advancements/legends/all_grimoires.json'
    old = json.loads(subprocess.check_output(['git','show','a5fa2013:'+adv_path], cwd=ROOT))
    adv = load(ROOT/adv_path)
    assert len(adv['criteria']) == 268 and 'spell_189' not in adv['criteria']
    for key, criterion in old['criteria'].items():
        if key != 'spell_189':
            assert adv['criteria'][key] == criterion
    assert {int(key.removeprefix('spell_')) for key in adv['criteria']} == set(range(1,280))-{189,242,*range(262,271)}
    for name in gen.RETIRED:
        assert 'skywars2:'+name+'_grimoire' not in names
        assert not (ROOT/f'src/main/resources/data/lbc/loot_table/ported/{name}_grimoire.json').exists()
        for folder,suffix in [('items','.json'),('models/item','.json'),('textures/item','.png')]:
            assert not (models/f'assets/lbc/{folder}/spelllib/{name}{suffix}').exists()
        for lang in ('ru_ru','en_us','rpr'):
            key_file=models/f'updates/extra_grimoires.{lang}.json'
            assert all(not key.startswith('libspell_'+name+'.') and key!='libspell_'+name for key in (load(key_file) if key_file.exists() else {}))
    outputs = [ROOT/broad_path, ROOT/adv_path, ROOT/'src/main/resources/skywars2/remaining_grimoires.json']
    outputs += [p for p in models.rglob('*') if p.is_file() and ('extra_grimoires.' in p.name or '/spelllib/' in p.as_posix())]
    before = {p: digest(p) for p in outputs}
    gen.generate()
    assert all(digest(p) == value for p, value in before.items()), 'Generator must be byte-idempotent'
    print('PASS28 books,268 unchanged-prefix library entries/attainable criteria, exact wand tiers,84 RP assets and168 keys resolved across author/template, all9 summon books absent and idempotent generation')


if __name__ == '__main__':
    check()
