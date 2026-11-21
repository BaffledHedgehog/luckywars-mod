package me.skywars2.game.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.*;
import me.skywars2.SkyWars2;
import me.skywars2.game.GameController;
import me.skywars2.game.core.config.LbcSourceConfig;
import me.skywars2.game.effects.LbcParasite;
import me.skywars2.game.items.loot.LbcDropRequests;
import me.skywars2.game.presentation.effects.AtlasGlitchSignal;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import me.skywars2.game.LbcItems;

/** Public parent for custom operator commands and native status consumers. */
public final class SkyWarsFeedbackCommands {
    public record Status(int ticksPerUnit, boolean harmful, boolean arenaOnly) {}
    private static final Map<String,Status> STATUSES=statuses();
    private static Map<String,Status> statuses(){
        var values=new TreeMap<String,Status>();
        add(values,1,false,"effect_boost_mana","all_seeing_eye","living_legend","ray","screamray");
        add(values,1,true,"effect_shock_potion","effect_dimshifting","effect_rtp_4","polymorph");
        add(values,1,true,"effect_big_explosion","effect_explosion","effect_nuking","effect_traps","effect_snus","effect_stand_arrow_hit","effect_witch_gens_reset","effect_hedgehog_boom","effect_vacuum","piss","scared","gigaponos","skill_issue");
        add(values,20,true,"nomagic","reverse_mana","delayed_death","target");
        add(values,20,false,"void_resistance","dimensional_shifting");
        add(values,5,false,"acid_rifle");add(values,10,false,"blaze");
        add(values,5,true,"lookback");add(values,2,true,"twitching");add(values,40,true,"shock");
        // These native owners cannot start or advance outside the current arena.
        arena(values,1,false,"effect_add_manamax","effect_add_manaregen","effect_add_mana");
        arena(values,1,true,"effect_chain_lightning","effect_kiuaskivi","personal_stasis","effect_remove_mana","effect_timestop",
                "effect_small_explosion","effect_lightning_bolt","effect_firebreak","effect_evoker","effect_shock","effect_lightarrow",
                "effect_evokerarrow","effect_rtp_1","effect_rtp_2","effect_rtp_3","stun");
        arena(values,20,true,"cold","frozen","effect_gravity_curse","effect_ghosting");
        arena(values,2,false,"gatling_fovos");
        return Collections.unmodifiableMap(values);
    }
    private static void add(Map<String,Status> map,int cadence,boolean harmful,String... ids){
        for(String id:ids)map.put(id,new Status(cadence,harmful,false));
    }
    private static void arena(Map<String,Status> map,int cadence,boolean harmful,String... ids){
        for(String id:ids)map.put(id,new Status(cadence,harmful,true));
    }
    public static Set<String> effectIds(){return STATUSES.keySet();}
    public static Map<String,Status> effects(){return STATUSES;}
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher){
        LbcDropRequests.register(dispatcher);
        SkyWarsAdminCommands.register(dispatcher);
        var genie = Commands.literal("genie").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
            .then(Commands.literal("login").executes(ctx -> {
                var link = Component.literal("chatgpt.com").withStyle(style -> style
                    .withColor(net.minecraft.ChatFormatting.AQUA).withUnderlined(true)
                    .withClickEvent(new net.minecraft.network.chat.ClickEvent.OpenUrl(java.net.URI.create("https://chatgpt.com/"))));
                ctx.getSource().sendSuccess(() -> Component.translatableWithFallback("skywars2.genie.login.firefox",
                        "Открой %s в Firefox, спаренном с сервером. Для ключа OpenAI API используй /skywars genie api.", link), false);
                return 1;
            }));
        SkyWarsCommandTree.add(dispatcher, genie);
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
                    ctx.getSource().sendSuccess(()->Component.literal("Reloaded "+count+" source values from "+LbcSourceConfig.RESOURCE),true);return count;
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
    public static int durationTicks(String argument){
        if(!argument.matches("[0-9]+[st]?"))throw new IllegalArgumentException("Time: seconds (10 or 10s), or ticks (200t)");
        boolean ticks=argument.endsWith("t"),suffix=ticks||argument.endsWith("s");
        try{long amount=Long.parseLong(suffix?argument.substring(0,argument.length()-1):argument);long value=Math.multiplyExact(amount,ticks?1:20);
            if(value<=0||value>Integer.MAX_VALUE)throw new ArithmeticException();return (int)value;
        }catch(ArithmeticException|NumberFormatException error){throw new IllegalArgumentException("Time must be positive and fit in 2147483647 ticks");}
    }
    public static void apply(GameController game, ServerPlayer player, String id, int ticks) {
        var status = STATUSES.get(id);
        if (status == null) throw new IllegalArgumentException("Unknown custom effect: " + id);
        if (!game.canUseGameplayItems(player) || !game.playing() || game.isObserver(player))
            throw new IllegalArgumentException("Custom effects require an active gameplay player");
        String rejected = grant(game, player, player, id, status, ticks);
        if (rejected != null) throw new IllegalArgumentException(rejected);
    }

    /** Item authority can only grant native player statuses within its current active world. */
    public static boolean applyFromItem(GameController game, ServerPlayer author, ServerPlayer target, String id, int ticks) {
        var status = STATUSES.get(id);
        var items = game.items();
        if (status == null || ticks <= 0 || !game.playing()
                || author.level() != target.level() || status.arenaOnly() && target.level() != game.maps().level()
                || !items.mana().visible(author) || !items.mana().visible(target)
                || !items.wands().validRoleTarget(author, false) || !items.wands().validRoleTarget(target, false)
                || items.polymorph().masked(author) || !id.equals("polymorph") && items.polymorph().masked(target)
                || items.wands().inputTimeStopped(author) || items.wands().inputTimeStopped(target)
                || items.timeStop().stopped(author.level())) return false;
        // The native score consumers for these effects have an additional admission rule.
        if (id.equals("shock") && items.magic().rejectEffect(target, MobEffects.WITHER)
                || id.equals("gigaponos") && items.magic().rejectEffect(target, MobEffects.HUNGER)
                || id.equals("dimensional_shifting") && LbcParasite.active(target)) return false;
        return grant(game, author, target, id, status, ticks) == null;
    }

    private static String grant(GameController game, ServerPlayer author, ServerPlayer target,
                                String id, Status status, int ticks) {
        var items = game.items();
        if (status.harmful() ? items.magic().rejectCustomDebuff(target) : items.magic().rejectCustomEffect(target))
            return "This effect is currently blocked for the player";
        int units = (int) (((long) ticks + status.ticksPerUnit() - 1) / status.ticksPerUnit());
        if (id.equals("shock") && author != target) {
            items.electro().setScriptShock(target, author, units);
            return null;
        }
        switch (id) {
            case "stun" -> {
                boolean applied = author == target ? items.shooters().selfStun(target, ticks)
                        : items.shooters().stun(author, target, ticks);
                if (!applied) return "Stun was rejected";
            }
            case "personal_stasis" -> items.legacyEffects().personalStasis().start(target, ticks);
            case "polymorph" -> {
                if (!items.polymorph().applyTimed(target, items.indirectKills().wildCause(author), ticks))
                    return "Polymorph was rejected";
            }
            case "effect_ghosting" -> {
                if (!items.soulless().setGhosting(target, units)) return "Ghosting was rejected";
            }
            case "effect_gravity_curse" -> {
                if (!items.pendingCombat().applyGravityCurse(author, target, units)) return "Gravity curse was rejected";
            }
            default -> {
                if (id.equals("nomagic")) items.mana().applyNoMagic(target, units);
                else if (id.equals("reverse_mana")) items.mana().applyReverse(author, target, units);
                else items.mana().score(target, id).set(units);
                adoptCounters(game, target);
            }
        }
        return null;
    }

    private static void adoptCounters(GameController game, ServerPlayer target) {
        target.entityTags().addAll(List.of("have_lucky_effects", "have_lucky_effects_1s", "have_lucky_effects_5t"));
        game.items().adoptRawFoodStatuses(target);
        game.items().magic().colors().loaded(target);
    }
}
