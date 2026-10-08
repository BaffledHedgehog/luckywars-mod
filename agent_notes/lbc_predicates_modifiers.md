# Проверки и изменения предметов LBC

Predicate возвращает да/нет для контекста вызова; он сам не применяет эффект. Item modifier меняет уже существующий предмет в слоте, выбранном вызывающей командой. Если modifier представляет массив, изменения выполняются по порядку. Условие функции проверяется перед её применением. Ниже сохранены все числа, слоты, метки и параметры.

Для атрибутов в этих JSON указана операция `add_value`: она прибавляет заданное число напрямую. В 36 шагах `set_attributes` всего 58 amount: 37 providers `minecraft:constant`, 14 `minecraft:score`, один `minecraft:storage` и 6 буквальных чисел. Случайных `uniform` providers здесь нет. Score/storage берут значение указанного источника и применяют `scale`, если он задан. В 34 шагах `replace` отсутствует, в двух задано `true`; все 36 заменяют текущий список модификаторов атрибутов предмета. У `set_count` и `set_enchantments` поле `add` отдельно определяет прибавление вместо установки; в карточках сохранены буквальные параметры.

Ссылки показывают буквальное использование ID в исследуемых функциях и JSON. Одинаковое имя в разных registry может встречаться и для другого ресурса; точный слот/контекст следует из команды в источнике. Сгенерированные макросами ссылки отдельно исследуются владельцами функций.

## item_modifier `lbc:add_armor`

Источник: [lbc:add_armor](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/add_armor.json).
Шаг 1: задать модификаторы атрибутов `{"modifiers":[{"attribute":"minecraft:armor","amount":{"type":"minecraft:score","target":{"type":"minecraft:fixed","name":"#tmp4"},"score":"lbc.math","scale":0.0009},"operation":"add_value","id":"minecraft:71eaf780-80f9-4861-8a64-49ab1247121b","slot":"head"},{"attribute":"minecraft:armor_toughness","amount":{"type":"minecraft:score","target":{"type":"minecraft:fixed","name":"#tmp4"},"score":"lbc.math","scale":0.0009},"operation":"add_value","id":"minecraft:bafd952e-692a-41ed-9b61-c7d604d107c9","slot":"head"}]}`.
Шаг 2: не менять компоненты (пустой список).
Шаг 3: добавить/перезаписать метки custom_data `{tanner_upgraded:1}`.
Буквальных ссылок на этот точный ID в исследуемых функциях/JSON не найдено; ресурс доступен при ручном или динамическом вызове.

## item_modifier `lbc:add_armor1`

Источник: [lbc:add_armor1](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/add_armor1.json).
Шаг 1: модификаторы атрибутов и активные слоты: `[{"type":"minecraft:armor_toughness","slot":"feet","id":"armor_toughness_feet","amount":1.5,"operation":"add_value"}]`.
Шаг 2: добавить/перезаписать метки custom_data `{tanner_upgraded:1}`.
Буквальных ссылок на этот точный ID в исследуемых функциях/JSON не найдено; ресурс доступен при ручном или динамическом вызове.

## item_modifier `lbc:add_armor2`

Источник: [lbc:add_armor2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/add_armor2.json).
Шаг 1: модификаторы атрибутов и активные слоты: `[{"type":"minecraft:armor_toughness","slot":"legs","id":"armor_toughnessnyazag","amount":1.5,"operation":"add_value"}]`.
Шаг 2: добавить/перезаписать метки custom_data `{tanner_upgraded:1}`.
Буквальных ссылок на этот точный ID в исследуемых функциях/JSON не найдено; ресурс доступен при ручном или динамическом вызове.

## item_modifier `lbc:add_armor3`

Источник: [lbc:add_armor3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/add_armor3.json).
Шаг 1: модификаторы атрибутов и активные слоты: `[{"type":"minecraft:armor_toughness","slot":"chest","id":"armor_thdfhhfghgh","amount":1.5,"operation":"add_value"}]`.
Шаг 2: добавить/перезаписать метки custom_data `{tanner_upgraded:1}`.
Буквальных ссылок на этот точный ID в исследуемых функциях/JSON не найдено; ресурс доступен при ручном или динамическом вызове.

## item_modifier `lbc:add_armor4`

Источник: [lbc:add_armor4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/add_armor4.json).
Шаг 1: модификаторы атрибутов и активные слоты: `[{"type":"minecraft:armor_toughness","slot":"head","id":"jjfjgfjfjyfjyfjyfjyfjs","amount":1.5,"operation":"add_value"}]`.
Шаг 2: добавить/перезаписать метки custom_data `{tanner_upgraded:1}`.
Буквальных ссылок на этот точный ID в исследуемых функциях/JSON не найдено; ресурс доступен при ручном или динамическом вызове.

## item_modifier `lbc:add_damage`

Источник: [lbc:add_damage](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/add_damage.json).
Шаг 1: задать модификаторы атрибутов `{"modifiers":[{"attribute":"minecraft:attack_damage","id":"minecraft:base_attack_damage","amount":{"type":"minecraft:score","target":"this","score":"attack_damage","scale":0.0001},"operation":"add_value","slot":"mainhand"},{"attribute":"minecraft:attack_speed","id":"minecraft:base_attack_speed","amount":-2.4,"operation":"add_value","slot":"mainhand"}]}`.
Буквальные ссылки на ID: [lbc/function/workingitems/evolution.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/evolution.mcfunction).

## item_modifier `lbc:add_lore_rocket`

Источник: [lbc:add_lore_rocket](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/add_lore_rocket.json).
Шаг 1: задать строки описания `lbckit.21.lore.1lbckit.21.lore.2lbckit.21.lore.3`; параметры `{"mode":"append","lore":[{"translate":"lbckit.21.lore.1","italic":false,"color":"green"},{"translate":"lbckit.21.lore.2","italic":false,"color":"gray"},{"translate":"lbckit.21.lore.3","italic":false,"color":"gray"}]}`.
Шаг 2: добавить/перезаписать метки custom_data `{"PublicBukkitValues": {"lws:item": {"lws:additionals": ["rocket_armor"]}},"explosive_feets": 1}`.
Шаг 3: возможность планирования: `{}`.
Буквальные ссылки на ID: [lbc/function/other/craft/rocket_boots_add_ability.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/rocket_boots_add_ability.mcfunction), [lbc/function/other/craft/rocket_boots_add_ability_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/rocket_boots_add_ability_inv.mcfunction).

## item_modifier `lbc:add_upgrade_desc`

Источник: [lbc:add_upgrade_desc](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/add_upgrade_desc.json).
Шаг 1: задать строки описания `upgraded_item`; параметры `{"mode":"append","lore":[{"translate":"upgraded_item","italic":false,"color":"green"}]}`.
Буквальные ссылки на ID: [lbc/function/other/magic_academy/interacted_upgrade.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_upgrade.mcfunction).

## item_modifier `lbc:append_upgradable`

Источник: [lbc:append_upgradable](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/append_upgradable.json).
Шаг 1: задать строки описания `upgraded_item`; параметры `{"mode":"append","lore":[{"translate":"upgraded_item","italic":false,"color":"green"}]}`.
Буквальных ссылок на этот точный ID в исследуемых функциях/JSON не найдено; ресурс доступен при ручном или динамическом вызове.

## item_modifier `lbc:attributefix/chainmail_boots`

Источник: [lbc:attributefix/chainmail_boots](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/attributefix/chainmail_boots.json).
Шаг 1: задать модификаторы атрибутов `{"modifiers":[{"attribute":"minecraft:armor","amount":{"type":"minecraft:constant","value":1},"operation":"add_value","id":"minecraft:bafd952e-692a-41ed-9b61-c7d604d107c9","slot":"feet"}]}`.
Буквальные ссылки на ID: [lbc/function/items/changed_inventory/fix_attribute_feet.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/changed_inventory/fix_attribute_feet.mcfunction).

## item_modifier `lbc:attributefix/chainmail_chestplate`

Источник: [lbc:attributefix/chainmail_chestplate](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/attributefix/chainmail_chestplate.json).
Шаг 1: задать модификаторы атрибутов `{"modifiers":[{"attribute":"minecraft:armor","amount":{"type":"minecraft:constant","value":5},"operation":"add_value","id":"minecraft:46e426d6-c51a-4703-b10e-b18a85e03659","slot":"chest"}]}`.
Буквальные ссылки на ID: [lbc/function/items/changed_inventory/fix_attribute_chest.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/changed_inventory/fix_attribute_chest.mcfunction).

## item_modifier `lbc:attributefix/chainmail_helmet`

Источник: [lbc:attributefix/chainmail_helmet](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/attributefix/chainmail_helmet.json).
Шаг 1: задать модификаторы атрибутов `{"modifiers":[{"attribute":"minecraft:armor","amount":{"type":"minecraft:constant","value":2},"operation":"add_value","id":"minecraft:0af55a83-3f8f-4494-b9e2-1e6cccd2c450","slot":"head"}]}`.
Буквальные ссылки на ID: [lbc/function/items/changed_inventory/fix_attribute_head.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/changed_inventory/fix_attribute_head.mcfunction).

## item_modifier `lbc:attributefix/chainmail_leggings`

Источник: [lbc:attributefix/chainmail_leggings](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/attributefix/chainmail_leggings.json).
Шаг 1: задать модификаторы атрибутов `{"modifiers":[{"attribute":"minecraft:armor","amount":{"type":"minecraft:constant","value":4},"operation":"add_value","id":"minecraft:e6f36f87-7802-411f-8688-056321a5f6a7","slot":"legs"}]}`.
Буквальные ссылки на ID: [lbc/function/items/changed_inventory/fix_attribute_legs.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/changed_inventory/fix_attribute_legs.mcfunction).

## item_modifier `lbc:attributefix/diamond_boots`

Источник: [lbc:attributefix/diamond_boots](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/attributefix/diamond_boots.json).
Шаг 1: задать модификаторы атрибутов `{"modifiers":[{"attribute":"minecraft:armor","amount":{"type":"minecraft:constant","value":3},"operation":"add_value","id":"minecraft:bafd952e-692a-41ed-9b61-c7d604d107c9","slot":"feet"},{"attribute":"minecraft:armor_toughness","amount":{"type":"minecraft:constant","value":2},"operation":"add_value","id":"minecraft:bdac1db5-1ef4-45c4-96f2-ff4700331454","slot":"feet"}]}`.
Буквальные ссылки на ID: [lbc/function/items/changed_inventory/fix_attribute_feet.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/changed_inventory/fix_attribute_feet.mcfunction).

## item_modifier `lbc:attributefix/diamond_chestplate`

Источник: [lbc:attributefix/diamond_chestplate](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/attributefix/diamond_chestplate.json).
Шаг 1: задать модификаторы атрибутов `{"modifiers":[{"attribute":"minecraft:armor","amount":{"type":"minecraft:constant","value":8},"operation":"add_value","id":"minecraft:46e426d6-c51a-4703-b10e-b18a85e03659","slot":"chest"},{"attribute":"minecraft:armor_toughness","amount":{"type":"minecraft:constant","value":2},"operation":"add_value","id":"minecraft:b03ad1b6-0e49-49f3-8d61-b9b26cd19b81","slot":"chest"}]}`.
Буквальные ссылки на ID: [lbc/function/items/changed_inventory/fix_attribute_chest.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/changed_inventory/fix_attribute_chest.mcfunction).

## item_modifier `lbc:attributefix/diamond_helmet`

Источник: [lbc:attributefix/diamond_helmet](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/attributefix/diamond_helmet.json).
Шаг 1: задать модификаторы атрибутов `{"modifiers":[{"attribute":"minecraft:armor","amount":{"type":"minecraft:constant","value":3},"operation":"add_value","id":"minecraft:0af55a83-3f8f-4494-b9e2-1e6cccd2c450","slot":"head"},{"attribute":"minecraft:armor_toughness","amount":{"type":"minecraft:constant","value":2},"operation":"add_value","id":"minecraft:56105362-945a-4be7-a6c0-61d80ef41d2c","slot":"head"}]}`.
Буквальные ссылки на ID: [lbc/function/items/changed_inventory/fix_attribute_head.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/changed_inventory/fix_attribute_head.mcfunction).

## item_modifier `lbc:attributefix/diamond_leggings`

Источник: [lbc:attributefix/diamond_leggings](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/attributefix/diamond_leggings.json).
Шаг 1: задать модификаторы атрибутов `{"modifiers":[{"attribute":"minecraft:armor","amount":{"type":"minecraft:constant","value":6},"operation":"add_value","id":"minecraft:e6f36f87-7802-411f-8688-056321a5f6a7","slot":"legs"},{"attribute":"minecraft:armor_toughness","amount":{"type":"minecraft:constant","value":2},"operation":"add_value","id":"minecraft:0a582477-1e0b-425a-9241-fd7533f45c0c","slot":"legs"}]}`.
Буквальные ссылки на ID: [lbc/function/items/changed_inventory/fix_attribute_legs.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/changed_inventory/fix_attribute_legs.mcfunction).

## item_modifier `lbc:attributefix/golden_boots`

Источник: [lbc:attributefix/golden_boots](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/attributefix/golden_boots.json).
Шаг 1: задать модификаторы атрибутов `{"modifiers":[{"attribute":"minecraft:armor","amount":{"type":"minecraft:constant","value":1},"operation":"add_value","id":"minecraft:bafd952e-692a-41ed-9b61-c7d604d107c9","slot":"feet"}]}`.
Буквальные ссылки на ID: [lbc/function/items/changed_inventory/fix_attribute_feet.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/changed_inventory/fix_attribute_feet.mcfunction).

## item_modifier `lbc:attributefix/golden_chestplate`

Источник: [lbc:attributefix/golden_chestplate](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/attributefix/golden_chestplate.json).
Шаг 1: задать модификаторы атрибутов `{"modifiers":[{"attribute":"minecraft:armor","amount":{"type":"minecraft:constant","value":5},"operation":"add_value","id":"minecraft:46e426d6-c51a-4703-b10e-b18a85e03659","slot":"chest"}]}`.
Буквальные ссылки на ID: [lbc/function/items/changed_inventory/fix_attribute_chest.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/changed_inventory/fix_attribute_chest.mcfunction).

## item_modifier `lbc:attributefix/golden_helmet`

Источник: [lbc:attributefix/golden_helmet](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/attributefix/golden_helmet.json).
Шаг 1: задать модификаторы атрибутов `{"modifiers":[{"attribute":"minecraft:armor","amount":{"type":"minecraft:constant","value":2},"operation":"add_value","id":"minecraft:0af55a83-3f8f-4494-b9e2-1e6cccd2c450","slot":"head"}]}`.
Буквальные ссылки на ID: [lbc/function/items/changed_inventory/fix_attribute_head.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/changed_inventory/fix_attribute_head.mcfunction).

## item_modifier `lbc:attributefix/golden_leggings`

Источник: [lbc:attributefix/golden_leggings](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/attributefix/golden_leggings.json).
Шаг 1: задать модификаторы атрибутов `{"modifiers":[{"attribute":"minecraft:armor","amount":{"type":"minecraft:constant","value":3},"operation":"add_value","id":"minecraft:e6f36f87-7802-411f-8688-056321a5f6a7","slot":"legs"}]}`.
Буквальные ссылки на ID: [lbc/function/items/changed_inventory/fix_attribute_legs.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/changed_inventory/fix_attribute_legs.mcfunction).

## item_modifier `lbc:attributefix/iron_boots`

Источник: [lbc:attributefix/iron_boots](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/attributefix/iron_boots.json).
Шаг 1: задать модификаторы атрибутов `{"modifiers":[{"attribute":"minecraft:armor","amount":{"type":"minecraft:constant","value":2},"operation":"add_value","id":"minecraft:bafd952e-692a-41ed-9b61-c7d604d107c9","slot":"feet"}]}`.
Буквальные ссылки на ID: [lbc/function/items/changed_inventory/fix_attribute_feet.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/changed_inventory/fix_attribute_feet.mcfunction).

## item_modifier `lbc:attributefix/iron_chestplate`

Источник: [lbc:attributefix/iron_chestplate](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/attributefix/iron_chestplate.json).
Шаг 1: задать модификаторы атрибутов `{"modifiers":[{"attribute":"minecraft:armor","amount":{"type":"minecraft:constant","value":6},"operation":"add_value","id":"minecraft:46e426d6-c51a-4703-b10e-b18a85e03659","slot":"chest"}]}`.
Буквальные ссылки на ID: [lbc/function/items/changed_inventory/fix_attribute_chest.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/changed_inventory/fix_attribute_chest.mcfunction).

## item_modifier `lbc:attributefix/iron_helmet`

Источник: [lbc:attributefix/iron_helmet](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/attributefix/iron_helmet.json).
Шаг 1: задать модификаторы атрибутов `{"modifiers":[{"attribute":"minecraft:armor","amount":{"type":"minecraft:constant","value":2},"operation":"add_value","id":"minecraft:0af55a83-3f8f-4494-b9e2-1e6cccd2c450","slot":"head"}]}`.
Буквальные ссылки на ID: [lbc/function/items/changed_inventory/fix_attribute_head.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/changed_inventory/fix_attribute_head.mcfunction).

## item_modifier `lbc:attributefix/iron_leggings`

Источник: [lbc:attributefix/iron_leggings](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/attributefix/iron_leggings.json).
Шаг 1: задать модификаторы атрибутов `{"modifiers":[{"attribute":"minecraft:armor","amount":{"type":"minecraft:constant","value":5},"operation":"add_value","id":"minecraft:e6f36f87-7802-411f-8688-056321a5f6a7","slot":"legs"}]}`.
Буквальные ссылки на ID: [lbc/function/items/changed_inventory/fix_attribute_legs.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/changed_inventory/fix_attribute_legs.mcfunction).

## item_modifier `lbc:attributefix/leather_boots`

Источник: [lbc:attributefix/leather_boots](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/attributefix/leather_boots.json).
Шаг 1: задать модификаторы атрибутов `{"modifiers":[{"attribute":"minecraft:armor","amount":{"type":"minecraft:constant","value":1},"operation":"add_value","id":"minecraft:bafd952e-692a-41ed-9b61-c7d604d107c9","slot":"feet"}]}`.
Буквальные ссылки на ID: [lbc/function/items/changed_inventory/fix_attribute_feet.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/changed_inventory/fix_attribute_feet.mcfunction).

## item_modifier `lbc:attributefix/leather_chestplate`

Источник: [lbc:attributefix/leather_chestplate](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/attributefix/leather_chestplate.json).
Шаг 1: задать модификаторы атрибутов `{"modifiers":[{"attribute":"minecraft:armor","amount":{"type":"minecraft:constant","value":3},"operation":"add_value","id":"minecraft:46e426d6-c51a-4703-b10e-b18a85e03659","slot":"chest"}]}`.
Буквальные ссылки на ID: [lbc/function/items/changed_inventory/fix_attribute_chest.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/changed_inventory/fix_attribute_chest.mcfunction).

## item_modifier `lbc:attributefix/leather_helmet`

Источник: [lbc:attributefix/leather_helmet](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/attributefix/leather_helmet.json).
Шаг 1: задать модификаторы атрибутов `{"modifiers":[{"attribute":"minecraft:armor","amount":{"type":"minecraft:constant","value":1},"operation":"add_value","id":"minecraft:0af55a83-3f8f-4494-b9e2-1e6cccd2c450","slot":"head"}]}`.
Буквальные ссылки на ID: [lbc/function/items/changed_inventory/fix_attribute_head.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/changed_inventory/fix_attribute_head.mcfunction).

## item_modifier `lbc:attributefix/leather_leggings`

Источник: [lbc:attributefix/leather_leggings](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/attributefix/leather_leggings.json).
Шаг 1: задать модификаторы атрибутов `{"modifiers":[{"attribute":"minecraft:armor","amount":{"type":"minecraft:constant","value":2},"operation":"add_value","id":"minecraft:e6f36f87-7802-411f-8688-056321a5f6a7","slot":"legs"}]}`.
Буквальные ссылки на ID: [lbc/function/items/changed_inventory/fix_attribute_legs.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/changed_inventory/fix_attribute_legs.mcfunction).

## item_modifier `lbc:attributefix/netherite_boots`

Источник: [lbc:attributefix/netherite_boots](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/attributefix/netherite_boots.json).
Шаг 1: задать модификаторы атрибутов `{"modifiers":[{"attribute":"minecraft:armor","amount":{"type":"minecraft:constant","value":3},"operation":"add_value","id":"minecraft:bafd952e-692a-41ed-9b61-c7d604d107c9","slot":"feet"},{"attribute":"minecraft:armor_toughness","amount":{"type":"minecraft:constant","value":3},"operation":"add_value","id":"minecraft:bdac1db5-1ef4-45c4-96f2-ff4700331454","slot":"feet"},{"attribute":"minecraft:knockback_resistance","amount":{"type":"minecraft:constant","value":0.1},"operation":"add_value","id":"minecraft:ce9dc58e-030a-4606-9330-5c578642f6cc","slot":"feet"}]}`.
Буквальные ссылки на ID: [lbc/function/items/changed_inventory/fix_attribute_feet.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/changed_inventory/fix_attribute_feet.mcfunction).

## item_modifier `lbc:attributefix/netherite_chestplate`

Источник: [lbc:attributefix/netherite_chestplate](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/attributefix/netherite_chestplate.json).
Шаг 1: задать модификаторы атрибутов `{"modifiers":[{"attribute":"minecraft:armor","amount":{"type":"minecraft:constant","value":8},"operation":"add_value","id":"minecraft:46e426d6-c51a-4703-b10e-b18a85e03659","slot":"chest"},{"attribute":"minecraft:armor_toughness","amount":{"type":"minecraft:constant","value":3},"operation":"add_value","id":"minecraft:b03ad1b6-0e49-49f3-8d61-b9b26cd19b81","slot":"chest"},{"attribute":"minecraft:knockback_resistance","amount":{"type":"minecraft:constant","value":0.1},"operation":"add_value","id":"minecraft:eaba3110-4e04-4223-b39d-e17ca30451a8","slot":"chest"}]}`.
Буквальные ссылки на ID: [lbc/function/items/changed_inventory/fix_attribute_chest.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/changed_inventory/fix_attribute_chest.mcfunction).

