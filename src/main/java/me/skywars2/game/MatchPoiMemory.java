package me.skywars2.game;

import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.shorts.*;
import java.util.*;
import me.skywars2.mixin.PoiCheckpointAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.*;
import net.minecraft.world.level.lighting.LeveledPriorityQueue;

/** Memory phase only. Cold POI storage and I/O must be restored/settled by their separate owner first.
 * Composed by MatchPoiCheckpoint with its persistent-storage phases. */
final class MatchPoiMemory {
    private final ServerLevel level;
    private final PoiManager owner;
    private final Object loadLock;
    private final Long2ObjectMap<?> pendingLoads;
    private final Long2ObjectMap<Optional<PoiSection>> storage,savedStorage;
    private final Optional<PoiSection> storageDefault;
    private final Longs dirty,loaded,validationRequests;
    private final Map<PoiSection,Section> sections=new IdentityHashMap<>();
    private final Graph graph;
    Set<Long> loadedChunkKeys(){var result=new HashSet<Long>();for(long key:loaded.saved)result.add(key);return Set.copyOf(result);}
    static boolean ready(ServerLevel level){
        var access = storageAccess(level.getPoiManager());
        synchronized (access.skywars2$poiLoadLock()) {
            return access.skywars2$poiPendingLoads().isEmpty();
        }
    }
    MatchPoiMemory(ServerLevel level){
        this.level=level;owner=level.getPoiManager();requireThread(false);
        var access = storageAccess(owner);
        var manager = managerAccess(owner);
        loadLock = access.skywars2$poiLoadLock();
        synchronized (loadLock) {
            pendingLoads = access.skywars2$poiPendingLoads();
            requireReady();
            storage = access.skywars2$poiStorage();
            savedStorage = new Long2ObjectOpenHashMap<>(storage);
            storageDefault = storage.defaultReturnValue();
            dirty = new Longs(access.skywars2$poiDirtyChunks());
            loaded = new Longs(access.skywars2$poiStoredChunks());
            validationRequests = new Longs(manager.skywars2$poiValidatedChunks());
            storage.values().forEach(optional -> optional.ifPresent(section -> sections.computeIfAbsent(section,Section::new)));
            graph = new Graph(manager.skywars2$poiDistanceTracker());
        }
    }

    static PoiCheckpointAccess.Storage storageAccess(PoiManager manager) {
        return (PoiCheckpointAccess.Storage)(Object) manager;
    }

