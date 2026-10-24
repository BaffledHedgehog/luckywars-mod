# Fartus Nadristailo: qualified native implementation

Audited 24 October 2026. Source is read-only:
`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`.
The bounded family consists of all 22 functions under
`function/workingitems/firtusnadristailo/`, plus its dispatch, Academy,
melee, one-second and presentation dependencies. `LbcFartus.java` implements
the bounded family; integrated runtime qualification is recorded below.

## Ordinary staff

`config.mcfunction:120` sets cost1250. Main/offhand default dispatch checks
funding and a magic stopper within20; shared native guards additionally apply
observer, antimagic, Time Stop and Overload contracts. `cast` chooses once
between two equal outcomes, emits five actors, charges once and triggers the
usual wand/trinket hooks. Original mage-set max-mana bonus remains shared.

Bananas use `lbc:banana`, carrot-on-a-stick, scale1, identity local rotations,
and initial velocity1.5 times look plus independent uniform[-.5,.5] XYZ spread.
The source Paper `PhysicalObjectPhysics.java` supplies gravity.08, maximum
axis-sweep step.5, centered1x1x1 collision cube, X/Y/Z clipping, bounce.9,
ground horizontal friction.8 and vertical friction.78, air friction1.
It updates orientation from motion. Banana `data.lifetime=100` is enforced by
the generic `raycastselector.mcfunction`: increment Lifetime, dispatch the
family, then read data.lifetime and kill when reached. Ordinary contact selects entities
in the source point selector and excludes same ID/team. Native physical contact
must instead preserve the shared friendly-mob/allied-player rule (user).

Labels use `lbc:banan`, repeating-command-block, scale.5, source left quaternion
[1,0,0,1], right identity; yaw fan+30,+15,0,-15,-30. Each tick checks targets
and advances.4 three times, dies on non-air, and expires at Lifetime60.
Only one target receives WitherII, PoisonIII and SlownessI for15s.
The original temporary player-tag selectors protect caster/allied players.
Native swept collision replaces tag scans and point tunneling; technical roles
and active Greed are protected by common guards.

Both banana forms apply Blindness, Slowness, Darkness and Weakness amplifier99
for2s, plus native Instant DamageIV (source duration40s, amplifier3).
They play `minecraft:lbcsounds.bananoponos`, sneeze40 and yellow-concrete-powder40.
Labels invoke the source random-low sound and emit one model item particle.
The native effect path must assign the current projectile owner and preserve
the original causal stamp through continuing poison/wither/blindness deaths.

## Academy trial and evolved sword

Trial `check_upgrade_set` recognizes A A B B, with either initial outcome A.
Failure resets score to0 and then immediately to1; previous outcome is updated.
Score4 marks readiness. Source writes only SelectedItem even for offhand casts;
the shared native contract requires the actual initiating hand/stack (user).

Academy promotion applies tierX and `fartus_upgrade`: retained yellow name,
model `lbc:palma`, six replacement lore rows, attack-damage modifier19,
attack-speed modifier-3.5, entity interaction range modifier+10. Every second
while held mainhand it replaces damage with19+10 times total carried `govnoq`
stack count; the empty `sword_tick_1s_damagebuff` adds no behavior.
The source melee dispatcher adds one `fartus_sword` per accepted melee event
with evolved Fartus in selected mainhand. The HUD is `    \uE103 §fx` followed
by the yellow charge score. Main/offhand evolved right-click requires3charges,
subtracts3, and emits five homing bananas without mana cost.

Homing starts from source random yaw/pitch offsets independently[-50,+50]
degrees. Each of three substeps turns toward nearest enemy eyes without a
range cap, using the source.05/.4 curvature construction, moves.4 and checks
contact. Inside non-air it reverses orientation. Its declared lifetime100 is
not consumed by PhysicalObjectPhysics because this actor lacks physical_object.
Generic raycastselector processing nevertheless enforces100ticks for homing
as well as ordinary bananas. Enemy-only acquisition is mandatory (user), while
physical contact continues using the shared projectile allegiance rule.

## Presentation and scope

Ordinary loot table `loot_table/fartus_nadristailo.json` retains model
`lbc:fartus`, tierS, yellow `fartus_nadristailo`, and six exact lore rows,
including the source Tinkist00 Halloween2025 attribution. Evolved presentation
comes from `item_modifier/fartus_upgrade.json`; resource-pack auditing is a
separate family artifact. The author resource pack stays read-only (user).
Craft, acquisition pools, unlock rewards and source helper `piss_particle`
are not implicitly enabled by this source audit. The latter is a rare-ray
selector branch, with no live family producer identified here.

## Required native hooks

Family service: register, use(owner,hand), tick, stop, forget,
entity-unload handling, accepted melee charge callback, and HUD.
Shared integration: LbcItems ownership/access/lifecycle; wand dispatch;
Academy enum/promotion; item/loot presentation; protectedWandProjectile only
for magic labels/homing, not physical ordinary bananas; native projectile
owner/reflection; shared indirect status application with captured cause.
All source effects, damage, reflection and homing must resolve getOwner()
at impact rather than retaining only the initiating caster.

Native source geometry uses one unsaved Polymer marker projectile paired with
an item display. Models are oriented before either entity enters tracking and
updated in the same family frame. The physical ordinary banana uses native
block collision shapes and source X/Y/Z substeps; swept entity contact replaces
the original overlap-only selector. Homing is loaded-enemy-only with source
curvature; labels and homing reject incidental impulse through the common
magic-projectile guard. Only ordinary bananas retain physical reactivity.

The evolved ability binds the normal spell/right-click path, as confirmed by
`fartus_nadristailo_evolved.lore.3` and both spell1_upgraded dispatchers. It has
no F binding. The `wtf_item` native registry constant is also provided for the
craft donor; its independent melee/Chef ability belongs to the separate native
WTF service and must not be inferred from this family implementation.


## Integrated runtime qualification (24 October 2026)

Build `b0dfd5ca` passed74 recorded suite results in two actual mode1 matches (168.90s), including Fartus/Chef and the existing shared regressions. Exact 311 sources/857 classes/2797 resources match the tested installed JAR. Main installed while stopped, rollback `0fb48f1a` retained, server not started. Evidence: `build/verification/fartus-final/`. Native synchronous tick boundaries are explicit simulations; no elapsed ten-second wait, real client pixels, actual disk restart/disconnect or general-world/load guarantee is claimed. This integrated result supersedes the earlier pending-status statements by the individual source/check agents.

Runtime log retains only the existing fake-bot Polymer packet-context ERROR pair during bot admission. No new error lines occurred; exact lines are recorded in the qualification manifest.
