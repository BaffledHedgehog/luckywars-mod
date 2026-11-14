package me.skywars2.game;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Public parent for custom operator commands and native status consumers. */
public final class SkyWarsFeedbackCommands {
    private record Status(int ticksPerUnit,boolean harmful){}
    private static final Map<String,Status> STATUSES=statuses();
    private static Map<String,Status> statuses(){
        var values=new TreeMap<String,Status>();
        add(values,1,false,"effect_boost_mana","effect_add_manamax","effect_add_manaregen","effect_add_mana","all_seeing_eye","living_legend","ray","screamray");
        add(values,1,true,"effect_chain_lightning","effect_shock_potion","effect_dimshifting","effect_rtp_4","effect_kiuaskivi");
        add(values,1,true,"personal_stasis","polymorph");
        add(values,1,true,"effect_remove_mana","effect_big_explosion","effect_explosion","effect_nuking","effect_traps","effect_snus","effect_stand_arrow_hit","effect_witch_gens_reset","effect_hedgehog_boom","effect_vacuum","effect_timestop","effect_small_explosion","effect_lightning_bolt","effect_firebreak","effect_evoker","effect_shock","effect_lightarrow","effect_evokerarrow","effect_rtp_1","effect_rtp_2","effect_rtp_3","piss","scared","gigaponos","skill_issue","stun");
        add(values,20,true,"nomagic","reverse_mana","delayed_death","target","cold","frozen","effect_gravity_curse","effect_ghosting");
        add(values,20,false,"void_resistance","dimensional_shifting");
        add(values,5,false,"acid_rifle");add(values,10,false,"blaze");add(values,2,false,"gatling_fovos");
        add(values,5,true,"lookback");add(values,2,true,"twitching");add(values,40,true,"shock");
        return Collections.unmodifiableMap(values);
    }
    private static void add(Map<String,Status> map,int cadence,boolean harmful,String... ids){for(String id:ids)map.put(id,new Status(cadence,harmful));}
    public static Set<String> effectIds(){return STATUSES.keySet();}
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher){
        LbcDropRequests.register(dispatcher);
        SkyWarsAdminCommands.register(dispatcher);
        var glitch=Commands.literal("atlasglitch").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(ctx->atlasGlitch(ctx.getSource(),List.of(ctx.getSource().getPlayerOrException()),"toggle"));
        for(String mode:List.of("on","off","toggle"))glitch.then(Commands.literal(mode)
                .executes(ctx->atlasGlitch(ctx.getSource(),List.of(ctx.getSource().getPlayerOrException()),mode))
                .then(Commands.argument("players",EntityArgument.players()).executes(ctx->
                        atlasGlitch(ctx.getSource(),EntityArgument.getPlayers(ctx,"players"),mode))));
        dispatcher.register(Commands.literal("skywars").then(glitch));
        dispatcher.register(Commands.literal("skywars")
            .then(Commands.literal("reloadconfig").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(ctx->{
                var game=SkyWars2.game();if(game==null)return 0;
                try{int count=game.items().mana().sourceConfig().reload();game.items().nexusCompass().reload();game.items().nexusCombat().reload();
                    for(var player:game.server().getPlayerList().getPlayers()){player.getInventory().setChanged();player.inventoryMenu.sendAllDataToRemote();if(player.containerMenu!=player.inventoryMenu)player.containerMenu.sendAllDataToRemote();}
                    ctx.getSource().sendSuccess(()->Component.literal("Reloaded "+count+" source values from "+LbcSourceConfig.FILE),true);return count;
                }catch(RuntimeException e){ctx.getSource().sendFailure(Component.literal(e.getMessage()));return 0;}
            }))
            .then(Commands.literal("effect").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.argument("id",StringArgumentType.word()).suggests((ctx,builder)->SharedSuggestionProvider.suggest(effectIds(),builder))
                    .then(Commands.argument("time",StringArgumentType.word()).suggests((ctx,builder)->SharedSuggestionProvider.suggest(List.of("10s","200t"),builder))
                        .executes(ctx->{
                            var game=SkyWars2.game();if(game==null)return 0;
                            var player=ctx.getSource().getPlayerOrException();
                            String id=StringArgumentType.getString(ctx,"id"),time=StringArgumentType.getString(ctx,"time");
                            try{
                                int ticks=durationTicks(time);apply(game,player,id,ticks);
                                ctx.getSource().sendSuccess(()->Component.literal(id+": "+ticks+" ticks"),false);return 1;
                            }catch(IllegalArgumentException error){throw new SimpleCommandExceptionType(Component.literal(error.getMessage())).create();}
                        })))));
    }
    private static int atlasGlitch(CommandSourceStack source,Collection<ServerPlayer> players,String mode){
        for(var player:players){
            boolean enabled=mode.equals("on")||mode.equals("toggle")&&!AtlasGlitchSignal.manualEnabled(player);
            AtlasGlitchSignal.manual(player,enabled);
            source.sendSuccess(()->Component.literal(player.getGameProfile().name()+": atlas_glitch manual "+(enabled?"on":"off")
                    +(player.getPostEffects().contains(AtlasGlitchSignal.EFFECT)&&!enabled?" (Supernova active)":"")),false);
        }
        return players.size();
    }
    static int durationTicks(String argument){
        if(!argument.matches("[0-9]+[st]?"))throw new IllegalArgumentException("Time: seconds (10 or 10s), or ticks (200t)");
        boolean ticks=argument.endsWith("t"),suffix=ticks||argument.endsWith("s");
        try{long amount=Long.parseLong(suffix?argument.substring(0,argument.length()-1):argument);long value=Math.multiplyExact(amount,ticks?1:20);
            if(value<=0||value>Integer.MAX_VALUE)throw new ArithmeticException();return (int)value;
        }catch(ArithmeticException|NumberFormatException error){throw new IllegalArgumentException("Time must be positive and fit in 2147483647 ticks");}
    }
    static void apply(GameController game,ServerPlayer player,String id,int ticks){
        var status=STATUSES.get(id);if(status==null)throw new IllegalArgumentException("Unknown custom effect: "+id);
        if(!game.canUseGameplayItems(player)||!game.playing()||game.isObserver(player))throw new IllegalArgumentException("Custom effects require an active gameplay player");
        var items=game.items();
        if(status.harmful()?items.magic().rejectCustomDebuff(player):items.magic().rejectCustomEffect(player))throw new IllegalArgumentException("This effect is currently blocked for the player");
        if(id.equals("stun")){if(!items.shooters().selfStun(player,ticks))throw new IllegalArgumentException("Stun was rejected");return;}
        if(id.equals("personal_stasis")){items.legacyEffects().personalStasis().start(player,ticks);return;}
        if(id.equals("polymorph")){if(!items.polymorph().applyTimed(player,items.indirectKills().wildCause(player),ticks))throw new IllegalArgumentException("Polymorph was rejected");return;}
        int units=(int)(((long)ticks+status.ticksPerUnit()-1)/status.ticksPerUnit());
        if(id.equals("effect_ghosting")){if(!items.soulless().setGhosting(player,units))throw new IllegalArgumentException("Ghosting was rejected");return;}
        if(id.equals("effect_gravity_curse")){if(!items.pendingCombat().applyGravityCurse(player,player,units))throw new IllegalArgumentException("Gravity curse was rejected");return;}
        if(id.equals("nomagic"))items.mana().applyNoMagic(player,units);
        else if(id.equals("reverse_mana"))items.mana().applyReverse(player,player,units);
        else items.mana().score(player,id).set(units);
        player.entityTags().addAll(List.of("have_lucky_effects","have_lucky_effects_1s","have_lucky_effects_5t"));
        items.adoptRawFoodStatuses(player);items.magic().colors().loaded(player);
    }
}
