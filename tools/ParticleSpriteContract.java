import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.JsonOps;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

/** Runs the real image decoder and SpriteContents mip0 preparation, without a window. */
public final class ParticleSpriteContract {
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private static int markers(NativeImage image){
        int[][] corners={{0,0},{image.getWidth()-1,0},{image.getWidth()-1,image.getHeight()-1},{0,image.getHeight()-1}};
        int count=0;
        for(int i=0;i<4;i++){
            int pixel=image.getPixel(corners[i][0],corners[i][1]);
            if(ARGB.red(pixel)==83&&ARGB.green(pixel)==77&&ARGB.blue(pixel)==241+i&&ARGB.alpha(pixel)==0)count++;
        }
        return count;
    }
    public static void main(String[] args)throws Exception{
        Path imagePath=Path.of(args[0]),metadataPath=Path.of(args[1]),result=Path.of(args[2]);
        var metadata=JsonParser.parseString(Files.readString(metadataPath)).getAsJsonObject().get("texture");
        var parsed=TextureMetadataSection.CODEC.parse(JsonOps.INSTANCE,metadata).getOrThrow();
        int positive,negative;
        try(var stream=Files.newInputStream(imagePath);var image=NativeImage.read(stream)){
            require(markers(image)==4,"Native PNG decoder erased coordinate bytes");
            try(var sprite=new SpriteContents(Identifier.withDefaultNamespace("flash"),
                    new FrameSize(image.getWidth(),image.getHeight()),image,Optional.empty(),List.of(),Optional.of(parsed))){
                sprite.increaseMipLevel(0);positive=markers(image);
                require(positive==4,"Actual native SpriteContents mip0 preparation erased markers");
            }
        }
        try(var stream=Files.newInputStream(imagePath);var image=NativeImage.read(stream)){
            try(var sprite=new SpriteContents(Identifier.withDefaultNamespace("flash"),
                    new FrameSize(image.getWidth(),image.getHeight()),image,Optional.empty(),List.of(),Optional.empty())){
                sprite.increaseMipLevel(0);negative=markers(image);
                require(negative==0,"AUTO negative control failed to reproduce native transparent-RGB solidification");
            }
        }
        String json="{\"passed\":true,\"nativePngDecode\":true,\"nativeSpriteContentsMip0\":true,\"retainedMarkers\":"+positive+
            ",\"autoNegativeControlRetainedMarkers\":"+negative+",\"windowOpened\":false,\"nativeCodeSource\":\""+
            SpriteContents.class.getProtectionDomain().getCodeSource().getLocation().toURI()+"\"}";
        Files.writeString(result,json+"\n");System.out.println(json);
    }
}
