# Источник: rocket boots, kit №22

Сверены исходный Paper-плагин, таблица LBC, связанные крафтовые функции, модификатор предмета и ресурс-пак. Это контракт исходников; фактический запуск datapack здесь не проверялся.

## Живая регистрация и активация

`RocketArmor` создаётся и регистрируется в `SigmaWarsMain`; значит обработчики класса не являются мёртвым кодом. Его `PlayerArmorChangeEvent`, `PlayerJoinEvent`, `PlayerDeathEvent` и `EntityToggleGlideEvent` обрабатывают предмет и состояние. Отдельно `SwrgNativeService` сопоставляет kit ID `22` с `lbc:kit/explosive_feets`, а datapack selector для score `lbc.kit=22` вызывает ту же loot table. Условия фактического запуска selector зависят от включённого legacy runtime; запуск сервера этой заметкой не подтверждается.

Активация — начало glide через `EntityToggleGlideEvent` при надетых ракетных ботинках и наличии заряда. Обычно доступность vanilla glide на сапогах обеспечивает компонент `glider`, который плагин ставит, пока зарядов больше нуля, и снимает при нуле. Проверка сапог плагином принимает вложенный PDC `item.type` или `item.additionals` со значениями `rocket_armor`, `rocketarmor` или `rocket`, а также специальные контейнеры `glider`; проверяются namespaces самого плагина, `lws` и `lucky_wars`. Флаг datapack `explosive_feets` сам по себе в эту проверку не входит, но loot table выдаёт также распознаваемый PDC.

При старте glide без нагрудной elytra обработчик отменяет toggle и вызывает `setGliding(false)`, но не делает `return`: при ракетных сапогах он всё равно списывает заряд и задаёт импульс. То есть glide-toggle служит входом способности даже при отменённом glide; это необычная ветка, отдельно не проверенная в игре. С установленной elytra тот же обработчик также срабатывает. Исключены миры `minecraft:nexus` и `minecraft:imprinted` и наличие MARKER с тегом `stopper` в кубе радиуса 20. При этих запретах уже начатый glide без elytra всё равно отменяется до выхода из обработчика.

Обработчик активации не проверяет game mode или observer state. Survival/adventure проверяются только в тиковом обновлении зарядов; отдельного `game.isObserver`-gate в этом классе нет. Учитывай требование проекта различать наблюдателя и активного игрока из `agent_notes/AGENTS.md` при переносе.

## Импульс, заряды и интерфейс

Импульс тратит один из трёх зарядов при каждом glide-start. К текущей скорости прибавляется направление взгляда (`velocity += direction`). Если вертикальная скорость уже положительная, к ней добавляется `0.5`; иначе, если взгляд направлен вниз, вертикальная скорость устанавливается в `0.5`. При взгляде горизонтально/вверх и неположительной текущей вертикальной скорости отдельная вертикальная поправка не добавляется.

Перезарядка задаёт 300 тиков ожидания, но фактическое восстановление занимает 301 тик: цикл сначала уменьшает `cooldown` до нуля, а восстановление находится в `else if` и проходит следующим вызовом. Таймер запускается при расходе, если он ещё не идёт, поэтому серия трат не перезапускает ожидание. При нехватке нескольких зарядов каждый следующий интервал также занимает 301 тик. Состояние хранится только в памяти по UUID, инициализируется тремя зарядами при первом надевании/входе, сбрасывается при снятии ботинок и смерти; выхода игрока отдельный обработчик не обслуживает, сохранения между рестартами нет. Игроки вне survival/adventure пропускаются циклом состояния.

