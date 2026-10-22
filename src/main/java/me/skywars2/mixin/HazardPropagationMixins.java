package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.LavaFluid;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Mixin;

/** Только нативные тики опасных блоков наследуют источник; обычный setBlock его не получает. */
public final class HazardPropagationMixins {
    @Mixin(ServerExplosion.class)
    public abstract static class Blast {
        @org.spongepowered.asm.mixin.Shadow @org.spongepowered.asm.mixin.Final
        private net.minecraft.world.level.ExplosionDamageCalculator damageCalculator;
        @org.spongepowered.asm.mixin.Shadow @org.spongepowered.asm.mixin.Final @org.spongepowered.asm.mixin.Mutable
        private net.minecraft.world.damagesource.DamageSource damageSource;
        @org.spongepowered.asm.mixin.Unique
        private boolean skywars2$skipEmptyBlastBlocks;
        @org.spongepowered.asm.mixin.injection.Inject(method="<init>",at=@At("TAIL"))
        private void skywars2$identifyWandBlast(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
            var source=((ServerExplosion)(Object)this).getDirectSourceEntity();
            skywars2$skipEmptyBlastBlocks=source instanceof net.minecraft.world.entity.item.PrimedTnt
                    && source.entityTags().contains(me.skywars2.game.LbcExasperato.EXPLOSIVE);
            var game=SkyWars2.game();
            if(game!=null && damageSource.getEntity()==null) {
                var authority=game.items().mobgun().owner(source);
                if(authority!=null) damageSource=new net.minecraft.world.damagesource.DamageSource(
                        damageSource.typeHolder(),source,authority);
            }
        }

        // Only replace block discovery for our native TNT; entity damage and block application stay native.
        @WrapMethod(method="calculateExplodedPositions")
        private java.util.List<BlockPos> skywars2$fastBlastBlocks(Operation<java.util.List<BlockPos>> original) {
            var blast=(ServerExplosion)(Object)this;
            var source=blast.getDirectSourceEntity();
            if(!skywars2$skipEmptyBlastBlocks || source.getClass()!=net.minecraft.world.entity.item.PrimedTnt.class
                    || damageCalculator.getClass()!=net.minecraft.world.level.EntityBasedExplosionDamageCalculator.class
                    || Boolean.getBoolean("skywars2.disableExplosionCache")) return original.call();
            return me.skywars2.game.LbcExasperato.explosionBlocks(blast,damageCalculator);
        }

        // Наш TNT не поджигает воздух: пропускаем бесполезные set/shuffle/onExplosionHit для пустых позиций.
        @WrapOperation(method="calculateExplodedPositions",at=@At(value="INVOKE",target=
                "Lnet/minecraft/world/level/ExplosionDamageCalculator;shouldBlockExplode(Lnet/minecraft/world/level/Explosion;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;F)Z"))
        private boolean skywars2$skipBlastAir(net.minecraft.world.level.ExplosionDamageCalculator calculator,
                net.minecraft.world.level.Explosion explosion,net.minecraft.world.level.BlockGetter world,
                BlockPos pos,BlockState state,float power,Operation<Boolean> original) {
            return !(skywars2$skipEmptyBlastBlocks && state.isAir())
                    && original.call(calculator,explosion,world,pos,state,power);
        }

