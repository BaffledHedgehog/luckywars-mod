# Bolt material / Infbolt resources — 19 October 2026

Importer: [import_lbc_infbolt.py](../tools/import_lbc_infbolt.py). Source root (read only): `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc`. Resource pack (read only): `C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8`.

| Item | Source loot table | Native ID | Source item | Model | Tooltip |
|---|---|---|---|---|---|
| Donor | `lightning_bolt_egg.json` | `skywars2:bolt_material` | `minecraft:panda_spawn_egg` | `lbc:lightningegg` | `lbc:tier_b` |
| Result | `tuntijaa_bolts_reforging.json` | `skywars2:infbolt_wand` | `minecraft:carrot_on_a_stick` | `lbc:infbolt` | `lbc:tier_x` |
| Academy result | same result + source Academy modifiers | `skywars2:academy_infbolt_wand` | `minecraft:carrot_on_a_stick` | `lbc:infbolt` | `lbc:tier_x` |

Donor preserves count 8, `minecraft:entity_data={id:"minecraft:lightning_bolt"}` and `{craftusable:1,bolt_material:1}`. Its seven lore lines and uncolored, nonitalic name are source payloads. `source_of_energy.json` contains the same donor presentation and count, with selection weight 10 / quality 5. The imba table has an explicitly different donor carrying additional `lightning_bolt`, `raycastspell`, `imba` markers; it is not the ordinary donor. No donor Academy variant was found or invented.

Result preserves count 1, aqua nonitalic name, six white nonitalic lore lines and `{raycastspell:1,infbolt_wand:1}`. Ground and inventory combination are explicitly Teleport (`stteleport:1`) + donor (`bolt_material:1`), in `function/other/craft_ground.mcfunction`, `function/other/craft/inventory_craft_check.mcfunction` and `function/other/sauvojen_tuntija/crafted_ultrareforge{,_inv}.mcfunction`.

Academy `function/other/magic_academy/interacted_upgrade.mcfunction` writes `upgraded:1,mode:1` and applies `item_modifier/add_upgrade_desc.json`; `upgrade/infbolt_wand.mcfunction` applies `item_modifier/tiers/x.json`. This importer overrides the shared helper's tag defaults: source does not add `upgradable:0` or `upgrade_available:1` here. Existing Teleport payloads and the previously imported Reforge 109 closure are not regenerated or duplicated.

## Verification

`python tools/import_lbc_infbolt.py` completed. All three complete output JSON trees independently compare equal against source processed by `migrate_loot_schema`, allowing only native entry IDs and the exact source Academy additions. This includes counts, pools, components, name/lore ordering and style, and modifier payloads.

| Output | SHA-256 |
|---|---|
| `bolt_material.json` | `4879876cc909727e82a1c794d0f88001cf6112810cc18b0f4f0f017dfc7a169f` |
| `infbolt_wand.json` | `9ac6f5928072f422b43647ac6109e03fd5359491448ff04ea42607bcc2fce373` |
| `academy_infbolt_wand.json` | `5cccf6c078b3781d718a7b0b96501cf1f39233bda42d1ff20464262c1d9de59d` |

All 16 keys referenced by these loot tables exist in both actual resource-pack languages under `assets/minecraft/lang/`. Existing values are reused without edits. The two source Academy announcement keys `upgrade_condition_infbolt` and `upgrade_succesful_infbolt` are absent from **both** languages. This is an existing resource gap; no replacement wording or language/template edits were made in this resource subtask.

Both item definitions `assets/lbc/items/{lightningegg,infbolt}.json` and referenced models `assets/lbc/models/item/{lightningegg,infbolt}.json` exist. Their generated model textures are `minecraft:item/lightningegg` and `minecraft:item/infbolt`. No additional loot-table or structure imports are introduced by these item payloads.

Read-only language SHA-256: RU `3e7b3f2c6aa2e9e223db99d52d4d1a9de9954bd546bad5ba3543b775c7dfb47b`, EN `d81685d3f4c65c833543ee9ece5f48904d01ba8b8c04e9a2dd2176dadeaa8782`.

These checks establish payload preservation and resource availability, not graphical appearance or runtime behavior. No Gradle build, server run, Java edits, language edits or resource-pack writes were performed.

## Ultra-reforge dependency closure

[import_lbc_reforge_resources.py](../tools/import_lbc_reforge_resources.py) now accepts `--infbolt`. It derives the 41 roots directly from `function/ultra_reforge.mcfunction`, follows the existing loot/modifier/predicate/tag dependency walker, and applies the same schema and legacy score-check migrations as Reforge. Every visited JSON is asserted equal to migrated source. No category list is invented: this includes the source `_all` categories, full armor set tables, `magnum_ammo` and `halloween/metasola`.

[INFBOLT_RESOURCE_MANIFEST.json](INFBOLT_RESOURCE_MANIFEST.json) lists 92 additional paths (235301 bytes), excluding paths already owned by [REFORGE_RESOURCE_MANIFEST.json](REFORGE_RESOURCE_MANIFEST.json). The complete visited closure has 198 loot tables / 771 direct entries and reuses 106 Reforge paths. Existing `big_snowball.nbt` is verified against source, SHA-256 `18499972dc2f1bb00668e41ea5ec1f1124e1f1d2127ca42dc2e5eb8adba2068e`, and excluded from the incremental manifest.

All 363 translation keys referenced by the incremental closure exist in both actual RP languages, and all referenced item-model definitions exist. The primary item textures and tier B/X tooltip backgrounds also exist. No resource-pack or language additions are needed for those payloads; the two Academy announcement gaps above remain separate.
