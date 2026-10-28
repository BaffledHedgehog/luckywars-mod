# Live lbcr reward consumers - 29 October 2026

Read-only source/Java audit at HEAD `1366cc0`; only this new note is owned.
Scope: `krazha`, `shetka`, `drone_pult`, `lightning_bolt`. The seven raw spells,
bean and electric checks are deliberately excluded. No build, server, production,
RP or commit changes; no runtime qualification claim. Full-original-pool release
gate and `root_PENDING` remain. `HostileElectroChecks.java` stays frozen.

Legacy root: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data`.
Java references below are relative to `src/main/java/me/skywars2/game`.

## Classification

| Consumer | Existing native behavior actually inspected | Real remaining work |
|---|---|---|
| `krazha` | Native item dispatch, roles/MAG, ray queues, ItemStack components; Communism and Greed examined | Missing consumer AND missing equivalent player-hand theft engine. Communism changes nonplayer mob allegiance; Greed reflects projectiles/protects its owner. Neither transfers another player's complete held stack. |
| `shetka` | Resident ray queue, queued terrain writes, provenance; Stonekivi and Consuming erasure examined | Missing consumer AND narrow source-equivalent erase engine: first non-air-only sample, one ordinary native setblock-air terminal. Existing sphere/template/persistent erasure abilities are different. |
| `drone_pult` | Technology rays, native TNT drone/bomber, LargeFireball homing, native blast/ownership infrastructure | Missing consumer AND homing item-display missile engine. Existing bat/cart/hoglin producer and flying hoglin bomber cannot be aliased to this controller. |
| `lightning_bolt` | Native LightningBolt engine, cause/fire/tick lifecycle, electrical classification and separate spawn-egg material alias | Missing raw short-ray selector/input bridge. Reuse existing native lightning terminal; no new lightning physics or damage type. Paid plural `lightning_bolts`, Infinite Bolt and spawn-egg use have different targeting. |

This is a behavior comparison, not merely a missing-literal search. The live
`LbcWands.use` recognizers/Spell dispatch were followed: unrecognized stacks fall
through `spell==null -> false`; `raycastspell:1` alone is not a generic execution
bridge for arbitrary legacy functions. The three missing behavior families were
compared against their closest existing native services below.

## Live input and common boundaries

[workingitems/spell.mcfunction](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/spell.mcfunction)
selects upgraded versus ordinary mainhand and resets `RC_Raycast` afterwards.
[spell_left.mcfunction](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/spell_left.mcfunction)
does the same for offhand. This audit expands the ordinary selected branches;
it does not assume upgraded dispatchers contain every ordinary flag.

[lbcr/raycast.mcfunction](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast.mcfunction)
sets `raycaster`, `Distance=0`, invokes these four branches at lines8/20/31/33,
then removes `raycaster`.
[raycast_left.mcfunction](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_left.mcfunction)
uses lines8/20/31/32. Offhand theft begins eyes+.5 forward; the other three begin
at eyes without that offset. None of these selected function chains debits mana.
Do not invent staff prices, wand-casted callbacks or armor bonuses for them.

Entry MAG marker radius20 gates theft/brush/lightning; TECH marker radius20 gates
the controller. Continuous gates differ below. New consumers must also use the
project's already-approved role, TimeStop, sealed-dimension, resident-chunk and
causal-author policies. Those common primitives do not supply missing engines.

## krazha - complete theft closure

Exact immediate chains: `raycast_loop6 -> raycast_end6` and
`raycast_loop6_left -> raycast_end6_left`. No other function helpers are called.
Each sample increments Distance, emits two forced transition-dust particles
from `[.149,0,.286]` to `[0,0,0]`, scale1/speed.01, then tests nonspectator players
in positive unit-box `dx=dy=dz=0`, distance<=15, excluding `raycaster`.
Continuation requires the broad source `#minecraft:airs`, Distance<=130,
and advances .5. Main samples0..130 therefore span65 blocks/131 checks; offhand
has its additional initial .5 offset. This is not a simple radius15 acquisition.

