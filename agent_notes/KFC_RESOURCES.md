# KFC resources — 20 October 2026

`python tools/import_kfc.py` extracts only the KFC gun entry from the read-only
legacy `transformables_shooter.json` and imports the edible
`kfc_yumyumyummy.json`. It uses the established Minecraft 26.3 loot-schema
migration, preserving all source metadata and text components. Only item IDs
change. No shooter random-pool change is included.

| Native item | Source proxy | Model | Tooltip | Custom data |
| --- | --- | --- | --- | --- |
| `skywars2:kfc_gun` | `minecraft:carrot_on_a_stick` | `lbc:kfcgun` | `lbc:tier_b` | `{transformable:1,shooter:1,magicwand:1,kfcgun:1}` |
| `skywars2:kfc_chick` | `minecraft:cooked_chicken` | `lbc:kfc_yum` | `lbc:tier_u` | `{kfc_chick:1}` |

Gun name: `kfc_gun`, `italic=false`. Four lore lines preserve the original nested
components: aqua `type` + `shooter`; aqua literal `1 ` + `recharge_time`; white
`kfc_gun.lore.1`; white `kfc_gun.lore.2`, all `italic=false`.
The source entry retains `weight=10`, `quality=0`, count 1.
Food name: `kfc_chick`, `italic=false`; one white lore line
`kfc_chick.lore.1`, `italic=false`, count 1. Its food behavior belongs to the
native item implementation; the source table supplies no food override.

Build resource includes needed:

- `data/lbc/loot_table/ported/kfc_gun.json`
- `data/lbc/loot_table/ported/kfc_chick.json`
- `data/lbc/loot_table/kfc_yumyumyummy.json`

The last path already existed in generated resources and now issues the native
food ID. It preserves the legacy loot reference used by `other/fire_drop_kfc`.
That function plays levelup at volume 0.5/pitch 1, emits 10 cloud particles,
spawns this loot and kills the source chicken.

Validation: importer completed successfully, checked generated JSON equality,
existing RU/EN translation keys, both item definitions and models, their texture
files, and tier B/U background/frame tooltip sprites. No new translation keys,
template files, models or textures are needed. The actual resource pack remains
read-only. No build or server run performed by this resource task.
