# Inventory hazards and Touhou consumables - 2026-10-27

Static source/native preparation only; no code, old note, build, test, server, git or RP changes. Legacy paths below are relative to `world/datapacksdisabled/LBC/data/lbc`; native paths relative to luckywars-mod. Historical source text is not observed runtime. Existing native roles, default allied-player protection, attribution, signed effect/item caps, Time Stop and passive-under-stun policy remain authoritative.

## Recommended complete batch

**Liquid Abyss plus all six carried Penis Bomb variants** is a coherent inventory-hazards batch independent of Belt/Quiver questions19-22. Parent owns Liquid contract. All six bomb consequence bodies already exist in native LbcKfcRewards for tracked CultItem contact; ordinary vanilla-carrier inventory admission/removal and a demonstrated normal-pickup acquisition route are missing. Preserve existing Cultus/boss-contact behavior while adding the complete independent inventory route. This corrects the earlier broad statement that no native bomb engine existed: there is a native engine, but its tracked-contact entrypoint is different.

**Defer whole Touhou family10**. Its complete closure includes real participant lives, permanent power attributes, persistent coins, Time Stop and Bomb snowball outputs, skill28 acquisition, kill/drop attribution and reset. Existing mana/coin fields, Stand clocks or Danmaku grimoire bomb do not prove this whole family works. Do not activate only the easy mana/power/coin leaves or drop inert rewards.

## Shared source inventory event

`tick:1 -> tick_not_timestopped:4 -> mobs_tick:4 -> players_tick:15 -> other/instants/select`. Ordinary nonspectator, non-time_stopped players outside nexus/imprinted execute once each active game tick. `items/changed_inventory/sel:38-40,54` tracks instantitem_active if container.*, weapon.* or armor.* contains instantitem1. This outer activation is broader than the following NBT Inventory matching. No RMB, mana cost, private cooldown or generic stopper/nomagic check wraps these two families.

`other/instants/select:1-3`: enabled #lbcskill1 +lbc.skill2 selects select2, which contains **neither bombs nor Touhou**. All other profiles/config-disabled use select_old. No legacy exemption appears in this switch. Source skill2 suppresses even held bombs/Touhou; do not reinterpret it as hands-only for these families. Existing native skill2 catalog remains deferred. Native common Time Stop pauses; equipped/inventory passives must follow established author stun policy, not generic active-use rejection.

`select_old:3` requires Inventory penis_bomb1 then calls other/penises/player_select. :9 requires data Inventory[].components.custom_data.touhou_item then Touhou select. Inventory NBT covers carried storage/equipment, **not personal crafting or cursor**. Once triggered, `/clear` below may remove/count from full command scope (inventory/equipment then personal crafting then cursor). Trigger and payment scopes must not be silently merged.

## Six complete carried bomb branches

`other/penises/player_select:1-6` checks each distinct penis_num presence in1..6 order. It does not itself require matching penis_bomb/instantitem on that same stack; the outer gate only establishes some bomb. Each present variant attempts one helper per tick regardless of total copies. Each helper, if executor is player, first emits global @a tellraw, then `/clear @s *[custom_data~{penis_num:N}] 1`. Clear failure does not condition later consequences. No source random draw here; randomness belongs to acquisition. Do not aggregate all six into one draw or consume whole stack.

| Variant/helper | Exact ordered payload after tellraw/clear | Sound / particle |
|---|---|---|
|1 normal|self6 minecraft:arrow damage|minecraft:lbcsounds.suction master @a volume1 pitch1; cloud y+1 spread2/2/2 speed.1 count10 normal|
|2 dirty|Slowness160t amp5 hidden; Weakness80t amp5 hidden; Nausea120t amp5 hidden; self4 arrow|minecraft:lbcsounds.dick_1; dirt block y+1 spread.2/.2/.2 speed1 count100 normal|
|3 legacy helper named nigger|summon TNT fuse2 at owner|minecraft:lbcsounds.spank; no explicit particle|
|4 holy|Blindness160t amp1 hidden; Levitation20t amp15 hidden; summon lightning at owner|minecraft:lbcsounds.woo; flash color[1,1,.8,1] y+1 spread2 count100 normal|
|5 horse|Slowness20t amp127 hidden; self10 arrow; positioned local^ ^ ^-5 then `/tp @s ~ ~5 ~`|minecraft:lbcsounds.shityeah; redstone_block y+1 spread2 count100 normal at the original captured command-source position (not the teleported entity position)|
|6 multicast|`execute at @e[tag=!spectator] run damage @s .1 arrow`: executor stays owner, one self-damage attempt per nonspectator entity in current command level; no target change|minecraft:lbcsounds.weewee; white_wool y+1 spread2 speed1 count100 normal|

