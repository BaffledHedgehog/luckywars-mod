# Electro sword and shield resource import

Статус: оба серверных типа зарегистрированы в `LbcElectro`, loot tables
включены в сборку. Пользователь подтвердил сохранение жезла с вероятностью
60% и повторное отражение каждый тик. Поведение и фактические результаты
проверок: [перенос](ELECTRO_GEAR_PORT.md), [SERVER_TESTS](../SERVER_TESTS.md).

## Source identity

The legacy recipe function `data/lbc/function/other/craft/crafted_electrosword.mcfunction`
spawns `lbc:cyberkatana`; the corresponding source loot table is
`data/lbc/loot_table/cyberkatana.json`. Its source item is
`minecraft:carrot_on_a_stick`, with item model `lbc:cyberkatana`, display-name
key `cyberkatana`, and custom data including `cyberkatana:1`, `sword:1`, and
`swordwithability:1`. The `electro` sword advancement also uses that model.
It is therefore imported under the canonical native identifier
`skywars2:cyberkatana` and destination `ported/cyberkatana.json`.

The Electro shield comes from `data/lbc/loot_table/electro_shield.json`. Its
source base item is `minecraft:shield`, name key `energy_shield`, and custom
data includes `energy_shield:1`, `energyshield:1`, and `shield:1`. It uses the
vanilla shield item model, whose appearance is affected by the source
`minecraft:base_color` and `minecraft:banner_patterns` components. The native
identifier is `skywars2:electro_shield`.

## Resource pack reuse

Both source loot payloads retain their original name and lore components,
translate keys, formatting, tooltip styles, and item components. The
Cyberkatana reuses the existing `lbc:cyberkatana` item model. The shield keeps
its vanilla base item model and banner components. No resource-pack file or
duplicate template was created.

## Reproduction

Run `python tools/import_lbc_electro_gear.py` from the project root to select
the source entries by base item, identifying custom-data flags, and (for the
Cyberkatana) its model; the script then adapts the loot schema with
`migrate_loot_schema` and writes the two ported tables.
