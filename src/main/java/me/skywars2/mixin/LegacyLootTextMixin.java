package me.skywars2.mixin;

import java.util.function.UnaryOperator;
import me.skywars2.game.OriginalGuiDesign;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.SetNameFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** One native name/lore resolver, preserving all ordinary loot contexts and source conditions. */
@Mixin(SetNameFunction.class)
public abstract class LegacyLootTextMixin {
    @Inject(method="createResolver",at=@At("RETURN"),cancellable=true)
    private static void skywars2$sourceText(LootContext context,LootContext.EntityTarget target,
            CallbackInfoReturnable<UnaryOperator<Component>> cir){
        var entity=target==null?context.getOptional(LootContextParams.THIS_ENTITY):context.getOptional(target.contextParam());
        if(!(entity instanceof ServerPlayer player))return;
        var nativeResolver=cir.getReturnValue();
        cir.setReturnValue(line->nativeResolver.apply(OriginalGuiDesign.resolveLootText(line,player)));
    }
}
