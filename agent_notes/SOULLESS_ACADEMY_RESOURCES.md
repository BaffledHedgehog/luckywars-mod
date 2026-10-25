# Soulless Academy presentation audit (25 October 2026)

Scope: narrow Academy Soulless leaf and read-only author resource audit. Resource parsing/presentation comparison does not qualify registry integration, gameplay, native loot decoding or client pixels. No Java, build, server, Git, author RP, translation or existing template edits were made.

## Output and exact payload

Added `src/main/resources/data/lbc/loot_table/ported/academy_soulcorruption_wand.json`: `lbc:ported/academy_soulcorruption_wand`, one deterministic `skywars2:academy_soulcorruption_wand` entry. Registration belongs to core integration.

Copied the ordinary port payload from `ported/soulcorruption_wand.json`, itself source entry index1 of `loot_table/halloween/transformables_staff.json`. Preserve all source components and styles: model `lbc:soulcorruptwand`, name `soulcorruption_wand` with italic=false and no forced color, initial tierB then final `lbc:tier_a`. Preserve all14 original lore rows and append the exact source green italic=false `upgraded_item` row. Preserve the score component `soulcorruption_wand_mana_use` / `lbc.math`, literals `0-6 ` and `15 `, every original translation and format, and the craft lore. The source radius15 summary disagrees with radius25 in actual selection and translated descriptive lore; this remains unchanged. No source enchantment/food/consumable/attribute components exist here.

Exact final custom data: `{craftusable:1,transformable:1,staff:1,magicwand:1,soulcorrupter:1,upgradable:0,upgrade_available:1,upgraded:1,mode:1}`. `soulcorrupter` identifies Soulless, distinct from ordinary Corruption Wand. Upgrade flags follow `other/magic_academy/can_upgrade_book` and `interacted_upgrade`; the specific `upgrade/soulcorrupter` applies `lbc:tiers/a`. The leaf retains 26.3 `modifier`/`type` syntax and omits source broad-pool weight10/quality9, as a deterministic narrow leaf requires. No broad Halloween pool was enabled.

## Author RP, existing keys and source quirks

Read-only RP root: `C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8`. Existing `assets/minecraft/lang/ru_ru.json`, `en_us.json`, `rpr.json` contain `soulcorruption_wand`, all nine source lore keys, `craft_with_antichrist_mace`, `upgraded_item`, `mode_default`, `mode_create_metasola`, `upgrade_condition_soulcorrupter`, and the intentionally misspelled `upgrade_succesful_soulcorrupter`. Reuse exact keys and values; no new translation or template is required. Source percent strings use literal double percent signs, retained unchanged in the RP.

Existing item definition `assets/lbc/items/soulcorruptwand.json` points to `lbc:item/soulcorruptwand`; model uses vanilla `minecraft:item/handheld_rod` and `minecraft:item/soulcorruptwand` texture. PNG exists at `assets/minecraft/textures/item/soulcorruptwand.png`. Existing tierA background sprite exists at `assets/lbc/textures/gui/sprites/tooltip/tier_a_background.png`.

Exact legacy root: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc`. Read `function/other/event/halloween/soulcorruptwand/cast_upgraded*`, `function/other/magic_academy/{can_upgrade,upgrade}/soulcorrupter.mcfunction`, common upgrade helpers and `loot_table/halloween/transformables_staff.json` (only matching staff source table exists). Source shifted mode selection uses `mode_default`/`mode_create_metasola` and two modes. Mode2 adds1000 mana and spawns `lbc:halloween/metasola`, before the shared1500 configured spend and equipment/trinket bookkeeping. Left-hand dispatcher uses `left`; shared cast clears it. These are gameplay integration observations, not resource changes. Existing ordinary and six Metasola leaves remain unchanged.

Validation: new JSON parses; structural comparison confirms all ordinary modifiers before custom-data upgrade are identical and all14 original lore rows remain in order. Native codec/build/server/client verification is owned by core integration. User choice remains READY ONLY: no installation or restart.

## User decisions (25 October 2026, user)

> сделай допуск и расход 1500, без возвращения 1000 маны получается

This supersedes the source mode2 refund above: admission and spend are1500 at the default configured cost, without a1000 refund.

> Сохранить 16 враждебных vex и немедленное открытие

Keep the trial16 ordinary hostile Vex and immediate Academy unlock.

## Integrated qualification

Build `63d98904` passed 26 recorded suite results
in two actual mode1 matches (55.21 seconds), including ordinary
Soulless, all six metasolas, Soulhunter and actual retained-trial match-stop cleanup.
Exact 343 Java sources / 939 classes /
2813 resources match the tested artifact.
Ready artifact: `build/verification/skywars2-soulless-academy-63d98904.jar`. Evidence: `build/verification/soulless-academy-final/`.
The main JAR remains unchanged at2558af6b; READY ONLY is the user's existing choice.
No author RP edits or translation fragments are required. Native fixtures do not
qualify real client pixels, actual disk restart/disconnect or peak-load performance.
