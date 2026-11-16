package me.skywars2.game;

import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.shorts.*;
import java.lang.reflect.Field;
import java.util.*;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.*;
import net.minecraft.world.level.chunk.storage.SectionStorage;
import net.minecraft.world.level.lighting.DynamicGraphMinFixedPoint;
import net.minecraft.world.level.lighting.LeveledPriorityQueue;

/** Memory phase only. Cold POI storage and I/O must be restored/settled by their separate owner first.
 * Not wired into actor replay until that persistent-storage phase is implemented. */
final class MatchPoiMemory {
    private final ServerLevel level;
    private final PoiManager owner;
    private final Object loadLock,pendingLoads;
    private final Long2ObjectMap<Optional<PoiSection>> storage,savedStorage;
    private final Optional<PoiSection> storageDefault;
    private final Longs dirty,loaded,validationRequests;
    private final Map<PoiSection,Section> sections=new IdentityHashMap<>();
    private final Graph graph;
    static boolean ready(ServerLevel level){
        Object owner=level.getPoiManager(),lock=read(owner,SectionStorage.class,"loadLock");
        synchronized(lock){return ((Map<?,?>)read(owner,SectionStorage.class,"pendingLoads")).isEmpty();}
    }
    @SuppressWarnings("unchecked") MatchPoiMemory(ServerLevel level){
        this.level=level;owner=level.getPoiManager();requireThread(false);
        loadLock=read(owner,SectionStorage.class,"loadLock");
        synchronized(loadLock){
            pendingLoads=read(owner,SectionStorage.class,"pendingLoads");requireReady();
            storage=(Long2ObjectMap<Optional<PoiSection>>)read(owner,SectionStorage.class,"storage");
            savedStorage=new Long2ObjectOpenHashMap<>(storage);storageDefault=storage.defaultReturnValue();
            dirty=new Longs(owner,SectionStorage.class,"dirtyChunks");loaded=new Longs(owner,SectionStorage.class,"loadedChunks");
            validationRequests=new Longs(owner,PoiManager.class,"loadedChunks");
            storage.values().forEach(optional->optional.ifPresent(section->sections.computeIfAbsent(section,Section::new)));
            graph=new Graph(read(owner,PoiManager.class,"distanceTracker"));
        }
    }
    void restoreFrozen(){
        requireThread(true);
        if(level.getPoiManager()!=owner)throw new IllegalStateException("Native POI owner changed");
        same(owner,SectionStorage.class,"loadLock",loadLock);
        synchronized(loadLock){
            same(owner,SectionStorage.class,"pendingLoads",pendingLoads);requireReady();
            // Validate I/O before replacing row contents; never cancel/clone asynchronous futures.
            same(owner,SectionStorage.class,"storage",storage);
            same(owner,PoiManager.class,"distanceTracker",graph.owner);
            sections.values().forEach(Section::restore);
            storage.clear();storage.putAll(savedStorage);storage.defaultReturnValue(storageDefault);
            dirty.restore();loaded.restore();validationRequests.restore();graph.restore();
        }
    }
    private void requireReady(){if(!((Map<?,?>)pendingLoads).isEmpty())throw new IllegalStateException("Settle native POI loads before its memory phase");}
    private void requireThread(boolean frozen){
        if(!level.getServer().isSameThread()||frozen&&!level.getServer().tickRateManager().isFrozen())throw new IllegalStateException("POI memory replay requires its frozen server thread");
    }
    private static final class Section {
        final PoiSection owner;
        final Short2ObjectMap<PoiRecord> records,savedRecords;final PoiRecord recordDefault;
        final Map<Holder<PoiType>,Set<PoiRecord>> byType,savedByType;
        final Map<Set<PoiRecord>,List<PoiRecord>> sets=new IdentityHashMap<>();
        final Map<PoiRecord,Record> values=new IdentityHashMap<>();
        final Object dirty;final boolean valid;
        @SuppressWarnings("unchecked") Section(PoiSection owner){
            this.owner=owner;records=(Short2ObjectMap<PoiRecord>)read(owner,PoiSection.class,"records");
            savedRecords=new Short2ObjectOpenHashMap<>(records);recordDefault=records.defaultReturnValue();
            byType=(Map<Holder<PoiType>,Set<PoiRecord>>)read(owner,PoiSection.class,"byType");savedByType=new LinkedHashMap<>(byType);
            byType.values().forEach(set->{sets.put(set,List.copyOf(set));set.forEach(record->values.computeIfAbsent(record,Record::new));});
            records.values().forEach(record->values.computeIfAbsent(record,Record::new));
            dirty=read(owner,PoiSection.class,"setDirty");valid=owner.isValid();
        }
        void restore(){
            same(owner,PoiSection.class,"records",records);same(owner,PoiSection.class,"byType",byType);same(owner,PoiSection.class,"setDirty",dirty);
            values.values().forEach(Record::restore);sets.forEach((set,saved)->{set.clear();set.addAll(saved);});
            records.clear();records.putAll(savedRecords);records.defaultReturnValue(recordDefault);
            byType.clear();byType.putAll(savedByType);write(owner,PoiSection.class,"isValid",valid);
        }
    }
    private static final class Record {
        final PoiRecord owner;final Object position,type,dirty;final int free;
        Record(PoiRecord owner){this.owner=owner;position=owner.getPos();type=owner.getPoiType();dirty=read(owner,PoiRecord.class,"setDirty");free=owner.getFreeTickets();}
        void restore(){same(owner,PoiRecord.class,"pos",position);same(owner,PoiRecord.class,"poiType",type);same(owner,PoiRecord.class,"setDirty",dirty);write(owner,PoiRecord.class,"freeTickets",free);}
    }
    private static final class Longs {
        final Object owner;final Class<?> type;final String field;final LongSet original;final long[] saved;
        Longs(Object owner,Class<?> type,String field){this.owner=owner;this.type=type;this.field=field;original=(LongSet)read(owner,type,field);saved=original.toLongArray();}
        void restore(){same(owner,type,field,original);original.clear();for(long value:saved)original.add(value);}
    }
    private static final class Bytes {
        final Long2ByteMap owner,saved;final byte defaultValue;
        Bytes(Long2ByteMap owner){this.owner=owner;saved=new Long2ByteOpenHashMap(owner);defaultValue=owner.defaultReturnValue();}
        void restore(){owner.clear();owner.putAll(saved);owner.defaultReturnValue(defaultValue);}
    }
    private static final class Graph {
        final Object owner;final Class<?> type;
        final Bytes levels,computed;final LeveledPriorityQueue queue;
        final LongLinkedOpenHashSet[] array;final LongLinkedOpenHashSet[] queues;final long[][] entries;
        final int count,first;final boolean work;
        Graph(Object owner){
            this.owner=owner;type=owner.getClass();levels=new Bytes((Long2ByteMap)read(owner,type,"levels"));
            computed=new Bytes((Long2ByteMap)read(owner,DynamicGraphMinFixedPoint.class,"computedLevels"));
            work=(Boolean)read(owner,DynamicGraphMinFixedPoint.class,"hasWork");count=(Integer)read(owner,DynamicGraphMinFixedPoint.class,"levelCount");
            queue=(LeveledPriorityQueue)read(owner,DynamicGraphMinFixedPoint.class,"priorityQueue");
            array=(LongLinkedOpenHashSet[])read(queue,LeveledPriorityQueue.class,"queues");queues=array.clone();entries=new long[array.length][];
            for(int i=0;i<array.length;i++)entries[i]=array[i].toLongArray();
            first=(Integer)read(queue,LeveledPriorityQueue.class,"firstQueuedLevel");
            if((Integer)read(queue,LeveledPriorityQueue.class,"levelCount")!=count||array.length!=count)throw new IllegalStateException("Native POI distance queue shape mismatch");
        }
        void restore(){
            same(owner,type,"levels",levels.owner);same(owner,DynamicGraphMinFixedPoint.class,"computedLevels",computed.owner);
            same(owner,DynamicGraphMinFixedPoint.class,"priorityQueue",queue);same(queue,LeveledPriorityQueue.class,"queues",array);
            if((Integer)read(owner,DynamicGraphMinFixedPoint.class,"levelCount")!=count||(Integer)read(queue,LeveledPriorityQueue.class,"levelCount")!=count)throw new IllegalStateException("Native POI distance level count changed");
            levels.restore();computed.restore();
            for(int i=0;i<array.length;i++){array[i]=queues[i];array[i].clear();for(long value:entries[i])array[i].add(value);}
            write(queue,LeveledPriorityQueue.class,"firstQueuedLevel",first);write(owner,DynamicGraphMinFixedPoint.class,"hasWork",work);
        }
    }
    private static void same(Object owner,Class<?> type,String name,Object expected){if(read(owner,type,name)!=expected)throw new IllegalStateException("Native POI memory owner identity changed: "+name);}
    private static Field field(Class<?> type,String name){try{var field=type.getDeclaredField(name);field.setAccessible(true);return field;}catch(ReflectiveOperationException error){throw new IllegalStateException("Review native POI field "+type.getName()+"."+name,error);}}
    private static Object read(Object owner,Class<?> type,String name){try{return field(type,name).get(owner);}catch(IllegalAccessException error){throw new IllegalStateException(error);}}
    private static void write(Object owner,Class<?> type,String name,Object value){try{field(type,name).set(owner,value);}catch(IllegalAccessException error){throw new IllegalStateException(error);}}
}