All sounds master @a volume1/pitch1/minVolume0: native command range/dimension audience, not unconditional all-server packet broadcast. Tellraw is global @a across dimensions. Source love_*_penis text colors normal#E921FF, dirty#5C2A0E,3dark_gray,4white,5light_purple,6gray. Text starts executor selector, not creator. Source damage has no attacker; source TNT/lightning unowned. Native must retain real known original author where available, with default ally protection; raw carried item flags do not establish a shooter UUID. No new WILD permission follows from these selectors.

Source variant6 is finite per invocation but scales with entities. Repeated same-tick native hurt acceptance is not guaranteed additive damage: preserve vanilla damage/cooldown behavior. Existing native payload instead queues bounded one-attempt-per-workstep multicast; this is an established implementation service, not evidence that source ran over multiple ticks. Do not create an uncapped main-thread damage loop or silently multiply .1 by entity count into one hit.

### Existing native engine and exact reuse boundary

`LbcKfcRewards.java:148-185` creates CultItem/native vanilla-looking stacks with flags instantitem1, penis_bomb1, penis_num; variant2 also govnoq1. Native owner is stored in owned/bombs maps tied to entity and epoch, not stack custom data. :233-239 native playerTouch requires pickup room, then activate consumes tracked entity without normal inventory insertion. :346-356 also detects NPC contact within1.2. :253-259 calls private effect after payload owner/target gate; :261-320 implements all six payloads with owner effects, native protected blast/lightning, ranged sound and particles. :324-340 owns bounded multicast work; :342-344 uses custom CULTUS_ARROW damage. :379-385 stops consequences/items/jobs.

`payloadTarget:196-199` uses projectileTarget for player owner, or victim!=hostileOwner plus roles/ally check. Thus **do not reuse activate unchanged for an inventory self-target**. Add an explicit inventory entrypoint with source admission/clear and self-target semantics; factor common presentation/payload primitives while keeping contact admission intact. Metadata/provenance should preserve actual known creator through any native pickup/drop lifecycle; never guess that a same-model item is owned by a previous CultItem, or automatically attribute plain item self-damage to holder as an attack.

Existing native blast service currently acts immediately radius4 (:281-282), while literal source summons fuse2 TNT. Existing native lightning tracks owner/terrain and drops shock-bolt tag (:284-299). Horse native code currently uses lookAngle*-5 +up5 (:301-309); source local-coordinate pitch/yaw semantics and teleport known-client-movement behavior must be preserved by the new inventory path. A separate input route is not permission to change qualified Cultus/boss behavior.

### Captured command-source position and rotation (verified native command mechanics)

Source `select_old:3` uses `execute at @s[...] run function player_select`; this captures owner location AND rotation. Each player_select branch invokes a helper without a new at/positioned context. Native26.3 `CommandSourceStack.withPosition:190-207` and withRotation:210+ construct a new stack; `ExecuteCommand:162-163` at derives position/rotation from entity once, and :173 positioned derives a child stack. `CallFunction.execute:24-35` binds every function entry to the same sender. Entity teleport does not mutate that sender's Vec3 or rotation.

Consequently horse's line `execute positioned ^ ^ ^-5 run tp @s ~ ~5 ~` changes only that command's child source. Later horse sound/particle lines execute at the ORIGINAL captured point; particle y+1 is originalY+1, sound originalXYZ. Following variant6 helper (if present) inherits the same original source point/rotation too. `@s` still selects the now-teleported entity; selectors centered on source coordinates and entity identity are distinct. Do not recalculate sender position from owner between helper lines or variants. Native existing KfcRewards.effect:307-309 resets `at=victim.position()` for tracked contact; new inventory parity must explicitly retain captured presentation point instead. This is a static command-mechanics proof, not historical1.21.4 runtime observation.

