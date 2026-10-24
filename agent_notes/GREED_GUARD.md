# Absolute active Greed guard (24 October 2026)

Author ruling (user):

> жадность - ты имеешь ввиду полномочие греха жадности? Его фишка в том что пока оно активно (че то там sin greed active в легаси было) на игрока не могут действовать никакие внешние эффекты - смысл в этом был. Снаряды кстати от такого игрока тоже должны отскакивать, неуязвимость грешника жадности ничем не должна пробиваться и должно всё отбивать без повреждений пока активно.

`sin_greed_active` is absolute immunity to all external effects and damage
while active; projectiles bounce harmlessly and nothing bypasses it (user).
This overrides earlier Babylon-only interpretations of Greed. It applies to
beneficial external effects as well as harmful effects. Existing dormant
Babylon contributions can wait until the tag ends; new contributions cannot
enter while active.

This narrow batch recognizes the actual active tag. It does not open Greed
acquisition, create a new authority or port its full activation/upkeep loop.
Read-only source intent:
`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/witch_gens/greed_tick_active.mcfunction`.
The old function grants Resistance V and Instant Health V, extensive knockback
resistance, clears many effect types, destroys a small local obstruction and
rescues below-void players. The explicit user guard supersedes the incomplete
Resistance/status list; those unrelated activation mechanics are not copied.

`LbcGreed.active(Entity)` checks the tag. `reflect(Projectile,Entity)` reverses
the actual native velocity without halving its speed, assigns the Greed target
as the native owner, runs the existing Electro ownership propagation for player
owners, synchronizes current/previous rotation, sets `needsSync`, and marks
`skywars2.greed.reflected`. Returning true means the caller must skip impact.

`GreedGuardMixin`, `GreedGuardMixin$Players`, and `GreedGuardMixin$Projectiles`
must be added to the mixin configuration. Early living/player `hurtServer`
guards reject every damage type, including void and generic kill; no damage
tag or armor-bypass exception is permitted. Living knockback is rejected.
The projectile guard intercepts `hitTargetOrDeflectSelf` before native `onHit`,
preventing potion, firework, arrow, trident and other normal impact effects.
It reports a deflection to the native collision loop, preserving the projectile.

Shared integration must also reject Greed before Time Stop damage is queued,
block external native effect ingress/instant effects/ticking effects, exclude
Greed from virtual traces and area-target queries, and allow physical native
projectile collision with Greed so it can bounce. A normal target filter that
excludes Greed before collision would merely let the projectile pass through.

Source acid projectiles override entity collision to false and some restore
their pre-tick velocity. They need a narrow active-Greed collision exception
and must preserve newly reflected velocity. Shared actor traces that do not
call native `hitTargetOrDeflectSelf` likewise require their own shared guard.
These integration requirements are not claimed complete by the helper alone.

Shared integration now rejects native/direct-HP/void damage before Time Stop
queuing, external effect ingress (including beneficial effects) and instant
effects. Harmful native effects and levitation/slow falling are cleared before
the native effect tick. Common spell target queries exclude active Greed;
physical collision still admits it for reflection. Explosions exclude Greed
before both damage and knockback. Collision impulses are suppressed while
ordinary movement and healing remain available.

Acid admits only Greed as an entity collision and preserves reflected motion.
Manual spell actors use a current-trajectory guard; virtual electric traces
reverse on Greed contact. These checks do not run a world scan when no player
in that world has active Greed. Acquisition and activation remain future work.

Runtime qualification is recorded with the Babylon batch below when complete.

## Qualified batch evidence

Build `0fb48f1a` passed62 recorded suite results across two actual mode1 matches (144.06s). Exact 306 sources / 837 classes / 2793 resources match the tested artifact. Installed stopped main with rollback `daa2f866`; main was not started. Shared lifecycle, source transport and active-Greed guards are integrated. Three protected user sources are unchanged/excluded. Evidence: `build/verification/babylon-final/`. Boundary fixtures include manual ticks; no real client pixels, actual disk restart/disconnect or peak-capacity performance claim.
