# Combined Sauvojen presentation and acquisition audit ? 26 October 2026

Source: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`.
Author resource pack read only: `C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8`.

## Exact ordinary leaf

`loot_table/sauvojen_tuntija_wand.json` is a single carrot-on-a-stick item, weight1/count1. The new `ported/sauvojen_wand.json` is its complete payload after the existing Minecraft26.3 loot schema migration, with only entry identity changed to `skywars2:sauvojen_wand`.

Model `lbc:sauvojen_wand`, tooltip `lbc:tier_a`. Original set_name array uses `sauvojen_wand`, light_purple, italic=false; no item_name override. Four lore rows retain original component structures, ordering and styles: aqua type+staff; aqua score sauvojen_wand_mana_use/lbc.math+mana_on_activate; white sauvojen_wand.lore.1; white sauvojen_wand.lore.2. Custom data remains `{craftusable:1,transformable:1,staff:1,magicwand:1,sauvojen_wand:1}`. Config line115 sets cost2500; no independent purchase-price field occurs in this leaf.

## Existing resource evidence

All ordinary name/lore/type/staff/mana keys and both advancement and Academy condition/success keys exist in both author assets/minecraft/lang/ru_ru.json and en_us.json. Existing identifiers are reused without translation duplication. All ten item definitions, backing models and direct PNG texture references exist: sauvojen_wand; st/ruby, st/terra, st/electro, st/air, st/acid, st/teleport, st/stone, st/fire, st/blind. No author RP changes or templates are necessary. Client rendered pixels have not been observed.

## Advancement

Already packaged `data/lbc/advancement/true_advancements/staffs/sauvojen_tuntija_wand.json` has nine impossible criteria wand1..wand9, no explicit requirements override (all criteria required), parent danmaku_staff, original icon/model and title/description keys. Source cast_2 maps outcomes1..9 to ruby, terra, electro, air, acid, teleport, stone, fire, blind respectively, granting the matching criterion to the caster.

## Acquisition boundary

Source `transformables_staff_all` references ordinary combined leaf weight1, but also unrelated random/tiny/Gaycaster/Halloween entries. Source loliluckyblock references it weight10. Neither complete enclosing pool is safe to enable merely because this leaf exists; retain a narrow mapped reference using original route weight when integrating with an already supported parent. No enclosing pool, live override or registration is changed by this audit.

Academy source can_upgrade/sauvojen_wand emits only upgrade_condition_sauvojen_wand; meditation/tick_ok authorizes can_upgrade after its specific challenge. upgrade/sauvojen_wand emits upgrade_succesful_sauvojen_wand and applies tiers/x. Generic Academy framework metadata is separate from that exact item-specific modifier. An ordinary-presentation leaf is not proof of completed meditation/Academy behavior. No Academy leaf was created and no Academy route enabled here.

Verification: JSON parse and existing schema migration; author RU/EN key presence; ten item/model/direct-texture checks. No generators, builds, servers, commits, author-resource writes or external grimoire override edits.
