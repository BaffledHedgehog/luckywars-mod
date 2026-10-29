# Food lifecycle wrappers — staged, not qualified

29 October 2026, base HEAD0864757. New owned production file:
`src/main/java/me/skywars2/game/LbcFoodLifecycle.java`. Only that file and this
note were edited. Legacy lbc/lbcr were read-only. No builds, server runs,
resources/RP, protected Java edits or commits. Full original Chaos release gate
is unchanged; this is not public-ready or a full transitive qualification claim.

Source root:
`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data`.
The prior [food reward source audit](FOOD_REWARD_SOURCE_2026_10_29.md) remains
the broader acquisition/advancement boundary.

## Scope and exact consume semantics

`LbcFoodLifecycle(GameController)` supplies `consumed(ServerPlayer,ItemStack)`.
It never debits a stack, feeds the player manually, calls a second KFC/CannedFood
consumer, or replaces the native consume pipeline. Call it with the original
source stack at the native ConsumeItemTrigger callback boundary, before other
custom rewards can kill/transform/teleport the participant. In particular, a
polymorph golden apple still qualifies for gluttony before native Spectator mode
would block new food admission. Native code retains the one original stack debit.

Each applicable flag is independent:

| Criterion/source | Native consequence |
| --- | --- |
| gluttony -> other/food_counter | food_count+1 for exact all_food item IDs |
| stewused, inf:1 -> other/infinitestewused | immediate mainhand replacement with one authored cooked-beef infinite steak |
| old_stew_used, inf_old:1 -> other/infinitestewused_old | scheduled_eating tag; global helper one actual tick later replaces mainhand with one authored rabbit stew |
| eat_halloween_food, halloween_food:1 | pumpkin_counter+1 and only its exact matching real criterion1..100 |
| halloween_chorus:1 -> other/halloween/eat/chorus |20native damaging falling pumpkins, exact random motion, particles/sound and native spreadplayers10 |
| badapple, bad_apple:1 -> other/bad_apple |15 authored vanilla effects via ItemEffectLevels/EffectLayers |

`countsAsFood` hardcodes the exact41 source #minecraft:all_food IDs, including
milk_bucket and honey_bottle. It does not infer membership from FOOD/CONSUMABLE,
raw flags, model, nutrition, or lore. Existing registered KFC_CHICK and
GOVNO_EATEN map to their source cooked_chicken/cookie carriers. A banana stick,
edible slab and arbitrary bucket do not qualify merely because edible.
The gluttony source helper revokes its root immediately; every actual accepted
native consumption can increment. CannedFood's passive inventory clear never
calls this completion hook and retains its own existing food_count increment.
KFC's own independent outcomes remain exclusively in LbcKfcFood.

The source infinite replacement components are constructed literally: white
nonitalic name/lore keys infinite_steak/infinite_steak.lore, inf:1, tier_c;
old horn/horn.lore, inf_old:1 with no invented tier/model. Native item defaults
retain nutrition, remainder and normal item identity. Replacement always targets
MAIN_HAND, including consumption from offhand. Native consumption still shrinks
the original object, independently of the newly replaced mainhand object.

## Native one-tick old helper

`tick()` runs once per actual server tick and checks `now > scheduledTick`.
Every inf_old callback resets the one global helper deadline, as source
`schedule function ... 1` defaults to replacement. All accepted queued consumers
are replaced together; there is no per-item private delay clock or simulated
production tick. Actual TimeStop pauses delivery. The accepted epoch, current
participant/role/dimension admission and scheduled_eating tag are rechecked at
delivery. Death/disconnect forget clears its owned pending tag; stage/end/stop
clears all owned pending state. It never restores the old inventory snapshot
over an intervening native selection change: source explicitly replaces the
then-current mainhand.

Source helper applies to all online tagged players and removes scheduled_eating
from all online players. This port owns only tags it scheduled in the current
match, rather than adopting arbitrary stale pre-port/admin tags as new work.

## Halloween count and real task

The root helper does not revoke itself. `players_tick.mcfunction:70` revokes
eat_halloween_food each tick. A per-player actual-server-tick guard therefore
permits one pumpkin_counter increase per tick, unlike immediately revoked
gluttony. It does not decrement a synthetic cooldown or fake player ticks.
Counts1..100 call native PlayerAdvancements.award on exactly Integer.toString(count)
of `lbc:true_advancements/event/eat_halloween_food`; counts outside that range
grant no criterion. The task is never granted wholesale via mana.award.

