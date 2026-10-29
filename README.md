## Vanity player checkpoint - NATIVE COMPONENT, NOT RELEASED (30 October 2026)

[Contract, limitations and exact native proof](agent_notes/VANITY_PLAYER_CHECKPOINT_2026_10_30.md).
Native player NBT plus transient attributes/runtime, raw motion/head/known input,
all metadata, inventory/ender/craft/cursor/components, signed hidden effects and
independent owned leases/shadow HP, native recipes/cooldowns/stats/advancements.
Real checkpoint-driven native respawn; repeated real-tick and reconnect replay;
progress restores without replaying rewards. Personal menus only; full owner
must snapshot/close other containers before this phase. No automatic offline hook.
55cec70b passed9 gameplay suites/one actualmode1 match;runner56.80s;11103 frozen
inputs/5276 packagedentries/34source receipts.199 authorpaths/31overrides/main
d9087f0a unchanged;normalchildexit/exactsnapshots/no locks. No RP/template/question
file edits/maininstall/restart/push. Full original25 gate CLOSED; Vanity stays gated.
Next: full input/writer and lifecycle coordinator, actor identities/references/
cameras/containers, private ability queues/counters, world/map metadata/jobs/clocks,
later-loaded chunks, pending offline-body replay and final persistence. This is
not a complete body/world rollback; retain the existing terrain/core/body owners.

## Vanity core checkpoint - NATIVE COMPONENT, NOT RELEASED (29 October 2026)

[Contract, boundaries and exact native proof](agent_notes/VANITY_CORE_CHECKPOINT_2026_10_29.md).
Explicit controller participants/lives/roles/clock/bar/map-selection values,
all settings/profiles with original array/set identities, native scoreboard with
original Objective/Team/PlayerScores/Score/ScoreAccess handles and native packets,
team assignments, event timeline, Lucky commitment, authority UUIDs and zone.
f93d8136 passed7 gameplay suites/one actualmode1 match;runner54.13s;11083 frozen
inputs/5262 packagedentries/26source receipts. Actual deaths/replacements, later
operator-enrolled newcomer, offline/reconnect CORE replay, repeated real-tick rewind.
199 authorpaths/31overrides/main d9087f0a unchanged;normalchildexit/exactsnapshots/
no locks. No RP edits/maininstall/restart/push/questionfileedit. Full25 gate CLOSED.
This is NOT native player-body/private-queue restoration; Vanity acquisition gated.
Next: native body/player lifecycle snapshot, input/timeline/private-state coordinator,
full map/world metadata/jobs and pending offline restore; preserve terrain/core owners.

## Vanity terrain checkpoint - NATIVE COMPONENT, NOT RELEASED (29 October 2026)

[Contract, implementation and exact native proof](agent_notes/VANITY_TERRAIN_CHECKPOINT_2026_10_29.md).
Independent native chunk palettes/biomes, full block-entity NBT, pending/long
block/fluid scheduler entries; bounded comparisons, existing MapManager/MapLighting
owners, unsupported fluids/fire and same-state sign/container publication.
3571b419 passed6 gameplay suites/one actualmode1 match;runner49.93s;11060 frozen
inputs/5246 packagedentries/24source receipts. Three repeats include actual world
ticks and exact delay/priority/sub-order preservation.199 authorpaths/31overrides/
main d9087f0a unchanged;normalchildexit/exactsnapshots/no locks. No RP edits.
This is a TERRAIN COMPONENT, not the full Vanity rewind. Acquisition stays gated;
full original25 Chaos gate CLOSED; no maininstall/restart/push/questionfileedit.
Next: complete core/all-player/private queue checkpoint and native lifecycle/input/
timeline coordinator; later-loaded chunks/world events/metadata remain required.

# LuckyWars: игровой цикл на Fabric 26.3

## Witch authority lifecycle: native stage, not released

Current ownership now survives normal reconnect/death while rejecting authority
already transferred away, stale Greed bonuses and old match tags. Native operator/
spectator/lives rules remain in charge. Thirteen gameplay suites passed in one
match; full Vanity world/core rewind is still pending.
[Contract and exact proof](agent_notes/WITCH_AUTHORITY_LIFECYCLE_2026_10_29.md).


