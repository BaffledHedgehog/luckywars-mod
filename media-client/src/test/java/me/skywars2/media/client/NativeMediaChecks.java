package me.skywars2.media.client;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;

/** Exercises the exact bundled STB/NativeImage decoder, color order and native-memory cleanup. */
public final class NativeMediaChecks {
    public static void main(String[] args)throws Exception{
        var source=new BufferedImage(720,1280,BufferedImage.TYPE_INT_RGB);
        for(int y=0;y<1280;y++)for(int x=0;x<720;x++)source.setRGB(x,y,x<240?0xff2222:x<480?0x22ff22:0x2222ff);
        var out=new ByteArrayOutputStream();ImageIO.write(source,"jpg",out);byte[] jpeg=out.toByteArray();
        for(int i=0;i<16;i++){
            var image=JpegDecoder.decode(jpeg,720,1280);
            try{
                if(image.getWidth()!=720||image.getHeight()!=1280)throw new AssertionError("dimensions");
                for(int x:new int[]{120,360,600}){
                    int pixel=image.getPixel(x,640),r=pixel>>>16&255,g=pixel>>>8&255,b=pixel&255;
                    if(pixel>>>24!=255||(x==120?r<245||g>45||b>45:x==360?g<245||r>45||b>45:b<245||r>45||g>45))throw new AssertionError("opaque bright RGB channel order");
                }
            }finally{image.close();if(!image.isClosed())throw new AssertionError("native free");}
        }
        boolean rejected=false;try{JpegDecoder.decode(jpeg,1,1);}catch(java.io.IOException expected){rejected=true;}
        if(!rejected)throw new AssertionError("dimension guard");
        System.out.println("PASS native JPEG720x1280: STB -> NativeImage,16decode/free cycles,opaque brightRGB,dimension guard");
    }
}
