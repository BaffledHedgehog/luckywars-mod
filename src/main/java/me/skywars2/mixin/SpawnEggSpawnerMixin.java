package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Spawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Keep native admission, consumption and synchronization; preserve survival eggs' complete data. */
@Mixin(SpawnEggItem.class)
public abstract class SpawnEggSpawnerMixin {
    @WrapOperation(method="useOn",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Spawner;setEntityId(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/util/RandomSource;)V"))
    private void skywars2$fullEgg(Spawner spawner,EntityType<?> type,RandomSource random,Operation<Void> original,
            @Local(argsOnly=true)UseOnContext context){
        var data=context.getItemInHand().get(DataComponents.ENTITY_DATA);
        if(data!=null)spawner.setEntityData(data,random);
        else original.call(spawner,type,random);
    }
}
