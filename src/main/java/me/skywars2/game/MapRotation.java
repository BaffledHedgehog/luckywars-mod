package me.skywars2.game;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** MapManager's accepted X-axis transform, advanced by the existing terrain dispatcher. */
final class MapRotation implements LbcStonekivi.Work {
    static final int SIZE=320;
    private static final int FLAGS=Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE|Block.UPDATE_SUPPRESS_DROPS
            |Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS|Block.UPDATE_SKIP_ON_PLACE;
    private final GameController game;
    private final MapManager maps;
    private final ServerLevel level;
    private final ServerPlayer author;
    private final String epoch;
    private final LbcIndirectKills.Owner cause;
    private final int xStart,zStart,minChunkX,minChunkZ,width,depth;
    private final LevelChunkSection[][] sections;
    private final Map<BlockPos,CompoundTag> entities=new LinkedHashMap<>();
    private final CompletableFuture<?> prepared;
    private final MapLighting.Update lighting;
    private int captured,chunkIndex,sectionIndex,cellIndex;
    private java.util.Iterator<Map.Entry<BlockPos,CompoundTag>> entityCursor;
    private boolean published,closed;
    private long visits,writes,comparisons,maxStepNanos;
    private final long created=System.nanoTime();

    MapRotation(GameController game,ServerPlayer author,BlockPos origin){
        this.game=game;maps=game.maps();level=author.level();this.author=author;
        epoch=game.items().epoch();cause=game.items().indirectKills().wildCause(author);
        xStart=origin.getX()-SIZE/2;zStart=origin.getZ()-SIZE/2;
        minChunkX=xStart>>4;minChunkZ=zStart>>4;
        width=((xStart+SIZE-1)>>4)-minChunkX+1;depth=((zStart+SIZE-1)>>4)-minChunkZ+1;
        sections=new LevelChunkSection[width*depth][];
        var futures=new ArrayList<CompletableFuture<?>>();
        for(int x=0;x<width;x++)for(int z=0;z<depth;z++)futures.add(maps.prepareChunk(level,new ChunkPos(minChunkX+x,minChunkZ+z)));
        prepared=CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
        lighting=new MapLighting.Update(level);
    }
    static BlockPos blockDestination(BlockPos source,BlockPos origin){
        int start=origin.getZ()-SIZE/2;
        return new BlockPos(source.getX(),SIZE-1-(source.getZ()-start),start+source.getY());
    }
    static Vec3 entityDestination(Vec3 source,BlockPos origin){
        double pivotZ=origin.getZ();
        return new Vec3(source.x,160-(source.z-pivotZ),pivotZ+source.y-160);
    }
    long visits(){return visits;}
    long writes(){return writes;}
    long comparisons(){return comparisons;}
    long maxStepNanos(){return maxStepNanos;}
    @Override public boolean ready(){return prepared.isDone();}
    private LevelChunk chunk(int index){
        var chunk=level.getChunkSource().getChunkNow(minChunkX+index/depth,minChunkZ+index%depth);
        if(chunk==null)throw new IllegalStateException("Map rotation lost a prepared source chunk");
        return chunk;
    }
    private void capture(int index){
        var chunk=chunk(index);var copy=new LevelChunkSection[SIZE/16];
        for(int y=0;y<copy.length;y++){
            var section=chunk.getSection(chunk.getSectionIndex(y*16));
            // Preserve cave/void air as distinct states too. No per-cell records for uniform air.
            if(section.maybeHas(s->s!=Blocks.AIR.defaultBlockState()))copy[y]=section.copy();
        }
        sections[index]=copy;
        for(var pos:chunk.getBlockEntitiesPos())if(inside(pos)){
            var nbt=chunk.getBlockEntityNbtForSaving(pos,level.registryAccess());
            if(nbt!=null)entities.put(pos.immutable(),nbt.copy());
        }
    }
    private boolean inside(BlockPos pos){return pos.getX()>=xStart&&pos.getX()<xStart+SIZE
            &&pos.getZ()>=zStart&&pos.getZ()<zStart+SIZE&&pos.getY()>=0&&pos.getY()<SIZE;}
    private LevelChunkSection section(int x,int y,int z){
        return sections[((x>>4)-minChunkX)*depth+(z>>4)-minChunkZ][y>>4];
    }
    private BlockState state(int x,int y,int z){var section=section(x,y,z);return section==null
            ?Blocks.AIR.defaultBlockState():section.getBlockState(x&15,y&15,z&15);}
    /** At most four source palettes feed a destination section, including unaligned cube edges. */
    private boolean sourceAir(int chunkX,int chunkZ,int sy){
        int sx=Math.max(xStart,chunkX*16),z0=Math.max(zStart,chunkZ*16),z1=Math.min(zStart+SIZE-1,chunkZ*16+15);
        int y0=z0-zStart,y1=z1-zStart,sourceZ0=zStart+SIZE-1-(sy*16+15),sourceZ1=zStart+SIZE-1-sy*16;
        for(int y=y0>>4;y<=y1>>4;y++)for(int z=sourceZ0>>4;z<=sourceZ1>>4;z++)
            if(section(sx,y*16,z*16)!=null)return false;
        return true;
    }
    @Override public boolean step(){
        if(closed)return true;
        if(!game.playing()||!game.items().currentEpoch(epoch)){cancel();return true;}
        if(game.items().timeStop().stopped(level))return false;
        long started=System.nanoTime(),deadline=Math.max(EffectTerrain.deadlineNanos(),started+1_000_000);
        try{
            prepared.join(); // ready() guarantees completion; never block the server on chunk IO.
            while(captured<sections.length){
                capture(captured++);
                if(System.nanoTime()>=deadline)return false;
            }
            int budget=1_048_576;
            var destination=new BlockPos.MutableBlockPos();
            while(chunkIndex<sections.length&&budget>0&&System.nanoTime()<deadline){
                var chunk=chunk(chunkIndex);int cx=chunk.getPos().x(),cz=chunk.getPos().z();
                var current=chunk.getSection(chunk.getSectionIndex(sectionIndex*16));
                if(cellIndex==0&&!current.maybeHas(s->s!=Blocks.AIR.defaultBlockState())&&sourceAir(cx,cz,sectionIndex)){
                    int count=(Math.min(xStart+SIZE,cx*16+16)-Math.max(xStart,cx*16))
                            *(Math.min(zStart+SIZE,cz*16+16)-Math.max(zStart,cz*16))*16;
                    visits+=count;sectionIndex++;
                }else{
                    while(cellIndex<4096&&budget>0){
                        if((cellIndex&63)==0&&System.nanoTime()>=deadline)return false;
                        int x=cx*16+(cellIndex&15),z=cz*16+(cellIndex>>4&15),y=sectionIndex*16+(cellIndex>>8);
                        cellIndex++;
                        if(x<xStart||x>=xStart+SIZE||z<zStart||z>=zStart+SIZE)continue;
                        visits++;comparisons++;budget--;
                        int sourceY=z-zStart,sourceZ=zStart+SIZE-1-y;
                        var wanted=state(x,sourceY,sourceZ);
                        if(current.getBlockState(x&15,y&15,z&15)==wanted)continue;
                        destination.set(x,y,z);lighting.changed(destination);
                        game.items().indirectKills().trapChanges(level,cause,
                                ()->maps.restoreSnapshot(level,destination,new MapManager.BlockSnapshot(wanted,null),FLAGS));writes++;
                    }
                    if(cellIndex==4096){cellIndex=0;sectionIndex++;}
                }
                if(sectionIndex==SIZE/16){sectionIndex=0;chunkIndex++;}
            }
            if(chunkIndex<sections.length)return false;
            if(!published){
                // Same-state chests must be replaced from the clipboard too; source inventories are immutable copies.
                if(entityCursor==null)entityCursor=entities.entrySet().iterator();
                while(entityCursor.hasNext()){
                    if(System.nanoTime()>=deadline)return false;
                    var entry=entityCursor.next();
                    var target=blockDestination(entry.getKey(),new BlockPos(xStart+160,160,zStart+160));
                    var nbt=entry.getValue().copy();nbt.putInt("x",target.getX());nbt.putInt("y",target.getY());nbt.putInt("z",target.getZ());
                    var snapshot=new MapManager.BlockSnapshot(state(entry.getKey().getX(),entry.getKey().getY(),entry.getKey().getZ()),nbt);
                    lighting.changed(target);
                    game.items().indirectKills().trapChanges(level,cause,()->maps.restoreSnapshot(level,target,snapshot,FLAGS));
                    level.sendBlockUpdated(target,snapshot.state(),snapshot.state(),Block.UPDATE_CLIENTS);
                }
                lighting.finishWrites();published=true;
            }
            if(!lighting.tick(deadline))return false;
            rotateEntities();
            var origin=new BlockPos(xStart+160,160,zStart+160);maps.rotateSpawnState(origin);game.rotateMapSpawns(origin);
            maxStepNanos=Math.max(maxStepNanos,System.nanoTime()-started);
            SkyWars2.LOGGER.info("Map rotation complete cells={} comparisons={} writes={} elapsedMs={} maxWorkTickMs={}",visits,comparisons,writes,(System.nanoTime()-created)/1_000_000.0,maxStepNanos/1_000_000.0);
            finish();return true;
        }catch(RuntimeException failure){
            SkyWars2.LOGGER.error("Native map rotation failed; accepted partial writes retained",failure);cancel();return true;
        }finally{maxStepNanos=Math.max(maxStepNanos,System.nanoTime()-started);}
    }
    private record Body(Entity entity,Vec3 at,Vec3 velocity,float yaw,float pitch,Entity vehicle){}
    private void rotateEntities(){
        var bounds=new AABB(xStart,0,zStart,xStart+SIZE,SIZE,zStart+SIZE);
        var bodies=new LinkedHashMap<Entity,Body>();
        for(var entity:level.getAllEntities())if(!entity.isRemoved()&&entity.getBoundingBox().intersects(bounds))save(bodies,entity);
        if(!author.isRemoved()&&author.level()==level)save(bodies,author);
        // Complete each selected vehicle graph once, avoiding native recursive passenger double transforms.
        for(var body:List.copyOf(bodies.values()))for(var member:body.entity.getRootVehicle().getSelfAndPassengers().toList())save(bodies,member);
        for(var body:bodies.values())body.entity.stopRiding();
        try{
            for(var body:bodies.values()){
                var point=new Vec3(body.at.x,160-(body.at.z-(zStart+160)),zStart+body.at.y);
                if(body.entity instanceof ServerPlayer player)
                    player.teleportTo(level,point.x,point.y,point.z,TeleportMomentum.positionFlags(Set.of()),body.yaw,body.pitch,true);
                else body.entity.teleport(new TeleportTransition(level,point,body.velocity,body.yaw,body.pitch,TeleportTransition.DO_NOTHING));
                body.entity.setDeltaMovement(body.velocity);
                if(body.entity instanceof LivingEntity living)game.items().indirectKills().teleported(living,author,true);
            }
        }finally{for(var body:bodies.values())if(body.vehicle!=null&&!body.vehicle.isRemoved()&&!body.entity.isRemoved())body.entity.startRiding(body.vehicle,true,false);}
    }
    private void save(Map<Entity,Body> into,Entity entity){into.putIfAbsent(entity,new Body(entity,entity.position(),entity.getDeltaMovement(),entity.getYRot(),entity.getXRot(),entity.getVehicle()));}
    private void finish(){closed=true;entities.clear();java.util.Arrays.fill(sections,null);maps.rotationFinished(this);}
    @Override public void cancel(){if(closed)return;lighting.cancel();finish();}
}
