"""Index manually authored mana prose without generating or replacing it."""
from pathlib import Path
import json
import re

ROOT = Path(__file__).resolve().parents[2]
NOTES = ROOT/'agent_notes'
OTHER = ROOT/'world/datapacksdisabled/LBC/data/lbc/function/other'


def main():
    document = NOTES/'lbc_mana.md'
    text = document.read_text(encoding='utf8')
    headings = re.findall(r'^## (.+)$', text, re.M)
    sources = sorted(OTHER.glob('manaregen*.mcfunction')) + sorted(OTHER.glob('negative_mana*.mcfunction'))
    sources += sorted((OTHER/'negative_mana').glob('*.mcfunction'))
    sources += [OTHER.parent/'damage_with_tmp_void.mcfunction', OTHER/'speed_up.mcfunction',
                OTHER/'overload_crux/give.mcfunction', OTHER/'mana_potions/bad.mcfunction', OTHER/'mana_potions/select.mcfunction']
    rows = []
    for path in sources:
        assert path.is_file(), path
        if path.name == 'manaregen.mcfunction' or '/overload_crux/' in path.as_posix():
            section = headings[0]
        elif path.name.startswith('manaregen') or path.name == 'speed_up.mcfunction':
            section = headings[1]
        elif path.name in ('negative_mana_5.mcfunction', 'negative_mana_5_helmet.mcfunction', 'damage_with_tmp_void.mcfunction', 'explode.mcfunction'):
            section = headings[3]
        elif '/negative_mana/' in path.as_posix():
            section = headings[4]
        elif '/mana_potions/' in path.as_posix():
            section = headings[5]
        else:
            section = headings[2]
        rows.append(dict(source=path.relative_to(ROOT).as_posix(), document='agent_notes/lbc_mana.md', section=section,
                         status='documented', note='Manually read source and linked mana loop, thresholds, numeric transforms, projectile recursion and caller guards.'))
    assert len(rows) == 33
    (NOTES/'coverage/root_mana_manual.json').write_text(json.dumps(rows, ensure_ascii=False, indent=2)+'\n', encoding='utf8')
    marker = '\n## Проверенные исходники\n'
    text = text.split(marker)[0].split('\n## Sources reviewed\n')[0]
    text += marker+'\n| Источник | Раздел |\n|---|---|\n'
    for row in rows:
        text += '| ['+row['source'].split('/function/', 1)[1]+'](../'+row['source']+') | '+row['section']+' |\n'
    document.write_text(text, encoding='utf8')
    print(json.dumps({'manually_indexed_sources': len(rows)}))


if __name__ == '__main__':
    main()
