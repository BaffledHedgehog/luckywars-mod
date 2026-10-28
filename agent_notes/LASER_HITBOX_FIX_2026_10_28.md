# Laser collision repair — 28 October 2026

## Author report (user)

> посмори, ты по моему сделал кривую проверку столкновения у рейкастов! Это касается сейчас gaycatser-медитации (лазеры), и скорее всего всех остальных лазеров! они задевают игрока даже когда он далеко от лазера и лазер не пересекает хитбокс

The standing native-hitbox rule in AGENTS.md also applies here: particles are
visual samples, not collision cubes. Preserve causal ownership, native damage,
role/epoch/pause gates and the authored durations.

## Diagnosis and change

Gaycaster meditation phase1 independently checked a positive-X/Y/Z one-block
cube at each0.3-block sample. It omitted the legacy second selector, which had
required the SAME target to overlap the opposing cell too. This both widened
and shifted the harmful volume. The complete27-block beam now clips each native
living hitbox once. Terrain paint, warning position, firework samples, timing,
penetration and arrow7damage remain unchanged.

God Hedgehog used two independent target queries for those cells. One creature
could satisfy the negative cell while another satisfied the positive cell and
was killed beside the beam. Its real2.5-block movement segment now clips each
native candidate once. The1e-7broadphase margin only keeps flat-axis candidate
queries nonempty. Existing target guards, inherited WILD kill credit,20-tick
life, cosmetic samples, residency and passage through blocks remain.

Both use the small LbcBeamGeometry helper, whose narrowphase is native AABB.clip
plus an inside-origin check. It adds no artificial hit radius or per-particle
world queries.

The shared resident ray service already uses ProjectileUtil with zero hitbox
padding. Its inflate(1)box is only broadphase, not the damaging volume. Ordinary
and academic Gaycaster colour casts, tiny rays and minigun continue using that
native path. Their legitimate endpoint areas, electric-column widths and acid
payloads are distinct from direct-ray collision and were not globally reduced.
See [the scoped source audit](BEAM_COLLISION_AUDIT_2026_10_28.md).

## Verification contract

GaycasterChecks now invokes the actual prism hazard against a native ServerPlayer:
axis-reversed, diagonal, vertical and sloped rays; centre hits, both-side misses,
finite endpoints, native hitbox edge hits/misses and vertical separation. Accepted
hits retain arrow7and caster credit. It also submits native queued rays against
on-axis and off-axis targets inside the broadphase, including diagonal beams.
The existing full six-stage meditation, warning and stop/cleanup checks remain.

GodHedgehogChecks drives the actual white-to-ray actor conversion and one real
ray update: two separate off-axis creatures explicitly satisfy the former paired
cells yet survive, while an intersected creature dies with owner credit. Before/
after-segment targets and Time Stop protection remain, with reversed/diagonal
directions. The existing complete encounter checks remain.

tools/smoke_beam_collision.py reuses the isolated runner with two actual mode1
matches, these suites and prism/minigun/rifle/Sauvojen/caustic/Phantom/TimeStop/
physical-projectile regressions plus actual stop probes. Qualification and exact
artifact evidence are local under build/verification/beam-collision. Physical
client images and peakTPS are not inferred from native tests.

Build784b026b passed36recorded results in two actual mode1matches in160.07seconds,
including all new player and God-ray geometry probes. Normal child shutdown and
exact fixture snapshots restored. The exact tested artifact matches540Java
sources,1533compiled classes and3230processed resources; Loom's manifest JSON
normalization and declared SGUI nesting are verified separately. All198author
dirty paths and29live override JSONs remain byte-identical. The first artifact
proof attempt rejected fabric.mod.json's expected Loom transformation; the final
proof compares its parsed descriptor and exact nested-JAR declaration instead.
This was an evidence-script correction, with no gameplay build change or failed
native run.

The ready artifact is build/verification/skywars2-beam-collision-784b026b.jar.
Its automatic installer replaces the previously queued Phantom artifact after
ordinary main-server shutdown/world unlock; it never forces stop/restart. Main
ccac368fwas unchanged at qualification. Installation status is recorded in
build/verification/beam-collision/main-install.json. No resource-pack changes
are required.
