package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import me.skywars2.SkyWars2;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonPhaseInstance;
import net.minecraft.world.phys.*;
import java.util.*;
import java.util.function.Predicate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Change steering and self-contact only; native multipart pose, health, griefing and damage remain. */
@Mixin(EnderDragon.class)
public abstract class LustDragonMixin {
    private boolean lust(){var g=SkyWars2.game();return g!=null&&g.items().witchLust().controlled((Entity)(Object)this);}
    @WrapOperation(method="aiStep",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/boss/enderdragon/phases/DragonPhaseInstance;doServerTick(Lnet/minecraft/server/level/ServerLevel;)V"))
    private void phase(DragonPhaseInstance phase,ServerLevel level,Operation<Void> original){if(!lust())original.call(phase,level);}
    @WrapOperation(method="aiStep",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/boss/enderdragon/phases/DragonPhaseInstance;getFlyTargetLocation()Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 target(DragonPhaseInstance phase,Operation<Vec3> original){return lust()?((Entity)(Object)this).position().add(SkyWars2.game().items().witchLust().flight((EnderDragon)(Object)this).scale(64)):original.call(phase);}
    @WrapOperation(method="aiStep",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/boss/enderdragon/EnderDragon;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"))
    private void move(EnderDragon dragon,MoverType type,Vec3 motion,Operation<Void> original){
        if(lust()){motion=SkyWars2.game().items().witchLust().flight(dragon);dragon.setDeltaMovement(motion);}original.call(dragon,type,motion);}
    @WrapOperation(method="aiStep",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerLevel;getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;"))
    private List<Entity> contacts(ServerLevel level,Entity except,AABB box,Predicate<? super Entity> predicate,Operation<List<Entity>> original){
        var result=original.call(level,except,box,predicate);if(!lust())return result;
        return result.stream().filter(e->!SkyWars2.game().items().witchLust().harmless((Entity)(Object)this,e)).toList();}
}
