package me.skywars2.mixin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

/** У экологического урона базовый перевод имеет один аргумент: владелец требует варианта .player. */
@Mixin(DamageSource.class)
public abstract class DamageDeathMessageMixin {
    private static final Set<String> SKYWARS2_SINGLE_NAME_MESSAGES=Set.of(
            "anvil","cramming","dragonBreath","drown","dryout","even_more_magic","explosion",
            "fall","fallingBlock","fallingStalactite","fireworks","flyIntoWall","freeze","generic","genericKill",
            "hotFloor","inFire","inWall","lava","lightningBolt","magic","onFire","outOfWorld","outsideBorder",
            "sonic_boom","stalagmite","starve","sting","sulfurCubeHot","sweetBerryBush","wither","skywars2.electric");

    @Inject(method="getLocalizedDeathMessage",at=@At("HEAD"),cancellable=true)
    private void skywars2$ownedDeathMessage(LivingEntity victim,CallbackInfoReturnable<Component> ci) {
        var source=(DamageSource)(Object)this;
        if(!SKYWARS2_SINGLE_NAME_MESSAGES.contains(source.getMsgId()) || !(source.getEntity() instanceof LivingEntity owner)) return;
        // Вариант с именованным оружием vanilla уже выбирает сама; template содержит .item.
        if(!owner.getMainHandItem().isEmpty() && owner.getMainHandItem().has(DataComponents.CUSTOM_NAME)) return;
        ci.setReturnValue(Component.translatable("death.attack."+source.getMsgId()+".player",victim.getDisplayName(),owner.getDisplayName()));
    }
}
