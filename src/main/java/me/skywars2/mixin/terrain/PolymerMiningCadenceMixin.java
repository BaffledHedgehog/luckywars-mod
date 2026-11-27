package me.skywars2.mixin.terrain;

import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.server.level.ServerPlayer;
import me.skywars2.game.net.metrics.TpsCompensation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Polymer's inter-block cooldown must elapse while the player is idle too. */
@Mixin(value=ServerPlayerGameMode.class,priority=900)
public abstract class PolymerMiningCadenceMixin {
    @Shadow @Final protected ServerPlayer player;
    @Shadow private int gameTicks;
    @Unique private int skywars2$cooldownUntil;
    @Unique private final TpsCompensation.Clock skywars2$polymerClock=new TpsCompensation.Clock();
    @Unique private int skywars2$polymerSteps;
    @Unique private double skywars2$polymerElapsed;

    @Unique private void skywars2$resetPolymerClock(){
        skywars2$polymerSteps=0;skywars2$polymerElapsed=0;skywars2$polymerClock.reset(0);
        skywars2$polymerClock.elapsed(player,0);
    }
    @Inject(method="handleBlockBreakAction",at=@At("HEAD"))
    private void skywars2$polymerAction(BlockPos pos,ServerboundPlayerActionPacket.Action action,
            Direction direction,int maxY,int sequence,CallbackInfo ci){
        if(action==ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK
                ||action==ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK)skywars2$resetPolymerClock();
    }

    @Inject(method="destroyAndAck",at=@At("HEAD"))
    private void skywars2$miningCompleted(CallbackInfo ci){
        skywars2$cooldownUntil=gameTicks+5;
        skywars2$resetPolymerClock();
    }

    // SkyWarsMixinPlugin connects the already merged Polymer field reads here.
    // Mixins deliberately exclude other mixins' private handlers from selectors.
    @Unique
    private int skywars2$elapsedCooldown(int remaining){
        int effective=Math.min(remaining,Math.max(0,skywars2$cooldownUntil-gameTicks));
        // The native five-tick inter-block delay grants no deferred mining credit.
        if(effective>0)skywars2$resetPolymerClock();
        return effective;
    }

    /** Connected only to Polymer's own sampled rate; its completion and crack packets remain native. */
    @Unique private float skywars2$polymerMiningDelta(float rate){
        double elapsed=skywars2$polymerClock.elapsed(player,++skywars2$polymerSteps);
        double delta=Math.max(0,elapsed-skywars2$polymerElapsed);skywars2$polymerElapsed=elapsed;
        return (float)(rate*delta);
    }
}
