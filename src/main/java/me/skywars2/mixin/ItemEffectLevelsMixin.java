package me.skywars2.mixin;

import me.skywars2.game.ItemEffectLevels;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Item effects keep their old saturated strength; /effect never passes these item paths. */
public final class ItemEffectLevelsMixin {
    private ItemEffectLevelsMixin() { }
    @Mixin(PotionContents.class)
    public abstract static class Potions {
        @ModifyArg(method="forEachEffect",at=@At(value="INVOKE",target="Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V"),index=0)
        private Object skywars2$legacyPotionStrength(Object effect) {
            return effect instanceof MobEffectInstance instance?ItemEffectLevels.cap(instance):effect;
        }
    }
    @Mixin(ApplyStatusEffectsConsumeEffect.class)
    public abstract static class Consumables {
        @ModifyArg(method="apply",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z"),index=0)
        private MobEffectInstance skywars2$legacyFoodStrength(MobEffectInstance effect) { return ItemEffectLevels.cap(effect); }
    }
}
