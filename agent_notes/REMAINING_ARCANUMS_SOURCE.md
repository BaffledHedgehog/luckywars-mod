# Remaining arcanums: bounded source audit (2026-10-24)

Read-only audit; no Java, source-server, resource-pack, or build changes. Source root below is `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`; native root is `E:/servers_mew/skywars2_last/luckywars-mod/src/main/java/me/skywars2/game/`. Pointers are root-relative file:line. Findings establish source behavior, not runtime validation.

## Current integration

Healing and Barrier are now implemented under the author rulings in [DEFENSIVE_ARCANUMS_PORT.md](DEFENSIVE_ARCANUMS_PORT.md). The source oddities below remain historical evidence; direct Barrier immunity, once-per-player500% upkeep, removal of activation fee and fresh-HP healing override their source branches. Babylon and Eye remain separate unported scopes.

## Recommendation (historical audit)

Next cohesive small batch: **Eternal Life + Absolute Barrier**, with shared per-hand holding/lifecycle dispatch. Healing is straightforward. Barrier needs an explicit decision on the shared mutable admission threshold described below before implementation. Do not classify `end*` as End spells: these files are cleanup functions for existing per-hand states. Eye laser is a separate source-oddity batch; Babylon is a projectile/effect batch rather than a small defensive-arcanum addition.

Native exclusions: `LbcItems.java:150,712-715,733` registers/wires `LbcVoidArcanum`, `LbcTanner`, `LbcKiuas`; `LbcIceMaterials.java:20-27,84` owns Neutral Ball. Skip those existing owners. Bounded Java search found no runtime owner/flag for `healing_arcanum`, `barrier_arcanum`, `eyelaser`, or `gate_of_babylon`; the Babylon advancement mentioned in `GameChecks.java:18258` is not an item implementation.

## Holding and cleanup

`function/players_tick.mcfunction:35-39`: mainhand and offhand `arcanumt` dispatch every tick, suppressed by a nonspectator `nomagic_active` player within 15 or `stopper_magic` marker within 20. `items/arcanums/select.mcfunction:4,13-16` dispatches healing/barrier/eye; `select2.mcfunction:4,13-14` dispatches healing/barrier only. Holding another arcanum removes the old barrier tag; removing arcanumt entirely calls `end.mcfunction:1-5` / `end2.mcfunction:1-5`. Those remove use/void/tanner/kiuas/barrier tags, separately per hand. `end5t.mcfunction:1-2` / `end25t.mcfunction:1-2` remove Sun/use tags only. Suppression does not itself call cleanup in these source lines; native lifecycle must be reconciled deliberately with existing owners.

## Eternal Life

`items/arcanums/select{,2}.mcfunction:4`: either hand; requires scoreboard `hplvl < MaxHealth` and mana >= configured 500 (`function/config.mcfunction:7`). `healing.mcfunction:1-5`: grant heal advancement if hplvl 0..1, apply instant_health for one second amplifier 0, spend 500, five hearts (spread 2), experience pickup sound volume/pitch 1. No cooldown. Both hands independently dispatch using scoreboard health; do not silently turn this into a single shared activation or replace score timing with fresh native health without checking intended behavior. Instant health is an effect command, not explicit setHealth/heal-to-full.

No offensive source effect. Preserve existing attackers' kill attribution when healing; healing must not erase damage/indirect-cause ownership.

## Absolute Barrier

`config.mcfunction:4-5,119`: base upkeep 40, activation 100, **global** temporary threshold `barrier_arcanum_mana_tick2` initially 10. `barrier{,2}.mcfunction:1-3`: inactive hand pays activation then can immediately upkeep in the same dispatch; active hand admits against the current global temporary threshold, calls tick, then removes its active tag if remaining mana is below the now-updated threshold. `barrier_tick.mcfunction:1-12`: one-second resistance amplifier 4; dual barrier holding clears a 3x3x3 box (relative -1..1 horizontally and y=0..2) using air strict, and while sneaking additionally clears 3x3 at y=-1. Six electric-spark faces centered at y=2.5,-1.5 and side offsets 1.5. Recompute global threshold as 40 + this player's `manaregen_visual`, then subtract it. Both hands can pay activation and upkeep independently; dual clearing is repeated.

