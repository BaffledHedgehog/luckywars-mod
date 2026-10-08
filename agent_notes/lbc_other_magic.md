# LBC: lbc other magic

Задание `lbc_other_magic`: проверены ровно 247 источников из `agent_notes/inventory/research_jobs.json`. В описаниях caller означает обнаруженную ссылку в проиндексированных функциях; если его нет, возможность ручного запуска остаётся. Условие caller приведено из literal command. `dx/dy/dz` вместе с `distance` задают совместное пересечение, а `execute as` сам по себе не переносит позицию. `/effect give` измеряет длительность в секундах; `fuse:2s` в SNBT — Short на два тика (0,1 с), тогда как `schedule 2s` — две секунды.


## Академия магии: проверка и применение апгрейда

`can_upgrade` — короткая выдача статуса, а не единое условие покупки: если в руке не `unstable_spellbook`, она передаёт работу в `can_upgrade_book`, который проигрывает звук, показывает `can_upgrade`, временно переносит предмет через блок-хранилище, ставит `upgrade_available:1` и сбрасывает `upgradable:0`. Другие функции вызывают этот сигнал при своих условиях (например, пороги очков/scoreboard, шанс или действие конкретной палочки), поэтому проверку конкретного предмета следует читать в caller.

`interacted_upgrade` — dispatcher с 53 отдельными последовательными проверками `custom_data` предмета в руке. Он выставляет `upgraded:1` и `mode:1`, добавляет описание апгрейда, обновляет challenge progress, после чего каждый совпавший ключ вызывает свой handler; для обычного одноключевого предмета срабатывает одна ветвь, но предмет с несколькими matching keys может вызвать несколько handlers подряд в порядке исходника. Сами эти функции не задают единую цену/расход ресурсов; каждый handler сообщает текст и применяет свой tier modifier. [Проверка статуса](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/can_upgrade_book.mcfunction), [dispatcher](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_upgrade.mcfunction).

## Файлы: other / grimoire_dungeon

### `lbc:other/grimoire_dungeon/room_reward`

Группа вызывается из dungeon raycast по marker-тегу `room_reward`. В пустом блоке под точкой запускается случайная награда (одна из 12 веток); если там barrier и игрок не в spectator находится в пределах 8 блоков, запускается подготовка босса. Условия оформлены двумя независимыми `execute if`, а не конструкцией `else`. [Источник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/room_reward.mcfunction)
### `lbc:other/grimoire_dungeon/room_reward_4`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/raycastselector_grimoire_dungeon.mcfunction`; условие вызова: `execute if entity @s[tag=room_reward_4] run return run function lbc:other/grimoire_dungeon/room_reward_4`. Передаёт шаг в `lbc:other/grimoire_dungeon/room_reward`. Завершение: удаляет сущность (`@s`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/room_reward_4.mcfunction)

### `lbc:other/grimoire_dungeon/room_reward_4_1`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/raycastselector_grimoire_dungeon.mcfunction`; условие вызова: `execute if entity @s[tag=room_reward_4_1] run return run function lbc:other/grimoire_dungeon/room_reward_4_1`. Передаёт шаг в `lbc:other/grimoire_dungeon/room_reward`. Завершение: удаляет сущность (`@s`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/room_reward_4_1.mcfunction)

### `lbc:other/grimoire_dungeon/room_reward_random`

После того как `room_reward` находит воздух в точке награды, эта функция выбирает равномерное целое 1–12 и вызывает ровно соответствующую ветку: мини-босс; обычный или grimoire-сундук; гнездо ежей; скелеты, зомби, pillagers, creepers или breezes; lucky-мобы; lucky-блоки; шахта. Затем исходный marker удаляется. Это выбор одного варианта, а не последовательный запуск всех двенадцати помощников. [Источник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/room_reward_random.mcfunction)
### `lbc:other/grimoire_dungeon/mobs/golden_skeleton/10s`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/mobs_10s.mcfunction`; условие вызова: `execute if entity @s[type=minecraft:skeleton,tag=golden_skeleton] run function lbc:other/grimoire_dungeon/mobs/golden_skeleton/10s`. Создаёт `marker` с {Tags:["slowraycast","player","triplesword_portal","magic"]}. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/golden_skeleton/10s.mcfunction)

### `lbc:other/grimoire_dungeon/mobs/golden_skeleton/portal_tick`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/raycastselector_player.mcfunction`; условие вызова: `execute if entity @s[tag=triplesword_portal] run return run function lbc:other/grimoire_dungeon/mobs/golden_skeleton/portal_tick`. Передаёт шаг в `lbc:other/grimoire_dungeon/mobs/golden_skeleton/portal_tick_attack`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/golden_skeleton/portal_tick.mcfunction)

### `lbc:other/grimoire_dungeon/mobs/golden_skeleton/portal_tick_attack`

Автоматический вход из `portal_tick` после Lifetime 60+ (60 тиков = 3 секунды) и независимого `chance30` на каждом обслуживающем вызове. Создаёт item-display с моделью `lbc:triple_golden_sword`, наводит его на ближайшую подходящую сущность в радиусе 30 и передаёт в `universal_razbros`; marker портала удаляется на Lifetime 120+ (6 секунд). Это даёт примерно трёхсекундное окно атак с шансом 30% за тик после первых 3 секунд; точное число ударов случайно и зависит от частоты внешнего raycast scheduler. [Источник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/golden_skeleton/portal_tick_attack.mcfunction). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/golden_skeleton/portal_tick_attack.mcfunction)

## Файлы: other / grimoire_dungeon / mobs / grimoire_boss

### `lbc:other/grimoire_dungeon/mobs/grimoire_boss/death`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/tick.mcfunction`; условие вызова: `execute at @s positioned ~ ~-6 ~ unless entity @e[type=minecraft:warden,distance=..1,tag=grimoire_boss_hitbox,limit=1] run function lbc:other/grimoire_dungeon/mobs/grimoire_boss/death`. Завершение: удаляет сущность (`@e[tag=boss_piece]`); удаляет сущность (`@s`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/death.mcfunction)

### `lbc:other/grimoire_dungeon/mobs/grimoire_boss/falling_meteor`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/raycastselector_other.mcfunction`; условие вызова: `execute if entity @s[tag=falling_tuff] run return run function lbc:other/grimoire_dungeon/mobs/grimoire_boss/falling_meteor`. Наносит 10 урона по @s типа `falling_block`. Передаёт шаг в `lbc:other/grimoire_dungeon/mobs/grimoire_boss/falling_meteor_break`. Завершение: удаляет сущность (`@s[scores={Lifetime=100..}]`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/falling_meteor.mcfunction)

### `lbc:other/grimoire_dungeon/mobs/grimoire_boss/falling_meteor_break`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/falling_meteor.mcfunction`; условие вызова: `execute unless block ~ ~1 ~ #minecraft:airs run function lbc:other/grimoire_dungeon/mobs/grimoire_boss/falling_meteor_break`. Изменяет блоки: fill ~ ~1 ~ ~ ~1 ~ minecraft:air destroy. Завершение: удаляет сущность (`@s`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/falling_meteor_break.mcfunction)

### `lbc:other/grimoire_dungeon/mobs/grimoire_boss/hit`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/tick_true.mcfunction`; условие вызова: `execute if entity @e[type=minecraft:warden,distance=..1,tag=grimoire_boss_hitbox,limit=1,nbt={HurtTime:10s}] run function lbc:other/grimoire_dungeon/mobs/grimoire_boss/hit`. Команда: `execute positioned ~ ~2 ~ run particle minecraft:block{block_state:{Name:"minecraft:tuff_bricks"}} ~ ~ ~ 1 1.6 1 0 40 normal`; `playsound minecraft:entity.iron_golem.hurt master @a ~ ~ ~ 1 0`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/hit.mcfunction)

### `lbc:other/grimoire_dungeon/mobs/grimoire_boss/phase_danmaku`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/random_phase_select.mcfunction`; условие вызова: `execute if score tmp lbc.math matches 3 at @s run function lbc:other/grimoire_dungeon/mobs/grimoire_boss/phase_danmaku`. Команда: `tag @s add phase_danmaku`; `particle minecraft:dust{color:[0.0,0.765,1.0],scale:1.0f} ~ ~ ~ 4 4 4 1 100 normal @a`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/phase_danmaku.mcfunction)

### `lbc:other/grimoire_dungeon/mobs/grimoire_boss/phase_danmaku_at`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/phase_danmaku_end.mcfunction`; условие вызова: `execute at @e[type=#minecraft:mobs,distance=..30,tag=!spectator,tag=!dungeon_mob,sort=nearest,limit=3] run function lbc:other/grimoire_dungeon/mobs/grimoire_boss/phase_danmaku_at`. Передаёт шаг в `lbc:other/grimoire_dungeon/mobs/grimoire_boss/phase_danmaku_at_prot`, `lbc:other/grimoire_dungeon/mobs/grimoire_boss/phase_danmaku_at_swordspawn`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/phase_danmaku_at.mcfunction)

### `lbc:other/grimoire_dungeon/mobs/grimoire_boss/phase_danmaku_at_prot`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/phase_danmaku_at.mcfunction`; условие вызова: `execute if score tmp lbc.math matches 2..7 run function lbc:other/grimoire_dungeon/mobs/grimoire_boss/phase_danmaku_at_prot`. Состояние scoreboard: scoreboard players add tmp1 lbc.math 93. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/phase_danmaku_at_prot.mcfunction)

### `lbc:other/grimoire_dungeon/mobs/grimoire_boss/phase_danmaku_at_swordspawn`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/phase_danmaku_at.mcfunction`; условие вызова: `function lbc:other/grimoire_dungeon/mobs/grimoire_boss/phase_danmaku_at_swordspawn with storage lbc.math`. Передаёт шаг в `lbc:other/grimoire_dungeon/mobs/grimoire_boss/phase_danmaku_at_swordspawn_2`. Использует макропараметры `$(rot)`; значения подставляются вызывающей функцией через `with storage`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/phase_danmaku_at_swordspawn.mcfunction)

### `lbc:other/grimoire_dungeon/mobs/grimoire_boss/phase_danmaku_at_swordspawn_2`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/phase_danmaku_at_swordspawn.mcfunction`; условие вызова: `$execute positioned ~ ~1 ~ rotated $(rot) 0 positioned ^ ^ ^-10 run function lbc:other/grimoire_dungeon/mobs/grimoire_boss/phase_danmaku_at_swordspawn_2 with storage lbc.math`. Создаёт `item_display` с {interpolation_duration:1,shadow_radius:0.0f,shadow_strength:0.0f,teleport_duration:1,item_display:"fixed",transformation:[0.0f,0.0f,1.0f,0.0f,1.0f,0.0f,0.0f,0.0f,0.0f,1.0f,0.0f,0.. Использует макропараметры `$(rot)`; значения подставляются вызывающей функцией через `with storage`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/phase_danmaku_at_swordspawn_2.mcfunction)

### `lbc:other/grimoire_dungeon/mobs/grimoire_boss/phase_danmaku_end`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/phase_danmaku_tick.mcfunction`; условие вызова: `execute if score @s Lifetime matches 200.. run function lbc:other/grimoire_dungeon/mobs/grimoire_boss/phase_danmaku_end`. Передаёт шаг в `lbc:other/grimoire_dungeon/mobs/grimoire_boss/phase_danmaku_at`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/phase_danmaku_end.mcfunction)

### `lbc:other/grimoire_dungeon/mobs/grimoire_boss/phase_danmaku_tick`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/tick_true.mcfunction`; условие вызова: `execute at @s if entity @s[tag=phase_danmaku,scores={Lifetime=20..}] if predicate lbc:chance30 run function lbc:other/grimoire_dungeon/mobs/grimoire_boss/phase_danmaku_tick`. Передаёт шаг в `lbc:other/grimoire_dungeon/mobs/grimoire_boss/phase_danmaku_at`, `lbc:other/grimoire_dungeon/mobs/grimoire_boss/phase_danmaku_end`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/phase_danmaku_tick.mcfunction)

### `lbc:other/grimoire_dungeon/mobs/grimoire_boss/phase_laser_at`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/phase_laser_start.mcfunction`; условие вызова: `execute if score @s Lifetime matches 71.. as @e[type=minecraft:marker,distance=..1,tag=grumm_laser,limit=1] at @s run function lbc:other/grimoire_dungeon/mobs/grimoire_boss/phase_laser_at`. Состояние scoreboard: scoreboard players set @s Distance 0. Передаёт шаг в `lbc:other/grimoire_dungeon/mobs/grimoire_boss/phase_laser_loop`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/phase_laser_at.mcfunction)

### `lbc:other/grimoire_dungeon/mobs/grimoire_boss/phase_laser_end`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/phase_laser_start.mcfunction`; условие вызова: `execute if score @s Lifetime matches 250 run function lbc:other/grimoire_dungeon/mobs/grimoire_boss/phase_laser_end`. Состояние scoreboard: scoreboard players set @s Lifetime 0. Завершение: удаляет сущность (`@e[type=minecraft:block_display,distance=..15,tag=spinning_red_penis]`); удаляет сущность (`@e[type=minecraft:marker,distance=..3,tag=grumm_laser]`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/phase_laser_end.mcfunction)

### `lbc:other/grimoire_dungeon/mobs/grimoire_boss/phase_laser_loop`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/phase_laser_at.mcfunction`; условие вызова: `execute at @s run function lbc:other/grimoire_dungeon/mobs/grimoire_boss/phase_laser_loop`. Наносит 10 урона по @s типа `magic`. Состояние scoreboard: scoreboard players add @s Distance 1. Передаёт шаг в `lbc:other/grimoire_dungeon/mobs/grimoire_boss/phase_laser_loop`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/phase_laser_loop.mcfunction)

### `lbc:other/grimoire_dungeon/mobs/grimoire_boss/phase_laser_pre`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/tick_true.mcfunction`; условие вызова: `execute at @s if entity @s[tag=phase_laser,scores={Lifetime=..69}] run function lbc:other/grimoire_dungeon/mobs/grimoire_boss/phase_laser_pre`. Команда: `execute if score @s Lifetime matches 45 run playsound minecraft:lbcsounds.ui_beam master @a[distance=..80] ~ ~ ~ 3 1 0.3`; `execute if score @s Lifetime matches 1 run playsound minecraft:block.beacon.activate master @a ~ ~ ~ 2 0`; `particle minecraft:portal ~ ~ ~ 0 0 0 1 5 normal`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/phase_laser_pre.mcfunction)

### `lbc:other/grimoire_dungeon/mobs/grimoire_boss/phase_laser_selected`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/room_reward/spawn_boss.mcfunction`; условие вызова: `execute positioned ~ ~6 ~ run function lbc:other/grimoire_dungeon/mobs/grimoire_boss/phase_laser_selected`. Создаёт `block_display` с {Tags:["spinning_red_penis","boss_piece"],Rotation:[0.0f,0.0f],view_range:10.0f,shadow_radius:0.0f,shadow_strength:0.0f,billboard:"fixed",start_interpolation:0,interpolation_durati. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/phase_laser_selected.mcfunction)

### `lbc:other/grimoire_dungeon/mobs/grimoire_boss/phase_laser_start`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/tick_true.mcfunction`; условие вызова: `execute at @s if entity @s[tag=phase_laser,scores={Lifetime=70..}] run function lbc:other/grimoire_dungeon/mobs/grimoire_boss/phase_laser_start`. Создаёт `marker` с {Tags:["grumm_laser","slowraycast","boss_piece","magic"]}. Состояние scoreboard: scoreboard players set @s Distance 0. Передаёт шаг в `lbc:other/grimoire_dungeon/mobs/grimoire_boss/phase_laser_at`, `lbc:other/grimoire_dungeon/mobs/grimoire_boss/phase_laser_end`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/phase_laser_start.mcfunction)

### `lbc:other/grimoire_dungeon/mobs/grimoire_boss/phase_meteors`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/random_phase_select.mcfunction`; условие вызова: `execute if score tmp lbc.math matches 2 at @s run function lbc:other/grimoire_dungeon/mobs/grimoire_boss/phase_meteors`. Команда: `tag @s add phase_meteors`; `particle minecraft:dust{color:[0.902,0.569,0.259],scale:1.0f} ~ ~ ~ 4 4 4 1 100 normal @a`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/phase_meteors.mcfunction)

### `lbc:other/grimoire_dungeon/mobs/grimoire_boss/phase_meteors_end`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/phase_meteors_tick.mcfunction`; условие вызова: `execute if score @s Lifetime matches 250.. run function lbc:other/grimoire_dungeon/mobs/grimoire_boss/phase_meteors_end`. Команда: `tag @s add phase_idle`; `tag @s remove phase_meteors`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/phase_meteors_end.mcfunction)

### `lbc:other/grimoire_dungeon/mobs/grimoire_boss/phase_meteors_spawn`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/phase_meteors_tick.mcfunction`; условие вызова: `execute if score @s Lifetime matches ..200 positioned ^ ^ ^7 positioned ~ ~20 ~ run function lbc:other/grimoire_dungeon/mobs/grimoire_boss/phase_meteors_spawn`. Создаёт `marker` с {Tags:["stone_rain_tmp"]}. Передаёт шаг в `lbc:other/grimoire_dungeon/mobs/grimoire_boss/phase_meteors_spawn_marker`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/phase_meteors_spawn.mcfunction)

