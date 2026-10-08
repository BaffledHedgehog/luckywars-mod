"""Coordinator review of all 70 projectile sources plus their explicit callers."""
import json
from pathlib import Path

notes = Path(__file__).resolve().parents[1]
p = notes / 'lbc_projectiles.md'
s = p.read_text(encoding='utf-8')

def replace(old, new):
    global s
    assert old in s, old
    s = s.replace(old, new)

replace('score `', 'score `')
replace('получает `dur`', 'увеличивается на `dur`')
replace('| 120 | score `delayed_death` увеличивается на `dur` |',
        '| 120 | score `delayed_death` **заменяется** значением `dur` |')
replace('которая обнуляет effect счётчики и не даёт им сработать.',
        'которая обнуляет ровно 25 отрицательных counters: remove_mana, snus, small_explosion, evoker, shock, explosion, big_explosion, traps, lightning_bolt, firebreak, kiuaskivi, lightarrow, evokerarrow, dimshifting, piss, witch_gens_reset, hedgehog_boom, delayed_death, nuking, vacuum, timestop, chain_lightning, shock_potion, skill_issue, nomagic. Положительные mana counters и RTP1..4 она не очищает. Прямые Kaname/rocket/shock-potion вызовы и создание relay-стрел уже выполняются **до** nodebuff и не отменяются задним числом.')
replace('blindness III', 'blindness II')
replace('Код 102 создаёт девять evoker fangs у игрока/моба в радиусе 8;',
        'Код 102 создаёт девять evoker fangs **у каждой** non-player сущности с DeathTime=0 и каждого non-spectator игрока в радиусе 8;')
replace('122 переносит не-player существа в радиусе 100 к позиции попадания',
        '122 переносит все не-player сущности в радиусе 100, кроме strict_map_object, к позиции попадания')
replace('Эти условия различаются по коду, их нельзя считать общей защитой всех стрел.',
        'Эти условия различаются по коду, их нельзя считать общей защитой всех стрел. arrows_f **безусловно убивает @s в конце**, даже если stopper заблокировал действие или код 112/113/115/116/117/120/123 не имеет в ней отдельной ветки. Коды 106/114 вообще обходят arrows_f и используются для дыма.')
replace('финал ставит вертикальную колонну из 27 TNT и 16 молний на разных высотах, если нет `weak_v`.',
        'финал сначала создаёт один TNT fuse=0 и одну молнию в текущей точке **даже при weak_v**. Затем weak_v делает return fail; без него добавляются ещё 26 TNT и 15 молний: всего 27 TNT / 16 молний на Y от +3 до −45. Это отдельные заданные высоты, а не сплошная колонна.')
replace('`inairtrappertrail` после Lifetime120 с шансом 25% срабатывает при игроке в 80 блоках, создаёт fangs и заканчивается на Lifetime390; позиционное рассеивание делегировано `universal_pos_razb_1`.',
        '`inairtrappertrail` при Lifetime>=120 делает 25% проверку и требует любого @a в радиусе 80 (spectator не исключён). Только тогда запускается evoker_rand: fang создаётся лишь при #mobs !spectator !strict_map_object в 5. Если таких мобов нет в 1.5, с отдельным шансом 50% helper поворачивает marker к ближайшему подходящему мобу в 15 и двигает на 1; потом зовёт universal_pos_razb_1. Его kill при Lifetime>=390 достигается только через эти gates. Внешний raycastselector всё равно удаляет trapper marker на data.lifetime=400. Light marker имеет data.lifetime=200. Возраст обоих повышает внешний dispatcher по одному за обслуженный tick; в time stop и исключённых измерениях это не гарантированные секунды настенных часов.')
replace('Поэтому относительные координаты `~`/`^` ниже отсчитываются от игрока, а не от попадания снаряда. Последующие тики удаляют снежки-гаджеты, кроме пути `tmt_item`.',
        'Поэтому обычные ~ /^ ниже отсчитываются от игрока. Есть явные исключения: rasengan сдвигает позицию на Y+1.62, kapusta/jack_o_lantern — к глазам, три *_of_everything — к абсолютной точке (0,64,0). Metasola отдельно переключает as/at на ближайший подходящий снежок, временно помечая игрока thrower. В конце **этого же вызова** удаляется ближайший gadget-снежок в 10, кроме tmt_item; он повторно выбирается и не привязан UUID к первой проверке.')
