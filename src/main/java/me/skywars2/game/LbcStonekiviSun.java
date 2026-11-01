package me.skywars2.game;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Restored retired Stonekivi SUN, an independent mode beside Crystallization. */
public final class LbcStonekiviSun {
    // Retired loop_sun_end: layers at impact+48..248, hence center+148 and radius100.
    public static final int RADIUS=100, CENTER_HEIGHT=148;
    static final int EXTENT=RADIUS+20;
    static final int FLAGS=Block.UPDATE_CLIENTS|Block.UPDATE_SKIP_ALL_SIDEEFFECTS;
    private static final BlockState MAGMA=Blocks.MAGMA_BLOCK.defaultBlockState(),
            GLOWSTONE=Blocks.GLOWSTONE.defaultBlockState(),SHROOMLIGHT=Blocks.SHROOMLIGHT.defaultBlockState(),
            ORANGE=Blocks.STAINED_GLASS.pick(net.minecraft.world.item.DyeColor.ORANGE).defaultBlockState(),YELLOW=Blocks.STAINED_GLASS.pick(net.minecraft.world.item.DyeColor.YELLOW).defaultBlockState();
    private static final MapManager.BlockSnapshot MAGMA_CELL=new MapManager.BlockSnapshot(MAGMA,null),GLOWSTONE_CELL=new MapManager.BlockSnapshot(GLOWSTONE,null),
            SHROOMLIGHT_CELL=new MapManager.BlockSnapshot(SHROOMLIGHT,null),ORANGE_CELL=new MapManager.BlockSnapshot(ORANGE,null),
            YELLOW_CELL=new MapManager.BlockSnapshot(YELLOW,null);
    private LbcStonekiviSun() { }

    /** The existing Stonekivi ray owns targeting, suppression, sound and mana. */
    public static LbcStonekivi.Work create(GameController game,ServerPlayer owner,BlockPos impact) {
        game.items().stonekivi().beginNativeWork();
        try { return new SunWork(game,owner,impact.above(CENTER_HEIGHT),RADIUS,owner.getRandom().nextLong()); }
        finally { game.items().stonekivi().endNativeWork(); }
    }

