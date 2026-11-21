package me.skywars2.game;

import java.util.*;
import java.util.function.Function;
import java.util.function.Consumer;
import me.skywars2.mixin.MobGoalCheckpointAccess;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.level.pathfinder.Path;

/** Original native selector identities and reviewed melee continuation; no goal lifecycle callbacks on replay. */
public final class MatchMobGoals {
    private static final boolean ENABLED=Boolean.getBoolean("skywars2.experimental-native-goal-replay");
    public static boolean enabled(){return ENABLED;}
    private final Mob owner;
    private final long worldTime;
    private final List<Selector> selectors=new ArrayList<>();
    private final Map<Goal,EnumSet<Goal.Flag>> flags=new IdentityHashMap<>();
    private final Map<MeleeAttackGoal,Melee> attacks=new IdentityHashMap<>();
    private final Map<RangedAttackGoal,Ranged> ranged=new IdentityHashMap<>();
    private final Map<TargetGoal,Target> targets=new IdentityHashMap<>();
    private final Map<BreedGoal,Breeding> breeding=new IdentityHashMap<>();
    private final Map<Goal,MatchGoalFields> common=new IdentityHashMap<>();
    private final Map<SpearUseGoal<?>,MatchSpearGoal> spears=new IdentityHashMap<>();
    private final MatchRetainedGoals retained;

