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
