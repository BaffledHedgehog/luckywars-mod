package me.skywars2.game;

import me.skywars2.mixin.CombatCheckpointAccess;
import me.skywars2.mixin.EntityRuntimeCheckpointAccess;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.animal.parrot.ShoulderRidingEntity;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Reviewed native transient values, with fresh navigation and goals for cloned bodies. */
final class MatchEntityRuntime {
    private final EntityValues entityValues;
    private final Collision collision;
    private final LivingEntityValues livingValues;
    private final SwingValues swing;
    private final CreeperValues creeper;
    private final AbstractHorseValues horse;
    private final IronGolemValues golem;
    private final LlamaValues llama;
    private final ShoulderRidingEntityValues shoulder;
    private final PlayerValues player;
    private final ServerPlayerValues serverPlayer;
    private final ProjectileValues projectile;
    private final ThrownEnderpearlValues pearl;
    private final FishingHookValues fishing;
    private final long worldTime;
    private final boolean using;
    private final InteractionHand usedHand;
    private final MatchMobNavigation navigation;
    private final MatchMobGoals goals;
    private final Long angerEndTime;

    MatchEntityRuntime(Entity entity) {
        this(entity, true);
    }

    MatchEntityRuntime(Entity entity, boolean sameBody) {
        navigation = sameBody && entity instanceof Mob mob ? new MatchMobNavigation(mob) : null;
        goals = sameBody && MatchMobGoals.enabled() && entity instanceof Mob mob ? new MatchMobGoals(mob) : null;
        angerEndTime = entity instanceof NeutralMob neutral ? neutral.getPersistentAngerEndTime() : null;
        collision = goals != null ? new Collision(entity) : null;
        creeper = goals != null && entity instanceof Creeper body ? new CreeperValues(body) : null;
        horse = goals != null && entity instanceof AbstractHorse body ? new AbstractHorseValues(body) : null;
        llama = goals != null && entity instanceof Llama body ? new LlamaValues(body) : null;
        shoulder = goals != null && entity instanceof ShoulderRidingEntity body ? new ShoulderRidingEntityValues(body) : null;
        golem = goals != null && entity instanceof IronGolem body ? new IronGolemValues(body) : null;
        worldTime = entity.level().getGameTime();
        entityValues = new EntityValues(entity);
        if (entity instanceof LivingEntity living) {
            using = living.isUsingItem();
            usedHand = living.getUsedItemHand();
            livingValues = new LivingEntityValues(living);
            swing = new SwingValues(swing(living));
        } else {
            using = false;
            usedHand = InteractionHand.MAIN_HAND;
            livingValues = null;
            swing = null;
        }
        player = entity instanceof Player body ? new PlayerValues(body) : null;
        serverPlayer = entity instanceof ServerPlayer body ? new ServerPlayerValues(body) : null;
        projectile = entity instanceof Projectile body ? new ProjectileValues(body) : null;
        pearl = entity instanceof ThrownEnderpearl body ? new ThrownEnderpearlValues(body) : null;
        fishing = entity instanceof FishingHook body ? new FishingHookValues(body) : null;
    }

    private static LivingEntity.SwingState swing(LivingEntity living) {
        return ((EntityRuntimeCheckpointAccess.LivingEntityAccess)(Object) living).skywars2$runtimeSwing();
    }

    private static long shift(long stamp, long delta) {
        return stamp < 0 ? stamp : Math.addExact(stamp, delta);
    }

    void prepareLoad(Entity entity) {
        if (navigation != null) navigation.requireOwner(entity);
        if (goals != null) goals.prepareLoad();
    }

    void captureGoalReferences(MatchDamageSources sources) {
        if (goals != null) goals.captureReferences(sources);
    }

    void restoreGoalReferences(java.util.function.Function<java.util.UUID, Entity> lookup, java.util.Set<java.util.UUID> pending) {
        if (goals != null) goals.restoreReferences(lookup, pending);
    }

