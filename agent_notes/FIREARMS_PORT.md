# AK47 / Magnum — target contract and prepared status, 21 October 2026

Sources: [bounded weapon audit](AK47_MAGNUM_NEXT_SOURCE.md) and
[resource verification](FIREARMS_RESOURCES.md). Legacy player AK47 and Magnum
are in the expanded shooter source, not the ordinary shooter pool. Source pools
remain unchanged. The Stalin boss and its firing logic are outside this port.

## Current prepared status

AK47 infinite/finite and all six Magnum modes are implemented and qualified.
Four tables are promoted under `src/main/resources/data/lbc/loot_table/ported/`:
`ak47.json`, `ak47_finite.json`, `magnum.json`, and `magnum_ammo.json`.

Native gun IDs are `skywars2:ak47` (both variants, distinguished by original
`finite:1`) and `skywars2:magnum`. Ammo uses six Polymer IDs `skywars2:magnum_round_{standard,incendiary,electric,acid,void,explosive}`
while preserving source
custom_data markers, weights, counts, components and four rolls. The importer also writes the original `lbc:magnum_ammo` alias for the already
ported 64-arrow UltraReforge. No new two-arrow NPC trade, source pool weights or
Stalin boss are introduced. Legacy vanilla rounds still match by custom_data.

Magnum reuses existing dependency APIs:
`LbcWands.magnumElectro` emits four existing lightning rays with lifetimes
5/7/9/11 and mechanism clipping. `LbcClouds.magnumAcid` exposes the exact six-charge
spread and one big-charge source profiles through existing acid behavior. The weapon engine is `LbcMagnum`.

The current 26.3 resource pack was verified read-only; original keys, name/lore
formatting, models/textures, tooltip sprites and custom AK sound are reused.
The resource pack and template are unchanged. Graphical-client visual matching
has not been tested.

## Author rules (user) ? answered, superseding the source bugs

- Magnum fires five shots for one consumed round.
- Cancel its remaining queued shots immediately when the firing gun is unheld.
- With simultaneous two-hand AK use, the finite gun spends hitbullet for its own
  shot; the infinite gun fires free. Gate each shot by the gun actually used.

All three required questions are answered; no author decision gate remains.
These rules supersede the legacy cross-hand finite charge and retained queue
behavior. Preserve the original source findings as history, not current policy.

## Shared target requirements

Apply existing team, spectator and service-NPC protection and native hitbox
collision. Preserve direct and indirect damage authorship through existing
helpers, including acid, ignition, lightning and explosions. These are globally
required corrections to the legacy source, independent of the three decisions.
Reuse existing cooldown, inventory, suppression, projectile, acid, lightning,
explosion and lifecycle owners; do not introduce parallel engines for them.

The bounded source audit records exact AK ray, block-drop/recoil behavior and
Magnum mode effects and dependency boundaries. Preserve source presentation;
implement the confirmed burst and finite-ammo contract above. Source ammo acquisition remains a separate dependency and must not be
replaced with invented free ammunition.

## Implemented behavior and validation boundary

All modes are implemented: standard pierces for 16 arrow damage; incendiary
pierces for 12 generic damage and ignition; electric creates four existing rays;
acid deals 6 generic and emits six existing acid charges or one big charge;
void pierces for 4 outside-border damage and erases terrain; explosive uses the
source radius-4 explosion on entity or wall impact. Big acid is aimed **down**;
the earlier bounded audit's ?up? reading was incorrect. Existing target/owner,
block journaling and secondary-effect helpers remain shared.

Magnum is TECH: mechanism suppression blocks it, magic suppression does not.
No mana is spent. SLR and Magnum share one per-player cooldown, with the source
ready sound at zero; multiplayer does not accelerate the timer. Both-hand input
and mode/ammo selection use the actual firing hand. Five-shot queue ownership,
unheld cancellation and lifecycle cleanup follow the author rules above.

Resource importer, catalog and final build checks passed. Two final native
firearm matches passed on SHA-256
`062c19b3dec8e378e09589633075e414182b820f850cd5ecae961790fd49ef40`:
seven suites per match, including both cleanups, in 50.75 seconds total.
Resource audit passed: 2,635 packaged entries, 1,977 excluded entries, 114 Java
sources and valid archive CRC. Four tables are ported; the original
`lbc:magnum_ammo` alias is byte-identical to ported ammo for UltraReforge.

Archive `build/verification/skywars2-firearms-062c19b3.jar` is ready without
installation; main JAR remains ba2e46e6. Source RP/template are unchanged.
Two additional shared-regression matches passed on the same final JAR; root owns SERVER_TESTS.
Next audit: [Govnomet / Helicopter Boss](FIREARMS_NEXT_SOURCE.md).
