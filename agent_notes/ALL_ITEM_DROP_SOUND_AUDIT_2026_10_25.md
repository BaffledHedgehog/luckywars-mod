# Drop, passive and encounter sound audit — 25 October 2026

Static source audit of the implemented drop/passive families. This is one part
of the all-item audit; active wands, projectile families, guns and their shared
status effects have a separate audit. It is not a claim of client playback.

Legacy root (read only):
`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/`.
Author resource pack (read only):
`C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8`.
The 48-file source snapshot, sound lines, 1,591 non-comment legacy sound command
lines, exact custom event definitions, referenced OGG existence and SHA256 are
recorded in ignored evidence
`build/verification/all-item-sounds/drop-passive-source-audit.json`.
The broad command index is evidence for subsequent comparisons, not a proof
that all those legacy functions are implemented or have been exercised.

## Concrete corrections

Only `src/main/java/me/skywars2/game/LbcHelicopterEncounter.java` was edited by
this audit worker, with explicit parent authorization. No build, test, server,
protected check, translation or resource-pack mutation was performed.

| Trigger | Source contract | Fault and correction |
| --- | --- | --- |
| Helicopter phase 3 cue | `drops/helicopter_boss/tick_phase_3.mcfunction:3`: `lbcsounds.drop_bomb`, MASTER, .5/1, selector150, minimum.5 | Former line197 used native1/1 and ordinary16-block delivery. Now source helper preserves .5/1,150/.5 at command origin. |
| Helicopter hurt | `drops/helicopter_boss/hitted.mcfunction:2`: blaze hurt, MASTER,1/1.5, selector150, minimum1 | Former line205 had ordinary16-block delivery and used post-move display position. Now helper uses pre-move command origin and150/1. |
| Helicopter alternating fire | `drops/helicopter_boss/shoot.mcfunction:17–18`: firework blast, MASTER,1/(2 or1.8), selector80, minimum.5, local sound coordinate `^±2 ^ ^` | Former line221 used the arrow spawn position and ordinary delivery. Shot context is pre-move origin plus victim-facing direction*8. New sound point is that context plus horizontal local-left*±2. A local packet helper keeps selector center separate from sound position. |
| Helicopter match stop | `end_resets.mcfunction:119` and encounter death lines1–2 | Previously only rewarded terminal cleanup stopped helicopter/kazahstan, so normal `stop()` left audio running. `stop()` now sends both all-category stop packets to all connected players. |

Existing corrected spawn at Java107–108 is retained: MASTER helicopter .1/1
selector20/minimum.1, kazahstan1/1 selector20/minimum.4. The new source-correct
shot helper is near Java306. The source `tick_phase_1.mcfunction:3` supplies the
`positioned ^ ^ ^8` context; its earlier `tp @s` does not relocate that command
context. The ray-facing horizontal-left axis is independent of vertical pitch.

Two further exact-position discrepancies were reported to the parent; this
worker did not edit their production files:

* `LbcAcademy.java:484` uses the BlockPos sound overload for academy token
  teleport. `drops/magic_academy_token.mcfunction:2` plays at the token's exact
  entity coordinates. Use the double-coordinate overload or source helper;
  preserve MASTER1/1 and ordinary audibility.
* `LbcUltraponos.java:75–79` plays ponos from the aligned block center. Source
  `drops/ponos.mcfunction:2` plays at exact token coordinates before any align.
  Use token.position(), MASTER1/1; helper with radius0/minimum0 also removes
  unnecessary distant packets.

## Scope and comparisons

`v/p` below means volume/pitch. A selector radius and minimum volume are
separate conditions; minimum-volume delivery relocates the packet two blocks
from the viewer toward the source. Normal audibility still limits sounds with
minimum0, even when the original command selects `@a`.

