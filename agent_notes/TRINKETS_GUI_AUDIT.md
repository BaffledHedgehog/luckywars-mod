# Trinket GUI contract audit

Scope: old datapack menu behavior and new mod GUI catalog only. No effect mechanics reviewed.

## Sources

- Old menu input: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/gui/select/11000.mcfunction`, `11000_prex_2.mcfunction`, `11000_prex_3.mcfunction`, `11000_ranked.mcfunction`, `11100.mcfunction`.
- Old menu render: `.../gui/page/11000.mcfunction`, `11000_x.mcfunction`, `11000_x1.mcfunction`, `11100.mcfunction`, plus `.../loot_table/gui/page/11000/{0..17,19..23}.json`.
- New GUI catalog: `E:/servers_mew/skywars2_last/luckywars-mod/src/main/resources/data/lbc/loot_table/gui/page/11000/*.json`; underlying item catalogs at `.../data/lbc/loot_table/trinket/*.json`; GUI layout in `.../resources/skywars2/gui_design.json` page `11000`.
- Old datapack global default is 9, assigned in `swarogskywars/data/swrg/function/core/default_settings.mcfunction`. The Java config fallback is also `lbcTrinketLimit=9`; accepted/config-clamped range is 0..12 in `src/main/java/me/skywars2/game/GameConfig.java`. GUI setting cycle is 1..12 in `GameMenus.java`.

## ID and slot contract

- A click's `swrg.gui_select` is the zero-based menu slot. `select/11000*.mcfunction` adds 1 before storing it in a packed selection digit. Thus packed item IDs are slot + 1, not the GUI slot and not the trinket model suffix.
- Selectable trinket slots are `0..17` and `19..23`. The resulting packed IDs are `1..18` and `20..24`; packed ID 19 is deliberately absent because menu slot 18 is reserved for Back. Slot 24 is not a trinket: it maps to packed ID 25 and resets all three packed scores to `100000000` (empty sentinel). Slot 25 is reset in the page layout and slot 26 advances to page 11100.
- GUI slot 18 is reserved for Back; slot 19 contains the trinket whose packed ID is 20. Do not compact slots around the gap or treat ID 19 as a real trinket.
- Packed `lbc.trinket1..3` are decimal digit strings, four IDs each: millions, ten-thousands, hundreds, units. The leading two decimal places encode cursor/selected position (`/100000000`). Cursor is 1-based and cycles `1..#lbctrinket`; clicking a trinket replaces that cursor digit, advances cursor with wrap, and preserves the other digits. `11000_prex_2` advances by one; `11000_prex_3` advances by 28 for the alternate/prex path. Duplicate selected IDs are not filtered by this GUI algorithm.
- Reset sentinel is `100000000` in each packed score. Rendering extracts each digit and marks the selected cell with `swrg:select`; the score itself is an internal encoding and must not be rendered as a number.

## Catalog and presentation

- GUI catalog slot/index is the translation/catalog index, while the click action must convert slot to packed ID by `+1` with the slot-18 hole retained. Existing newmod loot-table catalog has entries and translation keys `lbctrinket.N` for indices 0..17 and 19..23; index 15 is the valid Gneiss Stone entry using `minecraft:stone` and has no item_model in both old and new definitions.
- Expected catalog mapping: 0 blaze, 1 ghast, 2 penis_gun, 3 drojjevoe_odealo, 4 blood_anal_boom, 5 mandoahuet, 6 bedrcock, 7 nether_plug, 8 volos, 9 intercal, 10 cocktail, 11 banangay, 12 fourier, 13 rocket_spell, 14 twilight_sparkle, 15 gneiss_stone, 16 hedgehog_gauntlet, 17 snow_gem, 19 ice_gem, 20 mana_gem, 21 poly_gem, 22 glass_shard, 23 psycho_soul. Confirmed against the old slot loot tables and new item loot table names/models/translations.
- The menu uses each `lbctrinket.N` name/lore plus pick/win rate/count lines. The second page (11100) shows trinket statistics for the four stored IDs and navigation/reset controls; it is not another set of selectable IDs. Do not display raw packed scores in GUI text.
- New GUI source agrees with the old catalog for visible indices. Index 15 being plain stone is intentional, not a missing model import; `trinket/gneiss_stone.json` carries its `{trinket:"gneiss_stone"}` identity.

## Limit setting caveat

- The old datapack global default is 9 (`swarogskywars/data/swrg/function/core/default_settings.mcfunction`). The Java config fallback is also 9. Accepted/config-clamped range is 0..12, while the settings GUI cycles 1..12. The old LBC menu/render code consumes global fake-player score `#lbctrinket swrg.math`; this default source is in the old `swarogskywars` datapack rather than the audited LBC function set.
