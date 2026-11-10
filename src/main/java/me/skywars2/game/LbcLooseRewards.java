package me.skywars2.game;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.ItemEnchantments;
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
    private static boolean tracked(ItemStack stack){return LbcTmtLooseRewards.present(stack);}
    private int lastTick=Integer.MIN_VALUE;
    LbcLooseRewards(GameController game){this.game=game;}
    public static boolean luckyWand(ItemStack stack){return ItemData.intValue(stack,"luckyblockwand",0)==1;}
    public static boolean copier(ItemStack stack){return LbcTmtLooseRewards.copier(stack);}
    static ItemStack copyStack(ItemStack donor){return donor.copy();}
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
        if(actor instanceof ItemEntity item&&tracked(item.getItem())&&game.playing()&&LbcDimensionItems.ordinary(item.level())){
            // A retained TMT token from an older match cannot acquire a fresh epoch on reload.
            if(LbcTmtLooseRewards.outer(item.getItem()))for(var tag:item.entityTags())
                if(tag.startsWith("skywars2.items.")&&!game.items().currentEpoch(tag))return;
            game.items().markCurrentItemEpoch(item);copiers.add(item);
        }
    }
    public void unloaded(Entity actor){copiers.remove(actor);}
    public int count(){return copiers.size();}
    /** Frozen body replay suppresses ENTITY_LOAD; rebuild only this derived index, never epoch tags. */
    void reindexRestored(java.util.Collection<? extends Entity> bodies){
        if(!game.server().isSameThread()||!game.server().tickRateManager().isFrozen())
            throw new IllegalStateException("Reindex restored loose rewards on the frozen native thread");
        stop();
        if(!game.playing())return;
        for(var body:bodies)if(body instanceof ItemEntity item&&item.isAlive()&&!item.isRemoved()
                &&item.level() instanceof ServerLevel level&&level.getServer()==game.server()
                &&level.getEntity(item.getUUID())==item&&LbcDimensionItems.ordinary(level)
                &&game.items().currentEpoch(item)&&tracked(item.getItem()))copiers.add(item);
    }
    public void tick(){
        int now=game.server().getTickCount();if(lastTick==now)return;lastTick=now;
        for(var item:List.copyOf(copiers)){
            if(LbcTmtLooseRewards.outer(item.getItem()))tmt(item,LbcTmtLooseRewards.ALL);
            else if(copier(item.getItem()))copy(item);else tablet(item);
        }
    }
    private boolean tmt(ItemEntity item,int mode){
        if(item.isRemoved()||!tracked(item.getItem())||!game.playing()||!game.items().currentEpoch(item)){
            copiers.remove(item);return false;
        }
        if(game.stage()==GameController.Stage.ENDING||!LbcDimensionItems.ordinary(item.level())||paused(item))return false;
        try(var source=game.items().spellSources().scope(item)){return LbcTmtLooseRewards.work(this,item,mode);}
    }
    boolean copy(ItemEntity item){
        if(LbcTmtLooseRewards.outer(item.getItem()))return tmt(item,LbcTmtLooseRewards.COPIERS);
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
        var duplicate=new ItemEntity(level,item.getX(),item.getY(),item.getZ(),copyStack(donor.getItem()));
        duplicate.setDeltaMovement(item.getDeltaMovement());
        if(item.getOwner()!=null)duplicate.setThrower(item.getOwner());
        game.items().markCurrentItemEpoch(duplicate);
        try(var source=game.items().spellSources().scope(item)){
            if(!level.addFreshEntity(duplicate))return false;
        }
        consume(item);
        return true;
    }
    /** The shared loaded-item index and native tick own all dropped-tablet work. */
    boolean tablet(ItemEntity item){
        if(LbcTmtLooseRewards.outer(item.getItem()))return tmt(item,LbcTmtLooseRewards.TABLETS);
        int kind=LbcMultiplierTablets.kind(item.getItem());
        if(item.isRemoved()||kind<2||kind>4||!game.playing()||!game.items().currentEpoch(item)){
            copiers.remove(item);return false;
        }
        if(game.stage()==GameController.Stage.ENDING||!LbcDimensionItems.ordinary(item.level())||paused(item))return false;
        var level=(ServerLevel)item.level();
        if(game.server().tickRateManager().isFrozen()||game.items().magic().suppressedAt(level,item.position()))return false;
        ItemEntity donor=null;double nearest=Double.POSITIVE_INFINITY;
        for(var candidate:level.getEntitiesOfClass(ItemEntity.class,item.getBoundingBox().inflate(1),
                e->e!=item&&!e.isRemoved()&&!e.getItem().isEmpty()&&!LbcTmtLooseRewards.multiplyPresent(e.getItem())&&!paused(e))){
            double distance=candidate.distanceToSqr(item);
            if(distance<=1&&distance<nearest){nearest=distance;donor=candidate;}
        }
        // Select the nearest eligible physical item first; a no-op never falls through to a farther one.
        if(donor==null||game.items().magic().suppressedAt(level,donor.position()))return false;
        var original=donor.getItem();var changed=LbcMultiplierTablets.amplified(original,kind);
        if(changed.isEmpty())return false;
        if(kind==3){
            long remaining=original.getCount()*3L;
            int maximum=Math.max(1,original.getMaxStackSize());
            int first=(int)Math.min(remaining,maximum);remaining-=first;
            var inserted=new ArrayList<ItemEntity>();
            try(var source=game.items().spellSources().scope(donor)){
                while(remaining>0){
                    int count=(int)Math.min(remaining,maximum);
                    var extra=new ItemEntity(level,donor.getX(),donor.getY(),donor.getZ(),original.copyWithCount(count));
                    // Native serialization retains age, pickup delay, pickup owner, motion and entity flags.
                    var identity=extra.getUUID();extra.restoreFrom(donor);extra.setUUID(identity);
                    extra.setItem(original.copyWithCount(count));
                    game.items().markCurrentItemEpoch(extra);
                    if(!level.addFreshEntity(extra)){
                        extra.discard();for(var added:inserted)added.discard();return false;
                    }
                    inserted.add(extra);remaining-=count;
                }
            }catch(RuntimeException failure){for(var added:inserted)added.discard();throw failure;}
            changed.setCount(first);
        }
        donor.setItem(changed);
        consume(item);
        return true;
    }
    private void consume(ItemEntity item){
        // Native setItem replaces the actual host stack while the selected source is projected.
        // Publish a complete physical remainder now; closing the old stack's scope cannot fix a copy.
        var remainder=LbcTmtAbilities.outerCopy(item.getItem());remainder.shrink(1);item.setItem(remainder);
        if(remainder.isEmpty()){copiers.remove(item);item.discard();}
    }
    public void stop(){copiers.clear();lastTick=Integer.MIN_VALUE;}
}
