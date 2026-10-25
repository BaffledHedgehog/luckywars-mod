# Antigravity terrain/support correction - 25 October 2026

User scope: fix misaligned lifted terrain, sample real nonempty terrain rather than air cubes, and lift only actors supported by the lifted blocks. The earlier user choice of the finite15-second Paper storm remains authoritative. This correction supersedes the offset/proximity/area-lift parts of ANTIGRAVITY_PORT.md; its original qualification does not qualify this implementation.

## Sources reread

Read-only source server `E:/servers_mew/cringe1_21_4_skywars`:

- Paper `plugins/luckywars/src/main/java/me/luckywars/swrg/SwrgNativeService.java:6824-6943`: initial3x3x3 cube,30 pulses every10ticks, XZ offsets[-16,16), fixedY, solid-only exact states, no container drops, display offset(-.5,0,-.5), .04 acceleration while speed<3, broad display AABB1 lift and global pulse AABB17/60/17 lift/Slow Falling, stale20 cleanup with player80/110/80 accompaniment.
- Legacy `world/datapacksdisabled/LBC/data/lbc/function/drops/antigravity/start.mcfunction`, `tick_10t_marker.mcfunction`, `tick_10t_marker1.mcfunction`: perpetual marker, three spreadplayers surface samples per cadence, escalating gravity. These do not replace the user-selected Paper duration/cadence.
- Exact native26.3 cached sources: `net/minecraft/world/entity/Display.java`, `net/minecraft/client/renderer/entity/DisplayRenderer.java`, `net/minecraft/client/renderer/block/BlockModelResolver.java`, `BlockModelRenderState.java`, `net/minecraft/world/level/chunk/ChunkAccess.java`, `LevelChunkSection.java`, `net/minecraft/world/entity/Entity.java`. Native BlockDisplay submits the unit block model at its identity-transform origin, without a compensating half-block translation.

## Final implementation

`LbcAntigravity.java` places each display at the original integer lower corner, preserving exact orientation/state and identity transformation. This intentionally corrects the old Paper half-block XZ displacement.

An exact original3x3x3 sample is retained whenever it contains a solid block. Otherwise the sample searches its nine resident columns for the solid surface nearest trapY. Native empty sections are skipped. If those columns have no surface, a lazily built sparse index of resident solid columns inside the original32x32 center footprint supplies a live-revalidated fallback. The index is bounded to one storm and is discarded with it. Columns are never force-loaded; emptied/unloaded candidates are removed. The fallback can changeY to reach a real island below an airborne token. Every launched cube still uses offsets-1..1 in all axes; all-air terrain never creates a visual or an entity-only pulse. If there is no solid terrain, that scheduled pulse quietly does nothing.

One entity query per launched cube captures eligible actors before terrain removal. Each captured block uses the actor-specific native collision shape; feet must overlapXZ and touch an actual top within1/16block. Every subsequent impulse rechecks the same shape at the moving display's previous position. A per-tick identity set prevents duplicate commands when one actor straddles multiple blocks. Captured passengers have bounded contact history; at most one movement-step lag below the surface is accepted. XZ departure, excess height, jump/external upward departure, protected/removed actors and native ceiling collision permanently detach that passenger, so there is no distant or floating recapture. This removes the broad Paper proximity lift and unconditional origin-area pulse lift, as required by the user. Supported actors receive a vertical velocity servo targeting `nextSurfaceTop-currentFeet`, with XZ velocity retained, native hidden ambient Slow Falling30 and actual thrower displacement credit. Slow Falling refresh is restricted to accepted supported contacts. A player or ally standing on terrain remains WILD-eligible; active Greed and service/observer roles use the existing shared target guard. Frost rejects impulse/status; Time Stop freezes producer ages, displays and passenger updates.

The source initial launch plus30 ten-tick opportunities through300 active ticks, horizontal footprint, acceleration loop, exact block states, no-spill/no-block-loot removal, player accompaniment/stale20 cleanup, owner/epoch, unload orphan disposal, and quiet actual-stop cleanup remain. Existing separate global altitude hazards are unchanged. No source server/resource-pack changes or new public item registrations are involved.

## Native passenger physics correction

The first correction still used the old source `displaySpeed+.2` launch while requiring strict contact on every subsequent service tick. Real26.3 gravity/drag separates the actor from the platform, then the accelerating visual crosses its feet between contact checks; a service-only fixture could not expose this. Native `LivingEntity.travelInAir`/`handleRelativeFrictionAndCalculateMovement` moves using the current delta first and applies gravity/drag afterward. The corrected servo sets vertical delta to the next surface displacement plus the small current contact error. The following actual native move reaches the new surface; the next service update compensates the real gravity/drag result. There is no `.2` bounce, position teleport, forced onGround state or riding/mount UI. Native movement collision clips a blocked upward move, and the following support update detaches a ceiling-blocked actor. This intentionally replaces the source launch impulse to meet the requested terrain-riding behavior.

## Focused checks and status

`AntigravityDropChecks.java` now checks integer lower-corner placement and original oriented states, actual island terrain7blocks below the token, nonair-only visuals, true supported passenger versus hovering/adjacent actors, native half-slab collision-top support,40 actual Cow.tick movement steps against the support servo, a real native jump/permanent departure, a native solid-ceiling collision, departed-passenger rejection, protected roles/active Greed, native thrower void credit, and unsupported caster/ally immunity to broad pulses. Existing container no-spill, finite300ticks, stale19/20, accompanied beyond320, Time Stop, token debit/epoch/unload and actual match-stop probes remain.

The native fixture snapshots/clears nonair cells in resident nonempty sections beneath the sampling footprint before running, and restores every snapshot afterward; an off-height terrain search cannot silently consume unrelated unsnapshotted map terrain.

An earlier native run exposed an exact-double fixture comparison (`.04+.2` versus literal`.24`); support assertions now use an epsilon. Source changes prepared; compilation and actual native fixture qualification are performed by the integrating root agent. This note makes no client-pixel, peak-load, natural-physics or disk-restart claim.

Final root qualification: build `b4cdfa8d`,148 recorded results across two actual
mode1 matches and actual stop cleanup. See [combined review](GAMEPLAY_REVIEW_2026_10_25.md)
for the tested artifact, deployment state and verification boundaries.
