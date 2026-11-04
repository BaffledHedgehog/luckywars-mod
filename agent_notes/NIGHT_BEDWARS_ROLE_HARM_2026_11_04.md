# BedWars shop role and finite ally admission ? 4 November 2026

Implementation handoff; root owns compile/startup/runtime evidence and release.
No build, server operation, commit, source-server/RP/author-WIP edit by this agent.
Shared production files were released to root before this note was written.
Public mode3 still requires actual final-scene/artifact qualification.

## Durable shop identity

`LbcBedwars.shopRole(Entity)` now delegates to `LbcBedwarsRoleGuards.shop`.
Recognition uses source `bedwars_shop`, persisted `skywars2.bedwars.shop`, OR the
actual canonical arena topology SHOP UUID in its realm. This protects the same
identity even if representation/strict tags are missing and does not depend on
current Villager class, translated name, team or invulnerable flag. A cached set
is DERIVED from immutable existing Topology, rebuilt when its identity changes;
there is no second role lifecycle, world scan or private checkpoint.
Root mapReady now calls `markShop(e)` to stamp source/durable/strict native tags.
Native tags and the existing typed topology already have canonical replay owners.

`affectable` permits the existing explicit `MatchActorCheckpoint.restoring(entity)`
scope; this avoids blocking canonical body restoration. It is not a maps.busy or
arbitrary freeze bypass. `publication(ServerLevel)` means **true allowed**: it
rejects an actual currently scoped shop-source emitter before native body/world
publication. The typed hostile Source factory remains total with its old API;
no factory throw, null policy or partially published replacement was introduced.
Root wired this predicate before ArenaBlockMixin attribution and before the
MapManager.restoreSnapshot preclear/NBT boundary, and excluded shop chunk cargo.

## Finite native / existing owner admission

`BedwarsRoleGuardMixin` nested classes (root registered): Living, Mobs, Impulses,
Villagers, Publications, ProjectileOwner, CloudOwner, TntOwner, Clouds.

- Living rejects shop hurtServer, addEffect/forceAddEffect, heal, canBeAffected,
  isAffectedByPotions and source-bearing knockback before payload changes. This
  includes beneficial healing; permanent invulnerability alone was insufficient.
- Mob canAttack and existing WandProjectileMixin setTarget HEAD reject shop as
  target or actor. Both compose root Aux.canAttack before target installation;
  no temporary invalid target notification is installed/corrected afterwards.
- Mob.convertTo returns native rejection/null BEFORE its replacement factory or
  callback; native Villager thunderHit rejects before witch conversion/fallback
  ignition. NoAI/invulnerability are retained but are not the enforcement engine.
- Native Entity push(DDD), ignition and lightning admission prevent ordinary
  collision/explosion impulses/fire from altering the shop. There is no generic
  setHealth/setPos/setDeltaMovement/remove interception. Map reset, unload and
  approved replacement can still retire bodies normally through their own owners.
- Native Mob.interact consumes vanilla fallback on a shop, preventing spawn-egg,
  name-tag, feeding/leash/ordinary trading side effects. Root's registered shop UI
  callback owns authorized interaction before that fallback; actual packet menus
  must be qualified with the final mixin set.
- Native ServerLevel.addFreshEntity rejects a scoped shop emitter before child
  registration/merge callbacks. Root also guards terrain/BE admission as above.

Existing shared code changed only at reviewed boundaries: LbcDamage.allowDamage;
LbcWands.validRoleTarget, known NPC/status electricity/fire entry points and
retained trace admission; LbcSpellSource target/canAffect; Mobgun.dispose;
RuneGlobals.clearEntities; Sauvojen.emitHostile; Magic.magical/magicalMob/mechanical;
existing Mob.setTarget hook; native instant-effect and explosion recipient hooks.
Hagalaz now skips durable shop roles before its maploader whitelist can kill them;
its already-approved permanent Chef purge is unchanged. Magic beacon classification
rejects shop before particles/interception, not only at final damage.
Sauvojen's shared hostile switch rejects source before all eight payload families'
queues/sounds/factories. Electricity rejects before queue/sound and rechecks a
retained NPC/explicit/typed emitter before motion/particles. ChainLightningStatus
already rechecks shared validTarget on accept/observe/tick, so no duplicate engine
or extra patch was required. Ordinary role-aware selectors now inherit the durable
shop veto through validRoleTarget, including custom polymorph/clone admission.

