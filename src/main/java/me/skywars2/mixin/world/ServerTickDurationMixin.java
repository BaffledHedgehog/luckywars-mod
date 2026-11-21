package me.skywars2.mixin.world;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import me.skywars2.game.net.metrics.ServerTickMetrics;

import java.util.function.BooleanSupplier;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;

/** Include Fabric END_SERVER_TICK callbacks, which run after vanilla's own timing tally. */
@Mixin(MinecraftServer.class)
abstract class ServerTickDurationMixin {
    @WrapMethod(method="tickServer")
    private void skywars2$wholeTick(BooleanSupplier keepTicking,Operation<Void> original){
        long started=System.nanoTime();
        try{original.call(keepTicking);}finally{ServerTickMetrics.recordDuration((MinecraftServer)(Object)this,System.nanoTime()-started);}
    }
}
