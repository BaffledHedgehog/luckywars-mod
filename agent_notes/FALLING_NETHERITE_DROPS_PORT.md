# Falling netherite drops — 22 October 2026

Status: build and two isolated native match cycles **QUALIFIED** on `9ea7d9b2`.
The checked JAR is ready locally. Main server, original resource pack and template
remain unchanged.

## Author's landing rule (user)

> ну по задумке блок падает и при падении молнию вызывает. В датапаке таймер это костыль вообще.

The communicated implementation uses physical ground contact and the actual
impact position. This replaces the legacy shared 25-tick schedule, global scan
of falling netherite and approximate Y−4 offset. It is not a separate per-cast
timer. Unrelated falling blocks do not produce this trap's lightning.

## Items and payload

[Source audit](HOME_AND_FALLING_DROPS_SOURCE.md) includes exact acquisition,
native falling defaults and the separate home/mirror source contract. Home is
audit-only in this batch; exposing its dropped token without the working mirror
would be incomplete.

| Native item | Public loot | Presentation |
|---|---|---|
| `skywars2:falling_netherite_drop` | `lbc:ported/falling_netherite_drop` | `{lightning:1,drop:1}`; repeating command block; `lbc:drop_not_trap` model, `lbc:tier_f` tooltip, yellow nonitalic `drop_netherite` name, one white nonitalic `trap_desc` lore row. |
| `skywars2:falling_netherite_drop_old` | `lbc:ported/falling_netherite_drop_old` | `{lightning_old:1,drop:1}`; plain barrier, native name/model, no custom lore or tooltip. |

Both are registered Polymer items, using existing assets. Normal acquisition is
from `barriersnottraps.json`, rather than the ordinary traps table. Whole random
pools and the zero-weight undecorated IMBA placeholder remain gated.

Each activation emits 300 smoke particles at the original fractional token
position +7Y, spread `(0.3,6,0.3)`, speed zero. Normal uses ordinary visibility;
old uses force visibility. The one-cell column from `floor(token)` through +15Y
is cleared with shared strict flags818 for normal and ordinary flags258 for old.
The fill retains its own build-height and 16-cell volume gate. A rejected fill
does not cancel the following independent native summon. Native fill container
suppression and normal neighbour updates remain.

The payload is an exact native `FallingBlockEntity`, default-state netherite,
Time1, zero initial motion and original fractional Y+14. Normal centers X/Z at
floor+0.5; old retains fractional X/Z. The modern codec uses
`BlockState:"minecraft:netherite_block"`, avoiding legacy Name fallback to sand.
Native gravity, drag, landing placement, fallback item drops and expiry remain.
Netherite's default HurtEntities=false is retained: no invented crushing damage.

## Landing, ownership and lifecycle

`FallingNetheriteDropMixin` wraps native ticking only for managed actors. The
native tick decides collision, placement and item fallback. The post-tick
callback accepts on-ground contact even if successful placement has already
discarded the entity. A persistent per-entity landed tag claims the effect once,
including a native moving-piston wait. Airborne expiry/discard is not a landing.

One real vanilla lightning entity appears at the impact, with the token's actual
player thrower as cause. It retains vanilla damage type and hurt cooldown, plus
the already-established WILD lightning rule and protected-role exclusions. It
does not receive the electric-wand managed-lightning tag. Native lightning
ignition/indirect damage reuse existing attribution machinery.

Shaft and native landing writes run through the existing WILD trap scope. This
captures removed support and newly placed suffocating blocks, rather than
crediting every later fall nearby. More recent attackers retain precedence.
Reflection takes precedence over retained causes. Same-match unload UUIDs retain
causes; native lightning's empty save payload is compensated on load by restoring
its retained player cause. Restart persistence is a separate qualification limit.

`LbcSpecialDrops` owns source-order admission and one debit per tick. Fake
visuals, wrong arena, inactive/ending matches, stale epochs and time stop cannot
activate. The shaft's one-block X/Z neighbour halo must already be resident.
No synchronous chunk load, global block scan or scheduled timer is introduced.
Owner death does not cancel emitted actors. Time stop pauses native movement;
match stop quietly discards managed falling blocks and bolts without producing
new lightning, placement or death loot.

## Qualification

`tools/smoke_falling_netherite_drops.py` passed two actual mode-1 fixture matches:
48 suite results, 77.22 seconds including boot, loading, natural flight and all
previous zombie-TNT, corrosion, pit, cage, Creeper, TNT and chest regressions.
Both matches exercised actual stop/epoch cleanup. No final-run suite failed.

The fixtures check exact native/give/loot components, both fractional coordinate
variants, default native payload, the 16-cell shaft and independent limit15
rejection, real native flight beyond 25 ticks without an early bolt, physical
placement and one impact bolt, exclusion of an unrelated falling netherite,
native airborne expiry into an item without a bolt, and no crushing damage flag.
Native lightning checks include lethal disposable-mob credit, self/allied harm,
protected role exclusion, the actual vanilla damage type, and no additional
health loss on the next bolt tick. Fake visuals, one-unit debit, time stop,
unloaded tokens, stale epochs, residency and quiet cleanup also passed.

Ready artifact: `build/verification/skywars2-falling-netherite-drops-9ea7d9b2.jar`.
SHA-256: `9ea7d9b2aef0b4349538f2e13c16ddcf66796d62695756b97d9422b360d61251`.
Evidence: `build/verification/falling-netherite-drops-final/` (`build.log`,
`native.log`, `server.log`, `results.json`, `qualification.json`). All 165 Java
files match the source JAR; 2664 processed resources match the release. CRC,
transformed Fabric metadata and bundled JAR entries passed. The isolated server
stopped normally, released locks and restored its prior mod/configuration/
profile/scoreboard backups. Main JAR remains `69de8601`; not installed there.

Limits: real-client particle packets/graphics and throw packets are unqualified.
The native airborne-expiry item fallback is not a test of using a held artifact.
Same-match unload cause recovery is implemented, but actual chunk-unload,
disconnect and restart persistence have not been qualified. Moving-piston waits,
blocked landing fallback and a dedicated death from landed-block suffocation
remain untested. There is no maximum-concurrency/MSPT claim. The preexisting
Polymer fake-player chunk-packet-context diagnostic remains.

Next bounded migration is the complete sethome/magic-mirror pair from the source
audit, including marker and chunk-ticket lifecycle; no incomplete home token is
exposed here. Whole Lucky Block pools and unfinished modes remain gated.
