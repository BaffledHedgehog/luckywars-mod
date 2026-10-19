# Mystical Artifact Passive Regression Checks

The passive slice adds three operator checks in `GameChecks`: source loot and Polymer roundtrips for `mystical:1,type:6` and `type:8`, deterministic passive behavior during an active match, and match-stop typed-state cleanup. The focused runner entry is `tools/smoke_lbc_loadouts.py --artifact-passives-only`; the full loadout runner appends the same slice after the current magic/beacon slice.

The active check calls the native five-tick phase directly. It covers all 36 inventory slots, offhand, armor exclusion, cursor, 2x2 and 3x3 crafting inputs, type-6 clear-all and combined ordering after type 8, type-1 coexistence, native Polymer beacon suppression, Creative eligibility, and Creative observer exclusion. Metabolism assertions follow the source threshold `ceil(health + absorption) < (int) maxHealth`, including fractional health/max-health values and absorption. Native `MobEffectInstance.tickServer` advances Regeneration II over 100 ticks while the artifact refreshes every five; the fixture checks four native heals and expiry within 45 ticks after removing the artifact. It also checks stronger-amplifier merging and equal-tier duration/ambient/visibility behavior. The lobby cleanup check exercises the developer Creative bypass with type 8 after verifying stop cleared passive tracking.

The user explicitly requested “Исправить лечение”: the implementation keeps native Regeneration II so its 25-tick healing interval is reached, extends a matching effect only when its remaining duration is at most 20 ticks, and keeps its total lifetime bounded to 45 ticks after the last refresh. No manual healing scheduler is used.

The root built and ran the final implementation: the focused four-group slice
passed in 96.64 s; the full 33-group, 12-match run passed in 266.95 s on the
same JAR. The active fixture leaves a real tracked holder and native effects
for the subsequent stop command, so cleanup is verified against nonempty state.
Evidence, installed hash and limits: [SERVER_TESTS.md](../SERVER_TESTS.md).