        // Отбор до нативного цикла исключает и урон, и knockback; общая атрибуция блоков остаётся ниже.
        @WrapOperation(method="hurtEntities",at=@At(value="INVOKE",target=
                "Lnet/minecraft/server/level/ServerLevel;getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"))
        private java.util.List<Entity> skywars2$wandExplosionTargets(ServerLevel level,Entity except,AABB area,
                Operation<java.util.List<Entity>> original) {
            var entities=original.call(level,except,area); var game=SkyWars2.game();
            if(game!=null) {
                var source=((ServerExplosion)(Object)this).getDirectSourceEntity();
                if(source!=null && source.entityTags().contains(me.skywars2.game.LbcExasperato.EXPLOSIVE))
                    entities.removeIf(entity->!game.items().exasperato().explosionTarget(source,entity));
                else if(source instanceof net.minecraft.world.entity.projectile.Projectile projectile
                        && game.items().wands().isFireProjectile(source))
                    entities.removeIf(entity->!game.items().wands().canHit(projectile,entity));
                else {
                    var authority=game.items().mobgun().owner(source);
                    if(authority!=null) {
                        var nativeAttacker=me.skywars2.game.LbcIndirectKills.playerSource(
                                ((ServerExplosion)(Object)this).getIndirectSourceEntity());
                        var owner=nativeAttacker!=null?nativeAttacker:authority;
                        entities.removeIf(entity->entity!=owner && !game.items().wands().projectileTarget(owner,entity));
                    }
                }
            }
            return entities;
        }
        /** Native crystal/cart explosions have no indirect source; preserve their scoped Mobgun author. */
        @WrapMethod(method="getIndirectSourceEntity")
        private net.minecraft.world.entity.LivingEntity skywars2$mobgunBlastOwner(
                Operation<net.minecraft.world.entity.LivingEntity> original) {
            var nativeSource=original.call();
            var game=SkyWars2.game();
            if(game==null) return nativeSource;
            var blast=(ServerExplosion)(Object)this;
            var authority=game.items().mobgun().owner(blast.getDirectSourceEntity());
            if(authority==null) return nativeSource;
            // The constructor calls this while its DamageSource field is still null.
            var damage=blast.getDamageSource();
            var explicit=damage==null?null:me.skywars2.game.LbcIndirectKills.playerSource(damage.getEntity());
            if(explicit!=null) return explicit;
            var nativeOwner=me.skywars2.game.LbcIndirectKills.playerSource(nativeSource);
            return nativeOwner!=null?nativeOwner:authority;
        }
        @WrapMethod(method="explode")
        private int skywars2$blastOwner(Operation<Integer> original) {
            var game=SkyWars2.game();
            var blast=(ServerExplosion)(Object)this;
            if(game==null || blast.getIndirectSourceEntity()==null) return original.call();
            int[] result={0};
            game.items().indirectKills().worldChanges(blast.level(),blast.getIndirectSourceEntity(),()->result[0]=original.call());
            return result[0];
        }
    }
    /** Взрыв блока TNT сохраняет источник и политику защиты цепочки жезла. */
    @Mixin(net.minecraft.world.level.block.TntBlock.class)
    public abstract static class ChainTnt {
        @WrapOperation(method="wasExploded",at=@At(value="INVOKE",target=
                "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
        private boolean skywars2$wandChain(ServerLevel level,Entity spawned,Operation<Boolean> original,
                @Local(argsOnly=true) net.minecraft.world.level.Explosion explosion) {
            var source=explosion.getDirectSourceEntity(); var game=SkyWars2.game();
            if(game!=null && source!=null && source.entityTags().contains(me.skywars2.game.LbcExasperato.EXPLOSIVE)) {
                spawned.entityTags().add(me.skywars2.game.LbcExasperato.EXPLOSIVE);
                spawned.entityTags().add(game.items().epoch());
                game.items().academy().inheritBlast(source, spawned);
                game.items().exoperso().inheritBlast(source, spawned);
            }
            return original.call(level,spawned);
        }
    }
    @Mixin(FireBlock.class)
    public abstract static class Fire {
        @WrapMethod(method="tick")
        private void skywars2$fireOwner(BlockState state,ServerLevel level,BlockPos pos,RandomSource random,Operation<Void> original) {
            var game=SkyWars2.game();
            if(game==null) original.call(state,level,pos,random);
            else game.items().indirectKills().hazardTick(level,pos,()->original.call(state,level,pos,random));
        }
    }
    @Mixin(FlowingFluid.class)
    public abstract static class Fluid {
        @WrapMethod(method="tick")
        private void skywars2$fluidOwner(ServerLevel level,BlockPos pos,BlockState state,FluidState fluid,Operation<Void> original) {
            var game=SkyWars2.game();
            if(game==null) original.call(level,pos,state,fluid);
            else game.items().indirectKills().hazardTick(level,pos,()->original.call(level,pos,state,fluid));
        }
    }
    @Mixin(LavaFluid.class)
    public abstract static class Lava {
        @WrapMethod(method="randomTick")
        private void skywars2$lavaFireOwner(ServerLevel level,BlockPos pos,FluidState fluid,RandomSource random,Operation<Void> original) {
            var game=SkyWars2.game();
            if(game==null) original.call(level,pos,fluid,random);
            else game.items().indirectKills().hazardTick(level,pos,()->original.call(level,pos,fluid,random));
        }
    }
}
