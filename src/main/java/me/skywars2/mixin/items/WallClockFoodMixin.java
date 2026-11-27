package me.skywars2.mixin.items;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.skywars2.game.items.food.FoodUseClock;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class WallClockFoodMixin implements FoodUseClock.Control {
    @Shadow protected int useItemRemaining;
    @Shadow protected abstract void completeUsingItem();
    @Unique private ItemStack skywars2$timedFood = ItemStack.EMPTY;
    @Unique private long skywars2$foodStarted;
    @Unique private int skywars2$foodDuration;
    @Unique private long skywars2$foodPausedAt;
    @Unique private boolean skywars2$foodPaused;

    @WrapMethod(method = "startUsingItem")
    private void skywars2$startFood(InteractionHand hand, Operation<Void> original) {
        var user = (LivingEntity)(Object)this;
        boolean using = user.isUsingItem();
        original.call(hand);
        if (!using && user.isUsingItem() && user instanceof ServerPlayer player
                && FoodUseClock.eligible(player, user.getUseItem())) {
            skywars2$timedFood = user.getUseItem().copy();
            skywars2$foodDuration = user.getUseItem().getUseDuration(user);
            // Includes time spent waiting in the game-thread packet queue.
            skywars2$foodStarted = FoodUseClock.now();
            skywars2$foodPaused = false;
        }
    }

    @Unique private boolean skywars2$timedFoodValid() {
        var user = (LivingEntity)(Object)this;
        return !skywars2$timedFood.isEmpty() && user instanceof ServerPlayer player && user.isUsingItem()
                && FoodUseClock.eligible(player, user.getUseItem())
                && ItemStack.isSameItemSameComponents(skywars2$timedFood, user.getItemInHand(user.getUsedItemHand()));
    }

    @Inject(method = "updateUsingItem", at = @At("HEAD"))
    private void skywars2$foodProgress(ItemStack item, CallbackInfo ci) {
        if (skywars2$timedFoodValid()) {
            // Native updateUsingItem decrements once and owns the sole completion call.
            useItemRemaining = FoodUseClock.remaining(skywars2$foodDuration, skywars2$foodStarted,
                    skywars2$foodPaused ? skywars2$foodPausedAt : System.nanoTime()) + 1;
        }
    }

    @Inject(method = "releaseUsingItem", at = @At("HEAD"), cancellable = true)
    private void skywars2$finishBeforeClientRelease(CallbackInfo ci) {
        if (skywars2$timedFoodValid()
                && !skywars2$foodPaused
                && FoodUseClock.remaining(skywars2$foodDuration, skywars2$foodStarted, FoodUseClock.now()) == 0) {
            completeUsingItem();
            ci.cancel();
        }
    }

    @Inject(method = "stopUsingItem", at = @At("TAIL"))
    private void skywars2$clearFoodClock(CallbackInfo ci) { skywars2$timedFood = ItemStack.EMPTY; skywars2$foodPaused = false; }

    public void skywars2$pauseFood(long now) {
        if (!skywars2$timedFood.isEmpty() && !skywars2$foodPaused) {
            skywars2$foodPausedAt = now; skywars2$foodPaused = true;
        }
    }
    public void skywars2$resumeFood(long now) {
        if (skywars2$foodPaused) {
            skywars2$foodStarted += Math.max(0, now - skywars2$foodPausedAt);
            skywars2$foodPaused = false;
        }
    }
    public void skywars2$rebaseFood(long now) {
        skywars2$timedFood = ItemStack.EMPTY; skywars2$foodPaused = false;
        var user = (LivingEntity)(Object)this;
        if (user instanceof ServerPlayer player && user.isUsingItem() && FoodUseClock.eligible(player, user.getUseItem())) {
            skywars2$timedFood = user.getUseItem().copy();
            skywars2$foodDuration = user.getUseItem().getUseDuration(user);
            int remaining = Math.clamp(user.getUseItemRemainingTicks(), 0, skywars2$foodDuration);
            skywars2$foodStarted = now - (skywars2$foodDuration - remaining) * FoodUseClock.TICK_NANOS;
            var game = me.skywars2.SkyWars2.game();
            if (game != null && game.items().timeStop().frozen(player)) skywars2$pauseFood(now);
        }
    }

    /** Exact pause boundaries, independent of how often frozen player simulation runs. */
    @Mixin(value = me.skywars2.game.effects.time.LbcTimeStop.class, remap = false)
    public abstract static class TimeStop {
        @Inject(method = "prepareFrozen", at = @At("TAIL"))
        private void skywars2$pauseFood(net.minecraft.world.entity.Entity entity, CallbackInfo ci) {
            if (entity instanceof ServerPlayer player
                    && ((me.skywars2.game.effects.time.LbcTimeStop)(Object)this).frozen(entity))
                ((FoodUseClock.Control)(Object)player).skywars2$pauseFood(System.nanoTime());
        }
        @Inject(method = "restore", at = @At("TAIL"))
        private void skywars2$resumeFood(net.minecraft.world.entity.Entity entity,
                @org.spongepowered.asm.mixin.injection.Coerce Object state, CallbackInfo ci) {
            if (entity instanceof ServerPlayer player)
                ((FoodUseClock.Control)(Object)player).skywars2$resumeFood(System.nanoTime());
        }
    }

    @Mixin(value = me.skywars2.game.match.checkpoints.MatchPlayerCheckpoint.class, remap = false)
    public abstract static class Restore {
        @Inject(method = "restoreFrozen", at = @At("RETURN"))
        private void skywars2$rebaseRestoredFood(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<ServerPlayer> ci) {
            var player = ci.getReturnValue();
            if (player != null) ((FoodUseClock.Control)(Object)player).skywars2$rebaseFood(System.nanoTime());
        }
    }

    /** Transient receipt time follows each packet through native thread scheduling, with no side map. */
    @Mixin({ServerboundUseItemPacket.class, ServerboundUseItemOnPacket.class, ServerboundPlayerActionPacket.class})
    public abstract static class Arrival implements FoodUseClock.Arrival {
        @Unique private long skywars2$received;
        public long skywars2$foodArrival() { return skywars2$received; }
        public void skywars2$recordFoodArrival() { if (skywars2$received == 0) skywars2$received = System.nanoTime(); }
    }

    @Mixin(ServerGamePacketListenerImpl.class)
    public abstract static class Input {
        @Shadow public ServerPlayer player;
        @Unique private Long skywars2$begin(FoodUseClock.Arrival packet) {
            packet.skywars2$recordFoodArrival();
            return player.level().getServer().isSameThread() ? FoodUseClock.enter(packet.skywars2$foodArrival()) : null;
        }
        @WrapMethod(method = "handleUseItem")
        private void skywars2$useArrival(ServerboundUseItemPacket packet, Operation<Void> original) {
            boolean main = player.level().getServer().isSameThread();
            var previous = skywars2$begin((FoodUseClock.Arrival)(Object)packet);
            try { original.call(packet); } finally { if (main) FoodUseClock.leave(previous); }
        }
        @WrapMethod(method = "handleUseItemOn")
        private void skywars2$blockUseArrival(ServerboundUseItemOnPacket packet, Operation<Void> original) {
            boolean main = player.level().getServer().isSameThread();
            var previous = skywars2$begin((FoodUseClock.Arrival)(Object)packet);
            try { original.call(packet); } finally { if (main) FoodUseClock.leave(previous); }
        }
        @WrapMethod(method = "handlePlayerAction")
        private void skywars2$releaseArrival(ServerboundPlayerActionPacket packet, Operation<Void> original) {
            boolean main = player.level().getServer().isSameThread();
            var previous = skywars2$begin((FoodUseClock.Arrival)(Object)packet);
            try { original.call(packet); } finally { if (main) FoodUseClock.leave(previous); }
        }
    }
}
