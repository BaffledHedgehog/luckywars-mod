# Magic Bean native port — 29 October 2026

Current root status: bounded native checks pass in candidate8b38434f. The actual
source leaf is now packaged with exact native codec-key adaptation. See
[integration/proof and explicit limits](CHAOS_CHAIN_INTEGRATION_2026_10_29.md).
Full transitive reward readiness remains gated. The subtask report below records
its source contract and original handoff; later root proof supersedes its
then-unrun status.

Bounded implementation of the original Magic Bean growth chain in new
`src/main/java/me/skywars2/game/LbcMagicBean.java`. No build, server run,
deployment, commit or native qualification was performed for this subtask.
The complete original LuckyBlock/Chaos pool remains gated on its entire
dependency chain; a working lazy chest alone does not qualify that pool.

Source root:
`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/`.
Read-only Paper comparison:
`E:/servers_mew/cringe1_21_4_skywars/plugins/luckywars/src/main/java/me/luckywars/swrg/SwrgNativeService.java`.
No source-server or resource-pack files were modified.

## Exact source closure

`lbc/loot_table/drops/magic_bean.json` gives one repeating command block with
raw `magicbean:1,drop:1`, `lbc:drop_not_trap` model, tierU, yellow nonitalic
translated `drop_bean` name and white nonitalic `trap_desc` lore. Its consumers
do not require a special Java item identity. The current service recognizes
raw `magicbean` exactly1, so source normal and IMBA carriers retain the same
growth behavior without a fabricated IMBA growth variant.

Original `lbc/function/drops/specialdrops.mcfunction:46` invokes
`drops/magicbean`. Normal `barriersnottraps.json` references the Magic Bean leaf;
the IMBA table contains the same flag plus `imba:1`. Source
`other/baffledend/7.mcfunction:3` also drops the leaf at up to15 nonspectator
entities. The new service does not change these acquisition tables or enable
unrelated rewards.

The complete direct function chain is:

1. `lbc/function/drops/magicbean.mcfunction`: summon one marker at the exact
   token position, tags `magicbeaner,magic`; schedule `magicbean_loop` one tick
   later.
2. `drops/magicbean_loop.mcfunction`: execute `drops/bean` as/at every bean
   marker; schedule the same loop one tick later while any remains.
3. `drops/bean.mcfunction`: fill the inclusive3×3 horizontal layer at the current
   position with default `minecraft:oak_wood`, strict. Read each coordinate with
   native integer command-result semantics. Independently roll five50-percent
   predicates for X+1, X-1, Z+1, Z-1 and Y+1, in that order; write the resulting
   integer position back to the marker. X/Z therefore each have -1,0,+1 outcomes
   with probabilities.25,.5,.25; Y moves0 or1. No private speed, height progress
   counter, age limit or invented time-based termination exists.
4. Test `lbc/predicate/chance0_8.json`, whose literal probability is **0.008**.
   The random terminal helper executes at the marker's new position. A separate
   scoreboard condition `#pos_y >=300` invokes the same helper without a new
   `at @s`; it retains the function's original execution position.
5. `drops/beanend.mcfunction`: perform the five strict glass fills below, place
   one native chest at Y+1 with lazy `LootTable:"lbc:lucky_items_1"`, then kill
   the marker. There are no source structures, spawned mobs, combat/death
   rewards, sound commands or particle commands in this chain.

Exact endpoint rectangles, all at the selected endpoint Y and all inclusive:

| X extent | Z extent | Native block/update behavior |
| --- | --- | --- |
| -8..8 | -8..8 |white_stained_glass, strict |
| -6..6 | -10..10 |white_stained_glass, strict |
| -4..4 | -12..12 |white_stained_glass, strict |
| -10..10 | -6..6 |white_stained_glass, strict |
| -12..12 | -4..4 |white_stained_glass, strict |

The source writes oak wood with default Y axis, not stripped logs, vines,
leaves, a generated tree or a structure template. Glass rectangles overwrite
all original states; they are not an air-only replacement or a filled square
of radius12. Each source fill retains its own native block-modification rule
check. A failed fill does not prevent subsequent source commands.

## Native implementation and integration contract

`LbcMagicBean` owns only its native marker handles and their queued growth work.
Marker position remains the growth position. Shared `LbcStonekivi.Work` admits
at most one growth step per actual server tick; its clock guard uses
`MinecraftServer.getTickCount`, never a synthesized counter or elapsed-time
catch-up. The first growth is deferred to the next actual server tick after
publication. Root integration calls Bean tick before the shared stonekivi tick
so a ready step has the source one-tick scheduling opportunity.

`LbcDropBlocks.fill` performs the actual strict source layers/rectangles. Native
integer BlockPos floor semantics match `/data get` coordinate result rounding,
including negative/fractional spawn coordinates. Five real native random-source
draws preserve the source move distribution; a sixth draw tests.008. There is
no custom RNG or fixture override in production.