### `lbc:other/grimoire_dungeon/mobs/grimoire_boss/phase_meteors_spawn_2`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/phase_meteors_spawn_marker.mcfunction`; условие вызова: `execute at @s run function lbc:other/grimoire_dungeon/mobs/grimoire_boss/phase_meteors_spawn_2`. Создаёт `block_display` с {Tags:["slowraycast","falling_tuff","other"],view_range:100.0f,shadow_radius:0.0f,shadow_strength:0.0f,billboard:"fixed",start_interpolation:0,interpolation_duration:1,teleport_dur. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/phase_meteors_spawn_2.mcfunction)

### `lbc:other/grimoire_dungeon/mobs/grimoire_boss/phase_meteors_spawn_marker`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/phase_meteors_spawn.mcfunction`; условие вызова: `execute as @e[type=minecraft:marker,distance=..0.01,tag=stone_rain_tmp,limit=1] at @s run function lbc:other/grimoire_dungeon/mobs/grimoire_boss/phase_meteors_spawn_marker`. Передаёт шаг в `lbc:universal_pos_razb_15_xz`, `lbc:other/grimoire_dungeon/mobs/grimoire_boss/phase_meteors_spawn_2`. Завершение: удаляет сущность (`@s`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/phase_meteors_spawn_marker.mcfunction)

### `lbc:other/grimoire_dungeon/mobs/grimoire_boss/phase_meteors_tick`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/tick_true.mcfunction`; условие вызова: `execute at @s if entity @s[tag=phase_meteors,scores={Lifetime=30..}] if predicate lbc:chance50 run function lbc:other/grimoire_dungeon/mobs/grimoire_boss/phase_meteors_tick`. Передаёт шаг в `lbc:other/grimoire_dungeon/mobs/grimoire_boss/phase_meteors_spawn`, `lbc:other/grimoire_dungeon/mobs/grimoire_boss/phase_meteors_end`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/phase_meteors_tick.mcfunction)

### `lbc:other/grimoire_dungeon/mobs/grimoire_boss/prism_preloop`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/prism_tick.mcfunction`; условие вызова: `execute if score @s Lifetime matches 1 run function lbc:other/grimoire_dungeon/mobs/grimoire_boss/prism_preloop`. Состояние scoreboard: scoreboard players set @s Distance 0. Передаёт шаг в `lbc:other/grimoire_dungeon/mobs/grimoire_boss/prism_preloop_loop`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/prism_preloop.mcfunction)

### `lbc:other/grimoire_dungeon/mobs/grimoire_boss/prism_preloop_loop`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/prism_preloop.mcfunction`; условие вызова: `function lbc:other/grimoire_dungeon/mobs/grimoire_boss/prism_preloop_loop`. Состояние scoreboard: scoreboard players add @s Distance 1. Передаёт шаг в `lbc:other/grimoire_dungeon/mobs/grimoire_boss/prism_preloop_loop`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/prism_preloop_loop.mcfunction)

### `lbc:other/grimoire_dungeon/mobs/grimoire_boss/prism_shoot`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/prism_tick.mcfunction`; условие вызова: `execute if score @s Lifetime matches 31.. run function lbc:other/grimoire_dungeon/mobs/grimoire_boss/prism_shoot`. Состояние scoreboard: scoreboard players set @s Distance 0. Передаёт шаг в `lbc:other/grimoire_dungeon/mobs/grimoire_boss/prism_shoot_loop`. Завершение: удаляет сущность (`@s`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/prism_shoot.mcfunction)

### `lbc:other/grimoire_dungeon/mobs/grimoire_boss/prism_shoot_loop`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/prism_shoot.mcfunction`; условие вызова: `execute positioned ^ ^ ^ run function lbc:other/grimoire_dungeon/mobs/grimoire_boss/prism_shoot_loop`. Состояние scoreboard: scoreboard players add @s Distance 1. Передаёт шаг в `lbc:other/grimoire_dungeon/mobs/grimoire_boss/prism_shoot_loop_dmg`, `lbc:other/grimoire_dungeon/mobs/grimoire_boss/prism_shoot_loop`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/prism_shoot_loop.mcfunction)

### `lbc:other/grimoire_dungeon/mobs/grimoire_boss/prism_shoot_loop_dmg`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/prism_shoot_loop.mcfunction`; условие вызова: `execute if entity @e[type=#minecraft:mobs,dx=0,dy=0,dz=0,distance=..8,tag=!spectator,tag=!dungeon_mob,tag=!hit,limit=1] run function lbc:other/grimoire_dungeon/mobs/grimoire_boss/prism_shoot_loop_dmg`. Наносит 7 урона по @e[type=#minecraft:mobs,dx=0,dy=0,dz=0,distance=..8,tag=!spectator,tag=!dungeon_mob,tag=!hit,limit=1] типа `arrow`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/prism_shoot_loop_dmg.mcfunction)

### `lbc:other/grimoire_dungeon/mobs/grimoire_boss/prism_tick`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/raycastselector_other.mcfunction`; условие вызова: `execute if entity @s[tag=boss_prism] run return run function lbc:other/grimoire_dungeon/mobs/grimoire_boss/prism_tick`. Передаёт шаг в `lbc:other/grimoire_dungeon/mobs/grimoire_boss/prism_preloop`, `lbc:other/grimoire_dungeon/mobs/grimoire_boss/prism_shoot`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/prism_tick.mcfunction)

### `lbc:other/grimoire_dungeon/mobs/grimoire_boss/random_phase_select`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/tick_true.mcfunction`; условие вызова: `execute if entity @s[tag=phase_idle] if predicate lbc:chance1 run function lbc:other/grimoire_dungeon/mobs/grimoire_boss/random_phase_select`. Состояние scoreboard: scoreboard players set @s Lifetime 0. Передаёт шаг в `lbc:other/grimoire_dungeon/mobs/grimoire_boss/phase_laser_selected`, `lbc:other/grimoire_dungeon/mobs/grimoire_boss/phase_meteors`, `lbc:other/grimoire_dungeon/mobs/grimoire_boss/phase_danmaku`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/random_phase_select.mcfunction)

### `lbc:other/grimoire_dungeon/mobs/grimoire_boss/tick`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/raycastselector_other.mcfunction`; условие вызова: `execute if entity @s[tag=grimoire_boss_visual] run return run function lbc:other/grimoire_dungeon/mobs/grimoire_boss/tick`. Передаёт шаг в `lbc:other/grimoire_dungeon/mobs/grimoire_boss/tick1`, `lbc:other/grimoire_dungeon/mobs/grimoire_boss/tick_true`, `lbc:other/grimoire_dungeon/mobs/grimoire_boss/tppos`, `lbc:other/grimoire_dungeon/mobs/grimoire_boss/death`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/tick.mcfunction)

### `lbc:other/grimoire_dungeon/mobs/grimoire_boss/tick1`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/tick.mcfunction`; условие вызова: `execute positioned ~ ~-6 ~ if entity @e[type=minecraft:warden,distance=..1,tag=grimoire_boss_hitbox,limit=1,team=!] run function lbc:other/grimoire_dungeon/mobs/grimoire_boss/tick1`. Команда: `execute if entity @e[type=minecraft:warden,distance=..1,tag=grimoire_boss_hitbox,limit=1,team=1] run team join 1 @s`; `execute if entity @e[type=minecraft:warden,distance=..1,tag=grimoire_boss_hitbox,limit=1,team=2] run team join 2 @s`; `execute if entity @e[type=minecraft:warden,distance=..1,tag=grimoire_boss_hitbox,limit=1,team=3] run team join 3 @s`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/tick1.mcfunction)

### `lbc:other/grimoire_dungeon/mobs/grimoire_boss/tick_true`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/tick.mcfunction`; условие вызова: `execute positioned ~ ~-6 ~ if entity @e[type=minecraft:warden,distance=..1,tag=grimoire_boss_hitbox,limit=1] run function lbc:other/grimoire_dungeon/mobs/grimoire_boss/tick_true`. Передаёт шаг в `lbc:other/grimoire_dungeon/mobs/grimoire_boss/hit`, `lbc:other/grimoire_dungeon/mobs/grimoire_boss/random_phase_select`, `lbc:nodamage_for_team`, `lbc:other/grimoire_dungeon/mobs/grimoire_boss/phase_laser_pre`, `lbc:other/grimoire_dungeon/mobs/grimoire_boss/phase_laser_start`, `lbc:other/grimoire_dungeon/mobs/grimoire_boss/phase_meteors_tick`, `lbc:other/grimoire_dungeon/mobs/grimoire_boss/phase_danmaku_tick`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/tick_true.mcfunction)

### `lbc:other/grimoire_dungeon/mobs/grimoire_boss/tppos`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/tick.mcfunction`; условие вызова: `execute positioned ~ ~-6 ~ unless entity @e[type=minecraft:warden,distance=..1,tag=grimoire_boss_hitbox,limit=1] at @e[tag=grimoire_boss_hitbox] if score @s lbcID2 = @e[distance=..0.001,tag=grimoire_boss_hitbox,limit=1] lbcID2 run function lbc:other/grimoire_dungeon/mobs/grimoire_boss/tppos`. Команда: `tp @s @e[type=minecraft:warden,distance=..0.001,tag=grimoire_boss_hitbox,limit=1]`; `execute at @s run tp @s ~ ~6 ~ ~ 0`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/tppos.mcfunction)

## Файлы: other / grimoire_dungeon / mobs / tiny_skeleton

### `lbc:other/grimoire_dungeon/mobs/tiny_skeleton/hurt`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/mobs_tick_lbc.mcfunction`; условие вызова: `execute if entity @s[tag=tiny_skeleton,nbt={HurtTime:10s}] positioned ~ ~1.4 ~ run function lbc:other/grimoire_dungeon/mobs/tiny_skeleton/hurt`. Создаёт `item_display` с {interpolation_duration:1,shadow_radius:0.0f,shadow_strength:0.0f,teleport_duration:1,item_display:"fixed",transformation:[0.0f,0.0f,2.5f,0.0f,1.7678f,-1.7678f,0.0f,0.0f,1.7678f,1.. Передаёт шаг в `lbc:universal_razbros_up_active`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/tiny_skeleton/hurt.mcfunction)

## Файлы: other / grimoire_dungeon / room_reward

### `lbc:other/grimoire_dungeon/room_reward/spawn_boss`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/room_reward.mcfunction`; условие вызова: `execute if block ~ ~ ~ minecraft:barrier if entity @a[gamemode=!spectator,distance=..8] positioned ~ ~-.5 ~ run function lbc:other/grimoire_dungeon/room_reward/spawn_boss`. Создаёт `item_display` с {Rotation:[-90.0f,0.0f],Tags:["grimoire_boss_visual","slowraycast","other","phase_laser","dungeon_mob","magic","mob"],view_range:10.0f,shadow_radius:0.0f,shadow_strength:0.0f,billb; `item_display` с {Rotation:[90.0f,0.0f],Tags:["grimoire_boss_visual","slowraycast","other","phase_laser","dungeon_mob","magic","mob"],view_range:10.0f,shadow_radius:0.0f,shadow_strength:0.0f,billbo; `item_display` с {Rotation:[0.0f,0.0f],Tags:["grimoire_boss_visual","slowraycast","other","phase_laser","dungeon_mob","magic","mob"],view_range:10.0f,shadow_radius:0.0f,shadow_strength:0.0f,billboa; `item_display` с {Rotation:[180.0f,0.0f],Tags:["grimoire_boss_visual","slowraycast","other","phase_laser","dungeon_mob","magic","mob"],view_range:10.0f,shadow_radius:0.0f,shadow_strength:0.0f,billb; `warden` с {Tags:["grimoire_boss_hitbox","dungeon_mob"],HasVisualFire:0b,Silent:1b,Invulnerable:0b,DeathLootTable:"lbc:grimoire_boss",PersistenceRequired:1b,NoAI:1b,CanPickUpLoot:0b,Health:10. Изменяет блоки: fill ~1 ~ ~1 ~-1 ~ ~-1 minecraft:air strict. Состояние scoreboard: scoreboard players add id_cloud lbc.math 1; scoreboard players operation @e[type=minecraft:warden,distance=..1,limit=1] lbcID2 = id_cloud lbc.math; scoreboard players operation @e[type=minecraft:item_display,distance=..1,tag=grimoire_boss_visual,limit=1] lbcID2 = id_c. Передаёт шаг в `lbc:other/grimoire_dungeon/mobs/grimoire_boss/phase_laser_selected`. Завершение: удаляет сущность (`@s[type=minecraft:marker]`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/room_reward/spawn_boss.mcfunction)

### `lbc:other/grimoire_dungeon/room_reward/spawn_boss_music`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/room_reward/spawn_boss.mcfunction`; условие вызова: `schedule function lbc:other/grimoire_dungeon/room_reward/spawn_boss_music 2s`. Команда: `execute as @e[type=minecraft:item_display,tag=grimoire_boss_visual] at @s run stopsound @a[distance=..60] * minecraft:lbcsounds.penis_bossfight`; `execute as @e[type=minecraft:item_display,tag=grimoire_boss_visual] at @s run playsound minecraft:lbcsounds.penis_bossfight master @a[distance=..60] ~ ~ ~ 0.5 1`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/room_reward/spawn_boss_music.mcfunction)

### `lbc:other/grimoire_dungeon/room_reward/spawn_boss_pre`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/raycastselector_grimoire_dungeon.mcfunction`; условие вызова: `execute if entity @s[tag=spawn_monument] run return run function lbc:other/grimoire_dungeon/room_reward/spawn_boss_pre`. Изменяет блоки: setblock ~1 ~ ~ minecraft:barrier. Передаёт шаг в `lbc:other/grimoire_dungeon/room_reward/spawn_boss`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/room_reward/spawn_boss_pre.mcfunction)

### `lbc:other/grimoire_dungeon/room_reward/spawn_miniboss`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/raycastselector_grimoire_dungeon.mcfunction`; условие вызова: `execute if entity @s[tag=miniboss] if entity @a[gamemode=!spectator,distance=..4] run return run function lbc:other/grimoire_dungeon/room_reward/spawn_miniboss`. Передаёт шаг в `lbc:other/grimoire_dungeon/room_reward/spawn_miniboss_golden_skeleton`, `lbc:other/grimoire_dungeon/room_reward/spawn_miniboss_tiny_skeleton`. Завершение: удаляет сущность (`@s`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/room_reward/spawn_miniboss.mcfunction)

### `lbc:other/grimoire_dungeon/room_reward/spawn_miniboss_golden_skeleton`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/room_reward/spawn_miniboss.mcfunction`; условие вызова: `execute if score tmp lbc.math matches 1 run function lbc:other/grimoire_dungeon/room_reward/spawn_miniboss_golden_skeleton`. Создаёт `skeleton` с {Tags:["lucky_mob","golden_skeleton","dungeon_mob"],CustomName:{"color":"gold","italic":false,"translate":"golden_skeleton"},DeathLootTable:"lbc:golden_skeleton",PersistenceRequire. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/room_reward/spawn_miniboss_golden_skeleton.mcfunction)

### `lbc:other/grimoire_dungeon/room_reward/spawn_miniboss_tiny_skeleton`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/room_reward/spawn_miniboss.mcfunction`; условие вызова: `execute if score tmp lbc.math matches 2 run function lbc:other/grimoire_dungeon/room_reward/spawn_miniboss_tiny_skeleton`. Создаёт `skeleton` с {Tags:["lucky_mob","tiny_skeleton","dungeon_mob"],CustomName:{"color":"gold","italic":false,"translate":"tiny_skeleton"},DeathLootTable:"lbc:tiny_skeleton",PersistenceRequired:1b,H. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/room_reward/spawn_miniboss_tiny_skeleton.mcfunction)

## Файлы: other / grimoire_dungeon / room_rewards

### `lbc:other/grimoire_dungeon/room_rewards/breezes`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/room_reward_random.mcfunction`; условие вызова: `execute if score tmp lbc.math matches 9 run function lbc:other/grimoire_dungeon/room_rewards/breezes`. Создаёт `breeze` с {Tags:["dungeon_mob"],PersistenceRequired:1b}. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/room_rewards/breezes.mcfunction)

### `lbc:other/grimoire_dungeon/room_rewards/creepers`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/room_reward_random.mcfunction`; условие вызова: `execute if score tmp lbc.math matches 8 run function lbc:other/grimoire_dungeon/room_rewards/creepers`. Создаёт `creeper` с {powered:1b,Tags:["dungeon_mob"],PersistenceRequired:1b}. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/room_rewards/creepers.mcfunction)

### `lbc:other/grimoire_dungeon/room_rewards/grimoire_chest`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/room_reward_random.mcfunction`; условие вызова: `execute if score tmp lbc.math matches 3 run function lbc:other/grimoire_dungeon/room_rewards/grimoire_chest`. Изменяет блоки: setblock ~ ~ ~ minecraft:chest{LootTable:"lbc:random_grimoires"}; setblock ~ ~-1 ~ minecraft:diamond_block. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/room_rewards/grimoire_chest.mcfunction)

