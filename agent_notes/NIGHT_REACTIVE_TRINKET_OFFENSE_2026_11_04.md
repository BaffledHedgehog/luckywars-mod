# Reactive trinket offense audit ? 4 November 2026

Read-only mechanics/API audit for IDs4,6,24. No production edits, builds, server runs,
source-server writes, RP edits or commits. Baseline: parent-qualified BedWars artifact
`build/verification/bedwars-native-2026-11-04/test-dc033ee7.jar`, commit ea6d6572.
The audit does not declare these three missing consumers implemented or qualified.

## Exact source roots and trigger identity

Let F be `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/`.
Exact numbered source and SHA256 manifest are saved in ignored evidence:
`build/verification/reactive-trinkets-2026-11-04/offense-source-lines.txt` and
`offense-source-manifest.json`. Qualified public/package API evidence from actual
JAR javap is `qualified-dc033ee7-api.txt`; bounded current source excerpts are
`native-offense-boundaries.txt` in the same directory.

- `F/lbc/advancement/gowno.json:4-15,59`: native player_hurt_entity, blocked=false,
  excludes arrow_only; target must not be interaction; player excludes Nexus and
  Imprinted. Reward is items/swords, whose line66 dispatches swords_get_trinket
  only unless legacy=1 and with #lbctrinket>=1. This is broader than strictly
  direct melee: non-arrow indirect player-owned damage can satisfy the criterion.
- `items/swords_get_trinket:1-3,5-7,17-19` counts drojji, mandoahuet, psycho_soul
  independently using clear matching custom_data 0. Quantity is total physical
  item count, not occupied stacks or chosen-kit ID count. No consumption. Each
  branch runs once per source event, not once per copy; count changes probability.
- Actual `items/damaged_get_trinket` contains only banangay/gneiss/hedgehog/ice.
  It does NOT contain these three offense consumers. Prior broad summary in
  lbc_items_arcana_trinkets.md overstates that route; use exact source evidence.
- These three source entry functions have no mana charge, item cooldown, periodic
  inventory enrollment, cast_wand dispatch, or additional proc count/shot multiplier.
  Source says event, not click input: a missed swing must not roll.

## ID4: drojjevoe_odealo, custom_data trinket=drojji

`trinket_effects/acid`: n1=.15; n>=14 guaranteed; n2..13 computes
(150+70*(n-1))*.001, hence .22 at2 and .99 at13. One acid projectile on success.
REAL SOURCE BUG: acid_chance stores the value as storage tmp but macro substitutes
`$(tmpd)`. The intended intermediate branch is therefore not reliably callable;
never present the formula as proof that this broken branch ran. Parent must choose
functional typo repair under existing bug-fix authority, or put uncertainty in
its own night-question backlog. This audit supplies no new author approval.

`charge_acid_ball`: cure sound volume1 pitch2; one marker at eyes, lifetime300,
raycastacid/player/slowraycast/shield_blockable2/magic. `lbcr/acid_get_id` copies
caster rotation, lbcID2 and team_number; universal_razbros adds yaw[-30,30] and
pitch[-20,20] degrees independently in .01-degree steps. No six/eight-shot wand
volley: reusing a wand's full cast would be wrong.

Shared legacy small-acid aura radius2.5. Normal: Poison I15s, excludes muravei,
caster ID, same team and spectators. Burning:3 on_fire damage per aura delivery;
no muravei immunity. Ignites from fire/lava at current/trailing/below cells,
on-fire entity within2.5, burning raycaster within3.5. Burning adds3 age in
addition to ordinary age1, so accelerates remaining lifetime. Shield-blockable
source flag remains. No separate initial direct damage or instantaneous victim
hit: emit the native projectile and let shared collision/aura act later.

Existing author overrides AGENTS7514-7527 and ACID_ANT_DROPS_PORT: native projectile
velocity/gravity/drag; pass through ordinary creatures; native block impact sticks
until original lifetime expires without age reset. Time Stop pauses movement,
aura and age. Do not transplant legacy marker-step movement/trailing aura math
as a second physics engine. Native sourceAcid initial speed=.5; actual radius2.5,
Poison300 ticks, burning3 and +3 age are already in common LbcClouds.

## ID6: mandoahuet

