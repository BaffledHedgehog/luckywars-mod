package me.skywars2.game.genie;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import me.skywars2.game.GameController;
import me.skywars2.game.effects.magic.LbcMana;
import me.skywars2.game.skills.levelups.LbcSourceCounters;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.server.level.ServerPlayer;

/** A read of the native player state and writes through its existing owners. */
public final class GeniePlayerParameters {
    private static final Map<String, String> MANA_KEYS = Map.of(
            "mana", "current", "manamax", "maximum", "manaregen", "base_regen_per_tick",
            "manaregen_visual", "modified_regen_per_tick", "effect_boost_mana", "boost_ticks",
            "reverse_mana", "reverse_seconds", "nomagic", "no_magic_seconds");
    private static final Set<String> DERIVED = Set.of("manaregen_visual", "mana_effective_regen");
    private static final Set<String> ROCKET_COUNTERS = Set.of("rocket_jumps", "rocket_cooldown_ticks");

    private GeniePlayerParameters() {}

    public static JsonObject mana(GameController game, ServerPlayer player) {
        var result = new JsonObject();
        MANA_KEYS.forEach((key, field) -> result.addProperty(field, read(game, player, key)));
        result.addProperty("effective_regen_per_tick", read(game, player, "mana_effective_regen"));
        result.addProperty("wand_overdraft", game.items().mana().overloadArmor(player) == 15);
        return result;
    }

    public static JsonObject wishContext(GameController game, ServerPlayer player) {
        var context = new JsonObject();
        context.add("mana", mana(game, player));
        context.addProperty("lives", game.lives(player.getUUID()));
        var readable = new JsonArray();
        var writable = new JsonArray();
        var names = new TreeSet<>(LbcSourceCounters.NATIVE);
        names.addAll(MANA_KEYS.keySet());
        names.addAll(DERIVED);
        names.addAll(ROCKET_COUNTERS);
        names.add("lives");
        names.addAll(LbcMana.POTION_TIMERS);
        var objectives = game.server().getScoreboard().getObjectives();
        objectives.forEach(objective -> names.add(objective.getName()));
        for (String key : names) {
            readable.add(key);
            if (writable(game, key)) writable.add(key);
        }
        context.add("player_counter_ids", readable);
        context.add("mutable_player_counter_ids", writable);
        var rockets = new JsonObject();
        rockets.addProperty("rocket_jumps", read(game, player, "rocket_jumps"));
        rockets.addProperty("rocket_cooldown_ticks", read(game, player, "rocket_cooldown_ticks"));
        context.add("rocket_boots", rockets);
        return context;
    }

    public static Integer read(GameController game, ServerPlayer player, String key) {
        var mana = game.items().mana();
        return switch (key) {
            case "mana" -> mana.get(player);
            case "manamax" -> mana.maximum(player);
            case "manaregen" -> mana.baseRegen(player);
            case "manaregen_visual" -> mana.hudValue(player, key);
            case "mana_effective_regen" -> mana.activeRegeneration(player);
            case "lives" -> game.lives(player.getUUID());
            case "rocket_jumps" -> game.items().scriptRocketJumps(player);
            case "rocket_cooldown_ticks" -> game.items().scriptRocketCooldown(player);
            default -> {
                Integer current = new LbcSourceCounters(game).read(player, key);
                boolean known = MANA_KEYS.containsKey(key) || LbcMana.POTION_TIMERS.contains(key)
                        || game.server().getScoreboard().getObjective(key) != null;
                yield current != null ? current : known ? Integer.valueOf(0) : null;
            }
        };
    }

    private static boolean writable(GameController game, String key) {
        if (DERIVED.contains(key)) return false;
        if (key.equals("lives") || LbcSourceCounters.NATIVE.contains(key) || MANA_KEYS.containsKey(key)
                || LbcMana.POTION_TIMERS.contains(key) || ROCKET_COUNTERS.contains(key)) return true;
        var objective = game.server().getScoreboard().getObjective(key);
        return objective != null && !objective.getCriteria().isReadOnly();
    }

    public static Integer change(GameController game, ServerPlayer actor, ServerPlayer player,
                                 String key, boolean add, int amount) {
        if (!writable(game, key) || !game.items().mana().visible(player)) return null;
        Integer previous = read(game, player, key);
        if (previous == null) return null;
        long requested = add ? (long) previous + amount : amount;
        int value = (int) Math.clamp(requested, Integer.MIN_VALUE, Integer.MAX_VALUE);
        var mana = game.items().mana();
        switch (key) {
            case "mana" -> mana.set(player, value);
            case "lives" -> { if (!game.changeRemainingLives(player, value)) return null; }
            case "rocket_jumps" -> game.items().scriptRocketJumps(player, value);
            case "rocket_cooldown_ticks" -> game.items().scriptRocketCooldown(player, value);
            case "manamax" -> mana.maximum(player, value);
            case "manaregen" -> mana.baseRegen(player, value);
            case "effect_boost_mana" -> mana.boost(player, value);
            case "reverse_mana" -> {
                if (value > previous) mana.applyReverse(actor, player, value - previous);
                else mana.reverse(player, value);
            }
            case "nomagic" -> mana.noMagic(player, value);
            case "shock" -> game.items().electro().setScriptShock(player, actor, value);
            case "scared", "lookback", "twitching" -> {
                if (value > previous) {
                    int delta = value - previous;
                    game.items().psycho().afflict(player, actor,
                            key.equals("scared") ? delta : 0,
                            key.equals("lookback") ? delta : 0,
                            key.equals("twitching") ? delta : 0, 0);
                } else game.items().psycho().reduceSourceStatus(player, key, Math.max(0, value));
            }
            default -> {
                if (LbcMana.POTION_TIMERS.contains(key)) mana.setPotionCounter(player, key, value);
                else new LbcSourceCounters(game).write(player, key, value);
            }
        }
        return read(game, player, key);
    }
}
