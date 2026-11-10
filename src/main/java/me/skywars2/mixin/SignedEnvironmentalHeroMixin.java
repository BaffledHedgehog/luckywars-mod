package me.skywars2.mixin;

import me.skywars2.game.VanillaExpansionRules;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import me.skywars2.game.SignedEnvironmentalEffects;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Villager.class)
public abstract class SignedEnvironmentalHeroMixin {
    @WrapOperation(method="updateSpecialPrices",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/player/Player;getEffect(Lnet/minecraft/core/Holder;)Lnet/minecraft/world/effect/MobEffectInstance;"))
    private MobEffectInstance skywars2$handleSignedHero(Player player,Holder<MobEffect> effect,Operation<MobEffectInstance> original) {
        if(!VanillaExpansionRules.enabled()){return original.call(player,effect);}
        return null; // Replaced below without overflowing native int special prices.
    }
    @Inject(method="updateSpecialPrices",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/player/Player;getEffect(Lnet/minecraft/core/Holder;)Lnet/minecraft/world/effect/MobEffectInstance;"))
    private void skywars2$signedPrices(Player player,CallbackInfo ci) {
        if(!VanillaExpansionRules.enabled()){return;}
        long l=SignedEnvironmentalEffects.level(player,MobEffects.HERO_OF_THE_VILLAGE);if(l==0)return;
        for(var offer:((Villager)(Object)this).getOffers()) {
            int base=offer.getBaseCostA().getCount(),max=offer.getBaseCostA().getMaxStackSize();
            double fraction=.3+.0625*(Math.abs(l)-1);
            int delta=(int)Math.min(max,Math.max(1,Math.floor(fraction*base)));
            offer.addToSpecialPriceDiff(l<0?delta:-delta);
        }
    }
}
