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
    /** Loot children must carry the author before ENTITY_LOAD dispatches a drop token. */
    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(
            method="spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/entity/item/ItemEntity;",
            at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean skywars2$ownedLoot(ServerLevel level,Entity child,
            com.llamalad7.mixinextras.injector.wrapoperation.Operation<Boolean> original){
        var game=SkyWars2.game();
        if(game!=null&&child instanceof ItemEntity item){
            game.items().chaosTrapActors().inheritDrop((Entity)(Object)this,item);
            var author=game.items().spellSources().owner((Entity)(Object)this);
            if(author!=null){item.setThrower(author);item.entityTags().add(game.items().epoch());}
        }
        return original.call(level,child);
    }
    @Inject(method="spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/entity/item/ItemEntity;",
            at=@At("RETURN"))
    private void skywars2$anvilFallback(ServerLevel level, ItemStack stack, Vec3 offset,
                                       CallbackInfoReturnable<ItemEntity> cir) {
        var game = SkyWars2.game();
        if (game != null) game.items().opAnvilDrops().inheritDrop((Entity)(Object)this, cir.getReturnValue());
    }
}
