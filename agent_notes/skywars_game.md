# Игровой цикл SkyWars: core, лобби и матч

Источник — отключённый набор datapack `world/datapacksdisabled/swarogskywars`. Это описание исполняемого datapack, не утверждение о текущем сервере. GUI, maploader и реализация режимов LBC здесь рассмотрены только на уровне границ вызовов.

## Загрузка и общий цикл

Единственный элемент `minecraft:load` — `swrg:core/load`. Он немедленно запускает `core/tick` и `core/tick1s`, пишет сообщение о перезагрузке и вызывает `core/setup` только если `storage swrg:setup` не содержит `setup`. Обе функции ставят себя в расписание повторно: тик — через 1 игровой тик, секундная — через 1 секунду. `core/tick` распределяет режим по `#gamestat swrg.math`: 0 — лобби, 2–4 — игровой tick, 5–6 — эффекты победы; каждую итерацию он также отзывает advancement `swrg:core/player_reset`. Секундная ветка вызывает `lobby/tick1s` при 0 и `game/tick1s` при 2–4. Состояние 1 и значения вне перечисленных диапазонов циклом не обрабатываются.

Первичная настройка создаёт scoreboard-цели (`core/scores_create`), задаёт параметры (`core/default_settings`), ставит английский язык, bossbar, начальные `#mapstat/#gamestat/#developer = 0`, подготавливает GUI roll, держит загруженным чанк в положительных координатах (10241024,10241024), ставит жёлтый shulker на Y=0..8, выключает урон от падения в Overworld, задаёт день, выбирает ведущего и строит лобби через `swrg:maploader/lobby`; затем помечает setup выполненным, пересоздаёт 32 команды и включает `immediate_respawn`. Ошибка или отсутствие внешних функций GUI/maploader здесь не исследованы. (user) Не надо через scoreboard - создай settings, храни базу данных сохраненных настроек, добавь команду /settings <параметр> <значение>; не используй yellow shulke box и прогруженный чанк в положительных координатах - логику рулетки и все для чего нужны шалкеры на больших координатах сделай через мод (шалкеры это костыли датапака) (\user)

Начальные игровые параметры: `#pvp=2`, `#day=1`, `#gamemode=2`, `#kit/#lbckit/#lbcskill/#lbclevelup/#skill/#stage1/#stage2/#hp=1`, `#tnt_resist=4`, `live_count=1`, `lefthand=1`, `no_borders=0`, `#lbclevelupmax=4`, `#prefixes=3`, `#lbctrinket=9`, `#lbc.challenge=1`, `#auto_restart=0`, `cfg_lucky_modifier=0`, `#health=2`, `#imbadrop_chance=4`; также `imbadrop_chance lbc.math=4`. Значения — значения по умолчанию после первого setup/вызова сброса настроек, не обязательно значения конкретного матча.

## Лобби и запуск

Каждый тик лобби вызывает `swrg:gui/main` от имени каждого игрока, переводит всех не `developer` в adventure и удаляет сущности в Overworld, если они не проходят [`swrg:core/lobby`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/predicate/core/lobby.json) (user)во всех остальных измерениях полностью удаляй все сущности независимо от тега.(\user). Предикат сохраняет всех игроков, любую сущность с тегом swrg.quickstart, area_effect_cloud с `swrg.delete`/`swrg.lobby`, маркеры look/spawn/center/midle/default и сущности с тегами map_object/strict_map_object; в Nether и End удаляются все сущности. Раз в секунду игрокам (кроме developer) сбрасывают эффекты; затем всем, включая developer, выдают Weakness CI и Regeneration CI на 2 секунды (amplifier 100), Saturation I на 40 секунд и Resistance V на 2 секунды; также выполняется GUI-страница, очищается XP (`-10000 levels`), при отсутствии bedrock в (0,126,0) повторно запускается `maploader/lobby`, ставится template `swrg:lobby_light`, игроков ниже Y=100 телепортирует на (0,127,0), а у горящего игрока блок в позиции ног заменяется на powder snow только если это minecraft:light. Селектор GUI обрабатывается каждую секунду через `lobby/tick1s_checkgui`. (user)через мод убирай огонь без снега. Снег-костыль дата пака(\user)

Подробная ветка quickstart и последовательность `start/0..2` разобраны в следующем разделе. `start/config` сбрасывает атрибуты, применяет время суток и задаёт расписание стадий: первая — через 120 секунд, вторая — через 360 секунд при `#stage2 != 0`; первая запланирована даже при #stage1=0; bossbar получает 120 или 360 секунд соответственно. В режимах `#gamemode=3..4` стадии и привязка игроков к bossbar пропускаются.

`start/clear` очищает инвентари всем и запускает `core/player/reset` игрокам не в spectator, если рядом (2 блока) нет маркера `swrg.spawn`; затем зацикливается каждый тик. `start/bars` меняет уже существующий обычный glass в объёме 3×7×3 вокруг игрока (Y−2..Y+4) с цветом/материалом по `swrg.bars=1..20` (последние варианты используют барьеры, цепи, лёд или несколько цветов).

## Матч: контроль, смерть и границы

В каждом игровом тике `game/tick` считает игроков с `gamemode!=spectator`. При `#developer=0`, режимах вне 3..4 и числе живых 0..1 начинается `game/end/0_pre`; если живых 2+, `timer_end` ставится в 5 (каждый тик). Для режимов 3..4 вызывается внешняя `lbc:bedwars/game/tick` при `#developer=0`. При `#pvp=1` всем игрокам с предметом из тега `#axes` вызывается `player/set_damage`; при `#skill=1` живым игрокам вызывается `skills/tick`. Каждый тик очищается счётчик fall damage, отзывается advancement `swrg:core/attacking`, обрабатываются `tag=dead` через `game/resp`, TNT через `game/main/tnt`, случайный маркер spawn становится world spawn, spectator проходят `player/spectator`.

Каждую секунду таймер уменьшается на 1, отражается в bossbar и его заголовке. Игроки с предикатом `swrg:core/hold_compass` получают компасную логику. Не-developer за квадратом X/Z от −500 до 500 (проверка `x=-500,z=-500,dx=1000,dz=1000`) отправляются в `core/player/map_out`, кроме измерения `nexus` и при `no_borders=1`. Реальное место назначения зависит от функции map_out и маркеров карты.

`game/resp` сначала вызывает внешнюю `lbc:respawn`, снимает `dead` при нулевом DeathTime и маркере `swrg.spawn` в радиусе 1, затем безусловно снимает `dead`, поэтому последующая проверка `if entity @s[tag=dead]` в этой же функции не срабатывает. Это выглядит как мёртвая ветка, и ожидаемый respawn через `core/player/respawn` из неё недостижим. Вход death/reset описан ниже; внутренняя реализация вызванных функций LBC остаётся внешней зависимостью.

