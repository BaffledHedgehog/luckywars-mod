package me.skywars2.mixin;

import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Native falling-block fallback items retain cleanup identity without changing the drop itself. */
@Mixin(Entity.class)
public abstract class OpAnvilDropMixin {
    @Inject(method="spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/entity/item/ItemEntity;",
            at=@At("RETURN"))
    private void skywars2$anvilFallback(ServerLevel level, ItemStack stack, Vec3 offset,
                                       CallbackInfoReturnable<ItemEntity> cir) {
        var game = SkyWars2.game();
        if (game != null) game.items().opAnvilDrops().inheritDrop((Entity)(Object)this, cir.getReturnValue());
    }
}
