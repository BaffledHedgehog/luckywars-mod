package me.skywars2.mixin;

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
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Raw native runtime values with no gameplay callbacks during snapshot replay. */
public final class EntityRuntimeCheckpointAccess {
    private EntityRuntimeCheckpointAccess() { }

    @Mixin(Creeper.class)
    public interface CreeperAccess {
        @Accessor("oldSwell") int skywars2$runtimeOldSwell();
        @Accessor("oldSwell") void skywars2$runtimeOldSwell(int value);
        @Accessor("swell") int skywars2$runtimeSwell();
        @Accessor("swell") void skywars2$runtimeSwell(int value);
        @Accessor("maxSwell") int skywars2$runtimeMaxSwell();
        @Accessor("maxSwell") void skywars2$runtimeMaxSwell(int value);
        @Accessor("explosionRadius") int skywars2$runtimeExplosionRadius();
        @Accessor("explosionRadius") void skywars2$runtimeExplosionRadius(int value);
        @Accessor("droppedSkulls") boolean skywars2$runtimeDroppedSkulls();
        @Accessor("droppedSkulls") void skywars2$runtimeDroppedSkulls(boolean value);
    }

    @Mixin(AbstractHorse.class)
    public interface HorseAccess {
        @Accessor("eatingCounter") int skywars2$runtimeEatingCounter();
        @Accessor("eatingCounter") void skywars2$runtimeEatingCounter(int value);
        @Accessor("mouthCounter") int skywars2$runtimeMouthCounter();
        @Accessor("mouthCounter") void skywars2$runtimeMouthCounter(int value);
        @Accessor("standCounter") int skywars2$runtimeStandCounter();
        @Accessor("standCounter") void skywars2$runtimeStandCounter(int value);
        @Accessor("playerJumpPendingScale") float skywars2$runtimePlayerJumpPendingScale();
        @Accessor("playerJumpPendingScale") void skywars2$runtimePlayerJumpPendingScale(float value);
        @Accessor("allowStandSliding") boolean skywars2$runtimeAllowStandSliding();
        @Accessor("allowStandSliding") void skywars2$runtimeAllowStandSliding(boolean value);
        @Accessor("eatAnim") float skywars2$runtimeEatAnim();
        @Accessor("eatAnim") void skywars2$runtimeEatAnim(float value);
        @Accessor("eatAnimO") float skywars2$runtimeEatAnimO();
        @Accessor("eatAnimO") void skywars2$runtimeEatAnimO(float value);
        @Accessor("standAnim") float skywars2$runtimeStandAnim();
        @Accessor("standAnim") void skywars2$runtimeStandAnim(float value);
        @Accessor("standAnimO") float skywars2$runtimeStandAnimO();
        @Accessor("standAnimO") void skywars2$runtimeStandAnimO(float value);
        @Accessor("mouthAnim") float skywars2$runtimeMouthAnim();
        @Accessor("mouthAnim") void skywars2$runtimeMouthAnim(float value);
        @Accessor("mouthAnimO") float skywars2$runtimeMouthAnimO();
        @Accessor("mouthAnimO") void skywars2$runtimeMouthAnimO(float value);
        @Accessor("canGallop") boolean skywars2$runtimeCanGallop();
        @Accessor("canGallop") void skywars2$runtimeCanGallop(boolean value);
        @Accessor("gallopSoundCounter") int skywars2$runtimeGallopSoundCounter();
        @Accessor("gallopSoundCounter") void skywars2$runtimeGallopSoundCounter(int value);
    }

    @Mixin(Llama.class)
    public interface LlamaAccess {
        @Accessor("didSpit") boolean skywars2$runtimeDidSpit();
        @Accessor("didSpit") void skywars2$runtimeDidSpit(boolean value);
    }

    @Mixin(ShoulderRidingEntity.class)
    public interface ShoulderRidingEntityAccess {
        @Accessor("rideCooldownCounter") int skywars2$runtimeRideCooldownCounter();
        @Accessor("rideCooldownCounter") void skywars2$runtimeRideCooldownCounter(int value);
    }

    @Mixin(IronGolem.class)
    public interface IronGolemAccess {
        @Accessor("attackAnimationTick") int skywars2$runtimeAttackAnimationTick();
        @Accessor("attackAnimationTick") void skywars2$runtimeAttackAnimationTick(int value);
        @Accessor("offerFlowerTick") int skywars2$runtimeOfferFlowerTick();
        @Accessor("offerFlowerTick") void skywars2$runtimeOfferFlowerTick(int value);
    }

