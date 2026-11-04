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
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.inventory.BlastFurnaceMenu;
import net.minecraft.world.inventory.SmokerMenu;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.CrafterMenu;
import net.minecraft.world.inventory.ContainerData;
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
    private interface Binding {
        Container resolve(ServerPlayer player);
        default Container resolve(ServerPlayer player,Function<UUID,Entity> actors){return resolve(player);}
        default void activate(ServerPlayer player) { }
    }
    private record EntityBinding(ServerLevel level,UUID id,Class<?> kind,int size) implements Binding {
        @Override public Container resolve(ServerPlayer player){return resolve(player,level::getEntity);}
        @Override public Container resolve(ServerPlayer player,Function<UUID,Entity> actors){
            var entity=actors.apply(id);
            if(entity==null||entity.isRemoved()||entity.level()!=level||entity.getClass()!=kind
                    ||!(entity instanceof net.minecraft.world.entity.vehicle.ContainerEntity container)||container.getContainerSize()!=size
                    ||container.getContainerLootTable()!=null)throw new IllegalStateException("Restore native storage actor before view: "+id);
            return container;
        }
    }
    private record TeamBinding(LbcBedwarsAuxiliaries.SharedContainer container) implements Binding {
        @Override public Container resolve(ServerPlayer player){container.validate(player,true);return container;}
    }
    private record BlockBinding(ServerLevel level,BlockPos pos,BlockEntityType<?> type,Class<?> kind,int size) implements Binding {
        @Override public Container resolve(ServerPlayer player) {
            if(!level.hasChunkAt(pos))throw new IllegalStateException("Storage destination is not resident");
            var entity=level.getBlockEntity(pos);
            if(entity==null||entity.isRemoved()||entity.getType()!=type||entity.getClass()!=kind
                    ||!(entity instanceof Container container)||container.getContainerSize()!=size)
                throw new IllegalStateException("Restore native storage block entities before player views: "+pos);
            return container;
        }
    }
    private record Pair(Binding first,Binding second) implements Binding {
        @Override public Container resolve(ServerPlayer player){return new CompoundContainer(first.resolve(player),second.resolve(player));}
    }
    private record EnderBinding(UUID owner,ServerLevel level,BlockPos pos) implements Binding {
        private EnderChestBlockEntity chest(ServerPlayer player) {
            if(!player.getUUID().equals(owner)||!level.hasChunkAt(pos)
                    ||!(level.getBlockEntity(pos) instanceof EnderChestBlockEntity chest)||chest.isRemoved())
                throw new IllegalStateException("Restore the owning player and native ender chest before views: "+pos);
            return chest;
        }
        @Override public Container resolve(ServerPlayer player){chest(player);return player.getEnderChestInventory();}
        @Override public void activate(ServerPlayer player){player.getEnderChestInventory().setActiveChest(chest(player));}
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
        boolean matches(ServerPlayer player) {
            if(player.containerMenu==player.inventoryMenu)return false;
            try {
                validateMenu(player);var current=storage(player);
                return current!=null&&current.kind==kind&&current.type==type&&current.rows==rows&&current.binding.equals(binding)&&current.title.equals(title)
                        &&player.containerMenu.slots.stream().filter(s->s.container==player.getInventory()).count()==36
                        &&player.containerMenu.slots.stream().noneMatch(s->s.container instanceof net.minecraft.world.entity.player.Inventory&&s.container!=player.getInventory());
            }catch(IllegalStateException unsupported){return false;}
        }
        AbstractContainerMenu create(int id,net.minecraft.world.entity.player.Inventory inventory,Container backing) {
            if(kind==ChestMenu.class)return new ChestMenu(type,id,inventory,backing,rows);
            if(kind==HopperMenu.class)return new HopperMenu(id,inventory,backing);
            if(kind==DispenserMenu.class)return new DispenserMenu(id,inventory,backing);
            if(kind==ShulkerBoxMenu.class)return new ShulkerBoxMenu(id,inventory,backing);
            if(kind==FurnaceMenu.class)return new FurnaceMenu(id,inventory,backing,blockData(backing));
            if(kind==BlastFurnaceMenu.class)return new BlastFurnaceMenu(id,inventory,backing,blockData(backing));
            if(kind==SmokerMenu.class)return new SmokerMenu(id,inventory,backing,blockData(backing));
            if(kind==BrewingStandMenu.class)return new BrewingStandMenu(id,inventory,backing,blockData(backing));
            if(kind==CrafterMenu.class)return new CrafterMenu(id,inventory,(CrafterBlockEntity)backing,blockData(backing));
            throw new IllegalStateException("Unsupported native storage menu");
        }
    }
    private record Workshop(MatchWorkstations image,Component title,ItemStack cursor) { }
    private record View(UUID camera,Storage storage,Workshop workshop,MatchBoundViews bound,Drag drag) { }
    private final GameController game;
    private final Map<UUID,View> images=new LinkedHashMap<>();

    MatchPlayerViews(GameController game) {
        this.game=game;
        for(var player:game.server().getPlayerList().getPlayers()) {
            validateMenu(player);
            var menu=player.containerMenu;
            var workshop=MatchWorkstations.supports(menu)?new Workshop(MatchWorkstations.capture(player),
                    ((Title)player).skywars2$menuTitle(menu.containerId),menu.getCarried().copy()):null;
            var bound=MatchBoundViews.supports(menu)?new MatchBoundViews(player):null;
            images.put(player.getUUID(),new View(player.getCamera().getUUID(),storage(player),workshop,bound,Drag.capture(menu)));
        }
    }
    private static Binding binding(ServerPlayer player,Container container) {return binding(player,container,true);}
    private static Binding binding(ServerPlayer player,Container container,boolean current) {
        if(container instanceof LbcBedwarsAuxiliaries.SharedContainer team){team.validate(player,current);return new TeamBinding(team);}

        if(container instanceof net.minecraft.world.entity.vehicle.ContainerEntity vehicle&&container instanceof Entity entity){
            if(!(entity.level() instanceof ServerLevel level)||(current&&(entity.isRemoved()||level.getEntity(entity.getUUID())!=entity||vehicle.getContainerLootTable()!=null)))
                throw new IllegalStateException("Storage vehicle is not current native unpacked inventory");
            return new EntityBinding(level,entity.getUUID(),entity.getClass(),vehicle.getContainerSize());
        }
        if(container.getClass()==PlayerEnderChestContainer.class) {
            if(container!=player.getEnderChestInventory())throw new IllegalStateException("Ender storage belongs to a different player body");
            var chest=((PlayerViewCheckpointAccess.Ender)container).skywars2$active();
            // A future native ender session may have lost its block, but closing it is safe.
            if(!current)return null;
            if(chest==null||chest.getClass()!=EnderChestBlockEntity.class||chest.isRemoved()
                    ||!(chest.getLevel() instanceof ServerLevel level)||level.getBlockEntity(chest.getBlockPos())!=chest)
                throw new IllegalStateException("Ender screen needs a current native active chest");
            return new EnderBinding(player.getUUID(),level,chest.getBlockPos().immutable());
        }
        if(container.getClass()==CompoundContainer.class) {
            var pair=(PlayerViewCheckpointAccess.Compound)container;
            // Only the native two-block compound, never arbitrary nested/plugin graphs.
            if(pair.skywars2$first() instanceof CompoundContainer||pair.skywars2$second() instanceof CompoundContainer)
                throw new IllegalStateException("Nested storage requires its own checkpoint adapter");
            if(pair.skywars2$first() instanceof PlayerEnderChestContainer||pair.skywars2$second() instanceof PlayerEnderChestContainer)
                throw new IllegalStateException("Ender storage cannot be a compound container");
            if(pair.skywars2$first() instanceof Entity||pair.skywars2$second() instanceof Entity)throw new IllegalStateException("Compound actor storage needs its own adapter");
            return new Pair(binding(player,pair.skywars2$first(),current),binding(player,pair.skywars2$second(),current));
        }
        var kind=container.getClass();
        if(kind!=ChestBlockEntity.class&&kind!=TrappedChestBlockEntity.class&&kind!=BarrelBlockEntity.class
                &&kind!=HopperBlockEntity.class&&kind!=DispenserBlockEntity.class&&kind!=DropperBlockEntity.class
                &&kind!=ShulkerBoxBlockEntity.class&&kind!=FurnaceBlockEntity.class&&kind!=BlastFurnaceBlockEntity.class
                &&kind!=SmokerBlockEntity.class&&kind!=BrewingStandBlockEntity.class&&kind!=CrafterBlockEntity.class)
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
        if(menu.getClass()==ShulkerBoxMenu.class)return ((PlayerViewCheckpointAccess.Shulker)menu).skywars2$container();
        if(menu.getClass()==FurnaceMenu.class||menu.getClass()==BlastFurnaceMenu.class||menu.getClass()==SmokerMenu.class)
            return ((PlayerViewCheckpointAccess.Furnace)menu).skywars2$container();
        if(menu.getClass()==BrewingStandMenu.class)return ((PlayerViewCheckpointAccess.Brewing)menu).skywars2$container();
        if(menu.getClass()==CrafterMenu.class)return ((PlayerViewCheckpointAccess.Crafter)menu).skywars2$container();
        throw new IllegalStateException("Menu requires its own checkpoint adapter: "+menu.getClass().getName());
    }
    static void validateMenu(ServerPlayer player) {
        if(player.containerMenu==player.inventoryMenu)return;
        if(MatchBoundViews.supports(player.containerMenu)){MatchBoundViews.validate(player,true);return;}
        if(MatchWorkstations.supports(player.containerMenu)){
            MatchWorkstations.validate(player);((Title)player).skywars2$menuTitle(player.containerMenu.containerId);return;
        }
        var menu=player.containerMenu;var container=backing(menu);binding(player,container);
        if(menu.getClass()==ShulkerBoxMenu.class&&container.getClass()!=ShulkerBoxBlockEntity.class)
            throw new IllegalStateException("Shulker menu must use a native shulker box");
        if(menu instanceof AbstractFurnaceMenu furnace) {
            var expected=menu.getClass()==FurnaceMenu.class?FurnaceBlockEntity.class:menu.getClass()==BlastFurnaceMenu.class?BlastFurnaceBlockEntity.class:SmokerBlockEntity.class;
            if(container.getClass()!=expected||((PlayerViewCheckpointAccess.Furnace)furnace).skywars2$data()!=blockData(container))
                throw new IllegalStateException("Furnace menu must use its native block data");
        }
        if(menu.getClass()==BrewingStandMenu.class&&(container.getClass()!=BrewingStandBlockEntity.class
                ||((PlayerViewCheckpointAccess.Brewing)menu).skywars2$data()!=blockData(container)))
            throw new IllegalStateException("Brewing menu must use its native block data");
        if(menu.getClass()==CrafterMenu.class&&(container.getClass()!=CrafterBlockEntity.class
                ||((PlayerViewCheckpointAccess.Crafter)menu).skywars2$data()!=blockData(container)))
            throw new IllegalStateException("Crafter menu must use its native block data");
        ((Title)player).skywars2$menuTitle(player.containerMenu.containerId);
    }
    private static Storage storage(ServerPlayer player) {
        var menu=player.containerMenu;if(menu==player.inventoryMenu||MatchWorkstations.supports(menu)||MatchBoundViews.supports(menu))return null;
        return new Storage(menu.getClass(),menu.getType(),menu instanceof ChestMenu chest?chest.getRowCount():0,
                binding(player,backing(menu)),((Title)player).skywars2$menuTitle(menu.containerId),menu.getCarried().copy());
    }
    static void closeFutureStorage(ServerPlayer player) {
        validateFutureMenu(player);
        if(player.containerMenu!=player.inventoryMenu){MatchWorkstations.discard(player.containerMenu);MatchBoundViews.discard(player.containerMenu);player.containerMenu.setCarried(ItemStack.EMPTY);player.closeContainer();}
    }
    private static void validateFutureMenu(ServerPlayer player) {
        if(MatchBoundViews.supports(player.containerMenu)){MatchBoundViews.validate(player,false);return;}
        if(MatchWorkstations.supports(player.containerMenu)){MatchWorkstations.validateFuture(player);return;}
        if(player.containerMenu!=player.inventoryMenu)binding(player,backing(player.containerMenu),false);
    }
    static ContainerData blockData(Container container) {
        if(container instanceof AbstractFurnaceBlockEntity)return ((PlayerViewCheckpointAccess.FurnaceBlock)container).skywars2$data();
        if(container instanceof BrewingStandBlockEntity)return ((PlayerViewCheckpointAccess.BrewingBlock)container).skywars2$data();
        if(container instanceof CrafterBlockEntity)return ((PlayerViewCheckpointAccess.CrafterBlock)container).skywars2$data();
        throw new IllegalStateException("No native processing data for "+container.getClass().getName());
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
            if(saved.storage!=null)backings.put(entry.getKey(),saved.storage.binding.resolve(player,resolve));
            if(saved.workshop!=null)saved.workshop.image.preflight();
            if(saved.bound!=null)saved.bound.preflight(resolve);
        }
        for(var entry:images.entrySet()) {
            if(!(resolve.apply(entry.getKey()) instanceof ServerPlayer player)){pending.add(entry.getKey());continue;}
            var saved=entry.getValue();
            if(saved.storage!=null) {
                var storage=saved.storage;var backing=backings.get(entry.getKey());
                if(!storage.matches(player)) {
                    closeFutureStorage(player);storage.binding.activate(player);
                    var opened=player.openMenu(new SimpleMenuProvider((id,inventory,p)->storage.create(id,inventory,backing),copyTitle(player,storage.title)));
                    if(opened.isEmpty())throw new IllegalStateException("Native storage reopening failed");
                }
                player.containerMenu.setCarried(storage.cursor.copy());
            }else if(saved.workshop!=null){
                var workshop=saved.workshop;
                if(!workshop.image.matches(player)||!((Title)player).skywars2$menuTitle(player.containerMenu.containerId).equals(workshop.title)){
                    closeFutureStorage(player);
                    var opened=player.openMenu(new SimpleMenuProvider((id,inventory,p)->workshop.image.create(id,inventory),copyTitle(player,workshop.title)));
                    if(opened.isEmpty())throw new IllegalStateException("Native workstation reopening failed");
                }
                workshop.image.restore(player.containerMenu);player.containerMenu.setCarried(workshop.cursor.copy());
            }else if(saved.bound!=null){if(!saved.bound.restore(player,resolve,pending))continue;}
            else closeFutureStorage(player);
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
                    ||(camera!=player&&!tracked(player,camera))) {
                pending.add(saved.camera);camera=player;
            }
            // ServerPlayer.setCamera teleports the BODY to the camera. Body and view are
            // separate images here: publish the link without replaying that teleport.
            publishCamera(player,camera);
        }
    }
    static boolean tracked(ServerPlayer player,Entity entity){
        return entity!=null&&!entity.isRemoved()&&entity.level()==player.level()
                &&player.level().getChunkSource().chunkMap.isChunkTracked(player,entity.chunkPosition().x(),entity.chunkPosition().z())
                &&net.fabricmc.fabric.api.networking.v1.PlayerLookup.tracking(entity).contains(player);
    }
    void finishBoundViews(Function<UUID,Entity> resolve,Set<UUID> pending){
        for(var entry:images.entrySet()){
            var saved=entry.getValue();if(saved.bound==null||!saved.bound.isMount())continue;
            if(!(resolve.apply(entry.getKey()) instanceof ServerPlayer player)){pending.add(entry.getKey());continue;}
            if(saved.bound.restore(player,resolve,pending)){saved.drag.restore(player.containerMenu);player.containerMenu.sendAllDataToRemote();}
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
