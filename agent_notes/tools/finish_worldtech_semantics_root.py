from pathlib import Path

p = Path('agent_notes/lbc_other_world_tech.md')
s = p.read_text(encoding='utf8')
old = '`other/nexus/rc` — dispatcher: он сначала сбрасывает `RC_Raycast` в 0, затем по одному зажатому `custom_data`-ключу вызывает roadgun, corrupter, orb gun или pulsar.'
new = '`other/nexus/rc` обслуживает зарегистрированный клик `RC_Raycast>=1`: проверяет предметы в обеих руках и последовательно вызывает все подходящие ветки roadgun/corrupter/orb gun/pulsar. В конце сбрасывает `RC_Raycast=0`; проверки не образуют взаимоисключающий выбор одного предмета.'
assert old in s
s = s.replace(old,new)
anchor = '\n\n## Wisp book:'
extra = '''

Roadgun создаёт block_display, копирует ID/team/Rotation, сажает игрока на него, даёт Slow Falling15с и ставит cooldown15. При обслуживании Lifetime≤35 display продвигается на15 блоков за вызов; до36 создаёт13 частичных шаблонов white-concrete куба7×7×7 с integrity0.002, шаг между точками3 блока. Sneak найденного по ID владельца удаляет display; на Lifetime39 обычная ветка снимает владельца с транспорта и ставит pad5×1×5, временно forceload-ив его чанк. На40 display удаляется. За пределами Nexus обслуживание идёт через rare-dispatcher, поэтому нельзя переводить Lifetime40 в40 игровых тиков без проверки его периода.

Corrupter трассирует луч шагом0.5 до400 по кубу1×1×1 или до solid-блока. При попадании ставит Distance1000, создаёт sim_cube marker и только тогда cooldown20с; без попадания cooldown не устанавливается. Добавленный caster-tag эти локальные helpers не снимают. Pulsar аналогично ищет solid-блок; в точке попадания передаёт внешний макро-command `pulsar` и ставит Distance58395, а cooldown15 выставляется после луча и при промахе. Внутренний эффект команды описывает [граница command bridges](COMMAND_BRIDGES.md), эти helpers не задают его сами.
'''
assert anchor in s
s = s.replace(anchor,extra+anchor,1)

s = s.replace('этот счётчик убывает каждый игровой тик', 'на каждом вызове cooldown-handler уменьшаются cooldown всех игроков, а handler вызывается для каждого игрока с положительным cooldown. Поэтому несколько активных Magnum ускоряют общий отсчёт; это не строго один decrement на игрока за тик')
s = s.replace('левый клик использует предмет в offhand, правый — в main hand', 'левый клик требует Magnum в offhand, правый — в mainhand. Но обе ветки стрельбы вызывают один check_ammo, который читает mode из SelectedItem главной руки; offhand влияет отдельно только на смену режима при sneak')
s = s.replace('если у цели нет `void_resistance`', 'если у цели нет `void_resistance` и тега `sin_greed_active`')
s = s.replace('Цель помечается `hit` до конца луча, чтобы один луч не повторял попадание в неё; cleanup удаляет этот tag со всех помеченных сущностей.', 'Standard/incendiary/void помечают цели hit и проходят сквозь мобов; friendly-filter у этих попаданий нет. Standard/incendiary останавливаются на блоке, void очищает блоки и продолжает луч. Acid/electric/explosive пытаются остановить луч при попадании через Distance401. Cleanup удаляет hit глобально только при Distance≥401; остановка из-за stopper раньше предела может оставить hit-теги.')
s = s.replace('этот короткий повторный огонь вызывается в течение четырёх тиков.', 'cooldown-handler вызывает до4 повторов по MagnumNyauwu, уменьшая его на1 за вызов; cast каждого повтора вновь устанавливает полный cooldown. Обычный повтор при положительном cooldown не тратит патрон; искусственный вызов при cooldown≤0 снова расходует его.')
old = '`techrunner_katana` заряжает `swordcharged6`: пока он в выбранной main-hand позиции, `items/swords` добавляет `swordcharging6` раз в тик; на3 сбрасывает прогресс и выдаёт ещё5 зарядов.'
new = '`techrunner_katana` получает заряд при событии items/swords с предметом в mainhand без stopper20: обычный вход — advancement gowno (незаблокированный не-arrow урон игрока по не-interaction вне Nexus/Imprinted), также есть вызовы TMT. Каждый такой вызов добавляет swordcharging6; на3 сбрасывает прогресс и прибавляет5 swordcharged6. Это заряд от боевых событий, а не каждый тик удержания. Каждый charge также сбрасывает movement-speed amount на оружии в0.05.'
assert old in s
s = s.replace(old,new)
s = s.replace('Каждый dash увеличивает `minecraft:movement_speed` modifier на оружии на0,02', 'Каждый dash задаёт movement-speed amount как `0.01×(floor(100×старое_amount)+2)` — приблизительно прибавляет0.02, с округлением')
s = s.replace('его attack-damage modifier', 'attack-damage modifier самого игрока')
old = '`test_create_boss` вручную запускается при полном techno-сете на armor stand: по одному `custom_data.techno:1` в каждой из четырёх armor slots.'
new = '`test_create_boss` вызывается advancement place_techno_cyborg при взаимодействии со стойкой предметом techno:1 вне Nexus/Imprinted. Он выбирает все подходящие armor stands без ограничения расстояния: ровно по1 leather helmet/chestplate/leggings/boots с techno:1; превращение не ограничено только стойкой, по которой кликнули.'
assert old in s
s = s.replace(old,new)
s = s.replace('Удержание main-hand `custom_data.laser_minigun` перезапускает 6-тиковое окно раскрутки. Если игрок перестаёт удерживать,', 'Правый клик spell1_default с laser_minigun в главной руке перезапускает 6-тиковое окно раскрутки. Одного удержания без повторных кликов недостаточно. Если новые клики прекращаются,')
s = s.replace('пока игрок продолжает удерживать и spin достиг30', 'пока клики поддерживают окно и spin достиг30')
s = s.replace('`random -spread..spread` добавляет разброс к yaw и pitch', '`random -spread..spread` добавляет к yaw и pitch независимый разброс в тысячных долях градуса: spread12000 означает±12°, spread1000 —±1°')
s = s.replace('попадание наносит4 generic от игрока.', 'попадание пытается нанести4 generic от игрока. Первичная проверка выбирает вражеских mobs в кубе, но hit-helper повторно выбирает до3 сущностей любого типа в этом кубе и проверяет ID/team ближайшей к каждой точки сущности без type/sort; это не строгая гарантия урона ровно первой найденной mob-цели.')
s = s.replace('item_display-визуал, который до6 тиков тянется к владельцу', 'item_display-визуал, поставленный у глаз найденного raycaster; он до6 вызовов уменьшает ширину, без повторного следования за игроком')
s = s.replace('Marker начинает `railtime` с1', 'Первый tick увеличивает data.railtime до1')
s = s.replace('каждый проход расчищает пространство2 блока вверх', 'каждый проход расчищает текущий блок и2 блока над ним')
s = s.replace('В конце обычной ветки строится oak-wood block', 'При отсутствии игрока в75 блоках вызов не выполняется и таймер стоит. Каждый7-й строительный шаг ставит redstone block для питания rails; обычный путь удерживается выше Y30, infinite выше−34, подъём ограничен Y318. В конце обычной ветки строится oak-wood колонка из2 блоков')
p.write_text(s,encoding='utf8')
print('Nexus, Katana, Magnum, Laser Minigun, Cyborg and Railroad prose corrected')