    void restore(Entity entity) {
        if (navigation != null) navigation.requireOwner(entity);
        long delta = entity.level().getGameTime() - worldTime;
        if (collision != null) collision.restore(entity);
        if (creeper != null) creeper.restore((Creeper) entity);
        if (horse != null) horse.restore((AbstractHorse) entity);
        if (llama != null) llama.restore((Llama) entity);
        if (shoulder != null) shoulder.restore((ShoulderRidingEntity) entity);
        if (golem != null) golem.restore((IronGolem) entity);
        entityValues.restore(entity);
        if (livingValues != null) livingValues.restore((LivingEntity) entity, delta);
        if (player != null) player.restore((Player) entity);
        if (serverPlayer != null) serverPlayer.restore((ServerPlayer) entity, delta);
        if (projectile != null) projectile.restore((Projectile) entity);
        if (pearl != null) pearl.restore((ThrownEnderpearl) entity);
        if (fishing != null) fishing.restore((FishingHook) entity);
        if (swing != null) {
            var living = (LivingEntity) entity;
            swing.restore(swing(living));
            ((CombatCheckpointAccess.Living)(Object) living).skywars2$useItem(using ? living.getItemInHand(usedHand) : ItemStack.EMPTY);
        }
        if (goals != null) goals.restore();
        if (navigation != null) navigation.restore();
        if (angerEndTime != null) ((NeutralMob) entity).setPersistentAngerEndTime(shift(angerEndTime, delta));
    }

    private record Collision(
            boolean horizontalCollision, boolean verticalCollision, boolean verticalCollisionBelow,
            boolean minorHorizontalCollision) {
        Collision(Entity source) {
            this(source.horizontalCollision, source.verticalCollision, source.verticalCollisionBelow,
                source.minorHorizontalCollision);
        }

        void restore(Entity target) {
            target.horizontalCollision = horizontalCollision;
            target.verticalCollision = verticalCollision;
            target.verticalCollisionBelow = verticalCollisionBelow;
            target.minorHorizontalCollision = minorHorizontalCollision;
        }
    }

    private record CreeperValues(
            int oldSwell, int swell, int maxSwell, int explosionRadius, boolean droppedSkulls) {
        CreeperValues(Creeper source) {
            this((EntityRuntimeCheckpointAccess.CreeperAccess)(Object) source);
        }

        private CreeperValues(EntityRuntimeCheckpointAccess.CreeperAccess access) {
            this(access.skywars2$runtimeOldSwell(), access.skywars2$runtimeSwell(),
                access.skywars2$runtimeMaxSwell(), access.skywars2$runtimeExplosionRadius(),
                access.skywars2$runtimeDroppedSkulls());
        }

        void restore(Creeper target) {
            var access = (EntityRuntimeCheckpointAccess.CreeperAccess)(Object) target;
            access.skywars2$runtimeOldSwell(oldSwell);
            access.skywars2$runtimeSwell(swell);
            access.skywars2$runtimeMaxSwell(maxSwell);
            access.skywars2$runtimeExplosionRadius(explosionRadius);
            access.skywars2$runtimeDroppedSkulls(droppedSkulls);
        }
    }

