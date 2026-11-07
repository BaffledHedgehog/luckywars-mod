package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.skywars2.game.TpsCompensation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class TpsMiningMixin implements TpsCompensation.Tracked {
    @Shadow @Final protected ServerPlayer player;
    @Shadow private int gameTicks;
    @Shadow private int destroyProgressStart;
    @Shadow private int delayedTickStart;
    @Shadow private boolean hasDelayedDestroy;
    @Shadow private boolean isDestroyingBlock;
    @Shadow private BlockPos destroyPos;
    @Shadow private int lastSentState;
    @Unique private final TpsCompensation.Clock skywars2$miningClock = new TpsCompensation.Clock();
    @Unique private final TpsCompensation.Clock skywars2$delayedClock = new TpsCompensation.Clock();
    @Unique private BlockPos skywars2$miningPos;
    @Unique private boolean skywars2$miningActive;

    // This field is assigned only after vanilla's reach, protection and game-mode checks.
    @Inject(method="handleBlockBreakAction", at=@At(value="FIELD", target=
            "Lnet/minecraft/server/level/ServerPlayerGameMode;destroyProgressStart:I", opcode=181, shift=At.Shift.AFTER))
    private void skywars2$startMining(BlockPos pos, ServerboundPlayerActionPacket.Action action,
            Direction direction, int maxY, int sequence, CallbackInfo ci) {
        skywars2$miningClock.reset(0);
        skywars2$miningClock.elapsed(player, 0);
        skywars2$miningPos = pos.immutable();
        skywars2$miningActive = true;
    }

    @Inject(method="handleBlockBreakAction", at=@At(value="FIELD", target=
            "Lnet/minecraft/server/level/ServerPlayerGameMode;delayedTickStart:I", opcode=181, shift=At.Shift.AFTER))
    private void skywars2$delayMining(BlockPos pos, ServerboundPlayerActionPacket.Action action,
            Direction direction, int maxY, int sequence, CallbackInfo ci) {
        if (skywars2$miningActive && pos.equals(skywars2$miningPos))
            skywars2$delayedClock.copyFrom(skywars2$miningClock);
        else {
            int ticks = gameTicks - delayedTickStart;
            skywars2$delayedClock.reset(ticks);
            skywars2$delayedClock.elapsed(player, ticks);
        }
    }

    @Inject(method="handleBlockBreakAction", at=@At("RETURN"))
    private void skywars2$endMining(BlockPos pos, ServerboundPlayerActionPacket.Action action,
            Direction direction, int maxY, int sequence, CallbackInfo ci) {
        if (action == ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK
                || action == ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK
                || (action == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK
                    && (!isDestroyingBlock || !destroyPos.equals(skywars2$miningPos)))) skywars2$miningActive = false;
    }

    @WrapOperation(method="handleBlockBreakAction", at=@At(value="INVOKE", target=
            "Lnet/minecraft/world/level/block/state/BlockState;getDestroyProgress(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)F"))
    private float skywars2$stopMining(BlockState state, Player owner, BlockGetter level, BlockPos pos,
            Operation<Float> original, BlockPos requested, ServerboundPlayerActionPacket.Action action,
            Direction direction, int maxY, int sequence) {
        float rate = original.call(state, owner, level, pos);
        if (action != ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK || !(rate > 0)
                || !skywars2$miningActive || !isDestroyingBlock || !pos.equals(skywars2$miningPos)) return rate;
        int ticks = gameTicks - destroyProgressStart;
        return (float)(rate * (skywars2$miningClock.elapsed(player, ticks) + 1) / (ticks + 1d));
    }

    @Inject(method="incrementDestroyProgress", at=@At("HEAD"), cancellable=true)
    private void skywars2$miningProgress(BlockState state, BlockPos pos, int ignored, CallbackInfoReturnable<Float> cir) {
        // Polymer completes server-controlled mining in its RETURN injection. Cancelling at
        // HEAD skips that completion while its client-side fatigue prevents a native STOP.
        if(eu.pb4.polymer.core.api.block.PolymerBlockUtils.shouldMineServerSide(player,pos,state))return;
        // Native delayedTickStart is an absolute start tick, not an elapsed duration.
        int ticks = gameTicks - (hasDelayedDestroy ? delayedTickStart : destroyProgressStart);
        float rate = state.getDestroyProgress(player, player.level(), pos);
        var clock = hasDelayedDestroy ? skywars2$delayedClock : skywars2$miningClock;
        float progress = (float)(rate * (clock.elapsed(player, ticks) + 1));
        int stage = (int)(progress * 10);
        if (stage != lastSentState) {
            player.level().destroyBlockProgress(player.getId(), pos, stage);
            lastSentState = stage;
        }
        cir.setReturnValue(progress);
    }

    @Override public void skywars2$compensationTick() {
        skywars2$miningClock.elapsed(player, gameTicks - destroyProgressStart);
        skywars2$delayedClock.elapsed(player, gameTicks - delayedTickStart);
    }

    @Inject(method="setLevel", at=@At("HEAD"))
    private void skywars2$changeLevel(CallbackInfo ci) {
        skywars2$miningClock.reset(0);
        skywars2$delayedClock.reset(0);
        skywars2$miningActive = false;
    }
}
