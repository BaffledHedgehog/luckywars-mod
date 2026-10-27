# Movement armor - jump, airborne and sprint source closure

27 October2026 read-only prep. S=`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`. Only this new note written; no builds/tests/server/source/RP/git changes. Exact source commands inspected, legacy runtime unobserved. Existing native signed effects/layers/caps, role/ally/Greed, Time Stop, resident terrain and original-owner attribution remain authoritative. Rocket Boots, Overload and independently qualified movement services are not re-ported here.

## Concrete native gap matrix

|Source family|Current native evidence|Real remaining gap|
|---|---|---|
|jumptnt leggings|no jumptnt or tntjump live handler; no source loot leaf found|three-TNT jump callback exists in source dispatcher but no ordinary acquisition leaf established|
|reactive_old boots|no reactive_old/reactive0 live handler|legacy levitation/feedback jump passive; modern reactive/Rocket Boots are separate already-supported behavior|
|techno Wyvern/nitro|LbcDamage knows TECHNO for existing armor protection; LbcMagic levitation admission recognizes techno legs; reforge rules exist|neither wyvern/nitro jump input, cooldown/resource/HUD producer found; partial shared support is not full gameplay|
|jumpboost Traveller|LbcDamage CUSTOM_SETS includes jumpboost; LbcBolts reforge rules and existing Rocket craft routes exist|Traveller chest airborne slowfall, helmet food, full-set ten-second cleansing/buff callbacks absent|
|moisei boots|LbcBolts reforge rule and native kit7 catalog route exist, no moisei movement handler|continuous sprint tiers, dirt path and ten-TNT continuation absent; kit7 dirt grant is not boots passive|

Exact quoted flag/model/helper searches exclude checks and generic item registration. Recommended next complete independent family: Traveller four-piece set (all source callbacks below, clear finite native effects); alternatively standalone Moisei Boots complete sprint mechanism. Do not call merely one Traveller chest helper full set completion. TNT leggings lack source acquisition, so do not invent an item presentation. Techno set broader armor passives are out of scope; qualifying just two movement actions must state that boundary.

## Shared cadence and native input timing

`load:3` jump1 minecraft.custom:minecraft.jump; players_tick:6 calls items/armor if score>=1; armor tail sets jump1=0 AFTER all jump actions, falltimer+=0 only preserves/initializes score. Native existing LbcEquipmentMixin jumpFromGround TAIL is appropriate new jump callback primitive, after current jump impulse (new levitation/effects change subsequent native ticks, not retroactive jump velocity). Multiple pending source jumpstats coalesce one dispatcher call; native per-jump callbacks need declared timing.

players_tick:10 if actual is_sprinting -> on_sprint; :13 if sprint_timer>=1 and not sprinting -> players_sprint_end. Every active player tick, not20-tick second. on_sprint:1 increments sprint_timer BEFORE boots effects and path. This is shared player sprint time, even without Moisei Boots; equipping boots mid-sprint sees accumulated timer. Sprintend resets0, removes specific sprint modifiers c1cda724-0ea2-460a-98b8-9631eafd2515 movement_speed and585f98b8-a8b8-4e4c-838a-f2e124831040 attack_damage, removes sprinted tag, bubble_column.bubble_pop master onlyowner volume100000/pitch1. Do not attach unrelated sprint mods to Moisei.

players_tick:25 if OnGround!=true -> on_falling; :26 if grounded -> on_ground sets falltimer0. on_falling tests chest falltimer>=20 BEFORE adding1, so effect begins after20 prior airborne ticks. This includes rising/jumping/hovering, not negativeYvelocity/fall distance. on_falling_second_check (players_1s:51) dispatches trinkets only, not Traveller. Source outer active tick skips global Time Stop, spectators/time_stopped, nexus/imprinted; scheduled10s/15s timers have separate outer scheduled cadence and do not automatically share tick guard. Native established Time Stop policy governs all.

## Jump actions - complete bounded closures

### TNT leggings jumptnt

armor:1 feet-position jump event if armor.legs custom_data jumptnt1 -> tntjump:1-3 summons THREE vanilla TNT at ownerY-.1, fuse0. No per-item magic/mechanism guard, mana/resource/cooldown/effect/direct velocity. Native blast power/default TNT damage/knockback/terrain, three separate explosions, source no owner/team tags. Must carry original owner and normal allied-player/role/default blast policy; don't replace with one triple-power blast or direct launch. No literal jumptnt loot leaf found in data; flag/catalogue alone is insufficient acquisition.

### Old reactive boots

armor:2 feet reactive_old1 -> reactive0:1-3 self Levitation20ticks amp0 hidden, flame100 FORCE atY-.2 spread.2/.1/.2 speed.1, firework_rocket.blast master within20 volume10/pitch0. No local suppression, private cooldown/payment. Source legacy_luckyblock supplies old boots leaf; modern reactive/Rocket behavior and its authored ownership/flight policy remain unchanged.

### Techno Wyvern leggings

armor:3-4 triggers while jumping AND sneak predicate (actual player input), legs techno1, feet NOTtechnomk2, no stopper marker20. If coldown8>=1: tell owner component sequence wyvern_chestplate.cooldown dark_purple + ability_recharge white + coldown8 score white + second.lbc white. Otherwise wyvern executes:

- Levitation20ticks amp127 hidden and Resistance180ticks amp4 hidden self.
- Enemy !spectator #mobs distance.01..10 take8 CACTUS by temporary dmger owner; no HurtTime scan. Enemy nonspectator players .01..10 Blindness100ticks amp0 hidden.
- ender_dragon.growl master global volume4/pitch1; cloud1000 FORCE ownerY5 spread1/5/1 speed.2.
- coldown8=40 SECONDS; players_1s:5 decrementspositive; resets:35 zero. No mana payment. Helper name Wyvern chestplate is misleading; source checks LEGGINGS.

