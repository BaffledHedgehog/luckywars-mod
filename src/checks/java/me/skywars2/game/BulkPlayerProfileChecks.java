package me.skywars2.game;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.ProfileLookupCallback;
import com.mojang.authlib.services.response.NameAndId;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.Services;
import net.minecraft.server.players.CachedUserNameToIdResolver;
import net.minecraft.server.players.ProfileResolver;
import net.minecraft.server.permissions.PermissionSet;

/** A controlled profile service under the real bulk command, resolver and Carpet login. */
final class BulkPlayerProfileChecks implements AutoCloseable {
    private static final String PREFIX = "ChunkGateLookup";
    private final MinecraftServer server;
    private final Services original;
    private final Field services;
    private final Path file;
    private final boolean online;
    private final int start;
    private final List<UUID> ids = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
    private final CountDownLatch release = new CountDownLatch(1);
    private final ConcurrentLinkedQueue<String> requests = new ConcurrentLinkedQueue<>();
    private int observed = -1;
    private final net.minecraft.server.level.ServerPlayer requester;
    private boolean opAdded;

    BulkPlayerProfileChecks(MinecraftServer server, CommandSourceStack source) throws Exception {
        this.server = server;
        requester = source.getPlayerOrException();
        original = server.services();
        online = server.usesAuthentication();
        start = server.getTickCount();
        services = MinecraftServer.class.getDeclaredField("services");
        services.setAccessible(true);
        file = Files.createTempFile("skywars2-profile-check-", ".json");
        var repository = new GameProfileRepository() {
            @Override public Optional<NameAndId> findProfileByName(String name) {
                require(!server.isSameThread(), "profile name request runs off the tick");
                requests.add(name);
                if (!name.equalsIgnoreCase(PREFIX + "1")) return Optional.empty();
                try {
                    require(release.await(12, TimeUnit.SECONDS), "main tick releases the pending request");
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(error);
                }
                return Optional.of(new NameAndId(ids.getFirst(), PREFIX + "1"));
            }

            @Override public void findProfilesByNames(String[] names, ProfileLookupCallback callback) {
                require(!server.isSameThread(), "authenticated fallback runs off the tick");
                require(names.length == 1 && names[0].equals(PREFIX + "2"), "fallback retains the native name");
                requests.add("fallback");
                callback.onProfileLookupSucceeded(names[0], ids.get(1));
            }
        };
        var cache = new CachedUserNameToIdResolver(repository, file.toFile());
        cache.resolveOfflineUsers(false);
        cache.add(new net.minecraft.server.players.NameAndId(ids.get(2), PREFIX + "3"));
        var profiles = new ProfileResolver() {
            @Override public Optional<GameProfile> fetchByName(String name) { return Optional.empty(); }
            @Override public Optional<GameProfile> fetchById(UUID id) { return Optional.empty(); }
        };
        try {
            services.set(server, new Services(original.sessionService(), original.servicesKeySet(), repository, cache, profiles));
            server.setUsesAuthentication(true);
            if(!server.getPlayerList().isOp(requester.nameAndId())){server.getPlayerList().op(requester.nameAndId());opAdded=true;}
            var dispatcher = server.getCommands().getDispatcher();
            var nativeSource=requester.createCommandSourceStack();
            require(net.minecraft.commands.Commands.hasPermission(net.minecraft.commands.Commands.LEVEL_GAMEMASTERS).test(nativeSource),"real in-game operator permission, not a fabricated elevated command source");
            var parsed=dispatcher.parse("player spawn " + PREFIX + " 3",nativeSource);
            require(!parsed.getReader().canRead()&&parsed.getExceptions().isEmpty(),"actual operator parses complete standalone bulk spawn arguments");
            // Exercise the real incoming in-game command path, not just dispatcher
            // predicates or a console source with a substituted player entity.
            requester.connection.handleChatCommand(new net.minecraft.network.protocol.game.ServerboundChatCommandPacket("player spawn " + PREFIX + " 3"));
        } catch (Exception | AssertionError failure) {
            close();
            throw failure;
        }
    }

    boolean step() {
        int now = server.getTickCount();
        require(now - start < 100, "all three native Carpet logins complete within five seconds");
        if (release.getCount() != 0) {
            if (requests.isEmpty()) return false;
            if (observed == -1) observed = now;
            require(server.getPlayerList().getPlayer(ids.getFirst()) == null, "profile not available yet");
            if (now - observed < 3) return false;
            release.countDown();
        }
        if (ids.stream().anyMatch(id -> server.getPlayerList().getPlayer(id) == null)) return false;
        for (int i = 0; i < ids.size(); i++) {
            var player = server.getPlayerList().getPlayer(ids.get(i));
            require(player.getGameProfile().name().equals(PREFIX + (i + 1))
                    && player.connection.getClass().getName().equals("carpet.patches.NetHandlerPlayServerFake"),
                    "native Carpet actor retains the resolved or cached UUID");
        }
        try {
            require(server.getCommands().getDispatcher().execute("player "+PREFIX+"1 look east",requester.createCommandSourceStack())>0,
                    "ordinary Carpet individual-player control still executes after bulk registration");
        } catch(com.mojang.brigadier.exceptions.CommandSyntaxException problem){throw new AssertionError("Original Carpet command path was damaged",problem);}
        require(List.copyOf(requests).equals(List.of((PREFIX + "1").toLowerCase(java.util.Locale.ROOT),
                (PREFIX + "2").toLowerCase(java.util.Locale.ROOT), "fallback")),
                "only the two missing cache entries use the repository, with a single authenticated fallback");
        return true;
    }

    private static void require(boolean value, String why) {
        if (!value) throw new AssertionError("Bulk Carpet profiles: " + why);
    }

    @Override public void close() throws Exception {
        release.countDown();
        for (UUID id : ids) {
            var player = server.getPlayerList().getPlayer(id);
            if (player != null) server.getPlayerList().remove(player);
        }
        services.set(server, original);
        server.setUsesAuthentication(online);
        if(opAdded){server.getPlayerList().deop(requester.nameAndId());opAdded=false;}
        Files.deleteIfExists(file);
    }
}
