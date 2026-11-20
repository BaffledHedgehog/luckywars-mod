package me.skywars2.mixin;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Explicit native goal checkpoint slots for tasks represented by the match rewind. */
public final class MobGoalCheckpointAccess {
    private MobGoalCheckpointAccess() { }

    @Mixin(LookAtPlayerGoal.class)
    public interface LookAtPlayerGoalAccess {
        @Accessor("lookAt") Entity skywars2$goalLookAt();
        @Accessor("lookAt") void skywars2$goalLookAt(Entity value);
        @Accessor("lookTime") int skywars2$goalLookTime();
        @Accessor("lookTime") void skywars2$goalLookTime(int value);
    }

    @Mixin(RandomLookAroundGoal.class)
    public interface RandomLookAroundGoalAccess {
        @Accessor("relX") double skywars2$goalRelX();
        @Accessor("relX") void skywars2$goalRelX(double value);
        @Accessor("relZ") double skywars2$goalRelZ();
        @Accessor("relZ") void skywars2$goalRelZ(double value);
        @Accessor("lookTime") int skywars2$goalLookTime();
        @Accessor("lookTime") void skywars2$goalLookTime(int value);
    }

    @Mixin(FollowParentGoal.class)
    public interface FollowParentGoalAccess {
        @Accessor("parent") Animal skywars2$goalParent();
        @Accessor("parent") void skywars2$goalParent(Animal value);
        @Accessor("timeToRecalcPath") int skywars2$goalTimeToRecalcPath();
        @Accessor("timeToRecalcPath") void skywars2$goalTimeToRecalcPath(int value);
    }

    @Mixin(FollowOwnerGoal.class)
    public interface FollowOwnerGoalAccess {
        @Accessor("owner") LivingEntity skywars2$goalOwner();
        @Accessor("owner") void skywars2$goalOwner(LivingEntity value);
        @Accessor("timeToRecalcPath") int skywars2$goalTimeToRecalcPath();
        @Accessor("timeToRecalcPath") void skywars2$goalTimeToRecalcPath(int value);
        @Accessor("oldWaterCost") float skywars2$goalOldWaterCost();
        @Accessor("oldWaterCost") void skywars2$goalOldWaterCost(float value);
    }

    @Mixin(FollowMobGoal.class)
    public interface FollowMobGoalAccess {
        @Accessor("followingMob") Mob skywars2$goalFollowingMob();
        @Accessor("followingMob") void skywars2$goalFollowingMob(Mob value);
        @Accessor("timeToRecalcPath") int skywars2$goalTimeToRecalcPath();
        @Accessor("timeToRecalcPath") void skywars2$goalTimeToRecalcPath(int value);
        @Accessor("oldWaterCost") float skywars2$goalOldWaterCost();
        @Accessor("oldWaterCost") void skywars2$goalOldWaterCost(float value);
    }

    @Mixin(TemptGoal.class)
    public interface TemptGoalAccess {
        @Accessor("px") double skywars2$goalPx();
        @Accessor("px") void skywars2$goalPx(double value);
        @Accessor("py") double skywars2$goalPy();
        @Accessor("py") void skywars2$goalPy(double value);
        @Accessor("pz") double skywars2$goalPz();
        @Accessor("pz") void skywars2$goalPz(double value);
        @Accessor("pRotX") double skywars2$goalPRotX();
        @Accessor("pRotX") void skywars2$goalPRotX(double value);
        @Accessor("pRotY") double skywars2$goalPRotY();
        @Accessor("pRotY") void skywars2$goalPRotY(double value);
        @Accessor("player") Player skywars2$goalPlayer();
        @Accessor("player") void skywars2$goalPlayer(Player value);
        @Accessor("calmDown") int skywars2$goalCalmDown();
        @Accessor("calmDown") void skywars2$goalCalmDown(int value);
        @Accessor("isRunning") boolean skywars2$goalIsRunning();
        @Accessor("isRunning") void skywars2$goalIsRunning(boolean value);
    }

    @Mixin(AvoidEntityGoal.class)
    public interface AvoidEntityGoalAccess {
        @Accessor("toAvoid") LivingEntity skywars2$goalToAvoid();
        @Accessor("toAvoid") void skywars2$goalToAvoid(LivingEntity value);
        @Accessor("path") Path skywars2$goalPath();
        @Accessor("path") void skywars2$goalPath(Path value);
    }