Root must package the actual100-criterion advancement and its parent. Its source
completion rewards.function is `lbc:swrg_kit_integration/gain/default_kit`.
No existing native default_kit bridge was found by focused Java search; root was
told this explicit transitive edge. This wrapper leaves native advancement
completion in charge and does not invent a replacement kit or execute source
functions. That reward is still part of the full release qualification gate.

## Halloween chorus native actors

Twenty vanilla-type FallingBlockEntities spawn at original position Y+.5,
BlockState pumpkin, HurtEntities=true, FallHurtAmount5, FallHurtMax10,
source tag pumpkinfall. All twenty spawn before the source draws motion for
each: X/Z integer[-50,50]*.01, Y integer[50,150]*.01 using native level RNG.
Native gravity/collision/fall damage/drop/placement/lifetime remain authoritative.
No source TTL, falling-distance estimate, fake hurt, creeper or manufactured tick
is added. Native26.3 falling-block tick was inspected: Time0 increments and
normal gravity/motion runs; this factory does not need to steal a source block.

The original zombie.break_wooden_door sound and150pumpkin-item particles at
Y+1 before spread are retained. Native `spreadplayers ~ ~ 0 10 false @s` moves
the consumer, then another150particles emit at the result. Existing global
TeleportMomentum mixins preserve the consumer's full velocity; this service
does not zero or rotate it. A failed native spread does not undo accepted actors.
Carrier-native chorus fruit effects still execute in their native pipeline.

Each cast shares an immutable accepted WILD Owner and epoch. Initial Pumpkin
objects retain that cause on the direct source; a separate weak cause map also
retains it through converted native entities and queued DamageSource references
after active/unloaded tracking changes. Native unloaded-to-chunk state is keyed
by UUID for re-admission in the current runtime/epoch. Stale/orphan actors are
discarded. Accepted falling consequences persist across the author's death or
disconnect; only new wrapper admission depends on a living participant.
Native dropped pumpkin items inherit the same cause while their actual parent
tick runs. Actor maps do not pin already removed sources indefinitely.

## Bad apple graph and duration correction

The actual consume root `{bad_apple:1}` calls only `other/bad_apple.mcfunction`.
There are no Psycho calls or transitive function edges in this helper. The
`bad_apple_imba` dropped-item/show graph is a different flag and is not silently
substituted for a consumed apple.

Effects amp0, particles/icon visible:

* Absorption, ConduitPower, FireResistance, Haste, HealthBoost, Invisibility,
  NightVision, Regeneration, Resistance, SlowFalling, Speed, Strength,
  WaterBreathing:300seconds =6000ticks.
* InstantHealth argument30 =30ticks.
* Saturation argument300 =300ticks.

Native EffectCommands.computeDurationInTicks returns the literal duration
argument for isInstantaneous; SaturationMobEffect extends InstantaneousMobEffect.
Applying a universal seconds*20 conversion here would be wrong. Existing
ItemEffectLevels and EffectLayers own caps, independent effect layers, expiry
and interaction with the vanilla carrier effects. Root was also notified that
the earlier staged edible cum saturation100 must be100ticks rather than2000;
that existing service was not edited under this task.

## Root integration APIs

* Getter/constructor, completion callback before edible/custom mutation, and
  actual `tick`, `loaded`, `unloaded`, `forget`, `stop` service lifecycle.
* Add early `foodLifecycle.inheritChild(entity)` to native Items.entityLoaded;
  it claims only ItemEntities created inside this service's parent tick scope.
* In FallingNetheriteDropMixin (before existing tags), managed lifecycle
  FallingBlockEntity -> `withTick(actor,nativeTick)`. That wraps actual native
  placement/support writes in the existing WILD terrain provenance engine.
* Existing generic native conversion/teleport hook in ZombieTntDropMixin:
  `foodLifecycle.inheritConversion(parent,converted)` before insertion.
* IndirectKills.playerSource -> owner(Entity); babylonCause -> cause(direct).
  Existing inheritDamage now captures babylonCause before TimeStop queues damage,
  preserving accepted ownership after actor removal. Do not reduce it to the
  current online author alone.
