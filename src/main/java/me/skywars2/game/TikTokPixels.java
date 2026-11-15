package me.skywars2.game;

import java.util.AbstractList;
import java.util.List;
import java.util.RandomAccess;
import net.minecraft.world.item.component.CustomModelData;

/** Three native map-colour indices per RGB tint; one immutable bounded array per displayed frame. */
final class TikTokPixels {
    static final int GROUPS_PER_ROW=(TikTokMedia.WIDTH+2)/3, TINTS=GROUPS_PER_ROW*TikTokMedia.HEIGHT;
    private TikTokPixels(){}
    static CustomModelData encode(byte[] pixels){
        if(pixels.length!=TikTokMedia.PIXELS)throw new IllegalArgumentException("frame_size");
        int[] values=new int[TINTS];
        for(int y=0;y<TikTokMedia.HEIGHT;y++)for(int group=0;group<GROUPS_PER_ROW;group++){
            int x=group*3,offset=y*TikTokMedia.WIDTH;
            int r=Byte.toUnsignedInt(pixels[offset+x]);
            int g=Byte.toUnsignedInt(pixels[offset+Math.min(TikTokMedia.WIDTH-1,x+1)]);
            int b=Byte.toUnsignedInt(pixels[offset+Math.min(TikTokMedia.WIDTH-1,x+2)]);
            if(r>=248||g>=248||b>=248)throw new IllegalArgumentException("frame_palette");
            values[y*GROUPS_PER_ROW+group]=(r<<16)|(g<<8)|b;
        }
        return new CustomModelData(List.of(),List.of(),List.of(),new Colors(values));
    }
    private static final class Colors extends AbstractList<Integer> implements RandomAccess {
        private final int[] data;
        Colors(int[] data){this.data=data;}
        @Override public Integer get(int index){return data[index];}
        @Override public int size(){return data.length;}
    }
}