## item_modifier `lbc:attributefix/netherite_helmet`

Источник: [lbc:attributefix/netherite_helmet](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/attributefix/netherite_helmet.json).
Шаг 1: задать модификаторы атрибутов `{"modifiers":[{"attribute":"minecraft:armor","amount":{"type":"minecraft:constant","value":3},"operation":"add_value","id":"minecraft:0af55a83-3f8f-4494-b9e2-1e6cccd2c450","slot":"head"},{"attribute":"minecraft:armor_toughness","amount":{"type":"minecraft:constant","value":3},"operation":"add_value","id":"minecraft:56105362-945a-4be7-a6c0-61d80ef41d2c","slot":"head"},{"attribute":"minecraft:knockback_resistance","amount":{"type":"minecraft:constant","value":0.1},"operation":"add_value","id":"minecraft:db337e0b-6909-4f6d-9a43-cd2c66b601d1","slot":"head"}]}`.
Буквальные ссылки на ID: [lbc/function/items/changed_inventory/fix_attribute_head.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/changed_inventory/fix_attribute_head.mcfunction).

## item_modifier `lbc:attributefix/netherite_leggings`

Источник: [lbc:attributefix/netherite_leggings](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/attributefix/netherite_leggings.json).
Шаг 1: задать модификаторы атрибутов `{"modifiers":[{"attribute":"minecraft:armor","amount":{"type":"minecraft:constant","value":6},"operation":"add_value","id":"minecraft:e6f36f87-7802-411f-8688-056321a5f6a7","slot":"legs"},{"attribute":"minecraft:armor_toughness","amount":{"type":"minecraft:constant","value":3},"operation":"add_value","id":"minecraft:0a582477-1e0b-425a-9241-fd7533f45c0c","slot":"legs"},{"attribute":"minecraft:knockback_resistance","amount":{"type":"minecraft:constant","value":0.1},"operation":"add_value","id":"minecraft:5c9aac50-4321-452d-9adb-f3d901bdde2f","slot":"legs"}]}`.
Буквальные ссылки на ID: [lbc/function/items/changed_inventory/fix_attribute_legs.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/changed_inventory/fix_attribute_legs.mcfunction).

## item_modifier `lbc:attributefix/turtle_helmet`

Источник: [lbc:attributefix/turtle_helmet](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/attributefix/turtle_helmet.json).
Шаг 1: задать модификаторы атрибутов `{"modifiers":[{"attribute":"minecraft:armor","amount":{"type":"minecraft:constant","value":2},"operation":"add_value","id":"minecraft:0af55a83-3f8f-4494-b9e2-1e6cccd2c450","slot":"head"}]}`.
Буквальные ссылки на ID: [lbc/function/items/changed_inventory/fix_attribute_head.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/changed_inventory/fix_attribute_head.mcfunction).

## item_modifier `lbc:count`

Источник: [lbc:count](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/count.json).
Шаг 1: задать количество `{"type":"minecraft:score","target":{"type":"minecraft:fixed","name":"itemcount"},"score":"lbc.math","scale":1}`.
Буквальные ссылки на ID: [lbc/function/other/twitching_ender_pearl.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/twitching_ender_pearl.mcfunction), [lbc/function/other/twitching_ender_pearl_left.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/twitching_ender_pearl_left.mcfunction), [lbc/function/other/twitching_snowball.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/twitching_snowball.mcfunction), [lbc/function/other/twitching_snowball_left.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/twitching_snowball_left.mcfunction).

## item_modifier `lbc:count_reduce`

Источник: [lbc:count_reduce](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/count_reduce.json).
Шаг 1: прибавить количество `-1`.
Буквальные ссылки на ID: [lbc/function/other/craft/ananta_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/ananta_inv.mcfunction), [lbc/function/other/craft/crafted_acid_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/crafted_acid_inv.mcfunction), [lbc/function/other/craft/crafted_air_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/crafted_air_inv.mcfunction), [lbc/function/other/craft/crafted_blind_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/crafted_blind_inv.mcfunction), [lbc/function/other/craft/crafted_dust_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/crafted_dust_inv.mcfunction), [lbc/function/other/craft/crafted_electro_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/crafted_electro_inv.mcfunction), [lbc/function/other/craft/crafted_electroshield_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/crafted_electroshield_inv.mcfunction), [lbc/function/other/craft/crafted_electrosword_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/crafted_electrosword_inv.mcfunction), [lbc/function/other/craft/crafted_fire_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/crafted_fire_inv.mcfunction), [lbc/function/other/craft/crafted_mana_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/crafted_mana_inv.mcfunction), [lbc/function/other/craft/crafted_potion_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/crafted_potion_inv.mcfunction), [lbc/function/other/craft/crafted_railgun_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/crafted_railgun_inv.mcfunction), [lbc/function/other/craft/crafted_spell_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/crafted_spell_inv.mcfunction), [lbc/function/other/craft/crafted_stone_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/crafted_stone_inv.mcfunction), [lbc/function/other/craft/crafted_swargia_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/crafted_swargia_inv.mcfunction), [lbc/function/other/craft/crafted_teleport_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/crafted_teleport_inv.mcfunction), [lbc/function/other/craft/crafted_terra_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/crafted_terra_inv.mcfunction), [lbc/function/other/craft/cyberpunk_boots_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/cyberpunk_boots_inv.mcfunction), [lbc/function/other/craft/cyberpunk_chestplate_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/cyberpunk_chestplate_inv.mcfunction), [lbc/function/other/craft/cyberpunk_helmet_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/cyberpunk_helmet_inv.mcfunction), [lbc/function/other/craft/cyberpunk_leggings_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/cyberpunk_leggings_inv.mcfunction), [lbc/function/other/craft/divide_by_metal_pipe_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/divide_by_metal_pipe_inv.mcfunction), [lbc/function/other/craft/fartus_nadristailo_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/fartus_nadristailo_inv.mcfunction), [lbc/function/other/craft/fevgalea_star_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/fevgalea_star_inv.mcfunction), [lbc/function/other/craft/fotja_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/fotja_inv.mcfunction), [lbc/function/other/craft/gaycaster_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/gaycaster_inv.mcfunction), [lbc/function/other/craft/ice_wand_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/ice_wand_inv.mcfunction), [lbc/function/other/craft/mellstroy_mask_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/mellstroy_mask_inv.mcfunction), [lbc/function/other/craft/nyauwu_boots_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/nyauwu_boots_inv.mcfunction), [lbc/function/other/craft/nyauwu_chestplate_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/nyauwu_chestplate_inv.mcfunction), [lbc/function/other/craft/nyauwu_helmet_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/nyauwu_helmet_inv.mcfunction), [lbc/function/other/craft/nyauwu_leggings_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/nyauwu_leggings_inv.mcfunction), [lbc/function/other/craft/reroll_machine.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/reroll_machine.mcfunction), [lbc/function/other/craft/rocket_boots_add_ability_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/rocket_boots_add_ability_inv.mcfunction), [lbc/function/other/craft/shish_wall_wand_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/shish_wall_wand_inv.mcfunction), [lbc/function/other/craft/shulker_upgraded_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/shulker_upgraded_inv.mcfunction), [lbc/function/other/craft/shulker_upgraded_lukcy_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/shulker_upgraded_lukcy_inv.mcfunction), [lbc/function/other/craft/soulhunter_mace_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/soulhunter_mace_inv.mcfunction), [lbc/function/other/craft/stand_clocks_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/stand_clocks_inv.mcfunction), [lbc/function/other/craft/swargia_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/swargia_inv.mcfunction), [lbc/function/other/craft_flying_boots_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft_flying_boots_inv.mcfunction), [lbc/function/other/dildomet_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/dildomet_inv.mcfunction), [lbc/function/other/sauvojen_tuntija/crafted_acidacid_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sauvojen_tuntija/crafted_acidacid_inv.mcfunction), [lbc/function/other/sauvojen_tuntija/crafted_acidcorrupt_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sauvojen_tuntija/crafted_acidcorrupt_inv.mcfunction), [lbc/function/other/sauvojen_tuntija/crafted_avarice_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sauvojen_tuntija/crafted_avarice_inv.mcfunction), [lbc/function/other/sauvojen_tuntija/crafted_awakened_terra_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sauvojen_tuntija/crafted_awakened_terra_inv.mcfunction), [lbc/function/other/sauvojen_tuntija/crafted_blazefirewand_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sauvojen_tuntija/crafted_blazefirewand_inv.mcfunction), [lbc/function/other/sauvojen_tuntija/crafted_boltbolt_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sauvojen_tuntija/crafted_boltbolt_inv.mcfunction), [lbc/function/other/sauvojen_tuntija/crafted_energyblock_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sauvojen_tuntija/crafted_energyblock_inv.mcfunction), [lbc/function/other/sauvojen_tuntija/crafted_exahoming_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sauvojen_tuntija/crafted_exahoming_inv.mcfunction), [lbc/function/other/sauvojen_tuntija/crafted_exasperoso_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sauvojen_tuntija/crafted_exasperoso_inv.mcfunction), [lbc/function/other/sauvojen_tuntija/crafted_greed_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sauvojen_tuntija/crafted_greed_inv.mcfunction), [lbc/function/other/sauvojen_tuntija/crafted_kiuaswand_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sauvojen_tuntija/crafted_kiuaswand_inv.mcfunction), [lbc/function/other/sauvojen_tuntija/crafted_psychoblind_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sauvojen_tuntija/crafted_psychoblind_inv.mcfunction), [lbc/function/other/sauvojen_tuntija/crafted_psychovex_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sauvojen_tuntija/crafted_psychovex_inv.mcfunction), [lbc/function/other/sauvojen_tuntija/crafted_razorterra_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sauvojen_tuntija/crafted_razorterra_inv.mcfunction), [lbc/function/other/sauvojen_tuntija/crafted_tannerstone_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sauvojen_tuntija/crafted_tannerstone_inv.mcfunction), [lbc/function/other/sauvojen_tuntija/crafted_ultraelectro_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sauvojen_tuntija/crafted_ultraelectro_inv.mcfunction), [lbc/function/other/sauvojen_tuntija/crafted_ultrareforge_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sauvojen_tuntija/crafted_ultrareforge_inv.mcfunction), [lbc/function/other/sauvojen_tuntija/crafted_wallstone_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sauvojen_tuntija/crafted_wallstone_inv.mcfunction), [lbc/function/other/shishgun_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/shishgun_inv.mcfunction), [lbc/function/other/shitkatana_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/shitkatana_inv.mcfunction), [lbc/function/other/socializm_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/socializm_inv.mcfunction), [lbc/function/other/supernova_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/supernova_inv.mcfunction).

## item_modifier `lbc:crossbow_uncharge`

Источник: [lbc:crossbow_uncharge](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/crossbow_uncharge.json).
Шаг 1: заряженные снаряды: `[]`.
Буквальные ссылки на ID: [lbc/function/other/twitching_crossbow.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/twitching_crossbow.mcfunction).

## item_modifier `lbc:down_armor`

Источник: [lbc:down_armor](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/down_armor.json).
Шаг 1: задать модификаторы атрибутов `{"modifiers":[{"attribute":"minecraft:armor","amount":{"type":"minecraft:score","target":{"type":"minecraft:fixed","name":"#tmp1"},"score":"lbc.math","scale":1},"operation":"add_value","id":"minecraft:71eaf780-80f9-4861-8a64-49ab1247121b","slot":"head"},{"attribute":"minecraft:armor_toughness","amount":{"type":"minecraft:score","target":{"type":"minecraft:fixed","name":"#tmp1"},"score":"lbc.math"},"operation":"add_value","id":"minecraft:10c2542e-4ba7-4d60-bfe4-225113e8ecef","slot":"head"}]}`.
Буквальных ссылок на этот точный ID в исследуемых функциях/JSON не найдено; ресурс доступен при ручном или динамическом вызове.

## item_modifier `lbc:down_armor_chest`

Источник: [lbc:down_armor_chest](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/down_armor_chest.json).
Шаг 1: модель предмета: `lbc:armors/glass_chestplate`; максимальная прочность: `150`; слот/модель экипировки и камера: `{"slot":"chest","asset_id":"lbc:glass"}`.
Шаг 2: задать имя `glass_chestplate`; параметры `{"name":{"translate":"glass_chestplate","color":"white","italic":false}}`.
Шаг 3: задать строки описания `typechestplateglass.lore`; параметры `{"mode":"replace_all","lore":[[{"translate":"type","italic":false,"color":"aqua"},{"translate":"chestplate","italic":false,"color":"aqua"}],{"translate":"glass.lore","italic":false,"color":"aqua"}]}`.
Шаг 4: задать модификаторы атрибутов `{"modifiers":[{"attribute":"minecraft:armor","amount":{"type":"minecraft:score","target":{"type":"minecraft:fixed","name":"#tmp3"},"score":"lbc.math","scale":0.0009},"operation":"add_value","id":"minecraft:71eaf780-80f9-4861-8a64-49ab1247121b","slot":"chest"},{"attribute":"minecraft:armor_toughness","amount":{"type":"minecraft:score","target":{"type":"minecraft:fixed","name":"#tmp3"},"score":"lbc.math","scale":0.0009},"operation":"add_value","id":"minecraft:71eaf780-80f9-4861-8a64-49ab1247121b","slot":"chest"}]}`.
Шаг 5: добавить/перезаписать метки custom_data `{transformable:1,chestplate:1,glass:1}`.
Буквальные ссылки на ID: [lbc/function/armor/glass/damaged.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/armor/glass/damaged.mcfunction).

## item_modifier `lbc:down_armor_feet`

Источник: [lbc:down_armor_feet](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/down_armor_feet.json).
Шаг 1: модель предмета: `lbc:armors/glass_boots`; максимальная прочность: `150`; слот/модель экипировки и камера: `{"slot":"feet","asset_id":"lbc:glass"}`.
Шаг 2: задать имя `glass_boots`; параметры `{"name":{"translate":"glass_boots","color":"white","italic":false}}`.
Шаг 3: задать строки описания `typebootsglass.lore`; параметры `{"mode":"replace_all","lore":[[{"translate":"type","italic":false,"color":"aqua"},{"translate":"boots","italic":false,"color":"aqua"}],{"translate":"glass.lore","italic":false,"color":"aqua"}]}`.
Шаг 4: задать модификаторы атрибутов `{"modifiers":[{"attribute":"minecraft:armor","amount":{"type":"minecraft:score","target":{"type":"minecraft:fixed","name":"#tmp1"},"score":"lbc.math","scale":0.0009},"operation":"add_value","id":"minecraft:71eaf780-80f9-4861-8a64-49ab1247121b","slot":"feet"},{"attribute":"minecraft:armor_toughness","amount":{"type":"minecraft:score","target":{"type":"minecraft:fixed","name":"#tmp1"},"score":"lbc.math","scale":0.0009},"operation":"add_value","id":"minecraft:71eaf780-80f9-4861-8a64-49ab1247121b","slot":"feet"}]}`.
Шаг 5: добавить/перезаписать метки custom_data `{transformable:1,boots:1,glass:1}`.
Буквальные ссылки на ID: [lbc/function/armor/glass/damaged.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/armor/glass/damaged.mcfunction).

## item_modifier `lbc:down_armor_head`

Источник: [lbc:down_armor_head](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/down_armor_head.json).
Шаг 1: модель предмета: `lbc:armors/glass_helmet`; максимальная прочность: `150`; слот/модель экипировки и камера: `{"slot":"head","asset_id":"lbc:glass"}`.
Шаг 2: задать имя `glass_helmet`; параметры `{"name":{"translate":"glass_helmet","color":"white","italic":false}}`.
Шаг 3: задать строки описания `typehelmetglass.lore`; параметры `{"mode":"replace_all","lore":[[{"translate":"type","italic":false,"color":"aqua"},{"translate":"helmet","italic":false,"color":"aqua"}],{"translate":"glass.lore","italic":false,"color":"aqua"}]}`.
Шаг 4: задать модификаторы атрибутов `{"modifiers":[{"attribute":"minecraft:armor","amount":{"type":"minecraft:score","target":{"type":"minecraft:fixed","name":"#tmp4"},"score":"lbc.math","scale":0.0009},"operation":"add_value","id":"minecraft:71eaf780-80f9-4861-8a64-49ab1247121b","slot":"head"},{"attribute":"minecraft:armor_toughness","amount":{"type":"minecraft:score","target":{"type":"minecraft:fixed","name":"#tmp4"},"score":"lbc.math","scale":0.0009},"operation":"add_value","id":"minecraft:71eaf780-80f9-4861-8a64-49ab1247121b","slot":"head"}]}`.
Шаг 5: добавить/перезаписать метки custom_data `{transformable:1,helmet:1,glass:1}`.
Буквальные ссылки на ID: [lbc/function/armor/glass/damaged.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/armor/glass/damaged.mcfunction).

## item_modifier `lbc:down_armor_legs`

Источник: [lbc:down_armor_legs](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/down_armor_legs.json).
Шаг 1: модель предмета: `lbc:armors/glass_leggings`; максимальная прочность: `150`; слот/модель экипировки и камера: `{"slot":"legs","asset_id":"lbc:glass"}`.
Шаг 2: задать имя `glass_leggings`; параметры `{"name":{"translate":"glass_leggings","color":"white","italic":false}}`.
Шаг 3: задать строки описания `typeleggingsglass.lore`; параметры `{"mode":"replace_all","lore":[[{"translate":"type","italic":false,"color":"aqua"},{"translate":"leggings","italic":false,"color":"aqua"}],{"translate":"glass.lore","italic":false,"color":"aqua"}]}`.
Шаг 4: задать модификаторы атрибутов `{"modifiers":[{"attribute":"minecraft:armor","amount":{"type":"minecraft:score","target":{"type":"minecraft:fixed","name":"#tmp2"},"score":"lbc.math","scale":0.0009},"operation":"add_value","id":"minecraft:71eaf780-80f9-4861-8a64-49ab1247121b","slot":"legs"},{"attribute":"minecraft:armor_toughness","amount":{"type":"minecraft:score","target":{"type":"minecraft:fixed","name":"#tmp2"},"score":"lbc.math","scale":0.0009},"operation":"add_value","id":"minecraft:71eaf780-80f9-4861-8a64-49ab1247121b","slot":"legs"}]}`.
Шаг 5: добавить/перезаписать метки custom_data `{transformable:1,leggings:1,glass:1}`.
Буквальные ссылки на ID: [lbc/function/armor/glass/damaged.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/armor/glass/damaged.mcfunction).

## item_modifier `lbc:down_durability`