## Награды/матчевые ресурсы и завершение

В scoreboard создаются объекты для смерти, выхода, fall distance, здоровья, kit/skill/suffix, монет, пяти видов ключей, кейс-ролла, статистики, побед и убийств. Сами objectives не доказывают начисление; фактические формулы наград и обновления статистики из `core/player/stats`, `game/player/kill` и `game/end/*` разобраны ниже. Автоперезапуск имеет цепочку: `autorestart_1` обнуляет `#games_in_a_row`, сохраняет мир, ждёт 5 секунд; `_2` записывает `restart` в `storage lbc.math.mew`; `_3` выполняет макрос `$$(mew)`, то есть результат зависит от содержимого storage и формата макрокоманды.

## Связи и границы раздела

Внешние границы этой части — реализации вызываемых функций `lbc:*`, GUI и maploader. Переходы и эффекты источников SkyWars описаны ниже; соответствие числовых индексов quickstart конкретным картам задаёт maploader. Полные варианты префиксов и сундуковых пулов приведены в приложениях.

## Исходники

- Загрузка/состояния: [`load`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/core/load.mcfunction), [`tick`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/core/tick.mcfunction), [`tick1s`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/core/tick1s.mcfunction), [`setup`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/core/setup.mcfunction), [`scores_create`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/core/scores_create.mcfunction), [`default_settings`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/core/default_settings.mcfunction).
- Лобби: [`lobby/tick`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/lobby/tick.mcfunction), [`lobby/tick1s`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/lobby/tick1s.mcfunction).
- Матч: [`game/tick`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/tick.mcfunction), [`game/tick1s`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/tick1s.mcfunction), [`game/resp`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/resp.mcfunction), [`start/config`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/start/config.mcfunction), [`start/timer`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/start/timer.mcfunction), [`start/clear`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/start/clear.mcfunction).
- Тег загрузки: [`load.json`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/minecraft/tags/function/load.json).

## Уточнение ветки запуска и стадий

Quickstart вызывается advancement `swrg:core/quickstart` при взаимодействии с entity типа `minecraft:interaction` с тегом `swrg.quickstart`. Функция `quickstart/0` считает всех игроков. Admin может начать при двух и более игроках и `#quickstartstat=1`; обычный игрок — при тех же условиях, только если на сервере вообще нет admin. При менее двух ошибка тоже показывается только admin либо обычному игроку при отсутствии admin; обычный игрок при присутствующем ведущем ничего не запускает. Advancement затем отзывается во всех случаях. `quickstart/1` запускает `maploader/map/alwayspre` и повторяет `quickstart/1a` раз в секунду, пока `#if_map_loaded=0`; любое другое значение либо отсутствующий score пропускают это ожидание. Для `#gamemode=3..4` выбирается BedWars-карта (случайное число 1..6); для остальных выбор зависит от количества игроков: 1..4 и 5..8 — случайный индекс 1..61, 9..12 — случайный из 53,54,55,56,58,59,60,61,63; 13..16 — из 53,58,59,60,63; 17..20 — из 58,60,63; 21..24 — всегда 63. При 25+ новый индекс не выбирается, но вызов макроса `quickstart/2` всё равно происходит: используется ранее сохранённый quickstart, если он есть, либо макрос не получает обязательный параметр. Обычная загрузка планирует `start/0` через 5 секунд; instant-вариант выбирает индекс 1..56 и запускает его сразу. Это стык с `maploader`, где значения индексов преобразуются в конкретные карты.

`start/0` переводит `#gamestat` в 1, телепортирует игроков на (0,128,0), убирает вокруг lobby площадки bedrock/barrier/light/ender chest, удаляет lobby quickstart-сущности, очищает GUI, сбрасывает атрибуты и LBC данные, обнуляет счетчики команд 1..32. При `#gamemode=2` запускает `lbc:start`; режим 1 пропускает этот вызов; при BedWars — делает `return run lbc:start_bedwars`, то есть оставшиеся команды `start/0` пропускаются в этой ветке. Для обычной ветки увеличивает сыгранные игры и gameid, выбирает маркеры spawn, ориентирует игроков на `swrg.look`, назначает им случайные команды 1..32 через маркеры и `team_join`, выдаёт каждому число жизней из `live_count`, добавляет 10 монет и применяет выбранный суффикс. Затем запускает countdown и повторяющийся очиститель площадок.

Через обратный отсчёт вызывается `start/1`: применяется config, `#gamestat=2`, игрокам даётся Instant Health 255, вокруг spawn очищается площадка, через 5 секунд планируется `start/2`, наполняются сундуки и применяются SkyWars kit и интеграции LBC trinket/kit/levelup/skill/challenge. Устанавливается `game_started lbc.math=1`, вызывается `lbc:start_after_all`, игроки около spawn переходят в survival, включаются mob/block/entity drops. Через `start/2` в Overworld вновь включается fall damage, снимается forceload в измерениях imprinted и white, затем опционально запускается Halloween-визуализация (25 `item_display` на базе item model `lbc:strangeness`).

В обычном режиме `stage1/select` планируется через 120 секунд даже при `#stage1=0`; нулевое значение отключает награду и событие, но не сам вызов, обновление таймера и состояние 3. `stage1/select` выдаёт живым +20 монет, выбирает случайное событие 1..8 при `#stage1=1`, либо напрямую выбирает событие по значениям `#stage1=2..9`, переводит `#gamestat=3`, ставит красный bossbar и таймер 240; скрывает bossbar только при `#stage2=0`. Через следующие 240 секунд `stage2/select` выдаёт +40 монет, выбирает 1..7 при `#stage2=1` либо событие по значениям 2..8, затем ставит `#gamestat=4`. Эффекты событий приведены ниже в сводной таблице.

## Смерть, окончание и сброс

Advancement `swrg:core/player_reset` срабатывает на каждом `minecraft:tick`, если predicate `swrg:core/player_reset` видит `swrg.leave` или `swrg.death` от 1 до 998900; награда вызывает `core/player/reset_pre`. Там сначала вызывается `lbc:leave_death`, затем уменьшаются жизни (кроме `sin_vanity` и активного `indolence`); уход обнуляет жизни, снимает `have_bed`, выходит из команды и сразу сбрасывает игрока. Наличие `have_bed` переводит игрока в spectator, ставит respawn_timer=5 и компенсирует одну жизнь; таймер обслуживает BedWars. Смерть при оставшихся жизнях помечает игрока `dead`; без жизней — вызывает reset. Особые теги `indolence_active`/`sin_vanity` меняют ветку и могут оставить игрока dead. Перед анонсом вызывается `lbc:death_inject_before_announce`; затем смерть сбрасывается, в игровом состоянии игрока отправляют в spectator к `swrg.look`, проигрывается настроенный звук, одинаковые прибавления и вычитания ключей по диапазонам GUI взаимно компенсируются, закрываются GUI/ролл, сбрасываются атрибуты и вызывается `lbc:resets`.

