package me.skywars2.game;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import me.skywars2.mixin.EntityRuntimeCheckpointAccess;
import me.skywars2.mixin.MobGoalCheckpointAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.animal.fish.AbstractSchoolingFish;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

/** Explicit native task schemas that preserve cached bodies, mutable positions and path aliases. */
final class MatchGoalFields {
    private final Mob owner;
    final Goal goal;
    boolean wasRunning;
    private final List<Runnable> values = new ArrayList<>();
    private final List<Reference<?>> references = new ArrayList<>();
    private final Map<Path,MatchMobNavigation.Route> routes = new IdentityHashMap<>();
    private final Map<BlockPos.MutableBlockPos,BlockPos> positions = new IdentityHashMap<>();
    private final Map<List<BlockPos>,List<BlockPos>> positionLists = new IdentityHashMap<>();
    private boolean referencesCaptured;
    private final List<Entity> dependencies = new ArrayList<>();
    private List<MatchDamageSources.Ref> dependencyRefs = List.of();
    private float oldWaterCost;

    static MatchGoalFields capture(Mob owner, Goal goal) {
        var image = new MatchGoalFields(owner, goal);
        if (goal instanceof LookAtPlayerGoal) {
            var access = (MobGoalCheckpointAccess.LookAtPlayerGoalAccess)(Object) goal;
            image.saveReference(access.skywars2$goalLookAt(), body -> body, access::skywars2$goalLookAt);
            image.save(access.skywars2$goalLookTime(), access::skywars2$goalLookTime);
        }
        if (goal instanceof RandomLookAroundGoal) {
            var access = (MobGoalCheckpointAccess.RandomLookAroundGoalAccess)(Object) goal;
            image.save(access.skywars2$goalRelX(), access::skywars2$goalRelX);
            image.save(access.skywars2$goalRelZ(), access::skywars2$goalRelZ);
            image.save(access.skywars2$goalLookTime(), access::skywars2$goalLookTime);
        }
        if (goal instanceof FollowParentGoal) {
            var access = (MobGoalCheckpointAccess.FollowParentGoalAccess)(Object) goal;
            image.saveReference(access.skywars2$goalParent(), body -> (Animal) body, access::skywars2$goalParent);
            image.save(access.skywars2$goalTimeToRecalcPath(), access::skywars2$goalTimeToRecalcPath);
        }
        if (goal instanceof FollowOwnerGoal) {
            var access = (MobGoalCheckpointAccess.FollowOwnerGoalAccess)(Object) goal;
            image.saveReference(access.skywars2$goalOwner(), body -> (LivingEntity) body, access::skywars2$goalOwner);
            image.save(access.skywars2$goalTimeToRecalcPath(), access::skywars2$goalTimeToRecalcPath);
            image.oldWaterCost = access.skywars2$goalOldWaterCost();
            image.save(image.oldWaterCost, access::skywars2$goalOldWaterCost);
        }
        if (goal instanceof FollowMobGoal) {
            var access = (MobGoalCheckpointAccess.FollowMobGoalAccess)(Object) goal;
            image.saveReference(access.skywars2$goalFollowingMob(), body -> (Mob) body, access::skywars2$goalFollowingMob);
            image.save(access.skywars2$goalTimeToRecalcPath(), access::skywars2$goalTimeToRecalcPath);
            image.oldWaterCost = access.skywars2$goalOldWaterCost();
            image.save(image.oldWaterCost, access::skywars2$goalOldWaterCost);
        }
        if (goal instanceof TemptGoal) {
            var access = (MobGoalCheckpointAccess.TemptGoalAccess)(Object) goal;
            image.save(access.skywars2$goalPx(), access::skywars2$goalPx);
            image.save(access.skywars2$goalPy(), access::skywars2$goalPy);
            image.save(access.skywars2$goalPz(), access::skywars2$goalPz);
            image.save(access.skywars2$goalPRotX(), access::skywars2$goalPRotX);
            image.save(access.skywars2$goalPRotY(), access::skywars2$goalPRotY);
            image.saveReference(access.skywars2$goalPlayer(), body -> (Player) body, access::skywars2$goalPlayer);
            image.save(access.skywars2$goalCalmDown(), access::skywars2$goalCalmDown);
            image.save(access.skywars2$goalIsRunning(), access::skywars2$goalIsRunning);
        }
        if (goal instanceof AvoidEntityGoal<?>) {
            var access = (MobGoalCheckpointAccess.AvoidEntityGoalAccess)(Object) goal;
            image.saveReference(access.skywars2$goalToAvoid(), body -> (LivingEntity) body, access::skywars2$goalToAvoid);
            image.save(access.skywars2$goalPath(), access::skywars2$goalPath);
        }
        if (goal instanceof RangedBowAttackGoal<?>) {
            var access = (MobGoalCheckpointAccess.RangedBowAttackGoalAccess)(Object) goal;
            image.save(access.skywars2$goalAttackIntervalMin(), access::skywars2$goalAttackIntervalMin);
            image.save(access.skywars2$goalAttackTime(), access::skywars2$goalAttackTime);
            image.save(access.skywars2$goalSeeTime(), access::skywars2$goalSeeTime);
            image.save(access.skywars2$goalStrafingClockwise(), access::skywars2$goalStrafingClockwise);
            image.save(access.skywars2$goalStrafingBackwards(), access::skywars2$goalStrafingBackwards);
            image.save(access.skywars2$goalStrafingTime(), access::skywars2$goalStrafingTime);
        }
        if (goal instanceof RangedCrossbowAttackGoal<?>) {
            var access = (MobGoalCheckpointAccess.RangedCrossbowAttackGoalAccess)(Object) goal;
            image.save(access.skywars2$goalCrossbowState(), access::skywars2$goalCrossbowState);
            image.save(access.skywars2$goalSeeTime(), access::skywars2$goalSeeTime);
            image.save(access.skywars2$goalAttackDelay(), access::skywars2$goalAttackDelay);
            image.save(access.skywars2$goalUpdatePathDelay(), access::skywars2$goalUpdatePathDelay);
        }
        if (goal instanceof RandomStrollGoal) {
            var access = (MobGoalCheckpointAccess.RandomStrollGoalAccess)(Object) goal;
            image.save(access.skywars2$goalWantedX(), access::skywars2$goalWantedX);
            image.save(access.skywars2$goalWantedY(), access::skywars2$goalWantedY);
            image.save(access.skywars2$goalWantedZ(), access::skywars2$goalWantedZ);
            image.save(access.skywars2$goalInterval(), access::skywars2$goalInterval);
            image.save(access.skywars2$goalForceTrigger(), access::skywars2$goalForceTrigger);
        }
        if (goal instanceof PanicGoal) {
            var access = (MobGoalCheckpointAccess.PanicGoalAccess)(Object) goal;
            image.save(access.skywars2$goalPosX(), access::skywars2$goalPosX);
            image.save(access.skywars2$goalPosY(), access::skywars2$goalPosY);
            image.save(access.skywars2$goalPosZ(), access::skywars2$goalPosZ);
            image.save(access.skywars2$goalIsRunning(), access::skywars2$goalIsRunning);
        }
        if (goal instanceof FleeSunGoal) {
            var access = (MobGoalCheckpointAccess.FleeSunGoalAccess)(Object) goal;
            image.save(access.skywars2$goalWantedX(), access::skywars2$goalWantedX);
            image.save(access.skywars2$goalWantedY(), access::skywars2$goalWantedY);
            image.save(access.skywars2$goalWantedZ(), access::skywars2$goalWantedZ);
        }
        if (goal instanceof MoveTowardsTargetGoal) {
            var access = (MobGoalCheckpointAccess.MoveTowardsTargetGoalAccess)(Object) goal;
            image.saveReference(access.skywars2$goalTarget(), body -> (LivingEntity) body, access::skywars2$goalTarget);
            image.save(access.skywars2$goalWantedX(), access::skywars2$goalWantedX);
            image.save(access.skywars2$goalWantedY(), access::skywars2$goalWantedY);
            image.save(access.skywars2$goalWantedZ(), access::skywars2$goalWantedZ);
        }
        if (goal instanceof MoveTowardsRestrictionGoal) {
            var access = (MobGoalCheckpointAccess.MoveTowardsRestrictionGoalAccess)(Object) goal;
            image.save(access.skywars2$goalWantedX(), access::skywars2$goalWantedX);
            image.save(access.skywars2$goalWantedY(), access::skywars2$goalWantedY);
            image.save(access.skywars2$goalWantedZ(), access::skywars2$goalWantedZ);
        }
        if (goal instanceof LeapAtTargetGoal) {
            var access = (MobGoalCheckpointAccess.LeapAtTargetGoalAccess)(Object) goal;
            image.saveReference(access.skywars2$goalTarget(), body -> (LivingEntity) body, access::skywars2$goalTarget);
        }
        if (goal instanceof SwellGoal) {
            var access = (MobGoalCheckpointAccess.SwellGoalAccess)(Object) goal;
            image.saveReference(access.skywars2$goalTarget(), body -> (LivingEntity) body, access::skywars2$goalTarget);
        }
        if (goal instanceof EatBlockGoal) {
            var access = (MobGoalCheckpointAccess.EatBlockGoalAccess)(Object) goal;
            image.save(access.skywars2$goalEatAnimationTick(), access::skywars2$goalEatAnimationTick);
        }
        if (goal instanceof OcelotAttackGoal) {
            var access = (MobGoalCheckpointAccess.OcelotAttackGoalAccess)(Object) goal;
            image.saveReference(access.skywars2$goalTarget(), body -> (LivingEntity) body, access::skywars2$goalTarget);
            image.save(access.skywars2$goalAttackTime(), access::skywars2$goalAttackTime);
        }
        if (goal instanceof DoorInteractGoal) {
            var access = (MobGoalCheckpointAccess.DoorInteractGoalAccess)(Object) goal;
            image.saveReference(access.skywars2$goalMob(), body -> (Mob) body, access::skywars2$goalMob);
            image.save(access.skywars2$goalDoorPos(), access::skywars2$goalDoorPos);
            image.save(access.skywars2$goalHasDoor(), access::skywars2$goalHasDoor);
            image.save(access.skywars2$goalPassed(), access::skywars2$goalPassed);
            image.save(access.skywars2$goalDoorOpenDirX(), access::skywars2$goalDoorOpenDirX);
            image.save(access.skywars2$goalDoorOpenDirZ(), access::skywars2$goalDoorOpenDirZ);
        }
        if (goal instanceof OpenDoorGoal) {
            var access = (MobGoalCheckpointAccess.OpenDoorGoalAccess)(Object) goal;
            image.save(access.skywars2$goalForgetTime(), access::skywars2$goalForgetTime);
        }
        if (goal instanceof BreakDoorGoal) {
            var access = (MobGoalCheckpointAccess.BreakDoorGoalAccess)(Object) goal;
            image.save(access.skywars2$goalBreakTime(), access::skywars2$goalBreakTime);
            image.save(access.skywars2$goalLastBreakProgress(), access::skywars2$goalLastBreakProgress);
            image.save(access.skywars2$goalDoorBreakTime(), access::skywars2$goalDoorBreakTime);
        }
        if (goal instanceof RemoveBlockGoal) {
            var access = (MobGoalCheckpointAccess.RemoveBlockGoalAccess)(Object) goal;
            image.save(access.skywars2$goalTicksSinceReachedGoal(), access::skywars2$goalTicksSinceReachedGoal);
        }
        if (goal instanceof PathfindToRaidGoal<?>) {
            var access = (MobGoalCheckpointAccess.PathfindToRaidGoalAccess)(Object) goal;
            image.save(access.skywars2$goalRecruitmentTick(), access::skywars2$goalRecruitmentTick);
        }
        if (goal instanceof MoveToBlockGoal) {
            var access = (MobGoalCheckpointAccess.MoveToBlockGoalAccess)(Object) goal;
            image.save(access.skywars2$goalNextStartTick(), access::skywars2$goalNextStartTick);
            image.save(access.skywars2$goalTryTicks(), access::skywars2$goalTryTicks);
            image.save(access.skywars2$goalMaxStayTicks(), access::skywars2$goalMaxStayTicks);
            image.save(access.skywars2$goalBlockPos(), access::skywars2$goalBlockPos);
            image.save(access.skywars2$goalReachedTarget(), access::skywars2$goalReachedTarget);
            image.save(access.skywars2$goalVerticalSearchStart(), access::skywars2$goalVerticalSearchStart);
        }
        if (goal instanceof MoveThroughVillageGoal) {
            var access = (MobGoalCheckpointAccess.MoveThroughVillageGoalAccess)(Object) goal;
            image.save(access.skywars2$goalPath(), access::skywars2$goalPath);
            image.save(access.skywars2$goalPoiPos(), access::skywars2$goalPoiPos);
            image.savePositions(access.skywars2$goalVisited(), access::skywars2$goalVisited);
        }
        if (goal instanceof OwnerHurtByTargetGoal) {
            var access = (MobGoalCheckpointAccess.OwnerHurtByTargetGoalAccess)(Object) goal;
            image.saveReference(access.skywars2$goalOwnerLastHurtBy(), body -> (LivingEntity) body, access::skywars2$goalOwnerLastHurtBy);
            image.save(access.skywars2$goalTimestamp(), access::skywars2$goalTimestamp);
        }
        if (goal instanceof OwnerHurtTargetGoal) {
            var access = (MobGoalCheckpointAccess.OwnerHurtTargetGoalAccess)(Object) goal;
            image.saveReference(access.skywars2$goalOwnerLastHurt(), body -> (LivingEntity) body, access::skywars2$goalOwnerLastHurt);
            image.save(access.skywars2$goalTimestamp(), access::skywars2$goalTimestamp);
        }
        if (goal instanceof ResetUniversalAngerTargetGoal<?>) {
            var access = (MobGoalCheckpointAccess.ResetUniversalAngerTargetGoalAccess)(Object) goal;
            image.save(access.skywars2$goalLastHurtByPlayerTimestamp(), access::skywars2$goalLastHurtByPlayerTimestamp);
        }
        if (goal instanceof NearestHealableRaiderTargetGoal<?>) {
            var access = (MobGoalCheckpointAccess.NearestHealableRaiderTargetGoalAccess)(Object) goal;
            image.save(access.skywars2$goalCooldown(), access::skywars2$goalCooldown);
        }
        if (goal instanceof NearestAttackableWitchTargetGoal<?>) {
            var access = (MobGoalCheckpointAccess.NearestAttackableWitchTargetGoalAccess)(Object) goal;
            image.save(access.skywars2$goalCanAttack(), access::skywars2$goalCanAttack);
        }
        if (goal instanceof FollowFlockLeaderGoal) {
            var access = (MobGoalCheckpointAccess.FollowFlockLeaderGoalAccess)(Object) goal;
            image.save(access.skywars2$goalTimeToRecalcPath(), access::skywars2$goalTimeToRecalcPath);
            image.save(access.skywars2$goalNextStartTick(), access::skywars2$goalNextStartTick);
        }
        if (goal instanceof BegGoal) {
            var access = (MobGoalCheckpointAccess.BegGoalAccess)(Object) goal;
            image.saveReference(access.skywars2$goalPlayer(), body -> (Player) body, access::skywars2$goalPlayer);
            image.save(access.skywars2$goalLookTime(), access::skywars2$goalLookTime);
        }
        if (goal instanceof DolphinJumpGoal) {
            var access = (MobGoalCheckpointAccess.DolphinJumpGoalAccess)(Object) goal;
            image.save(access.skywars2$goalBreached(), access::skywars2$goalBreached);
        }
        if (goal instanceof FollowPlayerRiddenEntityGoal) {
            var access = (MobGoalCheckpointAccess.FollowPlayerRiddenEntityGoalAccess)(Object) goal;
            image.save(access.skywars2$goalTimeToRecalcPath(), access::skywars2$goalTimeToRecalcPath);
            image.saveReference(access.skywars2$goalFollowing(), body -> (Player) body, access::skywars2$goalFollowing);
            image.save(access.skywars2$goalCurrentGoal(), access::skywars2$goalCurrentGoal);
        }
        if (goal instanceof ZombieAttackGoal) {
            var access = (MobGoalCheckpointAccess.ZombieAttackGoalAccess)(Object) goal;
            image.save(access.skywars2$goalRaiseArmTicks(), access::skywars2$goalRaiseArmTicks);
        }
        if (goal instanceof LlamaFollowCaravanGoal) {
            var access = (MobGoalCheckpointAccess.LlamaFollowCaravanGoalAccess)(Object) goal;
            image.save(access.skywars2$goalSpeedModifier(), access::skywars2$goalSpeedModifier);
            image.save(access.skywars2$goalDistCheckCounter(), access::skywars2$goalDistCheckCounter);
        }
        if (goal instanceof OfferFlowerGoal) {
            var access = (MobGoalCheckpointAccess.OfferFlowerGoalAccess)(Object) goal;
            image.saveReference(access.skywars2$goalEntity(), body -> (LivingEntity) body, access::skywars2$goalEntity);
            image.save(access.skywars2$goalTick(), access::skywars2$goalTick);
        }
        if (goal instanceof RandomStandGoal) {
            var access = (MobGoalCheckpointAccess.RandomStandGoalAccess)(Object) goal;
            image.save(access.skywars2$goalNextStand(), access::skywars2$goalNextStand);
        }
        if (goal instanceof RunAroundLikeCrazyGoal) {
            var access = (MobGoalCheckpointAccess.RunAroundLikeCrazyGoalAccess)(Object) goal;
            image.save(access.skywars2$goalPosX(), access::skywars2$goalPosX);
            image.save(access.skywars2$goalPosY(), access::skywars2$goalPosY);
            image.save(access.skywars2$goalPosZ(), access::skywars2$goalPosZ);
        }
        if (goal instanceof LandOnOwnersShoulderGoal) {
            var access = (MobGoalCheckpointAccess.LandOnOwnersShoulderGoalAccess)(Object) goal;
            image.save(access.skywars2$goalIsSittingOnShoulder(), access::skywars2$goalIsSittingOnShoulder);
        }
        if (goal instanceof DefendVillageTargetGoal) {
            var access = (MobGoalCheckpointAccess.DefendVillageTargetGoalAccess)(Object) goal;
            image.saveReference(access.skywars2$goalPotentialTarget(), body -> (LivingEntity) body, access::skywars2$goalPotentialTarget);
        }
        return image.values.isEmpty() ? null : image;
    }

