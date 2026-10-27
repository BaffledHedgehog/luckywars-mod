# Aimbob and Death Compass complete-family boundaries - 2026-10-27

Read-only static source/native audit. Only this new note written; no build/tests/server/git/code/RP or previous-note edits. Reviewed current root ARCHITECTURE.md signed-effect/item caps and agent_notes/AGENTS.md attribution/source-evidence rules. Source paths below relative to legacy data/lbc; native paths relative to luckywars-mod. Historical source text is not observed runtime. Native original-owner attribution, roles/default allied-player protection, signed item/effect caps, Time Stop and passive-under-stun policy remain authoritative.

## Strong whole candidate

**Death Compass is the stronger complete finite next family** once its exact original current recovery-compass leaf has a real scoped normal-play acquisition route. The Warden's only scripted death reward is already packaged Death Scythe with a real existing native engine. Aimbob is **not five loot pieces**: the actual audited table is20 equal-weight gear entries with3 rolls, plus a separate End/Baffled snowball chance. Enabling only helmet or five selected entries would silently shrink the original family. Its currently missing output engines make it a larger future group.

Neither raw source JSON nor raw src/main/resources file is proof of JAR inclusion/live gameplay. No native Aimbob/Compass producer was found after searching flags, names, loot IDs and current services. This audit does not qualify or implement either family.

## Death Compass - complete source admission

`items/changed_inventory/sel:15-16,29` tracks death_compass_active if container.* OR weapon.* contains custom_data death_compass1; removes only when neither. Armor/craft/cursor alone do not activate. `players_1s:39`, shared20tick schedule, nonspectator players outside nexus/imprinted: unless marker stopper_magic within spherical20, run death_compass_check at owner. One invocation per phase, not two independent item probes. No mana, RMB, durability, chance, private cooldown, skill/config/legacy gate, nomagic_active or stopper gate.

`other/death_compass_check:1-5` stores `data get Pos[0]`/Pos[2] into shared player_x/player_z integer results; reads `LastDeathLocation{}.pos[0]`/pos[2] into death_x/death_z. If X and Z equal, call attack. Y and LastDeathLocation dimension are NEVER compared. Preserve source integer command conversion, not floating distance or a3D radius. Native26.3 ServerPlayer:938 records current death GlobalPos, :1671 copies it after respawn; use real player last-death data instead of a new independent saved location. No previous death/missing NBT handling is explicitly coded in source: do not silently invent a current-location fallback. Native command-result/error behavior must be verified before claiming origin/no-death parity; raw source fields use legacy schema and cannot simply be copied as26.3 NBT paths.

No hidden compass refresh task or extra death_mob scripted AI was found by whole flag/tag search. Native recovery compass retains vanilla presentation/pointer semantics; the source scripted trigger ignoring dimension remains distinct from vanilla compass pointer dimension behavior.

## Death Compass - exact six-line attack order

| Source line | Exact behavior |
|---|---|
|1|`clear @s *[custom_data~{death_compass:1}] 1`: full clear scope, not necessarily trigger stack. Later commands are independent of clear success.|
|2|Summon Warden at captured sourceXYZ+(0,15,0), Health150f, tag death_mob, DeathLootTable lbc:death_loot; Brain dig_cooldown memory value{} ttl32767L. No custom name, max-health attribute, NoAI, persistence flag, team or owner specified.|
|3|Self Darkness30seconds=600ticks amp0 visible. Native item caps allow this exact level.|
|4|minecraft:entity.warden.dig MASTER @a at original captured source point, volume1 pitch1/minVolume0 (ranged native command audience, not all-client broadcast).|
|5|From captured point+Y15, select nearest tagged death_mob Warden limit1, then at Warden: Nether predicate -> spreadplayers ~ ~ 0 10 under120 false @s.|
|6|Same independent selection/positioning, !Nether predicate -> spreadplayers ~ ~ 0 10 false @s.|

The nearest tagged selector can select a pre-existing Warden, especially if summon fails; do not call literal source a UUID-bound newly spawned actor. Earlier spawn/darkness/clear do not roll back if spread fails. No fixed expiry/kill timer; ordinary Warden brain/AI/despawn/death and source dig-cooldown memory apply. Health150 does not imply max-health150; native Warden base maxHealth remains500 unless normal loaded data changes it.

