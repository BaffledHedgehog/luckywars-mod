package me.skywars2.mixin;

import me.skywars2.SkyWars2;
import me.skywars2.game.LbcImbaAxe;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Revision invalidation belongs to successful native writes, including direct chunk writes. */
@Mixin(LevelChunk.class)
public abstract class ImbaAxeBlockMixin {
    @Inject(method="setBlockState",at=@At("RETURN"))
    private void skywars2$axeDirty(BlockPos pos,BlockState state,int flags,CallbackInfoReturnable<BlockState> ci){
        var game=SkyWars2.game();
        if(game!=null&&ci.getReturnValue()!=null&&((LevelChunk)(Object)this).getLevel() instanceof ServerLevel level)
            game.items().imbaAxe().blockChanged(level,pos,state);
    }

    /** Only an active synchronous axe removal scope captures native container-ejected items. */
    @Mixin(ServerLevel.class)
    public abstract static class Drops {
        @Inject(method="addFreshEntity",at=@At("HEAD"),cancellable=true)
        private void skywars2$axeContents(Entity entity,CallbackInfoReturnable<Boolean> ci){
            if(LbcImbaAxe.captureDrop((ServerLevel)(Object)this,entity))ci.setReturnValue(true);
        }
    }
}
