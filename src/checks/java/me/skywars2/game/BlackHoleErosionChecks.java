package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;

/** Explicit native cursor boundaries; no wall-clock throughput claim. Restored by the owning fixture. */
final class BlackHoleErosionChecks {
    private BlackHoleErosionChecks(){}
    private static void require(boolean value,String why){if(!value)throw new IllegalStateException("Black hole erosion: "+why);}
    private static Set<BlockPos> pass(BlackHoleErosion work,int allowance){
        var erased=new HashSet<BlockPos>();int calls=0;
        while(work.hasWork()){
            int spent=work.step(allowance,System.nanoTime()+100_000_000,(pos,state)->erased.add(pos));
            require(spent>0&&spent<=allowance,"bounded positive inspection allowance");
            require(++calls<4096,"finite pass");
        }
        return erased;
    }
    static void run(GameController game,ServerPlayer player,Vec3 at,BiConsumer<BlockPos,Block> place){
        var world=player.level();var center=BlockPos.containing(at);var cells=new ArrayList<BlockPos>();
        // Two disjoint live neighborhoods let movement expose any retained old-coordinate work.
        for(var pos:BlockPos.betweenClosed(center.offset(-7,-7,-7),center.offset(20,7,7))){
            require(world.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)!=null,"resident cursor fixture");
            cells.add(pos.immutable());place.accept(pos.immutable(),Blocks.DIRT);
        }
        try{
            var exact=new BlackHoleErosion(world,71,false);var fractional=at.add(.37,.21,-.33);double radius=5.5;
            exact.move(fractional,radius,10);int queued=exact.queuedBlockCount();
            require(exact.step(0,Long.MAX_VALUE,(pos,state)->{throw new IllegalStateException("zero allowance wrote");})==0&&exact.queuedBlockCount()==queued,"zero allowance leaves cursor unchanged");
            require(exact.step(64,System.nanoTime()-1,(pos,state)->{throw new IllegalStateException("expired deadline wrote");})==0&&exact.queuedBlockCount()==queued,"expired deadline leaves cursor unchanged");
            var first=new ArrayList<BlockPos>();exact.step(128,System.nanoTime()+100_000_000,(pos,state)->first.add(pos));
            require(!first.isEmpty()&&Vec3.atCenterOf(first.getFirst()).distanceToSqr(fractional)<9,"first budget reaches current center before far terrain");
            var erased=pass(exact,127);erased.addAll(first);var expected=new HashSet<BlockPos>();
            for(var pos:cells)if(Vec3.atCenterOf(pos).distanceToSqr(fractional)<=radius*radius)expected.add(pos);
            require(erased.equals(expected),"fractional-center exact sphere includes all interior and excludes every outside cell");
            exact.move(fractional,radius,10);require(!exact.hasWork(),"completed pass does not restart within the same clock");
            exact.move(fractional,radius,11);require(exact.hasWork(),"later active clock refreshes completed terrain");exact.clear();require(!exact.hasWork()&&exact.queuedBlockCount()==0,"clear releases cursor");

            var noisy=new BlackHoleErosion(world,991,true);noisy.move(at,6,20);var selected=pass(noisy,113);
            var repeat=new BlackHoleErosion(world,991,true);repeat.move(at,6,20);require(selected.equals(pass(repeat,8192)),"noise is stable across allowance partitions");
            var different=new BlackHoleErosion(world,992,true);different.move(at,6,20);require(!selected.equals(pass(different,113)),"distinct bodies dither different cells");
            long inner=0,innerErased=0,outer=0,outerErased=0;
            for(var pos:cells){double d=Vec3.atCenterOf(pos).distanceToSqr(at);if(d<=2.25)require(selected.contains(pos),"tiny center stays responsive");
                if(d>2.25&&d<9){inner++;if(selected.contains(pos))innerErased++;}
                if(d>=23.04&&d<=36){outer++;if(selected.contains(pos))outerErased++;}
                if(selected.contains(pos))require(d<=36,"noisy writes remain inside exact radius");
            }
            require(innerErased>0&&innerErased<inner&&outerErased>0&&outerErased<outer,"both annuli retain intermixed erosion and remains");
            require((double)innerErased/inner>(double)outerErased/outer,"erosion thins toward the radial boundary");
            noisy.move(at,6,21);require(!selected.equals(pass(noisy,113)),"later passes advance the erosion pattern");

            var moving=new BlackHoleErosion(world,3,false);moving.move(at,6,30);int maximum=moving.queuedBlockCount();moving.step(64,System.nanoTime()+100_000_000,(pos,state)->{});
            int calls=0;
            for(int i=0;i<80;i++){
                var current=at.add(12+(i&1),0,0);moving.move(current,6,31+i);
                require(moving.queuedBlockCount()<=maximum,"moving work never accumulates swept history");
                int spent=moving.step(128,System.nanoTime()+100_000_000,(pos,state)->require(Vec3.atCenterOf(pos).distanceToSqr(current)<=36,"moving writes use latest geometry, excluding old work"));
                require(spent>0&&spent<=128,"moving inspection budget");calls++;
                if(!moving.hasWork())break;
            }
            require(calls>0&&moving.queuedBlockCount()<=7*7*7,"continuous movement advances the outer pass instead of restarting it");
            var tail=pass(moving,128);var latest=at.add(12+((calls-1)&1),0,0);
            for(var pos:tail)require(Vec3.atCenterOf(pos).distanceToSqr(latest)<=36,"remaining core drains at latest center");moving.clear();

            // Probe an absent column using only getChunkNow, then ensure processing does not load it.
            int missingX=0,missingZ=0;boolean found=false;
            for(int distance=64;distance<=4096;distance*=2){int x=(center.getX()>>4)+distance,z=center.getZ()>>4;if(world.getChunkSource().getChunkNow(x,z)==null){missingX=x;missingZ=z;found=true;break;}}
            require(found,"absent-column fixture");var forced=new HashSet<>(world.getForceLoadedChunks());
            var absent=new BlackHoleErosion(world,4,false);absent.move(new Vec3(missingX*16+8.5,at.y,missingZ*16+8.5),2,100);
            require(pass(absent,128).isEmpty()&&world.getChunkSource().getChunkNow(missingX,missingZ)==null&&forced.equals(world.getForceLoadedChunks()),"unresident work neither writes nor loads or forces chunks");absent.clear();
        }finally{for(var pos:cells)place.accept(pos,Blocks.AIR);}
    }
}
