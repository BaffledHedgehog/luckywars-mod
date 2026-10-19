# Terra family recipe source audit

Read-only source audit for the upcoming native crafting implementation. Legacy
sources are under `E:/servers_mew/cringe1_21_4_skywars`; current code is in this
repository. Existing author requirement in `NEXT_MIGRATION_PLAN.md`: recipes
must use native 2x2/3x3 output slots; use the existing crafting-result helper,
`LbcTerra`, and common target filtering.

## Legacy recipe contract

`world/datapacksdisabled/LBC/data/lbc/function/other/craft/inventory_craft_check.mcfunction`
defines three Terra-family pairings. `craft_ground.mcfunction` has the two
awakening ground pairs; it also has the Tiny Terra pair.

| Inputs (one of each) | Result | Identity evidence |
|---|---|---|
| `stterra_tiny` + `stterra_tiny` | `sauvojen_tuntija_terra` (regular Terra wand) | `crafted_terra_inv.mcfunction` counts exactly two matching occupied crafting cells, fails otherwise, then reduces one from each; ground function kills two nearby Tiny Terra items then spawns same loot. |
| `stterra` + `terrasword` | `tuntijaa_terra_awakened` (`true_terra`) | Matching `custom_data` flags in dispatch; inventory function reduces one of each; ground function consumes nearest matching sword plus executor item. |
| `stterra` + `razorpine` | `tuntijaa_terra_razor` (`razorterra`, upgraded Terra pine gun) | Matching flags in dispatch; inventory function reduces one of each; ground function consumes nearest matching pine gun plus executor item. |

Each loot table produces one `minecraft:carrot_on_a_stick`. Output flags are
`stterra:1` for the regular wand, `true_terra:1` for awakened sword, and
`razorterra:1` for awakened pine gun. `true_terra` has `transformable:1`,
`sword:1`, `magicwand:1`; `razorterra` has `transformable:1`, `shooter:1`,
`magicwand:1`. Exact names, model and design are defined by the source loot
tables, not lore interpretation: regular wand model `lbc:st/terra`, output
sword `lbc:terrasword_true`, pine gun `lbc:terra_razorpine`; names use existing
keys `sauvojen_wand_terra`, `true_terra`, `razorterra`. Source loot also sets
tooltip tiers `lbc:tier_c` / `lbc:tier_s` (pine gun tier s); preserve all
existing lore component order and style on output.

The regular sword donor is defined in `data/lbc/loot_table/ported/terra_sword.json`
with model `lbc:terrasword` and flag `terrasword:1`. The regular staff source
is `loot_table/sauvojen_tuntija_terra.json`, flag `stterra:1`. The pine gun's
definition is in the same legacy datapack's item loot sources; its source flag
is `razorpine:1`. The resource pack's RU/EN language files already contain the
keys above (`razorpine.lore.1` through `.4`, `razorterra.lore`, and Terra staff/
sword lore); do not add duplicate keys or infer mechanics from prose.

## Legacy behavior and hazards

- Ground `craft_ground` dispatch is proximity-based (0.001–1 block), anchored
  on the executor item. Awakening functions kill `@s` plus the nearest matching
  donor item; with stacked/multiple donors or multiple executors, selection and
  repeated processing are ambiguous and can duplicate or consume unexpected
  items. The ground path has no ownership/pair reservation.
- Inventory awakening dispatch only checks presence of both markers. Its
  functions decrement every matching occupied cell and do not reject unrelated
  extra ingredients. Tiny Terra is stricter: it explicitly requires exactly
  two Tiny Terra cells. Native shapeless recipes should require exactly the two
  intended ingredient stacks in both 2x2 and 3x3 grids, and consume one of each.
- “Already upgraded” inputs do not match the donor flags: `true_terra` does not
  carry `terrasword:1`, and `razorterra` does not carry `razorpine:1`. Keep those
  as non-upgradeable unless the user specifies otherwise. Legacy code does not
  explicitly guard stack counts beyond consuming one per matching crafting
  cell; native recipes should naturally preserve remaining stack counts.
- The unrelated ground dispatch `stterra_tiny` + any nearby item with `Thrower`
  invokes `crafted_spruce_leaves.mcfunction`, which changes every nearby item
  entity's item id to spruce leaves and removes its components. It is a legacy
  side effect/bug, not a Terra recipe or intended native-craft behavior.
- Both awakening inventory callbacks spawn output loot and effects when called;
  previews are not modeled there. For a native recipe, assembly must be pure;
  particles/sound belong on actual result take, like the Rocket Boots helper.

## Current implementation reuse

At the time of this source audit, `RocketBootsRecipe` was a registered `CustomRecipe` with a `matches`/`assemble`
pair; `LbcCraftingResultMixin` calls `rocketCraftTaken` from `ResultSlot.onTake`
before ingredients are consumed. `LbcItems.rocketCraftTarget` provides a small
ingredient helper and `rocketCraftResult` builds the preview, while
`rocketCraftTaken` contains take-only effects. The migration plan's generalized
single `LbcCraftingRecipe` can reuse this split for three Terra recipes while
keeping existing rocket recipe id/behavior. Use `LbcTerra` for item identity /
construction if practical; do not couple preview assembly to world drops or
combat callbacks.

## Confirmed recipe requirements (user)

The awakening recipes use native 2x2 and 3x3 crafting grids, consume one of
each intended input, and reject any additional occupied ingredient cells. The
planned upgraded pine gun output is the legacy `razorterra` item from
`stterra + razorpine`, defined for the new registry item as
`skywars2:terra_razorpine` in `data/lbc/loot_table/ported/terra_razorpine.json`.

The implementation batch also includes the Tiny Terra pair (`stterra_tiny` +
`stterra_tiny` → regular Terra wand). The shared `LbcCraftingRecipe` now serves
all three Terra recipes and the existing rocket recipe. Current behavior and
verification: [TERRA_CRAFTING_PORT.md](TERRA_CRAFTING_PORT.md).
