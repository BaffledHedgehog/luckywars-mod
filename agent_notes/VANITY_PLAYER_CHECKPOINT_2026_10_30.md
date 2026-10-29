# Native player-body component for the Vanity checkpoint

## Contract and release boundary

(user) Vanity must rewind the FULL match: map/world, ALL players including killed,
offline and newly joined players, native/private counters, items, effects, statuses,
attributes, tags/NBT, positions, dimensions and motion. Captured offline players
retain their restored state until login; later newcomers become observers.

This stage supplies the player-body component alongside the existing terrain and
core components. It does not enable Vanity acquisition or complete the full rewind.
`Mode.VANITY.implemented()` remains false and the full original25 Chaos release gate
remains CLOSED. No main installation/restart/push or narrowed pool is authorized for
this partial stage. Questions stay in CHAT; the author's question file is untouched.

## Snapshot and native restoration

`MatchPlayerCheckpoint` binds an independent native NBT image to the original
GameController, match epoch and UUID, not to the continued existence of the old
ServerPlayer. It supplements native serialization with:

- A complete native AttributeMap value copy, including transient modifiers and
  supported defaults. Native NBT saves only permanent modifiers; player loading also
  overwrites movement_speed's base with abilities.walkingSpeed. Both are corrected.
- Serializer-copied SynchedEntityData values including defaults, allowing future
  flags/metadata to be cleared. Personal inventory, ender chest, equipment, complete
  item components, cursor and the inventory crafting grid are restored independently.
- Raw motion, position, camera rotation, last native client input and reviewed native
  transient clocks/state through `MatchPlayerRuntime`. Native NBT.load otherwise
  zeros velocity components above 10 and serializes several timers as shorts.
  Runtime fields restore AFTER native teleport resets head/previous rotations.
- Deep, non-destructive `OwnedEffectLayers` images, including external baselines
  and separate owners. Every restore clones the image again; withdrawing an owner
  after a previous restore cannot consume the saved baseline or leases.
- Native signed/hidden effect chains and ShadowAbsorption's own serialized ledger.
  The shadow maximum is reconciled after the complete attributes restore, then the
  exact captured health metadata is reinstated. Existing shared effect owners remain
  authoritative; there is no second effect or hidden-health implementation.
- Exact native cooldown group instances and clock, recipe book known/highlight/settings,
  full-width fire/air/freeze state, pose, attack/use-item timers and useItem's alias
  to the restored held stack. The reviewed transient field allowlist fails explicitly
  if an expected native field changes; it does not recursively clone server objects.

The full owner must restore terrain/core/roles before this body phase. On a currently
dead body, restoration invokes the real native PERFORM_RESPAWN handler. A narrow
UUID/server/thread scope makes GameController.respawned skip ordinary kit/life/reset
processing for this replacement only. It never manufactures another login session.
Normal native respawn outside that scope remains unchanged. Native dimension transfer,
teleport bookkeeping and explicit health/XP/attribute/effect/metadata/motion/inventory/
recipe packets publish the saved state. Native tracking viewers receive metadata and
motion; existing FullInvisibility/SignedEffectVisuals refresh their own state.

Personal FUTURE cursor/crafting contents are discarded before close/respawn, avoiding
new item drops from them. Non-personal menus must be checkpointed/closed by the full
owner's earlier container phase; this component rejects them before mutation. It also
rejects offline sessions and unloaded destinations rather than fabricating players or
synchronously loading arbitrary chunks. An offline image can later be replayed onto
the real current UUID session after normal login and the preceding core phase.

## Progress without replaying rewards

`MatchPlayerProgress` snapshots native statistic values and native-codec advancement
criterion dates/progress. Restore replaces the native maps, clears/rebuilds native
listeners/visibility and publishes a client reset plus the native progress flush.
Future-only stats receive zero updates. It never calls award/awardStat or replays
loot, XP, functions or challenge callbacks. A narrow award guard also prevents body
loading from accidentally granting criteria in the middle of restoration.

