"""Item identity lookup; never infer an ability from model/name/lore."""
import json
from collections import defaultdict
from pathlib import Path

from document_advancements import compact, text_component

ROOT = Path(__file__).resolve().parents[2]
NOTES = ROOT / 'agent_notes'


def cell(value):
    text = value if isinstance(value, str) else compact(value)
    return '`' + text.replace('|', '\\|').replace('`', '\\`').replace('\n', ' ') + '`'


def identity(entry):
    data = {'base': entry['name'], 'names': [], 'models': [], 'custom_data_writes': []}
    for function in entry.get('functions', []):
        kind = function.get('function', '').split(':')[-1]
        conditional = function.get('conditions', [])
        if kind == 'set_name':
            data['names'].append({'text': text_component(function.get('name', '')), 'conditions': conditional})
        if kind == 'set_custom_data':
            data['custom_data_writes'].append({'value': function.get('tag'), 'conditions': conditional})
        if kind == 'set_components':
            for key, value in function.get('components', {}).items():
                if key.startswith('!'):
                    continue
                component = key.split(':')[-1]
                if component in ('item_name', 'custom_name'):
                    data['names'].append({'text': text_component(value), 'conditions': conditional})
                elif component == 'item_model':
                    data['models'].append({'id': value, 'conditions': conditional})
                elif component == 'custom_data':
                    data['custom_data_writes'].append({'value': value, 'conditions': conditional})
    return data


def main():
    loot = json.loads((NOTES / 'inventory/loot_item_index.json').read_text(encoding='utf-8'))
    groups = {}
    for table in loot:
        if table['pack'] != 'LBC':
            continue
        for entry in table['entries']:
            if entry['type'] != 'item':
                continue
            item = identity(entry)
            if not item['names'] and not item['models'] and not item['custom_data_writes']:
                continue
            key = compact(item)
            group = groups.setdefault(key, dict(item, occurrences=[]))
            group['occurrences'].append({'table': table['id'], 'source': table['source'], 'pointer': entry['pointer'],
                                         'conditions': entry.get('conditions', []), 'functions': entry.get('functions', [])})
    indexed = sorted(groups.values(), key=lambda value: (value['names'][0]['text'] if value['names'] else value['base'], compact(value)))
    for i, entry in enumerate(indexed, 1):
        entry['lookup_id'] = i
    (NOTES / 'inventory/item_lookup.json').write_text(json.dumps(indexed, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    lines = ['# Указатель кастомных предметов LBC', '',
             'Ищите по имени/ключу перевода, ванильной основе, item_model или точному custom_data. '
             'Имя и модель — признаки оформления; способность описывается в тематических `lbc_*.md`. '
             'Полная выдача, количество, условия, зачарования и компоненты находятся в '
             '[lbc_loot_definitions.md](lbc_loot_definitions.md).', '',
             'Строки объединены только по одинаковой последовательности записей имени, модели и custom_data, '
             'а не по одинаковому поведению. Другие компоненты и условия у этих выдач могут различаться. '
             'Условные записи и точные JSON pointers сохранены в [item_lookup.json](inventory/item_lookup.json). '
             'Предметы, создаваемые только командами summon/item и не встречающиеся в этих loot tables, '
             'в этот указатель не входят: они остаются в описаниях функций.', '',
             '## Поиск предмета', '',
             '| Имя/ключ перевода | Основа | Модель | Записи custom_data | Таблицы выдачи |',
             '|---|---|---|---|---|']
    for item in indexed:
        names = ', '.join(cell(n['text']) + (' (условно)' if n['conditions'] else '') for n in item['names']) or '—'
        models = ', '.join(cell(n['id']) + (' (условно)' if n['conditions'] else '') for n in item['models']) or '—'
        flags = '; '.join(cell(n['value']) + (' (условно)' if n['conditions'] else '') for n in item['custom_data_writes']) or '—'
        refs = {(occ['table'], occ['source']) for occ in item['occurrences']}
        links = ', '.join('[' + rid + '](../' + source + ')' for rid, source in sorted(refs))
        lines.append('| ' + names + ' | ' + cell(item['base']) + ' | ' + models + ' | ' + flags + ' | ' + links + ' |')
    (NOTES / 'ITEM_LOOKUP.md').write_text('\n'.join(lines) + '\n', encoding='utf-8')
    print(json.dumps({'identity_groups': len(indexed), 'loot_occurrences': sum(len(item['occurrences']) for item in indexed),
                      'scope': 'LBC item entries with custom name/model/data; not a complete behavior inventory.'}))


if __name__ == '__main__':
    main()
