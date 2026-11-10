package me.skywars2.mixin;

import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Polymer's inter-block cooldown must elapse while the player is idle too. */
@Mixin(value=ServerPlayerGameMode.class,priority=900)
public abstract class PolymerMiningCadenceMixin {
    @Shadow private int gameTicks;
    @Unique private int skywars2$cooldownUntil;

    @Inject(method="destroyAndAck",at=@At("HEAD"))
    private void skywars2$miningCompleted(CallbackInfo ci){
        skywars2$cooldownUntil=gameTicks+5;
    }

    // SkyWarsMixinPlugin connects the already merged Polymer field reads here.
    // Mixins deliberately exclude other mixins' private handlers from selectors.
    @Unique
    private int skywars2$elapsedCooldown(int remaining){
        return Math.min(remaining,Math.max(0,skywars2$cooldownUntil-gameTicks));
    }
}