    @Mixin(RangedBowAttackGoal.class)
    public interface RangedBowAttackGoalAccess {
        @Accessor("attackIntervalMin") int skywars2$goalAttackIntervalMin();
        @Accessor("attackIntervalMin") void skywars2$goalAttackIntervalMin(int value);
        @Accessor("attackTime") int skywars2$goalAttackTime();
        @Accessor("attackTime") void skywars2$goalAttackTime(int value);
        @Accessor("seeTime") int skywars2$goalSeeTime();
        @Accessor("seeTime") void skywars2$goalSeeTime(int value);
        @Accessor("strafingClockwise") boolean skywars2$goalStrafingClockwise();
        @Accessor("strafingClockwise") void skywars2$goalStrafingClockwise(boolean value);
        @Accessor("strafingBackwards") boolean skywars2$goalStrafingBackwards();
        @Accessor("strafingBackwards") void skywars2$goalStrafingBackwards(boolean value);
        @Accessor("strafingTime") int skywars2$goalStrafingTime();
        @Accessor("strafingTime") void skywars2$goalStrafingTime(int value);
    }

    @Mixin(RangedCrossbowAttackGoal.class)
    public interface RangedCrossbowAttackGoalAccess {
        @Accessor("crossbowState") RangedCrossbowAttackGoal.CrossbowState skywars2$goalCrossbowState();
        @Accessor("crossbowState") void skywars2$goalCrossbowState(RangedCrossbowAttackGoal.CrossbowState value);
        @Accessor("seeTime") int skywars2$goalSeeTime();
        @Accessor("seeTime") void skywars2$goalSeeTime(int value);
        @Accessor("attackDelay") int skywars2$goalAttackDelay();
        @Accessor("attackDelay") void skywars2$goalAttackDelay(int value);
        @Accessor("updatePathDelay") int skywars2$goalUpdatePathDelay();
        @Accessor("updatePathDelay") void skywars2$goalUpdatePathDelay(int value);
    }

    @Mixin(RandomStrollGoal.class)
    public interface RandomStrollGoalAccess {
        @Accessor("wantedX") double skywars2$goalWantedX();
        @Accessor("wantedX") void skywars2$goalWantedX(double value);
        @Accessor("wantedY") double skywars2$goalWantedY();
        @Accessor("wantedY") void skywars2$goalWantedY(double value);
        @Accessor("wantedZ") double skywars2$goalWantedZ();
        @Accessor("wantedZ") void skywars2$goalWantedZ(double value);
        @Accessor("interval") int skywars2$goalInterval();
        @Accessor("interval") void skywars2$goalInterval(int value);
        @Accessor("forceTrigger") boolean skywars2$goalForceTrigger();
        @Accessor("forceTrigger") void skywars2$goalForceTrigger(boolean value);
    }

    @Mixin(PanicGoal.class)
    public interface PanicGoalAccess {
        @Accessor("posX") double skywars2$goalPosX();
        @Accessor("posX") void skywars2$goalPosX(double value);
        @Accessor("posY") double skywars2$goalPosY();
        @Accessor("posY") void skywars2$goalPosY(double value);
        @Accessor("posZ") double skywars2$goalPosZ();
        @Accessor("posZ") void skywars2$goalPosZ(double value);
        @Accessor("isRunning") boolean skywars2$goalIsRunning();
        @Accessor("isRunning") void skywars2$goalIsRunning(boolean value);
    }

    @Mixin(FleeSunGoal.class)
    public interface FleeSunGoalAccess {
        @Accessor("wantedX") double skywars2$goalWantedX();
        @Accessor("wantedX") void skywars2$goalWantedX(double value);
        @Accessor("wantedY") double skywars2$goalWantedY();
        @Accessor("wantedY") void skywars2$goalWantedY(double value);
        @Accessor("wantedZ") double skywars2$goalWantedZ();
        @Accessor("wantedZ") void skywars2$goalWantedZ(double value);
    }

    @Mixin(MoveTowardsTargetGoal.class)
    public interface MoveTowardsTargetGoalAccess {
        @Accessor("target") LivingEntity skywars2$goalTarget();
        @Accessor("target") void skywars2$goalTarget(LivingEntity value);
        @Accessor("wantedX") double skywars2$goalWantedX();
        @Accessor("wantedX") void skywars2$goalWantedX(double value);
        @Accessor("wantedY") double skywars2$goalWantedY();
        @Accessor("wantedY") void skywars2$goalWantedY(double value);
        @Accessor("wantedZ") double skywars2$goalWantedZ();
        @Accessor("wantedZ") void skywars2$goalWantedZ(double value);
    }

