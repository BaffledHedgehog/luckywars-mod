package me.skywars2.mixin;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Limit only Weakness's flat deduction; authored negative bases and other modifiers stay signed. */
@Mixin(AttributeInstance.class)
public abstract class WeaknessAttributeMixin {
    private static final Identifier WEAKNESS=Identifier.withDefaultNamespace("effect.weakness");
    @Inject(method="calculateValue",at=@At("HEAD"),cancellable=true)
    private void skywars2$weaknessPenalty(CallbackInfoReturnable<Double> cir){
        var attribute=(AttributeInstance)(Object)this;
        if(!attribute.getAttribute().equals(Attributes.ATTACK_DAMAGE))return;
        var weakness=attribute.getModifier(WEAKNESS);
        if(weakness==null||weakness.operation()!=AttributeModifier.Operation.ADD_VALUE||!(weakness.amount()<0))return;
        var modifiers=attribute.getModifiers();double base=attribute.getBaseValue();
        for(var modifier:modifiers)if(modifier.operation()==AttributeModifier.Operation.ADD_VALUE&&!modifier.id().equals(WEAKNESS))base+=modifier.amount();
        // A pre-existing negative total receives no extra negative contribution from Weakness.
        base+=Math.max(weakness.amount(),-Math.max(0,base));
        double result=base;
        for(var modifier:modifiers)if(modifier.operation()==AttributeModifier.Operation.ADD_MULTIPLIED_BASE)result+=base*modifier.amount();
        for(var modifier:modifiers)if(modifier.operation()==AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)result*=1+modifier.amount();
        cir.setReturnValue(attribute.getAttribute().value().sanitizeValue(result));
    }
}
