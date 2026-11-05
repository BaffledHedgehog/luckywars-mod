# Complete Funny and Delta TNT native port - 5 November 2026

Qualified and installed test SHA256 `a1afc8cbd23262cbdedc32cee6844071080b32d00f887bfbcc709936b20e98bd`; production
`797d39588a7a3446d7f11a94a8811d61ac7fc5987a4bd5a1320fdefa4adce26b`.

## Source and author decisions

Legacy source remains read-only under `E:/servers_mew/cringe1_21_4_skywars`.
The original [Delta source closure audit](DELTA_TNT_SOURCE_2026_11_05.md)
records the exact source paths, clocks, 33 offsets, 20 branch ordering, landing
variants, Exoperso4 flight/nuclear downstream and terminal fireball sweep.
That audit is historical; its unanswered decisions are resolved here.
Both canonical definitions retain every original function, count, condition and
presentation component except their explicit native item binding. Only ready
`lbc:ported/funny_tnt` and `lbc:ported/delta_tnt` leaves are admitted.

Author decisions: Funny uses TECH placement and keeps the global same-dimension
10% marker stop, including foreign markers. Delta uses TECH placement and keeps
all terminal foreign fireballs. Released Exoperso inherits the actual Delta
creator, correcting the source random-owner defect; random nearby-center aiming
remains. The old root datapack two-tick polling is replaced by native input.
All 33 stationary centers run 1000 active ticks with all 20 independent 1%
branches. No recursion, payload or entity caps are introduced.

## Native owners and lifecycle

`LbcImbaRecursionItems` owns native placement and ready grants;
`LbcFunnyTnt` and `LbcDeltaTnt` own typed native actor state and replayable RNG.
Delta reuses `LbcSpecialFireballs` for all four complete groundbam landing
variants: nine-fang target bursts, 11-cubed AIR candidate fire fill, ordinary lightning and
extra strong blast, preserving the carrier explosion. It reuses `LbcExoperso`
for released flight and the full existing nuclear sequence. The existing clone
and queued-work checkpoint owners carry canonical x2/x4 and whole replay.
Native TimeStop pauses accepted work. An unloaded actor remains pending in its
checkpoint; unloading is not death. Placement and downstream protection use
the shared native owners, with causal creator attribution retained.
Funny's strict fill shares the terrain writer with native skip-on-place behavior,
so preexisting redstone does not prime its blocks during the fill. Delta's
ordinary redstone placement explicitly notifies neighbors and primes adjacent
TNT. Native checks cover both. Fire retains ordinary on-place survival: the
fixture verifies the supported121-cell plane, immediate unsupported removal,
nonair control, completed1331-cell traversal and environmental kill credit.

## Exact qualification and installation

Eight isolated native matches passed on one exact test archive: modes1/3 each
run `tnt-recursion` and `tnt-recursion-rewind`; mode1 also runs
`special-fireballs`, `exoperso`, `clone-skills` and `imba-followup-rewind`.
Every suite records PASS, completed match groups, normal child shutdown, no
remaining child/locks, exact fixture snapshots and, for mode3, auxiliary snapshot
restoration and removal of temporary maps. See the per-archive result JSON/logs
listed in `build/verification/imba-recursion-2026-11-05/qualification.json`.

The gallery-chunk qualified baseline is test `88b9ab74` / production `2372f3b1`,
source HEAD `48753dde`. The candidate overlays 11 owned Java families and four
resource inputs. All archive entries, retained-class linkage, two canonical
definitions and two ready leaves are checked; 10129 other source inputs and
275 protected author paths retain their guarded hashes. Production has no
MapAuthoring/MapDeletion classes. Main stayed at its baseline hash until
qualification; both build/libs archives were atomically replaced after checks.
The separate safe installer installed the exact qualified test artifact without
stopping or starting main (`restarted:false`), retaining rollback
`main-rollback-88b9ab74.jar`. No author resource-pack changes.

Full IMBA pool, TMT-last, modes2/4, skill2 and broad public rewind release gates
remain unchanged. This scoped native qualification makes no physical-client,
peak-TPS or disk-restart-persistence claim.
