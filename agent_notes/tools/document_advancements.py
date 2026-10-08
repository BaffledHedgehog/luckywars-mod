"""Render every LBC advancement as a trigger contract, not an item effect claim."""
import json
import re
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
NOTES = ROOT / 'agent_notes'
TRIGGERS = {
    'consume_item': 'игрок заканчивает употребление предмета',
    'inventory_changed': 'меняется инвентарь игрока',
    'impossible': 'автоматического события нет; требуется выдача advancement командами',
    'player_killed_entity': 'игрок убивает сущность',
    'entity_killed_player': 'сущность убивает игрока',
    'player_hurt_entity': 'игрок наносит урон сущности',
    'entity_hurt_player': 'игрок получает урон от сущности',
    'using_item': 'игрок продолжает использовать предмет',
    'effects_changed': 'меняются эффекты игрока',
    'player_interacted_with_entity': 'игрок взаимодействует с сущностью',
    'item_durability_changed': 'меняется повреждение предмета',
    'tick': 'проверка происходит каждый игровой тик',
    'placed_block': 'игрок ставит блок',
    'used_totem': 'игрок спасается тотемом',
    'location': 'проверяется местоположение игрока',
    'shot_crossbow': 'игрок стреляет из арбалета',
    'player_generates_container_loot': 'игрок впервые генерирует лут контейнера',
    'fall_from_height': 'игрок завершает падение с высоты',
    'changed_dimension': 'игрок меняет измерение',
    'item_used_on_block': 'игрок использует предмет на блоке',
}
FIELD = {'item': 'предмет', 'items': 'предметы', 'count': 'количество',
         'player': 'игрок', 'entity': 'сущность', 'damage': 'урон',
         'location': 'место', 'effects': 'эффекты', 'distance': 'расстояние',
         'to': 'новое измерение', 'from': 'предыдущее измерение',
         'loot_table': 'таблица лута', 'durability': 'прочность', 'delta': 'изменение'}


def compact(value):
    return json.dumps(value, ensure_ascii=False, separators=(',', ':'))


def code(value):
    value = value if isinstance(value, str) else compact(value)
    return '`' + value.replace('`', '\\`').replace('\n', ' ') + '`'


def text_component(value):
    if isinstance(value, str):
        try:
            parsed = json.loads(value)
            if parsed != value:
                return text_component(parsed)
        except ValueError:
            pass
        return value
    if isinstance(value, list):
        return ''.join(text_component(v) for v in value)
    if isinstance(value, dict):
        return str(value.get('text', value.get('translate', ''))) + text_component(value.get('extra', []))
    return str(value)


def predicate(value):
    if isinstance(value, list):
        return ' И '.join('(' + predicate(v) + ')' for v in value) or 'нет ограничений'
    if not isinstance(value, dict):
        return code(value)
    kind = value.get('condition', '').split(':')[-1]
    if kind == 'inverted':
        return 'НЕ (' + predicate(value['term']) + ')'
    if kind in ('all_of', 'any_of'):
        sep = ' И ' if kind == 'all_of' else ' ИЛИ '
        return sep.join('(' + predicate(v) + ')' for v in value.get('terms', []))
    if kind == 'entity_properties':
        return 'свойства ' + code(value.get('entity', 'this')) + ': ' + code(value.get('predicate', {}))
    if kind == 'match_tool':
        return 'использованный предмет соответствует ' + code(value.get('predicate', {}))
    if kind == 'value_check':
        return 'значение ' + code(value.get('value')) + ' входит в ' + code(value.get('range'))
    if kind == 'reference':
        return 'должен выполниться predicate ' + code(value.get('name'))
    if kind == 'random_chance':
        return 'случайная проверка с вероятностью ' + code(value.get('chance'))
    if kind:
        return 'условие ' + code(kind) + ' с параметрами ' + code({k: v for k, v in value.items() if k != 'condition'})
    return code(value)


def conditions(value):
    return '; '.join(FIELD.get(k, k) + ': ' + predicate(v) for k, v in value.items()) or 'дополнительных ограничений нет'