    private record AbstractHorseValues(
            int eatingCounter, int mouthCounter, int standCounter, int tailCounter, int sprintCounter,
            float playerJumpPendingScale, boolean allowStandSliding, float eatAnim, float eatAnimO,
            float standAnim, float standAnimO, float mouthAnim, float mouthAnimO, boolean canGallop,
            int gallopSoundCounter) {
        AbstractHorseValues(AbstractHorse source) {
            this(source, (EntityRuntimeCheckpointAccess.HorseAccess)(Object) source);
        }

        private AbstractHorseValues(AbstractHorse source, EntityRuntimeCheckpointAccess.HorseAccess access) {
            this(access.skywars2$runtimeEatingCounter(), access.skywars2$runtimeMouthCounter(),
                access.skywars2$runtimeStandCounter(), source.tailCounter, source.sprintCounter,
                access.skywars2$runtimePlayerJumpPendingScale(), access.skywars2$runtimeAllowStandSliding(),
                access.skywars2$runtimeEatAnim(), access.skywars2$runtimeEatAnimO(),
                access.skywars2$runtimeStandAnim(), access.skywars2$runtimeStandAnimO(),
                access.skywars2$runtimeMouthAnim(), access.skywars2$runtimeMouthAnimO(),
                access.skywars2$runtimeCanGallop(), access.skywars2$runtimeGallopSoundCounter());
        }

        void restore(AbstractHorse target) {
            var access = (EntityRuntimeCheckpointAccess.HorseAccess)(Object) target;
            access.skywars2$runtimeEatingCounter(eatingCounter);
            access.skywars2$runtimeMouthCounter(mouthCounter);
            access.skywars2$runtimeStandCounter(standCounter);
            target.tailCounter = tailCounter;
            target.sprintCounter = sprintCounter;
            access.skywars2$runtimePlayerJumpPendingScale(playerJumpPendingScale);
            access.skywars2$runtimeAllowStandSliding(allowStandSliding);
            access.skywars2$runtimeEatAnim(eatAnim);
            access.skywars2$runtimeEatAnimO(eatAnimO);
            access.skywars2$runtimeStandAnim(standAnim);
            access.skywars2$runtimeStandAnimO(standAnimO);
            access.skywars2$runtimeMouthAnim(mouthAnim);
            access.skywars2$runtimeMouthAnimO(mouthAnimO);
            access.skywars2$runtimeCanGallop(canGallop);
            access.skywars2$runtimeGallopSoundCounter(gallopSoundCounter);
        }
    }

    private record LlamaValues(
            boolean didSpit) {
        LlamaValues(Llama source) {
            this((EntityRuntimeCheckpointAccess.LlamaAccess)(Object) source);
        }

        private LlamaValues(EntityRuntimeCheckpointAccess.LlamaAccess access) {
            this(access.skywars2$runtimeDidSpit());
        }

        void restore(Llama target) {
            var access = (EntityRuntimeCheckpointAccess.LlamaAccess)(Object) target;
            access.skywars2$runtimeDidSpit(didSpit);
        }
    }

    private record ShoulderRidingEntityValues(
            int rideCooldownCounter) {
        ShoulderRidingEntityValues(ShoulderRidingEntity source) {
            this((EntityRuntimeCheckpointAccess.ShoulderRidingEntityAccess)(Object) source);
        }

        private ShoulderRidingEntityValues(EntityRuntimeCheckpointAccess.ShoulderRidingEntityAccess access) {
            this(access.skywars2$runtimeRideCooldownCounter());
        }

        void restore(ShoulderRidingEntity target) {
            var access = (EntityRuntimeCheckpointAccess.ShoulderRidingEntityAccess)(Object) target;
            access.skywars2$runtimeRideCooldownCounter(rideCooldownCounter);
        }
    }

    private record IronGolemValues(
            int attackAnimationTick, int offerFlowerTick) {
        IronGolemValues(IronGolem source) {
            this((EntityRuntimeCheckpointAccess.IronGolemAccess)(Object) source);
        }

        private IronGolemValues(EntityRuntimeCheckpointAccess.IronGolemAccess access) {
            this(access.skywars2$runtimeAttackAnimationTick(), access.skywars2$runtimeOfferFlowerTick());
        }

        void restore(IronGolem target) {
            var access = (EntityRuntimeCheckpointAccess.IronGolemAccess)(Object) target;
            access.skywars2$runtimeAttackAnimationTick(attackAnimationTick);
            access.skywars2$runtimeOfferFlowerTick(offerFlowerTick);
        }
    }