    public MatchMobGoals(Mob mob) {
        owner=mob;worldTime=mob.level().getGameTime();
        var access = (MobGoalCheckpointAccess.MobAccess)(Object) mob;
        selectors.add(new Selector(access.skywars2$goalGoalSelector()));
        selectors.add(new Selector(access.skywars2$goalTargetSelector()));
        retained=new MatchRetainedGoals(mob,goal->capture(goal,false));
    }
    private void capture(Goal goal,boolean running){
        flags.computeIfAbsent(goal,key->key.getFlags().clone());
        if(goal instanceof MeleeAttackGoal attack)attacks.computeIfAbsent(attack,Melee::new);
        if(goal instanceof RangedAttackGoal attack)ranged.computeIfAbsent(attack,Ranged::new);
        if(goal instanceof TargetGoal target)targets.computeIfAbsent(target,Target::new);
        if(goal instanceof BreedGoal task)breeding.computeIfAbsent(task,Breeding::new);
        if(goal instanceof SpearUseGoal<?> spear)spears.computeIfAbsent(spear,key->new MatchSpearGoal(owner,key)).wasRunning|=running;
        var task=common.computeIfAbsent(goal,key->MatchGoalFields.capture(owner,key));
        if(task!=null)task.wasRunning|=running;
    }
    public void prepareLoad(){selectors.forEach(Selector::suspend);}
    public void restore() {
        // stop() can erase a restored route/target; start() resets attack timers and can repeat side effects.
        retained.restore();for(var selector:selectors)selector.retireFuture();
        flags.forEach((goal,saved)->{var live=goal.getFlags();live.clear();live.addAll(saved);});
        long delta=owner.level().getGameTime()-worldTime;
        attacks.values().forEach(attack->attack.restore(delta));
        ranged.values().forEach(Ranged::restore);
        targets.values().forEach(Target::restore);
        breeding.values().forEach(Breeding::restore);
        common.values().forEach(MatchGoalFields::restore);
        spears.values().forEach(MatchSpearGoal::restore);
        selectors.forEach(Selector::restore);
    }
    public void captureReferences(MatchDamageSources sources){
        ranged.values().forEach(attack->attack.captureReferences(sources));
        targets.values().forEach(target->target.captureReferences(sources));
        breeding.values().forEach(task->task.captureReferences(sources));
        common.values().forEach(task->task.captureReferences(sources));
        spears.values().forEach(task->task.captureReferences(sources));
    }
    public void restoreReferences(Function<UUID,Entity> lookup,Set<UUID> pending) {
        for(var attack:ranged.values()) {
            if(!attack.referencesCaptured)continue;
            Entity resolved=MatchDamageSources.resolve(attack.targetRef,lookup,pending);
            if(attack.targetRef!=null&&resolved==null) {
                if(MatchDamageSources.intentionallyAbsent(attack.targetRef.id())) {
                    // Match the native absent-target policy without calling stop() during world replay.
                    attack.absent();
                    for(var selector:selectors)selector.retire(attack.goal);
                }
                continue;
            }
            if(resolved!=null&&!(resolved instanceof LivingEntity))throw new IllegalStateException("Native ranged target resolved to a non-living body");
            attack.access.skywars2$goalTarget((LivingEntity) resolved);
        }
        for(var target:targets.values())if(target.restoreReferences(lookup,pending)){
            target.absent();for(var selector:selectors)selector.retire(target.goal);
        }
        for(var task:breeding.values()){
            if(!task.referencesCaptured)continue;
            Entity resolved=MatchDamageSources.resolve(task.partnerRef,lookup,pending);
            if(task.partnerRef!=null&&resolved==null){
                if(MatchDamageSources.intentionallyAbsent(task.partnerRef.id())){
                    task.absent();for(var selector:selectors)selector.retire(task.goal);
                }
                continue;
            }
            if(resolved!=null&&!(resolved instanceof Animal))throw new IllegalStateException("Native breeding partner resolved to a non-animal body");
            task.access.skywars2$goalPartner((Animal) resolved);
        }
        for(var task:common.values())if(task.restoreReferences(lookup,pending)){
            task.absent();for(var selector:selectors)selector.retire(task.goal);
        }
        for(var task:spears.values())if(task.restoreReferences(lookup,pending)){
            task.absent();for(var selector:selectors)selector.retire(task.goal);
        }
    }
    private final class Selector {
        final Set<WrappedGoal> available;
        final List<WrappedGoal> ordered;
        final Map<Goal.Flag,WrappedGoal> locked,savedLocked=new EnumMap<>(Goal.Flag.class);
        final EnumSet<Goal.Flag> disabled,savedDisabled;
        final Map<WrappedGoal,Boolean> running=new IdentityHashMap<>();
        Selector(GoalSelector selector) {
            var access = (MobGoalCheckpointAccess.GoalSelectorAccess)(Object) selector;
            available=selector.getAvailableGoals();ordered=List.copyOf(available);
            locked=access.skywars2$goalLockedFlags();savedLocked.putAll(locked);
            disabled=access.skywars2$goalDisabledFlags();savedDisabled=disabled.clone();
            var all=Collections.newSetFromMap(new IdentityHashMap<WrappedGoal,Boolean>());
            all.addAll(ordered);all.addAll(locked.values());
            for(var wrapper:all) {
                running.put(wrapper,wrapper.isRunning());Goal goal=wrapper.getGoal();
                capture(goal,wrapper.isRunning());
            }
        }
        void suspend(){
            // Native NBT/equipment loaders may remove tasks before restore(); never replay stop() there.
            for(var wrapper:available)running(wrapper,false);
            for(var wrapper:locked.values())running(wrapper,false);
        }
        void retireFuture() {
            for(var wrapper:available)if(!running.containsKey(wrapper))running(wrapper,false);
            for(var wrapper:locked.values())if(!running.containsKey(wrapper))running(wrapper,false);
        }
        void restore() {
            available.clear();available.addAll(ordered);
            running.forEach((wrapper,active)->running(wrapper,active));
            locked.clear();locked.putAll(savedLocked);disabled.clear();disabled.addAll(savedDisabled);
        }
        void retire(Goal goal) {
            for(var wrapper:available)if(wrapper.getGoal()==goal)running(wrapper,false);
            locked.entrySet().removeIf(entry->entry.getValue().getGoal()==goal);
        }
    }
    private static void running(WrappedGoal wrapper, boolean running) {
        ((MobGoalCheckpointAccess.WrappedGoalAccess)(Object) wrapper).skywars2$goalIsRunning(running);
    }

