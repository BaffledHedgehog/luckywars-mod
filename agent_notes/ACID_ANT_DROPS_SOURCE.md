# Acid Burst / Ant Swarm — source and resources, qualification pending

22 October2026. Item/resources prepared; root owns runtime/build/qualification.
Source S:
`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`.
Current AGENTS/read-only RESOURCE_PACK requirements apply; no RP/template writes.

## Tokens and reward API

LbcAntDropItem.Kind.ACID_BURST = skywars2:acid_burst_drop,flag acidboom;
Kind.ANTS = skywars2:ant_swarm_drop,flag muravei. Both expose item()/flag();
register(),matches(stack,kind),matches(stack),kind(stack) preserve nonempty
sourceflag+drop1 compatibility. Both tier_ff; yellow/nonitalic translated names,
one white/nonitalic trap_desc lore. DefaultItemComponentEvents overrides models
following Polymer defaults. Public leaves lbc:ported/acid_burst_drop and
ant_swarm_drop each yield exactly one native token.

Burst presentation from S loot_table/muravei.json:1–49: repeating_command_block,
count1,tier_ff,model lbc:muravei,name drop_acid,{acidboom:1,drop:1}.
Swarm presentation from loliluckyblock.json:1228–1282: repeating_command_block,
count1,weight10,tier_ff,model drop_boss,name drop_muravei,{muravei:1,drop:1}.
Original pool condition inverts this-entity Inventory containing zaika1. That
belongs to pool acquisition; do not invent a runtime holder restriction or open
whole loliluckyblock pool merely to expose a standalone native token.

Internal lbc:ant_rewards has two independent source pools: guaranteed native
burst token, then10% chance for ONE working LbcWands.ACID_WAND. Reuse nested
lbc:ported/acid_wand so exact source tooltip/name/lore/custom_data/modifiers
remain the established native wand representation; do not grant bare native
item prototype or inert carrot-stick. Native26.3 LootPool source CODEC confirms
singular condition/type and modifier fields. Existing acid_wand leaf already
uses native skywars2:acid_wand and current-schema source modifiers. Root must
whitelist ant_rewards plus both public leaves; source muravei table's outer
condition/chance is retained without copying unrelated whole pools.

## Burst exact payload

S specialdrops:74 -> drops/acid_boom:1–6 at tokenY+1: three acid markers at
additional .01/.02/.03Y, data.lifetime500,tags raycastacid/player/slowraycast/magic.
Nearby≤.1 unactivated three receive independent random_rot then actived.
random_rot:1–2 uses yaw−180000..180000 and pitch−90000..90000 integers scaled.001.
Spider.death MASTER@a volume2,pitch1.3. No shield_blockable2, homing IDs/team or
nogravity authored. Source raycastselector:1,12–13 counts Lifetime to500.
Current300tick shield-blockable wand helper is not this exact variant. Root owns
native existing acid-family extension and actual author/lifetime/roles/cleanup.

## Swarm exact payload and closed death rewards

S specialdrops:73 -> drops/muravei:1–5: five identical native baby zombies,
silent,persistent10HP/max10,head repeating_command_block modelmuravei/unbreakable,
head drop0,infinite Invisibility1 hidden particles,tags muravei/lucky_mob.
Actor name translate muravei with ALL styling unspecified: do not add italic:false
or a color. Attributes follow_range50,movement_speed.25,attack_damage3,armor5,
armor_toughness5,spawn_reinforcements100; native range clamping/defaults apply.

mobs_tick:6 -> mobs_tick_lbc:8 -> muravei_tick:1–2: chance.00125 bat.takeoff
volume1,pitch1.6; independent chance.03 aimed acid toward nearest eligible mob
within10 excluding spectator/muravei/strict_map_object. Anchored eyes,faces eyes.
muravei_acid:1–6 creates300tick shield_blockable2 acid,sets lbcID2−100,
universal_razbros yaw±30°/pitch±20° in.01 steps,then actived; cure sound1,pitch2.

Source DeathLootTable lbc:muravei grants guaranteed burst and10% stacid_wand.
This closure is native through new burst plus already ported REGULAR_ACID wand.
Root must qualify actual death/looting/per-actor chance, native melee/acid AI,
reinforcement descendants and conversion lineage, nearest roles/teams/WILD policy,
source owner/indirect kills, freeze/reload/epoch/stop. No fixture/runtime result
is asserted by item resource preparation.

## Read-only current RP

`C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8`.
Existing minecraft/lang/{ru_ru,rpr,en_us}.json values:

| Key | RU/RPR | EN |
| --- | --- | --- |
|drop_acid |Ловушка: Кислота |Trap: Acid |
|drop_muravei |Ловушка: Муравьи |Trap: Ants |
|muravei |Муравей. IlkaKilka \| B.L.S. Event. |Ant. IlkaKilka \| BLS Event. |
|sauvojen_wand_acid |Лахазор Ла Кадур Ха Харетз |Lahazor La Kadur Ha Haretz |

Original trap_desc spelling/styles and existing models/fonts remain; no duplicated
translation keys or template files. Three new JSON resources parse successfully.
No build/server/main/raw-source/userReroll/commit changes by this subtask.

## Danger and shared acid collision (user,22 October2026)

> Да, муравьи опасны для всех

Implementation goal: Ant target selection, melee and acid are WILD, including
thrower and allies, with common protected-role guards. This supersedes accidental
enemy-only inherited wand filtering; runtime qualification remains root work.

> Да, вернуть прохождение сквозь существ

Implementation goal applies to ALL shared acid variants, not just the new drop:
acid passes through entities while preserving block sticking and authored
lifetimes. This records the user requirement, not an already qualified result.

## Current implementation status (22 October 2026)

Acid Burst and Ant Swarm are now qualified together; the earlier source-only
status above records the audit stage. See [the tested contract](ACID_ANT_DROPS_PORT.md)
for artifact08f09e8f,136 suite results and exact native fixture limits.
Normal Fat Skeleton / normal-old portal remain [next source candidates](NEXT_DROPS_AFTER_ACID_ANT_SOURCE.md).