    private MatchGoalFields(Mob owner, Goal goal) {
        this.owner = owner;
        this.goal = goal;
        if (goal instanceof FollowFlockLeaderGoal && owner instanceof AbstractSchoolingFish fish) {
            var leader = MatchFishSchools.leader(fish);
            if (leader != null) dependencies.add(leader);
        }
        if (goal instanceof LlamaFollowCaravanGoal && owner instanceof Llama llama) {
            var seen = Collections.newSetFromMap(new IdentityHashMap<Entity,Boolean>());
            for (var head = llama.getCaravanHead(); head != null && dependencies.size() < 9 && seen.add(head); head = head.getCaravanHead())
                dependencies.add(head);
        }
    }

    private <T> void save(T value, Consumer<T> write) {
        if (value instanceof BlockPos pos) position(pos);
        if (value instanceof Path path) routes.computeIfAbsent(path,MatchMobNavigation.Route::new);
        if (value instanceof Vec3 && value.getClass() != Vec3.class)
            throw new IllegalStateException("Review mutable native task vector subtype");
        values.add(() -> write.accept(value));
    }

    private <T extends Entity> void saveReference(T body, Function<Entity,T> narrow, Consumer<T> write) {
        save(body,write);
        references.add(new Reference<>(body,narrow,write));
    }

