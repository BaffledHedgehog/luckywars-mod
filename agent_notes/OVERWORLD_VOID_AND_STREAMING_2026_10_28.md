# Void Overworld and progressive map publication ? 28 October 2026

(user) Actual minecraft:overworld must be void, matching the legacy dimension datapack. The existing world is disposable; old generated terrain may be removed. Preserve author loot overrides and settings.

(user) Partial generated structures, blocks/chunks not arriving and map loading lasting minutes are unacceptable. Temporary 18?19 TPS is preferable to a heavily stretched placement queue.

## Source and diagnosis

Legacy: `E:/servers_mew/cringe1_21_4_skywars/world/datapacksdisabled/swarogskywars/data/minecraft/dimension/overworld.json` uses a flat one-air-layer Overworld. The mod now uses that layer shape with native26.3 explicit plains/no-features/no-lakes/empty-structure-overrides fields.

The prior packaged empty-layer stem already selected actual void generation. Native26.3 `WorldDimensions.bake` prefers datapack dimensions over saved dimensions on both existing-world and new-world branches. Saved `world/data/minecraft/world_gen_settings.dat` can still name the normal noise preset while `ServerChunkCache` actually uses the flat datapack stem. Do not infer the active generator from that metadata or install a redundant generator mixin/preset override.

Initial build dfe421e6 passed actual FlatLevelSource/settings and two remote FULL all-air chunk assertions in two native matches plus remaining-life respawn. The current main world's saved regions separately contained 14 nonempty natural structure starts (mineshafts, ocean ruins, portals, shipwrecks, trial chambers), 623 structure-start-only chunks and other incomplete statuses. Their stored generation history predates the new stem; a generator setting does not remove old chunk blocks or pending structure starts.

Main log records map60 taking4183work ticks for24480slices, then success. Main-thread dump was sleeping normally after that completion, so no permanent main-thread deadlock was observed. A whole-operation light/send dependency held initial chunks across the entire clear/paste interval. This explains starvation during loading, without claiming every client symptom was reproduced physically. The longstanding Polymer missing-context warning on Carpet creation is not presented as a newly fixed fault.

## Changes

MapManager now seals each tick's writes into immutable lighting batches. Completed batches publish/release independently while remaining slices continue; at most8 pending batches provide backpressure. Native light propagation and authoritative interior-viewer light snapshots remain. All batch gates release on failure/cancel/shutdown, and map completion waits for outstanding batches. Only map work enters these batches; unrelated Overworld writes cannot restart their finished barriers. Lobby restore/removal keeps its own lighting transactions.

Placement work uses a24ms soft budget,384slices and64-slice warm window instead of4ms/64/4. Light publication shares a3ms budget. No unfinished future is joined and no lighting repair generates terrain. An individual native operation can exceed a soft budget; this is not a peakTPS claim.

## Validation and deployment

Build77354937 passed43recorded results in2actual mode1matches plus a real remaining-life respawn/elimination scenario (131.98s). Exact547sources/1555classes/3232resources match the tested JAR; all198author paths and29live overrides remain byte-identical. Child server stopped normally and exact snapshots, including temporary port settings, were restored. Source knockback audit passed1302processed tables. Native `map-streaming` performs actual60?61?1 loads using registered viewers, normal native ticks and native PlayerChunkSender full-packet capture before placement finishes. `map-world` checks the actual generator and remote FULL chunks rather than saved preset metadata. Existing light, respawn, map world, dimension, visual and TimeStop checks remain.

The isolated default port25576 is occupied by a different local server. Tests use25577 on the owned fixture with exact server.properties snapshot/restore and native session-lock checks. The unrelated process is untouched.

`tools/cleanup_overworld_when_stopped.ps1` targets only main `world/dimensions/minecraft/overworld` and matching `world/data/luckywars/overworld-bounds.json`. It requires qualified installed SHA, normal world unlock, closed server port, exact resolved targets and no reparse points. Root player/settings/datapack data and other dimensions are preserved. No forced stop/restart.

Final map-streaming measurements: map60 completed in469/377real ticks including17193/15348ms template preparation; actual write work123/68ticks. Map61 completed159/124ticks, write work132/103ticks. Previously gated changed chunks received full packets during placement (60:121/121unique chunks;61:107/107); final dependencies were complete. Differing prior map state means this is not a controlled speedup ratio against the live three-minute incident. No physical-client/TPS claim.

Installer18240 installed77354937 overdb830314 while stopped/unlocked; no restart. Cleanup worker16816 removed10,748,813bytes of old Overworld state and its matching journal. Both exact targets are absent; overrides verified unchanged after cleanup. Receipts: `build/verification/overworld-void/main-install.json` and `overworld-cleanup.json`. Preserve current77354937, db830314rollback and31b84a20fallback. Supersededb3c13ce9and initialdfe421e6archive copies can be retired with exact path/SHA checks; their proof/logs remain.
