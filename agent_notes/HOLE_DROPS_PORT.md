# Two hole special drops — 22 October 2026

Status: build and two isolated native match cycles **QUALIFIED** on2478ebdc.
The checked JAR is ready locally. Main server, original resource pack and
`resourcepack_template` remain unchanged.

## Source and public tokens

[Source audit](HOLE_DROPS_SOURCE.md) records exact legacy function/loot paths,
native fill semantics, distinct acquisition presentation and existing RP keys.
This port exposes two individual leaves; the enclosing Lucky Block random pools
and unfinished modes retain their existing gates.

| Item ID | Public loot ID | Custom data | Presentation |
|---|---|---|---|
| `skywars2:hole_drop` | `lbc:ported/hole_drop` | `{yama:1,drop:1}` | Repeating command block; model `lbc:drop_trap`; tooltip `lbc:tier_ff`; yellow nonitalic `trap_hole` name; one white nonitalic `trap_desc` lore row. |
| `skywars2:hole_drop_old` | `lbc:ported/hole_drop_old` | `{yama_old:1,drop:1}` | Undecorated barrier with ordinary barrier name/model, no custom name, lore or tooltip. |

Each public table emits one native registered Polymer token. Existing models,
textures and translations are reused; no new RP assets or translation keys are
introduced. The IMBA normal acquisition variant is documented in the audit but
is not published as another loot leaf by this change.

## Shared dispatch and admission

`LbcSpecialDrops` owns actual thrower capture, activation and one shared stack
debit. The source order places normal and old holes after the thirteen-TNT drop
and before the five cages. A token with several recognized flags executes those
effects in source order and loses one unit at most once per server tick.

The holes reuse the existing cage target helper: nearest eligible nonspectator
player in the token's dimension, including self, allies and Creative players,
with no radius cap. Observer and protected service-role exclusions still apply.
The supplied target position controls geometry; it is floored independently on
each axis. There is no teleport or recentering. No eligible target means no
terrain writes, while the shared source debit still occurs.

`LbcHoleDrops.ready`/`spawn` retain the existing dropped-token guards: removed or
fake-age5999 tokens, stale item epochs, inactive/ending matches, the wrong arena
and stopped time cannot activate. The selected target's footprint plus one
neighbour, X/Z offsets−2 through+2, must have resident FULL chunks. Admission
does not force-load chunks; failed admission defers consumption. Held items and
temporary fake `/give` visuals do not activate.

## Exact terrain behavior

All cuboids are inclusive X/Z−1 through+1 relative to the target's floored feet.
The table preserves each function's command order; default block states apply.

| Step | Normal hole: strict fill | Old hole: ordinary fill |
|---|---|---|
| 1 | Air at Y0 through Y−15 | Air at Y0 through Y−15 |
| 2 | Cobweb at Y−16 | Cobweb at Y−16 |
| 3 | Oak log at Y−18 | Soul soil at Y−18 |
| 4 | Cobblestone at Y−19 | Soul fire at Y−17 |
| 5 | Fire at Y−17 | No fifth command |

Normal geometry nominally writes180 cells; old geometry171. Old does not add a
Y−19 floor. Neither creates actors, titles or another input queue. Existing
causal support detection performs local collision/entity queries when removing
a solid supporting block; no new persistent world scan is added.

`LbcDropBlocks.fill` is the shared native-fill implementation extracted from
cages. Each fill uses default-state `BlockInput` with flags818 for strict and258
for ordinary placement. Ordinary fills retain the old states of successful
changes and defer `updateNeighboursOnBlockSet` until that particular fill ends.
Strict fills allocate no deferred-update list, avoid the explicit old-state read
needed by that list and omit the ordinary neighbour pass. Native container
replacement suppression remains; no chest-reward inventory-ejection routine is
applied to these terrain traps.

Strict placement suppresses the fire block's initial `onPlace` callback, including
its initial scheduled fire tick. The port does not invent that tick or force the
logs to burn. Native existing scheduled ticks and later updates can still affect
the blocks. Old soul fire retains ordinary placement and neighbour behavior.

Build-height endpoint validation occurs separately for each complete fill. An
invalid cuboid is skipped without clipping it, and later commands still run.
The native `MAX_BLOCK_MODIFICATIONS` volume gate also applies per complete fill.
If a limit of100 rejects the144-cell air fill, subsequent9-cell layers can still
execute, just as separate source commands do. This gate also corrects the same
omission inherited from the former cage-only helper.
There is no whole-function vertical clamp or new general replacement/world-border
policy. Nominal cell counts describe valid placements, not low-world-edge output.

## Causal ownership and cleanup

The actual token thrower remains the cause; the nearest trapped player and pickup
target do not become authors. For a player-owned token, all source writes run
inside `indirectKills.trapChanges(level, player, true, writes)`. The `true` carries
the approved WILD policy for native fire, support-removal falls and resulting
void consequences, permitting self/allied harm while retaining observer and
service protections. Ownerless tokens write terrain without inventing an author.

Support removal preserves the write scope's complete `Owner` record, including
WILD permission. It does not look up a still-present author when the supporting
block is deleted, so departure of the author does not silently discard the
captured cause. Existing indirect-kill machinery owns later causal resolution;
these drops add no native actor lifecycle or owner-lifetime rule.

Terrain has no new restoration timer. Existing map-change journaling handles
later arena reload/cleanup. Native environmental consequences retain their
existing behavior; match cleanup is not a new timed terrain rollback.

## Runtime qualification

Build and two actual isolated mode1 fixture matches passed on SHA256
2478ebdcd3d844d12c3b46e23fc532ec20cae120c90c79b1544f8a6d61bd078a.
The runner completed30 suite results in51.49 seconds, including the new holes,
cage/Creeper/TNT/chest regressions and all five families' real match-stop probes
and cleanup. Hole checks cover complete native give/ported presentation, distinct
old barrier appearance, all geometry cells and untouched borders/bottoms, no
target teleport, native container suppression, both flags/source order/one debit,
no-target debit, fake/held exclusion, per-unit ticks, admission without chunk
loading, limit100 rejecting only the144-cell fill, stopped time and stale epochs.

Real native cows lose actual supporting collision blocks through pit placement,
then die to native FALL or FELL_OUT_OF_WORLD damage sources with the thrower's
kill credit. A later actual Beta attack overrides Alpha's pit cause. Native
fire-block contact and death credit pass for normal and soul fire; allied-player
fire contact retains WILD harm and the causal author. These use native contact
sampling and environmental sources, without replacing them with direct player
damage. They do not qualify a complete natural falling trajectory. Mob-kill
statistics and fixture state are restored. A first-run cleanup failure from
mutating a live entity iterator was corrected by snapshotting before removal;
its logs remain beside the final passing evidence.

Ready archive: build/verification/skywars2-hole-drops-2478ebdc.jar.
Evidence: build/verification/hole-drops-final/ (build/native/server logs,
results.json and qualification.json). All154 Java sources match the sources JAR;
2659 processed resources match the release. Fabric metadata and bundled jars
were verified separately after Loom transformation; CRC passed. The isolated
server stopped normally, saved configuration and its previous JAR were restored,
and runner/world locks were released. Main remains69de8601, not installed.

Real-client visuals/throw packets, complete natural fall/void trajectories,
author-departure/restart persistence and measured peak capacity/MSPT remain
unqualified. The preexisting Polymer chunk-packet-context diagnostic on native
fixture bot join remains. No broad Lucky Block pool or unfinished mode is enabled.

Next: audit the corrosion/corrosion_old source branches before publishing their
individual outcomes; keep encounter/reward questions separate.
