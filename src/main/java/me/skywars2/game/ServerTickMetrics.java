package me.skywars2.game;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

/** One primitive timestamp per native tick; command reads never walk worlds or entities. */
public final class ServerTickMetrics {
    private static final long[] TIMES=new long[18_001];
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
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher){
        var node=SkyWarsCommandTree.add(dispatcher,Commands.literal("tps").executes(context->{
            var server=context.getSource().getServer();long now=System.nanoTime();
            double target=server.tickRateManager().isFrozen()?0:server.tickRateManager().tickrate();
            String message=String.format(java.util.Locale.ROOT,"TPS (1m / 5m / 15m): %.2f / %.2f / %.2f | MSPT: %.2f",
                    tps(60_000_000_000L,now,target),tps(300_000_000_000L,now,target),tps(900_000_000_000L,now,target),server.getAverageTickTimeNanos()/1_000_000d);
            context.getSource().sendSuccess(()->Component.literal(message),false);return 1;
        }));
        dispatcher.getRoot().addChild(node);
    }
}
