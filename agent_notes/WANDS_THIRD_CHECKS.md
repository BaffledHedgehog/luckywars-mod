# Tiny Ruby и Terra: серверные проверки

Обновление 15 октября: после перехода Terra на native hitboxes адресный прогон
прошёл за 81,54 с (один матч / четыре уникальные группы), JAR
`58dd11a16095f67baa7dcd8e663d3c3c39cb4a55de93f2c63cb355b2b1013c6e`.
К прежним проверкам добавлены cow в боковой части прежнего unit-cell и cow
за стеной: обе остаются невредимыми при успешных попаданиях по двум мобам
на отрезке. Последующие абзацы с 5099f9fd… описывают прошлый прогон.
[Текущая геометрия и нагрузка](PROJECTILE_PERFORMANCE.md).

Пользователь подтвердил контракт: Ruby — «Сквозной луч, любые блоки — как раньше»; Terra — «Пробивать существ, по одному удару 5 каждой цели за каст». Это принятые требования для native-переноса.

`GameChecks` добавляет `wands-third-menu`, `wands-third` и `wands-third-cleanup`; `tools/smoke_lbc_loadouts.py --wands-third-only` запускает их отдельным циклом, а обычный runner ставит матч после Tiny Air/Stone/Teleport. Меню проверяет таблицы `tiny_ruby_wand`/`tiny_terra_wand`, native IDs, исходные item model/marker/tier, Polymer roundtrip и цены Ruby 500 / Terra 250.

Матч-тест использует вертикальный луч длиной 35 блоков в уже загруженном chunk и сохраняет только позиции его 101 samples и верхние позиции, нужные Terra. Ruby получает Dirt, Bedrock и Water на линии; тест проверяет проход сквозь них, замену на air/fluid-free состояние, один нативный Dirt drop, отсутствие урона cow и одинаковый обычный дроп в Creative/developer Creative. Подготовленный короткий ray коридор возвращается к прежним BlockState в `finally`.

Terra сначала тестируется на пустой линии и при недостаточной мане, затем на двух cow за союзником и временной стеной: каждая цель теряет ровно 5 health один раз за каст, союзник не получает урон, стена заменяется moss block с short grass сверху, mana уходит один раз. Во втором проходе Adventure observer стоит на луче и остаётся прозрачным/защищённым; observer callback не тратит mana. Полный Overload set используется для проверки реального перехода mana 0 → -500/-250. Creative cast проверяется отдельно.

Fixture сохраняет и восстанавливает локальные блоки и только созданные item drops/cows, обеих игроков включая полный инвентарь, броню, cursor/crafting, эффекты, abilities, здоровье/max-health, XP, food, заморозку, положение/скорость, team, mana и mana timers. Observer entry меняет эти данные, поэтому `WandThirdPlayerState` хранит полное состояние Beta до входа и восстанавливает его после удаления observer. `readyForItemDamage` используется только для подготовки союзника, не сбрасывает native damage cooldown у Terra targets.

Native API проверен в локальном 26.3 sources jar. Ruby использует `Level.destroyBlock(pos, true, breaker, updateLimit)` для loot и затем явный `setBlock(AIR)`, поскольку `destroyBlock` оставляет legacy-fluid state. Это намеренно не `ServerPlayerGameMode.destroyBlock`: правила прохода «любые блоки» не должны проверять tool или player block restrictions. По последнему уточнению пользователя Creative и developer не меняют дроп жезла; оба сценария проверяют обычный нативный дроп.

Итог: build прошёл, финальный адресный прогон — 55,77 с, один матч,
четыре уникальные check-группы, без FAIL. Он проверяет одинаковый дроп Ruby
с Creative/developer по последнему уточнению пользователя. Установленный JAR:
`5099f9fd207fa9881adc4ff7ba08e3c3f6a90dbaafc9a7d0b8b199cda0b33234`.
Базовая полная регрессия до последней узкой правки дропа — 364,78 с,
23 матча, 65 сценариев / 69 уникальных check-групп на предыдущем 6458552a… JAR.
Разделение прогонов, логи и ограничения — [SERVER_TESTS.md](../SERVER_TESTS.md).
