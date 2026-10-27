# Chaos Sword and Kara source closure — 27 October 2026

Read-only audit. Source root S = `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/`.
All source paths below are relative to S. No Java, source-server, resource-pack, build or live server changes. Root AGENTS author priorities, agent_notes/AGENTS.md and RESOURCE_PACK.md apply. User requires inherited kill attribution for indirect consequences; missing source attribution is a migration gap. Suspicious mechanics require an author decision; author asleep: report questions to parent, do not guess.

## Entry and acquisition closure

`lbc/advancement/gowno.json` uses player_hurt_entity, blocked=false, damage type outside minecraft:arrow_only, victim != interaction, owner dimension neither minecraft:nexus nor minecraft:imprinted. Reward `lbc:items/swords` inspects current SelectedItem custom_data. Lines58/60 invoke chaos_pre / kara hit. End revokes gowno. These are accepted non-arrow damage events, including indirect damage credited to player while mainhand matches; not a right-click/sneak input or charge mechanic. No branch-local mana, cooldown, antimagic or stopper guard. Arrow child damage avoids recursive activation via advancement. No item-specific craft, advancement unlock, HUD, score reset or input route was found anywhere in full data search for chaos_swo/kara/helper names; existing tags list and TMT storage merely catalogue flags.

Acquisition: `lbc/loot_table/luckyblock_imba.json` and `random_glack_1.json` -> `imba/lucky_items_imba.json` -> diamondtools (weight10 quality0, line617), slabs(weight10 quality7,line869). Diamondtools chaos pool rolls uniform0..1 and emits one golden_sword: unbreakable{}, tooltip lbc:tier_s, custom_data {chaos_swo:1,imba:1}; vanilla golden sword model, no enchantment/attribute rewrite. Name component array [{translate:imba_sword,italic:false}]; single lore [{translate:imba_version,italic:false}], no explicit colors.

Slabs table pool rolls uniform1..8, bonus_rolls uniform0..8, equal default entry weight. Kara entries at lines172..313 are waxed_oxidized_cut_copper_slab, waxed_weathered_cut_copper_slab, waxed_exposed_cut_copper_slab. All count1, custom_data {kara:1}, tooltip lbc:tier_ss, Smite255. Oxidized name god_slab_imba; weathered/exposed not_decaying_slab_imba. Each name is one array component italic:false, no color. Lore exactly two lines: god_slab_imba.lore italic:false yellow; imba_version italic:false dark_purple. No item_model override, so each retains its own vanilla slab model. Ordinary vanilla melee/Smite behavior remains separate from carrier behavior.

## Chaos: exact spawn and motion

`lbc/function/items/swords/chaos_sword_imba_pre.mcfunction` has 80 unconditional calls, not40. Two identical40-call batches. First batch offsets (x,y,z):
(0,15,0),(11,14,0),(0,13,11),(-11,12,0),(0,10,-11),(0,9,0),(11,8,11),(-11,7,11),(-11,6,-11),(11,5,-11);
(0,16,0),(11,17,0),(0,18,11),(-11,19,0),(0,20,-11),(0,21,0),(11,22,11),(-11,23,11),(-11,24,-11),(11,25,-11);
(0,26,0),(11,27,0),(0,28,11),(-11,29,0),(0,30,-11),(0,31,0),(11,32,11),(-11,33,11),(-11,34,-11),(11,35,-11);
(0,36,0),(11,37,0),(0,38,11),(-11,39,0),(0,40,-11),(0,41,0),(11,42,11),(-11,43,11),(-11,44,-11),(11,5,-11).
Thus39 distinct spawn positions:38 positions occur twice and (11,5,-11) occurs four times, total80. Missing y11 and apparent y45 typo are literal source, not inferred contract.

