package me.skywars2.validation.mixin;

import java.util.IdentityHashMap;
import me.skywars2.game.LbcExoperso;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

public final class GameCheckAccess {
    private GameCheckAccess() { }

    @Mixin(LivingEntity.class)
    public interface CombatLinks {
        @Accessor("lastHurtByMobTimestamp") int validation$mobTime();
        @Accessor("lastHurtMob") LivingEntity validation$attacked();
        @Accessor("lastHurtMobTimestamp") int validation$attackTime();
    }

    @Mixin(ServerGamePacketListenerImpl.class)
    public interface TeleportAck {
        @Accessor("awaitingTeleport") int validation$teleportId();
        @Accessor("awaitingPositionFromClient") Vec3 validation$awaitingPosition();
    }

    @Mixin(value = LbcExoperso.class, remap = false)
    public interface Exoperso {
        @Accessor("blastSources") IdentityHashMap<Entity, ?> validation$blastSources();
    }
}
