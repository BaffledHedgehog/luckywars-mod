# LBC: lbc other misc

Задание `lbc_other_misc`: проверены ровно 214 источников из `agent_notes/inventory/research_jobs.json`. В описаниях caller означает обнаруженную ссылку в проиндексированных функциях; если его нет, возможность ручного запуска остаётся. Условие caller приведено из literal command. `dx/dy/dz` вместе с `distance` задают совместное пересечение, а `execute as` сам по себе не переносит позицию. `/effect give` измеряет длительность в секундах; `fuse:2s` в SNBT — Short на два тика (0,1 с), тогда как `schedule 2s` — две секунды.




## Baffled wall: выбор цели и варианты луча

`baffledwall/tick` сначала сохраняет команду стены, затем наводится на ближайшую цель-моба в пределах 10, исключая саму стену, spectator, strict_map_object и same_team. Основные `tick_rc` начинают луч с Distance 0 и звуковым сигналом; `tick_loop` двигается по воздуху шагами 0,3 блока и останавливается при блоке, цели или после Distance 50 (около 15 блоков). Семейство helper-loop задаёт разные задачи, но пересечение `dx=dy=dz=0,distance=..5` остаётся кубом 1×1×1 в точке команды: `tick_loop_dmg` наносит первому допустимому мобу 4 arrow damage и останавливает луч; вариант `tick_loop_dmg_big` наносит 20; `tick_loop_dmg_laser` наносит 0,1 generic без немедленного стопа. Дружеские варианты `tick_loop_pl_heal` лечат помеченную `nodmg` цель на 1 секунду instant_health при продвижении луча; `tick_loop_pl_laser` наносит немаркированным целям 0,02 generic на шагах, но ограничен Distance 30 (9 блоков); `tick_loop_pl_sniper` ищет цель до Distance 300 (90 блоков) и отдаёт 20 arrow damage с остановкой. Слово `pl` в имени не является доказательством переключения команды: условия задаются selector-тегами каждого варианта.

[Цикл выбора цели](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/baffledwall/tick.mcfunction), [обычный луч](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/baffledwall/tick_loop.mcfunction), [лечащий вариант](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/baffledwall/tick_loop_pl_heal.mcfunction), [лазерный вариант](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/baffledwall/tick_loop_pl_laser.mcfunction), [снайперский вариант](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/baffledwall/tick_loop_pl_sniper.mcfunction).

## Freeze block: сохранённая позиция и эффекты

`freeze/block/create` записывает Pos[0..2] цели в индивидуальное storage `minecraft:<UUID>`; команды, принудительно отключающей AI, в исходнике закомментированы. Периодическая ветка `tick` передаёт UUID в macro, возвращает сущность на сохранённые координаты, ставит лёд в столбце высотой 2 блока и накладывает Mining Fatigue и Weakness на 1 секунду с amplifier 127; только игроку дополнительно даёт Resistance amplifier 1 на 1 секунду. В конце `cold` сбрасывается в 0, цель получает тег `have_lucky_effects`, появляется снежинка. `destroy` заменяет этот столбец воздухом с drops и проигрывает ice/snow particles; это отдельная очистка, не часть tick. [Сохранение позиции](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/freeze/block/create.mcfunction), [tick](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/freeze/block/tick_3.mcfunction), [разрушение](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/freeze/block/destroy.mcfunction).

## Shock effect: выбор цели и цепная молния

Основной вход `shock_effect/zap` вызывается damage-hook при положительном `effect_shock_potion` и также из эффекта при `HurtTime:10s`. Он сохраняет ID атакующего/команду, помечает ближайших союзников и самого атакующего защитными тегами, затем наводит визуальный луч на ближайшую допустимую цель-моба в пределах 10 блоков. Луч проходит позициями с шагом 0,25, максимум до контрольного порога Distance 50; при столкновении с небезвоздушным блоком срабатывает hit-ветка. При цели функция наносит 6 урона типа arrow; `as_target/run` переносит запас `effect_shock_potion` в цепную переменную, помечает уже поражённую цель и выбирает следующую незатронутую цель в радиусе 20, исключая damager, союзников, spectator и strict_map_object. Повторные цели отсекаются тегом `chained`; после прохода снимаются `chained` и `nodamage`, но сам zap добавляет союзным мобам `nodamage1` и не снимает его. В просмотренных LBC источниках очистка этого общего тега есть лишь у других несвязанных обработчиков, поэтому дружеские сущности могут остаться исключёнными из последующих raycast-целей. Снарядный hit-check ограничен совместным пересечением `dx=dy=dz=0` и `distance`, а не сферой на весь радиус.

Ссылки: [вход zap](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/shock_effect/zap.mcfunction), [пошаговая проверка луча](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/shock_effect/zap/check.mcfunction), [переход к следующей цели](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/shock_effect/zap/as_target/run.mcfunction), [урон и остановка цепи](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/shock_effect/zap/hit_entity.mcfunction).

## Файлы: other / baffledend

### `lbc:other/baffledend/1`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/projectiles/eggs.mcfunction`; условие вызова: `execute if data entity @e[type=minecraft:snowball,sort=nearest,limit=1] Item.components.minecraft:custom_data{"baffled_of_everything":1} positioned 0 64 0 run function lbc:other/baffledend/1`. Передаёт шаг в `lbc:other/snus`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/baffledend/1.mcfunction)

### `lbc:other/baffledend/2`

Через4 секунды после предыдущего шага создаёт **две** неуязвимые серебрянки `minibaffled` у каждой несspectator сущности. Затем через6 секунд передаёт цепочку дальше; число мобов может удвоить масштаб призыва. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/baffledend/2.mcfunction)
### `lbc:other/baffledend/3`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/baffledend/2.mcfunction`; условие вызова: `schedule function lbc:other/baffledend/3 6s`. Изменяет блоки: fill 144 ~ 144 -96 ~ -96 minecraft:air strict. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/baffledend/3.mcfunction)

### `lbc:other/baffledend/4`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/baffledend/3.mcfunction`; условие вызова: `schedule function lbc:other/baffledend/4 8s`. Передаёт шаг в `lbc:other/baffledend/hedgehogrider`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/baffledend/4.mcfunction)

### `lbc:other/baffledend/5`

Через8 секунд после предыдущего шага выбирает случайно не более50 неп-player сущностей для command-block предметов `drop+skeleton`, не более25 для `drop+bob`, не более25 для `drop+aimbob` и ещё одну для `drop+sauvojen_tuntija`. Предметы лежат у сущностей; функция не выдаёт их игрокам в инвентарь. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/baffledend/5.mcfunction)
### `lbc:other/baffledend/6`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/baffledend/5.mcfunction`; условие вызова: `schedule function lbc:other/baffledend/6 10s`. Loot: spawn lbc:yameteluckyblock → ~. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/baffledend/6.mcfunction)

### `lbc:other/baffledend/7`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/baffledend/6.mcfunction`; условие вызова: `schedule function lbc:other/baffledend/7 4s`. Loot: spawn lbc:drops/magic_bean → ~. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/baffledend/7.mcfunction)

### `lbc:other/baffledend/8`

Последний scheduled шаг очищает эффекты у всех игроков кроме spectator, даёт им Luck V навсегда и выдаёт по64 red mushroom blocks, brown mushroom blocks и mushroom stems с пользовательскими item_model/custom name (это lucky-block предметы). В функции нет дальнейшей очистки или расхода этих предметов; общая цепочка до этого шага занимает последовательные задержки суммарно52 секунды от первого scheduled вызова. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/baffledend/8.mcfunction)
### `lbc:other/baffledend/hedgehogrider`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/baffledend/4.mcfunction`; условие вызова: `execute as @e[tag=minibaffled] at @s run function lbc:other/baffledend/hedgehogrider`. Создаёт `silverfish` с {CustomNameVisible:0b,Passengers:[{id:"minecraft:wither_skeleton",equipment:{mainhand:{id:"minecraft:iron_axe",components:{"minecraft:custom_name":{"italic":false,"translate":"hamm. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/baffledend/hedgehogrider.mcfunction)

## Файлы: other / baffledwall

### `lbc:other/baffledwall/tick`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/silverfish_tick.mcfunction`; условие вызова: `execute if entity @s[tag=baffled_wall] if predicate lbc:chance0_5 run function lbc:other/baffledwall/tick`. Состояние scoreboard: scoreboard players operation *tempt lbcID2 = @s team_number. Передаёт шаг в `lbc:other/baffledwall/tick_rc`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/baffledwall/tick.mcfunction)

### `lbc:other/baffledwall/tick_loop`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/baffledwall/tick_loop.mcfunction`; условие вызова: `execute if block ~ ~ ~ #minecraft:airs if score @s Distance matches ..50 positioned ^ ^ ^.3 run function lbc:other/baffledwall/tick_loop`. Состояние scoreboard: scoreboard players add @s Distance 1. Передаёт шаг в `lbc:other/baffledwall/tick_loop_dmg`, `lbc:other/baffledwall/tick_loop`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/baffledwall/tick_loop.mcfunction)

### `lbc:other/baffledwall/tick_loop_dmg`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/baffledwall/tick_loop.mcfunction`; условие вызова: `execute if entity @e[type=#minecraft:mobs,dx=0,dy=0,dz=0,distance=..5,tag=!baffled_wall,limit=1] run function lbc:other/baffledwall/tick_loop_dmg`. Наносит 4 урона по @e[type=#minecraft:mobs,dx=0,dy=0,dz=0,distance=..5,tag=!nodmg,tag=!nodmg1,limit=1] типа `arrow`. Состояние scoreboard: scoreboard players set @s Distance 1000. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/baffledwall/tick_loop_dmg.mcfunction)

### `lbc:other/baffledwall/tick_loop_dmg_big`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/baffledwall/tick_loop_pl_sniper.mcfunction`; условие вызова: `execute if entity @e[type=#minecraft:mobs,dx=0,dy=0,dz=0,distance=..5,tag=!nodmg,limit=1] run function lbc:other/baffledwall/tick_loop_dmg_big`. Наносит 20 урона по @e[type=#minecraft:mobs,dx=0,dy=0,dz=0,distance=..5,tag=!nodmg,limit=1] типа `arrow`. Состояние scoreboard: scoreboard players set @s Distance 1000. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/baffledwall/tick_loop_dmg_big.mcfunction)

### `lbc:other/baffledwall/tick_loop_dmg_laser`