replace('| `tmt_item` | исключение из общего удаления гаджетного снежка |',
        '| `metasola` | переключает as/at на снежок и вызывает `lbc:other/event/halloween/soulcorruptwand/metasola/throw` |\n| `tmt_item` | исключение из общего удаления гаджетного снежка |')
replace('Y зажимается до 1..320, после чего active-tag снимается.',
        'X/Z берутся через floor(Pos), а целый Y **при <=1 или >=321 заменяется на 320**; только Y=2..320 сохраняется. Template ставится со смещением (−2,−2,−2), затем tag снимается. Это не clamp снизу к Y=1.')
replace('выбирает случайного не-owner игрока без `stopper` в 24 блоках и меняет их местами;',
        'сначала выбирает случайного non-spectator не-owner игрока и лишь **после выбора** проверяет stopper в 24 у него; при блокировке повторного выбора нет. Если цель помечена tp, телепортирует executor к ней, а её — к marker исходной площадки;')
replace('Bucket выбирает противника в 4,5 блоках выше точки исполнения,',
        'Bucket проверяет сферу 4.5 с центром на Y+1 от снаряда, перебирает всех non-spectator игроков и требует различие обоих ID/team. Для каждого подходящего положения повторно выбирает ближайшего игрока,')
replace('Tikva делает то же с loot', 'Tikva использует аналогичный перебор и loot')
replace('Ruchka может сбросить identity, если игрок в пределах 7,',
        'Ruchka ставит ID/team=−1, **если игроков нет** в 7 от точки Y+1,')
replace('Honey target в 4,5 блоках вызывает `other/drop_inv` для слотов инвентаря 0..35, offhand и четырёх броневых слотов, то есть сбрасывает инвентарь; дочерние drop-функции принадлежат внешнему пакету.',
        'Honey перебирает противников в 4.5 от Y+1; если рядом никого нет, сбрасывает только ID=−1. honey_pl в позиции Y+1 над выбранным игроком копирует заполненные 36 inventory, offhand и 4 armor слота через drop_inv helpers, затем **сам очищает все 41 слота** и разбрасывает ближайшие items малым Motion. Drop helpers по отдельности только копируют предметы. Bucket/tikva сначала копируют существующий helmet в item с PickupDelay=40, затем заменяют слот loot. Их drop_helmet повторно выбирает ближайший modifyitem в 2, без очистки tag. Ruchka/bucket/tikva/honew после обработки убивают текущий снаряд; некоторые helper-effects/particles остаются в исходном target context без нового at.')
replace('по одному блоку под projectile только там, где воздух;',
        'площадку **2×1×2** на Y−2.5 с X/Z от −0.5 до +0.5 только вместо minecraft:air;')
replace('Эти fill-ветки проверяют `stopper` в 20 блоках.',
        '**Most/tntmost** проверяют stopper в 20; wool и bucket/tikva/honew такого gate в bridgebuilders не имеют, ruchka проверяет его. Все 16 wool-веток — отдельные if, поэтому при нескольких цветовых flags последующие цвета перекрасят предыдущие.')
replace('с шансом 35% Motion разворачивается и умножается на 10,',
        'для **каждой оси независимо** проверяется шанс 35% и только прошедшая ось Motion разворачивается и умножается примерно на 10 с floor промежуточного числа;')
replace('создаёт TNT с Fuse 0 в трёх блоках за ближайшим врагом.',
        'поворачивает контекст в сторону ближайшего врага и создаёт TNT fuse=0 **на 3 блока позади текущего снаряда вдоль этого направления** (positioned ^ ^ ^−3), а не позади позиции врага.')
replace('При `trappertrail` вместо этого создаётся', 'При `trappertrail` отдельной if-командой создаётся')
replace('6710884→damage 10^17', '6710884→damage **10^20**')
replace('`bi_arrow` заменяет блок в текущем кубе 1×1×1',
        '`bi_arrow` заполняет текущий куб **2×2×2** (~−0.5..~+0.5)')