    private record EntityValues(
            int tickCount, int remainingFireTicks, int invulnerableTime, int portalCooldown, double fallDistance,
            boolean onGround, boolean onGroundNoBlocks, boolean noPhysics, boolean firstTick, float xRotO,
            float yRotO, float moveDist, float flyDist, float nextStep, boolean hasVisualFire,
            int boardingCooldown, Vec3 lastKnownSpeed, Vec3 lastKnownPosition) {
        EntityValues(Entity source) {
            this(source, (EntityRuntimeCheckpointAccess.EntityAccess)(Object) source);
        }

        private EntityValues(Entity source, EntityRuntimeCheckpointAccess.EntityAccess access) {
            this(source.tickCount, access.skywars2$runtimeRemainingFireTicks(),
                access.skywars2$runtimeInvulnerableTime(), access.skywars2$runtimePortalCooldown(),
                source.fallDistance, access.skywars2$runtimeOnGround(),
                access.skywars2$runtimeOnGroundNoBlocks(), source.noPhysics, access.skywars2$runtimeFirstTick(),
                source.xRotO, source.yRotO, source.moveDist, source.flyDist, access.skywars2$runtimeNextStep(),
                access.skywars2$runtimeHasVisualFire(), access.skywars2$runtimeBoardingCooldown(),
                access.skywars2$runtimeLastKnownSpeed(), access.skywars2$runtimeLastKnownPosition());
        }

        void restore(Entity target) {
            var access = (EntityRuntimeCheckpointAccess.EntityAccess)(Object) target;
            target.tickCount = tickCount;
            access.skywars2$runtimeRemainingFireTicks(remainingFireTicks);
            access.skywars2$runtimeInvulnerableTime(invulnerableTime);
            access.skywars2$runtimePortalCooldown(portalCooldown);
            target.fallDistance = fallDistance;
            access.skywars2$runtimeOnGround(onGround);
            access.skywars2$runtimeOnGroundNoBlocks(onGroundNoBlocks);
            target.noPhysics = noPhysics;
            access.skywars2$runtimeFirstTick(firstTick);
            target.xRotO = xRotO;
            target.yRotO = yRotO;
            target.moveDist = moveDist;
            target.flyDist = flyDist;
            access.skywars2$runtimeNextStep(nextStep);
            access.skywars2$runtimeHasVisualFire(hasVisualFire);
            access.skywars2$runtimeBoardingCooldown(boardingCooldown);
            access.skywars2$runtimeLastKnownSpeed(lastKnownSpeed);
            access.skywars2$runtimeLastKnownPosition(lastKnownPosition);
        }
    }

    private record LivingEntityValues(
            int attackStrengthTicker, int itemSwapTicker, int hurtTime, int hurtDuration, int deathTime,
            int damageCooldownTime, boolean dead, int noActionTime, float lastHurt, boolean jumping, float xxa,
            float yya, float zza, float speed, int noJumpDelay, int fallFlyTicks, int useItemRemaining,
            int autoSpinAttackTicks, float autoSpinAttackDmg, int lastHurtByPlayerMemoryTime,
            int lastHurtByMobTimestamp, int lastHurtMobTimestamp, float yBodyRot, float yBodyRotO,
            float yHeadRot, float yHeadRotO, long lastDamageStamp, long lastKineticHitFeedbackTime) {
        LivingEntityValues(LivingEntity source) {
            this(source, (EntityRuntimeCheckpointAccess.LivingEntityAccess)(Object) source);
        }

        private LivingEntityValues(LivingEntity source, EntityRuntimeCheckpointAccess.LivingEntityAccess access) {
            this(access.skywars2$runtimeAttackStrengthTicker(), access.skywars2$runtimeItemSwapTicker(),
                source.hurtTime, source.hurtDuration, source.deathTime, source.damageCooldownTime,
                access.skywars2$runtimeDead(), access.skywars2$runtimeNoActionTime(),
                access.skywars2$runtimeLastHurt(), access.skywars2$runtimeJumping(), source.xxa, source.yya,
                source.zza, access.skywars2$runtimeSpeed(), access.skywars2$runtimeNoJumpDelay(),
                access.skywars2$runtimeFallFlyTicks(), access.skywars2$runtimeUseItemRemaining(),
                access.skywars2$runtimeAutoSpinAttackTicks(), access.skywars2$runtimeAutoSpinAttackDmg(),
                access.skywars2$runtimeLastHurtByPlayerMemoryTime(),
                access.skywars2$runtimeLastHurtByMobTimestamp(), access.skywars2$runtimeLastHurtMobTimestamp(),
                source.yBodyRot, source.yBodyRotO, source.yHeadRot, source.yHeadRotO,
                access.skywars2$runtimeLastDamageStamp(), access.skywars2$runtimeLastKineticHitFeedbackTime());
        }

        void restore(LivingEntity target, long shift) {
            var access = (EntityRuntimeCheckpointAccess.LivingEntityAccess)(Object) target;
            access.skywars2$runtimeAttackStrengthTicker(attackStrengthTicker);
            access.skywars2$runtimeItemSwapTicker(itemSwapTicker);
            target.hurtTime = hurtTime;
            target.hurtDuration = hurtDuration;
            target.deathTime = deathTime;
            target.damageCooldownTime = damageCooldownTime;
            access.skywars2$runtimeDead(dead);
            access.skywars2$runtimeNoActionTime(noActionTime);
            access.skywars2$runtimeLastHurt(lastHurt);
            access.skywars2$runtimeJumping(jumping);
            target.xxa = xxa;
            target.yya = yya;
            target.zza = zza;
            access.skywars2$runtimeSpeed(speed);
            access.skywars2$runtimeNoJumpDelay(noJumpDelay);
            access.skywars2$runtimeFallFlyTicks(fallFlyTicks);
            access.skywars2$runtimeUseItemRemaining(useItemRemaining);
            access.skywars2$runtimeAutoSpinAttackTicks(autoSpinAttackTicks);
            access.skywars2$runtimeAutoSpinAttackDmg(autoSpinAttackDmg);
            access.skywars2$runtimeLastHurtByPlayerMemoryTime(lastHurtByPlayerMemoryTime);
            access.skywars2$runtimeLastHurtByMobTimestamp(lastHurtByMobTimestamp);
            access.skywars2$runtimeLastHurtMobTimestamp(lastHurtMobTimestamp);
            target.yBodyRot = yBodyRot;
            target.yBodyRotO = yBodyRotO;
            target.yHeadRot = yHeadRot;
            target.yHeadRotO = yHeadRotO;
            access.skywars2$runtimeLastDamageStamp(shift(lastDamageStamp, shift));
            access.skywars2$runtimeLastKineticHitFeedbackTime(shift(lastKineticHitFeedbackTime, shift));
        }
    }

