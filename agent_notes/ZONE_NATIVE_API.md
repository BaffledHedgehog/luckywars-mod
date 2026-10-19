# Native ItemDisplay API audit

Read-only audit of `minecraft-merged-d6f9bb6878/26.3/*sources.jar` (classes `net.minecraft.world.entity.Display` and `Entity`) and the current `MatchEvents`/`MapManager` sources.

## Public API

`Display.ItemDisplay` inherits public final `Display` setters:

- `setItemStack(ItemStack)` and `setItemTransform(ItemDisplayContext)`.
- `setTransformation(Transformation)`; native interpolation controls are `setTransformationInterpolationDuration(int)` and `setTransformationInterpolationDelay(int)`.
- `setBillboardConstraints(Display.BillboardConstraints)`, `setViewRange(float)`, `setWidth(float)`, `setHeight(float)`, and `setGlowColorOverride(int)`.
- `Entity.setGlowingTag(boolean)` is public final and inherited by ItemDisplay. Glow color override only selects the outline color; it does not enable glowing.
- `Entity.shouldBeSaved()` is public, non-final, so subclasses can override it (`LbcTerra.TerraProjectile` already does).

No project helper wraps native ItemDisplay setup or identity transformation. Existing Polymer helper: `LbcTerra.TerraProjectile.attachModel()` creates an `ItemDisplayElement`, sets FIXED context and transformation; its `new Matrix4f()` branch is identity. There is no reusable identity-display factory/helper found.

## Range / tracking distinction

Display `view_range` is a render culling multiplier: native `shouldRenderAtSqrDistance` uses `view_range * 64 * getViewScale()`. Thus default `view_range=1` corresponds to a 64-block base radius (scaled by model size); setting it to 64 would mean a 4096-block base radius, not a 64-block radius. This is separate from server entity tracking: `EntityType.clientTrackingRange()` is type-level and `Display.ItemDisplay` exposes no per-instance tracking-range setter. A native ItemDisplay centered at y=-60 can fail to be tracked by far-away/high players even if its display `view_range` is increased; center-chunk placement also matters. A Polymer virtual display with per-viewer `ElementHolder` watching is the existing alternative pattern to investigate if every arena player must receive it.

## Shrink / late-join notes

Current `MatchEvents.shrinkBorder()` picks a random alive player for world-border center, sets 384, then lerps to 4 using `4000L * TICKS_PER_SECOND` (20 ticks/sec = 80,000 ticks). This is current code; the updated target from the user is 384→4 over 200 seconds (4,000 ticks). `MapManager` has no `center()` API found in its public methods; its current source shows `level()`, `spawns()`, `lobbyPosition()` etc. rather than `maps.center()`.

The native interpolation duration is a signed Java `int`; both 4,000 and 80,000 fit. Client Display computes start as its local `tickCount + transformationInterpolationDelay`, then progress is `clamp((tickCount - start)/duration, 0..1)`. The client initializes its render state on the first entity tick from the latest metadata values, so two transform metadata packets sent before that tick cannot reliably establish an initial pose and then a target pose. For this use, keep Polymer's shared element at authoritative current radius and interpolate only the one-tick change; a newly watching client spawns directly at current radius.

## Polymer Virtual Entity 0.18.2+26.3 API and chosen late-join approach

Jar: `polymer-virtual-entity-0.18.2+26.3-sources.jar`.

- `ManualAttachment(ElementHolder, ServerLevel, Supplier<Vec3>)` attaches a holder at a supplied world position. It intentionally has empty `updateTracking`/`updateCurrentlyTracking`, so viewers are controlled manually; its default attachment tick delegates to `holder.tick()` and must be invoked by the caller. `ElementHolder.startWatching(ServerPlayer)` and `stopWatching(ServerPlayer)` are public. There is no `isWatching`; use `getWatchingPlayers()` connections or maintain the arena roster. `destroy()` stops all viewers.
- For fixed y=-60, `new ManualAttachment(holder, level, () -> new Vec3(center.x, -60, center.z))`, manually start viewers from the arena roster, sync roster every 20 ticks and on player join/arena-entry, and call `holder.tick()` once per server tick. Use `stopWatching` on exit. This bypasses native ItemDisplay chunk tracking while retaining the real client `item_display` entity, shader/glow rendering, and normal Polymer packet encoding.
- `ItemDisplayElement` inherits `setTransformation(Transformation|Matrix4fc|Matrix4x3fc)`, `setScale(Vector3fc)`, `setInterpolationDuration(int)`, `setStartInterpolation(int)` / `startInterpolation()`, `setViewRange(float)`, `setDisplayWidth(float)`, `setDisplayHeight(float)`, `setGlowColorOverride(int)`. `GenericEntityElement.setGlowing(boolean)` is public final. Polymer names the start-delta setter `setStartInterpolation`; there is no `setInterpolationDelay` method. `DisplayEntityData` accessors and `getSyncedData()` are public; `sendChangedTrackerEntries` is protected/overridable.
- Client `Display.tick()` initializes its render state on a client tick, only after metadata updates set the update flags. Two transform metadata packets bundled before the next client entity tick do not provide a reliable two-stage initial state: the tick sees the last values and may initialize directly at target. Avoid the earlier proposal to send current+target packets from `startWatchingExtraPackets` in one bundle.

For the target 200-second (4,000-tick) radius effect, keep the holder's global display scale equal to the current radius and update `setScale(currentRadius)` each game tick with interpolation duration 1. This creates one small scale metadata update per tick for all current watchers. A late joiner's initial spawn metadata already contains current radius, so that client initializes at the present state; it does not replay the full effect or require personalized baseline packets. The same display follows center and radius from authoritative match state; no per-viewer transformation packet engine is needed.

## Direct world-border HP damage and death event path

The new desired effect is radius 384→4 over 200 seconds, with direct HP loss once per second of `dXZ - R`, bypassing defenses and applying no velocity/knockback. This is a target requirement; existing `MatchEvents.shrinkBorder()` still invokes vanilla world-border damage through its 4,000-second lerp.

Native source confirms `LivingEntity.setHealth(float)` only clamps/writes health. Normal health damage reaches `CombatTracker.recordDamage(source, damage)` inside `actuallyHurt`, after armor/magic/absorption mitigation; it also runs damage hooks/responsible-attacker bookkeeping. So direct `setHealth(getHealth()-amount)` bypasses those systems, including knockback and damage callbacks. If lethal, explicitly calling `ServerPlayer.die(outOfBorderSource)` runs the normal player death body; Fabric's `ServerLivingEntityEvents.AFTER_DEATH` is injected on that death method, and the current `SkyWars2` handler will therefore run. The event is post-death and cannot cancel it.

`die(source)` reads the death message from `CombatTracker.getDeathMessage()` and uses kill-credit state accumulated by normal hurt handling; passing `source` alone does not record it. With no recorded entries the message is generic; with previous combat entries it can select the latest prior entry. If the border cause should appear accurately in death messages, call `player.getCombatTracker().recordDamage(outOfBorderSource, amount)` before direct health subtraction/death. This records the source/damage without applying mitigation or velocity, but does not create a new attacker/kill credit because the border source has no entity; recent prior attacker credit can remain under vanilla memory rules. Direct health subtraction also bypasses ALLOW_DAMAGE/AFTER_DAMAGE. Confirm any required non-death game damage callbacks separately; `AFTER_DEATH` is covered by the direct `die` invocation.


