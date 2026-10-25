# Hedgehog wand, helpers, wall and spike source audit — 25 October 2026

Current implementation follows the author's native-explosion rule:32 power20
MOB bursts in a bounded queue, no spawned instant creepers. Captured WILD
owner/terrain/TNT lineage survives source death. Shared nuclear shader and
chat fallback replace the old marker trail. Any earlier implementation wording
about32 native creepers/200tick trail below is superseded; original source
mechanics remain evidence. Build4cd2ef8f passed native family/spike checks in
both actual matches, including real burst terrain, source death and stop.

Read-only source audit; this note is the only file owned by this researcher. No builds, servers, commits, source/RP edits. Rules read: project AGENTS.md, agent_notes/AGENTS.md, RESOURCE_PACK.md and SHEEP_HEDGEHOG_SOURCE.md. Source anchors below are relative to `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/`. Read as real UTF-8.

## Complete boundary

Ordinary + Academy five helper modes + 65-wall trial + seven executable spike drops + full God boss are one dependency family. God boss graph belongs to another researcher. Existing Silence fixture is not the boss. Narrow ordinary-only port would leave the actual upgrade trial and dropped executables incomplete.

## Input, ray and promotion

- `lbcr/function/raycast_loop9cd.mcfunction:1-7`: ordinary first casts normal ray, counts ordinary `hedgehog` within10 for40 advancement; sneak on upgradable baffledhedgehog invokes wall spawn; debit configured `baffled_wand_mana_use`2500, mage bonus and cast hooks. Native should use actual hand rather than source SelectedItem bug.
- `raycast_loop9:1-5`: sample at eye origin and then1 forward; increment Distance before action; <=200 recursive condition means201 samples (0..200). White ash2 spread. Mob-cell hit spawns1 behind sample; non-air spawns1 behind then block-aligned X/Z center. It does not stop after mob hit before checking block: same sample may spawn twice if both predicates hold. End sets Distance1000. Continue guarded nomagic R16 and stopper_magic R20.
- `raycast_loop9cd_upgraded[_left]`: sneak changes per-hand five modes Default/Healer/Tank/Far/Laser, tells original translated mode. Author's newer universal free-mode contract overrides legacy admission1000. Nonsneak calls `_2`; debit1000, mage and cast hooks. `_2_loop` reads actual selected/offhand mode. All five mode rays have401 samples separated.6 =>240 range. Entity impact at current sample; block impact back1/aligned. Original per-mode particles preserved.
- Ordinary sneak wall trial calls `can_upgrade` immediately, not conditional on boss threshold; S-tier Academy file is `lbc/function/other/magic_academy/upgrade/baffledhedgehog.mcfunction`. Do not confuse40-helper advancement with promotion.

## Helper payload and cadence

`raycast_end9[_mode_*]`: real silverfish, ambient sound MASTER1/pitch1.3; NoAI except Tank; gravity remains enabled, vulnerable; owner lbcID2/team copied. Ordinary BaffledHedgehog; Academy Default BaffledHedgehog, Healer HealingHedgehog, Sniper SniperHedgehog, Laser LaseringHedgehog, Tank HeavyHedgehog. Default/heal/sniper/ordinary timer8, usetimer; laser has no timer. Tank100HP, attack15, knockbackResistance1000, attackKnockback1 and native AI. Academy team joins via podchinenie_hedgehog; ordinary lacks explicit source team join but owner/team exclusion exists in attack selector.

`lbc/function/mobs_tick_lbc:5-6` runs `timer` BEFORE silverfish_tick. Timer at0 resets40 then decrements; initial8 reaches0 and fires on eighth active update, later every40 updates. Sniper chance20 only on timer eligibility, so failed chance waits40. No arbitrary helper TTL exists.

