package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import me.skywars2.game.LbcAcademy;
import me.skywars2.game.MatchAcademyCheckpoint;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Completed building metadata only; unfinished native structure placement must drain. */
@Mixin(LbcAcademy.class)
public interface AcademyCheckpointAccess {
    @Accessor("drops") Set<ItemEntity> skywars2$drops();
    @Accessor("entities") Set<Entity> skywars2$entities();
    @Accessor("lastInteractions") Map<UUID,Long> skywars2$interactions();
    @Accessor("singleUse") Map<UUID,Object> skywars2$singleUse();
    @Accessor("panelOwners") Map<GlobalPos,Object> skywars2$panelOwners();
    @Accessor("pendingWhite") Set<GlobalPos> skywars2$pendingWhite();
    @Accessor("wasStopped") boolean skywars2$wasStopped();
    @Accessor("wasStopped") void skywars2$wasStopped(boolean value);

    @Mixin(targets="me.skywars2.game.LbcAcademy$Placement")
    interface Placement {
        @Accessor("level") ServerLevel skywars2$level();
        @Accessor("spent") boolean skywars2$spent();
        @Accessor("spent") void skywars2$spent(boolean value);
        @Accessor("row") int skywars2$row();
        @Accessor("row") void skywars2$row(int value);
        @Accessor("spawned") Set<Entity> skywars2$spawned();
        @Accessor("panels") Set<GlobalPos> skywars2$panels();
    }

    @Mixin(LbcAcademy.class)
    abstract class Guard {
        @WrapMethod(method="tick")
        private void skywars2$tick(Operation<Void> original){
            var academy=(LbcAcademy)(Object)this;if(MatchAcademyCheckpoint.frozen(academy))return;
            MatchAcademyCheckpoint.enter(academy);try{original.call();}finally{MatchAcademyCheckpoint.exit(academy);}
        }
        @WrapMethod(method="interact")
        private boolean skywars2$interact(ServerPlayer player,InteractionHand hand,Entity target,Operation<Boolean> original){
            var academy=(LbcAcademy)(Object)this;if(MatchAcademyCheckpoint.frozen(academy))return false;
            MatchAcademyCheckpoint.enter(academy);try{return original.call(player,hand,target);}finally{MatchAcademyCheckpoint.exit(academy);}
        }
        @WrapMethod(method="chunkLoaded")
        private void skywars2$chunk(LevelChunk chunk,Operation<Void> original){
            var academy=(LbcAcademy)(Object)this;if(MatchAcademyCheckpoint.frozen(academy))return;
            MatchAcademyCheckpoint.enter(academy);try{original.call(chunk);}finally{MatchAcademyCheckpoint.exit(academy);}
        }
        @WrapMethod(method="loaded")
        private boolean skywars2$loaded(Entity entity,Operation<Boolean> original){
            var academy=(LbcAcademy)(Object)this;if(MatchAcademyCheckpoint.frozen(academy))return false;
            MatchAcademyCheckpoint.enter(academy);try{return original.call(entity);}finally{MatchAcademyCheckpoint.exit(academy);}
        }
        @WrapMethod(method="unloaded")
        private void skywars2$unloaded(Entity entity,Operation<Void> original){
            var academy=(LbcAcademy)(Object)this;if(MatchAcademyCheckpoint.frozen(academy))return;
            MatchAcademyCheckpoint.enter(academy);try{original.call(entity);}finally{MatchAcademyCheckpoint.exit(academy);}
        }
    }
}
