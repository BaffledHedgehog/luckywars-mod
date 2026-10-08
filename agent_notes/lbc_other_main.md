# LBC: прочие функции — основной набор

Здесь разобраны ровно 215 источников `lbc_other_main`. Для каждого runtime-файла приведено фактическое действие по его командам и caller, если он обнаружен; вспомогательные/пустые источники помечены отдельно. Reward advancement означает автоматический вход, но расход предмета связывается с trigger; если caller отсутствует, функция всё равно исполняется вручную.

Селекторы читаются буквально: `dx/dy/dz` вместе с `distance` задают пересечение условий; `execute as` меняет executor, но не позицию без `at`/`positioned`. Командный эффект `/effect give` измеряет длительность в секундах. В SNBT `fuse:2s` — Short на два тика (0,1 секунды), а `schedule 2s` — две секунды.

## Краткая карта механик

`stopper` и `stopper_magic` — два расходуемых на время мира подавителя, привязанных к lightning rod marker. Каждый вырезает из куба 31×31×31 блоки своего tag (`#minecraft:mecha` или `#minecraft:magic`), а также удаляет подходящие сущности в радиусе 20; когда блок rod пропадает, callback выдаёт loot-замену и убивает marker. Это массовая очистка материалов/сущностей, а не простой визуальный щит. [Обычный подавитель и завершение](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/stopper.mcfunction), [магический вариант](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/stopper_magic.mcfunction), [выдача после потери rod](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/stopperbreakedasitem.mcfunction).

Silverfish с hedgehog-тегами служат обработчиками предметов: `silverfish_tick` выбирает подходящие варианты, а healer направляет лечащий луч Baffledwall, laser — свой луч с малыми попаданиями, sniper проходит chance/timer gate и запускает дальний выстрел. Варианты временно ставят `nodmg` на локальную цель и снимают его после вызова, поэтому разные лучи используют собственные target-фильтры. [Диспетчер silverfish](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/silverfish_tick.mcfunction), [healer](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/hedgehoghealer.mcfunction), [laser](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/hedgehoglaser.mcfunction), [sniper](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/hedgehogsniper.mcfunction), [лучи Baffledwall](lbc_other_misc.md#baffled-wall-выбор-цели-и-варианты-луча).

`falling_power` создаёт slowraycast marker в `(0,310,0)`, затем записывает ему случайные X/Z в диапазоне −100…100, сохраняя Y=310; поле `data.lifetime=800` задаёт лимит общего runtime raycaster. Его дальнейшее движение и payload идут через `movingpower` runtime/LBCR обработчики. [Создание](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/falling_power.mcfunction), [координаты](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/falling_power_data.mcfunction), [runtime tick/lifetime](lbc_runtime.md#raycaster-и-marker-диспетчеры).

Consumable/event функции обычно входят через advancement reward или общий tick. `death_compass_check` сравнивает целые X/Z игрока с `LastDeathLocation` (без Y/измерения) и при совпадении тратит предмет через `death_compass_attack`, который ставит Warden с 150 HP и Darkness на 30 секунд, затем перемещает самого Warden через `spreadplayers` в пределах 10 блоков по горизонтали; в Nether к нему также применяется ограничение `under 120`. `deviltotem` вызывается при срабатывании тотема: даёт Resistance на 1 секунду, ставит death timer 5 и оставляет случайные 15% шаблона перед лавовым кубом; отдельные функции периодически обслуживают некоторые предметы, а consumable stew/food chains отзывают advancement после награды, чтобы разрешить следующий consume. [Compass check/attack](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/death_compass_check.mcfunction), [deviltotem](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/deviltotem.mcfunction), [повторная еда](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/infinitestewused.mcfunction).

## Сущности, события и прочие механики

### `lbc:other/absurd_child`

**Фактическая работа.** Вызов приходит из `mobs_5t_lbc`: zombie с тегом `otchim` должен быть в пределах2,5 блоков от armor stand `mom`. Сначала функция вызывает молнию у каждой сущности в текущем измерении, кроме Nexus, создаёт 5000 частиц и выдаёт nearby-игрокам достижение; затем сообщает всем, создаёт светящегося allay с 100000 здоровья и lucky-block в руке, удаляет ближайший `mom` и самого zombie. Это глобально громкий эффект на всё измерение, а не только появление allay; lucky-block остаётся у allay. [Функция](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/absurd_child.mcfunction), [вызывающий mob tick](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/mobs_5t_lbc.mcfunction)

### `lbc:other/absurd_child_tick`

**Фактическая работа.** Раз в секунду вызывается только для allay `absurd_child`, если нет `stopper_magic` marker в `distance=..20`. Когда не-spectator player в `distance=..5` отсутствует, allay двигается через `raycast_vpered_slower`; отдельно predicate `chance3` запускает случайную реплику. Если main hand равен red mushroom block, brown mushroom block или mushroom stem, соответственно заменяет его на `luckyblock_neko_block`, `luckyblock_loli_block` или `luckyblock_block_only`; каждая loot-ветка делает `return`, поэтому за тик выполняется не больше одной замены. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/absurd_child_tick.mcfunction)

### `lbc:other/absurd_child_tick_1`

**Фактическая работа.** Автоматический caller не найден; при ручном запуске выполняются описанные действия. Результат: меняет scoreboard: `scoreboard players add temp_lb_id lbc.math 1`; помещает результат loot table `lbc:luckyblock_loli_block` в указанный слот. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/absurd_child_tick_1.mcfunction)

### `lbc:other/absurd_child_tick_2`

**Фактическая работа.** Автоматический caller не найден; при ручном запуске выполняются описанные действия. Результат: меняет scoreboard: `scoreboard players add temp_lb_id lbc.math 1`; помещает результат loot table `lbc:luckyblock_neko_block` в указанный слот. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/absurd_child_tick_2.mcfunction)

### `lbc:other/absurd_child_tick_random_msg`

**Фактическая работа.** Выбирает число 1..20 и отправляет соответствующую переводимую реплику `absurd_child_msg_N` игрокам с `distance=..30`. Это только сообщение, без урона или выдачи предметов. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/absurd_child_tick_random_msg.mcfunction)

### `lbc:other/falling_power`

**Фактическая работа.** Создаёт marker в точке `0 310 0` с `lifetime:800` и тегами slowraycast/movingpower/spawned_new_power/can_deleted/magic/other, затем запускает обработчик координат marker. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/falling_power.mcfunction)

### `lbc:other/falling_power_data`

**Фактическая работа.** У текущего marker задаёт X и Z случайными числами от −100 до 100, сохраняет Y без изменений и снимает тег `spawned_new_power`. Вызывается через `execute as`; без `at @s` позиция выполнения не переносится к marker. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/falling_power_data.mcfunction)

### `lbc:other/hedgehog`

**Фактическая работа.** Автоматический caller не найден; при ручном запуске выполняются описанные действия. Результат: наносит 4 урона @s типа `cactus`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/hedgehog.mcfunction)

### `lbc:other/hedgehog_boom`

**Фактическая работа.** Без дополнительных локальных условий передаёт выполнение `lbcr:raycast_loop9cd_upgrade`; реализация есть в том же datapack, файл [`raycast_loop9cd_upgrade.mcfunction`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_loop9cd_upgrade.mcfunction), и её функции описываются в `lbc_lbcr`. В этой функции нет собственного selector/условия. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/hedgehog_boom.mcfunction)

### `lbc:other/hedgehoghealer`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/silverfish_tick.mcfunction`, условие/команда: `execute if entity @s[tag=hedgehog_healer] unless score @s simpleTimer matches 1.. run function lbc:other/hedgehoghealer`. Результат: меняет tag: `@e[type=#minecraft:mobs,distance=..0.01,limit=1,team=!] add nodmg`; меняет tag: `@s remove nodmg`; передаёт выполнение `lbc:other/baffledwall/tick_rc_pl_heal`; меняет tag: `@e remove nodmg`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/hedgehoghealer.mcfunction)

### `lbc:other/hedgehoglaser`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/silverfish_tick.mcfunction`, условие/команда: `execute if entity @s[tag=hedgehog_laser] run function lbc:other/hedgehoglaser`. Результат: меняет tag: `@e[type=#minecraft:mobs,distance=..0.01,limit=1,team=!] add nodmg`; передаёт выполнение `lbc:other/baffledwall/tick_rc_pl_laser`; меняет tag: `@e remove nodmg`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/hedgehoglaser.mcfunction)

### `lbc:other/hedgehogsniper`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/silverfish_tick.mcfunction`, условие/команда: `execute if entity @s[tag=hedgehog_sniper] unless score @s simpleTimer matches 1.. if predicate lbc:chance20 run function lbc:other/hedgehogsniper`. Результат: меняет tag: `@e[type=#minecraft:mobs,distance=..0.01,limit=1,team=!] add nodmg`; передаёт выполнение `lbc:other/baffledwall/tick_rc_pl_sniper`; меняет tag: `@e remove nodmg`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/hedgehogsniper.mcfunction)

### `lbc:other/kill_rand_hihihiha`

**Фактическая работа.** Выбирает 1..7: исходы 1..6 убивают текущую сущность без `void_resistance` и без исключающего тега `sin_greed_active`. Исход 7 выбирает другого не-spectator игрока и убивает его только при проверенных условиях сопротивления/Nexus/stopper; затем выполняет advancement grant для текущего контекста и снимает временный `owner`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/kill_rand_hihihiha.mcfunction)

### `lbc:other/killanvil`

**Фактическая работа.** Удаляет все сущности с тегом `killedanvils`; вызывающая функция планирует очистку через 60 game ticks. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/killanvil.mcfunction)

### `lbc:other/killcart`

**Фактическая работа.** Удаляет все сущности `killedcart`; вызов запланирован на `25s` после создания, то есть через 25 секунд. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/killcart.mcfunction)

### `lbc:other/stopper`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/markers_2t1.mcfunction`, условие/команда: `execute if entity @s[tag=stopper] run return run function lbc:other/stopper`. Результат: изменяет блоки: `fill ~15 ~15 ~15 ~-15 ~-15 ~-15 minecraft:air replace #minecraft:mecha strict`; передаёт выполнение `lbc:other/stopper_destroy`; удаляет выбранную сущность; передаёт выполнение `lbc:other/stopper_marker_destroy`; передаёт выполнение `lbc:other/stopper_player_remove_smth`; передаёт выполнение `lbc:other/stopperbreaked`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/stopper.mcfunction)

### `lbc:other/stopper_destroy`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/stopper.mcfunction`, условие/команда: `execute as @e[type=#minecraft:mecha,distance=..20] at @s run function lbc:other/stopper_destroy`. Результат: создаёт частицы `soul_fire_flame`; удаляет выбранную сущность. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/stopper_destroy.mcfunction)

### `lbc:other/stopper_magic`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/markers_2t1.mcfunction`, условие/команда: `execute if entity @s[tag=stopper_magic] run return run function lbc:other/stopper_magic`. Результат: изменяет блоки: `fill ~15 ~15 ~15 ~-15 ~-15 ~-15 minecraft:air replace #minecraft:magic strict`; передаёт выполнение `lbc:other/stopper_magic_destroy`; передаёт выполнение `lbc:other/stopperbreaked_magic`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/stopper_magic.mcfunction)

### `lbc:other/stopper_magic_destroy`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/stopper_magic.mcfunction`, условие/команда: `execute as @e[type=#minecraft:mobs,distance=..20,tag=summactivated] at @s run function lbc:other/stopper_magic_destroy`. Результат: создаёт частицы `witch`; удаляет выбранную сущность. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/stopper_magic_destroy.mcfunction)

### `lbc:other/stopper_marker_destroy`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/stopper.mcfunction`, условие/команда: `execute as @e[distance=..20,tag=mecha] at @s run function lbc:other/stopper_marker_destroy`. Результат: передаёт выполнение `lbc:other/stoppernuke`; удаляет выбранную сущность. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/stopper_marker_destroy.mcfunction)

### `lbc:other/stopper_player_remove_smth`

**Фактическая работа.** Удаляет у `@s` modifier движения и modifier урона атаки по их UUID. Caller сначала выбирает игроков в пределах `distance=..20`, затем `at @s` переносит позицию к каждому игроку. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/stopper_player_remove_smth.mcfunction)

### `lbc:other/stopperactivate`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/raycasters_1s.mcfunction`, условие/команда: `execute if entity @s[tag=stopper,tag=!activated] run function lbc:other/stopperactivate`. Результат: изменяет блоки: `setblock ~ ~ ~ minecraft:lightning_rod`; изменяет блоки: `setblock ~ ~1 ~ minecraft:lightning_rod`; перемещает @s в `~ ~1 ~`; меняет tag: `@s add activated`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/stopperactivate.mcfunction)

### `lbc:other/stopperactivate_magic`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/raycasters_1s.mcfunction`, условие/команда: `execute if entity @s[tag=stopper_magic,tag=!activated] run function lbc:other/stopperactivate_magic`. Результат: изменяет блоки: `setblock ~ ~ ~ minecraft:lightning_rod`; изменяет блоки: `setblock ~ ~1 ~ minecraft:lightning_rod`; перемещает @s в `~ ~1 ~`; меняет tag: `@s add activated`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/stopperactivate_magic.mcfunction)

### `lbc:other/stopperbreaked`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/stopper.mcfunction`, условие/команда: `execute if entity @s[tag=activated] unless block ~ ~ ~ minecraft:lightning_rod run function lbc:other/stopperbreaked`. Результат: передаёт выполнение `lbc:other/stopperbreakedasitem`; удаляет выбранную сущность. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/stopperbreaked.mcfunction)

### `lbc:other/stopperbreaked_magic`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/stopper_magic.mcfunction`, условие/команда: `execute if entity @s[tag=activated] unless block ~ ~ ~ minecraft:lightning_rod run function lbc:other/stopperbreaked_magic`. Результат: передаёт выполнение `lbc:other/stopperbreaked_magicasitem`; удаляет выбранную сущность. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/stopperbreaked_magic.mcfunction)

### `lbc:other/stopperbreaked_magicasitem`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/stopperbreaked_magic.mcfunction`, условие/команда: `execute as @e[type=minecraft:item,sort=nearest,limit=1,nbt={Item:{id:"minecraft:lightning_rod"}}] at @s run function lbc:other/stopperbreaked_magicasitem`. Результат: создаёт на месте результат loot table `lbc:stopper_magic`; удаляет выбранную сущность. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/stopperbreaked_magicasitem.mcfunction)

