# Overload native review

Read-only comparison of the current `LbcOverload.java` with the legacy LBC functions. Findings below identify behavior mismatches; no runtime claim is made.

## Concrete differences

1. **Corrected before the passing build:** the first implementation displayed chestplate Resistance particles. All five bands in `other/negative_mana_resist.mcfunction:1-5` end with the `true` hide-particles argument. The final call uses `false,false,false` for ambient/particles/icon and retains the source amplifier and twenty-tick refresh.

## Reviewed paths that align

- Crux source subtracts the configured mana amount, fires five full-set ray functions, replaces only matching-slot diamond armor, and clears one matching Crux (`other/overload_crux/use.mcfunction:1-18`). Native `use()` follows that order and uses signed mana addition, so debt is allowed. It does not call `beforeRegen()` or immediately re-drop Crux after use, matching the source's clear-only ending.
- Both the regular and full-set ray helpers use the same recursive path; the full-set variant starts its counter at `-30`, while the normal and big variants start at `0` (`tentacle_full_set_2.mcfunction:1`, `tentacle_2_1.mcfunction:4`, `tentacle_big_2_1.mcfunction:5`). The native 61-point full ray and 31-point normal/big rays reflect those recursions.
- The old player loop skips `time_stopped` (`mobs_tick.mcfunction:3`) and excludes spectators (`mobs_tick.mcfunction:3`, `players_tick.mcfunction:1` context); native activation uses the same time-stop tag and `canUseGameplayItems`, which also excludes observers. The source jump passive itself has no `stopper_magic` guard (`items/armor.mcfunction:13`), and the native armor movement path likewise has no magic-suppression check.
- The earlier suspected missing full-set spell mana adjustment is intentionally not listed as a finding here: native wand spending is being handled at the wand layer, and that +10,000,000 legacy temporary-mana behavior is explicitly excluded by the current implementation direction.
- Native 26.3 source confirms `ServerPlayer` updates the `HEALTH` scoreboard criterion from `Mth.ceil(getHealth() + getAbsorptionAmount())` (`ServerPlayer.java`, `tick()`), so the native helmet threshold at `LbcOverload.java:155` matches legacy `hplvl`; the previous review claim that this criterion excluded absorption was incorrect.
- Crux cost has no confirmed mismatch at the checked default: native construction initializes the scoreboard to 7500 and `use()` reads that value at cast time; legacy `config.mcfunction:110` also initializes 7500 and `other/overload_crux/use.mcfunction:1` subtracts the scoreboard value. The source debit allows debt and does not perform an affordability check.

## Unported source state to track separately

Legacy `players_tick.mcfunction:33` skips mana regeneration, Crux drop/rearm, and negative-mana dispatch while the `nomagic` score is positive; `other/effect_nomagic.mcfunction` and other functions increment that timer. The current native path implements the no-magic aura/beacon suppression plus `time_stopped`, but has no equivalent countdown provider. This is an unported legacy effect if it is in scope, not a defect in the aura/beacon replacement. The source's Crux wand invocation separately checks `stopper_magic` (`workingitems/spell1_default.mcfunction:114`).

Root subsequently corrected the chestplate Resistance to hide particles and its
icon, matching native `effect give ... true`. The separate source `nomagic`
countdown/providers remain a later migration; aura/beacon suppression already
uses `LbcMagic.suppressed`. No production files were changed by the reviewer.
