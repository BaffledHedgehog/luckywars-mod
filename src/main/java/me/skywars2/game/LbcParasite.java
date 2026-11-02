package me.skywars2.game;

import java.util.List;
import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/** Skill 19: unconditional debuff immunity and the original permanent damage penalty. */
public final class LbcParasite {
    private LbcParasite() {}
    private static final java.util.Set<ServerPlayer> CLEANSING=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());

    /** Admission deliberately has no magic, mechanism, artifact or pause suppression. */
    public static boolean active(LivingEntity entity) {
        var game=SkyWars2.game();
        return entity instanceof ServerPlayer player && game!=null && game.lbc().hasSkill(player,19);
    }

    /** Classify the actual signed mechanic, keeping useful transports and inverted boons. */
    public static boolean rejects(MobEffectInstance effect) {
        long level=(long)effect.getAmplifier()+1;
        if(effect.is(MobEffects.RAID_OMEN)) {
            int amp=effect.getAmplifier();
            // Stat/boost potions and the cleansing potion are useful transport, not raid debuffs.
            if(amp>=16 && amp<=19 || amp==43) return false;
            // Vanilla omens plus authored bad-potion/arrow/teleport/mutation transports.
            // Unknown transports are not presumed harmful merely for using this carrier.
            return amp>=0 && amp<=5 || amp==20 || amp>=40 && amp<=42 || amp==74
                    || amp>=101 && amp<=126 || amp>=234 && amp<=237;
        }
        // Native players still levitate at negative amplifiers (the client clamps this mechanic).
        if(effect.is(MobEffects.LEVITATION)) return true;
        if(level==0) return false;
        if(effect.is(MobEffects.INFESTED)||effect.is(MobEffects.OOZING)
                ||effect.is(MobEffects.WEAVING)||effect.is(MobEffects.WIND_CHARGED)) return true;
        if(level>0) return effect.getEffect().value().getCategory()==MobEffectCategory.HARMFUL
                ||effect.is(MobEffects.GLOWING)||effect.is(MobEffects.BAD_OMEN)||effect.is(MobEffects.TRIAL_OMEN);
        // These effects implement detrimental negative levels in the signed native mechanics.
        return effect.is(MobEffects.REGENERATION)||effect.is(MobEffects.INSTANT_HEALTH)
                ||effect.is(MobEffects.SATURATION)||effect.is(MobEffects.SPEED)||effect.is(MobEffects.HASTE)
                ||effect.is(MobEffects.STRENGTH)||effect.is(MobEffects.HEALTH_BOOST)||effect.is(MobEffects.ABSORPTION)
                ||effect.is(MobEffects.RESISTANCE)||effect.is(MobEffects.FIRE_RESISTANCE)||effect.is(MobEffects.LUCK)
                ||effect.is(MobEffects.WATER_BREATHING)||effect.is(MobEffects.CONDUIT_POWER)
                ||effect.is(MobEffects.BREATH_OF_THE_NAUTILUS)||effect.is(MobEffects.JUMP_BOOST)
                ||effect.is(MobEffects.INVISIBILITY)||effect.is(MobEffects.NIGHT_VISION)
                ||effect.is(MobEffects.BLINDNESS)||effect.is(MobEffects.HERO_OF_THE_VILLAGE);
    }

    /** Mixed breathing becomes a debuff only outside its useful environment. */
    public static boolean rejects(LivingEntity target,MobEffectInstance effect){
        if((effect.is(MobEffects.WATER_BREATHING)||effect.is(MobEffects.CONDUIT_POWER)
                ||effect.is(MobEffects.BREATH_OF_THE_NAUTILUS))&&(long)effect.getAmplifier()+1<0)
            return target.isEyeInFluid(net.minecraft.tags.FluidTags.WATER);
        return rejects(effect)||effect.is(MobEffects.WATER_BREATHING)&&(long)effect.getAmplifier()+1>=2
                &&!target.isEyeInFluid(net.minecraft.tags.FluidTags.WATER);
    }
    public static void beforeEnvironment(LivingEntity target){
        if(active(target)&&target instanceof ServerPlayer player){
            for(var type:List.of(MobEffects.WATER_BREATHING,MobEffects.CONDUIT_POWER,MobEffects.BREATH_OF_THE_NAUTILUS)){
                var effect=target.getEffect(type);
                if(effect!=null&&rejects(target,effect)){beforeEffects(player);break;}
            }
        }
    }

    /** Runs before the native effect iterator, including persisted hidden successors. */
    public static void beforeEffects(ServerPlayer player) {
        if(!active(player)||!CLEANSING.add(player))return;
        try {
            // Returning from a forced form restores its saved effects; cleanse that restored state too.
            var items=SkyWars2.game().items();
            items.polymorph().clearDebuffs(player);items.edibleSpells().clearDebuffs(player);
            items.ownedEffects().purgeDebuffs(player);
            cleanseAbsorption(player);
            java.util.ArrayList<MobEffectInstance> changed=null;
            for(var head:player.getActiveEffects())for(var node=head;node!=null;node=((EffectLayers.Access)node).skywars2$layerHidden()){
                if(rejects(player,node)){if(changed==null)changed=new java.util.ArrayList<>();changed.add(head);break;}
            }
            if(changed!=null)for(var head:changed) {
                MobEffectInstance kept=null,tail=null;
                for(var node=head;node!=null;node=((EffectLayers.Access)node).skywars2$layerHidden()) {
                    if(rejects(player,node))continue;
                    var copy=new MobEffectInstance(node);
                    ((EffectLayers.Access)copy).skywars2$layerHidden(null);
                    if(kept==null)kept=copy;else ((EffectLayers.Access)tail).skywars2$layerHidden(copy);
                    tail=copy;
                }
                if(!rejects(player,head))((EffectLayers.Access)head).skywars2$layerHidden(((EffectLayers.Access)kept).skywars2$layerHidden());
                else {
                    var owners=items.ownedEffects();var saved=owners.suspend(player);
                    try{player.removeEffect(head.getEffect());if(kept!=null)player.addEffect(kept,player);}
                    finally{owners.resume(player,saved);}
                }
            }
            clearCustom(player);
        }finally{CLEANSING.remove(player);}
    }

    /** Absorption has an independent signed ledger as well as its native carrier. */
    private static void cleanseAbsorption(ServerPlayer player) {
        var state=ShadowAbsorption.state(player);
        if(!state.entries.values().removeIf(LbcParasite::rejects)) return;
        ShadowAbsorption.reconcile(player,false);
        state.internal=true;
        try {
            if(state.entries.isEmpty()) player.removeEffect(MobEffects.ABSORPTION);
            else player.forceAddEffect(new MobEffectInstance(state.entries.values().iterator().next()),player);
        } finally { state.internal=false; }
    }

    /** Canonical custom owners purge only their harmful state; useful buffs retain their clocks. */
    public static void reconcile(ServerPlayer player) {
        if(!active(player)||!CLEANSING.add(player))return;
        try{clearCustom(player);}finally{CLEANSING.remove(player);}
    }
    private static void clearCustom(ServerPlayer player){
        var items=SkyWars2.game().items();
        items.babylonEffects().clear(player);
        items.quiver().clear(player);
        items.kiuas().clearCurse(player);
        items.acidCorruption().clear(player);
        items.bolts().clearChainStatus(player);
        items.clearTurnCurse(player);
        items.psycho().clearDebuffs(player);
        items.mana().clearDebuffs(player);
        items.electro().clearEffects(player);
        items.frost().clear(player);
        items.shooters().clearStun(player);
        items.homing().clearEffects(player);
        items.soulless().clear(player);
        items.witchWrath().clearDebuffs(player);
        items.kfcFood().clear(player);
        items.foodStatuses().clearDebuffs(player);
        items.totems().clear(player);
        items.magic().colors().clearEffects(player);
        items.legacyEffects().clearDebuffs(player);
    }

    /** One 40 percent roll per accepted damage event, followed by one uniform 1..26 roll. */
    public static void damaged(LivingEntity victim,DamageSource source) {
        if(active(victim) && victim instanceof ServerPlayer player && player.getRandom().nextFloat()<.4f)
            LbcParasitePenalties.roll(player);
    }
}