### `lbc:other/stopperbreakedasitem`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/stopperbreaked.mcfunction`, условие/команда: `execute as @e[type=minecraft:item,sort=nearest,limit=1,nbt={Item:{id:"minecraft:lightning_rod"}}] at @s run function lbc:other/stopperbreakedasitem`. Результат: создаёт на месте результат loot table `lbc:stopper_techno`; удаляет выбранную сущность. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/stopperbreakedasitem.mcfunction)

### `lbc:other/stoppernuke`

**Фактическая работа.** Удаляет в кубе ±1 вокруг позиции nuke блоки netherite и beacon, очищает блок над ним и удаляет marker. Затем **планирует** `stopnuke` через одну секунду; звук останавливается отдельной отложенной функцией. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/stoppernuke.mcfunction)

## Расходники, защита и эффекты

### `lbc:other/aimbob_apple`

**Фактическая работа.** Употребление предмета с `aimbobapple:1` вне Nexus и Imprinted запускает reward: Health Boost X на300с, Regeneration CXXVIII на3с, Resistance V на5с и Saturation CXXVIII на1с. Затем advancement отзывается, чтобы следующий предмет тоже сработал. [Функция](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/aimbob_apple.mcfunction), [условия употребления](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/aimbobapple.json)

### `lbc:other/areas`

**Фактическая работа.** Автоматический caller не найден; при ручном запуске выполняются описанные действия. Результат: передаёт выполнение `lbc:raycastselector`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/areas.mcfunction)

### `lbc:other/bad_apple`

**Фактическая работа.** Reward `lbc:badapple` срабатывает при употреблении предмета с `custom_data.bad_apple:1`; функция даёт игроку 15 эффектов: absorption, conduit_power, fire_resistance, haste, health_boost, invisibility, night_vision, regeneration, resistance, saturation, slow_falling, speed, strength и water_breathing на 300 секунд, а instant_health — на 30 секунд (везде amplifier 0). В конце reward advancement отзывается, чтобы следующий предмет снова мог его запустить. Цена — один съеденный предмет; отдельного cooldown нет. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/bad_apple.mcfunction), [trigger](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/badapple.json)
### `lbc:other/big_fireball_byid`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/mobs_5t.mcfunction`, условие/команда: `execute if entity @s[type=minecraft:blaze,tag=super_blaze] if predicate lbc:chance10 run function lbc:other/big_fireball_byid`. Результат: меняет scoreboard: `scoreboard players operation *temp lbcID2 = @s lbcID2`; меняет scoreboard: `scoreboard players operation *tempt lbcID2 = @s team_number`; передаёт выполнение `lbc:other/raycastfireballbig`; создаёт частицы `dust{color:[1.0,0.949,0.208],scale:4.0f}`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/big_fireball_byid.mcfunction)

### `lbc:other/blazehead`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/players_10t.mcfunction`, условие/команда: `execute if score @s blaze matches 1.. unless entity @e[type=minecraft:marker,distance=..20,tag=stopper_magic,limit=1] run function lbc:other/blazehead`. Результат: передаёт выполнение `lbc:other/blazehead1`; меняет scoreboard: `scoreboard players remove @s blaze 1`; накладывает `fire_resistance` на @s на 2 секунд, amplifier 0; проигрывает звук `minecraft:entity.blaze.shoot` для @a. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/blazehead.mcfunction)

### `lbc:other/blazehead1`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/blazehead.mcfunction`, условие/команда: `execute anchored eyes positioned ^ ^ ^ run function lbc:other/blazehead1`. Результат: создаёт частицы `flame`; меняет tag: `@s add caster`; создаёт сущность `small_fireball` с Tags ["blazehead"]; меняет NBT/components: `modify entity @e[type=minecraft:small_fireball,distance=..0.001,tag=blazehead,tag=!active,limit=1] Owner set from entity @s UUI`; передаёт выполнение `lbc:raycast_vpered_slow_power`; меняет tag: `@s remove caster`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/blazehead1.mcfunction)

### `lbc:other/blazestorm`

**Фактическая работа.** Создаёт три `minecraft:fireball` с `ExplosionPower:1b`, вертикальной Motion −0.1 и тегом `blazeball`. Helper сдвигает X/Z каждого на случайное целое от −15 до 15 и добавляет `active`; Owner helper не устанавливает. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/blazestorm.mcfunction)

### `lbc:other/death`

**Фактическая работа.** Каждые2 тика уменьшает положительный `death_timer`. Когда после уменьшения он равен0, убивает текущую сущность, кроме обладателей `void_resistance>=1` и тега `sin_greed_active`; иные значения (например timer=0 изначально) сами по себе не запускают этот handler. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/death.mcfunction)
### `lbc:other/death_compass_attack`

**Фактическая работа.** После проверки `death_compass_check` (сверяет X и Z с `LastDeathLocation`, Y не сравнивается) расходует один `death_compass`, создаёт warden на 15 блоков выше с Health 150 и `DeathLootTable:lbc:death_loot`. Даёт darkness на 30 секунд и звук; затем `spreadplayers` перемещает именно Warden, а не игрока, максимум на 10 блоков по горизонтали. Nether-ветка дополнительно требует `under 120`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/death_compass_attack.mcfunction)

### `lbc:other/death_compass_check`

**Фактическая работа.** Вызывается для игрока с `death_compass_active`, если в радиусе20 нет `stopper_magic` marker. Сохраняет целочисленные значения X/Z игрока и X/Z из `LastDeathLocation`; `data get` с масштабом1 округляет вниз. При совпадении обеих координат запускается атака; Y и измерение здесь отдельно не сравниваются. Реальный удар расходует компас и создаёт Warden в `death_compass_attack`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/death_compass_check.mcfunction)
### `lbc:other/deviltotem`

**Фактическая работа.** Срабатывание Totem of Undying с `deviltotem:1` вне Nexus и Imprinted запускает reward. Он выдаёт достижение, даёт владельцу Resistance V на1с, ставит `death_timer=5`, размещает шаблон лавы со смещением −15 по каждой оси и integrity0,15 (сохраняется случайная часть блоков), затем заливает куб3×3×3 лавой. Ближайшие несspectator сущности в радиусе10 получают Wither II на60000с; отдельные команды наносят Instant Damage II группам unholy и прочим mobs. [Функция](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/deviltotem.mcfunction), [условия срабатывания](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/devil_totem.json)
### `lbc:other/deviltotem0`

**Фактическая работа.** Каждые5 секунд, если тотем `deviltotem:1` находится в основной или второй руке, для каждого игрока-врага в пределах0,1…20 (не spectator, не союзник) создаёт четыре Wither Skeleton вокруг этого игрока. Если хоть один такой враг найден, владелец получает Instant Damage II на1с. Тотем здесь не расходуется; вызов не блокируется stopper-marker. [Функция](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/deviltotem0.mcfunction), [периодический вызов](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/players_5s.mcfunction)
### `lbc:other/dildomet`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/craft_ground.mcfunction`, условие/команда: `execute if entity @s[nbt={Item:{components:{"minecraft:custom_data":{fastexplode:1}}}}] if entity @e[type=minecraft:item,distance=0.001..1,limit=1,nbt={Item:{components:{"minecraft:custom_data":{fastfired:1}}}}] run function lbc:other/dildomet`. Результат: создаёт на месте результат loot table `lbc:dildomet`; создаёт частицы `block{block_state:{Name:"minecraft:pink_wool"}}`; проигрывает звук `minecraft:entity.player.levelup` для @a; проигрывает звук `minecraft:entity.pig.death` для @a; удаляет выбранную сущность. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/dildomet.mcfunction)

### `lbc:other/dildomet_inv`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/craft/inventory_craft_check.mcfunction`, условие/команда: `execute if items entity @s player.crafting.* *[minecraft:custom_data~{fastexplode:1}] if items entity @s player.crafting.* *[minecraft:custom_data~{fastfired:1}] run function lbc:other/dildomet_inv`. Результат: создаёт на месте результат loot table `lbc:dildomet`; создаёт частицы `block{block_state:{Name:"minecraft:pink_wool"}}`; проигрывает звук `minecraft:entity.player.levelup` для @a; проигрывает звук `minecraft:entity.pig.death` для @a. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/dildomet_inv.mcfunction)

### `lbc:other/drown`

**Фактическая работа.** `realdrownpre` вызывает эту функцию только при Water Breathing amplifier1. В воде снимается Wither и даются Strength II и Resistance III на1с; на воздухе — Wither IV на3с. Эффекты проверяются по текущему блоку у исполнителя. [Эта ветка](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/drown.mcfunction), [проверка уровня](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/realdrownpre.mcfunction)
### `lbc:other/drown2`

**Фактическая работа.** `realdrownpre` вызывает эту функцию только при Water Breathing amplifier2. В воде снимается Wither и даются Strength III и Resistance IV на1с; на воздухе накладываются Wither IV и Poison IV на3с. Эффекты проверяются по текущему блоку у исполнителя. [Эта ветка](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/drown2.mcfunction), [проверка уровня](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/realdrownpre.mcfunction)
### `lbc:other/drownpre`

**Фактическая работа.** Файл пуст: источник не накладывает эффект и не меняет состояние. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/drownpre.mcfunction)

Команд в источнике нет.

### `lbc:other/dynamite`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/projectilestick_tnt.mcfunction`, условие/команда: `execute if entity @s[tag=dynamite] run function lbc:other/dynamite`. Результат: меняет NBT/components: `modify entity @s fuse set value 1s`; меняет tag: `@s remove dynamite`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/dynamite.mcfunction)

### `lbc:other/effect_allseeingeye`

**Фактическая работа.** Раз в игровой тик при положительном `all_seeing_eye` и отсутствии `stopper_magic` marker в пределах20 выполняет подсветку мобов/игроков частицами, вызывает внешний эффект частиц powerhelmet, очищает у владельца Darkness, Blindness и Nausea, выдаёт advancement и уменьшает счётчик на1. Следовательно, очистка дебаффов и подсветка продолжаются только пока счётчик положительный и не подавлены marker; предмет в этой функции не расходуется. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/effect_allseeingeye.mcfunction)
### `lbc:other/effect_allseeingeye_shaped_player`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/effect_allseeingeye.mcfunction`, условие/команда: `execute at @a[gamemode=!spectator,distance=0.01..] run function lbc:other/effect_allseeingeye_shaped_player`. Результат: создаёт частицы `entity_effect{color:[0.0,0.01,0.0,1.0]}`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/effect_allseeingeye_shaped_player.mcfunction)

### `lbc:other/effect_gigaponos`

**Фактическая работа.** Каждый тик при положительном `gigaponos` создаёт частицы, играет звук и выбрасывает один результат loot table `lbc:govno`, затем уменьшает счётчик на1; дополнительные звуки включаются на точных значениях 350, 300, 250, 200, 150 и100. Это повторяется до нуля; функция не списывает инвентарный предмет. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/effect_gigaponos.mcfunction)
### `lbc:other/effect_living_legend`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/players_tick.mcfunction`, условие/команда: `execute if score @s living_legend matches 1.. unless entity @e[type=minecraft:marker,distance=..20,tag=stopper_magic,limit=1] run function lbc:other/effect_living_legend`. Результат: изменяет блоки: `fill ~3 ~-1 ~3 ~-3 ~-1 ~-3 minecraft:white_stained_glass replace #minecraft:airs strict`; изменяет блоки: `fill ~3 ~-1 ~3 ~-3 ~-1 ~-3 minecraft:red_stained_glass replace #minecraft:airs strict`; изменяет блоки: `fill ~3 ~-1 ~3 ~-3 ~-1 ~-3 minecraft:black_stained_glass replace #minecraft:airs strict`; изменяет блоки: `fill ~3 ~-1 ~3 ~-3 ~-1 ~-3 minecraft:blue_stained_glass replace #minecraft:airs strict`; изменяет блоки: `fill ~3 ~-1 ~3 ~-3 ~-1 ~-3 minecraft:light_blue_stained_glass replace #minecraft:airs strict`; изменяет блоки: `fill ~3 ~-1 ~3 ~-3 ~-1 ~-3 minecraft:magenta_stained_glass replace #minecraft:airs strict`; изменяет блоки: `fill ~3 ~-1 ~3 ~-3 ~-1 ~-3 minecraft:purple_stained_glass replace #minecraft:airs strict`; изменяет блоки: `fill ~3 ~-1 ~3 ~-3 ~-1 ~-3 minecraft:yellow_stained_glass replace #minecraft:airs strict`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/effect_living_legend.mcfunction)

### `lbc:other/effect_nomagic`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/items/swords.mcfunction`, условие/команда: `execute at @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{antimagic:1}}}}] as @a[gamemode=!spectator,distance=0.01..7,nbt={HurtTime:10s},predicate=!lbc:same_team] run function lbc:other/effect_nomagic`. Результат: меняет scoreboard: `scoreboard players add @s nomagic 15`; меняет scoreboard: `scoreboard players set @s mana 0`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/effect_nomagic.mcfunction)

### `lbc:other/effect_shifting`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/players_1s.mcfunction`, условие/команда: `execute at @s[scores={dimensional_shifting=1..}] run function lbc:other/effect_shifting`. Результат: передаёт выполнение `lbc:projectiles/teleport_nahui`; меняет scoreboard: `scoreboard players remove @s dimensional_shifting 1`; меняет advancement: `advancement grant @s only lbc:true_advancements/potions/effect_shifting`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/effect_shifting.mcfunction)

### `lbc:other/effect_skill_issue`

**Фактическая работа.** Пока `skill_issue>0` и поблизости нет stopper/stopper_magic markers, сравнивает текущий yaw/pitch игрока с поворотом, сохранённым при предыдущем проходе: движение камеры более чем на7 градусов вызывает 1 generic damage и Blindness на2 секунды. Затем новая Rotation сохраняется по ключу из UUID[0], advancement выдаётся, а счётчик уменьшается на1; прочие детали зависят от точности целочисленных scoreboard-углов. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/effect_skill_issue.mcfunction), [helpers](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/effect_skill_issue_2-0.mcfunction)
### `lbc:other/effect_skill_issue_2-0`

**Фактическая работа.** Macro читает `$(uid).Rotation[0]` и `[1]` из storage и сохраняет значения с scale 1 в scoreboard `rot0old`/`rot1old`. Это передача старого направления следующему этапу обработки, не самостоятельный эффект игроку. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/effect_skill_issue_2-0.mcfunction)

### `lbc:other/effect_skill_issue_2`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/effect_skill_issue.mcfunction`, условие/команда: `function lbc:other/effect_skill_issue_2 with storage lbc.math`. Результат: меняет NBT/components: `modify storage $(uid) Rotation set from entity @s Rotation`; использует macro-параметр `$(uid)`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/effect_skill_issue_2.mcfunction)

