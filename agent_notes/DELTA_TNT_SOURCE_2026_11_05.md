# Delta TNT full source closure audit — 5 November 2026

Read-only audit after HEAD 0fd08e4b. Only this new note written. Legacy root `E:/servers_mew/cringe1_21_4_skywars/`; native root this repository. No Java/resources/build/server edits or qualification run. Full IMBA root remains closed, TMT LAST. Previously qualified terrain and egg leaves were not reaudited. Initial Serena instructions and root/agent_notes AGENTS read; scoped symbol lookup works, unscoped lookup fails cross-mount.

## Reuse verdict

| Native owner/API | Actual reachable behavior | Delta gap |
|---|---|---|
| `LbcDroneDrops.groundbam(PrimedTnt)` public, line257; `DroneDropMixin` line47 | Called from TNT tick; requires managed drone, active match/epoch and TimeStop clear. Only onGround/remove groundbam/setFuse2. | Raw Delta tags cannot enroll; no evoker/fire/electro/strong behavior. Do not reuse as complete landing. |
| `LbcSpecialFireballs.loaded(Entity)`, `convert(Snowball)`, `tick()` public, lines47/65/108; private `insert`, `landing` lines55/126 | LbcItems line2262 ticks actual tracked actors. Registered CHILD TNT performs nine fangs per eligible mob within10, 11³ AIR-tag fire writes, raw lightning and separate power4 immediate blast. Attribution/epoch/frozen/resident protections exist. | Full combined implementation exists, but landing unconditional for all four effects; private and CHILD-only. Needs shared variant-aware enrollment/landing API; raw individual effect tags do not activate it. Reusing combined landing for single variant is incorrect. |
| `LbcExoperso.cast(ServerPlayer,boolean)` public108 | Owned player-eye charge, normal319/upgraded160 windup, captured look, flight, native nuclear series. | No arbitrary point/direction immediate Lifetime321 API. `edibleBurst` package224 reuses impact only and skips required flight. Extract explicit released charge entry point into this owner. |
| `LbcElectro.lightning(ServerPlayer,Vec3)` package573 | Creates/tracks owned SHOCK_BOLT lightning with epoch and lifecycle. | Changes raw lightning into electric shock semantics; source groundbam electro only summons vanilla lightning. `manageLightning` public566 specifically adds EGG_BOLT. Neither is automatically source-equivalent. |
| `LbcExasperato.encounterBlast` package895; `exopersoBurst` existing shared owner | Existing attributed immediate standard blast and full nuclear series, reused by special landing/Exoperso. | Preserve radius4 strong carrier-plus-extra blast; do not turn strong into only one larger carrier. |

No runtime `delta_tnt`/`delta_ex` consumer found in current Java. Source asset flags alone do not establish native behavior. These are reuse candidates, not a passed Delta release.

## Exact legacy entry and clock

Paths below relative to `world/datapacksdisabled/LBC/data/lbc/`.

`loot_table/imba/lucky_items_imba.json:625..674`: weight10 quality5, panda spawn egg count1, tier_k, model lbc:omegatnt, marker entity_data tags delta_tnt/marker_2t/mecha, translated green nonitalic delta_tnt, imba:1.

`function/tick2t:1..2` schedules itself every2t then dispatches marker_2t outside nexus/imprinted. `markers_2t:1` increments Lifetime by2 BEFORE `markers_2t1:7` dispatches positioned ~ ~.2 ~ and returns into other/delta_tnt. `other/delta_tnt:1..42`: set redstone block at adjusted origin, spawn33 stationary slowraycast/imba_rc/delta_ex/mecha markers with data.lifetime1000; six primed sounds (pitch1,0,2,1.5,.5,.2; volume1/minVolume.5); kill root.

Offsets: origin; ±1 and ±2 on each single axis (12); all eight (±1,±1,±1) corners; all12 two-axis ±1 edge offsets. No spreading/movement command exists for Delta marker. Root consumed once; redstone persists.

`tick.mcfunction:1` calls tick_not_timestopped only with no global time stop. `tick_not_timestopped:3` enumerates slowraycast outside nexus/imprinted, BEFORE mobs and projectiles. `raycastselector:1` increments Lifetime1; line4 invokes imba; `raycastselector_imba:4` returns from that dispatcher into delta_ex_tick. This return does not skip caller raycastselector lifetime cleanup lines12..13. Thus 1000 chance-bearing invocations per fresh marker, final1000 invocation included, then kill. 33,000 marker invocations if all survive; no inferred extra blank windup. New exoperso markers are not necessarily part of the current selector enumeration and should begin their own next invocation; do not rely on command-engine dynamic enumeration without a runtime probe.

## All20 independent chance1 rolls

`predicate/chance1.json` is random_chance0.01. `other/imba/delta_ex_tick:1..20` runs independent rolls in this exact order; these are not a mutually exclusive table:

1. TNT fuse100 normal gravity.
2. TNT fuse100 NoGravity.
3. Vanilla lightning.
4–8. Vanilla fireballs ExplosionPower1,5,10,15,20 (no supplied Motion).
9. TNT fuse0.
10. groundbam TNT fuse1000.
11. groundbam TNT fuse100 NoGravity.
12–15. groundbam plus evoker/fire/electro/strong respectively, fuse100 NoGravity.
16–19. groundbam plus evoker/fire/electro/strong respectively, fuse1000 normal gravity.
20. Face random delta_ex marker within10 (self/other clusters eligible), call workingitems/exoperso4.

