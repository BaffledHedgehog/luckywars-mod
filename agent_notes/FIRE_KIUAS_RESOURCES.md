# Fire + Kiuaskivi item resources

The importer `tools/import_lbc_kiuas.py` derives two native loot tables from the
legacy LBC source entries. The legacy base item is the repeating command block
proxy; the importer changes only the outer item ID and adapts legacy loot JSON
schema to the current schema.

| Native item | Legacy donor | Preserved item model | Preserved name key |
| --- | --- | --- | --- |
| `skywars2:kiuaskivi` | `transformables_arcanum.json`, `lbc:kiuaskivi`, tag `kiuaskivi:1` | `lbc:kiuaskivi` | `kiuaskivi` |
| `skywars2:sun_arcanum` | `tuntijaa_fire_kiuas.json`, `lbc:the_sun`, tag `sun_arcanum:1` | `lbc:the_sun` | `sun_arcanum` |

Both entries retain source tooltip style, count, full lore order/components,
scoreboard names, colors and italic flags, plus all source custom-data fields.
There is no source evidence for an Academy Kiuaskivi or Sun variant, so neither
is generated. Existing translation keys and item models are supplied by the
user resource pack; no lang or resource-pack template additions are needed.

Regenerate deterministically with `python tools/import_lbc_kiuas.py`.

## Kiuaskivi fill order clarification

The fire replacement runs after the air replacement. In Minecraft 26.3,
`#minecraft:replaceable` contains `minecraft:air` (confirmed in the bundled
26.3 server jar's `data/minecraft/tags/block/replaceable.json`). Since the
source `kiuaskivi_to_fire` tag includes `#minecraft:replaceable`, a block
matched by `kiuaskivi_to_air` becomes air and then matches the fire pass. Any
claim that `TO_AIR` entries cannot become fire is incorrect; preserve this
sequential result when implementing the fills.
