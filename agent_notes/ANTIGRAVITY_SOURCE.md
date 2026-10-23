# Antigravity source audit (23 October 2026)

Read-only source audit; no build/server or runtime qualification. Source root: E:/servers_mew/cringe1_21_4_skywars. Current native Paper drop is authoritative for this port; legacy datapack and standalone command differ materially.

## Native drop

`plugins/luckywars/src/main/java/me/luckywars/swrg/SwrgNativeService.java`: native working-drop dispatch3964-3966, antigravity interception4260, `runNativeAntigravityDrop`5700-5717, `startAntigravityStorm`6824-6868, `launchAntigravityBlocks`6870-6892, `spawnAntigravityDisplay`6894-6943, Bedwars helper6945-6951.

- Requires custom_data antigravity=1. Uses item location floored to block XYZ. Sends literal title Antigravity / Started to same-world nonspectator players in spherical radius17; title timings0/40/10. Starts storm and decrements one token.
- Immediately launches one cube; then30 pulses at ticks10,20,...300. Next callback tick310 cancels. Each pulse chooses independent continuous X/Z offsets in[-16,16), unchanged floored origin Y; no spreadplayers/terrain-height search. Cube samples floor(center)+dx/dy/dz for each offset -1..1:27 candidates per launch,31 launches, at most837 displays per activation before overlaps/non-solids. This is a derived finite opportunity bound, no explicit population cap.
- Each pulse emits25 campfire cosy smoke at pulseOrigin+(.5,1,.5), spread1/1/1 speed0; global parrot-fly and stone-break sounds BLOCKS volume1.5 pitch1.
- Each pulse queries living entities in AABB half-extents17 horizontal/60 vertical about FIXED origin (not pulse location). All living entities except spectator players qualify; no team/author/role immunity in this source. Set vertical velocity max(existing,.55), retain X/Z. Apply slow falling30t amplifier0 ambient=true particles/icon=false.
- Players in that box at Y-originY>=30 receive4 generic unattributed damage each pulse, drowning tag, one translated damage_from_no_air message on tag entry. Lower players in the same box lose drowning tag; leaving the box does not explicitly clear it. No native cosmos creeper or absolute-height hazard in these methods. Causal credit for direct damage and upward/terrain/fall/void consequences must be added under current user indirect-kill rule, not treated as intentionally absent.
- Only solid blocks launch. Clone exact BlockData (orientation/state retained); set original AIR with physics=false unless Bedwars protected snapshot. No container content copy/drop, block-entity snapshot or eventual terrain restoration is implemented here.
- Bedwars protection means manager exists, enabled, block inside its region AND coordinate is snapshotted. Protected source block remains, yet its visual copy still launches. Ordinary unsnapshotted/new blocks are removed. Do not generalize into protection for every Bedwars-region block.
- Display spawn blockLocation+(-.5,0,-.5), exact cloned BlockData, viewRange128f, no shadow, FIXED, interpolation1, teleport1, yaw/pitch0. Native drop does NOT set brightness (command does).
- Per-display anonymous runnable holds only velocityY (initial0), staleTicks; every tick from+1 add.04 while velocityY<3, then teleport straight up by resulting velocity. Floating arithmetic can exceed nominal3 slightly; preserve loop semantics. No gravity/rotation/damage/display collision. Nearby living AABB1/1/1 except spectator players receive max(existingY,velocityY+.2), X/Z retained.
- Every display tick increments staleTicks, resetting to0 if any nonspectator Player lies in AABB80/110/80. Remove after20 consecutive ticks without such player. Player accompaniment can sustain indefinitely; there is no absolute TTL or world-height cap. Per-activation pulse count and per-display age/velocity live in anonymous tasks, no shared field, scoreboard, PDC, persistence, author or lifecycle registry. Invalid/dead display cancels worker.

## Separate registered command

`plugins/luckywars/src/main/java/me/luckywars/command/CommandRegistry.java:74-75` registers antigravity; `command/AntigravityCommand.java:32-96` launches one cube, execute110-116, permission120-121 lws.antigravity. It is not called by native drop.

Command samples sourceLocation+(dx-.5,dy,dz-.5), preserving fractional display coordinates and different block-floor selection. It clears nonsolid blocks too, uses default AIR physics, destroys solid blocks without Bedwars check, reconstructs default material block state, forces brightness15/15. Display lift targets Players including spectators; overwrites Y with velocityY+.2 rather than max. Any Player including spectator prevents stale cleanup. Same .04/<3 trajectory and20 unaccompanied-tick cleanup. Nonplayer command executor is removed only when zero solids launch. These differences should not silently override native-drop behavior.

## Legacy datapack glue (not native storm specification)