| Kind | Target selection | Actual attack |
|---|---|---|
| Ordinary | nearest hostile .1..15, owner/team excluded using scans20, excludes spectators/nomagic/strict objects | `other/raycastarrow`: native piercing100 arrow, velocity2, vanilla default arrow damage, helper UUID Owner, magic/killedarrow tags. Native user kill-credit must trace to caster. Dust30 and arrow.shoot MASTER1/1. |
| Default | owner within17 required; nearest enemy17; owner scan20; same-team exclusion separate nodmg1 | beam .3 steps through50 counter =>15 range, air stops,4 arrow by helper first eligible cell. silverfish.hurt1/1 |
| Healer | owner OR same-team in19, requires non-silverfish allied candidate; aims nearest allied | beam .3/50=>15; instant_health amplifier0 on every eligible allied sample; passes entities until solid. Not simply one heal target. |
| Sniper | owner OR same-team within95 required; target nearest hostile95, scan100 | chance.2 per40; .3/300=>90 beam,20 arrow first eligible cell; solid stops. Extra trident.return MASTER5/1. |
| Laser | hostile10; owner/team exclusion13; source chooses arbitrary limit1 rather than nearest | .3/30=>9 beam; NO solid check, .02 generic EACH eligible sample; native generic hurt cooldown remains. silverfish.hurt each shot update. |

Source beam cell selector uses dx=dy=dz=0 (one-block selector cube) plus distance<=5; do not flatten into radius5 beams. Default/sniper hit resets Distance1000; heal/laser can repeat on the same entity. Source filters are inconsistent between aim and impact; native protected roles/Greed/player team policy must remain. Native owner attribution must preserve caster for direct beam, arrows and Tank melee.

## Trial wall

`lbcr/function/raycast_loop9cd_upgrade:1-5`: five columns at caster local (1,0,1),(2,0,1),(-1,0,1),(-2,0,1),(0,0,1). Each13 native silverfish nested passengers, total65. Every fish30HP/maxHealth30, follow200,KB100,attack10,armor10/toughness10; persistent, tags baffled_wall/lucky_mob, dark_red nonitalic translated grand_baffled_wall; DeathLootTable lbc:hedgehog_wall. Only base receives infinite slow_falling amplifier1. Ordinary enemy silverfish AI remains; no caster team assignment.

Line7 broadcasts baffled_awakened dark_red. Lines9-10 count all baffled_wall silverfish dimension-wide;>=150 calls megahedgehog_spawn, which kills EVERY silverfish. Line12 readies trial. Boom spikes reuse this SAME function, including immediate readiness dispatch on victim (normally no matching trial stack) and global boss threshold.

Wall each mob tick chance `chance0_5`=.005. `other/baffledwall/tick`: aims arbitrary #mobs<=10 except wall/spectator/strict and same_team. Beam .3*50, normal gray dust, first cell mob predicate excludes wall; actual damage selector omits wall/team but excludes nodmg/nodmg1. Damage4 arrow by source fish. Normal melee10 plus this rare beam. Wall deaths independently draw one equally weighted entry from7 spike variants (loot one roll, default weight1, no looting conditions).

## Seven spikes and additional real dependencies

`lbc/loot_table/hedgehog_wall.json`: seven native snowballs with special1 and spike-specific flag. Exact component source must be imported; existing RP already owns translated names/lore and models. Standard has no explicit name color; most lore yellow/nonitalic, impulse lore aqua. Models lbc:spike_ender,spike_blind,spike_explosive,spike,spike_wither,spike_poison,spike_impulse; tooltip tiers X,A,X,A,A,B,S. Source snowball consumption/stat/throw sound must remain.

`items/snowballs:3` dispatches `projectiles/specials`. Lines20-25 convert6 throws to physical arrows at thrown snowball position using its actual Motion, source-arrow Owner nearest thrower bug should become actual thrower. Six arrows retain normal gravity/native hit physics. Defaults pickup2 except boom which omits it. Source killedarrow tags remove ordinary variants on block impact. Boom persists until trigger then killed by arrows_f.

