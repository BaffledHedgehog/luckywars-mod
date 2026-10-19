# Mana core checks

Добавлены проверки в `src/main/java/me/skywars2/game/GameChecks.java` и сценарии в `tools/smoke_lbc_loadouts.py`; также обновлены этот note и один неверный абзац о числе прибавок в `agent_notes/ARTIFACT_COLORS_DEPENDENCIES.md`.

`mana-core` проверяет сервис через живого тестового игрока в PLAYING: стартовые значения и food, accessors, signed `add`, недостаточный/отрицательный `trySpend`, обычный tick, cap, gear-формулу на vanilla ItemStack с исходными custom_data маркерами, boost 3/2 и уменьшение таймера, reverse drain/секундный таймер/возврат regen, HUD glyph/text/цвет, Creative, time_stopped, stopper-veto и Adventure observer. Native Raid Omen XVI в custom effect duration 2 удерживается при time_stopped; при первом допустимом player tick конвертируется в boost duration 1 даже под stopper magic, а regens после снятия stopper один раз с boost. Фикстуры armor и belt — временные, регистрируемых предметов не добавлял. Снимки ресурсов, inventory/cursor, эффектов, еды, режима и observer-статуса восстанавливаются через `finally`.

В конце `mana-core` запускает обычную смерть и отложенную проверку после respawn: mana/max/base regeneration сохраняются, boost/reverse очищаются. Check логирует PASS после проверки нового ServerPlayer. `mana-cleanup` отдельно проверяет, что boost/reverse остаются очищенными после возврата матча в lobby.

`tools/smoke_lbc_loadouts.py --mana-only --server-dir <isolated-server>` использует штатный fixture `bots`, запускает один матч для `mana-core`, останавливает его, дожидается lobby и вызывает `mana-cleanup`; результат пишет в `mana-results.json`. Для focused режима отчёт содержит три группы: bots, mana-core и mana-cleanup. Полный сценарий добавляет два smoke groups и один матч после остальных срезов; после полного сценария ожидается lobby map 54. Серверный класс, который использует smoke runner, должен быть предварительно собран пользователем.

Исследователь не запускал сборку и сервер. После интеграции основной агент
проверил build, адресный сценарий и весь regression suite на одном JAR:
18 матчей / 50 групп для предыдущей сборки ядра. Архив полного прогона —
`lbc-loadouts-before-mana-potions.log`/`-results.json`;
[актуальные результаты сборки с зельями](../SERVER_TESTS.md).
API `game.items().mana()` содержит
`get/set/maximum/baseRegen/regeneration/boost/reverse/trySpend/add/resetResources/clearEffects/tickPlayer/secondPlayer/hud`.
