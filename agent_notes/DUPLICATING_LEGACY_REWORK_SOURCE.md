# Duplicating legacy rework source audit — 2026-10-21

Read-only audit of the original LBC source; no code/build/server changes. New user direction supplied for this task supersedes AGENTS.md's 20 October random-shape interpretation: restore ordinary legacy operations; Expand is ordinary behavior with larger area plus cross-dimension sources; no additional moving child projectiles. Existing owner attribution, containers, cleanup and repaired Academy trial remain requirements. Legacy defects are evidence, not authority for silently restoring bugs.

## Sources and scheduling

Legacy root: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`.
All filenames below under `function/workingitems/awaken_tuntija_wands/avarice/` unless otherwise stated.

`click2.mcfunction`, `click2_upgraded_default.mcfunction`: main marker starts local forward7 from cast eye context, lifetime200; `tick.mcfunction` emits one temporary marker at main position, randomizes it, moves main forward1, renders reverse_portal10 and bee sound volume.1 pitch0 within30. Expand `click2_upgraded_expand.mcfunction` starts forward14, lifetime100; `tick_2.mcfunction` moves main forward2. These are one moving main center per cast, with a one-shot clone position sampled each active tick.

`function/tick.mcfunction:1` calls `tick_not_timestopped` unless stopped; its line3 selects all slowraycast raycasters. `function/raycastselector.mcfunction` increments Lifetime, dispatches rare, then checks lifetime. `function/raycastselector_rare.mcfunction:2-6` dispatches temporary rnd_clone/rnd_clone_2 or main dublicating/dublicating_2 or hole. New temporary marker is not in the existing outer selector result; normally executes next active pass. Its lifetime3 is fallback; `rand_rot2*` kills it after ONE batch. It never advances or flies. Main 200/100 active passes gives nominal10s/5s at20TPS. Time stop pauses both.

`rand_rot.mcfunction`: yaw integer uniform inclusive −180000..180000 ×.001 degrees; pitch inclusive −90000..90000 ×.001. Position each axis is floor(original coordinate)+independent uniform integer inclusive−4..4. Neither random shape selection nor random action selection occurs.

## Exact ordinary operations

`rand_rot2.mcfunction:2-12`, in execution order; all clone commands use default replace/normal (include air, reject overlap; source snapshots are per command, later commands can read previous writes). `^` transforms endpoints and destination positions into world coordinates, then each endpoint is floored and forms a WORLD AXIS ALIGNED cuboid. It does not rotate block states or rotate every cell.

1. Local endpoints (0,0,0)..(0,0,7), destination(0,0,−8).
2. Local endpoints (0,0,0)..(0,7,0), destination(0,−8,0).
3. Local endpoints (0,0,0)..(7,0,0), destination(−8,0,0).
4. Five clones of world-relative cube [−1..1]³, volume27 each, destinations in local basis FACING MAIN CENTER: (0,0,4),(4,0,4),(−4,0,4),(0,4,4),(0,−4,4), in that order.
5. Fill world-relative [−1..1]³ with air strict, then kill temporary marker.

The first three commands are nominal8-cell lines only for axis aligned rotations; at arbitrary random rotation their two endpoints form larger cuboids (volume is product of absolute floored axis differences+1). Source cube in step4 remains world aligned; destination is the MIN corner, not a cube center. Legacy center selector is `@e[type=marker,tag=dublicating,limit=1]` without owner or sort: arbitrary center, a documented workaround. Associate each batch with its own main center in the port; do not restore cross-owner selection accidentally.

## Exact legacy Expand and new requested difference

`rand_rot2_2.mcfunction`: no ordinary source clearing at end in ORIGINAL Expand. First requests temporary source dimension chunks within X/Z floor(point)±20 using forceload add macros (`rand_from_dim.mcfunction`), then:

- Same-dimension local endpoints(0,0,0)..(15,0,0) ->(−16,0,0).
- Same-dimension local endpoints(8,8,8)..(5,5,5) ->(−5,−5,−5).
- Nether local(8,−8,8)..(5,−5,5) ->(−5,5,−5).
- Nether local(−8,8,−8)..(−5,5,−5) ->(5,−5,5).
- End local(0,0,0)..(0,0,15) ->(0,0,−16).
- End local(0,0,0)..(0,15,0) ->(0,−16,0).
- Nexus local(8,−8,8)..(5,−5,5) ->(5,−5,−5).
- Nexus local(−8,8,−8)..(−5,5,−5) ->(5,5,−5).

Then source forceload remove (`rand_from_dim_remove.mcfunction`) and five global [−7..7]³ cube clones, volume3375 each, local destinations facing arbitrary dublicating_2 center: (0,0,14),(14,0,14),(−14,0,14),(0,14,14),(0,−14,14); kill temporary. Cross-dimension destination was hardcoded overworld; latest user already requires spell dimension instead. Local endpoint cuboids are not nominal4³ at random rotations; use actual transformed corner AABBs. Nether/end/nexus all use SAME coordinates, no Nether coordinate scale conversion. Chunk macro bounds are block XZ±20, not chunk indices.

NEW user request means ordinary operation family should also apply to Expand, at larger scale, plus cross-dimension chunks. Whether the ordinary final source clear should also enlarge to [−7..7]³ is a target interpretation, not an exact old Expand fact. Do not describe old Expand as identical to old ordinary: it omitted ordinary Y/Z same-dimension lines, added the diagonal local cube, omitted final clear. Notify user if these differences require a decision. No extra moving children are justified by either source.

## Black Hole exact decoded geometry

`click2_upgraded_black_hole_charge.mcfunction:1` places `structure/obsidian_sphere_air_r7.nbt` at marker−(7,7,7), integrity0.65. Decoded with nbtlib, read-only:

- Size15×15×15; logical lattice center(7,7,7); placement makes lattice center floor(marker position).
- Explicit1791 cells EXACTLY matching integer squared distance d²<=56 from lattice center. Bounds(0,0,0)..(14,14,14), i.e. world floor(marker)±7.
- Palette0 obsidian389 cells EXACTLY d²<=20, bounds(3,3,3)..(11,11,11), i.e. world floor(marker)±4.
- Palette1 air1402 cells EXACTLY21<=d²<=56. This is an AIR OUTER SHELL, with OBSIDIAN SOLID CORE; it is not an obsidian shell around air.
- Remaining1584 cells of15³ are absent and untouched. Integrity applies randomly per explicit template cell; skipped cells stay existing terrain.

`function/raycastselector_kills.mcfunction:1` fills world-relative[−4..4]³ air strict on expiration,729 cells, then kills marker. Hole moves local forward.3 each active tick after template placement and entity work. Starts forward8, lifetime300. Expiration fill uses the raycastselector caller execution position captured before movement: its nested execute at @s dispatch does not mutate the caller context. Current LbcDuplicating final fill at previous point matches that ordering.

## Current source observed before rework

`src/main/java/me/skywars2/game/LbcDuplicating.java`: ordinary `seed()` delegates `randomPatch`: uniform1..7 sizes; CUBOID/SLAB/LINE/SPHERE; COPY/MOVE/DELETE/ROTATE/MIRROR. This implements prior20 October instruction but differs from latest restoration request and exact legacy. Expand currently uses two local clones, cross sources and five cube clones. Its light Seed is consumed once next tick; current source does NOT move seeds. Main center moves1 or2 each active tick. Current black-hole placement origin floor(point−7) matches decoded legacy structure center; do not shift by another7.