### `lbc:other/grimoire_dungeon/room_rewards/luckyblock_mobs`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/room_reward_random.mcfunction`; условие вызова: `execute if score tmp lbc.math matches 10 run function lbc:other/grimoire_dungeon/room_rewards/luckyblock_mobs`. Создаёт `drowned` с {Tags:["dungeon_mob"],PersistenceRequired:1b,CustomNameVisible:0b,CustomName:{"color":"yellow","italic":false,"text":"Lucky Block"},IsBaby:1b,Silent:1b,DeathLootTable:"lbc:luckyblo; `husk` с {Tags:["dungeon_mob"],PersistenceRequired:1b,CustomNameVisible:0b,CustomName:{"color":"yellow","italic":false,"text":"Lucky Block"},IsBaby:1b,Silent:1b,DeathLootTable:"lbc:luckyblo; `stray` с {Tags:["dungeon_mob"],PersistenceRequired:1b,CustomNameVisible:0b,CustomName:{"color":"yellow","italic":false,"text":"Lucky Block"},Silent:1b,DeathLootTable:"lbc:luckyblock",equipm. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/room_rewards/luckyblock_mobs.mcfunction)

### `lbc:other/grimoire_dungeon/room_rewards/luckyblocks`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/room_reward_random.mcfunction`; условие вызова: `execute if score tmp lbc.math matches 11 run function lbc:other/grimoire_dungeon/room_rewards/luckyblocks`. Изменяет блоки: setblock ~ ~ ~ minecraft:red_mushroom_block[down=false,east=false,north=false,south=false,up=false,west=false]; setblock ~ ~1 ~ minecraft:brown_mushroom_block[down=false,east=false,north=false,south=false,up=false,west=false]; setblock ~ ~2 ~ minecraft:mushroom_stem[down=false,east=false,north=false,south=false,up=false,west=false]. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/room_rewards/luckyblocks.mcfunction)

### `lbc:other/grimoire_dungeon/room_rewards/mine`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/room_reward_random.mcfunction`; условие вызова: `execute if score tmp lbc.math matches 12 run function lbc:other/grimoire_dungeon/room_rewards/mine`. Изменяет блоки: setblock ~ ~ ~ minecraft:spawner{SpawnCount:5,SpawnRange:5,Delay:0,MinSpawnDelay:200,MaxSpawnDelay:200,MaxNearbyEntities:100,Requ. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/room_rewards/mine.mcfunction)

### `lbc:other/grimoire_dungeon/room_rewards/pillagers`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/room_reward_random.mcfunction`; условие вызова: `execute if score tmp lbc.math matches 7 run function lbc:other/grimoire_dungeon/room_rewards/pillagers`. Создаёт `evoker` с {Tags:["dungeon_mob"],PersistenceRequired:1b}; `vindicator` с {Tags:["dungeon_mob"],PersistenceRequired:1b}. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/room_rewards/pillagers.mcfunction)

### `lbc:other/grimoire_dungeon/room_rewards/random_chest`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/room_reward_random.mcfunction`; условие вызова: `execute if score tmp lbc.math matches 2 run function lbc:other/grimoire_dungeon/room_rewards/random_chest`. Изменяет блоки: setblock ~ ~ ~ minecraft:barrel{LootTable:"lbc:random_glack_1"}; setblock ~ ~-1 ~ minecraft:gold_block. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/room_rewards/random_chest.mcfunction)

### `lbc:other/grimoire_dungeon/room_rewards/random_mob_miniboss`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/room_reward_random.mcfunction`; условие вызова: `execute if score tmp lbc.math matches 1 run function lbc:other/grimoire_dungeon/room_rewards/random_mob_miniboss`. Создаёт `marker` с {Tags:["slowraycast","grimoire_dungeon","miniboss"]}. Изменяет блоки: setblock ~ ~-1 ~ minecraft:red_stained_glass. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/room_rewards/random_mob_miniboss.mcfunction)

### `lbc:other/grimoire_dungeon/room_rewards/skeletons`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/room_reward_random.mcfunction`; условие вызова: `execute if score tmp lbc.math matches 5 run function lbc:other/grimoire_dungeon/room_rewards/skeletons`. Создаёт `skeleton` с {Tags:["dungeon_mob"],PersistenceRequired:1b}; `stray` с {Tags:["dungeon_mob"],PersistenceRequired:1b}; `wither_skeleton` с {Tags:["dungeon_mob"],PersistenceRequired:1b}. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/room_rewards/skeletons.mcfunction)

### `lbc:other/grimoire_dungeon/room_rewards/spawner_hehehogs`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/room_reward_random.mcfunction`; условие вызова: `execute if score tmp lbc.math matches 4 run function lbc:other/grimoire_dungeon/room_rewards/spawner_hehehogs`. Изменяет блоки: setblock ~ ~ ~ minecraft:spawner{SpawnCount:1,SpawnRange:3,Delay:1,MinSpawnDelay:4,MaxSpawnDelay:4,MaxNearbyEntities:100,Required. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/room_rewards/spawner_hehehogs.mcfunction)

### `lbc:other/grimoire_dungeon/room_rewards/zombies`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/room_reward_random.mcfunction`; условие вызова: `execute if score tmp lbc.math matches 6 run function lbc:other/grimoire_dungeon/room_rewards/zombies`. Создаёт `zombie` с {Tags:["dungeon_mob"],PersistenceRequired:1b}; `husk` с {Tags:["dungeon_mob"],PersistenceRequired:1b}; `drowned` с {Tags:["dungeon_mob"],PersistenceRequired:1b}. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/room_rewards/zombies.mcfunction)

## Файлы: other / magic_academy

### `lbc:other/magic_academy/can_upgrade`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbcr/function/damagewithid7_check_blinded.mcfunction`; условие вызова: `execute if entity @e[type=#minecraft:mobs,dx=0,dy=0,dz=0,distance=..15,tag=!nodamage,tag=!spectator,nbt=!{active_effects:[{id:"minecraft:blindness"}]}] at @a[gamemode=!spectator,nbt={SelectedItem:{components:{"minecraft:custom_data":{upgradable:1,stblind:1}}}}] if score @a[gamemode=!spectator,distance=..0.001,limit=1] lbcID2 = @s lbcID2 as @a[gamemode=!spectator,distance=..0.001,limit=1] at @s if predicate lbc:chance3 run function lbc:other/magic_academy/can_upgrade`. Передаёт шаг в `lbc:other/magic_academy/can_upgrade_book`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/can_upgrade.mcfunction)

### `lbc:other/magic_academy/can_upgrade2`

Файл пуст: при вызове командами Minecraft эффекта не будет. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/can_upgrade2.mcfunction)

### `lbc:other/magic_academy/can_upgrade_book`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/random_spell_check.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{have1:1,have2:1,have3:1,have4:1,have5:1,have6:1,have7:1,have8:1,have9:1,have10:1,have11:1,have12:1,have13:1,have14:1,have15:1,have16:1,have17:1,have18:1,have19:1,have20:1,have21:1,have22:1,have23:1,have24:1,have25:1,have26:1,have27:1,have28:1,have29:1,have30:1,have31:1,have32:1,have33:1,have34:1,have35:1,have36:1,have37:1,have38:1,have39:1,have40:1,have41:1,have42:1,have43:1,have44:1,have45:1,have46:1,have47:1,have48:1,have49:1,have50:1,have51:1,have52:1,have53:1}}}}] run function lbc:other/magic_academy/can_upgrade_book`. Обновляет NBT/components: modify block 10241024 100 10241024 Items[{Slot:0b}].components."minecraft:custom_data".upgrade_available set value 1; modify block 10241024 100 10241024 Items[{Slot:0b}].components."minecraft:custom_data".upgradable set value 0. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/can_upgrade_book.mcfunction)

### `lbc:other/magic_academy/change_mode_2`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbcr/function/raycast_loop4cd_upgraded.mcfunction`; условие вызова: `execute if predicate lbc:sneak run function lbc:other/magic_academy/change_mode_2`. Состояние scoreboard: scoreboard players add #mode lbc.math 1; scoreboard players set #mode lbc.math 1. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/change_mode_2.mcfunction)

### `lbc:other/magic_academy/change_mode_28_down`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/badwiki_upgraded_effect_change.mcfunction`; условие вызова: `execute if entity @s[x_rotation=0..89] run function lbc:other/magic_academy/change_mode_28_down`. Состояние scoreboard: scoreboard players remove #mode lbc.math 1; scoreboard players set #mode lbc.math 28. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/change_mode_28_down.mcfunction)

### `lbc:other/magic_academy/change_mode_28_down_left`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/badwiki_upgraded_effect_change_left.mcfunction`; условие вызова: `execute if entity @s[x_rotation=0..89] run function lbc:other/magic_academy/change_mode_28_down_left`. Состояние scoreboard: scoreboard players remove #mode lbc.math 1; scoreboard players set #mode lbc.math 28. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/change_mode_28_down_left.mcfunction)

### `lbc:other/magic_academy/change_mode_28_up`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/badwiki_upgraded_effect_change.mcfunction`; условие вызова: `execute if entity @s[x_rotation=-89..0] run function lbc:other/magic_academy/change_mode_28_up`. Состояние scoreboard: scoreboard players add #mode lbc.math 1; scoreboard players set #mode lbc.math 1. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/change_mode_28_up.mcfunction)

### `lbc:other/magic_academy/change_mode_28_up_left`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/badwiki_upgraded_effect_change_left.mcfunction`; условие вызова: `execute if entity @s[x_rotation=-89..0] run function lbc:other/magic_academy/change_mode_28_up_left`. Состояние scoreboard: scoreboard players add #mode lbc.math 1; scoreboard players set #mode lbc.math 1. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/change_mode_28_up_left.mcfunction)

### `lbc:other/magic_academy/change_mode_2_left`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbcr/function/raycast_loop4cd_upgraded_left.mcfunction`; условие вызова: `execute if predicate lbc:sneak run function lbc:other/magic_academy/change_mode_2_left`. Состояние scoreboard: scoreboard players add #mode lbc.math 1; scoreboard players set #mode lbc.math 1. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/change_mode_2_left.mcfunction)

### `lbc:other/magic_academy/change_mode_3`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbcr/function/raycast_lightningpre_upgraded.mcfunction`; условие вызова: `execute if predicate lbc:sneak run function lbc:other/magic_academy/change_mode_3`. Состояние scoreboard: scoreboard players add #mode lbc.math 1; scoreboard players set #mode lbc.math 1. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/change_mode_3.mcfunction)

### `lbc:other/magic_academy/change_mode_3_left`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbcr/function/raycast_lightningpre_upgraded_left.mcfunction`; условие вызова: `execute if predicate lbc:sneak run function lbc:other/magic_academy/change_mode_3_left`. Состояние scoreboard: scoreboard players add #mode lbc.math 1; scoreboard players set #mode lbc.math 1. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/change_mode_3_left.mcfunction)

### `lbc:other/magic_academy/change_mode_4`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbcr/function/raycast_loop2cd_upgraded.mcfunction`; условие вызова: `execute if predicate lbc:sneak if entity @s[x_rotation=-90..0] run function lbc:other/magic_academy/change_mode_4`. Состояние scoreboard: scoreboard players add #mode lbc.math 1; scoreboard players set #mode lbc.math 1. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/change_mode_4.mcfunction)

### `lbc:other/magic_academy/change_mode_4_down`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbcr/function/raycast_loop2cd_upgraded.mcfunction`; условие вызова: `execute if predicate lbc:sneak if entity @s[x_rotation=0..90] run function lbc:other/magic_academy/change_mode_4_down`. Состояние scoreboard: scoreboard players remove #mode lbc.math 1; scoreboard players set #mode lbc.math 4. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/change_mode_4_down.mcfunction)

### `lbc:other/magic_academy/change_mode_4_down_left`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbcr/function/raycast_loop2cd_upgraded_left.mcfunction`; условие вызова: `execute if predicate lbc:sneak if entity @s[x_rotation=0..90] run function lbc:other/magic_academy/change_mode_4_down_left`. Состояние scoreboard: scoreboard players remove #mode lbc.math 1; scoreboard players set #mode lbc.math 4. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/change_mode_4_down_left.mcfunction)

### `lbc:other/magic_academy/change_mode_4_left`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbcr/function/raycast_loop2cd_upgraded_left.mcfunction`; условие вызова: `execute if predicate lbc:sneak if entity @s[x_rotation=-90..0] run function lbc:other/magic_academy/change_mode_4_left`. Состояние scoreboard: scoreboard players add #mode lbc.math 1; scoreboard players set #mode lbc.math 1. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/change_mode_4_left.mcfunction)

### `lbc:other/magic_academy/change_mode_4_magnum`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/cycle.mcfunction`; условие вызова: `function lbc:other/magic_academy/change_mode_4_magnum`. Обновляет NBT/components: modify block 10241024 100 10241024 Items[{Slot:0b}].components."minecraft:custom_name" set value {"italic":false,"translate":"m. Состояние scoreboard: scoreboard players add #mode lbc.math 1; scoreboard players set #mode lbc.math 0. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/change_mode_4_magnum.mcfunction)

### `lbc:other/magic_academy/change_mode_5`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbcr/function/raycast_loop9cd_upgraded.mcfunction`; условие вызова: `execute if predicate lbc:sneak run function lbc:other/magic_academy/change_mode_5`. Состояние scoreboard: scoreboard players add #mode lbc.math 1; scoreboard players set #mode lbc.math 1. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/change_mode_5.mcfunction)

### `lbc:other/magic_academy/change_mode_50_down`

Автоматический caller среди исследованных функций не найден; при ручном запуске выполняются действия ниже. Состояние scoreboard: scoreboard players remove #mode lbc.math 1; scoreboard players set #mode lbc.math 51. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/change_mode_50_down.mcfunction)

### `lbc:other/magic_academy/change_mode_50_up`

Автоматический caller среди исследованных функций не найден; при ручном запуске выполняются действия ниже. Состояние scoreboard: scoreboard players add #mode lbc.math 1; scoreboard players set #mode lbc.math 1. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/change_mode_50_up.mcfunction)

### `lbc:other/magic_academy/change_mode_53_down`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/random_spell_upgraded_change.mcfunction`; условие вызова: `execute if entity @s[x_rotation=0..90] run function lbc:other/magic_academy/change_mode_53_down`. Состояние scoreboard: scoreboard players remove #mode lbc.math 1; scoreboard players set #mode lbc.math 54. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/change_mode_53_down.mcfunction)

### `lbc:other/magic_academy/change_mode_53_down_left`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/random_spell_upgraded_change_left.mcfunction`; условие вызова: `execute if entity @s[x_rotation=0..90] run function lbc:other/magic_academy/change_mode_53_down_left`. Состояние scoreboard: scoreboard players remove #mode lbc.math 1; scoreboard players set #mode lbc.math 54. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/change_mode_53_down_left.mcfunction)

### `lbc:other/magic_academy/change_mode_53_up`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/random_spell_upgraded_change.mcfunction`; условие вызова: `execute if entity @s[x_rotation=-90..0] run function lbc:other/magic_academy/change_mode_53_up`. Состояние scoreboard: scoreboard players add #mode lbc.math 1; scoreboard players set #mode lbc.math 1. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/change_mode_53_up.mcfunction)

### `lbc:other/magic_academy/change_mode_53_up_left`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/random_spell_upgraded_change_left.mcfunction`; условие вызова: `execute if entity @s[x_rotation=-90..0] run function lbc:other/magic_academy/change_mode_53_up_left`. Состояние scoreboard: scoreboard players add #mode lbc.math 1; scoreboard players set #mode lbc.math 1. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/change_mode_53_up_left.mcfunction)

### `lbc:other/magic_academy/change_mode_5_left`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbcr/function/raycast_loop9cd_upgraded_left.mcfunction`; условие вызова: `execute if predicate lbc:sneak run function lbc:other/magic_academy/change_mode_5_left`. Состояние scoreboard: scoreboard players add #mode lbc.math 1; scoreboard players set #mode lbc.math 1. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/change_mode_5_left.mcfunction)

### `lbc:other/magic_academy/change_mode_8`

Автоматический caller среди исследованных функций не найден; при ручном запуске выполняются действия ниже. Состояние scoreboard: scoreboard players add #mode lbc.math 1; scoreboard players set #mode lbc.math 1. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/change_mode_8.mcfunction)

### `lbc:other/magic_academy/change_mode_8_gaycaster`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/gaycaster/ray/cast_upgraded_change_mode.mcfunction`; условие вызова: `function lbc:other/magic_academy/change_mode_8_gaycaster`. Состояние scoreboard: scoreboard players add #mode lbc.math 1; scoreboard players set #mode lbc.math 1. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/change_mode_8_gaycaster.mcfunction)

### `lbc:other/magic_academy/change_mode_8_gaycaster_color`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/gaycaster/ray/cast_upgraded_pre_2.mcfunction`; условие вызова: `execute unless entity @s[tag=left] if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{gaycaster:1,upgraded:1,mode:8}}}}] run function lbc:other/magic_academy/change_mode_8_gaycaster_color`. Состояние scoreboard: scoreboard players add #color lbc.math 1; scoreboard players set #color lbc.math 1. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/change_mode_8_gaycaster_color.mcfunction)

