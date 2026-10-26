package me.skywars2.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.server.commands.EffectCommands;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Widen both finite/infinite amplifier arguments, retaining native duration syntax. */
@Mixin(EffectCommands.class)
public abstract class EffectCommandLevelsMixin {
    @WrapOperation(method = "register", at = @At(value = "INVOKE", target =
            "Lcom/mojang/brigadier/arguments/IntegerArgumentType;integer(II)Lcom/mojang/brigadier/arguments/IntegerArgumentType;"))
    private static IntegerArgumentType skywars2$signedEffectArgument(int minimum, int maximum,
                                                                   Operation<IntegerArgumentType> original) {
        return minimum == 0 && maximum == 255 ? IntegerArgumentType.integer() : original.call(minimum, maximum);
    }
}
