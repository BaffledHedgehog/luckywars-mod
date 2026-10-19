# Mystical artifact type 1:7 color effects: exact source audit

This note records the legacy datapack's static behavior for a later port. It does not claim that the behavior exists in the new mod or that all branches were runtime-tested. Preserve source values in a parity port; handle a proposed balance correction as a separate user decision rather than silently normalizing the source.

## Shared route and selection

`LBC/data/lbc/function/players_5t.mcfunction:1` invokes `other/artifact/afkartifact5t` every five ticks when the player has `afkartifact5t_active` and there is no `minecraft:marker` within 20 blocks carrying `stopper_magic`. `items/changed_inventory/sel.mcfunction:87-88,101` sets this broad active tag when any `mystical:1` item is in `container.*` or `weapon.*`, and removes it when neither search finds one.

Within `afkartifact5t.mcfunction:4`, the color branch requires a non-`spectator` tag state and an inventory item with custom data `mystical:1,type:7`. It dispatches `colors` if the block at feet (`~ ~-0.5 ~`) is in `#minecraft:colored`; a second command dispatches if the block at player position is in `#minecraft:in_colored`. Neither branch requires `unresolved_artifact`, an equipped/selected item, or a `nomagic_active` check. The shared 20-block marker is the explicit magic suppression on the scheduled route. `colors.mcfunction` then independently tests all 16 dye-color block tags at the execution position, so on the first route it is testing the block below the player.

## Exact block tag inputs

Legacy LBC defines `data/minecraft/tags/block/colored.json` with `replace:false` and 128 entries: 16 colors × stained glass, stained-glass pane, terracotta, glazed terracotta, wool, concrete, concrete powder, and shulker box. It defines `in_colored.json` with `replace:false` and 64 entries: 16 colors × carpet, bed, banner, and candle. Thus the four color families in `in_colored` select an effect only when the block occupies the player's current position; the eight families in `colored` select it from the block below.

Each of the 16 tags `data/minecraft/tags/block/{black,blue,brown,cyan,gray,green,light_blue,light_gray,lime,magenta,orange,pink,purple,red,white,yellow}.json` has 13 array entries. They contain that color's candle, stained glass, stained-glass pane, terracotta, glazed terracotta, wool, concrete, concrete powder, shulker box, a duplicate wool entry, carpet, bed, and banner. The duplicate wool is present in the source and has no behavioral effect. These are LBC-supplied files under the `minecraft` namespace: the local Minecraft 26.3 source jar's `net/minecraft/tags/BlockTags.java` and `net/minecraft/data/tags/VanillaBlockTagsProvider.java` contain no color tag keys/definitions. The source commands' `#minecraft:<color>` lookups therefore depend on the legacy datapack's own tag files, not a vanilla 26.3 color-tag contract. The LBC-specific `colored` and `in_colored` tags also remain required for dispatch.

## Color function table

All of these functions run on every qualifying five-tick dispatch. The durations below give each command duration argument in seconds and its native duration in ticks for non-instant effects; Minecraft amplifiers are zero-based. In this MC 26.3 `EffectCommands` implementation, non-instant durations are multiplied by 20. Instant effects use the supplied seconds integer directly as ticks, or default to 1 tick when the argument is omitted. `same_team` is the legacy predicate `data/lbc/predicate/same_team.json`: it compares target score `team_number` to fake player `*tempt` score `lbcID2`. Most opponent actions first copy the holder's `team_number` into that shared fake-player score; commands without that predicate do not inherit a team filter.

