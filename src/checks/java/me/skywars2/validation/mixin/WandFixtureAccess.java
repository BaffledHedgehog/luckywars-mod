package me.skywars2.validation.mixin;

import java.util.ArrayDeque;
import java.util.Set;

import me.skywars2.game.items.wands.LbcBlaze;
import me.skywars2.game.items.wands.LbcBlind;
import me.skywars2.game.items.wands.LbcElectro;
import me.skywars2.game.items.wands.LbcExasperato;
import me.skywars2.game.items.wands.LbcTerra;
import me.skywars2.game.skills.clone.LbcDuplicating;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Observes actual wand producers in the test edition. */
public final class WandFixtureAccess {
    private WandFixtureAccess() { }

    @Mixin(value = LbcExasperato.class, remap = false)
    public interface Exasperato {
        @Invoker("burst") void validation$burst(ServerPlayer owner, Vec3 origin, boolean academyTrial);
        @Invoker("step") void validation$step(LbcExasperato.Charge charge, ServerPlayer owner);
        @Accessor("blasts") ArrayDeque<?> validation$blasts();
    }

    @Mixin(value = LbcBlaze.class, remap = false)
    public interface Blaze {
        @Accessor("terrain") ArrayDeque<?> validation$terrain();
    }

    @Mixin(value = LbcDuplicating.class, remap = false)
    public interface Duplicating {
        @Accessor("jobs") Set<?> validation$jobs();
    }

    @Mixin(value = LbcBlind.class, remap = false)
    public interface Blind {
        @Accessor("clouds") Set<?> validation$clouds();
    }

    @Mixin(value = LbcTerra.TerraProjectile.class, remap = false)
    public interface TerraShot {
        @Accessor("kind") LbcTerra.Kind validation$kind();
    }

    @Mixin(value = LbcElectro.class, remap = false)
    public interface Electro {
        @Accessor("liveBolts") Set<LightningBolt> validation$liveBolts();
    }
}
