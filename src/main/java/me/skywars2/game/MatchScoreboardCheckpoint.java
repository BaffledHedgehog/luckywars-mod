package me.skywars2.game;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import me.skywars2.mixin.ScoreboardCheckpointAccess;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.ScoreboardSaveData;

/** Native codec value image plus original identity handles; no replacement scoreboard. */
final class MatchScoreboardCheckpoint {
    private record PlayerImage(Object original, Map<Objective, Score> scores) { }
    private final ServerScoreboard board;
    private final Tag packed;
    private final Map<String, Objective> objectives;
    private final Map<String, PlayerTeam> teams;
    private final Map<String, PlayerImage> players = new LinkedHashMap<>();
    private final Set<Objective> tracked;

    MatchScoreboardCheckpoint(MinecraftServer server) {
        if (!server.isSameThread()) throw new IllegalStateException("Server thread required");
        board = server.getScoreboard();
        var access = (ScoreboardCheckpointAccess) board;
        var nativeServer = (ScoreboardCheckpointAccess.Server) board;
        var ops = server.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        packed = ScoreboardSaveData.Packed.CODEC.encodeStart(ops, nativeServer.skywars2$store()).getOrThrow().copy();
        objectives = new LinkedHashMap<>(access.skywars2$objectives());
        teams = new LinkedHashMap<>(access.skywars2$teams());
        tracked = Set.copyOf(nativeServer.skywars2$tracked());
        access.skywars2$players().forEach((name, info) -> players.put(name,
                new PlayerImage(info, new LinkedHashMap<>(((ScoreboardCheckpointAccess.Player) info).skywars2$scores()))));
    }

    void restore(MinecraftServer server) {
        if (!server.isSameThread() || server.getScoreboard() != board || !server.tickRateManager().isFrozen())
            throw new IllegalStateException("Restore the original scoreboard on a frozen server thread");
        var ops = server.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        var values = ScoreboardSaveData.Packed.CODEC.parse(ops, packed.copy()).getOrThrow();
        var access = (ScoreboardCheckpointAccess) board;
        // Public native removals retire old client tracking and delete future entries.
        for (var slot : DisplaySlot.values()) board.setDisplayObjective(slot, null);
        for (var objective : List.copyOf(board.getObjectives())) board.removeObjective(objective);
        for (var owner : List.copyOf(board.getTrackedPlayers())) board.resetAllPlayerScores(owner);
        for (var team : List.copyOf(board.getPlayerTeams())) board.removePlayerTeam(team);
        access.skywars2$objectives().clear(); access.skywars2$criteria().clear(); access.skywars2$players().clear(); access.skywars2$teams().clear();

        for (var saved : values.objectives()) {
            var objective = objectives.get(saved.name());
            access.skywars2$objectives().put(saved.name(), objective);
            access.skywars2$criteria().computeIfAbsent(objective.getCriteria(), ignored -> new ArrayList<>()).add(objective);
            objective.setDisplayName(saved.displayName()); objective.setRenderType(saved.renderType());
            objective.setDisplayAutoUpdate(saved.displayAutoUpdate()); objective.setNumberFormat(saved.numberFormat().orElse(null));
            board.onObjectiveAdded(objective);
        }
        var scoreValues = new LinkedHashMap<String, Map<String, Score.Packed>>();
        for (var score : values.scores()) scoreValues.computeIfAbsent(score.owner(), ignored -> new LinkedHashMap<>()).put(score.objective(), score.score());
        for (var entry : players.entrySet()) {
            var image = entry.getValue();
            var nativeScores = ((ScoreboardCheckpointAccess.Player) image.original).skywars2$scores();
            nativeScores.clear(); nativeScores.putAll(image.scores);
            for (var score : image.scores.entrySet()) {
                var value = scoreValues.get(entry.getKey()).get(score.getKey().getName());
                score.getValue().value(value.value()); score.getValue().setLocked(value.locked());
                score.getValue().display(value.display().orElse(null)); score.getValue().numberFormat(value.numberFormat().orElse(null));
            }
            access.skywars2$players().put(entry.getKey(), image.original);
        }
        for (var saved : values.teams()) {
            var team = teams.get(saved.name());
            team.getPlayers().clear(); access.skywars2$teams().put(saved.name(), team);
            // Native setters publish MODIFY packets. Reintroduce the team first so
            // existing clients never receive modifications for an unknown team.
            board.onTeamAdded(team);
            saved.displayName().ifPresent(team::setDisplayName); team.setColor(saved.color());
            team.setAllowFriendlyFire(saved.allowFriendlyFire()); team.setSeeFriendlyInvisibles(saved.seeFriendlyInvisibles());
            team.setPlayerPrefix(saved.memberNamePrefix()); team.setPlayerSuffix(saved.memberNameSuffix());
            team.setNameTagVisibility(saved.nameTagVisibility()); team.setDeathMessageVisibility(saved.deathMessageVisibility());
            team.setCollisionRule(saved.collisionRule());
            for (var member : saved.players()) board.addPlayerToTeam(member, team);
        }
        values.displaySlots().forEach((slot, name) -> board.setDisplayObjective(slot, objectives.get(name)));
        var nativeServer = (ScoreboardCheckpointAccess.Server) board;
        for (var objective : tracked) if (!nativeServer.skywars2$tracked().contains(objective)) board.startTrackingObjective(objective);
    }
}
