# Explosion benchmark audit (read-only)

## Relevant project guidance

`AGENTS.md` § «CPU и потоки» (around line 560): optimize server CPU/tick time, measure before claiming speedups, account for total CPU and copying when considering concurrency. The Nuke-specific notes near lines 48–64 require all 32 power-20 explosions to finish, budget work across ticks, and retain queued work; benchmark must therefore compare both per-tick peak and total completion cost.

## Existing repeatable entry point

Run `python tools/smoke_terra.py --server-dir <isolated-server> --exasperato-academy` (script refuses the project parent as server dir and requires a stopped disposable server). It starts match, issues checks, stops match, and writes `agent_notes/test-results/exasperato-academy-results.json` with artifact hash and elapsed wall time. The mode calls checks in `GameChecks` in this order: `exasperato`, `exasperato-academy`, then `exasperato-cleanup`.

For a compact native-blast comparison, reuse Academy check's Chaos and especially Nuke fixture, rather than inventing a new command: `GameChecks.checkExasperatoAcademy` (around 5726), `checkExasperatoNuke` (5853), `LbcExasperato.nuclearBurst` (340), `blastTick` (289), and `instantBlast` (434). The Nuke fixture explicitly loads its test ray chunks, hits a known stone block, services all 32 power-20 native TNT explosions, and logs `blastSlices`, `maxSliceMs`, and `totalWorkMs`. Repeat the same artifact/check and same isolated world/server/JVM settings per Paper/Moonrise/Lithium build; retain each log/result and compare max slice plus total work. The current check only does one Nuke sample; for a useful benchmark collect repeated runs after warmup and report median and tail, keeping concurrent player/entity/world conditions fixed. This is native explosion workload; it is not a vanilla primed-TNT entity-fuse workload.

## Coverage already present

- `checkExasperato`: `GameChecks.java` around 5175. Impact gives exactly 85 TNT + 50 lightning; each TNT is owner-attributed, fuse 2, zero motion, and carries `LbcExasperato.EXPLOSIVE`. Also covers teammate filtering, one-burst behavior, native ownership/collision/lifecycle.
- `checkExasperatoAcademy`: around 5726. Academy item/mode/mana/hand gates, mode 1 lifetimes, electro-ray's exact 13 TNT fuse waves (50, 60–62), owner, gravity-free and stationary payload; Chaos produces 40 caused lightning, no TNT entities. Base Exasperato regression is run first by smoke script.
- Nuke subcheck `checkExasperatoNuke`: around 5853. Queued search, loaded target beyond 280 blocks, descent, immediate rocket removal, no TNT entities, pending blasts progress across slices until all 32 finish, and collision stone becomes air. `blastTick` itself uses `instantBlast(...,20,...)`, which creates fuse-zero owned `PrimedTnt` source and calls native `ServerLevel.explode(..., Level.ExplosionInteraction.TNT)`.
- `checkExasperatoCleanup` (5926) asserts charges/searches/mushroom/blasts and tagged TNT/lightning payload clear after stop.

## Gaps for benchmark / Nuke completeness

These are functional smoke assertions, not a controlled performance benchmark: Chaos only logs one cast's synchronous `System.nanoTime`; Nuke logs one manually serviced fixture, so its slice durations are not server tick timings. The 85 TNT ownership assertion sees delayed TNT before its fuse; it discards them before natural detonation and does not check explosion results. Nuke's `pendingBlasts()==0` proves queue completion, and one collision block becoming air proves an effect, but no assertion proves every one of the 32 explosion calls was reached/applied or that the blast's broader block/entity results match a baseline. There is no dedicated native TNT density check and no profiler/TPS sampling in `smoke_terra.py`.

A small addition for a genuine dense entity-TNT benchmark would need an opt-in fixture/check that places a fixed, documented cluster of tagged owner TNT with fixed fuse and fixed surrounding blocks/entities, waits for actual detonation over server ticks, and records tick durations plus survivors/results. Keep this separate from Nuke's existing native fuse-zero benchmark: they exercise different paths. To benchmark current Nuke path now without code changes, use the Academy smoke mode and extract the Nuke `blastSlices/maxSliceMs/totalWorkMs` log for repeated identical runs; label those as synchronous service-slice timings, not MSPT.