## Active ordinary BedWars ally protection

Concrete old blocker was verified in code: source team metadata deliberately leaves
friendlyFire TRUE; ordinary melee fell through allowDamage, and physical arrows
forced native canHarmPlayer true. Incoming damage to friendly mobs was also outside
the old player-only projectile guard. No native scoreboard FF flag was changed.

`LbcBedwarsHarm` is a finite admission helper, active ONLY while the actual ordinary
mode3 BedWars arena is initialized. Mode1 allegiance behavior is unchanged.
It reads actual native teams, canonical `assignedTeam(UUID)` for detached/offline
causes, and existing accepted Owner/Source data. Read-only `LbcIndirectKills.policyCause`
queries existing Captured/Transferred/InheritedKill/typed service causes; unlike
babylonCause it never generates a new attribution order. Aux.owner(source) is now
part of the existing playerSource chain, with null protection.

Existing explicit electrical/KFC/trap WILD helpers retain priority. Accepted
Owner.friendlyFire=true remains exempt; an explicit cold accepted Owner(false)
is not accidentally overwritten by an unrelated current Source scope. Source with
cold owner protects allies; a declared Source with no cause retains the prior
explicit Source policy. Current-scope fallback must match the actual/captured
author, so one synchronous nested actor does not borrow another actor's WILD flag.
Self costs and physical shots returning to their own actual shooter keep existing
behavior. Native projectile/cloud/TNT owner UUIDs are read directly from their
existing EntityReference fields; no extra author/clock/owner store was added.

Shared enforcement points:

- LbcDamage.allowDamage and Wands.allowDamage reject ordinary allied player/mob
  harm before acceptance, including deferred TimeStop admissions.
- Wands.physicalProjectileTarget and canHit reject ordinary allied collision BEFORE
  arrow/fireball status/impulse payloads. Existing LbcDamageMixin is UNCHANGED;
  its physical target hook now receives the guarded predicate.
- Default projectileTarget now protects allied native mobs as well as players in
  mode3; existing explicit Source target policy still dispatches first.
- LbcSpellSource target/canAffect respects cold Source allegiance while preserving
  explicit WILD/exclusion policy. Source/role rejection is not a blanket team flag.
- Native source-bearing knockback and actual explosion hurtEntities recipient
  selection reject ordinary allies before impulse, not just HP changes.
- Native addEffect/forceAddEffect and existing allowProjectileEffect reject harmful
  ordinary allied status/resource grants before custom payload/modifier acceptance.
  Ordinary beneficial aid remains admitted. Classification includes known signed
  regeneration/poison/wither/attribute/hunger inversion and actual undead instant
  heal/harm inversion; nonbeneficial neutral/custom carriers default to protected.
- Existing LbcInstantEffectMixin checks the actual holder/amplifier/target/source
  before instant heal/harm/resource execution. In active mode3 its old holder-only
  instant gate defers to that exact-amplifier entry, avoiding incorrect blanket
  treatment of undead/negative healing. No separate instant-effect engine exists.
- Native AreaEffectCloud filters recipients BEFORE victims/radius-use bookkeeping
  when none of its effects may affect that ally; beneficial or mixed clouds still
  deliver their legitimate aid, with harmful grants checked individually. Shop is
  excluded entirely. Owner references preserve ordinary offline-owner team policy.

## Actual qualification helper APIs

