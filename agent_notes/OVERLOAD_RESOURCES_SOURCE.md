# Overload resource source audit

Read-only audit for the Overload Crux and its four armor replacements. The authority is the disabled legacy datapack snapshot, `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc`; the live client pack is `C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8`. Neither source was changed. This is source/resource evidence, not a claim that the disabled datapack runs in the target server.

## Proposed server items

| Server ID | Loot proxy/native item | Source item-model and equipment asset | Source behavior marker |
|---|---|---|---|
| `skywars2:overload_crux` | `minecraft:carrot_on_a_stick` | `lbc:overload_crux` | `overload_crux:1`, `magicwand:1`, `craftusable:1` |
| `skywars2:overload_helmet` | `minecraft:leather_helmet` | `lbc:armors/overload_helmet`; equippable `head`, asset `lbc:overload` | `overload:1`, `helmet:1`, `transformable:1` |
| `skywars2:overload_chestplate` | `minecraft:leather_chestplate` | `lbc:armors/overload_chestplate`; equippable `chest`, asset `lbc:overload` | `overload:1`, `chestplate:1`, `transformable:1` |
| `skywars2:overload_leggings` | `minecraft:leather_leggings` | `lbc:armors/overload_leggings`; equippable `legs`, asset `lbc:overload` | `overload:1`, `leggings:1`, `transformable:1` |
| `skywars2:overload_boots` | `minecraft:leather_boots` | `lbc:armors/overload_boots`; equippable `feet`, asset `lbc:overload` | `overload:1`, `boots:1`, `transformable:1` |

These are suggested unique Polymer-backed IDs; keep the vanilla proxy compatible with each item’s actual use. The crux needs the carrot-on-a-stick interaction path, while the four armor items must remain wearable vanilla armor types. A shared equipment asset is intentional.

## Exact source payload

Crux source: `loot_table/overload_crux.json`. One item entry, count 1; max stack 99; tooltip `lbc:tier_q`; item model `lbc:overload_crux`; removes `minecraft:max_damage`. Name key `overload_crux`, non-italic `#B336ED`. Lore has nine rows: a white non-italic line composed of `overload_crux.lore.6` + live scoreboard component `overload_crux_mana_use` on `lbc.math` + `overload_crux.lore.7`; white `overload_crux.lore.8`; two yellow non-italic `craft_with_diamond_armor.1/.2` lines; four dark-gray italic `overload_crux.lore.1`–`.4`; and `overload_crux.lore.5` in `#5724A4`, italic. Preserve the score component (not a literal rendered number) and the function-built component ordering.

Armor sources: `loot_table/neko_set_overload/{helmet,chestplate,leggings,boots}.json`. All are leather proxy armor, max damage 900, tooltip `lbc:tier_s`, item name `overload_<piece>` in `#B336ED` non-italic, and equipment asset `lbc:overload`. Exact attribute modifiers in the source component are:

| Piece | Armor | Toughness | Other |
|---|---:|---:|---|
| Helmet/head | +3 | +2 | — |
| Chestplate/chest | +8 | +2 | — |
| Leggings/legs | +6 | +2 | — |
| Boots/feet | +3 | +2 | safe-fall-distance +16 |

Every modifier uses `add_value`. Keep their distinct modifier IDs from the JSON if copying the component. The payloads do not set enchantments, dye, or damage. Lore keys and line counts: helmet `overload_helmet.lore.1`; chestplate `.lore.1`–`.2`; leggings `.lore.1`–`.3`; boots `.lore.1`–`.4`. Each then has a blank literal space and the shared `set_bonus` / `set_bonus_overload` + `.1`–`.4` rows (the title and body styles are separately encoded in each source JSON). Live RU/EN language files contain these keys and corresponding translations.

## Reachability and effects in the source datapack

`other/overload_crux/give.mcfunction` spawns `loot lbc:overload_crux` and marks `obtained_crux`; `other/manaregen.mcfunction` routes players with mana below zero to `other/negative_mana`. The working-item spell selector recognizes `minecraft:custom_data {overload_crux:1}` and calls `other/overload_crux/use` (the configured debit is 7500). `use` debits mana, fires five `tentacle_full_set` calls, then individually replaces only currently equipped vanilla diamond helmet/chestplate/leggings/boots with the matching `neko_set_overload` loot table. It consumes one crux. This is direct slot replacement, not a crafting recipe.

The source has actual armor passive paths: `items/changed_inventory/sel.mcfunction` derives four active tags from armor slots by matching `overload:1`; `resets.mcfunction` calls that selector. The negative-mana function uses helmet tag to select the escalating debuff path and the special max-negative-mana helmet path; chestplate tag invokes `negative_mana_resist`. The helmet path removes/mitigates negative-mana debuffs but does not suppress damage (also stated in the EN lore). The chestplate path grants increasing Resistance for mana bands `-1..-5000` through `<=-20001` (amplifiers 0 through 4).

Leggings are reachable through `items/armor.mcfunction` while sneaking; `armor/overload/leggings/jump` applies levitation, Resistance, a cooldown, and removes 2500 mana from nearby players. Boots are reachable through `players_tick.mcfunction` when the player is airborne, sneaking, has the active boots tag, and has a clear fall path; slam/impact logic runs from there. EN/RU lore describes damage, mana drain/gain and fall-height scaling. The full-set marker `overload_full_active` is checked by the negative-mana tentacle damage path to alter tentacle behavior/self-damage. This audit establishes source references, not parity/runtime behavior in the mod.

Replacement semantics: each Crux replacement uses `loot replace entity ... armor.<slot> loot ...` on a matching diamond item. This replaces it with a fixed leather item carrying the listed components; the imported payload intentionally does not inherit enchantments, damage, custom data, or other components from the old diamond stack.

## Live resource pack and 26.3 notes

The live pack contains item definitions/models for `lbc:overload_crux` and all four `lbc:armors/overload_*` references under `assets/lbc/items` and `assets/lbc/models/item`. It also contains `assets/lbc/equipment/overload.json`, using humanoid texture `lbc:overload` and leggings texture `lbc:overload_leggings`, and the corresponding armor/item textures. RU and EN translations exist in `assets/minecraft/lang/ru_ru.json` and `en_us.json` for names, lore, crafting prompt, and full-set bonus.


Import status: the five generated tables live at `data/lbc/loot_table/ported/overload_{crux,helmet,chestplate,leggings,boots}.json`; only their item name changes to the corresponding `skywars2:overload_*` ID. `import_lbc_weapons.py` uses its selected-entry helper and legacy loot schema migration. `check_design_sources.py` independently selects all five source entries and exact-compares migrated payloads including the entire component/function list. All five paths are now in the `processResources` allowlist. The source component shapes validated against 26.3 during server build are `minecraft:item_model`, `tooltip_style`, `equippable` with `asset_id`, and `attribute_modifiers` with namespaced attribute type, slot, ID, amount and `add_value` operation.
