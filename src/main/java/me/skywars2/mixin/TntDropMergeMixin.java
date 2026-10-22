package me.skywars2.mixin;

import me.skywars2.SkyWars2;
import me.skywars2.game.LbcTntDropItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep separate causal authors only when native code tries to merge TNT trap tokens. */
@Mixin(ItemEntity.class)
public abstract class TntDropMergeMixin {
    @Inject(method="tryToMerge",at=@At("HEAD"),cancellable=true)
    private void skywars2$preserveTrapAuthor(ItemEntity other,CallbackInfo ci) {
        var self=(ItemEntity)(Object)this;
        var stack=self.getItem();
        if(!stack.has(DataComponents.CUSTOM_DATA) || !LbcTntDropItem.matches(stack)
                || !ItemStack.isSameItemSameComponents(stack,other.getItem())) return;
        var game=SkyWars2.game();
        if(game!=null && !game.items().tntDrops().canMerge(self,other)) ci.cancel();
    }
}
