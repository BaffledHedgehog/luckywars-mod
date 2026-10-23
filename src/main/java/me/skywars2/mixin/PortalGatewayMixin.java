package me.skywars2.mixin;

import me.skywars2.SkyWars2;
import me.skywars2.game.LbcPortalDrops;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.EndGatewayBlock;
import net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Persistent provenance for drop-created gateways; vanilla gateways keep vanilla behavior. */
public final class PortalGatewayMixin {
    private PortalGatewayMixin() { }

    @Mixin(TheEndGatewayBlockEntity.class)
    public abstract static class State implements LbcPortalDrops.StateAccess {
        @Unique private CompoundTag skywars2$portalData = new CompoundTag();

        @Override
        public CompoundTag skywars2$portalMetadata() { return skywars2$portalData; }

        @Inject(method = "loadAdditional", at = @At("TAIL"))
        private void skywars2$loadPortal(ValueInput input, CallbackInfo ci) {
            skywars2$portalData = input.read(LbcPortalDrops.DATA, CompoundTag.CODEC).orElseGet(CompoundTag::new);
        }

        @Inject(method = "saveAdditional", at = @At("TAIL"))
        private void skywars2$savePortal(ValueOutput output, CallbackInfo ci) {
            if (skywars2$portalData.contains("epoch"))
                output.store(LbcPortalDrops.DATA, CompoundTag.CODEC, skywars2$portalData);
        }
    }

    @Mixin(EndGatewayBlock.class)
    public abstract static class Travel {
        @Inject(method = "getPortalDestination", at = @At("HEAD"), cancellable = true)
        private void skywars2$guardPortal(ServerLevel level, Entity entity, BlockPos entry,
                                          CallbackInfoReturnable<TeleportTransition> cir) {
            var game = SkyWars2.game();
            if (game != null && !game.items().portalDrops().canUseGateway(level, entity, entry))
                cir.setReturnValue(null);
        }

        @Inject(method = "getPortalDestination", at = @At("RETURN"), cancellable = true)
        private void skywars2$portalArrival(ServerLevel level, Entity entity, BlockPos entry,
                                            CallbackInfoReturnable<TeleportTransition> cir) {
            var transition = cir.getReturnValue();
            var game = SkyWars2.game();
            if (transition == null || game == null
                    || !(level.getBlockEntity(entry) instanceof TheEndGatewayBlockEntity gateway)) return;
            var data = ((LbcPortalDrops.StateAccess) gateway).skywars2$portalMetadata();
            if (!data.contains("epoch")) return;
            var provenance = data.copy();
            cir.setReturnValue(new TeleportTransition(transition.newLevel(), transition.position(),
                    transition.deltaMovement(), transition.yRot(), transition.xRot(), transition.missingRespawnBlock(),
                    transition.asPassenger(), transition.relatives(), transition.postTeleportTransition().then(
                            arrived -> game.items().portalDrops().arrived(arrived, provenance))));
        }
    }
}