| Implemented family / Java owner | Compared legacy scope | Sound result |
| --- | --- | --- |
| Ant swarm / acid burst, `LbcAntDrops` | `drops/acid_boom:6`, `muravei_acid:1`, `muravei_tick:1` | MASTER spider death2/1.3; cure1/2; bat takeoff1/1.6 with source .00125 chance. Ordinary audibility via cloud source helper. |
| Antigravity, `LbcAntigravity` | `drops/antigravity/tick_10t_marker_at:3–4` | Both parrot fly and stone break BLOCKS1.5/1, normal range; helper retains category and source tick event. |
| Yamete / death lust, `LbcFeedbackDrops` | `drops/yameteluckyblock:3`, `death_lust_at:8` | MASTER yametekudasai1/1 to normally audible viewers; ultrascream10,000,000/1 only selected victim. |
| Pisa boats, `LbcPisaDrops` | `drops/pizanski:2`, original caller's +20Y context | MASTER wood break1/1 at raised origin, normal range. |
| Ultraponos, `LbcUltraponos` | `drops/ponos:2` | Correct event/category/v/p; exact position discrepancy reported above. Creepers are intentionally Silent in source and Java. |
| Levitation / Enzan, `LbcEnzanDrop` | `drops/levitation:3` | MASTER wool break1/1 at token exact position via native broadcast. |
| New/old scare, `LbcItems.scare` | `drops/skrimmer:1–16`, `skrimmer_old:1–15` | Victim only; three repetitions of ghast0/1 and cave0/1/2; current1 vs old100,000 volume; current only adds wither spawn1/0. |
| Academy placement, `LbcAcademy` | `drops/magic_academy_token:2` | MASTER teleport1/1 exists; exact position discrepancy above. |
| Helicopter encounter, `LbcHelicopterEncounter` | spawn, phase3, hitted, shoot, death and end_resets | Complete authored cue set traced; changes above. Reward and mechanism-removal terminal retain the original all-category stop. Unload terminal intentionally does not stop other live encounters' same event globally. |
| Stalin, `LbcStalin` | `other/stalin_shoot_pre2:1`, `stalin_shoot:7` | MASTER rasstrelat2/1 normal range; shot .5/1 selector200/minimum.05 through source helper. Both native shot branches use the same contract. |
| Transformator, `LbcTransformator` | `other/transformator:8–9`, `transformator_shoot:8`, `transformator_particle:1` | MASTER cure1/1 and2/1 with source independent health/chance gates; twinkle1/1 and creeper primed1/1. Native ordinary range. |
| KFC food and govno edible, `LbcKfcFood` | `other/eatables/kfc_chick/*`, eaten-govno hooks, `other/effect_gigaponos:3–11`, attribute feedback functions | All authored custom/vanilla cues and the20-outcome branches retained. Gigaponos rocket .3/1 selector50/minimum.3; timed ponos15/1 and shitting cues volume20/pitches.5,1,1.5 agree. |
| Cultus rewards/payloads, `LbcKfcRewards` | `other/penises/{normal,dirty,nigger,holy,horse,multicast}` | MASTER1/1 suction,dick_1,spank,woo,shityeah,weewee. Player and hostile-owner paths both preserve range and packet contract. These are payload cues, not the separate source item-summon random cue. |
| Trinkets, `LbcTrinkets` | `items/trinket_effects/charge_small_fireball:4`, `gneiss_stone_1:2`, `intercal_hit:4`, `ice_gem_1:1` | MASTER blaze shoot1/1, BLOCKS stone break1/1, MASTER wither ambient1/1, PLAYERS snow break1/1. Categories are intentional and agree with source. Snow-pearl freeze shares the Ice Sword owner. |
| Vex Sword, `LbcVexSword` | `workingitems/vexsword_arrow{r,l}:3`, `vex_ulta:6–15`, `other/phantomarrows/select:2`, `other/groundbreak{,_light}:3` | MASTER side arrows1/2, ten cave5/1, phantom arrows1/1, groundbreak explosion1/2. Source-helper normal audibility. |
| Chef/WTF, `LbcWtf`, `LbcChefSurvival` | `items/swords/wtf_item/tick:1`, `random_sound:2–7` | Six oluh events MASTER2/1, .005 probability. New native bucket and terrain/AI noises belong to accepted native Chef mechanics; they are not missing authored oluh variants. `random_sound_low` is a separate source hook, not a reason to halve the normal cue. |
| Overload movement armor, `LbcOverloadArmor` | `armor/overload/leggings/jump:1`, `boots/slam:4`, `boots/impact_2:6` | MASTER overload_jump2/1 selector40/minimum0; breeze land1/1.5 normal range; overload_impact1/1 normal range. No radius/minimum argument confusion. |
| Colors, `LbcColors` | artifact color consume/teleport hooks | MASTER burp1/1 and enderman teleport1/1. Broadcast packets currently exceed vanilla recipient culling; audible local event and values are present. |
| Rocket armor, crafting and nano, `LbcItems` | Paper `plugins/luckywars/src/main/java/me/luckywars/item/RocketArmor.java:204`; datapack rocket craft/nanobots hooks | Rocket native explode PLAYERS1.5/2 preserves Paper default category. Craft MASTER firecharge1/1 plus anvil.5/1.3; nano MASTER sniffer .6/2. Craft/nano currently overdeliver positional packets; this is not proof of distant audible sound or a missing event. |

