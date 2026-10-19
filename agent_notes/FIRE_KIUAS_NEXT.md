# Fire + Kiuaskivi → Sun Arcanum: source notes

Scope: read-only tracing against the legacy LBC datapack, Paper/Ignite sources, and current native helpers. This is behavior research, not a completed port.

## Live entry points and costs

- Craft result is `custom_data.sun_arcanum:1`, the `lbc:the_sun` item model, tier A, via `lbc:loot_table/tuntijaa_fire_kiuas.json:7-19, 93-95`. Ground recipe dispatch is `lbc:function/other/craft_ground.mcfunction:21`; inventory recipe is `lbc:function/other/craft/inventory_craft_check.mcfunction:21`; both call `lbc:function/other/sauvojen_tuntija/crafted_kiuaswand{,_inv}.mcfunction` (ground consumes nearest Kiuaskivi item; inventory decrements matching `stfire` and `kiuaskivi` slots).
- These functions are live on the datapack schedule: `lbc:function/tick5t.mcfunction:1-2` → `mobs_5t.mcfunction:9` → `pre_player_5t.mcfunction:1` → `players_5t.mcfunction:13,15`. Mainhand and offhand item paths call `items/arcanums/5true/select{,2}.mcfunction`, then `sun{,2}.mcfunction`.
- Standalone Kiuaskivi (`custom_data.kiuaskivi:1`) runs from main/offhand `items/arcanums/select{,2}.mcfunction:7-8`. Config is **1000 activate + 10 per game tick** (`lbc:function/config.mcfunction:14-15`; selector runs each tick through `players_tick`). It grants Fire Resistance 30s and transforms `kiuaskivi_to_air` blocks to air, then `kiuaskivi_to_fire` blocks to fire in the player-centered fill (`items/arcanums/kiuaskivi_tick.mcfunction:1-6`). When held in both hands, each call has a 5% chance of the large `big_fire_fill` (±15) (`kiuaskivi_tick.mcfunction:4`; `big_fire_fill.mcfunction:1-2`).
- Sun Arcanum has no selectable modes. Config is **1500 activate + 100 per 5-tick pass** (400 mana/sec; `config.mcfunction:16-17,76-77`). Its tick gives Fire Resistance 120s, places `lbc:the_sun` at `~-13 ~-13 ~-13`, charges mana, and turns eligible projectiles in distance 8..25 into lava (`items/arcanums/5true/sun_tick.mcfunction:1-8`, `projectile_boom.mcfunction:1-2`). On-fire 10 damage within 15 blocks requires Sun in both hands and excludes teammates (`sun_tick.mcfunction:3`).

Paper/Ignite Java references found for Kiuaskivi are unlock/menu registration and tag lifecycle (`plugins/luckywars/src/main/java/me/luckywars/perk/PerkRegistry.java:69`, `swrg/SwrgNativeService.java:313-314,588`); no Java behavior implementation for Kiuaskivi/Sun was found. The live ability behavior above is the datapack schedule path.

## Reusable native helpers

- `MapManager.prepareSpellTemplates` / `placeLoadedTemplate` already preload and place templates in loaded chunk slices (`src/main/java/me/skywars2/game/MapManager.java:747-760,999-1018`); `LbcStonekivi` has a shared bounded-work queue and spell-template slice flow (`src/main/java/me/skywars2/game/LbcStonekivi.java:30-44,118-152`).
- `LbcIndirectKills.worldChanges` carries owner attribution across block edits (`src/main/java/me/skywars2/game/LbcIndirectKills.java:218-228`); `LbcWands.target` applies ally, observer, and protected-NPC guards (`src/main/java/me/skywars2/game/LbcWands.java:917-930`). `LbcClouds` already has native tag-driven block replacements including fire/lava with owner-aware world changes (`LbcClouds.java:117-126,253-274`), though its tags differ from the Kiuaskivi tags. No dedicated Sun/Kiuaskivi native service exists in `game`.

## Clarify before behavior changes

- Holding Kiuaskivi in both hands runs both selector branches, so it can charge 10 mana twice per tick and roll the 5% large-fire chance twice (`select.mcfunction:7`, `select2.mcfunction:7`, `kiuaskivi_tick.mcfunction:4`). Confirm whether the double cost/chance is intended.
- Offhand Sun (`sun2.mcfunction:2`) calls `sun_tick`, while a separate `sun_tick_left.mcfunction` exists without a caller. `sun_tick`'s damage selector requires both hands and attributes to `@p[tag=sun_active]`; offhand-only use therefore cannot deal its stated damage. With Sun in both hands, main/offhand paths each place the template and charge a tick. Confirm whether the orphan left handler and duplicated processing are intentional.
- Kiuaskivi's fire tag deliberately includes broad tags such as `#minecraft:mineable/axe`, `#minecraft:mineable/hoe`, and `#minecraft:replaceable` (`minecraft/tags/block/kiuaskivi_to_fire.json:3-12`), so the fire fill can replace solid blocks such as wood. Confirm the intended destructive range when porting.
