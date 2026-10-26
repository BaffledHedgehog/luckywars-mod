# High-level effect terrain latency

User clarification (26 October 2026):

> помнишь что я говорил - временное падение тпс до 18-19 допустимо, это лучше чем прогрузщка заполнения огромная, и я все таки хочу чтобы заполнение было быстрее чем щас намного. Даже тупой fill на большие области на главном потоке без бюджета как будто работал быстрее и тпс не так сильно жрал, потому что щас совсем медленно стало, это недопустимо

Responsiveness takes priority over keeping every affected tick below50ms. This
change concerns fire-resistance/conduit fields and weaving block changes.
Infestation entity spawning retains its separate queue and count semantics.

## Implementation

The old fire/conduit scans spent128 candidates on a corner-first cube, including
points outside the sphere. At radius15 a full pass took466/233 ticks respectively
with no competing sources. Radius50 weaving spent up to504 ticks crossing its
cube. Moving a field reset its cursor. These are source-derived work-count
delays, not measurements of the user's physical client. See
[the exact source audit](EFFECT_QUEUE_SOURCE_2026_10_26.md).

`EffectTerrain` uses cached center-first sphere offsets for fields and a lazy
center-first cube-shell iterator for weaving. No rejected sphere corners or
million-element weaving sort/allocation is needed. Moving fields retain the
unfinished scan, discard only its fringe outside the latest radius, then follow
the latest center. One source retains one job instead of creating jobs per move.
Finished stationary fields refresh on producer requests after five ticks.

A START_SERVER_TICK timestamp lets the shared queue use current-tick headroom
to a54ms soft total target, with2ms reserved for remaining callbacks. This
replaces both old tiny per-source quotas and the initially considered fixed6ms
candidate. Every1024 visits rotate jobs; time is checked at most16visits apart.
An additional1,048,576-visit bound prevents runaway scans. A native write and
later fluid/light/callback/GC work can exceed the soft deadline; this is not a
hard global18.5TPS guarantee. World reads/writes remain on the server thread.

LegacyII/III local behavior, radius caps15/50, weaving's finite candidate cube,
water neighbor notifications and effect-author scopes remain. Unsupported fire
avoids a redundant place/remove pair while preserving native portal ignition.
Greed, roles, Time Stop/frost, resident chunks, cancellation and actual-stop
cleanup remain authoritative. Candidate scans skip unloaded chunks and preserve
native placement side effects.

High Fire ResistanceIV+ aura damage uses native `inFire(),1`, the ordinary fire
contact amount, independent of source level. Native cooldown and target fire
immunity/negative resistance still apply. The agreed level-dependent aura
radius and WILD source attribution remain; damage does not scale with level.

## Native verification

The new asynchronous `effect-terrain` fixture measures sparse radius15 fire and
conduit latency, a moving fire source, two simultaneous dense conduit fields
covering14,147 cells, and first nearby output from radius50 weaving. Setup,
resealing and restoration use bounded native block-snapshot batches. It also
compares aura levelsIV/CCXXVII/int-max with actual native fire contact, cooldown,
resistance, fire immunity, Greed and paused targets. Local runner:
`tools/smoke_effect_terrain.py --server-dir build/verification/key-reuse-server`.
Exact measured results, artifact qualification and install receipt belong in
`build/verification/effect-terrain/`.

The offline build and all44 recorded results passed in two actual mode1 matches
(84.37seconds including setup, restoration, map loading and regressions).
Both runs filled all14,147 cells with source water, with14,147 recorded writes,
at full-fill latency2 and1 native ticks respectively (about0.10/0.05seconds at
20TPS). The immediate center was visible at submission. Sparse radius15 fire
and conduit probes, including the boundary and moving fire source, appeared in
2ticks; the first nearby radius50 web removal took1tick. Maximum measured queue
work was49.76/43.96ms. These controlled native results do not assert peak TPS on
the main server or physical client-rendered timing.

Signed storage/commands, effect math, shadow health, environmental effects,
operator/player rules, projectiles, Time Stop, rifle scopes/SLR, map lighting,
artifact colors and actual match-stop cleanup also passed. No author resource
pack or template changes are required.

Qualified artifact: `build/verification/skywars2-effect-terrain-cbbb82ee.jar`;
immutable evidence: `build/verification/effect-terrain/qualification-cbbb82ee.json`.
Exact439 Java sources /1218 classes /3140 resources match the tested artifact.
Installer11424 adopted it instead of pending map-lightingff96bdee and waits for
normal main shutdown/world unlock (port24566, RCON disabled). Its live receipt
is `build/verification/effect-terrain/main-install.json`. Main disk remains
1a8a3825; no forced stop/restart. Keepcbbb82ee,ff96bdee,1a8a3825. Obsoletec9e36258
was retired after qualification: cumulative325 JARs /11589053338 bytes removed.
All175 protected author paths and31 live grimoire override files are unchanged.
