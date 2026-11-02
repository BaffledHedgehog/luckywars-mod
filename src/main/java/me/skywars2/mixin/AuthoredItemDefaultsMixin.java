package me.skywars2.mixin;

import net.minecraft.core.component.DataComponentInitializers;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 26.3's final initializer overwrites model/name components supplied during registration. */
@Mixin(Item.Properties.class)
public abstract class AuthoredItemDefaultsMixin {
    @Unique private Identifier skywars2$authoredModel;
    @Unique private Component skywars2$authoredName;

    @Inject(method="component",at=@At("HEAD"))
    private <T> void skywars2$remember(DataComponentType<T> type,T value,CallbackInfoReturnable<Item.Properties> cir){
        if(type==DataComponents.ITEM_MODEL&&value instanceof Identifier id)skywars2$authoredModel=id;
        if(type==DataComponents.ITEM_NAME&&value instanceof Component name)skywars2$authoredName=name;
    }

    @Inject(method="finalizeInitializer",at=@At("RETURN"),cancellable=true)
    private void skywars2$authoredDefaults(Component name,Identifier model,
            CallbackInfoReturnable<DataComponentInitializers.Initializer<Item>> cir){
        if(!((Item.Properties)(Object)this).itemIdOrThrow().identifier().getNamespace().equals("skywars2"))return;
        var result=cir.getReturnValue();
        if(skywars2$authoredModel!=null)result=result.add(DataComponents.ITEM_MODEL,skywars2$authoredModel);
        if(skywars2$authoredName!=null)result=result.add(DataComponents.ITEM_NAME,skywars2$authoredName);
        result=result.andThen((components,context,key)->me.skywars2.game.AuthoredItemPresentation.fill(components,name,key.identifier(),skywars2$authoredName!=null));
        cir.setReturnValue(result);
    }
}
