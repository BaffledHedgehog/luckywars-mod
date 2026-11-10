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
    @org.spongepowered.asm.mixin.injection.Inject(method="giveEffect",at=@At("HEAD"))
    private static void skywars2$nativeLegacyRange(net.minecraft.commands.CommandSourceStack source,
            java.util.Collection<? extends net.minecraft.world.entity.Entity> targets,
            net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect,Integer seconds,int amplifier,boolean particles,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Integer> ci)throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        if(me.skywars2.game.VanillaExpansionRules.enabled())return;
        if(amplifier<0)throw com.mojang.brigadier.exceptions.CommandSyntaxException.BUILT_IN_EXCEPTIONS.integerTooLow().create(amplifier,0);
        if(amplifier>255)throw com.mojang.brigadier.exceptions.CommandSyntaxException.BUILT_IN_EXCEPTIONS.integerTooHigh().create(amplifier,255);
    }
    @WrapOperation(method = "register", at = @At(value = "INVOKE", target =
            "Lcom/mojang/brigadier/arguments/IntegerArgumentType;integer(II)Lcom/mojang/brigadier/arguments/IntegerArgumentType;"))
    private static IntegerArgumentType skywars2$signedEffectArgument(int minimum, int maximum,
                                                                   Operation<IntegerArgumentType> original) {
        return minimum == 0 && maximum == 255 ? IntegerArgumentType.integer() : original.call(minimum, maximum);
    }
}
