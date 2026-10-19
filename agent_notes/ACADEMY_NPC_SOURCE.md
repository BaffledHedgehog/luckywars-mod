# Legacy Magic Academy NPC source audit

Read-only audit of the old LBC datapack. Source links below point to the legacy server checkout; quoted snippets are short excerpts from those files. The Academy interaction is datapack advancement driven, and its reward function runs with the interacting player as `@s`.

## Spawn and lifecycle

The NPC pair is embedded directly in the 12x11x12 [`magic_academy.nbt`](<E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/structure/magic_academy.nbt>) structure (decoded with `nbtlib`):

- `minecraft:interaction`: structure `pos [6.0, 1.0, 10.5]` / `blockPos [6, 1, 10]`, width 3, height 3, response 1, tag `magic_academy` (`Invulnerable:0`).
- `minecraft:text_display`: `pos [6.0, 2.0, 10.0]`, translated `welcome_to_academy`, aqua, non-italic (`Invulnerable:0`).

The structure has no command blocks or other command-bearing block NBT. These entities are embedded in the structure, not created by the separate [`commands.mcfunction`](<E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/commands.mcfunction>), which summons another welcome display and a larger 10x6 interaction hitbox at relative offsets. No in-datapack caller for that separate function was found; it is not needed to explain the normal structure spawn path. Neither structure entity has equipment, owner/team tags, or invulnerability enabled. No NPC-targeting or NPC cleanup code was found; `leave_death.mcfunction` only revokes the player's transient interaction advancement.

The two structure placement paths are [`drops/magic_academy.mcfunction`](<E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/drops/magic_academy.mcfunction>), base at item block position `(-6, 0, -6)`, and [`drops/magic_academy_token.mcfunction`](<E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/drops/magic_academy_token.mcfunction>), base `(-6, -1, -6)`. The token path is called for a grounded `academy_token` from `items_tick_act.mcfunction`; the ordinary item path is called from `specialdrops.mcfunction` for custom data `magic_academy:1`. There is no per-player placement limit or same-location entity guard in `placeLegacyStructure`; it unconditionally calls `structure.place`. The migrated Paper handler in [`SwrgNativeService.java`](<E:/servers_mew/cringe1_21_4_skywars/plugins/luckywars/src/main/java/me/luckywars/swrg/SwrgNativeService.java>) preserves structure placement: the ordinary drop teleports the nearest non-spectator within 12 blocks and decrements one item, while the token places at `-6,-1,-6`, plays the teleport sound and witch particles, then removes the item. Thus ordinary stacked item entities can place once per item until depleted; the token handler consumes the entity on its first trigger.

## Interaction gate and player context

[`interacted_with_academy.json`](<E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/advancement/interacted_with_academy.json>) uses `minecraft:player_interacted_with_entity`. Its predicates require a non-spectator player outside `minecraft:nexus` and `minecraft:imprinted`, an item carrying custom data `{staff:1}`, and an entity of type `minecraft:interaction` with tag `magic_academy`. Its reward calls `lbc:other/magic_academy/interacted`. Therefore `@s` in the reward is the player; checks in `interacted` read `SelectedItem` (main hand), not offhand.

The first branch in [`interacted.mcfunction`](<E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted.mcfunction>) is keyed on the selected main-hand item's custom data:

```mcfunction
...{upgraded:1}... run tellraw @s {"translate":"already_upgraded"}
...{upgrade_available:1}... run function .../interacted_upgrade
...{upgradable:1}... run tellraw @s {"translate":"no_conditions"}
```

If none of `upgradable`, `upgrade_available`, or `upgraded` is set, it calls `interacted_2`. That function first prints `magic_academy_root`, then dispatches by item identity keys (for example `acidwand:1`) to the corresponding `can_upgrade/<item>` function so the player sees that item's requirement. If no supported identity key matches, it revokes the transient advancement and prints `cannot_be_upgraded`.

## Three state markers and the success path

These markers have separate jobs:

