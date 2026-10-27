# Bulk Carpet player command ? 27 October 2026

`/player spawn harus 15` requests the normal Carpet spawn for `harus1` through
`harus15`. Operator permission is required on the new branch. Existing online
names are skipped; repeated batches can fill only missing names. All generated
names are validated before the first spawn: ASCII letters/digits/underscore,
maximum16characters including the numeric suffix. Count is1..1000.

The optional Carpet branch is registered only when Carpet is installed. It
reuses the native dispatcher and original source for each `/player <name> spawn`:
Carpet handles fake-player creation, its admission checks, profile lookup,
dimension/position/look/game mode. No alternative fake-player engine, join bypass
or mandatory Carpet compile/runtime dependency was introduced. Carpet may finish
joining on a scheduled task; feedback counts accepted requests, not completed
joins. Native profile lookup can take time for uncached names, as for individual
Carpet commands. Ordinary named player commands remain available.

Build4fa80764 passed14batch checks and10native fixture results across two actual
mode1matches (71.27seconds). Proof uses genuine `EntityPlayerMPFake` instances:
15names appear, no16th; repeat skips15; extending to16requests only one; bad
names/counts reject; normal single spawn/look/attack/kill still work; all17owned
fake players disconnect. Two player-rules matches and native menu/catalog checks
pass. The localhost25576child stopped normally; owned player/cache and existing
fixture/config/JAR snapshots were restored. No main-server bots were created.

Two earlier harness attempts were rejected: the first built a malformed query
and observed before Carpet's scheduled join, the second observed cleanup before
scheduled disconnect. Production code did not change. Final checks poll only
presence observations, never replay spawn/control actions. Rejected logs remain
in `build/verification/bulk-players/rejected-{1,2}.*`.

Artifact: `build/verification/skywars2-bulk-players-4fa80764.jar`.
SHA256: `4fa80764fab292d600d8fa98c7e62cdf55d03ad1257604459d7e7268e916f539`.
Evidence/qualification/installation receipt: `build/verification/bulk-players/`.
All507sources/1445classes/3216processed resources match the built artifact
(fabric.mod.json matches semantically plus Loom's expected bundledSGUI entry).
All175protected author paths and31live grimoire overrides are unchanged. No RP.

Automatic installation supersedes the antigravity-only request and waits for
normal shutdown of main735d9905. No forced stop/restart. After verified install,
keep this build, preceding antigravity7a747d86 and exact installed735d9905rollback;
retire four older verified artifact copies. No load benchmark was performed.
