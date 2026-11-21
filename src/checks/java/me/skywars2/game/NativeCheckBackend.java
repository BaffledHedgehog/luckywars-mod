package me.skywars2.game;

/** The test-server entrypoint for the command and its native tick witnesses. */
public final class NativeCheckBackend implements NativeCheckHooks {
    @Override public void run(GameController game, String suite) { GameChecks.run(game, suite); }
    @Override public void tick(GameController game) { GameChecks.tick(game); }
    @Override public void startupPhase(GameController game, String name) { MatchStartup24Checks.phase(game, name); }
    @Override public void startupResetStep(GameController game, String name) { MatchStartup24Checks.resetStep(game, name); }
}