### `lbc:other/magic_academy/change_mode_8_gaycaster_color_left`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/gaycaster/ray/cast_upgraded_pre_2.mcfunction`; условие вызова: `execute if entity @s[tag=left] if entity @s[nbt={equipment:{offhand:{components:{"minecraft:custom_data":{gaycaster:1,upgraded:1,mode:8}}}}}] run function lbc:other/magic_academy/change_mode_8_gaycaster_color_left`. Состояние scoreboard: scoreboard players add #color lbc.math 1; scoreboard players set #color lbc.math 1. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/change_mode_8_gaycaster_color_left.mcfunction)

### `lbc:other/magic_academy/change_mode_8_gaycaster_left`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/gaycaster/ray/cast_upgraded_change_mode_left.mcfunction`; условие вызова: `function lbc:other/magic_academy/change_mode_8_gaycaster_left`. Состояние scoreboard: scoreboard players add #mode lbc.math 1; scoreboard players set #mode lbc.math 1. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/change_mode_8_gaycaster_left.mcfunction)

### `lbc:other/magic_academy/change_tier_2`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/awaken_tuntija_wands/acidacid/click_upgraded.mcfunction`; условие вызова: `execute if predicate lbc:sneak if entity @s[x_rotation=89..90] run function lbc:other/magic_academy/change_tier_2`. Состояние scoreboard: scoreboard players set #tier lbc.math 0; scoreboard players add #tier lbc.math 1; scoreboard players set #tier lbc.math 1. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/change_tier_2.mcfunction)

### `lbc:other/magic_academy/change_tier_2_left`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/awaken_tuntija_wands/acidacid/click_upgraded_left.mcfunction`; условие вызова: `execute if predicate lbc:sneak if entity @s[x_rotation=89..90] run function lbc:other/magic_academy/change_tier_2_left`. Состояние scoreboard: scoreboard players set #tier lbc.math 0; scoreboard players add #tier lbc.math 1; scoreboard players set #tier lbc.math 1. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/change_tier_2_left.mcfunction)

### `lbc:other/magic_academy/change_tier_3_down`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/badwiki_upgraded_effect_change.mcfunction`; условие вызова: `execute if entity @s[x_rotation=89..90] run function lbc:other/magic_academy/change_tier_3_down`. Состояние scoreboard: scoreboard players set #tier lbc.math 0; scoreboard players remove #tier lbc.math 1; scoreboard players set #tier lbc.math 3. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/change_tier_3_down.mcfunction)

### `lbc:other/magic_academy/change_tier_3_down_left`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/badwiki_upgraded_effect_change_left.mcfunction`; условие вызова: `execute if entity @s[x_rotation=89..90] run function lbc:other/magic_academy/change_tier_3_down_left`. Состояние scoreboard: scoreboard players set #tier lbc.math 0; scoreboard players remove #tier lbc.math 1; scoreboard players set #tier lbc.math 3. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/change_tier_3_down_left.mcfunction)

### `lbc:other/magic_academy/change_tier_3_up`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/badwiki_upgraded_effect_change.mcfunction`; условие вызова: `execute if entity @s[x_rotation=-90..-89] run function lbc:other/magic_academy/change_tier_3_up`. Состояние scoreboard: scoreboard players set #tier lbc.math 0; scoreboard players add #tier lbc.math 1; scoreboard players set #tier lbc.math 1. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/change_tier_3_up.mcfunction)

### `lbc:other/magic_academy/change_tier_3_up_left`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/workingitems/badwiki_upgraded_effect_change_left.mcfunction`; условие вызова: `execute if entity @s[x_rotation=-90..-89] run function lbc:other/magic_academy/change_tier_3_up_left`. Состояние scoreboard: scoreboard players set #tier lbc.math 0; scoreboard players add #tier lbc.math 1; scoreboard players set #tier lbc.math 1. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/change_tier_3_up_left.mcfunction)

### `lbc:other/magic_academy/commands`

Автоматический caller среди исследованных функций не найден; при ручном запуске выполняются действия ниже. Создаёт `text_display` с {shadow_radius:0.0f,shadow_strength:0.0f,glow_color_override:0b,billboard:"fixed",line_width:150,text_opacity:255,default_background:0b,shadow:0b,see_through:0b,alignment:"center",; `interaction` с {width:10.0f,height:6.0f,response:1b,Tags:["magic_academy"]}. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/commands.mcfunction)

### `lbc:other/magic_academy/completed_challenge`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_upgrade.mcfunction`; условие вызова: `execute if score @s[scores={lbc.challenge=1}] challenge_score matches 20.. run function lbc:other/magic_academy/completed_challenge`. Состояние scoreboard: scoreboard players set @s challenge_score 0; scoreboard players set @s lbc.challenge 0. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/completed_challenge.mcfunction)

### `lbc:other/magic_academy/interacted`

Автоматический caller среди исследованных функций не найден; при ручном запуске выполняются действия ниже. Передаёт шаг в `lbc:other/magic_academy/interacted_cancel_update`, `lbc:other/magic_academy/interacted_upgrade`, `lbc:other/magic_academy/interacted_2`. Завершение: сбрасывает advancement для повторного запуска. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted.mcfunction)

### `lbc:other/magic_academy/interacted_2`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted.mcfunction`; условие вызова: `execute unless entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{upgradable:1}}}}] unless entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{upgraded:1}}}}] unless entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{upgrade_available:1}}}}] run function lbc:other/magic_academy/interacted_2`. Обновляет NBT/components: modify block 10241024 100 10241024 Items[{Slot:0b}].components."minecraft:custom_data".upgradable set value 1. Передаёт шаг в `lbc:other/magic_academy/can_upgrade/evocator`, `lbc:other/magic_academy/can_upgrade/bassboost`, `lbc:other/magic_academy/can_upgrade/enderwand`, `lbc:other/magic_academy/can_upgrade/corrupter`, `lbc:other/magic_academy/can_upgrade/lifestealer`, `lbc:other/magic_academy/can_upgrade/foxusing`, `lbc:other/magic_academy/can_upgrade/bloodmagic`, `lbc:other/magic_academy/can_upgrade/badwiki`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_2.mcfunction)

### `lbc:other/magic_academy/interacted_cancel_update`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{pissed_by_madoka_sob_sob_sob_sob_sob:1}}}}] run return run function lbc:other/magic_academy/interacted_cancel_update`. Завершение: сбрасывает advancement для повторного запуска. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_cancel_update.mcfunction)

### `lbc:other/magic_academy/interacted_upgrade`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{upgrade_available:1}}}}] unless entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{upgraded:1}}}}] run function lbc:other/magic_academy/interacted_upgrade`. Обновляет NBT/components: modify block 10241024 100 10241024 Items[{Slot:0b}].components."minecraft:custom_data".upgraded set value 1; modify block 10241024 100 10241024 Items[{Slot:0b}].components."minecraft:custom_data".mode set value 1. Состояние scoreboard: scoreboard players add @s[scores={lbc.challenge=1}] challenge_score 1. Передаёт шаг в `lbc:other/magic_academy/completed_challenge`, `lbc:other/magic_academy/upgrade/evocator`, `lbc:other/magic_academy/upgrade/bassboost`, `lbc:other/magic_academy/upgrade/enderwand`, `lbc:other/magic_academy/upgrade/corrupter`, `lbc:other/magic_academy/upgrade/lifestealer`, `lbc:other/magic_academy/upgrade/foxusing`, `lbc:other/magic_academy/upgrade/bloodmagic`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_upgrade.mcfunction)

## Файлы: other / magic_academy / can_upgrade

### `lbc:other/magic_academy/can_upgrade/acidacid_wand`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_2.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{acidacid_wand:1}}}}] run function lbc:other/magic_academy/can_upgrade/acidacid_wand`. Команда: `tellraw @s {"translate":"upgrade_condition_acidacid","color":"yellow"}`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/can_upgrade/acidacid_wand.mcfunction)

### `lbc:other/magic_academy/can_upgrade/acidcorrupt_wand`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_2.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{acidcorrupt_wand:1}}}}] run function lbc:other/magic_academy/can_upgrade/acidcorrupt_wand`. Команда: `tellraw @s {"translate":"upgrade_condition_acidcorrupt","color":"yellow"}`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/can_upgrade/acidcorrupt_wand.mcfunction)

### `lbc:other/magic_academy/can_upgrade/acidwand`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_2.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{acidwand:1}}}}] run function lbc:other/magic_academy/can_upgrade/acidwand`. Команда: `tellraw @s {"translate":"upgrade_condition_acid","color":"yellow"}`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/can_upgrade/acidwand.mcfunction)

### `lbc:other/magic_academy/can_upgrade/advancedteleport`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_2.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{advancedteleport:1}}}}] run function lbc:other/magic_academy/can_upgrade/advancedteleport`. Команда: `tellraw @s {"translate":"upgrade_condition_advancedteleport","color":"yellow"}`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/can_upgrade/advancedteleport.mcfunction)

### `lbc:other/magic_academy/can_upgrade/badwiki`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_2.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{badwiki:1}}}}] run function lbc:other/magic_academy/can_upgrade/badwiki`. Команда: `tellraw @s {"translate":"upgrade_condition_badwiki","color":"yellow"}`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/can_upgrade/badwiki.mcfunction)

### `lbc:other/magic_academy/can_upgrade/baffledhedgehog`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_2.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{baffledhedgehog:1}}}}] run function lbc:other/magic_academy/can_upgrade/baffledhedgehog`. Команда: `tellraw @s {"translate":"upgrade_condition_baffledhedgehog","color":"yellow"}`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/can_upgrade/baffledhedgehog.mcfunction)

### `lbc:other/magic_academy/can_upgrade/bassboost`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_2.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{bassboost:1}}}}] run function lbc:other/magic_academy/can_upgrade/bassboost`. Команда: `tellraw @s {"translate":"upgrade_condition_bassboost","color":"yellow"}`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/can_upgrade/bassboost.mcfunction)

### `lbc:other/magic_academy/can_upgrade/bedrock_wand`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_2.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{bedrock_wand:1}}}}] run function lbc:other/magic_academy/can_upgrade/bedrock_wand`. Команда: `tellraw @s {"translate":"upgrade_condition_bedrock","color":"yellow"}`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/can_upgrade/bedrock_wand.mcfunction)

### `lbc:other/magic_academy/can_upgrade/bloodmagic`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_2.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{bloodmagic:1}}}}] run function lbc:other/magic_academy/can_upgrade/bloodmagic`. Команда: `tellraw @s {"translate":"upgrade_condition_bloodmagic","color":"yellow"}`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/can_upgrade/bloodmagic.mcfunction)

### `lbc:other/magic_academy/can_upgrade/boltbolt_wand`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_2.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{boltbolt_wand:1}}}}] run function lbc:other/magic_academy/can_upgrade/boltbolt_wand`. Команда: `tellraw @s {"translate":"upgrade_condition_boltbolt","color":"yellow"}`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/can_upgrade/boltbolt_wand.mcfunction)

### `lbc:other/magic_academy/can_upgrade/consuming_wand`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_2.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{consuming_wand:1}}}}] run function lbc:other/magic_academy/can_upgrade/consuming_wand`. Команда: `tellraw @s {"translate":"upgrade_condition_consuming","color":"yellow"}`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/can_upgrade/consuming_wand.mcfunction)

### `lbc:other/magic_academy/can_upgrade/corrupter`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_2.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{corrupter:1}}}}] run function lbc:other/magic_academy/can_upgrade/corrupter`. Команда: `tellraw @s {"translate":"upgrade_condition_corrupter","color":"yellow"}`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/can_upgrade/corrupter.mcfunction)

### `lbc:other/magic_academy/can_upgrade/danmaku_staff`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_2.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{danmaku_staff:1}}}}] run function lbc:other/magic_academy/can_upgrade/danmaku_staff`. Команда: `tellraw @s {"translate":"upgrade_condition_danmaku_staff","color":"yellow"}`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/can_upgrade/danmaku_staff.mcfunction)

### `lbc:other/magic_academy/can_upgrade/dublicating_wand`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_2.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{dublicating_wand:1}}}}] run function lbc:other/magic_academy/can_upgrade/dublicating_wand`. Команда: `tellraw @s {"translate":"upgrade_condition_dublicating","color":"yellow"}`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/can_upgrade/dublicating_wand.mcfunction)

### `lbc:other/magic_academy/can_upgrade/enderwand`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_2.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{enderwand:1}}}}] run function lbc:other/magic_academy/can_upgrade/enderwand`. Команда: `tellraw @s {"translate":"upgrade_condition_enderwand","color":"yellow"}`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/can_upgrade/enderwand.mcfunction)

### `lbc:other/magic_academy/can_upgrade/erecto_wand`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_2.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{erecto_wand:1}}}}] run function lbc:other/magic_academy/can_upgrade/erecto_wand`. Команда: `tellraw @s {"translate":"upgrade_condition_erecto","color":"yellow"}`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/can_upgrade/erecto_wand.mcfunction)

### `lbc:other/magic_academy/can_upgrade/evocator`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_2.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{evocator:1}}}}] run function lbc:other/magic_academy/can_upgrade/evocator`. Команда: `tellraw @s {"translate":"upgrade_condition_evocator","color":"yellow"}`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/can_upgrade/evocator.mcfunction)

### `lbc:other/magic_academy/can_upgrade/exahoming_wand`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_2.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{exahoming_wand:1}}}}] run function lbc:other/magic_academy/can_upgrade/exahoming_wand`. Команда: `tellraw @s {"translate":"upgrade_condition_exahoming","color":"yellow"}`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/can_upgrade/exahoming_wand.mcfunction)

### `lbc:other/magic_academy/can_upgrade/exasperato_wand`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_2.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{exasperato_wand:1}}}}] run function lbc:other/magic_academy/can_upgrade/exasperato_wand`. Команда: `tellraw @s {"translate":"upgrade_condition_exasperato","color":"yellow"}`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/can_upgrade/exasperato_wand.mcfunction)

### `lbc:other/magic_academy/can_upgrade/exoperso`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_2.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{exoperso:1}}}}] run function lbc:other/magic_academy/can_upgrade/exoperso`. Команда: `tellraw @s {"translate":"upgrade_condition_exoperso","color":"yellow"}`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/can_upgrade/exoperso.mcfunction)

### `lbc:other/magic_academy/can_upgrade/fallingstars`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_2.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{fallingstars:1}}}}] run function lbc:other/magic_academy/can_upgrade/fallingstars`. Команда: `tellraw @s {"translate":"upgrade_condition_fallingstars","color":"yellow"}`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/can_upgrade/fallingstars.mcfunction)

### `lbc:other/magic_academy/can_upgrade/fartus_nadristailo`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_2.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{fartus_nadristailo:1}}}}] run function lbc:other/magic_academy/can_upgrade/fartus_nadristailo`. Команда: `tellraw @s {"translate":"upgrade_condition_fartus_nadristailo","color":"yellow"}`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/can_upgrade/fartus_nadristailo.mcfunction)

### `lbc:other/magic_academy/can_upgrade/fireblaze_wand`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_2.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{fireblaze_wand:1}}}}] run function lbc:other/magic_academy/can_upgrade/fireblaze_wand`. Команда: `tellraw @s {"translate":"upgrade_condition_fireblaze","color":"yellow"}`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/can_upgrade/fireblaze_wand.mcfunction)

### `lbc:other/magic_academy/can_upgrade/foxusing`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_2.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{foxusing:1}}}}] run function lbc:other/magic_academy/can_upgrade/foxusing`. Команда: `tellraw @s {"translate":"upgrade_condition_foxusing","color":"yellow"}`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/can_upgrade/foxusing.mcfunction)

### `lbc:other/magic_academy/can_upgrade/gaycaster`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_2.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{gaycaster:1}}}}] run function lbc:other/magic_academy/can_upgrade/gaycaster`. Команда: `tellraw @s {"translate":"upgrade_condition_gaycaster","color":"yellow"}`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/can_upgrade/gaycaster.mcfunction)

### `lbc:other/magic_academy/can_upgrade/ice_wand`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_2.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{ice_wand:1}}}}] run function lbc:other/magic_academy/can_upgrade/ice_wand`. Команда: `tellraw @s {"translate":"upgrade_condition_ice","color":"yellow"}`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/can_upgrade/ice_wand.mcfunction)

### `lbc:other/magic_academy/can_upgrade/infbolt_wand`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_2.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{infbolt_wand:1}}}}] run function lbc:other/magic_academy/can_upgrade/infbolt_wand`. Команда: `tellraw @s {"translate":"upgrade_condition_infbolt","color":"yellow"}`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/can_upgrade/infbolt_wand.mcfunction)

### `lbc:other/magic_academy/can_upgrade/lifestealer`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_2.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{lifestealer:1}}}}] run function lbc:other/magic_academy/can_upgrade/lifestealer`. Команда: `tellraw @s {"translate":"upgrade_condition_lifestealer","color":"yellow"}`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/can_upgrade/lifestealer.mcfunction)

### `lbc:other/magic_academy/can_upgrade/lightning_bolts`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_2.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{lightning_bolts:1}}}}] run function lbc:other/magic_academy/can_upgrade/lightning_bolts`. Команда: `tellraw @s {"translate":"upgrade_condition_lightning_bolt","color":"yellow"}`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/can_upgrade/lightning_bolts.mcfunction)

### `lbc:other/magic_academy/can_upgrade/lightningwand`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_2.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{lightningwand:1}}}}] run function lbc:other/magic_academy/can_upgrade/lightningwand`. Команда: `tellraw @s {"translate":"upgrade_condition_lightningwand","color":"yellow"}`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/can_upgrade/lightningwand.mcfunction)

