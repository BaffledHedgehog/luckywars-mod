# Regular Terra projectile API review

## Recommendation

Use the shared `LbcTerra` component's ordinary server-side projectile entity for each Terra charge and attach a Polymer Virtual Entity item display to it. This keeps swept block/entity collision, owner, and lifecycle in the real entity system; the `lbc:terra_charge` item model is a single virtual element. Construct the display stack from a vanilla proxy item (for example `Items.STICK`) and set `DataComponents.ITEM_MODEL` to `Identifier.parse("lbc:terra_charge")`. `lbc:terra_charge` is an item-model resource, not a registered item ID. At the requested 200–300 simultaneous projectiles, this avoids a second native display entity per projectile.

The installed dependency is `eu.pb4:polymer-virtual-entity:0.18.2+26.3` (`build.gradle`, `gradle.properties`). Local sources confirm these exact APIs:

- `ItemDisplayElement(ItemStack)` and `setItemDisplayContext(ItemDisplayContext)`; use the vanilla proxy stack with `DataComponents.ITEM_MODEL` set to `lbc:terra_charge`, in `ItemDisplayContext.FIXED`.
- `DisplayElement.setTransformation(Transformation)`, `setTransformation(Matrix4fc)`, `setScale(Vector3fc)`, and `setLeftRotation(Quaternionfc)` / `setRightRotation(Quaternionfc)`.
- `GenericEntityElement.setYaw(float)` and `setPitch(float)` for the virtual display's network rotation.
- `new EntityAttachment(holder, projectile, false)` attaches the holder without automatic ticking. The `ServerEntity.sendChanges` hook calls `attachment.tick()`, but with `false` it does not tick the holder. Call `holder.tick()` explicitly from `LbcTerra`'s `LbcItems.tick()` update: `ElementHolder.tick()` reads `attachment.getPos()`, updates position, and ticks elements. Polymer's tracker `addPairing` / `removePairing` hooks start/stop watchers independently of `Entity.tick()`, so tracking still works for FULL-loaded, non-entity-ticking projectiles. Explicit `holder.destroy()` / attachment cleanup on projectile removal keeps the virtual element lifecycle paired with the projectile.
- `ElementHolder.addElement(T)` and `destroy()` provide element registration and per-watcher removal. `EntityAttachment` takes care of current tracker watchers; do not broadcast a hand-built entity packet or maintain a parallel viewer list.


The old chaos sword is spawned by `world/datapacksdisabled/LBC/data/lbc/function/items/swords/chaos_sword_imba.mcfunction`. It uses `item_display:"fixed"`, `interpolation_duration:1`, `teleport_duration:1`, no shadow, and the 16-float transformation `[0,0,2.5,0, 1.7678,-1.7678,0,0, 1.7678,1.7678,0,0, 0,0,0,3]`. Minecraft 26.3 `Transformation.EXTENDED_CODEC` accepts the `ExtraCodecs.MATRIX4F` form, encoded as 16 row-major floats using `Matrix4fc.getRowColumn(row,column)` / `Matrix4f.setRowColumn(row,column,value)`. Populate a JOML `Matrix4f` row/column-wise with those exact values and call `element.setTransformation(matrix)`; do not decompose/rebuild into a generic yaw/pitch rotation. `EntityAttachment` follows position only, so set the virtual element's `setYaw(float)` / `setPitch(float)` from the projectile's `getYRot()` / `getXRot()` on its movement update to preserve the old `tp ... rotated` orientation.

## Collision and ticking fit

`LbcClouds.AcidProjectile extends ThrowableProjectile implements PolymerEntity` is the nearest working native pattern: it inherits vanilla velocity/gravity/drag and swept impact behavior, overrides `canHitEntity(Entity)` / `onHit(HitResult)`, and is registered with `PolymerEntityUtils.registerType(type)`. For a Terra model display, either retain a Polymer native projectile proxy that the client already sees as an invisible marker, or register the projectile type with an appropriate Polymer fallback; the attached virtual item is the visible model. Do not add the ordinary native `ItemDisplay` as the visual, because its display entity tracker is redundant beside the projectile and it bypasses the desired virtual-element viewer lifecycle.

