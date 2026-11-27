package me.skywars2.game;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import me.skywars2.SkyWars2;
import me.skywars2.game.effects.magic.LbcExplosionSuppression;
import me.skywars2.game.match.events.NuclearTerrain;
import me.skywars2.game.world.maps.MapManager;
import me.skywars2.game.world.terrain.LbcDropBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Exact old/new masks and native strict-write A/B on two owned sections; timings are observations. */
public final class NuclearTerrainBatchChecks {
    private NuclearTerrainBatchChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Nuclear terrain batch: "+why);}
    private record Section(int x,int y,int z,long[] mask){}
    private static Object read(Object owner,String name)throws ReflectiveOperationException{
        var field=owner.getClass().getDeclaredField(name);field.setAccessible(true);return field.get(owner);
    }
    private static Object invoke(Object owner,String name)throws ReflectiveOperationException{
        var method=owner.getClass().getDeclaredMethod(name);method.setAccessible(true);return method.invoke(owner);
    }
    public static NuclearTerrain.Point queuedPoint(GameController game){
        try{
            for(var work:game.items().stonekivi().queuedSnapshot())if(work.getClass().getName().equals("me.skywars2.game.match.events.LbcNukeEvent$Job"))
                for(var task:(Collection<?>)read(work,"terrain"))if(read(task,"shape") instanceof NuclearTerrain.Point point)return point;
        }catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
        throw new IllegalStateException("No actual native queued point terrain");
    }
    @SuppressWarnings("unchecked")
    private static List<Section> sections(NuclearTerrain.Geometry geometry)throws ReflectiveOperationException{
        var result=new ArrayList<Section>();for(var part:(List<Object>)read(geometry,"sections"))
            result.add(new Section((int)invoke(part,"x"),(int)invoke(part,"y"),(int)invoke(part,"z"),(long[])invoke(part,"mask")));
        return result;
    }
    private static Map<Long,long[]> referenceMasks(NuclearTerrain.Point point,long seed)throws ReflectiveOperationException{
        var masks=new it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<BitSet>();
        for(int i=0;i<point.cubes.size();i++){
            Object cube=point.cubes.get(i);var low=(BlockPos)invoke(cube,"low");float integrity=(float)invoke(cube,"integrity");
            long salt=seed^((i+1)*0x9e3779b97f4a7c15L);
            // Preserved pre-change per-voxel algorithm, independent of section traversal.
            for(int y=0;y<48;y++)for(int z=0;z<48;z++)for(int x=0;x<48;x++){
                int bx=low.getX()+x,by=low.getY()+y,bz=low.getZ()+z;
                if(NuclearTerrain.unit(salt^BlockPos.asLong(bx,by,bz))>=integrity)continue;
                long key=SectionPos.asLong(bx>>4,by>>4,bz>>4);var mask=masks.get(key);
                if(mask==null){mask=new BitSet(4096);masks.put(key,mask);}mask.set((bx&15)|((bz&15)<<4)|((by&15)<<8));
            }
        }
        var result=new HashMap<Long,long[]>();masks.forEach((key,mask)->result.put(key,mask.toLongArray()));return result;
    }
    private static void masks()throws ReflectiveOperationException{
        var prepare=NuclearTerrain.Point.class.getDeclaredMethod("prepareMask",Vec3.class,List.class,long.class);prepare.setAccessible(true);
        for(boolean marker:new boolean[]{false,true}){
            var at=new Vec3(-129.25,247.125,-31.75);long seed=marker?-771234129L:819917L;
            var point=NuclearTerrain.point(at,37,marker,seed);
            long started=System.nanoTime();var expected=referenceMasks(point,seed);long referenceNanos=System.nanoTime()-started;
            started=System.nanoTime();var actual=sections((NuclearTerrain.Geometry)prepare.invoke(null,at,point.cubes,seed));long actualNanos=System.nanoTime()-started;
            require(actual.size()==expected.size(),"same nonempty source sections marker="+marker);
            long cells=0;for(var section:actual){
                require(Arrays.equals(expected.get(SectionPos.asLong(section.x,section.y,section.z)),section.mask),"exact source mask at negative coordinates / rotated profile");
                for(long word:section.mask)cells+=Long.bitCount(word);
            }
            SkyWars2.LOGGER.info("[check] nuclear-mask A/B marker={} sections={} cells={} oldCpuMs={} sectionCpuMs={}",marker,actual.size(),cells,referenceNanos/1e6,actualNanos/1e6);
        }
    }
    private static NuclearTerrain.Point prepared(Vec3 at,List<Section> parts)throws ReflectiveOperationException{
        var type=Class.forName("me.skywars2.game.match.events.NuclearTerrain$Geometry$Section");
        var constructor=type.getDeclaredConstructor(int.class,int.class,int.class,long[].class);constructor.setAccessible(true);
        var nativeSections=new ArrayList<Object>();for(var part:parts)nativeSections.add(constructor.newInstance(part.x,part.y,part.z,part.mask));
        var geometryConstructor=NuclearTerrain.Geometry.class.getDeclaredConstructor(Vec3.class,List.class);geometryConstructor.setAccessible(true);
        var geometry=(NuclearTerrain.Geometry)geometryConstructor.newInstance(at,nativeSections);
        var point=NuclearTerrain.point(at);var future=NuclearTerrain.Point.class.getDeclaredField("preparation");future.setAccessible(true);
        future.set(point,CompletableFuture.completedFuture(geometry));return point;
    }
    /** Prior Point.clear cursor, including per-bit resident lookup and command-input placement. */
    private static final class Reference {
        final List<Section> parts;int section,word,visited;long bits,lookups;boolean loaded;
        Reference(List<Section> parts){this.parts=parts;}
        boolean step(ServerLevel level,LbcExplosionSuppression.Coverage coverage){
            visited=0;var pos=new BlockPos.MutableBlockPos();
            while(section<parts.size()&&visited<NuclearTerrain.CELLS_PER_STEP){
                var part=parts.get(section);lookups++;var chunk=level.getChunkSource().getChunkNow(part.x,part.z);
                if(chunk==null){advance();continue;}var slice=chunk.getSection(level.getSectionIndex(part.y<<4));
                if(slice.hasOnlyAir()||word>=part.mask.length){advance();continue;}
                if(!loaded){bits=part.mask[word];loaded=true;}if(bits==0){word++;loaded=false;continue;}
                int cell=(word<<6)+Long.numberOfTrailingZeros(bits);bits&=bits-1;visited++;
                pos.set((part.x<<4)+(cell&15),(part.y<<4)+(cell>>8),(part.z<<4)+((cell>>4)&15));
                if(!slice.getBlockState(cell&15,cell>>8,(cell>>4)&15).isAir()&&!coverage.blocks(pos))
                    LbcDropBlocks.setBlock(level,pos,Blocks.AIR.defaultBlockState(),true);
            }
            return section==parts.size();
        }
        void advance(){section++;word=0;bits=0;loaded=false;}
    }
    public static void run(GameController game){
        require(game.items().nukeEvent().jobCount()==0&&game.items().exasperato().pendingBlasts()==0,"exclusive native payload fixture");
        try{
            masks();
            try(var base=new ElementalGrimoireChecks.Fixture(game,true)){
                base.retain=true;base.initialize();var level=base.owner.level();
                var low=new BlockPos(base.owner.blockPosition().getX()&~15,
                        Math.min(level.getMaxY()-32,base.owner.blockPosition().getY()+32)&~15,base.owner.blockPosition().getZ()&~15);
                require(low.getY()>=level.getMinY()&&level.hasChunkAt(low),"owned resident two-section frame");
                var at=Vec3.atCenterOf(low.offset(8,16,8));var bounds=new net.minecraft.world.phys.AABB(
                        low.getX(),low.getY(),low.getZ(),low.getX()+16,low.getY()+32,low.getZ()+16);
                require(!LbcExplosionSuppression.coverage(game,level).contains(at),"unsuppressed benchmark origin");
                var before=new HashSet<UUID>();for(var entity:level.getAllEntities())before.add(entity.getUUID());
                var saved=new LinkedHashMap<BlockPos,MapManager.BlockSnapshot>();
                var cells=new ArrayList<BlockPos>();for(var pos:BlockPos.betweenClosed(low,low.offset(15,31,15))){
                    cells.add(pos.immutable());saved.put(pos.immutable(),game.maps().snapshot(level,pos));
                }
                long[] mask=new long[64];Arrays.fill(mask,-1L);mask[4]=mask[15]=mask[31]=0;mask[63]=0x8000000000000001L;
                var parts=List.of(new Section(low.getX()>>4,low.getY()>>4,low.getZ()>>4,mask),new Section(low.getX()>>4,(low.getY()>>4)+1,low.getZ()>>4,mask));
                var cause=game.items().indirectKills().wildCause(base.owner);long referenceNanos=0,nativeNanos=0,oldLookups=0;NuclearTerrain.Point.Progress progress=null;
                try{
                    List<BlockState> expected=null;
                    for(boolean optimized:new boolean[]{false,true}){
                        for(var pos:cells)LbcDropBlocks.setBlock(level,pos,Blocks.STONE.defaultBlockState(),true);
                        for(var pos:List.of(low.offset(1,0,0),low.offset(2,0,0),low.offset(3,0,0)))
                            LbcDropBlocks.setBlock(level,pos,pos.getX()==low.getX()+1?Blocks.BEDROCK.defaultBlockState():pos.getX()==low.getX()+2?Blocks.WATER.defaultBlockState():Blocks.GLOWSTONE.defaultBlockState(),true);
                        var chest=low.offset(7,0,0);LbcDropBlocks.setBlock(level,chest,Blocks.CHEST.defaultBlockState(),true);
                        ((net.minecraft.world.level.block.entity.ChestBlockEntity)level.getBlockEntity(chest)).setItem(0,new ItemStack(Items.DIAMOND,11));
                        LbcDropBlocks.setBlock(level,low.offset(13,0,0),Blocks.TNT.defaultBlockState(),true);
                        var point=optimized?prepared(at,parts):null;var reference=optimized?null:new Reference(parts);
                        var step=NuclearTerrain.Point.class.getDeclaredMethod("step",GameController.class,ServerLevel.class,me.skywars2.game.combat.LbcIndirectKills.Owner.class);step.setAccessible(true);
                        int steps=0;boolean done=false;long started=System.nanoTime();
                        while(!done){
                            if(optimized)done=(boolean)step.invoke(point,game,level,cause);
                            else{
                                boolean[] completed={false};var coverage=LbcExplosionSuppression.coverage(game,level);
                                game.items().indirectKills().withSupportCandidates(level,bounds,()->game.items().indirectKills().trapChanges(level,cause,()->completed[0]=reference.step(level,coverage)));
                                done=completed[0];require(reference.visited<=4096,"reference cell bound");
                            }
                            require(++steps<=4,"finite two-section native cursor");
                            if(optimized)require(point.progress().visitedCells()<=4096L*steps,"unchanged per-step cell bound");
                        }
                        long elapsed=System.nanoTime()-started;
                        require(level.getBlockEntity(chest)==null,"strict AIR retires actual chest without inventory callback");
                        if(!optimized){referenceNanos=elapsed;oldLookups=reference.lookups;expected=cells.stream().map(level::getBlockState).toList();}
                        else{
                            nativeNanos=elapsed;progress=point.progress();require(expected.equals(cells.stream().map(level::getBlockState).toList()),"identical authoritative native state after sparse mask/cursor writes");
                            require(progress.done()&&progress.slices()==2&&progress.sectionReads()<=3&&oldLookups>progress.sectionReads()*1000,
                                    "resident section discovery scales with sections and slices, not selected voxels");
                        }
                        require(level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,bounds).isEmpty()
                                &&level.getEntitiesOfClass(net.minecraft.world.entity.item.PrimedTnt.class,bounds).isEmpty(),"source strict clearing produces neither container loot nor primed TNT");
                    }
                    SkyWars2.LOGGER.info("[check] nuclear-clear A/B cells={} writes={} slices={} oldResidentLookups={} sectionReads={} oldCpuMs={} sectionCpuMs={}",
                            progress.visitedCells(),progress.changedBlocks(),progress.slices(),oldLookups,progress.sectionReads(),referenceNanos/1e6,nativeNanos/1e6);
                }finally{
                    saved.forEach((pos,snapshot)->game.maps().restoreSnapshot(level,pos,snapshot));
                    var fresh=new ArrayList<Entity>();for(var entity:level.getAllEntities())if(!before.contains(entity.getUUID())&&!(entity instanceof net.minecraft.server.level.ServerPlayer))fresh.add(entity);fresh.forEach(Entity::discard);
                }
            }
        }catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
        SkyWars2.LOGGER.info("[check] PASS nuclear-terrain-batch (exact rotated negative-coordinate12/13 source masks; same native strict states/BE/TNT semantics; unchanged4096 bound; section-scaled lookup counts, observed A/B CPU)");
    }
}