Автоматический caller среди исследованных функций не найден; при ручном запуске выполняются действия ниже. Наносит 0.1 урона по @e[type=#minecraft:mobs,dx=0,dy=0,dz=0,distance=..5,tag=!nodmg,limit=1] типа `generic`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/baffledwall/tick_loop_dmg_laser.mcfunction)

### `lbc:other/baffledwall/tick_loop_heal`

Автоматический caller среди исследованных функций не найден; при ручном запуске выполняются действия ниже. Эффект: @e[type=#minecraft:mobs,distance=..0.01,tag=nodmg,limit=1] получает `minecraft:instant_health` на 1 секунд, amplifier 0. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/baffledwall/tick_loop_heal.mcfunction)

### `lbc:other/baffledwall/tick_loop_pl`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/baffledwall/tick_loop_pl.mcfunction`; условие вызова: `execute if block ~ ~ ~ #minecraft:airs if score @s Distance matches ..50 positioned ^ ^ ^.3 run function lbc:other/baffledwall/tick_loop_pl`. Состояние scoreboard: scoreboard players add @s Distance 1. Передаёт шаг в `lbc:other/baffledwall/tick_loop_dmg`, `lbc:other/baffledwall/tick_loop_pl`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/baffledwall/tick_loop_pl.mcfunction)

### `lbc:other/baffledwall/tick_loop_pl_heal`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/baffledwall/tick_loop_pl_heal.mcfunction`; условие вызова: `execute if block ~ ~ ~ #minecraft:airs if score @s Distance matches ..50 positioned ^ ^ ^.3 run function lbc:other/baffledwall/tick_loop_pl_heal`. Эффект: @e[type=#minecraft:mobs,dx=0,dy=0,dz=0,distance=..5,tag=nodmg,limit=1] получает `minecraft:instant_health` на 1 секунд, amplifier 0. Состояние scoreboard: scoreboard players add @s Distance 1. Передаёт шаг в `lbc:other/baffledwall/tick_loop_pl_heal`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/baffledwall/tick_loop_pl_heal.mcfunction)

### `lbc:other/baffledwall/tick_loop_pl_laser`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/baffledwall/tick_loop_pl_laser.mcfunction`; условие вызова: `execute if score @s Distance matches ..30 positioned ^ ^ ^.3 run function lbc:other/baffledwall/tick_loop_pl_laser`. Наносит 0.02 урона по @e[type=#minecraft:mobs,dx=0,dy=0,dz=0,distance=..5,tag=!nodmg,limit=1] типа `generic`. Состояние scoreboard: scoreboard players add @s Distance 1. Передаёт шаг в `lbc:other/baffledwall/tick_loop_pl_laser`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/baffledwall/tick_loop_pl_laser.mcfunction)

### `lbc:other/baffledwall/tick_loop_pl_sniper`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/baffledwall/tick_loop_pl_sniper.mcfunction`; условие вызова: `execute if score @s Distance matches ..300 positioned ^ ^ ^.3 if block ~ ~ ~ #minecraft:airs run function lbc:other/baffledwall/tick_loop_pl_sniper`. Состояние scoreboard: scoreboard players add @s Distance 1. Передаёт шаг в `lbc:other/baffledwall/tick_loop_dmg_big`, `lbc:other/baffledwall/tick_loop_pl_sniper`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/baffledwall/tick_loop_pl_sniper.mcfunction)

### `lbc:other/baffledwall/tick_rc`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/baffledwall/tick.mcfunction`; условие вызова: `execute facing entity @e[type=#minecraft:mobs,distance=..10,tag=!baffled_wall,tag=!spectator,limit=1,predicate=!lbc:same_team,tag=!strict_map_object] eyes run function lbc:other/baffledwall/tick_rc`. Состояние scoreboard: scoreboard players set @s Distance 0. Передаёт шаг в `lbc:other/baffledwall/tick_loop`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/baffledwall/tick_rc.mcfunction)

### `lbc:other/baffledwall/tick_rc_pl`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/hedgehogarrow_upg.mcfunction`; условие вызова: `execute at @s if entity @e[type=#minecraft:mobs,distance=0.001..17,tag=nodmg,tag=!spectator,limit=1,tag=!strict_map_object] anchored eyes positioned ^ ^ ^ facing entity @e[type=#minecraft:mobs,distance=0.001..17,tag=!spectator,tag=!nodmg,tag=!nodmg1,sort=nearest,limit=1,tag=!strict_map_object] eyes run function lbc:other/baffledwall/tick_rc_pl`. Состояние scoreboard: scoreboard players set @s Distance 0. Передаёт шаг в `lbc:other/baffledwall/tick_loop_pl`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/baffledwall/tick_rc_pl.mcfunction)

### `lbc:other/baffledwall/tick_rc_pl_heal`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/hedgehoghealer.mcfunction`; условие вызова: `execute at @s if entity @e[type=#minecraft:mobs,type=!minecraft:silverfish,distance=0.001..19,tag=nodmg,tag=!spectator,limit=1,tag=!strict_map_object] anchored eyes positioned ^ ^ ^ facing entity @e[type=#minecraft:mobs,distance=..19,tag=nodmg,tag=!spectator,sort=nearest,limit=1,tag=!strict_map_object] eyes run function lbc:other/baffledwall/tick_rc_pl_heal`. Состояние scoreboard: scoreboard players set @s Distance 0. Передаёт шаг в `lbc:other/baffledwall/tick_loop_pl_heal`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/baffledwall/tick_rc_pl_heal.mcfunction)

### `lbc:other/baffledwall/tick_rc_pl_laser`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/hedgehoglaser.mcfunction`; условие вызова: `execute at @s if entity @e[type=#minecraft:mobs,distance=0.001..10,tag=!spectator,tag=!nodmg,limit=1,tag=!strict_map_object] anchored eyes positioned ^ ^ ^ facing entity @e[type=#minecraft:mobs,distance=0.001..10,tag=!spectator,tag=!nodmg,limit=1,tag=!strict_map_object] eyes run function lbc:other/baffledwall/tick_rc_pl_laser`. Состояние scoreboard: scoreboard players set @s Distance 0. Передаёт шаг в `lbc:other/baffledwall/tick_loop_pl_laser`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/baffledwall/tick_rc_pl_laser.mcfunction)

### `lbc:other/baffledwall/tick_rc_pl_sniper`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/hedgehogsniper.mcfunction`; условие вызова: `execute at @s if entity @e[type=#minecraft:mobs,distance=0.001..95,tag=nodmg,tag=!spectator,limit=1,tag=!strict_map_object] anchored eyes positioned ^ ^ ^ facing entity @e[type=#minecraft:mobs,distance=0.001..95,tag=!spectator,tag=!nodmg,sort=nearest,limit=1,tag=!strict_map_object] eyes run function lbc:other/baffledwall/tick_rc_pl_sniper`. Состояние scoreboard: scoreboard players set @s Distance 0. Передаёт шаг в `lbc:other/baffledwall/tick_loop_pl_sniper`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/baffledwall/tick_rc_pl_sniper.mcfunction)

## Файлы: other / bullshit

### `lbc:other/bullshit/anal`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/players_2t.mcfunction`; условие вызова: `execute unless score legacy lbc.math matches 1 at @s[predicate=lbc:chance_from_hunger,nbt={active_effects:[{id:"minecraft:hunger"}]}] positioned ~ ~0.4 ~ run function lbc:other/bullshit/anal`. Команда: `particle minecraft:falling_honey ^ ^ ^-0.5 0.1 0.1 0.1 0.5 100 normal`; `playsound minecraft:block.honey_block.break master @a ~ ~ ~ 1 0`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/bullshit/anal.mcfunction)

### `lbc:other/bullshit/oral`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/players_2t.mcfunction`; условие вызова: `execute unless score legacy lbc.math matches 1 at @s[predicate=lbc:chance_from_nausea,nbt={active_effects:[{id:"minecraft:nausea"}]}] anchored eyes positioned ^ ^ ^0.3 run function lbc:other/bullshit/oral`. Эффект: @s получает `minecraft:blindness` на 1 секунд, amplifier 0; @s получает `minecraft:darkness` на 1 секунд, amplifier 0. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/bullshit/oral.mcfunction)

## Файлы: other / chaoschestplate

### `lbc:other/chaoschestplate/absorption`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/effectselect.mcfunction`; условие вызова: `execute if score #random17 lbc.math matches 15 run function lbc:other/chaoschestplate/absorption`. Эффект: @s получает `minecraft:absorption` на 30 секунд, amplifier 0. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/absorption.mcfunction)

### `lbc:other/chaoschestplate/acid`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/effectselect.mcfunction`; условие вызова: `execute if score #random17 lbc.math matches 16 run function lbc:other/chaoschestplate/acid`. Эффект: @e[distance=0.01..10,tag=!spectator,predicate=!lbc:same_team] получает `minecraft:wither` на 2 секунд, amplifier 10; @e[distance=0.01..10,tag=!spectator,predicate=!lbc:same_team] получает `minecraft:poison` на 2 секунд, amplifier 10; @a[gamemode=!spectator,distance=0.01..10,predicate=!lbc:same_team] получает `minecraft:hunger` на 2 секунд, amplifier 10; @e[distance=0.01..10,tag=!spectator,predicate=!lbc:same_team] получает `minecraft:slowness` на 2 секунд, amplifier 10. Состояние scoreboard: scoreboard players operation *tempt lbcID2 = @s team_number. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/acid.mcfunction)

### `lbc:other/chaoschestplate/arrows`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/effectselect.mcfunction`; условие вызова: `execute if score #random17 lbc.math matches 1 run function lbc:other/chaoschestplate/arrows`. Создаёт `arrow` с {Motion:[0.0d,0.0d,1.0d],PierceLevel:10b,Tags:["killedarrow","bombardilo_crocodilo"]}; `arrow` с {Motion:[0.0d,0.0d,-1.0d],PierceLevel:10b,Tags:["killedarrow","bombardilo_crocodilo"]}; `arrow` с {Motion:[1.0d,0.0d,0.0d],PierceLevel:10b,Tags:["killedarrow","bombardilo_crocodilo"]}; `arrow` с {Motion:[-1.0d,0.0d,0.0d],PierceLevel:10b,Tags:["killedarrow","bombardilo_crocodilo"]}; `arrow` с {Motion:[1.0d,0.0d,1.0d],PierceLevel:10b,Tags:["killedarrow","bombardilo_crocodilo"]}. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/arrows.mcfunction)

### `lbc:other/chaoschestplate/bounce`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/effectselect.mcfunction`; условие вызова: `execute if score #random17 lbc.math matches 2 run function lbc:other/chaoschestplate/bounce`. Эффект: @s получает `minecraft:jump_boost` на 20 секунд, amplifier 2; @e[distance=0.01..10,tag=!spectator,predicate=!lbc:same_team] получает `minecraft:levitation` на 1 секунд, amplifier 4. Состояние scoreboard: scoreboard players operation *tempt lbcID2 = @s team_number. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/bounce.mcfunction)

### `lbc:other/chaoschestplate/bridge`

При результате6 из семи sprint-вариантов выполняет41 fill вдоль направления взгляда, от шага0 до40, каждый на 1,5 блока ниже исходной позиции; из воздуха кладёт light-blue stained glass. Крайние координаты ±0.5 могут в зависимости от дробной позиции захватывать соседний блок. Это постоянный мост, автоматического удаления здесь нет. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/bridge.mcfunction)
### `lbc:other/chaoschestplate/corrosion`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/effectselect.mcfunction`; условие вызова: `execute if score #random17 lbc.math matches 8 run function lbc:other/chaoschestplate/corrosion`. Команда: `place template lbc:air ~-24 ~-24 ~-24 none none 0.05`; `particle minecraft:large_smoke ~ ~ ~ 1 1 1 1 50 normal`; `playsound minecraft:block.portal.ambient master @a ~ ~ ~ 1 1`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/corrosion.mcfunction)

### `lbc:other/chaoschestplate/damage`

Результат5 из 17 вариантов вызывает урон6 типа cactus по каждому выбранному противнику в пределах10: команда `execute at` перебирает вражеских мобов, а в каждой точке выбирает ближайшую цель. Поэтому в обычной ситуации каждая цель получает попадание, а при совпадающих/близких точках возможны повторные удары. Перед проходом копируется team_number владельца для исключения союзников. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/damage.mcfunction)
### `lbc:other/chaoschestplate/doggies`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/effectselect.mcfunction`; условие вызова: `execute if score #random17 lbc.math matches 7 run function lbc:other/chaoschestplate/doggies`. Создаёт `wolf` с {DeathLootTable:"empty",Sitting:0b,CollarColor:14b,Tags:["summoned"],attributes:[{id:"minecraft:follow_range",base:200},{id:"minecraft:movement_speed",base:0.5},{id:"minecraft:atta. Обновляет NBT/components: modify entity @e[tag=summoned,sort=nearest,limit=1] Owner set from entity @s UUID. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/doggies.mcfunction)

### `lbc:other/chaoschestplate/effectselect`

Повреждение брони в main slot вызывает равновероятный выбор 1 из17 подэффектов, если рядом нет `stopper_magic` и игрока `nomagic_active`: стрелы, отскок, огонь, лечение, cactus-урон, телепортация врагов, волк, template-коррозия, неуязвимость, дым, ослабление врагов, яд/высасывание, transfusion, скорость+прыжок, absorption, кислотный набор либо TNT-взрыв. Вызов не списывает mana и сам не ставит cooldown; частота зависит от damage hook. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/effectselect.mcfunction)
### `lbc:other/chaoschestplate/effectselect_jump`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/items/armor.mcfunction`; условие вызова: `execute at @s if items entity @s armor.legs *[minecraft:custom_data~{chaoschestplate:1}] unless entity @e[type=minecraft:marker,distance=..20,tag=stopper_magic,limit=1] unless entity @a[gamemode=!spectator,distance=..20,tag=nomagic_active] run function lbc:other/chaoschestplate/effectselect_jump`. Передаёт шаг в `lbc:other/chaoschestplate/flame`, `lbc:other/chaoschestplate/slow`, `lbc:other/chaoschestplate/levitation`, `lbc:other/chaoschestplate/ponos`, `lbc:other/chaoschestplate/explode`, `lbc:other/chaoschestplate/speed`, `lbc:other/chaoschestplate/teleport`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/effectselect_jump.mcfunction)

### `lbc:other/chaoschestplate/effectselect_see`

Автоматический caller не найден. При ручном вызове выбирает равновероятно одно из13 воздействий на `@s`: Blindness15с, Darkness15с, Glowing60с, Hunger III60с, Instant Damage I, Levitation II10с, Mining Fatigue II60с, Nausea20с, Poison10с, Slowness II20с, Unluck II30с, Weakness I15с или Wither I15с. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/effectselect_see.mcfunction)
### `lbc:other/chaoschestplate/effectselect_sprint`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/items/on_sprint.mcfunction`; условие вызова: `execute at @s[predicate=lbc:chance5,predicate=lbc:on_ground] if items entity @s armor.feet *[minecraft:custom_data~{chaoschestplate:1}] unless entity @a[gamemode=!spectator,distance=..20,tag=nomagic_active] unless entity @e[type=minecraft:marker,distance=..20,tag=stopper_magic,limit=1] run function lbc:other/chaoschestplate/effectselect_sprint`. Передаёт шаг в `lbc:other/chaoschestplate/speed_only`, `lbc:other/chaoschestplate/jump_boost`, `lbc:other/chaoschestplate/phase`, `lbc:other/chaoschestplate/nitroflight`, `lbc:other/chaoschestplate/phase_speed`, `lbc:other/chaoschestplate/bridge`, `lbc:other/chaoschestplate/flash`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/effectselect_sprint.mcfunction)

### `lbc:other/chaoschestplate/explode`

Результат17 из17 исходов рандомной брони даёт caster Slow Falling на10 секунд и окружающим несspectator игрокам в радиусе8 Resistance V на1 секунду. Создаёт четыре TNT по сторонам на ±0.5 блока с `fuse:5s`: `s` здесь Short, то есть пять тиков (0,25с), не пять секунд. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/explode.mcfunction)
### `lbc:other/chaoschestplate/flame`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/effectselect.mcfunction`; условие вызова: `execute if score #random17 lbc.math matches 3 run function lbc:other/chaoschestplate/flame`. Эффект: @s получает `minecraft:fire_resistance` на 60 секунд, amplifier 0. Изменяет блоки: fill ~5 ~5 ~5 ~-5 ~-5 ~-5 minecraft:fire replace #minecraft:airs. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/flame.mcfunction)

### `lbc:other/chaoschestplate/flash`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/effectselect_sprint.mcfunction`; условие вызова: `execute if score #random7 lbc.math matches 7 run function lbc:other/chaoschestplate/flash`. Эффект: @a[gamemode=!spectator,distance=0.01..15,predicate=!lbc:same_team] получает `minecraft:blindness` на 5 секунд, amplifier 0; @e[distance=0.01..15,tag=!spectator,predicate=!lbc:same_team] получает `minecraft:slowness` на 5 секунд, amplifier 9. Состояние scoreboard: scoreboard players operation *tempt lbcID2 = @s team_number. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/flash.mcfunction)

### `lbc:other/chaoschestplate/heal`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/effectselect.mcfunction`; условие вызова: `execute if score #random17 lbc.math matches 4 run function lbc:other/chaoschestplate/heal`. Эффект: @s получает `minecraft:instant_health` на 1 секунд, amplifier 0. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/heal.mcfunction)

### `lbc:other/chaoschestplate/invulnerable`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/effectselect.mcfunction`; условие вызова: `execute if score #random17 lbc.math matches 9 run function lbc:other/chaoschestplate/invulnerable`. Эффект: @s получает `minecraft:resistance` на 1 секунд, amplifier 3. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/invulnerable.mcfunction)

### `lbc:other/chaoschestplate/jump_boost`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/effectselect_sprint.mcfunction`; условие вызова: `execute if score #random7 lbc.math matches 2 run function lbc:other/chaoschestplate/jump_boost`. Эффект: @s получает `minecraft:jump_boost` на 30 секунд, amplifier 2. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/jump_boost.mcfunction)

### `lbc:other/chaoschestplate/levitation`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/effectselect_jump.mcfunction`; условие вызова: `execute if score #random7 lbc.math matches 3 run function lbc:other/chaoschestplate/levitation`. Эффект: @s получает `minecraft:levitation` на 1 секунд, amplifier 30; @s получает `minecraft:slow_falling` на 30 секунд, amplifier 0. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/levitation.mcfunction)

