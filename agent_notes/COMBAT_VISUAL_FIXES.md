# Combat and camera corrections, 25 October 2026

This records the bounded implementation for user issues7–9. Runtime qualification
belongs to the parent verification record; these source edits alone are not a
claim of a tested or installed build.

## User requirements

Academy Fartus (user): «бананам с самонаводкой не нужна физика, они просто должны
лететь и самонаводиться». The user clarified the missing stun as «Удар жезлом в
ближнем бою». The Academy staff therefore stuns its actual accepted melee victim;
nonmelee damage does not charge the staff or start this melee stun. Duration is
40 active ticks, matching the family’s existing source impact incapacitation.

Sniper Erecto must retain legacy recoil without the unexpected one-second speed
increase. Piss particles must obstruct the affected player’s camera, as in the
source, rather than appear at the player’s feet.

## Source evidence and fixes

Read-only source root for all paths below:
`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/`.

- `function/workingitems/awaken_tuntija_wands/erecto/click_upgraded_2_mode_sniper.mcfunction`
  gives Slowness amplifier100 for one second and applies pitch-15 recoil. With
  the native port’s deliberately uncapped attribute minimum, that effect produces
  a large negative movement attribute rather than source vanilla immobilization.
  `LbcErecto` replaces that unsafe amplifier with a transient movement multiplier
  of zero for20 active ticks. It preserves recoil, shot heading before recoil,
  admission1100, total debit5000, flight and damage. The owned multiplier expires,
  pauses during Time Stop and clears on purify, forget, arena departure and stop;
  unrelated movement modifiers/effects remain owned by their original sources.
- `function/workingitems/firtusnadristailo/banan_impact_homing.mcfunction` calls
  `banan_impact_homing_det_rec` three times. The latter turns toward enemy eyes,
  moves.4 and reverses orientation at a non-air block. These actors lack
  `physical_object`; ordinary bananas alone use Paper’s ballistic gravity/bounce.
  Native `LbcFartus` already separates this guided flight from ordinary physical
  flight. The added regression includes native entity ticks and proves that an
  untargeted homing banana maintains straight1.2block/tick flight without gravity
  or drag. Source homing turns remain. The user’s no-physics requirement now
  supersedes the legacy homing wall reversal: guided bananas retire on their
  first solid impact after resolving contacts before that wall. They do not
  rebound. Ordinary bananas retain their original ballistic wall bounce.
- `function/items/swords.mcfunction:62` only increments `fartus_sword` for evolved
  staff melee. No legacy melee stun was located there. The explicit user request
  adds full40tick action stun through the shared SLR action-block system to real
  accepted Academy melee hits. Source banana impact in
  `function/workingitems/firtusnadristailo/banan_impact_success.mcfunction` gives
  Blindness/Slowness/Darkness/Weakness99 for40ticks and Instant DamageIV. Both
  banana forms now use shared40tick stun instead of unsafe negative-speed
  Slowness99; their other source status payload and native instantaneous damage
  remain. Label payload and ordinary banana gravity/bounce are unchanged.
  `LbcShooters.stun(owner,target,ticks)` adds only a safe zero movement multiplier
  and the existing complete voluntary-input/camera lock. It does not overwrite
  delta movement, so external impulses and native physics remain possible.
  Shared target, player-effect, Time Stop and active-owner guards apply; timers
  pause during Time Stop and existing unload/forget/purify/stop clear the state.
  SLR alone additionally installs its existing80tick amplifier127 Slow/Weakness/
  Fatigue effects. The zero multiplier also makes that source payload safe under
  uncapped attributes. Cleanup removes those effects only while the same owned
  instance still has amplifier127; prior effects are restored with elapsed native
  entity ticks deducted. Generic Fartus stun never removes someone else’s potion
  effects. Owned SLR Slow is removed before releasing the zero multiplier.
- `function/effects/tick.mcfunction:31` invokes
  `function/workingitems/yellow_lightning_asplayer.mcfunction` with
  `anchored eyes positioned ^ ^ ^.3`. The function sends200 custom `lbc:piss`
  stick particles, spread.3/.2/.3 and speed0, forced only to the victim; it sends
  three yellow-glass particles, spread.2/.2/.2, speed0, forced to others at least
  .1block from that execution position. The port previously used entity feet and
  ordinary delivery for the bystander particles. Both audiences now share
  `getEyePosition()+getLookAngle()*.3`, including pitch. Private model particles
  remain victim-only; bystanders receive only the yellow-glass payload.

## Verification scope

`ErectoChecks` checks source recoil/shot heading and costs, exactly zero movement,
the19th/20th active timer boundaries, and restoration of the previous attribute.
The native attribute packet is encoded, decoded and reconstructed as an
attribute instance to assert zero during windup and the previous speed afterward.
`FartusChecks` checks ballistic ordinary flight separately from native-ticked
straight homing flight, real accepted melee charging and full stun, absence of
melee consequences on magic damage, and the safe full source impact payload.
The homing regression includes a real resident stone impact and asserts both
actor/display retirement, impact position and unchanged forward velocity rather
than a rebound. The melee regression retains native one-point damage and owner
credit, adds actual native knockback while stunned, and proves input preparation
preserves that velocity. Clearing the generic stun restores the previous movement
attribute and preserves the same pre-existing Slow/Weakness/Fatigue instances;
it leaves no amplifier127 SLR potion residue.

New `CombatVisualChecks`, invoked inside the fully saved Fartus fixture, captures
native outbound packets at both player connections. Across three yaw/pitch
directions it checks exact camera-relative coordinates, forced delivery flags,
private200 versus bystander3 item batches, source models/spread/zero speed and
the actual native particle packet stream-codec round trip. These are server and
wire assertions; real client pixels and visual obstruction remain unverified.
No author resource pack, source server, shared acquisition pools or protected
user check sources are modified by this bounded work.

## Combined integration qualification

Root built `17daf0d4` and ran114 recorded suite results in two actual mode1
matches, including these native checks and actual stop cleanup. See
[combined qualification](GAMEPLAY_FIXES_2026_10_25.md) for artifact identity,
ready-only deployment state and verification limits.
