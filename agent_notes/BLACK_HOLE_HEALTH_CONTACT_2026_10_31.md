# Black-hole body contact and direct HP ? 31 October 2026

Status: implemented and checked in the isolated native fixture; not installed on the main server. The original 25-case Chaos release gate remains closed.

## Behavior

All three hole owners (Void Arcanum, sheep and Academy Duplicating) measure damage contact from the hole center to the nearest point of the target's **current native bounding box**. Their original spherical damage radii, damage amounts, target caps and tick clocks remain. Head contact works even when the feet lie outside the influence radius. Broad-phase box corners outside the actual sphere remain misses. Native pose and scale changes are respected. Existing pull geometry and force are retained.

`BlackHoleDamage` subtracts health directly, without native `hurtServer`, armor/absorption spending, hit callbacks, hit cooldown, hurt-event packets or movement impulses. It retains source inheritance, shared damage admission, native invulnerability, Greed/roles/Time Stop, player PvP admission and kill/combat attribution. Native death protection still evaluates the original damage type; an outside-border source retains its native totem bypass. Chef's existing `setHealth` override still turns accepted HP loss into knock progress. Polymorph sheep mirror HP to their player and resolve lethal contact immediately.

Ordinary player health packets alone are insufficient: native `LocalPlayer.hurtTo` starts a fresh hurt reaction whenever their HP decreases. Each accepted drain therefore publishes native self health metadata before the later health/food packet. That subsequent packet has no negative HP delta and does not renew the reaction. Existing hurt state and velocity are preserved. This is a checked native packet contract; actual client camera behavior still needs an in-game observation. No resource-pack update is required.

## Electricity audit

Tesla moving sources still advance every working tick, with 11 samples at 0.2-block intervals, 0.5 damage per accepted sample and the original per-sample target cap. The electric damage types are in the loaded native cooldown-bypass tag. A new native regression checks four real Tesla sources over two advances with an active hurt cooldown, counting every intersecting body sample and exact HP loss.

Legacy `lbcr` electrical raycasters used a 1x1x1 selector cell at each sample; the present exact line/body test can produce fewer contacts. The later user reply chose precise admission with restored pulse frequency; implemented in [the follow-up](ELECTRIC_CONTACT_CADENCE_2026_10_31.md). The black-hole patch itself did not alter electrical damage, cadence or geometry. Yellow Conduction's terminal cactus trap and native lightning deliberately retain their separate ordinary-cooldown behavior.

## Evidence

Artifacts and exact logs/results: `build/verification/black-hole-health-contact/`.

- Final JAR SHA-256: `5752d8480f060959f7e5c90dd2145f38279603951e8b1b171b40ffa54c74dd56`; offline Gradle build passed.
- `84cbc711` passed six native gameplay groups: sheep, duplicating-academy, tesla, polymorph, polymorph-death and vanity-rollback.
- `da7cad29` passed void-arcanum, including actual tall-head contact, pull, terrain and visual lifecycle checks.
- `5752d848` passed black-hole-contact: standing/swimming/scaled geometry, tangent/corner cases, repeated direct HP, absorption preservation, temporary/permanent native immunity, unchanged hurt timers/velocity, attribution, self health packets, native totem and polymorph HP.
- `production-continuity.json` establishes byte-identical production classes and data across these artifacts. Only the named check classes differ. These are eight staged passing gameplay groups, not a claim that the earlier aggregate runs had no failures.
- Earlier failed proofs are retained. Fixture corrections set a sufficient MAX_ABSORPTION, distinguish true native temporary immunity from damage cooldown, acknowledge player load/teleport, compare against actual capped initial HP and wait beyond the six-tick shader-row retirement.
- Each native child stopped normally; fixture snapshots were restored exactly, with no retained locks. No main-server process was restarted.
- `preservation.json`: all 196 pre-existing author paths and 31 live override files remain exact. Main JAR remains `d9087f0a07a321a5a9e57cac7f6f58a2d4ca6a86da9829a52623799f0342e087`.
