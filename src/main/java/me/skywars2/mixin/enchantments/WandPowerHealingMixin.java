package me.skywars2.mixin.enchantments;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.skywars2.game.combat.WeaponEnchantments;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

/** Scale only healing performed inside an attributed, captured wand spell scope. */
@Mixin(LivingEntity.class)
public abstract class WandPowerHealingMixin {
    @WrapMethod(method="heal")
    private void skywars2$wandPower(float amount,Operation<Void> original){
        original.call(WeaponEnchantments.scopedSpellHealing((LivingEntity)(Object)this,amount));
    }
}