`items/swords/chaos_sword_imba.mcfunction` summons an item_display golden_sword count1, fixed, teleport/interpolation_duration1, shadow radius/strength0. Matrix exactly [0,0,2.5,0,1.7678,-1.7678,0,0,1.7678,1.7678,0,0,0,0,0,3]. Tags homing_sword,slowraycast,imba_rc,magic. Selects local <=.01 unactived display limit1, calls random_rot then copies owner's lbcID2/team_number and adds actived. `random_rot` random integer yaw -180000..180000 scaled.001, pitch -90000..90000 scaled.001. This randomization/actived disambiguation lets overlapping source spawns each initialize independently, unless unrelated unactived carriers are coincident.

`tick.mcfunction` -> tick_not_timestopped -> every slowraycast type in #minecraft:raycasters (includes item_display and marker), excluding nexus/imprinted -> raycastselector: Lifetime +=1 then imba selector -> homing_sword. Global time stop skips these functions entirely. Generic data.lifetime expiry does not apply: chaos has no data.lifetime and no local Lifetime maximum. It may fly indefinitely until hit/embedded/antimagic/match cleanup.

homing_sword: current block air and !inground -> homing_sword_2; inground and local-forward .5 air -> break; inground 1% predicate -> break; current block nonair and !inground -> break_pre. No swept wall test or universal range limit.

homing_sword_2 marks every nonspectator player whose lbcID2 equals carrier as nodmg, every nonspectator player whose team_number equals carrier as nodmg1. Aim target: nearest #minecraft:mobs (includes players), !nodmg,!nodmg1,!spectator,!strict_map_object, NO range cap. Exact steering command is preserved in appendix: compute a point .1 toward target, then1 along old rotation, face back through carrier then reverse, yielding gradual turn rather than snap-target. Move1 along resulting direction per active tick. With no target the facing branch has no result and no move, but collision check still runs.

Collision checks the ORIGINAL function execution position, not refreshed after tp: #mobs dx=dy=dz=0 distance<=8 !nodmg !nodmg1 !spectator limit1, unspecified sort. This is native selector bounding-box overlap with a1x1x1 selector volume, not an8-block sphere hit radius. strict_map_object is excluded for AIM but not damage. On contact, damage4 arrow by nodmg owner if available, otherwise4 generic; crit particles .1 spread,.5 speed,count8 normal, player.attack.strong master volume1 pitch1; discard carrier. Removes nodmg/nodmg1 from all players after routine. Copying player identifiers and transient selector tags are legacy plumbing; preserve native owner/team attribution without global mutable-tag cross-talk.

break_pre recursively backs execution context .1 until an air block, then tp carrier local-forward .5; no recursion bound. Plays copper.break master1.5 pitch1; adds inground. Embedded carrier stays fixed until forward .5 air or independent1% chance/active tick. break plays item.break master.5 pitch1 and item particles golden_sword count10 spread.2 speed0 normal; bone-tag variant emits bone particles. homing_sword_dmg_mob is NOT called by chaos: it is shared by triple-golden mob throwing,4 arrow credited to nearby dungeon_mob<=20 or generic fallback; do not attach dungeon behavior to chaos.

## Kara: terrain fan closure

hit summons marker at owner eye anchor, yaw retained/pitch0, forward1.5, tags slowraycast,imba_rc,imba_kara,central,magic. Second command selects ANY marker<=.01 limit1 (no family/tag restriction), teleports in same execution context to set yaw/pitch. No owner IDs/team/data lifetime are copied. Native ownership must be carried for caused terrain/fall/void/death effects per user requirement.

Each active tick after shared Lifetime+=1, tick_kara: fill column at current flooredX/Z from absolute y0 through319 with air DESTROY (drops, block-break consequences; no block type/arena/team filters). Then end_rod particle spread(0,50,0),speed0,count100 FORCE; generic.explode master to players<=50 volume.1 pitch1 minVolume.1. No direct damage command or lightning summon. If central, create two children BEFORE movement/expiry. Move forward.7, then kill if Lifetime>=100. Thus central operates100 ticks; at age100 still fills and creates children.

