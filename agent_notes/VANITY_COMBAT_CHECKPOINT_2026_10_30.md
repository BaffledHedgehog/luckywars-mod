# Native combat histories and references for Vanity

## Contract and release boundary

The [full author rewind contract](VANITY_ACTOR_CHECKPOINT_2026_10_30.md) remains
mandatory. This adds one native state component to the existing terrain/core/body/
actor/ledger owners. It is NOT the complete transaction. Vanity acquisition remains
disabled and the full original25 Chaos gate stays CLOSED. No main installation,
restart, push, pool reduction, RP/template or pending-question-file changes.
Questions belong in CHAT; preserve the author's answers and live loot overrides.

## Native combat owner

`MatchCombatCheckpoint` captures living registered actors and connected players
alongside `MatchActorCheckpoint`. Its link/history replay runs AFTER actor bodies,
player bodies, native owner/target/passenger links and player actor ledgers. Native
body clocks must already be restored. UUID lookup uses actual current player sessions
and the actor owner's original instances, including hidden registered sections.

The image contains raw last-hurt-by player/mob UUIDs, the last attacked living target,
memory/timestamps and last DamageSource; complete native CombatEntry values, damage/
start/end clocks, in-combat/taking-damage flags; recent kinetic targets with their
world-time ages and map default value; auto-spin weapon data and equipped-stack
alias; and native last-position/climbable-position memory. Mutable collections,
entry values and standalone item stacks are copied independently. Every replay
rebuilds them again. World-time stamps are rebased to preserve their captured age;
entity tick clocks remain tied to the existing body owner's restored tickCount.

Removed or offline player references never bind an old body. Native EntityReference
fields keep unresolved UUIDs and the phase reports explicit pending identities.
Plain last-attacked and kinetic references bind the current body or remain pending.
This composes with explicit reconnect replay; no automatic pending-login hook is added.

The native CombatTracker remains attached to its actual mob/player. Its list is
updated directly, with fresh CombatEntry records and the captured clocks/flags.
Replay never calls recordDamage, applies another hit, grants an award, or invokes
onEnterCombat/onLeaveCombat callbacks. Current players receive native combat-state
packets after the state is restored. No second damage, death or reward system exists.

## Source identity and accepted causes

A plain DamageSource replacement would erase authored behavior: WILD and physical
shard markers, electric self-short-circuit identity, freeze NO_KNOCKBACK overrides,
trial/nuclear behavior and nested accepted attribution wrappers. The image therefore
retains ORIGINAL source objects/classes and their immutable accepted causes. A narrow
mutable accessor rebinds only the native direct/causing entity fields. The read-only
`LbcIndirectKills.checkpointWrappedSource` exposes its own wrapper parent; the entire
nested source graph is recorded by identity and all native links are rebound.
Accepted causes/orders are not recomputed from current authority ownership.

Historical direct anchors can already be removed, or never registered, when damage
is recorded. Those original references remain historical; replay does not register,
revive or fire them again, and does not mistake them for an offline live actor.
This preserves their source identity, not an arbitrary mutable detached body's full
private state. Such body mutations, unknown mutable source-subclass fields and the
private attribution/TimeStop service stores still need their full owners before release.

## Exact native proof

Final immutable artifact: `build/verification/skywars2-vanity-combat-04bafa02.jar`.
SHA256: `04bafa027667ebe970f30d7deaa89d4cfa2626d56207b77bef47e03c7751a811`.
11,135 frozen inputs, including both runner scripts; all 5,301 processed class/resource
entries matched the exact JAR. Source receipts cover 36 native entries, 12 legacy
Vanity functions and 11 application damage-source owner files. Build passed offline.

The exact final artifact passed FIFTEEN gameplay suites in ONE actual mode1 match:
`vanity-actors`, `vanity-player`, `vanity-core`, `vanity-terrain`, `map-lighting`,
`time-stop`, `witch-acquisition`, `witch-lifecycle`, `polymorph`, `effect-layers`,
`projectile-body-collision`, `entity-merging`, `momentum-teleports`, `glass-armor`,
`indirect-kills`. Runner elapsed 68.23 seconds. Normal child exit, exact tracked file
snapshots restored, no suite/cleanup failures or retained locks. Focus mode leaves
`qualified:false`; the generic runner measurementLimit describes its default suite
set, while this explicit focus/completed list defines the actual proof.

`VanityActorChecks` now records genuine native cow/player damage before capture,
two nested accepted WILD shard wrappers, positional/fall combat data, a removed
historical direct anchor and a source-specific NO_KNOCKBACK override. It mutates
combat history, recent targets and spin weapon state, kills the original bodies,
performs real native respawn twice, waits at least four real match ticks and then
exercises actual disconnect/login replay. Assertions require current native killer/
attacker/target identities, the ORIGINAL source objects/classes and nested cause,
exact entry values/flags, kinetic target identities/ages/default, and the restored
equipped-stack alias. Offline references must be null/pending rather than old players.
The prior native ledger, dimension-alias, tracked hook and passenger tests also run.

The first `14d0d3ef` actor-combat suite passed, but its combined run exposed a fixture
isolation defect: during the core scenario's four real ticks, Wrath's authentic 1%
conversion chance could mark borrowed TestBeta and kill it on the next holder death.
The core test passed while leaving that body dead; later TimeStop/indirect-kill checks
failed. The previous `6dde778e` passed the same 15-suite control, and `14d0d3ef` passed
actor/TimeStop/indirect-kill isolation. Logs retain the actual extra TestBeta death.
The core fixture now places borrowed Alpha/Beta outside 30 blocks and asserts they
survive the owned holder's death. Genuine holder death, core restoration, four real
ticks and authority transfers remain tested. Wrath gameplay code is unchanged.
The final `04bafa02` passes the original combined 15-suite order.

All 199 author-owned dirty paths and 31 live override files remain byte-identical.
Main JAR remains `d9087f0a07a321a5a9e57cac7f6f58a2d4ca6a86da9829a52623799f0342e087`.
Evidence: `build/verification/vanity-combat/{baseline,candidate,sources,final-audit}.json`,
`inputs-04bafa02.json`, SHA-named logs/results, parent/isolation controls, numbered
build logs and the bounded local commit receipt. Preexisting fixture startup warnings
are retained; no physical-client rendering, arbitrary-map or peak-TPS claim is made.

## Required follow-up

The complete input/writer/lifecycle transaction and automatic offline replay remain
unfinished. Native freeze still permits player input/ticks. Private abilities,
encounters, accepted attribution stores, AI goals/navigation/sensors/RNG, multipart/
camera/container references, TimeStop's underlying motion/identity/clock state,
world clocks/events/metadata, entity-region/pending IO and map jobs/baselines still
require their owners. Preserve the existing components and do not enable acquisition
or install a partial full25 release before the complete rewind is qualified.
