# God Hedgehog source contract — 25 October 2026

Read-only static research for the full Hedgehog family. Only this note is owned by this researcher; no implementation, build, server change, resource-pack edit or commit was performed. Read project AGENTS.md, agent_notes/AGENTS.md, SHEEP_HEDGEHOG_SOURCE.md and RESOURCE_PACK.md. Legacy root D = `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/`. Every legacy path below is relative to D. Main remains READY ONLY at04396ca4.

## Closed dependency scope

The family requires the ordinary helper, all five Academy helper modes,65-fish wall trial, seven wall spike items, God Hedgehog boss, its eater and white-arrow attacks, and its single `lbc:god` drop. The god drop is an authored trophy with magicwand metadata: **no executable god-item dispatch was found** in the entire legacy LBC functions or Paper/Ignite Java/Kotlin sources. Do not invent a new spell or let this trophy create further port gating. Seven spikes belong to the wall subsystem; boss itself produces only the one trophy. Broad loot pools need not be enabled.

## Spawn and native AI

`lbcr/function/megahedgehog_spawn.mcfunction:1-9` kills every silverfish in the current dimension, then spawns one persistent ordinary-AI silverfish, tags `lucky_mob,god_hedgehog`, exact stylized gradient name ARCHONFLEDGODGEHOG, scale16, Health/max_health2147483647, armor20 and toughness20, DeathLootTable `lbc:god`. There is no NoAI, attack attribute override, owner/team score, or Invulnerable flag. Thus vanilla silverfish AI/melee remains in addition to authored attacks. Do not confuse huge HP with invulnerability. Four global flowey_laugh cues are MASTER1 with pitches1/1.05/1.1/.95, minimum volume1; title godfled_spawn dark_red, long obfuscated dark_red subtitle.

Live graph: `lbc/function/tick.mcfunction:1` -> `tick_not_timestopped.mcfunction:3-4,7` -> `mobs_tick.mcfunction:6` -> `mobs_tick_lbc.mcfunction:6,12`. Global ticker increments after mob updates, only outside Time Stop. Boss and displays therefore pause with actual source Time Stop. Lucky boss also visits silverfish_tick; its astronomically rare1e-13 per update exopersoexplode1 hook is global silverfish behavior, not a bespoke boss attack. Native implementation should decide explicitly whether that incidental quirk belongs in this port, without gating the whole family on its broad unrelated dependency.

Searched all Paper `plugins/luckywars/src/main/java` and Ignite `mods/luckywarsignite/src` for god_hedgehog, hedgegod, godhedgehog, floatingwhitearrow, godbam: no overrides. Generic silverfish references in Paper are dungeon snapshot creation only, not registered boss behavior. This is static evidence, not old-server runtime observation.

## Attack timeline

`lbc/function/other/god_hedgehog/tick.mcfunction:1-20`; let t=global ticker modulo500, not boss age. First update can enter any phase. Every update: if floored y<=0, teleport to(0,250,0); refresh slow_falling100sec amplifier0. Count living god-tag silverfish (DeathTime0) dimension-wide; if>=2, kill executing boss. This is a duplicate suppression quirk; normal port should ensure one encounter and avoid duplicate reward farming.

| t | Behavior |
|---|---|
|1|Teleport all other silverfish to boss then kill them. Their normal death loot remains possible. Return from attack handler.|
|10,20,30,40,50|Death laser (five per cycle).|
|100..140|41 consecutive eater seeds; boss descends2 and rotates yaw+20 each seed.|
|240..280|41 rotating white arrows around a freshly selected random nonspectator player each tick.|
|281..320|40 form1 arrows.|
|321..360|40 form2 arrows.|
|361..400|40 form3 arrows.|
|401..440|40 form4 arrows.|

Death laser `death_laser:1-5`, `laser_rc:1-3`: reset Distance0, resistance1sec amplifier4; anchored at eyes aim at nearest #mobs within200 excluding silverfish, spectator and strict_map_object. Recursion increments then summons fuse0 TNT with explosion_power10, CustomName translate godbam, every3 blocks until Distance101: **101 TNT per laser**, offsets0..300. No terrain collision/end obstruction test. TNT has no owner in source (native must inherit encounter attribution). Zero own Motion and teleport15 toward fixed world coordinate(0,150,0). The effect is temporary resistanceV, not permanent boss Invulnerable.

## Eaters

