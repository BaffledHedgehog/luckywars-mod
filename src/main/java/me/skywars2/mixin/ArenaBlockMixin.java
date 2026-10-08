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

/** Records arena changes, including bridges outside the original template bounds. */
@Mixin(Level.class)
public abstract class ArenaBlockMixin {
    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At("RETURN"))
    private void skywars2$trackBlock(BlockPos pos, BlockState state, int flags, int recursion,
                                    CallbackInfoReturnable<Boolean> callback) {
        GameController game = SkyWars2.game();
        if (callback.getReturnValueZ() && game != null && (Object) this == game.maps().level()
                && (game.playing() || game.stage() == GameController.Stage.ENDING)) {
            game.maps().recordBlockChange(pos);
        }
    }
}
