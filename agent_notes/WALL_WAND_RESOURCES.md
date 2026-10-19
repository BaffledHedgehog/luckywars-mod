# Wall Wand resource import

The native loot tables preserve the ordinary Wall Wand entry from the legacy `transformables_staff` table and use item IDs `skywars2:wall_wand` and `skywars2:academy_wall_wand`. Its established presentation is kept: translate key `wall_wand`, model `lbc:wallwand`, and the source lore, colors, tooltip style, and custom data. The ordinary tooltip style is tier C. The Academy upgrade applies the shared Academy upgrade lore and source `tiers/a` modifier, so the Academy item uses tier A.

The source sets `wall_wand_mana_use` to 1500 in `function/config.mcfunction`; that is the ordinary activation cost displayed by the preserved score component. The source lore also shows a cast distance of 100. Academy behavior uses its original fixed 500 cost and approximately 180-block range; the inherited source lore remains unchanged.

Mode four uses the original `lbc:stair5x5x5` structure: 5×5×5 bounds with 45 `brick_stairs` entries. The structure and the original `minecraft:bricks` block tag (bricks, stairs, slab, wall) are whitelisted in the JAR. Native rotation changes block states and bounds without copying or inventing a second stair layout.

`tools/import_lbc_wall.py` imports the family with the shared `import_wand_family` helper. `tools/import_lbc_bedrock.py` invokes it after importing Bedrock so the existing combined resource-import workflow creates both Wall Wand tables. The Academy state uses the common flags `upgradable:0`, `upgrade_available:1`, `upgraded:1`, and `mode:1`; `upgrade/wall.mcfunction` supplies `tiers/a`.

Read-only source references:

- `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/transformables_staff.json` (ordinary `wall:1` entry)
- `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/wall.mcfunction`
- `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/config.mcfunction`
- `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/add_upgrade_desc.json`
- `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/tiers/a.json`