`game/tick` при выжившем одном или нуле запускает `end/0_pre`: таймер `timer_end` доводится до 0 за 5 игровых тиков, затем `end/0` объявляет ничью или победителя (по ранее подсчитанному `#count`). В конце отключаются дропы, победителю начисляются +100 монет и победа, при Halloween вызывается доп. награда, `lbc:win`; случайно выдаётся один ключ `swrg.keys0..3` (по одному из четырёх равновероятных исходов). Очищаются отложенные стадии и номера команд; `#gamestat=5`, игроки переносятся к look-маркеру, инвентари очищаются, победитель ненадолго получает creative, остальные spectator; через 10 секунд вызывается `end/1`. Финальный reset вызывает ряд функций LBC, грузит лобби, увеличивает `#games_in_a_row`, устанавливает `#gamestat=0`, обновляет статистику, GUI, `#quickstartstat=1`, обнуляет roll/case и возвращает всех в adventure на (0,127,0), выключает fall damage и снова готовит roll. При `#games_in_a_row>=10` и `#auto_restart=1` инициируется сохранение и макро-автоперезапуск.

### Открытые замечания по коду

- В `game/resp` тег `dead` снимается безусловно до проверки `if entity @s[tag=dead]`; эта последняя ветка с `core/player/respawn` недостижима при обычном выполнении функции.
- Звук смерти расположен в отдельном `core/player/reset`, до `reset_attributes`, обнуляющего death. При исчерпании жизней вызов `reset` из `reset_pre` может проиграть звук. При наличии оставшихся жизней запускается другая цепочка `dead`/`lbc:respawn`; её нельзя смешивать с reset.
- `start/0` в режиме BedWars завершает функцию через `return run`, поэтому стандартные назначения жизней, команд, монет, countdown и прочие команды после LBC-старта не исполняются.
- В `quickstart/1a` счётчик игроков включает всех игроков; при 25+ нет ветки задания нового индекса, однако последующий макрос всё равно вызывается. Диапазон выбора карты 1..61 для до 8 игроков может содержать недоступные/особые значения; сверка с maploader не входит в этот раздел.
- Таблицы стадий, наборов и сундуков приведены ниже. Вызванные реализации LBC связаны с отдельными заметками runtime, kit и bedwars; их содержательная приёмка ведётся отдельно.

### Конкретные события стадий

Первая стадия случайно выбирает один из восьми вариантов при `#stage1=1`; `#stage1=2..9` напрямую задаёт событие 1..8. Все восемь событий включают звук сломанного щита и объявление; 3 и 6 дополнительно перезаправляют сундуки.

| Событие | Фактический эффект | Исходник |
| --- | --- | --- |
| 1 | Живым Speed II бесконечно | [`event1`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/stage1/event1.mcfunction) |
| 2 | Живым Haste II бесконечно | [`event2`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/stage1/event2.mcfunction) |
| 3 | Удаляет содержимое chest-маркеров, заполняет сундуки заново | [`event3`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/stage1/event3.mcfunction) |
| 4 | Живым Jump Boost IV бесконечно | [`event4`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/stage1/event4.mcfunction) |
| 5 | Живым Health Boost V бесконечно | [`event5`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/stage1/event5.mcfunction) |
| 6 | Маркеры default и midle переводятся в center; сундуки заполняются заново с center loot | [`event6`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/stage1/event6.mcfunction) |
| 7 | Живым Invisibility I на 300с | [`event7`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/stage1/event7.mcfunction) |
| 8 | Живым Glowing бесконечно | [`event8`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/stage1/event8.mcfunction) |

Вторая стадия случайно выбирает событие 1..7 при `#stage2=1`; `#stage2=2..8` напрямую задаёт событие 1..7.

| Событие | Фактический эффект | Исходник |
| --- | --- | --- |
| 1 | Над каждым живым появляется Wither на 10 блоков выше | [`event1`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/stage2/event1.mcfunction) |
| 2 | Над каждым живым появляется Ender Dragon с `DragonPhase:8` на 20 блоков выше | [`event2`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/stage2/event2.mcfunction) |
| 3 | Wither I на 2с; повторное применение планируется раз в секунду, пока состояние 3..4. Звук/объявление только при первом состоянии 3 | [`event3`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/stage2/event3.mcfunction) |
| 4 | При состоянии 3 выполняет для каждого игрока не в spectator три `summon marker` дождя TNT через `execute as @a` (без `at @s`), разбрасывает сущности в радиусе 190 и запускает TNT сверху с `{fuse:100s}`. Суффикс `s` здесь задаёт тип NBT short; fuse равен 100 тикам, примерно 5с. Повтор планируется через 5с при состоянии 3..4 | [`event4`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/stage2/event4.mcfunction) |
| 5 | Центр границы ставится на случайного живого; размер немедленно 384, затем за 4000с уменьшается до 4 | [`event5`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/stage2/event5.mcfunction) |
| 6 | Живым Levitation I на 30с | [`event6`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/stage2/event6.mcfunction) |
| 7 | Размещает 10 маркеров в радиусе 192; в каждой позиции на Y=110 призывает курицу с glowing chest minecart `swrg:chests/center`, обрабатывает пустой modifier и число стаков | [`event7`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/stage2/event7.mcfunction), [`set_count_entity`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/chests/set_count_entity.mcfunction) |

Окончание очищает расписания `stage1/select` и `stage2/select`, но не уже запланированные повторы event3/event4. Каждый может выполниться ещё один раз после конца: event3 наложит Wither, но guard не поставит следующий вызов; event4 выполнит spread/summon для оставшихся маркеров (если они есть), но guard не создаст новых маркеров и не поставит следующий вызов.
### Атрибуты и статистика игрока

`core/player/reset_attributes` сбрасывает базовые значения множества атрибутов (броня, урон/скорость атаки, гравитация, здоровье, скорость, reach, сопротивления и др.), удаляет модификатор `gamehealthmodifier`, затем по `#health=0/1/3/4` применяет max-health множитель соответственно −95%, −50%, +100% или +200% (если `legacy lbc.math !=1`). При `#pvp=1` attack speed после reset устанавливается в 100000; всем выдаётся Instant Health на 3 сек. с amplifier 99, обнуляется смерть и снимается тег `dead`. Настройки #health=2 (начальное значение) не добавляют модификатор.

