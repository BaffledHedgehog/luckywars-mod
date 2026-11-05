package me.skywars2.game;

import java.util.List;
import me.skywars2.mixin.InfiniteAmmoMixin;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.FireworkRocketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;

/** Selected-arrow native draw economy, plus the source trade's separate inventory flag query. */
public final class LbcInfiniteAmmo {
    private LbcInfiniteAmmo(){}
    public static boolean matches(ItemStack stack){return ItemData.intValue(stack,"infinite_arrow",0)==1;}
    /** Only the selected arrow's own trait is free; unrelated inventory flags and rockets do not qualify. */
    public static boolean conserves(ItemStack selected){return !selected.isEmpty()&&!(selected.getItem() instanceof FireworkRocketItem)&&selected.is(ItemTags.ARROWS)&&matches(selected);}
    public static boolean has(ServerPlayer owner){
        return owner.getInventory().clearOrCountMatchingItems(LbcInfiniteAmmo::matches,true,0,owner.inventoryMenu.getCraftSlots())>0;
    }
    /** Source plain-arrow selectors also recognize the one native replacement of that arrow. */
    public static boolean plainArrow(ItemStack stack){return stack.is(Items.ARROW)||stack.is(LbcImbaNativeItems.INFINITE_ARROW);}
    public static ItemStack selectedPlainArrow(ServerPlayer owner){
        var held=ProjectileWeaponItem.getHeldProjectile(owner,LbcInfiniteAmmo::plainArrow);if(!held.isEmpty())return held;
        for(int slot=0;slot<owner.getInventory().getContainerSize();slot++){var stack=owner.getInventory().getItem(slot);if(plainArrow(stack))return stack;}
        return ItemStack.EMPTY;
    }
    /** Multishot copies, actual debit and intangible free projectiles remain native behavior. */
    public static List<ItemStack> drawReload(ServerPlayer owner,ItemStack weapon,ItemStack selectedAmmo){
        return InfiniteAmmoMixin.DrawAccess.skywars2$draw(weapon,selectedAmmo,owner);
    }
}
