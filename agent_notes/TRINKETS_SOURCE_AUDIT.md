# Trinkets: bounded source audit

Read-only audit of the old `LBC` datapack plus the matching plugin selection code. The old datapack defines the behavior; current plugin `Loadout.giveTrinkets` is still a TODO, and this audit does not establish a completed Fabric/ Ignite implementation.

## Selector IDs and item emission

`function/swrg_kit_integration/game/trinket_select/select_1.mcfunction` maps the picked numeric ID to a loot table and issues `loot give @s`. The plugin duplicates this table in `SwrgNativeService.TRINKET_SELECTION_LOOT`. Each referenced loot table emits one vanilla item with max stack size 99, translated name/lore, and `custom_data.trinket`; most use `minecraft:repeating_command_block` plus an `lbc:*` item model, while ID 16 is plain `minecraft:stone` with no item-model component. The custom-data string is the runtime dispatch key.

| ID | Loot table | Emitted item; item model | Runtime `custom_data.trinket` |
|---:|---|---|---|
| 1 | `blaze` | repeating command block; `lbc:trinket_0` | `blaze` |
| 2 | `ghast` | repeating command block; `lbc:trinket_1` | `ghast` |
| 3 | `penis_gun` | repeating command block; `lbc:trinket_2` | `penis_gun` |
| 4 | `drojjevoe_odealo` | repeating command block; `lbc:trinket_3` | `drojji` |
| 5 | `blood_anal_boom` | repeating command block; `lbc:blood_anal_boom` | `blood_anal_boom` |
| 6 | `mandoahuet` | repeating command block; `lbc:trinket_5` | `mandoahuet` |
| 7 | `bedrcock` | repeating command block; `lbc:trinket_6` | `bedrcock` |
| 8 | `nether_plug` | repeating command block; `lbc:trinket_7` | `nether_plug` |
| 9 | `volos` | repeating command block; `lbc:trinket_8` | `volos` |
| 10 | `intercal` | repeating command block; `lbc:trinket_9` | `intercal` (+ `govnoq:1`, `craftusable:1`) |
| 11 | `cocktail` | repeating command block; `lbc:trinket_10` | `cocktail` |
| 12 | `banangay` | repeating command block; `lbc:trinket_11` | `banangay` |
| 13 | `fourier` | repeating command block; `lbc:trinket_12` | `fourier` |
| 14 | `rocket_spell` | repeating command block; `lbc:trinket_13` | `rocket_spell` |
| 15 | `twilight_sparkle` | repeating command block; `lbc:trinket_14` | `twilight_sparkle` |
| 16 | `gneiss_stone` | `minecraft:stone`; no model component | `gneiss_stone` |
| 17 | `hedgehog_gauntlet` | repeating command block; `lbc:hedgehog_gauntlet` | `hedgehog_gauntlet` |
| 18 | `snow_gem` | repeating command block; `lbc:snow_pearl` | `snow_pearl` |
| 20 | `ice_gem` | repeating command block; `lbc:snow_gem` | `ice_gem` |
| 21 | `mana_gem` | repeating command block; `lbc:mana_gem` | `mana_gem` |
| 22 | `poly_gem` | repeating command block; `lbc:poly_gem` | `poly_gem` |
| 23 | `glass_shard` | repeating command block; `lbc:glass_shard` | `sunprism` |
| 24 | `psycho_soul` | repeating command block; `lbc:psycho_soul` | `psycho_soul` |
There is no ID 19 entry. IDs 4, 18, and 23 differ between the selector/loot-table name and the runtime dispatch key; code that checks the loot table name as the custom-data value will miss those effects.

## Selection, runtime gates, and slot semantics

- At game start, `bedwars/game/start.mcfunction` calls selection only when global `#lbctrinket swrg.math` is at least 1. Respawn does the same in `bedwars/player/respawn.mcfunction`. The selector reads up to `#lbctrinket` entries from the three packed per-player scores `lbc.trinket1..3` (four two-digit values per score; cursor/selection values are packed alongside them).
- Selection issues the loot-table grant once for each selected value. Duplicate values are allowed by the plugin perk model and produce duplicate item grants.
- The datapack then scans hotbar inventory slots 0–8 for any item whose custom data has `trinket`, moves each such item to inventory slots starting at 4, and clears its old hotbar slot. This is storage/organization, not an equipped-slot rule: effect checks use `clear @s *[custom_data~{trinket:"..."}] 0`, which counts matches anywhere in player inventory. No trinket armor/offhand slot is required.
- Runtime gates are explicit: the tick dispatcher, sword/bow/damage dispatchers, and other hooks are guarded by global `#lbctrinket >= 1`. The individual lookup then requires at least one matching inventory item (`tmp matches 1..`).
- Duplicate trinkets are not universally boolean. Dispatch calls each effect once, but chance formulas that read `tmp` scale with the matching item count. In the selected event batch: Gneiss/Snow chance is `0.08 + 0.02 × count`, Intercal is `0.10 + 0.05 × count`, and Ice Gem is `0.09 + 0.01 × count`.
- At match start, `bedwars/game/start.mcfunction` grants the configured selection. Bedwars respawn (`bedwars/player/respawn.mcfunction`) clears the inventory and re-grants trinkets, so that mode restores configured trinkets after death. The separate general `lbc/function/respawn.mcfunction` grants kits/skills/challenge kit but has no trinket selection call; SkyWars-mode death therefore has no datapack re-grant path located in this audit. The datapack search found no `gamerule keepInventory` setter, so whether previously held items are retained/dropped depends on server configuration and cannot be established from these source paths.

## Compact effect batch with direct native effects