    /** Pure coordinate sampler: no templates, world access, RNG draws or retained per-cell planner. */
    static BlockState material(int x,int y,int z,int radius,long seed) {
        double square=(double)x*x+(double)y*y+(double)z*z;
        double reach=radius*1.15+4.5;
        if(square>reach*reach)return null;
        double inner=radius*2.0/3.0;
        if(square<=inner*inner)return MAGMA;
        if(square<radius*radius*.78)return MAGMA;
        double distance=Math.sqrt(square);
        double nx=(double)x/radius,ny=(double)y/radius,nz=(double)z/radius;
        double rough=noise(nx*8,ny*8,nz*8,seed);
        double surface=radius+rough*2.5;
        long hash=hash(x,y,z,seed);
        int pick=(int)Long.remainderUnsigned(hash,100);
        if(distance<=surface) {
            if(pick<30)return MAGMA;
            if(pick<52)return GLOWSTONE;
            if(pick<64)return SHROOMLIGHT;
            if(pick<81)return ORANGE;
            if(pick<98)return YELLOW;
            return MAGMA;
        }
        // Narrow continuous angular ribbons curl radially away from the photosphere.
        // They are a finite part of the initial fill, never ticked emitters or flowing fluids (author: magma replaces all lava/fire).
        double excess=distance-surface;
        if(excess>radius*.15+2)return null;
        double longitude=Math.atan2(nz,nx),latitude=Math.atan2(ny,Math.sqrt(nx*nx+nz*nz));
        double bend=longitude*9+latitude*6+excess*.23+(seed&255)*.017;
        double ribbon=Math.abs(Math.sin(bend));
        double lobe=.5+.5*Math.sin(latitude*5-longitude*3+(seed&63));
        if(ribbon>.11||excess>3+radius*.13*lobe)return null;
        if(pick<48)return MAGMA;
        if(pick<72)return ORANGE;
        if(pick<97)return YELLOW;
        return MAGMA;
    }
    private static long hash(int x,int y,int z,long seed) {
        long value=seed^((long)x*0x9e3779b97f4a7c15L)^((long)y*0xc2b2ae3d27d4eb4fL)^((long)z*0x165667b19e3779f9L);
        value=(value^(value>>>30))*0xbf58476d1ce4e5b9L;
        value=(value^(value>>>27))*0x94d049bb133111ebL;
        return value^(value>>>31);
    }
    private static double sample(int x,int y,int z,long seed) { return (hash(x,y,z,seed)>>>11)*0x1.0p-52-1; }
    private static double smooth(double value) { return value*value*(3-2*value); }
    private static double lerp(double a,double b,double t) { return a+(b-a)*t; }
    private static double noise(double x,double y,double z,long seed) {
        int ix=(int)Math.floor(x),iy=(int)Math.floor(y),iz=(int)Math.floor(z);
        double tx=smooth(x-ix),ty=smooth(y-iy),tz=smooth(z-iz);
        double low=lerp(lerp(sample(ix,iy,iz,seed),sample(ix+1,iy,iz,seed),tx),
                lerp(sample(ix,iy+1,iz,seed),sample(ix+1,iy+1,iz,seed),tx),ty);
        double high=lerp(lerp(sample(ix,iy,iz+1,seed),sample(ix+1,iy,iz+1,seed),tx),
                lerp(sample(ix,iy+1,iz+1,seed),sample(ix+1,iy+1,iz+1,seed),tx),ty);
        return lerp(low,high,tz);
    }
    private static MapManager.BlockSnapshot cell(BlockState state) {
        if(state==MAGMA)return MAGMA_CELL;if(state==GLOWSTONE)return GLOWSTONE_CELL;
        if(state==SHROOMLIGHT)return SHROOMLIGHT_CELL;if(state==ORANGE)return ORANGE_CELL;if(state==YELLOW)return YELLOW_CELL;
        return MAGMA_CELL;
    }

