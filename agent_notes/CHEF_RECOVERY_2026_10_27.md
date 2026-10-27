# Chef recovery, downhill pursuit and lifetime — 27 October 2026

Status: **qualified698e428c; automatic main-server installation queued**.
Live receipt: `build/verification/chef-recovery/main-install.json`.
This supersedes the relevant details of [the earlier navigation patch](CHEF_NAVIGATION_2026_10_27.md).

## Requirements (user)

> если закопать его он не понимает что ему надо раскапывать себя (например если застроить блоками прямо его хитбокс или оставить его в одном блоке)

> он все еще тупит и не может спуститься вниз когда игрок далеко снизу на другой поверхности (нормальный игрок бы сразу ватердропнулся вниз просто)

> если медленно от него отходить он постоянно подбегает на расстояние удара но не успевает бить - нормальный игрок бы продолжал разгоняться, даже если бы влетел в игрока - лишь бы его отпиздить

> ещё он очень часто застревает на подьеме и пару секунд бежит в блок не понимая что надо сделать

> ещё - он каким то образом умудрятся деспауниться (возможно упал из мира где то вне прогруженных чанков хз)

## Implementation and causes

The action graph requires free standing headroom at its starting node. It therefore
could not start mining when its own body was obstructed. Before survival, combat or
planning, the brain now mines intersecting breakable body cells using the existing
tools, hardness, aim, sounds, drops and causal terrain service. Unbreakable blocks
remain intact. A native prone body can crawl through a supported low opening;
shape and hazard probes validate the swept body rather than assuming standing
dimensions. Escape invalidates the old graph once, not once per mining tick.

The survival controller searches eight local headings for a supported approach of
up to four blocks to a ledge, a clear resident falling shaft and a lower safe bank.
Bounded native rays find landings up to126blocks below without skipping the first
surface or unloaded segments. It equips/aims the real bucket before departure,
walks off using ordinary world-space input, falls under native gravity, places
water within native reach and retrieves its own source after contact. Horizontal
input continues through airborne aim/equipment waits. The graph also considers
real downward mining to supported floors up to16blocks below, instead of only3.
These are bounded alternatives to the existing planner, not arbitrary-map path
completeness or a globally fastest route guarantee.

Nearly vertical action aim had two errors: an artificial0.01horizontal-distance
floor distorted the requested pitch, and requiring matching head/body yaw could
reject a head already looking within2degrees of a point below. Pitch now uses the
actual vector, and BOTH camera and head direction vectors must meet the same
2degree angular tolerance. Real rotation packets still precede accepted actions;
there is no cosmetic-only facing or instantaneous rotation substitution.

At melee range the service previously halted every tick and could repeatedly
prepare a stationary critical jump while a victim walked away. It now retains
closing/sprint input, makes a moving critical jump when appropriate, and attacks
a moving victim without waiting for that victim to leave range. Stationary close
combat still attempts native falling critical hits. Actual reach, fixed victim,
sword equipment/dwell, accepted damage, sound/animation and summoner attribution
remain. A grounded failed jump can re-arm its launch latch promptly; a blocked
horizontal step has a6tick rather than24tick stuck threshold.

The unload callback previously permanently retired even a still-live Chef losing
tracking/accessibility. Such a callback now preserves the actor and service state.
Native nonpersistent simulation tickets follow each live Chef, acquired before
insertion/position changes and before releasing the previous chunk. Radius3 means
ticket level30, retaining adjacent entity-ticking chunks across crossings. Equal
chunk tickets are reference-counted: retiring one Chef cannot unpin another.
Actual removal, owner/target/epoch retirement and stop release their own tickets.
`shouldBeSaved=false` intentionally remains: loading a plain saved Mannequin would
not reconstruct this actor's final owner, victim, brain and survival controller.
Existing void rescue, gravity/prone knock, Time Stop and role gates remain.

## Verification and practical limits

`ChefRecoveryChecks` adds seven restored native fixtures: two embedded body blocks;
a prone one-block cell with an unbreakable ceiling; a narrow one-block uphill
collision; a victim retreating0.07blocks/tick across melee range; seven-block ledge
descent with actual water placement/contact/retrieval; deep floor mining/descent
with an actual bucket clutch; and native shared/crossing/retired ticket ownership.
The last case reads real TicketStorage entries and verifies level30 and cleanup,
in addition to preserving a live actor through the unload callback.

Movement fixtures require actual accepted fixed-victim damage credited to the
summoner, no artificial position jumps, gravity, empty offhand and both camera/head
angular errors within2degrees at accepted actions. Existing nine navigation and
24Chef groups run alongside them. These use explicit native physics steps.
The separate `chef-natural` case observes200actual scheduled ticks of prone
fall/wake, native traversal, bridging and attributed melee. The isolated runner
also exercises actual match-stop cleanup and shared gameplay regressions.

Initial rejected checks exposed the vertical-aim bugs. The deep-descent fixture
was also corrected to keep observing water contact after a midair melee hit rather
than treating that early hit as the end of the fall. Rejected deep-clutch evidence
is retained under `build/verification/chef-recovery/rejected-*`.

No physical client animation, network latency, arbitrary-map completeness or
peakTPS measurement is claimed. The disappearance report had no recording; this
patch proves the concrete tracking/unload and simulation-ticket behavior, rather
than asserting that every possible disappearance cause has been reproduced.
No resource pack changes are required.

## Qualified release

`gradlew build --offline` passed. Final unchanged JAR698e428c passed **42recorded
suite results in two actual mode1matches,118.56seconds**, including both new deep
water descent and real native ticket checks. All517Java sources,1463classes and
3221processed resources match the tested JAR; fabric metadata and its expected
nested SGUI archive were checked. All175protected author paths and31live grimoire
overrides remain byte-exact. The fixture child stopped normally and exact saved
bytes were restored. No source/resources changed during the qualifying run.

Artifact: `build/verification/skywars2-chef-recovery-698e428c.jar`.
SHA256: `698e428ca778debcad2eda69a03fa30b2f36a6afe989ba52d3eda7c2b258b78a`.
Evidence: `build/verification/chef-recovery/qualification.json`,
`chef-recovery-gameplay-results.json` and `chef-recovery-gameplay.log` under the
verification directory.

Main remains running6890ae59. Existing automatic installer PID16804 adopted this
qualified update, superseding its queueda81eb534. It waits for the main world's
normal lock/port release; no forced stop, restart or running-JAR replacement.
The new cumulative build includes the preceding Metal Pipe/Divider patch.
After confirming the installer's adoption, the obsoletea81eb534ready JAR was
removed by exact path/hash (40,601,170bytes). Retain698e428cready, installed6890ae59
and c260a27drollback while queued; keep the independent Polymer audit tool.
This supersedes older ready-JAR/install-status lists, not their historical proof.

The global Markdown link audit reports20existing historical links to previously
retired JARs in unchanged older documents. Its full result is retained in
`build/verification/chef-recovery/markdown-link-audit.json`; the new contract's
local link resolves. Those historical links are outside this AI patch's scope.