The endpoint independently selects **nearest `@p[gamemode=!spectator]`**, with no
raycaster, range or same-team predicate. It copies the **entire** selected
`weapon.mainhand` into the caster's mainhand, then clears the donor's mainhand.
Left copies/clears **offhand**, not mainhand. Independent predicate `chance5`
(random_chance .05) then replaces the acquired stack with exactly one
repeating_command_block: name `what?`, lore `what?.lore.1/2`, model `lbc:trusi`,
style `lbc:tier_f`. Wololo sound master volume1/pitch1; Distance1000 ends the ray.
The donor endpoint can select a different nearest player than the trigger;
in close overlap it can even select the caster. Do not silently change this
while claiming literal source equivalence.

Main recursion tests player MAG (`nomagic_active`, nonspectator, radius16) and
marker MAG20. Left recursion tests player MAG16 but has no repeated marker gate
after entry. This source hand asymmetry needs an explicit preservation/change
record.

Acquisition: `lbc:shadowglove`, carrot_on_a_stick, model `lbc:shadowglove`, raw
`{raycastspell:1,krazha:1}`; nested from `lucky_items`/`lucky_items_high_quality`.
Required implementation: both-hand raw input bridge; finite resident source
samples/particles/gates; safe full-stack copy+donor clear; independent 5% junk
replacement; source sound; both inventories reconciled after mutation.

Native comparison:
[LbcCommunism.java](../src/main/java/me/skywars2/game/LbcCommunism.java):61 onward
reteams/owns nonplayer mobs, resets their targets and sets friendly_tuntija;
it never implements this player inventory transfer.
[LbcGreed.java](../src/main/java/me/skywars2/game/LbcGreed.java) provides absolute
immunity/projectile reflection. Those are real working engines but no theft
alias. Native ItemStack and queue primitives remain reusable.

## shetka - a single ordinary native setblock

Immediate chain: `raycast_shetka -> raycast_shetka_end`, no other function calls.
Increment precedes inspection; white dust scale.5/count1/speed0 at every sample.
First block outside `#minecraft:airs_only` runs ordinary `setblock ~ ~ ~ air`,
sets Distance61, and emits white dust count5/speed.2. Until then, Distance<=60
continues by .15: 61 samples, maximum9 blocks. Marker MAG20 is checked at each
next positioned sample. No mana, item clear or cooldown is present.

Source `airs_only` has exactly air/cave_air/void_air. Source `airs` has259 entries,
including water, plants, buttons and carpets. Using the broad tag changes the
terminal; water is a required negative case. If BlockState.isAir is used,
demonstrate equivalence to these three air entries rather than assuming it.
Preserve ordinary native setblock behavior for block entities/replacement;
this audit adds no invented drop policy, bedrock blacklist or container immunity.

Acquisition is an inline carrot_on_a_stick leaf in `lbc:loliluckyblock`,
model/name `lbc:shetka`/`shetka`, raw `{raycastspell:1,shetka:1}`.
Required code: raw bridge for both hands, short sampled ray with the exact air
predicate, and one terminal native world write/causal provenance. This can use
[LbcStonekivi.Work](../src/main/java/me/skywars2/game/LbcStonekivi.java),
[LbcIndirectKills](../src/main/java/me/skywars2/game/LbcIndirectKills.java) and
LbcDropBlocks, but those primitives are not the missing source consumer.
Stonekivi.castAt has range150/250 and template/mode payloads;
[LbcConsuming](../src/main/java/me/skywars2/game/LbcConsuming.java) Silence is broad
persistent erasure with special entity handling. Neither is a compatible alias.

## drone_pult - display missile, not the existing bomber

[drone/click.mcfunction](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/misc/drone/click.mcfunction):1-3
increments Distance, looks for `#minecraft:mobs` in a positive unit box,
distance<=10, !spectator/!raycaster, without same-team/strict-map filtering.
It continues .33 while Distance<=900: up to901 samples/297 blocks from eyes.
It does not test walls or repeat TECH gate along this acquisition ray.
Successful spawn sets Distance10000. Miss performs no clear/debit.

