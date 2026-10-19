package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Consumer;

/** Один proc на успешный выстрел лука/арбалета; Multishot сохраняет один общий бросок шанса. */
@Mixin(ProjectileWeaponItem.class)
public abstract class TrinketBowMixin {
    @WrapOperation(method = "shoot", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/entity/projectile/Projectile;spawnProjectile(Lnet/minecraft/world/entity/projectile/Projectile;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Ljava/util/function/Consumer;)Lnet/minecraft/world/entity/projectile/Projectile;"))
    private Projectile skywars2$spawned(Projectile projectile, ServerLevel level, ItemStack ammo,
                                       Consumer<Projectile> configure, Operation<Projectile> original,
                                       @Share("spawned") LocalBooleanRef spawned) {
        var result = original.call(projectile, level, ammo, configure);
        if (level.getEntity(result.getUUID()) == result) spawned.set(true);
        return result;
    }

    @Inject(method = "shoot", at = @At("RETURN"))
    private void skywars2$shot(ServerLevel level, LivingEntity shooter, InteractionHand hand, ItemStack weapon,
                               List<ItemStack> ammunition, float power, float uncertainty, boolean critical,
                               LivingEntity target, CallbackInfo ci, @Share("spawned") LocalBooleanRef spawned) {
        var game = SkyWars2.game();
        if (spawned.get() && game != null && ((Object) this instanceof BowItem || (Object) this instanceof CrossbowItem)
                && shooter instanceof ServerPlayer player) game.items().trinkets().shot(player);
    }
}