The shared `LbcSpecialDrops.placeLootChest(ServerLevel,BlockPos,ResourceKey<LootTable>)`
bridge retains the already approved chest replacement behavior: unpack/eject
only the replaced container, preserve its neighbor's inventory, reset the
replaced chest's metadata, and use native default-state BlockInput placement
with flags258 and neighbor updates. The new service supplies the exact original
`lbc:lucky_items_1` resource key and leaves contents lazy. It never rolls loot
early, substitutes a smaller table, preselects a reward or invents a fallback.

All unfiltered wood/glass/chest placement retains WILD terrain causality through
the existing `LbcIndirectKills` world-change owner. Wearer UUID and the shared
causal-order stamp are stored in owned marker tags, so unload/reload retains
the original author even when that author is offline at reload. Ownerless tokens
remain ownerless; they do not borrow another player's identity. No separate
damage engine, blast carrier, magical effect ledger or entity target scan is
introduced.

Public integration API:

- Constructor `LbcMagicBean(GameController)`.
- Static `recognized(ItemStack)` and `managed(Entity)`.
- `ready(ItemEntity)` and `spawn(ItemEntity)`: spawn one marker without any token
  debit. Shared SpecialDrops retains the one-debit rule and original branch
  position after bees/before bullets.
- `loaded(Entity)`, `unloaded(Entity)`, `tick()`, `stop()`, `owner(Entity)`.
- `count()`, `steps()`, `completions()` and package-local `snapshot()` permit
  actual native checks without rewriting private clocks. `completions` counts
  executed endpoint helpers, which can be two for the simultaneous source case.

Root owns Items getter/lifecycle, common SpecialDrops dispatch, chest bridge and
resource packaging. This subtask modifies none of those files.

## Admission, pause, suppression and cleanup

Admission requires the active match dimension, ordinary dimension policy,
current/unmarked compatible token epoch, nonending game and resident initial
layer. This complements the existing common drop-role/ground/age admission;
the service does not invent a second token debit or require a player owner.

A source marker is explicitly magical. The existing shared magical-entity
consumer can delete it; the service also checks shared MAG suppression at the
marker before work. MAG cancels future growth without deleting earlier terrain
or clearing another owner's effects. No TECH suppression is added.

Global Time Stop and an actually frozen marker pause work without consuming RNG
or advancing growth. This follows current project Time Stop policy; the legacy
raw scheduled function itself had no explicit `time_stopped` predicate. Released
physical wood/glass/chest remain native world blocks.

The shared queue requires the complete possible next terminal footprint to be
resident (current X/Z±13), covering the move of at most one block and endpoint
extent12. It waits without forcing chunks. Unload detaches the marker and
cancels its pending work; same-epoch reload reconstructs ownership from native
tags/position. Stale epoch, match stop and ending cancel quietly. No unloaded
entity handle is retained, no canceled work can later place a chest, and no
already placed source terrain is reverted on cancellation.

## Pending source questions, recorded without waking the author

These discrepancies are material source facts, not silently approved repairs:

1. Paper `startMagicBeanGrowth` at6341 uses `random.nextDouble()<0.8` whereas
   datapack `chance0_8.json` is.008. Paper terminates approximately100times more
   frequently per step. This task explicitly named the original datapack chain;
   the implementation therefore preserves its.008. An eventual author decision
   to prefer Paper's.8 would be an explicit behavior change, not a correction
   inferred from the predicate name.
2. Paper floors the initial token location immediately and finishes at its moved
   location for both terminal conditions. Datapack retains the initial fractional
   location until its first coordinate writes; the height branch lacks `at @s`
   and therefore uses the pre-movement function position. The implementation
   preserves those literal datapack positions. If random termination and Y>=300
   both match, both original helper invocations are retained, including possible
   two overlapping platforms/chests. A cleaner single endpoint requires an
   explicit author decision.

The ordinary Paper entry `runNativeMagicBeanDrop` at5136 simply starts growth at
the token block location and consumes one token. Its scheduler at6341–6380 and
endpoint helper at6382–6389 are comparison evidence, not authority to replace
the explicitly requested datapack behavior. No new question was sent to the
sleeping author for this bounded task.

## Remaining qualification and release boundary

Required checks include actual SpecialDrops one-debit publication, next-real-
tick growth, fractional/negative coordinates, five independent random moves,
the literal.008 distribution, Y>=300 endpoint coordinates, simultaneous endpoint
commands, full glass footprint and lazy chest source table, replaced-container
contents, native Time Stop/MAG/TECH behavior, unload/reload, stale epoch and
quiet stop. These checks have not been run in this subtask.

The only transitive reward dependency is the complete native
`lbc:lucky_items_1` loot chain and all mechanics of its reachable outputs. Its
full acquisition/behavior closure and broad-pool release gate remain with the
parent task. No success statement here certifies those unresolved outputs.
