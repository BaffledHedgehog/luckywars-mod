# Locked GUI design audit

Scope: read-only comparison of the old GUI under `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars` and `.../LBC` with `src/main/resources/skywars2/gui_design.json`, `src/main/java/me/skywars2/game/OriginalGuiDesign.java`, and `LbcLoadouts`.

## Old fallback design and predicates

The old GUI writes a normal loot icon, replaces unavailable slots with a repeating command block carrying an `item_model`, then applies item modifiers for name/lore. Vanilla pages use `lbc:locked_case` with `swrg:locked_kit_default`, `swrg:locked_skill_default`, or `swrg:locked_cosmetics_default`. Special/event item pages use `lbc:locked` and per-item `swrg:locked/<id>` or `swrg:locked_event`; suffix reward pages use themed models `lbc:locked_case_pandora` / `lbc:locked_case_halloween` and `locked_suffix_pandora` / `locked_suffix_halloween` modifiers. The source family also includes `locked_suffix_tournament`.

The default modifiers preserve source components: locked name `gui.locked1` (red, bold, non-italic); red lore lines `use_case_for_open` plus the category prompt (`open_case_kit_normal`, `open_case_skill_normal`, or `open_case_cosmetics_normal`). Event modifiers retain per-item lore. Source definitions are under `swarogskywars/data/swrg/item_modifier/`, including `locked/*.json`.

Availability checks use both scoreboard tags and advancement predicates. Kit/skill/cosmetic examples are `swrg.kitN`, `swrg.skillN`, `swrg.soundN`, and `swrg.barsN`; event examples are `lbc:true_advancements/event/wind_wand` and `swrg:rewards/turnir/trophy1`. Old selector handlers map GUI slots back to selections in `data/swrg/function/gui/select/`.

Ranked coverage was understated in the earlier audit. There are ranked page and/or selection dispatches for vanilla kit/skill pages 3000/4000 and LBC catalog pages 8000, 8100, 9000, 9100, 10000, 10100, 11000, and 12000. Their `*_ranked.mcfunction` page implementations populate regular loot and selection state without the locked fallback pass, so those GUI advancement gates are bypassed. No ranked page/selector was found for LBC page 11100. This page/selection behavior does not award the source advancement or grant its locked perk; the ranked contract is to bypass gates without granting perks.

## Narrow locked refresh results

`tools/import_gui_design.py --refresh-locks` updates only locked variants, slot unlock conditions, and referenced modifier bindings while preserving the rest of the existing design JSON. Current result: **145 locked variants**, **144 slots with paired unlock conditions**, and **116 distinct source modifier bindings** in `sharedModifiers`. Every locked variant has at least one modifier. The 145 variants use `lbc:locked` or `lbc:locked_case`; selectors were parsed from raw `@s[...]` commands, including direct commands without `execute`. The refresh accepts tag and advancement fields, drops scoreboard selection fields from unlock requirements, and rejects unsupported locked-selector fields.

Examples: page 9000 slot 0 has model `lbc:locked`, modifier `lbc:swrg_unlocks/devilstomach`, and lock gate `lbc:true_advancements/accesories/devilstomach=false`; its slot unlock condition is the corresponding `true` predicate. Modifiers' raw definitions are in `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/item_modifier/swrg_unlocks/<id>.json`; their component bindings are imported through the existing conversion helper. These are per-item definitions, not generic shared source modifiers.

Ruby correction (user-confirmed): source page `LBC/data/lbc/function/swrg_kit_integration/gui/page/9100.mcfunction` mistakenly targets `lbc:swrg_unlocks/ruby_upgraded` at slot 0 although Ruby's locked replacement is slot 1. The refreshed design assigns that modifier to the slot 1 variant by its matching advancement predicate `lbc:true_advancements/legends/academy/ruby=false`; slot 1's unlock condition is the corresponding `true` predicate. This documents the confirmed correction, not the old target slot.

## Remaining source coverage limits

Seasonal cosmetics subpages in `swarogskywars/data/swrg/function/gui/page/7300_*` and `7310_*` are not represented by the current `gui_design.json` page inventory, so their seasonal models and suffix modifiers have not been ported into the design. Keep their old fallback models and modifier lore as the source of truth; do not invent seasonal design entries while working only with current pages. The schema's `lockedVariants` array supports multiple models/modifiers when those source pages are added.

`src/main/resources/skywars2/lbc_loadouts.json` maps 8000/8100 to levelups (30 entries), 9000/9100 to kits (11), 10000/10100 to skills (1), and 11000 to trinkets (8). It is not a complete inventory of GUI locks: some page items have no configured gate, page 9100 includes the lightning/ruby upgrades, and page 11000 has locks beyond the configured trinket entries. Do not infer complete catalog coverage from `lbc_loadouts.json` alone.
