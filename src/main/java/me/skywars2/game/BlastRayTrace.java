package me.skywars2.game;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Shared vanilla ray arithmetic for live native explosions and detached countdown preparation. */
final class BlastRayTrace {
    private BlastRayTrace(){}
    interface Cells {
        boolean inside(BlockPos pos);
        BlockState state(BlockPos pos);
        float loss(BlockPos pos,BlockState state);
        boolean destroy(BlockPos pos,BlockState state,float power);
    }
    private static final double[] FIXED=rays(false),NATIVE=rays(true);
    private static final class Cell {
        final BlockPos pos;final BlockState state;final float loss;boolean added;
        Cell(BlockPos pos,BlockState state,float loss){this.pos=pos;this.state=state;this.loss=loss;}
    }
    static int rayCount(){return NATIVE.length/3;}
    private static double[] rays(boolean nativePrecision){
        double[] steps=new double[(16*16*16-14*14*14)*3];int next=0;
        for(int x=0;x<16;x++)for(int y=0;y<16;y++)for(int z=0;z<16;z++){
            if(x!=0&&x!=15&&y!=0&&y!=15&&z!=0&&z!=15)continue;
            double dx=x/15.0*2-1,dy=y/15.0*2-1,dz=z/15.0*2-1;
            if(nativePrecision){dx=x/15.0F*2.0F-1.0F;dy=y/15.0F*2.0F-1.0F;dz=z/15.0F*2.0F-1.0F;}
            double length=Math.sqrt(dx*dx+dy*dy+dz*dz);
            if(nativePrecision){steps[next++]=(dx/length)*.3F;steps[next++]=(dy/length)*.3F;steps[next++]=(dz/length)*.3F;}
            else{double scale=.3/length;steps[next++]=dx*scale;steps[next++]=dy*scale;steps[next++]=dz*scale;}
        }
        return steps;
    }
    static List<BlockPos> trace(Vec3 origin,float radius,RandomSource random,boolean nativePrecision,Cells view){
        var rays=nativePrecision?NATIVE:FIXED;
        var result=new it.unimi.dsi.fastutil.objects.ObjectArrayList<BlockPos>();
        var cells=new it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<Cell>();
        long[] recentKeys=new long[512];java.util.Arrays.fill(recentKeys,Long.MIN_VALUE);Cell[] recentCells=new Cell[512];
        var probe=new BlockPos.MutableBlockPos();
        for(int ray=0;ray<rays.length;ray+=3){
            double x=origin.x,y=origin.y,z=origin.z,dx=rays[ray],dy=rays[ray+1],dz=rays[ray+2];
            float power=radius*(nativePrecision?.7F+random.nextFloat()*.6F:1F),loss=0;
            long previous=Long.MIN_VALUE;Cell cell=null;
            while(power>0){
                int bx=net.minecraft.util.Mth.floor(x),by=net.minecraft.util.Mth.floor(y),bz=net.minecraft.util.Mth.floor(z);long key=BlockPos.asLong(bx,by,bz);
                if(key!=previous){
                    probe.set(bx,by,bz);if(!view.inside(probe))break;
                    int slot=(bx&7)|((by&7)<<3)|((bz&7)<<6);
                    if(recentKeys[slot]==key){cell=recentCells[slot];loss=cell==null?0:cell.loss;}
                    else{
                        var state=view.state(probe);
                        if(state.isAir()&&state.getFluidState().isEmpty()){cell=null;loss=0;}
                        else{
                            cell=cells.get(key);
                            if(cell==null){var pos=probe.immutable();cell=new Cell(pos,state,view.loss(pos,state));cells.put(key,cell);}
                            loss=cell.loss;
                        }
                        recentKeys[slot]=key;recentCells[slot]=cell;
                    }
                    previous=key;
                }
                power-=loss;
                if(power>0&&cell!=null&&!cell.added&&!cell.state.isAir()&&view.destroy(cell.pos,cell.state,power)){cell.added=true;result.add(cell.pos);}
                power-=.22500001F;x+=dx;y+=dy;z+=dz;
            }
        }
        return result;
    }
}
