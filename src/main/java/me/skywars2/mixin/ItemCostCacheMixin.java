package me.skywars2.mixin;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentExactPredicate;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.trading.ItemCost;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Repair only an inherited cache conflict; the native positive predicate and wire schema stay intact. */
@Mixin(ItemCost.class)
public abstract class ItemCostCacheMixin {
    @Inject(method="<init>(Lnet/minecraft/core/Holder;ILnet/minecraft/core/component/DataComponentExactPredicate;)V",at=@At("RETURN"))
    private void skywars2$legalInheritedDamageCache(Holder<Item> item,int count,DataComponentExactPredicate components,CallbackInfo ci){
        var cached=((ItemCost)(Object)this).itemStack();
        // 26.3 patch.get requires a prototype. EMPTY distinguishes an explicitly
        // required positive value from a component inherited from the item default.
        if(cached.has(DataComponents.MAX_DAMAGE)&&cached.getMaxStackSize()>1
                &&components.asPatch().get(DataComponentMap.EMPTY,DataComponents.MAX_DAMAGE)==null)
            cached.remove(DataComponents.MAX_DAMAGE);
    }
}
