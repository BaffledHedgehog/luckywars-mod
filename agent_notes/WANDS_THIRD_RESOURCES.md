# Ресурсы Tiny Ruby и Terra

Из OLD LBC `data/lbc/loot_table/random_sauvojen_wand_tiny.json` импортированы два entry с весом 1 и proxy `minecraft:carrot_on_a_stick` в `data/lbc/loot_table/ported/tiny_{ruby,terra}_wand.json`. `import_third_wand_resources()` вызывает существующий точный `import_selected_entry`; кроме замены item ID на native ID, table/pool и все item components сохранены.

| Native ID | Source marker | Mana cost | Item model | Name key |
| --- | --- | ---: | --- | --- |
| `skywars2:tiny_ruby_wand` | `struby_tiny:1` | 500 | `lbc:st/ruby_tiny` | `wand_ruby_tiny` |
| `skywars2:tiny_terra_wand` | `stterra_tiny:1` | 250 | `lbc:st/terra_tiny` | `wand_terra_tiny` |

Значения mana заданы в source `function/config.mcfunction`; lore также использует score keys `tiny_ruby_wand_mana_use` и `tiny_terra_wand_mana_use` вместе с `mana_on_activate`. Оба предмета сохраняют общий lore `type`/`staff`, две tiny lore-строки и общий золотой lore об объединении с копией для улучшения. Ruby lore описывает превращение находящихся рядом гримуаров в заклинания без расхода жезла; Terra lore описывает превращение находящихся рядом предметов в еловую листву без расхода жезла. Это передача заявленного текста, не проверка фактической механики.

Сохранённые flags: Ruby — `craftusable`, `transformable`, `staff`, `magicwand`, `struby_tiny`; Terra — те же общие flags и `stterra_tiny`. Live RP содержит `assets/lbc/items/st/{ruby_tiny,terra_tiny}.json`, модели в `assets/lbc/models/item/st/{ruby_tiny,terra_tiny}.json` и все name/lore/shared translate keys для `ru_ru` и `en_us`; дополнительных lang или template файлов не нужно. `check_third_wand_payloads()` независимо выбирает оба source entry и сверяет полный мигрированный payload; новые таблицы указаны в `processResources` allowlist.
