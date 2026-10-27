# Void Arcanum: eye pursuit and fluid absorption — 27 October 2026

Status: **qualified0a6a5f3f; automatic main installation queued**.
Live receipt: `build/verification/arcanum-aim-fluids/main-install.json`.
This supersedes the relevant pursuit/fluid details in [the original contract](VOID_ARCANUM_PORT.md).

## Requirements (user)

> черная дыра из арканума - надо чтобы она преследовала цель до его eyes а не feet иначе она ползет по уровню земли и слишком сильно ломает её. Было бы прикольно еще сделать чтоб более большые дыры именно из арканума око вечной тьмы за каждый уровень чуть выше преследовали чем надо по высоте (типо, точку которая на сколько то блоков выше моба)
> ещё - почему то черные дыры не поглощают воду! а может и ещё что то

## Changes and native cause

`LbcVoidArcanum` alone now steers toward the victim's actual `getEyePosition()`
plus0.5blocks for each tier after the first. Extra height for tiers1–5 is therefore
0/0.5/1/1.5/2blocks. The nearest-target distance and contact selector use that same
point, so a body initially at the victim's feet can start ascending rather than
being excluded by feet-distance. Existing acceleration, inertia, one-block
slowdown/contact boundary, disc lean, loaded-world/suppression checks and total
3D legacy speed caps remain. Damage and physical pull retain their existing
geometry, ownership and WILD/protection rules. Sheep and Duplicating pursuit
destinations are unchanged.

The water bug belonged to Arcanum tiers1–2: native `Level.destroyBlock` replaces
the destroyed block with its fluid's legacy block state. Pure water/lava therefore
survived, and waterlogged blocks left water behind. Small-tier destruction still
runs natively for original block/container drops and ordinary feedback; if the
original state held fluid, its residue is then replaced with AIR using the
existing absorption flags. This covers sources, flowing fluids and waterlogging.

The shared erosion scanner does not reject fluids. Native `LevelChunkSection`
counts non-air liquid states, and WATER/LAVA use `.liquid()`, not `.air()`.
The existing non-air scan therefore already visits these states. Arcanum tiers3–5,
Sheep and Duplicating holes already replace selected blocks directly with AIR.
Their shared noisy erosion, radial masks, budgets and absence-of-chunk-load
behavior were left intact. No hardness/solid/replaceability filter was added.
New incoming water can still flow normally from outside an absorbed region.

Native26.3 evidence was read from the local merged sources archive:
`net/minecraft/world/level/Level.java`, `LevelChunkSection.java` and `Blocks.java`.
Relevant project paths are `LbcVoidArcanum.java`, `BlackHoleErosion.java`,
`LbcSheep.java` and `LbcDuplicating.java` in `src/main/java/me/skywars2/game/`.
No resource pack changes are required.

## Verification and release

The extended `VoidArcanumChecks` exercises all five elevated destinations with
real native living targets: upward movement from feet, bounded 3D velocity,
settling near the elevated point and finite upright tilt during vertical chase.
The existing exact speed-cap fixture now initializes momentum along the actual
elevated direction; its tight cap and acceleration checks remain. Ten actual
terrain cases cover tiers1/2 with source/flowing water/lava and a waterlogged slab,
requiring exact AIR and an empty fluid state. Native scheduler/terrain snapshots
are restored. No new invulnerability resets were added.

`gradlew build --offline` passed. The exact unchanged artifact passed **44recorded
results in two actual mode1matches,78.39seconds**, including shared erosion and
dense max-tier terrain, Sheep, Duplicating academy, actual stop/cleanup, player
rules, Time Stop, TPS compensation, signed effects, effect layers, firearms and
FOV transitions. Explicit physics/cursor calls are boundary checks, not elapsed
world-tick or physical-client evidence. No peakTPS or client-pixel claim is made.

All517Java sources,1463classes and3221processed resources match the tested JAR;
fabric metadata and its expected nested SGUI archive were checked. All175protected
author paths and31live grimoire overrides remain byte-exact. The isolated child
stopped normally and saved bytes were restored exactly.

Artifact: `build/verification/skywars2-arcanum-aim-fluids-0a6a5f3f.jar`.
SHA256: `0a6a5f3f3b2e6cbe86876373ec84fe66cfd18cb67710e0c2ac6c451cc9cdef0e`.
Evidence: `build/verification/arcanum-aim-fluids/qualification.json` and the
`arcanum-aim-fluids-gameplay-results.json`/`.log` in its parent directory.

Main remains running6890ae59. Automatic installer PID16804 adopted this cumulative
update, superseding queued698e428c; the Chef and Metal Pipe changes remain included.
It waits for normal main-world lock/port release, without forced stop/restart or
replacement of a running JAR. Retain the new ready artifact, installed6890ae59 and
c260a27drollback while queued, plus the independent Polymer audit tool. Older
status/ready lists remain historical and are superseded by this entry.

After installer adoption, the obsolete698e428cready JAR was removed by exact
path/hash (40617487bytes); its cleanup receipt remains beside qualification.