`BedwarsRoleChecks.start(game,owner,enemy,actualPlacedVillagerShop)` returns Probe.
Root calls Probe.finish() on ordinary ticks and closes in finally. It requires two
real different native participant teams, a canonical placed SHOP UUID and an
isolated1.5-block cloud radius. It temporarily disables permanent invulnerability
and removes mutable role tags to prove identity-based protection. Actual native
hurt/heal/status/forced status/impulse/AI/conversion/lightning calls, real custom
polymorph/dispose/electric borrowed-center calls and native child publication are
checked. A real native AreaEffectCloud advances on registered entity ticks: normal
cow receives regeneration, shop does not, radius charges exactly once. No test
publisher/policy/factory or artificial cloud/service tick loop is used.

`BedwarsRoleChecks.allies(game,owner,teammate,enemy)` uses three real enrolled players
from root's four-player/two-team scene. It checks real ordinary melee, incoming and
outgoing allied native mob damage, actual Projectile.canHitEntity plus native arrow
hurt bypass, poison/hunger rejection, regeneration aid and source-bearing knockback.
Enemy melee is a positive control. Actual registered conduction damage and a native
TNT DamageSource carrying an accepted explicit WILD cause must still damage the
ally; ordinary owned TNT must not. TNT protection configuration is removed as a
confound through its existing unprotected marker. Explicit WILD poison is accepted.
These are policy-delivery tests, not claims that every item variant was simulated.
Root must retain full coordinator/fixture restoration around combat/history state.

The initial root compile exposed owned Level/ServerLevel and native invulnerability
setter errors in Probe; those were fixed to exact26.3 ServerLevel casts and
isPermanentlyInvulnerable/setPermanentlyInvulnerable. No subsequent build/runtime
was run by this agent. Local native sources/javap verified owner field descriptors,
cloud list call, Mob conversion signatures and the cloud contents/scale fields.
Scoped whitespace review passed; final startup/mixin/runtime results are root-owned.

## Scope / concrete remaining qualification obligations

This does not claim every arbitrary raw setter can infer an attacker. A source-less
custom `setHealth/setPos/setDeltaMovement/mana` call carries no causal policy to
query; admitted custom producers must use their reviewed target/canAffect/accepted
Owner boundaries. The existing includeAllies=true calls are deliberately not
blanket-disabled: Stonekivi's comet centers explicitly preserve allies (user),
Lifestealer uses an ally query for an advancement, and other paths include Academy,
Psycho and explicit source/terrain policy. Changing that Boolean globally would
break unrelated/authorized behavior. Item-specific raw movement/resource paths
still need their actual rule qualified in mode3 rather than inferred from flags.

Ordinary melee/arrows/mob attacks/status/resource/impulse paths now have concrete
shared guards and helper assertions, but no universal proof is asserted for every
catalog actor or signed/custom neutral payload. Root should run real final scenes,
existing WILD families (ants/acids, conduction/meditation/KFC/traps, registered
captured owners) and reflection/controller/offline/replay cases before opening
mode3. Durable shop source/target selection is enforced at the listed common
owners; unreviewed future producers must obtain admission before particles/sounds,
not rely on late world publication rejection as a complete source-center contract.

## Native 7d8de3bf role/allies follow-up

The actual mode3 basic log stopped the allies helper on a fixture assumption:
fresh native Cow health is not 20. Both player-melee and native mob-melee checks
now compare with the cow's captured initial health; production harm policy was
not changed to satisfy that assertion.

The shop impulse assertion is now split between direct push and source-bearing
knockback. Source inspection found the existing DirectionalKnockbackMixin HEAD
can call DirectionalKnockback.living, write velocity, and cancel before another
HEAD admission executes. This is an existing replacement injection, rather than
a knockback WrapMethod. Its own entry now composes the same role and typed harm
admissions before any custom movement write. Existing native fallback guards
remain; accepted WILD/conduction exceptions still come from the shared Harm
policy. No new impulse implementation, mode1 team rule, or broad team flag was
introduced. Root owns the next compile/native rerun and subsequent positive
control evidence; this follow-up itself ran no build or server.
