package me.skywars2.game;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import java.util.ArrayList;
import me.skywars2.SkyWars2;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

/** Native cumulative damage metric and one immediate source lottery; no damage polling or second curse owner. */
public final class LbcGayradar {
    public static final String DAMAGE_OBJECTIVE="gayradar_damage";
    public static final Item TOKEN=Registry.register(BuiltInRegistries.ITEM,Identifier.parse("skywars2:gayradar"),new Detector(
            LbcUtilityItemPrototypes.properties("skywars2:gayradar","/data/lbc/loot_table/item_defs/lbc/gayradar__02394082461d.json",Items.SNOWBALL,16)));
    private LbcGayradar(){}
    public static void register(){}
    static boolean matches(ItemStack stack){return !stack.isEmpty()&&ItemData.intValue(stack,"gayradar",0)==1;}

    /** Resolve after a rewind as well; never retain a retired Objective or bind Witch's melee-only counter. */
    private static Objective objective(GameController game){
        var board=game.server().getScoreboard();var criterion=Stats.CUSTOM.get(Stats.DAMAGE_DEALT);
        var objective=board.getObjective(DAMAGE_OBJECTIVE);
        if(objective==null)return board.addObjective(DAMAGE_OBJECTIVE,criterion,Component.literal(DAMAGE_OBJECTIVE),ObjectiveCriteria.RenderType.INTEGER,false,null);
        if(objective.getCriteria()!=criterion)throw new IllegalStateException("Gayradar objective must retain its native damage-dealt criterion");
        return objective;
    }
    /** Register before damage can be dealt, not when the rare item first appears. */
    static void prepare(GameController game){objective(game);}
    /** Fresh round/full Balance only. Respawn, reconnect and Supernova retain this round's native metric. */
    static void resetRound(GameController game){
        var objective=objective(game);var board=game.server().getScoreboard();
        for(var player:game.server().getPlayerList().getPlayers())board.getOrCreatePlayerScore(player,objective,true).set(0);
    }
    static int damage(GameController game,ServerPlayer player){
        var score=game.server().getScoreboard().getPlayerScoreInfo(player,objective(game));return score==null?0:score.value();
    }
    private static boolean admitted(GameController game,ServerPlayer player){
        return game.playing()&&game.stage()!=GameController.Stage.ENDING&&!game.maps().busy()
                &&player.isAlive()&&!player.isRemoved()&&!player.isSpectator()&&!game.isObserver(player)
                &&game.canUseGameplayItems(player)&&LbcDimensionItems.ordinary(player)
                &&game.items().wands().validRoleTarget(player,false)&&!game.items().actionsBlocked(player)
                &&!game.items().wands().inputTimeStopped(player)&&!game.items().timeStop().stopped(player.level())
                &&game.server().tickRateManager().runsNormally();
    }
    private static boolean eligible(GameController game,ServerPlayer player){
        return player.isAlive()&&!player.isRemoved()&&!player.isSpectator()&&!game.isObserver(player)
                &&game.items().wands().validRoleTarget(player,false);
    }
    static ServerPlayer choose(GameController game,ServerPlayer owner){
        var best=new ArrayList<ServerPlayer>();int maximum=Integer.MIN_VALUE;
        for(var player:game.server().getPlayerList().getPlayers())if(eligible(game,player)){
            int damage=damage(game,player);
            if(damage>maximum){maximum=damage;best.clear();}
            if(damage==maximum)best.add(player);
        }
        if(best.isEmpty()||maximum==0)return owner;
        // Source visits a random permutation and keeps the first strict maximum: uniform tied maxima.
        return best.get(owner.getRandom().nextInt(best.size()));
    }
    public static boolean use(GameController game,ServerPlayer owner,InteractionHand hand){
        var stack=owner.getItemInHand(hand);
        if(!matches(stack)||!admitted(game,owner)||game.items().magic().mechanismSuppressedAt(owner.level(),owner.position()))return false;
        var target=choose(game,owner);String epoch=game.items().epoch();var origin=owner.position();
        owner.awardStat(Stats.ITEM_USED.get(stack.getItem()));stack.consume(1,owner);
        LbcTmtShotCompletion.afterNativeAction(()->{
            if(!game.items().currentEpoch(epoch)||!eligible(game,target))return;
            game.items().addTurnCurse(target,1800);
            target.addEffect(ItemEffectLevels.create(MobEffects.SLOWNESS,3600,1),owner);
            // The source addresses @a. Reuse the existing minVolume/position sender in every dimension.
            for(var level:game.server().getAllLevels())LbcOverload.sound(level,origin,"minecraft:lbcsounds.gayradar",1_000_000_000F,1,0,1);
        });
        return true;
    }
    private static final class Detector extends SimplePolymerItem {
        Detector(Properties properties){super(properties,Items.SNOWBALL,true);}
        @Override public InteractionResult use(Level level,Player player,InteractionHand hand){var game=SkyWars2.game();
            return game!=null&&player instanceof ServerPlayer owner&&LbcGayradar.use(game,owner,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;}
    }
}
