package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import me.skywars2.SkyWars2;
import me.skywars2.game.LbcDamage;
import me.skywars2.game.LbcElectro;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Нативные молнии и отражённые опасности сохраняют автора; обычная погода остаётся ванильной. */
public final class ElectroNativeMixins {
    /** Preserve vanilla egg spawning while attributing its lightning before the first native bolt tick. */
    @Mixin(net.minecraft.world.item.SpawnEggItem.class)
    public abstract static class Egg {
        @WrapOperation(method="spawnMob",at=@At(value="INVOKE",target=
                "Lnet/minecraft/world/entity/EntityType;spawn(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/EntitySpawnReason;ZZ)Lnet/minecraft/world/entity/Entity;"))
        private static Entity skywars2$eggLightning(net.minecraft.world.entity.EntityType<?> type,ServerLevel level,
                net.minecraft.world.item.ItemStack stack,LivingEntity user,net.minecraft.core.BlockPos pos,
                net.minecraft.world.entity.EntitySpawnReason reason,boolean moveDown,boolean movedUp,Operation<Entity> original) {
            Entity entity=original.call(type,level,stack,user,pos,reason,moveDown,movedUp);
            var game=SkyWars2.game();
            if(game!=null && entity instanceof LightningBolt bolt && user instanceof ServerPlayer player
                    && me.skywars2.game.LbcWands.isBoltMaterial(stack)) game.items().electro().manageLightning(player,bolt);
            return entity;
        }
    }
    @Mixin(LightningBolt.class)
    public abstract static class Bolt {
        @WrapMethod(method="tick")
        private void skywars2$lightningFireOwner(Operation<Void> original) {
            var bolt=(LightningBolt)(Object)this; var game=SkyWars2.game();
            if(game!=null) game.items().lightningAcquisition().beforeLightningTick(bolt);
            if(game==null || bolt.getCause()==null
                    || !(bolt.level() instanceof ServerLevel level)) original.call();
            else game.items().indirectKills().worldChanges(level,bolt,()->original.call());
        }
        @WrapOperation(method="tick",at=@At(value="INVOKE",target=
                "Lnet/minecraft/world/entity/Entity;thunderHit(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LightningBolt;)V"))
        private void skywars2$lightningTarget(Entity target,ServerLevel level,LightningBolt bolt,Operation<Void> original) {
            var game=SkyWars2.game();
            if(game!=null && target instanceof net.minecraft.world.entity.item.ItemEntity item
                    && game.items().lightningAcquisition().protectsLightning(item,bolt)) return;
            if(game==null || game.items().electro().lightningTarget(bolt,target)) original.call(target,level,bolt);
        }
    }
    @Mixin(Entity.class)
    public abstract static class Impact {
        @WrapOperation(method="thunderHit",at=@At(value="INVOKE",target=
                "Lnet/minecraft/world/entity/Entity;hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
        private boolean skywars2$lightningDamage(Entity target,ServerLevel level,DamageSource source,float amount,
                Operation<Boolean> original,@Local(argsOnly=true) LightningBolt bolt) {
            // Vanilla thunderHit loses the bolt in its DamageSource. Retain it even for
            // weather/commands so the common friendly-fire rule sees the actual entity.
            var type=LbcElectro.managedLightning(bolt) && bolt.getCause()!=null
                    ? level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(LbcDamage.LIGHTNING)
                    : source.typeHolder();
            var controller=SkyWars2.game();
            LivingEntity cause=controller==null ? null : controller.items().kfcRewards().lightningOwner(bolt);
            source=new DamageSource(type,bolt,cause==null ? bolt.getCause() : cause);
            boolean applied=original.call(target,level,source,amount);
            var game=SkyWars2.game();
            if(game!=null && bolt.getCause()!=null && target instanceof LivingEntity living)
                game.items().indirectKills().projectileIgnited(living,bolt.getCause(),true);
            return applied;
        }
    }
    @Mixin(PrimedTnt.class)
    public abstract static class Tnt {
        @Inject(method="addAdditionalSaveData",at=@At("TAIL"))
        private void skywars2$saveDropOwner(net.minecraft.world.level.storage.ValueOutput output,CallbackInfo ci) {
            var tnt=(PrimedTnt)(Object)this;
            if(me.skywars2.game.LbcTntDrops.managed(tnt) || me.skywars2.game.LbcCreeperDrops.managed(tnt) || me.skywars2.game.LbcCageDrops.managed(tnt) || me.skywars2.game.LbcZombieTntDrops.managed(tnt))
                net.minecraft.world.entity.EntityReference.store(
                        net.minecraft.world.entity.EntityReference.of(tnt.getOwner()),output,"owner");
        }
        @Inject(method="getOwner",at=@At("HEAD"),cancellable=true)
        private void skywars2$reflectedTntOwner(CallbackInfoReturnable<LivingEntity> ci) {
            var game=SkyWars2.game();
            if(game!=null) {
                LivingEntity owner=game.items().electro().reflectedOwner((Entity)(Object)this);
                if(owner==null) owner=game.items().tntDrops().retainedOwner((Entity)(Object)this);
                if(owner==null) owner=game.items().creeperDrops().owner((Entity)(Object)this);
                if(owner==null) owner=game.items().cageDrops().retainedOwner((Entity)(Object)this);
                if(owner==null) owner=game.items().zombieTntDrops().retainedOwner((Entity)(Object)this);
                if(owner!=null) ci.setReturnValue(owner);
            }
        }
    }
    @Mixin({LivingEntity.class,ServerPlayer.class})
    public abstract static class ReflectedSource {
        @ModifyVariable(method="hurtServer",at=@At("HEAD"),argsOnly=true)
        private DamageSource skywars2$reflectedDamageSource(DamageSource source) {
            var game=SkyWars2.game();
            if(game==null || source.getDirectEntity()==null) return source;
            LivingEntity owner=game.items().electro().reflectedOwner(source.getDirectEntity());
            if(owner==null) owner=game.items().cageDrops().owner(source.getDirectEntity());
            return owner==null || source.getEntity()==owner ? source : new DamageSource(source.typeHolder(),source.getDirectEntity(),owner);
        }
    }
}
