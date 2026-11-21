package me.skywars2.game.world.maps;

import java.nio.file.Path;

import me.skywars2.game.GameController;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;

/** Production-safe boundary: write/delete implementations exist only in the test-server JAR. */
public interface MapEditing extends AutoCloseable {
    TestBackend TEST_BACKEND = FabricLoader.getInstance().getEntrypointContainers("skywars2:map-editing",TestBackend.class)
            .stream().filter(entry->entry.getProvider().getMetadata().getId().equals("skywars2"))
            .findFirst().map(entry->entry.getEntrypoint()).orElse(null);
    boolean TEST_DIRECTORY = allowedDirectory(FabricLoader.getInstance().getGameDir());
    void select(ServerPlayer player,int corner);
    void save(ServerPlayer player,String mode,String name);
    void clearSelection(ServerPlayer player);
    AABB selectedBounds(ServerPlayer player);
    boolean busy();
    void tick();
    void close();

    interface TestBackend {
        MapEditing create(GameController game);
        void registerTicket();
        int delete(GameController game,ServerPlayer player,String id);
        void runCheck(GameController game,String name);
        void tickChecks(GameController game);
    }
    static boolean testBuildAvailable() { return TEST_BACKEND!=null; }
    static boolean enabledHere() { return testBuildAvailable() && TEST_DIRECTORY; }
    static boolean allowedDirectory(Path directory) {
        try {
            Path actual=directory.toRealPath();
            return actual.equals(Path.of("E:/servers_mew/skywars2_last").toRealPath())
                    || actual.equals(Path.of("E:/servers_mew/skywars2_last/luckywars-mod/build/verification/key-reuse-server").toRealPath());
        } catch (java.io.IOException absent) { return false; }
    }
    static boolean allowedServer(MinecraftServer server) {
        return server!=null && enabledHere();
    }
    static boolean allowedEditing(ServerPlayer player) { return GameController.operator(player) && allowedServer(player.level().getServer()); }
    static boolean allowedDeletion(ServerPlayer player,MinecraftServer server) {
        return player!=null && allowedServer(server) && GameController.operator(player)
                && player.getGameProfile().name().equals("BaffledHedgehog");
    }
    static MapEditing create(GameController game) {
        if (allowedServer(game.server())) return TEST_BACKEND.create(game);
        return new MapEditing() {
            private void denied(ServerPlayer player) { player.sendSystemMessage(Component.literal("Map writing is unavailable in this build or server.")); }
            public void select(ServerPlayer p,int corner){denied(p);}
            public void save(ServerPlayer p,String mode,String name){denied(p);}
            public void clearSelection(ServerPlayer p){}
            public AABB selectedBounds(ServerPlayer p){return null;}
            public boolean busy(){return false;}
            public void tick(){}
            public void close(){}
        };
    }
    static void registerTicket() {
        if (enabledHere()) TEST_BACKEND.registerTicket();
    }
    static int delete(GameController game,ServerPlayer player,String id) {
        if (!allowedDeletion(player,game.server())) {
            player.sendSystemMessage(Component.literal("Map deletion requires operator BaffledHedgehog on this test server.")); return 0;
        }
        return TEST_BACKEND.delete(game,player,id);
    }
    static void runCheck(GameController game,String name) {
        if (!allowedServer(game.server())) throw new IllegalStateException("Map editing checks are available only on the test server");
        TEST_BACKEND.runCheck(game,name);
    }
    static void tickChecks(GameController game) {
        if (allowedServer(game.server())) TEST_BACKEND.tickChecks(game);
    }
}