## Active Lust: native stage, not released

Four source Flesh modes, complete original90 mob pool, actual dragon steering/
rider concealment and antimagic retirement are connected to real mode3 acquisition.
Twelve gameplay suites passed in one actual match; full original25 Chaos release
gate remains closed. [Behavior, exact proof and limits](agent_notes/WITCH_LUST_PORT_2026_10_29.md).


Сервер находится в `E:\servers_mew\skywars2_last`, исходники — в этой папке.
Структура классов и решения переноса описаны в [ARCHITECTURE.md](ARCHITECTURE.md).
Исходное поведение описано в `agent_notes`; пометки `(user)` имеют приоритет.

## Запуск и сборка

Из папки сервера: `build.bat`, затем `start.bat`. Сервер должен быть остановлен
при замене JAR. Java 25 уже находится в `runtime/jdk-25`.

Из папки мода:

```powershell
$env:JAVA_HOME = 'E:\servers_mew\skywars2_last\runtime\jdk-25'
.\gradlew.bat --console=plain build deployServerMod
```

GUI использует вложенную в JAR библиотеку SGui. Fabric API и Polymer установлены
отдельными модами сервера. Carpet добавлен для тестовых игроков и необязателен
для обычной игры.

## Тексты и ресурс-пак

Исходный дизайн предметов сверяется с
[ресурс-паком пользователя и правилами переноса](agent_notes/RESOURCE_PACK.md).
`all_in_one_1.21.8` разрешено только читать. Правки для него готовятся в
`resourcepack_template/` этого проекта и переносятся пользователем вручную.
Существующие ключи, тексты name/lore и их оформление нужно сохранять один в один;
готовые ключи не дублируются в шаблонах или переводах мода.

Сообщения матча, команды, боссбар, названия карт и все страницы GUI передают
клиенту `translate`-ключи с параметрами. Клиент выбирает перевод по своему языку;
сервер не превращает компоненты в русские строки перед отправкой.
Существующие подписи используют исходные ключи `swrg.*` и другие ключи
`all_in_one_1.21.8`, включая `golden_pumpkin` и `golden_pumpkin.lore`.
Их переводы берутся из пользовательского пака и не копируются в мод.
В `src/main/resources/assets/skywars2/lang/ru_ru.json` и `en_us.json` остаются
только записи, отсутствующие в соответствующем языке исходного пака: новые
сообщения `skywars2.*` и недостающие переводы прежних ключей. Такие же записи
подготовлены в `resourcepack_template/assets/minecraft/lang/` для ручного переноса.
Копировать нужно файлы из `assets/` с сохранением путей; если файл уже существует,
добавления объединяются с ним. После переноса пользователь удаляет template;
следующая сверка учитывает уже добавленные в исходный пак ключи.
Технические логи и имена параметров команд не локализуются.

Polymer включает эти файлы через `addModAssets("skywars2")`. На этом сервере
включена автоматическая раздача: готовый пак находится в
`polymer/resource_pack.zip` и предлагается игроку при входе. Принятие необязательно;
Он содержит новые переводы мода. Для исходных `swrg.*`, моделей и шрифтов
на клиенте нужен также включённый `all_in_one_1.21.8`; автоматически собранный
Polymer-пак не является копией этого пака. Мод на клиенте для переводов не нужен.
Настройки раздачи: `config/polymer/auto-host.json`.

