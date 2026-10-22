# Small special-drop batch — corrected source shortlist

22 October2026. Source evidence for the combined implementation; current status and qualification: [batch contract](SMALL_DROPS_BATCH_PORT.md).
Source S: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`.

Final selected scope: OP Anvil normal/old plus THREE additional small mechanics:
Yamete feedback, Death from Lust, Aggressive Slab normal/old. Six public leaves,
without whole pools. Old Vex below is a researched exclusion from this batch.
Prior tentative Obsidian/clone shortlist is withdrawn: Obsidian is already a
registered cage kind; normal clone is29,791blocks and unsuitable for a small batch.

| Mechanic | Exact source | Complete dependency closure |
| --- | --- | --- |
| Yamete feedback | specialdrops:50 -> drops/yameteluckyblock:1–3 | two particle packets, one existing RP sound; no actor/reward |
| Death from Lust | specialdrops:81 -> drops/death_lust:1 -> death_lust_at:1–9 | existing native LbcPsycho twitching plus title/sound/chat; no reward |
| Slab normal/old | specialdrops:54–55, one summon each | native baby zombies and native attribute-only slab rewards |
| Old Vex optional | specialdrops:49 -> drops/vex_old:1–9 | five native1HP vex, ordinary AI/gear/loot defaults |

## Yamete feedback

At token fractional XYZ: falling_water normal delta(.5,.5,.5),speed1,count100;
effect normal same delta/speed,count10; minecraft:lbcsounds.yametekudasai MASTER
@a volume1,pitch1. Preserve standard particle audience/range and positional sound;
no invented nearest target, status effect, reward or duration.

Token source is ONLY second pool of `loot_table/yameteluckyblock.json:140–182`:
count-default1 repeating_command_block, drop_not_trap model, yellow nonitalic
`drop_yamete_kudasai`, white nonitalic trap_desc lore, `{drop:1,yameteluckyblock:1}`.
No tier is authored on that token. First pool in that table is a separate custom
Lucky Block with large authored lore: do not expose/copy the entire two-pool table
as the standalone feedback token. Isolate token entry into its native leaf.

## Death from Lust

Selector at token: nearest @p[gamemode=!spectator,tag=!sin_greed_active,
nbt=!{Dimension:"minecraft:nexus"}], then execute at target fractional XYZ.
The title JSON decodes to one U+E203 glyph, sent twice. The function also has a
commented-out kill (DO NOT add lethal damage), twitching+=1000, broadcast selector
plus dark_red died_from_cum, and target-only lbcsounds.ultrascream MASTER
volume10000000,pitch1. No radius/enemy/line-of-sight condition or reward.

`LbcPsycho.java:325–329` already exposes afflict(entity,owner,scared,lookback,
twitching,piss). It currently invokes rejectCustomEffect for players; verify that
standing native protections apply without silently changing feedback eligibility.
Its existing action/count decrement/time-stop/epoch pipeline and actual-owner
attribution should be reused. Source says score addition, not1000ticks duration.
S `loot_table/lust_death.json:1–52`: count1 repeating_command_block, tier_ff,
drop_trap, yellow nonitalic trap_died_from_cum, white nonitalic trap_desc,
{death_from_lust:1,drop:1}. No custom nested reward dependency.

Volume10000000 and duplicate literal title are explicit source commands. Preserve
the authored values; no new author question is required for this batch.

## Aggressive Slab pair

Both spawn at token fractional XYZ. Native invisible baby zombie, empty death
loot, head oak_slab count1, head drop chance1; invisibility2,000,000ticks,
amplifier1,particles hidden. Native melee/AI/health/default gear behavior remain.
Normal explicitly PersistenceRequired1; old omits it. Normal slab reward:
nonitalic translated agressive_slab, tier_d, mainhand attack_damage add_value6,
id minecraft:base_attack_damage. Old reward: literal nonitalic Russian
`Агрессивный полублок`, no tier, same mainhand modifier but amount3. No
custom_data/ability or recurring slab-specific function was found. Head-slot
modifier does not boost zombie melee because its slot is mainhand. Preserve native
equipment death/drop/looting and inspect conversion/epoch cleanup.

Tokens: `loot_table/traps.json:622–665` normal count1 repeating_command_block,
weight10 quality5, tier_ff/drop_boss, yellow nonitalic trap_slab, white nonitalic
trap_desc, {slab:1,drop:1}. Old `legacy_luckyblock.json:2751–2770` plain barrier,
count1, empty set_components, {slab_old:1,drop:1}. No shared reward resource needed.

## Optional Old Vex

Five summons local ^ ^ ^3, interleaved four executor yaw+72 teleports. Executor
is the item, not a selected player. Native vex health1/max_health1, no custom
loot/persistence/lifetime authored. Preserve initial pitch/yaw/local coordinates,
native AI/gear/default death behavior; add actual-owner/roles/epoch cleanup.
Token `legacy_luckyblock.json:1506–1523`: plain barrier,count1,flags
{vex_old:1,drop:1}. Normal ten-vex branch remains excluded: vex_sword -> vexsword
custom magic-sword mechanics. A bounded Java search found NO vexsword flag support;
LbcSummons SoulVex/psychovex is a different family, not that sword's completion.

## Current read-only RP evidence

Root `C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3/minecraft/resourcepacks/all_in_one_1.21.8`.
`assets/minecraft/lang/{ru_ru,en_us}.json` existing keys:

| Lines/key | RU | EN |
| --- | --- | --- |
|2624 drop_yamete_kudasai |Дроп: Yamete Kudasai~~ |Drop: Yamete Kudasai~~ |
|5228 died_from_cum |` обдрочился` |` died because of lust` |
|5229 trap_died_from_cum |Смерть от перевозбуждения \| monster148869 \| Halloween 2025 box reward |Death from Lust \| monster148869 \| Halloween 2025 box reward |
|2599 trap_slab |Ловушка: Агрессивный Полублок |Trap: Aggressive Slab |
|4277 agressive_slab |Агрессивный Полублок |Agressive slab |

`assets/minecraft/sounds.json:2` defines lbcsounds.yametekudasai; :929 defines
lbcsounds.ultrascream. Reuse actual models/styles/keys, no translations/RP writes.

The selected six leaves are implemented together; see the batch contract for evidence and remaining limits.

## Item/resource implementation — qualification pending

Prepared `LbcSlabDropItem`: Kind.NORMAL/OLD, item()/flag(), register(),
matches(stack,kind), matches(stack), kind(stack). Flags slab/slab_old +drop1 are
matched on any nonempty stack. `LbcFeedbackDropItem` exposes the same API with
Kind.YAMETE/DEATH_LUST, flags yameteluckyblock/death_from_lust +drop1. Each match
is independent; combined flags retain source-dispatch compatibility.
DefaultItemComponentEvents sets exact models after Polymer defaults. Both normal
custom names/lore preserve yellow/white and italic:false; old slab is plain barrier.
Yamete intentionally has no tooltip tier. Native IDs/public leaf basenames:
slab_drop, slab_drop_old, yamete_drop, death_lust_drop (skywars2 items,
lbc:ported loot leaves). All six leaves are registered and explicitly whitelisted together with the two internal OP book tables. RP/template stay unchanged. Runtime evidence is recorded in the batch contract.
