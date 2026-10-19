# Ice Sword: migrated source payload

Источник предмета: read-only `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/transformables_sword.json`, entry с `custom_data` `{transformable:1,sword:1,icesword:1}`. Исходный item — `minecraft:iron_sword`; native loot payload сохраняет count 1, `lbc:tier_c`, модель `lbc:icesword`, имя и lore, меняя только item id на `skywars2:ice_sword`.

Исходные компоненты: имя `ice_sword` (italic=false); lore из трёх компонентов/строк — aqua `type` + `sword`, затем белые `ice_sword.lore.1` и `ice_sword.lore.2`, каждая italic=false. Marker custom data сохраняется без изменений.

Проверка read-only resource pack `all_in_one_1.21.8`: `assets/minecraft/lang/en_us.json` и `ru_ru.json` уже содержат все три ключа. EN: “Sword of Absolute Zero”; “Every hit to the enemy makes it”; “colder and weaker”. RU: “Меч Абсолютного Нуля”; “С каждым ударом по врагу всё больше”; “охлаждает и ослабляет его”. Модельные файлы `assets/lbc/items/icesword.json` → `lbc:item/icesword` и `assets/lbc/models/item/icesword.json` → `minecraft:item/icesword` уже существуют. Новый language/model template не нужен.

Отдельная таблица сохранена в `src/main/resources/data/lbc/loot_table/ported/ice_sword.json`. Полная `transformables_sword` не переносится в runtime, поскольку содержит другие неперенесённые предметы.