### `lbc:other/magic_academy/can_upgrade/neutralmage`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_2.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{neutralmage:1}}}}] run function lbc:other/magic_academy/can_upgrade/neutralmage`. Команда: `tellraw @s {"translate":"upgrade_condition_neutralmage","color":"yellow"}`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/can_upgrade/neutralmage.mcfunction)

### `lbc:other/magic_academy/can_upgrade/omegaflag`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_2.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{omegaflag:1}}}}] run function lbc:other/magic_academy/can_upgrade/omegaflag`. Команда: `tellraw @s {"translate":"upgrade_condition_omegaflag","color":"yellow"}`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/can_upgrade/omegaflag.mcfunction)

### `lbc:other/magic_academy/can_upgrade/prisma`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_2.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{prisma:1}}}}] run function lbc:other/magic_academy/can_upgrade/prisma`. Команда: `tellraw @s {"translate":"upgrade_condition_prisma","color":"yellow"}`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/can_upgrade/prisma.mcfunction)

### `lbc:other/magic_academy/can_upgrade/psychoblind_wand`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_2.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{psychoblind_wand:1}}}}] run function lbc:other/magic_academy/can_upgrade/psychoblind_wand`. Команда: `tellraw @s {"translate":"upgrade_condition_psychoblind","color":"yellow"}`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/can_upgrade/psychoblind_wand.mcfunction)

### `lbc:other/magic_academy/can_upgrade/psychowand`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_2.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{psychowand:1}}}}] run function lbc:other/magic_academy/can_upgrade/psychowand`. Команда: `tellraw @s {"translate":"upgrade_condition_psychowand","color":"yellow"}`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/can_upgrade/psychowand.mcfunction)

### `lbc:other/magic_academy/can_upgrade/sauvojen_wand`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_2.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{sauvojen_wand:1}}}}] run function lbc:other/magic_academy/can_upgrade/sauvojen_wand`. Команда: `tellraw @s {"translate":"upgrade_condition_sauvojen_wand","color":"yellow"}`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/can_upgrade/sauvojen_wand.mcfunction)

### `lbc:other/magic_academy/can_upgrade/sheepwand`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_2.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{sheepwand:1}}}}] run function lbc:other/magic_academy/can_upgrade/sheepwand`. Команда: `tellraw @s {"translate":"upgrade_condition_sheepwand","color":"yellow"}`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/can_upgrade/sheepwand.mcfunction)

### `lbc:other/magic_academy/can_upgrade/snow_wand`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_2.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{snow_wand:1}}}}] run function lbc:other/magic_academy/can_upgrade/snow_wand`. Команда: `tellraw @s {"translate":"upgrade_condition_snow","color":"yellow"}`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/can_upgrade/snow_wand.mcfunction)

### `lbc:other/magic_academy/can_upgrade/soulcorrupter`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_2.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{soulcorrupter:1}}}}] run function lbc:other/magic_academy/can_upgrade/soulcorrupter`. Команда: `tellraw @s {"translate":"upgrade_condition_soulcorrupter","color":"yellow"}`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/can_upgrade/soulcorrupter.mcfunction)

### `lbc:other/magic_academy/can_upgrade/stacid`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_2.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{stacid:1}}}}] run function lbc:other/magic_academy/can_upgrade/stacid`. Команда: `tellraw @s {"translate":"upgrade_condition_stacid","color":"yellow"}`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/can_upgrade/stacid.mcfunction)

### `lbc:other/magic_academy/can_upgrade/stair`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_2.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{stair:1}}}}] run function lbc:other/magic_academy/can_upgrade/stair`. Команда: `tellraw @s {"translate":"upgrade_condition_stair","color":"yellow"}`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/can_upgrade/stair.mcfunction)

### `lbc:other/magic_academy/can_upgrade/stblind`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_2.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{stblind:1}}}}] run function lbc:other/magic_academy/can_upgrade/stblind`. Команда: `tellraw @s {"translate":"upgrade_condition_stblind","color":"yellow"}`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/can_upgrade/stblind.mcfunction)

### `lbc:other/magic_academy/can_upgrade/stelectro`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_2.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{stelectro:1}}}}] run function lbc:other/magic_academy/can_upgrade/stelectro`. Команда: `tellraw @s {"translate":"upgrade_condition_stelectro","color":"yellow"}`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/can_upgrade/stelectro.mcfunction)

### `lbc:other/magic_academy/can_upgrade/stfire`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_2.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{stfire:1}}}}] run function lbc:other/magic_academy/can_upgrade/stfire`. Команда: `tellraw @s {"translate":"upgrade_condition_stfire","color":"yellow"}`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/can_upgrade/stfire.mcfunction)

### `lbc:other/magic_academy/can_upgrade/stonekivi_wand`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_2.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{stonekivi_wand:1}}}}] run function lbc:other/magic_academy/can_upgrade/stonekivi_wand`. Команда: `tellraw @s {"translate":"upgrade_condition_stonekivi","color":"yellow"}`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/can_upgrade/stonekivi_wand.mcfunction)

### `lbc:other/magic_academy/can_upgrade/struby`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_2.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{struby:1}}}}] run function lbc:other/magic_academy/can_upgrade/struby`. Команда: `tellraw @s {"translate":"upgrade_condition_struby","color":"yellow"}`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/can_upgrade/struby.mcfunction)

### `lbc:other/magic_academy/can_upgrade/ststone`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_2.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{ststone:1}}}}] run function lbc:other/magic_academy/can_upgrade/ststone`. Команда: `tellraw @s {"translate":"upgrade_condition_ststone","color":"yellow"}`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/can_upgrade/ststone.mcfunction)

### `lbc:other/magic_academy/can_upgrade/stteleport`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_2.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{stteleport:1}}}}] run function lbc:other/magic_academy/can_upgrade/stteleport`. Команда: `tellraw @s {"translate":"upgrade_condition_stteleport","color":"yellow"}`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/can_upgrade/stteleport.mcfunction)

### `lbc:other/magic_academy/can_upgrade/stterra`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_2.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{stterra:1}}}}] run function lbc:other/magic_academy/can_upgrade/stterra`. Команда: `tellraw @s {"translate":"upgrade_condition_stterra","color":"yellow"}`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/can_upgrade/stterra.mcfunction)

### `lbc:other/magic_academy/can_upgrade/truefallingstars`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_2.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{truefallingstars:1}}}}] run function lbc:other/magic_academy/can_upgrade/truefallingstars`. Команда: `tellraw @s {"translate":"upgrade_condition_truefallingstars","color":"yellow"}`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/can_upgrade/truefallingstars.mcfunction)

### `lbc:other/magic_academy/can_upgrade/unstable_spellbook`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_2.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{unstable_spellbook:1}}}}] run function lbc:other/magic_academy/can_upgrade/unstable_spellbook`. Команда: `tellraw @s {"translate":"upgrade_condition_unstable_spellbook","color":"yellow"}`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/can_upgrade/unstable_spellbook.mcfunction)

### `lbc:other/magic_academy/can_upgrade/vex_wand`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_2.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{vex_wand:1}}}}] run function lbc:other/magic_academy/can_upgrade/vex_wand`. Команда: `tellraw @s {"translate":"upgrade_condition_vex","color":"yellow"}`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/can_upgrade/vex_wand.mcfunction)

### `lbc:other/magic_academy/can_upgrade/wall`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_2.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{wall:1}}}}] run function lbc:other/magic_academy/can_upgrade/wall`. Команда: `tellraw @s {"translate":"upgrade_condition_wall","color":"yellow"}`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/can_upgrade/wall.mcfunction)

## Файлы: other / magic_academy / upgrade

### `lbc:other/magic_academy/upgrade/acidacid_wand`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_upgrade.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{acidacid_wand:1}}}}] run function lbc:other/magic_academy/upgrade/acidacid_wand`. Команда: `tellraw @s {"translate":"upgrade_succesful_acidacid","color":"yellow"}`; `item modify entity @s weapon.mainhand lbc:tiers/ss`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/acidacid_wand.mcfunction)

### `lbc:other/magic_academy/upgrade/acidcorrupt_wand`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_upgrade.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{acidcorrupt_wand:1}}}}] run function lbc:other/magic_academy/upgrade/acidcorrupt_wand`. Команда: `tellraw @s {"translate":"upgrade_succesful_acidcorrupt","color":"yellow"}`; `item modify entity @s weapon.mainhand lbc:tiers/ss`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/acidcorrupt_wand.mcfunction)

### `lbc:other/magic_academy/upgrade/acidwand`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_upgrade.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{acidwand:1}}}}] run function lbc:other/magic_academy/upgrade/acidwand`. Команда: `tellraw @s {"translate":"upgrade_succesful_acid","color":"yellow"}`; `item modify entity @s weapon.mainhand lbc:tiers/c`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/acidwand.mcfunction)

### `lbc:other/magic_academy/upgrade/advancedteleport`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_upgrade.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{advancedteleport:1}}}}] run function lbc:other/magic_academy/upgrade/advancedteleport`. Команда: `tellraw @s {"translate":"upgrade_succesful_advancedteleport","color":"yellow"}`; `item modify entity @s weapon.mainhand lbc:tiers/s`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/advancedteleport.mcfunction)

### `lbc:other/magic_academy/upgrade/badwiki`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_upgrade.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{badwiki:1}}}}] run function lbc:other/magic_academy/upgrade/badwiki`. Команда: `tellraw @s {"translate":"upgrade_succesful_badwiki","color":"yellow"}`; `item modify entity @s weapon.mainhand lbc:tiers/a`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/badwiki.mcfunction)

### `lbc:other/magic_academy/upgrade/baffledhedgehog`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_upgrade.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{baffledhedgehog:1}}}}] run function lbc:other/magic_academy/upgrade/baffledhedgehog`. Команда: `tellraw @s {"translate":"upgrade_succesful_baffledhedgehog","color":"yellow"}`; `item modify entity @s weapon.mainhand lbc:tiers/s`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/baffledhedgehog.mcfunction)

### `lbc:other/magic_academy/upgrade/bassboost`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_upgrade.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{bassboost:1}}}}] run function lbc:other/magic_academy/upgrade/bassboost`. Команда: `tellraw @s {"translate":"upgrade_succesful_bassboost","color":"yellow"}`; `item modify entity @s weapon.mainhand lbc:tiers/b`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/bassboost.mcfunction)

### `lbc:other/magic_academy/upgrade/bedrock_wand`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_upgrade.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{bedrock_wand:1}}}}] run function lbc:other/magic_academy/upgrade/bedrock_wand`. Команда: `tellraw @s {"translate":"upgrade_succesful_bedrock","color":"yellow"}`; `item modify entity @s weapon.mainhand lbc:tiers/ss`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/bedrock_wand.mcfunction)

### `lbc:other/magic_academy/upgrade/bloodmagic`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_upgrade.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{bloodmagic:1}}}}] run function lbc:other/magic_academy/upgrade/bloodmagic`. Команда: `tellraw @s {"translate":"upgrade_succesful_bloodmagic","color":"yellow"}`; `item modify entity @s weapon.mainhand lbc:tiers/a`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/bloodmagic.mcfunction)

### `lbc:other/magic_academy/upgrade/boltbolt_wand`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_upgrade.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{boltbolt_wand:1}}}}] run function lbc:other/magic_academy/upgrade/boltbolt_wand`. Команда: `tellraw @s {"translate":"upgrade_succesful_boltbolt","color":"yellow"}`; `item modify entity @s weapon.mainhand lbc:tiers/ss`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/boltbolt_wand.mcfunction)

### `lbc:other/magic_academy/upgrade/consuming_wand`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_upgrade.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{consuming_wand:1}}}}] run function lbc:other/magic_academy/upgrade/consuming_wand`. Команда: `tellraw @s {"translate":"upgrade_succesful_consuming","color":"yellow"}`; `item modify entity @s weapon.mainhand lbc:tiers/x`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/consuming_wand.mcfunction)

### `lbc:other/magic_academy/upgrade/corrupter`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_upgrade.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{corrupter:1}}}}] run function lbc:other/magic_academy/upgrade/corrupter`. Команда: `tellraw @s {"translate":"upgrade_succesful_corrupter","color":"yellow"}`; `item modify entity @s weapon.mainhand lbc:tiers/c`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/corrupter.mcfunction)

### `lbc:other/magic_academy/upgrade/danmaku_staff`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_upgrade.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{danmaku_staff:1}}}}] run function lbc:other/magic_academy/upgrade/danmaku_staff`. Команда: `tellraw @s {"translate":"upgrade_succesful_danmaku_staff","color":"yellow"}`; `item modify entity @s weapon.mainhand lbc:tiers/s`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/danmaku_staff.mcfunction)

### `lbc:other/magic_academy/upgrade/dublicating_wand`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_upgrade.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{dublicating_wand:1}}}}] run function lbc:other/magic_academy/upgrade/dublicating_wand`. Команда: `tellraw @s {"translate":"upgrade_succesful_dublicating","color":"yellow"}`; `item modify entity @s weapon.mainhand lbc:tiers/ss`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/dublicating_wand.mcfunction)

### `lbc:other/magic_academy/upgrade/enderwand`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_upgrade.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{enderwand:1}}}}] run function lbc:other/magic_academy/upgrade/enderwand`. Команда: `tellraw @s {"translate":"upgrade_succesful_enderwand","color":"yellow"}`; `item modify entity @s weapon.mainhand lbc:tiers/a`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/enderwand.mcfunction)

### `lbc:other/magic_academy/upgrade/erecto_wand`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_upgrade.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{erecto_wand:1}}}}] run function lbc:other/magic_academy/upgrade/erecto_wand`. Команда: `tellraw @s {"translate":"upgrade_succesful_erecto","color":"yellow"}`; `item modify entity @s weapon.mainhand lbc:tiers/ss`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/erecto_wand.mcfunction)

### `lbc:other/magic_academy/upgrade/evocator`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_upgrade.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{evocator:1}}}}] run function lbc:other/magic_academy/upgrade/evocator`. Команда: `tellraw @s {"translate":"upgrade_succesful_evocator","color":"yellow"}`; `item modify entity @s weapon.mainhand lbc:tiers/b`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/evocator.mcfunction)

### `lbc:other/magic_academy/upgrade/exahoming_wand`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_upgrade.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{exahoming_wand:1}}}}] run function lbc:other/magic_academy/upgrade/exahoming_wand`. Команда: `tellraw @s {"translate":"upgrade_succesful_exahoming","color":"yellow"}`; `item modify entity @s weapon.mainhand lbc:tiers/x`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/exahoming_wand.mcfunction)

### `lbc:other/magic_academy/upgrade/exasperato_wand`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_upgrade.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{exasperato_wand:1}}}}] run function lbc:other/magic_academy/upgrade/exasperato_wand`. Команда: `tellraw @s {"translate":"upgrade_succesful_exasperato","color":"yellow"}`; `item modify entity @s weapon.mainhand lbc:tiers/x`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/exasperato_wand.mcfunction)

### `lbc:other/magic_academy/upgrade/exoperso`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_upgrade.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{exoperso:1}}}}] run function lbc:other/magic_academy/upgrade/exoperso`. Команда: `tellraw @s {"translate":"upgrade_succesful_exoperso","color":"yellow"}`; `item modify entity @s weapon.mainhand lbc:tiers/s`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/exoperso.mcfunction)

### `lbc:other/magic_academy/upgrade/fallingstars`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_upgrade.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{fallingstars:1}}}}] run function lbc:other/magic_academy/upgrade/fallingstars`. Команда: `tellraw @s {"translate":"upgrade_succesful_fallingstars","color":"yellow"}`; `item modify entity @s weapon.mainhand lbc:tiers/a`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/fallingstars.mcfunction)

### `lbc:other/magic_academy/upgrade/fartus_nadristailo`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_upgrade.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{fartus_nadristailo:1}}}}] run function lbc:other/magic_academy/upgrade/fartus_nadristailo`. Команда: `tellraw @s {"translate":"upgrade_succesful_fartus_nadristailo","color":"yellow"}`; `item modify entity @s weapon.mainhand lbc:tiers/x`; `item modify entity @s weapon.mainhand lbc:fartus_upgrade`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/fartus_nadristailo.mcfunction)

### `lbc:other/magic_academy/upgrade/fireblaze_wand`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_upgrade.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{fireblaze_wand:1}}}}] run function lbc:other/magic_academy/upgrade/fireblaze_wand`. Команда: `tellraw @s {"translate":"upgrade_succesful_fireblaze","color":"yellow"}`; `item modify entity @s weapon.mainhand lbc:tiers/s`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/fireblaze_wand.mcfunction)

### `lbc:other/magic_academy/upgrade/foxusing`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_upgrade.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{foxusing:1}}}}] run function lbc:other/magic_academy/upgrade/foxusing`. Команда: `tellraw @s {"translate":"upgrade_succesful_foxusing","color":"yellow"}`; `item modify entity @s weapon.mainhand lbc:tiers/a`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/foxusing.mcfunction)

### `lbc:other/magic_academy/upgrade/gaycaster`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_upgrade.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{gaycaster:1}}}}] run function lbc:other/magic_academy/upgrade/gaycaster`. Команда: `tellraw @s {"translate":"upgrade_succesful_gaycaster","color":"yellow"}`; `item modify entity @s weapon.mainhand lbc:tiers/s`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/gaycaster.mcfunction)

