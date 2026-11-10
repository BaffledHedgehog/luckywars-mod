package me.skywars2.game;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/** Legacy item amplitudes often encoded only a saturated rate, not stronger future formulas. */
public final class ItemEffectLevels {
    private ItemEffectLevels() { }
    public static int amplifier(Holder<MobEffect> effect,int value) {
        if(!VanillaExpansionRules.enabled())return value;
        int cap=effect.is(MobEffects.REGENERATION)||effect.is(MobEffects.WITHER)?5:
                effect.is(MobEffects.POISON)||effect.is(MobEffects.RESISTANCE)?4:
                effect.is(MobEffects.BLINDNESS)||effect.is(MobEffects.DARKNESS)?1:Integer.MAX_VALUE;
        return Math.min(cap,value);
    }
    public static MobEffectInstance cap(MobEffectInstance effect) {
        int value=amplifier(effect.getEffect(),effect.getAmplifier());
        return value==effect.getAmplifier()?effect:new MobEffectInstance(effect.getEffect(),effect.getDuration(),value,effect.isAmbient(),effect.isVisible(),effect.showIcon());
    }
    public static MobEffectInstance create(Holder<MobEffect> effect,int ticks) { return create(effect,ticks,0); }
    public static MobEffectInstance create(Holder<MobEffect> effect,int ticks,int amplifier) { return create(effect,ticks,amplifier,false,true,true); }
    public static MobEffectInstance create(Holder<MobEffect> effect,int ticks,int amplifier,boolean ambient) { return create(effect,ticks,amplifier,ambient,true,true); }
    public static MobEffectInstance create(Holder<MobEffect> effect,int ticks,int amplifier,boolean ambient,boolean visible) { return create(effect,ticks,amplifier,ambient,visible,visible); }
    public static MobEffectInstance create(Holder<MobEffect> effect,int ticks,int amplifier,boolean ambient,boolean visible,boolean icon) {
        return new MobEffectInstance(effect,ticks,amplifier(effect,amplifier),ambient,visible,icon);
    }
    public static MobEffectInstance create(MobEffectInstance effect) { return cap(new MobEffectInstance(effect)); }
}