Original cause must replace temporary global tags; use native cactus type and existing cooldown rules, no manual iframe reset. Default protected allied players and technical-role exclusion apply, friendly mobs remain targetable. Source levitation127 retained under signed/native policy, not direct arbitrary velocity.

### Techno nitro boots

armor:5 jump event, feet techno1, nitro>=1, pitch89..90 (looking almost straightDOWN), no stopper marker20. nitroboots gives owner Resistance20ticks amp4 hidden, thenTEN identical native creepers at localforward-1 from ownerY+1.2, pitchflattened0; ExplosionRadius1, Silenttrue, Invulnerabletrue, ignitedtrue, Fuse0. Removes nitro1 after spawns. Preserve ten radius1 explosions/native impulse/terrain, not a fake single velocity or one larger blast. Owner attribution required; source no owner tags. Current techmk2 boots also contain techno1 and aren't excluded here.

resets:24 nitro4; players_10s:1 adds1 if NOT score>=4 (recharge toward4, no trim if above4); purify:37 sets0; KFC all_up_scores_no_mana:16 adds1 uncapped; special stew setsINTMAX. HUD ui/get_data:17 feettechno shows white E101 nitro. Source no normal gear-held-only recharge requirement. Private duplicate resource would break KFC/purify. No mana cost. Existing Rocket Boots use a different resource/recipe and must not be replaced.

## Traveller jumpboost complete four-piece behaviors

Chest on_falling:2 when chestjumpboost1, prior falltimer>=20, no stopper_magic marker20 -> traveller_chestplate:1 grants Slow Falling20ticks amp0 VISIBLE each active airborne tick. No nearby nomagic_active aura check, no mana/cooldown. Ground zeroes falltimer; existing granted effect naturally expires after20ticks. Self only, no feedback/directvelocity. Reuse shared falltimer native rawtick semantics, not time-in-seconds or actual downward motion.

Helmet players_15s:3 every scheduled15s, headjumpboost1 and no stopper_magic marker20 -> players_15s_eatt:1-2 Saturation argument1 amp1 defaultvisibility (**1 native instant-effect TICK**, not20), food_count+1. No source eat sound. Native metabolism/food_count is shared; don't synthesize a fullfood refill.

Full set players_10s:4 requires all four equipped jumpboost1 -> traveller_set_bonus. NO local magic/stopper/nomagic gate on this branch. Calls popit_2 SIX times, each independently random1..20 with replacement; then seventh random1..11 buff draw. This is not six distinct removals and not clear-all-debuffs.

Each popit_2 invocation first sends its owner only bubble_column.bubble_pop, MASTER volume100000/pitch1. Full-set activation therefore sends SIX self-only sounds, including duplicate random removal outcomes. Then random1..15 clears whole native effect type in order: instant_damage, levitation, mining_fatigue, nausea, poison, slowness, unluck, water_breathing, weakness, wither, bad_omen, blindness, glowing, hunger, darkness. Outcomes16..20 set custom score lookback/piss/scared/shock/twitching respectively0. Source includes beneficial water_breathing and does not include modern raid_omen transport by alias. Native corresponding shared custom-status cleanup must be reused, including owned state not merely stale scoreboard. Clears all effect layers for chosen type, not only active level.

Buff random1..11: Absorption1200ticks amp4; FireResistance1200amp0; Haste600amp1; InstantHealth argument1 amp1 (=1TICK); Invisibility600amp1 hidden; Luck200amp0 default; NightVision2400amp0; Regeneration200amp1; Resistance400amp1; Strength200amp0 default; WaterBreathing600amp0 default. No particle/sound payment. Retain signed/independent layers and native instantheal/undead behavior. Loot boots/legs equipment attributes remain ordinary native component behavior, not an extra tick effect.

Old jumpboost_old boots have separate legacy_tick_1s_player:3 grants JumpBoost20ticks amp3; this is legacy-mode equipment tick, not current Traveller. Existing modern Rocket crafting consumes Hermes jumpboost boots +reactive boots; preserve it. Do not claim source current four-piece support by implementing oldboots alias.

## Moisei Boots - complete sprint closure

on_sprint:3 feetslotmoisei1 unless stopper_magic marker20 -> moisei_boots_tick every sprinttick. NO local nomagic_active aura/on-ground gate. Sprint timer increments irrespective of gear. Speed20tick grants: timer1..40amp0,41..80amp1,81..120amp2,121..160amp3,161..200amp4,201..240amp5,241..280amp6,281..320amp7,321..360amp8,361..400amp9,401..450amp10,451..500amp20,501..550amp30,551..600amp40,601..650amp50. At651+ NO new speed grant; ten fuse0 TNT spawn EVERY sprinttick localback5 using owner's full current look pitch (helper anchoredfeet, notflattened). Prior Speed50 layer expires20ticks after last grant. No cooldown/reset in explodehelper.

Every sprinttick also lays dirt at feetY-1 with yaw/current pitchFLATTENED0, replace #minecraft:airs STRICT. Samples forward0,.25,.5,.75,1,1.5,2,3. Extra axis-aligned local-endpoint fill segments: timer>=100 forward3.5..4;>=2004..5;>=3005..6 AND6..7;>=4007..8;>=5008..9 AND9..10;>=60010..11. Fill uses native floor coordinates, potentially duplicates cells/diagonal rectangular spans, no drops/air-only overwrite of existing solids. Platform/terrain support/fall/block ownership required; never load new chunks or replace source with cosmetic particles. No customparticles/sounds in Moisei helpers beyond native TNT and shared sprintend sound.

Acquisition standalone moisei_boots leaf referenced by loliluckyblock:688 and transformables_boots_all:28. Boots task advancement parent traveller, player_killed_entity targetplayer while equipped feetmoisei1, rewards gain/kit. Native kit7 catalog exists, gate moisei_boots, but kit/moisei grants16 rolls dirt64 (1024 dirt), not theboots! Source no dedicated ingredient recipe found. No specific new lifetime/reset beyond shared sprinttimer0 on stop; effects naturally expire, owned TNT/path consequences use shared match lifecycle.