`spawn_dim_scrap:1-4` seeds item_display10 forward from eye execution anchor, model lbc:hedgegod_eater, initial scale.01, fixed, full brightness15, view_range1000, tags slowraycast/rare/hedgegod_eater/magic. Plays energy_2 MASTER1 pitch1 min.5 at seed. Each seed modifies boss y-2 and yaw+20.

`raycastselector:1,9,12-13` advances Lifetime and routes rare; `raycastselector_rare:33` routes eater. `hedgegod_eater_tick:2-12` increments independent Distance age. Age1 `size1` scales each component20x (.01->.2), interpolation80. Age3 chance96% spawns **one** successor using the authored compound orientation transform (`tick:5`), nearest #minecraft:mobs excluding spectator/strict/silverfish, no distance cap. Successor4 forward in final composed rotation; this is chained branching one successor, not many per parent. Expected chain length25 including root, without an arbitrary lifetime/cap. Exact transform should be retained or deliberately adapted: facing target -> positioned forward.3 -> rotated as parent -> forward1 -> facing parent -> facing backward -> positioned as parent -> forward4. If no target, command chain fails. New display starts its own age.

Age79 scales20x (.2->4), interpolation1. Age80 plays energy_3 MASTER1 pitches1/1.5/2; squid_ink spread3 count50; places `lbc:air_sphere_7x7x7` at(-3,-3,-3), and deals10 OUTSIDE_BORDER to #mobs within4 excluding spectator/silverfish, explicitly attributed to one existing god-tag silverfish. Native role/team protection and indirect attribution remain required user contracts. Age81 source rounds current scale with data-get1 then multiplies1e-7 (nominal4e-7); age>=83 kills display. There is no continual eater travel between spawn and detonation.

Read-only gzip/NBT decode of `lbc/structure/air_sphere_7x7x7.nbt`: size7^3, one air palette,179 block records, no entities. Exactly the full centered lattice sphere dx²+dy²+dz²<=12, radius sqrt12=3.4641, bounds[-3,3]. Do not replace with a7^3 cube or radius3 sphere. Native terrain writes must obey current map/protected-region/indirect destruction contracts.

## White-arrow geometry and lethal ray

`spawn_white_arrows*:1-*`: macro starts random player's position+.4Y, rotated(i,r), moves10 forward, faces player eyes, shifts local-left j, rotates yaw+k, summons item_display lbc:whitearrow scale1.5 with fixed quaternion left_rotation[.6532815,.27059805,.6532815,.27059805], tags floatingwhitearrow/slowraycast/rare. Cue beacon.activate MASTER.5 pitch2, witch spread.4 speed2 count5. Calls Sauvojen meditate/loop_particle_white purely for40 white dust points, local forward .25..10; this is a small reusable presentation helper, not a dependency on the unported whole Sauvojen system.

Parameters: default i=5*ticker,j=1,k=1,r=0. Form1 i=int(.1*ticker),j=int(2.5*(ticker%20-10)),k=1,r=0. Form2 i=7*ticker,j=k=0,r=(ticker*7)%180-90. Form3 i=180*ticker,j=0,k=random[-10,10],r=0. Form4 i=95*ticker,j=k=0,r=(ticker*45)%180-90. Integer storage truncation is part of legacy math.

`hedgegod_white_arrow_tick:1` triggers exactly Lifetime40. shoot: beacon.power_select MASTER2 pitch2;10 end_rod spread.2 speed.05; marker with lifetime20, tags raycastarrow/player/slowraycast, copies display position+rotation, then kills display. Marker is not tagged magic/shield_blockable and has no caster ID/team assigned. `raycastselector_player:10` -> `lbcr:raycast_arrow:1-6` -> `damagewithid3:1-3`. Each update resets after21 recursion samples:21 end_rod+beacon.deactivate cues (volume.02,pitch2), samples0..2.5 forward at.125 increments, then moves marker to final sample and Distance0;20updates gives50 travel. Terrain does not stop ray.

Hit requires paired dx0/dy0/dz0 overlap tests offset(-.99,-.99,-.99); kill targets #minecraft:mobs except spectator, Greed, strict_map_object, god_hedgehog, void_resistance>=1, same lbcID2 or same team_number. **This is /kill, not damage3.** No source credit is set; source missing-score comparison can cause odd eligibility. Native should use deliberate WILD boss provenance, enforce current role protections and attribute lethal attack to encounter origin. Boss owner/team and lethal native interpretation should be confirmed with author if a faithful target differs from source bugs.

