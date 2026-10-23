package me.skywars2.mixin;

import me.skywars2.SkyWars2;
import me.skywars2.game.LbcSpecialEggs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PostSpawnProcessor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityType.class)
public abstract class SpecialEggMixin {
    // EntityType.create applies this chain after finalizeSpawn and before world insertion.
    // Appending preserves vanilla ENTITY_DATA loading and exposes causal state to ENTITY_LOAD.
    @Inject(method="appendCustomEntityStackConfig",at=@At("RETURN"),cancellable=true)
    private static <T extends Entity> void skywars2$adoptSpecialEgg(PostSpawnProcessor<T> initial,Level level,
            ItemStack stack,LivingEntity user,CallbackInfoReturnable<PostSpawnProcessor<T>> cir) {
        if(LbcSpecialEggs.matches(stack)) cir.setReturnValue(cir.getReturnValue()
                .andThen(entity->LbcSpecialEggs.adopt(SkyWars2.game(),entity,user,stack)));
    }
}