### `lbc:other/chaoschestplate/nitroflight`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/effectselect_sprint.mcfunction`; условие вызова: `execute if score #random7 lbc.math matches 4 run function lbc:other/chaoschestplate/nitroflight`. Эффект: @a[gamemode=!spectator,distance=..2] получает `minecraft:resistance` на 1 секунд, amplifier 4; @s получает `minecraft:slow_falling` на 20 секунд, amplifier 0. Создаёт `creeper` с {Fuse:0s,Invulnerable:1b,ignited:1b}. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/nitroflight.mcfunction)

### `lbc:other/chaoschestplate/perdesh`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/effectselect.mcfunction`; условие вызова: `execute if score #random17 lbc.math matches 12 run function lbc:other/chaoschestplate/perdesh`. Эффект: @a[gamemode=!spectator,distance=0.01..10,predicate=!lbc:same_team] получает `minecraft:nausea` на 10 секунд, amplifier 0; @e[distance=0.01..10,tag=!spectator,predicate=!lbc:same_team] получает `minecraft:poison` на 3 секунд, amplifier 0; @e[distance=0.01..10,tag=!spectator,predicate=!lbc:same_team] получает `minecraft:wither` на 3 секунд, amplifier 0. Состояние scoreboard: scoreboard players operation *tempt lbcID2 = @s team_number. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/perdesh.mcfunction)

### `lbc:other/chaoschestplate/phase`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/effectselect_sprint.mcfunction`; условие вызова: `execute if score #random7 lbc.math matches 3 run function lbc:other/chaoschestplate/phase`. Команда: `execute positioned ^ ^ ^10 run spreadplayers ~ ~ 0 5 under 120 false @s[predicate=lbc:the_nether]`; `execute positioned ^ ^ ^10 run spreadplayers ~ ~ 0 5 false @s[predicate=!lbc:the_nether]`; `playsound minecraft:entity.enderman.teleport master @a ~ ~ ~ 1 1`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/phase.mcfunction)

### `lbc:other/chaoschestplate/phase_speed`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/effectselect_sprint.mcfunction`; условие вызова: `execute if score #random7 lbc.math matches 5 run function lbc:other/chaoschestplate/phase_speed`. Эффект: @s получает `minecraft:speed` на 2 секунд, amplifier 20. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/phase_speed.mcfunction)

### `lbc:other/chaoschestplate/ponos`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/effectselect_jump.mcfunction`; условие вызова: `execute if score #random7 lbc.math matches 4 run function lbc:other/chaoschestplate/ponos`. Эффект: @s получает `minecraft:slowness` на 1 секунд, amplifier 9; @e[distance=0.01..10,tag=!spectator,predicate=!lbc:same_team] получает `minecraft:poison` на 2 секунд, amplifier 10; @e[distance=0.01..10,tag=!spectator,predicate=!lbc:same_team] получает `minecraft:wither` на 2 секунд, amplifier 10; @a[gamemode=!spectator,distance=0.01..10,predicate=!lbc:same_team] получает `minecraft:nausea` на 2 секунд, amplifier 10. Состояние scoreboard: scoreboard players operation *tempt lbcID2 = @s team_number. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/ponos.mcfunction)

### `lbc:other/chaoschestplate/slow`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/effectselect_jump.mcfunction`; условие вызова: `execute if score #random7 lbc.math matches 2 run function lbc:other/chaoschestplate/slow`. Эффект: @e[distance=0.01..6,tag=!spectator,predicate=!lbc:same_team] получает `minecraft:slowness` на 10 секунд, amplifier 3. Изменяет блоки: fill ~5 ~5 ~5 ~-5 ~-5 ~-5 minecraft:soul_soil replace #minecraft:solid strict. Состояние scoreboard: scoreboard players operation *tempt lbcID2 = @s team_number. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/slow.mcfunction)

### `lbc:other/chaoschestplate/smoke`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/effectselect.mcfunction`; условие вызова: `execute if score #random17 lbc.math matches 10 run function lbc:other/chaoschestplate/smoke`. Команда: `particle minecraft:block_marker{block_state:{Name:"minecraft:black_wool"}} ~ ~1 ~ 6 3 6 0.15 400 force @a[distance=0.01..]`; `playsound minecraft:block.fire.extinguish master @a ~ ~ ~ 2 1`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/smoke.mcfunction)

### `lbc:other/chaoschestplate/speed`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/effectselect.mcfunction`; условие вызова: `execute if score #random17 lbc.math matches 14 run function lbc:other/chaoschestplate/speed`. Эффект: @s получает `minecraft:speed` на 10 секунд, amplifier 4; @s получает `minecraft:jump_boost` на 10 секунд, amplifier 4. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/speed.mcfunction)

### `lbc:other/chaoschestplate/speed_only`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/effectselect_sprint.mcfunction`; условие вызова: `execute if score #random7 lbc.math matches 1 run function lbc:other/chaoschestplate/speed_only`. Эффект: @s получает `minecraft:speed` на 30 секунд, amplifier 1. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/speed_only.mcfunction)

### `lbc:other/chaoschestplate/teleport`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/effectselect_jump.mcfunction`; условие вызова: `execute if score #random7 lbc.math matches 7 run function lbc:other/chaoschestplate/teleport`. Эффект: @s получает `minecraft:resistance` на 2 секунд, amplifier 4. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/teleport.mcfunction)

### `lbc:other/chaoschestplate/teleportnazad`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/effectselect.mcfunction`; условие вызова: `execute if score #random17 lbc.math matches 6 run function lbc:other/chaoschestplate/teleportnazad`. Состояние scoreboard: scoreboard players operation *tempt lbcID2 = @s team_number. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/teleportnazad.mcfunction)

### `lbc:other/chaoschestplate/transfusion`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/effectselect.mcfunction`; условие вызова: `execute if score #random17 lbc.math matches 13 run function lbc:other/chaoschestplate/transfusion`. Эффект: @s получает `minecraft:regeneration` на 6 секунд, amplifier 0; @s получает `minecraft:saturation` на 1 секунд, amplifier 40; @e[distance=0.01..10,tag=!spectator,predicate=!lbc:same_team] получает `minecraft:wither` на 5 секунд, amplifier 0; @a[gamemode=!spectator,distance=0.01..10,predicate=!lbc:same_team] получает `minecraft:hunger` на 5 секунд, amplifier 0. Состояние scoreboard: scoreboard players operation *tempt lbcID2 = @s team_number; scoreboard players add @s food_count 1. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/transfusion.mcfunction)

### `lbc:other/chaoschestplate/weakall`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/effectselect.mcfunction`; условие вызова: `execute if score #random17 lbc.math matches 11 run function lbc:other/chaoschestplate/weakall`. Эффект: @e[distance=0.01..10,tag=!spectator,predicate=!lbc:same_team] получает `minecraft:weakness` на 2 секунд, amplifier 0. Состояние scoreboard: scoreboard players operation *tempt lbcID2 = @s team_number. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/weakall.mcfunction)

## Файлы: other / everythingend

**Ход эффекта.** Маркированный snowball запускает цепь из58 функций; в исходном первом шаге позиция явно переводится в `(0,64,0)`, а большая часть блоковых волн затем выполняется у каждого `swrg.look` marker. Шаги1–15 каждые2 тика расширяют куб очистки; после2 секунд шаги16–23 каждые5 тиков расширяют лавовый куб. Далее идут TNT/молния/огненные снаряды/магические raycast волны, несколько спавнов боссов и финальный вызов nuke helper; шаг58 выдаёт advancement игрокам и ставит loot chest у каждой несspectator сущности. Сумма последовательных schedule задержек составляет1863 тика (93,15с) от входа в шаг1; цепь не содержит общей команды возврата gamerule/world state, нет локальной команды вернуть worldborder; часть дальнейшего результата находится во внешнем `other/nuke/nuke_eoe`.

### `lbc:other/everythingend/1`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/projectiles/eggs.mcfunction`; условие вызова: `execute if data entity @e[type=minecraft:snowball,sort=nearest,limit=1] Item.components.minecraft:custom_data{"end_of_everything":1} positioned 0 64 0 run function lbc:other/everythingend/1`. Изменяет блоки: fill ~1 ~1 ~1 ~-1 ~-1 ~-1 minecraft:air strict. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/1.mcfunction)

### `lbc:other/everythingend/10`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/9.mcfunction`; условие вызова: `schedule function lbc:other/everythingend/10 2t`. Изменяет блоки: fill ~10 ~10 ~10 ~-10 ~-10 ~-10 minecraft:air strict. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/10.mcfunction)

### `lbc:other/everythingend/11`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/10.mcfunction`; условие вызова: `schedule function lbc:other/everythingend/11 2t`. Изменяет блоки: fill ~11 ~11 ~11 ~-11 ~-11 ~-11 minecraft:air strict. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/11.mcfunction)

### `lbc:other/everythingend/12`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/11.mcfunction`; условие вызова: `schedule function lbc:other/everythingend/12 2t`. Изменяет блоки: fill ~12 ~12 ~12 ~-12 ~-12 ~-12 minecraft:air strict. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/12.mcfunction)

### `lbc:other/everythingend/13`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/12.mcfunction`; условие вызова: `schedule function lbc:other/everythingend/13 2t`. Изменяет блоки: fill ~13 ~13 ~13 ~-13 ~-13 ~-13 minecraft:air strict. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/13.mcfunction)

### `lbc:other/everythingend/14`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/13.mcfunction`; условие вызова: `schedule function lbc:other/everythingend/14 2t`. Изменяет блоки: fill ~14 ~14 ~14 ~-14 ~-14 ~-14 minecraft:air strict. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/14.mcfunction)

### `lbc:other/everythingend/15`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/14.mcfunction`; условие вызова: `schedule function lbc:other/everythingend/15 2t`. Изменяет блоки: fill ~15 ~15 ~15 ~-15 ~-15 ~-15 minecraft:air strict. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/15.mcfunction)

### `lbc:other/everythingend/16`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/15.mcfunction`; условие вызова: `schedule function lbc:other/everythingend/16 2s`. Изменяет блоки: fill ~1 ~1 ~1 ~-1 ~-1 ~-1 minecraft:lava. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/16.mcfunction)

### `lbc:other/everythingend/17`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/16.mcfunction`; условие вызова: `schedule function lbc:other/everythingend/17 5t`. Изменяет блоки: fill ~2 ~2 ~2 ~-2 ~-2 ~-2 minecraft:lava. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/17.mcfunction)

### `lbc:other/everythingend/18`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/17.mcfunction`; условие вызова: `schedule function lbc:other/everythingend/18 5t`. Изменяет блоки: fill ~3 ~3 ~3 ~-3 ~-3 ~-3 minecraft:lava. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/18.mcfunction)

### `lbc:other/everythingend/19`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/18.mcfunction`; условие вызова: `schedule function lbc:other/everythingend/19 5t`. Изменяет блоки: fill ~4 ~4 ~4 ~-4 ~-4 ~-4 minecraft:lava. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/19.mcfunction)

### `lbc:other/everythingend/2`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/1.mcfunction`; условие вызова: `schedule function lbc:other/everythingend/2 2t`. Изменяет блоки: fill ~2 ~2 ~2 ~-2 ~-2 ~-2 minecraft:air strict. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/2.mcfunction)

### `lbc:other/everythingend/20`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/19.mcfunction`; условие вызова: `schedule function lbc:other/everythingend/20 5t`. Изменяет блоки: fill ~5 ~5 ~5 ~-5 ~-5 ~-5 minecraft:lava. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/20.mcfunction)

### `lbc:other/everythingend/21`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/20.mcfunction`; условие вызова: `schedule function lbc:other/everythingend/21 5t`. Изменяет блоки: fill ~6 ~6 ~6 ~-6 ~-6 ~-6 minecraft:lava. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/21.mcfunction)

### `lbc:other/everythingend/22`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/21.mcfunction`; условие вызова: `schedule function lbc:other/everythingend/22 5t`. Изменяет блоки: fill ~7 ~7 ~7 ~-7 ~-7 ~-7 minecraft:lava. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/22.mcfunction)

### `lbc:other/everythingend/23`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/22.mcfunction`; условие вызова: `schedule function lbc:other/everythingend/23 5t`. Изменяет блоки: fill ~8 ~8 ~8 ~-8 ~-8 ~-8 minecraft:lava. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/23.mcfunction)

### `lbc:other/everythingend/24`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/23.mcfunction`; условие вызова: `schedule function lbc:other/everythingend/24 5s`. Создаёт `tnt` с {fuse:80}. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/24.mcfunction)

### `lbc:other/everythingend/25`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/24.mcfunction`; условие вызова: `schedule function lbc:other/everythingend/25 15t`. Создаёт `tnt` с {fuse:80}. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/25.mcfunction)

### `lbc:other/everythingend/26`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/25.mcfunction`; условие вызова: `schedule function lbc:other/everythingend/26 15t`. Создаёт `tnt` с {fuse:80}. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/26.mcfunction)

### `lbc:other/everythingend/27`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/26.mcfunction`; условие вызова: `schedule function lbc:other/everythingend/27 15t`. Создаёт `tnt` с {fuse:80}. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/27.mcfunction)

### `lbc:other/everythingend/28`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/27.mcfunction`; условие вызова: `schedule function lbc:other/everythingend/28 2s`. Создаёт `lightning_bolt`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/28.mcfunction)

### `lbc:other/everythingend/29`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/28.mcfunction`; условие вызова: `schedule function lbc:other/everythingend/29 10t`. Создаёт `lightning_bolt`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/29.mcfunction)

### `lbc:other/everythingend/3`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/2.mcfunction`; условие вызова: `schedule function lbc:other/everythingend/3 2t`. Изменяет блоки: fill ~3 ~3 ~3 ~-3 ~-3 ~-3 minecraft:air strict. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/3.mcfunction)

### `lbc:other/everythingend/30`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/29.mcfunction`; условие вызова: `schedule function lbc:other/everythingend/30 10t`. Создаёт `lightning_bolt`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/30.mcfunction)

### `lbc:other/everythingend/31`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/30.mcfunction`; условие вызова: `schedule function lbc:other/everythingend/31 10t`. Создаёт `lightning_bolt`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/31.mcfunction)

### `lbc:other/everythingend/32`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/31.mcfunction`; условие вызова: `schedule function lbc:other/everythingend/32 2s`. Изменяет блоки: fill ~6 ~6 ~6 ~-6 ~-6 ~-6 minecraft:emerald_block replace #minecraft:solid strict. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/32.mcfunction)

