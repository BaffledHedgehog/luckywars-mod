package me.skywars2.game;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/** Finite resident-column search using vanilla spreadplayers' ground/two-air/ceiling rules. */
final class LbcSurfaceTeleport {
    private LbcSurfaceTeleport(){}
    static Vec3 destination(ServerLevel level,Vec3 center,int radius,int ceiling,RandomSource random){
        int minX=Mth.floor(center.x-radius),minZ=Mth.floor(center.z-radius);
        int width=(int)Math.ceil(center.x+radius)-minX,depth=(int)Math.ceil(center.z+radius)-minZ;
        int count=width*depth,index=random.nextInt(count),step;
        do{step=random.nextInt(count-1)+1;}while(gcd(step,count)!=1);
        var pos=new BlockPos.MutableBlockPos();
        for(int visited=0;visited<count;visited++,index=(index+step)%count){
            int x=minX+index%width,z=minZ+index/width;
            pos.set(x,0,z);if(!level.hasChunkAt(pos))continue;
            int surface=level.getHeight(Heightmap.Types.WORLD_SURFACE,x,z);
            if(surface<=level.getMinY())continue;
            int y=LbcPortalDrops.spawnY(level,pos,Math.min(ceiling,surface+1));
            pos.set(x,y-1,z);var state=level.getBlockState(pos);
            if(y-1>=ceiling||state.liquid()||!state.is(BlockTags.ENTITIES_CAN_TELEPORT_TO))continue;
            return new Vec3(x+.5,y,z+.5);
        }
        return null;
    }
    private static int gcd(int a,int b){while(b!=0){int next=a%b;a=b;b=next;}return a;}
}
