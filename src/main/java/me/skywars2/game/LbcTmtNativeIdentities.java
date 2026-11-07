package me.skywars2.game;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/** Normalize only audited native identities into their existing owner's source predicates. */
final class LbcTmtNativeIdentities {
    private LbcTmtNativeIdentities(){}

    /** Mutates the catalog's private copy; never the native prototype or a physical inventory item. */
    static void normalize(ItemStack source,CompoundTag privateData){
        if(source.isEmpty())return;
        // Both native owners already accept their registered item OR this exact flag.
        if(source.is(LbcItems.ICE_SWORD))privateData.putInt("icesword",1);
        if(source.is(LbcItems.ROCKET_BOOTS))privateData.putInt("explosive_feets",1);
        // Registration intentionally leaves presentation/flags to the four ported loot leaves.
        // Their source tags contain overload:1, never mage:1. The mana owner counts actual slots.
        if(source.is(LbcOverload.HELMET)||source.is(LbcOverload.CHESTPLATE)
                ||source.is(LbcOverload.LEGGINGS)||source.is(LbcOverload.BOOTS))privateData.putInt("overload",1);
    }

    /** This is a native ShieldItem/component contract, not the hylian sword's passive flag. */
    static boolean hylianShield(ItemStack source){
        return !source.isEmpty()&&(source.is(LbcMeleeItems.Kind.HYLIAN_SHIELD.item)
                ||ItemData.intValue(source,"hylianshield",0)==1);
    }
}