### `lbc:other/everythingend/33`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/32.mcfunction`; условие вызова: `schedule function lbc:other/everythingend/33 3s`. Создаёт `fireball` с {ExplosionPower:8,Motion:[-0.5d,-0.5d,-0.5d]}. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/33.mcfunction)

### `lbc:other/everythingend/34`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/33.mcfunction`; условие вызова: `schedule function lbc:other/everythingend/34 15t`. Создаёт `fireball` с {ExplosionPower:8,Motion:[0.5d,-0.5d,0.5d]}. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/34.mcfunction)

### `lbc:other/everythingend/35`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/34.mcfunction`; условие вызова: `schedule function lbc:other/everythingend/35 15t`. Создаёт `fireball` с {ExplosionPower:8,Motion:[0.5d,-0.5d,-0.5d]}. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/35.mcfunction)

### `lbc:other/everythingend/36`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/35.mcfunction`; условие вызова: `schedule function lbc:other/everythingend/36 15t`. Создаёт `fireball` с {ExplosionPower:8,Motion:[-0.5d,-0.5d,0.5d]}. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/36.mcfunction)

### `lbc:other/everythingend/37`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/36.mcfunction`; условие вызова: `schedule function lbc:other/everythingend/37 2s`. Создаёт `marker` с {Tags:["slowraycast","raycastruby","tuntija","can_deleted","magic"],Rotation:[90.0f,0.0f],data:{lifetime:50}}. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/37.mcfunction)

### `lbc:other/everythingend/38`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/37.mcfunction`; условие вызова: `schedule function lbc:other/everythingend/38 10t`. Создаёт `marker` с {Tags:["slowraycast","raycastruby","tuntija","can_deleted","magic"],data:{lifetime:50},Rotation:[180.0f,0.0f]}. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/38.mcfunction)

### `lbc:other/everythingend/39`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/38.mcfunction`; условие вызова: `schedule function lbc:other/everythingend/39 10t`. Создаёт `marker` с {Tags:["slowraycast","raycastruby","tuntija","can_deleted","magic"],data:{lifetime:50},Rotation:[270.0f,0.0f]}. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/39.mcfunction)

### `lbc:other/everythingend/4`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/3.mcfunction`; условие вызова: `schedule function lbc:other/everythingend/4 2t`. Изменяет блоки: fill ~4 ~4 ~4 ~-4 ~-4 ~-4 minecraft:air strict. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/4.mcfunction)

### `lbc:other/everythingend/40`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/39.mcfunction`; условие вызова: `schedule function lbc:other/everythingend/40 10t`. Создаёт `marker` с {Tags:["slowraycast","raycastruby","tuntija","can_deleted","magic"],data:{lifetime:50},Rotation:[0.0f,0.0f]}. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/40.mcfunction)

### `lbc:other/everythingend/41`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/40.mcfunction`; условие вызова: `schedule function lbc:other/everythingend/41 10t`. Создаёт `marker` с {Tags:["slowraycast","raycastruby","tuntija","can_deleted","magic"],data:{lifetime:50},Rotation:[90.0f,0.0f]}. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/41.mcfunction)

### `lbc:other/everythingend/42`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/41.mcfunction`; условие вызова: `schedule function lbc:other/everythingend/42 10t`. Создаёт `marker` с {Tags:["slowraycast","raycastruby","tuntija","can_deleted","magic"],data:{lifetime:50},Rotation:[180.0f,0.0f]}. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/42.mcfunction)

### `lbc:other/everythingend/43`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/42.mcfunction`; условие вызова: `schedule function lbc:other/everythingend/43 10t`. Создаёт `marker` с {Tags:["slowraycast","raycastruby","tuntija","can_deleted","magic"],data:{lifetime:50},Rotation:[270.0f,0.0f]}. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/43.mcfunction)

### `lbc:other/everythingend/44`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/43.mcfunction`; условие вызова: `schedule function lbc:other/everythingend/44 10t`. Создаёт `marker` с {Tags:["slowraycast","raycastruby","tuntija","can_deleted","magic"],data:{lifetime:50},Rotation:[0.0f,0.0f]}. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/44.mcfunction)

### `lbc:other/everythingend/45`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/44.mcfunction`; условие вызова: `schedule function lbc:other/everythingend/45 10t`. Создаёт `marker` с {Tags:["slowraycast","raycastruby","tuntija","can_deleted","magic"],data:{lifetime:50},Rotation:[90.0f,0.0f]}. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/45.mcfunction)

### `lbc:other/everythingend/46`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/45.mcfunction`; условие вызова: `schedule function lbc:other/everythingend/46 10t`. Создаёт `marker` с {Tags:["slowraycast","raycastruby","tuntija","can_deleted","magic"],data:{lifetime:50},Rotation:[180.0f,0.0f]}. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/46.mcfunction)

### `lbc:other/everythingend/47`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/46.mcfunction`; условие вызова: `schedule function lbc:other/everythingend/47 10t`. Создаёт `marker` с {Tags:["slowraycast","raycastruby","tuntija","can_deleted","magic"],data:{lifetime:50},Rotation:[270.0f,0.0f]}. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/47.mcfunction)

### `lbc:other/everythingend/48`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/47.mcfunction`; условие вызова: `schedule function lbc:other/everythingend/48 10t`. Создаёт `marker` с {Tags:["slowraycast","raycastruby","tuntija","can_deleted","magic"],data:{lifetime:50},Rotation:[0.0f,0.0f]}. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/48.mcfunction)

### `lbc:other/everythingend/49`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/48.mcfunction`; условие вызова: `schedule function lbc:other/everythingend/49 3s`. Создаёт `fireball` с {ExplosionPower:2,Motion:[-1.0d,0.0d,0.0d]}. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/49.mcfunction)

### `lbc:other/everythingend/5`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/4.mcfunction`; условие вызова: `schedule function lbc:other/everythingend/5 2t`. Изменяет блоки: fill ~5 ~5 ~5 ~-5 ~-5 ~-5 minecraft:air strict. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/5.mcfunction)

### `lbc:other/everythingend/50`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/49.mcfunction`; условие вызова: `schedule function lbc:other/everythingend/50 10t`. Создаёт `fireball` с {ExplosionPower:2,Motion:[1.0d,0.0d,0.0d]}. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/50.mcfunction)

### `lbc:other/everythingend/51`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/50.mcfunction`; условие вызова: `schedule function lbc:other/everythingend/51 10t`. Создаёт `fireball` с {ExplosionPower:2,Motion:[0.0d,0.0d,-1.0d]}. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/51.mcfunction)

### `lbc:other/everythingend/52`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/51.mcfunction`; условие вызова: `schedule function lbc:other/everythingend/52 10t`. Создаёт `fireball` с {ExplosionPower:2,Motion:[0.0d,0.0d,1.0d]}. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/52.mcfunction)

### `lbc:other/everythingend/53`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/52.mcfunction`; условие вызова: `schedule function lbc:other/everythingend/53 2s`. Передаёт шаг в `lbc:other/sauvojen_tuntija/select_eoe`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/53.mcfunction)

### `lbc:other/everythingend/54`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/53.mcfunction`; условие вызова: `schedule function lbc:other/everythingend/54 7s`. Передаёт шаг в `lbc:other/everythingend/stevari`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/54.mcfunction)

### `lbc:other/everythingend/55`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/54.mcfunction`; условие вызова: `schedule function lbc:other/everythingend/55 5s`. Создаёт `slime` с {Size:20,Tags:["worm","lucky_mob"]}. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/55.mcfunction)

### `lbc:other/everythingend/56`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/55.mcfunction`; условие вызова: `schedule function lbc:other/everythingend/56 7s`. Изменяет блоки: setblock ~ ~10 ~ minecraft:spawner{SpawnCount:1,SpawnRange:20,Delay:1,MinSpawnDelay:1,MaxSpawnDelay:2,MaxNearbyEntities:500,Requi. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/56.mcfunction)

### `lbc:other/everythingend/57`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/56.mcfunction`; условие вызова: `schedule function lbc:other/everythingend/57 15s`. Передаёт шаг в `lbc:other/nuke/nuke_eoe`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/57.mcfunction)

### `lbc:other/everythingend/58`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/57.mcfunction`; условие вызова: `schedule function lbc:other/everythingend/58 22s`. Передаёт шаг в `lbc:other/everythingend/trueknowledge`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/58.mcfunction)

### `lbc:other/everythingend/6`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/5.mcfunction`; условие вызова: `schedule function lbc:other/everythingend/6 2t`. Изменяет блоки: fill ~6 ~6 ~6 ~-6 ~-6 ~-6 minecraft:air strict. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/6.mcfunction)

### `lbc:other/everythingend/7`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/6.mcfunction`; условие вызова: `schedule function lbc:other/everythingend/7 2t`. Изменяет блоки: fill ~7 ~7 ~7 ~-7 ~-7 ~-7 minecraft:air strict. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/7.mcfunction)

### `lbc:other/everythingend/8`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/7.mcfunction`; условие вызова: `schedule function lbc:other/everythingend/8 2t`. Изменяет блоки: fill ~8 ~8 ~8 ~-8 ~-8 ~-8 minecraft:air strict. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/8.mcfunction)

### `lbc:other/everythingend/9`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/8.mcfunction`; условие вызова: `schedule function lbc:other/everythingend/9 2t`. Изменяет блоки: fill ~9 ~9 ~9 ~-9 ~-9 ~-9 minecraft:air strict. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/9.mcfunction)

### `lbc:other/everythingend/stevari`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/54.mcfunction`; условие вызова: `execute at @e[type=!minecraft:player,sort=random,limit=100] run function lbc:other/everythingend/stevari`. Создаёт `skeleton` с {Tags:["fat_skeleton"],DeathLootTable:"lbc:fatskeleton",PersistenceRequired:1b,CustomNameVisible:1b,CustomName:{"italic":false,"translate":"fat_skeleton"},equipment:{mainhand:{id:". Завершение: удаляет сущность (`@s[type=#minecraft:mobs,type=!minecraft:player]`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/stevari.mcfunction)

### `lbc:other/everythingend/trueknowledge`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/58.mcfunction`; условие вызова: `execute at @e[tag=!spectator] run function lbc:other/everythingend/trueknowledge`. Изменяет блоки: setblock ~ ~ ~ minecraft:chest{LootTable:"lbc:lucky_items_1"}. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/trueknowledge.mcfunction)

## Файлы: other / evoker

### `lbc:other/evoker/cast1`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/coolsword_old.mcfunction`; условие вызова: `execute if score #random17 lbc.math matches 4 run function lbc:other/evoker/cast1`. Создаёт `evoker_fangs`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/evoker/cast1.mcfunction)

### `lbc:other/evoker/cast2`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/evoker/cast1.mcfunction`; условие вызова: `schedule function lbc:other/evoker/cast2 5t`. Создаёт `evoker_fangs`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/evoker/cast2.mcfunction)

### `lbc:other/evoker/cast3`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/evoker/cast2.mcfunction`; условие вызова: `schedule function lbc:other/evoker/cast3 5t`. Создаёт `evoker_fangs`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/evoker/cast3.mcfunction)

## Файлы: other / freeze

### `lbc:other/freeze/1`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/freeze/check.mcfunction`; условие вызова: `execute if score @s cold matches 1..10 run function lbc:other/freeze/1`. Эффект: @s получает `minecraft:slowness` на 2 секунд, amplifier 0. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/freeze/1.mcfunction)

### `lbc:other/freeze/10`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/freeze/check.mcfunction`; условие вызова: `execute if score @s cold matches 91..99 run function lbc:other/freeze/10`. Эффект: @s получает `minecraft:slowness` на 2 секунд, amplifier 9; @s получает `minecraft:mining_fatigue` на 2 секунд, amplifier 8; @s получает `minecraft:weakness` на 2 секунд, amplifier 7; @s получает `minecraft:unluck` на 2 секунд, amplifier 2; @s получает `minecraft:blindness` на 2 секунд, amplifier 0. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/freeze/10.mcfunction)

### `lbc:other/freeze/2`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/freeze/check.mcfunction`; условие вызова: `execute if score @s cold matches 11..20 run function lbc:other/freeze/2`. Эффект: @s получает `minecraft:slowness` на 2 секунд, amplifier 1; @s получает `minecraft:mining_fatigue` на 2 секунд, amplifier 0. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/freeze/2.mcfunction)

### `lbc:other/freeze/3`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/freeze/check.mcfunction`; условие вызова: `execute if score @s cold matches 21..30 run function lbc:other/freeze/3`. Эффект: @s получает `minecraft:slowness` на 2 секунд, amplifier 2; @s получает `minecraft:mining_fatigue` на 2 секунд, amplifier 1; @s получает `minecraft:weakness` на 2 секунд, amplifier 0. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/freeze/3.mcfunction)

### `lbc:other/freeze/4`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/freeze/check.mcfunction`; условие вызова: `execute if score @s cold matches 31..40 run function lbc:other/freeze/4`. Эффект: @s получает `minecraft:slowness` на 2 секунд, amplifier 3; @s получает `minecraft:mining_fatigue` на 2 секунд, amplifier 2; @s получает `minecraft:weakness` на 2 секунд, amplifier 1; @s получает `minecraft:unluck` на 2 секунд, amplifier 0. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/freeze/4.mcfunction)

### `lbc:other/freeze/5`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/freeze/check.mcfunction`; условие вызова: `execute if score @s cold matches 41..50 run function lbc:other/freeze/5`. Эффект: @s получает `minecraft:slowness` на 2 секунд, amplifier 4; @s получает `minecraft:mining_fatigue` на 2 секунд, amplifier 3; @s получает `minecraft:weakness` на 2 секунд, amplifier 2; @s получает `minecraft:unluck` на 2 секунд, amplifier 0. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/freeze/5.mcfunction)

### `lbc:other/freeze/6`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/freeze/check.mcfunction`; условие вызова: `execute if score @s cold matches 51..60 run function lbc:other/freeze/6`. Эффект: @s получает `minecraft:slowness` на 2 секунд, amplifier 5; @s получает `minecraft:mining_fatigue` на 2 секунд, amplifier 4; @s получает `minecraft:weakness` на 2 секунд, amplifier 3; @s получает `minecraft:unluck` на 2 секунд, amplifier 0; @s получает `minecraft:blindness` на 2 секунд, amplifier 0. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/freeze/6.mcfunction)

