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
    private final Map<String,EntitySelector> selectors=new HashMap<>();
    private final Set<ServerPlayer> managed=Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<Entity,String> hearted=new WeakHashMap<>();
    private int lastTick=Integer.MIN_VALUE;
    public LbcWitchPassives(GameController game){this.game=game;}
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
            if(now%20==0){drainExisting(p,"lust_cd");drainExisting(p,"lust_cd_small");}
        }
    }
    private boolean magicBeacon(ServerPlayer p){return p.level()==game.maps().level()&&game.items().magic().beaconSuppressed(p.position());}
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
