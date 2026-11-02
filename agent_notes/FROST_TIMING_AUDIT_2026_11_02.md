# Cold/frozen timing audit ? 2 November 2026

No production timing change is justified by the source comparison. `LbcFrost` remains
unchanged; a native asynchronous `FrostTimingChecks` fixture is added for integration.
Register `FrostTimingChecks.start(game)` as `frost-timing` and tick it after normal game
advancement through `FrostTimingChecks.tick(game)`. Runtime is bounded to280server ticks;
it uses only its four cows, resident air and Frost's own reversible terrain leases.

Exact read-only legacy source under `world/datapacksdisabled/LBC/data/lbc/function`:

- `tick1s.mcfunction` schedules itself `1s`, then runs `mobs_1s` for all mob-tag entities.
- `mobs_1s.mcfunction` lines10-11 calls `other/freeze/cooldown` for positive cold,
  then `other/freeze/tick_1s_frozen` for positive frozen.
- `cooldown.mcfunction` applies/checks the current cold tier first, decrements positive
  cold_cd, then subtracts one cold in the SAME pulse when cold_cd reaches zero.
- `full.mcfunction` clears cold/cold_cd and sets frozen10; `tick_1s_frozen` subtracts
  one per scheduled second and destroys the ice at zero.
- `other/freeze/1..10.mcfunction` refreshes native debuffs for TWO seconds, unlike the
  cold/frozen counters, which advance once per global scheduled second.
- `other/freeze/block/tick_3.mcfunction` holds the frozen body and refreshes one-second
  fatigue/weakness/resistance each entity tick. Those effect durations are not the
  frozen duration. The current40/20native ticks map correctly to these2s/1s effects.

Consequences retained exactly: cold_cd5 means cold first decays on the fifth global
second pulse; cold_cd2 means the second pulse, not an extra full second after zero.
Threshold100 gives frozen10. The existing global phase allows a hit-created freeze to
last9..10seconds until ten decrements. A snow accumulation crossing100 is only checked
at the second boundary, and the frozen decrement later in that same source callback
makes its freshly-created frozen10 become9 immediately. Switching to a new per-target
200tick countdown would silently extend source behavior. SourceCold's first Slowness
amplifier+1 multiplier is correct, subsequent applications add only their base amount.

Pause call-chain is outside Frost: `GameController.tick` returns when the native server
tick-rate manager does not run normally. `LbcItems.tick` returns for dimension TimeStop
BEFORE calling frost.tick. Thus ordinary Frost timers already pause during both native
freeze and custom TimeStop. Looking only at Frost.second would incorrectly conclude
they run. The global20tick phase is preserved rather than recording a new per-entity
fractional timer; checkpoint secondOffset already restores the captured phase.

Legacy tick1s has no custom-TimeStop guard on these counters; the modern pause is an
existing shared TimeStop policy. AGENTS.md section '???? Stand ? ????????? ??????? ?
21 ???????2026' assigns pause/scheduled-tick ownership to LbcTimeStop/LbcTimeStopSchedules
and forbids a second pause implementation. This audit preserves that policy and reports
the source difference rather than adding a new inconsistent Frost clock.

Fixture assertions cover actual successive20server-tick pulses (never calls second()
manually), first existing-Slowness multiplier, cold_cd5/2, same-pulse decay,100threshold,
ten decrements, native footprint thaw and25real ticks through the existing TimeStop
entry path with unchanged counters. Integration owns compilation/runtime qualification;
this scoped agent did not build or start native fixtures while peers edited.

Retained first native frost-timing run failed after approximately six wall seconds:
the fixture incorrectly used an ordinary10HP cow as its frozen timing subject. Pinned
26.3 native tags include ICE in ice -> blocks_motion_no_leaves -> blocks_motion ->
causes_suffocation; Entity.isInWall tests the actual eye intersection and LivingEntity
baseTick applies native inWall1 damage. The frozen cow can therefore die before ten
active seconds (with25paused ticks making roughly six wall seconds). Frost correctly
clears a dead subject; the original diagnostic called that a countdown jump.
The fixture now gives each clock subject1000native MAX_HEALTH without invulnerability,
checks that all subjects remain alive/unremoved, and includes tick/previous/current
frozen/pulse count/nativeHP/inWall/cold-cooldown values on timing failures. Native damage
continues normally. Every original countdown and20tick pulse assertion is retained.
Production Frost remains unchanged; integration must run the corrected native trial.