[spawn_drone.mcfunction](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/misc/drone/spawn_drone.mcfunction):1-13:

- `clear @s *[custom_data~{drone_pult:1}] 1` removes one matching item using vanilla
  inventory order, which need not be the used hand.
- `lbcsounds.drone_signal`, player category, listeners radius50, volume1/pitch1,
  minimum volume.3.
- The first unspecified-order limit1 mob gets temporary target_for_drone.
  Missing positive lbcID2 invokes other/idgive; that target ID is copied onto
  the display. The temporary tag is then removed from all nonspectator mobs.
- Actor is item_display, golden_sword/model `lbc:drone`, fixed item display,
  interpolation1/teleport1, zero shadows and the exact original16-float matrix;
  tags homing_drone/slowraycast/player/mecha, then actived.
- Launch is **facing absolute `(0,100,0)`, flattened pitch, backwards45/up5**.
  It is not backwards relative to the caster's current look direction.

Actual recurring path:
`tick_not_timestopped:3 -> raycastselector -> raycastselector_player:43 ->
drone/tick -> drone/tick_fly`. The actor type item_display is in raycasters.
Generic raycastselector increments Lifetime, runs the player selector and only
uses data.lifetime if present; spawn gives no explicit data.lifetime/TTL.
Ordinary dispatch is skipped during global TimeStop and in nexus/imprinted.

One applicable conditional shared-wrapper helper is outside the six payload
roots: raycastselector line3 can call
[raycastselector_player_check_skill](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/raycastselector_player_check_skill.mcfunction),
whose line3 conditionally calls
[skills/homing/tick_markers](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/homing/tick_markers.mcfunction).
Its guard requires nonlegacy/global skills, positive lbcID2, chance50, no
cant_be_homing, a skill30 player somewhere and no nonspectator player within20.
The helper identifies a skill30 player whose ID equals the marker's ID, tags
that player nodmg and same-team players nodmg1, conditionally turns the marker
toward nearest eligible mobs within100, emits three forced red dust particles,
then clears temporary player tags. Drone copies its TARGET ID rather than its
caster ID, so this shared conditional can be tied to a targeted skill30 player.
No alias to unrelated fireball Curse is asserted. Preserve the established
shared homing-skill contract or record a deliberate native difference; do not
silently discard this reachable scheduler effect. These two scheduler-helper
files supplement the65-file payload-root graph below.

Tick first explodes on a block outside the broad airs tag, then calls fly only
if @s still exists. Fly identifies the target through same_id after copying
its own lbcID2 into `*temp`; its exact look-ahead/rotation expression uses .1
and moves `^ ^ ^1` per tick. Facing excludes strict_map_object at this stage.
After movement, any !spectator hitbox_entities unit-box contact/distance<=12
explodes it. Tick sound: drone_tick, hostile, radius35, volume.15/pitch1/min.04.
No explicit lost-target retirement or time limit was found in this chain.

Explode summons an invulnerable/ignited/silent Fuse0 creeper, radius8, name drone,
then kills the display. The project-wide approved replacement is a native
power8 blast without helper creeper, preserving causal author through damage,
terrain/fire/delayed kills. Released mecha displays are killed by source
`other/stopper -> other/stopper_marker_destroy` within20; the nuke-only branch
there does not apply. MAG deletes magic actors; this actor is mecha, not magic.

**Conditional identity side chain must not disappear from the audit:**
`other/idgive:1-6` increments shared ID, assigns it, removes identified/hasownid,
and invokes `lbc:resets` if the target is a player. Resets really calls
changed_inventory/sel, levelups_remove and leave_death, with further conditional
helpers. The complete65-file literal-call closure is below. Native UUID binding
can avoid those legacy provision-ID resets; record that intentional difference,
do not execute broad player resets just to imitate a legacy ID namespace.

