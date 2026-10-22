# Remaining acquisition routes — bounded source review, 21 October 2026

[Reroll Machine is now qualified](REROLL_MACHINE_PORT.md), including kit25 and both crafting grids.
The route descriptions below remain historical source findings; current next
review is [LuckyWars modifier ownership](LUCKYWARS_LUCK_SOURCE.md).

Current status: [ambient lightning acquisition is qualified](LIGHTNING_ACQUISITION_PORT.md).
[Tiny Teleport forced random crafting is also qualified](TINY_REFORGE_CRAFT_PORT.md).
The ground route below is historical source evidence. Current native contract is
an opaque stable preview, one donor per committed take, 30% consumption of one
Tiny and both crafting grids. Earlier whole-stack/whole-Tiny suggestions below
are not the native target; no ground interaction was enabled.


Read-only source/current Java review; no runtime qualification performed. Shooter
pool closure is established by SHOOTER_POOLS_PORT.md. Catalog suggestions do not
gate table acquisition. No Java, resources, build or legacy files changed here.

## Recommended next implementation: Tiny Teleport ground Ultra Reforge

Legacy root: `LBC/data/lbc/function/other/craft_ground.mcfunction:63`.
A dropped item with exact integer `stteleport_tiny:1` selects one transformable
item at distance `0.001..1`; the selected item's `Thrower` must exist. There is
no explicit `sort=nearest`, so do not describe this as a nearest-item contract.
`other/craft/crafted_reforge.mcfunction` executes Ultra Reforge as that item,
then enchant particles/sound and `chance30` removal of the entire Tiny item
entity. No mana cost or active spell is involved. `ultra_reforge.mcfunction`
and `reduce_count.mcfunction` recursively process the selected input stack.

Current gap: `LbcItems.loaded/removed/tick` tracks Rocket ground-crafting inputs,
but no Tiny ground recipe exists; `LbcCraftingRecipe` Tiny Teleport handles its
wand-upgrade recipe. `LbcWands` Tiny Teleport levitation is a separate action.
`LbcBolts.java:401–462` already maps and queues Ultra tables with input identity,
count, loaded chunk, epoch and cancellation checks; its private radius capture
currently processes ALL eligible items and additionally exact64 arrows. Reuse
the per-input machinery through a bounded service API, not the radius cast.
Do not add the arrow exception to this source ground recipe.

The qualified shooter pool and native Tiny item make this a cohesive next slice.
A shooter-only first release would be a deliberate scope restriction: source
accepts every transformable category. Do not enable unqualified branches to make
the recipe appear complete. Confirm category readiness before choosing scope.

Qualification: actual dropped native Tiny plus shooter stack; one input selected
when several are in range; exact radius endpoints, overlapping exclusion,
Thrower absent, unrelated/unsupported input preservation; deterministic both
30% branches; whole Tiny stack removal on success branch versus retained stack;
whole input-stack conversion, output identity/components; repeated tick/capture
deduplication, no output recapture; no mana debit; observer/arena/time-stop/epoch,
unload/removal and match-stop cleanup; final artifact in two native matches.

Source ambiguity to settle explicitly: `execute as` does not relocate command
origin. Legacy Ultra outputs and closest-player advancement therefore use the
Tiny item's origin, while loot executor is the input item. `Thrower` only gates
eligibility; it does not supply award ownership. Native ownership should follow
the project's player attribution rules; do not silently infer it from the gate.
Also decide whether failed/empty acquisition can consume Tiny; existing native
Reforge's user contract preserves inputs when nothing can produce output.

## Next alternatives

1. Ambient lightning conversion: `items_tick_act.mcfunction:4` ->
   `other/transformpre.mcfunction` -> `other/transform.mcfunction`. Checks live
   lightning within4, gives outputs to closest nonspectator, processes independent
   category flags and awards lightning_smith. Crucially `reduce_count_l.mcfunction`
   shrinks ONE unit without recursion, unlike Ultra's whole-stack recursion.
   Native lightning handling does not implement this ambient acquisition route.
   It needs recipient/owner/no-recipient rules and category qualification; it is
   broader and easier to accidentally duplicate through persistent lightning.
