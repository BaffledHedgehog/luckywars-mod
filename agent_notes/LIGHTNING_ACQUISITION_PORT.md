# Ambient lightning acquisition - qualified, 21 October 2026

## Overflow requirement - 22 October 2026 (user)

> кстати - при перековке с помощью удара ванильной lightning_bolt перекованные предметы в датапаке give-лись близжайшему игроку, и был баг, что если инвентарь заполнен, предмет просто удалялся. Я хочу исправить его на этом переходе, чтоб он просто выбрасывался из близжайшего игрока

The port already dropped Survival overflow at the nearest eligible recipient.
The remaining native Inventory.add pitfall was Creative's infinite-materials
fallback: it clears rewards that do not fit. Lightning now reuses the crafting
reforge delivery helper, which inserts bounded portions into available native
slots and drops the complete remainder at the recipient's position. Components
are retained; no replacement recipient or reward reroll is introduced. The
shared helper also closes the same Creative gap in committed crafting rewards.
Drops reuse the existing recipient pickup target and default pickup delay.
Lightning stamps its output before entity insertion to prevent same-tick recapture.

The native regression adds deterministic 16-item loot and checks full/partial
inventories in Survival/Creative: zero/one item fits, 16/15 drop at the nearest
recipient, donor consumption is exactly one, components survive, and repeated
bolt ticks cannot immediately reforge the overflow. Temporary test loot uses
the current modifier/type schema. Each phase removes its own prior drops so
an earlier phase's reward cannot become an unrelated input in the next phase.
Build and four isolated native matches passed on SHA256
91e08fee73f3afd11ae15c3f43b07c68ba930224e57ae53d37816e563c9aeea5:
18 lightning acquisition/shared gameplay results in 46.74 seconds, then 18
crafting/shared gameplay results in 45.09 seconds. Source JAR matches all 137
current Java files; 2643 processed resource files match the release JAR, with
Loom-transformed Fabric metadata and bundled jars verified separately.

Ready archive: build/verification/skywars2-lightning-overflow-91e08fee.jar.
Evidence: build/verification/lightning-overflow-final/ (logs, both result sets
and qualification.json). Temporary fixture loot is removed after shutdown.
Main JAR remains 69de8601; no main-server or resource-pack/template installation.
Tests use native server players without real clients; no new graphics claim.

## Behavior

Every live native lightning bolt, owned or ownerless, can reforge a dropped item
with integer custom_data.transformable=1 within Euclidean radius 4. Capture one
unit per item per unstopped server tick, regardless of overlapping bolts. A bolt
that survives into another tick can capture another unit. This is distinct from
normal/Ultra Reforge processing an entire captured stack.

Use the exact 21 independent legacy flags in source order; several flags can
produce several rewards while consuming one input. No libspell or Ultra arrow64
exception. Delivery and lightning_smith advancement go to the nearest eligible
active player in that dimension, not necessarily the bolt's caster. Inventory
overflow drops at the recipient; same-tick output recapture is excluded.

Unsupported, empty-loot and no-recipient inputs remain unchanged, following the
existing native no-result preservation policy. Transformable inputs resist only
the nearby bolt's native thunderHit path; no global invulnerability or generic
fire/explosion protection is added. The previously qualified top-level type item
protection remains independent.

## Shared implementation and CPU

LbcBolts.ReforgeInput is shared by wand and ambient routes: exact entity/stack
identity, expected count/components, epoch, loaded chunk, loot normalization,
consumption and reward delivery. Existing normal/Ultra mappings and exact64 ammo
behavior remain. LbcLightningAcquisition owns the ownerless scheduling: local
spatial queries around indexed bolts, one request per item, captured-unit budget,
maximum eight unit operations and a 1 ms cooperative budget per server tick. A
single native loot roll can exceed that budget; it is not a hard latency bound.
Requests are not dropped to enforce the CPU budget. Paused-world requests rotate
so active dimensions can progress. No world reads/writes run off the server thread.

Native bolt tick hooks capture before damage; local service queries also cover
items that arrive later in the tick. Supports loaded ordinary dimensions except
legacy nexus/imprinted; loading/ending stages reject work. Time stop is checked
per actual dimension. Stop/unload, replacement and epoch changes invalidate work.
No player is fabricated for a bolt without a caster.

## Evidence and limits

Build and two native matches passed on SHA256
6e5f45f7bd1c3278d3f293b953e96ba621bb9517db1cb4a549a643552f3ab279
in 43.37 seconds. Suites cover native owned/ownerless/managed bolts, persistent
capture, overlap deduplication, active/observer/no-recipient delivery, typed and
untyped source survival, exact mapper flags, independent multiple rewards,
strict unit cap and queue draining, stack replacement/unload, real time stop,
overflow, empty native table, match stop cleanup, prior lightning/item rules,
shooter pools and Infbolt/Ultra regression. Two fixture mistakes were corrected:
observer tools are not an empty inventory; the 1 ms budget cannot promise a fixed
six-tick drain during cold native loot execution.

Archive: build/verification/skywars2-lightning-acquisition-6e5f45f7.jar.
Evidence: build/verification/lightning-acquisition-final/ (build/native logs,
results and qualification). CRC, 2638 processed resource bytes and Fabric/SGUI
metadata verified. Main server JAR and resource pack/template unchanged; not
installed. This does not qualify every broader source loot pool or client graphics.

## Sources and next work

Legacy other/transformpre.mcfunction, other/transform.mcfunction and
other/reduce_count_l.mcfunction; see [bounded acquisition audit](NEXT_ACQUISITION_REVIEW.md).
Tiny Teleport's next native crafting transaction must hide random output until
irreversible ingredient consumption (user). See the audit's click-path findings.
The [hostile Helicopter encounter](HELICOPTER_ENCOUNTER_SOURCE.md) remains separate
from the qualified Boss Helicopter weapon and needs author decisions.
