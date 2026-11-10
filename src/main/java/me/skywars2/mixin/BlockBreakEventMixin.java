package me.skywars2.mixin;

import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polymer.core.api.block.PolymerBlockUtils;
import eu.pb4.polymer.core.api.utils.PolymerSyncedObject;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

/** Polymer26.3 rewrites the particles-only2014 event; native audio+particles2001 needs the same projection. */
@Mixin(ClientboundLevelEventPacket.class)
public abstract class BlockBreakEventMixin {
    @Shadow @Final private int type;
    @ModifyArg(method="write",at=@At(value="INVOKE",target="Lnet/minecraft/network/FriendlyByteBuf;writeInt(I)Lnet/minecraft/network/FriendlyByteBuf;",ordinal=1))
    private int skywars2$audibleBreakState(int data){
        if(type!=LevelEvent.PARTICLES_AND_SOUND_DESTROY_BLOCK)return data;
        var state=Block.stateById(data);
        if(!(PolymerSyncedObject.getSyncedObject(BuiltInRegistries.BLOCK,state.getBlock()) instanceof PolymerBlock block))return data;
        var context=PacketContext.get();
        state=PolymerBlockUtils.getBlockBreakBlockStateSafely(block,state,PolymerBlockUtils.NESTED_DEFAULT_DISTANCE,context);
        return Block.getId(PolymerBlockUtils.getPolymerBlockState(state,context));
    }
}
