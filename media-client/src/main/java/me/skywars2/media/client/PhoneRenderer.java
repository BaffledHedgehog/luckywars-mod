package me.skywars2.media.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.joml.*;

/** One fullbright quad; the composite's original model still provides the phone and hand transform. */
public final class PhoneRenderer implements SpecialModelRenderer<Identifier> {
    // FaceBakery uses model coordinates /16; both composite layers share the same hand transform.
    private static final float X0=.15f/16,X1=13.85f/16;
    private static final float Y1=29.5f/16,Y0=Y1-13.7f*1280/720/16,Z=2.2f/16;
    @Override public Identifier extractArgument(ItemStack stack){return MediaClient.texture();}
    @Override public void getExtents(Consumer<Vector3fc> output){
        output.accept(new Vector3f(0,-1f/16,2.1f/16));output.accept(new Vector3f(14f/16,29.55f/16,2.1f/16));
        output.accept(new Vector3f(X0,Y0,Z));output.accept(new Vector3f(X1,Y1,Z));
    }
    @Override public void submit(Identifier texture,PoseStack pose,SubmitNodeCollector collector,int light,int overlay,boolean foil,int outline){
        MediaClient.present();texture=MediaClient.texture();
        if(texture==null)return;
        collector.submitCustomGeometry(pose,RenderTypes.text(texture),(p,b)->{
            // Keep the original status bar/time/battery above29.55 visible.
            quad(p,b,0,-1f/16,14f/16,29.55f/16,2.1f/16,0xff000000);
            quad(p,b,X0,Y0,X1,Y1,Z,-1);
        });
    }
    private static void quad(PoseStack.Pose p,com.mojang.blaze3d.vertex.VertexConsumer b,float x0,float y0,float x1,float y1,float z,int color){
        b.addVertex(p.pose(),x0,y1,z).setColor(color).setUv(0,0).setLight(15728880);
        b.addVertex(p.pose(),x0,y0,z).setColor(color).setUv(0,1).setLight(15728880);
        b.addVertex(p.pose(),x1,y0,z).setColor(color).setUv(1,1).setLight(15728880);
        b.addVertex(p.pose(),x1,y1,z).setColor(color).setUv(1,0).setLight(15728880);
    }
    public record Unbaked() implements SpecialModelRenderer.Unbaked<Identifier> {
        public static final MapCodec<Unbaked> CODEC=MapCodec.unit(new Unbaked());
        @Override public MapCodec<Unbaked> type(){return CODEC;}
        @Override public PhoneRenderer bake(SpecialModelRenderer.BakingContext context){return new PhoneRenderer();}
    }
}