Acquisition: `lbc:drone`, carrot_on_a_stick, model `lbc:drone_controller`,
`{raycastspell:1,drone_pult:1}`, referenced by loliluckyblock.
Required code/resources: raw input bridge; tracked display-missile pose/visual,
target UUID, source acquisition/turn/contact rules, successful vanilla-order
clear, native8 blast, TECH/TimeStop/epoch/unload/stop ownership and sounds.

Native behavioral comparison:
[LbcDroneDrops.java](../src/main/java/me/skywars2/game/LbcDroneDrops.java) spawn is
a bat/cart/hoglin producer; spawnBomber is a moving hoglin that emits TNT+pig.
[LbcHohlopitek.java](../src/main/java/me/skywars2/game/LbcHohlopitek.java):70-93
uses a technology ray, shared200 cooldown and that bomber, with different spawn
offsets/attack payload. [LbcHoming.java](../src/main/java/me/skywars2/game/LbcHoming.java)
steers native LargeFireball. These working systems do not supply the controller
missile's display, target-following turn expression, collision or power8 payload.
[LbcExasperato.java](../src/main/java/me/skywars2/game/LbcExasperato.java):277 onward
already offers useTechnologyRay; its queue/chunk/MAG-vs-TECH infrastructure and
native blast/attribution primitives are reusable, not an equivalent finished item.

## lightning_bolt - native terminal is supported, short ray bridge is absent

Immediate chain: raycast_lightning_bolt -> raycast_lightning_bolt_end; no other
function helpers. Increment, one forced END_ROD, .4 steps while Distance<=40:
41 samples/16 blocks. A non-airs block calls endpoint **back .4**. Mobs in a
positive unit box/distance<=6, !raycaster/!spectator, call endpoint at the current
sample. Endpoint summons native lightning_bolt and sets Distance1000. An extra
Distance41.. summon sits between the block and entity tests. No mana/debit or
ordinary cooldown is coded in the selected chain.

**Preserve source statement order in closure accounting:**
block endpoint sets1000 BEFORE the41.. test, so wall contact produces one bolt
back.4 PLUS one at the original sample. If a qualifying mob is also there,
the subsequent entity endpoint can produce a third. An entity before the last
sample produces one; sample41 with an entity can produce the range bolt plus
the entity bolt. A one-terminal/one-bolt adapter is not literal source closure.

The raw consumer appears in `imba/lucky_items_imba` as carrot_on_a_stick with
`{craftusable:1,bolt_material:1,lightning_bolt:1,raycastspell:1,imba:1}`.
Separate `lightning_bolt_egg` is panda_spawn_egg/ENTITY_DATA with only
`{craftusable:1,bolt_material:1}`. Its existing native alias is a different use.

Existing engine evidence:

- [LbcWands.java](../src/main/java/me/skywars2/game/LbcWands.java):397-405 registers
  bolt_material as a real LightningEgg and recognizes its raw flag.
- [ElectroNativeMixins.java](../src/main/java/me/skywars2/mixin/ElectroNativeMixins.java):29-39
  wraps actual vanilla SpawnEggItem.spawnMob, handing its real LightningBolt to
  LbcElectro.manageLightning for causal ownership.
- [LbcElectro.java](../src/main/java/me/skywars2/game/LbcElectro.java):542-563
  manages existing egg lightning or creates native LightningBolt, sets cause,
  SHOCK_BOLT, tracked lifecycle/epoch/ENDING cleanup. Native bolt tick/fire uses
  scoped worldChanges in ElectroNativeMixins.Bolt. LbcDamage.isNativeLightning
  retains standing native friendly-fire exception and electrical protections.
- [LbcBolts.java](../src/main/java/me/skywars2/game/LbcBolts.java):373-398 donor
  uses range100, paid mana and area3/5 or chain/reforge. useInfbolt at400 onward
  is free range7, item/wall terminated; living entities do not terminate it.
  Plural lightning_bolts is not the singular consumer.

Required implementation is a both-hand raw alias plus finite source sample/
MAG/terminal-order adapter invoking the **existing** electro.lightning at the
correct positions/count. No new lightning engine/damage type is required.
Do not turn the carrot into a spawn egg or alias it to a priced plural staff.

