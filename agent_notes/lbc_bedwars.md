# Bedwars LBC: запуск, генераторы и жизнь матча

Проверены все 27 функций из `bedwars/**`. Основные входы приходят из верхнего игрового runtime: `start_bedwars` запускает старт, общий секундный/тик-цикл вызывает генераторы и личные таймеры, а `death_inject_before_announce` передаёт смерть игрока в обработчик Bedwars. Внешние `swrg:*` вызовы отмечены как границы пакета, их реализация здесь не приписывается LBC.

## Запуск и карта

`game/start` сначала вызывает `swrg:game/start/config`, ставит пустой subtitle всем, выставляет `#gamestat=2`, даёт всем игрокам Instant Health на1 секунду с amplifier255, очищает отложенный `swrg:game/start/clear`, затем назначает через 60 тиков (3 секунды) `game/start2`. Всем игрокам ставится `have_bed`; каждый marker `bedwars_bed` запускает поиск цвета кровати в своей текущей позиции и присоединяет marker к команде 1–16. Функция подготавливает compound `lbc.math` и вызывает `start1 with storage lbc.math`, затем по SWRG-флагам выполняет выбор trinket, kit, LBC kit, levelup, skill и challenge; в конце отмечает `game_started=1`, вызывает внешний `lbc:start_after_all`, переводит всех в Survival, случайно обрабатывает игроков через `swrg:game/start/player_suffix_select`, включает mob/block/entity drops и скрывает bossbar таймера.

`game/start1` — макро-функция, подставляющая команду `teaminit`; она скрывает таймер, обрабатывает все `swrg.spawn` markers через `swrg_spawn_place_beacon`, подставляет макрокоманду `bedwars init` и каждому игроку ставит `team_number` по фактической команде (1–32). `check_bed_get_team`, вызванный **до start1**, выполняет 32 команды `attribute @s ... base reset` после цветового сопоставления. В штатном caller `@s` — marker кровати, поэтому игрокам эти сбросы не применяются; попытки сброса атрибутов marker не меняют базовые атрибуты игроков, а ошибки на неприменимых атрибутах зависят от выполнения конкретной команды. Spawn helper строит основание 3×3 из diamond block на высоте −3, ставит beacon на −2, цветное стекло на −1 и заполняет столбец до абсолютного Y=319 стеклом команды, заменяя только блоки тега `#solid`; цвета заданы явно для команд 1–16. `check_bed_get_team` сопоставляет блок кровати под marker только для 16 цветов и присоединяет marker к соответствующей команде. Вызов `start_player_tp` закомментирован в `start`. При ручном запуске helper уменьшает общий score players на 1, выбирает случайного игрока без have_bed и ставит current, вызывает setup для **всех** игроков с current; старые current здесь не снимаются. Пока players>=1, рекурсивно повторяет проход. Внутренний выбор вызывает отсутствующую в LBC-функциях `bedwars/game/setup_team_at_player`; поэтому автопересадка этим helper сейчас не подтверждается и при ручном вызове не выполнит отсутствующий вызов и выдаст ошибку команды; о зависании игры этот source не свидетельствует. Счётчик players локально не инициализируется — ручной вызов зависит от заранее заданного значения.

Через 60 тиков (3 секунды) `game/start2` включает урон от падения в Overworld, снимает все force-load chunks в измерениях `imprinted` и `white`, затем скрывает bossbar. `game/start/timer` пуст — сам по себе ничего не делает. `game/end1` подставляет макрокоманду `bedwars stop` и очищает назначение sidebar через `scoreboard objectives setdisplay sidebar` без названия objective.

## Кровати, победа и смерть

Периодические `game/tick_1s`, `tick_5s`, `tick_10s`, `tick_15s`, `tick_10t` вызываются внешними dispatcher-ами при `developer=0` и `gamemode=3..4` без gate по gamestat. Общий `game/tick` отдельно ограничен `gamestat=2..4`. Каждый `game/tick` считает `p` как сумму всех игроков с `have_bed` и всех игроков без этого тега, кроме spectator. Затем для команд 1–16 считает такое же число активных игроков своей команды; если число команды равно общему числу `p`, немедленно вызывает внешний `swrg:game/end/0_pre` и завершает этот проход. Это не проверка «осталась ровно одна команда»: команда, содержащая всех игроков, удовлетворяет равенству даже если все они уже потеряли кровать. При `p=0` первое сравнение также истинно; отдельного условия `p>=1` нет. После этих проверок каждый `bedwars_bed` marker, под которым больше нет блока из `#beds`, запускает `bed_broken`.

