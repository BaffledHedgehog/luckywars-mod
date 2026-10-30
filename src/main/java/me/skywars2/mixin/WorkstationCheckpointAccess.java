package me.skywars2.mixin;

import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SelectableRecipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.level.block.entity.BannerPattern;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Native workstation values; never invoke recipes, result pickup or input callbacks during replay. */
public final class WorkstationCheckpointAccess {
    private WorkstationCheckpointAccess() { }
    @Mixin(AbstractContainerMenu.class) public interface Data {
        @Accessor("dataSlots") List<DataSlot> skywars2$data();
    }
    @Mixin(TransientCraftingContainer.class) public interface Grid {
        @Accessor("items") NonNullList<ItemStack> skywars2$items();
    }
    @Mixin(ResultContainer.class) public interface Result {
        @Accessor("itemStacks") NonNullList<ItemStack> skywars2$items();
    }
    @Mixin(CraftingMenu.class) public interface Crafting {
        @Accessor("access") ContainerLevelAccess skywars2$access();
        @Accessor("placingRecipe") boolean skywars2$placing();
    }
    @Mixin(ItemCombinerMenu.class) public interface Combiner {
        @Accessor("access") ContainerLevelAccess skywars2$access();
    }
    @Mixin(AnvilMenu.class) public interface Anvil {
        @Accessor("repairItemCountCost") int skywars2$repair();
        @Accessor("repairItemCountCost") void skywars2$repair(int value);
        @Accessor("itemName") String skywars2$name();
        @Accessor("itemName") void skywars2$name(String value);
        @Accessor("onlyRenaming") boolean skywars2$renaming();
        @Accessor("onlyRenaming") void skywars2$renaming(boolean value);
    }
    @Mixin(GrindstoneMenu.class) public interface Grindstone {
        @Accessor("access") ContainerLevelAccess skywars2$access();
    }
    @Mixin(StonecutterMenu.class) public interface Stonecutter {
        @Accessor("access") ContainerLevelAccess skywars2$access();
        @Accessor("input") ItemStack skywars2$input();
        @Accessor("input") void skywars2$input(ItemStack value);
        @Accessor("recipesForInput") SelectableRecipe.SingleInputSet<StonecutterRecipe> skywars2$recipes();
        @Accessor("recipesForInput") void skywars2$recipes(SelectableRecipe.SingleInputSet<StonecutterRecipe> value);
        @Accessor("lastSoundTime") long skywars2$sound();
        @Accessor("lastSoundTime") void skywars2$sound(long value);
    }
    @Mixin(CartographyTableMenu.class) public interface Cartography {
        @Accessor("access") ContainerLevelAccess skywars2$access();
        @Accessor("lastSoundTime") long skywars2$sound();
        @Accessor("lastSoundTime") void skywars2$sound(long value);
    }
    @Mixin(LoomMenu.class) public interface Loom {
        @Accessor("access") ContainerLevelAccess skywars2$access();
        @Accessor("selectablePatterns") List<Holder<BannerPattern>> skywars2$patterns();
        @Accessor("selectablePatterns") void skywars2$patterns(List<Holder<BannerPattern>> value);
        @Accessor("lastSoundTime") long skywars2$sound();
        @Accessor("lastSoundTime") void skywars2$sound(long value);
    }
    @Mixin(EnchantmentMenu.class) public interface Enchantment {
        @Accessor("access") ContainerLevelAccess skywars2$access();
    }
    @Mixin(ResultSlot.class) public interface CraftResult {
        @Accessor("removeCount") int skywars2$removed();
        @Accessor("removeCount") void skywars2$removed(int value);
    }
}
