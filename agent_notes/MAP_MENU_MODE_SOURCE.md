# Legacy map menu by game mode

Read-only source audit of `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars` and the current map GUI/catalog metadata.

The legacy menu routes selection `swrg.gui_select=2` to page id **2000** when `#gamemode` is outside 3..4, and to page id **20000** when `#gamemode` is 3..4. Page id 20000 dispatches to function/page file `2000b`; “2000b” is the function name, not the scoreboard page id. This conditional is in `data/swrg/function/gui/select/0.mcfunction`.

| Legacy page id | Function/page file | Rendered map entries and IDs | Navigation |
|---|---|---|---|
| 2000 | `gui/page/2000` | SkyWars page 1: GUI slots 0–17 map to source map IDs 1–18; slots 19–25 map to IDs 19–25. Slot 18 is back. | slot 26 opens page 2100 |
| 20000 | `gui/page/2000b` | BedWars page: 11 rendered maps, source IDs 1b–11b: Divine Defecation, Asteroid, Fountain, Bigarden, Desert Castle, Pernicious, Twins Towers, Highland Peaks, Pavilion, Sand Dunes, Coalwars. | Legacy `gui/select/2000b.mcfunction` routes slot 26 to page 2100, which leaks into the SkyWars list. Strict mode separation must not expose this next-page transition for BedWars. |
| 2100 | `gui/page/2100` | SkyWars page 2: slots 0–17 map to source IDs 26–43; slots 19–25 map to IDs 44–50. Slot 18 is back. Labels in slot order: Hell, Jungle, Monkeys, Oasis, Swamp, Iron Swords, Astraeus Station, Alchemy Table, Steampunk, Iron Axe, Glacier, Sandcastle, Natural, Concordia Horizon, Acua, Beach Rush, Candy, Rubik Cubes, Detective, Huts, Japon, Advanced, Amesa, Blimp, The End – Chapter I: Dragon Island. | slot 18 returns to 2000; slot 26 opens 2200 |
| 2200 | `gui/page/2200` | SkyWars page 3: slots 0–11 map to source IDs 51–62; slot 13 maps to ID 64. Slot 18 is back. Rendered labels: The End – Chapters II–IV, TNT, China, End, Stonehenge, Japan, End Pillars, Blossom Meadows, Sand, Orcs, Dragonball. | slot 18 returns to 2100 |

A page number, GUI slot, and source map ID are distinct values. The current [`gui_design.json`](../src/main/resources/skywars2/gui_design.json) stores `sourceMapId`/`sourceItem` for map entries; its IDs confirm the slot-to-map correspondence above. Do not list unrendered selector values as visible maps.

The legacy map handlers have more branches than the rendered BedWars page: `2000b_mapselect.mcfunction` contains selector-to-map-function cases beyond the 11 visible entries, but page `2000b` renders only source IDs 1b–11b. The current [`catalog.json`](../src/main/resources/skywars2/maps/catalog.json) has IDs 1b–11b marked `available: false`; their templates are missing, so these BedWars maps are unsupported in the current catalog and the mode filter does not implement BedWars. Keep the BedWars selection to its single page; do not use the legacy slot-26 route into SkyWars.

For mode filtering, the directly evidenced legacy predicate is `#gamemode matches 3..4` for the BedWars page route; all other values use page 2000. `core/default_settings.mcfunction` sets the default to 2. This audit does not establish individual semantic names for values 3 and 4.

## Source files

- `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/gui/select/0.mcfunction` — mode-dependent route to page IDs 2000/20000.
- `.../data/swrg/function/gui/page.mcfunction` — dispatches page IDs 2000, 20000, 2100, 2200 to page functions.
- `.../data/swrg/function/gui/select.mcfunction` — dispatches selection handling by page ID.
- `.../data/swrg/function/gui/page/2000.mcfunction`, `2000b.mcfunction`, `2100.mcfunction`, `2200.mcfunction` — visible labels and selected-map marker slots.
- `.../data/swrg/function/gui/select/2000.mcfunction`, `2000b.mcfunction`, `2100.mcfunction`, `2200.mcfunction` — selection and back/next transitions; `2000b` has the BedWars-to-SkyWars next-page leak.
- `.../data/swrg/function/gui/select/2000_mapselect.mcfunction`, `2000b_mapselect.mcfunction`, `2100_mapselect.mcfunction`, `2200_mapselect.mcfunction` — slot-to-map handler resolution.
- `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/swrg/function/core/default_settings.mcfunction` — default `#gamemode=2`.
- `../src/main/resources/skywars2/gui_design.json` and `../src/main/resources/skywars2/maps/catalog.json` — current map source IDs, items, and availability.

## Current correction — 16 October 2026

`GameMenus.renderMaps` now uses only `2000 → 2100 → 2200` for modes 1/2,
and only `2000b` for modes 3/4. Card slots, translated names, visuals,
selection markers and asynchronous selection handling are retained.
The remembered page is clamped to the current family; BedWars has no next
button and its back button returns to the hub. Clicking a stale cross-mode
card refreshes the menu without selecting/loading it, including unavailable
cards. Mode changes within the same family keep the same map list.

Build and native menu checks passed on
`c69fdf8aa1eebf7754dc321c60461895ae132229171f69485927fe77ef008cb1`.
The existing `menu-navigation` check covers all four mode values, repeated
native inventory clicks, every visible card's page family, both stale-mode
directions, third-page → BedWars clamping, back/next and locked inventory.
`menus` and `lobby-inventory` also passed. Graphical client appearance was
not separately tested. [Results](../build/verification/map-menu-modes-results.json),
[deployment status](../build/verification/map-menu-modes-installation.json).
The main server is still running with the previous JAR; the new one is staged.
