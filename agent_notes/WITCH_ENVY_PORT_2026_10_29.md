# Witch Envy passive stage — 29 October 2026

Staged source Envy passives and real counter-owner integration. Not a complete
Witch Gens or Chaos release. The original full25 Chaos transitive gate remains;
main stays `d9087f0a`. No staged JAR installed or queued for installation.

## Behavior and shared owners

`LbcWitchPassives` consumes `sin_envy` under its existing gameplay-role,
ordinary-dimension and TimeStop admission. It executes the literal original
`@p[gamemode=!spectator,tag=!sin_envy]` selector. **Correction to the earlier
source audit:** unbounded native @p is global across player dimensions, sorted
by coordinates; neither its nearest ordering nor gamemode/tag options limit it
to the execution dimension. A distance/AABB option would limit the dimension,
but the original has neither. Native EntitySelector.findPlayers and
EntitySelectorParser's p branch establish this; the actual cross-dimension test
confirms it. No new donor radius, team filter or random selection.

Each tick applies the original ordered19 MAX and16 MIN comparisons. The
production lists were compared directly against the original envy_tick source.
`LbcSourceCounters` is a view over existing owners, with no maps or ticking:

- Cyber/Yellow charged and charging counters update separate native Charge
  members; Terra updates its existing sword-charge map.
- Scared/lookback/twitching reduce the existing Affliction; zero uses its own
  release/clear path. Other afflictions and causal ownership survive. Shock
  reduces the existing pulse count, preserving its phase/owner, or removes it.
- TimeStop converts its actual remaining ticks to source seconds; Reactive to
  source5tick units. A reduction retains the existing subinterval rather than
  restarting either clock or ending a world stop.
- Mobgun, KFC gun, Magnum/SLR, Phantom Knives and Attack Helicopter update their
  existing cooldown or burst object. No refunded ammo, new activation or replay.
- Pine's existing six-volley burst now retains its original source counter20,
  decrement4 per two-tick volley. This represents Envy reductions precisely
  without rounding to a whole number of future volleys. coldown18 is ordinary
  ammo, coldown18b is best ammo; they are not left/right-hand timers. Existing
  paired-hand behavior is retained. KFC increments that same source counter by1,
  rather than inventing a whole extra volley. No second queue is introduced.
- Truly scoreboard-owned values use the native score. A missing operand does
  not compare or create a score. Native-owned absent states have their owner's
  existing idle value. Successful native writes also synchronize the source
  score for existing score consumers. Divider retains its existing scheduler.

After comparisons, source slots9..35 then0..8, offhand and four armor slots are
copied only if empty. ItemStack.copy preserves count and components; the donor
is untouched. One inventoryChanged/equipment notification plus menu broadcast
occurs only if something was copied. There is no per-tick full inventory reset.

The real one-second boundary applies the original29 effect rules in order:
16 presence-based fixed grants and13 clear-unless-present rules. These do not
copy the donor's level/duration. Instant health and saturation use native instant
duration units; ordinary grants40ticks, night vision500; InvisibilityII hides
particles. ItemEffectLevels caps the original Regeneration amplifier20 at5.
Shared signed effects/layers and existing immunity rules remain authoritative.

Unanswered suppression and source-second TimeStop differences remain explicitly
recorded in PENDING_QUESTIONS; no guessed blanket MAG/TECH policy. Acquisition,
other authorities, active abilities, summons and the complete reward chain still
require implementation/qualification before release.

## Native evidence and limitations

Candidate `69b0bca4`:
`build/verification/skywars2-witch-envy-69b0bca4.jar`, SHA256
`69b0bca45f77dfa6bdcd7df148b15c67d27107bfa09c9128f395ef4ba3a1d480`.
Offline build passed.10 gameplay suites plus5 setup checks passed in one actual
mode1 match,30.99seconds: witch-envy, witch-passives, pinegun, phantom-knives,
kfc-counters, signed-effects-math, effect-layers, time-stop, time-stop-contacts,
projectile-self-hits. Normal owned-child shutdown, exact disk snapshots restored,
no child/fixture locks/cleanup failures left behind.

WitchEnvyChecks uses actual native participants/listeners and the shared server
tick, with full borrowed-state restoration. It checks separate live charge
members, selective psychological/shock reduction including zero, untouched Piss,
native cooldown owners, paid Pine/Knife queues shortened without replay/refund,
missing scoreboard operands, actual tagged-authority MAX/MIN, independent item
component/count copies and occupied-slot identity, offhand/armor, source-second
fixed effects/caps/clear branches, and literal cross-dimension donor selection.
No private clock rewriting or fake-player subclass.

First rejected `dfa3cc25` exposed the audit's wrong dimension assumption and two
stale pre-WILD projectile assertions. `7949ea8f` and `644f73a2` then exposed the
Pine fixture's absolute global-arrow count in a later subfixture. Its scoped
teardown now unregisters removed entities and the independent paired subfixture
asserts exactly +36/+42 against its own starting registry. Old tests now expect
physical arrows/knives to hit allies, including actual knife damage, while
retaining observer protection. Rejected artifacts/proofs are retained; none is
qualified or installed. Production friendship policy was not weakened to satisfy
old tests.

Final proof directory: `build/verification/witch-envy/`.5761 frozen inputs:
604 Java sources,1745 classes,3411 processed resources and build.gradle. All
packaged class/resource bytes match, with the established Loom fabric.mod.json
nested-SGUI transformation verified separately.16 original source anchors
hashed, ordered counter lists checked against source.198 protected author paths,
31 live grimoire overrides and main JAR unchanged.666 normalized WARN/ERROR lines
exactly match the previous native stage: no added or removed diagnostics.

This is bounded server gameplay evidence, not every possible effect/slot/counter
combination, complete Witch acquisition/actor lifecycle, physical client rendering,
real socket/rejoin/network latency or peakTPS qualification. `qualified=false`,
`releaseEligible=false`; full Chaos gate remains closed.
