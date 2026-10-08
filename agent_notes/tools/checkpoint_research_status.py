"""Update coordinator status after completion of all first-pass coverage rows."""
from pathlib import Path
import re

p = Path('agent_notes/RESEARCH_STATUS.md')
s = p.read_text(encoding='utf-8')
s = s.replace('Документация находится в работе. Полное описание поведения пока **не завершено**.',
              'Полный черновик собран; независимая проверка содержания продолжается. Итоговая приёмка **не завершена**.')
rows = {
    'lbc_drop_effects': 'Сохранил все свои черновики, включая awaken (277). Независимо проверяет чужие other main (215), misc (214), events (137); отчёт review_other_main_misc_events.md.',
    'skywars_game': 'Сохранил игру/GUI/support, kit, LBCR и main (271). Независимо проверяет runtime (250); отчёт review_runtime.md.',
    'maps_catalogue': 'Сохранил NBT/maploader, other и generators (324). Независимо проверяет weapons (256) и arcana/trinkets (201); отчёт review_weapons_arcana.md.',
    'Координатор': 'Полностью сверил восемь разделов: UI (11), legacy raycast (20), effects (46), armor (77), bedwars (27), projectiles (70), SkyWars game (123), структуры/метаданные maps (1457). Отдельно проверены 33 mana/tentacle helper и 64 варианта mana-рецепта. Принимает независимые проверки и сверяет связи.'
}
for key, value in rows.items():
    pattern = r'^\| `?' + re.escape(key) + r'`? \|.*$'
    s, n = re.subn(pattern, '| `' + key + '` | ' + value + ' |', s, flags=re.M)
    assert n == 1, key
marker = '## Организация работы'
checkpoint = '''## Текущий контрольный результат

Все 10 800 источников имеют запись coverage и существующий точный заголовок:
missing=0, unresolved=0, link_error=0. Это завершение первого прохода,
а не доказательство правильности каждого описания. Проверено содержание восьми
разделов из 35; остальные проходят независимую сверку и приём координатором.

Полные палитры NBT и 14 707 блоков с дополнительными полями сохранены в
[maps_details.json](inventory/maps_details.json). Исправлен пропуск 2 398
block entities в старой сводке. Все 1 456 NBT прочитаны без ошибок,
сопоставлены с каталогом и SHA. Снимок 10 800 исходников повторно проверен:
изменений нет. Подробности исправлений — [REVIEW.md](REVIEW.md).

'''
assert marker in s and '## Текущий контрольный результат' not in s
s = s.replace(marker, checkpoint + marker)
p.write_text(s, encoding='utf-8')
print('Updated research status')
