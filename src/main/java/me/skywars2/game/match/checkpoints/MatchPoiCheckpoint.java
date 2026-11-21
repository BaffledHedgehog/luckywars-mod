package me.skywars2.game.match.checkpoints;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import me.skywars2.game.GameController;
import me.skywars2.game.entities.mob_ai.MatchMobGoals;

/** One save point owns every dimension's native POI cache and logical write journal. */
final class MatchPoiCheckpoint {
    private final List<MatchPoiStorage> worlds=new ArrayList<>();

    static boolean ready(GameController game){
        if(!MatchMobGoals.enabled())return true;
        for(var level:game.server().getAllLevels())if(!MatchPoiStorage.captureReady(level))return false;
        return true;
    }
    MatchPoiCheckpoint(GameController game){
        if(!MatchMobGoals.enabled())return;
        if(!ready(game))throw new IllegalStateException("Wait for native POI reads and previous restore leases before capture");
        try{for(var level:game.server().getAllLevels())worlds.add(new MatchPoiStorage(level));}
        catch(Throwable failure){closeAsync();throw failure;}
    }
    Restore beginRestoreFrozen(){
        var active=new ArrayList<MatchPoiStorage.Restore>();
        for(var world:worlds)active.add(world.beginRestoreFrozen());
        return new Restore(List.copyOf(active));
    }
    record Restore(List<MatchPoiStorage.Restore> worlds){
        boolean tickFrozen(){boolean ready=true;for(var world:worlds)ready=world.tickFrozen()&&ready;return ready;}
        boolean resumeFrozen(){
            // Do not resume one dimension while a different dimension still has pending reads.
            for(var world:worlds)if(!world.readyToResumeFrozen())return false;
            for(var world:worlds)if(!world.resumeFrozen())throw new IllegalStateException("Native POI readiness changed on the same server thread");
            return true;
        }
    }
    CompletableFuture<Void> closeAsync(){
        return CompletableFuture.allOf(worlds.stream().map(MatchPoiStorage::closeAsync).toArray(CompletableFuture[]::new));
    }
}
