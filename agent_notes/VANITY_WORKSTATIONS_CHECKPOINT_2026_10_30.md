# Native workstation sessions for the Vanity checkpoint

## Contract and release boundary

The [full author contract](VANITY_ACTOR_CHECKPOINT_2026_10_30.md) remains mandatory:
FULL world/map, all players including dead/offline/newcomers, native/private state,
items/effects/attributes/counters/queues/NBT/tags, dimensions/positions/motion and
core parameters. This extends [native views](VANITY_VIEWS_CHECKPOINT_2026_10_30.md)
and [processing/storage sessions](VANITY_STORAGE_CHECKPOINT_2026_10_30.md).
It is NOT the full rewind transaction. Vanity acquisition remains disabled and the
full original25 Chaos gate stays CLOSED. No main installation/restart/push, reduced
pool, RP/template or pending-question edits. Questions belong in CHAT.

## Eight native workspace forms

MatchWorkstations owns the ephemeral menu-local workspaces for CraftingMenu,
AnvilMenu, SmithingMenu, GrindstoneMenu, StonecutterMenu, CartographyTableMenu,
LoomMenu and EnchantmentMenu. Unlike a chest or furnace, these input/result buffers
are not a terrain BE inventory. They must not be merged into the personal 2x2 grid
or restored by copying an old player's Inventory object.

Each image records the exact native menu kind and resident ServerLevel/block site;
the native buffer graph is grouped by actual container identity and referenced by
its first slot. Every local input and result ItemStack is independently copied,
including counts/components. ResultContainer.recipeUsed remains the captured
immutable native recipe definition. Native DataSlot values are copied independently,
including costs, selected indices, errors, enchanting seed/costs/clues. Player
inventory/XP/recipe book and the personal grid/cursor remain canonical body-owner
state. Physical blocks remain terrain-owner state.

Explicit runtime values also include Anvil repairItemCountCost/itemName/onlyRenaming;
Stonecutter cached input, independent recipe-entry list and lastSoundTime; Loom
independent selectable-pattern list and lastSoundTime; Cartography lastSoundTime;
and crafting ResultSlot.removeCount. Recipe/pattern entries are immutable registry
definitions, not mutable future menu references. Capturing an in-progress native
recipe-placement transaction is rejected. A detached ContainerLevelAccess.NULL or
unsupported menu/buffer form still requires its own adapter and fails explicitly.

## Replay without another recipe operation

MatchPlayerViews composes these images with the existing storage/camera/drag/title
owner. It preflights all online workspace destinations before reopening windows.
Fresh native constructors bind slots to the CURRENT player's inventory and restored
site; an already-correct class/site/title session retains its menu object and ID.
Saved buffers are copied directly into the native backing lists, then native data
and explicit runtime values are written. Replay invokes no slotsChanged/createResult,
button-selection, crafting award or result-pickup operation. This retains the exact
preview, costs and pending native recipe without re-rolling custom crafting.
Only the existing native full-sync publishes contents/data after replay. Cursor,
deep-copied title and drag state remain owned by MatchPlayerViews.

When abandoning a future workspace during rewind, local inputs/results/recipeUsed
are silently cleared BEFORE native close. This prevents removed() returning future
inputs into a restored inventory or dropping them into the restored world. Normal
gameplay close behavior is unchanged: discard is called only by checkpoint helpers.
Block inventories and player Inventory are never cleared by this workspace helper.

The EnchantmentMenu scratch RNG is not serialized by this component: native offer
generation and getEnchantmentList reseed it from the saved DataSlot before use.
Replay itself performs neither calculation. The player's NEXT randomly drawn seed,
general player/world RNG and registry-reload coordination still require the full
RNG/resource owners. This does not claim arbitrary private RNG restoration.

## Exact native evidence

Immutable artifact: `build\verification\skywars2-vanity-workstations-c4b037a2.jar`.
SHA256: `c4b037a2090a63d69e1c739c9dfbf80b63b56fb2a009ec3e0230d5beb02d4230`.
11,198 frozen inputs, including both runner scripts; all
5,352 processed class/resource entries matched the exact JAR.
Offline build passed. Source receipts cover 104 native archive
entries, 12 legacy functions and existing damage/view/crafting owners, 136 total.

