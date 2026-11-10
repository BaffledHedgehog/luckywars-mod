package me.skywars2.game;

import com.mojang.serialization.Codec;
import java.io.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.saveddata.*;
import net.minecraft.world.level.storage.*;

/** The newest thousand physical drops stay live. Older drops wait in a compressed, persistent deque. */
public final class LbcItemQueue {
    static final int LIMIT=1000;
    private static final String ORDER="skywars2.item_order.";
    private static final SavedDataType<Store> TYPE=new SavedDataType<>(Identifier.parse("skywars2:dormant_items"),
            Store::new,CompoundTag.CODEC.xmap(Store::new,Store::save),null);
    private record Dormant(long order,UUID uuid,BlockPos pos,byte[] bytes){}
    private static final class Store extends SavedData {
        long next;
        final NavigableMap<Long,Dormant> sleeping=new TreeMap<>();
        Store(){}
        Store(CompoundTag tag){
            next=tag.getLongOr("next",0);
            for(var raw:tag.getListOrEmpty("items"))if(raw instanceof CompoundTag entry){
                long order=entry.getLongOr("order",0);
                var uuid=entry.read("uuid",net.minecraft.core.UUIDUtil.CODEC).orElseThrow();
                byte[] bytes=entry.getByteArray("nbt").orElseThrow();
                var pos=BlockPos.of(entry.getLongOr("pos",0));
                if(order<=0||sleeping.put(order,new Dormant(order,uuid,pos,bytes))!=null)
                    throw new IllegalArgumentException("Duplicate dormant item order");
                next=Math.max(next,order);
            }
        }
        CompoundTag save(){
            var tag=new CompoundTag();tag.putLong("next",next);var list=new ListTag();
            for(var entry:sleeping.values()){
                var row=new CompoundTag();row.putLong("order",entry.order);row.store("uuid",net.minecraft.core.UUIDUtil.CODEC,entry.uuid);
                row.putLong("pos",entry.pos.asLong());row.putByteArray("nbt",entry.bytes);list.add(row);
            }
            tag.put("items",list);return tag;
        }
    }
    private static final class World {
        final Store store;
        // Ordered index gives deque ends and O(log n) removal when native pickup/merge/unload removes a middle entry.
        final NavigableMap<Long,ItemEntity> live=new TreeMap<>();
        final Map<UUID,Long> order=new HashMap<>();
        long scanBefore=Long.MAX_VALUE;
        World(ServerLevel level){store=level.getDataStorage().computeIfAbsent(TYPE);}
    }
    private final GameController game;
    private final Map<ServerLevel,World> worlds=new IdentityHashMap<>();
    private boolean closed;
    public LbcItemQueue(GameController game){this.game=game;}
    private World world(ServerLevel level){return worlds.computeIfAbsent(level,World::new);}
    public void initialize(){
        for(var level:game.server().getAllLevels()){
            world(level);
            for(var entity:level.getAllEntities())loaded(entity);
        }
    }
    public void loaded(Entity entity){
        if(closed||!(entity instanceof ItemEntity item)||entity.isRemoved()||!entity.shouldBeSaved()||!(entity.level() instanceof ServerLevel level))return;
        var w=world(level);if(w.order.containsKey(item.getUUID()))return;
        long order=0;
        for(var tag:item.entityTags())if(tag.startsWith(ORDER))try{order=Long.parseLong(tag.substring(ORDER.length()));}catch(NumberFormatException ignored){}
        // A saved body and its archive cannot both become live (including interrupted native saves).
        if(order>0){var sleeping=w.store.sleeping.get(order);if(sleeping!=null&&sleeping.uuid.equals(item.getUUID())){w.store.sleeping.remove(order);w.store.setDirty();}}
        if(order<=0||w.live.containsKey(order)){
            if(w.store.next==Long.MAX_VALUE)throw new IllegalStateException("Item order exhausted");
            order=++w.store.next;item.entityTags().removeIf(t->t.startsWith(ORDER));item.entityTags().add(ORDER+order);w.store.setDirty();
        }else if(order>w.store.next){w.store.next=order;w.store.setDirty();}
        w.live.put(order,item);w.order.put(item.getUUID(),order);
    }
    public void unloaded(Entity entity){
        if(!(entity instanceof ItemEntity)||!(entity.level() instanceof ServerLevel level))return;
        var w=worlds.get(level);if(w==null)return;var order=w.order.remove(entity.getUUID());
        if(order!=null)w.live.remove(order,entity);
    }
    private static Dormant sleep(long order,ItemEntity item) throws IOException {
        var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,item.registryAccess());
        item.saveWithoutId(output);var tag=output.buildResult();tag.putString("id","minecraft:item");
        var bytes=new ByteArrayOutputStream();NbtIo.writeCompressed(tag,bytes);
        return new Dormant(order,item.getUUID(),item.blockPosition().immutable(),bytes.toByteArray());
    }
    public void endTick(){
        if(closed||!game.server().tickRateManager().runsNormally()||game.maps().busy())return;
        for(var level:game.server().getAllLevels()){
            if(game.items().timeStop().stopped(level))continue;
            var w=world(level);long deadline=System.nanoTime()+3_000_000L;int changed=0;
            while(w.live.size()>LIMIT&&changed<64&&System.nanoTime()<deadline){
                var first=w.live.firstEntry();var item=first.getValue();
                if(item.isRemoved()){unloaded(item);continue;}
                // Mounted/serving display actors are not physical loose drops; never orphan their graph.
                if(!item.shouldBeSaved()||item.isPassenger()||item.isVehicle()){unloaded(item);continue;}
                final Dormant image;
                try{image=sleep(first.getKey(),item);}catch(IOException|RuntimeException error){
                    me.skywars2.SkyWars2.LOGGER.error("Could not hibernate item {}; keeping its native body",item.getUUID(),error);break;
                }
                // Serialize successfully BEFORE taking the native entity out of the world.
                w.store.sleeping.put(image.order,image);w.store.setDirty();
                unloaded(item);item.discard();changed++;
            }
            if(w.live.size()>=LIMIT||changed>0||w.store.sleeping.isEmpty())continue;
            // A rotating cursor bounds scans through dormant items in unloaded chunks without loading any chunks.
            int inspected=0,revived=0;
            var entry=w.store.sleeping.lowerEntry(w.scanBefore);
            if(entry==null){w.scanBefore=Long.MAX_VALUE;entry=w.store.sleeping.lastEntry();}
            while(entry!=null&&w.live.size()<LIMIT&&revived<16&&inspected++<128&&System.nanoTime()<deadline){
                var image=entry.getValue();w.scanBefore=image.order;entry=w.store.sleeping.lowerEntry(image.order);
                if(!level.hasChunkAt(image.pos)||level.getEntity(image.uuid)!=null)continue;
                try{
                    var tag=NbtIo.readCompressed(new ByteArrayInputStream(image.bytes),NbtAccounter.unlimitedHeap());
                    var actor=EntityType.loadEntityRecursive(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),tag),level,EntitySpawnReason.LOAD,e->e);
                    if(!(actor instanceof ItemEntity item)||!item.getUUID().equals(image.uuid))throw new IOException("Invalid dormant item body");
                    // Native NBT loading clamps very high motion; suspension is not a new launch.
                    tag.read("Motion",net.minecraft.world.phys.Vec3.CODEC).ifPresent(item::setDeltaMovement);
                    if(level.addFreshEntity(item)){
                        loaded(item);w.store.sleeping.remove(image.order);w.store.setDirty();revived++;
                    }
                }catch(IOException|RuntimeException error){
                    // Retain the archive on failure; never trade a decode problem for silent loss.
                    me.skywars2.SkyWars2.LOGGER.error("Could not revive dormant item {}; keeping its saved data",image.uuid,error);break;
                }
            }
        }
    }
    /** Full round boundaries retire sleeping loot too; server shutdown alone keeps saved data. */
    public void roundEnded(){if(!closed)for(var w:worlds.values())if(!w.store.sleeping.isEmpty()){
        w.store.sleeping.clear();w.store.setDirty();w.scanBefore=Long.MAX_VALUE;
    }}
    /** Same-round map replacement must retire dormant loot in the old terrain as well as live entities. */
    void terrainReplaced(ServerLevel level,java.util.List<net.minecraft.world.level.levelgen.structure.BoundingBox> bounds){
        if(closed||bounds.isEmpty())return;
        var w=world(level);
        if(w.store.sleeping.values().removeIf(item->bounds.stream().anyMatch(box->box.isInside(item.pos)))){
            w.store.setDirty();w.scanBefore=Long.MAX_VALUE;
        }
    }
    public void close(){closed=true;worlds.clear();}
    int active(ServerLevel level){return world(level).live.size();}
    int dormant(ServerLevel level){return world(level).store.sleeping.size();}
    record Image(long next,List<Dormant> sleeping){}
    record Checkpoint(Map<ServerLevel,Image> worlds){}
    Checkpoint checkpoint(){
        var images=new IdentityHashMap<ServerLevel,Image>();
        worlds.forEach((level,w)->images.put(level,new Image(w.store.next,List.copyOf(w.store.sleeping.values()))));
        return new Checkpoint(Map.copyOf(images));
    }
    void restore(Checkpoint image){
        for(var level:game.server().getAllLevels()){
            var w=world(level);w.live.clear();w.order.clear();w.store.sleeping.clear();w.scanBefore=Long.MAX_VALUE;
            var saved=image.worlds.get(level);if(saved!=null){w.store.next=saved.next;for(var e:saved.sleeping)w.store.sleeping.put(e.order,e);}
            w.store.setDirty();
        }
        for(var entity:MatchActorCheckpoint.currentBodies(game).values())loaded(entity);
    }
}
