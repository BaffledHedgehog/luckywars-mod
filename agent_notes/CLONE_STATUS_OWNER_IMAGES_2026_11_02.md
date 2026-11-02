# Explicit clone target-status owner images ? 2 November 2026

Implementation only. No compilation, native qualification, installation, resource
change or gameplay exposure was performed by this owner task.

`CloneStatusStates.capture(game, source)` probes every admitted native body and
returns null when there is no scoped overlay. The returned image is an overlay:
the root chooses the native/custom body factory and applies native NBT, metadata,
attributes and runtime before installing these images. Every installation creates
independent mutable target records; rollback removes only clone records. Original
native effects, leases, score state, causes and pending queues are untouched.

Covered owners:

- OwnedEffectLayers: non-destructive per-target checkpoint and independent restore
  onto the clone, without withdrawing original leases or rebuilding native effects.
- LbcIndirectKills: fall/grace/levitation, effect causes and pending mana effect
  aliases, mana drain/debt, removed support, later attack, burning and arrival
  cells. Active native-effect links point to the fresh clone effect instance;
  detached effects are deep copied once per alias on each installation. Shared
  terrain provenance and unrelated already-emitted consequences are not cloned.
- ProjectileLifetimes: fresh Review for a tracked projectile, preserving terrain
  policy, owner, age, next review, target revision/cursor and terrain ray cursor.
  A queued review gets one new queue entry; rollback removes that Review only.
  Existing computeIfAbsent admission preserves prepared copies.
- LbcElectro: independent shock owner/pulse/delay, reflection owner, acceleration
  once-state and live native bolt membership. Player charge counters stay external.
- LbcShooters: independent stun controller/epoch/remaining ticks/locked angles,
  original native effect ownership links, copied previous hidden effect baselines,
  installed ages and restored-age records. Native copied attributes carry movement.
- LbcHoming: independent curse owner/remaining/raw-WILD policy and native fireball
  age/steering membership. ENTITY_LOAD uses putIfAbsent for copied flight age.
- LbcFrost: cold/cooldown/frozen time/cause and independent attachment/saved shell;
  the new UUID receives a lease on the same source ice cells, with no terrain writes.
  Rollback only removes its UUID leases. Prepared records survive ENTITY_LOAD.
- LbcBabylonEffects: independent FIFO contributions with original remaining time
  and cause, copied secondary TNT source, attachment and counter projection.
  Prepared pending queues survive ENTITY_LOAD.
- SignedEnvironmentalEffects: actual active effect author links retarget clone
  instances and offspring causal ownership persists. Spawn/terrain queues already
  emitted by the source remain external.

LbcLegacyEffects and LbcFoodStatuses have player-only scoreboard consumers and no
nonplayer private overlay. No empty clone protocol was added. Player personal
stasis/cooldowns/control/rebirth are outside the admitted actor task.

Additional target controllers observed outside this scope include LbcPendingCombat,
LbcMana, LbcQuiver, LbcKiuas, LbcMagic/LbcColors. Root owns their closure, polymorph,
Psycho/Acid status integration, native values and overall coverage qualification.
This component does not assert complete status coverage or expose skills15/17.
