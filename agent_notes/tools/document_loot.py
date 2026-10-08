"""Exhaustive readable loot contracts. Runtime activation remains in function docs."""
import json
import re
from collections import defaultdict
from pathlib import Path

from document_advancements import code, compact, conditions, predicate, text_component

ROOT = Path(__file__).resolve().parents[2]
NOTES = ROOT / 'agent_notes'
LABELS = {
    'custom_data': 'метки распознавания', 'item_model': 'модель предмета',
    'tooltip_style': 'оформление подсказки', 'max_stack_size': 'максимальный стак',
    'max_damage': 'максимальная прочность', 'damage': 'накопленное повреждение',
    'unbreakable': 'неразрушимость', 'food': 'питательность и насыщение',
    'consumable': 'время/анимация употребления и действия при употреблении',
    'use_effects': 'скорость/спринт во время использования',
    'enchantments': 'зачарования', 'stored_enchantments': 'зачарования книги',
    'attribute_modifiers': 'модификаторы атрибутов и активные слоты',
    'equippable': 'слот/модель экипировки и камера', 'glider': 'возможность планирования',
    'potion_contents': 'зелье и эффекты', 'potion_duration_scale': 'множитель длительности зелья',
    'suspicious_stew_effects': 'эффекты подозрительного рагу (duration в тиках)',
    'entity_data': 'данные создаваемой сущности', 'charged_projectiles': 'заряженные снаряды',
    'instrument': 'звук инструмента', 'fireworks': 'полёт/взрывы фейерверка',
    'block_state': 'состояния размещаемого блока', 'container_loot': 'таблица лута контейнера',
    'written_book_content': 'содержимое подписанной книги',
    'ominous_bottle_amplifier': 'уровень эффекта зловещей бутылки',
    'tool': 'скорость добычи, правила блоков и износ',
    'base_color': 'основной цвет', 'banner_patterns': 'узоры знамени',
    'dyed_color': 'цвет окраски', 'tooltip_display': 'скрытые компоненты подсказки',
    'enchantment_glint_override': 'принудительный блеск',
    'custom_name': 'отображаемое имя', 'item_name': 'имя типа предмета', 'lore': 'описание в подсказке',
}


def function_description(value, unknown):
    kind = value.get('function', '').split(':')[-1]
    params = {k: v for k, v in value.items() if k not in ('function', 'conditions')}
    if kind == 'set_count':
        out = ('прибавить количество ' if value.get('add', False) else 'задать количество ') + code(value.get('count'))
    elif kind == 'set_custom_data':
        out = 'добавить/перезаписать метки custom_data ' + code(value.get('tag'))
    elif kind == 'set_components':
        parts = []
        for key, val in value.get('components', {}).items():
            base = key.lstrip('!').split(':')[-1]
            label = LABELS.get(base, base)
            if base not in LABELS:
                unknown['components'].add(base)
            if key.startswith('!'):
                parts.append('удалить компонент ' + code(key.lstrip('!')))
            elif base in ('custom_name', 'item_name'):
                parts.append(label + ' ' + code(text_component(val)) + ' (компонент ' + code(val) + ')')
            elif base == 'lore':
                parts.append(label + ': ' + code(text_component(val)) + ' (компонент ' + code(val) + ')')
            else:
                parts.append(label + ': ' + code(val))
        out = '; '.join(parts) or 'не менять компоненты (пустой список)'
        extra = {k: v for k, v in params.items() if k != 'components'}
        if extra:
            out += '; дополнительные параметры ' + code(extra)
    elif kind == 'set_name':
        out = 'задать имя ' + code(text_component(value.get('name'))) + '; параметры ' + code(params)
    elif kind == 'set_lore':
        out = 'задать строки описания ' + code(text_component(value.get('lore', []))) + '; параметры ' + code(params)
    elif kind == 'set_attributes':
        out = 'задать модификаторы атрибутов ' + code(params)
    elif kind == 'set_enchantments':
        out = ('добавить уровни зачарований ' if value.get('add', False) else 'задать уровни зачарований ') + code(value.get('enchantments', {}))
    elif kind == 'enchant_with_levels':
        out = 'случайно зачаровать предмет: ' + code(params)
    elif kind == 'set_damage':
        out = 'задать оставшуюся долю прочности (это не количество потерянных единиц): ' + code(params)
    elif kind == 'limit_count':
        out = 'ограничить количество предметов: ' + code(params)
    elif kind == 'toggle_tooltips':
        out = 'показать/скрыть разделы подсказки: ' + code(params)
    elif kind == 'apply_bonus':
        out = 'изменить количество по зачарованию и формуле: ' + code(params)
    elif kind == 'explosion_decay':
        out = 'проверить выживание каждой единицы стека после взрыва; параметры ' + code(params)
    elif kind == 'copy_components':
        out = 'скопировать компоненты указанного источника: ' + code(params)
    elif kind == 'fill_player_head':
        out = 'заполнить профиль головы из контекста: ' + code(params)
    else:
        unknown['functions'].add(kind)
        out = 'функция ' + code(kind) + ' с параметрами ' + code(params)
    if value.get('conditions'):
        out = 'только при ' + predicate(value['conditions']) + ': ' + out
    return out


