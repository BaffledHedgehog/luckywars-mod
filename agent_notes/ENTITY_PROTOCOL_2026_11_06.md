# Login entity protocol hotfix — qualified and installed, 6 November 2026

## Report and exact cause

The author's vanilla26.3 client disconnected during login with:
`Invalid entity data item type for field 12 on entity Tadpole`, integer versus
`Vector3f(0.5,0.5,0.5)`. Client evidence is the local disconnect report
`debug/disconnect-2026-11-06_21.18.01-client.txt` in the author's26.3(1) instance;
the21.17.58 report has the same failure. The main server had qualified pre-TMT
`b8d1066e` installed. This was not caused by the unreleased TMT component.

`LbcClouds.register()` passed vanilla `EntityTypes.AREA_EFFECT_CLOUD` to
`PolymerEntityUtils.registerType`. In Polymer0.18.2+26.3 that API marks the registry
entry server-only. RSM partitions those entries after vanilla entries on freeze,
moving every subsequent vanilla entity's raw network ID down by one. A native
text display consequently went onto the wire as134, which a vanilla client
correctly interprets as tadpole. The display's scale metadata then failed on
the tadpole's integer field12.

Primary sources inspected locally: Polymer Core `PolymerEntityUtils`, RSM
`MappedRegistryMixin`, Polymer `ClientboundAddEntityPacketMixin`, and Mojang26.3
`EntityTypes`, `EntityTypeIds` and `ClientboundAddEntityPacket`. No guessed
client protocol or third-party entity-ID table is used.

## Change and independent regression

Only runtime change: register the existing per-instance cloud `PolymerEntity`
constructor through `registerPolymerEntityConstructor`. Do not register a
vanilla type as a server-only type. The existing ChemicalCloudVisualMixin still
maps chemical clouds to marker and ordinary clouds to area_effect_cloud.
Custom acid projectile types retain their proper server-only registration.
No resource-pack, world conversion or entity simulation changes are needed.

`EntityProtocolChecks`, exposed through the existing operator check dispatcher,
encodes native spawn packets in an actual scheduled connection's PacketContext.
It resolves raw IDs using the independent original vanilla order, then applies
encoded/decoded metadata to that canonical entity constructor. This avoids the
false positive of decoding through the server's own already-shifted registry.

`skywars2/vanilla_entity_order_26_3.json` contains all161 vanilla IDs in original
registration order and SHA256s of both original Mojang source files. Text, item
and block displays are tested; every vanilla raw ID and server-only flag is
checked. Ordinary and chemical cloud projections are checked separately.
This oracle must be regenerated from primary sources when Minecraft changes.
The source resource is read only by the explicit test, not every game tick.

## Qualification and installed artifacts

Stage: `build/verification/entity-protocol-2026-11-06/`.

* Old-code witness `91de8c4f`: actual native wire text_display134→tadpole,
  exact original field12 metadata exception reproduced. Expected rejection;
  normal fixture shutdown and exact restoration succeeded.
* Final test `83b0ea279d597905efd760cbacca753c5fc7ad824909533c4fb53bfdaf2abf78`.
* Final production `b90ffea1c1b0f111075fd04f082bed75d3a59b9311c6e123b3fa4f4888ddc77a`.
* Three actual mode1 matches pass on the final archive: `entity-protocol`,
  `fire-wand`, `brbad`. Text135/item73/block15 resolve correctly;161 vanilla
  IDs remain unchanged; existing wand and chemical cloud behavior passes.
* Normal child shutdown, exact fixture and auxiliary restoration; retained
  baseline linkage audit:3442 classes,27 freshly overlaid classes,0 unresolved
  removed-member references. Three fresh Java families and one test resource.
*10306 other source inputs and282 protected author paths unchanged.
* Both build/libs JARs published. Main test JAR installed after the author
  normally stopped it at21:31, with a verified `b8d1066e` rollback copy.
  `main-install.json` records `installed`, `restarted:false`.

The main server was never launched, stopped or restarted by this work. Only the
isolated fixture was run, at2GiB heap after the512MiB single-worker compiler
exited and free memory was checked. Author RP and legacy server stayed read-only.
The inherited fake-player fixture logs Polymer's missing chunk PacketContext
warning; it is separate from the reproduced entity-ID error. Physical client
reconnection/rendering and peak TPS remain unverified; do not claim otherwise.

## Required TMT carry-forward after this hotfix

This is a narrow fix on qualified `b8d1066e`/`256dc67b`, not a release of pending
TMT. The isolated compiler built the qualified GameChecks source plus the new
protocol case, excluding exactly the two unreleased TMT cases. Its equality to
current source minus those cases is sealed in `variant-proof.json`.
No TMT classes or property resource entered either installed artifact.

**Future TMT packaging must retain this cloud fix, protocol check and oracle.**
The older internal `032c8fba`/`1783ca96` archives still contain the faulty cloud
registration. Do not publish them or treat them as a fully current base.
Start with this qualified hotfix and explicitly carry forward the six committed
TMT families/cases and `skywars2/tmt_properties.json` described in
[TMT_WANDS_2026_11_06.md](TMT_WANDS_2026_11_06.md), or overlay all hotfix families
and resource onto the internal component before further qualification.
Do not infer committed, unreleased carry-forward solely from git status.
All full-TMT, pool, mode2/4, skill2, Academy and general rewind gates are unchanged.
