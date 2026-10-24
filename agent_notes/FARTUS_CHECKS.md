# Fartus native fixture design

Status: qualified in the integrated root build and two-match run; exact evidence and limits are recorded below.

Source root: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/firtusnadristailo/`.

- `cast_2.mcfunction`: equal random branch, either five bananas or five labels. `cast_etiketka.mcfunction`: labels at yaw offsets +30,+15,0,-15,-30 degrees.
- `cast_banan_shoot.mcfunction`: physical banana, lifetime100, friction.8, air friction1, bounce.9. Exact shared-engine gravity/initial velocity must be audited separately.
- `banan_impact_success.mcfunction`: native Blindness/Slowness/Darkness/Weakness at amplifier99 for40ticks, plus Instant DamageIV. The source command requests40seconds for the instant effect; native application semantics must be preserved explicitly.
- `etiketka/etiketka_damage_at.mcfunction`: WitherII, PoisonIII, SlownessI for300ticks. Labels stop on a solid block and expire at60ticks. Three .4block detector steps per source tick.
- `cast_upgraded.mcfunction`: spend3 sword charge and emit five homing bananas; no ordinary mana debit in that function.
- `check_upgrade_set.mcfunction`: source sequence state machine reaches4 with branches A,A,B,B (first establishes state1, then equal,different,equal). `check_upgrade.mcfunction` zero-to-one fallback is an explicit source oddity, awaiting root/user ruling.

Planned public fixture entrypoints: `FartusChecks.run(GameController)`, `stopProbe(GameController)`, `cleanup(GameController)`. Root owns command/script wiring and builds/server verification.

Checks will use the actual native loot and wand input path, an actual Academy interaction panel, native projectile movement/contact, and actual native direct/delayed mob deaths. Both initiating hands, ordinary/Academy budgets and charge, exact trial stack, promotion, actual reflection/current owner, allied-player safety, friendly-mob physical hits, technical roles, observer, Greed, antimagic, noMagic, dimension/entity Time Stop, reload/epoch retirement and actual match stop require coverage.

Fixture isolation follows BabylonChecks/DefensiveArcanumChecks. Reject participants with open menus, existing frost or pending indirect combat before saving or mutating. Save full inventory/crafting/cursor, effects/tags/native attacker references, attributes touched, complete profile, team, health/hunger/experience/mana/scores, all advancement criteria and relevant statistics. Snapshot every potentially changed block with MapManager including containers. Dispose fixture projectiles, helpers and all newly created item/xp entities. Restore config and participants even after failure. Cleanup must never clear pre-existing unrelated combat state; rejecting it is a precondition, not permission to erase it.

Explicit synchronous native calls establish dispatch boundaries; they do not claim elapsed real server time. Runtime evidence will be reported separately after root verification.


## Implemented fixture (runtime not yet executed)

`src/main/java/me/skywars2/game/FartusChecks.java` exposes `run`, `stopProbe`, `cleanup` for root-owned command and smoke wiring. Coverage now includes ordinary/Academy native singleton loot, six lore rows, S/X/model/flags, both used hands, strict1250 admission, charge3 with no mana payment, initial eye-positioned projectile/display, exact five-angle label fan, gravity.08, cube collision/rebound.9, three.4 label substeps, homing bend, source100/60 active lifetimes and no retired reload resurrection.

Native real contacts cover banana48 damage, undead32 healing inversion, four40tick amplifier99 debuffs, exact label300tick payload, actual native direct and delayed Wither deaths, current projectile owner, later accepted real-hit priority, and ignored weaker Wither preserving the existing stronger effect's author. Native gameplay guards cover noMagic, entity/dimension Time Stop, physical versus magic antimagic continuation, allied-player safety, friendly-mob physical damage, technical roles, Greed reflection and observer input. Academy admission/promotion uses an actual panel and actual offhand random releases; the unused mainhand stack cannot progress. Promotion checks exact X/palm/attributes. Accepted native melee charges the sword; indirect magic does not. Five carried/offhand govnoq yield69 damage bonus and removal restores19. Both2x2/3x3 native craft menus check ordinary/Academy Terra donors, extras rejection, full inventory refusal, exact pickup consumption and remaining shift craft.

Source decision now preserved: trial branches A,A,B,B; a failed transition resets to1. Banana lifetime100 is consumed by the generic source raycast selector. Author rulings and native core details belong to FARTUS_PORT.md. Chef is independently being redesigned by root under a new user requirement; these checks do not exercise the obsolete display Chef prototype.

These are assertions implemented in a fixture, not passed runtime evidence. No build, server launch or deployment was performed by this agent.

Root fixture correction: same-frame Academy duplicate callbacks remain ignored by the real guard. The first actor completes its offhand trial; the second actor promotes that transferred ready stack, then returns the evolved sword for charge assertions. This exercises native promotion without altering world time or clearing production debounce state.


## Integrated runtime qualification (24 October 2026)

Build `b0dfd5ca` passed74 recorded suite results in two actual mode1 matches (168.90s), including Fartus/Chef and the existing shared regressions. Exact 311 sources/857 classes/2797 resources match the tested installed JAR. Main installed while stopped, rollback `0fb48f1a` retained, server not started. Evidence: `build/verification/fartus-final/`. Native synchronous tick boundaries are explicit simulations; no elapsed ten-second wait, real client pixels, actual disk restart/disconnect or general-world/load guarantee is claimed. This integrated result supersedes the earlier pending-status statements by the individual source/check agents.
