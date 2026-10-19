# Moonrise 61c1328: entity movement into unloaded chunks

## Finding

This is not only an artifact of teleporting the fixture 100,000 blocks. The fixture is unusually abrupt (one `setPos` directly into an unloaded section), so it does not establish the normal-flight behavior by itself. However, Moonrise 61c1328 runs the same entity-section transition callbacks when a real `setPos` crosses a chunk/section boundary. `GameChecks.checkExasperato` reaches it at `GameChecks.java:5379`; the server log confirms the callback chain: `Entity.setPos -> EntityLookup.EntityCallback.onMove -> entityStatusChange -> Fabric entity unload -> LbcExasperato.unloaded -> charge.discard`.

Moonrise source at exact commit `61c1328dd3ad0d7944bf03fd3f76793446740b62` shows:

- `EntityCallback.onMove` calls `moveEntity`, then `entityStatusChange` for section changes: [EntityLookup.java at commit](https://github.com/Tuinity/Moonrise/blob/61c1328/src/main/java/ca/spottedleaf/moonrise/patches/chunk_system/level/entity/EntityLookup.java#L3692-L3709).
- On accessible-to-inaccessible status downgrade, Moonrise synchronously calls `entityEndLoaded`, then `onTrackingEnd`: [status transition](https://github.com/Tuinity/Moonrise/blob/61c1328/src/main/java/ca/spottedleaf/moonrise/patches/chunk_system/level/entity/EntityLookup.java#L2655-L2684).
- During the status callback it marks `moonrise$isUpdatingSectionStatus`; `canRemoveEntity` returns false then: [status guard](https://github.com/Tuinity/Moonrise/blob/61c1328/src/main/java/ca/spottedleaf/moonrise/patches/chunk_system/level/entity/EntityLookup.java#L2577-L2595), [canRemoveEntity](https://github.com/Tuinity/Moonrise/blob/61c1328/src/main/java/ca/spottedleaf/moonrise/patches/chunk_system/level/entity/EntityLookup.java#L2921-L2935).

The local test log matches that guard: the test's direct teleport causes `LbcExasperato.unloaded` to call `discard` while Moonrise is processing section status updates, and Moonrise refuses removal. The callback already removed the Charge from Exasperato's `charges` set, explaining why the subsequent test expects removal but sees a live entity.

## Real flight assessment

There is a credible real-path risk. `LbcExasperato.step` computes `wall = LbcTerra.wallHit(level, from, to)`, then calls `charge.setPos(end)` before checking whether the wall's block belongs to a loaded chunk (`LbcExasperato.java:452-469`). `LbcTerra.wallHit` intentionally treats unloaded blocks as solid using `hasChunkAt` and a full block clip (`LbcTerra.java:444-448`). If the clip point is exactly the first block coordinate of the unloaded chunk, the `setPos(end)` can move the entity across the boundary; Moonrise can run the same unload callback synchronously and block `discard` just as in the fixture. That would remove it from the plugin's charge set before the normal impact branch notices `!hasChunkAt(wall.getBlockPos())` and suppresses payload creation.

The exact boundary position and visibility transition should be reproduced in a small stepwise runtime check before calling the production path definitively broken: the existing fixture's `x + 100000` jump proves Moonrise callback behavior for a distant inaccessible chunk, not the precise one-block swept collision endpoint. Still, source ordering means the real risk is testable and plausible; it is not safe to dismiss this as solely a teleport-fixture quirk. A focused follow-up should advance a Charge from a loaded chunk toward its adjacent unloaded chunk with the same one-tick `setPos`/`wand.tick` path and inspect `isRemoved`, `wand.count`, and tagged world entities. Conceptually, the robust ordering is to reject/discard an unloaded-wall impact while the entity remains in its loaded section, before assigning the hit point to the entity.

Moonrise artifact metadata identifies `1.2.0+61c1328` for Minecraft 26.3; these source links are pinned to that commit prefix. This result describes the observed test binary and should not be generalized to unrelated Moonrise builds without checking their source.