Источник: [lbc:down_durability](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/down_durability.json).
Шаг 1: задать оставшуюся долю прочности (это не количество потерянных единиц): `{"damage":-0.03126,"add":true}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/items/grapping_hook_success.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/items/grapping_hook_success.mcfunction).

## item_modifier `lbc:down_resistance`

Источник: [lbc:down_resistance](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/down_resistance.json).
Шаг 1: задать уровни зачарований `{"minecraft:protection":{"type":"minecraft:score","target":{"type":"minecraft:fixed","name":"#tmp1"},"score":"lbc.math"}}`.
Буквальные ссылки на ID: [lbc/function/armor/erosion/tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/armor/erosion/tick.mcfunction), [lbc/function/armor/erosion/tick_rnd.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/armor/erosion/tick_rnd.mcfunction).

## item_modifier `lbc:fartus_upgrade`

Источник: [lbc:fartus_upgrade](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/fartus_upgrade.json).
Шаг 1: задать строки описания `typeswordfartus_nadristailo_evolved.lore.1fartus_nadristailo_evolved.lore.2fartus_nadristailo_evolved.lore.3fartus_nadristailo_evolved.lore.4Tinkist00 Halloween 2025 Legendary Drop Request`; параметры `{"mode":"replace_all","lore":[[{"translate":"type","italic":false,"color":"aqua"},{"translate":"sword","italic":false,"color":"aqua"}],{"translate":"fartus_nadristailo_evolved.lore.1","italic":false,"color":"white"},{"translate":"fartus_nadristailo_evolved.lore.2","italic":false,"color":"aqua"},{"translate":"fartus_nadristailo_evolved.lore.3","italic":false,"color":"aqua"},{"translate":"fartus_nadristailo_evolved.lore.4","italic":false,"color":"yellow"},[{"text":"Tinkist00","color":"green","italic":false,"type":"text"},{"text":" Halloween 2025 Legendary Drop Request","color":"dark_gray","italic":false,"type":"text"}]]}`.
Шаг 2: задать модификаторы атрибутов `{"replace":true,"modifiers":[{"attribute":"minecraft:attack_damage","id":"minecraft:base_attack_damage","amount":19,"operation":"add_value","slot":"mainhand"},{"attribute":"minecraft:attack_speed","id":"minecraft:base_attack_speed","amount":-3.5,"operation":"add_value","slot":"mainhand"},{"attribute":"minecraft:entity_interaction_range","id":"a8uvy89ryvmg89dmybv8gdfrgg","amount":10,"operation":"add_value","slot":"mainhand"}]}`.
Шаг 3: модель предмета: `lbc:palma`.
Буквальные ссылки на ID: [lbc/function/other/magic_academy/upgrade/fartus_nadristailo.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/fartus_nadristailo.mcfunction).

## item_modifier `lbc:kakashki_fartus_damage_buff`

Источник: [lbc:kakashki_fartus_damage_buff](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/kakashki_fartus_damage_buff.json).
Шаг 1: задать модификаторы атрибутов `{"replace":true,"modifiers":[{"attribute":"minecraft:attack_damage","id":"minecraft:base_attack_damage","amount":{"type":"minecraft:storage","path":"damagebuff","storage":"lbc.math"},"operation":"add_value","slot":"mainhand"},{"attribute":"minecraft:attack_speed","id":"minecraft:base_attack_speed","amount":-3.5,"operation":"add_value","slot":"mainhand"},{"attribute":"minecraft:entity_interaction_range","id":"a8uvy89ryvmg89dmybv8gdfrgg","amount":10,"operation":"add_value","slot":"mainhand"}]}`.
Буквальные ссылки на ID: [lbc/function/workingitems/firtusnadristailo/sword_tick_1s.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/firtusnadristailo/sword_tick_1s.mcfunction).

## item_modifier `lbc:magnum_switch_mode`

Источник: [lbc:magnum_switch_mode](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/magnum_switch_mode.json).
Шаг 1: только при (свойства `this`: `{"equipment":{"mainhand":{"components":{"minecraft:custom_data":"{mode: 0}"}}}}`): отображаемое имя `magnum_mode_incendiary` (компонент `{"italic":false,"translate":"magnum_mode_incendiary"}`).
Шаг 2: только при (свойства `this`: `{"equipment":{"mainhand":{"components":{"minecraft:custom_data":"{mode: 1}"}}}}`): отображаемое имя `magnum_mode_electric` (компонент `{"italic":false,"translate":"magnum_mode_electric"}`).
Шаг 3: только при (свойства `this`: `{"equipment":{"mainhand":{"components":{"minecraft:custom_data":"{mode: 2}"}}}}`): отображаемое имя `magnum_mode_acid` (компонент `{"italic":false,"translate":"magnum_mode_acid"}`).
Шаг 4: только при (свойства `this`: `{"equipment":{"mainhand":{"components":{"minecraft:custom_data":"{mode: 3}"}}}}`): отображаемое имя `magnum_mode_void` (компонент `{"italic":false,"translate":"magnum_mode_void"}`).
Шаг 5: только при (свойства `this`: `{"equipment":{"mainhand":{"components":{"minecraft:custom_data":"{mode: 4}"}}}}`): отображаемое имя `magnum_mode_explosive` (компонент `{"italic":false,"translate":"magnum_mode_explosive"}`).
Шаг 6: только при (свойства `this`: `{"equipment":{"mainhand":{"components":{"minecraft:custom_data":"{mode: 5}"}}}}`): отображаемое имя `magnum_mode_standart` (компонент `{"italic":false,"translate":"magnum_mode_standart"}`).
Шаг 7: добавить/перезаписать метки custom_data `{mode:1}`.
Буквальные ссылки на ID: [lbc/function/other/techno/magnum/cycle_left.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/cycle_left.mcfunction).

## item_modifier `lbc:one_punch`

Источник: [lbc:one_punch](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/one_punch.json).
Шаг 1: задать строки описания `typeswordGONZO Beta Server Harem event`; параметры `{"mode":"replace_all","entity":"this","lore":[[{"translate":"type","italic":false,"color":"aqua"},{"translate":"sword","italic":false,"color":"aqua"}],[{"text":"GONZO","color":"green","italic":false,"type":"text"},{"text":" Beta Server Harem event","color":"dark_gray","italic":false,"type":"text"}]]}`.
Шаг 2: задать модификаторы атрибутов `{"modifiers":[{"attribute":"minecraft:attack_damage","amount":{"type":"minecraft:score","target":"this","score":"hplvl","scale":1},"operation":"add_value","id":"minecraft:base_attack_damage","slot":"mainhand"}]}`.
Буквальных ссылок на этот точный ID в исследуемых функциях/JSON не найдено; ресурс доступен при ручном или динамическом вызове.

## item_modifier `lbc:remove_name_lore`

Источник: [lbc:remove_name_lore](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/remove_name_lore.json).
Шаг 1: задать строки описания `????????????????????????????`; параметры `{"mode":"replace_all","lore":[{"text":"????????????????????????????"}]}`.
Шаг 2: задать имя `????????????????????????????`; параметры `{"name":[{"text":"????????????????????????????"}]}`.
Шаг 3: показать/скрыть разделы подсказки: `{"toggles":{"minecraft:attribute_modifiers":false,"minecraft:can_break":false,"minecraft:can_place_on":false,"minecraft:dyed_color":false,"minecraft:enchantments":false,"minecraft:jukebox_playable":false,"minecraft:stored_enchantments":false,"minecraft:trim":false,"minecraft:unbreakable":false}}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/game/challenges/no_knowledge/inventory_change.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/challenges/no_knowledge/inventory_change.mcfunction).

## item_modifier `lbc:rotate_laser_minigun`

Источник: [lbc:rotate_laser_minigun](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/rotate_laser_minigun.json).
Шаг 1: только при (свойства `this`: `{"equipment":{"mainhand":{"components":{"minecraft:item_model":"lbc:laser_minigun/2"}}}}`): модель предмета: `lbc:laser_minigun/1`.
Шаг 2: только при (свойства `this`: `{"equipment":{"mainhand":{"components":{"minecraft:item_model":"lbc:laser_minigun/3"}}}}`): модель предмета: `lbc:laser_minigun/2`.
Шаг 3: только при (свойства `this`: `{"equipment":{"mainhand":{"components":{"minecraft:item_model":"lbc:laser_minigun/4"}}}}`): модель предмета: `lbc:laser_minigun/3`.
Шаг 4: только при (свойства `this`: `{"equipment":{"mainhand":{"components":{"minecraft:item_model":"lbc:laser_minigun/5"}}}}`): модель предмета: `lbc:laser_minigun/4`.
Шаг 5: только при (свойства `this`: `{"equipment":{"mainhand":{"components":{"minecraft:item_model":"lbc:laser_minigun/6"}}}}`): модель предмета: `lbc:laser_minigun/5`.
Шаг 6: только при (свойства `this`: `{"equipment":{"mainhand":{"components":{"minecraft:item_model":"lbc:laser_minigun/7"}}}}`): модель предмета: `lbc:laser_minigun/6`.
Шаг 7: только при (свойства `this`: `{"equipment":{"mainhand":{"components":{"minecraft:item_model":"lbc:laser_minigun/8"}}}}`): модель предмета: `lbc:laser_minigun/7`.
Шаг 8: только при (свойства `this`: `{"equipment":{"mainhand":{"components":{"minecraft:item_model":"lbc:laser_minigun/1"}}}}`): модель предмета: `lbc:laser_minigun/8`.
Буквальные ссылки на ID: [lbc/function/other/techno/laser_minigun/shoot/spin.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/laser_minigun/shoot/spin.mcfunction).

## item_modifier `lbc:swrg_unlocks/academy_acid`

Источник: [lbc:swrg_unlocks/academy_acid](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/academy_acid.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.upgraded_staff.acid`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.upgraded_staff.acid","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/11000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/11000.mcfunction).

## item_modifier `lbc:swrg_unlocks/academy_acid_tuntija`

Источник: [lbc:swrg_unlocks/academy_acid_tuntija](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/academy_acid_tuntija.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.upgraded_tuntija.acid`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.upgraded_tuntija.acid","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/9000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/9000.mcfunction).

## item_modifier `lbc:swrg_unlocks/academy_acidacid`

Источник: [lbc:swrg_unlocks/academy_acidacid](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/academy_acidacid.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.upgraded_tuntija.acidacid`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.upgraded_tuntija.acidacid","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/10000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/10000.mcfunction).

## item_modifier `lbc:swrg_unlocks/academy_acidcorrupt`

Источник: [lbc:swrg_unlocks/academy_acidcorrupt](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/academy_acidcorrupt.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.upgraded_tuntija.acidcorrupt`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.upgraded_tuntija.acidcorrupt","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/10000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/10000.mcfunction).

## item_modifier `lbc:swrg_unlocks/academy_air`

Источник: [lbc:swrg_unlocks/academy_air](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/academy_air.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.upgraded_tuntija.air`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.upgraded_tuntija.air","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/11000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/11000.mcfunction).

## item_modifier `lbc:swrg_unlocks/academy_badwiki`

Источник: [lbc:swrg_unlocks/academy_badwiki](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/academy_badwiki.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.upgraded_staff.badwiki`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.upgraded_staff.badwiki","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/9000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/9000.mcfunction).

## item_modifier `lbc:swrg_unlocks/academy_bass`

Источник: [lbc:swrg_unlocks/academy_bass](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/academy_bass.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.upgraded_staff.bass`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.upgraded_staff.bass","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/11000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/11000.mcfunction).

## item_modifier `lbc:swrg_unlocks/academy_bedrock`

Источник: [lbc:swrg_unlocks/academy_bedrock](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/academy_bedrock.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.upgraded_tuntija.bedrock`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.upgraded_tuntija.bedrock","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/11000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/11000.mcfunction).

## item_modifier `lbc:swrg_unlocks/academy_big_stone`

Источник: [lbc:swrg_unlocks/academy_big_stone](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/academy_big_stone.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.upgraded_tuntija.big_stone`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.upgraded_tuntija.big_stone","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/10000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/10000.mcfunction).

## item_modifier `lbc:swrg_unlocks/academy_blaze`

Источник: [lbc:swrg_unlocks/academy_blaze](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/academy_blaze.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.upgraded_staff.blaze`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.upgraded_staff.blaze","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/11000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/11000.mcfunction).

## item_modifier `lbc:swrg_unlocks/academy_blazefire`

Источник: [lbc:swrg_unlocks/academy_blazefire](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/academy_blazefire.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.upgraded_tuntija.blazefire`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.upgraded_tuntija.blazefire","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/9000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/9000.mcfunction).

## item_modifier `lbc:swrg_unlocks/academy_blind`

Источник: [lbc:swrg_unlocks/academy_blind](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/academy_blind.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.upgraded_tuntija.blind`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.upgraded_tuntija.blind","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/11000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/11000.mcfunction).

## item_modifier `lbc:swrg_unlocks/academy_boltbolt`

Источник: [lbc:swrg_unlocks/academy_boltbolt](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/academy_boltbolt.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.upgraded_tuntija.boltbolt`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.upgraded_tuntija.boltbolt","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/10000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/10000.mcfunction).

## item_modifier `lbc:swrg_unlocks/academy_confuse`

Источник: [lbc:swrg_unlocks/academy_confuse](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/academy_confuse.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.upgraded_staff.confuse`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.upgraded_staff.confuse","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/9000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/9000.mcfunction).

## item_modifier `lbc:swrg_unlocks/academy_consuming`

Источник: [lbc:swrg_unlocks/academy_consuming](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/academy_consuming.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.upgraded_tuntija.consuming`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.upgraded_tuntija.consuming","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/10000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/10000.mcfunction).

## item_modifier `lbc:swrg_unlocks/academy_corruption`

Источник: [lbc:swrg_unlocks/academy_corruption](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/academy_corruption.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.upgraded_staff.corruption`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.upgraded_staff.corruption","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/11000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/11000.mcfunction).

## item_modifier `lbc:swrg_unlocks/academy_danmaku`

Источник: [lbc:swrg_unlocks/academy_danmaku](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/academy_danmaku.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.upgraded_staff.danmaku_staff`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.upgraded_staff.danmaku_staff","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/10100.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/10100.mcfunction).

## item_modifier `lbc:swrg_unlocks/academy_dublicating`

Источник: [lbc:swrg_unlocks/academy_dublicating](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/academy_dublicating.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.upgraded_tuntija.dublicating`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.upgraded_tuntija.dublicating","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/10000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/10000.mcfunction).

## item_modifier `lbc:swrg_unlocks/academy_electro`

Источник: [lbc:swrg_unlocks/academy_electro](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/academy_electro.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.upgraded_tuntija.electro`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.upgraded_tuntija.electro","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/11000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/11000.mcfunction).

## item_modifier `lbc:swrg_unlocks/academy_ender_random`

Источник: [lbc:swrg_unlocks/academy_ender_random](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/academy_ender_random.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.upgraded_staff.ender_random`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.upgraded_staff.ender_random","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/11000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/11000.mcfunction).

## item_modifier `lbc:swrg_unlocks/academy_erecto`

Источник: [lbc:swrg_unlocks/academy_erecto](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/academy_erecto.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.upgraded_tuntija.erecto`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.upgraded_tuntija.erecto","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/9000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/9000.mcfunction).

## item_modifier `lbc:swrg_unlocks/academy_exasperato`

Источник: [lbc:swrg_unlocks/academy_exasperato](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/academy_exasperato.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.upgraded_tuntija.exasperato`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.upgraded_tuntija.exasperato","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/9000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/9000.mcfunction).

## item_modifier `lbc:swrg_unlocks/antimagic_armor`

Источник: [lbc:swrg_unlocks/antimagic_armor](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/antimagic_armor.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.armor.overload`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.armor.overload","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/8100.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/8100.mcfunction).

## item_modifier `lbc:swrg_unlocks/assasin`

Источник: [lbc:swrg_unlocks/assasin](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/assasin.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.armor.assasin`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.armor.assasin","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/8000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/8000.mcfunction).

## item_modifier `lbc:swrg_unlocks/babylon_gate`

Источник: [lbc:swrg_unlocks/babylon_gate](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/babylon_gate.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.arcanums.babylon_gate`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.arcanums.babylon_gate","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/8000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/8000.mcfunction).

## item_modifier `lbc:swrg_unlocks/banhammer`

Источник: [lbc:swrg_unlocks/banhammer](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/banhammer.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.axe.banhammer`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.axe.banhammer","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/10000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/10000.mcfunction).

## item_modifier `lbc:swrg_unlocks/barrier`

Источник: [lbc:swrg_unlocks/barrier](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/barrier.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.arcanums.barrier`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.arcanums.barrier","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/8000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/8000.mcfunction).

## item_modifier `lbc:swrg_unlocks/barrier_and_heal`

Источник: [lbc:swrg_unlocks/barrier_and_heal](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/barrier_and_heal.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.arcanums.barrier_and_heal`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.arcanums.barrier_and_heal","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/9000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/9000.mcfunction).

## item_modifier `lbc:swrg_unlocks/berserker`

Источник: [lbc:swrg_unlocks/berserker](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/berserker.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.armor.berserker`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.armor.berserker","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/8000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/8000.mcfunction).

## item_modifier `lbc:swrg_unlocks/black_hole`

Источник: [lbc:swrg_unlocks/black_hole](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/black_hole.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.arcanums.black_hole`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.arcanums.black_hole","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/8000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/8000.mcfunction).

## item_modifier `lbc:swrg_unlocks/black_hole_2`

Источник: [lbc:swrg_unlocks/black_hole_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/black_hole_2.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.arcanums.black_hole_2`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.arcanums.black_hole_2","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/8000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/8000.mcfunction).

## item_modifier `lbc:swrg_unlocks/black_hole_max`

Источник: [lbc:swrg_unlocks/black_hole_max](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/black_hole_max.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.arcanums.black_hole_max`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.arcanums.black_hole_max","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/8000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/8000.mcfunction).

## item_modifier `lbc:swrg_unlocks/blaze`

Источник: [lbc:swrg_unlocks/blaze](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/blaze.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.bow.blaze`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.bow.blaze","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/11000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/11000.mcfunction).

## item_modifier `lbc:swrg_unlocks/blood_academy`

Источник: [lbc:swrg_unlocks/blood_academy](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/blood_academy.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.upgraded_staff.blood`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.upgraded_staff.blood","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/8100.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/8100.mcfunction).

## item_modifier `lbc:swrg_unlocks/chaos`

Источник: [lbc:swrg_unlocks/chaos](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/chaos.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.armor.chaos`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.armor.chaos","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/8000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/8000.mcfunction).

## item_modifier `lbc:swrg_unlocks/chaos_full`

Источник: [lbc:swrg_unlocks/chaos_full](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/chaos_full.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.armor.chaos_full`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.armor.chaos_full","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/9000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/9000.mcfunction).

## item_modifier `lbc:swrg_unlocks/death_skytche`

Источник: [lbc:swrg_unlocks/death_skytche](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/death_skytche.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.hoe.death_skytche`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.hoe.death_skytche","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/10000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/10000.mcfunction).

## item_modifier `lbc:swrg_unlocks/devilstomach`

Источник: [lbc:swrg_unlocks/devilstomach](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/devilstomach.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.accessories.devil_stomach`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.accessories.devil_stomach","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/9000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/9000.mcfunction).

## item_modifier `lbc:swrg_unlocks/double`

Источник: [lbc:swrg_unlocks/double](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/double.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.bow.double`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.bow.double","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/10000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/10000.mcfunction).

## item_modifier `lbc:swrg_unlocks/erosion`

Источник: [lbc:swrg_unlocks/erosion](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/erosion.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.armor.erosion`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.armor.erosion","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/10000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/10000.mcfunction).

## item_modifier `lbc:swrg_unlocks/evoker_academy`

Источник: [lbc:swrg_unlocks/evoker_academy](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/evoker_academy.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.upgraded_staff.evoker`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.upgraded_staff.evoker","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/11000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/11000.mcfunction).

## item_modifier `lbc:swrg_unlocks/exahoming_academy`

Источник: [lbc:swrg_unlocks/exahoming_academy](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/exahoming_academy.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.upgraded_tuntija.exahoming`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.upgraded_tuntija.exahoming","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/10100.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/10100.mcfunction).

## item_modifier `lbc:swrg_unlocks/exoperso_academy`

Источник: [lbc:swrg_unlocks/exoperso_academy](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/exoperso_academy.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.upgraded_staff.exoperso`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.upgraded_staff.exoperso","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/11000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/11000.mcfunction).

## item_modifier `lbc:swrg_unlocks/falling_stars_academy`

Источник: [lbc:swrg_unlocks/falling_stars_academy](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/falling_stars_academy.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.upgraded_staff.falling_stars`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.upgraded_staff.falling_stars","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/11000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/11000.mcfunction).

## item_modifier `lbc:swrg_unlocks/fartus_nadristailo`

Источник: [lbc:swrg_unlocks/fartus_nadristailo](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/fartus_nadristailo.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.fartus_nadristailo_upgrade`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.fartus_nadristailo_upgrade","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/9000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/9000.mcfunction).

## item_modifier `lbc:swrg_unlocks/firework`

Источник: [lbc:swrg_unlocks/firework](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/firework.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.bow.firework`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.bow.firework","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/9000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/9000.mcfunction).

## item_modifier `lbc:swrg_unlocks/flesh`

Источник: [lbc:swrg_unlocks/flesh](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/flesh.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.armor.flesh`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.armor.flesh","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/8000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/8000.mcfunction).

## item_modifier `lbc:swrg_unlocks/gaycaster_academy`

Источник: [lbc:swrg_unlocks/gaycaster_academy](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/gaycaster_academy.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.upgraded_staff.gaycaster`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.upgraded_staff.gaycaster","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/10100.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/10100.mcfunction).

## item_modifier `lbc:swrg_unlocks/ghast`

Источник: [lbc:swrg_unlocks/ghast](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/ghast.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.bow.ghast`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.bow.ghast","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/11000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/11000.mcfunction).

## item_modifier `lbc:swrg_unlocks/glass`

Источник: [lbc:swrg_unlocks/glass](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/glass.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.armor.glass`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.armor.glass","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/8000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/8000.mcfunction).

## item_modifier `lbc:swrg_unlocks/hammer`

Источник: [lbc:swrg_unlocks/hammer](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/hammer.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.axe.hammer`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.axe.hammer","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/10000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/10000.mcfunction).

## item_modifier `lbc:swrg_unlocks/heal`

Источник: [lbc:swrg_unlocks/heal](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/heal.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.arcanums.heal`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.arcanums.heal","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/8000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/8000.mcfunction).

## item_modifier `lbc:swrg_unlocks/hedgehog_academy`

Источник: [lbc:swrg_unlocks/hedgehog_academy](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/hedgehog_academy.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.upgraded_staff.hedgehog`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.upgraded_staff.hedgehog","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/11000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/11000.mcfunction).

## item_modifier `lbc:swrg_unlocks/ice_wand_academy`

Источник: [lbc:swrg_unlocks/ice_wand_academy](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/ice_wand_academy.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.upgraded_staff.ice_wand`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.upgraded_staff.ice_wand","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/11000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/11000.mcfunction).

## item_modifier `lbc:swrg_unlocks/infinite_quiver`

Источник: [lbc:swrg_unlocks/infinite_quiver](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/infinite_quiver.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.accessories.infinite_quiver`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.accessories.infinite_quiver","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/8000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/8000.mcfunction).

## item_modifier `lbc:swrg_unlocks/jaakivi`

Источник: [lbc:swrg_unlocks/jaakivi](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/jaakivi.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.accessories.jaakivi`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.accessories.jaakivi","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/10000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/10000.mcfunction).

## item_modifier `lbc:swrg_unlocks/kiuaskivi`

Источник: [lbc:swrg_unlocks/kiuaskivi](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/kiuaskivi.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.arcanums.kiuaskivi`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.arcanums.kiuaskivi","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/9000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/9000.mcfunction).

## item_modifier `lbc:swrg_unlocks/lightning_upgraded`

Источник: [lbc:swrg_unlocks/lightning_upgraded](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/lightning_upgraded.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.upgraded_staff.lightning`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.upgraded_staff.lightning","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/9100.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/9100.mcfunction).

## item_modifier `lbc:swrg_unlocks/madoka_bow`

Источник: [lbc:swrg_unlocks/madoka_bow](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/madoka_bow.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.bow.madoka_bow`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.bow.madoka_bow","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/10100.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/10100.mcfunction).

## item_modifier `lbc:swrg_unlocks/mage`

Источник: [lbc:swrg_unlocks/mage](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/mage.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.armor.mage`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.armor.mage","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/8000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/8000.mcfunction).

## item_modifier `lbc:swrg_unlocks/magnet`

Источник: [lbc:swrg_unlocks/magnet](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/magnet.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.accessories.magnet`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.accessories.magnet","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/10000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/10000.mcfunction).

## item_modifier `lbc:swrg_unlocks/mana_belt`

Источник: [lbc:swrg_unlocks/mana_belt](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/mana_belt.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.accessories.mana_belt`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.accessories.mana_belt","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/8000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/8000.mcfunction).