def provider(value):
    if not isinstance(value, dict):
        return code(value)
    kind = value.get('type', '').split(':')[-1]
    if kind == 'constant':
        return code(value.get('value'))
    if kind == 'uniform':
        return 'случайное значение от ' + code(value.get('min')) + ' до ' + code(value.get('max'))
    if kind == 'binomial':
        return 'число успехов в ' + code(value.get('n')) + ' проверках с вероятностью ' + code(value.get('p'))
    return code(value)


def describe_entry(entry, path, lines, unknown):
    kind = entry.get('type', '').split(':')[-1]
    selection = 'Вес ' + code(entry.get('weight', 1)) + ', добавка за luck ' + code(entry.get('quality', 0)) + '.'
    if kind == 'item':
        result = 'Выдать ванильную основу ' + code(entry.get('name')) + '. ' + selection
    elif kind == 'loot_table':
        result = 'Вызвать вложенную loot table ' + code(entry.get('value', entry.get('name'))) + '. ' + selection
    elif kind == 'empty':
        result = 'Выбранная запись ничего не выдаёт. ' + selection
    elif kind == 'tag':
        result = 'Выдать предметы из тега ' + code(entry.get('name')) + '; expand=' + code(entry.get('expand', False)) + '. ' + selection
    elif kind == 'alternatives':
        result = 'Проверять дочерние записи по порядку и использовать первую прошедшую условия.'
    elif kind == 'sequence':
        result = 'Обрабатывать дочерние записи по порядку; остановиться при первой не прошедшей условия.'
    elif kind == 'group':
        result = 'Обрабатывать все дочерние записи как одну группу раскрытия.'
    elif kind == 'dynamic':
        result = 'Получить динамический дроп контекста ' + code(entry.get('name')) + '. ' + selection
    else:
        unknown['entries'].add(kind)
        result = 'Тип записи ' + code(kind) + ': ' + code(entry)
    lines.append('- ' + path + ': ' + result)
    if entry.get('conditions'):
        lines.append('  Условия записи: ' + predicate(entry['conditions']) + '.')
    for index, func in enumerate(entry.get('functions', []), 1):
        lines.append('  Изменение ' + str(index) + ': ' + function_description(func, unknown) + '.')
    for i, child in enumerate(entry.get('children', []), 1):
        describe_entry(child, path + '.' + str(i), lines, unknown)
    extra = {k: v for k, v in entry.items() if k not in ('type', 'name', 'value', 'weight', 'quality', 'conditions', 'functions', 'children', 'expand')}
    if extra:
        lines.append('  Дополнительные поля: ' + code(extra) + '.')


