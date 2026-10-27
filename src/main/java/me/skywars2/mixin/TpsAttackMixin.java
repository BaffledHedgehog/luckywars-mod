package me.skywars2.mixin;

import me.skywars2.game.TpsCompensation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class TpsAttackMixin extends net.minecraft.world.entity.LivingEntity implements TpsCompensation.PlayerClock {
    protected TpsAttackMixin(net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.LivingEntity> type,
            net.minecraft.world.level.Level level) { super(type, level); }
    @Shadow public abstract float getCurrentItemAttackStrengthDelay();
    @Unique private final TpsCompensation.Clock skywars2$attackClock = new TpsCompensation.Clock();
    @Unique private final TpsCompensation.Clock skywars2$swapClock = new TpsCompensation.Clock();
    @Unique private Object skywars2$attackLevel;
    @Unique private boolean skywars2$clockEnabled;
    @Unique private int skywars2$clockEpoch;
    @Override public int skywars2$compensationEpoch() { return skywars2$clockEpoch; }

    @Override public void skywars2$compensationTick() {
        if ((Object)this instanceof ServerPlayer player) {
            boolean enabled = TpsCompensation.enabled(player);
            if (skywars2$attackLevel != player.level() || enabled != skywars2$clockEnabled) skywars2$clockEpoch++;
            skywars2$clockEnabled = enabled;
            if (skywars2$attackLevel != player.level() || !enabled) {
                skywars2$attackClock.reset(attackStrengthTicker);
                skywars2$swapClock.reset(itemSwapTicker);
            }
            skywars2$attackLevel = player.level();
            skywars2$attackClock.elapsed(player, attackStrengthTicker);
            skywars2$swapClock.elapsed(player, itemSwapTicker);
        }
    }

    @Inject(method="resetAttackStrengthTicker", at=@At("RETURN"))
    private void skywars2$resetBoth(CallbackInfo ci) {
        skywars2$attackClock.reset(0);
        skywars2$swapClock.reset(0);
    }

    @Inject(method="resetOnlyAttackStrengthTicker", at=@At("RETURN"))
    private void skywars2$resetAttack(CallbackInfo ci) { skywars2$attackClock.reset(0); }

    @Unique private float skywars2$strength(TpsCompensation.Clock clock, int ticks, float partial, ServerPlayer player) {
        double value = (clock.elapsed(player, ticks) + partial) / getCurrentItemAttackStrengthDelay();
        return (float)Math.max(0, Math.min(1, value));
    }

    @Inject(method="getAttackStrengthScale", at=@At("HEAD"), cancellable=true)
    private void skywars2$attackTime(float partial, CallbackInfoReturnable<Float> cir) {
        if ((Object)this instanceof ServerPlayer player)
            cir.setReturnValue(skywars2$strength(skywars2$attackClock, attackStrengthTicker, partial, player));
    }

    @Inject(method="getItemSwapScale", at=@At("HEAD"), cancellable=true)
    private void skywars2$swapTime(float partial, CallbackInfoReturnable<Float> cir) {
        if ((Object)this instanceof ServerPlayer player)
            cir.setReturnValue(skywars2$strength(skywars2$swapClock, itemSwapTicker, partial, player));
    }

    @Inject(method="cannotAttackWithItem", at=@At("HEAD"), cancellable=true)
    private void skywars2$minimumCharge(ItemStack stack, int tolerance, CallbackInfoReturnable<Boolean> cir) {
        if ((Object)this instanceof ServerPlayer player) {
            float required = stack.getOrDefault(net.minecraft.core.component.DataComponents.MINIMUM_ATTACK_CHARGE, 0f);
            double optimistic = (skywars2$attackClock.elapsed(player, attackStrengthTicker) + tolerance)
                    / getCurrentItemAttackStrengthDelay();
            cir.setReturnValue(required > 0 && optimistic < required);
        }
    }
}
