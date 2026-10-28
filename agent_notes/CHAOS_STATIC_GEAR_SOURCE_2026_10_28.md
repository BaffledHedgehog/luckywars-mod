# Complete Chaos and static gear source audit — 28 October 2026

Static-only implementation/qualification: [Multiplying and Randchanted armor](STATIC_ARMOR_PORT_2026_10_28.md).
Chaos below remains source preparation. The bridge, arrow orientation and MAG
gate questions were sent to the author asynchronously; no unanswered choice is
implemented by this static-only patch.

Read-only bounded source audit; no builds/runtime qualification, gameplay, server,
RP/template or commit changes. Only this new note is owned by this audit.
Read AGENTS.md and agent_notes/AGENTS.md; user notes remain untouched.
Legacy root: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/`.
Author RP is read-only: `C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8`;
see [RESOURCE_PACK.md](RESOURCE_PACK.md). Existing EN names/lore at
assets/minecraft/lang/en_us.json:526-527,811-818,1387-1388,1461-1462 agree with
incoming chest, jump legs, sprint boots, nearby traps helmet and void rescue.

## Recommended bounded completion

Complete Chaos means ALL four equipped-slot behaviors, chest particles, full-set
void rescue, actual trap loot and its native dispatch, seven armor tables
(four leaves/root/pre/full), normal slot and Ultra acquisition, both advancement
consumers and source presentation. Current LbcArmorSets has no Chaos service;
LbcDamage only recognizes the custom family and LbcBolts already routes its Ultra
reforge. A catalog/attribute-only pass cannot complete Chaos.

Reuse shared LbcArmorSets eligibility/phase admission, native incoming criterion,
actual jump admission, native effect layers, LbcIndirectKills causal attribution,
LbcSpecialDrops dispatch and native blast services. LbcTechnoArmor's ten radius1
nitro blasts are NOT the Chaos source's nine ordinary-creeper-strength blasts.
No invented melee, successful-spell, fusion or crafting bonuses were found by
full flag/family/function-reference search; TMT trigger_smth variants invoke
items/armor and must remain linked to its existing shared action dispatch.

## Live entry closure

All paths below start in lbc/function unless stated otherwise.

- items/changed_inventory/sel:5-7 counts four raw chaoschestplate flags; sets
  chaos_set_bonus_active. Lines92/104 track equipped chest chaosdust_active.
- players_5t:12 emits white dust at Y+1, spread.3 each axis, count3, speed0.
- items/damaged:5 invokes effectselect for flagged CHEST, excluding both marker
  stopper_magic and nonspectator nomagic_active within20. Uniform1..17 below.
- players_tick:6 invokes items/armor on jump1>=1; items/armor:9 tests flagged
  LEGS and both MAG sources within20 -> uniform1..7 jump; line18 resets jump1.
  TMT trigger_smth, trigger_smth_not_bow and trigger_smth_not_crossbow also call
  items/armor; do not silently remove this active source linkage.
- players_tick:10 invokes items/on_sprint while sprinting each tick. Its line2
  needs on_ground AND chance5 (5 percent), flagged FEET and both MAG sources
  within20 -> uniform1..7 sprint. It is a roll per sprinting grounded tick,
  not one proc on sprint-start.
- tick10s:3 runs players_10s for nonspectators outside nexus/imprinted;
  players_10s:3 tests flagged HEAD, without an outer stopper gate.
  items/armor/chaoschestplate_10s excludes ALL output if any nomagic_active
  nonspectator within25; otherwise one lbc:traps loot spawn at each other
  nonallied nonspectator player's location at distance0.01..25.
- players_1s:3 full flag at Y<=-10 via y=-10,dy=-5000000; only stopper_magic
  marker within20 suppresses. armor/chaos_set_bonus grants Resistance20ticks
  amplifier4, spreadplayers radius190 center0,0 when no_borders=0, otherwise
  current position; Nether uses under125, others unconstrained; then drops one
  lbc:traps at rescued X/Z, absolute Y100.

## Acquisition and advancements

Four leather leaves retain actual source attributes, distinct IDs, durability,
asset lbc:chaos, lbc:armors/chaos_SLOT models, gold name and exact lore/components.
Root chooses one equal-weight slot; pre references root; full gives all four.
Legacy nekolucky_items -> pre; 6_sets -> full; transformables_SLOT -> matching
leaf; ultra_reforge:24 chaoschestplate -> root. build.gradle currently packages
root/four leaves but needs explicit pre/full closure. No source armor fusion found.

lbc/advancement/true_advancements/armor/chaos.json: PvP kill wearing all4,
nonspectator; parent assasin, levelup reward. chaos_full.json: inventory_changed,
all4 equipped plus inventory {transformable:1,sword:1,cool:1},
{transformable:1,staff:1,unstable_spellbook:1}, {luckyblockwand:1}; parent chaos,
kit reward. Preserve both; full does not mean another generic all4 achievement.

## Source ambiguities requiring author choices

1. MAG: retain helmet's absence of marker gate and25-radius player gate, and
   void rescue's marker-only20-radius gate, or normalize all bonuses to MAG?
   Chest/legs/feet explicitly gate both MAG forms20. No TECH source gate exists.
2. bridge uses `positioned ~ -1.5 ~` (ABSOLUTE Y=-1.5), with41 successive local
   forward positions0..40, +/-0.5 fills replacing airs. Should be wearerY-1.5?
3. arrows creates24 outward-world-velocity arrows (8 directions *3 layers),
   then rotates them to face wearer feet without changing Motion. Preserve
   outward Motion or intended inward direction? Ownership repair is already
   required by standing user attribution policy, not an unresolved permission.
4. doggies actually summons ONE wolf; limit4 applies to nearby temporary-tag
   owner assignment. Recommend one correctly owned wolf without collateral tags.
5. corrosion calls air template offset(-24,-24,-24), integrity.05. Native shared
   corrosion equivalence must retain original structure extent/palette/randomness;
   do not assume generic spherical terrain deletion is source-equivalent.

All damage and delayed effects need wearer credit per standing (user) rule.
No creeper carriers: use nine native ordinary-creeper blasts behind1 at flat
pitch, preserving source strength; already authorized project-wide approach.
Preserve existing granted effects through jammer admission; do not clear other
owners. Exact helper source archived below includes all sounds/particle constants.

## Trap dependency closure

lbc/loot_table/traps.json has25 entries, each weight10, source quality +/-10,
including TWO Academy entries with separate ranked condition. Raw drop flags:
explode,korrosion,creepers,yama,lookupanvil,lookuplava,bob,skeleton,vex,
obsidiantrap,badspawner,droppertnt,bullets,skrimmer,slab,clone,evilluckyblock,
lookupfireball,lookuppotions,zombietnt,aimbob,sauvojen_tuntija (twice),forest,
ultraponos. Existing LbcSpecialDrops handles many through source flag matching,
including cage/TNT/creeper/corrosion/hole/slab/creature/drone/ultraponos services.
No direct lbc:traps table call exists in current Java. Complete implementation
must exercise all25 actual weighted/conditional branches through shared native
admission and retain drop causality; unsupported branches must be resolved,
never silently removed or relabeled complete. This note establishes dependency
scope, not a claim that all trap leaf implementations were qualified here.

## Static summary validation

[NEXT_GEAR_SOURCE_2026_10_28_C.md](NEXT_GEAR_SOURCE_2026_10_28_C.md) remains
correct for multiplying native golden armor ADD_MULTIPLIED_BASE armor AND
toughness amounts2/4/3/1, one-slot root, all4 armor PvP condition; no timer or
set multiplier should be invented. Randchanted armor uses native
 enchant_with_levels uniform1..30; root chooses one slot, armor achievement
parent Multiplying. Native rolls/attributes require finite qualification, not
new runtime families. Normal slot and Ultra routes already exist.

Resolve prior hoe caveat explicitly: actual transformables_hoe.json has NO
randchanted flag or enchant_with_levels branch. RP randchanted_hoe translation
and TMT name are not acquisition definitions. Actual original Randchanted
weapon/tool branches are sword,pickaxe,axe,shovel,bow. Keep source Ultra armor
exclusions sword/pickaxe/bow/axe ONLY; adding shovel/hoe exclusions changes source.
Do not invent a Randchanted hoe. Existing ported random_enchanted tools remain
separate native leaf evidence. Preserve source weapon achievements independently.

## Exact Chaos helper payload archive

Each heading names a source file under lbc/function/other/chaoschestplate.
Durations in commands are seconds; amplifiers are zero-based. This archive is
source evidence, not approved decisions or runtime qualification.

### absorption.mcfunction

```mcfunction
effect give @s minecraft:absorption 30 0
particle minecraft:dripping_honey ~ ~1 ~ 0.5 1 0.5 0 40 normal
playsound minecraft:block.beehive.enter master @a ~ ~ ~ 1 0
```

### acid.mcfunction

```mcfunction
particle minecraft:sneeze ~ ~1 ~ 0.5 1 0.5 1 150 normal
scoreboard players operation *tempt lbcID2 = @s team_number
effect give @e[distance=0.01..10,tag=!spectator,predicate=!lbc:same_team] minecraft:wither 2 10
effect give @e[distance=0.01..10,tag=!spectator,predicate=!lbc:same_team] minecraft:poison 2 10
effect give @a[gamemode=!spectator,distance=0.01..10,predicate=!lbc:same_team] minecraft:hunger 2 10
effect give @e[distance=0.01..10,tag=!spectator,predicate=!lbc:same_team] minecraft:slowness 2 10
playsound minecraft:entity.shulker.ambient master @a ~ ~ ~ 1 0
```

### arrows.mcfunction

```mcfunction
particle minecraft:end_rod ~ ~1 ~ 0.3 1 0.3 0.1 40 force
summon minecraft:arrow ~ ~1.15 ~1 {Motion:[0.0d,0.0d,1.0d],PierceLevel:10b,Tags:["killedarrow","bombardilo_crocodilo"]}
summon minecraft:arrow ~ ~1.15 ~-1 {Motion:[0.0d,0.0d,-1.0d],PierceLevel:10b,Tags:["killedarrow","bombardilo_crocodilo"]}
summon minecraft:arrow ~1 ~1.15 ~ {Motion:[1.0d,0.0d,0.0d],PierceLevel:10b,Tags:["killedarrow","bombardilo_crocodilo"]}
summon minecraft:arrow ~-1 ~1.15 ~ {Motion:[-1.0d,0.0d,0.0d],PierceLevel:10b,Tags:["killedarrow","bombardilo_crocodilo"]}

