package me.skywars2.game;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import me.skywars2.SkyWars2;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.clock.ClockNetworkState;
import net.minecraft.world.clock.WorldClock;

/** Recipient-only atlas protocol. The event leases the effect without consuming manual previews. */
public final class AtlasGlitchSignal {
    public static final Identifier EFFECT=Identifier.parse("lbc:atlas_glitch");
    private static final ResourceKey<WorldClock> CLOCK=ResourceKey.create(Registries.WORLD_CLOCK,EFFECT);
    private static final String AUTO="skywars2.atlas_glitch.supernova", MANUAL="skywars2.atlas_glitch.manual";
    private static final Set<ServerGamePacketListenerImpl> ACTIVE=Collections.newSetFromMap(new IdentityHashMap<>());
    private AtlasGlitchSignal(){}

    private static boolean automatic(MinecraftServer server){
        var game=SkyWars2.game();
        return game!=null&&game.server()==server&&game.items().supernova().phase()!=LbcSupernova.Phase.IDLE;
    }
    public static boolean manualEnabled(ServerPlayer player){
        return player.entityTags().contains(AUTO)?player.entityTags().contains(MANUAL):player.getPostEffects().contains(EFFECT);
    }
    public static void manual(ServerPlayer player,boolean enabled){
        if(player.entityTags().contains(AUTO)){
            if(enabled)player.entityTags().add(MANUAL);else player.entityTags().remove(MANUAL);
        }else effect(player,enabled);
        synchronize(player,automatic(player.level().getServer()));
        publish(player);
    }
    private static void effect(ServerPlayer player,boolean enabled){
        boolean changed=enabled?player.addPostEffect(EFFECT):player.removePostEffect(EFFECT);
        if(changed&&player.connection!=null)player.sendPostEffects();
    }
    /** Tags and the native effect list participate in the existing player save/rewind image. */
    static void synchronize(ServerPlayer player,boolean automatic){
        var tags=player.entityTags();
        if(automatic){
            if(tags.add(AUTO)){
                if(player.getPostEffects().contains(EFFECT))tags.add(MANUAL);else tags.remove(MANUAL);
            }
            effect(player,true);
        }else if(tags.remove(AUTO)){
            effect(player,tags.remove(MANUAL));
        }else tags.remove(MANUAL);
    }
    /** Nearest tenth of a degree: 3600 yaw bins x 1801 pitch bins, no collisions. */
    public static int cameraSeed(float yawDegrees, float pitchDegrees) {
        double yaw = Float.isFinite(yawDegrees) ? yawDegrees % 360.0 : 0.0;
        if (yaw < 0.0) yaw += 360.0;
        int x = (int)Math.round(yaw * 10.0) % 3600;
        double pitch = Float.isFinite(pitchDegrees) ? Math.clamp((double)pitchDegrees, -90.0, 90.0) : 0.0;
        int y = (int)Math.round((pitch + 90.0) * 10.0);
        return x + 3600 * y;
    }

    static long code(ServerPlayer player){
        var camera=player.getCamera();
        return 1_000_000_000L+cameraSeed(camera.getYRot(),camera.getXRot());
    }
    static ClientboundSetTimePacket packet(ServerPlayer player,boolean enabled){
        var clock=player.registryAccess().lookupOrThrow(Registries.WORLD_CLOCK).getOrThrow(CLOCK);
        return new ClientboundSetTimePacket(player.level().getGameTime(),
                Map.of(clock,new ClockNetworkState(enabled?code(player):0L,0F,0F)));
    }
    private static void publish(ServerPlayer player){
        if(player.connection==null)return;
        boolean enabled=player.getPostEffects().contains(EFFECT);
        if(enabled||ACTIVE.contains(player.connection))player.connection.send(packet(player,enabled));
        if(enabled)ACTIVE.add(player.connection);else ACTIVE.remove(player.connection);
    }
    public static void tick(MinecraftServer server){
        boolean automatic=automatic(server);
        var online=Collections.newSetFromMap(new IdentityHashMap<ServerGamePacketListenerImpl,Boolean>());
        for(var player:server.getPlayerList().getPlayers()){
            online.add(player.connection);synchronize(player,automatic);publish(player);
        }
        ACTIVE.retainAll(online);
    }
    public static void leaving(ServerPlayer player){
        synchronize(player,false);ACTIVE.remove(player.connection);
    }
    public static void stop(MinecraftServer server){
        for(var player:server.getPlayerList().getPlayers()){
            synchronize(player,false);
            if(player.connection!=null&&ACTIVE.contains(player.connection))player.connection.send(packet(player,false));
        }
        ACTIVE.clear();
    }
}