Paths below relative `world/datapacksdisabled/LBC/data/lbc/function/`.

- `drops/antigravity/start.mcfunction`: adds10 Lifetime to executing item, summons magic/antigravity_marker/rc_10t marker with NO data.lifetime; translated title/subtitle radius17. `area_effect_cloud_10t.mcfunction:1,4-6` adds10 marker Lifetime per10t, calls marker worker, kills only if data.lifetime>=1 and reached. Thus marker has no authored finite TTL.
- `tick_10t_marker`: three temporary markers each cadence, select up to3 within.1, spreadplayers radius0..16, teleport down1; macro command resolves `antigravity` and invokes registered command. Each temporary marker is killed afterward. Marker center uses surface search rather than native fixed Y.
- Marker Lifetime>=50 refreshes gravity: storage lbc.math attr=-.0001*Lifetime, additive gravity modifier antigravity_effect for #mobs spherical radius17. No escalation clamp. Removes modifier first nearby and from distant>=100 mobs only when no antigravity marker within100; intermediate17..100 band not covered by this cleanup. This is legacy scoreboard/storage/attribute behavior, absent from native drop.
- Global `tick_10t` increments antigravity counter; chance5 predicate counts marker population and if <=30 summons another at absolute0,100,0 (can reach31). Caller in `tick10t.mcfunction:2` is COMMENTED; `mobs_10s` refresh caller also commented. Do not import this as native cap or live reproduction.
- `tick_10t_player` is still referenced by `players_10t.mcfunction:5` (global nonspectator dispatcher in tick10t:4, excluding nexus/imprinted). It tests existing y scoreboard150000..14499000 for4 drown damage/tag/message,1500000..14499000 for instantaneous ignited radius100 creeper named cosmos. Neither branch checks storm proximity. Numeric score thresholds are certain; score-to-Y conversion was not traced here, so do not assert world Y150/1500 without that additional trace. This global legacy caller is separate from native drop storm.

Item source `plugins/luckywars/src/main/resources/datapack/data/lbc/loot_table/herman_antigravity.json`: one repeating_command_block, tier_ff tooltip, drop_trap model, yellow nonitalic drop_antigravity name, white trap_desc lore, {antigravity:1,drop:1}; appears in loliluckyblock pool. No distinct old antigravity leaf found in this scoped audit.

No unavoidable author question for a faithful native-drop port. If choosing to restore perpetual legacy storms, gravity escalation, surface placement or global space hazards, explicitly obtain that scope decision; those are materially different behavior. Existing WILD/role safety, causal author/epoch, Time Stop and quiet-stop contracts need integration in the new mod and are not evidence of old source behavior.

## Author decision (user,23 October2026)

User explicitly selected the15-second Paper storm instead of perpetual legacy
gravity escalation. The finite native-drop specification above is the target.

## Container removal: concrete old Paper bytecode proof

Read-only javap of `E:/servers_mew/cringe1_21_4_skywars/versions/1.21.11/paper-1.21.11.jar` with JDK25, no server execution:

- CraftBlock.setType(Material,boolean) -> setBlockData -> static setBlockState. When old state has a block entity and block type changes, bytecode25-30 explicitly calls Level.removeBlockEntity BEFORE replacement. False physics then calls LevelAccessor.setBlock(newState,530), bytecode66-72, followed by sendBlockUpdated(...,3).
- Level.removeBlockEntity -> LevelChunk.removeBlockEntity. That method removes live and pending block-entity map entries, listener/debug registration, calls BlockEntity.setRemoved (sets remove=true only), and removes ticker. It does NOT call preRemoveSideEffects or dropContents.
- Ordinary LevelChunk block replacement would fetch old BE and call its preRemoveSideEffects before removing it. BlockEntity.preRemoveSideEffects checks Container and calls Containers.dropContents. The Bukkit pre-removal leaves no BE to fetch, so this spill callback is bypassed.
- Thus this exact source operation removes a normal chest/container and its inventory without dropping contents. The earlier phrase no explicit drop is now strengthened to a traced no-spill result for normal container BEs. No block entity/inventory rides the BlockDisplay.
- Old constants: UPDATE_CLIENTS=2, KNOWN_SHAPE=16, SUPPRESS_DROPS=32, SKIP_BLOCK_ENTITY_SIDEEFFECTS=256, SKIP_ON_PLACE=512; false-physics530=512+16+2. Raw native setBlock flags2 retains BE removal side effects and can spill, so it is NOT source-equivalent. Strict818=512+256+32+16+2 suppresses BE spill and additional side effects; alternatively explicit removeBlockEntity before the appropriate no-physics state change mirrors Bukkit sequence more literally. Assess surrounding update semantics when selecting port implementation; fixture expectation of no container items is source-supported.
