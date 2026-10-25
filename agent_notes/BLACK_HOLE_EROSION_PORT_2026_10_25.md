# Black-hole acceleration and noisy erosion ?25 October2026

Status: QUALIFIED READY on build54941d0e. This supersedes the
complete smooth terrain clearing in the preceding hole implementations.

## User request

> ?????? ???? ????. ?????? (? ????????? ?????? ?? ?????????????) - ???????? ????? ???? - ??????? ??????? ????????, ???? ?????????.

The user also requests less delayed, noisy clearing with dithered remnants,
similar to legacy low-integrity structure placement. Applies to Void Arcanum
levels3?5, Sheep hole and Academy Duplicating BlackHole. Levels1/2 may retain
complete clearing. This concerns body travel, not the entity attraction strength.

## Motion

Void Arcanum per-level acceleration is now.025/.035/.018/.022/.025 blocks/tick?,
with speed caps.4/.55/.6/.7/.8 blocks/tick. Levels1/2 are unchanged. Level5
acceleration falls from.12 to.025 and its maximum speed halves from1.6 to.8.
Nearest-target steering, momentum, contact slowdown, disc lean, TimeStop,
merging, mana and attribution policies retain their existing behavior.

## Terrain

All three families use BlackHoleErosion. Cached radial bands are shuffled
internally, with a cursor relative to the live center. Exact block-center
sphere masks preserve the authored radii and lens boundaries. Movement does
not restart the outer pass or queue historical world coordinates. A bounded
fresh-core supplement responds when the body moves to another cell; at least
three quarters of each allowance advances the main outer pass. Radius24 has
at most64,624 pending candidate cells including the small core supplement.
Absent chunks are inspected only via getChunkNow, never loaded or forced.

The tiny core min(1.5,radius*.3) clears unconditionally. Other cells use stable
per-body/per-pass spatial noise, with deletion probability
.04 + .76*(1-distanceSquared/radiusSquared)^2. Every completed pass may refresh
on a later active clock, so erosion advances while the remaining outer blocks
form a dithered, irregular surface. Work split across budgets does not change
the random selection. Moving fresh-core noise can advance with the active clock.
This reproduces the intended noisy effect rather than literally reintroducing
structures or their obsidian fill. Destruction radii remain2/3/5/9/24 for the
arcanum, sqrt552 for Sheep, and7 for Duplicating; shader resources are unchanged.

Arcanum physics and target searches now finish before its terrain deadline
starts. Arcanum and Sheep each share12,288 inspections/6ms across active holes,
rotating first-hole priority. Duplicating retains the shared Stonekivi scheduler,
using1024 inspections/0.75ms per ready worker slice. Native changes stream as
they are made; no final bulk commit waits for an entire sphere to finish.

Existing rules remain: Arcanum1/2 use native drops, larger holes clear without
loot; selected containers are emptied before removal. Antimagic, original
support-removal attribution and WILD policies are retained. Normal BlockAreaWork
and other spells' scheduling are not modified. Work is released on the existing
owner/unload/epoch/stop boundaries.

## Evidence and limits

See [native checks](BLACK_HOLE_EROSION_CHECKS_2026_10_25.md). Checks cover actual
radius/noise/drop integration for all three families; exact fractional masks,
noise partition invariance, radial falloff, live moving geometry, bounded work,
absent chunks, motion/tilt and cleanup. A dense57,777-block level5 fixture records
actual first-core clearing and native-write slice metrics; it drains one pass
without advancing the server clock. Filling/restoring the fixture is excluded
from its measured terrain slices. These metrics do not claim natural TPS on a
busy main server or client visual quality. CPU deadlines are cooperative, so an
individual write, GC or OS scheduling pause can overrun the nominal slice.

Ready-only user preference remains; main is not replaced or restarted. User
source edits, deleted applied templates and the read-only author RP are preserved.

## Final qualification

Build54941d0e passed28 recorded results in two actual mode1 matches,63.41s
including startup, map loading and regressions. Exact352 Java sources,
977 classes and2818 resources match the tested artifact.
Ready JAR: `build/verification/skywars2-hole-erosion-54941d0e.jar`.
SHA256: `54941d0edce6f8126ba7070aac94291103cf7849a646532ee56764cc9f958a47`.
Evidence: `build/verification/hole-erosion-final/qualification.json` and
archived source-matched build/runtime logs. Main stayed04396ca4, not installed.
Protected user files and applied-template deletions remained unchanged.

Dense sphere results in those two runs:57,777 filled cells;12,313/12,534 removed
in one noisy pass, leaving outer remnants. First6ms slices removed3582/4035
actual blocks including the entire tiny core. The full pass used7/6 slices and
22.8061/21.7951ms of measured wall time inside service slices. These are synchronous
native callbacks, not loaded-server natural TPS or network/client measurements.

Joint regression checks also repaired two old headless visual-fixture errors:
the simulated viewer now acknowledges its dimension round trip with native
hasChangedDimension(), preventing false subsequent damage immunity. The moved
shader test handle keeps its horizontal nearest16 rank, so its quarantine test
does not depend on whether that same tick triggers the10-tick nearest refresh.
Production damage and shader behavior were not altered for these fixture fixes.
Duplicating's retained stop cast is staged before Void intentionally freezes the
owner, then one real match stop verifies all three families clean up together.

After qualification, the obsolete Sheep ready archive was hash-verified and
removed; previous Chef and main-version fallbacks remain. Cumulative cleanup:
288 obsolete JARs,10,154,755,516 bytes (9.46GiB), with worlds/caches/main preserved.