The following snapshot owners contain no direct sound calls: BeeDrop,
CageDrops, ChestDropItem, CorrosionDrops, CreeperDrops, CreatureDrops,
DroneDrops, DropBlocks, FallingNetheriteDrops, HoleDrops, IslandDrops,
LibraryDrop, LuckyPigs, LuckyRoomDrops, OpAnvilDrop, PortalDrops,
PotionSpawnerDrops, SlabDrops, SkyHazards, SpecialDrops, SpecialEggs,
TntDrops, ZombieTntDrops, NpcDropItems, Loadouts, Skills, Mana,
ReactiveItems. These were included explicitly; no direct sound method is not
equivalent to silence: native explosions, entities, potion/item use and block
changes retain their own sounds. Ported leaf definitions do not imply that
unported broad armor/weapon systems exist. Old berserk/shulker/technomk2 and
Mellstroy casino source cues must not be reported as lost sounds of an
implemented passive without first establishing that passive's live port.
Home teleport is handled by the concurrent teleport/status audit.

## Resource and verification limits

The snapshot has30 raw custom string forms,28 canonical event IDs; helicopter
and kazahstan appear with and without their default namespace. Every event has
an existing definition in author `assets/minecraft/sounds.json`, and every
resolved file entry exists as OGG; no event recursion failure was found. This
includes six dynamic cultus payload names explicitly expanded by the audit.
Custom event/OGG presence proves availability in that local pack, not client
pack activation, decoded audio quality or in-game playback.

`LbcOverload.sound` was read: argument order is volume,pitch,selectorRadius,
minimumVolume. It checks selector range before ordinary audibility, applies
minimum relocation and sends MASTER. The scoped Overload armor, KFC, Stalin,
Vex and Chef calls do not repeat the earlier SLR radius/minimum mixup. Native
BLOCKS/PLAYERS trinket sounds correctly avoid that MASTER-only helper.

Helicopter follow-up packet checks should use real dispatch with viewers at
near,40,100,149,151 blocks for hurt/phase3; expect exact minimum relocation and
no151-block recipient. Fire tests should assert separate ±2 sound position,
pitch parity and80-block selector center at `origin+forward*8`, including an
edge viewer accepted by that center but outside80 from the sound point. End
stop checks should capture both identifier-specific packets with null category.
There is no runtime qualification in this document. `git diff --check` passed
for the worker's Helicopter edit; compilation and actual packets remain the
parent's verification responsibility.

## Joint parent qualification

Buildccb40b6c passed46 recorded results in two actual mode1 matches (88.93s).
Native packet captures cover the corrected Ender/Exahoming/Tanner/Acid Corruption/
Barrier/Clock/Helicopter cues, with the prior SLR/Magnum/spawn regressions.
Actual grimoire use, ordinary/Academy Danmaku, Ender/Wiki, defensive arcanums,
Corruption, helicopter encounter, Ultraponos, Tanner, Time Stop/contacts and
Academy acquisition passed, including three actual match-stop cleanup families.
Exact355 sources/988 classes/2819 resources match the tested artifact. All271
ported leaves, including the new grimoire JSON, are present in the packaged JAR.
The only error lines are the known Polymer packet-context diagnostic while
creating fake bots; no new runtime errors occurred. Real client audio and every
conditional mode's runtime are not independently observed.

Ready: `build/verification/skywars2-all-item-sounds-ccb40b6c.jar`.
Evidence: `build/verification/all-item-sounds-final/qualification.json` and
`tools/smoke_all_item_sounds.py`. The isolated server stopped, settings/profiles/
scoreboard/JAR restored and fixture locks released. Five pre-existing user
file hashes and applied-template deletions are preserved. READY ONLY: main
remains04396ca4; no installation/stop/restart or author RP/template change.
Obsolete Hole Erosion54941d0e archive was removed after qualification; previous
Weapon Sound593a39df and main fallbacks remain. Cumulative cleanup290 JARs/
10,231,414,603bytes/9.53GiB. The native packet fixture covers selected source
boundaries; inventory classification is broader than runtime/client playback.