`trinket_effects/bass`: n1=.15; n>=14 guaranteed; n2..13 computes
(150+50*(n-1))*.001, .20 at2 through .75 at13. Preserve the explicit jump to
certainty at14; a global linear formula would silently change source behavior.
One ordinary basscast per successful event. That cast chooses one mode uniformly
1..4: Weakness I160ticks, Slowness IV240, Blindness I160, MiningFatigue I180.
Sphere distance .01..25 around caster, excludes spectator/same_team. Message
player_stunned to eligible players, both firework sounds volume2 pitch0 and
100 flash particles spread17/17/17 are part of the cast. No base direct damage.

It ALSO invokes the real existing bass combo, not a reduced trinket-only debuff.
First cast starts combo1; second records interval and reaches2; later beats accept
interval>4 and within +/-1 tick of learned interval. Successful beat increments
combo, +500 mana, fireworks/sounds, sonic_boom damage equal to new combo to nearby
source mobs within25 excluding caster; combo>=10 awards staffs/bass. Failure
restarts the combo in the same call. A trinket proc and actual Bass wand share
these counters. Source upgradable held-item check can qualify Academy when a mob
within10 has all four statuses; do not invent a separate promotion or bypass.
Source legacy sonic damage uses a player `by` author, so this event can itself
satisfy non-arrow player_hurt_entity. Nested procs need explicit reviewed causal
handling; source does not contain a separate bass recursion guard.

## ID24: psycho_soul

Chance (40+10*(n-1))*.001 = .04 at1, .05 at2; naturally certain from n97.
No count>=14 shortcut. Success currently scans nonplayer #mobs within24 of caster
EYES with HurtTime10, strict_map_object excluded, NoAI not true. It then sets
NoAI=true permanently; no source timeout or automatic restoration. It does not
kill, transform to vex, inflict ordinary Psycho scared/lookback/piss or deal damage.

Known source identity bug: any unrelated nearby just-hurt mob is captured. Parent
has relayed author-approved actual-victim correction; carry actual native victim
from accepted trigger, never find it by HurtTime or nearest-entity scan. Retain
nonplayer Mob/noAI/range/protected-role checks and normal ally protection. There
is no need to reask whether accidental nearby capture should be retained.

The victim emits a soulparticle1 marker at its eyes, random yaw[-180,180] in .01
steps, default pitch; sound particle.soul_escape volume1 pitch1. Every unstopped
source tick: soul particle; local movement(.5,.15,-.3), yaw+30,pitch0; at age100
emit100 souls, then remove at>=100. This is visual-only continuation, not a new
projectile hitbox/homing/body. Paused source tick entry excludes Nexus/Imprinted.
NoAI permanence and100-tick visual lifetime are different facts.

## Suppression boundaries: source facts versus policy decisions

No stopper/nomagic checks occur in the three proc wrappers or swords_get_trinket.
Legacy acid is tagged magic, so artifact/nomagic can erase its body within25;
Bass immediate status/sonic has no emitted magic body, and soulparticle1 has no
magic tag. Native common source-acid explicitly strips magic/mecha tags on load
and applies its already accepted common physics/role policy. Native Bass use()
has mana+suppression admission, while bass() is the shared effect producer behind
it. Calling use() for a free trinket proc would add an unsupported mana charge,
wand-cast callbacks and input gates. Psycho's ordinary wand use()/afflict() is not
the NoAI operation. Do not infer that all three should inherit full wand input
suppression merely because their effects resemble wand families.

For parent backlog, if existing standing effect policy does not settle it: clarify
whether free Bass/PsychoSoul proc admission should remain the source ungated event
or inherit existing magical producer suppression; acid should follow the already
accepted shared acid rule, not a new bespoke jammer. This is a policy distinction,
not permission to expose an unqualified mechanic. Existing BedWars shop source,
role, ordinary ally protection, observers, TimeStop and dimension rules still apply.

## Current actual native/JAR API contracts and finite proposal

The four inspected production files have no local diff against the released
baseline; protected author WIP elsewhere (including Academy/resources) was not
modified. Evidence reads the qualified dc033ee7 JAR rather than assuming a future
getter exists. Public entries actually present:

- LbcTrinkets.damaged(LivingEntity,DamageSource), shot, summoned, castWand. Its
  count(ServerPlayer,Item,CompoundTag) is PRIVATE and includes inventory/crafting/
  carried slots through native clearOrCountMatchingItems. No ID4/6/24 item constants
  or consumers currently exist. registerAuthored is PRIVATE, already preserves
  original loot presentation/identity with stacks99; reuse it through a narrow
  package adapter, not hand-written metadata or unrelated catalog exposure.
