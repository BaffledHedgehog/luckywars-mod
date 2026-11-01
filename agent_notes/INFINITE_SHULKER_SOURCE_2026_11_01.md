## Current implementation findings superseding the preliminary source assumptions

Root integration confirmed the five contents/output roots were NOT packaged, and
still used ignored old functions/function fields. Both are corrected by the native
port. Base native container_loot metadata alone therefore did not prove opening
worked. Author confirmed the entire chain unjammed. Normal native output slots
and one-unit ground debit preserve ingredients. Implementation/native qualification
is recorded in INFINITE_GEAR_PORT_2026_11_01.md; the audit below remains source-only.

# Infinite shulker source closure — 1 November 2026

Read-only audit; only this note written. No builds, Java/resources/server/RP edits. TMT remains last. `LbcRerollCrafting.java` is user-dirty and was only read.

Legacy root: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`. Native paths below are relative to this project.

## Exact crafting contract

- `function/other/craft_ground.mcfunction:33–34`: two unopened base items (`inf_shulker:1` plus exact container loot `lbc:infinity`) produce one `lbc:infinity_upgraded_only_shulker`; upgraded unopened (`inf_inf_shulker:1`, `lbc:infinity_upgraded`) plus `luckyblockwand:1` produce one `lbc:infinity_upgraded_only_shulker_luck`. No mana, suppression, or craft advancement in these functions. Ground trigger runs each second (`function/tick1s.mcfunction:7`), excludes Nexus/Imprinted, requires craftusable anchor and second item 0.001..1 away. Recipes themselves have no MAG/TECH/artifact conditions.
- Ground upgrade functions spawn at anchor Y+0.1; black dust RGB0/scale2 or gold RGB1,.867,.271/scale2, 100 particles at Y+1 with spread1 and speed1, force; enchantment-table sound master/all players, volume1/pitch1. They kill matching item ENTITIES (two base, or one upgraded plus one wand), not decrement stacks. This can erase whole stacks; source proximity kill can select a different eligible neighbor. Recommend atomic one-unit debit per selected donor, subject to parent/user policy; do not silently reproduce entity deletion as intended stack cost.
- `function/other/craft/inventory_craft_check.mcfunction:33–34` and four `shulker_upgraded*.mcfunction` files: personal 2x2 grid only. Base requires exactly two matching occupied slots, one unit removed from each. Lucky path removes one from every matching wand/upgraded slot, with no exact-two validation. Both spawn result in world, not output slot. `function/players_tick.mcfunction:61` gates invocation by any craftusable item. Native ordinary output recipes should reject extra occupied slots and use vanilla debit, consistent with existing native crafting owner.

## Output/opening closure

Existing `src/main/resources/data/lbc/loot_table/infinity_upgraded_only_shulker.json`: exactly one vanilla black shulker, no special model, tier_s tooltip, container loot `lbc:infinity_upgraded`, name `infinite_shulker_upgraded` nonitalic, yellow nonitalic lore `infinite_shulker_upgraded.lore`, custom data `{craftusable:1,inf_inf_shulker:1}`. Bare item-def leaf `item_defs/minecraft/black_shulker_box__5be32c8f82ec` adds no components.

Existing lucky-only table: exactly one vanilla yellow shulker, tier_x tooltip, container loot `lbc:infinity_upgraded_luck`, custom data `{sigma_shulker:1}`, nonitalic name `infinite_shulker_upgraded_luck`; no lore/craftusable/model. Its item-def leaf is bare vanilla yellow shulker.

`infinity_upgraded` naturally regenerates one unopened upgraded black shulker in contents plus the full 26-roll `infinity_no_shulker` reward table, forcing each selected reward to count64/max_stack_size64 and removing max_damage. Base `infinity` similarly regenerates base shulker with normal 26-roll rewards. Keep actual container_loot generation; no custom GUI/reward reroll engine needed.

`infinity_upgraded_luck` generates 26 rolls of `lbc:luckyblock`, each count64/max_stack_size64/no max_damage; it DOES NOT put its replacement shulker inside contents. Legacy `advancement/generate_shulker.json` listens specifically to `minecraft:player_generates_container_loot` for this table, excludes spectators/Nexus/Imprinted, and rewards `function/other/give_shulker.mcfunction`: spawn one lucky-only shulker at opening PLAYER position, then immediately revoke `lbc:generate_shulker` for repeatability. Ordinary upgraded and sigma inventory achievements already exist in advancement resources (`inf_inf_shulker`/`sigma_shulker` predicates); crafting itself awards none explicitly.

Native resources contain that advancement but NO `data/lbc/function` reward implementation, and Java search finds no `generate_shulker`, `give_shulker`, `inf_shulker`, `inf_inf_shulker`, or `sigma_shulker` handler. Therefore lucky replacement is presently incomplete even though native container opening itself works. Add a narrowly typed native criterion/table-generation hook, with same spectator/dimension conditions and one replacement per actual loot generation; it must not fire on repeat opening of filled container, preview, checkpoint replay, or unrelated table. Prefer direct typed callback to a function interpreter. No suppression is present in source opening/replacement.

## Integration and dependency assessment

Use existing `LbcCraftingRecipe` Kind/register/matches/assemble and `LbcCraftingResultMixin` onTake path for two deterministic shapeless recipes; current enum/classification has neither upgrade. This supports native 2x2/3x3 output, vanilla Shift/debit/remainders, pure preview and take-only particles/sound. Use exact raw flags PLUS unopened container_loot table; do not identify by name/color or accept opened shulkers. Reuse existing root loot tables directly rather than inventing duplicate definitions. Add bounded ground handling to an appropriate typed service with existing match clock/epoch policy; no generic datapack executor. `LbcRerollCrafting` handles only retained-machine transformable donors and needs no changes. `LbcReforgeCrafting` random-result interception likewise is not this deterministic recipe owner.

Recursive literal loot-table reference audit from base/upgraded/lucky and both only-shulker roots: 3,643 reachable lbc tables, ZERO missing table files, only standard set_components/set_count/set_custom_data/set_lore/set_name functions. This proves file closure, NOT complete behavior of every generated luckyblock reward: lucky pool reaches broader unfinished dependencies including deferred TMT. Keep full LuckyWars/kit6 gates; do not claim sigma reward-family gameplay complete merely because all JSON exists. Existing `data/minecraft/loot_table/blocks/yellow_shulker_box.json` copies native custom_name/container/lock/container_loot from block entity; retain native placement/open/break behavior.

Questions only if not settled by parent context: authorize ordinary output slot instead of legacy spawned inventory result; atomic one-unit ground costs instead of source whole-entity deletion; exact native ground scan cadence and world-pause policy. Source itself supplies no rationale for changing stack limits or introducing any suppression. No RP work is required or authorized.
