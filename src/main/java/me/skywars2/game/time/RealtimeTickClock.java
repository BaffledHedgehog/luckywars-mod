package me.skywars2.game.time;

import java.util.function.LongSupplier;

/** Monotonic twentieths of a second, sampled by native ticks without a worker thread. */
public final class RealtimeTickClock {
    private static final long STEP=50_000_000L;
    private final LongSupplier nanos;
    private long sampled,remainder,ticks;
    public RealtimeTickClock(){this(System::nanoTime);}
    public RealtimeTickClock(LongSupplier nanos){this.nanos=nanos;sampled=nanos.getAsLong();}
    public long advance(boolean paused){
        long now=nanos.getAsLong(),elapsed=Math.max(0,now-sampled);sampled=now;
        if(paused)return 0;
        long total=elapsed+remainder,steps=total/STEP;remainder=total%STEP;ticks+=steps;
        return steps;
    }
    public long ticks(){return ticks;}
    /** Checkpoint restoration reanchors at the current monotonic instant. */
    public void restore(long value){ticks=value;sampled=nanos.getAsLong();remainder=0;}
}
