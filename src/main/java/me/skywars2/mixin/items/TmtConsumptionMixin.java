package me.skywars2.mixin.items;

import me.skywars2.game.items.tmt.LbcTmtAbilities;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Native actions retain the generated item policy without changing source items or inventory transfer. */
@Mixin(ItemStack.class)
public abstract class TmtConsumptionMixin {
    @Inject(method="consume",at=@At("HEAD"),cancellable=true)
    private void skywars2$reusableTmt(int amount,LivingEntity owner,CallbackInfo ci){
        if(LbcTmtAbilities.reusable((ItemStack)(Object)this))ci.cancel();
    }
    @Inject(method="shrink",at=@At("HEAD"),cancellable=true)
    private void skywars2$propertyDebit(int amount,CallbackInfo ci){
        var stack=(ItemStack)(Object)this;
        if(LbcTmtAbilities.active(stack)&&LbcTmtAbilities.reusable(stack))ci.cancel();
    }
}
