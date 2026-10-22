# Screamer drops - QUALIFIED (22 October 2026)

Source contract: [bounded drop audit](NEXT_DROPS_AFTER_HOME_SOURCE.md). This batch
ports the two complete screamer leaves, without exposing whole trap/legacy/imba
pools or the unfinished infinity/forest dependency trees.

## Items and execution

- `skywars2:scare_drop`, loot `lbc:ported/scare_drop`: normal source
  repeating-command-block carrier, `lbc:drop_trap`, tier FF, yellow nonitalic
  `trap_screamer`, one white nonitalic `trap_desc`, `skrimmer:1,drop:1`.
- `skywars2:scare_drop_old`, loot `lbc:ported/scare_drop_old`: plain barrier,
  `skrimmer_old:1,drop:1`, no authored name/lore/tier. Original resource-pack
  assets and keys are reused without writes to the pack or template.
- The shared dispatcher selects the nearest eligible player without a distance
  or allegiance filter: self, allied and Creative players remain eligible;
  observers and protected service roles are excluded by existing target policy.
  No eligible target still consumes one unit, as in the source.
- Normal then old run in source branch order after cages. A combined token
  debits one unit total. Fake items, stale epochs, unresident token chunks,
  unloaded pending tokens, time stop and match end reuse the shared lifecycle.
  Scares create no actor, timer, scan or secondary pending collection.
- Normal feedback reuses the existing broken-totem/psychic-wand helper: three
  ordered groups of GHAST_HURT pitches 0/1 and AMBIENT_CAVE pitches 0/1/2, then
  WITHER_SPAWN pitch0; 16 sounds total, MASTER, volume1. Only the target receives
  them. One normal cosy-smoke packet carries count5000 at target+1.62Y,
  delta(.5,.3,.5), speed.05. Blindness is 120 active ticks, amplifier10, hidden.
- Old feedback has the same 15 ghast/cave sounds, no wither sound, volume100000
  explicitly retained by the author. Smoke is force signal smoke with the same
  count/position/spread/speed. Blindness is 200 active ticks, amplifier10, hidden.
  Native ParticleCommand confirms force maps to overrideLimiter=true,
  alwaysShow=false. It does not mean broadcast or 5000 server-side entities.
- These traps have no source magic restriction. A beacon does not prevent
  activation; native immediate effect cleansing can reject blindness. Feedback
  remains independently dispatched, including when blindness is rejected.

## Attribution

Accepted player-sourced blindness uses the existing effect-status provenance
map. Lethal fall/void while that exact effect is active considers its owner and
any newer physical fall cause; a later actual attacker still wins. Native
cleansing/removal/expiry or replacement invalidates the effect reference.
Rejected shorter/ineffective applications cannot claim an unchanged effect.
This also covers existing psychic helper callers without new scans or direct
damage. Self or ownerless effects do not invent another player as killer.

## Qualification

Build and two actual isolated mode-1 matches passed: 60 suite results in 89.75s,
including all previous drop/home regressions. Native give/loot components,
nearest self/allies/Creative/protected roles, combined flags/one debit/no eligible
target, fake/stale/unresident/unloaded admission, immediate type5/6 cleansing,
nonmagic activation under a beacon, time-stop debit/effect-duration pause and
actual match-stop cleanup passed. Native cow lethal fall/void retained the
player cause and original environmental DamageSource; removal, newer attacker,
identical/weaker/shorter rejected applications passed. An actual lethal broken
totem pop retained native consumption/life protection, curse100 and blindness
clearance through the unchanged two-argument helper route.

Ready artifact: `build/verification/skywars2-scare-drops-1d07ce8a.jar`, SHA-256
`1d07ce8a45ab2d92d33784d784bb78cbf2aa60a9fdfc0f2f8a5f73f408013775`.
Evidence: `build/verification/scare-drops-final/` (`build.log`, `native.log`,
`server.log`, `results.json`, `qualification.json`). All 170 current Java files,
512 compiled classes and 2668 processed resources match the respective JARs;
ZIP CRC, mod metadata and nested dependencies passed. Main69de8601 remains
unchanged, not installed. Isolated backups restored, server stopped and both
fixture locks released. Only the two existing Polymer fake-bot packet-context
ERROR lines remain in the final log, with no failed suite.

Packet construction/recipients/order/volume/force are source reviewed; actual
packet delivery, client loudness/particles/rendering are not captured by this
run. Native mob lethal-damage attribution is not a full natural-trajectory,
player-coins or Carpet-player-death fixture. Actual restart/unload/disconnect
persistence, full psychic-mode casts and peak capacity remain unqualified.
Next bounded acquisition audit must retain the infinity/forest reward closure
requirements recorded in the source note; no whole pool is opened here.
