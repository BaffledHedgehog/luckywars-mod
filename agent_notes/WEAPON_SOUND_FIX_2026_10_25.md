# Weapon sound regression, 25 October 2026

User report: SLR firing sound is missing; audit other weapons for similar omissions.

## Source contracts and fixes

- SLR `lbc/function/items/shooter/slr/shoot.mcfunction:8` plays
  `minecraft:lbcsounds.karich`, MASTER, volume2, pitch2, selector radius120,
  minimum volume0.5. The native call accidentally passed0.5 as selector radius,
  excluding even the shooter because the muzzle lies farther away. Restore
  radius120 and pass minimum volume0.5 separately, retaining existing muzzle.
- Magnum `other/techno/magnum/cycle.mcfunction:3` and `cycle_left.mcfunction:2`
  play `minecraft:ui.button.click`, MASTER, volume0.8, pitch1.5 on mode changes.
  Restore the omitted sound through the shared playback helper for either hand.
- Encounter `drops/helicopter_boss_spawn.mcfunction:4-5` uses radius20 for both
  spawn sounds: `lbcsounds.helicopter` volume0.1/pitch1/minimum0.1 and
  `lbcsounds.kazahstan` volume1/pitch1/minimum0.4. Native level playback had
  dropped both selector and minimum volume. Route these two calls through a
  ServerLevel overload of the existing source-compatible playback helper.
  Other encounter combat sounds retain their existing calls.

The helper preserves same-dimension admission, separate selector/audible
ranges, minimum-volume relocation two blocks toward the source, MASTER and
native packet position quantization. No audio asset or RP change is required.

## Additional audit

Inspected AK47 (including finite variant), Laser Minigun firing/rev/spin,
Magnum firing/shared reload click, Mobgun, Razorpine, Govnomet and handheld
helicopter sound calls against their legacy branches. No other firing sound
omission or fractional-radius misuse was found in those inspected paths.
This is a scoped weapon audit, not a claim to have heard every item in-game.

Author RP was read only. Eight relevant custom sound events (SLR, AK47, four
minigun events, helicopter and kazahstan) and every referenced OGG exist.
The exact event/asset hashes are recorded in the ignored verification receipt.

## Verification

Build593a39df passed28 recorded results in two actual mode1 matches (58.83s).
`swcheck weapon-sounds` captures actual native outbound sound packets for:

- real SLR item use, both hands/all4modes, full-volume shooter playback,
  minimum-volume listener60 blocks away, exclusion beyond120 and silent
  rejected ammo/cooldown inputs and mode changes;
- actual Magnum mode changes in both hands, source MASTER/.8/1.5 click,
  native audible range and rejected offhand/no-ammunition inputs;
- actual encounter token spawns with both sounds at19 blocks, their respective
  minimum volumes, exclusion at21, and quiet owned-pair cleanup before ticking.

Joint regressions cover SLR, Magnum, Minigun behavior/visuals, next shooters and
rifle scope, plus actual match-stop cleanup. Isolated settings, profiles,
scoreboard and installed JAR were restored; server stopped and fixture locks
released. Exact353 Java sources/980 classes/2818 resources match the tested JAR.
Only the known Polymer packet-context diagnostic during fake-bot setup occurred.
Packet delivery and available RP assets are verified; client audio playback is
not directly observed. No author RP, translation or applied-template changes.
Five pre-existing user file hashes are preserved.

Ready: `build/verification/skywars2-weapon-sound-593a39df.jar`.
Evidence: `build/verification/weapon-sound-final/qualification.json`, native log,
result receipt and `rp-sound-audit.json`. READY ONLY: main remains04396ca4;
no installation, stop or restart. Obsolete Chef4bf4649e archive was removed
only after this qualification; preceding Hole Erosion54941d0e and main fallback
archives remain.
