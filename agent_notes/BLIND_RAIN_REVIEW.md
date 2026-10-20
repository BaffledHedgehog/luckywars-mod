# Blind rain: targeted implementation review

Initial read-only Java review, 20 October 2026. Integrated result and runtime evidence are recorded below.

Requested behavior (user): rain drops live 20 ticks instead of 40 and appear near mobs to avoid useless projectiles. This is a target requirement; at review time `RAIN_LIFETIME` remains 40.

## Existing behavior

[`LbcBlind`](../src/main/java/me/skywars2/game/LbcBlind.java) creates a 200 active-tick cloud ten blocks above the initial contact. Every second global tick it emits one native `Charge`, uniformly across a 21×21 integer square, downward at 1.8 blocks/tick. It also emits 30 cloud ink particles and a sound. Each drop performs swept wall/entity and shield collision, then sends a trail particle every .3 blocks. Damage and blindness/wither use the current projectile owner. Cloud and drop lifetimes pause during owner/global time-stop; epoch, arena, owner, loaded-chunk and antimagic checks terminate them.

## Keep native drops

Replacing rain drops with lightweight positions would remove existing integration unless explicitly rebuilt:

- [`LbcElectro`](../src/main/java/me/skywars2/game/LbcElectro.java), `tickReflectors` / `reflect` / `assignOwner`: native projectile queries reverse velocity and transfer owner; the blocker accelerates its own projectiles. Blind already reads current velocity/owner each tick, including damage attribution and target filtering.
- [`LbcHoming`](../src/main/java/me/skywars2/game/LbcHoming.java), `loaded` / `tickCurses`: registers all native projectiles, redirects them every 20 ticks and conditionally transfers authorship through Electro. Rain cannot safely assume drops remain vertical or owned by the caster.
- [`LbcKiuas`](../src/main/java/me/skywars2/game/LbcKiuas.java), sun aura: finds `Projectile` objects in the 8–25 block annulus and turns their locations into lava before removing them.
- [`LbcConsuming`](../src/main/java/me/skywars2/game/LbcConsuming.java), Silence/Avarice: ordinary native entities participate in deletion/pulling. `LbcItems.transformSpellOrigins` separately covers the lightweight rain cloud through `LbcBlind.transformOrigins`; it currently does not cover hypothetical lightweight drops.
- [`LbcDuplicating`](../src/main/java/me/skywars2/game/LbcDuplicating.java), black-hole query: can displace native drops through its ordinary entity query. Their Polymer client appearance as a marker does not make them server-side `Marker` objects.

Retaining `Charge` is the straightforward safe choice. Keep the original source in `DamageSource`, shared collision/shield helpers, current owner lookup, native tags and lifecycle checks.

## Straightforward optimization

Change only rain lifetime to 20. For each scheduled emission, perform one local `getEntitiesOfClass(LivingEntity.class, bounds, predicate)` query within the existing rain footprint and below the cloud, using `game.items().lifetimes().target(owner, entity)` rather than inventing another enemy filter. [`ProjectileLifetimes.target`](../src/main/java/me/skywars2/game/ProjectileLifetimes.java) includes the MOBS tag and shared `LbcWands.target` role/team protections used by the actual Blind impact. Choose an eligible target without sorting (random index from the returned list), then position the drop over/close to its hitbox while retaining downward motion and real collision. Limit the search vertically to the 20-tick reachable fall distance and reject targets above the spawn plane. Recompute from the cloud's current position because Consuming can move it.

If there is no eligible target, advance cloud age and preserve its visual/sound cadence but omit that useless native drop. This follows the requested targeting behavior; do not silently fall back to empty-space rain, impose arbitrary entity caps, discard paid clouds or truncate cast queues. A targeted drop can still hit cover or lose its target; do not replace flight with direct damage.

Additional cheap CPU work: calculate `delta=end.subtract(from)` once per charge step and reuse it for trail samples instead of subtracting on every particle. Use the existing two-tick emission cadence for target queries; do not scan every cloud on intervening ticks. A persistent target cache is unnecessary initially and risks stale ownership, role, movement and removal checks. At the same emission rate, changing TTL 40→20 halves the maximum concurrent uncollided rain drops (about 20→10 per cloud); target-only spawning reduces it further when the footprint is empty. Particle/network cost remains separate and should be measured before changing the established visual cadence.

## Integrated result

The implementation retains native Charge and uses TTL20. Rain searches living enemies including players in X/Z ±10 below the cloud, within 36 blocks of vertical reach. Spawn stays at cloud Y, with X/Z jitter ±0.6 around the selected target's current position, clamped to the footprint. No eligible target means no new entity; clouds still age and preserve their prior particle/sound cadence. Paid clouds are neither capped nor merged.

A local candidate list is refreshed no more often than five service ticks (six on the two-tick emission cadence). Eligibility is revalidated before each emission and positions are read live; moving the cloud clears its cache immediately. This addresses the stale-target risks noted above. Trail sample coordinates use scalar arithmetic and one shared displacement, avoiding temporary Vec3 objects per sample; the down-velocity vector is immutable/shared. General hit/shield/owner helpers remain unchanged.

Build `d650593dc09340b75d5a46cf59b8402b6013daa32fc1a6e3fa7143af8fa31b51` passed the native rain fixture, existing Blind regression and stop cleanup. Thirty real clouds reached 300 live native drops while maintaining their full moving trail/collision work for 80 service calls: mean 2.093 ms, p95 4.745 ms, max 5.155 ms. Drops were repositioned into empty air between calls to force full TTL; these are service timings, not full server MSPT, real-client networking or a before/after comparison. Detailed scope: SERVER_TESTS.md and `build/verification/blind-rain-verification.json`.

Verified archive `build/verification/skywars2-blind-rain-d650593d.jar` is ready without main-server installation. No resource-pack/template edits.
