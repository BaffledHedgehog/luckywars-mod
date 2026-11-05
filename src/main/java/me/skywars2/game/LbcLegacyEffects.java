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
            "effect_traps","effect_snus","effect_stand_arrow_hit","effect_witch_gens_reset","effect_hedgehog_boom","effect_vacuum","effect_timestop");
    private final GameController game;
    private final LbcPersonalStasis personalStasis;
    private int lastTick=Integer.MIN_VALUE,lastSelection;
    private long selections,actions;
    public LbcLegacyEffects(GameController game){this.game=game;this.personalStasis=new LbcPersonalStasis(game);}
    public LbcPersonalStasis personalStasis(){return personalStasis;}
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
        if(!admitted(p)||LbcParasite.active(p))return;
        p.entityTags().addAll(List.of("have_lucky_effects","have_lucky_effects_1s","have_lucky_effects_5t"));
        lastSelection=selection;selections++;
        game.items().mana().score(net.minecraft.world.scores.ScoreHolder.forNameOnly("randebuff"),"lbc.math").set(selection);
        if(selection<=17){var effect=VANILLA.get(selection-1);var cause=game.items().indirectKills().wildCause(p);
            game.items().indirectKills().withCause(p,cause,()->p.addEffect(ItemEffectLevels.create(effect,selection==6?1:600,0),p));
        }else{game.items().mana().score(p,RANDOM_COUNTERS.get(selection-18)).add(30);game.items().adoptRawFoodStatuses(p);}
    }
    /** Scores are authoritative; no private duration is introduced for generic consumers. */
    public void adoptRawFoodStatuses(ServerPlayer p){
        if(LbcParasite.active(p)){clearDebuffs(p);return;}
        if(OWNED.stream().anyMatch(k->read(p,k)>0))p.entityTags().add("have_lucky_effects");
        if(read(p,"effect_gravity_curse")>0)p.entityTags().add("have_lucky_effects_1s");
    }
    public void tick(){
        int now=game.server().getTickCount();if(now==lastTick)return;lastTick=now;
        for(var p:List.copyOf(game.server().getPlayerList().getPlayers())){
            if(LbcParasite.active(p))clearDebuffs(p);
            personalStasis.tick(p);
            if(game.items().polymorph().masked(p)||!admitted(p))continue;
            if((now&1)==0)ambientReactions(p);
            boolean greed=p.entityTags().contains("sin_greed_active");
            if(!greed){
                consume(p,"effect_explosion",()->{if(!explosionSuppressed(p))game.items().babylonEffects().primeExplosionAt(p.level(),p.position(),game.items().indirectKills().wildCause(p));});
                consume(p,"effect_big_explosion",()->{if(!explosionSuppressed(p))game.items().babylonEffects().bigExplosionAt(p.level(),p.position(),game.items().indirectKills().wildCause(p));});
                consume(p,"effect_traps",()->game.items().chaosTraps().spawnUnjammed(p,p.position()));
                consume(p,"effect_witch_gens_reset",()->{if(!magicBeacon(p))resetWitch(p);});
                consume(p,"effect_hedgehog_boom",()->{if(!game.items().magic().suppressed(p))game.items().hedgehog().spawnWall(p.level(),p.position(),p);});
            }
            // These branches deliberately lack the source Greed exclusion.
            consume(p,"effect_vacuum",()->vacuum(p));
            consume(p,"effect_stand_arrow_hit",()->{if(!game.items().magic().suppressed(p))game.items().stand().arrowHitPre(p);});
            if(!greed){
                // Debit before native lethal callbacks; death cleanup must not be followed by
                // re-adding the corpse's lucky-effect marker or draining a freshly cleared score.
                if(read(p,"effect_nuking")>0){
                    drain(p,"effect_nuking");actions++;
                    if(!game.items().magic().mechanismSuppressedAt(p.level(),p.position()))game.items().nukeEvent().pointStatusBurst(p,p.level(),p.position());
                    if(!admitted(p))continue;
                }
                consume(p,"effect_snus",()->snus(p));
                consume(p,"effect_timestop",()->personalStasis.start(p,LbcPersonalStasis.DEFAULT_DURATION_TICKS));
                if(now%20==0&&read(p,"effect_gravity_curse")>0&&!game.items().pendingCombat().gravityCurseActive(p))gravity(p);
            }
        }
    }
    /** Source players_2t: independent5% rolls for amplifier>=1, not a new private duration. */
    void ambientReactions(ServerPlayer p){
        if(!admitted(p)||game.items().mana().score(net.minecraft.world.scores.ScoreHolder.forNameOnly("legacy"),"lbc.math").get()==1)return;
        if(SignedEffectVisuals.level(p,MobEffects.HUNGER)>1&&p.level().getRandom().nextFloat()<.05f)reaction(p,false);
        if(SignedEffectVisuals.level(p,MobEffects.NAUSEA)>1&&p.level().getRandom().nextFloat()<.05f)reaction(p,true);
    }
    void reaction(ServerPlayer p,boolean vomiting){
        var at=vomiting?p.getEyePosition().add(p.getLookAngle().scale(.3)):p.position().add(0,.4,0).add(p.getLookAngle().scale(-.5));
        if(vomiting){
            p.level().sendParticles(new net.minecraft.core.particles.ItemParticleOption(net.minecraft.core.particles.ParticleTypes.ITEM,net.minecraft.world.item.Items.CHICKEN),at.x,at.y,at.z,100,.1,.1,.1,.1);
            p.addEffect(ItemEffectLevels.create(MobEffects.BLINDNESS,20,0,false,false));p.addEffect(ItemEffectLevels.create(MobEffects.DARKNESS,20,0,false,false));
            for(float pitch:new float[]{0,1,2})LbcOverload.sound(p,at,"minecraft:entity.player.burp",1,pitch,0);
        }else{
            p.level().sendParticles(net.minecraft.core.particles.ParticleTypes.FALLING_HONEY,at.x,at.y,at.z,100,.1,.1,.1,.5);
            LbcOverload.sound(p,at,"minecraft:block.honey_block.break",1,0,0);
        }
    }
    private void consume(ServerPlayer p,String objective,Runnable action){if(read(p,objective)<=0)return;
        action.run();drain(p,objective);actions++;
    }
    private void vacuum(ServerPlayer p){vacuumAt(p.level(),p.position(),p.getYRot(),p.getXRot(),game.items().indirectKills().wildCause(p));}
    /** Shared WILD non-player pull; visual children remain with their canonical ability owner. */
    void vacuumAt(net.minecraft.server.level.ServerLevel level,net.minecraft.world.phys.Vec3 origin,float yaw,float pitch,LbcIndirectKills.Owner cause){
        if(!level.hasChunkAt(net.minecraft.core.BlockPos.containing(origin)))return;
        var displacedCause=new LbcIndirectKills.Owner(cause.player(),cause.order(),true,cause.projectile());
        var transform=(java.util.function.UnaryOperator<net.minecraft.world.phys.Vec3>)(point->point.distanceToSqr(origin)<=10000?origin:point);
        var bounds=new net.minecraft.world.phys.AABB(origin,origin).inflate(100);
        for(var entity:List.copyOf(level.getEntities((net.minecraft.world.entity.Entity)null,bounds,
                e->!(e instanceof net.minecraft.world.entity.player.Player)&&game.items().wands().validRoleTarget(e,true)
                        &&!LbcPlanetoidSkill.familyChild(e)&&e.position().distanceToSqr(origin)<=10000))){
            if(entity.teleportTo(level,origin.x,origin.y,origin.z,java.util.Set.of(),yaw,pitch,true)
                    &&entity instanceof net.minecraft.world.entity.LivingEntity living)
                game.items().indirectKills().displaced(living,displacedCause);
        }
        game.items().transformSpellOrigins(level,transform);
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
    private void snus(ServerPlayer p){snus(p,game.items().indirectKills().wildCause(p));game.items().mana().award(p,"lbc:true_advancements/potions/snus");}
    void snus(net.minecraft.world.entity.LivingEntity p,LbcIndirectKills.Owner cause){
        var text=Component.empty().append(p.getDisplayName().copy().withStyle(ChatFormatting.GREEN)).append(Component.translatable("snus.used"));
        for(var viewer:game.server().getPlayerList().getPlayers())viewer.sendSystemMessage(text);
        for(var effect:List.of(MobEffects.REGENERATION,MobEffects.POISON,MobEffects.WITHER,MobEffects.SATURATION,
                MobEffects.ABSORPTION,MobEffects.HEALTH_BOOST,MobEffects.WEAKNESS,MobEffects.BLINDNESS,MobEffects.GLOWING,
                MobEffects.INVISIBILITY,MobEffects.FIRE_RESISTANCE,MobEffects.LEVITATION,MobEffects.MINING_FATIGUE,
                MobEffects.SLOW_FALLING,MobEffects.SLOWNESS,MobEffects.RESISTANCE,MobEffects.STRENGTH))p.removeEffect(effect);
        game.items().indirectKills().withCause(p,cause,()->{
            effect(p,MobEffects.SPEED,4000,15);effect(p,MobEffects.JUMP_BOOST,4000,15);effect(p,MobEffects.NAUSEA,5000,0);
            effect(p,MobEffects.MINING_FATIGUE,5000,2);effect(p,MobEffects.HUNGER,8000,0);
            effect(p,MobEffects.HUNGER,320,10);effect(p,MobEffects.NIGHT_VISION,5000,0);
        });
    }
    private void effect(net.minecraft.world.entity.LivingEntity p,Holder<MobEffect> effect,int ticks,int amp){p.addEffect(ItemEffectLevels.create(effect,ticks,amp),p);}
    private void gravity(ServerPlayer p){
        if(read(p,"effect_gravity_curse")==1){var id=Identifier.parse("minecraft:gravity_curse");
            for(var type:List.of(Attributes.FALL_DAMAGE_MULTIPLIER,Attributes.GRAVITY,Attributes.STEP_HEIGHT,
                    Attributes.SAFE_FALL_DISTANCE,Attributes.JUMP_STRENGTH,Attributes.MOVEMENT_SPEED)){
                var attribute=p.getAttribute(type);if(attribute!=null)attribute.removeModifier(id);}}
        game.items().mana().score(p,"effect_gravity_curse").add(-1);p.entityTags().add("have_lucky_effects_1s");
    }
    public void clearDebuffs(ServerPlayer p){
        var mana=game.items().mana();
        for(var key:RANDOM_COUNTERS)if(mana.hudValue(p,key)!=0)mana.score(p,key).set(0);
        for(var key:List.of("effect_vacuum","effect_timestop"))if(mana.hudValue(p,key)!=0)mana.score(p,key).set(0);
        game.items().pendingCombat().clearEffects(p);personalStasis.clear(p);
    }
    public void clear(ServerPlayer p){for(var key:OWNED)game.items().mana().score(p,key).set(0);
        // Shared PendingCombat owns active physical modifiers and its ordinary clear boundary.
        game.items().mana().score(p,"effect_gravity_curse").set(0);}
    public void forget(ServerPlayer p){personalStasis.clear(p);clear(p);}
    public void stop(){for(var p:game.server().getPlayerList().getPlayers())forget(p);lastTick=Integer.MIN_VALUE;}
    public int lastSelection(){return lastSelection;}public long selections(){return selections;}public long actions(){return actions;}
}