`core/player/first_time` при подключении во время состояний 1..4 переводит игрока в spectator и к look-маркеру, обнуляет счётчики косметики/монет/ключей/ролла и статистику, затем рассчитывает stat view и открывает GUI. В `core/player/stats` WR=floor(100×wins/games), целая часть K/D=floor(kills/deaths), дробная=(floor(100×kills/deaths) mod 100). Защиты от нулевых games/deaths нет: деление выдаёт ошибку, следующие команды продолжаются, а score остаётся результатом предшествующего присваивания/умножения. Поэтому при нулевом знаменателе показываемые числа не означают рассчитанное отношение. `map_out` берёт floor(0.1 × BASE max_health), прибавляет отдельно floor(max(abs(floor(X))−500,0)/10) и такую же величину по Z и передаёт итог команде `damage ... outside_border`. Например BASE max_health=20, X=510, Z=0 дают команду damage 3; бонусы max_health-модификаторов в этой формуле не учитываются. Затем spectator без `map_out` возвращает к look-маркеру; точный тип/обработчик damage зависит от внешней реализации.

## Навыки, компас и TNT

Если `#skill=1`, `game/tick` каждую игровую итерацию вызывает [`skills/tick`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/player/skills/tick.mcfunction) для каждого игрока не в spectator. Условия прямого вызова и эффекты сводятся к таблице; число в колонке — значение `swrg.skill`.

| `swrg.skill` | Механика и точное условие | Эффект | Источник |
| --- | --- | --- | --- |
| 1 | Паук: владелец получил урон (`HurtTime=10`) | Яд I на 3с ближайшему игроку на расстоянии ≥0.1 с advancement attacking; реальный источник удара и команда не сверяются | [`skills/tick`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/player/skills/tick.mcfunction) |
| 2 | Тяжеловес: fall-distance scoreboard ≥335 | Урон падением вокруг игрока; счётчик ограничивается 3999, урон = счётчик ×0.00915 | [`tick`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/player/skills/tick.mcfunction), [`fall_damage`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/player/skills/fall_damage.mcfunction) |
| 3 | Бегун: `HurtTime=10` | Speed II на 5с | [`skills/tick`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/player/skills/tick.mcfunction) |
| 4 | Всеядный: съедение любого предмета из тега `#swrg:food` | Absorption I на 10с, Regeneration I на 5с, Saturation на 2с; 50% снять Hunger | [`food advancement`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/advancement/core/food.json), [`food`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/player/skills/food.mcfunction) |
| 5 | Берсерк: здоровье ≤15 / ≤10 | Resistance II / IV на 3с | [`skills/tick`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/player/skills/tick.mcfunction) |
| 6 | Стрелок: стрела нанесла существу урон ≥3 | Одна стрела, затем 10% шанс выдать ещё одну | [`shoot_arrow advancement`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/advancement/core/shoot_arrow.json), [`bow`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/player/skills/bow.mcfunction) |
| 7 | Яростный: здоровье ≤15 / ≤10 | Strength I / III на 3с | [`skills/tick`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/player/skills/tick.mcfunction) |
| 8 | Феникс: predicate `swrg:core/fire` | Strength I и Resistance I на 2с | [`skills/tick`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/player/skills/tick.mcfunction), [`fire predicate`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/predicate/core/fire.json) |
| 9 | Маньяк: владелец атаковал, рядом цель с `HurtTime=10` | Darkness I на 7с | [`attacking advancement`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/advancement/core/attacking.json), [`skills/tick`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/player/skills/tick.mcfunction) |
| 10 | Владелец атаковал: ближайший не spectator с HurtTime=10 в 0.1..20 / владелец имеет HurtTime=10: ближайший не spectator в 0.1..7 | Шанс молнии 50% / 20%; фактический атакующий, жертва и команда не сверяются | [`skills/tick`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/player/skills/tick.mcfunction) |
| 11 | Вампир: владелец атаковал | Regeneration II на 5с и 25% шанс Instant Health I | [`attacking advancement`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/advancement/core/attacking.json), [`skills/tick`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/player/skills/tick.mcfunction) |
| 12 | Крылья ветра: спринт в воздухе, не legacy | Движение вперёд через LBC `motion add ^ ^-0.01 ^.1` | [`sprint`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/predicate/sprint.json), [`on_ground`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/predicate/on_ground.json), [`wind_rider`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/player/skills/wind_rider.mcfunction) |
| 1200 | Вариант крыльев: то же условие | Более сильное движение `motion add ^ ^ ^.4` | [`wind_rider_gay`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/player/skills/wind_rider_gay.mcfunction) |
| 13 | HurtTime ровно 5 и 50% шанс; не legacy | Призывает Vex с 0.1 здоровья и follow range 50, назначает команде владельца | [`summon_vex`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/player/skills/summon_vex.mcfunction) |

Advancement `core/food` использует список из 36 ванильных продуктов в [`swrg:food`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/tags/item/food.json). `core/shoot_arrow` требует, чтобы прямой источник урона был в теге `#minecraft:arrows`; `core/attacking` реагирует на неблокированный урон. Вероятностные predicates задают ровно 10%, 20%, 25% и 50%; `sprint` проверяет `is_sprinting=true`, а `on_ground` — `is_on_ground=true`, инвертированный селектор навыка требует нахождения в воздухе. См. [`chance_10`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/predicate/core/chance_10.json), [`chance_20`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/predicate/core/chance_20.json), [`chance_25`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/predicate/core/chance_25.json), [`chance_50`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/predicate/core/chance_50.json).

Каждую секунду `game/tick1s` обновляет компас у игроков, у которых [`hold_compass predicate`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/predicate/core/hold_compass.json) находит компас в hotbar слоте 0..8 или offhand. В обычном режиме он выбирает ближайшего не spectator на расстоянии ≥0.1; игроки ближе 0.1 исключены независимо от личности; в `#gamemode=3..4` функция [`compass_team_check`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/main/compass_team_check.mcfunction) выбирает ближайшего игрока из другой команды 1..32. Координаты X/Z округляются вниз до целых, Y остаётся 0; копируются в lodestone_tracker каждого компаса в hotbar и offhand; целевое измерение задано как Overworld. Начальная позиция storage — [0,0,0]. В командном режиме координаты исключают свою команду, но отображаемое имя во всех десяти слотах `compass0` снова выбирает ближайшего не spectator без team-фильтра: имя и направление могут относиться к разным игрокам. Игрок без команды 1..32 не получает координат в team-helper. См. [`compass`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/main/compass.mcfunction) и [`compass0`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/main/compass0.mcfunction).