Each tick central summons child marker with same family tags except central; copies central Lifetime. Child1 gets execution-local left offset+.8 and yaw+30; child2 offset-.8 and yaw-30. Neither tp uses `at @s`; coordinate frame and rotation are central's, despite selector child source. Children branch only from central, never recursively. Each child thereafter independently fills its column each tick and moves.7 at fixed angled yaw, ending at inherited age100. New children do not inherit owner/team. Scheduling is selector snapshot dependent: newly summoned children normally first join next shared tick; children born at age100 can process once at101 before discard. Native port should explicitly document this boundary instead of claiming all children vanish exactly with central. No direct target filter: terrain destruction affects whoever subsequently loses support.

## Shared helpers, lifecycle and known gaps

Antimagic `other/artifact/nomagic.mcfunction` kills magic entities<=25; no branch-local cast admission. Magic tag also makes both vulnerable to danmaku bomb entity purge. Shared homing skill30 can redirect chaos if owner ID positive, skills enabled,50% chance, no player<=20, !cant_be_homing; raycastselector_player_check_skill compares owner/team and calls skills/homing/tick_markers. Kara lacks owner ID so normally bypasses. Source generic data lifetime kill has no effect without positive data.lifetime. Neither family has dedicated match reset tags/scores or retained player HUD. Native integration must use established match/epoch cleanup and Time Stop, resident terrain guards, combat/owner rules; these are requirements of existing platform, not evidence source already does them.

Unresolved author choices: duplicated80 chaos volley versus intended40; repeated y5 versus y45; no chaos flight expiry; unsafe unbounded backward wall recursion; Kara fixed absolute y0..319 destroy/drop columns and final-child boundary; Kara marker selector collision with unrelated markers; damage target allows strict_map_object while aiming rejects it. Do not silently fix these or call them intentional. Bounded independent implementation can proceed for other families while answers pending.

## Exact resource-pack text

Read-only pack `C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8/assets/minecraft/lang/`. Keys exist in en_us,ru_ru,rpr (rpr values same as ru_ru). Preserve keys and supplied component styles, no template additions needed for these keys.

| key | ru_ru / rpr | en_us |
|---|---|---|
| imba_sword | Хаос-меч Хаос-мечей | Chaos Sword of Chaos Swords |
| imba_version | От этого предмета веет невообразимым чувстом юмора | This item exudes an unimaginable sense of humor |
| god_slab_imba | Постиндустриальное общество | Post-industrial society |
| not_decaying_slab_imba | Неразложившиеся ядерные отходы | Undecomposed nuclear waste |
| god_slab_imba.lore | Враг твой постигнет кару небесную. | Creates holy fury on hit |

No custom carrier model links: chaos carrier and held item both minecraft:golden_sword; Kara held native slab models, carrier invisible marker. Tooltip links lbc:tier_s and lbc:tier_ss are existing source declarations. Parent owns detailed model/tooltip resource integration. No graphical client observation made.

## Literal family functions

### items/swords/chaos_sword_imba.mcfunction

```mcfunction
summon minecraft:item_display ~ ~ ~ {interpolation_duration:1,shadow_radius:0.0f,shadow_strength:0.0f,teleport_duration:1,item_display:"fixed",transformation:[0.0f,0.0f,2.5f,0.0f,1.7678f,-1.7678f,0.0f,0.0f,1.7678f,1.7678f,0.0f,0.0f,0.0f,0.0f,0.0f,3.0f],item:{id:"minecraft:golden_sword",count:1},Tags:["homing_sword","slowraycast","imba_rc","magic"]}
execute as @e[type=minecraft:item_display,distance=..0.01,tag=homing_sword,tag=!actived,limit=1] at @s run function lbc:random_rot
scoreboard players operation @e[type=minecraft:item_display,distance=..0.01,tag=homing_sword,tag=!actived,limit=1] lbcID2 = @s lbcID2
scoreboard players operation @e[type=minecraft:item_display,distance=..0.01,tag=homing_sword,tag=!actived,limit=1] team_number = @s team_number
tag @e[type=minecraft:item_display,distance=..0.01,tag=homing_sword,tag=!actived,limit=1] add actived
```


### items/swords/chaos_sword_imba_pre.mcfunction

