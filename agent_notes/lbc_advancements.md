# События предметов и достижения LBC

Эта справка описывает все 651 advancement JSON и пять вспомогательных файлов: событие, фильтры, условия завершения и награду. Фактический эффект наградной функции описывается в соответствующем разделе LBC. Ссылка на функцию позволяет проверить его отдельно; название достижения не доказывает эффект.

Каждый критерий сохраняется у игрока. При отсутствии `requirements` нужны все критерии; при наличии массива нужны все его группы, а внутри каждой группы достаточно одного критерия. Награда срабатывает при завершении advancement. Повторное срабатывание требует его отзыва (обычно через `advancement revoke`); ниже перечислены найденные буквальные grant/revoke, но динамически составленные ID могут добавлять вызовы.

Фильтры приведены без потери чисел и ключей: `custom_data` — идентификатор предмета, `equipment.mainhand` — предмет в основной руке, `playerGameType:3` — зритель. НЕ/И/ИЛИ показывают вложенную логику. Ограничение измерения применяется только там, где оно действительно записано внутри `conditions`.

## `lbc:aimbobapple`

Источник: [lbc:aimbobapple](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/aimbobapple.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{aimbobapple:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/aimbob_apple](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/aimbob_apple.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:aimbobapple`; [other/aimbob_apple.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/aimbob_apple.mcfunction) — `advancement revoke @s only lbc:aimbobapple`.

## `lbc:any_shield`

Источник: [lbc:any_shield](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/any_shield.json).
Критерий `requirement`: игрок продолжает использовать предмет. Условия: предмет: `{"items":["minecraft:shield"]}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:items/shields/shield_block](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/shields/shield_block.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [items/shields/shield_block.mcfunction:4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/shields/shield_block.mcfunction) — `advancement revoke @s only lbc:any_shield`; [leave_death.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:any_shield`.

## `lbc:armor_toughnessattr`

Источник: [lbc:armor_toughnessattr](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/armor_toughnessattr.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{armor_toughnessattribute:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/armor_toughness](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/armor_toughness.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:7](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:armor_toughnessattr`; [other/attributes/armor_toughness.mcfunction:10](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/armor_toughness.mcfunction) — `advancement revoke @s only lbc:armor_toughnessattr`.

## `lbc:armorattr`

Источник: [lbc:armorattr](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/armorattr.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{armorattribute:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/armor](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/armor.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:6](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:armorattr`; [other/attributes/armor.mcfunction:9](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/armor.mcfunction) — `advancement revoke @s only lbc:armorattr`.

## `lbc:attack_damageattr`

Источник: [lbc:attack_damageattr](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/attack_damageattr.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{attack_damageattribute:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/attack_damage](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/attack_damage.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:8](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:attack_damageattr`; [other/attributes/attack_damage.mcfunction:10](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/attack_damage.mcfunction) — `advancement revoke @s only lbc:attack_damageattr`.

## `lbc:attack_speedattr`

Источник: [lbc:attack_speedattr](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/attack_speedattr.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{attack_speedattribute:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/attack_speed](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/attack_speed.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:9](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:attack_speedattr`; [other/attributes/attack_speed.mcfunction:9](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/attack_speed.mcfunction) — `advancement revoke @s only lbc:attack_speedattr`.

## `lbc:badapple`

Источник: [lbc:badapple](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/badapple.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{bad_apple:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/bad_apple](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/bad_apple.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:10](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:badapple`; [other/bad_apple.mcfunction:16](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/bad_apple.mcfunction) — `advancement revoke @s only lbc:badapple`.

## `lbc:blood_slab`

Источник: [lbc:blood_slab](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/blood_slab.json).
Критерий `requirement`: игрок ставит блок. Условия: место: (использованный предмет соответствует `{"predicates":{"minecraft:custom_data":"{blood_slab:1}"}}`); игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/imba/blood_slab/place](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/blood_slab/place.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:11](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:blood_slab`; [other/imba/blood_slab/place.mcfunction:7](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/blood_slab/place.mcfunction) — `advancement revoke @s only lbc:blood_slab`.

## `lbc:bow_pulling`

Источник: [lbc:bow_pulling](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/bow_pulling.json).
Критерий `requirement`: игрок продолжает использовать предмет. Условия: предмет: `{"items":["minecraft:bow"]}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:items/bows/charge_add](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/bows/charge_add.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [items/bows/charge_add.mcfunction:9](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/bows/charge_add.mcfunction) — `advancement revoke @s only lbc:bow_pulling`; [leave_death.mcfunction:12](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:bow_pulling`.

## `lbc:broken_totem`

Источник: [lbc:broken_totem](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/broken_totem.json).
Критерий `requirement`: игрок спасается тотемом. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{broken_totem:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:swrg_kit_integration/items/broken_totem](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/items/broken_totem.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:23](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:broken_totem`; [swrg_kit_integration/items/broken_totem.mcfunction:3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/items/broken_totem.mcfunction) — `advancement revoke @s only lbc:broken_totem`.

## `lbc:challenges/no_knowledge`

Источник: [lbc:challenges/no_knowledge](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/challenges/no_knowledge.json).
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Награда: выполнить [lbc:swrg_kit_integration/gain/challenge_completed](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/challenge_completed.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [other/magic_academy/completed_challenge.mcfunction:1](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/completed_challenge.mcfunction) — `advancement grant @s only lbc:challenges/no_knowledge`.

## `lbc:cocksuckers`

Источник: [lbc:cocksuckers](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/cocksuckers.json).
Критерий `requirement`: игрок получает урон от сущности. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:items/damaged](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/damaged.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [items/damaged.mcfunction:1](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/damaged.mcfunction) — `advancement revoke @s only lbc:cocksuckers`; [leave_death.mcfunction:13](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:cocksuckers`.

## `lbc:convert_arrows_supply`

Источник: [lbc:convert_arrows_supply](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/convert_arrows_supply.json).
Критерий `requirement`: игрок взаимодействует с сущностью. Условия: предмет: `{"items":["minecraft:arrow"],"count":{"min":2}}`; сущность: `{"type":"minecraft:interaction","nbt":"{Tags:[\"supply_drop\"]}"}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/techno/trade_arrows](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/trade_arrows.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:14](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:convert_arrows_supply`; [other/techno/trade_arrows.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/trade_arrows.mcfunction) — `advancement revoke @s only lbc:convert_arrows_supply`.

## `lbc:crossbow`

Источник: [lbc:crossbow](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/crossbow.json).
Критерий `requirement`: игрок стреляет из арбалета. Условия: игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:items/crossbows](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/crossbows.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [items/crossbows.mcfunction:26](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/crossbows.mcfunction) — `advancement revoke @s only lbc:crossbow`; [leave_death.mcfunction:15](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:crossbow`; [other/twitching_bow.mcfunction:11](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/twitching_bow.mcfunction) — `advancement grant @s only lbc:crossbow`; [other/twitching_crossbow.mcfunction:17](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/twitching_crossbow.mcfunction) — `advancement grant @s only lbc:crossbow`.

## `lbc:crossbow_charging`

Источник: [lbc:crossbow_charging](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/crossbow_charging.json).
Критерий `requirement`: игрок продолжает использовать предмет. Условия: предмет: `{"items":["minecraft:crossbow"]}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:items/crossbow_charging](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/crossbow_charging.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [items/crossbow_charging.mcfunction:3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/crossbow_charging.mcfunction) — `advancement revoke @s only lbc:crossbow_charging`; [leave_death.mcfunction:16](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:crossbow_charging`.

## `lbc:crossbow_gatling`

Источник: [lbc:crossbow_gatling](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/crossbow_gatling.json).
Критерий `requirement`: игрок продолжает использовать предмет. Условия: предмет: `{"items":["minecraft:crossbow"],"predicates":{"minecraft:custom_data":"{imba_gatlingator:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:items/crossbow/imba_gatlingator_crossbow](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/crossbow/imba_gatlingator_crossbow.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [items/crossbow/imba_gatlingator_crossbow.mcfunction:4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/crossbow/imba_gatlingator_crossbow.mcfunction) — `advancement revoke @s only lbc:crossbow_gatling`; [leave_death.mcfunction:17](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:crossbow_gatling`.

## `lbc:cum_slab`

Источник: [lbc:cum_slab](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/cum_slab.json).
Критерий `requirement`: игрок ставит блок. Условия: место: (использованный предмет соответствует `{"items":["minecraft:birch_slab"],"predicates":{"minecraft:custom_data":"{cum_slab:1}"}}`); игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/imba/place_cum_slab](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/place_cum_slab.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:18](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:cum_slab`; [other/imba/place_cum_slab.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/place_cum_slab.mcfunction) — `advancement revoke @s only lbc:cum_slab`.

## `lbc:damaged_by_player`

Источник: [lbc:damaged_by_player](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/damaged_by_player.json).
Критерий `requirement`: игрок получает урон от сущности. Условия: игрок: (свойства `this`: `{"nbt":"{Inventory:[{components:{\"minecraft:custom_data\":{mystical:4,type:1}}}]}"}`) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)); урон: `{"source_entity":{"type":"#minecraft:attacks"}}`.
Награда: выполнить [lbc:items/damaged_by_player](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/damaged_by_player.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [items/damaged_by_player.mcfunction:3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/damaged_by_player.mcfunction) — `advancement revoke @s only lbc:damaged_by_player`; [leave_death.mcfunction:19](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:damaged_by_player`.

## `lbc:damaged_by_super_guardian`

Источник: [lbc:damaged_by_super_guardian](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/damaged_by_super_guardian.json).
Критерий `requirement`: игрок получает урон от сущности. Условия: игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)); урон: `{"source_entity":{"type":"minecraft:guardian","nbt":"{Tags:[super_guardian]}"}}`.
Награда: выполнить [lbc:workingitems/siggewinne_syringe/super_guardian_damage](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/siggewinne_syringe/super_guardian_damage.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:damaged_by_super_guardian`; [workingitems/siggewinne_syringe/super_guardian_damage.mcfunction:1](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/siggewinne_syringe/super_guardian_damage.mcfunction) — `advancement revoke @s only lbc:damaged_by_super_guardian`.

## `lbc:death_skythe`

Источник: [lbc:death_skythe](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/death_skythe.json).
Критерий `requirement`: игрок убивает сущность. Условия: игрок: (свойства `this`: `{"equipment":{"mainhand":{"predicates":{"minecraft:custom_data":"{death_skythe:1}"}}}}`) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:workingitems/death_skythe_kill](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/death_skythe_kill.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:20](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:death_skythe`; [workingitems/death_skythe_kill.mcfunction:8](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/death_skythe_kill.mcfunction) — `advancement revoke @s only lbc:death_skythe`.

## `lbc:debris_place`

Источник: [lbc:debris_place](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/debris_place.json).
Критерий `requirement`: игрок ставит блок. Условия: место: (использованный предмет соответствует `{"items":["minecraft:ancient_debris"],"predicates":{"minecraft:custom_data":"{debriser:1}"}}`); игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:workingitems/imba/place_swastika](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/imba/place_swastika.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:21](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:debris_place`; [workingitems/imba/place_swastika.mcfunction:4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/imba/place_swastika.mcfunction) — `advancement revoke @s only lbc:debris_place`.

## `lbc:devil_totem`

Источник: [lbc:devil_totem](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/devil_totem.json).
Критерий `requirement`: игрок спасается тотемом. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{deviltotem:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/deviltotem](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/deviltotem.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:22](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:devil_totem`; [other/deviltotem.mcfunction:2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/deviltotem.mcfunction) — `advancement revoke @s only lbc:devil_totem`.

## `lbc:dirter_place`

Источник: [lbc:dirter_place](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/dirter_place.json).
Критерий `requirement`: игрок ставит блок. Условия: место: (использованный предмет соответствует `{"items":["minecraft:coarse_dirt"],"predicates":{"minecraft:custom_data":"{dirter:1}"}}`); игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:workingitems/imba/place_ground](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/imba/place_ground.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:24](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:dirter_place`; [workingitems/imba/place_ground.mcfunction:8](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/imba/place_ground.mcfunction) — `advancement revoke @s only lbc:dirter_place`.

## `lbc:eat_banana`

Источник: [lbc:eat_banana](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_banana.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{eat_banana:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/banana](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/banana.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [other/eatables/banana.mcfunction:1](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/banana.mcfunction) — `advancement revoke @s only lbc:eat_banana`.

## `lbc:eat_bedrock_apple`

Источник: [lbc:eat_bedrock_apple](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_bedrock_apple.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{bedrock_apple:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/bedrock_apple](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/bedrock_apple.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:25](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_bedrock_apple`; [other/eatables/bedrock_apple.mcfunction:10](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/bedrock_apple.mcfunction) — `advancement revoke @s only lbc:eat_bedrock_apple`.

## `lbc:eat_big_explode`

Источник: [lbc:eat_big_explode](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_big_explode.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{eat_big_explode:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/big_explode](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/big_explode.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:26](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_big_explode`; [other/eatables/big_explode.mcfunction:2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/big_explode.mcfunction) — `advancement revoke @s only lbc:eat_big_explode`.

## `lbc:eat_book`

Источник: [lbc:eat_book](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_book.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{eat_book:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/book](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/book.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:27](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_book`; [other/eatables/book.mcfunction:17](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/book.mcfunction) — `advancement revoke @s only lbc:eat_book`.

## `lbc:eat_buffs`

Источник: [lbc:eat_buffs](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_buffs.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{eat_buffs:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/buffs](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/buffs.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:28](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_buffs`; [other/eatables/buffs.mcfunction:4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/buffs.mcfunction) — `advancement revoke @s only lbc:eat_buffs`.

## `lbc:eat_chaos_explode`

Источник: [lbc:eat_chaos_explode](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_chaos_explode.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{eat_chaos_explode:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/chaos_explode](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/chaos_explode.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:29](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_chaos_explode`; [other/eatables/chaos_explode.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/chaos_explode.mcfunction) — `advancement revoke @s only lbc:eat_chaos_explode`.

## `lbc:eat_cum`

Источник: [lbc:eat_cum](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_cum.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{eat_cum:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/cum](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/cum.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:30](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_cum`; [other/eatables/cum.mcfunction:16](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/cum.mcfunction) — `advancement revoke @s only lbc:eat_cum`.

## `lbc:eat_damage`

Источник: [lbc:eat_damage](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_damage.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{eat_damage:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/damage](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/damage.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:31](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_damage`; [other/eatables/damage.mcfunction:2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/damage.mcfunction) — `advancement revoke @s only lbc:eat_damage`.

## `lbc:eat_damage_all`

Источник: [lbc:eat_damage_all](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_damage_all.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{eat_damage_all:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:workingitems/omegaflag](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/omegaflag.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:32](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_damage_all`; [workingitems/omegaflag.mcfunction:12](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/omegaflag.mcfunction) — `advancement revoke @s only lbc:eat_damage_all`; [workingitems/omegaflag_upgraded.mcfunction:7](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/omegaflag_upgraded.mcfunction) — `advancement revoke @s only lbc:eat_damage_all`.

## `lbc:eat_damage_penis`

Источник: [lbc:eat_damage_penis](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_damage_penis.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{random_damage:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/random_damage](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/random_damage.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:33](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_damage_penis`; [other/attributes/random_damage.mcfunction:9](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/random_damage.mcfunction) — `advancement revoke @s only lbc:eat_damage_penis`.

## `lbc:eat_danmaku`

Источник: [lbc:eat_danmaku](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_danmaku.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{eat_danmaku:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/danmaku](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/danmaku.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:34](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_danmaku`; [other/eatables/danmaku.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/danmaku.mcfunction) — `advancement revoke @s only lbc:eat_danmaku`.

## `lbc:eat_diamond_apple`

Источник: [lbc:eat_diamond_apple](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_diamond_apple.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{diamond_apple:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/diamond_apple](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/diamond_apple.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:35](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_diamond_apple`; [other/eatables/diamond_apple.mcfunction:7](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/diamond_apple.mcfunction) — `advancement revoke @s only lbc:eat_diamond_apple`.

## `lbc:eat_diamond_beef`

Источник: [lbc:eat_diamond_beef](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_diamond_beef.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{diamond_beef:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/diamond_beef](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/diamond_beef.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:36](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_diamond_beef`; [other/eatables/diamond_beef.mcfunction:6](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/diamond_beef.mcfunction) — `advancement revoke @s only lbc:eat_diamond_beef`.

## `lbc:eat_diamond_berries`

Источник: [lbc:eat_diamond_berries](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_diamond_berries.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{diamond_berries:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/diamond_berries](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/diamond_berries.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:37](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_diamond_berries`; [other/eatables/diamond_berries.mcfunction:3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/diamond_berries.mcfunction) — `advancement revoke @s only lbc:eat_diamond_berries`.

## `lbc:eat_diamond_carrot`

Источник: [lbc:eat_diamond_carrot](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_diamond_carrot.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{diamond_carrot:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/diamond_carrot](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/diamond_carrot.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:38](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_diamond_carrot`; [other/eatables/diamond_carrot.mcfunction:9](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/diamond_carrot.mcfunction) — `advancement revoke @s only lbc:eat_diamond_carrot`.

## `lbc:eat_diamond_chicken`

Источник: [lbc:eat_diamond_chicken](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_diamond_chicken.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{diamond_chicken:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/diamond_chicken](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/diamond_chicken.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:39](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_diamond_chicken`; [other/eatables/diamond_chicken.mcfunction:12](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/diamond_chicken.mcfunction) — `advancement revoke @s only lbc:eat_diamond_chicken`.

## `lbc:eat_diamond_fish`

Источник: [lbc:eat_diamond_fish](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_diamond_fish.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{diamond_fish:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/diamond_fish](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/diamond_fish.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:40](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_diamond_fish`; [other/eatables/diamond_fish.mcfunction:7](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/diamond_fish.mcfunction) — `advancement revoke @s only lbc:eat_diamond_fish`.

## `lbc:eat_diamond_mutton`

Источник: [lbc:eat_diamond_mutton](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_diamond_mutton.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{diamond_mutton:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/diamond_mutton](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/diamond_mutton.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:41](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_diamond_mutton`; [other/eatables/diamond_mutton.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/diamond_mutton.mcfunction) — `advancement revoke @s only lbc:eat_diamond_mutton`.

## `lbc:eat_diamond_porkchop`

Источник: [lbc:eat_diamond_porkchop](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_diamond_porkchop.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{diamond_porkchop:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/diamond_porkchop](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/diamond_porkchop.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:42](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_diamond_porkchop`; [other/eatables/diamond_porkchop.mcfunction:4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/diamond_porkchop.mcfunction) — `advancement revoke @s only lbc:eat_diamond_porkchop`.

## `lbc:eat_diamond_potato`

Источник: [lbc:eat_diamond_potato](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_diamond_potato.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{diamond_potato:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/diamond_potato](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/diamond_potato.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:43](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_diamond_potato`; [other/eatables/diamond_potato.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/diamond_potato.mcfunction) — `advancement revoke @s only lbc:eat_diamond_potato`.

## `lbc:eat_diamond_rabbit`

Источник: [lbc:eat_diamond_rabbit](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_diamond_rabbit.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{diamond_rabbit:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/diamond_rabbit](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/diamond_rabbit.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:44](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_diamond_rabbit`; [other/eatables/diamond_rabbit.mcfunction:4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/diamond_rabbit.mcfunction) — `advancement revoke @s only lbc:eat_diamond_rabbit`.

## `lbc:eat_diamond_stew`

Источник: [lbc:eat_diamond_stew](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_diamond_stew.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{diamond_stew:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/diamond_stew](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/diamond_stew.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:45](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_diamond_stew`; [other/eatables/diamond_stew.mcfunction:6](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/diamond_stew.mcfunction) — `advancement revoke @s only lbc:eat_diamond_stew`.

## `lbc:eat_evoker`

Источник: [lbc:eat_evoker](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_evoker.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{eat_evoker:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/evoker](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/evoker.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:46](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_evoker`; [other/eatables/evoker.mcfunction:2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/evoker.mcfunction) — `advancement revoke @s only lbc:eat_evoker`.

## `lbc:eat_explode`

Источник: [lbc:eat_explode](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_explode.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{eat_explode:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/explode](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/explode.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:47](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_explode`; [other/eatables/explode.mcfunction:2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/explode.mcfunction) — `advancement revoke @s only lbc:eat_explode`.

## `lbc:eat_fast_penis`

Источник: [lbc:eat_fast_penis](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_fast_penis.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{random_speed:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/random_speed](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/random_speed.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:48](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_fast_penis`; [other/attributes/random_speed.mcfunction:9](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/random_speed.mcfunction) — `advancement revoke @s only lbc:eat_fast_penis`.

## `lbc:eat_fire`

Источник: [lbc:eat_fire](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_fire.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{eat_fire:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/fire](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/fire.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:49](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_fire`; [other/eatables/fire.mcfunction:2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/fire.mcfunction) — `advancement revoke @s only lbc:eat_fire`.

## `lbc:eat_glowing`

Источник: [lbc:eat_glowing](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_glowing.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{eat_glowing:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/glowing](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/glowing.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:50](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_glowing`; [other/eatables/glowing.mcfunction:3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/glowing.mcfunction) — `advancement revoke @s only lbc:eat_glowing`.

## `lbc:eat_golden_beef`

Источник: [lbc:eat_golden_beef](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_golden_beef.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{golden_beef:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/golden_beef](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/golden_beef.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:51](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_golden_beef`; [other/eatables/golden_beef.mcfunction:4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/golden_beef.mcfunction) — `advancement revoke @s only lbc:eat_golden_beef`.

## `lbc:eat_golden_beetroot`

Источник: [lbc:eat_golden_beetroot](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_golden_beetroot.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{golden_beetroot:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/golden_beetroot](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/golden_beetroot.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:52](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_golden_beetroot`; [other/eatables/golden_beetroot.mcfunction:2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/golden_beetroot.mcfunction) — `advancement revoke @s only lbc:eat_golden_beetroot`.

## `lbc:eat_golden_berries`

Источник: [lbc:eat_golden_berries](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_golden_berries.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{golden_berries:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/golden_berries](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/golden_berries.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:53](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_golden_berries`; [other/eatables/golden_berries.mcfunction:3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/golden_berries.mcfunction) — `advancement revoke @s only lbc:eat_golden_berries`.

## `lbc:eat_golden_bread`

Источник: [lbc:eat_golden_bread](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_golden_bread.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{golden_bread:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/golden_bread](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/golden_bread.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:54](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_golden_bread`; [other/eatables/golden_bread.mcfunction:4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/golden_bread.mcfunction) — `advancement revoke @s only lbc:eat_golden_bread`.

## `lbc:eat_golden_cake`

Источник: [lbc:eat_golden_cake](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_golden_cake.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{golden_cake:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/golden_cake](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/golden_cake.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:55](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_golden_cake`; [other/eatables/golden_cake.mcfunction:3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/golden_cake.mcfunction) — `advancement revoke @s only lbc:eat_golden_cake`.

## `lbc:eat_golden_chicken`

Источник: [lbc:eat_golden_chicken](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_golden_chicken.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{golden_chicken:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/golden_chicken](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/golden_chicken.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:56](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_golden_chicken`; [other/eatables/golden_chicken.mcfunction:2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/golden_chicken.mcfunction) — `advancement revoke @s only lbc:eat_golden_chicken`.

## `lbc:eat_golden_chorus`

Источник: [lbc:eat_golden_chorus](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_golden_chorus.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{golden_chorus:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/golden_chorus](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/golden_chorus.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:57](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_golden_chorus`; [other/eatables/golden_chorus.mcfunction:6](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/golden_chorus.mcfunction) — `advancement revoke @s only lbc:eat_golden_chorus`.

## `lbc:eat_golden_cookie`

Источник: [lbc:eat_golden_cookie](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_golden_cookie.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{golden_cookie:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/golden_cookie](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/golden_cookie.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:58](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_golden_cookie`; [other/eatables/golden_cookie.mcfunction:3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/golden_cookie.mcfunction) — `advancement revoke @s only lbc:eat_golden_cookie`.

## `lbc:eat_golden_fish`

Источник: [lbc:eat_golden_fish](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_golden_fish.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{golden_fish:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/golden_fish](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/golden_fish.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:59](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_golden_fish`; [other/eatables/golden_fish.mcfunction:3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/golden_fish.mcfunction) — `advancement revoke @s only lbc:eat_golden_fish`.

## `lbc:eat_golden_kelp`

Источник: [lbc:eat_golden_kelp](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_golden_kelp.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{golden_kelp:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/golden_kelp](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/golden_kelp.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:60](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_golden_kelp`; [other/eatables/golden_kelp.mcfunction:4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/golden_kelp.mcfunction) — `advancement revoke @s only lbc:eat_golden_kelp`.

## `lbc:eat_golden_melon`

Источник: [lbc:eat_golden_melon](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_golden_melon.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{golden_melon:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/golden_melon](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/golden_melon.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:61](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_golden_melon`; [other/eatables/golden_melon.mcfunction:3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/golden_melon.mcfunction) — `advancement revoke @s only lbc:eat_golden_melon`.

## `lbc:eat_golden_mutton`

Источник: [lbc:eat_golden_mutton](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_golden_mutton.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{golden_mutton:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/golden_mutton](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/golden_mutton.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:62](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_golden_mutton`; [other/eatables/golden_mutton.mcfunction:4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/golden_mutton.mcfunction) — `advancement revoke @s only lbc:eat_golden_mutton`.

## `lbc:eat_golden_pie`

Источник: [lbc:eat_golden_pie](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_golden_pie.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{golden_pie:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/golden_pie](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/golden_pie.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:63](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_golden_pie`; [other/eatables/golden_pie.mcfunction:4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/golden_pie.mcfunction) — `advancement revoke @s only lbc:eat_golden_pie`.

## `lbc:eat_golden_porkchop`

Источник: [lbc:eat_golden_porkchop](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_golden_porkchop.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{golden_porkchop:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/golden_porkchop](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/golden_porkchop.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:64](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_golden_porkchop`; [other/eatables/golden_porkchop.mcfunction:4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/golden_porkchop.mcfunction) — `advancement revoke @s only lbc:eat_golden_porkchop`.

## `lbc:eat_golden_potato`

Источник: [lbc:eat_golden_potato](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_golden_potato.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{golden_potato:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/golden_potato](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/golden_potato.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:65](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_golden_potato`; [other/eatables/golden_potato.mcfunction:4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/golden_potato.mcfunction) — `advancement revoke @s only lbc:eat_golden_potato`.

## `lbc:eat_golden_rabbit`

Источник: [lbc:eat_golden_rabbit](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_golden_rabbit.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{golden_rabbit:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/golden_rabbit](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/golden_rabbit.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:66](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_golden_rabbit`; [other/eatables/golden_rabbit.mcfunction:2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/golden_rabbit.mcfunction) — `advancement revoke @s only lbc:eat_golden_rabbit`.

## `lbc:eat_golden_stew`

Источник: [lbc:eat_golden_stew](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_golden_stew.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{golden_stew:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/golden_stew](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/golden_stew.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:67](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_golden_stew`; [other/eatables/golden_stew.mcfunction:7](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/golden_stew.mcfunction) — `advancement revoke @s only lbc:eat_golden_stew`.

## `lbc:eat_halloween_food`

Источник: [lbc:eat_halloween_food](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_halloween_food.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{halloween_food:1}"}}`.
Награда: выполнить [lbc:other/event/halloween/eat_halloween_food](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [players_tick.mcfunction:70](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/players_tick.mcfunction) — `advancement revoke @s only lbc:eat_halloween_food`.

## `lbc:eat_haste`

Источник: [lbc:eat_haste](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_haste.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{eat_haste:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/haste](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/haste.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:68](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_haste`; [other/eatables/haste.mcfunction:2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/haste.mcfunction) — `advancement revoke @s only lbc:eat_haste`.

## `lbc:eat_healthy_penis`

Источник: [lbc:eat_healthy_penis](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_healthy_penis.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{random_health:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/random_health](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/random_health.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:69](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_healthy_penis`; [other/attributes/random_health.mcfunction:9](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/random_health.mcfunction) — `advancement revoke @s only lbc:eat_healthy_penis`.

## `lbc:eat_human_eye`

Источник: [lbc:eat_human_eye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_human_eye.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{human_eye:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/human_eye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/human_eye.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:70](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_human_eye`; [other/eatables/human_eye.mcfunction:7](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/human_eye.mcfunction) — `advancement revoke @s only lbc:eat_human_eye`.

## `lbc:eat_human_flesh`

Источник: [lbc:eat_human_flesh](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_human_flesh.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{human_flesh:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/human_flesh](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/human_flesh.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:71](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_human_flesh`; [other/eatables/human_flesh.mcfunction:14](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/human_flesh.mcfunction) — `advancement revoke @s only lbc:eat_human_flesh`.

## `lbc:eat_kfc_chick`

Источник: [lbc:eat_kfc_chick](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_kfc_chick.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{kfc_chick:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/kfc_chick](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/kfc_chick.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:72](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_kfc_chick`; [other/eatables/kfc_chick.mcfunction:25](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/kfc_chick.mcfunction) — `advancement revoke @s only lbc:eat_kfc_chick`.

## `lbc:eat_lava`

Источник: [lbc:eat_lava](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_lava.json).
Критерий `requirement`: игрок продолжает использовать предмет. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{lava_bucket:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/lava](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/lava.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:73](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_lava`; [other/eatables/lava.mcfunction:2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/lava.mcfunction) — `advancement revoke @s only lbc:eat_lava`.

## `lbc:eat_lightning`

Источник: [lbc:eat_lightning](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_lightning.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{eat_lightning:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/lightning](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/lightning.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:74](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_lightning`; [other/eatables/lightning.mcfunction:54](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/lightning.mcfunction) — `advancement revoke @s only lbc:eat_lightning`.

## `lbc:eat_multiverse_fish`

Источник: [lbc:eat_multiverse_fish](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_multiverse_fish.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{multiverse_fish:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/multiverse_fish](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/multiverse_fish.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:75](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_multiverse_fish`; [other/eatables/multiverse_fish.mcfunction:21](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/multiverse_fish.mcfunction) — `advancement revoke @s only lbc:eat_multiverse_fish`.

## `lbc:eat_netherite_apple`

Источник: [lbc:eat_netherite_apple](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_netherite_apple.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{netherite_apple:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/netherite_apple](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/netherite_apple.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:76](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_netherite_apple`; [other/eatables/netherite_apple.mcfunction:9](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/netherite_apple.mcfunction) — `advancement revoke @s only lbc:eat_netherite_apple`.

## `lbc:eat_netherite_carrot`

Источник: [lbc:eat_netherite_carrot](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_netherite_carrot.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{netherite_carrot:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/netherite_carrot](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/netherite_carrot.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:77](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_netherite_carrot`; [other/eatables/netherite_carrot.mcfunction:10](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/netherite_carrot.mcfunction) — `advancement revoke @s only lbc:eat_netherite_carrot`.

## `lbc:eat_netherite_fish`

Источник: [lbc:eat_netherite_fish](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_netherite_fish.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{netherite_fish:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/netherite_fish](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/netherite_fish.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:78](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_netherite_fish`; [other/eatables/netherite_fish.mcfunction:11](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/netherite_fish.mcfunction) — `advancement revoke @s only lbc:eat_netherite_fish`.

## `lbc:eat_netherite_potato`

Источник: [lbc:eat_netherite_potato](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_netherite_potato.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{netherite_potato:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/netherite_potato](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/netherite_potato.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:79](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_netherite_potato`; [other/eatables/netherite_potato.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/netherite_potato.mcfunction) — `advancement revoke @s only lbc:eat_netherite_potato`.

## `lbc:eat_netherite_stew`

Источник: [lbc:eat_netherite_stew](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_netherite_stew.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{netherite_stew:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/netherite_stew](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/netherite_stew.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:80](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_netherite_stew`; [other/eatables/netherite_stew.mcfunction:65](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/netherite_stew.mcfunction) — `advancement revoke @s only lbc:eat_netherite_stew`.

## `lbc:eat_netherstar_fish`

Источник: [lbc:eat_netherstar_fish](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_netherstar_fish.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{netherstar_fish:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/netherstar_fish](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/netherstar_fish.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:81](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_netherstar_fish`; [other/eatables/netherstar_fish.mcfunction:14](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/netherstar_fish.mcfunction) — `advancement revoke @s only lbc:eat_netherstar_fish`.

## `lbc:eat_oreshek`

Источник: [lbc:eat_oreshek](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_oreshek.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{eat_oreshek:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/oreshek](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/oreshek.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_oreshek`; [other/eatables/oreshek.mcfunction:8](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/oreshek.mcfunction) — `advancement revoke @s only lbc:eat_oreshek`.

## `lbc:eat_poison`

Источник: [lbc:eat_poison](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_poison.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{eat_poison:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/poison](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/poison.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:82](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_poison`; [other/eatables/poison.mcfunction:2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/poison.mcfunction) — `advancement revoke @s only lbc:eat_poison`.

## `lbc:eat_polymorph`

Источник: [lbc:eat_polymorph](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_polymorph.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{eat_polymorph:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/polymorph](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/polymorph.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:83](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_polymorph`; [other/eatables/polymorph.mcfunction:6](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/polymorph.mcfunction) — `advancement revoke @s only lbc:eat_polymorph`.

## `lbc:eat_psycho`

Источник: [lbc:eat_psycho](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_psycho.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{eat_psycho:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/psycho](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/psycho.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:84](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_psycho`; [other/eatables/psycho.mcfunction:2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/psycho.mcfunction) — `advancement revoke @s only lbc:eat_psycho`.

## `lbc:eat_regeneration`

Источник: [lbc:eat_regeneration](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_regeneration.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{eat_regeneration:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/regeneration](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/regeneration.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:85](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_regeneration`; [other/eatables/regeneration.mcfunction:2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/regeneration.mcfunction) — `advancement revoke @s only lbc:eat_regeneration`.

## `lbc:eat_shit`

Источник: [lbc:eat_shit](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_shit.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{eat_shit:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/shit](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/shit.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:86](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_shit`; [other/eatables/shit.mcfunction:8](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/shit.mcfunction) — `advancement revoke @s only lbc:eat_shit`.

## `lbc:eat_slab`

Источник: [lbc:eat_slab](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_slab.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{eat_slab:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/slab](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/slab.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:87](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_slab`; [other/eatables/slab.mcfunction:7](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/slab.mcfunction) — `advancement revoke @s only lbc:eat_slab`.

## `lbc:eat_snow`

Источник: [lbc:eat_snow](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_snow.json).
Критерий `requirement`: игрок продолжает использовать предмет. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{snow_bucket:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/snow](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/snow.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:88](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_snow`; [other/eatables/snow.mcfunction:2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/snow.mcfunction) — `advancement revoke @s only lbc:eat_snow`.

## `lbc:eat_strength`

Источник: [lbc:eat_strength](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_strength.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{eat_strength:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/strength](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/strength.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:89](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_strength`; [other/eatables/strength.mcfunction:2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/strength.mcfunction) — `advancement revoke @s only lbc:eat_strength`.

## `lbc:eat_teleport`

Источник: [lbc:eat_teleport](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_teleport.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{eat_teleport:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/teleport](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/teleport.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:90](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_teleport`; [other/eatables/teleport.mcfunction:3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/teleport.mcfunction) — `advancement revoke @s only lbc:eat_teleport`.

## `lbc:eat_teleport2`

Источник: [lbc:eat_teleport2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_teleport2.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{eat_teleport2:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/teleport2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/teleport2.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:91](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_teleport2`; [other/eatables/teleport2.mcfunction:3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/teleport2.mcfunction) — `advancement revoke @s only lbc:eat_teleport2`.

## `lbc:eat_transdimensional_fish`

Источник: [lbc:eat_transdimensional_fish](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_transdimensional_fish.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{transdimensional_fish:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/transdimensional_fish](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/transdimensional_fish.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:92](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_transdimensional_fish`; [other/eatables/transdimensional_fish.mcfunction:16](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/transdimensional_fish.mcfunction) — `advancement revoke @s only lbc:eat_transdimensional_fish`.

## `lbc:eat_weakness`

Источник: [lbc:eat_weakness](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_weakness.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{eat_weakness:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/weakness](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/weakness.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:93](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_weakness`; [other/eatables/weakness.mcfunction:2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/weakness.mcfunction) — `advancement revoke @s only lbc:eat_weakness`.

## `lbc:eat_wither`

Источник: [lbc:eat_wither](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_wither.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{eat_wither:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/wither](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/wither.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:94](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_wither`; [other/eatables/wither.mcfunction:2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/wither.mcfunction) — `advancement revoke @s only lbc:eat_wither`.

## `lbc:eat_yellow_lightings_bottle`

Источник: [lbc:eat_yellow_lightings_bottle](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/eat_yellow_lightings_bottle.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{yellow_lightings_bottle:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/yellow_lightings_bottle](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/yellow_lightings_bottle.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:95](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:eat_yellow_lightings_bottle`; [other/eatables/yellow_lightings_bottle.mcfunction:8](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/yellow_lightings_bottle.mcfunction) — `advancement revoke @s only lbc:eat_yellow_lightings_bottle`.

## `lbc:effects_changed`

Источник: [lbc:effects_changed](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/effects_changed.json).
Критерий `requirement`: меняются эффекты игрока. Условия: дополнительных ограничений нет.
Вне `conditions` записано `{"player":[{"condition":"minecraft:inverted","term":{"condition":"minecraft:entity_properties","entity":"this","predicate":{"location":{"dimension":"minecraft:nexus"}}}},{"condition":"minecraft:inverted","term":{"condition":"minecraft:entity_properties","entity":"this","predicate":{"location":{"dimension":"minecraft:imprinted"}}}}]}`. Это не обычные поля фильтра события; не считаем их доказанным ограничением срабатывания.
Награда: выполнить [lbc:items/effects_changed](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/effects_changed.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [items/effects_changed.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/effects_changed.mcfunction) — `advancement revoke @s only lbc:effects_changed`; [leave_death.mcfunction:96](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:effects_changed`.

## `lbc:energy_shield`

Источник: [lbc:energy_shield](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/energy_shield.json).
Критерий `requirement`: игрок продолжает использовать предмет. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{energyshield:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:items/shields/energy_shield](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/shields/energy_shield.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [items/shields/energy_shield.mcfunction:2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/shields/energy_shield.mcfunction) — `advancement revoke @s only lbc:energy_shield`; [leave_death.mcfunction:97](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:energy_shield`.

## `file_list.txt`

Источник: [файл](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/file_list.txt).
Сохранённый список имён файлов без расширений. Minecraft этот TXT не исполняет и не использует как registry; список может устареть.

## `lbc:gain_bucket`

Источник: [lbc:gain_bucket](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/gain_bucket.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{gain_bucket:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/eatables/gain_bucket](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/gain_bucket.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:98](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:gain_bucket`; [other/eatables/gain_bucket.mcfunction:3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/gain_bucket.mcfunction) — `advancement revoke @s only lbc:gain_bucket`.

## `lbc:generate_shulker`

Источник: [lbc:generate_shulker](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/generate_shulker.json).
Критерий `requirement`: игрок впервые генерирует лут контейнера. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)); таблица лута: `lbc:infinity_upgraded_luck`.
Награда: выполнить [lbc:other/give_shulker](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/give_shulker.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:99](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:generate_shulker`; [other/give_shulker.mcfunction:2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/give_shulker.mcfunction) — `advancement revoke @s only lbc:generate_shulker`; [resets.mcfunction:119](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/resets.mcfunction) — `advancement revoke @s only lbc:generate_shulker`.

## `lbc:gluttony`

Источник: [lbc:gluttony](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/gluttony.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"items":"#minecraft:all_food"}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/food_counter](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/food_counter.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:100](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:gluttony`; [other/food_counter.mcfunction:3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/food_counter.mcfunction) — `advancement revoke @s only lbc:gluttony`.

## `lbc:gowno`

Источник: [lbc:gowno](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/gowno.json).
Критерий `requirement`: игрок наносит урон сущности. Условия: урон: `{"blocked":false,"type":{"tags":[{"id":"minecraft:arrow_only","expected":false}]}}`; сущность: (НЕ (свойства `this`: `{"type":"minecraft:interaction"}`)); игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:items/swords](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [items/swords.mcfunction:69](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords.mcfunction) — `advancement revoke @s only lbc:gowno`; [leave_death.mcfunction:101](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:gowno`.

## `lbc:gowno_any_damage`

Источник: [lbc:gowno_any_damage](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/gowno_any_damage.json).
Критерий `requirement`: игрок наносит урон сущности. Условия: урон: `{"blocked":false,"type":{"tags":[{"id":"minecraft:acceptable_for_manavampirism","expected":true}]}}`; сущность: (НЕ (свойства `this`: `{"type":"minecraft:interaction"}`)); игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:items/swords_or_bow](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords_or_bow.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [items/swords_or_bow.mcfunction:4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords_or_bow.mcfunction) — `advancement revoke @s only lbc:gowno_any_damage`; [leave_death.mcfunction:135](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:gowno_any_damage`.

## `lbc:gowno_player_hit_player`

Источник: [lbc:gowno_player_hit_player](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/gowno_player_hit_player.json).
Критерий `requirement`: игрок наносит урон сущности. Условия: урон: `{"blocked":false,"type":{"tags":[{"id":"minecraft:acceptable_for_manavampirism","expected":true}]},"taken":{"min":0}}`; сущность: `{"type":"minecraft:player"}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:items/player_hit_player_with_bow_or_sword](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/player_hit_player_with_bow_or_sword.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [items/player_hit_player_with_bow_or_sword.mcfunction:3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/player_hit_player_with_bow_or_sword.mcfunction) — `advancement revoke @s only lbc:gowno_player_hit_player`; [leave_death.mcfunction:470](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:gowno_player_hit_player`.

## `lbc:gowno_player_hit_player_arrow`

Источник: [lbc:gowno_player_hit_player_arrow](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/gowno_player_hit_player_arrow.json).
Критерий `requirement`: игрок наносит урон сущности. Условия: урон: `{"blocked":false,"type":{"tags":[{"id":"minecraft:is_player_attack","expected":false}]}}`; сущность: `{"type":"minecraft:player"}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:items/player_hit_player_arrow](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/player_hit_player_arrow.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [items/player_hit_player_arrow.mcfunction:1](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/player_hit_player_arrow.mcfunction) — `advancement revoke @s only lbc:gowno_player_hit_player_arrow`.

## `lbc:gowno_player_hit_player_melee`

Источник: [lbc:gowno_player_hit_player_melee](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/gowno_player_hit_player_melee.json).
Критерий `requirement`: игрок наносит урон сущности. Условия: урон: `{"blocked":false,"type":{"tags":[{"id":"minecraft:is_player_attack","expected":true}]}}`; сущность: `{"type":"minecraft:player"}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:items/player_hit_player_melee](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/player_hit_player_melee.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [items/player_hit_player_melee.mcfunction:1](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/player_hit_player_melee.mcfunction) — `advancement revoke @s only lbc:gowno_player_hit_player_melee`.

## `lbc:halloween_chorus`

Источник: [lbc:halloween_chorus](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/halloween_chorus.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{halloween_chorus:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/halloween/eat/chorus](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/halloween/eat/chorus.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [other/halloween/eat/chorus.mcfunction:32](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/halloween/eat/chorus.mcfunction) — `advancement revoke @s only lbc:halloween_chorus`.

## `lbc:holy_shield`

Источник: [lbc:holy_shield](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/holy_shield.json).
Критерий `requirement`: игрок продолжает использовать предмет. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{holyshield:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:items/shields/holyshield](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/shields/holyshield.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [items/shields/holyshield.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/shields/holyshield.mcfunction) — `advancement revoke @s only lbc:holy_shield`; [leave_death.mcfunction:102](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:holy_shield`.

## `lbc:imba_fast_crossbow`

Источник: [lbc:imba_fast_crossbow](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/imba_fast_crossbow.json).
Критерий `requirement`: игрок продолжает использовать предмет. Условия: предмет: `{"items":["minecraft:crossbow"],"predicates":{"minecraft:custom_data":"{imba_fast_crossbow:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:items/crossbow/imba_fast_crossbow](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/crossbow/imba_fast_crossbow.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [items/crossbow/imba_fast_crossbow.mcfunction:4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/crossbow/imba_fast_crossbow.mcfunction) — `advancement revoke @s only lbc:imba_fast_crossbow`; [leave_death.mcfunction:104](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:imba_fast_crossbow`.

## `lbc:imbaxe`

Источник: [lbc:imbaxe](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/imbaxe.json).
Критерий `requirement`: меняется повреждение предмета. Условия: игрок: (свойства `this`: `{"nbt":"{SelectedItem:{components:{\"minecraft:custom_data\":{imbaxe:1}}}}"}`) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)) И (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)).
Награда: выполнить [lbc:other/imba/imbaxe_use](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/imbaxe_use.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:103](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:imbaxe`; [other/imba/imbaxe_use.mcfunction:1237](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/imbaxe_use.mcfunction) — `advancement revoke @s only lbc:imbaxe`.

## `lbc:infinite_totem`

Источник: [lbc:infinite_totem](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/infinite_totem.json).
Критерий `requirement`: игрок спасается тотемом. Условия: предмет: `{"predicates":{"minecraft:enchantments":[{"enchantments":"minecraft:infinity","levels":1}],"minecraft:custom_data":"{transformable:1,totem:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/infinite_totem](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/infinite_totem.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:105](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:infinite_totem`; [other/infinite_totem.mcfunction:2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/infinite_totem.mcfunction) — `advancement revoke @s only lbc:infinite_totem`.

## `lbc:interacted_with_academy`

Источник: [lbc:interacted_with_academy](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/interacted_with_academy.json).
Критерий `requirement`: игрок взаимодействует с сущностью. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)); предмет: `{"predicates":{"minecraft:custom_data":"{staff:1}"}}`; сущность: (свойства `this`: `{"type":"minecraft:interaction","nbt":"{Tags:[\"magic_academy\"]}"}`).
Награда: выполнить [lbc:other/magic_academy/interacted](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:106](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:interacted_with_academy`; [other/magic_academy/interacted.mcfunction:10](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted.mcfunction) — `advancement revoke @s only lbc:interacted_with_academy`; [other/magic_academy/interacted_cancel_update.mcfunction:1](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_cancel_update.mcfunction) — `advancement revoke @s only lbc:interacted_with_academy`.

## `lbc:interaction_nexus_frame_black`

Источник: [lbc:interaction_nexus_frame_black](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/interaction_nexus_frame_black.json).
Критерий `requirement`: игрок взаимодействует с сущностью. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предмет: `{"predicates":{"minecraft:custom_data":"{nexus_wire_black:1}"}}`; сущность: (свойства `this`: `{"type":"minecraft:interaction","nbt":"{Tags:[\"wire_converter_black\"]}"}`).
Награда: выполнить [lbc:other/nexus/interacted_black](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/interacted_black.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:107](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:interaction_nexus_frame_black`; [other/nexus/interacted_black.mcfunction:3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/interacted_black.mcfunction) — `advancement revoke @s only lbc:interaction_nexus_frame_black`.

## `lbc:interaction_nexus_frame_green`

Источник: [lbc:interaction_nexus_frame_green](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/interaction_nexus_frame_green.json).
Критерий `requirement`: игрок взаимодействует с сущностью. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предмет: `{"predicates":{"minecraft:custom_data":"{nexus_wire_blue:1}"}}`; сущность: (свойства `this`: `{"type":"minecraft:interaction","nbt":"{Tags:[\"wire_converter_green\"]}"}`).
Награда: выполнить [lbc:other/nexus/interacted_green](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/interacted_green.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:108](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:interaction_nexus_frame_green`; [other/nexus/interacted_green.mcfunction:3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/interacted_green.mcfunction) — `advancement revoke @s only lbc:interaction_nexus_frame_green`.

## `lbc:interaction_nexus_frame_red`

Источник: [lbc:interaction_nexus_frame_red](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/interaction_nexus_frame_red.json).
Критерий `requirement`: игрок взаимодействует с сущностью. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предмет: `{"predicates":{"minecraft:custom_data":"{nexus_wire_red:1}"}}`; сущность: (свойства `this`: `{"type":"minecraft:interaction","nbt":"{Tags:[\"wire_converter_red\"]}"}`).
Награда: выполнить [lbc:other/nexus/interacted_red](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/interacted_red.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:109](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:interaction_nexus_frame_red`; [other/nexus/interacted_red.mcfunction:3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/interacted_red.mcfunction) — `advancement revoke @s only lbc:interaction_nexus_frame_red`.

## `lbc:interaction_nexus_frame_white`

Источник: [lbc:interaction_nexus_frame_white](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/interaction_nexus_frame_white.json).
Критерий `requirement`: игрок взаимодействует с сущностью. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предмет: `{"predicates":{"minecraft:custom_data":"{nexus_wire_white:1}"}}`; сущность: (свойства `this`: `{"type":"minecraft:interaction","nbt":"{Tags:[\"wire_converter_white\"]}"}`).
Награда: выполнить [lbc:other/nexus/interacted_white](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/interacted_white.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:110](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:interaction_nexus_frame_white`; [other/nexus/interacted_white.mcfunction:3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/interacted_white.mcfunction) — `advancement revoke @s only lbc:interaction_nexus_frame_white`.

## `lbc:interaction_rightclickable`

Источник: [lbc:interaction_rightclickable](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/interaction_rightclickable.json).
Критерий `requirement`: игрок взаимодействует с сущностью. Условия: сущность: (свойства `this`: `{"nbt":"{Tags:[\"rc_able\"]}"}`); игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:items/spells](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/spells.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [items/spells.mcfunction:17](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/spells.mcfunction) — `advancement revoke @s only lbc:interaction_rightclickable`; [leave_death.mcfunction:111](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:interaction_rightclickable`.

## `lbc:jedi_killed_sith`

Источник: [lbc:jedi_killed_sith](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/jedi_killed_sith.json).
Критерий `requirement`: игрок убивает сущность. Условия: игрок: (условие `entity_scores` с параметрами `{"entity":"this","scores":{"november":1}}`); сущность: (условие `entity_scores` с параметрами `{"entity":"this","scores":{"november":2}}`).
Награда: выполнить [lbc:items/jedi_killed_sith](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/jedi_killed_sith.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [items/jedi_killed_sith.mcfunction:4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/jedi_killed_sith.mcfunction) — `advancement revoke @s only lbc:jedi_killed_sith`; [leave_death.mcfunction:112](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:jedi_killed_sith`; [resets.mcfunction:118](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/resets.mcfunction) — `advancement revoke @s only lbc:jedi_killed_sith`.

## `lbc:key_luck_used`

Источник: [lbc:key_luck_used](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/key_luck_used.json).
Критерий `requirement`: меняется повреждение предмета. Условия: игрок: (свойства `this`: `{"nbt":"{SelectedItem:{components:{\"minecraft:custom_data\":{luck_key:1}}}}"}`) И (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)).
Награда: выполнить [lbc:swrg_kit_integration/items/break_luck_key](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/items/break_luck_key.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:113](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:key_luck_used`; [swrg_kit_integration/items/break_luck_key.mcfunction:2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/items/break_luck_key.mcfunction) — `advancement revoke @s only lbc:key_luck_used`.

## `lbc:killed_by_bloodshed`

Источник: [lbc:killed_by_bloodshed](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/killed_by_bloodshed.json).
Критерий `requirement`: сущность убивает игрока. Условия: сущность: (свойства `this`: `{"type":"minecraft:player"}`) И (значение `{"type":"score","score":"swrg.kill_effect","target":"this","scale":1}` входит в `3`).
Награда: выполнить [lbc:swrg_kit_integration/kill_effects/bloodshed_player](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/kill_effects/bloodshed_player.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [swrg_kit_integration/kill_effects/bloodshed_player.mcfunction:3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/kill_effects/bloodshed_player.mcfunction) — `advancement revoke @s only lbc:killed_by_bloodshed`.

## `lbc:killed_by_exoperso`

Источник: [lbc:killed_by_exoperso](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/killed_by_exoperso.json).
Критерий `requirement`: сущность убивает игрока. Условия: сущность: (свойства `this`: `{"nbt":"{Tags:[\"exoper\"]}"}`).
Награда: выполнить [lbc:workingitems/exoperso_killed](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/exoperso_killed.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:114](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:killed_by_exoperso`; [workingitems/exoperso_killed.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/exoperso_killed.mcfunction) — `advancement revoke @s only lbc:killed_by_exoperso`.

## `lbc:killed_by_pozdman`

Источник: [lbc:killed_by_pozdman](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/killed_by_pozdman.json).
Критерий `requirement`: сущность убивает игрока. Условия: сущность: (свойства `this`: `{"type":"minecraft:player"}`) И (значение `{"type":"score","score":"lbc.skill","target":"this","scale":35}` входит в `2`).
Награда: выполнить [lbc:swrg_kit_integration/game/skills/absolute_cinema/pozdman_spawn_dog](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/absolute_cinema/pozdman_spawn_dog.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [swrg_kit_integration/game/skills/absolute_cinema/pozdman_spawn_dog.mcfunction:1](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/absolute_cinema/pozdman_spawn_dog.mcfunction) — `advancement revoke @s only lbc:killed_by_pozdman`.

## `lbc:killed_by_wagnerkilleffect`

Источник: [lbc:killed_by_wagnerkilleffect](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/killed_by_wagnerkilleffect.json).
Критерий `requirement`: сущность убивает игрока. Условия: сущность: (свойства `this`: `{"type":"minecraft:player"}`) И (значение `{"type":"score","score":"swrg.kill_effect","target":"this","scale":1}` входит в `2`).
Награда: выполнить [lbc:swrg_kit_integration/kill_effects/wagnerhammer](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/kill_effects/wagnerhammer.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [swrg_kit_integration/kill_effects/wagnerhammer.mcfunction:4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/kill_effects/wagnerhammer.mcfunction) — `advancement revoke @s only lbc:killed_by_wagnerkilleffect`.

## `lbc:knockback_resistanceattr`

Источник: [lbc:knockback_resistanceattr](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/knockback_resistanceattr.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{knockback_resistanceattribute:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/knockback_resistance](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/knockback_resistance.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:115](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:knockback_resistanceattr`; [other/attributes/knockback_resistance.mcfunction:10](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/knockback_resistance.mcfunction) — `advancement revoke @s only lbc:knockback_resistanceattr`.

## `lbc:lethal_damage`

Источник: [lbc:lethal_damage](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/lethal_damage.json).
Критерий `requirement`: сущность убивает игрока. Условия: сущность: (свойства `this`: `{"type":"minecraft:player"}`); игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)) И (свойства `this`: `{"type":"minecraft:player"}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [resets.mcfunction:221](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/resets.mcfunction) — `advancement revoke @s only lbc:lethal_damage`; [swrg_kit_integration/game/skills/killed_by_player.mcfunction:3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/killed_by_player.mcfunction) — `advancement revoke @s only lbc:lethal_damage`.

## `lbc:luckattr`

Источник: [lbc:luckattr](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/luckattr.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{luckattribute:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/luck](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/luck.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:116](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:luckattr`; [other/attributes/luck.mcfunction:10](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/luck.mcfunction) — `advancement revoke @s only lbc:luckattr`.

## `lbc:madoka_bow_hit`

Источник: [lbc:madoka_bow_hit](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/madoka_bow_hit.json).
Критерий `requirement`: игрок наносит урон сущности. Условия: игрок: нет ограничений; сущность: (свойства `this`: `{"type":"minecraft:player","nbt":"{Inventory:[{components:{\"minecraft:custom_data\":{\"upgraded\":1}}}]}"}`); урон: `{"type":{"direct_entity":{"type":"minecraft:arrow","nbt":"{Tags:[madoka_arrow]}"}}}`.
Награда: выполнить [lbc:items/bows/kaname_bow/hit_adv](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/bows/kaname_bow/hit_adv.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [items/bows/kaname_bow/hit_adv.mcfunction:4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/bows/kaname_bow/hit_adv.mcfunction) — `advancement revoke @s only lbc:madoka_bow_hit`; [leave_death.mcfunction:285](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:madoka_bow_hit`.

## `lbc:max_healthattr`

Источник: [lbc:max_healthattr](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/max_healthattr.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{max_healthattribute:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/max_health](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/max_health.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:117](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:max_healthattr`; [other/attributes/max_health.mcfunction:10](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/max_health.mcfunction) — `advancement revoke @s only lbc:max_healthattr`.

## `lbc:movement_speedattr`

Источник: [lbc:movement_speedattr](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/movement_speedattr.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{movement_speedattribute:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/movement_speed](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/movement_speed.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:118](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:movement_speedattr`; [other/attributes/movement_speed.mcfunction:10](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/movement_speed.mcfunction) — `advancement revoke @s only lbc:movement_speedattr`.

## `namecollect.py`

Источник: [файл](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/namecollect.py).
Вспомогательный Python-скрипт: при ручном запуске перечисляет обычные файлы текущего рабочего каталога, убирает расширения и перезаписывает `file_list.txt`. Подпапки не обходит; сам Minecraft его не запускает.

## `lbc:old_stew_used`

Источник: [lbc:old_stew_used](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/old_stew_used.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{inf_old:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/infinitestewused_old](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/infinitestewused_old.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:119](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:old_stew_used`; [other/infinitestewused_old.mcfunction:3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/infinitestewused_old.mcfunction) — `advancement revoke @s only lbc:old_stew_used`.

## `lbc:onepunch_get`

Источник: [lbc:onepunch_get](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/onepunch_get.json).
Критерий `requirement`: проверка происходит каждый игровой тик. Условия: игрок: (свойства `this`: `{"equipment":{"mainhand":{"predicates":{"minecraft:custom_data":"{one_punch:1}"}}}}`) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:workingitems/one_punch_get_damage](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/one_punch_get_damage.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:120](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:onepunch_get`; [workingitems/one_punch_get_damage.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/one_punch_get_damage.mcfunction) — `advancement revoke @s only lbc:onepunch_get`.

## `lbc:penis`

Источник: [lbc:penis](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/penis.json).
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:items/changed_inventory/sel](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/changed_inventory/sel.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:121](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:penis`; [players_tick.mcfunction:69](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/players_tick.mcfunction) — `advancement revoke @s only lbc:penis`.

## `pills/file_list.txt`

Источник: [файл](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/file_list.txt).
Сохранённый список имён файлов без расширений. Minecraft этот TXT не исполняет и не использует как registry; список может устареть.

## `pills/namecollect.py`

Источник: [файл](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/namecollect.py).
Вспомогательный Python-скрипт: при ручном запуске перечисляет обычные файлы текущего рабочего каталога, убирает расширения и перезаписывает `file_list.txt`. Подпапки не обходит; сам Minecraft его не запускает.

## `lbc:pills/pill_absorption`

Источник: [lbc:pills/pill_absorption](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_absorption.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:54}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/absorption](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/absorption.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:138](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_absorption`; [other/attributes/pills/absorption.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/absorption.mcfunction) — `advancement revoke @s only lbc:pills/pill_absorption`.

## `lbc:pills/pill_absorption_99`

Источник: [lbc:pills/pill_absorption_99](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_absorption_99.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:55}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/absorption_99](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/absorption_99.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:139](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_absorption_99`; [other/attributes/pills/absorption_99.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/absorption_99.mcfunction) — `advancement revoke @s only lbc:pills/pill_absorption_99`.

## `lbc:pills/pill_armor_down`

Источник: [lbc:pills/pill_armor_down](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_armor_down.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:2}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/armor_down](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/armor_down.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:140](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_armor_down`; [other/attributes/pills/armor_down.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/armor_down.mcfunction) — `advancement revoke @s only lbc:pills/pill_armor_down`.

## `lbc:pills/pill_armor_toughness_down`

Источник: [lbc:pills/pill_armor_toughness_down](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_armor_toughness_down.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:3}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/armor_toughness_down](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/armor_toughness_down.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:141](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_armor_toughness_down`; [other/attributes/pills/armor_toughness_down.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/armor_toughness_down.mcfunction) — `advancement revoke @s only lbc:pills/pill_armor_toughness_down`.

## `lbc:pills/pill_armor_toughness_up`

Источник: [lbc:pills/pill_armor_toughness_up](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_armor_toughness_up.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:30}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/armor_toughness_up](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/armor_toughness_up.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:142](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_armor_toughness_up`; [other/attributes/pills/armor_toughness_up.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/armor_toughness_up.mcfunction) — `advancement revoke @s only lbc:pills/pill_armor_toughness_up`.

## `lbc:pills/pill_armor_up`

Источник: [lbc:pills/pill_armor_up](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_armor_up.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:29}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/armor_up](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/armor_up.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:143](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_armor_up`; [other/attributes/pills/armor_up.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/armor_up.mcfunction) — `advancement revoke @s only lbc:pills/pill_armor_up`.

## `lbc:pills/pill_attack_damage_down`

Источник: [lbc:pills/pill_attack_damage_down](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_attack_damage_down.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/attack_damage_down](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/attack_damage_down.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:144](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_attack_damage_down`; [other/attributes/pills/attack_damage_down.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/attack_damage_down.mcfunction) — `advancement revoke @s only lbc:pills/pill_attack_damage_down`.

## `lbc:pills/pill_attack_damage_up`

Источник: [lbc:pills/pill_attack_damage_up](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_attack_damage_up.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:28}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/attack_damage_up](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/attack_damage_up.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:145](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_attack_damage_up`; [other/attributes/pills/attack_damage_up.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/attack_damage_up.mcfunction) — `advancement revoke @s only lbc:pills/pill_attack_damage_up`.

## `lbc:pills/pill_attack_knockback_down`

Источник: [lbc:pills/pill_attack_knockback_down](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_attack_knockback_down.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill: 8}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/attack_knockback_down](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/attack_knockback_down.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:146](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_attack_knockback_down`; [other/attributes/pills/attack_knockback_down.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/attack_knockback_down.mcfunction) — `advancement revoke @s only lbc:pills/pill_attack_knockback_down`.

## `lbc:pills/pill_attack_knockback_up`

Источник: [lbc:pills/pill_attack_knockback_up](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_attack_knockback_up.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:35}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/attack_knockback_up](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/attack_knockback_up.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:147](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_attack_knockback_up`; [other/attributes/pills/attack_knockback_up.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/attack_knockback_up.mcfunction) — `advancement revoke @s only lbc:pills/pill_attack_knockback_up`.

## `lbc:pills/pill_attack_speed_down`

Источник: [lbc:pills/pill_attack_speed_down](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_attack_speed_down.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:4}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/attack_speed_down](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/attack_speed_down.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:148](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_attack_speed_down`; [other/attributes/pills/attack_speed_down.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/attack_speed_down.mcfunction) — `advancement revoke @s only lbc:pills/pill_attack_speed_down`.

## `lbc:pills/pill_attack_speed_up`

Источник: [lbc:pills/pill_attack_speed_up](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_attack_speed_up.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:31}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/attack_speed_up](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/attack_speed_up.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:149](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_attack_speed_up`; [other/attributes/pills/attack_speed_up.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/attack_speed_up.mcfunction) — `advancement revoke @s only lbc:pills/pill_attack_speed_up`.

## `lbc:pills/pill_blindness`

Источник: [lbc:pills/pill_blindness](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_blindness.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:56}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/blindness](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/blindness.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:150](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_blindness`; [other/attributes/pills/blindness.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/blindness.mcfunction) — `advancement revoke @s only lbc:pills/pill_blindness`.

## `lbc:pills/pill_block_break_speed_down`

Источник: [lbc:pills/pill_block_break_speed_down](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_block_break_speed_down.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:20}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/block_break_speed_down](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/block_break_speed_down.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:151](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_block_break_speed_down`; [other/attributes/pills/block_break_speed_down.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/block_break_speed_down.mcfunction) — `advancement revoke @s only lbc:pills/pill_block_break_speed_down`.

## `lbc:pills/pill_block_break_speed_up`

Источник: [lbc:pills/pill_block_break_speed_up](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_block_break_speed_up.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:47}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/block_break_speed_up](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/block_break_speed_up.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:152](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_block_break_speed_up`; [other/attributes/pills/block_break_speed_up.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/block_break_speed_up.mcfunction) — `advancement revoke @s only lbc:pills/pill_block_break_speed_up`.

## `lbc:pills/pill_block_interaction_range_down`

Источник: [lbc:pills/pill_block_interaction_range_down](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_block_interaction_range_down.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:21}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/block_interaction_range_down](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/block_interaction_range_down.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:153](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_block_interaction_range_down`; [other/attributes/pills/block_interaction_range_down.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/block_interaction_range_down.mcfunction) — `advancement revoke @s only lbc:pills/pill_block_interaction_range_down`.

## `lbc:pills/pill_block_interaction_range_up`

Источник: [lbc:pills/pill_block_interaction_range_up](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_block_interaction_range_up.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:48}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/block_interaction_range_up](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/block_interaction_range_up.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:154](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_block_interaction_range_up`; [other/attributes/pills/block_interaction_range_up.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/block_interaction_range_up.mcfunction) — `advancement revoke @s only lbc:pills/pill_block_interaction_range_up`.

## `lbc:pills/pill_burning_time_down`

Источник: [lbc:pills/pill_burning_time_down](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_burning_time_down.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:9}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/burning_time_down](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/burning_time_down.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:155](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_burning_time_down`; [other/attributes/pills/burning_time_down.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/burning_time_down.mcfunction) — `advancement revoke @s only lbc:pills/pill_burning_time_down`.

## `lbc:pills/pill_burning_time_up`

Источник: [lbc:pills/pill_burning_time_up](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_burning_time_up.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:36}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/burning_time_up](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/burning_time_up.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:156](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_burning_time_up`; [other/attributes/pills/burning_time_up.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/burning_time_up.mcfunction) — `advancement revoke @s only lbc:pills/pill_burning_time_up`.

## `lbc:pills/pill_conduit_power_1`

Источник: [lbc:pills/pill_conduit_power_1](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_conduit_power_1.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:57}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/conduit_power_1](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/conduit_power_1.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:157](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_conduit_power_1`; [other/attributes/pills/conduit_power_1.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/conduit_power_1.mcfunction) — `advancement revoke @s only lbc:pills/pill_conduit_power_1`.

## `lbc:pills/pill_conduit_power_2`

Источник: [lbc:pills/pill_conduit_power_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_conduit_power_2.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:58}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/conduit_power_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/conduit_power_2.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:158](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_conduit_power_2`; [other/attributes/pills/conduit_power_2.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/conduit_power_2.mcfunction) — `advancement revoke @s only lbc:pills/pill_conduit_power_2`.

## `lbc:pills/pill_darkness`

Источник: [lbc:pills/pill_darkness](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_darkness.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:59}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/darkness](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/darkness.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:159](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_darkness`; [other/attributes/pills/darkness.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/darkness.mcfunction) — `advancement revoke @s only lbc:pills/pill_darkness`.

## `lbc:pills/pill_dolphins_grace`

Источник: [lbc:pills/pill_dolphins_grace](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_dolphins_grace.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:60}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/dolphins_grace](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/dolphins_grace.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:160](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_dolphins_grace`; [other/attributes/pills/dolphins_grace.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/dolphins_grace.mcfunction) — `advancement revoke @s only lbc:pills/pill_dolphins_grace`.

## `lbc:pills/pill_drowsy`

Источник: [lbc:pills/pill_drowsy](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_drowsy.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:143}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/drowsy](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/drowsy.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:161](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_drowsy`; [other/attributes/pills/drowsy.mcfunction:8](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/drowsy.mcfunction) — `advancement revoke @s only lbc:pills/pill_drowsy`.

## `lbc:pills/pill_entity_interaction_range_down`

Источник: [lbc:pills/pill_entity_interaction_range_down](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_entity_interaction_range_down.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:22}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/entity_interaction_range_down](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/entity_interaction_range_down.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:162](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_entity_interaction_range_down`; [other/attributes/pills/entity_interaction_range_down.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/entity_interaction_range_down.mcfunction) — `advancement revoke @s only lbc:pills/pill_entity_interaction_range_down`.

## `lbc:pills/pill_entity_interaction_range_up`

Источник: [lbc:pills/pill_entity_interaction_range_up](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_entity_interaction_range_up.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:49}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/entity_interaction_range_up](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/entity_interaction_range_up.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:163](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_entity_interaction_range_up`; [other/attributes/pills/entity_interaction_range_up.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/entity_interaction_range_up.mcfunction) — `advancement revoke @s only lbc:pills/pill_entity_interaction_range_up`.

## `lbc:pills/pill_excited`

Источник: [lbc:pills/pill_excited](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_excited.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:142}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/excited](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/excited.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:164](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_excited`; [other/attributes/pills/excited.mcfunction:7](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/excited.mcfunction) — `advancement revoke @s only lbc:pills/pill_excited`.

## `lbc:pills/pill_explosion_knockback_resistance_down`

Источник: [lbc:pills/pill_explosion_knockback_resistance_down](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_explosion_knockback_resistance_down.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:10}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/explosion_knockback_resistance_down](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/explosion_knockback_resistance_down.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:165](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_explosion_knockback_resistance_down`; [other/attributes/pills/explosion_knockback_resistance_down.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/explosion_knockback_resistance_down.mcfunction) — `advancement revoke @s only lbc:pills/pill_explosion_knockback_resistance_down`.

## `lbc:pills/pill_explosion_knockback_resistance_up`

Источник: [lbc:pills/pill_explosion_knockback_resistance_up](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_explosion_knockback_resistance_up.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:37}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/explosion_knockback_resistance_up](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/explosion_knockback_resistance_up.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:166](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_explosion_knockback_resistance_up`; [other/attributes/pills/explosion_knockback_resistance_up.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/explosion_knockback_resistance_up.mcfunction) — `advancement revoke @s only lbc:pills/pill_explosion_knockback_resistance_up`.

## `lbc:pills/pill_fall_damage_multiplier_down`

Источник: [lbc:pills/pill_fall_damage_multiplier_down](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_fall_damage_multiplier_down.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:11}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/fall_damage_multiplier_down](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/fall_damage_multiplier_down.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:167](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_fall_damage_multiplier_down`; [other/attributes/pills/fall_damage_multiplier_down.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/fall_damage_multiplier_down.mcfunction) — `advancement revoke @s only lbc:pills/pill_fall_damage_multiplier_down`.

## `lbc:pills/pill_fall_damage_multiplier_up`

Источник: [lbc:pills/pill_fall_damage_multiplier_up](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_fall_damage_multiplier_up.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:38}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/fall_damage_multiplier_up](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/fall_damage_multiplier_up.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:168](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_fall_damage_multiplier_up`; [other/attributes/pills/fall_damage_multiplier_up.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/fall_damage_multiplier_up.mcfunction) — `advancement revoke @s only lbc:pills/pill_fall_damage_multiplier_up`.

## `lbc:pills/pill_fire_resistance`

Источник: [lbc:pills/pill_fire_resistance](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_fire_resistance.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:61}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/fire_resistance](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/fire_resistance.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:169](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_fire_resistance`; [other/attributes/pills/fire_resistance.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/fire_resistance.mcfunction) — `advancement revoke @s only lbc:pills/pill_fire_resistance`.

## `lbc:pills/pill_fire_resistance_1`

Источник: [lbc:pills/pill_fire_resistance_1](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_fire_resistance_1.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:62}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/fire_resistance_1](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/fire_resistance_1.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:170](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_fire_resistance_1`; [other/attributes/pills/fire_resistance_1.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/fire_resistance_1.mcfunction) — `advancement revoke @s only lbc:pills/pill_fire_resistance_1`.

## `lbc:pills/pill_fire_resistance_2`

Источник: [lbc:pills/pill_fire_resistance_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_fire_resistance_2.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:63}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/fire_resistance_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/fire_resistance_2.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:171](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_fire_resistance_2`; [other/attributes/pills/fire_resistance_2.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/fire_resistance_2.mcfunction) — `advancement revoke @s only lbc:pills/pill_fire_resistance_2`.

## `lbc:pills/pill_glowing`

Источник: [lbc:pills/pill_glowing](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_glowing.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:64}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/glowing](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/glowing.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:172](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_glowing`; [other/attributes/pills/glowing.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/glowing.mcfunction) — `advancement revoke @s only lbc:pills/pill_glowing`.

## `lbc:pills/pill_gravity_down`

Источник: [lbc:pills/pill_gravity_down](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_gravity_down.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:12}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/gravity_down](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/gravity_down.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:173](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_gravity_down`; [other/attributes/pills/gravity_down.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/gravity_down.mcfunction) — `advancement revoke @s only lbc:pills/pill_gravity_down`.

## `lbc:pills/pill_gravity_up`

Источник: [lbc:pills/pill_gravity_up](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_gravity_up.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:39}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/gravity_up](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/gravity_up.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:174](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_gravity_up`; [other/attributes/pills/gravity_up.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/gravity_up.mcfunction) — `advancement revoke @s only lbc:pills/pill_gravity_up`.

## `lbc:pills/pill_haste`

Источник: [lbc:pills/pill_haste](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_haste.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:65}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/haste](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/haste.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:175](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_haste`; [other/attributes/pills/haste.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/haste.mcfunction) — `advancement revoke @s only lbc:pills/pill_haste`.

## `lbc:pills/pill_haste_99`

Источник: [lbc:pills/pill_haste_99](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_haste_99.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:66}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/haste_99](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/haste_99.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:176](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_haste_99`; [other/attributes/pills/haste_99.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/haste_99.mcfunction) — `advancement revoke @s only lbc:pills/pill_haste_99`.

## `lbc:pills/pill_health_boost`

Источник: [lbc:pills/pill_health_boost](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_health_boost.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:67}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/health_boost](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/health_boost.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:177](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_health_boost`; [other/attributes/pills/health_boost.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/health_boost.mcfunction) — `advancement revoke @s only lbc:pills/pill_health_boost`.

## `lbc:pills/pill_hero_of_the_village`

Источник: [lbc:pills/pill_hero_of_the_village](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_hero_of_the_village.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:68}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/hero_of_the_village](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/hero_of_the_village.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:178](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_hero_of_the_village`; [other/attributes/pills/hero_of_the_village.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/hero_of_the_village.mcfunction) — `advancement revoke @s only lbc:pills/pill_hero_of_the_village`.

## `lbc:pills/pill_hunger`

Источник: [lbc:pills/pill_hunger](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_hunger.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:69}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/hunger](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/hunger.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:179](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_hunger`; [other/attributes/pills/hunger.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/hunger.mcfunction) — `advancement revoke @s only lbc:pills/pill_hunger`.

## `lbc:pills/pill_hunger_99`

Источник: [lbc:pills/pill_hunger_99](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_hunger_99.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:70}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/hunger_99](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/hunger_99.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:180](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_hunger_99`; [other/attributes/pills/hunger_99.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/hunger_99.mcfunction) — `advancement revoke @s only lbc:pills/pill_hunger_99`.

## `lbc:pills/pill_i_found_pills`

Источник: [lbc:pills/pill_i_found_pills](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_i_found_pills.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:144}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/i_found_pills](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/i_found_pills.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:187](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_i_found_pills`; [other/attributes/pills/i_found_pills.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/i_found_pills.mcfunction) — `advancement revoke @s only lbc:pills/pill_i_found_pills`.

## `lbc:pills/pill_infested`

Источник: [lbc:pills/pill_infested](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_infested.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:71}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/infested](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/infested.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:181](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_infested`; [other/attributes/pills/infested.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/infested.mcfunction) — `advancement revoke @s only lbc:pills/pill_infested`.

## `lbc:pills/pill_instant_damage`

Источник: [lbc:pills/pill_instant_damage](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_instant_damage.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:72}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/instant_damage](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/instant_damage.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:182](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_instant_damage`; [other/attributes/pills/instant_damage.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/instant_damage.mcfunction) — `advancement revoke @s only lbc:pills/pill_instant_damage`.

## `lbc:pills/pill_instant_damage_2`

Источник: [lbc:pills/pill_instant_damage_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_instant_damage_2.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:73}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/instant_damage_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/instant_damage_2.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:183](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_instant_damage_2`; [other/attributes/pills/instant_damage_2.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/instant_damage_2.mcfunction) — `advancement revoke @s only lbc:pills/pill_instant_damage_2`.

## `lbc:pills/pill_instant_health`

Источник: [lbc:pills/pill_instant_health](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_instant_health.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:74}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/instant_health](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/instant_health.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:184](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_instant_health`; [other/attributes/pills/instant_health.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/instant_health.mcfunction) — `advancement revoke @s only lbc:pills/pill_instant_health`.

## `lbc:pills/pill_invisibility`

Источник: [lbc:pills/pill_invisibility](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_invisibility.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:75}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/invisibility](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/invisibility.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:185](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_invisibility`; [other/attributes/pills/invisibility.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/invisibility.mcfunction) — `advancement revoke @s only lbc:pills/pill_invisibility`.

## `lbc:pills/pill_invisibility_1`

Источник: [lbc:pills/pill_invisibility_1](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_invisibility_1.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:76}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/invisibility_1](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/invisibility_1.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:186](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_invisibility_1`; [other/attributes/pills/invisibility_1.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/invisibility_1.mcfunction) — `advancement revoke @s only lbc:pills/pill_invisibility_1`.

## `lbc:pills/pill_jump_boost`

Источник: [lbc:pills/pill_jump_boost](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_jump_boost.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:77}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/jump_boost](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/jump_boost.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:188](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_jump_boost`; [other/attributes/pills/jump_boost.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/jump_boost.mcfunction) — `advancement revoke @s only lbc:pills/pill_jump_boost`.

## `lbc:pills/pill_jump_boost_99`

Источник: [lbc:pills/pill_jump_boost_99](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_jump_boost_99.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:78}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/jump_boost_99](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/jump_boost_99.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:189](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_jump_boost_99`; [other/attributes/pills/jump_boost_99.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/jump_boost_99.mcfunction) — `advancement revoke @s only lbc:pills/pill_jump_boost_99`.

## `lbc:pills/pill_jump_strength_down`

Источник: [lbc:pills/pill_jump_strength_down](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_jump_strength_down.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:13}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/jump_strength_down](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/jump_strength_down.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:190](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_jump_strength_down`; [other/attributes/pills/jump_strength_down.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/jump_strength_down.mcfunction) — `advancement revoke @s only lbc:pills/pill_jump_strength_down`.

## `lbc:pills/pill_jump_strength_up`

Источник: [lbc:pills/pill_jump_strength_up](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_jump_strength_up.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:40}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/jump_strength_up](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/jump_strength_up.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:191](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_jump_strength_up`; [other/attributes/pills/jump_strength_up.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/jump_strength_up.mcfunction) — `advancement revoke @s only lbc:pills/pill_jump_strength_up`.

## `lbc:pills/pill_knockback_resistance_down`

Источник: [lbc:pills/pill_knockback_resistance_down](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_knockback_resistance_down.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:5}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/knockback_resistance_down](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/knockback_resistance_down.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:192](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_knockback_resistance_down`; [other/attributes/pills/knockback_resistance_down.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/knockback_resistance_down.mcfunction) — `advancement revoke @s only lbc:pills/pill_knockback_resistance_down`.

## `lbc:pills/pill_knockback_resistance_up`

Источник: [lbc:pills/pill_knockback_resistance_up](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_knockback_resistance_up.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:32}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/knockback_resistance_up](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/knockback_resistance_up.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:193](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_knockback_resistance_up`; [other/attributes/pills/knockback_resistance_up.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/knockback_resistance_up.mcfunction) — `advancement revoke @s only lbc:pills/pill_knockback_resistance_up`.

## `lbc:pills/pill_levitation`

Источник: [lbc:pills/pill_levitation](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_levitation.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:79}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/levitation](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/levitation.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:194](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_levitation`; [other/attributes/pills/levitation.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/levitation.mcfunction) — `advancement revoke @s only lbc:pills/pill_levitation`.

## `lbc:pills/pill_levitation_99`

Источник: [lbc:pills/pill_levitation_99](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_levitation_99.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:80}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/levitation_99](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/levitation_99.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:195](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_levitation_99`; [other/attributes/pills/levitation_99.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/levitation_99.mcfunction) — `advancement revoke @s only lbc:pills/pill_levitation_99`.

## `lbc:pills/pill_luck`

Источник: [lbc:pills/pill_luck](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_luck.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:81}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/luck](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/luck.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:196](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_luck`; [other/attributes/pills/luck.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/luck.mcfunction) — `advancement revoke @s only lbc:pills/pill_luck`.

## `lbc:pills/pill_luck_99`

Источник: [lbc:pills/pill_luck_99](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_luck_99.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:82}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/luck_99](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/luck_99.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:197](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_luck_99`; [other/attributes/pills/luck_99.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/luck_99.mcfunction) — `advancement revoke @s only lbc:pills/pill_luck_99`.

## `lbc:pills/pill_luck_down`

Источник: [lbc:pills/pill_luck_down](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_luck_down.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:6}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/luck_down](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/luck_down.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:198](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_luck_down`; [other/attributes/pills/luck_down.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/luck_down.mcfunction) — `advancement revoke @s only lbc:pills/pill_luck_down`.

## `lbc:pills/pill_luck_up`

Источник: [lbc:pills/pill_luck_up](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_luck_up.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:33}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/luck_up](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/luck_up.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:199](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_luck_up`; [other/attributes/pills/luck_up.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/luck_up.mcfunction) — `advancement revoke @s only lbc:pills/pill_luck_up`.

## `lbc:pills/pill_max_health_down`

Источник: [lbc:pills/pill_max_health_down](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_max_health_down.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:0}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/max_health_down](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/max_health_down.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:200](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_max_health_down`; [other/attributes/pills/max_health_down.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/max_health_down.mcfunction) — `advancement revoke @s only lbc:pills/pill_max_health_down`.

## `lbc:pills/pill_max_health_up`

Источник: [lbc:pills/pill_max_health_up](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_max_health_up.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:27}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/max_health_up](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/max_health_up.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:201](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_max_health_up`; [other/attributes/pills/max_health_up.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/max_health_up.mcfunction) — `advancement revoke @s only lbc:pills/pill_max_health_up`.

## `lbc:pills/pill_mining_efficiency_down`

Источник: [lbc:pills/pill_mining_efficiency_down](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_mining_efficiency_down.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:23}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/mining_efficiency_down](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/mining_efficiency_down.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:202](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_mining_efficiency_down`; [other/attributes/pills/mining_efficiency_down.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/mining_efficiency_down.mcfunction) — `advancement revoke @s only lbc:pills/pill_mining_efficiency_down`.

## `lbc:pills/pill_mining_efficiency_up`

Источник: [lbc:pills/pill_mining_efficiency_up](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_mining_efficiency_up.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:50}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/mining_efficiency_up](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/mining_efficiency_up.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:203](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_mining_efficiency_up`; [other/attributes/pills/mining_efficiency_up.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/mining_efficiency_up.mcfunction) — `advancement revoke @s only lbc:pills/pill_mining_efficiency_up`.

## `lbc:pills/pill_mining_fatigue`

Источник: [lbc:pills/pill_mining_fatigue](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_mining_fatigue.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:83}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/mining_fatigue](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/mining_fatigue.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:204](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_mining_fatigue`; [other/attributes/pills/mining_fatigue.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/mining_fatigue.mcfunction) — `advancement revoke @s only lbc:pills/pill_mining_fatigue`.

## `lbc:pills/pill_mining_fatigue_99`

Источник: [lbc:pills/pill_mining_fatigue_99](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_mining_fatigue_99.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:84}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/mining_fatigue_99](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/mining_fatigue_99.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:205](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_mining_fatigue_99`; [other/attributes/pills/mining_fatigue_99.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/mining_fatigue_99.mcfunction) — `advancement revoke @s only lbc:pills/pill_mining_fatigue_99`.

## `lbc:pills/pill_movement_efficiency_down`

Источник: [lbc:pills/pill_movement_efficiency_down](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_movement_efficiency_down.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:14}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/movement_efficiency_down](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/movement_efficiency_down.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:206](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_movement_efficiency_down`; [other/attributes/pills/movement_efficiency_down.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/movement_efficiency_down.mcfunction) — `advancement revoke @s only lbc:pills/pill_movement_efficiency_down`.

## `lbc:pills/pill_movement_efficiency_up`

Источник: [lbc:pills/pill_movement_efficiency_up](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_movement_efficiency_up.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:41}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/movement_efficiency_up](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/movement_efficiency_up.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:207](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_movement_efficiency_up`; [other/attributes/pills/movement_efficiency_up.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/movement_efficiency_up.mcfunction) — `advancement revoke @s only lbc:pills/pill_movement_efficiency_up`.

## `lbc:pills/pill_movement_speed_down`

Источник: [lbc:pills/pill_movement_speed_down](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_movement_speed_down.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:7}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/movement_speed_down](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/movement_speed_down.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:208](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_movement_speed_down`; [other/attributes/pills/movement_speed_down.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/movement_speed_down.mcfunction) — `advancement revoke @s only lbc:pills/pill_movement_speed_down`.

## `lbc:pills/pill_movement_speed_up`

Источник: [lbc:pills/pill_movement_speed_up](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_movement_speed_up.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:34}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/movement_speed_up](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/movement_speed_up.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:209](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_movement_speed_up`; [other/attributes/pills/movement_speed_up.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/movement_speed_up.mcfunction) — `advancement revoke @s only lbc:pills/pill_movement_speed_up`.

## `lbc:pills/pill_nausea`

Источник: [lbc:pills/pill_nausea](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_nausea.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:85}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/nausea](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/nausea.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:210](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_nausea`; [other/attributes/pills/nausea.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/nausea.mcfunction) — `advancement revoke @s only lbc:pills/pill_nausea`.

## `lbc:pills/pill_night_vision`

Источник: [lbc:pills/pill_night_vision](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_night_vision.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:86}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/night_vision](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/night_vision.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:211](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_night_vision`; [other/attributes/pills/night_vision.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/night_vision.mcfunction) — `advancement revoke @s only lbc:pills/pill_night_vision`.

## `lbc:pills/pill_oozing`

Источник: [lbc:pills/pill_oozing](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_oozing.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:87}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/oozing](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/oozing.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:212](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_oozing`; [other/attributes/pills/oozing.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/oozing.mcfunction) — `advancement revoke @s only lbc:pills/pill_oozing`.

## `lbc:pills/pill_oxygen_bonus_down`

Источник: [lbc:pills/pill_oxygen_bonus_down](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_oxygen_bonus_down.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:15}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/oxygen_bonus_down](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/oxygen_bonus_down.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:213](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_oxygen_bonus_down`; [other/attributes/pills/oxygen_bonus_down.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/oxygen_bonus_down.mcfunction) — `advancement revoke @s only lbc:pills/pill_oxygen_bonus_down`.

## `lbc:pills/pill_oxygen_bonus_up`

Источник: [lbc:pills/pill_oxygen_bonus_up](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_oxygen_bonus_up.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:42}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/oxygen_bonus_up](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/oxygen_bonus_up.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:214](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_oxygen_bonus_up`; [other/attributes/pills/oxygen_bonus_up.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/oxygen_bonus_up.mcfunction) — `advancement revoke @s only lbc:pills/pill_oxygen_bonus_up`.

## `lbc:pills/pill_paralysis`

Источник: [lbc:pills/pill_paralysis](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_paralysis.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:141}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/paralysis](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/paralysis.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:215](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_paralysis`; [other/attributes/pills/paralysis.mcfunction:6](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/paralysis.mcfunction) — `advancement revoke @s only lbc:pills/pill_paralysis`.

## `lbc:pills/pill_poison`

Источник: [lbc:pills/pill_poison](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_poison.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:88}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/poison](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/poison.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:216](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_poison`; [other/attributes/pills/poison.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/poison.mcfunction) — `advancement revoke @s only lbc:pills/pill_poison`.

## `lbc:pills/pill_raid_omen_101`

Источник: [lbc:pills/pill_raid_omen_101](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_raid_omen_101.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:98}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/raid_omen_101](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/raid_omen_101.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:217](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_raid_omen_101`; [other/attributes/pills/raid_omen_101.mcfunction:6](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/raid_omen_101.mcfunction) — `advancement revoke @s only lbc:pills/pill_raid_omen_101`.

## `lbc:pills/pill_raid_omen_102`

Источник: [lbc:pills/pill_raid_omen_102](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_raid_omen_102.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:99}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/raid_omen_102](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/raid_omen_102.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:218](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_raid_omen_102`; [other/attributes/pills/raid_omen_102.mcfunction:6](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/raid_omen_102.mcfunction) — `advancement revoke @s only lbc:pills/pill_raid_omen_102`.

## `lbc:pills/pill_raid_omen_103`

Источник: [lbc:pills/pill_raid_omen_103](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_raid_omen_103.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:100}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/raid_omen_103](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/raid_omen_103.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:219](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_raid_omen_103`; [other/attributes/pills/raid_omen_103.mcfunction:8](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/raid_omen_103.mcfunction) — `advancement revoke @s only lbc:pills/pill_raid_omen_103`.

## `lbc:pills/pill_raid_omen_104`

Источник: [lbc:pills/pill_raid_omen_104](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_raid_omen_104.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:101}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/raid_omen_104](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/raid_omen_104.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:220](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_raid_omen_104`; [other/attributes/pills/raid_omen_104.mcfunction:6](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/raid_omen_104.mcfunction) — `advancement revoke @s only lbc:pills/pill_raid_omen_104`.

## `lbc:pills/pill_raid_omen_105`

Источник: [lbc:pills/pill_raid_omen_105](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_raid_omen_105.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:102}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/raid_omen_105](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/raid_omen_105.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:221](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_raid_omen_105`; [other/attributes/pills/raid_omen_105.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/raid_omen_105.mcfunction) — `advancement revoke @s only lbc:pills/pill_raid_omen_105`.

## `lbc:pills/pill_raid_omen_106`

Источник: [lbc:pills/pill_raid_omen_106](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_raid_omen_106.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:103}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/raid_omen_106](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/raid_omen_106.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:222](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_raid_omen_106`; [other/attributes/pills/raid_omen_106.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/raid_omen_106.mcfunction) — `advancement revoke @s only lbc:pills/pill_raid_omen_106`.

## `lbc:pills/pill_raid_omen_107`

Источник: [lbc:pills/pill_raid_omen_107](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_raid_omen_107.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:104}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/raid_omen_107](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/raid_omen_107.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:223](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_raid_omen_107`; [other/attributes/pills/raid_omen_107.mcfunction:6](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/raid_omen_107.mcfunction) — `advancement revoke @s only lbc:pills/pill_raid_omen_107`.

## `lbc:pills/pill_raid_omen_108`

Источник: [lbc:pills/pill_raid_omen_108](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_raid_omen_108.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:105}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/raid_omen_108](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/raid_omen_108.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:224](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_raid_omen_108`; [other/attributes/pills/raid_omen_108.mcfunction:6](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/raid_omen_108.mcfunction) — `advancement revoke @s only lbc:pills/pill_raid_omen_108`.

## `lbc:pills/pill_raid_omen_109`

Источник: [lbc:pills/pill_raid_omen_109](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_raid_omen_109.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:106}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/raid_omen_109](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/raid_omen_109.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:225](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_raid_omen_109`; [other/attributes/pills/raid_omen_109.mcfunction:6](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/raid_omen_109.mcfunction) — `advancement revoke @s only lbc:pills/pill_raid_omen_109`.

## `lbc:pills/pill_raid_omen_110`

Источник: [lbc:pills/pill_raid_omen_110](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_raid_omen_110.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:107}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/raid_omen_110](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/raid_omen_110.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:226](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_raid_omen_110`; [other/attributes/pills/raid_omen_110.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/raid_omen_110.mcfunction) — `advancement revoke @s only lbc:pills/pill_raid_omen_110`.

## `lbc:pills/pill_raid_omen_111`

Источник: [lbc:pills/pill_raid_omen_111](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_raid_omen_111.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:108}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/raid_omen_111](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/raid_omen_111.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:227](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_raid_omen_111`; [other/attributes/pills/raid_omen_111.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/raid_omen_111.mcfunction) — `advancement revoke @s only lbc:pills/pill_raid_omen_111`.

## `lbc:pills/pill_raid_omen_112`

Источник: [lbc:pills/pill_raid_omen_112](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_raid_omen_112.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:109}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/raid_omen_112](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/raid_omen_112.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:228](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_raid_omen_112`; [other/attributes/pills/raid_omen_112.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/raid_omen_112.mcfunction) — `advancement revoke @s only lbc:pills/pill_raid_omen_112`.

## `lbc:pills/pill_raid_omen_113`

Источник: [lbc:pills/pill_raid_omen_113](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_raid_omen_113.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:110}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/raid_omen_113](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/raid_omen_113.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:229](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_raid_omen_113`; [other/attributes/pills/raid_omen_113.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/raid_omen_113.mcfunction) — `advancement revoke @s only lbc:pills/pill_raid_omen_113`.

## `lbc:pills/pill_raid_omen_114`

Источник: [lbc:pills/pill_raid_omen_114](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_raid_omen_114.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:111}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/raid_omen_114](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/raid_omen_114.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:230](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_raid_omen_114`; [other/attributes/pills/raid_omen_114.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/raid_omen_114.mcfunction) — `advancement revoke @s only lbc:pills/pill_raid_omen_114`.

## `lbc:pills/pill_raid_omen_115`

Источник: [lbc:pills/pill_raid_omen_115](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_raid_omen_115.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:112}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/raid_omen_115](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/raid_omen_115.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:231](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_raid_omen_115`; [other/attributes/pills/raid_omen_115.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/raid_omen_115.mcfunction) — `advancement revoke @s only lbc:pills/pill_raid_omen_115`.

## `lbc:pills/pill_raid_omen_116`

Источник: [lbc:pills/pill_raid_omen_116](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_raid_omen_116.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:113}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/raid_omen_116](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/raid_omen_116.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:232](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_raid_omen_116`; [other/attributes/pills/raid_omen_116.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/raid_omen_116.mcfunction) — `advancement revoke @s only lbc:pills/pill_raid_omen_116`.

## `lbc:pills/pill_raid_omen_117`

Источник: [lbc:pills/pill_raid_omen_117](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_raid_omen_117.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:114}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/raid_omen_117](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/raid_omen_117.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:233](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_raid_omen_117`; [other/attributes/pills/raid_omen_117.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/raid_omen_117.mcfunction) — `advancement revoke @s only lbc:pills/pill_raid_omen_117`.

## `lbc:pills/pill_raid_omen_118`

Источник: [lbc:pills/pill_raid_omen_118](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_raid_omen_118.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:115}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/raid_omen_118](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/raid_omen_118.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:234](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_raid_omen_118`; [other/attributes/pills/raid_omen_118.mcfunction:6](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/raid_omen_118.mcfunction) — `advancement revoke @s only lbc:pills/pill_raid_omen_118`.

## `lbc:pills/pill_raid_omen_119`

Источник: [lbc:pills/pill_raid_omen_119](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_raid_omen_119.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:116}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/raid_omen_119](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/raid_omen_119.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:235](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_raid_omen_119`; [other/attributes/pills/raid_omen_119.mcfunction:6](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/raid_omen_119.mcfunction) — `advancement revoke @s only lbc:pills/pill_raid_omen_119`.

## `lbc:pills/pill_raid_omen_120`

Источник: [lbc:pills/pill_raid_omen_120](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_raid_omen_120.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:117}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/raid_omen_120](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/raid_omen_120.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:236](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_raid_omen_120`; [other/attributes/pills/raid_omen_120.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/raid_omen_120.mcfunction) — `advancement revoke @s only lbc:pills/pill_raid_omen_120`.

## `lbc:pills/pill_raid_omen_121`

Источник: [lbc:pills/pill_raid_omen_121](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_raid_omen_121.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:118}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/raid_omen_121](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/raid_omen_121.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:237](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_raid_omen_121`; [other/attributes/pills/raid_omen_121.mcfunction:6](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/raid_omen_121.mcfunction) — `advancement revoke @s only lbc:pills/pill_raid_omen_121`.

## `lbc:pills/pill_raid_omen_122`

Источник: [lbc:pills/pill_raid_omen_122](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_raid_omen_122.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:119}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/raid_omen_122](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/raid_omen_122.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:238](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_raid_omen_122`; [other/attributes/pills/raid_omen_122.mcfunction:6](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/raid_omen_122.mcfunction) — `advancement revoke @s only lbc:pills/pill_raid_omen_122`.

## `lbc:pills/pill_raid_omen_123`

Источник: [lbc:pills/pill_raid_omen_123](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_raid_omen_123.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:120}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/raid_omen_123](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/raid_omen_123.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:239](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_raid_omen_123`; [other/attributes/pills/raid_omen_123.mcfunction:6](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/raid_omen_123.mcfunction) — `advancement revoke @s only lbc:pills/pill_raid_omen_123`.

## `lbc:pills/pill_raid_omen_124`

Источник: [lbc:pills/pill_raid_omen_124](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_raid_omen_124.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:121}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/raid_omen_124](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/raid_omen_124.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:240](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_raid_omen_124`; [other/attributes/pills/raid_omen_124.mcfunction:6](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/raid_omen_124.mcfunction) — `advancement revoke @s only lbc:pills/pill_raid_omen_124`.

## `lbc:pills/pill_raid_omen_16`

Источник: [lbc:pills/pill_raid_omen_16](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_raid_omen_16.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:89}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/raid_omen_16](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/raid_omen_16.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:241](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_raid_omen_16`; [other/attributes/pills/raid_omen_16.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/raid_omen_16.mcfunction) — `advancement revoke @s only lbc:pills/pill_raid_omen_16`.

## `lbc:pills/pill_raid_omen_17`

Источник: [lbc:pills/pill_raid_omen_17](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_raid_omen_17.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:90}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/raid_omen_17](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/raid_omen_17.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:242](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_raid_omen_17`; [other/attributes/pills/raid_omen_17.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/raid_omen_17.mcfunction) — `advancement revoke @s only lbc:pills/pill_raid_omen_17`.

## `lbc:pills/pill_raid_omen_18`

Источник: [lbc:pills/pill_raid_omen_18](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_raid_omen_18.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:91}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/raid_omen_18](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/raid_omen_18.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:243](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_raid_omen_18`; [other/attributes/pills/raid_omen_18.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/raid_omen_18.mcfunction) — `advancement revoke @s only lbc:pills/pill_raid_omen_18`.

## `lbc:pills/pill_raid_omen_19`

Источник: [lbc:pills/pill_raid_omen_19](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_raid_omen_19.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:92}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/raid_omen_19](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/raid_omen_19.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:244](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_raid_omen_19`; [other/attributes/pills/raid_omen_19.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/raid_omen_19.mcfunction) — `advancement revoke @s only lbc:pills/pill_raid_omen_19`.

## `lbc:pills/pill_raid_omen_20`

Источник: [lbc:pills/pill_raid_omen_20](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_raid_omen_20.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:93}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/raid_omen_20](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/raid_omen_20.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:245](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_raid_omen_20`; [other/attributes/pills/raid_omen_20.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/raid_omen_20.mcfunction) — `advancement revoke @s only lbc:pills/pill_raid_omen_20`.

## `lbc:pills/pill_raid_omen_40`

Источник: [lbc:pills/pill_raid_omen_40](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_raid_omen_40.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:95}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/raid_omen_40](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/raid_omen_40.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:246](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_raid_omen_40`; [other/attributes/pills/raid_omen_40.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/raid_omen_40.mcfunction) — `advancement revoke @s only lbc:pills/pill_raid_omen_40`.

## `lbc:pills/pill_raid_omen_41`

Источник: [lbc:pills/pill_raid_omen_41](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_raid_omen_41.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:96}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/raid_omen_41](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/raid_omen_41.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:247](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_raid_omen_41`; [other/attributes/pills/raid_omen_41.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/raid_omen_41.mcfunction) — `advancement revoke @s only lbc:pills/pill_raid_omen_41`.

## `lbc:pills/pill_raid_omen_42`

Источник: [lbc:pills/pill_raid_omen_42](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_raid_omen_42.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:97}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/raid_omen_42](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/raid_omen_42.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:248](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_raid_omen_42`; [other/attributes/pills/raid_omen_42.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/raid_omen_42.mcfunction) — `advancement revoke @s only lbc:pills/pill_raid_omen_42`.

## `lbc:pills/pill_raid_omen_74`

Источник: [lbc:pills/pill_raid_omen_74](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_raid_omen_74.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:94}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/raid_omen_74](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/raid_omen_74.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:249](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_raid_omen_74`; [other/attributes/pills/raid_omen_74.mcfunction:6](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/raid_omen_74.mcfunction) — `advancement revoke @s only lbc:pills/pill_raid_omen_74`.

## `lbc:pills/pill_regeneration`

Источник: [lbc:pills/pill_regeneration](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_regeneration.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:122}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/regeneration](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/regeneration.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:250](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_regeneration`; [other/attributes/pills/regeneration.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/regeneration.mcfunction) — `advancement revoke @s only lbc:pills/pill_regeneration`.

## `lbc:pills/pill_resistance`

Источник: [lbc:pills/pill_resistance](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_resistance.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:123}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/resistance](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/resistance.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:251](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_resistance`; [other/attributes/pills/resistance.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/resistance.mcfunction) — `advancement revoke @s only lbc:pills/pill_resistance`.

## `lbc:pills/pill_resistance_5`

Источник: [lbc:pills/pill_resistance_5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_resistance_5.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:124}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/resistance_5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/resistance_5.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:252](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_resistance_5`; [other/attributes/pills/resistance_5.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/resistance_5.mcfunction) — `advancement revoke @s only lbc:pills/pill_resistance_5`.

## `lbc:pills/pill_safe_fall_distance_down`

Источник: [lbc:pills/pill_safe_fall_distance_down](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_safe_fall_distance_down.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:16}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/safe_fall_distance_down](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/safe_fall_distance_down.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:253](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_safe_fall_distance_down`; [other/attributes/pills/safe_fall_distance_down.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/safe_fall_distance_down.mcfunction) — `advancement revoke @s only lbc:pills/pill_safe_fall_distance_down`.

## `lbc:pills/pill_safe_fall_distance_up`

Источник: [lbc:pills/pill_safe_fall_distance_up](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_safe_fall_distance_up.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:43}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/safe_fall_distance_up](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/safe_fall_distance_up.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:254](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_safe_fall_distance_up`; [other/attributes/pills/safe_fall_distance_up.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/safe_fall_distance_up.mcfunction) — `advancement revoke @s only lbc:pills/pill_safe_fall_distance_up`.

## `lbc:pills/pill_saturation`

Источник: [lbc:pills/pill_saturation](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_saturation.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:125}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/saturation](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/saturation.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:255](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_saturation`; [other/attributes/pills/saturation.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/saturation.mcfunction) — `advancement revoke @s only lbc:pills/pill_saturation`.

## `lbc:pills/pill_scale_down`

Источник: [lbc:pills/pill_scale_down](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_scale_down.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:17}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/scale_down](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/scale_down.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:256](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_scale_down`; [other/attributes/pills/scale_down.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/scale_down.mcfunction) — `advancement revoke @s only lbc:pills/pill_scale_down`.

## `lbc:pills/pill_scale_up`

Источник: [lbc:pills/pill_scale_up](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_scale_up.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:44}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/scale_up](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/scale_up.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:257](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_scale_up`; [other/attributes/pills/scale_up.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/scale_up.mcfunction) — `advancement revoke @s only lbc:pills/pill_scale_up`.

## `lbc:pills/pill_slow_falling`

Источник: [lbc:pills/pill_slow_falling](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_slow_falling.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:126}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/slow_falling](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/slow_falling.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:259](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_slow_falling`; [other/attributes/pills/slow_falling.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/slow_falling.mcfunction) — `advancement revoke @s only lbc:pills/pill_slow_falling`.

## `lbc:pills/pill_slowness`

Источник: [lbc:pills/pill_slowness](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_slowness.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:127}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/slowness](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/slowness.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:258](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_slowness`; [other/attributes/pills/slowness.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/slowness.mcfunction) — `advancement revoke @s only lbc:pills/pill_slowness`.

## `lbc:pills/pill_sneaking_speed_down`

Источник: [lbc:pills/pill_sneaking_speed_down](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_sneaking_speed_down.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:24}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/sneaking_speed_down](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/sneaking_speed_down.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:260](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_sneaking_speed_down`; [other/attributes/pills/sneaking_speed_down.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/sneaking_speed_down.mcfunction) — `advancement revoke @s only lbc:pills/pill_sneaking_speed_down`.

## `lbc:pills/pill_sneaking_speed_up`

Источник: [lbc:pills/pill_sneaking_speed_up](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_sneaking_speed_up.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:51}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/sneaking_speed_up](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/sneaking_speed_up.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:261](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_sneaking_speed_up`; [other/attributes/pills/sneaking_speed_up.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/sneaking_speed_up.mcfunction) — `advancement revoke @s only lbc:pills/pill_sneaking_speed_up`.

## `lbc:pills/pill_speed`

Источник: [lbc:pills/pill_speed](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_speed.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:128}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/speed](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/speed.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:262](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_speed`; [other/attributes/pills/speed.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/speed.mcfunction) — `advancement revoke @s only lbc:pills/pill_speed`.

## `lbc:pills/pill_step_height_down`

Источник: [lbc:pills/pill_step_height_down](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_step_height_down.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:18}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/step_height_down](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/step_height_down.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:263](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_step_height_down`; [other/attributes/pills/step_height_down.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/step_height_down.mcfunction) — `advancement revoke @s only lbc:pills/pill_step_height_down`.

## `lbc:pills/pill_step_height_up`

Источник: [lbc:pills/pill_step_height_up](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_step_height_up.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:45}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/step_height_up](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/step_height_up.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:264](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_step_height_up`; [other/attributes/pills/step_height_up.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/step_height_up.mcfunction) — `advancement revoke @s only lbc:pills/pill_step_height_up`.

## `lbc:pills/pill_strength`

Источник: [lbc:pills/pill_strength](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_strength.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:129}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/strength](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/strength.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:265](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_strength`; [other/attributes/pills/strength.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/strength.mcfunction) — `advancement revoke @s only lbc:pills/pill_strength`.

## `lbc:pills/pill_strength_99`

Источник: [lbc:pills/pill_strength_99](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_strength_99.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:130}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/strength_99](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/strength_99.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:266](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_strength_99`; [other/attributes/pills/strength_99.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/strength_99.mcfunction) — `advancement revoke @s only lbc:pills/pill_strength_99`.

## `lbc:pills/pill_submerged_mining_speed_down`

Источник: [lbc:pills/pill_submerged_mining_speed_down](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_submerged_mining_speed_down.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:25}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/submerged_mining_speed_down](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/submerged_mining_speed_down.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:267](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_submerged_mining_speed_down`; [other/attributes/pills/submerged_mining_speed_down.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/submerged_mining_speed_down.mcfunction) — `advancement revoke @s only lbc:pills/pill_submerged_mining_speed_down`.

## `lbc:pills/pill_submerged_mining_speed_up`

Источник: [lbc:pills/pill_submerged_mining_speed_up](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_submerged_mining_speed_up.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:52}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/submerged_mining_speed_up](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/submerged_mining_speed_up.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:268](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_submerged_mining_speed_up`; [other/attributes/pills/submerged_mining_speed_up.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/submerged_mining_speed_up.mcfunction) — `advancement revoke @s only lbc:pills/pill_submerged_mining_speed_up`.

## `lbc:pills/pill_sweeping_damage_ratio_down`

Источник: [lbc:pills/pill_sweeping_damage_ratio_down](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_sweeping_damage_ratio_down.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:26}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/sweeping_damage_ratio_down](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/sweeping_damage_ratio_down.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:269](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_sweeping_damage_ratio_down`; [other/attributes/pills/sweeping_damage_ratio_down.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/sweeping_damage_ratio_down.mcfunction) — `advancement revoke @s only lbc:pills/pill_sweeping_damage_ratio_down`.

## `lbc:pills/pill_sweeping_damage_ratio_up`

Источник: [lbc:pills/pill_sweeping_damage_ratio_up](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_sweeping_damage_ratio_up.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:53}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/sweeping_damage_ratio_up](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/sweeping_damage_ratio_up.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:270](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_sweeping_damage_ratio_up`; [other/attributes/pills/sweeping_damage_ratio_up.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/sweeping_damage_ratio_up.mcfunction) — `advancement revoke @s only lbc:pills/pill_sweeping_damage_ratio_up`.

## `lbc:pills/pill_unluck`

Источник: [lbc:pills/pill_unluck](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_unluck.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:131}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/unluck](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/unluck.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:271](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_unluck`; [other/attributes/pills/unluck.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/unluck.mcfunction) — `advancement revoke @s only lbc:pills/pill_unluck`.

## `lbc:pills/pill_unluck_99`

Источник: [lbc:pills/pill_unluck_99](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_unluck_99.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:132}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/unluck_99](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/unluck_99.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:272](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_unluck_99`; [other/attributes/pills/unluck_99.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/unluck_99.mcfunction) — `advancement revoke @s only lbc:pills/pill_unluck_99`.

## `lbc:pills/pill_water_breathing`

Источник: [lbc:pills/pill_water_breathing](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_water_breathing.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:133}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/water_breathing](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/water_breathing.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:273](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_water_breathing`; [other/attributes/pills/water_breathing.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/water_breathing.mcfunction) — `advancement revoke @s only lbc:pills/pill_water_breathing`.

## `lbc:pills/pill_water_breathing_2`

Источник: [lbc:pills/pill_water_breathing_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_water_breathing_2.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:134}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/water_breathing_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/water_breathing_2.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:274](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_water_breathing_2`; [other/attributes/pills/water_breathing_2.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/water_breathing_2.mcfunction) — `advancement revoke @s only lbc:pills/pill_water_breathing_2`.

## `lbc:pills/pill_water_movement_efficiency_down`

Источник: [lbc:pills/pill_water_movement_efficiency_down](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_water_movement_efficiency_down.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:19}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/water_movement_efficiency_down](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/water_movement_efficiency_down.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:275](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_water_movement_efficiency_down`; [other/attributes/pills/water_movement_efficiency_down.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/water_movement_efficiency_down.mcfunction) — `advancement revoke @s only lbc:pills/pill_water_movement_efficiency_down`.

## `lbc:pills/pill_water_movement_efficiency_up`

Источник: [lbc:pills/pill_water_movement_efficiency_up](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_water_movement_efficiency_up.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:46}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/water_movement_efficiency_up](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/water_movement_efficiency_up.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:276](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_water_movement_efficiency_up`; [other/attributes/pills/water_movement_efficiency_up.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/water_movement_efficiency_up.mcfunction) — `advancement revoke @s only lbc:pills/pill_water_movement_efficiency_up`.

## `lbc:pills/pill_weakness`

Источник: [lbc:pills/pill_weakness](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_weakness.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:135}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/weakness](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/weakness.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:277](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_weakness`; [other/attributes/pills/weakness.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/weakness.mcfunction) — `advancement revoke @s only lbc:pills/pill_weakness`.

## `lbc:pills/pill_weakness_99`

Источник: [lbc:pills/pill_weakness_99](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_weakness_99.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:136}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/weakness_99](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/weakness_99.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:278](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_weakness_99`; [other/attributes/pills/weakness_99.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/weakness_99.mcfunction) — `advancement revoke @s only lbc:pills/pill_weakness_99`.

## `lbc:pills/pill_weaving`

Источник: [lbc:pills/pill_weaving](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_weaving.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:137}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/weaving](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/weaving.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:279](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_weaving`; [other/attributes/pills/weaving.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/weaving.mcfunction) — `advancement revoke @s only lbc:pills/pill_weaving`.

## `lbc:pills/pill_wind_charged`

Источник: [lbc:pills/pill_wind_charged](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_wind_charged.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:138}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/wind_charged](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/wind_charged.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:280](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_wind_charged`; [other/attributes/pills/wind_charged.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/wind_charged.mcfunction) — `advancement revoke @s only lbc:pills/pill_wind_charged`.

## `lbc:pills/pill_wither`

Источник: [lbc:pills/pill_wither](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_wither.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:139}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/wither](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/wither.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:281](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_wither`; [other/attributes/pills/wither.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/wither.mcfunction) — `advancement revoke @s only lbc:pills/pill_wither`.

## `lbc:pills/pill_wither_99`

Источник: [lbc:pills/pill_wither_99](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pill_wither_99.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{pill:140}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/attributes/pills/wither_99](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/wither_99.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:282](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:pills/pill_wither_99`; [other/attributes/pills/wither_99.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/pills/wither_99.mcfunction) — `advancement revoke @s only lbc:pills/pill_wither_99`.

## `pills/pillgen.py`

Источник: [файл](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/pills/pillgen.py).
Ручной генератор JSON таблеток: проходит активный список эффектов по порядку, начиная с числового `pill:54`, и перезаписывает `pill_<имя>.json` в текущем рабочем каталоге. Создаёт событие употребления предмета с нужным custom_data, запрещает его в `minecraft:nexus` и вызывает `lbc:other/attributes/pills/<имя>`. Комментированный список атрибутов не выполняется. Итоговые JSON описаны отдельно; скрипт не запускается Minecraft автоматически.

## `lbc:place_techno_cyborg`

Источник: [lbc:place_techno_cyborg](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/place_techno_cyborg.json).
Критерий `requirement`: игрок взаимодействует с сущностью. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{techno:1}"}}`; сущность: `{"type":"minecraft:armor_stand"}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/techno/test_create_boss](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/test_create_boss.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:122](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:place_techno_cyborg`; [other/techno/test_create_boss.mcfunction:2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/test_create_boss.mcfunction) — `advancement revoke @s only lbc:place_techno_cyborg`.

## `lbc:player_killed_entity`

Источник: [lbc:player_killed_entity](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/player_killed_entity.json).
Критерий `requirement`: игрок убивает сущность. Условия: игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:items/player_killed_entity](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/player_killed_entity.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [items/player_killed_entity.mcfunction:4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/player_killed_entity.mcfunction) — `advancement revoke @s only lbc:player_killed_entity`; [leave_death.mcfunction:123](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:player_killed_entity`.

## `lbc:player_killed_player`

Источник: [lbc:player_killed_player](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/player_killed_player.json).
Критерий `requirement`: игрок убивает сущность. Условия: сущность: (свойства `this`: `{"type":"minecraft:player"}`); игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:items/player_killed_player](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/player_killed_player.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [items/player_killed_player.mcfunction:2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/player_killed_player.mcfunction) — `advancement revoke @s only lbc:player_killed_player`; [leave_death.mcfunction:124](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:player_killed_player`.

## `lbc:purify`

Источник: [lbc:purify](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/purify.json).
Критерий `requirement`: меняются эффекты игрока. Условия: эффекты: `{"minecraft:raid_omen":{"amplifier":43}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/purify](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/purify.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:125](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:purify`; [other/purify.mcfunction:109](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/purify.mcfunction) — `advancement revoke @s only lbc:purify`.

## `lbc:remove_boots`

Источник: [lbc:remove_boots](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/remove_boots.json).
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"equipment":{"feet":{"predicates":{"minecraft:custom_data":"{technomk2:1}"}}}}`)) И (свойства `this`: `{"nbt":"{Tags:[\"technomk2fly\"]}"}`) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:armor/technomk2/take_off](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/armor/technomk2/take_off.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [armor/technomk2/take_off.mcfunction:2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/armor/technomk2/take_off.mcfunction) — `advancement revoke @s only lbc:remove_boots`; [leave_death.mcfunction:126](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:remove_boots`; [resets.mcfunction:115](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/resets.mcfunction) — `advancement revoke @s only lbc:remove_boots`.

## `lbc:sith_killed_jedi`

Источник: [lbc:sith_killed_jedi](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/sith_killed_jedi.json).
Критерий `requirement`: игрок убивает сущность. Условия: игрок: (условие `entity_scores` с параметрами `{"entity":"this","scores":{"november":2}}`); сущность: (условие `entity_scores` с параметрами `{"entity":"this","scores":{"november":1}}`).
Награда: выполнить [lbc:items/sith_killed_jedi](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/sith_killed_jedi.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [items/sith_killed_jedi.mcfunction:3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/sith_killed_jedi.mcfunction) — `advancement revoke @s only lbc:sith_killed_jedi`; [leave_death.mcfunction:127](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:sith_killed_jedi`; [resets.mcfunction:117](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/resets.mcfunction) — `advancement revoke @s only lbc:sith_killed_jedi`.

## `lbc:skill_manavampire_player_hit_by_player`

Источник: [lbc:skill_manavampire_player_hit_by_player](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/skill_manavampire_player_hit_by_player.json).
Критерий `requirement`: игрок получает урон от сущности. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)); урон: `{"blocked":false,"type":{"tags":[{"id":"minecraft:acceptable_for_manavampirism","expected":true}]},"source_entity":{"nbt":"{Tags:[manavampire]}"}}`.
Награда: выполнить [lbc:swrg_kit_integration/game/skills/manavampire/damaged_by_manavampire](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/manavampire/damaged_by_manavampire.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:469](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:skill_manavampire_player_hit_by_player`; [swrg_kit_integration/game/skills/manavampire/damaged_by_manavampire.mcfunction:3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/manavampire/damaged_by_manavampire.mcfunction) — `advancement revoke @s only lbc:skill_manavampire_player_hit_by_player`.

## `lbc:slowness`

Источник: [lbc:slowness](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/slowness.json).
Критерий `requirement`: меняются эффекты игрока. Условия: эффекты: `{"minecraft:slowness":{}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)) И (условие `entity_scores` с параметрами `{"entity":"this","scores":{"lbc.skill":1}}`).
Награда: выполнить [lbc:swrg_kit_integration/game/skills/frozen_heart](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/frozen_heart.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:128](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:slowness`; [swrg_kit_integration/game/skills/frozen_heart.mcfunction:3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/frozen_heart.mcfunction) — `advancement revoke @s only lbc:slowness`.

## `lbc:soulhunter_mace_use`

Источник: [lbc:soulhunter_mace_use](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/soulhunter_mace_use.json).
Критерий `requirement`: игрок продолжает использовать предмет. Условия: предмет: `{"predicates":{"custom_data":"{soulhunter_mace:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/event/halloween/soulhunter_mace/use](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulhunter_mace/use.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:468](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:soulhunter_mace_use`; [other/event/halloween/soulhunter_mace/use.mcfunction:4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulhunter_mace/use.mcfunction) — `advancement revoke @s only lbc:soulhunter_mace_use`.

## `lbc:steampunk_pickaxe`

Источник: [lbc:steampunk_pickaxe](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/steampunk_pickaxe.json).
Критерий `requirement`: меняется повреждение предмета. Условия: игрок: (свойства `this`: `{"nbt":"{SelectedItem:{components:{\"minecraft:custom_data\":{steampunk_pickaxe:1}}}}"}`) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)) И (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)).
Награда: выполнить [lbc:swrg_kit_integration/items/steampunk_pickaxe_use](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/items/steampunk_pickaxe_use.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:129](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:steampunk_pickaxe`; [swrg_kit_integration/items/steampunk_pickaxe_use.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/items/steampunk_pickaxe_use.mcfunction) — `advancement revoke @s only lbc:steampunk_pickaxe`.

## `lbc:stewused`

Источник: [lbc:stewused](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/stewused.json).
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: предмет: `{"predicates":{"minecraft:custom_data":"{inf:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/infinitestewused](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/infinitestewused.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:130](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:stewused`; [other/infinitestewused.mcfunction:2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/infinitestewused.mcfunction) — `advancement revoke @s only lbc:stewused`.

## `lbc:true_advancements/accesories/devilstomach`

Источник: [lbc:true_advancements/accesories/devilstomach](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/accesories/devilstomach.json).
В дереве достижений: `lbc.adv.accessories.devil_stomach`; описание: `lbc.adv.accessories.devil_stomach.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:kishki"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/accesories/magnet`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Награда: выполнить [lbc:swrg_kit_integration/gain/kit](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/kit.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [other/extraheal.mcfunction:1](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/extraheal.mcfunction) — `execute if score @s hplvl matches 1 run advancement grant @s only lbc:true_advancements/accesories/devilstomach`.

## `lbc:true_advancements/accesories/infinite_quiver`

Источник: [lbc:true_advancements/accesories/infinite_quiver](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/accesories/infinite_quiver.json).
В дереве достижений: `lbc.adv.accessories.infinite_quiver`; описание: `lbc.adv.accessories.infinite_quiver.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:quiver"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/accesories/devilstomach`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Награда: выполнить [lbc:swrg_kit_integration/gain/levelup](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/levelup.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [other/arrowgive_pre.mcfunction:3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/arrowgive_pre.mcfunction) — `execute if entity @e[type=minecraft:item,distance=..0.01,limit=1,nbt={Item:{components:{"minecraft:custom_data":{stand_arrow:1}}},Age:0s}] run advancement grant @s only lbc:true_advancements/accesories/infinite_quiver`.

## `lbc:true_advancements/accesories/jaakivi`

Источник: [lbc:true_advancements/accesories/jaakivi](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/accesories/jaakivi.json).
В дереве достижений: `lbc.adv.accessories.jaakivi`; описание: `lbc.adv.accessories.jaakivi.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:runic_ice_shard"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/accesories/mystical_artifact`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Награда: выполнить [lbc:swrg_kit_integration/gain/skill](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/skill.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [workingitems/iceshard.mcfunction:11](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/iceshard.mcfunction) — `execute if entity @s[scores={frozen=1..}] run advancement grant @s only lbc:true_advancements/accesories/jaakivi`.

## `lbc:true_advancements/accesories/magnet`

Источник: [lbc:true_advancements/accesories/magnet](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/accesories/magnet.json).
В дереве достижений: `lbc.adv.accessories.magnet`; описание: `lbc.adv.accessories.magnet.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:magnet"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/accesories/mana_belt`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Награда: выполнить [lbc:swrg_kit_integration/gain/skill](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/skill.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [workingitems/magnet.mcfunction:2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/magnet.mcfunction) — `execute if entity @e[type=minecraft:item,distance=..1,limit=1,nbt={Item:{components:{"minecraft:custom_data":{penis_bomb:1}}}}] run advancement grant @s only lbc:true_advancements/accesories/magnet`; [workingitems/magnet.mcfunction:3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/magnet.mcfunction) — `execute if entity @e[type=minecraft:item,distance=..1,limit=1,nbt={Item:{components:{"minecraft:custom_data":{malevich:1}}}}] run advancement grant @s only lbc:true_advancements/accesories/magnet`.

## `lbc:true_advancements/accesories/mana_belt`

Источник: [lbc:true_advancements/accesories/mana_belt](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/accesories/mana_belt.json).
В дереве достижений: `lbc.adv.accessories.mana_belt`; описание: `lbc.adv.accessories.mana_belt.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:manaregenerationbelt"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/accesories/regeneration_belt`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Награда: выполнить [lbc:swrg_kit_integration/gain/levelup](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/levelup.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [other/manaregen_mana_belts.mcfunction:4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/manaregen_mana_belts.mcfunction) — `execute if score manabelts lbc.math > @s manaregen run advancement grant @s only lbc:true_advancements/accesories/mana_belt`.

## `lbc:true_advancements/accesories/mystical_artifact`

Источник: [lbc:true_advancements/accesories/mystical_artifact](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/accesories/mystical_artifact.json).
В дереве достижений: `lbc.adv.accessories.mystical_artifact`; описание: `lbc.adv.accessories.mystical_artifact.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:mysticalartifact"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/accesories/popit`; это связь отображения, а не дополнительный критерий.
Критерий `art_1`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{mystical: 1,type: 1}"}}`).
Критерий `art_2`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{mystical: 1,type: 2}"}}`).
Критерий `art_3`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{mystical: 1,type: 3}"}}`).
Критерий `art_4`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{mystical: 1,type: 4}"}}`).
Критерий `art_5`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{mystical: 1,type: 5}"}}`).
Критерий `art_6`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{mystical: 1,type: 6}"}}`).
Критерий `art_7`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{mystical: 1,type: 7}"}}`).
Критерий `art_8`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{mystical: 1,type: 8}"}}`).
Критерий `art_9`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{mystical: 2,type: 1}"}}`).
Критерий `art_10`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{mystical: 2,type: 2}"}}`).
Критерий `art_11`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{mystical: 2,type: 3}"}}`).
Критерий `art_12`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{mystical: 2,type: 4}"}}`).
Критерий `art_13`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{mystical: 2,type: 5}"}}`).
Критерий `art_14`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{mystical: 2,type: 6}"}}`).
Критерий `art_15`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{mystical: 3,type: 1}"}}`).
Критерий `art_16`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{mystical: 3,type: 2}"}}`).
Критерий `art_17`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{mystical: 3,type: 3}"}}`).
Критерий `art_18`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{mystical: 4,type: 1}"}}`).
Критерий `art_19`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{mystical: 4,type: 2}"}}`).
Критерий `art_20`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{mystical: 4,type: 3}"}}`).
Критерий `art_21`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{mystical: 4,type: 4}"}}`).
Критерий `art_22`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{mystical: 4,type: 5}"}}`).
Критерий `art_23`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{mystical: 4,type: 6}"}}`).
Для завершения требуются все 23 критериев.
Награда: выполнить [lbc:swrg_kit_integration/gain/skill](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/skill.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/accesories/popit`

Источник: [lbc:true_advancements/accesories/popit](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/accesories/popit.json).
В дереве достижений: `lbc.adv.accessories.popit`; описание: `lbc.adv.accessories.popit.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:popit"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/accesories/infinite_quiver`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Награда: выполнить [lbc:swrg_kit_integration/gain/kit](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/kit.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [workingitems/popit.mcfunction:3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/popit.mcfunction) — `execute if score #random27 lbc.math matches 1 if entity @s[nbt={active_effects:[{id:"minecraft:instant_damage",duration:-1}]}] run advancement grant @s only lbc:true_advancements/accesories/popit`; [workingitems/popit.mcfunction:8](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/popit.mcfunction) — `execute if score #random27 lbc.math matches 5 if entity @s[nbt={active_effects:[{id:"minecraft:poison",duration:-1}]}] run advancement grant @s only lbc:true_advancements/accesories/popit`; [workingitems/popit.mcfunction:14](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/popit.mcfunction) — `execute if score #random27 lbc.math matches 10 if entity @s[nbt={active_effects:[{id:"minecraft:wither",duration:-1}]}] run advancement grant @s only lbc:true_advancements/accesories/popit`; [workingitems/popit.mcfunction:21](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/popit.mcfunction) — `execute if score #random27 lbc.math matches 16 if score @s delayed_death matches 1.. run advancement grant @s only lbc:true_advancements/accesories/popit`; [workingitems/popit.mcfunction:33](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/popit.mcfunction) — `execute if score #random27 lbc.math matches 27 if entity @s[tag=greed_cursed] run advancement grant @s only lbc:true_advancements/accesories/popit`.

## `lbc:true_advancements/accesories/regeneration_belt`

Источник: [lbc:true_advancements/accesories/regeneration_belt](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/accesories/regeneration_belt.json).
В дереве достижений: `lbc.adv.accessories.regeneration_belt`; описание: `lbc.adv.accessories.regeneration_belt.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:regenerationbelt"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/accesories/sand_amulet`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Награда: выполнить [lbc:swrg_kit_integration/gain/levelup](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/levelup.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [workingitems/regeneration_belt.mcfunction:9](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/regeneration_belt.mcfunction) — `execute if score @s y matches ..-100000 if score tmp lbc.math matches 10.. run advancement grant @s only lbc:true_advancements/accesories/regeneration_belt`.

## `lbc:true_advancements/accesories/sand_amulet`

Источник: [lbc:true_advancements/accesories/sand_amulet](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/accesories/sand_amulet.json).
В дереве достижений: `lbc.adv.accessories.sand_amulet`; описание: `lbc.adv.accessories.sand_amulet.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:gaara"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/accesories/save_platform`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Награда: выполнить [lbc:swrg_kit_integration/gain/kit](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/kit.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [workingitems/gaara1_2.mcfunction:4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/gaara1_2.mcfunction) — `execute if entity @a[gamemode=!spectator,distance=0.01..8,predicate=!lbc:same_team] run advancement grant @s only lbc:true_advancements/accesories/sand_amulet`.

## `lbc:true_advancements/accesories/save_platform`

Источник: [lbc:true_advancements/accesories/save_platform](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/accesories/save_platform.json).
В дереве достижений: `lbc.adv.accessories.save_platform`; описание: `lbc.adv.accessories.save_platform.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:saveplatform"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/accesories`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Награда: выполнить [lbc:swrg_kit_integration/gain/levelup](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/levelup.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [workingitems/platform.mcfunction:6](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/platform.mcfunction) — `advancement grant @s only lbc:true_advancements/accesories/save_platform`.

## `lbc:true_advancements/accesories`

Источник: [lbc:true_advancements/accesories](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/accesories.json).
В дереве достижений: `lbc.adv.accessories`; описание: ``. Параметры показа: `{"icon":{"id":"minecraft:heart_of_the_sea"},"frame":"task","show_toast":true,"announce_to_chat":false,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/root`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable:1,accessory:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/arcanums/babylon_gate`

Источник: [lbc:true_advancements/arcanums/babylon_gate](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/arcanums/babylon_gate.json).
В дереве достижений: `lbc.adv.arcanums.babylon_gate`; описание: `lbc.adv.arcanums.babylon_gate.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:babylongate"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/arcanums/tannerkivi`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок убивает сущность. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)) И (свойства `this`: `{"equipment":{"mainhand":{"predicates":{"minecraft:custom_data":"{transformable:1,arcanum:1,gate_of_babylon:1}"}}}}`); сущность: (свойства `this`: `{"type":"minecraft:player"}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/levelup](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/levelup.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/arcanums/barrier`

Источник: [lbc:true_advancements/arcanums/barrier](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/arcanums/barrier.json).
В дереве достижений: `lbc.adv.arcanums.barrier`; описание: `lbc.adv.arcanums.barrier.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:arcanumbarrier"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/arcanums/black_hole`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок завершает падение с высоты. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)) И (свойства `this`: `{"equipment":{"mainhand":{"predicates":{"minecraft:custom_data":"{transformable:1,arcanum:1,barrier_arcanum:1}"}}}}`); расстояние: `{"y":{"min":100}}`.
Награда: выполнить [lbc:swrg_kit_integration/gain/levelup](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/levelup.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/arcanums/barrier_and_heal`

Источник: [lbc:true_advancements/arcanums/barrier_and_heal](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/arcanums/barrier_and_heal.json).
В дереве достижений: `lbc.adv.arcanums.barrier_and_heal`; описание: `lbc.adv.arcanums.barrier_and_heal.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:advancement_icon/barrier_and_heal"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/arcanums/barrier`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable:1,arcanum:1,healing_arcanum:1}"}}`) И (`{"predicates":{"minecraft:custom_data":"{transformable:1,arcanum:1,barrier_arcanum:1}"}}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/kit](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/kit.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/arcanums/black_hole`

Источник: [lbc:true_advancements/arcanums/black_hole](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/arcanums/black_hole.json).
В дереве достижений: `lbc.adv.arcanums.black_hole`; описание: `lbc.adv.arcanums.black_hole.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:arcanumblackhole"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/arcanums`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Награда: выполнить [lbc:swrg_kit_integration/gain/levelup](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/levelup.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [items/arcanums/void_curse_spawn.mcfunction:4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/arcanums/void_curse_spawn.mcfunction) — `advancement grant @s only lbc:true_advancements/arcanums/black_hole`.

## `lbc:true_advancements/arcanums/black_hole_2`

Источник: [lbc:true_advancements/arcanums/black_hole_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/arcanums/black_hole_2.json).
В дереве достижений: `lbc.adv.arcanums.black_hole_2`; описание: `lbc.adv.arcanums.black_hole_2.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:arcanumblackhole"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/arcanums/black_hole`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Награда: выполнить [lbc:swrg_kit_integration/gain/levelup](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/levelup.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [raycast/voidcursed2.mcfunction:7](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/raycast/voidcursed2.mcfunction) — `advancement grant @a[gamemode=!spectator,tag=curseowner] only lbc:true_advancements/arcanums/black_hole_2`.

## `lbc:true_advancements/arcanums/black_hole_max`

Источник: [lbc:true_advancements/arcanums/black_hole_max](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/arcanums/black_hole_max.json).
В дереве достижений: `lbc.adv.arcanums.black_hole_max`; описание: `lbc.adv.arcanums.black_hole_max.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:arcanumblackhole","minecraft:enchantment_glint_override":true}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/arcanums/black_hole_2`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Награда: выполнить [lbc:swrg_kit_integration/gain/levelup](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/levelup.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [raycast/voidcursed5.mcfunction:9](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/raycast/voidcursed5.mcfunction) — `advancement grant @a[gamemode=!spectator,tag=curseowner] only lbc:true_advancements/arcanums/black_hole_max`.

## `lbc:true_advancements/arcanums/heal`

Источник: [lbc:true_advancements/arcanums/heal](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/arcanums/heal.json).
В дереве достижений: `lbc.adv.arcanums.heal`; описание: `lbc.adv.arcanums.heal.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:arcanumhealing"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/arcanums/barrier`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Награда: выполнить [lbc:swrg_kit_integration/gain/levelup](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/levelup.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [items/arcanums/healing.mcfunction:1](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/arcanums/healing.mcfunction) — `execute if score @s hplvl matches 0..1 run advancement grant @s only lbc:true_advancements/arcanums/heal`.

## `lbc:true_advancements/arcanums/kiuaskivi`

Источник: [lbc:true_advancements/arcanums/kiuaskivi](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/arcanums/kiuaskivi.json).
В дереве достижений: `lbc.adv.arcanums.kiuaskivi`; описание: `lbc.adv.arcanums.kiuaskivi.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:kiuaskivi"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/arcanums/heal`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок меняет измерение. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)) И (свойства `this`: `{"location":{"dimension":"minecraft:the_nether"}}`) И (свойства `this`: `{"equipment":{"mainhand":{"predicates":{"minecraft:custom_data":"{transformable:1,arcanum:1,kiuaskivi:1}"}}}}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/kit](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/kit.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/arcanums/neutral`

Источник: [lbc:true_advancements/arcanums/neutral](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/arcanums/neutral.json).
В дереве достижений: `lbc.adv.arcanums.neutral`; описание: `lbc.adv.arcanums.neutral.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:neutral_ball"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/arcanums/babylon_gate`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Награда: выполнить [lbc:swrg_kit_integration/gain/levelup](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/levelup.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [items/arcanums/neutral_ball.mcfunction:3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/arcanums/neutral_ball.mcfunction) — `advancement grant @s only lbc:true_advancements/arcanums/neutral`.

## `lbc:true_advancements/arcanums/tannerkivi`

Источник: [lbc:true_advancements/arcanums/tannerkivi](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/arcanums/tannerkivi.json).
В дереве достижений: `lbc.adv.arcanums.tannerkivi`; описание: `lbc.adv.arcanums.tannerkivi.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:tannerkivi"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/arcanums/kiuaskivi`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Награда: выполнить [lbc:swrg_kit_integration/gain/skill](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/skill.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [items/arcanums/tannerskivi_tick.mcfunction:7](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/arcanums/tannerskivi_tick.mcfunction) — `execute if entity @e[type=#minecraft:projectiles_tannerkivi,tag=!swrg.center,tag=!swrg.default,tag=!swrg.look,tag=!swrg.midle,tag=!swrg.spawn,tag=!magic_academy,tag=!transformator,tag=!danmaku_bomb,tag=!danmaku_bomb_fast,tag=!crocodildo,tag=!crocodildo_hitbox,tag=!helicopter_boss,tag=!helicopter_boss_hitbox,tag=!grimoire_boss_hitbox,tag=!grimoire_boss_visual,distance=..6,limit=1] run advancement grant @s only lbc:true_advancements/arcanums/tannerkivi`.

## `lbc:true_advancements/arcanums`

Источник: [lbc:true_advancements/arcanums](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/arcanums.json).
В дереве достижений: `lbc.adv.arcanums`; описание: ``. Параметры показа: `{"icon":{"id":"minecraft:ender_eye"},"frame":"task","show_toast":true,"announce_to_chat":false,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/root`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable:1,arcanum:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/armor/antimagic`

Источник: [lbc:true_advancements/armor/antimagic](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/armor/antimagic.json).
В дереве достижений: `lbc.adv.armor.overload`; описание: `lbc.adv.armor.overload.lore`. Параметры показа: `{"icon":{"id":"minecraft:leather_chestplate","components":{"minecraft:item_model":"lbc:armors/overload_chestplate"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/armor/flesh`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок убивает сущность. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)) И (свойства `this`: `{"equipment":{"head":{"predicates":{"minecraft:custom_data":"{overload: 1}"}},"chest":{"predicates":{"minecraft:custom_data":"{overload: 1}"}},"legs":{"predicates":{"minecraft:custom_data":"{overload: 1}"}},"feet":{"predicates":{"minecraft:custom_data":"{overload: 1}"}}}}`); сущность: (свойства `this`: `{"type":"minecraft:player"}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/levelup](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/levelup.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/armor/assasin`

Источник: [lbc:true_advancements/armor/assasin](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/armor/assasin.json).
В дереве достижений: `lbc.adv.armor.assasin`; описание: `lbc.adv.armor.assasin.lore`. Параметры показа: `{"icon":{"id":"minecraft:leather_chestplate","components":{"minecraft:item_model":"lbc:armors/assasin_chestplate"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/armor/berserker`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок убивает сущность. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)) И (свойства `this`: `{"equipment":{"head":{"predicates":{"minecraft:custom_data":"{assasin: 1}"}},"chest":{"predicates":{"minecraft:custom_data":"{assasin: 1}"}},"legs":{"predicates":{"minecraft:custom_data":"{assasin: 1}"}},"feet":{"predicates":{"minecraft:custom_data":"{assasin: 1}"}}}}`); сущность: (свойства `this`: `{"type":"minecraft:player"}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/levelup](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/levelup.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/armor/berserker`

Источник: [lbc:true_advancements/armor/berserker](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/armor/berserker.json).
В дереве достижений: `lbc.adv.armor.berserker`; описание: `lbc.adv.armor.berserker.lore`. Параметры показа: `{"icon":{"id":"minecraft:leather_chestplate","components":{"minecraft:item_model":"lbc:armors/berserk_chestplate"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/armor/tnt_jacket`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок убивает сущность. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)) И (свойства `this`: `{"equipment":{"head":{"predicates":{"minecraft:custom_data":"{berserk:1}"}},"chest":{"predicates":{"minecraft:custom_data":"{berserk:1}"}},"legs":{"predicates":{"minecraft:custom_data":"{berserk:1}"}},"feet":{"predicates":{"minecraft:custom_data":"{berserk:1}"}}}}`); сущность: (свойства `this`: `{"type":"minecraft:player"}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/levelup](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/levelup.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/armor/chaos`

Источник: [lbc:true_advancements/armor/chaos](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/armor/chaos.json).
В дереве достижений: `lbc.adv.armor.chaos`; описание: `lbc.adv.armor.chaos.lore`. Параметры показа: `{"icon":{"id":"minecraft:leather_chestplate","components":{"minecraft:item_model":"lbc:armors/chaos_chestplate"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/armor/assasin`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок убивает сущность. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)) И (свойства `this`: `{"equipment":{"head":{"predicates":{"minecraft:custom_data":"{chaoschestplate: 1}"}},"chest":{"predicates":{"minecraft:custom_data":"{chaoschestplate: 1}"}},"legs":{"predicates":{"minecraft:custom_data":"{chaoschestplate: 1}"}},"feet":{"predicates":{"minecraft:custom_data":"{chaoschestplate: 1}"}}}}`); сущность: (свойства `this`: `{"type":"minecraft:player"}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/levelup](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/levelup.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/armor/chaos_full`

Источник: [lbc:true_advancements/armor/chaos_full](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/armor/chaos_full.json).
В дереве достижений: `lbc.adv.armor.chaos_full`; описание: `lbc.adv.armor.chaos_full.lore`. Параметры показа: `{"icon":{"id":"minecraft:leather_helmet","components":{"minecraft:item_model":"lbc:armors/chaos_helmet","minecraft:enchantment_glint_override":true}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/armor/chaos`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)) И (свойства `this`: `{"equipment":{"head":{"predicates":{"minecraft:custom_data":"{chaoschestplate: 1}"}},"chest":{"predicates":{"minecraft:custom_data":"{chaoschestplate: 1}"}},"legs":{"predicates":{"minecraft:custom_data":"{chaoschestplate: 1}"}},"feet":{"predicates":{"minecraft:custom_data":"{chaoschestplate: 1}"}}}}`); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable: 1,sword: 1,cool: 1}"}}`) И (`{"predicates":{"minecraft:custom_data":"{transformable: 1,staff: 1,unstable_spellbook: 1}"}}`) И (`{"predicates":{"minecraft:custom_data":"{luckyblockwand: 1}"}}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/kit](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/kit.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/armor/erosion`

Источник: [lbc:true_advancements/armor/erosion](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/armor/erosion.json).
В дереве достижений: `lbc.adv.armor.erosion`; описание: `lbc.adv.armor.erosion.lore`. Параметры показа: `{"icon":{"id":"minecraft:leather_chestplate","components":{"minecraft:item_model":"lbc:armors/erosion_chestplate"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/armor/glass`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок убивает сущность. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)) И (свойства `this`: `{"equipment":{"head":{"predicates":{"minecraft:custom_data":"{erosion: 1}"}},"chest":{"predicates":{"minecraft:custom_data":"{erosion: 1}"}},"legs":{"predicates":{"minecraft:custom_data":"{erosion: 1}"}},"feet":{"predicates":{"minecraft:custom_data":"{erosion: 1}"}}}}`); сущность: (свойства `this`: `{"type":"minecraft:player"}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/skill](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/skill.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/armor/flesh`

Источник: [lbc:true_advancements/armor/flesh](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/armor/flesh.json).
В дереве достижений: `lbc.adv.armor.flesh`; описание: `lbc.adv.armor.flesh.lore`. Параметры показа: `{"icon":{"id":"minecraft:leather_chestplate","components":{"minecraft:item_model":"lbc:armors/flesh_chestplate"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/armor/nano`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок убивает сущность. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)) И (свойства `this`: `{"equipment":{"head":{"predicates":{"minecraft:custom_data":"{flesh: 1}"}},"chest":{"predicates":{"minecraft:custom_data":"{flesh: 1}"}},"legs":{"predicates":{"minecraft:custom_data":"{flesh: 1}"}},"feet":{"predicates":{"minecraft:custom_data":"{flesh: 1}"}}}}`); сущность: (свойства `this`: `{"type":"minecraft:player"}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/levelup](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/levelup.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/armor/glass`

Источник: [lbc:true_advancements/armor/glass](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/armor/glass.json).
В дереве достижений: `lbc.adv.armor.glass`; описание: `lbc.adv.armor.glass.lore`. Параметры показа: `{"icon":{"id":"minecraft:leather_chestplate","components":{"minecraft:item_model":"lbc:armors/glass_chestplate"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/armor/traveller`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок убивает сущность. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)) И (свойства `this`: `{"equipment":{"head":{"predicates":{"minecraft:custom_data":"{glass: 1}"}},"chest":{"predicates":{"minecraft:custom_data":"{glass: 1}"}},"legs":{"predicates":{"minecraft:custom_data":"{glass: 1}"}},"feet":{"predicates":{"minecraft:custom_data":"{glass: 1}"}}}}`); сущность: (свойства `this`: `{"type":"minecraft:player"}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/levelup](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/levelup.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/armor/mage`

Источник: [lbc:true_advancements/armor/mage](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/armor/mage.json).
В дереве достижений: `lbc.adv.armor.mage`; описание: `lbc.adv.armor.mage.lore`. Параметры показа: `{"icon":{"id":"minecraft:leather_chestplate","components":{"minecraft:item_model":"lbc:armors/mage_mantle"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/armor/chaos`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок убивает сущность. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)) И (свойства `this`: `{"equipment":{"head":{"predicates":{"minecraft:custom_data":"{mage: 1}"}},"chest":{"predicates":{"minecraft:custom_data":"{mage: 1}"}},"legs":{"predicates":{"minecraft:custom_data":"{mage: 1}"}},"feet":{"predicates":{"minecraft:custom_data":"{mage: 1}"}}}}`); сущность: (свойства `this`: `{"type":"minecraft:player"}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/levelup](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/levelup.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/armor/moisei_boots`

Источник: [lbc:true_advancements/armor/moisei_boots](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/armor/moisei_boots.json).
В дереве достижений: `lbc.adv.armor.moisei`; описание: `lbc.adv.armor.moisei.lore`. Параметры показа: `{"icon":{"id":"minecraft:leather_boots","components":{"minecraft:item_model":"lbc:armors/moisei_boots"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/armor/traveller`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок убивает сущность. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)) И (свойства `this`: `{"equipment":{"feet":{"predicates":{"minecraft:custom_data":"{moisei:1}"}}}}`); сущность: (свойства `this`: `{"type":"minecraft:player"}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/kit](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/kit.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/armor/multiplying`

Источник: [lbc:true_advancements/armor/multiplying](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/armor/multiplying.json).
В дереве достижений: `lbc.adv.armor.multiplying`; описание: `lbc.adv.armor.multiplying.lore`. Параметры показа: `{"icon":{"id":"minecraft:golden_chestplate","components":{"minecraft:enchantment_glint_override":true}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/armor`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок убивает сущность. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)) И (свойства `this`: `{"equipment":{"head":{"predicates":{"minecraft:custom_data":"{multiplying: 1}"}},"chest":{"predicates":{"minecraft:custom_data":"{multiplying: 1}"}},"legs":{"predicates":{"minecraft:custom_data":"{multiplying: 1}"}},"feet":{"predicates":{"minecraft:custom_data":"{multiplying: 1}"}}}}`); сущность: (свойства `this`: `{"type":"minecraft:player"}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/kit](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/kit.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/armor/nano`

Источник: [lbc:true_advancements/armor/nano](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/armor/nano.json).
В дереве достижений: `lbc.adv.armor.nano`; описание: `lbc.adv.armor.nano.lore`. Параметры показа: `{"icon":{"id":"minecraft:leather_chestplate","components":{"minecraft:item_model":"lbc:armors/nanobots_chestplate"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/armor/erosion`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок убивает сущность. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)) И (свойства `this`: `{"equipment":{"head":{"predicates":{"minecraft:custom_data":"{nanobots: 1}"}},"chest":{"predicates":{"minecraft:custom_data":"{nanobots: 1}"}},"legs":{"predicates":{"minecraft:custom_data":"{nanobots: 1}"}},"feet":{"predicates":{"minecraft:custom_data":"{nanobots: 1}"}}}}`); сущность: (свойства `this`: `{"type":"minecraft:player"}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/kit](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/kit.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/armor/nyauwu`

Источник: [lbc:true_advancements/armor/nyauwu](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/armor/nyauwu.json).
В дереве достижений: `lbc.adv.armor.nyauwu`; описание: `lbc.adv.armor.nyauwu.lore`. Параметры показа: `{"icon":{"id":"minecraft:leather_helmet","components":{"minecraft:item_model":"lbc:armors/nyauwu_chestplate"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/armor/mage`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок убивает сущность. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)) И (свойства `this`: `{"equipment":{"head":{"predicates":{"minecraft:custom_data":"{nyauwu: 1}"}},"chest":{"predicates":{"minecraft:custom_data":"{nyauwu: 1}"}},"legs":{"predicates":{"minecraft:custom_data":"{nyauwu: 1}"}},"feet":{"predicates":{"minecraft:custom_data":"{nyauwu: 1}"}}}}`); сущность: (свойства `this`: `{"type":"minecraft:player"}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/skill](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/skill.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/armor/paladin`

Источник: [lbc:true_advancements/armor/paladin](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/armor/paladin.json).
В дереве достижений: `lbc.adv.armor.paladin`; описание: `lbc.adv.armor.paladin.lore`. Параметры показа: `{"icon":{"id":"minecraft:leather_chestplate","components":{"minecraft:item_model":"lbc:armors/paladin_chestplate"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/armor/mage`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок убивает сущность. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)) И (свойства `this`: `{"equipment":{"head":{"predicates":{"minecraft:custom_data":"{paladin: 1}"}},"chest":{"predicates":{"minecraft:custom_data":"{paladin: 1}"}},"legs":{"predicates":{"minecraft:custom_data":"{paladin: 1}"}},"feet":{"predicates":{"minecraft:custom_data":"{paladin: 1}"}}}}`); сущность: (свойства `this`: `{"type":"minecraft:player"}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/skill](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/skill.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/armor/phantom`

Источник: [lbc:true_advancements/armor/phantom](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/armor/phantom.json).
В дереве достижений: `lbc.adv.armor.phantom`; описание: `lbc.adv.armor.phantom.lore`. Параметры показа: `{"icon":{"id":"minecraft:leather_chestplate","components":{"minecraft:item_model":"lbc:armors/phantom_chestplate"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/armor/paladin`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок убивает сущность. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)) И (свойства `this`: `{"equipment":{"head":{"predicates":{"minecraft:custom_data":"{phantom: 1}"}},"chest":{"predicates":{"minecraft:custom_data":"{phantom: 1}"}},"legs":{"predicates":{"minecraft:custom_data":"{phantom: 1}"}},"feet":{"predicates":{"minecraft:custom_data":"{phantom: 1}"}}}}`); сущность: (свойства `this`: `{"type":"minecraft:player"}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/kit](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/kit.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/armor/randchanted`

Источник: [lbc:true_advancements/armor/randchanted](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/armor/randchanted.json).
В дереве достижений: `lbc.adv.armor.randchanted`; описание: `lbc.adv.armor.randchanted.lore`. Параметры показа: `{"icon":{"id":"minecraft:iron_chestplate","components":{"minecraft:enchantment_glint_override":true}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/armor/multiplying`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок убивает сущность. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)) И (свойства `this`: `{"equipment":{"head":{"predicates":{"minecraft:custom_data":"{randchanted: 1}"}},"chest":{"predicates":{"minecraft:custom_data":"{randchanted: 1}"}},"legs":{"predicates":{"minecraft:custom_data":"{randchanted: 1}"}},"feet":{"predicates":{"minecraft:custom_data":"{randchanted: 1}"}}}}`); сущность: (свойства `this`: `{"type":"minecraft:player"}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/skill](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/skill.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/armor/shitty`

Источник: [lbc:true_advancements/armor/shitty](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/armor/shitty.json).
В дереве достижений: `lbc.adv.armor.shitty`; описание: `lbc.adv.armor.shitty.lore`. Параметры показа: `{"icon":{"id":"minecraft:leather_chestplate","components":{"minecraft:item_model":"lbc:armors/shit_chestplate"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/armor/phantom`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок убивает сущность. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)) И (свойства `this`: `{"equipment":{"head":{"predicates":{"minecraft:custom_data":"{gownoleggings: 1}"}},"chest":{"predicates":{"minecraft:custom_data":"{gownoleggings: 1}"}},"legs":{"predicates":{"minecraft:custom_data":"{gownoleggings: 1}"}},"feet":{"predicates":{"minecraft:custom_data":"{gownoleggings: 1}"}}}}`); сущность: (свойства `this`: `{"type":"minecraft:player"}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/kit](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/kit.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/armor/shitty_full`

Источник: [lbc:true_advancements/armor/shitty_full](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/armor/shitty_full.json).
В дереве достижений: `lbc.adv.armor.shitty_full`; описание: `lbc.adv.armor.shitty_full.lore`. Параметры показа: `{"icon":{"id":"minecraft:leather_helmet","components":{"minecraft:item_model":"lbc:armors/shit_helmet","minecraft:enchantment_glint_override":true}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/armor/shitty`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)) И (свойства `this`: `{"equipment":{"head":{"predicates":{"minecraft:custom_data":"{gownoleggings: 1}"}},"chest":{"predicates":{"minecraft:custom_data":"{gownoleggings: 1}"}},"legs":{"predicates":{"minecraft:custom_data":"{gownoleggings: 1}"}},"feet":{"predicates":{"minecraft:custom_data":"{gownoleggings: 1}"}}}}`); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable: 1,shooter: 1,govnomet: 1}"}}`) И (`{"predicates":{"minecraft:custom_data":"{transformable: 1,sword: 1,shit_katana: 1}"}}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/skill](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/skill.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/armor/shulker`

Источник: [lbc:true_advancements/armor/shulker](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/armor/shulker.json).
В дереве достижений: `lbc.adv.armor.shulker`; описание: `lbc.adv.armor.shulker.lore`. Параметры показа: `{"icon":{"id":"minecraft:leather_chestplate","components":{"minecraft:item_model":"lbc:armors/shulker_chestplate"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/armor/shitty`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок убивает сущность. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)) И (свойства `this`: `{"equipment":{"head":{"predicates":{"minecraft:custom_data":"{shulkshell: 1}"}},"chest":{"predicates":{"minecraft:custom_data":"{shulkshell: 1}"}},"legs":{"predicates":{"minecraft:custom_data":"{shulkshell: 1}"}},"feet":{"predicates":{"minecraft:custom_data":"{shulkshell: 1}"}}}}`); сущность: (свойства `this`: `{"type":"minecraft:player"}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/levelup](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/levelup.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/armor/shulker_full`

Источник: [lbc:true_advancements/armor/shulker_full](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/armor/shulker_full.json).
В дереве достижений: `lbc.adv.armor.shulker_full`; описание: `lbc.adv.armor.shulker_full.lore`. Параметры показа: `{"icon":{"id":"minecraft:leather_helmet","components":{"minecraft:item_model":"lbc:armors/shulker_helmet","minecraft:enchantment_glint_override":true}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/armor/shulker`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)) И (свойства `this`: `{"equipment":{"head":{"predicates":{"minecraft:custom_data":"{shulkshell: 1}"}},"chest":{"predicates":{"minecraft:custom_data":"{shulkshell: 1}"}},"legs":{"predicates":{"minecraft:custom_data":"{shulkshell: 1}"}},"feet":{"predicates":{"minecraft:custom_data":"{shulkshell: 1}"}}}}`); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable: 1,sword: 1,ender: 1}"}}`) И (`{"predicates":{"minecraft:custom_data":"{transformable: 1,bow: 1,shulker_new: 1}"}}`) И (`{"predicates":{"minecraft:custom_data":"{transformable: 1,staff: 1,enderset: 1}"}}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/skill](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/skill.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/armor/steampunk`

Источник: [lbc:true_advancements/armor/steampunk](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/armor/steampunk.json).
В дереве достижений: `lbc.adv.armor.steampunk`; описание: `lbc.adv.armor.steampunk.lore`. Параметры показа: `{"icon":{"id":"minecraft:leather_chestplate","components":{"minecraft:item_model":"lbc:armors/reactive_chestplate"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/armor/shulker`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок убивает сущность. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)) И (свойства `this`: `{"equipment":{"head":{"predicates":{"minecraft:custom_data":"{reactive: 1}"}},"chest":{"predicates":{"minecraft:custom_data":"{reactive: 1}"}},"legs":{"predicates":{"minecraft:custom_data":"{reactive: 1}"}},"feet":{"predicates":{"minecraft:custom_data":"{reactive: 1}"}}}}`); сущность: (свойства `this`: `{"type":"minecraft:player"}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/kit](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/kit.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/armor/techno`

Источник: [lbc:true_advancements/armor/techno](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/armor/techno.json).
В дереве достижений: `lbc.adv.armor.techno`; описание: `lbc.adv.armor.techno.lore`. Параметры показа: `{"icon":{"id":"minecraft:leather_chestplate","components":{"minecraft:item_model":"lbc:armors/wyvern_chestplate"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/armor/steampunk`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок убивает сущность. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)) И (свойства `this`: `{"equipment":{"head":{"predicates":{"minecraft:custom_data":"{techno: 1}"}},"chest":{"predicates":{"minecraft:custom_data":"{techno: 1}"}},"legs":{"predicates":{"minecraft:custom_data":"{techno: 1}"}},"feet":{"predicates":{"minecraft:custom_data":"{techno: 1}"}}}}`); сущность: (свойства `this`: `{"type":"minecraft:player"}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/levelup](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/levelup.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/armor/techno_full`

Источник: [lbc:true_advancements/armor/techno_full](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/armor/techno_full.json).
В дереве достижений: `lbc.adv.armor.techno_full`; описание: `lbc.adv.armor.techno_full.lore`. Параметры показа: `{"icon":{"id":"minecraft:leather_helmet","components":{"minecraft:item_model":"lbc:armors/power_helmet","minecraft:enchantment_glint_override":true}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/armor/techno`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)) И (свойства `this`: `{"equipment":{"head":{"predicates":{"minecraft:custom_data":"{techno: 1}"}},"chest":{"predicates":{"minecraft:custom_data":"{techno: 1}"}},"legs":{"predicates":{"minecraft:custom_data":"{techno: 1}"}},"feet":{"predicates":{"minecraft:custom_data":"{techno: 1}"}}}}`); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable: 1,sword: 1,cyberkatana: 1}"}}`) И (`{"predicates":{"minecraft:custom_data":"{transformable: 1,staff: 1,cyberset: 1}"}}`) И (`{"predicates":{"minecraft:custom_data":"{transformable: 1,bow: 1,railgun_bow: 1}"}}`) И (`{"predicates":{"minecraft:custom_data":"{transformable: 1,shield: 1,energyshield: 1}"}}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/skill](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/skill.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/armor/technomk2`

Источник: [lbc:true_advancements/armor/technomk2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/armor/technomk2.json).
В дереве достижений: `lbc.adv.armor.technomk2`; описание: `lbc.adv.armor.technomk2.lore`. Параметры показа: `{"icon":{"id":"minecraft:leather_chestplate","components":{"minecraft:item_model":"lbc:armors/technomk2_chestplate"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/armor/techno`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок убивает сущность. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)) И (свойства `this`: `{"equipment":{"head":{"predicates":{"minecraft:custom_data":"{technomk2: 1}"}},"chest":{"predicates":{"minecraft:custom_data":"{technomk2: 1}"}},"legs":{"predicates":{"minecraft:custom_data":"{technomk2: 1}"}},"feet":{"predicates":{"minecraft:custom_data":"{technomk2: 1}"}}}}`); сущность: (свойства `this`: `{"type":"minecraft:player"}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/kit](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/kit.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/armor/tnt_jacket`

Источник: [lbc:true_advancements/armor/tnt_jacket](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/armor/tnt_jacket.json).
В дереве достижений: `lbc.adv.armor.tnt_jacket`; описание: `lbc.adv.armor.tnt_jacket.lore`. Параметры показа: `{"icon":{"id":"minecraft:leather_chestplate","components":{"minecraft:item_model":"lbc:armors/tnt_chestplate"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/armor/randchanted`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок убивает сущность. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)) И (свойства `this`: `{"equipment":{"chest":{"predicates":{"minecraft:custom_data":"{dynamite:1}"}}}}`); сущность: (свойства `this`: `{"type":"minecraft:player"}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/levelup](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/levelup.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/armor/traveller`

Источник: [lbc:true_advancements/armor/traveller](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/armor/traveller.json).
В дереве достижений: `lbc.adv.armor.traveller`; описание: `lbc.adv.armor.traveller.lore`. Параметры показа: `{"icon":{"id":"minecraft:leather_chestplate","components":{"minecraft:item_model":"lbc:armors/traveller_chestplate"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/armor/techno`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок убивает сущность. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)) И (свойства `this`: `{"equipment":{"head":{"predicates":{"minecraft:custom_data":"{jumpboost: 1}"}},"chest":{"predicates":{"minecraft:custom_data":"{jumpboost: 1}"}},"legs":{"predicates":{"minecraft:custom_data":"{jumpboost: 1}"}},"feet":{"predicates":{"minecraft:custom_data":"{jumpboost: 1}"}}}}`); сущность: (свойства `this`: `{"type":"minecraft:player"}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/levelup](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/levelup.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/armor`

Источник: [lbc:true_advancements/armor](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/armor.json).
В дереве достижений: `lbc.adv.armor`; описание: ``. Параметры показа: `{"icon":{"id":"minecraft:golden_chestplate"},"frame":"task","show_toast":true,"announce_to_chat":false,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/root`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)) И (свойства `this`: `{"equipment":{"head":{"predicates":{"minecraft:custom_data":"{transformable:1,helmet:1}"}},"chest":{"predicates":{"minecraft:custom_data":"{transformable:1,chestplate:1}"}},"legs":{"predicates":{"minecraft:custom_data":"{transformable:1,leggings:1}"}},"feet":{"predicates":{"minecraft:custom_data":"{transformable:1,boots:1}"}}}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/axes/banhammer`

Источник: [lbc:true_advancements/axes/banhammer](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/axes/banhammer.json).
В дереве достижений: `lbc.adv.axe.banhammer`; описание: `lbc.adv.axe.banhammer.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:banhammer"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/axes/hammer`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Награда: выполнить [lbc:swrg_kit_integration/gain/skill](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/skill.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [items/swords/banhammer/tick_fall_end_advancement.mcfunction:2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/banhammer/tick_fall_end_advancement.mcfunction) — `execute at @a[gamemode=!spectator] if score @a[gamemode=!spectator,distance=..0.01,limit=1] lbcID2 = @s lbcID2 run advancement grant @a[gamemode=!spectator,distance=..0.01,limit=1] only lbc:true_advancements/axes/banhammer`.

## `lbc:true_advancements/axes/hammer`

Источник: [lbc:true_advancements/axes/hammer](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/axes/hammer.json).
В дереве достижений: `lbc.adv.axe.hammer`; описание: `lbc.adv.axe.hammer.lore`. Параметры показа: `{"icon":{"id":"minecraft:iron_axe","components":{"minecraft:item_model":"lbc:hammer"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/axes/poleaxe`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок убивает сущность. Условия: игрок: (свойства `this`: `{"equipment":{"mainhand":{"predicates":{"minecraft:custom_data":"{transformable: 1,axe: 1,hammer: 1}"}}}}`); сущность: (свойства `this`: `{"type":"minecraft:silverfish"}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/skill](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/skill.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/axes/poleaxe`

Источник: [lbc:true_advancements/axes/poleaxe](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/axes/poleaxe.json).
В дереве достижений: `lbc.adv.axe.poleaxe`; описание: `lbc.adv.axe.poleaxe.lore`. Параметры показа: `{"icon":{"id":"minecraft:netherite_axe"},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/axes/tomahawk`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок наносит урон сущности. Условия: игрок: (свойства `this`: `{"equipment":{"mainhand":{"predicates":{"minecraft:custom_data":"{transformable:1,axe:1,poleaxe:1}"}}}}`); урон: `{"taken":{"min":20},"blocked":false}`; сущность: (свойства `this`: `{"type":"minecraft:player"}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/levelup](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/levelup.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/axes/randchanted`

Источник: [lbc:true_advancements/axes/randchanted](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/axes/randchanted.json).
В дереве достижений: `lbc.adv.axe.randchanted`; описание: `lbc.adv.axe.randchanted.lore`. Параметры показа: `{"icon":{"id":"minecraft:iron_axe","components":{"minecraft:enchantment_glint_override":true}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/axes`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:enchantments":[{"enchantments":"minecraft:efficiency","levels":{"min":4}},{"enchantments":"minecraft:fortune","levels":{"min":3}},{"enchantments":"minecraft:unbreaking","levels":{"min":3}}],"minecraft:custom_data":"{transformable:1,axe:1,randchanted:1}"}}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/kit](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/kit.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/axes/tomahawk`

Источник: [lbc:true_advancements/axes/tomahawk](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/axes/tomahawk.json).
В дереве достижений: `lbc.adv.axe.tomahawk`; описание: `lbc.adv.axe.tomahawk.lore`. Параметры показа: `{"icon":{"id":"minecraft:golden_axe"},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/axes/randchanted`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок убивает сущность. Условия: игрок: (свойства `this`: `{"equipment":{"mainhand":{"predicates":{"minecraft:custom_data":"{transformable:1,axe:1,tomahawk:1}"}}}}`); сущность: (свойства `this`: `{"type":"minecraft:player"}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/levelup](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/levelup.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/axes`

Источник: [lbc:true_advancements/axes](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/axes.json).
В дереве достижений: `lbc.adv.axes`; описание: ``. Параметры показа: `{"icon":{"id":"minecraft:golden_axe"},"frame":"task","show_toast":true,"announce_to_chat":false,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/root`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable:1,axe:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/bows/blaze`

Источник: [lbc:true_advancements/bows/blaze](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/bows/blaze.json).
В дереве достижений: `lbc.adv.bow.blaze`; описание: `lbc.adv.bow.blaze.lore`. Параметры показа: `{"icon":{"id":"minecraft:crossbow","components":{"minecraft:enchantment_glint_override":true,"charged_projectiles":[{"id":"arrow","count":1}]}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/bows/minigun`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable: 1,bow: 1,fastfired: 1}"}}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/trinket](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/trinket.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/bows/double`

Источник: [lbc:true_advancements/bows/double](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/bows/double.json).
В дереве достижений: `lbc.adv.bow.double`; описание: `lbc.adv.bow.double.lore`. Параметры показа: `{"icon":{"id":"minecraft:bow","components":{"minecraft:item_model":"lbc:doublebow"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/bows/firework`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:enchantments":[{"enchantments":"minecraft:power","levels":{"min":2}}],"minecraft:custom_data":"{transformable:1,bow:1,double:1}"}}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/skill](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/skill.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/bows/firework`

Источник: [lbc:true_advancements/bows/firework](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/bows/firework.json).
В дереве достижений: `lbc.adv.bow.firework`; описание: `lbc.adv.bow.firework.lore`. Параметры показа: `{"icon":{"id":"minecraft:bow","components":{"minecraft:item_model":"lbc:firework"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/bows/randchanted`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Награда: выполнить [lbc:swrg_kit_integration/gain/kit](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/kit.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [workingitems/fireworktnt_lol.mcfunction:13](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/fireworktnt_lol.mcfunction) — `advancement grant @p[gamemode=!spectator,scores={bow=1..}] only lbc:true_advancements/bows/firework`.

## `lbc:true_advancements/bows/ghast`

Источник: [lbc:true_advancements/bows/ghast](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/bows/ghast.json).
В дереве достижений: `lbc.adv.bow.ghast`; описание: `lbc.adv.bow.ghast.lore`. Параметры показа: `{"icon":{"id":"minecraft:crossbow","components":{"minecraft:enchantment_glint_override":true,"charged_projectiles":[{"id":"firework_rocket","count":1}]}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/bows/minigun`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable:1,bow:1,fastexplode:1}"}}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/trinket](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/trinket.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/bows/madoka_bow`

Источник: [lbc:true_advancements/bows/madoka_bow](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/bows/madoka_bow.json).
В дереве достижений: `lbc.adv.bow.madoka_bow`; описание: `lbc.adv.bow.madoka_bow.lore`. Параметры показа: `{"icon":{"id":"minecraft:bow","components":{"minecraft:item_model":"lbc:madoka_bow"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/bows/railgun`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Награда: выполнить [lbc:swrg_kit_integration/gain/skill](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/skill.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [items/bows/kaname_bow/hit_adv.mcfunction:3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/bows/kaname_bow/hit_adv.mcfunction) — `advancement grant @s only lbc:true_advancements/bows/madoka_bow`.

## `lbc:true_advancements/bows/minigun`

Источник: [lbc:true_advancements/bows/minigun](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/bows/minigun.json).
В дереве достижений: `lbc.adv.bow.minigun`; описание: `lbc.adv.bow.minigun.lore`. Параметры показа: `{"icon":{"id":"minecraft:crossbow","components":{"minecraft:enchantment_glint_override":true}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/bows/double`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Награда: выполнить [lbc:swrg_kit_integration/gain/skill](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/skill.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [workingitems/fastbow.mcfunction:6](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/fastbow.mcfunction) — `execute if score @s adv_game_minigun_shooted matches 320.. run advancement grant @s only lbc:true_advancements/bows/minigun`.

## `lbc:true_advancements/bows/penis_gun`

Источник: [lbc:true_advancements/bows/penis_gun](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/bows/penis_gun.json).
В дереве достижений: `lbc.adv.bow.penis_gun`; описание: `lbc.adv.bow.penis_gun.lore`. Параметры показа: `{"icon":{"id":"minecraft:crossbow","components":{"minecraft:item_model":"lbc:dildomet/dildomet","charged_projectiles":[{"id":"arrow","count":1}]}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/bows/ghast`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable:1,bow:1,penisgun:1}"}}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/trinket](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/trinket.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/bows/railgun`

Источник: [lbc:true_advancements/bows/railgun](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/bows/railgun.json).
В дереве достижений: `lbc.adv.bow.railgun`; описание: `lbc.adv.bow.railgun.lore`. Параметры показа: `{"icon":{"id":"minecraft:bow","components":{"minecraft:item_model":"lbc:railgun_bow"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/bows/shulker`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Награда: выполнить [lbc:swrg_kit_integration/gain/levelup](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/levelup.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [workingitems/railgun_bow.mcfunction:16](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/railgun_bow.mcfunction) — `execute if score speedy lbc.math matches 300.. run advancement grant @s only lbc:true_advancements/bows/railgun`.

## `lbc:true_advancements/bows/railshish`

Источник: [lbc:true_advancements/bows/railshish](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/bows/railshish.json).
В дереве достижений: `lbc.adv.bow.railshish`; описание: `lbc.adv.bow.railshish.lore`. Параметры показа: `{"icon":{"id":"minecraft:bow","components":{"minecraft:item_model":"lbc:shishgun_bow"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/bows/railgun`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable:1,bow:1,shish_gun:1}"}}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/levelup](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/levelup.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/bows/randchanted`

Источник: [lbc:true_advancements/bows/randchanted](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/bows/randchanted.json).
В дереве достижений: `lbc.adv.bow.randchanted`; описание: `lbc.adv.bow.randchanted.lore`. Параметры показа: `{"icon":{"id":"minecraft:bow","components":{"minecraft:enchantment_glint_override":true}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/bows`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable: 1,bow: 1,randchanted: 1}"}}`) И (`{"predicates":{"minecraft:enchantments":[{"enchantments":"minecraft:infinity","levels":{"min":1}},{"enchantments":"minecraft:power","levels":{"min":1}},{"enchantments":"minecraft:unbreaking","levels":{"min":1}},{"enchantments":"minecraft:flame","levels":{"min":1}},{"enchantments":"minecraft:punch","levels":{"min":1}}]}}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/levelup](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/levelup.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/bows/shulker`

Источник: [lbc:true_advancements/bows/shulker](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/bows/shulker.json).
В дереве достижений: `lbc.adv.bow.shulker`; описание: `lbc.adv.bow.shulker.lore`. Параметры показа: `{"icon":{"id":"minecraft:bow","components":{"minecraft:item_model":"lbc:shulkerbow"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/bows/minigun`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок наносит урон сущности. Условия: игрок: (свойства `this`: `{"equipment":{"mainhand":{"predicates":{"minecraft:custom_data":"{transformable:1,bow:1,shulker_new:1}"}}}}`); сущность: (свойства `this`: `{"type":"minecraft:player","equipment":{"mainhand":{"predicates":{"minecraft:custom_data":"{transformable:1,bow:1,shulker_new:1}"}}}}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/levelup](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/levelup.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/bows`

Источник: [lbc:true_advancements/bows](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/bows.json).
В дереве достижений: `lbc.adv.bows`; описание: ``. Параметры показа: `{"icon":{"id":"minecraft:bow"},"frame":"task","show_toast":true,"announce_to_chat":false,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/root`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable:1,bow:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/event/eat_halloween_food`

Источник: [lbc:true_advancements/event/eat_halloween_food](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/event/eat_halloween_food.json).
В дереве достижений: `lbc.adv.event.eat_halloween_food`; описание: `lbc.adv.event.eat_halloween_food.lore`. Параметры показа: `{"icon":{"id":"minecraft:pumpkin_pie"},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/event/halloween`; это связь отображения, а не дополнительный критерий.
Критерий `1`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `2`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `3`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `4`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `5`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `6`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `7`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `8`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `9`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `10`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `11`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `12`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `13`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `14`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `15`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `16`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `17`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `18`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `19`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `20`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `21`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `22`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `23`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `24`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `25`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `26`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `27`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `28`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `29`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `30`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `31`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `32`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `33`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `34`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `35`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `36`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `37`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `38`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `39`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `40`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `41`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `42`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `43`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `44`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `45`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `46`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `47`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `48`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `49`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `50`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `51`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `52`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `53`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `54`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `55`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `56`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `57`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `58`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `59`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `60`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `61`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `62`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `63`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `64`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `65`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `66`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `67`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `68`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `69`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `70`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `71`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `72`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `73`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `74`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `75`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `76`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `77`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `78`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `79`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `80`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `81`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `82`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `83`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `84`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `85`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `86`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `87`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `88`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `89`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `90`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `91`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `92`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `93`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `94`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `95`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `96`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `97`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `98`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `99`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `100`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Для завершения требуются все 100 критериев.
Награда: выполнить [lbc:swrg_kit_integration/gain/default_kit](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/default_kit.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [other/event/halloween/eat_halloween_food.mcfunction:2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 1 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 1`; [other/event/halloween/eat_halloween_food.mcfunction:3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 2 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 2`; [other/event/halloween/eat_halloween_food.mcfunction:4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 3 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 3`; [other/event/halloween/eat_halloween_food.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 4 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 4`; [other/event/halloween/eat_halloween_food.mcfunction:6](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 5 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 5`; [other/event/halloween/eat_halloween_food.mcfunction:7](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 6 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 6`; [other/event/halloween/eat_halloween_food.mcfunction:8](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 7 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 7`; [other/event/halloween/eat_halloween_food.mcfunction:9](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 8 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 8`; [other/event/halloween/eat_halloween_food.mcfunction:10](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 9 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 9`; [other/event/halloween/eat_halloween_food.mcfunction:11](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 10 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 10`; [other/event/halloween/eat_halloween_food.mcfunction:12](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 11 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 11`; [other/event/halloween/eat_halloween_food.mcfunction:13](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 12 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 12`; [other/event/halloween/eat_halloween_food.mcfunction:14](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 13 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 13`; [other/event/halloween/eat_halloween_food.mcfunction:15](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 14 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 14`; [other/event/halloween/eat_halloween_food.mcfunction:16](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 15 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 15`; [other/event/halloween/eat_halloween_food.mcfunction:17](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 16 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 16`; [other/event/halloween/eat_halloween_food.mcfunction:18](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 17 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 17`; [other/event/halloween/eat_halloween_food.mcfunction:19](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 18 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 18`; [other/event/halloween/eat_halloween_food.mcfunction:20](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 19 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 19`; [other/event/halloween/eat_halloween_food.mcfunction:21](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 20 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 20`; [other/event/halloween/eat_halloween_food.mcfunction:22](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 21 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 21`; [other/event/halloween/eat_halloween_food.mcfunction:23](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 22 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 22`; [other/event/halloween/eat_halloween_food.mcfunction:24](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 23 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 23`; [other/event/halloween/eat_halloween_food.mcfunction:25](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 24 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 24`; [other/event/halloween/eat_halloween_food.mcfunction:26](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 25 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 25`; [other/event/halloween/eat_halloween_food.mcfunction:27](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 26 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 26`; [other/event/halloween/eat_halloween_food.mcfunction:28](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 27 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 27`; [other/event/halloween/eat_halloween_food.mcfunction:29](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 28 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 28`; [other/event/halloween/eat_halloween_food.mcfunction:30](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 29 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 29`; [other/event/halloween/eat_halloween_food.mcfunction:31](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 30 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 30`; [other/event/halloween/eat_halloween_food.mcfunction:32](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 31 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 31`; [other/event/halloween/eat_halloween_food.mcfunction:33](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 32 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 32`; [other/event/halloween/eat_halloween_food.mcfunction:34](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 33 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 33`; [other/event/halloween/eat_halloween_food.mcfunction:35](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 34 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 34`; [other/event/halloween/eat_halloween_food.mcfunction:36](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 35 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 35`; [other/event/halloween/eat_halloween_food.mcfunction:37](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 36 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 36`; [other/event/halloween/eat_halloween_food.mcfunction:38](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 37 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 37`; [other/event/halloween/eat_halloween_food.mcfunction:39](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 38 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 38`; [other/event/halloween/eat_halloween_food.mcfunction:40](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 39 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 39`; [other/event/halloween/eat_halloween_food.mcfunction:41](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 40 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 40`; [other/event/halloween/eat_halloween_food.mcfunction:42](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 41 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 41`; [other/event/halloween/eat_halloween_food.mcfunction:43](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 42 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 42`; [other/event/halloween/eat_halloween_food.mcfunction:44](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 43 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 43`; [other/event/halloween/eat_halloween_food.mcfunction:45](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 44 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 44`; [other/event/halloween/eat_halloween_food.mcfunction:46](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 45 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 45`; [other/event/halloween/eat_halloween_food.mcfunction:47](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 46 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 46`; [other/event/halloween/eat_halloween_food.mcfunction:48](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 47 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 47`; [other/event/halloween/eat_halloween_food.mcfunction:49](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 48 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 48`; [other/event/halloween/eat_halloween_food.mcfunction:50](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 49 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 49`; [other/event/halloween/eat_halloween_food.mcfunction:51](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 50 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 50`; [other/event/halloween/eat_halloween_food.mcfunction:52](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 51 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 51`; [other/event/halloween/eat_halloween_food.mcfunction:53](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 52 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 52`; [other/event/halloween/eat_halloween_food.mcfunction:54](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 53 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 53`; [other/event/halloween/eat_halloween_food.mcfunction:55](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 54 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 54`; [other/event/halloween/eat_halloween_food.mcfunction:56](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 55 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 55`; [other/event/halloween/eat_halloween_food.mcfunction:57](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 56 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 56`; [other/event/halloween/eat_halloween_food.mcfunction:58](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 57 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 57`; [other/event/halloween/eat_halloween_food.mcfunction:59](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 58 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 58`; [other/event/halloween/eat_halloween_food.mcfunction:60](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 59 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 59`; [other/event/halloween/eat_halloween_food.mcfunction:61](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 60 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 60`; [other/event/halloween/eat_halloween_food.mcfunction:62](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 61 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 61`; [other/event/halloween/eat_halloween_food.mcfunction:63](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 62 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 62`; [other/event/halloween/eat_halloween_food.mcfunction:64](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 63 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 63`; [other/event/halloween/eat_halloween_food.mcfunction:65](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 64 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 64`; [other/event/halloween/eat_halloween_food.mcfunction:66](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 65 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 65`; [other/event/halloween/eat_halloween_food.mcfunction:67](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 66 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 66`; [other/event/halloween/eat_halloween_food.mcfunction:68](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 67 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 67`; [other/event/halloween/eat_halloween_food.mcfunction:69](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 68 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 68`; [other/event/halloween/eat_halloween_food.mcfunction:70](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 69 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 69`; [other/event/halloween/eat_halloween_food.mcfunction:71](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 70 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 70`; [other/event/halloween/eat_halloween_food.mcfunction:72](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 71 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 71`; [other/event/halloween/eat_halloween_food.mcfunction:73](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 72 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 72`; [other/event/halloween/eat_halloween_food.mcfunction:74](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 73 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 73`; [other/event/halloween/eat_halloween_food.mcfunction:75](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 74 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 74`; [other/event/halloween/eat_halloween_food.mcfunction:76](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 75 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 75`; [other/event/halloween/eat_halloween_food.mcfunction:77](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 76 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 76`; [other/event/halloween/eat_halloween_food.mcfunction:78](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 77 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 77`; [other/event/halloween/eat_halloween_food.mcfunction:79](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 78 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 78`; [other/event/halloween/eat_halloween_food.mcfunction:80](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 79 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 79`; [other/event/halloween/eat_halloween_food.mcfunction:81](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 80 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 80`; [other/event/halloween/eat_halloween_food.mcfunction:82](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 81 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 81`; [other/event/halloween/eat_halloween_food.mcfunction:83](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 82 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 82`; [other/event/halloween/eat_halloween_food.mcfunction:84](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 83 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 83`; [other/event/halloween/eat_halloween_food.mcfunction:85](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 84 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 84`; [other/event/halloween/eat_halloween_food.mcfunction:86](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 85 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 85`; [other/event/halloween/eat_halloween_food.mcfunction:87](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 86 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 86`; [other/event/halloween/eat_halloween_food.mcfunction:88](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 87 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 87`; [other/event/halloween/eat_halloween_food.mcfunction:89](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 88 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 88`; [other/event/halloween/eat_halloween_food.mcfunction:90](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 89 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 89`; [other/event/halloween/eat_halloween_food.mcfunction:91](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 90 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 90`; [other/event/halloween/eat_halloween_food.mcfunction:92](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 91 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 91`; [other/event/halloween/eat_halloween_food.mcfunction:93](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 92 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 92`; [other/event/halloween/eat_halloween_food.mcfunction:94](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 93 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 93`; [other/event/halloween/eat_halloween_food.mcfunction:95](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 94 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 94`; [other/event/halloween/eat_halloween_food.mcfunction:96](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 95 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 95`; [other/event/halloween/eat_halloween_food.mcfunction:97](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 96 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 96`; [other/event/halloween/eat_halloween_food.mcfunction:98](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 97 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 97`; [other/event/halloween/eat_halloween_food.mcfunction:99](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 98 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 98`; [other/event/halloween/eat_halloween_food.mcfunction:100](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 99 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 99`; [other/event/halloween/eat_halloween_food.mcfunction:101](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/eat_halloween_food.mcfunction) — `execute if score @s pumpkin_counter matches 100 run return run advancement grant @s only lbc:true_advancements/event/eat_halloween_food 100`.

## `lbc:true_advancements/event/halloween`

Источник: [lbc:true_advancements/event/halloween](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/event/halloween.json).
В дереве достижений: `lbc.adv.event.halloween`; описание: `lbc.adv.event.halloween.lore`. Параметры показа: `{"icon":{"id":"minecraft:jack_o_lantern"},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/event/root`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [other/event/halloween/start.mcfunction:1](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/start.mcfunction) — `advancement grant @a only lbc:true_advancements/event/halloween`.

## `lbc:true_advancements/event/halloween_mace`

Источник: [lbc:true_advancements/event/halloween_mace](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/event/halloween_mace.json).
В дереве достижений: `lbc.adv.event.halloween_mace`; описание: `lbc.adv.event.halloween_mace.lore`. Параметры показа: `{"icon":{"id":"minecraft:mace","components":{"item_model":"lbc:halloween_mace"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/event/eat_halloween_food`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок убивает сущность. Условия: killing_blow: `{"is_direct":true}`; сущность: `{"type":"player"}`; игрок: `{"movement":{"fall_distance":{"min":10},"vertical_speed":{"min":10}},"equipment":{"mainhand":{"predicates":{"custom_data":"{halloween_mace:1}"}}}}`.
Награда: выполнить [lbc:swrg_kit_integration/gain/default_kit](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/default_kit.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/event/pumpkin_sword`

Источник: [lbc:true_advancements/event/pumpkin_sword](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/event/pumpkin_sword.json).
В дереве достижений: `lbc.adv.event.pumpkin_sword`; описание: `lbc.adv.event.pumpkin_sword.lore`. Параметры показа: `{"icon":{"id":"minecraft:mace","components":{"item_model":"lbc:pumpkin_sword"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/event/wind_wand`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок наносит урон сущности. Условия: сущность: `{"type":"player","equipment":{"mainhand":{"items":"minecraft:pumpkin_seeds"}}}`; игрок: `{"equipment":{"mainhand":{"predicates":{"custom_data":"{pumpkin_king:1}"}}}}`.
Награда: выполнить [lbc:swrg_kit_integration/gain/death_effect](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/death_effect.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/event/root`

Источник: [lbc:true_advancements/event/root](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/event/root.json).
В дереве достижений: `lbc.adv.event_root`; описание: `lbc.adv.event_root.lore`. Параметры показа: `{"icon":{"id":"minecraft:netherite_hoe","components":{"item_model":"lbc:advancement_icon/calendar"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/root`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/event/soulcorrupt_wand`

Источник: [lbc:true_advancements/event/soulcorrupt_wand](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/event/soulcorrupt_wand.json).
В дереве достижений: `lbc.adv.event.soulcorrupt_wand`; описание: `lbc.adv.event.soulcorrupt_wand.lore`. Параметры показа: `{"icon":{"id":"minecraft:mace","components":{"item_model":"lbc:soulcorruptwand"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/event/pumpkin_sword`; это связь отображения, а не дополнительный критерий.
Критерий `meta_1`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{metasola:1,type:1}"}}`).
Критерий `meta_2`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{metasola:1,type:2}"}}`).
Критерий `meta_3`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{metasola:1,type:3}"}}`).
Критерий `meta_4`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{metasola:1,type:4}"}}`).
Критерий `meta_5`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{metasola:1,type:5}"}}`).
Критерий `meta_6`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{metasola:1,type:6}"}}`).
Для завершения требуются все 6 критериев.
Награда: выполнить [lbc:swrg_kit_integration/gain/default_skill](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/default_skill.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/event/soulcorrupt_wand_upgraded`

Источник: [lbc:true_advancements/event/soulcorrupt_wand_upgraded](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/event/soulcorrupt_wand_upgraded.json).
В дереве достижений: `lbc.adv.event.upgraded_soulcorrupt_wand`; описание: `lbc.adv.event.upgraded_soulcorrupt_wand.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:soulcorruptwand"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/event/soulcorrupt_wand`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{upgraded:1,transformable:1,staff:1,soulcorrupter:1}"}}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/default_kill_effect](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/default_kill_effect.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/event/soulhunter_mace`

Источник: [lbc:true_advancements/event/soulhunter_mace](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/event/soulhunter_mace.json).
В дереве достижений: `lbc.adv.event.soulhunter_mace`; описание: `lbc.adv.event.soulhunter_mace.lore`. Параметры показа: `{"icon":{"id":"minecraft:mace","components":{"item_model":"lbc:soulhunter_mace"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/event/halloween_mace`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок убивает сущность. Условия: killing_blow: `{"is_direct":true}`; сущность: `{"type":"player"}`; игрок: `{"movement":{"fall_distance":{"min":10},"vertical_speed":{"min":10}},"equipment":{"mainhand":{"predicates":{"custom_data":"{soulhunter_mace:1}"}}}}`.
Награда: выполнить [lbc:swrg_kit_integration/gain/default_kill_effect](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/default_kill_effect.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/event/wind_wand`

Источник: [lbc:true_advancements/event/wind_wand](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/event/wind_wand.json).
В дереве достижений: `lbc.adv.event.vex_wand`; описание: `lbc.adv.event.vex_wand.lore`. Параметры показа: `{"icon":{"id":"minecraft:mace","components":{"item_model":"lbc:vex_wand"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/event/halloween_mace`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: проверка происходит каждый игровой тик. Условия: игрок: `{"movement":{"horizontal_speed":{"min":75}},"equipment":{"mainhand":{"predicates":{"custom_data":"{vex_wand:1}"}}}}`.
Награда: выполнить [lbc:swrg_kit_integration/gain/default_skill](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/default_skill.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/event/wind_wand_upgraded`

Источник: [lbc:true_advancements/event/wind_wand_upgraded](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/event/wind_wand_upgraded.json).
В дереве достижений: `lbc.adv.event.upgraded_wind_wand`; описание: `lbc.adv.event.upgraded_wind_wand.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:vex_wand"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/event/wind_wand`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{upgraded:1,transformable:1,staff:1,vex_wand:1}"}}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/default_kit](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/default_kit.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/hoes/death_skytche`

Источник: [lbc:true_advancements/hoes/death_skytche](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/hoes/death_skytche.json).
В дереве достижений: `lbc.adv.hoe.death_skytche`; описание: `lbc.adv.hoe.death_skytche.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:death_skythe"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/hoes/sickle`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Награда: выполнить [lbc:swrg_kit_integration/gain/skill](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/skill.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [raycast_preloop14.mcfunction:7](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_preloop14.mcfunction) — `advancement grant @s only lbc:true_advancements/hoes/death_skytche`.

## `lbc:true_advancements/hoes/op_hoe`

Источник: [lbc:true_advancements/hoes/op_hoe](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/hoes/op_hoe.json).
В дереве достижений: `lbc.adv.hoe.op_hoe`; описание: `lbc.adv.hoe.op_hoe.lore`. Параметры показа: `{"icon":{"id":"minecraft:netherite_hoe","components":{"minecraft:enchantment_glint_override":true}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/hoes`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок использует предмет на блоке. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)) И (свойства `this`: `{"equipment":{"mainhand":{"predicates":{"minecraft:custom_data":"{transformable:1,hoe:1,op_hoe:1}"}}}}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/kit](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/kit.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/hoes/sickle`

Источник: [lbc:true_advancements/hoes/sickle](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/hoes/sickle.json).
В дереве достижений: `lbc.adv.hoe.sickle`; описание: `lbc.adv.hoe.sickle.lore`. Параметры показа: `{"icon":{"id":"minecraft:iron_hoe","components":{"minecraft:item_model":"lbc:sickle"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/hoes/op_hoe`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок наносит урон сущности. Условия: игрок: (свойства `this`: `{"equipment":{"mainhand":{"predicates":{"minecraft:custom_data":"{transformable:1,hoe:1,sickle:1}"}}}}`); сущность: (свойства `this`: `{"type":"minecraft:player"}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/skill](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/skill.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/hoes/socializm`

Источник: [lbc:true_advancements/hoes/socializm](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/hoes/socializm.json).
В дереве достижений: `lbc.adv.hoe.communism`; описание: `lbc.adv.hoe.communism.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:communism"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/hoes/sickle`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{communism:1}"}}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/skill](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/skill.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/hoes`

Источник: [lbc:true_advancements/hoes](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/hoes.json).
В дереве достижений: `lbc.adv.hoes`; описание: ``. Параметры показа: `{"icon":{"id":"minecraft:golden_hoe"},"frame":"task","show_toast":true,"announce_to_chat":false,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/root`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable:1,hoe:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/absurd_child`

Источник: [lbc:true_advancements/legends/absurd_child](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/absurd_child.json).
В дереве достижений: `lbc.adv.legends.absurd_child`; описание: `lbc.adv.legends.absurd_child.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:advancement_icon/gigachad"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/witch_gens`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [other/absurd_child.mcfunction:3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/absurd_child.mcfunction) — `advancement grant @a[gamemode=!spectator,distance=..15] only lbc:true_advancements/legends/absurd_child`.

## `lbc:true_advancements/legends/academy/acid`

Источник: [lbc:true_advancements/legends/academy/acid](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/academy/acid.json).
В дереве достижений: `lbc.adv.upgraded_staff.acid`; описание: `lbc.adv.upgraded_staff.acid.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:acidwand"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/academy/tesla`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{upgraded:1,transformable:1,staff:1,acidwand:1}"}}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/trinket](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/trinket.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/academy/acid_tuntija`

Источник: [lbc:true_advancements/legends/academy/acid_tuntija](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/academy/acid_tuntija.json).
В дереве достижений: `lbc.adv.upgraded_tuntija.acid`; описание: `lbc.adv.upgraded_tuntija.acid.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:st/acid"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/academy/air`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{upgraded:1,transformable:1,staff:1,stacid:1}"}}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/kit](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/kit.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/academy/acidacid`

Источник: [lbc:true_advancements/legends/academy/acidacid](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/academy/acidacid.json).
В дереве достижений: `lbc.adv.upgraded_tuntija.acidacid`; описание: `lbc.adv.upgraded_tuntija.acidacid.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:acidacidwand"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/academy/acid_tuntija`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{upgraded:1,transformable:1,staff:1,acidacid_wand:1}"}}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/skill](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/skill.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/academy/acidcorrupt`

Источник: [lbc:true_advancements/legends/academy/acidcorrupt](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/academy/acidcorrupt.json).
В дереве достижений: `lbc.adv.upgraded_tuntija.acidcorrupt`; описание: `lbc.adv.upgraded_tuntija.acidcorrupt.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:acidcorrupt"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/academy/acid_tuntija`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{upgraded:1,transformable:1,staff:1,acidcorrupt_wand:1}"}}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/skill](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/skill.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/academy/air`

Источник: [lbc:true_advancements/legends/academy/air](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/academy/air.json).
В дереве достижений: `lbc.adv.upgraded_tuntija.air`; описание: `lbc.adv.upgraded_tuntija.air.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:st/air"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/academy/electro`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{upgraded:1,transformable:1,staff:1,stair:1}"}}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/trinket](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/trinket.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/academy/badwiki`

Источник: [lbc:true_advancements/legends/academy/badwiki](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/academy/badwiki.json).
В дереве достижений: `lbc.adv.upgraded_staff.badwiki`; описание: `lbc.adv.upgraded_staff.badwiki.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:badwiki"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/academy/blood`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{upgraded:1,transformable:1,staff:1,badwiki:1}"}}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/kit](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/kit.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/academy/bass`

Источник: [lbc:true_advancements/legends/academy/bass](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/academy/bass.json).
В дереве достижений: `lbc.adv.upgraded_staff.bass`; описание: `lbc.adv.upgraded_staff.bass.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:basswand"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/academy/evoker`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{upgraded:1,transformable:1,staff:1,bassboost:1}"}}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/trinket](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/trinket.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/academy/bedrock`

Источник: [lbc:true_advancements/legends/academy/bedrock](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/academy/bedrock.json).
В дереве достижений: `lbc.adv.upgraded_tuntija.bedrock`; описание: `lbc.adv.upgraded_tuntija.bedrock.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:bedrock_wand"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/academy/stone`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{upgraded:1,transformable:1,staff:1,bedrock_wand:1}"}}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/trinket](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/trinket.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/academy/big_stone`

Источник: [lbc:true_advancements/legends/academy/big_stone](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/academy/big_stone.json).
В дереве достижений: `lbc.adv.upgraded_tuntija.big_stone`; описание: `lbc.adv.upgraded_tuntija.big_stone.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:stonekivi"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/academy/stone`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{upgraded:1,transformable:1,staff:1,stonekivi_wand:1}"}}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/skill](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/skill.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/academy/blaze`

Источник: [lbc:true_advancements/legends/academy/blaze](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/academy/blaze.json).
В дереве достижений: `lbc.adv.upgraded_staff.blaze`; описание: `lbc.adv.upgraded_staff.blaze.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:blazewand"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/academy/prism`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{upgraded:1,transformable:1,staff:1,fallingstars:1}"}}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/trinket](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/trinket.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/academy/blazefire`

Источник: [lbc:true_advancements/legends/academy/blazefire](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/academy/blazefire.json).
В дереве достижений: `lbc.adv.upgraded_tuntija.blazefire`; описание: `lbc.adv.upgraded_tuntija.blazefire.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:blazefire"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/academy/fire`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{upgraded:1,transformable:1,staff:1,fireblaze_wand:1}"}}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/kit](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/kit.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/academy/blind`

Источник: [lbc:true_advancements/legends/academy/blind](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/academy/blind.json).
В дереве достижений: `lbc.adv.upgraded_tuntija.blind`; описание: `lbc.adv.upgraded_tuntija.blind.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:st/blind"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/academy/fire`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{upgraded:1,transformable:1,staff:1,stblind:1}"}}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/trinket](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/trinket.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/academy/blood`

Источник: [lbc:true_advancements/legends/academy/blood](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/academy/blood.json).
В дереве достижений: `lbc.adv.upgraded_staff.blood`; описание: `lbc.adv.upgraded_staff.blood.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:killingwand"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/academy/confuse`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{upgraded:1,transformable:1,staff:1,bloodmagic:1}"}}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/levelup](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/levelup.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/academy/boltbolt`

Источник: [lbc:true_advancements/legends/academy/boltbolt](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/academy/boltbolt.json).
В дереве достижений: `lbc.adv.upgraded_tuntija.boltbolt`; описание: `lbc.adv.upgraded_tuntija.boltbolt.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:boltbolt_wand"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/academy/teleport_tuntija`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{upgraded:1,transformable:1,staff:1,boltbolt_wand:1}"}}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/skill](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/skill.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/academy/confuse`

Источник: [lbc:true_advancements/legends/academy/confuse](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/academy/confuse.json).
В дереве достижений: `lbc.adv.upgraded_staff.confuse`; описание: `lbc.adv.upgraded_staff.confuse.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:foxuswand"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/academy/vampire`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{upgraded:1,transformable:1,staff:1,foxusing:1}"}}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/kit](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/kit.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/academy/consuming`

Источник: [lbc:true_advancements/legends/academy/consuming](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/academy/consuming.json).
В дереве достижений: `lbc.adv.upgraded_tuntija.consuming`; описание: `lbc.adv.upgraded_tuntija.consuming.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:consuming_wand"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/academy/ruby`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{upgraded:1,transformable:1,staff:1,consuming_wand:1}"}}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/skill](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/skill.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/academy/corruption`

Источник: [lbc:true_advancements/legends/academy/corruption](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/academy/corruption.json).
В дереве достижений: `lbc.adv.upgraded_staff.corruption`; описание: `lbc.adv.upgraded_staff.corruption.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:corruptwand"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/academy/bass`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{upgraded:1,transformable:1,staff:1,corrupter:1}"}}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/trinket](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/trinket.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/academy/danmaku`

Источник: [lbc:true_advancements/legends/academy/danmaku](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/academy/danmaku.json).
В дереве достижений: `lbc.adv.upgraded_staff.danmaku_staff`; описание: `lbc.adv.upgraded_staff.danmaku_staff.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:danmaku"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/academy/blind`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{upgraded:1,transformable:1,staff:1,danmaku_staff:1}"}}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/skill](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/skill.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/academy/dublicating`

Источник: [lbc:true_advancements/legends/academy/dublicating](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/academy/dublicating.json).
В дереве достижений: `lbc.adv.upgraded_tuntija.dublicating`; описание: `lbc.adv.upgraded_tuntija.dublicating.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:dublicating_wand"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/academy/ruby`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{upgraded:1,transformable:1,staff:1,dublicating_wand:1}"}}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/skill](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/skill.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/academy/electro`

Источник: [lbc:true_advancements/legends/academy/electro](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/academy/electro.json).
В дереве достижений: `lbc.adv.upgraded_tuntija.electro`; описание: `lbc.adv.upgraded_tuntija.electro.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:st/electro"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/academy/terra`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{upgraded:1,transformable:1,staff:1,stelectro:1}"}}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/trinket](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/trinket.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/academy/ender_random`

Источник: [lbc:true_advancements/legends/academy/ender_random](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/academy/ender_random.json).
В дереве достижений: `lbc.adv.upgraded_staff.ender_random`; описание: `lbc.adv.upgraded_staff.ender_random.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:enderwand"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/academy/corruption`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{upgraded:1,transformable:1,staff:1,enderwand:1}"}}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/trinket](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/trinket.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/academy/erecto`

Источник: [lbc:true_advancements/legends/academy/erecto](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/academy/erecto.json).
В дереве достижений: `lbc.adv.upgraded_tuntija.erecto`; описание: `lbc.adv.upgraded_tuntija.erecto.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:erecto"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/academy/electro`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{upgraded:1,transformable:1,staff:1,erecto_wand:1}"}}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/kit](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/kit.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/academy/evoker`

Источник: [lbc:true_advancements/legends/academy/evoker](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/academy/evoker.json).
В дереве достижений: `lbc.adv.upgraded_staff.evoker`; описание: `lbc.adv.upgraded_staff.evoker.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:evokerwand"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/academy/root`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{upgraded:1,transformable:1,staff:1,evocator:1}"}}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/trinket](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/trinket.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/academy/exahoming`

Источник: [lbc:true_advancements/legends/academy/exahoming](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/academy/exahoming.json).
В дереве достижений: `lbc.adv.upgraded_tuntija.exahoming`; описание: `lbc.adv.upgraded_tuntija.exahoming.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:homing"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/academy/air`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{upgraded:1,transformable:1,staff:1,exahoming_wand:1}"}}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/skill](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/skill.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/academy/exasperato`

Источник: [lbc:true_advancements/legends/academy/exasperato](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/academy/exasperato.json).
В дереве достижений: `lbc.adv.upgraded_tuntija.exasperato`; описание: `lbc.adv.upgraded_tuntija.exasperato.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:exasperato"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/academy/air`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{upgraded:1,transformable:1,staff:1,exasperato_wand:1}"}}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/kit](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/kit.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/academy/exoperso`

Источник: [lbc:true_advancements/legends/academy/exoperso](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/academy/exoperso.json).
В дереве достижений: `lbc.adv.upgraded_staff.exoperso`; описание: `lbc.adv.upgraded_staff.exoperso.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:exoperso"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/academy/lightning`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{upgraded:1,transformable:1,staff:1,exoperso:1}"}}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/trinket](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/trinket.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/academy/falling_stars`

Источник: [lbc:true_advancements/legends/academy/falling_stars](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/academy/falling_stars.json).
В дереве достижений: `lbc.adv.upgraded_staff.falling_stars`; описание: `lbc.adv.upgraded_staff.falling_stars.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:starwand"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/academy/teleport`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{upgraded:1,transformable:1,staff:1,truefallingstars:1}"}}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/trinket](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/trinket.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/academy/fartus_nadristailo`

Источник: [lbc:true_advancements/legends/academy/fartus_nadristailo](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/academy/fartus_nadristailo.json).
В дереве достижений: `lbc.adv.fartus_nadristailo_upgrade`; описание: `lbc.adv.fartus_nadristailo_upgrade.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:palma"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/academy/terra`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{fartus_nadristailo:1,upgraded:1}"}}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/kit](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/kit.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/academy/fire`

Источник: [lbc:true_advancements/legends/academy/fire](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/academy/fire.json).
В дереве достижений: `lbc.adv.upgraded_tuntija.fire`; описание: `lbc.adv.upgraded_tuntija.fire.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:st/fire"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/academy/stone`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{upgraded:1,transformable:1,staff:1,stfire:1}"}}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/skill](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/skill.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/academy/gaycaster`

Источник: [lbc:true_advancements/legends/academy/gaycaster](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/academy/gaycaster.json).
В дереве достижений: `lbc.adv.upgraded_staff.gaycaster`; описание: `lbc.adv.upgraded_staff.gaycaster.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:gaycaster"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/academy/prism`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{upgraded:1,transformable:1,staff:1,gaycaster:1}"}}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/skill](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/skill.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/academy/hedgehog`

Источник: [lbc:true_advancements/legends/academy/hedgehog](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/academy/hedgehog.json).
В дереве достижений: `lbc.adv.upgraded_staff.hedgehog`; описание: `lbc.adv.upgraded_staff.hedgehog.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:hedgehogwand"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/academy/unstable_book`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{upgraded:1,transformable:1,staff:1,baffledhedgehog:1}"}}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/trinket](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/trinket.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/academy/ice_wand`

Источник: [lbc:true_advancements/legends/academy/ice_wand](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/academy/ice_wand.json).
В дереве достижений: `lbc.adv.upgraded_staff.ice_wand`; описание: `lbc.adv.upgraded_staff.ice_wand.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:ice_wand"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/academy/neutral`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{upgraded:1,transformable:1,staff:1,ice_wand:1}"}}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/trinket](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/trinket.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/academy/lightning`

Источник: [lbc:true_advancements/legends/academy/lightning](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/academy/lightning.json).
В дереве достижений: `lbc.adv.upgraded_staff.lightning`; описание: `lbc.adv.upgraded_staff.lightning.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:lightningboltwand"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/academy/neutral`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{upgraded:1,transformable:1,staff:1,lightning_bolts:1}"}}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/kit](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/kit.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/academy/neutral`

Источник: [lbc:true_advancements/legends/academy/neutral](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/academy/neutral.json).
В дереве достижений: `lbc.adv.upgraded_staff.neutral`; описание: `lbc.adv.upgraded_staff.neutral.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:neutralwand"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/academy/omega_flag`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{upgraded:1,transformable:1,staff:1,neutralmage:1}"}}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/trinket](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/trinket.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/academy/omega_flag`

Источник: [lbc:true_advancements/legends/academy/omega_flag](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/academy/omega_flag.json).
В дереве достижений: `lbc.adv.upgraded_staff.omega_flag`; описание: `lbc.adv.upgraded_staff.omega_flag.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:omegaflag"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/academy/acid`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{upgraded:1,transformable:1,staff:1,omegaflag:1}"}}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/skill](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/skill.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/academy/polymorph`

Источник: [lbc:true_advancements/legends/academy/polymorph](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/academy/polymorph.json).
В дереве достижений: `lbc.adv.upgraded_staff.polymorph`; описание: `lbc.adv.upgraded_staff.polymorph.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:sheepwand"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/academy/hedgehog`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{upgraded:1,transformable:1,staff:1,sheepwand:1}"}}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/trinket](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/trinket.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/academy/prism`

Источник: [lbc:true_advancements/legends/academy/prism](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/academy/prism.json).
В дереве достижений: `lbc.adv.upgraded_staff.prism`; описание: `lbc.adv.upgraded_staff.prism.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:prism"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/academy/falling_stars`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{upgraded:1,transformable:1,staff:1,prisma:1}"}}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/trinket](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/trinket.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/academy/psycho`

Источник: [lbc:true_advancements/legends/academy/psycho](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/academy/psycho.json).
В дереве достижений: `lbc.adv.upgraded_staff.psycho`; описание: `lbc.adv.upgraded_staff.psycho.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:psychowand"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/academy/polymorph`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{upgraded:1,transformable:1,staff:1,psychowand:1}"}}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/trinket](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/trinket.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/academy/psychoblind`

Источник: [lbc:true_advancements/legends/academy/psychoblind](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/academy/psychoblind.json).
В дереве достижений: `lbc.adv.upgraded_tuntija.psychoblind`; описание: `lbc.adv.upgraded_tuntija.psychoblind.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:psychoblind"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/academy/blind`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{upgraded:1,transformable:1,staff:1,psychoblind_wand:1}"}}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/skill](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/skill.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/academy/root`

Источник: [lbc:true_advancements/legends/academy/root](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/academy/root.json).
В дереве достижений: `lbc.adv.academy_upgraded`; описание: `lbc.adv.academy_upgraded.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:advancement_icon/academy"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/root`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{upgraded:1}"}}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/challenge](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/challenge.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/academy/ruby`

Источник: [lbc:true_advancements/legends/academy/ruby](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/academy/ruby.json).
В дереве достижений: `lbc.adv.upgraded_tuntija.ruby`; описание: `lbc.adv.upgraded_tuntija.ruby.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:st/ruby"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/academy/exoperso`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{upgraded:1,transformable:1,staff:1,struby:1}"}}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/kit](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/kit.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/academy/sauvojen_tuntija_wand`

Источник: [lbc:true_advancements/legends/academy/sauvojen_tuntija_wand](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/academy/sauvojen_tuntija_wand.json).
В дереве достижений: `lbc.adv.upgraded.sauvojen_tuntija_wand`; описание: `lbc.adv.upgraded.sauvojen_tuntija_wand.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:sauvojen_wand"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/academy/danmaku`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{upgraded:1,transformable:1,staff:1,sauvojen_wand:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/academy/snow_wand`

Источник: [lbc:true_advancements/legends/academy/snow_wand](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/academy/snow_wand.json).
В дереве достижений: `lbc.adv.upgraded_staff.snow_wand`; описание: `lbc.adv.upgraded_staff.snow_wand.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:snow_wand"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/academy/sauvojen_tuntija_wand`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{upgraded:1,transformable:1,staff:1,snow_wand:1}"}}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/trinket](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/trinket.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/academy/stone`

Источник: [lbc:true_advancements/legends/academy/stone](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/academy/stone.json).
В дереве достижений: `lbc.adv.upgraded_tuntija.stone`; описание: `lbc.adv.upgraded_tuntija.stone.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:st/stone"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/academy/teleport_tuntija`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{upgraded:1,transformable:1,staff:1,ststone:1}"}}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/skill](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/skill.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/academy/teleport`

Источник: [lbc:true_advancements/legends/academy/teleport](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/academy/teleport.json).
В дереве достижений: `lbc.adv.upgraded_staff.teleport`; описание: `lbc.adv.upgraded_staff.teleport.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:advancedenderwand"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/academy/wall`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{upgraded:1,transformable:1,staff:1,advancedteleport:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/academy/teleport_tuntija`

Источник: [lbc:true_advancements/legends/academy/teleport_tuntija](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/academy/teleport_tuntija.json).
В дереве достижений: `lbc.adv.upgraded_tuntija.teleport`; описание: `lbc.adv.upgraded_tuntija.teleport.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:st/teleport"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/academy/acid_tuntija`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{upgraded:1,transformable:1,staff:1,stteleport:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/academy/terra`

Источник: [lbc:true_advancements/legends/academy/terra](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/academy/terra.json).
В дереве достижений: `lbc.adv.upgraded_tuntija.terra`; описание: `lbc.adv.upgraded_tuntija.terra.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:st/terra"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/academy/ruby`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{upgraded:1,transformable:1,staff:1,stterra:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/academy/tesla`

Источник: [lbc:true_advancements/legends/academy/tesla](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/academy/tesla.json).
В дереве достижений: `lbc.adv.upgraded_staff.tesla`; описание: `lbc.adv.upgraded_staff.tesla.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:lightningwand"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/academy/psycho`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{upgraded:1,transformable:1,staff:1,lightningwand:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/academy/unstable_book`

Источник: [lbc:true_advancements/legends/academy/unstable_book](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/academy/unstable_book.json).
В дереве достижений: `lbc.adv.upgraded_staff.unstable_book`; описание: `lbc.adv.upgraded_staff.unstable_book.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:unstable_spellbook"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/academy/blaze`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{upgraded:1,transformable:1,staff:1,unstable_spellbook:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/academy/vampire`

Источник: [lbc:true_advancements/legends/academy/vampire](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/academy/vampire.json).
В дереве достижений: `lbc.adv.upgraded_staff.vampire`; описание: `lbc.adv.upgraded_staff.vampire.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:vampirewand"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/academy/ender_random`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{upgraded:1,transformable:1,staff:1,lifestealer:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/academy/wall`

Источник: [lbc:true_advancements/legends/academy/wall](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/academy/wall.json).
В дереве достижений: `lbc.adv.upgraded_staff.wall`; описание: `lbc.adv.upgraded_staff.wall.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:wallwand"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/academy/badwiki`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{upgraded:1,transformable:1,staff:1,wall:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/all_grimoires`

Источник: [lbc:true_advancements/legends/all_grimoires](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/all_grimoires.json).
В дереве достижений: `lbc.adv.legends.all_grimoires`; описание: `lbc.adv.legends.all_grimoires.lore`. Параметры показа: `{"icon":{"id":"minecraft:snowball","components":{"minecraft:item_model":"lbc:spelllib/random"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/enter_grimoire_dungeon`; это связь отображения, а не дополнительный критерий.
Критерий `spell_1`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 1}"}}`).
Критерий `spell_2`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 2}"}}`).
Критерий `spell_3`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 3}"}}`).
Критерий `spell_4`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 4}"}}`).
Критерий `spell_5`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 5}"}}`).
Критерий `spell_6`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 6}"}}`).
Критерий `spell_7`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 7}"}}`).
Критерий `spell_8`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 8}"}}`).
Критерий `spell_9`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 9}"}}`).
Критерий `spell_10`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 10}"}}`).
Критерий `spell_11`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 11}"}}`).
Критерий `spell_12`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 12}"}}`).
Критерий `spell_13`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 13}"}}`).
Критерий `spell_14`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 14}"}}`).
Критерий `spell_15`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 15}"}}`).
Критерий `spell_16`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 16}"}}`).
Критерий `spell_17`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 17}"}}`).
Критерий `spell_18`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 18}"}}`).
Критерий `spell_19`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 19}"}}`).
Критерий `spell_20`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 20}"}}`).
Критерий `spell_21`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 21}"}}`).
Критерий `spell_22`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 22}"}}`).
Критерий `spell_23`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 23}"}}`).
Критерий `spell_24`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 24}"}}`).
Критерий `spell_25`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 25}"}}`).
Критерий `spell_26`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 26}"}}`).
Критерий `spell_27`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 27}"}}`).
Критерий `spell_28`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 28}"}}`).
Критерий `spell_29`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 29}"}}`).
Критерий `spell_30`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 30}"}}`).
Критерий `spell_31`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 31}"}}`).
Критерий `spell_32`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 32}"}}`).
Критерий `spell_33`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 33}"}}`).
Критерий `spell_34`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 34}"}}`).
Критерий `spell_35`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 35}"}}`).
Критерий `spell_36`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 36}"}}`).
Критерий `spell_37`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 37}"}}`).
Критерий `spell_38`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 38}"}}`).
Критерий `spell_39`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 39}"}}`).
Критерий `spell_40`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 40}"}}`).
Критерий `spell_41`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 41}"}}`).
Критерий `spell_42`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 42}"}}`).
Критерий `spell_43`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 43}"}}`).
Критерий `spell_44`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 44}"}}`).
Критерий `spell_45`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 45}"}}`).
Критерий `spell_46`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 46}"}}`).
Критерий `spell_47`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 47}"}}`).
Критерий `spell_48`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 48}"}}`).
Критерий `spell_49`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 49}"}}`).
Критерий `spell_50`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 50}"}}`).
Критерий `spell_51`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 51}"}}`).
Критерий `spell_52`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 52}"}}`).
Критерий `spell_53`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 53}"}}`).
Критерий `spell_54`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 54}"}}`).
Критерий `spell_55`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 55}"}}`).
Критерий `spell_56`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 56}"}}`).
Критерий `spell_57`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 57}"}}`).
Критерий `spell_58`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 58}"}}`).
Критерий `spell_59`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 59}"}}`).
Критерий `spell_60`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 60}"}}`).
Критерий `spell_61`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 61}"}}`).
Критерий `spell_62`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 62}"}}`).
Критерий `spell_63`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 63}"}}`).
Критерий `spell_64`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 64}"}}`).
Критерий `spell_65`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 65}"}}`).
Критерий `spell_66`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 66}"}}`).
Критерий `spell_67`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 67}"}}`).
Критерий `spell_68`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 68}"}}`).
Критерий `spell_69`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 69}"}}`).
Критерий `spell_70`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 70}"}}`).
Критерий `spell_71`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 71}"}}`).
Критерий `spell_72`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 72}"}}`).
Критерий `spell_73`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 73}"}}`).
Критерий `spell_74`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 74}"}}`).
Критерий `spell_75`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 75}"}}`).
Критерий `spell_76`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 76}"}}`).
Критерий `spell_77`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 77}"}}`).
Критерий `spell_78`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 78}"}}`).
Критерий `spell_79`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 79}"}}`).
Критерий `spell_80`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 80}"}}`).
Критерий `spell_81`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 81}"}}`).
Критерий `spell_82`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 82}"}}`).
Критерий `spell_83`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 83}"}}`).
Критерий `spell_84`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 84}"}}`).
Критерий `spell_85`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 85}"}}`).
Критерий `spell_86`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 86}"}}`).
Критерий `spell_87`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 87}"}}`).
Критерий `spell_88`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 88}"}}`).
Критерий `spell_89`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 89}"}}`).
Критерий `spell_90`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 90}"}}`).
Критерий `spell_91`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 91}"}}`).
Критерий `spell_92`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 92}"}}`).
Критерий `spell_93`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 93}"}}`).
Критерий `spell_94`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 94}"}}`).
Критерий `spell_95`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 95}"}}`).
Критерий `spell_96`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 96}"}}`).
Критерий `spell_97`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 97}"}}`).
Критерий `spell_98`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 98}"}}`).
Критерий `spell_99`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 99}"}}`).
Критерий `spell_100`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 100}"}}`).
Критерий `spell_101`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 101}"}}`).
Критерий `spell_102`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 102}"}}`).
Критерий `spell_103`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 103}"}}`).
Критерий `spell_104`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 104}"}}`).
Критерий `spell_105`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 105}"}}`).
Критерий `spell_106`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 106}"}}`).
Критерий `spell_107`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 107}"}}`).
Критерий `spell_108`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 108}"}}`).
Критерий `spell_109`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 109}"}}`).
Критерий `spell_110`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 110}"}}`).
Критерий `spell_111`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 111}"}}`).
Критерий `spell_112`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 112}"}}`).
Критерий `spell_113`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 113}"}}`).
Критерий `spell_114`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 114}"}}`).
Критерий `spell_115`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 115}"}}`).
Критерий `spell_116`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 116}"}}`).
Критерий `spell_117`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 117}"}}`).
Критерий `spell_118`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 118}"}}`).
Критерий `spell_119`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 119}"}}`).
Критерий `spell_120`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 120}"}}`).
Критерий `spell_121`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 121}"}}`).
Критерий `spell_122`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 122}"}}`).
Критерий `spell_123`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 123}"}}`).
Критерий `spell_124`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 124}"}}`).
Критерий `spell_125`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 125}"}}`).
Критерий `spell_126`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 126}"}}`).
Критерий `spell_127`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 127}"}}`).
Критерий `spell_128`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 128}"}}`).
Критерий `spell_129`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 129}"}}`).
Критерий `spell_130`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 130}"}}`).
Критерий `spell_131`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 131}"}}`).
Критерий `spell_132`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 132}"}}`).
Критерий `spell_133`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 133}"}}`).
Критерий `spell_134`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 134}"}}`).
Критерий `spell_135`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 135}"}}`).
Критерий `spell_136`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 136}"}}`).
Критерий `spell_137`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 137}"}}`).
Критерий `spell_138`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 138}"}}`).
Критерий `spell_139`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 139}"}}`).
Критерий `spell_140`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 140}"}}`).
Критерий `spell_141`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 141}"}}`).
Критерий `spell_142`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 142}"}}`).
Критерий `spell_143`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 143}"}}`).
Критерий `spell_144`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 144}"}}`).
Критерий `spell_145`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 145}"}}`).
Критерий `spell_146`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 146}"}}`).
Критерий `spell_147`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 147}"}}`).
Критерий `spell_148`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 148}"}}`).
Критерий `spell_149`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 149}"}}`).
Критерий `spell_150`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 150}"}}`).
Критерий `spell_151`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 151}"}}`).
Критерий `spell_152`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 152}"}}`).
Критерий `spell_153`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 153}"}}`).
Критерий `spell_154`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 154}"}}`).
Критерий `spell_155`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 155}"}}`).
Критерий `spell_156`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 156}"}}`).
Критерий `spell_157`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 157}"}}`).
Критерий `spell_158`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 158}"}}`).
Критерий `spell_159`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 159}"}}`).
Критерий `spell_160`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 160}"}}`).
Критерий `spell_161`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 161}"}}`).
Критерий `spell_162`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 162}"}}`).
Критерий `spell_163`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 163}"}}`).
Критерий `spell_164`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 164}"}}`).
Критерий `spell_165`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 165}"}}`).
Критерий `spell_166`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 166}"}}`).
Критерий `spell_167`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 167}"}}`).
Критерий `spell_168`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 168}"}}`).
Критерий `spell_169`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 169}"}}`).
Критерий `spell_170`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 170}"}}`).
Критерий `spell_171`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 171}"}}`).
Критерий `spell_172`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 172}"}}`).
Критерий `spell_173`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 173}"}}`).
Критерий `spell_174`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 174}"}}`).
Критерий `spell_175`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 175}"}}`).
Критерий `spell_176`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 176}"}}`).
Критерий `spell_177`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 177}"}}`).
Критерий `spell_178`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 178}"}}`).
Критерий `spell_179`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 179}"}}`).
Критерий `spell_180`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 180}"}}`).
Критерий `spell_181`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 181}"}}`).
Критерий `spell_182`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 182}"}}`).
Критерий `spell_183`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 183}"}}`).
Критерий `spell_184`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 184}"}}`).
Критерий `spell_185`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 185}"}}`).
Критерий `spell_186`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 186}"}}`).
Критерий `spell_187`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 187}"}}`).
Критерий `spell_188`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 188}"}}`).
Критерий `spell_189`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 189}"}}`).
Критерий `spell_190`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 190}"}}`).
Критерий `spell_191`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 191}"}}`).
Критерий `spell_192`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 192}"}}`).
Критерий `spell_193`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 193}"}}`).
Критерий `spell_194`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 194}"}}`).
Критерий `spell_195`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 195}"}}`).
Критерий `spell_196`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 196}"}}`).
Критерий `spell_197`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 197}"}}`).
Критерий `spell_198`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 198}"}}`).
Критерий `spell_199`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 199}"}}`).
Критерий `spell_200`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 200}"}}`).
Критерий `spell_201`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 201}"}}`).
Критерий `spell_202`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 202}"}}`).
Критерий `spell_203`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 203}"}}`).
Критерий `spell_204`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 204}"}}`).
Критерий `spell_205`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 205}"}}`).
Критерий `spell_206`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 206}"}}`).
Критерий `spell_207`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 207}"}}`).
Критерий `spell_208`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 208}"}}`).
Критерий `spell_209`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 209}"}}`).
Критерий `spell_210`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 210}"}}`).
Критерий `spell_211`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 211}"}}`).
Критерий `spell_212`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 212}"}}`).
Критерий `spell_213`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 213}"}}`).
Критерий `spell_214`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 214}"}}`).
Критерий `spell_215`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 215}"}}`).
Критерий `spell_216`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 216}"}}`).
Критерий `spell_217`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 217}"}}`).
Критерий `spell_218`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 218}"}}`).
Критерий `spell_219`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 219}"}}`).
Критерий `spell_220`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 220}"}}`).
Критерий `spell_221`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 221}"}}`).
Критерий `spell_222`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 222}"}}`).
Критерий `spell_223`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 223}"}}`).
Критерий `spell_224`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 224}"}}`).
Критерий `spell_225`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 225}"}}`).
Критерий `spell_226`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 226}"}}`).
Критерий `spell_227`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 227}"}}`).
Критерий `spell_228`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 228}"}}`).
Критерий `spell_229`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 229}"}}`).
Критерий `spell_230`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 230}"}}`).
Критерий `spell_231`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 231}"}}`).
Критерий `spell_232`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 232}"}}`).
Критерий `spell_233`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 233}"}}`).
Критерий `spell_234`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 234}"}}`).
Критерий `spell_235`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 235}"}}`).
Критерий `spell_236`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 236}"}}`).
Критерий `spell_237`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 237}"}}`).
Критерий `spell_238`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 238}"}}`).
Критерий `spell_239`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 239}"}}`).
Критерий `spell_240`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 240}"}}`).
Критерий `spell_241`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{spellid: 241}"}}`).
Для завершения требуются все 241 критериев.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/avarice_diamond`

Источник: [lbc:true_advancements/legends/avarice_diamond](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/avarice_diamond.json).
В дереве достижений: `lbc.adv.legends.avarice_diamond`; описание: `lbc.adv.legends.avarice_diamond.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:avarice_diamond"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/supernova`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [other/greed/avarice_diamond_near1.mcfunction:2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/greed/avarice_diamond_near1.mcfunction) — `advancement grant @a[gamemode=!spectator,distance=..16] only lbc:true_advancements/legends/avarice_diamond`.

## `lbc:true_advancements/legends/avarice_divider`

Источник: [lbc:true_advancements/legends/avarice_divider](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/avarice_divider.json).
В дереве достижений: `lbc.adv.legends.avarice_divider`; описание: `lbc.adv.legends.avarice_divider.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:divider"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/avarice_diamond`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{greed_greed:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/avarice_feather`

Источник: [lbc:true_advancements/legends/avarice_feather](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/avarice_feather.json).
В дереве достижений: `lbc.adv.legends.avarice_feather`; описание: `lbc.adv.legends.avarice_feather.lore`. Параметры показа: `{"icon":{"id":"minecraft:feather","components":{"minecraft:enchantment_glint_override":true}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/avarice_diamond`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{feather_greed:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/end_of_baffled`

Источник: [lbc:true_advancements/legends/end_of_baffled](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/end_of_baffled.json).
В дереве достижений: `lbc.adv.legends.end_of_baffled`; описание: `lbc.adv.legends.end_of_baffled.lore`. Параметры показа: `{"icon":{"id":"minecraft:snowball","components":{"minecraft:item_model":"lbc:baffled_of_everything"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/end_of_everything`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [other/baffledend/8.mcfunction:8](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/baffledend/8.mcfunction) — `advancement grant @a[gamemode=!spectator] only lbc:true_advancements/legends/end_of_baffled`.

## `lbc:true_advancements/legends/end_of_everything`

Источник: [lbc:true_advancements/legends/end_of_everything](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/end_of_everything.json).
В дереве достижений: `lbc.adv.legends.end_of_everything`; описание: `lbc.adv.legends.end_of_everything.lore`. Параметры показа: `{"icon":{"id":"minecraft:snowball","components":{"minecraft:item_model":"lbc:end_of_everything"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/root`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [other/everythingend/58.mcfunction:1](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/everythingend/58.mcfunction) — `advancement grant @a[gamemode=!spectator] only lbc:true_advancements/legends/end_of_everything`.

## `lbc:true_advancements/legends/end_of_technomagic`

Источник: [lbc:true_advancements/legends/end_of_technomagic](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/end_of_technomagic.json).
В дереве достижений: `lbc.adv.legends.shish_of_everything`; описание: `lbc.adv.legends.shish_of_everything.lore`. Параметры показа: `{"icon":{"id":"minecraft:snowball","components":{"minecraft:item_model":"lbc:shish_of_everything"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/end_of_everything`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [other/shishend/11.mcfunction:4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/shishend/11.mcfunction) — `advancement grant @a[gamemode=!spectator] only lbc:true_advancements/legends/end_of_technomagic`.

## `lbc:true_advancements/legends/enter_grimoire_dungeon`

Источник: [lbc:true_advancements/legends/enter_grimoire_dungeon](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/enter_grimoire_dungeon.json).
В дереве достижений: `lbc.adv.legends.enter_grimoire_dungeon`; описание: `lbc.adv.legends.enter_grimoire_dungeon.lore`. Параметры показа: `{"icon":{"id":"minecraft:chiseled_tuff_bricks"},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/kill_technoshish`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [other/grimoire_dungeon/room_reward_4.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/room_reward_4.mcfunction) — `advancement grant @a[gamemode=!spectator,distance=..15] only lbc:true_advancements/legends/enter_grimoire_dungeon`; [other/grimoire_dungeon/room_reward_4_1.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/room_reward_4_1.mcfunction) — `advancement grant @a[gamemode=!spectator,distance=..15] only lbc:true_advancements/legends/enter_grimoire_dungeon`.

## `lbc:true_advancements/legends/hohlostaff`

Источник: [lbc:true_advancements/legends/hohlostaff](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/hohlostaff.json).
В дереве достижений: `lbc.adv.legends.hohlostaff`; описание: `lbc.adv.legends.hohlostaff.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:hohlopitek"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/infinity`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{hohlopitek:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/imitator_shulker`

Источник: [lbc:true_advancements/legends/imitator_shulker](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/imitator_shulker.json).
В дереве достижений: `lbc.adv.legends.imitator_shulker`; описание: `lbc.adv.legends.imitator_shulker.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:imitator","minecraft:enchantment_glint_override":true}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/avarice_diamond`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{copypaster:1}"},"count":{"min":64}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/infinity`

Источник: [lbc:true_advancements/legends/infinity](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/infinity.json).
В дереве достижений: `lbc.adv.legends.infinity`; описание: `lbc.adv.legends.infinity.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:infinity"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/kill_fat_skeleton`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [other/instants/random_infinity.mcfunction:62](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/instants/random_infinity.mcfunction) — `advancement grant @s only lbc:true_advancements/legends/infinity`.

## `lbc:true_advancements/legends/infinity_infinity_shulker`

Источник: [lbc:true_advancements/legends/infinity_infinity_shulker](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/infinity_infinity_shulker.json).
В дереве достижений: `lbc.adv.legends.infinity_infinity_shulker`; описание: `lbc.adv.legends.infinity_infinity_shulker.lore`. Параметры показа: `{"icon":{"id":"minecraft:black_shulker_box"},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/kill_button`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{inf_inf_shulker:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/infinity_sigma_aleph_gay_porno_cardinal_ordinal_shulker`

Источник: [lbc:true_advancements/legends/infinity_sigma_aleph_gay_porno_cardinal_ordinal_shulker](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/infinity_sigma_aleph_gay_porno_cardinal_ordinal_shulker.json).
В дереве достижений: `lbc.adv.legends.sigma_shulker`; описание: `lbc.adv.legends.sigma_shulker.lore`. Параметры показа: `{"icon":{"id":"minecraft:yellow_shulker_box"},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/infinity_infinity_shulker`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{sigma_shulker:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/kill_aimbob`

Источник: [lbc:true_advancements/legends/kill_aimbob](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/kill_aimbob.json).
В дереве достижений: `lbc.adv.legends.kill_aimbob`; описание: `lbc.adv.legends.kill_aimbob.lore`. Параметры показа: `{"icon":{"id":"minecraft:golden_apple","components":{"minecraft:item_model":"lbc:aimbobapple"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/kill_bob`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок убивает сущность. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); сущность: (свойства `this`: `{"type":"minecraft:skeleton","nbt":"{Tags:[\"aimbob\"]}"}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/kill_bob`

Источник: [lbc:true_advancements/legends/kill_bob](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/kill_bob.json).
В дереве достижений: `lbc.adv.legends.kill_bob`; описание: `lbc.adv.legends.kill_bob.lore`. Параметры показа: `{"icon":{"id":"minecraft:netherite_chestplate","components":{"minecraft:enchantment_glint_override":true}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/end_of_baffled`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок убивает сущность. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); сущность: (свойства `this`: `{"type":"minecraft:zombie","nbt":"{Tags:[\"bob\"]}"}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/kill_button`

Источник: [lbc:true_advancements/legends/kill_button](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/kill_button.json).
В дереве достижений: `lbc.adv.legends.kill_button`; описание: `lbc.adv.legends.kill_button.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:kill_button"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/enter_grimoire_dungeon`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [other/kill_rand_hihihiha.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/kill_rand_hihihiha.mcfunction) — `execute if score #random7 lbc.math matches 7 run advancement grant @s only lbc:true_advancements/legends/kill_button`.

## `lbc:true_advancements/legends/kill_fat_skeleton`

Источник: [lbc:true_advancements/legends/kill_fat_skeleton](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/kill_fat_skeleton.json).
В дереве достижений: `lbc.adv.legends.kill_fat_skeleton`; описание: `lbc.adv.legends.kill_fat_skeleton.lore`. Параметры показа: `{"icon":{"id":"minecraft:bone","components":{"minecraft:enchantment_glint_override":true}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/kill_aimbob`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок убивает сущность. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); сущность: (свойства `this`: `{"type":"minecraft:skeleton","nbt":"{Tags:[\"fat_skeleton\"]}"}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/kill_gold_skeleton`

Источник: [lbc:true_advancements/legends/kill_gold_skeleton](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/kill_gold_skeleton.json).
В дереве достижений: `lbc.adv.legends.kill_golden_skeleton`; описание: `lbc.adv.legends.kill_golden_skeleton.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:triple_golden_sword"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/enter_grimoire_dungeon`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок убивает сущность. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); сущность: (свойства `this`: `{"type":"minecraft:skeleton","nbt":"{Tags:[\"golden_skeleton\"]}"}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/kill_grimoire_boss`

Источник: [lbc:true_advancements/legends/kill_grimoire_boss](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/kill_grimoire_boss.json).
В дереве достижений: `lbc.adv.legends.kill_grimoire_boss`; описание: `lbc.adv.legends.kill_grimoire_boss.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:grimoire_boss"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/enter_grimoire_dungeon`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок убивает сущность. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); сущность: (свойства `this`: `{"type":"minecraft:warden","nbt":"{Tags:[\"grimoire_boss_hitbox\"]}"}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/kill_technoshish`

Источник: [lbc:true_advancements/legends/kill_technoshish](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/kill_technoshish.json).
В дереве достижений: `lbc.adv.legends.kill_technoshish`; описание: `lbc.adv.legends.kill_technoshish.lore`. Параметры показа: `{"icon":{"id":"minecraft:panda_spawn_egg","components":{"minecraft:item_model":"lbc:supply_beacon"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/absurd_child`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок убивает сущность. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); сущность: (свойства `this`: `{"type":"minecraft:skeleton","nbt":"{Tags:[\"cyborg_boss\"]}"}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/kill_tiny_skeleton`

Источник: [lbc:true_advancements/legends/kill_tiny_skeleton](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/kill_tiny_skeleton.json).
В дереве достижений: `lbc.adv.legends.kill_tiny_skeleton`; описание: `lbc.adv.legends.kill_tiny_skeleton.lore`. Параметры показа: `{"icon":{"id":"minecraft:golden_sword","components":{"minecraft:item_model":"lbc:tiny_bone"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/enter_grimoire_dungeon`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок убивает сущность. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); сущность: (свойства `this`: `{"type":"minecraft:skeleton","nbt":"{Tags:[\"tiny_skeleton\"]}"}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/mellstroy_mask`

Источник: [lbc:true_advancements/legends/mellstroy_mask](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/mellstroy_mask.json).
В дереве достижений: `lbc.adv.legends.mellstroy_mask`; описание: `lbc.adv.legends.mellstroy_mask.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:mellstroy_mask"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/trollge_all_masks`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [other/craft/mellstroy_mask.mcfunction:3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/mellstroy_mask.mcfunction) — `advancement grant @p[gamemode=!spectator,distance=..14] only lbc:true_advancements/legends/mellstroy_mask`; [other/craft/mellstroy_mask_inv.mcfunction:12](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/mellstroy_mask_inv.mcfunction) — `advancement grant @s only lbc:true_advancements/legends/mellstroy_mask`.

## `lbc:true_advancements/legends/new_world`

Источник: [lbc:true_advancements/legends/new_world](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/new_world.json).
В дереве достижений: `lbc.adv.legends.new_world`; описание: `lbc.adv.legends.new_world.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:balance"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/supernova`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [other/new_game/click.mcfunction:143](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/new_game/click.mcfunction) — `advancement grant @a[gamemode=!spectator] only lbc:true_advancements/legends/new_world`.

## `lbc:true_advancements/legends/overheaven`

Источник: [lbc:true_advancements/legends/overheaven](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/overheaven.json).
В дереве достижений: `lbc.adv.legends.overheaven`; описание: `lbc.adv.legends.overheaven.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:overheaven"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/stand_clocks`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [other/stand_arrow/stand_clocks_overheaven_upgrade.mcfunction:4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/stand_arrow/stand_clocks_overheaven_upgrade.mcfunction) — `advancement grant @s only lbc:true_advancements/legends/overheaven`.

## `lbc:true_advancements/legends/pipe_divider`

Источник: [lbc:true_advancements/legends/pipe_divider](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/pipe_divider.json).
В дереве достижений: `lbc.adv.legends.pipe_divider`; описание: `lbc.adv.legends.pipe_divider.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:metal_pipe_divider"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/avarice_divider`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{divider_by_metal_pipe:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/root`

Источник: [lbc:true_advancements/legends/root](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/root.json).
В дереве достижений: `lbc.adv.legends`; описание: `lbc.adv.legends.lore`. Параметры показа: `{"icon":{"id":"minecraft:nether_star"},"frame":"task","show_toast":true,"announce_to_chat":false,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/root`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок убивает сущность. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/sins/envy`

Источник: [lbc:true_advancements/legends/sins/envy](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/sins/envy.json).
В дереве достижений: `lbc.adv.legends.sins.envy`; описание: `lbc.adv.legends.sins.envy.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:advancement_icon/sin_envy"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/sins/gluttony`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [workingitems/witch_gens/envy.mcfunction:4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/witch_gens/envy.mcfunction) — `advancement grant @s only lbc:true_advancements/legends/sins/envy`.

## `lbc:true_advancements/legends/sins/gluttony`

Источник: [lbc:true_advancements/legends/sins/gluttony](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/sins/gluttony.json).
В дереве достижений: `lbc.adv.legends.sins.gluttony`; описание: `lbc.adv.legends.sins.gluttony.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:advancement_icon/sin_gluttony"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/sins/wrath`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [workingitems/witch_gens/gluttony_get.mcfunction:7](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/witch_gens/gluttony_get.mcfunction) — `advancement grant @s only lbc:true_advancements/legends/sins/gluttony`.

## `lbc:true_advancements/legends/sins/greed`

Источник: [lbc:true_advancements/legends/sins/greed](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/sins/greed.json).
В дереве достижений: `lbc.adv.legends.sins.greed`; описание: `lbc.adv.legends.sins.greed.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:advancement_icon/sin_greed"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/witch_gens`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [workingitems/witch_gens/greed_get.mcfunction:9](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/witch_gens/greed_get.mcfunction) — `advancement grant @s only lbc:true_advancements/legends/sins/greed`.

## `lbc:true_advancements/legends/sins/indolence`

Источник: [lbc:true_advancements/legends/sins/indolence](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/sins/indolence.json).
В дереве достижений: `lbc.adv.legends.sins.indolence`; описание: `lbc.adv.legends.sins.indolence.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:advancement_icon/sin_indolence"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/sins/greed`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [workingitems/witch_gens/indolence_get.mcfunction:10](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/witch_gens/indolence_get.mcfunction) — `advancement grant @s only lbc:true_advancements/legends/sins/indolence`.

## `lbc:true_advancements/legends/sins/lust`

Источник: [lbc:true_advancements/legends/sins/lust](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/sins/lust.json).
В дереве достижений: `lbc.adv.legends.sins.lust`; описание: `lbc.adv.legends.sins.lust.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:advancement_icon/sin_lust"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/sins/indolence`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [workingitems/witch_gens/lust_get.mcfunction:7](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/witch_gens/lust_get.mcfunction) — `advancement grant @s only lbc:true_advancements/legends/sins/lust`.

## `lbc:true_advancements/legends/sins/vanity`

Источник: [lbc:true_advancements/legends/sins/vanity](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/sins/vanity.json).
В дереве достижений: `lbc.adv.legends.sins.vanity`; описание: `lbc.adv.legends.sins.vanity.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:advancement_icon/sin_vanity"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/sins/lust`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [workingitems/witch_gens/vanity.mcfunction:115](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/witch_gens/vanity.mcfunction) — `advancement grant @s only lbc:true_advancements/legends/sins/vanity`.

## `lbc:true_advancements/legends/sins/wrath`

Источник: [lbc:true_advancements/legends/sins/wrath](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/sins/wrath.json).
В дереве достижений: `lbc.adv.legends.sins.wrath`; описание: `lbc.adv.legends.sins.wrath.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:advancement_icon/sin_wrath"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/sins/vanity`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [workingitems/witch_gens/wrath_get.mcfunction:6](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/witch_gens/wrath_get.mcfunction) — `advancement grant @s only lbc:true_advancements/legends/sins/wrath`.

## `lbc:true_advancements/legends/stand`

Источник: [lbc:true_advancements/legends/stand](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/stand.json).
В дереве достижений: `lbc.adv.legends.stand`; описание: `lbc.adv.legends.stand.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:oraora"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/infinity_infinity_shulker`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{oraora:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/stand_clocks`

Источник: [lbc:true_advancements/legends/stand_clocks](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/stand_clocks.json).
В дереве достижений: `lbc.adv.legends.stand_clocks`; описание: `lbc.adv.legends.stand_clocks.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:stand_clock"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/stand`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{stand_clocks:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/supernova`

Источник: [lbc:true_advancements/legends/supernova](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/supernova.json).
В дереве достижений: `lbc.adv.legends.supernova`; описание: `lbc.adv.legends.supernova.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:advancement_icon/omega","minecraft:enchantment_glint_override":true}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/hohlostaff`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [workingitems/supernovacore_newworld.mcfunction:70](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/supernovacore_newworld.mcfunction) — `advancement grant @a[gamemode=!spectator] only lbc:true_advancements/legends/supernova`.

## `lbc:true_advancements/legends/tiny_wands`

Источник: [lbc:true_advancements/legends/tiny_wands](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/tiny_wands.json).
В дереве достижений: `lbc.adv.legends.tiny_wands`; описание: `lbc.adv.legends.tiny_wands.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:tiny_wands"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/kill_tiny_skeleton`; это связь отображения, а не дополнительный критерий.
Критерий `wand_1`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{struby_tiny: 1}"}}`).
Критерий `wand_2`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{stterra_tiny: 1}"}}`).
Критерий `wand_3`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{stelectro_tiny: 1}"}}`).
Критерий `wand_4`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{stair_tiny: 1}"}}`).
Критерий `wand_5`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{stacid_tiny: 1}"}}`).
Критерий `wand_6`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{stteleport_tiny: 1}"}}`).
Критерий `wand_7`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{ststone_tiny: 1}"}}`).
Критерий `wand_8`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{stfire_tiny: 1}"}}`).
Критерий `wand_9`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{stblind_tiny: 1}"}}`).
Для завершения требуются все 9 критериев.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/trollge_all_masks`

Источник: [lbc:true_advancements/legends/trollge_all_masks](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/trollge_all_masks.json).
В дереве достижений: `lbc.adv.legends.trollge_all_masks`; описание: `lbc.adv.legends.trollge_all_masks.lore`. Параметры показа: `{"icon":{"id":"minecraft:snowball","components":{"minecraft:item_model":"lbc:troll_box","minecraft:enchantment_glint_override":true}},"frame":"goal","show_toast":true,"announce_to_chat":true}`.
Родитель в дереве: `lbc:true_advancements/legends/imitator_shulker`; это связь отображения, а не дополнительный критерий.
Критерий `mask_1`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{troll: 1,trollmask: 1}"}}`).
Критерий `mask_2`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{troll: 1,trollmask: 2}"}}`).
Критерий `mask_3`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{troll: 1,trollmask: 3}"}}`).
Критерий `mask_4`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{troll: 1,trollmask: 4}"}}`).
Критерий `mask_5`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{troll: 1,trollmask: 5}"}}`).
Критерий `mask_6`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{troll: 1,trollmask: 6}"}}`).
Для завершения требуются все 6 критериев.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/trollge_killer`

Источник: [lbc:true_advancements/legends/trollge_killer](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/trollge_killer.json).
В дереве достижений: `lbc.adv.legends.trollge_killer`; описание: `lbc.adv.legends.trollge_killer.lore`. Параметры показа: `{"icon":{"id":"minecraft:leather_helmet","components":{"minecraft:item_model":"lbc:mask_1"}},"frame":"goal","show_toast":true,"announce_to_chat":true}`.
Родитель в дереве: `lbc:true_advancements/legends/trollge_all_masks`; это связь отображения, а не дополнительный критерий.
Критерий `req_1`: игрок убивает сущность. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); сущность: (свойства `this`: `{"nbt":"{Tags:[\"trollge_hunter\"]}"}`).
Критерий `req_2`: игрок убивает сущность. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); сущность: (свойства `this`: `{"nbt":"{Tags:[\"stone_man\"]}"}`).
Критерий `req_3`: игрок убивает сущность. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); сущность: (свойства `this`: `{"nbt":"{Tags:[\"void_man\"]}"}`).
Для завершения требуются все 3 критериев.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/tuntija_wands/acid`

Источник: [lbc:true_advancements/legends/tuntija_wands/acid](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/tuntija_wands/acid.json).
В дереве достижений: `lbc.adv.tuntija.acid`; описание: `lbc.adv.tuntija.acid.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:st/acid"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/tuntija_wands/air`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable: 1,staff: 1,stacid: 1}"}}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/skill_acidman](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/skill_acidman.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/tuntija_wands/acidacid`

Источник: [lbc:true_advancements/legends/tuntija_wands/acidacid](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/tuntija_wands/acidacid.json).
В дереве достижений: `lbc.adv.tuntija.acidacid`; описание: `lbc.adv.tuntija.acidacid.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:acidacidwand"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/tuntija_wands/acid`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable:1,staff:1,acidacid_wand:1}"}}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/skill_acidman](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/skill_acidman.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/tuntija_wands/acidcorrupt`

Источник: [lbc:true_advancements/legends/tuntija_wands/acidcorrupt](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/tuntija_wands/acidcorrupt.json).
В дереве достижений: `lbc.adv.tuntija.acidcorrupt`; описание: `lbc.adv.tuntija.acidcorrupt.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:acidcorrupt"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/tuntija_wands/acid`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable:1,staff:1,acidcorrupt_wand:1}"}}`).
Награда: выполнить [lbc:swrg_kit_integration/gain/skill_acidman](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/skill_acidman.mcfunction) от лица игрока.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/tuntija_wands/air`

Источник: [lbc:true_advancements/legends/tuntija_wands/air](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/tuntija_wands/air.json).
В дереве достижений: `lbc.adv.tuntija.air`; описание: `lbc.adv.tuntija.air.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:st/air"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/tuntija_wands/electro`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable:1,staff:1,stair:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/tuntija_wands/bedrock`

Источник: [lbc:true_advancements/legends/tuntija_wands/bedrock](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/tuntija_wands/bedrock.json).
В дереве достижений: `lbc.adv.tuntija.bedrock`; описание: `lbc.adv.tuntija.bedrock.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:bedrock_wand"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/tuntija_wands/stone`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable:1,staff:1,bedrock_wand:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/tuntija_wands/big_stone`

Источник: [lbc:true_advancements/legends/tuntija_wands/big_stone](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/tuntija_wands/big_stone.json).
В дереве достижений: `lbc.adv.tuntija.big_stone`; описание: `lbc.adv.tuntija.big_stone.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:stonekivi"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/tuntija_wands/stone`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable:1,staff:1,stonekivi_wand:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/tuntija_wands/blazefire`

Источник: [lbc:true_advancements/legends/tuntija_wands/blazefire](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/tuntija_wands/blazefire.json).
В дереве достижений: `lbc.adv.tuntija.blazefire`; описание: `lbc.adv.tuntija.blazefire.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:blazefire"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/tuntija_wands/fire`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable:1,staff:1,fireblaze_wand:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/tuntija_wands/blind`

Источник: [lbc:true_advancements/legends/tuntija_wands/blind](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/tuntija_wands/blind.json).
В дереве достижений: `lbc.adv.tuntija.blind`; описание: `lbc.adv.tuntija.blind.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:st/blind"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/tuntija_wands/fire`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable:1,staff:1,stblind:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/tuntija_wands/boltbolt`

Источник: [lbc:true_advancements/legends/tuntija_wands/boltbolt](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/tuntija_wands/boltbolt.json).
В дереве достижений: `lbc.adv.tuntija.boltbolt`; описание: `lbc.adv.tuntija.boltbolt.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:boltbolt_wand"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/tuntija_wands/teleport`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable:1,staff:1,boltbolt_wand:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/tuntija_wands/consuming`

Источник: [lbc:true_advancements/legends/tuntija_wands/consuming](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/tuntija_wands/consuming.json).
В дереве достижений: `lbc.adv.tuntija.consuming`; описание: `lbc.adv.tuntija.consuming.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:consuming_wand"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/tuntija_wands/ruby`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable:1,staff:1,consuming_wand:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/tuntija_wands/dublicating`

Источник: [lbc:true_advancements/legends/tuntija_wands/dublicating](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/tuntija_wands/dublicating.json).
В дереве достижений: `lbc.adv.tuntija.dublicating`; описание: `lbc.adv.tuntija.dublicating.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:dublicating_wand"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/tuntija_wands/ruby`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable:1,staff:1,dublicating_wand:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/tuntija_wands/electro`

Источник: [lbc:true_advancements/legends/tuntija_wands/electro](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/tuntija_wands/electro.json).
В дереве достижений: `lbc.adv.tuntija.electro`; описание: `lbc.adv.tuntija.electro.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:st/electro"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/tuntija_wands/terra`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable: 1,staff: 1,stelectro: 1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/tuntija_wands/electro_shield`

Источник: [lbc:true_advancements/legends/tuntija_wands/electro_shield](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/tuntija_wands/electro_shield.json).
В дереве достижений: `lbc.adv.tuntija.electro_shield`; описание: `lbc.adv.tuntija.electro_shield.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:electroblocker"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/tuntija_wands/electro`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{electroblocker:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/tuntija_wands/erecto`

Источник: [lbc:true_advancements/legends/tuntija_wands/erecto](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/tuntija_wands/erecto.json).
В дереве достижений: `lbc.adv.tuntija.erecto`; описание: `lbc.adv.tuntija.erecto.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:erecto"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/tuntija_wands/electro`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{erecto_wand:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/tuntija_wands/exahoming`

Источник: [lbc:true_advancements/legends/tuntija_wands/exahoming](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/tuntija_wands/exahoming.json).
В дереве достижений: `lbc.adv.tuntija.exahoming`; описание: `lbc.adv.tuntija.exahoming.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:homing"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/tuntija_wands/air`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{exahoming_wand:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/tuntija_wands/exasperato`

Источник: [lbc:true_advancements/legends/tuntija_wands/exasperato](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/tuntija_wands/exasperato.json).
В дереве достижений: `lbc.adv.tuntija.exasperato`; описание: `lbc.adv.tuntija.exasperato.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:exasperato"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/tuntija_wands/air`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{exasperato_wand:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/tuntija_wands/fartus_nadristailo`

Источник: [lbc:true_advancements/legends/tuntija_wands/fartus_nadristailo](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/tuntija_wands/fartus_nadristailo.json).
В дереве достижений: `lbc.adv.fartus_nadristailo`; описание: `lbc.adv.fartus_nadristailo.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:fartus"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/tuntija_wands/terra`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{fartus_nadristailo:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/tuntija_wands/fire`

Источник: [lbc:true_advancements/legends/tuntija_wands/fire](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/tuntija_wands/fire.json).
В дереве достижений: `lbc.adv.tuntija.fire`; описание: `lbc.adv.tuntija.fire.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:st/fire"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/tuntija_wands/stone`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{stfire:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/tuntija_wands/psychoblind`

Источник: [lbc:true_advancements/legends/tuntija_wands/psychoblind](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/tuntija_wands/psychoblind.json).
В дереве достижений: `lbc.adv.tuntija.psychoblind`; описание: `lbc.adv.tuntija.psychoblind.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:psychoblind"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/tuntija_wands/blind`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{psychoblind_wand:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/tuntija_wands/razorterra`

Источник: [lbc:true_advancements/legends/tuntija_wands/razorterra](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/tuntija_wands/razorterra.json).
В дереве достижений: `lbc.adv.tuntija.razorterra`; описание: `lbc.adv.tuntija.razorterra.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:terra_razorpine"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/tuntija_wands/terra`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{razorterra:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/tuntija_wands/ruby`

Источник: [lbc:true_advancements/legends/tuntija_wands/ruby](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/tuntija_wands/ruby.json).
В дереве достижений: `lbc.adv.tuntija.ruby`; описание: `lbc.adv.tuntija.ruby.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:st/ruby"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/infinity`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{struby:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/tuntija_wands/stone`

Источник: [lbc:true_advancements/legends/tuntija_wands/stone](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/tuntija_wands/stone.json).
В дереве достижений: `lbc.adv.tuntija.stone`; описание: `lbc.adv.tuntija.stone.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:st/stone"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/tuntija_wands/teleport`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{ststone:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/tuntija_wands/teleport`

Источник: [lbc:true_advancements/legends/tuntija_wands/teleport](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/tuntija_wands/teleport.json).
В дереве достижений: `lbc.adv.tuntija.teleport`; описание: `lbc.adv.tuntija.teleport.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:st/teleport"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/tuntija_wands/acid`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{stteleport:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/tuntija_wands/terra`

Источник: [lbc:true_advancements/legends/tuntija_wands/terra](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/tuntija_wands/terra.json).
В дереве достижений: `lbc.adv.tuntija.terra`; описание: `lbc.adv.tuntija.terra.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:st/terra"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/tuntija_wands/ruby`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{stterra:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/tuntija_wands/the_sun`

Источник: [lbc:true_advancements/legends/tuntija_wands/the_sun](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/tuntija_wands/the_sun.json).
В дереве достижений: `lbc.adv.tuntija.the_sun`; описание: `lbc.adv.tuntija.the_sun.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:the_sun"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/tuntija_wands/fire`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{sun_arcanum:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/tuntija_wands/true_terra`

Источник: [lbc:true_advancements/legends/tuntija_wands/true_terra](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/tuntija_wands/true_terra.json).
В дереве достижений: `lbc.adv.tuntija.true_terra`; описание: `lbc.adv.tuntija.true_terra.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:terrasword_true"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/tuntija_wands/terra`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{true_terra:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/tuntija_wands/vexblind`

Источник: [lbc:true_advancements/legends/tuntija_wands/vexblind](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/tuntija_wands/vexblind.json).
В дереве достижений: `lbc.adv.tuntija.vexblind`; описание: `lbc.adv.tuntija.vexblind.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:blindvex"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/tuntija_wands/blind`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{psychovex_wand:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/legends/uniwand`

Источник: [lbc:true_advancements/legends/uniwand](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/uniwand.json).
В дереве достижений: `lbc.adv.legends.uniwand`; описание: `lbc.adv.legends.uniwand.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:uniwand"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/kill_grimoire_boss`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [items/wands/uniwand/click.mcfunction:2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/uniwand/click.mcfunction) — `execute if score @s mana < mana lbc.math if score @s manamax < mana lbc.math run advancement grant @s only lbc:true_advancements/legends/uniwand`.

## `lbc:true_advancements/legends/witch_gens`

Источник: [lbc:true_advancements/legends/witch_gens](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/legends/witch_gens.json).
В дереве достижений: `lbc.adv.legends.witch_gens`; описание: `lbc.adv.legends.witch_gens.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:witch_gens"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/legends/trollge_all_masks`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [workingitems/witch_gens/yes_gens.mcfunction:2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/witch_gens/yes_gens.mcfunction) — `advancement grant @s only lbc:true_advancements/legends/witch_gens`.

## `lbc:true_advancements/lightning_smith`

Источник: [lbc:true_advancements/lightning_smith](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/lightning_smith.json).
В дереве достижений: `lbc.adv.lightning_smith`; описание: `lbc.adv.lightning_smith.lore`. Параметры показа: `{"icon":{"id":"minecraft:panda_spawn_egg","components":{"minecraft:item_model":"lbc:lightningegg"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/root`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [other/transform.mcfunction:22](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/transform.mcfunction) — `advancement grant @p[gamemode=!spectator] only lbc:true_advancements/lightning_smith`; [reforge.mcfunction:24](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/reforge.mcfunction) — `advancement grant @p[gamemode=!spectator] only lbc:true_advancements/lightning_smith`; [reforge_crafting_0.mcfunction:24](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/reforge_crafting_0.mcfunction) — `advancement grant @s only lbc:true_advancements/lightning_smith`; [reforge_crafting_1.mcfunction:24](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/reforge_crafting_1.mcfunction) — `advancement grant @s only lbc:true_advancements/lightning_smith`; [reforge_crafting_2.mcfunction:24](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/reforge_crafting_2.mcfunction) — `advancement grant @s only lbc:true_advancements/lightning_smith`; [reforge_crafting_3.mcfunction:24](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/reforge_crafting_3.mcfunction) — `advancement grant @s only lbc:true_advancements/lightning_smith`.

## `lbc:true_advancements/lightning_smith_ultra`

Источник: [lbc:true_advancements/lightning_smith_ultra](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/lightning_smith_ultra.json).
В дереве достижений: `lbc.adv.lightning_smith_legendary`; описание: `lbc.adv.lightning_smith_legendary.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:infbolt"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/lightning_smith`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [ultra_reforge.mcfunction:44](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/ultra_reforge.mcfunction) — `advancement grant @p[gamemode=!spectator] only lbc:true_advancements/lightning_smith_ultra`.

## `lbc:true_advancements/nexus/cage`

Источник: [lbc:true_advancements/nexus/cage](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/nexus/cage.json).
В дереве достижений: `lbc.adv.nexus.cage`; описание: `lbc.adv.nexus.cage.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:nexus_item_cage_white"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/nexus/ground`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [other/nexus/tick_1s_player.mcfunction:6](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/tick_1s_player.mcfunction) — `execute if entity @e[type=minecraft:item_display,distance=..5,tag=wire_converter_frame,limit=1] run advancement grant @s only lbc:true_advancements/nexus/cage`.

## `lbc:true_advancements/nexus/corrupter`

Источник: [lbc:true_advancements/nexus/corrupter](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/nexus/corrupter.json).
В дереве достижений: `lbc.adv.nexus.corrupter`; описание: `lbc.adv.nexus.corrupter.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:nexus_corrupter"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/nexus/wire_black`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{nexus_corrupter:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/nexus/experience_gun`

Источник: [lbc:true_advancements/nexus/experience_gun](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/nexus/experience_gun.json).
В дереве достижений: `lbc.adv.nexus.xp_gun`; описание: `lbc.adv.nexus.xp_gun.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:nexus_orb_gun"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/nexus/wire_red`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{nexus_orb_gun:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/nexus/ground`

Источник: [lbc:true_advancements/nexus/ground](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/nexus/ground.json).
В дереве достижений: `lbc.adv.nexus.ground`; описание: `lbc.adv.nexus.ground.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:advancement_icon/nexus_ground"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/nexus/root`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: проверяется местоположение игрока. Условия: игрок: ((НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)) И (свойства `this`: `{"nbt":"{OnGround:1b}","location":{"dimension":"minecraft:nexus"}}`)).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/nexus/nexus_compass`

Источник: [lbc:true_advancements/nexus/nexus_compass](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/nexus/nexus_compass.json).
В дереве достижений: `lbc.adv.nexus.compass`; описание: `lbc.adv.nexus.compass.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:nexus_compass"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/nexus/wire_green`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{nexus_compass:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/nexus/roadgun`

Источник: [lbc:true_advancements/nexus/roadgun](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/nexus/roadgun.json).
В дереве достижений: `lbc.adv.nexus.roadgun`; описание: `lbc.adv.nexus.roadgun.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:nexus_roadgun"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/nexus/wire_white`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{nexus_roadgun:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/nexus/root`

Источник: [lbc:true_advancements/nexus/root](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/nexus/root.json).
В дереве достижений: `lbc.adv.nexus.root`; описание: `lbc.adv.nexus.root.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:advancement_icon/nexus_icon"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/root`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: проверяется местоположение игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)) И (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/nexus/white_unstabler`

Источник: [lbc:true_advancements/nexus/white_unstabler](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/nexus/white_unstabler.json).
В дереве достижений: `lbc.adv.nexus.pulsar`; описание: `lbc.adv.nexus.pulsar.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:pulsar"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/nexus/roadgun`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{nexus_pulsar:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/nexus/wire_black`

Источник: [lbc:true_advancements/nexus/wire_black](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/nexus/wire_black.json).
В дереве достижений: `lbc.adv.nexus.wire_black`; описание: `lbc.adv.nexus.wire_black.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:nexus_wire_onyx"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/nexus/cage`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{nexus_wire_black:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/nexus/wire_green`

Источник: [lbc:true_advancements/nexus/wire_green](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/nexus/wire_green.json).
В дереве достижений: `lbc.adv.nexus.wire_green`; описание: `lbc.adv.nexus.wire_green.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:nexus_wire_green"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/nexus/cage`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{nexus_wire_blue:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/nexus/wire_red`

Источник: [lbc:true_advancements/nexus/wire_red](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/nexus/wire_red.json).
В дереве достижений: `lbc.adv.nexus.wire_red`; описание: `lbc.adv.nexus.wire_red.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:nexus_wire_red"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/nexus/cage`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{nexus_wire_red:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/nexus/wire_white`

Источник: [lbc:true_advancements/nexus/wire_white](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/nexus/wire_white.json).
В дереве достижений: `lbc.adv.nexus.wire_white`; описание: `lbc.adv.nexus.wire_white.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:nexus_wire_white"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/nexus/cage`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{nexus_wire_white:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/not_a_lightning_smith`

Источник: [lbc:true_advancements/not_a_lightning_smith](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/not_a_lightning_smith.json).
В дереве достижений: `lbc.adv.yellow_lightning`; описание: `lbc.adv.yellow_lightning.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:yellow_lightning"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/lightning_smith_ultra`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{yellow_lightning:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/pickaxes/randchanted`

Источник: [lbc:true_advancements/pickaxes/randchanted](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/pickaxes/randchanted.json).
В дереве достижений: `lbc.adv.pickaxe.randchanted_pickaxe`; описание: `lbc.adv.pickaxe.randchanted_pickaxe.lore`. Параметры показа: `{"icon":{"id":"minecraft:iron_pickaxe","components":{"minecraft:enchantment_glint_override":true}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/pickaxes`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:enchantments":[{"enchantments":"minecraft:fortune","levels":{"min":3}}],"minecraft:custom_data":"{transformable:1,pickaxe:1,randchanted:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/pickaxes/silk`

Источник: [lbc:true_advancements/pickaxes/silk](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/pickaxes/silk.json).
В дереве достижений: `lbc.adv.pickaxe.silk`; описание: `lbc.adv.pickaxe.silk.lore`. Параметры показа: `{"icon":{"id":"minecraft:diamond_pickaxe","components":{"minecraft:enchantment_glint_override":true}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/pickaxes/randchanted`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [drops/specialdrops.mcfunction:77](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/drops/specialdrops.mcfunction) — `execute at @s[nbt={Item:{components:{"minecraft:custom_data":{adv_check_lbc:1}}}}] run advancement grant @a[gamemode=!spectator,distance=..7,limit=1,nbt={SelectedItem:{components:{"minecraft:custom_data":{silk:1}}}}] only lbc:true_advancements/pickaxes/silk`.

## `lbc:true_advancements/pickaxes`

Источник: [lbc:true_advancements/pickaxes](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/pickaxes.json).
В дереве достижений: `lbc.adv.pickaxes`; описание: ``. Параметры показа: `{"icon":{"id":"minecraft:golden_pickaxe"},"frame":"task","show_toast":true,"announce_to_chat":false,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/root`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable:1,pickaxe:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/potions/add_mana`

Источник: [lbc:true_advancements/potions/add_mana](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/potions/add_mana.json).
В дереве достижений: `lbc.adv.potion.add_mana`; описание: `lbc.adv.potion.add_mana.lore`. Параметры показа: `{"icon":{"id":"minecraft:potion","components":{"minecraft:enchantment_glint_override":true,"minecraft:potion_contents":{"custom_color":1114367}}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/potions/mana_regen`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предмет: `{"components":{"minecraft:custom_data":"{transformable: 1,potion: 1}","minecraft:potion_contents":{"custom_effects":[{"id":"minecraft:raid_omen","amplifier":19,"duration":10,"show_particles":false}],"custom_color":1114367}}}`.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [effects/effect_add_mana.mcfunction:2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/effects/effect_add_mana.mcfunction) — `advancement grant @s only lbc:true_advancements/potions/add_mana`.

## `lbc:true_advancements/potions/add_mana_big`

Источник: [lbc:true_advancements/potions/add_mana_big](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/potions/add_mana_big.json).
В дереве достижений: `lbc.adv.potion.add_mana_big`; описание: `lbc.adv.potion.add_mana_big.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:advancement_icon/magic"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/potions/add_mana`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [players_2s.mcfunction:10](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/players_2s.mcfunction) — `execute if score @s mana matches 35000.. run advancement grant @s only lbc:true_advancements/potions/add_mana_big`.

## `lbc:true_advancements/potions/add_mana_verybig`

Источник: [lbc:true_advancements/potions/add_mana_verybig](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/potions/add_mana_verybig.json).
В дереве достижений: `lbc.adv.potion.add_mana_verybig`; описание: `lbc.adv.potion.add_mana_verybig.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:advancement_icon/magic","minecraft:enchantment_glint_override":true}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/potions/add_mana_big`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [players_2s.mcfunction:11](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/players_2s.mcfunction) — `execute if score @s mana matches 200000.. run advancement grant @s only lbc:true_advancements/potions/add_mana_verybig`.

## `lbc:true_advancements/potions/chaotic`

Источник: [lbc:true_advancements/potions/chaotic](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/potions/chaotic.json).
В дереве достижений: `lbc.adv.potion.chaotic`; описание: `lbc.adv.potion.chaotic.lore`. Параметры показа: `{"icon":{"id":"minecraft:splash_potion","components":{"minecraft:potion_contents":{"custom_color":16753151}}},"frame":"task","show_toast":true,"announce_to_chat":false,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/potions/op`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"components":{"minecraft:custom_data":"{transformable: 1,potion: 1}","minecraft:potion_contents":{"custom_effects":[{"id":"minecraft:raid_omen","amplifier":40,"duration":300,"show_particles":false}],"custom_color":16753151}}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/potions/effect_all_seeing_eye`

Источник: [lbc:true_advancements/potions/effect_all_seeing_eye](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/potions/effect_all_seeing_eye.json).
В дереве достижений: `lbc.adv.potion.effect_all_seeing_eye`; описание: `lbc.adv.potion.effect_all_seeing_eye.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:advancement_icon/all_seeing_eye"}},"frame":"task","show_toast":true,"announce_to_chat":false,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/potions/effect_living_legend`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [other/effect_allseeingeye.mcfunction:7](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/effect_allseeingeye.mcfunction) — `advancement grant @s only lbc:true_advancements/potions/effect_all_seeing_eye`.

## `lbc:true_advancements/potions/effect_delayed_death`

Источник: [lbc:true_advancements/potions/effect_delayed_death](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/potions/effect_delayed_death.json).
В дереве достижений: `lbc.adv.potion.effect_delayed_death`; описание: `lbc.adv.potion.effect_delayed_death.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:advancement_icon/effect_delayed_death"}},"frame":"task","show_toast":true,"announce_to_chat":false,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/potions/effect_magic_buff`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [players_5s.mcfunction:6](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/players_5s.mcfunction) — `advancement grant @s[scores={delayed_death=1..}] only lbc:true_advancements/potions/effect_delayed_death`.

## `lbc:true_advancements/potions/effect_evoker`

Источник: [lbc:true_advancements/potions/effect_evoker](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/potions/effect_evoker.json).
В дереве достижений: `lbc.adv.potion.effect_evoker`; описание: `lbc.adv.potion.effect_evoker.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:advancement_icon/evoker"}},"frame":"task","show_toast":true,"announce_to_chat":false,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/potions/effect_high_hunger`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняются эффекты игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); эффекты: `{"minecraft:raid_omen":{"amplifier":113}}`.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/potions/effect_high_conduit_power`

Источник: [lbc:true_advancements/potions/effect_high_conduit_power](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/potions/effect_high_conduit_power.json).
В дереве достижений: `lbc.adv.potion.effect_high_conduit_power`; описание: `lbc.adv.potion.effect_high_conduit_power.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:advancement_icon/conduit_power"}},"frame":"task","show_toast":true,"announce_to_chat":false,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/potions/effect_high_water_breathing`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняются эффекты игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); эффекты: `{"minecraft:conduit_power":{"amplifier":{"min":1}}}`.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/potions/effect_high_hunger`

Источник: [lbc:true_advancements/potions/effect_high_hunger](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/potions/effect_high_hunger.json).
В дереве достижений: `lbc.adv.potion.effect_high_hunger`; описание: `lbc.adv.potion.effect_high_hunger.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:advancement_icon/hunger"}},"frame":"task","show_toast":true,"announce_to_chat":false,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/potions/effect_high_nausea`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняются эффекты игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); эффекты: `{"minecraft:hunger":{"amplifier":{"min":1}}}`.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/potions/effect_high_invisibility`

Источник: [lbc:true_advancements/potions/effect_high_invisibility](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/potions/effect_high_invisibility.json).
В дереве достижений: `lbc.adv.potion.effect_high_invisibility`; описание: `lbc.adv.potion.effect_high_invisibility.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:advancement_icon/invisibility"}},"frame":"task","show_toast":true,"announce_to_chat":false,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/potions`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняются эффекты игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); эффекты: `{"minecraft:invisibility":{"amplifier":{"min":1}}}`.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/potions/effect_high_nausea`

Источник: [lbc:true_advancements/potions/effect_high_nausea](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/potions/effect_high_nausea.json).
В дереве достижений: `lbc.adv.potion.effect_high_nausea`; описание: `lbc.adv.potion.effect_high_nausea.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:advancement_icon/nausea"}},"frame":"task","show_toast":true,"announce_to_chat":false,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/potions/effect_high_conduit_power`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняются эффекты игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); эффекты: `{"minecraft:nausea":{"amplifier":{"min":1}}}`.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/potions/effect_high_water_breathing`

Источник: [lbc:true_advancements/potions/effect_high_water_breathing](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/potions/effect_high_water_breathing.json).
В дереве достижений: `lbc.adv.potion.effect_high_water_breathing`; описание: `lbc.adv.potion.effect_high_water_breathing.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:advancement_icon/water_breathing"}},"frame":"task","show_toast":true,"announce_to_chat":false,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/potions/effect_high_invisibility`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняются эффекты игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); эффекты: `{"minecraft:water_breathing":{"amplifier":{"min":1}}}`.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/potions/effect_kiuaskivi`

Источник: [lbc:true_advancements/potions/effect_kiuaskivi](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/potions/effect_kiuaskivi.json).
В дереве достижений: `lbc.adv.potion.effect_kiuaskivi`; описание: `lbc.adv.potion.effect_kiuaskivi.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:advancement_icon/kiuaskivi"}},"frame":"task","show_toast":true,"announce_to_chat":false,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/potions/effect_lightarrow`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняются эффекты игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); эффекты: `{"minecraft:raid_omen":{"amplifier":111}}`.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/potions/effect_lightarrow`

Источник: [lbc:true_advancements/potions/effect_lightarrow](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/potions/effect_lightarrow.json).
В дереве достижений: `lbc.adv.potion.effect_lightarrow`; описание: `lbc.adv.potion.effect_lightarrow.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:advancement_icon/lightarrow"}},"frame":"task","show_toast":true,"announce_to_chat":false,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/potions/effect_evoker`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняются эффекты игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); эффекты: `{"minecraft:raid_omen":{"amplifier":112}}`.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/potions/effect_living_legend`

Источник: [lbc:true_advancements/potions/effect_living_legend](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/potions/effect_living_legend.json).
В дереве достижений: `lbc.adv.potion.effect_living_legend`; описание: `lbc.adv.potion.effect_living_legend.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:advancement_icon/living_legend"}},"frame":"task","show_toast":true,"announce_to_chat":false,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/potions/effect_void_resistance`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [other/effect_living_legend.mcfunction:20](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/effect_living_legend.mcfunction) — `advancement grant @s only lbc:true_advancements/potions/effect_living_legend`.

## `lbc:true_advancements/potions/effect_lookback`

Источник: [lbc:true_advancements/potions/effect_lookback](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/potions/effect_lookback.json).
В дереве достижений: `lbc.adv.potion.effect_lookback`; описание: `lbc.adv.potion.effect_lookback.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:advancement_icon/lookback"}},"frame":"task","show_toast":true,"announce_to_chat":false,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/potions/effect_twitching`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [players_2s.mcfunction:16](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/players_2s.mcfunction) — `execute if score @s lookback matches 1.. run advancement grant @s only lbc:true_advancements/potions/effect_lookback`.

## `lbc:true_advancements/potions/effect_magic_buff`

Источник: [lbc:true_advancements/potions/effect_magic_buff](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/potions/effect_magic_buff.json).
В дереве достижений: `lbc.adv.potion.effect_magic_buff`; описание: `lbc.adv.potion.effect_magic_buff.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:advancement_icon/magic_buff"}},"frame":"task","show_toast":true,"announce_to_chat":false,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/potions/effect_piss`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [other/manaregen_mana_boost.mcfunction:1](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/manaregen_mana_boost.mcfunction) — `advancement grant @s only lbc:true_advancements/potions/effect_magic_buff`.

## `lbc:true_advancements/potions/effect_no_magic`

Источник: [lbc:true_advancements/potions/effect_no_magic](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/potions/effect_no_magic.json).
В дереве достижений: `lbc.adv.potion.effect_no_magic`; описание: `lbc.adv.potion.effect_no_magic.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:advancement_icon/no_magic"}},"frame":"task","show_toast":true,"announce_to_chat":false,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/potions/effect_target`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [players_2s.mcfunction:14](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/players_2s.mcfunction) — `execute if score @s nomagic matches 1.. run advancement grant @s only lbc:true_advancements/potions/effect_no_magic`.

## `lbc:true_advancements/potions/effect_piss`

Источник: [lbc:true_advancements/potions/effect_piss](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/potions/effect_piss.json).
В дереве достижений: `lbc.adv.potion.effect_piss`; описание: `lbc.adv.potion.effect_piss.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:advancement_icon/piss"}},"frame":"task","show_toast":true,"announce_to_chat":false,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/potions/effect_no_magic`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [workingitems/yellow_lightning_asplayer.mcfunction:6](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/yellow_lightning_asplayer.mcfunction) — `execute if score @s[type=minecraft:player] piss matches 1.. run advancement grant @s only lbc:true_advancements/potions/effect_piss`.

## `lbc:true_advancements/potions/effect_reverse_mana`

Источник: [lbc:true_advancements/potions/effect_reverse_mana](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/potions/effect_reverse_mana.json).
В дереве достижений: `lbc.adv.potion.effect_reverse_mana`; описание: `lbc.adv.potion.effect_reverse_mana.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:advancement_icon/effect_reverse_mana"}},"frame":"task","show_toast":true,"announce_to_chat":false,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/potions/effect_delayed_death`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [players_5s.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/players_5s.mcfunction) — `advancement grant @s[scores={reverse_mana=1..}] only lbc:true_advancements/potions/effect_reverse_mana`.

## `lbc:true_advancements/potions/effect_scared`

Источник: [lbc:true_advancements/potions/effect_scared](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/potions/effect_scared.json).
В дереве достижений: `lbc.adv.potion.effect_scared`; описание: `lbc.adv.potion.effect_scared.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:advancement_icon/scared"}},"frame":"task","show_toast":true,"announce_to_chat":false,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/potions/effect_lookback`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [players_2s.mcfunction:15](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/players_2s.mcfunction) — `execute if score @s scared matches 1.. run advancement grant @s only lbc:true_advancements/potions/effect_scared`.

## `lbc:true_advancements/potions/effect_shifting`

Источник: [lbc:true_advancements/potions/effect_shifting](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/potions/effect_shifting.json).
В дереве достижений: `lbc.adv.potion.effect_shifting`; описание: `lbc.adv.potion.effect_shifting.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:advancement_icon/shifting"}},"frame":"task","show_toast":true,"announce_to_chat":false,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/potions/effect_shock`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [other/effect_shifting.mcfunction:3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/effect_shifting.mcfunction) — `advancement grant @s only lbc:true_advancements/potions/effect_shifting`.

## `lbc:true_advancements/potions/effect_shock`

Источник: [lbc:true_advancements/potions/effect_shock](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/potions/effect_shock.json).
В дереве достижений: `lbc.adv.potion.effect_shock`; описание: `lbc.adv.potion.effect_shock.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:advancement_icon/shock"}},"frame":"task","show_toast":true,"announce_to_chat":false,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/potions/effect_reverse_mana`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [other/shock.mcfunction:4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/shock.mcfunction) — `advancement grant @s[type=minecraft:player] only lbc:true_advancements/potions/effect_shock`.

## `lbc:true_advancements/potions/effect_skill_issue`

Источник: [lbc:true_advancements/potions/effect_skill_issue](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/potions/effect_skill_issue.json).
В дереве достижений: `lbc.adv.potion.effect_skill_issue`; описание: `lbc.adv.potion.effect_skill_issue.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:skill_issue"}},"frame":"task","show_toast":true,"announce_to_chat":false,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/potions/effect_all_seeing_eye`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [other/effect_skill_issue.mcfunction:12](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/effect_skill_issue.mcfunction) — `advancement grant @s only lbc:true_advancements/potions/effect_skill_issue`.

## `lbc:true_advancements/potions/effect_target`

Источник: [lbc:true_advancements/potions/effect_target](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/potions/effect_target.json).
В дереве достижений: `lbc.adv.potion.effect_target`; описание: `lbc.adv.potion.effect_target.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:advancement_icon/target"}},"frame":"task","show_toast":true,"announce_to_chat":false,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/potions/effect_scared`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [players_2s.mcfunction:13](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/players_2s.mcfunction) — `execute if score @s target matches 1.. run advancement grant @s only lbc:true_advancements/potions/effect_target`.

## `lbc:true_advancements/potions/effect_twitching`

Источник: [lbc:true_advancements/potions/effect_twitching](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/potions/effect_twitching.json).
В дереве достижений: `lbc.adv.potion.effect_twitching`; описание: `lbc.adv.potion.effect_twitching.lore`. Параметры показа: `{"icon":{"id":"minecraft:snowball","components":{"minecraft:item_model":"lbc:ruchka"}},"frame":"task","show_toast":true,"announce_to_chat":false,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/potions/effect_kiuaskivi`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [projectiles/gadgets/ruchka_pl.mcfunction:2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/gadgets/ruchka_pl.mcfunction) — `advancement grant @s only lbc:true_advancements/potions/effect_twitching`.

## `lbc:true_advancements/potions/effect_void_resistance`

Источник: [lbc:true_advancements/potions/effect_void_resistance](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/potions/effect_void_resistance.json).
В дереве достижений: `lbc.adv.potion.effect_void_resistance`; описание: `lbc.adv.potion.effect_void_resistance.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:advancement_icon/void_resistance"}},"frame":"task","show_toast":true,"announce_to_chat":false,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/potions/effect_shifting`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [other/effect_void_resistance.mcfunction:4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/effect_void_resistance.mcfunction) — `advancement grant @s only lbc:true_advancements/potions/effect_void_resistance`.

## `lbc:true_advancements/potions/explosive`

Источник: [lbc:true_advancements/potions/explosive](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/potions/explosive.json).
В дереве достижений: `lbc.adv.potion.explosive`; описание: `lbc.adv.potion.explosive.lore`. Параметры показа: `{"icon":{"id":"minecraft:lingering_potion","components":{"minecraft:potion_contents":{"custom_color":16743168}}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/potions/purify`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"items":["minecraft:tipped_arrow"],"components":{"minecraft:potion_contents":{"custom_color":16743168,"custom_effects":[{"id":"minecraft:raid_omen","amplifier":104,"duration":20}]}}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/potions/gvozd`

Источник: [lbc:true_advancements/potions/gvozd](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/potions/gvozd.json).
В дереве достижений: `lbc.adv.potion.gvozd`; описание: `lbc.adv.potion.gvozd.lore`. Параметры показа: `{"icon":{"id":"minecraft:lingering_potion","components":{"minecraft:potion_contents":{}}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/potions`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"items":["minecraft:tipped_arrow"],"components":{"minecraft:potion_contents":{"custom_effects":[{"id":"minecraft:instant_damage","amplifier":1},{"id":"minecraft:nausea","amplifier":5,"duration":880},{"id":"minecraft:regeneration","amplifier":5,"duration":240},{"id":"minecraft:invisibility","amplifier":5,"duration":880},{"id":"minecraft:night_vision","amplifier":5,"duration":880},{"id":"minecraft:hunger","amplifier":5,"duration":880},{"id":"minecraft:poison","amplifier":5,"duration":880},{"id":"minecraft:health_boost","amplifier":5,"duration":240},{"id":"minecraft:glowing","amplifier":5,"duration":880},{"id":"minecraft:levitation","amplifier":6,"duration":240},{"id":"minecraft:luck","amplifier":5,"duration":880},{"id":"minecraft:unluck","amplifier":6,"duration":880}]}}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/potions/mana_regen`

Источник: [lbc:true_advancements/potions/mana_regen](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/potions/mana_regen.json).
В дереве достижений: `lbc.adv.potion.mana_regen`; описание: `lbc.adv.potion.mana_regen.lore`. Параметры показа: `{"icon":{"id":"minecraft:potion","components":{"minecraft:potion_contents":{"custom_color":4495590}}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/potions/max_mana`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [effects/effect_add_manaregen.mcfunction:2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/effects/effect_add_manaregen.mcfunction) — `advancement grant @s only lbc:true_advancements/potions/mana_regen`.

## `lbc:true_advancements/potions/mana_regen_big`

Источник: [lbc:true_advancements/potions/mana_regen_big](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/potions/mana_regen_big.json).
В дереве достижений: `lbc.adv.potion.mana_regen_big`; описание: `lbc.adv.potion.mana_regen_big.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:advancement_icon/magic"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/potions/mana_regen`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [players_2s.mcfunction:6](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/players_2s.mcfunction) — `execute if score @s manaregen_visual matches 30.. run advancement grant @s only lbc:true_advancements/potions/mana_regen_big`.

## `lbc:true_advancements/potions/mana_regen_verybig`

Источник: [lbc:true_advancements/potions/mana_regen_verybig](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/potions/mana_regen_verybig.json).
В дереве достижений: `lbc.adv.potion.mana_regen_verybig`; описание: `lbc.adv.potion.mana_regen_verybig.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:advancement_icon/magic","minecraft:enchantment_glint_override":true}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/potions/mana_regen_big`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [players_2s.mcfunction:7](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/players_2s.mcfunction) — `execute if score @s manaregen_visual matches 150.. run advancement grant @s only lbc:true_advancements/potions/mana_regen_verybig`.

## `lbc:true_advancements/potions/mana_temp`

Источник: [lbc:true_advancements/potions/mana_temp](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/potions/mana_temp.json).
В дереве достижений: `lbc.adv.potion.mana_temp`; описание: `lbc.adv.potion.mana_temp.lore`. Параметры показа: `{"icon":{"id":"minecraft:potion","components":{"minecraft:potion_contents":{"custom_color":11380991}}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/potions/succubus`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [other/manaregen.mcfunction:12](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/manaregen.mcfunction) — `execute if score daeris lbc.math matches ..-15 run advancement grant @s only lbc:true_advancements/potions/mana_temp`.

## `lbc:true_advancements/potions/max_mana`

Источник: [lbc:true_advancements/potions/max_mana](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/potions/max_mana.json).
В дереве достижений: `lbc.adv.potion.max_mana`; описание: `lbc.adv.potion.max_mana.lore`. Параметры показа: `{"icon":{"id":"minecraft:potion","components":{"minecraft:potion_contents":{"custom_color":328482}}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/potions/explosive`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [effects/effect_add_manamax.mcfunction:2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/effects/effect_add_manamax.mcfunction) — `advancement grant @s only lbc:true_advancements/potions/max_mana`.

## `lbc:true_advancements/potions/max_mana_big`

Источник: [lbc:true_advancements/potions/max_mana_big](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/potions/max_mana_big.json).
В дереве достижений: `lbc.adv.potion.max_mana_big`; описание: `lbc.adv.potion.max_mana_big.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:advancement_icon/magic"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/potions/max_mana`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [players_2s.mcfunction:8](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/players_2s.mcfunction) — `execute if score @s manamax matches 15000.. run advancement grant @s only lbc:true_advancements/potions/max_mana_big`.

## `lbc:true_advancements/potions/max_mana_verybig`

Источник: [lbc:true_advancements/potions/max_mana_verybig](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/potions/max_mana_verybig.json).
В дереве достижений: `lbc.adv.potion.max_mana_verybig`; описание: `lbc.adv.potion.max_mana_verybig.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:advancement_icon/magic","minecraft:enchantment_glint_override":true}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/potions/max_mana_big`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [players_2s.mcfunction:9](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/players_2s.mcfunction) — `execute if score @s manamax matches 50000.. run advancement grant @s only lbc:true_advancements/potions/max_mana_verybig`.

## `lbc:true_advancements/potions/no_mana`

Источник: [lbc:true_advancements/potions/no_mana](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/potions/no_mana.json).
В дереве достижений: `lbc.adv.potion.no_mana`; описание: `lbc.adv.potion.no_mana.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:advancement_icon/nomagic"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/potions/remove_mana`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [players_2s.mcfunction:12](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/players_2s.mcfunction) — `execute if score @s mana matches ..0 if score @s manaregen_visual matches ..0 run advancement grant @s only lbc:true_advancements/potions/no_mana`.

## `lbc:true_advancements/potions/oh_no_mana`

Источник: [lbc:true_advancements/potions/oh_no_mana](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/potions/oh_no_mana.json).
В дереве достижений: `lbc.adv.potion.oh_no_mana`; описание: `lbc.adv.potion.oh_no_mana.lore`. Параметры показа: `{"icon":{"id":"minecraft:repeating_command_block","components":{"minecraft:item_model":"lbc:advancement_icon/nomagic","minecraft:enchantment_glint_override":true}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/potions/no_mana`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [other/negative_mana.mcfunction:9](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/negative_mana.mcfunction) — `execute if score @s mana matches ..-1 if score @s manaregen_visual matches ..-1 if score @s manamax matches ..-1 run advancement grant @s only lbc:true_advancements/potions/oh_no_mana`.

## `lbc:true_advancements/potions/op`

Источник: [lbc:true_advancements/potions/op](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/potions/op.json).
В дереве достижений: `lbc.adv.potion.op`; описание: `lbc.adv.potion.op.lore`. Параметры показа: `{"icon":{"id":"minecraft:potion","components":{"minecraft:item_model":"lbc:oppotion","minecraft:potion_contents":{"custom_color":16777215}}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/potions/gvozd`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок убивает сущность. Условия: игрок: (свойства `this`: `{"effects":{"minecraft:night_vision":{"amplifier":61}}}`); сущность: (свойства `this`: `{"type":"minecraft:player"}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/potions/purify`

Источник: [lbc:true_advancements/potions/purify](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/potions/purify.json).
В дереве достижений: `lbc.adv.potion.purify`; описание: `lbc.adv.potion.purify.lore`. Параметры показа: `{"icon":{"id":"minecraft:splash_potion","components":{"minecraft:potion_contents":{"custom_color":16777215}}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/potions/mana_temp`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"components":{"minecraft:potion_contents":{"custom_color":16777215,"custom_effects":[{"id":"minecraft:raid_omen","amplifier":43,"duration":100,"show_particles":false}]},"minecraft:custom_data":"{transformable: 1,potion: 1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/potions/remove_mana`

Источник: [lbc:true_advancements/potions/remove_mana](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/potions/remove_mana.json).
В дереве достижений: `lbc.adv.potion.remove_mana`; описание: `lbc.adv.potion.remove_mana.lore`. Параметры показа: `{"icon":{"id":"minecraft:lingering_potion","components":{"minecraft:potion_contents":{"custom_color":16056821}}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/potions/add_mana`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"items":["minecraft:tipped_arrow"],"components":{"minecraft:potion_contents":{"custom_effects":[{"id":"minecraft:raid_omen","amplifier":20,"duration":20,"show_particles":false}],"custom_color":16056821}}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/potions/snus`

Источник: [lbc:true_advancements/potions/snus](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/potions/snus.json).
В дереве достижений: `lbc.adv.potion.snus`; описание: `lbc.adv.potion.snus.lore`. Параметры показа: `{"icon":{"id":"minecraft:potion","components":{"minecraft:item_model":"lbc:snus","minecraft:potion_contents":{"custom_color":5731155}}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/potions/chaotic`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [effects/effect_snus.mcfunction:2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/effects/effect_snus.mcfunction) — `advancement grant @s only lbc:true_advancements/potions/snus`.

## `lbc:true_advancements/potions/succubus`

Источник: [lbc:true_advancements/potions/succubus](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/potions/succubus.json).
В дереве достижений: `lbc.adv.potion.succubus`; описание: `lbc.adv.potion.succubus.lore`. Параметры показа: `{"icon":{"id":"minecraft:potion","components":{"minecraft:potion_contents":{"custom_color":16777215}}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/potions/snus`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок заканчивает употребление предмета. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предмет: `{"predicates":{"minecraft:custom_data":"{transformable:1,potion:1,succubus:1}"}}`.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/potions`

Источник: [lbc:true_advancements/potions](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/potions.json).
В дереве достижений: `lbc.adv.potions`; описание: ``. Параметры показа: `{"icon":{"id":"minecraft:potion"},"frame":"task","show_toast":true,"announce_to_chat":false,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/root`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable:1,potion:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/root`

Источник: [lbc:true_advancements/root](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/root.json).
В дереве достижений: `Lucky Block`; описание: `lbc.adv.root.desc`. Параметры показа: `{"icon":{"id":"minecraft:acacia_hanging_sign","components":{"minecraft:item_model":"lbc:lucky_block"}},"frame":"task","background":"minecraft:block/lucky_block_nigger","show_toast":true,"announce_to_chat":false,"hidden":false}`.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"items":"minecraft:red_mushroom_block","components":{"minecraft:item_model":"lbc:lucky_block"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/shields/energy`

Источник: [lbc:true_advancements/shields/energy](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/shields/energy.json).
В дереве достижений: `lbc.adv.shield.energy`; описание: `lbc.adv.shield.energy.lore`. Параметры показа: `{"icon":{"id":"minecraft:shield","components":{"minecraft:base_color":"light_blue","minecraft:banner_patterns":[{"color":"black","pattern":"curly_border"},{"color":"white","pattern":"flower"},{"color":"white","pattern":"rhombus"},{"color":"light_blue","pattern":"circle"},{"color":"blue","pattern":"triangles_bottom"},{"color":"blue","pattern":"triangles_top"}]}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/shields/hylian`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок наносит урон сущности. Условия: игрок: ((свойства `this`: `{"equipment":{"mainhand":{"predicates":{"minecraft:custom_data":"{energy_shield: 1}"}}}}`) ИЛИ (свойства `this`: `{"equipment":{"offhand":{"predicates":{"minecraft:custom_data":"{energy_shield: 1}"}}}}`)); урон: `{"type":{"direct_entity":{"type":"#minecraft:arrows","nbt":"{Tags:[\"mirrored_2\"]}"}}}`.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/shields/holy`

Источник: [lbc:true_advancements/shields/holy](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/shields/holy.json).
В дереве достижений: `lbc.adv.shield.holy`; описание: `lbc.adv.shield.holy.lore`. Параметры показа: `{"icon":{"id":"minecraft:shield","components":{"minecraft:base_color":"light_blue","minecraft:banner_patterns":[{"color":"yellow","pattern":"stripe_middle"},{"color":"yellow","pattern":"stripe_center"},{"color":"yellow","pattern":"flower"}]}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/shields/energy`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [items/shields/holyshield.mcfunction:1](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/shields/holyshield.mcfunction) — `execute unless score time_stopped lbc.math matches 1.. anchored eyes positioned ^ ^ ^3 if entity @e[type=minecraft:zombie,distance=..4,limit=1,nbt={DeathLootTable:"lbc:reactive_shish"}] run advancement grant @s only lbc:true_advancements/shields/holy`.

## `lbc:true_advancements/shields/hylian`

Источник: [lbc:true_advancements/shields/hylian](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/shields/hylian.json).
В дереве достижений: `lbc.adv.shield.hylian`; описание: `lbc.adv.shield.hylian.lore`. Параметры показа: `{"icon":{"id":"minecraft:shield","components":{"minecraft:enchantment_glint_override":true,"minecraft:base_color":"blue","minecraft:banner_patterns":[{"color":"light_gray","pattern":"curly_border"},{"color":"red","pattern":"flower"},{"color":"blue","pattern":"half_horizontal"},{"color":"yellow","pattern":"triangle_top"},{"color":"blue","pattern":"stripe_top"},{"color":"light_gray","pattern":"border"}]}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/shields`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: проверка происходит каждый игровой тик. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)) И ((свойства `this`: `{"equipment":{"mainhand":{"predicates":{"minecraft:custom_data":"{hylianshield:1, transformable: 1,shield: 1}"}}}}`) И (свойства `this`: `{"equipment":{"offhand":{"predicates":{"minecraft:custom_data":"{hylianshield:1, transformable: 1,shield: 1}"}}}}`)).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/shields`

Источник: [lbc:true_advancements/shields](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/shields.json).
В дереве достижений: `lbc.adv.shields`; описание: ``. Параметры показа: `{"icon":{"id":"minecraft:shield"},"frame":"task","show_toast":true,"announce_to_chat":false,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/root`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable:1,shield:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/shooter/ak_47`

Источник: [lbc:true_advancements/shooter/ak_47](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/shooter/ak_47.json).
В дереве достижений: `lbc.adv.shooter.ak_47`; описание: `lbc.adv.shooter.ak_47.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:ak47"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/shooter/shit_cannon`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable:1,shooter:1,ak47:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/shooter/attack_helicopter`

Источник: [lbc:true_advancements/shooter/attack_helicopter](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/shooter/attack_helicopter.json).
В дереве достижений: `lbc.adv.shooter.attack_helicopter`; описание: `lbc.adv.shooter.attack_helicopter.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:attack_helicopter"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/shooter/razorpine`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [workingitems/attackhelicopter_left.mcfunction:21](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/attackhelicopter_left.mcfunction) — `execute if entity @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{attack_helicopter:1}}}}] run advancement grant @s only lbc:true_advancements/shooter/attack_helicopter`.

## `lbc:true_advancements/shooter/kazahstan`

Источник: [lbc:true_advancements/shooter/kazahstan](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/shooter/kazahstan.json).
В дереве достижений: `lbc.adv.shooter.kazahstan`; описание: `lbc.adv.shooter.kazahstan.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:helicopter_boss"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/shooter/woodificator`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{boss_helicopter:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/shooter/kfc_gun`

Источник: [lbc:true_advancements/shooter/kfc_gun](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/shooter/kfc_gun.json).
В дереве достижений: `lbc.adv.shooter.kfc_gun`; описание: `lbc.adv.shooter.kfc_gun.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:kfcgun"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/shooter/mob_gun`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [other/eatables/kfc_chick.mcfunction:23](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/kfc_chick.mcfunction) — `advancement grant @s only lbc:true_advancements/shooter/kfc_gun`.

## `lbc:true_advancements/shooter/laser_minigun`

Источник: [lbc:true_advancements/shooter/laser_minigun](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/shooter/laser_minigun.json).
В дереве достижений: `lbc.adv.shooter.laser_minigun`; описание: `lbc.adv.shooter.laser_minigun.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:laser_minigun/1"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/shooter/magnum`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [other/techno/laser_minigun/shoot/cast.mcfunction:6](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/laser_minigun/shoot/cast.mcfunction) — `execute if score @s laser_minigun_spread matches ..1000 run advancement grant @s only lbc:true_advancements/shooter/laser_minigun`.

## `lbc:true_advancements/shooter/magnum`

Источник: [lbc:true_advancements/shooter/magnum](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/shooter/magnum.json).
В дереве достижений: `lbc.adv.shooter.magnum`; описание: `lbc.adv.shooter.magnum.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:magnum"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/shooter/ak_47`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [other/techno/magnum/shoot/acid/cast.mcfunction:10](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/acid/cast.mcfunction) — `advancement grant @s only lbc:true_advancements/shooter/magnum`; [other/techno/magnum/shoot/electric/cast.mcfunction:10](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/electric/cast.mcfunction) — `advancement grant @s only lbc:true_advancements/shooter/magnum`; [other/techno/magnum/shoot/explosive/cast.mcfunction:9](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/explosive/cast.mcfunction) — `advancement grant @s only lbc:true_advancements/shooter/magnum`; [other/techno/magnum/shoot/incendiary/cast.mcfunction:10](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/incendiary/cast.mcfunction) — `advancement grant @s only lbc:true_advancements/shooter/magnum`; [other/techno/magnum/shoot/standard/cast.mcfunction:9](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/standard/cast.mcfunction) — `advancement grant @s only lbc:true_advancements/shooter/magnum`; [other/techno/magnum/shoot/void/cast.mcfunction:10](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/shoot/void/cast.mcfunction) — `advancement grant @s only lbc:true_advancements/shooter/magnum`.

## `lbc:true_advancements/shooter/mob_gun`

Источник: [lbc:true_advancements/shooter/mob_gun](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/shooter/mob_gun.json).
В дереве достижений: `lbc.adv.shooter.mob_gun`; описание: `lbc.adv.shooter.mob_gun.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:mobgun"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/shooter/phantom_knifes`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [workingitems/mobgun.mcfunction:120](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/mobgun.mcfunction) — `execute if score #random105 lbc.math matches 22 run advancement grant @s only lbc:true_advancements/shooter/mob_gun`.

## `lbc:true_advancements/shooter/phantom_knifes`

Источник: [lbc:true_advancements/shooter/phantom_knifes](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/shooter/phantom_knifes.json).
В дереве достижений: `lbc.adv.shooter.phantom_knifes`; описание: `lbc.adv.shooter.phantom_knifes.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:phantomknifes"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/shooter`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [workingitems/phantomknifesleft/start.mcfunction:4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/phantomknifesleft/start.mcfunction) — `advancement grant @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{phantomknifes:1}}}}] only lbc:true_advancements/shooter/phantom_knifes`.

## `lbc:true_advancements/shooter/razorpine`

Источник: [lbc:true_advancements/shooter/razorpine](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/shooter/razorpine.json).
В дереве достижений: `lbc.adv.shooter.razorpine`; описание: `lbc.adv.shooter.razorpine.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:razorpine"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/shooter/kfc_gun`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [workingitems/razorpine_ammo_check.mcfunction:7](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/razorpine_ammo_check.mcfunction) — `execute if score temp_check lbc.math matches 1 run advancement grant @s only lbc:true_advancements/shooter/razorpine`.

## `lbc:true_advancements/shooter/shit_cannon`

Источник: [lbc:true_advancements/shooter/shit_cannon](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/shooter/shit_cannon.json).
В дереве достижений: `lbc.adv.shooter.shit_cannon`; описание: `lbc.adv.shooter.shit_cannon.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:govnomet"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/shooter/attack_helicopter`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [raycast_gavno1.mcfunction:3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_gavno1.mcfunction) — `execute if entity @e[type=minecraft:creeper,dx=0,dy=0,dz=0,distance=..4,tag=govno_creeper] run advancement grant @a[tag=nodmg,limit=1] only lbc:true_advancements/shooter/shit_cannon`; [raycast_gavno_mega1.mcfunction:15](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_gavno_mega1.mcfunction) — `execute if entity @e[type=minecraft:creeper,dx=0,dy=0,dz=0,distance=..4,tag=govno_creeper] run advancement grant @a[tag=nodmg,limit=1] only lbc:true_advancements/shooter/shit_cannon`.

## `lbc:true_advancements/shooter/slr`

Источник: [lbc:true_advancements/shooter/slr](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/shooter/slr.json).
В дереве достижений: `lbc.adv.shooter.slr`; описание: `lbc.adv.shooter.slr.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:super_rifle"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/shooter/kazahstan`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [items/shooter/slr/dmg_with_id_standart_head.mcfunction:2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/shooter/slr/dmg_with_id_standart_head.mcfunction) — `advancement grant @a[gamemode=!spectator,tag=caster,limit=1] only lbc:true_advancements/shooter/slr`.

## `lbc:true_advancements/shooter/woodificator`

Источник: [lbc:true_advancements/shooter/woodificator](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/shooter/woodificator.json).
В дереве достижений: `lbc.adv.shooter.woodificator`; описание: `lbc.adv.shooter.woodificator.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:woodificator"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/shooter/laser_minigun`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable:1,shooter:1,woodificator:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/shooter`

Источник: [lbc:true_advancements/shooter](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/shooter.json).
В дереве достижений: `lbc.adv.shooter`; описание: ``. Параметры показа: `{"icon":{"id":"minecraft:spectral_arrow"},"frame":"task","show_toast":true,"announce_to_chat":false,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/root`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable:1,shooter:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/shovels/mortis`

Источник: [lbc:true_advancements/shovels/mortis](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/shovels/mortis.json).
В дереве достижений: `lbc.adv.shovel.mortis`; описание: `lbc.adv.shovel.mortis.lore`. Параметры показа: `{"icon":{"id":"minecraft:netherite_shovel","components":{"minecraft:item_model":"lbc:killer_shovel"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/shovels/ultra`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок убивает сущность. Условия: игрок: (свойства `this`: `{"equipment":{"mainhand":{"predicates":{"minecraft:custom_data":"{transformable: 1,shovel: 1,mortis: 1}"}}}}`); сущность: (свойства `this`: `{"type":"minecraft:player"}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/shovels/randchanted`

Источник: [lbc:true_advancements/shovels/randchanted](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/shovels/randchanted.json).
В дереве достижений: `lbc.adv.shovel.randchanted`; описание: `lbc.adv.shovel.randchanted.lore`. Параметры показа: `{"icon":{"id":"minecraft:iron_shovel","components":{"minecraft:enchantment_glint_override":true}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/shovels`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:enchantments":[{"enchantments":"minecraft:silk_touch","levels":1}],"minecraft:custom_data":"{transformable: 1,shovel: 1,randchanted: 1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/shovels/ultra`

Источник: [lbc:true_advancements/shovels/ultra](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/shovels/ultra.json).
В дереве достижений: `lbc.adv.shovel.ultra`; описание: `lbc.adv.shovel.ultra.lore`. Параметры показа: `{"icon":{"id":"minecraft:iron_shovel","components":{"minecraft:enchantment_glint_override":true}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/shovels/randchanted`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:enchantments":[{"enchantments":"minecraft:efficiency","levels":255}],"minecraft:custom_data":"{transformable: 1,shovel: 1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/shovels`

Источник: [lbc:true_advancements/shovels](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/shovels.json).
В дереве достижений: `lbc.adv.shovels`; описание: ``. Параметры показа: `{"icon":{"id":"minecraft:golden_shovel"},"frame":"task","show_toast":true,"announce_to_chat":false,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/root`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable:1,shovel:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/spells/all`

Источник: [lbc:true_advancements/spells/all](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/spells/all.json).
В дереве достижений: `lbc.adv.spells.all`; описание: `lbc.adv.spells.all.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:spell"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/spells`; это связь отображения, а не дополнительный критерий.
Критерий `spell_1`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable: 1,spellitem: 1,anvils: 1}"}}`).
Критерий `spell_2`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable: 1,spellitem: 1,arrows1: 1}"}}`).
Критерий `spell_3`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable: 1,spellitem: 1,heal1: 1}"}}`).
Критерий `spell_4`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable: 1,spellitem: 1,creeperz: 1}"}}`).
Критерий `spell_5`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable: 1,spellitem: 1,lava1: 1}"}}`).
Критерий `spell_6`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable: 1,spellitem: 1,lightning: 1}"}}`).
Критерий `spell_7`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable: 1,spellitem: 1,skeletons: 1}"}}`).
Критерий `spell_8`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable: 1,spellitem: 1,trap2: 1}"}}`).
Критерий `spell_9`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable: 1,spellitem: 1,tnts: 1}"}}`).
Критерий `spell_10`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable: 1,spellitem: 1,wither1: 1}"}}`).
Для завершения требуются все 10 критериев.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/spells`

Источник: [lbc:true_advancements/spells](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/spells.json).
В дереве достижений: `lbc.adv.spells`; описание: ``. Параметры показа: `{"icon":{"id":"minecraft:paper"},"frame":"task","show_toast":true,"announce_to_chat":false,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/root`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable:1,spell:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/staffs/acid`

Источник: [lbc:true_advancements/staffs/acid](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/staffs/acid.json).
В дереве достижений: `lbc.adv.staff.acid`; описание: `lbc.adv.staff.acid.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:acidwand"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/staffs/tesla`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Награда: выполнить [lbc:swrg_kit_integration/gain/skill_acidman](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/skill_acidman.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [damagewithid2_increased.mcfunction:3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/damagewithid2_increased.mcfunction) — `advancement grant @a[tag=nodamage,limit=1,nbt={Inventory:[{components:{"minecraft:custom_data":{acidwand:1}}}]}] only lbc:true_advancements/staffs/acid`.

## `lbc:true_advancements/staffs/badwiki`

Источник: [lbc:true_advancements/staffs/badwiki](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/staffs/badwiki.json).
В дереве достижений: `lbc.adv.staff.badwiki`; описание: `lbc.adv.staff.badwiki.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:badwiki"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/staffs/blood`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [workingitems/badwiki.mcfunction:90](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/badwiki.mcfunction) — `execute if score #random84 lbc.math matches 63 run advancement grant @s only lbc:true_advancements/staffs/badwiki`.

## `lbc:true_advancements/staffs/bass`

Источник: [lbc:true_advancements/staffs/bass](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/staffs/bass.json).
В дереве достижений: `lbc.adv.staff.bass`; описание: `lbc.adv.staff.bass.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:basswand"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/staffs/evoker`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [workingitems/basscast_combo_succesful.mcfunction:8](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/basscast_combo_succesful.mcfunction) — `execute if score @s bass_combo matches 10.. run advancement grant @s only lbc:true_advancements/staffs/bass`; [workingitems/basscast_combo_succesful_less.mcfunction:6](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/basscast_combo_succesful_less.mcfunction) — `execute if score @s bass_combo matches 10 run advancement grant @s only lbc:true_advancements/staffs/bass`.

## `lbc:true_advancements/staffs/blaze`

Источник: [lbc:true_advancements/staffs/blaze](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/staffs/blaze.json).
В дереве достижений: `lbc.adv.staff.blaze`; описание: `lbc.adv.staff.blaze.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:blazewand"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/staffs/prism`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [raycast_end4_blaze_nyam.mcfunction:2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_end4_blaze_nyam.mcfunction) — `advancement grant @s only lbc:true_advancements/staffs/blaze`.

## `lbc:true_advancements/staffs/blood`

Источник: [lbc:true_advancements/staffs/blood](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/staffs/blood.json).
В дереве достижений: `lbc.adv.staff.blood`; описание: `lbc.adv.staff.blood.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:killingwand"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/staffs/confuse`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [workingitems/bloodmagic.mcfunction:11](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/bloodmagic.mcfunction) — `execute if entity @s[nbt={active_effects:[{id:"minecraft:resistance",amplifier:4b}]}] run advancement grant @s only lbc:true_advancements/staffs/blood`.

## `lbc:true_advancements/staffs/confuse`

Источник: [lbc:true_advancements/staffs/confuse](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/staffs/confuse.json).
В дереве достижений: `lbc.adv.staff.confuse`; описание: `lbc.adv.staff.confuse.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:foxuswand"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/staffs/vampire`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [workingitems/foxuscast.mcfunction:28](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/foxuscast.mcfunction) — `execute if score tmp1 lbc.math matches 50.. run advancement grant @s only lbc:true_advancements/staffs/confuse`; [workingitems/foxuscast_upgraded.mcfunction:30](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/foxuscast_upgraded.mcfunction) — `execute if score tmp1 lbc.math matches 50.. run advancement grant @s only lbc:true_advancements/staffs/confuse`.

## `lbc:true_advancements/staffs/corruption`

Источник: [lbc:true_advancements/staffs/corruption](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/staffs/corruption.json).
В дереве достижений: `lbc.adv.staff.corruption`; описание: `lbc.adv.staff.corruption.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:corruptwand"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/staffs/bass`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Награда: выполнить [lbc:swrg_kit_integration/gain/skill_acidman](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gain/skill_acidman.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [workingitems/corrupted_pentagram/spawn.mcfunction:3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/corrupted_pentagram/spawn.mcfunction) — `advancement grant @p[gamemode=!spectator] only lbc:true_advancements/staffs/corruption`.

## `lbc:true_advancements/staffs/danmaku_staff`

Источник: [lbc:true_advancements/staffs/danmaku_staff](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/staffs/danmaku_staff.json).
В дереве достижений: `lbc.adv.staff.danmaku_staff`; описание: `lbc.adv.staff.danmaku_staff.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:danmaku"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/staffs/exoperso`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [items/wands/danmaku_staff/click_bomb.mcfunction:4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/danmaku_staff/click_bomb.mcfunction) — `advancement grant @s only lbc:true_advancements/staffs/danmaku_staff`.

## `lbc:true_advancements/staffs/ender_random`

Источник: [lbc:true_advancements/staffs/ender_random](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/staffs/ender_random.json).
В дереве достижений: `lbc.adv.staff.ender_random`; описание: `lbc.adv.staff.ender_random.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:enderwand"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/staffs/corruption`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [workingitems/endcast.mcfunction:12](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/endcast.mcfunction) — `execute at @s if entity @a[gamemode=!spectator,distance=0.01..8] run advancement grant @s only lbc:true_advancements/staffs/ender_random`.

## `lbc:true_advancements/staffs/evoker`

Источник: [lbc:true_advancements/staffs/evoker](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/staffs/evoker.json).
В дереве достижений: `lbc.adv.staff.evoker`; описание: `lbc.adv.staff.evoker.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:evokerwand"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/staffs`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [workingitems/evokercast.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/evokercast.mcfunction) — `execute if entity @e[type=minecraft:evoker,distance=..10] run advancement grant @s only lbc:true_advancements/staffs/evoker`; [workingitems/evokercast_upgraded.mcfunction:8](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/evokercast_upgraded.mcfunction) — `execute if entity @e[type=minecraft:evoker,distance=..10] run advancement grant @s only lbc:true_advancements/staffs/evoker`.

## `lbc:true_advancements/staffs/exoperso`

Источник: [lbc:true_advancements/staffs/exoperso](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/staffs/exoperso.json).
В дереве достижений: `lbc.adv.staff.exoperso`; описание: `lbc.adv.staff.exoperso.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:exoperso"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/staffs/lightning`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [workingitems/exopersoexplode_scheduled.mcfunction:2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/exopersoexplode_scheduled.mcfunction) — `execute if score players2 lbc.math matches -1 if score players lbc.math matches 3.. run advancement grant @a[gamemode=!spectator,tag=exoperso_casted,limit=1] only lbc:true_advancements/staffs/exoperso`.

## `lbc:true_advancements/staffs/falling_stars`

Источник: [lbc:true_advancements/staffs/falling_stars](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/staffs/falling_stars.json).
В дереве достижений: `lbc.adv.staff.falling_stars`; описание: `lbc.adv.staff.falling_stars.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:starwand"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/staffs/teleport`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [raycast_end5.mcfunction:23](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_end5.mcfunction) — `execute if score @s Distance matches 300.. if entity @e[type=#minecraft:mobs,distance=..7,tag=!spectator,tag=!raycaster] run advancement grant @s only lbc:true_advancements/staffs/falling_stars`.

## `lbc:true_advancements/staffs/gaycaster`

Источник: [lbc:true_advancements/staffs/gaycaster](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/staffs/gaycaster.json).
В дереве достижений: `lbc.adv.staff.gaycaster`; описание: `lbc.adv.staff.gaycaster.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:gaycaster"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/staffs/prism`; это связь отображения, а не дополнительный критерий.
Критерий `r`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `o`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `y`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `g`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `c`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `b`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `p`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Для завершения требуются все 7 критериев.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [workingitems/gaycaster/ray/impact/block/run.mcfunction:12](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/gaycaster/ray/impact/block/run.mcfunction) — `execute if score @s h matches 330..359 if entity @e[type=#minecraft:mobs,distance=..1.5,tag=!caster,tag=!spectator] run advancement grant @s only lbc:true_advancements/staffs/gaycaster r`; [workingitems/gaycaster/ray/impact/block/run.mcfunction:13](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/gaycaster/ray/impact/block/run.mcfunction) — `execute if score @s h matches 0..20 if entity @e[type=#minecraft:mobs,distance=..1.5,tag=!caster,tag=!spectator] run advancement grant @s only lbc:true_advancements/staffs/gaycaster r`; [workingitems/gaycaster/ray/impact/block/run.mcfunction:14](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/gaycaster/ray/impact/block/run.mcfunction) — `execute if score @s h matches 21..72 if entity @e[type=#minecraft:mobs,distance=..1.5,tag=!caster,tag=!spectator] run advancement grant @s only lbc:true_advancements/staffs/gaycaster o`; [workingitems/gaycaster/ray/impact/block/run.mcfunction:15](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/gaycaster/ray/impact/block/run.mcfunction) — `execute if score @s h matches 73..124 if entity @e[type=#minecraft:mobs,distance=..1.5,tag=!caster,tag=!spectator] run advancement grant @s only lbc:true_advancements/staffs/gaycaster y`; [workingitems/gaycaster/ray/impact/block/run.mcfunction:16](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/gaycaster/ray/impact/block/run.mcfunction) — `execute if score @s h matches 125..175 if entity @e[type=#minecraft:mobs,distance=..1.5,tag=!caster,tag=!spectator] run advancement grant @s only lbc:true_advancements/staffs/gaycaster g`; [workingitems/gaycaster/ray/impact/block/run.mcfunction:17](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/gaycaster/ray/impact/block/run.mcfunction) — `execute if score @s h matches 176..226 if entity @e[type=#minecraft:mobs,distance=..1.5,tag=!caster,tag=!spectator] run advancement grant @s only lbc:true_advancements/staffs/gaycaster c`; [workingitems/gaycaster/ray/impact/block/run.mcfunction:18](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/gaycaster/ray/impact/block/run.mcfunction) — `execute if score @s h matches 227..278 if entity @e[type=#minecraft:mobs,distance=..1.5,tag=!caster,tag=!spectator] run advancement grant @s only lbc:true_advancements/staffs/gaycaster b`; [workingitems/gaycaster/ray/impact/block/run.mcfunction:19](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/gaycaster/ray/impact/block/run.mcfunction) — `execute if score @s h matches 278..329 if entity @e[type=#minecraft:mobs,distance=..1.5,tag=!caster,tag=!spectator] run advancement grant @s only lbc:true_advancements/staffs/gaycaster p`; [workingitems/gaycaster/ray/impact/entity/run.mcfunction:13](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/gaycaster/ray/impact/entity/run.mcfunction) — `execute if score @s h matches 330..359 run advancement grant @s only lbc:true_advancements/staffs/gaycaster r`; [workingitems/gaycaster/ray/impact/entity/run.mcfunction:14](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/gaycaster/ray/impact/entity/run.mcfunction) — `execute if score @s h matches 0..20 run advancement grant @s only lbc:true_advancements/staffs/gaycaster r`; [workingitems/gaycaster/ray/impact/entity/run.mcfunction:15](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/gaycaster/ray/impact/entity/run.mcfunction) — `execute if score @s h matches 21..72 run advancement grant @s only lbc:true_advancements/staffs/gaycaster o`; [workingitems/gaycaster/ray/impact/entity/run.mcfunction:16](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/gaycaster/ray/impact/entity/run.mcfunction) — `execute if score @s h matches 73..124 run advancement grant @s only lbc:true_advancements/staffs/gaycaster y`; [workingitems/gaycaster/ray/impact/entity/run.mcfunction:17](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/gaycaster/ray/impact/entity/run.mcfunction) — `execute if score @s h matches 125..175 run advancement grant @s only lbc:true_advancements/staffs/gaycaster g`; [workingitems/gaycaster/ray/impact/entity/run.mcfunction:18](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/gaycaster/ray/impact/entity/run.mcfunction) — `execute if score @s h matches 176..226 run advancement grant @s only lbc:true_advancements/staffs/gaycaster c`; [workingitems/gaycaster/ray/impact/entity/run.mcfunction:19](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/gaycaster/ray/impact/entity/run.mcfunction) — `execute if score @s h matches 227..278 run advancement grant @s only lbc:true_advancements/staffs/gaycaster b`; [workingitems/gaycaster/ray/impact/entity/run.mcfunction:20](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/gaycaster/ray/impact/entity/run.mcfunction) — `execute if score @s h matches 278..329 run advancement grant @s only lbc:true_advancements/staffs/gaycaster p`.

## `lbc:true_advancements/staffs/hedgehog`

Источник: [lbc:true_advancements/staffs/hedgehog](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/staffs/hedgehog.json).
В дереве достижений: `lbc.adv.staff.hedgehog`; описание: `lbc.adv.staff.hedgehog.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:hedgehogwand"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/staffs/unstable_book`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [raycast_loop9cd.mcfunction:3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_loop9cd.mcfunction) — `execute if score tmp lbc.math matches 40.. run advancement grant @s only lbc:true_advancements/staffs/hedgehog`; [raycast_loop9cd_upgraded_2.mcfunction:3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_loop9cd_upgraded_2.mcfunction) — `execute if score tmp lbc.math matches 40.. run advancement grant @s only lbc:true_advancements/staffs/hedgehog`.

## `lbc:true_advancements/staffs/ice_wand`

Источник: [lbc:true_advancements/staffs/ice_wand](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/staffs/ice_wand.json).
В дереве достижений: `lbc.adv.staff.ice_wand`; описание: `lbc.adv.staff.ice_wand.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:ice_wand"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/staffs/neutral`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable:1,staff:1,ice_wand:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/staffs/lightning`

Источник: [lbc:true_advancements/staffs/lightning](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/staffs/lightning.json).
В дереве достижений: `lbc.adv.staff.lightning`; описание: `lbc.adv.staff.lightning.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:lightningboltwand"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/staffs/neutral`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [raycast_lightnings.mcfunction:2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_lightnings.mcfunction) — `execute if entity @e[type=!minecraft:lightning_bolt,distance=..3,tag=!spectator,tag=!raycaster,nbt=!{OnGround:1b}] run advancement grant @s only lbc:true_advancements/staffs/lightning`.

## `lbc:true_advancements/staffs/neutral`

Источник: [lbc:true_advancements/staffs/neutral](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/staffs/neutral.json).
В дереве достижений: `lbc.adv.staff.neutral`; описание: `lbc.adv.staff.neutral.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:neutralwand"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/staffs/omega_flag`; это связь отображения, а не дополнительный критерий.
Критерий `fire`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `ice`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `stone`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `void`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Для завершения требуются все 4 критериев.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [other/craft/ananta.mcfunction:4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/ananta.mcfunction) — `advancement grant @p[gamemode=!spectator,distance=..14] only lbc:true_advancements/staffs/neutral stone`; [other/craft/ananta_inv.mcfunction:4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/ananta_inv.mcfunction) — `advancement grant @s only lbc:true_advancements/staffs/neutral stone`; [other/craft/fotja.mcfunction:4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/fotja.mcfunction) — `advancement grant @p[gamemode=!spectator,distance=..14] only lbc:true_advancements/staffs/neutral fire`; [other/craft/fotja_inv.mcfunction:4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/fotja_inv.mcfunction) — `advancement grant @s only lbc:true_advancements/staffs/neutral fire`; [other/craft/ice_wand.mcfunction:3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/ice_wand.mcfunction) — `advancement grant @p[gamemode=!spectator,distance=..14] only lbc:true_advancements/staffs/neutral ice`; [other/craft/ice_wand_inv.mcfunction:3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/ice_wand_inv.mcfunction) — `advancement grant @s only lbc:true_advancements/staffs/neutral ice`; [other/craft/swargia.mcfunction:4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/swargia.mcfunction) — `advancement grant @p[gamemode=!spectator,distance=..14] only lbc:true_advancements/staffs/neutral void`; [other/craft/swargia_inv.mcfunction:4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/swargia_inv.mcfunction) — `advancement grant @p[gamemode=!spectator,distance=..14] only lbc:true_advancements/staffs/neutral void`.

## `lbc:true_advancements/staffs/omega_flag`

Источник: [lbc:true_advancements/staffs/omega_flag](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/staffs/omega_flag.json).
В дереве достижений: `lbc.adv.staff.omega_flag`; описание: `lbc.adv.staff.omega_flag.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:omegaflag"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/staffs/acid`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок убивает сущность. Условия: игрок: (свойства `this`: `{"equipment":{"mainhand":{"predicates":{"minecraft:custom_data":"{transformable: 1,staff: 1,omegaflag: 1}"}}}}`); сущность: (свойства `this`: `{"type":"minecraft:player"}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/staffs/polymorph`

Источник: [lbc:true_advancements/staffs/polymorph](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/staffs/polymorph.json).
В дереве достижений: `lbc.adv.staff.polymorph`; описание: `lbc.adv.staff.polymorph.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:sheepwand"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/staffs/hedgehog`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [raycast_end8_mob.mcfunction:3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_end8_mob.mcfunction) — `execute if score tmp lbc.math matches 1000.. run advancement grant @a[tag=raycaster,limit=1] only lbc:true_advancements/staffs/polymorph`; [raycast_end8_mob_check.mcfunction:4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_end8_mob_check.mcfunction) — `execute if score tmp lbc.math matches 1000.. run advancement grant @a[tag=raycaster,limit=1] only lbc:true_advancements/staffs/polymorph`.

## `lbc:true_advancements/staffs/prism`

Источник: [lbc:true_advancements/staffs/prism](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/staffs/prism.json).
В дереве достижений: `lbc.adv.staff.prism`; описание: `lbc.adv.staff.prism.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:prism"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/staffs/falling_stars`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [dmg_bypl_8.mcfunction:4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/dmg_bypl_8.mcfunction) — `execute if entity @s[tag=stable] if entity @e[type=#minecraft:mobs,dx=0,dy=0,dz=0,distance=..10,tag=!nodamage,tag=!nodamage1,tag=!spectator] run advancement grant @a[tag=nodamage,limit=1] only lbc:true_advancements/staffs/prism`; [dmg_bypl_8_1.mcfunction:4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/dmg_bypl_8_1.mcfunction) — `execute if entity @s[tag=stable] if entity @e[type=#minecraft:mobs,dx=0,dy=0,dz=0,distance=..10,tag=!nodamage,tag=!nodamage1,tag=!spectator] run advancement grant @a[tag=nodamage,limit=1] only lbc:true_advancements/staffs/prism`.

## `lbc:true_advancements/staffs/psycho`

Источник: [lbc:true_advancements/staffs/psycho](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/staffs/psycho.json).
В дереве достижений: `lbc.adv.staff.psycho`; описание: `lbc.adv.staff.psycho.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:psychowand"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/staffs/polymorph`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [workingitems/psychowand_check_hp_2.mcfunction:2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/psychowand_check_hp_2.mcfunction) — `advancement grant @a[tag=caster,limit=1] only lbc:true_advancements/staffs/psycho`.

## `lbc:true_advancements/staffs/sauvojen_tuntija_wand`

Источник: [lbc:true_advancements/staffs/sauvojen_tuntija_wand](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/staffs/sauvojen_tuntija_wand.json).
В дереве достижений: `lbc.adv.staff.sauvojen_wand`; описание: `lbc.adv.staff.sauvojen_wand.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:sauvojen_wand"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/staffs/danmaku_staff`; это связь отображения, а не дополнительный критерий.
Критерий `wand1`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `wand2`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `wand3`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `wand4`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `wand5`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `wand6`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `wand7`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `wand8`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Критерий `wand9`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Для завершения требуются все 9 критериев.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [items/wands/sauvojen_wand/cast_2.mcfunction:15](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/cast_2.mcfunction) — `execute if score #random10 lbc.math matches 1 run advancement grant @s only lbc:true_advancements/staffs/sauvojen_tuntija_wand wand1`; [items/wands/sauvojen_wand/cast_2.mcfunction:16](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/cast_2.mcfunction) — `execute if score #random10 lbc.math matches 2 run advancement grant @s only lbc:true_advancements/staffs/sauvojen_tuntija_wand wand2`; [items/wands/sauvojen_wand/cast_2.mcfunction:17](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/cast_2.mcfunction) — `execute if score #random10 lbc.math matches 3 run advancement grant @s only lbc:true_advancements/staffs/sauvojen_tuntija_wand wand3`; [items/wands/sauvojen_wand/cast_2.mcfunction:18](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/cast_2.mcfunction) — `execute if score #random10 lbc.math matches 4 run advancement grant @s only lbc:true_advancements/staffs/sauvojen_tuntija_wand wand4`; [items/wands/sauvojen_wand/cast_2.mcfunction:19](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/cast_2.mcfunction) — `execute if score #random10 lbc.math matches 5 run advancement grant @s only lbc:true_advancements/staffs/sauvojen_tuntija_wand wand5`; [items/wands/sauvojen_wand/cast_2.mcfunction:20](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/cast_2.mcfunction) — `execute if score #random10 lbc.math matches 6 run advancement grant @s only lbc:true_advancements/staffs/sauvojen_tuntija_wand wand6`; [items/wands/sauvojen_wand/cast_2.mcfunction:21](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/cast_2.mcfunction) — `execute if score #random10 lbc.math matches 7 run advancement grant @s only lbc:true_advancements/staffs/sauvojen_tuntija_wand wand7`; [items/wands/sauvojen_wand/cast_2.mcfunction:22](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/cast_2.mcfunction) — `execute if score #random10 lbc.math matches 8 run advancement grant @s only lbc:true_advancements/staffs/sauvojen_tuntija_wand wand8`; [items/wands/sauvojen_wand/cast_2.mcfunction:23](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/cast_2.mcfunction) — `execute if score #random10 lbc.math matches 9 run advancement grant @s only lbc:true_advancements/staffs/sauvojen_tuntija_wand wand9`.

## `lbc:true_advancements/staffs/snow_wand`

Источник: [lbc:true_advancements/staffs/snow_wand](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/staffs/snow_wand.json).
В дереве достижений: `lbc.adv.staff.snow_wand`; описание: `lbc.adv.staff.snow_wand.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:snow_wand"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/staffs/sauvojen_tuntija_wand`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [workingitems/snow_wand/summon.mcfunction:9](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/snow_wand/summon.mcfunction) — `execute if entity @a[distance=..5,gamemode=!spectator,predicate=!lbc:same_team,predicate=!lbc:same_team] run advancement grant @s only lbc:true_advancements/staffs/snow_wand`.

## `lbc:true_advancements/staffs/teleport`

Источник: [lbc:true_advancements/staffs/teleport](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/staffs/teleport.json).
В дереве достижений: `lbc.adv.staff.teleport`; описание: `lbc.adv.staff.teleport.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:advancedenderwand"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/staffs/wall`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [raycast_end3.mcfunction:7](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_end3.mcfunction) — `execute if score @s Distance matches 180..200 run advancement grant @s only lbc:true_advancements/staffs/teleport`.

## `lbc:true_advancements/staffs/tesla`

Источник: [lbc:true_advancements/staffs/tesla](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/staffs/tesla.json).
В дереве достижений: `lbc.adv.staff.tesla`; описание: `lbc.adv.staff.tesla.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:lightningwand"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/staffs/psycho`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [raycast_molnia_cunny.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_molnia_cunny.mcfunction) — `advancement grant @a[gamemode=!spectator,tag=nodamage,limit=1] only lbc:true_advancements/staffs/tesla`; [raycast_molnia_cunny_mom.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_molnia_cunny_mom.mcfunction) — `advancement grant @a[gamemode=!spectator,tag=nodamage,limit=1] only lbc:true_advancements/staffs/tesla`.

## `lbc:true_advancements/staffs/unstable_book`

Источник: [lbc:true_advancements/staffs/unstable_book](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/staffs/unstable_book.json).
В дереве достижений: `lbc.adv.staff.unstable_book`; описание: `lbc.adv.staff.unstable_book.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:unstable_spellbook"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/staffs/blaze`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [workingitems/random_spell_lol.mcfunction:167](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/random_spell_lol.mcfunction) — `advancement grant @s only lbc:true_advancements/staffs/unstable_book`.

## `lbc:true_advancements/staffs/vampire`

Источник: [lbc:true_advancements/staffs/vampire](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/staffs/vampire.json).
В дереве достижений: `lbc.adv.staff.vampire`; описание: `lbc.adv.staff.vampire.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:vampirewand"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/staffs/ender_random`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [workingitems/lifestealer_adv.mcfunction:1](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/lifestealer_adv.mcfunction) — `execute if entity @s[team=1] if entity @e[type=#minecraft:mobs,type=!player,distance=0.1..20,team=1] run advancement grant @s only lbc:true_advancements/staffs/vampire`; [workingitems/lifestealer_adv.mcfunction:2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/lifestealer_adv.mcfunction) — `execute if entity @s[team=2] if entity @e[type=#minecraft:mobs,type=!player,distance=0.1..20,team=2] run advancement grant @s only lbc:true_advancements/staffs/vampire`; [workingitems/lifestealer_adv.mcfunction:3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/lifestealer_adv.mcfunction) — `execute if entity @s[team=3] if entity @e[type=#minecraft:mobs,type=!player,distance=0.1..20,team=3] run advancement grant @s only lbc:true_advancements/staffs/vampire`; [workingitems/lifestealer_adv.mcfunction:4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/lifestealer_adv.mcfunction) — `execute if entity @s[team=4] if entity @e[type=#minecraft:mobs,type=!player,distance=0.1..20,team=4] run advancement grant @s only lbc:true_advancements/staffs/vampire`; [workingitems/lifestealer_adv.mcfunction:5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/lifestealer_adv.mcfunction) — `execute if entity @s[team=5] if entity @e[type=#minecraft:mobs,type=!player,distance=0.1..20,team=5] run advancement grant @s only lbc:true_advancements/staffs/vampire`; [workingitems/lifestealer_adv.mcfunction:6](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/lifestealer_adv.mcfunction) — `execute if entity @s[team=6] if entity @e[type=#minecraft:mobs,type=!player,distance=0.1..20,team=6] run advancement grant @s only lbc:true_advancements/staffs/vampire`; [workingitems/lifestealer_adv.mcfunction:7](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/lifestealer_adv.mcfunction) — `execute if entity @s[team=7] if entity @e[type=#minecraft:mobs,type=!player,distance=0.1..20,team=7] run advancement grant @s only lbc:true_advancements/staffs/vampire`; [workingitems/lifestealer_adv.mcfunction:8](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/lifestealer_adv.mcfunction) — `execute if entity @s[team=8] if entity @e[type=#minecraft:mobs,type=!player,distance=0.1..20,team=8] run advancement grant @s only lbc:true_advancements/staffs/vampire`; [workingitems/lifestealer_adv.mcfunction:9](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/lifestealer_adv.mcfunction) — `execute if entity @s[team=9] if entity @e[type=#minecraft:mobs,type=!player,distance=0.1..20,team=9] run advancement grant @s only lbc:true_advancements/staffs/vampire`; [workingitems/lifestealer_adv.mcfunction:10](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/lifestealer_adv.mcfunction) — `execute if entity @s[team=10] if entity @e[type=#minecraft:mobs,type=!player,distance=0.1..20,team=10] run advancement grant @s only lbc:true_advancements/staffs/vampire`; [workingitems/lifestealer_adv.mcfunction:11](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/lifestealer_adv.mcfunction) — `execute if entity @s[team=11] if entity @e[type=#minecraft:mobs,type=!player,distance=0.1..20,team=11] run advancement grant @s only lbc:true_advancements/staffs/vampire`; [workingitems/lifestealer_adv.mcfunction:12](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/lifestealer_adv.mcfunction) — `execute if entity @s[team=12] if entity @e[type=#minecraft:mobs,type=!player,distance=0.1..20,team=12] run advancement grant @s only lbc:true_advancements/staffs/vampire`; [workingitems/lifestealer_adv.mcfunction:13](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/lifestealer_adv.mcfunction) — `execute if entity @s[team=13] if entity @e[type=#minecraft:mobs,type=!player,distance=0.1..20,team=13] run advancement grant @s only lbc:true_advancements/staffs/vampire`; [workingitems/lifestealer_adv.mcfunction:14](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/lifestealer_adv.mcfunction) — `execute if entity @s[team=14] if entity @e[type=#minecraft:mobs,type=!player,distance=0.1..20,team=14] run advancement grant @s only lbc:true_advancements/staffs/vampire`; [workingitems/lifestealer_adv.mcfunction:15](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/lifestealer_adv.mcfunction) — `execute if entity @s[team=15] if entity @e[type=#minecraft:mobs,type=!player,distance=0.1..20,team=15] run advancement grant @s only lbc:true_advancements/staffs/vampire`; [workingitems/lifestealer_adv.mcfunction:16](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/lifestealer_adv.mcfunction) — `execute if entity @s[team=16] if entity @e[type=#minecraft:mobs,type=!player,distance=0.1..20,team=16] run advancement grant @s only lbc:true_advancements/staffs/vampire`; [workingitems/lifestealer_adv.mcfunction:18](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/lifestealer_adv.mcfunction) — `execute if entity @s[team=17] if entity @e[type=#minecraft:mobs,type=!player,distance=0.1..20,team=17] run advancement grant @s only lbc:true_advancements/staffs/vampire`; [workingitems/lifestealer_adv.mcfunction:19](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/lifestealer_adv.mcfunction) — `execute if entity @s[team=18] if entity @e[type=#minecraft:mobs,type=!player,distance=0.1..20,team=18] run advancement grant @s only lbc:true_advancements/staffs/vampire`; [workingitems/lifestealer_adv.mcfunction:20](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/lifestealer_adv.mcfunction) — `execute if entity @s[team=19] if entity @e[type=#minecraft:mobs,type=!player,distance=0.1..20,team=19] run advancement grant @s only lbc:true_advancements/staffs/vampire`; [workingitems/lifestealer_adv.mcfunction:21](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/lifestealer_adv.mcfunction) — `execute if entity @s[team=20] if entity @e[type=#minecraft:mobs,type=!player,distance=0.1..20,team=20] run advancement grant @s only lbc:true_advancements/staffs/vampire`; [workingitems/lifestealer_adv.mcfunction:22](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/lifestealer_adv.mcfunction) — `execute if entity @s[team=21] if entity @e[type=#minecraft:mobs,type=!player,distance=0.1..20,team=21] run advancement grant @s only lbc:true_advancements/staffs/vampire`; [workingitems/lifestealer_adv.mcfunction:23](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/lifestealer_adv.mcfunction) — `execute if entity @s[team=22] if entity @e[type=#minecraft:mobs,type=!player,distance=0.1..20,team=22] run advancement grant @s only lbc:true_advancements/staffs/vampire`; [workingitems/lifestealer_adv.mcfunction:24](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/lifestealer_adv.mcfunction) — `execute if entity @s[team=23] if entity @e[type=#minecraft:mobs,type=!player,distance=0.1..20,team=23] run advancement grant @s only lbc:true_advancements/staffs/vampire`; [workingitems/lifestealer_adv.mcfunction:25](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/lifestealer_adv.mcfunction) — `execute if entity @s[team=24] if entity @e[type=#minecraft:mobs,type=!player,distance=0.1..20,team=24] run advancement grant @s only lbc:true_advancements/staffs/vampire`; [workingitems/lifestealer_adv.mcfunction:26](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/lifestealer_adv.mcfunction) — `execute if entity @s[team=25] if entity @e[type=#minecraft:mobs,type=!player,distance=0.1..20,team=25] run advancement grant @s only lbc:true_advancements/staffs/vampire`; [workingitems/lifestealer_adv.mcfunction:27](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/lifestealer_adv.mcfunction) — `execute if entity @s[team=26] if entity @e[type=#minecraft:mobs,type=!player,distance=0.1..20,team=26] run advancement grant @s only lbc:true_advancements/staffs/vampire`; [workingitems/lifestealer_adv.mcfunction:28](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/lifestealer_adv.mcfunction) — `execute if entity @s[team=27] if entity @e[type=#minecraft:mobs,type=!player,distance=0.1..20,team=27] run advancement grant @s only lbc:true_advancements/staffs/vampire`; [workingitems/lifestealer_adv.mcfunction:29](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/lifestealer_adv.mcfunction) — `execute if entity @s[team=28] if entity @e[type=#minecraft:mobs,type=!player,distance=0.1..20,team=28] run advancement grant @s only lbc:true_advancements/staffs/vampire`; [workingitems/lifestealer_adv.mcfunction:30](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/lifestealer_adv.mcfunction) — `execute if entity @s[team=29] if entity @e[type=#minecraft:mobs,type=!player,distance=0.1..20,team=29] run advancement grant @s only lbc:true_advancements/staffs/vampire`; [workingitems/lifestealer_adv.mcfunction:31](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/lifestealer_adv.mcfunction) — `execute if entity @s[team=30] if entity @e[type=#minecraft:mobs,type=!player,distance=0.1..20,team=30] run advancement grant @s only lbc:true_advancements/staffs/vampire`; [workingitems/lifestealer_adv.mcfunction:32](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/lifestealer_adv.mcfunction) — `execute if entity @s[team=31] if entity @e[type=#minecraft:mobs,type=!player,distance=0.1..20,team=31] run advancement grant @s only lbc:true_advancements/staffs/vampire`; [workingitems/lifestealer_adv.mcfunction:33](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/lifestealer_adv.mcfunction) — `execute if entity @s[team=32] if entity @e[type=#minecraft:mobs,type=!player,distance=0.1..20,team=32] run advancement grant @s only lbc:true_advancements/staffs/vampire`.

## `lbc:true_advancements/staffs/wall`

Источник: [lbc:true_advancements/staffs/wall](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/staffs/wall.json).
В дереве достижений: `lbc.adv.staff.wall`; описание: `lbc.adv.staff.wall.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:wallwand"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/staffs/badwiki`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [raycast_end2.mcfunction:10](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_end2.mcfunction) — `execute if entity @a[gamemode=!spectator,distance=..3,tag=!raycaster] run advancement grant @s only lbc:true_advancements/staffs/wall`; [raycast_end2_upgraded.mcfunction:9](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_end2_upgraded.mcfunction) — `execute if entity @a[gamemode=!spectator,distance=..3,tag=!raycaster] run advancement grant @s only lbc:true_advancements/staffs/wall`.

## `lbc:true_advancements/staffs`

Источник: [lbc:true_advancements/staffs](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/staffs.json).
В дереве достижений: `lbc.adv.staffs`; описание: ``. Параметры показа: `{"icon":{"id":"minecraft:blaze_rod"},"frame":"task","show_toast":true,"announce_to_chat":false,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/root`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable:1,staff:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/summoner/all_summons`

Источник: [lbc:true_advancements/summoner/all_summons](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/summoner/all_summons.json).
В дереве достижений: `lbc.adv.summoner.all`; описание: `lbc.adv.summoner.all.lore`. Параметры показа: `{"icon":{"id":"minecraft:ender_dragon_spawn_egg","components":{"minecraft:enchantment_glint_override":true}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/summoner`; это связь отображения, а не дополнительный критерий.
Критерий `summon_true`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable: 1,summoner: 1,summonerblaze: 1}"}}`).
Критерий `summon_2`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable: 1,summoner: 1,summonereliteskeletons: 1}"}}`).
Критерий `summon_3`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable: 1,summoner: 1,summonerghast: 1}"}}`).
Критерий `summon_4`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable: 1,summoner: 1,summonerghostrider: 1}"}}`).
Критерий `summon_5`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable: 1,summoner: 1,summonerguardians: 1}"}}`).
Критерий `summon_6`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable: 1,summoner: 1,summonerrazoritel: 1}"}}`).
Критерий `summon_7`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable: 1,summoner: 1,summonerskeletons: 1}"}}`).
Критерий `summon_8`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable: 1,summoner: 1,summonervexes: 1}"}}`).
Для завершения требуются все 8 критериев.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/summoner/penis_cultus`

Источник: [lbc:true_advancements/summoner/penis_cultus](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/summoner/penis_cultus.json).
В дереве достижений: `lbc.adv.summoner.penis_cultis`; описание: `lbc.adv.summoner.penis_cultis.lore`. Параметры показа: `{"icon":{"id":"minecraft:stick","components":{"minecraft:item_model":"lbc:penis_cultus"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/summoner/all_summons`; это связь отображения, а не дополнительный критерий.
Критерий `1`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [workingitems/penis_cultus/use.mcfunction:3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/penis_cultus/use.mcfunction) — `advancement grant @s only lbc:true_advancements/summoner/penis_cultus`.

## `lbc:true_advancements/summoner`

Источник: [lbc:true_advancements/summoner](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/summoner.json).
В дереве достижений: `lbc.adv.summoner`; описание: ``. Параметры показа: `{"icon":{"id":"minecraft:phantom_spawn_egg"},"frame":"task","show_toast":true,"announce_to_chat":false,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/root`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable:1,summoner:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/swords/anal_whip`

Источник: [lbc:true_advancements/swords/anal_whip](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/swords/anal_whip.json).
В дереве достижений: `lbc.adv.sword.anal_whip`; описание: `lbc.adv.sword.anal_whip.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:anal_whip"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/swords/antimagic`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [workingitems/ender_whip_teleport.mcfunction:8](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/ender_whip_teleport.mcfunction) — `execute at @a[gamemode=!spectator,tag=!tp] if score @a[gamemode=!spectator,distance=..0.001,tag=!tp,limit=1] lbcID = @s lbcID run advancement grant @s only lbc:true_advancements/swords/anal_whip`; [workingitems/ender_whip_teleport_left.mcfunction:8](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/ender_whip_teleport_left.mcfunction) — `execute at @a[gamemode=!spectator,tag=!tp] if score @a[gamemode=!spectator,distance=..0.001,tag=!tp,limit=1] lbcID = @s lbcID run advancement grant @s only lbc:true_advancements/swords/anal_whip`.

## `lbc:true_advancements/swords/antimagic`

Источник: [lbc:true_advancements/swords/antimagic](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/swords/antimagic.json).
В дереве достижений: `lbc.adv.sword.antimagic`; описание: `lbc.adv.sword.antimagic.lore`. Параметры показа: `{"icon":{"id":"minecraft:iron_sword","components":{"minecraft:item_model":"lbc:antimagic_stick"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/swords/cyberkatanamk2`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [players_1s.mcfunction:36](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/players_1s.mcfunction) — `execute at @s[nbt={SelectedItem:{components:{"minecraft:custom_data":{antimagic:1}}}}] if entity @e[type=minecraft:armor_stand,distance=..4,tag=mom,limit=1] unless score @e[type=minecraft:armor_stand,distance=..4,tag=mom,limit=1] lbcID2 = @s lbcID2 run advancement grant @s only lbc:true_advancements/swords/antimagic`.

## `lbc:true_advancements/swords/crowbar`

Источник: [lbc:true_advancements/swords/crowbar](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/swords/crowbar.json).
В дереве достижений: `lbc.adv.sword.crowbar`; описание: `lbc.adv.sword.crowbar.lore`. Параметры показа: `{"icon":{"id":"minecraft:iron_sword","components":{"minecraft:item_model":"lbc:crowbar"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/swords/fragment`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок убивает сущность. Условия: игрок: (свойства `this`: `{"equipment":{"mainhand":{"predicates":{"minecraft:custom_data":"{transformable: 1,sword: 1,crowbar: 1}"}}}}`); сущность: (свойства `this`: `{"type":"minecraft:player"}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/swords/cyberkatanamk2`

Источник: [lbc:true_advancements/swords/cyberkatanamk2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/swords/cyberkatanamk2.json).
В дереве достижений: `lbc.adv.sword.cyberkatanamk2`; описание: `lbc.adv.sword.cyberkatanamk2.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:techrunner_katana"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/swords/metal_pipe`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок наносит урон сущности. Условия: игрок: (свойства `this`: `{"equipment":{"mainhand":{"predicates":{"minecraft:custom_data":"{transformable: 1,sword: 1,techrunner_katana: 1}"}}}}`); урон: `{"dealt":{"min":150}}`.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/swords/devil`

Источник: [lbc:true_advancements/swords/devil](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/swords/devil.json).
В дереве достижений: `lbc.adv.sword.devil`; описание: `lbc.adv.sword.devil.lore`. Параметры показа: `{"icon":{"id":"minecraft:wooden_sword","components":{"minecraft:item_model":"lbc:devilsword"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/swords/slimeball`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [workingitems/devilswordattacks/6.mcfunction:1](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/devilswordattacks/6.mcfunction) — `advancement grant @s only lbc:true_advancements/swords/devil`.

## `lbc:true_advancements/swords/devil_full`

Источник: [lbc:true_advancements/swords/devil_full](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/swords/devil_full.json).
В дереве достижений: `lbc.adv.sword.devil_full`; описание: `lbc.adv.sword.devil_full.lore`. Параметры показа: `{"icon":{"id":"minecraft:wooden_sword","components":{"minecraft:item_model":"lbc:devilsword","minecraft:enchantment_glint_override":true}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/swords/devil`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable: 1,sword: 1,heal: 1}"}}`) И (`{"predicates":{"minecraft:custom_data":"{transformable: 1,totem: 1,deviltotem: 1}"}}`) И (`{"predicates":{"minecraft:custom_data":"{transformable: 1,staff: 1,bloodmagic: 1}"}}`) И (`{"predicates":{"minecraft:custom_data":"{transformable: 1,summoner: 1,summonerghast: 1}"}}`) И (`{"predicates":{"minecraft:custom_data":"{transformable: 1,accessory: 1,demonkishki: 1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/swords/electro`

Источник: [lbc:true_advancements/swords/electro](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/swords/electro.json).
В дереве достижений: `lbc.adv.sword.electro`; описание: `lbc.adv.sword.electro.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:cyberkatana"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/swords/evolution`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [workingitems/cyberkatana.mcfunction:11](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/cyberkatana.mcfunction) — `advancement grant @s[scores={swordcharged1=10..}] only lbc:true_advancements/swords/electro`.

## `lbc:true_advancements/swords/ender`

Источник: [lbc:true_advancements/swords/ender](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/swords/ender.json).
В дереве достижений: `lbc.adv.sword.ender`; описание: `lbc.adv.sword.ender.lore`. Параметры показа: `{"icon":{"id":"minecraft:netherite_sword","components":{"minecraft:item_model":"lbc:endersword"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/swords/steak`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок убивает сущность. Условия: игрок: (свойства `this`: `{"equipment":{"mainhand":{"predicates":{"minecraft:custom_data":"{transformable: 1,sword: 1,ender: 1}"}}}}`); сущность: (свойства `this`: `{"type":"minecraft:player"}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/swords/epic`

Источник: [lbc:true_advancements/swords/epic](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/swords/epic.json).
В дереве достижений: `lbc.adv.sword.epic`; описание: `lbc.adv.sword.epic.lore`. Параметры показа: `{"icon":{"id":"minecraft:golden_sword","components":{"minecraft:item_model":"lbc:epicsword"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/swords/vampire`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок убивает сущность. Условия: игрок: (свойства `this`: `{"equipment":{"mainhand":{"predicates":{"minecraft:custom_data":"{transformable: 1,sword: 1,cool: 1}"}}}}`); сущность: (свойства `this`: `{"type":"minecraft:player"}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/swords/evolution`

Источник: [lbc:true_advancements/swords/evolution](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/swords/evolution.json).
В дереве достижений: `lbc.adv.sword.evolution`; описание: `lbc.adv.sword.evolution.lore`. Параметры показа: `{"icon":{"id":"minecraft:golden_sword","components":{"minecraft:item_model":"lbc:evolutionsword"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/swords/ice`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок наносит урон сущности. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)) И (свойства `this`: `{"equipment":{"mainhand":{"predicates":{"minecraft:custom_data":"{transformable: 1,sword: 1,evolution: 1}"}}}}`); урон: `{"dealt":{"min":7},"blocked":false}`.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/swords/evolution_1000`

Источник: [lbc:true_advancements/swords/evolution_1000](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/swords/evolution_1000.json).
В дереве достижений: `lbc.adv.sword.evolution_1000`; описание: `lbc.adv.sword.evolution_1000.lore`. Параметры показа: `{"icon":{"id":"minecraft:golden_sword","components":{"minecraft:item_model":"lbc:evolutionsword"}},"frame":"challenge","show_toast":true,"announce_to_chat":true}`.
Родитель в дереве: `lbc:true_advancements/swords/evolution`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок наносит урон сущности. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)) И (свойства `this`: `{"equipment":{"mainhand":{"predicates":{"minecraft:custom_data":"{transformable: 1,sword: 1,evolution: 1}"}}}}`); урон: `{"dealt":{"min":1000},"taken":{"min":1000},"blocked":false}`.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/swords/evolution_322124`

Источник: [lbc:true_advancements/swords/evolution_322124](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/swords/evolution_322124.json).
В дереве достижений: `lbc.adv.sword.evolution_322124`; описание: `lbc.adv.sword.evolution_322124.lore`. Параметры показа: `{"icon":{"id":"minecraft:golden_sword","components":{"minecraft:item_model":"lbc:evolutionsword","minecraft:enchantment_glint_override":true}},"frame":"challenge","show_toast":true,"announce_to_chat":true}`.
Родитель в дереве: `lbc:true_advancements/swords/evolution_1000`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок наносит урон сущности. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)) И (свойства `this`: `{"equipment":{"mainhand":{"predicates":{"minecraft:custom_data":"{transformable: 1,sword: 1,evolution: 1}"}}}}`); урон: `{"dealt":{"min":322124},"blocked":false}`.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/swords/evolution_max`

Источник: [lbc:true_advancements/swords/evolution_max](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/swords/evolution_max.json).
В дереве достижений: `lbc.adv.sword.evolution_max`; описание: `lbc.adv.sword.evolution_max.lore`. Параметры показа: `{"icon":{"id":"minecraft:golden_sword","components":{"minecraft:item_model":"lbc:evolutionsword","minecraft:enchantment_glint_override":true}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/swords/evolution_322124`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок наносит урон сущности. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)) И (свойства `this`: `{"equipment":{"mainhand":{"predicates":{"minecraft:custom_data":"{transformable: 1,sword: 1,evolution: 1}"}}}}`); урон: `{"taken":{"min":1000000},"blocked":false}`.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/swords/fevgalea_star`

Источник: [lbc:true_advancements/swords/fevgalea_star](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/swords/fevgalea_star.json).
В дереве достижений: `lbc.adv.sword.fevgalea_star`; описание: `lbc.adv.sword.fevgalea_star.lore`. Параметры показа: `{"icon":{"id":"minecraft:golden_sword","components":{"minecraft:item_model":"lbc:fevgalea_star"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/swords/piston_sword`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок наносит урон сущности. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)) И (свойства `this`: `{"equipment":{"mainhand":{"predicates":{"minecraft:custom_data":"{transformable: 1,sword: 1,fevgalea_star:1}"}}}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/swords/fragment`

Источник: [lbc:true_advancements/swords/fragment](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/swords/fragment.json).
В дереве достижений: `lbc.adv.sword.fragment`; описание: `lbc.adv.sword.fragment.lore`. Параметры показа: `{"icon":{"id":"minecraft:golden_sword","components":{"minecraft:item_model":"lbc:fragmentsword"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/swords/terra`; это связь отображения, а не дополнительный критерий.
Критерий `cool_1`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable: 1,sword: 1,fragmentsword: 1,cooltype: 0}"}}`).
Критерий `cool_2`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable: 1,sword: 1,fragmentsword: 1,cooltype: 1}"}}`).
Критерий `cool_3`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable: 1,sword: 1,fragmentsword: 1,cooltype: 2}"}}`).
Критерий `cool_4`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable: 1,sword: 1,fragmentsword: 1,cooltype: 3}"}}`).
Критерий `cool_5`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable: 1,sword: 1,fragmentsword: 1,cooltype: 4}"}}`).
Критерий `cool_6`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable: 1,sword: 1,fragmentsword: 1,cooltype: 5}"}}`).
Критерий `cool_7`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable: 1,sword: 1,fragmentsword: 1,cooltype: 6}"}}`).
Критерий `cool_8`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable: 1,sword: 1,fragmentsword: 1,cooltype: 7}"}}`).
Критерий `cool_9`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable: 1,sword: 1,fragmentsword: 1,cooltype: 8}"}}`).
Критерий `cool_10`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable: 1,sword: 1,fragmentsword: 1,cooltype: 9}"}}`).
Критерий `cool_11`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable: 1,sword: 1,fragmentsword: 1,cooltype: 10}"}}`).
Критерий `cool_12`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable: 1,sword: 1,fragmentsword: 1,cooltype: 11}"}}`).
Критерий `cool_13`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable: 1,sword: 1,fragmentsword: 1,cooltype: 12}"}}`).
Критерий `cool_14`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable: 1,sword: 1,fragmentsword: 1,cooltype: 13}"}}`).
Критерий `cool_15`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable: 1,sword: 1,fragmentsword: 1,cooltype: 14}"}}`).
Критерий `cool_16`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable: 1,sword: 1,fragmentsword: 1,cooltype: 15}"}}`).
Критерий `cool_17`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable: 1,sword: 1,fragmentsword: 1,cooltype: 16}"}}`).
Для завершения требуются все 17 критериев.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/swords/gravity`

Источник: [lbc:true_advancements/swords/gravity](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/swords/gravity.json).
В дереве достижений: `lbc.adv.sword.gravity`; описание: `lbc.adv.sword.gravity.lore`. Параметры показа: `{"icon":{"id":"minecraft:iron_sword","components":{"minecraft:item_model":"lbc:gravitysword"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/swords/ender`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок убивает сущность. Условия: игрок: (свойства `this`: `{"equipment":{"mainhand":{"predicates":{"minecraft:custom_data":"{transformable: 1,sword: 1,gravitationsword: 1}"}}}}`); сущность: (свойства `this`: `{"type":"minecraft:player"}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/swords/guardian`

Источник: [lbc:true_advancements/swords/guardian](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/swords/guardian.json).
В дереве достижений: `lbc.adv.sword.guardian`; описание: `lbc.adv.sword.guardian.lore`. Параметры показа: `{"icon":{"id":"minecraft:iron_sword","components":{"minecraft:item_model":"lbc:guardiansword"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/swords/gravity`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок наносит урон сущности. Условия: игрок: (свойства `this`: `{"equipment":{"mainhand":{"predicates":{"minecraft:custom_data":"{transformable:1,sword:1,seaguardian:1}"}}},"flags":{"is_swimming":true}}`); сущность: (свойства `this`: `{"type":"minecraft:player"}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/swords/hylian`

Источник: [lbc:true_advancements/swords/hylian](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/swords/hylian.json).
В дереве достижений: `lbc.adv.sword.hylian`; описание: `lbc.adv.sword.hylian.lore`. Параметры показа: `{"icon":{"id":"minecraft:netherite_sword","components":{"minecraft:item_model":"lbc:hylian_sword"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/swords/randchanted`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок убивает сущность. Условия: игрок: (свойства `this`: `{"equipment":{"mainhand":{"predicates":{"minecraft:custom_data":"{transformable: 1,sword: 1,hylian: 1}"}}}}`); сущность: (свойства `this`: `{"type":"#minecraft:unholy_monsters"}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/swords/hylian_full`

Источник: [lbc:true_advancements/swords/hylian_full](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/swords/hylian_full.json).
В дереве достижений: `lbc.adv.sword.hylian_full`; описание: `lbc.adv.sword.hylian_full.lore`. Параметры показа: `{"icon":{"id":"minecraft:netherite_sword","components":{"minecraft:enchantment_glint_override":true,"minecraft:item_model":"lbc:hylian_sword"}},"frame":"challenge","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/swords/hylian`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable: 1,sword: 1,hylian: 1}"}}`) И (`{"predicates":{"minecraft:custom_data":"{transformable: 1,shield: 1,hylianshield: 1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/swords/ice`

Источник: [lbc:true_advancements/swords/ice](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/swords/ice.json).
В дереве достижений: `lbc.adv.sword.ice`; описание: `lbc.adv.sword.ice.lore`. Параметры показа: `{"icon":{"id":"minecraft:iron_sword","components":{"minecraft:item_model":"lbc:icesword"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/swords/guardian`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [other/freeze/full.mcfunction:9](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/freeze/full.mcfunction) — `execute if entity @s[type=player,nbt={HurtTime:10s}] run advancement grant @p[distance=0.001..7,advancements={lbc:gowno=true}] only lbc:true_advancements/swords/ice`.

## `lbc:true_advancements/swords/metal_pipe`

Источник: [lbc:true_advancements/swords/metal_pipe](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/swords/metal_pipe.json).
В дереве достижений: `lbc.adv.sword.metal_pipe`; описание: `lbc.adv.sword.metal_pipe.lore`. Параметры показа: `{"icon":{"id":"minecraft:spyglass","components":{"minecraft:item_model":"lbc:metal_pipe"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/swords/vex`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок наносит урон сущности. Условия: игрок: (свойства `this`: `{"equipment":{"mainhand":{"predicates":{"minecraft:custom_data":"{transformable: 1,sword: 1,metal_pipe: 1}"}}}}`); сущность: (свойства `this`: `{"type":"minecraft:player"}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/swords/one_punch`

Источник: [lbc:true_advancements/swords/one_punch](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/swords/one_punch.json).
В дереве достижений: `lbc.adv.sword.one_punch`; описание: `lbc.adv.sword.one_punch.lore`. Параметры показа: `{"icon":{"id":"minecraft:iron_sword","components":{"minecraft:item_model":"lbc:one_punch_glove"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/swords/crowbar`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок наносит урон сущности. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)) И (свойства `this`: `{"equipment":{"mainhand":{"predicates":{"minecraft:custom_data":"{transformable: 1,sword: 1,one_punch: 1}"}}}}`); сущность: (свойства `this`: `{"type":"minecraft:player"}`); урон: `{"dealt":{"min":40},"blocked":false}`.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/swords/piston_sword`

Источник: [lbc:true_advancements/swords/piston_sword](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/swords/piston_sword.json).
В дереве достижений: `lbc.adv.sword.piston_sword`; описание: `lbc.adv.sword.piston_sword.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:piston_sword"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/swords/anal_whip`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок убивает сущность. Условия: игрок: (свойства `this`: `{"equipment":{"mainhand":{"predicates":{"minecraft:custom_data":"{transformable: 1,sword: 1,piston_sword: 1}"}}}}`); сущность: (свойства `this`: `{"type":"minecraft:player"}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/swords/randchanted`

Источник: [lbc:true_advancements/swords/randchanted](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/swords/randchanted.json).
В дереве достижений: `lbc.adv.sword.randchanted`; описание: `lbc.adv.sword.randchanted.lore`. Параметры показа: `{"icon":{"id":"minecraft:iron_sword","components":{"minecraft:enchantment_glint_override":true}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/swords`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:enchantments":[{"enchantments":"minecraft:sharpness","levels":{"min":4}},{"enchantments":"minecraft:knockback","levels":2},{"enchantments":"minecraft:fire_aspect","levels":2}],"minecraft:custom_data":"{transformable: 1,sword: 1,randchanted: 1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/swords/shit_katana`

Источник: [lbc:true_advancements/swords/shit_katana](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/swords/shit_katana.json).
В дереве достижений: `lbc.adv.sword.shit_katana`; описание: `lbc.adv.sword.shit_katana.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:shit_katana"}},"frame":"goal","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/swords/electro`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable:1,sword:1,shit_katana:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/swords/slimeball`

Источник: [lbc:true_advancements/swords/slimeball](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/swords/slimeball.json).
В дереве достижений: `lbc.adv.sword.slimeball`; описание: `lbc.adv.sword.slimeball.lore`. Параметры показа: `{"icon":{"id":"minecraft:slime_ball","components":{"minecraft:enchantment_glint_override":true}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/swords/hylian`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable:1,sword:1,slimeball:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/swords/steak`

Источник: [lbc:true_advancements/swords/steak](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/swords/steak.json).
В дереве достижений: `lbc.adv.sword.steak`; описание: `lbc.adv.sword.steak.lore`. Параметры показа: `{"icon":{"id":"minecraft:wooden_sword","components":{"minecraft:item_model":"lbc:steaksword"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/swords/epic`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется повреждение предмета. Условия: прочность: `{"max":0}`; игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предмет: `{"predicates":{"minecraft:custom_data":"{transformable:1,sword:1,feeding:1}"}}`.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/swords/terra`

Источник: [lbc:true_advancements/swords/terra](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/swords/terra.json).
В дереве достижений: `lbc.adv.sword.terra`; описание: `lbc.adv.sword.terra.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:terrasword"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/swords/electro`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок убивает сущность. Условия: игрок: (свойства `this`: `{"equipment":{"mainhand":{"predicates":{"minecraft:custom_data":"{transformable:1,sword:1,terrasword:1}"}}}}`); сущность: (свойства `this`: `{"type":"minecraft:player"}`); killing_blow: `{"tags":[{"id":"minecraft:cactus_only","expected":false}]}`.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/swords/vampire`

Источник: [lbc:true_advancements/swords/vampire](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/swords/vampire.json).
В дереве достижений: `lbc.adv.sword.vampire`; описание: `lbc.adv.sword.vampire.lore`. Параметры показа: `{"icon":{"id":"minecraft:iron_sword","components":{"minecraft:item_model":"lbc:vampiresword"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/swords/devil`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок убивает сущность. Условия: игрок: (свойства `this`: `{"equipment":{"mainhand":{"predicates":{"minecraft:custom_data":"{transformable:1,sword:1,vampire:1}"}}}}`); сущность: (свойства `this`: `{"type":"#minecraft:no_blood"}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/swords/vex`

Источник: [lbc:true_advancements/swords/vex](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/swords/vex.json).
В дереве достижений: `lbc.adv.sword.vex`; описание: `lbc.adv.sword.vex.lore`. Параметры показа: `{"icon":{"id":"minecraft:carrot_on_a_stick","components":{"minecraft:item_model":"lbc:vex_sword"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/swords/one_punch`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable: 1,sword: 1,vexsword: 1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/swords`

Источник: [lbc:true_advancements/swords](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/swords.json).
В дереве достижений: `lbc.adv.swords`; описание: ``. Параметры показа: `{"icon":{"id":"minecraft:golden_sword"},"frame":"task","show_toast":true,"announce_to_chat":false,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/root`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable:1,sword:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/totems/devil`

Источник: [lbc:true_advancements/totems/devil](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/totems/devil.json).
В дереве достижений: `lbc.adv.totem.devil`; описание: `lbc.adv.totem.devil.lore`. Параметры показа: `{"icon":{"id":"minecraft:totem_of_undying","components":{"minecraft:item_model":"lbc:deviltotem"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/totems/infinite`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: автоматического события нет; требуется выдача advancement командами. Условия: дополнительных ограничений нет.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [other/deviltotem.mcfunction:1](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/deviltotem.mcfunction) — `advancement grant @s only lbc:true_advancements/totems/devil`.

## `lbc:true_advancements/totems/infinite`

Источник: [lbc:true_advancements/totems/infinite](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/totems/infinite.json).
В дереве достижений: `lbc.adv.totem.infinite`; описание: `lbc.adv.totem.infinite.lore`. Параметры показа: `{"icon":{"id":"minecraft:totem_of_undying","components":{"minecraft:enchantment_glint_override":true,"item_model":"lbc:infinite_totem"}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/totems`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок спасается тотемом. Условия: предмет: `{"predicates":{"minecraft:enchantments":[{"enchantments":"minecraft:infinity","levels":{"min":1}}],"minecraft:custom_data":"{transformable: 1,totem: 1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/totems`

Источник: [lbc:true_advancements/totems](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/totems.json).
В дереве достижений: `lbc.adv.totem`; описание: ``. Параметры показа: `{"icon":{"id":"minecraft:totem_of_undying"},"frame":"task","show_toast":true,"announce_to_chat":false,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/root`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable:1,totem:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/tridents/harpoon`

Источник: [lbc:true_advancements/tridents/harpoon](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/tridents/harpoon.json).
В дереве достижений: `lbc.adv.trident.harpoon`; описание: `lbc.adv.trident.harpoon.lore`. Параметры показа: `{"icon":{"id":"minecraft:trident","components":{"minecraft:enchantment_glint_override":true}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/tridents/sea_legend`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок убивает сущность. Условия: игрок: (свойства `this`: `{"equipment":{"mainhand":{"predicates":{"minecraft:custom_data":"{transformable:1,trident:1,harpoon:1}"}}}}`); сущность: (свойства `this`: `{"type":"minecraft:player"}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/tridents/sea_legend`

Источник: [lbc:true_advancements/tridents/sea_legend](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/tridents/sea_legend.json).
В дереве достижений: `lbc.adv.trident.sea_legend`; описание: `lbc.adv.trident.sea_legend.lore`. Параметры показа: `{"icon":{"id":"minecraft:trident","components":{"minecraft:enchantment_glint_override":true}},"frame":"task","show_toast":true,"announce_to_chat":true,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/tridents`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: игрок продолжает использовать предмет. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предмет: `{"predicates":{"minecraft:custom_data":"{transformable:1,trident:1,sea_legend:1}"}}`.
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:true_advancements/tridents`

Источник: [lbc:true_advancements/tridents](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/true_advancements/tridents.json).
В дереве достижений: `lbc.adv.tridents`; описание: ``. Параметры показа: `{"icon":{"id":"minecraft:trident"},"frame":"task","show_toast":true,"announce_to_chat":false,"hidden":false}`.
Родитель в дереве: `lbc:true_advancements/root`; это связь отображения, а не дополнительный критерий.
Критерий `requirement`: меняется инвентарь игрока. Условия: игрок: (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)); предметы: (`{"predicates":{"minecraft:custom_data":"{transformable:1,trident:1}"}}`).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальных команд выдачи/отзыва этого точного ID в исследуемых функциях не найдено.

## `lbc:use_chaos_hoe`

Источник: [lbc:use_chaos_hoe](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/use_chaos_hoe.json).
Критерий `requirement`: меняется повреждение предмета. Условия: игрок: (свойства `this`: `{"nbt":"{SelectedItem:{components:{\"minecraft:custom_data\":{chaos_hoe:1}}}}"}`) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)) И (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)).
Наградной функции, опыта, рецептов и лута в JSON нет; ресурс сохраняет факт выполнения.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:133](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:use_chaos_hoe`; [other/imba/chaos_hoe_use.mcfunction:4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/chaos_hoe_use.mcfunction) — `advancement revoke @s only lbc:use_chaos_hoe`.

## `lbc:use_nahida`

Источник: [lbc:use_nahida](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/use_nahida.json).
Критерий `requirement`: игрок продолжает использовать предмет. Условия: предмет: `{"predicates":{"custom_data":"{nahida_dakimakura:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:other/imba/cunny_slab/cunnys/5_plapping](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/cunnys/5_plapping.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [players_tick.mcfunction:68](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/players_tick.mcfunction) — `advancement revoke @s only lbc:use_nahida`.

## `lbc:used_chaos_pic`

Источник: [lbc:used_chaos_pic](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/used_chaos_pic.json).
Критерий `requirement`: меняется повреждение предмета. Условия: игрок: (свойства `this`: `{"nbt":"{SelectedItem:{components:{\"minecraft:custom_data\":{chaos_pic:1}}}}"}`) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)) И (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)).
Награда: выполнить [lbc:other/imba/chaos_pic_use](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/chaos_pic_use.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:131](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:used_chaos_pic`; [other/imba/chaos_pic_use.mcfunction:8](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/chaos_pic_use.mcfunction) — `advancement revoke @s only lbc:used_chaos_pic`.

## `lbc:used_imba_shovel`

Источник: [lbc:used_imba_shovel](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/used_imba_shovel.json).
Критерий `requirement`: меняется повреждение предмета. Условия: игрок: (свойства `this`: `{"nbt":"{SelectedItem:{components:{\"minecraft:custom_data\":{chaos_sho:1}}}}"}`) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)) И (НЕ (свойства `this`: `{"nbt":"{playerGameType:3}"}`)).
Награда: выполнить [lbc:other/imba/chaos_sho_use](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/chaos_sho_use.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [leave_death.mcfunction:132](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction) — `advancement revoke @s only lbc:used_imba_shovel`; [other/imba/chaos_sho_use.mcfunction:437](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/chaos_sho_use.mcfunction) — `advancement revoke @s only lbc:used_imba_shovel`.

## `lbc:using_item`

Источник: [lbc:using_item](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/using_item.json).
Критерий `requirement`: игрок продолжает использовать предмет. Условия: предмет: `{"predicates":{"custom_data":"{tmt_item:1}"}}`; игрок: (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:nexus"}}`)) И (НЕ (свойства `this`: `{"location":{"dimension":"minecraft:imprinted"}}`)).
Награда: выполнить [lbc:items/tmt_item](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/tmt_item.mcfunction) от лица игрока.
Буквальные команды выдачи/отзыва: [items/tmt_item.mcfunction:3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/tmt_item.mcfunction) — `advancement revoke @s only lbc:using_item`.

