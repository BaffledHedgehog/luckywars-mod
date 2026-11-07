package me.skywars2.game;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Native utility factories own permission, source capture and debit, including immediate RMB consumables. */
final class LbcTmtNativeThrows {
    private LbcTmtNativeThrows(){}
    static Item canonical(ItemStack stack){
        if(LbcGayradar.matches(stack))return LbcGayradar.TOKEN;
        if(LbcSteampunkRocket.matches(stack))return LbcSteampunkRocket.TOKEN;
        if(LbcTrollBox.matches(stack))return LbcTrollBox.TOKEN;
        if(LbcClockFragments.duration(stack)>0)return LbcClockFragments.ITEM;
        if(LbcKillButton.matches(stack))return LbcKillButton.TOKEN;
        if(LbcBucksTrader.matches(stack))return LbcBucksTrader.TOKEN;
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
