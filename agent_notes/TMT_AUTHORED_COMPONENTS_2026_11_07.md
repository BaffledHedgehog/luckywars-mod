# Authored component-only TMT inputs and shared vanilla egg factory

Component verified, **not installed**; full TMT goal remains active. Complete parent:
`tmt-acquisition-2026-11-07/component-verification.json` (`82333594`/`757cdfe0`).

Complete internal candidates:

- Test `3981713db9eb5f005a7bf5f052be46b7fd8c6c39fe05d29b4287b4fe3c55008c`.
- Production `c09e2533eb2d02796a5b5d584d999df76515d15b53b1c3073bdc781d90097daf`.

Four explicit Java families/one resource and the canonical generator change.
Qualification: `build/verification/tmt-authored-eggs-2026-11-07/component-verification.json`.
All eight suites pass on this same final artifact: tmt-authored-eggs, tmt-block-use,
tmt-current-content, tmt-acquisition, content-gallery, tmt-catalog, tmt-mana-potions,
tmt-firework-ammo. No carry-forward from the failed first candidate.

The vanilla egg gap had two causes. Native use-on admitted only mod-native factories,
and build_tmt_use_profiles emitted vanilla-source leaves only when custom_data existed.
The latter silently omitted meaningful gameplay component-only definitions. Its
existing14-component allowlist now suffices for retaining such definitions. Cosmetics
stay outside that allowlist; native owner admission still decides executable profiles.
Canonical source prototypes1879->2274; actual native families1001->1125 and variants
1883->2227. These are inventory counts, not a full-goal completion percentage.

Authored vanilla SpawnEggItem inputs reuse LbcTmtBlockUses and the genuine native
use-on/fluid-use factories with private ENTITY_DATA. Admission compares against the
vanilla egg's real default; bare vanilla eggs remain excluded. Existing legacy-to-native
factory aliases retain priority over the new generic path, preventing an old special
egg from bypassing its actual owner. Entity type owns one semantic family across
vanilla egg/name aliases; full authored data remains a private variant.

Vanilla firework component inputs canonicalize to the existing authored native
firework item/family. The established factory, payload projection and crossbow-ammo
contract are reused, without a second implementation or alias ticket. Additional
ordinary potion component-only doses enter the existing typed drink/splash/lingering
families automatically, preserving their native effect identities/levels/scales.

The new native check uses an actual admitted Illusioner property and an existing
native low-Vex property on one genuine TMT item. Each has its own NoAI override; an
outer Cow ENTITY_DATA deliberately conflicts. Real player gameMode.useItemOn emits
exactly Illusioner+Vex, both NoAI, no Cow. Each accepted native factory debits once,
other hand remains exact, full outer state/private siblings and native item codec
survive. Illusioner aliases have one family; bare eggs are negative controls.

First candidate's five neighboring suites passed, but the new native check could
not find Illusioner: this exposed the prototype builder's custom_data-only admission.
After fixing that source gate and adding firework canonicalization, all eight final
artifact suites pass, including potion and ordinary/native/private crossbow fireworks.
Fresh report:2274 prototypes/1125 families/2227 variants,138 service and9 inert findings,
no pending dispositions. **Service classification is still not a completion proof.**

10490 guarded inputs and281 author paths exact; three unrelated source WIP/HUD/RP/Main
unchanged. Sequential512MiB compiler then1280MiB fixture; normal exit/exact fixture and
ticket restoration/no locks. Main83b0ea27 unchanged; no installer/restart/author-RP edit.
Keep all fixes for stable. Prepared item-atlas overlay remains the visual deliverable;
no physical Minecraft rendering claim.

## Concrete remaining capability work

The fresh service audit finds two meaningful component domains still excluded:

-40 ordinary potion-arrow source doses (e.g. poison/slowness/oozing) on vanilla tipped
 arrows. LbcTmtAmmoPayloads already captures native effects and LbcQuiver.payloadContact
 applies them; extend typed admission/canonical family without introducing another
 hit engine or granting free ammo. Native potion-duration scale must be retained.
-Old black shulker with CONTAINER_LOOT=lbc:infinity_old and no inf_shulker flag. This
 is a real recursive old loot container, not a blank prototype. Reuse exact-table
 admission and existing private placed-container/drop preservation; keep ordinary
 vanilla boxes and unrelated blank containers outside the ability domain.

Other remaining vanilla service examples are bare/default firework duration1, empty
lingering dose, category-only crossbow/head/block values; inspect actual components
before inferring another gap from an ID. Full current class/component audit and stable
assembly/atlas delivery remain. Do not substitute zero pending rows for that audit.

Read-only protected-source review is in
`build/verification/protected-java-token-comparison.json`: AcidAntDropChecks and
HelicopterEncounterChecks have identical Java tokens/literals (formatter-only WIP).
LbcRerollCrafting has two actual additions: white nonitalic preview name and dark-purple
nonitalic description. Preserve both authored styles in eventual full-source assembly;
none of these three files was edited or compiled by this component.
