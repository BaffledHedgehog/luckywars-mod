# Native IMBA connected mining and ammunition

Author decisions: connected pickaxe mining follows the block actually broken,
including diagonal contacts, with the captured tool and native drops. Nothing
suppresses the pickaxe. Only the selected Infinite Arrow is free; an infinite
token elsewhere does not conserve ordinary/special ammunition. Fireworks are
always paid. Homing arrows are suppressed by TECH only, never MAG.

## Shared native implementation

`LbcImbaPickaxe` captures a successful native block break and its original block
type. A primitive frontier traverses 26 neighbors, ignoring block-state variants.
It shares `LbcImbaAxe.harvest` for native loot, container contents, captured tool,
XP, provenance and BedWars protection, and `LbcStonekivi` for bounded dispatch.
The original break owns wear; continuation invents no extra durability charge.
There is no arbitrary vein-radius/count limit and no synchronous flood fill.
Missing frontier chunks terminate those branches without loading terrain;
already harvested output waits for its resident publication cell. TimeStop holds
both work and publication. Existing graph capture preserves partial frontier,
visited cells, pending loot and owner links.

`LbcInfiniteAmmo` uses the actual selected ammo stack and the native weapon draw.
Vanilla creates its normal component-complete intangible free projectile, so
survival pickup cannot duplicate it. Ordinary bow/crossbow and both minigun
variants share this path. SLR uses the same selected-stack decision. Shish's
separate non-shooting trade retains its source inventory-token exception.

`LbcHomingArrows` keeps the actual native Arrow and its collisions, ownership,
damage, weapon, ammunition components and trinkets. Source activation changes
only the potion color. The source nearest-eye targeting, age-dependent fixed-
point acceleration, speed reduction, independent bounce rolls and red dust stay.
A valid cached target bounds the native spatial query; initial acquisition
reuses the existing loaded-target index. There is no new world scan/index service
or per-tick NBT decoding. Immutable persistent attachment state integrates with
canonical x2/x4, native lifecycle and the existing whole-match checkpoint.

The native Elytra and Firework variants retain their authored source components
and vanilla behavior/property builders. In 26.3 vanilla holder components are not
bound during registration; bootstrap must not read `Item.components()` then.
The Firework variant uses vanilla held-only crossbow selection, native rocket
factory/power/wear and native dispenser behavior. It never enters the arrows tag.

## Canonical resources and test access

Five ready leaves: `lbc:ported/imba_pickaxe`, `lbc:ported/infinite_arrow`,
`lbc:ported/imba_homing_arrow`, `lbc:ported/imba_elytra`,
`lbc:ported/imba_firework`. Existing source groups retain their pools, weights,
rolls, counts, conditions and modifiers. Homing source group still gives five
stacks of 64; its individual leaf gives one stack of 64. The Infinite Arrow's
bare vanilla-arrow reference is replaced by its native identity; reviewed legacy
modifier field aliases are adapted without changing values or presentation.
Reversible source comparisons are saved in the qualification stage.

No author resource-pack changes. Full IMBA pool admission, TMT-last/Bucks,
unrelated game modes and broad rewind/skill release gates remain unchanged.

## Qualification

Evidence is kept in `build/verification/imba-mining-ammo-2026-11-05/`.
Test SHA256 `8dde95bb02758d98437dc6ff455c1c05aea5e8f165adf090409cd31444096417`; production `1c4a874f38a531717d74e0c2ded3c4968463695be88b96a0b1a276471542934b`.
All14 isolated native matches passed on this exact archive: new mining/ammo and
homing suites plus both whole-coordinator replays in modes1/3; selected minigun
economy, projectile trinkets, special/Shulker and Madoka bows, previous IMBA tools
and Axe native harvest. Actual draw/charge/release, free-arrow pickup protection,
exact loot conservation and SAME original native bodies after rewind are checked.

22 owned Java families and12 resources overlay the qualified baseline;
10093 other source inputs and275 author paths are guarded.
All ZIP entries and retained3230 class references were checked. Original mixed
source line endings were preserved; a fresh compile produced byte-identical
test/production archives. Both build/libs and main are updated, with rollback
`main-rollback-4982ad59.jar`; main was neither stopped nor started, and no push
was performed. No physical-client, peak-TPS or broad rewind release claim.

