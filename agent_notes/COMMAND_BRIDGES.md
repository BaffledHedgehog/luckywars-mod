# Команды, передаваемые за пределы функций datapack

Исходники обработчиков найдены в Paper-плагине
`E:/servers_mew/cringe1_21_4_skywars/plugins/luckywars`; Ignite-мод
`mods/luckywarsignite` дополняет защиту BedWars через mixin hooks.
Контракты Java-команд: [COMMAND_IMPLEMENTATIONS.md](COMMAND_IMPLEMENTATIONS.md).
Наличие регистрации в коде отличается от проверки работающего сервера.

Предметы, запускаемые без команд через события и задачи плагина, описаны в
[PLUGIN_ITEMS_AND_HOOKS.md](PLUGIN_ITEMS_AND_HOOKS.md): Nexus Compass, Rocket Armor,
Metal Pipe, Malevich и Terra Wand. Native/legacy запуск и ограничения загрузки
ресурсов — в [NATIVE_RUNTIME_OVERVIEW.md](NATIVE_RUNTIME_OVERVIEW.md).

Некоторые функции подставляют в начало команды строку из macro storage. Ниже перечислены все такие строки и отдельный буквальный вызов `buildmanager` в трёх исследуемых пакетах. Это входы в серверные команды; их Java-реализации теперь описываются отдельно.

Например, `mapclear includespecial`, `motion`, `clonetonexus`, `rotatemap`, `regen`, `badapple`, `antigravity` и `bedwars` не объясняются телом macro helper: он только передаёт команду. Эффект берётся из соответствующего Java-обработчика с учётом аргументов и контекста вызова. `transfer`, `kick` и `restart` тоже встречаются в подстановках, но их собственные определения в просмотренном реестре LuckyWars отсутствуют: разрешение этих имён зависит от серверной платформы/окружения.

## Где находятся обработчики

| Команда из datapack | Реализация |
|---|---|
| `motion` | `command/MotionBrigadier.java` |
| `midas`, `midasfr` | `command/MidasCommand.java`; второй вариант отключает обработку сущностей и ограничивает распространение |
| `pulsar` | `command/PulsarCommand.java` |
| `regen` | `command/Regeneration.java`; собственные эффекты регенерации и предметные modifiers |
| `clonetonexus` | `command/NexusCloneCommand.java` |
| `antigravity` | `command/AntigravityCommand.java` |
| `rotatemap`, `magmify` | `RotateMapService.java`, `MagmifyService.java` |
| `buildmanager` | `command/CommandRegistry.java` → `BuildManagerImpl.java` |
| `badapple` | `badapple/BadAppleCommand.java` → `BadAppleService.java` |
| `mapclear`, `bedwars`, `teaminit` | `bedwars/MapClearCommand.java`, `BedwarsCommand.java`, `TeamInitCommand.java` |

Регистрация прослежена от
[SigmaWarsMain](E:/servers_mew/cringe1_21_4_skywars/plugins/luckywars/src/main/java/me/luckywars/SigmaWarsMain.java)
через [CommandRegistry](E:/servers_mew/cringe1_21_4_skywars/plugins/luckywars/src/main/java/me/luckywars/command/CommandRegistry.java)
и [plugin.yml](E:/servers_mew/cringe1_21_4_skywars/plugins/luckywars/src/main/resources/plugin.yml).
Важные расхождения: `buildmanager $(pass) @s` не проверяет пароль;
`mapclear` не выбирает мир по заявленному аргументу; `bedwars` проверяет
другой permission, чем объявлен в descriptor. Подробности — в документе реализаций.

## Контракты исходных macro-вызовов

Список `set value` показывает возможные присваивания поля во всех функциях. Совпадение имени поля не доказывает, что именно это значение попадёт в конкретный вызов: проверяйте storage, caller и порядок команд. Другие способы записи, inline-аргументы и состояние, оставшееся от прошлых вызовов, автоматически здесь не вычисляются.

[Полный контракт с callers и строками присваивания](inventory/command_bridges.json). Исходники не изменены.

## `swrg:transfer_1`

`$$(transfer) "d12.joinserver.xyz" 25916 @s`

Источник: [transfer_1.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/transfer_1.mcfunction) — строка 1.
Найденные буквальные присваивания: `transfer = transfer`.
Прямых function callers: 1.

## `swrg:core/autorestart_3`

`$$(mew)`

Источник: [autorestart_3.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/core/autorestart_3.mcfunction) — строка 1.
Найденные буквальные присваивания: `mew = restart`.
Прямых function callers: 1.

## `swrg:maploader/clear1`

`$$(mapclear)`

Источник: [clear1.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/maploader/clear1.mcfunction) — строка 1.
Найденные буквальные присваивания: `mapclear = "mapclear includespecial"`.
Прямых function callers: 1.

## `lbc:run_command`

`$$(cmd)`