summon minecraft:arrow ~1 ~1.15 ~1 {Motion:[1.0d,0.0d,1.0d],PierceLevel:10b,Tags:["killedarrow","bombardilo_crocodilo"]}
summon minecraft:arrow ~-1 ~1.15 ~-1 {Motion:[-1.0d,0.0d,-1.0d],PierceLevel:10b,Tags:["killedarrow","bombardilo_crocodilo"]}
summon minecraft:arrow ~1 ~1.15 ~-1 {Motion:[1.0d,0.0d,-1.0d],PierceLevel:10b,Tags:["killedarrow","bombardilo_crocodilo"]}
summon minecraft:arrow ~-1 ~1.15 ~1 {Motion:[-1.0d,0.0d,1.0d],PierceLevel:10b,Tags:["killedarrow","bombardilo_crocodilo"]}
summon minecraft:arrow ~ ~1.7 ~1 {Motion:[0.0d,0.7d,1.0d],PierceLevel:10b,Tags:["killedarrow","bombardilo_crocodilo"]}

summon minecraft:arrow ~ ~1.7 ~-1 {Motion:[0.0d,0.7d,-1.0d],PierceLevel:10b,Tags:["killedarrow","bombardilo_crocodilo"]}
summon minecraft:arrow ~1 ~1.7 ~ {Motion:[1.0d,0.7d,0.0d],PierceLevel:10b,Tags:["killedarrow","bombardilo_crocodilo"]}
summon minecraft:arrow ~-1 ~1.7 ~ {Motion:[-1.0d,0.7d,0.0d],PierceLevel:10b,Tags:["killedarrow","bombardilo_crocodilo"]}
summon minecraft:arrow ~1 ~1.7 ~1 {Motion:[1.0d,0.7d,1.0d],PierceLevel:10b,Tags:["killedarrow","bombardilo_crocodilo"]}
summon minecraft:arrow ~-1 ~1.7 ~-1 {Motion:[-1.0d,0.7d,-1.0d],PierceLevel:10b,Tags:["killedarrow","bombardilo_crocodilo"]}

