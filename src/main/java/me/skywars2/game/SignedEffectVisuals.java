package me.skywars2.game;

import eu.pb4.polymer.common.api.PolymerCommonUtils;
import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
import me.skywars2.mixin.SignedEffectVisualMixin;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.core.particles.ParticleOptions;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Native-client presentation only. The authoritative signed effect remains untouched. */
public final class SignedEffectVisuals {
    private static final String PREFIX = "signed_effects/";
    private static final Identifier NEXUS = Identifier.parse("lbc:nexus");
    private record Presentation(long invisibility, boolean spectator, List<ParticleOptions> particles) { }
    private static final ConcurrentHashMap<Integer, Presentation> PRESENTATIONS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Long> REVEAL = new ConcurrentHashMap<>();
    // Entity references are accessed only from the server thread, never by the packet encoder.
    private static final Map<Integer, LivingEntity> ENTITIES = new HashMap<>();
    private SignedEffectVisuals() { }

    public static long level(LivingEntity entity, Holder<MobEffect> type) {
        var effect = entity.getEffect(type);
        return effect == null ? 0 : (long) effect.getAmplifier() + 1;
    }
    /** Negative Glowing supplies the matching invisibility tier without replacing real effect layers. */
    public static long invisibilityLevel(LivingEntity entity) {
        long invisibility=level(entity,MobEffects.INVISIBILITY),glowing=level(entity,MobEffects.GLOWING);
        return glowing<0?Math.max(invisibility,-glowing):invisibility;
    }
    public static long effectiveInvisibility(long invisibility, ServerPlayer viewer) {
        return invisibility <= 0 ? 0 : Math.max(0, invisibility -
                (viewer == null ? 0 : REVEAL.getOrDefault(viewer.getUUID(), 0L)));
    }
    public static boolean binary(Holder<MobEffect> effect) {
        return effect.is(MobEffects.INVISIBILITY) || effect.is(MobEffects.GLOWING)
                || effect.is(MobEffects.BLINDNESS) || effect.is(MobEffects.DARKNESS)
                || effect.is(MobEffects.NIGHT_VISION) || effect.is(MobEffects.NAUSEA);
    }
    /** Prevent a vanilla client from clamping negative levels into active level I. */
    public static int clientDuration(Holder<MobEffect> effect, int amplifier, int duration) {
        return (binary(effect) || effect.is(MobEffects.JUMP_BOOST) || effect.is(MobEffects.SLOW_FALLING) || effect.is(MobEffects.DOLPHINS_GRACE) || effect.is(MobEffects.CONDUIT_POWER)) && (long) amplifier + 1 <= 0 ? 0 : duration;
    }
    public static void effectsChanged(LivingEntity entity) {
        if (entity.level().isClientSide()) return;
        long invisibility = invisibilityLevel(entity);
        var particles = entity.getActiveEffects().stream().filter(MobEffectInstance::isVisible)
                .map(MobEffectInstance::getParticleOptions).toList();
        if (invisibility != 0) {
            PRESENTATIONS.put(entity.getId(), new Presentation(invisibility, entity instanceof ServerPlayer p && p.isSpectator(), particles));
            ENTITIES.put(entity.getId(), entity);
        } else {
            PRESENTATIONS.remove(entity.getId()); ENTITIES.remove(entity.getId());
        }
        FullInvisibility.effectsChanged(entity);
        SignedGlowVisuals.changed(entity);
        if (entity instanceof ServerPlayer player) {
            long reveal = Math.max(0, -level(player, MobEffects.BLINDNESS));
            long old = REVEAL.getOrDefault(player.getUUID(), 0L);
            if (reveal == 0) REVEAL.remove(player.getUUID()); else REVEAL.put(player.getUUID(), reveal);
            if (old != reveal) {
                FullInvisibility.viewerChanged(player);
                for (var target : List.copyOf(ENTITIES.values()))
                    if (target != player && PlayerLookup.tracking(target).contains(player)) refreshMetadata(target, player);
            }
            synchronize(player);
        }
        // Effect callbacks precede the native dirty-effect visibility update. The dedicated
        // updateInvisibilityStatus tail hook refreshes metadata after native flags are ready.
    }
    public static void tick(MinecraftServer server) {
        for (var player : server.getPlayerList().getPlayers()) synchronize(player);
        SignedGlowVisuals.tick(server);
    }
    public static void joined(ServerPlayer player) {
        clear(player);
        effectsChanged(player);
        player.sendPostEffects();
    }
    public static void clear(ServerPlayer player) {
        SignedGlowVisuals.forget(player);
        for (var id : List.copyOf(player.getPostEffects())) if (owned(id)) player.removePostEffect(id);
        REVEAL.remove(player.getUUID());
    }
    public static void stop(MinecraftServer server) {
        for (var player : server.getPlayerList().getPlayers()) clear(player);
        PRESENTATIONS.clear(); ENTITIES.clear(); REVEAL.clear();
        SignedGlowVisuals.stop();
    }
    public static void removed(Entity entity) {
        PRESENTATIONS.remove(entity.getId()); ENTITIES.remove(entity.getId());
        if (entity instanceof LivingEntity living) SignedGlowVisuals.removed(living);
        if (entity instanceof ServerPlayer player) REVEAL.remove(player.getUUID());
    }
    public static boolean owned(Identifier id) {
        return id.equals(NEXUS) || id.getNamespace().equals("lbc") && id.getPath().startsWith(PREFIX);
    }
    /** Visual response saturates at display limits; signed values never saturate server-side. */
    public static int visualTier(long level) {
        long magnitude = Math.abs(level);
        return (int) Math.min(16, magnitude);
    }
    public static List<Identifier> desired(LivingEntity entity) {
        return desired(entity, entity.level().dimension().identifier().toString());
    }
    static List<Identifier> desired(LivingEntity entity, String dimension) {
        var result = new ArrayList<Identifier>();
        if (dimension.equals("minecraft:nexus")) result.add(NEXUS);
        long blindness = level(entity, MobEffects.BLINDNESS);
        long darkness = level(entity, MobEffects.DARKNESS);
        long night = level(entity, MobEffects.NIGHT_VISION);
        long nausea = level(entity, MobEffects.NAUSEA);
        if (blindness > 1) add(result, "black", Math.min(3, blindness));
        if (blindness < 0) add(result, "fried", visualTier(blindness));
        if (darkness > 1) add(result, "black", Math.min(3, darkness));
        if (darkness < 0) add(result, "bright", visualTier(darkness));
        if (night < 0) add(result, "invert", visualTier(night));
        if (night > 1) add(result, "contrast", visualTier(night));
        if (nausea > 1) add(result, "nausea", visualTier(nausea));
        return List.copyOf(result);
    }
    private static void add(List<Identifier> result, String kind, long tier) {
        var id = Identifier.parse("lbc:" + PREFIX + kind + "_" + tier);
        if (!result.contains(id)) result.add(id);
    }
    /** Reconcile the current body/world after dimension, respawn or rollback publication. */
    public static void synchronize(ServerPlayer player) {
        var wanted = desired(player);
        for (var id : List.copyOf(player.getPostEffects()))
            if (owned(id) && !wanted.contains(id)) player.removePostEffect(id);
        for (var id : wanted) player.addPostEffect(id);
    }
    public static void visibilityUpdated(LivingEntity entity) {
        if (entity.level().isClientSide()) return;
        var snapshot=PRESENTATIONS.get(entity.getId());
        if(snapshot!=null) {
            boolean spectator=entity instanceof ServerPlayer p && p.isSpectator();
            if(snapshot.spectator!=spectator) PRESENTATIONS.put(entity.getId(),
                    new Presentation(snapshot.invisibility,spectator,snapshot.particles));
            for (var viewer : PlayerLookup.tracking(entity)) refreshMetadata(entity, viewer);
        }
    }
    private static void refreshMetadata(LivingEntity entity, ServerPlayer viewer) {
        var fields = new ArrayList<SynchedEntityData.DataValue<?>>();
        var flags = SignedEffectVisualMixin.Flags.flags();
        fields.add(SynchedEntityData.DataValue.create(flags, entity.getEntityData().get(flags)));
        var particles = SignedEffectVisualMixin.Particles.particles();
        fields.add(SynchedEntityData.DataValue.create(particles, entity.getEntityData().get(particles)));
        viewer.connection.send(PolymerEntityUtils.setEntityContext(
                new ClientboundSetEntityDataPacket(entity.getId(), fields), entity));
    }
    /** Runs on the packet encoder: only immutable snapshots and concurrent maps are consulted. */
    public static List<SynchedEntityData.DataValue<?>> project(int id,
            List<SynchedEntityData.DataValue<?>> fields, PacketContext context) {
        var snapshot = PRESENTATIONS.get(id);
        if (snapshot == null) return fields;
        var viewer = PolymerCommonUtils.getPlayer(context);
        if (FullInvisibility.proxyFor(id, viewer)) return fields;
        long effective = effectiveInvisibility(snapshot.invisibility, viewer);
        var result = new ArrayList<SynchedEntityData.DataValue<?>>(fields.size());
        int flagsId = SignedEffectVisualMixin.Flags.flags().id();
        int particlesId = SignedEffectVisualMixin.Particles.particles().id();
        for (var field : fields) {
            if (field.id() == flagsId && field.value() instanceof Byte flags) {
                // Only change a positive effect's invisible bit; preserve spectator/command flags.
                if (snapshot.invisibility > 0 && !snapshot.spectator) flags = effective > 0 ? (byte)(flags | 32) : (byte)(flags & ~32);
                result.add(SynchedEntityData.DataValue.create(SignedEffectVisualMixin.Flags.flags(), flags));
            } else if (field.id() == particlesId) {
                result.add(SynchedEntityData.DataValue.create(SignedEffectVisualMixin.Particles.particles(),
                        effective >= 2 ? List.of() : snapshot.particles));
            } else result.add(field);
        }
        return result;
    }
}
