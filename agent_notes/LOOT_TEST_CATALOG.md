# Каталог `/loot` для проверки перенесённых предметов

Каталог подсказок хранится в `src/main/resources/skywars2/loot_test_catalog.json`.
`LootTestCatalog.isSuggested` управляет namespace `lbc` и `swrg`, разрешает
остальные namespace без изменений, семейство
`lbc:ported/*` и явно перечисленные готовые исходные таблицы. Файл читается
однократно из JAR; изменение каталога требует сборки и перезапуска мода.
Новые загруженные `ported/*` автоматически появляются в подсказках.

Это каталог удобных входов для ручной проверки, а не исчерпывающая оценка
переноса каждого исходного файла. Наличие записи не заменяет runtime-проверку
всех режимов, рецептов, графики и взаимодействий предмета.

## Классификация источников

Каталог проверен настоящими серверными Brigadier completions и явной выдачей
скрытой вложенной таблицы в сборке `111f23c4…`. В JAR 109 ported-таблиц и
7 разрешённых исходных входов; архив готов, на основной сервер не установлен.
Proof: `build/verification/stand-verification.json`, подробности —
[SERVER_TESTS.md](../SERVER_TESTS.md). Ручного клиентского просмотра не было.

- `data/lbc/loot_table/ported/*.json` — публичные подготовленные таблицы
  с нативными предметами и сохранёнными исходными компонентами.
- `lbc:kfc_yumyumyummy` — единственный нативный `skywars2:kfc_chick`;
  источник еды действующего `LbcShooters`, обработка — `LbcKfcFood`.
- `lbc:trinket/blaze`, `ghast`, `intercal`, `gneiss_stone`, `snow_gem`,
  `ice_gem` — каждый файл выдаёт единственный нативный предмет,
  зарегистрированный в `LbcTrinkets` и используемый `skywars2/lbc_loadouts.json`.
- Остальные `lbc`-таблицы скрыты по умолчанию: внутренние `gui`, `uncrafted`,
  `drops`, составные/random/transformables-пулы, исходные дубликаты,
  незавершённые предметы и не классифицированные здесь готовые семейства.
  Широкий исходный пул может выдавать как готовые, так и неподдерживаемые
  варианты, поэтому наличие одного готового результата не разрешает весь пул.
- Все `swrg`-таблицы скрыты по умолчанию: среди них есть внутренние GUI
  и сундуковые пулы; ни один не внесён в публичный каталог без отдельной
  проверки. Их исходные ресурсы и вызовы игры сохраняются. Полный префикс
  `lbc:ported/` не разрешает одноимённые пути `swrg:ported/*`.

## Семантика и передача клиенту

`LootSuggestionsMixin` фильтрует результат `ResourceOrIdArgument.listSuggestions`
только для реестра `Registries.LOOT_TABLE` и серверного `CommandSourceStack`.
Сохраняются диапазон замены и tooltip каждой Brigadier-подсказки.
`ResourceOrIdArgument.parse` и содержимое реестра не изменяются: явно введённый
скрытый ID, вложенные ссылки и игровые вызовы `rollTable` продолжают работать.
Тот же loot-table argument используется у `loot` и `fish`; другие типы
аргументов (predicate, modifier, feature, dialog) не фильтруются.

Локально изучен `26.3-sources.jar` в `.gradle/loom-cache/minecraftMaven`:
`ResourceOrIdArgument`, `SharedSuggestionProvider`, `CommandSourceStack`,
`LootCommand`, `ClientSuggestionProvider`. Ванильный клиент не имеет
reloadable-реестра loot tables; `ClientSuggestionProvider.suggestRegistryElements`
переходит к `customSuggestion`, отправляет `ServerboundCommandSuggestionPacket`
и получает серверные подсказки. Фильтрация только отображения в собственном
клиенте не решила бы задачу серверного мода; здесь фильтруется серверный ответ.

## Проверка

Проверять реальную цепочку Brigadier: взять `server.getCommands().getDispatcher()`,
вызвать `parse(input, source)`, затем `getCompletionSuggestions(parsed)`.
Источник должен иметь разрешение команды `loot`. Пример входа без начального `/`:
`loot give <имя реального игрока> loot lbc:`. Проверить наличие
`lbc:ported/slr`, `lbc:kfc_yumyumyummy`, `lbc:trinket/blaze`, отсутствие
`lbc:uncrafted/stone` и `lbc:random_item`. Повторить с узкими префиксами
`lbc:ported/`, `lbc:uncrafted/`, а также с `minecraft:` для сохранности ванили.
Подсказки для префикса `swrg:` должны быть пустыми, включая GUI-таблицы;
произвольный сторонний namespace должен сохранять прежние подсказки.
Проверить ветку `loot spawn ~ ~ ~ loot lbc:` и fishing-аргумент.

Отдельно проверить успешный parse/execute скрытой, но существующей таблицы
`lbc:uncrafted/stone`, а также реальный вложенный roll готовой механики.
Проверка одного `isSuggested` или чтение JSON не доказывает работу mixin и
передачу подсказок. Нужен runtime-тест на работающем моде; интерактивное
нажатие Tab в ванильном клиенте подтверждает сетевую цепочку целиком.

В рамках реализации выполнена статическая сверка источников и JSON;
сборка и сервер этим исполнителем не запускались.
