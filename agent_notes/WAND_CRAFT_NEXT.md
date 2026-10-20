# Wand craft continuations: bounded next batch

Research-only comparison of the legacy ground/inventory continuations for the newly ported Fire, Stone, Blind, Air, Teleport, and Ruby staff families against `LbcCraftingRecipe`.

## Legacy recipe matrix

Each entry is a two-item shapeless combination: the base wand's `custom_data` flag plus the partner flag yields the listed legacy output flag / loot table. The ground trigger is `lbc:function/other/craft_ground.mcfunction`; the inventory trigger is `lbc:function/other/craft/inventory_craft_check.mcfunction`. The paired `crafted_*` and `crafted_*_inv` functions consume both inputs and spawn the loot result.

| Base wand flag | Partner flag | Output flag / loot table | Legacy implementation |
|---|---|---|---|
| `stfire` | `kiuaskivi` | `sun_arcanum` / `tuntijaa_fire_kiuas` | `crafted_kiuaswand{,_inv}` |
| `stfire` | `fallingstars` | `fireblaze_wand` / `tuntijaa_fire_blaze` | `crafted_blazefirewand{,_inv}` |
| `ststone` | `wall` | `bedrock_wand` / `tuntijaa_stone_wall` | `crafted_wallstone{,_inv}` |
| `ststone` | `tannerkivi` | `stonekivi_wand` / `tuntijaa_stone_tanner` | `crafted_tannerstone{,_inv}` |
| `stblind` | `psychowand` | `psychoblind_wand` / `tuntijaa_blind_psycho` | `crafted_psychoblind{,_inv}` |
| `stblind` | `summonervexes` | `psychovex_wand` / `tuntijaa_blind_vex` | `crafted_psychovex{,_inv}` |
| `stair` | `exoperso` | `exasperato_wand` / `tuntijaa_air_exasperoso` | `crafted_exasperoso{,_inv}` |
| `stair` | `truefallingstars` | `exahoming_wand` / `tuntijaa_air_homing` | `crafted_exahoming{,_inv}` |
| `stteleport` | `lightning_bolts` | `boltbolt_wand` / `tuntijaa_bolts_bolts` | `crafted_boltbolt{,_inv}` |
| `stteleport` | `bolt_material` | `infbolt_wand` / `tuntijaa_bolts_reforging` | `crafted_ultrareforge{,_inv}` |
| `struby` | `cons_material` | `consuming_wand` / `tuntijaa_greed_destroy` | `crafted_greed{,_inv}` |
| `struby` | `greed_greed` | `dublicating_wand` / `tuntijaa_greed_avarice` | `crafted_avarice{,_inv}` |

Recipe branches are at `data/lbc/function/other/craft_ground.mcfunction` and `data/lbc/function/other/craft/inventory_craft_check.mcfunction` (same-row flags); exact consume/output functions are under `data/lbc/function/other/sauvojen_tuntija/`. Loot outputs and their output flags are under `data/lbc/loot_table/tuntijaa_*.json`. Legacy flag matching checks item custom data, not item model/name.

## Port status

The six base families are represented in `LbcWands` and their behavior owners (`LbcAir`, `LbcStone`, `LbcBlind`, `LbcTeleport`, `LbcRuby`). Air + Exoperso, Stone + Wall, Stone + Tannerkivi, Fire + Kiuaskivi, Fire + Fallingstars, Blind + Psychowand, Blind + Summoner Vexes, Air + True Falling Stars, Teleport + Lightning Bolts, Teleport + bolt_material, Ruby + cons_material and Ruby + greed_greed are recognized by `LbcCraftingRecipe.Kind.EXASPERATO`, `Kind.BEDROCK`, `Kind.STONEKIVI`, `Kind.SUN`, `Kind.FIREBLAZE`, `Kind.PSYCHOBLIND`, `Kind.PSYCHOVEX`, `Kind.EXAHOMING`, `Kind.BOLTBOLT`, `Kind.INFBOLT`, `Kind.CONSUMING` and `Kind.DUPLICATING`; all twelve combinations are implemented and verified in native matches (12/12). Existing rocket transfer, Terra awakening, and Electro/Katana kinds share the same native recipe/result infrastructure.

