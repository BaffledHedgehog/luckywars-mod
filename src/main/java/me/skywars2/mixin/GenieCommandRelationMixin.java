package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.brigadier.RedirectModifier;
import me.skywars2.game.genie.GenieCommandAccess;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.ExecuteCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Keep native execute-on subjects inside the gameplay item's command dimension. */
@Mixin(ExecuteCommand.class)
public abstract class GenieCommandRelationMixin {
    @ModifyReturnValue(method = {"expandOneToOneEntityRelation", "expandOneToManyEntityRelation"},
            at = @At("RETURN"), require = 2)
    private static RedirectModifier<CommandSourceStack> skywars2$localRelations(RedirectModifier<CommandSourceStack> next) {
        return context -> {
            var sources = next.apply(context);
            var level = GenieCommandAccess.level(context.getSource());
            if (level == null || sources.isEmpty()) return sources;
            return sources.stream().filter(source -> source.getEntity() != null && source.getEntity().level() == level).toList();
        };
    }
}