`bed_broken` по команде marker рассылает всем сообщение с translation key `<номер>_bed_broken`, всем участникам той же команды снимает `have_bed` и показывает им title `your_bed_broken`. Затем проигрывает всем звук смерти эндер-дракона с громкостью 100 в точке marker и убивает marker. Другого сохранения статуса кровати здесь нет: флагом для повторного возрождения служит `have_bed` у игроков.

Смерть игрока входит из `death_inject_before_announce` для gamemode score 3–4. `player/died` с `have_bed` показывает умершему `you_died` / `respawn_in_5s` и задаёт `respawn_timer=5`; фактическая длительность зависит от фазы секундного tick и потому не обязана быть ровно пять секунд от смерти; без кровати и если `swrg.leave` не положителен — показывает `you_died` / `no_bed`, без установки таймера. При положительном `swrg.leave` сообщение без кровати пропускается. Раз в секунду `tick_player_1s` принудительно задаёт `respawn_timer=0` spectator-игрокам без кровати, удаляет у игрока gravity modifier UUID `f8hw83fhfjd3`, запускает отсчёт у spectator с положительным timer, затем пытается выполнить `team leave @s` через `unless score @s[gamemode=spectator,tag=!have_bed] respawn_timer matches 1..`. **Команду покидают spectator без кровати**: им в начале уже поставлен timer=0, поэтому unless проходит. Survival-игроки и игроки с кроватью этим helper команду не покидают: отфильтрованный @s не найден, resolver score holder выбрасывает ошибку до проверки unless. Пустой selector в score-команде нельзя трактовать как условие false с последующим логическим отрицанием. Spectator с кроватью проходят положительный timer/respawn; если в этом же проходе они возродились, последняя проверка также не находит spectator @s и не снимает команду.

`tick_player_1s_for_respawn` уменьшает таймер на 1. На нуле он сразу вызывает `player/respawn` и заканчивает функцию; иначе обновляет title с оставшимся значением. Возрождение телепортирует игрока к первому `swrg.spawn` marker его команды 1–16, включает Survival, снимает tag `spectator`, очищает title/subtitle, весь инвентарь и все эффекты, добавляет 30 уровней опыта, затем повторно вызывает текущие kit/skill/trinket selectors по SWRG-score flags. Возврат не восстанавливает инвентарь или уровни из снимка: он очищает содержимое и выдаёт указанные 30 уровней с повторным выбором модулей.

## Генераторы ресурсов и периодические вызовы

Для каждого вида ресурсов `game/tick_*` запускает функцию на каждом marker соответствующего типа: copper каждые 10 тиков, gold каждые 10 секунд, diamond и nether star каждые 15 секунд, iron каждые 5 секунд. Генератор проверяет игроков не в spectator в радиусе 1.5 от позиции marker. Если хотя бы один такой игрок есть, команда `give @a[...]` выдаёт по одному предмету каждому подходящему игроку в радиусе и функция возвращает, не создавая item entity. Если рядом никого нет, создаёт один выпавший предмет в точке marker с Y+0.5; затем считает markers этого типа в радиусе 1 и убивает текущий marker при результате 2 или больше. Эта проверка дубликата достигается только когда рядом нет игрока: ветка выдачи предмета делает return раньше. Таким образом, при пустом генераторе ресурсы копятся как item entities, а дублирующие markers могут быть удалены; логика выполняется от marker, `@s` — конкретная точка генератора.

`game/tick_1s` раз в секунду заполняет воздухом вертикаль каждого `swrg.spawn` от его позиции до абсолютного Y=319 командой `fill` с `destroy` (ломаемые блоки могут выпадать), затем обслуживает каждого игрока. `tick_10s`, `tick_15s`, `tick_5s` и `tick_10t` только перебирают соответствующие markers и вызывают их генераторы. Сами функции не имеют расхода или индивидуального cooldown: частоту задаёт внешний tick dispatcher.

`idnumberget` — таблица соответствия команд 1–32 числам objective `team_number`; для неизвестной/отсутствующей команды никаких изменений нет. `check_bed_get_team` поддерживает только цветные кровати команд 1–16. Эти ограничения важны: числовой индекс команды не подтверждает существование bed/spawn marker для значений 17–32.

