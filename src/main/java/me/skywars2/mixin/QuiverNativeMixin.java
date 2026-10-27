package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.skywars2.SkyWars2;
import me.skywars2.game.LbcQuiver;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Ordinary bow/crossbow arrows retain native physics and their exact pickup item components. */
@Mixin(Arrow.class)
public abstract class QuiverNativeMixin {
    @Inject(method="tick",at=@At("HEAD"),cancellable=true)
    private void skywars2$quiverTick(CallbackInfo ci){var game=SkyWars2.game();if(game!=null&&((Arrow)(Object)this).level() instanceof ServerLevel&&!game.items().quiver().beforeArrow((Arrow)(Object)this))ci.cancel();}
    @WrapMethod(method="doPostHurtEffects")
    private void skywars2$ordinaryArrowTransport(LivingEntity target,Operation<Void> original){
        var arrow=(Arrow)(Object)this;var game=SkyWars2.game();if(game==null||!LbcQuiver.custom(arrow)){original.call(target);return;}
        if(!me.skywars2.game.LbcDimensionItems.ordinary(arrow.level()))return;
        var previous=LbcQuiver.beginEffects(arrow);try{original.call(target);}finally{LbcQuiver.endEffects(previous);}
    }
    @Mixin(AbstractArrow.class)
    public static abstract class Block {
        @Inject(method="onHitBlock",at=@At("TAIL"))
        private void skywars2$ordinaryArrowGround(BlockHitResult hit,CallbackInfo ci){var game=SkyWars2.game();if(game!=null&&(Object)this instanceof Arrow arrow&&arrow.level() instanceof ServerLevel)game.items().quiver().ground(arrow);}
    }
}