- `upgradable:1` means the item is eligible to have its challenge progress checked; it does not mean the challenge is complete. In `interacted`, this state gets the generic `no_conditions` response.
- `upgrade_available:1` is set by shared [`can_upgrade.mcfunction`](<E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/can_upgrade.mcfunction>) after the item-specific success logic calls it. The function writes `upgrade_available:1` and resets `upgradable:0`; it then tells the player `can_upgrade`. It does not itself award the upgrade.
- `upgraded:1` is written by [`interacted_upgrade.mcfunction`](<E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/interacted_upgrade.mcfunction>) after the player returns with `upgrade_available:1`. This path also sets `mode:1`, applies `lbc:add_upgrade_desc`, runs the keyed `upgrade/<item>` function, increments a challenge counter when applicable, and revokes the interaction advancement.

The requirement checks are item-specific and occur in the gameplay function which calls `can_upgrade` (casting while in fire, satisfying effects, score thresholds, kills, etc. vary by item). `can_upgrade/<item>` mainly supplies the matching `upgrade_condition_*` dialogue; the `upgrade/<item>` function supplies the success dialogue and item-specific modifications. For example [`can_upgrade/acidwand.mcfunction`](<E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/can_upgrade/acidwand.mcfunction>) says `upgrade_condition_acid`, while [`upgrade/acidwand.mcfunction`](<E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/magic_academy/upgrade/acidwand.mcfunction>) says `upgrade_succesful_acid` and applies `lbc:tiers/c`. The shared Academy interaction has no universal item/material cost or item deduction. Do not infer resource-pack `item_modifier/swrg_unlocks/academy_*.json` display changes as the Academy upgrade path: those are presentation/unlock modifiers, while the interaction path is the three custom-data markers above.

## Dialogue keys and style

The datapack sends translatable `tellraw` components instead of hard-coded prose: `welcome_to_academy`, `magic_academy_root`, `already_upgraded`, `no_conditions`, `can_upgrade`, `cannot_be_upgraded`, plus `upgrade_condition_<item>` and `upgrade_succesful_<item>`. The resource pack's Russian strings are in its read-only [`assets/minecraft/lang/ru_ru.json`](<C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8/assets/minecraft/lang/ru_ru.json>): `welcome_to_academy` tells players to right-click a wand to learn more; the success text is individualized lore with casual, humorous instructions. NPC floating text itself is aqua, non-italic; generic root response is green, requirement/success lines are yellow, and ready-to-upgrade response is aqua. Preserve existing keys and line formatting when porting.

The Paper plugin's `SwrgNativeService` has handlers for the Academy token and structure placement; the searched Java source does not show a handler for this tagged interaction entity. The entity-interaction contract above is from the datapack advancement/reward chain.

## Native resource preparation

Added [`ported/academy_token.json`](../src/main/resources/data/lbc/loot_table/ported/academy_token.json) using the existing native `modifier` format and item ID `skywars2:academy_token`. It preserves the legacy kit table's exact display data: item model `lbc:advancement_icon/academy`, white non-italic `academy_token` name and `academy_token.lore`, and custom data `{academy_token:1}`.

The source token table is [`kit/academy_token.json`](<E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/loot_table/kit/academy_token.json>). Legacy challenge-kit selection references it at `challenge_kit_select.mcfunction` (`loot give ... loot lbc:kit/academy_token` when `lbc.challenge.kit=1`); the old Paper integration also maps challenge-kit selection 1 to `lbc:kit/academy_token` in `CHALLENGE_KIT_SELECTION_LOOT`. Separately, the ordinary thrown Academy drop is a weighted entry in `loot_table/barriersnottraps.json`, marked `{magic_academy:1,drop:1}`; it is distinct from this challenge token table.

The new project's `processResources` uses an explicit include list. The integration now includes `data/lbc/loot_table/ported/academy_token.json` and the Academy structure. Challenge-kit selection and general random Lucky-drop routing remain separate migration tasks.

The already-copied native resource [`data/lbc/structure/magic_academy.nbt`](../src/main/resources/data/lbc/structure/magic_academy.nbt) is byte-identical to the legacy structure: DataVersion 4189, 12x11x12, with the same two entities and text component. Native 26.3 decoding and token-driven sliced placement passed the isolated Academy smoke. The resource itself remains unchanged; the native template manager applies data fixing and is prewarmed at server startup/reload.
