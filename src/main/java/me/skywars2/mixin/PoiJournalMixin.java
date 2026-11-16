package me.skywars2.mixin;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import me.skywars2.game.MatchPoiStorage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.IOWorker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** No active POI point means a volatile false check only; other native workers are untouched. */
@Mixin(IOWorker.class)
public abstract class PoiJournalMixin {
    @Inject(method="store(Lnet/minecraft/world/level/ChunkPos;Ljava/util/function/Supplier;)Ljava/util/concurrent/CompletableFuture;",at=@At("HEAD"),cancellable=true)
    private void skywars2$poiCheckpoint(ChunkPos pos,Supplier<CompoundTag> value,CallbackInfoReturnable<CompletableFuture<Void>> callback){
        var suppressed=MatchPoiStorage.beforeWrite((IOWorker)(Object)this,pos);if(suppressed!=null)callback.setReturnValue(suppressed);
    }
}
