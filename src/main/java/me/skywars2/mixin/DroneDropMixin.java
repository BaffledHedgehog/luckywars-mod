package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.vehicle.minecart.MinecartSpawner;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Source hooks leave unrelated native carts, spawners and TNT unchanged. */
public final class DroneDropMixin {
    private DroneDropMixin(){ }

    @Mixin(MinecartSpawner.class)
    public abstract static class Cart {
        @WrapOperation(method="tick",at=@At(value="INVOKE",target="Ljava/lang/Runnable;run()V"))
        private void skywars2$nativeProducer(Runnable ticker,Operation<Void> original){
            var game=SkyWars2.game();
            if(game==null || game.items().droneDrops().canTick((MinecartSpawner)(Object)this))original.call(ticker);
        }
    }

    @Mixin(BaseSpawner.class)
    public abstract static class Candidates {
        @WrapOperation(method="serverTick",at=@At(value="INVOKE",target=
                "Lnet/minecraft/server/level/ServerLevel;noCollision(Lnet/minecraft/world/phys/AABB;)Z"))
        private boolean skywars2$residentCandidate(ServerLevel level,AABB bounds,Operation<Boolean> original){
            var game=SkyWars2.game();
            return (game==null || game.items().droneDrops().canSpawnAt((BaseSpawner)(Object)this,level,bounds))
                    && original.call(level,bounds);
        }
    }

    @Mixin(PrimedTnt.class)
    public abstract static class Ground {
        @Inject(method="tick",at=@At("HEAD"))
        private void skywars2$groundbam(CallbackInfo ci){
            var game=SkyWars2.game();
            if(game!=null)game.items().droneDrops().groundbam((PrimedTnt)(Object)this);
        }
    }

    @Mixin(LivingEntity.class)
    public abstract static class HostDeath {
        @Inject(method="die",at=@At("TAIL"))
        private void skywars2$hostDeath(DamageSource source,CallbackInfo ci){
            var game=SkyWars2.game();var actor=(LivingEntity)(Object)this;
            if(game!=null && actor.isDeadOrDying())game.items().droneDrops().hostDied(actor);
        }
    }
}