| Flag | Base arrow damage | Payload |
|---|---:|---|
| spike_confuse |1| custom tipped raid_omen115 duration2, color16746227; arrows2 adds effect_dimshifting, duration2 normalizes1 |
| spike_blind |1.5| raid116 duration6000 color16771895 -> piss counter; yellow_lightning_asplayer: private stick-model lbc:piss200 particles for player, yellow glass particles others; blindness/slowness1s refresh, mobs follow_range2 |
| spike_poison |1.5| raid117 duration2 color1119083 -> effect_witch_gens_reset -> witch_gens/reset: dmg_dealt=1,food_count/lolidefecation/sleep_time=0, add have_mom, remove greed_cursed,greed_prestige=0 |
| spike_wither |1| water_breathing amplifier1 duration600 color0; custom realdrownpre/drown checks feet water =>clear wither +strength1/resistance2; feet AIR =>wither3 for3s. Existing native enhanced water logic may cover this. |
| spike_standart |10| no potion payload |
| spike_boom |.01| raid118 duration2 color9079434 -> effect_hedgehog_boom -> wall spawn. On block `projectiles/arrows_f:12` directly wall-spawns unless stopper_magic20; on living hit queue normalized1 then same spawn |

Every tipped item explicitly includes potion_duration_scale:1.0. Duration must be verified against native Arrow API; do not assume default vanilla tipped reduction. Source raw durations above are exact.

`projectiles/teleport_nahui`: random1..4, source dimension selected from actual overworld/nether/end/imprinted, teleports same XYZ to one of other dimensions or calls tp_nexus_rtp (custom nexus). This is NOT existing short same-world RTP. It is a new real dependency/decision for native arena architecture; silently replacing with modest random teleport changes authored effect.

`spike_impulse`: no arrow spawn. `other/raycast_spike` tags actual caster; `raycast_spike_loop` gray dust, .25 through1000 counter =>250 blocks, repeats7 arrow damage on every sample containing noncaster nonspectator #mobs; does NOT terminate after entity hits; stops solids. Damage by actual player caster. Does not apply physical knockback payload separately.

`silverfish_tick:7`: global every-silverfish 1e-13 chance invokes `workingitems/exopersoexplode1` (many20-radius instant creepers). This unrelated global catastrophic quirk needs explicit preserve/defer decision; it is not intrinsically wand mode logic. Angry-hedgehog falling skill hook is separate and already existing source family.

## Native integration available

- Model implementation pattern LbcSheep: SimplePolymerItem with actual source CUSTOM_DATA/model/tooltip; LbcWands.use early dispatch; owner/suppression/resident/frozen checks; lifecycle loaded/unloaded/forget/stop and current epoch. Main ownership remains root; no modifications here.
- `LbcPsycho.yellow(target,owner,int)` exists but sets rather than adds; source116 uses cumulative counter, so adapter must account for queue contribution.
- `LbcColors.trackWater`/enhanced-water effect tracking covers water_breathing1 custom wither/strength/resistance and indirect attribution; verify actual ingress calls.
- `LbcBabylonEffects` offers narrow typed arrow effect transport and per-author queued contributions, lifecycle attachments and Greed/skill immunity behavior; should be adapted for115..118 rather than injecting raid markers into unrelated ordinary runtime.
- `LbcIndirectKills.coldCause`, inheritedEffect, withCause/captureDamage, attack inheritance preserve delayed/native source attribution. Source lacks caster tracing through helper; user requires repair.
- No existing native witch_gens_reset or dimensional-shifting transport implementation was located by exact effect-name search; simple source score reset is implementable, full cross-dimension random transfer is a separate architecture dependency.

Static research only. No runtime or design parity is claimed by this note.

## Native duration verification