replace('`smoke` и `smoke_old` запускаются по одному разу. Обе меняют arrow `life` с `100s` на `800s` (Short: 800 тиков=40 секунд), ставят `smoke_active` и дают Blindness II на 2 секунды не-spectator игрокам в радиусе 5.',
        '`smoke` и `smoke_old` вызываются повторно из prein пока подходит item payload (Raid Omen amp106/114 **duration=20**) и inGround. В каждом вызове рисуют дым и обновляют Blindness II на 2 секунды всем non-spectator игрокам в 5. Только команда изменения life защищена !smoke_active: если при первом таком вызове arrow life уже **ровно 100**, ставит 800; затем в любом случае добавляет smoke_active. Если первый вызов был на другом life, позднее 100 уже не изменяется этим helper. Числа short задают тики; это не гарантированные 40 секунд существования дымовой стрелы.')
replace('серые wool block-marker частицы в объёме 7×4×7',
        '100 серых wool block-marker частиц со spread (7,4,7)')
replace('`specials` превращает `custom_data` летящего snowball в стрелы, fireball или предметные projectiles и копирует Motion исходного snowball.',
        '`specials` читает custom_data у snowball-executor, но штатный items/snowballs caller делает **as без at**, сохраняя позицию глаз игрока и его поворот. Поэтому новые снаряды появляются от глаз игрока (^ offsets), а Motion читается у выбранного снежка. Вложенные pernul callers тоже делают as без at до создания новых сущностей.')
replace('Owner/owner у ближайшего не-spectator игрока в радиусе4,',
        'Owner/owner у ближайшего non-spectator @p **без ограничения радиуса**; радиус 4 ограничивает выбор нового projectile,')
replace('`teleport_nahui` выбирает 1..4 и переносит executor в то же положение XYZ в другое измерение. Из Overworld это Nether/End/Nexus/Imprinted; из Nether — Overworld/End/Nexus/Imprinted; из End — Overworld/Nether/Nexus/Imprinted; из Imprinted — Nether/End/Nexus/Overworld. Nexus ветка делегирована `tp_nexus_rtp`; функция сама не выбирает безопасную точку и не даёт защитных эффектов.',
        '''`teleport_nahui` делает один random 1..4, затем **последовательно**, без return, проверяет текущие predicates executor Overworld → Nether → End → Imprinted. Успешная телепортация в ранней ветке меняет dimension executor, и следующая ветка может сработать ещё раз. При обычном успешном исполнении vanilla tp итог по стартовому измерению получается таким:

| Старт | 1 | 2 | 3 | 4 |
|---|---|---|---|---|
| Overworld | Overworld (через Nether) | Nether (через End) | Nexus helper | Overworld (через Imprinted) |
| Nether | Overworld | Nether (через End) | Nexus helper | Overworld (через Imprinted) |
| End | Overworld | Nether | Nexus helper | Overworld (через Imprinted) |
| Imprinted | Nether | End | Nexus helper | Overworld |

Позиция CommandSourceStack при tp сама не обновляется: дальнейшие ~ берутся из исходного контекста. `execute in` учитывает coordinate_scale (Nether ↔ обычное измерение меняет X/Z в 8 раз), так что «то же XYZ» неверно. Прямые `_ne/_oe/_on/_im` helpers по отдельности выполняют только свою таблицу назначения; эффект повторного перехода относится к верхнему dispatcher. При неизвестном измерении все четыре predicates могут не пройти. Nexus делегирован tp_nexus_rtp: он бросает X/Z −4800..4800 и вызывает внешний macro teleport; защита/безопасная точка здесь не гарантированы.''')

