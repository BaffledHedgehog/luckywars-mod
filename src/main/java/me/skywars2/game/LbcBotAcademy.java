package me.skywars2.game;

import java.util.Comparator;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Teacher visits use the same native entity interaction and trial owner as human players. */
final class LbcBotAcademy {
    final ServerPlayer player;final LbcChefBrain.Driver driver;final LbcChefBrain brain;final Consumer<Vec3> destination;
    Entity teacher;Vec3 resume;int nextScan,deadline;
    LbcBotAcademy(ServerPlayer player,LbcChefBrain.Driver driver,LbcChefBrain brain,Consumer<Vec3> destination){this.player=player;this.driver=driver;this.brain=brain;this.destination=destination;}
    void cancel(){teacher=null;if(resume!=null){destination.accept(resume);resume=null;brain.reset();}}
    boolean tick(boolean interrupted){
        if(interrupted){cancel();return false;}
        if(teacher==null&&player.tickCount<nextScan)return false;
        if(teacher==null)nextScan=player.tickCount+40;
        var academy=driver.items().academy();
        int subject=-1;for(int i=0;i<36;i++)if(academy.canStudy(player.getInventory().getItem(i))){subject=i;break;}
        if(subject<0){cancel();return false;}
        if(teacher!=null&&(!academy.acceptsStudent(teacher)||player.tickCount>=deadline)){cancel();nextScan=player.tickCount+100;}
        if(teacher==null){
            teacher=player.level().getEntities(player,player.getBoundingBox().inflate(32),academy::acceptsStudent).stream()
                    .min(Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
            if(teacher==null)return false;resume=driver.destination();deadline=player.tickCount+300;brain.reset();
        }
        destination.accept(teacher.position());
        if(player.distanceToSqr(teacher)>player.entityInteractionRange()*player.entityInteractionRange()){
            if(!brain.closeGround(teacher.position()))brain.tick();return true;
        }
        if(!driver.equip(player.getInventory().getItem(subject))||!driver.aimAction(teacher.getBoundingBox().getCenter()))return true;
        player.connection.handleInteract(new net.minecraft.network.protocol.game.ServerboundInteractPacket(teacher.getId(),InteractionHand.MAIN_HAND,
                teacher.getBoundingBox().getCenter().subtract(teacher.position()),false));
        cancel();nextScan=player.tickCount+40;return true;
    }
}
