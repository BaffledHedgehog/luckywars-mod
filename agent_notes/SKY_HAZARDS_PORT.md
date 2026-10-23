# Global altitude hazards

User clarification23 October2026: high-altitude damage must be independent of
whether Antigravity is running. This correction supersedes the initial38da8388
local Paper hazard. The finite Paper storm still lifts blocks/living entities
and grants Slow Falling; it no longer damages at relative height+30 or owns
the drowning notification.

## Exact legacy thresholds

Source LBC `player_speed.mcfunction:36` stores floor(PosY*100) in player y.
`tick10t.mcfunction:4` dispatches nonspectators in every dimension except
minecraft:nexus and minecraft:imprinted to players_10t; line5 invokes
`drops/antigravity/tick_10t_player` without a marker/storm/radius test.
The function tests inclusive scores150000..14499000 for4 DROWN damage and
1500000..14499000 for an instant ignited Creeper explosion radius100, named
with the existing cosmos translation. Thus drowning starts atY1500 and Cosmos
atY15000, retaining the source upper score bound (Y<144990.01). The existing
no-air message/tag enters once and resets when outside the range.

`LbcSkyHazards` owns the independent10-active-tick player pass. There is no
Antigravity origin/lifetime/presence test and no mapX/Z or build-height gate.
Usual match, participant/observer/service safety and per-dimension Time Stop
rules remain. Spectator/Creative behavior stays native: spectators are skipped,
Creative damage protection remains vanilla. The service runs before the arena
Time Stop early-return so other loaded dimensions are not disabled with it.
Nexus/Imprinted exclusions compare dimension IDs, not entity tags of those names.

## Native damage and author

Drowning remains native positionless DROWN damage; protections/velocity follow
its normal native rules. A narrowly scoped LbcIndirectKills.altitudeDamage
context can reuse an active displacement/levitation cause and the last accepted
player impact. Dying hooks apply that causal player before native kill credit.
Ordinary underwater DROWN is outside this context and retains its own cause.
The same causal author is captured for Cosmos. Natural unauthored altitude
hazards remain natural; ordinary existing native last-hurt credit is not erased.

Cosmos adds one job per eligible pulse to the existing Exasperato round-robin
explosion queue and CPU budget, with no dropped jobs or accumulation cap. Its
source is a transient named native Creeper, never inserted as a live entity.
The engine's explosion DamageSource helper preserves native explosion type and
owner semantics, mobGriefing/MOB interaction and absence of TNT protection.
The shared queue now pauses each job's own dimension; nonarena Cosmos can make
progress while the arena is stopped. Source epoch/stage/resident-chunk guards
and quiet match-stop queue invalidation remain. No separate explosion engine,
shader mushroom, artificial build-height cap or new damage type is introduced.
Existing RP translations are reused; RP/template unchanged.

## Validation

Build20210f2b passed an offline build and two actual isolated mode1 matches:
16 recorded suite results in64.36s.239 source files,658 classes and2708
resources exactly match the qualified artifacts. Fixtures cover exact thresholds,
cadence, arbitrary source-independent X/Z, absence/end of storm, roles,
Creative/spectator and Time Stop, one other loaded dimension, DROWN kill credit
and ordinary aquatic comparison, queue count and one actual power100 Cosmos
in empty space. A10000HP disposable cow survives that native blast atY15050;
players remain at least500 blocks away in three-dimensional distance. No actual client rendering,
disk restart/unload/disconnect, offline coin framework or peak-CPU claim.

The same matches passed finite Antigravity, Exasperato/HUD regressions and
actual match-stop cleanup. Evidence: `build/verification/sky-hazard-final/`;
guarded runner: `tools/smoke_sky_hazard.py`. Three retained fixture attempts
were corrected before qualification: dimension IDs versus entity tags;
native dimension-change/load acknowledgement; and native entity-section
visibility of the high-altitude Cosmos probe. No production invulnerability
bypass or weakened damage assertion was introduced for those fixtures.
The known fake-player Polymer packet-context warning recurred; no final suite
failed. Installed main while stopped; rollback38da8388 retained. The agent did
not start main. Three user-owned Reroll/AcidAntDropChecks/HelicopterEncounterChecks
sources remain unchanged and excluded from this commit. RP/template unchanged.
