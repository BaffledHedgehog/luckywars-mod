package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.skywars2.SkyWars2;
import net.minecraft.world.entity.item.FallingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;

/** Native collision and landing remain authoritative; no scheduled estimate of flight time. */
@Mixin(FallingBlockEntity.class)
public abstract class FallingNetheriteDropMixin {
    @WrapMethod(method="tick")
    private void skywars2$netheriteLanding(Operation<Void> original) {
        var game=SkyWars2.game();
        var actor=(FallingBlockEntity)(Object)this;
        if(game==null || !me.skywars2.game.LbcFallingNetheriteDrops.managed(actor)) {
            original.call();
            return;
        }
        var service=game.items().fallingNetheriteDrops();
        service.withTick(actor,()->original.call());
        // Successful native placement discards the entity synchronously, before this callback.
        service.afterTick(actor);
    }
}