### `lbc:other/effect_skill_issue_gangbang`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/effect_skill_issue.mcfunction`, условие/команда: `execute unless score rot0 lbc.math matches -7..7 unless score rot0 lbc.math matches 180.. unless score rot0 lbc.math matches ..-180 run function lbc:other/effect_skill_issue_gangbang`. Результат: наносит 1 урона @s типа `generic`; накладывает `blindness` на @s на 2 секунд, amplifier 0. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/effect_skill_issue_gangbang.mcfunction)

### `lbc:other/effect_void_resistance`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/players_1s.mcfunction`, условие/команда: `execute at @s[scores={void_resistance=1..}] run function lbc:other/effect_void_resistance`. Результат: перемещает @s в `~ 320 ~`; меняет scoreboard: `scoreboard players remove @s void_resistance 1`; меняет advancement: `advancement grant @s only lbc:true_advancements/potions/effect_void_resistance`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/effect_void_resistance.mcfunction)

### `lbc:other/extraheal`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/players_5t.mcfunction`, условие/команда: `execute if entity @s[tag=demonkishki_active,scores={foodlvl=16..,hplvl=..4}] unless entity @e[type=minecraft:marker,distance=..20,tag=stopper_magic,limit=1] run function lbc:other/extraheal`. Результат: меняет advancement: `advancement grant @s only lbc:true_advancements/accesories/devilstomach`; накладывает `regeneration` на @s на 2 секунд, amplifier 10; накладывает `hunger` на @s на 10 секунд, amplifier 255; проигрывает звук `minecraft:entity.wither.ambient` для @a. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/extraheal.mcfunction)

### `lbc:other/fakepig`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/mobs_tick_lbc.mcfunction`, условие/команда: `execute if entity @s[type=minecraft:pig,tag=fakepig,nbt={HurtTime:2s}] unless score time_stopped lbc.math matches 1.. run function lbc:other/fakepig`. Результат: планирует отложенный вызов `lbc:other/fakepig1 5t`; создаёт сущность `lightning_bolt` с Tags ["bolt"]. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/fakepig.mcfunction)

### `lbc:other/fakepig1`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/fakepig.mcfunction`, условие/команда: `schedule function lbc:other/fakepig1 5t`. Результат: меняет NBT/components: `merge entity @s {CustomNameVisible:1b,Health:50.0f,CustomName:{"color":"red","italic":false,"text":"Fake Lucky Pig"},equipment`; меняет NBT/components: `modify entity @s angry_at set from entity @p[gamemode=!spectator] UUID`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/fakepig1.mcfunction)

### `lbc:other/fire_drop_kfc`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/fire_pre.mcfunction`, условие/команда: `execute if score fire lbc.math matches 0 run function lbc:other/fire_drop_kfc`. Результат: проигрывает звук `minecraft:entity.player.levelup` для @a; создаёт частицы `cloud`; создаёт на месте результат loot table `lbc:kfc_yumyumyummy`; удаляет выбранную сущность. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/fire_drop_kfc.mcfunction)

### `lbc:other/give`

**Фактическая работа.** Автоматический caller не найден; при ручном запуске выполняются описанные действия. Результат: выдаёт loot table `lbc:luckyblock_block_only` игроку; выдаёт loot table `lbc:luckyblock_loli_block` игроку; выдаёт loot table `lbc:luckyblock_neko_block` игроку. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/give.mcfunction)

### `lbc:other/give_shulker`

**Фактическая работа.** Reward запускается, когда несspectator игрок вне Nexus/Imprinted открывает контейнер с loot table `lbc:infinity_upgraded_luck`. Функция создаёт у него результат отдельной таблицы `lbc:infinity_upgraded_only_shulker_luck` и отзывает advancement. [Функция](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/give_shulker.mcfunction), [условия контейнера](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/generate_shulker.json)

### `lbc:other/gownoleggings`

**Фактическая работа.** Автоматический caller не найден; при ручном запуске выполняются описанные действия. Результат: меняет scoreboard: `scoreboard players operation *tempt lbcID2 = @s team_number`; накладывает `poison` на @e[distance=0.01..10,tag=!spectator,predicate=!lbc:same_team] на 2 секунд, amplifier 0; накладывает `nausea` на @e[distance=0.01..10,tag=!spectator,predicate=!lbc:same_team] на 2 секунд, amplifier 0. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/gownoleggings.mcfunction)

### `lbc:other/groundbreak`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/projectiles/prein.mcfunction`, условие/команда: `execute if entity @s[tag=groundbreaker] run function lbc:other/groundbreak`. Результат: изменяет блоки: `fill ~0.1 ~0.1 ~0.1 ~-0.1 ~-0.1 ~-0.1 minecraft:air destroy`; создаёт частицы `explosion`; проигрывает звук `minecraft:entity.generic.explode` для @a; удаляет выбранную сущность. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/groundbreak.mcfunction)

### `lbc:other/groundbreak_light`

**Фактическая работа.** Автоматический caller не найден; при ручном запуске выполняются описанные действия. Результат: изменяет блоки: `fill ~0.1 ~0.1 ~0.1 ~-0.1 ~-0.1 ~-0.1 minecraft:air destroy`; создаёт частицы `explosion`; проигрывает звук `minecraft:entity.generic.explode` для @a; удаляет выбранную сущность. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/groundbreak_light.mcfunction)

### `lbc:other/hohlopitek`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/mobs_5t_lbc.mcfunction`, условие/команда: `execute if entity @s[type=minecraft:hoglin,tag=hohlopitek] unless entity @e[type=marker,tag=stopper,distance=..20] run function lbc:other/hohlopitek`. Результат: создаёт сущность `tnt` с Tags ["groundbam"], fuse 100 тиков; перемещает @s в `^ ^ ^1 ~ ~`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/hohlopitek.mcfunction)

### `lbc:other/ice`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/raycasters_5t.mcfunction`, условие/команда: `execute at @s[tag=iceblock] run function lbc:other/ice`. Результат: изменяет блоки: `fill ~ ~ ~ ~ ~1 ~ minecraft:packed_ice strict`; изменяет блоки: `fill ~ ~ ~ ~ ~1 ~ minecraft:air destroy`; перемещает @e[type=#minecraft:mobs,distance=..2,limit=1,nbt={active_effects:[{id:"minecraft:slowness",amplifier:11b}]}] в `~ ~-.5 ~`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/ice.mcfunction)

### `lbc:other/idgive`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/players_1s.mcfunction`, условие/команда: `execute unless score @s lbcID2 matches 5.. run function lbc:other/idgive`. Результат: меняет scoreboard: `scoreboard players add *id.cloud lbcID2 1`; меняет scoreboard: `scoreboard players operation @s lbcID2 = *id.cloud lbcID2`; меняет tag: `@s remove identified`; меняет tag: `@s remove hasownid`; передаёт выполнение `lbc:resets`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/idgive.mcfunction)

### `lbc:other/idgivenoreset`

**Фактическая работа.** Автоматический caller не найден; при ручном запуске выполняются описанные действия. Результат: меняет scoreboard: `scoreboard players add *id.cloud lbcID2 1`; меняет scoreboard: `scoreboard players operation @s lbcID2 = *id.cloud lbcID2`; меняет tag: `@s remove identified`; меняет tag: `@s remove hasownid`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/idgivenoreset.mcfunction)

### `lbc:other/infinite_totem`

**Фактическая работа.** Когда срабатывает тотем с чарами Infinity I и `transformable:1,totem:1` вне Nexus/Imprinted, reward создаёт рядом предметы из loot table `lbc:infinite_totem` и отзывает advancement для повторного срабатывания. Сам datapack здесь не копирует тотем напрямую: содержимое выдаёт loot table. [Функция](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/infinite_totem.mcfunction), [триггер](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/infinite_totem.json)

### `lbc:other/konserva`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/players_2s.mcfunction`, условие/команда: `execute at @s[scores={foodlvl=..19}] if items entity @s container.* *[minecraft:custom_data~{konserva:1}] run function lbc:other/konserva`. Результат: накладывает `saturation` на @s на 1 секунд, amplifier 100; создаёт частицы `item{item:{id:"minecraft:cooked_beef"}}`; создаёт частицы `item{item:{id:"minecraft:cooked_porkchop"}}`; создаёт частицы `item{item:{id:"minecraft:cooked_rabbit"}}`; создаёт частицы `item{item:{id:"minecraft:cooked_salmon"}}`; создаёт частицы `item{item:{id:"minecraft:cooked_mutton"}}`; создаёт частицы `item{item:{id:"minecraft:potato"}}`; создаёт частицы `item{item:{id:"minecraft:baked_potato"}}`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/konserva.mcfunction)

### `lbc:other/lightning`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/tick2smobs.mcfunction`, условие/команда: `execute if entity @s[tag=lightningist,tag=!spectator] run function lbc:other/lightning`. Результат: меняет tag: `@s remove lightningist`; меняет tag: `@e[type=#minecraft:mobs,distance=0.001..16,tag=!lightningist,tag=!spectator] add lightningist`; создаёт сущность `lightning_bolt`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/lightning.mcfunction)

### `lbc:other/liquidvoiddamage`

**Фактическая работа.** Игрок, у которого в inventory есть предмет с `custom_data.liquidvoid:1`, обслуживается раз в2 секунды. Если не включён legacy-режим, функция дополнительно требует `swrg.math #lbcskill=1` и `lbc.skill=2`; при провале возвращает ошибку-команду. Число таких предметов считывается через `clear ... 0` (они не удаляются), передаётся macro и наносит владельцу столько `outside_border` damage, если у него нет `void_resistance`; повторные срабатывания продолжаются по таймеру players_2s. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/liquidvoiddamage.mcfunction), [macro hit](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/liquidvoiddamage_recur.mcfunction)
### `lbc:other/liquidvoiddamage_recur`

**Фактическая работа.** Macro использует переданное `tmp` для урона `@s` типа `outside_border`; удар пропускается, если `void_resistance` ≥1. Каждый вызов — один шаг урона, повтор/параметры задаёт caller. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/liquidvoiddamage_recur.mcfunction)

### `lbc:other/luckyblockspawn`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/raycasters_1s.mcfunction`, условие/команда: `execute if entity @s[tag=futureluckyblock] run function lbc:other/luckyblockspawn`. Результат: создаёт частицы `dust{color:[1.0,1.0,0.0],scale:2.0f}`; изменяет блоки: `setblock ~ ~0.5 ~ minecraft:red_mushroom_block[up=false,down=false,north=false,south=false,east=false,west=fa`; удаляет выбранную сущность. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/luckyblockspawn.mcfunction)

### `lbc:other/malevich_fill`

**Фактическая работа.** Для каждого пустого слота hotbar0–8 и main inventory9–35 помещает один предмет из `lbc:malevich_rnd`; затем отдельно проверяет четыре слота брони и пустую вторую руку. Максимум —41 предмет за вызов; занятые слоты пропускаются, расхода предметов здесь нет. Автоматического caller в изученных данных не найдено. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/malevich_fill.mcfunction)
### `lbc:other/metal_pipe`

**Фактическая работа.** Автоматический caller не найден; при ручном запуске выполняются описанные действия. Результат: передаёт выполнение `lbc:other/metal_pipe_in_air`; передаёт выполнение `lbc:other/metal_pipe_2`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/metal_pipe.mcfunction)

### `lbc:other/metal_pipe_2`

**Фактическая работа.** Когда `metal_pipe` ранее отметил падение и текущая сущность приземлилась, снимает `in_air_now`, семь раз проигрывает звук и наносит 25 урона falling_anvil. Удар выполняется отдельно с позиции каждой nearby mob сущности в пределах2.5, каждый раз выбирая ближайшего моба в радиусе2.5 от этой позиции; один и тот же моб может получить несколько попаданий. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/metal_pipe_2.mcfunction)
### `lbc:other/metal_pipe_in_air`

**Фактическая работа.** При запуске `metal_pipe` для сущности в воздухе helper добавляет `in_air_now` и ставит `PickupDelay` Short20, то есть20 игровых тиков (1с), а не20 секунд. При следующем запуске на земле этот тег включает `metal_pipe_2`; автоматического caller для `metal_pipe` в изученных данных не найдено. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/metal_pipe_in_air.mcfunction)

### `lbc:other/mirror`

**Фактическая работа.** Для `hometp.id` marker ставит под собой светло-голубое стекло, очищает блоки на высоте +1 и +2 и вызывает `forceload add` для текущего чанка. В этой функции нет команды снять принудительную загрузку; длительность такой загрузки здесь не ограничена. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/mirror.mcfunction)
### `lbc:other/mom`

**Фактическая работа.** Raycaster `mom` разворачивается к ближайшему несspectator-игроку и телепортируется на1 блок вперёд. После телепорта команда ставит огонь в исходной позиции выполнения функции, а не у нового места; затем из шести вариантов выбирает звук смерти свиньи, коровы или овцы. [Функция](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/mom.mcfunction), [вызов](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/raycasters_1s.mcfunction)

### `lbc:other/nitroboots`

**Фактическая работа.** Файл пуст: этот ресурс не задаёт эффект ботинок. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nitroboots.mcfunction)

Команд в источнике нет.

### `lbc:other/pidor`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/supernovacore.mcfunction`, условие/команда: `execute as @e[type=minecraft:marker,tag=supernova] at @s run function lbc:other/pidor`. Результат: меняет NBT/components: `modify storage lbc.math clonetonexus set value clonetonexus`; передаёт выполнение `lbc:other/pidor_yep` с параметрами из storage `lbc.math`; создаёт сущность `lightning_bolt`; удаляет выбранную сущность. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/pidor.mcfunction)

### `lbc:other/pidor_clone_rand`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/pidor_yep_3.mcfunction`, условие/команда: `function lbc:other/pidor_clone_rand with storage lbc.math`. Результат: использует macro-параметр `$(tmpx)`; использует macro-параметр `$(tmpy)`; использует macro-параметр `$(tmpz)`; изменяет блоки: `clone ~10 ~10 ~10 ~-10 ~-10 ~-10 to nexus $(tmpx) $(tmpy) $(tmpz)`; передаёт выполнение `lbc:other/nexus/spawn_cube`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/pidor_clone_rand.mcfunction)

### `lbc:other/pidor_yep`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/pidor.mcfunction`, условие/команда: `execute unless block ~ ~-0.6 ~ #airs_only run function lbc:other/pidor_yep with storage lbc.math`. Результат: использует macro-параметр `$(clonetonexus)`; создаёт сущность `lightning_bolt`; создаёт частицы `dust{color:[1.0,1.0,1.0],scale:4.0f}`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/pidor_yep.mcfunction)

### `lbc:other/pidor_yep_2`

