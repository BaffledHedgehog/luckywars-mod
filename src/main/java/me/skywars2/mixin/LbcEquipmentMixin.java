package me.skywars2.mixin;

import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LbcEquipmentMixin {
    /** Нативный прыжок живёт в LivingEntity; броня реагирует без опроса статистики jump каждый тик. */
    @Inject(method = "jumpFromGround", at = @At("TAIL"))
    private void skywars2$overloadJump(CallbackInfo ci) {
        var game=SkyWars2.game();
        if(game!=null && (Object)this instanceof ServerPlayer player) {
            game.items().halloweenWind().jump(player);
            game.items().soulhunter().jump(player);
            game.items().mana().overload().armor().jump(player);
            game.items().jumpArtifacts().jump(player);
            game.items().shulkerArmor().jump(player);
            game.items().armorSets().techno().jump(player);
            game.items().armorSets().chaos().jump(player);
        }
    }
    /** Normalize old miniguns before vanilla snapshots and applies equipment attribute modifiers. */
    @Inject(method = "onEquipItem", at = @At("HEAD"))
    private void skywars2$minigunEquipment(EquipmentSlot slot, ItemStack previous, ItemStack next, CallbackInfo ci) {
        if ((Object)this instanceof ServerPlayer) me.skywars2.game.LbcMinigunMovement.ensureStack(next);
    }
    /** RETURN покрывает ранние выходы vanilla (firstTick/observer), в том числе начальную экипировку. */
    @Inject(method = "onEquipItem", at = @At("RETURN"))
    private void skywars2$rocketEquipment(EquipmentSlot slot, ItemStack previous, ItemStack next, CallbackInfo ci) {
        var game = SkyWars2.game();
        if(game!=null && (Object)this instanceof ServerPlayer player) {
            game.items().mana().inventoryChanged(player);
            if(slot==EquipmentSlot.HEAD)game.items().clownKit().effectsChanged(player);
            game.items().refreshMinigunMovement(player);
            game.items().shulkerArmor().inventoryChanged(player);
            game.items().armorSets().inventoryChanged(player);game.items().shishEncounter().inventoryChanged(player);
        }
        if (slot == EquipmentSlot.FEET && game != null && (Object) this instanceof ServerPlayer player) {
            game.items().rocketEquipment(player, next);
        }
    }
}
