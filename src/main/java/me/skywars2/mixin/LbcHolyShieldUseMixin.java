package me.skywars2.mixin;

import me.skywars2.SkyWars2;
import me.skywars2.game.LbcHolyShield;
import net.minecraft.advancements.triggers.UsingItemTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Same native using_item boundary as the legacy shield criterion, without function recursion. */
@Mixin(UsingItemTrigger.class)
public abstract class LbcHolyShieldUseMixin {
    @Inject(method="trigger(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/item/ItemStack;)V",at=@At("TAIL"))
    private void skywars2$holyShield(ServerPlayer player,ItemStack stack,CallbackInfo ci){
        var game=SkyWars2.game();if(game==null)return;
        if(LbcHolyShield.matches(stack))LbcHolyShield.using(game,player,stack);
        game.items().edibleSpells().using(player,stack);
    }
}