**Decision required:** admission reads the threshold left by the previous player/hand/tick, before recomputing this player's actual charge. It can admit too cheaply and make mana negative. This is observable source logic, not permission to silently replace admission with 40+own regen. Also resistance V is what source grants; do not expand it into absolute cancellation of every damage type merely because lore says all damage except void. Source effect lasts up to a second after dispatch stops.

Kill attribution required for block removal: displaced support / falls / void deaths must retain the barrier holder as cause; dual repeated removal must not lose ownership. Resistance itself creates no outgoing hit.

## Eye laser: defer source oddities

`select.mcfunction:16` only mainhand; **no mana cost/admission**. `eyelaser_pre.mcfunction:1-4` marks caster, resets Distance=0, starts at eyes, removes caster mark. `eyelaser.mcfunction:1-7`: increment per 0.5-block sample; red dust hidden from caster until Distance>=30; one mob-tag target per sample, excluding caster/spectator, generic damage 0.05; chance10 creeper priming sound (0.2,1.5); any non-air sets Distance=201 via `eyelaser_kill.mcfunction:2`; recurse while <=200. With clear air this yields 201 samples through 100 blocks. No team predicate in this version. Target selectors use dx=dy=dz=0 plus distance<=7 and another offset overlap test, not a clean ordinary ray/AABB.

`eyelaser1.mcfunction:4,7` is a different mask path (`armor/mellstroy_mask/tick.mcfunction:20-23`), adding !same_team/!same_id and limit684 / stopper radius20. **Bug requiring decision:** obstruction resets Distance to201, which remains <=684; successive solid samples repeatedly reset it, so the ray does not stop at blocks and can recurse to command-chain limits on a long solid path. Do not import this as the arcanum implementation or normalize it silently. Source eye damage lacks attacker; native direct and delayed knockback/fall/void consequences must credit caster. No eye loot entry was found in the bounded transformables_arcanum search; acquisition/presentation remains unverified.

## Babylon: later batch

`players_2t.mcfunction:10` runs the misleadingly named `items/arcanums/5t/select.mcfunction:1-2` every two ticks for either `arcanum2t` hand; stopper_magic radius20 gate, no nomagic-player gate on that line. Each hand requires/pays300 (`config.mcfunction:9`; `5t/babylon_gate.mcfunction:8`). Spawn origins selected by outer random1..5: eyes-relative (2.5,2,0),(-2.5,2,0),(0,3,0),(4,0,0),(-4,0,0).

`5t/babylon_gate/1-5.mcfunction:1`: three arrows with raid_omen amplifiers101/109/110 duration20 and authored custom colors; trident; firework LifeTime100 / ShotAtAngle1 and four authored explosions. `sel1-5.mcfunction:1-16`: independently choose projectile, derive velocity from temporary origin marker with local offsets (-.2,.2,3),(.2,.2,3),(0,0,3),(-.5,.3,3),(.5,.3,3); assign Owner from caster UUID, clear temporary tag, emit dust transition.

**Bug requiring decision:** outer selection and all inner selection functions mutate the same `#random5` score. Returning from an inner selection changes subsequent outer branch conditions; one invocation can execute additional later origins. Do not replace with exactly one uniform origin silently. Selectors also choose an arbitrary tagged nearby entity (limit1, no sort), and kill all reikast markers. Native attribution must preserve projectile owner plus all special-arrow/firework/trident and delayed effects; raid_omen special handlers were not expanded in this bounded audit, so Babylon is not implementation-ready.

## Presentation evidence

`loot_table/transformables_arcanum.json:125-210,214-288,500-574` defines source entries: repeating_command_block, tier_a, gold nonitalic translated names; models `lbc:arcanumbarrier`, `lbc:arcanumhealing`, `lbc:babylongate`; flags transformable/arcanum plus arcanumt for first two or arcanum2t for Babylon. Preserve complete original lore components/score arguments when porting; this audit does not claim full lore validation.

Author resource pack read only: `C:/Users/yarik_temp/AppData/Roaming/FreesmLauncher/instances/26.3(1)/minecraft/resourcepacks/all_in_one_1.21.8/assets/minecraft/lang/{ru_ru,en_us}.json:681-695` already supplies barrier/healing/Babylon names and lore keys. Reuse keys and styles without duplicate translations. No pack files edited. Visual/model asset existence beyond these source references was not validated.
