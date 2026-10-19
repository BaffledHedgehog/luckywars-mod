# Air wand resource import

The Air family is imported from the read-only legacy source at
`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc`.
Run `python tools/import_lbc_wands.py --air` to regenerate only these resource
tables:

- `src/main/resources/data/lbc/loot_table/ported/air_wand.json`
- `src/main/resources/data/lbc/loot_table/ported/academy_air_wand.json`

The ordinary payload comes from `loot_table/sauvojen_tuntija_air.json`. Its
source identity is `minecraft:carrot_on_a_stick`, item model `lbc:st/air`,
tooltip style `lbc:tier_b`, and translated name key `sauvojen_wand_air`. Its
name and lore components are copied from the source without writing or
guessing translated text. The lore contains the `type` + `staff` line, the
`stair_wand_mana_use` score + `mana_on_activate` line, and the four source
keys `sauvojen_wand_air.lore.1` through `.4` with their original colors and
italic settings.

The source custom data is exactly
`{craftusable:1,transformable:1,staff:1,magicwand:1,stair:1}`. The default
mana cost is 1000, from `function/config.mcfunction`'s
`stair_wand_mana_use` score assignment.

The Academy entry retains the ordinary name, model, lore, and custom data,
then adds `upgradable:0,upgrade_available:1,upgraded:1,mode:1` through the
shared family importer. It appends the source `upgraded_item` lore from
`item_modifier/add_upgrade_desc.json` and applies `item_modifier/tiers/s.json`,
which sets the tooltip style to `lbc:tier_s`. No new translation text is
introduced.
