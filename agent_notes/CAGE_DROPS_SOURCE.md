# Cage drop source audit — 22 October 2026

Bounded read-only source audit. No gameplay code, build, server, original datapack, RP or template changes. Read project AGENTS.md and agent_notes/AGENTS.md / RESOURCE_PACK.md. Source root: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc`. Active RP inspected: `C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3/minecraft/resourcepacks/all_in_one_1.21.8` (current task path; older RESOURCE_PACK.md names 26.3(1)). Source text is evidence, not runtime qualification.

## Dispatch and token presentation

Subsequent author rulings and native qualification are recorded in
[CAGE_DROPS_PORT.md](CAGE_DROPS_PORT.md): nearest any nonspectator, WILD hazards,
and an upward-only shaft. Questions below describe the earlier audit state.

`function/items_tick_act.mcfunction:1` invokes `lbc:drops/specialdrops` for an item entity tagged `drop`. `function/drops/specialdrops.mcfunction:20..25` dispatches the four flags below and `obsidiantrap`. Each dispatch executes at the dropped item, selects `@p[gamemode=!spectator]`, then executes at that player. There is no distance cap, thrower exclusion, survival-only guard, or ally filter in these source commands. All coordinates below are relative to that selected player's feet, not the item. A nearby thrower may therefore trap themselves. No teleport or centering of the player is present.

| Full custom_data | Function ID | Name translate key |
|---|---|---|
| `{lookupanvil:1,drop:1}` | `lbc:drops/anvilcage` | `trap_anvil` |
| `{lookupfireball:1,drop:1}` | `lbc:drops/tntcage` | `trap_fireball` |
| `{lookuppotions:1,drop:1}` | `lbc:drops/potioncage` | `trap_potion` |
| `{lookuplava:1,drop:1}` | `lbc:drops/lavacage` | `trap_lava` |
| `{obsidiantrap:1,drop:1}` | `lbc:drops/obsidiantrap` | `trap_obsidian` |

Ordinary definitions: `loot_table/traps.json`, flag lines222/794/838/266/442 respectively. Every entry: `minecraft:repeating_command_block`, count1, weight10, quality−10, `minecraft:item_model="lbc:drop_trap"`, `minecraft:tooltip_style="lbc:tier_ff"`. Name is a one-element component array with the listed translate key, `italic:false,color:"yellow"`; lore replaces all with exactly one component `{"translate":"trap_desc","italic":false,"color":"white"}`. No new key or rewritten translation is needed.

`loot_table/imba/traps.json` also defines all five with `imba:1`, weight0/quality−10, repeating command block/count1 and empty set_components; no name/lore/model there. `loot_table/legacy_luckyblock.json` defines lookupanvil/lookuplava/obsidiantrap as barrier/count1, empty set_components and ordinary two flags; no name/lore/model. These differing definitions must not silently be presented as ordinary decorated tokens. This audit does not qualify their enclosing loot-table random reachability.

`specialdrops` finishes with `kill @s[nbt={Item:{count:1}}]`, then stores Item.count scaled0.99999. Dispatch switches executor only within each execute command; the tail remains the original dropped item. Thus source consumes one unit per invocation even if no nonspectator was selected. The four cage functions title the selected player with literal single-space title and subtitle `{"translate":"look_up"}`; no title timing override exists.

## Exact main cage geometry and payload

All four first fill inclusive relative cuboids `~1 ~-1 ~1` to `~-1 ~-1 ~-1` with `minecraft:stone_bricks` (9 floor blocks), then `~1 ~ ~1` to `~-1 ~3 ~-1` with `minecraft:iron_bars` (36 blocks). These are solid fills, not hollow-box mode. The center column is subsequently cleared. Relative block coordinates are floored command positions; a player's fractional Y is retained for entity spawn positions. Payload X/Z is separately floored then centered by `execute align xz positioned ~0.5 ~ ~0.5`.

`lbc:drops/anvilcage` (`function/drops/anvilcage.mcfunction:1..8`): two center-column air fills from current feet to absolute Y255, then to absolute Y319. Two `minecraft:falling_block` at relative Y+40 and Y+42. Each exact payload: `BlockState:{Name:"minecraft:anvil"},Time:1,DropItem:1b,HurtEntities:1b,FallHurtMax:35,FallHurtAmount:2.0f,Motion:[0.0d,0.0d,0.0d]`. Not two directly placed anvil blocks, and no lifespan/owner override is supplied.

`lbc:drops/tntcage` (`function/drops/tntcage.mcfunction:1..7`): same255/319 shafts; despite filename, exactly ONE `minecraft:fireball` at Y+70, `ExplosionPower:4,Motion:[0.0d,-0.05d,0.0d]`. No TNT, fuse, custom acceleration, owner or tags are supplied.

`lbc:drops/potioncage` (`function/drops/potioncage.mcfunction:1..47`): same255/319 shafts; exactly40 `minecraft:splash_potion`, one at every Y+50 multiple through+2000, all summoned immediately in one invocation. Every Item is count1 `minecraft:splash_potion`, potion_contents `custom_color:7543569`, one `minecraft:instant_damage` custom effect with duration20. Amplifier0 at +50..+450 (9), +1050..+1450 (9):18 total. Amplifier1 at +500..+1000 (11), +1500..+2000 (11):22 total. No Motion, tags, Owner, persistence or artificial timer supplied. Do not flatten to40 identical amplifiers or a timed spawn loop.

`lbc:drops/lavacage` (`function/drops/lavacage.mcfunction:1..6`): clear center from feet to Y+2 inclusive (3 air blocks), then place ONE `minecraft:lava` at Y+3, replacing the center top bar. No tall shaft, entities, fluid-state override, cleanup timer or special damage command. With normal valid placement final structure is9 stone bricks,32 bars,3 center air,1 lava.

For other main cages, at ordinary Y below255 final cage region is9 floor bricks and32 bars, with its4 center bars removed by the shaft. Shafts remove arbitrary existing blocks and extend through the cage roof; they do not restore anything later. At unusual heights the absolute endpoints can produce downward clearing. Fill failure due world bounds or command limits is not handled; subsequent commands still run. Payloads above build height (especially potion+2000) are source commands, not proven surviving entities. Native load/despawn/projectile limits and real fall timing require qualification, not invented compensating lifetimes.

## Obsidian and misleading empty cage name

`lbc:drops/obsidiantrap` (`function/drops/obsidiantrap.mcfunction:1..6`) IS reachable from the fifth ordinary flag above. Fill3×4×3=36 blocks with `minecraft:obsidian strict`, from offsets(+1,−1,+1) to(−1,+2,−1). Replace center at Y0/Y1 with `minecraft:water` (2), replace four horizontal cardinal side blocks at Y1 with `minecraft:glass strict`: (+1,1,0),(0,1,+1),(−1,1,0),(0,1,−1). Nominal final counts30 obsidian,4 glass,2 water; no title, air shaft, entities, teleport, timer or cleanup. Preserve/clarify source strict-update semantics rather than assuming normal updates.

There is NO `emptycage` flag/loot token or `specialdrops` branch in the bounded LBC function/loot-table search. Existing `lbc:drops/empty_cage` is invoked by `function/other/troll_box/4.mcfunction:2`: `execute as @a[gamemode=!spectator] at @s unless dimension nexus run function lbc:drops/empty_cage`; selection originates in `lbc:other/troll_box/select` when random6=4. It affects all nonspectators outside dimension nexus, independent of dropped cage token proximity. It is not empty: same stone/bar structure, clear center shaft to319 THEN255 (reverse order), two anvil falling blocks +40/+42 with `FallHurtMax:1,FallHurtAmount:1.0f`, otherwise same anvil NBT, and same look_up subtitle. Do not invent an empty-cage acquisition item or merge it into ordinary anvil drop semantics.

## RP evidence

`assets/lbc/items/drop_trap.json` resolves type `model` to `lbc:item/drop_trap`; `assets/lbc/models/item/drop_trap.json` has parent `minecraft:item/generated`, layer0 `minecraft:item/drop_trap`; `assets/minecraft/textures/item/drop_trap.png` exists. No RP write is needed for these token models.

Exact existing `assets/minecraft/lang/ru_ru.json` / `en_us.json` values:

| Key | ru_ru | en_us |
|---|---|---|
| `trap_anvil` | Ловушка: Клетка с наковальнями | Trap: Anvil Cage |
| `trap_fireball` | Ловушка: Клетка с фаерболом | Trap: Cage with Fireball |
| `trap_potion` | Ловушка: Клетка с зельями урона | Trap: Cage with damage potions |
| `trap_lava` | Ловушка: Клетка с лавой | Trap: Lava Cage |
| `trap_obsidian` | Ловушка: Обсидиановая клетка | Trap: Obsidian Cage |
| `trap_desc` | Активный Дроп. Для активации выбросте предмет. | Active Drop. Throw the item to activate. |
| `look_up` | Посмотри наверх | Look up |

Preserve the source typo `выбросте`. `trap_potions` is a DIFFERENT existing key for Potion Spawner, not this cage's `trap_potion`.

## Required author decisions / migration constraints

Confirm self targeting explicitly: source nearest nonspectator can be the thrower; excluding the author would materially alter it. Existing user ally/service-role guards still apply to harmful targeting, payload damage and secondary consequences. Do not target/activate at service entities or infer shop role from names; the nearest-player selector already excludes nonplayers but secondary effects do not. Source has no such checks.

Confirm literal absolute255/319 shaft destruction (including arbitrary blocks and reverse behavior above endpoints),40 simultaneous potions reaching+2000, and whether obsidiantrap belongs in this next four-cage implementation or a subsequent family. Existing source is unambiguous; any correction needs author intent. empty_cage belongs to troll-mask effect, not the four token family.

Per agent_notes/AGENTS.md user contract, native causal kill credit must retain actual token thrower through falling-anvil damage, fireball explosion and propagated consequences, potion direct harm, and lava/environmental aftermath. Missing source ownership is a migration gap, not permission to drop attribution. Source provides no general cleanup or forced lifetime; do not invent one. Keep source destruction and exact tokens reviewable separately from native protection policies. This audit makes no code/runtime claims.