    @Mixin(Entity.class)
    public interface EntityAccess {
        @Accessor("remainingFireTicks") int skywars2$runtimeRemainingFireTicks();
        @Accessor("remainingFireTicks") void skywars2$runtimeRemainingFireTicks(int value);
        @Accessor("invulnerableTime") int skywars2$runtimeInvulnerableTime();
        @Accessor("invulnerableTime") void skywars2$runtimeInvulnerableTime(int value);
        @Accessor("portalCooldown") int skywars2$runtimePortalCooldown();
        @Accessor("portalCooldown") void skywars2$runtimePortalCooldown(int value);
        @Accessor("onGround") boolean skywars2$runtimeOnGround();
        @Accessor("onGround") void skywars2$runtimeOnGround(boolean value);
        @Accessor("onGroundNoBlocks") boolean skywars2$runtimeOnGroundNoBlocks();
        @Accessor("onGroundNoBlocks") void skywars2$runtimeOnGroundNoBlocks(boolean value);
        @Accessor("firstTick") boolean skywars2$runtimeFirstTick();
        @Accessor("firstTick") void skywars2$runtimeFirstTick(boolean value);
        @Accessor("nextStep") float skywars2$runtimeNextStep();
        @Accessor("nextStep") void skywars2$runtimeNextStep(float value);
        @Accessor("hasVisualFire") boolean skywars2$runtimeHasVisualFire();
        @Accessor("hasVisualFire") void skywars2$runtimeHasVisualFire(boolean value);
        @Accessor("boardingCooldown") int skywars2$runtimeBoardingCooldown();
        @Accessor("boardingCooldown") void skywars2$runtimeBoardingCooldown(int value);
        @Accessor("lastKnownSpeed") Vec3 skywars2$runtimeLastKnownSpeed();
        @Accessor("lastKnownSpeed") void skywars2$runtimeLastKnownSpeed(Vec3 value);
        @Accessor("lastKnownPosition") Vec3 skywars2$runtimeLastKnownPosition();
        @Accessor("lastKnownPosition") void skywars2$runtimeLastKnownPosition(Vec3 value);
    }

    @Mixin(LivingEntity.class)
    public interface LivingEntityAccess {
        @Accessor("attackStrengthTicker") int skywars2$runtimeAttackStrengthTicker();
        @Accessor("attackStrengthTicker") void skywars2$runtimeAttackStrengthTicker(int value);
        @Accessor("itemSwapTicker") int skywars2$runtimeItemSwapTicker();
        @Accessor("itemSwapTicker") void skywars2$runtimeItemSwapTicker(int value);
        @Accessor("dead") boolean skywars2$runtimeDead();
        @Accessor("dead") void skywars2$runtimeDead(boolean value);
        @Accessor("noActionTime") int skywars2$runtimeNoActionTime();
        @Accessor("noActionTime") void skywars2$runtimeNoActionTime(int value);
        @Accessor("lastHurt") float skywars2$runtimeLastHurt();
        @Accessor("lastHurt") void skywars2$runtimeLastHurt(float value);
        @Accessor("jumping") boolean skywars2$runtimeJumping();
        @Accessor("jumping") void skywars2$runtimeJumping(boolean value);
        @Accessor("speed") float skywars2$runtimeSpeed();
        @Accessor("speed") void skywars2$runtimeSpeed(float value);
        @Accessor("noJumpDelay") int skywars2$runtimeNoJumpDelay();
        @Accessor("noJumpDelay") void skywars2$runtimeNoJumpDelay(int value);
        @Accessor("fallFlyTicks") int skywars2$runtimeFallFlyTicks();
        @Accessor("fallFlyTicks") void skywars2$runtimeFallFlyTicks(int value);
        @Accessor("useItemRemaining") int skywars2$runtimeUseItemRemaining();
        @Accessor("useItemRemaining") void skywars2$runtimeUseItemRemaining(int value);
        @Accessor("autoSpinAttackTicks") int skywars2$runtimeAutoSpinAttackTicks();
        @Accessor("autoSpinAttackTicks") void skywars2$runtimeAutoSpinAttackTicks(int value);
        @Accessor("autoSpinAttackDmg") float skywars2$runtimeAutoSpinAttackDmg();
        @Accessor("autoSpinAttackDmg") void skywars2$runtimeAutoSpinAttackDmg(float value);
        @Accessor("lastHurtByPlayerMemoryTime") int skywars2$runtimeLastHurtByPlayerMemoryTime();
        @Accessor("lastHurtByPlayerMemoryTime") void skywars2$runtimeLastHurtByPlayerMemoryTime(int value);
        @Accessor("lastHurtByMobTimestamp") int skywars2$runtimeLastHurtByMobTimestamp();
        @Accessor("lastHurtByMobTimestamp") void skywars2$runtimeLastHurtByMobTimestamp(int value);
        @Accessor("lastHurtMobTimestamp") int skywars2$runtimeLastHurtMobTimestamp();
        @Accessor("lastHurtMobTimestamp") void skywars2$runtimeLastHurtMobTimestamp(int value);
        @Accessor("lastDamageStamp") long skywars2$runtimeLastDamageStamp();
        @Accessor("lastDamageStamp") void skywars2$runtimeLastDamageStamp(long value);
        @Accessor("lastKineticHitFeedbackTime") long skywars2$runtimeLastKineticHitFeedbackTime();
        @Accessor("lastKineticHitFeedbackTime") void skywars2$runtimeLastKineticHitFeedbackTime(long value);
        @Accessor("swingState") LivingEntity.SwingState skywars2$runtimeSwing();
    }

