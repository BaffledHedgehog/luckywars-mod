package me.skywars2.game;

import me.skywars2.SkyWars2;
import java.nio.file.Files;
import net.minecraft.resources.Identifier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import eu.pb4.polymer.core.api.entity.PolymerEntity;

/** Scoped checks on actual running-match fixtures. */
public final class FeedbackBatchChecks {
    private FeedbackBatchChecks(){}
    private static void require(boolean value,String message){if(!value)throw new IllegalStateException(message);}
    public static void mana(GameController game){
        try(var fixture=new ElementalGrimoireChecks.Fixture(game)){
            ManaFeedbackChecks.run(game,fixture.owner);
            var path=LbcSourceConfig.FILE;byte[] original;
            try{original=Files.readAllBytes(path);}catch(java.io.IOException e){throw new IllegalStateException(e);}
            try{
                var document=com.google.gson.JsonParser.parseString(new String(original,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                document.getAsJsonObject("values").addProperty("wall_wand_mana_use",137);
                Files.writeString(path,document.toString());
                var dispatcher=game.server().getCommands().getDispatcher();var console=game.server().createCommandSourceStack();
                require(dispatcher.execute("skywars reloadconfig",console)>0,"actual reload command publishes source values");
                var holder=net.minecraft.world.scores.ScoreHolder.forNameOnly("wall_wand_mana_use");
                require(game.items().mana().hudValue(holder,"lbc.math")==137,"actual mechanic operand receives configured value");
                document.getAsJsonObject("values").addProperty("wall_wand_mana_use",1.5);Files.writeString(path,document.toString());
                require(dispatcher.execute("skywars reloadconfig",console)==0&&game.items().mana().hudValue(holder,"lbc.math")==137,"invalid reload preserves live operands atomically");
            }catch(Exception e){throw new IllegalStateException(e);}
            finally{try{Files.write(path,original);game.items().mana().sourceConfig().reload();}catch(java.io.IOException e){throw new IllegalStateException(e);}}
        }
        SkyWars2.LOGGER.info("[check] PASS feedback-mana");
    }
    public static void visuals(GameController game){
        try(var fixture=new ElementalGrimoireChecks.Fixture(game)){
            fixture.initialize();var player=fixture.owner;
            for(var item:java.util.List.of(LbcItems.TERMIT,LbcItems.SULFUR,LbcItems.PHOSPHIN,LbcItems.GREMUCH)){
                var stack=new ItemStack(item);var hidden=stack.get(DataComponents.TOOLTIP_DISPLAY);
                require(hidden!=null&&hidden.hiddenComponents().contains(DataComponents.POTION_CONTENTS),"chemical effects hidden in native item tooltip");
                var cloud=EntityTypes.AREA_EFFECT_CLOUD.create(player.level(),EntitySpawnReason.COMMAND);
                require(cloud!=null,"native chemical cloud");
                cloud.setPotionContents(stack.get(DataComponents.POTION_CONTENTS));
                require(((PolymerEntity)cloud).getPolymerEntityType(null)==EntityTypes.MARKER,"chemical client cloud is particle-free marker");
                cloud.setPotionContents(net.minecraft.world.item.alchemy.PotionContents.EMPTY);
                require(((PolymerEntity)cloud).getPolymerEntityType(null)==EntityTypes.AREA_EFFECT_CLOUD,"ordinary cloud retains native client particles");
            }
            var neighbor=Identifier.parse("lbc:fpv_drone");var effect=Identifier.parse("lbc:skill_issue");boolean had=player.getPostEffects().contains(neighbor);
            player.addPostEffect(neighbor);
            try{
                var dispatcher=game.server().getCommands().getDispatcher();
                require(dispatcher.execute("skywars effect skill_issue 17t",player.createCommandSourceStack())==1,"operator effect command executes actual status producer");
                require(game.items().curseTicks(player)==17,"command uses exact native timed disorder");
                SignedEffectVisuals.synchronize(player);require(player.getPostEffects().contains(effect)&&player.getPostEffects().contains(neighbor),"skill issue adds own post effect without removing drone effect");
                game.items().clearTurnCurse(player);SignedEffectVisuals.synchronize(player);
                require(!player.getPostEffects().contains(effect)&&player.getPostEffects().contains(neighbor),"clearing actual disorder removes only own post effect");
            }catch(com.mojang.brigadier.exceptions.CommandSyntaxException e){throw new IllegalStateException(e);}
            finally{game.items().clearTurnCurse(player);SignedEffectVisuals.synchronize(player);if(!had)player.removePostEffect(neighbor);}
        }
        SkyWars2.LOGGER.info("[check] PASS feedback-visuals");
    }
    public static void firstJoin(GameController game){
        require(game.server().getPlayerList().getPlayer(GameChecks.offlineUuid("TestFeedbackNew"))==null,"fresh join fixture absent");
        var player=GameChecks.createBot(game,"TestFeedbackNew");
        try{var mana=game.items().mana();require(player!=null&&mana.get(player)==5000&&mana.maximum(player)==5000&&mana.baseRegen(player)==10,"real first join supplies 5000/5000 +10 before first tick");}
        finally{if(player!=null)game.server().getPlayerList().remove(player);}
        SkyWars2.LOGGER.info("[check] PASS feedback-first-join");
    }
}
