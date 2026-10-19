# Mystical artifact type 1:7: color aura source audit

This is the historical read-only source audit after type 1:3 liquids. Findings below describe the old datapack. Type 1:7 has since been migrated within the boundaries in [ARTIFACT_COLORS_PORT.md](ARTIFACT_COLORS_PORT.md); current server evidence is in [SERVER_TESTS.md](../SERVER_TESTS.md).

## Dispatch and gate

`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/players_5t.mcfunction:1` runs the shared `afkartifact5t` function every five ticks for holders tagged `afkartifact5t_active`, unless a `stopper_magic` marker is within 20 blocks. The active tag is refreshed from any `mystical:1` item in `container.*` or `weapon.*` by `items/changed_inventory/sel.mcfunction:87-88,101`.

`function/other/artifact/afkartifact5t.mcfunction:4-5` requires `tag=!spectator` and an inventory item with `mystical:1,type:7`. It dispatches `colors` when the block at the player's feet (`~ ~-0.5 ~`) belongs to `#minecraft:colored`, or when the block at the player's position belongs to `#minecraft:in_colored`. The second condition lets a player standing inside one of those blocks activate the effect. It does not require `unresolved_artifact` or `nomagic_active`; the shared stopper marker is the explicit aura veto.

## Block tags and color selection

`data/minecraft/tags/block/colored.json` has `replace:false` and 128 explicit blocks: 16 colors each of stained glass, stained-glass panes, terracotta, glazed terracotta, wool, concrete, concrete powder, and shulker boxes. The separate `in_colored` tag has 64 values: 16 colors each of carpet, beds, banners, and candles. `colors.mcfunction` checks the legacy-provided `#minecraft:<color>` tags for the 16 dye colors and invokes the matching color function. Each source color tag has 13 values; these are datapack definitions, not assumed vanilla tags in 26.3. This includes all 16 colors: black, blue, brown, cyan, gray, green, light blue, light gray, lime, magenta, orange, pink, purple, red, white, yellow.

The full effect table and dependencies are recorded in
[ARTIFACT_COLORS_EFFECTS.md](ARTIFACT_COLORS_EFFECTS.md).

There is no block recoloring/conversion: the colored block only selects one of 16 effect functions. The color-specific functions apply different self effects, opponent effects, particles, and in some cases world/player actions. Their exact source is in `function/other/artifact/colors/{color}.mcfunction`. Several behaviors deserve explicit review before porting:

- `cyan.mcfunction:4` applies glowing to non-spectator entities within 300 blocks, a much wider radius than most color effects.
- `orange.mcfunction:4` executes a ±1 fire fill at each opposing player, changing blocks around targets.
- `purple.mcfunction:5-8` repeatedly spreads the holder's position (radius 200, or 190 when `swrg.math` score `no_borders` is 0; Nether calls use `under 120`). The function is dispatched every five ticks while the holder stands on/inside a matching block.
- `green.mcfunction:3` removes XP from opposing players within 200 blocks; `magenta.mcfunction:3` drains 20 mana from eligible players within 35 blocks.
- Many colors use `*tempt lbcID2 = @s team_number` and `predicate=!lbc:same_team`; correct team filtering depends on that shared scoreboard value being set immediately before the target operation.

The functions' effect durations, amplifiers, target selectors, and radius values should be copied from their source lines, including odd values, unless a deliberate behavior correction is authorized. Since dispatch is every five ticks, duration-1 effects and repeated area/world operations may be intentional pulse mechanics or source defects; static source alone cannot establish the intended balance.

## Candidate port boundary and dependencies

A behavior port must preserve the non-spectator and type 7 item gate, shared 20-block `stopper_magic` veto, block-underfoot and `in_colored` tests, 16 color mapping, target/team filtering, and each color-specific action. Use `game.isObserver(player)` for the new project's observer distinction. Resolve the actual `in_colored` tag values and exact effects of all color functions before implementing; this audit identified the dispatch and notable side effects but does not normalize every command into a full effect table.

Be careful with repeated side effects: color checks happen on each five-tick dispatch, so purple spreading, orange fire placement, XP/mana drains, and status applications can recur. Do not replace the explicit colored/in-colored tags with a broad material or dye inference unless that expansion is a conscious behavior change. No resource or implementation files were changed for type 1:7 in this batch.