Функция [`game/main/tnt`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/main/tnt.mcfunction) показывает оставшийся fuse TNT как имя от 4с до 0.1с; в последний тик по `#tnt_resist=1..4` ближайшие в радиусе 6 получают Resistance I..IV на секунду. По умолчанию `#tnt_resist=4`.
## Вспомогательные системные детали

`core/create_teams` удаляет и заново создаёт команды 1..32. Каждой задаётся один из 16 цветов (второй набор повторяет первый), отключено отображение невидимых союзников и death messages разрешены всегда; в обычном SkyWars при старте игроки распределяются по этим командам индивидуально. `core/get_random` переносит #random_min/#random_max из scoreboard в storage и макрофункцией делает `random value min..max`, диапазон включительный.

`core/select_host` раз в секунду сам себя перепланирует, пока нет игрока `admin`; каждый вызов дополнительно открывает GUI-страницу у `@p`. `game/player/kill` при advancement убийства игрока даёт только звук/сообщение `reward.kill` («Player Elimination +50 coins») и отзывает advancement; score монет здесь не меняется. Death sound выбирает один из 13 ванильных звуков по `swrg.sound`.

Заполнение сундуков выбирает по тегам marker `swrg.default`, `swrg.midle` и `swrg.center` (обобщённый [`chest predicate`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/predicate/core/chest.json)) соответствующие loot tables. Верхние таблицы `swrg:chests/default`, `midle`, `center` переключают вложенные `*_0` при gamemode=1 и `*_lb` при gamemode=2. Перед установкой loot table функция удаляет содержимое существующих сундуков, при отсутствии chest-блока ставит обычный сундук; затем для каждого маркера применяет модификатор `swrg:empty` (он пустой) и исправляет предметы. Исправление копирует число из `components.minecraft:attribute_modifiers[].amount` в поле `count` по слотам 0..26, затем удаляет эти модификаторы — предметы loot используют attribute amount как временный контейнер размера стака. Таблица `default` поэтому не описывает конечное содержимое сама; содержимое вложенных таблиц приведено в приложении skywars_chests.json и таблице ниже.

В завершении игры `win_effects/select` сначала учитывает режим турнира из `tournament lbc.event` (вариант 1 или 2), затем `swrg.death_effect=1` выбирает хэллоуинские тыквы, `=2` пытается вызвать `golden_swo` и прерывает функцию до фейерверков. Остальным запускается генератор фейерверков. `golden_swo` в исходнике пуст: отдельного начального действия нет, и фейерверки не запускаются. Однако `core/tick` в состояниях 5..6 вызывает `win_effects/tick_players`, где эффект 2 запускает `golden_swo_player_tick` каждый тик. Анимация золотых мечей и слитков поэтому активна. Турнирные варианты увеличивают `tournament_wins` победителю кроме BaffledHedgehog; при 3+ wins пишут сообщение, выдают призовой advancement, также раздают trophy2 игрокам с 2+ и trophy3 всем. Подробности золотого меча и граница pumpkin-raycast описаны в уточнениях ниже.

Ресурс автоперезапуска при 10 играх и включённом `#auto_restart` вызывает `core/autorestart_1`: сбрасывает серию игр, делает `save-all`, ждёт 5с, записывает строку `restart` в `storage lbc.math mew`, а следующий макро-вызов исполняет содержимое переменной как команду. Без значения макроса / обработки команды поведение за пределами datapack не установлено.



Сопутствующие predicate-файлы подтверждают буквальные критерии: [`fire`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/predicate/core/fire.json) = игрок горит; [`chance_10`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/predicate/core/chance_10.json), [`chance_20`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/predicate/core/chance_20.json), [`chance_25`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/predicate/core/chance_25.json), [`chance_50`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/predicate/core/chance_50.json) = random_chance 0.1/0.2/0.25/0.5. `core/chest` — наличие любого из трёх тегов chest-маркера. `core/hold_compass` проверяет только hotbar и offhand, не основную руку вне hotbar (она и так часть hotbar) и не armor slots.

## Стандартные наборы и режим наблюдателя

`game/start/1` вызывает `game/player/kit` при `#kit=1`. Набор определяется личным scoreboard `swrg.kit`; функция не ограничена живыми игроками и командами сама, поэтому выдаёт предметы всем соответствующим игрокам на момент вызова. Ниже перечислены vanilla-предметы, создаваемые командами; персональные наборы LBC выдаются отдельными внешними вызовами.

| `swrg.kit` | Выдача |
| --- | --- |
| 1 | 8 TNT, зажигалка, золотой нагрудник с Blast Protection 100 |
| 2 | 2 яйца призыва волка, 16 костей |
| 3 | По одному splash-зелью лечения II, Speed I и Regeneration I |
| 4 | Деревянный меч с Knockback I, кожаный нагрудник с Fire/Blast/Projectile Protection I |
| 5 | Золотое яблоко, 8 хлеба, 2 золотые моркови |
| 6 | Кольчужный нагрудник с Thorns IV |
| 7 | Золотой нагрудник с Protection IV, +5 брони и +3 сопротивления отбрасыванию |
| 8 | Splash Poison, 7 паутин, 64 нити |
| 9 | 16 gilded blackstone, 16 гравия, 16 золотых слитков, зелье Fire Resistance |
| 10 | Щит с max_damage 25 |
| 11 | Стол зачарования, 6 книжных полок, 20 лазурита, 20 уровней XP |
| 12 | Splash Poison, loot table `swrg:game/kit/12`, 2 ведра молока |
| 13 | Золотые кирка и топор |
| 14 | По 32 пороха/бумаги/красного красителя, 12 fire charge, арбалет max_damage 165 |
| 15 | Зелье невидимости |
| 16 | Каменный меч с Fire Aspect II, длительное Fire Resistance, ведро лавы, зажигалка |
| 17 | 2 торта, 8 slime block, 8 honey block, особый фейерверк (flight 2), золотой меч |
| 18 | Loot table `swrg:game/kit/18` |
| 20 | Золотой меч с Fire Aspect I, полный золотой комплект, 10 gold ore, золотое яблоко |
| 21 | Дубовая плита с Sharpness V и 16 каменных плит |
| 22 | При `legacy lbc.math=1` функция прекращается до выдачи; иначе loot table Halloween pies |
| 23 | Булава с Wind Burst I, +3 урона и −2.4 скорости атаки |
| 24 | 12 wind charge с max_stack_size 99 |

Для `swrg.kit=19` выдачи нет. Таблицы 12, 18 и 22 находятся в этом datapack; их предметы перечислены следующим абзацем.

