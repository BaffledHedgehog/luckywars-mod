package me.skywars2.game.items.tools;

import me.skywars2.game.GameController;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;

/** Whole-stack tablet operations; previews are pure, native ResultSlot owns the actual debit. */
public final class LbcMultiplierCrafting {
    private LbcMultiplierCrafting(){}
    public static int donor(CraftingInput input){
        if(input.ingredientCount()!=2)return -1;
        int donor=-1,tablet=-1;
        for(int i=0;i<input.size();i++){
            var item=input.getItem(i);if(item.isEmpty())continue;
            int kind=LbcMultiplierTablets.kind(item);
            if(kind>=2&&kind<=4){if(tablet>=0)return -1;tablet=i;}
            else {if(donor>=0)return -1;donor=i;}
        }
        return donor>=0&&tablet>=0?donor:-1;
    }
    public static int kind(CraftingInput input){
        int donor=donor(input);if(donor<0)return 0;
        for(int i=0;i<input.size();i++)if(i!=donor&&!input.getItem(i).isEmpty())return LbcMultiplierTablets.kind(input.getItem(i));
        return 0;
    }
    static ItemStack result(CraftingInput input){
        int donor=donor(input);if(donor<0)return ItemStack.EMPTY;
        var result=LbcMultiplierTablets.amplified(input.getItem(donor),kind(input));
        if(kind(input)==3)result.setCount((int)Math.min(result.getMaxStackSize(),input.getItem(donor).getCount()*2L));
        return result;
    }
    static NonNullList<ItemStack> remaining(CraftingInput input){
        var result=NonNullList.withSize(input.size(),ItemStack.EMPTY);
        if(kind(input)==3)result.set(donor(input),input.getItem(donor(input)).copyWithCount(1));
        return result;
    }
    /** x2/x4 transform the full original stack, rather than leaving N-1 unmodified units behind. */
    public static int wholeDonor(CraftingInput input){int kind=kind(input);return kind==2||kind==4?donor(input):-1;}
    static void taken(GameController game,ServerPlayer player,CraftingInput input){
        if(kind(input)!=3)return;
        var donor=input.getItem(donor(input));
        int overflow=(int)Math.min(Integer.MAX_VALUE,Math.max(0,donor.getCount()*2L-result(input).getCount()));
        if(overflow==0)return;
        var extra=new ItemEntity(player.level(),player.getX(),player.getY(),player.getZ(),donor.copyWithCount(overflow));
        extra.setPickUpDelay(0);extra.setThrower(player);game.items().markCurrentItemEpoch(extra);
        if(player.level().addFreshEntity(extra))extra.playerTouch(player);
    }
}
