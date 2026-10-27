# Answered Epic / Fragment / Ender and shared melee inputs

27 October 2026. Agent-owned implementation; build, native qualification, integration,
normal acquisition and installation remain root-owned. No build/server/git commands
were run by this agent. Existing protected author paths and live overrides were not
edited.

## Files and frozen integration API

- `LbcPendingSwords(GameController)`; public `damaged(LivingEntity,DamageSource)`,
  `tick()`, `loaded(Entity)`, `unloaded(Entity)`, `forget(ServerPlayer)`, `stop()`.
- Public `beginAttack(ServerPlayer)` / `endAttack(ServerPlayer)` surround native
  `Player.attack` through new `PendingSwordAttackMixin`; root must register mixin.
- Root `LbcItems.pendingSwords()` accessor and accepted-damage/lifecycle dispatch
  are required. Root exposes `LbcEvokerBass.spawnOwned(owner,Vec3)` returning the
  actual native owned fang. Existing lifecycle and native damage hook are reused.
- Root calls `PendingSwordChecks.run(game)` and scheduled `tick(game)` after the
  production service tick. No check manually ticks a player, effect or world.

## Implemented source behavior

All 17 branches are present for current Epic, current Fragment, old Epic and old
Fragment. The two old mappings remain distinct, including flame at old Epic17
versus old Fragment2 and conduit power old Epic3s/amp2 versus Fragment4s/amp1.
Native approved effect caps apply (old regeneration source99 becomes native5).
Current flame retains separate25s/0 and8s/1 layers. Old instant damage uses native
one-tick instant effect; current instantdamage uses8 arrow damage. Native direct
explosions preserve radii2/current and1/old, old local backward.5 origin, source
self protection and current-only slow falling. No instant creeper proxy is used.

Eight world-axis ordinary arrows retain native gravity, default damage and swept
collision, pierce10, original owner, source default DISALLOWED pickup, current
epoch, ground removal and shared bounded1200tick cleanup. Native owner assignment
would otherwise change pickup to ALLOWED, so source default is restored explicitly.
The native fang producer supplies owner, animation and default6 damage. Each cast
owns independent stages at0/5/10 native ticks. Stages2/3 evaluate current owner
feet/rotation and local up.5/forward3,4.5. Sword fangs carry a narrow marker that
prevents this same producer recursively creating new sword activations. Synchronous
AoE/blast production has a local reentrancy guard.

Source particle count, modes, geometry, sounds, pitches, old20block ordinary sound
selector and old million-volume screamer are retained. Current flash red block
marker600 FORCE uses the approved75block maximum audience; source screamer15block
recipient filter remains. All presentation remains native packets, not a claim of
physical client pixels/audio observation.

Ender binds accepted native victims without old HurtTime/range selection. A native
attack window collects actual primary/sweep victims before teleporting any target,
then teleports the owner once. The shared attack wrapper also brackets the
reactive artifact service, flushing it after pending sword movement. Indirect accepted damage outside a native attack
runs its own immediate event window. Source owner-origin feedback, radius10 square,
Nether ceiling120, integer spawnY and independent target/owner searches remain.
No unrelated recently hurt entity is selected. Allied players, technical roles,
Time Stop, source dimensions and native accepted positive/unblocked attribution
remain authoritative. The unusual source Ender anti-magic radius2 is explicit;
using generic suppressedAt here would incorrectly enlarge it to20.

Antimagic binds actual accepted PLAYER victim and calls existing additive
`LbcMana.applyNoMagic(target,15)`. Enhanced flint binds actual accepted #mobs victim
and passes source owner into native raid_omen400/111 transport, reusing Kiuas owned
curse service. Neither retains old selection radius7 or duplicates status logic.
The ancillary source Antimagic task checks held flag every20ticks, first actual
armor stand tag=mom within4, and read-only lbcID2 inequality, including missing
scores. Native mom producer equivalence is not established: LbcStand itself did
not expose source mom/lbcID2 strings in the selective audit.

## Exact new ported leaves

`epic_sword`, `legacy_epic_sword`, `fragment_sword`, `legacy_fragment_sword`,
`ender_sword`, `legacy_ender_sword`, `antimagic_sword`, `antimagic_sword_loli`,
`enhanced_flint` under `data/lbc/loot_table/ported`.

Fragment leaves contain all17 original source entries. All9 leaves are adapted
using tools/import_game_resources.py migrate_loot_schema for native26.3 codecs;
source function/function-array keys become native type/modifier keys without
rewriting gameplay or presentation values.
Other leaves wrap the unique matching original entry without rewriting any payload
components or source entry weight/quality. Antimagic ultra tier_s and loli tier_a
remain distinct. Enhanced flint keeps vanilla model with no invented override.
Root owns whitelist, availability and actual normal producer proofs. Importing
these scoped leaves alone is not proof of whole loli/IMBA/legacy pool completion.
Current Epic/Ender/Fragment already occur in ordinary source sword reforge pool.

## Native check coverage and limits

`PendingSwordChecks` includes68 real accepted-hit branch dispatches with scoped
Epic-only17way RNG control; distinct carriers, max durability, models and old
unbreakable; source self-effect outcomes/old water distinctions; eight owned
arrows; native queues; flash packet mode/count. It checks red600 FORCE audience
74.9/75.1 and old screamer million volume/pitch at14 versus16/76. It drives actual
`Player.attack` with primary and sweep Cow targets, records unchanged owner pose
inside all damage callbacks, checks one owner teleport packet, and retains an
unrelated HurtTime10 witness. Actual accepted14block antimagic/omen damage proves
old radius7 is gone; a same-team control protects allied-player custom input.
Two independent casts are then observed through genuine network scheduling at
5/10ticks after moving/turning owner. Latest emitted teleport ACK and loaded-player
readiness are carried forward from PassiveConsumableChecks. Fixture score restore
uses writable native scoreboard entries, including readonly criterion scores.

Checks are authored, not executed by this agent. Root must report qualification
results and reject/fix fixture assumptions if necessary. Normal reforge acquisition
proofs, Looper recipient lore, advancement producers and integration are root-owned.

Teleport uses existing resident finite per-entity `LbcSurfaceTeleport.destination`.
This preserves native safe support/two-air/ceiling and chunk residency but does not
claim exact legacy continuous random distribution,10000 attempts, or all-or-nothing
multi-entity spreadplayers assignment. It may partially succeed when only some
resident safe columns can be found. This limitation was explicitly reported to
root instead of silently representing it as legacy runtime observation.