### `lbc:other/magic_academy/upgrade/ice_wand`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_upgrade.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{ice_wand:1}}}}] run function lbc:other/magic_academy/upgrade/ice_wand`. Команда: `tellraw @s {"translate":"upgrade_succesful_ice","color":"yellow"}`; `item modify entity @s weapon.mainhand lbc:tiers/ss`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/ice_wand.mcfunction)

### `lbc:other/magic_academy/upgrade/infbolt_wand`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_upgrade.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{infbolt_wand:1}}}}] run function lbc:other/magic_academy/upgrade/infbolt_wand`. Команда: `tellraw @s {"translate":"upgrade_succesful_infbolt","color":"yellow"}`; `item modify entity @s weapon.mainhand lbc:tiers/x`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/infbolt_wand.mcfunction)

### `lbc:other/magic_academy/upgrade/lifestealer`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_upgrade.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{lifestealer:1}}}}] run function lbc:other/magic_academy/upgrade/lifestealer`. Команда: `tellraw @s {"translate":"upgrade_succesful_lifestealer","color":"yellow"}`; `item modify entity @s weapon.mainhand lbc:tiers/a`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/lifestealer.mcfunction)

### `lbc:other/magic_academy/upgrade/lightning_bolts`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_upgrade.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{lightning_bolts:1}}}}] run function lbc:other/magic_academy/upgrade/lightning_bolts`. Команда: `tellraw @s {"translate":"upgrade_succesful_lightning_bolt","color":"yellow"}`; `item modify entity @s weapon.mainhand lbc:tiers/s`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/lightning_bolts.mcfunction)

### `lbc:other/magic_academy/upgrade/lightningwand`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_upgrade.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{lightningwand:1}}}}] run function lbc:other/magic_academy/upgrade/lightningwand`. Команда: `tellraw @s {"translate":"upgrade_succesful_lightningwand","color":"yellow"}`; `item modify entity @s weapon.mainhand lbc:tiers/a`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/lightningwand.mcfunction)

### `lbc:other/magic_academy/upgrade/neutralmage`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_upgrade.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{neutralmage:1}}}}] run function lbc:other/magic_academy/upgrade/neutralmage`. Команда: `tellraw @s {"translate":"upgrade_succesful_neutralmage","color":"yellow"}`; `item modify entity @s weapon.mainhand lbc:tiers/a`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/neutralmage.mcfunction)

### `lbc:other/magic_academy/upgrade/omegaflag`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_upgrade.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{omegaflag:1}}}}] run function lbc:other/magic_academy/upgrade/omegaflag`. Команда: `tellraw @s {"translate":"upgrade_succesful_omegaflag","color":"yellow"}`; `item modify entity @s weapon.mainhand lbc:tiers/b`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/omegaflag.mcfunction)

### `lbc:other/magic_academy/upgrade/prisma`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_upgrade.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{prisma:1}}}}] run function lbc:other/magic_academy/upgrade/prisma`. Команда: `tellraw @s {"translate":"upgrade_succesful_prisma","color":"yellow"}`; `item modify entity @s weapon.mainhand lbc:tiers/a`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/prisma.mcfunction)

### `lbc:other/magic_academy/upgrade/psychoblind_wand`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_upgrade.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{psychoblind_wand:1}}}}] run function lbc:other/magic_academy/upgrade/psychoblind_wand`. Команда: `tellraw @s {"translate":"upgrade_succesful_psychoblind","color":"yellow"}`; `item modify entity @s weapon.mainhand lbc:tiers/ss`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/psychoblind_wand.mcfunction)

### `lbc:other/magic_academy/upgrade/psychowand`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_upgrade.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{psychowand:1}}}}] run function lbc:other/magic_academy/upgrade/psychowand`. Команда: `tellraw @s {"translate":"upgrade_succesful_psychowand","color":"yellow"}`; `item modify entity @s weapon.mainhand lbc:tiers/b`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/psychowand.mcfunction)

### `lbc:other/magic_academy/upgrade/sauvojen_wand`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_upgrade.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{sauvojen_wand:1}}}}] run function lbc:other/magic_academy/upgrade/sauvojen_wand`. Команда: `tellraw @s {"translate":"upgrade_succesful_sauvojen_wand","color":"yellow"}`; `item modify entity @s weapon.mainhand lbc:tiers/x`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/sauvojen_wand.mcfunction)

### `lbc:other/magic_academy/upgrade/sheepwand`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_upgrade.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{sheepwand:1}}}}] run function lbc:other/magic_academy/upgrade/sheepwand`. Команда: `tellraw @s {"translate":"upgrade_succesful_sheepwand","color":"yellow"}`; `item modify entity @s weapon.mainhand lbc:tiers/x`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/sheepwand.mcfunction)

### `lbc:other/magic_academy/upgrade/snow_wand`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_upgrade.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{snow_wand:1}}}}] run function lbc:other/magic_academy/upgrade/snow_wand`. Команда: `tellraw @s {"translate":"upgrade_succesful_snow","color":"yellow"}`; `item modify entity @s weapon.mainhand lbc:tiers/b`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/snow_wand.mcfunction)

### `lbc:other/magic_academy/upgrade/soulcorrupter`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_upgrade.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{soulcorrupter:1}}}}] run function lbc:other/magic_academy/upgrade/soulcorrupter`. Команда: `tellraw @s {"translate":"upgrade_succesful_soulcorrupter","color":"yellow"}`; `item modify entity @s weapon.mainhand lbc:tiers/a`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/soulcorrupter.mcfunction)

### `lbc:other/magic_academy/upgrade/stacid`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_upgrade.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{stacid:1}}}}] run function lbc:other/magic_academy/upgrade/stacid`. Команда: `tellraw @s {"translate":"upgrade_succesful_stacid","color":"yellow"}`; `item modify entity @s weapon.mainhand lbc:tiers/a`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/stacid.mcfunction)

### `lbc:other/magic_academy/upgrade/stair`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_upgrade.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{stair:1}}}}] run function lbc:other/magic_academy/upgrade/stair`. Команда: `tellraw @s {"translate":"upgrade_succesful_stair","color":"yellow"}`; `item modify entity @s weapon.mainhand lbc:tiers/s`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/stair.mcfunction)

### `lbc:other/magic_academy/upgrade/stblind`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_upgrade.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{stblind:1}}}}] run function lbc:other/magic_academy/upgrade/stblind`. Команда: `tellraw @s {"translate":"upgrade_succesful_stblind","color":"yellow"}`; `item modify entity @s weapon.mainhand lbc:tiers/b`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/stblind.mcfunction)

### `lbc:other/magic_academy/upgrade/stelectro`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_upgrade.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{stelectro:1}}}}] run function lbc:other/magic_academy/upgrade/stelectro`. Команда: `tellraw @s {"translate":"upgrade_succesful_stelectro","color":"yellow"}`; `item modify entity @s weapon.mainhand lbc:tiers/a`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/stelectro.mcfunction)

### `lbc:other/magic_academy/upgrade/stfire`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_upgrade.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{stfire:1}}}}] run function lbc:other/magic_academy/upgrade/stfire`. Команда: `tellraw @s {"translate":"upgrade_succesful_stfire","color":"yellow"}`; `item modify entity @s weapon.mainhand lbc:tiers/a`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/stfire.mcfunction)

### `lbc:other/magic_academy/upgrade/stonekivi_wand`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_upgrade.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{stonekivi_wand:1}}}}] run function lbc:other/magic_academy/upgrade/stonekivi_wand`. Команда: `tellraw @s {"translate":"upgrade_succesful_stonekivi","color":"yellow"}`; `item modify entity @s weapon.mainhand lbc:tiers/x`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/stonekivi_wand.mcfunction)

### `lbc:other/magic_academy/upgrade/struby`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_upgrade.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{struby:1}}}}] run function lbc:other/magic_academy/upgrade/struby`. Команда: `tellraw @s {"translate":"upgrade_succesful_struby","color":"yellow"}`; `item modify entity @s weapon.mainhand lbc:tiers/b`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/struby.mcfunction)

### `lbc:other/magic_academy/upgrade/ststone`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_upgrade.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{ststone:1}}}}] run function lbc:other/magic_academy/upgrade/ststone`. Команда: `tellraw @s {"translate":"upgrade_succesful_ststone","color":"yellow"}`; `item modify entity @s weapon.mainhand lbc:tiers/a`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/ststone.mcfunction)

### `lbc:other/magic_academy/upgrade/stteleport`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_upgrade.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{stteleport:1}}}}] run function lbc:other/magic_academy/upgrade/stteleport`. Команда: `tellraw @s {"translate":"upgrade_succesful_stteleport","color":"yellow"}`; `item modify entity @s weapon.mainhand lbc:tiers/a`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/stteleport.mcfunction)

### `lbc:other/magic_academy/upgrade/stterra`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_upgrade.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{stterra:1}}}}] run function lbc:other/magic_academy/upgrade/stterra`. Команда: `tellraw @s {"translate":"upgrade_succesful_stterra","color":"yellow"}`; `item modify entity @s weapon.mainhand lbc:tiers/a`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/stterra.mcfunction)

### `lbc:other/magic_academy/upgrade/truefallingstars`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_upgrade.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{truefallingstars:1}}}}] run function lbc:other/magic_academy/upgrade/truefallingstars`. Команда: `tellraw @s {"translate":"upgrade_succesful_truefallingstars","color":"yellow"}`; `item modify entity @s weapon.mainhand lbc:tiers/a`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/truefallingstars.mcfunction)

### `lbc:other/magic_academy/upgrade/unstable_spellbook`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_upgrade.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{unstable_spellbook:1}}}}] run function lbc:other/magic_academy/upgrade/unstable_spellbook`. Команда: `tellraw @s {"translate":"upgrade_succesful_unstable_spellbook","color":"yellow"}`; `item modify entity @s weapon.mainhand lbc:tiers/ss`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/unstable_spellbook.mcfunction)

### `lbc:other/magic_academy/upgrade/vex_wand`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_upgrade.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{vex_wand:1}}}}] run function lbc:other/magic_academy/upgrade/vex_wand`. Команда: `tellraw @s {"translate":"upgrade_succesful_vex","color":"yellow"}`; `item modify entity @s weapon.mainhand lbc:tiers/a`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/vex_wand.mcfunction)

### `lbc:other/magic_academy/upgrade/wall`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_upgrade.mcfunction`; условие вызова: `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{wall:1}}}}] run function lbc:other/magic_academy/upgrade/wall`. Команда: `tellraw @s {"translate":"upgrade_succesful_wall","color":"yellow"}`; `item modify entity @s weapon.mainhand lbc:tiers/a`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/wall.mcfunction)

## Файлы: other / sauvojen_tuntija

### `lbc:other/sauvojen_tuntija/crafted_acidacid`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/craft_ground.mcfunction`; условие вызова: `execute if entity @s[nbt={Item:{components:{"minecraft:custom_data":{stacid:1}}}}] if entity @e[type=minecraft:item,distance=0.001..1,limit=1,nbt={Item:{components:{"minecraft:custom_data":{acidwand:1}}}}] run function lbc:other/sauvojen_tuntija/crafted_acidacid`. Loot: spawn lbc:tuntijaa_acid_acid → ~. Завершение: удаляет сущность (`@e[type=minecraft:item,sort=nearest,limit=1,nbt={Item:{id:"minecraft:carrot_on_a_stick",components:{"minecraft:custom_data":{acidwand:1}}}}]`); удаляет сущность (`@s`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sauvojen_tuntija/crafted_acidacid.mcfunction)

### `lbc:other/sauvojen_tuntija/crafted_acidacid_inv`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/craft/inventory_craft_check.mcfunction`; условие вызова: `execute if items entity @s player.crafting.* *[minecraft:custom_data~{stacid:1}] if items entity @s player.crafting.* *[minecraft:custom_data~{acidwand:1}] run function lbc:other/sauvojen_tuntija/crafted_acidacid_inv`. Loot: spawn lbc:tuntijaa_acid_acid → ~. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sauvojen_tuntija/crafted_acidacid_inv.mcfunction)

### `lbc:other/sauvojen_tuntija/crafted_acidcorrupt`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/craft_ground.mcfunction`; условие вызова: `execute if entity @s[nbt={Item:{components:{"minecraft:custom_data":{stacid:1}}}}] if entity @e[type=minecraft:item,distance=0.001..1,limit=1,nbt={Item:{components:{"minecraft:custom_data":{corrupter:1}}}}] run function lbc:other/sauvojen_tuntija/crafted_acidcorrupt`. Loot: spawn lbc:tuntijaa_acid_corrupt → ~. Завершение: удаляет сущность (`@e[type=minecraft:item,sort=nearest,limit=1,nbt={Item:{id:"minecraft:carrot_on_a_stick",components:{"minecraft:custom_data":{corrupter:1}}}}]`); удаляет сущность (`@s`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sauvojen_tuntija/crafted_acidcorrupt.mcfunction)

### `lbc:other/sauvojen_tuntija/crafted_acidcorrupt_inv`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/craft/inventory_craft_check.mcfunction`; условие вызова: `execute if items entity @s player.crafting.* *[minecraft:custom_data~{stacid:1}] if items entity @s player.crafting.* *[minecraft:custom_data~{corrupter:1}] run function lbc:other/sauvojen_tuntija/crafted_acidcorrupt_inv`. Loot: spawn lbc:tuntijaa_acid_corrupt → ~. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sauvojen_tuntija/crafted_acidcorrupt_inv.mcfunction)

### `lbc:other/sauvojen_tuntija/crafted_avarice`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/craft_ground.mcfunction`; условие вызова: `execute if entity @s[nbt={Item:{components:{"minecraft:custom_data":{struby:1}}}}] if entity @e[type=minecraft:item,distance=0.001..1,limit=1,nbt={Item:{components:{"minecraft:custom_data":{greed_greed:1}}}}] run function lbc:other/sauvojen_tuntija/crafted_avarice`. Loot: spawn lbc:tuntijaa_greed_avarice → ~. Завершение: удаляет сущность (`@e[type=minecraft:item,sort=nearest,limit=1,nbt={Item:{id:"minecraft:carrot_on_a_stick",components:{"minecraft:custom_data":{greed_greed:1}}}}]`); удаляет сущность (`@s`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sauvojen_tuntija/crafted_avarice.mcfunction)

### `lbc:other/sauvojen_tuntija/crafted_avarice_inv`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/craft/inventory_craft_check.mcfunction`; условие вызова: `execute if items entity @s player.crafting.* *[minecraft:custom_data~{struby:1}] if items entity @s player.crafting.* *[minecraft:custom_data~{greed_greed:1}] run function lbc:other/sauvojen_tuntija/crafted_avarice_inv`. Loot: spawn lbc:tuntijaa_greed_avarice → ~. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sauvojen_tuntija/crafted_avarice_inv.mcfunction)

### `lbc:other/sauvojen_tuntija/crafted_awakened_terra`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/craft_ground.mcfunction`; условие вызова: `execute if entity @s[nbt={Item:{components:{"minecraft:custom_data":{stterra:1}}}}] if entity @e[type=minecraft:item,distance=0.001..1,limit=1,nbt={Item:{components:{"minecraft:custom_data":{terrasword:1}}}}] run function lbc:other/sauvojen_tuntija/crafted_awakened_terra`. Loot: spawn lbc:tuntijaa_terra_awakened → ~. Завершение: удаляет сущность (`@e[type=minecraft:item,sort=nearest,limit=1,nbt={Item:{id:"minecraft:carrot_on_a_stick",components:{"minecraft:custom_data":{terrasword:1}}}}]`); удаляет сущность (`@s`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sauvojen_tuntija/crafted_awakened_terra.mcfunction)

### `lbc:other/sauvojen_tuntija/crafted_awakened_terra_inv`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/craft/inventory_craft_check.mcfunction`; условие вызова: `execute if items entity @s player.crafting.* *[minecraft:custom_data~{stterra:1}] if items entity @s player.crafting.* *[minecraft:custom_data~{terrasword:1}] run function lbc:other/sauvojen_tuntija/crafted_awakened_terra_inv`. Loot: spawn lbc:tuntijaa_terra_awakened → ~. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sauvojen_tuntija/crafted_awakened_terra_inv.mcfunction)

### `lbc:other/sauvojen_tuntija/crafted_blazefirewand`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/craft_ground.mcfunction`; условие вызова: `execute if entity @s[nbt={Item:{components:{"minecraft:custom_data":{stfire:1}}}}] if entity @e[type=minecraft:item,distance=0.001..1,limit=1,nbt={Item:{components:{"minecraft:custom_data":{fallingstars:1}}}}] run function lbc:other/sauvojen_tuntija/crafted_blazefirewand`. Loot: spawn lbc:tuntijaa_fire_blaze → ~. Завершение: удаляет сущность (`@e[type=minecraft:item,distance=..1,limit=1,nbt={Item:{components:{"minecraft:custom_data":{fallingstars:1}}}}]`); удаляет сущность (`@s`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sauvojen_tuntija/crafted_blazefirewand.mcfunction)

