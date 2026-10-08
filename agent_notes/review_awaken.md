# Независимая сверка Awaken Wand

Сверены все **277 источников** job `lbc_workingitems_awaken` по исходным функциям и вызывающим веткам. Основной документ: [lbc_workingitems_awaken.md](lbc_workingitems_awaken.md). Точный manifest прочитанных job paths находится в [inventory](inventory/lbc_workingitems_awaken_read_manifest.json). Все 277 функций содержат команды.

Во время независимого прохода подтверждены условия оплаты, основные trace/collision selectors, контексты `as`/`at`, lifetime поля и staged effects по семьям жезлов. Проверены полные повторяющиеся массивы `erecto` arc tags и `stonekivi` sun templates; в последнем локальная функция безусловно возвращает после подготовки macro `command="magmify"`, поэтому следующие 180+ template-команд в ней недостижимы при штатном вызове.

Исправлены два семантических уточнения основного текста:

- `acidcorrupt` mode `chuma` повышает `reverse_mana` только игрокам не в spectator и без тега `raycaster`; источник — [`mode_chuma_end`](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/awaken_tuntija_wands/acidcorrupt/mode_chuma_end.mcfunction).
- `psychoblind/click_upgraded_mode_default_1` не является пользовательским mode жезла: он используется активным `psychblind/doppel_tick` skill caller. Helper выпускает один charge с ID/team исполнителя и затем убивает именно executor-сущность. Описаны [caller](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/psychblind/doppel_tick.mcfunction) и [helper](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/awaken_tuntija_wands/psychoblind/click_upgraded_mode_default_1.mcfunction).

Таблица coverage сохранена без смены заголовков и содержит все 277 источников этого job. Независимый coverage-check пройден; он подтверждает ссылочную целостность и точные заголовки, но не заменяет эту смысловую сверку. Внешние `lbcr` обработчики, общие dispatcher-ы, шаблоны, команды Magic Academy и item bridges помечены в основном документе там, где их финальное действие не следует из этих 277 источников.