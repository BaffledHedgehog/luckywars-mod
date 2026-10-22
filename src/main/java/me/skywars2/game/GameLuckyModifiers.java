package me.skywars2.game;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.world.scores.ScoreHolder;

/** Global LuckyWars category, separate from player luck and native loot-context luck. */
public final class GameLuckyModifiers {
    private final GameController game;
    private int current;
    private boolean committed;

    GameLuckyModifiers(GameController game) {
        this.game = game;
        syncConfig();
        // A completed LuckyWars result can remain between matches; ordinary SkyWars does not reroll it.
        int previous = game.items().mana().score(ScoreHolder.forNameOnly("final_luck_modifier"), "lbc.math").get();
        current = previous >= 1 && previous <= 12 ? previous : 1;
        if (previous != current) publish();
    }

    /** Source random table: 89 neutral outcomes and one outcome for each fixed variant. */
    public static int resolve(int configured, int draw) {
        if (configured < 0 || configured > 12) throw new IllegalArgumentException("Lucky modifier must be 0..12");
        if (draw < 1 || draw > 100) throw new IllegalArgumentException("Lucky modifier draw must be 1..100");
        if (configured != 0) return configured;
        return draw >= 76 && draw <= 86 ? draw - 74 : 1;
    }

    public int current() { return current; }

    /** Only LuckyWars resolves this selector. Other modes retain it without consuming randomness. */
    public int startMatch() {
        if (committed) return current;
        if (game.config().gameMode == 2) return commit(game.config().luckyModifier);
        committed = true;
        return current;
    }

    /** Unlock the next eligible start, preserving both the GUI setting and the previous result. */
    public void endMatch() { committed = false; }

    /** Configuration publication never rerolls an active category. Called only on actual edits. */
    public void syncConfig() {
        var score = game.items().mana().score(ScoreHolder.forNameOnly("cfg_lucky_modifier"), "swrg.math");
        if (score.get() != game.config().luckyModifier) score.set(game.config().luckyModifier);
    }

    /** Commit once, before the eligible match's first loot/kit roll. */
    public int commit(int configured) {
        if (committed) return current;
        current = resolve(configured, configured == 0 ? ThreadLocalRandom.current().nextInt(1, 101) : 1);
        committed = true;
        publish();
        return current;
    }

    public void reset() {
        current = 1;
        committed = false;
        publish();
    }

    private void publish() {
        var score = game.items().mana().score(ScoreHolder.forNameOnly("final_luck_modifier"), "lbc.math");
        if (score.get() != current) score.set(current);
    }
}
