package me.skywars2.mixin.genie;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import java.util.List;
import me.skywars2.game.genie.GenieCommandData;
import net.minecraft.server.commands.ArgProvider;
import net.minecraft.server.commands.data.BlockDataAccessor;
import net.minecraft.server.commands.data.DataAccessor;
import net.minecraft.server.commands.data.DataCommands;
import net.minecraft.server.commands.data.EntityDataAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;

public final class GenieCommandDataMixin {
    private GenieCommandDataMixin() {}

    @Mixin(DataCommands.class)
    public abstract static class Providers {
        @ModifyExpressionValue(method="<clinit>", at=@At(value="INVOKE",
                target="Lnet/minecraft/server/commands/ArgProvider;buildList(Ljava/lang/String;Ljava/util/List;)Ljava/util/List;"), require=2)
        private static List<ArgProvider<DataAccessor>> skywars2$scopedProviders(List<ArgProvider<DataAccessor>> providers) {
            return GenieCommandData.wrap(providers);
        }
    }

    @Mixin(EntityDataAccessor.class)
    public interface EntityAccess extends GenieCommandData.EntityAccess {
        @Override @Accessor("entity") Entity skywars2$genieDataEntity();
    }

    @Mixin(BlockDataAccessor.class)
    public interface BlockAccess extends GenieCommandData.BlockAccess {
        @Override @Accessor("entity") BlockEntity skywars2$genieDataBlockEntity();
    }
}