`game/player/spectator` вызывается каждому зрителю каждый тик. Если в радиусе 2 блока есть живой игрок, зрителю показывается его здоровье в actionbar; без такого игрока зритель очищается. При живом соседе вызывается `spec_inventory`, который копирует инвентарь, броню и offhand от `@p[gamemode=!spectator]` без ограничения радиуса, это остаётся тот же ближайший сосед при неизменной позиции, поскольку отсутствие radius не меняет sort=nearest. Ниже Y=0 или выше Y=10000 зрителя телепортирует к `swrg.look`; ему также постоянно выдаётся Instant Health.

Loot-таблицы наборов тоже проверены: kit 12 дополнительно выдаёт по одному splash-зелью Blindness II на 10с, Wither II на 20с и Nausea II на 20с; названия берутся из `swrg:lang` (`item.kit12_0..2`). Kit 18 выдаёт player head «Anonymous mask» с Protection II, Fire Protection III, Blast Protection III, Sharpness VI, Fire Aspect I и +3 armor в слоте головы, с закреплённой текстурой. Kit 22 — четыре golden apple-модели `lbc:golden_pumpkin`, которые едятся за 0.5с и дают Instant Health I на 1 тик и Absorption I на 1000 тиков; имя и lore переводимые.


Карта quickstart по числу игроков: при 1..4 и 5..8 выбирается число 1..61; 9..12 — равновероятно из индексов 53,54,55,56,58,59,60,61,63; 13..16 — из 53,58,59,60,63; 17..20 — из 58,60,63; 21..24 — индекс 63. Функции-таблицы: [`1_players_4`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/quickstart/1_players_4.mcfunction), [`1_players_8`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/quickstart/1_players_8.mcfunction), [`1_players_12`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/quickstart/1_players_12.mcfunction), [`1_players_16`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/quickstart/1_players_16.mcfunction), [`1_players_20`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/quickstart/1_players_20.mcfunction), [`1_players_24`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/quickstart/1_players_24.mcfunction). Соответствие индексов картам определяется `maploader` и здесь не заявлено.

Halloween-награда победителям включается только при `halloween lbc.event=1`: один случайный исход из 100 даёт всем не spectator 100 мемкоинов с шансом 70%, 250 с шансом 29% и 1500 с шансом 1%. Функция исправляет отсутствующий счётчик memcoins на 0 перед прибавлением. См. [`event_add_memcoins_halloween`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/end/event_add_memcoins_halloween.mcfunction).

Фейерверк включается после обычной ветки `win_effects/select`. `fireworks` ставит себя в расписание через 1с, при `#gamestat=5` создаёт четыре marker и переводит состояние в 6, разбрасывает все `swrg.firework` маркеры в радиусе 192 и в каждой позиции призывает ракету с `LifeTime:60`; `firework_random` назначает ей два случайных цвета/один fade color, один из пяти типов взрыва, случайный twinkle и жизнь 10..40. После смены состояния создание новых marker прекращается, но функция продолжает планировать себя в состояниях 5..6 и повторяет spread/summon поверх оставшихся маркеров. В коде здесь нет удаления маркеров и внутреннего лимита повторов. В обычном завершении `end/1` сбрасывает `#gamestat` через 10 секунд; после этого `core/tick` прекращает запуск win-effects tick, а условие повторного расписания fireworks не проходит.


В режиме `#pvp=1` [`player/set_damage`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/player/set_damage.mcfunction) заменяет item modifier каждого удерживаемого топора; их добавка к attack damage: деревянный 3, золотой 3, каменный 4, железный 5, алмазный 6, незеритовый 7 (`item_modifier/swrg:<material>_axe`). Остальные характеристики определяются самим ванильным топором и другими enchantment/attribute источниками.

`core/lang/en` и `core/lang/ru` — функции, наполняющие общий storage `swrg:lang` текстами и компонентами. Setup загружает английский вариант; отдельная GUI-функция может переключать язык, поэтому язык сообщения матча зависит от последнего общего выбора. #lang равен 2 для en и 1 для ru; обе функции меняют CustomName всех сущностей swrg.lobby и вызывают соответствующую LBC kit-локализацию. Значения ключей, связанных только с GUI и косметикой, в этой игровой документации не каталогизируются.

В `game/start/2_halloween` сезонная настройка события LBC `halloween` создаёт 25 item_display в точке (0,100,0) с item model `lbc:strangeness`, распределяет их через `spreadplayers` на радиус до 190; сразу после spread все screamer_halloween, включая ранее существовавшие, поднимаются на .01 блока; повторного движения или удаления сама эта функция не планирует.

## Суффиксы и командные префиксы

На `start/0` префикс выбирает `team_prefix/select_for`: цикл обходит команды 1..16, для каждой берёт случайную запись из `storage swrg:core prefix` и записывает её в team prefix; команды 17..32 остаются без случайного префикса. `list_sfw` загружает 170 записей, `list_nsfw` — 91, а `list_all` вызывает оба и даёт 261; каждая запись хранит JSON-текст и цвет. Меню выбирает список при `#prefixes=0,1,2`, а начальный `#prefixes=3` не загружает список; если storage пуст, безопасный выбор не гарантирован. Все тексты и цвета приведены в [`skywars_game_prefixes.json`](skywars_game_prefixes.json).

`player_suffix_select` переносит номер команды игрока в `storage lbc.math team`, вызывает перевод числового `swrg.suffix` в символ команды и показывает игроку ссылку с просьбой прислать drop, если у него есть advancement `drop_1..3` и нет соответствующего `drop_requested`. `player_suffix_select1` отображает значения 1..16 символами ★ ۞ ♛ ♔ ♠ ♣ ♥ ♦ ♪ ☉ «20㎝» ☠ 卍 ✟ ❤ 🤩, 17..29 — приватными символами шрифта; 0 или отсутствующий score дают пустой суффикс. Это только декорация scoreboard/команды; фактическое владение/снятие открытых суффиксов относится к reward advancement/GUI.

Все `swrg:rewards/drop_1..3`, `drop_1..3_requested` и `turnir`/`turnir2` `trophy1..3`/`killer` advancements имеют единственный критерий `impossible`, без display, родителя или reward function. Они сами не срабатывают от игрового события: datapack может только вручную выдавать/отзывать их как флаги прогресса. Поэтому drop-request сообщение показывается только для уже выданного `drop_N` при ещё не выданном `drop_N_requested`; триггер выдачи этих маркеров должен быть во внешней механике или в другом коде, который здесь не найден. Турнирные трофеи выдаются функциями турнира напрямую по числу побед; это UI-флаги, а не самостоятельные награды advancement.

### Открытые границы

