package me.skywars2.game.entities.mobs;

import java.util.Collection;

import me.skywars2.mixin.entities.ActorCheckpointAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

/** The living subset of an engine-visible entity lookup, in its native order. */
public interface NativeLivingEntities {
    Collection<LivingEntity> skywars2$living();

    static Collection<LivingEntity> in(ServerLevel level) {
        var manager = ((ActorCheckpointAccess.LevelEntities) level).skywars2$manager();
        var lookup = ((ActorCheckpointAccess.Sections) manager).skywars2$visibleEntities();
        return ((NativeLivingEntities) lookup).skywars2$living();
    }
}
