package me.skywars2.game;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;

/** Hand-operated kit utilities use the common two-hand input and canonical native effect owners. */
public final class LbcKitUtilities {
    private LbcKitUtilities(){}
    public static boolean palm(ItemStack stack){return !stack.isEmpty()&&LbcFartus.data(stack).getIntOr("banana_palm",0)==1;}
    public static boolean matches(ItemStack stack){return LbcMultiplierTablets.kind(stack)==5||palm(stack);}
    public static boolean use(GameController game,ServerPlayer player,InteractionHand hand){
        var stack=player.getItemInHand(hand);
        if(!matches(stack)||!player.isAlive()||player.isRemoved()||!game.canUseGameplayItems(player)||game.isObserver(player)
                ||game.stage()==GameController.Stage.ENDING||!LbcDimensionItems.ordinary(player)
                ||!game.items().wands().validRoleTarget(player,false)||game.items().actionsBlocked(player)
                ||game.server().tickRateManager().isFrozen()||!game.server().tickRateManager().runsNormally()
                ||game.items().timeStop().stopped(player.level())||game.items().wands().inputTimeStopped(player))return false;
        if(palm(stack)){
            // The ordinary palm is physical and uses only the source's main-hand input.
            if(hand!=InteractionHand.MAIN_HAND)return false;
            for(var banana:game.lbc().rollTable(player.level(),player.position(),player,"lbc:bananas")){
                var drop=new ItemEntity(player.level(),player.getX(),player.getY()+1,player.getZ(),banana);
                drop.setThrower(player);game.items().markCurrentItemEpoch(drop);player.level().addFreshEntity(drop);
            }
            player.level().playSound(null,player.getX(),player.getY(),player.getZ(),SoundEvents.HONEYCOMB_WAX_ON,SoundSource.MASTER,1,1);
        }else{
            if(game.items().magic().suppressed(player)||player.entityTags().contains("nomagic_active")
                    ||game.items().magic().suppressedAt(player.level(),player.getEyePosition()))return false;
            EffectLayers.multiplyDurations(game,player,5);
        }
        var item=stack.getItem();
        // The source consumes one x5 even without finite effects; debit only the actual used hand.
        stack.shrink(1);player.awardStat(Stats.ITEM_USED.get(item));
        player.getInventory().setChanged();game.items().magic().inventoryChanged(player);player.containerMenu.broadcastChanges();
        return true;
    }
}
