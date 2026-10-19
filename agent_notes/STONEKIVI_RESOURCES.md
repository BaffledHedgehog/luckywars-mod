# Stonekivi resource import

`tools/import_lbc_stonekivi.py` imports the two Stonekivi wand variants from
OLD LBC `loot_table/tuntijaa_stone_tanner.json`, then imports the real
Tannerkivi donor from `loot_table/transformables_arcanum.json`. The source
resource tree is read-only. Existing `import_wand_family` and loot-schema
migration preserve the original name, lore components/order, tier, model,
tooltip style, and custom-data payload. Academy adds the common flags and
source modifiers `add_upgrade_desc` and `tiers/x`.

| Native ID | Source item | Name key | Model | Tooltip | Source custom data |
| --- | --- | --- | --- | --- | --- |
| `skywars2:stonekivi_wand` | `minecraft:carrot_on_a_stick` | `stonekivi_wand` (gray) | `lbc:stonekivi` | `lbc:tier_a` | `transformable:1,staff:1,raycastspell:1,stonekivi_wand:1` |
| `skywars2:academy_stonekivi_wand` | same | same | same | upgraded by source `tiers/x` | source flags plus `upgradable:0,upgrade_available:1,upgraded:1,mode:1` |
| `skywars2:tannerkivi` | `minecraft:repeating_command_block` | `tannerkivi` (gold) | `lbc:tannerkivi` | `lbc:tier_c` | `craftusable:1,transformable:1,arcanum:1,arcanumt:1,tannerkivi:1` |

The selected Tannerkivi entry is the actual donor in the arcanum transformable
loot table, rather than a fabricated item. Its original type is a repeating
command block; its name, model, tier, lore, and flags are retained.

The active Stonekivi paths use six legacy NBT templates under
`workingitems/awaken_tuntija_wands/stonekivi`: five sphere templates
(`stone_sphere_r10`, `iron_sphere_r8`, `obsidian_sphere_r6`,
`deepslate_sphere_r4`, `bedrock_sphere_r2`) and `earth`. The large Sun template
sequence is unreachable: `loop_sun_end` stores `command="magmify"` and returns
through `loop_sun_end_nerfed1` before those placements. The live Sun behavior
uses the legacy `/magmify` service instead. The resource importer does not copy
or alter legacy structures. No translation files are changed because
the source translate keys are preserved verbatim.

Run `python tools/import_lbc_stonekivi.py` to regenerate these three loot
tables. `--stonekivi` and `--tannerkivi` select one portion independently.
