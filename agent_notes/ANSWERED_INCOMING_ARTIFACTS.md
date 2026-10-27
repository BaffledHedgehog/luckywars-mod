# Answered incoming artifacts and Sand Amulet — implementation handoff

27 October 2026. Implements approved choices 13, 15 and 16 against
[complete incoming source](INCOMING_ARTIFACT_SOURCE_2026_10_27.md) and
[complete Sand source](REACTIVE_INVENTORY_SOURCE_2026_10_27.md).
This child changed only its three new Java files, fourteen new isolated loot
leaves and this note. No builds, servers, git operations, original source/RP,
protected author paths or shared integration files were changed by this child.

## Fixed integration API

`LbcIncomingArtifacts(GameController)`; accessor `game.items().incomingArtifacts()`.
`damaged(LivingEntity, DamageSource)` belongs in the existing accepted positive,
unblocked outgoing path. `tick()`, `stop()`, `forget(ServerPlayer)`,
`loaded(Entity)`, `unloaded(Entity)` use existing item lifecycle points.
`owner(Entity)`, `canAffect(Entity, Entity)`, `explosionTarget(Entity, Entity)`
and `inheritChild(Entity, Entity)` belong in the shared owner, native damage,
explosion impulse and native TNT-chain boundaries. Native TNT/reflection may
transfer present authority. The service resolves that before its recorded owner.

Register mixins `IncomingArtifactCriterionMixin`,
`IncomingArtifactCriterionMixin$HurtAttempt`,
`IncomingArtifactCriterionMixin$SandPhysics`,
`IncomingArtifactCriterionMixin$WolfTarget`.
`IncomingArtifactChecks.run(GameController)` and `.tick(GameController)` use the
suite key `incoming-artifacts`; root owns the runner/registration and execution.

## Criterion and native effects

The exact native class is `net.minecraft.advancements.triggers.EntityHurtPlayerTrigger`.
Its `trigger(ServerPlayer, DamageSource, float originalDamage, float actualDamage,
boolean blocked)` HEAD calls the service before advancement listener dispatch.
The actual native `LivingEntity.hurtServer` call at source line1273 follows native
death handling and rejects cooldown damage earlier. There is no HurtTime scan
or positive/HP-loss condition. The wrapping native hurt invocation carries
pre-death admission and inventory masks so match death cleanup cannot erase a
fatal event's original artifact eligibility. It emits no event on its own.

Current type1 has the separate `minecraft:attacks` source-entity gate, native
ResistanceV40, both forced barrier markers and original sound. Current2..6 keep
the narrow container outer gate, exact Inventory types, original order and
beacon-only suppression. Type2 is actual vanilla PrimedTnt fuse0/default power4;
source Resistance remains the native damage rule, including self admission.
Type3 places the actual existing1331-cell air11x11 template with native
BlockRotProcessor(.5), flags2, and exactly native PlaceCommand seed0 random
creation. Weakness keeps self/friendly mobs and normal allied-player/technical/
Greed protection. Speed evaluates every original effect-presence line in order.
Wolf uses direct native owner, empty loot, red collar and200/.5/5 base attributes,
without a made-up duration, target or health override. Native role and owner
target admission remains authoritative.

All six old effects retain their original amplifiers, hidden flags and duration.
Old HarmI is an added native one-tick instant effect; there is no blanket proc
or recursion guard. Its future native effect tick and accepted hurt cooldown
decide whether the real criterion reenters. Existing native effect attribution
receives owner as grant source, as for the existing Devil Sword self-harm path.
Effects survive inventory removal and expire through the native clock.

## Sand behavior and choice13

The accepted event's actual victim within10 replaces the source current HurtTime
presence guard; no additional nearby recent-hit scan was added. Both current/old
inventory predicates remain distinct. Current source random1..3,14 falling
blocks/downward motion at ownerY+6, both strict native wall fills, task award and
all near/distant cage branches are implemented. Old has its own native16-block
pitched feet formation, unchanged zero initial motion and seven ranged sounds.
The current-only inventory passive checks container0..35 and weapon hands,
refreshes hidden ResistanceI40 on the scheduled20-tick phase with five forced
sand dust particles, and pauses under shared Time Stop. Armor/cursor/crafting
copies do not activate it; old amulet has no passive. Input stun/ice does not
disable the carried passive. Removing the item retains the last native grant.

