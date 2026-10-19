# LbcMagic review (read-only)

Reviewed current `LbcMagic.java` plus `LbcInventoryTriggerMixin`, `ArenaBlockMixin`, `LbcItems`, and `SkyWars2` lifecycle wiring. No Java edits.

## Concrete defect

- **A restored rod does not cancel a pending break.** `blockChanged` adds a beacon to `broken` when its tracked rod changes to a non-rod (`LbcMagic.java:226–231`), but a later rod placement at the same coordinate does not remove it; `tick` drains the set and `removeBroken` never rechecks the final block (`:255, :311–322`). If the rod is removed and restored before the next magic tick, the beacon is still deleted and the restored rod is orphaned. The old datapack checks the marker's current block on its next 2-tick pass. Revalidate the tracked coordinate before payout/removal or clear the queued break when it becomes a rod again.

## Lifecycle correction

После review исправлена очередь разрушения: финальное состояние rod повторно
проверяется, отменённое разрушение сохраняет маяк. Это проверено серверным
сценарием. Проверены также активный NBT reload и снимки старого матча для
активного и выгруженного неактивного marker. Результаты — [SERVER_TESTS.md](../SERVER_TESTS.md).

An unloaded marker reference alone does not cause a stale beacon to survive a match. `LbcItems.entityLoaded` rejects old `skywars2.items.<UUID>` tags before calling `magic.loaded`; `magic.loaded` stamps the current epoch; `LbcItems.stop` rotates the epoch after `magic.stop`. Thus a previously loaded and saved beacon marker (including an unactivated one) is discarded on its next entity-load callback. No separate session tombstone is needed. Normal chunk unload/reload unindexes and reindexes the same UUID correctly.

## Checks that passed

- Inventory recomputation checks slots `0..35` and offhand `40`, excludes armor/body armor, and coalesces events by UUID. Load/join/respawn wiring covers initialization; no all-player poll is used for normal refresh.
- `stop()` clears indexes/queues and player tags; stale marker entities are rejected by the epoch check before the magic provider adopts them.
- The 31³ block scan is budgeted at 2,048 block checks per game tick, reads only already loaded chunks, and rod break detection itself is exact-position indexed.
- `removeBroken` consumes at most one lightning-rod item within 1.5 blocks of the tracked rod and creates the beacon reward only if such a drop exists. This is bounded and avoids consuming a distant unrelated rod; it follows the old function's dependence on finding a dropped rod.
- Old-source correction confirmed: `stopperbreaked_magic.mcfunction` invokes the dropped-rod handler and then `kill @s` on the marker. `stopper_magic_destroy` kills each affected target only.
