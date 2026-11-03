package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import me.skywars2.SkyWars2;
import me.skywars2.game.LbcSpecialEggs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PostSpawnProcessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Optional;

@Mixin(EntityType.class)
public abstract class SpecialEggMixin {
    // EntityType.create applies this chain after finalizeSpawn and before world insertion.
    // Appending preserves vanilla ENTITY_DATA loading and exposes causal state to ENTITY_LOAD.
    @Inject(method="appendCustomEntityStackConfig",at=@At("RETURN"),cancellable=true)
    private static <T extends Entity> void skywars2$adoptSpecialEgg(PostSpawnProcessor<T> initial,Level level,
            ItemStack stack,LivingEntity user,CallbackInfoReturnable<PostSpawnProcessor<T>> cir) {
        if(stack.getItem() instanceof SpawnEggItem) cir.setReturnValue(cir.getReturnValue()
                .andThen(entity->LbcSpecialEggs.adopt(SkyWars2.game(),entity,user,stack)));
    }

    @Mixin(SpawnEggItem.class)
    public abstract static class Offspring {
        @Inject(method="spawnOffspringFromSpawnEgg",at=@At("HEAD"),cancellable=true)
        private static void skywars2$offspringAdmission(Player player,Mob parent,EntityType<? extends Mob> type,
                ServerLevel level,Vec3 pos,ItemStack stack,CallbackInfoReturnable<Optional<Mob>> cir) {
            if(LbcSpecialEggs.matches(stack) && !LbcSpecialEggs.allowed(SkyWars2.game(),player))
                cir.setReturnValue(Optional.empty());
        }

        // (user) Matching-mob use must apply the same egg payload and author as block/fluid use.
        // Vanilla still chooses the child, checks baby support and owns insertion/one-unit debit.
        @WrapOperation(method="spawnOffspringFromSpawnEgg",at=@At(value="INVOKE",target=
                "Lnet/minecraft/server/level/ServerLevel;addFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)V"))
        private static void skywars2$offspringPayload(ServerLevel level,Entity child,Operation<Void> original,
                @Local(argsOnly=true) Player player,@Local(argsOnly=true) ItemStack stack) {
            if(LbcSpecialEggs.matches(stack)) EntityType.<Entity>createDefaultStackConfig(level,stack,player).apply(child);
            original.call(level,child);
        }
    }
}
