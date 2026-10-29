package me.skywars2.mixin;

import com.google.common.collect.ImmutableList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

public final class ActorCheckpointAccess {
    private ActorCheckpointAccess() { }
    @Mixin(Entity.class) public interface Body {
        @Invoker("unsetRemoved") void skywars2$revive();
        @Invoker("setLevel") void skywars2$level(Level level);
        @Accessor("passengers") void skywars2$passengers(ImmutableList<Entity> passengers);
        @Accessor("vehicle") void skywars2$vehicle(Entity vehicle);
    }
    @Mixin(Mob.class) public interface Target {
        @Accessor("target") void skywars2$target(LivingEntity target);
    }
    @Mixin(net.minecraft.server.level.ServerLevel.class) public interface LevelEntities {
        @Accessor("entityManager") net.minecraft.world.level.entity.PersistentEntitySectionManager<Entity> skywars2$manager();
    }
    @Mixin(net.minecraft.world.level.entity.PersistentEntitySectionManager.class) public interface Sections {
        @Accessor("sectionStorage") net.minecraft.world.level.entity.EntitySectionStorage<Entity> skywars2$sections();
    }
}