* Wands.allowDamage early managed lifecycle source -> canAffect(source,victim),
  shared Damage trap-friendly-fire -> static wild(Entity), Items accepted-source
  observer exception includes managed current-epoch lifecycle actors. Native
  fall damage type, armor/Resistance/Greed and protected roles stay shared.

Diagnostics: `matches`, `countsAsFood`, `sourceFoodCount` (41), literal replacement
factories `infiniteSteak`/`oldInfiniteStew`, `pendingCount`, `actorCount`, `snapshot`,
`managed`, `wild`, `cause`, `owner`, `canAffect`. No public-ready catalog flag is
enabled. No source tag JSON needs packaging for the fixed41-ID classification.

## Source questions and differences recorded for review

Source eat_halloween_food has no dimension condition; the other five roots
exclude nexus/imprinted. Existing native sealed-dimension/global admission is
retained for all wrappers as instructed, including Halloween. No MAG/TECH filter
is inferred. Existing effect/physics/TimeStop policies still govern consequences.

Source mainhand replacement on offhand inf/inf_old is retained literally.
Whether the author eventually wants triggering-hand replacement is an open
behavior choice, not silently normalized here. The source's root revoke is in
players_tick, so exact relative phase can permit another Halloween callback at
that boundary; the port uses one native server-tick guard. A native zero-time
food/offhand fixture should qualify that boundary separately.

Still required: actual milk/honey/KFC/cookie and nonmember food classification
through native completion, no duplicate passive count, current/old infinite
mainhand/offhand plus one-tick schedule replacement and interruption, real100
individual criteria, bad-apple layered expiry/instant tick semantics, actual20
pumpkin motion/damage/placement/drop/TimeStop/portal/unload/cause and complete
teardown. No such build/runtime qualification is claimed in this note.


## Bounded native fixture authored (29 October 2026; not yet executed)

`FoodLifecycleChecks` exposes `run`/`start(GameController)`, `boolean tick(GameController)`
and `cancel(GameController)`. Root must call tick after the ordinary Items tick;
its deadline is 100 actual server ticks, normally about 10?15 ticks. It never
changes production clocks or advances private queue fields. No build/server was
run by the fixture author; all PASS statements remain conditional on native execution.

An independent exact41 item list is compared against every registered item,
allowing only the existing native KFC and edible-cookie carrier aliases. All41
native vanilla completions and both aliases use actual `finishUsingItem`, check
one debit and one `food_count` increment; only KFC's existing20-way native RNG
call is scoped to harmless outcome20. The source slab is an actual native leaf
with FOOD/CONSUMABLE outside the original41 tag and must not count. Native
`inf_steak`, `legacy_luckyblock` old-stew leaf, `halloween/lucky_items`, `badapple`
and slab entry expansion must retain authored components/flags: missing native
codec normalization causes an explicit fixture failure, never synthetic flags.
This expansion does not enable their broad acquisition pools.

Current infinite mainhand/offhand behavior, dormant inventory payload, native
one-tick old-stew mainhand/offhand replacement (including a changed mainhand),
two Halloween numeric criteria across actual ticks, same-tick duplicate callback
guard and three ordinary bad-apple layers are covered. One20 native pumpkin batch
checks exact source block/tags/damage parameters, RNG motion bounds/lattice,
captured WILD owner/epoch, persistence after owner forget, five actual TimeStop
ticks and resumed native movement. Nineteen actors are removed synchronously;
the retained actor is removed before landing and its captured cause must remain
resolvable. This does NOT qualify actual falling collision damage, queued damage
replay, native landing/drop/conversion, death/disconnect/portal/unload, the full100
progression or peak load.

The fixture snapshots the borrowed player with the established complete State
helper, expands item-used/broken stats to every registered carrier, preserves
advancements/scores/profile/attributes/effects/food/combat/equipment cache/abilities/
input/native connection attribution, uses actual client load/teleport ACKs and
restores all borrowed state before returning the prior listener. A bounded empty
31x31x10 air volume and every source/provenance snapshot are restored exactly;
only newly forced chunks and synchronously scoped alias drops are cleaned up.
Initialization errors and cancellation take the same teardown path.

Exact completion edge rechecked: `true_advancements/event/eat_halloween_food`
rewards `lbc:swrg_kit_integration/gain/default_kit`. That original function contains
only a blank title and `new_swrg_kit` subtitle. It has no real kit grant or further
function edge. The fixture does not complete100criteria or invent a kit reward.
Full Chaos/full-food release gate remains unchanged.

