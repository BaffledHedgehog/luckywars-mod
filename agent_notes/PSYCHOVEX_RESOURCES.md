# Summoner Vexes / Psychovex resources — 18 October 2026

Resource-only import; this document does not claim runtime behavior is implemented.
`tools/import_lbc_psychovex.py` reuses `import_lbc_fireblaze.import_item`, including
its `select_entry`, schema migration and `build_single_entry_table` codec path.
The only item identity replacement is the outer entry `name`; source name/lore,
formatting, models, tooltip styles, custom data, weights and quality are preserved.

| Native ID / ported filename | Source loot table | Name key | Model | Tooltip | Config mana |
|---|---|---|---|---|---|
| `skywars2:summoner_vexes_wand` / `summoner_vexes_wand.json` | `transformables_summons.json` | `vex_soul` | `lbc:summonvex` | `lbc:tier_a` | `vex_soul_mana_use = 5000` |
| `skywars2:psychovex_wand` / `psychovex_wand.json` | `tuntijaa_blind_vex.json` | `psychovex_wand` | `lbc:blindvex` | `lbc:tier_s` | `psychovex_wand_mana_use = 1000` |

The corresponding Academy tier categories are A and S respectively. Neither
source payload carries `upgradable`/`upgraded`, and no separate Academy variant
or source trial for these two items was found. These categories describe the
original tooltip tiers, not an added Academy upgrade. Native Academy registration
is outside this resource task.

Sources, relative to the read-only legacy `world/datapacksdisabled/LBC/data/lbc/`:
`loot_table/transformables_summons.json`, `loot_table/tuntijaa_blind_vex.json`,
`function/config.mcfunction` (lines 56 and 92). Recipe source is
`function/other/craft_ground.mcfunction:24` and
`function/other/craft/inventory_craft_check.mcfunction:24`:
`stblind` + `summonervexes` produces `tuntijaa_blind_vex`.

The real read-only resource pack `all_in_one_1.21.8` already supplies both
`assets/lbc/items/summonvex.json` and `assets/lbc/items/blindvex.json`, targeting
`lbc:item/summonvex` and `lbc:item/blindvex` respectively. No template is needed.

All 14 referenced translation keys exist in both
`assets/minecraft/lang/ru_ru.json` and `assets/minecraft/lang/en_us.json`:

| Key | RU | EN |
|---|---|---|
| `vex_soul` | present | present |
| `type` | present | present |
| `summoner` | present | present |
| `summon_damage` | present | present |
| `summon_health` | present | present |
| `summon_count` | present | present |
| `mana_on_activate` | present | present |
| `vex_soul.lore` | present | present |
| `summoners.all.lore.2` | present | present |
| `craft_with_blind` | present | present |
| `psychovex_wand` | present | present |
| `psychovex_wand.lore.1` | present | present |
| `psychovex_wand.lore.2` | present | present |
| `psychovex_wand.lore.3` | present | present |

Names are «Страх Обречённого» / “Fear of the Doomed” and
«Манья Конвокатио Культус» / “Manya Convocatio Cultus”. Existing translations
are reused verbatim, including trailing/leading spaces in lore fragments.
The real resource pack, template and language files were not edited.

Validation: ran the importer and compared each complete output JSON against the
shared migrated source codec with only its outer native item ID replaced; both
payload equality checks passed. This checks all preserved component structures,
not just names or selected metadata. Runtime/Java validation belongs to the
mechanics implementation.

## Immutable entity presets

The same importer additionally extracts all seven spawn payloads into
`src/main/resources/skywars2/summons_presets.json`. Its exact format is
`{originalDataVersion: 4440, presets: {vex: SNBT, strangeness: SNBT, chaos: SNBT,
luck: SNBT, omnipresence: SNBT, obsession: SNBT, hypocrisy: SNBT}}`.
The source SNBT is retained verbatim with only the entity `id` inserted so native
ENTITY DFU can identify its schema. Typed numeric values, all attributes,
equipment, armor assets, translation components, tags and drop chances survive.
No handwritten NBT/item conversion is introduced in Python. The importer checks
that the four source Vex summon payloads are identical before choosing one.

The entity source uses `equipment` / `drop_chances`, introduced after 1.21.4,
so the unrelated Academy structure's DataVersion 4189 is unsuitable. Version
4440 is the verified `world_version` in Mojang's official 1.21.8 server JAR
`version.json` (download obtained through the official version manifest). It
provides the compatible source schema for these already modern equipment fields;
this does not assert that the legacy folder name identifies the command version.
The loader applies native ENTITY DFU through `References.ENTITY` once during resource preparation,
then each cast copies the prepared tag; runtime wiring belongs to `LbcSummons`.

Profiles preserve source details: Vex has current Health 5 and follow range 200;
strangeness is a 30 HP Wither Skeleton with bow/black armor; chaos a 50 HP
Skeleton with Heart of the Sea/chaos armor; luck a 40 HP Skeleton with Totem,
knockback resistance 10/luck armor; omnipresence a 40 HP Skeleton with bow/white
armor; obsession a 10 HP Skeleton with diamond axe, attack 11, movement 0.25 and
femboy armor; hypocrisy a 60 HP Zombie with two iron swords/hypo armor. The exact
source fields remain authoritative, including absent max-health overrides on Vex.

Source functions: `function/workingitems/summoner/vexes.mcfunction` and
`function/workingitems/summoner/psychovex_spawn.mcfunction` under the same
read-only legacy LBC root. Entity resource extraction completed successfully;
native entity codec and behavior checks are handled by the runtime implementation.

Read-only RP verification also found all six entity name keys in both RU/EN:
`strangeness_man`, `chaos_man`, `luck_man`, `omnipresence_man`, `obsession_man`,
`hypocrisy_man`. All six armor definitions already exist under
`assets/lbc/equipment/`: `cultus_black.json`, `cultus_chaos.json`,
`cultus_luck.json`, `cultus_white.json`, `cultus_femboy.json`, `cultus_hypo.json`.
No translation or armor template additions are required.
