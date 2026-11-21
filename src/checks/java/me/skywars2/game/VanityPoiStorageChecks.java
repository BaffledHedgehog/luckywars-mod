package me.skywars2.game;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import me.skywars2.game.match.checkpoints.MatchPoiMemory;
import me.skywars2.game.match.checkpoints.MatchPoiStorage;
import net.minecraft.SharedConstants;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.world.entity.ai.village.poi.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.SimpleRegionStorage;

/** Actual native POI files/worker, nonblocking frame-driven replay and cleanup; no actor integration claim. */
final class VanityPoiStorageChecks implements AutoCloseable {
    private final GameController game;
    private final WoolBucketChecks.Fixture f;
    private final PoiManager poi;
    private final SimpleRegionStorage storage;
    private final BlockPos source,extra,coldPos,newPos;
    private final ChunkPos knownChunk,coldChunk,newChunk,supplierChunk;
    private final List<ChunkPos> chunks;
    private final Holder<PoiType> home,meeting;
    private final boolean initialFreeze;
    private final int started;
    private final Map<ChunkPos,Optional<CompoundTag>> original=new LinkedHashMap<>();
    private Map<ChunkPos,CompletableFuture<Optional<CompoundTag>>> reads;
    private CompletableFuture<?> work=CompletableFuture.completedFuture(null),suppressed;
    private MatchPoiMemory before;
    private MatchPoiStorage point,other;
    private MatchPoiStorage.Restore replay;
    private AtomicInteger suppressedSupplier;
    private int phase,cycle;
    private boolean otherReplayed,closed;
    private Throwable failure;
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Vanity POI storage: "+why);}
    VanityPoiStorageChecks(GameController game,WoolBucketChecks.Fixture f){
        this.game=game;this.f=f;poi=f.level.getPoiManager();storage=(SimpleRegionStorage)WoolBucketChecks.read(poi,"simpleRegionStorage");
        source=BlockPos.containing(f.home).above(6).offset(1792,0,1792);extra=source.east(2);coldPos=source.offset(512,0,0);newPos=source.offset(1024,0,0);
        knownChunk=ChunkPos.containing(source);coldChunk=ChunkPos.containing(coldPos);newChunk=ChunkPos.containing(newPos);supplierChunk=ChunkPos.containing(source.offset(1536,0,0));
        chunks=List.of(knownChunk,coldChunk,newChunk,supplierChunk);
        var registry=f.level.registryAccess().lookupOrThrow(Registries.POINT_OF_INTEREST_TYPE);home=registry.getOrThrow(PoiTypes.HOME);meeting=registry.getOrThrow(PoiTypes.MEETING);
        initialFreeze=game.server().tickRateManager().isFrozen();started=game.server().getTickCount();
    }
    boolean step(){
        f.client.acknowledge();f.audience.acknowledge();if(closed)return true;
        try{require(game.server().getTickCount()-started<1200,"bounded asynchronous native scene phase="+phase);return advance();}
        catch(Throwable error){
            if(phase>=90){if(failure==null)failure=error;else failure.addSuppressed(error);closed=true;game.server().tickRateManager().setFrozen(initialFreeze);throw new IllegalStateException("POI fixture cleanup failed",failure);}
            failure=error;cleanup();return false;
        }
    }
    private boolean advance(){
        if(!work.isDone())return false;work.join();
        switch(phase){
            case 0->{
                poi.tick(()->false);if(!MatchPoiMemory.ready(f.level))return false;
                before=new MatchPoiMemory(f.level);readAll();phase=1;
            }
            case 1->{
                reads.forEach((pos,data)->original.put(pos,data.join().map(CompoundTag::copy)));
                require(poi.getType(source).isEmpty()&&poi.getType(extra).isEmpty(),"isolated source positions");
                require(poi.add(source,home)!=null&&poi.take(type->type.is(PoiTypes.HOME),(type,pos)->pos.equals(source),source,0).isPresent(),"native occupied source row");
                poi.flushAll();
                work=CompletableFuture.allOf(storage.write(coldChunk,coldTag()),storage.write(newChunk,(CompoundTag)null),storage.write(supplierChunk,tag(23)))
                        .thenCompose(done->storage.synchronize(true));phase=2;
            }
            case 2->{
                poi.tick(()->false);if(!MatchPoiMemory.ready(f.level))return false;point=new MatchPoiStorage(f.level);
                require(point.knownAtCapture(knownChunk)&&!point.knownAtCapture(coldChunk)&&!point.knownAtCapture(newChunk)&&!point.knownAtCapture(supplierChunk),
                        "actual point distinguishes loaded source from three cold metadata chunks");
                mutate();phase=3;
            }
            case 3->{readAll();phase=4;}
            case 4->{
                verifyFuture(cycle);
                if(cycle==0&&!otherReplayed)other=new MatchPoiStorage(f.level);
                start(point);
                if(other!=null){boolean rejected=false;try{other.beginRestoreFrozen();}catch(IllegalStateException expected){rejected=expected.getMessage().contains("Another POI point");}require(rejected,"one restore lease per worker, independent points remain valid");}
                phase=5;
            }
            case 5->{if(!drive())return false;readAll();phase=6;}
            case 6->{
                verifyPoint();
                if(other!=null&&!otherReplayed){start(other);phase=7;}
                else if(++cycle<3){mutate();phase=3;}else cleanup();
            }
            case 7->{if(!drive())return false;readAll();phase=8;}
            case 8->{verifyFuture(0);otherReplayed=true;work=other.closeAsync();phase=9;}
            case 9->{other=null;start(point);phase=5;}
            case 90->{
                poi.tick(()->false);if(before!=null&&!MatchPoiMemory.ready(f.level))return false;
                game.server().tickRateManager().setFrozen(true);if(before!=null)before.restoreFrozen();
                var writes=new ArrayList<CompletableFuture<Void>>();original.forEach((pos,data)->writes.add(storage.write(pos,data.map(CompoundTag::copy).orElse(null))));
                work=CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new)).thenCompose(done->storage.synchronize(true));phase=91;
            }
            case 91->{
                game.server().tickRateManager().setFrozen(initialFreeze);closed=true;
                if(failure!=null)throw new IllegalStateException("Native POI storage scene failed after completed cleanup",failure);
                return true;
            }
            default->throw new IllegalStateException("Unknown POI storage fixture phase "+phase);
        }
        return false;
    }
    private void mutate(){
        require(poi.release(source)&&poi.add(extra,meeting)!=null,"future native source ticket/index mutation");
        require(poi.getType(coldPos).orElseThrow().is(PoiTypes.HOME),"cold native read sees initial home before future mutation");
        poi.remove(coldPos);require(poi.add(coldPos,meeting)!=null&&poi.take(type->type.is(PoiTypes.MEETING),(type,pos)->pos.equals(coldPos),coldPos,0).isPresent(),"future native cold type/ticket mutation");
        require(poi.add(newPos,home)!=null&&poi.take(type->type.is(PoiTypes.HOME),(type,pos)->pos.equals(newPos),newPos,0).isPresent(),"native creation in originally absent cold chunk");
        poi.flushAll();
        work=storage.write(supplierChunk,()->tag(100+cycle)).thenCompose(done->storage.synchronize(true));
    }
    private void start(MatchPoiStorage owner){
        game.server().tickRateManager().setFrozen(true);replay=owner.beginRestoreFrozen();
        suppressedSupplier=new AtomicInteger();
        suppressed=storage.write(newChunk,()->{suppressedSupplier.incrementAndGet();return tag(666);});
        require(!suppressed.isDone(),"ordinary save is held outside native worker during owned restoration");
    }
    private boolean drive(){
        if(!replay.tickFrozen()||!replay.resumeFrozen())return false;
        require(suppressed.isDone()&&suppressedSupplier.get()==0,"discarded intermediate-timeline supplier never runs and is acknowledged on resume");
        game.server().tickRateManager().setFrozen(initialFreeze);return true;
    }
    private void readAll(){
        reads=new LinkedHashMap<>();for(var pos:chunks)reads.put(pos,storage.read(pos).thenApply(data->data.map(CompoundTag::copy)));
        work=CompletableFuture.allOf(reads.values().toArray(CompletableFuture[]::new));
    }
    private void verifyFuture(int iteration){
        require(record(reads.get(knownChunk).join().orElseThrow(),source).freeTickets()==1
                &&record(reads.get(knownChunk).join().orElseThrow(),extra).poiType().is(PoiTypes.MEETING),"native disk contains actual changed known section");
        require(record(reads.get(coldChunk).join().orElseThrow(),coldPos).poiType().is(PoiTypes.MEETING)
                &&record(reads.get(newChunk).join().orElseThrow(),newPos).freeTickets()==0
                &&reads.get(supplierChunk).join().orElseThrow().getIntOr("proof",-1)==100+iteration,"future cold and supplier writes genuinely reached native storage");
    }
    private void verifyPoint(){
        var known=reads.get(knownChunk).join().orElseThrow();
        require(record(known,source).freeTickets()==0&&section(known,source).records().stream().noneMatch(value->value.pos().equals(extra)),"captured loaded memory was natively encoded back to disk");
        require(reads.get(coldChunk).join().orElseThrow().equals(coldTag())&&reads.get(newChunk).join().isEmpty()
                &&reads.get(supplierChunk).join().orElseThrow().getIntOr("proof",-1)==23,"first cold old tag including unknown fields, original absence and supplier tag restored");
        require(poi.getType(coldPos).orElseThrow().is(PoiTypes.HOME)&&poi.getType(newPos).isEmpty()
                &&poi.getInRange(type->type.is(PoiTypes.HOME),source,0,PoiManager.Occupancy.IS_OCCUPIED).count()==1,"native cache reload agrees with restored cold files and loaded tickets");
    }
    private CompoundTag coldTag(){
        var packed=new PoiSection.Packed(true,List.of(new PoiRecord.Packed(coldPos,home,1)));
        var sections=new CompoundTag();sections.put(Integer.toString(SectionPos.blockToSectionCoord(coldPos.getY())),PoiSection.Packed.CODEC.encodeStart(f.level.registryAccess().createSerializationContext(NbtOps.INSTANCE),packed).getOrThrow());
        var tag=tag(17);tag.put("Sections",sections);return tag;
    }
    private PoiSection.Packed section(CompoundTag chunk,BlockPos pos){return PoiSection.Packed.CODEC.parse(f.level.registryAccess().createSerializationContext(NbtOps.INSTANCE),chunk.getCompoundOrEmpty("Sections").get(Integer.toString(SectionPos.blockToSectionCoord(pos.getY())))).getOrThrow();}
    private PoiRecord.Packed record(CompoundTag chunk,BlockPos pos){return section(chunk,pos).records().stream().filter(value->value.pos().equals(pos)).findFirst().orElseThrow();}
    private static CompoundTag tag(int proof){var tag=new CompoundTag();tag.putInt("DataVersion",SharedConstants.getCurrentVersion().dataVersion().version());tag.putInt("proof",proof);return tag;}
    private void cleanup(){
        var closes=new ArrayList<CompletableFuture<Void>>();if(other!=null)closes.add(other.closeAsync());if(point!=null)closes.add(point.closeAsync());
        work=CompletableFuture.allOf(closes.toArray(CompletableFuture[]::new));phase=90;
    }
    @Override public void close(){
        if(closed)return;if(other!=null)other.closeAsync();if(point!=null)point.closeAsync();game.server().tickRateManager().setFrozen(initialFreeze);
    }
}
