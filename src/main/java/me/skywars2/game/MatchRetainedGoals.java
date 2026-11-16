package me.skywars2.game;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

/** Reviewed native task reference slots, including tasks currently absent from either selector. */
final class MatchRetainedGoals {
    private final Mob owner;
    private final Map<Field,Goal> values=new LinkedHashMap<>();
    MatchRetainedGoals(Mob owner,Consumer<Goal> capture){
        this.owner=owner;
        for(Class<?> type=owner.getClass();type!=null&&type!=Mob.class;type=type.getSuperclass()){
            String[] names=switch(type.getName()){
                case "net.minecraft.world.entity.animal.bee.Bee" -> new String[]{"beePollinateGoal","goToHiveGoal","goToKnownFlowerGoal"};
                case "net.minecraft.world.entity.animal.equine.SkeletonHorse" -> new String[]{"skeletonTrapGoal"};
                case "net.minecraft.world.entity.animal.feline.Cat" -> new String[]{"avoidPlayersGoal","temptGoal"};
                case "net.minecraft.world.entity.animal.feline.Ocelot" -> new String[]{"ocelotAvoidPlayersGoal","temptGoal"};
                case "net.minecraft.world.entity.animal.fox.Fox" -> new String[]{"landTargetGoal","turtleEggTargetGoal","fishTargetGoal"};
                case "net.minecraft.world.entity.animal.panda.Panda" -> new String[]{"lookAtPlayerGoal"};
                case "net.minecraft.world.entity.animal.sheep.Sheep" -> new String[]{"eatBlockGoal"};
                case "net.minecraft.world.entity.monster.Guardian" -> new String[]{"randomStrollGoal"};
                case "net.minecraft.world.entity.monster.Silverfish" -> new String[]{"friendsGoal"};
                case "net.minecraft.world.entity.monster.Witch" -> new String[]{"healRaidersGoal","attackPlayersGoal"};
                case "net.minecraft.world.entity.monster.Strider" -> new String[]{"temptGoal"};
                case "net.minecraft.world.entity.monster.skeleton.AbstractSkeleton" -> new String[]{"bowGoal","meleeGoal"};
                case "net.minecraft.world.entity.monster.zombie.Zombie" -> new String[]{"breakDoorGoal"};
                default -> new String[0];
            };
            for(String name:names)try{
                var field=type.getDeclaredField(name);field.setAccessible(true);
                if(!Goal.class.isAssignableFrom(field.getType()))throw new IllegalStateException("Review retained task slot: "+field);
                var goal=(Goal)field.get(owner);values.put(field,goal);if(goal!=null)capture.accept(goal);
            }catch(ReflectiveOperationException error){throw new IllegalStateException("Review native retained task "+type.getName()+"."+name,error);}
        }
    }
    void restore(){
        values.forEach((field,goal)->{
            try{
                if(field.get(owner)==goal)return;
                if(Modifier.isFinal(field.getModifiers()))throw new IllegalStateException("Final native task identity changed outside its owner: "+field);
                field.set(owner,goal);
            }catch(IllegalAccessException error){throw new IllegalStateException(error);}
        });
    }
}
