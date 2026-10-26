# Moving antigravity support and30-second storm - 27 October2026

User: rising terrain must carry a standing player, preserve horizontal input,
retain support until the entire footprint leaves the piece, and last30 seconds.
The earlier15-second choice is superseded by this request. Existing terrain
selection, alignment, no hovering pickup and global altitude hazards remain.

## Cause and implementation

The previous service captured passengers independently for every original
block, then applied only velocity. Crossing to another block could permanently
detach a rider even while standing on the same rising cube. That implementation
was qualified with native mob travel, not actual player movement input.

One `Platform` now owns each captured cube, its cells, common offset/speed and
passengers. Surface admission still requires actual feet contact at launch.
Continuing support uses the union of live cell collision-shape footprints.
Moving between cells or placing the center outside the outer edge retains
support while any part of the feet still overlaps; full departure releases it.
Jump/upward escape, technical roles, active Greed, frozen actors and passengers
retain their guards. Existing mob lift uses native velocity/gravity/drag.

Supported players move only vertically through native collision to the next
surface height, with grounded state and cleared fall debt. X/Z position and
momentum, yaw and pitch are preserved. A native `ClientboundTeleportEntityPacket`
to the player uses absolute Y/zero vertical momentum, relative zero X/Z,
horizontal momentum and rotation. Thus newer client input is preserved too.
The client handler applies this entity transport without the PlayerPosition
teleport ACK lock or its block-breaking reset. No mount or block replacement
is introduced. Source displacement attribution is retained.

`AntigravitySupportMixin` adds live platform shapes to native `Entity.collide`
for admitted supported riders. Other native terrain/entities remain in the
query. `AntigravityPlayerSupportMixin` runs after native main-thread dispatch:
while requested X/Z feet overlap the union, stale lower client Y is clamped to
the actual moving floor. Requested horizontal position and look remain intact;
full departure and higher/jump positions retain ordinary movement input. The
native noBlocksAround check recognizes this real server support, retaining
normal flight checks for unsupported actors. These hooks do not generate chunks.

Existing displays still move upward by the source .04 acceleration loop while
speed<3. Every10 active ticks the storm tries another terrain cube, through
tick600; tick599 remains active, tick600 ends the producer. Existing accompanied
pieces retain their source lifetime and may outlive the producer. Dimension
Time Stop pauses movement and ages. Removal/unload,20 consecutive stale ticks,
match stop and epoch cleanup discard cells and release support. Whole-cube
motion avoids per-cell duplicate rider impulses and keeps block alignment.

## Verification and limits

`AntigravitySupportChecks` exercises real native PosRot input, server collisions,
cross-cell walking/strafe, center outside the edge with feet still overlapping,
complete departure, jump, Time Stop and Greed. It wire-encodes/decodes the native
self transport and runs vanilla `PositionMoveRotation.calculateAbsolute` against
three arbitrary newer client positions/momenta/angles. Only Y/vertical momentum
change; no PlayerPosition correction is emitted. Native mob travel, ceiling
collision, no-hover admission,600tick duration,620tick accompanied motion,
650 frozen service steps, debit, terrain and lifecycle checks remain in
`AntigravityDropChecks`.

`tools/smoke_antigravity_support.py` passed22 recorded results in two actual
mode1 matches (53.27s), including shared player/projectile/Time Stop behavior,
previous visual-owner death recovery, Banhammer/actual Carpet removal and
special tools, plus retained storm/display cleanup across actual match stop.
Native packet/server fixtures do not observe actual Minecraft frames, display
interpolation under real latency or audible playback; request the author's
eventual observation through [pending questions](PENDING_QUESTIONS.md).

Build9d2591dc (`9d2591dc4b6d7448076776d434447430079215cd779e8ead25a30808d3d11433`)
contains exact444 sources /1225 classes /3140 resources matching the release.
The native-tested binary remains byte-identical after restoring the original LF
source formatting. Source archive was rebuilt and compared separately. The
isolated JVM stopped and restored its original settings/profiles/scoreboard/JAR
before qualification. All175 protected author paths and31 live grimoire files
retain their hashes/deletions. Existing RP keys contain no duration to replace;
the author's RP and template remain untouched.

Ready: `build/verification/skywars2-antigravity-support-9d2591dc.jar`.
Immutable qualification, logs/results, live installation and cleanup receipts:
`build/verification/antigravity-support/`. Installer11424 adopted this release
instead of pendingcc460bf8 and waits for normal main shutdown/world unlock
(24566, RCON off). Main disk remains1a8a3825; no forced stop/restart.
Keep9d2591dc,cc460bf8 and installed1a8a3825; obsolete1b68c82a retired after hash
verification. Cumulative cleanup328 JARs /11707221536 bytes.