## Presentation/acquisition evidence and remaining questions

Relevant exact source leaf component summaries below include existing vanilla item IDs, all supplied components/name/lore/flags. Preserve omitted fields and authored styles, not a new generic armor rewrite. Source current Traveller from neko_set_traveller pool, Techno from neko_set_techno plus overlapping technomk2 flags, oldreactive fromlegacy_luckyblock. Reforge/craft rules are existingpartial integration, not gameplay support. No new RP assets/template changes established as necessary.

Potential author questions: Moisei extreme Speed50 then speedgrantstop/tenTNT EVERYtick651+ and huge sprintend volume100000 are literal suspicious source; nitro tencreepers versus intendedblastcount; TNTlegging no live loot; shared sprint accumulation evenunequipped may surprise. Travellerfinitehelperclosure has no HurtTime ambiguity and is preferred next complete set. Established native ally/TimeStop/roles/owner policy resolves ordinarysource gaps without WILD exceptions. Do not broaden this audit into all Techno armor or re-port Overload/Rocket.

## Exact relevant loot component summaries

`loot_table/moisei_boots.json` SHA256 `d722d26758a6cf2bf392ba4001886ee8c75aa1d5bf488fed8908d8d54b10d37b`

```json
{"base":"minecraft:leather_boots","functions":[{"function":"minecraft:set_components","components":{"minecraft:tooltip_style":"lbc:tier_ss","minecraft:item_model":"lbc:armors/moisei_boots","minecraft:max_damage":600,"minecraft:equippable":{"slot":"feet","asset_id":"lbc:moisei"},"minecraft:attribute_modifiers":[{"type":"minecraft:armor","slot":"feet","id":"gehrththc55yyyyyharmor","amount":2,"operation":"add_value"}]}},{"function":"minecraft:set_name","name":{"translate":"moisei_boots","color":"#632F19","italic":false}},{"function":"minecraft:set_lore","mode":"replace_all","lore":[[{"translate":"type","italic":false,"color":"white"},{"translate":"boots","italic":false,"color":"white"}],{"translate":"moisei_boots.lore","italic":false,"color":"aqua"}]},{"function":"minecraft:set_custom_data","tag":"{transformable:1,boots:1,moisei:1}"}]}
```

`loot_table/neko_set_traveller/helmet.json` SHA256 `47c7057a15ca2f666bdf6c429fc190e7ab67749ae79a40fa2e92e5e6fd19fcd2`

```json
{"base":"minecraft:leather_helmet","functions":[{"function":"minecraft:set_components","components":{"minecraft:item_model":"lbc:armors/traveller_helmet","minecraft:tooltip_style":"lbc:tier_c","minecraft:max_damage":400,"minecraft:equippable":{"slot":"head","asset_id":"lbc:traveller"}}},{"function":"minecraft:set_name","name":[{"translate":"travel_helmet","color":"green","italic":false}]},{"function":"minecraft:set_lore","mode":"replace_all","lore":[[{"translate":"type","italic":false,"color":"aqua"},{"translate":"helmet","italic":false,"color":"aqua"}],{"translate":"travel_helmet.lore","color":"#FFBFBF","italic":false},{"translate":"set_bonus","color":"white","italic":false},{"translate":"set_bonus_traveller","color":"white","italic":false}]},{"function":"minecraft:set_custom_data","tag":"{transformable:1,helmet:1,jumpboost:1}"}]}
```

`loot_table/neko_set_traveller/chestplate.json` SHA256 `4e9e3d9f9f69ebec3fe8405646979840af86483a982e0c2dadf4537539889e6d`

```json
{"base":"minecraft:leather_chestplate","functions":[{"function":"minecraft:set_components","components":{"minecraft:item_model":"lbc:armors/traveller_chestplate","minecraft:tooltip_style":"lbc:tier_b","minecraft:max_damage":400,"minecraft:equippable":{"slot":"chest","asset_id":"lbc:traveller"}}},{"function":"minecraft:set_name","name":[{"translate":"travel_chestplate","color":"green","italic":false}]},{"function":"minecraft:set_lore","mode":"replace_all","lore":[[{"translate":"type","italic":false,"color":"aqua"},{"translate":"chestplate","italic":false,"color":"aqua"}],{"translate":"travel_chestplate.lore","color":"#FFBFBF","italic":false},{"translate":"set_bonus","color":"white","italic":false},{"translate":"set_bonus_traveller","color":"white","italic":false}]},{"function":"minecraft:set_custom_data","tag":"{transformable:1,chestplate:1,jumpboost:1}"}]}
```

`loot_table/neko_set_traveller/leggings.json` SHA256 `7c8559abadaeab5240e1b96f02e8e57b4ffeb5938d42ecd4fd9345354ec15242`

```json
{"base":"minecraft:leather_leggings","functions":[{"function":"minecraft:set_components","components":{"minecraft:item_model":"lbc:armors/frog_leggings","minecraft:tooltip_style":"lbc:tier_c","minecraft:max_damage":400,"minecraft:equippable":{"slot":"legs","asset_id":"lbc:traveller"},"minecraft:attribute_modifiers":[{"type":"minecraft:armor","slot":"legs","id":"gtqwthrt7k67k7mkmghjor","amount":3,"operation":"add_value"},{"type":"minecraft:jump_strength","slot":"legs","id":"gtqwt5535jghjmkmor","amount":1.25,"operation":"add_multiplied_total"},{"type":"minecraft:safe_fall_distance","slot":"legs","id":"gtq432364k67fghk7mkmor","amount":2,"operation":"add_multiplied_total"}]}},{"function":"minecraft:set_name","name":[{"translate":"frog_leggings","color":"green","italic":false}]},{"function":"minecraft:set_lore","mode":"replace_all","lore":[[{"translate":"type","italic":false,"color":"aqua"},{"translate":"leggings","italic":false,"color":"aqua"}],{"translate":"frog_leggings.lore","color":"#FFBFBF","italic":false},{"translate":"set_bonus","color":"white","italic":false},{"translate":"set_bonus_traveller","color":"white","italic":false}]},{"function":"minecraft:set_custom_data","tag":"{transformable:1,leggings:1,jumpboost:1}"}]}
```

