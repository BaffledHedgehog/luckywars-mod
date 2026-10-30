package me.skywars2.game;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import me.skywars2.mixin.RandomCheckpointAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomSequence;
import net.minecraft.world.RandomSequences;

/** Global native RNG owner, deliberately separate from partial-chunk terrain restores. */
final class MatchWorldRandomCheckpoint {
    private record World(ServerLevel level,RandomSource random,MatchRandomState state,RandomSource sound,MatchRandomState sounds,int blocks) {
        World(ServerLevel level){this(level,level.getRandom(),MatchRandomState.capture(level.getRandom()),
                ((RandomCheckpointAccess.World)level).skywars2$sounds(),MatchRandomState.capture(((RandomCheckpointAccess.World)level).skywars2$sounds()),((RandomCheckpointAccess.World)level).skywars2$blockRandom());}
        void validate(){if(level.getRandom()!=random||((RandomCheckpointAccess.World)level).skywars2$sounds()!=sound)throw new IllegalStateException("Native level RNG identity changed");}
        void restore(){state.restore(random);sounds.restore(sound);((RandomCheckpointAccess.World)level).skywars2$blockRandom(blocks);}
    }
    private record Sequence(RandomSequence original,RandomSource random,MatchRandomState state) {
        Sequence(RandomSequence source){this(source,source.random(),MatchRandomState.capture(source.random()));}
    }
    private final GameController game;
    private final String epoch;
    private final List<World> worlds;
    private final RandomSequences owner;
    private final Map<Identifier,RandomSequence> nativeMap;
    private final Map<Identifier,Sequence> sequences=new LinkedHashMap<>();
    private final int salt;
    private final boolean worldSeed,id;
    MatchWorldRandomCheckpoint(GameController game){
        this.game=game;if(!game.server().isSameThread()||!game.playing()||game.maps().busy())throw new IllegalStateException("Capture RNGs on the stable native match thread");epoch=game.items().epoch();
        var all=new java.util.ArrayList<World>();for(var world:game.server().getAllLevels())all.add(new World(world));worlds=List.copyOf(all);
        owner=game.server().getRandomSequences();var a=(RandomCheckpointAccess.Sequences)owner;nativeMap=a.skywars2$sequences();
        nativeMap.forEach((key,value)->sequences.put(key,new Sequence(value)));salt=a.skywars2$salt();worldSeed=a.skywars2$worldSeed();id=a.skywars2$id();
    }
    void restoreFrozen(){
        if(!game.server().isSameThread()||!game.server().tickRateManager().isFrozen()||!game.playing()||game.maps().busy()||!game.items().currentEpoch(epoch))throw new IllegalStateException("Restore RNGs in the same frozen native match");
        if(game.server().getRandomSequences()!=owner||((RandomCheckpointAccess.Sequences)owner).skywars2$sequences()!=nativeMap)throw new IllegalStateException("Native named RNG service identity changed");
        int count=0;for(var world:game.server().getAllLevels())count++;
        if(count!=worlds.size())throw new IllegalStateException("Reconcile native dimensions before RNG replay");
        for(var world:worlds){if(game.server().getLevel(world.level.dimension())!=world.level)throw new IllegalStateException("Native dimension instance changed");world.validate();}
        for(var image:sequences.values())if(image.original.random()!=image.random)throw new IllegalStateException("Saved native named RNG identity changed");
        for(var world:worlds)world.restore();
        owner.setSeedDefaults(salt,worldSeed,id);nativeMap.clear();
        for(var entry:sequences.entrySet()){var image=entry.getValue();image.state.restore(image.random);nativeMap.put(entry.getKey(),image.original);}
        owner.setDirty();
    }
}
