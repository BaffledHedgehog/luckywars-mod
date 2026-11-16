package me.skywars2.game;

import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.*;

/** Logical POI write journal and asynchronous storage/memory phases for a reusable save point. */
public final class MatchPoiStorage {
    private static final Object REGISTRY_LOCK=new Object();
    private static final Map<IOWorker,Set<MatchPoiStorage>> OWNERS=new IdentityHashMap<>();
    private static final Map<IOWorker,MatchPoiStorage> RESTORES=new IdentityHashMap<>();
    private static final ThreadLocal<MatchPoiStorage> WRITER=new ThreadLocal<>();
    private static volatile boolean tracking;
    private enum Mode { RECORDING, RESTORING, CLOSED }
    private final ServerLevel level;
    private final PoiManager owner;
    private final SimpleRegionStorage storage;
    private final IOWorker worker;
    private final MatchPoiMemory memory;
    private final Set<Long> known;
    private final Set<ChunkPos> writtenKnown=new LinkedHashSet<>();
    private final Map<ChunkPos,CompletableFuture<Optional<CompoundTag>>> cold=new LinkedHashMap<>();
    private Mode mode=Mode.RECORDING;
    private Restore restoring;
    private CompletableFuture<Void> closing;

    static boolean captureReady(ServerLevel level){
        if(!MatchPoiMemory.ready(level))return false;
        var storage=read(level.getPoiManager(),SectionStorage.class,"simpleRegionStorage");
        var worker=read(storage,SimpleRegionStorage.class,"worker");
        synchronized(REGISTRY_LOCK){return !RESTORES.containsKey(worker);}
    }
    MatchPoiStorage(ServerLevel level){
        this.level=level;requireThread(false);
        if(!MatchMobGoals.enabled())throw new IllegalStateException("POI checkpoint storage belongs to the unfinished native goal experiment");
        if(!captureReady(level))throw new IllegalStateException("Native POI storage is not ready for a new point");
        owner=level.getPoiManager();memory=new MatchPoiMemory(level);known=memory.loadedChunkKeys();
        storage=(SimpleRegionStorage)read(owner,SectionStorage.class,"simpleRegionStorage");worker=(IOWorker)read(storage,SimpleRegionStorage.class,"worker");
        synchronized(REGISTRY_LOCK){OWNERS.computeIfAbsent(worker,key->Collections.newSetFromMap(new IdentityHashMap<>())).add(this);tracking=true;}
    }
    /** Null allows the actual native store; a future acknowledges a discarded intermediate-restore write. */
    public static CompletableFuture<Void> beforeWrite(IOWorker worker,ChunkPos pos){
        if(!tracking)return null;
        List<MatchPoiStorage> owners;
        for(;;){
            MatchPoiStorage lease;
            synchronized(REGISTRY_LOCK){lease=RESTORES.get(worker);owners=List.copyOf(OWNERS.getOrDefault(worker,Set.of()));}
            if(lease!=null&&WRITER.get()!=lease){
                var suppressed=lease.suppressedWrite();if(suppressed!=null)return suppressed;continue;
            }
            break;
        }
        for(var owner:owners)owner.captureWrite(pos);
        return null;
    }
    private synchronized CompletableFuture<Void> suppressedWrite(){
        if(mode==Mode.RESTORING)return restoring.completion;
        if(mode==Mode.CLOSED)return closing;
        return null;
    }
    private synchronized void captureWrite(ChunkPos pos){
        if(mode!=Mode.RECORDING)return;
        if(known.contains(pos.pack()))writtenKnown.add(pos);
        else cold.computeIfAbsent(pos,key->storage.read(key).thenApply(value->value.map(CompoundTag::copy)));
        // read() is queued before the intercepted native store on the same FOREGROUND executor.
    }
    boolean knownAtCapture(ChunkPos pos){return known.contains(pos.pack());}
    synchronized Restore beginRestoreFrozen(){
        requireThread(true);validate();
        if(mode!=Mode.RECORDING)throw new IllegalStateException("POI point is not recording");
        synchronized(REGISTRY_LOCK){
            if(RESTORES.containsKey(worker))throw new IllegalStateException("Another POI point owns this worker's restore");
            RESTORES.put(worker,this);
        }
        mode=Mode.RESTORING;restoring=new Restore();restoring.start();return restoring;
    }
    final class Restore {
        private final CompletableFuture<Void> completion=new CompletableFuture<>();
        private CompletableFuture<Void> io;
        private int phase;
        private void start(){
            var originals=new LinkedHashMap<>(cold);
            io=CompletableFuture.allOf(originals.values().toArray(CompletableFuture[]::new)).thenCompose(ignored->{
                synchronized(MatchPoiStorage.this){
                    requireActive();var writes=new ArrayList<CompletableFuture<Void>>();
                    originals.forEach((pos,tag)->writes.add(owned(()->storage.write(pos,tag.join().map(CompoundTag::copy).orElse(null)))));
                    return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new)).thenCompose(done->storage.synchronize(true));
                }
            });
        }
        boolean tickFrozen(){
            requireThread(true);validate();
            synchronized(MatchPoiStorage.this){
                requireActive();if(phase==2)return true;if(!io.isDone())return false;io.join();
                owner.tick(()->false); // Unpack completed reads; do not emit intermediate dirty data.
                if(!MatchPoiMemory.ready(level))return false;
                memory.restoreFrozen();
                if(phase==0){
                    var writes=new ArrayList<CompletableFuture<Void>>();
                    for(var pos:writtenKnown){var tag=encodeLoaded(pos);writes.add(owned(()->storage.write(pos,tag)));}
                    io=CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new)).thenCompose(done->storage.synchronize(true));phase=1;return false;
                }
                phase=2;return true;
            }
        }
        /** Caller invokes this only after all remaining frozen world/body phases have finished. */
        boolean readyToResumeFrozen(){
            requireThread(true);validate();
            synchronized(MatchPoiStorage.this){
                requireActive();if(phase!=2)throw new IllegalStateException("Finish POI storage phases before resuming writes");
                owner.tick(()->false);return MatchPoiMemory.ready(level);
            }
        }
        boolean resumeFrozen(){
            requireThread(true);
            synchronized(MatchPoiStorage.this){
                requireActive();if(phase!=2)throw new IllegalStateException("Finish POI storage phases before resuming writes");
                owner.tick(()->false);if(!MatchPoiMemory.ready(level))return false;
                memory.restoreFrozen();mode=Mode.RECORDING;releaseLease();completion.complete(null);return true;
            }
        }
        private void requireActive(){if(mode!=Mode.RESTORING||restoring!=this)throw new CancellationException("POI restore owner was disposed");}
    }
    synchronized CompletableFuture<Void> closeAsync(){
        requireThread(false);if(closing!=null)return closing;
        mode=Mode.CLOSED;
        synchronized(REGISTRY_LOCK){var set=OWNERS.get(worker);if(set!=null){set.remove(this);if(set.isEmpty())OWNERS.remove(worker);}tracking=!OWNERS.isEmpty()||!RESTORES.isEmpty();}
        CompletableFuture<?> wait=restoring==null?CompletableFuture.allOf(cold.values().toArray(CompletableFuture[]::new)):restoring.io;
        closing=wait.handle((value,error)->null).thenCompose(ignored->storage.synchronize(restoring!=null)).whenComplete((value,error)->{
            synchronized(MatchPoiStorage.this){releaseLease();if(restoring!=null){if(error==null)restoring.completion.complete(null);else restoring.completion.completeExceptionally(error);}}
        });
        // The CLOSED guard and this monitor prevent any later restore continuation from
        // queuing old writes. Already queued stores precede all subsequent native stores.
        // Release now so map/reset writes are never discarded behind a retired point's lease.
        releaseLease();
        return closing;
    }
    private void releaseLease(){synchronized(REGISTRY_LOCK){RESTORES.remove(worker,this);tracking=!OWNERS.isEmpty()||!RESTORES.isEmpty();}}
    private <T>T owned(Supplier<T> task){
        var previous=WRITER.get();WRITER.set(this);try{return task.get();}finally{if(previous==null)WRITER.remove();else WRITER.set(previous);}
    }
    private CompoundTag encodeLoaded(ChunkPos pos){
        try{
            var method=SectionStorage.class.getDeclaredMethod("writeChunk",ChunkPos.class,DynamicOps.class);method.setAccessible(true);
            var encoded=(Dynamic<?>)method.invoke(owner,pos,level.registryAccess().createSerializationContext(NbtOps.INSTANCE));
            if(encoded.getValue() instanceof CompoundTag tag)return tag;
            throw new IllegalStateException("Native POI encoder returned non-compound data");
        }catch(ReflectiveOperationException error){throw new IllegalStateException("Review native POI chunk encoder",error);}
    }
    private void validate(){if(level.getPoiManager()!=owner||read(owner,SectionStorage.class,"simpleRegionStorage")!=storage||read(storage,SimpleRegionStorage.class,"worker")!=worker)throw new IllegalStateException("Native POI storage owner changed");}
    private void requireThread(boolean frozen){if(!level.getServer().isSameThread()||frozen&&!level.getServer().tickRateManager().isFrozen())throw new IllegalStateException("POI storage phases require their server thread and frozen restoration");}
    private static Object read(Object owner,Class<?> type,String name){try{var field=type.getDeclaredField(name);field.setAccessible(true);return field.get(owner);}catch(ReflectiveOperationException error){throw new IllegalStateException("Review native POI storage field "+type.getName()+"."+name,error);}}
}
