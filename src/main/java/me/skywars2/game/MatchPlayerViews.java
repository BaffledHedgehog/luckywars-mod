package me.skywars2.game;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import me.skywars2.mixin.PlayerViewCheckpointAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetCameraPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.*;

/** Native cameras and explicitly supported block-storage sessions. Not a private FPV controller image. */
public final class MatchPlayerViews {
    public interface Title { Component skywars2$menuTitle(int id); }
    /** Component.copy shares mutable sibling nodes; checkpoint titles must not. */
    public static Component copyTitle(ServerPlayer player,Component title) {
        var ops=player.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
        var codec=net.minecraft.network.chat.ComponentSerialization.CODEC;
        return codec.parse(ops,codec.encodeStart(ops,title).getOrThrow()).getOrThrow();
    }
    private interface Binding { Container resolve(); }
    private record BlockBinding(ServerLevel level,BlockPos pos,BlockEntityType<?> type,Class<?> kind,int size) implements Binding {
        @Override public Container resolve() {
            if(!level.hasChunkAt(pos))throw new IllegalStateException("Storage destination is not resident");
            var entity=level.getBlockEntity(pos);
            if(entity==null||entity.isRemoved()||entity.getType()!=type||entity.getClass()!=kind
                    ||!(entity instanceof Container container)||container.getContainerSize()!=size)
                throw new IllegalStateException("Restore native storage block entities before player views: "+pos);
            return container;
        }
    }
    private record Pair(Binding first,Binding second) implements Binding {
        @Override public Container resolve(){return new CompoundContainer(first.resolve(),second.resolve());}
    }
    private record Drag(int type,int status,List<Integer> slots) {
        static Drag capture(AbstractContainerMenu menu) {
            var access=(PlayerViewCheckpointAccess.Drag)menu;
            var slots=access.skywars2$slots().stream().map(menu.slots::indexOf).toList();
            if(slots.stream().anyMatch(i->i<0))throw new IllegalStateException("Foreign slot in native drag state");
            return new Drag(access.skywars2$type(),access.skywars2$status(),slots);
        }
        void restore(AbstractContainerMenu menu) {
            var access=(PlayerViewCheckpointAccess.Drag)menu;access.skywars2$type(type);access.skywars2$status(status);access.skywars2$slots().clear();
            for(int i:slots)access.skywars2$slots().add(menu.getSlot(i));
        }
    }
    private record Storage(Class<?> kind,MenuType<?> type,int rows,Binding binding,Component title,ItemStack cursor) {
        AbstractContainerMenu create(int id,net.minecraft.world.entity.player.Inventory inventory,Container backing) {
            if(kind==ChestMenu.class)return new ChestMenu(type,id,inventory,backing,rows);
            if(kind==HopperMenu.class)return new HopperMenu(id,inventory,backing);
            if(kind==DispenserMenu.class)return new DispenserMenu(id,inventory,backing);
            throw new IllegalStateException("Unsupported native storage menu");
        }
    }
    private record View(UUID camera,Storage storage,Drag drag) { }
    private final GameController game;
    private final Map<UUID,View> images=new LinkedHashMap<>();

