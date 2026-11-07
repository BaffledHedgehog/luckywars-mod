package me.skywars2.game;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;

/** Authored native shield blocking components; physical carrier use and damage remain native. */
final class LbcTmtShield {
    private LbcTmtShield(){}
    private static Item selected(ItemStack stack){
        var selected=LbcTmtAbilities.nativePredicatesActive()?LbcTmtAbilities.nativePredicateItem(stack):null;
        return selected==null?stack.getItem():selected;
    }
    /** Source predicate. The sword's hylian flag is deliberately unrelated. */
    static boolean candidate(ItemStack stack){
        if(stack.isEmpty()||!stack.has(DataComponents.BLOCKS_ATTACKS))return false;
        var item=selected(stack);
        // Energy is a SimplePolymerItem using generic Item.use, not a ShieldItem.
        // Inspect only this selected source; an aggregate sibling query here would
        // incorrectly admit another private property's use as an Energy input.
        return item==LbcElectro.SHIELD||item instanceof ShieldItem
                &&(item==LbcMeleeItems.Kind.HYLIAN_SHIELD.item||ItemData.intValue(stack,"hylianshield",0)==1
                    ||ItemData.intValue(stack,"energy_shield",0)==1||ItemData.intValue(stack,"energyshield",0)==1);
    }
    /** Aggregate admission is used only to attach/start the real private shield trait. */
    static boolean sustained(ItemStack stack){return LbcTmtItems.matches(stack)
            &&(ItemData.intValue(stack,"hylianshield",0)==1||LbcElectro.isShield(stack));}

    /** Run at generation/rehydration with the existing sustained-component applier. */
    static void apply(GameController game,ItemStack stack){
        if(!sustained(stack))return;
        for(var property:LbcTmtAbilities.read(stack)){
            var source=LbcTmtComponents.source(game,property);if(!candidate(source))continue;
            // BLOCKS_ATTACKS is the behavior. Keep generated wear, unbreakability, food,
            // attributes, model and every other independently generated component intact.
            stack.set(DataComponents.BLOCKS_ATTACKS,source.get(DataComponents.BLOCKS_ATTACKS));
        }
    }

    /** Existing private native-use coordinator calls the actual source ShieldItem/Item use body. */
    static boolean use(GameController game,ServerPlayer owner,InteractionHand hand){
        var stack=owner.getItemInHand(hand);
        if(!LbcTmtAbilities.active(stack)||!candidate(stack)||!game.items().tmt().admitted(owner))return false;
        // USING replays source spell groups every real tick. Restarting native use here
        // would reset its blocking delay forever and prevent a consumable from finishing.
        if(owner.isUsingItem()&&owner.getUseItem()==stack&&owner.getUsedItemHand()==hand)return true;
        return selected(stack).use(owner.level(),owner,hand).consumesAction();
    }

    /** Native Bow/Crossbow may fall back here when ordinary ammo use cannot start. */
    static InteractionResult input(GameController game,ServerPlayer owner,InteractionHand hand){
        var stack=owner.getItemInHand(hand);
        if(!sustained(stack)||LbcTmtAbilities.active(stack))return InteractionResult.PASS;
        if(!game.items().tmt().admitted(owner))return InteractionResult.FAIL;
        for(var property:LbcTmtAbilities.read(stack)){
            var source=LbcTmtComponents.source(game,property);if(!candidate(source))continue;
            return LbcTmtAbilities.invoke(stack,property.id(),()->use(game,owner,hand))?InteractionResult.CONSUME:InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }
}