Among the twelve result flags, `exasperato_wand`, `bedrock_wand`, `stonekivi_wand`, `fireblaze_wand` and `psychoblind_wand` have registered ordinary/Academy items and live mechanics (`LbcExasperato`, `LbcBedrock`, `LbcStonekivi`, `LbcBlaze`, `LbcPsycho`). `exahoming_wand` has ordinary/Academy mechanics in `LbcHoming`, plus its independent truefallingstars donor; two-match runtime verification passed, as recorded in SERVER_TESTS.md. `sun_arcanum` has its native item and automatic ability in `LbcKiuas`; `psychovex_wand` has its native item and six cultists in `LbcSummons`. No Academy variants were found for these two. Actual verification is recorded in SERVER_TESTS.md. `boltbolt_wand` has ordinary/Academy mechanics in `LbcBolts`, plus its independent Lightning Bolts donor. `infbolt_wand` has its free magic-tool implementation and native lightning-egg donor in LbcBolts/LbcWands, without any Academy version (user). Its build, two native matches and shared regressions passed on SHA 3dd622a6; see SERVER_TESTS.md. `consuming_wand` has its ordinary/Academy mechanics and independent donors in `LbcConsuming`, with both targeted matches passing. `dublicating_wand` now has ordinary/Academy mechanics and independent Divider donor in `LbcDuplicating`. The normal mode follows the author-approved random patch rework, and Expand targets the spell dimension. See DUPLICATING_PORT.md and SERVER_TESTS.md for five final native matches on SHA 6e4879a2 and the archive without installation.

## Completed first batch and next stage

`stair` + `exoperso` -> `exasperato_wand` now has a native 2x2/3x3 recipe, exact consumption, source output presentation and priority over vanilla repair. Verification and installation are recorded in SERVER_TESTS.md and AIR_EXASPERATO_CRAFT_PORT.md.

The separate Exoperso donor ability now has ordinary/Academy items and runtime
implementation; see EXOPERSO_PORT.md and SERVER_TESTS.md for its verification.
The recipe accepts native Exoperso types as well as legacy custom data.
Stone + Wall -> Bedrock now has a native 2x2/3x3 recipe and full ordinary/Academy
result behavior; see BEDROCK_WAND_PORT.md and SERVER_TESTS.md. Wall now has native
ordinary/Academy Polymer items and its independent four-mode ability; see
WALL_WAND_PORT.md. The recipe recognizes both native Wall IDs without requiring
legacy custom data, and continues accepting legacy `wall:1` donors.
Stone + Tannerkivi -> Stonekivi now uses the same native recipe engine, with
ordinary/Academy result modes and the independent donor aura; see STONEKIVI_PORT.md
and SERVER_TESTS.md for actual verification and deployment status.
Fire + Kiuaskivi -> Sun Arcanum now has its automatic donor/result abilities,
source items and native recipe; see FIRE_KIUAS_PORT.md and SERVER_TESTS.md for
actual verification/deployment status. Fire + Fallingstars -> Fireblaze now has
the independent donor, ordinary/Academy result modes and native recipe; see
FIRE_BLAZE_PORT.md and SERVER_TESTS.md. Blind + Psychowand -> Psychoblind now
has its independent donor, ordinary/Academy result, native recipe and bounded
native runtime checks; see PSYCHO_PORT.md and SERVER_TESTS.md.
Blind + Summoner Vexes -> Psychovex now has its independent donor, native recipe,
six cultist profiles and event-based callbacks; see PSYCHOVEX_PORT.md and SERVER_TESTS.md.
Air + True Falling Stars -> Exahoming now has its independent donor,
ordinary/Academy result and native recipe; see [EXAHOMING_PORT.md](EXAHOMING_PORT.md).
Build, two native matches and one shared regression match passed on SHA e985d0ba;
the verified JAR is ready without installation. Details and limits: SERVER_TESTS.md.
The original resource pack and template remain unchanged.
Teleport + Lightning Bolts -> Boltbolt is implemented; see [BOLTBOLT_PORT.md](BOLTBOLT_PORT.md) and SERVER_TESTS.md for verified behavior and artifact status.
Teleport + bolt_material -> Infbolt is implemented with the shared Reforge queue and native crafting; see [INFBOLT_PORT.md](INFBOLT_PORT.md). Build and five native matches passed on SHA 3dd622a6; the JAR is archived without installation. Ruby + cons_material -> Consuming is now implemented, including Silence sphere48 and dimension-wide Avarice (user). Ruby + greed_greed -> Duplicating is implemented and verified (DUPLICATING_PORT.md). Author decisions about random transformations, cross-dimension destinations, container contents and owned trial centers are recorded in DUPLICATING_SOURCE.md and AGENTS.md. Next step is a coverage audit of remaining items and their acquisition/loot pipelines.
Other continuations likewise need functional results before recipes are exposed.
AGENTS.md excludes old ground scanners and dropped results.
