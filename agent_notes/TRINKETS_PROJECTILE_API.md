# Trinket projectile and volley API audit (Minecraft 26.3)

Read-only audit for choosing a native hook for trinket projectiles. No Java sources were changed. The Fabric API sources jar is not present in the local cache; event names and callback argument types are verified against the project registration and the locally available Fabric API 0.161.0+26.3 binary. The Minecraft source archive is `../.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-d6f9bb6878/26.3/minecraft-merged-d6f9bb6878-26.3-sources.jar`; source paths below are entries inside that archive unless they point to the mod source tree.

## Bow/crossbow volley hook recommendation

For proc counting, wrap the one `Projectile.spawnProjectile` invocation inside `ProjectileWeaponItem.shoot`, accumulate whether any added entity is present in the supplied `ServerLevel`, then dispatch once at `shoot` RETURN. Do not dispatch from `ENTITY_LOAD`: it reports per projectile (and again for chunk loads), while the desired mechanic is once per successful volley. Keep `ENTITY_LOAD/UNLOAD` for registration, epoch rejection, lifetime tracking, and cleanup.

Mojang-mapped `ProjectileWeaponItem.shoot` is protected and has this exact source signature:

```java
protected void shoot(ServerLevel level, LivingEntity shooter, InteractionHand hand,
    ItemStack weapon, List<ItemStack> projectiles, float power, float uncertainty,
    boolean isCrit, @Nullable LivingEntity targetOverride)
```

The local compiled Minecraft jar confirms the erased descriptor:

```text
(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;
 Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/item/ItemStack;
 Ljava/util/List;FFZLnet/minecraft/world/entity/LivingEntity;)V
```

In that method (`world/item/ProjectileWeaponItem.java`, lines 43-77), each nonempty ammo stack calls the four-argument static overload:

```text
Projectile.spawnProjectile(Projectile, ServerLevel, ItemStack, Consumer)
```

Its verified erased target is:

```text
Lnet/minecraft/world/entity/projectile/Projectile;spawnProjectile(
Lnet/minecraft/world/entity/projectile/Projectile;
Lnet/minecraft/server/level/ServerLevel;
Lnet/minecraft/world/item/ItemStack;
Ljava/util/function/Consumer;)
Lnet/minecraft/world/entity/projectile/Projectile;
```

The source calls the shoot consumer, calls `serverLevel.addFreshEntity(projectile)`, applies spawn enchantments, and returns that same projectile (`world/entity/projectile/Projectile.java`, lines 265-280). The add result is not returned, so in the `@WrapOperation` handler call the original operation, then set a shared `LocalBooleanRef` true iff `serverLevel.getEntity(result.getUUID()) == result`. Initialize the shared flag to false at `shoot` HEAD; preserve true once set so it means at least one successful add. An `@Inject(at = @At("RETURN"))` on `shoot` reads that shared flag and runs the public instance method through `SkyWars2.game().items().trinkets().shot(shooter)` once only when true. `LbcItems` constructs one `LbcTrinkets` per `GameController` and exposes `trinkets()` (`LbcItems.java`, lines 129 and 368–370); the callback should remain match-owned and should receive the `ServerPlayer` plus any weapon context it needs. Guard the callback to BowItem/CrossbowItem users, server players, active participants in the arena, and `!game.isObserver(player)`; the observer requirement is explicit in `agent_notes/AGENTS.md`.

MixinExtras is locally available and on the offline-resolved main compile classpath as `io.github.llamalad7:mixinextras-fabric:0.5.5`; the local server also contains processed `mixinextras-0.5.5`. Verified API packages are `com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation`, `...injector.wrapoperation.Operation`, `com.llamalad7.mixinextras.sugar.Share`, and `com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef`. `Operation<R>.call(Object...)`, `LocalBooleanRef.get()/set(boolean)`, and `@Share` are present in the local 0.5.5 jar. This mechanism provides one callback for a multishot volley even when several calls inside the loop successfully add projectiles, and none if all adds fail.