## Resource closure and deferred author review

Needed source resources: shadowglove and drone loot leaves; inline shetka leaf
in loliluckyblock; inline lightning leaf in imba/lucky_items_imba; chance5 and
same_id (or explicitly documented native UUID replacement); airs/airs_only;
mobs/hitbox_entities/raycasters; original model/name/lore/sound references.
Source-file presence in src is not artifact packaging/public readiness: Gradle
uses selective includes. Check the actual artifact/native codecs/nested pools
when these consumers are implemented. No RP edits or invented models are needed.

Questions recorded here only, not sent to the sleeping author:

1. Theft: preserve unrestricted endpoint re-selection, possible self/ally
   transfer and left/main continuous marker-gate asymmetry, or explicitly bind
   the ray terminal victim and apply teammate protection? The5% acquired-stack
   replacement itself is explicit source behavior.
2. Drone: preserve fixed-world-point launch and no explicit orphan TTL, or use a
   separately approved new policy? UUID instead of conditional player ID resets
   is an intentional native difference to document.
3. Lightning: keep wall2/possible3 and range+entity2 multiplicity, or approve
   normalization as a source bug? Existing egg/staff qualification does not
   decide this. Until resolved, do not claim single-bolt source closure.
4. Brush: source has no bedrock/container blacklist. Extra restrictions need an
   explicit decision; ordinary setblock/block-entity semantics require scoped
   destructive fixtures with exact neighbor restoration.

Required later qualification: real input/both hands; exact payloads and sample
boundaries; correct air tags/MAG/TECH; complete stack transfer plus independent
5% outcome; miss without debit; controller's actual inventory clear order,
UUID/flight/contact/power8/lifecycle; lightning multiplicity and causal native
fire/ticks. This audit does not claim those tests have run.

## Complete selected literal function-call closure:65 files

Roots are the six selected loop/click/tick functions below. Comments are
excluded; conditional calls are retained. All referenced function files exist.
This is static conditional closure, NOT a claim that every cast executes65
functions. Most expansion is solely drone -> idgive -> conditional player
resets. Common input/scheduler wrappers are described above; unrelated branches
of those generic dispatchers are not expanded into this bounded audit.

