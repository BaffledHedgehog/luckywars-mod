package me.skywars2.mixin;

import me.skywars2.SkyWars2;
import me.skywars2.game.FullInvisibility;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.ValueInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

/** Изменения эффектов обновляют индексы маны/цветов и представление полной невидимости. */
@Mixin(LivingEntity.class)
public abstract class LbcColorEffectsMixin {
    /** Оглушение убирает AI/прыжок, сохраняя обычный travel с гравитацией и внешней скоростью. */
    @Inject(method="isImmobile",at=@At("HEAD"),cancellable=true)
    private void skywars2$stunnedInput(CallbackInfoReturnable<Boolean> ci) {
        var game = SkyWars2.game();
        if (game != null && game.items().shooters().stunned((LivingEntity)(Object)this)) ci.setReturnValue(true);
    }

    @Inject(method="aiStep",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;applyInput()V",shift=At.Shift.AFTER))
    private void skywars2$clearStunnedInput(CallbackInfo ci) {
        var game = SkyWars2.game();
        if (game != null) game.items().shooters().prepareStunnedInput((LivingEntity)(Object)this);
    }
    @Inject(method="resolvePlayerResponsibleForDamage",at=@At("TAIL"))
    private void skywars2$environmentOwner(net.minecraft.world.damagesource.DamageSource source,
            CallbackInfoReturnable<net.minecraft.world.entity.player.Player> ci) {
        var game=SkyWars2.game();
        if(game!=null) game.items().indirectKills().environmentDamage((LivingEntity)(Object)this,source);
    }
    @Inject(method="addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
            at=@At("HEAD"),cancellable=true)
    private void skywars2$projectileEffect(MobEffectInstance effect,Entity source,CallbackInfoReturnable<Boolean> ci) {
        var game=SkyWars2.game();
        if(game!=null && !game.items().indirectKills().allowProjectileEffect((LivingEntity)(Object)this,effect,source))
            ci.setReturnValue(false);
    }
    @Inject(method="canBeAffected",at=@At("HEAD"),cancellable=true)
    private void skywars2$artifactEffectGuard(MobEffectInstance effect, CallbackInfoReturnable<Boolean> ci) {
        // Raid Omen 119 служил транспортом Stand-стрелы в датапаке. Настоящее
        // попадание уже обработано один раз; не запускаем эффект и повторные попытки.
        if(me.skywars2.game.LbcStand.isArrowMarker(effect)) { ci.setReturnValue(false); return; }
        var game = SkyWars2.game();
        if (game != null && (Object)this instanceof ServerPlayer player
                && game.items().magic().rejectEffect(player, effect.getEffect())) ci.setReturnValue(false);
    }

    @Inject(method="tickEffects",at=@At("HEAD"))
    private void skywars2$artifactBeforeEffects(CallbackInfo ci) {
        var game = SkyWars2.game();
        if (game != null && (Object)this instanceof ServerPlayer player) game.items().magic().beforeEffects(player);
    }
    @Inject(method="readAdditionalSaveData",at=@At("TAIL"))
    private void skywars2$loadedInvisibility(ValueInput input, CallbackInfo ci) {
        // NBT заполняет active_effects напрямую; скрываем игрока ещё до первого pairing при входе.
        FullInvisibility.effectsChanged((LivingEntity)(Object)this);
    }
    @Inject(method="onEffectAdded",at=@At("TAIL"))
    private void skywars2$colorAdded(MobEffectInstance effect, Entity source, CallbackInfo ci) {
        indirectEffectChanged(effect,source);
        if(relevant(effect)) changed();
        if(effect.is(MobEffects.INVISIBILITY)) FullInvisibility.effectsChanged((LivingEntity)(Object)this);
    }
    @Inject(method="onEffectUpdated",at=@At("TAIL"))
    private void skywars2$colorUpdated(MobEffectInstance effect, boolean refresh, Entity source, CallbackInfo ci) {
        if(refresh) indirectEffectChanged(effect,source);
        if(relevant(effect)) changed();
        if(effect.is(MobEffects.INVISIBILITY)) FullInvisibility.effectsChanged((LivingEntity)(Object)this);
    }
    @Inject(method="onEffectsRemoved",at=@At("TAIL"))
    private void skywars2$colorRemoved(Collection<MobEffectInstance> effects, CallbackInfo ci) {
        var game=SkyWars2.game();
        if(game!=null) for(var effect:effects) game.items().indirectKills().effectRemoved((LivingEntity)(Object)this,effect);
        for(var effect:effects) if(relevant(effect)) { changed(); break; }
        for(var effect:effects) if(effect.is(MobEffects.INVISIBILITY)) {
            FullInvisibility.effectsChanged((LivingEntity)(Object)this); break;
        }
    }
    private static boolean relevant(MobEffectInstance effect) {
        return effect.is(MobEffects.CONDUIT_POWER) || effect.is(MobEffects.WATER_BREATHING) || effect.is(MobEffects.RAID_OMEN);
    }
    private void indirectEffectChanged(MobEffectInstance effect, Entity source) {
        var game=SkyWars2.game();
        if(game!=null) {
            var target=(LivingEntity)(Object)this;
            game.items().indirectKills().effectChanged(target,effect,source);
        }
    }
    private void changed() {
        var game=SkyWars2.game();
        if(game!=null) {
            var entity=(LivingEntity)(Object)this;
            game.items().magic().colors().effectsChanged(entity);
            game.items().mana().effectsChanged(entity);
        }
    }
}
