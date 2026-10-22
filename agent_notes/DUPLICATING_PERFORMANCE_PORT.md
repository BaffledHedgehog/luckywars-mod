# Duplicating CPU and latency correction - QUALIFIED, ready without installation

Latest user choices supersede [the earlier rework](DUPLICATING_REWORK_PORT.md).
Source/performance review: [DUPLICATING_PERFORMANCE_REVIEW](DUPLICATING_PERFORMANCE_REVIEW.md).

## Current implementation

Every accepted cast retains its original centers, including all 91 meditation
centers. Each moving center owns one shared-queue worker, with a running series
and one replaceable latest Seed. Further tick requests refresh that Seed instead
of appending old-position jobs. When its marker expires, the running and latest
series finish, then the worker exits. Thus sampled old positions are coalesced;
this deliberately replaces the previous unbounded per-tick backlog. It does not
promise execution of every intermediate random request under overload.

Each offered Seed stores its own immutable center for fan orientation. Ordered
axis and cross-dimension operations remain separate. The five fan clones reuse
one source capture: their non-overlap rule prevents them changing that source.
This gives the fan one coherent view, as during one synchronous legacy function,
rather than observing unrelated world edits between budget slices. Native block
entity data and container contents remain fully copied. Immutable non-block-entity
states reuse snapshots; identical non-BE writes are rejected before ownership and
support spatial queries. Clearing existing air is skipped.

All block reads/writes remain on the server thread through LbcStonekivi's existing
3ms cooperative queue. No new world thread or chunk engine is introduced. The
per-job cell cap is 4096 with the existing 0.5ms cooperative deadline. Job removal
uses a set. FULL chunk tickets alone are insufficient: actual getChunkNow residency
is required before block access, preventing a supposedly asynchronous source read
from blocking on generation. Cross-dimension sources prefetch their needed chunks
through MapManager.prepareChunk. A live series skips a local chunk that ceased to
be resident rather than waiting forever behind an old position; explicit finite
helper jobs retain their prior waiting semantics.

## Black hole - final user calibration

User chose no RP patch: existing shader radius R=0.5 means visible shadow about
1.3 blocks and outer lens support exactly 14R=7 blocks. Terrain destruction is
spherical R7 and uses the same center. Strong pull/core damage uses R1.3; weak
influence remains R16, with player input protection, ownership and at most two
sampled holes unchanged. The source shader and resource-pack template are untouched.

## Final evidence

Baseline source and artifact are saved under build/verification/duplicating-performance/.
The same isolated performance harness runs normal, Expand and all 91 meditation
centers for 40 service/terrain tick pairs near a bounded dense fixture. It records
wall/thread CPU timing, queue size and actual changed cells, then advances marker
expiry and cancels the baseline backlog instead of draining thousands of jobs.

A cold optimized run reduced meditation peak pending workers from2622 to 91,
with only one final worker after expiry instead of 2748. Expand's measured terrain
wall total was 46.99ms vs 871.43ms over 40 calls, and maximum 3.86ms vs453.56ms.
These are illustrative single-run measurements, not a full-server TPS guarantee:
JIT, GC, scheduling and random geometry affect timing. Normal cold setup did not
show an improvement. Final artifact 02952e5f passed two native matches in 57.82 seconds,
including normal/Expand/meditation performance, bounded pending work after expiry,
source presentation, exact 2x2/3x3 crafting, Divider/Divide, cross-dimension source
and container copying, Academy promotion, hole physics/terrain/render metadata,
Consuming interaction and actual match-stop cleanup. The same artifact is archived
as build/verification/skywars2-duplicating-fix-02952e5f.jar; evidence including the
baseline/cold/final timing reports is in build/verification/duplicating-performance-final/.

Final-match timing varies: Expand terrain totals over 40 calls were 125.39ms and
32.49ms, with maxima 3.46ms and 3.24ms; baseline was 871.43ms / max 453.56ms.
Meditation peak pending remained 91 instead of 2622, and only one final worker
remained after expiry instead of 2748. Both final matches retain all 91 centers and
make actual terrain progress. The workload is synthetic and timings are not
statistical production-MSPT guarantees. Windows thread CPU counters are coarse.

Build/ZIP CRC passed; 2638 packaged source resources byte-match, with fabric.mod.json
version/SGUI expansion checked separately; 1977 resources remain excluded. Isolated
server 25576 stopped normally and released its native world lock. Main JAR remains
332415d6; this batch did not install or stop it. RP/template were not modified.
Shader R=.5 / shadow~1.3 / lens 7 was verified against existing source and native
Polymer display metadata; a graphical client was not used. Existing startup
loot-context warnings and fake-player Polymer diagnostics remain in the test log.