- [lbc:armor/flesh_set_bonus_add](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/armor/flesh_set_bonus_add.mcfunction): no function calls.
- [lbc:armor/flesh_set_bonus_remove](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/armor/flesh_set_bonus_remove.mcfunction): no function calls.
- [lbc:armor/phantom_set_bonus](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/armor/phantom_set_bonus.mcfunction): no function calls.
- [lbc:armor/phantom_set_bonus_clear](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/armor/phantom_set_bonus_clear.mcfunction): no function calls.
- [lbc:armor/phantom_set_bonus_pre](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/armor/phantom_set_bonus_pre.mcfunction): 3 -> `lbc:armor/phantom_set_bonus`; 4 -> `lbc:armor/phantom_set_bonus_clear`.
- [lbc:get_team](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/get_team.mcfunction): no function calls.
- [lbc:items/changed_inventory/fix_attribute_chest](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/changed_inventory/fix_attribute_chest.mcfunction): no function calls.
- [lbc:items/changed_inventory/fix_attribute_feet](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/changed_inventory/fix_attribute_feet.mcfunction): no function calls.
- [lbc:items/changed_inventory/fix_attribute_head](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/changed_inventory/fix_attribute_head.mcfunction): no function calls.
- [lbc:items/changed_inventory/fix_attribute_legs](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/changed_inventory/fix_attribute_legs.mcfunction): no function calls.
- [lbc:items/changed_inventory/mellstroy_equip](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/changed_inventory/mellstroy_equip.mcfunction): no function calls.
- [lbc:items/changed_inventory/mellstroy_equip_check](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/changed_inventory/mellstroy_equip_check.mcfunction): 1 -> `lbc:items/changed_inventory/mellstroy_equip`.
- [lbc:items/changed_inventory/mellstroy_unequip](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/changed_inventory/mellstroy_unequip.mcfunction): no function calls.
- [lbc:items/changed_inventory/mellstroy_unequip_check](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/changed_inventory/mellstroy_unequip_check.mcfunction): 1 -> `lbc:items/changed_inventory/mellstroy_unequip`.
- [lbc:items/changed_inventory/sel](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/changed_inventory/sel.mcfunction): 94 -> `lbc:armor/phantom_set_bonus_pre`; 95 -> `lbc:armor/phantom_set_bonus_clear`; 140 -> `lbc:items/changed_inventory/mellstroy_equip_check`; 141 -> `lbc:items/changed_inventory/mellstroy_unequip_check`; 143 -> `lbc:items/changed_inventory/fix_attribute_feet`; 144 -> `lbc:items/changed_inventory/fix_attribute_legs`; 145 -> `lbc:items/changed_inventory/fix_attribute_chest`; 146 -> `lbc:items/changed_inventory/fix_attribute_head`; 149 -> `lbc:armor/flesh_set_bonus_add`; 150 -> `lbc:armor/flesh_set_bonus_remove`; 151 -> `lbc:other/speed_up`; 152 -> `lbc:swrg_kit_integration/game/skills/inventory_changed`; 153 -> `lbc:swrg_kit_integration/game/challenges/no_knowledge/inventory_change`.
- [lbc:items/misc/drone/click](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/misc/drone/click.mcfunction): 2 -> `lbc:items/misc/drone/spawn_drone`; 3 -> `lbc:items/misc/drone/click`.
- [lbc:items/misc/drone/explode](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/misc/drone/explode.mcfunction): no function calls.
- [lbc:items/misc/drone/spawn_drone](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/misc/drone/spawn_drone.mcfunction): 6 -> `lbc:other/idgive`.
- [lbc:items/misc/drone/tick](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/misc/drone/tick.mcfunction): 3 -> `lbc:items/misc/drone/explode`; 4 -> `lbc:items/misc/drone/tick_fly`.
- [lbc:items/misc/drone/tick_fly](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/items/misc/drone/tick_fly.mcfunction): 8 -> `lbc:items/misc/drone/explode`.
- [lbc:leave_death](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death.mcfunction): 344 -> `lbc:other/wisp_book/delete_wisp/air/all`; 345 -> `lbc:other/wisp_book/delete_wisp/fire/all`; 346 -> `lbc:other/wisp_book/delete_wisp/water/all`; 347 -> `lbc:other/wisp_book/delete_wisp/nature/all`; 348 -> `lbc:other/wisp_book/delete_wisp/shadow/all`; 349 -> `lbc:other/wisp_book/delete_wisp/light/all`; 355 -> `lbc:leave_death_kest`; 356 -> `lbc:leave_death_kest`; 434 -> `lbc:swrg_kit_integration/game/skills/killed_by_player`; 444 -> `lbc:swrg_kit_integration/game/skills/absolute_cinema/nahida_revive`.
- [lbc:leave_death_kest](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/leave_death_kest.mcfunction): no function calls.
- [lbc:other/idgive](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/idgive.mcfunction): 5 -> `lbc:resets`.
- [lbc:other/speed_up](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/speed_up.mcfunction): no function calls.
- [lbc:other/wisp_book/delete_wisp/air/all](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/delete_wisp/air/all.mcfunction): 8 -> `lbc:other/wisp_book/realign_wisps/start`.
- [lbc:other/wisp_book/delete_wisp/fire/all](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/delete_wisp/fire/all.mcfunction): 6 -> `lbc:other/wisp_book/realign_wisps/start`.
- [lbc:other/wisp_book/delete_wisp/light/all](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/delete_wisp/light/all.mcfunction): 6 -> `lbc:other/wisp_book/realign_wisps/start`.
- [lbc:other/wisp_book/delete_wisp/nature/all](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/delete_wisp/nature/all.mcfunction): 6 -> `lbc:other/wisp_book/realign_wisps/start`.
- [lbc:other/wisp_book/delete_wisp/shadow/all](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/delete_wisp/shadow/all.mcfunction): 9 -> `lbc:other/wisp_book/realign_wisps/start`.
- [lbc:other/wisp_book/delete_wisp/water/all](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/delete_wisp/water/all.mcfunction): 6 -> `lbc:other/wisp_book/realign_wisps/start`.
- [lbc:other/wisp_book/realign_wisps/assign_angle](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/realign_wisps/assign_angle.mcfunction): no function calls.
- [lbc:other/wisp_book/realign_wisps/count_wisps](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/realign_wisps/count_wisps.mcfunction): no function calls.
- [lbc:other/wisp_book/realign_wisps/start](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/wisp_book/realign_wisps/start.mcfunction): 4 -> `lbc:other/wisp_book/realign_wisps/count_wisps`; 11 -> `lbc:other/wisp_book/realign_wisps/assign_angle`.
- [lbc:resets](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/resets.mcfunction): 127 -> `lbc:items/changed_inventory/sel`; 128 -> `lbc:swrg_kit_integration/game/levelup_select/levelups_remove`; 222 -> `lbc:leave_death`.
- [lbc:swrg_kit_integration/game/challenges/no_knowledge/inventory_change](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/challenges/no_knowledge/inventory_change.mcfunction): no function calls.
- [lbc:swrg_kit_integration/game/levelup_select/levelups_remove](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/levelup_select/levelups_remove.mcfunction): 138 -> `lbc:swrg_kit_integration/game/levelup_select/levelups_remove_1`; 172 -> `lbc:get_team`.
- [lbc:swrg_kit_integration/game/levelup_select/levelups_remove_1](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/levelup_select/levelups_remove_1.mcfunction): no function calls.
- [lbc:swrg_kit_integration/game/skills/absolute_cinema/nahida_revive](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/absolute_cinema/nahida_revive.mcfunction): no function calls.
- [lbc:swrg_kit_integration/game/skills/armored_change_inv](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/armored_change_inv.mcfunction): no function calls.
- [lbc:swrg_kit_integration/game/skills/armory_upgrade_change_inv_chest](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/armory_upgrade_change_inv_chest.mcfunction): no function calls.
- [lbc:swrg_kit_integration/game/skills/armory_upgrade_change_inv_feet](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/armory_upgrade_change_inv_feet.mcfunction): no function calls.
- [lbc:swrg_kit_integration/game/skills/armory_upgrade_change_inv_head](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/armory_upgrade_change_inv_head.mcfunction): no function calls.
- [lbc:swrg_kit_integration/game/skills/armory_upgrade_change_inv_legs](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/armory_upgrade_change_inv_legs.mcfunction): no function calls.
- [lbc:swrg_kit_integration/game/skills/enchant_armor](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/enchant_armor.mcfunction): 1 -> `lbc:swrg_kit_integration/game/skills/armory_upgrade_change_inv_feet`; 2 -> `lbc:swrg_kit_integration/game/skills/armory_upgrade_change_inv_legs`; 3 -> `lbc:swrg_kit_integration/game/skills/armory_upgrade_change_inv_chest`; 4 -> `lbc:swrg_kit_integration/game/skills/armory_upgrade_change_inv_head`.
- [lbc:swrg_kit_integration/game/skills/fix_attributes_offhand](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/fix_attributes_offhand.mcfunction): no function calls.
- [lbc:swrg_kit_integration/game/skills/inventory_changed](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/inventory_changed.mcfunction): 1 -> `lbc:swrg_kit_integration/game/skills/tanner_resist_change_inv`; 2 -> `lbc:swrg_kit_integration/game/skills/armored_change_inv`; 3 -> `lbc:swrg_kit_integration/game/skills/enchant_armor`; 4 -> `lbc:swrg_kit_integration/game/skills/offhand_master`.
- [lbc:swrg_kit_integration/game/skills/killed_by_player](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/killed_by_player.mcfunction): 2 -> `lbc:swrg_kit_integration/game/skills/vip`.
- [lbc:swrg_kit_integration/game/skills/offhand_master](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/offhand_master.mcfunction): 2 -> `lbc:swrg_kit_integration/game/skills/fix_attributes_offhand`; 3 -> `lbc:swrg_kit_integration/game/skills/offhand_master1`.
- [lbc:swrg_kit_integration/game/skills/offhand_master1](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/offhand_master1.mcfunction): 5 -> `lbc:swrg_kit_integration/game/skills/offhand_master1_1`; 9 -> `lbc:swrg_kit_integration/game/skills/offhand_master1_1`; 13 -> `lbc:swrg_kit_integration/game/skills/offhand_master1_1`; 17 -> `lbc:swrg_kit_integration/game/skills/offhand_master1_1`; 21 -> `lbc:swrg_kit_integration/game/skills/offhand_master1_1`; 25 -> `lbc:swrg_kit_integration/game/skills/offhand_master1_1`; 29 -> `lbc:swrg_kit_integration/game/skills/offhand_master1_1`.
- [lbc:swrg_kit_integration/game/skills/offhand_master1_1](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/offhand_master1_1.mcfunction): 3 -> `lbc:swrg_kit_integration/game/skills/offhand_master1_11`.
- [lbc:swrg_kit_integration/game/skills/offhand_master1_11](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/offhand_master1_11.mcfunction): no function calls.
- [lbc:swrg_kit_integration/game/skills/tanner_resist_change_inv](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/tanner_resist_change_inv.mcfunction): 6 -> `lbc:swrg_kit_integration/game/skills/tanner_resist_change_inv_feet`; 7 -> `lbc:swrg_kit_integration/game/skills/tanner_resist_change_inv_legs`; 8 -> `lbc:swrg_kit_integration/game/skills/tanner_resist_change_inv_chest`; 9 -> `lbc:swrg_kit_integration/game/skills/tanner_resist_change_inv_head`.
- [lbc:swrg_kit_integration/game/skills/tanner_resist_change_inv_chest](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/tanner_resist_change_inv_chest.mcfunction): no function calls.
- [lbc:swrg_kit_integration/game/skills/tanner_resist_change_inv_feet](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/tanner_resist_change_inv_feet.mcfunction): no function calls.
- [lbc:swrg_kit_integration/game/skills/tanner_resist_change_inv_head](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/tanner_resist_change_inv_head.mcfunction): no function calls.
- [lbc:swrg_kit_integration/game/skills/tanner_resist_change_inv_legs](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/tanner_resist_change_inv_legs.mcfunction): no function calls.
- [lbc:swrg_kit_integration/game/skills/vip](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/vip.mcfunction): no function calls.
- [lbcr:raycast_end6](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_end6.mcfunction): no function calls.
- [lbcr:raycast_end6_left](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_end6_left.mcfunction): no function calls.
- [lbcr:raycast_lightning_bolt](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_lightning_bolt.mcfunction): 3 -> `lbcr:raycast_lightning_bolt_end`; 5 -> `lbcr:raycast_lightning_bolt_end`; 6 -> `lbcr:raycast_lightning_bolt`.
- [lbcr:raycast_lightning_bolt_end](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_lightning_bolt_end.mcfunction): no function calls.
- [lbcr:raycast_loop6](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_loop6.mcfunction): 3 -> `lbcr:raycast_end6`; 4 -> `lbcr:raycast_loop6`.
- [lbcr:raycast_loop6_left](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_loop6_left.mcfunction): 3 -> `lbcr:raycast_end6_left`; 4 -> `lbcr:raycast_loop6_left`.
- [lbcr:raycast_shetka](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_shetka.mcfunction): 3 -> `lbcr:raycast_shetka_end`; 4 -> `lbcr:raycast_shetka`.
- [lbcr:raycast_shetka_end](../../../cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbcr/function/raycast_shetka_end.mcfunction): no function calls.

Aggregate SHA256 (sorted id + NUL + raw bytes + NUL): `1f10a0595996dcee90d7f018638c27a68fdc54b4e9f73bec255c09570de4c617`.