    static final class SunWork implements LbcStonekivi.Work {
        final GameController game;final ServerLevel level;final String epoch;final BlockPos center;
        final int radius,extent,minChunkX,minChunkZ,chunkWidth,chunkDepth,minY,maxY;
        final long seed,started=System.nanoTime();
        ServerPlayer owner;
        final LbcIndirectKills.Owner cause;
        MapLighting.Update lighting;
        LongOpenHashSet touched=new LongOpenHashSet();
        int chunkIndex,y,cellIndex;
        boolean sealed,closed;
        long visits,writes,maxStepNanos;
        SunWork(GameController game,ServerPlayer owner,BlockPos center,int radius,long seed) {
            this.game=game;this.owner=owner;level=owner.level();epoch=game.items().epoch();this.center=center.immutable();
            this.radius=radius;extent=radius+(int)Math.ceil(radius*.15)+5;this.seed=seed;
            minChunkX=(center.getX()-extent)>>4;minChunkZ=(center.getZ()-extent)>>4;
            chunkWidth=((center.getX()+extent)>>4)-minChunkX+1;chunkDepth=((center.getZ()+extent)>>4)-minChunkZ+1;
            minY=Math.max(level.getMinY(),center.getY()-extent);maxY=Math.min(level.getMaxY()-1,center.getY()+extent);y=minY;
            cause=game.items().indirectKills().wildCause(owner);lighting=new MapLighting.Update(level);
        }
        long writes(){return writes;}
        long visits(){return visits;}
        boolean finished(){return closed;}
        public boolean step() {
            if(closed)return true;
            if(!game.playing()||!game.items().currentEpoch(epoch)){cancel();return true;}
            if(game.items().timeStop().stopped(level))return false;
            long stepStart=System.nanoTime();
            long deadline=Math.max(EffectTerrain.deadlineNanos(),stepStart+500_000);
            game.items().stonekivi().beginNativeWork();
            try {
                if(!sealed)game.items().indirectKills().trapChanges(level,cause,()->fill(deadline));
                if(sealed&&lighting.tick(deadline)) {
                    closed=true;
                    SkyWars2.LOGGER.info("Stonekivi SUN complete radius={} visits={} writes={} elapsedMs={} maxStepMs={}",radius,visits,writes,
                            (System.nanoTime()-started)/1_000_000.0,Math.max(maxStepNanos,System.nanoTime()-stepStart)/1_000_000.0);
                    return true;
                }
                return false;
            } finally { maxStepNanos=Math.max(maxStepNanos,System.nanoTime()-stepStart);game.items().stonekivi().endNativeWork(); }
        }
        private void fill(long deadline) {
            int allowance=262144,changed=0;
            var pos=new BlockPos.MutableBlockPos();
            while(chunkIndex<chunkWidth*chunkDepth&&allowance>0&&changed<32768) {
                if((allowance&255)==0&&System.nanoTime()>=deadline)return;
                int cx=minChunkX+chunkIndex/chunkDepth,cz=minChunkZ+chunkIndex%chunkDepth;
                var chunk=level.getChunkSource().getChunkNow(cx,cz);
                if(chunk==null||y>maxY){chunkIndex++;y=minY;cellIndex=0;continue;}
                int x=(cx<<4)+(cellIndex&15),z=(cz<<4)+(cellIndex>>4);
                int dx=x-center.getX(),dy=y-center.getY(),dz=z-center.getZ();
                if(Math.abs(dx)<=extent&&Math.abs(dz)<=extent) {
                    allowance--;visits++;
                    BlockState state=material(dx,dy,dz,radius,seed);
                    if(state!=null) {
                        pos.set(x,y,z);
                        if(chunk.getBlockState(pos)!=state) {
                            lighting.changed(pos);touched.add(chunk.getPos().pack());
                            // The strict palette write skips onPlace, neighbor notifications and fluid scheduling.
                            // Native map journaling and indirect environmental attribution stay in their existing hooks.
                            game.maps().restoreSnapshot(level,pos,cell(state),FLAGS);
                            writes++;changed++;
                        }
                    }
                } else allowance--;
                if(++cellIndex==256){cellIndex=0;y++;}
            }
            if(chunkIndex>=chunkWidth*chunkDepth) { lighting.finishWrites();sealed=true; }
        }
        public void cancel() { if(closed)return;closed=true;lighting.cancel(); }
        @Override public MatchQueuedWorkCheckpoint.Image checkpoint(MatchQueuedWorkCheckpoint.Graph graph) {
            game.items().stonekivi().checkpointBoundary();
            var author=graph.links.key(owner);
            int savedChunk=chunkIndex,savedY=y,savedCell=cellIndex;
            boolean savedSealed=sealed,savedClosed=closed;
            long savedVisits=visits,savedWrites=writes,savedMax=maxStepNanos;
            var savedTouched=touched.clone();
            return links->{
                game.items().stonekivi().checkpointBoundary();MatchAuthorityCheckpoint.world(game,level);
                var rebound=(ServerPlayer)links.get(author);
                return ()->{
                    lighting.cancel();lighting=new MapLighting.Update(level);owner=rebound;
                    chunkIndex=savedChunk;y=savedY;cellIndex=savedCell;sealed=savedSealed;closed=savedClosed;
                    visits=savedVisits;writes=savedWrites;maxStepNanos=savedMax;touched=savedTouched.clone();
                    for(long key:touched){var chunk=net.minecraft.world.level.ChunkPos.unpack(key);lighting.changed(new BlockPos(chunk.getMinBlockX(),Math.clamp(center.getY(),level.getMinY(),level.getMaxY()-1),chunk.getMinBlockZ()));}
                    if(sealed)lighting.finishWrites();
                };
            };
        }
    }
}
