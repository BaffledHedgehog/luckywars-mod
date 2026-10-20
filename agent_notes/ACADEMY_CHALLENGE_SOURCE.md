# Academy: испытание и наградной набор — источник, 20.10.2026

Проверка исходников, без запуска сервера и без изменения Java. Корни ниже: `OLD = E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled`, `L = OLD/LBC/data/lbc`, `S = OLD/swarogskywars/data/swrg`. Все пути в этом документе относительно этих корней.

## Два независимых выбора

`lbc.challenge=1` — активное испытание «без знаний»; `challenge_score` — его прогресс. `lbc.challenge.kit=1` — выбранная награда Academy. Нельзя переиспользовать `PlayerStore.Profile.lbcChallenge` одновременно для обоих: нынешняя кнопка reset24 соответствует активному испытанию. Предыдущий ACQUISITION_AUDIT упрощает это различие.

`L/function/swrg_kit_integration/gui/select/12000.mcfunction:6–8`: slot0 при academy/root и отсутствии no_knowledge выбирает активное испытание; при наличии no_knowledge выбирает наградной набор. В ranked `select/12000_ranked.mcfunction:1` сразу выбирает набор, без проверок достижений. Само получение награды не перепроверяет advancement: проверка доступа выполняется при выборе.

Reset24 (`select/12000.mcfunction:18–19`, ranked:6–7) очищает активное испытание и progress. Reset25 (:21–29, ranked:9–17) очищает выбранные challenge skill/kit/levelups/trinkets; активное испытание и достижения сохраняет. Для нынешней ограниченной реализации reset25 должен очищать отдельный выбор Academy reward, не обычные наборы/навыки.

Особенность исходника: select12000:6 сначала ставит active=1, затем :7 требует unless active=1 для reset progress. Поэтому этот reset недостижим при нормальном выборе. Повторный выбор фактически сохраняет progress; явный reset24 обнуляет.

## Испытание

`L/function/players_tick.mcfunction:47` при active=1 вызывает `swrg_kit_integration/game/challenges/no_knowledge/tick.mcfunction`: только отправка большого числа пустых строк, скрывающих чат. `L/function/items/changed_inventory/sel.mcfunction:153` вызывает соседний `inventory_change.mcfunction`: item modifier для container.0…53. `L/item_modifier/remove_name_lore.json` заменяет имя и lore строкой вопросительных знаков и отключает tooltip attributes/enchantments/trim/unbreakable и др. Это изменение самих стеков в legacy, обратного восстановления здесь нет.

`L/function/other/magic_academy/interacted_upgrade.mcfunction:6–7`: успешное повышение Academy увеличивает progress активного испытания на 1; при >=20 вызывает `completed_challenge.mcfunction`, который grant no_knowledge, progress=0, active=0. Достижение `L/advancement/challenges/no_knowledge.json` имеет impossible criterion и reward function `swrg_kit_integration/gain/challenge_completed` (title/subtitle). Автовыбора kit reward после завершения нет: игрок выбирает его в GUI. Счётчик привязан к успешным повышениям, не убийствам/победам и не уникальности типов жезлов.

## Выдача и жизненный цикл

`L/function/swrg_kit_integration/game/kit_select/challenge_kit_select.mcfunction:12` даёт `lbc:kit/academy_token` при kit=1. Полная обёртка `game/challenge_content_select.mcfunction` вызывает challenge kit/levelup/trinket; отдельного challenge skill в ней нет.

