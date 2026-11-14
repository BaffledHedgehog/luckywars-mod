package me.skywars2.game;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import me.skywars2.SkyWars2;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

/** Durable operator edits use the same canonical profiles and ownership as native menus/cases. */
public final class SkyWarsAdminCommands {
    private SkyWarsAdminCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var admin=Commands.literal("admin").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(ctx->{ctx.getSource().sendSuccess(()->Component.literal(
                        "/skywars admin coins|memcoins <player> [add|remove|set <amount>]; keys <player> <case 0..4> [add|remove|set <amount>]; unlock <player> give|take <id>"),false);return 1;});
        for(String currency:List.of("coins","memcoins")) {
            var target=playerArgument();operations(target,currency);
            admin.then(Commands.literal(currency).then(target));
        }
        var keyTarget=playerArgument().executes(ctx->{
            try{var game=game();var row=profile(game,StringArgumentType.getString(ctx,"player"));
                ctx.getSource().sendSuccess(()->Component.literal(row.username+" keys [0..4]: "+Arrays.toString(row.keys)),false);return 1;
            }catch(RuntimeException error){return fail(ctx,error);}
        });
        var kind=Commands.argument("case",IntegerArgumentType.integer(0,4));operations(kind,"keys");
        admin.then(Commands.literal("keys").then(keyTarget.then(kind)));
        var unlock=playerArgument();
        for(boolean give:new boolean[]{true,false})unlock.then(Commands.literal(give?"give":"take")
                .then(Commands.argument("id",StringArgumentType.greedyString())
                        .suggests((ctx,builder)->SharedSuggestionProvider.suggest(LbcSourceUnlocks.administrativeIds(),builder))
                        .executes(ctx->unlock(ctx,give))));
        admin.then(Commands.literal("unlock").then(unlock));
        SkyWarsCommandTree.add(dispatcher,admin);
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack,String> playerArgument(){
        return Commands.argument("player",StringArgumentType.word()).suggests((ctx,builder)->{
            var game=SkyWars2.game();return SharedSuggestionProvider.suggest(game==null?List.<String>of():game.players().knownProfiles().values().stream()
                    .map(p->p.username).filter(n->n.matches("[A-Za-z0-9_]{1,16}")).distinct().sorted().toList(),builder);
        });
    }
    private static void operations(ArgumentBuilder<CommandSourceStack,?> parent,String currency){
        parent.executes(ctx->balance(ctx,currency,null,0));
        for(String operation:List.of("add","remove","set"))parent.then(Commands.literal(operation)
                .then(Commands.argument("amount",IntegerArgumentType.integer(0))
                        .executes(ctx->balance(ctx,currency,operation,IntegerArgumentType.getInteger(ctx,"amount")))));
    }
    private static GameController game(){
        var game=SkyWars2.game();if(game==null)throw new IllegalStateException("SkyWars is starting");return game;
    }
    private static PlayerStore.Profile profile(GameController game,String name){
        return game.players().knownProfiles().get(LbcDropRequests.resolve(game.players().knownProfiles(),name));
    }
    private static int balance(CommandContext<CommandSourceStack> ctx,String currency,String operation,int amount){
        try{
            var game=game();var row=profile(game,StringArgumentType.getString(ctx,"player"));int kind=currency.equals("keys")?IntegerArgumentType.getInteger(ctx,"case"):-1;
            int previous=currency.equals("coins")?row.coins:currency.equals("memcoins")?row.memcoins:row.keys[kind];
            if(operation!=null){
                int value=switch(operation){case "add"->Math.addExact(previous,amount);case "remove"->Math.max(0,previous-amount);case "set"->amount;default->throw new IllegalArgumentException("Unknown operation");};
                var before=new MatchValueSnapshot(row);
                if(currency.equals("coins"))row.coins=value;else if(currency.equals("memcoins"))row.memcoins=value;else row.keys[kind]=value;
                try{game.players().saveAtomic();}catch(IOException|RuntimeException error){before.restore();throw error;}
            }
            int value=currency.equals("coins")?row.coins:currency.equals("memcoins")?row.memcoins:row.keys[kind];
            ctx.getSource().sendSuccess(()->Component.literal(row.username+" "+currency+(kind<0?"":"["+kind+"]")+": "+value),operation!=null);return 1;
        }catch(IOException|RuntimeException error){return fail(ctx,error);}
    }
    private static int unlock(CommandContext<CommandSourceStack> ctx,boolean give){
        try{
            var game=game();String name=StringArgumentType.getString(ctx,"player"),id=StringArgumentType.getString(ctx,"id");
            if(!LbcSourceUnlocks.administrativeIds().contains(id))throw new IllegalArgumentException("Not a manual perk/cosmetic unlock: "+id);
            if(id.contains(":")&&game.server().getAdvancements().get(net.minecraft.resources.Identifier.parse(id))==null)throw new IllegalArgumentException("Missing source flag: "+id);
            UUID uuid=LbcDropRequests.resolve(game.players().knownProfiles(),name);var row=game.players().knownProfiles().get(uuid);var before=new MatchValueSnapshot(row);
            if(give){row.revokedLegacyUnlocks.remove(id);row.legacyUnlocks.add(id);}
            else{row.legacyUnlocks.remove(id);row.revokedLegacyUnlocks.add(id);clearSelection(row,id);}
            try{game.players().saveAtomic();}catch(IOException|RuntimeException error){before.restore();throw error;}
            var online=game.server().getPlayerList().getPlayer(uuid);if(online!=null)LbcSourceUnlocks.publish(online,row);
            ctx.getSource().sendSuccess(()->Component.literal(row.username+" "+id+": "+(give?"granted":"revoked")),true);return 1;
        }catch(IOException|RuntimeException error){return fail(ctx,error);}
    }
    private static void clearSelection(PlayerStore.Profile row,String id){
        if(id.equals("swrg.kit"+row.kit))row.kit=0;
        if(id.equals("swrg.skill"+row.skill))row.skill=0;
        if(id.equals(LbcCosmetics.unlockKey(LbcCosmetics.Kind.SOUND,row.killSound)))row.killSound=0;
        if(id.equals(LbcCosmetics.unlockKey(LbcCosmetics.Kind.CAGE,row.cage)))row.cage=0;
        if(id.equals(LbcCosmetics.unlockKey(LbcCosmetics.Kind.SUFFIX,row.suffix)))row.suffix=0;
        if(id.equals(LbcCosmetics.unlockKey(LbcCosmetics.Kind.WIN,row.winEffect)))row.winEffect=0;
        if(id.equals(LbcCosmetics.unlockKey(LbcCosmetics.Kind.KILL,row.killEffect)))row.killEffect=0;
    }
    private static int fail(CommandContext<CommandSourceStack> ctx,Exception error){
        ctx.getSource().sendFailure(Component.literal("Could not update SkyWars profile: "+error.getMessage()));return 0;
    }
}
