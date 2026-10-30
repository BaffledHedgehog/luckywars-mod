package me.skywars2.game;

import java.util.IdentityHashMap;
import java.util.function.Consumer;
import me.skywars2.mixin.RandomCheckpointAccess;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.*;

/** Immutable native algorithm state, restored into the current RNG object without generating values. */
final class MatchRandomState {
    private record Gaussian(boolean present,double value,boolean has) {
        static Gaussian of(MarsagliaPolarGaussian source){
            if(source==null)return new Gaussian(false,0,false);
            var a=(RandomCheckpointAccess.Gaussian)source;return new Gaussian(true,a.skywars2$value(),a.skywars2$has());
        }
        void restore(MarsagliaPolarGaussian target){
            if(target==null||!present)throw new IllegalStateException("Native Gaussian cache shape mismatch");
            var a=(RandomCheckpointAccess.Gaussian)target;a.skywars2$value(value);a.skywars2$has(has);
        }
    }
    private final Class<?> kind;
    private final Consumer<RandomSource> restore;
    private MatchRandomState(RandomSource source,Consumer<RandomSource> restore){kind=source.getClass();this.restore=restore;}
    static MatchRandomState capture(RandomSource source){return capture(source,new IdentityHashMap<>());}
    private static MatchRandomState capture(RandomSource source,IdentityHashMap<RandomSource,Boolean> active){
        if(source==null||active.put(source,true)!=null)throw new IllegalStateException("Native RNG graph must be present and acyclic");
        try {
            if(source instanceof RandomCheckpointAccess.Dirty dirty){
                var state=capture(dirty.skywars2$delegate(),active);
                return new MatchRandomState(source,target->{var t=(RandomCheckpointAccess.Dirty)target;state.restore(t.skywars2$delegate());t.skywars2$owner().setDirty();});
            }
            if(source.getClass()==WorldgenRandom.class){
                var a=(RandomCheckpointAccess.Worldgen)source;var nested=capture(a.skywars2$delegate(),active);int count=a.skywars2$count();
                var base=(RandomCheckpointAccess.Legacy)source;long seed=base.skywars2$seed().get();var cache=Gaussian.of(base.skywars2$gaussian());
                return new MatchRandomState(source,target->{var t=(RandomCheckpointAccess.Worldgen)target;nested.restore(t.skywars2$delegate());t.skywars2$count(count);
                    var b=(RandomCheckpointAccess.Legacy)target;b.skywars2$seed().set(seed);cache.restore(b.skywars2$gaussian());});
            }
            if(source.getClass()==LegacyRandomSource.class){
                var a=(RandomCheckpointAccess.Legacy)source;long seed=a.skywars2$seed().get();var cache=Gaussian.of(a.skywars2$gaussian());
                return new MatchRandomState(source,target->{var t=(RandomCheckpointAccess.Legacy)target;t.skywars2$seed().set(seed);cache.restore(t.skywars2$gaussian());});
            }
            if(source.getClass()==ThreadSafeLegacyRandomSource.class){
                var a=(RandomCheckpointAccess.ThreadSafe)source;long seed=a.skywars2$seed().get();var cache=Gaussian.of(a.skywars2$gaussian());
                return new MatchRandomState(source,target->{var t=(RandomCheckpointAccess.ThreadSafe)target;t.skywars2$seed().set(seed);cache.restore(t.skywars2$gaussian());});
            }
            if(source.getClass()==SingleThreadedRandomSource.class){
                var a=(RandomCheckpointAccess.Single)source;long seed=a.skywars2$seed();var cache=Gaussian.of(a.skywars2$gaussian());
                return new MatchRandomState(source,target->{var t=(RandomCheckpointAccess.Single)target;t.skywars2$seed(seed);
                    if(!cache.present)t.skywars2$gaussian(null);
                    else{if(t.skywars2$gaussian()==null)t.skywars2$gaussian(new MarsagliaPolarGaussian(target));cache.restore(t.skywars2$gaussian());}});
            }
            if(source.getClass()==XoroshiroRandomSource.class){
                var a=(RandomCheckpointAccess.Xoroshiro)source;var e=(RandomCheckpointAccess.Engine)a.skywars2$engine();
                long lo=e.skywars2$lo(),hi=e.skywars2$hi();var cache=Gaussian.of(a.skywars2$gaussian());
                return new MatchRandomState(source,target->{var t=(RandomCheckpointAccess.Xoroshiro)target;var engine=(RandomCheckpointAccess.Engine)t.skywars2$engine();
                    engine.skywars2$lo(lo);engine.skywars2$hi(hi);cache.restore(t.skywars2$gaussian());});
            }
            throw new IllegalStateException("RNG algorithm requires its own checkpoint adapter: "+source.getClass().getName());
        } finally {active.remove(source);}
    }
    void restore(RandomSource target){
        if(target==null||target.getClass()!=kind)throw new IllegalStateException("Restore the same native RNG algorithm, not a replacement algorithm");
        restore.accept(target);
    }
}
