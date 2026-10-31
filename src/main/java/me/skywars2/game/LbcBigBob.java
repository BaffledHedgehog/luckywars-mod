package me.skywars2.game;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

/** Original Big Bob nut dispenser. The existing food owners handle every nut consequence. */
public final class LbcBigBob {
    private LbcBigBob(){}
    public static boolean matches(ItemStack stack){
        return !stack.isEmpty()&&LbcFartus.data(stack).getIntOr("big_bob",0)==1;
    }
    public static boolean use(GameController game,ServerPlayer player,InteractionHand hand){
        if(!matches(player.getItemInHand(hand))||!game.playing()||game.stage()==GameController.Stage.ENDING
                ||!game.canUseGameplayItems(player)||game.isObserver(player)||!player.isAlive()||player.isRemoved()
                ||!LbcDimensionItems.ordinary(player)||game.items().actionsBlocked(player)
                ||!game.server().tickRateManager().runsNormally()||game.items().timeStop().frozen(player)
                ||game.items().timeStop().stopped(player.level())||player.entityTags().contains("time_stopped")
                ||game.items().wands().inputTimeStopped(player)
                ||hand==InteractionHand.OFF_HAND&&!game.config().offhandWands)return false;
        // Both source dispatchers position the function one block above the executor.
        var at=player.position().add(0,1,0);
        for(var stack:game.lbc().rollTable(player.level(),at,player,"lbc:oreshki_10")){
            var drop=new ItemEntity(player.level(),at.x,at.y,at.z,stack);
            drop.setThrower(player);
            game.items().markCurrentItemEpoch(drop);
            player.level().addFreshEntity(drop);
        }
        // Source has no mana, stack debit, cooldown, MAG or TECH condition.
        LbcOverload.sound(player,at,"minecraft:ui.cartography_table.take_result",1,1,0);
        return true;
    }
}