All successful payloads can occur together. Expectation6.6 initial payloads per world tick across33 markers, not a cap; total expected6600 payloads over1000 ticks. TNT fuse is native entity clock; marker Lifetime is separate.

## Full groundbam downstream

`projectilestick:6` dispatches TNT; `projectilestick_tnt:4` onGround groundbam invokes `other/groundbam:1..7`: fuse2, then optional evoker, fire, electro, strong, no_explode in order, remove groundbam last. This occurs after slowraycast selection that tick. NoGravity does not imply onGround; variant can expire naturally without landing. Every variant retains carrier's own eventual standard explosion.

Evoker: at every entity in #minecraft:mobs within10 excluding tag spectator; `projectiles/arrows/evoker:1..9` summons nine fangs at target feet (center, four diagonal and four cardinal offsets1). Fire: `other/groundbam_fire:1` inclusive cube±5 replaces #minecraft:airs with fire (1331 candidate cells). Electro: one raw vanilla lightning at carrier. Strong: one additional fuse0 TNT, alongside original fuse2 carrier. no_explode not generated by Delta. No partial port limited to setting fuse2 or retaining inert variant tags is complete.

## Exoperso4 downstream and source quirks

`workingitems/exoperso4:1..5` spawns lifetime700 slowraycast/exoperso/player/magic marker at caller point, sets Lifetime321, copies lbcID2 from random @r, copies current facing rotation via tp, marks tagged. Ownership is unrelated to Delta creator; random spectator may be selected. `raycastselector_player:20` only dispatches if a nonspectator player with matching lbcID2 exists. Lifetime still advances and700 cleanup still applies if dispatch never happens.

`workingitems/exoperso2`: <=318 charge FX;319 capture owner's rotation;320 teleport owner-eye;321+ exopersorcall. Delta starts321, so avoids charge/capture/teleport and enters flight next invocation with incremented322. `exopersorcall`: tp current command location, increment Distance, emit particles, explode on non-#airs block, recursively positioned forward.5 through Distance<=4; reset Distance at5. Five samples span two blocks per invocation, no entity-hit condition here. `exopersorc` only sets rotation at319, never reached by Delta.

`exopersoexplode`: lifetime200 slowparticlesexplode visual marker at y-4;32 radius20, fuse0, ignited invulnerable persistent creepers across authored offsets; global player counters and caster tag; two-tick scheduled achievement check; five loud explosion sounds;500 explosion emitters; kill projectile. Shared native Exoperso/Exasperato series is right blast owner, but its native flight uses firstHit including entities and owner validation, unlike this particular Delta source flight. Source achievement globals can overlap other casts; native series isolates. Preserve attributed actual creator under existing universal author rule; random-owner source behavior needs explicit author decision.

## Lifetime1000 global fireball command

`other/imba/delta_ex_tick:21` executes after20 rolls, only Lifetime1000: at EVERY minecraft:fireball in current dimension, face current Delta marker eyes, as a fireball within.01 limit1, call `raycast_vpered_power` (actual spelling is raycast, not raycas). No distance/owner/epoch filter on first selector; nearby coincident balls can select same entity repeatedly. Each33 marker repeats this sweep before dying, so later iteration can overwrite earlier motion. Includes foreign players' preexisting balls and newly spawned successful final-tick fireballs. `raycast_vpered_power:1..6` uses global temporary reycaster marker at world origin facing vector, stores floor(direction*10000)*.0004 into Motion components (approximately speed4 toward Delta), kills ALL reycaster markers. Does not assign continuous acceleration power. Global scratch tags can cross-couple. This is a one-time terminal steering event, not per-tick homing.

## Author decisions and release closure

Unanswered: retain global foreign-fireball terminal sweep and order dependence or restrict to this activation? Retain random foreign/spectator Exoperso ownership, cross-cluster/self aiming and shared achievement globals, or bind causal creator while preserving randomized direction? What to do with self-facing zero-vector direction? Delta root/markers are mecha while Exoperso offspring magic; actual suppression reach/distance and payload behavior under jammer/artifact remains unverified. Tags alone cannot answer this. Native managed lightning changes shock/role semantics; confirm ordinary vanilla lightning is intended for both direct and groundbam electro rolls.

Required before release: full root activation and33 clocks,20 independent decisions with replayable RNG, individual groundbam effects, released Exoperso flight+complete nuclear downstream, terminal global sweep decision, attribution/protected roles, resident bounds, TimeStop, x2/x4 and whole-coordinator replay. Missing downstream must be ported in shared native owner, not omitted. This audit makes no runtime, performance, visual/audio, install or build claim and opens no full IMBA/TMT gate.


## Resolved author decisions and qualified native follow-up

See [IMBA_RECURSION_TNT_2026_11_05.md](IMBA_RECURSION_TNT_2026_11_05.md). TECH placement; all foreign terminal fireballs retained; Exoperso binds the actual Delta creator while retaining random nearby-center aim. All 20 independent branches, 33x1000 active ticks, complete shared landing/Exoperso downstream and native lifecycle/replay are qualified. The original read-only source audit above remains unchanged.