    @Mixin(Player.class)
    public interface PlayerAccess {
        @Accessor("sleepCounter") int skywars2$runtimeSleepCounter();
        @Accessor("sleepCounter") void skywars2$runtimeSleepCounter(int value);
        @Accessor("jumpTriggerTime") int skywars2$runtimeJumpTriggerTime();
        @Accessor("jumpTriggerTime") void skywars2$runtimeJumpTriggerTime(int value);
    }

    @Mixin(ServerPlayer.class)
    public interface ServerPlayerAccess {
        @Accessor("levitationStartPos") Vec3 skywars2$runtimeLevitationStartPos();
        @Accessor("levitationStartPos") void skywars2$runtimeLevitationStartPos(Vec3 value);
        @Accessor("levitationStartTime") int skywars2$runtimeLevitationStartTime();
        @Accessor("levitationStartTime") void skywars2$runtimeLevitationStartTime(int value);
        @Accessor("startingToFallPosition") Vec3 skywars2$runtimeStartingToFallPosition();
        @Accessor("startingToFallPosition") void skywars2$runtimeStartingToFallPosition(Vec3 value);
        @Accessor("enteredLavaOnVehiclePosition") Vec3 skywars2$runtimeEnteredLavaOnVehiclePosition();
        @Accessor("enteredLavaOnVehiclePosition") void skywars2$runtimeEnteredLavaOnVehiclePosition(Vec3 value);
        @Accessor("timeEntitySatOnShoulder") long skywars2$runtimeTimeEntitySatOnShoulder();
        @Accessor("timeEntitySatOnShoulder") void skywars2$runtimeTimeEntitySatOnShoulder(long value);
        @Accessor("lastKnownClientMovement") Vec3 skywars2$runtimeLastKnownClientMovement();
        @Accessor("lastKnownClientMovement") void skywars2$runtimeLastKnownClientMovement(Vec3 value);
    }

    @Mixin(Projectile.class)
    public interface ProjectileAccess {
        @Accessor("leftOwnerChecked") boolean skywars2$runtimeLeftOwnerChecked();
        @Accessor("leftOwnerChecked") void skywars2$runtimeLeftOwnerChecked(boolean value);
    }

    @Mixin(ThrownEnderpearl.class)
    public interface ThrownEnderpearlAccess {
        @Accessor("ticketTimer") long skywars2$runtimeTicketTimer();
        @Accessor("ticketTimer") void skywars2$runtimeTicketTimer(long value);
    }

    @Mixin(FishingHook.class)
    public interface FishingHookAccess {
        @Accessor("biting") boolean skywars2$runtimeBiting();
        @Accessor("biting") void skywars2$runtimeBiting(boolean value);
        @Accessor("outOfWaterTime") int skywars2$runtimeOutOfWaterTime();
        @Accessor("outOfWaterTime") void skywars2$runtimeOutOfWaterTime(int value);
        @Accessor("life") int skywars2$runtimeLife();
        @Accessor("life") void skywars2$runtimeLife(int value);
        @Accessor("nibble") int skywars2$runtimeNibble();
        @Accessor("nibble") void skywars2$runtimeNibble(int value);
        @Accessor("timeUntilLured") int skywars2$runtimeTimeUntilLured();
        @Accessor("timeUntilLured") void skywars2$runtimeTimeUntilLured(int value);
        @Accessor("timeUntilHooked") int skywars2$runtimeTimeUntilHooked();
        @Accessor("timeUntilHooked") void skywars2$runtimeTimeUntilHooked(int value);
        @Accessor("fishAngle") float skywars2$runtimeFishAngle();
        @Accessor("fishAngle") void skywars2$runtimeFishAngle(float value);
        @Accessor("openWater") boolean skywars2$runtimeOpenWater();
        @Accessor("openWater") void skywars2$runtimeOpenWater(boolean value);
        @Accessor("currentState") FishingHook.FishHookState skywars2$runtimeCurrentState();
        @Accessor("currentState") void skywars2$runtimeCurrentState(FishingHook.FishHookState value);
    }

    @Mixin(LivingEntity.SwingState.class)
    public interface SwingAccess {
        @Accessor("currentSwing") LivingEntity.SwingDescription skywars2$runtimeCurrentSwing();
        @Accessor("currentSwing") void skywars2$runtimeCurrentSwing(LivingEntity.SwingDescription value);
        @Accessor("ticks") int skywars2$runtimeTicks();
        @Accessor("ticks") void skywars2$runtimeTicks(int value);
        @Accessor("oldAnimation") float skywars2$runtimeOldAnimation();
        @Accessor("oldAnimation") void skywars2$runtimeOldAnimation(float value);
        @Accessor("animation") float skywars2$runtimeAnimation();
        @Accessor("animation") void skywars2$runtimeAnimation(float value);
    }
}