These are the best bounded candidates from the hit/damage dispatchers for a small port batch. Their only per-player checks live in existing damage/sword event callbacks; do not add a per-tick scan for these. The trinket gate is global `#lbctrinket >= 1`, then each dispatcher counts the matching `custom_data.trinket` in inventory. `tmp` is that count. The chance runner is one macro random predicate call; statements after that call in a function continue normally, even when the random check fails.

| ID | Hook, chance, effect path, nuance |
|---:|---|
| 16 Gneiss Stone | `items/damaged` → `damaged_get_trinket` checks `gneiss_stone` → `gneiss_stone` computes `(.08 + .02 × count)` chance per damage callback → universal chance runner → `gneiss_stone_1`. Success gives the owner Resistance for 2 seconds, amplifier 4, plus stone sound/particles. One copy: 10%; each additional copy adds 2 percentage points. Compact native status effect; no spell/raycast dependency. |
| 10 Intercal | `items/swords` → `swords_get_trinket` checks `intercal` → `intercal` computes `(.10 + .05 × count)` chance per `items/swords` callback → universal chance runner → `intercal_hit`. The `items/swords` callback is awarded by `advancement/gowno.json` on `player_hurt_entity` when damage is not blocked and is not tagged `minecraft:arrow_only`; it therefore covers non-arrow player attacks, not only swords. Success gives Wither 10 seconds, amplifier 0, to non-spectator, non-team mobs within 10 blocks and plays sound. One copy: 15%; each extra copy adds 5 percentage points. If global LBC skill is enabled and the player’s selected skill is 15, a second conditional in `intercal_hit` gives the same targets Wither 3 seconds, amplifier 3. This is direct native effects and a single nearby-entity pass on success. |
| 18 Snow Gem | `items/swords` → `swords_get_trinket` checks `snow_pearl` → `snow_pearl` computes `(.08 + .02 × count)` chance per sword-hit callback → universal chance runner → `snow_pearl_1`. Success applies `workingitems/icesword/pre` to non-team entities with `HurtTime:10s` within 7 blocks; plays snow sound. One copy: 10%; +2 percentage points per extra copy. `icesword/pre` adds 10 cold, sets `cold_cd=5`, checks the freeze thresholds, and emits snow particles. The downstream freeze subsystem is in `other/freeze/*`; this is a direct freeze mechanic, not SpellEngine. |
| 20 Ice Gem | `items/damaged` → `damaged_get_trinket` checks `ice_gem` → `ice_gem` computes `(.09 + .01 × count)` chance → universal chance runner → `ice_gem_1`; one copy: 10%, +1 percentage point per extra copy. `ice_gem.mcfunction` ends after the chance-runner call; the snow-break sound, team lookup, 12-block freeze AoE, and particles are all in `ice_gem_1` and therefore occur only on a successful roll. |

The old dispatcher makes these event-based, inventory-backed effects; preserving this event-driven shape fits the project CPU priority better than polling inventories every tick. Duplicate copies do not call the effect function multiple times; where chance math uses `tmp`, the inventory count changes the chance. Gneiss is dispatched only from `damaged_get_trinket` in the inspected source; the generic trinket tick dispatcher checks Poly Gem and Sunprism, and contains no Gneiss call. Ice Gem's freeze behavior is chance-gated in `ice_gem_1`, with no unconditional continuation in `ice_gem`. Intercal/Gneiss/Snow/Ice are small enough to implement from their effect paths above. Other apparent candidates inspected were less suitable for this compact batch: ID 9 Volos marks arrows and then enters `projectiles/pre`/custom raycast-blindwand logic; IDs 1–2 Blaze/Ghast launch custom fireball/firework paths; ID 4 Acid uses acid raycast markers; ID 23 Sunprism uses upgraded raycast; ID 24 Psycho Soul adds NoAI and soul-ray marker handling.

## Source paths

Old datapack root: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`.

- Selector/emission: `function/swrg_kit_integration/game/trinket_select/select.mcfunction`, `select_1.mcfunction`, `replace_trinket_to_slot.mcfunction`.
- Lifecycle/gates: `function/bedwars/game/start.mcfunction`, `function/bedwars/player/respawn.mcfunction`, `function/players_tick.mcfunction`, `function/items/trinket_effects/tick.mcfunction`, `function/resets.mcfunction`.
- Item definitions: `loot_table/trinket/*.json` (all emitted vanilla item/model/name/lore/custom-data fields).
- Selected effect paths: `function/items/trinket_effects/{tick,gneiss_stone,gneiss_stone_1,intercal,intercal_hit,snow_pearl,snow_pearl_1,ice_gem,ice_gem_1,func_with_chance_universal}.mcfunction`; damage dispatch `function/items/damaged.mcfunction` and `function/items/damaged_get_trinket.mcfunction`; sword dispatch `function/items/swords.mcfunction` and `function/items/swords_get_trinket.mcfunction`; direct hit advancement `advancement/gowno.json`; freeze helpers `function/workingitems/icesword/pre.mcfunction`, `function/other/freeze/{check,add_cold,1..10,full}.mcfunction`.
- Plugin mirror: `E:/servers_mew/cringe1_21_4_skywars/plugins/luckywars/src/main/java/me/luckywars/swrg/SwrgNativeService.java` (selection-loot map and selection application); `.../game/loadout/Loadout.java` (`giveTrinkets` still TODO).
- Ignite source: `E:/servers_mew/cringe1_21_4_skywars/mods/luckywarsignite/src/main/java` has no trinket selection/effect implementation located by targeted search. This is a bounded source search, not proof of absence from generated artifacts or runtime.








