# Chef navigation, lip recovery and airborne placement — 27 October 2026

Status: qualified build **25d7466b**; automatic main-server installation queued.
Live receipt: `build/verification/chef-navigation/main-install.json`.
This supersedes the relevant routing/takeoff details in
[the preceding route patch](CHEF_ROUTE_SELECTION_2026_10_26.md).

## Author requirements (user)

Retain routes/frontiers when the target barely moves and terrain is unchanged.
Mine downward through the ground to reach a victim underneath. Improve movement
through dense mixed partial-block terrain. Eliminate repeated run-up/fall loops,
backpedal without a 180-degree turn, and account for the sprint-jump impulse.
Place real blocks during parkour across wider gaps. A Chef standing on the very
edge/corner of a block must not remain permanently idle.

Earlier strict requirements remain: actual camera/head faces each accepted
interaction within two degrees, real gravity/collision, one main hand with its
equipment dwell, fixed victim/ownership, prone knock and survival actions.

## Changes

`LbcChefBrain` now checks straightforward supported/parkour routes before the
general action search. Per-search floor-clearance and movement-edge caches avoid
duplicate collision queries; unresolved searches retain their frontier between
ticks. Short safe walk shortcuts consume the skipped path prefix. Previously
the actor could pass its current waypoint while the queue continued to demand
that obsolete waypoint, eventually stopping or restarting the route.

Weighted travel estimates favor useful progress. Frontier slices allow192nodes
and6000probes, with a6ms slice deadline; the shared frontier budget is1536nodes,
36000probes and12ms per tick. Fast surface checks have their separate bounded
24-cell/720-probe envelope. These are planning limits, not a guarantee of total
entity CPU or the globally fastest route on arbitrary maps.

Downward mining is an explicit graph action. It validates resident, nonhazardous,
breakable shaft cells and supported landings up to three blocks below, mines
under the actor's actual feet with its real tools/feedback/ownership, and allows
gravity to complete the fall. Empty shaft cells between supports no longer
invalidate a pending descent. Native knock, hunger and water logic are retained.

Takeoff prediction includes the native **+0.2** horizontal sprint-jump impulse,
existing post-friction momentum and normal air acceleration. The old retreat
phase was interrupted by a forward-approach branch, causing an endless loop
which also disabled the progress watchdog. A single short retreat now finishes
before the forward phase and keeps facing the landing; preparation has a24tick
deadline. The fallback point is0.4blocks behind the launch-cell centre. A landing
near a platform's edge is walked onto rather than immediately jumping again
before that jump step retires. Automatic supported running hops aim farther
ahead, avoiding midflight turns towards an already overshot short landing.

Physics reference: native26.3 LivingEntity.setSprinting/applyInput/
getFrictionInfluencedSpeed/travelInAir, plus Chef's existing native jump override.
At ordinary friction0.6, sprint acceleration is0.13*0.98=0.1274 and retained
ground velocity0.546. From rest, three ticks cover0.5593blocks at83.72% terminal
speed; four cover0.8150 at91.11%; five cover1.0820 at95.15%. Full speed is not a
prerequisite for every jump. `getDeltaMovement` stores post-friction velocity,
not the larger displacement before drag.

The airborne-build action equips/aims before takeoff and extends an actual far
bank towards the flight using two visible, face-connected placements. Larger
gaps may first use a short supported approach. Equipment/aim/cooldown waits do
not cancel airborne steering. Full attachment faces are validated in planning;
actual placement still checks reach, visibility, collision, aim and world write.
There is no floating-block placement, artificial trajectory boost or teleport.

Starting support detection includes the mannequin's full0.6-wide footprint and
neighboring columns when its centre is above air. Safe recentering uses ordinary
world-space steering with independent facing. The actor therefore resumes from
thin edge/corner contact without invoking void-rescue teleportation.

## Verification and release

`ChefNavigationChecks` adds nine isolated fixtures: downward mining to an actual
attributed hit; stable and slightly jittered target routes;625cells of dense
snow/slab/oriented-stair/full-block terrain; stopped near-lip takeoff; real
airborne placement over gaps5/7; and edge/corner starts with only0.02blocks of
native footprint contact. Requirements include route reuse, bounded retreat,
unchanged terrain where edits are unnecessary, gravity, action alignment and
actual completed traversal/accepted victim hits. These fixtures and the24prior
Chef groups use explicit native physics steps, not wall-clock scheduling.

`chef-natural` separately observes actual server/world ticks:200tick prone
fall/wake, bounded rotations, shapes, bridge crossing and attributed melee.
Real match-stop cleanup, sheep/Danmaku/player-rule/Time Stop/TPS compensation,
signed-effect/FOV and firearm regressions also pass. Final result: **40 suite
results in two actual mode1 matches,109.11seconds**. Exact504Java sources,
1438classes and3215resources match the tested JAR.

An initial focused run exposed stale waypoint retirement and repeated jump
issues; its log is retained. One additional successful full run had an unrelated
preparation step inherited by the copied runner, so its metadata did not qualify;
the corrected runner then completed the final clean qualification above. No
physical-client pixels, subjective human likeness, arbitrary-map completeness,
network latency or peakTPS claim is made.

Reproduce: `python tools/smoke_chef_navigation.py --server-dir build/verification/key-reuse-server`.
Evidence: `build/verification/chef-navigation/`.
Ready: `build/verification/skywars2-chef-navigation-25d7466b.jar`.
SHA256: `25d7466b1be20b3c7384efa34c65f115f83917392d68a0fd9fa809fe7aa522f3`.

All175 protected author paths and31live grimoire overrides remain exact; no RP
change is needed. Main remains735d9905 while running. The safe installer waits
for port24566 and the world lock to release normally, then installs and keeps
735d9905 as rollback. Its follow-up removes obsolete16502e73 only after verified
installation/hash/size/path checks. No forced shutdown/restart is authorized.
