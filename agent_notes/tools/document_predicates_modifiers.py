"""Render exact predicate and item mutation contracts with their literal callers."""
import json
import re
from collections import defaultdict
from pathlib import Path

from document_advancements import code, predicate
from document_loot import function_description

ROOT = Path(__file__).resolve().parents[2]
NOTES = ROOT / 'agent_notes'


def main():
    rows = json.loads((NOTES / 'inventory/files.json').read_text(encoding='utf-8'))
    jobs = json.loads((NOTES / 'inventory/research_jobs.json').read_text(encoding='utf-8'))
    references = defaultdict(set)
    for row in rows:
        if row['suffix'] not in ('.mcfunction', '.json'):
            continue
        text = (ROOT / row['source']).read_text(encoding='utf-8-sig')
        if row['suffix'] == '.mcfunction':
            text = '\n'.join(line for line in text.splitlines() if not line.lstrip().startswith('#'))
        for rid in set(re.findall(r'(?<![\w./\-])lbc:[\w./\-]+', text)):
            references[rid].add(row['source'])
    doc = 'agent_notes/lbc_predicates_modifiers.md'
    lines = ['# Проверки и изменения предметов LBC', '',
             'Predicate возвращает да/нет для контекста вызова; он сам не применяет эффект. '
             'Item modifier меняет уже существующий предмет в слоте, выбранном вызывающей командой. '
             'Если modifier представляет массив, изменения выполняются по порядку. '
             'Условие функции проверяется перед её применением. Ниже сохранены все числа, слоты, метки и параметры.', '',
             'Ссылки показывают буквальное использование ID в исследуемых функциях и JSON. '
             'Одинаковое имя в разных registry может встречаться и для другого ресурса; '
             'точный слот/контекст следует из команды в источнике. Сгенерированные макросами ссылки отдельно исследуются владельцами функций.', '']
    coverage = []
    unknown = {'entries': set(), 'functions': set(), 'components': set()}
    for source in jobs['lbc_predicates_modifiers']['sources']:
        is_predicate = '/predicate/' in source
        kind = 'predicate' if is_predicate else 'item_modifier'
        rid = 'lbc:' + source.split('/' + kind + '/', 1)[-1][:-5]
        heading = kind + ' ' + code(rid)
        value = json.loads((ROOT / source).read_text(encoding='utf-8-sig'))
        lines.extend(['## ' + heading, '', 'Источник: [' + rid + '](../' + source + ').'])
        if is_predicate:
            if isinstance(value, dict) and value.get('condition', '').split(':')[-1] == 'entity_scores':
                lines.append('Проверяет значения scoreboard у ' + code(value.get('entity', 'this')) + ': ' + code(value.get('scores', {})) + '. Все указанные диапазоны должны выполняться.')
                extra = {k: v for k, v in value.items() if k not in ('condition', 'entity', 'scores')}
                if extra:
                    lines.append('Дополнительные параметры: ' + code(extra) + '.')
            elif isinstance(value, dict) and value.get('condition', '').split(':')[-1] == 'location_check':
                lines.append('Проверяет место выполнения с указанным смещением: ' + code({k: v for k, v in value.items() if k != 'condition'}) + '. Проверка не переносит игрока и не меняет блоки.')
            else:
                lines.append('Успешно только при: ' + predicate(value) + '.')
        else:
            for i, func in enumerate(value if isinstance(value, list) else [value], 1):
                lines.append('Шаг ' + str(i) + ': ' + function_description(func, unknown) + '.')
        refs = sorted(references[rid] - {source})
        if refs:
            lines.append('Буквальные ссылки на ID: ' + ', '.join('[' + s.split('/data/', 1)[-1] + '](../' + s + ')' for s in refs) + '.')
        else:
            lines.append('Буквальных ссылок на этот точный ID в исследуемых функциях/JSON не найдено; ресурс доступен при ручном или динамическом вызове.')
        lines.append('')
        coverage.append({'source': source, 'document': doc, 'section': heading, 'status': 'documented',
                         'note': 'Complete predicate condition or ordered item mutations with exact parameters and literal resource references.'})
    (ROOT / doc).write_text('\n'.join(lines) + '\n', encoding='utf-8')
    (NOTES / 'coverage/lbc_predicates_modifiers.json').write_text(json.dumps(coverage, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({'sources': len(coverage), 'unknown': {k: sorted(v) for k, v in unknown.items()}}))


if __name__ == '__main__':
    main()
