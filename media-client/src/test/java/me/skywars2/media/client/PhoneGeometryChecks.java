package me.skywars2.media.client;

import java.util.ArrayList;
import net.minecraft.client.renderer.FaceInfo;
import net.minecraft.core.Direction;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/** Compare the submitted special geometry to native cuboid coordinates, before shared hand transforms. */
public final class PhoneGeometryChecks {
    public static void main(String[] args){
        // Actual original lbc:item/zaika body cuboid. FaceBakery uses select(from,to)/16 directly.
        var from=new Vector3f(0,-1,.5f);var to=new Vector3f(14,31,2);
        var min=new Vector3f(Float.POSITIVE_INFINITY);var max=new Vector3f(Float.NEGATIVE_INFINITY);
        var face=FaceInfo.fromFacing(Direction.SOUTH);
        for(int i=0;i<4;i++){var p=face.getVertexInfo(i).select(from,to).div(16);min.min(p);max.max(p);}
        var vertices=new ArrayList<Vector3fc>();new PhoneRenderer().getExtents(vertices::add);
        if(vertices.size()<2)throw new AssertionError("screen extents required");
        for(var p:vertices){
            if(p.x()<min.x-1e-6f||p.x()>max.x+1e-6f||p.y()<min.y-1e-6f||p.y()>max.y+1e-6f)
                throw new AssertionError("screen outside original native phone face: "+p+" versus "+min+".."+max);
            if(p.z()<=max.z||p.z()-max.z>.02f)throw new AssertionError("screen must lie in front of original face: "+p);
            if(p.y()>29.55f/16+1e-6f)throw new AssertionError("screen covers original status bar: "+p);
        }
        var videoMin=vertices.get(vertices.size()-2);var videoMax=vertices.getLast();
        if(Math.abs(videoMax.y()-29.5f/16)>1e-6f||Math.abs((videoMax.y()-videoMin.y())/(videoMax.x()-videoMin.x())-1280f/720)>1e-5f)
            throw new AssertionError("video top and uncropped aspect must fit below status bar");
        System.out.println("PASS PhoneGeometry: native SOUTH cuboid bounds, original status bar retained, video raised without aspect change, separated front-plane depth; same shared hand transform");
    }
}
