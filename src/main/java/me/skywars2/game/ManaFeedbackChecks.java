package me.skywars2.game;

import com.mojang.datafixers.util.Either;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.ScoreContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Scoped fixture checks; caller owns player/score snapshots. No configuration file is rewritten. */
public final class ManaFeedbackChecks {
    private ManaFeedbackChecks(){}
    private static void require(boolean condition,String message){if(!condition)throw new IllegalStateException(message);}
    public static void run(GameController game,ServerPlayer player){
        require(SkyWarsFeedbackCommands.durationTicks("10")==200,"bare time uses seconds");
        require(SkyWarsFeedbackCommands.durationTicks("10s")==200,"explicit seconds");
        require(SkyWarsFeedbackCommands.durationTicks("7t")==7,"explicit ticks");
        for(String invalid:java.util.List.of("0","-1","1.2s","2147483647s","/function")){
            boolean rejected=false;try{SkyWarsFeedbackCommands.durationTicks(invalid);}catch(IllegalArgumentException expected){rejected=true;}
            require(rejected,"reject invalid duration "+invalid);
        }
        var mana=game.items().mana();int old=mana.get(player),maximum=mana.maximum(player),regen=mana.baseRegen(player);
        try{
            mana.set(player,-123);mana.maximum(player,0);mana.baseRegen(player,-4);mana.initializeResources(player);
            require(mana.get(player)==-123&&mana.maximum(player)==0&&mana.baseRegen(player)==-4,"existing zero/negative resources survive lifecycle initialization");
            var score=MutableComponent.create(new ScoreContents(Either.right("@s"),"mana")).withStyle(style->style.withColor(0x123456));
            var original=Component.translatableWithFallback("check_mana","Value: %s",score).append(Component.literal("!"));
            var saved=original.copy();var rendered=LbcTooltipScores.resolve(game,player,original);
            require(original.equals(saved),"projection preserves authoritative component tree");
            require(rendered.getString().equals("Value: -123!"),"nested recipient score resolves to signed literal");
            for(var pair:java.util.List.of(new String[]{"skywars2:academy_wall_wand","wall_wand_mana_use","academy_wall_wand_mana_use"},new String[]{"skywars2:academy_hedgehog_wand","baffled_wand_mana_use","academy_baffled_wand_mana_use"})){
                var price=MutableComponent.create(new ScoreContents(Either.right(pair[1]),"lbc.math"));
                var displayed=LbcTooltipScores.resolve(game,player,price,LbcTooltipScores.priceAliases(pair[0]));
                require(displayed.getString().equals(Integer.toString(mana.hudValue(net.minecraft.world.scores.ScoreHolder.forNameOnly(pair[2]),"lbc.math"))),"Academy presentation reads actual dedicated price "+pair[0]);
            }
            var base=new ItemStack(LbcWands.EVOKER);var academy=new ItemStack(LbcWands.ACADEMY_EVOKER);
            require(base.get(DataComponents.CUSTOM_NAME)!=null&&base.get(DataComponents.LORE)!=null&&!base.get(DataComponents.LORE).lines().isEmpty(),"native gallery fallback has source name/lore");
            require(Identifier.parse("lbc:tier_b").equals(academy.get(DataComponents.TOOLTIP_STYLE)),"Academy tier survives native gallery fallback");
            require(academy.get(DataComponents.LORE).lines().stream().anyMatch(c->c.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t&&t.getKey().equals("upgraded_item")),"Academy default includes authored upgrade description");
            var custom=academy.copy();custom.set(DataComponents.TOOLTIP_STYLE,Identifier.parse("lbc:tier_q"));custom.set(DataComponents.CUSTOM_NAME,Component.literal("Custom"));
            require(custom.get(DataComponents.CUSTOM_NAME).getString().equals("Custom")&&Identifier.parse("lbc:tier_q").equals(custom.get(DataComponents.TOOLTIP_STYLE)),"custom stack patches retain authority");
            require(Identifier.parse("lbc:tier_d").equals(new ItemStack(LbcWtf.SWORD).get(DataComponents.TOOLTIP_STYLE)),"Baguette tier D");
        }finally{mana.set(player,old);mana.maximum(player,maximum);mana.baseRegen(player,regen);}
    }
}
