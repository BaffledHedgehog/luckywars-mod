package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.List;
import me.skywars2.SkyWars2;
import me.skywars2.game.LbcExplosionSuppression;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ExplosionParticleInfo;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** TECH suppresses native blast publication, terrain, fire and all entity impulses. */
public final class TechExplosionMixin {
    private TechExplosionMixin(){}
    @Mixin(value=ServerLevel.class,priority=6000)
    public abstract static class LevelExplosion {
        @WrapMethod(method="explode(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/level/ExplosionDamageCalculator;DDDFZLnet/minecraft/world/level/Level$ExplosionInteraction;Lnet/minecraft/core/particles/ParticleOptions;Lnet/minecraft/core/particles/ParticleOptions;Lnet/minecraft/util/random/WeightedList;Lnet/minecraft/core/Holder;)V")
        private void skywars2$creeperPresentation(Entity source,DamageSource damage,ExplosionDamageCalculator calculator,
                double x,double y,double z,float radius,boolean fire,Level.ExplosionInteraction interaction,
                ParticleOptions small,ParticleOptions large,WeightedList<ExplosionParticleInfo> blocks,Holder<SoundEvent> sound,Operation<Void> original){
            // MOB is the legacy creeper blast path. Preserve its single central flash;
            // 26.3 otherwise adds emitter bursts and a separate cloud of block fragments.
            boolean creeper=interaction==Level.ExplosionInteraction.MOB;
            original.call(source,damage,calculator,x,y,z,radius,fire,interaction,small,
                    creeper?small:large,creeper?WeightedList.of():blocks,sound);
        }
        @Inject(method="explode(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/level/ExplosionDamageCalculator;DDDFZLnet/minecraft/world/level/Level$ExplosionInteraction;Lnet/minecraft/core/particles/ParticleOptions;Lnet/minecraft/core/particles/ParticleOptions;Lnet/minecraft/util/random/WeightedList;Lnet/minecraft/core/Holder;)V",at=@At("HEAD"),cancellable=true)
        private void skywars2$techBlastPublication(Entity source,DamageSource damage,ExplosionDamageCalculator calculator,
                double x,double y,double z,float radius,boolean fire,Level.ExplosionInteraction interaction,
                ParticleOptions small,ParticleOptions large,WeightedList<ExplosionParticleInfo> blocks,Holder<SoundEvent> sound,CallbackInfo ci){
            if(LbcExplosionSuppression.suppressed(SkyWars2.game(),(ServerLevel)(Object)this,new Vec3(x,y,z)))ci.cancel();
        }
    }
    @Mixin(value=ServerExplosion.class,priority=6000)
    public abstract static class Blast {
        @Unique private LbcExplosionSuppression.Coverage skywars2$techCoverage;
        @Unique private LbcExplosionSuppression.Coverage skywars2$coverage(){
            return skywars2$techCoverage!=null?skywars2$techCoverage:LbcExplosionSuppression.coverage(SkyWars2.game(),((ServerExplosion)(Object)this).level());
        }
        @WrapMethod(method="explode")
        private int skywars2$techBlast(Operation<Integer> original){
            var blast=(ServerExplosion)(Object)this;
            var coverage=LbcExplosionSuppression.coverage(SkyWars2.game(),blast.level());
            if(coverage.contains(blast.center()))return 0;
            var previous=skywars2$techCoverage;skywars2$techCoverage=coverage;
            try{return original.call();}finally{skywars2$techCoverage=previous;}
        }
        // Filter at the caller, after any existing native/TNT-specific calculate wrapper returns.
        @ModifyExpressionValue(method="explode",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/ServerExplosion;calculateExplodedPositions()Ljava/util/List;"))
        private List<BlockPos> skywars2$techExplodedPositions(List<BlockPos> positions){return skywars2$coverage().filterBlocks(positions);}
        @WrapOperation(method="hurtEntities",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerLevel;getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"))
        private List<Entity> skywars2$techBlastTargets(ServerLevel level,Entity except,AABB area,Operation<List<Entity>> original){
            return skywars2$coverage().filterTargets(original.call(level,except,area));
        }
        // Reviewed custom blast owners invoke these native block methods directly as well.
        @WrapMethod(method="interactWithBlocks")
        private void skywars2$techBlockInteraction(List<BlockPos> positions,Operation<Void> original){original.call(skywars2$techCoverage!=null?positions:skywars2$coverage().filterBlocks(positions));}
        @WrapMethod(method="createFire")
        private void skywars2$techBlastFire(List<BlockPos> positions,Operation<Void> original){original.call(skywars2$techCoverage!=null?positions:skywars2$coverage().filterBlocks(positions));}
    }
    @Mixin(value=FireworkRocketEntity.class,priority=6000)
    public abstract static class Firework {
        @Unique private LbcExplosionSuppression.Coverage skywars2$techFireworkCoverage;
        @WrapMethod(method="explode")
        private void skywars2$techFirework(ServerLevel level,Operation<Void> original){
            var rocket=(FireworkRocketEntity)(Object)this;var coverage=LbcExplosionSuppression.coverage(SkyWars2.game(),level);
            if(coverage.contains(rocket.position())){rocket.discard();return;}
            var previous=skywars2$techFireworkCoverage;skywars2$techFireworkCoverage=coverage;
            try{original.call(level);}finally{skywars2$techFireworkCoverage=previous;}
        }
        @WrapMethod(method="dealExplosionDamage")
        private void skywars2$techFireworkDamage(ServerLevel level,Operation<Void> original){
            var coverage=skywars2$techFireworkCoverage!=null?skywars2$techFireworkCoverage:LbcExplosionSuppression.coverage(SkyWars2.game(),level);
            if(coverage.contains(((Entity)(Object)this).position()))return;
            var previous=skywars2$techFireworkCoverage;skywars2$techFireworkCoverage=coverage;
            try{original.call(level);}finally{skywars2$techFireworkCoverage=previous;}
        }
        @WrapOperation(method="dealExplosionDamage",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
        private boolean skywars2$techFireworkTarget(LivingEntity target,ServerLevel level,DamageSource damage,float amount,Operation<Boolean> original){
            var coverage=skywars2$techFireworkCoverage;
            return (coverage==null||!coverage.targets(target))&&original.call(target,level,damage,amount);
        }
    }
}
