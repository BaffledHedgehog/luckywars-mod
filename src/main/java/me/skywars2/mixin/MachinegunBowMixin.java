package me.skywars2.mixin;

import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observe real native use callbacks without replacing release or custom bow dispatch. */
@Mixin(LivingEntity.class)
public abstract class MachinegunBowMixin {
    @Inject(method="updateUsingItem",at=@At("HEAD"))
    private void skywars2$usingBow(ItemStack used,CallbackInfo ci){
        var game=SkyWars2.game();
        if(game!=null&&(Object)this instanceof ServerPlayer player&&used.getItem() instanceof BowItem)
            game.items().machinegunSkill().usingBow(player,used);
    }
    @com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod(method="stopUsingItem")
    private void skywars2$stoppedBow(com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original){
        boolean bow=((LivingEntity)(Object)this).getUseItem().getItem() instanceof BowItem;
        try{original.call();}finally{
            var game=SkyWars2.game();
            if(bow&&game!=null&&(Object)this instanceof ServerPlayer player)game.items().machinegunSkill().clear(player);
        }
    }
    @Mixin(AbstractArrow.class)
    public abstract static class ArrowTick {
        @Inject(method="tick",at=@At("RETURN"))
        private void skywars2$machinegunArrow(CallbackInfo ci){
            var game=SkyWars2.game();if(game!=null)game.items().machinegunSkill().arrowTick((AbstractArrow)(Object)this);
        }
    }
}
