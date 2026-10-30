package me.skywars2.mixin;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomSequence;
import net.minecraft.world.RandomSequences;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exact native RNG internals, including lazy/cached Gaussian state; never substitute setSeed. */
public final class RandomCheckpointAccess {
    private RandomCheckpointAccess() { }
    @Mixin(LegacyRandomSource.class) public interface Legacy {
        @Accessor("seed") AtomicLong skywars2$seed();
        @Accessor("gaussianSource") MarsagliaPolarGaussian skywars2$gaussian();
    }
    @Mixin(ThreadSafeLegacyRandomSource.class) public interface ThreadSafe {
        @Accessor("seed") AtomicLong skywars2$seed();
        @Accessor("gaussianSource") MarsagliaPolarGaussian skywars2$gaussian();
    }
    @Mixin(SingleThreadedRandomSource.class) public interface Single {
        @Accessor("seed") long skywars2$seed();
        @Accessor("seed") void skywars2$seed(long value);
        @Accessor("gaussianSource") MarsagliaPolarGaussian skywars2$gaussian();
        @Accessor("gaussianSource") void skywars2$gaussian(MarsagliaPolarGaussian value);
    }
    @Mixin(XoroshiroRandomSource.class) public interface Xoroshiro {
        @Accessor("randomNumberGenerator") Xoroshiro128PlusPlus skywars2$engine();
        @Accessor("gaussianSource") MarsagliaPolarGaussian skywars2$gaussian();
    }
    @Mixin(Xoroshiro128PlusPlus.class) public interface Engine {
        @Accessor("seedLo") long skywars2$lo();
        @Accessor("seedLo") void skywars2$lo(long value);
        @Accessor("seedHi") long skywars2$hi();
        @Accessor("seedHi") void skywars2$hi(long value);
    }
    @Mixin(MarsagliaPolarGaussian.class) public interface Gaussian {
        @Accessor("nextNextGaussian") double skywars2$value();
        @Accessor("nextNextGaussian") void skywars2$value(double value);
        @Accessor("haveNextNextGaussian") boolean skywars2$has();
        @Accessor("haveNextNextGaussian") void skywars2$has(boolean value);
    }
    @Mixin(WorldgenRandom.class) public interface Worldgen {
        @Accessor("randomSource") RandomSource skywars2$delegate();
        @Accessor("count") int skywars2$count();
        @Accessor("count") void skywars2$count(int value);
    }
    @Mixin(targets="net.minecraft.world.RandomSequences$DirtyMarkingRandomSource") public interface Dirty {
        @Accessor("random") RandomSource skywars2$delegate();
        @Accessor("this$0") RandomSequences skywars2$owner();
    }
    @Mixin(RandomSequences.class) public interface Sequences {
        @Accessor("sequences") Map<Identifier,RandomSequence> skywars2$sequences();
        @Accessor("salt") int skywars2$salt();
        @Accessor("includeWorldSeed") boolean skywars2$worldSeed();
        @Accessor("includeSequenceId") boolean skywars2$id();
    }
    @Mixin(Level.class) public interface World {
        @Accessor("randValue") int skywars2$blockRandom();
        @Accessor("randValue") void skywars2$blockRandom(int value);
        @Accessor("soundSeedGenerator") RandomSource skywars2$sounds();
    }
}
