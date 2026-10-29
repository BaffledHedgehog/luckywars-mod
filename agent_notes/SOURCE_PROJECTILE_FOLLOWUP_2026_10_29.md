# Captured physical projectile follow-up — 29 October 2026

This repairs shared behavior in the staged full-pool Chaos dependency chain.
It does not release Chaos or enable an incomplete replacement pool. Main stays
`d9087f0a`; no staged artifact was installed. User's original self/ally physical
projectile and ordinary-source knockback rules apply to these new actors too.

## Repairs

Captured causal authority previously excluded the original emitter UUID for all
descendants. Native arrows and Shulker bullets now retain normal native muzzle
immunity until they leave the shooter, and can subsequently return into that
shooter or hit allies. The real native projectile owner remains its NPC; the
original causal player's UUID, influence order, WILD policy and epoch remain
separate. Spell rays and protected wand carriers retain their emitter exclusion,
source exclusions and role protections. Greed remains an actual physical
deflector and strips the former captured source on reflection.

The final acid admission hook also accidentally applied its emitter exclusion to
EVERY captured source, including unrelated native arrows and melee. It now
applies only to actual source-acid projectiles. This was an independent second
barrier: collision admission passed while native hurt was still rejected. No
fake damage, immunity bypass or alternate projectile engine was introduced.

Stevari's raw source base attack_knockback100 was being loaded directly. Fresh
actor payloads now pass through the existing one-time SourceKnockbackBalance,
as required by the user's ordinary-mob migration rule. His bone's KnockbackI is
preserved, base becomes4 and combined source budget is5. Native runtime attribute
limits remain unrestricted; imba exceptions still belong to the shared adapter.
Other source stats/equipment/IDs/loot and native slime splitting remain intact.

## Native proof and limits

Exact tested JAR SHA256:
`797a3cd1686477b8be67fe29fa4003b3e585f12138e93cdc4e04f8091bf1e238`.
600 Java sources/1737 classes/3411 processed resources +build.gradle =5749 frozen
inputs; all packaged outputs match, except Loom's verified nested-SGUI declaration
in fabric.mod.json.198 protected author paths,31 live grimoire overrides and main
JAR remained byte-exact. No RP or legacy-source edits.

One actual mode1 match passed14 gameplay suites plus5 setup checks in43.28s:
projectile-self-hits, source-knockback-balance, apocalypse-actors,
apocalypse-events, shish-encounters, hostile-electro, sauvojen-boss,
potion-spawner-drops, techno-armor, signed-effects-math, effect-layers,
time-stop, time-stop-contacts, elemental-grimoires.

The extended physical-projectile fixture uses the actual Shish cyborg producer
and native Skeleton.performRangedAttack. It verifies inherited accepted authority,
newborn muzzle immunity, actual accepted native hurt against causal player,
teammate, allied cow and the NPC emitter itself; native DamageSource still names
the NPC and arrow, captured cause retains exact original player UUID/order.
Also covers physical Greed reflection/no hurt/source transfer, service-role
exclusion and a native emission/hit while only the actual author's UUID resolver
entry is temporarily absent, restored in finally. No fabricated player or private
clock. Scoped fixture shutdown clears only exclusively owned actor/ray services.
Resolver masking does not claim a real socket disconnect/rejoin test.

Normal owned-child shutdown, exact snapshot restoration, no retained locks,
check failures or cleanup errors. Same pre-suite Polymer chunk-context diagnostic
as earlier stages. No physical-client rendering/network/latency or peak TPS claim.

Evidence: `build/verification/projectile-sources/artifact-proof-797a3cd1.json`,
`797a3cd1-native-stage-results.json`, `797a3cd1-armor-sets-gameplay.log`,
`post-native-audit.json`. Rejected2a7df79d and4338d5c2 native logs/JARs retained:
they exposed the second acid filter rather than weakening the expected self-hit.
An initial runner typo was rejected before launch; stale prior-run copies are
explicitly named `stale-before-start-*` and are not evidence for this candidate.
Compiler/debug build logs retained as well.

Full three-apocalypse natural schedules, nuclear expiry/cascade, all40 crates,
long cloud cadence and remaining Witch/reward/lifecycle dependencies are still
pending. `qualified=false`, `releaseEligible=false`; original full-chain gate
continues. The previous bounded stage's source-arrow coverage gap is now closed
for the actual tested native NPC path, not every future physical descendant.