    private record Melee(MobGoalCheckpointAccess.MeleeAttackGoalAccess access, Path path, double x, double y, double z,
                         int recalc, int attack, long lastCheck, MatchMobNavigation.Route route) {
        Melee(MeleeAttackGoal goal) {
            this((MobGoalCheckpointAccess.MeleeAttackGoalAccess)(Object) goal);
        }
        private Melee(MobGoalCheckpointAccess.MeleeAttackGoalAccess access) {
            this(access,access.skywars2$goalPath(),access.skywars2$goalPathedTargetX(),access.skywars2$goalPathedTargetY(),
                    access.skywars2$goalPathedTargetZ(),access.skywars2$goalTicksUntilNextPathRecalculation(),
                    access.skywars2$goalTicksUntilNextAttack(),access.skywars2$goalLastCanUseCheck(),
                    access.skywars2$goalPath() == null ? null : new MatchMobNavigation.Route(access.skywars2$goalPath()));
        }
        void restore(long delta) {
            if (route != null) route.restore();
            access.skywars2$goalPath(path);
            access.skywars2$goalPathedTargetX(x);
            access.skywars2$goalPathedTargetY(y);
            access.skywars2$goalPathedTargetZ(z);
            access.skywars2$goalTicksUntilNextPathRecalculation(recalc);
            access.skywars2$goalTicksUntilNextAttack(attack);
            access.skywars2$goalLastCanUseCheck(Math.addExact(lastCheck,delta));
        }
    }

    /** The target is not Mob.target: native stop clears this separate cache before a half-step. */
    private static final class Ranged {
        final RangedAttackGoal goal;
        final MobGoalCheckpointAccess.RangedAttackGoalAccess access;
        final LivingEntity target;
        final int attack, see;
        MatchDamageSources.Ref targetRef;
        boolean referencesCaptured;

        Ranged(RangedAttackGoal goal) {
            this.goal=goal;
            access=(MobGoalCheckpointAccess.RangedAttackGoalAccess)(Object) goal;
            target=access.skywars2$goalTarget();
            attack=access.skywars2$goalAttackTime();
            see=access.skywars2$goalSeeTime();
        }
        void restore() {
            access.skywars2$goalTarget(target);
            access.skywars2$goalAttackTime(attack);
            access.skywars2$goalSeeTime(see);
        }
        void captureReferences(MatchDamageSources sources) {
            targetRef=sources.dependencyRef(target);
            referencesCaptured=true;
        }
        void absent() {
            access.skywars2$goalTarget(null);
            access.skywars2$goalAttackTime(-1);
            access.skywars2$goalSeeTime(0);
        }
    }

    /** Reviewed target base, nearest-search cache and body-local retaliation stamp. */
    private static final class Target {
        final TargetGoal goal;
        final MobGoalCheckpointAccess.TargetGoalAccess access;
        final MobGoalCheckpointAccess.NearestAttackableTargetGoalAccess nearest;
        final HurtBy hurtBy;
        final int reach, reachTime, unseen, memory;
        final LivingEntity target, nearestTarget;
        MatchDamageSources.Ref targetRef, nearestRef;
        boolean referencesCaptured;

        Target(TargetGoal goal) {
            this.goal=goal;
            access=(MobGoalCheckpointAccess.TargetGoalAccess)(Object) goal;
            reach=access.skywars2$goalReachCache();
            reachTime=access.skywars2$goalReachCacheTime();
            unseen=access.skywars2$goalUnseenTicks();
            memory=access.skywars2$goalUnseenMemoryTicks();
            target=access.skywars2$goalTargetMob();
            nearest=goal instanceof NearestAttackableTargetGoal<?>
                    ? (MobGoalCheckpointAccess.NearestAttackableTargetGoalAccess)(Object) goal : null;
            nearestTarget=nearest == null ? null : nearest.skywars2$goalTarget();
            hurtBy=goal instanceof HurtByTargetGoal
                    ? new HurtBy((MobGoalCheckpointAccess.HurtByTargetGoalAccess)(Object) goal) : null;
        }

        void restore() {
            if (hurtBy != null) hurtBy.restoreArrays();
            access.skywars2$goalReachCache(reach);
            access.skywars2$goalReachCacheTime(reachTime);
            access.skywars2$goalUnseenTicks(unseen);
            access.skywars2$goalUnseenMemoryTicks(memory);
            access.skywars2$goalTargetMob(target);
            if (nearest != null) nearest.skywars2$goalTarget(nearestTarget);
            if (hurtBy != null) hurtBy.restore();
        }

