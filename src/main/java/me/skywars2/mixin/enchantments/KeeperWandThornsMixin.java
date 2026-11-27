package me.skywars2.mixin.enchantments;

import me.skywars2.game.combat.WeaponEnchantments;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentTarget;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A floating wand cannot take Thorns damage; player authorship still drives every other mechanic. */
@Mixin(Enchantment.class)
public abstract class KeeperWandThornsMixin {
    @Inject(method="doPostAttack(Lnet/minecraft/server/level/ServerLevel;ILnet/minecraft/world/item/enchantment/EnchantedItemInUse;Lnet/minecraft/world/item/enchantment/EnchantmentTarget;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;)V",
            at=@At("HEAD"),cancellable=true)
    private void skywars2$keeperCannotReceiveThorns(ServerLevel level,int enchantmentLevel,EnchantedItemInUse item,
            EnchantmentTarget forTarget,Entity victim,DamageSource source,CallbackInfo callback){
        if(forTarget==EnchantmentTarget.VICTIM
                &&(Object)this==level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.THORNS).value()
                &&WeaponEnchantments.keeperEmitterAttack(source))callback.cancel();
    }
}
