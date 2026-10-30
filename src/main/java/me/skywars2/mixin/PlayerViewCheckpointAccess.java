package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.Set;
import me.skywars2.game.MatchPlayerViews;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.CrafterMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.entity.CrafterBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;

/** Native view links and drag values only; storage contents belong to terrain/body owners. */
public final class PlayerViewCheckpointAccess {
    private PlayerViewCheckpointAccess() { }
    @Mixin(ServerPlayer.class) public interface Camera {
        @Accessor("camera") void skywars2$camera(Entity value);
    }
    @Mixin(ServerPlayer.class) public abstract static class Title implements MatchPlayerViews.Title {
        @Shadow private int containerCounter;
        @Unique private Component skywars2$title;
        @Unique private int skywars2$titleId=-1;
        @WrapOperation(method="openMenu",at=@At(value="INVOKE",target="Lnet/minecraft/world/MenuProvider;getDisplayName()Lnet/minecraft/network/chat/Component;"))
        private Component skywars2$remember(MenuProvider provider,Operation<Component> original) {
            var title=original.call(provider);skywars2$title=MatchPlayerViews.copyTitle((ServerPlayer)(Object)this,title);skywars2$titleId=containerCounter;return title;
        }
        @Override public Component skywars2$menuTitle(int id) {
            if(id!=skywars2$titleId||skywars2$title==null)throw new IllegalStateException("Storage screen was not opened through the native menu provider");
            return MatchPlayerViews.copyTitle((ServerPlayer)(Object)this,skywars2$title);
        }
    }
    @Mixin(AbstractContainerMenu.class) public interface Drag {
        @Accessor("quickcraftType") int skywars2$type();
        @Accessor("quickcraftType") void skywars2$type(int value);
        @Accessor("quickcraftStatus") int skywars2$status();
        @Accessor("quickcraftStatus") void skywars2$status(int value);
        @Accessor("quickcraftSlots") Set<Slot> skywars2$slots();
    }
    @Mixin(HopperMenu.class) public interface Hopper {
        @Accessor("hopper") Container skywars2$container();
    }
    @Mixin(DispenserMenu.class) public interface Dispenser {
        @Accessor("dispenser") Container skywars2$container();
    }
    @Mixin(CompoundContainer.class) public interface Compound {
        @Accessor("container1") Container skywars2$first();
        @Accessor("container2") Container skywars2$second();
    }
    @Mixin(ShulkerBoxMenu.class) public interface Shulker {
        @Accessor("container") Container skywars2$container();
    }
    @Mixin(PlayerEnderChestContainer.class) public interface Ender {
        @Accessor("activeChest") EnderChestBlockEntity skywars2$active();
    }
    @Mixin(AbstractFurnaceMenu.class) public interface Furnace {
        @Accessor("container") Container skywars2$container();
        @Accessor("data") ContainerData skywars2$data();
    }
    @Mixin(AbstractFurnaceBlockEntity.class) public interface FurnaceBlock {
        @Accessor("dataAccess") ContainerData skywars2$data();
    }
    @Mixin(BrewingStandMenu.class) public interface Brewing {
        @Accessor("brewingStand") Container skywars2$container();
        @Accessor("brewingStandData") ContainerData skywars2$data();
    }
    @Mixin(BrewingStandBlockEntity.class) public interface BrewingBlock {
        @Accessor("dataAccess") ContainerData skywars2$data();
    }
    @Mixin(CrafterMenu.class) public interface Crafter {
        @Accessor("container") CraftingContainer skywars2$container();
        @Accessor("containerData") ContainerData skywars2$data();
    }
    @Mixin(CrafterBlockEntity.class) public interface CrafterBlock {
        @Accessor("containerData") ContainerData skywars2$data();
    }
}
