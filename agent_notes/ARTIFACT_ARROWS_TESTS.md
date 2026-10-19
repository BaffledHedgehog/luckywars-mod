# Mystical Artifact Arrow Regression Checks

The focused slice is `tools/smoke_lbc_loadouts.py --server-dir <isolated-server> --artifact-arrows-only`. It checks source loot and the Polymer item roundtrip for `lbc:ported/mystical_artifact_arrows`, then exercises the type-4 inventory mask and native arrow cleanup during a match. Cursor, 2x2 crafting, and 3x3 crafting checks use the same native inventory click packets as the existing magic-artifact checks.

The active fixtures cover native `Arrow` and `SpectralArrow` entities, including owned and same-team arrows, overlapping type-4 holders, the inclusive 16-block boundary and a 16.01-block survivor, arrow and holder movement, stationary grounded arrows, `time_stopped`, native inventory removal, an unrelated projectile, another dimension, a type-1 artifact, a Polymer magic beacon, and Creative/developer observer eligibility. Each spawned entity and the temporary beacon block are cleaned in `finally`; one live type-4 inventory holder is deliberately retained so the runner's subsequent `sw stop` checks its cleanup. The cleanup check also expects the loaded arena's tracked-arrow index to be empty after map unload.

The adaptive search has explicit fixtures: one arrow against two holders exercises
the sparse-arrow branch; three arrows against two holders exercises local holder
queries, retaining a distant arrow. The fixture also rebuilds the loaded-entity
index and verifies that unload removes disposed references.

The final arrow-stage build passed its focus run in 62.55 s (four groups) and
the full run in 268.44 s (39 groups, 14 matches), both using SHA-256
`9066e62f5026f29a2cd3efb17b2e36c1e41ad0aec5f8974081e3e43fdd88654a`.
The arrow-stage full log/results are archived as
`build/verification/lbc-loadouts-before-looper-viewer.log` and its
`-results.json`. The fixture iteration with a probe in an unloaded chunk is
preserved in `artifact-arrows-unloaded-probe.log`; the final probe is far
vertically in the same loaded chunk. Current installation evidence is in
[SERVER_TESTS.md](../SERVER_TESTS.md), since the subsequent viewer-only Looper
change produces a newer build.
