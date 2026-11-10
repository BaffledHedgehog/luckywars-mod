package me.skywars2.game;

import java.util.WeakHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.loot.LootContext;

/** Shared one-second value from lbc:tick1salways; later successful rolls replace earlier ones. */
public final class LegacySlabRandom {
    private record Sample(long second,int value){}
    private static final WeakHashMap<MinecraftServer,Sample> samples=new WeakHashMap<>();
    private LegacySlabRandom(){}
    public static int value(LootContext context){
        var server=context.getLevel().getServer();long second=server.getTickCount()/20;
        var previous=samples.get(server);if(previous!=null&&previous.second==second)return previous.value;
        var random=context.getLevel().getRandom();int value=random.nextInt(7)-3;
        int[] bounds={30,300,30000,180000,1000000000};float[] chances={.7f,.5f,.25f,.05f,.01f};
        for(int i=0;i<bounds.length;i++)if(random.nextFloat()<chances[i])value=random.nextInt(2*bounds[i]+1)-bounds[i];
        samples.put(server,new Sample(second,value));return value;
    }
}