    private void position(BlockPos value) {
        if (value instanceof BlockPos.MutableBlockPos mutable) positions.computeIfAbsent(mutable,BlockPos::immutable);
        else if (value.getClass() != BlockPos.class) throw new IllegalStateException("Review native position subtype: "+value.getClass());
    }

    private void savePositions(List<BlockPos> original, Supplier<List<BlockPos>> live) {
        for (Object item : original) {
            if (item instanceof BlockPos pos) position(pos);
            else if (item != null) throw new IllegalStateException("Review native position history element");
        }
        positionLists.put(original,Collections.unmodifiableList(new ArrayList<>(original)));
        values.add(() -> {
            if (live.get() != original) throw new IllegalStateException("Final native task position history changed");
        });
    }

    void restore() {
        positions.forEach((position,saved) -> position.set(saved));
        positionLists.forEach((list,saved) -> { list.clear(); list.addAll(saved); });
        routes.values().forEach(MatchMobNavigation.Route::restore);
        values.forEach(Runnable::run);
    }

    void captureReferences(MatchDamageSources sources) {
        references.forEach(reference -> reference.capture(sources));
        dependencyRefs = dependencies.stream().map(sources::dependencyRef).toList();
        referencesCaptured = true;
    }

    boolean restoreReferences(Function<UUID,Entity> lookup, Set<UUID> pending) {
        if (!referencesCaptured) return false;
        boolean absent = false;
        for (var ref : dependencyRefs) {
            var body = MatchDamageSources.resolve(ref,lookup,pending);
            if (body == null && MatchDamageSources.intentionallyAbsent(ref.id())) absent = true;
        }
        for (var reference : references) absent |= reference.restore(lookup,pending);
        return absent;
    }