`loot_table/neko_set_traveller/boots.json` SHA256 `6603eb208789780e3d82dd7e94198cd3586edb62c95d2ec650184f121f1b7577`

```json
{"base":"minecraft:leather_boots","functions":[{"function":"minecraft:set_components","components":{"minecraft:item_model":"lbc:armors/hermes_boots","minecraft:tooltip_style":"lbc:tier_c","minecraft:max_damage":400,"minecraft:enchantments":{"minecraft:feather_falling":10},"minecraft:equippable":{"slot":"feet","asset_id":"lbc:traveller"},"minecraft:attribute_modifiers":[{"type":"minecraft:movement_speed","slot":"feet","id":"ggfdhfghfghfdhgspeed","amount":0.5,"operation":"add_multiplied_base"},{"type":"minecraft:armor","slot":"feet","id":"ghhghfdtyty55555","amount":2,"operation":"add_value"}]}},{"function":"minecraft:set_name","name":{"translate":"hermes_boots","color":"dark_green","italic":false}},{"function":"minecraft:set_lore","mode":"replace_all","lore":[[{"translate":"type","italic":false,"color":"aqua"},{"translate":"boots","italic":false,"color":"aqua"}],{"translate":"set_bonus","color":"white","italic":false},{"translate":"set_bonus_traveller","color":"white","italic":false},{"translate":"hermes_boots.lore","color":"gold","italic":false}]},{"function":"minecraft:set_custom_data","tag":"{craftusable:1,transformable:1,boots:1,jumpboost:1}"}]}
```

`loot_table/neko_set_techno/leggings.json` SHA256 `1fff03278df9b5b0aed397aa1f59c64a0630ec5516fdc391fe2dc57973cfb345`

```json
{"base":"minecraft:leather_leggings","functions":[{"function":"minecraft:set_components","components":{"minecraft:item_model":"lbc:armors/techno_leggings","minecraft:tooltip_style":"lbc:tier_a","minecraft:max_damage":600,"minecraft:equippable":{"slot":"legs","asset_id":"lbc:techno"},"minecraft:attribute_modifiers":[{"type":"minecraft:armor","slot":"legs","id":"gehtbjuiyrtefrfggfgfor","amount":5,"operation":"add_value"}]}},{"function":"minecraft:set_name","name":[{"translate":"power_leggings","color":"aqua","italic":false}]},{"function":"minecraft:set_lore","mode":"replace_all","lore":[[{"translate":"type","italic":false,"color":"aqua"},{"translate":"leggings","italic":false,"color":"aqua"}],{"translate":"wyvern_chestplate.lore.1","color":"white","italic":false},{"translate":"wyvern_chestplate.lore.2","color":"white","italic":false},{"translate":"wyvern_chestplate.lore.3","color":"white","italic":false},{"translate":"wyvern_chestplate.lore.4","color":"white","italic":false},{"translate":"wyvern_chestplate.lore.5","color":"white","italic":false},{"translate":"wyvern_chestplate.lore.6","color":"white","italic":false},[{"translate":"set_bonus","color":"white","italic":false}],[{"translate":"set_bonus_techno.1","color":"white","italic":false}],[{"translate":"set_bonus_techno.2","color":"white","italic":false}],[{"translate":"set_bonus_techno.3","color":"white","italic":false}],{"translate":"craft_with_reactive_leggings","color":"gold","italic":false}]},{"function":"minecraft:set_custom_data","tag":"{craftusable:1,transformable:1,leggings:1,techno:1}"}]}
```

`loot_table/neko_set_techno/boots.json` SHA256 `48f0901e83284120b7006e0cf709fa48d2b05400bf189e0fb915d623ab8c93d6`

```json
{"base":"minecraft:leather_boots","functions":[{"function":"minecraft:set_components","components":{"minecraft:item_model":"lbc:armors/nitroboots","minecraft:tooltip_style":"lbc:tier_a","minecraft:max_damage":600,"minecraft:equippable":{"slot":"feet","asset_id":"lbc:techno"},"minecraft:attribute_modifiers":[{"type":"minecraft:armor","slot":"feet","id":"geiyuirxwqzg4gfbutu","amount":3,"operation":"add_value"},{"type":"minecraft:safe_fall_distance","slot":"feet","id":"jdgjdgyvb6kij7finjukukifjgjg","amount":3,"operation":"add_multiplied_total"}]}},{"function":"minecraft:set_name","name":[{"translate":"nitro_boots","color":"aqua","italic":false}]},{"function":"minecraft:set_lore","mode":"replace_all","lore":[[{"translate":"type","italic":false,"color":"aqua"},{"translate":"boots","italic":false,"color":"aqua"}],{"translate":"nitro_boots.lore.1","color":"white","italic":false},{"translate":"nitro_boots.lore.2","color":"white","italic":false},[{"translate":"set_bonus","color":"white","italic":false}],[{"translate":"set_bonus_techno.1","color":"white","italic":false}],[{"translate":"set_bonus_techno.2","color":"white","italic":false}],[{"translate":"set_bonus_techno.3","color":"white","italic":false}],{"translate":"craft_with_reactive_boots","color":"gold","italic":false}]},{"function":"minecraft:set_custom_data","tag":"{craftusable:1,transformable:1,boots:1,techno:1}"}]}
```

## Source SHA256 ledger

