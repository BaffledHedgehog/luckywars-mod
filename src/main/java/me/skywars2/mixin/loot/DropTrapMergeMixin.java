package me.skywars2.mixin.loot;

import me.skywars2.SkyWars2;
import me.skywars2.game.items.drops.LbcCageDropItem;
import me.skywars2.game.items.drops.LbcCorrosionDropItem;
import me.skywars2.game.items.drops.LbcCreeperDropItem;
import me.skywars2.game.items.drops.LbcFallingNetheriteDropItem;
import me.skywars2.game.items.drops.LbcHoleDropItem;
import me.skywars2.game.items.drops.LbcHomeItems;
import me.skywars2.game.items.drops.LbcScareDropItem;
import me.skywars2.game.items.drops.LbcTntDropItem;
import me.skywars2.game.items.drops.LbcZombieTntDropItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import me.skywars2.game.GameController;

/** Keep separate causal authors only when native code tries to merge trap tokens. */
@Mixin(ItemEntity.class)
public abstract class DropTrapMergeMixin {
    @Inject(method="tryToMerge",at=@At("HEAD"),cancellable=true)
    private void skywars2$preserveTrapAuthor(ItemEntity other,CallbackInfo ci) {
        var self=(ItemEntity)(Object)this;
        var stack=self.getItem();
        if(!stack.has(DataComponents.CUSTOM_DATA) || (!LbcTntDropItem.matches(stack) && LbcCreeperDropItem.kind(stack)==null && LbcCageDropItem.kind(stack)==null && LbcHoleDropItem.kind(stack)==null && LbcCorrosionDropItem.kind(stack)==null && !LbcZombieTntDropItem.matches(stack) && LbcFallingNetheriteDropItem.kind(stack)==null && !LbcHomeItems.matchesDrop(stack) && LbcScareDropItem.kind(stack)==null && !me.skywars2.game.items.drops.LbcUltraponosDropItem.matches(stack) && !me.skywars2.game.items.weapons.melee.LbcMetalPipe.matches(stack))
                || !ItemStack.isSameItemSameComponents(stack,other.getItem())) return;
        var game=SkyWars2.game();
        if(game!=null && !(me.skywars2.game.items.weapons.melee.LbcMetalPipe.matches(stack)
                ? game.items().metalPipe().canMerge(self,other) : game.items().specialDrops().canMerge(self,other))) ci.cancel();
    }
}
