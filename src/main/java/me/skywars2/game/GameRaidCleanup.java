package me.skywars2.game;

/** Retire native raid owners so their client boss bars cannot survive into the lobby. */
final class GameRaidCleanup {
    private GameRaidCleanup(){}
    static void clear(GameController game){
        for(var level:game.server().getAllLevels()){
            var data=level.getRaids();var raids=((me.skywars2.mixin.LobbyRaidCleanupMixin)data).skywars2$raids();
            if(raids.isEmpty())continue;
            for(var raid:java.util.List.copyOf(raids.values()))raid.stop();
            raids.clear();data.setDirty();
        }
    }
}