    @Mixin(MoveTowardsRestrictionGoal.class)
    public interface MoveTowardsRestrictionGoalAccess {
        @Accessor("wantedX") double skywars2$goalWantedX();
        @Accessor("wantedX") void skywars2$goalWantedX(double value);
        @Accessor("wantedY") double skywars2$goalWantedY();
        @Accessor("wantedY") void skywars2$goalWantedY(double value);
        @Accessor("wantedZ") double skywars2$goalWantedZ();
        @Accessor("wantedZ") void skywars2$goalWantedZ(double value);
    }

    @Mixin(LeapAtTargetGoal.class)
    public interface LeapAtTargetGoalAccess {
        @Accessor("target") LivingEntity skywars2$goalTarget();
        @Accessor("target") void skywars2$goalTarget(LivingEntity value);
    }

    @Mixin(SwellGoal.class)
    public interface SwellGoalAccess {
        @Accessor("target") LivingEntity skywars2$goalTarget();
        @Accessor("target") void skywars2$goalTarget(LivingEntity value);
    }

    @Mixin(EatBlockGoal.class)
    public interface EatBlockGoalAccess {
        @Accessor("eatAnimationTick") int skywars2$goalEatAnimationTick();
        @Accessor("eatAnimationTick") void skywars2$goalEatAnimationTick(int value);
    }

    @Mixin(OcelotAttackGoal.class)
    public interface OcelotAttackGoalAccess {
        @Accessor("target") LivingEntity skywars2$goalTarget();
        @Accessor("target") void skywars2$goalTarget(LivingEntity value);
        @Accessor("attackTime") int skywars2$goalAttackTime();
        @Accessor("attackTime") void skywars2$goalAttackTime(int value);
    }

    @Mixin(DoorInteractGoal.class)
    public interface DoorInteractGoalAccess {
        @Accessor("mob") Mob skywars2$goalMob();
        @Accessor("mob") void skywars2$goalMob(Mob value);
        @Accessor("doorPos") BlockPos skywars2$goalDoorPos();
        @Accessor("doorPos") void skywars2$goalDoorPos(BlockPos value);
        @Accessor("hasDoor") boolean skywars2$goalHasDoor();
        @Accessor("hasDoor") void skywars2$goalHasDoor(boolean value);
        @Accessor("passed") boolean skywars2$goalPassed();
        @Accessor("passed") void skywars2$goalPassed(boolean value);
        @Accessor("doorOpenDirX") float skywars2$goalDoorOpenDirX();
        @Accessor("doorOpenDirX") void skywars2$goalDoorOpenDirX(float value);
        @Accessor("doorOpenDirZ") float skywars2$goalDoorOpenDirZ();
        @Accessor("doorOpenDirZ") void skywars2$goalDoorOpenDirZ(float value);
    }

    @Mixin(OpenDoorGoal.class)
    public interface OpenDoorGoalAccess {
        @Accessor("forgetTime") int skywars2$goalForgetTime();
        @Accessor("forgetTime") void skywars2$goalForgetTime(int value);
    }

    @Mixin(BreakDoorGoal.class)
    public interface BreakDoorGoalAccess {
        @Accessor("breakTime") int skywars2$goalBreakTime();
        @Accessor("breakTime") void skywars2$goalBreakTime(int value);
        @Accessor("lastBreakProgress") int skywars2$goalLastBreakProgress();
        @Accessor("lastBreakProgress") void skywars2$goalLastBreakProgress(int value);
        @Accessor("doorBreakTime") int skywars2$goalDoorBreakTime();
        @Accessor("doorBreakTime") void skywars2$goalDoorBreakTime(int value);
    }

    @Mixin(RemoveBlockGoal.class)
    public interface RemoveBlockGoalAccess {
        @Accessor("ticksSinceReachedGoal") int skywars2$goalTicksSinceReachedGoal();
        @Accessor("ticksSinceReachedGoal") void skywars2$goalTicksSinceReachedGoal(int value);
    }

