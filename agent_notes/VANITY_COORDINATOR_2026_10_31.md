# Vanity coordinator and sustained abilities ? 31 October 2026

## Current scope

The implementation now routes qualified Vanity acquisition through the actual end-of-tick
save coordinator, consumes one source item, and requests a full rewind on holder death.
This is still an unreleased branch; the broader Chaos reward-chain release gate remains
separate. The installed main-server JAR is unchanged. Resident item/status state now joins the same private graph. The production coordinator
is implemented and its final native lifecycle checks pass within the user-approved scope.

## User decisions

- Preserve existing exact checkpoint owners already implemented.
- Remaining unsupported short spells/callbacks may be cancelled at rewind.
- Accepted block construction must finish before capture; never keep a half structure.
- Sustained abilities, summons and reversible transformations must remain functional.

The coordinator therefore drains accepted construction using the existing bounded
shared dispatcher. Duplicating pauses new producers while already accepted writes drain;
idle producer shells do not prevent capture. Independent aura/event/academy queues must
also settle. Unsaved finite rooms/trials finish before acquisition grants the authority.
Capture itself does not freeze ordinary simulation or discard accepted construction.

## Shared restore order

Freeze input and simulation, retain resident destination chunks, detach future work,
restore terrain, core, native actor/player bodies and relationships, then private ability
owners using one canonical source graph. Publish views after native client handshakes,
restore random streams last, and release the previous native freeze state.

The sustained owners include Void Arcanum/Sheep holes, Duplicating centers, Soulless and
Metasola fields, Antigravity platforms/passengers, Polymorph forms, Frost/Stun/Electro
statuses, Academy ownership, Chef state/supplies, Hedgehog encounters, apocalypse/nuclear/
Sauvojen event actors, Shish/Helicopter encounters and rifle scope state. Resident item state includes turn-curse duration/rotation history,
rocket charges/recharge deadlines, nanotrap ownership and saved attribute baselines,
active MK2 flight, finite KFC/Babylon statuses and Stand grant/cooldown/burst state.
No replay grant/refund/sound/native attribute or effect writes occur in these installs.
Native AI path
search caches rebuild. Existing earlier spell, authority and queued-work owners remain.

A player absent at rewind receives no simulated replacement. Its image waits for the
real login. Retained removed logout bodies are private reference data only and are never
registered, revived or ticked. Current private references rebind to the real login body;
unrelated current terrain or other players are not rewound again. Pending forms/fields/
auras/scopes merge into replacement points so a later rewind retains them too.

Real Carpet players have no physical client. Only their restored native listener is
acknowledged server-side; normal clients still provide their own teleport/load ACKs.
Other packet actions and Carpet action ticks remain blocked while restore is active.

## Native evidence so far

`build/verification/vanity-coordinator/coordinator-b7bdf053.jar`
SHA-256: `b7bdf053376f40ca772534bc678de5ff922b4c23f9e7b3ae889cee021ec77322`.
The `vanity-rollback` scene passes actual qualified source acquisition with one debit,
a structure completed across multiple native ticks, repeated hole/Polymorph/Chef/
terrain/player restores, removal of future actors, actual logout/login, native holder
death and respawn, real packet ACKs, and an actual Carpet player without client ACKs.
The isolated child exits normally; focused logs/results and frozen input/package hashes
are retained under the same evidence directory. A broader regression is running.

Earlier native performance probe d43741fa measured 196 loaded chunks, 49 actors
(0 living) and 2 players: cold capture 68.369 ms, warmed median 16.663 ms/max 18.552 ms.
The subsequent actual coordinator scene be14528c with 233 chunks, 62 actors and 3 players
captured in 45.952 ms. Four rewinds took 242.026/94.699/47.333/125.665 ms including
handshake waits. These are bounded fixture measurements, not a peak-combat TPS claim.
There is no permanent capture cost on the main server while its old JAR remains installed.

## Workspace boundaries

Preserve the 199 pre-existing author paths, 31 live grimoire overrides and installed
main JAR d9087f0a. No resource-pack changes, question-file edits, main restart or push.
This note supersedes earlier implementation-roadmap statements that every remaining
short continuation needs a dedicated exact adapter; their historical evidence remains.

## Broader regression and follow-up

The b7bdf053 broad run completed 43 actual mode1 matches and exited normally,
restoring all fixture snapshots exactly. 89 of its 91 planned gameplay suites passed;
Witch acquisition still had the obsolete assertion that Vanity must be disabled,
and Helicopter encountered a thirty-TNT-count mismatch. The obsolete check was
updated for real unqualified source failure. Both suites pass in a separate fresh
4015022a run together with the full production coordinator scene. That scene also
verifies restored private curse/rocket state after actual reconnect. The Helicopter
broad-run failure remains retained; isolated success is not a peak-load qualification.

Replacement snapshots now capture inside the private retained-body scope. Exact
retained player identities bind to canonical UUIDs in both body and accepted damage
source links; unrelated historical bodies retain their original historical policy.
Retained offline inventories participate in canonical stack-location indexing.


## Final lifecycle qualification

Artifact `coordinator-9980b75a.jar`, SHA-256
`9980b75a3beec58a62db7a92f02722c8442e212571b712faae68281230198742`,
passes all four final suites in four fresh native matches: `vanity-rollback`,
`vanity-core`, `stand-arrow`, `vanity-resident-spells`. The child exits normally,
fixture snapshots restore exactly and no locks remain. This follows the broader
91-suite run and focused long-owner regressions above; prior failed proofs remain.

The production scene now also replaces the point twice while Gamma is offline,
restores Gamma on actual login, then kills it through native death handling and
rewinds again. Its real connection uses native respawn and client-load lifecycle,
including clearing an outstanding respawn wait even if health was already restored.
The complete captured roster includes pending offline player images: they do not
become newcomers/spectators on the next rewind. Disconnected flags reconcile with
current actual presence. No simulated offline player is created.

This scene captures 219 chunks/49 actors/4 players in 54.3584 ms; the two replacement
points take 30.2032 and 13.9029 ms. Four complete rewinds take
290.3384/139.3511/50.8293/140.8333 ms including client publication waits.
These measurements do not establish peak-combat TPS or physical-client rendering.

Short deferred work means one-shot transient attacks/callbacks, such as an unsupported
in-flight spell. It does not mean deleting sustained black holes, summons, forms or
construction midway. Accepted construction settles before the grant/save boundary;
new future queues are discarded when the world is rewound. Existing exact adapters
remain in use. Checkpoint creation is on demand, not a recurring full-world copy.

The authority implementation is complete within these agreed boundaries. The original
full Chaos reward-chain release gate remains closed: this does not install/restart the
main server or claim qualification of the remaining reward chain.
