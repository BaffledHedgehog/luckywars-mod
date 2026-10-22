package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Preserve native reinforcement choice/finalization, attaching only causal lifecycle identity. */
public final class AntDropMixin {
    private AntDropMixin(){ }

    @Mixin(Zombie.class)
    public abstract static class Reinforcements {
        @WrapOperation(method="hurtServer",at=@At(value="INVOKE",target=
                "Lnet/minecraft/world/entity/monster/zombie/Zombie;finalizeSpawn(Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/world/DifficultyInstance;Lnet/minecraft/world/entity/EntitySpawnReason;Lnet/minecraft/world/entity/SpawnGroupData;)Lnet/minecraft/world/entity/SpawnGroupData;"))
        private SpawnGroupData skywars2$nativeChild(Zombie child,ServerLevelAccessor level,DifficultyInstance difficulty,
                EntitySpawnReason reason,SpawnGroupData group,Operation<SpawnGroupData> original){
            var game=SkyWars2.game();
            if(game!=null)game.items().antDrops().inheritReinforcement((Entity)(Object)this,child);
            return original.call(child,level,difficulty,reason,group);
        }

        @WrapOperation(method="finalizeSpawn",at=@At(value="INVOKE",target=
                "Lnet/minecraft/world/level/ServerLevelAccessor;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
        private boolean skywars2$newJockey(ServerLevelAccessor level,Entity chicken,Operation<Boolean> original){
            var game=SkyWars2.game();
            if(game!=null)game.items().antDrops().inheritJockey((Entity)(Object)this,chicken);
            return original.call(level,chicken);
        }

        @WrapOperation(method="hurtServer",at=@At(value="INVOKE",target=
                "Lnet/minecraft/world/entity/SpawnPlacements;isSpawnPositionOk(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)Z"))
        private boolean skywars2$residentCandidate(EntityType<?> type,LevelReader level,BlockPos pos,Operation<Boolean> original){
            var game=SkyWars2.game();
            return (game==null || game.items().antDrops().residentReinforcement((Entity)(Object)this,level,pos))
                    && original.call(type,level,pos);
        }
    }
}
