package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.skywars2.game.WeaponEnchantments;
import net.minecraft.core.Holder;
import net.minecraft.server.level.*;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.functions.EnchantedCountIncreaseFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceWithEnchantedBonusCondition;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.spongepowered.asm.mixin.Mixin;

public final class WeaponEnchantmentsMixin {
    private WeaponEnchantmentsMixin(){}
    @Mixin(ServerPlayerGameMode.class)
    public abstract static class ItemUse {
        @WrapMethod(method="useItem")
        private InteractionResult skywars2$usedWeapon(ServerPlayer player,Level level,ItemStack stack,InteractionHand hand,Operation<InteractionResult> original){
            try(var scope=WeaponEnchantments.using(player,stack)){return original.call(player,level,stack,hand);}
        }
        @WrapMethod(method="useItemOn")
        private InteractionResult skywars2$usedOnWeapon(ServerPlayer player,Level level,ItemStack stack,InteractionHand hand,
                net.minecraft.world.phys.BlockHitResult hit,Operation<InteractionResult> original){
            try(var scope=WeaponEnchantments.using(player,stack)){return original.call(player,level,stack,hand,hit);}
        }
    }
    @Mixin(LivingEntity.class)
    public abstract static class EntityLoot {
        @WrapMethod(method="dropFromLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;ZLnet/minecraft/resources/ResourceKey;Ljava/util/function/Consumer;)V")
        private void skywars2$lootWeapon(ServerLevel level,DamageSource source,boolean playerKilled,
                net.minecraft.resources.ResourceKey<LootTable> table,java.util.function.Consumer<ItemStack> output,Operation<Void> original){
            try(var scope=WeaponEnchantments.loot((LivingEntity)(Object)this,source)){original.call(level,source,playerKilled,table,output);}
        }
    }
    @Mixin(EnchantedCountIncreaseFunction.class)
    public abstract static class LootCount {
        @WrapOperation(method="run",at=@org.spongepowered.asm.mixin.injection.At(value="INVOKE",target=
                "Lnet/minecraft/world/level/storage/loot/LootContext;getOptional(Lnet/minecraft/util/context/ContextKey;)Ljava/lang/Object;"))
        private <T>T skywars2$creditedCount(LootContext context,ContextKey<T> key,Operation<T> original){
            T actual=original.call(context,key);
            return key==LootContextParams.ATTACKING_ENTITY?(T)WeaponEnchantments.lootAttacker(context,(Entity)actual):actual;
        }
    }
    @Mixin(LootItemRandomChanceWithEnchantedBonusCondition.class)
    public abstract static class LootChance {
        @WrapOperation(method="test(Lnet/minecraft/world/level/storage/loot/LootContext;)Z",at=@org.spongepowered.asm.mixin.injection.At(value="INVOKE",target=
                "Lnet/minecraft/world/level/storage/loot/LootContext;getOptional(Lnet/minecraft/util/context/ContextKey;)Ljava/lang/Object;"))
        private <T>T skywars2$creditedChance(LootContext context,ContextKey<T> key,Operation<T> original){
            T actual=original.call(context,key);
            return key==LootContextParams.ATTACKING_ENTITY?(T)WeaponEnchantments.lootAttacker(context,(Entity)actual):actual;
        }
    }
    @Mixin(EnchantmentHelper.class)
    public abstract static class LootLevel {
        @WrapMethod(method="getEnchantmentLevel")
        private static int skywars2$sourceLootLevel(Holder<Enchantment> enchantment,LivingEntity attacker,Operation<Integer> original){
            return WeaponEnchantments.lootLevel(enchantment,attacker,original.call(enchantment,attacker));
        }
        @WrapMethod(method="modifyDamage")
        private static float skywars2$singleDamage(ServerLevel level,ItemStack weapon,Entity victim,DamageSource source,float amount,Operation<Float> original){
            float result=amount<=0&&me.skywars2.game.VanillaExpansionRules.enabled()?amount:original.call(level,weapon,victim,source,amount);
            WeaponEnchantments.prepared(source,victim,weapon);return result;
        }
        @WrapMethod(method="doPostAttackEffectsWithItemSourceOnBreak")
        private static void skywars2$singlePost(ServerLevel level,Entity victim,DamageSource source,ItemStack weapon,
                java.util.function.Consumer<ItemStack> onBreak,Operation<Void> original){
            if(WeaponEnchantments.post(source,victim))original.call(level,victim,source,weapon,onBreak);
        }
        @WrapMethod(method="modifyKnockback")
        private static float skywars2$singleKnockback(ServerLevel level,ItemStack weapon,Entity victim,DamageSource source,float amount,Operation<Float> original){
            return WeaponEnchantments.knockback(source,victim)?original.call(level,weapon,victim,source,amount):amount;
        }
    }
    @Mixin(DamageSource.class)
    public abstract static class SourceWeapon {
        @WrapMethod(method="getWeaponItem")
        private ItemStack skywars2$actualWeapon(Operation<ItemStack> original){return WeaponEnchantments.scopedWeapon((DamageSource)(Object)this,original.call());}
    }
}