```mcfunction
execute positioned ~ ~15 ~ run function lbc:items/swords/chaos_sword_imba
execute positioned ~11 ~14 ~ run function lbc:items/swords/chaos_sword_imba
execute positioned ~ ~13 ~11 run function lbc:items/swords/chaos_sword_imba
execute positioned ~-11 ~12 ~ run function lbc:items/swords/chaos_sword_imba
execute positioned ~ ~10 ~-11 run function lbc:items/swords/chaos_sword_imba
execute positioned ~ ~9 ~ run function lbc:items/swords/chaos_sword_imba
execute positioned ~11 ~8 ~11 run function lbc:items/swords/chaos_sword_imba
execute positioned ~-11 ~7 ~11 run function lbc:items/swords/chaos_sword_imba
execute positioned ~-11 ~6 ~-11 run function lbc:items/swords/chaos_sword_imba
execute positioned ~11 ~5 ~-11 run function lbc:items/swords/chaos_sword_imba

execute positioned ~ ~16 ~ run function lbc:items/swords/chaos_sword_imba
execute positioned ~11 ~17 ~ run function lbc:items/swords/chaos_sword_imba
execute positioned ~ ~18 ~11 run function lbc:items/swords/chaos_sword_imba
execute positioned ~-11 ~19 ~ run function lbc:items/swords/chaos_sword_imba
execute positioned ~ ~20 ~-11 run function lbc:items/swords/chaos_sword_imba
execute positioned ~ ~21 ~ run function lbc:items/swords/chaos_sword_imba
execute positioned ~11 ~22 ~11 run function lbc:items/swords/chaos_sword_imba
execute positioned ~-11 ~23 ~11 run function lbc:items/swords/chaos_sword_imba
execute positioned ~-11 ~24 ~-11 run function lbc:items/swords/chaos_sword_imba
execute positioned ~11 ~25 ~-11 run function lbc:items/swords/chaos_sword_imba

execute positioned ~ ~26 ~ run function lbc:items/swords/chaos_sword_imba
execute positioned ~11 ~27 ~ run function lbc:items/swords/chaos_sword_imba
execute positioned ~ ~28 ~11 run function lbc:items/swords/chaos_sword_imba
execute positioned ~-11 ~29 ~ run function lbc:items/swords/chaos_sword_imba
execute positioned ~ ~30 ~-11 run function lbc:items/swords/chaos_sword_imba
execute positioned ~ ~31 ~ run function lbc:items/swords/chaos_sword_imba
execute positioned ~11 ~32 ~11 run function lbc:items/swords/chaos_sword_imba
execute positioned ~-11 ~33 ~11 run function lbc:items/swords/chaos_sword_imba
execute positioned ~-11 ~34 ~-11 run function lbc:items/swords/chaos_sword_imba
execute positioned ~11 ~35 ~-11 run function lbc:items/swords/chaos_sword_imba

execute positioned ~ ~36 ~ run function lbc:items/swords/chaos_sword_imba
execute positioned ~11 ~37 ~ run function lbc:items/swords/chaos_sword_imba
execute positioned ~ ~38 ~11 run function lbc:items/swords/chaos_sword_imba
execute positioned ~-11 ~39 ~ run function lbc:items/swords/chaos_sword_imba
execute positioned ~ ~40 ~-11 run function lbc:items/swords/chaos_sword_imba
execute positioned ~ ~41 ~ run function lbc:items/swords/chaos_sword_imba
execute positioned ~11 ~42 ~11 run function lbc:items/swords/chaos_sword_imba
execute positioned ~-11 ~43 ~11 run function lbc:items/swords/chaos_sword_imba
execute positioned ~-11 ~44 ~-11 run function lbc:items/swords/chaos_sword_imba
execute positioned ~11 ~5 ~-11 run function lbc:items/swords/chaos_sword_imba





execute positioned ~ ~15 ~ run function lbc:items/swords/chaos_sword_imba
execute positioned ~11 ~14 ~ run function lbc:items/swords/chaos_sword_imba
execute positioned ~ ~13 ~11 run function lbc:items/swords/chaos_sword_imba
execute positioned ~-11 ~12 ~ run function lbc:items/swords/chaos_sword_imba
execute positioned ~ ~10 ~-11 run function lbc:items/swords/chaos_sword_imba
execute positioned ~ ~9 ~ run function lbc:items/swords/chaos_sword_imba
execute positioned ~11 ~8 ~11 run function lbc:items/swords/chaos_sword_imba
execute positioned ~-11 ~7 ~11 run function lbc:items/swords/chaos_sword_imba
execute positioned ~-11 ~6 ~-11 run function lbc:items/swords/chaos_sword_imba
execute positioned ~11 ~5 ~-11 run function lbc:items/swords/chaos_sword_imba

execute positioned ~ ~16 ~ run function lbc:items/swords/chaos_sword_imba
execute positioned ~11 ~17 ~ run function lbc:items/swords/chaos_sword_imba
execute positioned ~ ~18 ~11 run function lbc:items/swords/chaos_sword_imba
execute positioned ~-11 ~19 ~ run function lbc:items/swords/chaos_sword_imba
execute positioned ~ ~20 ~-11 run function lbc:items/swords/chaos_sword_imba
execute positioned ~ ~21 ~ run function lbc:items/swords/chaos_sword_imba
execute positioned ~11 ~22 ~11 run function lbc:items/swords/chaos_sword_imba
execute positioned ~-11 ~23 ~11 run function lbc:items/swords/chaos_sword_imba
execute positioned ~-11 ~24 ~-11 run function lbc:items/swords/chaos_sword_imba
execute positioned ~11 ~25 ~-11 run function lbc:items/swords/chaos_sword_imba

execute positioned ~ ~26 ~ run function lbc:items/swords/chaos_sword_imba
execute positioned ~11 ~27 ~ run function lbc:items/swords/chaos_sword_imba
execute positioned ~ ~28 ~11 run function lbc:items/swords/chaos_sword_imba
execute positioned ~-11 ~29 ~ run function lbc:items/swords/chaos_sword_imba
execute positioned ~ ~30 ~-11 run function lbc:items/swords/chaos_sword_imba
execute positioned ~ ~31 ~ run function lbc:items/swords/chaos_sword_imba
execute positioned ~11 ~32 ~11 run function lbc:items/swords/chaos_sword_imba
execute positioned ~-11 ~33 ~11 run function lbc:items/swords/chaos_sword_imba
execute positioned ~-11 ~34 ~-11 run function lbc:items/swords/chaos_sword_imba
execute positioned ~11 ~35 ~-11 run function lbc:items/swords/chaos_sword_imba

execute positioned ~ ~36 ~ run function lbc:items/swords/chaos_sword_imba
execute positioned ~11 ~37 ~ run function lbc:items/swords/chaos_sword_imba
execute positioned ~ ~38 ~11 run function lbc:items/swords/chaos_sword_imba
execute positioned ~-11 ~39 ~ run function lbc:items/swords/chaos_sword_imba
execute positioned ~ ~40 ~-11 run function lbc:items/swords/chaos_sword_imba
execute positioned ~ ~41 ~ run function lbc:items/swords/chaos_sword_imba
execute positioned ~11 ~42 ~11 run function lbc:items/swords/chaos_sword_imba
execute positioned ~-11 ~43 ~11 run function lbc:items/swords/chaos_sword_imba
execute positioned ~-11 ~44 ~-11 run function lbc:items/swords/chaos_sword_imba
execute positioned ~11 ~5 ~-11 run function lbc:items/swords/chaos_sword_imba

```