2. Reroll Machine: `other/craft/inventory_craft_check.mcfunction:77`,
   `other/craft/reroll_machine.mcfunction`, `reforge_crafting_0..3.mcfunction`.
   Source checks only four player-crafting slots, processes one unit per matching
   slot, charges5000 times slot count after rolling (no local affordability gate),
   retains machine and spawns ordinary-pool rewards. Native recipe engine has no
   reroll kind. This needs an explicit native crafting/mana interaction contract,
   so is less bounded than the ground route.
3. LuckyWars random/config integration: existing imported nested table references
   are not evidence of a live acquisition owner. `final_luck_modifier/lbc.math`
   is read by Mobgun and temporarily manipulated by checks, with no native match
   selection/reset owner. Treat this as a separate game-mode/config feature.

Boss encounters intentionally excluded from this review.

## Additional lifecycle and consumption findings

Ground entry is scheduled every1s by `tick1s.mcfunction:1,7`, checks outer
`craftusable:1`, some other item at `0.001..1`, and excludes nexus/imprinted.
There is no OnGround predicate despite the ground-crafting name. The Thrower
test selects a limit1 candidate before checking Thrower: another eligible item
with Thrower does not rescue a selected item lacking it. The function selects
again with identical unsorted selector; native code should capture one identity.
The gate precedes conversion and tests data existence only, not an online player.
Source30% kills the whole Tiny entity even for unsupported categories, empty
output or failed advancement; it runs after the complete donor recursion, once
per scheduled invocation. Persistent retained Tiny can convert new neighboring
inputs on later seconds. It must not repeatedly gamble on one pending job.

Ambient lightning runs every unstopped tick through `tick_not_timestopped:1`,
`items_tick`, `items_tick_tag` (transformable and working labels), then
`items_tick_act:4`. `transformpre` first writes Health1000000, then tests ANY live
lightning entity at distance<=4, regardless of caster/weather/ownership. There
is no per-lightning latch, so a surviving lightning can process another unit on
each source tick. The categories are independent; several flags create several
rewards for one consumed unit. Source consumes one unit and attempts advancement
even with no nonspectator recipient or no supported category. These loss cases
conflict with existing native preservation policy and require explicit resolution.
Do not copy the source's Health mutation as a substitute for an acquisition hook;
native typed-item damage protection and lightning processing order must be
verified before damage/removal. Stopped lightning must not slip through a native
hurt path before the acquisition service sees it.

## Suggested bounded shared engine shape (design, not implemented)

Extract exact-item processing from the private radius wand method. A request
captures level, epoch, input entity+stack snapshot, table family, explicit unit
budget (ONE or captured FULL_STACK), output policy (dropped at explicit origin
or delivered to selected player), optional accountable player, and completion
callback with successful unit count. The caller supplies already selected donor;
the engine never expands to nearby inputs. Keep native chunk/stack/count identity,
time-stop and epoch checks, preserved input on empty/unsupported results, bounded
queue work, deduplication and cleanup. Revalidate recipients before consuming.
Wand requests retain current owner-required lifecycle. Ground requests can derive
accountability separately from source Thrower eligibility. Ambient requests must
allow no caster and no fake player: use level+epoch lifecycle, choose the source
nearest eligible recipient explicitly, and do not invent damage ownership.
Grant/award attribution and paused policy belong to the request rather than a
mandatory `Owned(ServerPlayer)` wrapper. An event request must not silently defer
one input forever while retries enqueue more copies. Source Tiny completion
effects/chance should run once per accepted completed donor, with the policy for
zero-success completion settled explicitly before coding.

## Forced random crafting follow-up (user requirement, pending implementation)

User requires a placeholder result, with actual random selection only when taking
the craft and committing ingredients; inspection followed by cancellation must
not reveal or reroll outcomes. Target is both inventory2x2 and workbench3x3,
including30% Tiny consumption. This supersedes using a random visible recipe
preview for this new recipe; earlier ground-source observations remain evidence,
not the target UI contract.

Existing hooks alone are insufficient. `LbcCraftingRecipe.assemble:70` currently
rolls its deterministic named item table during preview; the new random recipe
must instead return a stable, unmistakable placeholder and never call loot/RNG.
`LbcCraftingResultMixin` injects `ResultSlot.onTake` HEAD, suitable for effects but
too late to replace the output. Verified against local26.3 native source JAR:
`CraftingMenu.quickMoveStack` and `InventoryMenu.quickMoveStack` transfer output
to inventory BEFORE onTake (its argument can then be empty). Native
`AbstractContainerMenu.doClick` PICKUP sets carried before onTake; SWAP sets
hotbar/offhand before onTake; THROW uses safeTake before dropping. Therefore do
not mutate the onTake argument or replace result-container contents there.

