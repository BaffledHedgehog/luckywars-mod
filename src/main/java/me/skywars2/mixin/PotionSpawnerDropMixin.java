package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.skywars2.SkyWars2;
import me.skywars2.game.LbcPotionSpawnerDrops;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Native producer lifecycle only; unowned spawners retain vanilla behavior. */
public final class PotionSpawnerDropMixin {
    private PotionSpawnerDropMixin() { }

    @Mixin(SpawnerBlockEntity.class)
    public abstract static class State implements LbcPotionSpawnerDrops.StateAccess {
        @Unique private CompoundTag skywars2$potionData=new CompoundTag();
        @Override public CompoundTag skywars2$potionMetadata() { return skywars2$potionData; }

        @Inject(method="loadAdditional",at=@At("TAIL"))
        private void skywars2$loadProducer(ValueInput input,CallbackInfo ci) {
            skywars2$potionData=input.read(LbcPotionSpawnerDrops.DATA,CompoundTag.CODEC).orElseGet(CompoundTag::new);
            var game=SkyWars2.game();if(game!=null)game.items().potionSpawnerDrops().loadedSpawner((SpawnerBlockEntity)(Object)this);
        }
        @Inject(method="saveAdditional",at=@At("TAIL"))
        private void skywars2$saveProducer(ValueOutput output,CallbackInfo ci) {
            if(skywars2$potionData.contains("epoch"))output.store(LbcPotionSpawnerDrops.DATA,CompoundTag.CODEC,skywars2$potionData);
        }
        @Inject(method="serverTick",at=@At("HEAD"),cancellable=true)
        private static void skywars2$producerTick(Level level,BlockPos pos,BlockState state,SpawnerBlockEntity entity,CallbackInfo ci) {
            var game=SkyWars2.game();if(game!=null && !game.items().potionSpawnerDrops().canTick(entity))ci.cancel();
        }
    }

    @Mixin(BlockEntity.class)
    public abstract static class Lifecycle {
        @Inject(method="setRemoved",at=@At("HEAD"))
        private void skywars2$unloadedProducer(CallbackInfo ci) {
            if((Object)this instanceof SpawnerBlockEntity spawner) {
                var game=SkyWars2.game();if(game!=null)game.items().potionSpawnerDrops().unloadedSpawner(spawner);
            }
        }
    }

    @Mixin(LevelChunk.class)
    public abstract static class ChunkLifecycle {
        // setLevel and clearRemoved occur before the chunk's BE map insertion. Register after it.
        @Inject(method="setBlockEntity",at=@At("TAIL"))
        private void skywars2$loadedProducer(BlockEntity entity,CallbackInfo ci) {
            if(entity instanceof SpawnerBlockEntity spawner) {
                var game=SkyWars2.game();if(game!=null)game.items().potionSpawnerDrops().loadedSpawner(spawner);
            }
        }
    }

    @Mixin(BaseSpawner.class)
    public abstract static class SpawnCandidates {
        @WrapOperation(method="serverTick",at=@At(value="INVOKE",target=
                "Lnet/minecraft/server/level/ServerLevel;noCollision(Lnet/minecraft/world/phys/AABB;)Z"))
        private boolean skywars2$residentCandidate(ServerLevel level,AABB bounds,Operation<Boolean> original) {
            var game=SkyWars2.game();
            return (game==null || game.items().potionSpawnerDrops().canSpawnAt((BaseSpawner)(Object)this,level,bounds))
                    && original.call(level,bounds);
        }
    }
}