### E:\servers_mew\cringe1_21_4_skywars\world\datapacksdisabled\LBC\data\lbc\function\items\swords\chaos_sword_imba\homing_sword.mcfunction

```mcfunction


execute if block ~ ~ ~ #minecraft:airs unless entity @s[tag=inground] run function lbc:items/swords/chaos_sword_imba/homing_sword_2
execute if block ^ ^ ^.5 #minecraft:airs if entity @s[tag=inground] run function lbc:items/swords/chaos_sword_imba/homing_sword_break
execute if entity @s[tag=inground] if predicate lbc:chance1 run function lbc:items/swords/chaos_sword_imba/homing_sword_break
execute unless block ~ ~ ~ #minecraft:airs if entity @s[tag=!inground] run function lbc:items/swords/chaos_sword_imba/homing_sword_break_pre
```


### E:\servers_mew\cringe1_21_4_skywars\world\datapacksdisabled\LBC\data\lbc\function\items\swords\chaos_sword_imba\homing_sword_2.mcfunction

```mcfunction
execute at @a[gamemode=!spectator] if score @a[gamemode=!spectator,distance=..0.001,limit=1] lbcID2 = @s lbcID2 run tag @a[gamemode=!spectator,distance=..0.001,limit=1] add nodmg
execute at @a[gamemode=!spectator] if score @a[gamemode=!spectator,distance=..0.001,limit=1] team_number = @s team_number run tag @a[gamemode=!spectator,distance=..0.001,limit=1] add nodmg1
execute facing entity @e[type=#minecraft:mobs,tag=!nodmg,tag=!nodmg1,tag=!spectator,sort=nearest,limit=1,tag=!strict_map_object] eyes positioned ^ ^ ^0.1 rotated as @s positioned ^ ^ ^1 facing entity @s eyes facing ^ ^ ^-1 positioned as @s run tp @s ^ ^ ^1 ~ ~
execute if entity @e[type=#minecraft:mobs,dx=0,dy=0,dz=0,distance=..8,tag=!nodmg,tag=!nodmg1,tag=!spectator,limit=1] run function lbc:items/swords/chaos_sword_imba/homing_sword_dmg

tag @a remove nodmg
tag @a remove nodmg1

```


