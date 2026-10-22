# Next small special-drop batch — source audit

Current implementation and qualification: [anvil and creature batch](ANVIL_CREATURE_BATCH_PORT.md). The following source audit predates implementation. The current RP path in RESOURCE_PACK.md (26.3(1)) was also verified read-only; the selected translation keys and values agree.

22 October2026. Read-only recommendation, not implementation or qualification.
S = `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`.
Read current AGENTS, agent_notes rules and actual LbcSpecialDrops recognition;
completed OP Anvil/Slab/Yamete/Death Lust and all previous cages are excluded.

Selected current scope: THREE remaining closed mechanics, FOUR possible native
presentation leaves: Old Vex; Old Fat Skeleton; Shulker Bullets normal/plain
legacy. None requires an unported custom reward ability. Potion Spawner normal/
old below is researched but DEFERRED to its own complete producer group; its18
effects/persistent ownership are outside the current patch. No implementation/
build/server/RP/source writes occurred.

## 1. Old Vex — one plain leaf

S `function/drops/specialdrops.mcfunction:49` -> `drops/vex_old.mcfunction:1–9`.
Five native Vex at local ^ ^ ^3, alternating four teleports of executor yaw+72.
Executor is the dropped item, not nearest player; retain its initial pitch/yaw,
fractional coordinates and exact local-coordinate rotation. Native Health1 and
max_health1; no persistence, custom loot, equipment, AI or lifetime authored.
Use actual native Vex behavior/ordinary drops and causal owner/role/epoch cleanup.
S `loot_table/legacy_luckyblock.json:1506–1523`: barrier,count1,empty components,
{vex_old:1,drop:1}; no name/lore/tier/model. No reward table dependency.

Normal Vex remains excluded: `drops/vex:1–10` -> `loot_table/vex_sword:1–21`
10% chance -> `vexsword.json` custom magic sword. No Java vexsword flag support
was found; existing LbcSummons SoulVex/psychovex is a different mechanic.

## 2. Old Fat Skeleton — one plain leaf

S `function/drops/specialdrops.mcfunction:57`: one native skeleton at token XYZ,
PersistenceRequired1, name visible/nonitalic translated fat_skeleton. Equipment:
mainhand named nonitalic omega_bone, Knockback1, four mainhand modifiers:
attack_damage add_value7 (minecraft:base_attack_damage), attack_speed add_value−3.5
(minecraft:base_attack_speed), knockback_resistance add_value1 (d90ihe89fyw39tg),
movement_speed add_multiplied_base−.7 (ih89urgh8ghfdg). Feet bone_meal64, legs
bone32, chest iron_chestplate1, head skeleton_skull1. Guaranteed drops only for
mainhand/legs/feet; chest/head drop chance omitted and must retain native defaults.
Attributes: movement_speed.6, armor20, attack_knockback100, armor_toughness5.
No custom death table/tag, extra attack_damage base, custom health or custom
recurring hook is authored. Native melee/gear/vanilla death loot completely close
this leaf. Preserve exact modifier IDs and source names on dropped equipment.
S `loot_table/legacy_luckyblock.json:1203–1220`: plain barrier,count1,
empty components,{skeleton_old:1,drop:1}; no custom name/lore/tier/model.

Normal Skeleton remains excluded: specialdrops:56 -> fatskeleton.json includes
random_sauvojen_wand. Do not substitute its relic/gear for old or open that tree.

## 3. Shulker Bullets — normal plus plain legacy

S `function/drops/specialdrops.mcfunction:47` -> `drops/shulkerbullets:1–10`.
Nine native shulker_bullet at token+(x,2,z), all x/z in−1..1: cardinalZ first,
then corners, cardinalX,center. Each has angry tag and Steps40. Line10 selects
nearby angry-tagged shulker_bullets within3,limit9, then FROM EACH bullet's
position writes nearest nonspectator UUID into Target. No enemy/radius/sight,
owner/existing shooter, shared center target or invented trajectory is authored.
Native projectile motion, hit/levitation/damage/reflection/destruction and actual
thrower attribution/role safety/time-stop/epoch cleanup form the whole closure.
No loot/custom reward. Self/allies remain source-target candidates under common
native role rules. Do not invent magic denial from unrelated wand callers.

Source selector nuance for root review: line10 can retarget a PREEXISTING angry
bullet within3, not necessarily just the nine new ones; default entity ordering
and limit9 matter. It also chooses target independently at each bullet. Decide
explicitly whether established explicit-handle migration policy preserves this
scope or author clarification is needed; do not silently replace it with one
nearest enemy target. This is the only potentially suspicious source interaction
found in this bounded batch.

Tokens: S `loot_table/traps.json:534–577`,count1 repeating_command_block,
weight10 quality−10, tier_ff/drop_trap, yellow/nonitalic trap_shulker, one white/
nonitalic trap_desc,{bullets:1,drop:1}. S `legacy_luckyblock.json:2381–2398`,
plain barrier/count1/empty components/same flags. Keep distinct presentation.

