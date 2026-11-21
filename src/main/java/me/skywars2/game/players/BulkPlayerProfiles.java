package me.skywars2.game.players;

import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.ProfileLookupCallback;
import com.mojang.authlib.services.ProfileNotFoundException;
import com.mojang.authlib.services.response.NameAndId;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;

/** Carries one prepared lookup through the original Carpet command on the server thread. */
public final class BulkPlayerProfiles {
    private BulkPlayerProfiles() {}

    public interface Cache {
        net.minecraft.server.players.NameAndId skywars2$cachedName(String name);
    }

    public static final ScopedValue<Lookup> CURRENT = ScopedValue.newInstance();

    public record Lookup(GameProfileRepository repository, String name, Optional<NameAndId> primary,
            String fallbackName, UUID fallbackId, Exception failure) {
        public boolean matches(GameProfileRepository repository, String name) {
            return this.repository == repository && this.name.equalsIgnoreCase(name);
        }

        public void complete(String name, ProfileLookupCallback callback) {
            if (fallbackId != null) callback.onProfileLookupSucceeded(fallbackName, fallbackId);
            else callback.onProfileLookupFailed(name, failure != null ? failure
                    : new ProfileNotFoundException("Server did not find the requested profile"));
        }
    }

    public static Lookup cached(MinecraftServer server, String name) {
        if (server.services().nameToIdCache() instanceof Cache cache) {
            var known = cache.skywars2$cachedName(name);
            if (known != null) return new Lookup(server.services().profileRepository(), name,
                    Optional.of(new NameAndId(known.id(), known.name())), known.name(), known.id(), null);
        }
        return null;
    }

    public static Lookup fetch(GameProfileRepository repository, String name, boolean useOnlineLookup) {
        var primary = repository.findProfileByName(name.toLowerCase(Locale.ROOT));
        if (primary.isPresent() || !useOnlineLookup) return new Lookup(repository, name, primary, null, null, null);
        class Fallback implements ProfileLookupCallback {
            String resolvedName;
            UUID id;
            Exception failure;
            @Override public void onProfileLookupSucceeded(String canonicalName, UUID canonicalId) {
                resolvedName = canonicalName;
                id = canonicalId;
            }
            @Override public void onProfileLookupFailed(String missingName, Exception error) {
                failure = error;
            }
        }
        var fallback = new Fallback();
        repository.findProfilesByNames(new String[]{name}, fallback);
        return new Lookup(repository, name, primary, fallback.resolvedName, fallback.id, fallback.failure);
    }
}
