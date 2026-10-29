package me.skywars2.game;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.*;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/** Shared source authority passives. Acquisition and remaining authorities keep their full-chain gate. */
public final class LbcWitchPassives {
    private static final Identifier GREED=Identifier.parse("minecraft:e916adfe-014b-4ff5-82f1-af6ec9226d21");
    private static final List<Holder<MobEffect>> CLEANSE=List.of(MobEffects.BAD_OMEN,MobEffects.BLINDNESS,
            MobEffects.CONDUIT_POWER,MobEffects.DARKNESS,MobEffects.RAID_OMEN,MobEffects.FIRE_RESISTANCE,
            MobEffects.GLOWING,MobEffects.HUNGER,MobEffects.INSTANT_DAMAGE,MobEffects.INVISIBILITY,
            MobEffects.MINING_FATIGUE,MobEffects.NAUSEA,MobEffects.POISON,MobEffects.SLOW_FALLING,
            MobEffects.SLOWNESS,MobEffects.UNLUCK,MobEffects.WATER_BREATHING,MobEffects.WEAKNESS,MobEffects.WITHER);
    private final GameController game;
    private final LbcSourceCounters counters;
    private static final List<String> ENVY_MAX=List.of("mana","manamax","manaregen","nitro","reactivefuel",
            "swordcharged1","swordcharged2","swordcharged3","swordcharged4","swordcharged5","swordcharged6",
            "swordcharging1","swordcharging2","swordcharging3","swordcharging4","swordcharging5","swordcharging6","swordcharging7");
    private static final List<String> ENVY_MIN=List.of("twitching","scared","shock","coldown1","coldown13",
            "coldown16","coldown17","coldown18","coldown18b","coldown25","coldown8","coldownhohol",
            "coldownshish","divider_cd","lookback","coldownMagnum");
    private record EnvyEffect(Holder<MobEffect> effect,int duration,int amplifier,boolean visible){}
    // A negative duration denotes the source's clear-unless-donor-has branch, not a negative effect.
    private static final List<EnvyEffect> ENVY_EFFECTS=List.of(
            new EnvyEffect(MobEffects.SPEED,40,2,true),new EnvyEffect(MobEffects.SLOWNESS,-1,0,true),
            new EnvyEffect(MobEffects.HASTE,40,127,true),new EnvyEffect(MobEffects.MINING_FATIGUE,-1,0,true),
            new EnvyEffect(MobEffects.STRENGTH,40,9,true),new EnvyEffect(MobEffects.INSTANT_HEALTH,2,99,true),
            new EnvyEffect(MobEffects.INSTANT_DAMAGE,-1,0,true),new EnvyEffect(MobEffects.JUMP_BOOST,40,2,true),
            new EnvyEffect(MobEffects.NAUSEA,-1,0,true),new EnvyEffect(MobEffects.REGENERATION,40,20,true),
            new EnvyEffect(MobEffects.RESISTANCE,40,4,true),new EnvyEffect(MobEffects.FIRE_RESISTANCE,40,0,true),
            new EnvyEffect(MobEffects.WATER_BREATHING,-1,0,true),new EnvyEffect(MobEffects.INVISIBILITY,40,1,false),
            new EnvyEffect(MobEffects.BLINDNESS,-1,0,true),new EnvyEffect(MobEffects.NIGHT_VISION,500,0,true),
            new EnvyEffect(MobEffects.HUNGER,-1,0,true),new EnvyEffect(MobEffects.WEAKNESS,-1,0,true),
            new EnvyEffect(MobEffects.POISON,-1,0,true),new EnvyEffect(MobEffects.WITHER,-1,0,true),
            new EnvyEffect(MobEffects.HEALTH_BOOST,40,9,true),new EnvyEffect(MobEffects.ABSORPTION,40,9,true),
            new EnvyEffect(MobEffects.SATURATION,2,100,true),new EnvyEffect(MobEffects.GLOWING,-1,0,true),
            new EnvyEffect(MobEffects.LUCK,40,9,true),new EnvyEffect(MobEffects.UNLUCK,-1,0,true),
            new EnvyEffect(MobEffects.SLOW_FALLING,40,0,true),new EnvyEffect(MobEffects.CONDUIT_POWER,40,2,true),
            new EnvyEffect(MobEffects.DARKNESS,-1,0,true));
    private final Map<String,EntitySelector> selectors=new HashMap<>();
    private final Set<ServerPlayer> managed=Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<Entity,String> hearted=new WeakHashMap<>();
    private int lastTick=Integer.MIN_VALUE;
    public LbcWitchPassives(GameController game){this.game=game;this.counters=new LbcSourceCounters(game);}
    private boolean eligible(ServerPlayer p){return game.playing()&&game.stage()!=GameController.Stage.ENDING
            &&p.isAlive()&&!p.isRemoved()&&!game.isObserver(p)&&game.canUseGameplayItems(p)
            &&LbcDimensionItems.ordinary(p)&&game.items().wands().validRoleTarget(p,false);}
    private boolean paused(ServerPlayer p){return game.items().timeStop().stopped(p.level())
            ||game.items().wands().timeStopped()||game.items().wands().inputTimeStopped(p);}
    private List<? extends Entity> select(ServerPlayer p,String literal){try{
        var selector=selectors.get(literal);
        if(selector==null){selector=new EntitySelectorParser(new StringReader(literal),true).parse();selectors.put(literal,selector);}
        return List.copyOf(selector.findEntities(p.createCommandSourceStack().withPermission(PermissionSet.ALL_PERMISSIONS).withSuppressedOutput()));
    }catch(CommandSyntaxException error){throw new IllegalStateException("Witch source selector "+literal,error);}}
    /** Once per real tick; a stopped authority retains its prior state without advancing source pulses. */
    public void tick(){
        int now=game.server().getTickCount();if(lastTick==now)return;lastTick=now;
        for(var entry:List.copyOf(hearted.entrySet()))if(entry.getKey().isRemoved()||!game.items().currentEpoch(entry.getValue())){
            entry.getKey().entityTags().remove("sin_greed_have_heart");hearted.remove(entry.getKey());
        }
        Boolean heartExists=null;
        for(var p:List.copyOf(game.server().getPlayerList().getPlayers())){
            if(!eligible(p)){if(managed.contains(p))removeAuthority(p);continue;}
            if(paused(p))continue;
            if(p.entityTags().contains("sin_greed")){
                managed.add(p);
                // No distance/AABB in the source selector: this is one global loaded-entity gate.
                if(heartExists==null)heartExists=!select(p,"@e[tag=sin_greed_have_heart,tag=!sin_greed,tag=!spectator]").isEmpty();
                greed(p,heartExists);
                if(now%20==0){hearts(p);heartExists=null;}
            }else if(managed.contains(p))removeAuthority(p);
            if(p.entityTags().contains("sin_lust")){
                if(now%20==0)p.addEffect(ItemEffectLevels.create(MobEffects.REGENERATION,20,4,false,false),p);
                // EnderWiki already owns falltimer; never introduce a second falling clock.
                if(!p.onGround()&&game.items().mana().hudValue(p,"falltimer")>=10&&!magicBeacon(p))
                    p.addEffect(ItemEffectLevels.create(MobEffects.SLOW_FALLING,20,0,false,false),p);
            }
            if(p.entityTags().contains("sin_envy"))envy(p,now%20==0);
            if(now%20==0){drainExisting(p,"lust_cd");drainExisting(p,"lust_cd_small");}
        }
    }
    private void envy(ServerPlayer player,boolean second){
        // Unbounded native @p searches loaded player dimensions; preserve the exact source selector.
        var donors=select(player,"@p[gamemode=!spectator,tag=!sin_envy]");
        if(donors.isEmpty())return;var donor=(ServerPlayer)donors.getFirst();
        for(var name:ENVY_MAX)counters.copyBetter(player,donor,name,true);
        for(var name:ENVY_MIN)counters.copyBetter(player,donor,name,false);
        boolean changed=false;
        // Source inventory.0..26 precedes hotbar.0..8. Equipment is copied only into empty slots too.
        for(int step=0;step<36;step++){
            int slot=(step+9)%36;var item=donor.getInventory().getItem(slot);
            if(player.getInventory().getItem(slot).isEmpty()&&!item.isEmpty()){
                player.getInventory().setItem(slot,item.copy());changed=true;
            }
        }
        for(var slot:List.of(EquipmentSlot.OFFHAND,EquipmentSlot.FEET,EquipmentSlot.LEGS,EquipmentSlot.CHEST,EquipmentSlot.HEAD)){
            var item=donor.getItemBySlot(slot);
            if(player.getItemBySlot(slot).isEmpty()&&!item.isEmpty()){
                player.setItemSlot(slot,item.copy());changed=true;
            }
        }
        if(changed){player.getInventory().setChanged();game.items().magic().inventoryChanged(player);player.inventoryMenu.broadcastChanges();}
        if(second)for(var rule:ENVY_EFFECTS){
            boolean present=donor.hasEffect(rule.effect);
            if(rule.duration<0){if(!present)player.removeEffect(rule.effect);}
            else if(present)player.addEffect(ItemEffectLevels.create(rule.effect,rule.duration,rule.amplifier,false,rule.visible),player);
        }
    }
    private boolean magicBeacon(ServerPlayer p){return p.level()==game.maps().level()&&game.items().magic().beaconSuppressed(p.position());}
    /** A source sword callback consumes the actual accepted melee target, including every native sweep hit. */
    public void damaged(net.minecraft.world.entity.LivingEntity victim,net.minecraft.world.damagesource.DamageSource source){
        if(!(source.getEntity() instanceof ServerPlayer player)||!LbcDamage.isMeleeAttack(source,player)
                ||!eligible(player)||paused(player)||!player.entityTags().contains("sin_lust")
                ||player.entityTags().contains("nomagic_active")||game.items().magic().suppressed(player)
                ||!game.items().wands().target(player,victim)||victim.entityTags().contains("lust_dragon"))return;
        double distance=player.position().distanceToSqr(victim.position());
        if(distance<.0001||distance>36)return;
        transferHealth(player,victim,2);
    }
    /** Shared by Lust melee and its flesh spell. Source integer base arithmetic does not heal the wearer. */
    void transferHealth(ServerPlayer owner,net.minecraft.world.entity.LivingEntity target,int amount){
        var own=owner.getAttribute(Attributes.MAX_HEALTH);var other=target.getAttribute(Attributes.MAX_HEALTH);
        if(own==null||other==null)return;
        own.setBaseValue(net.minecraft.util.Mth.floor(own.getBaseValue())+amount);
        other.setBaseValue(net.minecraft.util.Mth.floor(other.getBaseValue())-amount);
        if(net.minecraft.util.Mth.floor(other.getBaseValue())<=0
                &&game.items().mana().hudValue(target,"void_resistance")<=0&&!LbcGreed.active(target))
            game.items().indirectKills().inheritedKill(target,game.items().indirectKills().wildCause(owner));
    }
    private void drainExisting(ServerPlayer p,String name){var objective=game.server().getScoreboard().getObjective(name);
        if(objective==null)return;var info=game.server().getScoreboard().getPlayerScoreInfo(p,objective);
        if(info!=null&&info.value()>0)game.server().getScoreboard().getOrCreatePlayerScore(p,objective,true).set(info.value()-1);
    }
    private static void modifier(ServerPlayer p,Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,double amount){
        var instance=p.getAttribute(attribute);if(instance!=null&&!instance.hasModifier(GREED))
            instance.addPermanentModifier(new AttributeModifier(GREED,amount,AttributeModifier.Operation.ADD_VALUE));
    }
    private void greed(ServerPlayer p,boolean active){
        if(!active){p.entityTags().remove("sin_greed_active");removeModifiers(p);return;}
        p.entityTags().add("sin_greed_active");
        p.addEffect(ItemEffectLevels.create(MobEffects.RESISTANCE,40,4,false,false),p);
        p.addEffect(ItemEffectLevels.create(MobEffects.INSTANT_HEALTH,2,4,false,false),p);
        // The shared absolute guard rejects anonymous periodic instant ticks. This is the authority's
        // own one-tick pulse, using the native instantaneous entry and its existing self-source rule.
        MobEffects.INSTANT_HEALTH.value().applyInstantaneousEffect(p.level(),p,p,p,4,1);
        modifier(p,Attributes.ATTACK_DAMAGE,10);modifier(p,Attributes.KNOCKBACK_RESISTANCE,10000);
        var first=BlockPos.containing(p.position().add(0,.99,0));var last=BlockPos.containing(p.position().add(0,1.01,0));
        game.items().indirectKills().worldChanges(p.level(),p,true,()->{
            for(var cell:BlockPos.betweenClosed(first,last))if(p.level().hasChunkAt(cell)&&!p.level().isOutsideBuildHeight(cell))
                p.level().destroyBlock(cell,true);
        });
        if(!select(p,"@s[y=-30,dy=-10000]").isEmpty()){
            Vec3 motion=p.getDeltaMovement();
            try{p.teleportTo(p.level(),p.getX(),322,p.getZ(),TeleportMomentum.positionFlags(Set.of()),p.getYRot(),p.getXRot(),true);}
            finally{p.setDeltaMovement(motion);}
        }
        for(var effect:CLEANSE)p.removeEffect(effect);
        // A missing score does not match the source's ..20 range.
        var objective=game.server().getScoreboard().getObjective("coldownshish");
        var value=objective==null?null:game.server().getScoreboard().getPlayerScoreInfo(p,objective);
        if(value!=null&&value.value()<=20)p.removeEffect(MobEffects.LEVITATION);
        for(var status:List.of("lookback","scared","twitching"))game.items().psycho().clearStatus(p,status);
        game.items().electro().clearEffects(p);
    }
    /** Source first-target selection and two independent draws, not a random choice of nearby targets. */
    private void hearts(ServerPlayer p){
        if(select(p,"@e[type=#minecraft:mobs,distance=..5,tag=!spectator]").isEmpty())return;
        var mobs=select(p,"@e[type=#minecraft:mobs,type=!minecraft:player,distance=..5,tag=!sin_greed_have_heart,limit=1]");
        if(!mobs.isEmpty()&&p.level().getRandom().nextFloat()<.4f)heart(mobs.getFirst());
        var players=select(p,"@a[gamemode=!spectator,distance=..5,tag=!sin_greed,tag=!sin_greed_have_heart,limit=1]");
        if(!players.isEmpty()&&p.level().getRandom().nextFloat()<.00015625f)heart(players.getFirst());
        var audience=game.server().getPlayerList().getPlayers().stream().filter(viewer->viewer.entityTags().contains("sin_greed")).toList();
        for(var entity:select(p,"@e[type=#minecraft:mobs,distance=..5,tag=sin_greed_have_heart,tag=!spectator]"))
            for(var viewer:audience)((net.minecraft.server.level.ServerLevel)entity.level()).sendParticles(viewer,ParticleTypes.SOUL,false,false,
                    entity.getX(),entity.getY()+2,entity.getZ(),10,.3,.3,.3,0);
    }
    private void heart(Entity entity){
        var level=(net.minecraft.server.level.ServerLevel)entity.level();
        level.sendParticles(ParticleTypes.HEART,entity.getX(),entity.getY()+1,entity.getZ(),10,.3,.6,.3,0);
        LbcOverload.sound(level,entity.position(),"minecraft:block.enchantment_table.use",1,1,0,0);
        level.sendParticles(ParticleTypes.SOUL,entity.getX(),entity.getY()+1,entity.getZ(),20,.3,.6,.3,0);
        if(entity instanceof Mob mob)mob.setPersistenceRequired();
        if(entity.entityTags().add("sin_greed_have_heart"))hearted.put(entity,game.items().epoch());
    }
    private static void removeModifiers(ServerPlayer p){for(var attribute:List.of(Attributes.ATTACK_DAMAGE,Attributes.KNOCKBACK_RESISTANCE)){
        var instance=p.getAttribute(attribute);if(instance!=null)instance.removeModifier(GREED);
    }}
    private void removeAuthority(ServerPlayer p){if(managed.remove(p)){removeModifiers(p);p.entityTags().remove("sin_greed_active");}}
    public void forget(ServerPlayer p){removeAuthority(p);if(hearted.remove(p)!=null)p.entityTags().remove("sin_greed_have_heart");}
    int count(){return managed.size()+hearted.size();}
    public void stop(){for(var p:List.copyOf(managed))removeAuthority(p);for(var e:List.copyOf(hearted.keySet()))e.entityTags().remove("sin_greed_have_heart");hearted.clear();lastTick=Integer.MIN_VALUE;}
}
