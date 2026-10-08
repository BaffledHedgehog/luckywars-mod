from pathlib import Path
p=Path('agent_notes/lbc_other_misc.md'); s=p.read_text(encoding='utf8')
needle='\n## Freeze block:'
block='''
## Baffled wall: выбор цели и варианты луча

`baffledwall/tick` сначала сохраняет команду стены, затем наводится на ближайшую цель-моба в пределах 10, исключая саму стену, spectator, strict_map_object и same_team. Основные `tick_rc` начинают луч с Distance 0 и звуковым сигналом; `tick_loop` двигается по воздуху шагами 0,3 блока и останавливается при блоке, цели или после Distance 50 (около 15 блоков). Семейство helper-loop задаёт разные задачи, но пересечение `dx=dy=dz=0,distance=..5` остаётся кубом 1×1×1 в точке команды: `tick_loop_dmg` наносит первому допустимому мобу 4 arrow damage и останавливает луч; вариант `tick_loop_dmg_big` наносит 20; `tick_loop_dmg_laser` наносит 0,1 generic без немедленного стопа. Дружеские варианты `tick_loop_pl_heal` лечат помеченную `nodmg` цель на 1 секунду instant_health при продвижении луча; `tick_loop_pl_laser` наносит немаркированным целям 0,02 generic на шагах, но ограничен Distance 30 (9 блоков); `tick_loop_pl_sniper` ищет цель до Distance 300 (90 блоков) и отдаёт 20 arrow damage с остановкой. Слово `pl` в имени не является доказательством переключения команды: условия задаются selector-тегами каждого варианта.

[Цикл выбора цели](../world/datapacksdisabled/LBC/data/lbc/function/other/baffledwall/tick.mcfunction), [обычный луч](../world/datapacksdisabled/LBC/data/lbc/function/other/baffledwall/tick_loop.mcfunction), [лечащий вариант](../world/datapacksdisabled/LBC/data/lbc/function/other/baffledwall/tick_loop_pl_heal.mcfunction), [лазерный вариант](../world/datapacksdisabled/LBC/data/lbc/function/other/baffledwall/tick_loop_pl_laser.mcfunction), [снайперский вариант](../world/datapacksdisabled/LBC/data/lbc/function/other/baffledwall/tick_loop_pl_sniper.mcfunction).
'''
if '## Baffled wall:' not in s: s=s.replace(needle,'\n'+block+needle)
p.write_text(s,encoding='utf8')
