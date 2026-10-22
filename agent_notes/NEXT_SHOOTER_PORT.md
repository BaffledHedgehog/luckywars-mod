# Govnomet / Helicopter Boss weapon - QUALIFIED; archive ready without installation, 21 October 2026

Bounded source audits: [Govnomet](GOVNOMET_SOURCE.md),
[Helicopter Boss](HELICOPTER_BOSS_SOURCE.md),
[resource verification](NEXT_SHOOTER_RESOURCES.md).
The hostile helicopter encounter is a separate future feature, not this weapon.

## Confirmed author decisions (user)

All four questions are answered. Govnomet stops at any ordinary minecraft:shield,
using the existing shared blocking helper. The same applies to ported source
shield_blockable profiles; Acid now also checks the whole native movement segment
rather than only its endpoint. See [shield coverage](SHIELD_COMPATIBILITY.md).
Mega Govnomet applies 20% of vanilla explosion entity damage, without temporary
Resistance. Native defenses, knockback and terrain destruction remain native.
Wither XI remains 20 ticks normal / 60 ticks mega, with real caster attribution.
Boss Helicopter is TECH: magic suppression is ignored, mechanism suppression
blocks activation and scheduled volleys while the cooldown continues.

## Implemented scope

Native Polymer items skywars2:govnomet and skywars2:boss_helicopter reuse exact
source name/lore/styles/models and existing translations. Importer verifies the
current resource pack read-only. Qualified individual table IDs are lbc:ported/govnomet
and lbc:ported/helicopter_boss. The original helicopter_boss alias is byte-identical
for existing internal acquisition references; broad source pools remain gated.
No source pool weights, encounter drops or ordinary acquisition gates are changed.

Govnomet uses a hidden native projectile with source dust only, no new model.
One launch per used hand, no mana/ammo/cooldown, normal source spread and skill9
mega variant, 2 blocks per tick and 50 active ticks. Native swept hitboxes and
shared first-hit helpers replace recursive marker steps. Owner/ally players,
observers and service NPCs remain protected; friendly mobs can be hit. Mega uses
ten native falling dirt blocks and a radius1 native MOB explosion with the true
owner, without spawning a creeper or conflating TNT protection settings.
Shield, Wither and reduced explosion damage follow the confirmed decisions above.

Boss Helicopter extends LbcShooters' existing native arrow engine and terrain
impact/ownership helpers. Shared timer600; held hands fire independently at
600..400 inclusive: 201 volleys, 402 arrows per hand. Coefficient0.2, speed6,
piercing10, full pitch, source side offsets, no mana. Unheld hands stop firing;
reholding during the active interval resumes. Purify resets the timer, and KFC
increments the actual timer. Existing Attack Helicopter remains a distinct item.

LbcItems owns lifecycle, LbcWands owns two-hand input, SkyWars2 accepts old vanilla
stacks by original custom_data. No Creative/developer item-use exceptions.
ProjectileLifetimes classifies Govnomet as protected from incidental native forces.

## Verification boundary

Resource importer source equality and read-only key/model/texture/sound checks
passed. Build and four final native matches passed on `332415d6`: two item matches
(46.18 seconds) and two shared-regression matches (46.01 seconds). NextShooterChecks includes source presentation,
ordinary shield blocking, native Wither damage ownership, reduced explosion HP
damage, and actual magic/mechanism beacon behavior. Main server JAR remains unchanged; no installation.

## Next migration scope

The next bounded stage is acquisition and ordinary/expanded shooter-pool closure.
The expanded pool references ordinary shooters, stalin (player AK weapon), Magnum,
Terra Razorpine, Govnomet, Helicopter Boss weapon and SLR. Ordinary source has
Phantom Knives, two Mobgun entries, KFC gun, Razorpine, Attack Helicopter,
Laser Minigun and Woodificator. Native families now exist, but pool readiness
requires checking every source variant, dependency, preserved weight, resource
presentation and Reforge path; a list of implementations is not that proof.
Do not publish broad pools based only on these item-specific tests.

The hostile Helicopter Boss encounter is separately audited in
HELICOPTER_BOSS_SOURCE.md and remains unimplemented. Its phases, payload dependency,
paired hitbox lifecycle and death burst need a separate implementation/author review.
No encounter was silently substituted with the boss-named player weapon.

Final SHA-256: `332415d6ffac9c5f7dfb02a07f395849b4aa68d31312b512fb5748ce3e47ffe4`.
Ready archive: `build/verification/skywars2-next-shooters-332415d6.jar`.
Native fast-acid MISS crossing a complete shield sphere is explicitly tested,
as are ordinary-shield normal/mega Govnomet, native Wither kill credit and 20%
actual explosion damage. No graphical client visual match was exercised.
Logs and manifest: `build/verification/next-shooters-final/`.
