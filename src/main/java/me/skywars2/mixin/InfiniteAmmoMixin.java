package me.skywars2.mixin;

import java.util.List;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.skywars2.game.LbcInfiniteAmmo;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;

/** Native draw access shared by instant minigun reloads and ordinary weapon charging. */
public final class InfiniteAmmoMixin {
    private InfiniteAmmoMixin(){}
    @Mixin(ProjectileWeaponItem.class)
    public abstract static class Economy {
        @WrapOperation(method="useAmmo",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/enchantment/EnchantmentHelper;processAmmoUse(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;I)I"))
        private static int skywars2$selectedInfiniteAmmo(ServerLevel level,ItemStack weapon,ItemStack selected,int count,Operation<Integer> original){
            // Vanilla's zero-cost branch makes the component-complete count1 copy intangible.
            return LbcInfiniteAmmo.conserves(selected)?0:original.call(level,weapon,selected,count);
        }
    }
    @Mixin(ProjectileWeaponItem.class)
    public interface DrawAccess {
        @Invoker("draw")
        static List<ItemStack> skywars2$draw(ItemStack weapon,ItemStack projectile,LivingEntity shooter){throw new AssertionError();}
    }
}
