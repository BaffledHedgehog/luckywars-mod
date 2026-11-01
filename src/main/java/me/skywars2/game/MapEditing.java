package me.skywars2.game;

import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;

/** Production-safe boundary: write/delete implementations exist only in the test-server JAR. */
public interface MapEditing extends AutoCloseable {
    boolean TEST_BUILD = detectTestBuild();
    boolean TEST_DIRECTORY = allowedDirectory(FabricLoader.getInstance().getGameDir());
    void select(ServerPlayer player,int corner);
    void save(ServerPlayer player,String mode,String name);
    void clearSelection(ServerPlayer player);
    AABB selectedBounds(ServerPlayer player);
    boolean busy();
    void tick();
    void close();

    private static boolean detectTestBuild() {
        try { Class.forName("me.skywars2.game.MapAuthoring",false,MapEditing.class.getClassLoader()); return true; }
        catch (ClassNotFoundException absent) { return false; }
    }
    static boolean testBuildAvailable() { return TEST_BUILD; }
    static boolean enabledHere() { return TEST_BUILD && TEST_DIRECTORY; }
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
        if (allowedServer(game.server())) {
            try { return (MapEditing)Class.forName("me.skywars2.game.MapAuthoring").getConstructor(GameController.class).newInstance(game); }
            catch (ReflectiveOperationException failure) { throw new IllegalStateException("Cannot initialize test-server map authoring",failure); }
        }
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
        if (!enabledHere()) return;
        try { var method=Class.forName("me.skywars2.game.MapAuthoring").getDeclaredMethod("registerTicket");method.setAccessible(true);method.invoke(null); }
        catch (ReflectiveOperationException failure) { throw new IllegalStateException("Cannot register test map capture ticket",failure); }
    }
    static int delete(GameController game,ServerPlayer player,String id) {
        if (!allowedDeletion(player,game.server())) {
            player.sendSystemMessage(Component.literal("Map deletion requires operator BaffledHedgehog on this test server.")); return 0;
        }
        try { return ((Number)Class.forName("me.skywars2.game.MapDeletion").getMethod("delete",GameController.class,ServerPlayer.class,String.class).invoke(null,game,player,id)).intValue(); }
        catch (ReflectiveOperationException failure) { throw new IllegalStateException("Cannot invoke test-server map deletion",failure); }
    }
    static void runCheck(GameController game,String name) {
        if (!allowedServer(game.server())) throw new IllegalStateException("Map editing checks are available only on the test server");
        if (!name.equals("MapAuthoringChecks") && !name.equals("MapDeletionChecks")) throw new IllegalArgumentException("Unknown editing check");
        try { Class.forName("me.skywars2.game."+name).getMethod("run",GameController.class).invoke(null,game); }
        catch (ReflectiveOperationException failure) { throw new IllegalStateException("Map editing check failed",failure); }
    }
    static void tickChecks(GameController game) {
        if (!allowedServer(game.server())) return;
        try { Class.forName("me.skywars2.game.MapAuthoringChecks").getMethod("tick",GameController.class).invoke(null,game); }
        catch (ReflectiveOperationException failure) { throw new IllegalStateException("Map authoring check tick failed",failure); }
    }
}
