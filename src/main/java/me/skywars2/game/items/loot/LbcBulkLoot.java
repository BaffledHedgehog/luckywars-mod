package me.skywars2.game.items.loot;

import eu.pb4.polymer.core.api.item.PolymerItemUtils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.component.ItemContainerContents;

/** Reversible display only; the real item always retains its original component maximum. */
public final class LbcBulkLoot {
    private static boolean registered;
    private LbcBulkLoot(){}
    public static boolean isOversizedStoredStack(ItemInstance item) {
        // Native persistent item codecs accept counts through 99.
        return item.count() > item.getMaxStackSize() && item.count() <= 99;
    }
    public static void register(){
        if(registered)return;registered=true;
        // Vanilla client SimpleContainer.setItem clamps received stacks too.
        // Polymer preserves the exact real stack in its restoration metadata.
        PolymerItemUtils.CONTEXT_ITEM_CHECK.register((stack,context)->stack instanceof ItemStack item
                && (isOversizedStoredStack(item)||packedContents(item)!=null));
        PolymerItemUtils.ITEM_MODIFICATION_EVENT.register((original,client,context)->{
            if(client.isEmpty()||!(original instanceof ItemStack stack))return client;
            var contents=packedContents(stack);
            if(!isOversizedStoredStack(stack)&&contents==null)return client;
            var view=client.copy();
            if(isOversizedStoredStack(stack))view.set(DataComponents.MAX_STACK_SIZE,Math.max(view.getMaxStackSize(),stack.getCount()));
            if(contents!=null){
                var slots=NonNullList.withSize(contents.size(),ItemStack.EMPTY);
                contents.copyInto(slots);
                for(var slot:slots)if(isOversizedStoredStack(slot)){
                    // Nested client templates validate their components when materialized.
                    if(slot.has(DataComponents.MAX_DAMAGE)){
                        slot.remove(DataComponents.MAX_DAMAGE);
                        slot.remove(DataComponents.DAMAGE);
                    }
                    slot.set(DataComponents.MAX_STACK_SIZE,slot.getCount());
                }
                view.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(slots));
            }
            return view;
        });
    }
    private static ItemContainerContents packedContents(ItemStack item){
        var contents=item.get(DataComponents.CONTAINER);
        if(contents!=null)for(var stored:contents.nonEmptyItems())
            if(isOversizedStoredStack(stored))return contents;
        return null;
    }
}
