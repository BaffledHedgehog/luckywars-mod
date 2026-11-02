package me.skywars2.game;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.List;
import java.util.Set;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.phys.Vec3;

/** Original named edible profiles; vanilla owns the carrier's nutrition, effects and item debit. */
public final class LbcNamedFoods {
    private record Effect(Holder<MobEffect> type,int seconds,int amplifier,boolean hidden){}
    private record Profile(String flag,List<Effect> effects){}
    private static Effect e(Holder<MobEffect> type,int seconds,int amplifier){return new Effect(type,seconds,amplifier,false);}
    private static Profile p(String flag,Effect... effects){return new Profile(flag,List.of(effects));}
    private static final List<Profile> PROFILES=List.of(
        p("golden_beef",e(MobEffects.STRENGTH,20,1),e(MobEffects.HEALTH_BOOST,20,4),e(MobEffects.INSTANT_HEALTH,1,127)),
        p("golden_beetroot",e(MobEffects.INSTANT_HEALTH,600,0)),
        p("golden_berries",e(MobEffects.LUCK,180,4),e(MobEffects.HASTE,180,10)),
        p("golden_bread",e(MobEffects.ABSORPTION,40,0),e(MobEffects.REGENERATION,20,0),e(MobEffects.HEALTH_BOOST,90,0)),
        p("golden_cake",e(MobEffects.SPEED,120,19),e(MobEffects.HASTE,120,19)),
        p("golden_chicken",e(MobEffects.SLOW_FALLING,270,0)),
        p("golden_chorus",e(MobEffects.SLOW_FALLING,100,0),e(MobEffects.REGENERATION,100,2),e(MobEffects.RESISTANCE,100,3)),
        p("golden_cookie",e(MobEffects.SPEED,120,2),e(MobEffects.SLOW_FALLING,30,0)),
        p("golden_fish",e(MobEffects.WATER_BREATHING,90,0),e(MobEffects.RAID_OMEN,90,0)),
        p("golden_kelp",e(MobEffects.WATER_BREATHING,15,2),e(MobEffects.RAID_OMEN,15,2),e(MobEffects.CONDUIT_POWER,15,2)),
        p("golden_melon",e(MobEffects.REGENERATION,10,10),e(MobEffects.LUCK,60,0)),
        p("golden_mutton",e(MobEffects.REGENERATION,40,0),e(MobEffects.STRENGTH,40,0),e(MobEffects.HEALTH_BOOST,40,0)),
        p("golden_pie",e(MobEffects.SPEED,10,255),e(MobEffects.RESISTANCE,10,4)),
        p("golden_porkchop",e(MobEffects.ABSORPTION,40,0),e(MobEffects.REGENERATION,20,0),e(MobEffects.HEALTH_BOOST,3,90)),
        p("golden_potato",e(MobEffects.HASTE,40,0),e(MobEffects.REGENERATION,40,0),e(MobEffects.FIRE_RESISTANCE,40,0)),
        p("golden_rabbit",e(MobEffects.JUMP_BOOST,40,10)),
        p("golden_stew",e(MobEffects.SPEED,50,127),e(MobEffects.JUMP_BOOST,50,127),e(MobEffects.SLOW_FALLING,50,127),e(MobEffects.INSTANT_HEALTH,1000,99)),
        p("diamond_apple",e(MobEffects.ABSORPTION,360,9),e(MobEffects.REGENERATION,360,2),e(MobEffects.RESISTANCE,360,2),e(MobEffects.HASTE,360,1),e(MobEffects.SPEED,360,0),e(MobEffects.FIRE_RESISTANCE,360,0)),
        p("diamond_beef",e(MobEffects.RESISTANCE,150,2),e(MobEffects.STRENGTH,150,2),e(MobEffects.HEALTH_BOOST,150,4),e(MobEffects.INSTANT_HEALTH,1,127)),
        p("diamond_berries",e(MobEffects.LUCK,270,40)),
        p("diamond_carrot",e(MobEffects.ABSORPTION,360,9),e(MobEffects.JUMP_BOOST,360,1),e(MobEffects.RESISTANCE,360,2),e(MobEffects.HASTE,360,1),e(MobEffects.SPEED,360,0),e(MobEffects.NIGHT_VISION,360,0)),
        p("diamond_chicken",e(MobEffects.SLOW_FALLING,1000,0),e(MobEffects.SPEED,1000,2),e(MobEffects.STRENGTH,1000,0)),
        p("diamond_fish",e(MobEffects.ABSORPTION,720,9),e(MobEffects.REGENERATION,720,2),e(MobEffects.WATER_BREATHING,720,0),e(MobEffects.RAID_OMEN,720,4),e(MobEffects.RAID_OMEN,720,16),e(MobEffects.CONDUIT_POWER,720,1)),
        p("diamond_mutton",e(MobEffects.REGENERATION,360,2),e(MobEffects.RESISTANCE,360,2),e(MobEffects.STRENGTH,360,1),e(MobEffects.HEALTH_BOOST,360,4)),
        p("diamond_porkchop",e(MobEffects.ABSORPTION,360,9),e(MobEffects.REGENERATION,360,2),e(MobEffects.HEALTH_BOOST,360,9)),
        p("diamond_potato",e(MobEffects.REGENERATION,360,2),e(MobEffects.HASTE,360,2),e(MobEffects.FIRE_RESISTANCE,360,0)),
        p("diamond_rabbit",e(MobEffects.JUMP_BOOST,360,127),e(MobEffects.RESISTANCE,360,4)),
        p("diamond_stew",e(MobEffects.ABSORPTION,180,9),e(MobEffects.REGENERATION,180,2),e(MobEffects.WATER_BREATHING,180,2),e(MobEffects.RAID_OMEN,180,42),e(MobEffects.CONDUIT_POWER,180,2)),
        p("netherite_apple",e(MobEffects.ABSORPTION,720,18),e(MobEffects.REGENERATION,720,4),e(MobEffects.RESISTANCE,720,3),e(MobEffects.HASTE,720,5),e(MobEffects.SPEED,720,3),e(MobEffects.SATURATION,10000,0),e(MobEffects.FIRE_RESISTANCE,720,0),e(MobEffects.STRENGTH,720,0)),
        p("netherite_carrot",e(MobEffects.REGENERATION,30,4),e(MobEffects.ABSORPTION,360,9),e(MobEffects.RESISTANCE,360,2),e(MobEffects.HASTE,360,1),e(MobEffects.JUMP_BOOST,-1,1),e(MobEffects.SPEED,-1,1),e(MobEffects.NIGHT_VISION,-1,0)),
        p("netherite_fish",e(MobEffects.ABSORPTION,1440,19),e(MobEffects.REGENERATION,1440,4),e(MobEffects.WATER_BREATHING,1440,2),e(MobEffects.RAID_OMEN,1440,4),e(MobEffects.RAID_OMEN,1440,16),e(MobEffects.CONDUIT_POWER,1440,2),e(MobEffects.HASTE,1440,1),e(MobEffects.NIGHT_VISION,1440,0),e(MobEffects.GLOWING,1440,0)),
        p("netherite_potato",e(MobEffects.REGENERATION,720,4),e(MobEffects.RESISTANCE,720,3),e(MobEffects.HASTE,720,5),e(MobEffects.FIRE_RESISTANCE,-1,0)),
        p("netherite_stew",e(MobEffects.ABSORPTION,-1,127),e(MobEffects.BLINDNESS,-1,0),e(MobEffects.CONDUIT_POWER,-1,2),e(MobEffects.DARKNESS,-1,0),e(MobEffects.RAID_OMEN,-1,5),e(MobEffects.FIRE_RESISTANCE,-1,2),e(MobEffects.GLOWING,-1,0),e(MobEffects.HASTE,-1,127),e(MobEffects.HEALTH_BOOST,-1,127),e(MobEffects.RAID_OMEN,-1,5),e(MobEffects.HUNGER,-1,255),e(MobEffects.INSTANT_DAMAGE,-1,99),e(MobEffects.INSTANT_HEALTH,-1,99),new Effect(MobEffects.INVISIBILITY,-1,127,true),e(MobEffects.JUMP_BOOST,-1,127),e(MobEffects.LEVITATION,-1,127),e(MobEffects.LUCK,-1,255),e(MobEffects.MINING_FATIGUE,-1,255),e(MobEffects.NAUSEA,-1,0),e(MobEffects.NIGHT_VISION,-1,0),e(MobEffects.POISON,-1,127),e(MobEffects.REGENERATION,-1,127),e(MobEffects.RESISTANCE,-1,4),e(MobEffects.SATURATION,-1,127),e(MobEffects.SLOW_FALLING,-1,0),e(MobEffects.SLOWNESS,-1,127),e(MobEffects.SPEED,-1,255),e(MobEffects.STRENGTH,-1,127),e(MobEffects.UNLUCK,-1,127),e(MobEffects.WATER_BREATHING,-1,127),e(MobEffects.WEAKNESS,-1,127),e(MobEffects.WITHER,-1,127)),
        p("bedrock_apple",e(MobEffects.ABSORPTION,1440,36),e(MobEffects.REGENERATION,1440,8),e(MobEffects.RESISTANCE,1440,4),e(MobEffects.HASTE,1440,10),e(MobEffects.SPEED,1440,4),e(MobEffects.SATURATION,28800,0),e(MobEffects.FIRE_RESISTANCE,1440,0),e(MobEffects.STRENGTH,1440,2),e(MobEffects.SLOW_FALLING,1440,0)),
        p("netherstar_fish",e(MobEffects.ABSORPTION,2880,39),e(MobEffects.REGENERATION,2880,8),e(MobEffects.WATER_BREATHING,2880,2),e(MobEffects.RAID_OMEN,2880,4),e(MobEffects.RAID_OMEN,2880,16),e(MobEffects.CONDUIT_POWER,2880,2),e(MobEffects.HASTE,2880,5),e(MobEffects.NIGHT_VISION,2880,0),e(MobEffects.GLOWING,2880,0),e(MobEffects.FIRE_RESISTANCE,-1,0)),
        p("transdimensional_fish",e(MobEffects.ABSORPTION,5760,79),e(MobEffects.HEALTH_BOOST,5760,49),e(MobEffects.REGENERATION,5760,16),e(MobEffects.WATER_BREATHING,-1,0),e(MobEffects.RAID_OMEN,5760,4),e(MobEffects.RAID_OMEN,5760,16),e(MobEffects.HASTE,5760,32),e(MobEffects.NIGHT_VISION,5760,0),e(MobEffects.GLOWING,5760,0),e(MobEffects.FIRE_RESISTANCE,-1,0)),
        p("multiverse_fish",e(MobEffects.ABSORPTION,11520,127),e(MobEffects.HEALTH_BOOST,11520,127),e(MobEffects.REGENERATION,11520,32),e(MobEffects.WATER_BREATHING,-1,0),e(MobEffects.RAID_OMEN,11520,4),e(MobEffects.RAID_OMEN,11520,16),e(MobEffects.HASTE,11520,64),e(MobEffects.NIGHT_VISION,11520,0),e(MobEffects.GLOWING,11520,0),e(MobEffects.SLOW_FALLING,11520,0),e(MobEffects.RESISTANCE,11520,4),e(MobEffects.STRENGTH,11520,124),e(MobEffects.SPEED,11520,3),e(MobEffects.JUMP_BOOST,11520,3),e(MobEffects.FIRE_RESISTANCE,-1,0)),
        p("human_eye",e(MobEffects.NAUSEA,1,1),e(MobEffects.HUNGER,1,1),e(MobEffects.BLINDNESS,1,1)),
        p("human_flesh",e(MobEffects.NAUSEA,1,1),e(MobEffects.HUNGER,1,1),e(MobEffects.BLINDNESS,1,1)),
        p("eat_banana")
    );
    private static final List<String> STEW_COUNTERS=List.of("acid_rifle","all_seeing_eye","blaze","delayed_death","dimensional_shifting","end_damage","gatling_fovos","lolidefecation","greed_prestige","mana","living_legend","lookback","manamax","manaregen","reverse_mana","nanobots_coldown","nanobots_tier","nitro","nomagic","piss","ray","reactivefuel","scared","screamray","shock","sleep_time","target","twitching","void_resistance");
    private final GameController game;
    public LbcNamedFoods(GameController game){this.game=game;}
    private boolean admitted(ServerPlayer player){return game.playing()&&game.stage()!=GameController.Stage.ENDING&&player.isAlive()&&!player.isRemoved()
            &&player.level()==game.maps().level()&&game.canUseGameplayItems(player)&&!game.isObserver(player)
            &&game.items().wands().validRoleTarget(player,false)&&LbcDimensionItems.ordinary(player)
            &&!game.items().timeStop().stopped(player.level())&&!game.items().wands().timeStopped()&&!game.items().wands().inputTimeStopped(player);}
    public void consumed(ServerPlayer player,ItemStack stack){
        if(stack.isEmpty()||!admitted(player))return;var data=LbcFartus.data(stack);
        for(var profile:PROFILES)if(data.getIntOr(profile.flag(),0)==1){
            // Golden stew's source counter/tag commands precede its effect commands.
            if(profile.flag().equals("golden_stew")){add(player,"piss",1000);player.entityTags().add("have_lucky_effects");}
            for(var effect:profile.effects()){
                // Source carrot clears occur between its finite and infinite effect grants.
                if(profile.flag().equals("netherite_carrot")&&effect.type().is(MobEffects.JUMP_BOOST))clearSight(player);
                effect(player,effect);
            }
            switch(profile.flag()){
                case "golden_chorus"->spread(player);
                case "diamond_carrot"->clearSight(player);
                case "diamond_chicken"->{for(int i=0;i<5;i++)drop(player,"lbc:spawnegg");for(float pitch:new float[]{2,1,0})LbcOverload.sound(player,"minecraft:entity.chicken.death",1,pitch,0);}
                case "golden_pie"->{add(player,"living_legend",200);game.items().adoptRawFoodStatuses(player);}
                case "golden_stew"->game.items().adoptRawFoodStatuses(player);
                case "human_eye"->{add(player,"all_seeing_eye",12000);game.items().adoptRawFoodStatuses(player);}
                case "human_flesh"->swap(player);
                case "eat_banana"->banana(player);
                case "netherite_fish"->add(player,"mana",20000);
                case "netherstar_fish"->{add(player,"mana",100000);add(player,"manaregen",10);add(player,"manamax",10000);}
                case "transdimensional_fish"->{fish(player,5760,1000000,100,100000);}
                case "multiverse_fish"->{fish(player,11520,10000000,1000,1000000);}
                case "netherite_stew"->{
                    for(var counter:STEW_COUNTERS){
                        if(LbcParasite.active(player)&&(LbcLegacyEffects.RANDOM_COUNTERS.contains(counter)
                                ||List.of("reverse_mana","end_damage","dimensional_shifting","effect_shock","effect_vacuum","effect_timestop","effect_chain_lightning").contains(counter)))continue;
                        game.items().mana().score(player,counter).set(counter.equals("end_damage")?21474:Integer.MAX_VALUE);
                    }
                    player.entityTags().addAll(List.of("have_lucky_effects","have_lucky_effects_5t","have_lucky_effects_1s"));
                    game.items().adoptRawFoodStatuses(player);
                }
                default->{}
            }
        }
    }
    private void effect(ServerPlayer player,Effect effect){
        int ticks=effect.seconds()<0?-1:effect.type().value().isInstantaneous()?effect.seconds():effect.seconds()*20;
        player.addEffect(ItemEffectLevels.create(effect.type(),ticks,effect.amplifier(),false,!effect.hidden()),player);
    }
    private static void clearSight(ServerPlayer player){player.removeEffect(MobEffects.BLINDNESS);player.removeEffect(MobEffects.DARKNESS);}
    private void add(ServerPlayer player,String counter,int amount){if(LbcParasite.active(player)&&(counter.equals("dimensional_shifting")||counter.equals("end_damage")))return;game.items().mana().score(player,counter).add(amount);}
    private void fish(ServerPlayer player,int seconds,int mana,int regeneration,int maximum){
        add(player,"void_resistance",seconds);add(player,"dimensional_shifting",seconds);add(player,"mana",mana);add(player,"manaregen",regeneration);add(player,"manamax",maximum);
        game.items().adoptRawFoodStatuses(player);
    }
    private void drop(ServerPlayer player,String table){
        for(var stack:game.lbc().rollTable(player.level(),player.position(),player,table)){
            var item=new ItemEntity(player.level(),player.getX(),player.getY(),player.getZ(),stack.copy());item.setDefaultPickUpDelay();item.setThrower(player);game.items().markCurrentItemEpoch(item);player.level().addFreshEntity(item);
        }
    }
    private void banana(ServerPlayer player){
        int maximum=(int)player.getAttributeValue(Attributes.MAX_HEALTH),health=Mth.floor(player.getHealth());
        game.items().mana().score(ScoreHolder.forNameOnly("tmp"),"lbc.math").set(maximum);game.items().mana().score(ScoreHolder.forNameOnly("tmp1"),"lbc.math").set(health);
        if(player.level().getRandom().nextFloat()<.5f)drop(player,"lbc:govno_eaten");
        if(player.level().getRandom().nextFloat()<.15f)drop(player,"lbc:wtf_item");
        effect(player,maximum>health?e(MobEffects.INSTANT_HEALTH,1,1):e(MobEffects.STRENGTH,5,1));
    }
    private void spread(ServerPlayer player){
        boolean borders=game.config().noBorders;var at=LbcSurfaceTeleport.destination(player.level(),borders?player.position():Vec3.ZERO,borders?200:190,player.level().getMaxY()+1,player.getRandom());
        if(at!=null&&player.teleportTo(player.level(),at.x,at.y,at.z,Set.of(),player.getYRot(),player.getXRot(),true))game.items().indirectKills().teleported(player,player,true);
    }
    private void swap(ServerPlayer player){
        try{
            // @a has no distance/world restriction; native sort=random chooses across all dimensions.
            String selector="@a[gamemode=!spectator,name=!"+StringArgumentType.escapeIfRequired(player.getGameProfile().name())+",sort=random,limit=1]";
            var choices=new EntitySelectorParser(new StringReader(selector),true).parse().findPlayers(game.server().createCommandSourceStack().withEntity(player).withLevel(player.level()).withPosition(player.position()).withPermission(PermissionSet.ALL_PERMISSIONS).withSuppressedOutput());
            if(choices.isEmpty())return;var other=choices.getFirst();
            if(other==player||!game.items().wands().validTarget(other)||game.isObserver(other)||game.items().timeStop().stopped(other.level())||game.items().wands().inputTimeStopped(other))return;
            var from=player.position();var fromLevel=player.level();var to=other.position();var toLevel=other.level();float yaw=other.getYRot(),pitch=other.getXRot();
            // Original first entity-target tp copies the selected player's look; the new marker's look is0.
            if(!player.teleportTo(toLevel,to.x,to.y,to.z,Set.of(),yaw,pitch,true))return;
            game.items().indirectKills().teleported(player,player,true);
            if(other.teleportTo(fromLevel,from.x,from.y,from.z,Set.of(),0,0,true))game.items().indirectKills().teleported(other,player,true);
        }catch(CommandSyntaxException failure){throw new IllegalStateException("Native human-flesh selector failed",failure);}
    }
}
