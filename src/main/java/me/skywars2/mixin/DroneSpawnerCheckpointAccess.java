package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.skywars2.SkyWars2;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.SpawnData;
import net.minecraft.util.random.WeightedList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exact native producer runtime; entity NBT/AI remains owned by the native checkpoint. */
@Mixin(BaseSpawner.class)
public interface DroneSpawnerCheckpointAccess {
    @Accessor("spawnDelay") int skywars2$delay();
    @Accessor("spawnDelay") void skywars2$delay(int value);
    @Accessor("minSpawnDelay") int skywars2$min();
    @Accessor("minSpawnDelay") void skywars2$min(int value);
    @Accessor("maxSpawnDelay") int skywars2$max();
    @Accessor("maxSpawnDelay") void skywars2$max(int value);
    @Accessor("spawnCount") int skywars2$count();
    @Accessor("spawnCount") void skywars2$count(int value);
    @Accessor("maxNearbyEntities") int skywars2$near();
    @Accessor("maxNearbyEntities") void skywars2$near(int value);
    @Accessor("requiredPlayerRange") int skywars2$range();
    @Accessor("requiredPlayerRange") void skywars2$range(int value);
    @Accessor("spawnRange") int skywars2$spawnRange();
    @Accessor("spawnRange") void skywars2$spawnRange(int value);
    @Accessor("spin") double skywars2$spin();
    @Accessor("spin") void skywars2$spin(double value);
    @Accessor("oSpin") double skywars2$oldSpin();
    @Accessor("oSpin") void skywars2$oldSpin(double value);
    @Accessor("displayEntity") Entity skywars2$display();
    @Accessor("displayEntity") void skywars2$display(Entity value);
    @Accessor("nextSpawnData") SpawnData skywars2$next();
    @Accessor("nextSpawnData") void skywars2$next(SpawnData value);
    @Accessor("spawnPotentials") WeightedList<SpawnData> skywars2$potentials();
    @Accessor("spawnPotentials") void skywars2$potentials(WeightedList<SpawnData> value);

    @Mixin(BaseSpawner.class)
    abstract class Guard {
        @WrapMethod(method="serverTick")
        private void skywars2$droneBoundary(ServerLevel level,BlockPos pos,Operation<Void> original){
            var game=SkyWars2.game();var drones=game==null?null:game.items().droneDrops();
            var spawner=(BaseSpawner)(Object)this;
            boolean tracked=drones!=null && drones.checkpointNativeTracked(spawner);
            if(tracked)drones.checkpointNativeEnter(spawner);
            try{original.call(level,pos);}finally{if(tracked)drones.checkpointNativeExit(spawner);}
        }
    }
}
