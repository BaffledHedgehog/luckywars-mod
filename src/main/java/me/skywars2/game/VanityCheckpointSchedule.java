package me.skywars2.game;

/** Acquisition-fixed timeline: no world RNG calls or allocations on ordinary ticks. */
final class VanityCheckpointSchedule {
    record State(long seed,long elapsed,long ordinal,long nextAt) {}
    private final long seed;
    private long elapsed,ordinal,nextAt;

    VanityCheckpointSchedule(long seed){this(new State(seed,0,0,interval(seed,0)));}
    VanityCheckpointSchedule(State state){seed=state.seed();elapsed=state.elapsed();ordinal=state.ordinal();nextAt=state.nextAt();}
    void tick(boolean running){if(running)elapsed++;}
    boolean due(){return elapsed>=nextAt;}
    State state(){return new State(seed,elapsed,ordinal,nextAt);}
    /** Skip missed deadlines, never enqueue several copies of the same current world. */
    State afterCapture(){
        long index=ordinal,next=nextAt;
        while(next<=elapsed)next+=interval(seed,++index);
        return new State(seed,elapsed,index,next);
    }
    private static long interval(long seed,long ordinal){
        long value=seed+0x9e3779b97f4a7c15L*(ordinal+1);
        value=(value^(value>>>30))*0xbf58476d1ce4e5b9L;
        value=(value^(value>>>27))*0x94d049bb133111ebL;
        value^=value>>>31;
        return (30+Math.floorMod(value,571))*20;
    }
}
