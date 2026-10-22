package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Shared validation, commit and lossless delivery for forced random crafting. */
public final class LbcReforgeCrafting {
    private LbcReforgeCrafting() {}
    /** Rolls contain only committed rewards and slots whose single units must be consumed. */
    record Roll(List<ItemStack> output,List<Integer> consumedSlots,int manaCost) {}
    private static boolean random(LbcCraftingRecipe.Kind kind) {
        return kind==LbcCraftingRecipe.Kind.TINY_REFORGE || kind==LbcCraftingRecipe.Kind.REROLL_MACHINE;
    }
    private static ItemStack placeholder(LbcCraftingRecipe.Kind kind) {
        return kind==LbcCraftingRecipe.Kind.TINY_REFORGE ? LbcTinyReforge.placeholder() : LbcRerollCrafting.placeholder();
    }
    private static boolean isPlaceholder(ItemStack stack) {
        return LbcTinyReforge.isPlaceholder(stack) || LbcRerollCrafting.isPlaceholder(stack);
    }
    private static boolean eligible(GameController game,ServerPlayer player,AbstractContainerMenu menu) {
        return game!=null && game.playing() && game.canUseGameplayItems(player) && player.isAlive()
                && !player.isRemoved() && !player.isSpectator() && !game.isObserver(player)
                && !game.awaitingRespawn(player.getUUID()) && player.level()==game.maps().level()
                && !game.items().timeStop().stopped(player.level()) && player.containerMenu==menu
                && menu.stillValid(player);
    }

    /** Returns true for every action on this recipe, including actions rejected before RNG. */
    public static boolean handleClick(AbstractContainerMenu menu,int slotIndex,int button,ContainerInput action,ServerPlayer player) {
        if(slotIndex!=0 || !(menu instanceof InventoryMenu || menu instanceof CraftingMenu)) return false;
        var craftingMenu=(AbstractCraftingMenu)menu;
        var gridSlots=craftingMenu.getInputGridSlots();
        if(gridSlots.isEmpty() || !(gridSlots.getFirst().container instanceof CraftingContainer grid)) return false;
        var input=grid.asCraftInput();
        var kind=LbcCraftingRecipe.craftingKind(input);
        boolean recipe=random(kind);
        if(!recipe && !isPlaceholder(craftingMenu.getResultSlot().getItem())) return false;
        var game=SkyWars2.game();
        if(!recipe || !eligible(game,player,menu)) { menu.sendAllDataToRemote(); return true; }
        boolean allowed=switch(action) {
            case PICKUP -> (button==0 || button==1) && menu.getCarried().isEmpty();
            case QUICK_MOVE -> button==0 || button==1;
            case SWAP -> (button>=0 && button<9 || button==40) && player.getInventory().getItem(button).isEmpty();
            case THROW -> (button==0 || button==1) && menu.getCarried().isEmpty() && player.canDropItems();
            default -> false;
        };
        if(!allowed) { menu.sendAllDataToRemote(); return true; }
        var before=new ArrayList<ItemStack>();
        for(int i=0;i<grid.getContainerSize();i++) before.add(grid.getItem(i).copy());
        String epoch=game.items().epoch();
        // No asynchronous work, native onTake or output-dependent capacity check follows the roll.
        if(!eligible(game,player,menu) || !epoch.equals(game.items().epoch())) { menu.sendAllDataToRemote(); return true; }
        for(int i=0;i<before.size();i++) if(!ItemStack.matches(before.get(i),grid.getItem(i))) {
            menu.sendAllDataToRemote(); return true;
        }
        var roll=kind==LbcCraftingRecipe.Kind.TINY_REFORGE
                ? LbcTinyReforge.roll(game,player,grid) : LbcRerollCrafting.roll(game,player,grid);
        var output=roll.output();
        if(output.isEmpty()) { menu.sendAllDataToRemote(); return true; }
        // All loot was accepted before rolling; no output-dependent capacity veto is possible.
        for(int slot:roll.consumedSlots()) grid.getItem(slot).shrink(1);
        if(roll.manaCost()!=0) game.items().mana().add(player,-roll.manaCost());
        for(var result:output) result.onCraftedBy(player,result.getCount());
        var primary=output.removeFirst();
        switch(action) {
            case PICKUP -> menu.setCarried(primary);
            case SWAP -> player.getInventory().setItem(button,primary);
            case QUICK_MOVE -> deliver(player,primary);
            case THROW -> drop(player,primary);
            default -> throw new IllegalStateException("Unvalidated random reforge action");
        }
        for(var extra:output) { if(action==ContainerInput.THROW) drop(player,extra); else deliver(player,extra); }
        if(craftingMenu.getResultSlot().container instanceof net.minecraft.world.inventory.RecipeCraftingHolder holder)
            holder.awardUsedRecipes(player,before);
        if(kind==LbcCraftingRecipe.Kind.TINY_REFORGE)
            game.items().mana().award(player,"lbc:true_advancements/lightning_smith_ultra");
        else game.items().mana().award(player,"lbc:true_advancements/lightning_smith");
        player.level().sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,true,false,
                player.getX(),player.getY()+.2,player.getZ(),100,1,1,1,1);
        LbcOverload.sound(player,player.position(),"minecraft:block.enchantment_table.use",1,1,0);
        // Clear the spent preview even when a synthetic menu has no ContainerLevelAccess.
        craftingMenu.getResultSlot().set(ItemStack.EMPTY);
        grid.setChanged();
        menu.slotsChanged(grid);
        var remainingKind=LbcCraftingRecipe.craftingKind(grid.asCraftInput());
        if(random(remainingKind)) craftingMenu.getResultSlot().set(placeholder(remainingKind));
        player.getInventory().setChanged();
        menu.broadcastChanges();
        menu.sendAllDataToRemote();
        return true;
    }
    private static void deliver(ServerPlayer player,ItemStack stack) {
        if(!player.getInventory().add(stack) && !stack.isEmpty()) drop(player,stack);
    }
    private static void drop(ServerPlayer player,ItemStack stack) {
        // Explicit overflow policy: committed rewards always survive a full inventory.
        var entity=new ItemEntity(player.level(),player.getX(),player.getY(),player.getZ(),stack);
        entity.setTarget(player.getUUID());
        entity.setDefaultPickUpDelay();
        player.level().addFreshEntity(entity);
    }
}
