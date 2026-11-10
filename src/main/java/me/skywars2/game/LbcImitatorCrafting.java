package me.skywars2.game;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;

/** Copies the whole donor stack, returning the one donor unit vanilla temporarily consumes. */
final class LbcImitatorCrafting {
    private LbcImitatorCrafting(){}
    static int donor(CraftingInput input){
        if(input.ingredientCount()!=2)return -1;
        int copier=-1,donor=-1;
        for(int slot=0;slot<input.size();slot++){
            var stack=input.getItem(slot);if(stack.isEmpty())continue;
            if(LbcLooseRewards.copier(stack)){if(copier>=0)return -1;copier=slot;}
            else {if(donor>=0)return -1;donor=slot;}
        }
        return copier<0?-1:donor;
    }
    static ItemStack result(CraftingInput input){int slot=donor(input);return slot<0?ItemStack.EMPTY:LbcLooseRewards.copyStack(input.getItem(slot));}
    static NonNullList<ItemStack> remaining(CraftingInput input){
        var result=NonNullList.withSize(input.size(),ItemStack.EMPTY);int slot=donor(input);
        if(slot>=0)result.set(slot,input.getItem(slot).copyWithCount(1));return result;
    }
}
