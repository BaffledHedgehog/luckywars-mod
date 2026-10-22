# Arena fluid cleanup and lobby weather

Status: QUALIFIED on `f0e11a5f8c7441b6272872f2d37c4b472ff094a6bed5e6aa046be8c0e10a0ff3` (21 October 2026).
Ready archive: `build/verification/skywars2-map-fluids-f0e11a5f.jar`; not installed.
Main server installation and resource pack are untouched.

## Contract and failure causes

The next map replacement must remove water and other arena edits beyond the old
map footprint, including changes made in lobby/loading/countdown. Previously the
mutation hook only recorded PLAYING/ENDING changes, and journaled edits did not
extend the actual `mapBounds` consumed by replacement. Native fluid ticks could
also propagate water while budgeted clear/paste slices were still replacing blocks.
A disabled weather cycle alone preserved rain/thunder left by a previous match.

## Implemented behavior

- [ArenaBlockMixin](../src/main/java/me/skywars2/mixin/ArenaBlockMixin.java)
  records successful arena block changes in every stage. Loader-owned writes are
  excluded through `applyingMapWork` / `placingMapSlice` in
  [MapManager](../src/main/java/me/skywars2/game/MapManager.java), because their
  recovery envelope is already persisted.
- Flushing pending changes extends both the recovery journal and the actual
  replacement bounds. Before lobby/map preparation, recovery checks section
  palettes of full resident arena chunks for any fluid and records entire matching
  section bounds. This recovers older untracked water above, below or outside the
  next map without loading chunks or scanning individual cells. Resident chunks
  are exposed by [ArenaChunkAccess](../src/main/java/me/skywars2/mixin/ArenaChunkAccess.java).
- [TimeStopSchedulesMixin](../src/main/java/me/skywars2/mixin/TimeStopSchedulesMixin.java)
  defers the arena native fluid scheduler only while `busy && !reading`.
  Queued fluid ticks remain queued; block ticks and other dimensions continue.
  Existing asynchronous preparation and the 4 ms clear/paste work budget remain.
- The finished map remains available after a match; lobby overlay behavior remains.
  Recovery bounds become cleanup work when the next map replaces it.
- Entering LOBBY in [GameController](../src/main/java/me/skywars2/game/GameController.java)
  clears native global rain/thunder flags and sets clear/rain/thunder durations to
  `Integer.MAX_VALUE`, once per lobby entry. Match weather abilities remain active.

## Verification and limits

Build and two native matches passed in 56.77 seconds using
`tools/smoke_map_fluids.py` (map 1 -> 35 -> 1). Evidence:
`build/verification/map-fluids-final/`. Native probes cover water, lava,
waterlogged blocks and real scheduled downflow above/below the map; lobby edits;
a sealed fluid source written directly to the chunk; preservation of the finished
map before replacement; exact weather reset and match storms; fluid deferral for
83 and 70 busy ticks and resumed native flow after each load. Fresh loading probes
are required per selection, because the previous probe is rightly covered by the
next cleanup journal. Existing synthetic-player Polymer chunk-packet context
warnings remain; no check failed.

Package CRC, all 2638 included source-resource bytes, Fabric version expansion
and bundled SGUI passed. The isolated server stopped normally and released its
world lock. Main JAR observed SHA-256 `332415d6ffac9c5f7dfb02a07f395849b4aa68d31312b512fb5748ce3e47ffe4`; not modified.
No graphical client test is claimed.

Recovery inspects resident full chunks only. It does not claim to scan all old
unloaded disk regions or discover historic changes outside those chunks. Future
ordinary successful arena writes are covered by the all-stage persistent journal;
direct chunk writers require resident-fluid recovery. Palette recovery is
conservative: a matching section contributes its whole bounds to later cleanup.