### `lbc:other/freeze/7`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/freeze/check.mcfunction`; условие вызова: `execute if score @s cold matches 61..70 run function lbc:other/freeze/7`. Эффект: @s получает `minecraft:slowness` на 2 секунд, amplifier 6; @s получает `minecraft:mining_fatigue` на 2 секунд, amplifier 5; @s получает `minecraft:weakness` на 2 секунд, amplifier 4; @s получает `minecraft:unluck` на 2 секунд, amplifier 1; @s получает `minecraft:blindness` на 2 секунд, amplifier 0. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/freeze/7.mcfunction)

### `lbc:other/freeze/8`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/freeze/check.mcfunction`; условие вызова: `execute if score @s cold matches 71..80 run function lbc:other/freeze/8`. Эффект: @s получает `minecraft:slowness` на 2 секунд, amplifier 7; @s получает `minecraft:mining_fatigue` на 2 секунд, amplifier 6; @s получает `minecraft:weakness` на 2 секунд, amplifier 5; @s получает `minecraft:unluck` на 2 секунд, amplifier 1; @s получает `minecraft:blindness` на 2 секунд, amplifier 0. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/freeze/8.mcfunction)

### `lbc:other/freeze/9`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/freeze/check.mcfunction`; условие вызова: `execute if score @s cold matches 81..90 run function lbc:other/freeze/9`. Эффект: @s получает `minecraft:slowness` на 2 секунд, amplifier 8; @s получает `minecraft:mining_fatigue` на 2 секунд, amplifier 7; @s получает `minecraft:weakness` на 2 секунд, amplifier 6; @s получает `minecraft:unluck` на 2 секунд, amplifier 2; @s получает `minecraft:blindness` на 2 секунд, amplifier 0. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/freeze/9.mcfunction)

### `lbc:other/freeze/add_cold`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/icesword/pre.mcfunction`; условие вызова: `execute unless score @s cold matches 1.. run function lbc:other/freeze/add_cold`. Состояние scoreboard: scoreboard players operation *cold_multiplier lbc.math += 1 lbc.math; scoreboard players operation *cold_pre lbc.math *= *cold_multiplier lbc.math. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/freeze/add_cold.mcfunction)

### `lbc:other/freeze/check`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/iceshard.mcfunction`; условие вызова: `function lbc:other/freeze/check`. Передаёт шаг в `lbc:other/freeze/particle`, `lbc:other/freeze/1`, `lbc:other/freeze/2`, `lbc:other/freeze/3`, `lbc:other/freeze/4`, `lbc:other/freeze/5`, `lbc:other/freeze/6`, `lbc:other/freeze/7`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/freeze/check.mcfunction)

### `lbc:other/freeze/cooldown`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/mobs_1s.mcfunction`; условие вызова: `execute if entity @s[scores={cold=1..}] run function lbc:other/freeze/cooldown`. Состояние scoreboard: scoreboard players remove @s[scores={cold_cd=1..}] cold_cd 1; scoreboard players remove @s cold 1; scoreboard players set @s cold 0. Передаёт шаг в `lbc:other/freeze/check`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/freeze/cooldown.mcfunction)

### `lbc:other/freeze/full`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/freeze/check.mcfunction`; условие вызова: `execute if score @s cold matches 100.. run function lbc:other/freeze/full`. Обновляет NBT/components: modify storage entities uuid set from entity @s UUID[0]. Состояние scoreboard: scoreboard players set @s cold 0; scoreboard players set @s cold_cd 0; scoreboard players set @s frozen 10. Передаёт шаг в `lbc:other/freeze/block/create`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/freeze/full.mcfunction)

### `lbc:other/freeze/particle`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/freeze/check.mcfunction`; условие вызова: `function lbc:other/freeze/particle with storage lbc.math`. Использует макропараметры `$(tmp1)`; значения подставляются вызывающей функцией через `with storage`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/freeze/particle.mcfunction)

### `lbc:other/freeze/tick_1s_frozen`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/mobs_1s.mcfunction`; условие вызова: `execute if entity @s[scores={frozen=1..}] run function lbc:other/freeze/tick_1s_frozen`. Состояние scoreboard: scoreboard players remove @s frozen 1. Передаёт шаг в `lbc:other/freeze/block/destroy`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/freeze/tick_1s_frozen.mcfunction)

## Файлы: other / freeze / block

### `lbc:other/freeze/block/create`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/freeze/full.mcfunction`; условие вызова: `function lbc:other/freeze/block/create with storage entities`. Использует макропараметры `$(uuid)`; значения подставляются вызывающей функцией через `with storage`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/freeze/block/create.mcfunction)

### `lbc:other/freeze/block/destroy`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/ice_wand/shatter/run.mcfunction`; условие вызова: `function lbc:other/freeze/block/destroy`. Изменяет блоки: fill ~ ~ ~ ~ ~1 ~ minecraft:air destroy. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/freeze/block/destroy.mcfunction)

### `lbc:other/freeze/block/tick`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/effects/tick.mcfunction`; условие вызова: `execute if entity @s[scores={frozen=1..},tag=!sin_greed_active] run function lbc:other/freeze/block/tick with storage lbc.math`. Обновляет NBT/components: modify storage entities uuid set from entity @s UUID[0]. Передаёт шаг в `lbc:other/freeze/block/tick_2`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/freeze/block/tick.mcfunction)

### `lbc:other/freeze/block/tick_2`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/freeze/block/tick.mcfunction`; условие вызова: `function lbc:other/freeze/block/tick_2 with storage entities`. Передаёт шаг в `lbc:other/freeze/block/tick_3`. Использует макропараметры `$(uuid)`; значения подставляются вызывающей функцией через `with storage`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/freeze/block/tick_2.mcfunction)

### `lbc:other/freeze/block/tick_3`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/freeze/block/tick_2.mcfunction`; условие вызова: `$function lbc:other/freeze/block/tick_3 with storage $(uuid) freeze`. Эффект: @s получает `minecraft:mining_fatigue` на 1 секунд, amplifier 127; @s получает `minecraft:weakness` на 1 секунд, amplifier 127; @s[type=minecraft:player] получает `minecraft:resistance` на 1 секунд, amplifier 1. Изменяет блоки: fill ~ ~ ~ ~ ~1 ~ minecraft:ice strict. Состояние scoreboard: scoreboard players set @s cold 0. Использует макропараметры `$(x)`, `$(y)`, `$(z)`; значения подставляются вызывающей функцией через `with storage`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/freeze/block/tick_3.mcfunction)

## Файлы: other / gayradar

При входе `click` записывает результат терминального `if entity @a[gamemode=!spectator]` в `tmp`; `store result` получает число найденных игроков, а не boolean. `click_rec` случайно выбирает игрока, сравнивает его `dmg_dealt` с текущим максимумом и рекурсивно проверяет ещё столько игроков, сколько осталось в `tmp`, помечая просмотренных; в итоге отмечается игрок с максимальным `dmg_dealt` среди обойдённых. Есть отдельная особенность нулевого максимума: при `dmg_dealt_tmp=0` cleanup снимает `max` со всех игроков и ставит его текущему исполнителю команды `click`; потому получателем `click_2` в этом случае может стать бросивший предмет игрок из вызова через `items/snowballs`, а не найденный максимум. При ненулевом максимуме `click_2` добавляет отмеченному игроку `skill_issue+1800` и Slowness II на180 секунд. После прохода временные tags снимаются. [Основной вход](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/gayradar/click.mcfunction), [счётчик/рекурсия](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/gayradar/click_rec.mcfunction)
### `lbc:other/gayradar/click`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/projectiles/eggs.mcfunction`; условие вызова: `execute if data entity @e[type=minecraft:snowball,sort=nearest,limit=1] Item.components.minecraft:custom_data{"gayradar":1} run function lbc:other/gayradar/click`. Состояние scoreboard: scoreboard players set dmg_dealt_tmp lbc.math -1. Передаёт шаг в `lbc:other/gayradar/click_rec`, `lbc:other/gayradar/click_2`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/gayradar/click.mcfunction)

### `lbc:other/gayradar/click_2`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/gayradar/click.mcfunction`; условие вызова: `execute as @a[tag=max,limit=1] at @s run function lbc:other/gayradar/click_2`. Эффект: @s получает `minecraft:slowness` на 180 секунд, amplifier 1. Состояние scoreboard: scoreboard players add @s skill_issue 1800. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/gayradar/click_2.mcfunction)

### `lbc:other/gayradar/click_rec`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/gayradar/click.mcfunction`; условие вызова: `execute as @r[gamemode=!spectator] run function lbc:other/gayradar/click_rec`. Состояние scoreboard: scoreboard players remove tmp lbc.math 1. Передаёт шаг в `lbc:other/gayradar/max`, `lbc:other/gayradar/click_rec`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/gayradar/click_rec.mcfunction)

### `lbc:other/gayradar/max`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/gayradar/click_rec.mcfunction`; условие вызова: `execute if score @s dmg_dealt > dmg_dealt_tmp lbc.math run function lbc:other/gayradar/max`. Состояние scoreboard: scoreboard players operation dmg_dealt_tmp lbc.math = @s dmg_dealt. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/gayradar/max.mcfunction)

## Файлы: other / instants

### `lbc:other/instants/random_infinity`

Выбирает одно из14 последствий с равной вероятностью: manamax=100 000 000; manaregen=100 000 000; три защитных атрибута; attack damage; attack speed+block break speed; knockback resistances; max health; теги infinite_resistance/heal/saturation; специальный lucky-block wand; набор движения/падения/дыхания; либо один из двух масштабных комплектов attribute modifiers. Только исход11 выдаёт carrot-on-a-stick предмет; в каждом исходе удаляется один `infinity_rand`, показываются титр и advancement. Атрибуты и теги в самой функции не имеют обратного сброса. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/instants/random_infinity.mcfunction)
### `lbc:other/instants/select`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/players_tick.mcfunction`; условие вызова: `execute if entity @s[tag=instantitem_active] run function lbc:other/instants/select`. Передаёт шаг в `lbc:other/instants/select2`, `lbc:other/instants/select_old`, `lbc:workingitems/awaken_tuntija_wands/electroblock/tick`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/instants/select.mcfunction)

### `lbc:other/instants/select2`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/instants/select.mcfunction`; условие вызова: `execute if score #lbcskill swrg.math matches 1 if score @s lbc.skill matches 2 run function lbc:other/instants/select2`. Передаёт шаг в `lbc:other/instants/random_infinity`, `lbc:other/nexus/wires_other_worlds/black`, `lbc:other/nexus/wires_other_worlds/green`, `lbc:other/nexus/wires_other_worlds/red`, `lbc:other/greed/pickup`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/instants/select2.mcfunction)

### `lbc:other/instants/select_old`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/instants/select.mcfunction`; условие вызова: `execute unless score #lbcskill swrg.math matches 1 run function lbc:other/instants/select_old`. Передаёт шаг в `lbc:other/instants/random_infinity`, `lbc:other/penises/player_select`, `lbc:other/nexus/wires_other_worlds/black`, `lbc:other/nexus/wires_other_worlds/green`, `lbc:other/nexus/wires_other_worlds/red`, `lbc:other/greed/pickup`, `lbc:swrg_kit_integration/items/touhou_items/select`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/instants/select_old.mcfunction)

## Файлы: other / overload_crux

### `lbc:other/overload_crux/give`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/manaregen.mcfunction`; условие вызова: `execute if entity @s[tag=!obtained_crux,scores={mana=..0}] at @s anchored eyes positioned ^ ^ ^ run function lbc:other/overload_crux/give`. Loot: spawn lbc:overload_crux → ~. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/overload_crux/give.mcfunction)

### `lbc:other/overload_crux/use`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/spell1_default.mcfunction`; условие вызова: `execute at @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{overload_crux:1}}}}] anchored eyes positioned ^ ^ ^ unless entity @e[type=minecraft:marker,distance=..20,tag=stopper_magic,limit=1] run function lbc:other/overload_crux/use`. Loot: replace lbc:neko_set_overload/helmet → @s; replace lbc:neko_set_overload/chestplate → @s; replace lbc:neko_set_overload/leggings → @s; replace lbc:neko_set_overload/boots → @s. Состояние scoreboard: scoreboard players operation @s mana -= overload_crux_mana_use lbc.math; scoreboard players set tmp3 lbc.math 0. Передаёт шаг в `lbc:other/negative_mana/tentacle_full_set`. Завершение: расходует/очищает предметы. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/overload_crux/use.mcfunction)

## Файлы: other / penises

### `lbc:other/penises/dirty`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/penises/player_select.mcfunction`; условие вызова: `execute if entity @s[nbt={Inventory:[{components:{"minecraft:custom_data":{penis_num:2}}}]}] run function lbc:other/penises/dirty`. Эффект: @s получает `minecraft:slowness` на 8 секунд, amplifier 5; @s получает `minecraft:weakness` на 4 секунд, amplifier 5; @s получает `minecraft:nausea` на 6 секунд, amplifier 5. Наносит 4 урона по @s типа `minecraft:arrow`. Завершение: расходует/очищает предметы. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/penises/dirty.mcfunction)

### `lbc:other/penises/holy`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/penises/player_select.mcfunction`; условие вызова: `execute if entity @s[nbt={Inventory:[{components:{"minecraft:custom_data":{penis_num:4}}}]}] run function lbc:other/penises/holy`. Эффект: @s получает `minecraft:blindness` на 8 секунд, amplifier 1; @s получает `minecraft:levitation` на 1 секунд, amplifier 15. Создаёт `lightning_bolt`. Завершение: расходует/очищает предметы. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/penises/holy.mcfunction)

### `lbc:other/penises/horse`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/penises/player_select.mcfunction`; условие вызова: `execute if entity @s[nbt={Inventory:[{components:{"minecraft:custom_data":{penis_num:5}}}]}] run function lbc:other/penises/horse`. Эффект: @s получает `minecraft:slowness` на 1 секунд, amplifier 127. Наносит 10 урона по @s типа `minecraft:arrow`. Завершение: расходует/очищает предметы. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/penises/horse.mcfunction)

### `lbc:other/penises/multicast`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/penises/player_select.mcfunction`; условие вызова: `execute if entity @s[nbt={Inventory:[{components:{"minecraft:custom_data":{penis_num:6}}}]}] run function lbc:other/penises/multicast`. Наносит 0.1 урона по @s типа `minecraft:arrow`. Завершение: расходует/очищает предметы. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/penises/multicast.mcfunction)

