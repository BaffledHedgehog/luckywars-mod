package me.skywars2.mixin;

import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Вызывает навык в точке consume_item, до удаления использованного предмета из стека. */
@Mixin(Consumable.class)
public abstract class ConsumableSkillMixin {
    @Inject(method = "onConsume", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/advancements/triggers/ConsumeItemTrigger;trigger(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/item/ItemStack;)V",
            shift = At.Shift.AFTER))
    private void skywars2$consumed(Level level, LivingEntity user, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
        var game = SkyWars2.game();
        if (game != null && user instanceof ServerPlayer player) game.consumed(player, stack);
    }
}
