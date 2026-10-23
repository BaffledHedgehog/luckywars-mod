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

## Fresh remaining-batch audit — 23 October2026, after0d12d40

This section supersedes the historical recommendation above; preserves all prior
text/user clarifications. Research only: no code/build/server/source/RP edits.
S remains the legacy LBC root defined above. Read actual Java dispatcher and
item/service classes, not merely copied resource presence. Normal Vex and its
working sword are now complete. **Acidboom is already ported**:
LbcAntDropItem.Kind.ACID_BURST, LbcAntDrops:47 and special dispatcher handling;
it must not be counted as a new leaf.

Best next bounded grouped batch: FOUR distinct source egg items — low Vex,
Sarvatra creeper, Shahid, aggressive slab. Three reuse complete reward
families; Shahid adds a single native carrier variant to the existing four-carrier
zombietnt family. These acquisition items themselves have no native custom egg
implementation found in current Java (the separate LightningEgg is unrelated).
Do not advertise their whole source pool ready.

### Catalogue and public presentation

Exact S `loot_table/imba/spawnegg.json` contains23 entries, one pool with32 rolls,
no explicit entry weights/qualities/conditions. Every entry uses set_components,
set_name, replace_all lore and custom_data `{imba:1}`. The complete ordered
catalogue (zero based):0tuntija,1bob,2aimbob,3slab,4fat_skeleton,5otchim,
6low_vex,7sarvatra_creeper,8shahid,9shish,10lucky_block,11hohol,12lucky_pig,
13honey_bee,14stalin,15mom,16transformator,17helicopter,18muravei,19monument,
20golden_skeleton,21tiny_skeleton,22death_warden (all names end `_spawn_egg`).
This is a catalogue audit, not approval of all23 closures.

All four selected outer items (and the deferred bee alternative below): count defaults1; actual vanilla spawn egg base
shown below; red/nonitalic translated name; exactly one dark_purple/nonitalic
lore row `imba_version`; flags only `{imba:1}`. No extra custom model is authored
on the outer eggs. Preserve that outer presentation separately from embedded
item/entity payloads. Suggested standalone public leaf basenames (new proposals,
not existing IDs): low_vex_spawn_egg, sarvatra_creeper_spawn_egg,
shahid_spawn_egg, slab_spawn_egg. Honey bee is a deferred fifth alternative.

| Egg/key | Source entry range | Outer base / style | Actual spawned closure |
| --- | --- | --- | --- |
| low_vex_spawn_egg | spawnegg.json:681–724 | vex_spawn_egg / tier_f | ONE native Vex, Health1/max_health1, DeathLootTable lbc:vex_sword; no ring, tenfold spawn, persistence or custom name |
| sarvatra_creeper_spawn_egg | :725–766 | creeper_spawn_egg / tier_f | ONE native creeper, ExplosionRadius1, DeathLootTable lbc:creeper_wand; no invented fuse/powered flag |
| shahid_spawn_egg | :767–817 | zombie_spawn_egg / tier_ff | item entity, PickupDelay32767, one repeating_command_block, flags shahid1/drop1, item_model lbc:drop_trap; then specialdrops:76 |
| honey_bee_spawn_egg | :1088–1129 | bee_spawn_egg / tier_ff | ONE native bee, HasNectar true, DeathLootTable lbc:honey; no source AngerTime/target UUID |
| slab_spawn_egg | :441–515 | zombie_spawn_egg / tier_ff | ONE silent persistent baby zombie, empty death loot, invisible amp1/duration2000000/no particles; guaranteed head slab |

Ranges identify the enclosing entry; use parsed JSON paths as authority if a
boundary falls on neighbouring punctuation. All names/lore were checked in the
read-only documented26.3(1) RP ru_ru.json: imba_version2384, low_vex3255,
sarvatra3256, shahid3257, hohol3260, honey_bee3262. This check does not assert the
26.3(1) instance is active. Recheck current active pack and all shipped languages
before implementation; no RP/template addition is established by this audit.

### Exact reward/runtime closure and non-substitutable differences

Vex: S loot_table/vex_sword.json:1–21 rolls its pool only at10% and invokes
lbc:vexsword. Current normal-Vex port already closes this working sword. Egg
creates ONE, not normal-drop ten nor old ring five. Native no extra lifetime.

Creeper: S loot_table/creeper_wand.json:1–23 has one roll of
lbc:sauvojen_tuntija_air behind5% entry condition. Existing creeper drop service
already supports that reward/chain ownership. Reuse actual native radius1,
AI/fuse/physics/explosion, never the normal drop's20 alternating-radius actors.

Shahid: S function/drops/specialdrops.mcfunction:76 summons ONE baby zombie at
the emitted token XYZ with TNT passenger fuse200 and attack_damage base−10.
No persistence/equipment/name/tags/health override is authored. Native zombie
loot and native TNT are the whole reward closure; no custom table. Compare
ZOMBIE_TNT_DROP_SOURCE.md: four-carrier zombietnt is separate. Owner must pass
from actual egg user through item token to zombie/TNT, native children, terrain
and indirect kills. Embedded source custom_data is a SNBT STRING in old JSON;
normalize it into actual component data for the new codec, preserving fields.
Do not expose a newly decorated standalone Shahid token as an authored source
presentation: its embedded token has only source model and flags.

