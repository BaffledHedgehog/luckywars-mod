# Void Arcanum legacy travel ceilings — 25 October 2026

Status: QUALIFIED AND INSTALLED on the main server, without restart.

## Author correction

> арканум вечной тьмы - дохуя быстрые черные дыры стали.. Сделай чтоб по скорости они не могли быть быстрее легаси дыр

This supersedes the travel caps in
[the earlier motion correction](VOID_ARCANUM_MOTION_RADIUS_2026_10_25.md)
and [the erosion batch](BLACK_HOLE_EROSION_PORT_2026_10_25.md).

## Source and change

The original server remains read-only. In
`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/raycast/`,
`voidcursed1.mcfunction` through `voidcursed4.mcfunction` each chase with
`tp @s ^ ^ ^0.13`; `voidcursed5.mcfunction` uses `tp @s ^ ^ ^0.5`.
`voidcurse.mcfunction` dispatches these by hole count, via the owned raycast
selector. These are body travel distances, independent of entity attraction.
The spawned marker has `slowraycast`; `tick_not_timestopped.mcfunction` dispatches
that selector every active tick, confirming the movement cadence.

Native maximum total 3D travel per active tick is now0.13 at levels1–4 and0.5
at level5 (2.6 and10 blocks/second at20 TPS). The previous caps were
0.4/0.55/0.6/0.7/0.8. Existing acceleration, contact slowdown and bounded
steering remain. A final vector-length clamp also handles inherited excessive
momentum and membership loss that lowers a body's tier. Limiting individual
axes would incorrectly permit faster diagonal movement, so the full vector
is constrained before changing the authoritative point and marker position.

Destruction/noisy erosion, lens radius, attraction, damage, ownership/WILD,
merging, upkeep, disc tilt, time-stop and lifecycle are unchanged.

## Verification

The existing native `VoidArcanumChecks.motion` now checks both exact acceleration
until saturation and legacy caps at all five levels, including saturation at
the lower level2 ceiling. Each level also exercises three deliberately excessive
incoming momentum vectors and48 alternating diagonal/elevated chase steps.
The preceding positive-X visual/nearest-replacement fixture is restored after
these independent stress probes, preserving its original geometric assertions.
Existing behavior/terrain, noisy erosion, shader respawn and actual-stop cleanup
are included in the final two-match driver under `build/verification/void-speed-final/`.
These are native service/packet/lifecycle checks, not client-frame or peak-TPS
measurements. The qualified JAR must come from the complete Gradle build.

Full build22b646b6 passed ten results in two actual mode1 matches (69.12 seconds
including boot/map preparation), with exact371 sources/1044 classes/2837 resources.
The motion limits and excessive/turning vectors pass all five levels. Full Void
Arcanum checks include native F-swap/merging/upkeep/WILD/ownership, actual radial
and dense noisy erosion, tilt metadata, suppression and cleanup. Actual native
death/respawn/dimension replay and real match-stop cleanup also pass.

Evidence: `build/verification/void-speed-final/qualification.json`, immutable
`qualification-22b646b6.json`, results/log and `main-install.json`.
Ready artifact: `build/verification/skywars2-void-speed-22b646b6.jar`.
Main baseline16e097ca was unchanged through qualification. Installation uses
the existing normal-shutdown/world-lock guard with rollback and no server restart.
Author RP, user files/deletions and all resources remain unchanged.

The installer subsequently observed normal main shutdown and installed22b646b6,
with16e097ca retained as rollback. Installed hash and receipt were reverified.
