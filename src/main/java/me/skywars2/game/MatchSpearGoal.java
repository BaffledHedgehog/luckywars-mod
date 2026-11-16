package me.skywars2.game;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;
import me.skywars2.mixin.CombatCheckpointAccess;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.SpearUseGoal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** One reviewed nested state, not a recursive copy of arbitrary native task graphs. */
final class MatchSpearGoal {
    final SpearUseGoal<?> goal;
    boolean wasRunning;
    private final Mob owner;
    private final LivingEntity target;
    private final SpearUseGoal.SpearUseState state;
    private final Map<Field,Object> values=new LinkedHashMap<>();
    private MatchDamageSources.Ref targetRef;
    private boolean referencesCaptured;

    MatchSpearGoal(Mob owner,SpearUseGoal<?> goal){
        this.owner=owner;this.goal=goal;target=owner.getTargetUnchecked();
        state=(SpearUseGoal.SpearUseState)read(goal,SpearUseGoal.class,"state");
        if(state!=null){
            if(state.getClass()!=SpearUseGoal.SpearUseState.class)throw new IllegalStateException("Review native spear state subtype");
            for(String name:List.of("engageTime","fleeingTime","awayPos","done")){
                var field=field(SpearUseGoal.SpearUseState.class,name);var value=get(field,state);
                if(value instanceof Vec3&&value.getClass()!=Vec3.class)throw new IllegalStateException("Review mutable native spear vector");
                values.put(field,value);
            }
        }
    }
    void restore(){
        values.forEach((field,value)->set(field,state,value));
        write(goal,SpearUseGoal.class,"state",state);
    }
    void captureReferences(MatchDamageSources sources){targetRef=sources.dependencyRef(target);referencesCaptured=true;}
    boolean restoreReferences(Function<UUID,Entity> lookup,Set<UUID> pending){
        if(!referencesCaptured||!wasRunning)return false;
        var resolved=MatchDamageSources.resolve(targetRef,lookup,pending);
        return targetRef!=null&&resolved==null&&MatchDamageSources.intentionallyAbsent(targetRef.id());
    }
    @SuppressWarnings("unchecked") void absent(){
        write(goal,SpearUseGoal.class,"state",null);owner.getNavigation().stop();owner.setAggressive(false);
        // Native stopUsingItem can emit ITEM_INTERACT_FINISH when configured. Restore only its data values.
        ((CombatCheckpointAccess.Living)owner).skywars2$kinetic(null);
        var key=(EntityDataAccessor<Byte>)read(null,LivingEntity.class,"DATA_LIVING_ENTITY_FLAGS");
        owner.getEntityData().set(key,(byte)(owner.getEntityData().get(key)&~1));
        write(owner,LivingEntity.class,"useItem",ItemStack.EMPTY);write(owner,LivingEntity.class,"useItemRemaining",0);
    }
    private static Field field(Class<?> type,String name){
        try{var field=type.getDeclaredField(name);field.setAccessible(true);return field;}
        catch(ReflectiveOperationException e){throw new IllegalStateException("Review native spear field "+type.getName()+"."+name,e);}
    }
    private static Object read(Object object,Class<?> type,String name){return get(field(type,name),object);}
    private static Object get(Field field,Object object){try{return field.get(object);}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
    private static void set(Field field,Object object,Object value){try{field.set(object,value);}catch(IllegalAccessException e){throw new IllegalStateException(e);}}
    private static void write(Object object,Class<?> type,String name,Object value){set(field(type,name),object,value);}
}
