package me.skywars2.game;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.world.level.GameType;

/** The explicit core phase of a full rewind; does not pretend to restore native player NBT. */
final class MatchCoreCheckpoint {
    private final GameController game;
    private final GameController.Checkpoint core;
    private final MatchValueSnapshot config;
    private final Map<UUID, MatchValueSnapshot> profiles;
    private final MatchScoreboardCheckpoint scoreboard;
    private final LbcTeamColors.Checkpoint teams;
    private final MatchEvents.Timeline events;
    private final GameLuckyModifiers.Checkpoint lucky;
    private final ZoneBarrier.Checkpoint zone;
    private final Map<LbcWitchClaims.Mode,UUID> authorities;
    private final Set<UUID> spectators;
    private final Map<UUID,GameType> online = new LinkedHashMap<>();

    MatchCoreCheckpoint(GameController game) {
        this.game=game;core=game.checkpointCore();config=new MatchValueSnapshot(game.config());
        profiles=game.players().checkpointProfiles();scoreboard=new MatchScoreboardCheckpoint(game.server());
        teams=game.teamColors().checkpoint();events=game.events().checkpointTimeline();
        lucky=game.luckyModifiers().checkpoint();zone=game.zoneBarrier().checkpoint();
        authorities=game.items().witchClaims().ownership();spectators=game.spectators().checkpointMembers();
        for(var player:game.server().getPlayerList().getPlayers())online.put(player.getUUID(),player.gameMode.getGameModeForPlayer());
    }

    /**
     * Full owner quarantines inputs; terrain restores before this phase, native bodies,
     * actor/ability state and camera attachment afterwards. Disk persistence is a final
     * commit step of that owner, not a side effect of each partial component.
     */
    void restoreFrozen() {
        game.validateCheckpoint(core);
        config.restore();game.players().restoreProfiles(profiles);scoreboard.restore(game.server());
        game.restoreCore(core);game.teamColors().restore(teams);game.events().restoreTimeline(events);
        game.luckyModifiers().restore(lucky);game.items().witchClaims().restoreOwnership(authorities);
        game.spectators().restoreMembers(spectators,online.keySet());
        for(var player:game.server().getPlayerList().getPlayers()) {
            var mode=online.get(player.getUUID());if(mode!=null)player.setGameMode(mode);
        }
        game.zoneBarrier().restore(zone);
    }
}