## item_modifier `lbc:swrg_unlocks/miniguns`

Источник: [lbc:swrg_unlocks/miniguns](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/miniguns.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.bow.minigun`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.bow.minigun","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/10000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/10000.mcfunction).

## item_modifier `lbc:swrg_unlocks/moisei_boots`

Источник: [lbc:swrg_unlocks/moisei_boots](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/moisei_boots.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.armor.moisei`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.armor.moisei","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/9000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/9000.mcfunction).

## item_modifier `lbc:swrg_unlocks/multiplying`

Источник: [lbc:swrg_unlocks/multiplying](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/multiplying.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.armor.multiplying`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.armor.multiplying","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/9000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/9000.mcfunction).

## item_modifier `lbc:swrg_unlocks/mystical_artifact`

Источник: [lbc:swrg_unlocks/mystical_artifact](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/mystical_artifact.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.accessories.mystical_artifact`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.accessories.mystical_artifact","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/10000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/10000.mcfunction).

## item_modifier `lbc:swrg_unlocks/nano`

Источник: [lbc:swrg_unlocks/nano](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/nano.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.armor.nano`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.armor.nano","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/9000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/9000.mcfunction).

## item_modifier `lbc:swrg_unlocks/neutral`

Источник: [lbc:swrg_unlocks/neutral](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/neutral.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.arcanums.neutral`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.arcanums.neutral","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/8000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/8000.mcfunction).

## item_modifier `lbc:swrg_unlocks/neutral_wand_academy`

Источник: [lbc:swrg_unlocks/neutral_wand_academy](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/neutral_wand_academy.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.upgraded_staff.neutral`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.upgraded_staff.neutral","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/11000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/11000.mcfunction).

## item_modifier `lbc:swrg_unlocks/nyauwu`

Источник: [lbc:swrg_unlocks/nyauwu](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/nyauwu.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.armor.nyauwu`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.armor.nyauwu","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/10000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/10000.mcfunction).

## item_modifier `lbc:swrg_unlocks/omegaflag_academy`

Источник: [lbc:swrg_unlocks/omegaflag_academy](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/omegaflag_academy.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.upgraded_staff.omega_flag`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.upgraded_staff.omega_flag","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/10100.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/10100.mcfunction).

## item_modifier `lbc:swrg_unlocks/op_hoe`

Источник: [lbc:swrg_unlocks/op_hoe](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/op_hoe.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.hoe.op_hoe`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.hoe.op_hoe","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/9000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/9000.mcfunction).

## item_modifier `lbc:swrg_unlocks/paladin`

Источник: [lbc:swrg_unlocks/paladin](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/paladin.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.armor.paladin`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.armor.paladin","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/10000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/10000.mcfunction).

## item_modifier `lbc:swrg_unlocks/penis_gun`

Источник: [lbc:swrg_unlocks/penis_gun](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/penis_gun.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.bow.penis_gun`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.bow.penis_gun","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/11000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/11000.mcfunction).

## item_modifier `lbc:swrg_unlocks/phantom`

Источник: [lbc:swrg_unlocks/phantom](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/phantom.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.armor.phantom`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.armor.phantom","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/9000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/9000.mcfunction).

## item_modifier `lbc:swrg_unlocks/poleaxe`

Источник: [lbc:swrg_unlocks/poleaxe](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/poleaxe.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.axe.poleaxe`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.axe.poleaxe","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/8000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/8000.mcfunction).

## item_modifier `lbc:swrg_unlocks/polymorph_wand_academy`

Источник: [lbc:swrg_unlocks/polymorph_wand_academy](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/polymorph_wand_academy.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.upgraded_staff.polymorph`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.upgraded_staff.polymorph","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/11000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/11000.mcfunction).

## item_modifier `lbc:swrg_unlocks/popit`

Источник: [lbc:swrg_unlocks/popit](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/popit.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.accessories.popit`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.accessories.popit","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/9000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/9000.mcfunction).

## item_modifier `lbc:swrg_unlocks/prism_academy`

Источник: [lbc:swrg_unlocks/prism_academy](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/prism_academy.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.upgraded_staff.prism`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.upgraded_staff.prism","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/11000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/11000.mcfunction).

## item_modifier `lbc:swrg_unlocks/prism_psycho`

Источник: [lbc:swrg_unlocks/prism_psycho](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/prism_psycho.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.upgraded_staff.psycho`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.upgraded_staff.psycho","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/11000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/11000.mcfunction).

## item_modifier `lbc:swrg_unlocks/psychoblind_academy`

Источник: [lbc:swrg_unlocks/psychoblind_academy](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/psychoblind_academy.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.upgraded_tuntija.psychoblind`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.upgraded_tuntija.psychoblind","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/10100.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/10100.mcfunction).

## item_modifier `lbc:swrg_unlocks/railgun`

Источник: [lbc:swrg_unlocks/railgun](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/railgun.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.bow.railgun`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.bow.railgun","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/8000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/8000.mcfunction).

## item_modifier `lbc:swrg_unlocks/railshish`

Источник: [lbc:swrg_unlocks/railshish](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/railshish.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.bow.railshish`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.bow.railshish","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/8100.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/8100.mcfunction).

## item_modifier `lbc:swrg_unlocks/randchanted`

Источник: [lbc:swrg_unlocks/randchanted](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/randchanted.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.armor.randchanted`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.armor.randchanted","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/10000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/10000.mcfunction).

## item_modifier `lbc:swrg_unlocks/randchanted_axe`

Источник: [lbc:swrg_unlocks/randchanted_axe](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/randchanted_axe.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.axe.randchanted`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.axe.randchanted","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/9000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/9000.mcfunction).

## item_modifier `lbc:swrg_unlocks/randchanted_bow`

Источник: [lbc:swrg_unlocks/randchanted_bow](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/randchanted_bow.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.bow.randchanted`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.bow.randchanted","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/8100.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/8100.mcfunction).

## item_modifier `lbc:swrg_unlocks/regeneration_belt`

Источник: [lbc:swrg_unlocks/regeneration_belt](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/regeneration_belt.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.accessories.regeneration_belt`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.accessories.regeneration_belt","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/8000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/8000.mcfunction).

## item_modifier `lbc:swrg_unlocks/ruby_upgraded`

Источник: [lbc:swrg_unlocks/ruby_upgraded](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/ruby_upgraded.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.upgraded_tuntija.ruby`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.upgraded_tuntija.ruby","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/9100.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/9100.mcfunction).

## item_modifier `lbc:swrg_unlocks/sand_amulet`

Источник: [lbc:swrg_unlocks/sand_amulet](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/sand_amulet.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.accessories.sand_amulet`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.accessories.sand_amulet","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/9000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/9000.mcfunction).

## item_modifier `lbc:swrg_unlocks/save_platform`

Источник: [lbc:swrg_unlocks/save_platform](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/save_platform.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.accessories.save_platform`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.accessories.save_platform","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/8000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/8000.mcfunction).

## item_modifier `lbc:swrg_unlocks/shitty`

Источник: [lbc:swrg_unlocks/shitty](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/shitty.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.armor.shitty`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.armor.shitty","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/9000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/9000.mcfunction).

## item_modifier `lbc:swrg_unlocks/shitty_full`

Источник: [lbc:swrg_unlocks/shitty_full](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/shitty_full.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.armor.shitty_full`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.armor.shitty_full","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/10000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/10000.mcfunction).

## item_modifier `lbc:swrg_unlocks/shulker`

Источник: [lbc:swrg_unlocks/shulker](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/shulker.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.armor.shulker`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.armor.shulker","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/8000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/8000.mcfunction).

## item_modifier `lbc:swrg_unlocks/shulker_bow`

Источник: [lbc:swrg_unlocks/shulker_bow](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/shulker_bow.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.bow.shulker`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.bow.shulker","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/8100.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/8100.mcfunction).

## item_modifier `lbc:swrg_unlocks/shulker_full`

Источник: [lbc:swrg_unlocks/shulker_full](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/shulker_full.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.armor.shulker_full`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.armor.shulker_full","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/10000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/10000.mcfunction).

## item_modifier `lbc:swrg_unlocks/sickle`

Источник: [lbc:swrg_unlocks/sickle](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/sickle.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.hoe.sickle`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.hoe.sickle","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/10000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/10000.mcfunction).

## item_modifier `lbc:swrg_unlocks/snow_wand_academy`

Источник: [lbc:swrg_unlocks/snow_wand_academy](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/snow_wand_academy.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.upgraded_staff.snow_wand`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.upgraded_staff.snow_wand","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/11000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/11000.mcfunction).

## item_modifier `lbc:swrg_unlocks/socializm`

Источник: [lbc:swrg_unlocks/socializm](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/socializm.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.hoe.communism`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.hoe.communism","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/10000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/10000.mcfunction).

## item_modifier `lbc:swrg_unlocks/steampunk`

Источник: [lbc:swrg_unlocks/steampunk](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/steampunk.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.armor.steampunk`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.armor.steampunk","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/9000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/9000.mcfunction).

## item_modifier `lbc:swrg_unlocks/stfire_academy`

Источник: [lbc:swrg_unlocks/stfire_academy](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/stfire_academy.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.upgraded_tuntija.fire`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.upgraded_tuntija.fire","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/10100.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/10100.mcfunction).

## item_modifier `lbc:swrg_unlocks/ststone_academy`

Источник: [lbc:swrg_unlocks/ststone_academy](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/ststone_academy.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.upgraded_tuntija.stone`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.upgraded_tuntija.stone","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/10100.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/10100.mcfunction).

## item_modifier `lbc:swrg_unlocks/tannerkivi`

Источник: [lbc:swrg_unlocks/tannerkivi](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/tannerkivi.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.arcanums.tannerkivi`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.arcanums.tannerkivi","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/10000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/10000.mcfunction).

## item_modifier `lbc:swrg_unlocks/techno`

Источник: [lbc:swrg_unlocks/techno](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/techno.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.armor.techno`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.armor.techno","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/8000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/8000.mcfunction).

## item_modifier `lbc:swrg_unlocks/techno_full`

Источник: [lbc:swrg_unlocks/techno_full](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/techno_full.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.armor.techno_full`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.armor.techno_full","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/10000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/10000.mcfunction).

## item_modifier `lbc:swrg_unlocks/technomk2`

Источник: [lbc:swrg_unlocks/technomk2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/technomk2.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.armor.technomk2`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.armor.technomk2","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/9000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/9000.mcfunction).

## item_modifier `lbc:swrg_unlocks/tnt_jacket`

Источник: [lbc:swrg_unlocks/tnt_jacket](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/tnt_jacket.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.armor.tnt_jacket`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.armor.tnt_jacket","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/8000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/8000.mcfunction).

## item_modifier `lbc:swrg_unlocks/tomahawk`

Источник: [lbc:swrg_unlocks/tomahawk](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/tomahawk.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.axe.tomahawk`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.axe.tomahawk","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/8000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/8000.mcfunction).

## item_modifier `lbc:swrg_unlocks/traveller`

Источник: [lbc:swrg_unlocks/traveller](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/traveller.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.armor.traveller`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.armor.traveller","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/8000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/8000.mcfunction).

## item_modifier `lbc:swrg_unlocks/tweak_academy`

Источник: [lbc:swrg_unlocks/tweak_academy](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/tweak_academy.json).
Шаг 1: задать имя `gui.locked`; параметры `{"entity":"this","name":[{"translate":"gui.locked","color":"red","bold":true,"italic":false}]}`.
Шаг 2: задать строки описания `gui.for_openlbc.adv.academy_upgraded`; параметры `{"mode":"replace_all","entity":"this","lore":[{"translate":"gui.for_open","color":"red"},{"translate":"lbc.adv.academy_upgraded","color":"red"}]}`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/gui/page/12000.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/page/12000.mcfunction).

## item_modifier `lbc:testdaeris`

Источник: [lbc:testdaeris](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/testdaeris.json).
Шаг 1: задать модификаторы атрибутов `{"modifiers":[]}`.
Шаг 2: добавить/перезаписать метки custom_data `{testdaeris:2}`.
Буквальных ссылок на этот точный ID в исследуемых функциях/JSON не найдено; ресурс доступен при ручном или динамическом вызове.

## item_modifier `lbc:tiers/a`

Источник: [lbc:tiers/a](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/tiers/a.json).
Шаг 1: оформление подсказки: `lbc:tier_a`.
Буквальные ссылки на ID: [lbc/function/other/craft_uniwand_with_grimoire_value_check.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft_uniwand_with_grimoire_value_check.mcfunction), [lbc/function/other/magic_academy/upgrade/badwiki.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/badwiki.mcfunction), [lbc/function/other/magic_academy/upgrade/bloodmagic.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/bloodmagic.mcfunction), [lbc/function/other/magic_academy/upgrade/enderwand.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/enderwand.mcfunction), [lbc/function/other/magic_academy/upgrade/fallingstars.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/fallingstars.mcfunction), [lbc/function/other/magic_academy/upgrade/foxusing.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/foxusing.mcfunction), [lbc/function/other/magic_academy/upgrade/lifestealer.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/lifestealer.mcfunction), [lbc/function/other/magic_academy/upgrade/lightningwand.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/lightningwand.mcfunction), [lbc/function/other/magic_academy/upgrade/neutralmage.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/neutralmage.mcfunction), [lbc/function/other/magic_academy/upgrade/prisma.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/prisma.mcfunction), [lbc/function/other/magic_academy/upgrade/soulcorrupter.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/soulcorrupter.mcfunction), [lbc/function/other/magic_academy/upgrade/stacid.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/stacid.mcfunction), [lbc/function/other/magic_academy/upgrade/stelectro.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/stelectro.mcfunction), [lbc/function/other/magic_academy/upgrade/stfire.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/stfire.mcfunction), [lbc/function/other/magic_academy/upgrade/ststone.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/ststone.mcfunction), [lbc/function/other/magic_academy/upgrade/stteleport.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/stteleport.mcfunction), [lbc/function/other/magic_academy/upgrade/stterra.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/stterra.mcfunction), [lbc/function/other/magic_academy/upgrade/truefallingstars.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/truefallingstars.mcfunction), [lbc/function/other/magic_academy/upgrade/vex_wand.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/vex_wand.mcfunction), [lbc/function/other/magic_academy/upgrade/wall.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/wall.mcfunction), [lbc/function/workingitems/evolution.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/evolution.mcfunction).

## item_modifier `lbc:tiers/b`

Источник: [lbc:tiers/b](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/tiers/b.json).
Шаг 1: оформление подсказки: `lbc:tier_b`.
Буквальные ссылки на ID: [lbc/function/other/craft_uniwand_with_grimoire_value_check.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft_uniwand_with_grimoire_value_check.mcfunction), [lbc/function/other/magic_academy/upgrade/bassboost.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/bassboost.mcfunction), [lbc/function/other/magic_academy/upgrade/evocator.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/evocator.mcfunction), [lbc/function/other/magic_academy/upgrade/omegaflag.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/omegaflag.mcfunction), [lbc/function/other/magic_academy/upgrade/psychowand.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/psychowand.mcfunction), [lbc/function/other/magic_academy/upgrade/snow_wand.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/snow_wand.mcfunction), [lbc/function/other/magic_academy/upgrade/stblind.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/stblind.mcfunction), [lbc/function/other/magic_academy/upgrade/struby.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/struby.mcfunction), [lbc/function/workingitems/evolution.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/evolution.mcfunction).

## item_modifier `lbc:tiers/c`

Источник: [lbc:tiers/c](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/tiers/c.json).
Шаг 1: оформление подсказки: `lbc:tier_c`.
Буквальные ссылки на ID: [lbc/function/other/craft_uniwand_with_grimoire_value_check.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft_uniwand_with_grimoire_value_check.mcfunction), [lbc/function/other/magic_academy/upgrade/acidwand.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/acidwand.mcfunction), [lbc/function/other/magic_academy/upgrade/corrupter.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/corrupter.mcfunction), [lbc/function/workingitems/evolution.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/evolution.mcfunction).

## item_modifier `lbc:tiers/d`

Источник: [lbc:tiers/d](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/tiers/d.json).
Шаг 1: оформление подсказки: `lbc:tier_d`.
Буквальные ссылки на ID: [lbc/function/other/craft_uniwand_with_grimoire_value_check.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft_uniwand_with_grimoire_value_check.mcfunction), [lbc/function/workingitems/evolution.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/evolution.mcfunction).

## item_modifier `lbc:tiers/f`

Источник: [lbc:tiers/f](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/tiers/f.json).
Шаг 1: оформление подсказки: `lbc:tier_f`.
Буквальные ссылки на ID: [lbc/function/other/craft_uniwand_with_grimoire_value_check.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft_uniwand_with_grimoire_value_check.mcfunction), [lbc/function/workingitems/evolution.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/evolution.mcfunction).

## item_modifier `lbc:tiers/ff`

Источник: [lbc:tiers/ff](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/tiers/ff.json).
Шаг 1: оформление подсказки: `lbc:tier_ff`.
Буквальные ссылки на ID: [lbc/function/other/craft_uniwand_with_grimoire_value_check.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft_uniwand_with_grimoire_value_check.mcfunction).

## item_modifier `lbc:tiers/k`

Источник: [lbc:tiers/k](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/tiers/k.json).
Шаг 1: оформление подсказки: `lbc:tier_k`.
Буквальных ссылок на этот точный ID в исследуемых функциях/JSON не найдено; ресурс доступен при ручном или динамическом вызове.

## item_modifier `lbc:tiers/s`

Источник: [lbc:tiers/s](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/tiers/s.json).
Шаг 1: оформление подсказки: `lbc:tier_s`.
Буквальные ссылки на ID: [lbc/function/other/craft_uniwand_with_grimoire_value_check.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft_uniwand_with_grimoire_value_check.mcfunction), [lbc/function/other/magic_academy/upgrade/advancedteleport.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/advancedteleport.mcfunction), [lbc/function/other/magic_academy/upgrade/baffledhedgehog.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/baffledhedgehog.mcfunction), [lbc/function/other/magic_academy/upgrade/danmaku_staff.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/danmaku_staff.mcfunction), [lbc/function/other/magic_academy/upgrade/exoperso.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/exoperso.mcfunction), [lbc/function/other/magic_academy/upgrade/fireblaze_wand.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/fireblaze_wand.mcfunction), [lbc/function/other/magic_academy/upgrade/gaycaster.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/gaycaster.mcfunction), [lbc/function/other/magic_academy/upgrade/lightning_bolts.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/lightning_bolts.mcfunction), [lbc/function/other/magic_academy/upgrade/stair.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/stair.mcfunction), [lbc/function/workingitems/evolution.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/evolution.mcfunction).

## item_modifier `lbc:tiers/ss`

Источник: [lbc:tiers/ss](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/tiers/ss.json).
Шаг 1: оформление подсказки: `lbc:tier_ss`.
Буквальные ссылки на ID: [lbc/function/other/craft_uniwand_with_grimoire_value_check.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft_uniwand_with_grimoire_value_check.mcfunction), [lbc/function/other/magic_academy/upgrade/acidacid_wand.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/acidacid_wand.mcfunction), [lbc/function/other/magic_academy/upgrade/acidcorrupt_wand.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/acidcorrupt_wand.mcfunction), [lbc/function/other/magic_academy/upgrade/bedrock_wand.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/bedrock_wand.mcfunction), [lbc/function/other/magic_academy/upgrade/boltbolt_wand.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/boltbolt_wand.mcfunction), [lbc/function/other/magic_academy/upgrade/dublicating_wand.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/dublicating_wand.mcfunction), [lbc/function/other/magic_academy/upgrade/erecto_wand.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/erecto_wand.mcfunction), [lbc/function/other/magic_academy/upgrade/ice_wand.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/ice_wand.mcfunction), [lbc/function/other/magic_academy/upgrade/psychoblind_wand.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/psychoblind_wand.mcfunction), [lbc/function/other/magic_academy/upgrade/unstable_spellbook.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/unstable_spellbook.mcfunction), [lbc/function/workingitems/evolution.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/evolution.mcfunction).

## item_modifier `lbc:tiers/u`

Источник: [lbc:tiers/u](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/tiers/u.json).
Шаг 1: оформление подсказки: `lbc:tier_u`.
Буквальных ссылок на этот точный ID в исследуемых функциях/JSON не найдено; ресурс доступен при ручном или динамическом вызове.

## item_modifier `lbc:tiers/x`

Источник: [lbc:tiers/x](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/tiers/x.json).
Шаг 1: оформление подсказки: `lbc:tier_x`.
Буквальные ссылки на ID: [lbc/function/other/craft_uniwand_with_grimoire_value_check.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft_uniwand_with_grimoire_value_check.mcfunction), [lbc/function/other/magic_academy/upgrade/consuming_wand.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/consuming_wand.mcfunction), [lbc/function/other/magic_academy/upgrade/exahoming_wand.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/exahoming_wand.mcfunction), [lbc/function/other/magic_academy/upgrade/exasperato_wand.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/exasperato_wand.mcfunction), [lbc/function/other/magic_academy/upgrade/fartus_nadristailo.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/fartus_nadristailo.mcfunction), [lbc/function/other/magic_academy/upgrade/infbolt_wand.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/infbolt_wand.mcfunction), [lbc/function/other/magic_academy/upgrade/sauvojen_wand.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/sauvojen_wand.mcfunction), [lbc/function/other/magic_academy/upgrade/sheepwand.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/sheepwand.mcfunction), [lbc/function/other/magic_academy/upgrade/stonekivi_wand.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/stonekivi_wand.mcfunction), [lbc/function/workingitems/evolution.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/evolution.mcfunction).

## item_modifier `lbc:unbreakable`

