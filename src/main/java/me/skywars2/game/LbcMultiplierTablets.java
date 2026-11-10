package me.skywars2.game;

import java.util.ArrayList;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/** The Paper tablet payload operates on native components, without command/NBT scratch storage. */
public final class LbcMultiplierTablets {
    private LbcMultiplierTablets(){}
    public static int kind(ItemStack stack){return ItemData.intValue(stack,"multiply",0);}
    /** Shared ground/crafting payload. An empty result is a true no-op and spends no tablet. */
    static ItemStack amplified(ItemStack donor,int kind){
        var changed=donor.copy();
        if(kind==2){
            var before=donor.getOrDefault(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS,ItemAttributeModifiers.EMPTY);
            var after=doubled(before);if(after.equals(before))return ItemStack.EMPTY;
            changed.set(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS,after);
        }else if(kind==4){
            var before=donor.getOrDefault(net.minecraft.core.component.DataComponents.ENCHANTMENTS,ItemEnchantments.EMPTY);
            var after=quadrupled(before);if(after.equals(before))return ItemStack.EMPTY;
            changed.set(net.minecraft.core.component.DataComponents.ENCHANTMENTS,after);
        }else if(kind!=3)return ItemStack.EMPTY;
        return changed;
    }
    static ItemAttributeModifiers doubled(ItemAttributeModifiers source){
        var entries=new ArrayList<ItemAttributeModifiers.Entry>(source.modifiers().size());
        for(var entry:source.modifiers()){
            var old=entry.modifier();double amount=old.amount()*2;
            // Keep even extreme externally-authored attributes serializable after amplification.
            if(!Double.isFinite(amount))amount=Math.copySign(Double.MAX_VALUE,amount);
            entries.add(new ItemAttributeModifiers.Entry(entry.attribute(),new AttributeModifier(old.id(),amount,old.operation()),entry.slot(),entry.display()));
        }
        return new ItemAttributeModifiers(entries);
    }
    static ItemEnchantments quadrupled(ItemEnchantments source){
        var changed=new ItemEnchantments.Mutable(source);
        for(var entry:source.entrySet())changed.set(entry.getKey(),(int)Math.min(Integer.MAX_VALUE,entry.getIntValue()*4L));
        return changed.toImmutable();
    }
}