### `lbc:other/penises/nigger`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/penises/player_select.mcfunction`; условие вызова: `execute if entity @s[nbt={Inventory:[{components:{"minecraft:custom_data":{penis_num:3}}}]}] run function lbc:other/penises/nigger`. Создаёт `tnt` с {fuse:2}. Завершение: расходует/очищает предметы. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/penises/nigger.mcfunction)

### `lbc:other/penises/normal`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/penises/player_select.mcfunction`; условие вызова: `execute if entity @s[nbt={Inventory:[{components:{"minecraft:custom_data":{penis_num:1}}}]}] run function lbc:other/penises/normal`. Наносит 6 урона по @s типа `minecraft:arrow`. Завершение: расходует/очищает предметы. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/penises/normal.mcfunction)

### `lbc:other/penises/player_select`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/instants/select_old.mcfunction`; условие вызова: `execute at @s[nbt={Inventory:[{components:{"minecraft:custom_data":{penis_bomb:1}}}]}] run function lbc:other/penises/player_select`. Передаёт шаг в `lbc:other/penises/normal`, `lbc:other/penises/dirty`, `lbc:other/penises/nigger`, `lbc:other/penises/holy`, `lbc:other/penises/horse`, `lbc:other/penises/multicast`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/penises/player_select.mcfunction)

### `lbc:other/penises/select`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/penis_mob.mcfunction`; условие вызова: `execute at @e[type=#minecraft:mobs,type=!minecraft:player,distance=..1.2,tag=!no_penis,sort=nearest,limit=1] run function lbc:other/penises/select`. Передаёт шаг в `lbc:other/penises/normal`, `lbc:other/penises/dirty`, `lbc:other/penises/nigger`, `lbc:other/penises/holy`, `lbc:other/penises/horse`, `lbc:other/penises/multicast`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/penises/select.mcfunction)

## Файлы: other / phantomarrows

Каждые10 тиков cloud `phantomarrows` выбирает случайного моба в радиусе16, не spectator/nomagic, и с вероятностью1/8 запускает одну из восьми стрел: четыре кардинальные из точек ±5 со скоростью1 и четыре диагональные из точек ±3,5 со скоростью0,7 по осям. Все стрелы имеют base damage2,5, pickup=2, `groundbreaker`; Owner здесь не задаётся. Конечные блоковые/уронные последствия связаны с внешним `projectiles/prein` handler. [Выбор](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/phantomarrows/select.mcfunction)

### `lbc:other/phantomarrows/1`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/phantomarrows/select.mcfunction`; условие вызова: `execute if score #random8 lbc.math matches 1 run function lbc:other/phantomarrows/1`. Создаёт `arrow` с {Motion:[-1.0d,0.0d,0.0d],Tags:["groundbreaker"],pickup:2b,damage:2.5d,CustomName:{"translate":"phantom_arrow"},PierceLevel:100}. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/phantomarrows/1.mcfunction)

### `lbc:other/phantomarrows/2`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/phantomarrows/select.mcfunction`; условие вызова: `execute if score #random8 lbc.math matches 2 run function lbc:other/phantomarrows/2`. Создаёт `arrow` с {Motion:[1.0d,0.0d,0.0d],Tags:["groundbreaker"],pickup:2b,damage:2.5d,CustomName:{"translate":"phantom_arrow"}}. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/phantomarrows/2.mcfunction)

### `lbc:other/phantomarrows/3`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/phantomarrows/select.mcfunction`; условие вызова: `execute if score #random8 lbc.math matches 3 run function lbc:other/phantomarrows/3`. Создаёт `arrow` с {Motion:[0.0d,0.0d,-1.0d],Tags:["groundbreaker"],pickup:2b,damage:2.5d,CustomName:{"translate":"phantom_arrow"}}. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/phantomarrows/3.mcfunction)

### `lbc:other/phantomarrows/4`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/phantomarrows/select.mcfunction`; условие вызова: `execute if score #random8 lbc.math matches 4 run function lbc:other/phantomarrows/4`. Создаёт `arrow` с {Motion:[0.0d,0.0d,1.0d],Tags:["groundbreaker"],pickup:2b,damage:2.5d,CustomName:{"translate":"phantom_arrow"}}. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/phantomarrows/4.mcfunction)

### `lbc:other/phantomarrows/5`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/phantomarrows/select.mcfunction`; условие вызова: `execute if score #random8 lbc.math matches 5 run function lbc:other/phantomarrows/5`. Создаёт `arrow` с {Motion:[-0.7d,0.0d,-0.7d],Tags:["groundbreaker"],pickup:2b,damage:2.5d,CustomName:{"translate":"phantom_arrow"}}. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/phantomarrows/5.mcfunction)

### `lbc:other/phantomarrows/6`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/phantomarrows/select.mcfunction`; условие вызова: `execute if score #random8 lbc.math matches 6 run function lbc:other/phantomarrows/6`. Создаёт `arrow` с {Motion:[0.7d,0.0d,-0.7d],Tags:["groundbreaker"],pickup:2b,damage:2.5d,CustomName:{"translate":"phantom_arrow"}}. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/phantomarrows/6.mcfunction)

### `lbc:other/phantomarrows/7`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/phantomarrows/select.mcfunction`; условие вызова: `execute if score #random8 lbc.math matches 7 run function lbc:other/phantomarrows/7`. Создаёт `arrow` с {Motion:[-0.7d,0.0d,0.7d],Tags:["groundbreaker"],pickup:2b,damage:2.5d,CustomName:{"translate":"phantom_arrow"}}. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/phantomarrows/7.mcfunction)

### `lbc:other/phantomarrows/8`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/phantomarrows/select.mcfunction`; условие вызова: `execute if score #random8 lbc.math matches 8 run function lbc:other/phantomarrows/8`. Создаёт `arrow` с {Motion:[0.7d,0.0d,0.7d],Tags:["groundbreaker"],pickup:2b,damage:2.5d,CustomName:{"translate":"phantom_arrow"}}. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/phantomarrows/8.mcfunction)

### `lbc:other/phantomarrows/select`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/area_effect_cloud_10t.mcfunction`; условие вызова: `execute at @s[tag=phantomarrows] at @e[type=#minecraft:mobs,distance=..16,tag=!spectator,tag=!nomagic_active,sort=random,limit=1] positioned ~ ~1.7 ~ run function lbc:other/phantomarrows/select`. Передаёт шаг в `lbc:other/phantomarrows/1`, `lbc:other/phantomarrows/2`, `lbc:other/phantomarrows/3`, `lbc:other/phantomarrows/4`, `lbc:other/phantomarrows/5`, `lbc:other/phantomarrows/6`, `lbc:other/phantomarrows/7`, `lbc:other/phantomarrows/8`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/phantomarrows/select.mcfunction)

## Файлы: other / pumpkinator

### `lbc:other/pumpkinator/explode`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/pumpkinator/tick.mcfunction`; условие вызова: `execute if predicate lbc:chance1 run function lbc:other/pumpkinator/explode`. Наносит 10 урона по @s типа `cactus`. Изменяет блоки: fill ~1 ~2 ~ ~-1 ~2 ~ minecraft:pumpkin strict; fill ~ ~3 ~ ~ ~6 ~ minecraft:pumpkin strict; setblock ~ ~7 ~ minecraft:carved_pumpkin strict. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/pumpkinator/explode.mcfunction)

### `lbc:other/pumpkinator/tick`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/raycastselector_rare.mcfunction`; условие вызова: `execute if entity @s[tag=pumpkinator] positioned ~ ~-1.5 ~ run return run function lbc:other/pumpkinator/tick`. Изменяет блоки: fill ~-1 ~-1 ~-1 ~1 ~1 ~1 minecraft:pumpkin strict; fill ~2 ~ ~2 ~-2 ~ ~-2 minecraft:pumpkin strict. Передаёт шаг в `lbc:other/pumpkinator/explode`, `lbc:universal_pos_razb_2`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/pumpkinator/tick.mcfunction)

## Файлы: other / sheep_black_hole

### `lbc:other/sheep_black_hole/tick`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/raycastselector_rare.mcfunction`; условие вызова: `execute if entity @s[tag=sheep_black_hole] run return run function lbc:other/sheep_black_hole/tick`. Наносит 10 урона по @s типа `outside_border`. Завершение: удаляет сущность (`@e[type=#minecraft:projectiles,distance=..5,tag=!strict_map_object]`); удаляет сущность (`@e[type=minecraft:item,distance=..5,tag=!strict_map_object]`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sheep_black_hole/tick.mcfunction)

## Файлы: other / shishend

**Ход события.** Цепь `shishend/1…11` растянута на100 секунд: запускает forest markers у несspectator игроков, выдаёт Levitation и меняет worldborder на382,75; затем ставит электрические и supply маркеры, выдаёт lightning loot, ждёт и пытается создать cyborg boss возле beacon display, после чего размещает pre.anti_mecha/pre.anti_magic markers и выдаёт `source_of_energy`/advancement. Финальный файл ничего не планирует дальше; локальной команды вернуть прежний worldborder здесь не видно. Подробное содержимое таймеров, лута и внешнего boss handler остаётся привязано к индивидуальным разделам ниже.

### `lbc:other/shishend/1`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/projectiles/eggs.mcfunction`; условие вызова: `execute if data entity @e[type=minecraft:snowball,sort=nearest,limit=1] Item.components.minecraft:custom_data{"shish_of_everything":1} positioned 0 64 0 run function lbc:other/shishend/1`. Создаёт `marker` с {data:{lifetime:800},Tags:["forest","rc_5t","magic"]}. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/shishend/1.mcfunction)

### `lbc:other/shishend/10`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/shishend/9.mcfunction`; условие вызова: `schedule function lbc:other/shishend/10 5s`. Создаёт `marker` с {Tags:["pre.anti_magic"]}. Передаёт шаг в `lbc:other/shishend/randomize_position/30`. Завершение: удаляет сущность (`@e[type=minecraft:marker,tag=pre.anti_magic]`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/shishend/10.mcfunction)

### `lbc:other/shishend/11`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/shishend/10.mcfunction`; условие вызова: `schedule function lbc:other/shishend/11 5s`. Loot: spawn lbc:source_of_energy → ~. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/shishend/11.mcfunction)

### `lbc:other/shishend/2`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/shishend/1.mcfunction`; условие вызова: `schedule function lbc:other/shishend/2 5s`. Эффект: @a[gamemode=!spectator] получает `minecraft:levitation` на 3 секунд, amplifier 16. Состояние scoreboard: scoreboard players set @a[gamemode=!spectator] coldownshish 34. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/shishend/2.mcfunction)

### `lbc:other/shishend/3`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/shishend/2.mcfunction`; условие вызова: `schedule function lbc:other/shishend/3 5s`. Команда: `worldborder center 0 0`; `worldborder set 382.75`; `title @a title " "`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/shishend/3.mcfunction)

### `lbc:other/shishend/4`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/shishend/3.mcfunction`; условие вызова: `schedule function lbc:other/shishend/4 5s`. Создаёт `marker` с {Tags:["pre.sauv_electro"]}; `item_display` с {interpolation_duration:1,teleport_duration:1,item_display:"fixed",transformation:[0.0f,0.0f,2.5f,0.0f,1.7678f,-1.7678f,0.0f,0.0f,1.7678f,1.7678f,0.0f,0.0f,0.0f,0.0f,0.0f,1.0f],ite. Передаёт шаг в `lbc:other/shishend/randomize_position/raycast_electro`. Завершение: удаляет сущность (`@e[type=minecraft:marker,tag=pre.sauv_electro]`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/shishend/4.mcfunction)

### `lbc:other/shishend/5`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/shishend/4.mcfunction`; условие вызова: `schedule function lbc:other/shishend/5 10s`. Loot: spawn lbc:yellow_lightning → ~. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/shishend/5.mcfunction)

### `lbc:other/shishend/6`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/shishend/5.mcfunction`; условие вызова: `schedule function lbc:other/shishend/6 5s`. Создаёт `marker`. Передаёт шаг в `lbc:other/shishend/risk_of_rain/run`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/shishend/6.mcfunction)

### `lbc:other/shishend/7`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/shishend/6.mcfunction`; условие вызова: `schedule function lbc:other/shishend/7 20s`. Команда: `title @a title " "`; `title @a subtitle "The End has not come to an End.."`; `schedule function lbc:other/shishend/8 20s`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/shishend/7.mcfunction)

### `lbc:other/shishend/8`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/shishend/7.mcfunction`; условие вызова: `schedule function lbc:other/shishend/8 20s`. Создаёт `skeleton` с {attributes:[{base:2.0d,id:"minecraft:attack_damage"},{base:75.0d,id:"minecraft:max_health"}],PersistenceRequired:1b,Tags:["cyborg_boss","lucky_mob","mk2","new"],Health:75.0f,Silen. Передаёт шаг в `lbc:other/shishend/8_1`, `lbc:other/techno/cyborg_boss/tick`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/shishend/8.mcfunction)

### `lbc:other/shishend/8_1`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/shishend/8.mcfunction`; условие вызова: `execute as @e[type=minecraft:skeleton,tag=mk2,tag=new] run function lbc:other/shishend/8_1`. Loot: replace lbc:neko_set_technomk2/helmet → @s; replace lbc:neko_set_technomk2/chestplate → @s; replace lbc:neko_set_technomk2/leggings → @s; replace lbc:neko_set_technomk2/boots → @s. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/shishend/8_1.mcfunction)

### `lbc:other/shishend/9`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/shishend/8.mcfunction`; условие вызова: `schedule function lbc:other/shishend/9 20s`. Создаёт `marker` с {Tags:["pre.anti_mecha"]}. Передаёт шаг в `lbc:other/shishend/randomize_position/30`. Завершение: удаляет сущность (`@e[type=minecraft:marker,tag=pre.anti_mecha]`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/shishend/9.mcfunction)

## Файлы: other / shishend / randomize_position

### `lbc:other/shishend/randomize_position/30`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/shishend/10.mcfunction`; условие вызова: `execute as @e[type=minecraft:marker,tag=pre.anti_magic] at @s run function lbc:other/shishend/randomize_position/30`. Обновляет NBT/components: modify storage lbc:main temp.pos set from entity @s Pos. Состояние scoreboard: scoreboard players operation #randx lbc.math += #tmp lbc.math; scoreboard players operation #randy lbc.math += #tmp lbc.math; scoreboard players operation #randz lbc.math += #tmp lbc.math. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/shishend/randomize_position/30.mcfunction)

