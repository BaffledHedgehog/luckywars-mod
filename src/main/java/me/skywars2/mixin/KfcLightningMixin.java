package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import me.skywars2.SkyWars2;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Cultus keeps vanilla lightning damage and native fire, while sharing owner and target rules. */
public final class KfcLightningMixin {
    @Mixin(LightningBolt.class)
    public abstract static class Targets {
        @WrapOperation(method="tick",at=@At(value="INVOKE",target=
                "Lnet/minecraft/world/entity/Entity;thunderHit(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LightningBolt;)V"))
        private void skywars2$cultusTarget(Entity target,ServerLevel level,LightningBolt bolt,Operation<Void> original) {
            var game=SkyWars2.game();
            if(game==null || game.items().kfcRewards().lightningTarget(bolt,target)) original.call(target,level,bolt);
        }
    }
    @Mixin(Entity.class)
    public abstract static class Impact {
        @WrapOperation(method="thunderHit",at=@At(value="INVOKE",target=
                "Lnet/minecraft/world/entity/Entity;hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
        private boolean skywars2$cultusOwner(Entity target,ServerLevel level,DamageSource source,float amount,
                Operation<Boolean> original,@Local(argsOnly=true) LightningBolt bolt) {
            var game=SkyWars2.game(); var owner=game==null?null:game.items().kfcRewards().owner(bolt);
            if(owner!=null) source=new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                    .getOrThrow(DamageTypes.LIGHTNING_BOLT),bolt,owner);
            boolean applied=original.call(target,level,source,amount);
            if(owner!=null && target instanceof LivingEntity living) game.items().indirectKills().ignited(living,owner);
            return applied;
        }
    }
}