- LbcClouds.sourceAcid(ServerLevel,Vec3,float,float,int,boolean,ServerPlayer) is
  PUBLIC and already accepts budget300/shield=true/real author with empty native
  owner (avoids caster-collision immunity); sourceAcidCanAffect handles typed Source.
  SourceAcid owner/age/lifetime/snapshot are public. Emit one using exact spread,
  attach accepted ordinary Source/Owner before native publication using existing
  scoped capture path. hostile() creates WILD: do not use it for normal trinkets.
- LbcEvokerBass.use is PUBLIC but is the wrong free-proc entry. Existing bass
  (ServerPlayer,InteractionHand,ItemStack,int,boolean) is PACKAGE scope and already
  owns correct debuffs/combo/trial/sounds. Add finite trinketCast(owner) adapter
  selecting one random ordinary mode, no mana debit/no wandCasted; share bass().
- LbcPsycho has public use/tick/clear, package afflict/yellow and clone status/vex
  images. None implements soul NoAI. Add narrow actual-victim soulEscape admission
  at this owner, not afflict or execute/vex transform. No factory null/throw hack.

Native SkyWars2 AFTER_DAMAGE222-229 calls trinkets.damaged only nonblocked positive
actual damage. Existing consumer excludes arrow_only but does not require
LbcDamage.isMeleeAttack; that helper itself is strictly PLAYER_ATTACK with both
native entities==owner. Existing OutgoingSkillCriterionMixin is the EXACT source
player_hurt_entity hook and includes lethal/resisted native hits. Prefer one new
outgoing offense method fed there, separate from existing incoming trinkets method;
do not also call it from AFTER_DAMAGE/death or fire one proc twice. Parent must
explicitly settle whether this batch preserves full source non-arrow damage or
narrows to direct melee: source itself is NOT strictly melee. Avoid adding ad hoc
recursion cooldowns/count caps; accepted-source identity and nested reentrancy need
tests, especially real Bass combo sonic callbacks.

Use native Mob.setNoAi(true) as canonical persistent actor state. Full existing
MatchActorCheckpoint native image owns NoAI/replay; do not add private full-NBT
snapshots or restore NoAI on ordinary visual expiry. Visual soul continuation can
be one LbcStonekivi.Work over Vec3/yaw/age/realm/epoch/typed cause with its existing
queuedWork + MatchQueuedWorkCheckpoint.Graph image; ready() must admit at most one
visual sample per actual unstopped native tick, because this budgeted queue can
step a ready task repeatedly (up to128 steps) within one tick. It needs no new tick world scan,
projectile entity type or second scheduler. snapshotIdle/checkpoint must be real,
not default-throw. Rewind prepares all links before install, cancellation drops
only continuation, and clone rules must not clone cosmetic markers as mob bodies.
Current common acid clone adapter already routes through CloneComplexProjectileStates;
new acid shots should use it, preserving age/stuck/burning/cause. Existing short
fang actors are omitted/cancelled by MatchTransientSpells; Bass statuses/scores have
native canonical owners. No new parallel Bass state map is needed. Shared clock
rebase across whole coordinator rewind must be verified rather than assumed.

## Bounded qualification proposed for next authorized implementation

Actual native criterion hits with source identities: blocked/missed/arrow rejected,
ordinary accepted and lethal hits counted once, Nexus/Imprinted/observer/shop/ally
protected; two actual teams; unrelated adjacent HurtTime10 mob untouched. Exercise
actual inventory/cursor/craft counts and n1/2/13/14/97 chance boundaries through
meaningful deterministic RNG or guaranteed-source counts, without fake factories.
Acid must really fly, stick, poison/burn with causal author, shield/TimeStop and
source lifetime; x2 clone preserves real managed image. Bass proc must cost no
mana initially, apply one actual mode, share a real wand rhythm and refund/damage
only on successful beats; recursion/normal/WILD causes must remain bounded and
correct. Soul sets only actual eligible Mob NoAI, persists beyond visual100 ticks,
respects protected roles and later native saved/reloaded actor state. Nonempty full
coordinator rewind must preserve native NoAI, accepted acid state and queued visual
age without restarting effects/sounds or granting items. Root owns final native
qualification and exposure; this note is audit evidence only.


## Authorized implementation handoff (same night, after audit)

Parent authorized source defaults Q10-12: ungated proc wrappers, existing common
acid rule, fix acid tmpd typo using fresh15%+7% per extra copy, Bass>=14 certain,
full accepted non-arrow outgoing criterion including indirect damage. The actual
victim correction is retained. This authorization supersedes the audit's pending
scope choices above; no new author approval is asserted by this agent.

