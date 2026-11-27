package me.skywars2.mixin.entities;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.ArrayDeque;
import java.util.stream.Stream;
import me.skywars2.game.entities.EntityVisibilitySnapshots;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.level.entity.Visibility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/** Visibility callbacks may synchronously add/remove actors, including siblings in the same section. */
@Mixin(PersistentEntitySectionManager.class)
public abstract class EntitySectionVisibilityMixin<T extends EntityAccess> {
    @Unique private final ThreadLocal<ArrayDeque<EntityVisibilitySnapshots.View<T>>> skywars2$visibilityViews=ThreadLocal.withInitial(ArrayDeque::new);

    @WrapOperation(method="updateChunkStatus(Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/world/level/entity/Visibility;)V",
            at=@At(value="INVOKE",target="Lnet/minecraft/world/level/entity/EntitySectionStorage;getExistingSectionsInChunk(J)Ljava/util/stream/Stream;"))
    private Stream<EntitySection<T>> skywars2$stableSections(EntitySectionStorage<T> storage,long chunk,Operation<Stream<EntitySection<T>>> original){
        return original.call(storage,chunk).toList().stream();
    }

    // This is the pinned 26.3 callback, also named in both crash reports. Cache once for ALL phases:
    // a stop-ticking callback may discard a sibling that still needs its stop-tracking callback.
    @WrapMethod(method="lambda$updateChunkStatus$0")
    private void skywars2$visibilityView(Visibility next,EntitySection<T> section,Operation<Void> original){
        var previous=section.getStatus();
        boolean starting=next.isAccessible()&&!previous.isAccessible()||next.isTicking()&&!previous.isTicking();
        var stack=skywars2$visibilityViews.get();
        stack.push(new EntityVisibilitySnapshots.View<>(section,section.getEntities().toList(),starting));
        try{original.call(next,section);}
        finally{stack.pop();if(stack.isEmpty())skywars2$visibilityViews.remove();}
    }

    @WrapOperation(method="lambda$updateChunkStatus$0",at=@At(value="INVOKE",
            target="Lnet/minecraft/world/level/entity/EntitySection;getEntities()Ljava/util/stream/Stream;"))
    private Stream<T> skywars2$stableActors(EntitySection<T> section,Operation<Stream<T>> original){
        var view=skywars2$visibilityViews.get().peek();
        if(view==null||view.section()!=section)return original.call(section);
        var actors=view.actors().stream();
        // Ending callbacks must finish unregistering removed siblings. Starting callbacks must
        // never resurrect their tracking/ticking after an earlier callback has discarded them.
        return view.starting()?actors.filter(actor->!(actor instanceof Entity entity)||!entity.isRemoved()):actors;
    }
}