**Фактическая работа.** Автоматический caller не найден; при ручном запуске выполняются описанные действия. Результат: изменяет блоки: `setblock 10241025 ~-10 10241025 minecraft:air`; изменяет блоки: `setblock 10241025 ~-10 10241025 minecraft:stone`; передаёт выполнение `lbc:other/pidor_yep_3`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/pidor_yep_2.mcfunction)

### `lbc:other/pidor_yep_3`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/pidor_yep_2.mcfunction`, условие/команда: `execute if score tmp1 lbc.math matches 1 run function lbc:other/pidor_yep_3`. Результат: передаёт выполнение `lbc:other/pidor_clone_rand` с параметрами из storage `lbc.math`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/pidor_yep_3.mcfunction)

### `lbc:other/ponos`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/supernova.mcfunction`, условие/команда: `execute if score #gamemode swrg.math matches 3..4 at @s run return run function lbc:other/ponos`. Результат: проигрывает звук `minecraft:lbcsounds.ponos` для @a; проигрывает звук `minecraft:lbcsounds.yametekudasai` для @a; проигрывает звук `minecraft:lbcsounds.dick_1` для @a; проигрывает звук `minecraft:lbcsounds.weewee` для @a; проигрывает звук `minecraft:lbcsounds.spank` для @a; проигрывает звук `minecraft:lbcsounds.woo` для @a; проигрывает звук `minecraft:lbcsounds.suction` для @a; проигрывает звук `minecraft:lbcsounds.vomiting` для @a. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/ponos.mcfunction)

### `lbc:other/powerhelmet`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/mobs_5t_lbc.mcfunction`, условие/команда: `execute if entity @s[type=minecraft:skeleton,tag=cyborg_boss,predicate=lbc:chance5] unless entity @e[type=marker,tag=stopper,distance=..20] run function lbc:other/powerhelmet`. Результат: накладывает `resistance` на @s на 2 секунд, amplifier 4; создаёт частицы `electric_spark`; проигрывает звук `minecraft:block.beacon.activate` для @a. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/powerhelmet.mcfunction)

### `lbc:other/purify`

**Фактическая работа.** Эффект Raid Omen amplifier43 вне Nexus/Imprinted запускает reward. Он очищает все эффекты, снимает теги `lightningist` и `greed_cursed`, сбрасывает таймеры, заряды и выбранные состояния, включая кулдауны вертолёта, заморозку, цель и накопленные удары; затем отзывает advancement. Функция не тратит предмет сама. [Функция](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/purify.mcfunction), [триггер эффекта](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/purify.json)
### `lbc:other/random_mob_spawn`

**Фактическая работа.** Выбирает целое 1..90; при `final_luck_modifier=7` фиксирует выбор на 22. Из 90 ветвей `return run summon` срабатывает только соответствующая выбранному числу, поэтому появляется одна сущность, а не весь список. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/random_mob_spawn.mcfunction)

### `lbc:other/reactive_boots`

**Фактическая работа.** Каждые2 тика обслуживает игрока с `reactiveboots_active`, если рядом нет `stopper`: снимает Levitation, вызывая подпроцедуру только при sneak+в воздухе, и на земле восполняет `reactivefuel` до20. При надетых реактивных leggings добавляет ещё единицу в диапазонах 0–9 и 10–19; если надеты и leggings, и chestplate — повторно добавляет единицу во втором диапазоне. Предмет функция не расходует; топливо тратится при срабатывании levitation. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/reactive_boots.mcfunction)
### `lbc:other/reactive_boots2`

**Фактическая работа.** Только при приседании в воздухе даёт Slow Falling на10 секунд и частицы; затем при положительном `reactivefuel` выбирает стандартный levitation helper либо ветвь mega. Счётчик `tmp` устанавливается через `execute store result ... if items`, то есть это результат проверки (0/1), а не количество надетых частей; поэтому условие mega `tmp matches 4..` штатным успешным проходом недостижимо, и доступен только обычный helper. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/reactive_boots2.mcfunction), [ветви](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/reactiveboots.mcfunction)
### `lbc:other/reactive_shish_place`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/players_5t.mcfunction`, условие/команда: `execute if entity @s[scores={coldownshish=19}] unless entity @e[type=minecraft:marker,distance=..20,tag=stopper_magic,limit=1] run function lbc:other/reactive_shish_place`. Результат: изменяет блоки: `fill ~1 319 ~1 ~-1 319 ~-1 minecraft:spruce_leaves[persistent=false]`; изменяет блоки: `fill ~1 ~-2 ~1 ~-1 ~-2 ~-1 minecraft:spruce_leaves[persistent=false]`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/reactive_shish_place.mcfunction)

### `lbc:other/reactiveboots`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/reactive_boots2.mcfunction`, условие/команда: `execute unless score @s reactivefuel matches ..0 if score tmp lbc.math matches ..3 at @s run function lbc:other/reactiveboots`. Результат: проигрывает звук `minecraft:block.fire.extinguish` для @a; создаёт частицы `flame`; накладывает `levitation` на @s на 1 секунд, amplifier 15; меняет scoreboard: `scoreboard players remove @s reactivefuel 1`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/reactiveboots.mcfunction)

### `lbc:other/reactiveboots_mega`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/reactive_boots2.mcfunction`, условие/команда: `execute unless score @s reactivefuel matches ..0 if score tmp lbc.math matches 4.. run function lbc:other/reactiveboots_mega`. Результат: проигрывает звук `minecraft:block.fire.extinguish` для @a; создаёт частицы `flame`; накладывает `levitation` на @s на 1 секунд, amplifier 30; меняет scoreboard: `scoreboard players remove @s reactivefuel 1`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/reactiveboots_mega.mcfunction)

### `lbc:other/realdrownpre`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/mobs_5t.mcfunction`, условие/команда: `execute if data entity @s[tag=!spectator] active_effects[{id:"minecraft:water_breathing"}] run function lbc:other/realdrownpre`. Результат: передаёт выполнение `lbc:other/drown`; передаёт выполнение `lbc:other/drown2`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/realdrownpre.mcfunction)

### `lbc:other/sand_blow`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/raycastselector_other.mcfunction`, условие/команда: `execute if entity @s[tag=sand_blow,scores={Lifetime=100}] run return run function lbc:other/sand_blow`. Результат: передаёт выполнение `lbc:other/sand_blow_fill_small`; передаёт выполнение `lbc:other/sand_blow_fill_big`; удаляет выбранную сущность. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sand_blow.mcfunction)

### `lbc:other/sand_blow_fill_big`

**Фактическая работа.** Если текущий блок не входит в `#minecraft:airs`, очищает объём от абсолютной Y=0 до относительной Y=текущая+3 с `destroy`; X/Z идут от −0 до текущей позиции. Это заметно отличается от `sand_blow_fill_small`, где весь вертикальный диапазон задан относительно anchor. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sand_blow_fill_big.mcfunction)
### `lbc:other/sand_blow_fill_small`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/sand_blow.mcfunction`, условие/команда: `execute if block ~ ~ ~ #minecraft:airs run function lbc:other/sand_blow_fill_small`. Результат: изменяет блоки: `fill ~ ~ ~ ~ ~3 ~ minecraft:air destroy`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sand_blow_fill_small.mcfunction)

### `lbc:other/shishfill`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/raycastselector_instant.mcfunction`, условие/команда: `execute if entity @s[tag=explodein,scores={Lifetime=3}] run return run function lbc:other/shishfill`. Результат: изменяет блоки: `fill ~2 ~7 ~2 ~-2 ~-5 ~-2 minecraft:spruce_leaves replace #minecraft:tannerkivi strict`; изменяет блоки: `fill ~5 ~4 ~2 ~-5 ~-2 ~-2 minecraft:spruce_leaves replace #minecraft:tannerkivi strict`; изменяет блоки: `fill ~2 ~4 ~5 ~-2 ~-2 ~-5 minecraft:spruce_leaves replace #minecraft:tannerkivi strict`; изменяет блоки: `fill ~3 ~6 ~3 ~-3 ~-3 ~-3 minecraft:spruce_leaves replace #minecraft:tannerkivi strict`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/shishfill.mcfunction)

### `lbc:other/shishgun`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/craft_ground.mcfunction`, условие/команда: `execute if entity @s[nbt={Item:{components:{"minecraft:custom_data":{railgun_bow:1}}}}] if entity @e[type=minecraft:item,distance=0.001..1,limit=1,nbt={Item:{components:{"minecraft:custom_data":{reactive_shish:1}}}}] run function lbc:other/shishgun`. Результат: создаёт на месте результат loot table `lbc:shishgun`; создаёт частицы `dust{color:[0.31,0.78,0.373],scale:2.0f}`; проигрывает звук `minecraft:item.trident.riptide_1` для @a; проигрывает звук `minecraft:item.trident.riptide_2` для @a; проигрывает звук `minecraft:item.trident.riptide_3` для @a; удаляет выбранную сущность. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/shishgun.mcfunction)

### `lbc:other/shishgun_inv`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/craft/inventory_craft_check.mcfunction`, условие/команда: `execute if items entity @s player.crafting.* *[minecraft:custom_data~{railgun_bow:1}] if items entity @s player.crafting.* *[minecraft:custom_data~{reactive_shish:1}] run function lbc:other/shishgun_inv`. Результат: создаёт на месте результат loot table `lbc:shishgun`; создаёт частицы `dust{color:[0.31,0.78,0.373],scale:2.0f}`; проигрывает звук `minecraft:item.trident.riptide_1` для @a; проигрывает звук `minecraft:item.trident.riptide_2` для @a; проигрывает звук `minecraft:item.trident.riptide_3` для @a. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/shishgun_inv.mcfunction)

### `lbc:other/shitkatana`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/craft_ground.mcfunction`, условие/команда: `execute if entity @s[nbt={Item:{components:{"minecraft:custom_data":{cyberkatana:1}}}}] if entity @e[type=minecraft:item,distance=0.001..1,limit=1,nbt={Item:{components:{"minecraft:custom_data":{govnoq:1}}}}] run function lbc:other/shitkatana`. Результат: создаёт на месте результат loot table `lbc:shit_katana`; создаёт частицы `dust{color:[1.0,1.0,0.0],scale:1.0f}`; проигрывает звук `minecraft:entity.zombie_villager.cure` для @a; проигрывает звук `minecraft:entity.player.breath` для @a; удаляет выбранную сущность. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/shitkatana.mcfunction)

### `lbc:other/shitkatana_inv`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/craft/inventory_craft_check.mcfunction`, условие/команда: `execute if items entity @s player.crafting.* *[minecraft:custom_data~{cyberkatana:1}] if items entity @s player.crafting.* *[minecraft:custom_data~{govnoq:1}] run function lbc:other/shitkatana_inv`. Результат: создаёт на месте результат loot table `lbc:shit_katana`; создаёт частицы `dust{color:[1.0,1.0,0.0],scale:1.0f}`; проигрывает звук `minecraft:entity.zombie_villager.cure` для @a; проигрывает звук `minecraft:entity.player.breath` для @a. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/shitkatana_inv.mcfunction)

### `lbc:other/shock`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/tick2smobs.mcfunction`, условие/команда: `execute if entity @s[tag=!spectator,scores={shock=1..}] run function lbc:other/shock`. Результат: создаёт сущность `lightning_bolt`; создаёт частицы `end_rod`; меняет scoreboard: `scoreboard players remove @s shock 1`; меняет advancement: `advancement grant @s[type=minecraft:player] only lbc:true_advancements/potions/effect_shock`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/shock.mcfunction)

### `lbc:other/silverfish_tick`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/mobs_tick_lbc.mcfunction`, условие/команда: `execute if entity @s[type=minecraft:silverfish] run function lbc:other/silverfish_tick`. Результат: передаёт выполнение `lbc:other/hedgehogarrow`; передаёт выполнение `lbc:other/baffledwall/tick`; передаёт выполнение `lbc:other/hedgehogarrow_upg`; передаёт выполнение `lbc:other/hedgehoghealer`; передаёт выполнение `lbc:other/hedgehogsniper`; передаёт выполнение `lbc:other/hedgehoglaser`; передаёт выполнение `lbc:workingitems/exopersoexplode1`; передаёт выполнение `lbc:swrg_kit_integration/game/skills/falling_hedgehog_explode`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/silverfish_tick.mcfunction)

### `lbc:other/small_fireball_byid`

**Фактическая работа.** Автоматический caller не найден; при ручном запуске выполняются описанные действия. Результат: меняет scoreboard: `scoreboard players operation *temp lbcID2 = @s lbcID2`; меняет scoreboard: `scoreboard players operation *tempt lbcID2 = @s team_number`; передаёт выполнение `lbc:other/raycastfireball`; создаёт частицы `dust{color:[1.0,0.949,0.208],scale:4.0f}`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/small_fireball_byid.mcfunction)

### `lbc:other/snus`

**Фактическая работа.** Очищает17 эффектов, затем даёт Speed XVI и Jump Boost XVI на200с, Nausea и Mining Fatigue III на250с, Hunger I на400с, затем Hunger XI на16с, и Night Vision на250с. Обычный обработчик `effects/effect_snus` также выдаёт достижение, уменьшает `effect_snus` и ставит `have_lucky_effects`; отдельный Baffledend-вызов применяет его ко всем выбранным `#minecraft:mobs` и планирует следующий этап через4с. [Функция](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/snus.mcfunction), [обычный caller](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/effects/effect_snus.mcfunction), [Baffledend-вызов](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/baffledend/1.mcfunction)
### `lbc:other/socializm`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/craft_ground.mcfunction`, условие/команда: `execute if entity @s[nbt={Item:{components:{"minecraft:custom_data":{socializm:1,hammer:1}}}}] if entity @e[type=minecraft:item,distance=0.001..1,limit=1,nbt={Item:{components:{"minecraft:custom_data":{socializm:1,sickle:1}}}}] run function lbc:other/socializm`. Результат: создаёт сущность `item` (предмет `minecraft:carrot_on_a_stick`); создаёт частицы `firework`; проигрывает звук `minecraft:entity.player.levelup` для @a; удаляет выбранную сущность. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/socializm.mcfunction)

### `lbc:other/socializm_inv`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/craft/inventory_craft_check.mcfunction`, условие/команда: `execute if items entity @s player.crafting.* *[minecraft:custom_data~{socializm:1,hammer:1}] if items entity @s player.crafting.* *[minecraft:custom_data~{socializm:1,sickle:1}] run function lbc:other/socializm_inv`. Результат: создаёт сущность `item` (предмет `minecraft:carrot_on_a_stick`); создаёт частицы `firework`; проигрывает звук `minecraft:entity.player.levelup` для @a. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/socializm_inv.mcfunction)

