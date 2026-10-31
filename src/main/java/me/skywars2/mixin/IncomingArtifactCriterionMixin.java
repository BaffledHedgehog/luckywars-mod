package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.skywars2.SkyWars2;
import me.skywars2.game.LbcIncomingArtifacts;
import net.minecraft.advancements.triggers.EntityHurtPlayerTrigger;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.FallingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** This native criterion includes accepted shield, environmental and fatal events. */
@Mixin(EntityHurtPlayerTrigger.class)
public abstract class IncomingArtifactCriterionMixin {
    @Inject(method="trigger(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/damagesource/DamageSource;FFZ)V",at=@At("HEAD"))
    private void skywars2$incoming(ServerPlayer player,DamageSource source,float originalDamage,float actualDamage,boolean blocked,CallbackInfo ci){
        var game=SkyWars2.game();if(game!=null)try(var scope=game.items().spellSources().scope((me.skywars2.game.LbcSpellSource.Source)null)){
            game.items().incomingArtifacts().incoming(player,source,originalDamage,actualDamage,blocked);
            game.items().shulkerArmor().incoming(player);
            game.items().armorSets().glass().incoming(player,source);
            game.items().armorSets().nanobots().incoming(player);
            game.items().armorSets().berserk().incoming(player);
            game.items().armorSets().techno().incoming(player);
            game.items().armorSets().chaos().incoming(player);
        }
    }
    @Mixin(LivingEntity.class)
    public static abstract class HurtAttempt {
        @WrapMethod(method="hurtServer")
        private boolean skywars2$incomingAttempt(ServerLevel level,DamageSource source,float amount,Operation<Boolean> nativeHurt){
            var game=SkyWars2.game();
            if(game!=null&&(Object)this instanceof ServerPlayer player)
                return game.items().withIncomingDamageAttempt(player,()->nativeHurt.call(level,source,amount));
            return nativeHurt.call(level,source,amount);
        }
    }
    @Mixin(FallingBlockEntity.class)
    public static abstract class SandPhysics {
        @WrapMethod(method="tick")
        private void skywars2$ownedSand(Operation<Void> nativeTick){
            var game=SkyWars2.game();var actor=(FallingBlockEntity)(Object)this;
            if(game!=null&&LbcIncomingArtifacts.managed(actor))game.items().incomingArtifacts().withSandTick(actor,()->nativeTick.call());
            else nativeTick.call();
        }
    }
    @Mixin(Mob.class)
    public static abstract class WolfTarget {
        @Inject(method="setTarget",at=@At("RETURN"))
        private void skywars2$ownedWolf(LivingEntity target,CallbackInfo ci){
            var game=SkyWars2.game();if(game!=null)game.items().incomingArtifacts().targetChanged((Mob)(Object)this,target);
        }
    }
}
