package me.skywars2.game;

import java.util.*;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Independent vanilla method path and RNG oracle for ordinary native TNT's optimized collector. */
public final class NativeExplosionCacheChecks {
    private NativeExplosionCacheChecks() { }
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Native explosion cache: "+why);}
    /** Observe real ray crossings of an independent Euclidean shell, rather than testing the ray table itself. */
    private static void sphericalContract(){
        int[] bins=new int[8*16];int[] crossings={0};var origin=new Vec3(.5,.5,.5);
        var random=net.minecraft.util.RandomSource.create(73);var oracle=net.minecraft.util.RandomSource.create(73);
        var blocks=BlastRayTrace.traceSpherical(origin,36,random,new BlastRayTrace.Cells(){
            public boolean inside(BlockPos pos){
                var relative=Vec3.atCenterOf(pos).subtract(origin);if(relative.lengthSqr()<12*12)return true;
                var direction=relative.normalize();int band=Math.min(7,(int)((direction.y+1)*4));
                int sector=Math.min(15,(int)((Math.atan2(direction.z,direction.x)+Math.PI)/(Math.PI*2)*16));
                bins[band*16+sector]++;crossings[0]++;return false;
            }
            public net.minecraft.world.level.block.state.BlockState state(BlockPos pos){return Blocks.AIR.defaultBlockState();}
            public float loss(BlockPos pos,net.minecraft.world.level.block.state.BlockState state){return 0;}
            public boolean destroy(BlockPos pos,net.minecraft.world.level.block.state.BlockState state,float power){throw new AssertionError("Air must never enter native block callbacks");}
        });
        require(blocks.isEmpty()&&crossings[0]==BlastRayTrace.rayCount(),"every spherical ray reaches the Euclidean shell; air emits no block callbacks");
        int min=Arrays.stream(bins).min().orElseThrow(),max=Arrays.stream(bins).max().orElseThrow();
        require(min>=3&&max<=20,"equal-solid-angle shell coverage avoids cube-corner concentration: min="+min+" max="+max);
        for(int i=0;i<BlastRayTrace.rayCount();i++)oracle.nextFloat();
        require(random.nextLong()==oracle.nextLong(),"spherical collection preserves the native one-energy-draw-per-ray RNG budget");
        SkyWars2.LOGGER.info("[check] spherical-blast geometry: {} shell crossings,128 equal-solid-angle bins,min={},max={}",crossings[0],min,max);
    }
    public static void run(GameController game){
        sphericalContract();
        String previous=System.getProperty("skywars2.disableExplosionCache");
        try(var f=new WoolBucketChecks.Fixture(game,false)){
            f.prepareParticipants();var center=BlockPos.containing(f.home.add(0,55,0));var at=Vec3.atCenterOf(center);
            var method=ServerExplosion.class.getDeclaredMethod("calculateExplodedPositions");method.setAccessible(true);
            var tnt=new PrimedTnt(EntityTypes.TNT,f.level);tnt.setPos(at);
            long begin=LbcExasperato.optimizedBlockSearches(),slowNanos=0,fastNanos=0;int cases=0;
            for(int scene=0;scene<3;scene++){
                for(var pos:BlockPos.betweenClosed(center.offset(-6,-6,-6),center.offset(6,6,6))) {
                    int pick=Math.floorMod(pos.getX()*17+pos.getY()*31+pos.getZ()*7,7);
                    var block=scene==0?Blocks.AIR:scene==1?Blocks.STONE:pick==0?Blocks.OBSIDIAN:pick<3?Blocks.WATER:pick==3?Blocks.OAK_PLANKS:Blocks.STONE;
                    f.set(pos,block.defaultBlockState());
                }
                for(int seed=0;seed<5;seed++){
                    System.setProperty("skywars2.disableExplosionCache","true");f.level.getRandom().setSeed(seed);
                    long start=System.nanoTime();
                    var original=(List<BlockPos>)method.invoke(new ServerExplosion(f.level,tnt,null,null,at,4,false,Explosion.BlockInteraction.DESTROY));
                    slowNanos+=System.nanoTime()-start;long expectedRandom=f.level.getRandom().nextLong();
                    System.setProperty("skywars2.disableExplosionCache","false");f.level.getRandom().setSeed(seed);start=System.nanoTime();
                    var actual=(List<BlockPos>)method.invoke(new ServerExplosion(f.level,tnt,null,null,at,4,false,Explosion.BlockInteraction.DESTROY));
                    fastNanos+=System.nanoTime()-start;
                    require(new HashSet<>(actual).equals(new HashSet<>(original)),"exact native selected blocks in air/stone/fluids/high-resistance scene="+scene+" seed="+seed);
                    require(f.level.getRandom().nextLong()==expectedRandom,"native random stream consumption preserved");cases++;
                }
            }
            require(LbcExasperato.optimizedBlockSearches()-begin==cases,"ordinary TNT really enters optimized collection, including lightshoot column TNT");
            SkyWars2.LOGGER.info("[check] PASS native-explosion-cache ({} seeded native/original block sets+RNG identical; block-search benchmark native={}ms optimized={}ms; damage/drop pipeline unchanged)",cases,slowNanos/1e6,fastNanos/1e6);
        }catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
        finally{if(previous==null)System.clearProperty("skywars2.disableExplosionCache");else System.setProperty("skywars2.disableExplosionCache",previous);}
    }
}