### `lbc:other/shishend/randomize_position/raycast_electro`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/shishend/4.mcfunction`; условие вызова: `execute as @e[type=minecraft:marker,tag=pre.sauv_electro] at @s run function lbc:other/shishend/randomize_position/raycast_electro`. Обновляет NBT/components: modify storage lbc:main temp.pos set from entity @s Pos. Состояние scoreboard: scoreboard players operation #randx lbc.math += #tmp lbc.math; scoreboard players operation #randz lbc.math += #tmp lbc.math. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/shishend/randomize_position/raycast_electro.mcfunction)

## Файлы: other / shishend / risk_of_rain

### `lbc:other/shishend/risk_of_rain/as_crate`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/shishend/risk_of_rain/run.mcfunction`; условие вызова: `execute as @e[type=minecraft:item_display,distance=..1,tag=supply_drop,tag=!spawned,limit=1] run function lbc:other/shishend/risk_of_rain/as_crate`. Обновляет NBT/components: modify entity @s transformation.translation set value [0.0f,0.5f,0.0f]. Состояние scoreboard: scoreboard players set @s supplyFall 99. Передаёт шаг в `lbc:other/techno/supply_crate/run`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/shishend/risk_of_rain/as_crate.mcfunction)

### `lbc:other/shishend/risk_of_rain/run`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/shishend/6.mcfunction`; условие вызова: `execute as @e[type=minecraft:marker,tag=supply_point,limit=1] at @s run function lbc:other/shishend/risk_of_rain/run`. Создаёт `item_display` с {Tags:["supply_drop","new","rc_1sec"],Rotation:[0.0f,0.0f],item:{id:"minecraft:stick",components:{"minecraft:item_model":"lbc:supply_beacon"},count:1}}. Передаёт шаг в `lbc:other/shishend/risk_of_rain/as_crate`. Завершение: удаляет сущность (`@s`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/shishend/risk_of_rain/run.mcfunction)

### `lbc:other/shishend/risk_of_rain/tick`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/shishend/risk_of_rain/run.mcfunction`; условие вызова: `schedule function lbc:other/shishend/risk_of_rain/tick 1s`. Создаёт `item` с {Item:{id:"minecraft:arrow",count:16}}. Передаёт шаг в `lbc:other/shishend/risk_of_rain/run`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/shishend/risk_of_rain/tick.mcfunction)

## Файлы: other / shock_effect

### `lbc:other/shock_effect/zap`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/effects/effect_shock_potion.mcfunction`; условие вызова: `execute if data entity @s {HurtTime:10s} run function lbc:other/shock_effect/zap`. Состояние scoreboard: scoreboard players operation *chain lbc.math = @s effect_shock_potion; scoreboard players remove @s effect_shock_potion 1; scoreboard players operation *temp.effect lbc.math = @s effect_shock_potion; scoreboard players reset @s effect_shock_potion. Передаёт шаг в `lbc:other/idgive`, `lbc:other/shock_effect/zap/cast`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/shock_effect/zap.mcfunction)

## Файлы: other / shock_effect / potion

### `lbc:other/shock_effect/potion/convert`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/raycastselector_area_cloud.mcfunction`; условие вызова: `execute if score tmp lbc.math matches 126 run return run function lbc:other/shock_effect/potion/convert`. Создаёт `area_effect_cloud` с {Radius:5.0f,RadiusOnUse:0.0f,Duration:300,custom_particle: {type:"electric_spark"},potion_contents:{custom_color:0,custom_effects:[{id:"slowness",show_icon:0b,show_particles:0b,am. Обновляет NBT/components: merge entity @s[tag=!done] {"custom_particle":{"type": "minecraft:dust_color_transition", "from_color": [0.0,0.8,1.0], to_colo. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/shock_effect/potion/convert.mcfunction)

### `lbc:other/shock_effect/potion/get_effect`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/projectiles/arrows2.mcfunction`; условие вызова: `execute if score tmp lbc.math matches 126 run function lbc:other/shock_effect/potion/get_effect`. Состояние scoreboard: scoreboard players operation @s effect_shock_potion += 10 lbc.math. Передаёт шаг в `lbc:other/shock_effect/zap`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/shock_effect/potion/get_effect.mcfunction)

### `lbc:other/shock_effect/potion/particle`

Автоматический caller среди исследованных функций не найден; при ручном запуске выполняются действия ниже. Команда: `$particle electric_spark ~ ~ ~ $(Radius) 0.2 $(Radius) 0 1`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/shock_effect/potion/particle.mcfunction)

## Файлы: other / shock_effect / zap

### `lbc:other/shock_effect/zap/as_display`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/shock_effect/zap/end.mcfunction`; условие вызова: `execute as @e[type=minecraft:item_display,tag=lightning_ray,tag=!done] run function lbc:other/shock_effect/zap/as_display`. Состояние scoreboard: scoreboard players set @s death_timer 4. Передаёт шаг в `lbc:other/shock_effect/zap/tp`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/shock_effect/zap/as_display.mcfunction)

### `lbc:other/shock_effect/zap/cast`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/shock_effect/zap.mcfunction`; условие вызова: `execute anchored eyes positioned ^ ^ ^ facing entity @e[type=#minecraft:mobs,distance=..10,tag=!damager,tag=!nodamage,tag=!nodamage1,tag=!spectator,sort=nearest,limit=1,tag=!strict_map_object] eyes run function lbc:other/shock_effect/zap/cast`. Создаёт `item_display`. Обновляет NBT/components: modify entity @e[type=minecraft:item_display,tag=lightning_ray,tag=!done,limit=1] transformation.scale set value [3.0f,3.0f,1.0. Состояние scoreboard: scoreboard players set *test_ray Distance 0; scoreboard players operation @e[type=minecraft:item_display,tag=lightning_ray,tag=!done,limit=1] lbcID2 = @s lbcID2; scoreboard players operation @e[type=minecraft:item_display,tag=lightning_ray,tag=!done,limit=1] team_number = @s team_n; scoreboard players operation *check_id lbcID2 = @s lbcID2. Передаёт шаг в `lbc:other/shock_effect/zap/loop`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/shock_effect/zap/cast.mcfunction)

### `lbc:other/shock_effect/zap/check`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/shock_effect/zap/loop.mcfunction`; условие вызова: `function lbc:other/shock_effect/zap/check`. Состояние scoreboard: scoreboard players add *test_ray Distance 1. Передаёт шаг в `lbc:other/shock_effect/zap/hit`, `lbc:other/shock_effect/zap/hit_entity`, `lbc:other/shock_effect/zap/end`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/shock_effect/zap/check.mcfunction)

### `lbc:other/shock_effect/zap/end`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/shock_effect/zap/check.mcfunction`; условие вызова: `execute if score *test_ray Distance matches 50.. run function lbc:other/shock_effect/zap/end`. Состояние scoreboard: scoreboard players set *test_ray Distance 401. Передаёт шаг в `lbc:other/shock_effect/zap/as_display`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/shock_effect/zap/end.mcfunction)

### `lbc:other/shock_effect/zap/hit`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/shock_effect/zap/check.mcfunction`; условие вызова: `execute unless block ~ ~ ~ #minecraft:airs run function lbc:other/shock_effect/zap/hit`. Состояние scoreboard: scoreboard players set *chain lbc.math 0; scoreboard players set *test_ray Distance 401. Передаёт шаг в `lbc:other/shock_effect/zap/as_display`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/shock_effect/zap/hit.mcfunction)

### `lbc:other/shock_effect/zap/hit_entity`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/shock_effect/zap/check.mcfunction`; условие вызова: `execute as @e[type=#minecraft:mobs,dx=0,dy=0,dz=0,distance=..5,tag=!spectator,tag=!damager,tag=!damager1,tag=!chained] positioned ~-0.99 ~-0.99 ~-0.99 if entity @s[dx=0,dy=0,dz=0] positioned ~0.99 ~0.99 ~0.99 as @e[distance=..100,tag=damager,limit=1] run function lbc:other/shock_effect/zap/hit_entity`. Состояние scoreboard: scoreboard players remove *chain lbc.math 1; scoreboard players set *test_ray Distance 401. Передаёт шаг в `lbc:other/shock_effect/zap/as_display`, `lbc:other/shock_effect/zap/as_target/run`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/shock_effect/zap/hit_entity.mcfunction)

### `lbc:other/shock_effect/zap/loop`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/shock_effect/zap/cast.mcfunction`; условие вызова: `function lbc:other/shock_effect/zap/loop`. Передаёт шаг в `lbc:other/shock_effect/zap/check`, `lbc:other/shock_effect/zap/loop`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/shock_effect/zap/loop.mcfunction)

### `lbc:other/shock_effect/zap/run`

Автоматический caller среди исследованных функций не найден; при ручном запуске выполняются действия ниже. Создаёт `marker` с {Tags:["raycaster","lightning_ray_marker"]}. Состояние scoreboard: scoreboard players operation @e[type=minecraft:marker,distance=..2,tag=lightning_ray_marker,limit=1] lbcID2 = @s lbcID2; scoreboard players operation @e[type=minecraft:marker,distance=..2,tag=lightning_ray_marker,limit=1] team_number = @s te. Передаёт шаг в `lbc:other/shock_effect/zap/cast`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/shock_effect/zap/run.mcfunction)

### `lbc:other/shock_effect/zap/tp`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/shock_effect/zap/as_display.mcfunction`; условие вызова: `function lbc:other/shock_effect/zap/tp`. Команда: `tp @s ^ ^ ^ ~ ~`; `tag @s add done`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/shock_effect/zap/tp.mcfunction)

## Файлы: other / shock_effect / zap / as_target

### `lbc:other/shock_effect/zap/as_target/end`

Автоматический caller среди исследованных функций не найден; при ручном запуске выполняются действия ниже. Команда: `tag @e[type=#minecraft:mobs,tag=chained] remove chained`; `say END`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/shock_effect/zap/as_target/end.mcfunction)

### `lbc:other/shock_effect/zap/as_target/run`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/shock_effect/zap/hit_entity.mcfunction`; условие вызова: `execute as @e[type=#minecraft:mobs,dx=0,dy=0,dz=0,distance=..6,tag=!damager,tag=!spectator,tag=!chained,limit=1] positioned ~-.99 ~-.99 ~-.99 if entity @s[dx=0,dy=0,dz=0] at @s run function lbc:other/shock_effect/zap/as_target/run`. Наносит 6 урона по @s типа `arrow`. Состояние scoreboard: scoreboard players operation @s effect_shock_potion = *temp.effect lbc.math. Передаёт шаг в `lbc:other/shock_effect/zap/cast`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/shock_effect/zap/as_target/run.mcfunction)

## Файлы: other / shock_effect / zap / visual

### `lbc:other/shock_effect/zap/visual/run`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/shock_effect/zap/visual/tick.mcfunction`; условие вызова: `execute as @e[type=minecraft:item_display,tag=done,tag=lightning_ray] at @s unless entity @e[type=minecraft:marker,distance=..20,tag=stopper_magic,limit=1] run function lbc:other/shock_effect/zap/visual/run`. Состояние scoreboard: scoreboard players remove @s death_timer 1. Завершение: удаляет сущность (`@s`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/shock_effect/zap/visual/run.mcfunction)

### `lbc:other/shock_effect/zap/visual/tick`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/shock_effect/zap/as_display.mcfunction`; условие вызова: `schedule function lbc:other/shock_effect/zap/visual/tick 2t`. Передаёт шаг в `lbc:other/shock_effect/zap/visual/run`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/shock_effect/zap/visual/tick.mcfunction)

## Файлы: other / stand_arrow

### `lbc:other/stand_arrow/hit`

При попадании по сущности выбирает случайное число1–100: 1–10 убивает цель, кроме `sin_greed_active`; 11–100 при отсутствии `have_stand` и положительного `overheaven` ставит `have_stand` и пытается выдать loot `stand_oraora`. Следующая команда `no_stand` повторяет тот же gate, но уже после того, как `stand_gain` поставил `have_stand`; поэтому эта ветвь не выполнится после успешного grant, а при уже имеющемся stand также не срабатывает. Caller дополнительно выполняет `hit_pre`, который выбрасывает loot `stand_arrow`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/stand_arrow/hit.mcfunction)
### `lbc:other/stand_arrow/hit_pre`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/effects/effect_stand_arrow_hit.mcfunction`; условие вызова: `execute unless entity @e[type=minecraft:marker,distance=..20,tag=stopper_magic,limit=1] run function lbc:other/stand_arrow/hit_pre`. Loot: spawn lbc:stand_arrow → ~. Передаёт шаг в `lbc:other/stand_arrow/hit`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/stand_arrow/hit_pre.mcfunction)

### `lbc:other/stand_arrow/homing_lightning_spawn`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/spell1_default.mcfunction`; условие вызова: `execute unless score @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{homing_lightning:1}}}}] coldownlightning matches 1.. run function lbc:other/stand_arrow/homing_lightning_spawn`. Создаёт `marker` с {data:{lifetime:300},Tags:["everlasting_bolt","rare","rc_1sec","slowraycast","magic"]}. Состояние scoreboard: scoreboard players set @s coldownlightning 30. Передаёт шаг в `lbc:workingitems/awaken_tuntija_wands/boltbolt/get_id_with_tp`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/stand_arrow/homing_lightning_spawn.mcfunction)

### `lbc:other/stand_arrow/no_stand`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/stand_arrow/hit.mcfunction`; условие вызова: `execute if score tmp lbc.math matches 11..100 unless entity @s[tag=have_stand] unless entity @s[scores={overheaven=1..}] run function lbc:other/stand_arrow/no_stand`. Команда: `tellraw @s {"translate":"no_stand"}`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/stand_arrow/no_stand.mcfunction)

### `lbc:other/stand_arrow/stand_clocks_overheaven_upgrade`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/stand_arrow/tick_10t_stand_clocks.mcfunction`; условие вызова: `execute if entity @s[scores={y=20000..,overheaven=..0}] run function lbc:other/stand_arrow/stand_clocks_overheaven_upgrade`. Loot: spawn lbc:homing_lightning → ~. Состояние scoreboard: scoreboard players set @s overheaven 5. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/stand_arrow/stand_clocks_overheaven_upgrade.mcfunction)

### `lbc:other/stand_arrow/stand_gain`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/stand_arrow/hit.mcfunction`; условие вызова: `execute if score tmp lbc.math matches 11..100 unless entity @s[tag=have_stand] unless entity @s[scores={overheaven=1..}] run function lbc:other/stand_arrow/stand_gain`. Loot: give lbc:stand_oraora → @s. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/stand_arrow/stand_gain.mcfunction)

