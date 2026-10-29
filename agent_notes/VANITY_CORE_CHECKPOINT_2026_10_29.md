# Core component for the full Vanity checkpoint

## Author contract and release boundary

(user) Vanity must rewind the complete match: world/map, ALL players (including
dead, offline and newly joined cases), items, statuses, effects, attributes, motion,
native/private counters, tags/NBT and core/map parameters. A player-NBT-only replay
is unacceptable. Captured offline players retain restored state until login;
newcomers become observers. Authority control follows its real current owner.

This stage adds the explicit **core phase**, alongside the already verified
terrain component. It is not the complete Vanity ability. Acquisition stays gated
by `Mode.VANITY.implemented()==false`; the full original25 Chaos release gate is
CLOSED. No main installation/restart/push, reduced pool, RP edits or question-file
edits. Questions belong in CHAT.

## Core state and value ownership

`GameController.Checkpoint` captures participants in their existing order, original
spawns, lives, awaiting-respawn/disconnected flags, pending lobby entries and native
unload references, stage/host/winner, match/lifetime/countdown/finish/grace clocks,
fall/lobby/map-selection flags and parameters, and bossbar values/membership. Restore
creates independent participant records on each replay. Original game/epoch, stable
map, server thread and frozen simulation are checked before component mutation.

`MatchCoreCheckpoint` coordinates the existing owners instead of starting another
match or rerolling kits: controller values, settings, player profiles, native
scoreboard, `LbcTeamColors` assignments/departures, MatchEvents' three timeline fields,
committed Lucky selector, canonical authority UUIDs, spectator membership/gamemodes
and ZoneBarrier's analytical clock/radii/visual. Inactive zone counters are preserved
as well as active state. Authority tags themselves belong to the later body phase.

`MatchValueSnapshot` is deliberately restricted to the two plain data schemas
`GameConfig` and `PlayerStore.Profile`. It covers every declared instance field;
supported types are int/boolean/String, int arrays and string sets. An unsupported
future type fails capture explicitly. It does not traverse arbitrary server objects.
Both values and original profile/array/set identities are restored, including when
normalization or later edits replaced the live arrays/sets. New profile entries are
removed and original map order is reinstated. No save/clamp/normalization or JSON
generator runs as a side effect; durable persistence belongs to final full-restore
commit after all components are coherent.

## Native scoreboard and client publication

`MatchScoreboardCheckpoint` stores a deep native-codec NBT image of all objectives,
scores (including fake holders), locks/displays/number formats, display slots and
team metadata/members. It separately retains original Objective, PlayerTeam,
PlayerScores and Score handles. Native public removals retire future data and
client tracking. Narrow mixin access reinstates original handles and criterion
indexes before native display/tracking/team publishers advertise the restored data.

This preserves already cached `ScoreAccess` writers and existing team references,
even when the original objective/team was deleted and its name reused after capture.
It also removes future fake-holder scores and restores signed int values. Native
team ADD precedes setter-generated MODIFY packets; clients do not receive updates
for unknown removed teams. No second scoreboard, private score ledger or custom
score/team packet implementation is introduced.

## Lifecycle ordering and limitations

The full owner must quarantine inputs/writers, restore terrain, apply this core
phase, then restore native player bodies/actors/private queues and camera state
before resuming. Spectator membership reset intentionally precedes native body,
inventory/effect and camera restoration. Native simulation freeze alone still lets
players tick and is not a complete input barrier.

Captured membership follows UUIDs, including replacement ServerPlayers. Online
newcomers lose later enrollment and become real observers. An offline captured
participant retains its checkpoint record/lives while the absent body is not
counted as connected. The test replays the core image explicitly after native
reconnect; an automatic pending full-body/login coordinator is NOT implemented.

This phase does not restore native player NBT/inventories/effects/attributes/motion,
native statistics/advancements, native actor identity/lifecycle or private ability
queues. MatchEvents skills/Vex queues and victory actors are not its three-field
timeline image. Full MapManager metadata/jobs/baselines, world events/metadata,
later-loaded chunks, shared native clocks and final persistence also remain owned
by future full-checkpoint phases. These omissions must not be hidden by opening
the Vanity acquisition gate.

## Exact native proof

Final immutable JAR: `build/verification/skywars2-vanity-core-f93d8136.jar`.
SHA256: `f93d8136a80b1360337f611ea0df0cc3fbf9f3f56abbd99366e8e0c5e8fbf73f`.
11,083 frozen inputs (including both native-runner scripts), 5,262 processed
class/resource entries matched the exact JAR. Source receipt has 14 native Java
entries and 12 legacy Vanity functions. Legacy narrow restoration is historical
evidence; the author's full-rewind contract supersedes it.

`vanity-core` uses only owned TestGamma/TestDelta sessions and real original
TestAlpha/TestBeta participants. It tests:

1. Actual native join/operator enrollment and successful raw Witch acquisition.
2. Genuine attributed lethal damage, life elimination, kill/death/coin reward
   changes and PERFORM_RESPAWN replacement; no direct synthetic call to core death.
3. Checkpoint lives/role/gamemode on that native living replacement, removal of
   a later operator-enrolled newcomer into real observer restrictions.
4. Replaced arrays/sets, profile identity, settings/map readiness, fake scores,
   trigger locks, custom number format, display slot, original native scoreboard
   object handles and old cached ScoreAccess writing to the live restored score.
5. Restored native score/team packets at another actual connected participant,
   including ADD/MODIFY ordering, and analytical zone restoration.
6. Repeated replay and another genuine native death, with at least four real
   running match ticks between restores; match time returns to its checkpoint.
7. Actual disconnect, retained offline UUID record, normal observer login, and
   explicit core replay onto the reconnected body. No automatic body replay claim.

For the repeated-death control, native authority tags are explicitly prepared
before unfreezing because the separate body/tag phase is not implemented. Native
client respawn creates the living body; this test does not disguise it as full
player-body restoration. The fixture cleans only its owned sessions/dropped items,
restores original participants/values/tags/connection/freeze/zone and leaves the
actual match running for independent suites.

Final exact artifact passed **seven gameplay suites in one actual mode1 match**:
`vanity-core`, `vanity-terrain`, `map-lighting`, `time-stop`, `witch-acquisition`,
`witch-lifecycle`, `polymorph`. Owned runner elapsed 54.13 seconds; normal child
shutdown, exact tracked fixture-file restoration, no cleanup errors or retained
locks. The runner snapshots native data/stats/advancement paths for owned TestDelta
as well as existing Alpha/Beta/Gamma snapshots. Earlier `7f70456c` initial core
proof remains archived separately; no native failures were discarded.

All 199 author-owned dirty paths and 31 live loot overrides remain byte-identical.
Main JAR remains `d9087f0a07a321a5a9e57cac7f6f58a2d4ca6a86da9829a52623799f0342e087`.
No physical-client rendering, arbitrary network/input races, native full-body
rewind, full private queue replay, world-wide restore or peak-TPS qualification.

Evidence: `build/verification/vanity-core/{baseline,candidate,sources,final-audit}.json`,
`inputs-f93d8136.json`, SHA-named gameplay log/results, build logs and local commit
receipt. Next work is native player body/lifecycle snapshots and the full
input/timeline/private-state coordinator, retaining both existing terrain/core
components. Full release remains gated until that complete behavior is proved.
