# Environmental effect queue audit ? 26 October 2026

Read-only source audit; no performance measurements, production changes, builds or server actions. Target request: make bulk effect terrain respond promptly; high fire-resistance area damage must equal standing in ordinary fire and stay independent of effect level. This is a target requirement, not a claim about current behavior.

## Current timing and concrete delay

Source: [SignedEnvironmentalEffects.java](../src/main/java/me/skywars2/game/SignedEnvironmentalEffects.java), especially fireTick94?121, conduitFill125?153, tickWebs229?247; [LbcColors.java](../src/main/java/me/skywars2/game/LbcColors.java)278?290; [living injection](../src/main/java/me/skywars2/mixin/SignedEnvironmentalLivingMixin.java)40?44.

* Fire runs only on even entity ticks. Its local cube (radius0 for II, radius1 for III+) is immediate. IV+ extended radius is ceil(sqrt(level?2)), capped15. Each call examines only128 CUBE offsets, including rejected offsets outside the sphere, in x/z/y order starting at the lowest corner. Radius15 has29,791 cube offsets: full traversal needs233 calls /466 game ticks /23.3 seconds at20TPS. The first possible sphere point (0,?15,0) is cube offset480, so even first extended output can wait four calls. Radius/center changes reset cursor; continuous block movement can starve distant strips. No actual queued Runnable causes this delay: this is retained scan cursor with a very small fixed allowance.
* Conduit IV+ similarly has128 cube-offset work/source/server tick, with total2048 across sources and rotating starting source. Radius15 traversal needs233 ticks /11.65s with <=16 active sources. More sources share the global limit. Nearby3? water is immediate. Center/radius resets restart scan; water maps are copied into an ArrayList every tick and revisit completed unchanged volumes indefinitely.
* Weaving death jobs scan a full cube (radius ceil(sqrt(abs(level))), cap50), never a sphere, starting at lowest corner. Global2048 offsets/tick; one job can consume the whole budget before requeue. At radius50,1,030,301 offsets take504 ticks /25.2s. Skipped unloaded cells, invalid support and nonreplaceable cells all consume budget. Remaining desired placements can outlive useful candidates, and no further pass occurs after cube exhaustion. Preserve this completion contract unless intentionally changing it.
* Fire has no shared global terrain budget: each affected entity receives its128-offset slice independently. High fire also traverses every loaded entity every2 ticks; damage radius is full signed level (not terrain radius), so spatial queries cannot blindly use the15-block cap. No cost percentage is established by source alone.

## Exact cached native fire behavior

Read from local `.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-d6f9bb6878/26.3/minecraft-merged-d6f9bb6878-26.3-sources.jar` (no decompiler/build required):

* `net/minecraft/world/level/block/FireBlock.java:55` constructs ordinary fire with damage1.0F.
* `BaseFireBlock.java:127?134` applies FIRE_IGNITE and invokes `hurt(damageSources().inFire(), fireDamage)` for contact. `:136?149` increases ServerPlayer fire ticks by random1..2 and ignites for8seconds once nonnegative.
* `Entity.java:560?568` applies `onFire(),1.0F` when remainingFireTicks%20==0 and outside lava, then decrements fire ticks.
* `LivingEntity.java:1212?1225` rejects repeated equal/lower damage while damageCooldownTime>10; accepted full hit resets cooldown20. Ordinary continuous fire contact therefore normally accepts1HP about every10ticks (2HP/s), with burn calls sharing cooldown. Do not bypass cooldown or clear lastHurt to force a numeric rate.
* Current high aura uses `onFire(),2` every2 ticks: constant across SOURCE levels already, but approximately twice ordinary contact damage under native cooldown. SignedEnvironmentalLivingMixin preserves positive fire-resistance immunity and multiplies negative TARGET resistance fire damage; preserve those rules.

## Implementation pitfalls / useful bounded benchmark

Prefer ordered precomputed offsets (near-first sphere for extended fire/conduit; preserve weaving cube candidate domain), avoiding rejected cube offsets and repeated distance arithmetic. Retain lifecycle, chunk residency, center-change behavior, target protection, Time Stop, block predicates, water neighbor notification and indirect-kill cause scopes. Precomputed geometry is safe pure computation; world reads/writes remain server-thread work. Consider measured time-budget bursts with useful headroom instead of merely another tiny fixed count. Larger fixed allowance alone can multiply cost across many fire actors. Preserve legacyII/III behavior and full signed stored levels.

Measure one stationary and one moving radius15 fire/conduit source,16/32 sources, and radius50 weaving on resident controlled replaceable/support terrain. Record first nonlocal write latency,90%/completion latency, attempted versus changed blocks, oldest pending work, and total server-tick duration/p95/max. Add repeated center changes and unchanged completed fields. Compare exact old/new fixture and preserve authored terrain outside fixture. An isolated native fire-contact player versus aura target over200 ticks (same defenses, zero regen, both fresh cooldown) verifies damage amount/cadence, then overlapping sources and negative/positive target resistance verify cooldown/immunity. These are suggested bounded checks, not executed results.

Applicable instructions: AGENTS.md CPU/time sections at4079?4115 prioritize latency jointly with tick cost and forbid unmeasured speed claims; agent_notes/AGENTS.md requires preserving user notes, exact source links and distinguishing implementation from observed behavior.

## Follow-up read-only review of EffectTerrain candidate

Reviewed new [EffectTerrain.java](../src/main/java/me/skywars2/game/EffectTerrain.java) plus integration. A bounded standalone Python translation of the exact Web.next arithmetic verified every radius1..50: no duplicates, every emitted point belongs to its shell, and total is exactly(2r+1)^3. This checks enumeration arithmetic, not compiled Java or native writes. All-protected pending queues exit after one rotation; active jobs reset the paused counter and consume useful bounded work. No protected-field busy spin found. Shared time checks every16visits permit one native write cost and up to15additional visits beyond deadline;6ms is a target, not a hard interruptible bound.

Legacy fireII/III radius/local cadence and conduitII/III branch remain. High terrain radii, full-level damage radius, source/target protections, water attribution scope, Time Stop and clear lifecycle remain visible in integration. Dormant completed fields wait for their producer to refresh; active moving fields finish their old scan intersected with current radius then queue newest center. No off-thread world access was introduced.

Concrete preservation edge: fireCell now checks canSurvive before setBlock. Native BaseFireBlock.onPlace (cached source152?166) checks and creates a valid Nether portal BEFORE its canSurvive removal. Consequently unsupported fire placed in a valid portal frame previously could ignite it; the precheck can suppress this native side effect, including legacyII/III local fire. Calling this only removal of two useless writes is incomplete. Preserve portal behavior if ordinary native fire-placement side effects are part of the contract. No production edits made by this audit.

## Root integration after review

The final fireCell retains PortalShape.findEmptyPortalShape before skipping an unsupported placement, preserving native portal ignition. The fixed6ms draft budget was replaced after the user's clarification with current-tick headroom to a54ms soft total target (2ms remaining-hook reserve). START_SERVER_TICK stamps the deadline; world operations remain on the game thread. The final native two-match results are recorded separately in EFFECT_TERRAIN_LATENCY.md.
