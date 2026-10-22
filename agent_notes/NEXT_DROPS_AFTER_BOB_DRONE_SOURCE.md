# Next group after old Bob / Drone — bounded closure audit

22 October2026. Source facts and next-group proposal only; no implementation or
qualification. S = `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`.
Read current registrations/source. No completed leaf is reselected, no full pool
is promoted, and no placeholder reward is proposed to increase batch size.

Recommended dependency-connected group to audit/implement next: Acid Burst plus
Ant Swarm; Random Portal normal/old can be a third independent mechanic only once
native gateway/spread and marker-scope decisions are concrete. These are several
real closures, but finite source lines do not alone qualify runtime completion.

## Acid Burst — existing native acid family can be extended

S specialdrops:74 runs at tokenY+1 -> drops/acid_boom:1–6. Three markers at
additionalY+.01,+.02,+.03; data.lifetime500, tags raycastacid/player/slowraycast/
magic. Select three unactivated nearby≤.1, run random_rot, add actived. Sound
spider.death MASTER @a volume2,pitch1.3. random_rot:1–2 chooses yaw uniformly
integer−180000..180000 scaled.001, pitch−90000..90000 scaled.001. No shield_blockable2,
no homing lbcID2/team assignment, no nogravity tag is authored on this burst.

Source raycastselector:1,12–13 increments Lifetime and kills at data.lifetime.
raycastselector_player:8 -> lbcr:raycast_acid, its normal/fire/hit subfunctions
are the established shared acid behavior. Existing native LbcClouds.AcidProjectile
and acidImpact machinery are present; however current aimedWandAcid tags
shield_blockable2 and ordinary300age budget. Blindly invoking that helper would
invent shield response and truncate this500tick variant. Add an exact variant,
not a generic damaging potion. Audit source no-owner targeting against actual
thrower/role/ally attribution before qualification. No custom reward required.

Token: S loot_table/muravei.json first pool:1–49 guarantees count1 repeating
command block,tier_ff,item_model lbc:muravei,yellow/nonitalic drop_acid,one white/
nonitalic trap_desc,{acidboom:1,drop:1}. Do not copy the entire reward table into
this standalone token leaf; its second pool independently grants a wand below.

## Ant Swarm — rewards close through Acid Burst and an already ported wand

S specialdrops:73 -> drops/muravei:1–5: FIVE identical native baby zombies at
token fractional XYZ; silent,persistent10HP/max10,infinite Invisibility1 hidden
particles; tags muravei/lucky_mob,custom_name translate muravei (style unspecified,
do not add italic:false/color). Head repeating_command_block with lbc:muravei
model/unbreakable,head drop0. Attributes follow_range50,movement_speed.25,
attack_damage3,armor5,armor_toughness5,spawn_reinforcements100. Preserve native
attribute range/load/AI behavior, not an invented100% spawn override. Native
reinforcement descendants/targeting and conversion must retain causal cleanup
where actually spawned; the high source reinforcement value is a lifecycle cost.

mobs_tick:6 -> mobs_tick_lbc:8 -> drops/muravei_tick:1–2: chance.00125 bat.takeoff
sound(volume1,pitch1.6); independent chance.03 acid shot at nearest eligible mob
within10, excluding spectator/muravei/strict_map_object. Actor anchors eyes and
faces target eyes. muravei_acid:1–6 creates300tick shared acid, shield_blockable2,
sets lbcID2−100, applies universal_razbros: yaw±30°,pitch±20° in.01 increments,
then actived. Source is not ordinary zombie melee alone.

DeathLootTable lbc:muravei has exactly two pools: guaranteed Acid Burst token,
and10% native stacid_wand count1. Existing LbcWands.ACID_WAND/REGULAR_ACID
(stacid flag) is a real port, so this is NOT an unavailable new custom ability.
Still promote the real native wand representation/formatting in an internal
reward leaf and validate use; do not leave an inert carrot-stick reward. Keep
per-death chance and both rewards independently, no whole muravei acquisition pool.

Token S loliluckyblock.json:1228–1282: repeating_command_block,count1,weight10,
tier_ff/drop_boss,yellow/nonitalic drop_muravei,white/nonitalic trap_desc,
{muravei:1,drop:1}. Entry condition: inverted this-entity NBT Inventory contains
custom_data zaika1. Preserve that acquisition condition in its actual pool; it
is not an invented runtime prohibition on a separately held token. Do not open
loliluckyblock pool just to expose the standalone native leaf.

## Random Portal — vanilla closure with explicit scope review

S specialdrops:44 -> drops/portall:1–24 creates randomportal marker and chooses
spreadplayers origin/range via no_borders and lbc:the_nether. Default center is
source/range200; bordered uses(0,0),range170; Nether uses under120. It creates
bedrockY−1/Y+4 and end_gatewayY+3 at source and destination; ExactTeleport1.
Source gateway exit is marker integer-stored Pos; destination gateway exit is
TOKEN Pos via @s. That execute-at-without-as is intentional selector semantics
for a two-way pair, not proof of a bug. Preserve native integer storage behavior,
height/neighbor updates and actual gateway teleports without command execution.

Broad randomportal selectors spread/write/kill ALL matching markers; final
source-data lookup has limit1 with default ordering. Root should resolve whether
explicit per-activation marker handles follow the existing source-workaround
migration policy or need author approval; do not silently change cross-instance
scope. Native spread may require chunk admission/height search/border handling;
this is more than six decorative blocks. No custom reward item dependency.
Tokens: barriersnottraps:490–533 normal count1,weight10quality0,tier_d/drop_not_trap,
yellow/nonitalic drop_portals +white/nonitalic trap_desc,{portall:1,drop:1};
legacy_luckyblock:2972–2989 plain barrier same flags. Keep old presentation distinct without opening either pool.

## Not small/native-closed in this audit

Greedcurse -> greedstone instant pickup -> persistent curse/diamond search,
Lucky Block replacement terrain, feather_greed/greed_greed custom rewards and
advancement. Three source setup lines are not its whole closure; do not promote
before those reward mechanics are audited.
Antigravity start -> eight functions/global gravity/player damage/marker spread;
tick_10t_marker_at macro literally executes command supplied as antigravity.
That is an old extension command dependency, not vanilla place-blocks-only logic;
inspect original Ignite implementation before choosing a native replacement.
TMT randomizes item action/NBT/storage paths, not an ordinary stick reward.
Normal Bob/Drone/Vex/FatSkeleton and infinity/forest/LuckyRoom/magicbean remain
outside this bounded complete-closure recommendation. No source question is answered by a placeholder.

Current read-only RP26.3(1) contains drop_acid EN Trap: Acid,drop_muravei EN
Trap: Ants,muravei EN Ant. IlkaKilka | BLS Event.,drop_portals EN
Drop: Two-way portal. Preserve exact existing RU/RPR/EN components when each leaf
is promoted. No new translations or RP/template writes performed here.

## Current implementation status (22 October 2026)

Acid Burst and Ant Swarm are now qualified together; the earlier source-only
status above records the audit stage. See [the tested contract](ACID_ANT_DROPS_PORT.md)
for artifact08f09e8f,136 suite results and exact native fixture limits.
Normal Fat Skeleton / normal-old portal remain [next source candidates](NEXT_DROPS_AFTER_ACID_ANT_SOURCE.md).
