# BedWars3 protection integration audit ? 4 November 2026

Read-only bounded audit. Owned output: this note and ignored
`build/verification/bedwars-protection/native-write-boundaries.txt`. No production
edit, build, server operation, commit, RP/template mutation or user question.
Repository/agent_notes instructions, WORKFLOW, RESOURCE_PACK and
[NIGHT_BEDWARS_SOURCE_2026_11_04.md](NIGHT_BEDWARS_SOURCE_2026_11_04.md) were read.
Mode3 remains gated. This is an implementation map, not qualification or approval.

## Smallest honest shared boundary

Use one match-owned **coordinate -> original Block type** policy, scoped by actual
arena dimension and geometry, with one ordinary mutation predicate and narrowly
issued typed transaction permits. Reuse `ArenaBlockMixin`, `MapManager`, native
mutation transactions and the existing full coordinator. A new private terrain
rollback, parallel world scan, function runner or alternate protection engine is
unnecessary. However, **Level.setBlock cancellation alone is incomplete**: several
native callers publish loot/entities, mutate inventories or process paired cells
before/after that call without respecting its Boolean result.

Exact current shared point: `src/main/java/me/skywars2/mixin/ArenaBlockMixin.java`
HEAD/RETURN of `Level.setBlock(BlockPos,BlockState,int,int)`. HEAD currently calls
`LbcIndirectKills.beforeBlockChange` and `MapManager.noteMapBlockWrite`; RETURN
calls attribution, bounds journal, MAG, bedrock, Tanner and Kiuas observers only
on successful writes. Put denial BEFORE all these HEAD observers. A rejected
write must return false and must not create support-removal credit, map-overlay
ownership or topology notifications. Notify the BedWars index after actual
successful changes; do not use an attempted write as bed-loss evidence.

Native26.3 `Level.setBlock` delegates to `LevelChunk.setBlockState`; `removeBlock`
and `setBlockAndUpdate` ultimately enter this common route. Existing ordinary
native abilities, `BlockAreaWork.step`, `EffectTerrain`, MapRotation live commits,
map clear/paste and coordinator terrain replay use it. MapRotation's direct
`LevelChunkSection.setBlockState` calls prepare detached destination copies;
they do NOT publish live terrain. Do not intercept detached palette writes as
if they were arena edits.

A lower `LevelChunk.setBlockState` guard can cover **direct live chunk writes**
that bypass Level (return null on rejection, matching native no-change contract),
using the SAME predicate. Current production direct live exception found:
`LbcTimeStop.releaseContact` restores its retained chunk when no longer resident.
Its other section writes sanitize detached persisted chunk copies, not live arena
blocks. No production direct live section mutation was found in the bounded
`setBlockState/getSection/getSections` search. Do not claim this covers arbitrary
future code or world generation. Section replacement and arbitrary BE edits do
not pass either hook; keep live bulk publication through reviewed shared owners.

## Finite native transaction hooks required in addition

Native method bodies below were read from the current local26.3 sources JAR,
not guessed from older mappings. Exact numbered excerpts and source/entry SHA256s
are in the ignored evidence file. These are shared native operations, not one
patch per custom ability.

