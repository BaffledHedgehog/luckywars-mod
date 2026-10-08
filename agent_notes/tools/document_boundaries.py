"""Human-readable external/missing literal function boundaries."""
import json
from collections import defaultdict
from pathlib import Path

from document_advancements import code

ROOT = Path(__file__).resolve().parents[2]
NOTES = ROOT / 'agent_notes'


def main():
    audit = json.loads((NOTES / 'inventory/resource_reference_audit.json').read_text(encoding='utf-8'))
    lines = ['# Границы набора datapack', '',
             'Ниже перечислены точные функции, которые вызываются исполняемыми строками или advancement rewards, '
             'но не имеют определения в LBC, maps и swarogskywars. Это не доказывает отсутствие ресурса '
             'во всех пакетах работающего сервера. Дальнейшее поведение нельзя восстанавливать по одному имени.', '',
             'Команды вызывающей функции до/после такого вызова описываются по имеющимся исходникам. '
             'Не заполняем отсутствующие обработчики предполагаемой реализацией. Макросы рассматриваются отдельно: '
             'конкретный ID определяется переданными данными.', '',
             'Проверка использует токены команды, сохраняя JSON/SNBT/селекторы и текстовые аргументы целиком. '
             'Слово function в lore не считается вызовом. Inline predicate/item modifier не считается отсутствующим файлом. '
             'NBT, chat click actions и автоматически составленные ID требуют отдельных исследований.', '',
             'Повторный запуск: `python -X utf8 agent_notes/tools/audit_references.py`, затем '
             '`python -X utf8 agent_notes/tools/document_boundaries.py`. Машинный отчёт: '
             '[resource_reference_audit.json](inventory/resource_reference_audit.json).', '',
             '## Отсутствующие определения функций', '',
             '| Точный ID | Известные вызывающие источники |', '|---|---|']
    for entry in audit['boundaries']:
        if entry['kind'] != 'function' or entry['status'] != 'outside_studied_packs':
            continue
        callers = sorted({c['source'] for c in entry['callers']})
        links = '; '.join('[' + p.split('/function/', 1)[-1] + '](../' + p + ')' for p in callers)
        lines.append('| ' + code(entry['target']) + ' | ' + links + ' |')
    lines.extend(['', '## Динамические вызовы', '',
                  'Само выражение не является отсутствующей функцией. Нужно проследить источник каждого '
                  'макрополя и диапазон разрешённых значений. Это делают владельцы предметов/GUI/навыков.', '',
                  '| Выражение | Исходники |', '|---|---|'])
    for entry in audit['boundaries']:
        if entry['status'] != 'dynamic_macro':
            continue
        callers = sorted({c['source'] for c in entry['callers']})
        links = '; '.join('[' + p.split('/function/', 1)[-1] + '](../' + p + ')' for p in callers)
        lines.append('| ' + code(entry['target']) + ' | ' + links + ' |')
    structures = [entry for entry in audit['boundaries'] if entry['kind'] == 'structure' and entry['status'] == 'outside_studied_packs']
    lines.extend(['', '## Шаблоны структур', '',
                  'Статический аудит команд нашёл ' + str(len(structures)) + ' разных template ID без одноимённого '
                  'NBT в трёх исследуемых пакетах. Среди них есть карты и дополнительные структуры LBC. '
                  'Пофайловое сопоставление, состав карт, подпапки и вызывающие функции находятся в '
                  '[maps_catalog.md](maps_catalog.md) и [maps_structures.md](maps_structures.md).', '',
                  'Шаблоны могут храниться отдельно, например в generated-данных мира или другом datapack. '
                  'Такое возможное происхождение не является подтверждением их наличия. '
                  'Исследование фиксирует именно границы заданного набора.', ''])
    (NOTES / 'RESOURCE_BOUNDARIES.md').write_text('\n'.join(lines), encoding='utf-8')
    print(json.dumps({'missing_function_ids': sum(e['kind'] == 'function' and e['status'] == 'outside_studied_packs' for e in audit['boundaries']),
                      'dynamic_expressions': sum(e['status'] == 'dynamic_macro' for e in audit['boundaries']),
                      'missing_template_ids': len(structures)}))


if __name__ == '__main__':
    main()
