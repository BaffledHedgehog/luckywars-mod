# Defensive arcanum resources (2026-10-24)

Owned additions only: `src/main/resources/data/lbc/loot_table/ported/healing_arcanum.json` and `barrier_arcanum.json`.

Source: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/transformables_arcanum.json`, barrier entry lines 123-212; healing entry lines 214-290. Each leaf has one roll and its original single item entry including weight/quality10. Changes are limited to native item IDs `skywars2:healing_arcanum` / `skywars2:barrier_arcanum`, modern `functions` -> `modifier`, and function discriminator `function` -> `type`, following existing `ported/void_arcanum.json`.

The initial resource audit preserved every original component, flag, count, translated name, colour, italic setting and lore component. Healing retains its four rows with `healing_arcanum_mana_tick_sec`. After the explicit user ruling, Barrier has four rows: the obsolete activation-cost row is removed, while literal `500%`, names and remaining components stay unchanged. The current price is five times regeneration once per player, without activation charge; see DEFENSIVE_ARCANUMS_PORT.md.

Validation: parsed each written JSON and reversed only the three schema/ID transformations; deep equality with its complete original source entry passed for both. No build performed by resource subtask.

Author resource pack read only: `C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8`. Both `assets/minecraft/lang/ru_ru.json` and `en_us.json` contain every name/lore/shared translation key referenced by these two entries (including type, arcanum, mana_on_use, mana_on_activate). Existing translations are reused; no language/template edits.

Verified author item definitions `assets/lbc/items/arcanumhealing.json` and `arcanumbarrier.json`, referencing `lbc:item/arcanumhealing` and `lbc:item/arcanumbarrier`; verified corresponding `assets/lbc/models/item/*.json` and their referenced texture PNG files exist. Source model identifiers remain `lbc:arcanumhealing` and `lbc:arcanumbarrier`; tooltip remains `lbc:tier_a`. File existence/reference and source parity were checked; no visual client rendering claim.

Runtime item registration, leaf admission/coverage and scoreboard initialization belong to the parent implementation. No shared files, Java, source server, pack or template files changed.

## Subsequent user ruling
Parent removed the obsolete Barrier activation-cost lore row under the user ruling that there is no activation fee. Barrier now has four lore rows; literal 500% and all remaining original components are preserved. This authorized change supersedes the earlier five-row/full-entry equality claim above. Runtime uses 500% regeneration once per player, independently of holding one or two barriers.
