package me.skywars2.game;

import java.util.*;
import java.util.concurrent.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.storage.SimpleRegionStorage;

/** Real block discovery, occupied tickets and native files through the actual full rollback coordinator. */
final class VanityPoiCoordinatorChecks implements AutoCloseable {
    private final GameController game;
    private final WoolBucketChecks.Fixture f;
    private final List<World> worlds=new ArrayList<>();
    private final boolean frozen;
    private final int started;
    private CompletableFuture<?> work=CompletableFuture.completedFuture(null);
    private int phase,cycle;
    private boolean closed;
    private Throwable failure;
    private final class World {
        final ServerLevel level;
        final BlockPos source,extra;
        final ChunkPos chunk;
        final PoiManager poi;
        final SimpleRegionStorage storage;
        final Map<BlockPos,MapManager.BlockSnapshot> blocks=new LinkedHashMap<>();
        MatchPoiMemory before;
        CompletableFuture<Optional<CompoundTag>> read;
        Optional<CompoundTag> original;
        World(ServerLevel level,BlockPos pos){
            this.level=level;source=pos;extra=pos.east(2);chunk=ChunkPos.containing(pos);poi=level.getPoiManager();
            storage=(SimpleRegionStorage)WoolBucketChecks.read(poi,"simpleRegionStorage");
            require(ChunkPos.containing(extra).equals(chunk),"both native discovery cells in one owned chunk");
        }
        void set(BlockPos pos,boolean composter){
            blocks.computeIfAbsent(pos,key->game.maps().snapshot(level,key));
            require(level.setBlock(pos,composter?Blocks.COMPOSTER.defaultBlockState():Blocks.AIR.defaultBlockState(),2|16|32),"real block state changed");
        }
        void read(){read=storage.read(chunk).thenApply(tag->tag.map(CompoundTag::copy));}
        void verify(){
            require(level.getBlockState(source).is(Blocks.COMPOSTER)&&level.getBlockState(extra).is(Blocks.AIR),"terrain restored in "+level.dimension());
            require(poi.getType(source).orElseThrow().is(PoiTypes.FARMER)&&poi.getType(extra).isEmpty(),"actual native block discovery restored in "+level.dimension());
            require(poi.getInRange(type->type.is(PoiTypes.FARMER),source,0,PoiManager.Occupancy.IS_OCCUPIED).count()==1,"point ticket ownership restored in "+level.dimension());
        }
        void verifyDisk(){
            var tag=read.join().orElseThrow();
            var packed=PoiSection.Packed.CODEC.parse(level.registryAccess().createSerializationContext(NbtOps.INSTANCE),
                    tag.getCompoundOrEmpty("Sections").get(Integer.toString(SectionPos.blockToSectionCoord(source.getY())))).getOrThrow();
            require(packed.records().stream().anyMatch(row->row.pos().equals(source)&&row.poiType().is(PoiTypes.FARMER)&&row.freeTickets()==0)
                    &&packed.records().stream().noneMatch(row->row.pos().equals(extra)),"native POI file agrees with full coordinator in "+level.dimension());
        }
    }
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Vanity POI coordinator: "+why);}
    VanityPoiCoordinatorChecks(GameController game,WoolBucketChecks.Fixture f){
        this.game=game;this.f=f;frozen=game.server().tickRateManager().isFrozen();started=game.server().getTickCount();
        require(!game.vanity().hasSnapshot(),"exclusive full point");
        // Keep the cells away from the shared native task arena and in a single chunk.
        var home=BlockPos.containing(f.home);int x=(home.getX()>>4)*16+4,z=(home.getZ()>>4)*16+4;
        worlds.add(new World(f.level,new BlockPos(x,home.getY()+12,z)));
        worlds.add(new World(Objects.requireNonNull(game.server().getLevel(LbcHedgehogDimensions.NEXUS)),new BlockPos(4,180,4)));
        for(var world:worlds)game.maps().prepareChunk(world.level,world.chunk);
    }
    boolean step(){
        f.client.acknowledge();f.audience.acknowledge();if(closed)return true;
        try{require(game.server().getTickCount()-started<1000,"bounded async coordinator scene phase="+phase+" status="+game.vanity().status());return advance();}
        catch(Throwable error){if(phase>=90)throw new IllegalStateException("POI coordinator cleanup failed",error);failure=error;cleanup();return false;}
    }
    private boolean advance(){
        if(!work.isDone())return false;work.join();
        switch(phase){
            case 0->{
                for(var world:worlds){
                    if(world.level.getChunkSource().getChunkNow(world.chunk.x(),world.chunk.z())==null)return false;
                    world.poi.tick(()->false);if(!MatchPoiMemory.ready(world.level))return false;
                }
                for(var world:worlds){world.before=new MatchPoiMemory(world.level);world.read();}
                work=CompletableFuture.allOf(worlds.stream().map(w->w.read).toArray(CompletableFuture[]::new));phase=1;
            }
            case 1->{
                for(var world:worlds){world.original=world.read.join();require(world.level.getBlockState(world.source).isAir()&&world.level.getBlockState(world.extra).isAir(),"isolated AIR cells");world.set(world.source,true);}
                phase=2;
            }
            case 2->{
                for(var world:worlds)if(world.poi.getType(world.source).isEmpty())return false;
                for(var world:worlds){require(world.poi.take(type->type.is(PoiTypes.FARMER),(type,pos)->pos.equals(world.source),world.source,0).isPresent(),"actual native occupied discovered block");world.poi.flushAll();}
                work=CompletableFuture.allOf(worlds.stream().map(w->w.storage.synchronize(true)).toArray(CompletableFuture[]::new));phase=3;
            }
            case 3->{require(game.vanity().requestCapture(f.owner,false),"real coordinator capture request");phase=4;}
            case 4->{
                if(!game.vanity().hasSnapshot()||game.vanity().capturePreparing()||game.vanity().busy())return false;
                for(var world:worlds){world.verify();world.set(world.source,false);world.set(world.extra,true);}
                phase=5;
            }
            case 5->{
                for(var world:worlds)if(world.poi.getType(world.source).isPresent()||world.poi.getType(world.extra).isEmpty())return false;
                for(var world:worlds)world.poi.flushAll();
                work=CompletableFuture.allOf(worlds.stream().map(w->w.storage.synchronize(true)).toArray(CompletableFuture[]::new));phase=6;
            }
            case 6->{require(game.vanity().requestRestoreForCheck(),"real coordinator restore request");phase=7;}
            case 7->{
                require(!game.vanity().status().startsWith("FAILED"),"full coordinator remains valid: "+game.vanity().status());
                if(game.vanity().busy())return false;
                require(game.server().tickRateManager().isFrozen()==frozen,"native simulation resumes after storage and body publication");
                for(var world:worlds){world.verify();world.read();}
                work=CompletableFuture.allOf(worlds.stream().map(w->w.read).toArray(CompletableFuture[]::new));phase=8;
            }
            case 8->{
                for(var world:worlds)world.verifyDisk();
                if(++cycle<3){phase=cycle==2?3:4;return false;}
                cleanup();
            }
            case 90->{
                game.server().tickRateManager().setFrozen(true);
                for(var world:worlds){
                    world.blocks.forEach((pos,state)->game.maps().restoreSnapshot(world.level,pos,state,2|16|32));
                    world.poi.tick(()->false);if(!MatchPoiMemory.ready(world.level))return false;
                }
                for(var world:worlds)if(world.before!=null)world.before.restoreFrozen();
                work=CompletableFuture.allOf(worlds.stream().filter(w->w.original!=null)
                        .map(w->w.storage.write(w.chunk,w.original.map(CompoundTag::copy).orElse(null))).toArray(CompletableFuture[]::new));phase=91;
            }
            case 91->{work=CompletableFuture.allOf(worlds.stream().map(w->w.storage.synchronize(true)).toArray(CompletableFuture[]::new));phase=92;}
            case 92->{
                closed=true;game.server().tickRateManager().setFrozen(frozen);
                if(failure!=null)throw new IllegalStateException("Native POI full coordinator scene",failure);
                SkyWars2.LOGGER.info("[check] Native POI full coordinator: two dimensions, actual composter block discovery, occupied tickets, disk/memory agreement, repeated rewind and replacement, awaited journal retirement");return true;
            }
            default->throw new IllegalStateException("Unknown coordinator scene phase");
        }
        return false;
    }
    private void cleanup(){game.vanity().stop();work=game.vanity().poiRetirement();phase=90;}
    @Override public void close(){if(closed)return;game.vanity().stop();game.server().tickRateManager().setFrozen(frozen);}
}
