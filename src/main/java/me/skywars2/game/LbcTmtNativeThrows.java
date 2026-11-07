package me.skywars2.game;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Native snowball factories already own permission, aim, source capture, sound and debit. */
final class LbcTmtNativeThrows {
    private LbcTmtNativeThrows(){}
    static Item canonical(ItemStack stack){
        var color=LbcBalanceExtras.woolColor(stack);
        if(color!=null)return LbcWoolTrailItems.tokens().get(color);
        var thrown=LbcThrownUtilities.kind(stack);
        if(thrown!=null)return switch(thrown){
            case RUCHKA->LbcThrownUtilityItems.RUCHKA;
            case HONEW->LbcThrownUtilityItems.HONEW;
            case THROWABLE_METAL_PIPE->LbcThrownUtilityItems.THROWABLE_METAL_PIPE;
            case BUCKET->LbcThrownUtilityItems.BUCKET_PROJECTILE;
            case PUMPKIN->LbcThrownUtilityItems.THROWABLE_PUMPKIN;
        };
        return LbcBridgeBuilder.recognized(stack)?LbcBridgeBuilderItem.TOKEN:null;
    }
    static boolean matches(ItemStack stack){return canonical(stack)!=null;}
    static boolean wool(ItemStack stack){return LbcBalanceExtras.woolColor(stack)!=null;}
}