summon minecraft:arrow ~1 ~1.7 ~-1 {Motion:[1.0d,0.7d,-1.0d],PierceLevel:10b,Tags:["killedarrow","bombardilo_crocodilo"]}
summon minecraft:arrow ~-1 ~1.7 ~1 {Motion:[-1.0d,0.7d,1.0d],PierceLevel:10b,Tags:["killedarrow","bombardilo_crocodilo"]}
summon minecraft:arrow ~ ~1 ~1 {Motion:[0.0d,-0.7d,1.0d],PierceLevel:10b,Tags:["killedarrow","bombardilo_crocodilo"]}
summon minecraft:arrow ~ ~1 ~-1 {Motion:[0.0d,-0.7d,-1.0d],PierceLevel:10b,Tags:["killedarrow","bombardilo_crocodilo"]}
summon minecraft:arrow ~1 ~1 ~ {Motion:[1.0d,-0.7d,0.0d],PierceLevel:10b,Tags:["killedarrow","bombardilo_crocodilo"]}

summon minecraft:arrow ~-1 ~1 ~ {Motion:[-1.0d,-0.7d,0.0d],PierceLevel:10b,Tags:["killedarrow","bombardilo_crocodilo"]}
summon minecraft:arrow ~1 ~1 ~1 {Motion:[1.0d,-0.7d,1.0d],PierceLevel:10b,Tags:["killedarrow","bombardilo_crocodilo"]}
summon minecraft:arrow ~-1 ~1 ~-1 {Motion:[-1.0d,-0.7d,-1.0d],PierceLevel:10b,Tags:["killedarrow","bombardilo_crocodilo"]}
summon minecraft:arrow ~1 ~1 ~-1 {Motion:[1.0d,-0.7d,-1.0d],PierceLevel:10b,Tags:["killedarrow","bombardilo_crocodilo"]}
summon minecraft:arrow ~-1 ~1 ~1 {Motion:[-1.0d,-0.7d,1.0d],PierceLevel:10b,Tags:["killedarrow","bombardilo_crocodilo"]}
tag @s add huesos_i_pidoras
execute as @e[type=arrow,distance=..3,tag=bombardilo_crocodilo,limit=24] at @s facing entity @p[tag=huesos_i_pidoras] feet run tp @s ~ ~ ~ ~ ~
tag @s remove huesos_i_pidoras
playsound minecraft:entity.skeleton.shoot master @a ~ ~ ~ 1 1
```

### bounce.mcfunction

```mcfunction
effect give @s minecraft:jump_boost 20 2
scoreboard players operation *tempt lbcID2 = @s team_number
effect give @e[distance=0.01..10,tag=!spectator,predicate=!lbc:same_team] minecraft:levitation 1 4 true
particle minecraft:smoke ~ ~ ~ 7 7 7 0.05 100 normal
playsound minecraft:entity.firework_rocket.launch master @a ~ ~ ~ 1 1
```

### bridge.mcfunction

```mcfunction
execute positioned ~ -1.5 ~ run fill ~0.5 ~ ~0.5 ~-0.5 ~ ~-0.5 minecraft:light_blue_stained_glass replace #minecraft:airs strict
execute positioned ~ -1.5 ~ positioned ^ ^ ^1 run fill ~0.5 ~ ~0.5 ~-0.5 ~ ~-0.5 minecraft:light_blue_stained_glass replace #minecraft:airs strict
execute positioned ~ -1.5 ~ positioned ^ ^ ^2 run fill ~0.5 ~ ~0.5 ~-0.5 ~ ~-0.5 minecraft:light_blue_stained_glass replace #minecraft:airs strict
execute positioned ~ -1.5 ~ positioned ^ ^ ^3 run fill ~0.5 ~ ~0.5 ~-0.5 ~ ~-0.5 minecraft:light_blue_stained_glass replace #minecraft:airs strict
execute positioned ~ -1.5 ~ positioned ^ ^ ^4 run fill ~0.5 ~ ~0.5 ~-0.5 ~ ~-0.5 minecraft:light_blue_stained_glass replace #minecraft:airs strict
execute positioned ~ -1.5 ~ positioned ^ ^ ^5 run fill ~0.5 ~ ~0.5 ~-0.5 ~ ~-0.5 minecraft:light_blue_stained_glass replace #minecraft:airs strict
execute positioned ~ -1.5 ~ positioned ^ ^ ^6 run fill ~0.5 ~ ~0.5 ~-0.5 ~ ~-0.5 minecraft:light_blue_stained_glass replace #minecraft:airs strict
execute positioned ~ -1.5 ~ positioned ^ ^ ^7 run fill ~0.5 ~ ~0.5 ~-0.5 ~ ~-0.5 minecraft:light_blue_stained_glass replace #minecraft:airs strict
execute positioned ~ -1.5 ~ positioned ^ ^ ^8 run fill ~0.5 ~ ~0.5 ~-0.5 ~ ~-0.5 minecraft:light_blue_stained_glass replace #minecraft:airs strict
execute positioned ~ -1.5 ~ positioned ^ ^ ^9 run fill ~0.5 ~ ~0.5 ~-0.5 ~ ~-0.5 minecraft:light_blue_stained_glass replace #minecraft:airs strict
execute positioned ~ -1.5 ~ positioned ^ ^ ^10 run fill ~0.5 ~ ~0.5 ~-0.5 ~ ~-0.5 minecraft:light_blue_stained_glass replace #minecraft:airs strict
execute positioned ~ -1.5 ~ positioned ^ ^ ^11 run fill ~0.5 ~ ~0.5 ~-0.5 ~ ~-0.5 minecraft:light_blue_stained_glass replace #minecraft:airs strict
execute positioned ~ -1.5 ~ positioned ^ ^ ^12 run fill ~0.5 ~ ~0.5 ~-0.5 ~ ~-0.5 minecraft:light_blue_stained_glass replace #minecraft:airs strict
execute positioned ~ -1.5 ~ positioned ^ ^ ^13 run fill ~0.5 ~ ~0.5 ~-0.5 ~ ~-0.5 minecraft:light_blue_stained_glass replace #minecraft:airs strict
execute positioned ~ -1.5 ~ positioned ^ ^ ^14 run fill ~0.5 ~ ~0.5 ~-0.5 ~ ~-0.5 minecraft:light_blue_stained_glass replace #minecraft:airs strict
execute positioned ~ -1.5 ~ positioned ^ ^ ^15 run fill ~0.5 ~ ~0.5 ~-0.5 ~ ~-0.5 minecraft:light_blue_stained_glass replace #minecraft:airs strict
execute positioned ~ -1.5 ~ positioned ^ ^ ^16 run fill ~0.5 ~ ~0.5 ~-0.5 ~ ~-0.5 minecraft:light_blue_stained_glass replace #minecraft:airs strict
execute positioned ~ -1.5 ~ positioned ^ ^ ^17 run fill ~0.5 ~ ~0.5 ~-0.5 ~ ~-0.5 minecraft:light_blue_stained_glass replace #minecraft:airs strict
execute positioned ~ -1.5 ~ positioned ^ ^ ^18 run fill ~0.5 ~ ~0.5 ~-0.5 ~ ~-0.5 minecraft:light_blue_stained_glass replace #minecraft:airs strict
execute positioned ~ -1.5 ~ positioned ^ ^ ^19 run fill ~0.5 ~ ~0.5 ~-0.5 ~ ~-0.5 minecraft:light_blue_stained_glass replace #minecraft:airs strict
execute positioned ~ -1.5 ~ positioned ^ ^ ^20 run fill ~0.5 ~ ~0.5 ~-0.5 ~ ~-0.5 minecraft:light_blue_stained_glass replace #minecraft:airs strict
execute positioned ~ -1.5 ~ positioned ^ ^ ^21 run fill ~0.5 ~ ~0.5 ~-0.5 ~ ~-0.5 minecraft:light_blue_stained_glass replace #minecraft:airs strict
execute positioned ~ -1.5 ~ positioned ^ ^ ^22 run fill ~0.5 ~ ~0.5 ~-0.5 ~ ~-0.5 minecraft:light_blue_stained_glass replace #minecraft:airs strict
execute positioned ~ -1.5 ~ positioned ^ ^ ^23 run fill ~0.5 ~ ~0.5 ~-0.5 ~ ~-0.5 minecraft:light_blue_stained_glass replace #minecraft:airs strict
execute positioned ~ -1.5 ~ positioned ^ ^ ^24 run fill ~0.5 ~ ~0.5 ~-0.5 ~ ~-0.5 minecraft:light_blue_stained_glass replace #minecraft:airs strict
execute positioned ~ -1.5 ~ positioned ^ ^ ^25 run fill ~0.5 ~ ~0.5 ~-0.5 ~ ~-0.5 minecraft:light_blue_stained_glass replace #minecraft:airs strict
execute positioned ~ -1.5 ~ positioned ^ ^ ^26 run fill ~0.5 ~ ~0.5 ~-0.5 ~ ~-0.5 minecraft:light_blue_stained_glass replace #minecraft:airs strict
execute positioned ~ -1.5 ~ positioned ^ ^ ^27 run fill ~0.5 ~ ~0.5 ~-0.5 ~ ~-0.5 minecraft:light_blue_stained_glass replace #minecraft:airs strict
execute positioned ~ -1.5 ~ positioned ^ ^ ^28 run fill ~0.5 ~ ~0.5 ~-0.5 ~ ~-0.5 minecraft:light_blue_stained_glass replace #minecraft:airs strict
execute positioned ~ -1.5 ~ positioned ^ ^ ^29 run fill ~0.5 ~ ~0.5 ~-0.5 ~ ~-0.5 minecraft:light_blue_stained_glass replace #minecraft:airs strict
execute positioned ~ -1.5 ~ positioned ^ ^ ^30 run fill ~0.5 ~ ~0.5 ~-0.5 ~ ~-0.5 minecraft:light_blue_stained_glass replace #minecraft:airs strict
execute positioned ~ -1.5 ~ positioned ^ ^ ^31 run fill ~0.5 ~ ~0.5 ~-0.5 ~ ~-0.5 minecraft:light_blue_stained_glass replace #minecraft:airs strict
execute positioned ~ -1.5 ~ positioned ^ ^ ^32 run fill ~0.5 ~ ~0.5 ~-0.5 ~ ~-0.5 minecraft:light_blue_stained_glass replace #minecraft:airs strict
execute positioned ~ -1.5 ~ positioned ^ ^ ^33 run fill ~0.5 ~ ~0.5 ~-0.5 ~ ~-0.5 minecraft:light_blue_stained_glass replace #minecraft:airs strict
execute positioned ~ -1.5 ~ positioned ^ ^ ^34 run fill ~0.5 ~ ~0.5 ~-0.5 ~ ~-0.5 minecraft:light_blue_stained_glass replace #minecraft:airs strict
execute positioned ~ -1.5 ~ positioned ^ ^ ^35 run fill ~0.5 ~ ~0.5 ~-0.5 ~ ~-0.5 minecraft:light_blue_stained_glass replace #minecraft:airs strict
execute positioned ~ -1.5 ~ positioned ^ ^ ^36 run fill ~0.5 ~ ~0.5 ~-0.5 ~ ~-0.5 minecraft:light_blue_stained_glass replace #minecraft:airs strict
execute positioned ~ -1.5 ~ positioned ^ ^ ^37 run fill ~0.5 ~ ~0.5 ~-0.5 ~ ~-0.5 minecraft:light_blue_stained_glass replace #minecraft:airs strict
execute positioned ~ -1.5 ~ positioned ^ ^ ^38 run fill ~0.5 ~ ~0.5 ~-0.5 ~ ~-0.5 minecraft:light_blue_stained_glass replace #minecraft:airs strict
execute positioned ~ -1.5 ~ positioned ^ ^ ^39 run fill ~0.5 ~ ~0.5 ~-0.5 ~ ~-0.5 minecraft:light_blue_stained_glass replace #minecraft:airs strict
execute positioned ~ -1.5 ~ positioned ^ ^ ^40 run fill ~0.5 ~ ~0.5 ~-0.5 ~ ~-0.5 minecraft:light_blue_stained_glass replace #minecraft:airs strict