The missing old marker routing is repaired as an owner/epoch-scoped delayed
column task. Lifetime210 reaches100 after110 unpaused service ticks. It records
successful own four-cell red-sand writes and support. Delayed cleanup DESTROY
clears only unchanged recorded red-sand cells; it never uses absoluteY0 or the
whole world column and preserves a later foreign block replacement. Original
support is retained. Column expiration and native volley landings add the
approved4 falling-block-type shard damage in spherical radius1. Native falling
sand collision still owns its source capped5 damage, placement and lifespan.
All physical sand tick writes and terrain tasks use existing default-policy
trap/support attribution; terrain operations run through the shared Stonekivi
work queue and wait for resident chunks. Forget/stop cancel tasks/actors; placed
source terrain follows ordinary arena cleanup rather than invented restoration.

## Source leaves and acquisition boundary

Fourteen isolated source-exact leaves are packaged at `data/lbc/loot_table/ported/`:
`incoming_artifact_1`..`6`, `legacy_incoming_artifact_1`..`6`, `sand_amulet`,
`legacy_sand_amulet`. Only the26.3 loot codec field conversion
`functions→modifier` / `function→type` was applied. Current artifacts are
source artifact pool1 entries17..22; old are pool0 entries15..20. Both amulets
are the unique exact gaara/gaara_old source item leaves. Structural equality
against all twelve original artifact entries passed locally after inverse
codec conversion. No whole unresolved pool, catalog entry, recipe or mode was
enabled by this child. These leaves alone prove operator staging, not a normal
acquisition route. Root owns inspection and qualification of the real existing
normal artifact/legacy references and their reachability.

## Verification limits

Native asynchronous checks are prepared for fourteen vanilla payloads, real
environmental/full-shield criterion dispatch, ordered current/old effect grants,
source nominal native TNT actor, actual accepted old16-sand event, wolf owner/
attributes/technical-target guards, scheduled instant Harm reentry/cooldown,
current40-tick passive and old absence, native delayed local sand cleanup,
foreign replacement preservation, native4 shards,14 current actors and scoped
restoration. A tracked inherited native TNT child also receives real native hurt
checks after its author enters the observer registry and spectator gamemode,
compared against independent native same-type damage/impulse. Greed and a real
allied-player scoreboard team remain protected; no world blast is detonated.
The fixture reuses the recent passive native network scheduler,
latest teleport packet retention, true loaded/teleport ACK, score forceWritable
restoration and HP-before-Greed reset ordering. It does not tick players,
effects or worlds manually.

No runtime pass is claimed here. Actual fatal native death, whole TNT blast/
chain/impulse behavior, corrosion/wall placement, Time Stop and full normal
acquisition still require root's integrated native execution/review; fixture
coverage does not establish those results. Physical client pixels/audio, latency,
peakTPS and legacy runtime remain unobserved.

Initial handoff SHA256 (before any integrated compiler/runtime fixes):

- LbcIncomingArtifacts.java: `33c312bcc54276e0874c369cd25b40effe25614e7a917e1532a93ce5b6e9dfe2`
- IncomingArtifactChecks.java: `a9cc094d7475341f49c9f9a981c05d8acbe22b9b01420168f0a3be1f48d74c6a`
- IncomingArtifactCriterionMixin.java: `4fae3e174551d4f9939e4d3dcb8e389957358523e9ad491d7e0b6b8adf52e5ab`

Prequalification fixture followup: the first integrated run reached the delayed
column damage assertion after passing native observer-child controls. Its old
fixture did not isolate the second eligible player from source column creation,
and omitted actual HP/source diagnostics. This is a fixture overlap hypothesis,
not a confirmed production cause. The new fixture keeps that player outside10
within the already captured resident volume, asserts a single column, records
every native damage call/HP/position/cooldown/source and compares a separate
actual native owner-attributed falling_block4 control against96HP. The delayed
column itself still requires exact96HP. Production is unchanged by this followup.
