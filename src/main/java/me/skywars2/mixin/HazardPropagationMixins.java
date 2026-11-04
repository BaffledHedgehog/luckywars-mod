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
    public abstract static class Blast implements me.skywars2.game.NuclearTerrain.BlockEffects {
        @org.spongepowered.asm.mixin.gen.Invoker("interactWithBlocks")
        public abstract void skywars2$applyNuclearBlocks(java.util.List<BlockPos> positions);
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
                    && (source.entityTags().contains(me.skywars2.game.LbcExasperato.EXPLOSIVE)
                        || me.skywars2.game.LbcTntDrops.managed(source)
                        || me.skywars2.game.LbcDroneDrops.managed(source)
                        || source.entityTags().contains(me.skywars2.game.LbcGodHedgehog.BLAST));
            var game=SkyWars2.game();
            if(game!=null) {
                var authority=game.items().creeperDrops().owner(source);
                if(authority==null) authority=game.items().spellSources().owner(source);
                if(authority==null) authority=game.items().incomingArtifacts().owner(source);
                if(authority==null) authority=game.items().moiseiBoots().owner(source);
                if(authority==null) authority=game.items().quiver().owner(source);
                if(authority==null) authority=game.items().babylonEffects().owner(source);
                if(authority==null) authority=game.items().cageDrops().owner(source);
                if(authority==null) authority=game.items().zombieTntDrops().owner(source);
                if(authority==null) authority=game.items().droneDrops().owner(source);
                if(authority==null) authority=game.items().transformator().owner(source);
                if(authority==null) authority=game.items().godHedgehog().owner(source);
                if(authority==null) authority=game.items().hedgehog().owner(source);
                if(authority==null) authority=game.items().nukeEvent().owner(source);
                if(authority==null) authority=game.items().oreshek().owner(source);
                if(authority==null) authority=game.items().foodLifecycle().owner(source);
                if(authority==null && damageSource.getEntity()==null) authority=game.items().mobgun().owner(source);
                if(authority!=null&&!me.skywars2.game.LbcNukeEvent.managed(source)) damageSource=new net.minecraft.world.damagesource.DamageSource(
                        damageSource.typeHolder(),source,authority);
                var godCause=game.items().godHedgehog().cause(source);
                var babylonCause=game.items().babylonEffects().cause(source);
                if(babylonCause!=null)damageSource=game.items().indirectKills().captureDamage(damageSource,babylonCause);
                if(godCause!=null)damageSource=game.items().indirectKills().captureDamage(damageSource,godCause);
                var hedgehogCause=game.items().hedgehog().cause(source);
                if(hedgehogCause!=null)damageSource=game.items().indirectKills().captureDamage(damageSource,hedgehogCause);
                var nuclearCause=game.items().nukeEvent().cause(source);
                if(nuclearCause!=null)damageSource=game.items().indirectKills().captureDamage(damageSource,nuclearCause);
                var foodCause=game.items().foodLifecycle().cause(source);
                if(foodCause!=null)damageSource=game.items().indirectKills().captureDamage(damageSource,foodCause);
                var nutCause=game.items().oreshek().cause(source);
                if(nutCause!=null)damageSource=game.items().indirectKills().captureDamage(damageSource,nutCause);
                damageSource=game.items().spellSources().damage(damageSource);
            }
        }

        // Only replace block discovery for our native TNT; entity damage and block application stay native.
        @WrapMethod(method="calculateExplodedPositions")
        private java.util.List<BlockPos> skywars2$fastBlastBlocks(Operation<java.util.List<BlockPos>> original) {
            var blast=(ServerExplosion)(Object)this;
            var source=blast.getDirectSourceEntity();
            if(me.skywars2.game.NuclearTerrain.damageOnly(blast))return java.util.List.of();
            if(!skywars2$skipEmptyBlastBlocks || source.getClass()!=net.minecraft.world.entity.item.PrimedTnt.class
                    && !source.entityTags().contains(me.skywars2.game.LbcGodHedgehog.BLAST)
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
                entities.removeIf(entity->!game.items().halloweenWind().explosionTarget(source,entity));
                entities.removeIf(entity->!game.items().specialFireballs().explosionTarget(source,entity));
                entities.removeIf(entity->!game.items().helicopterEncounter().explosionTarget(source,entity));
                entities.removeIf(entity->!game.items().transformator().canAffect(source,entity));
                entities.removeIf(entity->!game.items().tntDrops().explosionTarget(source,entity));
                entities.removeIf(entity->!game.items().creeperDrops().explosionTarget(source,entity));
                entities.removeIf(entity->!game.items().inventoryHazards().explosionTarget(source,entity));
                entities.removeIf(entity->!game.items().incomingArtifacts().explosionTarget(source,entity));
                entities.removeIf(entity->!game.items().moiseiBoots().explosionTarget(source,entity));
                entities.removeIf(entity->!game.items().quiver().canAffect(source,entity));
                entities.removeIf(entity->!game.items().babylonEffects().canAffect(source,entity));
                var savedDamage=((ServerExplosion)(Object)this).getDamageSource();
                if(savedDamage!=null)entities.removeIf(entity->!me.skywars2.game.LbcBedwarsHarm.damageAllowed(entity,savedDamage));
                if(savedDamage!=null && (game.items().indirectKills().babylonCause(savedDamage)!=null
                        || game.items().ultraponos().isExplosion(savedDamage)))
                    entities.removeIf(entity->!game.items().wands().allowDamage(entity,savedDamage));
                entities.removeIf(me.skywars2.game.LbcGreed::active);
                if(me.skywars2.game.LbcNukeEvent.managed(source))
                    entities.removeIf(entity->!game.items().nukeEvent().canAffect(source,entity));
                entities.removeIf(entity->!game.items().cageDrops().canAffect(source,entity));
                entities.removeIf(entity->!game.items().zombieTntDrops().canAffect(source,entity));
                entities.removeIf(entity->!game.items().droneDrops().canAffect(source,entity));
                entities.removeIf(entity->!game.items().chaosTrapActors().canAffect(source,entity));
                entities.removeIf(entity->!game.items().godHedgehog().canAffect(source,entity));
                entities.removeIf(entity->!game.items().hedgehog().explosionTarget(source,entity)
                        || !game.items().hedgehog().explosionTarget(level,entity));
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
            var hedgehogOwner=game.items().hedgehog().owner(blast.getDirectSourceEntity());
            if(hedgehogOwner!=null)return hedgehogOwner;
            var sneezeDamage=blast.getDamageSource();
            if(sneezeDamage!=null && game.items().hedgehog().executingSneeze(blast.level())) {
                var author=me.skywars2.game.LbcIndirectKills.playerSource(sneezeDamage.getEntity());
                if(author!=null)return author;
            }
            var godOwner=game.items().godHedgehog().owner(blast.getDirectSourceEntity());
            if(godOwner!=null)return godOwner;
            var transformatorOwner=game.items().transformator().owner(blast.getDirectSourceEntity());
            if(transformatorOwner!=null) return transformatorOwner;
            var incomingOwner=game.items().incomingArtifacts().owner(blast.getDirectSourceEntity());
            if(incomingOwner!=null)return incomingOwner;
            var moiseiOwner=game.items().moiseiBoots().owner(blast.getDirectSourceEntity());
            if(moiseiOwner!=null)return moiseiOwner;
            var quiverOwner=game.items().quiver().owner(blast.getDirectSourceEntity());
            if(quiverOwner!=null)return quiverOwner;
            var babylonOwner=game.items().babylonEffects().owner(blast.getDirectSourceEntity());
            if(babylonOwner!=null)return babylonOwner;
            var explicitOwner=game.items().babylonEffects().explicitOwner(blast.getDamageSource());
            if(explicitOwner!=null)return explicitOwner;
            var creeperOwner=game.items().creeperDrops().owner(blast.getDirectSourceEntity());
            if(creeperOwner!=null) return creeperOwner;
            var zombieOwner=game.items().zombieTntDrops().owner(blast.getDirectSourceEntity());
            if(zombieOwner!=null) return zombieOwner;
            var droneOwner=game.items().droneDrops().owner(blast.getDirectSourceEntity());
            if(droneOwner!=null) return droneOwner;
            var cageOwner=game.items().cageDrops().owner(blast.getDirectSourceEntity());
            if(cageOwner!=null) return cageOwner;
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
            if(game!=null && !game.items().transformator().allowExplosion(
                    blast.getDirectSourceEntity(),blast.level(),blast.center(),blast.radius())) return 0;
            if(game==null) return original.call();
            var babylonCause=game.items().babylonEffects().cause(blast.getDirectSourceEntity());
            if(babylonCause==null)babylonCause=game.items().quiver().cause(blast.getDirectSourceEntity());
            var godCause=game.items().godHedgehog().cause(blast.getDirectSourceEntity());
            var savedCause=babylonCause!=null?babylonCause:godCause!=null?godCause
                    :blast.getDamageSource()==null?null:game.items().indirectKills().babylonCause(blast.getDamageSource());
            if(savedCause!=null) {
                int[] inheritedResult={0};
                game.items().indirectKills().trapChanges(blast.level(),savedCause,()->inheritedResult[0]=original.call());
                return inheritedResult[0];
            }
            if(blast.getIndirectSourceEntity()==null) return original.call();
            int[] result={0};
            var direct=blast.getDirectSourceEntity();
            // Keep the indirect player as author, and the actual trap as the source of WILD permission.
            boolean wild=game.items().cageDrops().wild(direct) || game.items().zombieTntDrops().wild(direct)
                    || game.items().droneDrops().wild(direct) || game.items().creeperDrops().wild(direct)
                    || me.skywars2.game.LbcTransformator.managed(direct);
            if(wild) game.items().indirectKills().worldChanges(blast.level(),blast.getIndirectSourceEntity(),true,()->result[0]=original.call());
            else game.items().indirectKills().worldChanges(blast.level(),blast.getIndirectSourceEntity(),()->result[0]=original.call());
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
            if(game!=null) {
                game.items().tntDrops().inheritChain(source,spawned);
                game.items().inventoryHazards().inheritChild(source,spawned);
                game.items().incomingArtifacts().inheritChild(source,spawned);
                game.items().spellSources().inherit(source,spawned);
                game.items().nukeEvent().inherit(source,spawned);
                game.items().nukeEvent().inheritChild(spawned);
                game.items().oreshek().inherit(source,spawned);
                game.items().oreshek().inheritChild(spawned);
                game.items().foodLifecycle().inheritConversion(source,spawned);
                game.items().foodLifecycle().inheritChild(spawned);
                game.items().chaosTrapActors().inheritProjectile(source,spawned);
                game.items().moiseiBoots().inheritChild(source,spawned);
                game.items().quiver().inheritChild(source,spawned);
                game.items().babylonEffects().inheritChild(source,spawned);
                if(source==null&&explosion instanceof ServerExplosion blast) {
                    if(game.items().ultraponos().isExplosion(blast.getDamageSource()))
                        game.items().ultraponos().inheritExplosionChild(blast.getDamageSource(),spawned);
                    else game.items().babylonEffects().inheritExplosionChild(blast.getDamageSource(),spawned);
                }
                game.items().creeperDrops().inheritChild(source,spawned);
                game.items().cageDrops().inheritChild(source,spawned);
                game.items().zombieTntDrops().inheritChild(source,spawned);
                game.items().droneDrops().inheritChild(source,spawned);
                game.items().transformator().inheritChild(source,spawned);
                game.items().godHedgehog().inheritChild(source,spawned);
                game.items().hedgehog().inheritSneezeChild(source,spawned);
                game.items().hedgehog().inheritSneezeChild(level,spawned);
            }
            if(game!=null && source!=null && source.entityTags().contains(me.skywars2.game.LbcExasperato.EXPLOSIVE)) {
                spawned.entityTags().add(me.skywars2.game.LbcExasperato.EXPLOSIVE);
                spawned.entityTags().add(game.items().epoch());
                game.items().academy().inheritBlast(source, spawned);
                game.items().exoperso().inheritBlast(source, spawned);
            }
            return original.call(level,spawned);
        }
    }
    /** Native creeper clouds otherwise lose all causal ownership. */
    @Mixin(net.minecraft.world.entity.monster.Creeper.class)
    public abstract static class CreeperCloud {
        @WrapOperation(method="spawnLingeringCloud",at=@At(value="INVOKE",target=
                "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
        private boolean skywars2$creeperCloud(net.minecraft.world.level.Level level,Entity child,Operation<Boolean> original) {
            var game=SkyWars2.game();
            if(game!=null) game.items().creeperDrops().inheritChild((Entity)(Object)this,child);
            return original.call(level,child);
        }
    }
    /** A removed thrower can still own its already-created potion cloud. */
    @Mixin(net.minecraft.world.entity.AreaEffectCloud.class)
    public abstract static class CreeperCloudOwner {
        @org.spongepowered.asm.mixin.injection.Inject(method="getOwner",at=@At("HEAD"),cancellable=true)
        private void skywars2$cloudOwner(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<net.minecraft.world.entity.LivingEntity> ci) {
            var game=SkyWars2.game();
            if(game!=null) {
                var owner=game.items().creeperDrops().owner((Entity)(Object)this);
                if(owner!=null) ci.setReturnValue(owner);
            }
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
