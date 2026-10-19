# Lightning Bolts / Boltbolt resources — 19 October 2026

Importer: [import_lbc_boltbolt.py](../tools/import_lbc_boltbolt.py), using the existing `import_lbc_wands.import_wand_family` helper. It writes only the four new ported loot tables; the already ported Teleport family is not regenerated.

Read-only source root: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc`.
Read-only resource pack: `C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8`.

| Family | Source | Registered IDs | Marker | Model | Academy tier |
|---|---|---|---|---|---|
| Donor | `loot_table/transformables_staff.json` | `skywars2:lightning_bolts_wand`, `skywars2:academy_lightning_bolts_wand` | `lightning_bolts:1` | `lbc:lightningboltwand` | `s` |
| Result | `loot_table/tuntijaa_bolts_bolts.json` | `skywars2:boltbolt_wand`, `skywars2:academy_boltbolt_wand` | `boltbolt_wand:1` | `lbc:boltbolt_wand` | `ss` |

Source `function/config.mcfunction`: `lightning_bolt_wand_mana_use=3000`, `boltbolt_wand_mana_use=1830`. Ordinary tooltip style is `lbc:tier_a` for both. Academy appends the exact migrated source `item_modifier/add_upgrade_desc.json` and `item_modifier/tiers/{s,ss}.json`, preserving the source lore and tooltip changes. Academy custom-data fields are `upgradable:0,upgrade_available:1,upgraded:1,mode:1`, following the shared family helper.

Source `function/other/magic_academy/interacted_upgrade.mcfunction` sets `upgraded=1`, `mode=1`, then applies `add_upgrade_desc`; family upgrade functions apply `tiers/s` and `tiers/ss`. Boltbolt `click_upgraded_pre.mcfunction` maps modes 1/default, 2/wind racer, 3/snow meteor, 4/watering. Existing translation keys are `mode_default`, `mode_wind_racer`, `mode_snow_meteor`, `mode_watering`. The matching upgrade keys are `upgrade_condition_lightning_bolt`, `upgrade_succesful_lightning_bolt`, `upgrade_condition_boltbolt`, `upgrade_succesful_boltbolt`.

## Verification

`python tools/import_lbc_boltbolt.py` completed. Independently rebuilt expected tables from migrated source entries and exact Academy modifiers; all four JSON trees compare equal, allowing only the registered item ID and helper Academy custom-data additions. Thus name/lore components, order, style, models, tooltip, weights and modifier payloads are preserved.

| File | SHA-256 |
|---|---|
| `lightning_bolts_wand.json` | `fff9413897ec24fd7c3e5c0344d16c92e98daa6848418cbff2a433c332e5a4e2` |
| `academy_lightning_bolts_wand.json` | `20e20ed49bd03d7183be8a75aa467c75de0b4334c5130d4733527d780b3148d3` |
| `boltbolt_wand.json` | `8332762c765d97669829d8790426e2b94f4e10e0f801d7fd2e331282ccec3409` |
| `academy_boltbolt_wand.json` | `d34e0841fda5bd115e3aff8e9e4d065b06a72f824ba61ce44685e1fed96a6ad1` |

All 21 referenced loot/Academy/mode translation keys exist in both resource-pack `assets/minecraft/lang/ru_ru.json` and `en_us.json`. Both `assets/lbc/items/lightningboltwand.json` and `boltbolt_wand.json` exist. No language keys or templates are added and the resource pack is untouched. Language SHA-256: RU `3e7b3f2c6aa2e9e223db99d52d4d1a9de9954bd546bad5ba3543b775c7dfb47b`, EN `d81685d3f4c65c833543ee9ece5f48904d01ba8b8c04e9a2dd2176dadeaa8782`.

These checks establish source payload equality and resource availability, not client visual appearance or runtime gameplay. No Gradle/server run or legacy writes were performed.