Источник: [lbc:unbreakable](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/unbreakable.json).
Шаг 1: неразрушимость: `{}`.
Буквальные ссылки на ID: [lbc/function/other/craft/inventory_make_unbreakable.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/inventory_make_unbreakable.mcfunction), [lbc/function/swrg_kit_integration/game/skills/tanner_resist_change_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/tanner_resist_change_inv.mcfunction).

## predicate `lbc:below`

Источник: [lbc:below](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/predicate/below.json).
Успешно только при: (условие `entity_scores` с параметрами `{"entity":"this","scores":{"y":{"max":{"type":"minecraft:score","target":{"type":"minecraft:fixed","name":"*tempy"},"score":"lbcID2"}}}}`).
Буквальные ссылки на ID: [lbc/function/items/trinket_effects/sunprism_activate.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/trinket_effects/sunprism_activate.mcfunction).

## predicate `lbc:bow_or_crossbow`

Источник: [lbc:bow_or_crossbow](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/predicate/bow_or_crossbow.json).
Успешно только при: (свойства `this`: `{"equipment":{"offhand":{"items":"#minecraft:vanilla_shooting"}}}`) ИЛИ (свойства `this`: `{"equipment":{"mainhand":{"items":"#minecraft:vanilla_shooting"}}}`).
Буквальные ссылки на ID: [lbc/function/tick_nexus_player.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/tick_nexus_player.mcfunction), [lbc/function/ui/get_data.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/ui/get_data.mcfunction).

## predicate `lbc:chance001`

Источник: [lbc:chance001](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/predicate/chance001.json).
Успешно только при: случайная проверка с вероятностью `0.0001`.
Буквальные ссылки на ID: [lbc/function/other/eatables/oreshek.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/oreshek.mcfunction).

## predicate `lbc:chance0_00000000000001`

Источник: [lbc:chance0_00000000000001](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/predicate/chance0_00000000000001.json).
Успешно только при: случайная проверка с вероятностью `1e-12`.
Буквальных ссылок на этот точный ID в исследуемых функциях/JSON не найдено; ресурс доступен при ручном или динамическом вызове.

## predicate `lbc:chance0_015625`

Источник: [lbc:chance0_015625](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/predicate/chance0_015625.json).
Успешно только при: случайная проверка с вероятностью `0.00015625`.
Буквальные ссылки на ID: [lbc/function/other/nexus/explode_wire.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/explode_wire.mcfunction), [lbc/function/other/nexus/rays/pulsar_tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/rays/pulsar_tick.mcfunction), [lbc/function/workingitems/witch_gens/greed_second_mobs_near.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/witch_gens/greed_second_mobs_near.mcfunction).

## predicate `lbc:chance0_03125`

Источник: [lbc:chance0_03125](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/predicate/chance0_03125.json).
Успешно только при: случайная проверка с вероятностью `0.0003125`.
Буквальных ссылок на этот точный ID в исследуемых функциях/JSON не найдено; ресурс доступен при ручном или динамическом вызове.

## predicate `lbc:chance0_0625`

Источник: [lbc:chance0_0625](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/predicate/chance0_0625.json).
Успешно только при: случайная проверка с вероятностью `0.000625`.
Буквальных ссылок на этот точный ID в исследуемых функциях/JSON не найдено; ресурс доступен при ручном или динамическом вызове.

## predicate `lbc:chance0_125`

Источник: [lbc:chance0_125](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/predicate/chance0_125.json).
Успешно только при: случайная проверка с вероятностью `0.00125`.
Буквальные ссылки на ID: [lbc/function/drops/muravei_tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/drops/muravei_tick.mcfunction), [lbcr/function/raycast_loop7.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_loop7.mcfunction).

## predicate `lbc:chance0_25`

Источник: [lbc:chance0_25](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/predicate/chance0_25.json).
Успешно только при: случайная проверка с вероятностью `0.0025`.
Буквальные ссылки на ID: [lbc/function/items/wands/sauvojen_wand/meditate/tick_blind_picture.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/meditate/tick_blind_picture.mcfunction), [lbc/function/other/nexus/rays/sim_cube_t.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/rays/sim_cube_t.mcfunction), [lbc/function/workingitems/witch_gens/snake_big_tick_rc.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/witch_gens/snake_big_tick_rc.mcfunction).

## predicate `lbc:chance0_5`

Источник: [lbc:chance0_5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/predicate/chance0_5.json).
Успешно только при: случайная проверка с вероятностью `0.005`.
Буквальные ссылки на ID: [lbc/function/drops/helicopter_boss/tick_phase_2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/drops/helicopter_boss/tick_phase_2.mcfunction), [lbc/function/items/swords/wtf_item/tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/wtf_item/tick.mcfunction), [lbc/function/items/wands/sauvojen_wand/meditate/tick_blind_picture.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/meditate/tick_blind_picture.mcfunction), [lbc/function/other/negative_mana/tentacle_damage_restore_hp.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/negative_mana/tentacle_damage_restore_hp.mcfunction), [lbc/function/other/nexus/explode_wire.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/explode_wire.mcfunction), [lbc/function/other/silverfish_tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/silverfish_tick.mcfunction), [lbc/function/swrg_kit_integration/items/nanotrapik_tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/items/nanotrapik_tick.mcfunction).

## predicate `lbc:chance0_8`

Источник: [lbc:chance0_8](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/predicate/chance0_8.json).
Успешно только при: случайная проверка с вероятностью `0.008`.
Буквальные ссылки на ID: [lbc/function/drops/bean.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/drops/bean.mcfunction).

## predicate `lbc:chance1`

Источник: [lbc:chance1](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/predicate/chance1.json).
Успешно только при: случайная проверка с вероятностью `0.01`.
Буквальные ссылки на ID: [lbc/function/drops/helicopter_boss/tick_phase_1.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/drops/helicopter_boss/tick_phase_1.mcfunction), [lbc/function/drops/helicopter_boss/tick_phase_3.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/drops/helicopter_boss/tick_phase_3.mcfunction), [lbc/function/drops/tmt/select_random_attribute.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/drops/tmt/select_random_attribute.mcfunction), [lbc/function/items/swords/chaos_sword_imba/homing_sword.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/chaos_sword_imba/homing_sword.mcfunction), [lbc/function/items/swords/triple_golden_sword/tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/triple_golden_sword/tick.mcfunction), [lbc/function/items/wands/sauvojen_wand/meditate/tick_blind_picture.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/meditate/tick_blind_picture.mcfunction), [lbc/function/other/eatables/oreshek.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/oreshek.mcfunction), [lbc/function/other/event/halloween/soulcorruptwand/metasola/6_tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulcorruptwand/metasola/6_tick.mcfunction), [lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/tick_true.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/tick_true.mcfunction), [lbc/function/other/imba/cunny_slab/tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/tick.mcfunction), [lbc/function/other/imba/delta_ex_tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/delta_ex_tick.mcfunction), [lbc/function/other/pumpkinator/tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/pumpkinator/tick.mcfunction), [lbc/function/other/twitching.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/twitching.mcfunction), [lbc/function/swrg_kit_integration/game/skills/expansion_tick_marker_main_x-.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/expansion_tick_marker_main_x-.mcfunction), [lbc/function/swrg_kit_integration/game/skills/expansion_tick_marker_main_x.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/expansion_tick_marker_main_x.mcfunction), [lbc/function/swrg_kit_integration/game/skills/expansion_tick_marker_main_z-.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/expansion_tick_marker_main_z-.mcfunction), [lbc/function/swrg_kit_integration/game/skills/expansion_tick_marker_main_z.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/expansion_tick_marker_main_z.mcfunction), [lbc/function/swrg_kit_integration/game/skills/expansion_tick_marker_x-.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/expansion_tick_marker_x-.mcfunction), [lbc/function/swrg_kit_integration/game/skills/expansion_tick_marker_x.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/expansion_tick_marker_x.mcfunction), [lbc/function/swrg_kit_integration/game/skills/expansion_tick_marker_z-.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/expansion_tick_marker_z-.mcfunction), [lbc/function/swrg_kit_integration/game/skills/expansion_tick_marker_z.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/expansion_tick_marker_z.mcfunction), [lbc/function/swrg_kit_integration/game/trinkets/cocktail_arrow_tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/trinkets/cocktail_arrow_tick.mcfunction), [lbc/function/swrg_kit_integration/kill_effects/hammerfalling1.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/kill_effects/hammerfalling1.mcfunction), [lbc/function/tick1salways.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/tick1salways.mcfunction), [lbc/function/workingitems/imba/mystical_tree_tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/imba/mystical_tree_tick.mcfunction), [lbc/function/workingitems/witch_gens/snake_big_tick_rc.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/witch_gens/snake_big_tick_rc.mcfunction), [lbc/function/workingitems/witch_gens/whale_clone_random_sound.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/witch_gens/whale_clone_random_sound.mcfunction), [lbc/function/workingitems/witch_gens/whale_random_sound.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/witch_gens/whale_random_sound.mcfunction), [lbc/function/workingitems/witch_gens/wrath_tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/witch_gens/wrath_tick.mcfunction).

## predicate `lbc:chance10`

Источник: [lbc:chance10](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/predicate/chance10.json).
Успешно только при: случайная проверка с вероятностью `0.1`.
Буквальные ссылки на ID: [lbc/function/drops/helicopter_boss/tick_phase_3.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/drops/helicopter_boss/tick_phase_3.mcfunction), [lbc/function/drops/tmt/select_random_attribute.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/drops/tmt/select_random_attribute.mcfunction), [lbc/function/drops/tmt/select_random_enchantment.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/drops/tmt/select_random_enchantment.mcfunction), [lbc/function/effects/cumming.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/effects/cumming.mcfunction), [lbc/function/items/arcanums/eyelaser.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/arcanums/eyelaser.mcfunction), [lbc/function/items/arcanums/eyelaser1.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/arcanums/eyelaser1.mcfunction), [lbc/function/items/trinket_effects/penis_gun.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/trinket_effects/penis_gun.mcfunction), [lbc/function/items/trinket_effects/twilight_sparkle_horse.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/trinket_effects/twilight_sparkle_horse.mcfunction), [lbc/function/mobs_5t.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/mobs_5t.mcfunction), [lbc/function/other/eatables/oreshek.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/oreshek.mcfunction), [lbc/function/other/event/halloween/soulcorruptwand/metasola/3_tick_at.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulcorruptwand/metasola/3_tick_at.mcfunction), [lbc/function/other/nexus/tree_tick_move.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/tree_tick_move.mcfunction), [lbc/function/other/transformator.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/transformator.mcfunction), [lbc/function/other/twitching.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/twitching.mcfunction), [lbc/function/projectilestick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectilestick.mcfunction), [lbc/function/swrg_kit_integration/game/skills/expansion_tick_marker_main_x-.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/expansion_tick_marker_main_x-.mcfunction), [lbc/function/swrg_kit_integration/game/skills/expansion_tick_marker_main_x.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/expansion_tick_marker_main_x.mcfunction), [lbc/function/swrg_kit_integration/game/skills/expansion_tick_marker_main_z-.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/expansion_tick_marker_main_z-.mcfunction), [lbc/function/swrg_kit_integration/game/skills/expansion_tick_marker_main_z.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/expansion_tick_marker_main_z.mcfunction), [lbc/function/swrg_kit_integration/game/skills/expansion_tick_marker_x-.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/expansion_tick_marker_x-.mcfunction), [lbc/function/swrg_kit_integration/game/skills/expansion_tick_marker_x.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/expansion_tick_marker_x.mcfunction), [lbc/function/swrg_kit_integration/game/skills/expansion_tick_marker_z-.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/expansion_tick_marker_z-.mcfunction), [lbc/function/swrg_kit_integration/game/skills/expansion_tick_marker_z.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/expansion_tick_marker_z.mcfunction), [lbc/function/workingitems/clock_fragment/jump_1s.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/clock_fragment/jump_1s.mcfunction), [lbc/function/workingitems/corrupter.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/corrupter.mcfunction), [lbc/function/workingitems/corrupter_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/corrupter_upgraded.mcfunction), [lbc/function/workingitems/imba/funny_tnt.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/imba/funny_tnt.mcfunction), [lbc/function/workingitems/witch_gens/whale_second.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/witch_gens/whale_second.mcfunction), [lbcr/function/railgun_lol.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/railgun_lol.mcfunction), [lbcr/function/raycast_preloop7_2_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_preloop7_2_upgraded.mcfunction).

## predicate `lbc:chance15`

Источник: [lbc:chance15](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/predicate/chance15.json).
Успешно только при: случайная проверка с вероятностью `0.15`.
Буквальные ссылки на ID: [lbc/function/items/swords/fevgalea_star/click_uncharged_shift_rc.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/fevgalea_star/click_uncharged_shift_rc.mcfunction), [lbc/function/items/swords/piston_sword/ray.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/piston_sword/ray.mcfunction), [lbc/function/items/trinket_effects/acid.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/trinket_effects/acid.mcfunction), [lbc/function/items/trinket_effects/bass.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/trinket_effects/bass.mcfunction), [lbc/function/items/trinket_effects/blaze.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/trinket_effects/blaze.mcfunction), [lbc/function/items/wands/sauvojen_wand/meditate/meditator_tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/meditate/meditator_tick.mcfunction), [lbc/function/other/eatables/banana.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/banana.mcfunction), [lbc/function/workingitems/clock_fragment/jump_2s.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/clock_fragment/jump_2s.mcfunction), [lbc/function/workingitems/clock_fragment/jump_5s.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/clock_fragment/jump_5s.mcfunction), [lbc/function/workingitems/psychowand/scaredloop.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/psychowand/scaredloop.mcfunction), [lbcr/function/raycast_loop7.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_loop7.mcfunction).

## predicate `lbc:chance1_5`

Источник: [lbc:chance1_5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/predicate/chance1_5.json).
Успешно только при: случайная проверка с вероятностью `0.015`.
Буквальных ссылок на этот точный ID в исследуемых функциях/JSON не найдено; ресурс доступен при ручном или динамическом вызове.

## predicate `lbc:chance20`

Источник: [lbc:chance20](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/predicate/chance20.json).
Успешно только при: случайная проверка с вероятностью `0.2`.
Буквальные ссылки на ID: [lbc/function/effects/termit_tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/effects/termit_tick.mcfunction), [lbc/function/items/bows/kaname_bow/tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/bows/kaname_bow/tick.mcfunction), [lbc/function/items/damaged.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/damaged.mcfunction), [lbc/function/items/wands/sauvojen_wand/meditate/phase_acid_tick_baller_ball.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/meditate/phase_acid_tick_baller_ball.mcfunction), [lbc/function/mobs_1s.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/mobs_1s.mcfunction), [lbc/function/other/craft/crafted_dust.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/crafted_dust.mcfunction), [lbc/function/other/craft/crafted_dust_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/crafted_dust_inv.mcfunction), [lbc/function/other/kapusta/explode.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/kapusta/explode.mcfunction), [lbc/function/other/nexus/tree_tick_move.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/tree_tick_move.mcfunction), [lbc/function/other/silverfish_tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/silverfish_tick.mcfunction), [lbc/function/swrg_kit_integration/game/skills/damaged.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/damaged.mcfunction), [lbc/function/swrg_kit_integration/game/skills/expansion_active.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/expansion_active.mcfunction), [lbc/function/swrg_kit_integration/game/skills/god_claw_tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/god_claw_tick.mcfunction), [lbc/function/swrg_kit_integration/game/skills/rainbow_aura/aura_purple_tick_particle.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/rainbow_aura/aura_purple_tick_particle.mcfunction), [lbc/function/workingitems/awaken_tuntija_wands/acidacid/tick_caust.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/awaken_tuntija_wands/acidacid/tick_caust.mcfunction), [lbc/function/workingitems/awaken_tuntija_wands/acidcorrupt/ball_tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/awaken_tuntija_wands/acidcorrupt/ball_tick.mcfunction), [lbc/function/workingitems/awaken_tuntija_wands/erecto/central_end_2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/awaken_tuntija_wands/erecto/central_end_2.mcfunction), [lbc/function/workingitems/awaken_tuntija_wands/exasperato/chtick_electro_ray.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/awaken_tuntija_wands/exasperato/chtick_electro_ray.mcfunction), [lbc/function/workingitems/clock_fragment/jump_1s.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/clock_fragment/jump_1s.mcfunction), [lbc/function/workingitems/clock_fragment/jump_2s.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/clock_fragment/jump_2s.mcfunction), [lbc/function/workingitems/corrupted_pentagram/ahriman.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/corrupted_pentagram/ahriman.mcfunction), [lbc/function/workingitems/corrupted_pentagram/belial.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/corrupted_pentagram/belial.mcfunction), [lbc/function/workingitems/corrupted_pentagram/lamashtu.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/corrupted_pentagram/lamashtu.mcfunction), [lbc/function/workingitems/corrupted_pentagram/sigyl_bafomet.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/corrupted_pentagram/sigyl_bafomet.mcfunction), [lbc/function/workingitems/sauvojen_staffs/stone_preloop_upgrade.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/sauvojen_staffs/stone_preloop_upgrade.mcfunction), [lbc/function/workingitems/sauvojen_staffs/terra_preloop_check_upgrade.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/sauvojen_staffs/terra_preloop_check_upgrade.mcfunction), [lbcr/function/random_debuff.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/random_debuff.mcfunction), [lbcr/function/raycast_acid.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_acid.mcfunction), [lbcr/function/raycast_molnia_phantom.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_molnia_phantom.mcfunction).

## predicate `lbc:chance25`

Источник: [lbc:chance25](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/predicate/chance25.json).
Успешно только при: случайная проверка с вероятностью `0.25`.
Буквальные ссылки на ID: [lbc/function/effects/effect_lightarrow.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/effects/effect_lightarrow.mcfunction), [lbc/function/items/trinket_effects/ghast.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/trinket_effects/ghast.mcfunction), [lbc/function/items/trinket_effects/tnt.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/trinket_effects/tnt.mcfunction), [lbc/function/projectiles/arrows/inairtrappertrail.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/arrows/inairtrappertrail.mcfunction), [lbc/function/swrg_kit_integration/items/steampunk_pickaxe_use.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/items/steampunk_pickaxe_use.mcfunction), [lbc/function/tick1salways.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/tick1salways.mcfunction), [lbc/function/workingitems/clock_fragment/jump_1s.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/clock_fragment/jump_1s.mcfunction), [lbc/function/workingitems/seaguardian.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/seaguardian.mcfunction).

## predicate `lbc:chance3`

Источник: [lbc:chance3](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/predicate/chance3.json).
Успешно только при: случайная проверка с вероятностью `0.03`.
Буквальные ссылки на ID: [lbc/function/drops/muravei_tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/drops/muravei_tick.mcfunction), [lbc/function/effects/sulfur_tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/effects/sulfur_tick.mcfunction), [lbc/function/other/absurd_child_tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/absurd_child_tick.mcfunction), [lbc/function/other/nexus/click/orb_gun/uncraft.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/uncraft.mcfunction), [lbc/function/swrg_kit_integration/game/skills/rainbow_aura/aura_blue_tick_particle.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/rainbow_aura/aura_blue_tick_particle.mcfunction), [lbc/function/swrg_kit_integration/game/skills/rainbow_aura/red_spin_particle_tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/rainbow_aura/red_spin_particle_tick.mcfunction), [lbc/function/workingitems/clock_fragment/jump_1s.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/clock_fragment/jump_1s.mcfunction), [lbc/function/workingitems/witch_gens/whale_clone_second.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/witch_gens/whale_clone_second.mcfunction), [lbcr/function/damagewithid7_check_blinded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/damagewithid7_check_blinded.mcfunction), [lbcr/function/raycast_molnia_cunny_2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_molnia_cunny_2.mcfunction).

## predicate `lbc:chance30`

Источник: [lbc:chance30](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/predicate/chance30.json).
Успешно только при: случайная проверка с вероятностью `0.3`.
Буквальные ссылки на ID: [lbc/function/other/craft/crafted_reforge.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/crafted_reforge.mcfunction), [lbc/function/other/grimoire_dungeon/mobs/golden_skeleton/portal_tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/golden_skeleton/portal_tick.mcfunction), [lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/tick_true.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/tick_true.mcfunction), [lbc/function/other/imba/blood_slab/tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/blood_slab/tick.mcfunction), [lbc/function/projectiles/racer_arrow.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/racer_arrow.mcfunction), [lbc/function/swrg_kit_integration/game/skills/absolute_cinema/akyla_devid_tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/absolute_cinema/akyla_devid_tick.mcfunction), [lbc/function/swrg_kit_integration/game/win_effects/winpumpkin.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/win_effects/winpumpkin.mcfunction), [lbc/function/workingitems/sauvojen_staffs_tiny/blind_2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/sauvojen_staffs_tiny/blind_2.mcfunction), [lbc/function/workingitems/sauvojen_staffs_tiny/terra_2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/sauvojen_staffs_tiny/terra_2.mcfunction).

## predicate `lbc:chance35`

Источник: [lbc:chance35](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/predicate/chance35.json).
Успешно только при: случайная проверка с вероятностью `0.35`.
Буквальные ссылки на ID: [lbc/function/effects/effect_evokerarrow.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/effects/effect_evokerarrow.mcfunction), [lbc/function/other/sauvojen_tuntija/select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sauvojen_tuntija/select.mcfunction), [lbc/function/projectiles/homing_hit.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/homing_hit.mcfunction), [lbc/function/workingitems/awaken_tuntija_wands/erecto/click_upgraded_2_mode_ray_loop.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/awaken_tuntija_wands/erecto/click_upgraded_2_mode_ray_loop.mcfunction), [lbc/function/workingitems/awaken_tuntija_wands/erecto/click_upgraded_2_mode_sphere.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/awaken_tuntija_wands/erecto/click_upgraded_2_mode_sphere.mcfunction), [lbc/function/workingitems/clock_fragment/jump_10s.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/clock_fragment/jump_10s.mcfunction), [lbc/function/workingitems/clock_fragment/jump_1s.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/clock_fragment/jump_1s.mcfunction), [lbc/function/workingitems/clock_fragment/jump_5s.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/clock_fragment/jump_5s.mcfunction), [lbc/function/workingitems/sauvojen_staffs/electro_ray.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/sauvojen_staffs/electro_ray.mcfunction), [lbc/function/workingitems/sauvojen_staffs/teleport_preloop_upgrade.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/sauvojen_staffs/teleport_preloop_upgrade.mcfunction), [lbcr/function/damagewithid9_rad.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/damagewithid9_rad.mcfunction), [lbcr/function/railgun_lol.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/railgun_lol.mcfunction), [lbcr/function/raycast_loop7_stabilized.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_loop7_stabilized.mcfunction).

