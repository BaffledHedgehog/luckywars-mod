# Gameplay review and signed attributes, 25 October2026

This batch addresses the user's eleven-point follow-up and the subsequent
forty attribute decisions. It supersedes the previous interpretation that
Academy Fartus homing bananas must stop at walls: they now bounce without
gravity, as explicitly clarified by the user.

## Delivered behavior

- Gaycaster's ordinary acid remains the original six ordinary300-tick charges;
  Academy uses the original500-tick great-acid projectile and its children.
  This audit required stronger checks, not a variant substitution.
- Thunder's native-lightning chain remains WILD. The previously missing generic
  Raid124 chain is now a separate WILD status with source16 ARROW damage,
  electric arcs, owned contributions, HUD and lifecycle. Both reach allied
  players and the caster. Captured WILD sources pass native player friendship
  admission; a managed mob carrier supplies geometry, never its own attack
  ownership/policy. Native lightning and ordinary projectile rules remain shared.
- Thunder Reforge does have source particles: cyan dust scale0.3 every0.35block.
  The native END_ROD substitution is corrected to the authored dust.
- Academy Neutral adds free selector mode6 with independent per-stack cycling
  through the original five modes; failed casts do not advance its cursor.
- Academy Tesla Instant shares ordinary arc conductor detection and WILD
  conductivity. Tesla direct/chain allied-player protection remains distinct.
- Antigravity preserves the selected15-second Paper storm but aligns block
  displays to their true lower corners, samples actual resident solid terrain
  and lifts only actors contacting the lifted block's collision top. Empty
  terrain creates no entity-only lift. Independent altitude hazards remain.
- Ant bodies retain original invisible baby-zombie physics. The source head
  model needed a26.3 client transform correction; the user's author pack now
  already contains translation[0,-7.4625,1.5], scale[1.5,1.5,1.5]. The applied
  template was removed by the user and is deliberately not recreated.
- Ordinary bananas preserve initial caster body rotation and the source's later
  motion quaternion; homing bananas retain identity-local rotation and source
  wall reversal, with no gravity on projectile or display. Label scale0.5 and
  its normalized source quaternion now load correctly: the native preset lacked
  the required translation field, which silently discarded the transformation.
- Babylon's three tipped-arrow transports now execute their source effects
  on solid block impact once, with native owner, stopper and ward rules.
  Their WILD owned explosions now explicitly protect technical living actors.
- Special eggs replace the complete authored mob name after native component
  loading, preventing the egg item's red/bold formatting from leaking through
  recursive NBT merge. Unnamed source mobs retain vanilla egg-name behavior.
- Attribute limits follow the explicit user policy, including finite native
  lower bounds, selected unrestricted values and custom knockback ranges.
  Unknown/modded attributes retain their own limits. Source mob base values are
  retained while actual values obey the new caps (e.g. skeleton knockback100
  becomes10 and ant reinforcement chance100 becomes1). Armor above40 scales the
  remaining armor40 damage by40/armor; toughness-8 and nonfinite intermediate
  results cannot corrupt HP. Negative melee and fall values heal directly;
  a narrow direct-source fallback also covers native spear/stab, bee and golem
  attack overrides, with projectile and indirect-effect damage excluded;
  negative oxygen accelerates native air loss. Negative effective mining
  clones one adjacent free cell toward the player, including independent full
  container contents, with a magnitude-based cadence and no original removal.
  Native creature regressions now require clean participant attribution and
  remove their fixture-owned pending hits before restoring participant state.

Detailed contracts: [acid audit](GAYCASTER_ACID_REVIEW.md),
[chains/Tesla/Neutral](CHAIN_TESLA_NEUTRAL_FIXES.md),
[antigravity](ANTIGRAVITY_REWORK.md), [ant/names](ANT_APPEARANCE_AND_EGG_NAMES.md),
[bananas/Babylon](BANANAS_BABYLON_FIXES.md),
[user attribute decisions](ATTRIBUTE_DECISIONS_2026_10_25.md),
[bounds/armor](ATTRIBUTE_POLICY_ARMOR.md),
[signed combat/oxygen](NEGATIVE_ATTRIBUTE_EFFECTS.md),
[negative mining](NEGATIVE_MINING.md).

## Verification and deployment

Build `b4cdfa8d` passed148 recorded results in two actual mode1 matches
(164.19s), including actual match-stop cleanup. Exact
340 sources / 925 classes / 2812 resources match the tested JAR. The
isolated server shut down normally and restored its configuration, profiles,
scoreboard and previous JAR. No check or cleanup failures occurred. The old
Carpet fake-player setup still logs its known packet-context warning; the
qualification records those two expected lines rather than concealing them.
Driver: `tools/smoke_gameplay_review.py`. Local evidence and verified ready
artifact: `build/verification/gameplay-review-final/qualification.json` and
`build/verification/skywars2-gameplay-review-b4cdfa8d.jar`.

Main remains the installed2558af6b build under the user's prior request to
leave updates prepared. No running main-server JAR replacement, forced stop,
restart or author resource-pack write is part of this batch. Existing user
changes to three Java files, emptied language templates and removed applied
resource templates are preserved and excluded from the implementation commit.

Native fixtures include explicit synchronous boundaries; they do not qualify
real client pixels, actual disconnect/disk restart or peak-capacity performance.
Unmodified client attribute calculations still have their own native ranges.