|Boundary|Why late setBlock rejection is insufficient|Necessary integration|
|---|---|---|
|`Level.destroyBlock(pos,dropResources,breaker,limit)`|`Block.dropResources` runs BEFORE setBlock (Level.java276?294)|Reject protected destructive attempt before particles/drop/XP callbacks; use the same ordinary final-type predicate.|
|`ServerPlayerGameMode.destroyBlock(pos)`|`block.playerWillDestroy` runs before removeBlock; tool mining runs afterwards|Preflight at entry before native block callbacks; route enemy **registered arena bed** to one typed two-part transaction; send native block resync on denial. Existing CreativeBlockDropsMixin already hooks this method.|
|`BlockBehaviour.BlockStateBase.onExplosionHit` / native explosion selected-block dispatch|Base onExplosionHit computes loot/XP before AIR and calls wasExploded after it|Filter protected selected cells before per-block callback, including subclass TNT reactions; Level guard remains final backstop. Existing `HazardPropagationMixins.Blast` already wraps explosion phase for attribution/entity harm.|
|`PistonBaseBlock.moveBlocks` after resolver, BEFORE any movement/destruction|Drops toDestroy first, then installs moving states and MovingBlockEntities even if individual writes reject|Preflight whole resolved source/destination/destroy set including arm/head. Abort transaction if any protected type would change. Do not leave half a moving piston or drop an immovable original.|
|`FallingBlockEntity.fall` / `FallingBlock.tick` launch boundary|fall calls setBlockAndUpdate then UNCONDITIONALLY addFreshEntity (94?104)|Prevent conversion of protected source before entity publication. This includes source retained while a duplicate falling body appears.|
|`FlowingFluid.spreadTo`|beforeDestroyingBlock runs before replacement; waterlogging uses separate container branch|Check final fluid/block type before destruction/drop hook. Same-block waterlogging remains permitted by type-only semantics.|
|`FireBlock.checkBurnOut` / TNT prime path|Fire attempts remove/replace then primes old TNT regardless rejection|Prevent protected burn transaction before TNT child side effects. Similar direct TNT ignition must honor the same source-removal check.|
|`MapManager.restoreSnapshot(target,pos,image,flags)`|Clears old Clearable BEFORE setBlock, then loads BE NBT regardless result|Preflight final type BEFORE clear/load. Distinguish policy rejection from native identical-state false; intentional same-type NBT restore must still work.|
|`StructureTemplate.placeInWorld` / native CloneCommands transaction|BE cells use temporary BARRIER; Clone MOVE clears source before destination and later loads BE NBT|Preflight final cell types; authorize only the temporary state sequence for admitted exact cells. Reject an atomic MOVE before clearing either side if destination/source protection disallows it.|

The list proves why an assertion that one HEAD mixin protects every consequence
would be false. The current native listener-exception behavior must be selected
before deciding plant/fluid destructive preflights. The additional entry hooks
should query the common owner; do not maintain per-hook shadow snapshots.

`LbcIslandDrops.spawn` actually executes native clone/fill commands and has an
existing author rule: failed copy leaves BOTH terrain regions intact, does its
entity teleports and consumes token. A partial protected CloneCommands success
must not be reported as full copy and then clear source. Reuse command-level
preflight / existing copy-admission boundary, with no new item-specific rule.
`LbcNexusClone` MOVE uses `MapManager.restoreSnapshot` destination then source AIR
and currently returns no publication result; denied destination cannot be followed
by source deletion. This is the shared chunk-movement boundary used by Supernova,
not permission to silently classify all Supernova work as bypassed.

## Baseline, bypass and coordinator ownership

`MapManager.finishPlacement` currently seals lighting, gathers entities only in
pendingBounds, reduces spawn marker data to `Spawn(position,yaw,pitch)`, combines
mapBounds with placementExternalBounds and then completes admission. This is the
right finalization boundary for one typed BedWars marker index and baseline.
Build pending final type entries incrementally from completed native placement
slices (including overlaps and air erasures), then finalize BEFORE mode3 activates.
Do not freeze temporary template barriers or lobby-overlay visible states as the
map baseline. `placeMapSlice`/`noteMapBlockWrite` already distinguish map-under-lobby
writes; `mapUnderLobby` holds actual map cells. `recordBlockChange` is a coarse
cleanup/recovery bounds journal, NOT original terrain contents, and deliberately
ignores loader writes. Do not repurpose it as the protection snapshot.

Original start order is teaminit -> remove empty-team beds -> construct spawn
beacons -> `/bedwars init`. Therefore the final source-equivalent baseline includes
new diamond bases, beacons and colored glass/solid-tag column replacements, and
excludes deleted empty-team beds. Stage these changes under setup ownership, then
activate the final baseline. Do not accidentally protect only pre-beacon template
terrain. Column source selection remains exact solid tag up to319, not any nonair.

Proposed typed interfaces (names illustrative; not implemented):

- `allowsType(level,pos,wantedBlock)` is a constant-time read of current active
  arena baseline; source absence means originally-air/unprotected. Compare Block
  identity, not BlockState, NBT, collision or hardness. Exemption choice is explicit.
