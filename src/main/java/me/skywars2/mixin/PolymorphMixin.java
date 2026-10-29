package me.skywars2.mixin;

import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public final class PolymorphMixin {
    private PolymorphMixin(){}
    @Mixin(ServerLevel.class)
    public static abstract class Level {
        @Inject(method="tickNonPassenger",at=@At("HEAD"),cancellable=true)
        private void polymorphSuspend(Entity entity,CallbackInfo ci){var g=SkyWars2.game();if(g!=null&&g.items().polymorph().suspended(entity))ci.cancel();}
    }
    @Mixin(value={LivingEntity.class,ServerPlayer.class},priority=3000)
    public static abstract class Damage {
        @Inject(method="hurtServer",at=@At("HEAD"),cancellable=true)
        private void polymorphDamage(ServerLevel level,DamageSource source,float amount,CallbackInfoReturnable<Boolean> cir){
            var g=SkyWars2.game();var entity=(LivingEntity)(Object)this;
            if(g!=null&&g.items().polymorph().active(entity)){cir.setReturnValue(g.items().polymorph().redirectDamage(entity,level,source,amount));}
        }
    }
    @Mixin(LivingEntity.class)
    public static abstract class Effects {
        @Inject(method="updateInvisibilityStatus",at=@At("HEAD"),cancellable=true)
        private void polymorphHidden(CallbackInfo ci){var g=SkyWars2.game();if(g!=null&&g.items().polymorph().active((Entity)(Object)this)){((Entity)(Object)this).setInvisible(true);ci.cancel();}}
        @Inject(method="heal",at=@At("HEAD"),cancellable=true)
        private void polymorphNoHeal(float amount,CallbackInfo ci){var g=SkyWars2.game();if(g!=null&&g.items().polymorph().active((Entity)(Object)this)&&!g.items().polymorph().restoring())ci.cancel();}
        @Inject(method="addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",at=@At("HEAD"),cancellable=true)
        private void polymorphNoEffects(MobEffectInstance effect,Entity source,CallbackInfoReturnable<Boolean> cir){
            var g=SkyWars2.game();if(g!=null&&g.items().polymorph().active((Entity)(Object)this)&&!g.items().polymorph().restoring())cir.setReturnValue(false);
        }
    }
    @Mixin(net.minecraft.server.players.PlayerList.class)
    public static abstract class Lifecycle {
        @Inject(method="remove",at=@At("HEAD"))
        private void polymorphRestoreBeforeSave(ServerPlayer player,CallbackInfo ci){var g=SkyWars2.game();if(g!=null)g.items().polymorph().forget(player);}
    }
    @Mixin(Entity.class)
    public static abstract class Collision {
        @Inject(method="push(Lnet/minecraft/world/entity/Entity;)V",at=@At("HEAD"),cancellable=true)
        private void polymorphNoPush(Entity other,CallbackInfo ci){var g=SkyWars2.game();if(g!=null&&(g.items().polymorph().concealed((Entity)(Object)this)||g.items().polymorph().concealed(other)))ci.cancel();}
    }
    @Mixin(net.minecraft.world.entity.item.ItemEntity.class)
    public static abstract class Pickup {
        @Inject(method="playerTouch",at=@At("HEAD"),cancellable=true)
        private void polymorphNoPickup(net.minecraft.world.entity.player.Player p,CallbackInfo ci){var g=SkyWars2.game();if(g!=null&&g.items().polymorph().active(p))ci.cancel();}
    }
}
