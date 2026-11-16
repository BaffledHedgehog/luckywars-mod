package me.skywars2.game;

import java.util.ArrayDeque;

/** Recipient-local reservations, including native source cleanup and delayed client ticks. */
final class TikTokVoiceBudget {
    static final int CAPACITY=192;
    private record Batch(long nanos,long tick,int count){}
    private final ArrayDeque<Batch> batches=new ArrayDeque<>();
    private int occupied;
    int available(long now,long clientTick){
        while(!batches.isEmpty()){
            var first=batches.getFirst();
            // The96ms basis can last99ms after native pitch. Require three actual
            // client ticks plus wall time, so tick bursts alone cannot free voices.
            boolean ready=clientTick>=0&&first.tick>=0?clientTick-first.tick>=3&&now-first.nanos>=130_000_000L
                    :now-first.nanos>=200_000_000L;
            if(!ready)break;
            occupied-=batches.removeFirst().count;
        }
        return CAPACITY-occupied;
    }
    void emitted(long now,long clientTick,int count){
        if(count<0||count>available(now,clientTick))throw new IllegalArgumentException("media_voice_budget");
        if(count>0){batches.addLast(new Batch(now,clientTick,count));occupied+=count;}
    }
    static void verify(){
        var budget=new TikTokVoiceBudget();int total=0;
        for(int i=0;i<120;i++){
            long now=i*50_000_000L;int count=Math.min(72,budget.available(now,i));
            if(count<48)throw new IllegalStateException("Normal spectral cadence lost its sustained voice budget");
            budget.emitted(now,i,count);total+=count;
        }
        if(total<120*60)throw new IllegalStateException("Spectral budget unnecessarily reduced normal bandwidth");
        budget=new TikTokVoiceBudget();
        for(int i=0;i<3;i++)budget.emitted(i,0,Math.min(72,budget.available(i,0)));
        if(budget.available(1_000_000_000L,0)!=0||budget.available(3,100)!=0)
            throw new IllegalStateException("Stalled client or burst client ticks released active voices");
        if(budget.available(1_000_000_000L,100)!=CAPACITY)throw new IllegalStateException("Resumed recipient did not release reservations");
    }
}