Все найденные входы:
- `S/function/game/start/1.mcfunction:16` и `developer/game/start/1.mcfunction:15`: при #lbc.challenge=1 обёртка выполняется as @a, до game_started=1. Обычный start вызывается timer; developer timer тоже назначает обычный start/1. Дополнительный прямой start/1 есть в `L/function/workingitems/supernovacore_newworld.mcfunction:69`.
- `L/function/bedwars/game/start.mcfunction:23`: та же обёртка/условие, затем game_started=1.
- `L/function/respawn.mcfunction:5`: повторно выдаёт challenge kit при #lbc.challenge>=1. Входы — `S/function/game/resp.mcfunction:1` и `core/player/respawn.mcfunction:1`; core/player/respawn_more вызывает последний. Game tick:11 вызывает game/resp для dead. Legacy game/resp очищает dead до условного core respawn, поэтому его последняя ветка недостижима; это уже известная сломанная respawn-цепь, не основание отменять явную повторную выдачу kit.

Рекомендация: native `LbcLoadouts.apply` должен выдавать выбранный Academy token при старте И при реальном respawn с оставшимися жизнями. Observer исключать по user-контракту. Не выдавать при входе в лобби/переходе в наблюдателя. Существующую native Academy token/placement переиспользовать.

## Настройка

Независимый переключатель `#lbc.challenge` по умолчанию 1: `S/function/core/default_settings.mcfunction:19`. GUI общих настроек page1000 slot22: `gui/select/1000.mcfunction:97–99`, 0/1; обычный игрок переключает лишь если нет admin, admin может сам. Дизайн состояния: `gui/page/1000-lang.mcfunction:141–142`. Этот флаг независим от #lbckit/#lbcskill/#lbclevelup. Он управляет выдачей challenge rewards, а не доступностью выбора или исполнением самого no_knowledge. В нынешнем GameConfig отдельного флага пока нет; lbcKits заменой ему не является.

## GUI и статистика

`L/function/swrg_kit_integration/gui/page/12000.mcfunction`: slot0 до завершения — loot `gui/page/12000/0.json`, model lbc:no_knowledge, name lbcchallenge.0, lore включает challenge_score/20; после — `0_completed.json`, model lbc:advancement_icon/academy, name lbcchallenge.kit.0, reward lore + pick/win rates/counts. Выделение swrg:select если active=1 ИЛИ kit=1. Без academy/root в конце безусловно подменяется locked repeating_command_block, model lbc:locked + `L/item_modifier/swrg_unlocks/tweak_academy.json` (gui.locked/gui.for_open/lbc.adv.academy_upgraded). Поэтому вручную выданный no_knowledge без root функционально выбирает reward, но GUI скрывает его замком: legacy oddity. Ranked показывает completed независимо от root/no_knowledge и выделяет только kit. Slot18 back,24 reset_challenge,25 reset_challenge_perks.

Статистика: challenge_kit_select:1–10 вычисляет x_kit=kit−1 и вызывает `gui/stats_x_kit_add_pick` только когда game_started !=1. Значит обычный respawn повторяет предмет, но не pick. При kit=0 тоже пишется индекс −1 — legacy oddity; для native считать только реальный выбранный reward. `L/function/win.mcfunction:1` -> `gui/stats_grant_wins.mcfunction:19–23` -> stats_x_kit_add_win, тоже kit−1. Функции хранят личные и общие pick/win counters в stats:stats; `gui/stats_refresh_x_kit` подготавливает lore. `L/function/only_end_resets.mcfunction:2` сбрасывает game_started, а не постоянный выбор reward. Native OriginalGuiDesign пока возвращает пустую строку для challenge_score (строка613), поэтому простого открытия таблицы недостаточно.

## Рекомендуемая граница переноса

Связная пачка: отдельные persistent active/progress/reward, две функции slot0 (trial/reward), ranked bypass, reset24/reset25, исходные три визуальных состояния и выделение, progress успешных Academy upgrades до20, native advancement, независимая настройка rewards default=true, выдача start+respawn, реальные reward picks/wins. Скрытие знаний лучше делать представлением получателя через существующий Polymer tooltip pattern, чтобы не уничтожать имена предметов навсегда. Спам чата и недостижимые ветки — особенности legacy, их буквальное воспроизведение требует отдельного решения владельца. Новые RP модели не нужны: дизайн уже в gui_design.json, token уже native.