Чтобы поменять тексты без пересборки Java, скопируйте целиком нужный файл языка
в `polymer/override_assets/assets/skywars2/lang/ru_ru.json` (или `en_us.json`)
**в папке сервера**, затем измените значения. Полный файл нужен потому, что
Polymer заменяет одноимённый файл в своём собранном паке. Сохраняйте ключи
и параметры `%s` либо `%1$s`, `%2$s`: это числа, имена и вложенные переводы.
После изменения выполните `/polymer generate-pack` и перезайдите для получения
обновлённого пака. При первом создании папки override или изменении настройки
генерации перезапустите сервер. При обновлении переводов мода обновляйте свою
копию override: она имеет приоритет над ресурсами JAR.
Правила приоритета описаны в
[документации Polymer](https://polymer.pb4.eu/latest/user/resource-pack-custom-assets/).

Проверка языков и готового пака из папки мода:

```powershell
python -X utf8 tools/check_localization.py --pack ../polymer/resource_pack.zip
```

Проверка читает исходный ресурс-пак, проверяет наличие переиспользуемых ключей
и отсутствие их копий в новых переводах и template. На другой машине путь
передаётся через `--resource-pack <папка>`.

## Управление

- `/sw`, ПКМ компасом или ЛКМ/ПКМ по эндер-сундуку в лобби — главное меню.
  Компас закреплён в пятом слоте хотбара. В лобби и при загрузке карты запрещены
  перемещение, выбрасывание, подбор предметов и перенос в другую руку; GUI работает.
- `/sw map 1` — выбрать карту; `/sw map 1 broken` — её разрушенный вариант.
- `/sw start` — запуск выбранной карты; `/sw quick` — случайная карта из исходного
  пула для числа игроков, с исключением отсутствующих ресурсов.
- `/sw status` — текущее состояние; `/sw stop` — отмена матча без награды, для оператора.
- `/settings` — параметры и диапазоны; `/settings lives 2`, `/settings day 2`,
  `/settings stageOneSeconds 120` — сохранить настройку в лобби, для оператора
  или консоли. `day`: 1 — день, 2 — ночь, 3 — полночь.
- `/skywars` — альтернативное имя `/sw`.

Настройка режима хранит варианты 1–4, но реализован только обычный SkyWars (1);
в GUI можно выбрать остальные значения, однако запуск сообщает, что режим
ещё не перенесён. Для игры выберите режим 1. Старт требует
двух игроков и достаточного числа спавнов. Первый вошедший игрок
становится ведущим; оператор тоже может запускать игру. Во время матча новые
игроки наблюдают и присоединяются после возврата в лобби.

Настройки: `config/luckywars/game.json`. Профили по UUID:
`config/luckywars/players.json`. Файлы находятся в рабочей папке **сервера**.
Настройки также меняются через GUI в лобби. Запись JSON атомарная; повреждённые
файлы копируются в резервный `.bak` перед восстановлением значений.

Карты и лобби находятся в `minecraft:overworld`, как на легаси-сервере.
Для новых миров используется пустой плоский генератор. При старте сервера
асинхронно подготавливаются лобби и выбранная в конфиге карта. Выбор в GUI
сразу запрашивает подготовку; при перекрывающихся запросах действует последний.
Старт и quickstart во время подготовки ожидают её завершения и затем запускают
матч без второго клика. Повторный выбор той же карты не размещает её заново.
Стекло по умолчанию использует вариант 21; очистка пола полностью удаляет блоки
в области перед заполнением. Нативный загрузчик использует
настоящие NBT и маркеры из старых карт. Журнал
`world/data/luckywars/overworld-bounds.json` с привязкой к измерению обеспечивает
очистку после перезапуска. Старый журнал отдельного измерения и его файлы мира
не переносятся в Overworld и не удаляются автоматически.
NBT читается и подготавливается в фоне, чанки загружаются без ожидания в игровом
тике, а готовые части вставляются постепенно. Во время загрузки открытое меню
сохраняется; `/sw`, навигация, статистика и покупки продолжают работать.
Компас открывает меню и во время загрузки: игроки ждут над ареной в adventure
с временным полётом и защитой от урона; перед отсчётом эти способности снимаются.
Общие настройки, смена карты и повторный старт ждут готового лобби. При переходе
к отсчёту меню закрывается. Размер части — до 8×8×8 блоков; бюджет вставки —
4 мс и максимум 64 операций за тик. Это ограничение очереди: отдельный вызов
ванильного кода может занять больше времени.

## Границы текущего этапа

Реализован обычный SkyWars: лобби, меню, карта, клетки и отсчёт, участники и жизни,
смерть и респаун, две стадии событий, победа/ничья, награды и возврат в лобби.
Есть магазин ключей, сохранение статистики и компас ближайшего живого соперника.

Перенесены прямые действия всех восьми типов артефакта `mystical:1`, включая
16 цветовых веток, усиленные водные эффекты и накопительный RTP-статус.
Ядро маны содержит восстановление, лимиты, усиление/обратный поток,
бонусы mage-брони/ремней и единый API расхода. HUD сохраняет прежний вид и
объединён с ракетой. Пять семейств мановых зелий используют нативные
питьё/бросок/облака через три типа Polymer; сохранены 39 исходных записей
в 15 таблицах, оформление и таймеры. Добавлены стадии отрицательной маны,
Crux и перегруженная Polymer-броня: [контракт и проверки](agent_notes/OVERLOAD_PORT.md).
Добавлены Tiny Fire/Tiny Blind и cast-тринкеты Mana Gem/Blood Anal Boom:
[поведение и границы](agent_notes/WANDS_FIRST_PORT.md). Кнопка настроек в
слоте 8 управляет второй рукой (`offhandWands`, по умолчанию выключено).
Перенесены Tiny Air/Stone/Teleport в тот же компонент:
[контракт и пользовательское исправление повторного попадания](agent_notes/WANDS_SECOND_PORT.md).
Актуальный результат проверки и установка указаны в [SERVER_TESTS.md](SERVER_TESTS.md).
Добавлены пробивающие Tiny Ruby/Terra с согласованными правилами разрушения
и урона: [контракт](agent_notes/WANDS_THIRD_PORT.md).
Tiny Acid создаёт шесть снарядов на уже готовой нативной физике кислоты:
[контракт и границы](agent_notes/WANDS_FOURTH_PORT.md).
Остальные заклинания, Nyauwu и семейства артефакта 2/3/4 ещё требуют переноса.
[Контракт маны](agent_notes/MANA_PORT.md),
[мановые зелья](agent_notes/MANA_POTIONS_PORT.md),
[Контракт цветов](agent_notes/ARTIFACT_COLORS_PORT.md),
[следующий этап](agent_notes/NEXT_MIGRATION_PLAN.md).

Поддержаны наборы LBC 1/2/4/5/7/9/10/15/17/22/23, навык 5 и 30 улучшений. Динамит, наноловушки и ракетные ботинки используют существующий `LbcItems`; облака BrBad — его компонент `LbcClouds`. Ракетные ботинки и четыре зелья BrBad имеют отдельные серверные типы Polymer с прежним ванильным видом. Импортированы 357 `lbc:true_advancements/**`; 122 прежних уведомления отправляются после успешной выдачи достижения. Пять уведомлений acidman требуют все десять исходных достижений. Сохранены прежние ключи, модели и lore; дополнения ресурспака не нужны. Живые проверки описаны в [SERVER_TESTS.md](SERVER_TESTS.md).

Выбор работает через исходный GUI; выдача — при старте и респауне, очистка — при смерти, выходе и завершении матча. Профиль содержит 12 слотов улучшений, активный лимит по умолчанию 4 (настраивается от 1 до 12). Закрытые элементы проверяют исходные advancement ID; оператор может перенести доступ командой `/sw lbc-unlock <player> <lbc:true_advancements/...>` в лобби. Достижимость всех 357 критериев зависит от переноса связанных механик.
Остальные предметы LBC, BedWars, кейсы и косметика требуют дальнейшей работы; режимы 2–4 пока не запускаются. Обычные навыки 2/4/13 используют нативные события.

Навык 12 добавляет воздушный импульс и синхронизирует скорость с клиентом. Наблюдатели хранятся в реестре: клиент видит Adventure вне просмотра, сервер считает игрока spectator; просмотр переключает на настоящую spectator-камеру. `/sw inventory` открывает инвентарь цели только для просмотра, `/sw detach` возвращает инструменты наблюдателя. Копии инвентаря и эффектов для HUD не активируют эффекты на сервере; действия, inventory ticks и awards наблюдателя отменяются. Модовые цели проверяют `game.isObserver`, поскольку vanilla `@a` может включать spectator. У Vex есть владелец, защищённый от его атак; призывы очищаются вместе
с состоянием игрока/матча. Параметры и отличия от старого lifecycle описаны в
[VANILLA_SKILLS_2_4_13.md](agent_notes/VANILLA_SKILLS_2_4_13.md).

## Ближайший план переноса — 11 октября 2026

Набор 22: нативная попытка планирования даёт импульс, три заряда восстанавливаются по одному с исходным интервалом 301 тик. Перенос способности — обычный крафт с выходным слотом в сетках 2×2 и 3×3; также сохранён перенос между брошенными предметами. Расходуется один донор, повторное улучшение запрещено. Детали: [ROCKET_BOOTS_PORT.md](agent_notes/ROCKET_BOOTS_PORT.md).

Набор 17 BrBad: термит, сера, фосфин и гремучая смесь используют нативный бросок lingering potion. Кислота по уточнению пользователя летит с нативной гравитацией и сопротивлением воздуха. Детали и границы: [BRBAD_PORT.md](agent_notes/BRBAD_PORT.md).

Тринкеты Blaze №1, Ghast №2, Intercal №10, Gneiss Stone №16, Snow Gem №18 и Ice Gem №20 зарегистрированы через Polymer с прежним дизайном. Выбор хранит до 12 циклических слотов, допускает повторы, лимит по умолчанию 9. `LbcTrinkets` обрабатывает выстрелы и урон без опроса инвентаря по тикам. Blaze и Ghast используют нативные огненный шар и ракету с одним броском шанса на залп; ледяные предметы используют общий `LbcFrost`. Поведение и границы: [FROST_PORT.md](agent_notes/FROST_PORT.md), [TRINKETS_PROJECTILE_PORT.md](agent_notes/TRINKETS_PROJECTILE_PORT.md), [TRINKETS_PORT.md](agent_notes/TRINKETS_PORT.md), [план переноса](agent_notes/NEXT_MIGRATION_PLAN.md), результаты — [SERVER_TESTS.md](SERVER_TESTS.md).

1. Первый шаг CPU: новые сущности лобби проверяются через очередь `ENTITY_LOAD`,
   полный обход всех измерений — при входе и раз в 20 тиков. Условия whitelist
   сохранены; изменение тегов уже сохранённой сущности обнаруживается в пределах
   секунды. Проверка инвентаря остаётся каждый тик ради прямых серверных изменений.
   Контролируемый замер выполняет `/sw check lobby-cpu` на тестовом сервере.
   Далее требуется измерить весь тик/CPU отдельно при загрузке и в матче
   с представительной нагрузкой; подбор целей компасов остаётся кандидатом.
2. Обычные навыки 2, 4 и 13 реализованы в `MatchEvents`: нативные события
   вместо старых scoreboard/advancement-проверок каждого игрока. Для Vex
   очередь содержит только ожидающие срабатывания окна `HurtTime=5`.
   Живые проверки запускает `tools/smoke_skills.py`; их результат и границы —
   [SERVER_TESTS.md](SERVER_TESTS.md).
3. Выбор/выдача независимых наборов и перков LBC реализованы первой частью;
   нативный предметный движок начат тотемом и очищением. Далее — боевые обработчики,
   от которых зависят остальные
   наборы/навыки, затем автоматические исходные разблокировки. Учитывать условия доступа
   и сброс состояния матча. Источники и порядок интеграции —
   [INTEGRATION_REVIEW.md](agent_notes/INTEGRATION_REVIEW.md) и
   [COMMAND_IMPLEMENTATIONS.md](agent_notes/COMMAND_IMPLEMENTATIONS.md).
   Кейсы, косметика и BedWars остаются отдельными этапами.

Подготовку данных и файловый ввод-вывод можно разгружать в фоновые потоки;
изменения мира, сущностей и инвентарей остаются в серверном потоке. Принципы
оптимизации CPU и экономной работы агентов закреплены в [AGENTS.md](AGENTS.md).
Профилирование под представительной нагрузкой ещё не проводилось; имеющиеся
серверные проверки подтверждают функциональность, но не масштабируемость.

Эталон дизайна — исходный datapack: 27 слотов эндер-сундука, прежняя раскладка,
пустые ячейки, модели, имена и lore. Сверка источников и ограничения описаны в
[GUI_DESIGN_RESTORE.md](agent_notes/GUI_DESIGN_RESTORE.md) и
[DESIGN_SOURCE_AUDIT.md](agent_notes/DESIGN_SOURCE_AUDIT.md). Игровые надписи —
в [GAME_TEXT_DESIGN_RESTORE.md](agent_notes/GAME_TEXT_DESIGN_RESTORE.md).

Старые ресурсы в `src/main/resources/data` сохранены, но `processResources`
включает только новую dimension, NBT, loot-таблицы и каталог. Старые функции,
advancements и переопределения ванильных реестров не выполняются параллельно с Java.
Повторный импорт карт: `python tools/import_game_resources.py` (пути исходного
сервера указаны в начале скрипта). Неполные карты видны с причиной недоступности.
Импорт переводит loot tables на формат 26.3 (`modifier`/`condition`, discriminator
`type`). Три старые таблицы, переключавшие режим через scoreboard, ссылаются на
обычный SkyWars `*_0`; Lucky-пулы `_lb` сохранены для следующего этапа. Количества
предметов из временного атрибута датапака превращаются в реальные размеры стаков.

В лобби прочие измерения очищаются от загруженных сущностей кроме игроков,
согласно пометке `(user)`. Не размещайте постоянные декорации вне арены, пока
это правило включено в игровой цикл.

Результаты серверной проверки записываются в `SERVER_TESTS.md`.

Меч Абсолютного Нуля перенесён как Polymer-предмет с общим cold/frozen
и исходным оформлением. Получение для проверки и границы переноса случайной
выдачи — [ICE_SWORD_PORT.md](agent_notes/ICE_SWORD_PORT.md).

Начальная форма мистического артефакта и антимагический маяк перенесены
через Polymer. Маяк хранится как нативный lightning-rod block с эпохой в
нетикающем block entity; индекс областей по чанкам очищает сферу ограниченной
очередью. Артефакт учитывается в инвентаре, курсоре и сетках крафта. Подавление
используется мечом и навыками; Looper меняет описание только для получателя
с выбранным навыком. Получение и границы: [MAGIC_PORT.md](agent_notes/MAGIC_PORT.md).

Пассивы артефактов `1:6` (очистка эффектов) и `1:8` (метаболизм)
используют тот же снимок инвентаря и цикл `LbcMagic`, без новых классов.
Регенерация II исправлена по просьбе пользователя. Контракт, команды выдачи
и проверенная сборка — [ARTIFACT_PASSIVES_PORT.md](agent_notes/ARTIFACT_PASSIVES_PORT.md).
Выборочная очистка 1:5 и общий сброс кастомных статусов артефактом 1:6/очищающим зельем:
[поведение и статус проверок](agent_notes/ARTIFACT_DEBUFF_PORT.md).
Артефакт защиты от стрел 1:4: [поведение, выдача и проверки](agent_notes/ARTIFACT_ARROWS_PORT.md).
Looper показывает описание только получателю с выбранным перком, сохраняя
общий серверный предмет, включая предметы на земле:
[контракт, синхронизация и проверки](agent_notes/LOOPER_PORT.md).
Артефакт 1:2 удаляет взрывоопасные сущности в сфере радиуса 24 каждые два тика:
[поведение, выдача и границы ядерной ветки](agent_notes/ARTIFACT_EXPLOSIONS_PORT.md).
Артефакт 1:3 превращает жидкости и падающие блоки в сфере радиуса 16,
с событийной обработкой вместо постоянного полного обхода:
[поведение, выдача и пользовательское уточнение](agent_notes/ARTIFACT_LIQUIDS_PORT.md).
