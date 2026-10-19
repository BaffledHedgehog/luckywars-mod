# Blaze Wand resource payloads

## Sources and import

The read-only legacy datapack root used by the importer is
`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc`.
The user resource pack at the path in [RESOURCE_PACK.md](RESOURCE_PACK.md) was
read-only; this task did not write to it or create a resource pack template.

| Item | Exact source loot table | Selected item marker | Ported output |
| --- | --- | --- | --- |
| Blaze Wand (Fallingstars) | `loot_table/transformables_staff.json` | `blaze_wand` and `fallingstars:1` | `src/main/resources/data/lbc/loot_table/ported/blaze_wand.json` |
| Fireblaze Wand | `loot_table/tuntijaa_fire_blaze.json` | `fireblaze_wand` and `fireblaze_wand:1` | `src/main/resources/data/lbc/loot_table/ported/fireblaze_wand.json` |

Run from the project root with `python tools/import_lbc_fireblaze.py`. The
importer selects exactly one matching entry per source, migrates only the loot
JSON schema using the existing helper, preserves pool metadata and the complete
item payload, and changes only the outer item ID to `skywars2:blaze_wand` or
`skywars2:fireblaze_wand`. It is deterministic and safe to rerun.

## Payload comparison

The ordinary Blaze Wand keeps the source `minecraft:carrot_on_a_stick` payload:
name key `blaze_wand`; six lore lines/rows with `type`, `staff`, the mana score
and `mana_on_activate`, three `blaze_wand.lore.*` lines, then
`craft_with_fire`; model `lbc:blazewand`; tooltip style `lbc:tier_c`; and custom
data `{craftusable:1,transformable:1,staff:1,raycastspell:1,fallingstars:1}`.
The Fireblaze Wand keeps name key `fireblaze_wand`; four lore rows with `type`,
`staff`, the mana score and `mana_on_activate`, then two `fireblaze_wand.lore.*`
lines; model `lbc:blazefire`; tooltip style `lbc:tier_a`; and custom data
`{transformable:1,staff:1,magicwand:1,fireblaze_wand:1}`. All source
translation components, score arguments, component order and explicitly set
formatting are retained.

The resource pack's `assets/lbc/items/blazewand.json` and
`assets/lbc/items/blazefire.json` point to their corresponding existing models;
the model JSONs use `minecraft:item/blazewand` and `minecraft:item/blazefire`
textures. Existing `assets/minecraft/lang/ru_ru.json` and `en_us.json` contain
the item names and lore keys listed above, along with `type`, `staff`,
`mana_on_activate` and `craft_with_fire`. No translation keys or resource-pack
files need to be added or changed for these two payloads.

## Academy tier modifiers and remaining integration

Source behavior confirms Academy upgrades are modifiers applied after the
ordinary loot payload: `function/other/magic_academy/upgrade/fallingstars.mcfunction`
uses `lbc:tiers/a`, while `.../upgrade/fireblaze_wand.mcfunction` uses
`lbc:tiers/s`. Those modifier JSONs set tooltip styles `lbc:tier_a` and
`lbc:tier_s` respectively. Academy blaze is therefore tier A; Academy
Fireblaze is tier S. Do not bake an Academy tier into the ordinary payload.
The Academy advancements match `upgraded:1` with the same `fallingstars:1` or
`fireblaze_wand:1` marker.

The old crafting path creates Fireblaze from the fallingstars marker and an
existing Fire wand marker; `function/other/sauvojen_tuntija/crafted_blazefirewand_inv.mcfunction`
shows both inventory crafting ingredients. The output table for that craft is
`lbc:tuntijaa_fire_blaze`. A recipe in the new project still needs native
recipe definition and handling for the legacy custom-data ingredient, plus
registration/creation of both native item IDs and the Academy tier modifier
behavior. Whitelist for those follow-up edits: relevant item registration and
Polymer/model mapping, the Fireblaze recipe and its crafting validation, and
the Academy upgrade/tier modifier integration. This resource-only change did
not alter Java, recipes, registrations, build files, or other resources.