Three expanded attempts were rejected and retained: the first omitted the dedicated
Tiny/Reroll test loot tables; the next provided those tables but immediately tested
world drops in an unready native destination section; player-ticket-only preparation
then failed the explicit chunk-readiness marker before dispatching the craft suites.
No gameplay assertion was
weakened. run_prepared.py uses the same native loot fixtures as the existing dedicated
runner, verifies resource publication, and supplies a native forced-chunk ticket at
the crafting location, with real world ticks plus execute-if-loaded confirmation before
the synchronous crafting/drop checks. Minecraft auto-enables this new test datapack
at startup, so the expanded run uses that explicit fixture resource configuration.
The pack, both level metadata files and native chunk-ticket files are restored exactly after confirmed normal
child exit; prepared-fixture.json records the tables and restoration evidence.
The Java/resource JAR stayed unchanged through all attempts.

The new workspace scenario first passed alongside storage/views/terrain in one
actual mode1 match (44.35 seconds). The SAME artifact then passed TWENTY-TWO suites
in ONE actual mode1 match: `vanity-workstations`, `vanity-storage`, `vanity-views`, `vanity-actors`, `vanity-player`, `vanity-core`, `vanity-terrain`, `map-lighting`, `time-stop`, `witch-acquisition`, `witch-lifecycle`, `polymorph`, `effect-layers`, `projectile-body-collision`, `entity-merging`, `momentum-teleports`, `glass-armor`, `indirect-kills`, `enchantment-rules`, `advanced-anvil`, `tiny-reforge`, `reroll-machine`.
Runner elapsed 62.14 seconds. Normal child exit, exact tracked file
snapshots restored, no suite/cleanup failures or retained locks. Focus mode purposely
retains qualified:false; the completed suite list defines this stage's proof.
Pre-existing bootstrap warnings remain; this is no physical-client, arbitrary-map
or peak-TPS qualification.

VanityWorkstationChecks opens all eight actual native menus for TWO players at ONE
site with independent workspace stacks, cursors and anvil names. Each round mutates
local stacks, clears abandoned future buffers without returning them, destroys the
workstation, causes genuine player death, restores terrain/core/body/actor/view
phases and requires an actual new player object with 36 CURRENT inventory bindings.
All saved local slots/data/components/cursors/XP and personal-buffer isolation must
match. A repeated view replay must retain the correct native window and emit no
extra open-screen packet. The Anvil round also disconnects/recreates the native
player and proves pending offline UUID replay while the other workspace stays valid.

Every restored crafting/repair/selection menu must actually allow native result
pickup into the CURRENT player inventory. Enchanting must actually consume lapis/XP
and enchant the restored item. Another complete replay repeats each operation and
compares actual slots/resources/XP and menu state. Before pickup, ALL saved data,
including the enchanting seed, must match exactly. Only the NEXT random seed drawn
after actual enchanting is excluded from this repeated-consumption comparison;
it belongs to the still-unfinished player RNG owner. This exclusion never applies to
the checkpoint seed or saved offer/clue restoration. The peer's workspace must remain
unchanged by the other player's pickup. Native advanced-anvil/enchantment rules and
both forced-random crafting suites also pass unchanged.

All 199 pre-existing author dirty paths and all 31 live override files remain
byte-identical. Main JAR remains
`d9087f0a07a321a5a9e57cac7f6f58a2d4ca6a86da9829a52623799f0342e087`.
Evidence: `build/verification/vanity-workstations/{baseline,candidate,sources,final-audit}.json`,
SHA-named frozen-input manifest and focused/final logs/results, numbered build logs
and bounded local commit receipt. Entire prior root-documentation tails are retained.

## Required follow-up

Beacon/merchant/entity-backed menus, native BE lid/private animation state and other
world services still need owners. Complete input/writer/lifecycle coordination,
automatic offline replay/persistence, private AI/navigation/sensors/RNG, native
multipart links, ability/attribution/FPV controllers, TimeStop, world clocks/game
events/listeners/metadata, entity-region/pending IO and map jobs/baselines remain
unfinished. Preserve all canonical terrain/core/body/actor/combat/view owners.
These eight workspace adapters do not enable full Vanity or release the Chaos pool.