    @Mixin(PathfindToRaidGoal.class)
    public interface PathfindToRaidGoalAccess {
        @Accessor("recruitmentTick") int skywars2$goalRecruitmentTick();
        @Accessor("recruitmentTick") void skywars2$goalRecruitmentTick(int value);
    }

    @Mixin(MoveToBlockGoal.class)
    public interface MoveToBlockGoalAccess {
        @Accessor("nextStartTick") int skywars2$goalNextStartTick();
        @Accessor("nextStartTick") void skywars2$goalNextStartTick(int value);
        @Accessor("tryTicks") int skywars2$goalTryTicks();
        @Accessor("tryTicks") void skywars2$goalTryTicks(int value);
        @Accessor("maxStayTicks") int skywars2$goalMaxStayTicks();
        @Accessor("maxStayTicks") void skywars2$goalMaxStayTicks(int value);
        @Accessor("blockPos") BlockPos skywars2$goalBlockPos();
        @Accessor("blockPos") void skywars2$goalBlockPos(BlockPos value);
        @Accessor("reachedTarget") boolean skywars2$goalReachedTarget();
        @Accessor("reachedTarget") void skywars2$goalReachedTarget(boolean value);
        @Accessor("verticalSearchStart") int skywars2$goalVerticalSearchStart();
        @Accessor("verticalSearchStart") void skywars2$goalVerticalSearchStart(int value);
    }

    @Mixin(MoveThroughVillageGoal.class)
    public interface MoveThroughVillageGoalAccess {
        @Accessor("path") Path skywars2$goalPath();
        @Accessor("path") void skywars2$goalPath(Path value);
        @Accessor("poiPos") BlockPos skywars2$goalPoiPos();
        @Accessor("poiPos") void skywars2$goalPoiPos(BlockPos value);
        @Accessor("visited") List<BlockPos> skywars2$goalVisited();
    }

    @Mixin(OwnerHurtByTargetGoal.class)
    public interface OwnerHurtByTargetGoalAccess {
        @Accessor("ownerLastHurtBy") LivingEntity skywars2$goalOwnerLastHurtBy();
        @Accessor("ownerLastHurtBy") void skywars2$goalOwnerLastHurtBy(LivingEntity value);
        @Accessor("timestamp") int skywars2$goalTimestamp();
        @Accessor("timestamp") void skywars2$goalTimestamp(int value);
    }

    @Mixin(OwnerHurtTargetGoal.class)
    public interface OwnerHurtTargetGoalAccess {
        @Accessor("ownerLastHurt") LivingEntity skywars2$goalOwnerLastHurt();
        @Accessor("ownerLastHurt") void skywars2$goalOwnerLastHurt(LivingEntity value);
        @Accessor("timestamp") int skywars2$goalTimestamp();
        @Accessor("timestamp") void skywars2$goalTimestamp(int value);
    }

    @Mixin(ResetUniversalAngerTargetGoal.class)
    public interface ResetUniversalAngerTargetGoalAccess {
        @Accessor("lastHurtByPlayerTimestamp") int skywars2$goalLastHurtByPlayerTimestamp();
        @Accessor("lastHurtByPlayerTimestamp") void skywars2$goalLastHurtByPlayerTimestamp(int value);
    }

    @Mixin(NearestHealableRaiderTargetGoal.class)
    public interface NearestHealableRaiderTargetGoalAccess {
        @Accessor("cooldown") int skywars2$goalCooldown();
        @Accessor("cooldown") void skywars2$goalCooldown(int value);
    }

    @Mixin(NearestAttackableWitchTargetGoal.class)
    public interface NearestAttackableWitchTargetGoalAccess {
        @Accessor("canAttack") boolean skywars2$goalCanAttack();
        @Accessor("canAttack") void skywars2$goalCanAttack(boolean value);
    }

    @Mixin(FollowFlockLeaderGoal.class)
    public interface FollowFlockLeaderGoalAccess {
        @Accessor("timeToRecalcPath") int skywars2$goalTimeToRecalcPath();
        @Accessor("timeToRecalcPath") void skywars2$goalTimeToRecalcPath(int value);
        @Accessor("nextStartTick") int skywars2$goalNextStartTick();
        @Accessor("nextStartTick") void skywars2$goalNextStartTick(int value);
    }

    @Mixin(BegGoal.class)
    public interface BegGoalAccess {
        @Accessor("player") Player skywars2$goalPlayer();
        @Accessor("player") void skywars2$goalPlayer(Player value);
        @Accessor("lookTime") int skywars2$goalLookTime();
        @Accessor("lookTime") void skywars2$goalLookTime(int value);
    }