The item system already has a shared ordinary tick: `LbcItems.tick()` calls `wands.tick()`. `LbcTerra` can own the projectile collection and update active projectiles there, including FULL-loaded entities which Minecraft does not entity-tick. The existing swept-query example is `ProjectileUtil.getManyEntityHitResult(level, owner, from, to, new AABB(from, to).inflate(...), predicate, 0, ClipContext.Block.COLLIDER, true, false)` in `LbcWands.electroHits`; `AcidProjectile` already demonstrates native segment collision for the other wand projectile. Keep filtering (owner/allies, arena damage policy, spectator, permitted mob tag) in the native predicate/`canHitEntity` layer.

## Lifecycle and long flight

- Route the projectile through `LbcItems.entityLoaded(Entity)` / `entityUnloaded(Entity)` into `LbcTerra` as needed. Validate current `skywars2.items.<uuid>` epoch and owner/arena eligibility; removal must synchronously destroy the holder and remove the projectile from the set. `LbcItems.stop()` advances the epoch; clear `LbcTerra` there.
- Respect the existing time-stop state: `LbcWands` already reads scoreboard objective `lbc.math`, participant `time_stopped` in `electroTimeStopped()`, and the owner's `time_stopped` entity tag. A Terra update should skip movement, collision, and display rotation while stopped; it should still permit epoch/owner invalidation cleanup.
- Do not force chunk loads. If the next movement segment enters an unavailable chunk, remove the projectile and its visual immediately. Do not retain it awaiting a later `ENTITY_LOAD`.
- Avoid a fixed TTL and max travel distance for Terra. Use an infrequent, staggered validity scan (e.g. one bucket of projectiles per tick, with a full scan every few seconds) to drop only when no plausible eligible player/mob lies ahead in the traveled direction and the projectile is already receding far from relevant players. Keep native swept collision every active movement tick; the amortized scan is only an abandonment guard, not a replacement for collision or a short timeout.

## Local source anchors

- `LbcWands.java`: `ElectroTrace`, `tick()`, `electroHits(...)`, and its `TIME_STOPPED` scoreboard lookup (existing examples only; Terra's chosen owner is shared `LbcTerra`).
- `LbcClouds.java`: `AcidProjectile`, `registerAcidProjectile()`, `loaded(Entity)`, `unloaded(Entity)`, and `wandAcid(...)`.
- `LbcItems.java`: `entityLoaded`, `entityUnloaded`, `tick`, `stop`, `currentEpoch(Entity)`, and its unique item-epoch tag.
- Polymer source JAR in the local Gradle cache: `eu/pb4/polymer/virtualentity/api/elements/ItemDisplayElement.java`, `DisplayElement.java`, `ElementHolder.java`, `attachment/EntityAttachment.java`, and `mixin/EntityTrackerEntryMixin.java`. Minecraft 26.3 merged/deobfuscated JAR confirms `Transformation.EXTENDED_CODEC` and the row/column matrix codec.

This is a read-only design review; no build or runtime verification was performed.

## Read-only review of `LbcTerra.java`

The manual attachment integration is correct for the chosen component tick: `new EntityAttachment(holder, this, false)` registers the holder, `holder.tick()` explicitly advances its position from the projectile and its display element, and Polymer's tracker `addPairing` / `removePairing` mixin hooks handle viewers independently of native `Entity.tick()`. The `setRow(0..3, Vector4f)` calls preserve the exact 16 values as matrix rows. The item stack correctly uses `Items.STICK` plus `DataComponents.ITEM_MODEL = lbc:terra_charge`.

Block sweep deliberately uses `Shapes.block().clip(...)` for every block
outside the source-derived `minecraft:airs` tag. Flowers, carpet, etc. are
in that tag and pass through. Other partial blocks stop Terra by the full
cell, preserving the original classification rather than changing it to a
physical collision shape. Entity hits use native swept hitboxes.

Native `MatrixUtil.svdDecompose(Matrix4fc, ...)` divides by `m33` (3 in the
source matrix). A reconstructed affine display matrix must be compared with
the source after this homogeneous normalization; raw element equality is
an incorrect test.

The particle loop sends at most seven dust particles per active projectile
per tick, plus one entity query and block traversal per projectile. With
300 shots and 1000 irrelevant candidates, reviews can require about 3000
candidate checks/tick against the 2048 budget. The queue can backlog; this
keeps shots conservatively and does not exceed the per-tick budget. Actual
runtime performance must be measured separately from this source review.
