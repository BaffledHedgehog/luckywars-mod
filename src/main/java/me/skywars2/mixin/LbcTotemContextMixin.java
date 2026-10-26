package me.skywars2.mixin;

import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The used_totem criterion has no damage argument; retain the actual native attempt through its reward. */
@Mixin(LivingEntity.class)
public abstract class LbcTotemContextMixin {
    @Inject(method="checkTotemDeathProtection",at=@At("HEAD"))
    private void skywars2$beginTotem(DamageSource source,CallbackInfoReturnable<Boolean> ci){
        var game=SkyWars2.game();if(game!=null&&(Object)this instanceof ServerPlayer p)game.items().totems().begin(p,source);
    }
    @Inject(method="checkTotemDeathProtection",at=@At("RETURN"))
    private void skywars2$endTotem(DamageSource source,CallbackInfoReturnable<Boolean> ci){
        var game=SkyWars2.game();if(game!=null&&(Object)this instanceof ServerPlayer p)game.items().totems().end(p);
    }
}
