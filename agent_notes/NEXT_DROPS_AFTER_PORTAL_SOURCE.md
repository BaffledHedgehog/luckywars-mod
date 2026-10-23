# After Random Portal - bounded source-only group

23 October 2026. Initial source audit, followed by the qualified batch below.
Normal/old Lucky Room have since been implemented and installed with Random
Portal. Only Pisa boats remain an unimplemented candidate from this group.
S = `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`.

## Initial group: normal/old Lucky Room plus normal Pisa boats

Three acquisition leaves, two mechanics; no complete raw pool should be enabled.
At audit time these flags had no native dispatch; the final status below
supersedes that finding for both Lucky Room variants.

- NORMAL Lucky Room: S `loot_table/barriersnottraps.json:402-445`,
  `{lbh:1,drop:1}`, count1 repeating command block, weight10 quality8,
  tier_c/drop_not_trap, yellow/nonitalic drop_lbc_room,
  one white/nonitalic trap_desc line.
- OLD Lucky Room: S `loot_table/legacy_luckyblock.json:2151-2169`,
  plain barrier/count1/empty components, identical flags.
- NORMAL Pisa boats: S `loot_table/barriersnottraps.json:699-742`,
  `{pizanski:1,drop:1}`, source presentation must be copied from that complete
  entry before implementation. S `function/drops/specialdrops.mcfunction:64`
  faces origin, forces pitch0, offsets Y+20, calls `drops/pizanski`.

Lucky Room dispatch is `function/drops/specialdrops.mcfunction:40`:
nearest nonspectator is executor and origin. `function/drops/luckyroom.mcfunction`
is exactly ten strict block writes: 5x5 yellow-concrete shell around Y-1..Y+1,
3x3 air cavity at Y..Y+1, four gold corner blocks at Y+2, four all-faces-false
red mushroom blocks at interior corners Y. No loot table, custom lucky block,
reward, scheduled continuation or extension command occurs. Its name must not
be mistaken for an actual lucky-block generator. Native strict block semantics,
role/target policy and terrain overlap still need qualification.

Pisa source `function/drops/pizanski.mcfunction:1-65` emits wood particles/sound,
61 native oak boats at local (0,n,-0.4n), n0..60, then one oak chest boat at
(0,61,-24.4). First and chest boats have visible pizansk translation names.
Chest slot14 contains one real native dirt relic (bullshit translation),
craftusable/govnoq flags, tier_b, Smite10/Bane10/Knockback1/Looting10,
and four actual native attributes: knockback resistance+1, attack damage+5,
armor+10, toughness+10 in main hand with exact source IDs.
The final source line upgrades slot14 for enabled skill9 to tier_a,
enchantments20/20/2/20 and attributes2/10/20/20 with separately authored IDs.
This reward is data-driven native equipment, not a missing custom ability:
current `LbcDamage.java` recognizes govnoq and `LbcCraftingRecipe.java` accepts
its donor flag. Implement the actual source components/skill branch and verify
survival chest access, melee attributes and crafting; no placeholder dirt.
Native boats supply their ordinary break drops; there is no custom death table
or later tick function referenced by the source tower.

## Explicitly excluded incomplete chains

Forest is not a one-line marker port: `drops/forest` -> raycasters_5t ->
`drops/forest_loop` includes reactive_shish custom loot. Magic Bean's scheduled
`magicbean_loop` -> bean -> beanend creates chest `lbc:lucky_items_1`, a broad
unclosed reward pool. Infinity creates a new infinity_rand instant item and
wand-cloud reward continuation. Do not add these based on short setup functions.
Normal Bob, normal TNT Drone, Greed and TMT remain excluded under the earlier
[NEXT_DROPS_AFTER_ACID_ANT_SOURCE.md](NEXT_DROPS_AFTER_ACID_ANT_SOURCE.md) audit.
No source server, RP, template or extra implementation writes were made here.

## Portal/Room batch status (23 October2026)

Normal/old Random Portal and normal/old Lucky Room are now implemented and
qualified in the combined [portal](RANDOM_PORTAL_PORT.md) /
[room](LUCKY_ROOM_PORT.md) batch. Earlier pending/source-only recommendations
for these four leaves are historical and superseded. Pisa boats remain a
source-only candidate; do not expose its reward until full native closure.
<!-- portal-batch-next-end -->

## Pisa status - 23 October2026

Pisa boats and both real relic variants are now implemented, qualified and
installed. Earlier source-only Pisa recommendations above are superseded.
See [native contract](PISA_BOATS_PORT.md) and [exact source audit](PISA_BOATS_SOURCE.md).
The explicitly excluded incomplete chains remain unavailable.
