package me.skywars2.game;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.crafting.CraftingInput;

import java.util.ArrayList;
import java.util.List;

/** One forced Ultra roll per accepted result action; preview never rolls loot or chance. */
public final class LbcTinyReforge {
    private LbcTinyReforge() {}
    private static final String PREVIEW="skywars2_tiny_reforge_preview";

    public static ItemStack placeholder() {
        var stack=new ItemStack(Items.BARRIER);
        var tag=new net.minecraft.nbt.CompoundTag(); tag.putInt(PREVIEW,1);
        stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
        stack.set(DataComponents.CUSTOM_NAME,Component.translatableWithFallback("skywars2.crafting.tiny_reforge","Random Reforge"));
        stack.set(DataComponents.LORE,new ItemLore(List.of(Component.translatableWithFallback("skywars2.crafting.tiny_reforge.lore","The result is rolled on taking. 30% chance to consume the wand."))));
        return stack;
    }
    public static boolean isPlaceholder(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getIntOr(PREVIEW,0)==1;
    }
    private static boolean tiny(ItemStack stack) {
        return stack.is(LbcWands.TINY_TELEPORT) || stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY)
                .copyTag().getIntOr("stteleport_tiny",0)==1;
    }
    static List<String> tables(ItemStack donor) {
        if(donor.isEmpty() || tiny(donor) || donor.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY)
                .copyTag().getIntOr("transformable",0)!=1) return List.of();
        // Unit count deliberately prevents the wand's exact-64 arrow exception.
        return LbcBolts.reforgeTables(donor.copyWithCount(1),true);
    }
    public static boolean matches(CraftingInput input) {
        if(input.ingredientCount()!=2) return false;
        int tiny=0,donor=0;
        for(var stack:input.items()) if(!stack.isEmpty()) {
            if(tiny(stack)) tiny++;
            else if(!tables(stack).isEmpty()) donor++;
            else return false;
        }
        return tiny==1 && donor==1;
    }
    static LbcReforgeCrafting.Roll roll(GameController game,ServerPlayer player,CraftingContainer grid) {
        int tinySlot=-1,donorSlot=-1;
        for(int i=0;i<grid.getContainerSize();i++) if(!grid.getItem(i).isEmpty()) {
            if(tiny(grid.getItem(i))) tinySlot=i; else donorSlot=i;
        }
        var donor=grid.getItem(donorSlot).copyWithCount(1);
        var context=new ItemEntity(player.level(),player.getX(),player.getY(),player.getZ(),donor);
        var output=game.items().bolts().rollReforge(context,tables(donor));
        var consumed=new ArrayList<Integer>();
        if(!output.isEmpty()) {
            consumed.add(donorSlot);
            if(player.level().getRandom().nextFloat()<.3f) consumed.add(tinySlot);
        }
        return new LbcReforgeCrafting.Roll(output,consumed,0);
    }
    /** Compatibility entry point; all random crafts use the shared transaction. */
    public static boolean handleClick(AbstractContainerMenu menu,int slotIndex,int button,ContainerInput action,ServerPlayer player) {
        return LbcReforgeCrafting.handleClick(menu,slotIndex,button,action,player);
    }
}
