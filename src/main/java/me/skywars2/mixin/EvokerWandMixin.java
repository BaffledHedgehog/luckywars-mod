package me.skywars2.mixin;

import me.skywars2.SkyWars2;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.EvokerFangs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep native animation/timing while applying the shared wand hit policy to our fangs. */
@Mixin(EvokerFangs.class)
public abstract class EvokerWandMixin {
    @Inject(method="dealDamageTo",at=@At("HEAD"),cancellable=true)
    private void skywars2$wandHit(LivingEntity target,CallbackInfo ci){
        var fang=(EvokerFangs)(Object)this;
        if(!fang.entityTags().contains("skywars2.evoker_wand"))return;
        var game=SkyWars2.game();if(game!=null)game.items().evokerBass().fangHit(fang,target);
        ci.cancel();
    }
}
