# Datapack family correction,1 November2026

Author rulings supersede the provisional Paper implementation in6917e42f.
Lucky opening and Mom/Otchim/birth are unjammed. Current integer Luck+1 roll
behavior was already implemented. Birth emits real damaging native lightning
at each existing loaded entity in its CURRENT dimension, with both Nexus and
Imprinted excluded. Positions are snapshotted before spawning lightning, and
before inserting the child, preventing recursive enumeration/newborn selection.
Nearest Mom selection now uses the source2.5-block spherical distance.

The invented6-damage Paper beam is removed. Each active second the child follows
the nearest nonspectator player only if none is within5, using the source's
integer-quantized direction and approximately0.5 velocity. Source movement is
MAG/artifact suppressed; author-requested chat and three-Lucky-block hand cycle
always run. TimeStop/server freeze still pause clocks. Shared causal sources,
native body tags and MatchSpellCheckpoint derived-index replay are retained.

Only LbcFamilyDrops.java and FamilyDropChecks.java changed in active runtime
sources. Incomplete TMT drafts are excluded from compilation and registration.
The actual RP,224 pre-existing author paths and31 live override files are intact.

Validation: compileJava/processResources passed. Frozen9747-input manifest and
incrementally assembled component:
40a3370df28a5562e4e4378e1afa2ce5504875c531a30c2aec85593c579294ba.
Evidence directory: build/verification/lucky-datapack-correction-2026-11-01/.
component-verification.json and40a3370d-family-drops-results.json record passing
family-drops, family-drop-source, family-drops-stop-probe, actual-after-stop
family-drops-cleanup and full vanity-rollback. The test verifies birth lightning,
no recursive enumeration, source motion/no intervening-cow damage, unjammed hand
cycle, dimension exclusions, paused clocks, source survival and actual shared
birth->rewind->reindex->rebirth. Generated fixture lightning is removed before
the following real tick. Fixture25577 exited normally; exact backed-up state,
extra level/ticket files restored and no retained child/world locks.

This is component verification, not an installer qualification. Main remains
unchanged at e67403fbea792d945fc9013d3ab10376f0528999e6e0e67348388c0d3bf029d9.
No main stop/restart/install or push was performed. The newer TMT deferral does
not block unrelated porting; full Lucky acquisition still requires its dependency
closure under the normal release process.
