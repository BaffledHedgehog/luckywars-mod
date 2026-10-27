# God Hedgehog: warning lines and elevated spawn

Requested: replace the boss's particle warning beams with the shared warning
lines and create the boss20blocks above its former spawn position.

`LbcGodHedgehog.summon` now adds20to the requested spawn Y once. Duplicate
summons reuse the existing encounter without lifting it again. Spawn audio
comes from the resulting boss position. Native physics and the legacy scripted
attack movements remain unchanged; this is not a general collision repair.

Each white-arrow actor owns one shared red `TrialWarningVisual`, replacing the
former white dust samples. It preserves the10block warning span and40active
ticks before the original firing transition. The existing burgundy-to-red
countdown and final opacity fade are reused. Removal, encounter retirement and
match stop destroy the warning; paused actors pause their warning too.

The shared factory now offers an encounter-world beam: it uses the real boss
dimension rather than being retired solely because it is outside the arena.
Meditation factories retain their arena restriction. Epoch retirement, viewer
tracking and Time Stop remain common. No new RP assets or shader edits are
required; the previously prepared red warning model/shader must be installed.

## Verification boundaries

Native probes check exact initial+20placement, duplicate reuse, all201white
attack owners, red model/span/lifetime, actor pause, the actual Imprinted world,
unchanged ray/blast/trophy behavior and normal match-stop warning cleanup.
Shared Sauvojen/Gaycaster warnings and Time Stop are exercised too. This does
not assert physical-client pixels, arbitrary network latency or peak TPS.

Initial broad runs exposed outdated family-fixture assumptions. They are retained
as `initial-family-probes-*` and `second-family-probes-*` under the evidence
directory, not accepted as qualification. Fixtures now exhaust air before
expecting the enhanced-water debuff, respect the established Nexus/Imprinted
custom-item restriction after a real random transfer, and await native entity
chunk readiness before testing distant sneeze-blast exposure. Production spike,
dimension and sneeze behavior was not changed by those fixture corrections.

## Qualified and installed

Offline build passed. Final28recorded results passed in two actual mode1matches
in149.37seconds, with normal owned-child shutdown and exact restored snapshots.
Exact523Java sources,1482classes and3224resources match the candidate archives;
195preexisting dirty author paths and31live grimoire overrides remain intact.

Build `a4683fe2`, full SHA256:
`a4683fe23595b1b20dda1154ab152209f3b5b61c68ba4e92007a2ef860263c56`.
Evidence: `build/verification/god-hedgehog-warnings/`.
Artifact: `build/verification/skywars2-god-hedgehog-warnings-a4683fe2.jar`.

Installer PID24208 installed this build over46034d3c while the main server was
stopped, without forcing shutdown or restarting it. The main JAR hash matches
the qualified candidate. The authoritative receipt is `main-install.json` in
the evidence directory. Earlier installer PID15932 completed its authorized
23a25d43-to46034d3c update during this task; that transition is recorded too.

Two obsolete ready JARs (8d2f7513and6890ae59) were retired after verifying their
exact hashes and the current/rollback copies. Keep a4683fe2, immediate rollback
46034d3c, fallback23a25d43 and the separate network-audit tool. Evidence logs stay.
