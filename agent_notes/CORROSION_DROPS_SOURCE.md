# Corrosion drops source audit - 22 October 2026

Bounded read-only audit; read project AGENTS.md, agent_notes/AGENTS.md and RESOURCE_PACK.md. Only this new document is written. No implementation, build, runtime, server, datapack, RP or template edits. Source root: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc`. Native evidence: local `.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-d6f9bb6878/26.3/minecraft-merged-d6f9bb6878-26.3-sources.jar`. This is source evidence, not runtime qualification.

## Dispatch, geometry and effects

`function/drops/specialdrops.mcfunction:9-10` dispatches `{korrosion:1}` and `{korrosion_old:1}` at the actual dropped token. No nearest-player selection, centering, victim radius or exclusion occurs. Shared tail consumes one unit per invocation; reuse existing source-order dispatcher and debit. Unlike holes/cages, geometry follows token coordinates, never a selected player's feet.

Both `function/drops/corrosion.mcfunction:1` and `corrosion_old.mcfunction:1` execute `place template lbc:air ~-24 ~-24 ~-24 none none 0.5`. Root audit independently decoded `structure/air.nbt`: size48x48x48, all110592 cells explicitly minecraft:air, no entities or block entities. Integrity0.5 probabilistically retains air placements; it does not shrink the cube or replace exactly half its cells. Native BlockRotProcessor accepts when nextFloat() <= integrity. Expected retained cells55296; individual results vary and existing air can produce no write.

For token real coordinates (x,y,z), origin is (floor(x-24),floor(y-24),floor(z-24)), equal to floor(token)-24 on each axis. Relative template cells0..47 therefore occupy floor(token)-24 through floor(token)+23 inclusive. This is an asymmetric48-cell cube, not radius24 inclusive49 cells, sphere, or a fully cleared cube. Fractions are retained until BlockPos flooring, including negative coordinates; no yaw rotation/mirror applies.

Normal line3 emits dust color[0.255,0.204,0.302], scale2.0, token-relative center, spread1/1/1, speed1, count300, normal visibility. Old line2 additionally clears the exact floored token cell and cell above with `fill ~ ~ ~ ~ ~1 ~ minecraft:air strict`; this is guaranteed independently of the random mask when its fill succeeds. Old line3 emits large_smoke, same center/spread/speed/visibility, count5000. Failed template placement does not prevent subsequent fill/particles or token consumption. No direct damage, actor, timer, restoration or custom lifetime exists.

## Native placement semantics and meaningful quirks

Read PlaceCommand.placeTemplate/checkLoaded, StructureTemplate.placeInWorld/processBlockInfos/updateShapeAtEdge, StructurePlaceSettings, BlockRotProcessor, StructureBlockEntity.createRandom, LevelChunk.setBlockState and BlockEntity.preRemoveSideEffects in the local26.3 sources.

Source command omits seed and strict: seed0, knownShape=false, placement flags2. Seed0 calls RandomSource.create(Util.getMillis()); random placement is not a stable position hash. StructurePlaceSettings receives the seeded random used sequentially during processing. Do not substitute a fixed mask or force exactly55296 writes.

PlaceCommand checks every chunk from origin through origin+template.size inclusive before placement; actual last cell is origin+47, but checked upper endpoint is origin+48. Thus an extra adjacent chunk can be required when that endpoint crosses a chunk boundary. Do not silently force-load chunks. The command has no FillCommand MAX_BLOCK_MODIFICATIONS gate and no complete cuboid build-height rejection. Individual Level.setBlock bounds still reject invalid cells, so vertically crossing the world boundary can partially place. This differs from the old variant's later strict two-cell FillCommand, which has its own fill checks.

All-air template blocks have null NBT, so StructureTemplate does not insert its temporary barrier (that branch applies only to incoming NBT). Flags2 omit256: LevelChunk invokes removed block entity preRemoveSideEffects; ordinary Container inventories eject through Containers.dropContents. This differs materially from strict fill818, whose256 suppresses inventory ejection. Native randomizable-container behavior remains native; do not resolve or spill containers in cells the mask skipped. Ordinary removed blocks do not gain artificial block-item loot merely because the template writes air.

KnownShape=false causes StructureTemplate to construct a voxel occupancy mask for successful writes, call updateShapeAtEdge, then updateFromNeighbourShapes and updateNeighborsAt for each placed position. Flags2 also retain normal shape/onPlace behavior where applicable. Secondary unsupported blocks/fluid behavior can extend beyond random selected cells. Replacing this with strict bulk fill changes the effect. Old center fill818 subsequently skips known-shape updates, drops, block-entity side effects and onPlace; if the mask skipped a center container, old fill can remove it without ejection.

Native processing allocates/processes all110592 source entries and maintains successful placement/voxel-neighbor work. Sparse terrain does not remove the initial full-template scan; dense terrain and native follow-up updates can be costly. Any staged processing, cap, strict-write shortcut, reduced particles or alternate mask is an intentional fidelity/performance choice, not a transparent optimization. The implementation timing choice and qualification are recorded separately in CORROSION_DROPS_PORT.md.

## Exact acquisition presentation

`loot_table/traps.json` normal entry: minecraft:repeating_command_block, count1, weight10, quality-10; tooltip_style lbc:tier_ff, item_model lbc:drop_trap. Name one-element array translate trap_corrosion, italic:false, yellow. Lore replace_all exactly one row translate trap_desc, italic:false, white. custom_data `{korrosion:1,drop:1}`.

`loot_table/legacy_luckyblock.json` old entry: minecraft:barrier,count1, empty set_components, custom_data `{drop:1,korrosion_old:1}`. No custom name/lore/model/tooltip. `loot_table/imba/traps.json`: repeating_command_block,count1, weight0, quality-10, empty set_components, custom_data `{korrosion:1,drop:1,imba:1}`; also undecorated. No old entry in those three bounded tables. IMBA shares normal behavior but preserves its acquisition presentation; enclosing random reachability is not qualified.

## RP verification

Active original inspected read-only at `C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3/minecraft/resourcepacks/all_in_one_1.21.8` (current task path; RESOURCE_PACK.md's older path uses26.3(1)). assets/lbc/items/drop_trap.json resolves lbc:item/drop_trap; assets/lbc/models/item/drop_trap.json uses minecraft:item/generated and layer0 minecraft:item/drop_trap. assets/minecraft/textures/item/drop_trap.png exists. No new resource or translation is needed.

| Key | ru_ru | en_us |
|---|---|---|
| trap_corrosion | Ловушка: Коррозия блоков | Trap: Block Corrosion |
| trap_desc | Активный Дроп. Для активации выбросте предмет. | Active Drop. Throw the item to activate. |

Preserve exact source translation spelling/style. Old and IMBA do not request these decorations.

## Ownership and qualification boundaries

Author contract requires actual thrower kill credit; source lacks attribution. Support removal can cause falling/void deaths and secondary unsupported terrain/fluid changes; preserve causal thrower independently from token location and apply established WILD terrain-trap policy. No direct artificial damage should be added. Reuse support-removal ownership rather than attributing every nearby later fall. Qualification must distinguish native template inventory ejection from old strict center suppression, random support removal, fractional/negative floor coordinates, chunk preflight's extra endpoint, partial height clipping and no fill-limit gate. No implementation or runtime success is claimed here.
