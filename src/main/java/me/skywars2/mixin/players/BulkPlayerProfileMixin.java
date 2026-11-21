package me.skywars2.mixin.players;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.ProfileLookupCallback;
import com.mojang.authlib.services.response.NameAndId;

import me.skywars2.game.players.BulkPlayerProfiles;

import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import net.minecraft.server.players.CachedUserNameToIdResolver;
import net.minecraft.server.players.OldUsersConverter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CachedUserNameToIdResolver.class)
public abstract class BulkPlayerProfileMixin implements BulkPlayerProfiles.Cache {
    @Shadow @Final private Map<String, ?> profilesByName;

    @Override public net.minecraft.server.players.NameAndId skywars2$cachedName(String name) {
        var entry = (Entry) profilesByName.get(name.toLowerCase(Locale.ROOT));
        return entry != null && entry.skywars2$expiration().getTime() > System.currentTimeMillis()
                ? entry.skywars2$identity() : null;
    }

    @WrapOperation(method = "lookupGameProfile", at = @At(value = "INVOKE",
            target = "Lcom/mojang/authlib/GameProfileRepository;findProfileByName(Ljava/lang/String;)Ljava/util/Optional;"))
    private Optional<NameAndId> skywars2$preparedName(GameProfileRepository repository, String name,
            Operation<Optional<NameAndId>> original) {
        var lookup = BulkPlayerProfiles.CURRENT.isBound() ? BulkPlayerProfiles.CURRENT.get() : null;
        return lookup != null && lookup.matches(repository, name) ? lookup.primary() : original.call(repository, name);
    }

    @Mixin(targets = "net.minecraft.server.players.CachedUserNameToIdResolver$GameProfileInfo")
    public interface Entry {
        @Accessor("expirationDate") Date skywars2$expiration();
        @Invoker("nameAndId") net.minecraft.server.players.NameAndId skywars2$identity();
    }

    @Mixin(OldUsersConverter.class)
    public static class OnlineFallback {
        @WrapOperation(method = "lookupPlayers", at = @At(value = "INVOKE",
                target = "Lcom/mojang/authlib/GameProfileRepository;findProfilesByNames([Ljava/lang/String;Lcom/mojang/authlib/ProfileLookupCallback;)V"))
        private static void skywars2$preparedFallback(GameProfileRepository repository, String[] names,
                ProfileLookupCallback callback, Operation<Void> original) {
            var lookup = BulkPlayerProfiles.CURRENT.isBound() ? BulkPlayerProfiles.CURRENT.get() : null;
            if (lookup != null && names.length == 1 && lookup.matches(repository, names[0])) {
                lookup.complete(names[0], callback);
            } else {
                original.call(repository, names, callback);
            }
        }
    }
}
