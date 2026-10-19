# Trinket port: Intercal and Gneiss Stone

Перенесены два тринкета с общим выбором нескольких слотов, Polymer-предметами и обработкой урона. Адресный серверный сценарий прошёл за 55,51 с, полный — за 143,32 с (семь матчей, 18 групп runner); проверенный JAR установлен с резервной копией. Логи, SHA и границы проверок — [SERVER_TESTS.md](../SERVER_TESTS.md). Вход настоящим клиентом и CPU под нагрузкой ещё не проверены.

## Текущая реализация

В Fabric-каталоге доступны только Intercal (ID 10) и Gneiss Stone (ID 16). Intercal закрыт достижением `lbc:true_advancements/legends/academy/corruption`; Gneiss не имеет gate. Остальные тринкеты пока недоступны. Страница `11100` относится к статистике старого меню, а не к дополнительному выбору в текущем каталоге.

Оба предмета зарегистрированы через Polymer как серверные типы с прокси-видом исходных предметов. Loot tables сохраняют исходные translate keys, модели, имя, lore и custom data: Intercal использует `lbctrinket.9`, `lbc:trinket_9` и идентификатор `intercal`; Гнейс использует `lbctrinket.15`, обычный `minecraft:stone` без кастомной модели и идентификатор `gneiss_stone`. Камень размещается обычным действием Minecraft и расходует предмет.

Выбор хранится в профиле как массив из 12 ID и циклический курсор. Повторные ID разрешены; каждый выбор заменяет текущий слот и двигает курсор с учётом действующего лимита. Лимит по умолчанию — 9, конфигурационный диапазон — 0..12, а GUI переключает 1..12. При нуле выдача и эффекты тринкетов отключены. Счётчик эффекта читает совпадающие предметы в инвентаре, личной сетке крафта и слоте курсора; отдельного слота экипировки нет. Перебора инвентаря по тикам для этих двух эффектов нет.

Intercal срабатывает при уроне от игрока, кроме урона типа `ARROW`. Вероятность равна `10% + 5% × число копий` (15% с одной копией). При успехе живые мобы, включая активных игроков, в радиусе 10 блоков получают Wither I на 10 секунд; при выбранном LBC-навыке 15 и включённых LBC-навыках дополнительно накладывается Wither IV на 3 секунды. После него возвращается оставшийся Wither I через нативную скрытую цепочку. Это сохранение сочетания для старых профилей; сам навык 15 ещё не доступен для выбора в новом каталоге. Наблюдатели и союзники исключены. Перенос проверяет союзность через `isAlliedTo`; старый datapack использовал `team_number` и `predicate=!lbc:same_team`. При переносе BedWars нужно связать команды с этим фильтром; сейчас запускается только обычный SkyWars без команд.

Gneiss проверяется при получении игроком урона. Вероятность `8% + 2% × число копий` (10% с одной копией); успех даёт Resistance V на 40 тиков (2 секунды), звук камня и частицы. Текущий обработчик защищает наблюдателей через `allowDamage`.

## Выдача при старте и смерти

Текущий код выдаёт выбранные тринкеты при начальном применении loadout матча. В ветке возрождения вызывается `lbc.apply(player, false)`, поэтому выбранные тринкеты после смерти сейчас повторно не выдаются. В старом LBC отдельная выдача есть для BedWars start/respawn; в общем `function/respawn.mcfunction` старого SkyWars явной выдачи тринкетов не найдено. Предпочтение пользователя о восстановлении в общем SkyWars ещё не утверждено; сохраняем текущее поведение до такого решения.

## Источники

- Текущий код: [`LbcTrinkets.java`](../src/main/java/me/skywars2/game/LbcTrinkets.java), [`LbcLoadouts.java`](../src/main/java/me/skywars2/game/LbcLoadouts.java), [`PlayerStore.java`](../src/main/java/me/skywars2/game/PlayerStore.java), [`GameConfig.java`](../src/main/java/me/skywars2/game/GameConfig.java), [`GameMenus.java`](../src/main/java/me/skywars2/game/GameMenus.java), каталог [`lbc_loadouts.json`](../src/main/resources/skywars2/lbc_loadouts.json), loot tables [`intercal.json`](../src/main/resources/data/lbc/loot_table/trinket/intercal.json) и [`gneiss_stone.json`](../src/main/resources/data/lbc/loot_table/trinket/gneiss_stone.json).
- Сверка старого поведения: [`TRINKETS_SOURCE_AUDIT.md`](TRINKETS_SOURCE_AUDIT.md) и [`lbc_items_arcana_trinkets.md`](lbc_items_arcana_trinkets.md).
- Старый LBC datapack, только для чтения: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`. Ключевые источники: `advancement/gowno.json` (прямое попадание без `arrow_only`), `function/items/swords.mcfunction`, `function/items/swords_get_trinket.mcfunction`, `function/items/damaged.mcfunction`, `function/items/damaged_get_trinket.mcfunction`, `function/items/trinket_effects/{intercal,intercal_hit,gneiss_stone,gneiss_stone_1,func_with_chance_universal}.mcfunction`, `loot_table/trinket/{intercal,gneiss_stone}.json`, `function/bedwars/game/start.mcfunction`, `function/bedwars/player/respawn.mcfunction` и общий `function/respawn.mcfunction`.
