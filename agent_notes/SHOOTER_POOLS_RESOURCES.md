# Shooter pool resource/native-ID audit - 21 October 2026

Read-only audit; no code, loot/resource, RP or template edits. Source: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table`. Current RP used by import helpers: `C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3/minecraft/resourcepacks/all_in_one_1.21.8`. RESOURCE_PACK.md historically links 26.3(1); this audit deliberately follows the requested/current helper 26.3 location.

## Graph and exact replacement contract

All pools below roll once. `transformables_shooter_all` entries, in order: shooter subtable weight 7; stalin 1; Magnum item 1; tuntijaa_terra_razor 1; Govnomet item 1; helicopter_boss 1; slr 1. Preserve table nesting and each reference weight. Do not flatten these distributions.

| Source entry (zero based) | Weight | Qualified payload | Native ID |
| --- | ---: | --- | --- |
| transformables_shooter[0] Phantom Knives | 10 | ported/phantom_knives | skywars2:phantom_knives |
| transformables_shooter[1] Mobgun | 10 | ported/mobgun | skywars2:mobgun |
| transformables_shooter[2] conditional Mobgun | 10000 | same presentation as qualified Mobgun | skywars2:mobgun |
| transformables_shooter[3] KFC | 10 | ported/kfc_gun | skywars2:kfc_gun |
| transformables_shooter[4] razorpine_with_leaves reference | 10 | ported/razorpine (complete two-pool table) | skywars2:razorpine + vanilla wheat_seeds |
| transformables_shooter[5] Attack Helicopter | 10 | ported/attack_helicopter | skywars2:attack_helicopter |
| transformables_shooter[6] Laser Minigun | 10 | ported/laser_minigun | skywars2:laser_minigun |
| transformables_shooter[7] Woodificator | 10 | ported/woodificator | skywars2:woodificator |
| stalin[0] AK47 | 10 | ported/ak47 | skywars2:ak47 |
| transformables_shooter_all[2] Magnum | 1 | ported/magnum | skywars2:magnum |
| tuntijaa_terra_razor[0] Terra Razor | 1 | ported/terra_razorpine | skywars2:terra_razorpine |
| transformables_shooter_all[4] Govnomet | 1 | ported/govnomet | skywars2:govnomet |
| helicopter_boss[0] | 10 | ported/helicopter_boss | skywars2:boss_helicopter |
| slr[0] | 10 | ported/slr | skywars2:slr |

`razorpine_with_leaves` pool 0 weapon weight 10, pool 1 vanilla wheat_seeds weight 1, with its original modifier/reference; ported/razorpine retains both pools. Never use a single weapon entry to replace the entire two-pool helper.

The 10000-weight Mobgun entry has its own legacy value_check: fixed score-holder final_luck_modifier, objective lbc.math, range 7, scale 1. Preserve that condition and the ordinary 10-weight entry independently. Qualified standalone Mobgun intentionally lacks this pool-selection condition. Correct schema migration of the condition must preserve exact equality to 7.

## Variant differences

Comparison used `import_lbc_firearms.migrated` and current ported payloads; native ID and entry weight excluded only at the top level. All names, lore, component structure and styles match exactly except the qualified author changes below:

- Mobgun: source recharge literal `3.5 ` becomes qualified `2.5 ` at modifier[3].lore[1][0].text. Both weighted source entries otherwise have identical visual presentation. Conditional selection is a pool concern, not a reason to create another native item.
- Laser Minigun: source damage literal `4 ` becomes qualified `2.5 `. Qualified payload additionally contains four movement/jump attribute modifiers, one per relevant attribute/hand. Source's two mainhand attack attributes remain. Preserve the qualified payload.
- All other listed weapon payloads are source-equal apart from native ID and extraction weight. In particular, do not substitute finite AK47 for the infinite AK47 yielded by stalin, or alter Boss's source name/presentation because its native ID says boss_helicopter.

No missing model/translation visual variant was found among the actual source graph leaves. All 13 distinct item definitions/models/textures resolve through `import_lbc_firearms.verify_model`; every leaf translation key exists in RU, EN and RPR. Models checked: phantomknifes, mobgun, kfcgun, razorpine, attack_helicopter, laser_minigun/1, woodificator, ak47, magnum, terra_razorpine, govnomet, helicopter_boss, super_rifle (all lbc namespace). Any requested additional missing variant must be distinguished from these closed source leaves; the graph has no finite AK branch or alternate named SLR leaf.

## Reuse helpers

Use import_lbc_firearms.migrated, import_lbc_wands.build_single_entry_table, and existing selected payloads from import_lbc_mobgun, import_lbc_shooter_pair, import_lbc_next_shooters, import_lbc_slr, import_lbc_weapons/import_razorpine_resource. The source pools themselves still require promotion to native IDs; standalone payload existence does not qualify pool acquisition. Do not rerun write-enabled import main functions during this audit.
