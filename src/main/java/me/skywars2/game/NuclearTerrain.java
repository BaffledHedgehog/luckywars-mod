package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** One bounded, section-aware terrain operation per nuclear payload, shared by all producers. */
public final class NuclearTerrain {
    private NuclearTerrain() {}
    /** Implemented by the existing native-explosion mixin; never invokes discovery, damage or packets. */
    public interface BlockEffects { void skywars2$applyNuclearBlocks(java.util.List<BlockPos> positions); }
    private static final ThreadLocal<Entity> DAMAGE_WAVE=new ThreadLocal<>();
    static final int CELLS_PER_STEP=4096;
    public static void damageWave(Entity emitter,Runnable nativeExplosion){
        var previous=DAMAGE_WAVE.get();DAMAGE_WAVE.set(emitter);
        try{nativeExplosion.run();}finally{if(previous==null)DAMAGE_WAVE.remove();else DAMAGE_WAVE.set(previous);}
    }
    /** Exact emitter identity keeps nested ordinary chain reactions on their own native terrain path. */
    public static boolean damageOnly(net.minecraft.world.level.ServerExplosion blast){
        var expected=DAMAGE_WAVE.get();return expected!=null&&(expected==blast.getDirectSourceEntity()
                ||blast.getDirectSourceEntity()==null&&expected==blast.getDamageSource().getDirectEntity());
    }
    static Shape point(Vec3 at){return new Shape(at,84,65);}
    static Shape wand(Vec3 at){return new Shape(at,at.add(0,2.5,0),40,45);}
    /** Source point cube-ring outer reach60+24; wand32-offset native envelope about40x45. */
    static final class Shape {
        final Vec3 origin,center;final double horizontal,vertical;final long seed;
        final int lowX,highX,lowY,highY,lowZ,highZ;
        int chunkX,chunkZ,sectionY,cell,visited;boolean done;
        net.minecraft.world.level.ServerExplosion nativeApplication;
        Shape(Vec3 center,double horizontal,double vertical){
            this(center,center,horizontal,vertical);
        }
        private Shape(Vec3 origin,Vec3 center,double horizontal,double vertical){
            this.origin=origin;this.center=center;this.horizontal=horizontal;this.vertical=vertical;seed=BlockPos.containing(center).asLong();
            lowX=(int)Math.floor(center.x-horizontal*1.12);highX=(int)Math.ceil(center.x+horizontal*1.12);
            lowY=(int)Math.floor(center.y-vertical*1.12);highY=(int)Math.ceil(center.y+vertical*1.12);
            lowZ=(int)Math.floor(center.z-horizontal*1.12);highZ=(int)Math.ceil(center.z+horizontal*1.12);
            chunkX=lowX>>4;chunkZ=lowZ>>4;sectionY=lowY>>4;
        }
        Shape copy(){var next=new Shape(origin,center,horizontal,vertical);next.chunkX=chunkX;next.chunkZ=chunkZ;
            next.sectionY=sectionY;next.cell=cell;next.visited=visited;next.done=done;return next;}
        /** No tickets, no forced chunks, no block snapshots/NBT, one read/write per admitted cell. */
        boolean step(GameController game,ServerLevel level,LbcIndirectKills.Owner cause){
            return step(game,level,cause,null,Runnable::run);
        }
        /** The restored job lazily creates one new application object from rebound sources. */
        boolean step(GameController game,ServerLevel level,LbcIndirectKills.Owner cause,
                java.util.function.Supplier<net.minecraft.world.level.ServerExplosion> factory,
                java.util.function.Consumer<Runnable> sourceContext){
            var coverage=LbcExplosionSuppression.coverage(game,level);
            if(coverage.contains(origin)){done=true;return true;}
            if(factory!=null&&nativeApplication==null)nativeApplication=factory.get();
            if(nativeApplication!=null&&nativeApplication.getBlockInteraction()==net.minecraft.world.level.Explosion.BlockInteraction.KEEP){done=true;return true;}
            boolean[] complete={done};
            sourceContext.accept(()->game.items().indirectKills().withSupportCandidates(level,new net.minecraft.world.phys.AABB(lowX,lowY,lowZ,highX+1,highY+1,highZ+1),
                    ()->game.items().indirectKills().trapChanges(level,cause,()->{
                        java.util.List<BlockPos> admitted=nativeApplication==null?null:new java.util.ArrayList<>();
                        complete[0]=stepBlocks(level,admitted,coverage);
                        if(admitted!=null&&!admitted.isEmpty()){
                            // Native application can trigger callbacks. Re-snapshot current TECH fields at its actual boundary.
                            var application=LbcExplosionSuppression.coverage(game,level);
                            if(application.contains(origin)){done=true;complete[0]=true;}
                            else{admitted.removeIf(application::blocks);if(!admitted.isEmpty())((BlockEffects)nativeApplication).skywars2$applyNuclearBlocks(admitted);}
                        }
                    })));
            return complete[0];
        }
        private boolean stepBlocks(ServerLevel level,java.util.List<BlockPos> admitted,LbcExplosionSuppression.Coverage coverage){
            if(done)return true;
            var probe=new BlockPos.MutableBlockPos();int budget=CELLS_PER_STEP;visited=0;
            while(budget>0&&!done){
                var chunk=level.getChunkSource().getChunkNow(chunkX,chunkZ);
                int y=sectionY<<4;
                if(chunk==null||y+15<level.getMinY()||y>level.getMaxY()) {advanceSection();continue;}
                var section=chunk.getSection(level.getSectionIndex(Math.max(y,level.getMinY())));
                if(section.hasOnlyAir()){advanceSection();continue;}
                while(cell<4096&&budget-->0){
                    int x=(chunkX<<4)+(cell&15),by=y+((cell>>8)&15),z=(chunkZ<<4)+((cell>>4)&15);cell++;visited++;
                    if(x<lowX||x>highX||by<lowY||by>highY||z<lowZ||z>highZ||by<level.getMinY()||by>level.getMaxY())continue;
                    var state=section.getBlockState(x&15,by&15,z&15);if(state.isAir())continue;
                    double dx=(x+.5-center.x)/horizontal,dy=(by+.5-center.y)/vertical,dz=(z+.5-center.z)/horizontal;
                    double radius=Math.sqrt(dx*dx+dy*dy+dz*dz);
                    if(!contains(seed,dx,dy,dz,radius))continue;
                    probe.set(x,by,z);if(coverage.blocks(probe)||state.getDestroySpeed(level,probe)<0)continue;
                    float resistance=Math.max(state.getBlock().getExplosionResistance(),state.getFluidState().getExplosionResistance());
                    if(unit(seed^probe.asLong())>=breakChance(resistance,radius))continue;
                    if(admitted==null)LbcDropBlocks.setBlock(level,probe,Blocks.AIR.defaultBlockState(),true);
                    else admitted.add(probe.immutable());
                }
                if(cell>=4096)advanceSection();
            }
            return done;
        }
        private void advanceSection(){cell=0;if(++sectionY>highY>>4){sectionY=lowY>>4;
            if(++chunkZ>highZ>>4){chunkZ=lowZ>>4;if(++chunkX>highX>>4)done=true;}}}
    }
    /** Dense core, coherent directional spikes at the edge; no random holes from sparse rays. */
    static boolean contains(long seed,double dx,double dy,double dz,double radius){
        if(radius<=.78)return true;if(radius>1.12)return false;
        int x=(int)Math.floor(dx/radius*13),y=(int)Math.floor(dy/radius*13),z=(int)Math.floor(dz/radius*13);
        return radius<=.88+.24*unit(seed^BlockPos.asLong(x,y,z));
    }
    static double breakChance(float resistance,double radius){return 1/(1+Math.max(0,resistance)/(96*Math.max(.2,1-radius)));}
    static double unit(long value){
        value=(value^(value>>>30))*0xbf58476d1ce4e5b9L;value=(value^(value>>>27))*0x94d049bb133111ebL;
        return ((value^(value>>>31))>>>11)*0x1.0p-53;
    }
}
