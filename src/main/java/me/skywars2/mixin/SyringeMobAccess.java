package me.skywars2.mixin;

import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

public interface SyringeMobAccess {
    @Mixin(net.minecraft.world.entity.LivingEntity.class)
    abstract class DeathLoot {
        @org.spongepowered.asm.mixin.injection.Inject(method="dropAllDeathLoot",at=@org.spongepowered.asm.mixin.injection.At("HEAD"))
        private void skywars2$syringeLoot(net.minecraft.server.level.ServerLevel level,net.minecraft.world.damagesource.DamageSource source,
                org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci){
            var game=me.skywars2.SkyWars2.game();if(game!=null)game.items().syringe().beforeDeathLoot((net.minecraft.world.entity.LivingEntity)(Object)this);
        }
    }
    @Mixin(Ghast.class)
    interface GhastPower {
        @Accessor("explosionPower") int skywars2$syringePower();
        @Accessor("explosionPower") void skywars2$syringePower(int value);
    }
    @Mixin(Creeper.class)
    interface CreeperRadius {
        @Accessor("explosionRadius") int skywars2$syringeRadius();
        @Accessor("explosionRadius") void skywars2$syringeRadius(int value);
    }
}
