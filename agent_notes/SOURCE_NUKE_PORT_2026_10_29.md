# Source bruh nuclear event - 29 October 2026

Staged implementation only. Java is frozen for the parent's compilation and native qualification; **no build/server run was performed here**. Full original Chaos-pool release gate and `root_PENDING` remain in force. Only NEW `LbcNukeEvent.java` and this NEW note were edited. No resources, RP, protected author paths, live overrides or commits were changed.

[Implementation](../src/main/java/me/skywars2/game/LbcNukeEvent.java) is a native event service; root owns Items lifecycle, food caller, shared damage lineage and engine hooks. It does not execute datapack functions/commands, publish fake creepers, substitute a smaller blast or use the Exasperato32 layout.

## Exact source boundary

The Oreshek Zelensky branch creates a marker carrying `nuke_bruh,rc_1sec,marker_2t,mecha,zelensky`, a source piglin and four Hohlopitek drones. The named-food owner handles piglin/drones; this service owns the nuclear marker and complete nuclear consequences. Both source namespaces were searched: **no lbcr function participates in the bruh nuclear graph**. The source and parent/helper files are linked with hashes below; unrelated normal/old/eoe nuclear variants are not silently merged into this one.

* `tick2t -> markers_2t -> markers_2t1` adds Lifetime2 and dispatches `nuke_bruh` only while `!nuke1`; both Nexus and Imprinted are excluded by the tick2t parent.
* `nuke_bruh` sends title `" "`, subtitle red/italic=false nearest nonspectator-mode player's display name (or exact source literal `Володимир Зеленський ` for Zelensky), followed by original dark-red `nuke_lol_activated` translation. It adds nuke1; emits the20 warning sounds below; **unless any alive entity tagged `stooper` is within10**, schedules nuke1_bruh for35s. `stooper` is the actual spelling in source and is distinct from the real `stopper` jammer tag.
* `schedule function ...35s` defaults to **replace**, not append. There is ONE pending global deadline; later qualifying marker activations replace it. It is not one private700tick timer per marker. A marker blocked by stooper still gains nuke1 and issues the warning, but does not enqueue/retry scheduling automatically.
* `tick1s -> raycasters_1s` adds Lifetime20, excludes Nexus/Imprinted at the parent, and invokes nuket_2 unless stooper is within10. Thus the marker receives BOTH +2/2t and +20/20t, not a wall-clock-only lifetime.
* At the global deadline `nuke1_bruh` selects every `@e[tag=nuke1,tag=nuke_bruh]`. For each marker, `nuke1_at_bruh` selects **random16 alive entities with only tag=!spectator**, runs the target payload at their positions/rotations, stops authored music, creates a slowparticlesexplode_2 marker with lifetime400, and kills local eligible nonplayers at the original marker too.
* Each target payload places the13 templates, gives Wither to the radius45 selector, creates24 fuse1 radius16 creeper-equivalent native MOB explosions and performs radius14 cleanup. The original creepers were an explosion carrier; the port creates **zero creepers and zero published blast helper entities**.

Native26.3 selector code was inspected: an @e selector without distance/coordinates is **not worldLimited**. Both the armed-marker selection and the random16 target selection span **all server levels**, and execute-at snapshots each entity's level, position and yaw. Nearest @p is also evaluated by the native selector. The service uses actual `EntitySelectorParser`/`findEntities`; RANDOM uses vanilla `Collections.shuffle`, not a new weighted/random algorithm. Distance14/45 cleanup/effects are local to each captured target level. Sealed-dimension input exclusion does not silently remove already captured target contexts in other levels.

## Warning / stopsound payload

Every warning uses original volume.5/minVolume.5, all players selector, native audible-dimension/distance behavior via the existing LbcOverload sound helper:

| Sound | Pitch order |
|---|---|
| minecraft:lbcsounds.nuke | 1, .5, 1.5, 2 |
| minecraft:lbcsounds.ponos | 1, 1.5, 2, .5 |
| minecraft:lbcsounds.backrooms | 1, 2, 0 |
| minecraft:lbcsounds.burning_balance | 1, 2, 0 |
| minecraft:lbcsounds.penis_bossfight | 1, 2, 0 |
| minecraft:lbcsounds.kazahstan | 1, 2, 0 |

Order after ponos is all four music names at pitch1, then all four at2, then all four at0. Global stop packets use MASTER: nuke **twice**, then ponos/backrooms/burning_balance/penis_bossfight/kazahstan once each. Title animation timings are not overwritten; source only writes title/subtitle. The original named death presentation is `death.attack.explosion.player` with `killed_by_nuke`, independently of causal credit.

## Beacon and exact terrain

