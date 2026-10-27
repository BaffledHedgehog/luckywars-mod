# Shared mechanics audit, 27 October 2026

The author requested a wider search after Sauvojen meditation kept an independent
fear counter although the native Psycho service already owned fear and its HUD.
This audit follows producers to consumers; similar names alone are not evidence
of a duplicate mechanic. Source presence is not a claim of native qualification.

## Confirmed corrections

- `SauvojenMeditation.blind` now calls `LbcPsycho.afflict` and reads
  `effectValue("scared")`. Its old raw score had neither the native five-tick
  decay nor the native HUD state. Fire-stage cleanup uses `clearStatus`, retaining
  unrelated Psycho effects. Damage remains above105, after accumulation.
- The Academy Air queue invoked the falling nuclear rocket's consumer on the
  drone itself. That cannot create the real bomb. Both ordinary Exasperato and
  detached Academy mode12 now call the same nuclear targeting producer,
  `LbcExasperato.castNukeAt`; the real projectile retains its existing consumer,
  impact, mushroom and32-offset explosion queue. The Academy-only imitation is
  removed. This corrects a legacy dispatch defect to the author's requested cast.
- `void_resistance` was already functional in `LbcMana.voidProtectionSecond`,
  produced by Fevgalea Star and Soulless shields, but absent from the shared HUD.
  E126 now displays its existing seconds in GREEN. No parallel timer is added.

## Other producer/consumer connections inspected

[Detailed status ledger](SHARED_STATUS_AUDIT_2026_10_27.md) records scared,
lookback, twitching, piss, screamray, shock, cold, frozen, target, delayed_death,
reverse_mana, gatling_fovos, acid_rifle and ray. No further confirmed bypass was
found in these families. KFC explicitly grants native effects as well as its
historic compatibility scores; Popit and Traveller clear the native service
before resetting compatibility scores. Those are intentional bridges.

Ordinary, awakened and Ant acid already share `LbcClouds` consumers. Exoperso
already shares Exasperato's nuclear burst. Blaze, turrets, library fire rain and
Sauvojen fire already use `LbcWands.launchFire`. Sauvojen's other detached payloads
already dispatch to their respective native spell services.

Some similar implementations must retain different policies:

- Hedgehog/Babylon native explosions use MOB interaction, captured WILD causes
  and different self-exposure rules. Exasperato's existing API uses TNT rules
  and its protection policy. Replacing these calls wholesale changes gameplay.
- Raw falling-netherite/Quiver lightning retains WILD attribution and epoch
  behavior. Electro's lightning additionally marks electric damage/SHOCK_BOLT.
- Syringe's BonusFireball has origin-mob immunity and summoner tracking, while
  the magical fireball API applies different collision and shielding rules.

These are consolidation opportunities only if a common API can preserve every
policy; this patch does not rewrite them based on resemblance.

## Additional meditation defects

The terra ring depended on an unmaintained legacy fake-player ticker. Its source
angles now use a native clock, pausing with map work/dimensional Time Stop.

The lightning WEB retains all source edges and half-block collision samples.
It queries living candidates once per graph rather than at every sample, and
emits uniformly sampled visuals with a bounded packet budget. It does not spread
the damage graph over additional ticks. Coincident nodes retain their initial
warning/contact sample without Infinity-times-zero arithmetic.

## Limits

This is an audit of the listed implemented status and projectile families,
not proof that every unfinished legacy item is ported. No physical-client pixels
or peakTPS measurement is inferred from code inspection. Native verification and
installation evidence are recorded separately with the qualified artifact.
