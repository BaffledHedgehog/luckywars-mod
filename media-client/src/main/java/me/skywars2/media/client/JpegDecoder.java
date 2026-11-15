package me.skywars2.media.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import me.skywars2.media.MediaPackets;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.*;

/** NativeImage.read is PNG-only in 26.3; decode bounded JPEG explicitly with the bundled STB. */
public final class JpegDecoder {
    private JpegDecoder(){}
    public static NativeImage decode(byte[] jpeg,int width,int height)throws IOException{
        if(!MediaPackets.jpegMatches(jpeg,width,height))throw new IOException("media_jpeg_bounds");
        var encoded=MemoryUtil.memAlloc(jpeg.length);
        try(var stack=MemoryStack.stackPush()){
            encoded.put(jpeg).flip();var w=stack.mallocInt(1);var h=stack.mallocInt(1);var channels=stack.mallocInt(1);
            var rgba=STBImage.stbi_load_from_memory(encoded,w,h,channels,4);
            if(rgba==null)throw new IOException("media_jpeg_invalid");
            try{
                if(w.get(0)!=width||h.get(0)!=height)throw new IOException("media_jpeg_dimensions");
                var image=new NativeImage(width,height,false);
                try{MemoryUtil.memCopy(MemoryUtil.memAddress(rgba),image.getPointer(),(long)width*height*4);return image;}
                catch(Throwable failure){image.close();throw failure;}
            }finally{STBImage.stbi_image_free(rgba);}
        }finally{MemoryUtil.memFree(encoded);}
    }
}