### `lbc:other/sauvojen_tuntija/crafted_blazefirewand_inv`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/craft/inventory_craft_check.mcfunction`; условие вызова: `execute if items entity @s player.crafting.* *[minecraft:custom_data~{stfire:1}] if items entity @s player.crafting.* *[minecraft:custom_data~{fallingstars:1}] run function lbc:other/sauvojen_tuntija/crafted_blazefirewand_inv`. Loot: spawn lbc:tuntijaa_fire_blaze → ~. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sauvojen_tuntija/crafted_blazefirewand_inv.mcfunction)

### `lbc:other/sauvojen_tuntija/crafted_boltbolt`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/craft_ground.mcfunction`; условие вызова: `execute if entity @s[nbt={Item:{components:{"minecraft:custom_data":{stteleport:1}}}}] if entity @e[type=minecraft:item,distance=0.001..1,limit=1,nbt={Item:{components:{"minecraft:custom_data":{lightning_bolts:1}}}}] run function lbc:other/sauvojen_tuntija/crafted_boltbolt`. Loot: spawn lbc:tuntijaa_bolts_bolts → ~. Завершение: удаляет сущность (`@e[type=minecraft:item,sort=nearest,limit=1,nbt={Item:{id:"minecraft:carrot_on_a_stick",components:{"minecraft:custom_data":{lightning_bolts:1}}}}]`); удаляет сущность (`@s`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sauvojen_tuntija/crafted_boltbolt.mcfunction)

### `lbc:other/sauvojen_tuntija/crafted_boltbolt_inv`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/craft/inventory_craft_check.mcfunction`; условие вызова: `execute if items entity @s player.crafting.* *[minecraft:custom_data~{stteleport:1}] if items entity @s player.crafting.* *[minecraft:custom_data~{lightning_bolts:1}] run function lbc:other/sauvojen_tuntija/crafted_boltbolt_inv`. Loot: spawn lbc:tuntijaa_bolts_bolts → ~. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sauvojen_tuntija/crafted_boltbolt_inv.mcfunction)

### `lbc:other/sauvojen_tuntija/crafted_energyblock`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/craft_ground.mcfunction`; условие вызова: `execute if entity @s[nbt={Item:{components:{"minecraft:custom_data":{stelectro:1}}}}] if entity @e[type=minecraft:item,distance=0.001..1,limit=1,nbt={Item:{components:{"minecraft:custom_data":{energy_shield:1}}}}] run function lbc:other/sauvojen_tuntija/crafted_energyblock`. Loot: spawn lbc:tuntijaa_electro_shield → ~. Завершение: удаляет сущность (`@e[type=minecraft:item,sort=nearest,limit=1,nbt={Item:{id:"minecraft:shield",components:{"minecraft:custom_data":{energy_shield:1}}}}]`); удаляет сущность (`@s`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sauvojen_tuntija/crafted_energyblock.mcfunction)

### `lbc:other/sauvojen_tuntija/crafted_energyblock_inv`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/craft/inventory_craft_check.mcfunction`; условие вызова: `execute if items entity @s player.crafting.* *[minecraft:custom_data~{stelectro:1}] if items entity @s player.crafting.* *[minecraft:custom_data~{energy_shield:1}] run function lbc:other/sauvojen_tuntija/crafted_energyblock_inv`. Loot: spawn lbc:tuntijaa_electro_shield → ~. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sauvojen_tuntija/crafted_energyblock_inv.mcfunction)

### `lbc:other/sauvojen_tuntija/crafted_exahoming`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/craft_ground.mcfunction`; условие вызова: `execute if entity @s[nbt={Item:{components:{"minecraft:custom_data":{stair:1}}}}] if entity @e[type=minecraft:item,distance=0.001..1,limit=1,nbt={Item:{components:{"minecraft:custom_data":{truefallingstars:1}}}}] run function lbc:other/sauvojen_tuntija/crafted_exahoming`. Loot: spawn lbc:tuntijaa_air_homing → ~. Завершение: удаляет сущность (`@e[type=minecraft:item,sort=nearest,limit=1,nbt={Item:{id:"minecraft:carrot_on_a_stick",components:{"minecraft:custom_data":{truefallingstars:1}}}}]`); удаляет сущность (`@s`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sauvojen_tuntija/crafted_exahoming.mcfunction)

### `lbc:other/sauvojen_tuntija/crafted_exahoming_inv`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/craft/inventory_craft_check.mcfunction`; условие вызова: `execute if items entity @s player.crafting.* *[minecraft:custom_data~{stair:1}] if items entity @s player.crafting.* *[minecraft:custom_data~{truefallingstars:1}] run function lbc:other/sauvojen_tuntija/crafted_exahoming_inv`. Loot: spawn lbc:tuntijaa_air_homing → ~. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sauvojen_tuntija/crafted_exahoming_inv.mcfunction)

### `lbc:other/sauvojen_tuntija/crafted_exasperoso`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/craft_ground.mcfunction`; условие вызова: `execute if entity @s[nbt={Item:{components:{"minecraft:custom_data":{stair:1}}}}] if entity @e[type=minecraft:item,distance=0.001..1,limit=1,nbt={Item:{components:{"minecraft:custom_data":{exoperso:1}}}}] run function lbc:other/sauvojen_tuntija/crafted_exasperoso`. Loot: spawn lbc:tuntijaa_air_exasperoso → ~. Завершение: удаляет сущность (`@e[type=minecraft:item,sort=nearest,limit=1,nbt={Item:{id:"minecraft:carrot_on_a_stick",components:{"minecraft:custom_data":{exoperso:1}}}}]`); удаляет сущность (`@s`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sauvojen_tuntija/crafted_exasperoso.mcfunction)

### `lbc:other/sauvojen_tuntija/crafted_exasperoso_inv`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/craft/inventory_craft_check.mcfunction`; условие вызова: `execute if items entity @s player.crafting.* *[minecraft:custom_data~{stair:1}] if items entity @s player.crafting.* *[minecraft:custom_data~{exoperso:1}] run function lbc:other/sauvojen_tuntija/crafted_exasperoso_inv`. Loot: spawn lbc:tuntijaa_air_exasperoso → ~. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sauvojen_tuntija/crafted_exasperoso_inv.mcfunction)

### `lbc:other/sauvojen_tuntija/crafted_greed`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/craft_ground.mcfunction`; условие вызова: `execute if entity @s[nbt={Item:{components:{"minecraft:custom_data":{struby:1}}}}] if entity @e[type=minecraft:item,distance=0.001..1,limit=1,nbt={Item:{components:{"minecraft:custom_data":{cons_material:1}}}}] run function lbc:other/sauvojen_tuntija/crafted_greed`. Loot: spawn lbc:tuntijaa_greed_destroy → ~. Завершение: удаляет сущность (`@e[type=minecraft:item,sort=nearest,limit=1,nbt={Item:{id:"minecraft:carrot_on_a_stick",components:{"minecraft:custom_data":{cons_material:1}}}}]`); удаляет сущность (`@s`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sauvojen_tuntija/crafted_greed.mcfunction)

### `lbc:other/sauvojen_tuntija/crafted_greed_inv`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/craft/inventory_craft_check.mcfunction`; условие вызова: `execute if items entity @s player.crafting.* *[minecraft:custom_data~{struby:1}] if items entity @s player.crafting.* *[minecraft:custom_data~{cons_material:1}] run function lbc:other/sauvojen_tuntija/crafted_greed_inv`. Loot: spawn lbc:tuntijaa_greed_destroy → ~. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sauvojen_tuntija/crafted_greed_inv.mcfunction)

### `lbc:other/sauvojen_tuntija/crafted_kiuaswand`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/craft_ground.mcfunction`; условие вызова: `execute if entity @s[nbt={Item:{components:{"minecraft:custom_data":{stfire:1}}}}] if entity @e[type=minecraft:item,distance=0.001..1,limit=1,nbt={Item:{components:{"minecraft:custom_data":{kiuaskivi:1}}}}] run function lbc:other/sauvojen_tuntija/crafted_kiuaswand`. Loot: spawn lbc:tuntijaa_fire_kiuas → ~. Завершение: удаляет сущность (`@e[type=minecraft:item,sort=nearest,limit=1,nbt={Item:{components:{"minecraft:custom_data":{kiuaskivi:1}}}}]`); удаляет сущность (`@s`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sauvojen_tuntija/crafted_kiuaswand.mcfunction)

### `lbc:other/sauvojen_tuntija/crafted_kiuaswand_inv`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/craft/inventory_craft_check.mcfunction`; условие вызова: `execute if items entity @s player.crafting.* *[minecraft:custom_data~{stfire:1}] if items entity @s player.crafting.* *[minecraft:custom_data~{kiuaskivi:1}] run function lbc:other/sauvojen_tuntija/crafted_kiuaswand_inv`. Loot: spawn lbc:tuntijaa_fire_kiuas → ~. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sauvojen_tuntija/crafted_kiuaswand_inv.mcfunction)

### `lbc:other/sauvojen_tuntija/crafted_psychoblind`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/craft_ground.mcfunction`; условие вызова: `execute if entity @s[nbt={Item:{components:{"minecraft:custom_data":{stblind:1}}}}] if entity @e[type=minecraft:item,distance=0.001..1,limit=1,nbt={Item:{components:{"minecraft:custom_data":{psychowand:1}}}}] run function lbc:other/sauvojen_tuntija/crafted_psychoblind`. Loot: spawn lbc:tuntijaa_blind_psycho → ~. Завершение: удаляет сущность (`@e[type=minecraft:item,sort=nearest,limit=1,nbt={Item:{components:{"minecraft:custom_data":{psychowand:1}}}}]`); удаляет сущность (`@s`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sauvojen_tuntija/crafted_psychoblind.mcfunction)

### `lbc:other/sauvojen_tuntija/crafted_psychoblind_inv`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/craft/inventory_craft_check.mcfunction`; условие вызова: `execute if items entity @s player.crafting.* *[minecraft:custom_data~{stblind:1}] if items entity @s player.crafting.* *[minecraft:custom_data~{psychowand:1}] run function lbc:other/sauvojen_tuntija/crafted_psychoblind_inv`. Loot: spawn lbc:tuntijaa_blind_psycho → ~. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sauvojen_tuntija/crafted_psychoblind_inv.mcfunction)

### `lbc:other/sauvojen_tuntija/crafted_psychovex`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/craft_ground.mcfunction`; условие вызова: `execute if entity @s[nbt={Item:{components:{"minecraft:custom_data":{stblind:1}}}}] if entity @e[type=minecraft:item,distance=0.001..1,limit=1,nbt={Item:{components:{"minecraft:custom_data":{summonervexes:1}}}}] run function lbc:other/sauvojen_tuntija/crafted_psychovex`. Loot: spawn lbc:tuntijaa_blind_vex → ~. Завершение: удаляет сущность (`@e[type=minecraft:item,sort=nearest,limit=1,nbt={Item:{components:{"minecraft:custom_data":{summonervexes:1}}}}]`); удаляет сущность (`@s`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sauvojen_tuntija/crafted_psychovex.mcfunction)

### `lbc:other/sauvojen_tuntija/crafted_psychovex_inv`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/craft/inventory_craft_check.mcfunction`; условие вызова: `execute if items entity @s player.crafting.* *[minecraft:custom_data~{stblind:1}] if items entity @s player.crafting.* *[minecraft:custom_data~{summonervexes:1}] run function lbc:other/sauvojen_tuntija/crafted_psychovex_inv`. Loot: spawn lbc:tuntijaa_blind_vex → ~. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sauvojen_tuntija/crafted_psychovex_inv.mcfunction)

### `lbc:other/sauvojen_tuntija/crafted_razorterra`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/craft_ground.mcfunction`; условие вызова: `execute if entity @s[nbt={Item:{components:{"minecraft:custom_data":{stterra:1}}}}] if entity @e[type=minecraft:item,distance=0.001..1,limit=1,nbt={Item:{components:{"minecraft:custom_data":{razorpine:1}}}}] run function lbc:other/sauvojen_tuntija/crafted_razorterra`. Loot: spawn lbc:tuntijaa_terra_razor → ~. Завершение: удаляет сущность (`@e[type=minecraft:item,sort=nearest,limit=1,nbt={Item:{id:"minecraft:carrot_on_a_stick",components:{"minecraft:custom_data":{razorpine:1}}}}]`); удаляет сущность (`@s`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sauvojen_tuntija/crafted_razorterra.mcfunction)

### `lbc:other/sauvojen_tuntija/crafted_razorterra_inv`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/craft/inventory_craft_check.mcfunction`; условие вызова: `execute if items entity @s player.crafting.* *[minecraft:custom_data~{stterra:1}] if items entity @s player.crafting.* *[minecraft:custom_data~{razorpine:1}] run function lbc:other/sauvojen_tuntija/crafted_razorterra_inv`. Loot: spawn lbc:tuntijaa_terra_razor → ~. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sauvojen_tuntija/crafted_razorterra_inv.mcfunction)

### `lbc:other/sauvojen_tuntija/crafted_tannerstone`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/craft_ground.mcfunction`; условие вызова: `execute if entity @s[nbt={Item:{components:{"minecraft:custom_data":{ststone:1}}}}] if entity @e[type=minecraft:item,distance=0.001..1,limit=1,nbt={Item:{components:{"minecraft:custom_data":{tannerkivi:1}}}}] run function lbc:other/sauvojen_tuntija/crafted_tannerstone`. Loot: spawn lbc:tuntijaa_stone_tanner → ~. Завершение: удаляет сущность (`@e[type=minecraft:item,sort=nearest,limit=1,nbt={Item:{components:{"minecraft:custom_data":{tannerkivi:1}}}}]`); удаляет сущность (`@s`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sauvojen_tuntija/crafted_tannerstone.mcfunction)

### `lbc:other/sauvojen_tuntija/crafted_tannerstone_inv`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/craft/inventory_craft_check.mcfunction`; условие вызова: `execute if items entity @s player.crafting.* *[minecraft:custom_data~{ststone:1}] if items entity @s player.crafting.* *[minecraft:custom_data~{tannerkivi:1}] run function lbc:other/sauvojen_tuntija/crafted_tannerstone_inv`. Loot: spawn lbc:tuntijaa_stone_tanner → ~. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sauvojen_tuntija/crafted_tannerstone_inv.mcfunction)

### `lbc:other/sauvojen_tuntija/crafted_ultraelectro`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/craft_ground.mcfunction`; условие вызова: `execute if entity @s[nbt={Item:{components:{"minecraft:custom_data":{stelectro:1}}}}] if entity @e[type=minecraft:item,distance=0.001..1,limit=1,nbt={Item:{components:{"minecraft:custom_data":{lightningwand:1}}}}] run function lbc:other/sauvojen_tuntija/crafted_ultraelectro`. Loot: spawn lbc:tuntijaa_electro_electro → ~. Завершение: удаляет сущность (`@e[type=minecraft:item,sort=nearest,limit=1,nbt={Item:{id:"minecraft:carrot_on_a_stick",components:{"minecraft:custom_data":{lightningwand:1}}}}]`); удаляет сущность (`@s`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sauvojen_tuntija/crafted_ultraelectro.mcfunction)

### `lbc:other/sauvojen_tuntija/crafted_ultraelectro_inv`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/craft/inventory_craft_check.mcfunction`; условие вызова: `execute if items entity @s player.crafting.* *[minecraft:custom_data~{stelectro:1}] if items entity @s player.crafting.* *[minecraft:custom_data~{lightningwand:1}] run function lbc:other/sauvojen_tuntija/crafted_ultraelectro_inv`. Loot: spawn lbc:tuntijaa_electro_electro → ~. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sauvojen_tuntija/crafted_ultraelectro_inv.mcfunction)

### `lbc:other/sauvojen_tuntija/crafted_ultrareforge`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/craft_ground.mcfunction`; условие вызова: `execute if entity @s[nbt={Item:{components:{"minecraft:custom_data":{stteleport:1}}}}] if entity @e[type=minecraft:item,distance=0.001..1,limit=1,nbt={Item:{components:{"minecraft:custom_data":{bolt_material:1}}}}] run function lbc:other/sauvojen_tuntija/crafted_ultrareforge`. Loot: spawn lbc:tuntijaa_bolts_reforging → ~. Завершение: удаляет сущность (`@e[type=minecraft:item,sort=nearest,limit=1,nbt={Item:{components:{"minecraft:custom_data":{bolt_material:1}}}}]`); удаляет сущность (`@s`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sauvojen_tuntija/crafted_ultrareforge.mcfunction)

### `lbc:other/sauvojen_tuntija/crafted_ultrareforge_inv`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/craft/inventory_craft_check.mcfunction`; условие вызова: `execute if items entity @s player.crafting.* *[minecraft:custom_data~{stteleport:1}] if items entity @s player.crafting.* *[minecraft:custom_data~{bolt_material:1}] run function lbc:other/sauvojen_tuntija/crafted_ultrareforge_inv`. Loot: spawn lbc:tuntijaa_bolts_reforging → ~. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sauvojen_tuntija/crafted_ultrareforge_inv.mcfunction)

