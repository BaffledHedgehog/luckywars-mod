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
    private CompletableFuture<LevelChunkSection[][]> transformed;
    private final LevelChunkSection empty;
    private LevelChunkSection[][] destinationSections;
    private final MapLighting.Update lighting;
    private int captured;
    private boolean published,closed;
    private long visits,writes,comparisons,maxStepNanos;
    private final long created=System.nanoTime();

    MapRotation(GameController game,ServerPlayer author,BlockPos origin){
        this.game=game;maps=game.maps();level=author.level();this.author=author;
        empty=new LevelChunkSection(net.minecraft.world.level.chunk.PalettedContainerFactory.create(level.registryAccess()));
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
            if(transformed==null){
                // The worker sees detached section palettes and copied NBT only.
                transformed=maps.computeFromSnapshot(this::prepareDestination);
                return false;
            }
            if(!transformed.isDone())return false;
            destinationSections=transformed.join();
            if(!published){
                // Validation and publication share a server tick: no live reader can observe a half map.
                validateSource();
                commitBlocks();
                rotateEntities();
                var origin=new BlockPos(xStart+160,160,zStart+160);
                maps.rotateSpawnState(origin);game.rotateMapSpawns(origin);
                maps.rotationPhysics().rotate(origin);
                scheduleRotatedFluids();
                lighting.finishWrites();published=true;
            }
            if(!lighting.tick(deadline))return false;
            maxStepNanos=Math.max(maxStepNanos,System.nanoTime()-started);
            SkyWars2.LOGGER.info("Map rotation complete cells={} comparisons={} writes={} elapsedMs={} maxWorkTickMs={}",visits,comparisons,writes,(System.nanoTime()-created)/1_000_000.0,maxStepNanos/1_000_000.0);
            finish();return true;
        }catch(RuntimeException failure){
            SkyWars2.LOGGER.error("Native map rotation failed; see publication phase",failure);cancel();return true;
        }finally{maxStepNanos=Math.max(maxStepNanos,System.nanoTime()-started);}
    }
    private LevelChunkSection[][] prepareDestination(){
        var result=new LevelChunkSection[sections.length][SIZE/16];
        for(int i=0;i<result.length;i++){
            int cx=minChunkX+i/depth,cz=minChunkZ+i%depth;
            for(int sy=0;sy<SIZE/16;sy++){
                var copy=sections[i][sy]==null?null:sections[i][sy].copy();
                for(int dy=0;dy<16;dy++)for(int dz=0;dz<16;dz++)for(int dx=0;dx<16;dx++){
                    int x=cx*16+dx,y=sy*16+dy,z=cz*16+dz;
                    if(x<xStart||x>=xStart+SIZE||z<zStart||z>=zStart+SIZE)continue;
                    var wanted=state(x,z-zStart,zStart+SIZE-1-y);
                    if(copy==null&&wanted==Blocks.AIR.defaultBlockState())continue;
                    if(copy==null)copy=empty.copy();
                    copy.setBlockState(dx,dy,dz,wanted,false);
                }
                result[i][sy]=copy;
            }
        }
        return result;
    }
    private void validateSource(){
        // Rebase only changed cells against the completed detached transform. Ordinary fluid,
        // player and redstone edits therefore do not cancel or starve an accepted turn.
        entities.clear();
        for(int i=0;i<sections.length;i++){
            var live=chunk(i);
            for(int sy=0;sy<SIZE/16;sy++){
                var current=live.getSection(live.getSectionIndex(sy*16));var saved=sections[i][sy];
                if(saved==null&&!current.maybeHas(s->s!=Blocks.AIR.defaultBlockState()))continue;
                for(int cell=0;cell<4096;cell++){
                    int x=live.getPos().x()*16+(cell&15),z=live.getPos().z()*16+(cell>>4&15),y=sy*16+(cell>>8);
                    if(x<xStart||x>=xStart+SIZE||z<zStart||z>=zStart+SIZE)continue;
                    var now=current.getBlockState(x&15,y&15,z&15);
                    if(now==state(x,y,z))continue;
                    if(saved==null)sections[i][sy]=saved=empty.copy();
                    saved.setBlockState(x&15,y&15,z&15,now,false);
                    int ty=SIZE-1-(z-zStart),tz=zStart+y,ti=((x>>4)-minChunkX)*depth+(tz>>4)-minChunkZ;
                    var target=destinationSections[ti][ty>>4];
                    if(target==null)destinationSections[ti][ty>>4]=target=empty.copy();
                    target.setBlockState(x&15,ty&15,tz&15,now,false);
                }
            }
            for(var pos:live.getBlockEntitiesPos())if(inside(pos)){
                var nbt=live.getBlockEntityNbtForSaving(pos,level.registryAccess());
                if(nbt!=null)entities.put(pos.immutable(),nbt.copy());
            }
        }
    }
    private void commitBlocks(){
        var pos=new BlockPos.MutableBlockPos();
        for(int i=0;i<sections.length;i++){
            var live=chunk(i);
            for(int sy=0;sy<SIZE/16;sy++){
                var current=live.getSection(live.getSectionIndex(sy*16));var wanted=destinationSections[i][sy];
                if(wanted==null&&!current.maybeHas(s->s!=Blocks.AIR.defaultBlockState())){
                    visits+=(Math.min(xStart+SIZE,live.getPos().x()*16+16)-Math.max(xStart,live.getPos().x()*16))
                            *(Math.min(zStart+SIZE,live.getPos().z()*16+16)-Math.max(zStart,live.getPos().z()*16))*16;continue;
                }
                for(int cell=0;cell<4096;cell++){
                    int x=live.getPos().x()*16+(cell&15),z=live.getPos().z()*16+(cell>>4&15),y=sy*16+(cell>>8);
                    pos.set(x,y,z);if(!inside(pos))continue;visits++;comparisons++;
                    var state=wanted==null?Blocks.AIR.defaultBlockState():wanted.getBlockState(x&15,y&15,z&15);
                    if(current.getBlockState(x&15,y&15,z&15)==state)continue;
                    lighting.changed(pos);
                    game.items().indirectKills().trapChanges(level,cause,()->maps.restoreSnapshot(level,pos,new MapManager.BlockSnapshot(state,null),FLAGS));writes++;
                }
            }
        }
        for(var entry:entities.entrySet()){
            var target=blockDestination(entry.getKey(),new BlockPos(xStart+160,160,zStart+160));
            var nbt=entry.getValue().copy();nbt.putInt("x",target.getX());nbt.putInt("y",target.getY());nbt.putInt("z",target.getZ());
            var snapshot=new MapManager.BlockSnapshot(state(entry.getKey().getX(),entry.getKey().getY(),entry.getKey().getZ()),nbt);
            lighting.changed(target);
            game.items().indirectKills().trapChanges(level,cause,()->maps.restoreSnapshot(level,target,snapshot,FLAGS));
            level.sendBlockUpdated(target,snapshot.state(),snapshot.state(),Block.UPDATE_CLIENTS);
        }
    }
    private void scheduleRotatedFluids(){
        var pos=new BlockPos.MutableBlockPos();
        for(int i=0;i<destinationSections.length;i++)for(int sy=0;sy<SIZE/16;sy++){
            var section=destinationSections[i][sy];
            if(section==null||!section.maybeHas(s->!s.getFluidState().isEmpty()))continue;
            for(int cell=0;cell<4096;cell++){
                int x=(minChunkX+i/depth)*16+(cell&15),z=(minChunkZ+i%depth)*16+(cell>>4&15),y=sy*16+(cell>>8);
                pos.set(x,y,z);if(!inside(pos))continue;
                var fluid=section.getBlockState(x&15,y&15,z&15).getFluidState();
                if(!fluid.isEmpty())level.scheduleTick(pos.immutable(),fluid.getType(),fluid.getType().getTickDelay(level));
            }
        }
    }
    static Vec3 rotateVector(Vec3 v){return new Vec3(v.x,-v.z,v.y);}
    static float[] rotateLook(float yaw,float pitch){
        var look=rotateVector(Vec3.directionFromRotation(pitch,yaw));
        return new float[]{(float)Math.toDegrees(Math.atan2(-look.x,look.z)),(float)Math.toDegrees(Math.asin(-Math.clamp(look.y,-1,1)))};
    }
    private record Body(Entity entity,Vec3 at,Vec3 velocity,float yaw,float pitch,Entity vehicle){}
    private void rotateEntities(){
        var bounds=new AABB(xStart,0,zStart,xStart+SIZE,SIZE,zStart+SIZE);
        var bodies=new LinkedHashMap<Entity,Body>();
        for(var entity:level.getAllEntities())if(!entity.isRemoved()&&entity.getBoundingBox().intersects(bounds))save(bodies,entity);
        // Complete each selected vehicle graph once, avoiding native recursive passenger double transforms.
        for(var body:List.copyOf(bodies.values()))for(var member:body.entity.getRootVehicle().getSelfAndPassengers().toList())save(bodies,member);
        for(var body:bodies.values())body.entity.stopRiding();
        try{
            for(var body:bodies.values()){
                var point=entityDestination(body.at,new BlockPos(xStart+160,160,zStart+160));
                var velocity=rotateVector(body.velocity);var angles=rotateLook(body.yaw,body.pitch);
                if(body.entity instanceof ServerPlayer player)
                    player.teleportTo(level,point.x,point.y,point.z,TeleportMomentum.positionFlags(Set.of()),angles[0],angles[1],true);
                else {
                    // Same-dimension placement preserves identity, mob owners and every entity subtype.
                    body.entity.teleportTo(point.x,point.y,point.z);
                    body.entity.setYRot(angles[0]);body.entity.setXRot(angles[1]);
                    if(body.entity instanceof LivingEntity living){living.setYHeadRot(angles[0]);living.yBodyRot=angles[0];}
                }
                body.entity.setDeltaMovement(velocity);body.entity.needsSync=true;
                if(body.entity instanceof LivingEntity living)game.items().indirectKills().teleported(living,author,true);
            }
        }finally{for(var body:bodies.values())if(body.vehicle!=null&&!body.vehicle.isRemoved()&&!body.entity.isRemoved())body.entity.startRiding(body.vehicle,true,false);}
    }
    private void save(Map<Entity,Body> into,Entity entity){into.putIfAbsent(entity,new Body(entity,entity.position(),entity.getDeltaMovement(),entity.getYRot(),entity.getXRot(),entity.getVehicle()));}
    private void finish(){closed=true;entities.clear();if(transformed==null||transformed.isDone())java.util.Arrays.fill(sections,null);destinationSections=null;maps.rotationFinished(this);}
    @Override public void cancel(){if(closed)return;lighting.cancel();finish();}
}
