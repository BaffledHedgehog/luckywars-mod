package me.skywars2.game;

import eu.pb4.polymer.core.api.item.SimplePolymerItem;
import java.util.ArrayList;
import me.skywars2.SkyWars2;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** Immediate source seven-way lottery; a legacy snowball only detected the input. */
public final class LbcKillButton {
    public static final Item TOKEN=Registry.register(BuiltInRegistries.ITEM,Identifier.parse("skywars2:kill_button"),new Button(
            LbcUtilityItemPrototypes.properties("skywars2:kill_button","/data/lbc/loot_table/item_defs/lbc/kill_button__37566676de11.json",Items.SNOWBALL,16)));
    private LbcKillButton(){}
    public static void register(){}
    static boolean matches(ItemStack stack){return !stack.isEmpty()&&ItemData.intValue(stack,"kill_rand",0)==1;}
    private static boolean admitted(GameController game,ServerPlayer owner){
        return game.playing()&&game.stage()!=GameController.Stage.ENDING&&!game.maps().busy()
                &&owner.isAlive()&&!owner.isRemoved()&&!owner.isSpectator()&&!game.isObserver(owner)
                &&game.canUseGameplayItems(owner)&&LbcDimensionItems.ordinary(owner)
                &&game.items().wands().validRoleTarget(owner,false)&&!game.items().actionsBlocked(owner)
                &&!game.items().wands().inputTimeStopped(owner)&&!game.items().timeStop().stopped(owner.level())
                &&game.server().tickRateManager().runsNormally();
    }
    private static boolean tech(GameController game,ServerPlayer player){return game.items().magic().mechanismSuppressedAt(player.level(),player.position(),24);}
    private static boolean protectedTarget(GameController game,ServerPlayer player){return LbcGreed.active(player)
            ||game.items().mana().score(player,"void_resistance").get()>=1;}
    public static boolean use(GameController game,ServerPlayer owner,InteractionHand hand){
        var stack=owner.getItemInHand(hand);if(!matches(stack)||!admitted(game,owner))return false;
        // The accepted legacy use consumed its snowball even when TECH prevented the effect.
        // Debit before any lethal action; The existing native source adapter owns the selected property's debit.
        owner.awardStat(Stats.ITEM_USED.get(stack.getItem()));stack.consume(1,owner);
        if(tech(game,owner))return true;
        int outcome=owner.getRandom().nextInt(7);ServerPlayer chosen=owner;
        if(outcome==6){
            var candidates=new ArrayList<ServerPlayer>();
            for(var player:game.server().getPlayerList().getPlayers())if(player!=owner&&player.isAlive()&&!player.isRemoved()
                    &&!player.isSpectator()&&!game.isObserver(player)&&!LbcGreed.active(player)
                    &&game.items().wands().validRoleTarget(player,false))candidates.add(player);
            chosen=candidates.isEmpty()?null:candidates.get(owner.getRandom().nextInt(candidates.size()));
        }
        // Choose once. Void protection, target dimension and TECH never cause a reroll.
        var target=chosen;boolean execute=target!=null&&!protectedTarget(game,target)
                &&(outcome!=6||!target.level().dimension().identifier().toString().equals("minecraft:nexus")&&!tech(game,target));
        var cause=game.items().indirectKills().wildCause(owner);String epoch=game.items().epoch();
        // Native shot criteria and private component restoration complete synchronously first.
        LbcTmtShotCompletion.afterNativeAction(()->{
            if(!game.items().currentEpoch(epoch))return;
            if(execute)game.items().indirectKills().inheritedKill(target,cause);
            if(outcome==6){var advancement=game.server().getAdvancements().get(Identifier.parse("lbc:true_advancements/legends/kill_button"));
                if(advancement!=null)for(var criterion:advancement.value().criteria().keySet())owner.getAdvancements().award(advancement,criterion);}
        });
        return true;
    }
    private static final class Button extends SimplePolymerItem {
        Button(Properties properties){super(properties,Items.SNOWBALL,true);}
        @Override public InteractionResult use(Level level,Player player,InteractionHand hand){var game=SkyWars2.game();
            return game!=null&&player instanceof ServerPlayer owner&&LbcKillButton.use(game,owner,hand)?InteractionResult.SUCCESS:InteractionResult.FAIL;}
    }
}
