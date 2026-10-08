# LBC: атрибуты, таблетки и преобразования инвентаря

Задание `lbc_other_attributes` охватывает 331 точный источник. Основные входы — advancement с триггером употребления предмета, функции-посредники атрибутов, старые эффекты экипировки и обработчики передачи предметов из инвентаря в выброшенные сущности. Реальные идентификаторы и числа приведены в таблицах; названия переводов из subtitle не считаются описанием эффекта.

## Фактические механики и важные края

Обычные обработчики меняют **base** значения Minecraft-атрибутов, не добавляя временный modifier: читают `base get S`, кладут число в целочисленный scoreboard, прибавляют или вычитают дельту и передают результат в парный `*_update`, который делает `base set`. `S` различается по функции; результат `base get S` — `trunc(base × S)`, то есть дробная часть отбрасывается к нулю. Для неотрицательной базы это совпадает с floor, для отрицательной — нет. Шаг `delta/S` относится к изменению уже квантованного числа, а не гарантирует такую абсолютную дельту от прежней дробной базы. 26 `*_up_godclaw` helpers получают `tmp1` от SWRG caller: `god_claw_active` пишет в него результат селектора `@e[type=minecraft:marker,tag=my_godclaw]`, то есть количество совпавших marker-ов, а не boolean; нулевой результат завершает навык. Значение используется как дельта с собственным scale. Проверка знака по файлам показывает, что имена `_up` у burning_time и fall_damage_multiplier фактически вычитают `tmp1`.