    @Mixin(DolphinJumpGoal.class)
    public interface DolphinJumpGoalAccess {
        @Accessor("breached") boolean skywars2$goalBreached();
        @Accessor("breached") void skywars2$goalBreached(boolean value);
    }

    @Mixin(FollowPlayerRiddenEntityGoal.class)
    public interface FollowPlayerRiddenEntityGoalAccess {
        @Accessor("timeToRecalcPath") int skywars2$goalTimeToRecalcPath();
        @Accessor("timeToRecalcPath") void skywars2$goalTimeToRecalcPath(int value);
        @Accessor("following") Player skywars2$goalFollowing();
        @Accessor("following") void skywars2$goalFollowing(Player value);
        @Accessor("currentGoal") FollowPlayerRiddenEntityGoal.FollowEntityGoal skywars2$goalCurrentGoal();
        @Accessor("currentGoal") void skywars2$goalCurrentGoal(FollowPlayerRiddenEntityGoal.FollowEntityGoal value);
    }

    @Mixin(ZombieAttackGoal.class)
    public interface ZombieAttackGoalAccess {
        @Accessor("raiseArmTicks") int skywars2$goalRaiseArmTicks();
        @Accessor("raiseArmTicks") void skywars2$goalRaiseArmTicks(int value);
    }

    @Mixin(LlamaFollowCaravanGoal.class)
    public interface LlamaFollowCaravanGoalAccess {
        @Accessor("speedModifier") double skywars2$goalSpeedModifier();
        @Accessor("speedModifier") void skywars2$goalSpeedModifier(double value);
        @Accessor("distCheckCounter") int skywars2$goalDistCheckCounter();
        @Accessor("distCheckCounter") void skywars2$goalDistCheckCounter(int value);
    }

    @Mixin(OfferFlowerGoal.class)
    public interface OfferFlowerGoalAccess {
        @Accessor("entity") LivingEntity skywars2$goalEntity();
        @Accessor("entity") void skywars2$goalEntity(LivingEntity value);
        @Accessor("tick") int skywars2$goalTick();
        @Accessor("tick") void skywars2$goalTick(int value);
    }

    @Mixin(RandomStandGoal.class)
    public interface RandomStandGoalAccess {
        @Accessor("nextStand") int skywars2$goalNextStand();
        @Accessor("nextStand") void skywars2$goalNextStand(int value);
    }

    @Mixin(RunAroundLikeCrazyGoal.class)
    public interface RunAroundLikeCrazyGoalAccess {
        @Accessor("posX") double skywars2$goalPosX();
        @Accessor("posX") void skywars2$goalPosX(double value);
        @Accessor("posY") double skywars2$goalPosY();
        @Accessor("posY") void skywars2$goalPosY(double value);
        @Accessor("posZ") double skywars2$goalPosZ();
        @Accessor("posZ") void skywars2$goalPosZ(double value);
    }

    @Mixin(LandOnOwnersShoulderGoal.class)
    public interface LandOnOwnersShoulderGoalAccess {
        @Accessor("isSittingOnShoulder") boolean skywars2$goalIsSittingOnShoulder();
        @Accessor("isSittingOnShoulder") void skywars2$goalIsSittingOnShoulder(boolean value);
    }

    @Mixin(DefendVillageTargetGoal.class)
    public interface DefendVillageTargetGoalAccess {
        @Accessor("potentialTarget") LivingEntity skywars2$goalPotentialTarget();
        @Accessor("potentialTarget") void skywars2$goalPotentialTarget(LivingEntity value);
    }

    @Mixin(Mob.class)
    public interface MobAccess {
        @Accessor("goalSelector") GoalSelector skywars2$goalGoalSelector();
        @Accessor("targetSelector") GoalSelector skywars2$goalTargetSelector();
    }

    @Mixin(GoalSelector.class)
    public interface GoalSelectorAccess {
        @Accessor("lockedFlags") Map<Goal.Flag, WrappedGoal> skywars2$goalLockedFlags();
        @Accessor("disabledFlags") EnumSet<Goal.Flag> skywars2$goalDisabledFlags();
    }

