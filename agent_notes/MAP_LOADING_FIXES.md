# Исправления загрузки карт и клеток

## Клетка спавна

Старая функция [developer/bars.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/developer/bars.mcfunction) задаёт открытую клетку: четыре угловые колонны и центральная колонна удаляются. Остаются четыре стеклянные стойки на уровнях `y=0..3` и крестообразный пол из пяти блоков на `y=-1`; крыши нет. Проверка обоих `swrg.spawn` в реальном [1-10.nbt](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/maps/data/swrg/structure/1-10.nbt) показывает ровно те же относительные стеклянные позиции. Игровой старт [game/start/1.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/game/start/1.mcfunction) очищает объём `~-1 ~-1 ~-1` — `~1 ~4 ~1`, включая этот пол.

Текущая реализация `MapManager.cages` подменяет барьерами блоки стекла в указанной форме. При открытии клетки `restoreCages` удаляет временные барьеры и любое оставшееся стекло в полном стартовом объёме, поэтому NBT-пол не восстанавливается и не остаётся снизу. Менеджер не создаёт отсутствующие в шаблоне блоки.

## Загрузка и сохранение лобби

`MapManager.load(id, broken, callback)` уже неблокирующий API для полной загрузки выбранной карты: подготовка шаблонов идёт в single-thread executor, операции чтения NBT/разбиения проверяют `closed`, а запись мира и очередь срезов исполняются из `tick()` на серверном потоке. `tick()` ограничивает работу 4 мс или 64 срезами за вызов; чанки запрашиваются ticket futures и не ожидаются блокирующим `join`, пока future не завершён. Callback означает, что карта уже размещена и маркеры спавна собраны. `shutdown()` отменяет подготовку без interrupt текущего чтения ZipFS, ждёт исполнитель до 5 секунд, сохраняет journal и снимает chunk tickets.

Регионы теперь разделены в runtime/journal: карта остаётся в исходных manifest coordinates, лобби ставится на X+2048 и выдаёт Controller безопасную точку через `lobbyPosition()`. `loadPrepared` чистит только прежние map bounds/entities и сохраняет lobby bounds/entities; загрузка выбранной карты не переносит исходные позиции спавнов, look, worldborder и игровых событий. `lobby()` очищает прежние journal bounds и ставит лобби в его удалённый регион.

Controller вызывает `maps.load` из `prepareSelectedMap()` сразу после выбора карты, а также после восстановления лобби. Игроки остаются в `lobbyPosition()`; `begin()` использует уже размещённую карту. Готовность привязана к версии выбора: устаревший callback запускает последнюю выбранную карту, не разрешая старт предыдущей.

Сдвиг лобби на X+2048 оставляет все игровые карты в исходных координатах, в отличие от переноса арен; он в пределах journal limits (X/Z `-8192..8192`). Controller должен направлять lobby игроков в `lobbyPosition()` и возвращать их на обычные `maps.spawns()` только при старте. Selection-load вызывается при выборе GUI; Controller ждёт callback через generation token, оставляет игроков в лобби и разрешает start только после успешной загрузки всё ещё выбранной карты. При busy нужно запомнить последний выбор и запустить его после callback активной операции; устаревший callback не должен переключать матч. Ошибка загрузки должна оставить доступным лобби и запретить start.

## Источники реализации

- [MapManager.java](../src/main/java/me/skywars2/game/MapManager.java): `load`, `preloadTemplatesAsync`, `tick`, journal и cages.
- [GameController.java](../src/main/java/me/skywars2/game/GameController.java): выбор запускает `prepareSelectedMap`; `begin()` распределяет игроков по готовой карте. Клетки закрываются в countdown и открываются в `startPlaying()`.
- [карта 1](../src/main/resources/skywars2/maps/1.json): манифест содержит исходные origins; они не меняются.
- [maps_structures.json](inventory/maps_structures.json): структурный каталог исходных NBT, включая `1-10.nbt`.