- `preflightCells(level, finalChanges)` validates a finite native operation against
  the same predicate, returning a plan/permit or rejection before publication.
  Temporary BARRIER states are legal only for that admitted cell transaction.
- Private-owner-issued lexical permits for `MAP_PLACEMENT`, `MAP_RESET`,
  `COORDINATOR_REPLAY`, `BED_BREAK`, `SPAWN_BEACON_SETUP/REMOVE` and TimeStop's
  **owned temporary air contact**. Bind permit to epoch, level and exact footprint;
  close per synchronous server-thread step. Work queued for later ticks reacquires
  its own typed permission. No global `ignoreProtection`, public string tag,
  update-flags privilege or blanket `maps.busy()/vanity.busy()` bypass.
- Bed break resolves actual registered bed/team/spawn, validates both current bed
  parts and enemy participant, removes those two baseline entries and owned beacon
  entry, then performs narrow no-drop edits once. Source listener explicitly
  forgets the beacon as well as both parts. Empty-team cleanup uses setup ownership.
  Ordinary player-placed beds elsewhere do not become team authority.
- The BedWars lifecycle owner joins existing full coordinator with typed baseline,
  removed entries, topology/team bed entitlement and pending operations. Prepare
  gates gameplay, canonical terrain/body restore installs with replay permission,
  then private typed index links install and publish. Runtime rebind/reload must
  restore the SAME owner; no independent snapshots or replay timer.

`MatchTerrainCheckpoint.Restore.step/finishChunk` restores changed block values,
same-state BE inventories/components, pending block entities, schedules/lighting
and beacon Levels. Replay permission is necessary even when its saved baseline
currently differs (e.g. rewind to before/after a bed loss). Its direct
`chunk.addAndRegisterBlockEntity` is legitimate canonical replay, not ordinary
ability publication. Current global freeze is necessary but is not authorization
for arbitrary scripts to edit protected blocks.

## Same-type state / storage semantics

Source region snapshot stores Material only. Door/open, bed occupancy, facing,
waterlogging, crop age and chest content/NBT edits do not inherently change type.
A stronger immutable BE/inventory/state policy is NOT authorized by that source.
`LbcExpansionSkill.normalize` deliberately removes source container Items through
same-type snapshot restore before copying; type protection alone does not forbid
that. Likewise ordinary chest access, hopper transfer and native block-entity
loading cannot be globally cancelled merely because the block is original.

Conversely, when final type change is DENIED, MapManager.clearContent or template
barrier/clone NBT callbacks must not still alter a protected container. This is a
transaction integrity requirement, not stronger same-type protection. Same-type
copying may legitimately overwrite NBT under source semantics; retain it unless
root records a separate author decision. TeamChest's virtual shared54slot storage
requires its own coordinate/team inventory lifecycle owner; protecting its physical
ender-chest block type does not preserve virtual contents or authorize a duplicate
storage association. Coordinate hooks must retire associations exactly once on
accepted replacement/reset and restore them through existing full replay.

## Boba, Hagalaz, Supernova and rune resets

**Boba:** current `LbcBoba.use` already branches gameMode>=3 to player-only effects
through `wands.target`, including player teleport/sounds/loot/particles. It never
requests map rotation for mode3/4. No new Boba map-protection question is needed.
`MapManager.rotateMap` itself has no mode3 guard and MapRotation rotates every
selected entity including shops/markers, so future alternate consumers cannot
use it to evade mode policy. Mode1 rotation live writes remain Level-backed;
its native region physics and spawn/pristine references have existing ownership.
Do not change that qualified behavior merely to port BedWars.

**Hagalaz:** `LbcImbaRunes.use -> LbcRuneGlobals.clearMap` has no BedWars-specific
branch. Shared queued `BlockAreaWork` sweeps -192..192,y0..319, so a type gate
preserves protected original blocks while permitting deletion of player additions.
That would leave protected map content while #mapstat is set1 and the existing
six-pass entity cleanup runs. Author has already ruled permanent Chef removal for
Hagalaz; do not reopen/generalize it. `clearEntities` preserves strict-only shops,
BUT `maploader || !lobby` wins: a shop with an additional swrg.spawn/map_object/etc.
maploader tag is still killed. Durable shop role must override this destructive
classification during gameplay. Mode3 baseline/bed index must not be cleared just
because rune code assumes the map was destroyed. Whether Hagalaz should keep the
source protection-limited map result versus receive a specific wider BedWars
behavior is a meaningful unresolved item/mode policy; no exemption inferred here.