- `function/items/armor.mcfunction` `9d0141c94439147e77eb3a1184cf00795567ca75655ae59eb537b312d759fac6`
- `function/items/on_sprint.mcfunction` `6db40ad81082d1a14cce50002956c13ff34f54104f8b0a6aa8fcba7ec30f0e2c`
- `function/items/on_falling.mcfunction` `51581a63eada18c980b448f63f99215322662f6dc0dcf117fb9169aa01b5e620`
- `function/items/on_ground.mcfunction` `28ded60b84820bba6604f93d2e43309220187e456b65709fce189d50678ae9ee`
- `function/players_tick.mcfunction` `851491714a1ed2831411a19a829d362e051fdc4b2d5b3db581318e01d383c483`
- `function/players_sprint_end.mcfunction` `ef5df784dd9d20cb166c97c8c72f66c1c5e33ce7cc5218ffe2531a6cb9de6a3b`
- `function/players_1s.mcfunction` `4e354224511c16f697d04089b43875b28547a24bec54c0e6190b56b4dcdff905`
- `function/players_10s.mcfunction` `ae3a5e4f7dec892f847bbda5fdedfd78fc5e2fe851c9d5f6b89e16a9d5d3e5eb`
- `function/players_15s.mcfunction` `c05c8439938cae53e84ebb671eba52e3a3aacd40d9178671aba35b2dd8be4ef1`
- `function/players_15s_eatt.mcfunction` `9182e8c588835333eb7db73796c220fdecebb643514e6a07c16a427dc2cefde5`
- `function/armor/traveller_chestplate.mcfunction` `edf218e32dcdd86e4b034c512e8cbc18ce0ccef4270f0cd7752946dccb39c89a`
- `function/armor/traveller_set_bonus.mcfunction` `371b9b2f67620ce099707ac027efd81d9762621337d21dc4767a0517aaa08103`
- `function/workingitems/tntjump.mcfunction` `0205265ec35e0230a50fe53ad0fe3d07c8e22c95c8881e831a314eea4afb130f`
- `function/workingitems/reactive0.mcfunction` `01323977efcfcc876302a5a6ef32eec80a3f1d1ccec3ddede1c52531eb06567c`
- `function/workingitems/wyvern.mcfunction` `d81f052ff22c60dfb043851e61b95963258447696596e5fa3d84f7d226690f38`
- `function/workingitems/nitroboots.mcfunction` `62a44de375dd2da9f9aa46206661f9c228ca8ecde3b06e481d958a0767e3d3ac`
- `function/workingitems/moisei_boots_tick.mcfunction` `af4ed301c5ef950ce2d2e2cec014f1bfd904c21ecda34f8365a973667062b249`
- `function/workingitems/moisei_boots_tick_place.mcfunction` `6c7b9730c62ce1dde11ffa7e83a4823666099b3e8c4c564486370c3f5cf1ec41`
- `function/workingitems/moisei_boots_tick_explode.mcfunction` `2954f52097b0923926e3e62db7ab5ce9ae0609da0af3916d9e5edd3594b00525`
- `function/workingitems/popit_2.mcfunction` `a4716fe075b9dbd8fc479b94a9f4a543c5ead06ac7f8d0cc504d14ffbc2917af`
- `advancement/true_advancements/armor/moisei_boots.json` `bdbffa8c4d6b6f340a7a63c1f70b6db133735daf0fb07868f76d9ece456c6ee9`
- `advancement/true_advancements/armor/traveller.json` `eb4d3982c4b34ba341f4ce40f0f2ff5aa544f49c03cdc2eda52af733d2970b58`
- `advancement/true_advancements/armor/techno.json` `e0cafdef0ce8818ef0b8fdb7a169c79329a880726103601eadcecdacdc080ef2`
- `loot_table/legacy_luckyblock.json` `bd7a1a5aa859362d96e93b7261ed160b48686829ffcd8be33a24daa93818ef01`
- `loot_table/kit/moisei.json` `da62ae73724c82bb184c4ed624e3c89861e1bf9964a643c1143a7577634dd82d`

## Read-only RP exact language values

`en_us` SHA256 `7971188ae0d2e5f16f7253c6495168650451599e2826eeb46f7cfcb4a0e507f7`

```json
{
  "craft_with_reactive_boots": "Can be crafted with Reactive Boots (Steampunk)",
  "craft_with_reactive_leggings": "Can be crafted with Reactive Leggings (Steampunk)",
  "frog_leggings": "Frog leggings",
  "frog_leggings.lore": "Increases jump height",
  "hermes_boots": "Hermes Boots",
  "hermes_boots.lore": "Can be combined with Jet Boots",
  "moisei_boots": "Moses' Boots",
  "moisei_boots.lore": "When you run, you can run through the air (turning it into shit)",
  "nitro_boots": "Nitro boots",
  "nitro_boots.lore.1": "Explodes, if you look down while",
  "nitro_boots.lore.2": "jumping, strongly pushing you forward",
  "power_leggings": "Wyvern Quantum Accelerator",
  "set_bonus": "Full set bonus:",
  "set_bonus_techno.1": "Cyborg: - Taking damage creates arcs of electricity",
  "set_bonus_techno.2": " - Water deals damage in an area (to you too)",
  "set_bonus_techno.3": " - Damage from Electric Arcs does not cause new arcs to appear",
  "set_bonus_traveller": "Traveller: - Gives random buffs and randomly removes debuffs",
  "travel_chestplate": "Floating Chestplate",
  "travel_chestplate.lore": "Protects from falling",
  "travel_helmet": "Helmet of Abundance",
  "travel_helmet.lore": "Periodically feeds the owner",
  "wyvern_chestplate.lore.1": "Wyvern flight:",
  "wyvern_chestplate.lore.2": "(Sneak + jump)",
  "wyvern_chestplate.lore.3": "throws up the owner very high,",
  "wyvern_chestplate.lore.4": "dealing damage and blinding",
  "wyvern_chestplate.lore.5": "everyone around. Gives invulnerability for jump.",
  "wyvern_chestplate.lore.6": "40 second cooldown."
}
```

`ru_ru` SHA256 `9ba24ccecc189950fe970f525ff45b6e40df5b375fabd333cfa6786ca8dac2fc`