Read local26.3 mapped sources JAR `.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-d6f9bb6878/26.3/minecraft-merged-d6f9bb6878-26.3-sources.jar`, `net/minecraft/world/entity/projectile/arrow/Arrow.java:45,109-114`. `getPotionDurationScale` reads pickup stack component default1.0; `doPostHurtEffects` calls potionContents.forEachEffect(effect -> mob.addEffect(effect,effectSource), durationScale) directly. Explicit source1.0 therefore FULL blind6000 ticks (300s) and enhanced-water600 ticks (30s). Raid2 later normalizes1 in source arrows2. Earlier preliminary chat estimate600 for blind is withdrawn.

## Implementation boundary handed to root

New LbcHedgehog.java and HedgehogChecks.java authored under explicit parent ownership. No build/server/commit performed by this researcher. Core has actual stationary helper native fish, Tank melee, all source rays/cadences,65 passenger trial and global150 boss dispatch, source table drops, captured author and lifecycle. Checks are awaiting root compilation/qualification and do not themselves establish runtime behavior.

Subsequent user decision relayed by root: boss deletes ALL other silverfish in its dimension and blocks all new silverfish while alive, replacing the periodic25s source workaround. Core exposes eraseForBoss(level,boss); God service and root loaded gate enforce block. WALL remains WILD against caster and allied players, consistent with hostile-trap source and user trap contract; cast helpers protect allied players at impact.

User retains the rare global silverfish sneeze and adds translated broadcast `skywars2.hedgehog.sneeze` = `Ёжик чихнул.` Root owns RP template. Source LootItemRandomChanceCondition uses nextFloat, which quantizes tiny probabilities to roughly1/16777216 rather than the authored1e-13. User numerical intent1in10trillion is implemented with nextDouble<1e-13. Native deterministic sneeze boundary creates exact32 radius20 source creepers,5 sounds,500 initial explosion emitters and200tick block-marker trail. Creeper lineage uses existing native CreeperDrops attribution/lifecycle. Boss fish participates in the all-silverfish index.

### Damage-tag correction

Source and native `minecraft/tags/damage_type/bypasses_cooldown.json` both explicitly include minecraft:generic AND arrow. Consequently the live Laser `.02 generic` repeats damage at every occupied ray sample, bypassing hurt cooldown through the real source tag. The earlier phrase above about retaining generic cooldown is incorrect; native code correctly uses generic and existing imported tag, with no additional reset. The check requires multiple sample hits through solid blocks. This also means impulse7/arrow repeats each cell as source.

## Spike runtime/checks handoff

Root assigned and integrated `LbcHedgehogSpikes.java` and `HedgehogSpikeChecks.java`. Six native Arrow transports retain native physical launch, baseDamage, shield/contact handling, current reflected owner and colored tipped visuals without fake Raid markers. Native Snowball launch computes original throw Motion; actual item use consumes once, awards statistic and plays source throw cue. Impulse keeps source multiple7-damage ray samples. BLIND adds6000, POISON queues exact source reset, WITHER applies full600 enhancedwater1, BOOM queues/blocks spawn65, CONFUSE queues real cross-dimension service. Spike runtime intentionally allows supported source worlds outside the arena, permitting subsequent casts after transfer. Core WALL spawning/liveness similarly allows supplied ServerLevel, preserving boss threshold within that dimension.

WALL now survives author death/disconnect and owner-forget callbacks, with original captured cause. Explicit match-stop/epoch/unload/suppression cleanup remains. `retireOwned(owner,true)` is reserved for deterministic fixture retirement. Wall drops roll with the fish as THIS_ENTITY, independent of online author. Its native melee copies the native26.3 Mob operations but captures DamageSource before ServerPlayer friendship admission; rare beam also captures before hurtServer. This keeps WILD damage to caster/allied players. Checks exercise that rare beam directly after its randomized admission boundary.

