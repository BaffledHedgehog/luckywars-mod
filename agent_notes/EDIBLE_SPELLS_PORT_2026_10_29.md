# Ordinary edible spell source port — staged, not qualified

29 October 2026. Source root (read-only):
`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data`.
Only this note and new `LbcEdibleSpells.java` belong to this bounded task.
No builds, servers, resources, RP, deployment or commits were performed.
The full original Chaos pool and its full dependency release gate remain intact.

## Native integration contract

Constructor `LbcEdibleSpells(GameController)`; root supplies `items().edibleSpells()`.
`consumed(ServerPlayer, ItemStack)` receives the original source components at
the native consume-item trigger boundary and never debits a stack or mana.
`using(ServerPlayer, ItemStack)` is separate: source lava/snow criteria use
`minecraft:using_item`, including interrupted use. Root's native
`UsingItemTrigger.trigger` hook must call this once per actual trigger.
Do not additionally route those two flags through completed consumption.

The initial hypothesis that consume rewards necessarily occur after the item
debit was unsupported. Root identified the actual native criterion callback
before stack debit. Consequently slab rewrites run immediately at that boundary;
there is no invented one-tick queue. A native end-to-end food check must confirm
this ordering with source component food, count and cursor/crafting assertions.

Required root integration:

* Initialize/getter, native consumed/using callbacks; `tick`, `loaded`, `unloaded`,
  `forget`, `stop` alongside existing services. Tick owns no manufactured clocks.
* `LbcExoperso.edibleBurst(ServerPlayer,Vec3)` delegates to existing private
  `impact`: preserve its series, required players, shared blast queue and native
  achievement accounting. A direct second explosion engine is not used.
* Include `LbcEdibleSpells.wild(DamageSource)` in shared trap friendly fire.
  It admits only the authored self-arrow source and this service's native
  fireworks/fangs. Native arrow type/text is retained for eat_damage.
* At `EvokerWandMixin.dealDamageTo` HEAD, before the existing wand-tag branch,
  route managed edible fangs to `edibleSpells().fangHit(fang,target)` and cancel
  that vanilla method. This is the actual native fang attack tick, with native
  animation/lifetime. Vanilla otherwise excludes the owner and allies; source
  ownerless summon does neither. The hook preserves WILD6 magic with author
  credit, protected roles and ordinary native damage/enchanted attack effects.
* In shared indirect-kill source resolution, resolve managed edible child via
  `edibleSpells().owner(direct)` and its original wild cause. Native lightning
  has setCause, fangs native owner, fireworks projectile owner, dropped rewards
  native thrower and current epoch before insertion. The service does not
  impersonate a creeper to cause explosions.

`matches`, `matchesUsing`, `managed`, `wild(Entity)`, `wild(DamageSource)`,
`owner`, `polymorphCount`, and `snapshot` expose bounded diagnostic state.
Ordinary dropped book/slab rewards are not tagged as short-lived managed actors:
they retain native item persistence and shared dropped activation on reload.

## Exact criterion map and behavior

`lbc:advancement/eat_*.json` resolves these 25 consume flags. Unless noted,
the helper is `lbc:function/other/eatables/<suffix>.mcfunction`. Criteria exclude
nexus and imprinted. Root shared participant, stage, dimension and TimeStop
admission applies. There is no invented blanket MAG/TECH food condition;
existing spell engines retain their own target/suppression rules.

| Raw flag | Source native consequence |
| --- | --- |
| eat_damage | 15 arrow damage by self; native arrow damage type |
| eat_fire | fill the centered3cube with fire, replace only minecraft:airs |
| eat_explode | immediate native radius4 TNT blast, shared unprotected source |
| eat_big_explode | workingitems/exopersoexplode: existing32 radius20 burst points/series |
| eat_chaos_explode | lightning; mobs within10 Wither80ticks amp9 through caps; original five-explosion rocket components; fuse0 TNT |
| eat_haste | Haste1200ticks amp127 |
| eat_strength | Strength1200ticks amp2 |
| eat_weakness | Weakness600ticks amp9 hidden |
| eat_poison | Poison200ticks amp9 through caps |
| eat_wither | Wither600ticks amp0 hidden |
| eat_regeneration | Regeneration200ticks amp10 through caps, hidden |
| eat_buffs | Invisibility/Speed/Jump200ticks amp1 hidden |
| eat_lightning | exactly52 native lightning bolts at eater |
| eat_psycho | add60 scared through existing live Psycho affliction consumer |
| eat_polymorph | native jeb_ sheep, hidden name, empty death loot, knockback base10; native Spectator mode and camera on sheep |
| eat_teleport | native spreadplayers teams=true, radius1000 around eater; no_borders0 uses origin0 radius190 |
| eat_teleport2 | same, unbounded radius300 |
| eat_slab | original global inventory/equipment/cursor/crafting/entity/item conversion described below |
| eat_book | ten native transformables_spells rolls +one transformables_staff_all; pitch-40; three20tick hidden debuffs; source sound selector radius10 |
| eat_evoker | actual native fang at feet, WILD6 magic on native attack tick via required hook |
| eat_cum | original six sound pitches0,.2,.3,.5,.8,1; clear effects; Poison/Wither/Nausea/Slowness200ticks amp0 hidden; Saturation2000amp0, Regen2000amp10 capped, Speed/Strength2000amp1 hidden |
| eat_danmaku | exactly360 homing cards yaw1..360 pitch-10, then360 ordinary cards yaw1..360 unchanged pitch, at eyes-.5; original sound per card |
| yellow_lightings_bottle | Psycho live twitching+25000/piss+100000; mana+100000, baseRegen+20,maxMana+10000; have_lucky_effects |
| eat_glowing | Glowing/NightVision600ticks amp0 |
| eat_damage_all | workingitems/omegaflag, existing global WILD cactus6 factory and source trial test; no mana/debit |

