# Исходный аудит каменного жезла `ststone`

Область: только read-only datapack `world/datapacksdisabled/LBC` старого сервера. Ссылки ниже даны относительно `E:/servers_mew/cringe1_21_4_skywars/`; файлы нового мода в этом аудите не менялись. Tiny Stone (`ststone_tiny`, `sauvojen_staffs_tiny`) — отдельный предмет и сюда не включён.

## Обычный жезл

Триггер — обычный spell1 для main hand и offhand (`function/workingitems/spell1_default.mcfunction:20`, `spell1_default_left.mcfunction:34`). Оба требуют item marker `ststone`, маны не меньше `ststone_wand_mana_use` (config:2000), отсутствия `stopper_magic` в радиусе 20; запуск позиционируется у глаз. `function/workingitems/sauvojen_staffs/stone.mcfunction:1-4` создаёт луч, списывает 2000 маны, затем обрабатывает полный mage-комплект и trinket cast-hook.

`stone_preloop.mcfunction:1-7` воспроизводит звук и создаёт marker с `lifetime:200`, `raycaststonewand`, `slowraycast`, `magic`; сохраняет поворот, `lbcID2` владельца и `team_number`. Функция `lbc:tick` подключена к tick tag (`data/minecraft/tags/function/tick.json:3`) и каждый игровой тик вызывает `tick_not_timestopped`; тот вызывает `raycastselector` для каждого `slowraycast` marker. `raycastselector.mcfunction:1` увеличивает Lifetime на 1, маршрутизирует `player` в `raycastselector_player.mcfunction:27`, а затем проверяет `data.lifetime` и завершает marker при достижении срока. `raycast_stone_wand.mcfunction:6` передвигает marker на 1 блок за этот вызов. У этого summon нет тега `marker_2t`, так что двухтиковый счётчик `markers_2t` к нему не относится. Фактический предел без столкновения — около 200 блоков за 200 активных тиков; тики, замороженные общей паузой, не считаются.

`lbcr/function/raycast_stone_wand.mcfunction:1-6`: на каждом шаге проверяет selector мобов с `distance=..15` и одновременно `dx=dy=dz=0` относительно текущей позиции marker; отдельно проверяет, что текущий блок не воздух; затем перемещает marker на 1 блок. Это комбинированный selector, поэтому `..15` нельзя трактовать как безусловное попадание по любому мобу в сфере 15 блоков; это также не дальность самого луча. При найденных мобах `stoningwithid.mcfunction:1-4` вызывает фильтр владельца/команды `dmgwithid_un_playeronly`, разрушает лучом цель только если после фильтра остались живые цели, затем чистит теги защиты. Собственник и союзники пропускаются; spectators также пропускаются. При попадании/столкновении `raycast_stone_kill.mcfunction:1-12` ставит четыре шаблона вокруг точки (air 11³, stone 9³, iron ore 7³, obsidian 5³), звук и удаляет raycast marker. Прямой урон этим путём не задан. Mob-check выполняется до проверки текущего блока; фактическое попадание определяется всей комбинацией selector-условий и фильтром владельца/команды, так что простая трактовка `distance=..15` как доступной сферы неверна.

## Academy-путь обычного `ststone`

Пользовательское уточнение для переноса (user): «Сохранить бедрок и шанс 20%».

Улучшение через способность жезла: при приседании `stone_preloop.mcfunction:7` вызывает `stone_preloop_upgrade.mcfunction:1-2` только если предмет в `SelectedItem` помечен `upgradable`. Она помещает bedrock в объём от `~1 ~2 ~1` до `~-1 ~-1 ~-1` (3×4×3) и с шансом 20% вызывает общий `other/magic_academy/can_upgrade`. Для обычного жезла общий путь попадает в `can_upgrade_book.mcfunction:1-6`: сообщает о возможности, ставит `upgrade_available:1` и снимает `upgradable`. NPC Academy проверяет/показывает предмет через `interacted.mcfunction:5-8` и `interacted_2.mcfunction:25`; успешный путь `interacted_upgrade.mcfunction:1-8,28` устанавливает общие `upgraded:1`, `mode:1`, добавляет upgrade description и вызывает `upgrade/ststone.mcfunction:1-2`, который выводит сообщение и применяет `lbc:tiers/a`.

