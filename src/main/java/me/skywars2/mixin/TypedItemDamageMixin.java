package me.skywars2.mixin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Legacy type markers protect the actual server stack, including Polymer items. */
@Mixin(ItemEntity.class)
public abstract class TypedItemDamageMixin {
    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void skywars2$protectTypedStack(ServerLevel level, DamageSource source, float amount,
                                          CallbackInfoReturnable<Boolean> cir) {
        if (source.is(DamageTypes.FELL_OUT_OF_WORLD)) return;
        var data = ((ItemEntity) (Object) this).getItem().get(DataComponents.CUSTOM_DATA);
        // Numeric artifact types and string clock types share presence semantics.
        // Read on damage so replacement, merging and loading need no cached entity flag.
        if (data != null) {
            var tag=data.copyTag();
            if(tag.contains("type")||tag.getIntOr("transformable",0)==1)cir.setReturnValue(false);
        }
    }
}
