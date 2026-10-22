package me.skywars2.game;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.crafting.CraftingInput;

import java.util.ArrayList;
import java.util.List;

/** Retained machine, one normal Reforge per occupied donor slot, paid per successful donor. */
public final class LbcRerollCrafting {
    private LbcRerollCrafting() {}
    private static final String PREVIEW="skywars2_reroll_machine_preview";
    static final int MANA_PER_DONOR=5000;
    private static final net.minecraft.nbt.CompoundTag MACHINE_FLAG=new net.minecraft.nbt.CompoundTag();
    static { MACHINE_FLAG.putInt("reroll_machine",1); }

    public static ItemStack placeholder() {
        var stack=new ItemStack(Items.BARRIER);
        var tag=new net.minecraft.nbt.CompoundTag(); tag.putInt(PREVIEW,1);
        stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
        stack.set(DataComponents.CUSTOM_NAME,Component.translatable("lbckit.24"));
        stack.set(DataComponents.LORE,new ItemLore(List.of(Component.translatableWithFallback(
                "skywars2.crafting.reroll_machine.lore","The result is rolled on taking. Costs 5000 mana per transformed item."))));
        return stack;
    }
    public static boolean isPlaceholder(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr(PREVIEW,0)==1;
    }
    static boolean machine(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(LbcReforgeMachineItem.MACHINE)
                || stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).matchedBy(MACHINE_FLAG));
    }
    static List<String> tables(ItemStack donor) {
        if(donor.isEmpty() || machine(donor) || donor.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY)
                .copyTag().getIntOr("transformable",0)!=1) return List.of();
        return LbcBolts.reforgeTables(donor.copyWithCount(1),false);
    }
    public static boolean matches(CraftingInput input) {
        if(input.ingredientCount()<2 || input.ingredientCount()>9 || input.width()>3 || input.height()>3) return false;
        // Ordinary recipes must not allocate category maps for every occupied crafting slot.
        int machines=0;
        for(var stack:input.items()) if(machine(stack)) machines++;
        if(machines!=1) return false;
        for(var stack:input.items()) if(!stack.isEmpty() && !machine(stack) && tables(stack).isEmpty()) return false;
        return true;
    }
    static LbcReforgeCrafting.Roll roll(GameController game,ServerPlayer player,CraftingContainer grid) {
        var output=new ArrayList<ItemStack>();
        var consumed=new ArrayList<Integer>();
        for(int i=0;i<grid.getContainerSize();i++) {
            var donor=grid.getItem(i);
            if(donor.isEmpty() || machine(donor)) continue;
            // Legacy execute context is the crafting PLAYER, unlike Tiny's dropped donor context.
            var bundle=game.items().bolts().rollReforge(player,player.position().add(0,1,0),tables(donor));
            if(bundle.isEmpty()) continue;
            consumed.add(i);
            output.addAll(bundle);
        }
        return new LbcReforgeCrafting.Roll(output,consumed,consumed.size()*MANA_PER_DONOR);
    }
}