## predicate `lbc:chance4`

Источник: [lbc:chance4](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/predicate/chance4.json).
Успешно только при: случайная проверка с вероятностью `0.04`.
Буквальные ссылки на ID: [lbc/function/other/event/halloween/particle_wind.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/particle_wind.mcfunction).

## predicate `lbc:chance40`

Источник: [lbc:chance40](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/predicate/chance40.json).
Успешно только при: случайная проверка с вероятностью `0.4`.
Буквальные ссылки на ID: [lbc/function/drops/helicopter_boss/tick_phase_2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/drops/helicopter_boss/tick_phase_2.mcfunction), [lbc/function/other/craft/crafted_electroshield.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/crafted_electroshield.mcfunction), [lbc/function/other/craft/crafted_electrosword.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/crafted_electrosword.mcfunction), [lbc/function/other/craft/crafted_railgun.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/crafted_railgun.mcfunction), [lbc/function/other/craft/crafted_techno_armor.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/crafted_techno_armor.mcfunction), [lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/phase_meteors_spawn_2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/phase_meteors_spawn_2.mcfunction), [lbc/function/other/twitching.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/twitching.mcfunction), [lbc/function/other/unstablearea_rc.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/unstablearea_rc.mcfunction), [lbc/function/swrg_kit_integration/game/skills/damaged.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/damaged.mcfunction), [lbc/function/swrg_kit_integration/game/skills/god_claw_tick_marker.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/god_claw_tick_marker.mcfunction), [lbc/function/workingitems/awaken_tuntija_wands/acidacid/tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/awaken_tuntija_wands/acidacid/tick.mcfunction), [lbc/function/workingitems/clock_fragment/jump_2s.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/clock_fragment/jump_2s.mcfunction), [lbc/function/workingitems/witch_gens/greed_second_mobs_near.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/witch_gens/greed_second_mobs_near.mcfunction).

## predicate `lbc:chance45`

Источник: [lbc:chance45](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/predicate/chance45.json).
Успешно только при: случайная проверка с вероятностью `0.45`.
Буквальных ссылок на этот точный ID в исследуемых функциях/JSON не найдено; ресурс доступен при ручном или динамическом вызове.

## predicate `lbc:chance5`

Источник: [lbc:chance5](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/predicate/chance5.json).
Успешно только при: случайная проверка с вероятностью `0.05`.
Буквальные ссылки на ID: [lbc/function/drops/antigravity/tick_10t.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/drops/antigravity/tick_10t.mcfunction), [lbc/function/effects/cumming.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/effects/cumming.mcfunction), [lbc/function/items/arcanums/kiuaskivi_tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/arcanums/kiuaskivi_tick.mcfunction), [lbc/function/items/on_sprint.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/on_sprint.mcfunction), [lbc/function/items/swords/fevgalea_star/star_raindrop_tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/fevgalea_star/star_raindrop_tick.mcfunction), [lbc/function/items/wands/sauvojen_wand/meditate/tick_blind_face.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/meditate/tick_blind_face.mcfunction), [lbc/function/mobs_5t_lbc.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/mobs_5t_lbc.mcfunction), [lbc/function/other/effect_shifting.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/effect_shifting.mcfunction), [lbc/function/other/imba/blood_slab/tick_move.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/blood_slab/tick_move.mcfunction), [lbc/function/other/imba/most_tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/most_tick.mcfunction), [lbc/function/other/nexus/explode_wire.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/explode_wire.mcfunction), [lbc/function/other/nexus/tree_end.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/tree_end.mcfunction), [lbc/function/other/transformator.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/transformator.mcfunction), [lbc/function/other/troll_box/spawn_trollge_pre.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/troll_box/spawn_trollge_pre.mcfunction), [lbc/function/swrg_kit_integration/game/skills/rainbow_aura/aura_blue_tick_particle.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/rainbow_aura/aura_blue_tick_particle.mcfunction), [lbc/function/swrg_kit_integration/game/trinkets/volos_arrow_tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/trinkets/volos_arrow_tick.mcfunction), [lbc/function/tick1salways.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/tick1salways.mcfunction), [lbc/function/workingitems/ak47_rec.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/ak47_rec.mcfunction), [lbc/function/workingitems/awaken_tuntija_wands/consuming_wand_upgraded_mode_the_end.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/awaken_tuntija_wands/consuming_wand_upgraded_mode_the_end.mcfunction), [lbc/function/workingitems/awaken_tuntija_wands/exahoming_click_upgraded_mode_destiny_tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/awaken_tuntija_wands/exahoming_click_upgraded_mode_destiny_tick.mcfunction), [lbc/function/workingitems/corrupted_pentagram/belial_mob.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/corrupted_pentagram/belial_mob.mcfunction), [lbc/function/workingitems/sauvojen_staffs/fire_preloop_upgrade.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/sauvojen_staffs/fire_preloop_upgrade.mcfunction), [lbc/function/workingitems/witch_gens/whale_second.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/witch_gens/whale_second.mcfunction), [lbcr/function/raycast_end6.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_end6.mcfunction), [lbcr/function/raycast_end6_left.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_end6_left.mcfunction), [lbcr/function/raycast_preloop10cd_upgraded_mode_instant_loop_pre.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_preloop10cd_upgraded_mode_instant_loop_pre.mcfunction), [lbcr/function/raycast_woodifier_loop.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_woodifier_loop.mcfunction).

## predicate `lbc:chance50`

Источник: [lbc:chance50](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/predicate/chance50.json).
Успешно только при: случайная проверка с вероятностью `0.5`.
Буквальные ссылки на ID: [lbc/function/drops/bean.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/drops/bean.mcfunction), [lbc/function/items/swords/fevgalea_star/star_rain_tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/fevgalea_star/star_rain_tick.mcfunction), [lbc/function/items/wands/sauvojen_wand/meditate/phase_acid_tick_baller.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/meditate/phase_acid_tick_baller.mcfunction), [lbc/function/items/wands/sauvojen_wand/meditate/tick_ok.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/meditate/tick_ok.mcfunction), [lbc/function/markers_2t.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/markers_2t.mcfunction), [lbc/function/other/attributes/random_attrs_down.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/attributes/random_attrs_down.mcfunction), [lbc/function/other/eatables/banana.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/banana.mcfunction), [lbc/function/other/event/halloween/soulcorruptwand/metasola/5_tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulcorruptwand/metasola/5_tick.mcfunction), [lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/tick_true.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/grimoire_dungeon/mobs/grimoire_boss/tick_true.mcfunction), [lbc/function/other/imba/most_tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/most_tick.mcfunction), [lbc/function/other/imba/most_tick_random_double_most_create_loop.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/most_tick_random_double_most_create_loop.mcfunction), [lbc/function/other/nexus/rays/sim_cube_t.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/rays/sim_cube_t.mcfunction), [lbc/function/other/stalin_shoot_pre.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/stalin_shoot_pre.mcfunction), [lbc/function/other/transformator.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/transformator.mcfunction), [lbc/function/other/twitching.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/twitching.mcfunction), [lbc/function/players_2s.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/players_2s.mcfunction), [lbc/function/projectiles/arrows/evoker_rand.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/arrows/evoker_rand.mcfunction), [lbc/function/projectiles/pre.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/pre.mcfunction), [lbc/function/raycasters_1s.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/raycasters_1s.mcfunction), [lbc/function/raycasters_5t.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/raycasters_5t.mcfunction), [lbc/function/raycastselector.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/raycastselector.mcfunction), [lbc/function/swrg_kit_integration/game/skills/expansion_tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/expansion_tick.mcfunction), [lbc/function/swrg_kit_integration/game/skills/pulemet_shoot.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/pulemet_shoot.mcfunction), [lbc/function/swrg_kit_integration/game/skills/skills_tick_1s.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/skills_tick_1s.mcfunction), [lbc/function/swrg_kit_integration/game/skills/stone_golem/damaged.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/stone_golem/damaged.mcfunction), [lbc/function/tick1salways.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/tick1salways.mcfunction), [lbc/function/workingitems/awaken_tuntija_wands/erecto/click_upgraded_2_mode_sniper_charge.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/awaken_tuntija_wands/erecto/click_upgraded_2_mode_sniper_charge.mcfunction), [lbc/function/workingitems/awaken_tuntija_wands/erecto/click_upgraded_2_mode_sniper_charge_bruh.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/awaken_tuntija_wands/erecto/click_upgraded_2_mode_sniper_charge_bruh.mcfunction), [lbc/function/workingitems/clock_fragment/jump_1s.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/clock_fragment/jump_1s.mcfunction), [lbc/function/workingitems/clock_fragment/jump_2s.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/clock_fragment/jump_2s.mcfunction), [lbc/function/workingitems/clock_fragment/jump_5s.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/clock_fragment/jump_5s.mcfunction), [lbc/function/workingitems/gaycaster/meditate/tick_5t_ok.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/gaycaster/meditate/tick_5t_ok.mcfunction), [lbcr/function/raycast_end8_mob_check.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_end8_mob_check.mcfunction), [lbcr/function/raycast_loop7_prikol.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_loop7_prikol.mcfunction).

## predicate `lbc:chance6`

Источник: [lbc:chance6](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/predicate/chance6.json).
Успешно только при: случайная проверка с вероятностью `0.06`.
Буквальные ссылки на ID: [lbc/function/workingitems/clock_fragment/jump_2s.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/clock_fragment/jump_2s.mcfunction), [lbc/function/workingitems/witch_gens/snake_tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/witch_gens/snake_tick.mcfunction).

## predicate `lbc:chance60`

Источник: [lbc:chance60](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/predicate/chance60.json).
Успешно только при: случайная проверка с вероятностью `0.6`.
Буквальные ссылки на ID: [lbc/function/other/craft/crafted_electroshield_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/crafted_electroshield_inv.mcfunction), [lbc/function/other/craft/crafted_electrosword_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/crafted_electrosword_inv.mcfunction), [lbc/function/other/craft/crafted_railgun_inv.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/crafted_railgun_inv.mcfunction).

## predicate `lbc:chance7`

Источник: [lbc:chance7](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/predicate/chance7.json).
Успешно только при: случайная проверка с вероятностью `0.07`.
Буквальные ссылки на ID: [lbc/function/workingitems/clock_fragment/jump_1s.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/clock_fragment/jump_1s.mcfunction).

## predicate `lbc:chance70`

Источник: [lbc:chance70](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/predicate/chance70.json).
Успешно только при: случайная проверка с вероятностью `0.7`.
Буквальные ссылки на ID: [lbc/function/items/damaged.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/damaged.mcfunction), [lbc/function/items/wands/sauvojen_wand/meditate/tick_ok.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/meditate/tick_ok.mcfunction), [lbc/function/other/imba/dickcockgalactic.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/dickcockgalactic.mcfunction), [lbc/function/projectiles/pre.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/pre.mcfunction), [lbc/function/tick1salways.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/tick1salways.mcfunction), [lbc/function/workingitems/awaken_tuntija_wands/acidacid/tick_burst.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/awaken_tuntija_wands/acidacid/tick_burst.mcfunction), [lbc/function/workingitems/clock_fragment/jump_10s.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/clock_fragment/jump_10s.mcfunction), [lbc/function/workingitems/clock_fragment/jump_2s.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/clock_fragment/jump_2s.mcfunction).

## predicate `lbc:chance75`

Источник: [lbc:chance75](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/predicate/chance75.json).
Успешно только при: случайная проверка с вероятностью `0.75`.
Буквальные ссылки на ID: [lbc/function/swrg_kit_integration/game/skills/absolute_cinema/dtrump.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/absolute_cinema/dtrump.mcfunction).

## predicate `lbc:chance80`

Источник: [lbc:chance80](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/predicate/chance80.json).
Успешно только при: случайная проверка с вероятностью `0.8`.
Буквальные ссылки на ID: [lbc/function/other/imba/most_tick_random_double_most_create_loop.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/most_tick_random_double_most_create_loop.mcfunction), [lbc/function/other/nexus/explode_wire.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/explode_wire.mcfunction).

## predicate `lbc:chance85`

Источник: [lbc:chance85](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/predicate/chance85.json).
Успешно только при: случайная проверка с вероятностью `0.85`.
Буквальные ссылки на ID: [lbc/function/other/transformator_particle.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/transformator_particle.mcfunction).

## predicate `lbc:chance90`

Источник: [lbc:chance90](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/predicate/chance90.json).
Успешно только при: случайная проверка с вероятностью `0.9`.
Буквальные ссылки на ID: [lbc/function/items/wands/sauvojen_wand/flying_wand_tick_ruby_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/flying_wand_tick_ruby_upgraded.mcfunction), [lbc/function/other/imba/most_tick_random_colon_2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/most_tick_random_colon_2.mcfunction).

## predicate `lbc:chance95`

Источник: [lbc:chance95](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/predicate/chance95.json).
Успешно только при: случайная проверка с вероятностью `0.95`.
Буквальных ссылок на этот точный ID в исследуемых функциях/JSON не найдено; ресурс доступен при ручном или динамическом вызове.

## predicate `lbc:chance96`

Источник: [lbc:chance96](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/predicate/chance96.json).
Успешно только при: случайная проверка с вероятностью `0.96`.
Буквальные ссылки на ID: [lbc/function/other/god_hedgehog/hedgegod_eater_tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/god_hedgehog/hedgegod_eater_tick.mcfunction).

## predicate `lbc:chance98`

Источник: [lbc:chance98](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/predicate/chance98.json).
Успешно только при: случайная проверка с вероятностью `0.98`.
Буквальных ссылок на этот точный ID в исследуемых функциях/JSON не найдено; ресурс доступен при ручном или динамическом вызове.

## predicate `lbc:chance99`

Источник: [lbc:chance99](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/predicate/chance99.json).
Успешно только при: случайная проверка с вероятностью `0.99`.
Буквальных ссылок на этот точный ID в исследуемых функциях/JSON не найдено; ресурс доступен при ручном или динамическом вызове.

## predicate `lbc:chance_from_hunger`

Источник: [lbc:chance_from_hunger](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/predicate/chance_from_hunger.json).
Успешно только при: (свойства `this`: `{"effects":{"minecraft:hunger":{"amplifier":{"min":1}}}}`) И (случайная проверка с вероятностью `0.05`).
Буквальные ссылки на ID: [lbc/function/players_2t.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/players_2t.mcfunction).

## predicate `lbc:chance_from_nausea`

Источник: [lbc:chance_from_nausea](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/predicate/chance_from_nausea.json).
Успешно только при: (свойства `this`: `{"effects":{"minecraft:nausea":{"amplifier":{"min":1}}}}`) И (случайная проверка с вероятностью `0.05`).
Буквальные ссылки на ID: [lbc/function/players_2t.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/players_2t.mcfunction).

## predicate `lbc:diamond_armor`

Источник: [lbc:diamond_armor](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/predicate/diamond_armor.json).
Успешно только при: (свойства `this`: `{"nbt":"{Item:{id:\"minecraft:diamond_boots\"}}"}`) ИЛИ (свойства `this`: `{"nbt":"{Item:{id:\"minecraft:diamond_leggings\"}}"}`) ИЛИ (свойства `this`: `{"nbt":"{Item:{id:\"minecraft:diamond_chestplate\"}}"}`) ИЛИ (свойства `this`: `{"nbt":"{Item:{id:\"minecraft:diamond_helmet\"}}"}`).
Буквальных ссылок на этот точный ID в исследуемых функциях/JSON не найдено; ресурс доступен при ручном или динамическом вызове.

## predicate `lbc:imprinted`

Источник: [lbc:imprinted](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/predicate/imprinted.json).
Проверяет место выполнения с указанным смещением: `{"predicate":{"dimension":"minecraft:imprinted"}}`. Проверка не переносит игрока и не меняет блоки.
Буквальные ссылки на ID: [lbc/function/projectiles/teleport_nahui.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/teleport_nahui.mcfunction).

## predicate `lbc:is_sprinting`

Источник: [lbc:is_sprinting](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/predicate/is_sprinting.json).
Успешно только при: свойства `this`: `{"flags":{"is_sprinting":true}}`.
Буквальные ссылки на ID: [lbc/function/effects/ghosting_tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/effects/ghosting_tick.mcfunction), [lbc/function/players_tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/players_tick.mcfunction), [lbc/function/workingitems/shish_wall_wand/end.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/shish_wall_wand/end.mcfunction).

## predicate `lbc:jump`

Источник: [lbc:jump](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/predicate/jump.json).
Успешно только при: свойства `this`: `{"type_specific":{"type":"minecraft:player","input":{"jump":true}}}`.
Буквальных ссылок на этот точный ID в исследуемых функциях/JSON не найдено; ресурс доступен при ручном или динамическом вызове.

## predicate `lbc:looking_at_scare`

Источник: [lbc:looking_at_scare](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/predicate/looking_at_scare.json).
Успешно только при: свойства `this`: `{"type_specific":{"type":"minecraft:player","looking_at":{"type":"minecraft:interaction","nbt":"{Tags:[scare_picture]}"}}}`.
Буквальные ссылки на ID: [lbc/function/items/wands/sauvojen_wand/meditate/tick_blind.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/meditate/tick_blind.mcfunction).

## predicate `lbc:nexus`

Источник: [lbc:nexus](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/predicate/nexus.json).
Проверяет место выполнения с указанным смещением: `{"predicate":{"dimension":"minecraft:nexus"}}`. Проверка не переносит игрока и не меняет блоки.
Буквальные ссылки на ID: [lbc/function/other/nexus/click/nexus_compass_3.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/nexus_compass_3.mcfunction), [lbc/function/other/nexus/spawn_cube_save_coords.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/spawn_cube_save_coords.mcfunction), [lbc/function/storage_fix_rec_2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/storage_fix_rec_2.mcfunction), [lbc/function/workingitems/omegaflag.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/omegaflag.mcfunction), [lbc/function/workingitems/omegaflag_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/omegaflag_upgraded.mcfunction), [lbc/function/workingitems/supernovacore_newworld.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/supernovacore_newworld.mcfunction), [lbc/function/workingitems/supernovacore_newworld_pre.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/supernovacore_newworld_pre.mcfunction).

## predicate `lbc:on_fire`

Источник: [lbc:on_fire](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/predicate/on_fire.json).
Успешно только при: свойства `this`: `{"flags":{"is_on_fire":true}}`.
Буквальные ссылки на ID: [lbc/function/workingitems/awaken_tuntija_wands/acidacid/click2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/awaken_tuntija_wands/acidacid/click2.mcfunction), [lbc/function/workingitems/sauvojen_staffs/acid_preloop.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/sauvojen_staffs/acid_preloop.mcfunction), [lbcr/function/raycast_acid2_check_if_fire.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_acid2_check_if_fire.mcfunction), [lbcr/function/raycast_acidbig_check_if_fire.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_acidbig_check_if_fire.mcfunction), [lbcr/function/raycast_acidbig_check_if_fire_very.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_acidbig_check_if_fire_very.mcfunction), [lbcr/function/raycast_preloop11cd.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_preloop11cd.mcfunction).

## predicate `lbc:on_ground`

Источник: [lbc:on_ground](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/predicate/on_ground.json).
Успешно только при: свойства `this`: `{"flags":{"is_on_ground":true}}`.
Буквальные ссылки на ID: [lbc/function/effects/ghosting_tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/effects/ghosting_tick.mcfunction), [lbc/function/items/on_not_sprint.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/on_not_sprint.mcfunction), [lbc/function/items/on_sprint.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/on_sprint.mcfunction), [lbc/function/items_tick_act.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items_tick_act.mcfunction), [lbc/function/mobs_tick_lbc.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/mobs_tick_lbc.mcfunction), [lbc/function/players_tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/players_tick.mcfunction), [lbc/function/swrg_kit_integration/game/skills/rainbow_aura/aura_purple_tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/rainbow_aura/aura_purple_tick.mcfunction).

## predicate `lbc:overworld`

Источник: [lbc:overworld](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/predicate/overworld.json).
Проверяет место выполнения с указанным смещением: `{"predicate":{"dimension":"minecraft:overworld"}}`. Проверка не переносит игрока и не меняет блоки.
Буквальные ссылки на ID: [lbc/function/projectiles/teleport_nahui.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/teleport_nahui.mcfunction).

## predicate `lbc:reactive_boots`

Источник: [lbc:reactive_boots](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/predicate/reactive_boots.json).
Успешно только при: (свойства `this`: `{"equipment":{"feet":{"predicates":{"minecraft:custom_data":"{technomk2:1}"}}}}`) ИЛИ (свойства `this`: `{"equipment":{"feet":{"predicates":{"minecraft:custom_data":"{reactive:1}"}}}}`).
Буквальные ссылки на ID: [lbc/function/ui/get_data.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/ui/get_data.mcfunction).

## predicate `lbc:same_id`

