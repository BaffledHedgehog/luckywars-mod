# Danmaku resources ? READY ONLY, 25 October 2026

Only the two new native leaf tables and this note are authored here. No source server, author resource pack, translations, template, Java or build changes.

## Source choice and native leaves

The requested legacy `loot_table/danmaku_staff.json` does not exist in the checked source. The actual non-edible staff entry is in `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/transformables_staff.json`, entry selected by custom data `danmaku_staff:1`. The similarly named entry in `rofls.json` is an edible golden apple with `eat_danmaku:1`; it is not used.

`lbc:ported/danmaku_staff` produces `skywars2:danmaku_staff`. Source weight/quality are pool selection data, omitted in a deterministic leaf. Source functions become repository 26.3 `modifier` entries with `type` discriminators; item component attribute entries retain their own `type`. The original carrot-on-a-stick carrier is replaced by the native item; authored components remain: A tooltip, model `lbc:danmaku`, offhand movement speed modifier `genej656j6j6tk87ukukyeed`, amount -0.75, add_multiplied_base. No explicit durability, food or consumable component exists in this source staff entry.

Name is aqua/nonitalic `danmaku_wand`; nine original lore rows retain every style and order: aqua type+staff, aqua score `danmaku_staff_mana_use`/`lbc.math`+mana_on_activate, then seven white/nonitalic `danmaku_wand.lore.1` through `.7`. Source configured mana is 1000. Original custom data is exactly `{transformable:1,spellwithcooldown:1,staff:1,magicwand:1,danmaku_staff:1}`.

`lbc:ported/academy_danmaku_staff` produces `skywars2:academy_danmaku_staff`, preserves that presentation and source flags, adds `upgradable:0,upgrade_available:1,upgraded:1,mode:1`, appends the generic green/nonitalic `upgraded_item` row, and sets final tooltip `lbc:tier_s`. Source Academy upgrade uses `function/other/magic_academy/upgrade/danmaku_staff.mcfunction` and `item_modifier/add_upgrade_desc.json`; the readiness test is a bomb with score >=1500 while the used mainhand has upgradable1. Translation says ?over 1500?, but command includes 1500.

## Authored display presentation

Sources are `function/items/wands/danmaku_staff/shoot.mcfunction`, `shoot_homing.mcfunction`, `click_bomb.mcfunction`, `bomb_tick.mcfunction` and `damage.mcfunction` under the same legacy LBC data root.

Cards use item_display, repeating_command_block carrier, `lbc:card_1` (normal) or `lbc:card_2` (homing), fixed context, scale [0.5,0.5,0.5], left rotation [1,0,0,1], right identity, zero translation, view_range10, interpolation_duration1, teleport_duration1. Tags are slowraycast/player/danmaku/magic, plus homing for card2. Damage particles use the same card1 model.

Bomb is two red_stained_glass item displays, scales [1,1,1] and [-1,-1,-1], zero translation, identity rotations, no model override, view_range500000, zero shadows, start_interpolation0 and interpolation/teleport30. Both carry slowraycast/player/danmaku_bomb/magic/cant_be_homing; positive display additionally carries act. Lifetime advances by3; visual scales are +/-0.2 times Lifetime. This negative shell is authored geometry and must remain.

## Resource pack audit (read-only)

Pack: `C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8`.

Existing assets/lbc/items/{danmaku,card_1,card_2}.json resolve corresponding lbc:item models. Danmaku model parent is minecraft:item/handheld_rod; cards use minecraft:item/generated. All three minecraft:item textures exist. Existing minecraft language files en_us, ru_ru and rpr contain authored name, seven lore rows, upgrade condition/success and upgraded_item. No replacement translation or new resource fragment is needed.

HUD source `function/ui/cooldown.mcfunction` rows59?60 displays positive danmaku with four leading spaces, U+E133, one trailing space, score color #FFA1F2; positive danmaku_score has two leading spaces, U+E134, one trailing space, score color #D494FF. Both bitmap providers already exist in minecraft/font/default.json with ascent8,height9; textures font/icons/danmaku.png and danmaku_score.png exist.

Cast sound is vanilla minecraft:ui.cartography_table.take_result, master, volume0.5,pitch2. Bomb sound is minecraft:lbcsounds.bomb, master, volume2,pitch1; existing minecraft/sounds.json resolves lbcsounds/bomb and its OGG exists. No author pack writes occurred.

JSON parse and structural comparison preserve original modifiers exactly apart from required codec/carrier changes and explicit Academy additions. This audit establishes source/assets correspondence, not real client pixel validation or native runtime qualification.

## Integrated qualification

Build `04396ca4` passed 24 recorded suite results
in two actual mode1 matches (62.1 seconds), including all six actual-tick hand patterns, bombs, source cooldown bypass, Curse/electro bridges and actual match-stop cleanup.
Exact 346 Java sources / 952 classes /
2816 resources match the tested artifact.
Ready artifact: `build/verification/skywars2-danmaku-04396ca4.jar`. Evidence: `build/verification/danmaku-final/`.
The main JAR remains unchanged at2558af6b; READY ONLY is the user's existing choice.
No author RP edits or translation fragments are required. Native fixtures do not
qualify real client pixels, actual disk restart/disconnect or peak-load performance.
