package me.skywars2.game;

import net.minecraft.server.level.ServerPlayer;

/** Registered only by the test-server artifact. */
public final class MapAuthoringBackend implements MapEditing.TestBackend {
    @Override public MapEditing create(GameController game) { return new MapAuthoring(game); }
    @Override public void registerTicket() { MapAuthoring.registerTicket(); }
    @Override public int delete(GameController game,ServerPlayer player,String id) { return MapDeletion.delete(game,player,id); }
    @Override public void runCheck(GameController game,String name) {
        switch(name){
            case "MapAuthoringChecks" -> MapAuthoringChecks.run(game);
            case "MapDeletionChecks" -> MapDeletionChecks.run(game);
            default -> throw new IllegalArgumentException("Unknown editing check");
        }
    }
    @Override public void tickChecks(GameController game) { MapAuthoringChecks.tick(game); }
}