### Source ground contact and acquisition

Whole flag search finds ground behavior too: items_tick:1-2 tags item then working dispatcher; items_tick_tag:12,26 marks penis_bomb/working; items_tick_act:7 -> workingitems/penis_mob when nonplayer #minecraft:mobs within1.2 !no_penis. penis_mob chooses nearest, repositions execution; other/penises/select:1-6 changes executor to nonplayer at<=.001 and runs same helper, then penis_mob removes one source stack unit/whole count1 entity. This is separate from ordinary player inventory, already represented by tracked native contact (do not claim arbitrary vanilla dropped items are automatically tracked).

Original source producers include workingitems/penis_cultus/spawn_random_penis (uniform1..6; bat riders), items/trinket_effects/charge_penis and penis_gun, workingitems/penispenispenis_random_penis_big_penis_small_penispenis (free drops). Common repeating_command_block carrier, original model lbc:penis_1..6, item_name penis; flags above; some free source variants explicitly max_stack_size1/tier_ff, others omit them. Preserve each original producer presentation rather than standardizing all leaves. No _old flag family found. No dedicated six-leaf loot-table entries found; this is producer-defined item acquisition, not license to add a new catalog/pool. Native Cultus/boss contact acquisition is already concrete, but it does NOT demonstrate carried-stack acquisition: see the verified pickup boundary below. Unrelated fish/crossbow/trinket ports remain outside this batch. Magnet task detects nearby dropped penis_bomb as already qualified; inventory passives grant no new task themselves.

### Actual carried acquisition boundary (verified, no invented producer)

Legacy producers summon ordinary minecraft:item stacks. Vanilla native `ItemEntity.playerTouch:333-346` checks delay/target then `player.getInventory().add(itemStack)`, performs pickup animation/stat/callback and discards empty entity: this supplies source carried items for the inventory helper.

Native `LbcKfcRewards.spawnBomb:148-167` and spawnLooseBomb:170-185 instead instantiate the private CultItem subtype. `CultItem.playerTouch:233-239` never calls super: after room check it calls activate; activate:253-259 removes maps/discards entity then applies payload. Even rejected/self/ally targets have NO fallback normal pickup. Magnet teleport keeps the entity subtype and does not change this. Owner/provenance is in entity maps rather than copied item stack flags. No existing stack-give/carry API from these producers was found. Therefore the same currently qualified producer cannot create a carried stack by ordinary native pickup. A raw manually inserted stack is a useful dispatcher fixture but is not production acquisition evidence.

Complete carried-bomb restoration requires an explicit normal-pickup source route with correct presentation/known provenance, or a verified already live independent producer which yields ordinary ItemEntity. Do not silently change existing qualified Cultus/boss exclusive-contact behavior to manufacture that route. This is an implementation/acquisition gap, not a new author choice about source intent. All six payload contracts remain viable; batch completeness must include this concrete route before claiming ordinary acquisition restored.

## Touhou complete10 consumer and weighted acquisition table

`swrg_kit_integration/items/touhou_items/select:4-47` runs ALL ten types in fixed1..10 order, consuming every matching stack unit for each type with unbounded `/clear` max omitted. There is no per-copy active input, chance, mana debit or cooldown. Every admitted invocation executes update_power even if no1..3 consumed; old score thresholds can therefore refresh attempted modifiers. Score arithmetic is source integer scoreboard arithmetic; native must not fake currencies as unused scores.

| Type / exact model+name key | Table weight | Consumer |
|---|---:|---|
|1 power_item|800|power +=count unless already exactly150|
|2 big_power_item|100|power +=10count unless already exactly150|
|3 full_power_item|3|if count>0 and power!=150 set150; after1..3 clamp151+ to150; title full_power_mode gold bold only if newly reaches150|
|4 score_item|1000|consume count; tmp100count; only if mana<manamax addmana helper -> mana+=tmp then clampmax; owner honey_bottle.drink player volume.5 pitch2|
|5 life_item|1|swrg.lives +=count; if count>0 owner tellraw you_gain color#ffbfff +count+lives|
|6 time_orb|200|time_orbs +=count; if count>0 and total>=25 remove25 and loot GIVE one lbc:time_stopper; no while-loop|
|7 bomb_item_fragment|100|bomb_fragments +=count; if total>=5 remove5 and GIVE one lbc:bomb_item; condition runs even count0|
|8 life_item_fragment|10|life_fragments +=count; if total>=10 remove10 and lives+1; condition runs even count0|
|9 faith_item|10|manaregen +=count, no cap/no sound here|
|10 money_item|1000|swrg.coins +=2count; if positive owner lbcsounds.coin PLAYER1/pitch1 and +count coins_added gold tellraw|

