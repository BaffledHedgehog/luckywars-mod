# Suppression audit: encounters, drops and world services

Static read-only audit, 27 October 2026. Only this new English note was written.
No source/resource/production changes, builds, server tests, network access or commits.
Read project AGENTS.md, agent_notes/AGENTS.md and RESOURCE_PACK.md. Existing author
decisions remain authoritative. Findings describe code paths, not runtime proof.

## Roots and interpretation

- Native N = `E:/servers_mew/skywars2_last/luckywars-mod/`.
- Legacy D = `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/`.
- Paper P = `E:/servers_mew/cringe1_21_4_skywars/plugins/luckywars/src/main/java/`.
- Native file names below are under `N/src/main/java/me/skywars2/game/`, unless stated.
- MAG = magic beacon; TECH = mechanism beacon. Personal no-magic, artifact auras,
  role immunity, Time Stop and WILD allegiance are separate concepts. WILD alone
  does not establish an exemption from either jammer.

The audit follows drop admission, the spawned entity/block identity, persistent
job state, and central deletion before calling a local missing check an omission.
Existing electricity and helicopter decisions must not be reopened by analogy.

## Strongest confirmed discrepancy: Soul Vex and cultists lose suppression identity

**High-confidence static port discrepancy.** Ordinary seven summon formations
have a working central MAG deletion identity; Soul Vex and Psychovex/Cultus do not.

- D `lbc/function/workingitems/summoner/vexes.mcfunction:75` adds `summactivated`
  after assigning the four Vex. D `.../psychovex.mcfunction:41` adds the same tag
  to the assigned cultist. D `lbc/function/other/stopper_magic.mcfunction:3`
  deletes `#minecraft:mobs` with `summactivated` within20.
- N `src/main/resources/skywars2/summons_presets.json:4` Vex preset has only
  `summvex`; cultist presets at5-10 have their family/lucky tags, with no `magic`
  or `summactivated`. `LbcSummons.java:273` spawnPreset loads those presets;
  at282 it only sets position and epoch before tracking/insertion at283-285.
- `LbcSummons.java:225` explicitly adds `summactivated` in ordinaryTree, which
  is a different route used by the seven later ordinary formations. There is no
  corresponding addition for spawnPreset; the load hook at472 also adds none.
- `LbcMagic.java:522` magical accepts type membership, `magic`, or
  `summactivated` plus mob membership. Vex/skeleton/wither-skeleton/zombie are
  absent from `N/src/main/resources/data/minecraft/tags/entity_type/magic_entities.json`.
  Central pulse at976-990 therefore does not select these untagged summon bodies.
- Launch is already MAG-gated at `LbcSummons.java:176` and191. Ongoing
  `paused(Mob)` at130 checks time/frost, not jammer. `canAttack` at158 delegates
  to the victim-position gate at134-137. A victim inside MAG is protected by this
  filter, but the summon body itself survives, and targets outside MAG are not
  rejected merely because the summon/caster stands inside MAG.
- `LbcSummons.java:300` tick and loaded at472 do not restore the missing identity.
  Exact identifiers were also searched in legacy Paper/Ignite Java/Kotlin: no
  additional override for these actors was found in that bounded search.

Question: should Soul Vex and all six Psychovex/Cultus bodies remain MAG summons
and be destroyed when their own position enters MAG, like the other formations?
The source says destruction, not pause. If a broader redesign is desired, choose
MAG/TECH/neither explicitly and specify whether released projectiles survive.
No correction was applied during this audit.

## High-confidence current bypasses requiring a policy answer

### Antigravity: author-selected Paper storm has no jammer gate

`LbcAntigravity.java:75` ready checks lifecycle, resident chunks and Time Stop;
spawn at89-96 puts a Storm record in a Java list. Display creation at184 adds
ACTOR/epoch only. Platform updates at207 and storm emission at262 have no MAG or
TECH condition; central LbcMagic has neither this record nor a matching display
tag/type. Thus both beacons currently leave the storm and its moving pieces active.

This is **not a demonstrated port regression**. The author explicitly selected
the finite Paper storm, documented in [ANTIGRAVITY_SOURCE.md](ANTIGRAVITY_SOURCE.md).
P `me/luckywars/swrg/SwrgNativeService.java:5700` consumes the drop,6824 starts
the finite runnable,6870 removes/launches blocks,6894 starts display workers;
those methods have no jammer check or magic/mecha identity either.
D `lbc/function/drops/antigravity/start.mcfunction:2` instead creates a magic-tagged
marker, but that perpetual datapack engine was explicitly superseded. Its tag
cannot silently override the chosen Paper behavior.