Отдельный Academy trial/unlock — не эта случайная процедура улучшения. Advancement `advancement/true_advancements/legends/academy/stone.json:20-49` засчитывает наличие в инвентаре не-spectator игрока улучшенного `ststone` (с `transformable`, `staff`) и вызывает `swrg_kit_integration/gain/skill`; Academy GUI/выбор проверяет этот advancement (`function/swrg_kit_integration/gui/page/10100.mcfunction:17,52,66`, `gui/select/10100.mcfunction:11,29`). В старых источниках для самого `ststone` не найдено отдельных условий trial-попаданий/счётчика: criterion — наличие предмета.

## Улучшенные режимы

Spell1 upgraded routes (`spell1_upgraded.mcfunction:21`, `spell1_upgraded_left.mcfunction:21`) требуют `ststone`, минимум 2000 маны и отсутствия stopper_magic в 20 блоках; левый вариант ставит `left`. Обычная активация списывает 2000 в `stone_upgraded_pre_2.mcfunction:1-4`; приседание вместо каста меняет mode (`stone_upgraded_pre.mcfunction:1-6`, offhand analogue `stone_upgraded_pre_left.mcfunction:1-7`). Mode cycling общий 1→2→3→4→1 (`change_mode_4.mcfunction:1-6` и `_left:1-6`), выводимые имена: default, shield, bridge, spike.

- **1 default:** raycast вперёд по 0.5 блока за вызов, до Distance 300; каждый шаг проверяет selector мобов с `dx=dy=dz=0`, `distance=..10`, исключая caster/spectator, и текущий блок. Условие расстояния работает совместно с dx/dy/dz. При столкновении `raycast_stone_nokill` размещает air 11³, stone 9³, iron ore 7³, obsidian 5³ и останавливает луч (`stone_preloop_upgraded_default.mcfunction:1-6`, `lbcr/function/raycast_stone_nokill.mcfunction:1-10`).
- **2 shield:** даёт caster Resistance V на 5 секунд и частицы bedrock (`stone_preloop_upgraded_shield.mcfunction:1-2`).
- **3 bridge:** перед cast origin смещается вниз на 1 блок; выполняет 15 strict-fill сегментов по 3 блока вперёд и 3 блока в ширину, материалы идут от crying obsidian через каменные/рудные блоки к diorite (`stone_preloop_upgraded.mcfunction:7-10`, `stone_preloop_upgraded_bridge.mcfunction:1-15`).
- **4 spike:** такой же шаг/дальность, как default; при цели/блоке ставит spike template 10³ и наносит 5 arrow damage мобам в радиусе 5, исключая spectator/caster; source `lbcr/function/raycast_stone_nokill_dmg.mcfunction:1-9` (вызов — `stone_preloop_upgraded_spike.mcfunction:1-6`).

## Подозрительные места для переноса

- Сверка новой native-ветки на момент аудита: заряд движется по 0,5 блока/тик и удаляется на возрасте 200, то есть проходит не более примерно 100 блоков без попадания. Исходный slowraycast проходит 1 блок/активный тик с `lifetime:200` (около 200 блоков). Это конкретное сокращение дальности ordinary cast вдвое.

- Приседание с offhand-жезлом входит в `stone_preloop` из spell1-left, но проверка `upgradable` читает только `SelectedItem` (main hand). В переносе исправлено по ответу пользователя: «Да, проверять используемый жезл». Проверка медитации обеими руками прошла.
- Upgrade mode-change находится за общим mana gate ≥2000 в `spell1_upgraded*.mcfunction`; режим нельзя переключить при нехватке маны, хотя сам mode-change расход не делает.
- `stone_preloop_upgrade` ставит bedrock 3×4×3 вокруг cast-origin без восстановления. Пользователь подтвердил сохранение бедрока и шанса 20%; исходная механика перенесена без таймера удаления.
- Mob-check базового луча вызывается до block-check, но selector сочетает `distance=..15` с `dx=dy=dz=0`; форму фактического hitbox пересечения не сводить к сфере радиуса 15. При переносе цель должна быть отдельно зафиксирована и проверена.

## Точные источники

См. файлы, перечисленные по тексту выше, под `world/datapacksdisabled/LBC/data/{lbc,lbcr}`. Основные entry points: `lbc/function/workingitems/spell1_default*.mcfunction`, `spell1_upgraded*.mcfunction`; `lbc/function/workingitems/sauvojen_staffs/stone*.mcfunction`; `lbc/function/markers_2t.mcfunction`, `tick2t.mcfunction`, `other/areas.mcfunction`, `raycastselector_player.mcfunction`; `lbcr/function/raycast_stone*.mcfunction`, `stoningwithid.mcfunction`, `dmgwithid_un_playeronly.mcfunction`; Academy flow under `lbc/function/other/magic_academy/` and Academy advancement above.