Источник: [lbc:same_id](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/predicate/same_id.json).
Успешно только при: (условие `entity_scores` с параметрами `{"entity":"this","scores":{"lbcID2":{"min":{"type":"minecraft:score","target":{"type":"minecraft:fixed","name":"*temp"},"score":"lbcID2"},"max":{"type":"minecraft:score","target":{"type":"minecraft:fixed","name":"*temp"},"score":"lbcID2"}}}}`).
Буквальные ссылки на ID: [lbc/function/armor/mellstroy_mask/tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/armor/mellstroy_mask/tick.mcfunction), [lbc/function/items/arcanums/eyelaser1.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/arcanums/eyelaser1.mcfunction), [lbc/function/items/armor/chaoschestplate_10s.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/armor/chaoschestplate_10s.mcfunction), [lbc/function/items/misc/drone/tick_fly.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/misc/drone/tick_fly.mcfunction), [lbc/function/items/trinket_effects/hedgehog_gauntlet_spike.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/trinket_effects/hedgehog_gauntlet_spike.mcfunction), [lbc/function/items/trinket_effects/hedgehog_gauntlet_spike_damage.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/trinket_effects/hedgehog_gauntlet_spike_damage.mcfunction), [lbc/function/items/trinket_effects/sunprism_activate.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/trinket_effects/sunprism_activate.mcfunction), [lbc/function/items/wands/sauvojen_wand/flying_wand_tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/flying_wand_tick.mcfunction), [lbc/function/items/wands/sauvojen_wand/flying_wand_tick_acid_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/flying_wand_tick_acid_upgraded.mcfunction), [lbc/function/items/wands/sauvojen_wand/flying_wand_tick_ruby_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/flying_wand_tick_ruby_upgraded.mcfunction), [lbc/function/items/wands/sauvojen_wand/flying_wand_tick_stone_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/flying_wand_tick_stone_upgraded.mcfunction), [lbc/function/items/wands/sauvojen_wand/flying_wand_tick_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/flying_wand_tick_upgraded.mcfunction), [lbc/function/other/big_fireball_byid.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/big_fireball_byid.mcfunction), [lbc/function/other/event/halloween/soulcorruptwand/metasola/1_tick_1s.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulcorruptwand/metasola/1_tick_1s.mcfunction), [lbc/function/other/event/halloween/soulcorruptwand/metasola/1_tick_1s1.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulcorruptwand/metasola/1_tick_1s1.mcfunction), [lbc/function/other/event/halloween/soulcorruptwand/metasola/2_tick_1s.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulcorruptwand/metasola/2_tick_1s.mcfunction), [lbc/function/other/event/halloween/soulcorruptwand/metasola/4_tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulcorruptwand/metasola/4_tick.mcfunction), [lbc/function/other/event/halloween/soulcorruptwand/metasola/6_tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulcorruptwand/metasola/6_tick.mcfunction), [lbc/function/other/event/halloween/soulcorruptwand/metasola/tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulcorruptwand/metasola/tick.mcfunction), [lbc/function/other/event/halloween/soulhunter_mace/use_splash.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulhunter_mace/use_splash.mcfunction), [lbc/function/other/small_fireball_byid.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/small_fireball_byid.mcfunction), [lbc/function/other/wisp_book/passive_effects/air/as_player/zap.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/passive_effects/air/as_player/zap.mcfunction), [lbc/function/projectiles/gadgets/ruchka_2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/gadgets/ruchka_2.mcfunction), [lbc/function/projectiles/shulker_bullet_generate.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/shulker_bullet_generate.mcfunction), [lbc/function/swrg_kit_integration/game/skills/psychblind/doppel_tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/psychblind/doppel_tick.mcfunction), [lbc/function/swrg_kit_integration/game/skills/psychblind/doppel_tick1.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/psychblind/doppel_tick1.mcfunction), [lbc/function/swrg_kit_integration/game/skills/rainbow_aura/aura_orange_tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/rainbow_aura/aura_orange_tick.mcfunction), [lbc/function/swrg_kit_integration/game/skills/rainbow_aura/aura_orange_tick_at.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/rainbow_aura/aura_orange_tick_at.mcfunction), [lbc/function/workingitems/awaken_tuntija_wands/electroblock/tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/awaken_tuntija_wands/electroblock/tick.mcfunction), [lbc/function/workingitems/awaken_tuntija_wands/fireblaze/click2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/awaken_tuntija_wands/fireblaze/click2.mcfunction), [lbc/function/workingitems/awaken_tuntija_wands/fireblaze/click2_upgraded_mode_kiuas.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/awaken_tuntija_wands/fireblaze/click2_upgraded_mode_kiuas.mcfunction), [lbc/function/workingitems/awaken_tuntija_wands/fireblaze/click2_upgraded_mode_mega.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/awaken_tuntija_wands/fireblaze/click2_upgraded_mode_mega.mcfunction), [lbc/function/workingitems/awaken_tuntija_wands/fireblaze/click2_upgraded_mode_pillars.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/awaken_tuntija_wands/fireblaze/click2_upgraded_mode_pillars.mcfunction), [lbc/function/workingitems/awaken_tuntija_wands/fireblaze/click2_upgraded_mode_world.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/awaken_tuntija_wands/fireblaze/click2_upgraded_mode_world.mcfunction), [lbc/function/workingitems/awaken_tuntija_wands/stonekivi/loop_sun_end_nerfed_tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/awaken_tuntija_wands/stonekivi/loop_sun_end_nerfed_tick.mcfunction), [lbc/function/workingitems/firtusnadristailo/banan_impact.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/firtusnadristailo/banan_impact.mcfunction), [lbc/function/workingitems/firtusnadristailo/banan_impact_homing_det_rec.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/firtusnadristailo/banan_impact_homing_det_rec.mcfunction), [lbc/function/workingitems/siggewinne_syringe/buffs_init.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/siggewinne_syringe/buffs_init.mcfunction), [lbc/function/workingitems/siggewinne_syringe/drop.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/siggewinne_syringe/drop.mcfunction), [lbc/function/workingitems/summoner/cultists/chaos/2s.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/summoner/cultists/chaos/2s.mcfunction), [lbc/function/workingitems/summoner/cultists/chaos/get_electro.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/summoner/cultists/chaos/get_electro.mcfunction), [lbc/function/workingitems/summoner/cultists/chaos/get_evoker.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/summoner/cultists/chaos/get_evoker.mcfunction), [lbc/function/workingitems/summoner/cultists/chaos/get_fireshot.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/summoner/cultists/chaos/get_fireshot.mcfunction), [lbc/function/workingitems/summoner/cultists/chaos/get_splash.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/summoner/cultists/chaos/get_splash.mcfunction), [lbc/function/workingitems/summoner/cultists/chaos/get_stun.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/summoner/cultists/chaos/get_stun.mcfunction), [lbc/function/workingitems/summoner/cultists/chaos/get_terra.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/summoner/cultists/chaos/get_terra.mcfunction), [lbc/function/workingitems/summoner/cultists/chaos/get_tridents.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/summoner/cultists/chaos/get_tridents.mcfunction), [lbc/function/workingitems/summoner/cultists/luck_arrow_spam.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/summoner/cultists/luck_arrow_spam.mcfunction), [lbcr/function/damagewithid2_1_player.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/damagewithid2_1_player.mcfunction), [lbcr/function/raycast_airplayer.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_airplayer.mcfunction), [lbcr/function/raycast_stoneplayer.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_stoneplayer.mcfunction), [lbcr/function/raycast_teleportplayer.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_teleportplayer.mcfunction).

## predicate `lbc:same_id_mob`

Источник: [lbc:same_id_mob](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/predicate/same_id_mob.json).
Успешно только при: (условие `entity_scores` с параметрами `{"entity":"this","scores":{"lbcID":{"min":{"type":"minecraft:score","target":{"type":"minecraft:fixed","name":"*temp"},"score":"lbcID"},"max":{"type":"minecraft:score","target":{"type":"minecraft:fixed","name":"*temp"},"score":"lbcID"}}}}`).
Буквальные ссылки на ID: [lbc/function/other/event/halloween/soulcorruptwand/metasola/5_tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulcorruptwand/metasola/5_tick.mcfunction), [lbc/function/other/event/halloween/soulcorruptwand/metasola/break.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulcorruptwand/metasola/break.mcfunction), [lbc/function/other/event/halloween/soulcorruptwand/metasola/tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulcorruptwand/metasola/tick.mcfunction).

## predicate `lbc:same_team`

Источник: [lbc:same_team](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/predicate/same_team.json).
Успешно только при: (условие `entity_scores` с параметрами `{"entity":"this","scores":{"team_number":{"min":{"type":"minecraft:score","target":{"type":"minecraft:fixed","name":"*tempt"},"score":"lbcID2"},"max":{"type":"minecraft:score","target":{"type":"minecraft:fixed","name":"*tempt"},"score":"lbcID2"}}}}`).
Буквальные ссылки на ID: [lbc/function/armor/assasin_set_bonus.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/armor/assasin_set_bonus.mcfunction), [lbc/function/armor/glass/damaged_full_set_dmg.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/armor/glass/damaged_full_set_dmg.mcfunction), [lbc/function/armor/gownoleggings/1.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/armor/gownoleggings/1.mcfunction), [lbc/function/armor/gownoleggings/2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/armor/gownoleggings/2.mcfunction), [lbc/function/armor/gownoleggings/3.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/armor/gownoleggings/3.mcfunction), [lbc/function/armor/gownoleggings/4.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/armor/gownoleggings/4.mcfunction), [lbc/function/armor/gownoleggings/5.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/armor/gownoleggings/5.mcfunction), [lbc/function/armor/gownoleggings/6.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/armor/gownoleggings/6.mcfunction), [lbc/function/armor/gownoleggings/7.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/armor/gownoleggings/7.mcfunction), [lbc/function/armor/gownoleggings/8.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/armor/gownoleggings/8.mcfunction), [lbc/function/armor/gownoleggings/any.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/armor/gownoleggings/any.mcfunction), [lbc/function/armor/mellstroy_mask/tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/armor/mellstroy_mask/tick.mcfunction), [lbc/function/armor/overload/boots/impact_2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/armor/overload/boots/impact_2.mcfunction), [lbc/function/armor/phantom_set_bonus_pre.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/armor/phantom_set_bonus_pre.mcfunction), [lbc/function/items/arcanums/5true/sun_tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/arcanums/5true/sun_tick.mcfunction), [lbc/function/items/arcanums/eyelaser1.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/arcanums/eyelaser1.mcfunction), [lbc/function/items/armor/chaoschestplate_10s.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/armor/chaoschestplate_10s.mcfunction), [lbc/function/items/misc/huynya/click.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/misc/huynya/click.mcfunction), [lbc/function/items/swords.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords.mcfunction), [lbc/function/items/swords/fevgalea_star/click_charged_shift.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/fevgalea_star/click_charged_shift.mcfunction), [lbc/function/items/swords/ruchka.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/ruchka.mcfunction), [lbc/function/items/swords/wtf_item/hit.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/wtf_item/hit.mcfunction), [lbc/function/items/trinket_effects/hedgehog_gauntlet_spike.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/trinket_effects/hedgehog_gauntlet_spike.mcfunction), [lbc/function/items/trinket_effects/hedgehog_gauntlet_spike_damage.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/trinket_effects/hedgehog_gauntlet_spike_damage.mcfunction), [lbc/function/items/trinket_effects/ice_gem_1.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/trinket_effects/ice_gem_1.mcfunction), [lbc/function/items/trinket_effects/intercal_hit.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/trinket_effects/intercal_hit.mcfunction), [lbc/function/items/trinket_effects/snow_pearl_1.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/trinket_effects/snow_pearl_1.mcfunction), [lbc/function/items/trinket_effects/sunprism_activate.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/trinket_effects/sunprism_activate.mcfunction), [lbc/function/items/wands/sauvojen_wand/flying_wand_tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/flying_wand_tick.mcfunction), [lbc/function/items/wands/sauvojen_wand/flying_wand_tick_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/flying_wand_tick_upgraded.mcfunction), [lbc/function/other/artifact/colors/black.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/colors/black.mcfunction), [lbc/function/other/artifact/colors/brown.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/colors/brown.mcfunction), [lbc/function/other/artifact/colors/cyan.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/colors/cyan.mcfunction), [lbc/function/other/artifact/colors/green.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/colors/green.mcfunction), [lbc/function/other/artifact/colors/light_blue.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/colors/light_blue.mcfunction), [lbc/function/other/artifact/colors/light_gray.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/colors/light_gray.mcfunction), [lbc/function/other/artifact/colors/lime.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/colors/lime.mcfunction), [lbc/function/other/artifact/colors/magenta.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/colors/magenta.mcfunction), [lbc/function/other/artifact/colors/orange.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/colors/orange.mcfunction), [lbc/function/other/artifact/colors/pink.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/colors/pink.mcfunction), [lbc/function/other/artifact/colors/purple.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/colors/purple.mcfunction), [lbc/function/other/artifact/colors/red.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/colors/red.mcfunction), [lbc/function/other/artifact/colors/white.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/colors/white.mcfunction), [lbc/function/other/artifact/colors/yellow.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/colors/yellow.mcfunction), [lbc/function/other/artifact/highspeed.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/highspeed.mcfunction), [lbc/function/other/artifact/ondamage.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/ondamage.mcfunction), [lbc/function/other/artifact/vampirism.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/vampirism.mcfunction), [lbc/function/other/baffledwall/tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/baffledwall/tick.mcfunction), [lbc/function/other/big_fireball_byid.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/big_fireball_byid.mcfunction), [lbc/function/other/chaoschestplate/acid.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/acid.mcfunction), [lbc/function/other/chaoschestplate/bounce.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/bounce.mcfunction), [lbc/function/other/chaoschestplate/damage.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/damage.mcfunction), [lbc/function/other/chaoschestplate/flash.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/flash.mcfunction), [lbc/function/other/chaoschestplate/perdesh.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/perdesh.mcfunction), [lbc/function/other/chaoschestplate/ponos.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/ponos.mcfunction), [lbc/function/other/chaoschestplate/slow.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/slow.mcfunction), [lbc/function/other/chaoschestplate/teleportnazad.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/teleportnazad.mcfunction), [lbc/function/other/chaoschestplate/transfusion.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/transfusion.mcfunction), [lbc/function/other/chaoschestplate/weakall.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/weakall.mcfunction), [lbc/function/other/deviltotem0.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/deviltotem0.mcfunction), [lbc/function/other/event/halloween/soulcorruptwand/metasola/1_tick_1s.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulcorruptwand/metasola/1_tick_1s.mcfunction), [lbc/function/other/event/halloween/soulcorruptwand/metasola/2_tick_1s.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulcorruptwand/metasola/2_tick_1s.mcfunction), [lbc/function/other/event/halloween/soulcorruptwand/metasola/4_tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulcorruptwand/metasola/4_tick.mcfunction), [lbc/function/other/event/halloween/soulcorruptwand/metasola/6_tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulcorruptwand/metasola/6_tick.mcfunction), [lbc/function/other/event/halloween/soulhunter_mace/use_splash.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulhunter_mace/use_splash.mcfunction), [lbc/function/other/gownoleggings.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/gownoleggings.mcfunction), [lbc/function/other/imba/blood_slab/tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/blood_slab/tick.mcfunction), [lbc/function/other/imba/cunny_slab/cunnys/1.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/cunnys/1.mcfunction), [lbc/function/other/imba/cunny_slab/cunnys/6.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/cunny_slab/cunnys/6.mcfunction), [lbc/function/other/imba/runes/thurisaz_cast.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/imba/runes/thurisaz_cast.mcfunction), [lbc/function/other/small_fireball_byid.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/small_fireball_byid.mcfunction), [lbc/function/other/techno/cyborg_boss/wyvern.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/cyborg_boss/wyvern.mcfunction), [lbc/function/projectiles/gadgets/ruchka_2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/gadgets/ruchka_2.mcfunction), [lbc/function/swrg_kit_integration/game/skills/absolute_cinema/akyla_devid_tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/absolute_cinema/akyla_devid_tick.mcfunction), [lbc/function/swrg_kit_integration/game/skills/absolute_cinema/bitcoin_tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/absolute_cinema/bitcoin_tick.mcfunction), [lbc/function/swrg_kit_integration/game/skills/psychblind/doppel_tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/psychblind/doppel_tick.mcfunction), [lbc/function/swrg_kit_integration/game/skills/rainbow_aura/aura_orange_tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/rainbow_aura/aura_orange_tick.mcfunction), [lbc/function/workingitems/awaken_tuntija_wands/electroblock/tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/awaken_tuntija_wands/electroblock/tick.mcfunction), [lbc/function/workingitems/awaken_tuntija_wands/fireblaze/click2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/awaken_tuntija_wands/fireblaze/click2.mcfunction), [lbc/function/workingitems/awaken_tuntija_wands/fireblaze/click2_upgraded_mode_kiuas.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/awaken_tuntija_wands/fireblaze/click2_upgraded_mode_kiuas.mcfunction), [lbc/function/workingitems/awaken_tuntija_wands/fireblaze/click2_upgraded_mode_mega.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/awaken_tuntija_wands/fireblaze/click2_upgraded_mode_mega.mcfunction), [lbc/function/workingitems/awaken_tuntija_wands/fireblaze/click2_upgraded_mode_pillars.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/awaken_tuntija_wands/fireblaze/click2_upgraded_mode_pillars.mcfunction), [lbc/function/workingitems/awaken_tuntija_wands/fireblaze/click2_upgraded_mode_world.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/awaken_tuntija_wands/fireblaze/click2_upgraded_mode_world.mcfunction), [lbc/function/workingitems/basscast.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/basscast.mcfunction), [lbc/function/workingitems/basscast_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/basscast_upgraded.mcfunction), [lbc/function/workingitems/basscast_upgraded_spelllib.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/basscast_upgraded_spelllib.mcfunction), [lbc/function/workingitems/bloodmagic.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/bloodmagic.mcfunction), [lbc/function/workingitems/bloodmagic_old.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/bloodmagic_old.mcfunction), [lbc/function/workingitems/bloodmagic_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/bloodmagic_upgraded.mcfunction), [lbc/function/workingitems/coolsword_old.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/coolsword_old.mcfunction), [lbc/function/workingitems/corrupter.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/corrupter.mcfunction), [lbc/function/workingitems/corrupter_old.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/corrupter_old.mcfunction), [lbc/function/workingitems/corrupter_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/corrupter_upgraded.mcfunction), [lbc/function/workingitems/corrupter_upgraded_spelllib.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/corrupter_upgraded_spelllib.mcfunction), [lbc/function/workingitems/cyberkatana.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/cyberkatana.mcfunction), [lbc/function/workingitems/cyberkatanaability.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/cyberkatanaability.mcfunction), [lbc/function/workingitems/epicswordattacks/bounce.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/epicswordattacks/bounce.mcfunction), [lbc/function/workingitems/epicswordattacks/flash.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/epicswordattacks/flash.mcfunction), [lbc/function/workingitems/epicswordattacks/instantdamage.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/epicswordattacks/instantdamage.mcfunction), [lbc/function/workingitems/epicswordattacks/randomtp.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/epicswordattacks/randomtp.mcfunction), [lbc/function/workingitems/epicswordattacks/screamer.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/epicswordattacks/screamer.mcfunction), [lbc/function/workingitems/epicswordattacks/weakness.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/epicswordattacks/weakness.mcfunction), [lbc/function/workingitems/firtusnadristailo/banan_impact.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/firtusnadristailo/banan_impact.mcfunction), [lbc/function/workingitems/firtusnadristailo/banan_impact_homing_det_rec.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/firtusnadristailo/banan_impact_homing_det_rec.mcfunction), [lbc/function/workingitems/foxuscast.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/foxuscast.mcfunction), [lbc/function/workingitems/foxuscast_old.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/foxuscast_old.mcfunction), [lbc/function/workingitems/foxuscast_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/foxuscast_upgraded.mcfunction), [lbc/function/workingitems/foxuscast_upgraded_lib.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/foxuscast_upgraded_lib.mcfunction), [lbc/function/workingitems/fragmentsword_old.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/fragmentsword_old.mcfunction), [lbc/function/workingitems/gaara1_2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/gaara1_2.mcfunction), [lbc/function/workingitems/gaara1_3.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/gaara1_3.mcfunction), [lbc/function/workingitems/gaara1_32.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/gaara1_32.mcfunction), [lbc/function/workingitems/gravitationsword.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/gravitationsword.mcfunction), [lbc/function/workingitems/icesword.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/icesword.mcfunction), [lbc/function/workingitems/lifestealer.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/lifestealer.mcfunction), [lbc/function/workingitems/lifestealer_old.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/lifestealer_old.mcfunction), [lbc/function/workingitems/lifestealer_upgraded_mode_1.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/lifestealer_upgraded_mode_1.mcfunction), [lbc/function/workingitems/phantom.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/phantom.mcfunction), [lbc/function/workingitems/psychowand.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/psychowand.mcfunction), [lbc/function/workingitems/psychowand_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/psychowand_upgraded.mcfunction), [lbc/function/workingitems/psychowand_upgraded_lib.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/psychowand_upgraded_lib.mcfunction), [lbc/function/workingitems/seaguardian_1.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/seaguardian_1.mcfunction), [lbc/function/workingitems/seaguardian_2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/seaguardian_2.mcfunction), [lbc/function/workingitems/shulkershell.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/shulkershell.mcfunction), [lbc/function/workingitems/snow_wand/summon.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/snow_wand/summon.mcfunction), [lbc/function/workingitems/summoner/cultists/chaos/2s.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/summoner/cultists/chaos/2s.mcfunction), [lbc/function/workingitems/summoner/cultists/chaos/get_electro.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/summoner/cultists/chaos/get_electro.mcfunction), [lbc/function/workingitems/summoner/cultists/chaos/get_evoker.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/summoner/cultists/chaos/get_evoker.mcfunction), [lbc/function/workingitems/summoner/cultists/chaos/get_fireshot.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/summoner/cultists/chaos/get_fireshot.mcfunction), [lbc/function/workingitems/summoner/cultists/chaos/get_splash.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/summoner/cultists/chaos/get_splash.mcfunction), [lbc/function/workingitems/summoner/cultists/chaos/get_stun.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/summoner/cultists/chaos/get_stun.mcfunction), [lbc/function/workingitems/summoner/cultists/chaos/get_terra.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/summoner/cultists/chaos/get_terra.mcfunction), [lbc/function/workingitems/summoner/cultists/chaos/get_tridents.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/summoner/cultists/chaos/get_tridents.mcfunction), [lbc/function/workingitems/summoner/cultists/luck_arrow_spam.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/summoner/cultists/luck_arrow_spam.mcfunction), [lbc/function/workingitems/witch_gens/lust_damage.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/witch_gens/lust_damage.mcfunction), [lbc/function/workingitems/witch_gens/lust_flesh_sneak_ground.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/witch_gens/lust_flesh_sneak_ground.mcfunction), [lbc/function/workingitems/witch_gens/whale_fog_rc.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/witch_gens/whale_fog_rc.mcfunction), [lbc/function/workingitems/witch_gens/whale_fog_rc_explode.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/witch_gens/whale_fog_rc_explode.mcfunction), [lbc/function/workingitems/wyvern.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/wyvern.mcfunction), [lbcr/function/damagewithid2_1_player.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/damagewithid2_1_player.mcfunction), [lbcr/function/railgun_lol.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/railgun_lol.mcfunction), [lbcr/function/raycast_airplayer.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_airplayer.mcfunction), [lbcr/function/raycast_loop14.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_loop14.mcfunction), [lbcr/function/raycast_stoneplayer.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_stoneplayer.mcfunction), [lbcr/function/raycast_teleportplayer.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_teleportplayer.mcfunction).

