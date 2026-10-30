package me.skywars2.mixin;

import java.util.Map;
import me.skywars2.game.LbcAcademy;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Typed checkpoint boundary; the academy's source implementation and other private owners stay separate. */
@Mixin(LbcAcademy.class)
public interface AcademyBlastCheckpointAccess {
    @Accessor("blastSources") Map<Entity,?> skywars2$blastSources();
    @Mutable @Accessor("blastSources") void skywars2$blastSources(Map<Entity,?> values);
}
