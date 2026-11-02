# Remaining target-status and native producer images ? 2 November 2026

Implementation only; no build, native process, installation or skill exposure was
performed by this owner task. Scoped whitespace checks passed.

`CloneRemainingStatusStates.capture(game, entity)` is called for every admitted
body. It composes target overlays and returns null when none exist. `recognizes`
and `recognizesGameplay` identify PendingCombat homing-sword displays, Quiver
Shot/Trail/child bodies and IncomingArtifacts bodies. All these use their existing
native classes, so the default native EntityType factory is the primary factory.
Actual Trail markers and homing-sword displays are gameplay producers. They are
not excluded as render children. `protectedActor` excludes only stopper/beacon
markers that own map mechanism authority.

PendingCombat copies gravity curse controller/cause/epoch/remaining seconds, and
copies each logical homing-sword Copy record with original direction and grounded
phase onto the fresh display. Native body positions, rotation and tags belong to
the shared native image. No forty-copy volley is recast and its author is retained.

Mana copies nonplayer pending potion/stat-effect membership and existing canonical
stat-potion timer scores. Only for an active potion target it also preserves the
existing mana/manamax/manaregen score baselines used by pending permanent changes.
Player gear, reverse-mana author maps, equipment resources, cooldowns and overload
abilities are not cloned. Source score inspection never creates new score entries.

Quiver independently copies Shot owner/cause/epoch/amplifier/age/ground once-state,
logical Trail amplifier/authority/age, child source authority, and each target FIFO
contribution with exact remaining ticks. Target score/attachment projection is
rebuilt from those copied queues. ENTITY_LOAD preserves prepared queues; native
Shot lookup already returns existing owner state. No world trail is duplicated
merely because its original caster or victim was cloned.

Kiuas copies target curse membership (including an unowned null cause), its exact
canonical effect_kiuaskivi counter, and the causal attachment. Equipment auras,
Sun queues and existing world terrain work stay external. Colors copies only its
three canonical RTP counters and target teleport/water index membership. Signed
native water effects are already carried by the native image.

IncomingArtifacts copies each native actor's original UUID author into the clone
index before adoption. Column terrain journals, artifact acceptance frames and
source world terrain queues are not cloned. ReactiveArtifacts copies smoke range
reduction membership and, if present on an admitted target, an independent hidden
blindness baseline/amplifier/last-tick record. Existing source smoke clouds are
external; native copied attributes preserve the current follow-range modifier.

Magic has no separate target overlay: arrows/unstable/falling sets are derived from
native type and rebuilt during normal adoption. Its other state belongs to player
equipment, liquid auras or map beacons. No empty owner protocol was added.

Images do not mutate originals, spend mana, reroll, recast, duplicate player
camera/control authority or restore a whole checkpoint. Rollback removes clone
private entries without triggering explosions, rewards or source cleanup. Native
values, common status/effect causes, Polymorph and complete integration remain
root-owned. This note does not assert full owner coverage or runtime qualification.
