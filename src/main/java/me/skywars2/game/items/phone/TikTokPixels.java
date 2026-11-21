package me.skywars2.game.items.phone;

import java.util.AbstractList;
import java.util.List;
import java.util.RandomAccess;
import net.minecraft.world.item.component.CustomModelData;

/** Three native map-colour indices per RGB tint; one immutable bounded array per displayed frame. */
final class TikTokPixels {
    static final int GROUPS_PER_ROW=(TikTokMedia.WIDTH+2)/3, TINTS=GROUPS_PER_ROW*TikTokMedia.HEIGHT;
    static final int MAX_STEP=16;
    private TikTokPixels(){}
    static CustomModelData encode(byte[] pixels){
        return encode(pixels,1);
    }
    static CustomModelData encode(byte[] pixels,int step){
        if(pixels.length!=TikTokMedia.PIXELS)throw new IllegalArgumentException("frame_size");
        if(step<1||step>MAX_STEP||Integer.bitCount(step)!=1)throw new IllegalArgumentException("pixel_step");
        int[] values=new int[TINTS];
        for(int row=0;row<TikTokMedia.HEIGHT;row+=step){
            int sourceRow=Math.min(row+step/2,TikTokMedia.HEIGHT-1)*TikTokMedia.WIDTH;
            int encodedRow=row*GROUPS_PER_ROW;
            for(int group=0;group<GROUPS_PER_ROW;group++){
                int x=group*3;
                int r=sample(pixels,sourceRow,x,step);
                int g=sample(pixels,sourceRow,x+1,step);
                int b=sample(pixels,sourceRow,x+2,step);
                if(r>=248||g>=248||b>=248)throw new IllegalArgumentException("frame_palette");
                values[encodedRow+group]=(r<<16)|(g<<8)|b;
            }
            for(int copy=1;copy<step&&row+copy<TikTokMedia.HEIGHT;copy++)
                System.arraycopy(values,encodedRow,values,encodedRow+copy*GROUPS_PER_ROW,GROUPS_PER_ROW);
        }
        return new CustomModelData(List.of(),List.of(),List.of(),new Colors(values));
    }
    private static int sample(byte[] pixels,int row,int x,int step){
        int center=(x & -step)+step/2;
        return Byte.toUnsignedInt(pixels[row+Math.min(center,TikTokMedia.WIDTH-1)]);
    }
    private static final class Colors extends AbstractList<Integer> implements RandomAccess {
        private final int[] data;
        Colors(int[] data){this.data=data;}
        @Override public Integer get(int index){return data[index];}
        @Override public int size(){return data.length;}
    }
}