**Supernova:** [existing binding ruling](SUPERNOVA_CHAIN_PORT_2026_11_03.md)
ALREADY authorizes same-match map replacement selecting current-mode catalog,
including local maps, retaining participant/inventory/XP/lives, no regrant or
eliminated revival, returning Nexus players and leaving Imprinted untouched.
Do not ask whether replacement itself is permitted. `prepareSupernovaMap` already
selects `renewalCandidates(...,maps.bedwars(config.mapId))`; controller replacement
uses MapManager's existing lifecycle. BedWars must retire OLD topology/protection
and build the NEW baseline/index at that authorized replacement, then rebind
participant team spawns. Narrow unresolved question: do previously lost team beds
stay lost on the new map, or are replacement-map beds renewed? How to represent a
participating team whose replacement map has no corresponding valid bed/spawn
must be resolved at renewal admission, not by reverting to solo capacity or
regranting kits. Earlier running Supernova source chunk MOVE and final reset are
different transactions; root must deliberately classify protection interaction,
not issue a generic ability bypass. Existing permanent final Chef purge remains.

**Rune restart:** `LbcRuneGlobals.restart -> GameController.restartFromRune` uses
existing stopMatch and runeRestartPending. Retire current protected owner at the
actual match stop, and allow the manager's authorized reset/next placement. Do not
leave prior epoch original cells protecting the next map. This differs from the
approved same-match Supernova renewal, whose participant continuity is retained.

## Shop role and harmful source / center audit

Standing author requirement: shop cannot be **affected, targeted or used as an
ability source/center**. Existing Bukkit ShopMobIgnoreListener is narrower: only
Villager+bedwars_shop; cancels mob targeting, Monster/lightning damage, lightning
combust and villager-to-witch lightning transform. It does not authorize allowing
all other damage, resource changes or source-center selection.

Current native reuse points:

- `DeveloperTools.MarkerKind.SHOP` creates persistent NoAI permanently invulnerable
  Villager, high health, bedwars_shop + strict_map_object. Imported/native entity
  NBT and MapAuthoring retain tags. `MapManager.keepLobbyEntity` preserves strict
  markers and shop developer markers. These are persistence hints, not complete
  runtime role protection. Establish role from placed topology/entity identity,
  not translated name, team, invulnerable flag or present custom tags alone.
- `LbcWands.validRoleTarget` excludes strict_map_object alongside observer,
  rcdenyalways, magic_academy etc. Shared target/projectileTarget/homing and most
  explicit role-aware areas reuse it. `LbcSpellSource.target/canAffect` also call
  it and retain item-specific exclusions. CloneSkill.baseEligible calls it, so
  strict shops are already excluded from ordinary x2/x4 clone proximity.
- `SkyWars2.ServerLivingEntityEvents.ALLOW_DAMAGE -> LbcItems.allowDamage` and
  native projectile canHit dispatch are central harm entry points. They handle
  many item owners and author-approved WILD exceptions. They do NOT presently
  guarantee an unconditional protected-shop veto: `LbcWands.allowDamage` reaches
  true for an otherwise ordinary nonprojectile source, and `LbcDamage.allowDamage`
  has no general shop check. Permanent invulnerability does not stop effect hooks,
  direct kill/discard, transformation or being selected as an attack center.
- `LbcColorEffectsMixin.addEffect`, LbcInstantEffectMixin and
  `LbcIndirectKills.allowProjectileEffect` cover potion/cloud effects and causal
  status ownership. The latter deliberately allows beneficial effects and most
  non-potion sources; that is insufficient for "shop cannot be affected". The
  role veto belongs BEFORE item-specific payload handlers, without suppressing
  canonical replay/load or ordinary player beneficial effects.
