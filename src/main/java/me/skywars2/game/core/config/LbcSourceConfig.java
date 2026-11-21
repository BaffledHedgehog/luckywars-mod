package me.skywars2.game.core.config;

import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

/** Bundled source constants. The same scoreboard operands drive mechanics and tooltips. */
public final class LbcSourceConfig {
    public static final String RESOURCE = "/skywars2/source_config_defaults.json";
    private final MinecraftServer server;
    private final Map<String, Integer> values;

    public LbcSourceConfig(MinecraftServer server) {
        this.server = server;
        this.values = loadValues();
        reload();
    }

    private static Map<String, Integer> loadValues() {
        var values = new LinkedHashMap<String, Integer>();
        try (var input = LbcSourceConfig.class.getResourceAsStream(RESOURCE)) {
            if (input == null) throw new IOException("Missing bundled source config");
            var document = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
            for (var entry : document.getAsJsonObject("values").entrySet()) {
                values.put(entry.getKey(), entry.getValue().getAsBigDecimal().intValueExact());
            }
            for (var entry : document.getAsJsonObject("derived").entrySet()) {
                var rule = entry.getValue().getAsJsonObject();
                // Derived display prices cannot diverge from their actual charge cadence.
                int source = values.get(rule.get("source").getAsString());
                int multiplier = rule.get("multiplier").getAsBigDecimal().intValueExact();
                values.put(entry.getKey(), Math.multiplyExact(source, multiplier));
            }
        } catch (IOException | RuntimeException error) {
            throw new IllegalStateException("Cannot read " + RESOURCE + ": " + error.getMessage(), error);
        }
        return Collections.unmodifiableMap(values);
    }

    public boolean owns(String objective, String holder) {
        return objective.equals("lbc.math") && values.containsKey(holder);
    }

    public int reload() {
        var board = server.getScoreboard();
        var objective = board.getObjective("lbc.math");
        if (objective == null) {
            objective = board.addObjective("lbc.math", ObjectiveCriteria.DUMMY, Component.literal("lbc.math"),
                    ObjectiveCriteria.RenderType.INTEGER, false, null);
        }
        for (var entry : values.entrySet()) {
            board.getOrCreatePlayerScore(ScoreHolder.forNameOnly(entry.getKey()), objective).set(entry.getValue());
        }
        return values.size();
    }
}
