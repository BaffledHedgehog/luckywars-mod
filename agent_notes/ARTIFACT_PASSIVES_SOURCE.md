# Mystical artifact passive source audit

This note records the legacy contract for unresolved `mystical:1` artifact types 6 and 8 and the imported loot payloads. The legacy server is read-only; the user's `all_in_one_1.21.8` resource pack is also read-only.

## Runtime behavior

- `tick.mcfunction:4` schedules `tick5t` every 5 ticks; `tick5t.mcfunction:2` dispatches `mobs_5t`, which calls `pre_player_5t.mcfunction:1` for players. `pre_player_5t` dispatches `players_5t` for non-spectators. `players_5t.mcfunction:1` calls `other/artifact/afkartifact5t` only while `afkartifact5t_active` is present and no `stopper_magic` marker is within distance 20.
- `items/changed_inventory/sel.mcfunction:87-88,101` sets or clears that active tag by checking `container.*` and `weapon.*` for any `mystical:1` artifact. This source selector covers carried inventory and either hand, not armor slots. The passive dispatch rechecks `Inventory` for the particular item and `tag=!spectator`. The new mod's wider inventory interaction scope is separately constrained by the current user requirement to include cursor and crafting slots.
- `other/artifact/afkartifact5t.mcfunction:6,8` calls metabolism for `type:8`, then effect cleanup for `type:6`, in that order. Both paths therefore run on the 5-tick player loop only, while the player is not suppressed by a `stopper_magic` marker within 20 blocks.
- `other/artifact/metabolism.mcfunction:1-2` applies hunger amplifier 2 and regeneration amplifier 1, each with duration 1 second, only if `hplvl < MaxHealth`.
- `other/artifact/nohp.mcfunction:1` runs `effect clear @s`, which clears every active effect. Since type 6 is called after type 8, a player carrying both has the type 8 effects removed on that same pass; type 6 wins their combined result.
- `load.mcfunction:23,96` defines `hplvl` as the built-in `health` criterion and `MaxHealth` as a dummy objective. `players_tick.mcfunction:2` writes `attribute @s minecraft:max_health get` to `MaxHealth` with `execute store result score` (unit scale); for positive values the attribute command result is truncated to an integer. `hplvl` is also an integer scoreboard value, so the comparison does not retain fractional precision. Inspection of the locally cached Paper 1.21.11 runtime (`versions/1.21.11/paper-1.21.11.jar`, `ServerPlayer` bytecode) confirms the health criterion stores `Mth.ceil(health + absorption)`. The legacy source folder name says `cringe1_21_4_skywars`, but the available server runtime is 1.21.11. `MaxHealth` is refreshed by the per-player tick function before the five-tick passive comparison uses its latest stored score.

Other passive artifact types share the 5-tick dispatcher but call different functions: type 3 calls `nofluid`, type 5 calls `noeffects1` (select effects only), and type 7 calls color logic conditionally. Types 1, 2 and 4 do not invoke another function from this dispatcher. Other `mystical` families include event-triggered effects, but they do not use these type-6/type-8 passive functions.

## Item design and payload

The selected entries are in `world/datapacksdisabled/LBC/data/lbc/loot_table/artifact.json`, each a weight-10, quality-0, count-1 `minecraft:repeating_command_block`. Both use tooltip style `lbc:tier_u`, model `lbc:mysticalartifact`, translated name `mystical_artifact` in `#C28CFF` with italic false, and the same two lore rows: aqua, non-italic `type` + `accessory`; then red, non-italic `mystical_artifact.lore`. Their custom data is `transformable:1,accessory:1,unresolved_artifact:1,mystical:1` plus `type:6` or `type:8`. The port changes only the server item id to `skywars2:mystical_artifact` and migrates the legacy loot-function schema.

The resource pack's `assets/minecraft/lang/en_us.json` retains `mystical_artifact` = “Mystical Artifact”, `mystical_artifact.lore` = “Unknown effect”, `artifact_6` = “Remove all effects”, and `artifact_8` = “Metabolism”. The datapack's `swrg_kit_integration/game/skills/looper_explain_artifact.mcfunction:7,9` substitutes the type-specific lore when the unresolved artifact is selected for explanation. No new language entries are needed for these imports.

## Evidence files

- `world/datapacksdisabled/LBC/data/lbc/loot_table/artifact.json`
- `world/datapacksdisabled/LBC/data/lbc/function/pre_player_5t.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/tick.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/tick5t.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/mobs_5t.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/players_5t.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/players_tick.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/items/changed_inventory/sel.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/artifact/afkartifact5t.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/artifact/metabolism.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/other/artifact/nohp.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/swrg_kit_integration/game/skills/looper_explain_artifact.mcfunction`
- `world/datapacksdisabled/LBC/data/lbc/function/load.mcfunction`
- Read-only resource pack: `assets/minecraft/lang/en_us.json`
