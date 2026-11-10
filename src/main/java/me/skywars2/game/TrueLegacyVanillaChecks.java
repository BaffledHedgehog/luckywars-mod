package me.skywars2.game;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;

/** Runs inside the actual Supernova/Balance legacy match and again after its real stop. */
final class TrueLegacyVanillaChecks {
    private TrueLegacyVanillaChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("True legacy vanilla: "+why);}
    static void run(GameController game,ServerPlayer player,Cow target){
        require(game.balanceLegacy()&&!VanillaExpansionRules.enabled(),"actual legacy switch disables common expansion policy");
        require(Attributes.ARMOR.value().sanitizeValue(-50)==0&&Attributes.ARMOR.value().sanitizeValue(100)==30,"native armor bounds0..30");
        require(Attributes.MAX_HEALTH.value().sanitizeValue(5000)==1024&&Attributes.ATTACK_DAMAGE.value().sanitizeValue(10000)==2048,"native health and damage upper bounds");
        require(Attributes.GRAVITY.value().sanitizeValue(-9)==-1&&Attributes.GRAVITY.value().sanitizeValue(9)==1,"native gravity bounds-1..1");
        require(new MobEffectInstance(MobEffects.SPEED,100,Integer.MAX_VALUE).getAmplifier()==255&&new MobEffectInstance(MobEffects.SPEED,100,-2).getAmplifier()==0,"native constructor amplifier clamp0..255");
        require(ItemEffectLevels.create(MobEffects.REGENERATION,100,64).getAmplifier()==64,"legacy item strength bypasses modern saturated-item cap");
        require(!MobEffects.REGENERATION.value().shouldApplyEffectTickThisTick(17,64),"native shift64 cadence bug retained instead of widened signed cadence");
        target.removeAllEffects();target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20);target.setHealth(5);
        MobEffects.REGENERATION.value().applyEffectTick(player.level(),target,64);
        require(target.getHealth()==6,"high regeneration still heals one native HP per pulse");
        target.setHealth(5);MobEffects.INSTANT_HEALTH.value().applyInstantaneousEffect(player.level(),null,null,target,32,1);
        require(target.getHealth()==9,"native32-bit instant-heal shift behavior retained");
        if(target.getAttribute(Attributes.ATTACK_DAMAGE)==null){
            var attack=new net.minecraft.world.entity.ai.attributes.AttributeInstance(Attributes.ATTACK_DAMAGE,a->target.getAttributes().getAttributesToUpdate().add(a));
            ((me.skywars2.mixin.GlyphAiMixin.AttributeAccess)target.getAttributes()).skywars2$glyphAttributes().put(Attributes.ATTACK_DAMAGE,attack);
        }
        target.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(10);target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,200,1));
        require(target.getAttributeValue(Attributes.ATTACK_DAMAGE)==2,"WeaknessII uses native-8 damage instead of modern0.75 multiplier");
        target.removeAllEffects();target.setHealth(20);
        target.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,200,1));target.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,200,2));
        require(target.getAbsorptionAmount()==12&&target.getMaxHealth()==20&&ShadowAbsorption.state(target).entries.isEmpty(),"native absorption replaces8with12 rather than stacking ledger yellow/shadow HP");
        boolean ground=player.onGround();
        try{
            player.removeAllEffects();player.setOnGround(true);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND_PICKAXE));
            float baseline=player.getDestroySpeed(Blocks.STONE.defaultBlockState());require(baseline>0,"native mining baseline");
            player.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE,200,4));
            require(Math.abs(player.getDestroySpeed(Blocks.STONE.defaultBlockState())/baseline-Math.pow(.3,5))<.0000001,"native26.3 fatigueV formula retained");
            player.removeEffect(MobEffects.MINING_FATIGUE);player.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE,200,-2));
            require(Math.abs(player.getDestroySpeed(Blocks.STONE.defaultBlockState())/baseline-.3)<.0000001,"negative input clamps to vanilla fatigueI instead of increasing mining speed");
            player.removeAllEffects();player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY,100,8));player.addEffect(new MobEffectInstance(MobEffects.GLOWING,100,8));
            require(SignedEffectVisuals.invisibilityLevel(player)==1&&SignedEffectVisuals.desired(player).isEmpty(),"ordinary invisibility and no signed post-effect augmentation");
            SignedGlowVisuals.tick(game.server());require(SignedGlowVisuals.carrierCount()==0,"no expanded high-glow display carriers in legacy");
            for(int amplifier:new int[]{-1,256}){
                boolean denied=false;
                try{game.server().getCommands().getDispatcher().execute("effect give "+player.getScoreboardName()+" minecraft:speed 2 "+amplifier,game.server().createCommandSourceStack());}
                catch(com.mojang.brigadier.exceptions.CommandSyntaxException expected){denied=true;}
                require(denied,"native/effect rejects amplifier outside0..255: "+amplifier);
            }
        }finally{player.setOnGround(ground);player.removeAllEffects();}
    }
    static void exit(GameController game){
        require(!game.balanceLegacy()&&VanillaExpansionRules.enabled(),"modern expansion policy restored after real legacy exit");
        require(Attributes.ARMOR.value().sanitizeValue(-50)==-50&&Attributes.MAX_HEALTH.value().sanitizeValue(5000)==5000,"modern native attribute endpoints restored");
        require(new MobEffectInstance(MobEffects.SPEED,100,Integer.MAX_VALUE).getAmplifier()==Integer.MAX_VALUE&&new MobEffectInstance(MobEffects.SPEED,100,-2).getAmplifier()==-2,"full signed amplifier storage restored");
        require(MobEffects.REGENERATION.value().shouldApplyEffectTickThisTick(17,64),"widened signed periodic cadence restored");
        require(ItemEffectLevels.create(MobEffects.REGENERATION,100,64).getAmplifier()==5,"modern item cap restored");
    }
}
