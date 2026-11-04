# Night checkpoint and clone integration ? 4 November 2026

Owned changes: `MatchResidentItemCheckpoint.java`, `MatchSpellCheckpoint.java`,
`CloneActorStates.java`, the companion rollback guard inside
`LbcDanmaku.cloneImage`, and this note. All other existing workspace changes
were preserved. No build, server operation, commit or resource-pack edit ran.

Resident capture now includes typed Expansion, Hakurei reward queue and Touhou
collectible images. Frozen preparation gates their lifecycle callbacks before
canonical player/actor replay. Every new plan is prepared and checked before
any resident installation; publication releases the three owners only through
the existing coordinator. Collectible counters/modifiers install after canonical
player restore. Expansion terrain remains owned by shared terrain snapshots;
its Dispatcher is restored exclusively by its existing shared queued-work graph.
No second dispatcher image, terrain journal or private rewind engine was added.
Both ordinary rollback and runtime rebind already compose these checkpoint owners.

Spell capture now records Danmaku through the same `MatchSpellCheckpoint.Links`
instance as other shared spell owners. Native bodies and caster references all
resolve before Danmaku's card/bomb/external-hand timer plan installs. This uses
the existing Danmaku typed checkpoint and leaves native entity bodies canonical.

CloneActorStates routes managed Danmaku cards and positive bombs through the
existing gameplay-projectile path and native Danmaku image. Shared x2 remains
projectiles only; shared x4 adds mobs through its existing classification.
Negative bomb companions are protected from independent proximity cloning.
Unsupported managed shells fail capture rather than receiving an inert display
copy. Each copied positive bomb installs one companion; companion ownership is
recorded immediately after publication so outer clone rollback also discards it
if a subsequent scale/state install fails. Card direction/homing/age and bomb
charge/fast/age/empty state come from the existing typed image. Original author,
role policy, source protection and native clone transaction remain shared.

Validation: reviewed actual shared rollback/runtime-rebind preparation,
installation and queue publication order; checked typed method signatures and
all changed call sites; scoped `git diff --check` passed. Existing mixed LF/CRLF
files were edited as byte substitutions without whole-file normalization.
Compilation and scheduled live qualification remain root-owned. In particular,
run nonempty coordinator replay and Danmaku x2/x4 companion rollback cases;
this note does not claim those runtime checks passed.

## Scheduled native Danmaku clone qualification

New standalone `DanmakuCloneChecks.start(game)` / `.tick(game)` implements suite
`danmaku-clones`; root owns dispatch, compilation and actual server execution.
The fixture reuses the existing bounded `ChefChecks.Fixture` and simulated native
client acknowledgements. It requires no existing Vanity point or unrelated
Danmaku/clone holders. Production sources are unchanged by this qualification.

Actual selected skill15/17 is serviced by the ordinary registered proximity
scheduler; the fixture never calls `copyNow`, clone adapters or private component
restore. A native card factory publishes one controlled ordinary card; ordinary
`ServerPlayer.gameMode.useItem` on the canonical bomb consumable creates the
paired fast source, debits one and leaves vanilla projectile fallthrough visible.
The source author is TestBeta and the proximity holder TestAlpha. Registered x2
must produce two card/bomb families while leaving an ordinary native cow single;
x4 must produce four of each including four cows. Assertions check author,
phase, age/direction, distinct body identities, paired colocated companions,
negative protection, recursion markers and exact native managed-body census.

The fixture then pauses four active card and fast-bomb families in shared
TimeStop, requests a real full coordinator point, actually forgets all saved
Danmaku state, publishes an abandoned-future homing card, and requests ordinary
coordinator rewind. All saved card/positive/negative UUIDs, positions, authors,
ages and fast/charge/empty values must reappear exactly; the future body must be
absent. After resuming, all four bombs advance naturally to310, counting the
crossing invocation as empty tick1, and retire at60 empty ticks with no shells.
No artificial service ticking or field mutation advances a source phase.

No safe native entity-publication rejection hook exists in this fixture without
replacing a production adapter or introducing an artificial publisher. Therefore
publication-failure injection is deliberately not claimed tested. The production
companion rollback guard received static review only; this scheduled suite proves
successful pairing, independent-negative exclusion and natural/frozen teardown.
Scoped whitespace check passed; compilation and runtime results are root-owned.