All ten leaves count1 repeating_command_block, custom_data instantitem1/touhou_item:type, translated white italicfalse name, model lbc:key. No lore rows are present. Only type1 explicitly tier_d; do not invent styles for the others. Type6 table has extra set_count1 function after data (same result). **Table also has an11th entry** referencing lbc:bomb_item weight10 between5/6: it is a direct usable output, not an11th Touhou consumer. Total raw weights3234; preserve pool conditions/rolls exactly from JSON, no reweighting.

`update_power:1-10`: named modifiers touhou_power1..10 added at thresholds10/25/40/55/70/85/100/115/130/150. Damage add_value at10:+1,40:+1.5,70:+1,100:+1.5,130:+1,150:+2 (sum8 at150). MaxHP add_multiplied_base+.2 at25/55/85/115 (sum+.8). No removal at lower power in this helper. Native modifiers must be idempotent by original IDs and removed by real owner reset/death lifecycle, preserve current health behavior, native signed/cap policy. Source `leave_death` resets power/counters/modifiers; do not persist match buffs as profile equipment bonuses.

### Acquisition is skill28, not an ordinary lucky loot pool

`swrg_kit_integration/game/skills/player_killed_entity:5-6` skill28 adds danmaku_score_spawn tag and schedules danmaku_cards_spawn1tick. That helper runs for tagged nonspec players at owner then removes tag globally. score_items_spawn:1-5 selects **all** current-level item and XP entities Age0 and Age1 (no distance/owner filter). For each, score_items_spawn_1:2-5 spawns four independent lbc:touhou_item rolls, endrod4 spread.1, amethyst chime BLOCK @a volume.5 pitch2. Do not invent a per-killed-mob drop count or assume attribution/proximity already exists. Multiple tagged players and fresh drops can multiply the scan; literal source is not proof of historical runtime expansion. Skill28/native whole kill acquisition needs its own exact implementation boundary. No old Touhou flag variants found.

### Output item dependencies are gameplay, not token grants

- time_stopper: original snowball count1, lbc:time_stopper tier_a, aqua nonitalic name, yellow time_stopper.lore; flags time_stopper1/gadget1. Native build.gradle:224 packages leaf; native LbcCraftingRecipe:322 recognizes donor for Stand clocks. No native time_stopper throw/active gameplay handler was found. Stand clocks are a different item/family.
- bomb_item: snowball count1 max_stack_size99, no max_damage/damage, model lbc:bomb_item tier_b, white nonitalic name, gadget1/bomb_item1. Native similarly named LbcDanmaku useGrimoire:92-111 creates its own charged spell bomb; it does not accept this flag or item input. Bomb output cannot be called restored based on that service.
- Source projectiles/eggs:37-38 identifies nearest snowball Item flags then calls Touhou time_stop/bomb; this is a suspicious source nearest-projectile association. Source time_stop applies global freeze tags except stand/NoAI/clock immunity, time_stopped11, original zawardo sound/gamerules and clears scheduled periodic functions. Native shared TimeStop is authoritative; no independent gamerule rewrites or scheduler clearing should be reintroduced.
- Source bomb spawns paired mirrored yellow glass displays, tags slowraycast/player/danmaku_bomb_fast/magic, assigns owner lbcID2/team to act display and Distance300, plays lbcsounds.bomb MASTER @a2/pitch1. Full bomb expansion/collision/service closure is required before its input can be enabled. The paired display is a gameplay owner, not mere cosmetic item.

