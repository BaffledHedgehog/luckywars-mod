package me.skywars2.game;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

/** Native loot and whole-stack copying; the item entities themselves own all persistent state. */
public final class LbcLooseRewards {
    private final GameController game;
    private final Set<ItemEntity> copiers=new LinkedHashSet<>();
    private int lastTick=Integer.MIN_VALUE;
    LbcLooseRewards(GameController game){this.game=game;}
    public static boolean luckyWand(ItemStack stack){return !stack.isEmpty()&&LbcFartus.data(stack).getIntOr("luckyblockwand",0)==1;}
    public static boolean copier(ItemStack stack){return !stack.isEmpty()&&LbcFartus.data(stack).getIntOr("copypaster",0)==1;}
    private boolean paused(Entity actor){return !game.server().tickRateManager().runsNormally()
            ||game.items().timeStop().frozen(actor)||game.items().timeStop().stopped((ServerLevel)actor.level())
            ||actor.entityTags().contains("time_stopped");}
    public boolean useLucky(ServerPlayer player,InteractionHand hand){
        if(!luckyWand(player.getItemInHand(hand))||!game.canUseGameplayItems(player)||game.isObserver(player)
                ||!player.isAlive()||player.isRemoved()||!LbcDimensionItems.ordinary(player)
                ||game.stage()==GameController.Stage.ENDING||game.items().actionsBlocked(player)||paused(player)
                ||game.items().wands().inputTimeStopped(player))return false;
        // Legacy has neither a mana price nor a debit/cooldown; use the original unfiltered table.
        for(var stack:game.lbc().rollTable(player.level(),player.position(),player,"lbc:luckyblock")){
            var drop=new ItemEntity(player.level(),player.getX(),player.getY(),player.getZ(),stack);
            drop.setThrower(player);game.items().markCurrentItemEpoch(drop);player.level().addFreshEntity(drop);
        }
        return true;
    }
    public void loaded(Entity actor){
        if(actor instanceof ItemEntity item&&copier(item.getItem())&&game.playing()&&LbcDimensionItems.ordinary(item.level())){
            game.items().markCurrentItemEpoch(item);copiers.add(item);
        }
    }
    public void unloaded(Entity actor){copiers.remove(actor);}
    public int count(){return copiers.size();}
    public void tick(){
        int now=game.server().getTickCount();if(lastTick==now)return;lastTick=now;
        for(var item:List.copyOf(copiers))copy(item);
    }
    boolean copy(ItemEntity item){
        if(item.isRemoved()||!copier(item.getItem())||!game.playing()||!game.items().currentEpoch(item)){
            copiers.remove(item);return false;
        }
        if(game.stage()==GameController.Stage.ENDING||!LbcDimensionItems.ordinary(item.level())||paused(item))return false;
        var level=(ServerLevel)item.level();
        ItemEntity donor=null;double nearest=Double.POSITIVE_INFINITY;
        for(var candidate:level.getEntitiesOfClass(ItemEntity.class,item.getBoundingBox().inflate(1),
                e->e!=item&&!e.isRemoved()&&!e.getItem().isEmpty()&&!copier(e.getItem())&&!paused(e))){
            double distance=candidate.distanceToSqr(item);
            if(distance<=1&&distance<nearest){nearest=distance;donor=candidate;}
        }
        if(donor==null)return false;
        // Only Item and Motion were copied in legacy: no donor age, pickup delay or entity NBT.
        var duplicate=new ItemEntity(level,item.getX(),item.getY(),item.getZ(),donor.getItem().copy());
        duplicate.setDeltaMovement(item.getDeltaMovement());
        if(item.getOwner()!=null)duplicate.setThrower(item.getOwner());
        game.items().markCurrentItemEpoch(duplicate);
        try(var source=game.items().spellSources().scope(item)){
            if(!level.addFreshEntity(duplicate))return false;
        }
        item.getItem().shrink(1);
        if(item.getItem().isEmpty()){copiers.remove(item);item.discard();}
        return true;
    }
    public void stop(){copiers.clear();lastTick=Integer.MIN_VALUE;}
}
