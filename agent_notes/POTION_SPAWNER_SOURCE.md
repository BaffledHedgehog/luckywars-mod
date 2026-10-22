# Potion Spawner — source and item audit, qualification pending

22 October2026. Item/resources prepared; root integration, build, lifecycle and
runtime qualification pending. No claim of a working/qualified migration here.
Read-only source S:
`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`.

## Exact token leaves

S `loot_table/traps.json:446–489`: normal repeating_command_block,count1,
weight10 quality−10; tooltip_style lbc:tier_ff; item_model lbc:drop_trap;
custom name yellow/nonitalic translate trap_potions; one white/nonitalic
trap_desc lore line; {badspawner:1,drop:1}. No entry conditions.
S `loot_table/legacy_luckyblock.json:2188–2205`: barrier,count1,empty components,
{badspawner_old:1,drop:1}; no authored custom name/lore/tier/model.

`LbcPotionSpawnerDropItem.Kind.NORMAL/OLD` exposes item()/flag(); class exposes
register(), matches(stack,kind), matches(stack), kind(stack). Native item IDs:
skywars2:potion_spawner_drop and skywars2:potion_spawner_drop_old. Flags match
independently on any nonempty stack with drop1, preserving existing source
dispatch compatibility. DefaultItemComponentEvents overrides the model after
Polymer defaults. Source defaults/empty old lore remain native prototype data.
Standalone lbc:ported/potion_spawner_drop and potion_spawner_drop_old return one
native item each; no acquisition-pool weights or whole pools are activated.
Root owns registration, build whitelist, native service and final qualification.

## Authored payload

S `function/drops/specialdrops.mcfunction:35–37`: normal green entity_effect
color[0,1,0,1] at tokenY+5,delta(.3,4,.3),speed0,count300,normal. Then normal
spawner at tokenY+10 via ordinary replace; old only creates its own spawner.
No nearest-player relocation, custom lifetime/timer or anti-magic condition.

Both: SpawnCount1,Delay1,MinSpawnDelay4,MaxSpawnDelay5. Normal SpawnRange6,
MaxNearbyEntities200,RequiredPlayerRange35; old15/1000/100. Initial SpawnData:
splash_potion with Item count1, custom_color8388608 and instant_damage amp0,
duration20. Same18 weight1 SpawnPotentials for both; all vanilla splash potions:

| Effect | amplifier | duration | custom_color |
| --- | --- | --- | --- |
| instant_damage |0 |20 |8388608 |
| slowness |4 |600 |2035284 |
| nausea |1 |600 |1935126 |
| blindness |1 |600 |0 |
| hunger |127 |400 |5476112 |
| weakness |0 |400 |4728399 |
| mining_fatigue |2 |600 |13433087 |
| poison |2 |300 |6618974 |
| wither |2 |300 |10921638 |
| glowing |1 |1200 |16774925 |
| levitation |10 |40 |16192767 |
| speed |20 |600 |14417906 |
| haste |127 |600 |16756293 |
| strength |0 |800 |16384000 |
| jump_boost |20 |600 |13762327 |
| slow_falling |1 |600 |3932152 |
| instant_damage |1 |20 |4521984 |
| levitation |0 |400 |12910585 |

No custom reward/item ability dependency. Preserve beneficial as well as harmful
source effects; common native protections/role/causal ownership still apply.

## Independent native26.3 inspection

Read source directly from local Loom sources JAR:
`.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-d6f9bb6878/26.3/minecraft-merged-d6f9bb6878-26.3-sources.jar`.
No generated files/build/server launch was required.

`net/minecraft/world/level/BaseSpawner.java:94–195`: real native serverTick gates
near-player and isSpawnerBlockEnabled; positive countdown decrements without
spawning. Random X/Z uses two random doubles times SpawnRange around block+.5;
Y uses nextInt3−1. Native collision and SpawnPlacements checks remain. Nearby cap
counts exact entity class within unit block AABB inflated SpawnRange, excluding
spectators; it is not a global count. Spawned potion gets native random yaw and
pitch0. Mob-only finalizeSpawn/equipment code does not apply to potions.

`:198–207` resets min+nextInt(max−min):4/5 means EXACTLY4 after each successful
spawn/cap reset, giving a normal five active block-tick opportunity interval,
not uniform4..5 and not an invented fixed every4tick task. Delay and successful
spawn reset chooses next data from18 weighted potentials. Failed collision/spawn
checks need native retry semantics. `:210–220` loads real source spawner fields.

`net/minecraft/world/entity/projectile/throwableitemprojectile/ThrowableItemProjectile.java:56–64`
still saves/loads uppercase Item through ItemStack.CODEC in26.3. Do not blindly
rename Item to item. Source potion item/components have no custom reward schema.

No suspicious authored source bug established. Explicit extreme amplifiers,
range/cap and no lifetime are authored data, not missing instructions. Root must
qualify replacement/destruction, moved/persisted spawner cause, native activation,
spawned-potion ownership without breaking self/ally splash, role safety, effects'
indirect lethal cause, freeze/epoch/unload and quiet stop. This note does not
replace evidence for those lifecycle paths.

## Current RP (read-only)

Root supplied current path for this batch:
`C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8`.
`assets/minecraft/lang/{ru_ru,rpr,en_us}.json:2595`: trap_potions RU/RPR
`Ловушка: Спаунер Зелий`, EN `Trap: Potion Spawner`. At2609 trap_desc RU/RPR
`Активный Дроп. Для активации выбросте предмет.`, EN
`Active Drop. Throw the item to activate.` Preserve source spelling and styles.
No RP/template/translation writes. Leaf JSON parsing and static presentation
review passed; no build/runtime result is asserted.

## Runtime status update

The audit above records the source/preparation stage. Final migration and
118-result/two-match qualification on 884de7db are documented in
[POTION_SPAWNER_PORT.md](POTION_SPAWNER_PORT.md). Earlier pending statements are
historical. This update does not qualify the separate next-source candidates.
