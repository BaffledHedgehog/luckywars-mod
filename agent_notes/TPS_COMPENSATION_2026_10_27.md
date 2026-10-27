# TPS compensation — implemented and installed

Author authorization, 27 October 2026:

> Да, компенсацию добавь кстати

This replaces the earlier feasibility-only decision for block mining and attack
speed. Historical notes describing that earlier scope remain historical; do not
repeat those footnotes in unrelated progress reports.

## Behavior

Native server player melee charge, item-swap charge, minimum attack charge and
mining progress now account for elapsed wall time when the server falls behind
its normal 20 TPS. The vanilla client already advances its local prediction in
real time, so synchronized attack/mining attributes remain untouched. There is
no client mod, FOV modification, faster client prediction or permanent modifier.
Custom weapon casting timers and damage invulnerability intervals are unchanged.

Each action uses the greater of native elapsed ticks and elapsed wall time,
without adding them together. Catch-up ticks therefore do not count credited lag
twice. Attack resets preserve vanilla's distinction between resetting only attack
charge and resetting both attack/swap charge. Zero/negative attack speed retains
the existing signed-delay behavior.

Mining preserves native START admission, STOP's 0.7 validation threshold,
instant-break behavior and delayed completion. Active and delayed sessions have
separate clocks; starting another block cannot erase the older pending clock.
ABORT/completion invalidate active compensation, including the stale STOP to
delayed-destroy path. Native delayedTickStart is an absolute start tick; progress
uses gameTicks minus that start rather than treating the absolute value as a
duration. Negative mining uses the same fractional wall-time credit and retains
its original one-copy-per-update bound, full block entities and permission checks.

An always-running server tick maintains pause/level epochs even when Time Stop
cancels Player.doTick. Native freeze, deliberate `/tick rate` changes, Time Stop,
death, spectator state and dimension changes cannot bank compensation. The
interval credited per sample is capped at 250 ms: prolonged stalls/below roughly
4 TPS are only partially compensated. Original packet arrival times cannot be
reconstructed when multiple actions arrive together after a blocked server tick.

## Native evidence

Build **735d9905** passed **72 suite results**, 8 preparation and 2 setup/cleanup
results in two actual mode1 matches, 70.45 seconds. Exact packaged contents:
503 Java sources, 1436 classes and 3215 resources. `TpsCompensationChecks` is
restricted to localhost fixture port25576 and its explicit fixture directory;
its bounded real-time stalls cannot be invoked on the main server.

Checks cover native attack/reset/minimum-charge methods, native mining
START/STOP/delayed/ABORT/instant paths, real bounded stalls without intervening
server ticks, fractional 18/19 TPS arithmetic, catch-up accounting, freeze and
deliberate tick-rate opt-out. Shared signed attributes, negative mining,
knockback, melee, Time Stop, grimoires, effect layers, FOV and firearm suites pass.
The delayed-progress helper is invoked reflectively to inspect native progress;
this is not a claim of a physical client's rendering or a measured 18/19 TPS load
test. No network latency or peak TPS claim. Existing Polymer packet-context
diagnostics during scoped third-participant creation remain recorded separately.

Native source: local 26.3 merged sources JAR, Player cooldown/reset methods,
ServerPlayerGameMode.tick/handleBlockBreakAction/incrementDestroyProgress and
TickRateManager. No legacy timer proxies or damage-source changes are involved.

Reproduce: `python tools/smoke_tps_compensation.py --server-dir build/verification/key-reuse-server`.
Evidence and install/cleanup receipts: `build/verification/tps-compensation/`.

Main `mods/skywars2.jar` is installed with SHA256
`735d990587941b2cdd0d762e8c93798a7a76975d952b901a036a07bd4355c540`.
No forced stop or restart. Current ready JAR and prior installed16502e73 rollback
are retained; obsolete736bbbc1 was removed after hash/size/path verification,
freeing 40,315,397 bytes. All175 protected author paths and31 live grimoire
overrides remain byte-exact. No resource-pack changes are required.