Released files: NEW LbcReactiveOffense.java; narrow existing LbcClouds.java and
LbcEvokerBass.java adapters only. Root owns all registrations, catalog/resources,
input/criterion, lifecycle and coordinator wiring. No changes to LbcTrinkets,
LbcItems, mixins, GameController, existing tests or author WIP. No build/server/
commit performed. Scoped diff whitespace review passes; root owns compilation and
real native qualification.

Integration API:

- `new LbcReactiveOffense(game)`.
- `outgoing(ServerPlayer attacker, LivingEntity actualVictim, DamageSource source,
  boolean blocked, int acidCount, int bassCount, int soulCount)`. Supply finalized
  live inventory counts from ONE native PlayerHurtEntityTrigger callback, not also
  AFTER_DAMAGE/death. No additional positive-actual-damage filter is introduced;
  native criterion already owns blocked/resisted/lethal admission. Counts<=0 skip.
- `incoming(LivingEntity,DamageSource,int,int,int)` explicitly does nothing for
  these offense-only IDs; the source incoming root has no such branches.
- `tick()`, `forget(ServerPlayer)`, `stop()`, `visualCount()`, `visualSnapshot()`.
  Native NoAI is permanent and is never undone by visual expiry/forget/stop.
- Package typed coordinator API `checkpointBoundary()`, `checkpoint(Graph)`;
  returned Checkpoint exposes `prepare(Links)->Runnable`. Use the SAME existing
  MatchQueuedWorkCheckpoint.Graph; individual Soul Work images capture exact
  position/yaw/age/activity, so do not image these a second time elsewhere.
- Call `prepareFrozen()` before restoring shared queue/body state; execute graph
  image installs plus offense owner plan after all links prepare successfully;
  call `publishFrozen()` afterwards to rebase only native-tick readiness. On
  cancellation use `cancelFrozen()`. Owner plan reinstalls the captured existing
  Bass clock together with native canonical bass_combo/ticker/len score replay.
  No private score/status/NBT snapshot or alternate rewind engine is introduced.

Soul visual is an existing Stonekivi.Work, with ready() limiting it to one actual
unstopped native tick;100 visual samples retain source local spiral and final100
souls. Only active owned jobs are visited for stale epoch retirement. No world or
idle inventory scan, new projectile type, hitbox, item loop or second scheduler.
Canonical MatchActorCheckpoint/MatchEntityState owns NoAI; no custom afflict/scare/
vex state is invented. Saved visual metadata includes real author/epoch/realm and
accepted cold cause; visual samples themselves perform no damage/resource effects.

Acid adapter creates exactly one common native source-acid projectile,300 budget,
shield=true and source centidegree spread, with ordinary typed policy captured
before publication. Its persisted skywars2.trinket_acid identity narrowly retains
caster/team immunity in all modes; sourceAcidWild is false for this identity.
Unrelated existing source-acid variants keep their prior policies. Native movement,
sticking, aura, age, reflection, clone image and accepted source owners remain
shared. Rejected trinket publication discards its unpublished body before the
calling wrapper can emit a success sound.

Bass adapter invokes the existing bass producer, including combo/trial/messages/
sounds/particles, choosing one ordinary random mode. It skips only wand input,
wand mana debit and wandCasted hooks, and clears an enclosing WILD Source so a new
ordinary trinket does not inherit the parent's unrelated danger policy.

Important correction to initial cooldown hypothesis: native LivingEntity cooldown
rejects damage<=lastHurt, but increasing synchronous combo damage can exceed it;
Bass updates its ticker AFTER its sonic loop. The actual source recursion latch
is advancement completion: gowno is granted/unregistered BEFORE rewards execute,
and items/swords revokes gowno only at its END. A nested same-player hit therefore
cannot grant that still-complete advancement again. New offense implements only
that per-author synchronous reward latch with finally cleanup. Independent later
hits and genuine different-author nested hits remain admitted; no tick cooldown,
recursion depth/count limit or gameplay cap exists. Exact native cooldown and
PlayerAdvancements grant-before-reward code is saved as ignored evidence
`native-cooldown-advancement-latch.txt` beside the original source revoke line.

Qualification still required by root: actual native trigger/blocked/arrow/lethal
and indirect coverage; same-author nested Bass sonic reward cannot reenter;
later independent rewards still work; actual four modes/shared rhythm; real acid
physics/status/ownership/clone; actual-victim-only permanent NoAI; queued visual
natural100-tick terminal, TimeStop, stop/forget and nonempty full-coordinator rewind.