Source attribution is absent. Native must associate the real original Compass trigger owner with the new hostile actor and later native damage/terrain/death consequences through established ownership policy; do not attribute to a random nearby player. Shared default allied-player/roles/Greed rules remain, no WILD authorization. This is an owned hostile consequence, not permission to replace Warden with a visual marker or empty loot placeholder. Stopping match/owner epoch follows shared actor lifecycle; source itself has no explicit private cleanup function.

## Exact native SpreadPlayers semantics to preserve

Cached26.3 `SpreadPlayersCommand:102-128`: random X/Z inside square center+/-10, one target, spreadDistance0, respectTeamsfalse. :35/:226-230 bound search10000 iterations, fail with command exception. Default maxHeight is level.getMaxY()+1 (:66), explicit Nether branch is120. `Position.getSpawnY:338-354` scans downward from above maxHeight to world minY, finding nonair floor with TWO air cells above; :357-360 rejects liquid/unsafe floor via ENTITIES_CAN_TELEPORT_TO tag and floorY<maxHeight. `setPlayerPositions:259-260` uses floor(X/Z)+.5 and derivedY, Set.of() teleport. This is not a uniform random Y, vanilla randomTeleport, radius-circle sample or highest-column approximation.

Stock helper reads level block state without a resident-chunk admission guard. Native current resident terrain policy must avoid chunk loads; preserve the full native terrain algorithm over admitted resident columns rather than claiming approximate random teleport is source equivalent. Parent/test qualification must cover Nether under120, no valid floor, floor tag/liquid/two-air rules and ordered failure. Spread source is centered at the selected Warden's location, not necessarily original player'sX/Z after earlier actor movement. This note does not authorize narrowing source range or replacing failed spread with an invented safe spot.

## Original Compass leaf, normal acquisition and reward engine

`loot_table/lucky_items.json:1143-1186`, current pool0 entry73: recovery_compass, weight10 quality7; default count1, tooltip lbc:tier_q; name death_compass aqua italicfalse; two lore keys death_compass.lore.1/.2 white italicfalse; data death_compass1 only. No model override, transformable/accessory/gadget flags, recipe, old variant or new task found. Source luckyblock/luckyblock_without_rec/luckyblock_64 reference lucky_items, but root native build does not include raw lucky_items merely because a raw copy exists. A scoped exact current leaf needs integration into the existing real normal-play producer, preserving its original weight/quality and current routing rather than whole-pool activation.

`loot_table/death_loot.json` has one pool/roll1/entryweight10 -> original carrot_on_a_stick Death Scythe: lbc:death_skythe tier_s; mainhand base_attack_damage+8 and attack_speed-2.7 add_value; green italicfalse name; aqua type+hoe row, white lore1/lore2, white RCability row, white lore3; data transformable1/hoe1/death_skythe1/raycastspell1/swordwithability1. No additional death rewards or random-table dependency. build.gradle:261 explicitly packages death_loot. Native LbcSpecialTools:41,52-54 registers replacement SCYTHE and :71 accepts original vanilla flag, :82-91 actual credited-kill counter (every2 ->charge, ink3), :94+ accepted melee lunge/terrain, :128-155 charged RMB/sonic damage/terrain and task, :275+ charging HUD. Existing native source drop is therefore genuine gameplay, not inert raw loot. New Warden must die with original lbc:death_loot and permit ordinary pickup of exact flag/replacement mapping through existing loot bridge.

Source `workingitems/death_skythe_kill:1-8` increments swordcharging3, charges at2 then resetsprogress, ink3 and revoke internal criterion; source UI charging:3 displays swordcharged3/progress2. Preserve existing native engine rather than re-porting or introducing a separate charge counter. Its native qualification is separate existing work, not a new achievement claimed here.

## Full Aimbob producer and actual table

`drops/specialdrops:53` triggers when dropped source item has aimbob1 (outer ordinary drop dispatcher/consumption applies). Summons skeleton at source point, tag aimbob, DeathLootTable lbc:aimbob, PersistenceRequired1; Instant Damage20ticks amp100 (heals undead through vanilla inversion), aqua translated custom name. No explicit Health/maxHealth/AI/speed base attributes. Mainhand enchanted bow includes movement_speed+1 total whilemainhand, offhand six named apples; four named iron armor slots. ALLsix source gear drop_chances0, so reward items come from DeathLootTable, not equipped gear. The mob's own helmet has no night_vision flag and different enchant levels; never treat any named Aimbob helmet as passive-enabled.

