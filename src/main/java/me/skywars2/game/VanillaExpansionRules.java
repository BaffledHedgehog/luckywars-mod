package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.world.entity.LivingEntity;

/** One dynamic boundary for extended vanilla effects/attributes, including their client projection. */
public final class VanillaExpansionRules {
    private VanillaExpansionRules(){}
    public static boolean enabled(){var game=SkyWars2.game();return game==null||!game.balanceLegacy();}
    static void changed(GameController game){
        boolean rebuilding=game.server().tickRateManager().isFrozen();
        if(!rebuilding)SignedEnvironmentalEffects.clear();
        SignedEffectVisuals.stop(game.server());
        for(var level:game.server().getAllLevels())for(var entity:level.getAllEntities())if(entity instanceof LivingEntity living){
            if(!rebuilding){
                if(!enabled())for(var effect:java.util.List.copyOf(living.getActiveEffects())){
                    int amplifier=effect.getAmplifier();if(amplifier>=0&&amplifier<=255)continue;
                    living.removeEffect(effect.getEffect());
                    living.addEffect(new net.minecraft.world.effect.MobEffectInstance(effect.getEffect(),effect.getDuration(),Math.clamp(amplifier,0,255),effect.isAmbient(),effect.isVisible(),effect.showIcon()));
                }
                ShadowAbsorption.modeChanged(living);
            }
            SignedEffectMath.dolphin(living);
            var attributes=((me.skywars2.mixin.GlyphAiMixin.AttributeAccess)living.getAttributes()).skywars2$glyphAttributes();
            for(var attribute:java.util.List.copyOf(attributes.values()))((me.skywars2.mixin.LegacyVanillaStateMixin.Attribute)attribute).skywars2$invalidate();
            if(!rebuilding&&!enabled()){
                living.setHealth(living.getHealth());
                living.setAbsorptionAmount(living.getAbsorptionAmount());
            }
            ((me.skywars2.mixin.LegacyVanillaStateMixin.Visibility)living).skywars2$nativeVisibility();
            SignedEffectVisuals.effectsChanged(living);
        }
    }
}
