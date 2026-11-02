# Armor/Zeus native check scope (2 November 2026)

Final author policy and native qualification supersede this historical implementation/source note: [completed port](ARMOR_ZEUS_SKILLS_PORT_2026_11_02.md). Do not repeat resolved questions.

`ArmorZeusSkillChecks.run(GameController)` prepares the actual TestAlpha/TestBeta
participants and performs bounded structural/native checks. It then leaves a
staged fixture for `ArmorZeusSkillChecks.tick(GameController)` to observe actual
registered native updates; PASS is published only after that stage completes.
This source addition has not itself been built or executed. Root qualification
must record actual build/native outcomes and retains ownership of integration.

The immediate checks cover original category10000 slots6/22, their actual
Paladin/BoltBolt advancements and real OP menu callbacks; default and custom
armor attribute entries, all matching positive flat entries, proportional drain,
metadata and unrelated components; actual player damage, native Resistance
before pool drain, terminal overflow, next-hit leakage, native hurt/cooldown
bookkeeping, same-tick equipped attributes, bypass exclusion, global disable,
the accepted BlackHoleDamage rawHP bridge with native hole momentum/hurt preservation,
and MAG/TECH/strong-artifact positive controls for entirely unjammed skill7.

Native Cow bodies verify lightning4, owner attribution, chain-once behavior,
ordinary native hurt cooldown, unrelated targets, solid voxel clipping, and
released arcs crossing a MAG boundary but stopping at TECH. Exact geometry uses
the actual Cow AABB with a positive contact and a nearby non-contact. Two
deterministic RandomSource outcomes exercise the actual pulse's one nextInt(2)
trial. The staged clock measures20 actual registered updates, TimeStop pause,
global disable, MAG launch rejection and strong-artifact launch rejection.
No fabricated world or manual loop advancing game time is used.

Expected source APIs: `game.items().zeusSkill()`, package `fire(owner)`, package
`phase(owner)`, public `clear(owner)`, and native registered per-player `tick`.
The root must call the checker tick from its existing native check dispatcher.

The fixture uses existing Snapshot/ScheduledClient infrastructure and restores
profiles, advancements, scores, native attribute/equipment snapshots, statistics,
use state, actual player bodies, attacker credit, cooldowns, configuration,
tick freeze state, bounded terrain, owned entities, and temporary RNG delegation.
Operator status remains owned by the runner. Armor fixtures are unbreakable.
