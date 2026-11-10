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
        // A saved scoreboard is not a live match. Never resurrect its previous category.
        current = 1;
        publish();
    }

    /** Source random table: 89 neutral outcomes and one outcome for each fixed variant. */
    public static int resolve(int configured, int draw) {
        if (configured < 0 || configured > 12) throw new IllegalArgumentException("Lucky modifier must be 0..12");
        if (draw < 1 || draw > 100) throw new IllegalArgumentException("Lucky modifier draw must be 1..100");
        if (configured != 0) return configured;
        return draw >= 76 && draw <= 86 ? draw - 74 : 1;
    }

    public int current() { return current; }
    record Checkpoint(int current, boolean committed) { }
    Checkpoint checkpoint() { return new Checkpoint(current, committed); }
    void restore(Checkpoint image) { current = image.current; committed = image.committed; }

    /** Only LuckyWars resolves this selector. Other modes start neutral without consuming randomness. */
    public int startMatch() {
        return startMatch(game.gameMode());
    }
    /** Explicit startup context also permits isolated source-policy checks without opening a mode. */
    int startMatch(int mode) {
        if (committed) return current;
        if (mode == 2) return commit(game.config().luckyModifier);
        current = 1;
        committed = true;
        publish();
        return current;
    }

    /** The configured selector persists; its resolved match result does not. */
    public void endMatch() { reset(); }

    /** Configuration publication never rerolls an active category. Called only on actual edits. */
    public void syncConfig() {
        MatchSettings.sync(game);
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
