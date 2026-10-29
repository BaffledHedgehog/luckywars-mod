package me.skywars2.game;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.*;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/** Current-holder Wrath control, with native source selectors and the existing team/damage owners. */
public final class LbcWitchWrath {
    private final GameController game;
    private final Map<String,EntitySelector> selectors=new HashMap<>();
    private record Death(ServerLevel level,String epoch,LbcIndirectKills.Owner cause){}
    private final List<Death> deaths=new ArrayList<>();
    private int lastTick=Integer.MIN_VALUE;
    LbcWitchWrath(GameController game){this.game=game;}
    private List<? extends Entity> select(ServerPlayer owner,ServerLevel level,Vec3 at,String literal){try{
        var selector=selectors.get(literal);
        if(selector==null){selector=new EntitySelectorParser(new StringReader(literal),true).parse();selectors.put(literal,selector);}
        return List.copyOf(selector.findEntities(owner.createCommandSourceStack().withLevel(level).withPosition(at)
                .withPermission(PermissionSet.ALL_PERMISSIONS).withSuppressedOutput()));
    }catch(CommandSyntaxException error){throw new IllegalStateException("Wrath source selector "+literal,error);}}
    private boolean holder(ServerPlayer p){return p.entityTags().contains("sin_wrath")&&p.isAlive()&&!p.isRemoved()
            &&game.canUseGameplayItems(p)&&!game.isObserver(p)&&LbcDimensionItems.ordinary(p);}
    private boolean paused(ServerPlayer p){return game.items().wands().timeStopped()||game.items().wands().inputTimeStopped(p)
            ||game.items().timeStop().stopped(p.level());}
    private boolean protectedAt(Entity target){return !LbcDimensionItems.ordinary(target.level())||target.entityTags().contains("nomagic_active")
            ||game.items().magic().suppressedAt((ServerLevel)target.level(),target.position());}
    private boolean target(Entity e,boolean boss){return e.isAlive()&&!e.isRemoved()&&!e.isSpectator()
            &&!e.entityTags().contains("spectator")&&!e.entityTags().contains("strict_map_object")
            &&!e.entityTags().contains("magic_academy")&&!e.entityTags().contains("rcdenyalways")
            &&(boss||!e.entityTags().contains("sauvojen_tuntija"))
            &&(!(e instanceof ServerPlayer p)||game.canUseGameplayItems(p))&&!protectedAt(e)
            &&!game.items().timeStop().stopped((ServerLevel)e.level())&&!game.items().timeStop().frozen(e);}
    public void tick(){
        int now=game.server().getTickCount();if(lastTick==now)return;lastTick=now;
        if(!game.playing()||game.stage()==GameController.Stage.ENDING)return;
        drainDeaths();
        for(var p:List.copyOf(game.server().getPlayerList().getPlayers()))if(holder(p)&&!paused(p)
                &&!p.entityTags().contains("nomagic_active")&&!game.items().magic().suppressed(p))wrath(p);
    }
    private void wrath(ServerPlayer owner){
        // No distance/AABB on the original mob/affected-player selectors: native scope is global.
        var mobs=select(owner,owner.level(),owner.position(),"@e[type=#minecraft:mobs,type=!minecraft:player]");
        for(var entity:mobs)if(target(entity,true)){
            if(!entity.entityTags().contains("affected_by_wrath")){
                convertAt(owner,(ServerLevel)entity.level(),entity.position(),false);
                entity.entityTags().add("affected_by_wrath");
            }else if(game.items().communism().owner(entity)!=owner){
                // A transferred authority really transfers live control; accepted old child causes stay intact.
                game.items().communism().subjugate(owner,entity,true,true);
            }
        }
        for(var player:select(owner,owner.level(),owner.position(),"@a[gamemode=!spectator,distance=0.01..30,tag=!affected_by_wrath]"))
            if(target(player,false)&&owner.level().getRandom().nextFloat()<.01f)convertAt(owner,owner.level(),player.position(),true);
        for(var entity:mobs)if(entity instanceof LivingEntity living&&target(entity,false)){
            var health=living.getAttribute(Attributes.MAX_HEALTH);if(health!=null){
                health.setBaseValue(Mth.floor(owner.getAttribute(Attributes.MAX_HEALTH).getBaseValue()));
                living.setHealth(Mth.floor(owner.getHealth()));
            }
        }
        for(var entity:select(owner,owner.level(),owner.position(),"@a[gamemode=!spectator,tag=affected_by_wrath,tag=!sin_wrath,tag=!sin_greed_active]"))
            if(entity instanceof ServerPlayer player&&target(player,false))health(owner,player);
    }
    /** Native dx=dy=dz=0 is a positive unit box, plus the source 15-block distance gate. */
    void convertAt(ServerPlayer owner,ServerLevel level,Vec3 at,boolean players){
        boolean lock=players&&game.config().gameMode>=3&&game.config().gameMode<=4;
        if(lock)for(var player:game.server().getPlayerList().getPlayers())player.entityTags().add("cantleave");
        try{
            String filter=players?"@e[type=#minecraft:mobs,dx=0,dy=0,dz=0,distance=..15,tag=!spectator,tag=!cantleave]"
                    :"@e[type=#minecraft:mobs,type=!minecraft:player,dx=0,dy=0,dz=0,distance=..15]";
            var targets=select(owner,level,at,filter);
            for(var e:targets)if(target(e,true))game.items().communism().subjugate(owner,e,
                    !players||!e.entityTags().contains("sin_wrath"),!players);
            if(!players)LbcOverload.sound(level,at,"minecraft:entity.villager.trade",1,1,0,0);
            if(players)for(var e:select(owner,level,at,"@p[gamemode=!spectator,distance=..2,tag=!sin_wrath]"))
                if(target(e,false))e.entityTags().add("affected_by_wrath");
        }finally{
            // (user) This intentionally also clears preexisting cantleave on ALL players.
            if(players)for(var player:game.server().getPlayerList().getPlayers())player.entityTags().remove("cantleave");
        }
    }
    private void health(ServerPlayer owner,ServerPlayer target){
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(Mth.floor(owner.getMaxHealth()));
        // The old hplvl criterion is ceil(HP+absorption). Read its actual native owner, without a stale dummy copy.
        int theirs=Mth.ceil(target.getHealth()+target.getAbsorptionAmount());
        int ours=Mth.ceil(owner.getHealth()+owner.getAbsorptionAmount());
        if(theirs<ours)target.addEffect(ItemEffectLevels.create(MobEffects.INSTANT_HEALTH,1,99,false,true),owner);
        else if(theirs>ours){
            int amount=(int)Math.min(Integer.MAX_VALUE,Math.abs((long)Mth.floor(owner.getHealth())-Mth.floor(target.getHealth())));
            if(amount>0&&game.items().mana().hudValue(target,"void_resistance")<=0){
                var cause=game.items().indirectKills().wildCause(owner);
                target.hurtServer(target.level(),game.items().indirectKills().captureDamage(
                        owner.damageSources().source(DamageTypes.OUTSIDE_BORDER,owner),cause),amount);
            }
            target.removeEffect(MobEffects.HEALTH_BOOST);
        }
    }
    /** Called before the core clears the dead holder. Never runs a second player-death/lives system. */
    public void died(ServerPlayer owner){
        if(!game.playing()||!game.allowDamage(owner)||!owner.entityTags().contains("sin_wrath")||!LbcDimensionItems.ordinary(owner)
                ||owner.entityTags().contains("nomagic_active")||game.items().magic().suppressed(owner))return;
        deaths.add(new Death(owner.level(),game.items().epoch(),game.items().indirectKills().wildCause(owner)));drainDeaths();
    }
    private void drainDeaths(){
        for(var death:List.copyOf(deaths)){
            if(!game.items().currentEpoch(death.epoch)){deaths.remove(death);continue;}
            if(game.items().wands().timeStopped()||game.items().timeStop().stopped(death.level))continue;
            deaths.remove(death);
            var victims=new ArrayList<LivingEntity>();
            for(var level:game.server().getAllLevels())for(var e:level.getAllEntities())
                if(e instanceof LivingEntity living&&e.entityTags().contains("affected_by_wrath")
                        &&!e.entityTags().contains("sin_wrath")&&!LbcGreed.active(e)&&target(e,false))victims.add(living);
            for(var victim:victims)game.items().indirectKills().inheritedKill(victim,death.cause);
        }
    }
    public void stop(){deaths.clear();lastTick=Integer.MIN_VALUE;}
}