На каждом тике состояния `glider` синхронизируется с наличием хотя бы одного заряда. Успешное срабатывание даёт `EXPLOSION` (1), `FLAME` (10, разброс 0.5 по осям) и `ENTITY_GENERIC_EXPLODE` (громкость 1.5, pitch 2). У обоих вызовов частиц default extra равен 1.0: подтверждено через `javap -c -p org.bukkit.World` в локальном `libraries/io/papermc/paper/paper-api/1.21.11-R0.1-SNAPSHOT/paper-api-1.21.11-R0.1-SNAPSHOT.jar` старого сервера; цепочка interface overloads подставляет `dconst_1`. Действующие побочные эффекты из кода: заряд и импульс; дополнительные баффы не выдаются.

Плагинский actionbar зарегистрирован через `CoreUiElements` и показывает строку с glyph `ROCKET_JUMPS`, текущими зарядами и округлённым вверх числом секунд до следующего заряда. Строка видна при datapack-флаге `explosive_feets` на надетых ботинках либо когда `RocketArmor.isWearing` распознаёт PDC.

## Крафт и предметный контракт

Основная loot table выдаёт `chainmail_boots`: `item_model=lbc:armors/rocket_boots`, `equippable.slot=feet`, `asset_id=lbc:rocket`, `max_damage=280`, броня +2 и `glider`. Name — перевод `lbckit.21` белым, `italic:false`. Исходный lore состоит из трёх серых строк `lbckit.21.lore.2`, `.3`, `.4`, все с `italic:false`. В resource pack значения ключей: RU «Ракетные сапоги»; `> Ракетные сапоги`; « * Прыжок в воздухе позволяет дэшнуться вперёд»; « * Максимум 3 дэша, перезарядка по 15 секунд»; « * Можно объединить с любыми лаки-ботинками для передачи эффекта». EN: “Rocket Boots”; “- Rocket Boots”; “ * Jump in midair to perform double jump”; “ * Max 3 dashes, 15 seconds recharge”; “ * Combine with any other boots (Lucky boots too!) to transfer the double jump ability”. Loot table использует lore 2–4, а craft modifier добавляет 1–3.

Ground path не является native recipe: раз в секунду `tick1s` выбирает выброшенные craftusable предметы рядом с любым другим item entity (кроме Nexus/Imprinted), а `craft_ground` проверяет сочетание rocket marker `explosive_feets:1` и второго item с `transformable:1,boots:1` в радиусе 1. После этого `rocket_boots_add_ability` меняет слот 0 второго предмета; исходные rocket boots entity удаляются. Inventory path запускается из `players_tick` каждый player tick, когда в crafting grid есть craftusable item; отдельная функция проверяет любые slots на те же два маркера. Она уменьшает количество rocket ingredient по одному во всех слотах 0–3, затем модифицирует первые найденные трансформируемые ботинки. Поэтому несколько rocket ingredients могут быть израсходованы за один проход, хотя целевой предмет модифицируется один.

В Minecraft 26.3 `SetCustomDataFunction` merge-ит новые custom fields с исходными. Поэтому после modifier target сохраняет `transformable:1,boots:1` (и возможный `craftusable:1`) вместе с rocket marker. Inventory функция вызывается повторно каждый тик, пока craftusable item остаётся в grid; без дополнительного gate она может снова модифицировать уже ракетные сапоги, дописывать lore и расходовать donor stack по одному предмету за тик. Ground ветка удаляет donor entity целиком после срабатывания. Это источник подтверждённых багов повторного наложения/расхода.

## Пользовательское уточнение переноса (user)

Перед исправлением был задан вопрос: «Исправить перенос: одна пара-донор на одну пару обычных трансформируемых ботинок, без повторного улучшения?» Пользователь ответил: «Исправить эти баги (рекомендую)».

Это целевое требование нового переноса: одна пара-донор усиливает одну пару обычных трансформируемых ботинок, а rocket boots исключаются как получатель. Новый порт использует этот gate и native item modifier; оба способа переноса и запрет повторного улучшения прошли живые сценарии. Подробности проверки — [ROCKET_BOOTS_PORT.md](ROCKET_BOOTS_PORT.md); исходный источник выше описывает старую логику.