Прямые источники этого участка описаны и связаны в coverage. Остаются только внешние границы: реализация вызываемых `lbc:*`, GUI и maploader, включая соответствие числовых индексов quickstart конкретным картам.
## Отдельные Unix/calendar утилиты

`core/unix/init` вручную использует `Paper.LastSeen` случайного игрока (`@r`) как источник времени и применяет масштабы `0.001`, `0.000031810662934`, затем ещё `0.001`; ниже аналогично преобразует поле через `0.0011574074` и `0.01`, считает год/високосность/номер месяца и печатает дату. Константы `#seconds_in_year=31436000`, `#days_from_1970=86400` и последовательность перезаписи одного storage значения выглядят нестандартно и дают основание подозревать ошибку расчёта; корректность календарной даты не подтверждена. `core/unix/time` ставит repeating command block по (10241024,10,10241024) с командой `help` и `auto:1b`, затем извлекает символы 10..15 из block entity `LastOutput` в storage `swrg:core time`. Эти функции исполняемые, но поиск вызовов в datapack не нашёл; они не входят в известный автоматический цикл load/tick.

## Полные варианты командных префиксов

Три загрузчика записывают варианты в `storage swrg:core prefix`: `list_sfw` — 170, `list_nsfw` — 91, `list_all` вызывает оба последовательно и даёт 261. В каждой записи сохранены JSON-текст и цвет. Источники: [`list_all`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/start/team_prefix/list_all.mcfunction), [`list_sfw`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/start/team_prefix/list_sfw.mcfunction), [`list_nsfw`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/start/team_prefix/list_nsfw.mcfunction). [`team_prefix/select`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/start/team_prefix/select.mcfunction) выбирает равномерный случайный индекс от 0 до длины списка−1, передаёт текст и цвет в макрофункцию `team_prefix/select_prefix`. При старте игрока цикл перебирает только команды 1..16, хотя команды 17..32 существуют. В GUI переключатель удаляет предыдущий массив и выбирает SFW, NSFW или объединённый список при `#prefixes=0,1,2`; значения 3 и 4 список не загружают. Setup по умолчанию задаёт `#prefixes=3`, а стартовый цикл вызывается без загрузки списка. При пустом массиве безопасный выбор не гарантирован; меню может предварительно загрузить список. Все 261 записи, включая цвет, перечислены в [`skywars_game_prefixes.json`](skywars_game_prefixes.json).

## Содержимое сундуков

Таблицы `swrg:chests/default`, `midle` и `center` выбирают дочернюю версию `_0` при `#gamemode=1`, `_lb` при `#gamemode=2`; это отдельная проверка score таблицы, независимая от отображаемого имени режима. Каждая дочерняя pool делает один roll. Условие `random_chance` независимо решает, выполняется ли pool; `weight` нормирует выбор только среди альтернатив внутри выполненного pool. Все 85 пулов (12+13+14+15+15+16) с компонентами, raw weights, эффектами и условиями сохранены как структурированное приложение [`skywars_chests.json`](skywars_chests.json).

| Loot family | `_0` pools | `_lb` pools | Группы/варианты и значимые различия |
| --- | ---: | ---: | --- |
| default | 12 | 13 | Шанс лука .15 → .33, воды/лавы .60 → .95. Инструменты .75 в обеих версиях, вес железной кирки 4 → 6. Железная броня меняется на кольчужную. Веса пустого исхода/железного/каменного меча: 10/31/47 → 5/40/60. `_lb` дополнительно гарантирует 4 Lucky Block. |
| midle | 14 | 15 | Шанс стрел .60 → .90, параметры amount 3–17 → 8–17; жемчуг .20, инструменты .75, компас .25, вода/лава .60, TNT/зажигалка .70. Лук .30 → .45. Блоки .92 и броня .90 в обеих версиях; железная/алмазная броня меняется на кольчужную/железную. Мечи .90 с разными весами вариантов. `_lb` дополнительно даёт Lucky Block с amount min=6/max=8. |
| center | 15 | 16 | Шанс стрел .60, жемчуга .15, алмазного инструмента .80, компаса .90, TNT/зажигалки .93; вода либо лава гарантированы. Морковь .80, яблоко .75, блоки .90, лук .50. Броня: ботинки .70, поножи .65, нагрудник .55, шлем .70; в `_lb` нагрудник .65 и меч .70 вместо .80. `_lb` дополнительно даёт 5–11 Lucky Block. |

Диапазоны количества также сохранены в JSON: default — TNT 3–7, стрелы 4–11, хлеб 2–7, жареная свинина 5–13, доски 20–33, камень 16–31; midle — TNT 8–13, свинина 4–17, хлеб 3–10, золотые яблоки 1–2, доски 20–47, камень 16–33; center — TNT 12–21, жемчуг 1–3, морковь 4–13, яблоки 1–3, доски 40–65, камень 20–65. Lucky Block здесь — ванильный `minecraft:red_mushroom_block` с `minecraft:item_model=lbc:lucky_block`. Временный attribute amount затем превращается в размер стака через `game/chests/set_count`, после чего attribute_modifiers удаляются. Пулы содержат взвешенные альтернативы и зачарования, в том числе Protection I–IV, Efficiency III–V, Flame/Power I; точные компоненты каждого варианта находятся в приложении.

Эти диапазоны — min/max временного **float amount**, а не гарантированные
равномерные целые числа. После копирования в count дробная часть отбрасывается:
min=6/max=8 обычно дают 6 или 7, а 8 возможен только у самой верхней границы
из-за float-округления. Числа шансов в таблице относятся к соответствующим
условиям pool/entry; итоговый шанс конкретного предмета также учитывает веса
и условия остальных альтернатив. Подробности преобразования:
[SOURCE_FINDINGS](SOURCE_FINDINGS.md).

## Уточнения координатора по игровому циклу

Это прямое чтение исполняемых команд: исправления относятся к состоянию datapack,
а не к желаемому балансу будущего мода.

- `game/tick` продолжает оставшиеся команды после вызова `end/0_pre`, даже когда
  тот завершил игру и изменил gamestat. Поэтому в той же итерации могут ещё
  выполняться навыки, respawn, TNT и spectator-логика. Живыми в счётчике считаются
  все не spectator, включая creative/adventure; команды не ограничивают их картой.
- `start/config`: #hp=0 скрывает здоровье в имени и TAB; 1 показывает под именем;
  2 — в TAB; 3 — в обоих местах. Day 1/2/3 означает day/night/midnight;
  `as @a` не меняет измерение time set. В BedWars привязка игроков к bossbar и
  расписания стадий пропущены, но общие reset/time/health-display команды остаются.
