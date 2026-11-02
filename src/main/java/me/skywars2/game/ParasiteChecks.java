package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Bounded native skill-19 admission, persisted-layer and permanent-attribute fixtures. */
public final class ParasiteChecks {
    private ParasiteChecks() {}
    private static final String UNLOCK="lbc:true_advancements/legends/academy/acidacid";
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Parasite: "+why);}
    private static void near(double got,double expected,String why){require(Math.abs(got-expected)<1e-6,why+" got="+got+" expected="+expected);}
    private static MobEffectInstance effect(Holder<MobEffect> type,int amp){return ItemEffectLevels.create(type,200,amp,false,false,false);}
    private static List<Holder<Attribute>> attributes(){return List.of(Attributes.ARMOR,Attributes.ARMOR_TOUGHNESS,Attributes.ATTACK_DAMAGE,
            Attributes.ATTACK_KNOCKBACK,Attributes.ATTACK_SPEED,Attributes.BURNING_TIME,Attributes.EXPLOSION_KNOCKBACK_RESISTANCE,
            Attributes.FALL_DAMAGE_MULTIPLIER,Attributes.JUMP_STRENGTH,Attributes.KNOCKBACK_RESISTANCE,Attributes.LUCK,Attributes.MAX_HEALTH,
            Attributes.MOVEMENT_EFFICIENCY,Attributes.MOVEMENT_SPEED,Attributes.OXYGEN_BONUS,Attributes.SAFE_FALL_DISTANCE,Attributes.SCALE,
            Attributes.STEP_HEIGHT,Attributes.WATER_MOVEMENT_EFFICIENCY,Attributes.BLOCK_BREAK_SPEED,Attributes.BLOCK_INTERACTION_RANGE,
            Attributes.ENTITY_INTERACTION_RANGE,Attributes.MINING_EFFICIENCY,Attributes.SNEAKING_SPEED,Attributes.SUBMERGED_MINING_SPEED,
            Attributes.SWEEPING_DAMAGE_RATIO);}
    public static void run(GameController game){
        var p=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
        require(p!=null&&game.canUseGameplayItems(p)&&game.playing(),"actual live ordinary participant");
        var snapshot=new RegenerationBeltChecks.Snapshot(game,p);boolean skills=game.config().lbcSkills,ranked=game.config().ranked;
        try{
            p.closeContainer();p.stopUsingItem();p.getInventory().clearContent();p.removeAllEffects();
            p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.getAbilities().invulnerable=false;
            p.entityTags().removeAll(Set.of("time_stopped","spectator","strict_map_object","magic_academy","rcdenyalways","sin_greed_active","nomagic_active"));
            var profile=game.players().get(p.getUUID());profile.lbcKit=0;profile.lbcChallengeKit=0;profile.lbcChallenge=0;
            profile.lbcTrinket=0;Arrays.fill(profile.trinkets,0);profile.lbcSkill=19;profile.lbcUnlocks.add(UNLOCK);
            game.config().lbcSkills=true;game.config().ranked=false;game.items().magic().inventoryChanged(p);game.items().magic().flushCleansing();
            require(LbcParasite.active(p),"unlocked configured selected19 admitted");
            var advancement=game.server().getAdvancements().get(net.minecraft.resources.Identifier.parse(UNLOCK));
            require(advancement!=null,"source achievement exists");
            profile.lbcUnlocks.remove(UNLOCK);p.entityTags().remove("skywars2.debug_perks");
            for(var criterion:advancement.value().criteria().keySet())p.getAdvancements().revoke(advancement,criterion);
            require(!LbcParasite.active(p),"source achievement required");
            for(var criterion:advancement.value().criteria().keySet())p.getAdvancements().award(advancement,criterion);
            profile.lbcUnlocks.add(UNLOCK);
            game.config().lbcSkills=false;require(!LbcParasite.active(p),"global skills gate");game.config().lbcSkills=true;
            p.entityTags().add("spectator");require(!LbcParasite.active(p),"observer role cannot acquire gameplay immunity");p.entityTags().remove("spectator");
            p.entityTags().add("time_stopped");require(LbcParasite.active(p)&&!p.addEffect(effect(MobEffects.POISON,0),p),"frozen admission still rejects new harm");p.entityTags().remove("time_stopped");
            nativeAdmission(p);storedLayers(game,p);mixedWater(game,p);customAdmission(game,p);jammers(game,p);penalties(game,p);
        }finally{
            game.players().get(p.getUUID()).lbcSkill=0;
            LbcParasite.reconcile(p);game.items().ownedEffects().withdraw(p,"parasite-fixture");
            game.items().blaze().clearEffects(p);
            game.config().lbcSkills=skills;game.config().ranked=ranked;snapshot.restore(game);snapshot.restoreAttributes();snapshot.restoreUse();
            game.items().magic().inventoryChanged(p);game.players().save();
        }
        SkyWars2.LOGGER.info("[check] PASS parasite");
    }
    private static void nativeAdmission(ServerPlayer p){
        for(var type:List.of(MobEffects.POISON,MobEffects.WITHER,MobEffects.SLOWNESS,MobEffects.WEAKNESS,MobEffects.MINING_FATIGUE,
                MobEffects.HUNGER,MobEffects.BLINDNESS,MobEffects.DARKNESS,MobEffects.LEVITATION,MobEffects.GLOWING,MobEffects.BAD_OMEN,
                MobEffects.TRIAL_OMEN,MobEffects.INFESTED,MobEffects.OOZING,MobEffects.WEAVING,MobEffects.WIND_CHARGED)){
            require(!p.canBeAffected(effect(type,0)),"native canBeAffected rejects "+type);
            require(!p.addEffect(effect(type,0),p)&&!p.hasEffect(type),"native addEffect rejects before first execution "+type);
        }
        for(int amp:new int[]{20,40,41,42,74,101,102,103,104,105,106,107,108,109,110,111,112,113,114,115,116,117,118,119,120,121,122,123,124,126})
            require(!p.addEffect(effect(MobEffects.RAID_OMEN,amp),p),"harmful sentinel rejected before translation "+amp);
        for(int amp:new int[]{16,17,18,19})require(!LbcParasite.rejects(effect(MobEffects.RAID_OMEN,amp)),"useful mana transport "+amp);
        for(var type:List.of(MobEffects.SPEED,MobEffects.STRENGTH,MobEffects.RESISTANCE,MobEffects.REGENERATION))
            require(p.addEffect(effect(type,0),p),"positive boon admitted "+type);
        for(var type:List.of(MobEffects.GLOWING,MobEffects.POISON,MobEffects.WITHER,MobEffects.SLOWNESS,MobEffects.WEAKNESS))
            require(p.addEffect(effect(type,-2),p),"useful inverted mechanic retained "+type);
        require(!p.addEffect(effect(MobEffects.REGENERATION,-2),p),"inverted regeneration hurts and is rejected");
        p.setHealth(Math.min(10,p.getMaxHealth()));float health=p.getHealth();
        MobEffects.INSTANT_DAMAGE.value().applyInstantaneousEffect(p.level(),null,null,p,0,1);
        near(p.getHealth(),health,"direct positive instant harm prevented before damage");
        MobEffects.INSTANT_HEALTH.value().applyInstantaneousEffect(p.level(),null,null,p,-2,1);
        near(p.getHealth(),health,"direct negative instant health prevented before damage");
        require(MobEffects.INSTANT_DAMAGE.value().applyEffectTick(p.level(),p,0),"direct harmful instant tick reports handled");
        near(p.getHealth(),health,"direct positive instant harm tick prevented before damage");
        require(MobEffects.INSTANT_HEALTH.value().applyEffectTick(p.level(),p,-2),"direct inverted instant tick reports handled");
        near(p.getHealth(),health,"direct negative instant health tick prevented before damage");
        MobEffects.INSTANT_DAMAGE.value().applyInstantaneousEffect(p.level(),null,null,p,-2,1);
        require(p.getHealth()>health,"negative instant harm still heals");
        p.setHealth(health);MobEffects.INSTANT_HEALTH.value().applyInstantaneousEffect(p.level(),null,null,p,0,1);
        require(p.getHealth()>health,"positive instant health still heals");
        p.setHealth(health);var game=SkyWars2.game();game.items().pills().consumed(p,LbcPills.stack(72));
        near(p.getHealth(),health,"actual native bad instant pill causes no damage");
        game.items().pills().consumed(p,LbcPills.stack(74));require(p.getHealth()>health,"actual native good instant pill heals");
        p.removeAllEffects();
    }
    private static void storedLayers(GameController game,ServerPlayer p){
        var profile=game.players().get(p.getUUID());profile.lbcSkill=0;
        require(game.items().ownedEffects().grant(p,"parasite-fixture",effect(MobEffects.POISON,-2),p),"useful hidden baseline lease admitted");
        require(p.addEffect(effect(MobEffects.POISON,2),p),"already stored harmful head");
        p.addEffect(effect(MobEffects.ABSORPTION,2),p);p.addEffect(effect(MobEffects.ABSORPTION,-2),p);
        require(ShadowAbsorption.state(p).entries.size()==2,"native signed absorption ledger contains both signs");
        profile.lbcSkill=19;
        try{var method=net.minecraft.world.entity.LivingEntity.class.getDeclaredMethod("tickEffects");method.setAccessible(true);method.invoke(p);}
        catch(ReflectiveOperationException error){throw new IllegalStateException("native tickEffects",error);}
        var healed=p.getEffect(MobEffects.POISON);require(healed!=null&&healed.getAmplifier()==-2,"loaded harmful head removed before native pulse, useful hidden survives");
        require(ShadowAbsorption.state(p).entries.keySet().equals(Set.of(2)),"only harmful absorption debt removed");
        game.items().ownedEffects().withdraw(p,"parasite-fixture");require(!p.hasEffect(MobEffects.POISON),"useful survivor retains owner withdrawal");
        p.removeAllEffects();
        profile.lbcSkill=0;
        require(p.addEffect(effect(MobEffects.SPEED,-2),p)&&p.addEffect(effect(MobEffects.SPEED,0),p),"loaded useful head and harmful trailing layer");
        require(((EffectLayers.Access)p.getEffect(MobEffects.SPEED)).skywars2$layerHidden()!=null,"native trailing layer really exists");
        profile.lbcSkill=19;LbcParasite.beforeEffects(p);
        var speed=p.getEffect(MobEffects.SPEED);
        require(speed!=null&&speed.getAmplifier()==0&&((EffectLayers.Access)speed).skywars2$layerHidden()==null,
                "harmful trailing hidden node is removed beneath useful head");
        p.removeAllEffects();
    }
    private static java.lang.reflect.Field field(Object actor,String name)throws NoSuchFieldException{
        for(Class<?> type=actor.getClass();type!=null;type=type.getSuperclass())try{var found=type.getDeclaredField(name);found.setAccessible(true);return found;}catch(NoSuchFieldException ignored){}
        throw new NoSuchFieldException(name);
    }
    /** Actual resident fluid at the native eye coordinate; retain the boon only while useful. */
    private static void mixedWater(GameController game,ServerPlayer p){
        var pos=net.minecraft.core.BlockPos.containing(p.getX(),p.getEyeY(),p.getZ());
        require(p.level().hasChunkAt(pos)&&p.level().getBlockState(pos).isAir(),"resident empty native eye cell for bounded water fixture");
        var saved=game.maps().snapshot(p.level(),pos);int air=p.getAirSupply();
        try{
            // 26.3 owns eye state inside EntityFluidInteraction rather than fluidOnEyes.
            // Scope a fresh native tracker, preserving the original tracker object and every
            // cached current/height/eye value intact. update() reads actual resident fluid.
            var fluidField=field(p,"fluidInteraction");var previousFluid=fluidField.get(p);
            var fluid=new net.minecraft.world.entity.EntityFluidInteraction(Set.of(net.minecraft.tags.FluidTags.WATER,net.minecraft.tags.FluidTags.LAVA));
            fluidField.set(p,fluid);
            try{
                p.level().setBlock(pos,net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(),2);fluid.update(p,true);
                require(p.isEyeInFluid(net.minecraft.tags.FluidTags.WATER),"actual native eye immersed in resident water");
                game.players().get(p.getUUID()).lbcSkill=0;
                require(p.addEffect(effect(MobEffects.WATER_BREATHING,-2),p),"already-loaded harmful breathing debt");
                require(p.addEffect(effect(MobEffects.WATER_BREATHING,1),p),"Water Breathing II covers loaded harmful hidden layer");
                game.players().get(p.getUUID()).lbcSkill=19;
                require(p.canBeAffected(effect(MobEffects.WATER_BREATHING,1)),"Water Breathing II admitted while useful underwater");
                require(p.addEffect(effect(MobEffects.SPEED,0),p),"unrelated useful boon present through transition");
                p.setAirSupply(42);float health=p.getHealth();LbcParasite.beforeEffects(p);SignedEnvironmentalEffects.airTick(p);
                require(p.hasEffect(MobEffects.WATER_BREATHING)&&MobEffectUtil.hasWaterBreathing(p),"native underwater breathing II boon survives cleansing");
                require(((EffectLayers.Access)p.getEffect(MobEffects.WATER_BREATHING)).skywars2$layerHidden()==null,"harmful hidden breathing debt removed under useful underwater head");
                require(p.getAirSupply()==42&&p.getHealth()==health,"underwater status does not drain air or HP");
                p.level().setBlock(pos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);fluid.update(p,true);
                require(!p.isEyeInFluid(net.minecraft.tags.FluidTags.WATER),"actual native eye transitions onto land");
                SignedEnvironmentalEffects.airTick(p);
                require(!p.hasEffect(MobEffects.WATER_BREATHING),"mixed breathing removed before first harmful land air action");
                require(p.getAirSupply()==42&&p.getHealth()==health&&p.hasEffect(MobEffects.SPEED),"first land action preserves air/HP and unrelated useful boon");
            }finally{
                game.maps().restoreSnapshot(p.level(),pos,saved);
                fluidField.set(p,previousFluid);p.setAirSupply(air);
                p.removeEffect(MobEffects.WATER_BREATHING);p.removeEffect(MobEffects.SPEED);
            }
        }catch(ReflectiveOperationException error){game.maps().restoreSnapshot(p.level(),pos,saved);p.setAirSupply(air);throw new IllegalStateException("native mixed breathing fixture",error);}
    }
    private static void customAdmission(GameController game,ServerPlayer p){
        var items=game.items();var mana=items.mana();mana.set(p,1234);mana.applyNoMagic(p,10);mana.reverse(p,10);
        require(mana.get(p)==1234&&mana.noMagic(p)==0&&mana.reverse(p)==0,"nomagic cannot empty mana before admission; reverse rejected");
        require(!items.electro().shock(p,p,5)&&items.electro().shocks(p)==0,"shock rejected before first pulse");
        items.frost().add(p,p);require(items.frost().cold(p)==0&&!items.frost().isFrozen(p),"cold/freeze rejected before body mutation");
        require(!items.shooters().stun(p,p,20)&&items.shooters().stunTicks(p)==0,"stun rejected before attribute/input mutation");
        require(!items.magic().colors().acceptTeleport(p,effect(MobEffects.RAID_OMEN,40),p),"direct RTP rejected before score queue");
        for(var key:List.of("scared","piss","lookback","twitching","delayed_death","effect_gravity_curse","effect_timestop",
                "dimensional_shifting","end_damage","effect_remove_mana","nomagic","reverse_mana"))mana.score(p,key).set(10);
        require(mana.hudValue(p,"blaze")==0,"exclusive useful head-charge fixture");
        mana.boost(p,50);mana.score(p,"effect_add_mana").set(3);mana.score(p,"void_resistance").set(9);items.blaze().addHeadCharges(p,7);
        LbcParasite.reconcile(p);
        for(var key:List.of("scared","piss","lookback","twitching","delayed_death","effect_gravity_curse","effect_timestop",
                "dimensional_shifting","end_damage","effect_remove_mana","nomagic","reverse_mana"))require(mana.hudValue(p,key)==0,"raw loaded debuff reconciled "+key);
        require(mana.boost(p)==50&&mana.hudValue(p,"effect_add_mana")==3&&mana.hudValue(p,"void_resistance")==9
                &&mana.hudValue(p,"blaze")==7,"boost/add mana/void resistance/useful head charges survive");
    }
    private static void penalties(GameController game,ServerPlayer p){
        var types=attributes();double[] expected={9,9,9,9.9,9.8,10.1,9.9,10.1,9.9,9,9,9,9.9,9.98,9.9,9.8,9.9,9.9,9.9,9.9,9.8,9.8,9.666666657,9.9,9.95,9.95};
        for(int index=1;index<=26;index++){
            for(var type:types)p.getAttribute(type).setBaseValue(10);
            LbcParasitePenalties.apply(p,index);
            for(int i=0;i<types.size();i++)near(p.getAttribute(types.get(i)).getBaseValue(),i==index-1?expected[i]:10,"source penalty "+index+" attribute "+i);
        }
        try{
            var field=Entity.class.getDeclaredField("random");field.setAccessible(true);var previous=field.get(p);
            var worldField=net.minecraft.world.level.Level.class.getDeclaredField("random");worldField.setAccessible(true);var previousWorld=worldField.get(p.level());
            try{
                var actual=RandomSource.create(191926);var expectedRandom=RandomSource.create(191926);field.set(p,actual);worldField.set(p.level(),actual);
                for(var type:types)p.getAttribute(type).setBaseValue(10);
                var expectedBases=new double[26];Arrays.fill(expectedBases,10);
                int rolls=0;
                for(int i=0;i<64;i++){
                    if(expectedRandom.nextFloat()<.4f){int index=expectedRandom.nextInt(26);rolls++;double base=expectedBases[index];
                        // Use the exact native helper on a temporary base, independently of the trigger stream.
                        var attribute=p.getAttribute(types.get(index));double before=attribute.getBaseValue();attribute.setBaseValue(base);
                        LbcParasitePenalties.apply(p,index+1);expectedBases[index]=attribute.getBaseValue();attribute.setBaseValue(before);
                    }
                    LbcParasite.damaged(p,p.damageSources().generic());
                }
                require(rolls>0&&rolls<64,"seeded stream exercises accepted and rejected 40% branches");
                for(int i=0;i<26;i++)near(p.getAttribute(types.get(i)).getBaseValue(),expectedBases[i],"exact bounded40percent stream "+i);
                near(actual.nextFloat(),expectedRandom.nextFloat(),"one chance draw and one bounded26 draw per accepted event");
                // Replace only this player's RNG for one actual native hit; every chance succeeds,
                // every bounded outcome chooses index1. The original RNG object is restored intact.
                var deterministic=(RandomSource)java.lang.reflect.Proxy.newProxyInstance(RandomSource.class.getClassLoader(),new Class<?>[]{RandomSource.class},
                        (proxy,method,args)->{if(method.getName().equals("nextFloat"))return Float.valueOf(0);
                            if(method.getName().equals("nextInt"))return Integer.valueOf(0);
                            return method.invoke(RandomSource.create(19),args);});
                field.set(p,deterministic);worldField.set(p.level(),deterministic);p.removeAllEffects();p.getAttribute(Attributes.ARMOR).removeModifiers();
                p.getAttribute(Attributes.ARMOR).setBaseValue(10);p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
                p.setHealth(100);p.setInvulnerableTime(0);float before=p.getHealth();
                require(p.hurtServer(p.level(),p.damageSources().generic(),1)&&p.getHealth()<before,"actual native accepted hit");
                near(p.getAttribute(Attributes.ARMOR).getBaseValue(),9,"native damage dispatches exactly one Parasite penalty");
            }finally{field.set(p,previous);worldField.set(p.level(),previousWorld);}
        }catch(ReflectiveOperationException error){throw new IllegalStateException("bounded native RNG fixture",error);}
    }
    private static void jammers(GameController game,ServerPlayer p){
        for(var tag:List.of("stopper_magic","stopper")){
            var jammer=new net.minecraft.world.entity.Marker(net.minecraft.world.entity.EntityTypes.MARKER,p.level());
            jammer.setPos(p.position());jammer.entityTags().add(tag);
            try{
                require(p.level().addFreshEntity(jammer),"native jammer fixture");game.items().magic().loaded(jammer);
                require(tag.equals("stopper")?game.items().magic().mechanismSuppressed(p):game.items().magic().beaconSuppressed(p.position()),"canonical jammer active "+tag);
                require(LbcParasite.active(p)&&!p.addEffect(effect(MobEffects.WITHER,0),p),"jammer cannot suppress native debuff immunity "+tag);
                game.items().mana().set(p,1234);game.items().mana().applyNoMagic(p,10);
                require(game.items().mana().get(p)==1234&&game.items().mana().noMagic(p)==0,"jammer cannot suppress direct custom immunity "+tag);
            }finally{game.items().magic().unloaded(jammer);jammer.discard();}
        }
    }
}
