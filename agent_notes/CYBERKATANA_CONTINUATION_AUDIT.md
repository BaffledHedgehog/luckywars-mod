# Cyberkatana continuation audit

Read-only references: legacy datapack `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/` and RP `C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8`. Existing source notes are preserved in `CYBERKATANA_UPGRADE_SOURCE.md`.

## Findings

- The reachable continuation recipe is `cyberkatana` + any item carrying custom-data `govnoq:1` -> one `lbc:shit_katana`, through both dropped-item (`craft_ground`/`shitkatana`) and player 2x2 (`inventory_craft_check`/`shitkatana_inv`) paths. Ground path kills one nearest item entity per ingredient and spawns the result. Inventory path reduces matching stacks in slots 0–3. Generic crafting must therefore match exactly those two ingredient predicates, consume one each, and work in 2x2/3x3 with exact ingredients; reuse the existing `LbcCraftingRecipe` and result mixin infrastructure.
- There is no upgraded Cyberkatana item flag in the legacy output. `shit_katana` loot has `shit_katana:1` plus shared sword/ability flags; it drops the base `cyberkatana:1` and `govnoq:1`. `cyberkatanamk2` is a misleading advancement key: its icon is `techrunner_katana`, and its criterion is a 150-damage hit with that item. The named, reachable continuation is `shit_katana`, not an item called Cyberkatana Mk2.
- Base Cyberkatana loot is carrot-on-a-stick model `lbc:cyberkatana`, +5 damage/−2.4 speed, durability 25 in current Java. Legacy handler accumulates one charge per 10 invocations and applies nearby debuffs; active ability spends a charge for area lightning. Current project `LbcElectro.damaged` already narrows charging and the attack aura to successful melee, and `use` spends charges on area lightning. These already-ported abilities remain intact. The user's nonmagic/melee clarification changes their guards/triggers; it does not remove the aura or charged ability.
- `shit_katana` is a separate behavior path: its handler accumulates a charge per 7 invocations and launches 7 short yellow ray markers each invocation (plus 4 more on legacy skill 9); its charged use launches a spinning ray marker (TTL 200). Basic rays copy owner ID/team and exclude the owner/allies, but the damage helper uses a player-marker proxy; ultimate ray damage has no `by` attacker. Preserve attribution, team filtering, observer protection, and CPU-first bounded ticking if this behavior is ported. Do not use blanket creative/developer exclusions.
- Actual RP contains `assets/lbc/models/item/{cyberkatana,shit_katana}.json`, both handheld models with textures `assets/minecraft/textures/item/{cyberkatana,shit_katana}.png`; `assets/lbc/items/{cyberkatana,shit_katana}.json` bind the corresponding item models. RU/EN locale keys already include the two names, lore, and advancement strings, including `cyberkatana.lore.3` describing the combine hint. Avoid duplicate RP keys; the current project can reference these existing assets/keys.

## Author clarifications (user)

Conductor impact affects all nearby enemies, including those not standing in conductors:

> Сохранить воздействие на всех врагов рядом

The spinning beam passes through walls and ordinary magic shields:

> Да, сохранять оба свойства

Item identity and the broad `govnoq:1` ingredient family are established by
the reachable source and were preserved. The nonmagic continuation uses the
same successful-melee trigger policy as the base katana. See
[YELLOW_KATANA_PORT.md](YELLOW_KATANA_PORT.md) for implementation and limits.

No legacy datapack or resource-pack files were modified.
