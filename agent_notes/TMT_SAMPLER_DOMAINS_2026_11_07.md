# Unified TMT sampler domains — 7 November 2026

Standalone implementation draft: `LbcTmtCatalog.java`. No shared Java/resource/build
edits, JVM/server launch, compilation, installation or author resource-pack edits.
This is an integration component and a concrete domain audit, not full TMT closure.

The constructor takes the existing `LbcTmtUseProfiles`. It retains every admitted
`native_use/…` dispatch ID and all `wand/…` IDs, then reads registered native defaults
and the existing canonical typed-SNBT overlay manifest. Native identity is stored
in each property's `tmt_native_item`; variant deduplication recursively sorts
compound keys and preserves tag types and list order. It does not interpret any
gameplay functions or infer support from `ownerCandidates`.

`drawSeven(random)` performs exactly seven independent family draws with replacement
and one uniform private-variant draw per selected family. Feed that list unchanged
to the existing generator / `LbcTmtAbilities.write`; the latter implements last
duplicate-family-wins. Ordinary/Academy versions share each existing wand family;
Tiny remains its own existing native family. Library spell IDs retain the direct
catalog's distinct supported spell identities. Pills are one family with all145
actual `LbcPills.definitions()` outcomes, including typed zero and the native tail
through144; acquisition `OUTPUT_COUNT=144` is not the native consumer domain.
Common wands begin in their existing native mode1; their real Shift input cycles
and persists private modes through `castSpell`. No extra initial-mode domain is
invented.

The explicit non-use domain includes the native melee owners, real-shot modifiers,
attribute/named/edible/lifecycle foods, accessories, armor traits, and the five
special tool actions already implemented by root's `LbcTmtTools`. It supplements
all17 Fragment selectors in each version and all44 supported mystical outcomes:
current passive1..8; legacy passive1..6 through `LbcBalancePassive`; current/legacy
reactive1..6, jump1..3 and incoming1..6. The count is8+6+12+6+12. The four mystical
event groups have separate family IDs; `type` remains private. Legacy admission
remains in the existing owner.

Each family exposes native owner, event and location metadata. Metadata describes
the required real event, not an assertion that every event adapter is complete.
For example consume properties wait for the actual food event; the actual TMT
carrier retains its food-count membership. Armor properties retain actual equipped
slot and set requirements. No held carrot is turned into an artificial food event.

Explicit findings retain source and reason for:

- Known legacy native owner gaps from the existing manifest, without inventing
  replacements or silently turning source effects into no-ops.
- Unknown native domains, including identity-only defaults with empty custom data.
- Ordinary tools' base-carrier mining/strip/till/path/ignite behavior is explicitly
  excluded from independent ability draws. Native stats/enchantments belong to the
  property generator; an item can still contribute its real custom melee/use/mining
  trait. Complete vanilla carrier simulation is not a TMT release requirement.
- All19 ordinary/IMBA typed-arrow identities plus infinite/homing ammo: native
  accepted-ammo and potion/color/tag projection still need their real adapters.
- Holy shield blocking still requires the real native blocking/component adapter.
  The totem component/criterion adapter is now implemented by root and admitted
  as described below; it still awaits the next native verification checkpoint.
- PhD certificate eligibility is explicitly excluded: the current native consumer
  is reusable `LbcPhdCrafting` recipe eligibility, with no independent inventory
  passive. Actual pill rewards remain sampled through their typed definitions.
- TMT recursive transports/acquisition and standalone vanilla service markers.

Integration hooks: lazily construct `new LbcTmtCatalog(useProfiles())` only after
holder-bound native defaults finalize, retain until reload, clear on reload, and
call `generate(random, catalog.drawSeven(random))` at real acquisition. Do not gate
the generated item's existence on an individual ability's runtime jammer policy.
Inspect `catalog.domains().findings()` and actual native owner adapters before
enabling public acquisition or claiming full coverage. Findings deliberately
remain pending; blanket exclusion of unknown identities would hide real abilities.

Evidence inspected directly: `LbcTmtUseProfiles`, `LbcWands`, `LbcTmtAbilities`,
`LbcPendingSwords`, `LbcClassicMelee`, `LbcPendingCombat`, `LbcChargedSwords`,
`LbcPills`, `LbcNamedFoodItems`, `LbcFoodLifecycle`, `LbcAttributeFood`,
`LbcEdibleSpells`, `LbcLibrarySpells`, `LbcMagic`, `LbcBalancePassive`,
`LbcReactiveArtifacts`, `LbcIncomingArtifacts`, `LbcJumpArtifacts`, `LbcTotems`,
`LbcHolyShield`, `LbcNativeToolItems`, `LbcImbaNativeItems`, `LbcQuiver`,
`LbcInfiniteAmmo`, and the actual armor/accessory flag consumers.

Verification here: source/symbol consistency and whitespace only; no build/native
test was authorized for this delegated sampler task. Root must compile and exercise
the integrated sampler before relying on counts or public acquisition.

## Diagnostic closure pass

Root's first integrated native checkpoint `cb3f24db` passed generation/codecs and
reported613 families/1203 variants before this extension. The diagnostic is
`build/verification/tmt-catalog-2026-11-07/catalog-domains.json`; the613 count is not
the updated catalog's count or a completeness claim.

The extension admits exactly23 audited trinket STRING identities (source indices
0..23 except18): eight shot, five offensive, four defensive, three periodic, two
wand-cast and one summon identity. Identity-only native defaults receive their
actual authored string selector; selectors stay private and families are distinct.
`LbcTrinkets` already reads private identities. `LbcReactiveTrinkets` needs the same
private identity union in its own counts/matches to compose those reactions;
root owns that adapter correction. No arbitrary `trinket` string is admitted.

Kara's three registered identities and their original vanilla slab acquisition
aliases form one melee family with three native variants. The vanilla aliases
resolve to the corresponding native identity instead of adding duplicate variants.
The real `LbcKaraSlabs` damage consumer has no oxidation-dependent effect branch.

Current `reactive`/`technomk2` traits map to `LbcTechnoArmor`/`LbcTechnoFlight`, and
`jumpboost` maps to `LbcTraveller`, retaining their actual equipped slots/set
requirements. Native legacy `jumpboost_old` and `magichat_old` map to the conditional
`LbcBalancePassive` owner. `multiplying`/`randchanted` flags alone are excluded:
current native readers classify reforge recipes; actual benefits are attribute
and enchantment components covered by the separate property generator.

The bolt recipe material, unparameterized PhD-pill default, developer-menu item and
generated consumed-block representation have explicit service dispositions.
Academy tokens remain pending meaningful loose-item structure placement through
`LbcAcademy`, with their DROP/LOOSE_ITEM contract named. They are not blanket-excluded
as inert currency or silently treated as held-use abilities. Genuine ammo/Holy-shield
prerequisites and other unresolved custom domains remain visible in findings.

Devil, Infinite and Broken Totem are now admitted as three real DEATH families,
following root's `LbcTmtTotems.apply/used` integration. Devil/Infinite retain the
source `transformable`+`totem` prerequisites; Broken keeps its own `broken_totem`
payload. Each retains its actual `tmt_native_item`. Infinite requires the canonical
`skywars2:infinite_totem` identity, since the native default Infinity is meaningful;
a standalone `totem` category on another carrier remains pending. Root applies
native DEATH_PROTECTION at generation, dispatches private properties from the real
used-totem criterion, preserves the property's Infinity floor and refunds the outer
TMT snapshot. This admits the implemented native contract and does not assert that
its new compilation/native tests have passed yet.