`nuket_2` performs the following native STRICT fills, each relative to the marker's floored command position, in order. No item drops or generic placement wrapper are substituted for strict block writes.

| Y | X/Z extent | Block |
|---|---|---|
| 0 | -1..1 | netherite_block |
| -1 | -2..2 | diamond_block |
| -2 | -3..3 | emerald_block |
| -3 | -4..4 | gold_block |
| -4 | -5..5 | iron_block |

Only when resulting Lifetime is1..20 does source clear its column Y+3 through **absolute319** using strict air fill. Then the beacon atY+1 and random pane atY+2 use ordinary native setblock semantics, including neighbor updates. The uniform16 pane order is white, red, black, blue, light_blue, magenta, purple, yellow, pink, brown, orange, green, lime, cyan, gray, light_gray. The actual level's native random draws one nextInt16 at that operation; source random-command1..16 and this zero-based selection have equal boundaries.

Each selected blast target places original `lbc:air` at local forward radius minus world24 on all axes:

| Radius | Relative yaw list | Native integrity |
|---|---|---|
| 25 | 0,60,120,180,240,300 | .5f |
| 60 | 0,30,90,150,210,270,330 | .25f |

The radius60 zero direction is present in addition to the30/90/... ring. Target yaw is captured from the actual entity selected by execute-at. Native BlockPos.containing floor semantics are used for the final place coordinates, including negatives. There is no player-relative teleport, fake air-pattern generation or replacement with Exasperato's terrain.

Actual local mapped PlaceCommand26.3 proves `place template ... none none integrity` defaults **strict=false**, `knownShape=false`, block flags2, BlockRotProcessor(integrity), a processor `StructureBlockEntity.createRandom(0)` and a separately created placement random. The service preserves these native settings and distinct streams while processing prepared template slices through the existing Stonekivi Work budget. The default unseeded template randomness is not the player's/world's food/loot random. The complete invocation's native chunk bounds are checked before any slice writes. Slicing necessarily spreads work over native ticks; no identical world-time/seed-stream/pixel claim is made.

## Wither, bursts, kill and reward consequences

Each target gives actual native Wither duration400ticks/amplifier30/hidden particles to alive local entities within45 except tag spectator. It uses approved ItemEffectLevels and the canonical WILD status attribution ledger, so later real attackers may supersede the cause normally. This does not create a private effect clock or bypass shared native effect policy.

The24 native blast points are the exact source order: radius39 cardinal (+X,-X,+Z,-Z) atY0,+40,-40, then radius18 same cardinal/Y order. Each blast uses native radius16, fire=false, `Level.ExplosionInteraction.MOB` and the existing PLAYER_EXPLOSION damage type. Native mob-griefing, exposure, block rays, entity knockback, native cooldown and damage modifiers remain in the native explosion engine. No TNT-protection tag or smaller fake blast is added. Source creepers' living-nonplayer difficulty scaling is retained explicitly although causal author is a player.

The source fuse1 carriers defer blasts until after the synchronous target cleanup. The bounded native queue keeps all target template/Wither/kill phases and original-marker finish phases before its pending burst phase; at least one actual server tick is required. Each operation remains resumable under Time Stop and subject to the shared CPU budget. This is deliberate native queued-work timing, not a synthetic test clock or a claim that the enormous multi-center event completes in one tick.

Radius14 cleanup excludes actual creeper type, actual player type, slowparticlesexplode_2 tags and strict_map_object tags. Ordinary nonliving entities receive native kill; living targets use the canonical inherited generic-kill path with the accepted WILD cause so source death drops/hooks execute and project-required causal credit is preserved. There is **no explicit loot-table reward** in the bruh nuclear source itself: its reward chain is normal entity death/drop hooks plus native explosion support/fire/TNT consequences. Root's existing actor/boss/item consumers remain responsible for their real death outputs; this service does not mint a substitute reward.

The source marker/mushroom tail carries magic and lifetime400. An actual native marker preserves its selector/lifecycle role, Time Stop lifetime and MAG deletion; the already approved shared NuclearVisuals per-client shader supplies the mushroom presentation rather than legacy hundreds of block-marker packets per tick. The shared shader's own120tick growth and fade-slot contract is retained, not duplicated. Physical explosion/terrain work and the mechanical core are not MAG-suppressed merely because the visual tail is magical.

## Cause, common policy and lifecycle

Every accepted event captures UUID/order/epoch in the canonical WILD cause. It includes owner and allies while retaining current protected-role/absolute-Greed guards at native damage admission. The unpublished direct `BlastSource extends Marker` owns an immutable cause, exact source position and name, is discarded in finally, and is never inserted into a growing service source map. Captured/deferred DamageSource records can still resolve that cause after discard. Root delegates localized death message and difficulty scaling through CapturedDamage so those source semantics are not erased by attribution wrapping.