Vanilla bow `releaseUsing(...)` calls the list-based shoot once and plays the bow release sound after it (`BowItem.java`, lines 25-58). Crossbow calls the same base `shoot(...)` from `performShooting(...)` once per release (`CrossbowItem.java`, lines 169-189); its shot sound is played inside `shootProjectile(...)` for each projectile (lines 106-134), while its `SHOT_CROSSBOW` criterion is triggered once after `shoot(...)` returns (lines 178-186). The RETURN callback therefore has the desired volley granularity and does not use sound count. A mixin into `shootFromRotation` is unsuitable because Bow calls it but Crossbow calls `Projectile.shoot(...)` instead. Do not confuse `ProjectileWeaponItem.shoot` with custom trinket spawns: Blaze SmallFireball and Ghast FireworkRocket creation do not automatically use this method. Keep their item-specific launch behavior in their own trigger code; use the volley hook only for Bow/Crossbow release-driven procs.

## Existing lifecycle and persistence to reuse

`LbcItems.entityLoaded(Entity)` already rejects stale epoch-tagged entities first, routes recognized entities into `LbcClouds.loaded`, validates arena/owner, and records tracked TNT, snowballs, markers, or items (`src/main/java/me/skywars2/game/LbcItems.java`, around lines 388–424). `entityUnloaded(Entity)` removes references from all tracking collections and delegates to `clouds.unloaded` (around lines 426–431). `LbcItems.tick()` calls the cloud tracker and item-specific tracked work once per server tick (around lines 605–606); `stop()` discards tracked entities, clears collections, and rotates `entityEpochTag` (around lines 651–666). `SkyWars2` already owns the global entity load/unload event wiring, so reuse it for tracking; the Bow/Crossbow proc still needs the separate volley mixin described above.

The current epoch scheme is persistent: `entityEpochTag` is `skywars2.items.` plus a UUID; `hasStaleItemEpoch(Entity)` identifies a prior `skywars2.items.*` tag, and `markCurrentItemEpoch(Entity)` adds the current one (`LbcItems.java`, around lines 130, 469–476). Minecraft `Entity` serializes its entity tags under `Tags` and reads them back (`world/entity/Entity.java`, lines 2157 and 2228). For new match-owned projectiles, mark them with the current epoch before tracking. On a later chunk load, a prior-match projectile is discarded before its type-specific handler runs.

For one-off expiration, reuse the `LbcItems` tracked-entity set plus a tick counter/state and process only that set; snapshot it before effects or discard can synchronously trigger unload callbacks, as `tickDynamites()` does. For persistent age across chunk unload/reload, follow `LbcClouds.ACID_AGE`: a persistent Fabric `AttachmentType<Integer>` and `AcidState` seeded from `getAttachedOrCreate(...)`; `LbcClouds.loaded/unloaded/tick/stop` is the closest in-project lifecycle pattern (`src/main/java/me/skywars2/game/LbcClouds.java`, around lines 59–61, 119–130, 143–188, 348). An age attachment handles reload survival; the LbcItems epoch tag handles match invalidation. Do not rely on an in-memory counter alone if unloaded entities must retain their remaining lifetime.

## Native projectile constructors and behavior

Packages use the 26.3 split names:

- `net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball`: `SmallFireball(Level level, LivingEntity mob, Vec3 direction)` calls the owner-aware `AbstractHurtingProjectile` constructor. The position/direction overload `SmallFireball(Level, double x, double y, double z, Vec3 direction)` has no owner; call `setOwner(owner)` if using it. On entity hit it ignites for 5 seconds and attempts 5 damage; on block hit it can place fire; server-side hit discards it (`SmallFireball.java`, lines 20–70).
- `net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball`: `LargeFireball(Level level, LivingEntity mob, Vec3 direction, int explosionPower)` sets owner through the superclass and sets explosion power; default power is 1. It explodes on hit with `Level.ExplosionInteraction.MOB`, using the `MOB_GRIEFING` gamerule, and discards (`LargeFireball.java`, lines 18–38). It has no position/direction convenience overload in this source; the generic `(EntityType<? extends LargeFireball>, Level)` constructor is for entity loading/creation and does not set owner or direction.
- Both inherit `AbstractHurtingProjectile`: initial directional movement is normalized direction × acceleration power (default `0.1`); each tick adds normalized acceleration and applies `0.95` air inertia or `0.8` liquid inertia. It performs swept movement collision and deflection through the normal Projectile path; no gravity is applied by this class (`AbstractHurtingProjectile.java`, lines 21–49, 71–120, 149–155). Thus fireballs use acceleration rather than arrow-like gravity and drag.
- `net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball`: `Snowball(Level, LivingEntity mob, ItemStack itemStack)` uses the owner's eye position and sets the owner (`ThrowableItemProjectile.java`, lines 25–37). `Snowball(Level, double x, double y, double z, ItemStack)` is ownerless. The standard thrown-projectile tick applies gravity, inertia, swept collision and rotation; defaults are gravity `0.03`, air drag `0.99`, water inertia `0.8` (`ThrowableProjectile.java`, lines 43–103, 86–89). On hit, vanilla emits the snowball particle event and discards (`Snowball.java`, lines 54–69).

