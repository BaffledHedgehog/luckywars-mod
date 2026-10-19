# Stfire — источник для переноса

Исследован legacy datapack `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc`. Это аудит исходника, не свидетельство фактически запущенного теста.

## Обычный посох

- Каст маршрутизируется из `function/workingitems/spell1_default.mcfunction` (main hand) и `spell1_default_left.mcfunction` (offhand) в `function/workingitems/sauvojen_staffs/fire.mcfunction`; оба проверяют mana `>= stfire_wand_mana_use` и отсутствие `stopper_magic` в радиусе 20. `function/config.mcfunction` задаёт цену 100 маны. `fire.mcfunction` вызывает `fire_preloop`, затем списывает цену; полный комплект мага может увеличить manamax, но не меняет цену.
- `fire_preloop.mcfunction` создаёт три `small_fireball`: по центру и со смещением ±0.15 по локальной X. Их Motion берётся от marker на дистанции 2.5; значит это вектор скорости длиной примерно 2.5 блока/тик (не нормализуется в этих командах). Шары помечены `stwandfireball`, `can_deleted`; каст также издаёт звук и ставит лаву вокруг позиции.
- Upgraded cast вызывается из `spell1_upgraded.mcfunction` / `_left.mcfunction` → `fire_upgraded_pre.mcfunction` / `_left.mcfunction` → `_pre_2.mcfunction`. Цена та же 100. Режим сохраняется в custom_data `mode`: 1 default, 2 big, 3 concentrated; sneak переключает режим через `magic_academy/change_mode_3` (и `_left`).
- Upgraded default — те же три малых шара, скорость ~2.5 (`fire_preloop_upgraded_default.mcfunction`). Big — один `minecraft:fireball`, `ExplosionPower:2`, скорость ~2.5 (`fire_preloop_upgraded_big.mcfunction`). Concentrated — рекурсивная трасса с шагом 0.5 до Distance 200, то есть до ~100 блоков, с остановкой при мобе в текущем блоке или непрозрачном блоке (`fire_preloop_upgraded_concentrated.mcfunction`). Каждый шаг проигрывает звук и частицы; только конечная функция `fire_preloop_upgraded_concentrated_end.mcfunction` ставит лаву в кубе ±0.3 вокруг конечной позиции. Это не обычный снаряд; отдельного срока жизни тут нет.

## Попадания, owner, shield и lifetime

- Прямой cast chain из `spell1_default*.mcfunction` → `fire.mcfunction` → `fire_preloop.mcfunction` и соответствующие upgraded функции создаёт нативные `small_fireball`/`fireball`, задаёт Motion через marker и ставит tags `stwandfireball`, `can_deleted`. В этой цепочке нет явной записи owner/`lbcID2`/team, shield callback/tag или собственного срока жизни. Это только то, что видно в этих функциях; поведение нативных projectile после вызова здесь отдельно не аудировал.
- Не включаю `other/raycastfireball*` и byid helpers в эту цепочку: поиск usages показал их отдельные вызовы, но прямые spell1 функции посоха их не вызывают.
## Academy unlock/upgrade и условие

- Academy interaction routing: `function/other/magic_academy/interacted_2.mcfunction` выбирает `can_upgrade/stfire` для предмета с custom_data `stfire:1`; `interacted_upgrade.mcfunction` вызывает `upgrade/stfire`.
- Academy `can_upgrade/stfire.mcfunction` показывает подсказку `upgrade_condition_stfire` («Spend some time in meditation (Shift + RMB)…»). Само получение доступности проходит через `function/other/magic_academy/can_upgrade.mcfunction`: он вызывает `can_upgrade_book.mcfunction`, который выставляет `upgrade_available:1` и сбрасывает `upgradable:0`. В обычном Academy routing `interacted.mcfunction` сначала отправляет предмет без `upgradable`, `upgrade_available` и `upgraded` в `interacted_2`, где показывается stfire condition.
- Фактический путь trial для stfire: первое Academy взаимодействие через `interacted_2` выводит условие и ставит `upgradable:1`; затем Shift+RMB вызывает в `fire_preloop.mcfunction` условие `sneak && upgradable` → `fire_preloop_upgrade.mcfunction`, где с predicate `chance5` вызывается общий `can_upgrade`, тем самым выставляя `upgrade_available:1`. Следующее Academy взаимодействие (`interacted.mcfunction`) при наличии этого флага направляет в `interacted_upgrade`; общий upgrade выставляет `upgraded:1`, `mode:1`, item modifier и challenge progress; `upgrade/stfire.mcfunction` показывает success. Иными словами, trial — медитировать с посохом (Shift+RMB) до 5%-ного успешного броска на каст, не попадания/убийства. В коде нет накопительного таймера или счётчика попаданий.


## Учёт при переносе

1. Trial — исходный шанс 5% на каждом Shift+RMB после подготовки предмета в Академии, без накопления прогресса. Этот механизм сохранён, нового таймера не добавлено.
2. Owner и защита союзников добавлены по общим пользовательским требованиям. Собственный lifetime и обычный магический щит для этого жезла исходником не подтверждены: новые ограничения не вводились. Уже существующий антимагический маяк распознаёт vanilla fireball-типы через `magic_entities`.
3. Финальная лава concentrated сохранена. Рекурсия заменена общим DDA и одной проверкой целей; первое столкновение завершает луч согласно общему уточнению пользователя. Детали и границы проверки — [FIRE_WAND_PORT.md](FIRE_WAND_PORT.md).

## Ключевые ссылки

- `function/workingitems/sauvojen_staffs/fire.mcfunction`
- `function/workingitems/sauvojen_staffs/fire_preloop.mcfunction`
- `function/workingitems/sauvojen_staffs/fire_preloop_upgraded*.mcfunction`
- `function/workingitems/sauvojen_staffs/fire_upgraded_pre*.mcfunction`
- `function/workingitems/spell1_default*.mcfunction`, `spell1_upgraded*.mcfunction`
- `function/config.mcfunction`
- `function/other/magic_academy/{interacted,interacted_2,interacted_upgrade}.mcfunction`
- `function/other/magic_academy/can_upgrade/stfire.mcfunction`, `upgrade/stfire.mcfunction`, `can_upgrade.mcfunction`, `can_upgrade_book.mcfunction`
- `function/other/magic_academy/interacted_upgrade.mcfunction` (общий dispatcher)
- `plugins/luckywars/resourcepack_lang/en_us.json` (`upgrade_condition_stfire`)