## Зависимости и точные границы

Макро-команды `teaminit`, `bedwars init`, `bedwars stop` передаются через storage/with; их реализации находятся на границе команд и описаны в `agent_notes/COMMAND_BRIDGES.md`. Здесь доказаны только факты подстановки и позиции вызовов. Остальные внешние контракты: `swrg:game/start/config`, `swrg:game/end/0_pre`, `swrg:game/player/kit`, selectors SWRG kit/skill/trinket и `lbc:start_after_all` здесь зафиксированы только как вызовы и условия; их внутреннее поведение покрывается другими пакетами. `swrg_spawn_place_beacon` непосредственно меняет блоки карты, но точные макрокоманды `teaminit/init/stop` задаются вызывающей storage-подстановкой и не раскрываются этим job. В текущем datapack отсутствует точная цель `lbc:bedwars/game/setup_team_at_player`, хотя `start_player_tp` содержит исполняемый вызов; отдельного исполняемого caller `start_player_tp` не найден, а вызов из `start` закомментирован.

## Команды и цвета

Для bed marker и стеклянного столба используется одна таблица (цвет tellraw местами иной):

| Команда | Цвет кровати и стекла |
|---|---|
| 1 | `light_blue` |
| 2 | `black` |
| 3 | `blue` |
| 4 | `cyan` |
| 5 | `purple` |
| 6 | `gray` |
| 7 | `green` |
| 8 | `magenta` |
| 9 | `brown` |
| 10 | `orange` |
| 11 | `light_gray` |
| 12 | `lime` |
| 13 | `pink` |
| 14 | `red` |
| 15 | `white` |
| 16 | `yellow` |

## Проверенные исходники

Координатор прочитал все 27 функций и сопоставил внешние dispatchers. Исходники сохранены без изменений.

- [bedwars/game/bed_broken.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/bedwars/game/bed_broken.mcfunction)
- [bedwars/game/end1.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/bedwars/game/end1.mcfunction)
- [bedwars/game/generator/copper.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/bedwars/game/generator/copper.mcfunction)
- [bedwars/game/generator/diamond.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/bedwars/game/generator/diamond.mcfunction)
- [bedwars/game/generator/gold.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/bedwars/game/generator/gold.mcfunction)
- [bedwars/game/generator/iron.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/bedwars/game/generator/iron.mcfunction)
- [bedwars/game/generator/star.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/bedwars/game/generator/star.mcfunction)
- [bedwars/game/idnumberget.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/bedwars/game/idnumberget.mcfunction)
- [bedwars/game/start/check_bed_get_team.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/bedwars/game/start/check_bed_get_team.mcfunction)
- [bedwars/game/start/swrg_spawn_place_beacon.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/bedwars/game/start/swrg_spawn_place_beacon.mcfunction)
- [bedwars/game/start/timer.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/bedwars/game/start/timer.mcfunction)
- [bedwars/game/start.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/bedwars/game/start.mcfunction)
- [bedwars/game/start1.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/bedwars/game/start1.mcfunction)
- [bedwars/game/start2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/bedwars/game/start2.mcfunction)
- [bedwars/game/start_player_tp.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/bedwars/game/start_player_tp.mcfunction)
- [bedwars/game/tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/bedwars/game/tick.mcfunction)
- [bedwars/game/tick_10s.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/bedwars/game/tick_10s.mcfunction)
- [bedwars/game/tick_10t.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/bedwars/game/tick_10t.mcfunction)
- [bedwars/game/tick_15s.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/bedwars/game/tick_15s.mcfunction)
- [bedwars/game/tick_1s.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/bedwars/game/tick_1s.mcfunction)
- [bedwars/game/tick_5s.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/bedwars/game/tick_5s.mcfunction)
- [bedwars/player/died.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/bedwars/player/died.mcfunction)
- [bedwars/player/died_with_bed.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/bedwars/player/died_with_bed.mcfunction)
- [bedwars/player/died_without_bed.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/bedwars/player/died_without_bed.mcfunction)
- [bedwars/player/respawn.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/bedwars/player/respawn.mcfunction)
- [bedwars/tick_player_1s.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/bedwars/tick_player_1s.mcfunction)
- [bedwars/tick_player_1s_for_respawn.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/bedwars/tick_player_1s_for_respawn.mcfunction)
