package me.skywars2.game.entities;

import java.util.List;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntitySection;

/** Public bridge for the native manager mixin; no entity state or callbacks are duplicated. */
public final class EntityVisibilitySnapshots {
    private EntityVisibilitySnapshots() { }
    public record View<E extends EntityAccess>(EntitySection<E> section,List<E> actors,boolean starting) { }
}
