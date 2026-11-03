package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Actual loadout application, native attributes and scoreboard resource layers. */
public final class ManaLevelupChecks {
    private ManaLevelupChecks(){}
    private static void require(boolean ok,String message){if(!ok)throw new IllegalStateException("Mana levelups: "+message);}
    private static LbcManaLevelups.Values values(GameController game,net.minecraft.server.level.ServerPlayer player){
        var mana=game.items().mana();return new LbcManaLevelups.Values(mana.get(player),mana.maximum(player),mana.baseRegen(player));
    }
    public static void run(GameController game){
        try(var fixture=new ElementalGrimoireChecks.Fixture(game)){
            var attributes=new net.minecraft.world.entity.ai.attributes.AttributeMap(net.minecraft.world.entity.ai.attributes.DefaultAttributes.getSupplier(net.minecraft.world.entity.EntityTypes.PLAYER));attributes.assignAllValues(fixture.owner.getAttributes());
            fixture.initialize();var player=fixture.owner;var mana=game.items().mana();var profile=game.players().get(player.getUUID());
            int[] oldPicks=profile.levelups.clone(),oldTrinkets=profile.trinkets.clone();int oldSkill=profile.lbcSkill,oldKit=profile.lbcKit,oldChallengeKit=profile.lbcChallengeKit;
            boolean enabled=game.config().lbcLevelups,ranked=game.config().ranked;int limit=game.config().lbcLevelupLimit;
            var board=game.server().getScoreboard();var names=List.of("levelup_mana_delta","levelup_manamax_delta","levelup_manaregen_delta");
            var originalObjectives=new HashMap<String,net.minecraft.world.scores.Objective>();var originalValues=new HashMap<String,Integer>();
            for(var name:names){var objective=board.getObjective(name);originalObjectives.put(name,objective);var score=objective==null?null:board.getPlayerScoreInfo(player,objective);originalValues.put(name,score==null?null:score.value());}
            try{
                game.config().lbcLevelups=true;game.config().ranked=true;game.config().lbcLevelupLimit=Math.min(5,profile.levelups.length);
                profile.lbcSkill=0;profile.lbcKit=0;profile.lbcChallengeKit=0;Arrays.fill(profile.trinkets,0);player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(40);
                int[] ids={2,11,17,31,32};int[][] expected={{5000,5000,15},{2500,2500,30},{5000,6500,14},{5000,10000,30},{0,0,0}};
                for(int n=0;n<ids.length;n++){
                    game.lbc().clear(player);mana.resetResources(player);Arrays.fill(profile.levelups,0);profile.levelups[0]=ids[n];
                    game.lbc().apply(player,false);var want=new LbcManaLevelups.Values(expected[n][0],expected[n][1],expected[n][2]);
                    require(values(game,player).equals(want),"actual initial source resources "+ids[n]);
                    game.lbc().apply(player,false);require(values(game,player).equals(want),"reapplication cannot accumulate "+ids[n]);
                    mana.resetResources(player);game.lbc().apply(player,false);require(values(game,player).equals(want),"fresh life drops old layer before base reset "+ids[n]);
                    if(ids[n]==31)require(player.getMaxHealth()==24,"Sanguis subtracts16 from configured native40 HP");
                    if(ids[n]==32)require(player.getMaxHealth()==46&&player.getAttributeValue(Attributes.ARMOR)>=10&&player.getAttributeValue(Attributes.ARMOR_TOUGHNESS)>=8,"Nulla native stat bonuses");
                }
                game.lbc().clear(player);mana.resetResources(player);Arrays.fill(profile.levelups,0);System.arraycopy(ids,0,profile.levelups,0,ids.length);
                game.lbc().apply(player,false);require(values(game,player).equals(new LbcManaLevelups.Values(0,0,0)),"Nulla clears resources after all other initial mana bonuses");
                require(player.getMaxHealth()==30,"combined native health modifiers preserve configured base");
                game.lbc().clear(player);mana.resetResources(player);Arrays.fill(profile.levelups,31);game.lbc().apply(player,false);
                require(values(game,player).equals(new LbcManaLevelups.Values(5000,10000,30))&&player.getMaxHealth()==24,"duplicate source tag and modifier IDs remain nonstacking");
                mana.add(player,123);mana.maximum(player,mana.maximum(player)+77);mana.baseRegen(player,mana.baseRegen(player)+2);
                var external=values(game,player);game.lbc().apply(player,false);require(values(game,player).equals(external),"debug reapplication preserves unrelated resource changes");
                game.config().lbcLevelups=false;game.lbc().apply(player,false);
                require(values(game,player).equals(new LbcManaLevelups.Values(5123,5077,12))&&player.getMaxHealth()==40,"disabling removes only owned layer and native modifiers");
                require(LbcManaLevelups.resolve(new LbcManaLevelups.Values(Integer.MIN_VALUE,Integer.MIN_VALUE,Integer.MAX_VALUE),Set.of(11)).equals(new LbcManaLevelups.Values(Integer.MIN_VALUE-2500,Integer.MIN_VALUE-2500,Integer.MAX_VALUE+20)),"native signed scoreboard arithmetic is preserved");
            }finally{
                game.lbc().clear(player);profile.levelups=oldPicks;profile.trinkets=oldTrinkets;profile.lbcSkill=oldSkill;profile.lbcKit=oldKit;profile.lbcChallengeKit=oldChallengeKit;
                game.config().lbcLevelups=enabled;game.config().ranked=ranked;game.config().lbcLevelupLimit=limit;
                for(var name:names){var objective=board.getObjective(name);if(originalObjectives.get(name)==null){if(objective!=null)board.removeObjective(objective);}else if(originalValues.get(name)==null)board.resetSinglePlayerScore(player,objective);else board.getOrCreatePlayerScore(player,objective).set(originalValues.get(name));}
                player.getAttributes().assignAllValues(attributes);
            }
        }
        SkyWars2.LOGGER.info("[check] PASS mana-levelups (five source perks, native initial/fresh-life resources and health, unique tags, Nulla ordering, relative debug layers, external changes and native signed arithmetic)");
    }
}
