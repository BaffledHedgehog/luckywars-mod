# Complete Berserker armor

Source closure is recorded in [NEXT_ARMOR_SOURCE_2026_10_28_B.md](NEXT_ARMOR_SOURCE_2026_10_28_B.md).
The finite family is four equipped pieces, the random root, five-draw pre table,
four-distinct-piece full table, existing normal/Ultra reforges and one hurt passive.
Do not enable unrelated Lucky Block pools merely because they reference this set.

## Author decisions (28 October 2026)

Antimagic suppression:

> Да, только антимагией

Health threshold:

> убери вообще проверку здоровья, пусть на любом хп хил будет

Already-granted effects:

> ? там разве не мгновенное исцеление? ну вообще глушилка не должна очищать уже имеющийся эффект

The source helper gives BOTH Instant Health I (default one-tick instant instance)
and Regeneration II for five seconds. This was explained in chat; preserve both.
The author's removal of the health threshold supersedes the earlier question
about a delayed `hplvl` score, rounding and absorption. No health/absorption gate
or private health-history tracker remains. A full-health wearer still rolls the
proc; native healing naturally respects maximum health.

## Implementation

`LbcBerserkArmor` joins the existing `EntityHurtPlayerTrigger` bridge. It shares
the armor owner's eligibility predicate and captures pre-hurt admission through
the existing wrapped native call. Accepted native shield/absorbed events count;
rejected damage does not create a second callback. There is no independent
damage listener, cooldown, tick service or Time Stop replay.

Every actual equipped head/chest/legs/feet stack must have raw `berserk:1`.
Carried armor cannot replace a missing physical slot. Presentation, vanilla
base item and extra slot flags are not runtime identity. The player-native
random source performs a 70% draw. Only the existing MAG field blocks new
activation; TECH and personal mana-cast suppression do not suppress this passive.
Already-granted effects keep their normal shared duration/layering behavior.

The successful proc adds sourced Instant Health I for one tick, emits three
heart particles at y+1 with spread .5/1/.5 and zero speed, plays wolf growl
on MASTER at volume/pitch1, then adds sourced visible Regeneration II for100ticks.
The native signed-effect implementation and item-effect cap remain authoritative;
no direct health assignment or replacement regeneration formula is introduced.
Other effect sources and stronger/longer layers are preserved.

Original seven tables become public ready entries. Pre/full packaging is added
explicitly; all four existing payloads remain byte-exact, including their leather
bases, negative armor/toughness, positive melee/movement modifiers, per-slot IDs,
durability400, names/lore/color, tier_B style, models and equipment asset. The
original Berserker advancement is reused. Existing normal slot-reforge and Ultra
family-reforge acquisition remain the concrete native routes.

Existing author RP assets and translation keys were read-verified. No RP edits,
new template copies, model replacements or source-server writes are required.

## Verification and limits

`tools/smoke_berserk_armor.py` runs Berserk plus adjacent shared incoming-armor,
attribute, signed-effect/layer and Time Stop suites in two actual native matches.
The bounded Berserk fixture checks source readiness/payloads, real normal/Ultra
reforges, native effective signed attributes, partial/carried/wrong-slot cases,
the .7 chance boundary, all-health and absorption combinations, the native
one-tick Instant Health effect and regeneration layers, particle/sound packets,
actual accepted/rejected/absorbed/shielded damage and MAG/TECH/Greed/role guards.

The fatal-admission boundary is a native criterion call at temporary zero health
inside a captured attempt; it is not a full player-death/elimination scenario.
No physical-client rendering, arbitrary-map, latency or natural TPS claim.
Immutable artifact/qualification and stopped-only installation receipts are in
`build/verification/berserk-armor/`.

The initial `95ea719c` batch passed adjacent suites in both matches but its new
Berserk fixture incorrectly expected an instant-effect tick to heal under Greed.
Own-sourced grants are accepted, while the existing shared signed instant formula
suppresses this tick benefit under Greed; actual hostile damage is rejected and
cannot trigger Berserk at all. Correct only the fixture expectation. Do not
change global Greed/effect rules for this item. Initial results/log/proof are
retained as `initial-95ea719c-*`.

Final immutable build `22bd9562` passed28recorded results in two actual native
matches (101.86seconds), normal owned-child shutdown and exact fixture snapshot
restoration. All554sources,1577classes and3236resources plus build.gradle match
the tested JAR.198preexisting author paths and29live override JSONs are exact.
Only the Berserk fixture source/class changed after the initial failure; all
production bytecode is identical. Installation uses the existing stopped-only
worker, with no forced main shutdown or restart.
