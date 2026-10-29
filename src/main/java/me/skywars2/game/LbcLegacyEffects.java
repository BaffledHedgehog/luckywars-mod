package me.skywars2.game;

import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Shared original random_debuff producer and the remaining source scoreboard consumers. */
public final class LbcLegacyEffects {
    private static final List<Holder<MobEffect>> VANILLA=List.of(MobEffects.BLINDNESS,MobEffects.DARKNESS,
            MobEffects.GLOWING,MobEffects.HUNGER,MobEffects.INFESTED,MobEffects.INSTANT_DAMAGE,
            MobEffects.LEVITATION,MobEffects.MINING_FATIGUE,MobEffects.NAUSEA,MobEffects.OOZING,
            MobEffects.POISON,MobEffects.SLOWNESS,MobEffects.UNLUCK,MobEffects.WEAKNESS,
            MobEffects.WEAVING,MobEffects.WIND_CHARGED,MobEffects.WITHER);
    static final List<String> RANDOM_COUNTERS=List.of("cold","cold_cd","delayed_death","effect_big_explosion",
            "effect_evoker","effect_evokerarrow","effect_explosion","effect_firebreak","effect_gravity_curse",
            "effect_hedgehog_boom","effect_kiuaskivi","effect_lightarrow","effect_lightning_bolt","effect_nuking",
            "effect_remove_mana","effect_small_explosion","effect_snus","effect_stand_arrow_hit","effect_traps",
            "effect_witch_gens_reset","frozen","gigaponos","lookback","nomagic","piss","scared","shock",
            "skill_issue","target","twitching");
    private static final List<String> OWNED=List.of("effect_big_explosion","effect_explosion","effect_nuking",
            "effect_traps","effect_snus","effect_stand_arrow_hit","effect_witch_gens_reset","effect_hedgehog_boom");
    private final GameController game;
    private int lastTick=Integer.MIN_VALUE,lastSelection;
    private long selections,actions;
    public LbcLegacyEffects(GameController game){this.game=game;}
    private boolean admitted(ServerPlayer p){return p!=null&&game.playing()&&game.stage()!=GameController.Stage.ENDING
            &&p.isAlive()&&!p.isRemoved()&&game.canUseGameplayItems(p)&&!game.isObserver(p)
            &&game.items().wands().validRoleTarget(p,false)&&LbcDimensionItems.ordinary(p)
            &&!game.items().timeStop().stopped(p.level())&&!game.items().wands().timeStopped()
            &&!game.items().wands().inputTimeStopped(p);}
    private int read(ServerPlayer p,String objective){return game.items().mana().score(p,objective).get();}
    private void drain(ServerPlayer p,String objective){game.items().mana().score(p,objective).add(-1);p.entityTags().add("have_lucky_effects");}
    /** Source world random, one uniform draw; the caller retains all subsequent native random draws. */
    public void randomDebuff(ServerPlayer p){if(admitted(p))applyDebuff(p,p.level().getRandom().nextInt(47)+1);}
    /** Known literal selections share the actual producer, useful for other source consumers too. */
    public void applyDebuff(ServerPlayer p,int selection){
        if(selection<1||selection>47)throw new IllegalArgumentException("random_debuff selection "+selection);
        if(!admitted(p))return;
        p.entityTags().addAll(List.of("have_lucky_effects","have_lucky_effects_1s","have_lucky_effects_5t"));
        lastSelection=selection;selections++;
        game.items().mana().score(net.minecraft.world.scores.ScoreHolder.forNameOnly("randebuff"),"lbc.math").set(selection);
        if(selection<=17){var effect=VANILLA.get(selection-1);var cause=game.items().indirectKills().wildCause(p);
            game.items().indirectKills().withCause(p,cause,()->p.addEffect(ItemEffectLevels.create(effect,selection==6?1:600,0),p));
        }else{game.items().mana().score(p,RANDOM_COUNTERS.get(selection-18)).add(30);game.items().adoptRawFoodStatuses(p);}
    }
    /** Scores are authoritative; no private duration is introduced for generic consumers. */
    public void adoptRawFoodStatuses(ServerPlayer p){
        if(OWNED.stream().anyMatch(k->read(p,k)>0))p.entityTags().add("have_lucky_effects");
        if(read(p,"effect_gravity_curse")>0)p.entityTags().add("have_lucky_effects_1s");
    }
    public void tick(){
        int now=game.server().getTickCount();if(now==lastTick)return;lastTick=now;
        for(var p:List.copyOf(game.server().getPlayerList().getPlayers())){
            if(!admitted(p))continue;
            boolean greed=p.entityTags().contains("sin_greed_active");
            if(!greed){
                consume(p,"effect_explosion",()->{if(!explosionSuppressed(p))game.items().babylonEffects().primeExplosionAt(p.level(),p.position(),game.items().indirectKills().wildCause(p));});
                consume(p,"effect_big_explosion",()->{if(!explosionSuppressed(p))game.items().babylonEffects().bigExplosionAt(p.level(),p.position(),game.items().indirectKills().wildCause(p));});
                consume(p,"effect_traps",()->game.items().chaosTraps().spawnUnjammed(p,p.position()));
                consume(p,"effect_witch_gens_reset",()->{if(!magicBeacon(p))resetWitch(p);});
                consume(p,"effect_hedgehog_boom",()->{if(!magicBeacon(p))game.items().hedgehog().spawnWall(p.level(),p.position(),p);});
            }
            // This branch deliberately lacks the source Greed exclusion.
            consume(p,"effect_stand_arrow_hit",()->{if(!magicBeacon(p))game.items().stand().arrowHitPre(p);});
            if(!greed){
                consume(p,"effect_nuking",()->{if(!game.items().magic().mechanismSuppressedAt(p.level(),p.position()))game.items().nukeEvent().pointBurst(p,p.level(),p.position());});
                consume(p,"effect_snus",()->snus(p));
                if(now%20==0&&read(p,"effect_gravity_curse")>0&&!game.items().pendingCombat().gravityCurseActive(p))gravity(p);
            }
        }
    }
    private void consume(ServerPlayer p,String objective,Runnable action){if(read(p,objective)<=0)return;
        action.run();drain(p,objective);actions++;
    }
    private boolean magicBeacon(ServerPlayer p){return p.level()==game.maps().level()&&game.items().magic().beaconSuppressed(p.position());}
    private boolean explosionSuppressed(ServerPlayer p){
        if(game.items().magic().mechanismSuppressedAt(p.level(),p.position()))return true;
        // The source queries raw type2 inventory even when an artifact's own active behavior is suppressed.
        for(var holder:p.level().players())if(holder.position().distanceToSqr(p.position())<=144)
            for(int slot=0;slot<=40;slot++){var data=LbcFartus.data(holder.getInventory().getItem(slot));
                if(data.getIntOr("mystical",0)==1&&data.getIntOr("type",0)==2)return true;}
        return false;
    }
    void resetWitch(net.minecraft.world.entity.LivingEntity p){game.items().mana().score(p,"dmg_dealt").set(1);
        for(var objective:List.of("food_count","lolidefecation","sleep_time","greed_prestige"))game.items().mana().score(p,objective).set(0);
        p.entityTags().add("have_mom");p.entityTags().remove("greed_cursed");}
    private void snus(ServerPlayer p){
        var text=Component.empty().append(p.getDisplayName().copy().withStyle(ChatFormatting.GREEN)).append(Component.translatable("snus.used"));
        for(var viewer:game.server().getPlayerList().getPlayers())viewer.sendSystemMessage(text);
        for(var effect:List.of(MobEffects.REGENERATION,MobEffects.POISON,MobEffects.WITHER,MobEffects.SATURATION,
                MobEffects.ABSORPTION,MobEffects.HEALTH_BOOST,MobEffects.WEAKNESS,MobEffects.BLINDNESS,MobEffects.GLOWING,
                MobEffects.INVISIBILITY,MobEffects.FIRE_RESISTANCE,MobEffects.LEVITATION,MobEffects.MINING_FATIGUE,
                MobEffects.SLOW_FALLING,MobEffects.SLOWNESS,MobEffects.RESISTANCE,MobEffects.STRENGTH))p.removeEffect(effect);
        var cause=game.items().indirectKills().wildCause(p);
        game.items().indirectKills().withCause(p,cause,()->{
            effect(p,MobEffects.SPEED,4000,15);effect(p,MobEffects.JUMP_BOOST,4000,15);effect(p,MobEffects.NAUSEA,5000,0);
            effect(p,MobEffects.MINING_FATIGUE,5000,2);effect(p,MobEffects.HUNGER,8000,0);
            effect(p,MobEffects.HUNGER,320,10);effect(p,MobEffects.NIGHT_VISION,5000,0);
        });game.items().mana().award(p,"lbc:true_advancements/potions/snus");
    }
    private void effect(ServerPlayer p,Holder<MobEffect> effect,int ticks,int amp){p.addEffect(ItemEffectLevels.create(effect,ticks,amp),p);}
    private void gravity(ServerPlayer p){
        if(read(p,"effect_gravity_curse")==1){var id=Identifier.parse("minecraft:gravity_curse");
            for(var type:List.of(Attributes.FALL_DAMAGE_MULTIPLIER,Attributes.GRAVITY,Attributes.STEP_HEIGHT,
                    Attributes.SAFE_FALL_DISTANCE,Attributes.JUMP_STRENGTH,Attributes.MOVEMENT_SPEED)){
                var attribute=p.getAttribute(type);if(attribute!=null)attribute.removeModifier(id);}}
        game.items().mana().score(p,"effect_gravity_curse").add(-1);p.entityTags().add("have_lucky_effects_1s");
    }
    public void clear(ServerPlayer p){for(var key:OWNED)game.items().mana().score(p,key).set(0);
        // Shared PendingCombat owns active physical modifiers and its ordinary clear boundary.
        game.items().mana().score(p,"effect_gravity_curse").set(0);}
    public void forget(ServerPlayer p){clear(p);}
    public void stop(){for(var p:game.server().getPlayerList().getPlayers())clear(p);lastTick=Integer.MIN_VALUE;}
    public int lastSelection(){return lastSelection;}public long selections(){return selections;}public long actions(){return actions;}
}
