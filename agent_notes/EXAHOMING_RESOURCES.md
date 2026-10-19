# Exahoming: ресурсы и точное соответствие исходнику

Проверено 18 октября 2026. Legacy datapack и настоящий RP использованы только для чтения. Gradle и сервер не запускались. Языки и resourcepack_template не менялись.

## IDs и источники

| Source marker | Native ID | Loot | Academy ID / loot |
| --- | --- | --- | --- |
| `truefallingstars:1` | `skywars2:falling_stars_wand` | `lbc:ported/falling_stars_wand` | `skywars2:academy_falling_stars_wand` / `lbc:ported/academy_falling_stars_wand` |
| `stair:1` | существующий `skywars2:air_wand` | существующий `lbc:ported/air_wand` | существующий `skywars2:academy_air_wand` / `lbc:ported/academy_air_wand` |
| `exahoming_wand:1` | `skywars2:exahoming_wand` | `lbc:ported/exahoming_wand` | `skywars2:academy_exahoming_wand` / `lbc:ported/academy_exahoming_wand` |

Legacy root: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc`.
Donor Falling Stars выбран из `loot_table/transformables_staff.json` одновременно по имени `falling_stars_wand` и маркеру `truefallingstars:1`. Обычный `fallingstars:1` относится к Blaze и здесь не выбран.
Air находится в `loot_table/sauvojen_tuntija_air.json`; он уже перенесён и повторно не генерировался.
Результат взят из `loot_table/tuntijaa_air_homing.json` по имени и маркеру `exahoming_wand`.
Крафт `truefallingstars + stair` подтверждают `function/other/craft_ground.mcfunction:14`, `function/other/craft/inventory_craft_check.mcfunction:14` и `function/other/sauvojen_tuntija/crafted_exahoming_inv.mcfunction:5`.

## Генерация и source equality

`tools/import_lbc_exahoming.py` переиспользует существующий `import_wand_family`, `migrate_loot_schema`, `build_single_entry_table`, `write_table` без их изменения. Запуск: `python tools/import_lbc_exahoming.py`.

Проверено структурным равенством JSON для обоих ordinary payload: исходная таблица после штатной миграции, выбранная единственная entry и единственное изменение item ID полностью совпадают с созданной таблицей. Имена, аргументы, порядок/стили lore, item_model и tooltip не редактировались вручную.

Academy структурно равны тем же source payload с ID Academy, полями принятого общего helper (`upgradable:0,upgrade_available:1,upgraded:1,mode:1`) и исходными modifiers `add_upgrade_desc`, `tiers/a` для Falling Stars или `tiers/x` для Exahoming. Это соответствие source + штатные helper-поля; оно не заявляет буквальное равенство custom_data финальному NBT исходного живого предмета. Источник `function/other/magic_academy/interacted_upgrade.mcfunction:1` добавляет `upgraded=1`, `mode=1`, затем `add_upgrade_desc`; соответствующие `upgrade/truefallingstars.mcfunction:2`, `upgrade/stair.mcfunction:2`, `upgrade/exahoming_wand.mcfunction:2` назначают tiers/a, tiers/s, tiers/x.

## Config и режимы

`function/config.mcfunction:30`: `falling_stars_wand_mana_use=4000`.
`function/config.mcfunction:84`: `exahoming_wand_mana_use=2000`.
Оба исходных scoreboard-компонента lore сохранены с objective `lbc.math`.

`function/workingitems/awaken_tuntija_wands/exahoming_click_upgraded_left.mcfunction:1` задаёт четыре режима и исходные ключи:
1. `mode_default`
2. `mode_sisyfus` (именно такое написание)
3. `mode_destiny`
4. `mode_curse`

## Проверка настоящего RP

RP: `C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8`.
Существуют `assets/lbc/items/starwand.json`, `assets/lbc/items/homing.json`; модели ordinary и Academy остаются соответственно `lbc:starwand` и `lbc:homing`.
Academy skins задаются существующими tooltip styles `lbc:tier_a` и `lbc:tier_x` (Air — `lbc:tier_s`). В RP найдены соответствующие `textures/gui/sprites/tooltip/tier_{a,s,x}_{background,frame}.png` и metadata.

Все 17 translate keys сгенерированных payload найдены в обоих языках RP `ru_ru` и `en_us`: `cast_count`, `cast_distance`, `craft_with_fevgalea`, `exahoming_wand`, `exahoming_wand.lore.1`, `exahoming_wand.lore.2`, `explosive_damage`, `falling_stars_wand`, `falling_stars_wand.lore.1` … `.4`, `mana_on_activate`, `mazoku.lore`, `staff`, `type`, `upgraded_item`. Также найдены все четыре mode keys и `upgrade_condition_truefallingstars`, `upgrade_succesful_truefallingstars`, `upgrade_condition_exahoming`, `upgrade_succesful_exahoming`. Новые языковые записи не нужны.