Root resolved the completion edge during integration: native criterion completion
will emit that exact blank-title/translated-subtitle transition directly. There
is no missing real kit grant to invent. The packaged task intentionally has no
runtime function reward; the fixture still leaves final100 completion untested.


## Independent integration/source review (29 October 2026; read only Java)

Root's current native callback composition runs lifecycle before KFC/Aimbob,
attribute, edible, named and Oreshek consumers, then the existing skill callback.
Thus general food counting precedes potentially fatal/teleporting/replacing
custom consequences. The consumers match independent flags; inventory adoption
is not a second consume listener and does not debit stacks. No duplicate KFC or
CannedFood completion dispatch was introduced by the inspected composition.

`LbcItems.adoptRawFoodStatuses` attaches the existing owners for Blaze, awakened
acid, Erecto, Psycho, Electro, Frost, acid corruption, KFC, Blind, Babylon, Quiver,
Kiuas, mana potion timers, Homing and legacy effects. Babylon/Quiver import only
the positive difference from existing queued contributions. Other adapters set
or attach canonical score values rather than issue another dose. KFC's existing
score loop plus owner synchronization must be considered together: target and
shock are synchronized from private owner state after their raw score write,
so those two calls do not by themselves establish a double grant.

End cadence/units: source `tick10s` schedules itself10s and invokes
`players_10s` outside Nexus/Imprinted; its End/non-Greed branch invokes
`damage_end` then the native outside_border macro. Current FoodStatuses uses
one `serverTick % 200` branch, the same `(end_damage+1)*6/5` Java int operations
and a positive void_resistance gate. This is10seconds, not10ticks. Source global
scheduled phase and native absolute modulo phase can differ; neither source
phase was claimed by the bounded food fixture. No new End double-consumer was
found in Java.

Target source score is in seconds. Current Homing adoption multiplies a
**long** by20, keeps long native remaining ticks and publishes ceiling seconds;
there is no Integer.MAX_VALUE stew overflow at this conversion and no missing
20 multiplier. Homing's actual native tick drains one tick. Nitro already has
Techno's200 active-tick refill and existing fuel consume; nanobots already run
through ArmorSets'20-active-tick pulse, including resistance, incoming tier loss
and cooldown decay. Both inspect canonical counters, so neither needs another
food-owned manager/adoption grant.

Remaining downstream boundary is concrete: `sleep_time`, `lolidefecation` and
`greed_prestige` are faithfully written by stew/KFC, but no active Java reader of
these three was found outside reset/write paths. They are NOT durations to drain
with a new generic status clock. Source `player_speed` updates sleep_time and
`workingitems/witch_gens/indolence` selects its3600-threshold owner;
`witch_gens/lust` selects the64-threshold lolidefecation owner;
`witch_gens/greed` compares greed_prestige when carrying greed_greed and cursed.
Their complete Witch Generation acquisition/selection families remain separate
full-chain dependencies. In source, lolidefecation is additionally a mined
mushroom_stem statistic; writing the stew's numeric maximum alone does not port
that native mining/event route. Greed prestige is also incremented by source
`other/greed/pickup`. Do not treat these intentionally preserved ledgers as
implemented Witch abilities or invent periodic damage/buffs for them.

Independent17-resource check: parsed every file in
`build/verification/named-foods/resource-normalization.json`, parsed its ORIGINAL
legacy counterpart, recursively adapted only functions->modifier,
function->type, loot_table name->value, and the bananas uniform discriminator,
and compared complete decoded JSON trees. All17 matched exactly: badapple,
bananas, food, food_old, foodblocks, halloween/lucky_items, imba/enderpearls,
imba/food, imba/foodblocks, imba/slabs, inf_steak, oreshki_10,
legacy_luckyblock, stews, stews_old, spawnegg and wtf_item. This preserves all
weights/qualities/counts, payloads, text/lore/styles, models, native consume
components and original adverse effects without flattening source text. It is
source-preservation evidence, not native codec/runtime qualification. Original
food warning remains: netherite stew deliberately issues harmful/huge/infinite
native effects and active counters; do not replace them with benign presentation
or claim every downstream family complete. Full food/Chaos release remains gated.
No Java/resource/build input was edited, no build/server run, and fixture Java
remained frozen throughout this independent review.