- `WandProjectileMixin.BlazeTarget` hooks Mob.setTarget but currently corrects
  known summons/Blaze/trial policy only. A general protected-shop target veto is
  missing for ordinary native hostile AI. Native lightning transformation,
  entity pushes/teleports, trading/breeding/spawn-egg offspring and direct removal
  need the durable role checked at their appropriate shared operation boundaries.
  Current UseEntityCallback prioritizes acquisition/developer/ability handlers
  and otherwise passes to vanilla; no BedWars shop interaction owner exists yet.
- `LbcSpellSource.hostile` constructs policy without validating emitter role;
  bind/scope similarly are causal owners, not a source-center permission system.
  Many selectors correctly exclude strict targets, and LbcWands trace validity
  checks a native NPC source in its ordinary path, but sourcePolicy paths use the
  explicit emitter/accepted policy graph. A blanket claim that protected targets
  can never become an emitter/center is therefore unproven. Check role BEFORE
  selecting/publishing such a producer and AGAIN when a retained entity source
  changes identity/role; keep accepted author/epoch attribution canonical.

Smallest coherent role interface is a shared durable `protectedShop(entity)` plus
`canTarget/canAffect/canEmitFrom` entry predicates in the EXISTING role/target/
publication owners, backed by the same placed marker index. A shop-centered
ability is denied because that entity was selected as source, not because any
unrelated world effect happens to pass near the shop. A generic Entity.remove
cancellation is not sufficient: transformations may already debit/create a new
body; chunk unload, dimension change and canonical reset/replay are legitimate
lifecycle removals. Do not install an unreviewed all-setter invulnerability engine.
Audit finite transformation/direct-erase/resource owners admitted in mode3 and
keep admission gated until their early role veto is qualified.

Ally protection remains separate: ordinary `wands.target` excludes allied entities,
while projectileTarget's default team test specifically concerns ServerPlayer
and physical projectiles / WILD sources carry established exceptions. Existing
ants and conduction author rulings must survive. Do not equate shop immunity
with a new blanket friendly-fire rule or treat every Source as permission to
ignore team policy. Resource/velocity/area effect payloads still require their
actual item allegiance contract, including summons/team zombies.

## Source disagreements and root backlog (no new approval inferred)

1. Bukkit `ProtectionListener.isExempt` ignores original OR new water/lava/
   powdersnow/flowers/grass/fern; Ignite BedwarsHooks has **no exemption list** and
   rejects every new Material != saved Material. Do not claim plants/fluid escape
   both backends. Select effective native policy explicitly; same-type state/NBT
   permissiveness is common to the low-level rule, not an inventory lock.
2. Source BedwarsRegionManager.initAt captures first selected world centered on
   command's0,0?384 inclusive, full height. Actual native imported/local/shifted
   arena geometry requires its own dimension/bounds. Coarse cleanup envelopes,
   lobby bounds, pristine source bounds and active BedWars topology are distinct.
   Record adaptation rather than silently protecting the historical empty square
   while leaving a shifted saved map unprotected.
3. No fresh permission is needed for Boba's existing mode>=3 player branch or
   approved Supernova replacement. Ask only the BedWars-specific retained-vs-renewed
   bed entitlement and missing-team mapping questions, plus any unresolved source
   MOVE interaction with protected terrain.
4. Hagalaz/other authorized map-destructive abilities versus protected originals:
   late denial can leave entity/map-status side effects without actual map clear.
   Root should record concrete per-item policy instead of an all-abilities bypass.
5. Same-type container/source inventory mutation is permitted by original type
   model; stronger original-chest/BE locks need their own ruling. Denied type edits
   must have zero inventory/drop/entity/attribution side effects regardless.

Release proof should cover actual player enemy/own bed halves and nearest owned
beacon, ordinary added blocks, same-type properties/NBT, plants/fluids under chosen
policy, native explosion drops, piston push/destroy/destination, falling launch,
water/fire/TNT reactions, command clone MOVE and same-BE copy, denied typed snapshot
restore, source attribution unchanged after rejection, local shifted geometry,
TimeStop contacts/unload, full coordinator before/after bed loss, approved renewal
retargeting, and shop targeting/effect/source-center/transform/direct-delete/clone
and reload cases. This audit executed none of those runtime checks.