## predicate `lbc:see_sky`

Источник: [lbc:see_sky](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/predicate/see_sky.json).
Успешно только при: свойства `this`: `{"location":{"can_see_sky":true}}`.
Буквальные ссылки на ID: [lbc/function/items/trinket_effects/sunprism_activate.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/trinket_effects/sunprism_activate.mcfunction).

## predicate `lbc:smallhealthinfire`

Источник: [lbc:smallhealthinfire](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/predicate/smallhealthinfire.json).
Успешно только при: свойства `this`: `{"nbt":"{Health:1.0f}","flags":{"is_on_fire":true}}`.
Буквальных ссылок на этот точный ID в исследуемых функциях/JSON не найдено; ресурс доступен при ручном или динамическом вызове.

## predicate `lbc:sneak`

Источник: [lbc:sneak](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/predicate/sneak.json).
Успешно только при: свойства `this`: `{"type_specific":{"type":"minecraft:player","input":{"sneak":true}}}`.
Буквальные ссылки на ID: [lbc/function/armor/technomk2/boots/tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/armor/technomk2/boots/tick.mcfunction), [lbc/function/items/arcanums/barrier_tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/arcanums/barrier_tick.mcfunction), [lbc/function/items/armor.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/armor.mcfunction), [lbc/function/items/shooter/slr/click.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/shooter/slr/click.mcfunction), [lbc/function/items/swords/fevgalea_star/click_select.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/swords/fevgalea_star/click_select.mcfunction), [lbc/function/items/wands/danmaku_staff/click_2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/danmaku_staff/click_2.mcfunction), [lbc/function/items/wands/sauvojen_wand/cast.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/wands/sauvojen_wand/cast.mcfunction), [lbc/function/other/event/halloween/soulcorruptwand/cast.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulcorruptwand/cast.mcfunction), [lbc/function/other/event/halloween/soulcorruptwand/cast_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulcorruptwand/cast_upgraded.mcfunction), [lbc/function/other/event/halloween/soulcorruptwand/cast_upgraded_left.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulcorruptwand/cast_upgraded_left.mcfunction), [lbc/function/other/event/halloween/soulhunter_mace/use.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/soulhunter_mace/use.mcfunction), [lbc/function/other/event/halloween/vex_wand/cast.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/vex_wand/cast.mcfunction), [lbc/function/other/event/halloween/vex_wand/cast_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/vex_wand/cast_upgraded.mcfunction), [lbc/function/other/event/halloween/vex_wand/cast_upgraded_left.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/event/halloween/vex_wand/cast_upgraded_left.mcfunction), [lbc/function/other/nexus/click/orb_gun/pre.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/click/orb_gun/pre.mcfunction), [lbc/function/other/nexus/rays/roadgun_2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nexus/rays/roadgun_2.mcfunction), [lbc/function/other/reactive_boots.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/reactive_boots.mcfunction), [lbc/function/other/techno/magnum/check_shift.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/check_shift.mcfunction), [lbc/function/other/techno/magnum/check_shift_left.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/techno/magnum/check_shift_left.mcfunction), [lbc/function/players_tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/players_tick.mcfunction), [lbc/function/swrg_kit_integration/game/skills/expansion_tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/expansion_tick.mcfunction), [lbc/function/swrg_kit_integration/game/skills/god_claw_tick.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/god_claw_tick.mcfunction), [lbc/function/swrg_kit_integration/items/steampunk_sword.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/items/steampunk_sword.mcfunction), [lbc/function/workingitems/awaken_tuntija_wands/acidacid/click_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/awaken_tuntija_wands/acidacid/click_upgraded.mcfunction), [lbc/function/workingitems/awaken_tuntija_wands/acidacid/click_upgraded_left.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/awaken_tuntija_wands/acidacid/click_upgraded_left.mcfunction), [lbc/function/workingitems/awaken_tuntija_wands/acidcorrupt/click.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/awaken_tuntija_wands/acidcorrupt/click.mcfunction), [lbc/function/workingitems/awaken_tuntija_wands/acidcorrupt/click_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/awaken_tuntija_wands/acidcorrupt/click_upgraded.mcfunction), [lbc/function/workingitems/awaken_tuntija_wands/acidcorrupt/click_upgraded_left.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/awaken_tuntija_wands/acidcorrupt/click_upgraded_left.mcfunction), [lbc/function/workingitems/awaken_tuntija_wands/avarice/click2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/awaken_tuntija_wands/avarice/click2.mcfunction), [lbc/function/workingitems/awaken_tuntija_wands/avarice/click_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/awaken_tuntija_wands/avarice/click_upgraded.mcfunction), [lbc/function/workingitems/awaken_tuntija_wands/avarice/click_upgraded_left.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/awaken_tuntija_wands/avarice/click_upgraded_left.mcfunction), [lbc/function/workingitems/awaken_tuntija_wands/bedrock/click.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/awaken_tuntija_wands/bedrock/click.mcfunction), [lbc/function/workingitems/awaken_tuntija_wands/bedrock/click_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/awaken_tuntija_wands/bedrock/click_upgraded.mcfunction), [lbc/function/workingitems/awaken_tuntija_wands/bedrock/click_upgraded_left.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/awaken_tuntija_wands/bedrock/click_upgraded_left.mcfunction), [lbc/function/workingitems/awaken_tuntija_wands/boltbolt/click_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/awaken_tuntija_wands/boltbolt/click_upgraded.mcfunction), [lbc/function/workingitems/awaken_tuntija_wands/boltbolt/click_upgraded_left.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/awaken_tuntija_wands/boltbolt/click_upgraded_left.mcfunction), [lbc/function/workingitems/awaken_tuntija_wands/consuming_wand.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/awaken_tuntija_wands/consuming_wand.mcfunction), [lbc/function/workingitems/awaken_tuntija_wands/consuming_wand_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/awaken_tuntija_wands/consuming_wand_upgraded.mcfunction), [lbc/function/workingitems/awaken_tuntija_wands/consuming_wand_upgraded_left.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/awaken_tuntija_wands/consuming_wand_upgraded_left.mcfunction), [lbc/function/workingitems/awaken_tuntija_wands/erecto/click_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/awaken_tuntija_wands/erecto/click_upgraded.mcfunction), [lbc/function/workingitems/awaken_tuntija_wands/erecto/click_upgraded_left.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/awaken_tuntija_wands/erecto/click_upgraded_left.mcfunction), [lbc/function/workingitems/awaken_tuntija_wands/exahoming_click.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/awaken_tuntija_wands/exahoming_click.mcfunction), [lbc/function/workingitems/awaken_tuntija_wands/exahoming_click_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/awaken_tuntija_wands/exahoming_click_upgraded.mcfunction), [lbc/function/workingitems/awaken_tuntija_wands/exahoming_click_upgraded_left.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/awaken_tuntija_wands/exahoming_click_upgraded_left.mcfunction), [lbc/function/workingitems/awaken_tuntija_wands/exasperato/click_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/awaken_tuntija_wands/exasperato/click_upgraded.mcfunction), [lbc/function/workingitems/awaken_tuntija_wands/exasperato/click_upgraded_left.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/awaken_tuntija_wands/exasperato/click_upgraded_left.mcfunction), [lbc/function/workingitems/awaken_tuntija_wands/fireblaze/click2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/awaken_tuntija_wands/fireblaze/click2.mcfunction), [lbc/function/workingitems/awaken_tuntija_wands/fireblaze/click_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/awaken_tuntija_wands/fireblaze/click_upgraded.mcfunction), [lbc/function/workingitems/awaken_tuntija_wands/fireblaze/click_upgraded_left.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/awaken_tuntija_wands/fireblaze/click_upgraded_left.mcfunction), [lbc/function/workingitems/awaken_tuntija_wands/psychoblind/click2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/awaken_tuntija_wands/psychoblind/click2.mcfunction), [lbc/function/workingitems/awaken_tuntija_wands/psychoblind/click_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/awaken_tuntija_wands/psychoblind/click_upgraded.mcfunction), [lbc/function/workingitems/awaken_tuntija_wands/psychoblind/click_upgraded_left.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/awaken_tuntija_wands/psychoblind/click_upgraded_left.mcfunction), [lbc/function/workingitems/awaken_tuntija_wands/stonekivi/click_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/awaken_tuntija_wands/stonekivi/click_upgraded.mcfunction), [lbc/function/workingitems/awaken_tuntija_wands/stonekivi/click_upgraded_left.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/awaken_tuntija_wands/stonekivi/click_upgraded_left.mcfunction), [lbc/function/workingitems/badwiki_upgraded_pre.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/badwiki_upgraded_pre.mcfunction), [lbc/function/workingitems/badwiki_upgraded_pre_left.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/badwiki_upgraded_pre_left.mcfunction), [lbc/function/workingitems/basscast_upgraded_pre.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/basscast_upgraded_pre.mcfunction), [lbc/function/workingitems/basscast_upgraded_pre_left.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/basscast_upgraded_pre_left.mcfunction), [lbc/function/workingitems/bloodmagic.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/bloodmagic.mcfunction), [lbc/function/workingitems/corrupter_upgraded_pre.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/corrupter_upgraded_pre.mcfunction), [lbc/function/workingitems/corrupter_upgraded_pre_left.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/corrupter_upgraded_pre_left.mcfunction), [lbc/function/workingitems/evokercast_upgraded_pre.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/evokercast_upgraded_pre.mcfunction), [lbc/function/workingitems/evokercast_upgraded_pre_left.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/evokercast_upgraded_pre_left.mcfunction), [lbc/function/workingitems/foxuscast_upgraded_pre.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/foxuscast_upgraded_pre.mcfunction), [lbc/function/workingitems/foxuscast_upgraded_pre_left.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/foxuscast_upgraded_pre_left.mcfunction), [lbc/function/workingitems/gaycaster/ray/cast.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/gaycaster/ray/cast.mcfunction), [lbc/function/workingitems/gaycaster/ray/cast_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/gaycaster/ray/cast_upgraded.mcfunction), [lbc/function/workingitems/gaycaster/ray/cast_upgraded_left.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/gaycaster/ray/cast_upgraded_left.mcfunction), [lbc/function/workingitems/lifestealer_upgraded_pre.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/lifestealer_upgraded_pre.mcfunction), [lbc/function/workingitems/lifestealer_upgraded_pre_left.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/lifestealer_upgraded_pre_left.mcfunction), [lbc/function/workingitems/neutralmage/select_upgraded_pre.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/neutralmage/select_upgraded_pre.mcfunction), [lbc/function/workingitems/neutralmage/select_upgraded_pre_left.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/neutralmage/select_upgraded_pre_left.mcfunction), [lbc/function/workingitems/psychowand_upgraded_pre.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/psychowand_upgraded_pre.mcfunction), [lbc/function/workingitems/psychowand_upgraded_pre_left.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/psychowand_upgraded_pre_left.mcfunction), [lbc/function/workingitems/random_spell_upgraded_pre.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/random_spell_upgraded_pre.mcfunction), [lbc/function/workingitems/random_spell_upgraded_pre_left.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/random_spell_upgraded_pre_left.mcfunction), [lbc/function/workingitems/sauvojen_staffs/acid_upgraded_pre.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/sauvojen_staffs/acid_upgraded_pre.mcfunction), [lbc/function/workingitems/sauvojen_staffs/acid_upgraded_pre_left.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/sauvojen_staffs/acid_upgraded_pre_left.mcfunction), [lbc/function/workingitems/sauvojen_staffs/air_upgraded_pre.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/sauvojen_staffs/air_upgraded_pre.mcfunction), [lbc/function/workingitems/sauvojen_staffs/air_upgraded_pre_left.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/sauvojen_staffs/air_upgraded_pre_left.mcfunction), [lbc/function/workingitems/sauvojen_staffs/blind_upgraded_pre.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/sauvojen_staffs/blind_upgraded_pre.mcfunction), [lbc/function/workingitems/sauvojen_staffs/blind_upgraded_pre_left.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/sauvojen_staffs/blind_upgraded_pre_left.mcfunction), [lbc/function/workingitems/sauvojen_staffs/electro_preloop.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/sauvojen_staffs/electro_preloop.mcfunction), [lbc/function/workingitems/sauvojen_staffs/electro_upgraded_pre.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/sauvojen_staffs/electro_upgraded_pre.mcfunction), [lbc/function/workingitems/sauvojen_staffs/electro_upgraded_pre_left.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/sauvojen_staffs/electro_upgraded_pre_left.mcfunction), [lbc/function/workingitems/sauvojen_staffs/fire_preloop.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/sauvojen_staffs/fire_preloop.mcfunction), [lbc/function/workingitems/sauvojen_staffs/fire_upgraded_pre.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/sauvojen_staffs/fire_upgraded_pre.mcfunction), [lbc/function/workingitems/sauvojen_staffs/fire_upgraded_pre_left.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/sauvojen_staffs/fire_upgraded_pre_left.mcfunction), [lbc/function/workingitems/sauvojen_staffs/ruby_preloop.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/sauvojen_staffs/ruby_preloop.mcfunction), [lbc/function/workingitems/sauvojen_staffs/ruby_upgraded_pre.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/sauvojen_staffs/ruby_upgraded_pre.mcfunction), [lbc/function/workingitems/sauvojen_staffs/ruby_upgraded_pre_left.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/sauvojen_staffs/ruby_upgraded_pre_left.mcfunction), [lbc/function/workingitems/sauvojen_staffs/stone_preloop.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/sauvojen_staffs/stone_preloop.mcfunction), [lbc/function/workingitems/sauvojen_staffs/stone_upgraded_pre.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/sauvojen_staffs/stone_upgraded_pre.mcfunction), [lbc/function/workingitems/sauvojen_staffs/stone_upgraded_pre_left.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/sauvojen_staffs/stone_upgraded_pre_left.mcfunction), [lbc/function/workingitems/sauvojen_staffs/teleport_preloop.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/sauvojen_staffs/teleport_preloop.mcfunction), [lbc/function/workingitems/sauvojen_staffs/teleport_upgraded_pre.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/sauvojen_staffs/teleport_upgraded_pre.mcfunction), [lbc/function/workingitems/sauvojen_staffs/teleport_upgraded_pre_left.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/sauvojen_staffs/teleport_upgraded_pre_left.mcfunction), [lbc/function/workingitems/sauvojen_staffs/terra.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/sauvojen_staffs/terra.mcfunction), [lbc/function/workingitems/shish_wall_wand/end.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/shish_wall_wand/end.mcfunction), [lbc/function/workingitems/spell1_default.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/spell1_default.mcfunction), [lbc/function/workingitems/spell1_default_left.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/spell1_default_left.mcfunction), [lbc/function/workingitems/spell1_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/spell1_upgraded.mcfunction), [lbc/function/workingitems/spell1_upgraded_left.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/spell1_upgraded_left.mcfunction), [lbc/function/workingitems/spellsummoner.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/spellsummoner.mcfunction), [lbc/function/workingitems/spellsummoner_left.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/spellsummoner_left.mcfunction), [lbc/function/workingitems/witch_gens/activate.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/witch_gens/activate.mcfunction), [lbc/function/workingitems/witch_gens/indolence_hand.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/witch_gens/indolence_hand.mcfunction), [lbc/function/workingitems/witch_gens/lust_dragon_5t.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/witch_gens/lust_dragon_5t.mcfunction), [lbc/function/workingitems/witch_gens/lust_flesh.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/witch_gens/lust_flesh.mcfunction), [lbcr/function/raycast_lightningpre_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_lightningpre_upgraded.mcfunction), [lbcr/function/raycast_lightningpre_upgraded_left.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_lightningpre_upgraded_left.mcfunction), [lbcr/function/raycast_loop2cd_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_loop2cd_upgraded.mcfunction), [lbcr/function/raycast_loop2cd_upgraded_left.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_loop2cd_upgraded_left.mcfunction), [lbcr/function/raycast_loop3cd_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_loop3cd_upgraded.mcfunction), [lbcr/function/raycast_loop3cd_upgraded_left.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_loop3cd_upgraded_left.mcfunction), [lbcr/function/raycast_loop4cd.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_loop4cd.mcfunction), [lbcr/function/raycast_loop4cd_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_loop4cd_upgraded.mcfunction), [lbcr/function/raycast_loop4cd_upgraded_left.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_loop4cd_upgraded_left.mcfunction), [lbcr/function/raycast_loop5cd.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_loop5cd.mcfunction), [lbcr/function/raycast_loop5cd_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_loop5cd_upgraded.mcfunction), [lbcr/function/raycast_loop5cd_upgraded_left.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_loop5cd_upgraded_left.mcfunction), [lbcr/function/raycast_loop9cd.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_loop9cd.mcfunction), [lbcr/function/raycast_loop9cd_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_loop9cd_upgraded.mcfunction), [lbcr/function/raycast_loop9cd_upgraded_left.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_loop9cd_upgraded_left.mcfunction), [lbcr/function/raycast_preloop10cd_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_preloop10cd_upgraded.mcfunction), [lbcr/function/raycast_preloop10cd_upgraded_left.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_preloop10cd_upgraded_left.mcfunction), [lbcr/function/raycast_preloop11cd_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_preloop11cd_upgraded.mcfunction), [lbcr/function/raycast_preloop11cd_upgraded_left.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_preloop11cd_upgraded_left.mcfunction).

## predicate `lbc:swimming`

Источник: [lbc:swimming](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/predicate/swimming.json).
Успешно только при: свойства `this`: `{"flags":{"is_swimming":true}}`.
Буквальные ссылки на ID: [lbc/function/workingitems/seaguardian.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/seaguardian.mcfunction).

## predicate `lbc:the_end`

Источник: [lbc:the_end](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/predicate/the_end.json).
Проверяет место выполнения с указанным смещением: `{"predicate":{"dimension":"minecraft:the_end"}}`. Проверка не переносит игрока и не меняет блоки.
Буквальные ссылки на ID: [lbc/function/players_10s.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/players_10s.mcfunction), [lbc/function/projectiles/teleport_nahui.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/teleport_nahui.mcfunction).

## predicate `lbc:the_nether`

Источник: [lbc:the_nether](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/predicate/the_nether.json).
Проверяет место выполнения с указанным смещением: `{"predicate":{"dimension":"minecraft:the_nether"}}`. Проверка не переносит игрока и не меняет блоки.
Буквальные ссылки на ID: [lbc/function/armor/chaos_set_bonus.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/armor/chaos_set_bonus.mcfunction), [lbc/function/armor/shulker_teleport.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/armor/shulker_teleport.mcfunction), [lbc/function/drops/forest_loop.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/drops/forest_loop.mcfunction), [lbc/function/drops/portall.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/drops/portall.mcfunction), [lbc/function/effects/effect_rtp_4.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/effects/effect_rtp_4.mcfunction), [lbc/function/effects/rtp_1.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/effects/rtp_1.mcfunction), [lbc/function/effects/rtp_2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/effects/rtp_2.mcfunction), [lbc/function/effects/rtp_3.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/effects/rtp_3.mcfunction), [lbc/function/items/armor.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/armor.mcfunction), [lbc/function/items/trinket_effects/rtp_gas1.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/trinket_effects/rtp_gas1.mcfunction), [lbc/function/items/trinket_effects/rtp_gas2.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/trinket_effects/rtp_gas2.mcfunction), [lbc/function/items/trinket_effects/rtp_gas21.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/trinket_effects/rtp_gas21.mcfunction), [lbc/function/other/artifact/colors/purple.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/artifact/colors/purple.mcfunction), [lbc/function/other/chaoschestplate/phase.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/phase.mcfunction), [lbc/function/other/chaoschestplate/teleport.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/chaoschestplate/teleport.mcfunction), [lbc/function/other/death_compass_attack.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/death_compass_attack.mcfunction), [lbc/function/other/troll_box/trollge_spawn.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/troll_box/trollge_spawn.mcfunction), [lbc/function/players_10t.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/players_10t.mcfunction), [lbc/function/projectiles/teleport_nahui.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/projectiles/teleport_nahui.mcfunction), [lbc/function/workingitems/endcast.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/endcast.mcfunction), [lbc/function/workingitems/endcast_lib.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/endcast_lib.mcfunction), [lbc/function/workingitems/endersword.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/endersword.mcfunction), [lbc/function/workingitems/epicswordattacks/randomtp.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/epicswordattacks/randomtp.mcfunction), [lbc/function/workingitems/foxuscast.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/foxuscast.mcfunction), [lbc/function/workingitems/foxuscast_upgraded.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/foxuscast_upgraded.mcfunction), [lbc/function/workingitems/foxuscast_upgraded_lib.mcfunction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/foxuscast_upgraded_lib.mcfunction).