Checks cover both-hand consumption/stats, source base damage and durations, no fake Raid, native physical Greed reflection, protected roles/allied players, derived-Wither kill credit, accepted-arrow Confuse actual nonplayer clone, repeated Impulse samples and both BOOM native contact modes. New deterministic stop probe retains a native spike before root's actual match stop. Code/checks were handed to root for compilation and real disposable-match qualification; this researcher ran no build/server/commit. Post-disk restart/potion-unload persistence remains subject to root's usual qualification limits.

## Final nuclear and fixture correction

Standing user contract requires the shared shader for legacy nuclear marker effects. Sneeze now calls `NuclearVisuals.show(level,at,12,false)` and has no200tick legacy block-marker trail. The5 source sound events and initial500 explosion emitters remain. Broadcast uses `Component.translatableWithFallback` with exact `Ёжик чихнул.` so it renders before the user's manual RP-template application. Rare indexing and deterministic sneeze entry permit every actual ServerLevel; root broadens existing WILD CreeperDrops lifecycle accordingly.

Core fixture now triggers the rare event deterministically, captures both participant chat packets, verifies actual32 radius20/Fuse0/ignited/persistent/invulnerable native creepers with WILD caster cause/currentepoch and exactly one shared nuclear visual. Its guaranteed finally discovers and discards every new creeper synchronously before the next native entity tick, including on assertion failure. It clears nuclear visuals only after requiring an empty service before this owned probe.

Native focus revealed an integration gap in the shared Wands damage filter, repaired by root for captured WILD Hedgehog/God damage. Fixture melee expectations now retain native difficulty scaling (10normal/6easy/15hard), and both initial isolation guards exclude already removed lookup entries. Native cow drops/XP are cleaned only around owned lethal probes; unrelated live entities remain diagnostic failures rather than being silently erased.

## Author correction: native sneeze explosions supersede creeper workaround

The user explicitly rejected spawned ignited fuse0/1 creepers as datapack workarounds. The current core contains NO sneeze creeper/TNT dummy actor creation. It queues32 original positions and directly invokes native `level.explode(null,capturedDamage(actualFish,originalAuthor),null,x,y,z,20,false,MOB)`. Null explosion actor includes the fish itself in native exposure; captured DamageSource retains the real fish and author. MOB interaction preserves native mobGriefing and bypasses the configured TNT-only protection. The shared nuclear shader,5 source sounds and single500-emitter burst remain.

An immediate bounded slice runs on activation. The shared native queue processes up to4 offsets/slice under a4ms deadline checked between indivisible native explosions, rotating batches. A completed native explosion may exceed4ms; this is not an enforceable hard bound inside vanilla's explosion routine. Batches capture level/origin/source/author/WILDcause/epoch and continue after fish death/removal or author departure. Per-dimension Time Stop pauses them; actual stop clears them. Point admission checks existing chunks and never explicitly acquires missing chunks. Root owns the global native hazard hooks for role filtering, original captured damage and terrain lineage.

Genuine TNT blocks primed by these explosions retain native TNT entities and fuses, with captured WILD lineage through `inheritSneezeChild(level,child)` and recursive `inheritSneezeChild(parent,child)`. These are existing terrain TNT, not artificial blast carriers. Child source ownership remains visible through native discard-before-explode callbacks, then is cleaned on the next service tick/actual stop. God-source sneezes resolve the boss summoner, including the boss's retained UUID cause if that player is offline.

The deterministic fixture now uses a bounded prevalidated empty remote blast region with one stone and one genuine TNT block. It verifies actual immediate native terrain destruction, exposure killing the source fish, native TNT block priming with WILD ownership, absence of spawned creepers, shared shader/chat, source-death continuation and Time Stop pause. It cancels its owned remaining queue/children before their next native ticks. The actual stop probe retains a native queue above build height and starts Time Stop last in the parent probe sequence; cleanup verifies pending0 and invalid epoch. Spike CONFUSE fixture now observes the actual player object across worlds, explicitly restores original dimension/team and acknowledges native dimension changes, avoiding hidden nonplayer destination lookup assumptions.