Fast bomb closure: `raycastselector:1` first adds Lifetime1, then `raycastselector_player:38 -> bomb_tick_fast`. That helper adds another10 while Lifetime<=Distance300, initializes mirrored visual scales+/-30 at Lifetime<=11, computes integer radius by int(Lifetime*1.2) then int(*.05), and calls bomb_tick_2_fast only for act. After age exceeds300, empty increments each tick; discard at empty60. `bomb_tick_2_fast` temporarily marks same-owner lbcID2 nodmg and same-team nodmg1 by location-based selectors; kills enemy projectiles/magic or mecha raycasters/mob_projectile mobs inside radius, excluding strict_map_object and own bomb displays. Then enemy #mobs take2 magic by @a[tag=nodmg,limit1] per active tick. Only player tags are cleared afterward (literal broad source tag behavior). Native needs local ownership/ally/roles and bounded loaded collision, not global tag mutation or arbitrary first-player attribution. The unused scale-only helper repeats initial visual writes and is not itself a damage producer.

## Native resource/currency aliases checked

Production searches cover flags, score names, translated/model names and current services. PlayerStore.Profile:104 has persistent real coins; GameController already awards them, OriginalGuiDesign:664 reads profile coins for swrg.coins. Therefore type10 can use actual currency service, not a dummy score. GameController.Participant lives are private game state; :126 reads, :584 decrements, :609+ controls respawn; there is no existing additive-lives bridge for source swrg.lives. A score write would not grant a life. Mana/manaregen have real native LbcMana and signed-state semantics; adding raw scores without corresponding state API is insufficient. No native touhou_power/time_orbs/bomb_fragments/life_fragments consumer or skill28 acquisition found. LbcDanmaku and LbcTimeStop offer reusable underlying services, not proof of item/consumption/reward parity.

## Concrete questions/boundaries for parent file

No Belt/Quiver answer is required for full six-bomb inventory plus Liquid. Carry existing author ownership/default-allies/roles/Time Stop rules; no new WILD question is necessary. Keep source self-target damage distinct from creator-owned collateral. Decide exact optional owner provenance only from actual known native producer metadata; absent source metadata is not a player to blame.

Touhou remains one deferred complete family because usable outputs/lives/acquisition are genuine unfinished dependencies. Source globally associated nearest-snowball admission and global fresh-drop/XP scans should be recorded before any redesign; do not silently narrow them to one nearby kill. This note does not authorize making skill2/28 selectable, altering existing qualified Cultus/Boss payloads, or using unsupported currencies as substitutes.

## SHA-256 ledger

Full source bytes, plus native reuse snapshots. Models and language remain original RP; no edits needed. Exact presentation remains authoritative in these sources.

