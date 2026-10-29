package me.skywars2.mixin;

import me.skywars2.game.MatchPlayerCheckpoint;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Intermediate body loading must never execute criterion rewards or functions. */
@Mixin(PlayerAdvancements.class)
public abstract class PlayerCheckpointAwardMixin {
    @Shadow private ServerPlayer player;
    @Inject(method="award",at=@At("HEAD"),cancellable=true)
    private void skywars2$noReplayRewards(AdvancementHolder holder,String criterion,CallbackInfoReturnable<Boolean> cir) {
        if(MatchPlayerCheckpoint.restoring(player))cir.setReturnValue(false);
    }
}
