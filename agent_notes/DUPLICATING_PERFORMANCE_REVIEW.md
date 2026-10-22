# Duplicating CPU and latency review — 21 October 2026

Scope: static read-only audit of current LbcDuplicating, shared LbcStonekivi queue,
MapManager snapshots/restoration and LbcIndirectKills. No Java changes, build,
server execution, resource pack or source-server changes. Numbers below are
algorithmic counts, not measured MSPT. Read root/agent_notes AGENTS and relevant
ARCHITECTURE contract before research. Current legacy order, live sequential
captures, container contents and ownership are requirements; world access and
mutation remain on the server thread.

## Highest priority findings

1. **Meditation is an immediate producer burst outside the work budget.**
   LbcDuplicating.java:121–128 creates one 200-tick center plus 90 30-tick
   centers. tick():512–536 calls applyShuffle once per active center per tick.
   seed():314–333 allocates one Job, three axis Clone operations, five cube
   Clone operations and one fill for every call. A full surviving trial therefore
   creates 2,900 batches and 26,100 operations (including rejected clones), with
   91 batches/819 operations generated on each of the initial 30 active ticks.
   This planning, allocation, suppression checking, particles and sounds is
   outside Stonekivi's 3 ms work budget. See also random point/rotation work
   at 520–523. Reduced terrain work alone cannot bound this producer cost.

2. **Expand's work arrival rate vastly exceeds normal.**
   Five independent 15³ cube copies plus the 15³ fill require 37,125 cell
   iterations per batch (33,750 capture/write + 3,375 fill), before rotated-axis
   AABB and cross-dimension copies. Normal's equivalent is 297 cells. At 100
   active ticks that is 3,712,500 cube/fill iterations per Expand center. Clones
   rejected for overlap or height do no work, so this is a nominal full accepted
   batch count, not a guaranteed observed count. The three axis endpoints are
   transformed into world AABBs; their volumes can exceed the apparent line
   length. See seed():318–332, Clone constructor:261–275, expandSources:340–348.

3. **Bounded consumption does not bound pending work or its latency.**
   LbcStonekivi.java:163–179 services at most 128 turns/3 ms per tick;
   Job.step():150–160 uses up to 512 cells/0.5 ms per turn. No admission/backlog
   policy exists at queue():139 or seed():333. Marker expiration removes its
   marker, not its independently owned Jobs (tick():535–538; Job only validates
   owner/epoch). Work at old positions survives and competes with Stonekivi,
   Magmify and other shared clients. This explains a plausible mechanism for
   normal/Expand being far behind; confirming CPU share requires profiling.
   Adding threads cannot remove this server-world workload.

4. **No-op destination cells still pay attribution collision/entity queries.**
   Clone.step():291 calls supportRemoved before MapManager.restoreSnapshot's
   identical non-BE-state guard (1307). supportRemoved (LbcIndirectKills:170–179)
   reads collision shapes and queries nearby grounded LivingEntity for every
   solid destination cell, even when restoreSnapshot immediately returns.
   Repeating this across a mostly unchanged dense volume can dominate useful
   mutations. fill():363 similarly invokes supportRemoved even for air, then
   setBlock; air exits cheaply but still performs calls/lookups.

5. **Container copies are intentionally expensive and must stay complete.**
   MapManager.snapshot:1278–1281 allocates a BlockSnapshot per cell and serializes
   full BE metadata. restoreSnapshot:1308–1315 clears existing inventories and
   reloads full components even for the same block type. Removing those steps
   or comparing only BlockState loses content semantics. Sequential source
   snapshots are intentional: earlier operations can change later sources.
   SharedCapture is currently only used by grid (371–379), where the contract
   explicitly permits one capture for 26 copies; do not apply it blindly to
   shuffle's five clones.

6. **The queue's deadlines are cooperative, not strict tick limits.**
   trapChanges finalization (LbcIndirectKills:217–223), finishSupportRemoval
   (184–189), a single native setBlock/BE load and Clone's native scheduled-tick
   copy (298–300) run outside per-cell deadline checks. A Job step can overrun;
   the outer queue deadline cannot preempt it. Native scheduled-tick copy is
   once per clone, already better than per row. Its exact queue-wide cost is
   not established here. Do not reintroduce per-row tick copies.

## Minimal optimization candidates, in order

- Add lightweight counters/timers around producer planning, capture, destination
  no-op, supportRemoved, restore/BE load, scheduled tick copy and trap finalizer.
  Record jobs/cells queued, oldest job age, completion latency and peak depth.
  Profile separate single normal, Expand, meditation and simultaneous shared
  clients. Measure MSPT/percentiles and allocation rate, with identical fixtures.
- Move the existing exact no-BE/no-change decision ahead of supportRemoved in a
  Duplicating-specific restore helper, then perform support attribution only
  for actual replacement. Preserve the current no-op behavior: unchanged cells
  must not acquire a new trap/hazard author. Never skip same-state BE writes.
  Read the destination once and pass its state/BE through the helper if useful.
- In fill, skip already-air cells before supportRemoved/setBlock, preserving
  treatment of cave/void air and ownership expectations explicitly. For solid
  replacements reuse a frame-local list of nearby living targets and evaluate
  their actual feet/collision intersections per removed block, rather than
  repeating spatial entity queries per block. Keep genuine support loss and
  trap/hazard finalization at the existing slice boundary.
