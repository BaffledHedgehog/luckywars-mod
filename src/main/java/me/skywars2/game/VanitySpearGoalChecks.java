package me.skywars2.game;

import java.util.*;
import me.skywars2.mixin.CombatCheckpointAccess;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.SpearUseGoal;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.UseEffects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.*;
import net.minecraft.world.phys.Vec3;

/** Native engagement/use/retreat, owned state identity and accepted kinetic history resume together. */
final class VanitySpearGoalChecks {
    private VanitySpearGoalChecks(){}
    private static void require(boolean ok,String why){VanityActionGoalChecks.require(ok,"spear: "+why);}
    private static Object state(SpearUseGoal<?> goal){return WoolBucketChecks.read(goal,"state");}
    static void verify(GameController game,WoolBucketChecks.Fixture f){
        var floor=BlockPos.containing(f.home).above(3).offset(28,0,56);
        for(int x=-12;x<=12;x++)for(int z=-12;z<=12;z++){
            var p=floor.offset(x,0,z);f.set(p,Blocks.STONE.defaultBlockState());for(int y=1;y<=3;y++)f.set(p.above(y),Blocks.AIR.defaultBlockState());
        }
        var at=Vec3.atBottomCenterOf(floor.above());
        var mob=VanityActionGoalChecks.add(f,new Zombie(EntityTypes.ZOMBIE,f.level),at);
        var target=VanityActionGoalChecks.add(f,new Cow(EntityTypes.COW,f.level),at.add(4,0,0));
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);target.setHealth(100);target.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
        var spear=new ItemStack(Items.IRON_SPEAR);var useEffects=spear.getOrDefault(DataComponents.USE_EFFECTS,UseEffects.DEFAULT);
        spear.set(DataComponents.USE_EFFECTS,new UseEffects(useEffects.canSprint(),true,useEffects.speedMultiplier()));
        mob.setItemSlot(EquipmentSlot.MAINHAND,spear);mob.setTarget(target);
        int[] finished={0};var cell=mob.blockPosition();
        var listener=new GameEventListener(){
            public PositionSource getListenerSource(){return new BlockPositionSource(cell);}
            public int getListenerRadius(){return 16;}
            public boolean handleGameEvent(ServerLevel level,Holder<GameEvent> event,GameEvent.Context context,Vec3 pos){
                if(event==GameEvent.ITEM_INTERACT_FINISH&&context.sourceEntity()==mob)finished[0]++;return true;
            }
        };
        var registry=f.level.getChunkAt(cell).getListenerRegistry(SectionPos.blockToSectionCoord(cell.getY()));registry.register(listener);
        try{
            var goal=new SpearUseGoal<>(mob,1,1,16,.5F);var wrapper=VanityActionGoalChecks.admit(mob,goal);
            require(mob.isUsingItem()&&mob.isAggressive(),"real native spear engagement raises weapon");
            float health=target.getHealth();require(mob.stabAttack(EquipmentSlot.MAINHAND,target,1,true,false,false)&&target.getHealth()<health,"genuine accepted native stab");
            mob.rememberStabbedEntity(target);require(mob.wasRecentlyStabbed(target,20),"native kinetic history registered");
            var original=state(goal);var old=new LinkedHashMap<String,Object>();for(var name:List.of("engageTime","fleeingTime","awayPos","done"))old.put(name,WoolBucketChecks.read(original,name));
            require((Integer)old.get("engageTime")>1&&!(Boolean)old.get("done"),"saved in-progress engagement");
            int use=mob.getUseItemRemainingTicks();var image=MatchActorCheckpoint.captureRegistered(game);
            for(int replay=0;replay<2;replay++){
                for(int ticks=0;VanityActionGoalChecks.count(original,"engageTime")>0&&ticks<10000;ticks++)goal.tick();
                require(VanityActionGoalChecks.count(original,"engageTime")==0&&VanityActionGoalChecks.count(original,"fleeingTime")>0,"original native state really entered retreat");
                VanityActionGoalChecks.selector(mob).removeAllGoals(g->true);require(finished[0]>0&&!mob.isUsingItem(),"native stop emits observable finish event");
                VanityActionGoalChecks.admit(mob,goal);require(state(goal)!=original,"future native admission allocates a different state object");
                finished[0]=0;VanityActionGoalChecks.restore(game,image,null,Set.of());
                require(finished[0]==0&&state(goal)==original&&wrapper.isRunning(),"restore original state alias without start/stop vibration");
                old.forEach((name,value)->require(Objects.equals(WoolBucketChecks.read(original,name),value),"saved nested "+name));
                require(mob.isUsingItem()&&mob.isAggressive()&&mob.getUseItem()==mob.getMainHandItem()&&mob.getUseItemRemainingTicks()==use,
                        "exact native weapon-use data and pose restore");
                require(mob.wasRecentlyStabbed(target,20)&&mob.stabbedEntities(e->true)==1,"shared native kinetic hit history restores with engagement");
            }
            finished[0]=0;VanityActionGoalChecks.restore(game,image,null,Set.of(target.getUUID()));
            require(finished[0]==0&&!wrapper.isRunning()&&state(goal)==null&&!mob.isUsingItem()&&!mob.isAggressive()&&mob.getUseItem().isEmpty()&&mob.getUseItemRemainingTicks()==0,
                    "absent target retires engagement and use values without finish vibration");
            require(((CombatCheckpointAccess.Living)mob).skywars2$kinetic()==null,"absent-task cleanup runs after combat replay and clears kinetic ledger");
            VanityActionGoalChecks.selector(mob).tickRunningGoals(false); // Before full selector cleanup, no null-target tick is permitted.
            VanityActionGoalChecks.restore(game,image,null,Set.of());
            require(state(goal)==original&&wrapper.isRunning()&&mob.isUsingItem()&&mob.wasRecentlyStabbed(target,20),"same full image remains usable after target omission");
        }finally{registry.unregister(listener);}
    }
}