### `lbc:other/spawn_birch`

**Фактическая работа.** Raycast-вариант `birch` разбрасывает текущую сущность на позицию в пределах20 блоков, затем явно переносит исполнение к ней и ставит там feature `birch`; после этого удаляет сущность. Поэтому feature ставится в точке после телепорта, несмотря на то что `spreadplayers` сам не меняет исходную позицию функции. [Функция](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/spawn_birch.mcfunction), [вызов](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/raycastselector_instant.mcfunction)

### `lbc:other/spawnfireball`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/raycasters_1s.mcfunction`, условие/команда: `execute if entity @s[tag=blazespell] run function lbc:other/spawnfireball`. Результат: меняет tag: `@p[gamemode=!spectator] add rcdeny`; передаёт выполнение `lbc:other/raycastfireball`; меняет tag: `@a remove rcdeny`; создаёт частицы `dust{color:[1.0,0.949,0.208],scale:4.0f}`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/spawnfireball.mcfunction)

### `lbc:other/spawnfireballchick`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/raycasters_1s.mcfunction`, условие/команда: `execute if entity @s[tag=chickblazespell] run function lbc:other/spawnfireballchick`. Результат: передаёт выполнение `lbc:other/raycastfireballchick`; создаёт частицы `dust{color:[0.992,0.647,0.0],scale:4.0f}`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/spawnfireballchick.mcfunction)

### `lbc:other/speed_up`

**Фактическая работа.** При ботинках `nyauwu:1` и `mana < manamax` добавляет movement_speed modifier с UUID `916c89e6-ae62-4b11-91d7-10e7e57c726d`, operation `add_multiplied_total`, значение 1. При mana ≥ manamax этот modifier удаляется. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/speed_up.mcfunction)

### `lbc:other/stalin_shoot`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/stalin_shoot_pre.mcfunction`, условие/команда: `execute if score health lbc.math matches ..495 if score @s simpleTimer matches 0 if predicate lbc:chance50 if entity @e[type=#minecraft:mobs,distance=..40,tag=!spectator,tag=!stalin] run function lbc:other/stalin_shoot`. Результат: передаёт выполнение `lbc:workingitems/ak47_otdacha_stalin` с параметрами из storage `lbc.math`; проигрывает звук `minecraft:lbcsounds.shot` для @a[distance=..200]. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/stalin_shoot.mcfunction)

### `lbc:other/stalin_shoot_pre`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/mobs_5t_lbc.mcfunction`, условие/команда: `execute if entity @s[type=minecraft:zombified_piglin,tag=stalin] unless entity @e[type=marker,tag=stopper,distance=..20] run function lbc:other/stalin_shoot_pre`. Результат: передаёт выполнение `lbc:other/stalin_shoot`; передаёт выполнение `lbc:other/stalin_shoot_pre2`; меняет scoreboard: `scoreboard players remove @s simpleTimer 1`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/stalin_shoot_pre.mcfunction)

### `lbc:other/stalin_shoot_pre2`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/stalin_shoot_pre.mcfunction`, условие/команда: `execute if score health lbc.math matches ..495 unless score @s simpleTimer matches 0..16 run function lbc:other/stalin_shoot_pre2`. Результат: проигрывает звук `minecraft:lbcsounds.rasstrelat` для @a; меняет scoreboard: `scoreboard players set @s simpleTimer 16`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/stalin_shoot_pre2.mcfunction)

### `lbc:other/stopnuke`

**Фактическая работа.** Останавливает у всех игроков звук `minecraft:lbcsounds.nuke` через `stopsound @a master`. `stoppernuke` планирует этот вызов через `1s` (одну секунду). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/stopnuke.mcfunction)

### `lbc:other/supernova`

**Фактическая работа.** Рецепт объединяет `shit_katana` и `shish_gun`; при score gamemode3–4 вместо эффекта вызывает `ponos` и сразу возвращается. В остальных режимах создаёт пять `supernova` raycast markers и один `supernovacore`, разбрасывает пять markers максимум на50 блоков, создаёт TNT с fuse0 и молнию, запускает частицы и звуки, даёт ближайшему несspectator-игроку Levitation и Absorption на10000с и тег `balance_user`. Команда `execute at @e` рисует вспышку у каждой сущности; функция отключает block/entity/mob drops и включает forceload, но в ней нет сброса этих gamerules, cleanup markers или снятия forceload. [Выпадающие предметы](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/supernova.mcfunction), [крафт из сетки игрока](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/supernova_inv.mcfunction)
### `lbc:other/supernova_inv`

**Фактическая работа.** Crafting-grid-вариант требует оба предмета в слотах crafting.*; в отличие от брошенных предметов, ингредиенты уменьшаются через `count_reduce`, а Levitation/Absorption и `balance_user` назначаются самому игроку `@s`. В gamemode score3–4 команда `return` останавливает взрыв и вызывает только `ponos`; в остальных режимах создаётся такой же набор markers/TNT/молния и меняются те же gamerules, что и в варианте для выброшенных предметов. [Функция](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/supernova_inv.mcfunction), [инвентарный caller](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/inventory_craft_check.mcfunction)

### `lbc:other/target_curse`

**Фактическая работа.** Каждую секунду при положительном `target` у любого mob вне nexus/imprinted все проектiles наводятся лучом на ближайшую цель с положительным счётчиком, а все raycasters поворачиваются к цели. После прохода счётчик уменьшается на1 у каждой такой цели; это означает приблизительный срок эффекта по единицам score, если внешний код правильно выставил его. Raycast и его конечный hit выполняются общими helpers. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/target_curse.mcfunction)
### `lbc:other/target_curse_rc`

**Фактическая работа.** Для каждого raycaster поворачивает его к одной цели `target>0`; селектор не сортирует цели, поэтому конкретный адресат при нескольких целях не гарантирован как ближайший. Если lbcID2 или team_number raycaster совпадает со значением выбранного игрока, временно записывает −1, чтобы снять совпадение собственника/команды на последующей проверке. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/target_curse_rc.mcfunction)
### `lbc:other/teleport_chebosraki`

**Фактическая работа.** Автоматический caller не найден; при ручном запуске выполняются описанные действия. Результат: удаляет выбранную сущность; меняет NBT/components: `modify entity @s NoGravity set value 1b`; меняет NBT/components: `modify entity @s PersistenceRequired set value 1b`; передаёт выполнение `lbc:other/teleport_chebosraki_2` с параметрами из storage `lbc.math`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/teleport_chebosraki.mcfunction)

### `lbc:other/teleport_chebosraki_2`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/teleport_chebosraki.mcfunction`, условие/команда: `function lbc:other/teleport_chebosraki_2 with storage lbc.math`. Результат: использует macro-параметр `$(tmpx)`; использует macro-параметр `$(tmpy)`; использует macro-параметр `$(tmpz)`; перемещает @s в `$(tmpx) $(tmpy) $(tmpz)`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/teleport_chebosraki_2.mcfunction)

### `lbc:other/unstablearea`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/raycasters_5t.mcfunction`, условие/команда: `execute at @s[tag=unstablespell] run function lbc:other/unstablearea`. Результат: создаёт сущность `marker` с Tags ["raycaster_void_area","magic"]; меняет scoreboard: `scoreboard players set @e[type=minecraft:marker,distance=..0.001,tag=raycaster_void_area,limit=1] Distance 150`; передаёт выполнение `lbc:other/unstablearea_rc`; создаёт частицы `portal`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/unstablearea.mcfunction)

### `lbc:other/unstablearea_rc`

**Фактическая работа.** Синхронно проходит 150 шагов по0,5 блока (около75 блоков), очищая на каждом небольшую область вокруг луча, затем удаляет marker. На шагах со счётчиком101–150 поворот меняется на yaw1°, на51–100 — yaw5°/pitch−0,2°, на1–50 — yaw10°/pitch1°; начальные yaw/pitch выбираются случайно. Каждая функция рекурсивно вызывает следующую, поэтому это единый проход без планировщика между шагами; точное число затронутых координат зависит от округления fill-углов к блокам. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/unstablearea_rc.mcfunction), [инициализация](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/unstablearea.mcfunction)
### `lbc:other/waterpower`

**Фактическая работа.** Для сущности с Conduit Power amplifier1 заменяет воздух на воду `level=1` в объёме3×4×3 вокруг неё; при amplifier2 тот же объём заполняется source water. Поскольку это два независимых `if`, при более высоком amplifier2 выполняются обе команды, и вторая перезаписывает вновь поставленную воду на источники. Caller находится в `mobs_5t`; блоки кроме воздуха сохраняются. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/waterpower.mcfunction)
## Наведение, снаряды и попадания

### `lbc:other/arrowgive`

**Фактическая работа.** Уменьшает `arrowgens` на1 и рекурсивно повторяет это, пока счётчик положительный; сам предметов не создаёт. Другого вызова этой функции в изученном datapack не найдено. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/arrowgive.mcfunction)

### `lbc:other/arrowgive_pre`

**Фактическая работа.** Каждые15с вызывается, если у игрока есть `arrowgenerator` в контейнере или руке и рядом нет stopper_magic. `clear ... 0` считает предметы, но не забирает их; функция делает один loot-roll `lbc:arrows`, при появлении свежего `stand_arrow` даёт достижение Infinite Quiver и играет звук. Она записывает число генераторов в `arrowgens`, но не вызывает рекурсивный `arrowgive`, который иначе только уменьшил бы этот счётчик. [Функция](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/arrowgive_pre.mcfunction), [периодический caller](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/players_15s.mcfunction)

### `lbc:other/blownedbow`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/transformation.mcfunction`, условие/команда: `execute if entity @e[type=minecraft:tnt,distance=..7,limit=1,nbt={fuse:2s}] run function lbc:other/blownedbow`. Результат: создаёт сущность `item` (предмет `minecraft:crossbow`); создаёт частицы `explosion_emitter`; удаляет выбранную сущность. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/blownedbow.mcfunction)

### `lbc:other/blownedbow_old`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/transformation_old.mcfunction`, условие/команда: `execute if entity @e[type=minecraft:tnt,distance=..7,limit=1,nbt={fuse:2s}] run function lbc:other/blownedbow_old`. Результат: создаёт сущность `item` (предмет `minecraft:crossbow`); создаёт частицы `explosion_emitter`; удаляет выбранную сущность. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/blownedbow_old.mcfunction)

### `lbc:other/hedgehogarrow`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/silverfish_tick.mcfunction`, условие/команда: `execute if entity @s[tag=hedgehog] unless score @s simpleTimer matches 1.. run function lbc:other/hedgehogarrow`. Результат: меняет tag: `@e[type=#minecraft:mobs,distance=..0.001,tag=!spectator] add rcdeny`; передаёт выполнение `lbc:other/raycastarrow`; меняет tag: `@e remove rcdeny`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/hedgehogarrow.mcfunction)

### `lbc:other/hedgehogarrow_upg`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/silverfish_tick.mcfunction`, условие/команда: `execute if entity @s[tag=hedgehog_default] unless score @s simpleTimer matches 1.. run function lbc:other/hedgehogarrow_upg`. Результат: меняет tag: `@e[type=#minecraft:mobs,distance=..0.01,limit=1,team=!] add nodmg`; меняет tag: `@e[type=#minecraft:mobs,distance=..0.01,limit=1,team=!] add nodmg1`; передаёт выполнение `lbc:other/baffledwall/tick_rc_pl`; меняет tag: `@e remove nodmg`; меняет tag: `@e remove nodmg1`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/hedgehogarrow_upg.mcfunction)

### `lbc:other/rasengan`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/projectiles/eggs.mcfunction`, условие/команда: `execute if data entity @e[type=minecraft:snowball,sort=nearest,limit=1] Item.components.minecraft:custom_data{"rasengan":1} unless entity @e[type=minecraft:marker,distance=..24,tag=stopper_magic,limit=1] unless entity @a[distance=..12,nbt={Inventory:[{components:{"minecraft:custom_data":{mystical:1,type:2}}}]}] unless entity @a[gamemode=!spectator,distance=..20,tag=nomagic_active] positioned ~ ~1.62 ~ run function lbc:other/rasengan`. Результат: накладывает `resistance` на @a[gamemode=!spectator,distance=..4] на 1 секунд, amplifier 3; накладывает `resistance` на @s на 1 секунд, amplifier 4; создаёт сущность `creeper`; накладывает `levitation` на @s на 1 секунд, amplifier 0; накладывает `slow_falling` на @s на 10 секунд, amplifier 0. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/rasengan.mcfunction)

### `lbc:other/raycast_spike`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/projectiles/specials.mcfunction`, условие/команда: `execute if data entity @s Item.components.minecraft:custom_data{"spike_impulse":1} as @a[gamemode=!spectator,distance=..4,scores={snowball=1..},limit=1] at @s anchored eyes positioned ^ ^ ^ run function lbc:other/raycast_spike`. Результат: меняет scoreboard: `scoreboard players set @s Distance 0`; меняет tag: `@s add caster`; передаёт выполнение `lbc:other/raycast_spike_loop`; меняет tag: `@s remove caster`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/raycast_spike.mcfunction)

### `lbc:other/raycast_spike_loop`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/raycast_spike.mcfunction`, условие/команда: `function lbc:other/raycast_spike_loop`. Результат: создаёт частицы `dust{color:[0.824,0.89,0.894],scale:1.0f}`; меняет scoreboard: `scoreboard players add @s Distance 1`; наносит 7 урона @s типа `arrow`; передаёт выполнение `lbc:other/raycast_spike_loop`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/raycast_spike_loop.mcfunction)

### `lbc:other/raycastarrow`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/hedgehogarrow.mcfunction`, условие/команда: `execute facing entity @e[type=#minecraft:mobs,distance=0.1..15,tag=!rcdeny,tag=!spectator,tag=!nomagic_active,sort=nearest,limit=1,tag=!strict_map_object] eyes run function lbc:other/raycastarrow`. Результат: создаёт частицы `dust{color:[0.8,0.8,0.8],scale:1.0f}`; создаёт сущность `arrow` с Tags ["killedarrow","anhedgehogarrow","magic"]; создаёт сущность `marker` с Tags ["reikast"]; меняет NBT/components: `modify entity @e[type=minecraft:arrow,distance=..2,tag=anhedgehogarrow,limit=1] Owner set from entity @s UUID`; проигрывает звук `minecraft:entity.arrow.shoot` для @a; меняет tag: `@e[type=minecraft:arrow,distance=..2,tag=anhedgehogarrow,limit=1] remove anhedgehogarrow`; удаляет выбранную сущность. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/raycastarrow.mcfunction)