extra = '''
## Условия маршрутизации и скорость особых снарядов

У arrows_f защита mystical/type2 в Inventory любого @a<=12 проверяется только
для 101/104/107/109/110. Stopper<=20 нужен для 101/103/104/107/108/121/122/124,
stopper_magic<=20 — для 102/109/110/111/118; 119 и 125 проходят без обоих gates.
Такая защита не фильтрует союзников и не отменяет финальный kill стрелы.
Поля duration в arrow payload — тики, счётчики mobs увеличиваются на прочитанное
duration и обслуживаются своим runtime; длительность effect give задаётся секундами.

В eggs stopper<=24 проверяется у игрока для teleport1/teleport1_imba/islandtp/
randtp/kill_rand. Rasengan требует stopper_magic<=24 отсутствующим, Inventory
anti-TNT у любого @a<=12 отсутствующим и nomagic_active non-spectator @a<=20
отсутствующим. Остальные перечисленные egg-вызовы локально не имеют этих gates.
Значения custom_data проверяются независимо: составной предмет может вызвать
несколько ветвей до повторного выбора удаляемого снежка.

Передача Motion в specials:

| Группа нового снаряда | Фактическая запись |
|---|---|
| edited | целый Motion копируется со снежка (один ближайший edited<=4) |
| edited6, dynamite | каждая ось floor(Motion×10000)×0.0001, примерно ×1 |
| edited4, ender pearl | floor(Motion×10000)×0.00025, примерно ×2.5 |
| edited3, small_fireball/impulse/TNT puknul | floor(Motion×10000)×0.0006, примерно ×6 |
| edited2 fireball | floor(Motion×100000)×0.00000075, примерно ×0.075 |
| edited2 dragon_fireball | floor(Motion×100000)×0.0000005, примерно ×0.05 |
| edited2 small_fireball | прописана ×0.2, но обычная smallfireball-ветка создаёт edited3, поэтому эта строка к ней не относится |

Owner/owner и ID/team устанавливаются отдельно для edited/2/3/4, **не edited6**.
Для #arrows этой группы Rotation записывается как −floor(pitch/yaw ближайшего @p).
Повторные selectors выбирают по tag/distance/nearest, а не UUID только что созданного
объекта; наличие другого подходящего объекта может изменить адресата записи.
Metal pipe: item PickupDelay=10 тиков, attack_damage +10, attack_speed −3.1;
tooltip_style фактически вложен в custom_data. Impulse — arrow damage=1.
Spike payloads не следует описывать по названию: spike_blind передаёт счётчик piss,
spike_poison — witch_gens_reset, spike_wither — Water Breathing II.

Pernul создаёт 16 pearls / 16 dragon_fireballs / 32 TNT. Pearl/TNT staging выбирает
глобальный tag pernul/pernul_2 с limit=16/32 без distance; dragon staging имеет
distance<=5. Child helpers ищут Motion/Owner у own tag (у dragon<=10), дают разброс
и снимают staging tag. Поведение rand_motion_razbros/rand_power_razbros_small
связано с общим runtime, а не с независимой случайной ExplosionPower каждого fireball.
Shulker генерация копирует team, но Target-фильтр исключает только same_id,
не same_team. Два if lighttrail/trappertrail могут исполниться в одном вызове.

Homинг_hit всегда очищает inGround, даже когда все три 35% проверки провалились.
Racer ultra задаёт Motion примерно 2 блока/тик по взгляду выбранного ездока;
steampunk добавляет вектор 0.1 к текущему Motion, с ограничением общим helper.
Снятие NoGravity при отсутствии ездока не является удалением снаряда.
pre меняет Tags на список [updated] для первых четырёх цветовых вариантов,
а не добавляет updated к старому списку. Цвет 6710885 сменяется на 6710877
в том же проходе и сразу попадает в следующий homing if.

'''
assert '## Условия маршрутизации и скорость особых снарядов' not in s
s += extra
jobs = json.loads((notes/'inventory/research_jobs.json').read_text(encoding='utf-8'))
sources = jobs['lbc_projectiles']['sources']
assert len(sources) == 70
s += '## Проверенные исходники\n\nВсе 70 функций прочитаны координатором; отдельно сопоставлены items/snowballs, projectilestick, raycastselector и mobs_tick.\n\n'
s += '\n'.join('- ['+x.split('/function/')[1]+'](../'+x+')' for x in sources)+'\n'
p.write_text(s,encoding='utf-8')
print('Projectiles corrected; all 70 direct sources linked.')
