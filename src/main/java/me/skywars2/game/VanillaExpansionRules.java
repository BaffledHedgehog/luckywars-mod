package me.skywars2.game;

import java.util.ArrayList;
import java.util.List;
import me.skywars2.SkyWars2;
import me.skywars2.mixin.ActorCheckpointAccess;
import net.minecraft.world.entity.LivingEntity;

/** One dynamic boundary for extended vanilla effects/attributes, including their client projection. */
public final class VanillaExpansionRules {
    private VanillaExpansionRules(){}
    public static boolean enabled(){var game=SkyWars2.game();return game==null||!game.balanceLegacy();}
    public static void changed(GameController game){
        boolean rebuilding=game.server().tickRateManager().isFrozen();
        if(!rebuilding)SignedEnvironmentalEffects.clear();
        SignedEffectVisuals.stop(game.server());
        for(var living:registeredLiving(game)){
            if(living.isRemoved())continue;
            if(!rebuilding){
                if(!enabled())for(var effect:List.copyOf(living.getActiveEffects())){
                    int amplifier=effect.getAmplifier();if(amplifier>=0&&amplifier<=255)continue;
                    living.removeEffect(effect.getEffect());
                    living.addEffect(new net.minecraft.world.effect.MobEffectInstance(effect.getEffect(),effect.getDuration(),Math.clamp(amplifier,0,255),effect.isAmbient(),effect.isVisible(),effect.showIcon()));
                }
                ShadowAbsorption.modeChanged(living);
            }
            SignedEffectMath.dolphin(living);
            var attributes=((me.skywars2.mixin.GlyphAiMixin.AttributeAccess)living.getAttributes()).skywars2$glyphAttributes();
            for(var attribute:List.copyOf(attributes.values()))((me.skywars2.mixin.LegacyVanillaStateMixin.Attribute)attribute).skywars2$invalidate();
            if(!rebuilding&&!enabled()){
                living.setHealth(living.getHealth());
                living.setAbsorptionAmount(living.getAbsorptionAmount());
            }
            ((me.skywars2.mixin.LegacyVanillaStateMixin.Visibility)living).skywars2$nativeVisibility();
            SignedEffectVisuals.effectsChanged(living);
        }
    }

    private static List<LivingEntity> registeredLiving(GameController game) {
        // Tracking can resume with the same body and cached attributes in a hidden section.
        // Snapshot native residents before effect callbacks can mutate those sections.
        var living = new ArrayList<LivingEntity>();
        for (var level : game.server().getAllLevels()) {
            var manager = ((ActorCheckpointAccess.LevelEntities) level).skywars2$manager();
            var sections = ((ActorCheckpointAccess.Sections) manager).skywars2$sections();
            var chunks = sections.getAllChunksWithExistingSections().iterator();
            while (chunks.hasNext()) {
                sections.getExistingSectionsInChunk(chunks.nextLong()).flatMap(section -> section.getEntities())
                        .forEach(entity -> { if (entity instanceof LivingEntity resident) living.add(resident); });
            }
        }
        return living;
    }
}