All native effect grants use ItemEffectLevels. No static scoreboard-only
stand-ins replace the existing Psycho, cold, mana or projectile consumers.
Flags may coexist: each independent source criterion is dispatched.

Two additional criteria are using-item triggers:

| Flag | Source consequence |
| --- | --- |
| lava_bucket | other/eatables/lava: self lava4 on each actual using trigger |
| snow_bucket | other/eatables/snow: workingitems/icesword_player cold ladder, shared Frost.add(self,self), original snow sound |

Source icesword_player references a missing `icesword/0` function for an
existing amplifier0; this port reuses the already ported common cold engine,
not a runtime function interpreter. The accepted native cold/frozen contract
and ItemEffectLevels remain shared. No silent source-file creation occurs.

## Slab closure and native reward resources

`slab_at_player`: independent native random-eatable-slab roll for all four armor,
offhand, cursor, four crafting, nine hotbar,27 inventory slots. Native owner item
count is not separately debited. `slab_at_mob`: all six equipment slots and
DeathLootTable `lbc:random_eatable_slab_random_count_64`. Source ordinary entities
spawn one roll then native kill; markers also native loot insert at their block
using `lbc:random_eatable_slab_random_count_27`. Existing dropped ItemEntities
keep their count/components and transmute only item ID to brick_slab.
Shared protected roles are excluded; no enemy/team-only filter is invented.

The three slab tables plus transformables_spells and transformables_staff_all
already exist under src/main/resources. Root must explicitly package their
transitive tables in the staged artifact and qualify all resulting mechanical
outputs. Their presence alone does not establish broad reward closure.
No public ready pool or unrelated catalog list is enabled here.

Only literal native spreadplayers, native marker loot insert, and native
DeathLootTable update commands are used where public setters would otherwise
lose vanilla semantics. No function commands, source parsing at runtime, or
generic command/function interpreter is introduced.

## Polymorph lifecycle and open source decisions

The edible differs from the existing nonplayer polymorph wand. It does not
discard the eater or register a match-observer role. Its native sheep/camera
has no invented TTL or automatic gameplay restoration on sheep death. The
accepted body record retains the original mode for owned cleanup on forget,
epoch/end/stop if its camera is still owned. Native server camera removal on
sheep death remains native. Managed orphan actors are discarded on reload;
item rewards preserve native item lifetime independently.

Questions for source review (not chat blockers): whether the source's unbounded
Spectator transformation should eventually restore Survival is not specified
in its helper. This implementation keeps the literal no-gameplay-timer behavior.
MAG/TECH author exceptions for these foods were not supplied; no blanket gate
is inferred from Chaos armor's separate MAG-only decision.

Excluded ownership: all attribute-food helpers belong to chaos_core; named
golden/diamond/netherite/human/fish/banana/oreshek/gain_bucket belong to root.
KFC/eat_shit remain LbcKfcFood. Halloween pumpkin_counter/chorus helpers are
separate event-food dependencies outside ordinary other/eatables spell scope;
checks reported those to root. No duplicate handlers are created here.

## Qualification still required

Native raw food components through actual consumption/use, each trigger and
one native item debit; unfinished use must still run lava/snow. Verify source
effect durations/visibility/clamps,52lightning,720native cards, firework payload,
real fang self/allied/role damage, causal native blasts/TimeStop, all global slab
branches/container failure, native book loot and delayed shared drop activation,
polymorph real camera/mode/unload/epoch cleanup, full teardown and neighbor suites.
This source-only implementation is staged and has not been compiled or run by
this agent; full Chaos release remains gated.


## Root integration review corrections

The root also connected independent `gain_bucket` to the same native reward-drop
primitive and packaged the original imba/buckets and imba/bucket_rofl tables.
Completed-consumption payloads now total26; using criteria remain separate2.
Entry admission uses common protected-role/observer and input-TimeStop checks.
Every native child retains one immutable accepted WILD Owner, available through
`cause(Entity)`. Shared indirect attribution and TimeStop capture preserve this
cause before native firework removal; fangs reuse their accepted child's cause.
The shared damage wrapper also preserves source position, difficulty scaling
and localized death message. This is integration, not a second food damage engine.
