package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Void and border damage ignore creative ability immunity, retaining the native damage pipeline. */
@Mixin(Player.class)
public abstract class CreativeVoidDamageMixin {
    @ModifyExpressionValue(method="hurtServer",at=@At(value="FIELD",target="Lnet/minecraft/world/entity/player/Abilities;invulnerable:Z"))
    private boolean skywars2$creativeEnvironmentalDamage(boolean invulnerable,@Local(argsOnly=true) DamageSource source){
        return invulnerable&&!(((Player)(Object)this).isCreative()
                &&(source.is(DamageTypes.FELL_OUT_OF_WORLD)||source.is(DamageTypes.OUTSIDE_BORDER)));
    }
}
