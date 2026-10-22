package me.skywars2.game;

import eu.pb4.polymer.core.api.item.PolymerItemUtils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/** Server enchantment levels stay authoritative; vanilla clients receive safe component payloads. */
public final class EnchantmentRules {
    private static boolean registered;
    private EnchantmentRules() { }

    public static int mergeLevels(int first,int second) {
        int stronger=Math.max(first,second),weaker=Math.min(first,second);
        if(weaker<=0)return Math.max(0,stronger);
        long bonus=weaker/4L;
        if(Math.abs((long)first-second)<=1)bonus=Math.max(1,bonus);
        return (int)Math.min(Integer.MAX_VALUE,(long)stronger+bonus);
    }

    /** Register before the skill knowledge-hiding projection, which must retain the last word. */
    public static void register() {
        if(registered)return;
        registered=true;
        PolymerItemUtils.CONTEXT_ITEM_CHECK.register((stack,context)->needsProjection(stack));
        PolymerItemUtils.ITEM_MODIFICATION_EVENT.register((original,client,context)->{
            if(client.isEmpty() || !needsProjection(original))return client;
            var projected=client.copy();
            // Polymer already rendered the true server tooltip into lore and stored its real-stack
            // restoration metadata. Only native client components need their constructor-safe bound.
            sanitize(projected,DataComponents.ENCHANTMENTS);
            sanitize(projected,DataComponents.STORED_ENCHANTMENTS);
            return projected;
        });
    }

    private static boolean needsProjection(ItemInstance stack) {
        return numeric(stack.get(DataComponents.ENCHANTMENTS)) || numeric(stack.get(DataComponents.STORED_ENCHANTMENTS));
    }
    private static boolean numeric(ItemEnchantments enchantments) {
        return enchantments!=null && enchantments.entrySet().stream().anyMatch(entry->entry.getIntValue()>10);
    }
    private static boolean wide(ItemEnchantments enchantments) {
        return enchantments!=null && enchantments.entrySet().stream().anyMatch(entry->entry.getIntValue()>255);
    }
    private static void sanitize(ItemStack stack,net.minecraft.core.component.DataComponentType<ItemEnchantments> type) {
        var current=stack.get(type);
        if(!wide(current))return;
        var safe=new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        for(var entry:current.entrySet())safe.set(entry.getKey(),Math.min(entry.getIntValue(),255));
        stack.set(type,safe.toImmutable());
    }
}