```json
{
  "craft_with_reactive_boots": "Можно соединить с Реактивными Сапогами (Стимпанк)",
  "craft_with_reactive_leggings": "Можно соединить с Реактивными Поножами (Стимпанк)",
  "frog_leggings": "Жабьи поножи",
  "frog_leggings.lore": "Увеличивают высоту прыжка",
  "hermes_boots": "Сапоги гермеса",
  "hermes_boots.lore": "Можно соединить с Реактивными Сапогами",
  "moisei_boots": "Сапоги Моисея",
  "moisei_boots.lore": "Разбежавшись, вы можете бегать по воздуху (превращая его в говно)",
  "nitro_boots": "Нитроботинки",
  "nitro_boots.lore.1": "Взрываются, если смотреть вниз при",
  "nitro_boots.lore.2": "прыжке, сильно откидывая вперёд",
  "power_leggings": "Квантовый акселлератор виверны",
  "set_bonus": "Бонус полного сета:",
  "set_bonus_techno.1": "Киборг: - Получение урона создаёт электро-дуги",
  "set_bonus_techno.2": "        - Вода наносит урон в области (вам тоже)",
  "set_bonus_techno.3": "        - Урон Электро дуг не провоцирует появление новых дуг",
  "set_bonus_traveller": "Путешественник: - Дает хорошие эффекты и убирает плохие",
  "travel_chestplate": "Парящий нагрудник",
  "travel_chestplate.lore": "Защищает от падения",
  "travel_helmet": "Шлем изобилия",
  "travel_helmet.lore": "Периодически кормит владельца",
  "wyvern_chestplate.lore.1": "Способность виверны:",
  "wyvern_chestplate.lore.2": "(Присесть + прыжок)",
  "wyvern_chestplate.lore.3": "очень высоко подбрасывает",
  "wyvern_chestplate.lore.4": "владельца, нанося урон всем вокруг и",
  "wyvern_chestplate.lore.5": "ослепляя их.",
  "wyvern_chestplate.lore.6": "Перезарядка 40 секунд."
}
```


## Resource lifecycle and current/old pool completion

Additional full-score closure: load:10 adds coldown8 dummy, :20 nitro dummy, :29 sprint_timer dummy; load_scores_reload:8/18/26 remove them. These are ordinary source scoreboard persisted scores, no item-only NBT save timer. Vanity stores nitro :56 /coldown8 :81 and restores nitro:48/coldown8:73. Envy copies greater nearby nitro (envy_tick:5) and smaller nearby coldown8 (:30). KFC adds coldown8:8, nitro:16, sprint_timer:22. Purify clears coldown8:15, nitro:37, sprint_timer:49; resets initializes nitro4/coldown8zero. Nitro scheduled10s recharge toward4 applies regardless of equipped boots; don't truncate above4. Wyvern cooldown has no separate persistent HUD segment, only source cooldown tellraw on attempted jump. Nitro white E101 HUD while feettechno. Shared resource source/copy/purify must use existing counters, not private new timers; source integer extremes do not justify global clamps beyond authored caps.

No source special family-specific unload/save or death cleanup task was found for these action sources; actual native match stop/owner invalidation must clean owned TNT/creeper/terrain attribution and transient action state. Existing ordinary effect expiration remains. Full source scheduled cooldown/recharge ticks can run during globalstop while active movement tick is paused; native established active-time policy takes precedence. Shared sprint_timer should not overwrite unrelated sprint weapon/trinket state.

Legacy leaves and source pool indices (zero-based pool/entry, recursive paths) follow. The modern Rocket spectral boots craft (Hermes current jumpboost boots +reactive craftusable boots) is existing separate functionality; do not alias legacy reactive_old boots or consume them in that recipe. Groundcraft creates spectral leaf,100 green dust FORCE +enchantment_table.use1/1, consumes one matched pair; existing native recipe must remain unchanged. No dedicated new TNTleggings recipe/source leaf was found; declaration exists only in jump handler.

- `legacy_luckyblock.json` `root.pools[0].entries[63]`: base `minecraft:leather_leggings`, flags `{jumpboost_old:1}`

```json
{"type":"minecraft:item","name":"minecraft:leather_leggings","functions":[{"function":"minecraft:set_count","count":1},{"function":"minecraft:set_components","components":{"minecraft:dyed_color":655104,"tooltip_display":{"hidden_components":["minecraft:dyed_color"]}}},{"function":"minecraft:set_name","name":[{"translate":"frog_leggings","color":"green","italic":false}]},{"function":"minecraft:set_lore","mode":"replace_all","lore":[{"translate":"frog_leggings_old.lore","color":"#FFBFBF","italic":false}]},{"function":"minecraft:set_custom_data","tag":"{jumpboost_old:1}"}]}
```

- `legacy_luckyblock.json` `root.pools[0].entries[80]`: base `minecraft:iron_boots`, flags `{reactive_old:1}`

```json
{"type":"minecraft:item","name":"minecraft:iron_boots","functions":[{"function":"minecraft:set_count","count":1},{"function":"minecraft:set_components","components":{"minecraft:item_model":"lbc:reactive"}},{"function":"minecraft:set_name","name":[{"translate":"reactive_boots","color":"gold","italic":false}]},{"function":"minecraft:set_lore","mode":"replace_all","lore":[{"translate":"reactive_boots_old.lore.1","color":"white","italic":false},{"translate":"reactive_boots_old.lore.2","color":"white","italic":false}]},{"function":"minecraft:set_custom_data","tag":"{reactive_old:1}"}]}
```