### E:\servers_mew\cringe1_21_4_skywars\world\datapacksdisabled\LBC\data\lbc\function\items\swords\chaos_sword_imba\homing_sword_break.mcfunction

```mcfunction
playsound minecraft:entity.item.break master @a ~ ~ ~ 0.5 1
execute unless entity @s[tag=bone] run particle minecraft:item{item:{id:"minecraft:golden_sword"}} ~ ~ ~ 0.2 0.2 0.2 0 10 normal
execute if entity @s[tag=bone] run particle minecraft:item{item:{id:"minecraft:bone"}} ~ ~ ~ 0.2 0.2 0.2 0 10 normal
kill @s
```


### E:\servers_mew\cringe1_21_4_skywars\world\datapacksdisabled\LBC\data\lbc\function\items\swords\chaos_sword_imba\homing_sword_break_pre.mcfunction

```mcfunction
function lbc:items/swords/chaos_sword_imba/homing_sword_break_raycheck

playsound minecraft:block.copper.break master @a ~ ~ ~ 1.5 1
tag @s add inground
```


### E:\servers_mew\cringe1_21_4_skywars\world\datapacksdisabled\LBC\data\lbc\function\items\swords\chaos_sword_imba\homing_sword_break_raycheck.mcfunction

```mcfunction

execute if block ~ ~ ~ #minecraft:airs run tp @s ^ ^ ^0.5
execute unless block ~ ~ ~ #minecraft:airs positioned ^ ^ ^-0.1 run function lbc:items/swords/chaos_sword_imba/homing_sword_break_raycheck
```


### E:\servers_mew\cringe1_21_4_skywars\world\datapacksdisabled\LBC\data\lbc\function\items\swords\chaos_sword_imba\homing_sword_dmg.mcfunction

```mcfunction
execute if entity @a[tag=nodmg,limit=1] as @e[type=#minecraft:mobs,dx=0,dy=0,dz=0,distance=..8,tag=!nodmg,tag=!nodmg1,tag=!spectator,limit=1] run damage @s 4 arrow by @a[tag=nodmg,limit=1]
execute unless entity @a[tag=nodmg,limit=1] as @e[type=#minecraft:mobs,dx=0,dy=0,dz=0,distance=..8,tag=!nodmg,tag=!nodmg1,tag=!spectator,limit=1] run damage @s 4 generic
particle minecraft:crit ~ ~ ~ 0.1 0.1 0.1 0.5 8 normal
playsound minecraft:entity.player.attack.strong master @a ~ ~ ~ 1 1
kill @s
```


