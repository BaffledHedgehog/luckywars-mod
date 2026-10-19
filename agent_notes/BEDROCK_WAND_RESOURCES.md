# Bedrock Wand resource import

This import adds the ordinary Bedrock Wand and its Academy tier, and supplies the legacy Wall Wand donor loot payload for native crafting. The legacy server and the resource pack were read-only during this work.

## Sources

- Ordinary Bedrock Wand: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/tuntijaa_stone_wall.json`
- Academy upgrade: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/bedrock_wand.mcfunction`
- Academy modifier: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/tiers/ss.json`
- Wall Wand donor entry: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/transformables_staff.json`

## Outputs

- Ordinary Bedrock Wand: `src/main/resources/data/lbc/loot_table/ported/bedrock_wand.json`, native ID `skywars2:bedrock_wand`.
- Academy Bedrock Wand: `src/main/resources/data/lbc/loot_table/ported/academy_bedrock_wand.json`, native ID `skywars2:academy_bedrock_wand`.
- Wall Wand donor payload: `src/main/resources/data/lbc/loot_table/ported/wall_wand.json`. The initial Bedrock batch used a vanilla carrot-on-a-stick; the subsequent [Wall batch](WALL_WAND_PORT.md) registers native ordinary/Academy Wall IDs and imports through `tools/import_lbc_wall.py`.
- Reproducible importer: `tools/import_lbc_bedrock.py`. It reuses `import_lbc_wands.py` parsing and migration helpers; the Academy modifier is read from the actual source upgrade function.

## Preserved source data

The ordinary wand is named by the existing `bedrock_wand` translation key, uses `lbc:bedrock_wand` as its item model and `lbc:tier_a` tooltip style. Its source lore, component styles, and custom-data flags are preserved: `transformable:1`, `staff:1`, `magicwand:1`, `bedrock_wand:1`.

The source Academy function applies only `lbc:tiers/ss`; the generated Academy loot therefore uses `lbc:tier_ss`. The Academy payload also receives the standard generic-upgrade metadata (`upgradable:0`, `upgrade_available:1`, `upgraded:1`, `mode:1`) and the shared `add_upgrade_desc` modifier used by the Academy upgrade flow. These standard values are added alongside the source wand flags; the source name, model, lore and flags remain intact.

The Wall Wand donor was selected from `transformables_staff.json` by its `wall_wand` translation and `wall:1` custom-data marker. It retains model `lbc:wallwand`, tooltip tier `lbc:tier_c`, source name/lore, and flags `craftusable:1`, `transformable:1`, `staff:1`, `raycastspell:1`, `wall:1`. There is no native Wall Wand item registration yet, so this is prepared donor data only; its vanilla carrot-on-a-stick type keeps the loot table packable and does not implement the donor's independent Wall Wand ability; the native recipe accepts its source flag.

The configured resource pack was checked read-only. It already contains the Bedrock Wand and Wall Wand model files and existing translation/lore keys in English and Russian. No resource-pack files or new translation keys were added.
