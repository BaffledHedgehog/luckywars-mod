# Hole drops source audit — 22 October 2026

Bounded read-only source audit. Read project AGENTS.md, agent_notes/AGENTS.md and RESOURCE_PACK.md. No build, runtime, code, server, original datapack, RP or template changes. Source root: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc`. Active RP inspected read-only: `C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3/minecraft/resourcepacks/all_in_one_1.21.8`. Evidence is not runtime qualification.

## Dispatch and consumption

Subsequent native implementation and qualification are recorded separately in
[HOLE_DROPS_PORT.md](HOLE_DROPS_PORT.md). The audit below remains source evidence.

`function/items_tick_act.mcfunction:1` routes dropped entities tagged drop to `lbc:drops/specialdrops`. Its lines18/19 dispatch custom_data yama:1 to `lbc:drops/hole` and yama_old:1 to `lbc:drops/hole_old`. Both first execute at the token, then as `@p[gamemode=!spectator]` and at that player. Geometry uses player feet, not token position. No radius, thrower exclusion, ally filter, survival-only guard, centering or teleport exists. Parent task carries author-approved nearest ANY nonspectator/self/allies and WILD policy from cages; reuse that selection.

The specialdrops tail kills the original count1 item, otherwise stores count scaled0.99999. It consumes one unit per invocation even without a selected player. Reuse shared dispatch/debit. Neither hole summons actors, displays titles or establishes restoration/lifetime timers.

## Exact geometry

All fills are inclusive X/Z−1 through+1, relative to floored selected-player feet. Order below is source order. Each command replaces arbitrary prior blocks; these are not hollow/keep/destroy commands. Failed earlier commands do not prevent later commands.

| Function line | Relative Y | Block | Mode | Nominal cells |
|---|---|---|---|---|
| hole:1 | 0 through−15 | air | strict | 144 |
| hole:2 | −16 | cobweb | strict | 9 |
| hole:3 | −18 | oak_log | strict | 9 |
| hole:4 | −19 | cobblestone | strict | 9 |
| hole:5 | −17 | fire | strict | 9 |
| hole_old:1 | 0 through−15 | air | ordinary replace | 144 |
| hole_old:2 | −16 | cobweb | ordinary replace | 9 |
| hole_old:3 | −18 | soul_soil | ordinary replace | 9 |
| hole_old:4 | −17 | soul_fire | ordinary replace | 9 |

Normal180 nominal cells, old171. Old does NOT write a −19 support layer. All default states; oak logs vertical. No rotation, custom fire age or face properties.

## Native API semantics

Read local26.3 sources in `.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-d6f9bb6878/26.3/minecraft-merged-d6f9bb6878-26.3-sources.jar`: FillCommand, BlockInput, Block, FireBlock and SoulFireBlock. FillCommand uses default-state BlockInput, empty property set, null NBT, BlockPos.betweenClosed order, and input.place flags818 strict /258 ordinary. Each ordinary fill collects successful positions with OLD states, then calls updateNeighboursOnBlockSet after THAT fill. Strict has no deferred neighbor pass. Existing LbcCageDrops.fill mirrors this algorithm.

818 = clients2 + known-shape16 + suppress-drops32 + skip-BE-sideeffects256 + skip-on-place512.258 = clients2 + skip-BE-sideeffects256. BlockInput resolves neighbor shapes only without16; if resolution produces air, it restores the requested state. Both modes skip native old-container inventory ejection through256; never reuse chest-drop spill behavior. Ordinary neighbor updates can still produce native secondary changes/drops outside the replaced cells.

Strict512 skips FireBlock.onPlace, which normally schedules its initial30..39tick fire tick. Do not invent an initial fire tick or force logs to burn. Existing scheduled ticks and later external updates can still matter; strict is not a permanent freeze. Old soul fire retains ordinary placement/update behavior and soul-soil support.

Native FillCommand validates endpoint loading/build bounds and maximum block-modification gamerule before EACH complete fill. Do not clip invalid cuboids, abort the entire function after one failure or silently force-load chunks. Nominal counts are valid-placement geometry, not a promise at the world floor/chunk edge.

## Exact acquisition presentation

`loot_table/traps.json` ordinary entry (flag line178): repeating_command_block, count1, weight10, quality−10; tooltip_style lbc:tier_ff; item_model lbc:drop_trap; set_name one-element array with translate trap_hole, italic:false, color:yellow. set_lore replace_all exactly one row: translate trap_desc, italic:false, color:white. custom_data `{yama:1,drop:1}`.

`loot_table/legacy_luckyblock.json` old entry (flag1119): barrier/count1, empty set_components, custom_data `{yama_old:1,drop:1}`. NO custom name, lore, model or tooltip. Preserve undecorated OLD presentation unless author explicitly changes it.

`loot_table/imba/traps.json` normal entry (flag82): repeating_command_block/count1, weight0/quality−10, empty set_components, custom_data `{yama:1,drop:1,imba:1}`. NO decoration. No old entry occurs in these three bounded tables. Enclosing random reachability is not qualified. Two normal/old registrations suffice; IMBA is a normal behavior acquisition/presentation variant.

## RP evidence

assets/lbc/items/drop_trap.json model type model resolves lbc:item/drop_trap. assets/lbc/models/item/drop_trap.json parent minecraft:item/generated, layer0 minecraft:item/drop_trap. assets/minecraft/textures/item/drop_trap.png exists. No new model, texture or translation needed.

| Key | ru_ru | en_us |
|---|---|---|
| trap_hole | Ловушка: Огненная яма | Trap: Fire Pit |
| trap_desc | Активный Дроп. Для активации выбросте предмет. | Active Drop. Throw the item to activate. |

Keep source typo/style exactly. Old/IMBA entries do not request these decorations.

## Genuine risks

User contract requires actual thrower attribution through fall/fire/environmental consequences; source lacks ownership. Preserve thrower independently from nearest victim. No actors needed. WILD cause must cover ordinary/soul fire and falls caused by removed terrain: registering only fire blocks does not prove fall/void credit. Do not attribute unrelated later terrain changes to this token. Qualify strict container suppression, old regular neighbor updates, negative/fractional coordinates and independent per-fill failure at low Y/chunk boundaries.

Old undecorated barrier, normal strict fire and absent old−19 floor are clear preservation constraints, not open author questions. This audit claims no implementation/runtime completion.
