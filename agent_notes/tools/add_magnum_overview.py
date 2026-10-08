from pathlib import Path
p=Path('agent_notes/lbc_other_world_tech.md'); s=p.read_text(encoding='utf8')
block='''
## Magnum: режимы, патроны и лучевой выстрел

`sneak` на `check_shift`/`check_shift_left` меняет режим вперёд/назад через Academy mode helper; без sneak выстрел возможен только при `coldownMagnum=0`. `check_ammo` требует одновременно текущий `mode` и соответствующий stack `custom_data` в инвентаре; режимы 0–5 — standard, incendiary, electric, acid, void, explosive. В отличие от этого, `check_ammo_2` внутри короткой серии проверяет только mode, без патронного стека. У обычного standard-выстрела расходуется одна `standard_round`, запускается луч, поворачивается угол игрока на −3°, звучат выстрелы, выдаётся advancement и ставится `coldownMagnum=25`; этот счётчик убывает каждый игровой тик. Луч идёт по 0,5 блока до `Distance=400` (примерно 200 блоков), останавливается на блоке или marker `stopper`, наносит мобу 16 arrow-урона; проверка цели объединяет `dx=dy=dz=0` с `distance=..15`, поэтому здесь действует куб 1×1×1 у точки, а не широкий радиус 15. Иные типы патронов направляются в отдельные `cast/check/hit`-цепочки ниже и имеют собственные результаты/расход.

Опорные исходники: [mode checks](../world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/check_shift.mcfunction), [ammo dispatcher](../world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/check_ammo.mcfunction), [alternate dispatcher](../world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/check_ammo_2.mcfunction), [standard ray](../world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/standard/loop.mcfunction), [standard ray checks](../world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/standard/check.mcfunction), [standard shot output](../world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/standard/cast.mcfunction), [tick cooldown](../world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/cooldown.mcfunction).
'''
if '## Magnum: режимы, патроны и лучевой выстрел' not in s:
 i=s.index('\n## Файлы:'); s=s[:i]+'\n'+block+s[i:]
p.write_text(s,encoding='utf8')
