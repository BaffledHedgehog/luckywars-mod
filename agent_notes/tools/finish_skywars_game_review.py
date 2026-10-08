"""Apply remaining direct-source review notes without touching source packs."""
from pathlib import Path
import json

p = Path('agent_notes/skywars_game.md')
s = p.read_text(encoding='utf-8')

def replace(old, new):
    global s
    if old not in s:
        raise ValueError(old[:120])
    s = s.replace(old, new)

replace('Затем затем spectator', 'Затем spectator')
replace('quickstart interaction, area_effect_cloud', 'любую сущность с тегом swrg.quickstart, area_effect_cloud')
replace('Наличие `have_bed` переводит игрока в spectator на 5 секунд и компенсирует одну жизнь.', 'Наличие `have_bed` переводит игрока в spectator, ставит respawn_timer=5 и компенсирует одну жизнь; таймер обслуживает BedWars.')
replace('ключ при попытке открыть страницы 5100..5500 сначала возвращается/списывается,', 'одинаковые прибавления и вычитания ключей по диапазонам GUI взаимно компенсируются,')
replace('вторая — через 360 секунд при `#stage2 != 0`;', 'вторая — через 360 секунд при `#stage2 != 0`; первая запланирована даже при #stage1=0;')
replace('В `quickstart/1a` счётчик игроков включает всех игроков; при 25+ нет соответствующей ветки задания индекса.', 'В `quickstart/1a` счётчик игроков включает всех игроков; при 25+ нет ветки задания нового индекса, однако последующий макрос всё равно вызывается.')
replace('переносит число из `components.minecraft:attribute_modifiers[].amount`', 'переносит число из `components.minecraft:attribute_modifiers[].amount`') if 'переносит число из `components.minecraft:attribute_modifiers[].amount`' in s else None
replace('язык сообщения матча зависит от последнего выбора, а не от языка сервера.', 'язык сообщения матча зависит от последнего общего выбора. #lang равен 2 для en и 1 для ru; обе функции меняют CustomName всех сущностей swrg.lobby и вызывают соответствующую LBC kit-локализацию.')
replace('их дальнейшее движение/удаление этой функцией не задаётся.', 'сразу после spread все screamer_halloween, включая ранее существовавшие, поднимаются на .01 блока; повторного движения или удаления сама эта функция не планирует.')
replace('В `core/player/stats` winrate (`swrg.wr`) — процент побед от сыгранных матчей, а K/D делится на смерти без защиты от деления на ноль: новый игрок с нулём смертей может не получить корректные значения.', 'В `core/player/stats` WR=floor(100×wins/games), целая часть K/D=floor(kills/deaths), дробная=(floor(100×kills/deaths) mod 100). Защиты от нулевых games/deaths нет: деление выдаёт ошибку, следующие команды продолжаются, а score остаётся результатом предшествующего присваивания/умножения. Поэтому при нулевом знаменателе показываемые числа не означают рассчитанное отношение.')
replace('то есть выбранный источник предметов может оказаться не тем самым соседом в двух блоках.', 'это остаётся тот же ближайший сосед при неизменной позиции, поскольку отсутствие radius не меняет sort=nearest.')

s += '''

### Дополнение к взаимодействию эффектов победы с LBC

Дисплей золотого меча обрабатывает
[`wingoldensword`](../world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/win_effects/wingoldensword.mcfunction).
На каждом проходе он проверяет четыре точки вперёд с шагом 1, рисует gold_block
и при Distance=4 телепортируется к конечной точке через LBCR, сбрасывая Distance.
Попадание в #minecraft:mobs в кубе 1×1×1 с distance≤10 (без tag spectator),
либо блок вне #minecraft:airs, вызывает один из пяти равновероятных исходов:
золотая фигура из пересекающихся fill-объёмов, raw_gold_block плюс внешняя
команда midasfr, 18 falling gold blocks по вееру с шагом yaw 10° плюс TNT
fuse=0/explosion_power=3, аналогичная фигура из raw_gold_block, либо полный
куб золота 3×3×3. Фигуры из четырёх fill заменяют только #solid; полный куб
варианта 5 заменяет любые блоки. Каждый исход удаляет дисплей; дополнительный
предел самого дисплея — Lifetime≥30. Условия попадания в сущность и блок
записаны отдельными if, общего stopper-фильтра здесь нет. Команда midasfr
относится к [внешним командным мостам](COMMAND_BRIDGES.md).

[`winpumpkin`](../world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/win_effects/winpumpkin.mcfunction)
при каждом вызове с шансом 30% призывает falling pumpkin на один блок вперёд,
повернувшись к (0,100,0), заменяет #tannerkivi в объёме 11×11×11 тыквами,
создаёт TNT fuse=0 и переносит сам marker через spreadplayers в range=25 от
текущей X/Z позиции. Сам helper не удаляет marker; общий slowraycast/reset
определяет длительность. Это эффект над картой, а не только частицы.

Повторяющиеся исходники слотов 0..26, 25 одинаковых Halloween summons,
261 запись префиксов и все 85 chest pools сверены с приложениями;
результат — [skywars_variants_audit.json](inventory/skywars_variants_audit.json).
Прямые функции этого раздела прочитаны координатором. Другие разделы LBC,
GUI и maploader остаются отдельными заданиями содержательной проверки.
'''
p.write_text(s, encoding='utf-8')
jobs_path = Path('agent_notes/inventory/research_jobs.json')
jobs = json.loads(jobs_path.read_text(encoding='utf-8'))
jobs['skywars_game']['content_reviewed'] = True
jobs['skywars_game']['reviewed_by'] = '/root'
jobs['skywars_game']['review_note'] = 'Direct 123-source review; repetitive families and raw appendices independently compared; integration references linked to separately audited jobs.'
jobs_path.write_text(json.dumps(jobs, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
print('Reviewed skywars_game 123')
