package me.skywars2.game;

import java.util.List;
import me.skywars2.mixin.ActorCheckpointAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
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
        hiddenModeTransitions(game,player,target);
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

    private static void hiddenModeTransitions(GameController game, ServerPlayer player, Cow existingTarget) {
        var level = player.level();
        var manager = ((ActorCheckpointAccess.LevelEntities) level).skywars2$manager();
        var cold = new BlockPos(1_000_000, 200, 1_000_000);
        var warm = existingTarget.position().add(0, 3, 0);
        require(!level.hasChunkAt(cold) && level.getEntity(existingTarget.getUUID()) == existingTarget,
                "the mode probe has a cold section and an already-tracked destination");
        var hidden = new Cow(EntityTypes.COW, level);
        var visible = new Cow(EntityTypes.COW, level);
        hidden.setPos(cold.getX() + .5, cold.getY(), cold.getZ() + .5);
        visible.setPos(warm);
        try {
            publishMode(game, false);
            for (var cow : List.of(hidden, visible)) {
                cow.setNoAi(true);
                cow.setNoGravity(true);
                require(level.addFreshEntity(cow), "ordinary native cow registers for the mode probe");
                cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(4096);
                cow.setHealth(4096);
                require(cow.getMaxHealth() == 4096 && cow.getHealth() == 4096,
                        "modern max-health cache and current health start at 4096");
                require(cow.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200, 300)),
                        "modern mode probe accepts a real expanded effect");
            }
            require(manager.isLoaded(hidden.getUUID()) && level.getEntity(hidden.getUUID()) == null
                            && level.getEntity(visible.getUUID()) == visible,
                    "native registration includes the hidden cow while only the control is visible");

            publishMode(game, true);
            requireLegacyCow(visible, "visible control");
            require(level.getEntity(hidden.getUUID()) == null && !level.hasChunkAt(cold),
                    "mode entry does not load the cold section or track its cow");
            hidden.setPos(warm);
            require(level.getEntity(hidden.getUUID()) == hidden,
                    "native movement promotes the original hidden cow in the already-tracked section");
            requireLegacyCow(hidden, "promoted hidden cow");

            hidden.setPos(cold.getX() + .5, cold.getY(), cold.getZ() + .5);
            require(manager.isLoaded(hidden.getUUID()) && level.getEntity(hidden.getUUID()) == null,
                    "same cow is again registered but hidden before mode exit");
            publishMode(game, false);
            require(visible.getMaxHealth() == 4096 && level.getEntity(hidden.getUUID()) == null && !level.hasChunkAt(cold),
                    "visible control resumes modern bounds while the remote section stays cold");
            hidden.setPos(warm);
            require(level.getEntity(hidden.getUUID()) == hidden && hidden.getMaxHealth() == 4096 && hidden.getHealth() == 1024,
                    "mode exit reopens the formerly hidden cow's modern max-health bound without healing it");
        } finally {
            hidden.discard();
            visible.discard();
            if (!game.balanceLegacy()) publishMode(game, true);
        }
    }

    private static void requireLegacyCow(Cow cow, String witness) {
        var effect = cow.getEffect(MobEffects.FIRE_RESISTANCE);
        require(cow.getMaxHealth() == 1024 && cow.getHealth() == 1024
                        && cow.getAttribute(Attributes.MAX_HEALTH).getBaseValue() == 4096
                        && effect != null && effect.getAmplifier() == 255,
                witness + " applies the native caps without changing the base attribute: max="
                        + cow.getMaxHealth() + ", health=" + cow.getHealth() + ", effect=" + effect);
    }

    private static void publishMode(GameController game, boolean legacy) {
        // The outer suite already reached Balance by the real cast. Replay only its publication
        // boundary here so the registered hidden body cannot unload between the probes.
        try {
            var method = GameController.class.getDeclaredMethod("balanceLegacy", boolean.class);
            method.setAccessible(true);
            method.invoke(game, legacy);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Cannot publish the native Balance mode for the hidden cow probe", error);
        }
    }

    static void exit(GameController game){
        require(!game.balanceLegacy()&&VanillaExpansionRules.enabled(),"modern expansion policy restored after real legacy exit");
        require(Attributes.ARMOR.value().sanitizeValue(-50)==-50&&Attributes.MAX_HEALTH.value().sanitizeValue(5000)==5000,"modern native attribute endpoints restored");
        require(new MobEffectInstance(MobEffects.SPEED,100,Integer.MAX_VALUE).getAmplifier()==Integer.MAX_VALUE&&new MobEffectInstance(MobEffects.SPEED,100,-2).getAmplifier()==-2,"full signed amplifier storage restored");
        require(MobEffects.REGENERATION.value().shouldApplyEffectTickThisTick(17,64),"widened signed periodic cadence restored");
        require(ItemEffectLevels.create(MobEffects.REGENERATION,100,64).getAmplifier()==5,"modern item cap restored");
    }
}