Source drop acquisition routes include exact original drop leaf in ordinary barrier/drop pools and event `other/baffledend/5` (25 randomly positioned drop items). No native producer or explicit build inclusion for aimbob table found. Implementing only a skeleton with vanilla drops would not restore source family.

`loot_table/aimbob.json` pool0 rolls1 references lbc:end_of_everything. Referenced table has pool random_chance.3 and weight20 End snowball /weight10 Baffled snowball. So30% chance any event item, then2:1 choice, not guaranteed apocalypse. Those gadget outputs have large native-missing event closures; suppressing the pool changes original behavior.

Pool1 rolls3 WITH replacement among20 equalweight1 leaves. All named italicfalse, original vanilla carriers; most count1/unbreakable/tier as table. Exact component details are bound by SHA, summarized here without duplicating huge JSON:

| Entries0..19 | Relevant full-family dependencies |
|---|---|
|0 ironaxe,1 ironhoe,5 ironshovel,6 ironpickaxe|Extreme efficiency/fortune/enchant levels; native item caps/signed mechanics authoritative. No custom source helper flags.|
|2 ironboots|Overenchanted boots; feet knockback and explosion resistance+1000 namedadd_value.|
|3 ironsword|Overenchanted; mainhand attack_speed+1000 and damage+5 namedbase modifiers.|
|4 ironhelmet|Unbreakable tier_b, Protection/Fire/Blast/Projectile4, Respiration3, AquaAffinity1, Thorns3; night_vision1 passive.|
|7 ironlegs,8 ironchest|Leg movement_speed+1 add_multiplied_base; chest maxHP+20add_value, original defenseenchants.|
|9 bow,10 crossbow|Original cross-weapon enchantment combinations (multishot5 bow etc), not just visual names. Existing projectile enchantment architecture must be retained.|
|11 trident|Riptide15/other sourceenchants +slow_falling1 metadata. Whole function search found no slow_falling custom-data flag consumer in this source; do not invent a trident passive merely from this inert tag.|
|12 shield|Bothhand knockback resistance+1 base-multiplied; armor+toughness+10 bothhands, knockback10 enchant.|
|13 flint,14 fishingrod,15 shears|Extreme fire/efficiency/luck and source named attack/attribute modifiers; original vanilla usable behavior.|
|16 goldenapple count2|aimbobapple1, lbc:aimbobapple tier_a -> consume advancement and buff helper (below).|
|17 ironspear,18 elytra,19 mace|Current source includes spear/lunge3/handgravity-.7total/safeFall+10total; elytra armor+5; mace wind_burst15/safeFall+100total. These source entries prove table is not an assumed old five-piece collection.|

Aimbob Helmet full passive: `items/changed_inventory/sel:11,31`, head night_vision1 ->tag aimbob_helmet; `players_1s:29` every20ticks ->helper full4lines NightVision300t amp0 visible, clears Darkness/Blindness/Nausea. No own marker/mana/skill gate. Remove helmet stopsrefresh, does not cancel existing NightVision. Native generic metadata comment LbcDamage:201 is not a handler. No native actual Aimbob head producer found.

Aimbob Apple full input: `advancement/aimbobapple.json`, consume_item custom_data aimbobapple1, outside nexus/imprinted -> other/aimbob_apple: HealthBoost6000t amp9, Regeneration60t sourceamp127 (native item cap5), Resistance100t amp4, Saturation1 instantaneoustick sourceamp127; revoke criterion. Native flag/consume/helper handler not found. This buff cannot be silently dropped from full Aimbob loot.

No source Aimbob skill selection/reward task, global private timer or lifetime beyond skeleton/native item effects. Native creator provenance for the summoned hostile and later dropped eventgadgets must survive new source-normal producer; do not attribute its lethal effects to whoever later stands nearest. Existing default policies settle allied-player/roles; raw source lacks attribution due datapack limitations, not desired author behavior.

## Native gaps and concrete decisions before implementation

DeathCompass: missing complete input/last-death admission, original scoped normal acquisition, owned Warden construction/brain/deathloot and exact resident SpreadPlayers routing. Scythe engine/packaged death_loot already exist. Keep command failure order; note missing-last-death and pre-existing nearest Warden selectors rather than silently improving them. These are exact mechanics requiring native source controls; no blanket author approval to rewrite their edge cases is inferred.

