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
    // Baked item cuboids are centered at (8,8,8); special geometry must use the same origin.
    private static final float X0=(.15f-8)/16,X1=(13.85f-8)/16;
    private static final float Y0=(15-13.7f*1280/720/2-8)/16,Y1=(15+13.7f*1280/720/2-8)/16,Z=(2.015f-8)/16;
    @Override public Identifier extractArgument(ItemStack stack){return MediaClient.texture();}
    @Override public void getExtents(Consumer<Vector3fc> output){output.accept(new Vector3f(X0,Y0,Z));output.accept(new Vector3f(X1,Y1,Z));}
    @Override public void submit(Identifier texture,PoseStack pose,SubmitNodeCollector collector,int light,int overlay,boolean foil,int outline){
        if(texture==null)return;
        collector.submitCustomGeometry(pose,RenderTypes.text(texture),(p,b)->{
            b.addVertex(p.pose(),X0,Y1,Z).setColor(-1).setUv(0,0).setLight(15728880);
            b.addVertex(p.pose(),X0,Y0,Z).setColor(-1).setUv(0,1).setLight(15728880);
            b.addVertex(p.pose(),X1,Y0,Z).setColor(-1).setUv(1,1).setLight(15728880);
            b.addVertex(p.pose(),X1,Y1,Z).setColor(-1).setUv(1,0).setLight(15728880);
        });
    }
    public record Unbaked() implements SpecialModelRenderer.Unbaked<Identifier> {
        public static final MapCodec<Unbaked> CODEC=MapCodec.unit(new Unbaked());
        @Override public MapCodec<Unbaked> type(){return CODEC;}
        @Override public PhoneRenderer bake(SpecialModelRenderer.BakingContext context){return new PhoneRenderer();}
    }
}