        void captureReferences(MatchDamageSources sources) {
            targetRef=sources.dependencyRef(target);
            nearestRef=nearest == null ? null : sources.dependencyRef(nearestTarget);
            referencesCaptured=true;
        }

        boolean restoreReferences(Function<UUID,Entity> lookup, Set<UUID> pending) {
            if (!referencesCaptured) return false;
            boolean absent=bind(targetRef,lookup,pending,access::skywars2$goalTargetMob);
            if (nearest != null) absent |= bind(nearestRef,lookup,pending,nearest::skywars2$goalTarget);
            return absent;
        }

        private static boolean bind(MatchDamageSources.Ref ref, Function<UUID,Entity> lookup, Set<UUID> pending,
                                    Consumer<LivingEntity> write) {
            Entity resolved=MatchDamageSources.resolve(ref,lookup,pending);
            boolean missing=ref!=null && resolved==null;
            if (missing && !MatchDamageSources.intentionallyAbsent(ref.id())) return false;
            if (resolved!=null && !(resolved instanceof LivingEntity))
                throw new IllegalStateException("Native target task resolved to a non-living body");
            write.accept((LivingEntity) resolved);
            return missing;
        }

        void absent() {
            access.skywars2$goalTargetMob(null);
            if (nearest!=null) nearest.skywars2$goalTarget(null);
            access.skywars2$goalReachCache(0);
            access.skywars2$goalReachCacheTime(0);
            access.skywars2$goalUnseenTicks(0);
        }
    }

    private record HurtBy(MobGoalCheckpointAccess.HurtByTargetGoalAccess access, int timestamp, boolean alert,
                          Class<?>[] damage, Class<?>[] savedDamage, Class<?>[] ignored, Class<?>[] savedIgnored) {
        HurtBy(MobGoalCheckpointAccess.HurtByTargetGoalAccess access) {
            this(access,access.skywars2$goalTimestamp(),access.skywars2$goalAlertSameType(),
                    access.skywars2$goalToIgnoreDamage(),copy(access.skywars2$goalToIgnoreDamage()),
                    access.skywars2$goalToIgnoreAlert(),copy(access.skywars2$goalToIgnoreAlert()));
        }
        private static Class<?>[] copy(Class<?>[] values) {
            return values == null ? null : values.clone();
        }
        void restoreArrays() {
            // Native constructors/setAlertOthers keep caller-owned arrays, including the final damage filter.
            if (damage != null) System.arraycopy(savedDamage,0,damage,0,savedDamage.length);
            if (ignored != null && ignored != damage) System.arraycopy(savedIgnored,0,ignored,0,savedIgnored.length);
        }
        void restore() {
            access.skywars2$goalTimestamp(timestamp);
            access.skywars2$goalAlertSameType(alert);
            if (access.skywars2$goalToIgnoreDamage()!=damage)
                throw new IllegalStateException("Final native target damage filter identity changed");
            access.skywars2$goalToIgnoreAlert(ignored);
        }
    }

    private static final class Breeding {
        final BreedGoal goal;
        final MobGoalCheckpointAccess.BreedGoalAccess access;
        final Animal partner;
        final int loveTime;
        MatchDamageSources.Ref partnerRef;
        boolean referencesCaptured;

        Breeding(BreedGoal goal) {
            this.goal=goal;
            access=(MobGoalCheckpointAccess.BreedGoalAccess)(Object) goal;
            partner=access.skywars2$goalPartner();
            loveTime=access.skywars2$goalLoveTime();
        }
        void restore() {
            access.skywars2$goalPartner(partner);
            access.skywars2$goalLoveTime(loveTime);
        }
        void captureReferences(MatchDamageSources sources) {
            partnerRef=sources.dependencyRef(partner);
            referencesCaptured=true;
        }
        void absent() {
            access.skywars2$goalPartner(null);
            access.skywars2$goalLoveTime(0);
        }
    }
}