Aimbob: missing original producer,20-leaf table packaging, helmet/Apple gameplay and End/Baffled gadget full engines. It is a coherent future large family, not the finite next item. Do not ship five selected loot pieces or no-event pool as complete Aimbob. This note's strongest candidate is complete Compass with real current leaf delivery, not a direct-give-only demonstration.

## SHA-256 ledger

```text
function/drops/specialdrops.mcfunction 8077ebcc7123798445c57c4a5d4b8f72982aceaacedb26ec34e3d80c3c0abf41
function/other/death_compass_check.mcfunction 5eb2abb77ba36d34f4bbd8e1b26eeeb073dabf2a7166649a688f89ffb5384e5b
function/other/death_compass_attack.mcfunction bfa4baf68fd8eb853d0e1455633f0762e7154516e4c0efeaa1431d77c55f3dfc
function/players_1s.mcfunction 4e354224511c16f697d04089b43875b28547a24bec54c0e6190b56b4dcdff905
function/items/changed_inventory/sel.mcfunction 0b9c6a85afe888ab288ccb32822c7aece0cb4903c2ebbee37bf24e48ea99e7d5
function/workingitems/aimbob_helmet.mcfunction 5b952f819612b9423ff3f91542883b2ead5696375d540d298ef03ca1fbe7f3d2
function/other/aimbob_apple.mcfunction 10a11c1778aeb7ef0f3f1c5129d39c01fba37706ec6b6a85e98c32b52156b806
function/workingitems/death_skythe_kill.mcfunction d279a3336b101c5fe6797fef240c75e56c26c2e671874656cc5a9d72d51a6fc2
advancement/aimbobapple.json 1fd55100d4c28098a1954d645aa582b1d9c0be649caa9a39903a9d5ced31658b
loot_table/aimbob.json 7cbe6de0d2d32fcb9f5c833924c9e5d289856d4e50656c13603af4d156f3d24a
loot_table/end_of_everything.json 98df0489299489a4dad3b400262eaaa0daaedda91b8b6f0d558a052c01636792
loot_table/death_loot.json ee9c944bb80a920556aac91db3e3a4e87b06294c4e7be9d568d5765343d05e22
loot_table/lucky_items.json a9b73cc64f482231beb27f0ae07328180bdcb358fa6cb9cb0867369d792bbcb2
native/src/main/java/me/skywars2/game/LbcSpecialTools.java c089b8a5ba0f2581b53e5fb0e4aa5cb4b5c51715c9baa083e34c3f2d96eb5721
native/src/main/resources/data/lbc/loot_table/aimbob.json 7cbe6de0d2d32fcb9f5c833924c9e5d289856d4e50656c13603af4d156f3d24a
native/src/main/resources/data/lbc/loot_table/death_loot.json a60529e67c3f30891201e6338156c097ea5f49788596e474158c8530a6f1fcaf
native/build.gradle 7f1407704c2b7d3b7dc6e438ad4b7f2e6d6b5c4d53009f707c550065662287c9
Native26.3/net/minecraft/server/commands/SpreadPlayersCommand.java 2db5b24f8c775d06edd68158c2830dc709cd95e12be210e29fdfcbe69adab350
Native26.3/net/minecraft/server/level/ServerPlayer.java f5677c657385af1516ee3c18ff589ce33bb950ef70b08948892d13e4ae4ca128
RP/assets/minecraft/lang/en_us.json 7971188ae0d2e5f16f7253c6495168650451599e2826eeb46f7cfcb4a0e507f7
RP/assets/minecraft/lang/ru_ru.json 9ba24ccecc189950fe970f525ff45b6e40df5b375fabd333cfa6786ca8dac2fc
RP/assets/lbc/items/death_skythe.json 0be6f559205b5906ed2be8ea2041ef8b1e303b23a13352a9380a89aa862600d4
RP/assets/lbc/models/item/death_skythe.json 98a89c5cf41d1c7e4a7e9b908bdd822cd53ce454177b6180664c8e41eb327fec
```

Frozen static preparation. Original RP presentation reused; physical client/audio, native gameplay and normal acquisition remain unqualified by this note.
