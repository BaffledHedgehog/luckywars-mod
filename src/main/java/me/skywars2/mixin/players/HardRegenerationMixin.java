package me.skywars2.mixin.players;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** One food/health policy regardless of world difficulty; mob difficulty stays native. */
@Mixin(ServerPlayer.class)
public abstract class HardRegenerationMixin {
    @Inject(method="tickRegeneration",at=@At("HEAD"),cancellable=true)
    private void skywars2$noPeacefulBonus(CallbackInfo ci) {
        // This native method contains only Peaceful's extra health, food and saturation.
        // Ordinary saturation/food regeneration still runs in FoodData.tick.
        ci.cancel();
    }

    @Mixin(FoodData.class)
    public abstract static class Food {
        @ModifyExpressionValue(method="tick",at=@At(value="INVOKE",
                target="Lnet/minecraft/server/level/ServerLevel;getDifficulty()Lnet/minecraft/world/Difficulty;"))
        private Difficulty skywars2$hardFoodPolicy(Difficulty difficulty) {
            // Includes Peaceful hunger depletion and starvation below Easy/Normal's HP floors.
            return Difficulty.HARD;
        }
    }
}
