package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Lets ordinary Creative block breaks continue through vanilla's native loot path. */
@Mixin(ServerPlayerGameMode.class)
public abstract class CreativeBlockDropsMixin {
    @Shadow @Final protected ServerPlayer player;
    @Shadow private boolean isDestroyingBlock;
    @Shadow private boolean hasDelayedDestroy;
    @Shadow private BlockPos destroyPos;
    @Shadow private BlockPos delayedDestroyPos;

    /** Сбрасываем прежнее копание, чтобы оно не завершилось само после заморозки или оттаивания. */
    @Inject(method = "tick", at = @At("HEAD"))
    private void skywars2$stopFrozenDestroyProgress(CallbackInfo ci) {
        if (!isDestroyingBlock && !hasDelayedDestroy) return;
        var game = SkyWars2.game();
        if (game == null || !game.items().actionsBlocked(player)) return;
        if (isDestroyingBlock) player.level().destroyBlockProgress(player.getId(), destroyPos, -1);
        if (hasDelayedDestroy) player.level().destroyBlockProgress(player.getId(), delayedDestroyPos, -1);
        isDestroyingBlock = hasDelayedDestroy = false;
    }

    /** Запрещаем и отложенное разрушение, начатое до заморозки, а не только новые пакеты. */
    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void skywars2$frozenBlockBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        var game = SkyWars2.game();
        if (game != null && game.items().actionsBlocked(player)) cir.setReturnValue(false);
    }

    /** Also reject native/server-script use paths, which do not pass through incoming packets. */
    @Inject(method = {"useItem", "useItemOn"}, at = @At("HEAD"), cancellable = true)
    private void skywars2$blockedNativeUse(CallbackInfoReturnable<InteractionResult> cir) {
        var game = SkyWars2.game();
        if (game != null && game.items().actionsBlocked(player)) cir.setReturnValue(InteractionResult.FAIL);
    }

    @ModifyExpressionValue(method = "destroyBlock", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;preventsBlockDrops()Z"))
    private boolean skywars2$allowCreativeBlockDrops(boolean preventsDrops) {
        if (!preventsDrops || player.entityTags().contains("developer")) return preventsDrops;
        var game = SkyWars2.game();
        if (game != null && game.isObserver(player)) return preventsDrops;
        return false;
    }

    @ModifyExpressionValue(method = "destroyBlock", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;hasCorrectToolForDrops(Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean skywars2$allowBareHandCreativeDrops(boolean correctTool) {
        if (correctTool || !player.getAbilities().instabuild || player.entityTags().contains("developer")) return correctTool;
        var game = SkyWars2.game();
        if (game != null && game.isObserver(player)) return false;
        return true;
    }
}
