# Looper skill — 2 November 2026

This stage completed skill3 while reactive skills6/10/19 awaited author decisions.
Those answers and the subsequent completed qualification are now recorded in
[the reactive skills contract](REACTIVE_SKILLS_PORT_2026_11_02.md). The earlier
audit under `build/verification/reactive-skills-2026-11-02/` is historical.

## Source and access

Legacy root: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`.
The original selection is page10000, slot2, gated by
`lbc:true_advancements/accesories/mystical_artifact`. Existing GUI translations,
models and `mana_usage_2` are reused. No resource-pack changes are necessary.

Existing native artifact/fragment recipient descriptions and Infinity outcome
explanations are retained. Missing grimoire knowledge now projects the original
second lore line as a light-purple, nonitalic `mana_usage_2` plus the exact signed
decimal `custom_data.mana`. This applies to registered and original vanilla
carriers bearing `unresolved_grimoire:1`. The authoritative item, its flag,
count and other components remain unchanged; other recipients keep their source
lore. Inventory, dropped-item and equipment refresh includes these grimoires.
The existing knowledge-hiding challenge continues to take precedence.

## One-time ground announcement

`items_tick.mcfunction` calls `items_tick_tag` only before the source `tagged`
marker is set. Its `items_tick_tag_2`/`items_tick_tag_3` chain announces an
`imba:1` item once. It is not a continuous particle beacon or sound loop.

A native ItemEntity tick hook keeps that once-only behavior without scanning
world entities or player inventories. A persisted private entity tag survives
save/load; legacy `tagged` items do not reannounce. Actual time stop defers the
first announcement. A disabled global skill setting does not queue a later
replay. Only same-level Looper recipients receive the original 200 pink dust
particles (scale4, Y+20, vertical spread40) and aggressive-bee MASTER sound
(volume1, pitch1, minVolume1). Far sound recipients use the native command's
two-block relative-position rule. Other viewers receive neither packet.

## Verification

Test-server artifact `7757f568`, SHA-256
`7757f56898daf2e2fddbdde5c43c4bd3c2156bfdedd8d51a292a2e1d75c68979`;
production `057ba9b6`, SHA-256
`057ba9b677023374463f4c66fd7a9393d56926659f056e1fd4d351c103ae3fb2`.
The production/test split retains byte-identical shared entries and excludes
all eight map-authoring implementation/check classes from production.

Two final native groups passed in two actual mode1 matches (32 seconds):
`looper` and `inventory-skills`. Coverage includes every registered unresolved
grimoire, vanilla carriers, signed integer extremes, exact source line indexing
with duplicate/all-identical lore, native Polymer round-trips and two recipient
views, knowledge-hiding challenge precedence, original unlock gates and actual
GUI callbacks. Native first/second item ticks, exact particles/sound, far
minVolume placement, native save/load, legacy tags, Time Stop and disabled-first-
tick behavior pass. Earlier artifact/fragment/Infinity consumers are retained.

9796 frozen inputs, 2481 compiled classes and 7353 processed resources match
the selected artifact exactly. Loom's additional Fabric dependency metadata is
verified separately. All 236 protected author paths and 31 live grimoire
overrides remain unchanged. Children stopped normally, exact player/config/
OP/level/ticket snapshots were restored and no fixture locks remain; port25577
only. No main stop/restart, remote push, physical-client pixels/audio or peak-TPS
claim. Receipts live under `build/verification/looper-2026-11-02/`.

Retained diagnostics distinguish the initial reviewed duplicate-line issue from
the following native timeout: the synchronous test returned without a PASS
marker. The latter child shut down/restored normally. Only the test completion
log differs between `cc8eb088` and the final artifact; production behavior and
authorization were not weakened. The final run verifies the complete callback
and successful cleanup before reporting PASS.

The normal-stop installer confirms `7757f568` is installed in the main server,
with `7968842a` retained for rollback and `restarted:false`. No RP update is needed.
