from pathlib import Path
p=Path('agent_notes/lbc_other_world_tech.md'); s=p.read_text(encoding='utf8')
block='''
## Laser minigun: разгон, ресурс и выстрел

`shoot/click` начинает/продлевает раскрутку: проигрывает звук при cooldown=0, ставит окно 6 тиков, `revved` и расписание тика. Пока cooldown >0, `shoot/run` наращивает spin; после окна spin убывает. На значении 30 `full_rev` пытается стрелять при mana≥100. Каждый `cast` тратит 100 mana, удерживает spin на 30, уменьшает `laser_minigun_spread` на 100 только от значения 1100+, выдаёт shooter advancement при spread≤1000 и создаёт ray-marker с ID, командой и rotation владельца. Поэтому этот ствол тратит ману и копит spread, а не боеприпасы. [Вход раскрутки](../world/datapacksdisabled/LBC/data/lbc/function/other/techno/laser_minigun/shoot/click.mcfunction), [тик spin/cooldown](../world/datapacksdisabled/LBC/data/lbc/function/other/techno/laser_minigun/shoot/run.mcfunction), [порог полной раскрутки](../world/datapacksdisabled/LBC/data/lbc/function/other/techno/laser_minigun/shoot/full_rev.mcfunction), [расход и spawn луча](../world/datapacksdisabled/LBC/data/lbc/function/other/techno/laser_minigun/shoot/cast.mcfunction).

## Supply crate: таймер, приземление и содержимое

Появление supply beacon передаёт display в supply-crate tick; `as_crate` задаёт `supplyFall=99` и сразу начинает `run`. Дальше расписание раз в тик увеличивает счётчик: на 100 звучит предупреждение, на 146–149 по очереди рисуются облака сверху вниз, на 150 ящик фиксируется barrier-колонной высотой 2 и наносит 10 generic урона целям в пределах 3 и 900 целям в пределах 1; на 200 модель меняется на открытую и появляются interaction плюс armor stands с Magnum и Techrunner Katana. `open` снимает тег `new`, поэтому повторная scheduled цепочка перестаёт подбирать этот display. Это появление предметов-предложений; выдачу по взаимодействию нужно читать отдельно в item-interaction caller. [Создание beacon](../world/datapacksdisabled/LBC/data/lbc/function/other/shishend/risk_of_rain/run.mcfunction), [инициализация счётчика](../world/datapacksdisabled/LBC/data/lbc/function/other/shishend/risk_of_rain/as_crate.mcfunction), [таймер](../world/datapacksdisabled/LBC/data/lbc/function/other/techno/supply_crate/run.mcfunction), [приземление](../world/datapacksdisabled/LBC/data/lbc/function/other/techno/supply_crate/land.mcfunction), [открытие](../world/datapacksdisabled/LBC/data/lbc/function/other/techno/supply_crate/open.mcfunction).
'''
if '## Laser minigun: разгон, ресурс и выстрел' not in s:
 i=s.index('\n## Файлы:'); s=s[:i]+'\n'+block+s[i:]
p.write_text(s,encoding='utf8')
