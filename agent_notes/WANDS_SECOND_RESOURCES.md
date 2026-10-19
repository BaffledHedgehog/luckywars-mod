# Ресурсы Tiny Air, Stone и Teleport

Три точных entry импортированы из OLD LBC `data/lbc/loot_table/random_sauvojen_wand_tiny.json` в `data/lbc/loot_table/ported/tiny_{air,stone,teleport}_wand.json`. Каждый исходный entry имеет вес 1 и proxy `minecraft:carrot_on_a_stick`; импортер меняет только имя предмета на соответствующий native ID, сохраняя полный table/pool/item payload.

| Native ID | Source marker | Mana cost | Item model | Name key |
| --- | --- | ---: | --- | --- |
| `skywars2:tiny_air_wand` | `stair_tiny:1` | 250 | `lbc:st/air_tiny` | `wand_air_tiny` |
| `skywars2:tiny_stone_wand` | `ststone_tiny:1` | 500 | `lbc:st/stone_tiny` | `wand_stone_tiny` |
| `skywars2:tiny_teleport_wand` | `stteleport_tiny:1` | 750 | `lbc:st/teleport_tiny` | `wand_teleport_tiny` |

В source config costs заданы в `function/config.mcfunction`. Имя, lore и оформление каждого предмета остаются в исходных компонентах: общий тип `type`/`staff`, score-lore `tiny_*_wand_mana_use` с `mana_on_activate`, две tiny lore-строки и два соответствующих общего wand lore keys. RP уже содержит обе item/model JSON ссылки для трёх моделей и все используемые `wand_*`, `type`, `staff`, `mana_on_activate` ключи в `ru_ru` и `en_us`; новые переводы или template не нужны.

Сохранены source flags: Air — `craftusable`, `transformable`, `staff`, `magicwand`, `stair_tiny`; Stone — те же общие flags и `ststone_tiny`; Teleport — те же общие flags и `stteleport_tiny`. Узкий генератор `import_second_wand_resources()` использует `import_selected_entry`; независимый `check_second_wand_payloads()` сравнивает все три полных payload с мигрированным OLD LBC entry. Эти три файла отдельно включены в `processResources` allowlist.
