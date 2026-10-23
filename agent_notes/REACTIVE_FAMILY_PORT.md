# Reactive Shish and Shish Wall port

Source: [read-only audit](REACTIVE_FAMILY_SOURCE.md). Native implementation:
[LbcReactive](../src/main/java/me/skywars2/game/LbcReactive.java),
[item presentation](../src/main/java/me/skywars2/game/LbcReactiveItems.java).

## Ready acquisitions and behavior

- `lbc:ported/reactive_shish` yields `skywars2:reactive_shish`.
- `lbc:ported/shish_wall_wand` yields `skywars2:shish_wall_wand`.
- Reactive Shish + Academy Wall Wand crafts one Shish Wall in both 2x2 and 3x3.
  Native result-taking consumes one item of each selected donor; extra ingredients
  reject the recipe. Preview never consumes anything or rolls randomness.

Reactive Shish uses native Levitation amplifier 15 for 20 ticks, costs no mana,
and shares a 125-active-tick cooldown across hands. The original score decrements
once per five ticks: actionbar retains its raw 25-to-1 units and source ready/
cooldown icons. At 30 active ticks it places the original 3x3 nonpersistent spruce
leaves at the player's current floor Y-2, or fixed Y319 at floor Y>=322. Putting
away the rod does not cancel an accepted cast. Existing overwritten-container
contents are dropped; terrain-caused harm retains the caster. Source particles
and grass-step sound last 25 ticks. The existing debuff-cleanser exclusion remains.
KFC's +1 adds one original five-tick cooldown unit, preserving mixed source units.

Shish Wall selects its mode at cast time: sneak dismantles; otherwise airborne
makes a horizontal wall; otherwise sprinting makes stairs; otherwise a vertical
wall. Its ray starts at eyes+0.5 and stops at the first non-air block. Entities
and shields are not terrain terminals. The shared Exasperato search runs native
DDA in bounded work slices, stops at unloaded chunks/suppression/world geometry,
and never loads a chunk for the ray. User explicitly chose no fixed range.
No global living-target scan is needed for this terrain-only consumer.

Admission requires 500 mana (or the existing full-Overload exception). A miss is
free; a deferred actual block terminal rechecks admission and spends the configured
`shish_wall_wand_mana_use` once (default 500). It reuses LbcWall's 7x6 vertical,
7x7 horizontal, rotated stair template and 5x5x5 brick dismantling helpers.
Walls replace only the source #airs; existing solid blocks/containers remain.
Dismantling returns full brick blocks and refunds at most 500 mana when at least
15 were removed, following the user's earlier Wall refund rule.

Both weapons use normal right-click two-hand dispatch, the offhand setting,
role/spectator/frozen/stun and antimagic admission. Dimension Time Stop pauses
active timers and pending search. Purification/death/forget/stop invalidates
native state; accepted deferred hits cannot survive an old match epoch.
Native item names, lore order/colors, flags, models, tooltip tiers and carrot
stack/durability defaults are retained. No resource-pack file is changed.
Railgun, Shishgun, Forest and other broad unready reward pools remain gated.

## Verification

Offline build `6e98549e` passed two actual isolated mode1 matches: 14 recorded
suite results in 49.08 seconds. All 245 Java sources, 670 compiled classes and
2713 resources exactly match their source/release artifacts. Native fixture
suites: reactive-family, wall-menu (lobby), wall-wand, kfc-counters, exasperato,
reactive-family-stop-probe and reactive-family-cleanup, repeated in both matches.
The new suite checks both hands/config, exact source presentation, cooldown/HUD,
current-position platform timing, high-altitude platform, real cleanser/purifier,
frozen/role/suppression guards, dimension pause, four exact wall terminals,
300-block hit, unloaded-boundary free miss without new tickets, native result
consumption/full-inventory handling and cleanup through actual sw stop.
Tests use real vanilla ServerPlayer, native item-use/result-taking and shared
terrain helpers rather than mocked behavior.

Evidence: `build/verification/reactive-final/qualification.json`,
`installation.json`, final build/native logs and `reactive-family-results.json`.
Installed exact verified JAR to main `mods/skywars2.jar` while its world locks and
listening port were absent. The agent did not start main. Previous `91d85205`
remains at `build/verification/main-rollback/skywars2-before-reactive-91d85205.jar`.
Three pre-existing user source edits (RerollCrafting, AcidAntDropChecks and
HelicopterEncounterChecks) retain their prior hashes and are excluded from commit.
Retained early attempts are under `build/verification/reactive-final/attempt*/`.
The first geometry assertion incorrectly assumed a vanilla cow had 20 HP;
its expected health now comes from the spawned cow. A later runner attempt put
lobby-only `wall-menu` in a live match; it was moved before each match start.
Neither issue required changing production wall geometry.

Native synchronous fixture stepping does not qualify real-client GPU pixels,
full natural trajectories, actual disk restart/unload/disconnect or peak capacity.
The known fake-player Polymer packet-context warning remains a separate fixture
limitation; no final suite failed.
