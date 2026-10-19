# Artifact damage poll audit

Scope: `LbcMagic.java`, `LbcItems.java`, and generated/runtime resources under `src/main/resources`; read-only code audit. No code changes.

## Finding

No artifact-related polling of `HurtTime` (or equivalent damage-state NBT) exists in the current runtime sources or resources. A case-insensitive search for `HurtTime`, `hurt_time`, and `hurtTime` in both named Java files and all `src/main/resources` returned no hits. In particular, `LbcMagic.tick()` dispatches arrow, explosion, liquid, color, passive, and periodic work; it does not inspect damage state ([LbcMagic.java](../src/main/java/me/skywars2/game/LbcMagic.java#L467), [passives](../src/main/java/me/skywars2/game/LbcMagic.java#L735)). Its artifact passives are inventory-derived flags and effect/health maintenance, not damage reactions ([refresh](../src/main/java/me/skywars2/game/LbcMagic.java#L324)). `LbcItems.tick()` delegates to those systems and handles other item trackers ([LbcItems.java](../src/main/java/me/skywars2/game/LbcItems.java#L778)); its `damaged()` callback is for Ice/Terra swords, not the artifact ([LbcItems.java](../src/main/java/me/skywars2/game/LbcItems.java#L130)). No test/fixture match was found in the named runtime locations; `src/test` and `test` directories are absent.

## Existing damage event path

The remaining `hurtTime` reads in gameplay Java use the native integer field,
not entity NBT serialization. Ice Sword / Snow Pearl call
[`LbcFrost.hitTargets`](../src/main/java/me/skywars2/game/LbcFrost.java#L78)
from damage callbacks; its `hurtTime == 10` narrows a nearby-entity selection,
not a tick-based damage detector. Replacing that area with only the event
victim would change which nearby targets receive cold.

One genuine tick-based detector remains in
[`MatchEvents.tickSkill`, skill 3](../src/main/java/me/skywars2/game/MatchEvents.java#L420):
it grants speed when `player.hurtTime == 10`. This is a vanilla skill, not
the mystical artifact. No change was made in this audit. The skill-13 queue
is different: `onPlayerHurt` already schedules work from the damage event,
then checks the intentionally delayed `hurtTime == 5` window; it does not
scan all players to discover damage.

`SkyWars2` registers Fabric `ServerLivingEntityEvents.AFTER_DAMAGE`. For positive, unblocked damage it dispatches to indirect-kill, item, and trinket handlers; player skill/game damage handling is also here ([SkyWars2.java](../src/main/java/me/skywars2/SkyWars2.java#L101)). This event path is the appropriate place to trigger a genuine “when damaged” artifact effect: it reports a completed damage application with the victim and `DamageSource`, and avoids interpreting a lingering hurt animation/state as a new hit. The existing item dispatcher receives that same victim/source pair through [LbcItems.damaged](../src/main/java/me/skywars2/game/LbcItems.java#L131).

## Semantics to preserve if adding a reaction

- The registered handler excludes blocked damage and damage `<= 0`; a listener attached there will not observe attempted/fully resisted hits.
- The event can fire for damage sources beyond direct player attacks. Filter by the intended victim, source, and artifact ownership/active state explicitly; do not infer a player attacker from the victim's `HurtTime`.
- Direct health edits that bypass `hurtServer`/normal damage processing do not necessarily reach `AFTER_DAMAGE`. Decide whether those should count before reusing this hook.
- The artifact's existing inventory/passive scan cadence and behavior are separate from a per-damage callback; adding a HurtTime poll would conflate a timed hurt state with individual damage events and can miss/duplicate event semantics.