```

### corrosion.mcfunction

```mcfunction
place template lbc:air ~-24 ~-24 ~-24 none none 0.05

particle minecraft:large_smoke ~ ~ ~ 1 1 1 1 50 normal
playsound minecraft:block.portal.ambient master @a ~ ~ ~ 1 1

```

### damage.mcfunction

```mcfunction
scoreboard players operation *tempt lbcID2 = @s team_number
execute at @e[type=#minecraft:mobs,distance=0.01..10,tag=!spectator,predicate=!lbc:same_team] run damage @e[type=#minecraft:mobs,tag=!spectator,sort=nearest,limit=1,predicate=!lbc:same_team] 6 cactus by @s
particle minecraft:damage_indicator ~ ~ ~ 8 8 8 0 40 normal
playsound minecraft:entity.player.attack.strong master @a ~ ~ ~ 1 0
```

### doggies.mcfunction

```mcfunction
summon minecraft:wolf ~ ~ ~ {DeathLootTable:"empty",Sitting:0b,CollarColor:14b,Tags:["summoned"],attributes:[{id:"minecraft:follow_range",base:200},{id:"minecraft:movement_speed",base:0.5},{id:"minecraft:attack_damage",base:5}]}
execute at @e[tag=summoned,sort=nearest,limit=4] run data modify entity @e[tag=summoned,sort=nearest,limit=1] Owner set from entity @s UUID
tag @e[tag=summoned,sort=nearest,limit=4] remove summoned
playsound minecraft:entity.wolf.growl master @a ~ ~ ~ 1 1
particle minecraft:dust{color:[0.6,0.6,0.6],scale:1.0f} ~ ~ ~ 0.5 0.5 0.5 0.02 10 normal
```

### effectselect.mcfunction

```mcfunction
execute store result score #random17 lbc.math run random value 1..17
execute if score #random17 lbc.math matches 17 run function lbc:other/chaoschestplate/explode
execute if score #random17 lbc.math matches 1 run function lbc:other/chaoschestplate/arrows
execute if score #random17 lbc.math matches 2 run function lbc:other/chaoschestplate/bounce
execute if score #random17 lbc.math matches 3 run function lbc:other/chaoschestplate/flame
execute if score #random17 lbc.math matches 4 run function lbc:other/chaoschestplate/heal
execute if score #random17 lbc.math matches 5 run function lbc:other/chaoschestplate/damage
execute if score #random17 lbc.math matches 6 run function lbc:other/chaoschestplate/teleportnazad
execute if score #random17 lbc.math matches 7 run function lbc:other/chaoschestplate/doggies
execute if score #random17 lbc.math matches 8 run function lbc:other/chaoschestplate/corrosion
execute if score #random17 lbc.math matches 9 run function lbc:other/chaoschestplate/invulnerable
execute if score #random17 lbc.math matches 10 run function lbc:other/chaoschestplate/smoke
execute if score #random17 lbc.math matches 11 run function lbc:other/chaoschestplate/weakall
execute if score #random17 lbc.math matches 12 run function lbc:other/chaoschestplate/perdesh
execute if score #random17 lbc.math matches 13 run function lbc:other/chaoschestplate/transfusion
execute if score #random17 lbc.math matches 14 run function lbc:other/chaoschestplate/speed
execute if score #random17 lbc.math matches 15 run function lbc:other/chaoschestplate/absorption
execute if score #random17 lbc.math matches 16 run function lbc:other/chaoschestplate/acid
```

### effectselect_jump.mcfunction

```mcfunction
execute store result score #random7 lbc.math run random value 1..7
execute if score #random7 lbc.math matches 1 run function lbc:other/chaoschestplate/flame
execute if score #random7 lbc.math matches 2 run function lbc:other/chaoschestplate/slow
execute if score #random7 lbc.math matches 3 run function lbc:other/chaoschestplate/levitation
execute if score #random7 lbc.math matches 4 run function lbc:other/chaoschestplate/ponos
execute if score #random7 lbc.math matches 5 run function lbc:other/chaoschestplate/explode
execute if score #random7 lbc.math matches 6 run function lbc:other/chaoschestplate/speed
execute if score #random7 lbc.math matches 7 run function lbc:other/chaoschestplate/teleport
```

### effectselect_see.mcfunction

```mcfunction
execute store result score #random13 lbc.math run random value 1..13
execute if score #random13 lbc.math matches 1 run effect give @s minecraft:blindness 15 0
execute if score #random13 lbc.math matches 2 run effect give @s minecraft:darkness 15 0
execute if score #random13 lbc.math matches 3 run effect give @s minecraft:glowing 60 0
execute if score #random13 lbc.math matches 4 run effect give @s minecraft:hunger 60 2
execute if score #random13 lbc.math matches 5 run effect give @s minecraft:instant_damage
execute if score #random13 lbc.math matches 6 run effect give @s minecraft:levitation 10 1
execute if score #random13 lbc.math matches 7 run effect give @s minecraft:mining_fatigue 60 1
execute if score #random13 lbc.math matches 8 run effect give @s minecraft:nausea 20 0
execute if score #random13 lbc.math matches 9 run effect give @s minecraft:poison 10 0
execute if score #random13 lbc.math matches 10 run effect give @s minecraft:slowness 20 1
execute if score #random13 lbc.math matches 11 run effect give @s minecraft:unluck 30 1
execute if score #random13 lbc.math matches 12 run effect give @s minecraft:weakness 15
execute if score #random13 lbc.math matches 13 run effect give @s minecraft:wither 15
```

### effectselect_sprint.mcfunction

```mcfunction
execute store result score #random7 lbc.math run random value 1..7
execute if score #random7 lbc.math matches 1 run function lbc:other/chaoschestplate/speed_only
execute if score #random7 lbc.math matches 2 run function lbc:other/chaoschestplate/jump_boost
execute if score #random7 lbc.math matches 3 run function lbc:other/chaoschestplate/phase
execute if score #random7 lbc.math matches 4 run function lbc:other/chaoschestplate/nitroflight
execute if score #random7 lbc.math matches 5 run function lbc:other/chaoschestplate/phase_speed
execute if score #random7 lbc.math matches 6 run function lbc:other/chaoschestplate/bridge
execute if score #random7 lbc.math matches 7 run function lbc:other/chaoschestplate/flash
```

### explode.mcfunction

```mcfunction
effect give @a[gamemode=!spectator,distance=..8] minecraft:resistance 1 4 true
effect give @s minecraft:slow_falling 10 0 true
summon minecraft:tnt ~0.5 ~ ~ {fuse:5s}
summon minecraft:tnt ~-0.5 ~ ~ {fuse:5s}
summon minecraft:tnt ~ ~ ~0.5 {fuse:5s}
summon minecraft:tnt ~ ~ ~-0.5 {fuse:5s}
playsound minecraft:entity.tnt.primed master @a ~ ~ ~ 1 1
```

### flame.mcfunction

```mcfunction
effect give @s minecraft:fire_resistance 60 0
fill ~5 ~5 ~5 ~-5 ~-5 ~-5 minecraft:fire replace #minecraft:airs
playsound minecraft:item.firecharge.use master @a ~ ~ ~ 1 1
particle minecraft:flame ~ ~1 ~ 0.5 1 0.5 0.1 32 normal
```

### flash.mcfunction

```mcfunction
particle minecraft:flash{color:[1,1,0,1]} ~ ~ ~ 10 10 10 1 1000 normal
scoreboard players operation *tempt lbcID2 = @s team_number
effect give @a[gamemode=!spectator,distance=0.01..15,predicate=!lbc:same_team] minecraft:blindness 5 0
effect give @e[distance=0.01..15,tag=!spectator,predicate=!lbc:same_team] minecraft:slowness 5 9
```

### heal.mcfunction

```mcfunction
effect give @s minecraft:instant_health 1 0
particle minecraft:heart ~ ~1 ~ 0.5 1 0.5 0 10 normal
playsound minecraft:entity.player.levelup master @a ~ ~ ~ 1 1.5
```

### invulnerable.mcfunction

```mcfunction
effect give @s minecraft:resistance 1 3 true
particle minecraft:block_marker{block_state:{Name:"minecraft:barrier"}} ~ ~0.5 ~
particle minecraft:block_marker{block_state:{Name:"minecraft:barrier"}} ~ ~1.5 ~
playsound minecraft:block.anvil.use master @a ~ ~ ~ 1 2
```

### jump_boost.mcfunction

```mcfunction
particle minecraft:sneeze ~ ~1 ~ 0.5 1 0.5 1 10 normal
playsound minecraft:entity.firework_rocket.twinkle_far master @a ~ ~ ~ 1 1
effect give @s minecraft:jump_boost 30 2
```

### levitation.mcfunction

```mcfunction
effect give @s minecraft:levitation 1 30
effect give @s minecraft:slow_falling 30 0
playsound minecraft:block.beacon.activate master @a ~ ~ ~ 1 1
```

### nitroflight.mcfunction

```mcfunction
effect give @a[gamemode=!spectator,distance=..2] minecraft:resistance 1 4 true
effect give @s minecraft:slow_falling 20 0 true
execute rotated ~ 0 positioned ^ ^ ^-1 run summon minecraft:creeper ~ ~ ~ {Fuse:0s,Invulnerable:1b,ignited:1b}
execute rotated ~ 0 positioned ^ ^ ^-1 run summon minecraft:creeper ~ ~ ~ {Fuse:0s,Invulnerable:1b,ignited:1b}
execute rotated ~ 0 positioned ^ ^ ^-1 run summon minecraft:creeper ~ ~ ~ {Fuse:0s,Invulnerable:1b,ignited:1b}
execute rotated ~ 0 positioned ^ ^ ^-1 run summon minecraft:creeper ~ ~ ~ {Fuse:0s,Invulnerable:1b,ignited:1b}
execute rotated ~ 0 positioned ^ ^ ^-1 run summon minecraft:creeper ~ ~ ~ {Fuse:0s,Invulnerable:1b,ignited:1b}
execute rotated ~ 0 positioned ^ ^ ^-1 run summon minecraft:creeper ~ ~ ~ {Fuse:0s,Invulnerable:1b,ignited:1b}
execute rotated ~ 0 positioned ^ ^ ^-1 run summon minecraft:creeper ~ ~ ~ {Fuse:0s,Invulnerable:1b,ignited:1b}
execute rotated ~ 0 positioned ^ ^ ^-1 run summon minecraft:creeper ~ ~ ~ {Fuse:0s,Invulnerable:1b,ignited:1b}
execute rotated ~ 0 positioned ^ ^ ^-1 run summon minecraft:creeper ~ ~ ~ {Fuse:0s,Invulnerable:1b,ignited:1b}
```

### perdesh.mcfunction

```mcfunction
playsound minecraft:entity.bee.loop master @a ~ ~ ~ 1 0
scoreboard players operation *tempt lbcID2 = @s team_number
effect give @a[gamemode=!spectator,distance=0.01..10,predicate=!lbc:same_team] minecraft:nausea 10 0
effect give @e[distance=0.01..10,tag=!spectator,predicate=!lbc:same_team] minecraft:poison 3 0
effect give @e[distance=0.01..10,tag=!spectator,predicate=!lbc:same_team] minecraft:wither 3 0
particle minecraft:dust{color:[0.231,0.047,0.047],scale:1.0f} ~ ~1 ~ 7 7 7 1 1000 normal
```

### phase.mcfunction

```mcfunction
execute positioned ^ ^ ^10 run spreadplayers ~ ~ 0 5 under 120 false @s[predicate=lbc:the_nether]
execute positioned ^ ^ ^10 run spreadplayers ~ ~ 0 5 false @s[predicate=!lbc:the_nether]
playsound minecraft:entity.enderman.teleport master @a ~ ~ ~ 1 1
particle minecraft:portal ~ ~1 ~ 0 1 0 1 100 normal
```

### phase_speed.mcfunction

```mcfunction
particle minecraft:firework ~ ~1 ~ 0.5 1 0.5 1 100 normal
playsound minecraft:entity.firework_rocket.twinkle_far master @a ~ ~ ~ 1 1
effect give @s minecraft:speed 2 20
```

### ponos.mcfunction

```mcfunction
particle minecraft:falling_honey ^ ^ ^-0.5 0.1 0.1 0.1 0.5 100 normal
effect give @s minecraft:slowness 1 9 true
playsound minecraft:block.honey_block.break master @a ~ ~ ~ 1 0
scoreboard players operation *tempt lbcID2 = @s team_number
effect give @e[distance=0.01..10,tag=!spectator,predicate=!lbc:same_team] minecraft:poison 2 10
effect give @e[distance=0.01..10,tag=!spectator,predicate=!lbc:same_team] minecraft:wither 2 10
effect give @a[gamemode=!spectator,distance=0.01..10,predicate=!lbc:same_team] minecraft:nausea 2 10
```

### slow.mcfunction

```mcfunction
fill ~5 ~5 ~5 ~-5 ~-5 ~-5 minecraft:soul_soil replace #minecraft:solid strict
particle minecraft:soul ~ ~ ~ 3 3 3 0 20 normal
scoreboard players operation *tempt lbcID2 = @s team_number
effect give @e[distance=0.01..6,tag=!spectator,predicate=!lbc:same_team] minecraft:slowness 10 3
```

### smoke.mcfunction

```mcfunction
particle minecraft:block_marker{block_state:{Name:"minecraft:black_wool"}} ~ ~1 ~ 6 3 6 0.15 400 force @a[distance=0.01..]
playsound minecraft:block.fire.extinguish master @a ~ ~ ~ 2 1
```

### speed.mcfunction

```mcfunction
particle minecraft:firework ~ ~1 ~ 0.5 1 0.5 1 100 normal
playsound minecraft:entity.firework_rocket.twinkle_far master @a ~ ~ ~ 1 1
effect give @s minecraft:speed 10 4
effect give @s minecraft:jump_boost 10 4
```

### speed_only.mcfunction

```mcfunction
particle minecraft:firework ~ ~1 ~ 0.5 1 0.5 1 100 normal
playsound minecraft:entity.firework_rocket.twinkle_far master @a ~ ~ ~ 1 1
effect give @s minecraft:speed 30 1
```

### teleport.mcfunction

```mcfunction
spreadplayers ~ ~ 0 10 under 120 false @s[predicate=lbc:the_nether]
spreadplayers ~ ~ 0 10 false @s[predicate=!lbc:the_nether]
particle minecraft:portal ~ ~1 ~ 0 1 0 1 100 normal
playsound minecraft:entity.enderman.teleport master @a ~ ~ ~ 1 1
effect give @s minecraft:resistance 2 4
```

### teleportnazad.mcfunction

```mcfunction
scoreboard players operation *tempt lbcID2 = @s team_number
execute as @e[type=#minecraft:mobs,distance=0.01..10,tag=!spectator,predicate=!lbc:same_team] at @s run tp @s ^ ^ ^-3
particle minecraft:smoke ~ ~1 ~ 0.5 1 0.5 0.7 50 normal
playsound minecraft:entity.firework_rocket.large_blast master @a ~ ~ ~ 2 0
```

### transfusion.mcfunction

```mcfunction
scoreboard players operation *tempt lbcID2 = @s team_number
execute if entity @e[type=#minecraft:mobs,distance=0.01..10,tag=!spectator,predicate=!lbc:same_team] run effect give @s minecraft:regeneration 6 0 true
execute if entity @e[type=#minecraft:mobs,distance=0.01..10,tag=!spectator,predicate=!lbc:same_team] run effect give @s minecraft:saturation 1 40 true
scoreboard players add @s food_count 1
effect give @e[distance=0.01..10,tag=!spectator,predicate=!lbc:same_team] minecraft:wither 5 0
effect give @a[gamemode=!spectator,distance=0.01..10,predicate=!lbc:same_team] minecraft:hunger 5 0
playsound minecraft:block.portal.travel master @a ~ ~ ~ 0.15 1 0.15
particle minecraft:portal ~ ~1 ~ 0.5 1 0.5 1 100 normal
```

### weakall.mcfunction

```mcfunction
scoreboard players operation *tempt lbcID2 = @s team_number
effect give @e[distance=0.01..10,tag=!spectator,predicate=!lbc:same_team] minecraft:weakness 2 0
particle minecraft:ash ~ ~1 ~ 0.5 1 0.5 0.2 200 normal
playsound minecraft:entity.elder_guardian.curse master @a ~ ~ ~ 1 1
```

## Static release scope and source hash receipt

Current parent release scope is complete Multiplying + Randchanted ARMOR only;
Chaos above is next-family preparation, not implementation or qualification.
Exactly5+5 source tables: set_FAMILY plus neko_set_FAMILY/{helmet,chestplate,
leggings,boots}. No extra pre/full source wrappers exist for these families.
Current native files use entry modifier arrays and function type fields; this
is the active modern schema, not an inferred defect. No schema rewrite is
recommended. Only native codec/roll qualification can establish acceptance.

Original source file SHA256 values follow. These are original file hashes,
including original formatting; they are not normalized native payload hashes.
- neko_set_multiplying/helmet.json : `1DB465C8600C149350093BB1FFFAEEEDC0C35546E37A36CD56D496EBB25C2DC5`
- neko_set_multiplying/chestplate.json : `990CCD61F9082C26931FCB3B118014436FBCEDB418466193C9377191BFB289D7`
- neko_set_multiplying/leggings.json : `AAA0CBC976932074826C60CEB1185A868C828FBBC02E8A41DBCBB558493A4D6D`
- neko_set_multiplying/boots.json : `D5D8D4AD48EFBAB4D56CC35C4230BEC312AEBB6CD0532C80E3A1E9EB4F1B0BCE`
- set_multiplying.json : `F7F03C0E49B891D5697E16F485F8D61E9A7E753A8DB3870B8C5A41D9C752FF2D`
- neko_set_randchanted/helmet.json : `C50350FF40C5F85EB1F3D299666992EB9214515E21F28676455A2F7E4E9988FA`
- neko_set_randchanted/chestplate.json : `C684ED01BAF20A0BDF20E7730AE56AE8B2EA99917CE93CA8CD3E170FCF071E34`
- neko_set_randchanted/leggings.json : `98B2BC010591EDB2F50F8590BB4C05015A53CD329CC76F2E0640C67B0BE59CDA`
- neko_set_randchanted/boots.json : `C038265509A5C9A67A9C682B6914937C09F4FC560D5806D7C1641ECF9670EFA1`
- set_randchanted.json : `6D0B4A294DE88E8654ACF5A00F94F50E68056AABF4655A54FAF5615E50088D0B`

Exact Multiplying armor / toughness IDs, paired per slot:

- head: dhgfxhdhgfdhhghr / hdhdfhgghughness; amount2 each.
- chest: geghfnhfnhfor / gefkhkdyhnbnbess; amount4 each.
- legs: genedgjdgnbvr / whdrtgxgggggs; amount3 each.
- feet: generhfhfihchfarmor / generjjjjjjjjjjoughness; amount1 each.

Both static armor advancements are player_killed_entity, require victim player,
nonspectator wearer and all4 actual raw flags. Multiplying parent
lbc:true_advancements/armor and gain/kit reward; Randchanted parent
lbc:true_advancements/armor/multiplying and gain/skill reward.
Normal transformables_{helmet,chestplate,leggings,boots} entries8/12 reference
respective Multiplying/Randchanted leaves. Ultra source applies multiplying
unconditionally on its flag, Randchanted except sword/pickaxe/bow/axe. Global
function search finds static family flags only in Ultra and TMT storage; no
separate armor tick/incoming/melee/cast consumer exists.
