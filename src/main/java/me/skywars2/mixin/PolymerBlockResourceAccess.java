package me.skywars2.mixin;

import eu.pb4.polymer.blocks.api.*;
import java.util.List;
import java.util.Map;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Use the installed Polymer allocator once; family reservations must share its occupied-state registry. */
@Mixin(PolymerBlockResourceUtils.class)
public interface PolymerBlockResourceAccess {
    @Accessor("CREATOR") static BlockResourceCreator skywars2$creator(){throw new AssertionError();}
    @Mixin(BlockResourceCreator.class)
    interface States {
        @Accessor("states") Map<BlockModelType,List<BlockState>> skywars2$available();
    }
}