| Color | Holder effects / changes | Other targets and radius | Additional action / dependency |
|---|---|---|---|
| black | Clear blindness; night vision 300 s (6000 ticks), amp 0 | `@a`, non-spectator gamemode, 0.01–20 blocks, not same team: blindness 30 s (600 ticks), amp 0. Mobs tagged `#minecraft:mobs`, excluding players, within 20 and not same team: set `follow_range` base to 4 | Glow particle; mob attribute write has no restore/timer in this function |
| blue | Water breathing 30 s (600 ticks), amp 2; conduit power 30 s (600 ticks), amp 1 | None | Nautilus particle |
| brown | Saturation command duration 1 second maps to 1 tick for this instant effect, amp 1; add 1 to `food_count`; clear hunger | `@a`, non-spectator gamemode, 0.01–25 blocks, not same team: hunger 30 s (600 ticks), amp 1 | Requires scoreboard objective `food_count`; beef item particle and burp sound |
| cyan | Invisibility 300 s (6000 ticks), amp 1, hide particles; night vision 300 s (6000 ticks), amp 1, hide particles | `@e`, excluding tag `spectator`, 0.01–300 blocks, not same team: glowing 30 s (600 ticks), amp 0, hide particles | Glow-squid-ink particle; unusually wide 300-block target search |
| gray | Resistance 10 s (200 ticks), amp 4 | None | Two barrier block-marker particles |
| green | Add 3 XP | `@a`, non-spectator gamemode, 0.01–200 blocks, not same team: remove 3 XP | Dust particle |
| light_blue | Slow falling 300 s (6000 ticks), amp 0 | `@e`, excluding tag `spectator`, 0.01–20 blocks, not same team: levitation 2 s (40 ticks), amp 5 | Cloud particle |
| light_gray | Resistance 300 s (6000 ticks), amp 2 | `@e`, excluding tag `spectator`, 0.01–15 blocks, not same team: clear resistance | Iron-block particle |
| lime | Jump boost 300 s (6000 ticks), amp 2; clear holder levitation | `@e`, excluding tag `spectator`, 0.01–15 blocks, not same team: levitation 1 s (20 ticks), amp 0, hide particles | Sneeze particle |
| magenta | Add 20 to holder `mana` | `@a`, 0.01–35 blocks, with `mana` score ≥20, not same team: subtract 20 mana | Witch particle; requires the custom `mana` scoreboard system and its score initialization/semantics |
| orange | Fire resistance 300 s (6000 ticks), amp 0 | `@e`, excluding tag `spectator`, 0.01–30 blocks, not same team: at each target fill a 3×3×3 cube with fire, replacing `#minecraft:airs` | Lava particle; mutates nearby world blocks every five ticks |
| pink | Instant health (command omits duration/amplifier; default 1 tick, amp 0); regeneration 300 s (6000 ticks), amp 1; clear poison, wither, and instant damage | `@e`, excluding tag `spectator`, 0.01–20 blocks, not same team: wither 10 s (200 ticks), amp 1 | Heart particle |
| purple | No potion effect on holder | `@e` tagged `#minecraft:mobs`, excluding tag `spectator`, 0.01–20 blocks, not same team: raid omen 30 s (600 ticks), amp 40, hide particles | Particles/sound then `spreadplayers`: if `no_borders != 0`, local spread radius 200 (Nether uses `under 120`); if `no_borders == 0`, spread around world origin radius 190 (Nether `under 120`). Runs every five ticks while selected block condition remains true |
| red | Strength 300 s (6000 ticks), amp 0; clear weakness | `@e`, excluding tag `spectator`, 0.01–15 blocks, not same team: weakness 60 s (1200 ticks), amp 0 | Crit particle |
| white | Speed 300 s (6000 ticks), amp 1; clear slowness | `@e`, excluding tag `spectator`, 0.01–25 blocks, not same team: slowness 30 s (600 ticks), amp 1, hide particles | End-rod particle |
| yellow | Haste 300 s (6000 ticks), amp 10; clear mining fatigue | `@e`, excluding tag `spectator`, 0.01–25 blocks, not same team: mining fatigue 60 s (1200 ticks), amp 2 | Falling-honey particle |

Entity selectors that say `tag=!spectator` differ from player selectors that say `gamemode=!spectator`; do not collapse those into one rule without matching the new project's observer check and non-player target behavior. Self-only colors do not write `*tempt` because they do not use `same_team`.

## Source dependencies and behavior questions

- Team exclusion depends on the shared fake-player score `*tempt lbcID2` and each target's `team_number`. Every color function that uses `predicate=!lbc:same_team` first writes the holder score. Whether every candidate target has a valid `team_number` score is a runtime/data initialization dependency.
- Brown depends on `food_count`; magenta depends on `mana`. These are custom scoreboard systems, not vanilla potion behavior. Verify availability and semantics in the new project's native systems before implementing those color branches.
- Purple depends on `swrg.math` score `no_borders`, the `lbc:the_nether` predicate, and the world-border policy. It teleports the holder every five ticks while the artifact's dispatch condition remains true, which can prevent normal movement and make the holder difficult to control. This is an early user-choice point: strict source parity preserves repeated relocation; correcting it changes the source behavior and should be approved explicitly before implementation.
- Orange repeatedly creates fire in opponent-centered volumes, and cyan repeatedly applies glowing out to 300 blocks. These are source behaviors with broad effects. Preserve for exact parity or present a scoped behavior-change choice; do not silently narrow them.
- Black sets mob follow range to 4 without expiry/restoration. Brown repeatedly modifies hunger/food scores, green repeatedly drains XP, magenta repeatedly drains mana, and purple repeats world relocation. Their cadence can dominate the nominal duration or effect wording; this is source behavior, not evidence of intended balance.
- No function converts or recolors the trigger block. The trigger block selects the color function. Orange is the only color function that changes blocks, placing fire in air near opponents.

A future implementation should retain the source selection and five-tick cadence only if the product wants source parity; use the project's `game.isObserver(player)` for its observer policy, and verify each referenced scoreboard/predicate/world operation against available native systems. This is implementation guidance, separate from the source behavior above. No server, old datapack, build output, root notes, resource-pack files, or code were edited for this audit.
