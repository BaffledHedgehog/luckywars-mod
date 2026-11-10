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
    public static void run(GameController game){
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
