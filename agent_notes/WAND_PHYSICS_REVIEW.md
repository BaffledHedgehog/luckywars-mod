# Wand projectile physics review — 20 October 2026

User policy: magical wand projectiles, except acid, must ignore incidental native
projectile collisions, attacks, and explosions. Explicit Electro Shield and
Electroblocker reflection and ownership changes, Curse, homing, and Kiuas
conversion remain intentional controls.

`ProjectileLifetimes.protectedWandProjectile(Entity)` is the common classification;
the existing `EntityVelocityMixin` and `WandProjectileMixin` apply the policy.
There are no per-family physics overrides or additional entity scans.

| Family | Classification and existing source |
| --- | --- |
| Air, Blind, Exasperato, Exoperso, Psycho, Stone, Ruby, Teleport | Their native `Charge extends Projectile` classes are protected. All modes share that class; Blind rain is included. |
| Terra | `LbcTerra.TerraProjectile` is protected, including the shared magical sword/beam/razor variants. |
| Fire and Homing; Blaze spells, summoned Blaze and turret fire | `skywars2.wand.fire`, assigned by `LbcWands.launchFire`; both native small and large fireballs are protected. Summons Chaos fire uses the same launcher. |
| Tiny Air terminal rocket | `skywars2.wand.air`, assigned in `LbcWands.rayEnd`; the native firework is protected. |
| Teleport cloud / Wind Racer | `LbcTeleport.CLOUD`; native arrows are protected, including the racer subtype. |
| Acid | `LbcClouds.AcidProjectile` is explicitly excluded before tag checks; native acid physics remain. |
| Pine gun | `LbcShooters.PROJECTILE_TAG` arrows are physical gun ammunition, outside this spell policy. |
| Summons arrows / Chaos trident | `LbcSummons.PROJECTILE` marks actual summoned weapon ammunition; these remain physical. Their Chaos fireball uses the protected Fire launcher above. |
| Trinket fireballs and rockets | `LbcTrinkets.shot` does not apply wand tags; these remain native. |
| TNT payloads, chain TNT and ordinary TNT | `PrimedTnt` is not a projectile; delayed placed payloads remain native. The flying Exasperato and Exoperso spell charges are protected above. |
| Instant traces, lightweight jobs and areas | No native projectile to receive impulses; existing deliberate owner/origin control remains. |

Native method verification used the local Loom 26.3 sources JAR, especially
`Entity`, `Projectile`, `AbstractHurtingProjectile`, `AbstractArrow`, `Player`,
and `ServerExplosion`.

The `ignoreExplosion` early return skips the native blast branch before exposure
rays, damage calculation, knockback, redirectable-projectile owner reassignment,
and `onExplosionHit`. A separate `pushFromExplosion` guard also rejects direct
native explosion impulses. These checks apply to every explosion source, without
changing explosion damage to ordinary targets or unrelated projectiles.

Protected projectiles are not pickable, attackable through the inherited entity
path, or eligible projectile targets. Native projectile target filtering also
rejects collisions whenever either projectile is protected. All protected native
classes already inherit `Entity.isPushable() == false` and
`Entity.canBeCollidedWith(...) == false`; ordinary mob collision does not push
them. Block contact, spell collision geometry and spell impact remain with their
existing implementation.

The common Vec3 `Projectile.deflect` overload rejects `byAttack=true`, used by
native player attacks and projectile redirection. The scalar overload forwards
that flag unchanged. `hitTargetOrDeflectSelf` treats native entity deflection as
`NONE` for protected spells. Explicit `LbcElectro.reflect` uses
`byAttack=false` and remains available; no general `setOwner`, `setPos`,
`setDeltaMovement`, or `push` interception was added. Curse, homing, deliberate
reflection, and conversions keep their existing control paths.

Parent integration: build and 10 isolated-server suites passed on SHA `b93d9fee…`.
Native TNT, real small/large fireballs, rocket/cloud forms, crossing arrows,
acid impulse and intentional reflection are covered. See SERVER_TESTS and
`build/verification/wand-physics-verification.json`. No real-client manual
test or new throughput benchmark was performed; the audit agent itself did not run a server.
