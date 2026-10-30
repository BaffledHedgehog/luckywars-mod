package me.skywars2.game;

import java.util.*;
import java.util.function.Function;
import me.skywars2.mixin.ActorCheckpointAccess;
import me.skywars2.mixin.ArenaChunkAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

/** Shared private references bind to canonical native slots, never to an equal replacement item. */
final class MatchItemStackLinks {
    private interface Location { ItemStack get(MatchSpellCheckpoint.Links links); }
    static final class Ref {
        private final ItemStack saved;
        private final List<Location> locations;
        private ItemStack current;
        Ref(ItemStack stack,List<Location> locations){if(locations.size()>1)throw new IllegalStateException("Shared stack across distinct canonical slots needs its alias owner");saved=stack.copy();this.locations=List.copyOf(locations);}
        ItemStack get(MatchSpellCheckpoint.Links links){return Objects.requireNonNull(current,"Resolve canonical item-stack links before private metadata");}
        void resolve(MatchSpellCheckpoint.Links links){
            ItemStack value=null;
            for(var location:locations){var next=location.get(links);
                if(!ItemStack.matches(saved,next))throw new IllegalStateException("Restore canonical native stack values before private aliases");
                // Native NBT cannot represent one mutable stack installed in distinct physical slots.
                // Do not silently split that unusual graph; it needs a canonical multi-root alias owner.
                if(value!=null&&value!=next)throw new IllegalStateException("Shared stack across distinct canonical slots needs its alias owner");value=next;
            }
            current=value==null?saved.copy():value;
        }
    }
    private final IdentityHashMap<ItemStack,List<Location>> roots=new IdentityHashMap<>();
    private final IdentityHashMap<ItemStack,Ref> refs=new IdentityHashMap<>();
    private final Set<Container> containers=Collections.newSetFromMap(new IdentityHashMap<>());
    private final MatchSpellCheckpoint.Links links;
    MatchItemStackLinks(GameController game,MatchSpellCheckpoint.Links links){
        this.links=links;
        for(var player:game.server().getPlayerList().getPlayers()){
            var key=links.key(player);
            container(player.getInventory(),l->((ServerPlayer)l.get(key)).getInventory());
            container(player.inventoryMenu.getCraftSlots(),l->((ServerPlayer)l.get(key)).inventoryMenu.getCraftSlots());
            container(player.getEnderChestInventory(),l->((ServerPlayer)l.get(key)).getEnderChestInventory());
            add(player.inventoryMenu.getCarried(),l->((ServerPlayer)l.get(key)).inventoryMenu.getCarried());
            if(player.containerMenu!=player.inventoryMenu)add(player.containerMenu.getCarried(),l->((ServerPlayer)l.get(key)).containerMenu.getCarried());
        }
        for(var level:game.server().getAllLevels()){
            var manager=((ActorCheckpointAccess.LevelEntities)level).skywars2$manager();
            var storage=((ActorCheckpointAccess.Sections)manager).skywars2$sections();
            for(long chunk:storage.getAllChunksWithExistingSections().toLongArray())
                storage.getExistingSectionsInChunk(chunk).forEach(section->section.getEntities().forEach(entity->{
                    if(entity instanceof ServerPlayer||entity.isRemoved())return;var key=links.key(entity);
                    if(entity instanceof LivingEntity living)for(var slot:EquipmentSlot.values())add(living.getItemBySlot(slot),l->((LivingEntity)l.get(key)).getItemBySlot(slot));
                    if(entity instanceof Container c)container(c,l->(Container)l.get(key));
                    if(entity instanceof InventoryCarrier carrier)container(carrier.getInventory(),l->((InventoryCarrier)l.get(key)).getInventory());
                    if(entity instanceof AbstractHorse||entity instanceof AbstractNautilus)container(MatchBoundViews.mountInventory((LivingEntity)entity),l->MatchBoundViews.mountInventory((LivingEntity)l.get(key)));
                    if(entity instanceof ItemEntity item)add(item.getItem(),l->((ItemEntity)l.get(key)).getItem());
                }));
            for(var holder:((ArenaChunkAccess)level.getChunkSource().chunkMap).skywars2$visibleChunks().values())
                if(holder.getLatestChunk() instanceof LevelChunk chunk)for(var block:chunk.getBlockEntities().values())if(block instanceof Container c){
                    var pos=block.getBlockPos().immutable();var kind=block.getClass();
                    container(c,l->{var current=level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);var be=current==null?null:current.getBlockEntity(pos);
                        if(be==null||be.isRemoved()||be.getClass()!=kind)throw new IllegalStateException("Restore canonical native container before stack alias: "+pos);return (Container)be;});
                }
        }
        // Open-window local buffers come after physical roots; identical backing containers are indexed once.
        for(var player:game.server().getPlayerList().getPlayers())if(player.containerMenu!=player.inventoryMenu){
            var key=links.key(player);var kind=player.containerMenu.getClass();
            for(int index=0;index<player.containerMenu.slots.size();index++){
                var slot=player.containerMenu.slots.get(index);int menuIndex=index;
                container(slot.container,l->{var menu=((ServerPlayer)l.get(key)).containerMenu;
                    if(menu.getClass()!=kind||menuIndex>=menu.slots.size())throw new IllegalStateException("Restore canonical native window before stack alias");return menu.slots.get(menuIndex).container;});
            }
        }
    }
    private void add(ItemStack stack,Location location){if(!stack.isEmpty())roots.computeIfAbsent(stack,k->new ArrayList<>()).add(location);}
    private void container(Container source,Function<MatchSpellCheckpoint.Links,Container> lookup){
        if(!containers.add(source))return;int size=source.getContainerSize();
        for(int index=0;index<size;index++){int slot=index;add(source.getItem(index),l->{var c=lookup.apply(l);if(c.getContainerSize()!=size)throw new IllegalStateException("Restore native container shape before stack alias");return c.getItem(slot);});}
    }
    Ref capture(ItemStack stack){return refs.computeIfAbsent(stack,k->new Ref(k,roots.getOrDefault(k,List.of())));}
    void resolve(){for(var ref:refs.values())ref.resolve(links);}
}