Bee: S loot_table/honey.json:1–70 is entirely vanilla honey_block/honey_bottle/
honeycomb/honeycomb_block, equal weight1, count1; uniform rolls1..4 and uniform
bonus_rolls0..4 (native luck semantics). Existing LbcBeeDrop covers service/reward
family. The egg has no authored anger; do not replace it with the drop's hostile
swarm. Native bee behavior, nectar use, hive entry and death semantics remain.

Slab: exact egg head uses `item_name` (not normal-drop custom_name), translated
agressive_slab/nonitalic, attack_damage modifier mainhand7/add_value with ID
minecraft:base_attack_damage. No tier/model is authored on the head slab. Egg
actor is Silent true; normal specialdrops:54 differs (modifier6, head tooltip,
custom_name, no Silent). Existing slab registration cannot be reused by silently
flattening those differences. Native source guaranteed head drop closes reward;
mainhand-only modifier does not grant worn-head attack7.

### Integration boundaries to qualify

Raw native entity_data spawning does NOT automatically adopt existing services.
LbcCreatureDrops.admit is private; loaded() ignores actors without its managed
ACTOR tag; Vex reward/credit require family-specific adoption plus normal-Vex tag.
LbcCreeperDrops.loaded likewise depends on its managed tag and cause map; existing
adoptUltraponos sets an unwanted ULTRAPONOS/WILD marker and is not a generic egg
adoption API. Use narrowly scoped admission retaining source NBT. Check LbcBeeDrop
and slab service ownership/admission similarly. Never identify arbitrary vanilla
mobs by broad entity type, displayed name or user-controlled loose data.

Native spawn-egg interactions include block-face placement, fluid use, baby
creation when used on matching mobs, dispensers and spawner editing. Determine
which source paths worked on OLD and what the selected native26.3 SpawnEggItem
preserves before asserting support. No source-based reason to invent ally-target
or lifetime policy. Existing role/epoch/time-stop/resident-chunk/quiet-stop and
causal-credit contracts still apply. Any constructor must capture actor/egg user
before load callback and preserve admission on reload without rewarding quiet
cleanup. For spawner editing, native cloned source NBT must not escape ownership
or create untracked actors. These are implementation/qualification boundaries,
not an automatic demand for author permission.

### Explicit exclusions

hohol_spawn_egg (:991–1040) embeds droppertnt, NOT droppertnt_old. Current
LbcDroneDropItem only recognizes the OLD flag. Source specialdrops:32 normal
hoglin has DeathLootTable lbc:hohlopitek; old:34 does not. S
loot_table/hohlopitek.json:1–63 emits a custom carrot_on_a_stick weapon, tier_b,
model lbc:hohlopitek, light_purple name/lore, flags transformable/raycastspell/
hohlopitek/spellwithcooldown. No Java hohlopitek support found. Do not include
this egg merely because old drone lifecycle already exists.

Fat Skeleton egg is another possible bounded follow-up, but differs materially
from normal completed actor: no authored attack_damage7 base/mainhand attribute
array, no feet/legs rewards, vanilla-curse equipment; use exact :516–630 NBT.
TMT is a large random-item mutation subsystem. Greedcurse reaches permanent
curse/prestige, random diamond-placement retry and damage logic; Otchim reaches
Mom/absurd_child. Dungeon/monument/tiny/golden/death actor reward closures were
not fully inspected here and are not claimed safe. Infinity/forest/Mom remain
excluded as requested.

### Root scope selection and schema constraints

Root selected exactly entries3/6/7/8: slab/low_vex/sarvatra_creeper/shahid;
bee above remains an inspected alternative, excluded from this patch. Root owns
item/service/adoption and JSON resources. This audit writes no resource leaves.
Proposed exact registered IDs: skywars2:slab_spawn_egg,
skywars2:low_vex_spawn_egg, skywars2:sarvatra_creeper_spawn_egg,
skywars2:shahid_spawn_egg. Keep source outer flags `{imba:1}` only.

Old JSON entity_data contains native source NBT maps; current26.3 component
ENTITY_DATA is typed and needs the actual entity type (not the egg visual type),
e.g. Shahid ENTITY_DATA item while outer visual zombie_spawn_egg. Current native
NBT equipment/effects/attributes codec must be used rather than assuming copied
old serialized JSON applies. Normal Vex already uses native max_health and custom
loot table NBT successfully; source map can be converted through existing native
TagValueInput conventions. Explicit source Health1 and max_health1 both retained.
Shahid inner Item components custom_data SNBT string must be an actual compound;
inner count1/id/model and PickupDelay32767 retained. Preserve slab component
`item_name` and source slot semantics; changing it to custom_name would alter
native anvil/name presentation. Existing namespaced enchantment/attribute IDs and
native missing fields/defaults remain; no inventing runtime data to fill blanks.
Slab's minecraft:empty death table must retain the guaranteed equipment head drop.

No author ambiguity blocks these four source closures. Main uncertainty is
native egg lifecycle coverage (especially spawner/dispenser/baby-use paths),
not unknown source rewards. Verify real native-use placement, debit, actor
count/NBT, accepted melee and TNT harm ownership, exact Vex10%/creeper5% death
reward chance, guaranteed working slab reward, stop/unload/time-stop/role guards.
No rendering proof or runtime qualification is claimed by this source audit.
# Implementation follow-up - 23 October 2026

The four selected source eggs3/6/7/8 are implemented in
[SPECIAL_EGGS_PORT.md](SPECIAL_EGGS_PORT.md). The audit below remains historical
source evidence; its unselected alternatives are not implicitly qualified.
