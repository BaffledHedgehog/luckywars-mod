package me.skywars2.game;

import java.util.*;
import net.minecraft.world.phys.Vec3;

/** Source distribution/geometry plus the two scheduled actual native Cinema groups. */
public final class AbsoluteCinemaChecks {
    private AbsoluteCinemaChecks(){}
    private static void require(boolean condition,String why){if(!condition)throw new IllegalStateException("Cinema: "+why);}
    public static void sourceRules(){
        var counts=new EnumMap<LbcAbsoluteCinema.Role,Integer>(LbcAbsoluteCinema.Role.class);
        for(int n=1;n<=601;n++)counts.merge(LbcAbsoluteCinema.roleFor(n),1,Integer::sum);
        for(var role:LbcAbsoluteCinema.Role.values())require(counts.get(role)==(role==LbcAbsoluteCinema.Role.NAHIDA?1:100),"uniform601 source distribution "+role);
        for(int n:new int[]{Integer.MIN_VALUE,0,602,Integer.MAX_VALUE}){boolean rejected=false;try{LbcAbsoluteCinema.roleFor(n);}catch(IllegalArgumentException expected){rejected=true;}require(rejected,"invalid source roll rejected");}
        var parent=LbcOreshnik.offsets(false);var child=LbcOreshnik.offsets(true);
        require(parent.size()==19&&child.size()==13&&new HashSet<>(parent).size()==19&&new HashSet<>(child).size()==13,"exact distinct source19/13 blast sites");
        require(parent.subList(0,13).equals(child)&&parent.getFirst().equals(Vec3.ZERO),"child retains source center and six-axis1/3 order");
        require(parent.subList(13,19).equals(List.of(new Vec3(4,4,0),new Vec3(4,0,4),new Vec3(0,4,4),new Vec3(-4,-4,0),new Vec3(-4,0,-4),new Vec3(0,-4,-4))),"exact source signed-pair4 offsets");
        require(parent.size()+11*child.size()==162,"finite parent plus11-child blast closure");
    }
    public static void run(GameController game){CinemaNativeChecks.start(game,false);}
    public static void runRewind(GameController game){CinemaNativeChecks.start(game,true);}
    public static void tick(GameController game){CinemaNativeChecks.tick(game);}
}
