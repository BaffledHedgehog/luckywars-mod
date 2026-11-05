package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.skywars2.game.LbcImbaNativeVariants;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.component.ChargedProjectiles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** One exact native firework variant follows vanilla held selection, factory, power and durability. */
public final class ImbaNativeVariantsMixin {
    private ImbaNativeVariantsMixin(){}
    @Mixin(ProjectileWeaponItem.class)
    public abstract static class HeldProjectile {
        // Verified 26.3 synthetic predicate; inventory selection still uses vanilla ARROW_ONLY.
        @WrapOperation(method="lambda$static$1",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/ItemStack;is(Ljava/lang/Object;)Z"))
        private static boolean skywars2$nativeHeldFirework(ItemStack stack,Object requested,Operation<Boolean> original){
            return original.call(stack,requested)||requested==Items.FIREWORK_ROCKET&&LbcImbaNativeVariants.firework(stack);
        }
    }
    @Mixin(CrossbowItem.class)
    public abstract static class Crossbow {
        @WrapOperation(method={"createProjectile","getDurabilityUse"},at=@At(value="INVOKE",target="Lnet/minecraft/world/item/ItemStack;is(Ljava/lang/Object;)Z"))
        private boolean skywars2$nativeFireworkFactoryAndWear(ItemStack stack,Object requested,Operation<Boolean> original){
            return original.call(stack,requested)||requested==Items.FIREWORK_ROCKET&&LbcImbaNativeVariants.firework(stack);
        }
    }
    @Mixin(ChargedProjectiles.class)
    public abstract static class Charged {
        // Native crossbow power and other native charged-rocket queries retain their own code.
        @WrapOperation(method="contains",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/ItemStackTemplate;is(Ljava/lang/Object;)Z"))
        private boolean skywars2$nativeChargedFirework(ItemStackTemplate stack,Object requested,Operation<Boolean> original){
            return original.call(stack,requested)||requested==Items.FIREWORK_ROCKET&&LbcImbaNativeVariants.firework(stack);
        }
    }
}
