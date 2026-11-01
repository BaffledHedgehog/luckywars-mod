# Pursuit kits and shared ray scheduling ? 1 November 2026

Complete release: Bouncing Poop kit11, redesigned Deus kit21, source Govnomancer skill9, and fair shared ray scheduling. User leaves stack sizes unchanged. The prior staged preparation is superseded; all suppression replies are incorporated.

## Behavior

- Bouncing Poop is physical and unjammed, including MAG, TECH and the strong artifact. Source curved flight, one native item-display actor/attachment, wall reflection, strict soil ladder, body payload, 600 active ticks and source skill9 mega variant remain. Actual used hand alone spends one unit. Item-used statistics are recorded. Mega rubble uses the existing native Govnomet falling-dirt factory with captured author/epoch and finite match lifecycle; owner absence does not cancel these accepted children.
- Deus is an electro-whip, not a player teleport. Each active frame performs one broadphase query, at most 140 exact .2-block segments, and visibly bends even under straight aim. It locks a target until actual native hitbox contact, then seeks the next, up to three distinct targets; blocks do not stop it. Each tenth active frame applies 2 ordinary electric damage with native hurt cooldown and mitigation. Accepted hits pull opposite the incoming curve tangent, including signed knockback resistance and native player/Carpet velocity synchronization. It cannot trigger real-melee weapon/Wrath/Vanity counters. Particle transmission is reduced to roughly .6-block spacing plus contacts/endpoints; collision resolution is unchanged.
- Only TECH and the strong antimagic artifact suppress Deus. Ordinary MAG does not. Active phase and cooldown pause, retaining legacy semantics. The canonical deus_cd score owns all sustained state: 260..200 inclusive is 61 active frames/seven damage pulses; 199 subsequent active ticks are recovery. No separate persistent runtime/checkpoint engine, mana cost or durability cost. Both configured hands use the common dispatcher; busy input cannot restart the clock. Holding the weapon is unnecessary after launch. Death/clear and match stop reset the score; diagnostics are not gameplay state.
- Actual source kit menu slots/gates now select and grant kit11 (16 Bouncing Poop) and kit21 (one Deus). Skill9 uses source page10000/slot8/full-shitty-armor gate; its native effects_changed passive removes complete Nausea/Poison/Wither chains and ignores jammers. All other existing skill9 consumers stay with their current owners. Corrected the loadout's formerly generic skill payload so only skill5 receives its Resistance chain.
- Shared loaded-ray work now rotates by author UUID. Within each author, service cycles newest fresh, oldest fresh and resumed FIFO work. New casts start promptly while older accepted work retains guaranteed service; all owner/lane turns/cursors restore exactly. Normal budget rises 64?80 quanta, up to96 with shared terrain headroom; time window 3?3.5?4ms, with a small busy-tick progress floor. No forced chunks, generation or accepted-cast drops. This queue computes over already loaded chunks.
- Existing RP models, translations and HUD glyph are reused; no RP/template update is required.

## Native evidence

Qualified artifact: `build/verification/pursuit-whip-final-2026-11-01/pursuit-whip-057b67aa.jar`.
SHA256: `057b67aa422a8514d607dc42dace5d8ea2ef8a01c99c63682fbbd857a74dfc42`.
`qualification.json` maps ten distinct scoped groups to their retained native receipts:

- `deus-whip`, `next-shooters`, `pursuit-kit-menu`, `govnomancer-menu`: 057b67aa; final child succeeds in two fresh mode1 matches.
- `bouncing-poop`: 1b115e91; actual native/raw hands, real MAG/TECH/artifact controls, source visual/attachment reload/image, scheduled curve/bounce/soil/body/shared cap/mega children/lifetime/TimeStop.
- `ray-queue-fairness`, `govnomancer`, `raw-raycast-spells`, `steampunk-hook`, `vanity-rollback`: 97f4eb49, individually successful scoped groups. Fairness uses 192 long Alpha rays and a Beta near hit within two real ticks, continuous fresh traffic/oldest/resumed progress, all65 source samples, freeze/cancel, no forced chunks, exact128-event checkpoint replay.

Every production class and every resource is byte-identical across these evidence artifacts. Later changes touch only fixture classes. All2379 compiled classes and7175 processed resources match final JAR; native bundled Fabric dependencies preserved. All9587 source/build inputs frozen;224 author paths,31 live grimoire override files and installed main EF417EDD unchanged before installation. Every child exits normally with exact snapshots and additional level/ticket bytes restored; no retained locks. Fixture uses port25577 only. No peakTPS, physical client frames, arbitrary-map or latency claim.

## Retained failures and corrections

Initial compilation rejected a fixture call through a private connection type; reflection now explicitly views it as Object. 97f4eb49's Deus fixture re-clipped an exact contact endpoint and falsely missed the boundary; fixture-only 1e-9 tolerance fixes roundoff without widening production rays. Further retained receipts 05a4751b/c98b08bf/1b115e91 identify cleanup and observation issues: native TECH marker migration created a persistent beacon during the test; cleanup now restores that block and shared index, and the no-Deus-pose measurement window clears legitimate TimeStop restoration packets before resuming.

The older NextShooter fixture also assumed forget deleted previously launched Boss arrows, depended on an unprepared distant second-player chunk, and expected WitherXI despite the already-agreed shared item cap. Explicit episode cleanup, a resident second-player position and shared capped assertions correct those expectations. The unchanged installed EF417EDD independently reproduces the old WitherXI failure (`ef417edd-results.json`). Final corrected full NextShooter regression passes, including the preexisting Govnomet path. Historical failures are retained, not qualified wholesale; scoped successful production-identical evidence is named above.

## Installation

Use the shared `tools/install_when_stopped.py` with this qualification only. Never stop/restart the main JVM. Installer retains prior EF417EDD rollback; installation status and receipt are recorded after the normal-stop worker completes.
