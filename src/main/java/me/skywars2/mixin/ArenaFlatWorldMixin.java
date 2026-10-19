package me.skywars2.mixin;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Reports the arena's flat terrain to clients without inheriting the overworld setting. */
@Mixin(ServerLevel.class)
public abstract class ArenaFlatWorldMixin {
    @Inject(method = "isFlat", at = @At("HEAD"), cancellable = true)
    private void skywars2$arenaIsFlat(CallbackInfoReturnable<Boolean> cir) {
        ServerLevel level = (ServerLevel) (Object) this;
        if (level.dimension().identifier().equals(Identifier.fromNamespaceAndPath("skywars2", "arena"))) {
            cir.setReturnValue(true);
        }
    }
}
