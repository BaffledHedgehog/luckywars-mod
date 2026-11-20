package me.skywars2.mixin;

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
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.animal.equine.SkeletonTrapGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Native goal fields retained even while absent from a mob's selectors. */
public final class RetainedGoalCheckpointAccess {
    private RetainedGoalCheckpointAccess() { }

    @Mixin(Bee.class)
    public interface BeeAccess {
        @Accessor("beePollinateGoal") Bee.BeePollinateGoal skywars2$retainedBeePollinateGoal();
        @Accessor("beePollinateGoal") void skywars2$retainedBeePollinateGoal(Bee.BeePollinateGoal value);
        @Accessor("goToHiveGoal") Bee.BeeGoToHiveGoal skywars2$retainedGoToHiveGoal();
        @Accessor("goToHiveGoal") void skywars2$retainedGoToHiveGoal(Bee.BeeGoToHiveGoal value);
        @Accessor("goToKnownFlowerGoal") Bee.BeeGoToKnownFlowerGoal skywars2$retainedGoToKnownFlowerGoal();
        @Accessor("goToKnownFlowerGoal") void skywars2$retainedGoToKnownFlowerGoal(Bee.BeeGoToKnownFlowerGoal value);
    }

    @Mixin(SkeletonHorse.class)
    public interface SkeletonHorseAccess {
        @Accessor("skeletonTrapGoal") SkeletonTrapGoal skywars2$retainedSkeletonTrapGoal();
    }

    @Mixin(Cat.class)
    public interface CatAccess {
        @Accessor("avoidPlayersGoal") Cat.CatAvoidEntityGoal<Player> skywars2$retainedAvoidPlayersGoal();
        @Accessor("avoidPlayersGoal") void skywars2$retainedAvoidPlayersGoal(Cat.CatAvoidEntityGoal<Player> value);
        @Accessor("temptGoal") TemptGoal skywars2$retainedTemptGoal();
        @Accessor("temptGoal") void skywars2$retainedTemptGoal(TemptGoal value);
    }

    @Mixin(Ocelot.class)
    public interface OcelotAccess {
        @Accessor("ocelotAvoidPlayersGoal") Ocelot.OcelotAvoidEntityGoal<Player> skywars2$retainedOcelotAvoidPlayersGoal();
        @Accessor("ocelotAvoidPlayersGoal") void skywars2$retainedOcelotAvoidPlayersGoal(Ocelot.OcelotAvoidEntityGoal<Player> value);
        @Accessor("temptGoal") Ocelot.OcelotTemptGoal skywars2$retainedTemptGoal();
        @Accessor("temptGoal") void skywars2$retainedTemptGoal(Ocelot.OcelotTemptGoal value);
    }

    @Mixin(Fox.class)
    public interface FoxAccess {
        @Accessor("landTargetGoal") Goal skywars2$retainedLandTargetGoal();
        @Accessor("landTargetGoal") void skywars2$retainedLandTargetGoal(Goal value);
        @Accessor("turtleEggTargetGoal") Goal skywars2$retainedTurtleEggTargetGoal();
        @Accessor("turtleEggTargetGoal") void skywars2$retainedTurtleEggTargetGoal(Goal value);
        @Accessor("fishTargetGoal") Goal skywars2$retainedFishTargetGoal();
        @Accessor("fishTargetGoal") void skywars2$retainedFishTargetGoal(Goal value);
    }

    @Mixin(Panda.class)
    public interface PandaAccess {
        @Accessor("lookAtPlayerGoal") Panda.PandaLookAtPlayerGoal skywars2$retainedLookAtPlayerGoal();
        @Accessor("lookAtPlayerGoal") void skywars2$retainedLookAtPlayerGoal(Panda.PandaLookAtPlayerGoal value);
    }

    @Mixin(Sheep.class)
    public interface SheepAccess {
        @Accessor("eatBlockGoal") EatBlockGoal skywars2$retainedEatBlockGoal();
        @Accessor("eatBlockGoal") void skywars2$retainedEatBlockGoal(EatBlockGoal value);
    }

    @Mixin(Guardian.class)
    public interface GuardianAccess {
        @Accessor("randomStrollGoal") RandomStrollGoal skywars2$retainedRandomStrollGoal();
        @Accessor("randomStrollGoal") void skywars2$retainedRandomStrollGoal(RandomStrollGoal value);
    }

    @Mixin(Silverfish.class)
    public interface SilverfishAccess {
        @Accessor("friendsGoal") Silverfish.SilverfishWakeUpFriendsGoal skywars2$retainedFriendsGoal();
        @Accessor("friendsGoal") void skywars2$retainedFriendsGoal(Silverfish.SilverfishWakeUpFriendsGoal value);
    }

    @Mixin(Witch.class)
    public interface WitchAccess {
        @Accessor("healRaidersGoal") NearestHealableRaiderTargetGoal<Raider> skywars2$retainedHealRaidersGoal();
        @Accessor("healRaidersGoal") void skywars2$retainedHealRaidersGoal(NearestHealableRaiderTargetGoal<Raider> value);
        @Accessor("attackPlayersGoal") NearestAttackableWitchTargetGoal<Player> skywars2$retainedAttackPlayersGoal();
        @Accessor("attackPlayersGoal") void skywars2$retainedAttackPlayersGoal(NearestAttackableWitchTargetGoal<Player> value);
    }

    @Mixin(Strider.class)
    public interface StriderAccess {
        @Accessor("temptGoal") TemptGoal skywars2$retainedTemptGoal();
        @Accessor("temptGoal") void skywars2$retainedTemptGoal(TemptGoal value);
    }

    @Mixin(AbstractSkeleton.class)
    public interface AbstractSkeletonAccess {
        @Accessor("bowGoal") RangedBowAttackGoal<AbstractSkeleton> skywars2$retainedBowGoal();
        @Accessor("meleeGoal") MeleeAttackGoal skywars2$retainedMeleeGoal();
    }

    @Mixin(Zombie.class)
    public interface ZombieAccess {
        @Accessor("breakDoorGoal") BreakDoorGoal skywars2$retainedBreakDoorGoal();
    }
}