    private record PlayerValues(
            int sleepCounter, int jumpTriggerTime) {
        PlayerValues(Player source) {
            this((EntityRuntimeCheckpointAccess.PlayerAccess)(Object) source);
        }

        private PlayerValues(EntityRuntimeCheckpointAccess.PlayerAccess access) {
            this(access.skywars2$runtimeSleepCounter(), access.skywars2$runtimeJumpTriggerTime());
        }

        void restore(Player target) {
            var access = (EntityRuntimeCheckpointAccess.PlayerAccess)(Object) target;
            access.skywars2$runtimeSleepCounter(sleepCounter);
            access.skywars2$runtimeJumpTriggerTime(jumpTriggerTime);
        }
    }

    private record ServerPlayerValues(
            Vec3 levitationStartPos, int levitationStartTime, Vec3 startingToFallPosition,
            Vec3 enteredLavaOnVehiclePosition, long timeEntitySatOnShoulder, Vec3 lastKnownClientMovement) {
        ServerPlayerValues(ServerPlayer source) {
            this((EntityRuntimeCheckpointAccess.ServerPlayerAccess)(Object) source);
        }

        private ServerPlayerValues(EntityRuntimeCheckpointAccess.ServerPlayerAccess access) {
            this(access.skywars2$runtimeLevitationStartPos(), access.skywars2$runtimeLevitationStartTime(),
                access.skywars2$runtimeStartingToFallPosition(),
                access.skywars2$runtimeEnteredLavaOnVehiclePosition(),
                access.skywars2$runtimeTimeEntitySatOnShoulder(),
                access.skywars2$runtimeLastKnownClientMovement());
        }

        void restore(ServerPlayer target, long shift) {
            var access = (EntityRuntimeCheckpointAccess.ServerPlayerAccess)(Object) target;
            access.skywars2$runtimeLevitationStartPos(levitationStartPos);
            access.skywars2$runtimeLevitationStartTime(levitationStartTime);
            access.skywars2$runtimeStartingToFallPosition(startingToFallPosition);
            access.skywars2$runtimeEnteredLavaOnVehiclePosition(enteredLavaOnVehiclePosition);
            access.skywars2$runtimeTimeEntitySatOnShoulder(shift(timeEntitySatOnShoulder, shift));
            access.skywars2$runtimeLastKnownClientMovement(lastKnownClientMovement);
        }
    }