    @Mixin(WrappedGoal.class)
    public interface WrappedGoalAccess {
        @Accessor("isRunning") boolean skywars2$goalIsRunning();
        @Accessor("isRunning") void skywars2$goalIsRunning(boolean value);
    }

    @Mixin(MeleeAttackGoal.class)
    public interface MeleeAttackGoalAccess {
        @Accessor("path") Path skywars2$goalPath();
        @Accessor("path") void skywars2$goalPath(Path value);
        @Accessor("pathedTargetX") double skywars2$goalPathedTargetX();
        @Accessor("pathedTargetX") void skywars2$goalPathedTargetX(double value);
        @Accessor("pathedTargetY") double skywars2$goalPathedTargetY();
        @Accessor("pathedTargetY") void skywars2$goalPathedTargetY(double value);
        @Accessor("pathedTargetZ") double skywars2$goalPathedTargetZ();
        @Accessor("pathedTargetZ") void skywars2$goalPathedTargetZ(double value);
        @Accessor("ticksUntilNextPathRecalculation") int skywars2$goalTicksUntilNextPathRecalculation();
        @Accessor("ticksUntilNextPathRecalculation") void skywars2$goalTicksUntilNextPathRecalculation(int value);
        @Accessor("ticksUntilNextAttack") int skywars2$goalTicksUntilNextAttack();
        @Accessor("ticksUntilNextAttack") void skywars2$goalTicksUntilNextAttack(int value);
        @Accessor("lastCanUseCheck") long skywars2$goalLastCanUseCheck();
        @Accessor("lastCanUseCheck") void skywars2$goalLastCanUseCheck(long value);
    }

    @Mixin(RangedAttackGoal.class)
    public interface RangedAttackGoalAccess {
        @Accessor("target") LivingEntity skywars2$goalTarget();
        @Accessor("target") void skywars2$goalTarget(LivingEntity value);
        @Accessor("attackTime") int skywars2$goalAttackTime();
        @Accessor("attackTime") void skywars2$goalAttackTime(int value);
        @Accessor("seeTime") int skywars2$goalSeeTime();
        @Accessor("seeTime") void skywars2$goalSeeTime(int value);
    }

    @Mixin(TargetGoal.class)
    public interface TargetGoalAccess {
        @Accessor("reachCache") int skywars2$goalReachCache();
        @Accessor("reachCache") void skywars2$goalReachCache(int value);
        @Accessor("reachCacheTime") int skywars2$goalReachCacheTime();
        @Accessor("reachCacheTime") void skywars2$goalReachCacheTime(int value);
        @Accessor("unseenTicks") int skywars2$goalUnseenTicks();
        @Accessor("unseenTicks") void skywars2$goalUnseenTicks(int value);
        @Accessor("unseenMemoryTicks") int skywars2$goalUnseenMemoryTicks();
        @Accessor("unseenMemoryTicks") void skywars2$goalUnseenMemoryTicks(int value);
        @Accessor("targetMob") LivingEntity skywars2$goalTargetMob();
        @Accessor("targetMob") void skywars2$goalTargetMob(LivingEntity value);
    }

    @Mixin(NearestAttackableTargetGoal.class)
    public interface NearestAttackableTargetGoalAccess {
        @Accessor("target") LivingEntity skywars2$goalTarget();
        @Accessor("target") void skywars2$goalTarget(LivingEntity value);
    }

    @Mixin(HurtByTargetGoal.class)
    public interface HurtByTargetGoalAccess {
        @Accessor("timestamp") int skywars2$goalTimestamp();
        @Accessor("timestamp") void skywars2$goalTimestamp(int value);
        @Accessor("alertSameType") boolean skywars2$goalAlertSameType();
        @Accessor("alertSameType") void skywars2$goalAlertSameType(boolean value);
        @Accessor("toIgnoreDamage") Class<?>[] skywars2$goalToIgnoreDamage();
        @Accessor("toIgnoreAlert") Class<?>[] skywars2$goalToIgnoreAlert();
        @Accessor("toIgnoreAlert") void skywars2$goalToIgnoreAlert(Class<?>[] value);
    }

    @Mixin(BreedGoal.class)
    public interface BreedGoalAccess {
        @Accessor("partner") Animal skywars2$goalPartner();
        @Accessor("partner") void skywars2$goalPartner(Animal value);
        @Accessor("loveTime") int skywars2$goalLoveTime();
        @Accessor("loveTime") void skywars2$goalLoveTime(int value);
    }
}
