# Tiny wand pair recipes — source audit, 2026-10-20

Follow-up: all nine pairs and ordinary Acid are now implemented; this audit
records the missing pieces found before the patch. See [port status](TINY_WAND_CRAFT_PORT.md)
and [native verification](../SERVER_TESTS.md). The user's earlier acid physics
override applies: native gravity/drag and sticky surfaces replace the legacy
straight/no-gravity marker movement described below.

Legacy root: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc`.
Exactly nine identical-tiny pair recipes are dispatched by [inventory_craft_check.mcfunction](/E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/inventory_craft_check.mcfunction) and [craft_ground.mcfunction](/E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft_ground.mcfunction).

Each row consumes two identical tiny wands and produces one ordinary wand. All item IDs below use `skywars2:`; loot IDs use `lbc:`. Legacy ingredient predicates require the stated custom-data flag equal to 1.

| Tiny item | Ingredient flag | Legacy output loot | Current ordinary item / constant | Legacy craft function |
| --- | --- | --- | --- | --- |
| tiny_fire_wand | stfire_tiny | sauvojen_tuntija_fotja | fire_wand / FIRE_WAND | crafted_fire |
| tiny_blind_wand | stblind_tiny | sauvojen_tuntija_blind | blind_wand / BLIND_WAND | crafted_blind |
| tiny_air_wand | stair_tiny | sauvojen_tuntija_air | air_wand / AIR_WAND | crafted_air |
| tiny_stone_wand | ststone_tiny | sauvojen_tuntija_ananta | stone_wand / STONE_WAND | crafted_stone |
| tiny_teleport_wand | stteleport_tiny | sauvojen_tuntija_teleport | teleport_wand / TELEPORT_WAND | crafted_teleport |
| tiny_ruby_wand | struby_tiny | sauvojen_tuntija_swargia | ruby_wand / RUBY_WAND | crafted_swargia |
| tiny_terra_wand | stterra_tiny | sauvojen_tuntija_terra | terra_wand / TERRA | crafted_terra |
| tiny_electro_wand | stelectro_tiny | sauvojen_tuntija_electro | electro_wand / ELECTRO | crafted_electro |
| tiny_acid_wand | stacid_tiny | stacid_wand | acid_wand / ACID_WAND (added in this patch) | crafted_acid |

For every function name in the last column, the files are `function/other/craft/<name>.mcfunction` and `<name>_inv.mcfunction` under the legacy root. The `_inv` versions count exactly two matching occupied slots among `player.crafting.0..3`, then reduce each matching stack by one. Preserve the intended pair recipe, not legacy acceptance of unrelated extra ingredients or ground scanning.

All nine use exactly the same craft effects: `particle minecraft:enchant ~ ~.2 ~ 1 1 1 1 100 force` and `playsound minecraft:block.enchantment_table.use master @a ~ ~ ~ 1 1`. They belong on actual result taking, never preview. [AGENTS.md](../AGENTS.md:724) requires native 2×2 and 3×3 output-slot crafting, exact ingredient consumption, stack remainders, rejection of extras and Shift-click support through the existing recipe/result callback.

The nine source output loot files are also present under [src/main/resources/data/lbc/loot_table](../src/main/resources/data/lbc/loot_table). Those retained source tables still specify `minecraft:carrot_on_a_stick`; current registered item defaults are in [LbcWands.java](../src/main/java/me/skywars2/game/LbcWands.java:59). Registration currently covers all nine tiny items and eight corresponding ordinary items. Their ordinary translation keys are `sauvojen_wand_<element>` and models `lbc:st/<element>`; Ruby/Fire/Stone naming mismatches in legacy loot filenames are intentional.

Acid is a real source pair recipe, not an inferred extension: [crafted_acid_inv.mcfunction](/E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/crafted_acid_inv.mcfunction:1) produces [stacid_wand.json](../src/main/resources/data/lbc/loot_table/stacid_wand.json). Output is count 1, model `lbc:st/acid`, translation `sauvojen_wand_acid`, custom data `{craftusable:1,transformable:1,staff:1,magicwand:1,stacid:1}`. Its source existence does not imply the ordinary Acid behavior has been ported.

Terra and Electro already have pair recipes in [LbcCraftingRecipe.java](../src/main/java/me/skywars2/game/LbcCraftingRecipe.java:17); Terra recognition lives in `LbcTerra.craftingKind`, Electro recognition in the shared recipe. Do not duplicate them. Other seven pair recipes were absent at audit time.

Exclusions: no tiny Exoperso registration or source `exoperso`/`tiny` pair was found. Exoperso is the independent `megumin_wand` ordinary item. These nine recipes produce ordinary versions, never Academy versions. Mixed tiny elements, tiny + spell, Electro equipment crafts, Acid dust, Fire mana, Stone unbreakable and Teleport reforge are separate recipes/mechanics. Electro equipment's 60% tiny-wand preservation is an explicit exception for those equipment recipes only; identical-tiny pairs consume both ingredients.

## Ordinary Acid behavior needed for the ninth result

Source entry: [acid.mcfunction](/E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/sauvojen_staffs/acid.mcfunction), then sibling `acid_preloop.mcfunction`; default/offhand dispatch requires `stacid:1`. Config cost is **250 mana**. Emit **one** eyes-origin `raycastacidbig` marker, TTL **300 active ticks**, owner/team copied, yaw spread ±30°, pitch spread ±20° (`universal_razbros`). Sound zombie villager cure volume1 pitch2. Shared cast bonuses follow the existing wand path.

Tick source: [raycast_acidbig.mcfunction](/E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_acidbig.mcfunction). It advances **0.2 blocks per active tick while its current block is #airs**, otherwise remains still. No gravity, separate cloud emission, terrain alteration or impact removal occurs. It repeatedly affects enemies within **5 blocks**: nonburning poison **600 ticks, amplifier0** (`damagewithid2_lite_big`); burning **4 on_fire damage per tick** (`damagewithid2_increased_big`), retaining native damage cooldown semantics. Same owner/team and spectators are excluded. Each tick plays fire extinguish volume .04 pitch2.

Normal particles: sneeze count2, offsets(.5,.5,.5), speed .1; forced dust transition `[.035,.886,.4945]` to `[.025,.46,.3945]`, scale2, same offsets/speed/count. Burning particles: flame count15, offsets(.5,.1,.5), speed .2, positioned -.5 along direction. Burning increments age by **3 extra per tick**, in addition to shared +1. Ignition becomes permanent if any block in the surrounding **3×3×3** cube is #firelava, any burning entity is within5, or any on_fire #raycasters entity is within7 (`raycast_acidbig_check_if_fire`).

Reuse assessment: [LbcClouds.java](../src/main/java/me/skywars2/game/LbcClouds.java:76) already owns native `AcidProjectile`, collision/sticking, reflection ownership, effects, epoch cleanup and active TTL. `wandAcid` currently emits the tiny wand's six ballistic shots and `acidImpact` uses radius2.5, poison300, burn3. Extend that common owner with a distinct ordinary profile rather than invoking the tiny spell unchanged or creating a second effect engine. Ordinary needs one straight/no-gravity shot, speed .2, radius5, poison600, burn4, larger ignition checks and source particles. Preserve source timing through the common active-tick lifecycle. No user decision is needed to identify these values; native swept collision can use the established projectile adaptation.
