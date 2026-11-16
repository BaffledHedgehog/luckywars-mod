package me.skywars2.game;

import net.minecraft.core.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;

/** Native collision/open/close and door destruction; saved terrain is supplied before the actor phase. */
final class VanityDoorGoalChecks implements AutoCloseable {
    private final GameController game;
    private final WoolBucketChecks.Fixture f;
    private final BlockPos door;
    private final Vec3 approach;
    private final Zombie mob;
    private final GoalSelector selector;
    private final OpenDoorGoal open;
    private final boolean griefing;
    private MatchActorCheckpoint image;
    private int phase,when,remaining,forget,cycles;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Vanity door goals: "+why);}
    VanityDoorGoalChecks(GameController game,WoolBucketChecks.Fixture f){
        this.game=game;this.f=f;griefing=f.level.getGameRules().get(GameRules.MOB_GRIEFING);f.level.getGameRules().set(GameRules.MOB_GRIEFING,true,game.server());
        var floor=BlockPos.containing(f.home).above(3).offset(0,0,8);door=floor.offset(4,1,0);approach=Vec3.atBottomCenterOf(floor.offset(1,1,0));
        for(int x=0;x<=10;x++)for(int z=-1;z<=1;z++){
            var pos=floor.offset(x,0,z);f.set(pos,Blocks.STONE.defaultBlockState());
            for(int y=1;y<=3;y++)f.set(pos.above(y),z==0?Blocks.AIR.defaultBlockState():Blocks.STONE.defaultBlockState());
        }
        place(false);var target=new Cow(EntityTypes.COW,f.level);target.setPos(Vec3.atBottomCenterOf(floor.offset(9,1,0)));target.setNoAi(true);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);target.setHealth(100);target.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
        mob=new Zombie(EntityTypes.ZOMBIE,f.level);mob.setPos(approach);mob.setBaby(false);mob.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.LEATHER_HELMET));
        selector=(GoalSelector)WoolBucketChecks.read(mob,"goalSelector");selector.removeAllGoals(g->true);((GoalSelector)WoolBucketChecks.read(mob,"targetSelector")).removeAllGoals(g->true);
        ((GroundPathNavigation)mob.getNavigation()).setCanOpenDoors(true);mob.setTarget(target);
        open=new OpenDoorGoal(mob,true);selector.addGoal(0,open);selector.addGoal(1,new MeleeAttackGoal(mob,1,true));
        require(f.level.addFreshEntity(target)&&f.level.addFreshEntity(mob),"ordinary native door actors");f.own(target);f.own(mob);when=game.server().getTickCount();
    }
    private BlockState state(boolean upper,boolean opened){return Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING,Direction.EAST).setValue(DoorBlock.HALF,upper?DoubleBlockHalf.UPPER:DoubleBlockHalf.LOWER).setValue(DoorBlock.OPEN,opened);}
    private void place(boolean opened){f.set(door,state(false,opened));f.set(door.above(),state(true,opened));}
    private boolean opened(){var state=f.level.getBlockState(door);return state.is(Blocks.OAK_DOOR)&&state.getValue(DoorBlock.OPEN);}
    boolean step(){
        f.client.acknowledge();f.audience.acknowledge();int now=game.server().getTickCount();require(now-when<140,"bounded native door phase="+phase+",pos="+mob.position()+",collision="+mob.horizontalCollision);
        if(phase==0){
            if(!opened())return false;
            forget=(Integer)WoolBucketChecks.read(open,"forgetTime");require(forget>8,"actual native collision admission opened the door before capturing its close timer");
            image=MatchActorCheckpoint.captureRegistered(game);phase=1;when=now;return false;
        }
        if(phase==1){
            if(opened()){if(cycles>0)require(now-when<remaining,"native door stays open only for saved remaining duration");return false;}
            if(cycles==0){remaining=now-when;require(remaining>8,"native close really consumed the remaining timer");}
            else require(now-when==remaining,"same actual remaining close interval after replay");
            place(true);restore(image);require(opened()&&(Integer)WoolBucketChecks.read(open,"forgetTime")==forget,"open terrain and saved close countdown restored without another start");
            if(cycles++<2){when=now;return false;}
            // The opening snapshot can already overlap the door plane. Closing it
            // around that body lets the zombie finish crossing without another
            // collision. Begin the independent breaking scene from the corridor
            // entrance, then require real movement to admit the native task.
            selector.removeGoal(open);place(false);mob.getNavigation().stop();mob.setPos(approach);mob.setDeltaMovement(Vec3.ZERO);
            selector.addGoal(0,new BreakDoorGoal(mob,240,difficulty->true));phase=2;when=now;return false;
        }
        var breaking=selector.getAvailableGoals().stream().map(WrappedGoal::getGoal).filter(g->g instanceof BreakDoorGoal).map(g->(BreakDoorGoal)g).findFirst().orElseThrow();
        int elapsed=(Integer)WoolBucketChecks.read(breaking,"breakTime");
        if(elapsed<3)return false;
        require(DoorBlock.isWoodenDoor(f.level,door),"native breaking task started against a real closed wooden door");
        var block=(BlockPos)WoolBucketChecks.read(breaking,"doorPos");var saved=MatchActorCheckpoint.captureRegistered(game);int left=240-elapsed;
        for(int replay=0;replay<3;replay++){
            mob.setNoAi(true);int steps=0;
            while(DoorBlock.isWoodenDoor(f.level,block)&&steps<250){breaking.tick();steps++;if(steps<left)require(DoorBlock.isWoodenDoor(f.level,block),"saved break duration cannot destroy door early");}
            require(steps==left&&!DoorBlock.isWoodenDoor(f.level,block),"native break really removes door at the remaining saved step");
            breaking.stop();place(false);restore(saved);
            require((Integer)WoolBucketChecks.read(breaking,"breakTime")==elapsed&&WoolBucketChecks.read(breaking,"doorPos")==block,"saved door position and break phase restore without lifecycle replay");
        }
        mob.setNoAi(true);close();return true;
    }
    private void restore(MatchActorCheckpoint value){
        boolean frozen=game.server().tickRateManager().isFrozen();game.server().tickRateManager().setFrozen(true);
        try{value.restoreBodiesFrozen();require(value.restoreLinksFrozen().isEmpty(),"native door-task owner/target links resolve");}finally{game.server().tickRateManager().setFrozen(frozen);}
    }
    @Override public void close(){f.level.getGameRules().set(GameRules.MOB_GRIEFING,griefing,game.server());}
}
