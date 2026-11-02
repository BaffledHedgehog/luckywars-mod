package me.skywars2.mixin;

import me.skywars2.SkyWars2;
import me.skywars2.game.LbcFpvDrone;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public final class FpvDroneMixin {
    private FpvDroneMixin(){}
    /** Consume only voluntary client movement; actual body gravity/knockback stay server-simulated. */
    @Mixin(value=ServerGamePacketListenerImpl.class,priority=1200)
    public abstract static class Connection {
        @Shadow public ServerPlayer player;
        @Inject(method={"handleUseItem","handleUseItemOn","handleInteract","handlePlayerAction","handlePlayerCommand",
                "handlePlayerAbilities","handleMoveVehicle","handleContainerClick","handleContainerButtonClick","handleSetCreativeModeSlot"},
                at=@At(value="INVOKE",target="Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",shift=At.Shift.AFTER),cancellable=true)
        private void fpvActions(@Coerce net.minecraft.network.protocol.Packet<?> packet,CallbackInfo ci){var g=SkyWars2.game();if(g!=null&&g.items().fpvDrone().controlling(player))ci.cancel();}
    }
    @Mixin(net.minecraft.server.network.ServerCommonPacketListenerImpl.class)
    public abstract static class Presentation {
        @org.spongepowered.asm.mixin.injection.ModifyVariable(method="send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V",at=@At("HEAD"),argsOnly=true)
        private net.minecraft.network.protocol.Packet<?> fpvPresentation(net.minecraft.network.protocol.Packet<?> packet){
            if((Object)this instanceof ServerGamePacketListenerImpl connection){var game=SkyWars2.game();if(game!=null)return game.items().fpvDrone().presentation(connection.player,packet);}return packet;
        }
    }
    /** Move the existing native player ticket/view, without moving the real player entity. */
    @Mixin(ChunkMap.class)
    public abstract static class Chunks {
        @Redirect(method={"move","updatePlayerPos","updatePlayerStatus"},at=@At(value="INVOKE",target=
                "Lnet/minecraft/core/SectionPos;of(Lnet/minecraft/world/level/entity/EntityAccess;)Lnet/minecraft/core/SectionPos;"))
        private SectionPos fpvSection(EntityAccess entity){return entity instanceof ServerPlayer p?SectionPos.of(BlockPos.containing(LbcFpvDrone.trackingPosition(p))):SectionPos.of(entity);}
        @Redirect(method="updateChunkTracking",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerPlayer;chunkPosition()Lnet/minecraft/world/level/ChunkPos;"))
        private ChunkPos fpvChunk(ServerPlayer p){var at=BlockPos.containing(LbcFpvDrone.trackingPosition(p));return new ChunkPos(at.getX()>>4,at.getZ()>>4);}
    }
    @Mixin(targets="net.minecraft.server.level.ChunkMap$TrackedEntity")
    public abstract static class Entities {
        @Redirect(method="updatePlayer",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerPlayer;position()Lnet/minecraft/world/phys/Vec3;"))
        private Vec3 fpvEntityView(ServerPlayer p){return LbcFpvDrone.trackingPosition(p);}
    }
    /** These are packet audience checks only; damage and physical block interactions use the body. */
    @Mixin(net.minecraft.server.players.PlayerList.class)
    public abstract static class Sounds {
        @Redirect(method="broadcast",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerPlayer;getX()D"))
        private double fpvSoundX(ServerPlayer p){return LbcFpvDrone.viewpoint(p).x;}
        @Redirect(method="broadcast",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerPlayer;getY()D"))
        private double fpvSoundY(ServerPlayer p){return LbcFpvDrone.viewpoint(p).y;}
        @Redirect(method="broadcast",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerPlayer;getZ()D"))
        private double fpvSoundZ(ServerPlayer p){return LbcFpvDrone.viewpoint(p).z;}
    }
    @Mixin(net.minecraft.server.level.ServerLevel.class)
    public abstract static class Effects {
        @Redirect(method="sendParticles(Lnet/minecraft/server/level/ServerPlayer;ZDDDLnet/minecraft/network/protocol/Packet;)Z",
                at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerPlayer;blockPosition()Lnet/minecraft/core/BlockPos;"))
        private BlockPos fpvParticles(ServerPlayer p){return BlockPos.containing(LbcFpvDrone.viewpoint(p));}
        @Redirect(method="explode",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerPlayer;distanceToSqr(Lnet/minecraft/world/phys/Vec3;)D"))
        private double fpvExplosionPacket(ServerPlayer p,Vec3 center){return LbcFpvDrone.viewpoint(p).distanceToSqr(center);}
    }
}
