package me.skywars2.game.entities.mob_ai;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import me.skywars2.mixin.mobs.RetainedGoalCheckpointAccess;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.animal.equine.SkeletonHorse;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.feline.Ocelot;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.animal.panda.Panda;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;

/** Reviewed native task reference slots, including tasks currently absent from either selector. */
final class MatchRetainedGoals {
    private final List<Runnable> restores = new ArrayList<>();

    MatchRetainedGoals(Mob owner, Consumer<Goal> capture) {
        if (owner instanceof Bee) {
            var access = (RetainedGoalCheckpointAccess.BeeAccess)(Object) owner;
            save(access::skywars2$retainedBeePollinateGoal, access::skywars2$retainedBeePollinateGoal, capture);
            save(access::skywars2$retainedGoToHiveGoal, access::skywars2$retainedGoToHiveGoal, capture);
            save(access::skywars2$retainedGoToKnownFlowerGoal, access::skywars2$retainedGoToKnownFlowerGoal, capture);
        }
        if (owner instanceof SkeletonHorse) {
            var access = (RetainedGoalCheckpointAccess.SkeletonHorseAccess)(Object) owner;
            saveFinal(access::skywars2$retainedSkeletonTrapGoal, capture, "SkeletonHorse.skeletonTrapGoal");
        }
        if (owner instanceof Cat) {
            var access = (RetainedGoalCheckpointAccess.CatAccess)(Object) owner;
            save(access::skywars2$retainedAvoidPlayersGoal, access::skywars2$retainedAvoidPlayersGoal, capture);
            save(access::skywars2$retainedTemptGoal, access::skywars2$retainedTemptGoal, capture);
        }
        if (owner instanceof Ocelot) {
            var access = (RetainedGoalCheckpointAccess.OcelotAccess)(Object) owner;
            save(access::skywars2$retainedOcelotAvoidPlayersGoal, access::skywars2$retainedOcelotAvoidPlayersGoal, capture);
            save(access::skywars2$retainedTemptGoal, access::skywars2$retainedTemptGoal, capture);
        }
        if (owner instanceof Fox) {
            var access = (RetainedGoalCheckpointAccess.FoxAccess)(Object) owner;
            save(access::skywars2$retainedLandTargetGoal, access::skywars2$retainedLandTargetGoal, capture);
            save(access::skywars2$retainedTurtleEggTargetGoal, access::skywars2$retainedTurtleEggTargetGoal, capture);
            save(access::skywars2$retainedFishTargetGoal, access::skywars2$retainedFishTargetGoal, capture);
        }
        if (owner instanceof Panda) {
            var access = (RetainedGoalCheckpointAccess.PandaAccess)(Object) owner;
            save(access::skywars2$retainedLookAtPlayerGoal, access::skywars2$retainedLookAtPlayerGoal, capture);
        }
        if (owner instanceof Sheep) {
            var access = (RetainedGoalCheckpointAccess.SheepAccess)(Object) owner;
            save(access::skywars2$retainedEatBlockGoal, access::skywars2$retainedEatBlockGoal, capture);
        }
        if (owner instanceof Guardian) {
            var access = (RetainedGoalCheckpointAccess.GuardianAccess)(Object) owner;
            save(access::skywars2$retainedRandomStrollGoal, access::skywars2$retainedRandomStrollGoal, capture);
        }
        if (owner instanceof Silverfish) {
            var access = (RetainedGoalCheckpointAccess.SilverfishAccess)(Object) owner;
            save(access::skywars2$retainedFriendsGoal, access::skywars2$retainedFriendsGoal, capture);
        }
        if (owner instanceof Witch) {
            var access = (RetainedGoalCheckpointAccess.WitchAccess)(Object) owner;
            save(access::skywars2$retainedHealRaidersGoal, access::skywars2$retainedHealRaidersGoal, capture);
            save(access::skywars2$retainedAttackPlayersGoal, access::skywars2$retainedAttackPlayersGoal, capture);
        }
        if (owner instanceof Strider) {
            var access = (RetainedGoalCheckpointAccess.StriderAccess)(Object) owner;
            save(access::skywars2$retainedTemptGoal, access::skywars2$retainedTemptGoal, capture);
        }
        if (owner instanceof AbstractSkeleton) {
            var access = (RetainedGoalCheckpointAccess.AbstractSkeletonAccess)(Object) owner;
            saveFinal(access::skywars2$retainedBowGoal, capture, "AbstractSkeleton.bowGoal");
            saveFinal(access::skywars2$retainedMeleeGoal, capture, "AbstractSkeleton.meleeGoal");
        }
        if (owner instanceof Zombie) {
            var access = (RetainedGoalCheckpointAccess.ZombieAccess)(Object) owner;
            saveFinal(access::skywars2$retainedBreakDoorGoal, capture, "Zombie.breakDoorGoal");
        }
    }

    private <T extends Goal> void save(Supplier<T> read, Consumer<T> write, Consumer<Goal> capture) {
        T original = read.get();
        restores.add(() -> {
            if (read.get() != original) write.accept(original);
        });
        if (original != null) capture.accept(original);
    }

    private <T extends Goal> void saveFinal(Supplier<T> read, Consumer<Goal> capture, String label) {
        T original = read.get();
        restores.add(() -> {
            if (read.get() != original) throw new IllegalStateException("Final native task identity changed: " + label);
        });
        if (original != null) capture.accept(original);
    }

    void restore() {
        restores.forEach(Runnable::run);
    }
}
