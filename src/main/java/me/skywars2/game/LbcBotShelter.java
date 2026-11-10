package me.skywars2.game;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** Finite, camera-visible native cover, shared by ranged defense and imminent TNT defense. */
final class LbcBotShelter {
    final ServerPlayer player;final LbcChefBrain.Driver driver;final LbcBotSurvival survival;
    BlockPos at;List<BlockPos> cells=List.of();int stage;
    LbcBotShelter(ServerPlayer player,LbcChefBrain.Driver driver,LbcBotSurvival survival){this.player=player;this.driver=driver;this.survival=survival;}
    void clear(){at=null;cells=List.of();stage=0;}
    boolean urgent(){return at==null||stage<cells.size();}
    String diagnostics(){return "shelter="+at+" shelterStage="+stage+"/"+cells.size();}
    Boolean tick(Vec3 danger){
        if(at==null||player.position().subtract(Vec3.atBottomCenterOf(at)).horizontalDistanceSqr()>4){
            if(!survival.brain.grounded()||!driver.hasBlocks())return null;
            int height=(int)Math.ceil(player.getBoundingBox().maxY)-player.blockPosition().getY();if(height>4)return null;
            at=player.blockPosition().immutable();stage=0;
            var front=Direction.getApproximateNearest(danger.subtract(player.position()).multiply(1,0,1));
            var plan=new ArrayList<BlockPos>();
            for(int y=0;y<height;y++)plan.add(at.relative(front).above(y));
            // Stop upward blast movement early. Both support and roof still need real visible faces.
            plan.add(at.relative(front).above(height));plan.add(at.above(height));
            // Four cardinal walls close the body. Outside corners are unnecessary
            // and their attachment faces can become hidden behind completed walls.
            for(int y=0;y<height;y++)for(var side:Direction.Plane.HORIZONTAL)
                if(side!=front)plan.add(at.relative(side).above(y));
            cells=List.copyOf(plan);
        }
        for(int i=0;i<stage;i++)if(player.level().getBlockState(cells.get(i)).getCollisionShape(player.level(),cells.get(i)).isEmpty()){
            stage=i;break;
        }
        driver.halt();player.setSprinting(false);driver.crouch(false);
        var center=Vec3.atBottomCenterOf(at);
        if(player.onGround()&&player.position().subtract(center).horizontalDistanceSqr()>.0625){
            driver.steer(center,false,stage<cells.size()?Vec3.atCenterOf(cells.get(stage)):danger);return true;
        }
        while(stage<cells.size()){
            var cell=cells.get(stage);
            if(!player.level().getBlockState(cell).getCollisionShape(player.level(),cell).isEmpty()){stage++;continue;}
            if(!driver.hasBlocks())return null;
            boolean upper=cell.getY()>=at.getY()+2;
            if(cell.equals(at.above(2))&&!player.onGround())return true;
            boolean placed=survival.place(cell);
            // Rotating to an already visible face is not a reason to jump away
            // from it. The ceiling over our own head is reachable from the floor.
            if(!placed&&!survival.placementWaiting&&upper&&!cell.equals(at.above(2))&&player.onGround())driver.pillarJump();
            return true;
        }
        return false;
    }
}
