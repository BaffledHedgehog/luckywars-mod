package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Capture raw hits before damage modifiers, then replay through the complete native pipeline. */
public final class TimeStopDamageMixin {
    private TimeStopDamageMixin() { }

    @Mixin(ServerPlayer.class)
    public abstract static class Players {
        @WrapMethod(method="hurtServer")
        private boolean skywars2$queueStoppedPlayerDamage(ServerLevel level,DamageSource source,float amount,
                                                         Operation<Boolean> original) {
            var game=SkyWars2.game();
            if(game!=null && !game.items().timeStop().replaying(source)
                    && game.items().timeStop().queueDamage((ServerPlayer)(Object)this,level,source,amount)) return true;
            return original.call(level,source,amount);
        }
    }

    @Mixin(LivingEntity.class)
    public abstract static class Living {
        @WrapMethod(method="hurtServer")
        private boolean skywars2$queueStoppedLivingDamage(ServerLevel level,DamageSource source,float amount,
                                                         Operation<Boolean> original) {
            var game=SkyWars2.game();
            if(game!=null && !game.items().timeStop().replaying(source)
                    && game.items().timeStop().queueDamage((LivingEntity)(Object)this,level,source,amount)) return true;
            return original.call(level,source,amount);
        }
    }

    @Mixin(DamageSource.class)
    public abstract static class ReplaySource {
        @Inject(method="is(Lnet/minecraft/tags/TagKey;)Z",at=@At("HEAD"),cancellable=true)
        private void skywars2$queuedHitCooldown(TagKey<DamageType> tag,CallbackInfoReturnable<Boolean> ci) {
            var game=SkyWars2.game();
            if(tag==DamageTypeTags.BYPASSES_COOLDOWN && game!=null
                    && game.items().timeStop().replaying((DamageSource)(Object)this)) ci.setReturnValue(true);
        }
    }
}
