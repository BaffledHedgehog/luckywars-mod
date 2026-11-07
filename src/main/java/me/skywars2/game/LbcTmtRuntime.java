package me.skywars2.game;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomModelData;

/** Native TMT entry-point coordination. Property mechanics remain with their existing owners. */
public final class LbcTmtRuntime {
    private final GameController game;
    private final Set<ServerPlayer> triggered=Collections.newSetFromMap(new IdentityHashMap<>());
    private int tick=Integer.MIN_VALUE;
    private String epoch;
    private LbcTmtProperties generator;
    private LbcTmtUseProfiles useProfiles;
    private LbcTmtCatalog catalog;
    LbcTmtRuntime(GameController game){this.game=game;}

    public boolean admitted(ServerPlayer player){
        return player!=null&&player.isAlive()&&!player.isRemoved()
                &&game.canUseGameplayItems(player)&&!game.isObserver(player)
                &&game.stage()!=GameController.Stage.ENDING&&!game.items().actionsBlocked(player)
                &&!game.items().wands().inputTimeStopped(player);
    }
    private boolean enter(ServerPlayer player){
        if(!admitted(player))return false;
        int now=game.server().getTickCount();String current=game.items().epoch();
        if(now!=tick||!current.equals(epoch)){triggered.clear();tick=now;epoch=current;}
        // Mark before any native callback; nested damage or another hand cannot reenter this tick.
        return triggered.add(player);
    }
    private void selfGroups(ServerPlayer player){
        if(LbcTmtItems.matches(player.getMainHandItem()))LbcTmtSelfAbilities.swords(game,player);
        else {game.items().classicMelee().sourceSelf(player);game.items().chargedSwords().sourceSelf(player);}
        LbcTmtSelfAbilities.armor(game,player);
    }
    private void charging(ServerPlayer player){
        if(!LbcDimensionItems.ordinary(player))return;
        // The obsolete source infinite-arrow item injection is deliberately replaced by native ammo selection.
        game.items().mana().score(player,"crossbow_charge").add(1);
    }
    public void usingItem(ServerPlayer player,InteractionHand hand,ItemStack used){
        if(player.getUseItem()==used&&LbcTmtItems.matches(used)&&!LbcTmtAbilities.active(used))LbcTmtContinuous.using(game,player,used);
        if(player.getUseItem()!=used||!LbcTmtItems.matches(used)||LbcTmtAbilities.active(used)||!enter(player))return;
        selfGroups(player);charging(player);
    }
    /** Native accepted bow/crossbow event; ammo/trinkets were already handled once by the shot owner. */
    public void bowShot(ServerPlayer player,InteractionHand hand,ItemStack weapon,ItemStack fired,List<Projectile> family){
        if(family.isEmpty()||!LbcTmtItems.matches(fired)||!enter(player))return;
        if(!LbcDimensionItems.ordinary(player)){
            if(player.getItemInHand(hand)==weapon&&!weapon.isEmpty())spells(player,hand);
            return;
        }
        selfGroups(player);
        if(fired.getItem() instanceof CrossbowItem){
            game.items().mana().score(player,"bow").set(0);
            game.items().mana().score(player,"bow_charge").set(0);
            game.items().mana().score(player,"bow_charge_sub").set(0);
        }
        charging(player);
        if(fired.getItem() instanceof BowItem)game.items().mana().score(player,"crossbow_charge").set(0);
        if(player.getItemInHand(hand)==weapon&&!weapon.isEmpty())spells(player,hand);
    }
    private int spells(ServerPlayer player,InteractionHand hand){
        int count=game.items().wands().castTmtSpells(player,hand);
        count+=useProfiles().use(player,hand);
        if(hand==InteractionHand.MAIN_HAND&&game.config().offhandWands){
            var other=player.getOffhandItem();
            if(LbcTmtItems.matches(other)){
                count+=game.items().wands().castTmtSpells(player,InteractionHand.OFF_HAND);
                count+=useProfiles().use(player,InteractionHand.OFF_HAND);
            }
            else if(game.items().wands().use(player,InteractionHand.OFF_HAND))count++;
        }
        return count;
    }
    /** Carrot and instant throwing carriers have their ordinary direct-use spell event. */
    boolean rightClick(ServerPlayer player,InteractionHand hand){
        var stack=player.getItemInHand(hand);
        if(!LbcTmtItems.matches(stack)||LbcTmtAbilities.active(stack)||!enter(player))return false;
        selfGroups(player);charging(player);return spells(player,hand)>0;
    }
    net.minecraft.world.InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context){
        if(!(context.getPlayer() instanceof ServerPlayer owner)||!LbcTmtItems.matches(context.getItemInHand())||LbcTmtAbilities.active(context.getItemInHand())||!enter(owner))return net.minecraft.world.InteractionResult.PASS;
        int count=LbcTmtBlockUses.use(game,context);
        if(count==0){triggered.remove(owner);return net.minecraft.world.InteractionResult.PASS;}
        return net.minecraft.world.InteractionResult.SUCCESS;
    }
    /** Source damaged continuation only owns the once-per-tick guard. It never fabricates a self activation. */
    void damaged(ServerPlayer player){
        if(!admitted(player))return;
        for(int slot=0;slot<=40;slot++)if(LbcTmtItems.matches(player.getInventory().getItem(slot))){enter(player);return;}
    }
    public boolean heldTraitsNeeded(net.minecraft.world.damagesource.DamageSource source){
        return source.getEntity() instanceof ServerPlayer owner&&LbcDamage.isMeleeAttack(source,owner)
                &&LbcTmtItems.matches(owner.getMainHandItem())&&!LbcTmtAbilities.active(owner.getMainHandItem());
    }
    /** One genuinely accepted melee event; independent player/armor/criterion owners stay outside this group. */
    public void withHeldTraits(net.minecraft.world.damagesource.DamageSource source,Runnable nativeOwners){
        if(!heldTraitsNeeded(source)){nativeOwners.run();return;}
        var owner=(ServerPlayer)source.getEntity();var stack=owner.getMainHandItem();
        for(var slot:LbcTmtAbilities.read(stack)){
            if(stack.isEmpty()||owner.getMainHandItem()!=stack)break;
            LbcTmtAbilities.invoke(stack,slot.id(),()->{nativeOwners.run();return true;});
        }
    }
    void withConsumedTraits(ServerPlayer player,ItemStack stack,Runnable nativeOwners){
        if(!LbcTmtItems.matches(stack)||LbcTmtAbilities.active(stack)){nativeOwners.run();return;}
        game.items().foodLifecycle().countTmtFood(player,stack);
        for(var slot:LbcTmtAbilities.read(stack)){
            if(stack.isEmpty())break;
            LbcTmtAbilities.invoke(stack,slot.id(),()->{nativeOwners.run();LbcTmtManaPotions.consumed(game,player,stack);return true;});
        }
        game.items().foodLifecycle().finishTmtTraits(player,stack);
    }
    ItemStack generate(RandomSource random,List<LbcTmtAbilities.Ability> draws){
        if(generator==null)generator=new LbcTmtProperties(game.maps().level());
        var result=generator.generate(random,LbcTmtItems::prototype,draws);LbcTmtTotems.apply(result);LbcTmtContinuous.apply(result);return result;
    }
    public ItemStack generate(RandomSource random){return generate(random,catalog().drawSeven(random));}
    LbcTmtCatalog catalog(){if(catalog==null)catalog=new LbcTmtCatalog(game,useProfiles());return catalog;}
    static void visual(ItemStack stack){
        int seed=ItemData.intValue(stack,"tmt_texture_seed",0);
        var current=stack.getOrDefault(DataComponents.CUSTOM_MODEL_DATA,CustomModelData.EMPTY);
        var colors=new java.util.ArrayList<>(current.colors());
        if(colors.isEmpty())colors.add(0);colors.set(0,0xFA0000|(seed&0xffff));
        stack.set(DataComponents.CUSTOM_MODEL_DATA,new CustomModelData(current.floats(),current.flags(),current.strings(),List.copyOf(colors)));
        stack.set(DataComponents.ITEM_MODEL,net.minecraft.resources.Identifier.parse("lbc:tmt/procedural"));
    }
    LbcTmtUseProfiles useProfiles(){
        if(useProfiles==null)useProfiles=new LbcTmtUseProfiles(game,game.items().wands()::tmtNativeUseCandidate);
        return useProfiles;
    }
    public void reload(){generator=null;useProfiles=null;catalog=null;}
    void forget(ServerPlayer player){triggered.remove(player);}
    void stop(){triggered.clear();epoch=null;tick=Integer.MIN_VALUE;}
}
