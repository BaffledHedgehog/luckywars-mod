# `nomagic`: источник и точка переноса

Источник только для чтения: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc`.

## Состояния, которые нельзя смешивать

- **Счётчик `nomagic`** — целочисленная длительность в секундах. Это не potion effect: `other/effect_nomagic.mcfunction` прибавляет 15 и сразу устанавливает `mana=0`; самого эффекта Minecraft она не выдаёт. Таймер убывает на 1 только раз в секунду: `players_1s.mcfunction:20`, вызываемый из `mobs_1s.mcfunction:7` для игроков не в spectator. При time stop `time_stop.mcfunction` отменяет расписание `tick1s`; `load_aftertimestop.mcfunction` восстанавливает его после завершения остановки. Поэтому countdown **паузается** на время time stop. Положительный счётчик также выдаёт advancement в `players_2s.mcfunction:14`.
- **Аура `nomagic_active`** — отдельное состояние предмета mystic type 1. `items/changed_inventory/sel.mcfunction:60-62` добавляет/снимает тег; `players_2t.mcfunction:7` запускает `other/artifact/nomagic`, которая очищает nearby entities из `#minecraft:magic_entities` и другие помеченные magic сущности. Тег также служит запретом в wand dispatchers и hostile selectors.
- **`stopper_magic` / маяк подавления** — пространственный запрет с радиусом 20 блоков, отдельный от обоих состояний выше. Например, mana dispatcher проверяет marker напрямую в `players_tick.mcfunction:33`, а Crux cast — в `workingitems/spell1_default.mcfunction:114`. Native `LbcMagic.suppressed()` объединяет активную ауру и beacon suppression для проверок способностей; это не должно стирать или подменять персональный таймер.

## Все найденные источники и очистки таймера

Источники `scoreboard players ... nomagic`:

- `other/effect_nomagic.mcfunction:1`: +15 секунд, вместе с обнулением маны. Вызывается antimagic sword попаданием из `items/swords.mcfunction:46`; selector выбирает пострадавших игроков с `HurtTime:10s` и `predicate=!lbc:same_team`.
- `items/swords/get_penised_3.mcfunction:5`, `_4:5`, `_5:5`: +100, +1000 и +10000 секунд. Их вызывают удары diamond/netherstar, transdimensional и multiverse fish в `items/swords.mcfunction:49-53`; селекторы используют `predicate=!lbc:same_team`.
- `random_debuff.mcfunction:45`: +30 секунд.
- `other/eatables/netherite_stew.mcfunction:51`: ставит `2147483647` (практически бессрочный счётчик, но не отдельный тип состояния).
- `other/eatables/kfc_chick/all_up_scores_no_mana.mcfunction:77`: +1 при каждом выполнении этого challenge helper.
- `workingitems/witch_gens/vanity.mcfunction:102` и `vanity_respawn.mcfunction:94`: сохраняют и восстанавливают score через marker; это перенос состояния, не новый источник длительности.

Явные сбросы: `resets.mcfunction:36` и `leave_death.mcfunction:300` ставят 0; cleanse paths `other/purify.mcfunction:38`, `other/wisp_book/passive_effects/light/remove_debuff/4.mcfunction:24`, `projectiles/arrows2_nodebuff.mcfunction:25` и случайный исход `workingitems/popit.mcfunction:25` тоже очищают счётчик. Учесть отдельно, что Vanity respawn затем может восстановить сохранённое значение.

## Что именно блокирует счётчик

Главный resource consumer — единственный gate `players_tick.mcfunction:33`: при `nomagic>0` пропускается `other/manaregen`, то есть регенерация, генерация/rearm Crux и negative-mana dispatcher. Сам `nomagic` score **не является общим запретом wand cast**: `items/spells.mcfunction` и wand dispatcher отдельно проверяют aura-tag и `stopper_magic`; у отдельных wand entries свои условия. В частности, Crux entry проверяет `stopper_magic`, но не score `nomagic` (`workingitems/spell1_default.mcfunction:114`). При переносе не надо добавлять countdown в общий `LbcMagic.suppressed()` или запрещать им `trySpendWand()` без отдельного требования: это смешает resource pause с source cast gates.

В текущем native-коде `LbcMana` уже владеет scoreboard bridge, общей mana tick и one-second `secondPlayer()` cadence; `LbcItems.clear()` ведёт в `mana.clearEffects()`, а `GameController.died/resetPlayer` использует существующий item cleanup. Если потребуется переносить сам countdown, естественная точка — scoreboard-backed поле/accessor и секундный decrement в `LbcMana`, со сбросом через существующие `clearEffects/resetResources/stop` paths. Декремент должен быть внутри time-stop guard существующего секундного пути: source timer приостановлен, пока отключено расписание `tick1s`. Сейчас countdown и перечисленные sword/fish providers в новом проекте не реализованы; это будущий перенос, а не уже работающая native-функция.

`LbcFrost.hitTargets()` уже показывает локальный пример проверки `!owner.isAlliedTo(entity)`, но `GameController.damaged()` пока фильтрует player damage по стадиям/участию, не по командам. Это не общий предохранитель для будущих wand-эффектов.

## Требование пользователя для будущего hostile combat

(user) Все hostile abilities, обычные атаки и projectile effects не должны вредить союзникам. Командная проверка и исключение `BedWarsShopVillager` требуются на каждом этапе: при обработке hit/trigger, при проверке владельца и origins projectile/ability, при построении target set, и непосредственно перед применением урона/эффекта. Фильтра только в финальном damage call недостаточно: allied или shop-villager сущность не должна запускать цепочку on-hit, менять mana/status или становиться origin дальнейшего эффекта. Root зафиксировал это как план следующего командного этапа; здесь код не менялся.

Никакие production Java, исходный сервер или datapack для этого исследования не менялись; runtime-проверки не выполнялись.
