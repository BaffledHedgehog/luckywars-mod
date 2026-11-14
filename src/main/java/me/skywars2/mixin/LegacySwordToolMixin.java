package me.skywars2.mixin;

import me.skywars2.game.LbcSwordTools;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class LegacySwordToolMixin {
    @Inject(method="<init>(Lnet/minecraft/core/Holder;ILnet/minecraft/core/component/PatchedDataComponentMap;)V",at=@At("RETURN"))
    private void skywars2$swordCreated(CallbackInfo ci){LbcSwordTools.repair((ItemStack)(Object)this);}
    @Inject(method="set",at=@At("RETURN"))
    private <T> void skywars2$swordData(DataComponentType<T> type,T value,CallbackInfoReturnable<T> cir){
        if(type==DataComponents.CUSTOM_DATA)LbcSwordTools.repair((ItemStack)(Object)this);
    }
    // 26.3 inherits this method from TypedInstance. Declare the ItemStack override,
    // keeping ordinary holder membership for every tag and every other stack.
    public boolean is(net.minecraft.tags.TagKey<net.minecraft.world.item.Item> tag){
        var stack=(ItemStack)(Object)this;
        return stack.typeHolder().is(tag)||tag.equals(net.minecraft.tags.ItemTags.SWORDS)&&LbcSwordTools.sword(stack);
    }
    @Inject(method="applyComponents",at=@At("RETURN"))
    private void skywars2$swordComponents(CallbackInfo ci){LbcSwordTools.repair((ItemStack)(Object)this);}
}
