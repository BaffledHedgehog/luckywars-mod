package me.skywars2.mixin;

import java.util.function.Predicate;
import me.skywars2.game.LbcTmtAbilities;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Native ability identity checks see their selected prototype; the actual stack and transport stay native TMT. */
@Mixin(ItemStack.class)
public abstract class TmtItemPredicateMixin {
    @Inject(method="is(Ljava/util/function/Predicate;)Z",at=@At("HEAD"),cancellable=true)
    private void skywars2$tmtIdentity(Predicate<Holder<Item>> predicate,CallbackInfoReturnable<Boolean> cir){
        if(!LbcTmtAbilities.nativePredicatesActive())return;
        var item=LbcTmtAbilities.nativePredicateItem((ItemStack)(Object)this);
        if(item!=null)cir.setReturnValue(predicate.test(item.builtInRegistryHolder()));
    }
}
