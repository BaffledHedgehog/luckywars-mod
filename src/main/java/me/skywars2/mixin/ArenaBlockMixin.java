package me.skywars2.mixin;

import me.skywars2.SkyWars2;
import me.skywars2.game.GameController;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Записывает изменения блоков арены, включая мосты за границами исходного шаблона карты. */
@Mixin(Level.class)
public abstract class ArenaBlockMixin {
    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At("HEAD"), cancellable = true)
    private void skywars2$mapOverlayWrite(BlockPos pos, BlockState state, int flags, int recursion,
                                          CallbackInfoReturnable<Boolean> callback) {
        var game = SkyWars2.game();
        if((Object)this instanceof net.minecraft.server.level.ServerLevel source&&!me.skywars2.game.LbcBedwarsRoleGuards.publication(source)){callback.setReturnValue(false);return;}
        var arena=me.skywars2.game.LbcBedwarsArena.at((Object)this);
        if(arena!=null&&!arena.allowsType((Object)this,pos,state)){callback.setReturnValue(false);return;}
        if(arena!=null)arena.captureUnchanged((Object)this,pos,state);
        if(game!=null && (Object)this instanceof net.minecraft.server.level.ServerLevel level){
            me.skywars2.game.LbcBadApple.blockChanging(level,pos,state);
            game.items().indirectKills().beforeBlockChange(level,pos,state);
        }
        // Даже запись одинакового state сообщает, что клетка принадлежит карте под временным лобби.
        if (game != null) game.maps().noteMapBlockWrite((Level)(Object)this, pos);
    }
    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At("RETURN"))
    private void skywars2$trackBlock(BlockPos pos, BlockState state, int flags, int recursion,
                                    CallbackInfoReturnable<Boolean> callback) {
        GameController game = SkyWars2.game();
        if(callback.getReturnValueZ() && game!=null && (Object)this instanceof net.minecraft.server.level.ServerLevel level){
            game.items().indirectKills().blockChanged(level,pos,state);
            var arena=me.skywars2.game.LbcBedwarsArena.at(level);
            if(arena!=null)arena.blockChanged(level,pos,state);
            if(game.bedwars()!=null)game.bedwars().auxiliaries().blockChanged(level,pos,state);
        }
        if (callback.getReturnValueZ() && game != null && (Object) this == game.maps().level()) {
            game.maps().recordBlockChange(pos);
            game.items().magic().blockChanged(pos, state);
            game.items().bedrock().blockChanged(pos, state);
            game.items().tanner().blockChanged(pos, state);
            game.items().kiuas().blockChanged(pos, state);
        }
    }
}
