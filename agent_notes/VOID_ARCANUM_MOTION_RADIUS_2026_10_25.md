# Void Arcanum radius and motion correction (25 October 2026)

Source read-only: legacy `LBC/data/lbc/function/raycast/voidcursed1..5.mcfunction`
and `LBC/data/lbc/structure/air.nbt`, under the original server's
`world/datapacksdisabled`. The first four fill commands use horizontal offsets
±1/±2/±4/±8. Their vertical endpoints are respectively `.05..2.05`,
`-1.05..3.05`, `-4.05..4.05`, `-8.05..8.05`. Fractional negative coordinates
floor downward when resolved to block positions. The fifth level places an
exactly48×48×48 air template at offset(-24,-24,-24), with source integrity.02;
its separate obsidian cube is deliberately excluded by the existing user ruling.

The radial translation uses whole-block reaches2/3/5/9/24. This is an explicit
interpretation of the legacy block footprint, including vertical endpoints,
rather than a claim that legacy cubes had spherical masks. It restores the small
level's missing footprint while retaining the user's radial mask and excluding
cube corners. Hurt/pull radii, damage, merging and upkeep are unchanged.
All levels enqueue their complete current radius immediately. Existing moving
resident section work clips stale cells; inspection still shares8192 cells and
4ms per service frame. No chunk request, historical swept queue or obsidian is
added. Dense terrain remains budgeted and can take multiple frames to clear.

Author RP was read only at the26.3(1) instance from `RESOURCE_PACK.md`:
`blockbench/black_holes.txt`, and the active `black_hole_image.glsl`.
`BH_REACH=14.0` remains authoritative, so scale is destruction-radius/14.
The large model's disc accepts left rotation; right rotation stays identity.
No resource or template changes are needed.

Each body retains velocity and accelerates toward the nearest suitable loaded
living entity. Per-level acceleration is.025/.035/.05/.075/.12 blocks/tick²;
speed caps are.4/.55/.75/1/1.6 blocks/tick. Steering uses the same acceleration
limit and slows its desired speed near the source's one-block target exclusion.
Blocked residency/antimagic destinations and absence of a target clear velocity.
The level5 disc normal leans18degrees toward the horizontal target direction,
with quaternion interpolation.12 each active frame; other levels return toward
identity. Time Stop already skips the entire physics/visual update. Body cleanup
resets velocity/tilt and releases its shared renderer handle.

`VoidArcanumChecks` now probes actual destruction at each radius and rejects the
next off-axis cell/corner, checks lens equality and drop rules, observes increasing
native displacement and bounded nearest-target steering, reads actual virtual
display left/right quaternion metadata, and checks cleanup. Its expanded sky
fixture clears/restores only resident nonair cells, never loads absent chunks.
These are synchronous native boundaries, not a client pixel or dense-load claim.

Status: implementation and native checks prepared; build/runtime qualification
belongs to the parent batch. Author RP, applied/deleted templates and user-owned
source files remain untouched. No build, server operation or git action was run
by this subtask.
