# Reactive inventory artifacts: answered full current / old family

27 October2026. Whole mystical2 and mystical_old2 family implemented from
REACTIVE_INVENTORY_SOURCE_2026_10_27.md, original ondamage/current helpers and
saved author answers10/11/12/14. Sand Amulet belongs to another integration.
No build, test server, git or protected/shared source operations were run here.
Native check bodies are authored but unexecuted by this agent.

## Frozen API and integration

`LbcReactiveArtifacts(GameController)` exposes `damaged(LivingEntity,DamageSource)`,
`beginAttack(ServerPlayer)`, `endAttack(ServerPlayer)`, `tick()`, `loaded(Entity)`,
`unloaded(Entity)`, `clearEffects(ServerPlayer)`, `forget(ServerPlayer)`, `stop()`.
`externalBlindness(LivingEntity,MobEffectInstance)` observes ordinary native
blindness grants; `canTarget(Mob,LivingEntity)` guards this smoke's mob targets.
Root registers both `ReactiveSmokeMixin` and nested `ReactiveSmokeMixin$Target`.
Root LbcItems accessor, accepted-event, lifecycle/purification and scheduler
integration are required. Root owns availability/acquisition/build/qualification.

The owned PendingSwordAttackMixin now brackets pending swords and reactive
artifacts. Its finally block flushes pending sword movement first, then reactive
source events. All accepted native sweep targets are collected before moving the
attacker. Each reactive event replays the complete current1..6 then old1..6 order;
this preserves old4 movement before later old5/6 area queries when different
artifacts coexist. Flags and current magic admission are captured per accepted
event. Events outside native Player.attack execute directly.

## Whole source behavior

Inventory0..40, including armor/offhand, supplies independent type bitsets.
Identical copies do not multiply one predicate; distinct types/families do.
Native extra body/saddle41/42 and menu craft/cursor are excluded. Admission uses
actual accepted positive/unblocked non-arrow victim, default allied-player and
technical-role protection, native Time Stop, epoch/ENDING and source dimensions.
Current family retains nomagic_active player20 and stopper20 gates; old bypasses
these branch-local gates.

Current1 gives owner Regen40/2 hidden and Saturation2 native ticks/4 hidden,
actual victim Wither40/4 and Hunger40/99 hidden, food_count+1, source heart5 and
cat hiss1/2. Current2 gives actual victim Poison400/4 visible. Current4 moves
actual victim and owner flat-forward4 through walls, with source four NORMAL
firework packets of5 at forward0/1/2/3 and y1. Existing yaw/pitch are retained;
flattening affects the translation direction only, as native positional /tp does.
Current5 rotates actual victim yaw180 without selection range. Current6 performs
one signed scoreboard mana+1000, source witch20 and ambient1/1; it does not add a
cooldown mechanic or clamp to ordinary pool maximum.

Current3 preserves initial other-viewer signal smoke500 NORMAL (distance>=.01)
and owner SMOKE10 NORMAL. No repeated500-per-tick emitter exists. Each actual
activation creates independent owner/epoch100tick radius7 resident smoke state.
WILD membership includes owner, allies, enemy players and eligible mobs while
shared role/Greed guards remain. Players own a short hidden native Blindness0
layer only while inside. An original/foreign same-amplifier layer is separately
retained and aged; the addEffect observer captures new ordinary same-amplifier
grants. On exit/expiry only the smoke layer is removed and the surviving normal
layers/visibility restored. Higher native layers remain intact. Overlapping
clouds share one membership layer rather than removing each other's effect.

Mobs receive a unique transient ADD_VALUE follow-range modifier, recalculated
from all other current modifiers to yield3. Base value and foreign modifiers are
never rewritten. Exit/expiry/unload/stop removes only that owned modifier. A
preexisting target farther3 is cleared and navigation stopped. The scoped
Mob.setTarget mixin rejects new targets farther3 while this smoke owns the mob;
near targets remain allowed. A mathematically singular foreign multiplier0
cannot be inverted by an additional ordinary additive modifier; no foreign
modifier is removed to force a value. Ordinary positive follow-range math is
covered by native checks.

Old1 gives Regen20/1 and native Saturation1tick/1 hidden. Old2 gives Strength40/0
hidden. Old3 remains visual-only signal smoke200 FORCE, minimum viewer distance2;
no current cloud/Blindness/range mutation is aliased onto old3. Old4 moves owner
from eyes forward3 using current pitch, then nearby8 nonplayer/non-armorstand/
non-area-effect-cloud entities feet-forward3 in their own pose. It is WILD and
keeps source order and wall passage, with resident destination guard. Old5 uses
owned shared WILD Wither80/source10 hidden, approved ItemEffectLevels cap5,
including owner/allied players/mobs. Old6 is area Regen80/2 hidden, not mana;
default allied-player protection applies to that independently unapproved WILD
branch. Shared indirect-kill effect and displacement APIs retain original owner.

## Loot and authored checks

Twelve exact singleton leaves: ported/reactive_artifact_1..6 and
ported/legacy_reactive_artifact_1..6. Each wraps its unique matching original
artifact/artifact_old entry and preserves entry weights/qualities/components.
The existing import_game_resources.py migrate_loot_schema helper alone adapts
legacy loot discriminator/function arrays to native26.3 codec shape. No broad
whole-pool regeneration or source presentation rewrite occurred.

ReactiveArtifactChecks.run/tick exercises all12 actual accepted native inputs,
exact source carriers/flags/model/tier/lore counts, duplicate predicates, source
instant1/2tick durations, far14block actual victims, food_count and signed mana,
Inventory0..40 versus41/42/craft/cursor, old WILD Wither/movement and old200FORCE
recipient lower bound. Actual Player.attack primary+sweep checks verify that
owner remains at original pose during accepted callbacks, then both victims
move and owner retains one source4step per event. Smoke checks record exact
initial500/10 counts/modes, reject repeated500 emission, include allied player
and allied mob, and use genuine registered native connection scheduling with
loaded-player/latest-teleport ACK after every teleport. They observe leave,
reentry and100tick expiry, preserved foreign follow modifier/base, original and
new foreign same-amplifier blindness, far-target clearing/rejection and near3
admission. No player/effect/world is manually ticked.

Debug leaves and these fixture inputs do not establish normal acquisition.
The original artifact table occurs in the accessory transformation pool, but
whole normal reforge proof and pool availability remain root-owned. Root must
report actual compilation/qualification results, any fixture failures and fixes.
