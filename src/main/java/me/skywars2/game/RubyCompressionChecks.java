package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public final class RubyCompressionChecks {
    private RubyCompressionChecks() { }
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Ruby compression: "+why);}
    public static void run(GameController game){
        try(var f=new WoolBucketChecks.Fixture(game,false)){
            f.prepareParticipants();var at=f.home.add(0,20,0);var center=BlockPos.containing(at);
            for(var pos:BlockPos.betweenClosed(center.offset(-4,-4,-4),center.offset(4,4,4)))f.remember(pos);
            f.move(f.owner,at.add(0,0,-8));f.move(f.other,at.add(1,0,0));
            f.other.setNoGravity(false);f.other.setDeltaMovement(new Vec3(.9,-2,.3));
            f.audience.packets().clear();
            var method=LbcRuby.class.getDeclaredMethod("createHole",net.minecraft.server.level.ServerPlayer.class,Vec3.class);
            method.setAccessible(true);method.invoke(game.items().ruby(),f.owner,at);
            require(f.level.getBlockState(center).is(Blocks.BEDROCK)&&f.other.position().distanceToSqr(at)<1e-10,"actual compression clears sphere, places bedrock and captures player");
            var packets=f.audience.packets();int terrain=-1,teleport=-1,motion=-1;
            for(int i=0;i<packets.size();i++){
                var packet=packets.get(i);
                if(packet instanceof ClientboundSectionBlocksUpdatePacket)terrain=i;
                if(packet instanceof ClientboundPlayerPositionPacket)teleport=i;
                if(packet instanceof ClientboundSetEntityMotionPacket)motion=i;
            }
            require(terrain>=0&&teleport>terrain&&motion>teleport,"actual block snapshots precede teleport and explicit motion reset follows");
            require(f.other.getDeltaMovement().equals(Vec3.ZERO)&&!f.other.isNoGravity(),"capture clears stale falling velocity without freezing gravity");
            f.audience.acknowledge();
            f.audience.listener.handleMovePlayer(new ServerboundMovePlayerPacket.Rot(77,15,false,false));
            require(Math.abs(f.other.getYRot()-77)<.01&&Math.abs(f.other.getXRot()-15)<.01&&f.audience.ready(),"native teleport acknowledgment releases movement handler and accepts new camera rotation");
            SkyWars2.LOGGER.info("[check] PASS ruby-compression (native bedrock capture, ordered section snapshots/teleport/motion reset, real ACK and subsequent camera input)");
        }catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
    }
}
