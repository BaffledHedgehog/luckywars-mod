package me.skywars2.game;

/** Compatibility entry for the current loose-TMT nonactivation contract. */
public final class TmtDropChecks {
    private TmtDropChecks() {}
    public static void run(GameController game){
        FeedbackTmtDropChecks.run(game);
        me.skywars2.SkyWars2.LOGGER.info("[check] PASS tmt-drops (loose TMT is not admitted to native drop dispatch)");
    }
    public static void tick(GameController game) {}
}