    MatchPlayerViews(GameController game) {
        this.game=game;
        for(var player:game.server().getPlayerList().getPlayers()) {
            validateMenu(player);
            images.put(player.getUUID(),new View(player.getCamera().getUUID(),storage(player),Drag.capture(player.containerMenu)));
        }
    }
    private static Binding binding(Container container) {return binding(container,true);}
    private static Binding binding(Container container,boolean current) {
        if(container.getClass()==CompoundContainer.class) {
            var pair=(PlayerViewCheckpointAccess.Compound)container;
            // Only the native two-block compound, never arbitrary nested/plugin graphs.
            if(pair.skywars2$first() instanceof CompoundContainer||pair.skywars2$second() instanceof CompoundContainer)
                throw new IllegalStateException("Nested storage requires its own checkpoint adapter");
            return new Pair(binding(pair.skywars2$first(),current),binding(pair.skywars2$second(),current));
        }
        var kind=container.getClass();
        if(kind!=ChestBlockEntity.class&&kind!=TrappedChestBlockEntity.class&&kind!=BarrelBlockEntity.class
                &&kind!=HopperBlockEntity.class&&kind!=DispenserBlockEntity.class&&kind!=DropperBlockEntity.class)
            throw new IllegalStateException("Storage backing requires its own checkpoint adapter: "+kind.getName());
        var entity=(BlockEntity)container;
        if(!(entity.getLevel() instanceof ServerLevel level)||(current&&(entity.isRemoved()||level.getBlockEntity(entity.getBlockPos())!=entity)))
            throw new IllegalStateException("Storage backing is not a current native block entity");
        return new BlockBinding(level,entity.getBlockPos().immutable(),entity.getType(),kind,container.getContainerSize());
    }
    private static Container backing(AbstractContainerMenu menu) {
        if(menu.getClass()==ChestMenu.class)return ((ChestMenu)menu).getContainer();
        if(menu.getClass()==HopperMenu.class)return ((PlayerViewCheckpointAccess.Hopper)menu).skywars2$container();
        if(menu.getClass()==DispenserMenu.class)return ((PlayerViewCheckpointAccess.Dispenser)menu).skywars2$container();
        throw new IllegalStateException("Menu requires its own checkpoint adapter: "+menu.getClass().getName());
    }
    static void validateMenu(ServerPlayer player) {
        if(player.containerMenu==player.inventoryMenu)return;
        binding(backing(player.containerMenu));
        ((Title)player).skywars2$menuTitle(player.containerMenu.containerId);
    }
    private static Storage storage(ServerPlayer player) {
        var menu=player.containerMenu;if(menu==player.inventoryMenu)return null;
        return new Storage(menu.getClass(),menu.getType(),menu instanceof ChestMenu chest?chest.getRowCount():0,
                binding(backing(menu)),((Title)player).skywars2$menuTitle(menu.containerId),menu.getCarried().copy());
    }
    static void closeFutureStorage(ServerPlayer player) {
        validateFutureMenu(player);
        if(player.containerMenu!=player.inventoryMenu){player.containerMenu.setCarried(ItemStack.EMPTY);player.closeContainer();}
    }
    private static void validateFutureMenu(ServerPlayer player) {
        if(player.containerMenu!=player.inventoryMenu)binding(backing(player.containerMenu),false);
    }
    /** Call before the terrain/body phases: closing a native viewer may schedule block events. */
    void prepareFrozen() {
        if(!game.server().isSameThread()||!game.server().tickRateManager().isFrozen())throw new IllegalStateException("Freeze native views before restore");
        for(var player:game.server().getPlayerList().getPlayers())validateFutureMenu(player);
        for(var player:game.server().getPlayerList().getPlayers()) {
            closeFutureStorage(player);
            // Retire future links before their actors disappear, including newcomers
            // with no saved image. Do not teleport their bodies as setCamera would.
            if(player.getCamera()!=player)publishCamera(player,player);
        }
    }
    void restore(Function<UUID,Entity> resolve,Set<UUID> pending) {
        // Resolve ALL supported backing blocks before changing any window.
        var backings=new LinkedHashMap<UUID,Container>();
        for(var entry:images.entrySet())if(resolve.apply(entry.getKey()) instanceof ServerPlayer player) {
            validateFutureMenu(player);var saved=entry.getValue();
            if(saved.storage!=null)backings.put(entry.getKey(),saved.storage.binding.resolve());
        }
        for(var entry:images.entrySet()) {
            if(!(resolve.apply(entry.getKey()) instanceof ServerPlayer player)){pending.add(entry.getKey());continue;}
            var saved=entry.getValue();closeFutureStorage(player);
            if(saved.storage!=null) {
                var storage=saved.storage;var backing=backings.get(entry.getKey());
                var opened=player.openMenu(new SimpleMenuProvider((id,inventory,p)->storage.create(id,inventory,backing),copyTitle(player,storage.title)));
                if(opened.isEmpty())throw new IllegalStateException("Native storage reopening failed");
                player.containerMenu.setCarried(storage.cursor.copy());
            }
            saved.drag.restore(player.containerMenu);player.containerMenu.sendAllDataToRemote();
        }
        restoreCameras(resolve,pending);
    }
    /** Chunk/entity publication continues while simulation is frozen. Retry this
     * phase after native tracking catches up, without reopening menus or bodies. */
    void restoreCameras(Function<UUID,Entity> resolve,Set<UUID> pending) {
        for(var entry:images.entrySet()) {
            if(!(resolve.apply(entry.getKey()) instanceof ServerPlayer player)){pending.add(entry.getKey());continue;}
            var saved=entry.getValue();
            var camera=resolve.apply(saved.camera);
            if(camera==null||camera.isRemoved()||camera.level()!=player.level()
                    ||(camera!=player&&(!player.level().getChunkSource().chunkMap.isChunkTracked(player,camera.chunkPosition().x(),camera.chunkPosition().z())
                    ||!net.fabricmc.fabric.api.networking.v1.PlayerLookup.tracking(camera).contains(player)))) {
                pending.add(saved.camera);camera=player;
            }
            // ServerPlayer.setCamera teleports the BODY to the camera. Body and view are
            // separate images here: publish the link without replaying that teleport.
            publishCamera(player,camera);
        }
    }
    private static void publishCamera(ServerPlayer player,Entity camera) {
        FullInvisibility.cameraChanging(player,camera);
        ((PlayerViewCheckpointAccess.Camera)player).skywars2$camera(camera);
        player.level().getChunkSource().move(player);
        player.connection.send(new ClientboundSetCameraPacket(camera));
        player.connection.resetPosition();
    }
}