## Deferred producer: Potion Spawner — normal and old

All source is inline `function/drops/specialdrops.mcfunction:35–37`.
Normal35: green entity_effect color[0,1,0,1], tokenY+5,delta(.3,4,.3),speed0,
count300,normal. Normal36/old37: ordinary replace spawner at tokenY+10.

| Native spawner NBT | Normal | Old |
| --- | --- | --- |
| SpawnCount / Delay / MinSpawnDelay / MaxSpawnDelay |1 /1 /4 /5 |1 /1 /4 /5 |
| SpawnRange |6 |15 |
| MaxNearbyEntities |200 |1000 |
| RequiredPlayerRange |35 |100 |

No old particle command, custom timer, removal or replacement TTL exists.
SpawnData initial splash_potion is custom-color8388608,instant_damage amp0,
duration20,count1. Both SpawnPotentials contain the SAME18 weight1 entries:

| Effect | Amplifier | Duration | custom_color |
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

All native splash_potion/count1/custom_effects. No reward table/custom item/ability
or recurring datapack hook. Keep native delay/random/spawn rejection/activation
semantics rather than a handwritten every4ticks loop. Inspect current26.3 potion
entity/item schema and real spawner serialization before implementation. Carry
actual owner/epoch through block, spawned potions and accepted effects; native
beneficial effects still retain authored values. Block replacement/destruction,
chunk unload/reload, projectile cleanup, owner-caused poison/wither/fall/void,
strict common role safety, time-stop and quiet stop need explicit qualification.
No new source lifetime or owner-death cancellation should be invented.

Tokens: S `loot_table/traps.json:446–489`,count1 repeating_command_block,
weight10 quality−10, tier_ff/drop_trap, yellow/nonitalic trap_potions plus white/
nonitalic trap_desc,{badspawner:1,drop:1}. S `legacy_luckyblock.json:2188–2205`:
plain barrier,count1,empty components,{badspawner_old:1,drop:1}.
Do not confuse these with completed lookuppotions cage.

## Resource-pack references and common scope

Read-only RP:
`C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3/minecraft/resourcepacks/all_in_one_1.21.8`.
`assets/minecraft/lang/{ru_ru,rpr,en_us}.json` existing values:

| Key/line | RU/RPR | EN |
| --- | --- | --- |
| fat_skeleton391 |Очень толстый скелет |Very fat skeleton |
| omega_bone392 |Омега Кость |Omega bone |
| trap_potions2595 |Ловушка: Спаунер Зелий |Trap: Potion Spawner |
| trap_shulker2597 |Ловушка: Шалкер-пули |Trap: Shulker Bullets |

Reuse existing trap_desc/style/model and preserve old plain-barrier/gear text;
no translations/RP additions requested. Native recognition should follow existing
nonempty sourceflags+drop compatibility, with source-order independent branches
and one shared unit debit. Expose only qualified standalone leaves, never full
legacy/traps/imba pools or unrelated modes. Exact native lifetime/AI/persistence
and rendering evidence remain root qualification work, not results of this audit.

Other exclusions: already completed Obsidian/Cage variants; clone29,791block work
is not a small closure; Lucky Room opens Lucky Block rewards; infinity/forest/
magicbean/Vexsword stay deferred. Shahid has inline behavior at specialdrops:76
but this bounded loot search found no source acquisition token; do not invent a
new author presentation simply to increase batch size.

## Bullet selector clarification (user,22 October2026)

The user approved fixing retarget scope to only the nine newly created bullets.
Keep each new bullet's independent nearest eligible player selection; do not
retarget preexisting nearby angry bullets. This supersedes the source selector
review point above. Root owns the runtime implementation and qualification.

## Item/resource leaves — qualification pending

Prepared LbcCreatureDropItem.Kind OLD_VEX/OLD_SKELETON/BULLETS/BULLETS_OLD,
item()/flag(), register(), matches(stack,kind), matches(stack). Nonempty source
flags plus drop1 match independently; both bullet kinds deliberately share the
same bullets trigger and root must dispatch that payload once. Polymer model
overrides preserve only normal bullet tier_ff/drop_trap/name/lore; all other
variants are plain barriers. Native IDs/ported leaf basenames: old_vex_drop,
old_skeleton_drop, shulker_bullets_drop, shulker_bullets_drop_old. JSON parsing
passed; build/registration/runtime qualification remain root work. No RP/template
or shared integration files were changed by this item/resource task.

## Potion Spawner qualification update

The deferred producer audited above is now migrated and qualified on884de7db.
See [POTION_SPAWNER_PORT.md](POTION_SPAWNER_PORT.md) for118 recorded results in
two actual matches, native lifecycle coverage and limits. Historical pending
statements above describe the audit stage. No full reward pool is opened.
