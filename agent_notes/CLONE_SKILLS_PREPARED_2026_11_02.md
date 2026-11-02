# Skills15/17 prepared native helper — CLOSED, 2 November 2026

`LbcCloneSkills.java` is prepared code only. It is not connected to LbcItems,
loadout admission, tick dispatch or a menu. No ActorAdapter is installed, so
`ready()` is false. No compilation, native process or runtime qualification was
performed by this implementation task. Do not expose either skill on this evidence.

Author decisions supersede the older questions in
[the source audit](BOW_HARVEST_SKILLS_SOURCE_2026_11_02.md): both skills entirely
unjammed; WILD self/allied/enemy/unowned actors; broad boats/carts/XP/cloud scope;
17 includes bosses, owned actors, damaged mobs, gear and AI. Preserve original
cause and source ownership. Independent permanent doubled/quadried flags compose
to eight bodies, without same-skill recursion. Protect map/shop authority and
exclude visual children; regenerate those with their main actor. The old Intercal
condition belongs to9, not15 (separate root-owned change).

## Prepared behavior and API

`ActorAdapter.completeCoverage()` is a deliberate integration gate. It must remain
false until every admitted actor family and every active state overlay below has
an owner-approved reconstruction path. `protectedActor` must recognize actual
protected roles, independent visual carriers and hitboxes. `capture` returns an
immutable operation-local ActorImage, or null with an unsupported counter. It
must never return a vanilla shell image for a managed actor. No default adapter
exists, and no field-reflection copier substitutes for these owners.

An ActorImage creates the real native class (override `create` for custom bodies),
installs copied private state in `install`, and removes every partial private
entry/child in `rollback`. Install must not mutate the original. Each install gets
an identity-based original-to-this-clone map, and must create independent mutable
queues/collections. Visual children, vehicle/passenger/leash/fishing links and
actor-local source references require the owning adapter. The helper strips native
Passengers rather than manufacture duplicate authority-bearing children. Original
external native projectile owner and native Mob target are retained directly.

Native image capture occurs once per actual operation, never every tick. Fresh
constructor numeric IDs and UUIDs survive native NBT load; source UUID and
Passengers are removed. Native NBT, metadata with serializer copies, actual
attribute values and reviewed scalar MatchEntityRuntime values are copied.
MatchEntityState.load is deliberately not reused: it also forgets original owned
effects and restores old identity. This helper does not forget original effects.

Source15 radius10 produces one positional-spread copy with unchanged velocity.
17 radius16 produces three projectile copies with large velocity spread and
position spread, or three mob copies at the same position with small motion
spread. Original17 also receives small motion spread. Negative coordinates use
Math.floor exactly. Original tags are copied and the current once flag is added
to all successful copies and original only after publication; temporary flags
are stripped. Four projectile scoreboard fields, eleven mob fields and mob native
teams are copied. Shared spellSources.inherit retains original causal authority.

Discovery rotates admitted holders and uses the native resident local AABB query
with spherical predicates and max256 results; it does not scan all world entities
or request chunks. A publication pass creates at most48 main bodies, with a soft4ms
deadline between operations. A single three-copy operation is atomic at that
boundary. Spread into a nonresident chunk is retried on a later encounter. Custom
visual-child work must fit a future aggregate owner budget; it is not yet covered
by the main-body count. Native spatial traversal itself is not a hard CPU bound.
Owner-count/many-dense-overlap performance needs runtime qualification before use.

## Exact blocking owners found in source

These are concrete integration owners, not adapters already implemented:

- `OwnedEffectLayers`: copy `checkpoint(original)` with restoreCheckpoint on each
  clone; original leases must survive. `LbcIndirectKills`: effect-instance causes,
  fire/displacement/execution and pending consequence ledgers need per-body copies.
  `LbcBabylonEffects`/`SignedEnvironmentalEffects` also own active signed layers.
- `LbcSpellSource`: base inherited source is already copied, but adapters must
  review actor-local emitter IDs separately from immutable causal author/Owner.
  `LbcCommunism.owners`, `LbcSummons.mobs`/`consequences`,
  `LbcSummonOrders.assignments` and summon target/control queues are not native NBT.
- `ProjectileLifetimes.tracked`/Review owns projectile age, next review, terrain
  cursor and owner. Registering a fresh default review loses active flight state.
