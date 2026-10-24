# Lightning staff resources and acquisition audit — 24 October 2026

Read-only audit of ordinary/Academy `lightningwand` (Tesla Wand / Жезл Теслы). Runtime implementation is audited separately. No source, build, template or author resource-pack changes were made.

Source roots used below:

- `D` = `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data`.
- `RP` = `C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8` (read only).

## Exact ordinary definition

`D/lbc/loot_table/transformables_staff.json:1950–2085`: item carrot_on_a_stick, count 1, weight 10, quality 8, tooltip `lbc:tier_b`, model `lbc:lightningwand`; custom_name translate `lightning_wand`, aqua, italic=false. Custom data exactly `{craftusable:1,cyberset:1,transformable:1,staff:1,raycastspell:1,lightningwand:1}`.

Lore has eleven lines, in this order, all italic=false:

1. `type` + `staff` (both aqua).
2. literal `6 ` + `magic_damage` (aqua).
3. literal `4 ` + `cast_count` (aqua).
4. score `lightning_wand_mana_use` / `lbc.math` + `mana_on_activate` (aqua).
5. literal `90 ` + `cast_distance` (aqua).
6. `lightning_wand.lore.1` (white).
7. `lightning_wand.lore.2` (white).
8. `lightning_wand.lore.3` (white).
9. `lightning_wand.lore.4` (white).
10. `lightning_wand.lore.5` (white).
11. `lightning_wand.lore.6` (gold).

`D/lbc/function/config.mcfunction:37` sets the displayed mana score to 1200. Preserve literal spaces and component boundaries. The project's copied `src/main/resources/data/lbc/loot_table/transformables_staff.json:1950` contains the same presentation entry; it is not evidence of native item readiness.

**Source variant discrepancy:** `D/lbc/loot_table/source_of_energy.json:23–155` has the same flags/model/name/lore but **tier_c**, weight 1, in a pool with ten rolls. Do not silently use its tier as the canonical staff-reforge tier. `rofls.json:1918` also references this model; a model reference alone does not prove identical flags/acquisition.

## Academy presentation and unlock

`D/lbc/function/other/magic_academy/interacted_upgrade.mcfunction:1–5` copies the held stack, sets custom data `upgraded:1`, `mode:1`, restores it and applies `lbc:add_upgrade_desc`. That modifier (`D/lbc/item_modifier/add_upgrade_desc.json`) appends exactly one green italic=false translate `upgraded_item`; all eleven original lore lines and name/model survive. `upgrade/lightningwand.mcfunction:1–2` tells yellow `upgrade_succesful_lightningwand` and applies `lbc:tiers/a` (tooltip `lbc:tier_a`). There is no separate source Academy Tesla loot entry required to change its name/model.

`can_upgrade/lightningwand.mcfunction:1` tells yellow `upgrade_condition_lightningwand`. Author RP text:

- RU: `Нанесите урон минимум 10 мобам, попав в электропроводящие блоки этим жезлом!`
- EN: `Deal damage to at least 10 mobs by hitting conductive blocks with this wand!`
- RU success: `Теперь у жезла есть новые режимы (Переключение на Shift+ПКМ) - Мгновенные и цепные молнии!`
- EN success: `The wand now has new modes (Shift+RMB to switch) - Instant and chain lightning!`

References: `RP/assets/minecraft/lang/{ru_ru,en_us}.json:2015,2061`. `D/lbcr/function/raycast_electro_electroprovod_check_upgrade.mcfunction:3` tests tmp>=10, participant ID equality and inventory `upgradable:1,lightningwand:1`, then calls common Academy eligibility; the mismatch between wording “mobs” and broad runtime selectors requires the separate runtime audit. Do not infer persistent ten-hit accumulation from this resource text.

Academy Tesla achievement is **`lbc:true_advancements/legends/academy/tesla`**, not `/academy/lightning`: its JSON uses Tesla model and inventory custom data `{upgraded:1,transformable:1,staff:1,lightningwand:1}`. Ordinary Tesla achievement is `.../staffs/tesla`. Existing `/lightning` achievements concern the other lightning family.

## Acquisition and dependencies

- Staff transformation/reforge: `D/lbc/function/other/transform.mcfunction:11`, `reforge.mcfunction:10`, and `reforge_crafting_0.mcfunction` through `_3.mcfunction:10` draw `lbc:transformables_staff` for staff inputs. `transformables_staff_all.json:8` includes this ordinary staff pool; `ultra_reforge.mcfunction:10` and `other/eatables/book.mcfunction:12` draw the all pool. These are broad acquisition dependencies, not authorization to enable incomplete families.
- Source-of-energy event: `D/lbc/function/other/shishend/11.mcfunction:1` spawns `lbc:source_of_energy` at every nonspectator. Its Tesla variant differs in tier as noted above.
- Tesla is an **ingredient**, not the output, of the Sosueneruga combination. `D/lbc/function/other/craft/inventory_craft_check.mcfunction:11` requires `stelectro:1` plus `lightningwand:1`; `other/sauvojen_tuntija/crafted_ultraelectro_inv.mcfunction:1,5–12` spawns `lbc:tuntijaa_electro_electro` and decrements each matching crafting slot (source 2x2). Ground craft hook: `other/craft_ground.mcfunction:11`; `crafted_ultraelectro.mcfunction` consumes nearest Tesla carrot_on_a_stick and partner. Output table `tuntijaa_electro_electro.json:18,25,73` is `lbc:erecto`, translate `erecto_wand`, flag `erecto_wand:1`. Partner table `sauvojen_tuntija_electro.json:94` has `stelectro:1`.
- Do not label this as a Tesla creation recipe or reuse the Lightning Bolts recipe family.

## Existing author resources; template verdict

Both RU and EN author language files already contain `lightning_wand` and lore1–5 at lines 534–539, lore6 at 1737, condition/success at 2061/2015, and the shared type/staff/damage/count/mana/distance labels. RU name is `Жезл Теслы`; EN is `Tesla Wand`. Lore6 RU is `Можно объединить с жезлом Сосуэнеруги`, EN `Can be combined with Sosueneruga's staff`. Preserve the existing remaining exact translations directly from these files rather than rewriting them.

Verified asset chain exists: `RP/assets/lbc/items/lightningwand.json` -> model `lbc:item/lightningwand`; `RP/assets/lbc/models/item/lightningwand.json` -> parent `minecraft:item/handheld`, texture layer0 `minecraft:item/lightningwand`; actual `RP/assets/minecraft/textures/item/lightningwand.png` exists. No new model, name or lore translation is needed. **No template change is needed for source-identical Tesla presentation and Academy text.** A deliberate user-approved behavioral/text correction would require a separate narrowly scoped template change, not duplicated keys.

Existing native `src/main/resources/data/lbc/loot_table/ported/lightning_bolts_wand.json` instead uses item `skywars2:lightning_bolts_wand`, model `lbc:lightningboltwand`, translate `lightning_bolt_wand`, blue name and tier_a. It must remain distinct from Tesla; `boltbolt` is also outside this audit. Asset presence is not a graphical client verification.
