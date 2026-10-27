package me.skywars2.mixin;

import me.skywars2.SkyWars2;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keep foreign native blindness grants separate from this temporary smoke layer. */
@Mixin(LivingEntity.class)
public abstract class ReactiveSmokeMixin {
    @Inject(method="addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",at=@At("HEAD"))
    private void skywars2$foreignBlindness(MobEffectInstance effect,Entity source,CallbackInfoReturnable<Boolean> ci){
        var game=SkyWars2.game();if(game!=null)game.items().reactiveArtifacts().externalBlindness((LivingEntity)(Object)this,effect);
    }
    @Inject(method="removeEffect",at=@At("HEAD"))
    private void skywars2$foreignBlindnessClear(net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect,CallbackInfoReturnable<Boolean> ci){
        if(!effect.is(net.minecraft.world.effect.MobEffects.BLINDNESS))return;
        var game=SkyWars2.game();if(game!=null)game.items().reactiveArtifacts().externalBlindnessRemoved((LivingEntity)(Object)this);
    }
    @Inject(method="removeAllEffects",at=@At("HEAD"))
    private void skywars2$foreignAllEffectsClear(CallbackInfoReturnable<Boolean> ci){
        var game=SkyWars2.game();if(game!=null)game.items().reactiveArtifacts().externalBlindnessRemoved((LivingEntity)(Object)this);
    }
    @Mixin(net.minecraft.world.entity.Mob.class)
    public static abstract class Target {
        @Inject(method="setTarget",at=@At("HEAD"),cancellable=true)
        private void skywars2$smokeTarget(LivingEntity target,org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci){
            var game=SkyWars2.game();if(game!=null&&!game.items().reactiveArtifacts().canTarget((net.minecraft.world.entity.Mob)(Object)this,target))ci.cancel();
        }
    }
}