- `LbcClouds.clouds`/CloudState, `acids`/AcidState, `acidAuthors`, potions and batches;
  `LbcSpecialFireballs.actors`/`pending`; `LbcExoperso.blastSources`/Series;
  `LbcExasperato.charges`, Search/RayTicket scheduler and pending BlastBatch.
- Custom projectile factories and private body fields require their exact owners:
  `LbcAcidCorruption.Charge`, `LbcAir.Charge`, `LbcBlind.Charge`,
  `LbcChargedSwords.ThrownSword`, `LbcClouds.AcidProjectile`,
  `LbcExasperato.Charge`, `LbcExoperso.Charge`, `LbcFartus.Shot`,
  `LbcGovnomet.Charge`, `LbcIceWand.IceShot`, `LbcLifestealer.Charge`,
  `LbcPrism.Charge`, `LbcPsycho.Charge`, `LbcRuby.Charge`, `LbcStone.Charge`,
  `LbcTeleport.Charge`, `LbcTerra.TerraProjectile`, `LbcHalloweenWind.NativeCharge`,
  and `LbcSyringe.BonusFireball`. Also audit managed native carriers in LbcNeutral.
- `LbcSummons` inner SoulVex/CultSkeleton/CultWither/CultZombie/SoulRavager/
  SoulGhast/SoulBlaze/SoulGuardian/SoulBat/SoulHorse; `LbcPsycho.PsychoVex`,
  `LbcSoulless.GhostVex`, `LbcKfcRewards.CultBat`, `LbcTransformator.Actor`/Blast,
  `LbcShishEncounter.NativeCyborg`: their public EntityType factory may create a
  different class; the helper rejects such replacement instead of dropping AI.
- `LbcApocalypseActors.actors`/Actor/unloaded; `LbcSauvojenBoss.actors`/Actor;
  `LbcShishEncounter.actors`/State; `LbcGrimoireBoss.actors`/bars/warnings;
  `LbcHelicopterEncounter` attacks/source graph; `LbcGodHedgehog` phase/attack
  state. Boss bars, targets and delayed attacks are service-owned.
- `LbcChaosTrapActors.tracked`/authors, `LbcAntDrops.tracked`/authors,
  `LbcDroneDrops.tracked`/slots (cart-to-own-host link), `LbcConsuming.summons`,
  `LbcIndolenceFingers.actors`, `LbcWitchLust.actors`, `LbcLibraryRandom.actorTable`.
- `LbcFpvDrone.actors`/Visual and controllers/Session: main body and visual must
  be rebuilt together. Do not duplicate controller authority, private camera seat
  or body chunk tickets. `LbcFamilyDrops` actor/child relationships and all visual
  hitboxes/TrialWarningVisual descendants need the same main-child audit.
- Per-entity custom status overlays: `LbcElectro.shocks`/reflectedOwners,
  `LbcShooters.stuns`/restoredStunAges, `LbcHoming.curses`, magic queues,
  polymorph/transform control, freeze state and delayed death/food state.
- Pure virtual ray branches need explicit owner cloning at their current
  position/direction/lifetime, including slowraycast eligibility and both once
  flags. This native-body helper cannot discover or duplicate a ray with no body.
- Native fishing-hook construction/hooked target and player's fishing ledger;
  ender-pearl owner ledger; boat/cart passengers; leash holders; Dragon multipart
  body identities; vanilla transient projectile target/collision state and brain
  memories need a reviewed native relationship/runtime adapter, not UUID merging.

This inventory identifies observed blockers; it is not a proof that the complete
owner closure has been enumerated. Continue by tracing every member of the final
26.3 projectiles_double/mobs/raycasters sets into its body, runtime registry,
consequence queues and visual-child owner. The broad declaration grep also finds
unrelated player state and protected map actors, which must be classified rather
than blindly copied. No whole MatchActorCheckpoint/MatchSpellCheckpoint replay,
world snapshot, or whole-world periodic NBT cache is authorized as a shortcut.

Remaining integration after adapters: lifecycle tick/stop/checkpoint boundary;
native and virtual eligibility tables (root owns resources); exact source/once
composition and original-cause tests; custom body/visual/AI/gear/loot/fishing/
boss tests; TimeStop, rewind, epoch cleanup and dense overlap budgeting. Skills15
and17 remain closed until this closure is qualified on the exact artifact.
