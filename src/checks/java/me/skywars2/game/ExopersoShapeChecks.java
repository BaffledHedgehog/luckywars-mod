package me.skywars2.game;

import me.skywars2.SkyWars2;
import net.minecraft.world.phys.Vec3;

/** Inspect radial shells and directional sectors rather than equating a crater with a block count. */
public final class ExopersoShapeChecks {
    private ExopersoShapeChecks() { }
    private static void require(boolean ok,String why){if(!ok)throw new IllegalStateException("Exoperso shape: "+why);}
    public static void run(GameController game){
        var shape=NuclearTerrain.wand(Vec3.ZERO);shape.seed=0x51ac3f125L;
        double[] radii={18,25,35,50,65,75,84};double previous=1.001;
        for(double radius:radii){
            int selected=0;int[] sectors=new int[6],kept=new int[6];
            for(int i=0;i<4096;i++){
                double y=1-2*(i+.5)/4096,angle=i*Math.PI*(3-Math.sqrt(5));double horizontal=Math.sqrt(1-y*y);
                double x=Math.cos(angle)*horizontal,z=Math.sin(angle)*horizontal;
                int sector=Math.abs(x)>=Math.abs(y)&&Math.abs(x)>=Math.abs(z)?x>=0?0:1:Math.abs(y)>=Math.abs(z)?y>=0?2:3:z>=0?4:5;
                sectors[sector]++;
                if(shape.contains(x*radius/23,y*radius/23,z*radius/23,radius/23)){selected++;kept[sector]++;}
            }
            double density=selected/4096.0;
            require(density<=previous+.02,"exterior density declines with radius "+radius+" density="+density+" previous="+previous);previous=density;
            if(radius==18)require(selected==4096,"solid spherical core in every direction");
            if(radius==35||radius==50||radius==65)require(density>.03,"frequent far-noise shell exists at "+radius);
            if(radius==84)require(selected==0,"far boundary is spherical, not the preparation box");
            double low=1,high=0;for(int side=0;side<6;side++){double value=kept[side]/(double)sectors[side];low=Math.min(low,value);high=Math.max(high,value);}
            require(high-low<.13,"axis/diagonal sectors have comparable radial density, not a cuboid: "+radius);
            SkyWars2.LOGGER.info("[check] Exoperso radial shell radius={} density={} sectorRange={}..{}",radius,density,low,high);
        }
        SkyWars2.LOGGER.info("[check] PASS exoperso-shape (connected spherical core, frequent decreasing radial noise, isotropic sectors and spherical outer cutoff)");
    }
}