### `lbc:other/raycastfireball`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/small_fireball_byid.mcfunction`, условие/команда: `execute facing entity @e[type=#minecraft:mobs,tag=!rcdenyalways,tag=!spectator,tag=!nomagic_active,sort=nearest,limit=1,tag=!strict_map_object,predicate=!lbc:same_id,predicate=!lbc:same_team] eyes run function lbc:other/raycastfireball`. Результат: создаёт сущность `small_fireball` с Tags ["can_deleted"]; создаёт сущность `marker` с Tags ["reikast"]; передаёт выполнение `lbc:other/raycastfireball_2`; проигрывает звук `minecraft:entity.blaze.shoot` для @a; удаляет выбранную сущность. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/raycastfireball.mcfunction)

### `lbc:other/raycastfireball_2`

**Фактическая работа.** Передаёт три компонента Pos marker `reikast` в Motion ближайшего small fireball (`distance=..0.001`): `data get ×1000`, запись double `×0.001`. Это направляет снаряд вектором из marker. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/raycastfireball_2.mcfunction)

### `lbc:other/raycastfireball_2_big`

**Фактическая работа.** Копирует Pos[0..2] marker `reikast` в Motion большого fireball с коэффициентами чтения 1000 и записи 0.001, затем убирает временный тег `current`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/raycastfireball_2_big.mcfunction)

### `lbc:other/raycastfireballbig`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/big_fireball_byid.mcfunction`, условие/команда: `execute anchored eyes positioned ^ ^ ^ facing entity @e[type=#minecraft:mobs,tag=!rcdenyalways,tag=!spectator,tag=!nomagic_active,sort=nearest,limit=1,tag=!strict_map_object,predicate=!lbc:same_id,predicate=!lbc:same_team] eyes run function lbc:other/raycastfireballbig`. Результат: создаёт сущность `fireball` с Tags ["can_deleted","current"]; создаёт сущность `marker` с Tags ["reikast"]; передаёт выполнение `lbc:other/raycastfireball_2_big`; проигрывает звук `minecraft:entity.blaze.shoot` для @a; удаляет выбранную сущность. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/raycastfireballbig.mcfunction)

### `lbc:other/raycastfireballchick`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/spawnfireballchick.mcfunction`, условие/команда: `execute facing entity @e[type=#minecraft:mobs,tag=!chick,tag=!rcdenyalways,tag=!spectator,tag=!nomagic_active,sort=nearest,limit=1,tag=!strict_map_object] eyes run function lbc:other/raycastfireballchick`. Результат: создаёт сущность `small_fireball` с Tags ["can_deleted"]; создаёт сущность `marker` с Tags ["reikast"]; передаёт выполнение `lbc:other/raycastfireballchick_2`; проигрывает звук `minecraft:entity.blaze.shoot` для @a; удаляет выбранную сущность. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/raycastfireballchick.mcfunction)

### `lbc:other/raycastfireballchick_2`

**Фактическая работа.** Копирует координаты marker `reikast` в Motion ближайшего small fireball; чтение scale 1000 и запись double scale 0.0016. Селектор fireball ограничен `distance=..0.001`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/raycastfireballchick_2.mcfunction)

## Взрывы, огонь и воздействие на блоки

### `lbc:other/compressedtnt`

**Фактическая работа.** Маркер ставит под собой redstone block, создаёт 16 TNT без гравитации и удаляет себя. Четыре TNT получают fuse 60, четыре 65, четыре 70 и четыре 75 тиков (3–3,75 с); выполняется звуковой набор primed TNT. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/compressedtnt.mcfunction)

### `lbc:other/delta_tnt`

**Фактическая работа.** Ставит redstone block и создаёт 33 marker с `lifetime:1000` и тегами для raycast/mecha, затем проигрывает звуки primed TNT и удаляет исходный marker. Последующее поведение выполняют обработчики этих тегов. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/delta_tnt.mcfunction)

### `lbc:other/fire`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/fire_pre.mcfunction`, условие/команда: `execute if entity @s[nbt={HurtTime:9s}] run function lbc:other/fire`. Результат: передаёт выполнение `lbc:other/fire2`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/fire.mcfunction)

### `lbc:other/fire2`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/fire.mcfunction`, условие/команда: `execute if score #health lbc.math matches 1..20 run function lbc:other/fire2`. Результат: создаёт сущность `marker` с Tags ["chickblazespell","activatedblaze","rc_1sec","magic"]; создаёт частицы `dust{color:[1.0,0.961,0.604],scale:4.0f}`; удаляет выбранную сущность. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/fire2.mcfunction)

### `lbc:other/fire_pre`

**Фактическая работа.** Обслуживает chicken с тегом `chick` в mob tick, кроме периода time stop. При `HurtTime=9` (SNBT Short, девять игровых тиков, не секунд) запускает `fire`, создающий marker с lifetime60; когда счётчик `Fire` у курицы равен0, выбрасывает loot `lbc:kfc_yumyumyummy` и убивает её. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/fire_pre.mcfunction), [ветви](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/fire.mcfunction)
### `lbc:other/firedbow`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/transformation.mcfunction`, условие/команда: `execute if block ~ ~ ~ minecraft:fire run function lbc:other/firedbow`. Результат: создаёт сущность `item` (предмет `minecraft:crossbow`); создаёт сущность `lightning_bolt`; создаёт частицы `flame`; изменяет блоки: `fill ~1 ~1 ~1 ~-1 ~-1 ~-1 minecraft:air replace minecraft:fire strict`; удаляет выбранную сущность. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/firedbow.mcfunction)

### `lbc:other/firedbow_old`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/transformation_old.mcfunction`, условие/команда: `execute if block ~ ~ ~ minecraft:fire run function lbc:other/firedbow_old`. Результат: создаёт сущность `item` (предмет `minecraft:crossbow`); создаёт сущность `lightning_bolt`; создаёт частицы `flame`; изменяет блоки: `fill ~1 ~1 ~1 ~-1 ~-1 ~-1 minecraft:air replace minecraft:fire strict`; удаляет выбранную сущность. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/firedbow_old.mcfunction)

### `lbc:other/groundbam`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/projectilestick_tnt.mcfunction`, условие/команда: `execute if entity @s[tag=groundbam,nbt={OnGround:1b}] run function lbc:other/groundbam`. Результат: меняет NBT/components: `merge entity @s {fuse:2s}`; передаёт выполнение `lbc:projectiles/arrows/evoker`; передаёт выполнение `lbc:other/groundbam_fire`; создаёт сущность `lightning_bolt`; создаёт сущность `tnt`, fuse 0 тиков; передаёт выполнение `lbc:other/groundbam_no`; меняет tag: `@s remove groundbam`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/groundbam.mcfunction)

### `lbc:other/groundbam_fire`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/groundbam.mcfunction`, условие/команда: `execute if entity @s[tag=fire] run function lbc:other/groundbam_fire`. Результат: изменяет блоки: `fill ~5 ~5 ~5 ~-5 ~-5 ~-5 minecraft:fire replace #minecraft:airs`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/groundbam_fire.mcfunction)

### `lbc:other/groundbam_no`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/groundbam.mcfunction`, условие/команда: `execute if entity @s[tag=no_explode] run function lbc:other/groundbam_no`. Результат: создаёт частицы `explosion`; проигрывает звук `minecraft:entity.generic.explode` для @a; удаляет выбранную сущность. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/groundbam_no.mcfunction)

## Крафт, сборка и преобразование предметов

### `lbc:other/craft_flying_boots`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/craft_ground.mcfunction`, условие/команда: `execute if entity @s[nbt={Item:{id:"minecraft:leather_boots",components:{"minecraft:custom_data":{jumpboost:1,boots:1},"minecraft:item_model":"lbc:armors/hermes_boots"}}}] if entity @e[distance=0.001..1,limit=1,nbt={Item:{id:"minecraft:leather_boots",components:{"minecraft:custom_data":{reactive:1,boots:1},"minecraft:item_model":"lbc:armors/reactive"}}}] run function lbc:other/craft_flying_boots`. Результат: создаёт на месте результат loot table `lbc:neko_set_steampunk/boots_spectral`; создаёт частицы `dust{color:[0.655,1.0,0.702],scale:2.0f}`; проигрывает звук `minecraft:block.enchantment_table.use` для @a; удаляет выбранную сущность. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft_flying_boots.mcfunction)

### `lbc:other/craft_flying_boots_inv`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/craft/inventory_craft_check.mcfunction`, условие/команда: `execute if items entity @s player.crafting.* minecraft:leather_boots[minecraft:item_model="lbc:armors/hermes_boots"] if items entity @s player.crafting.* minecraft:leather_boots[minecraft:custom_data~{reactive:1,boots:1},minecraft:item_model="lbc:armors/reactive"] run function lbc:other/craft_flying_boots_inv`. Результат: создаёт на месте результат loot table `lbc:neko_set_steampunk/boots_spectral`; создаёт частицы `dust{color:[0.655,1.0,0.702],scale:2.0f}`; проигрывает звук `minecraft:block.enchantment_table.use` для @a. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft_flying_boots_inv.mcfunction)

### `lbc:other/craft_ground`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/tick1s.mcfunction`, условие/команда: `execute as @e[type=minecraft:item,nbt={Item:{components:{"minecraft:custom_data":{craftusable:1}}}}] at @s if entity @e[type=minecraft:item,distance=0.001..1] unless dimension minecraft:nexus unless dimension minecraft:imprinted run function lbc:other/craft_ground`. Результат: передаёт выполнение `lbc:other/socializm`; передаёт выполнение `lbc:other/dildomet`; передаёт выполнение `lbc:other/shitkatana`; передаёт выполнение `lbc:other/shishgun`; передаёт выполнение `lbc:other/supernova`; передаёт выполнение `lbc:other/craft_flying_boots`; передаёт выполнение `lbc:other/sauvojen_tuntija/crafted_greed`; передаёт выполнение `lbc:other/sauvojen_tuntija/crafted_avarice`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft_ground.mcfunction)

### `lbc:other/craft_uniwand_with_grimoire`

**Фактическая работа.** При найденном рядом grimoire с `libspell:1` увеличивает spellcount wand, вычисляет делитель floor(spellcount/5) (не меньше 1) и добавляет соответствующую долю маны. Копирует spellid/lore, обновляет tier/value и уменьшает предмет через `reduce_count_l`; обработка повторяется, пока grimoire остаётся. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft_uniwand_with_grimoire.mcfunction)

### `lbc:other/craft_uniwand_with_grimoire_get_tags`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/craft_uniwand_with_grimoire.mcfunction`, условие/команда: `function lbc:other/craft_uniwand_with_grimoire_get_tags with storage lbc.math`. Результат: меняет NBT/components: `modify entity @s Item.components.minecraft:custom_data.spellcount set value $(grim)`; использует macro-параметр `$(grim)`; меняет NBT/components: `modify entity @s Item.components.minecraft:custom_data.spell$(grim) set from entity @e[type=item,nbt={Item:{components:{"minecr`; меняет NBT/components: `modify entity @s Item.components.minecraft:lore[4] set value {"translate":"mana_usage","extra":[{"text":"$(mana)","italic":fals`; использует macro-параметр `$(mana)`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft_uniwand_with_grimoire_get_tags.mcfunction)

### `lbc:other/craft_uniwand_with_grimoire_tier_check`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/craft_uniwand_with_grimoire.mcfunction`, условие/команда: `execute as @e[type=minecraft:item,distance=..1,sort=nearest,limit=1,nbt={Item:{components:{"minecraft:custom_data":{libspell:1}}}}] run function lbc:other/craft_uniwand_with_grimoire_tier_check`. Результат: меняет scoreboard: `scoreboard players remove value lbc.math 99`; меняет scoreboard: `scoreboard players remove value lbc.math 9`; меняет scoreboard: `scoreboard players add value lbc.math 1`; меняет scoreboard: `scoreboard players add value lbc.math 10`; меняет scoreboard: `scoreboard players add value lbc.math 100`; меняет scoreboard: `scoreboard players add value lbc.math 1000`; меняет scoreboard: `scoreboard players add value lbc.math 10000`; меняет scoreboard: `scoreboard players add value lbc.math 100000`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft_uniwand_with_grimoire_tier_check.mcfunction)

### `lbc:other/craft_uniwand_with_grimoire_value_check`

**Фактическая работа.** По `value` применяет к wand ровно один tier modifier: ≤−11 ff, −10..−1 f, 0..9 d, 10..99 c, 100..999 b, 1000..9999 a, 10000..99999 s, 100000..999999 ss, от 1000000 x. Каждая ветвь завершается `return`, поэтому выбран только один tier. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft_uniwand_with_grimoire_value_check.mcfunction)

### `lbc:other/transform`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/transformpre.mcfunction`, условие/команда: `execute if entity @e[type=minecraft:lightning_bolt,distance=..4] run function lbc:other/transform`. Результат: выдаёт loot table `lbc:transformables_accessory` игроку; выдаёт loot table `lbc:transformables_axe` игроку; выдаёт loot table `lbc:transformables_bow` игроку; выдаёт loot table `lbc:transformables_chestplate` игроку; выдаёт loot table `lbc:transformables_hoe` игроку; выдаёт loot table `lbc:transformables_pickaxe` игроку; выдаёт loot table `lbc:transformables_potion` игроку; выдаёт loot table `lbc:transformables_shield` игроку. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/transform.mcfunction)

### `lbc:other/transformation`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/items_tick_act.mcfunction`, условие/команда: `execute if entity @s[tag=fast] run function lbc:other/transformation`. Результат: передаёт выполнение `lbc:other/firedbow`; передаёт выполнение `lbc:other/blownedbow`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/transformation.mcfunction)

### `lbc:other/transformation_old`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/items_tick_act.mcfunction`, условие/команда: `execute if entity @s[tag=fast_old] run function lbc:other/transformation_old`. Результат: передаёт выполнение `lbc:other/firedbow_old`; передаёт выполнение `lbc:other/blownedbow_old`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/transformation_old.mcfunction)

### `lbc:other/transformator`

**Фактическая работа.** Каждые5 тиков обслуживает zombie `transformator`, если рядом нет stopper. С шансом10% запускает электрический залп из пяти raycast markers с lifetime30 тиков (1,5 секунды); два независимых шанса по5% дают Instant Health I и II (мгновенные эффекты), а при порогах Health≤490/480/470/460/450/440 добавляются случайные частицы/дым/звуки/огонь; при≤440 marker helper запускает взрывной конец и убивает сущность. Пороги читаются целым scoreboard через `data get Health`, поэтому это дискретные значения, не дробные интервалы. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/transformator.mcfunction), [взрывной конец](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/transformator_explode.mcfunction)
### `lbc:other/transformator_explode`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/transformator.mcfunction`, условие/команда: `execute if score tr_health lbc.math matches ..440 run function lbc:other/transformator_explode`. Результат: создаёт сущность `fireball`; изменяет блоки: `setblock ~ ~-1 ~ minecraft:dirt`; удаляет выбранную сущность. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/transformator_explode.mcfunction)

