# LBC: диспетчер предметов и запуск ядра

Источник: `world/datapacksdisabled/LBC/data/lbc/function`. Разобраны цепочка `tick` → `tick_not_timestopped` → `items_tick` → `items_tick_tag`/`items_tick_act`, диспетчер `drops/specialdrops`; полный lifecycle вынесен в [lbc_runtime.md](lbc_runtime.md). Это заметка по исполнению datapack, а не по содержимому lore.

## Обработка выброшенных предметов

В обычной ветке `lbc:tick_not_timestopped` каждый выброшенный `minecraft:item` в измерениях, кроме `minecraft:nexus` и `minecraft:imprinted`, получает `lbc:items_tick`. При глобальном `time_stopped >= 1` эта ветка не вызывается: `lbc:tick` переходит в `lbc:time_stopped_tick`. Поэтому выброшенные предметы во время остановки времени не проходят обычную логику этого диспетчера.

`items_tick_tag` срабатывает только если у предмета ещё нет entity-тега `tagged`, после чего выставляет его. По `Item.components."minecraft:custom_data"` выставляются рабочие метки: `drop`, `fast`, `fast_old`, `transformable`, `penis_bomb`, `copypaster`, `multiply=2/3/4`; `Glowing:1b` тоже делает предмет `working`. Все перечисленные кроме специальных `academy_token` и `bad_apple_imba` получают `working`; `academy_token` отдельно получает одноимённый тег. Тег `malevich` и обработчик его подбора закомментированы. Если включён общий флаг `#lbcskill swrg.math = 1`, вызывается `items_tick_tag_2`: `custom_data.imba:1` создаёт визуальные эффекты из `items_tick_tag_3` (пыль на высоте 20, 200 частиц, звук пчелы игрокам со счётом `lbc.skill=3`).

На последующих тиках `working` запускает `items_tick_act`: `drop` вызывает `drops/specialdrops`; `fast`/`fast_old` вызывают две версии `other/transformation`; `transformable` — `other/transformpre`; `Glowing` испускает частицу `end_rod`; `penis_bomb` при мобе (кроме игрока) в радиусе 1.2 и без `no_penis` зовёт `workingitems/penis_mob`; `multiply_item` при соседнем предмете без этого entity-тега вызывает `swrg_kit_integration/items/multiply_select`; токен магической академии при predicate `lbc:on_ground` вызывает `drops/magic_academy_token`; плохое яблоко Imba постоянно направляется в `other/imba/bad_apple/item_tick`. Отдельно `items_tick` вызывает `items_copypaster`, когда у предмета есть тег `copypaster` и в радиусе 1 есть предмет без `custom_data.copypaster:1`.

Таким образом, большая часть рабочих меток является только входом в функции, которые лежат в исключённых из этого участка `other/**`, `workingitems/**` или `swrg_kit_integration/**`. Сами конечные эффекты для них здесь намеренно не заявлены. Условие «один раз помечен `tagged`» означает, что изменение `custom_data` после первого прохода не пересканируется данным обработчиком.

## Предметы, направляемые `drops/specialdrops`

`items_tick_act` передаёт сюда только entity-предметы с тегом `drop`. `specialdrops` проверяет `custom_data` по ключам (в основном строго значение `1`) и исполняет подходящие строки. Предмет не удаляется безусловно: в конце удаляется только `@s` с `Item.count=1`; для стака выполняется вычитание ровно одной единицы (`store result ... 0.99999`). Следовательно, за один проход потребляется по одной штуке, а стаки остаются для следующих тиков. Если предмет одновременно имеет несколько распознаваемых ключей, несколько эффектов могут сработать в одном вызове до расходования единицы.

