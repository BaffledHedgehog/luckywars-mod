package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Synchronous native boundary fixture; pulseShard bypasses elapsed cadence only. */
public final class IceMaterialChecks {
    private IceMaterialChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Ice materials: "+why);}
    private static Object copy(Object value){
        if(value instanceof int[] array)return array.clone();
        if(value instanceof Set<?> set)return new HashSet<>(set);
        return value;
    }
    private static Map<java.lang.reflect.Field,Object> profile(PlayerStore.Profile profile){
        var result=new LinkedHashMap<java.lang.reflect.Field,Object>();
        try{for(var field:PlayerStore.Profile.class.getFields())if(!java.lang.reflect.Modifier.isStatic(field.getModifiers()))result.put(field,copy(field.get(profile)));}
        catch(IllegalAccessException error){throw new IllegalStateException(error);}
        return result;
    }
    private static ItemStack loot(GameController game,ServerPlayer player,String key){
        var rolls=game.lbc().rollTable(player.level(),player.position(),player,"lbc:ported/"+key);
        require(rolls.size()==1&&rolls.getFirst().getCount()==1,"live "+key+" exact single result");return rolls.getFirst();
    }
    private static boolean done(GameController game,ServerPlayer player,String key){
        var advancement=game.server().getAdvancements().get(Identifier.parse("lbc:true_advancements/"+key));
        return advancement!=null&&player.getAdvancements().getOrStartProgress(advancement).isDone();
    }
    public static void run(GameController game){
        var player=game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestAlpha"));
        require(player!=null&&game.playing()&&game.canUseGameplayItems(player),"active native TestAlpha participant");
        var mana=game.items().mana();var frost=game.items().frost();var service=game.items().iceMaterials();
        require(frost.cold(player)==0&&!frost.isFrozen(player)&&!game.items().timeStop().stopped(player.level()),"clean initial frost and Time Stop");
        var saved=new GameChecks.WandThirdPlayerState(player,mana);var targetProfile=game.players().get(player.getUUID());
        var profile=profile(targetProfile);boolean skills=game.config().lbcSkills;
        var stats=new LinkedHashMap<net.minecraft.stats.Stat<Identifier>,Integer>();
        for(var id:BuiltInRegistries.CUSTOM_STAT){var stat=Stats.CUSTOM.get(id);stats.put(stat,player.getStats().getValue(stat));}
        var advancements=new LinkedHashMap<AdvancementHolder,Set<String>>();
        for(var advancement:game.server().getAdvancements().getAllAdvancements()){
            var criteria=new HashSet<String>();player.getAdvancements().getOrStartProgress(advancement).getCompletedCriteria().forEach(criteria::add);advancements.put(advancement,criteria);
        }
        int admission=mana.score(net.minecraft.world.scores.ScoreHolder.forNameOnly("neutral_ball_mana_activate"),"lbc.math").get();
        Marker stopper=null;
        try{
            player.closeContainer();player.setGameMode(GameType.SURVIVAL);player.setNoGravity(true);player.removeAllEffects();
            player.teleportTo(player.level(),player.getX(),player.getY()+6,player.getZ(),Set.of(),0,0,true);
            player.getInventory().setSelectedSlot(0);player.getInventory().clearContent();mana.noMagic(player,0);
            game.items().magic().inventoryChanged(player);game.items().magic().flushCleansing();
            player.entityTags().remove("time_stopped");targetProfile.lbcSkill=0;game.config().lbcSkills=false;
            for(String name:java.util.List.of("accesories/jaakivi","arcanums/neutral")){
                var advancement=game.server().getAdvancements().get(Identifier.parse("lbc:true_advancements/"+name));
                require(advancement!=null,"source advancement exists "+name);
                for(String criterion:advancement.value().criteria().keySet())player.getAdvancements().revoke(advancement,criterion);
            }
            var shard=loot(game,player,"runic_ice_shard");var ball=loot(game,player,"neutral_ball");
            for(var stack:java.util.List.of(shard,ball)){
                boolean isShard=LbcIceMaterials.isRunicIceShard(stack);String name=isShard?"runic_ice_shard":"neutral_ball";
                require(stack.is(isShard?LbcIceMaterials.RUNIC_ICE_SHARD:LbcIceMaterials.NEUTRAL_BALL)
                        &&Identifier.parse("lbc:"+name).equals(stack.get(DataComponents.ITEM_MODEL))
                        &&Identifier.parse(isShard?"lbc:tier_q":"lbc:tier_b").equals(stack.get(DataComponents.TOOLTIP_STYLE))
                        &&stack.get(DataComponents.LORE)!=null&&stack.get(DataComponents.LORE).lines().size()==(isShard?4:9),"native "+name+" model/tier/authored lore");
            }
            service.pulseShard(player);require(frost.cold(player)==0,"empty inventory no pulse");
            player.getInventory().setItem(9,shard.copyWithCount(7));service.pulseShard(player);require(frost.cold(player)==1&&frost.cooldown(player)==2,"carried stack one pulse regardless of count");
            frost.clear(player);player.removeAllEffects();player.getInventory().clearContent();player.setItemInHand(InteractionHand.MAIN_HAND,shard.copy());
            service.pulseShard(player);require(frost.cold(player)==2,"selected mainhand overlaps container and weapon branches");
            frost.clear(player);player.removeAllEffects();player.getInventory().clearContent();player.setItemInHand(InteractionHand.OFF_HAND,shard.copy());
            service.pulseShard(player);require(frost.cold(player)==1,"offhand alone one pulse");
            frost.clear(player);player.removeAllEffects();player.getInventory().setItem(9,shard.copy());player.setItemInHand(InteractionHand.MAIN_HAND,shard.copy());
            service.pulseShard(player);require(frost.cold(player)==2,"both hands plus inventory still two source branches");
            frost.clear(player);player.removeAllEffects();player.getInventory().clearContent();player.getInventory().setItem(9,shard.copy());
            game.config().lbcSkills=true;targetProfile.lbcSkill=2;service.pulseShard(player);require(frost.cold(player)==0,"skill2 rejects carried-only donor");
            player.setItemInHand(InteractionHand.OFF_HAND,shard.copy());service.pulseShard(player);require(frost.cold(player)==2,"skill2 hand enables both original branches");
            game.config().lbcSkills=false;frost.clear(player);player.removeAllEffects();player.getInventory().clearContent();player.getInventory().setItem(9,shard.copy());
            for(int i=0;i<20;i++)service.pulseShard(player);require(frost.cold(player)==10,"plain shard cold stops at ten");
            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,100,1),player);service.pulseShard(player);require(frost.cold(player)==15,"Slowness II adds five above ten");
            for(int i=0;i<17;i++){player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,100,1),player);service.pulseShard(player);}
            require(frost.isFrozen(player)&&frost.frozenSeconds(player)==10&&frost.cold(player)==0,"real frost threshold freezes at one hundred");
            player.removeEffect(MobEffects.RESISTANCE);service.pulseShard(player);var resistance=player.getEffect(MobEffects.RESISTANCE);
            require(resistance!=null&&resistance.getAmplifier()==4&&resistance.getDuration()==500&&frost.frozenSeconds(player)==10
                    &&done(game,player,"accesories/jaakivi"),"still frozen grants source Resistance V 500 ticks and actual jaakivi advancement");
            frost.clear(player);player.removeAllEffects();player.getInventory().clearContent();
            mana.score(net.minecraft.world.scores.ScoreHolder.forNameOnly("neutral_ball_mana_activate"),"lbc.math").set(5000);
            player.setItemInHand(InteractionHand.MAIN_HAND,ball.copy());mana.set(player,5000);mana.baseRegen(player,10);service.tickPlayer(player);
            require(mana.get(player)==0&&mana.baseRegen(player)==11&&done(game,player,"arcanums/neutral"),"mainhand debits5000 adds one permanent regen and actual advancement");
            mana.set(player,4999);service.tickPlayer(player);require(mana.get(player)==4999&&mana.baseRegen(player)==11,"below admission leaves resources unchanged");
            player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);player.setItemInHand(InteractionHand.OFF_HAND,ball.copy());mana.set(player,0);mana.baseRegen(player,1);service.tickPlayer(player);
            require(mana.get(player)==5000&&mana.baseRegen(player)==0,"offhand converts one regen into5000 mana");service.tickPlayer(player);require(mana.get(player)==5000&&mana.baseRegen(player)==0,"offhand zero regen gate");
            player.setItemInHand(InteractionHand.MAIN_HAND,ball.copy());mana.baseRegen(player,10);service.tickPlayer(player);require(mana.get(player)==5000&&mana.baseRegen(player)==10,"dual hands execute main then off same frame");
            player.getInventory().setItem(9,shard.copy());
            game.items().timeStop().begin(player.level(),40);try{service.pulseShard(player);service.tickPlayer(player);require(frost.cold(player)==0&&mana.get(player)==5000&&mana.baseRegen(player)==10,"actual Time Stop guards both passives");}finally{game.items().timeStop().end(player.level());}
            stopper=new Marker(EntityTypes.MARKER,player.level());stopper.setPos(player.position());stopper.entityTags().add("stopper_magic");player.level().addFreshEntity(stopper);game.items().magic().loaded(stopper);
            service.pulseShard(player);service.tickPlayer(player);require(frost.cold(player)==0&&mana.get(player)==5000&&mana.baseRegen(player)==10,"real antimagic guards both passives");
            game.items().magic().unloaded(stopper);stopper.discard();stopper=null;
            game.spectators().enter(player);player.getInventory().setItem(9,shard.copy());player.setItemInHand(InteractionHand.MAIN_HAND,ball.copy());
            service.pulseShard(player);service.tickPlayer(player);require(game.isObserver(player)&&frost.cold(player)==0&&mana.get(player)==5000&&mana.baseRegen(player)==10,"actual observer gameplay role guards both passives");
        }finally{
            if(stopper!=null){game.items().magic().unloaded(stopper);stopper.discard();}
            game.items().timeStop().end(player.level());frost.clear(player);game.config().lbcSkills=skills;
            mana.score(net.minecraft.world.scores.ScoreHolder.forNameOnly("neutral_ball_mana_activate"),"lbc.math").set(admission);
            for(var entry:advancements.entrySet())for(var criterion:entry.getKey().value().criteria().keySet()){
                if(entry.getValue().contains(criterion))player.getAdvancements().award(entry.getKey(),criterion);else player.getAdvancements().revoke(entry.getKey(),criterion);
            }
            saved.restore(game);
            stats.forEach((stat,value)->player.getStats().setValue(player,stat,value));
            try{for(var entry:profile.entrySet())entry.getKey().set(targetProfile,copy(entry.getValue()));}catch(IllegalAccessException error){throw new IllegalStateException(error);}
            game.players().save();game.items().mana().inventoryChanged(player);game.items().magic().inventoryChanged(player);game.items().magic().flushCleansing();
        }
        SkyWars2.LOGGER.info("[check] PASS ice-materials");
    }
}
