# Caustic staff / Singularity of Absurdity - qualified 24 October 2026

## Ready test entrances

- `lbc:ported/caustic_wand`
- `lbc:ported/academy_caustic_wand`
- `lbc:ported/omega_flag`
- `lbc:ported/academy_omega_flag`

`caustic_wand` identifies the legacy `acidwand` staff, model `lbc:acidwand`,
not the previously ported Sauvojen `stacid` leaf named `acid_wand`.
Only these independent ready leaves are exposed; broad staff/reward/craft pools
remain gated pending closure of their other members. Original name/lore/model/
color/tier are retained, including the original Academy appended line/tier.

## Source and requested behavior

Read-only source root:
`E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data`.

- Acid: `lbcr/function/raycast_preloop11*.mcfunction`, `acid_get_id.mcfunction`,
  `lbc/function/universal_razbros.mcfunction`, shared `raycast_acid*` and
  `damagewithid2*` helpers. Costs from `lbc/function/config.mcfunction`.
- Omega: `lbc/function/workingitems/omegaflag*.mcfunction`.
- Presentation: the two entries of `lbc/loot_table/transformables_staff.json`;
  Academy `other/magic_academy/{can_upgrade,upgrade}/{acidwand,omegaflag}`.
- Author resource pack was read only: existing item models and translation keys
  were checked; no texture/font changes or new translation aliases.

Acid costs1200 and launches6 ordinary or8 Academy charges from the eyes (offhand
origin+.5 forward), offsets0..0.05/0.07 and source random yaw+/-30/pitch+/-20.
The existing native acid service owns speed.5, gravity/drag, radius2.5 poison I
300 ticks, ignited3 on_fire damage and300 age budget (burning advances age4/tick).
Source shared user corrections apply: passage through actors, sticky surfaces,
physical reactivity, shield handling, real projectile owner and indirect poison
credit. This batch adds no new flight/actor/cause manager.
Academy has default/ignited modes. Shift switch is free at negative mana (user).
Ignition belongs only to the newly launched volley; legacy's nearby global marker
selector could ignite another cast's charges. The ordinary trial tests fire on
the caster and updates the actual used stack in either hand.

Omega costs3000 and applies native6 cactus damage throughout the loaded casting
dimension, preserving source six-color particles and the growl. Native hurt
cooldown/armor/effects apply; no chunk loads, radius cap or artificial actors.
Ordinary Omega hits caster and allies, even when scoreboard friendlyFire=false
(user). A narrow DamageSource subclass preserves native cactus death text and
real caster attribution while allowing this WILD hit through native team checks.
Academy Omega protects caster and ALL allies (including friendly mobs), per the
user's explicit correction on24 October; enemy mobs use source generic6 and
players cactus6. Antimagic regions and protected technical/observer roles remain
excluded; Academy also respects source nomagic_active. Ordinary trial compares
integer HP before/after the cast, checks the used hand and does not invent a
requirement that HP loss was attempted/accepted.

Two replacement existing-key translations in the resourcepack template describe
Academy's expanded ally protection; the user merges them into the actual pack.
No permanent gameplay counter is introduced, so no new HUD/KFC timer is needed.

## Implementation

- [Instant casts](../src/main/java/me/skywars2/game/LbcCausticOmega.java).
- [Shared acid](../src/main/java/me/skywars2/game/LbcClouds.java): Magnum delegates
  to the same volley helper with its previous arguments/count/ignition intact.
- [Dispatch](../src/main/java/me/skywars2/game/LbcWands.java) and
  [Academy](../src/main/java/me/skywars2/game/LbcAcademy.java): native and legacy
  flags, upgraded masks first, PKM only, existing lifecycle/mana/cast hooks.
- [Native qualification](../src/main/java/me/skywars2/game/CausticOmegaChecks.java)
  and [two-match runner](../tools/smoke_caustic_omega.py).

## Qualification and installation

Offline buildb62a6093 passed22 recorded suite results in two actual mode1 matches
(62.43s including boot/map load). Exact276sources/740classes/2759resources match
the tested and installed artifacts. Tests cover actual native item use, both
hand routes, source presentation/counts/price, native poison credit/fire damage,
sticky block collision/age300 boundary, scoped ignition, negative-mana free mode
switch, offhand trials and successive-tick real Academy promotions. Omega hits
a distant high target and allies with team friendlyFire=false; Academy protects
owner/friendly player/friendly mob. Actual match stop removes all six charges.
Regressions: AcidAnt, AdvancedSnow, BloodConfuse, Neutral, Academy acquisition,
KFC counters/menu and Frost menu. Source acid and Time Stop regressions reuse
existing AcidAnt checks; no separate new Time Stop implementation.

Evidence: `build/verification/caustic-omega-final/qualification.json`, copied full
log/results, installation manifest and exact artifact audits. Only the previously
known fake-player Polymer packet-context ERROR pair appeared; all suites passed.
Native effect/lifetime boundaries include manual ticks. No real client pixels,
actual disk restart/unload/disconnect or peak-capacity performance claim.

Installed `../mods/skywars2.jar` on stopped main (not started by the agent).
Rollback: `build/verification/main-rollback/skywars2-before-caustic-omega-1196e521.jar`.
Three preexisting user sources remain byte-identical and excluded from commit.