| `custom_data` | Исполняемая ветка / видимый эффект на входе |
|---|---|
| `creepers`, `creepers_old` | `drops/creepers`, `drops/creepers_old` |
| `bastion`, `end_city`, `treasure`, `village`, `bonus_chest` | Ставит сундук с соответствующей ванильной loot table: Bastion, End City, buried treasure, village weaponsmith, bonus chest |
| `explode` | `drops/tnts` |
| `korrosion`, `korrosion_old` | Две версии `drops/corrosion*` |
| `zombietnt`, `sethome` | `drops/zombietnt`, `drops/sethome` |
| `lightning`, `lightning_old` | `drops/fallingblock`, `drops/fallingblock_old` |
| `infinity_spawn`, `forest` | `drops/infinity`, `drops/forest` |
| `sauvojen_tuntija` | `drops/sauvojen_tuntija` |
| `yama`, `yama_old` | `drops/hole`, `drops/hole_old`, привязанные к ближайшему не-спектатору |
| `lookupanvil`, `lookupfireball`, `lookuppotions`, `lookuplava` | Клетки: `anvilcage`, `tntcage`, `potioncage`, `lavacage`, с исполнителем ближайшим не-спектатором |
| `evilluckyblock`, `obsidiantrap` | Lucky Block-зомби; обсидиановая ловушка (вторая ветка привязана к ближайшему не-спектатору) |
| `luckyspawner`, `luckyspawner_old` | Устанавливают спавнер маркеров `futureluckyblock`/`rc_1sec`; параметры версий различаются (новый: диапазон 5, 80–100 тиков, игрок 50; старый: диапазон 3, 100–101, игрок 100) |
| `skrimmer`, `skrimmer_old` | Вызовы соответствующих версий |
| `droppertnt`, `droppertnt_old` | Большая частица взрыва и невидимая летучая мышь со спавнер-вагоном, который производит TNT с пассажиром-свиньёй; варианты отличаются лишь рядом тегов/свойств призываемых мобов |
| `badspawner`, `badspawner_old` | Ставят спавнер вредных splash-зелий; основная версия содержит набор равновероятных зелий (урон, замедление IV, тошнота, слепота, голод, слабость, усталость, яд, замедленное падение, урон, левитация); требуется дальнейшая проверка полного списка/старой версии |
| `library`, `lbh`, `clone`, `clone_old` | Книжная постройка, `luckyroom`, клонирование структуры в версии `falled`/`falled_old`; для первых трёх выбирается ближайший не-спектатор |
| `applepig`, `portall`, `bees`, `magicbean`, `bullets` | Lucky Pig, случайный портал, пчелиный улей, маркер магического боба, shulker bullet |
| `vex`, `vex_old`, `yameteluckyblock` | Vex и отдельная старая ветка; вызов эффекта Yama |
| `opanvil`, `opanvil_old` | Вызовы версий снятия блоков сверху |
| `aimbob`, `slab`, `slab_old`, `skeleton`, `skeleton_old`, `bob`, `bob_old` | Призывают мобов; непосредственно в диспетчере заданы снаряжение/атрибуты. Для старых вариантов дроп предметов заметно отличается, для slab новый урон оружия 6 против 3 у старого |
| `greedcurse`, `pizanski`, `ultraponos`, `levitation`, `stalin`, `mom` | Зовут соответствующие эффекты; `levitation` даёт ближайшему не-спектатору левитацию на 3 сек. уровня 127 |
| `transformator`, `otchim`, `magic_academy`, `helicopter_boss`, `muravei`, `acidboom`, `grimoire_dungeon` | Превращение/мобы/постройка/босс: отдельные функции `transformator`, `otchim`, `magic_academy`, `helicopter_boss_spawn`, `muravei`, `acid_boom`, `shadow_citadel` |
| `shahid` | Зомби-младенец с пассажиром TNT (запал 200 тиков) |
| `adv_check_lbc` | Выдаёт advancement `lbc:true_advancements/pickaxes/silk` ближайшему игроку в 7 блоках, держащему предмет `custom_data.silk:1` |
| `tmt`, `crocodildo_boss`, `antigravity`, `death_from_lust` | Соответственно процедурная генерация предмета, спавн босса, начало антигравитации, эффект lust |

Эта таблица — карта диспетчера и не заменяет разбор дочерних функций. В частности, ниже ещё требуется проследить вызовы, изменяющие блоки/лут/сущности, scheduler и последующие tick-функции. Ветка `specialdrops` не является единственным путём активации предметов: часть механик запускается напрямую из `tick1s` и иных игровых функций.

## Загрузка: прочитанная часть `load`

`load` создаёт многочисленные scoreboard objectives для предметных способностей, кулдаунов, здоровья, маны, raycaster-ов и состояний; начальные числовые константы и повторные запуски objectives требуют аудита на совместимость. Он выполняет `recipe give @a *`, задаёт значения `cfg_lucky_modifier` при выходе за 0..100, выбирает языковой init через `#lang swrg.math`, включает forceload, ставит несколько жёлтых шалкеров в координате 10241024, запускает ряд tick-функций и вызывает сброс gamerules в четырёх измерениях. Создаются также objectives интеграции `swrg.hp_display`, `swrg.hp`, `swrg.death`, `swrg.roll`, `swrg.roll_timer`, `swrg.lives`, `swrg.fall_damage`, а также `swrg`-GUI holder. Точные внешние предпосылки: objective `swrg.math`, его fake-player holders `#lang`, `#gamemode`, `#developer`, измерения `nexus`/`imprinted` и функции `lbc:swrg_kit_integration/**`.

Полный разбор load, objectives, буферов, периодических циклов, timestop и reset находится в [lbc_runtime.md](lbc_runtime.md); он принят по отдельному аудиту. Здесь оставлен краткий вход в диспетчер предметов.

## Где искать конечное действие

- Тики, мана, смерть/выход и cleanup: [runtime](lbc_runtime.md), [mana](lbc_mana.md).
- Дропы, постройки, ловушки и TMT: [drops](lbc_drops.md).
- Преобразование/крафт предметов: [other main](lbc_other_main.md), [consumables/craft](lbc_other_consumables_craft.md).
- Imba-предметы и артефакты: [other imba](lbc_other_imba.md), [workingitems other](lbc_workingitems_other.md).
- Multiply: [kit interface](lbc_kit_interface.md); способности/выдача: [kit game](lbc_kit_game.md).
- Все входные JSON: [advancements](lbc_advancements.md), [loot](lbc_loot_definitions.md), [predicates/modifiers](lbc_predicates_modifiers.md), [registry](lbc_registry_support.md).

Это обзор маршрутов, а не отдельный незавершённый участок. Приёмка конкретных
документов отражена в [RESEARCH_PROGRESS.md](RESEARCH_PROGRESS.md).
Реально отсутствующие ресурсы и внешние команды перечислены в
[RESOURCE_BOUNDARIES.md](RESOURCE_BOUNDARIES.md) и [COMMAND_BRIDGES.md](COMMAND_BRIDGES.md).
