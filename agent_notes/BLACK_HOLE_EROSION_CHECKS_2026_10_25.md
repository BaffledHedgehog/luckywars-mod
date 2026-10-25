# Black-hole erosion and gentler Void motion checks (25 October 2026)

Initially prepared source-only by the subtask. Parent qualification completed:
[qualified contract](BLACK_HOLE_EROSION_PORT_2026_10_25.md), build54941d0e,28 recorded
results in two actual matches.
No server operation, build, commit or resource-pack change was performed by this subtask.

`VoidArcanumChecks` retains holding/F-swap, merge/upkeep/WILD, attribution,
Time Stop, antimagic, visual quaternion, nearest-target and cleanup coverage.
Its terrain fixture now plants many cells in each high-tier sphere and requires
both actual air and retained cells, including the outer annulus, after one pass.
Every tier still checks the next off-axis cell and excluded cube corner, the
shader/destruction radius equality, and small-tier drops versus high-tier no drops.
These statistical probes replace the invalid requirement that one particular
noisy boundary cell must always disappear. Tiers1/2 retain full deletion checks.

Motion probes now measure actual per-call native displacement and velocity
changes at all five levels. Expected accelerations are .025/.035/.018/.022/.025;
expected speed caps are .4/.55/.6/.7/.8. Cap-boundary initial velocity is set
explicitly; this is not an elapsed native travel-time claim. Existing level5
nearest replacement, reduced steering bound, tilt/display metadata and removal
checks remain.

`BlackHoleErosionChecks` is called by the Void fixture and probes the shared
helper directly without changing `GameChecks`: exact fractional-center sphere
coverage, immediate center attention, zero allowance/deadline behavior, stable
noise across budget partitions, different per-body seeds, guaranteed tiny core,
inner/outer probability falloff, and renewed passes at a later active clock.
Continuous movement must advance the outer cursor with a bounded queue, and
all callbacks must use the latest sphere; its final core is then drained.
An absent-column probe uses only `getChunkNow`, verifies no writes or loading,
and preserves the forced-chunk set. Direct helper callbacks collect candidate
positions; actual writes/drop behavior are separately exercised through Void.

The owning fixture snapshots every resident cell in its bounded test region,
including block entities, and saves/restores pending block/fluid ticks. Absent
columns are never requested. These checks do not qualify client pixels,
peak-load timings, disk restart, or arbitrary moving-map terrain.

The final Void fixture also plants a completely filled resident radius24 sphere
(over57,000 actual dirt blocks), calls the real service with an allowance of12,288
and a6ms deadline, and requires actual tiny-core removal on the first slice.
Further bounded calls drain that same pass without advancing the active clock.
The completed sphere must retain mixed outer erosion/remains. A METRIC log records
filled/removed cells, outer-band removal, call and inspection counts, first-slice
removal, and first/sum/max wall milliseconds. There is no hard wall-time assertion;
these synchronous actual callbacks do not claim naturally sustained server TPS.
The existing full terrain and pending-tick restoration owns this probe as well.
