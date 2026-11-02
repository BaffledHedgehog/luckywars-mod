package me.skywars2.game;

import eu.pb4.polymer.common.api.PolymerCommonUtils;
import eu.pb4.polymer.core.api.item.PolymerItemUtils;
import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.*;
import net.minecraft.network.chat.contents.ScoreContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.scores.ScoreHolder;

/** Score components have no client scoreboard context. Resolve only the recipient's projection. */
public final class LbcTooltipScores {
    private static boolean registered;
    private LbcTooltipScores(){}
    public static void register(){
        if(registered)return;registered=true;
        PolymerItemUtils.CONTEXT_ITEM_CHECK.register((stack,context)->dynamic(stack));
        PolymerItemUtils.ITEM_MODIFICATION_EVENT.register((original,client,context)->{
            var player=PolymerCommonUtils.getPlayer(context);var game=SkyWars2.game();
            if(player==null||game==null||client.isEmpty()||game.lbc().hidesKnowledge(player)||!dynamic(client))return client;
            var result=client.copy();
            var aliases=priceAliases(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(original.getItem()).toString());
            for(var type:java.util.List.of(DataComponents.CUSTOM_NAME,DataComponents.ITEM_NAME)){
                var name=result.get(type);if(name!=null&&dynamic(name))result.set(type,resolve(game,player,name,aliases));
            }
            var lore=result.get(DataComponents.LORE);
            if(lore!=null)result.set(DataComponents.LORE,new ItemLore(lore.lines().stream().map(line->resolve(game,player,line,aliases)).toList()));
            return result;
        });
    }
    static boolean dynamic(ItemInstance stack){
        var name=stack.get(DataComponents.CUSTOM_NAME);if(name!=null&&dynamic(name))return true;
        name=stack.get(DataComponents.ITEM_NAME);if(name!=null&&dynamic(name))return true;
        var lore=stack.get(DataComponents.LORE);return lore!=null&&lore.lines().stream().anyMatch(LbcTooltipScores::dynamic);
    }
    private static boolean dynamic(Component text){
        if(text.getContents() instanceof ScoreContents)return true;
        if(text.getContents() instanceof TranslatableContents translated)for(Object argument:translated.getArgs())if(argument instanceof Component part&&dynamic(part))return true;
        return text.getSiblings().stream().anyMatch(LbcTooltipScores::dynamic);
    }
    static Component resolve(GameController game,ServerPlayer recipient,Component text){
        return resolve(game,recipient,text,java.util.Map.of());
    }
    static java.util.Map<String,String> priceAliases(String itemId){
        return switch(itemId){
            case "skywars2:academy_wall_wand"->java.util.Map.of("wall_wand_mana_use","academy_wall_wand_mana_use");
            case "skywars2:academy_hedgehog_wand"->java.util.Map.of("baffled_wand_mana_use","academy_baffled_wand_mana_use");
            default->java.util.Map.of();
        };
    }
    static Component resolve(GameController game,ServerPlayer recipient,Component text,java.util.Map<String,String> aliases){
        MutableComponent result;
        if(text.getContents() instanceof ScoreContents score){
            String name=score.name().map(selector->selector.source(),literal->literal);
            name=aliases.getOrDefault(name,name);
            ScoreHolder holder=name.equals("@s")||name.equals("*")?recipient:ScoreHolder.forNameOnly(name);
            result=Component.literal(Integer.toString(game.items().mana().hudValue(holder,score.objective())));
        }else if(text.getContents() instanceof TranslatableContents translated){
            Object[] arguments=translated.getArgs().clone();
            for(int i=0;i<arguments.length;i++)if(arguments[i] instanceof Component part)arguments[i]=resolve(game,recipient,part,aliases);
            result=Component.translatableWithFallback(translated.getKey(),translated.getFallback(),arguments);
        }else result=MutableComponent.create(text.getContents());
        result.setStyle(text.getStyle());
        for(var sibling:text.getSiblings())result.append(resolve(game,recipient,sibling,aliases));
        return result;
    }
}
