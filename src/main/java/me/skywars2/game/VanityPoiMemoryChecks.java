package me.skywars2.game;

import java.util.*;
import java.util.concurrent.*;
import me.skywars2.mixin.PoiCheckpointAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.village.poi.*;
import net.minecraft.world.level.ChunkPos;

/** Native memory consumers only; this does not qualify cold POI disk replay or village AI. */
final class VanityPoiMemoryChecks {
    private VanityPoiMemoryChecks(){}
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Vanity POI memory: "+why);}
    private static void restore(GameController game,MatchPoiMemory image){
        boolean frozen=game.server().tickRateManager().isFrozen();game.server().tickRateManager().setFrozen(true);
        try{image.restoreFrozen();}finally{game.server().tickRateManager().setFrozen(frozen);}
    }
    static void verify(GameController game,WoolBucketChecks.Fixture f){
        var poi=f.level.getPoiManager();settle(poi);var before=new MatchPoiMemory(f.level);
        var storageState=(PoiCheckpointAccess.Storage)poi;
        var managerState=(PoiCheckpointAccess.Manager)poi;
        try{
            var registry=f.level.registryAccess().lookupOrThrow(Registries.POINT_OF_INTEREST_TYPE);
            var home=registry.getOrThrow(PoiTypes.HOME);var meeting=registry.getOrThrow(PoiTypes.MEETING);
            var source=BlockPos.containing(f.home).above(5).offset(1536,0,1536);var spare=source.east(2);
            poi.getInSquare(type->true,source,0,PoiManager.Occupancy.ANY).count();
            require(poi.sectionsToVillage(SectionPos.of(source))==7,"isolated source has no other loaded village influence before native registration");
            require(poi.getType(source).isEmpty()&&poi.getType(spare).isEmpty(),"isolated native POI API positions");
            PoiRecord record=poi.add(source,home);require(record!=null&&poi.add(spare,home)!=null,"native registration creates genuine records");
            require(poi.take(type->type.is(PoiTypes.HOME),(type,pos)->pos.equals(source),source,0).orElseThrow().equals(source)
                    &&record.isOccupied()&&!record.hasSpace(),"actual native take consumes one home ticket");
            var storage=storageState.skywars2$poiStorage();
            var section=storage.get(SectionPos.asLong(source)).orElseThrow();
            var sectionState=(PoiCheckpointAccess.Section)section;
            var records=sectionState.skywars2$poiRecords();
            var types=sectionState.skywars2$poiRecordsByType();var homeSet=types.get(home);
            var dirty=storageState.skywars2$poiDirtyChunks();var loaded=managerState.skywars2$poiValidatedChunks();
            var graph=managerState.skywars2$poiDistanceTracker();
            require(graph.getQueueSize()>0,"native ticket mutation queued real distance propagation before image");
            var image=new MatchPoiMemory(f.level);var keys=Set.copyOf(storage.keySet());var packed=section.pack();
            int pending=graph.getQueueSize();
            require(poi.sectionsToVillage(SectionPos.of(source))==0,"actual native distance consumer recognizes occupied village center");
            var future=source.offset(2048,0,2048);var futureChunk=ChunkPos.containing(future);
            require(!keys.contains(SectionPos.asLong(future)),"future metadata chunk absent from captured memory table");
            for(int replay=0;replay<3;replay++){
                require(poi.release(source)&&record.hasSpace(),"actual release changes the retained old record");
                poi.remove(source);var replacement=poi.add(source,meeting);
                require(replacement!=null&&replacement!=record,"native replacement at same position has another object and type");
                var request=poi.prefetch(futureChunk);
                require(!MatchPoiMemory.ready(f.level),"real native prefetch owns pending entry even if I/O completes early");
                boolean rejected=false;
                try{restore(game,image);}catch(IllegalStateException expected){rejected=expected.getMessage().contains("Settle native POI loads");}
                require(rejected&&poi.getType(source).orElseThrow().is(PoiTypes.MEETING)&&record.hasSpace(),
                        "memory restore rejects unsettled native load before changing captured rows or ticket values");
                try{request.get(10,TimeUnit.SECONDS);}catch(Exception error){throw new IllegalStateException("Native POI prefetch",error);}
                settle(poi);
                require(poi.add(future,home)!=null&&poi.take(type->type.is(PoiTypes.HOME),(type,pos)->pos.equals(future),future,0).isPresent(),
                        "genuine later loaded section and occupied record change native village graph");
                poi.ensureLoadedAndValid(f.level,future,0);settle(poi);
                int sourceDistance=poi.sectionsToVillage(SectionPos.of(source)),futureDistance=poi.sectionsToVillage(SectionPos.of(future));
                // Native DynamicGraphMinFixedPoint saturates a touched former center at levelCount-1 (6).
                // The default 7 denotes an unknown node; it is not the native removal result.
                require(sourceDistance==PoiManager.MAX_VILLAGE_DISTANCE&&futureDistance==0
                        &&poi.getCountInRange(type->true,source,0,PoiManager.Occupancy.IS_OCCUPIED)==0,
                        "native graph saturates removed center and moves occupancy; source="+sourceDistance+", future="+futureDistance);
                restore(game,image);
                require(storageState.skywars2$poiStorage()==storage&&storage.keySet().equals(keys)&&storage.get(SectionPos.asLong(source)).orElseThrow()==section
                        &&sectionState.skywars2$poiRecords()==records&&sectionState.skywars2$poiRecordsByType()==types&&types.get(home)==homeSet,
                        "same native manager table, section, position/type indexes and original type-set aliases restored");
                require(section.pack().equals(packed)&&!record.hasSpace()&&record.isOccupied()
                        &&poi.getInRange(type->type.is(PoiTypes.HOME),source,0,PoiManager.Occupancy.IS_OCCUPIED).findFirst().orElseThrow()==record,
                        "original record and ticket count restored independently from future replacement");
                require(storageState.skywars2$poiDirtyChunks()==dirty&&managerState.skywars2$poiValidatedChunks()==loaded&&graph.getQueueSize()==pending,
                        "native dirty queue/load aliases and exact pending graph phase restored");
                require(poi.sectionsToVillage(SectionPos.of(source))==0&&poi.sectionsToVillage(SectionPos.of(future))==7,
                        "real propagation resumes old occupied center and forgets future graph influence");
            }
        }finally{settle(poi);restore(game,before);}
    }
    private static void settle(PoiManager poi){
        var storage=(PoiCheckpointAccess.Storage)poi;
        // Fixture preparation only. The production memory phase never waits for disk or drains native queues.
        for(int attempt=0;attempt<8;attempt++){
            CompletableFuture<?>[] pending;
            synchronized(storage.skywars2$poiLoadLock()){
                pending=storage.skywars2$poiPendingLoads().values().toArray(CompletableFuture<?>[]::new);
            }
            if(pending.length==0)return;
            try{CompletableFuture.allOf(pending).get(10,TimeUnit.SECONDS);}catch(Exception error){throw new IllegalStateException("Settle fixture POI reads",error);}
            poi.tick(()->false); // Native unpack/propgation, deliberately no dirty-chunk writes.
        }
        throw new IllegalStateException("Fixture POI loads did not settle");
    }
}
