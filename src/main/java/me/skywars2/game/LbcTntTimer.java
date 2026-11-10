package me.skywars2.game;

import net.minecraft.network.chat.Component;

/** Fuse is authoritative ticks: integer seconds above one, rounded tenths below it. */
public final class LbcTntTimer {
    private LbcTntTimer(){}
    private static final Component INFINITE=Component.literal("\u221e");
    private static final Component[] FRACTIONS=new Component[20];
    static {for(int i=0;i<20;i++)FRACTIONS[i]=Component.literal(i==19?"1.0":"0."+((i+1)/2));}
    public static int bucket(int fuse){return fuse<0?Integer.MIN_VALUE:fuse>=20?fuse/20:-(fuse+1)/2-1;}
    public static Component label(int fuse){return fuse<0?INFINITE:fuse>=20?Component.literal(Integer.toString(fuse/20)):FRACTIONS[fuse];}
}