    private record ProjectileValues(
            boolean leftOwnerChecked) {
        ProjectileValues(Projectile source) {
            this((EntityRuntimeCheckpointAccess.ProjectileAccess)(Object) source);
        }

        private ProjectileValues(EntityRuntimeCheckpointAccess.ProjectileAccess access) {
            this(access.skywars2$runtimeLeftOwnerChecked());
        }

        void restore(Projectile target) {
            var access = (EntityRuntimeCheckpointAccess.ProjectileAccess)(Object) target;
            access.skywars2$runtimeLeftOwnerChecked(leftOwnerChecked);
        }
    }

    private record ThrownEnderpearlValues(
            long ticketTimer) {
        ThrownEnderpearlValues(ThrownEnderpearl source) {
            this((EntityRuntimeCheckpointAccess.ThrownEnderpearlAccess)(Object) source);
        }

        private ThrownEnderpearlValues(EntityRuntimeCheckpointAccess.ThrownEnderpearlAccess access) {
            this(access.skywars2$runtimeTicketTimer());
        }

        void restore(ThrownEnderpearl target) {
            var access = (EntityRuntimeCheckpointAccess.ThrownEnderpearlAccess)(Object) target;
            access.skywars2$runtimeTicketTimer(ticketTimer);
        }
    }

    private record FishingHookValues(
            boolean biting, int outOfWaterTime, int life, int nibble, int timeUntilLured, int timeUntilHooked,
            float fishAngle, boolean openWater, FishingHook.FishHookState currentState) {
        FishingHookValues(FishingHook source) {
            this((EntityRuntimeCheckpointAccess.FishingHookAccess)(Object) source);
        }

        private FishingHookValues(EntityRuntimeCheckpointAccess.FishingHookAccess access) {
            this(access.skywars2$runtimeBiting(), access.skywars2$runtimeOutOfWaterTime(),
                access.skywars2$runtimeLife(), access.skywars2$runtimeNibble(),
                access.skywars2$runtimeTimeUntilLured(), access.skywars2$runtimeTimeUntilHooked(),
                access.skywars2$runtimeFishAngle(), access.skywars2$runtimeOpenWater(),
                access.skywars2$runtimeCurrentState());
        }

        void restore(FishingHook target) {
            var access = (EntityRuntimeCheckpointAccess.FishingHookAccess)(Object) target;
            access.skywars2$runtimeBiting(biting);
            access.skywars2$runtimeOutOfWaterTime(outOfWaterTime);
            access.skywars2$runtimeLife(life);
            access.skywars2$runtimeNibble(nibble);
            access.skywars2$runtimeTimeUntilLured(timeUntilLured);
            access.skywars2$runtimeTimeUntilHooked(timeUntilHooked);
            access.skywars2$runtimeFishAngle(fishAngle);
            access.skywars2$runtimeOpenWater(openWater);
            access.skywars2$runtimeCurrentState(currentState);
        }
    }

    private record SwingValues(
            LivingEntity.SwingDescription currentSwing, int ticks, float oldAnimation, float animation) {
        SwingValues(LivingEntity.SwingState source) {
            this((EntityRuntimeCheckpointAccess.SwingAccess)(Object) source);
        }

        private SwingValues(EntityRuntimeCheckpointAccess.SwingAccess access) {
            this(access.skywars2$runtimeCurrentSwing(), access.skywars2$runtimeTicks(),
                access.skywars2$runtimeOldAnimation(), access.skywars2$runtimeAnimation());
        }

        void restore(LivingEntity.SwingState target) {
            var access = (EntityRuntimeCheckpointAccess.SwingAccess)(Object) target;
            access.skywars2$runtimeCurrentSwing(currentSwing);
            access.skywars2$runtimeTicks(ticks);
            access.skywars2$runtimeOldAnimation(oldAnimation);
            access.skywars2$runtimeAnimation(animation);
        }
    }
}
