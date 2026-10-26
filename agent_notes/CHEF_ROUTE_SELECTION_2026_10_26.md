# Chef route selection and compact takeoff - 26 October 2026

Status: QUALIFIED; automatic installation queued. Supersedes the route-selection,
run-up and pillar-facing details in [the previous movement patch](CHEF_ACTION_MOVEMENT_2026_10_25.md).

## Author requirements (user)

Retain accumulated speed before parkour, with at most a short 0.5-1-block
acceleration approach. Climb diagonal full-block stairs and noisy partial-block
terrain without repeated stationary jumps. Compare direct construction against
long surface detours, and start building promptly when the victim is airborne.
While pillaring upward, keep looking down instead of turning toward the victim
between floors. The previous strict action-facing requirement remains in force.

## Implementation

`LbcChefBrain` uses one action frontier from the first node. Walking, dropping,
parkour, bridging, ramps, pillars and mining compete using approximate travel-time
scores. There is no preliminary terrain-only search that must exhaust before
construction becomes eligible. Existing resident-chunk, world-border, voxel-shape,
hazard, reach, placement-attachment and CPU/node limits remain. A height estimate
allows for height gained while travelling along stairs rather than encouraging
an unnecessary artificial tower beside them.

For an elevated victim within twelve horizontal blocks, a completely supported,
clear approach followed by a clear column can be validated directly. Every
approach segment, future standing box and replaceable support is checked before
committing the route; walls, stairs, roofs and gaps return to the general action
search. This avoids scanning the flat arena for an airborne target. It does not
place blocks during planning or load chunks. All route execution still uses
ordinary native travel, gravity and collision.

Walk steering aims through the next takeoff rather than slowing at its cell
centre. Long jumps preserve existing forward velocity; a stopped actor first
accelerates forward if the supported approach permits it. The fallback runway
is 0.75 blocks along the actual direction, including diagonals. Short jumps need
no runway. Full-block rises on small landings use an ordinary jump instead of
the sprint impulse that could overshoot a diagonal step. Noisy walk/drop edges
can sprint even when the remaining route contains only one or two nodes.

Pillar execution holds the downward action aim during ascent, placement and
landing. Top/bottom attachment points follow the actor's horizontal position
within the real face, so a nearly vertical action does not repeatedly change
azimuth. An exactly vertical look preserves yaw. Every accepted action still
requires the actual camera/head alignment within two degrees. There is no
forced position update or removal of gravity.

## Verification

`ChefChecks` adds six native fixtures to the preceding seventeen groups:
five diagonal full-block rises without edits; deterministic noisy full blocks,
slabs and stairs; a short bridge versus a long available surface detour; five
pillar levels with stable yaw/downward pitch; a four-block gap approached with
existing sprint velocity and less than 0.8 blocks of retreat; and an offset
airborne victim reached through prompt approach/construction.

The earlier mining, ownership, real knock/falling/prone pose, water, hunger,
combat, hand dwell, sounds, strict action-ray and lifecycle fixtures remain.
`chef-natural` separately observes real server/world ticks and actual match-stop
cleanup. These tests measure native states and movement, not rendered client
frames or subjective human likeness. Routing scores approximate travel time;
they do not guarantee the globally fastest route on arbitrary maps.

Full Gradle build `385f9f1140b7e9657d2cb98685db85003f23c94932f54eac57071d5653623fb5`
passed 22 recorded suite results in two fresh mode1 matches (98.01 seconds,
including startup and map preparation). Each match passed all 23 Chef groups,
the naturally ticking chase, sheep/Danmaku/player-rule/Time-Stop regressions and
actual-stop probe/cleanup. Exact 401 Java sources, 1130 classes and 3138 resources
match the tested release. Only the previously known fake-player packet-context
diagnostic pair appeared; no suite failed. Protected author files/deletions,
live grimoire overrides and the running main JAR stayed unchanged.

Evidence: `build/verification/chef-routing-final/qualification-385f9f11.json`,
the copied native log/results and final build log. Ready artifact:
`build/verification/skywars2-chef-routing-385f9f11.jar`. Live installation status
is in `build/verification/chef-routing-final/main-install.json`; the safe installer
waits for normal main-server shutdown/world unlock and does not restart it.
The build includes the previously qualified native shields/tridents and totems.