    private static PoiCheckpointAccess.Manager managerAccess(PoiManager manager) {
        return (PoiCheckpointAccess.Manager)(Object) manager;
    }
    void restoreFrozen(){
        requireThread(true);
        if(level.getPoiManager()!=owner)throw new IllegalStateException("Native POI owner changed");
        var access = storageAccess(owner);
        var manager = managerAccess(owner);
        same(access.skywars2$poiLoadLock(),loadLock,"loadLock");
        synchronized (loadLock) {
            same(access.skywars2$poiPendingLoads(),pendingLoads,"pendingLoads");
            requireReady();
            // Validate I/O before replacing row contents; never cancel or clone asynchronous futures.
            same(access.skywars2$poiStorage(),storage,"storage");
            same(manager.skywars2$poiDistanceTracker(),graph.owner,"distanceTracker");
            sections.values().forEach(Section::restore);
            storage.clear();
            storage.putAll(savedStorage);
            storage.defaultReturnValue(storageDefault);
            dirty.restore(access.skywars2$poiDirtyChunks(),"dirtyChunks");
            loaded.restore(access.skywars2$poiStoredChunks(),"storedChunks");
            validationRequests.restore(manager.skywars2$poiValidatedChunks(),"validatedChunks");
            graph.restore();
        }
    }
    private void requireReady(){if(!pendingLoads.isEmpty())throw new IllegalStateException("Settle native POI loads before its memory phase");}
    private void requireThread(boolean frozen){
        if(!level.getServer().isSameThread()||frozen&&!level.getServer().tickRateManager().isFrozen())throw new IllegalStateException("POI memory replay requires its frozen server thread");
    }
    private static final class Section {
        final PoiSection owner;
        final Short2ObjectMap<PoiRecord> records,savedRecords;final PoiRecord recordDefault;
        final Map<Holder<PoiType>,Set<PoiRecord>> byType,savedByType;
        final Map<Set<PoiRecord>,List<PoiRecord>> sets=new IdentityHashMap<>();
        final Map<PoiRecord,Record> values=new IdentityHashMap<>();
        final Runnable dirty;
        final boolean valid;
        Section(PoiSection owner) {
            this.owner=owner;
            var access = (PoiCheckpointAccess.Section)(Object) owner;
            records = access.skywars2$poiRecords();
            savedRecords = new Short2ObjectOpenHashMap<>(records);
            recordDefault = records.defaultReturnValue();
            byType = access.skywars2$poiRecordsByType();
            savedByType = new LinkedHashMap<>(byType);
            byType.values().forEach(set->{sets.put(set,List.copyOf(set));set.forEach(record->values.computeIfAbsent(record,Record::new));});
            records.values().forEach(record->values.computeIfAbsent(record,Record::new));
            dirty = access.skywars2$poiSectionDirty();
            valid = owner.isValid();
        }
        void restore() {
            var access = (PoiCheckpointAccess.Section)(Object) owner;
            same(access.skywars2$poiRecords(),records,"records");
            same(access.skywars2$poiRecordsByType(),byType,"byType");
            same(access.skywars2$poiSectionDirty(),dirty,"section setDirty");
            values.values().forEach(Record::restore);
            sets.forEach((set,saved)->{set.clear();set.addAll(saved);});
            records.clear();
            records.putAll(savedRecords);
            records.defaultReturnValue(recordDefault);
            byType.clear();
            byType.putAll(savedByType);
            access.skywars2$poiValid(valid);
        }
    }
    private record Record(PoiRecord owner, BlockPos position, Holder<PoiType> type, Runnable dirty, int free) {
        Record(PoiRecord owner) {
            this(owner,owner.getPos(),owner.getPoiType(),((PoiCheckpointAccess.Record)(Object) owner).skywars2$poiRecordDirty(),
                    owner.getFreeTickets());
        }
        void restore() {
            var access = (PoiCheckpointAccess.Record)(Object) owner;
            same(owner.getPos(),position,"pos");
            same(owner.getPoiType(),type,"poiType");
            same(access.skywars2$poiRecordDirty(),dirty,"record setDirty");
            access.skywars2$poiFreeTickets(free);
        }
    }
    private record Longs(LongSet original, long[] saved) {
        Longs(LongSet original) {
            this(original,original.toLongArray());
        }
        void restore(LongSet target, String label) {
            same(target,original,label);
            original.clear();
            for(long value:saved) original.add(value);
        }
    }
    private static final class Bytes {
        final Long2ByteMap owner,saved;final byte defaultValue;
        Bytes(Long2ByteMap owner){this.owner=owner;saved=new Long2ByteOpenHashMap(owner);defaultValue=owner.defaultReturnValue();}
        void restore(){owner.clear();owner.putAll(saved);owner.defaultReturnValue(defaultValue);}
    }
    private static final class Graph {
        final PoiManager.DistanceTracker owner;
        final Bytes levels, computed;
        final LeveledPriorityQueue queue;
        final LongLinkedOpenHashSet[] array, queues;
        final long[][] entries;
        final int count, first;
        final boolean work;

        Graph(PoiManager.DistanceTracker owner) {
            this.owner = owner;
            var access = (PoiCheckpointAccess.Graph)(Object) owner;
            levels = new Bytes(((PoiCheckpointAccess.Distances)(Object) owner).skywars2$poiLevels());
            computed = new Bytes(access.skywars2$poiComputedLevels());
            work = access.skywars2$poiGraphHasWork();
            count = access.skywars2$poiGraphLevelCount();
            queue = access.skywars2$poiPriorityQueue();
            var queueAccess = (PoiCheckpointAccess.Queue)(Object) queue;
            array = queueAccess.skywars2$poiQueues();
            queues = array.clone();
            entries = new long[array.length][];
            for(int i=0;i<array.length;i++) entries[i]=array[i].toLongArray();
            first = queueAccess.skywars2$poiFirstQueuedLevel();
            if (queueAccess.skywars2$poiQueueLevelCount()!=count || array.length!=count)
                throw new IllegalStateException("Native POI distance queue shape mismatch");
        }
        void restore() {
            var access = (PoiCheckpointAccess.Graph)(Object) owner;
            var queueAccess = (PoiCheckpointAccess.Queue)(Object) queue;
            same(((PoiCheckpointAccess.Distances)(Object) owner).skywars2$poiLevels(),levels.owner,"levels");
            same(access.skywars2$poiComputedLevels(),computed.owner,"computedLevels");
            same(access.skywars2$poiPriorityQueue(),queue,"priorityQueue");
            same(queueAccess.skywars2$poiQueues(),array,"queues");
            if (access.skywars2$poiGraphLevelCount()!=count || queueAccess.skywars2$poiQueueLevelCount()!=count)
                throw new IllegalStateException("Native POI distance level count changed");
            levels.restore();
            computed.restore();
            for(int i=0;i<array.length;i++) {
                array[i]=queues[i];
                array[i].clear();
                for(long value:entries[i]) array[i].add(value);
            }
            queueAccess.skywars2$poiFirstQueuedLevel(first);
            access.skywars2$poiGraphHasWork(work);
        }
    }

    private static void same(Object value, Object expected, String label) {
        if (value != expected) throw new IllegalStateException("Native POI memory owner identity changed: " + label);
    }
}
