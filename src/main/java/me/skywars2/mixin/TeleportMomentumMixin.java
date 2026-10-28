package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.skywars2.game.TeleportMomentum;
import net.minecraft.server.commands.TeleportCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Set;

/** Position and camera edits do not implicitly stop or turn existing momentum. */
public final class TeleportMomentumMixin {
    private TeleportMomentumMixin() { }

    @Mixin(ServerGamePacketListenerImpl.class)
    public abstract static class Connection {
        @Shadow public net.minecraft.server.level.ServerPlayer player;
        @WrapMethod(method="teleport(Lnet/minecraft/world/entity/PositionMoveRotation;Ljava/util/Set;)V")
        private void skywars2$positionKeepsMomentum(PositionMoveRotation destination,Set<Relative> relatives,Operation<Void> original) {
            // A nonzero payload is an explicit motion transition, e.g. a rotated portal exit.
            if(!destination.deltaMovement().equals(Vec3.ZERO)){original.call(destination,relatives);return;}
            Vec3 velocity=player.getDeltaMovement();
            try {original.call(destination,TeleportMomentum.positionFlags(relatives));}
            // Native teleportSetPosition reads lastKnownClientMovement, not server delta.
            // Keep the server value independently; the zero relative packet keeps client-local motion.
            finally {player.setDeltaMovement(velocity);}
        }
    }

    @Mixin(Entity.class)
    public abstract static class Pose {
        @WrapMethod(method="teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDLjava/util/Set;FFZ)Z")
        private boolean skywars2$coordinateEdit(ServerLevel level,double x,double y,double z,Set<Relative> relatives,
                                               float yaw,float pitch,boolean resetCamera,Operation<Boolean> original) {
            Entity entity=(Entity)(Object)this;Vec3 velocity=entity.getDeltaMovement();
            try {return original.call(level,x,y,z,TeleportMomentum.positionFlags(relatives),yaw,pitch,resetCamera);}
            finally {entity.setDeltaMovement(velocity);}
        }
    }

    @Mixin(TeleportCommand.class)
    public abstract static class Command {
        @WrapOperation(method="performTeleport",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"))
        private static void skywars2$keepVerticalMomentum(Entity entity,Vec3 motion,Operation<Void> original) {
            // /tp has a second vanilla Y-velocity reset after the actual teleport.
        }
        @WrapOperation(method="performTeleport",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;setOnGround(Z)V"))
        private static void skywars2$keepAirborneState(Entity entity,boolean grounded,Operation<Void> original) {
            // Moving coordinates alone must not land an airborne entity.
        }
    }
}
