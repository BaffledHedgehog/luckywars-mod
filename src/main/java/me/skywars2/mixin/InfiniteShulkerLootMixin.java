package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.skywars2.game.LbcInfiniteShulker;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;

/** Native key transition is the generation boundary; filled containers and null-player reads cannot reward. */
@Mixin(RandomizableContainer.class)
public interface InfiniteShulkerLootMixin {
    @WrapMethod(method = "unpackLootTable")
    default void skywars2$luckyShulkerGenerated(Player player, Operation<Void> original) {
        var container = (RandomizableContainer) this;
        var oldKey = container.getLootTable();
        original.call(player);
        if (player instanceof ServerPlayer opener && oldKey != null && container.getLootTable() == null)
            LbcInfiniteShulker.generated(container, oldKey, opener);
    }
}
