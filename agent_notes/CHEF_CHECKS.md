# Chef native fixture

Status: qualified in the integrated root build and two-match run; exact evidence and limits are recorded below.

User ruling supersedes the old item-display Chef: a real Mannequin pursues its fixed victim with server pathfinding, infinite building supplies and tools. It has infinite HP; every20 received damage produces200ticks of stasis with no movement/actions/damage. Void recovery chooses a nearby loaded surface and continues. Root owns the gameplay implementation and runtime/build orchestration. The checks agent owns only ChefChecks.java and this document.

Planned public entrypoints are `ChefChecks.run(GameController)`, `stopProbe`, `cleanup`. They will reuse full participant/profile/advancement/statistic/native-effect/tag/attacker restoration and reject pending combat, existing Chefs/fountains or dimension Time Stop before fixture mutation. A resident bounded airbox and platform will be snapshotted before any mutation, including block entities/containers. Controlled NPC goals must remain inside that saved region; generated drops/xp and fixture actors are disposed. No assertion claims general-world Baritone behavior or client pixels.

Planned cases: actual material melee consumption and fixed victim, real Mannequin type, collision and victim-only sword damage, actual direct death credit and newer-hit precedence, obstacle mining/bridge/pillar within saved terrain, damage partial accounting/20threshold/200ticks frozen/wake, external impulse rejection, native Time Stop, Greed/technical/allied/observer boundaries, two independent owners/targets, void recovery to the saved platform, and real match-stop epoch cleanup. Terrain consequences require their captured owner and newer-hit priority when naturally reachable in the bounded fixture.


## Implemented boundaries

The fixture snapshots a resident33x17x33 region before clearing it and constructing a stone floor. Controlled goals and terrain stay near its center. Explicit boundaries call service.step then native Chef.tick, with the planner budget reset to represent a new explicit boundary. This does not claim elapsed wall-clock time or an actual ten-second wait.

Assertions cover actual accepted material melee consumption at zero mana, no consumption for indirect magic, native vanilla MANNEQUIN type/profile/equipment/tools, separate fixed victims/owners, real victim-only sword damage and direct death credit. Actual native7+13 damage leaves HP full and enters200ticks of stasis; dimension Time Stop pauses that clock,199 boundaries retain immobility/no attacks, and wake permits native movement. Native knockback is rejected during stasis.

A narrow bedrock corridor forces native mining of obsidian headroom beyond36 stationary boundaries, followed by a separate saved gap that requires native bridge placement. A higher saved platform exercises ascent. A25-block-wide,16-block-tall bedrock wall exercises a real long detour with search work continued across slices. Two cows standing on the actually mined block acquire the summoner's terrain cause; native fatal void damage verifies the real death credit, and a later accepted real player hit takes priority for the second cow. This tests actual support mutation plus an explicit terminal native damage boundary, not a natural full void fall.

Void recovery verifies a loaded solid surface and preservation of the fixed victim. The nearest surface may be outside the high-altitude saved fixture; in that case the actor is immediately repositioned into the saved region before further AI ticks. Continued pursuit is checked inside the controlled region. No AI terrain writes occur at the external recovery location. Technical targets, allied players, observers, actual dimension Time Stop and a nearby Greed sword attempt are guarded. Actual match-stop probe/cleanup verifies actor removal, pending work and epoch invalidation.

Coverage is limited to the bounded native obstacle cases above. General-world navigation, arbitrary inventories/enchantments, client skin pixels, actual disk reload and sustained load are not qualified here. No builds or servers were run by this agent.

Final review additions: stasis rejects actual native `snapTo` and `teleportTo` relocation. The real-stop probe stages one native Chef and one60tick crafting fountain (`pendingCount==2`); cleanup requires pending work0 after actual stop. Root reported whole-project compile passing before these final additions; this agent did not build or run servers.


## Integrated runtime qualification (24 October 2026)

Build `b0dfd5ca` passed74 recorded suite results in two actual mode1 matches (168.90s), including Fartus/Chef and the existing shared regressions. Exact 311 sources/857 classes/2797 resources match the tested installed JAR. Main installed while stopped, rollback `0fb48f1a` retained, server not started. Evidence: `build/verification/fartus-final/`. Native synchronous tick boundaries are explicit simulations; no elapsed ten-second wait, real client pixels, actual disk restart/disconnect or general-world/load guarantee is claimed. This integrated result supersedes the earlier pending-status statements by the individual source/check agents.


## Author skin runtime qualification (25 October 2026)

Build `2558af6b` passed 10 recorded suite results in two actual mode1 matches
(56.58s), including the existing Chef fixtures with static native wide skin
profile and native stream-codec round-trip assertions, and actual stop cleanup.
The original PNG bytes occur unchanged in both the built JAR and the generated
Polymer server resource pack. Exact 313 sources/866 classes/2801 resources match
the tested artifact. Main installed while stopped; rollback `07ff2ecd` retained;
server not started. Evidence: `build/verification/chef-skin-final/`.
No real client pixel verification; clients need the Polymer server pack loaded.
