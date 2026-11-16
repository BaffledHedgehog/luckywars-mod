package me.skywars2.game;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.level.pathfinder.Path;

/** Original native selector identities and reviewed melee continuation; no goal lifecycle callbacks on replay. */
final class MatchMobGoals {
    private static final boolean ENABLED=Boolean.getBoolean("skywars2.experimental-native-goal-replay");
    static boolean enabled(){return ENABLED;}
    private final Mob owner;
    private final long worldTime;
    private final List<Selector> selectors=new ArrayList<>();
    private final Map<Goal,EnumSet<Goal.Flag>> flags=new IdentityHashMap<>();
    private final Map<MeleeAttackGoal,Melee> attacks=new IdentityHashMap<>();
    private final Map<RangedAttackGoal,Ranged> ranged=new IdentityHashMap<>();
    private final Map<TargetGoal,Target> targets=new IdentityHashMap<>();

    MatchMobGoals(Mob mob) {
        owner=mob;worldTime=mob.level().getGameTime();
        selectors.add(new Selector((GoalSelector)read(mob,Mob.class,"goalSelector")));
        selectors.add(new Selector((GoalSelector)read(mob,Mob.class,"targetSelector")));
    }
    void restore() {
        // stop() can erase a restored route/target; start() resets attack timers and can repeat side effects.
        for(var selector:selectors)selector.retireFuture();
        flags.forEach((goal,saved)->{var live=goal.getFlags();live.clear();live.addAll(saved);});
        long delta=owner.level().getGameTime()-worldTime;
        attacks.values().forEach(attack->attack.restore(delta));
        ranged.values().forEach(Ranged::restore);
        targets.values().forEach(Target::restore);
        selectors.forEach(Selector::restore);
    }
    void captureReferences(MatchDamageSources sources){
        ranged.values().forEach(attack->attack.captureReferences(sources));
        targets.values().forEach(target->target.captureReferences(sources));
    }
    void restoreReferences(Function<UUID,Entity> lookup,Set<UUID> pending) {
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
            write(attack.goal,RangedAttackGoal.class,"target",resolved);
        }
        for(var target:targets.values())if(target.restoreReferences(lookup,pending)){
            target.absent();for(var selector:selectors)selector.retire(target.goal);
        }
    }
    private final class Selector {
        final Set<WrappedGoal> available;
        final List<WrappedGoal> ordered;
        final Map<Goal.Flag,WrappedGoal> locked,savedLocked=new EnumMap<>(Goal.Flag.class);
        final EnumSet<Goal.Flag> disabled,savedDisabled;
        final Map<WrappedGoal,Boolean> running=new IdentityHashMap<>();
        @SuppressWarnings("unchecked") Selector(GoalSelector selector) {
            available=selector.getAvailableGoals();ordered=List.copyOf(available);
            locked=(Map<Goal.Flag,WrappedGoal>)read(selector,GoalSelector.class,"lockedFlags");savedLocked.putAll(locked);
            disabled=(EnumSet<Goal.Flag>)read(selector,GoalSelector.class,"disabledFlags");savedDisabled=disabled.clone();
            var all=Collections.newSetFromMap(new IdentityHashMap<WrappedGoal,Boolean>());
            all.addAll(ordered);all.addAll(locked.values());
            for(var wrapper:all) {
                running.put(wrapper,wrapper.isRunning());Goal goal=wrapper.getGoal();
                flags.computeIfAbsent(goal,key->key.getFlags().clone());
                if(goal instanceof MeleeAttackGoal attack)attacks.computeIfAbsent(attack,Melee::new);
                if(goal instanceof RangedAttackGoal attack)ranged.computeIfAbsent(attack,Ranged::new);
                if(goal instanceof TargetGoal target)targets.computeIfAbsent(target,Target::new);
            }
        }
        void retireFuture() {
            for(var wrapper:available)if(!running.containsKey(wrapper))write(wrapper,WrappedGoal.class,"isRunning",false);
            for(var wrapper:locked.values())if(!running.containsKey(wrapper))write(wrapper,WrappedGoal.class,"isRunning",false);
        }
        void restore() {
            available.clear();available.addAll(ordered);
            running.forEach((wrapper,active)->write(wrapper,WrappedGoal.class,"isRunning",active));
            locked.clear();locked.putAll(savedLocked);disabled.clear();disabled.addAll(savedDisabled);
        }
        void retire(Goal goal) {
            for(var wrapper:available)if(wrapper.getGoal()==goal)write(wrapper,WrappedGoal.class,"isRunning",false);
            locked.entrySet().removeIf(entry->entry.getValue().getGoal()==goal);
        }
    }
    private static final class Melee {
        final MeleeAttackGoal goal;
        final Map<Field,Object> values=new LinkedHashMap<>();
        final long lastCheck;
        final MatchMobNavigation.Route route;
        Melee(MeleeAttackGoal goal) {
            this.goal=goal;
            for(String name:List.of("path","pathedTargetX","pathedTargetY","pathedTargetZ","ticksUntilNextPathRecalculation","ticksUntilNextAttack")) {
                Field field=field(MeleeAttackGoal.class,name);values.put(field,get(field,goal));
            }
            lastCheck=(Long)read(goal,MeleeAttackGoal.class,"lastCanUseCheck");
            Path path=(Path)read(goal,MeleeAttackGoal.class,"path");route=path==null?null:new MatchMobNavigation.Route(path);
        }
        void restore(long delta) {
            if(route!=null)route.restore();values.forEach((field,value)->set(field,goal,value));
            write(goal,MeleeAttackGoal.class,"lastCanUseCheck",Math.addExact(lastCheck,delta));
        }
    }
    /** The target is not Mob.target: native stop clears this separate cache before a half-step. */
    private static final class Ranged {
        final RangedAttackGoal goal;
        final Map<Field,Object> values=new LinkedHashMap<>();
        final LivingEntity target;
        MatchDamageSources.Ref targetRef;boolean referencesCaptured;
        Ranged(RangedAttackGoal goal) {
            this.goal=goal;target=(LivingEntity)read(goal,RangedAttackGoal.class,"target");
            for(String name:List.of("target","attackTime","seeTime")) {
                Field field=field(RangedAttackGoal.class,name);values.put(field,get(field,goal));
            }
        }
        void restore(){values.forEach((field,value)->set(field,goal,value));}
        void captureReferences(MatchDamageSources sources){targetRef=sources.bodyRef(target);referencesCaptured=true;}
        void absent(){write(goal,RangedAttackGoal.class,"target",null);write(goal,RangedAttackGoal.class,"attackTime",-1);write(goal,RangedAttackGoal.class,"seeTime",0);}
    }
    /** Reviewed target base, nearest-search cache and body-local retaliation stamp; not arbitrary subclasses. */
    private static final class Target {
        final TargetGoal goal;
        final Map<Field,Object> values=new LinkedHashMap<>();
        final Map<Field,LivingEntity> bodies=new LinkedHashMap<>();
        final Map<Field,MatchDamageSources.Ref> references=new LinkedHashMap<>();
        boolean referencesCaptured;
        Target(TargetGoal goal){
            this.goal=goal;
            for(String name:List.of("reachCache","reachCacheTime","unseenTicks","unseenMemoryTicks"))save(TargetGoal.class,name);
            body(TargetGoal.class,"targetMob");
            if(goal instanceof NearestAttackableTargetGoal<?>)body(NearestAttackableTargetGoal.class,"target");
            if(goal instanceof HurtByTargetGoal)save(HurtByTargetGoal.class,"timestamp");
        }
        void save(Class<?> type,String name){Field field=field(type,name);values.put(field,get(field,goal));}
        void body(Class<?> type,String name){save(type,name);Field field=field(type,name);bodies.put(field,(LivingEntity)get(field,goal));}
        void restore(){values.forEach((field,value)->set(field,goal,value));}
        void captureReferences(MatchDamageSources sources){bodies.forEach((field,body)->references.put(field,sources.bodyRef(body)));referencesCaptured=true;}
        boolean restoreReferences(Function<UUID,Entity> lookup,Set<UUID> pending){
            if(!referencesCaptured)return false;
            boolean absent=false;
            for(var entry:references.entrySet()){
                var ref=entry.getValue();Entity resolved=MatchDamageSources.resolve(ref,lookup,pending);
                if(ref!=null&&resolved==null){
                    if(!MatchDamageSources.intentionallyAbsent(ref.id()))continue;
                    absent=true;
                }
                if(resolved!=null&&!(resolved instanceof LivingEntity))throw new IllegalStateException("Native target task resolved to a non-living body");
                set(entry.getKey(),goal,resolved);
            }
            return absent;
        }
        void absent(){
            bodies.keySet().forEach(field->set(field,goal,null));
            for(String name:List.of("reachCache","reachCacheTime","unseenTicks"))write(goal,TargetGoal.class,name,0);
        }
    }
    private static Field field(Class<?> type,String name) {
        try{var field=type.getDeclaredField(name);field.setAccessible(true);return field;}
        catch(ReflectiveOperationException error){throw new IllegalStateException("Review native goal checkpoint "+type.getName()+"."+name,error);}
    }
    private static Object read(Object target,Class<?> type,String name){return get(field(type,name),target);}
    private static Object get(Field field,Object target) {
        try{return field.get(target);}catch(IllegalAccessException error){throw new IllegalStateException(error);}
    }
    private static void write(Object target,Class<?> type,String name,Object value){set(field(type,name),target,value);}
    private static void set(Field field,Object target,Object value) {
        try{field.set(target,value);}catch(IllegalAccessException error){throw new IllegalStateException(error);}
    }
}