Question: is Antigravity MAG, TECH, or neither? If suppressible, should it reject
new activation, pause/end future terrain emission when the center enters a field,
and/or stop already flying pieces locally? These are separate choices. Do not
silently remove existing pieces, delete a carried body's velocity, restore already
removed terrain, or introduce a gravity modifier; none follows from the evidence.
The latest60-second velocity/support contract remains in force.

### Chef: physical NPC is currently outside both jammer categories

`LbcWtf.java:99` constructs a native Mannequin with no magic/mecha tag.
The actual accepted-melee Label path at305-325 checks material hit, roles,
actions and target, but not either jammer. Ongoing step at330-345 checks
owner/target lifecycle, Time Stop, stasis and actions, not either jammer.
Mannequin belongs to neither central suppression entity tag. Chef therefore
spawns and continues pursuit/mining/building/melee through both beacon fields.

The author explicitly requested the physical Mannequin redesign and bound target;
see [CHEF_REWORK.md](CHEF_REWORK.md). That does not by itself classify the Label
or NPC as MAG, TECH or neither. This is a policy question, not a proven regression.
Question: keep Chef unaffected, or classify its Label activation and/or released
NPC separately? If affected, specify pause versus terminal removal and which
position controls it (owner, Chef, victim). Do not infer classification from
the old display model or from the word summon.

### Stand Clocks: activation gated, ongoing dimensional stop has no local anchor

`LbcTimeStop.java:105-116` already rejects activation under magic suppression at
the player's eye. `begin` at122 stores one dimensional timer in stops; `frozen`
at134 checks that state and Stand/clocks/observer exemptions. Tick at331 decrements
the timer until end. There is no suppressible magic/mecha entity representing the
stop and no continuing beacon test. Entering/placing MAG does not end or locally
exclude a running stop. TECH also has no continuing effect on it.

Question: keep MAG as launch-only admission, or should a later MAG end the whole
dimensional stop / produce a local unfrozen zone? These are materially different
mechanics and must not be invented by adding a check to frozen. Preserve the
author's dimension-only20-second stop, restart-to20,40-second cooldown and retained
hit replay contracts in [TIME_STOP_PORT.md](TIME_STOP_PORT.md).

## Central coverage verified; missing local call is not sufficient evidence

`LbcMagic.java:608` calls entity pulse every2 active ticks. magical at522 and
mechanical at999 select concrete types/tags; pulse at973-990 applies radius20
position deletion. Type-tag membership is source/resource data, not the item's
visual theme. Blocks use queued scans/dirty-block processing at609-626 and
erase at1039-1042. Current native MAG block clearing is spherical20; TECH retains
the source +/-15 cube and not_block rule. Immediate deny-on-use and eventual
central payload destruction are different behaviors.

