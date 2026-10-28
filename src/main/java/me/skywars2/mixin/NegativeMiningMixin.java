package me.skywars2.mixin;

import me.skywars2.SkyWars2;
import me.skywars2.game.NegativeMining;
import me.skywars2.game.NegativeMiningClient;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class NegativeMiningMixin implements NegativeMiningClient.State {
    @Shadow @Final protected ServerPlayer player;
    @Shadow private boolean isDestroyingBlock;
    @Shadow private boolean hasDelayedDestroy;
    @Shadow private BlockPos destroyPos;
    @Shadow private BlockPos delayedDestroyPos;
    @Unique private NegativeMining.Session skywars2$negativeMining;
    @Unique private volatile boolean skywars2$predictionBlocked;
    @Override public boolean skywars2$predictionBlocked() {return skywars2$predictionBlocked;}
    @Override public void skywars2$predictionBlocked(boolean blocked) {skywars2$predictionBlocked=blocked;}

    @Inject(method="handleBlockBreakAction",at=@At("HEAD"),cancellable=true)
    private void skywars2$negativeMiningPacket(BlockPos pos,ServerboundPlayerActionPacket.Action action,
            Direction direction,int maxY,int sequence,CallbackInfo ci) {
        if(action==ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK
                && player.level().getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)==null
                && NegativeMining.mayHaveNegativeRate(player)) {
            skywars2$negativeMining=null;isDestroyingBlock=hasDelayedDestroy=false;ci.cancel();return;
        }
        // A STOP can arrive after a sign change without ever creating a custom session.
        // Clear native delayed work immediately, rather than relying on a later tick to notice.
        if(action==ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK && NegativeMining.negativeRate(player,pos)) {
            player.level().destroyBlockProgress(player.getId(),pos,-1);
            skywars2$negativeMining=null;isDestroyingBlock=hasDelayedDestroy=false;ci.cancel();return;
        }
        if(skywars2$negativeMining==null)return;
        if(action==ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK
                || action==ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK) {
            player.level().destroyBlockProgress(player.getId(),skywars2$negativeMining.source(),-1);
            skywars2$negativeMining=null;isDestroyingBlock=hasDelayedDestroy=false;
            ci.cancel(); // STOP must not turn a negative rate into vanilla's never-ending delayed destroy.
        } else if(action==ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK) {
            skywars2$negativeMining=null;isDestroyingBlock=hasDelayedDestroy=false;
        }
    }
    @Inject(method="handleBlockBreakAction",at=@At("RETURN"))
    private void skywars2$negativeMiningStarted(BlockPos pos,ServerboundPlayerActionPacket.Action action,
            Direction direction,int maxY,int sequence,CallbackInfo ci) {
        if(action!=ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK || !isDestroyingBlock || !destroyPos.equals(pos))return;
        var session=NegativeMining.begin(SkyWars2.game(),player,pos);
        if(session==null)return;
        skywars2$negativeMining=session;isDestroyingBlock=hasDelayedDestroy=false;
        player.level().destroyBlockProgress(player.getId(),pos,-1);
    }
    // Exact native order: reach, height, spawn protection and mayInteract have already passed here.
    // Creative otherwise bypasses getDestroyProgress entirely; only a negative effective rate diverts it.
    @Inject(method="handleBlockBreakAction",at=@At(value="INVOKE",target=
            "Lnet/minecraft/server/level/ServerPlayerGameMode;destroyAndAck(Lnet/minecraft/core/BlockPos;ILjava/lang/String;)V",ordinal=0),cancellable=true)
    private void skywars2$negativeCreative(BlockPos pos,ServerboundPlayerActionPacket.Action action,
            Direction direction,int maxY,int sequence,CallbackInfo ci) {
        var session=NegativeMining.begin(SkyWars2.game(),player,pos);
        if(session==null)return;
        skywars2$negativeMining=session;isDestroyingBlock=hasDelayedDestroy=false;destroyPos=pos.immutable();
        player.level().destroyBlockProgress(player.getId(),pos,-1);ci.cancel();
    }
    @Inject(method="tick",at=@At("HEAD"))
    private void skywars2$negativeMiningTick(CallbackInfo ci) {
        NegativeMiningClient.tick(player);
        if(hasDelayedDestroy && NegativeMining.negativeRate(player,delayedDestroyPos)) {
            hasDelayedDestroy=false;
            player.level().destroyBlockProgress(player.getId(),delayedDestroyPos,-1);
        }
        if(isDestroyingBlock && NegativeMining.negativeRate(player,destroyPos)) {
            skywars2$negativeMining=NegativeMining.begin(SkyWars2.game(),player,destroyPos);
            isDestroyingBlock=hasDelayedDestroy=false;
            player.level().destroyBlockProgress(player.getId(),destroyPos,-1);
        }
        if(skywars2$negativeMining!=null && !NegativeMining.tick(SkyWars2.game(),player,skywars2$negativeMining))
            skywars2$negativeMining=null;
    }
    // Every native completion (creative, instant, STOP, delayed and direct callers) converges here.
    @Inject(method="destroyBlock",at=@At("HEAD"),cancellable=true)
    private void skywars2$neverDestroyNegative(BlockPos pos,CallbackInfoReturnable<Boolean> cir) {
        if(!NegativeMining.negativeRate(player,pos))return;
        isDestroyingBlock=hasDelayedDestroy=false;
        player.level().destroyBlockProgress(player.getId(),pos,-1);
        cir.setReturnValue(false);
    }
    @Inject(method="setLevel",at=@At("HEAD"))
    private void skywars2$negativeMiningChangedLevel(CallbackInfo ci) {
        skywars2$negativeMining=null;
    }
}
