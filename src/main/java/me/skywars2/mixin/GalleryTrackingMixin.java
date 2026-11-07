package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.skywars2.game.LbcFpvDrone;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Native pairing/removal retains only nearby showroom frames on each client. */
@Mixin(targets="net.minecraft.server.level.ChunkMap$TrackedEntity")
public abstract class GalleryTrackingMixin {
    @WrapOperation(method="updatePlayer",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;broadcastToPlayer(Lnet/minecraft/server/level/ServerPlayer;)Z"))
    private boolean skywars2$galleryAudience(Entity target,ServerPlayer viewer,Operation<Boolean> original){
        return (!(target instanceof ItemFrame)||!target.entityTags().contains("skywars2.gallery")
                ||target.position().distanceToSqr(LbcFpvDrone.trackingPosition(viewer))<=400)&&original.call(target,viewer);
    }
}
