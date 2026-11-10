package me.skywars2.game;

import java.util.List;
import me.skywars2.SkyWars2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.ScoreHolder;

/** Actual locator stacks, persisted operator settings and native command dispatch. */
public final class FeedbackPatchChecks {
    private FeedbackPatchChecks() {}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Feedback patch: "+why);}
    public static void run(GameController game){
        int health=game.config().healthDisplay,chance=game.config().imbaDropChance,prefixes=game.config().prefixes;
        try(var f=new WoolBucketChecks.Fixture(game,false)){
            f.prepareParticipants();var board=game.server().getScoreboard();
            for(int mode=0;mode<4;mode++){
                game.configureLobbySetting(f.owner,"healthDisplay",Integer.toString(mode));
                require((board.getDisplayObjective(DisplaySlot.BELOW_NAME)!=null)==((mode&1)!=0),"below-name setting "+mode);
                require((board.getDisplayObjective(DisplaySlot.LIST)!=null)==((mode&2)!=0),"TAB setting "+mode);
            }
            for(int mode=0;mode<7;mode++){
                game.configureLobbySetting(f.owner,"imbaDropChance",Integer.toString(mode));
                require(game.items().mana().score(ScoreHolder.forNameOnly("#imbadrop_chance"),"swrg.math").get()==mode
                        &&game.items().mana().score(ScoreHolder.forNameOnly("imbadrop_chance"),"lbc.math").get()==mode,"both real source chance consumers updated "+mode);
            }
            game.configureLobbySetting(f.owner,"prefixes","2");
            var loaded=GameConfig.load();require(loaded.healthDisplay==3&&loaded.imbaDropChance==6&&loaded.prefixes==2,"all new settings survive actual file reload");
            // The shared WILD fixture deliberately removes both players from match teams.
            var team=board.addPlayerTeam("feedback_prefix_check");
            try{
                MatchSettings.prefixes(game,List.of(team));require(!team.getPlayerPrefix().getString().isEmpty(),"actual source mixed prefix applied to a match team");
                game.configureLobbySetting(f.owner,"prefixes","3");MatchSettings.prefixes(game,List.of(team));require(team.getPlayerPrefix().getString().isEmpty(),"disabled prefix clears owned presentation");
            }finally{board.removePlayerTeam(team);}
            f.other.setPos(f.owner.getX()+1,f.owner.getY(),f.owner.getZ());
            var compass=new ItemStack(Items.COMPASS);f.owner.getInventory().setItem(9,compass);
            game.updateTrackers();var pointer=compass.get(DataComponents.LODESTONE_TRACKER);
            require(pointer!=null&&pointer.target().orElseThrow().pos().equals(f.other.blockPosition())
                    &&pointer.target().orElseThrow().dimension().equals(f.other.level().dimension()),"actual chest locator tracks nearest live player");
            require(compass.get(DataComponents.CUSTOM_NAME).getString().equals(f.other.getName().getString()),"actual locator includes target nickname");
            var source=f.owner.createCommandSourceStack();
            try{
                require(game.server().getCommands().getDispatcher().getRoot().getChild("tps")==null
                        &&game.server().getCommands().getDispatcher().execute("skywars tps",source)==1,"only the prefixed TPS command is registered and executes");
            }catch(com.mojang.brigadier.exceptions.CommandSyntaxException error){throw new IllegalStateException("TPS command dispatch",error);}
        }finally{
            game.config().healthDisplay=health;game.config().imbaDropChance=chance;game.config().prefixes=prefixes;
            game.config().save();game.luckyModifiers().syncConfig();
        }
        SkyWars2.LOGGER.info("[check] PASS feedback-patch (all4 HP modes, all7 real loot chance settings, persisted config, actual source team prefixes, nearest-player locator name/coordinates and native TPS commands)");
    }
}