The native explosion runs inside `indirectKills.trapChanges(level,cause,...)`, preserving real block-removal/support/fire ownership. Root wires managed/WILD source admission, causal lookup, entityLoaded-before-loaded `inheritChild`, TNT propagation and ZombieTnt conversion through this service's narrow lineage API. `inherit(parent,child)` and current-explosion `inheritChild(child)` retain cause/epoch for actual native children, without publishing a helper creeper or claiming that a raw tag alone reproduces their engine.

Core markers have mecha, so the existing real TECH pulse destroys them by source marker policy. Source stopper_marker_destroy only special-cleans ordinary tag=nuke, not nuke_bruh; this service does not invent removal of the five resource layers. The misspelled stooper radius10 enqueue/beacon condition is preserved separately. Neither stooper nor TECH appearing after accepted global scheduling retroactively cancels a schedule; if all matching loaded markers were removed, that deadline selects none. MAG removes only the magic visual tail through the shared engine.

Loaded owned markers retain activated state/Lifetime/cause through normal same-process chunk unload/reload. Unloaded markers are absent from the deadline selector, as in source. A missed deadline is not replayed on later loading of an already nuke1 marker. Child/visual unload state is held by UUID without retaining unloaded entities/chunks. Unknown old-epoch managed markers are retired. Jobs/deadline/children cancel on stop/epoch; accepted armed consequences deliberately survive causal-author forget/death/disconnect like already armed native explosives. `forget` therefore has no cancellation side effect.

No operation forces chunk loads. Source PlaceCommand fails an unavailable template invocation rather than waiting forever: native Operation checks skip that invocation and continue the job. Fill/setblock and explosion operations also skip missing required neighborhoods under current project no-force-load policy. A permanently unloaded neighbor cannot stall every later center/burst. This native safety boundary is recorded; source natural creepers can themselves be suspended by entity chunk unloading, so cross-chunk live timing equivalence still requires qualification.

## API / wiring and qualification boundary

Public API: constructor(game), prepare/prepared, boolean start(owner,origin,zelensky), tick, loaded/unloaded, forget/stop; static managed/wild; owner/cause/canAffect; executing(level)/currentEmitter, inherit(parent,child)/inheritChild(child). Diagnostics: markers/snapshot with actual marker/author/dimension/position/Lifetime/armed/epoch; markerCount/pendingTicks/jobCount and warning/schedule/detonation/center/blast/beacon counters.

Root places tick **before** the map-world Time Stop early return. Each actor/job uses its own captured level's Time Stop; the one global scheduled deadline pauses with common global Time Stop. Template preparation uses existing MapManager.prepareSpellTemplates and the Stonekivi Work queue; there is no new scheduler thread reading mutable world state. `prepared()` describes template availability only; it is not catalog readiness or a release approval.

Required remaining proof belongs to the frozen candidate: compile/mapping checks, exact real warning packets and20sounds, duplicate/global schedule replacement boundaries, no-repeat nuke1 state, native beacon fills/pane16 boundaries, source Lifetime dual cadence and column condition, bounded actual native radius16 hit/attribution/child propagation, cross-dimension native selector contexts, strict/ordinary block behavior, Time Stop/lifecycle/cleanup and neighboring service snapshots. No whole-event performance or full-pool completion claim is made from source inspection alone.

## Recorded ambiguities / deliberate common-policy choices

1. Source independently scheduled tick2t and tick1s can coincide with different global phase ordering; both increment Lifetime. Implementation uses existing server-global parity/second boundaries, **2tick branch before1second branch when coincident**. This can make the source1..20 column clear skip on the first second because Lifetime is already22+. The exact source condition remains; no first-pulse workaround is invented. Parent may align a known original phase if runtime evidence establishes one.
2. Source schedule itself is a global wall/game-time event, whereas tick_not_timestopped gates the slow visual. Parent explicitly requested common global Time Stop pause by default; implementation pauses that global remaining700tick timer and each level's active work. This is the adopted integration rule pending any explicit author alternative, not an invented MAG/TECH gate.
3. Prepared slices preserve native integrity/settings/operation ordering but distribute an originally synchronous command over ticks and current resident chunks. Random16 is selected lazily at each marker payload stage, after prior marker cleanup, using the real native selector. Native positions/yaw already selected for a fork are immutable even if earlier cleanup kills those entities. Unloaded-neighbor handling above is explicit; full instantaneous source timing is not claimed.

## Exact source files and SHA-256