### `lbc:other/transformator_particle`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/transformator.mcfunction`, условие/команда: `execute if score tr_health lbc.math matches ..490 if predicate lbc:chance50 run function lbc:other/transformator_particle`. Результат: проигрывает звук `minecraft:entity.creeper.primed` для @a; создаёт частицы `campfire_signal_smoke`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/transformator_particle.mcfunction)

### `lbc:other/transformator_shoot`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/transformator.mcfunction`, условие/команда: `execute if predicate lbc:chance10 run function lbc:other/transformator_shoot`. Результат: создаёт сущность `marker` с Tags ["slowraycast","raycastelectro","tuntija","shield_blockable","can_deleted","mecha"]; проигрывает звук `minecraft:entity.firework_rocket.twinkle` для @a; перемещает @s в `~ ~ ~ facing entity @e[type=#minecraft:mobs,distance=..30,tag=!transformator,tag=!spectator,sort=nea`; меняет tag: `@e[type=minecraft:marker,distance=..2,tag=raycastelectro,tag=!actived,limit=7] add actived`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/transformator_shoot.mcfunction)

### `lbc:other/transformpre`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/items_tick_act.mcfunction`, условие/команда: `execute if entity @s[tag=transformable] run function lbc:other/transformpre`. Результат: меняет NBT/components: `modify entity @s Health set value 1000000`; передаёт выполнение `lbc:other/transform`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/transformpre.mcfunction)

## Служебный исправитель контейнеров

### `lbc:other/datafixer`

**Фактическая работа.** Автоматический caller в изученном пакете не найден. Ручной вызов выполняет 63 750 последовательных `data remove block ... LootTableSeed`: относительный куб координат 0…39, но файл обрывается внутри края при X=39,Y=33,Z=29; всё выше по Y=34…39 и часть этого ребра не включены. Функция не меняет сами блоки и не перегенерирует лут, только удаляет поле seed у каждого затронутого block entity; такой массовый вызов очень тяжёлый и исполняется командной цепочкой без разбиения на тики. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/datafixer.mcfunction)
### `lbc:other/datafixer`

**Фактическая работа.** Вспомогательный скрипт; Minecraft не исполняет его как runtime-функцию. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/datafixer.py)

## Мана, экипировка и состояния игрока

### `lbc:other/food_counter`

**Фактическая работа.** Каждое употребление еды из `#minecraft:all_food` вне Nexus/Imprinted увеличивает `food_count` на1; reward сразу отзывает advancement, чтобы считать следующую еду. [Функция](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/food_counter.mcfunction), [условия consume_item](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/gluttony.json)

### `lbc:other/infinitestewused`

**Фактическая работа.** Употребление предмета с `inf:1` вне Nexus/Imprinted награждает игрока одной cooked beef `infinite_steak` с тем же `inf:1` и стилем `lbc:tier_c`; reward отзывает advancement, сохраняя цепочку повторного употребления. [Функция](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/infinitestewused.mcfunction), [триггер](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/stewused.json)

### `lbc:other/infinitestewused_old`

**Фактическая работа.** Употребление предмета с `inf_old:1` вне Nexus/Imprinted ставит игроку `scheduled_eating`, планирует через1 тик выдачу rabbit stew `horn` с тем же `inf_old:1`, затем отзывает advancement. Отложенная функция заменяет main hand только у игроков с этим тегом и снимает тег. [Reward](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/infinitestewused_old.mcfunction), [отложенная выдача](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/infinitestewused_old_sch.mcfunction), [триггер](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/old_stew_used.json)

### `lbc:other/infinitestewused_old_sch`

**Фактическая работа.** Заменяет main hand игроков с тегом `scheduled_eating` на rabbit stew `{inf_old:1}` с именем `horn`, затем снимает тег. Задержку задаёт caller. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/infinitestewused_old_sch.mcfunction)

### Цикл маны: подробный разбор

Точный разбор автоматического восстановления, отрицательной маны, порогов, overload-веток, эффектов и вложенных tentacles вынесен в [ручной аудит mana handlers](lbc_mana.md). Ниже оставлены исходники из этого job; таблицы расходящихся условий и количеств следует читать по указанному разбору.

### `lbc:other/manaregen`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/players_tick.mcfunction`, условие/команда: `execute unless score legacy lbc.math matches 1 unless score @s nomagic matches 1.. unless entity @e[type=minecraft:marker,distance=..20,tag=stopper_magic,limit=1] run function lbc:other/manaregen`. Результат: передаёт выполнение `lbc:other/overload_crux/give`; меняет tag: `@s remove obtained_crux`; меняет scoreboard: `scoreboard players operation @s manaregen_visual = @s manaregen`; передаёт выполнение `lbc:other/manaregen_mana_belts`; передаёт выполнение `lbc:other/manaregen_mage_armor`; меняет scoreboard: `scoreboard players operation daeris lbc.math = @s manaregen_visual`; передаёт выполнение `lbc:other/manaregen_mana_boost`; меняет scoreboard: `scoreboard players operation daeris lbc.math -= @s manaregen_visual`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/manaregen.mcfunction)

### `lbc:other/manaregen_belt_stacked`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/manaregen_belt_stacked.mcfunction`, условие/команда: `execute if score manabelts lbc.math matches 1.. run function lbc:other/manaregen_belt_stacked`. Результат: меняет scoreboard: `scoreboard players add @s mana 1`; меняет scoreboard: `scoreboard players remove manabelts lbc.math 1`; передаёт выполнение `lbc:other/manaregen_belt_stacked`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/manaregen_belt_stacked.mcfunction)

### `lbc:other/manaregen_hit`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/items/swords.mcfunction`, условие/команда: `execute at @s if items entity @s armor.chest *[minecraft:custom_data~{nyauwu:1}] unless entity @e[type=minecraft:marker,distance=..20,tag=stopper_magic,limit=1] run function lbc:other/manaregen_hit`. Результат: меняет scoreboard: `scoreboard players operation @s manaregen_visual *= 70 lbc.math`; передаёт выполнение `lbc:other/manaregen_true`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/manaregen_hit.mcfunction)

### `lbc:other/manaregen_inwater`

**Фактическая работа.** Файл пуст: при ручном и автоматическом запуске не меняет состояние. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/manaregen_inwater.mcfunction)

Команд в источнике нет.

### `lbc:other/manaregen_mage_armor`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/manaregen.mcfunction`, условие/команда: `execute if items entity @s armor.* *[minecraft:custom_data~{mage:1}] run function lbc:other/manaregen_mage_armor`. Результат: меняет scoreboard: `scoreboard players add @s manaregen_visual 4`; меняет scoreboard: `scoreboard players add @s manaregen_visual 6`; меняет scoreboard: `scoreboard players add @s manaregen_visual 7`; меняет scoreboard: `scoreboard players add @s manamax 20`; меняет scoreboard: `scoreboard players operation @s manaregen_visual += @s manaregen_visual`; передаёт выполнение `lbc:other/manaregen_true_pre`; передаёт выполнение `lbc:other/speed_up`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/manaregen_mage_armor.mcfunction)

### `lbc:other/manaregen_mana_belts`

**Фактическая работа.** Считает предметы `manabelt:1` в инвентаре через `clear ... 0`, умножает их число на2 и добавляет эту величину к `manaregen_visual`; сами пояса не удаляются. Если полученная добавка превышает базовый `manaregen`, выдаётся advancement аксессуара. Основной проход `manaregen` дальше прибавляет визуальное значение к mana с учётом reverse_mana и ограничивает положительную mana значением manamax. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/manaregen_mana_belts.mcfunction), [основной расчёт](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/manaregen.mcfunction)
### `lbc:other/manaregen_mana_boost`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/manaregen.mcfunction`, условие/команда: `execute if score @s effect_boost_mana matches 1.. run function lbc:other/manaregen_mana_boost`. Результат: меняет advancement: `advancement grant @s only lbc:true_advancements/potions/effect_magic_buff`; меняет scoreboard: `scoreboard players operation @s manaregen_visual *= 3 lbc.math`; меняет scoreboard: `scoreboard players operation @s manaregen_visual /= 2 lbc.math`; меняет scoreboard: `scoreboard players remove @s effect_boost_mana 1`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/manaregen_mana_boost.mcfunction)

### `lbc:other/manaregen_true`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/manaregen.mcfunction`, условие/команда: `execute unless score @s mana >= @s manamax if score @s manaregen_visual matches 1.. run function lbc:other/manaregen_true`. Результат: меняет scoreboard: `scoreboard players operation @s[scores={reverse_mana=..0}] mana += @s manaregen_visual`; меняет scoreboard: `scoreboard players operation @s mana = @s manamax`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/manaregen_true.mcfunction)

### `lbc:other/manaregen_true_pre`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/manaregen_mage_armor.mcfunction`, условие/команда: `execute if items entity @s armor.chest *[minecraft:custom_data~{nyauwu:1}] if score @s mana >= @s manamax if block ~ ~ ~ #minecraft:water run function lbc:other/manaregen_true_pre`. Результат: меняет scoreboard: `scoreboard players add @s mana 1`; меняет scoreboard: `scoreboard players operation @s[scores={reverse_mana=..0}] mana += @s manaregen_visual`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/manaregen_true_pre.mcfunction)

### `lbc:other/maxmana`

**Фактическая работа.** Считает разницу `mana−manamax`; только если она меньше `manaregen_visual`, устанавливает mana ровно в manamax. При разнице, равной или большей regen, ничего не меняет. Непосредственный автоматический caller в проверенном наборе не найден. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/maxmana.mcfunction)
### `lbc:other/negative_mana`

**Фактическая работа.** Обработчик делит отрицательную ману на диапазоны: −1…−5000 → `negative_mana_1`, −5001…−10000 → `_2`, −10001…−15000 → `_3`, −15001…−20000 → `_4`, ≤−20001 → `_5`; эти ветви применяются только без `overload_helmet_active`. При теге шлема ≤−20001 выбирается `negative_mana_5_helmet`, а `overload_chestplate_active` при mana≤−1 отдельно запускает сопротивление. Advancement `oh_no_mana` выдаётся, когда mana, manaregen_visual и manamax все ≤−1. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/negative_mana.mcfunction)

### `lbc:other/negative_mana_1`

**Фактическая работа.** Накладывает на `@s` Slowness II: эффект `minecraft:slowness`, длительность 1 секунда, amplifier 1. Условий на блок в этой функции нет; она сама не выдаёт предмет. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/negative_mana_1.mcfunction)

### `lbc:other/negative_mana_2`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/negative_mana.mcfunction`, условие/команда: `execute if score @s[tag=!overload_helmet_active] mana matches -10000..-5001 run function lbc:other/negative_mana_2`. Результат: накладывает `slowness` на @s на 1 секунд, amplifier 2; накладывает `mining_fatigue` на @s на 1 секунд, amplifier 0; накладывает `weakness` на @s на 1 секунд, amplifier 0. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/negative_mana_2.mcfunction)

### `lbc:other/negative_mana_3`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/negative_mana.mcfunction`, условие/команда: `execute if score @s[tag=!overload_helmet_active] mana matches -15000..-10001 run function lbc:other/negative_mana_3`. Результат: накладывает `slowness` на @s на 1 секунд, amplifier 6; накладывает `mining_fatigue` на @s на 1 секунд, amplifier 2; накладывает `nausea` на @s на 4 секунд, amplifier 0; накладывает `weakness` на @s на 1 секунд, amplifier 1; накладывает `hunger` на @s на 1 секунд, amplifier 0. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/negative_mana_3.mcfunction)

### `lbc:other/negative_mana_4`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/negative_mana.mcfunction`, условие/команда: `execute if score @s[tag=!overload_helmet_active] mana matches -20000..-15001 run function lbc:other/negative_mana_4`. Результат: накладывает `slowness` на @s на 1 секунд, amplifier 127; накладывает `mining_fatigue` на @s на 1 секунд, amplifier 127; накладывает `nausea` на @s на 6 секунд, amplifier 0; накладывает `weakness` на @s на 1 секунд, amplifier 127; накладывает `hunger` на @s на 1 секунд, amplifier 2. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/negative_mana_4.mcfunction)

### `lbc:other/negative_mana_5`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/negative_mana.mcfunction`, условие/команда: `execute if score @s[tag=!overload_helmet_active] mana matches ..-20001 run function lbc:other/negative_mana_5`. Результат: накладывает `slowness` на @s на 1 секунд, amplifier 127; накладывает `mining_fatigue` на @s на 1 секунд, amplifier 127; накладывает `nausea` на @s на 6 секунд, amplifier 0; накладывает `weakness` на @s на 1 секунд, amplifier 127; накладывает `blindness` на @s на 2 секунд, amplifier 0; накладывает `hunger` на @s на 1 секунд, amplifier 6; меняет атрибут: `attribute @s minecraft:max_health get`; меняет scoreboard: `scoreboard players operation tmp1 lbc.math = @s mana`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/negative_mana_5.mcfunction)

### `lbc:other/negative_mana_5_helmet`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/negative_mana.mcfunction`, условие/команда: `execute if score @s[tag=overload_helmet_active] mana matches ..-20001 run function lbc:other/negative_mana_5_helmet`. Результат: меняет атрибут: `attribute @s minecraft:max_health get`; меняет scoreboard: `scoreboard players operation tmp1 lbc.math = @s mana`; меняет scoreboard: `scoreboard players operation tmp1 lbc.math /= @s manamax`; меняет scoreboard: `scoreboard players operation tmp lbc.math -= tmp1 lbc.math`; передаёт выполнение `lbc:other/negative_mana/explode`; передаёт выполнение `lbc:damage_with_tmp_void` с параметрами из storage `lbc.math`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/negative_mana_5_helmet.mcfunction)

### `lbc:other/negative_mana_resist`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/negative_mana.mcfunction`, условие/команда: `execute if score @s[tag=overload_chestplate_active] mana matches ..-1 run function lbc:other/negative_mana_resist`. Результат: накладывает `resistance` на @s на 1 секунд, amplifier 0; накладывает `resistance` на @s на 1 секунд, amplifier 1; накладывает `resistance` на @s на 1 секунд, amplifier 2; накладывает `resistance` на @s на 1 секунд, amplifier 3; накладывает `resistance` на @s на 1 секунд, amplifier 4. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/negative_mana_resist.mcfunction)

### `lbc:other/twitching`

