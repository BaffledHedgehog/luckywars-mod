# Meditation item-display telegraphs ? 27 October 2026

## Requested changes (user)

Sauvojen: restore one legacy web discharge sound at the player's location;
replace phase1/2 red particle warnings with thinner translucent fading red beams;
replace falling-acid warnings with green beams and the damaging annulus with
an interpolated green item-display ring, one step ahead of damage motion.
Gaycaster: the same red laser warnings, moving red two-dimensional wall previews,
vertical green acid warnings, a translucent blue future-flood volume, and expanding
orange spheres ending at explosion radius. Verify electric-wall counts against
legacy. Keep the upcoming stone-statue boss able to reuse the warning beam (user).

## Source and implementation

Read-only legacy Sauvojen `meditate/phase_electro_tick_at_dmg.mcfunction` contains
`minecraft:entity.firework_rocket.twinkle`, volume1 pitch1. `webDamage` now sends
this once per actual graph discharge at the owner, replacing per-node repetition.
Complete web collision sampling remains6edges for4nodes and120edges for16nodes;
visual warnings use one display per traced edge instead of sampled red particles.
Damage sparks remain separately bounded. Terra warning lasts40ticks; web20ticks.

`TrialWarningVisual` is a shared visual-only Polymer item-display helper. It uses
existing manual visual tracking and lifecycle ownership, never damage entities,
colliders, team selectors, terrain writes or a new client mod. The native item
renderer rotates around Y by pi; beam JSON Z=-8..8 therefore renders effective
+Z from0to1. ScaleZ is traced length; X/Y keep a1/16block cross-section. Raw custom
model tint encodes alpha without darkening the existing bloom RGB palette.

Sauvojen's green ring is the actual10..15annulus (model inner1/3outer.5 at scale30),
with native position and transform interpolation1. Each damage turn advances the
visual to the next center, compensating the client's one-tick position interpolation.
Acid sources retain their original movement, delayed shots and suppression; green
warnings follow their own source and close on expiration/removal.

[Gaycaster source and exact timings](GAYCASTER_WARNING_2026_10_27.md) confirm the
wall count was already correct:11independent50percent opportunities, five lanes
per wall. Warning changes do not alter that schedule, original lane gaps,
27block lasers, radius5explosions,15block acid descent, damage or WILD ownership.
The ocean cube encloses the whole future12radius/floorY sweep; it is a bounding
cube, while original circular damage and21x21terrain paint remain unchanged.

Warnings advance exactly once per active trial tick; Time Stop pauses lifetime,
fade, expansion and motion. Hazard retirement, suppression, failed/finished trial,
death, disconnect, held-stack loss and match/epoch cleanup close owned visuals.
The same finite beam factory can serve the future stone-statue attack; that boss
is not introduced by this patch.

## Resource pack

[Manual merge instructions](../resourcepack_template/MEDITATION_WARNINGS_README.md)
contain20new files and the small item vertex/fragment and bloom shader edits.
The author's current resource pack is only read, never modified. Its existing
black-hole, nuclear, signed-effect, zoom, zone and alpha254/130 routes remain.
GPU validation covers8item shader variants,18alpha samples,24bloom samples and
65,341sphere radial coverage rays; evidence is local in
`build/verification/meditation-warning-resources/results.json`.

Bloom reuses scene-color palette extraction, so translucent colors mixed with
arbitrary backgrounds cannot be isolated perfectly; exact material-only bloom
would need a separate mask/render target. The orange sphere is a1152patch
spherical shell with less than1percent radius approximation and slight seam
overlaps. These are shader/geometry tests, not in-game client image evidence.
The user must apply the new models/textures/include and three shader merges.

## Native qualification

Final artifact evidence is written after qualification under
`build/verification/meditation-warnings/qualification.json` and
`build/verification/meditation-warnings-gameplay-results.json`.
The native suite covers model metadata, actual item-display spawn/remove packets,
beam matrix endpoints, fade clocks, Time Stop, cleanup, complete web collision,
one owner-centered discharge sound, and all naturally scheduled Gaycaster phases.

Final integration build `8d2f7513` passed 28 results in two actual
mode1 matches (125.13 seconds). Native geometry/wire/lifetime, complete
Sauvojen web damage and all natural Gaycaster warnings passed in both matches.
Exact518sources/1467classes/3221resources;175author paths and31live overrides
unchanged. Child shutdown and exact snapshot restoration passed. GPU and20asset
hash evidence are included in the local qualification. Automatic main install
waits for normal shutdown; resource-pack edits remain manual.
