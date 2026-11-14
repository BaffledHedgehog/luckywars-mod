package me.skywars2.game;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

/** One primitive timestamp per native tick; command reads never walk worlds or entities. */
public final class ServerTickMetrics {
    private static final long[] TIMES=new long[18_001];
    private static final long[] DURATIONS=new long[100];
    private static final int[] DURATION_TICKS=new int[100];
    private static MinecraftServer durationOwner;
    private static int durationCount;
    private static MinecraftServer owner;
    private static int cursor,count;
    private ServerTickMetrics() {}
    static void tick(MinecraftServer server){
        if(owner!=server){owner=server;cursor=count=0;}
        record(System.nanoTime());
    }
    static void record(long now){TIMES[cursor]=now;cursor=(cursor+1)%TIMES.length;count=Math.min(count+1,TIMES.length);}
    static double tps(long window,long now,double target){
        if(count<2)return target;
        int newest=Math.floorMod(cursor-1,TIMES.length),oldest=newest,intervals=0;
        for(int step=1;step<count;step++){
            oldest=Math.floorMod(newest-step,TIMES.length);intervals=step;
            if(now-TIMES[oldest]>=window)break;
        }
        long elapsed=now-TIMES[oldest];
        return elapsed<=0?target:Math.min(target,intervals*1_000_000_000d/elapsed);
    }
    /** Called outside the real tick method, after all Fabric/mod end callbacks finish. */
    public static void recordDuration(MinecraftServer server,long nanos){
        if(durationOwner!=server){durationOwner=server;durationCount=0;java.util.Arrays.fill(DURATIONS,0);java.util.Arrays.fill(DURATION_TICKS,Integer.MIN_VALUE);}
        int tick=server.getTickCount(),index=Math.floorMod(tick,DURATIONS.length);
        DURATIONS[index]=nanos;DURATION_TICKS[index]=tick;durationCount=Math.min(DURATIONS.length,durationCount+1);
    }
    static long completedTickNanos(MinecraftServer server,int tick){
        int index=Math.floorMod(tick,DURATIONS.length);
        return durationOwner==server&&DURATION_TICKS[index]==tick?DURATIONS[index]:-1;
    }
    private static double meanNanos(MinecraftServer server){
        if(durationOwner!=server||durationCount==0)return server.getAverageTickTimeNanos();
        long sum=0;for(long value:DURATIONS)sum+=value;return (double)sum/durationCount;
    }
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher){
        var node=SkyWarsCommandTree.add(dispatcher,Commands.literal("tps").executes(context->{
            var server=context.getSource().getServer();long now=System.nanoTime();
            double target=server.tickRateManager().isFrozen()?0:server.tickRateManager().tickrate();
            String message=String.format(java.util.Locale.ROOT,"TPS (1m / 5m / 15m): %.2f / %.2f / %.2f | MSPT: %.2f",
                    tps(60_000_000_000L,now,target),tps(300_000_000_000L,now,target),tps(900_000_000_000L,now,target),meanNanos(server)/1_000_000d);
            context.getSource().sendSuccess(()->Component.literal(message),false);return 1;
        }));
        dispatcher.getRoot().addChild(node);
    }
}
