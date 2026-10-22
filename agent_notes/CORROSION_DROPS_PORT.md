# Corrosion special drops — 22 October 2026

Status: build and two isolated native match cycles **QUALIFIED** on `fb8ccf00`.
The checked JAR is ready locally. Main server, original resource pack and
`resourcepack_template` remain unchanged.

## Source and public items

[Source audit](CORROSION_DROPS_SOURCE.md) records the original functions, token
components, air template and Minecraft 26.3 placement semantics.

| Native item | Public loot | Components and appearance |
|---|---|---|
| `skywars2:corrosion_drop` | `lbc:ported/corrosion_drop` | `{korrosion:1,drop:1}`; repeating command block; `lbc:drop_trap` model, `lbc:tier_ff` tooltip, yellow nonitalic `trap_corrosion` name and one white nonitalic `trap_desc` lore row. |
| `skywars2:corrosion_drop_old` | `lbc:ported/corrosion_drop_old` | `{korrosion_old:1,drop:1}`; undecorated barrier with native name/model and no custom lore or tooltip. |

Both are registered Polymer items. Existing resource-pack models and translations
are reused. Only these individual loot leaves are published; this does not enable
unfinished Lucky Block pools, IMBA acquisition or game modes.

## Terrain and dispatch contract

The cube follows the dropped token, with no nearest-player target. Its inclusive
bounds are `floor(token) - 24` through `floor(token) + 23` on each axis: exactly
48×48×48 cells. Each eligible cell has an independent 50% removal probability,
not a requirement to remove exactly half. Bedrock, liquids, invisible blocks and
containers have no special immunity. There is no direct damage or block-item
loot from ordinary terrain removal.

Random erasure uses native placement flags 2. Replaced containers therefore
retain native inventory ejection. The old variant subsequently clears the token
cell and the cell above with the shared strict-fill helper; that separate fill
suppresses inventory ejection if its cells survived the random mask. It retains
its own two-cell build-height and `MAX_BLOCK_MODIFICATIONS` checks. The random
cube has no fill-volume gate and can partially operate across build-height
limits.

Ordinary corrosion ends with one native dust packet containing 300 particles,
color `[0.255,0.204,0.302]`, scale 2; old corrosion ends with one packet containing
5000 large-smoke particles. Both use the original fractional token position,
spread 1 on each axis and speed 1. No particle-count reduction is introduced.

`LbcSpecialDrops` retains shared admission, actual thrower capture, one unit per
server tick, fake-item guards, epoch validation and time-stop handling. Corrosion
is dispatched after TNT and before pit/cage traps. Multiple recognized flags
share one debit. The complete source placement footprint through `origin + 48`
must be resident before admission; no chunk is synchronously loaded.

## Processing and causal consequences

The implementation traverses resident sections without decoding or allocating a
110592-entry structure at activation. Successful positions use primitive
storage and a compact occupancy mask. Native non-strict edge-shape and neighbour
updates are retained. Random draw and face visitation order may differ from the
datapack while preserving the independent removal distribution.

After an optional timing question remained unanswered, the implementation choice
was stated explicitly: progressive processing through the existing Stonekivi
terrain scheduler, with a shared soft budget of 3 ms per tick. Every job retains
its captured origin, fractional particle position, owner and epoch after the
token is consumed. No accepted activation is discarded to bound the queue.
Intermediate changes use ordinary native block-update delivery.

An erasure step permits at most 4096 cell inspections, 128 successful placements
or approximately 0.5 ms. Shape/update steps allow at most 128 operations or the
same time slice. These are cooperative limits; an individual native callback can
exceed them. Other terrain spells share the existing scheduler fairly. This is
not parallel world mutation or a claim of a hard MSPT ceiling.

After admission, the one-block neighbour halo must also remain resident before a
job advances, avoiding synchronous chunk loads from native shape reads. If it is
missing, the committed job waits without losing its remaining work. Time stop
likewise pauses work. Match stop/epoch replacement cancels outstanding work
quietly. There is no owner-death/disconnect cancellation or new entity lifetime.
Different concurrent activations can interleave; dispatch order is preserved as
admission order, rather than pretending that queued work is one atomic command.
For manually fabricated tokens combining corrosion with synchronous pit/cage
flags, the pit/cage writes can precede the queued erasure. Source acquisition
uses separate tokens; atomic legacy ordering of such cross-family composites
is not qualified by this port.

The actual thrower is retained independently of the consumed token. Erasure runs
through the existing indirect-kill write scope with WILD terrain consequences,
so removed supports can retain fall/void kill credit, including self/allied
players. A later attack still replaces an older indirect cause. No artificial
damage, owner-death lifetime or cap discarding accepted effects is added.

## Verification and ready artifact

`tools/smoke_corrosion_drops.py` passed two actual mode-1 fixture matches: 36 suite
results, 62.31 seconds including boot, map loading and the earlier pit, cage,
Creeper, TNT and chest regressions. Both matches exercised actual stop/epoch
cleanup. Initial failed runs are retained in the evidence directory: their
allied-player assertion incorrectly expected nonlethal fall credit and ignored
the match's initial `FALL_DAMAGE=false` grace. The fixture now checks nonlethal
damage separately, scopes/restores that gamerule, and uses native lethal cow
fall/void hits for kill-credit checks. Production behavior was not changed to
satisfy those incorrect expectations.

The qualified fixtures cover:

- Complete native give/loot components and distinct old-token presentation.
- Fractional negative coordinates, both 48-cell edges and untouched outer faces.
- Bedrock, water, light and structure-void eligibility, and a 4096-block
  statistical mask even with `MAX_BLOCK_MODIFICATIONS=1`.
- Native container item ejection with complete components; skipped-container
  retention; old strict center suppression and its separate two-cell limit.
- Partial build-height placement and unloaded/extra-endpoint chunk admission.
- Actual support removal followed by native lethal fall/void sources, later
  attacker precedence, and nonlethal allied-player fall damage.
- Shared one-unit debit, combined normal/old flags, concurrent jobs, time stop,
  fake visuals, unloaded tokens, stale epochs and quiet cancellation.

The ordinary shared scheduler also processed a 4096-bedrock fixture without the
test drain hook. In each match it finished in 3 ticks and exposed intermediate
progress. This is a bounded liveness observation, not a benchmark of the full
110592-cell cube, many simultaneous casts, or a hard server-capacity guarantee.

Ready artifact: `build/verification/skywars2-corrosion-drops-fb8ccf00.jar`.
SHA-256: `fb8ccf00f62582a6ec03855fbc81571edabf2d556eaca5e0488e336c8e8403cc`.
Evidence: `build/verification/corrosion-drops-final/` (`build.log`, `native.log`,
`server.log`, `results.json`, `qualification.json`, and earlier failed evidence).
All 157 Java files match the source JAR, and 2661 processed resource files match
the release. CRC, transformed Fabric metadata and bundled JAR entries passed.
The isolated server stopped normally, released locks and restored its previous
mod/configuration/profile/scoreboard backups. Main JAR remains `69de8601`, and
this ready artifact has not been installed there.

Real-client graphics/throw packets, full natural falling trajectories,
departed-author/restart persistence and worst-case saturation remain unqualified.
The existing Polymer fake-player chunk-packet-context diagnostic remains; there
were no failed suites in the final run. The cross-family composite ordering
limitation above is explicit. Next bounded migration: remaining special drops,
starting with the zombie-TNT source audit; unfinished modes stay gated.
