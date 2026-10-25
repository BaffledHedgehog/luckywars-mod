# Banana flight/presentation and Babylon solid impacts

25 October2026. This record supersedes the earlier interpretation in
[COMBAT_VISUAL_FIXES.md](COMBAT_VISUAL_FIXES.md) that retired a homing banana on
its first wall. The latest user clarification requires source wall bounce and
homing with gravity disabled. The Academy staff’s accepted melee40tick full stun
remains in place.

These are implementation and native fixture changes. The parent verification
record must supply build/runtime results; no build, server, installation, Git or
real-client pixel validation was performed in this subtask.

## Source audit

Datapack paths below are relative to the read-only root
`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`.
The Paper physics source is read-only
`E:/servers_mew/cringe1_21_4_skywars/plugins/luckywars/src/main/java/me/luckywars/PhysicalObjectPhysics.java`.
The author resource pack is the read-only location in
[RESOURCE_PACK.md](RESOURCE_PACK.md).

`function/workingitems/firtusnadristailo/cast_banan_shoot.mcfunction` creates a
fixed display with identity local rotations and unit scale, teleports its entity
rotation to the cast yaw/pitch, then writes independently randomized physical
motion. Its first displayed entity rotation therefore follows the caster, not
the randomized motion vector. On later physical frames, Paper
`PhysicalObjectPhysics.applyFacing` computes player-convention yaw
`atan2(-vx,vz)` and pitch `-atan2(vy,horizontal)`, retains the existing display
entity rotation, and replaces `left_rotation` with `rotateY(-yaw)*rotateX(pitch)`.
There is no authored independent roll/spin animation. The physical step invokes
this facing update only when squared speed exceeds1e-6.

The previous native port instead used only entity yaw/pitch from current motion,
leaving the display’s local rotation at identity. It also used randomized motion
for the ordinary banana’s first displayed frame. `LbcFartus` now preserves the
source first-frame caster rotation and subsequent combination of retained body
rotation plus motion quaternion. This intentionally reproduces the source
combination rather than adding an invented90/180degree correction.

`cast_banan_shoot_homing.mcfunction` creates identity-local/unit-scale displays
without `physical_object`; its randomized entity rotation is subsequently moved
and turned directly by `banan_impact_homing_det_rec.mcfunction`. That helper moves
.4 three times per frame and calls `banan_impact_rerot` in a non-air block. The
latter reverses entity facing. Native homing again retains this wall reversal,
while keeping NoGravity on the projectile and visual display and never calling
the ordinary physical solver. The swept collision stays a negligible epsilon
outside the wall so the next outward substep cannot collide again with the same
boundary. Ordinary banana gravity.08, bounce.9 and friction are unchanged.

`cast_etiketka_shoot.mcfunction` uses the second ordinary firing mode’s label:
fixed `lbc:banan`, scale[.5,.5,.5], left quaternion[1,0,0,1], right identity and
zero translation. Native26.3’s quaternion codec normalizes that source tuple to
a90degree X rotation; source scale.5 remains exact. The existing native values
were the intended values, but runtime metadata revealed that the native SNBT omitted the required translation field. Minecraft26.3 therefore discarded the entire transformation and rendered identity scale/rotation. Adding translation[0,0,0] activates the original scale0.5 and normalized quaternion; the strict metadata check remains. The author `assets/lbc/models/item/banana.json`
also has its own fixed-model rotation[0,-76.5,0]; `banan.json` uses the generated
flat label texture. Both original models and all source textures remain intact.
The26.3 fixed Display renderer applies entity rotation and local transformation,
then its standard item-display Y180 rotation; no extra server compensation is
introduced.

## Babylon block impacts

`function/projectiles/pre.mcfunction` dispatches `projectiles/prein` when
`inGround:1b`. `prein` dispatches `projectiles/arrows`, which reads Raid Omen’s
amplifier and calls `projectiles/arrows_f`. That function executes101/109/110
once at the embedded arrow position, then kills the arrow regardless of whether
its action was suppressed:

- 101: native creeper power1 explosion; nearby inventory mystical type2 ward12
  or mechanism `stopper`20 suppresses it.
- 109: native lightning; the same ward12 or `stopper_magic`20 suppresses it.
- 110: fire replacing air in the relative±3 cube, followed by the exact fractional
  ±1.5 destructive carve with drops; the same ward/magic-stopper gates apply.

The port previously handled only the native living-target potion application.
The scoped native `BabylonArrow.onHitBlock` now calls the shared consequence
implementation after vanilla establishes its embedded position and then retires
the arrow. A per-arrow consumed flag and retirement prevent repeated execution.
The block path creates one action, without fabricating living-target duration
queues or changing their FIFO/persistence/duration behavior. The three kinds are
stored on the live native Babylon arrow; this is separate from generic Raid124
chain-lightning work.

Both paths reuse the exact same explosion/lightning/firebreak implementation,
source ward distinctions, resident edits, captured indirect-cause order and WILD
secondary policy. Current `getOwner()` at impact supplies attribution after
reflection. Existing shared Greed, technical-role and newer-real-hit guards
continue to apply to secondary native damage and terrain consequences. Native
arrow flight explicitly pauses before collision during Time Stop; warded impacts
consume the transport. Existing epoch/unload/forget/stop retirement and secondary
cleanup remain shared. No shared callback or mixin integration is required.

## Added native assertions

`FartusChecks` includes native projectile and visual entity ticks across eight
flight frames, asserting no downward drift both without a target and while
homing toward level enemy eyes. A resident stone collision verifies a retained
forward-to-backward velocity reversal, continued actor/display lifetime and no
vertical acceleration. Existing ordinary physical gravity/bounce checks remain.

The display fixture checks ordinary first and subsequent frames, homing frames
and the label scale/orientation using native spawn-packet and metadata codecs.
Decoded metadata is applied to another native ItemDisplay; its transformation,
fixed item context, source item model, scale, local rotations and NoGravity flag
are then inspected. These are native/wire assertions, not real client pixels.

`BabylonChecks` exercises actual native arrow-to-solid collisions for all three
transports, including reflected explosion/lightning ownership, protected roles,
embedded positions, second-tick nonduplication, destructive firebreak bounds,
ward consumption, mechanism-versus-magic stopper distinction, flight pause and
single resumed consequence after Time Stop, and no fabricated target queues.
Existing native entity-hit queues, Greed, lifecycle and ordinary flight checks
remain enabled.

Native block-blast testing also exposed a shared-role gap: owned Babylon creeper explosions were WILD but did not reject technical living actors. The shared wand damage guard now explicitly applies validTarget to OwnerDamageSource; active Greed and all existing upstream defenses remain guarded.

Final root qualification: build `b4cdfa8d`,148 recorded results across two actual
mode1 matches and actual stop cleanup. See [combined review](GAMEPLAY_REVIEW_2026_10_25.md)
for the tested artifact, deployment state and verification boundaries.