| Family / current route | Verified coverage or explicit boundary |
| --- | --- |
| Seven ordinary summons, including nested horse/bat/blaze actors | `LbcSummons.java:225` adds summactivated for every ordinaryTree node; MAG central deletion applies. Launch gates191-192. Soul Vex/cultists are the discrepancy above, not evidence that all summons are broken. |
| Ant acid and dropped acid burst | `LbcAntDrops.java:51,141` calls sourceAcid. `LbcClouds.java:496` preserves magic on the custom AcidProjectile, so MAG pulse selects it despite no explicit suppression in sourceAcidActive515. Ant bodies themselves are untagged physical zombies, not automatically magical. |
| Potion spawner traps | `LbcPotionSpawnerDrops.java:103` places an actual SPAWNER. Native block tags: `data/minecraft/tags/block/magic.json:46` and mecha.json:44 both include it. Splash potions are in magic_entities.json:11. No missing producer gate is claimed. |
| Random portals | `LbcPortalDrops.java:201` creates END_GATEWAY; magic.json:51 includes it. Local canUseGateway216 has no MAG call, but the gateway block is centrally removed. Bedrock shell is not thereby removed. Pending construction versus immediate admission is a separate policy, not a proven gap. |
| Home drop/mirror | `LbcHomes.java:108,177` uses homeMarkerSuppressedAt; `LbcMagic.java:419` preserves distinct artifact25/beacon20 marker deletion radii. Covered without guessing from marker tags alone. |
| TNT, Zombie TNT, creeper traps and their chains | Native TNT/creeper type membership in entity_type/mecha.json:7,9 is sufficient for TECH pulse; clouds belong to magic_entities.json:4. WILD provenance does not exempt these payloads. Carriers do not acquire those types automatically. |
| Falling netherite/thunderbolts, anvils, cages, slabs | Physical falling blocks/zombies and constructed blocks need actual membership checks; lightning_bolt is TECH by entity_type/mecha.json:5. No blanket MAG requirement was inferred for the physical families. |
| Vex/skeleton/Bob/shulker-bullet drops | `LbcCreatureDrops.java:65,122,152` uses actual native actors and source presets. Native shulker bullets are MAG type members; ordinary untagged Vex/skeleton/zombie bodies are not. These traps are distinct from player summoners' missing summactivated. |
| Bees / ant bodies / lucky pigs | Actual native physical mobs; no magic/mecha body identity follows from WILD targeting. Their custom effects/projectiles are separate paths. No blanket immunity or classification was inferred. |
| Helicopter hostile encounter | `LbcHelicopterEncounter.java:95` tags mecha;157 checks TECH point;241 handles terminal suppression. `LbcMagic.java:985` delegates reward/lifecycle before generic kill. Already author-approved TECH behavior, keep it. |
| Normal/Bob drone | Spawner-minecart/TNT are TECH by type. `LbcDroneDrops.java:82` explicitly suppresses the native production step under TECH. No claim that the bat/hoglin must itself disappear. |
| Ultraponos | `LbcUltraponos.java:105-106` permanently cancels emitter under TECH, preserving existing creeper lifecycle. Do not replace terminal source deletion with a temporary pause. |
| Stalin | `LbcStalin.java:100` TECH suppression in ongoing activity predicate. Actor lacks mecha, so producer pause does not imply body destruction. |
| Transformator | `LbcTransformator.java:142-146,167` explicitly uses TECH for producer attacks. Exact actor tags111 and124; central mechanical exclusions1002 include transformator. Its fireball/electrical payloads have their own rules. |
| Pisa boats | `LbcPisaDrops.java:83-95` inserts real oak/chest boats with ACTOR/epoch, no MAG/TECH identity; boats absent from both entity tags. No port regression established for this physical trap. |
| God Hedgehog | Boss tags at `LbcGodHedgehog.java:147` are source-compatible, not magic/mecha. Eater adds magic236 and removed-body guard167 retires centrally killed actors. White displays/kill-ray markers are source-untagged;274 likewise does not add magic. Silence immunity is an explicitly settled separate rule, not blanket jammer immunity. |
| Sauvojen / Gaycaster trials / Hedgehog helpers | `LbcSauvojen.java:108,134` preserves magic carrier plus point suppression. `LbcGaycasterAcademy.java:75,154,168` checks suppression at creation/owner/hazard. `LbcHedgehog.java:84,96,156` checks ongoing helpers, activation and insertion. Covered at these service boundaries; not a re-audit of all wand engines. |
| Soulless fields / ghost Vex | Field display carries magic at `LbcSoulless.java:222`, valid field229 checks point. Ghost/trial Vex193 has custom tag only and source effects/ghosting_tick.mcfunction:7 / soulcorruptwand/cast_2meditate.mcfunction:1 spawn untagged Vex too; no summactivated regression was established for this separate branch. |
| Academy NPC and upgrades | `LbcAcademy.java:186-210` interaction allows progress without a jammer call; central TECH explicitly excludes magic_academy1002. This is a service actor, not automatically a magical payload. Classification/upgrading under a jammer remains an optional policy question, not a demonstrated source discrepancy. |
| Global altitude DROWN/Cosmos | `LbcSkyHazards.java:24-41` applies independent altitude hazard without jammer. `LbcExasperato.java:442-450` uses a transient Creeper as explosion source without inserting it as an entity, so central TECH entity killing cannot intercept it. Author explicitly requires independent high-altitude hazard (SKY_HAZARDS_PORT.md). No storm-dependent or jammer-dependent rewrite is authorized by that fact alone. |

## Scope limits and preserved decisions

This is bounded class/call-chain coverage of currently implemented encounter/drop
services. Wands/firearms themselves and passive equipment were assigned elsewhere.
Crocodildo, forest and magic-bean matches in the old datapack do not establish
ported current encounters: native searches found only Crocodildo exclusions in
LbcTanner and unrelated forest feature names, not registered encounter handlers.
Do not turn unported source mechanics into current jammer regressions.

Source-compatible central killing can occur after creation; this note does not
claim an entity cannot act during that interval, or that a queued block scan has
an instantaneous deadline. No native timing/performance proof was run here.
No broad technical immunity follows from role guards or swrg/service exclusions.
No unresolved MAG/TECH policy was changed, and existing author exemptions were
not treated as omissions. The previously approved electricity continuation,
TECH helicopter, finite Paper Antigravity, physical Chef, independent sky hazards
and God Hedgehog Silence contracts remain as documented.
