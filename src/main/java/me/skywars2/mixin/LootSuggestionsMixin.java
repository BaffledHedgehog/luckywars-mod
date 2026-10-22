package me.skywars2.mixin;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.concurrent.CompletableFuture;
import me.skywars2.game.LootTestCatalog;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ResourceOrIdArgument;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ResourceOrIdArgument.class)
public abstract class LootSuggestionsMixin<T> {
    @Shadow @Final private ResourceKey<? extends Registry<T>> registryKey;

    @Inject(method = "listSuggestions", at = @At("RETURN"), cancellable = true)
    private <S> void skywars2$publicLootSuggestions(CommandContext<S> context, SuggestionsBuilder builder,
            CallbackInfoReturnable<CompletableFuture<Suggestions>> cir) {
        // Loot registries are server-only: vanilla clients request these completions from the server.
        // Filter the result here so explicit IDs, nested tables and every other resource argument still work.
        if (context.getSource() instanceof CommandSourceStack && registryKey.equals(Registries.LOOT_TABLE)) {
            cir.setReturnValue(cir.getReturnValue().thenApply(LootTestCatalog::filter));
        }
    }
}
