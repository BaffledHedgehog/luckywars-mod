# Acid Burst, Ant Swarm and shared acid corrections

22 October 2026. Qualified native implementation; ready artifact, not installed.
[Source audit](ACID_ANT_DROPS_SOURCE.md).

## User decisions (22 October 2026)

> Да, муравьи опасны для всех

Ant targeting, melee and acid are WILD: thrower, allied players and ordinary
mobs remain eligible. Service NPCs and observer roles remain protected. The
source poison immunity of muravei-tagged actors remains; burning acid can hurt
them. Ordinary native reinforcements keep the same causal danger policy.

> Да, вернуть прохождение сквозь существ

All shared acid projectiles now use native block collision only. Their moving
aura still applies effects to eligible entities while the projectile passes
through them. Block collision still sticks the projectile to the surface and
does not shorten its lifetime. Existing wand/Magnum/chemical variants keep their
other physics, effects and authored budgets. Acid aura and age now pause for all
variants during Time Stop, alongside their already frozen native movement.

## Complete native closures

Public leaves: `lbc:ported/acid_burst_drop` and `lbc:ported/ant_swarm_drop`.
Both retain the source's original names/lore/styles/models. Actual RP and template
are unchanged. Internal `lbc:ant_rewards` preserves the guaranteed native burst
token and independent10% nested existing native `lbc:ported/acid_wand` reward.
No placeholder wand, complete acquisition pool or unrelated reward is exposed.

Burst creates three randomly aimed shared acid projectiles at tokenY+1.01/1.02/
1.03, with500-tick budgets and no shield-blockable tag. Ant shots instead use
300 ticks, source eye aim/spread and shield blocking. Both begin at age0, persist
age/budget/cause/epoch, and share the existing acid sticking, reactivity and aura.
Burning retains its source accelerated aging. Author is independent of native
projectile owner; reflection/native owner can take priority. Common damage and
effect attribution preserve the real thrower for subsequent consequences.

Swarm creates five source-configured persistent silent baby zombies,10HP,
infinite InvisibilityII, exact head model/unbreakability/drop chance and authored
attributes including spawn_reinforcements100. Native AI, attributes and native
reinforcement probabilities/placement/finalization remain authoritative. Each
caster retains the independent per-tick sound and acid chances. Nearest valid
living source-mob targets exclude other muravei-tagged actors.

The reinforcement hook attaches only author/epoch/lifecycle identity before
native finalization and insertion. Ordinary children receive native equipment,
loot and AI, not ant visual/caster/reward tags. A newly created native jockey
chicken inherits the same lifecycle identity before insertion; an existing
chicken selected as a mount remains unclaimed. Native conversions retain their
copied tags and normal conversion equipment/loot rules. Candidate guards inspect resident chunks
without requesting generation. Shared role, WILD damage/effects, Fire Aspect
causes, reload/stale epoch and quiet actual-stop cleanup are integrated.
No extra spawn guarantee, arbitrary TTL/cap or world scan is added.

The shared legacy lucky_mob dispatch was also reviewed: mobs_tick:6 reaches
mobs_tick_lbc:8 and muravei_tick. The separate mobs_5t:10 / mobs_5t_lbc dispatch
contains no additional ant branch. No other boss behavior is inferred from
the shared lucky_mob tag.

## Qualification

Offline Gradle build and `python tools/smoke_acid_ant_batch.py --server-dir
build/verification/key-reuse-server` passed.136 recorded suite results across
two actual mode1 matches,112.47s, retain all130 previous regression results.
The new stop probe is last and its cleanup first, covering five ants, one newly
created native jockey chicken, three burst projectiles and a paused pending token
through the actual match stop, with no reward or XP emission.

Ready artifact: `build/verification/skywars2-acid-ant-batch-08f09e8f.jar`.
SHA256: `08f09e8f5961282946c48254a7e76045ddab6374c322cb826c0b3b747a82429d`.
All220 Java sources match the source JAR byte-for-byte;615 compiled classes and
2697 processed resources match the tested release. Evidence/manifest live under
`build/verification/acid-ant-batch-final/`. Main JAR96ee50a7 and user's Reroll
source6ef80947 are unchanged. Not installed. The isolated server is stopped,
original files restored and fixture locks released. Only the known fake-bot
Polymer packet-context ERROR pair remains; no check failures.

Native fixtures cover exact tokens/presentation/source order/unit debit, ant
payload/head/attributes/effects, nearest WILD target selection and source shot
geometry/spread/shield distinction. Shared acid physically traverses entities
for new and existing ordinary/Magnum variants; native block sticking remains.
Cold acid poison attribution uses a real poison tick; burning aura death is
attributed directly. No post-contact ignition is invented. Real native melee,
Fire Aspect consequences, accepted-source priority, protected roles and actual
death loot with guaranteed burst plus independent wand rolls are exercised.

Source age budgets are inspected in native serialized payloads, then advanced
through actual ticks to expiry. Shield tests exercise native use and blocking.
Three actual stopped ticks retain both source and old ordinary acid age, motion
and aura; repeated token stacks debit on successive actual dispatch ticks.
Same-match service reload, persisted cause/epoch, stale actors/pending token,
removed/unresident admission and quiet actual stop are checked.

The reinforcement test calls actual native hurtServer under controlled resident
spawn conditions and inspects the natively admitted child. Source ant tags,
custom loot, visuals and acid-caster behavior do not leak into that child.
Native finalization is then explicitly replayed with seeded jockey branch inputs
to verify a newly created chicken inherits identity and an existing unrelated
chicken remains unclaimed. Native mounting/collision and60-tick dismount cooldown
are respected. This is a finalizer integration fixture after real reinforcement
admission, not a claim that both mounts appeared autonomously in ordinary play.

Retained development evidence: `attempt1/` exposed fake ServerPlayer item-use
progress: its tick alone does not run network-driven doTick. The shield fixture
now performs six native doTick updates and still requires actual isBlocking.
`attempt2/` exposed the native60-tick boarding cooldown after dismount. The
fixture now waits60 actual server ticks, rather than forcing mounting or removing
the original ownership assertions. Production source review also moved causal
identity before reinforcement finalization and newly created jockey insertion;
existing foreign chickens stay untouched.

Limits: no actual client rendering/audio, disk restart/chunk unloading/
disconnection, autonomous long-running reinforcement/AI population, offline coin
framework or peak capacity benchmark. Reload checks use native payload inspection
plus service callbacks, not disk restart. Freeze assertions cover the stated
native ticks. Native seeded finalization and controlled reinforcement placement
are distinct from natural AI waiting. The112.47s suite includes boot, maps and
regressions and is not an MSPT benchmark. No arbitrary lifetime/cap is introduced.

Next source-only group: [normal Fat Skeleton and normal/old Random Portal](NEXT_DROPS_AFTER_ACID_ANT_SOURCE.md).
Their source/reward closure is audited; implementation and any portal marker-scope
author decision remain future work.
