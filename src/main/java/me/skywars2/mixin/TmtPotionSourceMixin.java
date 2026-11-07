package me.skywars2.mixin;

import me.skywars2.SkyWars2;
import me.skywars2.game.LbcTmtManaPotions;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** A native thrown potion receives its accepted private dose, not a copy of the whole physical TMT carrier. */
@Mixin(ThrowableItemProjectile.class)
public abstract class TmtPotionSourceMixin {
    @ModifyVariable(method="setItem",at=@At("HEAD"),argsOnly=true)
    private ItemStack skywars2$potionSource(ItemStack stack){
        var game=SkyWars2.game();return game!=null&&(Object)this instanceof AbstractThrownPotion
                ?LbcTmtManaPotions.projectileSource(game,stack):stack;
    }
}
