package me.skywars2.game;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Only source slowraycast abilities; instant search queues are deliberately absent. */
public final class CloneVirtualAbilityStates {
    private CloneVirtualAbilityStates() { }
    public static int duplicateNear(GameController game,ServerPlayer holder,int skill,int budget,long deadlineNanos) {
        if(skill!=15&&skill!=17||budget<count(skill)||System.nanoTime()>=deadlineNanos)return 0;
        int made=game.items().wands().cloneNearby(holder,skill,budget,deadlineNanos);
        if(budget-made>=count(skill)&&System.nanoTime()<deadlineNanos)made+=game.items().voidArcanum().cloneNearby(holder,skill,budget-made,deadlineNanos);
        if(budget-made>=count(skill)&&System.nanoTime()<deadlineNanos)made+=game.items().homing().cloneNearby(holder,skill,budget-made,deadlineNanos);
        if(budget-made>=count(skill)&&System.nanoTime()<deadlineNanos)made+=game.items().sauvojen().stars().cloneNearby(holder,skill,budget-made,deadlineNanos);
        if(budget-made>=count(skill)&&System.nanoTime()<deadlineNanos)made+=game.items().duplicating().cloneNearby(holder,skill,budget-made,deadlineNanos);
        return made;
    }
    public static boolean protectedActor(Entity e) {
        return LbcVoidArcanum.cloneProtected(e)||e.entityTags().contains(SauvojenStars.TAG);
    }
    public static boolean recognizes(Entity e){return LbcHedgehog.cloneRecognizes(e);}
    public static LbcCloneSkills.ActorImage captureNative(GameController game,Entity e){return game.items().hedgehog().cloneImage(e);}
    static int count(int skill){return skill==15?1:3;}
    static int bit(int skill){return skill==15?1:2;}
    static boolean near(GameController game,ServerPlayer holder,ServerLevel level,Vec3 point,int mask,int skill) {
        double radius=skill==15?10:16;
        return (mask&bit(skill))==0&&level==holder.level()&&point.distanceToSqr(holder.position())<=radius*radius
                &&level.hasChunkAt(BlockPos.containing(point))&&!game.items().timeStop().stopped(level);
    }
    static List<Vec3> positions(ServerPlayer holder,ServerLevel level,Vec3 point,int skill) {
        var result=new ArrayList<Vec3>();
        for(int i=0;i<count(skill);i++){var next=LbcCloneSkills.spreadPosition(point,holder);
            if(!level.hasChunkAt(BlockPos.containing(next)))return null;result.add(next);}
        return List.copyOf(result);
    }
}