- `moisei_boots.json` `root.pools[0].entries[0]`: base `minecraft:leather_boots`, flags `{transformable:1,boots:1,moisei:1}`
- `neko_set_traveller/helmet.json` `root.pools[0].entries[0]`: base `minecraft:leather_helmet`, flags `{transformable:1,helmet:1,jumpboost:1}`
- `neko_set_traveller/chestplate.json` `root.pools[0].entries[0]`: base `minecraft:leather_chestplate`, flags `{transformable:1,chestplate:1,jumpboost:1}`
- `neko_set_traveller/leggings.json` `root.pools[0].entries[0]`: base `minecraft:leather_leggings`, flags `{transformable:1,leggings:1,jumpboost:1}`
- `neko_set_traveller/boots.json` `root.pools[0].entries[0]`: base `minecraft:leather_boots`, flags `{craftusable:1,transformable:1,boots:1,jumpboost:1}`
- `neko_set_techno/helmet.json` `root.pools[0].entries[0]`: base `minecraft:leather_helmet`, flags `{craftusable:1,transformable:1,helmet:1,techno:1}`
- `neko_set_techno/chestplate.json` `root.pools[0].entries[0]`: base `minecraft:leather_chestplate`, flags `{craftusable:1,transformable:1,chestplate:1,techno:1}`
- `neko_set_techno/leggings.json` `root.pools[0].entries[0]`: base `minecraft:leather_leggings`, flags `{craftusable:1,transformable:1,leggings:1,techno:1}`
- `neko_set_techno/boots.json` `root.pools[0].entries[0]`: base `minecraft:leather_boots`, flags `{craftusable:1,transformable:1,boots:1,techno:1}`
- `neko_set_technomk2/helmet.json` `root.pools[0].entries[0]`: base `minecraft:leather_helmet`, flags `{transformable:1,helmet:1,techno:1,reactive:1,technomk2:1}`
- `neko_set_technomk2/chestplate.json` `root.pools[0].entries[0]`: base `minecraft:leather_chestplate`, flags `{transformable:1,chestplate:1,techno:1,reactive:1,technomk2:1}`
- `neko_set_technomk2/leggings.json` `root.pools[0].entries[0]`: base `minecraft:leather_leggings`, flags `{transformable:1,leggings:1,techno:1,reactive:1,technomk2:1}`
- `neko_set_technomk2/boots.json` `root.pools[0].entries[0]`: base `minecraft:leather_boots`, flags `{transformable:1,boots:1,technomk2:1,techno:1}`

Current/old equivalence boundary: legacy jumpboost_old boots are not the current four-piece Traveller set. reactive_old boots jump only levitate and emit flame; modern reactive Rocket machinery remains qualified. Techmk2 overlaps techno flags but Wyvern excludes techmk2 FEET; nitro does not. TNTleggings noleaf is an acquisition blocker for claiming a complete newly available family. Moisei extreme TNT and speed continuation are literal suspicious behaviors to defer, rather than quietly cap; whole6-family batch is not yet no-question ready. Traveller alone is complete bounded finite source closure.

## Whole-flag search correction: additional per-piece passives

A full source function scan for jumptnt/reactive_old/jumpboost_old/moisei/jumpboost/techno (excluding catalogue generators) completed. Current Traveller extra matches are the chest/helmet/fullset callbacks already detailed; its boots/legs remaining bonuses are exact loot attributes, not hidden jump helpers. Moisei extra matches are source ultra-reforge rule, no additional passive. Jumptnt still has no source item leaf. **Old reactive boots are NOT jump-only**: `other/new_game/legacy_tick_player:2-3` while feet are specifically minecraft:iron_boots with reactive_old1, emits1 forced CLOUD atY-.2 spread.2/.02/.2 speed.01 and grants Slow Falling20ticks amp0 hidden EACH legacy tick. legacy_tick:9 iterates nonspectator players, self-schedules1tick; no local stopper/magic/TimeStop guard. This loop is legacy-mode service, not proof ordinary mode grants it. legacy_tick_1s_player:3 oldjumpboost grants20ticks amp3 per legacysecond. Preserve mode scope and native TimeStop policy; current reactive Rocket passives remain separate.

Techno WHOLE pieces have more behavior than Wyvern/nitro; do not mark full armor items migrated from movement actions alone:

- changed_inventory/sel:14/28 headtechno toggles techno_active; :49/56 anyarmortechno toggles techno_tick_active; :89/102 feettechno toggles techno_flame_active; :109-116 fourtechno and fewerthan4techmk2 toggles techno_set_bonus_active. Inventory refresh/reset routes recalculate these.
- players_1s:38 headactive/no stopper20 -> armor/powerhelmet. Helper globally iterates nonspectator players distance>=1, creates observer-local3Y facing direction, then powerhelmet_particle (directional detection visual); this is a separate head visual closure, not nitro.
- players_tick:42 anytechnoactive/no stopper20 -> armor/techno_sel; headtechno -> powerhelmettick:1-3 per-tick green entity_effect particles at EVERY nonplayer#mobs (NO range) and nonspectator distant players (NO max), shown only toowner, then tagtickactive. technomk2boots dispatch is separate existing overlapping set.
- players_2s:1 chesttechno50% chance/no stopper20 -> other/powerhelmet:1-3 self Resistance40ticks amp4 hidden, electric_spark50 normal Y1 spread.5/1/.5 speed.1, beacon.activate globally1/1. Source functionname powerhelmet actually gates CHEST.
- players_5t:2 feetflameactive/no stopper20 -> soul_fire_flame2 normal ownerfeet spread.3/.1/.3 speed.01.
- players_10t:1 fullsetactive while ownercell literal WATER/no stopper20 -> armor/techno_set_bonus: at #mobs radius10 !spectator standing in literalwater calls techno_set_bonus1; twinkle globally1/1. This reaches shared electric damage and is not merely a cosmetic boot callback.
- items/damaged:11 fullsetactive/no stopper20 -> **lbcr:raycast_preloop10_setbonus**, three source owned15tick raycastmolnia mecha/player shield_blockable markers and shared electrical beam closure. This is a separate incoming action and must retain existing electrical attribution/friendly-fire rules; it was not newly qualified by this movement audit.

Source additional Techno crafting routes combine corresponding reactive+techno armor pieces with equippable asset_id steampunk/techno into technomk2 via other/craft/cyberpunk_{piece} ground/inventory routes. Reforge rules exist independently. These matches prove full6-family/allarmor implementation would require broader electrical/head visual/set/craft closure; **bounded recommendation remains complete Traveller +oldreactive or standalone Moisei only after suspicious explosion continuation choices**, not blanket complete Techno armor. Specific jump-action contract can still be implemented with clearly stated partial armor boundary.

