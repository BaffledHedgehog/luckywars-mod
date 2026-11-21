package me.skywars2.game;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.Display;
import org.joml.Vector3fc;

/** Inspect native display metadata after the real gameplay producer has loaded its preset. */
final class DisplayTransformChecks {
    private DisplayTransformChecks(){}
    private static final EntityDataAccessor<Vector3fc> TRANSLATION=accessor("DATA_TRANSLATION_ID");
    private static final EntityDataAccessor<Vector3fc> SCALE=accessor("DATA_SCALE_ID");
    @SuppressWarnings("unchecked")
    private static EntityDataAccessor<Vector3fc> accessor(String name){
        try{var field=Display.class.getDeclaredField(name);field.setAccessible(true);return (EntityDataAccessor<Vector3fc>)field.get(null);}
        catch(ReflectiveOperationException error){throw new ExceptionInInitializerError(error);}
    }
    static void pose(Display display,float tx,float ty,float tz,float sx,float sy,float sz){
        vector(display.getEntityData().get(TRANSLATION),tx,ty,tz,"translation");
        vector(display.getEntityData().get(SCALE),sx,sy,sz,"scale");
    }
    private static void vector(Vector3fc actual,float x,float y,float z,String field){
        if(Math.abs(actual.x()-x)>1e-5||Math.abs(actual.y()-y)>1e-5||Math.abs(actual.z()-z)>1e-5)
            throw new IllegalStateException("Native display "+field+" rejected authored preset: actual="+actual+" expected="+x+","+y+","+z);
    }
}