- Avoid allocating snapshots for ordinary repeated states (especially air)
  through immutable per-state snapshots with null entity; retain fresh BE NBT
  and never share mutable entity data across restoration without proving load
  does not mutate the source tag. Reduce repeated chunk readiness checks using
  a per-slice chunk view on the server thread.
- Store compact pending batch descriptors/lazy operations rather than eagerly
  allocating all Clone objects. This reduces producer allocation, not cell work.
  Preserve random draw timing/order and per-Job fair scheduling if reproducing
  current behavior is required. Keep sequential capture-before-write and
  current live source timing. Do not drop old batches, coalesce across casts,
  freeze all captures up front, or reuse snapshots without a dependency proof.
- Pass the remaining shared queue deadline into cooperative jobs to avoid the
  current extra half-millisecond slice at the end of the queue window. A tighter
  bound improves latency predictability, not necessarily throughput. Retain
  fairness with other clients and measure the completion-latency tradeoff.

A major sustainable throughput fix may need faster native/bulk mutation, but
changing shape/on-place callback flags has gameplay consequences. MapManager's
normal MAP_WRITE_FLAGS (88) differs from Duplicating's deletion FLAGS (37).
Any clone-specific flags need native validation of light/client updates,
containers, scheduled ticks, falling blocks and ownership before adoption.
Simply raising the 3 ms budget worsens TPS; lowering it increases backlog.
Dropping/coalescing producer batches changes the approved legacy sequence and
requires an explicit behavior decision, not an optimization disguised as one.

## Black-hole radius request

Current static code still has shader radius 4/2.6 at LbcDuplicating:43, terrain
candidate cube ±4 at 47 and distance² <=16 at 440, strong pull R4 at 42 and
living damage distance² <=16 at 472. A requested radius 1.5 must specify which
of visual shadow/absorption/strong pull/damage is being changed; the parent task
has the user's current context. Changing only shader radius does not reduce
terrain scan work. Absorption radius 1.5 needs a matching compact offset list
and distance² 2.25. Keep R16 influence unless explicitly changed. This report
does not claim the new radius is implemented.

## Validation boundary

No timing claim or speedup is proven by this static audit. Existing gameplay
qualification is documented in DUPLICATING_REWORK_PORT.md; it explicitly did
not measure peak MSPT. For a fix, rerun native sequential clone/container,
scheduled-tick, support-loss/trap attribution, owner death/epoch cancellation,
time-stop, cross-dimension readiness, and shared-queue fairness scenarios.

## Follow-up: destruction R7 and lens/core calibration (read-only RP audit)

Latest parent-provided user requirement: destruction sphere R7; outer lens
boundary must coincide with R7; earlier shadow reduction 4/1.5 = 8/3 remains
unless incompatible. This supersedes the earlier radius-1.5 ambiguity above.
No RP/template or Java edits were made.

Primary RP root read:
`C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3/minecraft/resourcepacks/all_in_one_1.21.8`.

- `blockbench/black_holes.txt:13–15`: scale R is event-horizon radius;
  distant visible shadow is approximately 2.6R. This is approximate, not an
  exact screen-space/world-distance equality at every camera distance.
- Active chain `assets/minecraft/post_effect/end_of_frame.json:325–347`
  uses black_hole_image_fast/image. Shared active include
  `assets/lbc/shaders/include/black_hole_image.glsl:21`: BH_REACH=14.0.
- `assets/lbc/shaders/include/black_hole_gravity.glsl:43–56`: analytic
  entry intersects sphere radius R*BH_REACH; :76 stops field at that reach;
  :81 tapers field from 10R to14R. Image include :76–77 also tests R*BH_REACH
  in lens-depth occlusion logic. Thus mathematical support boundary is 14R,
  although visible distortion approaches zero smoothly near the edge.
- `assets/lbc/shaders/include/black_hole_vertex.glsl:73–91` decodes radius
  solely from transformed X-axis length. Y's magnitude is discarded when
  deriving disk normal (:97); Z/X supplies visibility in
  `assets/lbc/shaders/post/effect_state.fsh:169–177`. Current shader provides
  no independently adjustable lens radius through existing server channels.

With the unchanged RP: R=7/14=0.5 yields lens support R7 but distant shadow
about1.3. R=(8/3)/2.6=40/39≈1.025641 yields desired shadow≈2.666667 but
lens support≈14.358974. Both requirements cannot hold using existing R alone.

Minimal prospective template shader option (NOT implemented): choose R=40/39,
BH_REACH=7/(40/39)=6.825, and change taper start10.0 to a value below6.825,
for example proportional4.875. Changing BH_REACH alone makes smoothstep's
edges10.0,6.825 reversed and is invalid/undefined GLSL behavior. Active shared
image include covers fast/many paths; gravity include is also shared with old
inactive renderers, so changing taper globally needs care. Reach/taper changes
alter the null-ray trajectory, so the 2.6 multiplier must be checked by rendered
calibration; cannot promise exact shadow2.666 from a static scalar calculation.
Global reach changes also affect other black-hole users and disk rendering.

A more flexible template change could transport Y-axis length as independent
lens support for black-hole kinds (it is already present in metadata but unused
as magnitude), then carry an extra per-hole varying and use it consistently for
entry, field cutoff/taper and lens-depth test. Preserve normalized Y direction
for disk orientation, X for horizon radius and Z/X for visibility. This requires
shader/template implementation and validation, not merely different Java scale.
Animation currently scales the physical radius and lens together with appearance
(vertex:79–85); a new independent reach must apply the same phase if desired.
