# Наблюдения по исходникам и точности описаний

## Пустые loot tables

Проверка всех 2 874 JSON нашла два файла длиной ровно 0 байт:

| Исходник | Фактическое содержимое и предел вывода |
|---|---|
| [lbc:reroll_9999/material_block](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/reroll_9999/material_block.json) | Содержимого нет. Нельзя определить ни выдаваемый предмет, ни вероятность, ни модификаторы. Пустой файл не является корректной JSON loot table. |
| [swrg:locked_suffix_tournament](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/loot_table/locked_suffix_tournament.json) | Содержимого нет. Название намекает на турнирный суффикс, но не доказывает его выдачу или параметры. Пустой файл не является корректной JSON loot table. |

Это установленное состояние исходников. При анализе вызывающих функций нужно
отдельно показывать, что ожидаемый ресурс отсутствует; ничего дописывать за авторов
не следует. Исходники во время исследования не исправлялись.

Повторная проверка: `python agent_notes/tools/build_item_index.py`.
Полный машинный результат: `inventory/json_parse_report.json`.

## Чтение времени и контекста команды

`schedule function ... 1s` означает одну секунду. В SNBT суффикс числа `s`
означает тип **short**: например, `fuse:100s` — значение 100 тиков, то есть
5 секунд при 20 TPS. В описаниях предметов и TNT эти формы не смешиваются.
Типы чисел SNBT подтверждаются [примечаниями Mojang к 25w09a](https://www.minecraft.net/en-us/article/minecraft-snapshot-25w09a).

`execute as @a` меняет исполнителя, но сам по себе не переносит позицию исполнения
к игроку. Для этого нужен `at @s` или другой явный переход контекста. Поэтому
фразы «у каждого игрока» проверяются по полной цепочке вызовов, а не только по `as`.

Запланированная функция может выполнить один последний вызов после смены стадии.
Если её повторное планирование защищено проверкой стадии, это ещё не защищает
безусловные команды в начале самой функции. Документируем оба факта отдельно.

## Геометрия и масштаб чисел

`dx=0,dy=0,dz=0` ограничивает выбор кубом 1×1×1 у позиции команды.
Дополнительное `distance=..15` пересекается с этим объёмом и не превращает его
в сферу радиусом 15. Проверка объёма учитывает hitbox сущности; точка её ног
сама по себе не является единственным критерием попадания. Построение AABB
проверено по `EntitySelectorParser.createAabb` из локального Minecraft 26.3:
положительная верхняя граница каждой оси равна delta+1.

При связке `data get ... 1000` и `execute store ... double -0.001`
действует общий множитель −1 с округлением промежуточного целого.
Именно так energy shield обращает Motion снаряда, сохраняя примерно его скорость.
Масштаб записи отдельно не равен итоговому множителю исходного значения.

Команды `damage` описываются отдельно от гарантированного изменения Health:
бронирование, эффекты, теги типа урона и cooldown могут менять итог.
Например SLR head-helper исполняет `damage 20`, а затем общий проход всё равно
исполняет `damage 10`; запись команд не доказывает снятие 30 HP.

## Целые числа и пустой score selector

`data get` числового NBT с масштабом возвращает **floor(value × scale)**,
то есть округляет вниз, в том числе отрицательные числа. Scoreboard `/=`
использует **floorDiv**: −5 / 2 = −3. Это отличается от преобразования
результата команды при `execute store ... int scale`; каждое преобразование
нужно читать отдельно. Доказательства для установленного Minecraft 26.3:
[DataCommands](inventory/minecraft_data_26_3_bytecode.txt) и
[OperationArgument](inventory/minecraft_operation_26_3_bytecode.txt).
Для отрицательной маны и pitch это может менять результат на единицу.

В `execute unless score @s[условие] objective matches ...` пустой entity selector
вызывает **NO_ENTITIES_FOUND** при разрешении score holder. Это ошибка команды
до проверки unless; `run` не выполняется. Нельзя считать, что отсутствие игрока
в selector автоматически обращается в true. Так, в BedWars последний team leave
относится к spectator без кровати, а не к survival или игрокам с кроватью.
Проверена реализация
[ScoreHolderArgument.SelectorResult](inventory/minecraft_score_holder_26_3_bytecode.txt).

`execute facing entity` допускает множество targets и разветвляет командный
контекст. Один `sort=nearest` **без limit** сортирует цели, но не оставляет
единственную цель. Пример — Stalin-вход AK47; ветви при этом разделяют
scoreboard executor, а не получают независимый Distance.
Регистрация plural EntityArgument и fork сохранена в
[ExecuteCommand](inventory/minecraft_execute_26_3_bytecode.txt).

Терминальная команда `execute store result score ... if entity @a[...]`
**без run** сохраняет число найденных сущностей. В ExecuteCommand numeric handler
получает Collection.size и возвращает это число; отсутствие целей даёт результат
0 через failure callback. `store success` сохраняет 0/1. Эти два варианта нельзя
смешивать: BedWars считает игроков/генераторы, а Envy сравнивает число всех других
игроков с числом других игроков с have_mom, а не два признака наличия.

Терминальный `execute store result ... if data ... storage[]` также возвращает
**число совпавших NBT-элементов**, а не 0/1. `checkMatchingData` вызывает
`NbtPath.countMatching`, positive numeric handler возвращает это число.
Поэтому восстановление Pumpkin Curse считает все записи массива и перебирает
индексы 0..N−1; ложный вывод «возвращается только первый предмет» основан на
смешении result/success. Когда список пуст, отдельно проверяется безусловный
первый вызов рекурсии. Реализация: [ExecuteCommand](inventory/minecraft_execute_26_3_bytecode.txt).

## Вектор направления и UUID снаряда

`execute positioned <pos>` после вычисления позиции устанавливает anchor FEET.
Цепочка `positioned 0 0 0 positioned ^ ^ ^6` создаёт точку примерно в шести
блоках от начала координат по текущему направлению взгляда. Копирование её Pos
в Motion — способ получить вектор скорости; абсолютные координаты владельца
в этот вектор не входят. Это подтверждено регистрацией positioned в
[ExecuteCommand 26.3](inventory/minecraft_execute_26_3_bytecode.txt).

Команда `data modify entity <снаряд> UUID set from entity @s` без NBT path
копирует весь compound исполнителя в поле UUID, а не его UUID и не Owner.
Кроме того, EntityDataAccessor сохраняет исходный UUID сущности перед загрузкой
изменённого NBT и восстанавливает его после загрузки. Поэтому такую команду
нельзя описывать как передачу владельца или UUID снаряду; scoreboard ID,
team и Owner нужно проверять отдельно. Доказательство:
[EntityDataAccessor 26.3](inventory/minecraft_entity_data_26_3_bytecode.txt).

## Проверка чтения структур

Отдельный проход всех 1 456 NBT полностью прочитал каждый распакованный файл,
проверил строгую декодировку строк UTF-8, SHA исходного снимка, размер структуры,
число блоков/сущностей и допустимость всех индексов палитры. Ошибок нет.
В 37 byte-array тегах нет значений ≥128: различие signed/unsigned byte-array
в текущем парсере не меняет фактические данные этих структур. Результат:
[maps_parser_audit.json](inventory/maps_parser_audit.json). Это проверка
декодирования и каталога; поведение вложенных команд и вызывающих функций
проверяется отдельно.

## Размер стака через attribute amount

SkyWars сундуки используют `minecraft:set_attributes` как временный контейнер
числа: `amount` потом копируется в item `count`. Это отличается от обычного
loot `set_count`. В локальном 26.3 SetAttributesFunction получает **float**,
а uniform вычисляет `min + nextFloat() × (max−min)`. При чтении ItemStack
целочисленный count использует Number.intValue, то есть отбрасывает дробную часть.
Поэтому amount с min=6/max=8 не означает три равновероятных стака 6/7/8:
обычные исходы — 6 и 7; float-округление возле верхней границы может дать 8.
Диапазоны amount в приложении сохраняются буквально; нельзя превращать их
в равномерный целочисленный диапазон. Источники реализации:
[SetAttributesFunction](inventory/minecraft_set_attributes_26_3_bytecode.txt),
[uniform float](inventory/minecraft_uniform_float_26_3_bytecode.txt),
[Mth.nextFloat](inventory/minecraft_mth_26_3_bytecode.txt),
[ItemStack count](inventory/minecraft_itemstack_26_3_bytecode.txt),
[NbtOps](inventory/minecraft_nbt_ops_26_3_bytecode.txt) и
[целочисленный codec](inventory/minecraft_codec_int_bytecode.txt).

## Удаление исполнителя и продолжение функции

`kill @s` не прекращает mcfunction. Для обычного `@s` parser не добавляет
проверку alive; EntitySelector берёт объект из CommandSourceStack, а не снова
ищет его в мире. Поэтому следующая NBT/tag-команда может выбрать даже уже
удалённого исполнителя. `return`, изменение данных и другие явные условия
проверяются отдельно. Нельзя автоматически обрывать drops-dispatch или
reduce_count после kill. Первичная реализация26.3:
[EntitySelector](inventory/minecraft_entity_selector_26_3_bytecode.txt) и
[EntitySelectorParser](inventory/minecraft_entity_selector_parser_26_3_bytecode.txt).

## Подтверждённые особенности интерфейса и trinkets

- Challenge slot0 устанавливает1; следующая проверка `unless ...1` уже ложна
  и не сбрасывает challenge_score. Выключение выполняет slot24.
- Молот и текст крови запускаются advancement смерти игрока: проверяется
  kill_effect убийцы, reward выполняется как жертва. Отсутствие function-call
  не означает неиспользуемый эффект.
- `false_tumba` перемещает игрока, но ставит bookshelf/barrel в прежней
  позиции контекста команды. `spreadplayers` задаёт квадрат X/Z, не круг.
- Mana Gem прибавляет45+15n. Multiplier2 задаёт0.1×floor(20×amount), поэтому
  точного удвоения дробных/отрицательных attribute amounts ожидать нельзя.
- Acid trinket на2..13 копиях пишет вероятность в storage tmp, а macro читает
  tmpd. В этой цепочке рассчитанная вероятность не используется.
- Spell ID189 работает только у wand; snowball-dispatcher его не содержит.
  Читаемый snowball выбирается без distance/libspell selector-фильтра,
  а удаляемый — отдельным selector с ними.
- Общий Eyelaser kill ставит Distance201; этого недостаточно для завершения
  Mellstroy-варианта с пределом684. Solid-блоки могут продлевать его рекурсию.

Команды и исправленные человеческие описания связаны в
[проверке интерфейса/арканумов](review_kit_arcana_root.md),
[аудите chance и spell-маршрутов](inventory/arcana_routes_chances_audit.json).

## Attribute get: отдельное правило округления

`attribute ... get/base get S` возвращает `trunc(value × S)` к нулю. Это отличается от `data get`, где числовой NBT использует floor. Например, −1,7 при S=1 даёт −1 у attribute get и −2 у data get. Проверено по `dmul; d2i` в [AttributeCommand](inventory/AttributeCommand.javap.txt) локального 26.3 jar. В описании LBC attributes исправлена прежняя общая формулировка floor; это проверка семантики команды, а не запуск датапака в 26.3.

## Frozen Heart: тики передаются как секунды

`frozen_heart_2` читает `active_effects[{id:"minecraft:slowness"}].duration`
с коэффициентом 1, а `frozen_heart_3` подставляет число прямо в `/effect give`.
Нормальная положительная длительность Resistance II поэтому увеличивается
в 20 раз: NBT 20 тиков превращается в 20 секунд; затем Slowness снимается.
Это фактическая ошибка единиц в датапаке, а не исправленное поведение мода.
Источники: [чтение и очистка](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/frozen_heart_2.mcfunction),
[макро-эффект](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/frozen_heart_3.mcfunction),
[условия advancement](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/slowness.json).
