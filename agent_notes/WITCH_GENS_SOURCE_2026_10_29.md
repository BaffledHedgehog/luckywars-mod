# Witch Generals source closure, 29 October 2026

Subsequent root work: [Greed/Lust passive stage and bounded native proof](WITCH_PASSIVES_PORT_2026_10_29.md).
This does not implement acquisition or the other authority/actor/checkpoint paths.
Scope correction: unbounded native `@e` in greed_tick searches loaded dimensions
globally; the historical "current-dimension" shorthand below is not a world limit.

Audit only; no native implementation, gameplay qualification or producer readiness claim. Original full25 Chaos pool and full dependency release gate remain. Existing author paths, overrides, resource pack and source data are read-only.

Source root: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/`. Function paths below are relative to `lbc/function/`; reward paths to `lbc/loot_table/`. This audit covers the shared acquisition and seven authorities. Detailed whale/snake/rabbit/invisible-hand actor algorithms belong to the separate actor audit and are not replaced by generic actors here.

## Acquisition and exact mode map

`lucky_items.json` and `lucky_items_high_quality.json` reference `witch_gens.json`. The latter emits one carrot_on_a_stick, tier_q, model lbc:witch_gens, translated name/eight lore lines, custom data `{witch_gens:1,magicwand:1}`. Existing original translation keys/assets must be reused. All below are distinct authorities, not aliases:

| gens_type | authority/tag | acquisition condition |
|---|---|---|
| 1 | Gluttony / sin_gluttony | food_count >=64 and no nonspectator incumbent, or strictly greater food_count than the first nonspectator incumbent |
| 2 | Wrath / sin_wrath | dmg_dealt >=5000 native damage-dealt statistic units, then same strict incumbent comparison |
| 3 | Lust / sin_lust | lolidefecation >=64, then same strict incumbent comparison |
| 4 | Greed / sin_greed | greed_cursed and any Inventory item raw greed_greed:1; no incumbent or strictly greater greed_prestige |
| 5 | Vanity / sin_vanity | dmg_dealt exactly0, swrg.lives <=1, #timer lbc.math >=600 |
| 6 | Indolence / sin_indolence | sleep_time >=3600, then same strict incumbent comparison |
| 7 | Envy / sin_envy | caster lacks have_mom and every other nonspectator player has have_mom (zero others vacuously succeeds) |

`activate.mcfunction`: sneak invokes switch, otherwise check. Switch adds1 to gens_type and wraps8+ to1, sends mode translation; missing score adds from0, yielding1. Nonsneak unset/invalid type matches no branch. Already having selected sin suppresses its acquisition branch; it does not consume or punish. `yes_gens` clears exactly ONE first inventory item matching witch_gens:1 (not necessarily used hand), awards legends/witch_gens. Failed qualification invokes no_gens: native Wither10s amplifier9, wither ambient self volume.5 pitch1 and no_witch_gens text, without debit. Greed/Lust/Indolence/Vanity failures also display source counters; Indolence divides copied sleep score by20, Vanity dmg_dealt by10 using scoreboard integer division.

All seven success paths remove their authority tag from ALL players including spectators, then assign caster; other authority tags are retained, so one player can hold several sins. Greed additionally removes all sin_greed_active tags and caster greed_cursed; Indolence removes all indolence_active tags. Award corresponding legends/sins advancement. Gluttony emits all3 `glutton_summons` pools before success debit: whale magicwand+cooldown flags, rabbit raycastspell+cooldown flags, snake raycastspell. Lust emits `lust_get`: model metamorph_flesh, magicwand/lust_cooldown/spellwithcooldown. Indolence emits `indolence_claw`: invisible_hand model, magicwand/indolence/spellwithcooldown. Its initial skeleton creation is delegated to actor audit, not optional.

## Parent admission, cadence and dispatch

`items/spells` main/offhand magicwand calls deny caster nomagic_active or nonspectator nomagic_active player within20. Main `spell1` sets caster/raycaster/Distance around default dispatch; offhand `spell1_left` chooses default/upgraded separately. `spell1_default:53` and `spell1_default_left:40` dispatch witch_gens without local stopper_magic test. Therefore do not blanket-apply beacon suppression to acquisition solely because summoned actors use it. No TECH beacon gate appears here.

Whale, flesh and claw default use paths have explicit stopper_magic within20 suppression; whale additionally denies an already present white_whale ghast in current dimension. Main claw requires sin_indolence, while offhand claw line43 omits that tag requirement. Flesh requires sin_lust. Rabbit/snake are raycastspell routes and must retain their own source selectors/cooldowns (actor audit).

`players_tick:27..29` invokes Greed, Envy, Wrath respectively. `mobs_tick` restricts players to gamemode!=spectator and !time_stopped; time_stopped_tickentity also only ticks unstopped players. No local MAG-player/beacon test is in these three passives. `players_1s:33..35` applies Greed nearby-heart routine, Lust Regeneration1s amp4 hidden, Envy effect routine; mobs_1s dispatch lacks the same player time_stopped exclusion. This legacy pause mismatch needs an explicit project-policy decision; no private duplicate scheduler is justified.

`items/swords:30` invokes Lust hit propagation only for sin_lust and no nearby nonspectator nomagic_active player or stopper_magic marker within20. `items/on_falling` gives sin_lust SlowFalling1s amp0 hidden at falltimer>=10 absent stopper_magic. Respawn and on_death are separate required hooks, below.

## Greed shared authority and existing guard overlap

`LbcGreed` already owns active-tag absolute immunity/native projectile reflection and manual protected-wand collision. Its header explicitly excludes acquisition/activation loop. Preserve this common authority; never duplicate reflection, electrical owner assignment or damage admission in the new service.

`greed_tick`: active iff ANY current-dimension entity has sin_greed_have_heart but lacks sin_greed and spectator tags, with no distance limit. Active grants hidden Resistance2s amp4 and InstantHealth2 native ticks amp4; adds attack_damage +10 and knockback_resistance +10000 ADD_VALUE, both ID `minecraft:e916adfe-014b-4ff5-82f1-af6ec9226d21`. Native duplicate add fails harmlessly; inactive removes those IDs. Destroys source two-block-ish column fill from relative y.99 to1.01 (native floor coordinates); below source y volume -30 downward teleports to y322. Uses actual destroy block mode, not radius approximation.

Exact active clears: bad_omen,blindness,conduit_power,darkness,raid_omen,fire_resistance,glowing,raid_omen again,hunger,instant_damage,invisibility,levitation ONLY coldownshish<=20,mining_fatigue,nausea,poison,slow_falling,slowness,unluck,water_breathing,weakness,wither. Sets lookback/scared/shock/twitching raw counters0 and needs shared owners reconciled.

Per second choose first nonplayer #mobs within5 without heart, 40% chance; separately first nonspectator player within5 without sin_greed/heart, predicate chance0_015625 = actual JSON probability0.00015625 (0.015625%, not probability0.015625). On success emit10 hearts/20 souls and enchantment_table.use global sound; PersistenceRequired1 and heart tag. Soul particles around nearby heart mobs target all sin_greed players. Existing raw greed_prestige is updated by `other/greed/pickup` (+1), not by this heartbeat.

## Envy exact copying

Each tick chooses source nearest nonspectator !sin_envy player (`@p`, current dimension), not globally random or hostile-only. Copy maximum-valued beneficial scores, minimum-valued negative/cooldown scores, then copy ONLY EMPTY slots: all27 main inventory slots9..35, hotbar0..8, offhand, four armor slots. This duplicates items without stealing. A successful copy must enter normal native inventory/equipment callbacks; no fake item model or readiness filtering.

Each second conditionally grants effects based on presence on that same nearest player, with fixed source amplitudes rather than copying actual amplifier/duration: Speed2s/2,Haste2s/127,Strength2s/9,InstantHealth2ticks/99,Jump2s/2,Regen2s/20,Resistance2s/4,FireRes2s/0,Invisibility2s/1 hidden,NightVision25s/0,HealthBoost2s/9,Absorption2s/9,Saturation2ticks/100,Luck2s/9,SlowFalling2s/0,Conduit2s/2. Clears slowness,fatigue,instant_damage,nausea,blindness,hunger,weakness,poison,wither,water_breathing,glowing,unluck,darkness when source LACKS them. WaterBreathing is deliberately clear-only; no invented counterpart grant. Use approved ItemEffectLevels where applicable. Missing source means predicates/score commands fail according to native selectors; do not invent nearest fallback across dimensions.

## Wrath health authority

Tick delegates nonplayer #mobs not yet affected to `lbcr:podchinenie`, tags ALL nonplayer #mobs affected_by_wrath in dimension, and attempts player conversion for first-time targets at .01..30 with chance1. Conversion is a separate actual source dependency; current common native actor owner machinery needs explicit policy/target bridge, never a fake player. Every nonplayer #mob has max-health base and Health copied from nearest sin_wrath player; Sauvojen boss exempt from both writes.

Affected nonspectator players except sin_wrath/sin_greed_active are processed at their locations, with executing @s still Wrath owner. They get owner's cached effective MaxHealth through existing max_health_update; health-level comparison uses hplvl. If target hplvl lower, native InstantHealth1tick/99; if higher, source stores integer Health(owner)-Health(target), absolute value, and damages target outside_border unless void_resistance, clearing HealthBoost afterward. Exact native command integer result matters. `items/on_death:2` kills every affected_by_wrath entity except spectator/sin_wrath/sin_greed_active/strict_map_object. No source team filter exists; current causal policy must be explicit.

## Lust authority, distinct from named human_flesh food

Successful acquisition depends on mined mushroom_stem statistic lolidefecation, not eating human flesh. `lust_damage` uses team_number temp and same_team predicate: for each recently HurtTime10 target .01..6 (not lust_dragon/spectator/ally), first increases CASTER base MaxHP using `execute at` (executor unchanged), then reduces target MaxHP by2; base<=0 kills unless void_resistance or active Greed. Ground+sneak flesh similarly caster+8 per target, target-8, radius20, cooldown30. The two mutation passes use actual native attributes/shared caps; not generic damage/lifesteal.

Flesh (`lust_cooldown:1`): sneak is suppressed if lust_cd positive; nonsneak dispatch ground or air, with airborne lust_cd_small check. Ground nonsneak swaps with one random #mobs entity excluding spectator and temporary owner, via source local @e; do not reuse named-food human_flesh global @a crossdimension selector. Air nonsneak grants hidden Resistance1s/4, actual EnderDragon20HP at local forward3, native source forward2 velocity, follow_range200, mounts wearer, small cooldown10; summon conversion and 5tick dragon disposal require complete actor ownership. Air+sneak invokes actual random_mob_spawn TWENTY times then Levitation1s/20, cooldown10; no filtered summon table. Ground+sneak behavior above. Dragon5t kills when any nonspectator sin_lust sneaks OR no nonspectator sin_lust within20. This is a separate actor dependency not covered by whale/snake/rabbit/claw audit unless explicitly assigned.

## Vanity checkpoint and respawn

Acquisition consumes before checkpoint capture, removes previous sin_vanity, adds current tag, writes41 inventory slots into two far-coordinate yellow shulkers (27 main,9 hotbar,offhand,4armor), and stores selected scores plus active_effects,Tags,attributes,Pos in a singleton stored_stats_vanity marker. No Dimension, yaw/pitch, health, XP, food, native stat map or private service maps are stored. Native scoped memory can replace storage implementation but cannot silently widen restored state or lose shared score-owner adoption.

Respawn restores41 slots and selected scores, attribute BASE values (no modifiers), potion effects through native effect command, then remove_tags plus restored tags; teleports to captured Pos in CURRENT execution dimension, places one stone y-2 replacing #minecraft:airs strict, sets swrg.lives2, plays lbcsounds.vanity_respawn self volume1 pitch1 minVolume1, schedules1s delayed selected sin_vanity player replay of position+stone. Original shared respawn then also applies other kit/spawn/item consequences; hook ordering is material.

Hard-coded loops visit attribute indexes0..27, effect0..32, tags0..201 irrespective actual array lengths. Missing data commands may retain prior lbc.math macro values, so they can replay a preceding entry. Finite effect duration is command-result floor(duration*.05), then native seconds; instant effects also undergo this conversion although effect give interprets argument as ticks. Infinite uses string infinite; zero duration intentionally invalidates effect ID. Hide-particle flags and hidden effect chains are not faithfully serialized by replay. These are pending source quirks, not license to silently replace with an unbounded exact Java snapshot. Existing multiply_5 kit additionally calls vanity_respawn_return_effects_pre_x5; keep its distinct x5 effect semantics outside ordinary Vanity restoration.

## Indolence lifecycle boundary

Acquisition/get spawns up to20 skeleton fingers at random existing NONPLAYER entities in execution dimension, applies lbcr:podchinenie around every skeleton finger, emits claw. Detailed models/native AI/equipment and actor count are delegated. On death active Indolence must survive common lifecycle clearing sufficiently to run respawn relocation to random finger, source gear/bow/64arrows/claw, kill_one_skeleton tag and5tick scheduled finger disposal. Source chest/legs/feet item modifier entries incorrectly say slot=head; do not silently repair. Ordinary sin_indolence respawn still reissues claw. Main/offhand claw gate mismatch above remains pending.

## Raw counters and existing native ownership

load defines sleep_time dummy; lolidefecation minecraft.mined:mushroom_stem; dmg_dealt minecraft.custom:damage_dealt; food_count,gens_type,greed_prestige dummy. player_speed computes hundredth-block coordinate deltas, squared sum/2 then sqrt_loop speed; removes15 from positive sleep_time then adds16 iff speed0 (net+1 stationary, not a fresh private seconds timer). Movement may make positive score negative. Source retains stored previous positions while time_stopped_lol tagged. Native mined statistics must be synchronized for lolidefecation rather than duplicating a counter per block interaction. Food count already has shared consume authority; raw KFC/NamedFoods additions remain same scoreboard.

Netherite stew sets sleep_time/lolidefecation/greed_prestige INT_MAX and other raw payload; KFC adds sleep/loli by1. Greed pickup adds prestige1. `reset` sets dmg_dealt1, food_count/loli/sleep/prestige0, adds have_mom, removes greed_cursed; does not revoke sins. Existing LbcLegacyEffects.resetWitch ALREADY implements these writes and its effect_witch_gens_reset countdown/beacon check. Reuse that owner; no duplicate reset countdown. Source reset effect also ignored under sin_greed_active.

## Proposed native contract, not implemented

A new shared authority service can own acquisition/mode switching and passives, with constructor(GameController), use(player,hand,stack), tick(), native respawn hooks and clear/forget/stop. Stack family detection must distinguish witch_gens, lust_cooldown and assigned actor flags. Shared input integration owns role/dimension/epoch/TimeStop admission; source MAG-player versus beacon distinctions above must remain visible. Source successful clear searches inventory, so hand.shrink is not equivalent. Rewards use current registered loot and native ItemEntities with wearer/epoch attribution. Persistent Vanity checkpoint and scheduled tasks carry actual epoch, owner and dimension where authorized; no global helper marker or fake owner player.

Required integration bridges: authoritative successful melee/HurtTime criterion for Lust (not prehurt speculative damage); actual native mined/damage statistics and movement speed score bridge; lifecycle before death/after respawn preserving authority tags and Vanity/Indolence ordering; existing attribute helpers with source integer quantization; existing raw owner adoption after copied/restored scores (including Blind queue), ordinary native inventory callbacks; explicit lbcr conversion bridge; actor service APIs for complete summons/claw, plus separate Lust dragon/random-mob closure. Existing LbcGreed immunity/reflection and LbcLegacyEffects reset remain sole shared engines.

## Deferred questions / release blockers

No author chat question sent. Root should retain questions about source offhand Indolence missing authority gate; unpaused1s passives versus project TimeStop; Vanity fixed-loop stale macro replay, no stored dimension/rotation and potion quantization; unintended gear modifier slot=head. No behavior may be aliased or removed from any reward pool to bypass these. Seven-authority acquisition alone is not complete Witch closure: conversion, persistent hearts/health control, full checkpoint replay and all actor/reward descendants need native proof. No builds, server runs, resource edits or existing Java edits were performed for this audit.

## Exact ordered Vanity score payload

57 source scoreboard copies, in command order:

`mana, manamax, manaregen, nitro, reactivefuel, swordcharged1, swordcharged2, swordcharged3, swordcharged4, swordcharged5, swordcharged6, swordcharging1, swordcharging2, swordcharging3, swordcharging4, swordcharging5, swordcharging6, swordcharging7, twitching, scared, shock, coldown1, coldown13, coldown16, coldown17, coldown18, coldown18b, coldown25, coldown8, coldownhohol, coldownshish, divider_cd, lookback, coldownMagnum, acid_rifle, blaze, delayed_death, end_damage, food_count, gatling_fovos, gens_type, greed_prestige, indolence_cd, lolidefecation, lucky_block_count, lust_cd, lust_cd_small, nanobots_coldown, nanobots_tier, nomagic, piss, ray, reverse_mana, screamray, sleep_time, target, technomk2shield`

## Exact Envy score directions

Copy larger: `mana, manamax, manaregen, nitro, reactivefuel, swordcharged1, swordcharged2, swordcharged3, swordcharged4, swordcharged5, swordcharged6, swordcharging1, swordcharging2, swordcharging3, swordcharging4, swordcharging5, swordcharging6, swordcharging7`.

Copy smaller: `twitching, scared, shock, coldown1, coldown13, coldown16, coldown17, coldown18, coldown18b, coldown25, coldown8, coldownhohol, coldownshish, divider_cd, lookback, coldownMagnum`.

## Original shared authority function index

- `activate.mcfunction` -> `lbc:workingitems/witch_gens/activate_switch`, `lbc:workingitems/witch_gens/activate_check`
- `activate_check.mcfunction` -> `lbc:workingitems/witch_gens/activate_vanity_check`, `lbc:workingitems/witch_gens/wrath`, `lbc:workingitems/witch_gens/activate_envy_check`, `lbc:workingitems/witch_gens/gluttony`, `lbc:workingitems/witch_gens/lust`, `lbc:workingitems/witch_gens/greed`, `lbc:workingitems/witch_gens/indolence`
- `activate_envy_check.mcfunction` -> `lbc:workingitems/witch_gens/activate_envy_check_2`, `lbc:workingitems/witch_gens/no_gens`
- `activate_envy_check_2.mcfunction` -> `lbc:workingitems/witch_gens/envy`
- `activate_switch.mcfunction`
- `activate_vanity_check.mcfunction` -> `lbc:workingitems/witch_gens/vanity`, `lbc:workingitems/witch_gens/no_gens`
- `envy.mcfunction` -> `lbc:workingitems/witch_gens/yes_gens`
- `envy_second.mcfunction`
- `envy_tick.mcfunction`
- `gluttony.mcfunction` -> `lbc:workingitems/witch_gens/gluttony_get`, `lbc:workingitems/witch_gens/no_gens`
- `gluttony_get.mcfunction` -> `lbc:workingitems/witch_gens/yes_gens`
- `greed.mcfunction` -> `lbc:workingitems/witch_gens/greed_get`, `lbc:workingitems/witch_gens/no_gens`
- `greed_apply_heart.mcfunction`
- `greed_get.mcfunction` -> `lbc:workingitems/witch_gens/yes_gens`
- `greed_second_mobs_near.mcfunction` -> `lbc:workingitems/witch_gens/greed_apply_heart`
- `greed_tick.mcfunction` -> `lbc:workingitems/witch_gens/greed_tick_active`, `lbc:workingitems/witch_gens/greed_tick_inactive`
- `greed_tick_active.mcfunction`
- `greed_tick_inactive.mcfunction`
- `indolence.mcfunction` -> `lbc:workingitems/witch_gens/indolence_get`, `lbc:workingitems/witch_gens/no_gens`
- `indolence_get.mcfunction` -> `lbc:workingitems/witch_gens/yes_gens`, `lbcr:podchinenie`
- `indolence_respawn.mcfunction` -> `lbc:workingitems/witch_gens/indolence_respawn_cheduled`
- `indolence_respawn_cheduled.mcfunction` -> `lbc:workingitems/witch_gens/indolence_respawn_cheduled_as`
- `indolence_respawn_cheduled_as.mcfunction`
- `indolence_respawn_hand_only.mcfunction`
- `lust.mcfunction` -> `lbc:workingitems/witch_gens/lust_get`, `lbc:workingitems/witch_gens/no_gens`
- `lust_damage.mcfunction` -> `lbc:other/attributes/max_health_up`, `lbc:workingitems/witch_gens/lust_damage_2`
- `lust_damage_2.mcfunction` -> `lbc:other/attributes/max_health_down_two`
- `lust_damage_2_many.mcfunction` -> `lbc:other/attributes/max_health_down_eight`
- `lust_dragon_5t.mcfunction` -> `lbc:workingitems/witch_gens/lust_dragon_kill`, `lbc:raycast_vpered_slow_dragon`
- `lust_dragon_kill.mcfunction`
- `lust_flesh.mcfunction` -> `lbc:workingitems/witch_gens/lust_flesh_sneak`, `lbc:workingitems/witch_gens/lust_flesh_not_sneak`
- `lust_flesh_not_sneak.mcfunction` -> `lbc:workingitems/witch_gens/lust_flesh_not_sneak_air`, `lbc:workingitems/witch_gens/lust_flesh_not_sneak_ground`
- `lust_flesh_not_sneak_air.mcfunction` -> `lbcr:podchinenie`
- `lust_flesh_not_sneak_ground.mcfunction`
- `lust_flesh_sneak.mcfunction` -> `lbc:workingitems/witch_gens/lust_flesh_sneak_ground`, `lbc:workingitems/witch_gens/lust_flesh_sneak_air`
- `lust_flesh_sneak_air.mcfunction` -> `lbc:other/random_mob_spawn`
- `lust_flesh_sneak_ground.mcfunction` -> `lbc:workingitems/witch_gens/lust_flesh_sneak_ground_many`, `lbc:workingitems/witch_gens/lust_damage_2_many`
- `lust_flesh_sneak_ground_many.mcfunction` -> `lbc:other/attributes/max_health_up_eight`
- `lust_get.mcfunction` -> `lbc:workingitems/witch_gens/yes_gens`
- `no_gens.mcfunction`
- `reset.mcfunction`
- `vanity.mcfunction` -> `lbc:workingitems/witch_gens/yes_gens`
- `vanity_respawn.mcfunction` -> `lbc:workingitems/witch_gens/vanity_respawn_return_attributes_pre`, `lbc:workingitems/witch_gens/vanity_respawn_return_effects_pre`, `lbc:remove_tags`, `lbc:workingitems/witch_gens/vanity_respawn_return_tags_pre`, `lbc:workingitems/witch_gens/vanity_respawn_return_pos`, `lbc:workingitems/witch_gens/vanity_respawn_1`
- `vanity_respawn_1.mcfunction` -> `lbc:workingitems/witch_gens/vanity_respawn_12`
- `vanity_respawn_12.mcfunction` -> `lbc:workingitems/witch_gens/vanity_respawn_return_pos`
- `vanity_respawn_return_attributes.mcfunction`
- `vanity_respawn_return_attributes_pre.mcfunction` -> `lbc:workingitems/witch_gens/vanity_respawn_return_attributes`, `lbc:workingitems/witch_gens/vanity_respawn_return_attributes_pre`
- `vanity_respawn_return_effects.mcfunction`
- `vanity_respawn_return_effects_pre.mcfunction` -> `lbc:workingitems/witch_gens/vanity_respawn_return_effects`, `lbc:workingitems/witch_gens/vanity_respawn_return_effects_pre`
- `vanity_respawn_return_effects_pre_x5.mcfunction` -> `lbc:workingitems/witch_gens/vanity_respawn_return_effects`, `lbc:workingitems/witch_gens/vanity_respawn_return_effects_pre_x5`
- `vanity_respawn_return_pos.mcfunction`
- `vanity_respawn_return_tags.mcfunction`
- `vanity_respawn_return_tags_pre.mcfunction` -> `lbc:workingitems/witch_gens/vanity_respawn_return_tags`, `lbc:workingitems/witch_gens/vanity_respawn_return_tags_pre`
- `wrath.mcfunction` -> `lbc:workingitems/witch_gens/wrath_get`, `lbc:workingitems/witch_gens/no_gens`
- `wrath_get.mcfunction` -> `lbc:workingitems/witch_gens/yes_gens`
- `wrath_tick.mcfunction` -> `lbcr:podchinenie`, `lbcr:podchinenie_with_players`, `lbc:workingitems/witch_gens/wrath_tick_2`, `lbc:workingitems/witch_gens/wrath_tick_edit_health_players`
- `wrath_tick_2.mcfunction`
- `wrath_tick_edit_health_players.mcfunction` -> `lbc:other/attributes/max_health_update`, `lbc:workingitems/witch_gens/wrath_tick_edit_health_players_dmg_pre2`
- `wrath_tick_edit_health_players_dmg.mcfunction`
- `wrath_tick_edit_health_players_dmg_pre.mcfunction` -> `lbc:workingitems/witch_gens/wrath_tick_edit_health_players_dmg`
- `wrath_tick_edit_health_players_dmg_pre2.mcfunction` -> `lbc:workingitems/witch_gens/wrath_tick_edit_health_players_dmg`
- `yes_gens.mcfunction`

## Actual lbcr conversion bridge detail

`lbcr/function/podchinenie` is a real local native team conversion, not a summon or arbitrary aggro switch: first team-leave nonplayer #mobs intersecting dx=dy=dz=0 at current execute position AND within15; join caster team1..32 if matched; global villager.trade sound; copy lbcID2 and team_number from caster to those entities; add friendly_tuntija to caster if converted intersection includes Sauvojen. It does not assign a projectile owner itself.

`podchinenie_with_players` also includes players, excludes spectator/cantleave targets, and temporarily makes all players cantleave for #gamemode swrg.math3..4. Team leave excludes sin_wrath; join does not have that exclusion. It tags nearest nonspectator non-Wrath within2 affected_by_wrath, copies team_number, adds friendly_tuntija as applicable, finally removes cantleave from ALL players (including potentially preexisting tags). Preserve conversion geometry and policy distinction; shared owner attribution supplements these source effects, not substitutes for native team semantics. Legacy source's global temporary-tag removal is a pending scoping quirk for the bridge.

`other/greed/pickup` consumes one greedstone, increments greed_prestige1, adds greed_cursed, displays title/subtitle, plays self enchantment sound volume1 pitch0, and calls avarice_diamond_spawn. Its upstream curse/greed_greed inventory chain is separate from heart conversion and must remain a live acquisition dependency.
