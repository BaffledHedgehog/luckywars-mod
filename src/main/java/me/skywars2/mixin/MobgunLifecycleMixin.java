package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.skywars2.SkyWars2;
import me.skywars2.game.LbcMobgun;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Native children inherit their actual managed actor's lifecycle before entity-load handlers run. */
public final class MobgunLifecycleMixin {
    @Mixin(ServerLevel.class)
    public abstract static class Ticks {
        @WrapMethod(method="tickNonPassenger")
        private void skywars2$actorTick(Entity entity,Operation<Void> original) {
            var game=SkyWars2.game();
            if(game==null) { original.call(entity); return; }
            var service=game.items().mobgun();
            if(service.isManaged(entity) && service.owner(entity)==null) { service.dispose(entity); return; }
            service.withParent(entity,()->{ original.call(entity); return null; });
        }

        @WrapMethod(method="tickPassenger")
        private void skywars2$actorPassengerTick(Entity vehicle,Entity passenger,Operation<Void> original) {
            var game=SkyWars2.game();
            if(game==null) { original.call(vehicle,passenger); return; }
            var service=game.items().mobgun();
            if(service.isManaged(passenger) && service.owner(passenger)==null) { service.dispose(passenger); return; }
            service.withParent(passenger,()->{ original.call(vehicle,passenger); return null; });
        }

    }

    /** Cube splitting can run from removal outside an entity tick, including magma cubes. */
    @Mixin(AbstractCubeMob.class)
    public abstract static class Split {
        @WrapMethod(method="remove")
        private void skywars2$actorSplit(Entity.RemovalReason reason,Operation<Void> original) {
            var game=SkyWars2.game();
            if(game==null) original.call(reason);
            else game.items().mobgun().withParent((Entity)(Object)this,()->{ original.call(reason); return null; });
        }
    }

    /** The three-argument overload delegates here; wrapping one method avoids duplicate scopes. */
    @Mixin(Mob.class)
    public abstract static class Convert {
        @WrapMethod(method="convertTo(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/ConversionParams;Lnet/minecraft/world/entity/EntitySpawnReason;Lnet/minecraft/world/entity/ConversionParams$AfterConversion;)Lnet/minecraft/world/entity/Mob;")
        private <T extends Mob> T skywars2$actorConversion(EntityType<T> type,ConversionParams params,
                EntitySpawnReason reason,ConversionParams.AfterConversion<T> afterConversion,Operation<T> original) {
            var game=SkyWars2.game();
            return game==null ? original.call(type,params,reason,afterConversion)
                    : game.items().mobgun().withParent((Entity)(Object)this,()->original.call(type,params,reason,afterConversion));
        }
    }

    @Mixin(net.minecraft.world.entity.boss.enderdragon.EndCrystal.class)
    public abstract static class Crystal {
        @WrapMethod(method="hurtServer")
        private boolean skywars2$actorCrystalAttack(ServerLevel level,net.minecraft.world.damagesource.DamageSource source,
                float amount,Operation<Boolean> original) {
            var game=SkyWars2.game();
            return game==null ? original.call(level,source,amount)
                    : game.items().mobgun().withParent((Entity)(Object)this,()->original.call(level,source,amount));
        }
    }

    @Mixin(net.minecraft.world.entity.vehicle.minecart.MinecartTNT.class)
    public abstract static class Minecart {
        @WrapMethod(method="hurtServer")
        private boolean skywars2$actorMinecartAttack(ServerLevel level,net.minecraft.world.damagesource.DamageSource source,
                float amount,Operation<Boolean> original) {
            var game=SkyWars2.game();
            return game==null ? original.call(level,source,amount)
                    : game.items().mobgun().withParent((Entity)(Object)this,()->original.call(level,source,amount));
        }
        @WrapMethod(method="explode")
        private void skywars2$actorMinecartExplosion(net.minecraft.world.damagesource.DamageSource source,
                double speedSqr,Operation<Void> original) {
            var game=SkyWars2.game();
            if(game==null) original.call(source,speedSqr);
            else game.items().mobgun().withParent((Entity)(Object)this,()->{ original.call(source,speedSqr); return null; });
        }
    }

    @Mixin(Entity.class)
    public abstract static class Save {
        @Inject(method="shouldBeSaved",at=@At("HEAD"),cancellable=true)
        private void skywars2$actorNotSaved(CallbackInfoReturnable<Boolean> ci) {
            if(((Entity)(Object)this).entityTags().contains(LbcMobgun.ACTOR)) ci.setReturnValue(false);
        }
    }
}
