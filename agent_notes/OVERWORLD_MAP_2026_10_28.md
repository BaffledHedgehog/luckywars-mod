> Follow-up (28 October 2026): [Void terrain cleanup and progressive chunk publication](OVERWORLD_VOID_AND_STREAMING_2026_10_28.md) supersedes the existing-terrain policy below. The actual generator already uses the datapack void stem even while saved world_gen_settings names noise. Old natural terrain/unfinished chunks have now been removed under the author's disposable-world permission; do not restore them or interpret saved preset metadata as the active generator.

# Actual Overworld map world — 28 October 2026

## Author requirements (user)

> ещё - пусть измерение карты останется overworld вместо arena (поменяй это везде где ты так сделал), это надо для совместимости с миром легаси-сервера

> какие журналы? они для чего?
> кстати - есть ли вариант как то запечь структуры карт в .jar так чтобы ускорить их загрузку/механику с восстановлением?

> и зачем сохранять старые файлы arena? если что мир не важен и почистить все можно без проблем

## Runtime and resources

MapManager now uses the exact server.overworld() instance, not a logical alias
for a custom dimension. Lobby, imported maps/spawn markers, respawn anchors,
item admission, block/light/fluid tracking and existing map-level services all
follow that same object. Hedgehog dimension returns now resolve actual native
Overworld directly, without the prior arena alias.

Remove the skywars2:arena LevelStem and obsolete client-flat-reporting mixin.
Package a valid native26.3 minecraft:overworld void-flat stem for new worlds;
retain the native Overworld dimension type. Existing saved generation settings
remain subject to vanilla's native load behavior; this patch does not silently
rewrite an imported world's generator or move legacy chunks. The incompatible
old legacy JSON fragment is replaced only in this mod's source tree.

Active source/test/tool commands no longer target skywars2:arena. Independent
world controls in native checks use Nether/End instead of treating Overworld
as separate from the map. Time Stop still advances the real global clock; its
dimension-local queues/simulation remain paused. Source Nether/End/Nexus/
Imprinted transfers and coordinate scaling stay native.

## Cleanup journals and obsolete files

The journal records owned cleanup bounds, not a complete world copy. It allows
old map/lobby/item terrain to be cleared after map replacement or restart. The
old dimensionless arena journal cannot be replayed against the same coordinates
in another world. A new data/luckywars/overworld-bounds.json uses version2 and
requires dimension minecraft:overworld. Wrong-dimension/invalid files fail
closed. The old loaded-fluid scan is removed: finding existing legacy water
does not establish map ownership. Real block mutations and map writes remain
journaled.

The author explicitly permits discarding the old world. The one-time
tools/cleanup_obsolete_arena_when_stopped.ps1 helper therefore removes only
world/dimensions/skywars2/arena and data/luckywars/arena-bounds.json after the
qualified migration JAR is installed and the main server has stopped normally.
It validates resolved paths/reparse points, holds the real world session lock,
checks the closed listening port and installed SHA, and writes its receipt.
User datapacks/loot overrides and settings remain. It does not copy old arena
regions into Overworld. The script's ValidateOnly path resolves both exact
targets without performing deletion. No running-world removal or forced stop.

## Structure preparation question

Map NBT structures already ship in the JAR. At runtime preloadTemplatesAsync
reads/decodes them and splitTemplate constructs8x8x8native placement slices.
Moving this decode/split stage to build time can reduce preparation work.
It would not eliminate native block placement, lighting, entity/block-entity
publication or client chunk updates. Respawn islands already use pristine
in-memory block/block-entity snapshots rather than rereading JAR templates.
No new baked-map format is implemented or performance gain claimed by this patch.

## Verification

MapWorldChecks validates actual native Overworld identity, persisted journal
dimension/version and player/spawn lifecycle. Existing dimension-sensitive
Expand, nuclear/black-hole visuals, visual reset, TimeStop, lighting, signed
mining/effect checks run in two actual map matches. A separate real two-life
death/ServerboundPERFORM_RESPAWN/elimination scenario validates replacement
player identity/world/spawn, followed by lobby return.

Initial b3c13ce9 completed both map matches and all gameplay checks, but the
runner invoked its extra lives scenario before asynchronous selected-map
preparation completed. The existing canStart guard correctly rejected it.
The harness now waits for Selected map ready before that scenario, without
changing gameplay or rebuilding the candidate. Initial logs/results retained.
The next harness revision waited for automatic preparation, but post-match
inspection deliberately does not prepare a map. It now explicitly requests a
fresh map. A third run passed the actual replacement-player respawn/elimination
checks; its retained third participant meant one elimination did not naturally
end the match, so the harness now explicitly stops that final scenario before
checking lobby/third-player cleanup. Those failed harness logs/results are also
retained. The Java/resource artifact remains exactly the same throughout.

During this task the previous authorized installer completed c33bd703 over
ccac368f after ordinary main shutdown. The migration baseline was explicitly
advanced to that verified receipt/hash; it did not overwrite an unknown JAR.
Final unchanged buildb3c13ce9 passed41recorded results in two actual mode1matches
plus the real remaining-life respawn/elimination scenario (82.95s), with two
scoped third-participant setup/cleanup records. Normal owned-child shutdown and
exact fixture snapshots restored. All542sources,1540classes and3230processed
resources match the artifact; the old dimension JSON and flat mixin class are
absent from the final JAR. All198author dirty paths and29live loot overrides
remain unchanged. The full final harness passes, with no weakened gameplay
assertions and all earlier failures retained as test-runner evidence.

Installer3244 installedb3c13ce9overc33bd703 after verifying normal shutdown/world
unlock; no process was restarted. Cleanup worker21728 then removed the obsolete
arena directory (1,151,792,092bytes) and its journal (68bytes) under the session
lock. Both paths are absent. Receipts: main-install.json and
obsolete-world-cleanup.json in the qualification directory. Datapacks/settings
remain. Retainb3c13ce9current,c33bd703rollback,ccac368ffallback and audit tool;
obsolete784b026barchive is hash/path checked before removal. No RP update needed.