## Silence and reward

User explicitly requires god_hedgehog immunity to Silence and retaliation. Already implemented in native `src/main/java/me/skywars2/game/LbcConsuming.java:357,366,530` via exact tag immunity, with one10-billion raw OUTSIDE_BORDER hit attributed to nearby boss (`:377` onward). `GameChecks.java:10619-10652` qualifies entity-load/event/queued immunity and retaliation. Preserve and reuse these hooks; spawn exact tag on real boss.

Source: `workingitems/awaken_tuntija_wands/consuming_wand_upgraded_mode_silence.mcfunction:12-13` excludes god from nonplayer removal within48, retaliates10,000,000,000 outside_border by nearby boss. This immunity does not imply immunity to every other effect or weapon.

`lbc/loot_table/god.json`: exactly one carrot_on_a_stick, lbc:god model, lbc:tier_x tooltip, name translate god with italicfalse,10 intentionally blank dark_red italicfalse lore lines (first3 minecraft:alt, remaining7 minecraft:illageralt), custom data {god:1,magicwand:1}. No max_stack override, consumption or use action. Implement native inert trophy/presentation; broad magicwand metadata must not make it invoke unrelated spells. Current native no dedicated god trophy registration found.

RP read-only check at author26.3(1): god, whitearrow, hedgegod_eater item+model JSON exist; eater/whitearrow texture exists; flowey_laugh/energy_2/energy_3 OGG exist. Existing ru_ru keys: god='Бог', godbam='Ежовый Смертолуч', godfled_spawn='БОГ ВОЗРОЖДЁН'. Preserve keys/styles; no new translation fragment is warranted.

## Decisions for integrating complete family

No additional large family is required by boss reward. Preserve authored attack phase/damage/geometry and complete trophy acquisition. Source quirks worth explicit author choice: current-dimension all-silverfish wipe (including unrelated ones); boss-global ticker phase vs age; whether astronomically rare global silverfish explosion should carry over; whether raw2.147b health attribute should be retained when target version clamps max-health; WILD kill ray vs revised finite damage. Existing user contracts already authorize ownership inheritance, protected-role guards, Time Stop and match cleanup, and exact Silence behavior. Do not ask again about already settled Silence or leave the entire family gated on speculative dependencies. This document is static research only; native behavior and real client rendering remain unqualified.

## Author follow-up and implementation ownership

The author has now overridden the legacy periodic sweep: delete ALL other silverfish when God Hedgehog spawns and reject EVERY new silverfish for its entire living lifetime, preserving the boss itself. Remove the old25-second sweep workaround. Core `eraseForBoss(level,boss)` owns the initial quiet removal; native LbcItems entity-load rejection owns continuous admission. This is deliberate target behavior, not a claim about the historical function.

The author also selected preservation of the extraordinarily rare global silverfish explosion with a new chat notice; the core worker owns that hook. No additional author permission is needed for these settled choices.

Researcher received implementation ownership of **new LbcGodHedgehog.java only**. Root owns integration/explosion source hooks and serial build/runtime/deployment. Resource worker owns GOD item/native aliases; core worker owns initial silverfish removal and rare hook. No build/server/commit is executed by this researcher.

Implementation now exists in new `src/main/java/me/skywars2/game/LbcGodHedgehog.java`, including native silverfish melee, default authored health/armor/scale, boss-tag Silence reuse, eater displays and179-cell removal, native white displays/marker kill rays, and bounded retained101-explosion laser series. `GodHedgehogChecks.java` supplies native source-boundary probes,201 actual elapsed attack ticks and an actual-stop retention probe. Root owns compilation, runtime validation and global hooks. This researcher has not claimed these checks passed and has not built or run a server.

## Qualified native integration

Build4cd2ef8f passes actual arena and Imprinted boss/laser fixtures, source101
queue, native white/eater attacks, Silence, trophy and real stop cleanup.
Encounter validity/summon is per actual level, not arena-only. The narrow
Exasperato.encounterBlast entry permits that dimension and marks native
fuse0 blasts UNPROTECTED; configured TNT protection therefore does not weaken
source God lasers. Native-actor metadata never becomes a spawned instant TNT.
Exact artifact and32-result/two-match evidence: build/verification/hedgehog-final/.
