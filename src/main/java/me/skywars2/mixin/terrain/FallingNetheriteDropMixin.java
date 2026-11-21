package me.skywars2.mixin.terrain;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.skywars2.SkyWars2;
import net.minecraft.world.entity.item.FallingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import me.skywars2.game.GameController;
import me.skywars2.game.items.drops.LbcFallingNetheriteDrops;

/** Native collision and landing remain authoritative; no scheduled estimate of flight time. */
@Mixin(FallingBlockEntity.class)
public abstract class FallingNetheriteDropMixin {
    @WrapMethod(method="tick")
    private void skywars2$netheriteLanding(Operation<Void> original) {
        var game=SkyWars2.game();
        var actor=(FallingBlockEntity)(Object)this;
        if(game!=null && me.skywars2.game.items.food.LbcFoodLifecycle.managed(actor)){
            game.items().foodLifecycle().withTick(actor,()->original.call());return;
        }
        if(game!=null && me.skywars2.game.items.drops.LbcOpAnvilDrop.managed(actor)) {
            game.items().opAnvilDrops().withTick(actor,()->original.call());
            return;
        }
        if(game==null || !me.skywars2.game.items.drops.LbcFallingNetheriteDrops.managed(actor)) {
            original.call();
            return;
        }
        var service=game.items().fallingNetheriteDrops();
        service.withTick(actor,()->original.call());
    }
}
