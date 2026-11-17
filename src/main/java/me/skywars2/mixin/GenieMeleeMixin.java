package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import me.skywars2.game.GenieCombat;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public abstract class GenieMeleeMixin {
    private static final String SOURCE = "Lnet/minecraft/world/entity/player/Player;createAttackSource("
        + "Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/damagesource/DamageSource;";

    @WrapOperation(method = "attack", at = @At(value = "INVOKE", target = SOURCE))
    private DamageSource skywars2$attackSource(Player attacker, ItemStack weapon, Operation<DamageSource> original) {
        var source = original.call(attacker, weapon);
        if (attacker instanceof ServerPlayer player) GenieCombat.beganMelee(player, InteractionHand.MAIN_HAND, weapon, source);
        return source;
    }

    @WrapOperation(method = "stabAttack", at = @At(value = "INVOKE", target = SOURCE))
    private DamageSource skywars2$stabSource(Player attacker, ItemStack weapon,
            Operation<DamageSource> original, @Local(argsOnly = true) EquipmentSlot slot) {
        var source = original.call(attacker, weapon);
        if (attacker instanceof ServerPlayer player) {
            var hand = slot == EquipmentSlot.OFFHAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            GenieCombat.beganMelee(player, hand, weapon, source);
        }
        return source;
    }
}