def main():
    inventory = json.loads((NOTES / 'inventory/files.json').read_text(encoding='utf-8'))
    jobs = json.loads((NOTES / 'inventory/research_jobs.json').read_text(encoding='utf-8'))
    refs = defaultdict(list)
    functions = {}
    for row in inventory:
        if row['suffix'] != '.mcfunction':
            continue
        source = row['source']
        if row.get('resource_id'):
            functions[row['resource_id']] = source
        for number, line in enumerate((ROOT / source).read_text(encoding='utf-8-sig').splitlines(), 1):
            if line.lstrip().startswith('#'):
                continue
            for match in re.finditer(r'\badvancement (grant|revoke) (.+?) (only|from|through|until) ([\w.\-]+:[\w./\-]+)', line):
                refs[match.group(4)].append({'source': source, 'line': number, 'command': line.strip()})
    doc = 'agent_notes/lbc_advancements.md'
    lines = ['# События предметов и достижения LBC', '',
             'Эта справка описывает все 651 advancement JSON и пять вспомогательных файлов: событие, фильтры, условия завершения и награду. '
             'Фактический эффект наградной функции описывается в соответствующем разделе LBC. '
             'Ссылка на функцию позволяет проверить его отдельно; название достижения не доказывает эффект.', '',
             'Каждый критерий сохраняется у игрока. При отсутствии `requirements` нужны все критерии; '
             'при наличии массива нужны все его группы, а внутри каждой группы достаточно одного критерия. '
             'Награда срабатывает при завершении advancement. Повторное срабатывание требует его отзыва '
             '(обычно через `advancement revoke`); ниже перечислены найденные буквальные grant/revoke, '
             'но динамически составленные ID могут добавлять вызовы.', '',
             'Фильтры приведены без потери чисел и ключей: `custom_data` — идентификатор предмета, '
             '`equipment.mainhand` — предмет в основной руке, `playerGameType:3` — зритель. '
             'НЕ/И/ИЛИ показывают вложенную логику. Ограничение измерения применяется только там, '
             'где оно действительно записано внутри `conditions`.', '']
    coverage, details, unknown = [], {}, set()
    for source in jobs['lbc_advancements']['sources']:
        if not source.endswith('.json'):
            heading = code(source.split('/advancement/', 1)[-1])
            lines.extend(['## ' + heading, '', f'Источник: [файл](../{source}).'])
            if source.endswith('file_list.txt'):
                lines.append('Сохранённый список имён файлов без расширений. Minecraft этот TXT не исполняет и не использует как registry; список может устареть.')
            elif source.endswith('namecollect.py'):
                lines.append('Вспомогательный Python-скрипт: при ручном запуске перечисляет обычные файлы текущего рабочего каталога, убирает расширения и перезаписывает `file_list.txt`. Подпапки не обходит; сам Minecraft его не запускает.')
            else:
                lines.append('Ручной генератор JSON таблеток: проходит активный список эффектов по порядку, начиная с числового `pill:54`, и перезаписывает `pill_<имя>.json` в текущем рабочем каталоге. Создаёт событие употребления предмета с нужным custom_data, запрещает его в `minecraft:nexus` и вызывает `lbc:other/attributes/pills/<имя>`. Комментированный список атрибутов не выполняется. Итоговые JSON описаны отдельно; скрипт не запускается Minecraft автоматически.')
            lines.append('')
            coverage.append({'source': source, 'document': doc, 'section': heading, 'status': 'non_runtime', 'note': 'Auxiliary file behavior described; not a Minecraft resource.'})
            continue
        value = json.loads((ROOT / source).read_text(encoding='utf-8-sig'))
        rid = 'lbc:' + source.split('/advancement/', 1)[1][:-5]
        heading = code(rid)
        lines.extend(['## ' + heading, '', f'Источник: [{rid}](../{source}).'])
        display = value.get('display')
        if display:
            title = text_component(display.get('title', ''))
            description = text_component(display.get('description', ''))
            lines.append('В дереве достижений: ' + code(title) + '; описание: ' + code(description) + '. '
                         + 'Параметры показа: ' + code({k: v for k, v in display.items() if k not in ('title', 'description')}) + '.')
        if value.get('parent'):
            lines.append('Родитель в дереве: ' + code(value['parent']) + '; это связь отображения, а не дополнительный критерий.')
        criteria = value.get('criteria', {})
        for key, criterion in criteria.items():
            kind = criterion.get('trigger', '').split(':')[-1]
            if kind not in TRIGGERS:
                unknown.add(kind)
            sentence = TRIGGERS.get(kind, 'событие ' + code(criterion.get('trigger')))
            lines.append('Критерий ' + code(key) + ': ' + sentence + '. Условия: ' + conditions(criterion.get('conditions', {})) + '.')
            extra = {k: v for k, v in criterion.items() if k not in ('trigger', 'conditions')}
            if extra:
                lines.append('Вне `conditions` записано ' + code(extra) + '. Это не обычные поля фильтра события; '
                             'не считаем их доказанным ограничением срабатывания.')
        if 'requirements' in value:
            lines.append('Для завершения: ' + code(value['requirements']) + ' (И между группами, ИЛИ внутри группы).')
        elif len(criteria) > 1:
            lines.append('Для завершения требуются все ' + str(len(criteria)) + ' критериев.')
        rewards = value.get('rewards', {})
        if rewards:
            parts = []
            for key, val in rewards.items():
                if key == 'function' and val in functions:
                    parts.append('выполнить [' + val + '](../' + functions[val] + ') от лица игрока')
                else:
                    parts.append({'experience': 'выдать опыт', 'loot': 'выдать лут из таблиц', 'recipes': 'открыть рецепты',
                                  'function': 'вызвать функцию'}.get(key, key) + ': ' + code(val))
            lines.append('Награда: ' + '; '.join(parts) + '.')
        else:
            lines.append('Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.')
        calls = refs.get(rid, [])
        if calls:
            lines.append('Буквальные команды выдачи/отзыва: ' + '; '.join('[' + c['source'].split('/function/', 1)[-1] + ':' + str(c['line']) + '](../' + c['source'] + ') — ' + code(c['command']) for c in calls) + '.')
        else:
            lines.append('Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.')
        extra = {k: v for k, v in value.items() if k not in ('parent', 'display', 'criteria', 'requirements', 'rewards')}
        if extra:
            lines.append('Дополнительные параметры ресурса: ' + code(extra) + '.')
        lines.append('')
        coverage.append({'source': source, 'document': doc, 'section': heading, 'status': 'documented',
                         'note': 'Trigger, all conditions, requirements, display and rewards rendered; literal grant/revoke callers listed. Reward function effects have separate owners.'})
        details[rid] = {'source': source, 'definition': value, 'literal_commands': calls}
    (ROOT / doc).write_text('\n'.join(lines) + '\n', encoding='utf-8')
    (NOTES / 'inventory/lbc_advancement_contracts.json').write_text(json.dumps(details, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    (NOTES / 'coverage/lbc_advancements.json').write_text(json.dumps(coverage, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({'sources': len(coverage), 'unknown_triggers': sorted(unknown), 'document_bytes': (ROOT / doc).stat().st_size}))


if __name__ == '__main__':
    main()
