# Multiplying and Randchanted armor — 28 October 2026

Source closure: [bounded source audit](CHAOS_STATIC_GEAR_SOURCE_2026_10_28.md).
Only the two static armor families are in this release. Chaos evidence in that
audit is next-family preparation, including unresolved author decisions and the
complete trap-table dependency; it is not implemented or qualified here.

## Complete source behavior

Multiplying consists of four golden armor leaves and `lbc:set_multiplying`, which
chooses one equal-weight slot. Preserve each original unique armor/toughness
modifier ID and ADD_MULTIPLIED_BASE operation: head2, chest4, legs3, feet1.
The vanilla attribute engine applies these to its actual base plus additive
modifiers, including other equipped items. Replacing default item modifiers is
part of the authored payload; do not invent a flat armor contribution, set-wide
multiplier, periodic ability, jammer gate or full-set loot wrapper.

Randchanted consists of four iron armor leaves and `lbc:set_randchanted`, likewise
choosing one equal-weight slot. Each original leaf uses vanilla
`enchant_with_levels`, uniform1..30. Keep the native enchanting algorithm,
compatibility, defaults, exact names/lore/tier/raw flags and ordinary armor
attributes. Do not substitute independently randomized enchantment integers or
invent a source Randchanted hoe. Original weapon/tool families remain separate.

All ten tables were already packaged, but excluded from the public ready loot
catalog. Expose exactly these now-qualified IDs. Existing normal slot and Ultra
family reforges already implement acquisition and remain unchanged. Preserve
the source Ultra Randchanted armor exclusions sword/pickaxe/bow/axe; shovel/hoe
are not additional exclusions. Synthetic flag controls in the fixture prove
that route boundary, not the existence or qualification of a Randchanted hoe.

Both original armor achievements require a native player-killed-player criterion
and all four actually equipped raw family flags. Multiplying retains parent
armor and original kit reward; Randchanted retains parent Multiplying and skill
reward. No invented inventory-only achievement trigger or custom reward engine.

All17 original translation keys, including achievement presentation, exist in
the read-only author RU/EN resource pack. Vanilla armor models/equipment and the
existing tier style remain. No RP/template changes are needed.

## Native verification

`StaticArmorChecks` compares all eight complete native leaf tables and both
roots against literal independently frozen original source payloads adapted only
for the26.3 codec keys. Each leaf also runs24 matching native seeds against the
original payload. Actual public rolls and consumed normal/Ultra Work exercise
acquisition. Equipment checks cover partial/full/mixed sets, additive neighbors,
carried/offhand exclusions, idempotence and unequip. Actual native PvP criteria
verify equipped-slot requirements. Existing shared production behavior is
reused; no parallel attribute/enchant/advancement engine is added.

Artifact `d9087f0a07a321a5a9e57cac7f6f58a2d4ca6a86da9829a52623799f0342e087`
passed38 recorded results in two actual mode1 matches (135.47 seconds). Its
one-match diagnostic also passed, explicitly not used as release qualification.
Exact564 sources,1599 classes,3257 resources and build.gradle match the archived
JAR. All198 pre-existing author paths and31 live override files are protected
by fresh baseline hashes. The31 count includes pack metadata/documentation as
well as the29 loot JSON files; this patch does not add or modify an override.
No gameplay attribute/enchant/reward implementation or leaf payload was rewritten:
the actual change exposes the two fully verified original families in the ready
catalog and adds their native regression suite.

All static source payload/reforge/equipment/criterion checks pass in each match,
alongside Techno/MK2, Mage/Nyauwu, Flesh, Berserk, Glass, Nanobots, ordinary and
Shulker armor, incoming artifacts, attributes, signed effects, effect layers,
shadow absorption and Time Stop. Native child stopped normally, exact fixture
snapshots restored, no check failures or crash. The same pre-suite Polymer
chunk-packet-context diagnostic from initial Carpet-created bot delivery remains;
its exact two messages are allowlisted in `release-audit.json` and already
documented by the preceding Techno proof. No error-free network-log claim.

Evidence and archived artifact are under `build/verification/static-armor/`:
`artifact-proof.json`, `native-results.json`, `native.log`, `release-audit.json`
and `qualification.json`. The stopped-only worker records actual installation
status in `main-install.json`, without forcing a main shutdown/restart. No
physical client, natural TPS or real socket reconnect claim.
