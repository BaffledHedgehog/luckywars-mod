# Ordinary Acid wand source audit

The next regular wand candidate is the ordinary **Acidacid** wand (`acidacid_wand`),
not the already-portable Tiny Acid wand (`tiny_acid_wand`). Its source item is
`world/datapacksdisabled/LBC/data/lbc/loot_table/tuntijaa_acid_acid.json`: carrot-on-a-stick,
`lbc:acidacidwand`, `lbc:tier_s`, dark-green `acidacid_wand` name, then the source
four lore rows: `type` + `staff` in aqua; mana score `acidacid_wand_mana_use` +
`mana_on_activate` in aqua; `acidacid_wand.lore.1` and `.lore.2` in white.
The custom-data marker is `acidacid_wand:1` with `transformable`, `staff`, and
`magicwand`. Mana cost is 1450 (`function/config.mcfunction`).

Ordinary use is selected by `spell1_default.mcfunction` / its left-hand sibling,
then `workingitems/awaken_tuntija_wands/acidacid/click.mcfunction` charges mana
and calls `click2.mcfunction`. That creates one forward-facing `great_acid`
marker with 500-tick lifetime and runs the `lbcr:raycast_acidbig_*_very` family;
the per-tick `tick.mcfunction` advances the ray, switches to its burning path,
spawns occasional effects, and detonates on obstruction. Crafting is a manual
recipe: `function/other/craft_ground.mcfunction` or
`function/other/craft/inventory_craft_check.mcfunction` combines an `stacid`
wand and an `acidwand` wand, then `other/sauvojen_tuntija/crafted_acidacid*.mcfunction`
rolls the source loot table. It is not a registered shaped recipe.

Existing native support in `LbcClouds.wandAcid()` is specifically the Tiny Acid
cast: it launches six `AcidProjectile`s with random spread, speed 0.5, and the
shared acid impact/lifecycle logic. It is a useful target/impact and attribution
reference, but it does not reproduce the ordinary wand's single forward ray,
500-tick marker lifecycle, `great_acid` raycast scale, obstruction detonation,
or mana value. Existing `Spell.ACID` is registered only against `TINY_ACID`
with cost 63. The ordinary wand should therefore get its own item/dispatch entry
and cast path; reusing the tiny spell unchanged would alter source behavior.

The ordinary source is a bounded regular cast plus one recipe hook. Its dependent
raycast/damage family is appreciably larger than `wandAcid()`, but the inspected
files did not reveal an obvious source defect that requires stopping this port.