def main():
    inventory = json.loads((NOTES / 'inventory/files.json').read_text(encoding='utf-8'))
    jobs = json.loads((NOTES / 'inventory/research_jobs.json').read_text(encoding='utf-8'))
    refs = defaultdict(set)
    for row in inventory:
        if row['suffix'] not in ('.mcfunction', '.json'):
            continue
        text = (ROOT / row['source']).read_text(encoding='utf-8-sig')
        if row['suffix'] == '.mcfunction':
            text = '\n'.join(line for line in text.splitlines() if not line.lstrip().startswith('#'))
        for rid in set(re.findall(r'(?<![\w./\-])lbc:[\w./\-]+', text)):
            refs[rid].add(row['source'])
    all_details = {}
    unknown = {'entries': set(), 'functions': set(), 'components': set()}
    for job in ['lbc_loot_definitions', 'lbc_loot_uncrafted']:
        doc = 'agent_notes/' + job + '.md'
        title = 'Выдача предметов и случайные таблицы LBC' if job == 'lbc_loot_definitions' else 'Выходы разборки предметов LBC'
        lines = ['# ' + title, '',
                 'Для каждой таблицы ниже указаны все пулы, число выборов, основания предметов, веса, условия и изменения компонентов. '
                 'Это поведение выдачи: способ активации предмета ищите в описании соответствующей функции. '
                 'Название/lore записано как текст подсказки и само по себе не исполняет способность.', '',
                 'При генерации обрабатываются все пулы. В обычном пуле один выбор берёт одну прошедшую условия запись по весу; '
                 '`rolls` задаёт число выборов, `bonus_rolls` — добавку с учётом luck. '
                 'Вес и quality — исходные параметры, а не конечный процент: вложенные таблицы, условия, luck и число выборов меняют результат. '
                 'Изменения применяются по порядку: запись, затем её пул, затем таблица. '
                 'Изначальный item-стек имеет одну единицу; явный `set_count` меняет количество, а `add:true` прибавляет. '
                 'Компоненты, не изменённые таблицей, сохраняют defaults ванильной основы.', '',
                 'Все имена ресурсов и численные параметры сохранены. Вложенные таблицы и команды, содержащие тот же ID, '
                 'указаны ссылками; список буквальных ссылок не исключает макросы и ссылки внутри NBT. '
                 'NBT-контейнеры изучены в `maps_catalog.md`.', '']
        coverage = []
        for source in jobs[job]['sources']:
            tail = source.split('/loot_table/', 1)[-1]
            rid = 'lbc:' + tail[:-5] if source.endswith('.json') else tail
            heading = code(rid)
            lines.extend(['## ' + heading, '', 'Источник: [' + rid + '](../' + source + ').'])
            if not source.endswith('.json'):
                if source.endswith('tagscraper.py'):
                    lines.append('Ручной Python-скрипт рекурсивно читает JSON текущего каталога и подпапок, собирает custom_data и set_custom_data, сортирует уникальные метки и перезаписывает `custom_data_list.txt`. Ошибки чтения JSON пропускает. Regex для SNBT извлекает только простые true/false/целые числа, поэтому список не является полным SNBT-парсером. Minecraft скрипт не запускает.')
                elif source.endswith('custom_data_list.txt'):
                    lines.append('Сохранённый текстовый список custom_data, полученный вспомогательным скриптом. Это справочный снимок, не обработчик и не автоматически читаемый Minecraft ресурс.')
                else:
                    lines.append('Черновик команд для разборки жёлтых предметов, warped-вариантов и waxed copper с заглушкой `ffffff`. При копировании в функцию команды выдавали бы соответствующие uncrafted-таблицы для подходящего Item.id. Расширение `.tmp` не загружается как `.mcfunction`, поэтому этот файл сам не выполняется.')
                status = 'non_runtime'
            else:
                try:
                    value = json.loads((ROOT / source).read_text(encoding='utf-8-sig'))
                except ValueError:
                    lines.append('Файл пустой и не является допустимым loot-table JSON. Описать выдаваемый им дроп невозможно: таблица не задаёт результат. Связанные вызовы требуют отсутствующих данных; это текущий дефект исходников, а не подтверждённый нулевой дроп.')
                    status = 'documented'
                else:
                    if value.get('type'):
                        lines.append('Набор параметров контекста: ' + code(value['type']) + '.')
                    if value.get('random_sequence'):
                        lines.append('Именованная последовательность случайных чисел: ' + code(value['random_sequence']) + '.')
                    for i, pool in enumerate(value.get('pools', []), 1):
                        lines.append('Пул ' + str(i) + ': выборов ' + provider(pool.get('rolls', 1)) + '; bonus_rolls ' + provider(pool.get('bonus_rolls', 0)) + '.')
                        if pool.get('conditions'):
                            lines.append('Условия пула: ' + predicate(pool['conditions']) + '.')
                        lines.append('')
                        for k, entry in enumerate(pool.get('entries', []), 1):
                            describe_entry(entry, str(k), lines, unknown)
                        for k, func in enumerate(pool.get('functions', []), 1):
                            lines.append('Изменение пула ' + str(k) + ': ' + function_description(func, unknown) + '.')
                        extra = {k: v for k, v in pool.items() if k not in ('rolls', 'bonus_rolls', 'conditions', 'entries', 'functions')}
                        if extra:
                            lines.append('Дополнительные поля пула: ' + code(extra) + '.')
                    for k, func in enumerate(value.get('functions', []), 1):
                        lines.append('Изменение таблицы ' + str(k) + ': ' + function_description(func, unknown) + '.')
                    extra = {k: v for k, v in value.items() if k not in ('pools', 'functions', 'type', 'random_sequence')}
                    if extra:
                        lines.append('Дополнительные поля таблицы: ' + code(extra) + '.')
                    if not value.get('pools'):
                        lines.append('Пулов выдачи в таблице нет.')
                    all_details[rid] = {'source': source, 'definition': value}
                    status = 'documented'
                related = sorted(refs[rid] - {source})
                if related:
                    lines.append('')
                    lines.append('Буквальные ссылки на ID: ' + ', '.join('[' + p.split('/data/', 1)[-1] + '](../' + p + ')' for p in related) + '.')
                else:
                    lines.append('Буквальных ссылок на этот ID в исследуемых JSON/функциях не найдено; таблицу можно вызвать вручную.')
            lines.append('')
            coverage.append({'source': source, 'document': doc, 'section': heading, 'status': status,
                             'note': 'All pools, entries, conditions, ordered modifications and resource fields rendered; activation is described by function owners.'})
        (ROOT / doc).write_text('\n'.join(lines) + '\n', encoding='utf-8')
        (NOTES / 'coverage' / (job + '.json')).write_text(json.dumps(coverage, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
        print(json.dumps({'job': job, 'sources': len(coverage), 'bytes': (ROOT / doc).stat().st_size}))
    (NOTES / 'inventory/lbc_loot_contracts.json').write_text(json.dumps(all_details, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({'unknown': {k: sorted(v) for k, v in unknown.items()}}))


if __name__ == '__main__':
    main()