- [other/eatables/oreshek_zelensky](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/eatables/oreshek_zelensky.mcfunction) - `bb42f294f86da69dc83266d08d906614b9fbfa2e742f97c254287c90ce47ea92`
- [other/nuke/nuke_bruh](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nuke/nuke_bruh.mcfunction) - `b240ecacf05f519280e224de314e4ec92df41ef7398586e766a395594c26c5b7`
- [other/nuke/nuke1_bruh](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nuke/nuke1_bruh.mcfunction) - `4b78243994858b0192eff7f41a7e9e064cd62add7998f98224a60fb5d48f8ead`
- [other/nuke/nuke1_at_bruh](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nuke/nuke1_at_bruh.mcfunction) - `13089a1152fe0cfad256b608f26da5179ac4189b7ec6a0dbdf9ff785c34fdcd2`
- [other/nuke/nuke1_at_bruh_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nuke/nuke1_at_bruh_2.mcfunction) - `3b308c9c081bfc9f62c042de4133bd46373de4b9f0f91ed6150997af680dfd6c`
- [other/nuke/nuket_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/nuke/nuket_2.mcfunction) - `d563ce3feb61e29e80e6b961b19916fc367efe9fa1c9c2e26b317c21119fc488`
- [tick2t](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/tick2t.mcfunction) - `fcc4b8f246e9e03b4d1f0d12c011c29c791a64cb2960fe8f08098e9e164de2a0`
- [markers_2t](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/markers_2t.mcfunction) - `79d0c58a79ac45a7fada964cf5cef041097ed268e3b0d34e4e4cd8c01122d0bd`
- [markers_2t1](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/markers_2t1.mcfunction) - `95d0bd6662e731c5a25a3a570f1d53b2d3cfe4c325db348cdb2732abb062dda9`
- [tick1s](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/tick1s.mcfunction) - `da799ec32ca58bb89f77cd22e84e85ecae6e9eebccce5e032851aa9bc799d1b4`
- [raycasters_1s](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/raycasters_1s.mcfunction) - `7603dd59cacb666446d768ab3fb64c4881a1d5bd22f9d4248706633ab4898c1b`
- [tick_not_timestopped](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/tick_not_timestopped.mcfunction) - `55faddfdca07e3a486d0f826e9593969ae5fa05260cb2392c162569efa8cf5cc`
- [raycastselector](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/raycastselector.mcfunction) - `7b4951e7c1ac5aae5fd606e2d8ea53db0bbd8f9125fd77cd1ff4b0836f0c62b6`
- [raycastselector_player](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/raycastselector_player.mcfunction) - `288deedb5f8835078612af498ecbe8cf9814784274d853511758fe0ce0e46094`
- [workingitems/exopersoexplodeparticles_2](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/workingitems/exopersoexplodeparticles_2.mcfunction) - `2ca1c7754a4ada96e9cf0dd7e19f8f48d177713cf87a12b139704f8d52e0f14f`
- [other/stopper](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/stopper.mcfunction) - `ea29adb062ea86506293dfe2ea546e72c2c9d2bd14d7b4823981d289eacd8066`
- [other/stopper_marker_destroy](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/stopper_marker_destroy.mcfunction) - `338640c80789fa6dc160d71f9681ce5454465b8c76b7d5683f2f2510271acaec`
- [other/stopper_magic](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/stopper_magic.mcfunction) - `bb0718cecabf2ba51601cb2d8f660b2b44811b735a210a1afd8ecb942dec9342`
- [other/stopper_magic_destroy](E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/LBC/data/lbc/function/other/stopper_magic_destroy.mcfunction) - `89e2e98d87fcec4b489a6991e24a844df75f67b57a1f4107d9637e064d2efdba`

Original bruh resource/presentation references: existing `lbc:air` template, author `nuke_lol_activated`/`killed_by_nuke` translations and lbcsounds. No new key, sound asset, model or RP edit was introduced.

## Added bounded native suite (frozen, not yet run)

[NukeEventChecks.java](../src/main/java/me/skywars2/game/NukeEventChecks.java) exposes run/tick/cancel for the root `nuke-event` command. It starts real source markers and waits for natural server-global2/20tick callbacks; checks exact title/20sound sequence, native five-layer beacon/block entity and pane, unchanged neighboring highY frame through319, eight actual ticks of Time Stop with frozen pending/Lifetime/beacon counters, natural deadline resumption, a second Zelensky activation replacing the same global timer with700, and explicit stop plus natural cancelled-work drain. Cleanup restores exact terrain/block-entity snapshots, the original shared queue and scoped player/advancement/profile/attribution state; reflection restores diagnostic counters only. It never rewrites production tick/Lifetime/pending clocks, invokes detonation or waits out700ticks. The suite explicitly makes **no full deadline, detonation, epoch-invalidation or event-performance qualification claim**. Parent owns compilation/execution and results.