### `lbc:other/sauvojen_tuntija/crafted_wallstone`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/craft_ground.mcfunction`; условие вызова: `execute if entity @s[nbt={Item:{components:{"minecraft:custom_data":{ststone:1}}}}] if entity @e[type=minecraft:item,distance=0.001..1,limit=1,nbt={Item:{components:{"minecraft:custom_data":{wall:1}}}}] run function lbc:other/sauvojen_tuntija/crafted_wallstone`. Loot: spawn lbc:tuntijaa_stone_wall → ~. Завершение: удаляет сущность (`@e[type=minecraft:item,sort=nearest,limit=1,nbt={Item:{id:"minecraft:carrot_on_a_stick",components:{"minecraft:custom_data":{wall:1}}}}]`); удаляет сущность (`@s`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sauvojen_tuntija/crafted_wallstone.mcfunction)

### `lbc:other/sauvojen_tuntija/crafted_wallstone_inv`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/craft/inventory_craft_check.mcfunction`; условие вызова: `execute if items entity @s player.crafting.* *[minecraft:custom_data~{ststone:1}] if items entity @s player.crafting.* *[minecraft:custom_data~{wall:1}] run function lbc:other/sauvojen_tuntija/crafted_wallstone_inv`. Loot: spawn lbc:tuntijaa_stone_wall → ~. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sauvojen_tuntija/crafted_wallstone_inv.mcfunction)

### `lbc:other/sauvojen_tuntija/drop_all_wands`

Автоматический caller среди исследованных функций не найден; при ручном запуске выполняются действия ниже. Создаёт `item` с {Age:-32768,NoGravity:1b,Glowing:1b,CustomNameVisible:1b,Health:999990,CustomName:{"color":"gold","italic":false,"translate":"sauvojen_wand_ruby"},Item:{id:"minecraft:carrot_on_a_s; `item` с {Age:-32768,NoGravity:1b,Glowing:1b,CustomNameVisible:1b,Health:999990,CustomName:{"color":"gold","italic":false,"translate":"sauvojen_wand_terra"},Item:{id:"minecraft:carrot_on_a_; `item` с {Age:-32768,NoGravity:1b,Glowing:1b,CustomNameVisible:1b,Health:999990,CustomName:{"color":"gold","italic":false,"translate":"sauvojen_wand_electro"},Item:{id:"minecraft:carrot_on_; `item` с {Age:-32768,NoGravity:1b,Glowing:1b,CustomNameVisible:1b,Health:999990,CustomName:{"color":"gold","italic":false,"translate":"sauvojen_wand_air"},Item:{id:"minecraft:carrot_on_a_st; `item` с {Age:-32768,NoGravity:1b,Glowing:1b,CustomNameVisible:1b,Health:999990,CustomName:{"color":"gold","italic":false,"translate":"sauvojen_wand_acid"},Item:{id:"minecraft:carrot_on_a_s. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sauvojen_tuntija/drop_all_wands.mcfunction)

### `lbc:other/sauvojen_tuntija/drop_wand`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/drops/infinity.mcfunction`; условие вызова: `execute as @e[type=minecraft:item_display,distance=..40,tag=savoujen_cloud] at @s run function lbc:other/sauvojen_tuntija/drop_wand`. Создаёт `item` с {Age:-32768,NoGravity:1b,Glowing:1b,CustomNameVisible:1b,Health:999990,CustomName:{"color":"gold","italic":false,"translate":"sauvojen_wand_ruby"},Item:{id:"minecraft:carrot_on_a_s; `item` с {Age:-32768,NoGravity:1b,Glowing:1b,CustomNameVisible:1b,Health:999990,CustomName:{"color":"gold","italic":false,"translate":"sauvojen_wand_terra"},Item:{id:"minecraft:carrot_on_a_; `item` с {Age:-32768,NoGravity:1b,Glowing:1b,CustomNameVisible:1b,Health:999990,CustomName:{"color":"gold","italic":false,"translate":"sauvojen_wand_electro"},Item:{id:"minecraft:carrot_on_; `item` с {Age:-32768,NoGravity:1b,Glowing:1b,CustomNameVisible:1b,Health:999990,CustomName:{"color":"gold","italic":false,"translate":"sauvojen_wand_air"},Item:{id:"minecraft:carrot_on_a_st; `item` с {Age:-32768,NoGravity:1b,Glowing:1b,CustomNameVisible:1b,Health:999990,CustomName:{"color":"gold","italic":false,"translate":"sauvojen_wand_acid"},Item:{id:"minecraft:carrot_on_a_s. Завершение: удаляет сущность (`@s`). [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sauvojen_tuntija/drop_wand.mcfunction)

### `lbc:other/sauvojen_tuntija/select`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/tick2smobs.mcfunction`; условие вызова: `execute if entity @s[type=minecraft:ghast,tag=sauvojen_tuntija] if entity @e[type=#minecraft:mobs,distance=..110,tag=!spectator,tag=!rcdenyalways,tag=!friendly_tuntija] run function lbc:other/sauvojen_tuntija/select`. Эффект: @s[predicate=lbc:chance35] получает `minecraft:resistance` на 1 секунд, amplifier 4. Создаёт `item_display` с {interpolation_duration:1,teleport_duration:1,item_display:"fixed",transformation:[0.0f,0.0f,2.5f,0.0f,1.7678f,-1.7678f,0.0f,0.0f,1.7678f,1.7678f,0.0f,0.0f,0.0f,0.0f,0.0f,1.0f],ite. Изменяет блоки: fill ~15 ~15 ~15 ~-15 ~-15 ~-15 minecraft:air strict. Передаёт шаг в `lbc:drops/infinity`. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sauvojen_tuntija/select.mcfunction)

### `lbc:other/sauvojen_tuntija/select_eoe`

Автоматический вход из `world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/53.mcfunction`; условие вызова: `execute at @e[tag=!spectator,sort=random,limit=24] positioned ~ ~30 ~ run function lbc:other/sauvojen_tuntija/select_eoe`. Создаёт `item_display` с {interpolation_duration:1,teleport_duration:1,item_display:"fixed",transformation:[0.0f,0.0f,2.5f,0.0f,1.7678f,-1.7678f,0.0f,0.0f,1.7678f,1.7678f,0.0f,0.0f,0.0f,0.0f,0.0f,1.0f],ite. [Исходник](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sauvojen_tuntija/select_eoe.mcfunction)



## Проверенные механики семейств (ручная сверка)

### Гримуарный босс: диспетчеризация и фазы

`mobs/grimoire_boss/tick` работает от визуального исполнителя босса: ищет его Warden-hitbox на 6 блоков ниже, вызывает обслуживание/обновление босса, а при исчезновении hitbox вызывает завершение. При наличии хитбокса `tick1` подбирает боссу одну из 32 команд-тим по тегу hitbox; `tick_true` обновляет bossbar из `Health`, показывает попадание при `HurtTime:10s`, снимает временный `nodamage` и запускает текущую фазу. Выбор атаки возможен только при `phase_idle` и `chance1`: случайно выбирается один из трёх вариантов, счётчик Lifetime обнуляется; частота и приоритет этих тиков принадлежат внешнему `raycastselector_other`/runtime-контуру, а не функциям фаз.

`phase_danmaku` начинается синей пылью и добавляет тег фазы. Начиная с Lifetime 20, на каждом boss tick с `chance30` выбираются до трёх ближайших подходящих мобов в радиусе 30 (исключая spectator, `dungeon_mob`, `nodamage`); по каждому вызывается одна из вариаций sword spawn/protection. При Lifetime 200 фаза завершает серию ещё 25 вызовами выбора трёх целей, снимает тег и возвращает `phase_idle`; фиксированной маны/стоимости нет.

Лазерная фаза выбирает ближайшую цель-моба в радиусе 40 (исключает spectator, dungeon mob, nodamage и strict-map-object), поворачивает marker в её сторону и гонит ray с шагом 0,3 блока. Пока клетка свободна, проверка `Distance ..200` ограничивает луч примерно 60 блоками; при каждом ray step в кубе 1×1×1 (`dx=dy=dz=0`) с дистанцией до 8 команда наносит 10 magic damage максимум двум допустимым мобам. Marker появляется на Lifetime 70, луч обслуживается при Lifetime 71+, а Lifetime 250 вызывает очистку: удаляются близкие `spinning_red_penis` displays и laser marker, фаза возвращается в idle.

Метеорная фаза с Lifetime 30+ делает очередной запуск только при `chance50`; до Lifetime 200 она создаёт marker на 20 блоков выше и в 7 блоках перед исполнителем. Оттуда до 49 отдельных позиций в сетке 7×7 получают каждая независимый бросок `chance40` и могут породить block_display из chiseled tuff bricks (то есть среднее около 19–20 камней на полный запуск, а не гарантированная сплошная плита). Периодическое обслуживание прекращает фазу при Lifetime 250+, снимая её тег и включая idle; летящие камни имеют отдельный `falling_meteor` handler: до Lifetime 100 проверяют землю, при столкновении разрушают блок непосредственно выше себя и удаляются.

Отдельный prism-контур имеет свой маркер-призму: preloop рисует луч длиной до 90 шагов по 0,3 (27 блоков); prism_tick запускает такие предупреждающие лучи на Lifetime 1, 11 и 21, а с 31 — firing loop. Луч движется до 90 шагов; при первом подходящем мобе в текущем 1-блочном кубе и дистанции ≤8 наносит 7 arrow damage и ставит ему `hit`, чтобы не повторить попадание в ту же цель в пределах этой серии. По окончании цикл очищает hit tags, играет звук/вспышку и удаляет prism emitter. Это продолжение отдельного item-display emitter, а не одна из трёх случайных phase функций.

При исчезновении hitbox `death` удаляет все `boss_piece`; если во всём измерении больше нет grimoire hitbox, удаляет bossbar и останавливает bossfight sound, после чего убивает текущий visual. Сам `tick` также синхронизирует visual с найденным hitbox через `tppos`; в этих функциях нет платы от игрока или расходуемого предмета. [Полный диспетчер и фазы](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/tick_true.mcfunction).

### Академия: реальный цикл взаимодействия

Advancement `lbc:interacted_with_academy` выдаёт `interacted` игроку при клике по сущности interaction с тегом `magic_academy`, если в руке предмет с `custom_data.staff:1`; также он исключает creative и измерения Nexus/imprinted. `commands` лишь ставит текстовый welcome-display и interaction размером 10×6 на координаты, выровненные по xyz. В `interacted` специальная метка `pissed_by_madoka_sob_sob_sob_sob_sob:1` запрещает апгрейд; уже upgraded сообщает об этом; `upgrade_available:1` ведёт к upgrade dispatcher, а обычный предмет получает `upgradable:1` и проходит item-specific can_upgrade checks. В конце advancement отзывается.

`can_upgrade` только запускает status helper для предметов, кроме `unstable_spellbook`; `can_upgrade_book` показывает звук/текст и через container slot 0 абсолютного блока 10241024 100 10241024 ставит предмету `upgrade_available:1`, `upgradable:0`. Предметные `can_upgrade/<key>` обработчики — только жёлтый текст `upgrade_condition_<key>`: численные/ресурсные условия живут в вызывающем коде, так что этот ответ сам по себе не доказывает наличие оплаты. Пустой `can_upgrade2` не исполняет команды.

Игрок, повторно кликнувший академию с предметом `upgrade_available:1` и без `upgraded:1`, попадает в `interacted_upgrade`: предмет получает `upgraded:1` и `mode:1`, на него добавляется modifier `lbc:add_upgrade_desc`, challenge progress увеличивается на 1, а при `lbc.challenge=1` и значении challenge_score ≥20 вызывается `completed_challenge` (выдаёт `lbc:challenges/no_knowledge` и сбрасывает оба challenge счётчика). Далее 53 независимые последовательные проверки смотрят на ключи custom_data: обычный предмет совпадает с одним ключом, а предмет с несколькими такими ключами вызовет несколько `upgrade/<key>` handlers по порядку строк. Каждый совпавший handler сообщает текст и применяет свой конкретный `lbc:tiers/<tier>` item modifier. Единой денежной/мановой цены у этих 53 handlers нет, а боевые изменения находятся в соответствующих item definitions/modifiers, не в этих функциях.

`change_mode_*`, `change_tier_*` и четыре `change_mode_8_gaycaster_color*` — обслуживающие функции смены выбранного параметра предмета в main hand или offhand. Они временно переносят предмет в container slot, читают `custom_data.mode`, `tier` либо `color`, двигают scoreboard на один шаг, заворачивают значение на установленных в конкретном файле границах, записывают значение обратно и возвращают предмет в тот же слот. Суффиксы `_left` означают offhand; различающиеся числовые суффиксы задают предел и направление цикла, поэтому их нельзя свести к единому «прибавить режим» без проверки соответствующего источника. [Триггер advancement](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/interacted_with_academy.json), [interaction dispatcher](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted.mcfunction).

### Крафт жезлов Sauvojen Tuntija

Ground-вариант запускается из `tick1s`: раз в секунду каждый предмет-entity с `craftusable:1` ищет второй craftusable item в 0,001–1 блоке; `craft_ground` dispatch-ит по паре ключей ниже. Точка исполнения — позиция первого item entity. Рецепт выдаёт loot table, звук и частицы, затем удаляет ингредиенты; у большинства ground handlers селектор удаления второго ингредиента не ограничен расстоянием, но до функции caller требует парный предмет в пределах 1 блока. Inventory-вариант проверяется из `players_tick` у игрока, если в его crafting grid есть `craftusable:1`; он спавнит тот же loot table и уменьшает на 1 каждый совпавший ингредиент в слотах crafting. Это может повторно крафтить при оставшихся стопках, пока условие снова истинно; отдельной маны/перезарядки у рецепта нет.

| Handler | Пара custom_data | Выходная loot table |
|---|---|---|
| `crafted_greed` | `struby` + `cons_material` | `tuntijaa_greed_destroy` |
| `crafted_avarice` | `struby` + `greed_greed` | `tuntijaa_greed_avarice` |
| `crafted_awakened_terra` | `stterra` + `terrasword` | `tuntijaa_terra_awakened` |
| `crafted_razorterra` | `stterra` + `razorpine` | `tuntijaa_terra_razor` |
| `crafted_ultraelectro` | `stelectro` + `lightningwand` | `tuntijaa_electro_electro` |
| `crafted_energyblock` | `stelectro` + `energy_shield` | `tuntijaa_electro_shield` |
| `crafted_exasperoso` | `stair` + `exoperso` | `tuntijaa_air_exasperoso` |
| `crafted_exahoming` | `stair` + `truefallingstars` | `tuntijaa_air_homing` |
| `crafted_acidacid` | `stacid` + `acidwand` | `tuntijaa_acid_acid` |
| `crafted_acidcorrupt` | `stacid` + `corrupter` | `tuntijaa_acid_corrupt` |
| `crafted_boltbolt` | `stteleport` + `lightning_bolts` | `tuntijaa_bolts_bolts` |
| `crafted_ultrareforge` | `stteleport` + `bolt_material` | `tuntijaa_bolts_reforging` |
| `crafted_wallstone` | `ststone` + `wall` | `tuntijaa_stone_wall` |
| `crafted_tannerstone` | `ststone` + `tannerkivi` | `tuntijaa_stone_tanner` |
| `crafted_kiuaswand` | `stfire` + `kiuaskivi` | `tuntijaa_fire_kiuas` |
| `crafted_blazefirewand` | `stfire` + `fallingstars` | `tuntijaa_fire_blaze` |
| `crafted_psychoblind` | `stblind` + `psychowand` | `tuntijaa_blind_psycho` |
| `crafted_psychovex` | `stblind` + `summonervexes` | `tuntijaa_blind_vex` |

`drop_all_wands` вызывает 9 frozen/glowing wand item entities (ruby, terra, electro, air, acid, teleport, stone, fire, blind) с их соответствующими tier/custom_data ключами. `drop_wand` создаёт одну такую палочку для каждого присутствующего тега `sauvojen_<element>` и затем убивает исполнителя; это не random выбор, поэтому при нескольких тегах появятся несколько жезлов. `select` случайно выбирает 1..10: варианты 1..9 создают фиксированный wand item-display emitter с соответствующим area/raycast tag, а вариант 10 очищает куб воздуха ±15 и оставляет particle; `select_eoe` делает только равномерный выбор 1..9 без ветки очистки. Обе функции ставят portal particles; `select` дополнительно даёт resistance при `chance35`, переносит исполнителя из крайних координат/y-диапазонов и вызывает `drops/infinity`, если Health×1 ≤99900. Для выбранных display tags дальнейшие атаки/расходы выполняют внешние raycast/effect обработчики; этот job фиксирует только создание эмиттера и сам по себе не устанавливает стоимость.


Ветка `room_reward_random` выбирает равномерно 1..12 и вызывает одну награду: 1 — marker miniboss (случайно golden skeleton или tiny skeleton); 2 — barrel с loot `random_glack_1`; 3 — chest с `random_grimoires`; 4 — spawner ежей; 5–9 — группы skeletons, zombies, pillagers, creepers и breezes; 10 — lucky mobs; 11 — luckyblocks; 12 — mine. После обработки маркер удаляется, а под ним ставится red stained glass. Это равновероятные ветви без входной платы; содержимое loot tables определено отдельно в `lbc_loot_definitions.md`. Отдельная boss-подготовка включается для barrier в точке reward и живого игрока не дальше 8 блоков; miniboss helper случайно выбирает одного из двух типов, а boss helper разыгрывает shriek/sound и ставит barrier, затем передаёт summon/preparation helper. 
