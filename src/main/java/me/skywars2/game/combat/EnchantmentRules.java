package me.skywars2.game.combat;

import eu.pb4.polymer.core.api.item.PolymerItemUtils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import me.skywars2.game.items.ItemData;
import me.skywars2.game.items.wands.LbcWands;
import me.skywars2.game.items.weapons.melee.LbcSwordTools;
import me.skywars2.game.presentation.LbcArmorTooltips;

/** Server enchantment levels stay authoritative; vanilla clients receive safe component payloads. */
public final class EnchantmentRules {
    private static boolean registered;
    private static final ThreadLocal<Boolean> TMT_TABLE=new ThreadLocal<>();
    private static final net.minecraft.world.item.enchantment.Enchantable CUSTOM_ENCHANTABLE=new net.minecraft.world.item.enchantment.Enchantable(1);
    private EnchantmentRules() { }
    public static WeaponEnchantments.Scope table(ItemStack stack){
        var previous=TMT_TABLE.get();TMT_TABLE.set(tmt(stack));
        return ()->{if(previous==null)TMT_TABLE.remove();else TMT_TABLE.set(previous);};
    }
    public static int tableCostOffset(net.minecraft.world.item.enchantment.Enchantment enchantment){
        return Boolean.TRUE.equals(TMT_TABLE.get())?Math.max(0,enchantment.getMinCost(1)-30):0;
    }

    public static boolean tmt(ItemStack stack){return !stack.isEmpty()&&ItemData.intValue(stack,"tmt_item",0)==1;}
    /** Gameplay identity, not the client's carrot/stick representation, decides weapon eligibility. */
    public static boolean weapon(ItemStack stack){
        if(stack.isEmpty())return false;
        if(LbcSwordTools.sword(stack)||stack.getItem() instanceof LbcWands.NativeTool
                ||ItemData.intValue(stack,"magicwand",0)==1||ItemData.intValue(stack,"magnum",0)==1)return true;
        var attributes=stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        return attributes!=null&&attributes.modifiers().stream().anyMatch(entry->entry.attribute().equals(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)
                &&entry.modifier().amount()>0);
    }
    public static void prepare(ItemStack stack){
        if(stack.isEmpty()||!stack.has(DataComponents.CUSTOM_DATA)||!tmt(stack)&&!weapon(stack))return;
        if(!stack.has(DataComponents.ENCHANTABLE))stack.set(DataComponents.ENCHANTABLE,CUSTOM_ENCHANTABLE);
        if(!stack.has(DataComponents.ENCHANTMENTS))stack.set(DataComponents.ENCHANTMENTS,ItemEnchantments.EMPTY);
    }
    public static boolean supports(ItemStack stack,net.minecraft.core.HolderSet<net.minecraft.world.item.Item> accepted,
                                   net.minecraft.world.item.enchantment.Enchantment enchantment){
        if(tmt(stack))return true;
        if(!weapon(stack))return false;
        var effects=enchantment.effects();
        if(!LbcSwordTools.sword(stack)&&!stack.isDamageableItem()&&(effects.has(net.minecraft.world.item.enchantment.EnchantmentEffectComponents.ITEM_DAMAGE)
                ||effects.has(net.minecraft.world.item.enchantment.EnchantmentEffectComponents.REPAIR_WITH_XP)))return false;
        if(!LbcSwordTools.sword(stack)&&enchantment.getEffects(net.minecraft.world.item.enchantment.EnchantmentEffectComponents.ATTRIBUTES).stream()
                .anyMatch(effect->effect.attribute().equals(net.minecraft.world.entity.ai.attributes.Attributes.SWEEPING_DAMAGE_RATIO)))return false;
        return accepted.contains(net.minecraft.world.item.Items.IRON_SWORD.builtInRegistryHolder());
    }

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
            if(LbcArmorTooltips.aimbobBoots(original))LbcArmorTooltips.groupFeet(projected);
            return projected;
        });
    }

    private static boolean needsProjection(ItemInstance stack) {
        return numeric(stack.get(DataComponents.ENCHANTMENTS)) || numeric(stack.get(DataComponents.STORED_ENCHANTMENTS))
                ||LbcArmorTooltips.aimbobBoots(stack);
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