### `lbc:other/stand_arrow/tick_10t_stand_clocks`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/players_10t.mcfunction`; условие вызова: `execute if entity @s[tag=have_stand,tag=have_stand_clocks_active] unless entity @e[type=minecraft:marker,distance=..20,tag=stopper_magic,limit=1] run function lbc:other/stand_arrow/tick_10t_stand_clocks`. Наносит 1 урона по @s типа `in_fire`. Передаёт шаг в `lbc:other/stand_arrow/stand_clocks_overheaven_upgrade`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/stand_arrow/tick_10t_stand_clocks.mcfunction)

## Файлы: other / workingitems

### `lbc:other/workingitems/razorpine2t`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/players_2t.mcfunction`; условие вызова: `execute at @s[tag=razorpine_active] run function lbc:other/workingitems/razorpine2t`. Состояние scoreboard: scoreboard players remove @s coldown18 4; scoreboard players remove @s coldown18b 4. Передаёт шаг в `lbc:workingitems/razorpine`, `lbc:workingitems/razorpine2`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/workingitems/razorpine2t.mcfunction)




## Дополненная ручная сводка семейств

### Chaos Chestplate: события и случайные режимы

`items/damaged` запускает `effectselect` при повреждении владельца с `custom_data.chaoschestplate:1` на груди; рядом не должно быть stopper_magic в 20 блоках или non-spectator игрока с `nomagic_active` в 20. Это равномерный выбор 1..17, без отдельного расхода и cooldown; эффекты включают стрелы, отбрасывание/левитацию врагов, огонь, лечение, урон, телепорт, волков с Owner=владелец, шаблон corrosion, краткую Resistance, дым/слабость/отравление/перекачку, скорость, поглощение, acid и четыре TNT с fuse=5 тиков (0,25 секунды). Ношение того же ключа на leggings запускает отдельный 1..7 набор при событии прыжка, а на boots при спринте на земле есть `chance5` (5% на проверку); последний набор включает скорость, прыжок, phase, nitroflight, phase_speed, мост и flash. Пассивный `effectselect_see` (13 вариантов эффектов) в изученных функциях LBC не имеет буквального caller, поэтому он описан как ручной helper, не как установленный штатный триггер. [События повреждения/прыжка/спринта](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/damaged.mcfunction), [таблицы выбора](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/effectselect.mcfunction).

### Baffledend: финальная цепочка

Snowball с `custom_data.baffled_of_everything:1` запускает восемь отложенных стадий; складывая буквальные schedule delays, финальный шаг приходит через 52 секунды, если `new_game` не очистит расписание. Шаги вызывают `snus` у мобов, затем создают два invulnerable silverfish на каждый `@e[tag=!spectator]`; поэтому это не только два на игрока. Дальше чистится большая плоскость X/Z от −96 до144 на высоте исполнителя, появляются riders, выдаются случайные repeating-command-block drops (лимиты 50 skeleton, 25 bob, 25 aimbob и 1 Sauvojen), loot `yameteluckyblock`, до 15 magic-bean drops, а финал очищает эффекты non-spectator игроков, даёт Luck с amplifier4 без конечного таймера, выдаёт по64 каждого из трёх custom lucky-block предметов и advancement. [Старт из egg dispatcher](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/eggs.mcfunction), [цепочка с финальным шагом](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/baffledend/8.mcfunction).

### Shishend: сценарий конца технологии

Snowball `shish_of_everything:1` запускает последовательность на 100 секунд суммарных `schedule`: Forest-маркеры у каждого non-spectator игрока; cooldown `coldownshish=34` и Levitation на3 секунды с amplifier16; worldborder center 0,0 и размер382.75; затем четыре electro-ray markers на игрока со случайными координатами X/Z в пределах ±10, поднятых на8, и теги electro emitter. Поздние стадии выдают yellow lightning, распределяют 40 supply-point markers из `(0,128,0)` в радиусе192 и запускают только один выбранный supply crate helper, затем ставят cyborg skeleton босса у каждого совпавшего player/crate контекста и вызывают внешний `other/techno/cyborg_boss/tick`. Перед завершением создаются по30 stopper/stopper_magic markers на игрока в случайных смещениях ±30; финал выдаёт `source_of_energy` и advancement `end_of_technomagic`. Эти функции не оплачивают запуск ресурсом, а `end_resets`/`new_game` могут отменить ожидающие schedule calls; внешний crate/boss package определяет downstream-поведение. [Старт](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/shishend/1.mcfunction), [подготовка и финал](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/shishend/6.mcfunction).

### Everythingend и остановка сценариев

Snowball `end_of_everything:1` запускает 58 numbered stages от абсолютной позиции `(0,64,0)`; задержки между ними в сумме ровно 1863 тика = 93,15 секунды. Полная последовательность очищает растущие кубы воздуха, затем заливает лаву, позже добавляет explosion/raycast/wave события, боссов и заключительные награды; фактические payloads остаются в каждом из 58 source cards, а `trueknowledge` и `stevari` — два дополнительных helper source. В самом сценарии нет общего возврата уже удалённых/поставленных блоков к прежнему состоянию. `end_resets` снимает многие эффекты, очищает расписания и служебное состояние, включая `keep_inventory=false`; `other/new_game/click` дополнительно чистит игроков/сущности, вызывает `reset_gamerules` в overworld, nether, end и imprinted и вызывает `swrg:maploader/clear` с размещением шаблонов карты. Эти reset-функции меняют gamerules и другие состояния, но не являются транзакционным откатом уже выполненных этапов Everythingend. Подробности полного reset есть в [runtime](lbc_runtime.md#старт-матча-reset-и-смертьвозрождение); финальные вызовы `other/nuke/nuke_eoe` — внешняя граница этого job. [Суммирование delays и входной dispatcher](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/eggs.mcfunction), [очистка расписаний](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/end_resets.mcfunction), [новая игра](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/new_game/click.mcfunction).

### Instant items, Infinity и Overload Crux

`players_tick` обслуживает `instants/select`, пока активен `instantitem_active`. Ветка `#lbcskill=1` и skill2 использует выбранный main-hand item и `select2`; прочие случаи попадают в старую ветку, которая ищет часть ключей во всём Inventory (включая `penis_bomb`, infinity, nexus wires и Touhou bridge). Для `infinity_rand:1` вызывается случайный 1..14 результат равного веса; каждый результат удаляет один такой предмет и выдаёт advancement, но постоянные `manamax`, атрибуты и `infinite_*` tags в этой функции не сбрасываются. Результаты варьируются от `manamax/manaregen=100000000` и больших чисел атрибутов до предмета infinite lucky block; два последних варианта прибавляют `infinity_scale` modifiers к широкому набору атрибутов.

`overload_crux/give` — автоматическая единоразовая выдача при `mana<=0`, если у игрока нет `obtained_crux`; успешный вызов создаёт loot и ставит этот tag. `use` тратит ровно 7500 mana, делает пять вызовов `negative_mana/tentacle_full_set`, заменяет каждый надетый vanilla diamond armor slot его Overload-set loot-результатом (helmet/chest/leggings/boots проверяются отдельно), проигрывает звук на каждую успешную замену и удаляет один crux. У функции нет собственного cooldown/возврата предмета; детали tentacle hit принадлежат [mana job](lbc_mana.md) и [вызванному helper](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/negative_mana/tentacle_full_set.mcfunction).

### Penises: числовой код, игроки и мобы

Старый `instants/select_old` при наличии `penis_bomb` в инвентаре вызывает `player_select`, который независимо проверяет все ключи `penis_num:1..6`; если их несколько, срабатывают все. Для игрока каждый handler очищает ровно один предмет с тем же `penis_num`, затем применяет эффект: 1 — 6 arrow damage; 2 — 4 damage плюс Slowness/Weakness/Nausea на 8/4/6 секунд, amplifier5; 3 — TNT с fuse 2 тика (0,1 секунды); 4 — Blindness8 секунд и Levitation1 секунда amplifier15 плюс молния; 5 — 10 arrow damage, Slowness1 секунда amplifier127 и teleport на пять блоков назад и пять вверх; 6 — отдельная ветка multicast.

Другой вход `workingitems/penis_mob` вызывает `penises/select` у ближайшего не-player mob в1.2 блока от `penis_bomb` item entity, кроме `no_penis`; тогда numeric item entity проверяется по `Item.custom_data.penis_num`, а handler исполняется как моб и предмет не очищается его player-only `clear`. У multicast команда `execute at @e[tag=!spectator] run damage @s 0.1 arrow` меняет позицию, но не исполнителя: при этом многократно получает урон исходный `@s`, а не каждая выбранная сущность; число повторов равно количеству найденных entity. Остальные подробности числовых вариантов закреплены за прямыми файлами `penises/*.mcfunction`.

### Phantom Arrows и lightning/shock markers

Cloud с тегом `phantomarrows` каждые10 тиков обслуживается `area_effect_cloud_10t`: он находит ровно одну случайную цель-моба в радиусе16 (исключает spectator и `nomagic_active`) и вызывает `select` из точки на Y+1.7. Равномерный выбор1..8 создаёт по одному паттерну из стрел damage2.5 с pickup=creative-only; четыре направления — глобальные ±X/±Z на5 блоков, диагонали — ±3.5, скорости прописаны мировыми Motion-векторами, поэтому паттерн не вращается по yaw цели и эти источники сами не копируют Owner/команду. Только первый шаблон задаёт PierceLevel100; прочие шаблоны в буквальном NBT его не имеют. [Выбор паттерна](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/phantomarrows/select.mcfunction), [обслуживание clouds](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/area_effect_cloud_10t.mcfunction).

Shock potion cloud конвертируется через `raycastselector_area_cloud` helper `convert`: радиус5, Duration300 тиков (15 секунд), ReapplicationDelay10 тиков (0,5 секунды), hidden Slowness на1 тик и custom particles; original cloud получает `done`. `potion/get_effect` запускает zap при положительном `effect_shock_potion` и добавляет10, только пока текущий score меньше40 (при обычном старте с кратных10 это даёт максимум40); damage-hook переносит этот заряд в одноразовый chain. Zap строит луч с шагом0,25 и порогом Distance50; общий target selector с `dx=dy=dz=0` всё равно ограничивает геометрию единичным кубом, а отдельные `distance` не растягивают его в сферу. Каждое попадание в цепи наносит6 arrow damage, выбирает следующую цель в20, но общий `nodamage1` союзников в этой функции не снимается; сам луч/графика удаляются позже отдельным visual schedule или stopper-условием. [Конвертация area cloud](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/shock_effect/potion/convert.mcfunction), [цепь](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/shock_effect/zap.mcfunction).

### Freeze, Evoker и побочные состояния

`freeze` ведёт `cold` по десяти полосам по10 очков: с 1..10 до 91..99 растут уровни Slowness, Mining Fatigue, Weakness, Unluck и затем Blindness; на100+ `full` сохраняет UUID/позицию, сбрасывает `cold/cold_cd`, задаёт `frozen=10` и ставит ледяной блок. Мобский `mobs_1s` tick уменьшает frozen по секунде; на нуле macro снимает лёд. На каждом секундном cooldown-check cold сначала ждёт `cold_cd`, затем уменьшается на1; само добавление холода задают ледяные weapon/projectile callers (например, ice sword +10×Slowness multiplier, snow wand +2). Пока `frozen>0`, отдельный `mobs_1s` dispatcher обновляет Resistance на25 секунд с amplifier4 каждую секунду; это относится к внешнему lifecycle score, а не к одноразовой freeze-функции.

`evoker/cast1..3` — общий трёхударный конус: первый fang появляется в локальной позиции вперёд1.5, ещё один через5 тиков на3 и последний через5 тиков на4.5; intermediate caster tags перекидываются `evokercast1→2→3`, затем убираются. Число целей/стоимость задаёт spell caller, а в этой функции нет mana расхода. `players_2t` может вызывать `bullshit/anal` при Hunger amplifier≥1 и отдельном 5% predicate (только particles/sound), а `oral` при Nausea amplifier≥1 и 5% predicate (добавочно Blindness/Darkness на1 секунду); `legacy=1` отключает обе реакции.

### Pumpkinator, Sheep Black Hole, Stand Arrow и Razorpine bridge

Rare dispatcher обслуживает `pumpkinator` каждый raycast проход с позицией на1.5 блока ниже marker: тик заполняет strict pumpkin-каркасом область, затем с `chance1` (1%) вызывает blast; blast наносит мобам до5 блоков 10 cactus damage у позиции самого marker и строит pumpkin-столб с carved pumpkin на вершине. `sheep_black_hole` каждый свой rare проход place-template-ит black hole, тянет допустимые сущности из3..20 шагом1 блока к marker, наносит10 outside_border тем, кто внутри5 и не имеет `void_resistance`, и удаляет projectile/item entities в5; исключены spectator, `sin_greed_active` и strict-map objects. В этих функциях нет отдельного timer/cleanup у marker, поэтому прекращение обработки зависит от удаления сущности вызывающей системой.

`effect_stand_arrow_hit` обрабатывается раз в тик независимо от sin greed, если его score положителен; при отсутствии stopper_magic запускает hit_pre (сначала hit roll, затем loot spawn stand_arrow), но score всё равно убывает на1 даже если stopper заблокировал действие. В hit число1..10 означает 10% kill цели, кроме `sin_greed_active`; 11..100 даёт stand только цели без `have_stand` и `overheaven>=1`; при успешном `stand_gain` сразу ставится `have_stand`, поэтому следующая `no_stand` проверка с теми же условиями уже не проходит. Отдельно активные stand clocks наносят владельцу 1 fire damage каждые10 тиков, кроме `lifestealer`; при Y≥20000 и `overheaven<=0` переводят в `overheaven=5`, loot homing lightning и achievement.

`homing_lightning_spawn` создаёт20 everlasting-bolt markers с `data.lifetime=300` и cooldown30 секунд; внешний boltbolt `get_id_with_tp` копирует caster `lbcID2/team_number` и активирует marker, но не задаёт Owner UUID; обычный `raycasters_1s` поднимает Lifetime по20 за вызов, значит nominal lifetime здесь15 секунд. `other/workingitems/razorpine2t` — игрокский bridge через `players_2t`: пока есть active tag, он независимо проверяет два cooldown scores и при каждом значении ≤−2 зовёт razorpine/razorpine2; после проверок вычитает4 из обоих счетчиков. Конечные arrow Motion/owner setup принадлежат этим внешним helpers, а вызов wrapper-а сам не задаёт mana расход.