### E:\servers_mew\cringe1_21_4_skywars\world\datapacksdisabled\LBC\data\lbc\function\items\swords\chaos_sword_imba\homing_sword_dmg_mob.mcfunction

```mcfunction
execute if entity @e[distance=..20,tag=dungeon_mob,limit=1] as @e[type=#minecraft:mobs,dx=0,dy=0,dz=0,distance=..8,tag=!dungeon_mob,tag=!spectator,limit=1] run damage @s 4 arrow by @e[distance=..20,tag=dungeon_mob,limit=1]
execute unless entity @e[distance=..20,tag=dungeon_mob,limit=1] as @e[type=#minecraft:mobs,dx=0,dy=0,dz=0,distance=..8,tag=!dungeon_mob,tag=!spectator,limit=1] run damage @s 4 generic
particle minecraft:crit ~ ~ ~ 0.1 0.1 0.1 0.5 8 normal
playsound minecraft:entity.player.attack.strong master @a ~ ~ ~ 1 1
kill @s
```


### E:\servers_mew\cringe1_21_4_skywars\world\datapacksdisabled\LBC\data\lbc\function\items\swords\imba_kara_slab\hit.mcfunction

```mcfunction
execute anchored eyes rotated ~ 0 positioned ^ ^ ^1.5 run summon minecraft:marker ~ ~ ~ {Tags:["slowraycast","imba_rc","imba_kara","central","magic"]}
execute anchored eyes rotated ~ 0 positioned ^ ^ ^1.5 as @e[type=minecraft:marker,distance=..0.01,limit=1] run tp @s ~ ~ ~ ~ ~
```


### E:\servers_mew\cringe1_21_4_skywars\world\datapacksdisabled\LBC\data\lbc\function\items\swords\imba_kara_slab\tick_kara.mcfunction

```mcfunction
fill ~ 0 ~ ~ 319 ~ minecraft:air destroy
particle minecraft:end_rod ~ ~ ~ 0 50 0 0 100 force
playsound minecraft:entity.generic.explode master @a[distance=..50] ~ ~ ~ 0.1 1 0.1
execute if entity @s[tag=central] run function lbc:items/swords/imba_kara_slab/tick_kara_central

tp @s ^ ^ ^.7


kill @s[scores={Lifetime=100..}]
```


### E:\servers_mew\cringe1_21_4_skywars\world\datapacksdisabled\LBC\data\lbc\function\items\swords\imba_kara_slab\tick_kara_central.mcfunction

```mcfunction
summon minecraft:marker ~ ~ ~ {Tags:["slowraycast","imba_rc","imba_kara","magic"]}
scoreboard players operation @e[type=minecraft:marker,distance=..0.01,tag=imba_kara,tag=!central,limit=1] Lifetime = @s Lifetime
execute as @e[type=minecraft:marker,distance=..0.01,tag=imba_kara,tag=!central,limit=1] run tp @s ^0.8 ^ ^ ~30 ~

summon minecraft:marker ~ ~ ~ {Tags:["slowraycast","imba_rc","imba_kara","magic"]}
scoreboard players operation @e[type=minecraft:marker,distance=..0.01,tag=imba_kara,tag=!central,limit=1] Lifetime = @s Lifetime
execute as @e[type=minecraft:marker,distance=..0.01,tag=imba_kara,tag=!central,limit=1] run tp @s ^-0.8 ^ ^ ~-30 ~
```


Shared skill30 helper exact extension: `swrg_kit_integration/game/skills/homing/tick_markers.mcfunction` uses nearest eligible mob within100, !spectator !nodmg !nodmg1 !strict_map_object; same .1-versus1 gradual steering construction as chaos, but teleports ^ ^ ^ (rotation only, no movement), then red dust color[1,0,0] scale2 count3 spread.1 speed0 force. Therefore on admitted skill ticks it can rotate chaos before its own normal homing movement. Skill helper does not spawn extra carriers, damage or change Lifetime.
