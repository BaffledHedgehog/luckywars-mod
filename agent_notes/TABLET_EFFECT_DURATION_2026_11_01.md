# Effect-duration tablet shared owner (1 November 2026)

User-approved contract: multiply the exact remaining duration of ALL native effect
layers by five; preserve full signed amplifiers, ambient/particles/icon flags and
infinite clocks. Finite clocks saturate at Integer.MAX_VALUE without overflow.
Never clear/re-add effects, replay instant payloads, heal, refill absorption or
reset attributes/HP.

`EffectLayers.multiplyDurations(GameController game, LivingEntity target, int multiplier)`
returns whether any finite live native/owned/absorption clock changed. A positive
multiplier is required; one and client targets return false. It edits existing
native instances through the already registered EffectLayersMixin, then uses
native `onEffectUpdated(effect, false, null)` to sync without modifier refresh or
effect-start callbacks. Hidden chain identities and every non-duration field stay
intact. Dead finite clocks remain dead; infinite clocks remain infinite.

OwnedEffectLayers scales its independently ticking baseline and owner leases, so
withdrawal and native expiry retain the multiplied clocks. ShadowAbsorption
scales its independent amplifier ledger without calling reconcile/publish;
capacity, stored shadow HP, yellow HP and modifiers are untouched. Legacy direct
native absorption is imported before scaling its carrier to avoid double scaling.
No parallel effect engine or checkpoint adapter was added.

`EffectDurationScaleChecks.run(game)` supplies synchronous native Cow checks for
actual hidden layers/full signed amplifiers/flags, large finite/infinite clocks,
identity preservation, instant-heal non-replay, HP/attribute stability, owner
withdrawal/expiry and native positive/shadow absorption preservation/expiry. The
isolated runner must register `effect-duration-scale` and execute it.

Implementation status: source prepared only. No Gradle, native/server, Git or
installation runs performed by this subtask; no runtime PASS is claimed here.
Tablet admission/consumption/MAG/TECH and build/integration belong to the parent.
