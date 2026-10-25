# Danmaku ordinary and Academy native port

Implementation: `LbcDanmaku.java`. Authoritative source geometry and presentation:
[source audit](DANMAKU_SOURCE.md), [resources](DANMAKU_RESOURCES.md).
User decisions, including exact original quotes, are retained in
[DANMAKU_DECISIONS](DANMAKU_DECISIONS.md).

The staff adds 20 to the shared remaining counter per paid hand. Two accepted
hand activations add 40. Main/offhand emissions retain source parity and exact
phase geometry; the actual corresponding hand must currently hold a Danmaku
staff. Removing it suppresses emission while the counter continues. A separate
explicit external-source queue permits future consumed items to continue
without held-stack checks; no external acquisition source is enabled here.
Bombs require score 50, with no mana admission or spending. Ordinary paid casts
use configured `danmaku_staff_mana_use` (default 1000), existing mana admission
and cast hooks. Personal nomagic is a regeneration state, not admission.

Exact generated pattern literals contain 40 ordinary main, **35 ordinary left**,
89 Academy main and 60 Academy left cards per 20 remaining ticks. The audit's
summary of 31 left cards was arithmetic error: shoot_10l has 17 cards, following
18 from its first nine phases. The literal source was preserved. Academy left
uses source eye height; other variants start at eye minus 0.5. Authored local
coordinates are transformed using the caster's actual yaw and pitch.

Native item displays use source card models, fixed context, scale 0.5 and
quaternion (1,0,0,1), with no explosion or native projectile force. Each active
tick runs three 0.4 steps, and homing steers at each step. Eligible homing
candidates are gathered once per owner per tick; only enemies can be selected.
Physical collision uses swept native hitboxes and source non-air block cells,
respecting the user's first-contact ruling, protecting allied players while
allowing friendly mob contacts. Greed reverses direction and transfers owner
before damage or score. Generic damage is 0.65 and uses the imported source bypasses_cooldown tag. Its source omits an
attacker; captured owner credit preserves kills without introducing an attacker
impulse. Bomb magic retains its explicitly authored caster.
The score remains source +3 per physical impact. The user corrected the
blocked-hit question premise: source generic damage already bypasses cooldown.

Bombs retain both red-glass shells, signed scale 0.2 times age, global-plus-local
age increment 4 per tick and radius truncation floor(floor(age*1.2)*0.05).
Captured charge controls age expiry. Every active tick applies normal native
1 magic damage without clearing hurtTime. Enemy projectile actors are discarded
before damage, preventing mob-projectile death drops; allied/own/technical actors
are retained. Nonentity rays use the shared `clearDanmakuRays` bridge. Blocks
are not edited. Charge >=1500 immediately readies the actual ordinary trial
stack used for the bomb, including an offhand trial stack.

Ownership is exposed to shared attribution, homing curses and projectile
purging. Epoch, owner departure/death, antimagic at actor position, unload,
orphan reload and match stop retire helpers and paired shells. Time Stop
pauses the actual display lifetimes, flight, bomb growth and volley timer.
Spawning identity guards allow synchronous entity-loaded callbacks without
discarding a freshly admitted display. HUD retains source glyphs/colors.

Package fixture APIs expose cards, bombs, remaining counter, direct card spawn
and actor counts for meaningful native collision/lifecycle checks. This note
does not claim build/runtime qualification: root owns compilation, actual match
checks and artifact preparation. READY ONLY; no installation or server action.

## Integrated qualification

Build `04396ca4` passed 24 recorded suite results
in two actual mode1 matches (62.1 seconds), including all six actual-tick hand patterns, bombs, source cooldown bypass, Curse/electro bridges and actual match-stop cleanup.
Exact 346 Java sources / 952 classes /
2816 resources match the tested artifact.
Ready artifact: `build/verification/skywars2-danmaku-04396ca4.jar`. Evidence: `build/verification/danmaku-final/`.
The main JAR remains unchanged at2558af6b; READY ONLY is the user's existing choice.
No author RP edits or translation fragments are required. Native fixtures do not
qualify real client pixels, actual disk restart/disconnect or peak-load performance.
