package me.skywars2.mixin;

import java.util.List;
import java.util.function.Consumer;
import me.skywars2.game.LbcAcademy;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Interaction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public interface AcademyInteractionMixin {
    @Mixin(Interaction.class)
    interface Data {
        @Accessor("DATA_WIDTH_ID") static EntityDataAccessor<Float> skywars2$width(){throw new AssertionError();}
        @Accessor("DATA_HEIGHT_ID") static EntityDataAccessor<Float> skywars2$height(){throw new AssertionError();}
    }
    @Mixin(ServerEntity.class)
    abstract class Pairing {
        @Shadow @Final private Entity entity;
        @Inject(method="sendPairingData",at=@At("TAIL"))
        private void skywars2$transparentTeacher(ServerPlayer player,Consumer<Packet<ClientGamePacketListener>> sink,CallbackInfo ci){
            if(LbcAcademy.transparentInteraction(entity)){
                LbcAcademy.publishInteraction(entity);
                sink.accept(new ClientboundSetEntityDataPacket(entity.getId(),LbcAcademy.transparentMetadata(entity.getId(),List.of())));
            }
        }
    }
    @Mixin(ClientboundSetEntityDataPacket.class)
    abstract class Metadata {
        @Shadow public abstract int id();
        @ModifyArg(method="write",at=@At(value="INVOKE",target="Lnet/minecraft/network/protocol/game/ClientboundSetEntityDataPacket;pack(Ljava/util/List;Lnet/minecraft/network/RegistryFriendlyByteBuf;)V"),index=0)
        private List<SynchedEntityData.DataValue<?>> skywars2$keepTransparent(List<SynchedEntityData.DataValue<?>> values){return LbcAcademy.transparentMetadata(id(),values);}
    }
}