`Projectile.setOwner(Entity)` is public and `getOwner()` is nullable (`world/entity/projectile/Projectile.java`, lines 85–96). Prefer the owner-taking constructor when available: ownership is needed for damage sources and collision filtering. Vanilla class constructors do not by themselves add the entity to the level; add with `ServerLevel.addFreshEntity(...)` (or the native `Projectile.spawnProjectile(...)` helper where its item-stack/enchantment semantics fit), and only begin tracking after the load callback sees the successful add.

The clarified candidates are Blaze SmallFireball and Ghast FireworkRocket; Snow Gem freeze is a nonprojectile effect and stays deferred.

## Ghast firework construction and lifetime

The native class is `net.minecraft.world.entity.projectile.FireworkRocketEntity`. Its public constructor `FireworkRocketEntity(Level, ItemStack, Entity owner, double x, double y, double z, boolean shotAtAngle)` sets the owner and angle flag (source lines 88-93); the source item's `DataComponents.FIREWORKS` should hold exactly five intended explosions for Ghast. Its `lifetime` field is private; there is no public lifetime setter in the local source/API. Constructor-derived lifetime is normally `10 * flightCount + random(0..11)` (lines 57-70), so do not expect the boolean constructor or `setDeltaMovement` to set Lifetime.

To force exactly 100 while keeping vanilla serialization and movement, construct the native rocket with its owner/item/position and `shotAtAngle=true`. Create `TagValueOutput` with `TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess())`, call `rocket.saveWithoutId(output)`, set `var tag = output.buildResult(); tag.putInt("LifeTime", 100);`, and call public `rocket.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag))`. `FireworkRocketEntity` reads `Life`, `LifeTime`, `FireworksItem`, and `ShotAtAngle` in `readAdditionalSaveData` (lines 278-293). Set its required Motion using `setDeltaMovement` with the source-prescribed per-axis `floor(direction * 10000) * 0.0002` after loading, then call `ServerLevel.addFreshEntity`. Existing code exercises the exact save/load API with `TagValueOutput`, `saveWithoutId`, `TagValueInput.create`, and `Entity.load` in `GameChecks.java:1257-1264`; there is no need to add a mixin accessor for `lifetime`.

## Observer and lifecycle safeguards

The repository's observer rule says UUID role is authoritative: new items, targets, and game events must check `game.isObserver(player)`; gamemode alone is insufficient (`agent_notes/AGENTS.md`, “Наблюдатели”). `LbcItems.active(ServerPlayer)` already combines `game.allowDamage`, not-awaiting-respawn, and dimension exclusions (`LbcItems.java`, around lines 562–565), but use the explicit observer check too for projectile ownership. Restrict tracking to the arena level and a live match. On stop, discard current-epoch tracked projectiles before rotating the epoch, and clear both tracking and batch collections. On unload, remove state; on re-load, restore persistent age only for a still-current epoch and avoid re-triggering the launch effect.

Existing source precedent for trinket-created projectiles: `agent_notes/lbc_items_arcana_trinkets.md` says Blaze creates a small fireball and delegates its direction to a raycast helper (around line 257). That is source behavior, not evidence that the new mod has already ported it.








