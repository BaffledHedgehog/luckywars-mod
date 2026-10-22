# Reroll Machine source audit — 21 October 2026

Read-only audit; no gameplay code or resource pack changed. Source root: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data`; native root is this repository. Applicable `AGENTS.md`, `agent_notes/AGENTS.md` and `RESOURCE_PACK.md` preserve user notes and forbid source/RP edits during research.

## Exact source behavior

- `lbc/function/players_tick.mcfunction:61` invokes inventory crafting checks whenever any of the four player crafting slots has `craftusable:1`. `other/craft/inventory_craft_check.mcfunction:77` requires any machine (`reroll_machine:1`) and any `transformable:1` donor. No mana, magic suppression or mechanism suppression condition exists on this invocation. It runs on ticks, not taking a result; no workbench route is present here. Earlier crafting checks may mutate or return before reaching line 77.
- `other/craft/reroll_machine.mcfunction:1` resets count, calls `reforge_crafting_0` through `_3` for every occupied transformable slot, then subtracts `5000 * count` from player mana. There is **no affordability threshold or clamp**; negative mana is explicit design in the source RP lore. Cost is per occupied donor slot, regardless of stack size or number of matching categories, including transformable donors with no matching output category.
- Each slot function rolls all independent matching categories, grants `lbc:true_advancements/lightning_smith`, increments count once. Final four `count_reduce` modifications consume one unit from every transformable donor slot. Machine stays; no probability of machine consumption. With one machine in the 2x2 grid, up to three donor stacks are processed per tick. Extra unrelated ingredients do not block processing.
- Rewards use `loot spawn ~ ~1 ~` while executor remains **player**, so command loot THIS_ENTITY is player and origin is one block above player. This differs from Tiny's synthetic dropped-item entity context. All reward bundles survive as spawned items; no inventory capacity precondition.
- Slot functions 0..3 have identical category order after substituting slot number: accessory, axe, bow, hoe, pickaxe, potion, shield, shooter, shovel, staff, sword, totem, spellitem, summoner, trident, arcanum, libspell, helmet, chestplate, leggings, boots, metasola. Tables are `transformables_<category>` except spellitem→`transformables_spells`, summoner→`transformables_summons`, libspell→`spells_library`, metasola→`halloween/metasola`.
- This is exactly native `LbcBolts.NORMAL_REFORGE` (LbcBolts.java:55) category/table order; unlike Ultra, no armor-set flags, no `_all` pools and no exact-64 arrows exception. Natural LIGHTNING mapper has different order and lacks libspell. Use normal mapper only after separately checking `transformable:1` (normal mapper itself does not require it).

## Item and acquisition

`lbc/loot_table/kit/reroll_machine.json`: repeating_command_block, count defaults 1, item_model `lbc:reroll_machine`, white nonitalic custom name `lbckit.24`, exactly two gray nonitalic lore lines `lbckit.24.lore.1` and `.2`, custom_data `{reroll_machine:1,craftusable:1}`. No transformable/type/category marker, enchantment, durability or reduced max-stack component. GUI card `gui/page/9000/24.json` uses carrot_on_a_stick and adds source statistics; distinguish card from reward.

Legacy selection is kit ID 25, second kit page 9100 slot 0: `swrg_kit_integration/gui/page/9100.mcfunction:11` and `gui/select/9100.mcfunction:4` require `lbc:true_advancements/legends/academy/lightning`; ranked page/select remove that gate. `game/kit_select/select.mcfunction:36` grants `lbc:kit/reroll_machine` for selected ID25.

Native: original reward and GUI loot JSON exist, but no Java reroll_machine implementation/registration and **no kit ID25 in `src/main/resources/skywars2/lbc_loadouts.json`**. Presence of copied source loot does not make it selectable/playable. `LbcLoadouts.java:345` grants only catalog entries, applying their unlock gate. Existing ported kits generally preserve original `lbc:kit/...` alias and substitute native Polymer payloads there; acquisition requires adding the catalog entry with source page/slot/gate as appropriate. Native Polymer patterns: `LbcItems.java:112`, `LbcKfcRewards.java:72`, `LbcWands.java:255`; connect registration in `LbcItems.register()` and preserve model/name/lore/custom data through native loot.

## Resource pack (read only)

Actual inspected root: `C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3/minecraft/resourcepacks/all_in_one_1.21.8` (RESOURCE_PACK.md still records an older 26.3(1) path). Existing `assets/lbc/items/reroll_machine.json`, `assets/lbc/models/item/reroll_machine.json`; no template needed for existing keys.

`assets/minecraft/lang/{ru_ru,rpr}.json:5253`:
- `lbckit.24`: `Реролл-машина`
- `.lore.1`: `> Перековывает предметы в сетке крафта в инвентаре (положить с предметом)`
- `.lore.2`: `>Требует 5000 маны для перековки, но может расходовать ману даже в минус`

`en_us.json:5253`:
- `Reroll-machine`
- `- Rerolls items in inventory crafting grid (place it with item)`
- `- Drains 5000 mana for reroll, but also can drain it even in negative mana`

## Implementation reuse and author decisions

Reuse `LbcTinyReforge.java` result-action validation, stable opaque preview, precommit grid snapshot, supported pickup/shift/swap/drop handling, atomic unit consumption, complete bundle/overflow delivery, epoch/role/menu checks and `LbcTinyReforgeMixin`. Generalize rather than clone the transaction machinery. Add ordinary category selection and player-context roller entry point: existing `LbcBolts.rollReforge(ItemEntity,...)` always uses an item context, while `LbcLoadouts.rollTable(level,origin,source,table)` already accepts a player.

No author question needed about price, negative mana, retained machine, ordinary pools, independent categories, model/name/lore or source unlock: evidence is explicit. A genuinely unresolved migration choice is whether to preserve automatic 2x2 per-tick processing (up to three donors, unrelated slots tolerated) or adopt committed result-taking with Tiny's safe preview and support workbench too. If adopting result-taking, specify one donor versus all donor slots per commit and policy for transformable donors with no mapped table (source consumes and charges anyway). These are UX/transaction changes, not unknown source behavior. Preserve source player loot context regardless of UI choice.