- Countdown начинает с числа 5 немедленно, показывает 4/3/2/1 по секундам,
  затем через ещё секунду вызывает start/1: пять секунд от первого показа.
  `start/clear` начинается спустя секунду и далее очищает всех каждый тик до
  отмены расписания start/1; встроенной проверки gamestat у него нет.
- `core/player/reset`: прибавление и вычитание ключей в одинаковых селекторах
  страниц 5100../5200../5300../5400../5500.. взаимно компенсируются. Диапазоны
  вложены, но итоговая добавка каждой валюты равна нулю; это не возврат ключа.
  Команда team leave пропущена только в #gamemode=3..4.
- `reset_pre` сначала вызывает LBC leave_death. Проверка Indolence ищет любого
  skeleton indolence_finger через @e без distance и связи с владельцем. Have_bed
  ставит respawn_timer=5, spectator и возвращает одну жизнь; точное расписание
  возврата в игру находится в [BedWars](lbc_bedwars.md). Все if ниже идут
  последовательно: состояние после вызванного reset уже могло измениться.
- `end/0` начисляет wins/100 coins/ключ и вызывает lbc:win для @p не spectator,
  а не заранее сохранённого UUID победителя. При ничьей без таких игроков эти
  команды не находят цель; Halloween-награда тоже не имеет получателя. После
  телепорта creative выдаётся снова ближайшему не spectator, остальные
  не creative становятся spectator. Удаление командных счётчиков и отключение
  дропов происходит и при ничьей. Возврат в лобби через 10с даёт всем Absorption I
  на секунду и меняет размер Overworld border на 2 000 000; центр не сбрасывается.
- `spectator` копирует 41 слот ближайшего не spectator, когда тот есть в пределах
  2. Это один и тот же nearest selector при неизменной позиции, поэтому источник
  совпадает с ближайшим соседом; отсутствующий radius в дочерних командах сам по
  себе не меняет порядок ближайших. Y-проверка использует hitbox и объём
  y=0,dy=10000, а не только точку ног.
- Навыки, выбирающие `@p`, выбирают игроков, не произвольных мобов. У них нет
  общего исключения союзников. Для Маньяка предел 0.1..6 и HurtTime=10; у
  «Тяжеловеса» конечный damage выбирает всех @e на расстоянии 0.1..6, включая
  сущности других типов, без team/spectator-фильтра. Fall score ограничивается
  3999, значение float равно score×0.00915 (максимум около 36.59); промежуточный
  HurtTime сохраняется, но дальше не используется. Всем fall score обнуляется
  после прохода навыков.
- Стеклянные варианты 1..16 идут в порядке white/orange/magenta/light_blue/
  yellow/lime/pink/gray/light_gray/cyan/purple/blue/brown/green/red/black.
  17 — barrier; 18 — ice; 19 — barrier плюс четыре безусловные колонки iron_chain
  по углам (Y−2..Y+3); 20 — пять слоёв red/yellow/green/blue/purple на Y−2..Y+2.
  Основные замены затрагивают только обычный glass; верхние слои при варианте 20
  остаются исходными. Первичный бокс создаёт отдельная developer/bars функция.

### Золотые мечи после победы

Каждый тик эффекта 2 вычисляется `m=ticker % 60`; при m≥30 он заменяется на
60−m. `i=0.1m`, `j=6m`: на Y+5, с yaw игрока+j и pitch=0 создаётся обычный
item золотого слитка на расстоянии i. Затем ровно один равновероятный вариант
из восьми создаёт item_display golden_sword около глаз владельца: X/Y
(-3,0),(-2,1),(-1,2),(-.25,2.5),(.25,2.5),(1,2),(2,1),(3,0).
Дисплею задаются transformation, teleport/interpolation 1, теги
supergoldensword/slowraycast/player; ближайший новый дисплей в .01 получает
Rotation и actived. Видны частицы gold_block и слышен iron.break.
У золотых слитков нет специального pickup delay или удаления в этих helpers;
дальнейшая обработка slowraycast относится к [runtime](lbc_runtime.md).

Pumpkins создаёт пять markers slowraycast/player/winpumpkin и разбрасывает все
winpumpkin в квадрате spreadplayers с range=190; эти теги затем обрабатывает LBC.
При вызове fireworks случайные параметры назначаются всем firework_rocket без
distance, а не только четырём новым ракетам. Цвета выбираются 0..16777216
включительно, формы — пять вариантов, twinkle 0/1, LifeTime 10..40 тиков.
Один уже запланированный вызов fireworks может дойти после возврата в лобби:
его spread/summon/random-команды без guard, только следующее расписание защищено.


### Дополнение к взаимодействию эффектов победы с LBC

Дисплей золотого меча обрабатывает
[`wingoldensword`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/win_effects/wingoldensword.mcfunction).
На каждом проходе он проверяет четыре точки вперёд с шагом 1, рисует gold_block
и при Distance=4 телепортируется к конечной точке через LBCR, сбрасывая Distance.
Попадание в #minecraft:mobs в кубе 1×1×1 с distance≤10 (без tag spectator),
либо блок вне #minecraft:airs, вызывает один из пяти равновероятных исходов:
золотая фигура из пересекающихся fill-объёмов, raw_gold_block плюс внешняя
команда midasfr, 18 falling gold blocks по вееру с шагом yaw 10° плюс TNT
fuse=0/explosion_power=3, аналогичная фигура из raw_gold_block, либо полный
куб золота 3×3×3. Фигуры из четырёх fill заменяют только #solid; полный куб
варианта 5 заменяет любые блоки. Каждый исход удаляет дисплей; дополнительный
предел самого дисплея — Lifetime≥30. Условия попадания в сущность и блок
записаны отдельными if, общего stopper-фильтра здесь нет. Команда midasfr
относится к [внешним командным мостам](COMMAND_BRIDGES.md).

[`winpumpkin`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/win_effects/winpumpkin.mcfunction)
при каждом вызове с шансом 30% призывает falling pumpkin на один блок вперёд,
повернувшись к (0,100,0), заменяет #tannerkivi в объёме 11×11×11 тыквами,
создаёт TNT fuse=0 и переносит сам marker через spreadplayers в range=25 от
текущей X/Z позиции. Сам helper не удаляет marker; общий slowraycast/reset
определяет длительность. Это эффект над картой, а не только частицы.

Повторяющиеся исходники слотов 0..26, 25 одинаковых Halloween summons,
261 запись префиксов и все 85 chest pools сверены с приложениями;
результат — [skywars_variants_audit.json](inventory/skywars_variants_audit.json).
Прямые функции этого раздела прочитаны координатором. Другие разделы LBC,
GUI и maploader остаются отдельными заданиями содержательной проверки.