Таблетки — расходуемые cookies с `custom_data` вида `{pill:N}` и advancement `minecraft:consume_item`. Выдача/хэш-таблица находится в отдельном loot контракте; использование исключено в `minecraft:nexus` и `minecraft:imprinted`, а функция после эффекта отзывает advancement для следующего употребления. Это не гарантирует полезный результат: набор включает отрицательные status effects; например, `raid_omen_17` добавляет 5000 mana, `raid_omen_18` — 1000 manamax, `raid_omen_19` — 3 manaregen, а `raid_omen_74` и варианты `raid_omen_101`, `102`, `103`, `104`, `107`–`109`, `118`, `119`, `121`–`124` увеличивают свой счётчик effect_* и включают `have_lucky_effects` (у `raid_omen_103` дополнительно два периодических флага). Эти функции только взводят счётчики; фактическое периодическое действие выполняют внешние effect-handlers и здесь не приписывается pill-функции. `i_found_pills` лишь показывает subtitle/звук и отзывает advancement. Конечные runtime эффекты счётчиков `effect_*` после употребления перечислены в [документе об эффектных диспетчерах](lbc_effects.md#счётчики-эффектов-попадания-и-мира); таблица здесь фиксирует именно то, что pill-функции начисляют и какой обслуживающий tag ставят. В эффектных вариантах присутствуют буквальные amplifier до127/99. Точные длительности, ID и результаты перечислены в таблице ниже; attribute pills используют те же числовые handlers из первого раздела.

41 функция `drop_inv` копирует стек из одного конкретного источника в новый item entity с `PickupDelay:40` тиков (2 секунды), но сам helper не очищает слот. Реальный caller `projectiles/gadgets/honew_2` запускает `honey_pl` как ближайший nonspectator player, и тот создаёт копии для36 inventory slots, offhand и четырёх armor slots, после чего очищает инвентарь и экипировку вызывающего игрока. Поэтому итог зависит от caller; ручной вызов отдельного helper оставляет исходный stack на месте и создаёт дубликат.

`random_attrs_up` выбирает ровно один результат из26 равновероятных номеров; два из них уменьшают burning_time и fall_damage_multiplier. `random_attrs_down` независимо бросает шанс50% для каждого из восьми обработчиков, так что один вызов может применить от0 до8 уменьшений. Случайные base-замены для attack_damage/max_health/movement_speed выбирают целое `1..N` из сохранённого значения, после чего записывают `0.001 × random`; N задаётся как `trunc(base×3000)`, `trunc(base×2500)` и `trunc(base×4000)` соответственно. Они дают nausea 3с и blindness 2с и сбрасывают advancement, но не добавляют/не отнимают отдельный фиксированный шаг.

`reduce_hp_armor` запускается из SWRG skill tick как каждый игрок в радиусе1; функция делает три отдельные попытки вычитания2 и после каждой сразу обновляет base. Первые уменьшают max_health и armor; третья читает `armor_toughness`, но вызывает `armor_update`, поэтому фактически переписывает armor из toughness−2 и не меняет toughness. `new_game/click` — отдельный игровой reset по `item_balance:1`: отключает drops, перемещает/убивает неигровые сущности кроме `supernovacore`, очищает инвентари/эффекты игроков в текущем измерении, сбрасывает состояние и расписания, передаёт очистку карты/шаблоны во внешний SWRG loader и запускает legacy schedulers. Legacy-циклы повторяются раз в тик, секунду,5 секунд и5 тиков и сами себя перепланируют без проверки флага `legacy`; они обслуживают старые repeating-command-block предметы в инвентарях/экипировке игроков и снижают набор cooldown scores. В цикле `legacy_tick_5t_player` каждому nonspectator игроку каждые5 тиков убирается5 из восьми cooldown-score; при старом `manabelt_old` добавочно убирается2, при старом шлеме `magichat_old` —4 ещё из семи score (wiki отсутствует в belt/hat helpers). Циклы не завершаются изнутри: внешний `end_resets` явно очищает их четыре расписания. Вызовы `swrg:*` и четыре SWRG structure templates являются границей другого datapack, поэтому их внутренний результат здесь не приписан LBC.

## Изменение базовых атрибутов

Обычные обработчики читают текущий `attribute @s ... base get S` в целочисленный scoreboard `tmp`, прибавляют/вычитают дельту и вызывают парную функцию `*_update` с `storage minecraft:lbcmath`; update применяет macro `$attribute @s ... base set $(tmp)`. Следовательно, изменение к базе равно указанному числу scoreboard units / S. Функции `*_up_godclaw` используют значение `tmp1` вместо постоянного шага; это динамическая формула, и размер задаётся вызывающим кодом. Если есть advancement revoke, функция сбрасывает его для повторного срабатывания, но сама не удаляет предмет.

| Функция | Атрибут | Scale S | Изменение в tmp | Итог к базе | Update | Advancement reset |
|---|---|---:|---|---|---|---|
| `armor` | `armor` | 1 | `+2` | `+2` | `armor_update` | `lbc:armorattr` |
| `armor_1` | `armor` | 1 | `+1` | `+1` | `armor_update` | `—` |
| `armor_down` | `armor` | 1 | `-1` | `-1` | `armor_update` | `—` |
| `armor_toughness` | `armor_toughness` | 1 | `+1` | `+1` | `armor_toughness_update` | `lbc:armor_toughnessattr` |
| `armor_toughness_down` | `armor_toughness` | 1 | `-1` | `-1` | `armor_toughness_update` | `—` |
| `armor_toughness_up` | `armor_toughness` | 1 | `+1` | `+1` | `armor_toughness_update` | `—` |
| `armor_toughness_up_godclaw` | `armor_toughness` | 100 | `+ tmp1` | `+ tmp1 / 100` | `armor_toughness_update` | `—` |
| `armor_toughness_update` | `armor_toughness` | — | `прямой дельты атрибута нет` | `прямой дельты атрибута нет` | `armor_toughness_update` | `—` |
| `armor_up` | `armor` | 1 | `+2` | `+2` | `armor_update` | `—` |
| `armor_up_godclaw` | `armor` | 100 | `+ tmp1` | `+ tmp1 / 100` | `armor_update` | `—` |
| `armor_update` | `armor` | — | `прямой дельты атрибута нет` | `прямой дельты атрибута нет` | `armor_update` | `—` |
| `attack_damage` | `attack_damage` | 1 | `+1` | `+1` | `attack_damage_update` | `lbc:attack_damageattr` |
| `attack_damage_down` | `attack_damage` | 1 | `-1` | `-1` | `attack_damage_update` | `—` |
| `attack_damage_up` | `attack_damage` | 1 | `+1` | `+1` | `attack_damage_update` | `—` |
| `attack_damage_up_godclaw` | `attack_damage` | 1000 | `+ tmp1` | `+ tmp1 / 1000` | `attack_damage_update` | `—` |
| `attack_damage_up_tiny` | `attack_damage` | 10 | `+1` | `+0.1` | `attack_damage_update` | `—` |
| `attack_damage_update` | `attack_damage` | — | `прямой дельты атрибута нет` | `прямой дельты атрибута нет` | `attack_damage_update` | `—` |
| `attack_knockback_down` | `attack_knockback` | 10 | `-1` | `-0.1` | `attack_knockback_update` | `—` |
| `attack_knockback_up` | `attack_knockback` | 10 | `+1` | `+0.1` | `attack_knockback_update` | `—` |
| `attack_knockback_up_godclaw` | `attack_knockback` | 1000 | `+ tmp1` | `+ tmp1 / 1000` | `attack_knockback_update` | `—` |
| `attack_knockback_update` | `attack_knockback` | — | `прямой дельты атрибута нет` | `прямой дельты атрибута нет` | `attack_knockback_update` | `—` |
| `attack_speed` | `attack_speed` | 5 | `+1` | `+0.2` | `attack_speed_update` | `lbc:attack_speedattr` |
| `attack_speed_down` | `attack_speed` | 5 | `-1` | `-0.2` | `attack_speed_update` | `—` |
| `attack_speed_up` | `attack_speed` | 5 | `+1` | `+0.2` | `attack_speed_update` | `—` |
| `attack_speed_up_godclaw` | `attack_speed` | 10000 | `+ tmp1` | `+ tmp1 / 10000` | `attack_speed_update` | `—` |
| `attack_speed_update` | `attack_speed` | — | `прямой дельты атрибута нет` | `прямой дельты атрибута нет` | `attack_speed_update` | `—` |
| `block_break_speed_down` | `block_break_speed` | 10 | `-1` | `-0.1` | `block_break_speed_update` | `—` |
| `block_break_speed_up` | `block_break_speed` | 10 | `+1` | `+0.1` | `block_break_speed_update` | `—` |
| `block_break_speed_up_godclaw` | `block_break_speed` | 1000 | `+ tmp1` | `+ tmp1 / 1000` | `block_break_speed_update` | `—` |
| `block_break_speed_update` | `block_break_speed` | — | `прямой дельты атрибута нет` | `прямой дельты атрибута нет` | `block_break_speed_update` | `—` |
| `block_interaction_range_down` | `block_interaction_range` | 5 | `-1` | `-0.2` | `block_interaction_range_update` | `—` |
| `block_interaction_range_up` | `block_interaction_range` | 5 | `+1` | `+0.2` | `block_interaction_range_update` | `—` |
| `block_interaction_range_up_godclaw` | `block_interaction_range` | 10000 | `+ tmp1` | `+ tmp1 / 10000` | `block_interaction_range_update` | `—` |
| `block_interaction_range_update` | `block_interaction_range` | — | `прямой дельты атрибута нет` | `прямой дельты атрибута нет` | `block_interaction_range_update` | `—` |
| `burning_time_down` | `burning_time` | 10 | `-1` | `-0.1` | `burning_time_update` | `—` |
| `burning_time_up` | `burning_time` | 10 | `+1` | `+0.1` | `burning_time_update` | `—` |
| `burning_time_up_godclaw` | `burning_time` | 10000 | `- tmp1` | `- tmp1 / 10000` | `burning_time_update` | `—` |
| `burning_time_update` | `burning_time` | — | `прямой дельты атрибута нет` | `прямой дельты атрибута нет` | `burning_time_update` | `—` |
| `entity_interaction_range_down` | `entity_interaction_range` | 5 | `-1` | `-0.2` | `entity_interaction_range_update` | `—` |
| `entity_interaction_range_up` | `entity_interaction_range` | 5 | `+1` | `+0.2` | `entity_interaction_range_update` | `—` |
| `entity_interaction_range_up_godclaw` | `entity_interaction_range` | 10000 | `+ tmp1` | `+ tmp1 / 10000` | `entity_interaction_range_update` | `—` |
| `entity_interaction_range_update` | `entity_interaction_range` | — | `прямой дельты атрибута нет` | `прямой дельты атрибута нет` | `entity_interaction_range_update` | `—` |
| `explosion_knockback_resistance_down` | `explosion_knockback_resistance` | 10 | `-1` | `-0.1` | `explosion_knockback_resistance_update` | `—` |
| `explosion_knockback_resistance_up` | `explosion_knockback_resistance` | 10 | `+1` | `+0.1` | `explosion_knockback_resistance_update` | `—` |
| `explosion_knockback_resistance_up_godclaw` | `explosion_knockback_resistance` | 100000 | `+ tmp1` | `+ tmp1 / 100000` | `explosion_knockback_resistance_update` | `—` |
| `explosion_knockback_resistance_update` | `explosion_knockback_resistance` | — | `прямой дельты атрибута нет` | `прямой дельты атрибута нет` | `explosion_knockback_resistance_update` | `—` |
| `fall_damage_multiplier_down` | `fall_damage_multiplier` | 10 | `-1` | `-0.1` | `fall_damage_multiplier_update` | `—` |
| `fall_damage_multiplier_up` | `fall_damage_multiplier` | 10 | `+1` | `+0.1` | `fall_damage_multiplier_update` | `—` |
| `fall_damage_multiplier_up_godclaw` | `fall_damage_multiplier` | 100000 | `- tmp1` | `- tmp1 / 100000` | `fall_damage_multiplier_update` | `—` |
| `fall_damage_multiplier_update` | `fall_damage_multiplier` | — | `прямой дельты атрибута нет` | `прямой дельты атрибута нет` | `fall_damage_multiplier_update` | `—` |
| `gravity_down` | `gravity` | 100 | `-1` | `-0.01` | `gravity_update` | `—` |
| `gravity_up` | `gravity` | 100 | `+1` | `+0.01` | `gravity_update` | `—` |
| `gravity_update` | `gravity` | — | `прямой дельты атрибута нет` | `прямой дельты атрибута нет` | `gravity_update` | `—` |
| `jump_strength_down` | `jump_strength` | 10 | `-1` | `-0.1` | `jump_strength_update` | `—` |
| `jump_strength_up` | `jump_strength` | 10 | `+1` | `+0.1` | `jump_strength_update` | `—` |
| `jump_strength_up_godclaw` | `jump_strength` | 10000 | `+ tmp1` | `+ tmp1 / 10000` | `jump_strength_update` | `—` |
| `jump_strength_up_tiny` | `jump_strength` | 1000 | `+1` | `+0.001` | `jump_strength_update` | `—` |
| `jump_strength_update` | `jump_strength` | — | `прямой дельты атрибута нет` | `прямой дельты атрибута нет` | `jump_strength_update` | `—` |
| `knockback_resistance` | `knockback_resistance` | 10 | `+1` | `+0.1` | `knockback_resistance_update` | `lbc:knockback_resistanceattr` |
| `knockback_resistance_down` | `knockback_resistance` | 10 | `-10` | `-1` | `knockback_resistance_update` | `—` |
| `knockback_resistance_up` | `knockback_resistance` | 10 | `+10` | `+1` | `knockback_resistance_update` | `—` |
| `knockback_resistance_up_godclaw` | `knockback_resistance` | 100000 | `+ tmp1` | `+ tmp1 / 100000` | `knockback_resistance_update` | `—` |
| `knockback_resistance_update` | `knockback_resistance` | — | `прямой дельты атрибута нет` | `прямой дельты атрибута нет` | `knockback_resistance_update` | `—` |
| `luck` | `luck` | 1 | `+1` | `+1` | `luck_update` | `lbc:luckattr` |
| `luck_down` | `luck` | 1 | `-1` | `-1` | `luck_update` | `—` |
| `luck_up` | `luck` | 1 | `+1` | `+1` | `luck_update` | `—` |
| `luck_up_godclaw` | `luck` | 5000 | `+ tmp1` | `+ tmp1 / 5000` | `luck_update` | `—` |
| `luck_update` | `luck` | — | `прямой дельты атрибута нет` | `прямой дельты атрибута нет` | `luck_update` | `—` |
| `max_health` | `max_health` | 1 | `+2` | `+2` | `max_health_update` | `lbc:max_healthattr` |
| `max_health_1` | `max_health` | 1 | `+1` | `+1` | `max_health_update` | `—` |
| `max_health_down` | `max_health` | 1 | `-1` | `-1` | `max_health_update` | `—` |
| `max_health_down_eight` | `max_health` | 1 | `-8` | `-8` | `max_health_update` | `—` |
| `max_health_down_two` | `max_health` | 1 | `-2` | `-2` | `max_health_update` | `—` |
| `max_health_up` | `max_health` | 1 | `+2` | `+2` | `max_health_update` | `—` |
| `max_health_up_eight` | `max_health` | 1 | `+8` | `+8` | `max_health_update` | `—` |
| `max_health_up_godclaw` | `max_health` | 1000 | `+ tmp1` | `+ tmp1 / 1000` | `max_health_update` | `—` |
| `max_health_update` | `max_health` | — | `прямой дельты атрибута нет` | `прямой дельты атрибута нет` | `max_health_update` | `—` |
| `mining_efficiency_down` | `mining_efficiency` | 3 | `-1` | `-0.3333333333333333` | `mining_efficiency_update` | `—` |
| `mining_efficiency_up` | `mining_efficiency` | 3 | `+1` | `+0.3333333333333333` | `mining_efficiency_update` | `—` |
| `mining_efficiency_up_godclaw` | `mining_efficiency` | 100 | `+ tmp1` | `+ tmp1 / 100` | `mining_efficiency_update` | `—` |
| `mining_efficiency_update` | `mining_efficiency` | — | `прямой дельты атрибута нет` | `прямой дельты атрибута нет` | `mining_efficiency_update` | `—` |
| `movement_efficiency_down` | `movement_efficiency` | 10 | `-1` | `-0.1` | `movement_efficiency_update` | `—` |
| `movement_efficiency_up` | `movement_efficiency` | 10 | `+1` | `+0.1` | `movement_efficiency_update` | `—` |
| `movement_efficiency_up_godclaw` | `movement_efficiency` | 100 | `+ tmp1` | `+ tmp1 / 100` | `movement_efficiency_update` | `—` |
| `movement_efficiency_update` | `movement_efficiency` | — | `прямой дельты атрибута нет` | `прямой дельты атрибута нет` | `movement_efficiency_update` | `—` |
| `movement_speed` | `movement_speed` | 100000 | `+2000` | `+0.02` | `movement_speed_update` | `lbc:movement_speedattr` |
| `movement_speed_down` | `movement_speed` | 100000 | `-2000` | `-0.02` | `movement_speed_update` | `—` |
| `movement_speed_up` | `movement_speed` | 100000 | `+2000` | `+0.02` | `movement_speed_update` | `—` |
| `movement_speed_up_godclaw` | `movement_speed` | 100000 | `+ tmp1` | `+ tmp1 / 100000` | `movement_speed_update` | `—` |
| `movement_speed_up_tiny` | `movement_speed` | 100000 | `+100` | `+0.001` | `movement_speed_update` | `—` |
| `movement_speed_update` | `movement_speed` | — | `прямой дельты атрибута нет` | `прямой дельты атрибута нет` | `movement_speed_update` | `—` |
| `oxygen_bonus_down` | `oxygen_bonus` | 10 | `-1` | `-0.1` | `oxygen_bonus_update` | `—` |
| `oxygen_bonus_up` | `oxygen_bonus` | 10 | `+1` | `+0.1` | `oxygen_bonus_update` | `—` |
| `oxygen_bonus_up_godclaw` | `oxygen_bonus` | 100 | `+ tmp1` | `+ tmp1 / 100` | `oxygen_bonus_update` | `—` |
| `oxygen_bonus_update` | `oxygen_bonus` | — | `прямой дельты атрибута нет` | `прямой дельты атрибута нет` | `oxygen_bonus_update` | `—` |
| `random_damage` | `attack_damage` | 3000 | random 1..(current×3000) | 0.001..(current×3), step 0.001 | `attack_damage_update` | `lbc:eat_damage_penis` |
| `random_health` | `max_health` | 2500 | random 1..(current×2500) | 0.001..(current×2.5), step 0.001 | `max_health_update` | `lbc:eat_healthy_penis` |
| `random_speed` | `movement_speed` | 4000 | random 1..(current×4000) | 0.001..(current×4), step 0.001 | `movement_speed_update` | `lbc:eat_fast_penis` |
| `reduce_hp_armor` | `max_health`, `armor`, `armor_toughness` | 1 | `−2` each | health −2; armor −2; toughness branch routes to `armor_update` | `mixed` | `—` |
| `safe_fall_distance_down` | `safe_fall_distance` | 5 | `-1` | `-0.2` | `safe_fall_distance_update` | `—` |
| `safe_fall_distance_up` | `safe_fall_distance` | 5 | `+1` | `+0.2` | `safe_fall_distance_update` | `—` |
| `safe_fall_distance_up_godclaw` | `safe_fall_distance` | 100 | `+ tmp1` | `+ tmp1 / 100` | `safe_fall_distance_update` | `—` |
| `safe_fall_distance_update` | `safe_fall_distance` | — | `прямой дельты атрибута нет` | `прямой дельты атрибута нет` | `safe_fall_distance_update` | `—` |
| `scale_down` | `scale` | 10 | `-1` | `-0.1` | `scale_update` | `—` |
| `scale_up` | `scale` | 10 | `+1` | `+0.1` | `scale_update` | `—` |
| `scale_up_godclaw` | `scale` | 10000 | `+ tmp1` | `+ tmp1 / 10000` | `scale_update` | `—` |
| `scale_up_tiny` | `scale` | 100 | `+1` | `+0.01` | `scale_update` | `—` |
| `scale_update` | `scale` | — | `прямой дельты атрибута нет` | `прямой дельты атрибута нет` | `scale_update` | `—` |
| `sneaking_speed_down` | `sneaking_speed` | 10 | `-1` | `-0.1` | `sneaking_speed_update` | `—` |
| `sneaking_speed_up` | `sneaking_speed` | 10 | `+1` | `+0.1` | `sneaking_speed_update` | `—` |
| `sneaking_speed_up_godclaw` | `sneaking_speed` | 1000 | `+ tmp1` | `+ tmp1 / 1000` | `sneaking_speed_update` | `—` |
| `sneaking_speed_update` | `sneaking_speed` | — | `прямой дельты атрибута нет` | `прямой дельты атрибута нет` | `sneaking_speed_update` | `—` |
| `step_height_down` | `step_height` | 10 | `-1` | `-0.1` | `step_height_update` | `—` |
| `step_height_up` | `step_height` | 10 | `+1` | `+0.1` | `step_height_update` | `—` |
| `step_height_up_godclaw` | `step_height` | 500 | `+ tmp1` | `+ tmp1 / 500` | `step_height_update` | `—` |
| `step_height_update` | `step_height` | — | `прямой дельты атрибута нет` | `прямой дельты атрибута нет` | `step_height_update` | `—` |
| `submerged_mining_speed_down` | `submerged_mining_speed` | 20 | `-1` | `-0.05` | `submerged_mining_speed_update` | `—` |
| `submerged_mining_speed_up` | `submerged_mining_speed` | 20 | `+1` | `+0.05` | `submerged_mining_speed_update` | `—` |
| `submerged_mining_speed_up_godclaw` | `submerged_mining_speed` | 100 | `+ tmp1` | `+ tmp1 / 100` | `submerged_mining_speed_update` | `—` |
| `submerged_mining_speed_update` | `submerged_mining_speed` | — | `прямой дельты атрибута нет` | `прямой дельты атрибута нет` | `submerged_mining_speed_update` | `—` |
| `sweeping_damage_ratio_down` | `sweeping_damage_ratio` | 20 | `-1` | `-0.05` | `sweeping_damage_ratio_update` | `—` |
| `sweeping_damage_ratio_up` | `sweeping_damage_ratio` | 20 | `+1` | `+0.05` | `sweeping_damage_ratio_update` | `—` |
| `sweeping_damage_ratio_up_godclaw` | `sweeping_damage_ratio` | 1000 | `+ tmp1` | `+ tmp1 / 1000` | `sweeping_damage_ratio_update` | `—` |
| `sweeping_damage_ratio_update` | `sweeping_damage_ratio` | — | `прямой дельты атрибута нет` | `прямой дельты атрибута нет` | `sweeping_damage_ratio_update` | `—` |
| `water_movement_efficiency_down` | `water_movement_efficiency` | 10 | `-1` | `-0.1` | `water_movement_efficiency_update` | `—` |
| `water_movement_efficiency_up` | `water_movement_efficiency` | 10 | `+1` | `+0.1` | `water_movement_efficiency_update` | `—` |
| `water_movement_efficiency_up_godclaw` | `water_movement_efficiency` | 10000 | `+ tmp1` | `+ tmp1 / 10000` | `water_movement_efficiency_update` | `—` |
| `water_movement_efficiency_update` | `water_movement_efficiency` | — | `прямой дельты атрибута нет` | `прямой дельты атрибута нет` | `water_movement_efficiency_update` | `—` |


Пограничные варианты сохраняют собственный шаг и scale: `max_health` обычный `+2`, `max_health_1` — `+1`, `_down_two` — `−2`, `_down_eight` — `−8`, `_up_eight` — `+8`; knockback resistance обычный шаг равен 10, и `_down`/`_up` тоже меняют на 10. Для `movement_speed` обычный шаг ±2000 при scale 100000 (±0.02), tiny +100 (0.001). `attribute base get S` сохраняет в scoreboard целое квантованное значение, поэтому таблица `delta/S` показывает шаг после квантования, а не гарантирует абсолютную дельту от любой дробной исходной базы; `base get` отбрасывает дробную часть к нулю. Рандомные `random_damage`, `random_health`, `random_speed` заменяют базу случайным значением от 0.001 до соответственно 3×, 2.5× и 4× прежнего значения, шаг 0.001; после замены дают nausea 3 секунды и blindness 2 секунды, затем отзывают свой advancement. `reduce_hp_armor` выполняет три вычитания по 2: max health и armor обновляются ожидаемыми функциями, но третья ветка читает `armor_toughness` и ошибочно вызывает `armor_update`, поэтому она перезаписывает armor значением toughness−2, а toughness не обновляет. `gen.py` — неисполняемый генератор, перечисляющий 26 godclaw-файлов и шаблон с get-scale 1000, `tmp1` и коэффициентом 0.001; существующие файлы содержат разные scale и коэффициенты, поэтому фактическая таблица взята из самих функций.

Обычные consumable entry points у восьми baseline wrappers используют trigger `minecraft:consume_item` и соответствующий компонент-подобный ключ `custom_data` (`armorattribute:1`, `armor_toughnessattribute:1`, `attack_damageattribute:1`, `attackspeedattribute:1`, `knockback_resistanceattribute:1`, `luckattribute:1`, `max_healthattribute:1`, `movement_speedattribute:1`); criteria исключают dimensions `minecraft:nexus` и `minecraft:imprinted`. Событие употребления расходует предмет, reward меняет base и отзывает свой advancement. Остальные `_up`, `_down`, Godclaw и random helpers сами advancement не слушают: их вызывает внешний предмет/навык или pill wrapper.

## Таблетки: trigger, входной предмет и результат

В таблице `effect ... D amp=A`: D задано в секундах, `infinite` — без конечного таймера; amplifier A означает уровень A+1. Для мгновенного эффекта D не означает непрерывное действие столько секунд.

Для функций из `attributes/pills` найденные advancement-контракты показывают `minecraft:consume_item` и custom data `pill:<число>`; условия игрока исключают измерения `minecraft:nexus` и `minecraft:imprinted`. Успешное употребление расходует предмет по самому consume trigger, затем reward вызывает функцию ниже. Loot table `lbc:kit/pill` выдаёт одну `minecraft:cookie` с consume time 0.2 s, `minecraft:food` nutrition/saturation 1 и can_always_eat=true; для hash N ставит model `lbc:pills/pill_N` и custom data `{pill:N}`. Таким образом, pill:N из advancement связывает предмет комплекта с обработчиком. `lbc:kit/pill` выбирает запись проверкой `lbc.math hash == N`; предметы имеют count 1, translation name `pill` и lore `pill.lore`. Обработчик применяет эффект либо вызывает attribute handler, показывает subtitle и звук `minecraft:lbcsounds.pill_good`/`pill_bad`, после чего отзывает advancement `lbc:pills/pill_*` для следующего предмета. Отзыв advancement не возвращает таблетку.

| Функция | Advancement / trigger / custom data | Результат эффекта или атрибута |
|---|---|---|
| `absorption` | `lbc:pills/pill_absorption; minecraft:consume_item; {pill:54}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:absorption 60 amp=1 |
| `absorption_99` | `lbc:pills/pill_absorption_99; minecraft:consume_item; {pill:55}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:absorption 5 amp=127 |
| `armor_down` | `lbc:pills/pill_armor_down; minecraft:consume_item; {pill:2}; outside minecraft:nexus and minecraft:imprinted` | attribute handler `armor_down` |
| `armor_toughness_down` | `lbc:pills/pill_armor_toughness_down; minecraft:consume_item; {pill:3}; outside minecraft:nexus and minecraft:imprinted` | attribute handler `armor_toughness_down` |
| `armor_toughness_up` | `lbc:pills/pill_armor_toughness_up; minecraft:consume_item; {pill:30}; outside minecraft:nexus and minecraft:imprinted` | attribute handler `armor_toughness_up` |
| `armor_up` | `lbc:pills/pill_armor_up; minecraft:consume_item; {pill:29}; outside minecraft:nexus and minecraft:imprinted` | attribute handler `armor_up` |
| `attack_damage_down` | `lbc:pills/pill_attack_damage_down; minecraft:consume_item; {pill:1}; outside minecraft:nexus and minecraft:imprinted` | attribute handler `attack_damage_down` |
| `attack_damage_up` | `lbc:pills/pill_attack_damage_up; minecraft:consume_item; {pill:28}; outside minecraft:nexus and minecraft:imprinted` | attribute handler `attack_damage_up` |
| `attack_knockback_down` | `lbc:pills/pill_attack_knockback_down; minecraft:consume_item; {pill: 8}; outside minecraft:nexus and minecraft:imprinted` | attribute handler `attack_knockback_down` |
| `attack_knockback_up` | `lbc:pills/pill_attack_knockback_up; minecraft:consume_item; {pill:35}; outside minecraft:nexus and minecraft:imprinted` | attribute handler `attack_knockback_up` |
| `attack_speed_down` | `lbc:pills/pill_attack_speed_down; minecraft:consume_item; {pill:4}; outside minecraft:nexus and minecraft:imprinted` | attribute handler `attack_speed_down` |
| `attack_speed_up` | `lbc:pills/pill_attack_speed_up; minecraft:consume_item; {pill:31}; outside minecraft:nexus and minecraft:imprinted` | attribute handler `attack_speed_up` |
| `blindness` | `lbc:pills/pill_blindness; minecraft:consume_item; {pill:56}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:blindness 60 amp=0 |
| `block_break_speed_down` | `lbc:pills/pill_block_break_speed_down; minecraft:consume_item; {pill:20}; outside minecraft:nexus and minecraft:imprinted` | attribute handler `block_break_speed_down` |
| `block_break_speed_up` | `lbc:pills/pill_block_break_speed_up; minecraft:consume_item; {pill:47}; outside minecraft:nexus and minecraft:imprinted` | attribute handler `block_break_speed_up` |
| `block_interaction_range_down` | `lbc:pills/pill_block_interaction_range_down; minecraft:consume_item; {pill:21}; outside minecraft:nexus and minecraft:imprinted` | attribute handler `block_interaction_range_down` |
| `block_interaction_range_up` | `lbc:pills/pill_block_interaction_range_up; minecraft:consume_item; {pill:48}; outside minecraft:nexus and minecraft:imprinted` | attribute handler `block_interaction_range_up` |
| `burning_time_down` | `lbc:pills/pill_burning_time_down; minecraft:consume_item; {pill:9}; outside minecraft:nexus and minecraft:imprinted` | attribute handler `burning_time_down` |
| `burning_time_up` | `lbc:pills/pill_burning_time_up; minecraft:consume_item; {pill:36}; outside minecraft:nexus and minecraft:imprinted` | attribute handler `burning_time_up` |
| `conduit_power_1` | `lbc:pills/pill_conduit_power_1; minecraft:consume_item; {pill:57}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:conduit_power 30 amp=1 |
| `conduit_power_2` | `lbc:pills/pill_conduit_power_2; minecraft:consume_item; {pill:58}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:conduit_power 15 amp=2 |
| `darkness` | `lbc:pills/pill_darkness; minecraft:consume_item; {pill:59}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:darkness 120 amp=0 |
| `dolphins_grace` | `lbc:pills/pill_dolphins_grace; minecraft:consume_item; {pill:60}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:dolphins_grace 60 amp=0 |
| `drowsy` | `lbc:pills/pill_drowsy; minecraft:consume_item; {pill:143}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:slowness 30 amp=4; effect minecraft:mining_fatigue 30 amp=0; effect minecraft:weakness 30 amp=0 |
| `entity_interaction_range_down` | `lbc:pills/pill_entity_interaction_range_down; minecraft:consume_item; {pill:22}; outside minecraft:nexus and minecraft:imprinted` | attribute handler `entity_interaction_range_down` |
| `entity_interaction_range_up` | `lbc:pills/pill_entity_interaction_range_up; minecraft:consume_item; {pill:49}; outside minecraft:nexus and minecraft:imprinted` | attribute handler `entity_interaction_range_up` |
| `excited` | `lbc:pills/pill_excited; minecraft:consume_item; {pill:142}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:speed 30 amp=4; effect minecraft:haste 30 amp=4; effect minecraft:jump_boost 30 amp=0 |
| `explosion_knockback_resistance_down` | `lbc:pills/pill_explosion_knockback_resistance_down; minecraft:consume_item; {pill:10}; outside minecraft:nexus and minecraft:imprinted` | attribute handler `explosion_knockback_resistance_down` |
| `explosion_knockback_resistance_up` | `lbc:pills/pill_explosion_knockback_resistance_up; minecraft:consume_item; {pill:37}; outside minecraft:nexus and minecraft:imprinted` | attribute handler `explosion_knockback_resistance_up` |
| `fall_damage_multiplier_down` | `lbc:pills/pill_fall_damage_multiplier_down; minecraft:consume_item; {pill:11}; outside minecraft:nexus and minecraft:imprinted` | attribute handler `fall_damage_multiplier_down` |
| `fall_damage_multiplier_up` | `lbc:pills/pill_fall_damage_multiplier_up; minecraft:consume_item; {pill:38}; outside minecraft:nexus and minecraft:imprinted` | attribute handler `fall_damage_multiplier_up` |
| `fire_resistance` | `lbc:pills/pill_fire_resistance; minecraft:consume_item; {pill:61}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:fire_resistance 60 amp=0 |
| `fire_resistance_1` | `lbc:pills/pill_fire_resistance_1; minecraft:consume_item; {pill:62}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:fire_resistance 30 amp=1 |
| `fire_resistance_2` | `lbc:pills/pill_fire_resistance_2; minecraft:consume_item; {pill:63}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:fire_resistance 15 amp=2 |
| `glowing` | `lbc:pills/pill_glowing; minecraft:consume_item; {pill:64}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:glowing 60 amp=0 |
| `gravity_down` | `lbc:pills/pill_gravity_down; minecraft:consume_item; {pill:12}; outside minecraft:nexus and minecraft:imprinted` | attribute handler `gravity_down` |
| `gravity_up` | `lbc:pills/pill_gravity_up; minecraft:consume_item; {pill:39}; outside minecraft:nexus and minecraft:imprinted` | attribute handler `gravity_up` |
| `haste` | `lbc:pills/pill_haste; minecraft:consume_item; {pill:65}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:haste 60 amp=1 |
| `haste_99` | `lbc:pills/pill_haste_99; minecraft:consume_item; {pill:66}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:haste 10 amp=99 |
| `health_boost` | `lbc:pills/pill_health_boost; minecraft:consume_item; {pill:67}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:health_boost 60 amp=4 |
| `hero_of_the_village` | `lbc:pills/pill_hero_of_the_village; minecraft:consume_item; {pill:68}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:hero_of_the_village 120 amp=127 |
| `hunger` | `lbc:pills/pill_hunger; minecraft:consume_item; {pill:69}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:hunger 100 amp=1 |
| `hunger_99` | `lbc:pills/pill_hunger_99; minecraft:consume_item; {pill:70}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:hunger 4 amp=127 |
| `i_found_pills` | `lbc:pills/pill_i_found_pills; minecraft:consume_item; {pill:144}; outside minecraft:nexus and minecraft:imprinted` | только subtitle, плохой звук и отзыв advancement; отдельного игрового эффекта в файле нет |
| `infested` | `lbc:pills/pill_infested; minecraft:consume_item; {pill:71}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:infested infinite amp=0 |
| `instant_damage` | `lbc:pills/pill_instant_damage; minecraft:consume_item; {pill:72}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:instant_damage 1 amp=0 |
| `instant_damage_2` | `lbc:pills/pill_instant_damage_2; minecraft:consume_item; {pill:73}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:instant_damage 1 amp=2 |
| `instant_health` | `lbc:pills/pill_instant_health; minecraft:consume_item; {pill:74}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:instant_health 1 amp=99 |
| `invisibility` | `lbc:pills/pill_invisibility; minecraft:consume_item; {pill:75}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:invisibility 60 amp=0 |
| `invisibility_1` | `lbc:pills/pill_invisibility_1; minecraft:consume_item; {pill:76}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:invisibility 30 amp=1 |
| `jump_boost` | `lbc:pills/pill_jump_boost; minecraft:consume_item; {pill:77}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:jump_boost 60 amp=2 |
| `jump_boost_99` | `lbc:pills/pill_jump_boost_99; minecraft:consume_item; {pill:78}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:jump_boost 10 amp=127 |
| `jump_strength_down` | `lbc:pills/pill_jump_strength_down; minecraft:consume_item; {pill:13}; outside minecraft:nexus and minecraft:imprinted` | attribute handler `jump_strength_down` |
| `jump_strength_up` | `lbc:pills/pill_jump_strength_up; minecraft:consume_item; {pill:40}; outside minecraft:nexus and minecraft:imprinted` | attribute handler `jump_strength_up` |
| `knockback_resistance_down` | `lbc:pills/pill_knockback_resistance_down; minecraft:consume_item; {pill:5}; outside minecraft:nexus and minecraft:imprinted` | attribute handler `knockback_resistance_down` |
| `knockback_resistance_up` | `lbc:pills/pill_knockback_resistance_up; minecraft:consume_item; {pill:32}; outside minecraft:nexus and minecraft:imprinted` | attribute handler `knockback_resistance_up` |
| `levitation` | `lbc:pills/pill_levitation; minecraft:consume_item; {pill:79}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:levitation 20 amp=0 |
| `levitation_99` | `lbc:pills/pill_levitation_99; minecraft:consume_item; {pill:80}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:levitation 1 amp=127 |
| `luck` | `lbc:pills/pill_luck; minecraft:consume_item; {pill:81}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:luck 30 amp=0 |
| `luck_99` | `lbc:pills/pill_luck_99; minecraft:consume_item; {pill:82}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:luck 2 amp=255 |
| `luck_down` | `lbc:pills/pill_luck_down; minecraft:consume_item; {pill:6}; outside minecraft:nexus and minecraft:imprinted` | attribute handler `luck_down` |
| `luck_up` | `lbc:pills/pill_luck_up; minecraft:consume_item; {pill:33}; outside minecraft:nexus and minecraft:imprinted` | attribute handler `luck_up` |
| `max_health_down` | `lbc:pills/pill_max_health_down; minecraft:consume_item; {pill:0}; outside minecraft:nexus and minecraft:imprinted` | attribute handler `max_health_down` |
| `max_health_up` | `lbc:pills/pill_max_health_up; minecraft:consume_item; {pill:27}; outside minecraft:nexus and minecraft:imprinted` | attribute handler `max_health_up` |
| `mining_efficiency_down` | `lbc:pills/pill_mining_efficiency_down; minecraft:consume_item; {pill:23}; outside minecraft:nexus and minecraft:imprinted` | attribute handler `mining_efficiency_down` |
| `mining_efficiency_up` | `lbc:pills/pill_mining_efficiency_up; minecraft:consume_item; {pill:50}; outside minecraft:nexus and minecraft:imprinted` | attribute handler `mining_efficiency_up` |
| `mining_fatigue` | `lbc:pills/pill_mining_fatigue; minecraft:consume_item; {pill:83}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:mining_fatigue 60 amp=1 |
| `mining_fatigue_99` | `lbc:pills/pill_mining_fatigue_99; minecraft:consume_item; {pill:84}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:mining_fatigue 20 amp=99 |
| `movement_efficiency_down` | `lbc:pills/pill_movement_efficiency_down; minecraft:consume_item; {pill:14}; outside minecraft:nexus and minecraft:imprinted` | attribute handler `movement_efficiency_down` |
| `movement_efficiency_up` | `lbc:pills/pill_movement_efficiency_up; minecraft:consume_item; {pill:41}; outside minecraft:nexus and minecraft:imprinted` | attribute handler `movement_efficiency_up` |
| `movement_speed_down` | `lbc:pills/pill_movement_speed_down; minecraft:consume_item; {pill:7}; outside minecraft:nexus and minecraft:imprinted` | attribute handler `movement_speed_down` |
| `movement_speed_up` | `lbc:pills/pill_movement_speed_up; minecraft:consume_item; {pill:34}; outside minecraft:nexus and minecraft:imprinted` | attribute handler `movement_speed_up` |
| `nausea` | `lbc:pills/pill_nausea; minecraft:consume_item; {pill:85}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:nausea 100 amp=0 |
| `night_vision` | `lbc:pills/pill_night_vision; minecraft:consume_item; {pill:86}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:night_vision 120 amp=0 |
| `oozing` | `lbc:pills/pill_oozing; minecraft:consume_item; {pill:87}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:oozing infinite amp=0 |
| `oxygen_bonus_down` | `lbc:pills/pill_oxygen_bonus_down; minecraft:consume_item; {pill:15}; outside minecraft:nexus and minecraft:imprinted` | attribute handler `oxygen_bonus_down` |
| `oxygen_bonus_up` | `lbc:pills/pill_oxygen_bonus_up; minecraft:consume_item; {pill:42}; outside minecraft:nexus and minecraft:imprinted` | attribute handler `oxygen_bonus_up` |
| `paralysis` | `lbc:pills/pill_paralysis; minecraft:consume_item; {pill:141}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:slowness 10 amp=127 |
| `poison` | `lbc:pills/pill_poison; minecraft:consume_item; {pill:88}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:poison 30 amp=0 |
| `raid_omen_101` | `lbc:pills/pill_raid_omen_101; minecraft:consume_item; {pill:98}; outside minecraft:nexus and minecraft:imprinted` | `effect_small_explosion += 1`; включает `have_lucky_effects` |
| `raid_omen_102` | `lbc:pills/pill_raid_omen_102; minecraft:consume_item; {pill:99}; outside minecraft:nexus and minecraft:imprinted` | `effect_evoker += 1`; включает `have_lucky_effects` |
| `raid_omen_103` | `lbc:pills/pill_raid_omen_103; minecraft:consume_item; {pill:100}; outside minecraft:nexus and minecraft:imprinted` | `effect_shock += 1`; включает флаги обслуживания `have_lucky_effects`, `_1s`, `_5t` |
| `raid_omen_104` | `lbc:pills/pill_raid_omen_104; minecraft:consume_item; {pill:101}; outside minecraft:nexus and minecraft:imprinted` | `effect_explosion += 1`; включает `have_lucky_effects` |
| `raid_omen_105` | `lbc:pills/pill_raid_omen_105; minecraft:consume_item; {pill:102}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:raid_omen 10 amp=105 |
| `raid_omen_106` | `lbc:pills/pill_raid_omen_106; minecraft:consume_item; {pill:103}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:raid_omen 1 amp=106 |
| `raid_omen_107` | `lbc:pills/pill_raid_omen_107; minecraft:consume_item; {pill:104}; outside minecraft:nexus and minecraft:imprinted` | `effect_big_explosion += 1`; включает `have_lucky_effects` |
| `raid_omen_108` | `lbc:pills/pill_raid_omen_108; minecraft:consume_item; {pill:105}; outside minecraft:nexus and minecraft:imprinted` | `effect_traps += 1`; включает `have_lucky_effects` |
| `raid_omen_109` | `lbc:pills/pill_raid_omen_109; minecraft:consume_item; {pill:106}; outside minecraft:nexus and minecraft:imprinted` | `effect_lightning_bolt += 1`; включает `have_lucky_effects` |
| `raid_omen_110` | `lbc:pills/pill_raid_omen_110; minecraft:consume_item; {pill:107}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:raid_omen 1 amp=110 |
| `raid_omen_111` | `lbc:pills/pill_raid_omen_111; minecraft:consume_item; {pill:108}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:raid_omen 15 amp=111 |
| `raid_omen_112` | `lbc:pills/pill_raid_omen_112; minecraft:consume_item; {pill:109}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:raid_omen 30 amp=112 |
| `raid_omen_113` | `lbc:pills/pill_raid_omen_113; minecraft:consume_item; {pill:110}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:raid_omen 30 amp=113 |
| `raid_omen_114` | `lbc:pills/pill_raid_omen_114; minecraft:consume_item; {pill:111}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:raid_omen 1 amp=114 |
| `raid_omen_115` | `lbc:pills/pill_raid_omen_115; minecraft:consume_item; {pill:112}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:raid_omen 1 amp=115 |
| `raid_omen_116` | `lbc:pills/pill_raid_omen_116; minecraft:consume_item; {pill:113}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:raid_omen 30 amp=116 |
| `raid_omen_117` | `lbc:pills/pill_raid_omen_117; minecraft:consume_item; {pill:114}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:raid_omen 1 amp=117 |
| `raid_omen_118` | `lbc:pills/pill_raid_omen_118; minecraft:consume_item; {pill:115}; outside minecraft:nexus and minecraft:imprinted` | `effect_hedgehog_boom += 1`; включает `have_lucky_effects` |
| `raid_omen_119` | `lbc:pills/pill_raid_omen_119; minecraft:consume_item; {pill:116}; outside minecraft:nexus and minecraft:imprinted` | `effect_stand_arrow_hit += 1`; включает `have_lucky_effects` |
| `raid_omen_120` | `lbc:pills/pill_raid_omen_120; minecraft:consume_item; {pill:117}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:raid_omen 120 amp=120 |
| `raid_omen_121` | `lbc:pills/pill_raid_omen_121; minecraft:consume_item; {pill:118}; outside minecraft:nexus and minecraft:imprinted` | `effect_nuking += 1`; включает `have_lucky_effects` |
| `raid_omen_122` | `lbc:pills/pill_raid_omen_122; minecraft:consume_item; {pill:119}; outside minecraft:nexus and minecraft:imprinted` | `effect_vacuum += 1`; включает `have_lucky_effects` |
| `raid_omen_123` | `lbc:pills/pill_raid_omen_123; minecraft:consume_item; {pill:120}; outside minecraft:nexus and minecraft:imprinted` | `effect_timestop += 1`; включает `have_lucky_effects` |
| `raid_omen_124` | `lbc:pills/pill_raid_omen_124; minecraft:consume_item; {pill:121}; outside minecraft:nexus and minecraft:imprinted` | `effect_chain_lightning += 1`; включает `have_lucky_effects` |
| `raid_omen_16` | `lbc:pills/pill_raid_omen_16; minecraft:consume_item; {pill:89}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:raid_omen 60 amp=16 |
| `raid_omen_17` | `lbc:pills/pill_raid_omen_17; minecraft:consume_item; {pill:90}; outside minecraft:nexus and minecraft:imprinted` | `mana += 5000` (это число scoreboard, не секунд) |
| `raid_omen_18` | `lbc:pills/pill_raid_omen_18; minecraft:consume_item; {pill:91}; outside minecraft:nexus and minecraft:imprinted` | `manamax += 1000` |
| `raid_omen_19` | `lbc:pills/pill_raid_omen_19; minecraft:consume_item; {pill:92}; outside minecraft:nexus and minecraft:imprinted` | `manaregen += 3` |
| `raid_omen_20` | `lbc:pills/pill_raid_omen_20; minecraft:consume_item; {pill:93}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:raid_omen 1 amp=20 |
| `raid_omen_40` | `lbc:pills/pill_raid_omen_40; minecraft:consume_item; {pill:95}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:raid_omen 10 amp=40 |
| `raid_omen_41` | `lbc:pills/pill_raid_omen_41; minecraft:consume_item; {pill:96}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:raid_omen 10 amp=41 |
| `raid_omen_42` | `lbc:pills/pill_raid_omen_42; minecraft:consume_item; {pill:97}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:raid_omen 10 amp=42 |
| `raid_omen_74` | `lbc:pills/pill_raid_omen_74; minecraft:consume_item; {pill:94}; outside minecraft:nexus and minecraft:imprinted` | `effect_snus += 1`; включает `have_lucky_effects` |
| `regeneration` | `lbc:pills/pill_regeneration; minecraft:consume_item; {pill:122}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:regeneration 60 amp=1 |
| `resistance` | `lbc:pills/pill_resistance; minecraft:consume_item; {pill:123}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:resistance 60 amp=1 |
| `resistance_5` | `lbc:pills/pill_resistance_5; minecraft:consume_item; {pill:124}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:resistance 10 amp=4 |
| `safe_fall_distance_down` | `lbc:pills/pill_safe_fall_distance_down; minecraft:consume_item; {pill:16}; outside minecraft:nexus and minecraft:imprinted` | attribute handler `safe_fall_distance_down` |
| `safe_fall_distance_up` | `lbc:pills/pill_safe_fall_distance_up; minecraft:consume_item; {pill:43}; outside minecraft:nexus and minecraft:imprinted` | attribute handler `safe_fall_distance_up` |
| `saturation` | `lbc:pills/pill_saturation; minecraft:consume_item; {pill:125}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:saturation 1 amp=127 |
| `scale_down` | `lbc:pills/pill_scale_down; minecraft:consume_item; {pill:17}; outside minecraft:nexus and minecraft:imprinted` | attribute handler `scale_down` |
| `scale_up` | `lbc:pills/pill_scale_up; minecraft:consume_item; {pill:44}; outside minecraft:nexus and minecraft:imprinted` | attribute handler `scale_up` |
| `slow_falling` | `lbc:pills/pill_slow_falling; minecraft:consume_item; {pill:126}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:slow_falling 60 amp=0 |
| `slowness` | `lbc:pills/pill_slowness; minecraft:consume_item; {pill:127}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:slowness 60 amp=2 |
| `sneaking_speed_down` | `lbc:pills/pill_sneaking_speed_down; minecraft:consume_item; {pill:24}; outside minecraft:nexus and minecraft:imprinted` | attribute handler `sneaking_speed_down` |
| `sneaking_speed_up` | `lbc:pills/pill_sneaking_speed_up; minecraft:consume_item; {pill:51}; outside minecraft:nexus and minecraft:imprinted` | attribute handler `sneaking_speed_up` |
| `speed` | `lbc:pills/pill_speed; minecraft:consume_item; {pill:128}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:speed 60 amp=2 |
| `step_height_down` | `lbc:pills/pill_step_height_down; minecraft:consume_item; {pill:18}; outside minecraft:nexus and minecraft:imprinted` | attribute handler `step_height_down` |
| `step_height_up` | `lbc:pills/pill_step_height_up; minecraft:consume_item; {pill:45}; outside minecraft:nexus and minecraft:imprinted` | attribute handler `step_height_up` |
| `strength` | `lbc:pills/pill_strength; minecraft:consume_item; {pill:129}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:strength 40 amp=0 |
| `strength_99` | `lbc:pills/pill_strength_99; minecraft:consume_item; {pill:130}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:strength 2 amp=99 |
| `submerged_mining_speed_down` | `lbc:pills/pill_submerged_mining_speed_down; minecraft:consume_item; {pill:25}; outside minecraft:nexus and minecraft:imprinted` | attribute handler `submerged_mining_speed_down` |
| `submerged_mining_speed_up` | `lbc:pills/pill_submerged_mining_speed_up; minecraft:consume_item; {pill:52}; outside minecraft:nexus and minecraft:imprinted` | attribute handler `submerged_mining_speed_up` |
| `sweeping_damage_ratio_down` | `lbc:pills/pill_sweeping_damage_ratio_down; minecraft:consume_item; {pill:26}; outside minecraft:nexus and minecraft:imprinted` | attribute handler `sweeping_damage_ratio_down` |
| `sweeping_damage_ratio_up` | `lbc:pills/pill_sweeping_damage_ratio_up; minecraft:consume_item; {pill:53}; outside minecraft:nexus and minecraft:imprinted` | attribute handler `sweeping_damage_ratio_up` |
| `unluck` | `lbc:pills/pill_unluck; minecraft:consume_item; {pill:131}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:unluck 120 amp=0 |
| `unluck_99` | `lbc:pills/pill_unluck_99; minecraft:consume_item; {pill:132}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:unluck 60 amp=99 |
| `water_breathing` | `lbc:pills/pill_water_breathing; minecraft:consume_item; {pill:133}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:water_breathing 120 amp=0 |
| `water_breathing_2` | `lbc:pills/pill_water_breathing_2; minecraft:consume_item; {pill:134}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:water_breathing 60 amp=2 |
| `water_movement_efficiency_down` | `lbc:pills/pill_water_movement_efficiency_down; minecraft:consume_item; {pill:19}; outside minecraft:nexus and minecraft:imprinted` | attribute handler `water_movement_efficiency_down` |
| `water_movement_efficiency_up` | `lbc:pills/pill_water_movement_efficiency_up; minecraft:consume_item; {pill:46}; outside minecraft:nexus and minecraft:imprinted` | attribute handler `water_movement_efficiency_up` |
| `weakness` | `lbc:pills/pill_weakness; minecraft:consume_item; {pill:135}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:weakness 120 amp=0 |
| `weakness_99` | `lbc:pills/pill_weakness_99; minecraft:consume_item; {pill:136}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:weakness 30 amp=99 |
| `weaving` | `lbc:pills/pill_weaving; minecraft:consume_item; {pill:137}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:weaving infinite amp=0 |
| `wind_charged` | `lbc:pills/pill_wind_charged; minecraft:consume_item; {pill:138}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:wind_charged infinite amp=0 |
| `wither` | `lbc:pills/pill_wither; minecraft:consume_item; {pill:139}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:wither 60 amp=0 |
| `wither_99` | `lbc:pills/pill_wither_99; minecraft:consume_item; {pill:140}; outside minecraft:nexus and minecraft:imprinted` | effect minecraft:wither 10 amp=99 |


Атрибутные таблетки используют те же значения из таблицы выше; плохие изменения запускают соответствующий `*_down`, хорошие — `*_up`. Эффекты с суффиксом `_99` имеют буквально приведённые высокие amplifier-значения, не обычную длительность/силу: это отдельные ветки и отдельные custom_data IDs в таблице. Функции, для которых не найден reward-контракт, в таблице явно отмечены как ручной/caller запуск.

## Случайный выбор изменения атрибута

`random_attrs_up` разыгрывает равномерное число `#rand` от 1 до 26 и вызывает ровно один из перечисленных обработчиков. Варианты 8 и 11 уменьшают burning_time и fall_damage_multiplier, хотя функция называется `up`. `random_attrs_down` независимо проверяет `lbc:chance50` для каждого из восьми обработчиков: один вызов может уменьшить несколько атрибутов сразу, один атрибут или ни одного. `gen_rand` выбирает целое от 1 до `tmp`, `randdouble_ab` — от 0 до `tmpi`; сами функции записывают случайное значение, а смысл ему задаёт вызывающий обработчик.

### `gen_rand`

Команды: `$execute store result score #random_out lbc.math run random value 1..$(tmp)`

### `randdouble_ab`

Команды: `$execute store result score tmp_aft lbc.math run random value 0..$(tmpi)`

### `random_attrs_down`

Команды: `execute if predicate lbc:chance50 run function lbc:other/attributes/armor_down`; `execute if predicate lbc:chance50 run function lbc:other/attributes/armor_toughness_down`; `execute if predicate lbc:chance50 run function lbc:other/attributes/knockback_resistance_down`; `execute if predicate lbc:chance50 run function lbc:other/attributes/luck_down`; `execute if predicate lbc:chance50 run function lbc:other/attributes/max_health_down_two`; `execute if predicate lbc:chance50 run function lbc:other/attributes/attack_damage_down`; `execute if predicate lbc:chance50 run function lbc:other/attributes/attack_speed_down`; `execute if predicate lbc:chance50 run function lbc:other/attributes/movement_speed_down`

### `random_attrs_up`

Команды: `execute store result score #rand lbc.math run random value 1..26`; `execute if score #rand lbc.math matches 1 run function lbc:other/attributes/armor_1`; `execute if score #rand lbc.math matches 2 run function lbc:other/attributes/armor_toughness`; `execute if score #rand lbc.math matches 3 run function lbc:other/attributes/attack_damage_up_tiny`; `execute if score #rand lbc.math matches 4 run function lbc:other/attributes/attack_knockback_up`; `execute if score #rand lbc.math matches 5 run function lbc:other/attributes/attack_speed_up`; `execute if score #rand lbc.math matches 6 run function lbc:other/attributes/block_break_speed_up`; `execute if score #rand lbc.math matches 7 run function lbc:other/attributes/block_interaction_range_up`; `execute if score #rand lbc.math matches 8 run function lbc:other/attributes/burning_time_down`; `execute if score #rand lbc.math matches 9 run function lbc:other/attributes/entity_interaction_range_up`; `execute if score #rand lbc.math matches 10 run function lbc:other/attributes/explosion_knockback_resistance_up`; `execute if score #rand lbc.math matches 11 run function lbc:other/attributes/fall_damage_multiplier_down`; `execute if score #rand lbc.math matches 12 run function lbc:other/attributes/jump_strength_up_tiny`; `execute if score #rand lbc.math matches 13 run function lbc:other/attributes/knockback_resistance_up`; `execute if score #rand lbc.math matches 14 run function lbc:other/attributes/luck_up`; `execute if score #rand lbc.math matches 15 run function lbc:other/attributes/max_health_up`; `execute if score #rand lbc.math matches 16 run function lbc:other/attributes/mining_efficiency_up`; `execute if score #rand lbc.math matches 17 run function lbc:other/attributes/movement_efficiency_up`; `execute if score #rand lbc.math matches 18 run function lbc:other/attributes/oxygen_bonus_up`; `execute if score #rand lbc.math matches 19 run function lbc:other/attributes/safe_fall_distance_up`; `execute if score #rand lbc.math matches 20 run function lbc:other/attributes/scale_up_tiny`; `execute if score #rand lbc.math matches 21 run function lbc:other/attributes/sneaking_speed_up`; `execute if score #rand lbc.math matches 22 run function lbc:other/attributes/step_height_up`; `execute if score #rand lbc.math matches 23 run function lbc:other/attributes/submerged_mining_speed_up`; `execute if score #rand lbc.math matches 24 run function lbc:other/attributes/sweeping_damage_ratio_up`; `execute if score #rand lbc.math matches 25 run function lbc:other/attributes/water_movement_efficiency_up`; `execute if score #rand lbc.math matches 26 run function lbc:other/attributes/movement_speed_up_tiny`

## Передача предметов из инвентаря в item entities

Обычный caller `lbc:projectiles/gadgets/honey_pl` проверяет каждый присутствующий слот 0..35 и вызывает соответствующий helper; затем отдельно проверяет offhand и четыре armor slots. Поэтому функция с конкретным slot не выбирает его сама: ветку выбирает caller по факту наличия предмета.

Каждый `drop_inv` helper создаёт рядом item entity на основе временного `minecraft:stone`, копирует в неё поле `Item` из выбранного слота и снимает временный tag `modifyitem`. Сам helper исходный слот не очищает: ручной вызов оставляет оригинал рядом с копией. Caller `projectiles/gadgets/honey_pl` вызывает все 41 helper (slots 0..35, offhand и четыре armor slots), затем сам заменяет inventory/offhand/armor игрока на air; очистка выполняется caller-ом. `PickupDelay:40` равен 40 game ticks (2 секунды). Варианты различаются источником поля: `0..35` — inventory, `100..103` — boots/legs/chest/head, `-106` — offhand.

| Файл | Источник предмета |
|---|---|
| `-106` | `equipment.offhand` |
| `0` | `Inventory[{Slot:0b}]` |
| `1` | `Inventory[{Slot:1b}]` |
| `10` | `Inventory[{Slot:10b}]` |
| `100` | `equipment.feet` |
| `101` | `equipment.legs` |
| `102` | `equipment.chest` |
| `103` | `equipment.head` |
| `11` | `Inventory[{Slot:11b}]` |
| `12` | `Inventory[{Slot:12b}]` |
| `13` | `Inventory[{Slot:13b}]` |
| `14` | `Inventory[{Slot:14b}]` |
| `15` | `Inventory[{Slot:15b}]` |
| `16` | `Inventory[{Slot:16b}]` |
| `17` | `Inventory[{Slot:17b}]` |
| `18` | `Inventory[{Slot:18b}]` |
| `19` | `Inventory[{Slot:19b}]` |
| `2` | `Inventory[{Slot:2b}]` |
| `20` | `Inventory[{Slot:20b}]` |
| `21` | `Inventory[{Slot:21b}]` |
| `22` | `Inventory[{Slot:22b}]` |
| `23` | `Inventory[{Slot:23b}]` |
| `24` | `Inventory[{Slot:24b}]` |
| `25` | `Inventory[{Slot:25b}]` |
| `26` | `Inventory[{Slot:26b}]` |
| `27` | `Inventory[{Slot:27b}]` |
| `28` | `Inventory[{Slot:28b}]` |
| `29` | `Inventory[{Slot:29b}]` |
| `3` | `Inventory[{Slot:3b}]` |
| `30` | `Inventory[{Slot:30b}]` |
| `31` | `Inventory[{Slot:31b}]` |
| `32` | `Inventory[{Slot:32b}]` |
| `33` | `Inventory[{Slot:33b}]` |
| `34` | `Inventory[{Slot:34b}]` |
| `35` | `Inventory[{Slot:35b}]` |
| `4` | `Inventory[{Slot:4b}]` |
| `5` | `Inventory[{Slot:5b}]` |
| `6` | `Inventory[{Slot:6b}]` |
| `7` | `Inventory[{Slot:7b}]` |
| `8` | `Inventory[{Slot:8b}]` |
| `9` | `Inventory[{Slot:9b}]` |


## Новый игровой цикл: legacy предметы и очистка мира

`click` вызывается из `spell1_default` для игрока с `item_balance:1`; он временно отключает три gamerule drops, перемещает и убивает неигровые сущности кроме `supernovacore`, очищает inventory/effects игроков текущего измерения, сбрасывает расписания `everythingend/*` и `baffledend/*`, сбрасывает состояния, возвращает gamerules, вызывает SWRG очистку карты/GUI/атрибутов/сундуков, ставит четыре SWRG templates, включает `legacy=1`, выдаёт уровни Resistance V, Instant Health V и Saturation XLI на5 секунд и grants `new_world` nonspectators. Вызовы `swrg:*` остаются внешней границей. Четыре legacy scheduler-а повторяют работу через1t,1s,5s и5t, обслуживают всех nonspectator игроков текущего измерения и перепланируются без проверки `legacy`; `end_resets` очищает все четыре расписания. Каждый5t player tick убирает5 из восьми старых cooldown-score; при `manabelt_old` helper дополнительно убирает2 из семи, при `magichat_old` —4 из тех же семи (ни belt, ни hat helper не меняют `coldown_wiki`). Старые artifact helpers вызываются по legacy custom_data: `mystical_old` type1 снимает blindness/slowness/weakness/strength/levitation/slow_falling/haste/mining_fatigue, type3 снимает regeneration/wither/poison/instant_damage/instant_health/absorption/health_boost; type2 убивает area_effect_cloud, evoker_fangs, experience_bottle/orb и dragon_fireball в4 блоках; type5 превращает TNT в item, fireball в fire_charge и end_crystal в соответствующие item в пределах5/6/7, затем удаляет рядом creepers/TNT/fireball/crystals; type6 заменяет lava на basalt и water на frosted_ice в кубе5×5×5. type4 превращает arrow entities до4 блоков в выпавшие arrows и убивает исходные снаряды. Экипированная `reactive_old` железная обувь каждый тик даёт Slow Falling на1с и cloud particle; `heall_old` раз в5с даёт Regeneration III на1с, а `jumpboost_old` каждую секунду — Jump Boost IV на1с. Эти эффекты перепроверены в дочерних artifact-функциях, хотя сами файлы не входят в manifest задания.

Проверенные caller/зависимости: [spell1_default](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/spell1_default.mcfunction), [legacy_tick_5t_player](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/new_game/legacy_tick_5t_player.mcfunction), [end_resets](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/end_resets.mcfunction), [старые effect-removal helpers](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/noeffects1_old.mcfunction), [старые hazard-removal helpers](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/notnt_old.mcfunction), [fluid cleanup](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/nofluid_old.mcfunction), [arrow cleanup](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/noarrows_old.mcfunction).

### `new_game/click`

- вызов: `function lbc:resets`; `function lbc:reset_gamerules`; `function lbc:other/new_game/legacy_tick`; `function lbc:other/new_game/legacy_tick_1s`; `function lbc:other/new_game/legacy_tick_5t`; `function lbc:other/new_game/legacy_tick_5s`; `function lbc:swrg_kit_integration/game/levelup_select/levelups_remove`
- эффект: `effect give @a minecraft:resistance 5 4 true`; `effect give @a minecraft:instant_health 5 4 true`; `effect give @a minecraft:saturation 5 40 true`
- счётчик: `scoreboard players set darkness lbc.math 0`; `scoreboard players set abyss lbc.math 0`; `scoreboard players set powerfall lbc.math 0`; `scoreboard players set trollhunter lbc.math 0`; `scoreboard players set @a swrg.lives 1`; `scoreboard players set legacy lbc.math 1`; `scoreboard players set #lbcskill swrg.math 0`
- gamerule: `gamerule block_drops false`; `gamerule entity_drops false`; `gamerule mob_drops false`; `gamerule block_drops true`; `gamerule entity_drops true`; `gamerule mob_drops true`
- перемещение: `tp @e[type=!minecraft:player,tag=!supernovacore] 0 -600000 0`; `tp @a 0 128 0`; `tp @r[gamemode=!spectator,x=0,y=128,z=0,distance=..20] @s`; `tp @s ~ ~1 ~ facing entity @e[type=minecraft:marker,tag=swrg.look,limit=1]`

### `new_game/legacy_tick`

- вызов: `function lbc:other/new_game/legacy_tick_player`; `function lbc:other/new_game/legacy_tick`
- повтор: `schedule function lbc:other/new_game/legacy_tick 1`

### `new_game/legacy_tick_1s`

- вызов: `function lbc:other/new_game/legacy_tick_1s_player`; `function lbc:other/new_game/legacy_tick_1s`
- повтор: `schedule function lbc:other/new_game/legacy_tick_1s 1s`
- очистка блоков: `fill ~5 ~5 ~5 ~-5 ~-5 ~-5 minecraft:air replace #minecraft:portals_restricted strict`

### `new_game/legacy_tick_1s_player`

- вызов: `function lbc:other/artifact/noeffects1_old`; `function lbc:other/artifact/nohp_old`
- проверка legacy custom_data: `minecraft:custom_data":{mystical_old:1,type:1}}}]}] run function lbc:other/artifact/noeffects1_old`; `minecraft:custom_data":{mystical_old:1,type:3}}}]}] run function lbc:other/artifact/nohp_old`; `minecraft:custom_data~{jumpboost_old:1}] run effect give @s jump_boost 1 3`
- эффект: `effect give @s jump_boost 1 3`

### `new_game/legacy_tick_5s`

- вызов: `function lbc:other/new_game/legacy_tick_5s_player`; `function lbc:other/new_game/legacy_tick_5s`
- повтор: `schedule function lbc:other/new_game/legacy_tick_5s 5s`

### `new_game/legacy_tick_5s_player`

- проверка legacy custom_data: `minecraft:custom_data":{heall_old:1}}}]}] minecraft:regeneration 1 2 true`
- эффект: `effect give @s[nbt={Inventory:[{id:"minecraft:repeating_command_block",components:{"minecraft:custom_data":{heall_old:1}}}]}] minecraft:regeneration 1 2 true`

### `new_game/legacy_tick_5t`

- вызов: `function lbc:other/new_game/legacy_tick_5t_player`; `function lbc:other/new_game/legacy_tick_5t`
- повтор: `schedule function lbc:other/new_game/legacy_tick_5t 5t`

### `new_game/legacy_tick_5t_player`

- вызов: `function lbc:other/new_game/magic_boost_hat`; `function lbc:other/new_game/magic_boost_belt`; `function lbc:other/artifact/nomagic_old`; `function lbc:other/artifact/notnt_old`; `function lbc:other/artifact/nofluid_old`
- проверка legacy custom_data: `minecraft:custom_data~{magichat_old:1}] run function lbc:other/new_game/magic_boost_hat`; `minecraft:custom_data":{manabelt_old:1}}}]}] run function lbc:other/new_game/magic_boost_belt`; `minecraft:custom_data":{mystical_old:1,type:2}}}]}] run function lbc:other/artifact/nomagic_old`; `minecraft:custom_data":{mystical_old:1,type:5}}}]}] run function lbc:other/artifact/notnt_old`; `minecraft:custom_data":{mystical_old:1,type:6}}}]}] run function lbc:other/artifact/nofluid_old`
- счётчик: `scoreboard players remove @s coldown_evoker 5`; `scoreboard players remove @s coldown_bass 5`; `scoreboard players remove @s coldown_ender 5`; `scoreboard players remove @s coldown_corrupt 5`; `scoreboard players remove @s coldown_vampire 5`; `scoreboard players remove @s coldown_foxus 5`; `scoreboard players remove @s coldown_blood 5`; `scoreboard players remove @s coldown_wiki 5`

### `new_game/legacy_tick_player`

- вызов: `function lbc:other/artifact/noarrows_old`
- проверка legacy custom_data: `minecraft:custom_data":{mystical_old:1,type:4}}}]}] run function lbc:other/artifact/noarrows_old`; `minecraft:custom_data~{reactive_old:1}] run particle minecraft:cloud ~ ~-0.2 ~ 0.2 0.02 0.2 0.01 1 force`; `minecraft:custom_data~{reactive_old:1}] run effect give @s minecraft:slow_falling 1 0 true`
- эффект: `effect give @s minecraft:slow_falling 1 0 true`

### `new_game/magic_boost_belt`

- счётчик: `scoreboard players remove @s coldown_evoker 2`; `scoreboard players remove @s coldown_bass 2`; `scoreboard players remove @s coldown_ender 2`; `scoreboard players remove @s coldown_corrupt 2`; `scoreboard players remove @s coldown_vampire 2`; `scoreboard players remove @s coldown_foxus 2`; `scoreboard players remove @s coldown_blood 2`

### `new_game/magic_boost_hat`

- счётчик: `scoreboard players remove @s coldown_evoker 4`; `scoreboard players remove @s coldown_bass 4`; `scoreboard players remove @s coldown_ender 4`; `scoreboard players remove @s coldown_corrupt 4`; `scoreboard players remove @s coldown_vampire 4`; `scoreboard players remove @s coldown_foxus 4`; `scoreboard players remove @s coldown_blood 4`

## Вспомогательные генераторы

### `other/attributes/gen.py`

Этот ручной Python-скрипт не запускается Minecraft datapack-ом. Если его запустить из `other/attributes`, он перезапишет 26 перечисленных Godclaw-функций шаблоном `base get 1000`, `tmp1` прибавляется и результат умножается на0.001; таким образом он стирает фактические индивидуальные scale и меняет знак у двух текущих исключений (`burning_time_up_godclaw`, `fall_damage_multiplier_up_godclaw`). Для runtime описания значимы текущие `.mcfunction` выше, а не предполагаемый результат скрипта.

Команды: `files=["armor_up_godclaw.mcfunction","armor_toughness_up_godclaw.mcfunction","attack_damage_up_godclaw.mcfunction","attack_knockback_up_godclaw.mcfunction","attack_speed_up_godclaw.mcfunction","burning_time_up_godclaw.mcfunction","explosion_knockback_resistance_up_godclaw.mcfunction","fall_damage_multiplier_up_godclaw.mcfunction","jump_strength_up_godclaw.mcfunction","knockback_resistance_up_godclaw.mcfunction","luck_up_godclaw.mcfunction","max_health_up_godclaw.mcfunction","movement_efficiency_up_godclaw.mcfunction","movement_speed_up_godclaw.mcfunction","oxygen_bonus_up_godclaw.mcfunction","safe_fall_distance_up_godclaw.mcfunction","scale_up_godclaw.mcfunction","step_height_up_godclaw.mcfunction","water_movement_efficiency_up_godclaw.mcfunction","block_break_speed_up_godclaw.mcfunction","block_interaction_range_up_godclaw.mcfunction","entity_interaction_range_up_godclaw.mcfunction","mining_efficiency_up_godclaw.mcfunction","sneaking_speed_up_godclaw.mcfunction","submerged_mining_speed_up_godclaw.mcfunction","sweeping_damage_ratio_up_godclaw.mcfunction"]`; `attributes_no_prefix=["armor","armor_toughness","attack_damage","attack_knockback","attack_speed","burning_time","explosion_knockback_resistance","fall_damage_multiplier","jump_strength","knockback_resistance","luck","max_health","movement_efficiency","movement_speed","oxygen_bonus","safe_fall_distance","scale","step_height","water_movement_efficiency","block_break_speed","block_interaction_range","entity_interaction_range","mining_efficiency","sneaking_speed","submerged_mining_speed","sweeping_damage_ratio"]`; `attributes=["minecraft:armor","minecraft:armor_toughness","minecraft:attack_damage","minecraft:attack_knockback","minecraft:attack_speed","minecraft:burning_time","minecraft:explosion_knockback_resistance","minecraft:fall_damage_multiplier","minecraft:jump_strength","minecraft:knockback_resistance","minecraft:luck","minecraft:max_health","minecraft:movement_efficiency","minecraft:movement_speed","minecraft:oxygen_bonus","minecraft:safe_fall_distance","minecraft:scale","minecraft:step_height","minecraft:water_movement_efficiency","minecraft:block_break_speed","minecraft:block_interaction_range","minecraft:entity_interaction_range","minecraft:mining_efficiency","minecraft:sneaking_speed","minecraft:submerged_mining_speed","minecraft:sweeping_damage_ratio"]`; `for i in range(len(files)):`; `print(files[i],attributes_no_prefix[i],attributes[i])`; `f=open(f"{files[i]}",mode='w')`; `f.write(f"execute store result score tmp lbc.math run attribute @s {attributes[i]} base get 1000\n")`; `f.write("scoreboard players operation tmp lbc.math += tmp1 lbc.math\n")`; `f.write("execute store result storage lbcmath tmp double 0.001 run scoreboard players get tmp lbc.math\n")`; `f.write(f"function lbc:other/attributes/{attributes_no_prefix[i]}_update with storage minecraft:lbcmath\n")`; `f.close()`

### `other/attributes/pills/pillgen.py`

Это ручной генератор, не datapack-функция. При запуске из каталога `pills` он перезаписывает 91 файл из списка имен, сдвигая локальный счётчик `j` от53 до144, но не используя его при записи; IDs таблеток в эти функции скрипт не добавляет; каждый выходной файл содержит только `effect give @s <имя>`, subtitle и advancement revoke. Из-за этого шаблон не сохраняет текущие длительности/amplifier, особые scoreboard/tag результаты и attribute calls; `i_found_pills` в таком шаблоне даже попадает как имя status effect, хотя checked-in функция отдельного игрового эффекта не содержит. Текущее поведение описано по `.mcfunction`, а не по генератору.

Команды: `a=["absorption","absorption_99","blindness","conduit_power_1","conduit_power_2","darkness","dolphins_grace","fire_resistance","fire_resistance_1","fire_resistance_2","glowing","haste","haste_99","health_boost","hero_of_the_village","hunger","hunger_99","infested","instant_damage","instant_damage_2","instant_health","invisibility","invisibility_1","jump_boost","jump_boost_99","levitation","levitation_99","luck","luck_99","mining_fatigue","mining_fatigue_99","nausea","night_vision","oozing","poison","raid_omen_16","raid_omen_17","raid_omen_18","raid_omen_19","raid_omen_20","raid_omen_74","raid_omen_40","raid_omen_41","raid_omen_42","raid_omen_101","raid_omen_102","raid_omen_103","raid_omen_104","raid_omen_105","raid_omen_106","raid_omen_107","raid_omen_108","raid_omen_109","raid_omen_110","raid_omen_111","raid_omen_112","raid_omen_113","raid_omen_114","raid_omen_115","raid_omen_116","raid_omen_117","raid_omen_118","raid_omen_119","raid_omen_120","raid_omen_121","raid_omen_122","raid_omen_123","raid_omen_124","regeneration","resistance","resistance_5","saturation","slow_falling","slowness","speed","strength","strength_99","unluck","unluck_99","water_breathing","water_breathing_2","weakness","weakness_99","weaving","wind_charged","wither","wither_99","paralysis","excited","drowsy","i_found_pills"]`; `j=53`; `for i in a:`; `j+=1`; `f=open(f'{i}.mcfunction',mode='w')`; `k='effect give @s '+i+'\n'`; `f.write(k)`; `k='title @s title " "'+'\n'`; `f.write(k)`; `k='title @s subtitle {"translate":"pill_'+i+'"}'+'\n'`; `f.write(k)`; `k='advancement revoke @s only lbc:pills/pill_'+i`; `f.write(k)`; `f.close()`

