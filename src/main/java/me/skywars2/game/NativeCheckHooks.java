package me.skywars2.game;

import net.fabricmc.loader.api.FabricLoader;

/** Native verification callbacks supplied by the test-server artifact. */
public interface NativeCheckHooks {
    void run(GameController game, String suite);
    void tick(GameController game);
    void startupPhase(GameController game, String name);
    void startupResetStep(GameController game, String name);

    static NativeCheckHooks get() {
        return Provider.INSTANCE;
    }

    static void phase(GameController game, String name) {
        var hooks = get();
        if (hooks != null) hooks.startupPhase(game, name);
    }

    static void resetStep(GameController game, String name) {
        var hooks = get();
        if (hooks != null) hooks.startupResetStep(game, name);
    }

    final class Provider {
        private static final NativeCheckHooks INSTANCE = load();

        private Provider() {}

        private static NativeCheckHooks load() {
            var entrypoints = FabricLoader.getInstance().getEntrypoints("skywars2:test-checks", NativeCheckHooks.class);
            if (entrypoints.size() > 1) throw new IllegalStateException("Multiple native verification entrypoints");
            return entrypoints.isEmpty() ? null : entrypoints.getFirst();
        }
    }
}
