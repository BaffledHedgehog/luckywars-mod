# Awakened acid acquisition and presentation audit

Read-only source audit, 2026-10-24. Java/build/resources and author resource pack were not modified. Native registration/runtime is audited separately by the parent agent.

## Acquisition

Legacy craft is one regular acid donor (`stacid:1`) plus one caustic donor (`acidwand:1`), producing one `lbc:tuntijaa_acid_acid` loot item. Both ground proximity (0.001..1 block) and player 2x2 crafting slots support it: [ground dispatch](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft_ground.mcfunction:15), [inventory dispatch](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/craft/inventory_craft_check.mcfunction:15), [ground result/consumption](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sauvojen_tuntija/crafted_acidacid.mcfunction), [inventory result/consumption](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/sauvojen_tuntija/crafted_acidacid_inv.mcfunction). Inventory legacy implementation reduces every matching occupied donor slot; native recipe should represent the intended one-plus-one combination rather than duplicate this incidental multi-slot consumption.

No native awakened-acid recipe/registration was found in current Java at audit time; `LbcCraftingRecipe` ACID is the tiny-to-regular acid recipe. Existing Java contains regular acid and caustic registrations; source loot is already copied under `src/main/resources/data/lbc/loot_table/tuntijaa_acid_acid.json`.

## Base presentation

[Source loot](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/tuntijaa_acid_acid.json): one `minecraft:carrot_on_a_stick`; model `lbc:acidacidwand`; tooltip `lbc:tier_s`; custom data `{transformable:1,staff:1,magicwand:1,acidacid_wand:1}`. Name `acidacid_wand`, dark_green, italic=false. Four lore rows, with every component italic=false:

1. `type` then `staff`, both aqua.
2. Score `acidacid_wand_mana_use` / `lbc.math`, then `mana_on_activate`, both aqua.
3. `acidacid_wand.lore.1`, white.
4. `acidacid_wand.lore.2`, white.

[Config](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/config.mcfunction:85) assigns 1450 mana. Preserve score-derived number and exact source formatting.

## Academy

[Initial Academy interaction](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_2.mcfunction) marks held item `upgradable:1` and gives yellow `upgrade_condition_acidacid`. [Cast condition](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/awaken_tuntija_wands/acidacid/click2.mcfunction:6) requires caster `lbc:on_fire` and selected mainhand `upgradable:1`; shared `can_upgrade`/`can_upgrade_book` marks availability. Thus condition means cast while burning after Academy consultation; generic availability is then redeemed at Academy.

[Upgrade](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_upgrade.mcfunction) sets `upgraded:1`, `mode:1`, appends [green italic=false `upgraded_item` lore](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/add_upgrade_desc.json), and calls [acid-specific upgrade](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/acidacid_wand.mcfunction). It emits yellow `upgrade_succesful_acidacid` and changes only tooltip tier to [lbc:tier_ss](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/tiers/ss.json). Name/model/base lore remain the same. Important: custom-data `tier` used for ignition modes is separate from S/SS tooltip rarity; shared upgrade sets mode but does not initialize numeric tier.

## Author RP availability

All required keys already exist in both [RU](<C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8/assets/minecraft/lang/ru_ru.json:1755>) and [EN](<C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8/assets/minecraft/lang/en_us.json:1755>): `acidacid_wand`, `.lore.1`, `.lore.2`, `type`, `staff`, `mana_on_activate`, `upgrade_condition_acidacid`, `upgrade_succesful_acidacid`, `upgraded_item`. No duplicate keys/template additions needed.

Exact name RU: `Нешак Хераль Шель Халитаа Хааруа`; EN: `Neshak Heral Shel Halitaa Haarua`.
Exact lore RU: `Стреляет огромным шаром кислотно-ядовитого газа.` / `Депортация из Израиля теперь неизбежна.`
Exact lore EN: `Shoots a huge ball of acidic poison gas.` / `Deportation from Israel is now inevitable.`
The condition text says to ignite yourself before casting; success text describes four shooting modes with Shift+RMB, and ignition toggling with Shift+RMB while looking straight down.

[Item definition](<C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8/assets/lbc/items/acidacidwand.json>) exists and points to `lbc:item/acidacidwand`; its model uses vanilla handheld parent and `minecraft:item/acidacidwand`. Referenced PNG exists. Both S and SS tooltip background/frame PNGs exist under `assets/lbc/textures/gui/sprites/tooltip/`. Filesystem checks confirm resource availability, not graphical-client visual equivalence.
