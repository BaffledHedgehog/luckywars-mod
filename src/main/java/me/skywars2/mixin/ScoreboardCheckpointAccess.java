package me.skywars2.mixin;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.ScoreboardSaveData;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Native object identity matters to cached ScoreAccess and team/ability references. */
@Mixin(Scoreboard.class)
public interface ScoreboardCheckpointAccess {
    @Accessor("objectivesByName") Object2ObjectMap<String, Objective> skywars2$objectives();
    @Accessor("objectivesByCriteria") Reference2ObjectMap<ObjectiveCriteria, List<Objective>> skywars2$criteria();
    @Accessor("playerScores") Map<String, Object> skywars2$players();
    @Accessor("teamsByName") Object2ObjectMap<String, PlayerTeam> skywars2$teams();

    @Mixin(targets = "net.minecraft.world.scores.PlayerScores")
    interface Player {
        @Accessor("scores") Reference2ObjectOpenHashMap<Objective, Score> skywars2$scores();
    }

    @Mixin(ServerScoreboard.class)
    interface Server {
        @Invoker("store") ScoreboardSaveData.Packed skywars2$store();
        @Accessor("trackedObjectives") Set<Objective> skywars2$tracked();
    }
}
