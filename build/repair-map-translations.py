from pathlib import Path
import json
import re

root = Path(__file__).resolve().parents[1]
lang = root / 'src/main/resources/assets/skywars2/lang'
fragment_path = root / 'build/map-translations.json'
fragment = json.loads(fragment_path.read_text(encoding='utf-8'))
errors = {
    'skywars2.map.error.placement': 'Не удалось разместить карту: %s',
    'skywars2.map.error.busy': 'Операция с картой уже выполняется.',
    'skywars2.map.error.dimension_missing': 'Измерение арены skywars2:arena не загружено.',
    'skywars2.map.error.journal_unreadable': 'Журнал границ арены не читается; размещение отменено, чтобы не оставить блоки предыдущей карты.',
    'skywars2.map.error.unknown': 'Неизвестная карта: %s',
    'skywars2.map.error.broken_missing': 'У карты %s нет доступного разрушенного варианта.',
    'skywars2.map.error.inspect_map': 'Не удалось проверить карту %s: %s',
    'skywars2.map.error.persist_changes': 'Не удалось сохранить последние изменения арены; размещение не начато.',
    'skywars2.map.error.save_bounds': 'Не удалось сохранить границы очистки арены; размещение не начато.',
    'skywars2.map.error.prepare_map': 'Не удалось подготовить карту %s: %s',
    'skywars2.map.error.template_missing': 'Отсутствует шаблон структуры %s.',
    'skywars2.map.error.inspect_lobby': 'Не удалось проверить структуру лобби: %s',
    'skywars2.map.error.prepare_lobby': 'Не удалось подготовить лобби: %s',
    'skywars2.map.error.shutdown': 'Загрузчик карт завершает работу.',
    'skywars2.map.error.preload': 'Не удалось предварительно загрузить шаблоны карты: %s',
    'skywars2.map.error.preloader_unavailable': 'Предварительный загрузчик шаблонов карт недоступен.',
    'skywars2.map.error.spawns_missing': 'Карта %s размещена, но на ней нет маркеров спавна игроков.',
    'skywars2.map.error.height': 'Шаблон %s занимает Y=%s..%s за пределами высоты арены Y=%s..%s.',
    'skywars2.map.ready.lobby': 'Лобби готово.',
    'skywars2.map.ready.map': 'Карта %s готова.',
}
reasons = {
    'Map is unavailable.': 'Карта недоступна.',
    'Required structure templates are missing for this map.': 'Для этой карты отсутствуют необходимые шаблоны структур.',
    'This map has no placeable structures yet.': 'У этой карты пока нет структур для размещения.',
}
restored = {}
for key, english in fragment['en_us'].items():
    if key.endswith('.name'):
        restored[key] = 'Карта ' + re.fullmatch(r'skywars2\.map\.(\d+b?)\.name', key)[1]
    elif key.endswith('.unavailable'):
        restored[key] = reasons[english]
    else:
        restored[key] = errors[key]

ru_path = lang / 'ru_ru.json'
before = ru_path.read_bytes()
current = json.loads(before)
changed = {key: value for key, value in restored.items() if re.search(r'\?{2,}', current[key])}
assert len(changed) == 198, len(changed)
backup = root / 'build/verification/ru_ru.corrupt-encoding.json'
backup.parent.mkdir(parents=True, exist_ok=True)
backup.write_bytes(before)
current.update(changed)
ru_path.write_text(json.dumps(current, ensure_ascii=False, indent=2) + '\n', encoding='utf-8', newline='\n')
fragment['ru_ru'].update(restored)
english_height = 'Template %s spans Y=%s..%s outside the arena build range Y=%s..%s.'
fragment['en_us']['skywars2.map.error.height'] = english_height
en_path = lang / 'en_us.json'
en = json.loads(en_path.read_text(encoding='utf-8'))
if en['skywars2.map.error.height'] == 'Template %s spans Y=%s?%s outside the arena build range Y=%s?%s.':
    en['skywars2.map.error.height'] = english_height
    en_path.write_text(json.dumps(en, ensure_ascii=False, indent=2) + '\n', encoding='utf-8', newline='\n')
fragment_path.write_text(json.dumps(fragment, ensure_ascii=False, indent=2) + '\n', encoding='utf-8', newline='\n')
print(f'Repaired {len(changed)} Russian strings; other translations preserved; UTF-8 written directly')
