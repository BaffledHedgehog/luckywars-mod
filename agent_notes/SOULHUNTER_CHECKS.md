# Soulhunter / ordinary Soulless native fixture contract

25 October 2026. Implementation: [SoulhunterChecks.java](../src/main/java/me/skywars2/game/SoulhunterChecks.java). Source contract: [SOULHUNTER_SOURCE.md](SOULHUNTER_SOURCE.md).

This file describes fixture code, not a qualification result. This agent did not build, launch a server, or run a fixture. Parent integration owns dispatch registration and actual runtime evidence. Public entry points are `run(game)`, `stopProbe(game)`, and `cleanup(game)`; the latter two must surround a real match stop.

The suite requires native active TestAlpha/TestBeta in one dimension, closed inventory menus, no pending frost or combat attribution, normal server ticking, no Time Stop, and exclusive empty Soulless service state. It rejects unrelated entities or unloaded chunks in the terrain region. A bounded 25 by 26 by 25 region above the participant is snapshotted, including block entities. Fixtures restore inventory, both grids and cursor, effects, abilities, position/velocity, health, food/XP, tags, native attacker fields, profile/configuration, teams, relevant scores, item/custom statistics, advancement criteria, and Soulhunter's second cadence. Added actors, drops and XP are removed. Native effect ownership and combat state are explicitly cleared before restoring participants.

Cases exercise these boundaries:

- Native loot identity, native `MaceItem`, authored model/tier, prerequisites and six reward identities.
- Real 2x2 and 3x3 native menu preview and ResultSlot pickup. Extra or duplicate donors are rejected; a successful pickup consumes exactly one of each stacked donor.
- Airborne sprint's full pitch-bearing vector, exhaustion at zero, grounded recharge while unequipped, cap/rearm at 60, actual `jumpFromGround()` integration, cooldown seconds, Time Stop and HUD.
- Native first player melee and a falling smash against a fresh native cow, compared with a fully cooled grounded strike.
- Native mace item RMB input, independent dash/splash cooldowns, replacement dash velocity and zero-mana use. Repeated dispatch remains subject to its branch's cooldown.
- Every cell of the seven-rectangle source splash mask at y0..5, preserved surrounding cells and real chest-content drops. Splash damage guards cover caster, allied player, protected role and active Greed. Explicit support/void deaths verify owned causality and a newer accepted player hit's priority.
- Actual offhand wand dispatch, strict 1500 admission, WILD eye-radius25 boundary, caster/allies, protected role, additive five-second ghosting, native gravity modifier, horizontal ghost sprint, invisibility, Greed, Time Stop and modifier expiry.
- An accepted native HurtTime10 hit emits a native owned Vex; HurtTime9 does not. Conversion creates exactly one falling reward from the six identities, with ownership established by the later deploying holder.
- All six full-HP vulnerable pig/display pairs, native gravity/AI/motion stepping, source floored motion conversion, exact matching display follow, Time Stop, pair unload and foreign-owner preservation.
- Field activation boundaries for magic damage, native instant healing, WILD lift, mana floor and ResistanceV/void protection; types1/2 retain separate +1 tick and +20 second age increments. A real `mana.secondPlayer` dispatch teleports y-1 to320 before spending the protection counter2->1->0; expiry leaves y-1 unchanged; real antimagic retires both members of a pair.
- Type5's warmup and native owned in_fire20 hit, host lowering, exact 4945-cell sparse source template and one fractional placement: changed cells must come from its fire/lava/void-air palette, with both selected and omitted cells.
- A real match-stop probe seeds all six pairs plus ghosting, then checks changed epoch, all twelve entity removals, and status cleanup.

Explicit loops advance named service boundaries only; they do not claim that world/server time, native client inputs or full autonomous combat elapsed. Native pig `tick()`, player attack, jump, item use and menu pickup calls are identified separately. The suite does not qualify client pixels, real disk restart/disconnect/unload, peak load, arbitrary terrain, probabilistic six-way reward frequencies, every possible field stacking combination, or all consequences of long-running type5 fire/lava physics. Type5's fractional result is checked against allowed source cells rather than a fixed random seed.

Academy Soulless modes are outside this batch. Protected pre-existing AcidAntDropChecks, HelicopterEncounterChecks and LbcRerollCrafting are untouched.

Fixture correction after parent runtime attempt: cows now explicitly receive max/current HP20 (native cows default to10); this keeps a15-damage splash victim alive for the later terminal-void assertion and makes the stated20-HP field boundaries valid. Exact26.3 merged sources declare attackStrengthTicker and itemSwapTicker on LivingEntity, not Player; the native melee fixture sets/restores both inherited counters for equal fully cooled strikes. No production behavior was weakened to accommodate these failures. Parent rerun remains required.

Type5 mask correction after the next parent runtime attempt: native BaseFireBlock.onPlace synchronously removes fire when canSurvive is false. A selected source FIRE cell may therefore finish as ordinary AIR. The fixture allows this native terminal state only for source FIRE cells, while retaining sparse-mask membership and exact block matching for lava/void-air cells. Assertion failures report actual and expected states. This accepts the native fire callback rather than changing production placement.


## Integrated qualification (25 October 2026)

Build `fa036823` passed86 recorded results in two actual mode1 matches (151.33s),
including all14 Soulhunter-family cases twice, the existing shared regressions,
real match-stop probes and cleanup. Exact316 source files/881 classes/2812
resources match the tested ready JAR. No new ERROR lines beyond the existing
fake-bot Polymer packet-context pair. Evidence: `build/verification/soulhunter-final/`.
Main installation was refused while its session.lock was held. The user then
requested leaving this build ready, so the main JAR remains `2558af6b`; no restart
or replacement occurred. This supersedes individual task-agent pending statuses.
Native fixture checks include explicit tick boundaries, not real client pixels,
actual disk restart/disconnect or peak-capacity qualification.
