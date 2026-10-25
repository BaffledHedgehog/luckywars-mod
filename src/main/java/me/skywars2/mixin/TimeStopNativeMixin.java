package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.WrapWithCondition;
import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.TickRateManager;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.minecraft.world.entity.raid.Raids;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Scoped simulation gates. Global clocks and vanilla tick-rate state remain authoritative. */
@Mixin(ServerLevel.class)
public abstract class TimeStopNativeMixin {
    @org.spongepowered.asm.mixin.Unique
    private boolean skywars2$worldStopped() {
        var game = SkyWars2.game();
        return game != null && game.items().timeStop().stopped((ServerLevel)(Object)this);
    }

    @Redirect(method = "lambda$tick$0", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/TickRateManager;isEntityFrozen(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean skywars2$skipFrozenDespawn(TickRateManager manager, Entity entity) {
        boolean original = manager.isEntityFrozen(entity);
        var game = SkyWars2.game();
        if (game == null || !game.items().timeStop().frozen(entity)) return original;
        game.items().timeStop().prepareFrozen(entity);
        return true;
    }

    @Inject(method = "tickNonPassenger", at = @At("HEAD"), cancellable = true)
    private void skywars2$freezeEntity(Entity entity, CallbackInfo ci) {
        var game = SkyWars2.game();
        if (game == null || !game.items().timeStop().frozen(entity)) return;
        game.items().timeStop().prepareFrozen(entity);
        ci.cancel();
    }

    @Inject(method = "tickPassenger", at = @At("HEAD"), cancellable = true)
    private void skywars2$freezePassenger(Entity vehicle, Entity passenger, CallbackInfo ci) {
        var game = SkyWars2.game();
        if (game == null || !game.items().timeStop().frozen(passenger)) return;
        game.items().timeStop().prepareFrozen(passenger);
        ci.cancel();
    }

    @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/server/level/ServerLevel;advanceWeatherCycle()V"))
    private boolean skywars2$pauseWeather(ServerLevel level) { return !skywars2$worldStopped(); }

    @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/server/level/ServerLevel;runBlockEvents()V"))
    private boolean skywars2$pauseBlockEvents(ServerLevel level) { return !skywars2$worldStopped(); }

    @WrapWithCondition(method = "tick", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/entity/raid/Raids;tick(Lnet/minecraft/server/level/ServerLevel;)V"))
    private boolean skywars2$pauseRaids(Raids raids, ServerLevel level) { return !skywars2$worldStopped(); }

    @Mixin(ServerPlayer.class)
    public abstract static class PlayerTick {
        @Inject(method = "doTick", at = @At("HEAD"), cancellable = true)
        private void skywars2$freezePlayerSimulation(CallbackInfo ci) {
            var game = SkyWars2.game();
            var player = (ServerPlayer)(Object)this;
            if (game == null || !game.items().timeStop().frozen(player)) return;
            game.items().timeStop().handleFrozenPlayer(player);
            ci.cancel();
        }
    }

    @Mixin(Level.class)
    public abstract static class BlockEntities {
        @ModifyExpressionValue(method = "tickBlockEntities", at = @At(value = "INVOKE", target =
                "Lnet/minecraft/world/TickRateManager;runsNormally()Z"))
        private boolean skywars2$pauseBlockEntities(boolean original) {
            var game = SkyWars2.game();
            return original && (!((Object)this instanceof ServerLevel level)
                    || game == null || !game.items().timeStop().stopped(level));
        }
    }

    @Mixin(ServerChunkCache.class)
    public abstract static class ChunkSimulation {
        @Shadow @Final private ServerLevel level;

        @Inject(method = "tickChunks(Lnet/minecraft/util/profiling/ProfilerFiller;)V",
                at = @At("HEAD"), cancellable = true)
        private void skywars2$pauseChunkSimulation(ProfilerFiller profiler, CallbackInfo ci) {
            var game = SkyWars2.game();
            if (game != null && game.items().timeStop().stopped(level)) ci.cancel();
        }
    }

    @Mixin(net.minecraft.world.entity.item.PrimedTnt.class)
    public interface TntData {
        @org.spongepowered.asm.mixin.gen.Accessor("DATA_FUSE_ID")
        static net.minecraft.network.syncher.EntityDataAccessor<Integer> skywars2$fuseData() {
            throw new AssertionError("mixin accessor");
        }
    }

    /** Pairing must also use the presentation fuse, including a late viewer of fuse-one TNT. */
    @Mixin(net.minecraft.server.level.ServerEntity.class)
    public abstract static class TntTracking {
        @Shadow @Final private Entity entity;
        @org.spongepowered.asm.mixin.injection.ModifyArg(method={"sendPairingData","sendDirtyEntityData"},
                at=@At(value="INVOKE",target="Lnet/minecraft/network/protocol/game/ClientboundSetEntityDataPacket;<init>(ILjava/util/List;)V"),index=1)
        private java.util.List<net.minecraft.network.syncher.SynchedEntityData.DataValue<?>> skywars2$tntPresentation(
                java.util.List<net.minecraft.network.syncher.SynchedEntityData.DataValue<?>> values) {
            var game=SkyWars2.game();
            return game==null?values:game.items().timeStop().tntPresentation(entity,values);
        }
    }


    @Mixin(Entity.class)
    public abstract static class Persistence {
        @ModifyExpressionValue(method="saveWithoutId",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;getDeltaMovement()Lnet/minecraft/world/phys/Vec3;"))
        private net.minecraft.world.phys.Vec3 skywars2$saveMotion(net.minecraft.world.phys.Vec3 original) {
            var game=SkyWars2.game();
            return game==null?original:game.items().timeStop().persistedVelocity((Entity)(Object)this,original);
        }
        @ModifyExpressionValue(method="saveWithoutId",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;isNoGravity()Z"))
        private boolean skywars2$saveGravity(boolean original) {
            var game=SkyWars2.game();
            return game==null?original:game.items().timeStop().persistedNoGravity((Entity)(Object)this,original);
        }
    }

    @Mixin(net.minecraft.world.level.chunk.storage.SerializableChunkData.class)
    public abstract static class ChunkPersistence {
        @Inject(method="copyOf",at=@At("RETURN"),cancellable=true)
        private static void skywars2$saveContacts(ServerLevel level,net.minecraft.world.level.chunk.ChunkAccess chunk,
                org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<net.minecraft.world.level.chunk.storage.SerializableChunkData> cir) {
            var game=SkyWars2.game();
            if(game!=null) cir.setReturnValue(game.items().timeStop().persistedChunk(level,chunk,cir.getReturnValue()));
        }
    }

}
