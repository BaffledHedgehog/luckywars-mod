# Gameplay corrections, 25 October 2026

## User requirements

The nine requested corrections apply to already ported mechanics. They do not
open additional legacy loot pools or claim completion of the remaining migration.

1. Belial makes mobs fight through their AI, without synthetic provoking damage.
2. Restore the legacy team palette.
3. Baphomet controls native navigation toward/away from its field, ahead of
   ordinary combat and species AI, instead of imparting velocity.
4. Native void and outside-border damage also affect creative players.
5. Magic plague reverses the actual signed mana regeneration and its HUD sign,
   including negative regeneration becoming positive.
6. Academy Confuse, Corruption and Evoker gain an alternating mode.
7. Remove Erecto railgun's unintended enormous movement speed.
8. Academy Fartus homing bananas fly and home without ballistic physics;
   restore stun on a melee hit with the wand.
9. The piss effect puts its particles at the camera, as in the legacy effect.

Fartus clarification (user):

> Удар жезлом в ближнем бою

## Implementation and source contracts

- [Powder AI](GLYPH_AI_FIXES.md): reversible exclusive native AI lease,
  actual native melee, captured causal attribution and lifecycle cleanup.
- [Team colors](TEAM_COLORS_FIX.md): exact 32-team legacy palette, distinct
  hostile solo memberships and restoration of earlier memberships on leave/stop.
- [Alternating modes](ALTERNATING_MODES.md): per-stack cursors cycle the existing
  modes only after admitted casts; selection is free and each hand is independent.
  The new translated key has a Russian fallback for existing author packs.
- [Combat and particles](COMBAT_VISUAL_FIXES.md): safe owned movement multiplier,
  shared full stun that retains external knockback, first-impact homing bananas,
  and exact camera-relative particle packets.
- `CreativeVoidDamageMixin` bypasses only the creative ability gate for native
  `fell_out_of_world` and `outside_border` sources. It retains the native damage
  pipeline, other creative immunities, protected roles and active Greed.
- `LbcMana` reverses the signed effective rate, while keeping the stored base
  rate available for upkeep. Positive effective regeneration respects the cap
  and does not truncate an existing excess; negative effective regeneration
  drains even above the cap. The actionbar prints one actual sign.

## Qualification

Build `17daf0d4` passed114 recorded suite results across two actual mode1 matches
in157.63seconds. Exact323 Java sources,896 classes and2812 resources match the
tested JAR. Native Mixin application, real passive-mob AI combat/death, hostile
Baphomet navigation, creative void/border damage, signed mana/HUD, alternating
cycles, railgun attributes, Fartus melee/knockback/wall impact and particle packet
encoding were exercised. Actual match stops restored the new team assignments
and cleared the owned spell states. The older SLR fixture now removes only its
own attribution residues before following suites.

The combined driver is `tools/smoke_gameplay_fixes.py`. It uses the isolated
port25576 fixture, two actual mode1 matches and normal shutdown, retaining the
earlier family regressions alongside the nine corrections. The running main
server and its installed JAR are not changed by this driver.

Native fixtures inspect real server entities, attributes and encoded packets;
they do not establish real client pixels or peak-capacity performance. The
author's separate resource pack is read-only. The three pre-existing user
sources are preserved and excluded from the task commit.

Ready JAR: `build/verification/skywars2-gameplay-fixes-17daf0d4.jar`.
SHA256: `17daf0d46081738d2b10e919731d5cc752f49677e7fbf024d2fb22c5bd5813a2`.
Evidence: `build/verification/gameplay-fixes-final/qualification.json`.

Main remains running on `2558af6b`; it was not replaced or restarted. The prior
user request to keep the build prepared is retained. The exact earlier skin PNG
is still present in the generated Polymer pack. The two known Polymer fake-player
packet-context log lines also occur in the baseline fixture; no new error lines
or failed suites occurred in the successful run.
