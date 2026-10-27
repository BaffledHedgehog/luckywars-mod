# Shared custom-status audit (27 October 2026)

Read-only production audit of src/main/java/me/skywars2/game, excluding *Checks.java. Families: scared, lookback, twitching, piss, screamray, shock, cold, frozen, target, delayed_death, reverse_mana, gatling_fovos, acid_rifle, ray. No source changes, builds, Git or server actions.

## Result

No further confirmed raw-score bypass beyond the Sauvojen scared defect already corrected in the current tree. Do not replace intentional source ledgers with parallel mechanics.

- SauvojenMeditation.madness now uses psycho.afflict, psycho.effectValue (lines 275-276); cancellation uses psycho.clearStatus (220). These bind the shared actual state.
- LbcPsycho.hudValue/effectValue (148-153) reads native effects/rays maps. afflict (366), addRayTicks (123), clearStatus (156) own lifecycle. Other producers (FeedbackDrops, HedgehogSpikes, Summons) call afflict.
- LbcElectro.shock (370), clearEffects (368), shocks (586) own native shock map. Academy and Bolts use shock. Popit and Traveller call clearEffects before zeroing compatibility shock score; that reset is not a bypass.
- LbcFrost uses native state including persisted cold/frozen NBT, not those scoreboard counters; AdvancedSnow calls frost.snow. The quoted cold/frozen strings in Frost are persistence keys.
- LbcHoming.curseSeconds/curseTicks read native curses map. clearCurse (113) clears native state and compatibility target score.
- LbcAcidCorruption.deathRemaining/applyDeath/second/clear (185-207) deliberately use delayed_death score plus original-owner ledger. Raw score writes within this service are canonical. Popit checks the same canonical value before native clear.
- LbcMana.reverse/applyReverse deliberately use reverse_mana score with reverseOwners attribution. KFC calls reverse setter, preserves existing hostile attribution, and its own source extension is not a replacement hostile application.
- LbcAwakenedAcid pending/queue/clear use acid_rifle score plus rifles epoch/hand ledger; LbcErecto rayPending/addPlayerCounter/clearRay similarly use ray score plus rays epoch map. These services own the score.
- LbcBlind.queued/addPlayerCounters owns native volley state corresponding to gatling_fovos; no external raw consumer found.

## False positives excluded

LbcKfcFood.SOURCE_SCORE_ADDITIONS is the authored historic score-addition ledger. addExactSourceScores (175 onward) also explicitly routes native grants: Psycho, Electro, Blind, Homing, Frost, and other services; special cases route ray, acid_rifle, reverse_mana. Its raw historic entries are intentional compatibility side effects, not missing native grants. Likewise clear registries and precise compatibility resets are not independent effect engines.

Production exact-objective scan found no external raw score reads for scared/lookback/twitching/piss/screamray, no external raw writes for those names outside KFC ledger/clearStatus, and no cold/frozen score bridge. Dynamic name clearStatus is owned by Psycho itself.

Recommendation: no additional change from this focused audit. Broader non-status duplication requires separate behavioral inspection rather than inferring bugs from similarly named counters.

## Follow-up integration

The confirmed void_resistance HUD omission is now addressed in LbcItems through
mana.hudValue, without another state store. SharedStatusHudChecks exercises actual
outgoing packets, GREEN styling, native Time Stop pause, source-second decrement,
expiry and selective canonical fear clearing. See SHARED_MECHANICS_AUDIT_2026_10_27.md
for the larger producer/consumer audit and qualification notes.
