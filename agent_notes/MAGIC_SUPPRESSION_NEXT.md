# Следующий этап: провайдеры подавления магии

Исторический план после Ice Sword. Начальная форма артефакта и маяк теперь
перенесены; актуальное поведение и оптимизация — [MAGIC_PORT.md](MAGIC_PORT.md).
Результаты запусков — [SERVER_TESTS.md](../SERVER_TESTS.md).

## Источник предметов

- `custom_data {mystical:1,type:1}` — это начальный, ещё не раскрытый Мистический Артефакт. В `loot_table/artifact.json` он создаётся как `minecraft:repeating_command_block`, с `item_model: lbc:mysticalartifact`, `tooltip_style: lbc:tier_u` и данными `transformable:1, accessory:1, unresolved_artifact:1, mystical:1, type:1`. Имя — ключ `mystical_artifact`, цвет `#C28CFF`, без курсива; lore — `mystical_artifact.lore`, красный, без курсива. Тексты из read-only resource pack: RU «Мистический Артефакт» / «Неизвестный эффект»; EN “Mystical Artifact” / “Unknown effect”. Модель: `assets/lbc/items/mysticalartifact.json` → `lbc:item/mysticalartifact`; её модель использует `minecraft:item/mysticalartifact`.
- `nomagic_active` выставляется по наличию этого custom data, а не по надетой броне. `items/changed_inventory/sel.mcfunction` проверяет `container.*` и `weapon.*`: слоты основного инвентаря и hotbar, затем mainhand и offhand; `armor.*` в этой проверке нет. При нахождении подходящего предмета ставит тег, иначе снимает. `advancement/penis.json` запускает `sel` на `minecraft:inventory_changed`, исключая creative и измерения `nexus`/`imprinted`.
- `stopper_magic` — Panda Spawn Egg из `loot_table/stopper_magic.json`. При применении его `entity_data` задаёт marker и теги `stopper_magic`, `rc_1sec`, `marker_2t`. Предмет называется через `magic_breaker` (красный, без курсива); lore `magic_breaker.lore.1` и `.2` белый, без курсива. RU: «Антимагический Маяк» / «Блокирует магию в области вокруг себя» / «Не блокирует другие глушители.» EN: “Anti-Magic Beacon” / “Blocks magic in an area around itself” / “Does not block other jammers.” Стиль подсказки — `lbc:tier_b`, модель `lbc:stopper_magic`; пути пака: `assets/lbc/items/stopper_magic.json`, `assets/lbc/models/item/stopper_magic.json` (текстура `minecraft:item/stopper_magic`). `loot_table/stoppers.json` делает 6 бросков; в каждом magic stopper имеет вес 10 и count 3. Отдельная таблица `stopper_magic.json` выдаёт один предмет.

## Жизненный цикл старого marker

- `stopper_magic.json` задаёт `minecraft:marker` через `entity_data` spawn egg с тегами `stopper_magic`, `rc_1sec`, `marker_2t`. `tick2t.mcfunction` раз в 2 тика обходит такие marker; `markers_2t1.mcfunction` направляет `stopper_magic` в `other/stopper_magic.mcfunction`. В этом вызове `@s` остаётся самим marker.
- `other/stopper_magic.mcfunction` на каждом проходе очищает блоки тега `#minecraft:magic` в кубе от −15 до +15, кроме marker с `not_block`. Затем команды `execute as ... at @s` выбирают магических мобов, `#minecraft:magic_entities` и сущностей с тегом `magic` в радиусе 20 и запускают на каждой `stopper_magic_destroy.mcfunction`. Там `@s` уже цель: ей показываются witch-частицы и она удаляется. Marker функция не убивает.
- Для тега `rc_1sec` `tick1s.mcfunction` раз в секунду вызывает `raycasters_1s.mcfunction`. Пока marker не `activated`, вызывается `stopperactivate_magic.mcfunction`: ставится lightning rod на позицию marker, а при занятом блоке — на блок выше и marker переносится туда; после этого marker получает `activated`. Активный marker продолжает обрабатываться в 2-тиковом цикле и показывает частицы в секундном цикле. Сам marker остаётся центром постоянной области подавления; в лут-таблице для него нет `data.lifetime`.
- Если активированный marker оказывается не на lightning rod, `stopper_magic.mcfunction` вызывает `stopperbreaked_magic.mcfunction`. Та переключает контекст на ближайший выпавший rod для выдачи одного `loot lbc:stopper_magic` и удаления дропа, затем собственная команда `kill @s` удаляет исходный marker. Прежнее утверждение, что marker сохранялся, было ошибкой исследования контекста `@s`.
## Минимальный этап переноса

1. Зарегистрировать через Polymer только эти два предмета с исходной основой, моделями, текстами и данными. Для артефакта сохранить признак `mystical:1,type:1`; для маяка сохранить идентичность его специального spawn egg/предмета.
2. Поддерживать `nomagic_active` при фактических изменениях инвентаря: событием пересчитывать главный инвентарь, mainhand и offhand; пересчёт выполнять при входе игрока и после изменений. Не опрашивать всех игроков каждый тик. Не включать armor slots, поскольку их нет в старом условии.
3. Маяк реализован через учтённый Marker, родной lightning rod и событийную очередь блоков. Разрушение повторно проверяется в конце тика; один локальный rod-дроп возвращает одно яйцо и удаляет marker. Source тоже удалял marker; собственного исправления «вечного marker» здесь нет.
4. Ice Sword использует общую `LbcMagic.suppressed`; нативные провайдеры уже поддержаны. Следующие этапы — раскрытие артефакта/его другие типы и остальные заклинания. Движок посохов и общий Lucky-лут пока отдельно.

## Ключевые источники

Старый datapack: `data/lbc/loot_table/artifact.json`, `stopper_magic.json`, `stoppers.json`; `data/lbc/function/items/changed_inventory/sel.mcfunction`, `other/stopperactivate_magic.mcfunction`, `other/stopper_magic.mcfunction`, `other/stopper_magic_destroy.mcfunction`, `other/stopperbreaked_magic.mcfunction`, `other/stopperbreaked_magicasitem.mcfunction`, `markers_2t.mcfunction`, `markers_2t1.mcfunction`, `tick2t.mcfunction`, `tick1s.mcfunction`, `raycasters_1s.mcfunction`; `data/lbc/advancement/penis.json`. Тексты и модели сверены только для чтения в `all_in_one_1.21.8/assets/minecraft/lang/{ru_ru,en_us}.json` и `assets/lbc/{items,models/item}`.