Safe narrow reuse: add a cancellable server-side result-slot interception before
`AbstractContainerMenu.clicked` native dispatch, restricted to InventoryMenu or
CraftingMenu and a freshly validated random-recipe input. Snapshot exact grid,
menu identity, epoch, player eligibility and destination state; decide admissible
click/destination BEFORE RNG. Then generate complete loot bundle and Tiny chance,
consume inputs and apply outcomes in one synchronous committed transaction,
refresh result and synchronize menu. No callback or packet may reveal the roll
between RNG and commitment. Do not invoke native ResultSlot.onTake afterwards:
it would consume inputs again and recompute recipe remainders.

PICKUP should require empty carried before rolling, put a chosen primary result
there and deliver additional results with a predeclared overflow policy. QUICK_MOVE
must run the same transaction, not native placeholder-based repeat loop; explicit
one craft per click is bounded, while batch semantics need repeated independently
committed transactions. SWAP supports empty target hotbar/offhand and rejects an
occupied incompatible destination before rolling (native ResultSlot mayPlace is
false, so occupied result swaps already fail). THROW requires empty carried and
canDropItems before roll; drop the actual complete output bundle. Block CLONE
and other unsupported result actions so placeholder never becomes an item.
Native PICKUP_ALL excludes result container; QUICK_CRAFT cannot place into a
ResultSlot, but intercepting every random result action still closes bypasses.
Also guard direct quickMoveStack entry if used by checks or another mod.

Capacity must never expose an output then reject consumption based on its random
identity: either reserve capacity for the worst-case complete bundle before RNG,
or commit with deterministic inventory-then-ground overflow after RNG. The latter
fits source dropped rewards but needs acceptance if desired. Qualified shooter
Ultra pool can include Razor seeds as a SECOND stack, so using getFirst or a
single result ItemStack loses rewards. Mixed-category input can multiply outputs.
Guarantee no placeholder reaches carried, inventory, dropped item or creative
clone. Empty/unsupported loot follows explicit preserved-input policy without
revealing a selectable outcome; avoid retaining a cached rejected random roll.

`LbcCraftingRecipe.getRemainingItems:240` currently handles only Rocket remainder.
Do not put Tiny30% RNG into generic remainder calculation: custom commit owns
that once-per-craft decision. Latest parent target is exactly ONE input donor
unit and ONE Tiny unit subject to30% consumption; on the other70% Tiny returns.
Never consume an entire crafting stack. Legacy ground whole-entity removal is
source evidence only. Existing remainder logic subtracts one ingredient
then restores one Rocket donor; the new transaction should calculate final grid
contents explicitly and preserve NBT/components/counts on retained Tiny.

`LbcCursorCraftingMixin` snapshots input/carried around accepted network clicks
and refreshes artifact state after changes; retain it around the new transaction.
It does not enforce atomicity or prevent preview RNG. Recipe placement must only
refresh placeholder. Native result-slot achievements/statistics and sound/particle
effects need explicit once-per-commit handling if bypassing ResultSlot.onTake.
Use broadcast/full sync to correct vanilla-client prediction of the placeholder.

Required adversarial checks: all click types above on both menus; stale menu/state
or changed grid, nonempty cursor/occupied hotbar/full inventory; recipe placement,
close/reopen and input shuffle produce zero rolls; successful take produces one
bundle and one consumption/chance; both Tiny branches, stacked ingredients,
multiple output stacks, stop/time-stop/observer gates and no placeholder leakage.
No implementation or runtime proof is claimed by this design review.

Pre-removal native-path reuse is incomplete by itself: ResultSlot.remove can
intercept PICKUP/THROW, but QUICK_MOVE and SWAP move getItem directly and CLONE
uses safeClone. A getItem hook must remain pure because rendering/sync/preflight
also call it. Randomizing getItem or result matching is therefore unsafe.
Preserving native dispatch would require separate transactional interception of
every transfer branch and a pending commit record spanning early transfer through
onTake, including rollback on failed capacity. That is more fragile than the
narrow result-action transaction above. Single-unit, one craft per click avoids
native quickmove/throw repeat loops and count-dependent placeholder confusion.
