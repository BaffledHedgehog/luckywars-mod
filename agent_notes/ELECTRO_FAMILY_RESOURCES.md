# Electro wand resource import

## Native identifiers and item markers

The ordinary Electro item is `skywars2:electro_wand`; the Academy variant is
`skywars2:academy_electro_wand`. Their shared legacy selector marker is
`stelectro:1`. The ordinary payload preserves the other source markers
`craftusable:1`, `transformable:1`, `staff:1`, `cyberset:1`, and `magicwand:1`.
The Academy payload additionally carries `upgradable:0`,
`upgrade_available:1`, `upgraded:1`, and `mode:1`, following the native
Academy wand convention used by the Terra resource importer.

Both variants retain item model `lbc:st/electro`, tooltip style `lbc:tier_a`,
and translated name `sauvojen_wand_electro` with light-purple, non-italic
formatting. Lore is copied from the ordinary source entry, including the
dynamic `stelectro_wand_mana_use` score in `lbc.math`. The Academy payload also
applies the original `add_upgrade_desc` and `tiers/a` modifiers: a green
`upgraded_item` lore line and the source tier-A component.

## Source and reuse

The source loot entry is
`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/sauvojen_tuntija_electro.json`.
The Academy success function
`data/lbc/function/other/magic_academy/upgrade/stelectro.mcfunction` applies
`lbc:tiers/a`; the source `data/lbc/item_modifier/add_upgrade_desc.json` adds
the upgrade lore. The source resource pack already contains the English and
Russian translations for the Electro name and all five lore lines, the
`lbc.adv.upgraded_tuntija.electro` translations, and
`assets/lbc/items/st/electro.json`. No resource-pack files or translation keys
were changed or duplicated.

The focused import is `python tools/import_lbc_wands.py --electro-family`.
It writes only `electro_wand.json` and `academy_electro_wand.json` under
`src/main/resources/data/lbc/loot_table/ported/`. Both are included by
`processResources` in `build.gradle`.