    private static final class Reference<T extends Entity> {
        final T body;
        final Function<Entity,T> narrow;
        final Consumer<T> write;
        MatchDamageSources.Ref saved;

        Reference(T body, Function<Entity,T> narrow, Consumer<T> write) {
            this.body = body;
            this.narrow = narrow;
            this.write = write;
        }

        void capture(MatchDamageSources sources) {
            saved = sources.dependencyRef(body);
        }

        boolean restore(Function<UUID,Entity> lookup, Set<UUID> pending) {
            var resolved = MatchDamageSources.resolve(saved,lookup,pending);
            boolean missing = saved != null && resolved == null;
            if (missing && !MatchDamageSources.intentionallyAbsent(saved.id())) return false;
            try {
                write.accept(narrow.apply(resolved));
            } catch (ClassCastException wrongBody) {
                throw new IllegalStateException("Native task reference resolved to the wrong body type",wrongBody);
            }
            return missing;
        }

        void clear() {
            write.accept(null);
        }
    }

    void absent() {
        references.forEach(Reference::clear);
        // Restore the native stop values without cancelling a different task's route.
        if (goal instanceof TemptGoal) {
            var access = (MobGoalCheckpointAccess.TemptGoalAccess)(Object) goal;
            access.skywars2$goalIsRunning(false);
            access.skywars2$goalCalmDown(50);
            if (wasRunning) {
                if (goal instanceof TemptGoal.ForNonPathfinders) owner.getMoveControl().setWait();
                else owner.getNavigation().stop();
            }
        }
        if (wasRunning && (goal instanceof FollowOwnerGoal || goal instanceof FollowMobGoal)) {
            owner.getNavigation().stop();
            owner.setPathfindingMalus(PathType.WATER,oldWaterCost);
        }
        if (wasRunning && goal instanceof OcelotAttackGoal) owner.getNavigation().stop();
        if (wasRunning && goal instanceof BegGoal) ((Wolf) owner).setIsInterested(false);
        if (wasRunning && goal instanceof LlamaFollowCaravanGoal) {
            MatchLlamaCaravans.leave((Llama) owner);
            ((MobGoalCheckpointAccess.LlamaFollowCaravanGoalAccess)(Object) goal).skywars2$goalSpeedModifier(2.1);
        }
        if (wasRunning && goal instanceof OfferFlowerGoal)
            ((EntityRuntimeCheckpointAccess.IronGolemAccess)(Object) (IronGolem) owner).skywars2$runtimeOfferFlowerTick(0);
    }
}
