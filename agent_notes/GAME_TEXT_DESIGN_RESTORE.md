# Восстановление дизайна текстов игрового цикла

Сверка выполнена 8 октября 2026 между текущими
[`GameController.java`](../src/main/java/me/skywars2/game/GameController.java) и
[`MatchEvents.java`](../src/main/java/me/skywars2/game/MatchEvents.java), исходными
`swrg:lang` RU/EN и функциями datapack `swrg:game/start/timer`,
`swrg:game/start/1`, `swrg:game/tick1s`, `swrg:game/stage1/event1..8`,
`swrg:game/stage2/event1..7`, `swrg:game/player/kill` и `swrg:game/end/0`.
Старый сервер и пользовательский ресурс-пак читались без изменений.

## Восстановлено

- Объявления событий 1-й и 2-й стадий остаются обычным чатом через прежние
  `swrg.title.stage1.eventN` и `swrg.title.stage2.eventN`. Переводы из `swrg:lang`
  уже доступны по тем же ключам в ресурспаке; сохраняются прежние зелёный/красный
  цвет и исходное наследование остальных свойств компонентов. Возвращены звуки:
  `minecraft:item.shield.break` (ambient, стадия 1) и
  `minecraft:entity.ender_dragon.ambient` (player, стадия 2).
- Обратный отсчёт показывается пустым основным title и зелёным subtitle с
  числами секунд, как в `game/start/timer.mcfunction`; сервер не задаёт отдельные
  title timings, поэтому клиент использует штатные. Временная bossbar при
  отсчёте скрыта.
- Bossbar использует `swrg.title.gametimer`: gold-текст и скобки, light-purple
  число; EN `Game event in [`, RU `Игровое событие через [`. Сохраняется исходная
  схема: зелёный таймер до первого события, красный до второго, скрытие после
  второй стадии либо при отсутствии следующего события. `swrg.title.gametimerb`
  (`Игра длится [` для RU) определён только в RU lang и в исходных функциях игры
  не используется; EN ключа и живого вызова не найдено.
- Ничья сохраняет `swrg.title.draw` и зелёный цвет. Победа использует исходную
  компонентную структуру `swrg.title.player_win` + имя победителя (white, bold) +
  `swrg.title.player_win2`; победитель получает прежнее gold-сообщение
  `swrg.reward.win`.
- Имя меню-компаса использует существующий `swrg.item.compass`, yellow и
  `italic=false`: RU `Локатор`, EN `Locator`.
- При убийстве возвращён исходный `minecraft:entity.player.levelup` для убийцы,
  master, громкость 1, pitch 2, затем точный `swrg.reward.kill` (gold,
  `italic=false`); существующая награда за убийство дополнена 50 монетами по
  тому же условию, что и запись убийства в статистику.
- Возвращены сообщения `swrg.reward.start_game`, `swrg.reward.stage1` и
  `swrg.reward.stage2` с gold и `italic=false` в местах начисления соответственно
  10, 20 и 40 монет. Первое сообщение идёт участникам, которым контроллер уже
  начисляет стартовые монеты; стадийные — выжившим участникам, получающим бонус.
  Текущие переводы ресурспака сохранены без правок: RU `Старт игры +10 монет`,
  `Пережито первое событие +20 монет`, `Пережито второе событие +40 монет`,
  `Ликвидация игрока +50 монет`; EN `Game started +10 coins`, `The first event
  was experienced +20 coins`, `The second event was experienced +40 coins`,
  `Player Elimination +50 coins`.
- У компаса в матче и после возрождения также исходное имя `swrg.item.compass`
  (yellow, `italic=false`), а не стандартное ванильное имя.

## Удалённые лишние объявления

Из обычного player flow убраны новые `game.join_hint`, `game.started`,
`game.lobby_ready`, `game.kill`, `game.eliminated`, `game.lives` и
`game.cancelled_leave`, а также bossbar-строка `bossbar.countdown`. Они не
соответствуют сообщениям исходного цикла: старый старт использует title subtitle,
убийство — звук и отдельную награду, а исходная функция завершения сообщает ничью
или победителя. Административные ответы, ошибки загрузки карты и их записи в лог
сохранены.

## Не переносилось как косметическая механика

Эта правка восстанавливает только оформление и звуки поддержанных переходов.
Не добавлялись наборы косметических звуков убийства/смерти, случайные командные
префиксы и система послематчевых эффектов победителя из старого datapack/plugin.
Это отдельные косметические подсистемы с выбором и состоянием игрока; текущий
цикл их не поддерживает, и перенос потребовал бы расширить механику за пределы
текстового дизайна. Текущие частицы победы и логика событий сохранены.

Изменения сверялись с точными исходниками старого сервера:
[game/start/0.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/start/0.mcfunction)
(выдача старых +10, строки 105–106),
[game/stage1/select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/stage1/select.mcfunction)
(+20 и bossbar, строки 1–7),
[game/stage2/select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/stage2/select.mcfunction)
(+40 и скрытие bossbar, строки 1–5),
[game/player/kill.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/player/kill.mcfunction)
(звук и +50 текст, строки 1–2),
[game/start/config.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/start/config.mcfunction)
(bossbar setup),
[game/tick1s.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/tick1s.mcfunction)
(bossbar update),
[game/start/timer.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/start/timer.mcfunction)
(countdown), [game/end/0.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/end/0.mcfunction)
(winner/draw/reward) и RU/EN
[core/lang/ru.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/core/lang/ru.mcfunction)/[core/lang/en.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/core/lang/en.mcfunction)
(компоненты/переводы). Все эти translate keys уже имеют исходные EN/RU
значения в read-only pack; новых ключей или правок JSON локализации не требуется.
Оформление сообщений и наград этого игрового цикла сверено по указанным путям;
остальные не перенесённые механики перечислены выше.

При дополнительной сверке восстановлены пробелы вокруг имени победителя и
открывающая скобка таймера: в существующих ключах пака эти символы отсутствуют.
Есть одно различие источников: английский `swrg.title.player_win2` в ресурс-паке
равен `wins the game!`, а функция `core/lang/en.mcfunction` содержит
` wins the game` без восклицательного знака. Используется существующий ключ
пользовательского пака; он не менялся и дублирующий перевод не создавался.
