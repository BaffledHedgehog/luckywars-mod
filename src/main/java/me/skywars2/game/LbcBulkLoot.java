package me.skywars2.game;

import eu.pb4.polymer.core.api.item.PolymerItemUtils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemInstance;

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
        PolymerItemUtils.CONTEXT_ITEM_CHECK.register((stack,context)->stack instanceof ItemStack && isOversizedStoredStack(stack));
        PolymerItemUtils.ITEM_MODIFICATION_EVENT.register((original,client,context)->{
            if(client.isEmpty()||!(original instanceof ItemStack stack)||!isOversizedStoredStack(stack))return client;
            var view=client.copy();view.set(DataComponents.MAX_STACK_SIZE,Math.max(view.getMaxStackSize(),stack.getCount()));
            return view;
        });
    }
}