```text
function/other/instants/select.mcfunction f94c2ff79adac83d4d409d301612b94aeb53652acd925086e20c8654171b56fd
function/other/instants/select_old.mcfunction 372dd17ad2ac40c31010a65b7a48461281faf1769789f0f9520c04c00b37cb22
function/other/penises/player_select.mcfunction 6e0513351ffb17294ca8cf4c21d019e90aff5a36955b42383d4145a46343ddfb
function/other/penises/select.mcfunction fd56f8fe470b7912b7c80ef69c0b5df093c972eb81de79cb459f9d2f07c6375b
function/workingitems/penis_mob.mcfunction 5bc6fbdca1d6d8b1b8ebe1adbf900735ec98d11af7928fe1b4d66e2e8d54afc6
function/items_tick.mcfunction 253d455ccc455045ec5c0b63e21642cb45fd2d067d5ddbea5f0a721e7854e2bc
function/items_tick_tag.mcfunction 0daf5c1ec63b873d02055dd46f16401ab660ef4fef118a484f4c38eb15aad88b
function/items_tick_act.mcfunction 81d149d466d906b411143351c9a944263d1e42934206c29e2cb94b84a58e0cbf
function/projectiles/eggs.mcfunction b5ac5a2ddd652af8b24b0f05cff08df82b55fea11819ece48692344f506bab66
loot_table/touhou_item.json f5d4a07969f492deaaa24365830e94f818cb5981b66f0ab4f49f9213dc49f201
loot_table/bomb_item.json d62355cec4c3881e309eb304fc9d44ab32caad3a92c928d9c5c05701c5c7fcbd
loot_table/time_stopper.json 5413730782fa039d08e7fc546ea837c51de2dfdf81439f8805cd58a8584828ee
function/swrg_kit_integration/game/skills/player_killed_entity.mcfunction 536b164cf00fe65b885e1ece25a0389fd16fee8555af110dd8861ba0243fb0ed
function/swrg_kit_integration/game/skills/danmaku_cards_spawn.mcfunction 93ce5cf0877060d76ef2511733f8d4c488557cb3bd9f28da2630ed763e74bc9f
function/swrg_kit_integration/game/skills/score_items_spawn.mcfunction 29ba2ad05935b6b71dbc9c862f3daf7889c92cace16d1b6558c0d1ce8ba68245
function/swrg_kit_integration/game/skills/score_items_spawn_1.mcfunction 0cbfa4ba80471e3beebb1af7baf52cc7dccf62f4d35229f3f3b5a8fd476983c3
function/other/penises/normal.mcfunction 40ac4298f3544ad20f15fb5a59e3bf8eca51a22796c9f66ad45883786000aa3b
function/other/penises/dirty.mcfunction 3e6709d7af8c81aaec9bec9ab32197bc8b03c664b26b14c2267541073818e30d
function/other/penises/nigger.mcfunction 6779a97ee3f31c7105001a86c8b399fa6bcfc1fef681874a4d71480dfda17324
function/other/penises/holy.mcfunction 3e4903cf7caaf605277a2c8f7bb1c2292b71f281dd82346a586b5e89948f9c5a
function/other/penises/horse.mcfunction a6d6a88065c50340d6751a5786f1ae46bf9f709f1e756a2de8a5e2ffcad1bec3
function/other/penises/multicast.mcfunction 2e0d95a8872995380e9b7e92b059ab58df2a3baf9fffb16b52b49b8f8c4b2a44
function/swrg_kit_integration/items/touhou_items/addmana.mcfunction f2ae39bf3086f4c3552a27845484b88406d936447510cf9d7ebcc1da4bc14cfa
function/swrg_kit_integration/items/touhou_items/bomb.mcfunction 93a80a214cd4b2f6fe969c0c2cd540c61d055e1a9ff283545b1b96ebd16bd290
function/swrg_kit_integration/items/touhou_items/give_bomb.mcfunction fdb587eb4e49427f401cd52d1ba499f5dae62048eb0ae7c0cb9416881cf79ee4
function/swrg_kit_integration/items/touhou_items/give_life.mcfunction d152cfdf4db6a53d32ce308f416377e6c496d76a8231a99a3c775b6d93d3852a
function/swrg_kit_integration/items/touhou_items/select.mcfunction f2706f30959bfc849810a695526ff2322150b130a78c4ba1e2b1836569997642
function/swrg_kit_integration/items/touhou_items/time_orbs_count.mcfunction b21996797e9b2a4f6c624c225b1c646af044fc5ac244da4a407f825b9cd286a5
function/swrg_kit_integration/items/touhou_items/time_orbs_count_tell.mcfunction 5ee941380423cadb8039d061249f4ca2f406914518ebef59be2c6f2d1d8c58bc
function/swrg_kit_integration/items/touhou_items/time_orbs_give_clocks.mcfunction b3eb9338487649e312c3bb136799488e4c255fcf89cd65648022bebd28147c8e
function/swrg_kit_integration/items/touhou_items/time_stop.mcfunction 958a558510a9f2911358751559bd06c8458564664cfd2455c865e7e1de9e5c01
function/swrg_kit_integration/items/touhou_items/update_power.mcfunction 3596b57b3457730100b707763afbef0c53120d23119a903bf6f1dab3f2fc99c5
src/main/java/me/skywars2/game/LbcKfcRewards.java ac5bb4aab25ffbfd3db3704972c19850f5b41426069b4e42e1b831684ddb40a6
src/main/java/me/skywars2/game/LbcDanmaku.java a6a151bd49b39828e5733aa0fd46aff220c9a08ab05ba54d5e3206e7941c974b
src/main/java/me/skywars2/game/GameController.java 3539528cc0aeda12172b307504c4e3f7f8fe530c6ba64a04c1299769599de98b
src/main/java/me/skywars2/game/PlayerStore.java 9c6c1fd2a4b06c07d0f0c2bdfa0b59baa3d1c1615e4779e06800135eada7a3d5
RP/assets/lbc/items/penis_1.json 4ae204b538e639dcb8f8dfd15b855e3b6328cde09e39a8692db9e19a0bc5e202
RP/assets/lbc/items/penis_2.json 2190d2b81d5a02638f49bdb736e245f094793f2b5dcbb2100ad2170cae1fbe1a
RP/assets/lbc/items/penis_3.json 1788a31804cc4663dff71858c48f21fa1a06042c06c8c54e4f8e1e28a1a09cf0
RP/assets/lbc/items/penis_4.json 3b12dda8731e4f05ba03976d0360d225e01741e11c4d13b4092dbd9f8603f67a
RP/assets/lbc/items/penis_5.json 2358eac0f358888a2528172255b41181aeeb83dd47ee7fca96bc5d115a50d2d5
RP/assets/lbc/items/penis_6.json f72b4c6b3558a7b2ba1cb389db180950083d4b9f37276bed5dfbd56191a40196
RP/assets/lbc/items/power_item.json 1286b55cd29f0dd27579fe2546cb0cf316fcd0bac5d01d9d848e1c7a6fbc73d9
RP/assets/lbc/items/big_power_item.json 88293ae266d7d62dd76b3fc7dc1348c3a9169ad1bc4167015de0293d26b66463
RP/assets/lbc/items/full_power_item.json e2f936e87bcabbce0cb1635d2a09993db4df554246991ee20999bb24d6764c0c
RP/assets/lbc/items/score_item.json 4f4b296be691c58f247a62105e3d0fd8ee86fad6919a70909b0d106d36a0fdff
RP/assets/lbc/items/life_item.json 562e1bdca293dca7845555dcbcb058540a674c9361da3a1b3d385f29e2104920
RP/assets/lbc/items/time_orb.json 035992cff81ba07c9677772f95131552373cadcddc817acd5fc66e262cb1c776
RP/assets/lbc/items/bomb_item_fragment.json 0e6f556cd2c83c77003dda3e98d851327f0d881b093513d5a8d16137481815d0
RP/assets/lbc/items/life_item_fragment.json 0ee6eaa4b5d1aa1beccc40f8f6b26f54f2c527a4f00648e685e87ac39b5ef8da
RP/assets/lbc/items/faith_item.json 2cb0ab76249baf6cacf90f52d0ba40ebb56cb15aabcc61142dfaf7044c08ebc3
RP/assets/lbc/items/money_item.json b8a330017291eb33e0f8ed2b6a90db5e42031e267e9831868c5922c0e3ba0ee0
RP/assets/lbc/items/bomb_item.json 44132d4b2b3f20f719c83a35b5b7dc1bcde6785f2a06a3f938437b956a54ed8c
RP/assets/lbc/items/time_stopper.json 43c381af1a332d005b293ad47b2bf1e3b521e2c0d9d8b26ea9179866bef08075
function/items/wands/danmaku_staff/bomb_tick_fast.mcfunction a7f03cbb77d5ccf562926a8419b55d38e6fb38fb5279a87823200df671cf0cb4
function/items/wands/danmaku_staff/bomb_tick_fast_scale.mcfunction dcc482f55e18f1e5606c1b7762839aeff3ed4527daa5ac79612530daeb34e2d0
function/items/wands/danmaku_staff/bomb_tick_2_fast.mcfunction 133b28c36e862a5f8ae8f477bfd5d72d98b20e7d597d8e2a555fadbf76a13528
function/leave_death.mcfunction 71fde472367495b17a72c5eaac1039161b3992cd642683994ccc062a763ec715
Native26.3/net/minecraft/commands/CommandSourceStack.java 0a85cab198976541b7e1bf41e9823372c7f914c54d80a3478639c9bd9954ea56
Native26.3/net/minecraft/server/commands/ExecuteCommand.java 5018693cf75062df762923286ba1f2d3c2f0cf3beccdf05c41b852642275c5e9
Native26.3/net/minecraft/commands/execution/tasks/CallFunction.java bed543029222d28792473eb9a2dee12447c9ea30474871a37d730bb6e77d3cd6
Native26.3/net/minecraft/world/entity/item/ItemEntity.java 5b21a1aa3d16e7880501d90d6c7beff3f02489890967b64e2cdf4857730ec2f9
```

Frozen read-only preparation; no native qualification or physical client/audio observation claimed.
