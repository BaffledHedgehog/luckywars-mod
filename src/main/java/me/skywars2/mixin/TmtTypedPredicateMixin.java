package me.skywars2.mixin;

import me.skywars2.game.LbcTmtAbilities;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.TypedInstance;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 26.3 moved the ordinary item/tag/holder predicates to TypedInstance defaults. */
@Mixin(TypedInstance.class)
public interface TmtTypedPredicateMixin {
    private Item skywars2$selectedItem(){
        if(!LbcTmtAbilities.nativePredicatesActive()||!((Object)this instanceof ItemStack stack))return null;
        return LbcTmtAbilities.nativePredicateItem(stack);
    }
    @Inject(method="is(Ljava/lang/Object;)Z",at=@At("HEAD"),cancellable=true)
    private void skywars2$tmtType(Object type,CallbackInfoReturnable<Boolean> cir){
        var item=skywars2$selectedItem();if(item!=null)cir.setReturnValue(item==type);
    }
    @Inject(method="is(Lnet/minecraft/core/Holder;)Z",at=@At("HEAD"),cancellable=true)
    private void skywars2$tmtHolder(Holder<?> type,CallbackInfoReturnable<Boolean> cir){
        var item=skywars2$selectedItem();if(item!=null)cir.setReturnValue(item.builtInRegistryHolder()==type);
    }
    @Inject(method="is(Lnet/minecraft/tags/TagKey;)Z",at=@At("HEAD"),cancellable=true)
    private void skywars2$tmtTag(TagKey<Item> tag,CallbackInfoReturnable<Boolean> cir){
        var item=skywars2$selectedItem();if(item!=null)cir.setReturnValue(item.builtInRegistryHolder().is(tag));
    }
    @Inject(method="is(Lnet/minecraft/core/HolderSet;)Z",at=@At("HEAD"),cancellable=true)
    private void skywars2$tmtSet(HolderSet<Item> set,CallbackInfoReturnable<Boolean> cir){
        var item=skywars2$selectedItem();if(item!=null)cir.setReturnValue(set.contains(item.builtInRegistryHolder()));
    }
    @Inject(method="is(Lnet/minecraft/resources/ResourceKey;)Z",at=@At("HEAD"),cancellable=true)
    private void skywars2$tmtKey(ResourceKey<Item> key,CallbackInfoReturnable<Boolean> cir){
        var item=skywars2$selectedItem();if(item!=null)cir.setReturnValue(item.builtInRegistryHolder().is(key));
    }
}
