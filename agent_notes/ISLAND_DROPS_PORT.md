# Native Island Raise and old Island Lower

22 October 2026. Build and two actual isolated mode1 matches qualified.
[Source audit](ISLAND_DROPS_SOURCE.md).

## Failed-copy fallback (user, 22 October 2026)

> при таком раскладе пусть только телепортация существ исполнится, а остров не поднимется (ну и не удалится естественно), предмет удалится

This supersedes legacy continuation after a rejected copy: both variants preserve
source/destination terrain, execute only their native entity-teleport sequence,
and debit one token. Old Resistance is granted only after a successful copy.
Source descriptions of the original unconditional clear remain historical facts.

## Native behavior and causal bookkeeping

Normal public leaf is lbc:ported/island_raise_drop; old is
lbc:ported/island_lower_drop_old. Exact source item designs are retained.
Normal copies31x31x31 from the selected player's fractional origin and translates
its block contents +46Y, then clears the source and teleports eligible entities
within16 by+46. Old copies17x13x17 down13, clears only after successful copy,
teleports nonplayers within9 down13 and grants its selected player ResistanceV
for40ticks only after a successful copy. Role/service protections apply; self
and allied players remain eligible. Old does not teleport any player.

Native clone, strict fill and teleport commands execute synchronously through
the registered Brigadier endpoints. Native BE components/inventories, block
scheduler copy, source flooring and vanilla update/failure rules remain. The
user fallback above prevents clearing on a rejected copy. Full source and
destination height bounds are validated before invoking clone: native clone can
partially copy a region straddling the build limit and still report success.
Combined source flags
run normal before old with one debit; the second nearest-player selector uses
the token/players' actual new positions. Whole-pair loaded preflight projects
that movement before mutations and never requests chunks or adds tickets.

A bounded operation-local candidate query replaces per-block spatial living
queries during terrain attribution. Per-block live bounding/feet/support shape
checks remain; nested scopes restore correctly, native newly loaded living actors
join active scopes, and neighbor writes outside the declared region use the
ordinary query. Hazards, removed support and newly suffocating destination blocks
retain the actual thrower through common scopes. Successful mob/player movement
also captures target-local arrival wall/lava/water/fire/magma contacts, including
unchanged terrain outside the clone footprint. Reads never load chunks; contact
expiry prevents unrelated later wall deaths from inheriting a stale arrival.
This does not globally claim existing terrain. Ordinary damage still uses its
existing displacement path. Later accepted player attacks override older causes,
even after native last-hurt references expire.

Active recognized special-drop admission now persists its match epoch on the
item entity as well as the in-memory queue. Chunk reload cannot admit an old
pending token into a fresh match. Inventory components and one-unit debit remain.
No independent island entities/jobs/timers remain after synchronous dispatch;
the common pending-token lifecycle covers freeze/unload/actual stop.

## Qualification

Offline Gradle build and `python tools/smoke_island_batch.py --server-dir
build/verification/key-reuse-server` passed. The runner recorded 124 suite results
across two actual mode1 matches in 93.46 seconds, including all previous
special-drop regressions, advanced anvil/enchantment tests, actual stop probes
and post-stop cleanup. Only the known fake-bot Polymer packet-context ERROR pair
remains in the log; there are no check failures.

Ready artifact: `build/verification/skywars2-island-batch-97debb18.jar`.
SHA256: `97debb18761992361a0712fdbd5e061d17953e919d65a1984e59dfb7ecc5f3d6`.
All 211 Java sources match the source JAR byte-for-byte; all 598 compiled classes
and 2692 processed resources match the tested release. Evidence and qualification
manifest: `build/verification/island-batch-final/`. Main JAR96ee50a7 and the user's
Reroll source6ef80947 remain unchanged. Isolated server stopped, files restored,
locks released. Not installed. Actual RP and template remain unchanged.

The island fixtures exercise source presentation and both public loot leaves,
negative fractional flooring, full source clearing, exact block destinations,
BE item components/inventories without loose duplication, native copied block
tick trigger/priority, inclusive entity spheres, service roles, allied Creative
and thrower selection, volume rejection and both build-height failure fallbacks.
Actual native environmental deaths check support loss, fall/void, cloned hazards,
preexisting arrival wall/lava/water outside the clone footprint, newer attacker
priority even after clearing native hurt references, and expired arrival contact.
They also check normal-before-old mixed flags, one debit, actual sequential
stack activation, freeze, unload callback, stale/fake/unresident rejection and
actual match-stop epoch invalidation without replay. WILD arrival lava damage
is checked against self/allied players with team friendly fire disabled.

Retained development evidence: `build-attempt1/` contains the fixture's native
TickContainerAccess/LevelChunkTicks compile correction. `attempt1/` and
`attempt2/` exposed a fixture readiness issue: blocks were resident before the
native entity section became accessible to bounded searches. Fixtures now wait
for actual entity loading/query visibility; production entity queries were not
bypassed or manually indexed. `attempt3/` exposed native partial clone success at
the world ceiling; full-height prevalidation fixed production behavior before
the final passing run. The stop probe uses the resident player's section after
geometry-fixture chunk tickets have been released.

Limits: native packet capture/tests do not qualify real client pixels/audio,
actual disk restart/chunk unloading/disconnection, natural human falling or
movement, offline coin rewards or peak load. Unload guards use service callbacks.
Arrival provenance is contact/state-based; an unrelated teleport away and back
within the same tick is not separately qualified. Bounded support reuse avoids
repeated spatial searches by construction; this run is not an MSPT/capacity
benchmark and its elapsed time includes startup, maps and prior regressions.
No full reward pool or game mode is opened by these two leaves.

Next source-only candidates: [old Bob / old TNT Drone audit](NEXT_DROPS_AFTER_ISLAND_SOURCE.md).
They remain unimplemented.