Источник: [run_command.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/run_command.mcfunction) — строка 1.
Найденные буквальные присваивания: `cmd = "motion add ^ ^-0.01 ^.1 @s"`; `cmd = "motion add ^ ^ ^.4 @s"`; `cmd = "execute rotated ~ 0 run motion add ^ ^ ^.2 @s"`; `cmd = "midasfr"`; `cmd = "midas"`; `cmd = "pulsar"`; `cmd = "motion add ^ ^ ^.1 @s"`; `cmd = "execute as @s at @s positioned as @s rotated as @s run motion set ~ ~1 ~ @s"`; `cmd = "motion set ~ ~1.2 ~ @s"`; `cmd = "motion add ^ ^ ^.2 @s"`; `cmd = "motion set ^ ^ ^3 @s"`; `cmd = "motion set ^ ^ ^10 @s"`; `cmd = "motion add ^ ^ ^1 @e[distance=0.01..5,type=#mobs,tag=!spectator]"`; `cmd = "motion add ^ ^ ^1 @s"`; `cmd = "execute facing entity @e[type=item_display,distance=..20,tag=metasola_3,limit=1] eyes rotated ~ 0 run motion add ^.074 ^.08 ^.08 @s"`.
Прямых function callers: 15.

## `lbc:run_command1`

`$$(cmd1)`

Источник: [run_command1.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/run_command1.mcfunction) — строка 1.
Найденные буквальные присваивания: `cmd1 = "motion add ^ ^ ^1 @e[distance=..5,type=#mobs,tag=!spectator]"`.
Прямых function callers: 1.

## `lbc:other/pidor_yep`

`$$(clonetonexus)`

Источник: [pidor_yep.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/pidor_yep.mcfunction) — строка 2.
Найденные буквальные присваивания: `clonetonexus = clonetonexus`.
Прямых function callers: 1.

## `lbc:workingitems/supernovacore_newworld_clonestop`

`$$(clonetonexus) stop`

Источник: [supernovacore_newworld_clonestop.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/supernovacore_newworld_clonestop.mcfunction) — строка 1.
Найденные буквальные присваивания: `clonetonexus = clonetonexus`.
Прямых function callers: 1.

## `lbc:workingitems/awaken_tuntija_wands/stonekivi/loop_sun_end_nerfed1`

`$$(command)`

Источник: [loop_sun_end_nerfed1.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/awaken_tuntija_wands/stonekivi/loop_sun_end_nerfed1.mcfunction) — строка 1.
Найденные буквальные присваивания: `command = "magmify"`; `command = rotatemap`.
Прямых function callers: 1.

## `lbc:swrg_kit_integration/gui/build_manager/open1`

`$execute as @s run buildmanager $(pass) @s`

Источник: [open1.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/build_manager/open1.mcfunction) — строка 1.
Прямых function callers: 1.

## `lbc:swrg_kit_integration/game/levelup_select/levelups_remove_1`

`$$(regen) set 0 @s`

Источник: [levelups_remove_1.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/levelup_select/levelups_remove_1.mcfunction) — строка 1.
Найденные буквальные присваивания: `regen = regen`.
Прямых function callers: 1.

## `lbc:swrg_kit_integration/game/skills/rainbow_aura/damaged_change_aqua`

`$$(o)regen add -0.025 @s`

Источник: [damaged_change_aqua.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/rainbow_aura/damaged_change_aqua.mcfunction) — строка 4.
Найденные буквальные присваивания: `o = ""`.
Прямых function callers: 1.

## `lbc:swrg_kit_integration/game/skills/rainbow_aura/damaged_change_lime`

`$$(o)regen add 0.025 @s`

Источник: [damaged_change_lime.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/rainbow_aura/damaged_change_lime.mcfunction) — строка 3.
Найденные буквальные присваивания: `o = ""`.
Прямых function callers: 1.

## `lbc:other/imba/bad_apple/display_tick1`

`$$(b)adapple start`

Источник: [display_tick1.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/bad_apple/display_tick1.mcfunction) — строка 1.
Найденные буквальные присваивания: `b = b`.
Прямых function callers: 1.

## `lbc:other/imba/bad_apple/end1`

`$$(b)adapple stop`

Источник: [end1.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/bad_apple/end1.mcfunction) — строка 1.
Найденные буквальные присваивания: `b = b`.
Прямых function callers: 1.

## `lbc:items/swords/banhammer/ability_at`

`$$(kick) @s "был забанен Банхаммером"`

Источник: [ability_at.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/banhammer/ability_at.mcfunction) — строка 40394.
Найденные буквальные присваивания: `kick = kick`.
Прямых function callers: 2.

## `lbc:items/misc/huynya/click_m`

`$$(command)`

Источник: [click_m.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/misc/huynya/click_m.mcfunction) — строка 1.
Найденные буквальные присваивания: `command = "magmify"`; `command = rotatemap`.
Прямых function callers: 2.

## `lbc:drops/antigravity/tick_10t_marker_at`

`$$(antigravity)`

Источник: [tick_10t_marker_at.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/drops/antigravity/tick_10t_marker_at.mcfunction) — строка 1.
Найденные буквальные присваивания: `antigravity = antigravity`.
Прямых function callers: 1.

## `lbc:bedwars/game/end1`

`$$(bedwars) stop`

Источник: [end1.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/bedwars/game/end1.mcfunction) — строка 1.
Найденные буквальные присваивания: `bedwars = bedwars`.
Прямых function callers: 1.

## `lbc:bedwars/game/start1`

`$$(t)eaminit`

Источник: [start1.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/bedwars/game/start1.mcfunction) — строка 1.
Найденные буквальные присваивания: `t = t`.
Прямых function callers: 1.

## `lbc:bedwars/game/start1`

`$$(bedwars) init`

Источник: [start1.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/bedwars/game/start1.mcfunction) — строка 4.
Найденные буквальные присваивания: `bedwars = bedwars`.
Прямых function callers: 1.