**Фактическая работа.** Каждые2 тика игрок с положительным `twitching` получает независимые случайные события: звук при chance1, стеклянные частицы при chance10, выбор действия при chance50, слизневый звук при chance40; счётчик уменьшается на1, если рядом нет `stopper`. Выбор действия равновероятен из шести ветвей (доля 1/6 каждой после chance50): включить `RC_Raycast`, бросить подходящее зелье/снаряд из лука, разрядить арбалет либо кинуть снежок/эндер-жемчуг. Расход определяется типом: стрела уменьшается на1 (кроме infinite_arrow), снежок/жемчуг на1, а зелья забираются целиком; предметы превращаются в летящие сущности с движением по взгляду. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/twitching.mcfunction), [выбор](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/twitching_random.mcfunction)
### `lbc:other/twitching_bottle_fill`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/twitching_sound.mcfunction`, условие/команда: `execute if entity @s[nbt={Inventory:[{id:"minecraft:glass_bottle"}]}] run function lbc:other/twitching_bottle_fill`. Результат: создаёт на месте результат loot table `lbc:succubus_potion`; расходует/очищает matching item. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/twitching_bottle_fill.mcfunction)

### `lbc:other/twitching_bow`

**Фактическая работа.** Выстреливает обычной стрелой из лука: создаёт её на полблока перед глазами, задаёт Owner и поворот игрока, а helper скорости строит движение по этому повороту. Списывает одну стрелу, кроме игрока с любым `infinite_arrow` в инвентаре; ставит `bow=1`, `bow_charge=30` и выдаёт advancement `crossbow`. Ветвь доступна при случайном исходе3, если у игрока есть стрела и выбран лук. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/twitching_bow.mcfunction), [вектор движения](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/twitching_bow_arrow_motion.mcfunction)
### `lbc:other/twitching_bow_arrow_motion`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/twitching_bow.mcfunction`, условие/команда: `execute rotated as @s as @e[type=minecraft:arrow,tag=vpered_rc,tag=!active,sort=nearest,limit=1,distance=..4] run function lbc:other/twitching_bow_arrow_motion`. Результат: меняет NBT/components: `modify entity @s Motion set value [0.0d,0.0d,0.0d]`; создаёт сущность `marker` с Tags ["reycaster"]; удаляет выбранную сущность; проигрывает звук `minecraft:entity.arrow.shoot` для @a; меняет tag: `@s add active`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/twitching_bow_arrow_motion.mcfunction)

### `lbc:other/twitching_crossbow`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/twitching_random.mcfunction`, условие/команда: `execute if score #random6 lbc.math matches 4 if data entity @s[nbt={SelectedItem:{id:"minecraft:crossbow"}}] SelectedItem.components."minecraft:charged_projectiles" run function lbc:other/twitching_crossbow`. Результат: создаёт сущность `arrow` с Tags ["vpered_rc"]; создаёт сущность `spectral_arrow` с Tags ["vpered_rc"]; создаёт сущность `firework_rocket` (предмет `minecraft:firework_rocket`) с Tags ["vpered_rc"]; меняет NBT/components: `modify entity @e[type=#crossbow_charges,tag=vpered_rc,tag=!active,sort=nearest,limit=1] Owner set from entity @s UUID`; меняет NBT/components: `modify entity @e[type=#crossbow_charges,tag=vpered_rc,tag=!active,sort=nearest,limit=1] item set from entity @s SelectedItem.co`; меняет NBT/components: `modify entity @e[type=firework_rocket,tag=vpered_rc,tag=!active,sort=nearest,limit=1] FireworksItem.components."minecraft:firew`; передаёт выполнение `lbc:other/twitching_bow_arrow_motion`; меняет scoreboard: `scoreboard players set @s bow 1`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/twitching_crossbow.mcfunction)

### `lbc:other/twitching_ender_pearl`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/twitching_pearl_sel.mcfunction`, условие/команда: `execute if entity @s[nbt={SelectedItem:{id:"minecraft:ender_pearl"}}] run function lbc:other/twitching_ender_pearl`. Результат: создаёт сущность `ender_pearl` с Tags ["vpered_rc"]; меняет NBT/components: `modify entity @e[type=minecraft:ender_pearl,tag=vpered_rc,tag=!active,sort=nearest,limit=1] Owner set from entity @s UUID`; передаёт выполнение `lbc:other/twitching_random_snowball_motion_big`; меняет scoreboard: `scoreboard players remove itemcount lbc.math 1`; заменяет предмет/слот: `entity @s weapon.mainhand with minecraft:air`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/twitching_ender_pearl.mcfunction)

### `lbc:other/twitching_ender_pearl_left`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/twitching_pearl_sel.mcfunction`, условие/команда: `execute if items entity @s weapon.offhand minecraft:ender_pearl run function lbc:other/twitching_ender_pearl_left`. Результат: создаёт сущность `ender_pearl` с Tags ["vpered_rc"]; меняет NBT/components: `modify entity @e[type=minecraft:ender_pearl,tag=vpered_rc,tag=!active,sort=nearest,limit=1] Owner set from entity @s UUID`; передаёт выполнение `lbc:other/twitching_random_snowball_motion_big`; меняет scoreboard: `scoreboard players remove itemcount lbc.math 1`; заменяет предмет/слот: `entity @s weapon.offhand with minecraft:air`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/twitching_ender_pearl_left.mcfunction)

### `lbc:other/twitching_pearl_sel`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/twitching_random.mcfunction`, условие/команда: `execute if score #random6 lbc.math matches 6 run function lbc:other/twitching_pearl_sel`. Результат: передаёт выполнение `lbc:other/twitching_ender_pearl`; передаёт выполнение `lbc:other/twitching_ender_pearl_left`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/twitching_pearl_sel.mcfunction)

### `lbc:other/twitching_random`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/twitching.mcfunction`, условие/команда: `execute if predicate lbc:chance50 run function lbc:other/twitching_random`. Результат: меняет scoreboard: `scoreboard players set @s RC_Raycast 1`; передаёт выполнение `lbc:other/twitching_random_potion`; передаёт выполнение `lbc:other/twitching_bow`; передаёт выполнение `lbc:other/twitching_crossbow`; передаёт выполнение `lbc:other/twitching_snowball_sel`; передаёт выполнение `lbc:other/twitching_pearl_sel`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/twitching_random.mcfunction)

### `lbc:other/twitching_random_potion`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/twitching_random.mcfunction`, условие/команда: `execute if score #random6 lbc.math matches 2 run function lbc:other/twitching_random_potion`. Результат: передаёт выполнение `lbc:other/twitching_random_potion_rc_splash`; передаёт выполнение `lbc:other/twitching_random_potion_rc_splash_left`; передаёт выполнение `lbc:other/twitching_random_potion_rc_lingering`; передаёт выполнение `lbc:other/twitching_random_potion_rc_lingering_left`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/twitching_random_potion.mcfunction)

### `lbc:other/twitching_random_potion_motion`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/twitching_random_potion_rc_lingering.mcfunction`, условие/команда: `execute rotated as @s as @e[type=minecraft:lingering_potion,tag=vpered_rc,tag=!active,sort=nearest,limit=1] run function lbc:other/twitching_random_potion_motion`. Результат: меняет NBT/components: `modify entity @s Motion set value [0.0d,0.0d,0.0d]`; создаёт сущность `marker` с Tags ["reycaster"]; удаляет выбранную сущность; проигрывает звук `minecraft:entity.splash_potion.throw` для @a; меняет tag: `@s add active`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/twitching_random_potion_motion.mcfunction)

### `lbc:other/twitching_random_potion_rc_lingering`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/twitching_random_potion.mcfunction`, условие/команда: `execute if entity @s[nbt={SelectedItem:{id:"minecraft:lingering_potion"}}] run function lbc:other/twitching_random_potion_rc_lingering`. Результат: создаёт сущность `lingering_potion` (предмет `minecraft:lingering_potion`) с Tags ["vpered_rc"]; меняет NBT/components: `modify entity @e[type=minecraft:lingering_potion,tag=vpered_rc,tag=!active,sort=nearest,limit=1] Item.components."minecraft:pot`; заменяет предмет/слот: `entity @s weapon.mainhand with minecraft:air`; передаёт выполнение `lbc:other/twitching_random_potion_motion`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/twitching_random_potion_rc_lingering.mcfunction)

### `lbc:other/twitching_random_potion_rc_lingering_left`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/twitching_random_potion.mcfunction`, условие/команда: `execute if items entity @s weapon.offhand minecraft:lingering_potion run function lbc:other/twitching_random_potion_rc_lingering_left`. Результат: создаёт сущность `lingering_potion` (предмет `minecraft:lingering_potion`) с Tags ["vpered_rc"]; меняет NBT/components: `modify entity @e[type=minecraft:lingering_potion,tag=vpered_rc,tag=!active,sort=nearest,limit=1] Item.components."minecraft:pot`; заменяет предмет/слот: `entity @s weapon.offhand with minecraft:air`; передаёт выполнение `lbc:other/twitching_random_potion_motion`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/twitching_random_potion_rc_lingering_left.mcfunction)

### `lbc:other/twitching_random_potion_rc_splash`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/twitching_random_potion.mcfunction`, условие/команда: `execute if entity @s[nbt={SelectedItem:{id:"minecraft:potion"}}] run function lbc:other/twitching_random_potion_rc_splash`. Результат: создаёт сущность `splash_potion` (предмет `minecraft:splash_potion`) с Tags ["vpered_rc"]; меняет NBT/components: `modify entity @e[type=minecraft:splash_potion,tag=vpered_rc,tag=!active,sort=nearest,limit=1] Item.components."minecraft:potion`; заменяет предмет/слот: `entity @s weapon.mainhand with minecraft:air`; передаёт выполнение `lbc:other/twitching_random_potion_motion`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/twitching_random_potion_rc_splash.mcfunction)

### `lbc:other/twitching_random_potion_rc_splash_left`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/twitching_random_potion.mcfunction`, условие/команда: `execute if items entity @s weapon.offhand minecraft:potion run function lbc:other/twitching_random_potion_rc_splash_left`. Результат: создаёт сущность `splash_potion` (предмет `minecraft:splash_potion`) с Tags ["vpered_rc"]; меняет NBT/components: `modify entity @e[type=minecraft:splash_potion,tag=vpered_rc,tag=!active,sort=nearest,limit=1] Item.components."minecraft:potion`; заменяет предмет/слот: `entity @s weapon.offhand with minecraft:air`; передаёт выполнение `lbc:other/twitching_random_potion_motion`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/twitching_random_potion_rc_splash_left.mcfunction)

### `lbc:other/twitching_random_snowball_motion`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/twitching_snowball.mcfunction`, условие/команда: `execute rotated as @s as @e[type=minecraft:snowball,tag=vpered_rc,tag=!active,sort=nearest,limit=1] run function lbc:other/twitching_random_snowball_motion`. Результат: меняет NBT/components: `modify entity @s Motion set value [0.0d,0.0d,0.0d]`; создаёт сущность `marker` с Tags ["reycaster"]; удаляет выбранную сущность; проигрывает звук `minecraft:entity.snowball.throw` для @a; меняет tag: `@s add active`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/twitching_random_snowball_motion.mcfunction)

### `lbc:other/twitching_random_snowball_motion_big`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/twitching_ender_pearl.mcfunction`, условие/команда: `execute rotated as @s as @e[type=minecraft:ender_pearl,tag=vpered_rc,tag=!active,sort=nearest,limit=1] run function lbc:other/twitching_random_snowball_motion_big`. Результат: меняет NBT/components: `modify entity @s Motion set value [0.0d,0.0d,0.0d]`; создаёт сущность `marker` с Tags ["reycaster"]; удаляет выбранную сущность; проигрывает звук `minecraft:entity.snowball.throw` для @a; меняет tag: `@s add active`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/twitching_random_snowball_motion_big.mcfunction)

### `lbc:other/twitching_snowball`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/twitching_snowball_sel.mcfunction`, условие/команда: `execute if entity @s[nbt={SelectedItem:{id:"minecraft:snowball"}}] run function lbc:other/twitching_snowball`. Результат: создаёт сущность `snowball` с Tags ["vpered_rc"]; меняет NBT/components: `modify entity @e[type=minecraft:snowball,tag=vpered_rc,tag=!active,sort=nearest,limit=1] Item set from entity @s SelectedItem`; передаёт выполнение `lbc:other/twitching_random_snowball_motion`; меняет scoreboard: `scoreboard players remove itemcount lbc.math 1`; заменяет предмет/слот: `entity @s weapon.mainhand with minecraft:air`; меняет scoreboard: `scoreboard players set @s snowball 1`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/twitching_snowball.mcfunction)

### `lbc:other/twitching_snowball_left`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/twitching_snowball_sel.mcfunction`, условие/команда: `execute if items entity @s weapon.offhand minecraft:snowball run function lbc:other/twitching_snowball_left`. Результат: создаёт сущность `snowball` с Tags ["vpered_rc"]; меняет NBT/components: `modify entity @e[type=minecraft:snowball,tag=vpered_rc,tag=!active,sort=nearest,limit=1] Item set from entity @s equipment.offh`; передаёт выполнение `lbc:other/twitching_random_snowball_motion`; меняет scoreboard: `scoreboard players remove itemcount lbc.math 1`; заменяет предмет/слот: `entity @s weapon.offhand with minecraft:air`; меняет scoreboard: `scoreboard players set @s snowball 1`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/twitching_snowball_left.mcfunction)

### `lbc:other/twitching_snowball_sel`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/twitching_random.mcfunction`, условие/команда: `execute if score #random6 lbc.math matches 5 run function lbc:other/twitching_snowball_sel`. Результат: передаёт выполнение `lbc:other/twitching_snowball`; передаёт выполнение `lbc:other/twitching_snowball_left`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/twitching_snowball_sel.mcfunction)

### `lbc:other/twitching_sound`

**Фактическая работа.** Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/twitching.mcfunction`, условие/команда: `execute if predicate lbc:chance1 run function lbc:other/twitching_sound`. Результат: проигрывает звук `minecraft:lbcsounds.icum` для @a; проигрывает звук `minecraft:lbcsounds.yamai_desu` для @a; проигрывает звук `minecraft:lbcsounds.yamai_desu_2` для @a; проигрывает звук `minecraft:lbcsounds.yamai_desu_3` для @a; проигрывает звук `minecraft:lbcsounds.ooh` для @a; проигрывает звук `minecraft:lbcsounds.nyah` для @a; создаёт частицы `dust{color:[1.0,1.0,1.0],scale:1.0f}`; передаёт выполнение `lbc:other/twitching_bottle_fill`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/twitching_sound.mcfunction)


