package me.skywars2.mixin.academy;

import java.util.Map;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import me.skywars2.game.academy.LbcAcademy;

/** Typed checkpoint boundary; the academy's source implementation and other private owners stay separate. */
@Mixin(LbcAcademy.class)
public interface AcademyBlastCheckpointAccess {
    @Accessor("blastSources") Map<Entity,?> skywars2$blastSources();
    @Mutable @Accessor("blastSources") void skywars2$blastSources(Map<Entity,?> values);
}