В исходнике фактическое восстановление занимает 301 тик: timer убывает до нуля, а заряд добавляется веткой `else if` на следующем тике. Это выявленный контракт кода, не пользовательское уточнение. Runtime-проверка переноса отдельно подтвердила границы 300/301 тик.

`add_lore_rocket` дописывает строки lore 1–3 зелёным/серым, `italic:false`, записывает PDC additionals `rocket_armor` и `explosive_feets:1`, и задаёт `glider`. Модификатор не задаёт имя, модель, броню или durability — их компоненты целевых сапог остаются исходными. Ограничение по datapack-коду — только `transformable:1,boots:1`, а не явная проверка lucky boots; это шире, чем формулировка lore «любыми лаки-ботинками».

## Нативный выходной слот крафта (user)

> кстати крафт в инвентаре - предмет по прежнему просто дропается? можно же вроде сделать чтоб именно как крафт работал - чтоб результат был в клетке крафта? И чтоб в верстаке тоже могло работать

Новый порт заменяет автоматическое изменение предмета в сетке нативным
рецептом с выходным слотом в инвентаре 2×2 и верстаке 3×3. Это изменение
относительно исходного datapack: ингредиенты расходуются только при взятии
результата. Требования одного донора и запрета повторного улучшения сохраняются.
Перенос между выброшенными предметами остаётся отдельным способом.
Актуальное поведение и проверки — [ROCKET_BOOTS_PORT.md](ROCKET_BOOTS_PORT.md).

## Регистрационные несоответствия и мёртвые остатки

- Kit slot/ID — 22; selector и каталог связывают его с `lbc:kit/explosive_feets`, а доступ требует `lbc:true_advancements/legends/academy/exasperato`. GUI loot-table ID `lbc:gui/page/9000/21` — идентификатор отдельной таблицы отображения предмета; его нельзя трактовать как slot набора. Модель `lbc:armors/rocket_boots` и ключи `lbckit.21` также принадлежат отдельным пространствам идентификаторов.
- Старые dummy objectives `rocket_armor_jumps` и `rocket_armor_jump_cd` ещё объявляются/удаляются, а `ui/get_data.mcfunction` читает их для строки boots. Других записей/обновлений этих scores в datapack не найдено; текущий Java UI берёт значения напрямую из `RocketArmor`, так что score-ветка — оставшийся datapack-контракт, а не источник живых зарядов плагина.
- Loot table лежит под `world/datapacksdisabled/LBC`; она присутствует в исходном наборе файлов, но имя каталога не подтверждает, что datapack включён в конкретном мире.

## Источники

- [RocketArmor.java](E:/servers_mew/cringe1_21_4_skywars/plugins/luckywars/src/main/java/me/luckywars/item/RocketArmor.java)
- [SigmaWarsMain.java](E:/servers_mew/cringe1_21_4_skywars/plugins/luckywars/src/main/java/me/luckywars/SigmaWarsMain.java), [SwrgNativeService.java](E:/servers_mew/cringe1_21_4_skywars/plugins/luckywars/src/main/java/me/luckywars/swrg/SwrgNativeService.java)
- [explosive_feets.json](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/kit/explosive_feets.json)
- [craft_ground.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft_ground.mcfunction), [rocket_boots_add_ability.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/rocket_boots_add_ability.mcfunction), [rocket_boots_add_ability_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/rocket_boots_add_ability_inv.mcfunction), [inventory_craft_check.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/inventory_craft_check.mcfunction)
- [add_lore_rocket.json](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/add_lore_rocket.json), [CoreUiElements.java](E:/servers_mew/cringe1_21_4_skywars/plugins/luckywars/src/main/java/me/luckywars/ui/CoreUiElements.java)
- Resource pack, read-only: `all_in_one_1.21.8/assets/minecraft/lang/{ru_ru,en_us}.json`, keys `lbckit.21` and `lbckit.21.lore.1`–`.4`.