Selected advancement tabs are explicitly republished after the client reset, even
if the server's selected holder did not change. Native recipe loading normally adds
to existing known/highlight sets; the exact packed native apply replaces them here.
Post-effects are cleared before load because native loading appends to that list.

## Native proof and remaining work

Final immutable artifact: `build/verification/skywars2-vanity-player-55cec70b.jar`.
SHA256: `55cec70bc75eb44c7f9075c1ecd961ad0045d35796e875e080ba96b4a0875672`.
11,103 frozen inputs (including both runner scripts), 5,276 processed class/resource
entries matched the exact JAR. Source receipts cover 22 native Java entries and 12
legacy Vanity functions; the author's full-rewind contract supersedes that legacy
narrow replay. Build succeeded offline. The final exact artifact passed NINE native
gameplay suites in ONE actual mode1 match: `vanity-player`, `vanity-core`,
`vanity-terrain`, `map-lighting`, `time-stop`, `witch-acquisition`, `witch-lifecycle`,
`polymorph`, `effect-layers`. Runner elapsed 56.80 seconds, normal child shutdown,
exact tracked fixture-file restoration, no cleanup errors or retained locks.

All 199 author-owned dirty paths and 31 live loot overrides remain byte-identical.
Main JAR remains `d9087f0a07a321a5a9e57cac7f6f58a2d4ca6a86da9829a52623799f0342e087`.
No RP/template or question-file changes, installation, restart or push occurred.
The earlier focused passing artifact `badf51be` remains archived. Earlier failed
fixtures are also retained: the disabled vanilla achievement, a test that did not
unwrap the native respawn bundle, and a test that incorrectly expected known
movement to survive a subsequent native movement ACK unchanged. Those observations
also led to the real head-rotation ordering correction and explicit tab publication.

Evidence: `build/verification/vanity-player/{baseline,candidate,sources,final-audit}.json`,
`inputs-55cec70b.json`, SHA-named gameplay log/results, numbered build logs and the
bounded local commit receipt. The runner's generic measurementLimit text describes
its default suite set; the explicit focus/actual completed list above defines THIS
proof. Focus mode deliberately leaves `qualified:false`; stage proof does not imply
full release qualification.

`VanityPlayerChecks` uses an exclusively owned TestGamma session in a real mode1
match, and the existing participant/core owners. It performs actual lethal damage,
Nether transfer, native checkpoint-driven respawn, repeated restoration, at least
four actual running match ticks, real disconnect and reconnect. Offline restore is
explicitly rejected; the saved image is explicitly replayed AFTER genuine login.
This is not a claim that automatic pending-login coordination is implemented.

The fixture checks item components/large enchantments, inventory/ender/craft/cursor,
base and transient attributes, abilities distinct from movement attributes, signed
hidden effects, independently removable effect leases, hidden healed HP, full-width
fire timers, pose/use/attack state, recipes/cooldowns/stats, progression and native
packet payloads. Respawn packets inside Polymer bundles are recursively inspected.
Movement/head/known input are checked immediately after restore; an actual native
ACK then counts as subsequent input and may legitimately recompute known movement.
Raw motion still must survive that ACK and appear exactly in the outbound packet.

Production deliberately filters vanilla advancements, and its registered LBC tree
has no reward-bearing entries. The test completes a registered real advancement
AND a scoped native AdvancementHolder whose genuine native award grants 77 XP.
The latter does not modify the registry/filter. Both progress records must restore,
and XP must remain at the saved value over repeated restores. This separates native
UI/listener proof from actual reward-replay proof without changing production pools.

The full input/writer quarantine and multi-component coordinator are STILL REQUIRED.
Native simulation freeze alone permits player ticks and is not an input barrier.
Also pending: actor identities/references/passengers/cameras, non-personal container
state, all private ability/service queues/counters, native combat/portal/reference
state outside the reviewed allowlist, world clocks/events/metadata and later-loaded
chunks, full MapManager jobs/baselines, automatic pending offline-body restoration,
and final durable persistence. Nothing here opens the acquisition/release gate or
claims a complete player/world rollback. No physical-client, network-race, arbitrary
GUI/actor, full-world or peak-TPS qualification is claimed.