Additional whole-flag source evidence SHA256:

- `function/other/new_game/legacy_tick_player.mcfunction` `5d8424300a59f95feafe7e66efa3c9e9213942ebeee0ae9cd6d7b3d0bc5b73de`
- `function/other/new_game/legacy_tick_1s_player.mcfunction` `b787e6eb96cbd7c24434aaf78492586e55ed0aad5fc643d23e6213dd7ddca6e4`
- `function/other/new_game/legacy_tick.mcfunction` `1411a6787928e33cd0b60211fbbc1749c728cbc6d91825e4e57179d4abd77784`
- `function/items/changed_inventory/sel.mcfunction` `0b9c6a85afe888ab288ccb32822c7aece0cb4903c2ebbee37bf24e48ea99e7d5`
- `function/players_2s.mcfunction` `abdb09f5ca07c52fa5adf335311d011d8ef3f643242d17d41905db521d9a1a5e`
- `function/players_5t.mcfunction` `6ebbf9d5b67e221f0cec67fee494963e9d2b22c11830370f1249274ab224d1c1`
- `function/players_10t.mcfunction` `0d43573e79c672783938b72793c5f5c912a4f5dbcdb691c05fc0de4fbf80795e`
- `function/armor/techno_sel.mcfunction` `400f0f67ea35cca4b92b97397527c585b77fffc98ae38570a6f274b2c4624721`
- `function/armor/powerhelmet.mcfunction` `18d4e46d7018b623a14c021b848864ff06f06d1ebbef923c81b85ee17b943153`
- `function/armor/powerhelmettick.mcfunction` `211779cc52799f19c54fe5ebac4fab423baa269974a868850cfedc12e5763ecf`
- `function/other/powerhelmet.mcfunction` `4b4924e8d367c30f7db258e9bc254100b3157c4297277f0b1c5efc913471f13e`
- `function/armor/techno_set_bonus.mcfunction` `c3fa85e7a470de061bd8f09ae9f121b12cdaddec2b7e8270bc7169fea9cc089b`

## Final bounded batch boundary

Complete source closure is available for **old reactive boots** (jump grant plus legacy-only continuous cloud/SlowFall), **current four Traveller pieces/set** (all per-piece/set/equipment attributes, oldjumpboost mode variant explicitly separate), and **Moisei Boots** (all sprint tiers/dirt/TNT/resource/acquisition and separate kit7 dirt). No additional inventory passive for these flags was found in whole-function scan. Do not activate full artifact/armor random pools or Techno leaves. Native kit7's existing1024 dirt grant and gate are an ancillary already-live source route, NOT proof Moisei Boots themselves are granted.

**TNT leggings acquisition remains unresolved**: literal jumptnt appears in source jump dispatch/helper but no loot leaf, modifier, advancement, craft or RP model defining a real leggings stack was found. A native handler may recognize an externally supplied custom_data jumptnt stack; inventing its base/durability/name/lore/attributes would require author input. Do not call a new concrete TNTleggings item source-exact without that missing definition.

**Explicit deferred source choices**: Moisei10TNT EVERYsprinttick651+ and speedgrant ending after650, global shared sprint accumulation while unequipped, extreme shared sprintend audio100000; preserve-versus-change requires pending author note under suspicious-source policy. Source oldreactive continuous behavior runs only legacy service; ordinary-mode extension would be a design change. Traveller finite set has no HurtTime ambiguity or new friendly-fire requirement; its source water_breathing cleanse oddity is literal existing Popit behavior and should reuse shared rules. Native TimeStop/roles/ally/resident/attribution are already-authorized precedence, not new blocks. Techno remains SOURCE-ONLY until complete external electrical/head/chest/set/craft qualification; no partial item leaves should be enabled from this note.

Existing author RP relevant model/item references were checked read-only below; actual new template need is none. All exact equipment component asset_ids/model refs remain source JSON authority.

- `assets/lbc/items/armors/moisei_boots.json` SHA256 `9f1643c60669f7c21c4ae2c8113f93564922cda7563c842a352e6fa82032501a`
- `assets/lbc/models/item/armors/moisei_boots.json` SHA256 `5aea6c2b565eca484621732ad6691c62820743ae9f4a7a3459c2a8bd7e1fff0b`
- `assets/lbc/items/armors/traveller_helmet.json` SHA256 `2f76609f93996d040eee06e0c7cd4d07159e01e155dc87050c98bfbd82f6a93d`
- `assets/lbc/models/item/armors/traveller_helmet.json` SHA256 `c7ef8a51233ea37f82162c77c4bfa20951b4a60dbc483fe74a878174d3fab16b`
- `assets/lbc/items/armors/traveller_chestplate.json` SHA256 `876236b50f7f9ec5ab57a638f7f26df24445b598c73e0194ad1b254e6c1c40c9`
- `assets/lbc/models/item/armors/traveller_chestplate.json` SHA256 `d6e841c0bf5c9c721aa7cc031b482ca17aa9618706a06f4701098970f2cb7a65`
- `assets/lbc/items/armors/hermes_boots.json` SHA256 `ab249f1e1c3c729fc4e4360995fd5849608bdac571bd8213a3398e365a9c5128`
- `assets/lbc/models/item/armors/hermes_boots.json` SHA256 `8587b493bf9e495f7a34d208661ce4eac8a254ec4324d81085b0821ee232eb33`
- `assets/lbc/items/armors/reactive.json` SHA256 `f9c505699c0e5fe9e80877c6b662e513d2029a13d4dd3b8d3c9304d8241c35e4`
- `assets/lbc/models/item/armors/reactive.json` SHA256 `644b7371c1486fcbd7894a811f3637fa4377d59c546eef82f311ffb5fb8ec250`
