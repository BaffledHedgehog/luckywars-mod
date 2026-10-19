# Terra sword nativecheck results

Current rules (user, 16 October): regular sword melee and a real own `Kind.SWORD`
projectile recharge the sword. Owner-attributed cactus without that projectile
and an actual own Terra-wand projectile do not. TRUE_CHARGE still creates its
free beam; unrelated attributed damage cannot. Five melee hits test the cap.
The focused runner also checks `terra-beam`. Current results and artifact:
[SERVER_TESTS.md](../SERVER_TESTS.md).

## Historical run before the charge-source correction

The isolated `tools/smoke_terra.py --swords` run completed successfully. All three
nativecheck groups passed: `terra-sword-menu`, `terra-sword`, and
`terra-sword-cleanup`. The result is recorded in
`build/verification/terra-swords-results.json`.

- Tested artifact SHA-256: `1ce3fa37a598543bd5a49ecc004a3cf6076e1b42a2f78a4fd8ad5e82bce0a271`
- Runner elapsed time: 47.21 seconds
- Coverage: both loot payloads and Polymer restoration; native item use; melee
  and arrow damage behavior; owner-attributed cactus charge gain and charge cap;
  TRUE_CHARGE hit creating a beam; swept target and wall behavior; pause and
  40/20/50 tick lifetimes; suppression, frozen and observer guards; Creative,
  developer, full Overload overdraft; match-stop projectile, display, and charge
  cleanup.

The fixture uses actual Overload loot-table items with their set components for
the overdraft check, and five owner-attributed native cactus hits to check the
four-charge cap. These are server-side nativecheck results; no client visual test
is recorded here.

The non-arrow behavior fixture checks melee and owner-attributed cactus damage,
plus arrow exclusion. It does not assert exact beam arc boundaries.
