package me.skywars2.mixin;

import me.skywars2.game.LegacySlabRandom;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.providers.number.ints.ScoreboardValue;
import net.minecraft.world.level.storage.loot.providers.score.FixedScoreboardNameProvider;
import net.minecraft.world.level.storage.loot.providers.score.ScoreboardNameProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ScoreboardValue.class)
public abstract class LegacySlabValueMixin {
    @Shadow public abstract String score();
    @Shadow public abstract ScoreboardNameProvider target();
    @Inject(method="getIntUnsafe",at=@At("HEAD"),cancellable=true)
    private void skywars2$sharedSlabValue(LootContext context,CallbackInfoReturnable<Integer> result){
        if(score().equals("lbc.math")&&target() instanceof FixedScoreboardNameProvider fixed&&fixed.name().equals("#random_out"))
            result.setReturnValue(LegacySlabRandom.value(context));
    }
}
